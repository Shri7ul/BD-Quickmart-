# BD QuickMart — Technology Stack

The stack is fixed by the project brief. This document records the exact
versions in use, why each choice was made, and the constraints that apply to
each.

---

## 1. Version matrix

| Layer | Technology | Version | Status |
|---|---|---|---|
| Backend runtime | Java (Eclipse Temurin) | 21.0.13 LTS | Verified |
| Backend framework | Spring Boot | 3.5.6 | Verified |
| Build tool | Apache Maven | 3.9.9 | Verified |
| Persistence | Spring Data JPA / Hibernate | Managed by Spring Boot 3.5.6 | Verified |
| Database | PostgreSQL (Supabase) | 16 | Fresh schema; Flyway-managed |
| JDBC driver | `org.postgresql:postgresql` | 42.7.7 (Boot BOM) | Verified |
| Migrations | Flyway (`flyway-core` + `flyway-database-postgresql`) | 11.7.2 (Boot BOM) | Separate migration step |
| API documentation | Springdoc OpenAPI (WebMVC UI) | 2.8.13 | Verified |
| Security | Spring Security | Managed by Spring Boot 3.5.6 | Verified |
| JWT library | JJWT (api / impl / jackson) | 0.12.6 | Verified (Goal 02) |
| Fast test database | H2 in `MODE=PostgreSQL` (test scope) | Spring Boot managed (2.3.232) | Verified |
| Real-PostgreSQL tests | Testcontainers `postgresql` + `junit-jupiter` (test scope) | Spring Boot managed | Skips without Docker |
| MySQL driver | — | **Removed** | No active MySQL runtime dependency |
| Real-time | Spring WebSocket | Managed by Spring Boot 3.5.6 | Verified |
| Customer app | Flutter | 3.35.1 | Verified |
| Rider app | Flutter | 3.35.1 | Verified |
| Dart SDK | (bundled with Flutter) | 3.9.0 | Verified |
| Flutter state management | provider | ^6.1.5 | Verified (Goal 02) |
| Flutter secure storage | flutter_secure_storage | ^9.2.2 | Verified (Goal 02) |
| Flutter settings storage | shared_preferences | ^2.5.5 | Verified (Goal 02) |
| Flutter HTTP | Dio | ^5.7.0 | Verified |
| Flutter routing | go_router | ^14.6.2 (14.8.1 resolved) | Verified |
| Admin web | React | 19.3.1 | Verified |
| Admin web language | TypeScript | ~7.0.2 | Verified |
| Admin web build | Vite | 8.3.6 | Verified |
| Admin web routing | React Router DOM | ^7.18.5 | Verified |
| Admin web HTTP | Axios | ^1.21.0 | Verified |
| Node.js | Node | 22.22.2 | Verified |

---

## 2. Backend

### Java 21 LTS

Chosen for virtual threads, which matter for a platform that will hold
WebSocket connections open for live tracking alongside ordinary REST traffic.
It is also the current LTS release, so it stays supported for years.

### Spring Boot 3.5.x

The Spring Boot 3.x line requires Java 17 or newer and Jakarta EE APIs, which
the project needs anyway. Version 3.5.6 is the current 3.5.x patch.

### Maven over Gradle

Both would work. Maven was chosen because its configuration is declarative and
its defaults are predictable across machines, which matters more than build
speed for a project of this size.

### Spring Data JPA / Hibernate

Chosen because the domain is relational and heavily transactional: orders must
update inventory atomically, and stock updates must be safe under concurrency
(Goal 18). JPA's transaction management and optimistic locking support that
directly.

**Note on `ddl-auto`:** pinned to `none`. Hibernate never creates or alters
tables. Schema ownership belongs to versioned **Flyway** migrations
(`V1__init_auth_schema.sql`), applied as a separate deployment step by a
DDL-capable role. The running application has `spring.flyway.enabled=false`
and `ddl-auto: none`; set `DB_DDL_AUTO=validate` in CI/staging to assert
entity/schema alignment.

**Note on the Hibernate dialect:** pinned explicitly to
`org.hibernate.dialect.PostgreSQLDialect`. Without it, Hibernate tries to read
dialect metadata over a live JDBC connection and fails to start when the
database is unreachable. Pinning it lets the backend boot and report database
state honestly through `/api/v1/health` (liveness) and
`/actuator/health/readiness` (readiness, DB-aware) instead of crashing. The
old MySQL `DB_DIALECT_VERSION` server-version workaround has been removed.

### Spring Security

Present from the start so that the security posture is deliberate rather than
retrofit. Goal 01 shipped a stateless, CSRF-free baseline; **Goal 02 completed
the chain**: `SessionCreationPolicy.STATELESS`, a `JwtAuthenticationFilter`
that validates HS256 bearer tokens and rebuilds authorities from the database
on every request, JSON-writing `AuthenticationEntryPoint` / `AccessDeniedHandler`
for filter-level 401/403 responses, and literal authority gates. A single
`CorsConfigurationSource` bean registered for `/**` ensures CORS headers are
present even on filter-level errors (MVC `addCorsMappings` could not do this).
There is no `RoleHierarchy` and no `@PreAuthorize`: every gate is an explicit
authority, so least privilege is visible in one file.

JWTs are issued/parsed with JJWT 0.12.6 against a `SecretKey` bean built from
`JWT_SECRET` (Base64 or raw, ≥32 bytes). There is no hardcoded secret; a blank
one fails fast outside `dev`/`test`. An injectable `java.time.Clock` makes JWT
timestamps and JPA audit times deterministic in tests. Refresh tokens are
stored only as SHA-256 hashes and rotated with family-based replay detection.

### Springdoc OpenAPI

Chosen over hand-written documentation because it derives the contract from the
code, so the spec cannot drift from the implementation. Serves OpenAPI 3.1 at
`/v3/api-docs` and Swagger UI at `/swagger-ui.html`.

### Spring WebSocket

Chosen over Server-Sent Events because live tracking needs bidirectional
traffic: the rider app sends location updates, the customer app receives them.
Registered at `/ws/tracking`.

---

## 3. PostgreSQL 16 via Supabase

The active database is a **fresh Supabase PostgreSQL 16** project, migrated
from the original local MySQL 8 / XAMPP baseline with **no data carried over**.
PostgreSQL is the approved cloud target: managed backups/PITR, TLS by default,
and a first-class fit for the versioned Flyway migration workflow and Render
hosting. `UTF8` encoding (Supabase default) stores Bangla script and emoji
correctly.

The connection strategy matters:

| Connection | Port | Used by |
|---|---|---|
| Session Pooler | `5432` | Running Spring Boot app — preserves session state and `SELECT ... FOR UPDATE` |
| Direct (`db.<ref>.supabase.co`) | `5432` | Flyway migrations / admin tasks |
| Transaction Pooler | `6543` | **Avoid** — breaks session state and pessimistic locking |

All connections require `sslmode=require`. Two roles are used: a
least-privilege DML-only runtime role and a separate DDL-capable migration
role. The application never runs migrations at startup
(`spring.flyway.enabled=false`), so a Supabase outage cannot block startup.

Because MySQL's `utf8mb4_unicode_ci` was case- **and** accent-insensitive while
PostgreSQL's default collation is neither, case-insensitive uniqueness is
defined **explicitly** with functional unique indexes on `lower(email)` /
`lower(phone)`; accent-insensitive behaviour is deliberately not implemented.
The original MySQL scripts are retained under
`infrastructure/database/legacy/mysql/` as a historical, DO-NOT-RUN
reference. Real PostgreSQL behaviour is validated with
Testcontainers (`PostgresMigrationIntegrationTest`) in CI.

---

## 4. Flutter (both mobile apps)

### Flutter over React Native

Chosen for rendering consistency across a fragmented Android device base
common in Bangladesh, and for a single Dart codebase covering both Android and
iOS from one codebase.

### provider + ChangeNotifier for state management (Goal 02)

Goal 01 documented Riverpod as an intended choice but shipped no state
management dependency at all. Goal 02 implemented the session with `provider`
(`^6.1.5`) and a `ChangeNotifier`-based `AuthController`. This keeps the
dependency surface small and lets the same notifier double as go_router's
`refreshListenable`, so a single redirect re-evaluates on every session
change. The correction is intentional: the documentation now matches the
code rather than the earlier plan. Over Bloc because the auth flow has no
event-stream complexity to justify the extra indirection.

Tokens are persisted separately: `flutter_secure_storage` (`^9.2.2`, Android
Keystore / iOS Keychain) for the raw token pair, and `shared_preferences`
(`^2.5.5`) only for the non-sensitive runtime server-URL override. Both are
behind interfaces so tests swap in in-memory implementations without touching
platform channels. `flutter_secure_storage` is pinned to the Java-11-compatible
9.x line because both apps' Android Gradle configs target JVM 11.

### go_router for navigation

Chosen over `Navigator 1.0` because it gives named routes, typed parameters
and declarative redirects — all needed for the guarded flows that authentication
(Goal 02) introduces.

### Dio over `http`

Chosen for interceptors, which is exactly where the bearer token, the trace id
and the response-envelope unwrapping belong. One interceptor location means
auth headers cannot be forgotten on a new endpoint.

### flutter_localizations with a custom delegate

Chosen so the translation table is a single reviewable class today and can
move to ARB files later without changing call sites.

### Distinct applications, not one with a flag

The rider app is a separate package with its own entry point, theme, router,
API client, localization table and domain model. A single codebase with a
role flag would couple the two products, ship customer-only code to riders,
and make it hard to give the rider app a different look and performance
profile.

The rider app is scaffolded as its own package so that location-sensitive
dependencies (for example `geolocator` and `connectivity_plus`) can be added
for its delivery workflow in a later goal without pulling them into the
customer app. Neither plugin is a dependency yet; both apps currently share
the same foundation dependency set.

---

## 5. React + TypeScript (admin web)

### React 19

The standard choice for a React console, with improved async rendering.

### TypeScript

Chosen because the admin console is the surface most likely to handle
heterogeneous backend data — order status unions, role enums, inventory
levels. A strict union such as `OrderStatus` catches a typo at compile time
that would otherwise surface as a wrong badge in production.

### Vite over Webpack

Chosen for fast dev-server startup and a simpler configuration surface. The
production build takes ~140ms on this project.

### React Router 7

Chosen for declarative, nested routing that matches the sidebar-plus-content
layout, and for the data-loading primitives it will serve once modules begin
fetching real data.

### Axios

Chosen for interceptors, matching the backend client's interceptor-based
design so that auth headers and envelope unwrapping are handled identically on
both sides.

### Admin session handling (Goal 02)

The access token is memory-only; the refresh token lives in `sessionStorage`.
A single-flight 401 refresh queue shares one refresh future across concurrent
requests and retries each failed request exactly once. Unauthenticated auth
calls use a bare Axios instance with no interceptors so a 401 from
`/auth/login` or `/auth/refresh` cannot trigger a refresh loop. Route guards
(`RequireAuth`, `RequireRole`) gate the console by role.

This is **not** HttpOnly-cookie protection: a token in `sessionStorage` is
readable by any script on the page and is therefore XSS-exposed. The trade-off
is documented in `admin-web/src/features/auth/session.ts`; a cookie-based
session is deferred to a later hardening goal. TypeScript stays strict
(`verbatimModuleSyntax`, `erasableSyntaxOnly`, `noUnusedLocals`), using
`import type` and no enums or path aliases.

---

## 6. AI providers

| Provider | Role | Model |
|---|---|---|
| Gemini | Product recommendation, chatbot, insight generation | `gemini-2.5-flash` (configurable) |
| Groq | Low-latency inference, chatbot fallback | configurable |

The API keys are environment variables, blank by default, with `AI_ENABLED`
defaulting to `false`. The platform runs fully without them.

**An important architectural constraint:** Gemini and Groq are language models.
They are appropriate for explaining data, generating insights, and driving the
conversational shopping assistant. They are **not** appropriate as the
quantitative engine for demand forecasting — a language model does not
reproduce the statistical reliability of a forecasting model trained on
historical sales. Goal 10 therefore stages the forecasting work: moving
averages first, then LLM-assisted analysis, then XGBoost/LightGBM, with the
statistical model as the source of truth and the LLM used to explain it.

---

## 7. Version control

Git on GitHub, one repository holding all four applications. Each application
is independently runnable and independently buildable, but a single repository
keeps the API contract and the three clients in one reviewable unit — which is
what makes a monorepo the right fit for a modular monolith.

---

## 8. Rules that follow from this stack

1. **No secrets in the repository.** Environment variables only, `.env` git-ignored,
   `.env.example` committed.
2. **No premature infrastructure.** No broker, no orchestrator, no service mesh.
   The stack grows when a goal requires it.
3. **The schema is migration-managed.** The repository provides a versioned
   Flyway migration and documentation; the running application never performs
   DDL. Migrations are a separate step executed by a dedicated DDL-capable
   role, never by the runtime role.
4. **One version of the API contract.** The OpenAPI document is generated from
   the backend and is the single source of truth for all three clients.
5. **Honest empty states.** A placeholder screen states which goal fills it.
   The admin dashboard shows `—` rather than invented numbers.
