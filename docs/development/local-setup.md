# BD QuickMart — Local Setup Guide

This guide takes a fresh machine from nothing to three running applications and
a connected **Supabase PostgreSQL** database.

> **Assumed:** Windows 10/11, the repository cloned, and a Supabase project
> (or a local PostgreSQL 16 instance) available. XAMPP/MySQL is **not** used —
> see the [cloud migration plan](CLOUD_MIGRATION_PLAN.md).

---

## 0. Toolchain

Goal 01 was developed and verified with the following versions; the database
layer was migrated to PostgreSQL in the cloud-migration task.

| Tool | Version | Install location used |
|---|---|---|
| Java (Temurin) | 21.0.13 LTS | `C:\Users\USER\tools\jdk-21.0.13+9` |
| Apache Maven | 3.9.9 | `C:\Users\USER\tools\apache-maven-3.9.9` |
| Flutter | 3.35.1 (Dart 3.9.0) | `C:\Users\USER\tools\flutter` |
| Node.js | 22.22.2 | system install |
| Android SDK | platforms 34 & 36, build-tools 34–36.1 | `C:\Users\USER\AppData\Local\Android\Sdk` |
| PostgreSQL | 16 (Supabase) | managed cloud project |
| Docker | optional | only for the real-PostgreSQL Testcontainers suite |

If a tool is missing, install it and make sure `java`, `mvn`, `flutter` and
`node` resolve on your `PATH`. For reference, a `PATH` setup looks like this:

```bash
export JAVA_HOME="/c/Users/USER/tools/jdk-21.0.13+9"
export MAVEN_HOME="/c/Users/USER/tools/apache-maven-3.9.9"
export FLUTTER_HOME="/c/Users/USER/tools/flutter"
export ANDROID_SDK_ROOT="/c/Users/USER/AppData/Local/Android/Sdk"
export ANDROID_HOME="$ANDROID_SDK_ROOT"
export PATH="$JAVA_HOME/bin:$MAVEN_HOME/bin:$FLUTTER_HOME/bin:$PATH"
```

On Windows, set the equivalent variables through *System Properties →
Environment Variables*, then open a **new** terminal so they take effect.

---

## 1. Database setup — Supabase PostgreSQL (developer task)

The database is **fresh**: no legacy MySQL data is migrated. The schema is
owned by the versioned Flyway migration

```
backend/src/main/resources/db/migration/V1__init_auth_schema.sql
```

which creates `users` and `refresh_tokens` with their constraints and
PostgreSQL-specific indexes. Migrations are applied as a **separate step**
with dedicated DDL-capable credentials; the running application connects with
a **least-privilege DML-only role** and has `spring.flyway.enabled=false` by
default, so it never performs DDL at startup. Hibernate is pinned to
`ddl-auto: none` (set `DB_DDL_AUTO=validate` in CI/staging to assert
entity/schema alignment).

> The original MySQL 8 / XAMPP scripts from Goals 01–02 were retained as a
> historical reference under
> [`infrastructure/database/legacy/mysql/`](../../infrastructure/database/legacy/mysql/README.md).
> They are **not** an active schema path and must not be run against
> PostgreSQL.

### Step 1 — Create the Supabase project

1. Create a **fresh** Supabase project. No existing data is imported.
2. Choose the region closest to the Bangladesh user base (region cannot be
   changed later without a project migration); keep the backend host nearby.
3. Confirm the database encoding is `UTF8` (Supabase default) so Bangla script
   and emoji are stored correctly.
4. Record the project reference, hosts and generated database password in the
   team secret store. **Never commit them.**
5. Enable backups / Point-in-Time Recovery on the chosen plan.

### Step 2 — Choose the connection string

| Connection | Port | Use for |
|---|---|---|
| **Session Pooler** | 5432 | The running Spring Boot app (recommended) — preserves session state, compatible with Hikari and `FOR UPDATE` |
| **Direct** (`db.<ref>.supabase.co`) | 5432 | **Migrations** and admin tasks |
| Transaction Pooler | 6543 | **Avoid** — breaks session state, prepared statements and pessimistic locking |

Supabase enforces TLS: every connection string must use `sslmode=require`
(or `verify-full` with the CA mounted in the host's trust store). Because
`RefreshTokenRepository` uses `@Lock(PESSIMISTIC_WRITE)` for rotation, never
use the transaction-mode pooler for the application. Keep Hikari small
(`DB_POOL_MAX_SIZE` ~5–10) to respect Supabase connection limits.

### Step 3 — Create the two roles (names only, no secrets)

Run as an administrator in the Supabase SQL editor. Do **not** connect the app
as the `postgres` superuser. Placeholders in `<ANGLE_BRACKETS>`:

```sql
-- Least-privilege RUNTIME role: DML only, no DDL, no superuser.
CREATE ROLE <DB_APP_ROLE> LOGIN PASSWORD '<DB_APP_PASSWORD>';
GRANT USAGE ON SCHEMA public TO <DB_APP_ROLE>;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO <DB_APP_ROLE>;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO <DB_APP_ROLE>;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO <DB_APP_ROLE>;

-- Separate DDL-capable MIGRATION role, used ONLY during deploys.
CREATE ROLE <DB_MIGRATION_ROLE> LOGIN PASSWORD '<DB_MIGRATION_PASSWORD>';
GRANT ALL ON SCHEMA public TO <DB_MIGRATION_ROLE>;
```

`GRANT ... ON ALL TABLES` applies to objects that already exist, and
`ALTER DEFAULT PRIVILEGES` applies to future objects created by the role that
runs it. After each new migration, either refresh the runtime grants or set the
default privileges on the **migrating** role so new tables are covered.
Confirm `PUBLIC` cannot `CREATE` in the `public` schema, and enable RLS only
if client-side Supabase data access is ever introduced (the current
architecture is backend-only).

### Step 4 — Apply the Flyway migration (separate step)

The `flyway-maven-plugin` is deliberately **not** bound to any lifecycle
phase, so the migration runs only when explicitly invoked. From `backend/`,
using the **direct** connection and the migration role:

```bash
cd backend
mvn -B flyway:migrate \
  -Dflyway.url="$DB_MIGRATION_URL" \
  -Dflyway.user="$DB_MIGRATION_USERNAME" \
  -Dflyway.password="$DB_MIGRATION_PASSWORD"
```

`DB_MIGRATION_URL` is a JDBC URL such as
`jdbc:postgresql://db.<PROJECT_REF>.supabase.co:5432/postgres?sslmode=require`
(no real credential is shown). Useful companion goals:

```bash
mvn -B flyway:info     -Dflyway.url="$DB_MIGRATION_URL" -Dflyway.user="$DB_MIGRATION_USERNAME" -Dflyway.password="$DB_MIGRATION_PASSWORD"
mvn -B flyway:validate -Dflyway.url="$DB_MIGRATION_URL" -Dflyway.user="$DB_MIGRATION_USERNAME" -Dflyway.password="$DB_MIGRATION_PASSWORD"
```

Flyway records the applied version in `flyway_schema_history` and serialises
concurrent migrators with its own lock. Migrations are **forward-only**: a
correction is a new `V2__...sql`, never a destructive edit to a live schema.
Optional alternative: set `SPRING_FLYWAY_ENABLED=true` together with all three
`DB_MIGRATION_*` variables so the app migrates at startup — but the
recommended production sequence is migration step → deploy → health check.

### Step 5 — Configure the runtime environment

Set these in the root `.env` (see
[environment-variables.md](environment-variables.md)) — names only, no real
secrets:

```bash
DB_URL=jdbc:postgresql://<POOLER_HOST>:5432/postgres?sslmode=require
DB_USERNAME=<DB_APP_ROLE>
DB_PASSWORD=<DB_APP_PASSWORD>
DB_DRIVER=org.postgresql.Driver
DB_DIALECT=org.hibernate.dialect.PostgreSQLDialect
DB_DDL_AUTO=validate        # none is also safe; use validate in CI/staging
```

For a Render deployment, `PORT` is injected by the platform and the app binds
via `server.port: ${PORT:${SERVER_PORT:8080}}`; set `APP_PUBLIC_URL` to the
real public backend URL so OpenAPI advertises it, and add the Vercel origin to
`APP_CORS_ALLOWED_ORIGINS`. The three `DB_MIGRATION_*` variables belong only
to the migration step / CI secret store, never to the long-running app.

### Step 6 — Verify (all read-only)

```sql
select version, description, success from flyway_schema_history order by installed_rank;  -- one V1 row, success=true
select tablename from pg_tables where schemaname='public' order by tablename;           -- users, refresh_tokens
select indexname, indexdef from pg_indexes where schemaname='public' order by indexname;
```

Then start the backend and check the probes:

| Endpoint | Meaning |
|---|---|
| `GET /api/v1/health` | Liveness — always 200 while the app is up; never fails on a DB outage |
| `GET /actuator/health/liveness` | Liveness (`livenessState` only; the `db` indicator is deliberately excluded) |
| `GET /actuator/health/readiness` | Readiness (`readinessState` + `db`); returns **503** when PostgreSQL is down |

Readiness must report `db: UP` only after Step 4 has succeeded. The fast test
suite (`cd backend && mvn -B clean verify`) uses H2 in `MODE=PostgreSQL` and
needs no database; `PostgresMigrationIntegrationTest` applies the real V1
migration to a PostgreSQL Testcontainers instance and is **skipped
automatically when Docker is unavailable** — run it in CI for genuine
PostgreSQL evidence (never report a skipped run as a passed one).

> Historical note: the Goal 02 verification evidence in
> [`docs/progress.md`](../progress.md) was produced against the old local
> MySQL 8 instance. That baseline is superseded by this fresh PostgreSQL
> schema; no MySQL data was carried over.

---

## 2. Environment configuration

### Backend

```bash
cp .env.example .env
```

Then edit `.env` and set at least:

```bash
DB_URL=jdbc:postgresql://<POOLER_HOST>:5432/postgres?sslmode=require
DB_USERNAME=<DB_APP_ROLE>
DB_PASSWORD=<DB_APP_PASSWORD>
DB_DRIVER=org.postgresql.Driver
DB_DIALECT=org.hibernate.dialect.PostgreSQLDialect
DB_DDL_AUTO=validate

# Goal 02 — generate a >=32-byte secret, e.g. `openssl rand -base64 48`
JWT_SECRET=<paste-generated-secret-here>
```

Use the **Session Pooler** connection for the running app and the
least-privilege runtime role — never the `postgres` superuser. The
`DB_MIGRATION_*` credentials belong only to the separate Flyway step
(§1 Step 4), not to the running application.

`.env` is git-ignored and must never be committed. Leaving `JWT_SECRET` blank
makes the backend **refuse to start** (outside `dev`/`test`) rather than
silently using a weak key. `JWT_ALLOW_EPHEMERAL_DEV_KEY=true` is an emergency
local-dev escape hatch whose key invalidates all tokens on restart.

> **Verified on this machine (2026-11-12):** a >=32-byte random `JWT_SECRET`
> is set in the ignored `.env`, `JWT_ALLOW_EPHEMERAL_DEV_KEY=false`, access TTL
> `900` / refresh TTL `604800`. Starting the jar with `JWT_SECRET` removed and
> the escape hatch off was confirmed to fail fast with
> `IllegalStateException: JWT_SECRET is not configured` (exit 1) — no weak or
> ephemeral key was used. Never print or commit the secret.

> The Spring backend does not read a `.env` file by itself. Export the
> variables in your shell before starting, or paste them into your IDE's run
> configuration.
>
> **Linux / macOS / Git Bash:**
>
> ```bash
> export $(grep -v '^#' .env | xargs)
> ```
>
> **Windows / PowerShell (recommended on this machine):** the `export` line
> above is a Bash command and does nothing in PowerShell. Running
> `mvn spring-boot:run` directly therefore leaves `JWT_SECRET` blank and the
> backend fails fast with `IllegalStateException: JWT_SECRET is not
> configured`. Use the repository-root launcher instead, from the repository
> root:
>
> ```powershell
> .\start-backend.ps1
> ```
>
> It reads the root `.env`, sets each value as a process-scoped environment
> variable (inherited by Maven and the JVM it forks), never prints any value,
> leaves variables already set in the environment untouched (so real CI / IDE /
> production configuration wins), and clears them again when the backend stops.
> See `start-backend.ps1` for the full description. If your execution policy
> blocks local scripts, run
> `powershell -ExecutionPolicy Bypass -File .\start-backend.ps1`.

### First administrator (Goal 02, one-time)

There is no public admin registration. Either enable the opt-in bootstrap in
`.env` for exactly one startup:

```bash
APP_BOOTSTRAP_ADMIN_ENABLED=true
APP_BOOTSTRAP_ADMIN_EMAIL=admin@example.com
APP_BOOTSTRAP_ADMIN_PASSWORD=<strong-password>
APP_BOOTSTRAP_ADMIN_FULL_NAME=...
APP_BOOTSTRAP_ADMIN_PHONE=...
```

…then set `APP_BOOTSTRAP_ADMIN_ENABLED=false` again immediately after the first
successful start. The runner re-checks for an existing `SUPER_ADMIN` inside a
transaction and relies on the unique constraints, so concurrent startup
cannot create duplicate administrators; it never logs the password. A
commented, non-runnable SQL template for the old MySQL schema is kept only
as a historical reference at
`infrastructure/database/legacy/mysql/04_admin_bootstrap_TEMPLATE.sql` — the
environment-variable runner above is the supported path.

> **Verified on this machine (2026-11-12):** the opt-in runner was used once;
> exactly one `SUPER_ADMIN` (`ACTIVE`) exists, successfully logged in and
> reached privileged endpoints, and `APP_BOOTSTRAP_ADMIN_ENABLED` is `false`
> again in `.env`. No bootstrap credential is committed.

### Customer and Rider apps

```bash
cp customer-app/.env.example customer-app/.env
cp rider-app/.env.example rider-app/.env
```

The defaults target the Android emulator. Adjust for your situation:

| Target | API base URL |
|---|---|
| Android emulator | `http://10.0.2.2:8080/api/v1` |
| iOS simulator | `http://localhost:8080/api/v1` |
| Physical device (same Wi-Fi) | `http://<your-LAN-IP>:8080/api/v1` |

### Admin web

```bash
cp admin-web/.env.example admin-web/.env
```

The default `http://localhost:8080/api/v1` is correct for browser use.

---

## 3. Running the backend

### Windows / PowerShell (documented command)

From the **repository root**:

```powershell
.\start-backend.ps1
```

This loads the root `.env` and then runs `mvn spring-boot:run` from `backend/`.
Prerequisites: JDK 21 and Maven on `PATH` (see [Toolchain](#0-toolchain)).
If the execution policy blocks the script, use
`powershell -ExecutionPolicy Bypass -File .\start-backend.ps1`.

### Linux / macOS / Git Bash, or an IDE run configuration

The Spring backend does not read `.env` by itself, so export the variables
first (or set them in the IDE run configuration) before starting:

```bash
cd backend
export $(grep -v '^#' ../.env | xargs)
mvn spring-boot:run
```

Or build a jar and run it:

```bash
cd backend
mvn clean package
java -jar target/bd-quickmart-backend-0.1.0.jar
```

### Verify it started

```bash
curl http://localhost:8080/api/v1/health
```

Expected response when PostgreSQL is reachable:

```json
{
  "success": true,
  "code": "BDQM-0000",
  "message": "Service is running",
  "data": {
    "status": "UP",
    "application": "bd-quickmart-backend",
    "database": "UP",
    "details": { "database": "UP" }
  },
  "path": "/api/v1/health"
}
```

> **If `database` reads `DOWN`:** the backend is running correctly but cannot
> reach PostgreSQL. Check the Supabase project is active, that the connection
> string uses `sslmode=require`, that the Session Pooler host/port is correct,
> and that `DB_URL`, `DB_USERNAME` and `DB_PASSWORD` match the runtime role.
> `/api/v1/health` is **liveness** — it stays HTTP 200 during a DB outage so
> the platform does not restart a healthy process; use
> `/actuator/health/readiness` for the DB-aware readiness probe (it returns
> 503 while the database is unreachable). The check is bounded to 2 seconds,
> so the endpoint never hangs.

### Verify authentication (Goal 02)

With the Flyway `V1__init_auth_schema.sql` migration applied (§1 Step 4) and
`JWT_SECRET` set, register and log in from the command line:

```bash
# Public registration (CUSTOMER or RIDER only)
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"fullName":"Test Customer","email":"c@example.com","phone":"01700000001","password":"password123","role":"CUSTOMER"}'

# Login with email or phone
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"identifier":"c@example.com","password":"password123"}'
```

The login response `data` contains `accessToken`, `refreshToken` and a safe
`user` projection (no password hash). Use the access token as
`Authorization: Bearer <token>` on `GET /api/v1/auth/me`. A `RIDER`
registration returns `status: PENDING`; it can log in but cannot reach
`/api/v1/rider/**` until an `ADMIN`/`SUPER_ADMIN` approves it via
`POST /api/v1/admin/riders/{publicId}/approve`.

The fast application-level tests cover this flow end-to-end against an
in-memory H2 database in `MODE=PostgreSQL` (`cd backend && mvn -B clean
verify`), so no running database is needed. H2 is **not** a substitute for
real PostgreSQL: `PostgresMigrationIntegrationTest` applies the actual Flyway
V1 migration to a PostgreSQL Testcontainers instance and is skipped
automatically when Docker is unavailable — run it in CI for genuine
PostgreSQL evidence, and never report a skipped run as passed.

### Swagger UI

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>

### WebSocket endpoint

The live-tracking transport is registered at `/ws/tracking`. Verify the
handshake from the command line:

```bash
curl -D - -o /dev/null \
  -H "Connection: Upgrade" \
  -H "Upgrade: websocket" \
  -H "Sec-WebSocket-Version: 13" \
  -H "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==" \
  http://localhost:8080/ws/tracking
```

A `HTTP/1.1 101` response with a `Sec-WebSocket-Accept` header means the
endpoint is reachable. A `403` means the security filter is blocking the
handshake — see the note in the troubleshooting section.

---

## 4. Running the Customer app

```bash
cd customer-app
flutter pub get
flutter run
```

With a device connected, `flutter devices` lists the available targets. An
Android emulator or a physical device works.

### Verify it builds

```bash
flutter analyze   # expect: No issues found!
flutter test      # expect: +14: All tests passed!
```

### Build an installable debug APK

```bash
flutter build apk --debug
# → build/app/outputs/flutter-apk/app-debug.apk  (166,090,410 bytes)
```

A verified delivery copy is kept at
`installers/BD-QuickMart-Customer.apk` — see
[Verified installable debug APKs](#verified-installable-debug-apks).

Install it on a connected device/emulator:

```bash
adb install -r build/app/outputs/flutter-apk/app-debug.apk
```

The debug APK has application ID `com.bdquickmart.customer_app`, declares the
`INTERNET` permission and permits HTTP cleartext to the dev backend (release
builds do not). On a physical device, open the sign-in screen's **Change server
URL** dialog and replace `10.0.2.2` with your machine's LAN IP.

---

## 5. Running the Rider app

```bash
cd rider-app
flutter pub get
flutter run
```

The rider app is a separate application with its own package (`rider_app`),
its own entry point and its own identity. Run it independently of the customer
app.

### Verify it builds

```bash
flutter analyze   # expect: No issues found!
flutter test      # expect: +16: All tests passed!
```

### Build an installable debug APK

```bash
flutter build apk --debug
# → build/app/outputs/flutter-apk/app-debug.apk  (167,905,227 bytes)
```

A verified delivery copy is kept at
`installers/BD-QuickMart-Rider.apk` — see
[Verified installable debug APKs](#verified-installable-debug-apks).

Install it on a connected device/emulator:

```bash
adb install -r build/app/outputs/flutter-apk/app-debug.apk
```

Its application ID is `com.bdquickmart.rider_app` — deliberately distinct from
the customer app so both can be installed on the same device. New riders
register as `PENDING` and see an in-app "Awaiting approval" screen until an
administrator approves them; "Check approval status" re-reads the account.

---

## 6. Running the Admin web

```bash
cd admin-web
npm install
npm run dev
```

Vite prints the local URL, normally <http://localhost:5173>. The console is
guarded: unauthenticated visits redirect to `/login`. Sign in with a privileged
account (`ADMIN`, `SUPER_ADMIN`, `STORE_MANAGER` or `INVENTORY_OPERATOR`);
`CUSTOMER` and `RIDER` accounts are rejected. The access token is held in
memory only and the refresh token in `sessionStorage`, with a single-flight
silent refresh on `401`. This is **not** HttpOnly-cookie protection and is
XSS-exposed — see `admin-web/src/features/auth/session.ts`.

### Verify it builds

```bash
npm run lint     # expect: 0 warnings and 0 errors
npm run build    # expect: built successfully
```

> **Verified on this machine (2026-11-12):** `npm run lint` returned
> `Found 0 warnings and 0 errors.` (24 files, 116 rules) and `npm run build`
> transformed 98 modules (`✓ built in 1.25s`). Browser-level E2E (privileged
> login, CUSTOMER/RIDER denial, expired session, refresh failure, logout) was
> **not** automated; it remains a manual developer acceptance step. See
> [docs/progress.md](../progress.md).

---

## 7. Full verification sweep

Run from the repository root:

```bash
# Backend
cd backend && mvn -B clean verify

# Customer app
cd ../customer-app && flutter analyze && flutter test && flutter build apk --debug

# Rider app
cd ../rider-app && flutter analyze && flutter test && flutter build apk --debug

# Admin web
cd ../admin-web && npm run lint && npm run build
```

### Verified results (2026-11-12)

The full sweep was re-run after the final source and documentation changes.
All commands exited `0`:

| Check | Result |
|---|---|
| `mvn -B clean verify` | `BUILD SUCCESS` — `Tests run: 27, Failures: 0, Errors: 0, Skipped: 0` |
| `customer-app`: `flutter analyze` | `No issues found!` |
| `customer-app`: `flutter test` | `+14: All tests passed!` |
| `customer-app`: `flutter build apk --debug` | `app-debug.apk` built, 166,090,410 bytes |
| `rider-app`: `flutter analyze` | `No issues found!` |
| `rider-app`: `flutter test` | `+16: All tests passed!` |
| `rider-app`: `flutter build apk --debug` | `app-debug.apk` built, 167,905,227 bytes |
| `admin-web`: `npm run lint` | `Found 0 warnings and 0 errors.` (24 files, 116 rules) |
| `admin-web`: `npm run build` | `✓ 98 modules transformed`, `✓ built in 1.25s` |

**Total automated tests: 57** (backend 27 + customer 14 + rider 16). Admin web
has no test runner and is verified via lint + production build. The freshly
rebuilt APKs were byte-identical to the delivery copies below, so the recorded
SHA-256 hashes did not change.

> **Post-migration note:** the table above is the dated Goal 02 baseline, kept
> verbatim. After the MySQL → Supabase PostgreSQL migration the backend suite
> grew to **44 tests** (`Tests run: 44, Failures: 0, Errors: 0`), of which 9
> real-PostgreSQL Testcontainers tests skip cleanly when Docker is unavailable
> (35 execute and pass). Those skipped tests are **not** reported as passed;
> run them in CI with Docker for genuine PostgreSQL evidence.

### Verified installable debug APKs

Final delivery copies (git-ignored, **not** committed):

| App | Build output | Delivery copy | Size (bytes) | SHA-256 | Application ID |
|---|---|---|---|---|---|
| Customer | `customer-app/build/app/outputs/flutter-apk/app-debug.apk` | `installers/BD-QuickMart-Customer.apk` | 166,090,410 | `0ad369221d418b4a527fdb42b7aed8eaae1f90ea82c583ff9f89efa61a9ba225` | `com.bdquickmart.customer_app` |
| Rider | `rider-app/build/app/outputs/flutter-apk/app-debug.apk` | `installers/BD-QuickMart-Rider.apk` | 167,905,227 | `25b91d222b60c43a43e95624172c4b3731caf40204cf6e28c55d015524cda5ce` | `com.bdquickmart.rider_app` |

Both are `versionName 0.1.0`, minSdk 24 / targetSdk 36, declare
`android.permission.INTERNET`, and enable cleartext only in debug/profile
manifests. No production secrets are embedded in either APK.

Install on a connected device/emulator with:

```bash
adb install -r installers/BD-QuickMart-Customer.apk
adb install -r installers/BD-QuickMart-Rider.apk
```

**Device networking:** the shipped default is the Android-emulator URL
`http://10.0.2.2:8080/api/v1`. On a physical device that address does not
resolve — open the sign-in screen's **Change server URL** dialog (or build with
`flutter run --dart-define API_BASE_URL=http://<PC-LAN-IP>:8080/api/v1`) and
point at your machine's LAN IP, with the phone and PC on the same Wi-Fi and the
firewall allowing port 8080. Debug/profile builds permit HTTP cleartext to that
host; release builds do not.

> **No device attached (2026-11-12):** `adb devices` returned no attached
> device/emulator, so `adb install` and any on-device walkthrough were **not**
> performed and are not claimed. The APKs were verified by a successful build,
> `aapt dump badging` / `aapt dump xmltree`, size/SHA-256 recording and a
> production-secret scan. On-device installation and the in-app customer /
> rider approval walkthrough remain manual developer acceptance steps.

---

## Troubleshooting

**`java: command not found`** — `JAVA_HOME` is not set or `PATH` does not
include `$JAVA_HOME/bin`. Open a new terminal after changing it.

**`mvn: command not found`** — Maven is not on `PATH`. Set `MAVEN_HOME` and add
`$MAVEN_HOME/bin`.

**Backend fails with `Unable to determine Dialect without JDBC metadata`** —
this happens when the dialect is not pinned. The repository pins
`DB_DIALECT=org.hibernate.dialect.PostgreSQLDialect`; confirm your local
`application.yml` still contains it. (The old `DB_DIALECT_VERSION` MySQL
workaround has been removed.)

**Backend refuses to start with `JWT_SECRET is not configured`** — Goal 02
fails fast rather than signing tokens with a weak or hardcoded key. Generate a
secret (`openssl rand -base64 48`) and set `JWT_SECRET` in `.env` / your IDE
run configuration. Only for local development you may instead set
`JWT_ALLOW_EPHEMERAL_DEV_KEY=true`, but every token then invalidates on
restart.

On Windows PowerShell the usual cause is that the root `.env` was never
loaded: `mvn spring-boot:run` does not read `.env` on its own, so the
`${JWT_SECRET:}` placeholder resolves to blank and `JwtConfig` throws at
startup. Start with `.\start-backend.ps1` from the repository root instead
(see [Running the backend](#3-running-the-backend)). Do not "fix" this by
enabling `JWT_ALLOW_EPHEMERAL_DEV_KEY` — that only masks the missing
configuration and invalidates every token on restart.

**Auth endpoints return 500 / "relation \"users\" does not exist"** — the
Flyway migration has not been applied yet. The running application has
`spring.flyway.enabled=false` and `ddl-auto: none`, so it will **not** create
tables at startup. Run the separate migration step (§1 Step 4) against the
Supabase **direct** connection with the DDL-capable migration role, then
confirm with `select * from flyway_schema_history;` and restart the backend.

**A new RIDER can log in but cannot open rider screens** — rider accounts
start `PENDING` and only `ACTIVE` riders receive the `PERM_RIDER_ACTIVE`
authority. Approve the account as an `ADMIN`/`SUPER_ADMIN` via
`POST /api/v1/admin/riders/{publicId}/approve`, then use "Check approval
status" in the app (a token rotation picks up the new status).

**Backend starts but health reports `database: DOWN`** — PostgreSQL is
unreachable or the credentials are wrong. Re-check the Supabase project is
active, the connection string uses `sslmode=require`, the Session Pooler
host/port is correct, and that `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` match
the runtime role (§1 Steps 2, 3 and 5). The probe is bounded to 2 seconds,
so a `DOWN` result arrives quickly rather than hanging. `/api/v1/health`
stays HTTP 200 during a DB outage by design; `/actuator/health/readiness`
returns 503 until the database is reachable.

**Cannot connect to Supabase PostgreSQL** — confirm the project is not paused,
that you are using the **Session Pooler** (port 5432) for the running app and
the **direct** connection (port 5432, `db.<ref>.supabase.co`) for migrations,
and that `sslmode=require` is present. Avoid the transaction-mode pooler
(port 6543): it breaks session state and the `SELECT ... FOR UPDATE`
pessimistic locking used by refresh-token rotation. Verify reachability from
the host:

```powershell
Test-NetConnection <POOLER_HOST> -Port 5432
```

If the connection is refused, check the Supabase dashboard, network egress and
any IP allow-list rather than restarting a local service — the database is a
managed cloud project, not a local `mysqld` process.

**WebSocket handshake returns `403`** — the security filter chain is
intercepting the upgrade request before the WebSocket handler sees it. The
`/ws/**` path must be permitted in `SecurityConfig`; authentication of the
tracking connection itself arrives in Goal 08.

**`Port 8080 was already in use`** — a previous backend process is still
running. Stopping the app from the IDE, or stopping a `mvn spring-boot:run`
task, can leave the forked JVM alive and the port bound. Find and stop it:

```powershell
Get-NetTCPConnection -LocalPort 8080 -State Listen | Select-Object OwningProcess
Stop-Process -Id <pid> -Force
```

Confirm the port is free before starting again:

```powershell
if (Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue) {
  'STILL HELD'
} else {
  'PORT 8080 RELEASED'
}
```

**Flutter cannot find the Android SDK** — set `ANDROID_HOME` and
`ANDROID_SDK_ROOT` to `C:\Users\USER\AppData\Local\Android\Sdk`, then run
`flutter doctor`.

**`flutter pub get` fails on a package** — run `flutter pub upgrade` and retry.

**Admin web shows CORS errors in the browser console** — the origin must be in
`APP_CORS_ALLOWED_ORIGINS`. The Vite default origin `http://localhost:5173` is
already included. If you changed the Vite port, add the new origin and restart
the backend.

**A physical device cannot reach the backend** — use your machine's LAN IP
instead of `localhost` or `10.0.2.2`, and make sure the phone and computer are
on the same network with the firewall allowing the port.
