# BD QuickMart — Project Progress

> **Last updated:** 2026-11-15 (production API integration run)
> **Branch:** `main`
> **Method:** completion is measured by **verified functionality**, not code
> volume. Every claim below is backed by an actual command/HTTP result from
> this run. `BUILT` ≠ `TESTED` ≠ `DEPLOYED`.

## Overall completion: **83%**

The repository (backend, migrations, both Flutter apps, admin web, deployment
config and docs) is implemented, tested and built to the maximum extent the
environment allows. **New in this run:** the Render backend was found to be
**actually deployed and reachable**, and all three client applications were
configured and rebuilt against that real production URL. The remaining ~17% is
dominated by one hard blocker: **the deployed backend's database is DOWN**
(Supabase credential rejected), so live end-to-end auth flows cannot be
verified. No production database was modified and no deployment was performed
from this repository.

### Weighted breakdown

| Area | Weight | Verified | Status | Weighted |
|---|---|---|---|---|
| Backend | 20% | 90% | TESTED + DEPLOYED (DB DOWN) | 18.0% |
| Supabase migration | 20% | 75% | IMPLEMENTED (live verify BLOCKED) | 15.0% |
| Customer app | 12% | 95% | TESTED + BUILT (production URL) | 11.4% |
| Rider app | 12% | 95% | TESTED + BUILT (production URL) | 11.4% |
| Admin Web | 12% | 90% | BUILT (production VITE_ baked) | 10.8% |
| Deployment | 12% | 70% | IMPLEMENTED + backend DEPLOYED | 8.4% |
| Testing / E2E | 12% | 65% | PARTIAL (live HTTP/WSS/CORS probed) | 7.8% |
| **Total** | **100%** | | | **82.8% → 83%** |

### Status legend

- **IMPLEMENTED** — code/config/docs written and reviewed.
- **TESTED** — automated tests pass.
- **BUILT** — a production build artifact is produced.
- **DEPLOYED** — a running, reachable deployed application was verified.
- **BLOCKED** — an external prerequisite prevents further progress.

The **backend** is now marked DEPLOYED: `https://bd-quickmart-backend.onrender.com`
was verified live (liveness `200 UP`, `/api/v1/health` `200`, OpenAPI title
"BD QuickMart API", WSS `101`). It is **degraded** — readiness/DB are `DOWN`.
The Flutter apps and admin web are **not** deployed (no Vercel deploy / no
device install was performed).

---

## 1. Backend — 90% — TESTED + DEPLOYED (DB DOWN)

Spring Boot 3.5.6 / Java 21. `mvn -B clean verify` → **BUILD SUCCESS,
56 tests, 0 failures, 0 errors, 9 skipped** (the 9 are real-PostgreSQL
Testcontainers tests, skipped because Docker is unavailable).

- Registration / login, JWT access + refresh, **refresh rotation + replay
  rejection**, logout / token revocation, `/me` — full-stack MockMvc integration
  tests (`AuthIntegrationTest`, 23 tests).
- Six canonical roles, rider `PENDING/APPROVED/REJECTED` workflow, admin
  authorization, privileged provisioning — covered by the same suite.
- CORS (`WebConfig`/`SecurityConfig`), WebSocket auth, liveness vs readiness,
  Render `PORT` binding, production-safe config, `RealHttpSmokeTest`,
  portable JPA entities — implemented and tested.
- **Live probes this run (2026-11-14/15):**
  - `/actuator/health/liveness` → `200 {"status":"UP"}` (cold start ~59 s).
  - `/api/v1/health` → `200`, app `UP`, **`database: DOWN`**.
  - `/actuator/health` / `/actuator/health/readiness` → `503`, `db: DOWN`
    (`CannotGetJdbcConnectionException`).
  - `/v3/api-docs` → `200`, title **"BD QuickMart API"**, 13 paths.
  - `wss://…/ws/tracking` handshake → **`101 Switching Protocols`**.
  - CORS preflight: `localhost:5173`/`3000` → `200`; `vercel.app` origins → `403`.
  - OpenAPI `servers` still `http://localhost:8080` (`APP_PUBLIC_URL` stale).

**Why not 100%:** the live database is DOWN, so live auth/RBAC/persistence E2E
is impossible; `APP_PUBLIC_URL` and production CORS are misconfigured on Render.

## 2. Supabase migration — 75% — IMPLEMENTED (live verification BLOCKED)

- PostgreSQL JDBC driver + Flyway + out-of-band `flyway-maven-plugin`; MySQL
  connector/dialect removed from active config.
- **Complete fresh schema** in `V1__init_auth_schema.sql`: `users`,
  `refresh_tokens`, identity PKs, 7 indexes (incl. `lower()` functional unique
  + partial live-family), CHECK constraints for six roles / four states, FK,
  `TIMESTAMPTZ` UTC columns, soft deletes, case-insensitive uniqueness.
  Structurally validated by `FlywayMigrationSqlTest`.
- Runtime/migration **privilege separation** (`spring.flyway.enabled=false`,
  `DB_MIGRATION_*` for DDL, DML-only runtime role, prepared least-privilege SQL).
- `SupabaseDatabaseUrl` (URI parser, URL-encoded password decoding,
  `sslmode=require`) + `DatabaseUrlEnvironmentPostProcessor` (registered via
  `spring.factories`, authoritative over stale `DB_*`), unit-tested (7+5).
- Obsolete MySQL config/docs removed; legacy scripts archived.

**Why not 100%:** the schema was **not applied to Supabase**, and **no live
PostgreSQL verification** was obtained (credential fails auth; Docker absent).
The `PostgresMigrationIntegrationTest` is present but **skipped**. PostgreSQL
compatibility is backed by structural SQL validation + H2 `MODE=PostgreSQL`
tests **only**; it is explicitly **NOT** claimed as verified on live PG.

## 3. Customer app — 95% — TESTED + BUILT (production URL)

- `flutter analyze` → **No issues found**; `flutter test` → **14 passed**;
  `flutter build apk --release --dart-define-from-file=dart-defines/production.json.example`
  → **`customer-app/build/app/outputs/flutter-apk/app-release.apk` (47.1 MB)**.
- Registration, login, secure token storage, session restoration, silent
  single-flight refresh, logout, error handling, API integration — tested.
- **Production URL now real and verified in the APK:**
  `https://bd-quickmart-backend.onrender.com/api/v1` / `wss://…/ws/tracking`,
  `IS_DEVELOPMENT=false`. Confirmed by constant-resolution check and by APK
  inspection: production host present (3×), fail-closed `.example.invalid`
  default absent (0×). `10.0.2.2` strings that remain are dead dev-branch
  literals / hint text, not active URLs.
- Application ID **preserved**: `com.bdquickmart.customer_app` (no change to
  `build.gradle.kts`).

**Why not 100%:** no physical-device / cellular E2E against the live backend
(DB is DOWN), and no release signing key configured (debug-signed).

## 4. Rider app — 95% — TESTED + BUILT (production URL)

- `flutter analyze` → clean; `flutter test` → **16 passed**;
  `flutter build apk --release --dart-define-from-file=dart-defines/production.json.example`
  → **`rider-app/build/app/outputs/flutter-apk/app-release.apk` (47.1 MB)**.
- PENDING / approved / rejected behaviour (test: "pending rider gets a pending
  session; approval promotes it"), secure storage, session restore, refresh,
  logout — implemented/tested.
- Same real production HTTPS/WSS config + `IS_DEVELOPMENT=false`, verified in
  the APK. Application ID **preserved**: `com.bdquickmart.rider_app`.

**Why not 100%:** same no-live-E2E / debug-signing caveat as the customer app.

## 5. Admin Web — 90% — BUILT (production VITE_ baked)

- `npm run lint` (oxlint) → **0 warnings, 0 errors**; `npm run build`
  (`tsc -b && vite build`) → **success** (98 modules) with production env.
- **Production values baked and verified in the bundle:**
  `VITE_API_BASE_URL=https://bd-quickmart-backend.onrender.com/api/v1`,
  `VITE_WS_BASE_URL=wss://bd-quickmart-backend.onrender.com/ws/tracking`,
  `VITE_IS_DEVELOPMENT=false`, `VITE_REQUEST_TIMEOUT=30000`. The only
  `localhost:8080` strings left are dead `??` fallbacks.
- `vercel.json`: Vite preset, `npm ci` / build / `dist`, **top-level `env`
  block with the four public `VITE_*` values**, SPA fallback rewrites, security
  headers. No secret in any `VITE_*` value.
- Login, session, protected routes (`RequireAuth`/`RequireRole`), RBAC, rider
  approve/reject, Axios envelope + single-flight refresh — implemented.

**Why not 100%:** not deployed to Vercel; no real-browser CORS/deep-link E2E
(backend currently returns `403` to `vercel.app` origins).

## 6. Deployment — 70% — IMPLEMENTED + backend DEPLOYED

- **Backend is deployed and reachable on Render** (verified live, section 1),
  though degraded (DB DOWN). This is an external operator deployment, not one
  performed from this repository.
- Prepared: `render.yaml` (Docker/Java 21, region, liveness health check,
  secret env var names, `PORT`, `SPRING_FLYWAY_ENABLED=false`),
  `backend/Dockerfile`, `admin-web/vercel.json` (+ production `env`),
  `.env.example` files, `docs/development/DEPLOYMENT.md`,
  `environment-variables.md`, `infrastructure/database/postgres/*`.
- All three clients configured with the confirmed production URL.

**Why not 100%:** admin web not deployed to Vercel; Render dashboard values
(`SUPABASE_DATABASE_URL`, `APP_CORS_ALLOWED_ORIGINS`, `APP_PUBLIC_URL`) need
operator correction; no authenticated cloud tools available here.

## 7. Testing / E2E — 65% — PARTIAL

**Passing this run:** 56 backend tests, 14 customer tests, 16 rider tests,
admin-web lint, all production builds, both release APKs, `FlywayMigrationSqlTest`,
parser + EPP unit tests, **live HTTP/health/OpenAPI/WSS/CORS probes**, and APK
+ bundle production-URL verification.
**Not obtained:** real-PostgreSQL Testcontainers suite (skipped, no Docker),
live auth/E2E against the deployed backend (DB DOWN), browser Vercel CORS E2E
(preflight 403), device/cellular Flutter E2E.

### End-to-end flow matrix

| # | Flow | State | Evidence |
|---|---|---|---|
| 1 | Customer registration & login | TESTED (H2 + unit), LIVE BLOCKED | `AuthIntegrationTest`, Flutter `auth_test.dart`; live DB DOWN |
| 2 | Customer session restoration | TESTED (unit/widget) | customer `auth_test.dart` (refresh rotation) |
| 3 | Rider registration & PENDING | TESTED (H2 + unit), LIVE BLOCKED | `AuthIntegrationTest`, rider `auth_test.dart` |
| 4 | Admin login | TESTED (H2 full-stack) | `AuthIntegrationTest` |
| 5 | Rider approval | TESTED (H2 full-stack) | `AuthIntegrationTest` |
| 6 | Approved rider login | TESTED (H2 + unit) | `AuthIntegrationTest` / pending→active test |
| 7 | Role-based restrictions | TESTED (H2 full-stack) | `AuthIntegrationTest` / role gates |
| 8 | Refresh rotation | TESTED (H2 + unit) | `AuthIntegrationTest`, single-flight refresh test |
| 9 | Logout / token revocation | TESTED (H2 + widget) | `AuthIntegrationTest`, Flutter `auth_test.dart` |
| 10 | PostgreSQL persistence | **BLOCKED** | live DB DOWN; Testcontainers skipped; H2 only |
| 11 | Admin Web API connectivity | PARTIAL / BLOCKED | backend HTTP `200`, but CORS `403`; Vercel not deployed |
| 12 | Flutter API connectivity | PARTIAL | APKs wired to real URL; no device E2E; live DB DOWN |
| 13 | WebSocket connectivity | **TESTED (live)** | `101 Switching Protocols` on `wss://…/ws/tracking` |
| 14 | Backend health & readiness | TESTED (live + H2) | liveness UP / readiness DOWN (DB), `HealthControllerTest` |

---

## Blockers (exact)

1. **Deployed backend database is DOWN (highest priority).** Live
   `/api/v1/health` returns `database: DOWN`; `/actuator/health/readiness` is
   `503` with `CannotGetJdbcConnectionException`. Root cause matches the earlier
   finding: the root `.env` `SUPABASE_DATABASE_URL` (Session Pooler,
   `aws-1-ap-northeast-1`) is reachable over TCP/TLS but returns
   `FATAL: password authentication failed for user "postgres"` — the
   **password is wrong/stale** (likely rotated/reset). No SQL inspection,
   migration application, role creation or live-PG E2E is possible.
2. **Production CORS not configured.** The deployed backend allows only
   `http://localhost:5173` and `http://localhost:3000`; every `vercel.app`
   origin returns `403` on preflight. `APP_CORS_ALLOWED_ORIGINS` must be set in
   the Render dashboard (wildcards invalid with `setAllowCredentials(true)`).
3. **`APP_PUBLIC_URL` stale.** OpenAPI `servers` still advertise
   `http://localhost:8080`.
4. **No Docker / no local PostgreSQL.** `PostgresMigrationIntegrationTest`
   (9 tests) skipped; real-PG dialect verification cannot be produced locally.
5. **No authenticated cloud deployment tools.** Admin web not deployed to
   Vercel; Render dashboard values cannot be changed from here.
6. **Production DDL / role changes not auto-applied.** Prepared as templates
   only; they change production permissions/schema and require approval.
7. **Release signing not configured.** Both APKs are debug-signed (no
   keystore/`key.properties`), fine for internal testing, not for Play Store.

## Exact manual actions required

1. **Fix the DB credential (unblocks most of the remainder).** In Supabase →
   Project Settings → Database, reset/confirm the DB password, copy the
   **current** Session Pooler URI, and update the Render
   `SUPABASE_DATABASE_URL` secret (and the git-ignored root `.env`). Recheck
   `/actuator/health/readiness` → `UP`.
2. **Apply the schema** (fresh DB) from `backend/` using the **Direct**
   connection + DDL migration role:
   `mvn -B flyway:migrate -Dflyway.url="$DB_MIGRATION_URL" -Dflyway.user="$DB_MIGRATION_USERNAME" -Dflyway.password="$DB_MIGRATION_PASSWORD"`,
   then `flyway:validate`. Verify with
   `infrastructure/database/postgres/02_verify_schema.sql`.
3. **Create the least-privilege runtime role** (approval required): run
   `infrastructure/database/postgres/01_create_app_role_TEMPLATE.sql` once.
4. **Correct Render dashboard values:** set `APP_CORS_ALLOWED_ORIGINS` to the
   exact Vercel production origin, and
   `APP_PUBLIC_URL=https://bd-quickmart-backend.onrender.com`.
5. **Deploy admin web to Vercel** (Root Directory `admin-web`; `vercel.json`
   already carries the production `VITE_*` values). Add the Vercel origin to
   CORS. Re-run the section 7 checklist.
6. **Run real-PostgreSQL tests** in CI with Docker.
7. **Sign the release APKs** from CI secrets (or use the debug-signed artifacts
   for internal testing), then verify on a physical device over cellular.
8. **Re-run the E2E matrix** against the fully healthy deployed stack.

## Deployment state

- **Backend: deployed & reachable (degraded).**
  `https://bd-quickmart-backend.onrender.com` — liveness UP, HTTP/WSS reachable,
  **database DOWN**. Deployed externally by the operator, not from this repo.
- **Admin web: NOT deployed.** `vercel.json` + production `VITE_*` are ready.
- **Flutter apps: NOT deployed** (APK artifacts built locally only).
- Do not treat a successful local build as a successful deployment; do not
  treat a live-but-DB-down backend as a fully working system.

## Production URL used (this run)

Confirmed by the operator and verified live:
- Base: `https://bd-quickmart-backend.onrender.com`
- API: `https://bd-quickmart-backend.onrender.com/api/v1`
- WebSocket: `wss://bd-quickmart-backend.onrender.com/ws/tracking`

Applied to: `customer-app/dart-defines/production.json.example`,
`rider-app/dart-defines/production.json.example`, both apps' `.env.example`,
`admin-web/vercel.json` (`env`), `admin-web/.env.example`, `render.yaml`
(comments), `DEPLOYMENT.md`. It is a **public URL**, not a secret.

## Secret handling (this run)

- `.env`, `.env.*` (except `.env.example`), `*.jks`, `key.properties`,
  `*.apk` are git-ignored; `.gitignore` verified.
- The `SUPABASE_DATABASE_URL`, `JWT_SECRET` and DB passwords were read from the
  git-ignored `.env` **without being printed**; only structural facts / a
  one-way fingerprint were emitted, never a value.
- Only the **public** production hostname was added to client configs; no
  secret in any `--dart-define`, `VITE_*` variable, `vercel.json`, `render.yaml`,
  doc, or commit.
- No production database was reset, dropped, or destroyed; no DDL applied;
  no role/permission change performed; no cloud deployment performed.
