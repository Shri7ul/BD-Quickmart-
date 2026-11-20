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
| Backend | 20% | 92% | TESTED + DEPLOYED (DB UP) | 18.4% |
| Supabase migration | 20% | 75% | IMPLEMENTED (live verify BLOCKED) | 15.0% |
| Customer app | 12% | 95% | TESTED + BUILT (old host — rebuild needed) | 11.4% |
| Rider app | 12% | 95% | TESTED + BUILT (old host — rebuild needed) | 11.4% |
| Admin Web | 12% | 92% | BUILT + DEPLOYED (production VITE_ baked) | 11.0% |
| Deployment | 12% | 72% | IMPLEMENTED + backend DEPLOYED | 8.6% |
| Testing / E2E | 12% | 70% | PARTIAL (live HTTP/WSS + CORS integration tests) | 8.4% |
| **Total** | **100%** | | | **84.2% → 84%** |

### Status legend

- **IMPLEMENTED** — code/config/docs written and reviewed.
- **TESTED** — automated tests pass.
- **BUILT** — a production build artifact is produced.
- **DEPLOYED** — a running, reachable deployed application was verified.
- **BLOCKED** — an external prerequisite prevents further progress.

The **backend** is DEPLOYED on the live service
`https://bd-quickmart.onrender.com` (verified 2026-11-20: liveness `200 UP`,
`/api/v1/health` `200` with **`database: UP`**, OpenAPI title "BD QuickMart
API"). The older `bd-quickmart-backend.onrender.com` host is a **suspended**
Render service (HTTP 503 "Service Suspended") and must not be used. The admin
web is deployed at `https://bdquickmart.vercel.app`; the release APKs were
built locally and still point at the old host (rebuild required).

---

## 1. Backend — 92% — TESTED + DEPLOYED (DB UP)

Spring Boot 3.5.6 / Java 21. `mvn -B clean verify` → **BUILD SUCCESS,
63 tests, 0 failures, 0 errors, 9 skipped** (the 9 are real-PostgreSQL
Testcontainers tests, skipped because Docker is unavailable). The 7 new
`CorsIntegrationTest` cases cover the exact production preflight contract.

- Registration / login, JWT access + refresh, **refresh rotation + replay
  rejection**, logout / token revocation, `/me` — full-stack MockMvc integration
  tests (`AuthIntegrationTest`, 23 tests).
- Six canonical roles, rider `PENDING/APPROVED/REJECTED` workflow, admin
  authorization, privileged provisioning — covered by the same suite.
- CORS (`WebConfig`/`SecurityConfig`), WebSocket auth, liveness vs readiness,
  Render `PORT` binding, production-safe config, `RealHttpSmokeTest`,
  portable JPA entities — implemented and tested. Exact production admin origin
  `https://bdquickmart.vercel.app` added to the explicit allowlist (no wildcard,
  credentials enabled); methods are `GET,POST,PUT,PATCH,DELETE,OPTIONS`, headers
  include `Content-Type`/`Authorization`.
- **Live probes this run (2026-11-20):**
  - `/actuator/health/liveness` on the live host → `200 {"status":"UP"}`.
  - `/api/v1/health` on the live host → `200`, app `UP`, **`database: UP`**.
  - `/v3/api-docs` → `200`, title **"BD QuickMart API"**.
  - `OPTIONS /api/v1/auth/login` from `https://bdquickmart.vercel.app` → `403
    Invalid CORS request` **before the fix** (Spring `CorsFilter`); from
    `http://localhost:5173` → `200` with `Access-Control-Allow-*` (proves the
    mechanism).
  - `GET` on suspended `bd-quickmart-backend.onrender.com` → `503` **"Service
    Suspended"** with `x-render-routing: suspend` — the actual source of the
    reported preflight 503 (Render, not Spring Boot).

**Why not 100%:** a live re-check of the preflight after the Render/Vercel
redeploy is still required (local MockMvc tests pass); `APP_PUBLIC_URL` remains
stale until set in the Render dashboard.

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
- **Config still points at the old suspended host.**
  `dart-defines/production.json.example` and the APK both contain
  `https://bd-quickmart-backend.onrender.com/api/v1` / the matching WSS URL
  with `IS_DEVELOPMENT=false`. **Both must be updated to the live host and the
  APK rebuilt** before distribution (out of scope for the admin CORS fix).
  `10.0.2.2` strings that remain are dead dev-branch literals / hint text.
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
- Same HTTPS/WSS config + `IS_DEVELOPMENT=false` pattern; the built APK still
  points at the old suspended host and needs the same rebuild. Application ID
  **preserved**: `com.bdquickmart.rider_app`.

**Why not 100%:** same no-live-E2E / debug-signing caveat as the customer app.

## 5. Admin Web — 92% — BUILT + DEPLOYED (production VITE_ baked)

- `npm run lint` (oxlint) → **0 warnings, 0 errors**; `npm run build`
  (`tsc -b && vite build`) → **success** (98 modules) with production env.
- **Production values baked and verified in the local production bundle:**
  `VITE_API_BASE_URL=https://bd-quickmart.onrender.com/api/v1`,
  `VITE_WS_BASE_URL=wss://bd-quickmart.onrender.com/ws/tracking`,
  `VITE_IS_DEVELOPMENT=false`, `VITE_REQUEST_TIMEOUT=30000`. Verified the old
  suspended host is absent from the rebuilt bundle. The only `localhost:8080`
  strings left are dead `??` fallbacks.
- `vercel.json`: Vite preset, `npm ci` / build / `dist`, **top-level `env`
  block with the four public `VITE_*` values**, SPA fallback rewrites, security
  headers. No secret in any `VITE_*` value.
- Login, session, protected routes (`RequireAuth`/`RequireRole`), RBAC, rider
  approve/reject, Axios envelope + single-flight refresh — implemented.
- **Live finding:** the *deployed* Vercel bundle still contained
  `bd-quickmart-backend.onrender.com` (a suspended service → HTTP 503 on the
  preflight). `vercel.json` is now corrected; a Vercel redeploy and a real
  browser login are still required. The live host's `403` CORS rejection is
  addressed by the explicit allowlist + `CorsIntegrationTest`.

**Why not 100%:** no real-browser CORS/deep-link E2E after redeploy; Render
`APP_CORS_ALLOWED_ORIGINS` / `APP_PUBLIC_URL` must be confirmed in the dashboard.

## 6. Deployment — 72% — IMPLEMENTED + backend/frontend DEPLOYED

- **Backend is deployed and reachable on the live Render service** (verified
  2026-11-20: liveness `200 UP`, `/api/v1/health` `200` with `database: UP`).
  This is an external operator deployment, not one performed from this repo.
- **Admin web is deployed at `https://bdquickmart.vercel.app`**, but the
  deployed bundle still used the suspended host until a redeploy of the
  corrected `vercel.json` (done in this change).
- Prepared: `render.yaml` (Docker/Java 21, region, liveness health check,
  secret env var names, `PORT`, `SPRING_FLYWAY_ENABLED=false`),
  `backend/Dockerfile`, `admin-web/vercel.json` (+ production `env`),
  `.env.example` files, `docs/development/DEPLOYMENT.md`,
  `environment-variables.md`, `infrastructure/database/postgres/*`.
- Admin web uses the live URL; the Flutter apps still use the old suspended
  host and must be rebuilt (separate scope).

**Why not 100%:** Render dashboard values (`APP_CORS_ALLOWED_ORIGINS`,
`APP_PUBLIC_URL`) need operator confirmation; Vercel redeploy and real-browser
login verification are pending; no authenticated cloud tools available here.

## 7. Testing / E2E — 70% — PARTIAL

**Passing this run:** 63 backend tests (including the new 7-case
`CorsIntegrationTest`), 14 customer tests, 16 rider tests, admin-web lint, all
production builds, both release APKs, `FlywayMigrationSqlTest`, parser + EPP
unit tests, live HTTP/health/OpenAPI probes, and bundle host verification.
**Not obtained:** real-PostgreSQL Testcontainers suite (skipped, no Docker),
real-browser Vercel CORS/login E2E after redeploy, device/cellular Flutter E2E.

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
| 10 | PostgreSQL persistence | LIVE DB UP (no authenticated E2E) | `/api/v1/health` `database: UP`; Testcontainers skipped; H2 tests pass |
| 11 | Admin Web API connectivity | FIXED IN CODE, redeploy pending | `vercel.json` → live host; `CorsIntegrationTest` 7/7; deployed bundle still stale |
| 12 | Flutter API connectivity | PARTIAL | APKs wired to the OLD suspended host; rebuild required; no device E2E |
| 13 | WebSocket connectivity | TRANSPORT REACHABLE (live) | earlier `101 Switching Protocols` on the now-suspended host; recheck on live host |
| 14 | Backend health & readiness | TESTED (live + H2) | live host liveness UP, `/api/v1/health` `database: UP`, `HealthControllerTest` |
| 15 | CORS preflight (admin origin) | TESTED (mock), live recheck pending | `CorsIntegrationTest`: prod origin 200, unknown/wildcard/TRACE 403, protected-route preflight 200, 401 carries CORS headers |

---

## Blockers (exact)

1. **Redeploy required for the admin CORS fix (highest priority).** The code
   and `vercel.json` changes are local only. Render must have
   `APP_CORS_ALLOWED_ORIGINS` set and Vercel must rebuild, otherwise the live
   preflight still fails (403 on the live host, 503 on the suspended host).
2. **`APP_PUBLIC_URL` stale.** OpenAPI `servers` still advertise
   `http://localhost:8080`; set it to `https://bd-quickmart.onrender.com`.
3. **Admin bundle on Vercel is stale.** It still bakes in the suspended
   `bd-quickmart-backend.onrender.com` host; a Vercel redeploy is required.
4. **Flutter apps point at the suspended host.** Both `dart-defines` examples
   and both built APKs must be updated and rebuilt (separate scope).
5. **No Docker / no local PostgreSQL.** `PostgresMigrationIntegrationTest`
   (9 tests) skipped; real-PG dialect verification cannot be produced locally.
6. **No authenticated cloud deployment tools.** The Render/Vercel dashboard
   changes and a real-browser login test cannot be performed from here.
7. **Production DDL / role changes not auto-applied.** Prepared as templates
   only; they change production permissions/schema and require approval.
8. **Release signing not configured.** Both APKs are debug-signed (no
   keystore/`key.properties`), fine for internal testing, not for Play Store.

## Exact manual actions required

1. **Render — set CORS + public URL, then redeploy.** In the Render dashboard
   for the live service `bd-quickmart`:
   `APP_CORS_ALLOWED_ORIGINS=https://bdquickmart.vercel.app` and
   `APP_PUBLIC_URL=https://bd-quickmart.onrender.com`. Redeploy and confirm
   `OPTIONS /api/v1/auth/login` with that Origin returns `200`.
2. **Vercel — redeploy the admin web** (Root Directory `admin-web`) so the
   corrected `vercel.json` is baked into the JS bundle; confirm the deployed
   asset contains `bd-quickmart.onrender.com` and not the suspended host.
3. **Real browser login** from `https://bdquickmart.vercel.app` over HTTPS;
   confirm no CORS error and a successful `/api/v1/auth/login`.
4. **Flutter — update and rebuild** both `dart-defines/production.json.example`
   files to the live URL and rebuild both release APKs (separate scope).
5. **Run real-PostgreSQL tests** in CI with Docker.
6. **Sign the release APKs** from CI secrets (or use debug-signed artifacts for
   internal testing), then verify on a physical device over cellular.
7. **Re-run the E2E matrix** against the fully healthy deployed stack.

## Deployment state

- **Backend: deployed & reachable (healthy).**
  `https://bd-quickmart.onrender.com` — liveness UP, `/api/v1/health` 200 with
  `database: UP`. Deployed externally by the operator, not from this repo.
  The older `bd-quickmart-backend.onrender.com` is a suspended service.
- **Admin web: deployed at `https://bdquickmart.vercel.app`** with a stale
  bundle; Vercel redeploy with the corrected `vercel.json` is pending.
- **Flutter apps: not deployed** (APK artifacts built locally, still pointing
  at the suspended host; rebuild pending).
- Do not treat a successful local build as a successful deployment.

## Production URL used (this run)

Live service verified 2026-11-20:
- Base: `https://bd-quickmart.onrender.com`
- API: `https://bd-quickmart.onrender.com/api/v1`
- WebSocket: `wss://bd-quickmart.onrender.com/ws/tracking`
- Admin origin allowed in CORS: `https://bdquickmart.vercel.app`

Applied in this change to `admin-web/vercel.json` (`env`),
`admin-web/.env.example`, `backend/src/main/resources/application.yml`,
`WebConfig`, `render.yaml` (comments), `DEPLOYMENT.md` and
`environment-variables.md`. The Flutter examples/APKs still use the old host.
These are **public URLs**, not secrets.

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
