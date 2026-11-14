# BD QuickMart — Cloud Migration Plan

> **Implementation status (2026-11-14):** this document is retained as the
> original design/audit baseline (the "PLANNING ONLY" note below describes the
> state at the time it was authored). The migration and cloud deployment have
> since been **implemented** per the brief. For the **current, verified**
> status see [`PROJECT_PROGRESS.md`](PROJECT_PROGRESS.md); for step-by-step
> deployment see [`DEPLOYMENT.md`](DEPLOYMENT.md). Note that applying schema
> DDL / creating roles on the hosted Supabase project and the actual
> Render/Vercel deployment remain operator/approval steps, not automatic.

> **Original status (historical): PLANNING ONLY.** This document is a repository-specific audit and
> migration design. No source code, Maven dependency, database migration,
> database, deployment service, API contract, Git state, or other file has been
> created or modified while producing it. The only artefact of this task is this
> report. Every item in the implementation plan below is a **proposed future
> change that requires explicit approval** before work begins.
>
> **Baseline:** repository `D:\Versity\263\SWE lab\BD-Quickmart-`, branch `main`,
> clean working tree, `HEAD = 024bb6b`. Preserved stack: Spring Boot 3.5.6 +
> Java 21, existing Flutter customer/rider apps, existing React + TypeScript +
> Vite admin web, existing Spring Security + JWT authentication.
>
> **Approved architecture decisions (from the task brief):** fresh Supabase
> PostgreSQL project; existing MySQL data is **not** migrated (old records are
> not required); backend on Render; AI/ML (model training, evaluation,
> inference and integration) is owned by Shriful.
>
> **Confidentiality rule used throughout:** only environment-variable **names**
> are listed. No value from `.env`, `.env.example` defaults, or any live
> configuration is reproduced. New values are expressed as `<PLACEHOLDERS>`.

---

## Table of contents

1. [Current architecture audit](#1-current-architecture-audit)
2. [MySQL-to-PostgreSQL compatibility matrix](#2-mysql-to-postgresql-compatibility-matrix)
3. [Proposed PostgreSQL schema (fresh database)](#3-proposed-postgresql-schema-fresh-database)
4. [File-by-file implementation plan](#4-file-by-file-implementation-plan)
5. [Environment variable mapping](#5-environment-variable-mapping)
6. [Supabase setup instructions](#6-supabase-setup-instructions)
7. [Cloud deployment specifications](#7-cloud-deployment-specifications)
8. [Testing and acceptance plan](#8-testing-and-acceptance-plan)
9. [Risks and mitigations](#9-risks-and-mitigations)
10. [Recommended implementation order](#10-recommended-implementation-order)
11. [Estimated complexity and dependencies](#11-estimated-complexity-and-dependencies)

---

## 1. Current architecture audit

### 1.1 Backend architecture

The backend is a **Spring Boot 3.5.6 / Java 21 modular monolith** built with
Maven (`backend/pom.xml`). It is a single deployable listening on port 8080,
serving three clients over `/api/v1` REST plus a WebSocket at `/ws/tracking`.

Package layout (`backend/src/main/java/com/bdquickmart/`):

| Area | Contents | State |
|---|---|---|
| `BdQuickMartApplication.java` | `@SpringBootApplication`, `@ConfigurationPropertiesScan`, `@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")` | Implemented |
| `common/api/` | `ApiResponse`, `ApiCode`, `ApiResponses`, `HealthController` | Implemented |
| `common/exception/` | `BadRequestException`, `BusinessException`, `DuplicateResourceException`, `ResourceNotFoundException`, `GlobalExceptionHandler` | Implemented |
| `common/util/` | `TraceIdFilter` (correlation id) | Implemented |
| `config/` | `AdminBootstrapRunner`, `AppProperties`, `BootstrapProperties`, `ClockConfig`, `JwtConfig`, `JwtProperties`, `NoopTrackingHandler`, `OpenApiConfig`, `SecurityConfig`, `WebConfig`, `WebSocketConfig` | Implemented |
| `modules/auth/` | `AuthController`, `AuthService`, `JwtService`, `JwtAuthenticationFilter`, `RefreshTokenEntity`, `RefreshTokenRepository`, `RestAuthenticationEntryPoint`, `RestAccessDeniedHandler`, `TokenClaims`, 5 DTOs, 3 exceptions | Implemented (Goal 02) |
| `modules/user/` | `UserEntity`, `UserMapper`, `UserRepository`, `UserRole`, `AccountStatus`, `UserResponse` | Implemented (Goal 02) |
| `modules/admin/` | `AdminUserController`, `AdminRiderController`, `RoleProbeController` | Implemented (Goal 02) |
| `modules/customer/`, `modules/rider/` | `RoleProbeController` each + `package-info.java` | Scaffolding only |
| `modules/ai`, `analytics`, `cart`, `delivery`, `inventory`, `notification`, `order`, `payment`, `product`, `store`, `tracking` | `package-info.java` placeholders only | Reserved for Goals 03+ |

There are **15 reserved module packages**; only `auth`, `user` and the three
role-probe/admin controllers contain business logic today. All remaining
screens/endpoints are deliberately placeholders.

**API conventions:** versioned `/api/v1`; a single `ApiResponse<T>` envelope
(`success`, `code`, `message`, `data`, `path`, `timestamp`, `traceId`); stable
machine-readable codes (`BDQM-0000` success, `BDQM-04xx` client, `BDQM-05xx`
server, plus `BDQM-0402/0410/0411/0412` for auth); Jakarta Bean Validation;
`GlobalExceptionHandler` translating all exceptions into the envelope;
`TraceIdFilter` propagating `X-Trace-Id`.

### 1.2 Entities, repositories, services

| Layer | Class / table | Key facts |
|---|---|---|
| Entity | `UserEntity` → `users` | `Long id` (`GenerationType.IDENTITY`); `public_id` (String, 36); `email` (255, unique); `phone` (32, unique); `password_hash` (255, BCrypt); `full_name` (120); `role`/`status` (`@Enumerated(EnumType.STRING)`); `version` (`@Version`); `created_at`/`updated_at` (`Instant`, JPA auditing); `deleted_at` (`Instant`, soft delete) |
| Entity | `RefreshTokenEntity` → `refresh_tokens` | `Long id` (`IDENTITY`); `token_hash` (64, unique, SHA-256 hex); `family_id` (36); `@ManyToOne(LAZY) UserEntity user` via `user_id` FK; `expires_at`, `created_at`, `revoked_at`, `replaced_by_hash`, `version` |
| Repository | `UserRepository` | Derived queries only: `findByPublicId`, `findByEmail`, `findByPhone`, `findByEmailOrPhone`, `existsByEmail`, `existsByPhone`, `existsByRole`, `findAllByRoleAndStatus` |
| Repository | `RefreshTokenRepository` | Derived + JPQL: `findByHashForUpdate` with `@Lock(PESSIMISTIC_WRITE)`, `findLiveByFamilyId`, `findLiveByUserId` |
| Service | `AuthService` | register / login / refresh (rotation + replay) / logout / me / pendingRiders / approveRider / rejectRider / provision; `@Transactional`; email normalised to lowercase, phone trimmed |
| Service | `JwtService` | HS256 issue/parse; claims `sub`, `iss`, `iat`, `exp`, `jti`, `purpose`, `role`, `familyId`; SHA-256 token hashing |
| Config | `JwtConfig`, `JwtProperties`, `AppProperties`, `BootstrapProperties`, `ClockConfig`, `SecurityConfig`, `WebConfig`, `OpenApiConfig`, `WebSocketConfig` | Typed `app.*` properties; injectable `Clock`; stateless security; CORS; OpenAPI; WebSocket |
| Bootstrap | `AdminBootstrapRunner` | Opt-in first-`SUPER_ADMIN` creation, disabled by default |

**Portability note (important for migration):** both entities use **only
standard JPA annotations** — no `columnDefinition`, no `UNSIGNED`, no
`ON UPDATE`, no native SQL, no `@SequenceGenerator`/`@TableGenerator`. All
repository queries are derived or JPQL (portable). The only lock is
`@Lock(PESSIMISTIC_WRITE)`, which Hibernate renders as a row-level
`FOR UPDATE` on both databases. This means the **Java persistence layer needs
no change** for a MySQL → PostgreSQL move; the work is confined to the
driver, configuration, and DDL/migrations.

### 1.3 Every MySQL-specific SQL query, type, annotation and configuration

Exhaustive inventory from a repository-wide scan:

**(a) Build dependency**
- `backend/pom.xml:83-86` — `com.mysql:mysql-connector-j`, `runtime` scope.
- `backend/pom.xml:113-119` — H2 (test scope) used in MySQL compatibility mode.

**(b) `backend/src/main/resources/application.yml`**
- `:8` — `spring.datasource.url` default is a `jdbc:mysql://...` URL with
  MySQL-only query parameters (`useSSL`, `allowPublicKeyRetrieval`,
  `serverTimezone=Asia/Dhaka`, `characterEncoding=UTF-8`, `connectTimeout`,
  `socketTimeout`).
- `:11` — `DB_DRIVER` default `com.mysql.cj.jdbc.Driver`.
- `:28` — `DB_DDL_AUTO` default `none` (schema owned by the developer, not Hibernate).
- `:35` — `DB_DIALECT` default `org.hibernate.dialect.MySQLDialect` (pinned so
  the context can start with no live DB connection).
- `:38` — `DB_DIALECT_VERSION` default `8.0.0` (MySQL 5.5.5 fallback workaround,
  avoids the `HHH000511` warning).
- `:40` — `hibernate.jdbc.time_zone: UTC`.
- `:48` — `spring.jackson.time-zone: Asia/Dhaka` (presentation layer).
- `:64` — `app.cors.allowed-origins` default list of localhost browser origins.
- `:106-112` — Springdoc paths.
- `:123-128` — rolling log file configuration.

**(c) SQL scripts — `infrastructure/database/init/` (5 files, all MySQL 8 / phpMyAdmin-oriented)**

| File | MySQL-specific constructs |
|---|---|
| `00_create_database.sql` | `CREATE DATABASE IF NOT EXISTS \`bd_quickmart\` DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci`; `USE \`bd_quickmart\``; backtick identifiers; `information_schema.SCHEMATA` (`DEFAULT_CHARACTER_SET_NAME`, `DEFAULT_COLLATION_NAME`) |
| `01_verify_connection.sql` | `USE`; backticks; `DATABASE()`, `VERSION()`; `information_schema.TABLES` (`TABLE_ROWS`, `ENGINE`); `information_schema.SCHEMATA` |
| `02_create_auth_tables.sql` | `USE`; backticks; `BIGINT AUTO_INCREMENT`; `CHAR(36)`/`CHAR(64)`; `VARCHAR`; `DATETIME(6)`; inline `KEY` / `UNIQUE KEY`; `CONSTRAINT ... FOREIGN KEY`; `ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci` |
| `03_verify_auth_tables.sql` | `USE`; `information_schema.TABLES` (`ENGINE`, `TABLE_COLLATION`), `.COLUMNS` (`COLUMN_TYPE`, `COLUMN_KEY`), `.STATISTICS` (MySQL-only), `.KEY_COLUMN_USAGE` (`REFERENCED_TABLE_NAME`, `REFERENCED_COLUMN_NAME`) |
| `04_admin_bootstrap_TEMPLATE.sql` | `USE`; commented `INSERT` using `UUID()` and `UTC_TIMESTAMP(6)`; intentionally not runnable as-is |

**(d) Test profile**
- `backend/src/test/resources/application-test.yml:5` — H2 URL
  `jdbc:h2:mem:bdqm;MODE=MySQL;DB_CLOSE_DELAY=-1`.
- `:11` — `ddl-auto: create-drop`; `:15` — `H2Dialect`; `:16` — empty
  MySQL-specific `dialect.version`.

**(e) Documentation / examples**
- `.env.example`, `docs/development/environment-variables.md`,
  `docs/database/schema.md`, `docs/architecture/system-overview.md`,
  `docs/architecture/technology-stack.md`, `docs/development/local-setup.md`,
  `README.md`, `docs/progress.md` all describe MySQL 8 / XAMPP /
  `utf8mb4` / `utf8mb4_unicode_ci` / `MySQLDialect` / `InnoDB`.
- `docs/database/schema.md:184` — cross-cutting rule "Primary keys are
  `BIGINT UNSIGNED` auto-increment surrogate keys" (the implemented DDL uses
  plain signed `BIGINT AUTO_INCREMENT`).
- `docs/database/schema.md:176` — money rule `DECIMAL(12,2)`, no FLOAT/DOUBLE.

**Not found anywhere:** `nativeQuery = true`, `@Query(nativeQuery)`,
`columnDefinition`, `TINYINT(1)`, `ON UPDATE CURRENT_TIMESTAMP`, `ENUM` column
types, `LIMIT`-specific native SQL, `GROUP_CONCAT`, `IFNULL`, stored procedures,
or MySQL functions in Java code. `UUID()` and `UTC_TIMESTAMP(6)` appear **only**
inside the commented-out bootstrap template.

### 1.4 Existing migration mechanism and all migration files

There is **no automated migration tool**. No Flyway, no Liquibase, no
`schema.sql`/`data.sql` (confirmed by scanning `backend/pom.xml` and
`backend/src/main/resources/`; `application.yml:26` even comments that
"Flyway/Liquibase migrations arrive with the schema design goal").

The mechanism today is **manual, numbered SQL scripts executed by the developer
through the phpMyAdmin SQL tab**, in run order:

1. `00_create_database.sql` — create empty DB (charset/collation).
2. `01_verify_connection.sql` — read-only connection/charset check.
3. `02_create_auth_tables.sql` — `CREATE TABLE IF NOT EXISTS users, refresh_tokens`.
4. `03_verify_auth_tables.sql` — read-only `information_schema` verification.
5. `04_admin_bootstrap_TEMPLATE.sql` — commented, non-runnable first-admin
   `INSERT` template (alternative to the env bootstrap).

All are idempotent (`CREATE ... IF NOT EXISTS`) and contain no `DROP`,
`TRUNCATE`, `ALTER`, `DELETE` or `UPDATE`. `spring.jpa.hibernate.ddl-auto` is
pinned to `none`, so Hibernate never creates or alters the schema. Schema
ownership is explicitly **developer-managed**.
`infrastructure/README.md` documents this workflow. Only two tables
(`users`, `refresh_tokens`) exist; all other module tables are a documented
**plan** in `docs/database/schema.md` §4, not implemented.

### 1.5 PostgreSQL compatibility issues

| # | Issue | Detail | Severity |
|---|---|---|---|
| 1 | JDBC driver & dependency | `mysql-connector-j` → `org.postgresql:postgresql`; driver class `com.mysql.cj.jdbc.Driver` → `org.postgresql.Driver` | High |
| 2 | JDBC URL & query params | MySQL params (`useSSL`, `allowPublicKeyRetrieval`, `serverTimezone`, `characterEncoding`, `connectTimeout`, `socketTimeout`) are not valid for PostgreSQL. Replace with `sslmode`, `prepareThreshold` (pooler), `ApplicationName`, `connectTimeout`/`socketTimeout` (PG equivalents), etc. | High |
| 3 | Hibernate dialect | `MySQLDialect` → `PostgreSQLDialect`; remove `DB_DIALECT_VERSION` (MySQL-only workaround). With a live DB the dialect need not be pinned, but pinning avoids metadata lookups during startup — keep it pinned to the PG dialect. | High |
| 4 | `AUTO_INCREMENT` | → `GENERATED BY DEFAULT AS IDENTITY` (Hibernate 6 emits this for `GenerationType.IDENTITY` on PG) or `BIGSERIAL`. Entity mapping unchanged. | High |
| 5 | Backtick identifiers | → unquoted lowercase identifiers or double-quoted identifiers. PG folds unquoted identifiers to lowercase; avoid mixed-case quoted names. | Medium |
| 6 | `USE \`db\`` | Not valid PG. Use `\connect` (psql), `SET search_path`, or schema-qualified names. Supabase exposes a single `public` schema. | Medium |
| 7 | Table options `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci` | No PG equivalent. Encoding is database-level (`UTF8`, Supabase default); collation is column/database-level (ICU). | High |
| 8 | **Case-insensitive collation** | `utf8mb4_unicode_ci` is case- and accent-insensitive. PG default collations are **case-sensitive**. Unique constraints on `email`/`phone` would change behaviour: `AuthService.normalizeEmail()` lowercases email, but `phone` is only trimmed. Also affects future `LIKE`/`ORDER BY` for bilingual search (Goal 04). Needs functional unique indexes on `lower(...)`, `citext`, or a non-deterministic ICU collation. | High |
| 9 | `DATETIME(6)` | → `TIMESTAMPTZ` (preferred for `java.time.Instant`, UTC) or `TIMESTAMP(6)`. `hibernate.jdbc.time_zone=UTC` already set; verify Hibernate 6 maps `Instant` to `timestamp with time zone`. | High |
| 10 | `CHAR(36)` / `CHAR(64)` | Valid in PG but `CHAR(n)` is blank-padded; prefer `VARCHAR(36)`/`VARCHAR(64)`. Native `uuid` is possible but would require changing `UserEntity.publicId` / `familyId` from `String` to `UUID` (deferred). | Medium |
| 11 | `information_schema` verification scripts | MySQL `STATISTICS`, `TABLE_COLLATION`, `ENGINE`, `REFERENCED_TABLE_NAME`/`REFERENCED_COLUMN_NAME`, `TABLE_ROWS` do not exist in PG. Must rewrite using `pg_indexes`, `pg_constraint`, `information_schema.columns`, `information_schema.referential_constraints`, `pg_class`. `DATABASE()` → `current_database()`; `VERSION()` → `version()`. | Medium |
| 12 | `UUID()` / `UTC_TIMESTAMP(6)` in bootstrap template | → `gen_random_uuid()` (PG 13+ built in / `pgcrypto`) and `CURRENT_TIMESTAMP` / `now()`. | Low |
| 13 | Pessimistic locking + pooling | `FOR UPDATE` works on PG, but Supabase **transaction-mode** pooler can break session state and server-side prepared statements. Prefer Session Pooler or direct connection for the app; direct connection for migrations. | High |
| 14 | Test database | H2 `MODE=MySQL` no longer matches production. Switch to `MODE=PostgreSQL` or, better, Testcontainers PostgreSQL for real dialect verification. | Medium |
| 15 | Timezone presentation | `serverTimezone=Asia/Dhaka` disappears; PG/TIMESTAMPTZ stores UTC. `spring.jackson.time-zone: Asia/Dhaka` largely affects legacy `java.util.Date`; `Instant` serialises as ISO-8601 UTC. Verify no drift in API timestamps. | Medium |
| 16 | `ddl-auto: none` → `validate` | With Flyway owning schema, `validate` is appropriate but strict: any drift fails startup. Validate against migrations in CI before enabling. | Medium |
| 17 | Unicode / Bangla / emoji | Supabase `UTF8` database encoding handles this; no per-column charset needed. Confirm client encoding and ICU collation availability for Bangla sorting. | Low |
| 18 | Actuator exposure | `SecurityConfig` permits `/actuator/**`; `management.endpoint.health.show-details: always` and `management.info.env.enabled: true`. Review before exposing publicly on Render. | Low |

### 1.6 Authentication, refresh-token and six-role RBAC implementation

**JWT / session model**
- Stateless `SessionCreationPolicy.STATELESS`; CSRF disabled; JSON
  `RestAuthenticationEntryPoint` / `RestAccessDeniedHandler` for filter-level
  401/403; CORS from a single `CorsConfigurationSource` registered for `/**`.
- `JwtConfig.jwtSecretKey()` builds an HS256 `SecretKey` from `JWT_SECRET`
  (Base64 or raw, **≥ 32 bytes**) and **fails fast** at startup when blank,
  unless the `test`/`dev` profile or `JWT_ALLOW_EPHEMERAL_DEV_KEY=true`
  (ephemeral key invalidates all tokens on restart — never for production).
- Claims: `sub` = `public_id` (UUID string), `iss`, `iat`, `exp`, `jti`,
  `purpose` (`access`\|`refresh`), `role`, and `familyId` on refresh tokens.
  Access TTL 900 s, refresh TTL 604800 s (configurable via env vars).
- `JwtService` hashes refresh tokens with SHA-256; **only the hash is stored**.
- `AuthService.refresh()` uses `@Lock(PESSIMISTIC_WRITE)` on
  `findByHashForUpdate`, detects replay of a revoked token and revokes the
  whole `family_id`, rotates with a successor hash in `replaced_by_hash`.
  `logout()` revokes all live tokens for the user.
- `JwtAuthenticationFilter` re-reads the user row from the DB on **every**
  request and rebuilds authorities — so a role/status change takes effect
  immediately. `PERM_RIDER_ACTIVE` is granted only to `ACTIVE` riders.
- BCrypt (strength 10) via `PasswordEncoder` bean.

**Six roles** (`modules/user/UserRole.java`, shared verbatim across DB, JWT
claims, Spring Security authorities, admin-web TS types and both Flutter apps):

`CUSTOMER`, `RIDER`, `ADMIN`, `SUPER_ADMIN`, `STORE_MANAGER`,
`INVENTORY_OPERATOR`.

- Public self-registration limited to `CUSTOMER` / `RIDER`; new riders start
  `PENDING`.
- `AccountStatus`: `ACTIVE`, `PENDING`, `REJECTED`, `DISABLED`.
- Authorities: `ROLE_<NAME>` for all; plus `PERM_RIDER_ACTIVE`,
  `PERM_APPROVE_RIDERS` (ADMIN, SUPER_ADMIN), `PERM_PROVISION_PRIVILEGED`
  (SUPER_ADMIN). No `RoleHierarchy`, no `@PreAuthorize` — explicit gates in
  `SecurityConfig`.
- Endpoints: public `/api/v1/auth/register|login|refresh`; authenticated
  `/api/v1/auth/me|logout`; `/api/v1/customer/**` (`ROLE_CUSTOMER`);
  `/api/v1/rider/**` (`PERM_RIDER_ACTIVE`); `/api/v1/admin/ping` (4 privileged
  roles); `/api/v1/admin/riders/**` (`PERM_APPROVE_RIDERS`);
  `POST /api/v1/admin/users/provision` (`PERM_PROVISION_PRIVILEGED`).
- **First SUPER_ADMIN**: opt-in `AdminBootstrapRunner` guarded by
  `APP_BOOTSTRAP_ADMIN_ENABLED` (default off), requiring
  `APP_BOOTSTRAP_ADMIN_EMAIL/PASSWORD/FULL_NAME/PHONE`; hashes immediately,
  never logs, skips if any `SUPER_ADMIN` already exists. Alternative:
  `04_admin_bootstrap_TEMPLATE.sql` (commented `INSERT` with a
  `<BCRYPT_HASH>` placeholder).

**Migration impact:** authentication is entirely application-level and
database-agnostic. The only DB-touching concerns are the `users` /
`refresh_tokens` tables, the `FOR UPDATE` lock, and case-sensitivity of the
unique email/phone constraints. JWT behaviour, RBAC and API contracts are
preserved unchanged (as required).

### 1.7 Customer and Rider API configuration

Both Flutter apps (Flutter 3.35.1 / Dart 3.9.0) are independent packages with
distinct Android application IDs:
- `com.bdquickmart.customer_app` (`customer-app/`)
- `com.bdquickmart.rider_app` (`rider-app/`)

Configuration is **compile-time** via `String.fromEnvironment` in
`lib/core/config/app_config.dart`:
- `API_BASE_URL` — backend base URL including `/api/v1`.
- `WS_BASE_URL` — WebSocket endpoint (`/ws/tracking`).
- `IS_DEVELOPMENT` — dev flag.

Plus a **runtime override**: `ServerConfig` persists a user-entered base URL
in a `KeyValueStore` (`shared_preferences`); the "Change server URL" dialog on
the sign-in screen lets a physical device reach a LAN or public backend without
rebuilding. `AppConfig` defaults target the Android emulator host address
(`10.0.2.2`); iOS simulator uses localhost; physical devices need a LAN IP.
Compile-time override: `flutter run --dart-define API_BASE_URL=...`.

Networking: Dio with a `QueuedInterceptorsWrapper` that injects the bearer
token, unwraps the `ApiResponse` envelope, and performs a **single-flight**
silent refresh + exactly one retry on 401; public auth endpoints use a separate
bare Dio/AuthApi to avoid refresh loops. Tokens are stored in
`flutter_secure_storage` (Keystore/Keychain); only the non-sensitive server URL
override uses `shared_preferences`.

Android manifests: `INTERNET` permission in `main`; `usesCleartextTraffic=true`
in `debug` only (release builds keep cleartext disabled — compatible with an
HTTPS production URL). iOS uses standard ATS.

### 1.8 Admin Web API configuration and deployment requirements

React 19.3.1 + TypeScript ~7.0.2 + Vite 8.3.6, Axios 1.21, React Router 7.
Build: `npm run build` (`tsc -b && vite build`); lint: `npm run lint`
(oxlint); output directory `dist` (Vite default). Node 22.x.

Configuration is **build-time** via `import.meta.env` in
`src/lib/config/env.ts` — only `VITE_` prefixed variables are exposed to the
browser bundle:
- `VITE_API_BASE_URL`, `VITE_WS_BASE_URL`, `VITE_REQUEST_TIMEOUT`,
  `VITE_IS_DEVELOPMENT`.

`src/lib/api/client.ts` (`ApiClient`) unwraps the envelope, injects the bearer
token, and performs single-flight 401 refresh + one retry. The access token is
memory-only; the refresh token is in **`sessionStorage`**
(`src/features/auth/session.ts`) — explicitly documented as **not HttpOnly
cookie protection** (XSS-exposed); cookie-based sessions are deferred to a
later hardening goal. `AuthProvider` + `RequireAuth` + `RequireRole` guard
routes; `roles.ts` defines `PRIVILEGED_ROLES` (4 roles) and
`RIDER_APPROVER_ROLES` (ADMIN, SUPER_ADMIN).

Routing uses `createBrowserRouter` (`src/app/router.tsx`) with paths `/login`,
`/unauthorized`, `/`, `/orders`, `/products`, `/inventory`, `/riders`,
`/analytics` and a `*` catch-all → **requires an SPA fallback rewrite on the
host** or deep links will 404.

Deployment requirements: set `VITE_*` variables **before build** (they are
baked into the bundle); add the production origin to the backend's
`APP_CORS_ALLOWED_ORIGINS` (`WebConfig` uses `setAllowCredentials(true)` with
explicit origins, so `*` is not allowed); use HTTPS/`wss://` in production.

### 1.9 Existing health endpoint and Swagger configuration

- **`GET /api/v1/health`** — custom `HealthController`. Returns the standard
  envelope with `status: UP`, `application`, `timestamp`, `database`
  (`UP`/`DOWN`/`UNKNOWN`) and `details`. The DB probe runs on a daemon thread
  with a **2-second bound**, uses `Connection.isValid(2)`, and **always returns
  HTTP 200** even when the DB is down (liveness, not readiness).
- **Spring Boot Actuator** — `management.endpoints.web.exposure.include =
  health,info,metrics`; `management.endpoint.health.show-details: always`;
  `management.info.env.enabled: true`. `/actuator/health` includes the
  auto-configured DataSource health indicator and returns **503 when the DB is
  down** (suitable as a readiness probe).
- **Springdoc / OpenAPI** — `springdoc.api-docs.path: /v3/api-docs`,
  `springdoc.swagger-ui.path: /swagger-ui.html`; `OpenApiConfig` defines the
  `bearerAuth` HTTP bearer/JWT scheme, an `Info` block, and **two hard-coded
  server entries**: a localhost dev server and a placeholder production server
  `https://api.bdquickmart.com` — the latter must be replaced with the real
  Render URL (or made configurable).
- `SecurityConfig` permits `/api/v1/health`, `/actuator/**`, `/v3/api-docs/**`,
  `/swagger-ui/**`, `/swagger-ui.html`, `/api-docs/**`, `/ws/**`;
  `JwtAuthenticationFilter.shouldNotFilter()` mirrors these paths.
- `WebSocketConfig` registers `/ws/tracking` with `NoopTrackingHandler`
  (placeholder until Goal 08), `setAllowedOriginPatterns("*")`.

### 1.10 Existing environment variables — names only

Per the confidentiality rule, **only names are listed**; no values, defaults or
secrets are reproduced.

**Backend (root `.env.example` / `application.yml` / `AppProperties`)**

| Group | Variable names |
|---|---|
| Database | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `DB_DRIVER`, `DB_DDL_AUTO`, `DB_DIALECT`, `DB_DIALECT_VERSION`, `DB_POOL_MAX_SIZE`, `DB_POOL_MIN_IDLE`, `DB_POOL_CONNECTION_TIMEOUT`, `DB_POOL_INIT_FAIL_TIMEOUT`, `DB_SHOW_SQL` |
| Server | `SERVER_PORT`, `SERVER_CONTEXT_PATH`, `LOG_LEVEL_BACKEND`, `LOG_LEVEL_SQL`, `LOG_FILE` |
| API / CORS | `APP_API_VERSION`, `APP_API_BASE_PATH`, `APP_CORS_ALLOWED_ORIGINS` |
| JWT | `JWT_SECRET`, `JWT_ISSUER`, `JWT_ACCESS_EXPIRATION`, `JWT_REFRESH_EXPIRATION`, `JWT_ALLOW_EPHEMERAL_DEV_KEY` |
| First-admin bootstrap | `APP_BOOTSTRAP_ADMIN_ENABLED`, `APP_BOOTSTRAP_ADMIN_EMAIL`, `APP_BOOTSTRAP_ADMIN_PASSWORD`, `APP_BOOTSTRAP_ADMIN_FULL_NAME`, `APP_BOOTSTRAP_ADMIN_PHONE` |
| AI providers | `GEMINI_API_KEY`, `GROQ_API_KEY`, `AI_CHAT_MODEL`, `AI_EMBEDDING_MODEL`, `AI_ENABLED` |
| Client URL hints | `CUSTOMER_API_BASE_URL`, `RIDER_API_BASE_URL`, `ADMIN_API_BASE_URL` |

**Customer app (`customer-app/.env.example`)** — `API_BASE_URL`, `WS_BASE_URL`,
`IS_DEVELOPMENT`.

**Rider app (`rider-app/.env.example`)** — `API_BASE_URL`, `WS_BASE_URL`,
`IS_DEVELOPMENT`.

**Admin web (`admin-web/.env.example`)** — `VITE_API_BASE_URL`,
`VITE_WS_BASE_URL`, `VITE_REQUEST_TIMEOUT`, `VITE_IS_DEVELOPMENT`.

`.env` is git-ignored; only `.env.example` files are tracked. No credential is
committed. `start-backend.ps1` loads the root `.env` into the **process**
environment on Windows (values never printed; real env vars take precedence;
restored on exit) — this is a local-development convenience only; production
uses real environment variables with the packaged jar.

### 1.11 Existing tests and database test strategy

- Backend: `cd backend && mvn -B clean verify` — 27 tests, 0 failures/errors.
  Full-stack MockMvc integration tests (`BaseIntegrationTest` with
  `@SpringBootTest`, `@AutoConfigureMockMvc`, `@ActiveProfiles("test")`) use a
  **real Spring Security chain, real BCrypt/JWT/JPA** against **H2 in-memory in
  `MODE=MySQL`** with `ddl-auto: create-drop` and `H2Dialect`. The `test`
  profile permits an ephemeral JWT key, so `mvn verify` needs **no environment
  variables and no MySQL**. `TestClockConfiguration` provides a mutable
  `Clock` so token expiry and audit timestamps are deterministic.
  Test classes: `AuthIntegrationTest`, `HealthControllerTest`,
  `RealHttpSmokeTest`, `BaseIntegrationTest`, `TestClockConfiguration`.
- Customer app: `flutter analyze`, `flutter test` (14 tests),
  `flutter build apk --debug`.
- Rider app: `flutter analyze`, `flutter test` (16 tests),
  `flutter build apk --debug`.
- Admin web: `npm run lint` (oxlint), `npm run build`.
- Live (manual, developer) verification was previously performed against the
  real local MySQL via `GET /api/v1/health` reporting `database: UP` and the
  read-only `03_verify_auth_tables.sql`; this is evidence in `docs/progress.md`,
  not part of automated CI.

**Migration impact:** no test currently exercises a real PostgreSQL dialect.
The test strategy must be extended (see §8) with PostgreSQL-mode H2 or
Testcontainers PostgreSQL, plus migration/schema-validation tests.

### 1.12 AI/ML integration points and proposed service boundary

**Current state (no implementation):**
- `ai/README.md` defines a home for versioned prompts (`prompts/`), eval sets
  (`evals/`), model config/fallback chains (`models/`) and forecasting
  artifacts (`forecasting/`), targeting Goals 10–13 / 15 / 16. It states a
  clear doctrine: Gemini/Groq are **language models** for explanation,
  insights, recommendations and the bilingual assistant; they are **not** the
  quantitative engine for demand forecasting (moving averages → LLM-assisted
  analysis → XGBoost/LightGBM → purpose-built model). Prompts are bilingual
  (Bangla/English), forbid hallucinated catalogue data, and are versioned.
- Backend integration point (config only): `AppProperties.Ai` record
  (`geminiApiKey`, `groqApiKey`, `chatModel`, `embeddingModel`, `enabled`)
  bound from `app.ai.*`; env vars `GEMINI_API_KEY`, `GROQ_API_KEY`,
  `AI_CHAT_MODEL`, `AI_EMBEDDING_MODEL`, `AI_ENABLED`. `AI_ENABLED` defaults
  off; with blank keys the platform runs fully and makes no AI network calls.
  `modules/ai/package-info.java` is an empty placeholder. **No AI service,
  controller, or client exists yet.**
- Planned AI/ML tables (`docs/database/schema.md` §4): `product_embeddings`,
  `recommendation_cache` (Goal 12), `demand_forecasts` (Goal 10).
- Shriful owns model training, evaluation, inference and integration.

**Proposed service boundary (design only):**
- A **separate Python inference service** (e.g. FastAPI/Uvicorn, containerised,
  deployed independently of the Spring monolith) hosts models and LLM calls.
  The Spring backend remains the only public API and calls the inference
  service **server-to-server** with an internal service credential
  (mTLS or a short-lived service JWT). No AI provider key is ever placed in a
  Flutter or browser bundle; keys stay in the inference service (or the
  backend), injected as secrets.
- **Training is fully offline/separate**: `ai/` assets, scheduled pipelines,
  and a versioned model registry; artifacts are promoted to the inference
  service, never trained on the serving path.
- **Statistical vs LLM split preserved**: XGBoost/LightGBM (or the purpose-built
  model) produces numbers; Gemini/Groq explain them.
- **Data plane**: use Supabase `pgvector` for `product_embeddings` and a cache
  table for `recommendation_cache`; `demand_forecasts` in PostgreSQL. Decision
  needed: same Supabase database vs a dedicated vector store.
- **Versioning & monitoring**: versioned inference endpoints, model version in
  every response, latency/accuracy/drift metrics, structured logs, Gemini→Groq
  fallback chain, and a documented rollback to the previous model version.
- **Backend wiring point**: replace the current `app.ai.*` config/flag with an
  `AI_INFERENCE_SERVICE_URL` + service-token pair; keep `AI_ENABLED` as the
  master switch so the platform still runs fully with AI disabled.

---

## 2. MySQL-to-PostgreSQL compatibility matrix

| Area | Current (MySQL 8) | Target (Supabase PostgreSQL) | Where it lives | Action |
|---|---|---|---|---|
| JDBC driver | `com.mysql:mysql-connector-j` | `org.postgresql:postgresql` | `backend/pom.xml` | Swap dependency |
| Driver class | `com.mysql.cj.jdbc.Driver` | `org.postgresql.Driver` | `application.yml` `DB_DRIVER` default | Change default / set env var |
| JDBC URL | `jdbc:mysql://...` + `useSSL`, `allowPublicKeyRetrieval`, `serverTimezone`, `characterEncoding` | `jdbc:postgresql://...` + `sslmode=require`, pooler params, `ApplicationName` | `application.yml` `DB_URL` default | Replace |
| Hibernate dialect | `org.hibernate.dialect.MySQLDialect` | `org.hibernate.dialect.PostgreSQLDialect` | `application.yml` `DB_DIALECT` | Change |
| Dialect version pin | `DB_DIALECT_VERSION=8.0.0` | Not applicable (remove) | `application.yml` / `.env.example` | Remove |
| Schema ownership | `ddl-auto: none`, manual phpMyAdmin scripts | Flyway versioned migrations + `ddl-auto: validate` | `application.yml`, new `db/migration/` | Introduce |
| Primary key | `BIGINT AUTO_INCREMENT` | `BIGINT GENERATED BY DEFAULT AS IDENTITY` | `02_create_auth_tables.sql` → `V1__*.sql` | Rewrite DDL |
| JPA ID strategy | `GenerationType.IDENTITY` | Same (Hibernate 6 emits identity on PG) | `UserEntity`, `RefreshTokenEntity` | **No change** |
| Identifiers | Backticks `` `users` `` | Unquoted lowercase or `"double quotes"` | All 5 SQL scripts | Rewrite |
| Database selection | `USE \`bd_quickmart\`` | `search_path` / `\connect` / schema-qualified | All 5 SQL scripts | Rewrite |
| Table engine | `ENGINE = InnoDB` | Not applicable | `02_create_auth_tables.sql` | Remove |
| Charset | `DEFAULT CHARSET = utf8mb4` | Database-level `UTF8` (Supabase default) | `00`, `02` | Remove; verify DB encoding |
| Collation | `utf8mb4_unicode_ci` (case/accent-insensitive) | ICU non-deterministic collation, `citext`, or `lower()` functional unique indexes | `00`, `02` | **Design decision** (§3) |
| String types | `VARCHAR(n)`, `CHAR(36)`, `CHAR(64)` | `VARCHAR(n)` / `TEXT`; optional native `uuid` later | `02` | Prefer `VARCHAR`; defer `uuid` |
| Enum-ish columns | `role VARCHAR(32)`, `status VARCHAR(24)` (`EnumType.STRING`) | `VARCHAR` + `CHECK` constraint (or a `roles` lookup table) | `02` | Add `CHECK` |
| Timestamps | `DATETIME(6)` (UTC via `hibernate.jdbc.time_zone`) | `TIMESTAMPTZ` (UTC) | `02` | Rewrite |
| Money (future tables) | `DECIMAL(12,2)` | `NUMERIC(12,2)` | `docs/database/schema.md` §5 | Preserve rule |
| Inline indexes | `KEY idx_x (...)`, `UNIQUE KEY uk_x (...)` | `CREATE INDEX` / `UNIQUE` constraints (separate statements or column constraints) | `02` | Rewrite |
| Foreign keys | `CONSTRAINT fk_refresh_user FOREIGN KEY ...` | Same, `ON DELETE RESTRICT`/`NO ACTION` (soft deletes) | `02` | Preserve |
| Optimistic locking | `version BIGINT` + `@Version` | Same | Entity + DDL | **No change** |
| Pessimistic lock | `@Lock(PESSIMISTIC_WRITE)` → `FOR UPDATE` | `FOR UPDATE` (supported) — avoid transaction-mode pooler | `RefreshTokenRepository` | Verify under pooler |
| Verification queries | `information_schema.TABLES/COLUMNS/STATISTICS/KEY_COLUMN_USAGE`, `DATABASE()`, `VERSION()`, `TABLE_ROWS`, `ENGINE`, `TABLE_COLLATION` | `pg_indexes`, `pg_constraint`, `information_schema.columns`, `referential_constraints`, `current_database()`, `version()` | `01`, `03` | Rewrite |
| UUID / time functions | `UUID()`, `UTC_TIMESTAMP(6)` (template only) | `gen_random_uuid()`, `CURRENT_TIMESTAMP`/`now()` | `04` template | Rewrite if used |
| First-admin bootstrap | Env `APP_BOOTSTRAP_ADMIN_*` runner or SQL template | Same runner works; PG-flavoured template | `AdminBootstrapRunner`, `04` | Preserve; hash secrets |
| Test DB | H2 `MODE=MySQL`, `H2Dialect` | H2 `MODE=PostgreSQL` or Testcontainers PostgreSQL | `application-test.yml` | Change |
| Timezone handling | `serverTimezone=Asia/Dhaka` in URL; `spring.jackson.time-zone` | UTC storage in `TIMESTAMPTZ`; presentation in Asia/Dhaka | `application.yml` | Verify |
| Bangla/emoji | `utf8mb4` | `UTF8` DB encoding + ICU collation | DB setup | Verify |
| Connection pool | Hikari `maximum-pool-size: 10`, `idle-timeout: 600000`, `max-lifetime: 1800000`, `initialization-fail-timeout: -1` | Same, tuned to Supabase limits; consider enabling keepalive | `application.yml` | Tune |
| SSL | `useSSL=false` (local) | `sslmode=require` (Supabase enforces TLS) | `DB_URL` | Enforce |
| Health probe | `Connection.isValid(2)` | Same (JDBC standard) | `HealthController` | **No change** |

---

## 3. Proposed PostgreSQL schema (fresh database)

> No old data is migrated. The schema is created from versioned migrations on a
> fresh Supabase project. Only `users` and `refresh_tokens` are implemented
> today; the remaining module tables are a documented plan
> (`docs/database/schema.md` §4) and are **not** part of this initial schema.

### 3.1 Design decisions

| Concern | Decision | Rationale |
|---|---|---|
| **Identity strategy** | Internal PKs: `BIGINT GENERATED BY DEFAULT AS IDENTITY`. External IDs (`public_id`, `family_id`): application-generated UUID v4 strings stored as `VARCHAR(36)` with a length/uniqueness constraint. | `GenerationType.IDENTITY` needs no entity change. Keeping UUIDs as strings preserves the JWT `sub`, API responses, `UserEntity.publicId`/`RefreshTokenEntity.familyId` `String` types and the API contract. A later, separately-approved change could move to native `uuid` + `gen_random_uuid()` and `UUID` Java types. |
| **Timestamp / timezone** | All audit/token timestamps `TIMESTAMPTZ NOT NULL`, stored in UTC (existing `hibernate.jdbc.time_zone: UTC` and injectable `Clock`). Presentation renders `Asia/Dhaka`. | Matches `java.time.Instant`; avoids the MySQL `serverTimezone` URL parameter. |
| **Numeric / decimal precision** | Money columns `NUMERIC(12,2)`; never `FLOAT`/`DOUBLE`. (No money columns exist in the two implemented tables; this rule applies to future module tables.) | From `docs/database/schema.md` §5; exact decimal arithmetic. |
| **Case-insensitive uniqueness** | Keep plain `UNIQUE` constraints on `email`/`phone`, and add **functional unique indexes** `UNIQUE (lower(email))` and `UNIQUE (lower(phone))` as defence. Alternative: `citext` or a non-deterministic ICU collation. | Preserves `utf8mb4_unicode_ci` semantics. `AuthService` already lowercases email; phone is only trimmed, so the functional index closes the gap. Non-deterministic ICU collations cannot be used with `LIKE` and affect future bilingual search — functional indexes are safer. |
| **Six-role seed** | **Initial (recommended):** `role VARCHAR(32)` + `CHECK (role IN (...six values...))`; roles are the code-defined `UserRole` enum (single source of truth), so there is no table to seed. **Optional normalised alternative:** a `roles` lookup table seeded with 6 `INSERT ... ON CONFLICT DO NOTHING` rows and `users.role_id` FK — requires `UserEntity`, JWT claim, admin-web and Flutter enum changes. | Zero code change initially; keeps the "shared verbatim" role invariant in `UserRole.java`. |
| **Soft deletes** | `deleted_at TIMESTAMPTZ NULL`; `fk_refresh_user` uses `ON DELETE RESTRICT`/`NO ACTION` (rows are never hard-deleted). | Matches current design; preserves history. |
| **Schema / search path** | Single `public` schema (Supabase default); explicit `SET search_path` for migration scripts. | No `USE` equivalent needed. |
| **Charset** | Database encoding `UTF8`; ICU collation available for Bangla sorting. | Handles Bangla script and emoji. |

### 3.2 `users`

```sql
-- V1__init_auth_schema.sql (PostgreSQL / Supabase)
CREATE TABLE users (
    id             BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    public_id      VARCHAR(36)  NOT NULL,
    email          VARCHAR(255) NOT NULL,
    phone          VARCHAR(32)  NOT NULL,
    password_hash  VARCHAR(255) NOT NULL,
    full_name      VARCHAR(120) NOT NULL,
    role           VARCHAR(32)  NOT NULL,
    status         VARCHAR(24)  NOT NULL,
    version        BIGINT       NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ  NOT NULL,
    updated_at     TIMESTAMPTZ  NOT NULL,
    deleted_at     TIMESTAMPTZ,

    CONSTRAINT uk_users_public_id UNIQUE (public_id),
    CONSTRAINT uk_users_email     UNIQUE (email),
    CONSTRAINT uk_users_phone     UNIQUE (phone),
    CONSTRAINT ck_users_role      CHECK (role IN
        ('CUSTOMER','RIDER','ADMIN','SUPER_ADMIN','STORE_MANAGER','INVENTORY_OPERATOR')),
    CONSTRAINT ck_users_status    CHECK (status IN
        ('ACTIVE','PENDING','REJECTED','DISABLED'))
);

-- Preserve case/accent-insensitive uniqueness semantics of utf8mb4_unicode_ci.
CREATE UNIQUE INDEX uk_users_email_lower ON users (lower(email));
CREATE UNIQUE INDEX uk_users_phone_lower ON users (lower(phone));

-- Serves AuthService.pendingRiders() / findAllByRoleAndStatus().
CREATE INDEX idx_users_role_status ON users (role, status);
```

### 3.3 `refresh_tokens`

```sql
CREATE TABLE refresh_tokens (
    id               BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    token_hash       VARCHAR(64) NOT NULL,
    family_id        VARCHAR(36) NOT NULL,
    user_id          BIGINT      NOT NULL,
    expires_at       TIMESTAMPTZ NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL,
    revoked_at       TIMESTAMPTZ,
    replaced_by_hash VARCHAR(64),
    version          BIGINT      NOT NULL DEFAULT 0,

    CONSTRAINT uk_refresh_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE RESTRICT
);

CREATE INDEX idx_refresh_family ON refresh_tokens (family_id);
CREATE INDEX idx_refresh_user   ON refresh_tokens (user_id);
-- Supports the replay-detection query (live members of a family).
CREATE INDEX idx_refresh_family_live
    ON refresh_tokens (family_id) WHERE revoked_at IS NULL;
```

### 3.4 Tables, relationships, keys and indexes summary

| Table | PK | FKs | Unique | Indexes |
|---|---|---|---|---|
| `users` | `id` (identity) | — | `public_id`, `email`, `phone`, `lower(email)`, `lower(phone)` | `idx_users_role_status` |
| `refresh_tokens` | `id` (identity) | `user_id → users.id` (`fk_refresh_user`, RESTRICT) | `token_hash` | `idx_refresh_family`, `idx_refresh_user`, `idx_refresh_family_live` (partial) |

Relationship: one `users` row → many `refresh_tokens` rows (`@ManyToOne LAZY`,
`optional = false`). Both tables carry `@Version` optimistic-lock columns and
JPA-audited `Instant` timestamps.

### 3.5 Planned module tables (not implemented — forward design only)

From `docs/database/schema.md` §4, staged by goal: `user_addresses`,
`otp_verifications`, `customers`, `riders`, `rider_documents`, `stores`,
`store_service_areas`, `products`, `categories`, `product_store_prices`,
`inventory_batches`, `stock_movements`, `carts`, `cart_items`, `orders`,
`order_items`, `order_status_history`, `payments`, `deliveries`,
`rider_assignments`, `rider_locations`, `product_embeddings`,
`recommendation_cache`, `notifications`, `device_tokens`, `demand_forecasts`.

All must follow the cross-cutting rules: bilingual `<field>` / `<field>_bn`
text columns, `NUMERIC(12,2)` money, UTC `TIMESTAMPTZ`, identity PKs,
`created_at`/`updated_at` audit columns, and `deleted_at` soft deletes.

### 3.6 Initial six-role seed strategy

- **Recommended (initial):** no `roles` table. The six role identifiers are the
  `UserRole` enum in `backend/.../modules/user/UserRole.java` and are enforced
  in the database by `ck_users_role`. "Seeding" consists of the `CHECK`
  constraint plus the existing registration/provisioning paths:
  `CUSTOMER`/`RIDER` via public `POST /api/v1/auth/register` (RIDER starts
  `PENDING`), and the four privileged roles via
  `POST /api/v1/admin/users/provision` (`PERM_PROVISION_PRIVILEGED`) or the
  first-admin bootstrap. This preserves the shared-verbatim role invariant and
  requires zero code change.
- **Optional normalised alternative (later, requires approval):** a `roles`
  table seeded with six rows via `INSERT ... ON CONFLICT (name) DO NOTHING`, a
  `users.role_id` FK, and a `RoleEntity`. This improves relational integrity
  but changes `UserEntity`, JWT `role` claim handling, admin-web TS types and
  both Flutter enums — a much larger blast radius. Not recommended for the
  initial migration.

### 3.7 Secure SUPER_ADMIN provisioning

Reuse the existing opt-in mechanism, hardened for a hosted environment:

1. On the **first** Render deploy (single instance), set
   `APP_BOOTSTRAP_ADMIN_ENABLED` plus `APP_BOOTSTRAP_ADMIN_EMAIL`,
   `APP_BOOTSTRAP_ADMIN_PASSWORD`, `APP_BOOTSTRAP_ADMIN_FULL_NAME`,
   `APP_BOOTSTRAP_ADMIN_PHONE` as **secret** environment variables.
2. `AdminBootstrapRunner` hashes the password with BCrypt immediately, never
   logs it, and skips creation when a `SUPER_ADMIN` already exists.
3. **Immediately disable** `APP_BOOTSTRAP_ADMIN_ENABLED` after the first
   successful start and remove the four secret values (env vars are visible
   via process inspection).
4. **Concurrency guard:** the current `existsByRole(SUPER_ADMIN)` check is racy
   if Render ever runs more than one instance. Mitigate with a single-instance
   deploy for provisioning, a PostgreSQL advisory lock
   (`pg_advisory_xact_lock`) in a migration, or a partial unique index; do not
   rely on the in-process check alone.
5. **Alternative:** a one-off versioned migration / `psql` script that inserts
   a pre-generated BCrypt hash (never plaintext) from the
   `04_admin_bootstrap_TEMPLATE.sql` pattern, with PG syntax
   (`gen_random_uuid()`, `CURRENT_TIMESTAMP`). No secret is ever committed.
6. Verify exactly one `SUPER_ADMIN` exists, then confirm privileged
   provisioning and rider approval work end-to-end.

### 3.8 Versioned PostgreSQL migration strategy

**Recommendation: Flyway**, integrated with Spring Boot (native support).

- Add `flyway-core` and `flyway-database-postgresql` (versions managed by the
  Spring Boot 3.5.6 BOM) to `backend/pom.xml`.
- Store migrations in `backend/src/main/resources/db/migration/` using strict
  versioned naming: `V1__init_auth_schema.sql`, `V2__...sql`, … plus optional
  repeatable `R__*.sql` for views/grants.
- Configure `spring.flyway.enabled`, `locations`, `baseline-on-migrate`,
  `validate-on-migrate`, and switch `spring.jpa.hibernate.ddl-auto` from
  `none` to `validate` so Hibernate verifies (never mutates) the schema.
- Sequence: Supabase project ready → run migrations (Flyway at app startup, a
  Render pre-deploy command, or `psql`/Supabase CLI as a separate job) →
  start backend with `ddl-auto: validate` → health/acceptance checks.
- Flyway serialises concurrent migrators with its own lock, which matters if
  Render runs more than one instance.
- **Alternative:** Supabase CLI migrations (`supabase/migrations/*.sql`) for
  database-as-code, applied out-of-band before deploy. Simpler ops, but keeps
  schema changes out of the application repo and requires a separate release
  step. **Alternative 2:** retain the current manual numbered-script process
  with an explicit `schema_migrations` ledger table. Least recommended — it
  does not provide automated versioning or drift detection.
- The five existing MySQL scripts should be **retained as a historical/legacy
  reference** (or moved under `infrastructure/database/legacy/mysql/`) and new
  PostgreSQL scripts added; nothing is deleted in this plan.

### 3.9 Rollback and recovery approach

- **Forward-only migrations.** Flyway `undo` is a commercial (Teams) feature;
  assume it is unavailable. Rollbacks are corrective **forward** migrations
  (`V3__fix_...sql`), never `DROP`/`TRUNCATE`/destructive edits to a live
  schema. Use an **expand/contract** pattern for later breaking changes.
- **Because the database is fresh and no legacy data is migrated**, the
  ultimate rollback is trivially safe: drop the empty project and recreate it
  from `V1` onward. This is the primary recovery path for the initial cutover.
- **Supabase backups / PITR**: enable the plan's backup and Point-in-Time
  Recovery features where available; document the restore runbook. Keep
  logical `pg_dump` snapshots as an independent, portable backup.
- **RPO/RTO**: to be defined by the team before go-live (no duration estimates
  in this plan); document them in the runbook once the backup tier is chosen.
- **Application rollback**: Render supports rolling back to a previous deploy.
  Ensure the previous deploy is compatible with the current schema version
  (expand/contract), otherwise roll the schema forward first.
- **Migration safety rules**: every migration is idempotent where possible,
  reviewed, tested on a preview/branch database first, and applied to
  production only after `mvn verify` and `flyway validate` pass. No secret is
  ever embedded in a migration file.

---

## 4. File-by-file implementation plan

> **All entries below are proposed future changes requiring explicit approval.**
> None is applied now. No file is created or modified by this planning task
> other than this report.

| # | File | Proposed change | Why / notes |
|---|---|---|---|
| 1 | `backend/pom.xml` | Replace `com.mysql:mysql-connector-j` with `org.postgresql:postgresql` (runtime); add `flyway-core` + `flyway-database-postgresql` | PostgreSQL JDBC + versioned migrations |
| 2 | `backend/src/main/resources/application.yml` | `DB_URL` default → PostgreSQL URL; `DB_DRIVER` → PG driver; `DB_DIALECT` → `PostgreSQLDialect`; remove `DB_DIALECT_VERSION`; `ddl-auto` → `validate`; add `spring.flyway.*`; `APP_CORS_ALLOWED_ORIGINS` → include Vercel origin; map `server.port` to the host `PORT`; review `spring.jackson.time-zone` | Core configuration switch |
| 3 | `backend/src/main/resources/db/migration/V1__init_auth_schema.sql` (**new**) | PostgreSQL DDL from §3 | Fresh schema |
| 4 | `backend/src/main/resources/db/migration/V2__*.sql` (**new**, optional) | `roles` lookup seed **only if** the normalised alternative is chosen | Deferred decision |
| 5 | `backend/src/main/java/com/bdquickmart/modules/user/UserEntity.java` | **No change** (portable). Only if native `uuid` is adopted: change `publicId` to `UUID` | Preserve API contract |
| 6 | `backend/src/main/java/com/bdquickmart/modules/auth/RefreshTokenEntity.java` | **No change** (portable) | — |
| 7 | `backend/src/main/java/com/bdquickmart/modules/auth/RefreshTokenRepository.java` | **No change**; verify `FOR UPDATE` behaves under the chosen Supabase pooler | JPQL portable |
| 8 | `backend/src/main/java/com/bdquickmart/config/OpenApiConfig.java` | Replace the hard-coded placeholder production `Server` URL with the Render URL (or make it configurable) | Correct API docs |
| 9 | `backend/src/main/java/com/bdquickmart/config/WebConfig.java` / `SecurityConfig.java` | No functional change; confirm CORS origins and review public `/actuator/**` exposure for production | Hardening |
| 10 | `backend/src/main/java/com/bdquickmart/common/api/HealthController.java` | No change required; document that it is liveness-only (always 200) and use `/actuator/health` for readiness | Health-check semantics |
| 11 | `backend/src/test/resources/application-test.yml` | `MODE=MySQL` → `MODE=PostgreSQL`, or switch to Testcontainers PostgreSQL; remove MySQL-specific `dialect.version` | Test parity |
| 12 | `backend/src/test/java/com/bdquickmart/*` (**new tests**) | PostgreSQL-dialect integration tests, migration/`ddl-auto: validate` test, concurrency test for refresh rotation, case-insensitive uniqueness test, UTC/`TIMESTAMPTZ` and Bangla round-trip tests | §8 |
| 13 | `infrastructure/database/init/00–04*.sql` | Retain as legacy MySQL reference; add PostgreSQL equivalents (or a `legacy/mysql/` folder). Rewrite verification scripts with `pg_catalog`/`information_schema` PG views | No deletion |
| 14 | `infrastructure/database/init/04_admin_bootstrap_TEMPLATE.sql` | PG syntax if used: `gen_random_uuid()`, `CURRENT_TIMESTAMP` | Secure provisioning |
| 15 | `.env.example` | Replace MySQL-oriented variable descriptions/defaults with PostgreSQL/Supabase ones; add new names from §5 | Names only in docs |
| 16 | `docs/development/environment-variables.md` | Update all tables for PostgreSQL, Render, Vercel, Supabase; add new variables | Documentation |
| 17 | `docs/database/schema.md` | Rewrite identity/charset/collation/timestamp/migration sections for PostgreSQL; document Flyway | Documentation |
| 18 | `docs/architecture/system-overview.md`, `technology-stack.md` | Update DB diagram/stack (MySQL → Supabase PostgreSQL; add Render/Vercel) | Documentation |
| 19 | `docs/development/local-setup.md`, `README.md`, `docs/progress.md` | Update setup/verify commands and evidence for the new topology | Documentation |
| 20 | `start-backend.ps1` | Keep for local dev; confirm production uses real env vars with the packaged jar | Local-only mechanism |
| 21 | `customer-app/.env.example`, `rider-app/.env.example` | Document production `--dart-define` values (placeholders) | Flutter build config |
| 22 | `customer-app/lib/core/config/app_config.dart`, `rider-app/...` | No source change required; supply production URL at build time via `--dart-define` | Compile-time config |
| 23 | `admin-web/.env.example`, `admin-web/src/lib/config/env.ts`, `admin-web/src/vite-env.d.ts` | Document/produce production `VITE_*` values; no secrets | Build-time config |
| 24 | `admin-web/vercel.json` (**new**) | SPA fallback rewrites (`/*` → `/index.html`) + optional security headers | Deep-link support |
| 25 | `admin-web/src/features/auth/session.ts` | **Later hardening (separate goal):** move refresh token to Secure/HttpOnly/SameSite cookie with server-side rotation | XSS mitigation |
| 26 | `render.yaml` / Render dashboard (**new**) | Java 21 build/start commands, env var names, health-check path, plan/region | Backend hosting |
| 27 | `ai/` + separate Python inference service (**new, Shriful**) | `AI_INFERENCE_SERVICE_URL`/token wiring in backend; `pgvector` tables; model registry & monitoring | AI/ML boundary |
| 28 | `BD_QuickMart_Feature_List_and_Team_Responsibilities.md` | **Do not modify** (pre-existing team file, not authored by this plan) | Scope boundary |

---

## 5. Environment variable mapping

**Only names are listed.** New values are expressed as `<PLACEHOLDERS>`; no
real value from `.env` or any live configuration appears here.

### 5.1 Backend — existing names whose *meaning* changes

| Name | Current meaning | PostgreSQL/cloud target | Notes |
|---|---|---|---|
| `DB_URL` | MySQL JDBC URL | PostgreSQL JDBC URL `<SUPABASE_PG_URL>` with `sslmode=require` (+ pooler params) | MySQL-only URL params removed |
| `DB_USERNAME` | MySQL user | Supabase application role `<DB_APP_ROLE>` | Least-privilege role |
| `DB_PASSWORD` | MySQL password | Supabase role password `<DB_APP_PASSWORD>` | Secret; never committed |
| `DB_DRIVER` | `com.mysql.cj.jdbc.Driver` | `org.postgresql.Driver` | Set explicitly |
| `DB_DDL_AUTO` | `none` | `validate` (with Flyway owning schema) | Validate in CI first |
| `DB_DIALECT` | `MySQLDialect` | `PostgreSQLDialect` | Keep pinned |
| `DB_DIALECT_VERSION` | MySQL version workaround | **Remove** (not applicable) | — |
| `DB_POOL_MAX_SIZE` / `DB_POOL_MIN_IDLE` / `DB_POOL_CONNECTION_TIMEOUT` / `DB_POOL_INIT_FAIL_TIMEOUT` | Hikari tuning | Retune to Supabase connection limits (small pool) | See §6 |
| `DB_SHOW_SQL` | SQL logging | Unchanged | Keep off in production |
| `APP_CORS_ALLOWED_ORIGINS` | localhost origins | Add `<VERCEL_PRODUCTION_ORIGIN>` | Explicit origins required (`allowCredentials=true`) |
| `SERVER_PORT` | `8080` | Set to the Render-provided `PORT` value | Render injects `PORT`; map it (see §7.2) |
| `JWT_SECRET`, `JWT_ISSUER`, `JWT_ACCESS_EXPIRATION`, `JWT_REFRESH_EXPIRATION`, `JWT_ALLOW_EPHEMERAL_DEV_KEY` | JWT config | Unchanged semantics | `JWT_SECRET` ≥32 bytes; keep `JWT_ALLOW_EPHEMERAL_DEV_KEY` off in production |
| `APP_BOOTSTRAP_ADMIN_*` | First-admin bootstrap | Unchanged; use once as secrets then disable | See §3.7 |
| `GEMINI_API_KEY`, `GROQ_API_KEY`, `AI_CHAT_MODEL`, `AI_EMBEDDING_MODEL`, `AI_ENABLED` | AI provider config | Move keys to the inference service; keep `AI_ENABLED` as master switch | See §7.5 |
| `LOG_LEVEL_BACKEND`, `LOG_LEVEL_SQL`, `LOG_FILE`, `SERVER_CONTEXT_PATH`, `APP_API_VERSION`, `APP_API_BASE_PATH` | Misc | Unchanged | Review log file path on ephemeral Render filesystem (use stdout) |

### 5.2 Backend — proposed new names

| Name | Purpose |
|---|---|
| `SPRING_FLYWAY_ENABLED`, `SPRING_FLYWAY_LOCATIONS`, `SPRING_FLYWAY_BASELINE_ON_MIGRATE`, `SPRING_FLYWAY_VALIDATE_ON_MIGRATE` | Flyway configuration (or set in `application.yml`) |
| `DATABASE_URL` / `PGSSLMODE` | Optional platform-standard aliases if Render/Supabase integration supplies them; prefer the existing `DB_*` names to avoid churn |
| `AI_INFERENCE_SERVICE_URL`, `AI_INFERENCE_SERVICE_TOKEN` | Server-to-server calls to the Python inference service |
| `SUPABASE_URL`, `SUPABASE_ANON_KEY`, `SUPABASE_SERVICE_ROLE_KEY` | Only if Supabase client-side/Storage/Auth SDKs are adopted; **never** expose a service-role key to clients |

### 5.3 Client applications

| App | Names | Production target |
|---|---|---|
| Customer Flutter | `API_BASE_URL`, `WS_BASE_URL`, `IS_DEVELOPMENT` | `<RENDER_HTTPS_URL>/api/v1`, `wss://<RENDER_HOST>/ws/tracking`, `false` — supplied via `--dart-define` at build time |
| Rider Flutter | `API_BASE_URL`, `WS_BASE_URL`, `IS_DEVELOPMENT` | Same, built as a separate app ID |
| Admin web (Vercel) | `VITE_API_BASE_URL`, `VITE_WS_BASE_URL`, `VITE_REQUEST_TIMEOUT`, `VITE_IS_DEVELOPMENT` | `<RENDER_HTTPS_URL>/api/v1`, `wss://...`, `false` — set **before** `vite build` (baked into bundle) |

**Security rules:** `.env` stays git-ignored; no secret in any `VITE_*` or
Flutter `--dart-define` value; `VITE_*` values are public by definition;
`JWT_SECRET`, DB passwords and AI keys live only in Render/Supabase secret
stores. `git status` must never show a staged `.env`.

---

## 6. Supabase setup instructions

### 6.1 Project setup

1. Create a **fresh** Supabase project (no existing data is imported — the old
   MySQL records are explicitly not required).
2. Record the project reference, database host(s), and the generated database
   password in the team secret store. Never commit them.
3. Confirm the database encoding is `UTF8` (Supabase default) — required for
   Bangla script and emoji.
4. Decide the migration mechanism (§3.8): Flyway in the app (recommended) or
   Supabase CLI migrations. Create the `schema_migrations`/Flyway history table
   automatically via the first migration.
5. Enable required extensions in the SQL editor (verify availability on the
   chosen plan): `pgcrypto`/`gen_random_uuid()` (UUID generation), `pgvector`
   (future `product_embeddings`), `pg_stat_statements` (monitoring).

### 6.2 Region selection

Choose the **closest region Supabase currently offers to the Bangladesh user
base** (South/Southeast Asia) to minimise latency for the Flutter apps and the
admin console, subject to the region list available on the chosen plan at
creation time. Keep Render in a nearby region where possible to reduce
backend↔database round trips. Region cannot be changed after creation without
a project migration, so decide before provisioning. Data-residency/compliance
requirements should be confirmed by the team.

### 6.3 Session Pooler vs direct connection

Supabase exposes several connection strings; the decision matters here:

| Connection | Port | Use for | Notes |
|---|---|---|---|
| **Direct** (`db.<ref>.supabase.co`) | 5432 | **Migrations** (Flyway/`psql`/Supabase CLI), admin tasks, and any session-state-heavy work | Full PostgreSQL feature set; verify Render's outbound IPv4/IPv6 support (Supabase direct hosts may be IPv6-only; the pooler provides IPv4) |
| **Session Pooler** | 5432 | **Recommended for the Render Spring Boot app** | pgBouncer in session mode preserves session state; compatible with Hikari's persistent connections and `FOR UPDATE` |
| **Transaction Pooler** | 6543 | Serverless/short-lived clients only | Transaction mode does **not** preserve session state; requires `?pgbouncer=true` and `prepareThreshold=0`; can break prepared statements, `SET`, and pessimistic locking. **Avoid for this app.** |

**Decision:** use the **Session Pooler for the application** (or direct if
networking permits) and the **direct connection for migrations**. Because
`RefreshTokenRepository` uses `@Lock(PESSIMISTIC_WRITE)`, avoid transaction-mode
pooling. Add `?pgbouncer=true` only if the transaction pooler is ever chosen.

### 6.4 SSL configuration

- Supabase enforces TLS: set `sslmode=require` (or `verify-full` with the
  Supabase CA certificate mounted in Render's trust store) in `DB_URL`.
- Never set `useSSL=false` (the old MySQL dev default) in production.
- Confirm the Render JVM trust store validates the certificate chain; test the
  handshake from a Render shell before the first deploy.

### 6.5 Database roles and permissions

Do **not** connect the app as the Supabase `postgres` superuser. Create a
least-privilege application role, e.g. `<DB_APP_ROLE>`:

```sql
-- Run as an administrator in the Supabase SQL editor.
CREATE ROLE <DB_APP_ROLE> LOGIN PASSWORD '<DB_APP_PASSWORD>';
GRANT USAGE ON SCHEMA public TO <DB_APP_ROLE>;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO <DB_APP_ROLE>;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO <DB_APP_ROLE>;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO <DB_APP_ROLE>;
-- No CREATEDB, no SUPERUSER, no BYPASSRLS.
```

- Use a separate **migration role** with DDL rights for Flyway/Supabase CLI,
  used only during deploys.
- Confirm `PUBLIC` cannot `CREATE` in the `public` schema (Supabase PG 15+
  already restricts this); revoke if needed.
- Enable Row Level Security only if Supabase client-side data access is ever
  introduced — the current architecture is backend-only, so RLS is not needed
  for the app role now.
- Grant `EXECUTE` on any `pgcrypto`/`pgvector` functions the app needs.

### 6.6 Connection limits

- Supabase plans cap concurrent connections; size Hikari accordingly:
  `DB_POOL_MAX_SIZE` small (single-digit to ~10), `DB_POOL_MIN_IDLE` low,
  `DB_POOL_CONNECTION_TIMEOUT` bounded (already 5000 ms), and
  `max-lifetime`/`idle-timeout` below the pooler's idle timeout (currently
  `max-lifetime: 1800000`, `idle-timeout: 600000`). Consider enabling
  Hikari keepalive.
- With multiple Render instances, the **aggregate** pool size across all
  instances must stay under the plan limit.
- Keep `initialization-fail-timeout: -1` so a transient DB outage does not
  prevent the app from booting; `/api/v1/health` will honestly report
  `database: DOWN`.
- Enable Supabase connection-pooler/DB metrics and alert on connection
  saturation.

### 6.7 Backups, extensions and preview databases

- Enable daily backups and Point-in-Time Recovery where the plan allows; test
  a restore into a fresh project (see §3.9).
- Use Supabase **branch/preview databases** (or a separate staging project)
  for CI to run migrations and acceptance tests before production.
- Install `pgvector` ahead of Goal 12 if embeddings will live in the same DB.

---

## 7. Cloud deployment specifications

### 7.1 Supabase

Covered in §6 (project, region, pooler, SSL, roles, limits). Deliverable
sequencing: project + roles + extensions exist **before** the first Render
deploy.

### 7.2 Render (backend)

| Item | Specification |
|---|---|
| Service type | Web Service (Native Java environment, or a small `Dockerfile` using an Eclipse Temurin 21 base image) |
| Runtime | **Java 21** pinned explicitly (e.g. `JAVA_VERSION` / `jdk` setting). Render's default JDK may differ — do not rely on it. |
| Build command | `mvn -B -DskipTests clean package` (the repo has **no Maven wrapper**; Render provides Maven). Optional future improvement: commit a Maven wrapper (`mvnw`) and use `./mvnw -B -DskipTests clean package`. |
| Artifact | `backend/target/bd-quickmart-backend-0.1.0.jar` (from `pom.xml` `<artifactId>bd-quickmart-backend</artifactId>`, `<version>0.1.0</version>`) |
| Start command | `java -jar target/bd-quickmart-backend-0.1.0.jar` |
| Port | Render injects `PORT`; the app binds via `server.port: ${SERVER_PORT:8080}`. **Set `SERVER_PORT` to the Render `PORT` value** (or change the placeholder to `${PORT:8080}`). This is a required wiring step. |
| Environment variables (names only) | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `DB_DRIVER`, `DB_DDL_AUTO`, `DB_DIALECT`, `DB_POOL_*`, `JWT_SECRET`, `JWT_ISSUER`, `JWT_ACCESS_EXPIRATION`, `JWT_REFRESH_EXPIRATION`, `JWT_ALLOW_EPHEMERAL_DEV_KEY`, `APP_CORS_ALLOWED_ORIGINS`, `APP_API_*`, `AI_*`, `APP_BOOTSTRAP_ADMIN_*` (one-off), `SERVER_PORT`, `LOG_*` |
| Secrets | Store `DB_PASSWORD`, `JWT_SECRET`, AI keys and bootstrap password as Render secret env vars. Never in `render.yaml` or Git. |
| Health-check path | `/api/v1/health` for **liveness** (always HTTP 200 when the app is up). Use `/actuator/health` for **readiness/monitoring** (returns 503 when the DB is down via the DataSource health indicator) — decide deliberately to avoid restart loops. |
| Database connectivity | Supabase Session Pooler (or direct) URL with `sslmode=require`; Hikari tuned per §6.6; `PostgreSQLDialect`; Flyway migrations applied before/at startup. |
| Migration sequencing | (1) Supabase ready; (2) run `V1__...` migrations via a Render pre-deploy command (if available on the chosen plan), a one-off job, or Flyway at startup with `ddl-auto: validate`; (3) deploy backend; (4) health check; (5) one-off SUPER_ADMIN bootstrap; (6) then Vercel/Flutter. Flyway's lock makes concurrent-instance migrators safe. |
| Logs | Prefer stdout/stderr (Render captures it); `LOG_FILE` points at a local path on an ephemeral filesystem — set `LOG_FILE` to an empty/stdout configuration or accept non-persistent file logs. |
| WebSockets | `/ws/tracking` works on a single Render instance. If scaled to multiple instances, add a shared backplane (e.g. Redis) for tracking broadcasts — future Goal 08 concern. |
| Rollback | Render deploy rollback to the previous release; keep schema changes expand/contract compatible (§3.9). |

A `render.yaml` Blueprint (or equivalent dashboard configuration) should encode
the runtime, build/start commands, plan, region, health-check path and the
**names** of the environment variables; values are entered as secrets in the
Render dashboard.

### 7.3 Vercel (admin web)

| Item | Specification |
|---|---|
| Framework preset | Vite |
| Root directory | `admin-web` |
| Install command | `npm ci` (uses `admin-web/package-lock.json`) |
| Build command | `npm run build` (`tsc -b && vite build`) |
| Output directory | `dist` |
| Node version | 22.x (matches the verified local toolchain) |
| Environment variables (names only) | `VITE_API_BASE_URL`, `VITE_WS_BASE_URL`, `VITE_REQUEST_TIMEOUT`, `VITE_IS_DEVELOPMENT` — set **before build**, they are baked into the bundle |
| Production API base URL | `https://<RENDER_HOST>/api/v1` (HTTPS) and `wss://<RENDER_HOST>/ws/tracking` |
| SPA fallback | `vercel.json` rewrite, e.g. `{ "rewrites": [{ "source": "/(.*)", "destination": "/index.html" }] }` — required because `createBrowserRouter` uses the History API and direct loads of `/orders`, `/riders`, etc. would otherwise 404 |
| CORS requirements | Add the Vercel production origin to `APP_CORS_ALLOWED_ORIGINS`. `WebConfig` uses `setAllowCredentials(true)` with an explicit origin list — `*` is not permitted. Verify `OPTIONS` preflight and the `X-Trace-Id` exposed header. |
| Security headers (optional) | HSTS, CSP, `X-Content-Type-Options` via `vercel.json` `headers` |
| Hardening (separate goal) | Migrate the `sessionStorage` refresh token to a Secure/HttpOnly/SameSite cookie (§1.8, §9) |

### 7.4 Flutter (customer & rider apps)

**Production API URL configuration**
- Build-time: `flutter build apk --release --dart-define=API_BASE_URL=<RENDER_HTTPS_URL>/api/v1 --dart-define=WS_BASE_URL=wss://<RENDER_HOST>/ws/tracking --dart-define=IS_DEVELOPMENT=false` (and the iOS equivalent). `AppConfig` uses `String.fromEnvironment`, so the URL **must** be supplied at build time; there is no runtime env file in the shipped app.
- Keep the `ServerConfig` runtime "Change server URL" override for QA, but consider disabling/hiding it in release builds so an end user cannot repoint the app.
- Use HTTPS/`wss://` only. Release Android manifests already omit `usesCleartextTraffic` (it is `debug`-only), so cleartext HTTP is correctly blocked. Never ship a private LAN IP or the emulator-only `10.0.2.2` default.

**Customer app deployment configuration**
- Release signing: configure `android/app/build.gradle.kts` signingConfigs with a keystore whose credentials come from CI secrets; never commit the keystore or passwords. Build an `.aab` for Play / `.apk` for internal distribution.
- Application ID `com.bdquickmart.customer_app` (distinct from rider).
- Distribution: Google Play internal testing / Firebase App Distribution / direct APK as the team chooses.

**Rider app deployment configuration**
- Same process, with its own signing config and application ID
  `com.bdquickmart.rider_app`. Rider-specific flows: assignment, availability,
  earnings, "awaiting approval" (PENDING) screen.

**Physical-device verification**
- Install the **release** build on a real Android (and iOS, if targeted) device
  connected over **cellular/public internet** (not the LAN) to prove the public
  Render URL is reachable.
- Verify register/login/refresh/logout/me, silent token refresh on 401,
  role/status router guards (PENDING rider → awaiting-approval screen),
  Bangla/English rendering, the health endpoint, and a `wss://` WebSocket
  handshake.
- Verify the Vercel admin console in a real browser (CORS from the production
  origin, SPA deep links) and the rider-approval workflow end-to-end.

### 7.5 AI/ML (Shriful's scope)

| Item | Specification |
|---|---|
| Python inference service boundary | Separate containerised Python service (e.g. FastAPI + Uvicorn) deployed independently (Render / Cloud Run / Fly.io). It is the **only** component that calls Gemini/Groq and hosts trained models. The Spring monolith remains the sole public API. |
| Model training separation | Offline pipelines using `ai/` assets (`prompts/`, `evals/`, `models/`, `forecasting/`); versioned artifacts in a model registry; no training on the serving path. |
| Authenticated backend integration | Backend → inference service **server-to-server** over HTTPS with a service credential (mTLS or short-lived service JWT), using new `AI_INFERENCE_SERVICE_URL` / `AI_INFERENCE_SERVICE_TOKEN`. `AI_ENABLED` remains the master switch; with it off, the platform runs fully and makes no AI calls. Provider keys stay server-side only. |
| Data plane | `pgvector` for `product_embeddings` / `recommendation_cache`; `demand_forecasts` in PostgreSQL. Statistical model (XGBoost/LightGBM) produces numbers; LLM explains them (`ai/README.md` doctrine). |
| Model versioning & monitoring | Versioned endpoints, model version in every response, latency/accuracy/drift metrics, structured logs, Gemini→Groq fallback, documented rollback to the previous model version. |
| Ownership | Shriful owns training, evaluation, inference and integration; this plan only defines the boundary and the backend wiring points. |

---

## 8. Testing and acceptance plan

### 8.1 Pre-deploy (local / CI)

| Check | Command / action | Expected |
|---|---|---|
| Backend unit + integration | `cd backend && mvn -B clean verify` | BUILD SUCCESS; tests pass against PostgreSQL-mode H2 or Testcontainers PostgreSQL |
| Migration correctness | Apply `V1__...` to a fresh PostgreSQL instance, then start the app with `ddl-auto: validate` | No schema-validation errors; Flyway `validate` passes |
| Migration idempotency/forward-only | Re-run migrations; inspect `flyway_schema_history` | No duplicate application, no destructive statements |
| DB compatibility | Concurrency test for refresh rotation (`FOR UPDATE`), replay/family revocation, case-insensitive uniqueness (`lower(email)`/`lower(phone)`), `TIMESTAMPTZ` UTC round-trip, Bangla/emoji UTF8 round-trip, `NUMERIC(12,2)` precision | Behaviours identical to the MySQL baseline |
| Secret scan | `git diff --check`, verify no `.env`/keystore/secret staged, grep for committed keys | Clean |
| Customer app | `flutter analyze && flutter test && flutter build apk --debug` (and a release build with production `--dart-define`) | No issues, all tests pass, APK builds |
| Rider app | Same | Same |
| Admin web | `npm run lint && npm run build` | 0 warnings/errors, build succeeds |

### 8.2 Staging / production acceptance

1. **Supabase**: project reachable; `UTF8`; app role can CRUD but cannot DDL;
   SSL enforced; pool within limits; PITR/backups enabled.
2. **Render**: `GET /api/v1/health` → HTTP 200 with `database: UP`;
   `/actuator/health` → UP; `/v3/api-docs` and `/swagger-ui.html` reachable;
   OpenAPI server list shows the real Render URL; `PORT` binding correct;
   logs contain no secrets.
3. **Auth/RBAC end-to-end**: register CUSTOMER and RIDER (RIDER starts PENDING);
   login by email and phone; refresh rotation; replay detection revokes family;
   logout; `/me`; six-role authority matrix; PENDING rider denied
   `/api/v1/rider/**`; admin rider approve/reject; SUPER_ADMIN provisioning;
   exactly one SUPER_ADMIN exists and `APP_BOOTSTRAP_ADMIN_ENABLED` is off.
4. **CORS**: browser preflight and authenticated request from the Vercel origin
   succeed; `X-Trace-Id` exposed; requests from a disallowed origin fail.
5. **Vercel**: production bundle points at the Render URL; deep links
   (`/orders`, `/riders`) reload correctly via SPA fallback; silent refresh
   works; role guards behave.
6. **Flutter**: release builds on physical devices over cellular — full auth
   flow, router guards, Bangla rendering, `wss://` handshake, health check.
7. **Security**: no secret in Git, logs, or client bundles; `JWT_SECRET` ≥32
   bytes; `JWT_ALLOW_EPHEMERAL_DEV_KEY=false`; TLS everywhere; least-privilege
   DB role; actuator/info exposure reviewed; HttpOnly-cookie migration tracked
   as a follow-up.
8. **AI**: with `AI_ENABLED=false`, the platform is fully functional and makes
   no external AI calls (acceptance gate before Shriful's service is wired).

### 8.3 Sign-off gate

No implementation, deployment, commit or push occurs until the team explicitly
approves this plan. Each phase in §10 ends with its own verification gate.

---

## 9. Risks and mitigations

| # | Risk | Impact | Mitigation |
|---|---|---|---|
| 1 | **Case-sensitivity change** in unique `email`/`phone` constraints (PG default is case-sensitive vs `utf8mb4_unicode_ci`) | Duplicate accounts, login failures, changed API behaviour | Functional unique indexes on `lower(email)`/`lower(phone)`; keep `AuthService.normalizeEmail()`; add tests; verify phone normalisation |
| 2 | **Transaction-mode pooler** breaking `FOR UPDATE`, prepared statements, session state | Refresh rotation/replay detection fails silently | Use Session Pooler (or direct) for the app; `prepareThreshold=0` + `?pgbouncer=true` only if transaction mode is unavoidable; direct connection for migrations |
| 3 | **Supabase connection limits** exceeded by Hikari (esp. multiple instances) | Connection errors at runtime | Small pool, bounded timeouts, aggregate sizing, keepalive, monitoring/alerts |
| 4 | **Render `PORT` vs `SERVER_PORT`** mismatch | App binds the wrong port; health check fails; deploy fails | Set `SERVER_PORT` from Render's `PORT` or change the placeholder to `${PORT:8080}`; verify on first deploy |
| 5 | **Timezone drift** (`serverTimezone=Asia/Dhaka` removed) | Wrong timestamps in API/DB | `TIMESTAMPTZ` UTC, existing `hibernate.jdbc.time_zone=UTC` and injectable `Clock`; verify Jackson `Instant` serialisation and presentation rendering |
| 6 | **`ddl-auto: validate`** strictness | Startup failure on any schema drift | Flyway owns schema; run `validate` in CI against a migrated DB before enabling in production |
| 7 | **Flyway concurrent migrations** across instances | Corrupt/partial schema | Flyway's built-in lock; prefer single-instance deploys during migrations; or run migrations as a separate pre-deploy job |
| 8 | **SUPER_ADMIN bootstrap race** | Multiple or zero super admins | Single-instance provisioning, advisory lock or partial unique index, disable switch immediately, verify count |
| 9 | **Health-check semantics** | Restart loops or false "healthy" | `/api/v1/health` is liveness-only (always 200); use `/actuator/health` (503 on DB down) for readiness; choose deliberately per platform behaviour |
| 10 | **`sessionStorage` refresh token XSS** on Vercel | Token theft | Track HttpOnly/Secure/SameSite cookie migration as a separate hardening goal; CSP; short access TTL |
| 11 | **Vite `VITE_*` baked at build time** | Wrong API URL if set after build | Set variables in Vercel project settings before build; verify the built bundle; never store secrets there |
| 12 | **Missing SPA fallback** | 404 on deep links | `vercel.json` rewrites; test `/orders` reload |
| 13 | **CORS misconfiguration** | Browser admin console blocked | Add the exact Vercel origin to `APP_CORS_ALLOWED_ORIGINS`; explicit origins (no `*` with credentials); test preflight |
| 14 | **Flutter compile-time URL / cleartext** | Shipped app points at `10.0.2.2` or HTTP | Release `--dart-define` with HTTPS URL; confirm `usesCleartextTraffic` is debug-only; physical-device test over cellular |
| 15 | **Native `uuid` / `CHAR(n)` change** | Entity mapping or contract break | Keep `VARCHAR(36)` UUID strings initially; defer native `uuid` to an approved change with entity/DTO/JWT updates |
| 16 | **`DATETIME` → `TIMESTAMPTZ` mapping** | Hibernate type mismatch | Verify Hibernate 6 maps `Instant` to `timestamp with time zone`; add a round-trip test; keep `hibernate.jdbc.time_zone=UTC` |
| 17 | **ICU collation availability** for Bangla | Sort/search differences (Goal 04) | Confirm ICU collations on the chosen Supabase PG version; use deterministic collation + `lower()` indexes for uniqueness; document search design later |
| 18 | **Actuator exposure** (`show-details: always`, `info.env.enabled`) | Information disclosure | Restrict `/actuator/**` in production or reduce exposure; review `SecurityConfig` before go-live |
| 19 | **AI keys leaking into clients** | Key compromise | Server-side only; inference service boundary; secret scanning; never in `VITE_*`/`--dart-define` |
| 20 | **No legacy data migration** (by design) | Fresh DB has no roles/accounts | CHECK-constraint role definition + secure first-SUPER_ADMIN bootstrap + explicit seed/acceptance steps; never assume data exists |
| 21 | **Vendor/region/PITR constraints** | Latency, compliance, recovery gaps | Choose region before creation; confirm backup/PITR availability on the plan; keep `pg_dump` as an independent backup |
| 22 | **WebSocket scaling** (Goal 08) | Dropped tracking messages on multiple instances | Single instance initially; add a Redis/shared backplane when scaling |
| 23 | **Log file path on Render** | Lost/non-persistent logs | Prefer stdout logging; set `LOG_FILE` accordingly |

---

## 10. Recommended implementation order

> Every phase is gated on **explicit approval** of this plan. Nothing here is
> executed now. No commit or push at any phase until approved. The existing
> MySQL database is never deleted, reset, or modified.

| Phase | Work | Gate / exit criteria |
|---|---|---|
| 0 | **Approval of this plan** | Written approval from the developer/team |
| 1 | **Supabase foundation**: create fresh project, choose region, set `UTF8`, install extensions, create least-privilege app + migration roles, decide Session Pooler vs direct, enable SSL/backups, create a staging/preview DB | Connections verified from a Render shell; roles tested |
| 2 | **Backend dependency & config**: swap JDBC driver, add Flyway, update `application.yml` (URL/driver/dialect/`ddl-auto: validate`/Flyway/CORS/`PORT`), update `OpenApiConfig` production server URL, add new env var names to `.env.example` | `mvn -B clean verify` passes locally with no MySQL |
| 3 | **Versioned PG migrations**: author `V1__init_auth_schema.sql` (§3), optional `V2__` if the roles-table alternative is chosen, PG-flavoured verification/bootstrap scripts, retain legacy MySQL scripts | Fresh staging DB migrates cleanly; `flyway validate` passes |
| 4 | **Test strategy**: H2 `MODE=PostgreSQL` or Testcontainers PostgreSQL; add compatibility/migration/concurrency/case/UTF8/timestamp tests | All tests green; no dialect-specific assumptions remain |
| 5 | **Staging cutover**: apply migrations to staging, deploy backend with `ddl-auto: validate`, run health + auth/RBAC + CORS + WebSocket checks | Acceptance §8.2 items 1–4 pass on staging |
| 6 | **Render production deploy**: Blueprint/dashboard config, secrets, health check, migration sequencing, logs | `GET /api/v1/health` → `database: UP`; no secrets in logs |
| 7 | **Secure SUPER_ADMIN provisioning**: one-off bootstrap, immediately disable switch, verify exactly one account | §8.2 item 3 privileged flows pass |
| 8 | **Vercel admin-web**: `vercel.json` SPA fallback, production `VITE_*` values, CORS origin, deploy | Deep links + browser auth + CORS pass |
| 9 | **Flutter release builds**: production `--dart-define` URLs, release signing, distribute, physical-device verification over cellular | §8.2 item 6 passes for both apps |
| 10 | **AI/ML boundary (Shriful)**: Python inference service, `AI_INFERENCE_SERVICE_*` wiring, `pgvector`, model registry, monitoring — with `AI_ENABLED=false` acceptance gate first | §8.2 item 8 passes |
| 11 | **Final sweep**: update all documentation (`docs/`, `README.md`), secret scan, `git status`/diff review, acceptance evidence record, sign-off | Clean tree (no unapproved changes), documented acceptance |

---

## 11. Estimated complexity and dependencies

Complexity is expressed as **relative size** (Low / Medium / High), not a time
estimate.

| Component | Complexity | Primary drivers |
|---|---|---|
| Supabase foundation (§6) | Medium | Region decision (irreversible), pooler vs direct, roles/permissions, SSL, extensions, backups |
| Backend driver/config swap (§4 #1–2, #8–10) | Low | Portable entities mean few code changes; mostly config + one dependency |
| Versioned PG migrations (§3, §4 #3–4, #13–14) | Medium | DDL rewrite, collation/case-insensitivity decision, verification-script rewrite, Flyway integration |
| Test strategy update (§4 #11–12) | Medium | Switching H2 mode or adopting Testcontainers; new compatibility tests |
| Render backend deploy (§7.2) | Medium | Java 21 pinning, `PORT` wiring, secrets, health-check semantics, migration sequencing |
| First-SUPER_ADMIN provisioning (§3.7) | Low | Reuse existing runner; add race mitigation + disable discipline |
| Vercel admin-web (§7.3) | Low–Medium | Build-time env vars, SPA fallback, CORS origin, XSS hardening deferred |
| Flutter production builds (§7.4) | Medium | Release signing, compile-time URLs, two app IDs, physical-device verification |
| AI/ML inference boundary (§7.5) | High | Separate service, model registry, authenticated integration, `pgvector`, monitoring; owned by Shriful |
| Documentation sweep (§4 #15–19) | Low–Medium | Many files, but mechanical |
| HttpOnly cookie hardening (§4 #25) | Medium | Separate security goal; changes auth flow on web only |

**Dependency / critical path:**

```
Phase 0 approval
   └─> Phase 1 Supabase foundation
         ├─> Phase 2 backend config ─> Phase 3 migrations ─> Phase 4 tests
         │                                   └─> Phase 5 staging cutover
         │                                         └─> Phase 6 Render prod
         │                                               ├─> Phase 7 SUPER_ADMIN
         │                                               ├─> Phase 8 Vercel (needs Render URL + CORS origin)
         │                                               └─> Phase 9 Flutter (needs Render URL)
         └─> Phase 10 AI/ML (independent of the DB cutover; needs backend wiring points
                             from Phase 2 and Shriful's ownership)
Phase 11 final sweep (depends on all above)
```

**External prerequisites / dependencies (to be provisioned by the team):**
- Supabase account + project (region, plan with required backup/PITR and IPv4
  pooler availability).
- Render account + Web Service (Java 21) and secret storage.
- Vercel account + project (Node 22).
- A production domain / TLS termination (Render and Vercel provide default
  HTTPS hostnames; a custom domain is optional).
- Android release keystores (and iOS signing if iOS is shipped) held in CI
  secrets; Google Play / App Store / distribution accounts.
- Shriful's ownership of AI/ML training, evaluation, inference and integration
  (a hard dependency for Phase 10 only — the rest of the migration does **not**
  depend on AI/ML).
- Team decisions required before implementation: pooled connection choice
  (§6.3), migration tool (§3.8), roles as CHECK vs lookup table (§3.6),
  case-insensitivity approach (§3.1), health-check path semantics (§7.2), and
  whether to adopt Testcontainers PostgreSQL (§8).

**What does *not* change:** the REST API contract (`/api/v1`, response envelope,
error codes), JWT claims and signing model, six-role RBAC and authorities,
Flutter/admin-web feature behaviour, and the existing MySQL database (preserved
untouched). Because the two JPA entities and all repository queries are already
database-portable, the application-layer blast radius is small; the substantive
work is in the DDL/migrations, connection/configuration, collation semantics,
and deployment wiring.

---

## Completion

This report is the **only** artefact produced for the cloud-migration planning
task. No source code, dependency, migration file, database, deployment, Git
state, or other file was changed. Per the completion rule, work stops here:
**implementation must wait for explicit approval.**
