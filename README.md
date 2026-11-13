# BD QuickMart

AI-powered hyperlocal quick-commerce platform for Bangladesh.

Three client applications, one centralized Java Spring Boot backend.

| Application | Stack | Purpose |
|---|---|---|
| **Customer App** | Flutter (Android, iOS) | Browse, search, order, live tracking |
| **Rider App** | Flutter (Android, iOS) | Accept deliveries, navigate routes, earnings |
| **Admin Web** | React + TypeScript (Vite) | Store operations, inventory, dispatch, analytics |
| **Backend** | Java 21 + Spring Boot 3.5 | REST API `/api/v1`, WebSocket, JPA/MySQL |

---

## Architecture

**Modular monolith.** One deployable Spring Boot application, partitioned into
15 strict module boundaries (`auth`, `user`, `customer`, `rider`, `store`,
`product`, `inventory`, `cart`, `order`, `payment`, `delivery`, `tracking`,
`ai`, `notification`, `analytics`).

No microservices, no Kubernetes, no message broker. See
[docs/architecture/system-overview.md](docs/architecture/system-overview.md).

```
customer-app/  rider-app/  admin-web/  ──HTTP/WS──▶  backend/  ──JDBC──▶  MySQL 8
```

---

## Repository structure

```
bd-quickmart/
├── backend/          Spring Boot modular monolith
├── customer-app/     Flutter customer mobile app
├── rider-app/        Flutter rider mobile app
├── admin-web/        React + TypeScript admin console
├── ai/               AI provider prompts and evaluation assets
├── infrastructure/   Database scripts and local infrastructure
├── docs/             Architecture, setup and progress documentation
└── README.md
```

Each application is independently runnable and independently buildable.

---

## Quick start

Full instructions: **[docs/development/local-setup.md](docs/development/local-setup.md)**

### 1. Database (developer task, in XAMPP)

Start MySQL from the XAMPP Control Panel, then open
<http://localhost/phpmyadmin> → **SQL** tab and run these scripts in order:

1. `infrastructure/database/init/00_create_database.sql` — creates
   `bd_quickmart` with `utf8mb4` / `utf8mb4_unicode_ci`.
2. `infrastructure/database/init/02_create_auth_tables.sql` — creates the
   `users` and `refresh_tokens` tables (Goal 02).
3. `infrastructure/database/init/03_verify_auth_tables.sql` — read-only check
   that both tables exist with the expected engine and collation.

**The repository never creates or modifies a database for you.** It ships the
scripts and the documentation; you run them. The scripts use
`CREATE TABLE IF NOT EXISTS` and contain no `DROP`, `TRUNCATE` or `DELETE`.

### 2. Environment

```bash
cp .env.example .env
cp customer-app/.env.example customer-app/.env
cp rider-app/.env.example rider-app/.env
cp admin-web/.env.example admin-web/.env
```

Set `DB_USERNAME` / `DB_PASSWORD` in the root `.env` to match your XAMPP MySQL
account, and generate a JWT signing secret (at least 32 bytes):

```bash
openssl rand -base64 48
```

Put the result in `JWT_SECRET`. Leaving it blank makes the backend **refuse to
start** outside the `dev`/`test` profiles rather than silently using a weak
key. `JWT_ALLOW_EPHEMERAL_DEV_KEY=true` is an emergency local-dev escape hatch
only: the generated key is ephemeral and invalidates every token on restart.

### 3. First administrator (one-time)

There is no public admin registration. Enable the opt-in bootstrap in the root
`.env`:

```bash
APP_BOOTSTRAP_ADMIN_ENABLED=true
APP_BOOTSTRAP_ADMIN_EMAIL=admin@example.com
APP_BOOTSTRAP_ADMIN_PASSWORD=<strong-password>
APP_BOOTSTRAP_ADMIN_FULL_NAME=...
APP_BOOTSTRAP_ADMIN_PHONE=...
```

Start the backend once, then set `APP_BOOTSTRAP_ADMIN_ENABLED=false` again.
Alternatively, insert a `SUPER_ADMIN` row manually with a BCrypt hash — see
`infrastructure/database/init/04_admin_bootstrap_TEMPLATE.sql`.

### 4. Run

**Windows / PowerShell (recommended on this machine):** from the repository
root, start the backend with the `.env` launcher:

```powershell
.\start-backend.ps1
```

`mvn spring-boot:run` does not read `.env` by itself; the launcher loads the
root `.env` into the backend process without printing any secret, already-set
environment variables are not overridden, and the shell is restored on exit.
If your execution policy blocks local scripts, use
`powershell -ExecutionPolicy Bypass -File .\start-backend.ps1`.

**Linux / macOS / Git Bash / IDE:** export the root `.env` first (or set the
values in your IDE run configuration) — see
[local-setup.md](docs/development/local-setup.md#3-running-the-backend).

```bash
# Backend — http://localhost:8080
cd backend && mvn spring-boot:run

# Customer app
cd customer-app && flutter pub get && flutter run

# Rider app
cd rider-app && flutter pub get && flutter run

# Admin web — http://localhost:5173
cd admin-web && npm install && npm run dev
```

On a physical Android device, open the in-app **Change server URL** dialog on
the sign-in screen and set the backend's LAN IP. Debug/profile builds allow
HTTP cleartext to that host; release builds keep cleartext disabled.

---

## Verification

| Check | Command | Expected |
|---|---|---|
| Backend tests | `cd backend && mvn -B clean verify` | `Tests run: 27, Failures: 0, Errors: 0` |
| Backend package | `cd backend && mvn -B package` | `BUILD SUCCESS` |
| Health endpoint | `curl http://localhost:8080/api/v1/health` | `"success": true` |
| Swagger UI | <http://localhost:8080/swagger-ui.html> | HTTP 200 |
| OpenAPI document | <http://localhost:8080/v3/api-docs> | HTTP 200, OpenAPI 3.1 |
| Customer analysis | `cd customer-app && flutter analyze` | `No issues found!` |
| Customer tests | `cd customer-app && flutter test` | `+14: All tests passed!` |
| Rider analysis | `cd rider-app && flutter analyze` | `No issues found!` |
| Rider tests | `cd rider-app && flutter test` | `+16: All tests passed!` |
| Admin lint | `cd admin-web && npm run lint` | `0 warnings and 0 errors` |
| Admin build | `cd admin-web && npm run build` | `✓ built` |
| Customer debug APK | `cd customer-app && flutter build apk --debug` | `✓ Built build/app/outputs/flutter-apk/app-debug.apk` |
| Rider debug APK | `cd rider-app && flutter build apk --debug` | `✓ Built build/app/outputs/flutter-apk/app-debug.apk` |

#### Installable debug APKs

Final delivery copies (git-ignored, not committed):

| App | Path | Size (bytes) | SHA-256 | Application ID |
|---|---|---|---|---|
| Customer | `installers/BD-QuickMart-Customer.apk` | 166,090,410 | `0ad369221d418b4a527fdb42b7aed8eaae1f90ea82c583ff9f89efa61a9ba225` | `com.bdquickmart.customer_app` |
| Rider | `installers/BD-QuickMart-Rider.apk` | 167,905,227 | `25b91d222b60c43a43e95624172c4b3731caf40204cf6e28c55d015524cda5ce` | `com.bdquickmart.rider_app` |

Both target the Android emulator at `http://10.0.2.2:8080/api/v1` by default.
On a physical device, use the sign-in screen's **Change server URL** dialog to
point at `http://<PC-LAN-IP>:8080/api/v1` — no rebuild is needed. See
[docs/development/local-setup.md](docs/development/local-setup.md).

Latest recorded results: [docs/progress.md](docs/progress.md).

---

## API

- Base path: `/api/v1`
- Health: `GET /api/v1/health`
- OpenAPI: `GET /v3/api-docs`
- Swagger UI: `/swagger-ui.html`
- WebSocket: `/ws/tracking`

### Authentication (Goal 02)

| Method | Endpoint | Auth | Purpose |
|---|---|---|---|
| POST | `/api/v1/auth/register` | public | Self-register a `CUSTOMER` or `RIDER` |
| POST | `/api/v1/auth/login` | public | Login with email or phone |
| POST | `/api/v1/auth/refresh` | public | Rotate a refresh token |
| POST | `/api/v1/auth/logout` | bearer | Revoke all of the user's refresh tokens |
| GET | `/api/v1/auth/me` | bearer | Current user |
| GET | `/api/v1/customer/ping` | `ROLE_CUSTOMER` | Role probe |
| GET | `/api/v1/rider/ping` | `PERM_RIDER_ACTIVE` | Role probe (ACTIVE riders only) |
| GET | `/api/v1/admin/ping` | privileged roles | Role probe |
| GET | `/api/v1/admin/riders/pending` | `PERM_APPROVE_RIDERS` | List PENDING riders |
| POST | `/api/v1/admin/riders/{id}/approve` | `PERM_APPROVE_RIDERS` | Approve a rider |
| POST | `/api/v1/admin/riders/{id}/reject` | `PERM_APPROVE_RIDERS` | Reject a rider |
| POST | `/api/v1/admin/users/provision` | `PERM_PROVISION_PRIVILEGED` | Create a privileged account |

Six canonical roles: `CUSTOMER`, `RIDER`, `ADMIN`, `SUPER_ADMIN`,
`STORE_MANAGER`, `INVENTORY_OPERATOR`. Public registration only allows
`CUSTOMER` and `RIDER`; new riders start `PENDING` and must be approved by an
`ADMIN` / `SUPER_ADMIN`. Access tokens are short-lived HS256 JWTs; refresh
tokens are stored only as SHA-256 hashes, rotated on every use, with replay
detection that revokes the whole token family.

Every endpoint returns the same envelope:

```json
{
  "success": true,
  "code": "BDQM-0000",
  "message": "Request completed successfully",
  "data": {},
  "path": "/api/v1/health",
  "timestamp": "2026-11-08T18:48:24.305651Z",
  "traceId": "..."
}
```

Goal 02 adds the error codes `BDQM-0402` (invalid credentials), `BDQM-0410`
(invalid token), `BDQM-0411` (expired token) and `BDQM-0412` (inactive
account).

---

## Documentation

| Document | Contents |
|---|---|
| [docs/architecture/system-overview.md](docs/architecture/system-overview.md) | Architecture, module boundaries, API conventions |
| [docs/architecture/technology-stack.md](docs/architecture/technology-stack.md) | Exact versions, rationale, constraints |
| [docs/development/local-setup.md](docs/development/local-setup.md) | Toolchain, XAMPP database setup, troubleshooting |
| [docs/development/environment-variables.md](docs/development/environment-variables.md) | Every environment variable |
| [docs/database/schema.md](docs/database/schema.md) | Database configuration and schema |
| [docs/progress.md](docs/progress.md) | Goal-by-goal status and verification evidence |

---

## Current state

**Goal 01 — System Architecture & Project Foundation** is `ACCEPTED`.

**Goal 02 — Authentication & Role Management** is `READY_FOR_REVIEW`.

Goal 01 delivered the monorepo structure, the backend skeleton with API
versioning, global error handling, OpenAPI documentation, logging and trace
correlation; both Flutter applications with independent architecture, theming,
routing and Bangla/English localization; the admin console with routing,
layout, API client and responsive design; and the database connection
foundation.

Goal 02 adds end-to-end authentication across all four applications: JWT
issuance/refresh with rotation and replay detection, six-role authorization,
SQL migrations and an opt-in first-admin bootstrap on the backend; sign-in,
registration, pending-approval and server-URL-override screens with secure
token storage on both Flutter apps; and a guarded React admin console with
single-flight silent refresh. Both Flutter apps build installable debug APKs
with distinct application IDs.

**No Goal 03 business feature is implemented.** Screens that would show live
catalogue/order data remain placeholders that state which goal delivers them.

Goal 02 was finalised on 2026-11-12: the auth tables are present in live
MySQL and verified against the migrations, `JWT_SECRET` is configured with
fail-safe startup, the first `SUPER_ADMIN` was bootstrapped (and the bootstrap
disabled again), and 64 live HTTP checks against real MySQL passed in addition
to the 57 automated tests. See [docs/progress.md](docs/progress.md) for the
full evidence.

Known limitations are recorded honestly in
[docs/progress.md](docs/progress.md) — notably that **no device/emulator was
available**, so no on-device install or in-app walkthrough was performed
(APKs were verified with `aapt`, size/hash recording and a secret scan), that
no browser-level admin-web E2E was automated, and that the admin web's
refresh token lives in `sessionStorage` (memory-only access token,
XSS-exposed — no HttpOnly cookies; the cookie migration was evaluated and
deferred as a production-hardening item).

---

## License

Proprietary. See [LICENSE](LICENSE).
