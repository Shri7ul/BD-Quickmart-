# BD QuickMart — Deployment Guide

End-to-end deployment instructions for the cloud topology. **No credential,
password, JWT secret or keystore appears in this document.** The production
backend hostname below is a **public URL, not a secret** (it is visible in
every client bundle). This is a guide, not an executable script; no deployment
was performed from this repository.

> **Production backend (live service, verified 2026-11-20):**
> `https://bd-quickmart.onrender.com`
> - API base: `https://bd-quickmart.onrender.com/api/v1`
> - WebSocket: `wss://bd-quickmart.onrender.com/ws/tracking`
>
> `admin-web/vercel.json` is configured with this URL. See section 0 for live
> health and the CORS fix. **Warning:** the older hostname
> `bd-quickmart-backend.onrender.com` is a *different, suspended* Render service
> (HTTP 503 "Service Suspended", header `x-render-routing: suspend`). The
> customer/rider Flutter release builds still bake in that old host and must be
> rebuilt against the live URL — see section 5.

## Topology

| Component | Technology | Target | Config in repo |
|---|---|---|---|
| Backend | Spring Boot 3.5.6 / Java 21 | **Render** Web Service (Docker) | `backend/Dockerfile`, `render.yaml`, `backend/src/main/resources/application.yml` |
| Database | Supabase PostgreSQL (fresh) | **Supabase** | `backend/src/main/resources/db/migration/V1__init_auth_schema.sql` (Flyway) |
| Admin web | React + TypeScript + Vite | **Vercel** | `admin-web/vercel.json`, `admin-web/.env.example` |
| Customer app | Flutter Android | APK / Play internal testing | `customer-app/dart-defines/production.json.example`, `app_config.dart` |
| Rider app | Flutter Android | APK / Play internal testing | `rider-app/dart-defines/production.json.example`, `app_config.dart` |
| AI/ML | separate service (optional) | independent | `AI_ENABLED=false`; out of scope |

Order: **Supabase → Render → Vercel → Flutter**. Vercel and the Flutter apps need
the real Render HTTPS URL; Render needs the Supabase connection URI.

---

## 0. Verified live deployment status (2026-11-20)

Probed directly against `https://bd-quickmart.onrender.com`. Results are
**observations, not claims of success**:

| Probe | Result | Meaning |
|---|---|---|
| `GET /actuator/health/liveness` | `200` `{"status":"UP"}` | JVM/app process alive, deploy reachable |
| `GET /api/v1/health` | `200`, `data.status=UP`, **`database: UP`** | HTTP layer and Supabase both healthy |
| `GET /v3/api-docs` | `200`, title **"BD QuickMart API"** | Confirms service identity |
| `OPTIONS /api/v1/auth/login` from `https://bdquickmart.vercel.app` | `403 Invalid CORS request` **before the fix** | Spring `CorsFilter`: origin not in allowlist |
| `OPTIONS /api/v1/auth/login` from `http://localhost:5173` | `200` + `Access-Control-Allow-*` | Confirms CORS mechanism works when origin is allowed |
| `GET /actuator/health/liveness` on `bd-quickmart-backend.onrender.com` | `503` **"Service Suspended"**, `x-render-routing: suspend` | **Render-level suspension, not Spring Boot** — source of the reported preflight 503 |

**Root cause of the admin login failure:** the deployed Vercel bundle had
`VITE_API_BASE_URL=https://bd-quickmart-backend.onrender.com/api/v1` baked in.
That host is a *suspended* Render service, so Chrome saw a **503** on the
preflight. Separately, the live backend rejected the exact Vercel origin with a
**403** because `APP_CORS_ALLOWED_ORIGINS` was never set (only localhost
defaults). Both are fixed in this change: `vercel.json` now points at the live
service, and the exact origin `https://bdquickmart.vercel.app` is in the backend
allowlist default. Local MockMvc integration tests cover the preflight contract;
a live re-check after deploy is still required (see section 7).

**Exact operator actions still required** (Render/Vercel dashboards):

1. In Render, set `APP_CORS_ALLOWED_ORIGINS=https://bdquickmart.vercel.app`
   (or the full comma-separated list if other clients need CORS). Wildcards
   are invalid because `setAllowCredentials(true)` is used. Redeploy.
2. In Render, set `APP_PUBLIC_URL=https://bd-quickmart.onrender.com` so the
   OpenAPI server list stops advertising `localhost:8080`.
3. Redeploy Vercel so the corrected `vercel.json` values are baked into the JS
   bundle; verify the bundle contains `bd-quickmart.onrender.com` and not the
   suspended host.
4. Re-run the section 7 checklist, including a real browser login.
5. Update and rebuild the Flutter apps against the live URL (section 5).

---

## 1. Supabase (database foundation)

1. Create a **fresh** Supabase project. Choose the region closest to the
   Bangladesh user base (irreversible — decide before creation).
2. Confirm database encoding is `UTF8` (Supabase default).
3. In **Project Settings → Database → Connection string**, copy the
   **Session Pooler** URI (port `5432`, user `postgres.<PROJECT_REF>`). Keep the
   **Direct** connection (port `5432`, `db.<PROJECT_REF>...`) for migrations.
   Use the **Session Pooler or Direct** for the app — never the transaction
   pooler (port `6543`), because refresh-token rotation uses
   `SELECT ... FOR UPDATE`.
4. Treat the connection URI as a **secret**. It contains a URL-encoded
   password. Store it in Render secret env vars (and locally in the git-ignored
   root `.env`). Never commit it.
5. Extensions (SQL editor, if available on your plan): `pgcrypto` /
   `gen_random_uuid()` for future native UUIDs, `pgvector` for later AI
   embeddings, `pg_stat_statements` for monitoring. `V1` itself needs **no**
   extension (external IDs are application-generated strings).
6. Enable daily backups / Point-in-Time Recovery where the plan allows.

> **Get the CURRENT connection URI from the dashboard.** Database passwords can
> be rotated/reset; a copied URI will stop authenticating. If authentication
> fails, regenerate/reset the password in Project Settings and update every
> secret store that holds the URI.

### Roles and privileges (separation)

- **Migration role**: the DDL-capable `postgres` connection. Used **only** for
  the Flyway step, never by the running app.
- **Runtime role**: a least-privilege login (DML only). Run
  `infrastructure/database/postgres/01_create_app_role_TEMPLATE.sql` **once,
  manually, with approval**, after `V1` is applied. Do **not** grant
  `CREATEDB`/`SUPERUSER`/`BYPASSRLS` or `CREATE` on `public`.

Creating/altering roles changes production permissions — it is intentionally
**not** automated by the backend.

---

## 2. Apply the schema (Flyway, separate step)

Migrations are **out of band**, never at app startup
(`spring.flyway.enabled=false` in production). `ddl-auto` is `none` (set
`validate` in staging/CI to assert entity/schema alignment).

From `backend/`, using the **Direct** connection and the migration role:

```bash
# Values come from the secret store; they are NOT in this repository.
mvn -B flyway:migrate \
  -Dflyway.url="$DB_MIGRATION_URL" \
  -Dflyway.user="$DB_MIGRATION_USERNAME" \
  -Dflyway.password="$DB_MIGRATION_PASSWORD"

mvn -B flyway:info     -Dflyway.url="$DB_MIGRATION_URL" -Dflyway.user="$DB_MIGRATION_USERNAME" -Dflyway.password="$DB_MIGRATION_PASSWORD"
mvn -B flyway:validate -Dflyway.url="$DB_MIGRATION_URL" -Dflyway.user="$DB_MIGRATION_USERNAME" -Dflyway.password="$DB_MIGRATION_PASSWORD"
```

- `DB_MIGRATION_URL` is a JDBC URL such as
  `jdbc:postgresql://db.<PROJECT_REF>.supabase.co:5432/postgres?sslmode=require`.
- Alternatively set `SPRING_FLYWAY_ENABLED=true` with all three
  `DB_MIGRATION_*` variables for a one-off in-app migration. Leave it `false`
  afterwards.
- Flyway records `V1` in `flyway_schema_history`. Migrations are
  **forward-only**; rollbacks are corrective forward migrations, never `DROP`/
  `TRUNCATE` on a live schema. Because the database is fresh, the ultimate
  recovery is to recreate the empty project from `V1` onward.
- Verify with `infrastructure/database/postgres/02_verify_schema.sql` (read-only).

---

## 3. Render (backend)

`render.yaml` is a Blueprint (`runtime: docker`, Java 21 pinned in
`backend/Dockerfile`, root dir `backend`, region `singapore`). No `plan` is set —
choosing a compute plan is a billing decision you make in the dashboard.

1. Create the Web Service from `render.yaml` (or import the Blueprint).
2. Set secret env vars (`sync: false`) in the dashboard:
   - **Database** — choose ONE of:
     - `SUPABASE_DATABASE_URL` = complete Session Pooler URI (recommended; the
       backend parses it and enforces `sslmode=require`), **or**
     - `DB_URL` + `DB_USERNAME` + `DB_PASSWORD` (split form,
       `DB_URL=jdbc:postgresql://<POOLER_HOST>:5432/postgres?sslmode=require`).
   - `JWT_SECRET` (≥ 32 bytes, e.g. `openssl rand -base64 48`),
     `JWT_ALLOW_EPHEMERAL_DEV_KEY=false`.
   - `APP_CORS_ALLOWED_ORIGINS` = exact Vercel production origin(s).
   - `APP_PUBLIC_URL` = the Render HTTPS URL (for the OpenAPI server list).
3. Leave `PORT` unset — Render injects it; the app binds
   `${PORT:${SERVER_PORT:8080}}`. Keep `SPRING_FLYWAY_ENABLED=false`.
4. **Health check**: `healthCheckPath: /actuator/health/liveness` (liveness
   only, always 200). `/actuator/health/readiness` (adds `db`, 503 on a DB
   outage) is for monitoring, not the platform check, to avoid restart loops.
5. Prefer stdout logs (the Render filesystem is ephemeral; `LOG_FILE` is
   non-persistent).
6. First deploy with a single instance while provisioning the first
   `SUPER_ADMIN` (`APP_BOOTSTRAP_ADMIN_*`), then **disable**
   `APP_BOOTSTRAP_ADMIN_ENABLED` immediately and remove those secrets.
7. Do **not** put `DB_MIGRATION_*` in the runtime service.

---

## 4. Vercel (admin web)

1. Import `admin-web` as a Vite project. Set **Root Directory** to `admin-web`
   (a dashboard setting, not a `vercel.json` field). Node 22.x.
2. `vercel.json` supplies: `framework: vite`, `installCommand: npm ci`,
   `buildCommand: npm run build`, `outputDirectory: dist`, SPA fallback
   rewrites (`/(.*)` → `/index.html`, so `/orders`, `/riders` deep links work),
   security headers (HSTS, nosniff, etc.), **and a top-level `env` block** with
   the production `VITE_*` values so a deploy builds correctly out of the box:
   - `VITE_API_BASE_URL=https://bd-quickmart.onrender.com/api/v1`
   - `VITE_WS_BASE_URL=wss://bd-quickmart.onrender.com/ws/tracking`
   - `VITE_REQUEST_TIMEOUT=30000`
   - `VITE_IS_DEVELOPMENT=false`
3. These can also/instead be set in Vercel **Production** environment variables
   (dashboard values override `vercel.json`). They are baked into the JS
   bundle at build time — changing one requires a redeploy. A local production
   build:
   ```bash
   cd admin-web
   VITE_API_BASE_URL=https://bd-quickmart.onrender.com/api/v1 \
   VITE_WS_BASE_URL=wss://bd-quickmart.onrender.com/ws/tracking \
   VITE_REQUEST_TIMEOUT=30000 VITE_IS_DEVELOPMENT=false \
   npm run build
   ```
   After deploy, fetch the built JS asset and confirm it contains
   `bd-quickmart.onrender.com` and NOT the suspended
   `bd-quickmart-backend.onrender.com` host.
4. `VITE_*` values are **public** by definition. **Never** put `JWT_SECRET`,
   DB credentials or AI keys there.
5. On Render set `APP_CORS_ALLOWED_ORIGINS=https://bdquickmart.vercel.app`
   and redeploy. `WebConfig` uses `setAllowCredentials(true)` with an explicit
   origin list, so `*` is invalid. The code default also includes this exact
   origin so a fresh deploy is not broken if the env var is missing. Covered by
   `CorsIntegrationTest` (7 tests).

---

## 5. Flutter (customer & rider apps)

Configuration is **compile-time** (`String.fromEnvironment`). Release builds
MUST pass an HTTPS API URL / WSS WS URL and `IS_DEVELOPMENT=false`; the release
defaults are deliberately non-routable `.invalid` hosts so a misconfigured
build fails closed. **Never** ship `10.0.2.2`, a LAN IP or cleartext HTTP.

```bash
# Customer
cd customer-app
flutter build apk --release \
  --dart-define-from-file=dart-defines/production.json.example

# Rider
cd ../rider-app
flutter build apk --release \
  --dart-define-from-file=dart-defines/production.json.example
```

- **Both apps' `dart-defines/production.json.example` files contain the confirmed
  production values** (`IS_DEVELOPMENT=false`, the HTTPS API URL and the WSS URL)
  pointing at the live host `https://bd-quickmart.onrender.com`. They are public
  config, not secrets — no keystore, password or JWT is involved. `AppConfig`
  reads them at compile time; `ServerConfig` inherits that value as its default
  (a runtime QA override may still replace it per-device).
- **Release APKs have been rebuilt with those live values** and copied to the
  repository-root `apps/` directory as `BDQuickMart-Customer.apk` and
  `BDQuickMart-Rider.apk` (see `apps/README.md`). Both were string-verified to
  contain the live host and zero occurrences of the suspended
  `bd-quickmart-backend.onrender.com` host. The fail-closed `.example.invalid`
  default is absent from these builds.
- Release Android manifests omit `usesCleartextTraffic` (debug-only), so
  cleartext is correctly blocked.
- **Release signing**: configure `android/app/build.gradle.kts` `signingConfigs`
  with a keystore from CI secrets. **Never** commit a keystore (`.jks`),
  `key.properties`, or signing passwords. Without a signing config, Flutter
  signs the release APK with the debug key — acceptable for internal testing,
  not for the Play Store.
- Application IDs are distinct: `com.bdquickmart.customer_app` /
  `com.bdquickmart.rider_app`.
- The runtime "Change server URL" dialog remains for QA; hide/disable it in
  production builds if end users must not repoint the app.
- Verify on a physical device over cellular: full auth flow, PENDING rider
  screen, role guards, Bangla rendering and a `wss://` handshake.

---

## 6. Environment variables

Full reference (names, defaults, precedence, security rules):
[`environment-variables.md`](environment-variables.md). Templates:
root `.env.example`, `admin-web/.env.example`, and each Flutter app's
`.env.example` / `dart-defines/`.

Quick rules:
- `.env` and `*.jks` / `key.properties` are git-ignored; only `*.example` is
  tracked.
- `SUPABASE_DATABASE_URL` / `JWT_SECRET` / DB passwords live **only** in
  Render/Supabase secret stores, never in Git, logs, or client bundles.
- `AI_ENABLED=false` keeps the platform fully functional with no AI calls.

---

## 7. Post-deploy verification checklist

- [ ] `GET https://bd-quickmart.onrender.com/api/v1/health` → 200,
      **`database: UP`** (observed UP on 2026-11-20).
- [ ] `GET /actuator/health/readiness` → `UP`; liveness → `UP`.
- [ ] `OPTIONS /api/v1/auth/login` with
      `Origin: https://bdquickmart.vercel.app` → `200` with
      `Access-Control-Allow-Origin: https://bdquickmart.vercel.app`;
      an unknown origin → `403`.
- [ ] Deployed Vercel JS bundle contains `bd-quickmart.onrender.com` and not the
      suspended `bd-quickmart-backend.onrender.com`.
- [ ] `/v3/api-docs` and `/swagger-ui.html` reachable; OpenAPI shows the real
      Render URL (`APP_PUBLIC_URL`) instead of `localhost:8080`.
- [ ] Exactly one `SUPER_ADMIN` exists; `APP_BOOTSTRAP_ADMIN_ENABLED=false`.
- [ ] Register `CUSTOMER` and `RIDER` (RIDER starts `PENDING`); login, refresh
      rotation, replay rejection, logout, `/me`.
- [ ] PENDING rider denied `/api/v1/rider/**`; admin approve/reject works.
- [ ] Browser preflight from the Vercel origin succeeds; disallowed origin fails.
- [ ] Admin deep links (`/orders`, `/riders`) reload via SPA fallback.
- [ ] Flutter release builds reach the public Render URL over cellular.
- [ ] No secret in Git, logs, or client bundles; TLS everywhere.

Build success is **not** deployment verification. Only a running, reachable
deployed application confirms a deployment.
