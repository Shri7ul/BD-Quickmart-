# BD QuickMart — Local Setup Guide

This guide takes a fresh machine from nothing to three running applications and
a connected database.

> **Assumed:** Windows 10/11, XAMPP installed, and the repository cloned.

---

## 0. Toolchain

Goal 01 was developed and verified with the following versions.

| Tool | Version | Install location used |
|---|---|---|
| Java (Temurin) | 21.0.13 LTS | `C:\Users\USER\tools\jdk-21.0.13+9` |
| Apache Maven | 3.9.9 | `C:\Users\USER\tools\apache-maven-3.9.9` |
| Flutter | 3.35.1 (Dart 3.9.0) | `C:\Users\USER\tools\flutter` |
| Node.js | 22.22.2 | system install |
| Android SDK | platforms 34 & 36, build-tools 34–36.1 | `C:\Users\USER\AppData\Local\Android\Sdk` |
| MySQL | 8 | XAMPP (`C:\xampp`) |

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

## 1. Database setup in XAMPP — developer task

**The backend does not create the database for you.** It is created and
managed manually, so that no automated process touches your local MySQL
instance.

### Step 1 — Start MySQL

Open the **XAMPP Control Panel** and click **Start** next to **MySQL**.

The status must read *running*. If the port is already taken, see
[Troubleshooting](#troubleshooting).

### Step 2 — Create the database

There are two ways. Either is fine.

**Option A — phpMyAdmin (recommended)**

1. In the XAMPP Control Panel, click **Admin** next to MySQL. This opens
   `http://localhost/phpmyadmin`.
2. Click the **SQL** tab in the top menu.
3. Open `infrastructure/database/init/00_create_database.sql` in your editor
   and copy its contents.
4. Paste into the SQL editor and press **Go**.

**Option B — MySQL command line**

```bash
"C:/xampp/mysql/bin/mysql.exe" -u root < infrastructure/database/init/00_create_database.sql
```

> If your XAMPP MySQL root account has a password, add `-p` and supply it.
> This script only creates the `bd_quickmart` database. It does **not** drop,
> truncate or modify any other database on the server, and it is safe to run
> more than once.

### Step 3 — Import the Goal 02 auth tables

Goal 02 adds two tables. Run
`infrastructure/database/init/02_create_auth_tables.sql` through the phpMyAdmin
**SQL** tab (or the CLI, as in Option B above). It creates `users` and
`refresh_tokens` with `CREATE TABLE IF NOT EXISTS`, unique indexes, a foreign
key, InnoDB and `utf8mb4_unicode_ci`. It contains no `DROP`, `TRUNCATE` or
`DELETE` and is safe to run repeatedly.

**The backend cannot serve auth requests until this script has been run.**
Hibernate is pinned to `ddl-auto: none` and will not create the tables for you.

### Step 4 — Verify

Run `infrastructure/database/init/01_verify_connection.sql` and then
`infrastructure/database/init/03_verify_auth_tables.sql` the same way. You
expect to see:

- `current_database` = `bd_quickmart`
- `charset` = `utf8mb4`
- `collation` = `utf8mb4_unicode_ci`
- `03_verify_auth_tables.sql` returns exactly two rows: `refresh_tokens` and
  `users`, both `InnoDB` / `utf8mb4_unicode_ci`

Before Goal 02's script is imported, the table list is empty — that is the
correct Goal 01 state. If you see tables you did not create, stop and check
which script was run.

---

## 2. Environment configuration

### Backend

```bash
cp .env.example .env
```

Then edit `.env` and set at least:

```bash
DB_URL=jdbc:mysql://localhost:3306/bd_quickmart?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Dhaka&characterEncoding=UTF-8
DB_USERNAME=root
DB_PASSWORD=              # your XAMPP root password, if any

# Goal 02 — generate a >=32-byte secret, e.g. `openssl rand -base64 48`
JWT_SECRET=<paste-generated-secret-here>
```

`.env` is git-ignored and must never be committed. Leaving `JWT_SECRET` blank
makes the backend **refuse to start** (outside `dev`/`test`) rather than
silently using a weak key. `JWT_ALLOW_EPHEMERAL_DEV_KEY=true` is an emergency
local-dev escape hatch whose key invalidates all tokens on restart.

> The Spring backend does not read a `.env` file by itself. Export the
> variables in your shell before starting, or paste them into your IDE's run
> configuration:
>
> ```bash
> export $(grep -v '^#' .env | xargs)
> ```

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
successful start. Or insert a `SUPER_ADMIN` row manually with a BCrypt hash,
using the commented template in
`infrastructure/database/init/04_admin_bootstrap_TEMPLATE.sql`. The runner
skips creation when a `SUPER_ADMIN` already exists and never logs the
password.

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

```bash
cd backend
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

Expected response when MySQL is running:

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
> reach MySQL. Check that MySQL is started in XAMPP and that `DB_URL`,
> `DB_USERNAME` and `DB_PASSWORD` are correct. The backend deliberately
> starts even when the database is down, so that you can diagnose the problem
> from the health endpoint instead of from a stack trace. The check is
> bounded to 2 seconds, so the endpoint never hangs.

### Verify authentication (Goal 02)

With `02_create_auth_tables.sql` imported and `JWT_SECRET` set, register and
log in from the command line:

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

The tests cover this flow end-to-end against an in-memory H2 database
(`cd backend && mvn -B clean verify`), so the backend does not need MySQL
running to be verified.

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
# → build/app/outputs/flutter-apk/app-debug.apk  (~159 MB)
```

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
# → build/app/outputs/flutter-apk/app-debug.apk  (~161 MB)
```

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

---

## Troubleshooting

**`java: command not found`** — `JAVA_HOME` is not set or `PATH` does not
include `$JAVA_HOME/bin`. Open a new terminal after changing it.

**`mvn: command not found`** — Maven is not on `PATH`. Set `MAVEN_HOME` and add
`$MAVEN_HOME/bin`.

**Backend fails with `Unable to determine Dialect without JDBC metadata`** —
this happens when the dialect is not pinned. The repository pins
`DB_DIALECT=org.hibernate.dialect.MySQLDialect`; confirm your local
`application.yml` still contains it.

**Backend refuses to start with `JWT_SECRET is not configured`** — Goal 02
fails fast rather than signing tokens with a weak or hardcoded key. Generate a
secret (`openssl rand -base64 48`) and set `JWT_SECRET` in `.env` / your IDE
run configuration. Only for local development you may instead set
`JWT_ALLOW_EPHEMERAL_DEV_KEY=true`, but every token then invalidates on
restart.

**Auth endpoints return 500 / "Table 'bd_quickmart.users' doesn't exist"** —
`02_create_auth_tables.sql` has not been imported yet. Hibernate is pinned to
`ddl-auto: none` and will not create the tables. Run the script in phpMyAdmin
(see Step 3) and restart the backend.

**A new RIDER can log in but cannot open rider screens** — rider accounts
start `PENDING` and only `ACTIVE` riders receive the `PERM_RIDER_ACTIVE`
authority. Approve the account as an `ADMIN`/`SUPER_ADMIN` via
`POST /api/v1/admin/riders/{publicId}/approve`, then use "Check approval
status" in the app (a token rotation picks up the new status).

**Backend starts but health reports `database: DOWN`** — MySQL is not running
or the credentials are wrong. Re-check Step 1 and Step 2 above. The check is
bounded to 2 seconds, so a `DOWN` result arrives quickly rather than hanging.

**MySQL stopped unexpectedly** — the XAMPP MySQL service can stop between
sessions. Confirm whether it is running:

```powershell
Get-NetTCPConnection -LocalPort 3306 -State Listen -ErrorAction SilentlyContinue
Get-Process mysqld -ErrorAction SilentlyContinue
```

If both come back empty, restart it from the XAMPP Control Panel. To start
`mysqld` directly from a terminal instead, note that the defaults file lives
in the **bin** directory, not the `mysql` directory:

```bash
"C:/xampp/mysql/bin/mysqld.exe" --defaults-file="C:/xampp/mysql/bin/my.ini"
```

Using `C:/xampp/mysql/my.ini` fails with
`Could not open required defaults file`.

**`Port 3306 is already in use`** — another MySQL instance is running. Either
stop it, or set `DB_URL` to the port your instance uses. To find the
occupying process on Windows: `netstat -ano | findstr 3306`, then look up the
PID in Task Manager.

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
