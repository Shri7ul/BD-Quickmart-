# BD QuickMart — Project Progress

> **Last updated:** 2026-11-14 (autonomous delivery run)
> **Branch:** `main`
> **Method:** completion is measured by **verified functionality**, not code
> volume. Every claim below is backed by an actual command result from this
> run. `BUILT` ≠ `TESTED` ≠ `DEPLOYED`.

## Overall completion: **79%**

The repository (backend, migrations, both Flutter apps, admin web, deployment
config and docs) is implemented, tested and built to the maximum extent the
environment allows. The remaining ~21% is dominated by two **hard blockers** that
cannot be resolved locally: **(1)** no authenticated cloud deployment
(Render/Vercel) and **(2)** no live PostgreSQL verification — the provided
Supabase credential does not authenticate and Docker (for Testcontainers) is
unavailable. No production database was modified.

### Weighted breakdown

| Area | Weight | Verified | Status | Weighted |
|---|---|---|---|---|
| Backend | 20% | 90% | TESTED | 18.0% |
| Supabase migration | 20% | 75% | IMPLEMENTED (live verify BLOCKED) | 15.0% |
| Customer app | 12% | 90% | TESTED + BUILT | 10.8% |
| Rider app | 12% | 90% | TESTED + BUILT | 10.8% |
| Admin Web | 12% | 85% | BUILT | 10.2% |
| Deployment | 12% | 60% | IMPLEMENTED (deploy BLOCKED) | 7.2% |
| Testing / E2E | 12% | 60% | PARTIAL | 7.2% |
| **Total** | **100%** | | | **79.2% → 79%** |

### Status legend

- **IMPLEMENTED** — code/config/docs written and reviewed.
- **TESTED** — automated tests pass.
- **BUILT** — a production build artifact is produced.
- **DEPLOYED** — a running, reachable deployed application was verified.
- **BLOCKED** — an external prerequisite prevents further progress.

No component is marked **DEPLOYED**: no authenticated cloud tools were
available and no deployed URL was verified.

---

## 1. Backend — 90% — TESTED

Spring Boot 3.5.6 / Java 21. `mvn -B clean verify` → **BUILD SUCCESS,
56 tests, 0 failures, 0 errors, 9 skipped** (the 9 are real-PostgreSQL
Testcontainers tests, skipped because Docker is unavailable).

- Registration / login, JWT access + refresh, **refresh rotation + replay
  rejection**, logout / token revocation, `/me` — full-stack MockMvc integration
  tests (`AuthIntegrationTest`, 23 tests).
- Six canonical roles, rider `PENDING/APPROVED/REJECTED` workflow, admin
  authorization, privileged provisioning — covered by the same suite.
- CORS (`WebConfig`/`SecurityConfig`), WebSocket auth (`/ws/**` permitted, JWT
  filter), liveness (`/api/v1/health`, `/actuator/health/liveness`) vs readiness
  (`/actuator/health/readiness`), Render `PORT` binding
  (`${PORT:${SERVER_PORT:8080}}`), production-safe config (no secrets in yml,
  fail-fast JWT, ephemeral-dev-key off) — implemented and reviewed.
- `RealHttpSmokeTest` starts a real embedded server and hits real HTTP endpoints.
- `UserEntity` / `RefreshTokenEntity` use portable JPA annotations only; the
  MySQL→PostgreSQL move needs no entity change.

**Why not 100%:** full end-to-end auth/RBAC against a **live PostgreSQL** was not
run (see blockers); WebSocket connectivity was not exercised against a live
deployed server.

## 2. Supabase migration — 75% — IMPLEMENTED (live verification BLOCKED)

- PostgreSQL JDBC driver (`org.postgresql:postgresql`), Flyway (`flyway-core`,
  `flyway-database-postgresql`) + out-of-band `flyway-maven-plugin`; MySQL
  connector and `MODE=MySQL`/`MySQLDialect` removed from active config.
- **Complete fresh schema** in `V1__init_auth_schema.sql`: `users`,
  `refresh_tokens`, identity PKs, 7 indexes (incl. functional `lower(email)` /
  `lower(phone)` unique indexes and the partial live-family index), CHECK
  constraints for six roles / four states, FK, `TIMESTAMPTZ` UTC columns, soft
  deletes, case-insensitive uniqueness. Structurally validated by
  `FlywayMigrationSqlTest` (always-on).
- Runtime/migration **privilege separation**: `spring.flyway.enabled=false` by
  default, dedicated `DB_MIGRATION_*` for DDL, DML-only runtime role, prepared
  least-privilege role SQL (`infrastructure/database/postgres/`).
- **NEW this run** — the provided `SUPABASE_DATABASE_URL` was not consumed by
  anything. Added `SupabaseDatabaseUrl` (URI parser: percent-decodes
  URL-encoded passwords, enforces `sslmode=require`, keeps credentials out of
  the JDBC URL) + `DatabaseUrlEnvironmentPostProcessor` (wires a single URI into
  Spring's DataSource at startup, authoritative over stale `DB_*`). Documented
  in `.env.example`, `environment-variables.md`, `render.yaml`, `DEPLOYMENT.md`.
  Unit-tested: `SupabaseDatabaseUrlTest` (7) + `DatabaseUrlEnvironmentPostProcessorTest` (5).
- Obsolete MySQL config/deps/docs removed; legacy MySQL scripts moved to
  `infrastructure/database/legacy/mysql/`.

**Why not 100%:** the schema was **not applied to Supabase** and **no live
PostgreSQL verification** was obtained. Per §8, applying DDL/changing roles on a
hosted production database and permission changes are deliberately treated as
requiring approval; additionally the supplied credential fails authentication.
The `PostgresMigrationIntegrationTest` (real PG via Testcontainers) is present
and well-designed but **skipped** — Docker is unavailable. PostgreSQL
compatibility is therefore supported by structural SQL validation and H2
`MODE=PostgreSQL` tests **only**; it is explicitly **NOT** claimed as verified
on a live PostgreSQL server.

## 3. Customer app — 90% — TESTED + BUILT

- `flutter pub get` OK; `flutter analyze` → **No issues found**;
  `flutter test` → **14 tests passed**; `flutter build apk --release` →
  **`customer-app/build/app/outputs/flutter-apk/app-release.apk` (47.1 MB)**.
- Registration, login, secure token storage, session restoration, silent
  single-flight refresh, logout, error handling, API integration — covered by
  tests and existing implementation.
- Production HTTPS config: `app_config.dart` release defaults are
  non-routable `.invalid` hosts (fail-closed), cleartext disabled in release.

**Why not 100%:** release APK was built with a **placeholder** production URL
(no backend is deployed yet), so no physical-device / cellular E2E against a
real Render URL was possible.

## 4. Rider app — 90% — TESTED + BUILT

- `flutter pub get` OK; `flutter analyze` → **No issues found**;
  `flutter test` → **16 tests passed**; `flutter build apk --release` →
  **`rider-app/build/app/outputs/flutter-apk/app-release.apk` (47.1 MB)**.
- PENDING / approved / rejected behaviour, secure storage, session restore,
  refresh, logout, production API config — implemented/tested.

**Why not 100%:** same placeholder-URL / no-live-E2E caveat as the customer app.

## 5. Admin Web — 85% — BUILT

- `npm run lint` (oxlint) → **0 warnings, 0 errors**; `npm run build`
  (`tsc -b && vite build`) → **success** (`admin-web/dist`, 98 modules).
- Login, session handling, protected routes (`RequireAuth`/`RequireRole`),
  RBAC (`PRIVILEGED_ROLES`, `RIDER_APPROVER_ROLES`), rider approve/reject,
  Axios envelope + single-flight refresh, `VITE_*` config — implemented.
- `vercel.json`: Vite preset, `npm ci` / `npm run build` / `dist`, SPA fallback
  rewrites, security headers. No secret in any `VITE_*` value.

**Why not 100%:** not deployed to Vercel; no real-browser CORS/deep-link E2E.

## 6. Deployment — 60% — IMPLEMENTED (deploy BLOCKED)

Prepared and reviewed, **not deployed**:
- `render.yaml` (Docker/Java 21, region, health check, secret env var names,
  `PORT`, `SPRING_FLYWAY_ENABLED=false`), `backend/Dockerfile` (multi-stage,
  non-root, Java 21), `admin-web/vercel.json`, `.env.example` files,
  `docs/development/DEPLOYMENT.md`, `environment-variables.md`,
  `infrastructure/database/postgres/*` (role + verification scripts).
- Production CORS, health checks (liveness vs readiness), migration procedure,
  SUPER_ADMIN bootstrap discipline, Flutter release signing guidance.

**Why not 100%:** no authenticated Render/Vercel/Supabase cloud tools or
credentials were available to deploy, and the Supabase credential is invalid.
No production deployment was attempted or claimed; no paid resources created.

## 7. Testing / E2E — 60% — PARTIAL

**Passing this run:** 56 backend tests, 14 customer tests, 16 rider tests,
admin-web lint, all four production builds, `FlywayMigrationSqlTest`, parser +
EPP unit tests.
**Not obtained:** real-PostgreSQL Testcontainers suite (skipped, no Docker),
live cross-application E2E against a deployed backend, WebSocket handshake
against a live server, browser-level Vercel CORS/deep-link verification.

### End-to-end flow matrix (§9)

| # | Flow | State | Evidence |
|---|---|---|---|
| 1 | Customer registration & login | TESTED (H2 full-stack) | `AuthIntegrationTest` |
| 2 | Customer session restoration | TESTED (unit/widget) | customer `auth_test.dart` / `app_test.dart` |
| 3 | Rider registration & PENDING | TESTED (H2 full-stack) | `AuthIntegrationTest` |
| 4 | Admin login | TESTED (H2 full-stack) | `AuthIntegrationTest` |
| 5 | Rider approval | TESTED (H2 full-stack) | `AuthIntegrationTest` |
| 6 | Approved rider login | TESTED (H2 full-stack) | `AuthIntegrationTest` |
| 7 | Role-based restrictions | TESTED (H2 full-stack) | `AuthIntegrationTest` / `RoleProbe` gates |
| 8 | Refresh rotation | TESTED (H2 full-stack) | `AuthIntegrationTest` |
| 9 | Logout / token revocation | TESTED (H2 + widget) | `AuthIntegrationTest`, Flutter `auth_test.dart` |
| 10 | PostgreSQL persistence | **BLOCKED** | no live PG; Testcontainers skipped; H2 only |
| 11 | Admin Web API connectivity | **BLOCKED** | no deployed backend / browser E2E |
| 12 | Flutter API connectivity | **BLOCKED** | release APK uses placeholder URL |
| 13 | WebSocket connectivity | PARTIAL | permitted + configured; not exercised live |
| 14 | Backend health & readiness | TESTED (H2) | `HealthControllerTest`, `RealHttpSmokeTest` |

---

## Blockers (exact)

1. **Supabase credential rejected (highest priority).** The root `.env`
   `SUPABASE_DATABASE_URL` (Session Pooler, `aws-1-ap-northeast-1`,
   port 5432, `postgres.<ref>` user) is **reachable over TCP/TLS** but the
   server returns `FATAL: password authentication failed for user "postgres"`.
   Verified with an independent JDBC probe: the URI parses correctly (16-char
   alphanumeric password, no URL-encoding, fingerprint matches), all three
   connection variants fail, and a plain `postgres` user is rejected with
   "no tenant identifier" — i.e. the `postgres.<ref>` format is correct and
   the **password is wrong/stale** (likely rotated/reset). Consequence: no SQL
   inspection, no migration application, no role creation, no live-PG E2E.
2. **No Docker / no local PostgreSQL.** `PostgresMigrationIntegrationTest`
   (9 tests) is skipped via `@Testcontainers(disabledWithoutDocker = true)`;
   no `initdb`/`postgres` binary exists locally. Real-PostgreSQL dialect
   verification therefore cannot be produced locally.
3. **No authenticated cloud deployment.** No Render/Vercel/Supabase CLI or
   tokens; nothing was deployed, so no production URL is verified.
4. **Production DDL / role changes not auto-applied.** Creating the
   least-privilege role and applying `V1` to the hosted DB change production
   permissions/schema; per the brief these were **prepared, not executed**.

## Exact manual actions required

1. **Fix the database credential (unblocks most of the remainder).** In Supabase
   → Project Settings → Database, reset/confirm the database password and copy
   the **current** Session Pooler URI. Update the git-ignored root `.env`
   `SUPABASE_DATABASE_URL` (and the Render `SUPABASE_DATABASE_URL` secret).
2. **Apply the schema** (fresh DB), from `backend/` using the **Direct**
   connection and the DDL-capable migration role:
   `mvn -B flyway:migrate -Dflyway.url="$DB_MIGRATION_URL" -Dflyway.user="$DB_MIGRATION_USERNAME" -Dflyway.password="$DB_MIGRATION_PASSWORD"`,
   then `flyway:validate`. Verify with
   `infrastructure/database/postgres/02_verify_schema.sql`.
3. **Create the least-privilege runtime role** (approval required): run
   `infrastructure/database/postgres/01_create_app_role_TEMPLATE.sql` once in
   the Supabase SQL editor with real out-of-band values; point `DB_*` at it.
4. **Run real-PostgreSQL tests** in CI with Docker:
   `mvn -B clean verify` will then execute `PostgresMigrationIntegrationTest`.
5. **Deploy backend to Render** from `render.yaml`; set secrets; verify
   `/api/v1/health` → `database: UP`. Provision the first SUPER_ADMIN once,
   then disable `APP_BOOTSTRAP_ADMIN_ENABLED`.
6. **Deploy admin web to Vercel**; set `VITE_*` before build; add the Vercel
   origin to `APP_CORS_ALLOWED_ORIGINS`.
7. **Rebuild Flutter release APKs** with the real Render HTTPS/WSS URLs in
   `production.json` / `--dart-define`, configure release signing from CI
   secrets, and verify on a physical device over cellular.
8. **Re-run the §9 E2E matrix** against the deployed stack.

## Deployment state

**Not deployed.** `render.yaml`, `Dockerfile`, `vercel.json` and
`DEPLOYMENT.md` are ready but no Render/Vercel service was created and no
deployed URL was verified. Do not treat a successful local build as a
successful deployment.

## Secret handling (this run)

- `.env`, `.env.*` (except `.env.example`), `*.jks`, `key.properties`,
  `installers/`, `*.apk` are git-ignored; `.gitignore` verified.
- The `SUPABASE_DATABASE_URL`, `JWT_SECRET` and DB passwords were read from the
  git-ignored `.env` **without being printed**; only structural facts and a
  one-way fingerprint were emitted, never a value.
- No secret was placed in any Flutter `--dart-define`, `VITE_*` variable,
  Dockerfile, `render.yaml`, migration, doc, or commit.
- No production database was reset, dropped, or destroyed; no DDL applied;
  no role/permission change performed.
