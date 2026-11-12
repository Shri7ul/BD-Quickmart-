# BD QuickMart — Development Progress

---

## Goal 01 — System Architecture & Project Foundation

| Field | Value |
|---|---|
| **Goal number** | 01 |
| **Goal name** | System Architecture & Project Foundation |
| **Status** | `ACCEPTED` |
| **Started** | 2026-11-08 |
| **Completed** | 2026-11-09 |
| **Accepted** | 2026-11-10 |
| **Next permitted goal** | Goal 02 — Authentication & Role Management |

> Status is `ACCEPTED`. The final audit on 2026-11-10 re-ran every verification
> command, fixed the defects listed below and confirmed all acceptance
> criteria. See [Final audit & acceptance record](#final-audit--acceptance-record).

---

### Completed tasks

#### Task A — Repository inspection

- Inspected the working tree, branches, history and `git status`.
- Found an **empty skeleton**: `.gitignore`, `LICENSE`, `README.md` (a single
  heading) and `plan.md` (the roadmap). No implementation to preserve.
- Git state on arrival: branch `main`, tracking `origin/main`, working tree
  clean, 2 commits (`0b5ce1e implementation plan ready`, `19300d4 Initial
  commit`).
- Existing conventions adopted rather than replaced: `plan.md` kept
  unmodified as the roadmap of record; the original `.gitignore` retained and
  extended with project-specific rules.
- **Toolchain was absent** and had to be installed — see Known issues.

#### Task B — Monorepo structure

Created the required layout. Each application is independently executable and
independently buildable.

```
bd-quickmart/
├── backend/
├── customer-app/
├── rider-app/
├── admin-web/
├── ai/
├── infrastructure/
├── docs/
└── README.md
```

#### Task C — Spring Boot backend foundation

| Requirement | Implementation |
|---|---|
| Main entry point | `backend/src/main/java/com/bdquickmart/BdQuickMartApplication.java` |
| Configuration structure | `config/` — `OpenApiConfig`, `WebConfig`, `SecurityConfig`, `WebSocketConfig`, `AppProperties` |
| Environment variable config | Every value bound via `${VAR:default}` placeholders in `application.yml`; no secrets in code |
| Global exception handling | `common/exception/GlobalExceptionHandler.java` — 8 handler methods covering validation, auth, access, malformed body and unexpected failures |
| Standard API response | `common/api/ApiResponse.java` + `common/api/ApiCode.java` — uniform envelope with stable `BDQM-xxxx` codes |
| Health-check endpoint | `GET /api/v1/health` — reports service and database status; bounded probe, never blocks on a dead database |
| WebSocket transport | `config/WebSocketConfig` — `/ws/tracking` registered and reachable (`101`); handler logic arrives in Goal 08 |
| Request validation | `spring-boot-starter-validation`; `MethodArgumentNotValidException` mapped to `BDQM-0400` with field-level detail |
| Logging configuration | Rolling file + console pattern with `%X{traceId}`; levels via `LOG_LEVEL_*` env vars |
| Swagger/OpenAPI | Springdoc 2.8.13 — `/v3/api-docs` and `/swagger-ui.html` |
| CORS for development | `WebConfig` — origins from `APP_CORS_ALLOWED_ORIGINS`, credentials enabled |
| Package organization | `common/` (api, exception, util) + `config/` + `modules/` |

**Module boundaries reserved** (15 packages, each with `package-info.java`):
`auth`, `user`, `customer`, `rider`, `store`, `product`, `inventory`, `cart`,
`order`, `payment`, `delivery`, `tracking`, `ai`, `notification`, `analytics`.

No business logic was implemented in these packages.

#### Task D — Customer mobile app

Independently scaffolded Flutter project (`customer_app`, org
`com.bdquickmart`, Android + iOS).

- **Entry point** — `lib/main.dart` → `lib/app/app.dart` (`CustomerApp`)
- **Feature-based structure** — `lib/features/{auth,home,products,cart,orders,profile}`
- **Routing** — `go_router` with named routes and nested child routes under
  `/home`; `AppRoutes` / `AppRouter` keep paths in one place
- **Theme** — `core/theme/app_theme.dart`, Material 3, green grocery palette,
  seeded `ColorScheme`, consistent radius/spacing scale
- **Centralized API client** — `core/network/api_client.dart` (Dio); one
  interceptor, one envelope unwrap, one error translation point
- **Environment configuration** — `core/config/app_config.dart` using
  `String.fromEnvironment`, plus `.env.example`
- **Reusable widgets** — `core/widgets/app_scaffold.dart` (`AppScaffold`,
  `EmptyState`, `ErrorState`), `core/widgets/loading_indicator.dart`
- **Localization foundation** — `core/localization/app_localizations.dart`;
  `flutter_localizations` + a custom delegate; `bn` and `en`, Bangla default

#### Task E — Rider mobile app

**Separately scaffolded Flutter project** (`rider_app`, own package name, own
`main.dart`, own `RiderApp` root).

Not a renamed copy of the customer app. The differences are substantive:

| Aspect | Customer app | Rider app |
|---|---|---|
| Domain model | none yet | `DeliveryAssignment` (offered/accepted/rejected/expired/completed), `RiderStatus`, `RiderLocation` |
| Location freshness | — | `RiderLocation.isFresh()` — a stale GPS fix is never presented as live |
| Routes | storefront (home/products/cart/orders/profile) | shift loop (dashboard/assignments/delivery/earnings/profile) |
| Palette | green (grocery) | deep indigo + orange, distinguishable on a shared device |
| Localization table | shopping vocabulary | rider vocabulary (offers, pickups, payouts) |
| Dependencies | foundation set | identical foundation set; location plugins are deferred to a later goal |

Includes `.env.example`, `core/{config,network,theme,router,localization,widgets}`,
`features/{dashboard,assignments,delivery,navigation,earnings,profile,availability}`,
and a reusable `AssignmentCard` widget.

#### Task F — Admin web application

React 19 + TypeScript + Vite 8.

- **Routing** — `src/app/router.tsx` using `createBrowserRouter`; six routes
  registered up front
- **Application layout** — `src/components/layout/AdminLayout.tsx`; fixed
  sidebar + topbar + content region
- **Authentication-ready structure** — `src/features/auth/`, `ApiClient.setAuthToken()`,
  a `bearerAuth` security scheme in the OpenAPI document, and an `AuthenticateUser`
  type with the three backend roles
- **API client** — `src/lib/api/client.ts` (Axios); request interceptor for the
  bearer token, response interceptor normalizing errors to `ApiError`,
  envelope unwrapping
- **Environment configuration** — `src/lib/config/env.ts` reading `VITE_*`
  variables, plus `.env.example`
- **Reusable components** — `StatCard`, `StatusBadge`, `DataTable` (generic)
- **Feature-based modules** — `src/features/{dashboard,products,orders,inventory,riders,analytics,auth}`
- **Responsive design foundation** — sidebar collapses to a horizontal
  scroller below 900px; CSS custom properties for the full palette

#### Task G — Database preparation

The agent did **not** create the database. XAMPP was not launched and
phpMyAdmin was not opened during the work; the bootstrap script was authored
and the **developer ran it manually** in phpMyAdmin, after which the connection
was verified end to end (see [Verification evidence](#1-live-mysql-connection)).

| Deliverable | Location |
|---|---|
| Proposed database name | `bd_quickmart` |
| Bootstrap script | `infrastructure/database/init/00_create_database.sql` |
| Verification script | `infrastructure/database/init/01_verify_connection.sql` |
| Schema documentation | `docs/database/schema.md` |
| Datasource configuration | `backend/src/main/resources/application.yml` — `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `DB_DRIVER`, `DB_DIALECT`, `DB_DDL_AUTO` |
| `.env.example` | Root, `customer-app/`, `rider-app/`, `admin-web/` |

The bootstrap script uses `CREATE DATABASE IF NOT EXISTS` with
`utf8mb4` / `utf8mb4_unicode_ci` — required for Bangla script — and does not
touch any other database. It creates **no tables**; schema design belongs to a
later goal.

#### Task H — Documentation

| Document | Created |
|---|---|
| `README.md` | ✅ |
| `docs/architecture/system-overview.md` | ✅ |
| `docs/architecture/technology-stack.md` | ✅ |
| `docs/development/local-setup.md` | ✅ |
| `docs/development/environment-variables.md` | ✅ |
| `docs/database/schema.md` | ✅ |
| `docs/progress.md` | ✅ (this file) |
| `ai/README.md` | ✅ |
| `infrastructure/README.md` | ✅ |

#### Task I — Quality assurance

All checks below were **actually executed**. Commands and results are in
[Verification evidence](#verification-evidence).

---

### Verification evidence

**Environment:** Windows 11 Pro 10.0.26200 · Temurin JDK 21.0.13 LTS ·
Maven 3.9.9 · Flutter 3.35.1 (Dart 3.9.0) · Node 22.22.2 (npm 10.9.7) · Android SDK
platforms 34 & 36.

#### 1. Live MySQL connection

The developer created the database manually by running
`infrastructure/database/init/00_create_database.sql` in phpMyAdmin. The
connection was then verified against the running server.

```
$ "C:/xampp/mysql/bin/mysql.exe" -u root -e "SHOW DATABASES LIKE 'bd_quickmart';"
Database (bd_quickmart)
bd_quickmart

$ "C:/xampp/mysql/bin/mysql.exe" -u root -e "SELECT SCHEMA_NAME, DEFAULT_CHARACTER_SET_NAME,
    DEFAULT_COLLATION_NAME FROM information_schema.SCHEMATA WHERE SCHEMA_NAME='bd_quickmart';
    SELECT COUNT(*) AS table_count FROM information_schema.TABLES WHERE TABLE_SCHEMA='bd_quickmart';"
SCHEMA_NAME  DEFAULT_CHARACTER_SET_NAME  DEFAULT_COLLATION_NAME
bd_quickmart utf8mb4                     utf8mb4_unicode_ci

table_count
0
```

The database exists, uses the required `utf8mb4` / `utf8mb4_unicode_ci`
collation for Bangla script, and contains **zero tables** — the correct
end-of-Goal-01 state, since schema design belongs to a later goal.

#### 2. Backend compilation and packaging

```
$ cd backend && mvn -B clean package
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 3.456 s
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] Building jar: ...\backend\target\bd-quickmart-backend-0.1.0.jar
[INFO] BUILD SUCCESS
```

The test is `HealthControllerTest`, with two cases: the health endpoint
returns `success: true`, `code: BDQM-0000` and `data.status: UP`; and the
endpoint responds promptly even without a database. It excludes the datasource
auto-configuration, so it does not require a running MySQL.

#### 3. Backend startup (packaged jar)

```
$ cd backend && java -jar target/bd-quickmart-backend-0.1.0.jar
2026-11-09 15:45:16.369 INFO  com.zaxxer.hikari.HikariDataSource - bd-quickmart-pool - Starting...
2026-11-09 15:45:16.378 INFO  com.zaxxer.hikari.HikariDataSource - bd-quickmart-pool - Start completed.
2026-11-09 15:45:17.646 INFO  o.s.b.w.e.tomcat.TomcatWebServer - Tomcat started on port 8080 (http) with context path '/'
2026-11-09 15:45:17.655 INFO  c.bdquickmart.BdQuickMartApplication - Started BdQuickMartApplication in 3.991 seconds
```

The connection pool started cleanly against the live MySQL instance, with no
errors or exceptions in the startup log.

#### 4. Health endpoint — database UP

```
$ curl http://localhost:8080/api/v1/health
{
  "success": true,
  "code": "BDQM-0000",
  "message": "Service is running",
  "data": {
    "status": "UP",
    "application": "bd-quickmart-backend",
    "timestamp": "2026-11-09T13:45:01.245098400Z",
    "database": "UP",
    "details": {"status": "UP", "latencyMs": 1}
  },
  "path": "/api/v1/health",
  "timestamp": "2026-11-09T13:45:01.246099200Z"
}
```

**`"database": "UP"`** — the outstanding Goal 01 acceptance item is now
closed. Response times across three consecutive calls: `0.009s`, `0.004s`,
`0.004s`, with database latency of `0–2ms`.

The endpoint was also observed correctly reporting `"database": "DOWN"` earlier
in the same session, when the MySQL server had stopped. Both states were
verified against the real server, which confirms the probe reports the live
condition rather than a cached or hardcoded value.

#### 5. Swagger and OpenAPI

```
$ curl -o /dev/null -w "swagger-ui/index.html: %{http_code}  %{time_total}s\n" http://localhost:8080/swagger-ui/index.html
swagger-ui/index.html: 200  0.013220s
$ curl -o /dev/null -w "v3/api-docs: %{http_code}  %{time_total}s\n" http://localhost:8080/v3/api-docs
v3/api-docs: 200  0.243146s
$ curl -o /dev/null -w "actuator/health: %{http_code}  %{time_total}s\n" http://localhost:8080/actuator/health
actuator/health: 200  0.041996s
```

`/swagger-ui.html` redirects `302` to `/swagger-ui/index.html`, which serves
`200`.

The OpenAPI document was fetched and confirmed to contain
`"openapi":"3.1.0"`, title `BD QuickMart API`, version `v1`, the `bearerAuth`
security scheme, and the `/api/v1/health` path.

The actuator composite health now returns `200` with
`"db": {"status": "UP", "details": {"database": "MySQL"}}`.

#### 6. CORS preflight (admin web origin)

```
$ curl -X OPTIONS http://localhost:8080/api/v1/health -H "Origin: http://localhost:5173" -H "Access-Control-Request-Method: GET"
HTTP/1.1 200
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET,POST,PUT,PATCH,DELETE,OPTIONS
Access-Control-Expose-Headers: X-Trace-Id
Access-Control-Allow-Credentials: true
Access-Control-Max-Age: 3600
```

The admin web dev origin is accepted, and `X-Trace-Id` is exposed to the
browser so the client can log the correlation id.

#### 7. WebSocket handshake

```
$ curl -D - -H "Connection: Upgrade" -H "Upgrade: websocket" -H "Sec-WebSocket-Version: 13" \
       -H "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==" -H "Origin: http://localhost:5173" \
       http://localhost:8080/ws/tracking
HTTP/1.1 101
Upgrade: websocket
Connection: upgrade
Sec-WebSocket-Accept: s3pPLMBiTxaQ9kYGzzhZRbK+xOo=
```

`101 Switching Protocols` with a valid `Sec-WebSocket-Accept` confirms the
tracking endpoint is reachable. This required a fix — see
[Known issue 8](#8--the-websocket-endpoint-was-blocked-by-spring-security-fixed).

#### 8. Customer app analysis

```
$ cd customer-app && flutter analyze
Analyzing customer-app...
No issues found! (ran in 17.4s)
```

#### 9. Customer app tests

```
$ cd customer-app && flutter test
00:00 +1: Localizations resolve Bangla and English strings
00:00 +2: All tests passed!
```

#### 10. Customer app Android build

```
$ cd customer-app && flutter build apk --debug
Running Gradle task 'assembleDebug'...                            211.1s
√ Built build\app\outputs\flutter-apk\app-debug.apk
```

(A Kotlin incremental-compilation cache warning appears in the Gradle output.
It is non-fatal — the APK builds successfully.)

#### 11. Rider app analysis

```
$ cd rider-app && flutter analyze
Analyzing rider-app...
No issues found! (ran in 6.0s)
```

#### 12. Rider app tests

```
$ cd rider-app && flutter test
00:00 +3: A stale location fix is not treated as live
00:00 +4: Assignment starts in the offered state
00:00 +4: All tests passed!
```

Four tests: dashboard renders, rider-specific localization resolves, a stale
location fix is not treated as live, and an assignment starts in the `offered`
state.

#### 13. Rider app Android build

```
$ cd rider-app && flutter build apk --debug
Running Gradle task 'assembleDebug'...                             62.6s
√ Built build\app\outputs\flutter-apk\app-debug.apk
```

#### 14. Admin web lint and production build

```
$ cd admin-web && npm run lint
Found 0 warnings and 0 errors.
Finished in 50ms on 15 files with 116 rules using 24 threads.

$ cd admin-web && npm run build
✓ 30 modules transformed.
dist/index.html                   0.57 kB │ gzip:  0.34 kB
dist/assets/index-Dnvb6chx.css    4.83 kB │ gzip:  1.47 kB
dist/assets/index-RnD5FiYT.js   315.34 kB │ gzip: 99.68 kB
✓ built in 169ms
```

TypeScript compiles under `verbatimModuleSyntax` and `erasableSyntaxOnly`
(strict project references) with no errors.

#### 15. Configuration consistency

| Check | Result |
|---|---|
| No secret committed | ✅ `git check-ignore` confirms `.env` ignored, `.env.example` tracked, in all four locations |
| Secret scan | ✅ regex sweep for `sk-*`, `AIza*`, `gsk_*`, `xox?-*`, `AKIA*`, PEM private keys and literal `password`/`secret`/`api_key` assignments — **no matches** |
| `.env.example` values are placeholders only | ✅ `DB_PASSWORD=`, `GEMINI_API_KEY=`, `GROQ_API_KEY=` are empty in the root template; the three client templates contain only URLs and a boolean |
| Backend default port matches client URLs | ✅ `8080` in `application.yml`, `.env.example` and both Flutter apps |
| API path prefix consistent | ✅ `/api/v1` in backend `application.yml`, all three `.env.example` files and the OpenAPI document |
| WebSocket path consistent | ✅ `/ws/tracking` in `WebSocketConfig`, all three `.env.example` files, and the endpoint answers `101` |
| Client env keys match their templates | ✅ customer/rider use `API_BASE_URL`, `WS_BASE_URL`, `IS_DEVELOPMENT`; admin uses `VITE_API_BASE_URL`, `VITE_WS_BASE_URL`, `VITE_REQUEST_TIMEOUT`, `VITE_IS_DEVELOPMENT` |
| CORS origin matches admin dev server | ✅ `http://localhost:5173` in `APP_CORS_ALLOWED_ORIGINS`, confirmed by a live preflight |
| Database name consistent | ✅ `bd_quickmart` in `application.yml`, both SQL scripts, `docs/database/schema.md` and the live server |
| Charset consistent | ✅ `utf8mb4` / `utf8mb4_unicode_ci` in the bootstrap script, the schema document and the live server |
| No `.env` file staged for commit | ✅ verified with `git status` and `git ls-files` |
| No build artifacts staged | ✅ `git ls-files --others --exclude-standard` returns no `node_modules/`, `.dart_tool/`, `target/`, `dist/`, `build/` or `.gradle/` entries |

#### 16. Tools not exercised

| Check | Why not |
|---|---|
| `flutter run` on a device | No emulator or physical device is attached to this machine. The apps are verified via `flutter analyze`, `flutter test` and a successful Android APK build. |
| iOS build | Requires macOS with Xcode. This is a Windows machine. |
| Browser rendering of the admin web | No browser is attached. Verified via `npm run build` and `npm run lint`. |
| Schema migrations | No tables exist yet by design. Schema ownership belongs to a later goal. |

---

### Known issues

**1 — The MySQL database did not exist at first. Closed.**
The database is created by the **developer**, not the agent: Task G forbids
the agent from launching XAMPP or opening phpMyAdmin. The developer ran
`infrastructure/database/init/00_create_database.sql` manually, and the
connection has since been **verified end to end** — `SHOW DATABASES` confirms
`bd_quickmart` exists with `utf8mb4` / `utf8mb4_unicode_ci`, and
`GET /api/v1/health` reports `"database": "UP"` with a measured latency of
0–2ms. See [Verification evidence §1](#1-live-mysql-connection) and
[§4](#4-health-endpoint--database-up).

This issue is **resolved**. It remains listed because the manual nature of
the step is worth recording for anyone setting the project up fresh.

**2 — The toolchain had to be installed during this goal.**
No Java, Maven or Flutter was present on the machine. The following were
installed to `C:\Users\USER\tools\`:

| Tool | Version | Method |
|---|---|---|
| Temurin JDK | 21.0.13 LTS | Adoptium API archive |
| Apache Maven | 3.9.9 | Apache archive mirror |
| Flutter | 3.35.1 | Google storage archive |

These are **outside the repository** and are not committed. The `.tooling/env.sh`
helper was added during the work and is git-ignored. A fresh machine needs the
same three tools, or equivalents already on `PATH`.

**3 — `@EnableJpaAuditing` was removed from the application class.**
It was added initially, but the auditing handler requires a JPA mapping
context, which does not exist before the first entity is introduced. It must be
re-enabled when the schema goal introduces entities, so that `created_at` and
`updated_at` are populated automatically. This is noted in
`docs/database/schema.md`.

**4 — The Hibernate dialect is pinned explicitly.**
Without `DB_DIALECT`, Hibernate reads dialect metadata over a live JDBC
connection and refuses to start when MySQL is stopped. Pinning
`org.hibernate.dialect.MySQLDialect` lets the backend boot and report database
state honestly instead of crashing with an opaque startup error.

**5 — No automated CI pipeline exists.**
Continuous integration is deferred to Goal 18. Every verification command in
this document is currently run manually.

**6 — The health endpoint blocked on a dead database. Fixed.**
The first implementation of `GET /api/v1/health` delegated the database check
to Spring Boot's actuator `db` health indicator. That indicator borrows a
connection from the Hikari pool, so with MySQL stopped the endpoint blocked
for the full **30-second** connection timeout on every call.

This was found during final verification: repeated health checks hung, and a
follow-up backend start failed with `Port 8080 was already in use` because the
earlier process had not released the socket.

The fix replaces the actuator indicator with a **bounded probe**:

- The probe runs on its own daemon thread with a 2-second budget, so the HTTP
  request is never blocked by the JDBC connect timeout.
- The JDBC URL now carries `connectTimeout=2000&socketTimeout=10000`, so the
  MySQL driver fails fast rather than waiting on a dead socket.
- `DB_POOL_CONNECTION_TIMEOUT` (default `5000`) bounds the pool wait, which
  also brings `/actuator/health` down from 30s to ~5s.
- A missing `DataSource` bean is handled via `ObjectProvider`, so the
  controller still constructs in tests that exclude datasource
  auto-configuration.

Measured before and after, with MySQL stopped:

| Endpoint | Before | After |
|---|---|---|
| `/api/v1/health` | 30s+ (hung) | **2.67s**, stable across 3 consecutive calls |
| `/actuator/health` | 30.02s | **5.06s** |
| `/swagger-ui/index.html` | 8ms | 8ms (unaffected) |
| `/v3/api-docs` | 6ms | 6ms (unaffected) |

A second test, `healthEndpointRespondsQuicklyWithoutDatabase`, now asserts
the endpoint responds in under 10 seconds so this cannot regress silently.

**Lesson applied:** a health check that blocks is worse than one that reports
the failure. The same principle will be applied to any future endpoint that
touches the database.

**7 — A leftover backend JVM held port 8080 after the verification sweep.**
Stopping the `mvn spring-boot:run` background task terminated the Maven
wrapper but left its forked JVM running, so the port stayed bound. A later
start failed with `Port 8080 was already in use`.

This was a test-harness artifact rather than an application defect, but it
would equally affect a developer who stops the app from the IDE. The remedy is
in [`docs/development/local-setup.md`](development/local-setup.md) under
Troubleshooting: identify the holder with
`Get-NetTCPConnection -LocalPort 8080 -State Listen` and stop it with
`Stop-Process -Id <pid> -Force`.

**8 — The WebSocket endpoint was blocked by Spring Security. Fixed.**
`WebSocketConfig` registered the tracking handler at `/ws/tracking`, but the
security filter chain ended with `anyRequest().authenticated()`. A WebSocket
handshake is a plain HTTP upgrade request that carries no bearer token, so
Spring Security rejected it with `403` **before** Spring's WebSocket
dispatcher ever saw it. The endpoint documented in the README and in all
three client `.env.example` files was a dead URL.

This was found during final verification, when the handshake was tested
alongside the other endpoints and returned `403` instead of `101`.

The fix adds an explicit permit rule for `/ws/**` in `SecurityConfig`,
alongside the existing rules for health, actuator and Swagger:

```java
// The WebSocket handshake is a plain HTTP upgrade request and
// never carries a bearer token, so it must bypass the
// authenticated filter. Per-connection authorization is
// applied by the tracking handler from Goal 08 onwards.
.requestMatchers("/ws/**")
.permitAll()
```

Verified after the fix — the handshake now returns `101 Switching Protocols`
with a valid `Sec-WebSocket-Accept`:

```
HTTP/1.1 101
Upgrade: websocket
Connection: upgrade
Sec-WebSocket-Accept: s3pPLMBiTxaQ9kYGzzhZRbK+xOo=
```

The backend test suite was re-run after the change:
`Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`.

**Lesson applied:** registering a transport is not the same as exposing it.
Any endpoint referenced by a client must be reachable from that client.
Goal 08 will add per-connection authentication to the tracking handler.

---

### Final audit & acceptance record

The independent final acceptance audit on **2026-11-10** re-reviewed every
changed and untracked file, re-ran every verification command after all fixes,
and confirmed the full acceptance checklist. Status is therefore `ACCEPTED`.

**Verified environment (final audit):** Windows 11 Pro · Temurin JDK 21.0.13
LTS · Maven 3.9.9 · Flutter 3.35.1 (Dart 3.9.0) · Node 22.22.2 (npm 10.9.7) ·
XAMPP MySQL (read-only verification).

#### Verification commands and results

| # | Command | Result |
|---|---|---|
| 1 | `cd backend && mvn -B clean verify` | `BUILD SUCCESS` — `Tests run: 2, Failures: 0, Errors: 0, Skipped: 0` |
| 2 | `cd customer-app && flutter analyze` | `No issues found!` |
| 3 | `cd customer-app && flutter test` | `+2: All tests passed!` |
| 4 | `cd rider-app && flutter analyze` | `No issues found!` |
| 5 | `cd rider-app && flutter test` | `+4: All tests passed!` |
| 6 | `cd admin-web && npm run lint` | `Found 0 warnings and 0 errors.` (15 files, 116 rules) |
| 7 | `cd admin-web && npm run build` | `✓ 30 modules transformed` — `✓ built` |

**Total automated tests: 8** (backend 2 + customer 2 + rider 4). All passed.

As a post-fix check, both Flutter apps also passed `flutter build bundle --debug`
once their declared `assets/` roots were anchored (see defect 2 below). No
source file changed after the final verification sweep — only this
documentation was edited afterwards.

#### Endpoint verification (packaged jar, port 8080)

| Endpoint | Result |
|---|---|
| `GET /api/v1/health` | `200` — `success: true`, `code: BDQM-0000`, `database: UP`, latency ~1ms |
| `GET /swagger-ui/index.html` | `200` |
| `GET /v3/api-docs` | `200` — `"openapi":"3.1.0"`, `/api/v1/health`, `bearerAuth` |
| `GET /actuator/health` | `200` — `db` status `UP` |
| `WS /ws/tracking` | `HTTP/1.1 101` — valid `Sec-WebSocket-Accept` |
| `OPTIONS` CORS preflight | `200` — allowed origin `http://localhost:5173`, `X-Trace-Id` exposed |

MySQL was verified **read-only** (`information_schema.SCHEMATA` / `TABLES`):
`bd_quickmart` exists with `utf8mb4` / `utf8mb4_unicode_ci` and
`table_count = 0`. No schema was created, modified, dropped or recreated.

> Environment caveat (not an application defect): the agent runtime injects
> `SERVER__PORT=63614`, which Spring Boot relaxed binding interprets as the
> server port, causing an "address already in use" failure. The audit ran the
> jar with `env -u SERVER__PORT SERVER_PORT=8080 ...`. On a normal developer
> machine this variable is absent and no workaround is needed.

#### Defects found and fixed during the final audit

All resolved before the acceptance commit.

1. **Duplicate API result code.** `ApiCode.java` defined both `BAD_REQUEST` and
   `UNAUTHORIZED` as `BDQM-0401`. Removed the redundant `BAD_REQUEST` and
   mapped malformed request bodies in `GlobalExceptionHandler` to
   `VALIDATION_ERROR` (`BDQM-0400`), keeping `UNAUTHORIZED` as `BDQM-0401`.
2. **Flutter asset root missing on a fresh clone.** Both `pubspec.yaml` files
   declare `assets/`, but an empty directory is not tracked by Git; a fresh
   clone failed `flutter build bundle` with "unable to find directory entry in
   pubspec.yaml". Added `customer-app/assets/.gitkeep` and
   `rider-app/assets/.gitkeep`; both builds now succeed.
3. **Documentation accuracy.** `technology-stack.md` claimed the rider app
   already carried `geolocator` / `connectivity_plus`; corrected to state they
   are deferred to a later goal. Node version aligned to the verified 22.22.2
   across the doc set; the rider dependency row in this file was corrected.
4. **Stray artifacts.** An unexpected `hj/` directory of decompiled Hibernate
   `.class` files was added to `.gitignore` and excluded from the commit.

#### Security audit

- No API keys, passwords or private credentials in any tracked file.
- Only placeholder-only `.env.example` templates are tracked; no real `.env`.
- Secret regex sweep over staged content (`sk-*`, `AIza*`, `gsk_*`, `AKIA*`,
  PEM private keys, literal `password`/`secret`/`api_key` assignments):
  **no matches**.
- No build artifacts or caches staged (`target/`, `node_modules/`, `dist/`,
  `.dart_tool/`, `build/`, `hj/`).
- `git diff --cached --check`: clean (exit 0).

#### Commit record

| Field | Value |
|---|---|
| Commit hash | `19f64cc1e5041d042da6912269a2429abcaa1329` (`19f64cc`) |
| Commit message | `feat: complete BD QuickMart goal 01 foundation` |
| Staged diff | 236 files changed, 11133 insertions(+), 2 deletions(-) |
| Branch | `main` |
| Remote | `https://github.com/Shri7ul/BD-Quickmart-.git` |

The first commit intentionally excluded `docs/progress.md` so that the final
commit hash and message could be recorded here; this document is committed as
a separate, non-amending follow-up commit. No amend, force push or history
rewrite was performed.

#### Remaining known issues (none blocking)

- No automated CI pipeline — deferred to Goal 18.
- `@EnableJpaAuditing` re-enable deferred until the schema goal adds entities.
- No device/emulator or macOS/Xcode here, so `flutter run`, iOS builds and
  live browser rendering were verified indirectly via analyze/test/build/lint.
- No schema tables exist yet by design (later goal); WebSocket handler logic
  is a no-op placeholder until Goal 08; the 15 module packages hold no
  business logic yet.
- Toolchain (JDK/Maven/Flutter) is installed outside the repo; the local
  `.tooling/env.sh` helper is git-ignored and not committed.
- Non-fatal warnings observed: LF→CRLF conversion on `git add`, Mockito/Byte
  Buddy dynamic-agent notices, Hibernate dialect `HHH000511`, and a generated
  Spring Security development password in test logs. None affect the build.

---

### Next permitted goal

**Goal 02 — Authentication & Role Management**

Entry condition: this goal is accepted by the developer. The MySQL
connection is now verified (`"database": "UP"` in the health response), so
that condition is satisfied.

Expected scope, per the roadmap:

- Registration and login for all three role families
- JWT access and refresh tokens
- OTP verification architecture
- Password hashing (BCrypt encoder is already registered in `SecurityConfig`)
- Role-based authorization for `CUSTOMER`, `RIDER`, `SUPER_ADMIN`,
  `STORE_MANAGER`, `INVENTORY_OPERATOR`
- Protected routes and the `bearerAuth` scheme already declared in the OpenAPI
  document
- Rider and admin approval workflow

The `auth` and `user` module packages are already reserved and empty, ready to
receive this work.

**Goal 03 must not begin until Goal 02 is accepted.**

---

### Change log

| Date | Change |
|---|---|
| 2026-11-08 | Repository inspected; toolchain installed; monorepo structure created |
| 2026-11-08 | Spring Boot backend foundation built, tested, packaged and started |
| 2026-11-08 | Customer Flutter app scaffolded and architected |
| 2026-11-09 | Rider Flutter app scaffolded and architected independently |
| 2026-11-09 | React + TypeScript admin web scaffolded and architected |
| 2026-11-09 | Database scripts, schema documentation and `.env.example` files prepared |
| 2026-11-09 | Documentation set written; full verification sweep executed |
| 2026-11-09 | Health endpoint made non-blocking against a down database; JDBC/pool timeouts bounded; second test added |
| 2026-11-09 | Leftover backend JVM on port 8080 identified and stopped; troubleshooting entry added |
| 2026-11-09 | Goal 01 set to `READY_FOR_REVIEW` |
| 2026-11-09 | Developer created the database manually in phpMyAdmin; live MySQL connection verified — `data.database: UP` |
| 2026-11-09 | WebSocket endpoint unblocked from Spring Security; handshake now returns `101` |
| 2026-11-10 | Final acceptance audit: re-reviewed every file; fixed duplicate `BDQM-0401` code, anchored Flutter `assets/` roots with `.gitkeep`, corrected rider dependency / Node version docs, ignored stray `hj/` artifacts |
| 2026-11-10 | Re-ran full verification (`mvn -B clean verify`, both `flutter analyze`/`flutter test`, `npm run lint`/`npm run build`) plus all endpoint checks — all passed |
| 2026-11-10 | Committed Goal 01 foundation as `19f64cc` (`feat: complete BD QuickMart goal 01 foundation`); status set to `ACCEPTED` |

---

## Goal 02 — Authentication & Role Management

| Field | Value |
|---|---|
| **Goal number** | 02 |
| **Goal name** | Authentication & Role Management |
| **Status** | `READY_FOR_REVIEW` |
| **Started** | 2026-11-10 |
| **Implementation completed** | 2026-11-11 |
| **Finalization verified** | 2026-11-12 |
| **Next permitted goal** | Goal 03 — must not begin until Goal 02 is accepted |

> Status remains `READY_FOR_REVIEW`, **not** `ACCEPTED`. The 2026-11-12
> finalization closed three of the four earlier blockers: the auth tables are
> present in the live MySQL database and were verified against the migrations,
> `JWT_SECRET` is configured (fail-safe startup confirmed), and the first
> `SUPER_ADMIN` was bootstrapped and disabled again. Live HTTP requests against
> real MySQL now back the authentication, six-role authorization and rider
> approval results. Acceptance is still gated on manual **on-device /
> emulator** testing (no `adb` device was attached) and browser-level admin-web
> E2E, which the developer must perform. The Goal 02 brief explicitly forbids
> marking a goal `ACCEPTED` merely because code compiles, and requires keeping
> `READY_FOR_REVIEW` when manual testing is still outstanding.

---

### Scope delivered

| Application | Delivered |
|---|---|
| **Backend** (Java 21 / Spring Boot 3.5.6) | `users` / `refresh_tokens` entities + repositories; JJWT HS256 access & refresh tokens (0.12.6); refresh-token rotation with SHA-256 hash storage, token families, `PESSIMISTIC_WRITE` locking and replay detection that revokes the whole family; stateless Spring Security (CSRF off, `STATELESS`, BCrypt); JSON-writing `AuthenticationEntryPoint` / `AccessDeniedHandler` for filter-level 401/403; a single `CorsConfigurationSource`; six canonical roles with least-privilege literal authorities (`ROLE_*`, `PERM_RIDER_ACTIVE`, `PERM_APPROVE_RIDERS`, `PERM_PROVISION_PRIVILEGED`); public registration limited to `CUSTOMER`/`RIDER`; new riders start `PENDING`; rider approve/reject workflow; privileged provisioning; opt-in first-`SUPER_ADMIN` bootstrap; `02`/`03`/`04` SQL scripts; `application-test.yml` + H2 integration suite + real-HTTP smoke test. |
| **Admin web** (React 19 / TS / Vite) | Login page, `AuthProvider`, `RequireAuth`, `RequireRole`, `UnauthorizedPage`, guarded `createBrowserRouter`; memory-only access token, refresh token in `sessionStorage`; single-flight 401 refresh queue with one retry; normalized auth errors; six roles + `AccountStatus` types. |
| **Customer app** (Flutter) | Sign-in / register / pending / splash screens; `provider` + `ChangeNotifier` `AuthController`; `flutter_secure_storage` token store + `shared_preferences` settings; `Dio` `QueuedInterceptorsWrapper` single-flight refresh; runtime server-URL override; `go_router` `refreshListenable` + top-level role/status redirect; profile sign-out; debug APK with `INTERNET` + cleartext. |
| **Rider app** (Flutter) | Same generic auth layer parameterized with `allowedRole: 'RIDER'`, `pendingAllowed: true`; pending-approval gate; rider-specific UI/router; distinct `com.bdquickmart.rider_app` debug APK. |

### Verification evidence

**Environment:** Windows 11 Pro · Temurin JDK 21.0.13 LTS · Maven 3.9.9 ·
Flutter 3.35.1 (Dart 3.9.0) · Node 22.22.2 · Android SDK build-tools 36.1.0.

| # | Command | Result |
|---|---|---|
| 1 | `cd backend && mvn -B clean verify` | `BUILD SUCCESS` — `Tests run: 27, Failures: 0, Errors: 0, Skipped: 0` |
| 2 | `cd admin-web && npm run lint` | `Found 0 warnings and 0 errors.` |
| 3 | `cd admin-web && npm run build` | `✓ 98 modules transformed` — `✓ built in 1.13s` |
| 4 | `cd customer-app && flutter analyze` | `No issues found!` |
| 5 | `cd customer-app && flutter test` | `+14: All tests passed!` |
| 6 | `cd customer-app && flutter build apk --debug` | `app-debug.apk` built (159M) |
| 7 | `cd rider-app && flutter analyze` | `No issues found!` |
| 8 | `cd rider-app && flutter test` | `+16: All tests passed!` |
| 9 | `cd rider-app && flutter build apk --debug` | `app-debug.apk` built (161M) |
| 10 | `aapt dump badging` on both APKs | distinct application IDs, minSdk 24, targetSdk 36, `INTERNET`, debug cleartext |
| 11 | `git diff --check` / `git diff --cached --check` | clean |

**Total automated tests: 57** (backend 27 + customer 14 + rider 16). All pass.
Admin web has no test runner configured; it is verified via lint + production
build.

#### Finalization re-verification (2026-11-12)

All commands below were re-run after the Goal 02 commits, from a clean `main`.

| # | Check | Result |
|---|---|---|
| 12 | `cd backend && mvn -B clean verify` | `BUILD SUCCESS` — `Tests run: 27, Failures: 0, Errors: 0, Skipped: 0`; fresh jar built |
| 13 | Start jar with **no** `JWT_SECRET`, `JWT_ALLOW_EPHEMERAL_DEV_KEY=false` | Refused to start (`JAVA_EXIT=1`, `IllegalStateException: JWT_SECRET is not configured`) — **fail-safe verified**, no ephemeral-key fallback |
| 14 | Start jar with generated `.env` against **live MySQL** (port 8080) | Started; `GET /api/v1/health` reported `database: UP`; Hikari/MySQLDialect connected |
| 15 | `03_verify_auth_tables.sql` (read-only) | Both tables `InnoDB` / `utf8mb4_unicode_ci`; all columns, indexes, unique keys and the `fk_refresh_user` foreign key match `02_create_auth_tables.sql` |
| 16 | Live HTTP auth suite (real MySQL) | **24/24 PASS** — registration (CUSTOMER/RIDER/PENDING), duplicate 409, privileged self-registration rejected, validation, login by email/phone, invalid password/unknown identifier, SUPER_ADMIN login, `/auth/me`, malformed/refresh-token-as-bearer rejection, access TTL 900s / refresh TTL 604800s, claims carry `role`+`purpose`, refresh rotation, replay revokes the family, logout, revoked-token rejection |
| 17 | Live six-role + rider approval suite (real MySQL) | **31/31 PASS** — single-SUPER_ADMIN bootstrap check, F1–F16 least-privilege matrix (CUSTOMER/RIDER denied admin routes; STORE_MANAGER/INVENTORY_OPERATOR denied rider approval), G1–G11 rider `PENDING` → approve via protected API → DB `ACTIVE` → re-auth reaches dashboard, H1–H3 reject leaves account blocked, all six roles present in MySQL |
| 18 | Live customer session + CORS suite (real MySQL) | **9/9 PASS** — register/login/`/auth/me`, session restored from stored refresh token, `/auth/me` with restored token, refresh rotation invalidates the old token, logout, refresh-token reuse rejected after logout, CORS preflight for `http://localhost:5173` |
| 19 | `cd admin-web && npm install && npm run lint && npm run build` | 60 packages / `0 vulnerabilities`; `0 warnings and 0 errors`; `98 modules transformed`, exit 0 |
| 20 | `cd customer-app && flutter pub get && flutter analyze && flutter test && flutter build apk --debug` | `No issues found!`; `+14: All tests passed!`; APK built, exit 0 |
| 21 | `cd rider-app && flutter pub get && flutter analyze && flutter test && flutter build apk --debug` | `No issues found!`; `+16: All tests passed!`; APK built, exit 0 |
| 22 | `aapt dump badging` / `aapt dump xmltree` on both rebuilt APKs | distinct application IDs, `versionName 0.1.0`, minSdk 24 / targetSdk 36, `INTERNET`, `usesCleartextTraffic` present in debug manifest only |
| 23 | APK production-secret scan | No `JWT_SECRET`, DB URL, admin bootstrap email/password or admin API URL found in either APK |
| 24 | `adb devices` | No device/emulator attached → **no install smoke test performed** |
| 25 | Tracked-file secret/artifact scan | 321 tracked files; no real secrets (all regex hits were UI labels / placeholders), no real `.env`, no APK/AAB/build/cache artifacts; `git diff --check` clean |

**Total live HTTP checks: 64** (24 + 31 + 9), all passing against real MySQL —
in addition to the 57 automated tests. No result in this table comes from H2.

#### Installable debug APKs

Final copies were placed in `installers/` (git-ignored, **not** committed):

| App | Build output | Delivery copy | Size (bytes) | SHA-256 | Application ID |
|---|---|---|---|---|---|
| Customer | `customer-app/build/app/outputs/flutter-apk/app-debug.apk` | `installers/BD-QuickMart-Customer.apk` | 166,090,410 | `0ad369221d418b4a527fdb42b7aed8eaae1f90ea82c583ff9f89efa61a9ba225` | `com.bdquickmart.customer_app` |
| Rider | `rider-app/build/app/outputs/flutter-apk/app-debug.apk` | `installers/BD-QuickMart-Rider.apk` | 167,905,227 | `25b91d222b60c43a43e95624172c4b3731caf40204cf6e28c55d015524cda5ce` | `com.bdquickmart.rider_app` |

Both are `versionName 0.1.0`, minSdk 24 / targetSdk 36, declare
`android.permission.INTERNET`, and enable cleartext only in debug/profile
manifests (the main manifest has no cleartext flag). Both APKs and the
`installers/` folder are git-ignored and were **not** committed. No production
secrets are embedded in either APK.

### Commit record

| Field | Value |
|---|---|
| Implementation commit | `3ca41caafb01ae5a71968a522a94e52a09281c92` (`3ca41ca`) |
| Commit message | `feat: complete BD QuickMart goal 02 authentication & role management` |
| Staged diff | 126 files changed, 7929 insertions(+), 522 deletions(-) |
| Branch | `main` |
| Remote | `https://github.com/Shri7ul/BD-Quickmart-.git` — **not pushed** |
| Documentation commit | This file — a separate, non-amending follow-up commit |

The implementation commit intentionally excluded `docs/progress.md` so that
`3ca41ca` could be recorded here; this document is committed afterwards as a
second commit. No amend, force push or history rewrite was performed.

> This table and the "Git policy compliance" list below record the state **as of
> 2026-11-11**, when the Goal 02 brief required no push. The 2026-11-12
> finalization task explicitly authorized a normal fast-forward push; see
> "Goal 02 finalization" below for the current push status.

### Git policy compliance

- **No push.** Both Goal 02 commits remain local; `main` is ahead of
  `origin/main`.
- **No `git add .`** — 126 files were staged with explicit pathspecs.
- No amend / history rewrite.
- Reviewed commit created only after all verification gates,
  `git diff --cached --check`, and a staged secret/artifact scan passed.
- No real `.env`, APK/AAB, build output, cache or lock file staged
  (`git diff --cached --name-only` audited — 126 files, all source/docs/SQL).
- No Goal 03 work was started.

### Known issues and limitations

1. **Live MySQL schema — resolved (2026-11-12).** `users` and
   `refresh_tokens` exist in the developer's `bd_quickmart` database and were
   verified against `02_create_auth_tables.sql` with the read-only
   `03_verify_auth_tables.sql` (columns, indexes, unique keys, foreign key,
   engine and collation all match). All 64 live HTTP checks ran against this
   real MySQL instance, not H2. The SQL scripts remain in the repository for
   re-creation on a fresh machine; do not re-import them onto this database
   (they are `CREATE TABLE IF NOT EXISTS`, so a repeat is harmless but
   unnecessary).
2. **No on-device install smoke test — still open.** `adb devices` returned no
   attached device/emulator, so `adb install` was not run and the apps were
   not opened on a phone. APKs were verified with `aapt dump badging`,
   `aapt dump xmltree`, size/hash recording and a secret scan. The in-app
   "Pending Approval" display, close/reopen session restoration and the
   physical-device LAN URL were therefore validated at the API/session layer
   and by source review, not by manual device operation.
3. **No browser-level admin-web E2E.** The admin route guards, login, silent
   refresh, session-expiry/failure handling and logout were verified by code
   review, lint, production build and the backend authorization matrix, but
   no automated browser session was driven. Manual browser walkthrough remains
   a developer acceptance step.
4. **Admin web refresh token is XSS-exposed — known production-hardening
   issue.** It lives in `sessionStorage` (not an HttpOnly cookie); the access
   token is memory-only. The brief's proposed `HttpOnly` / `Secure` /
   `SameSite` cookie migration was **evaluated and deliberately deferred**:
   doing it properly requires changing the shared backend auth contract
   (`login` / `refresh` / `logout`), enabling CSRF protection (currently
   disabled for the stateless bearer API), `withCredentials` handling and a
   coordinated rewrite of both Flutter apps plus the full test suite. A
   half-working cookie implementation would break the verified end-to-end
   auth, so the working implementation was kept and the trade-off documented
   in `admin-web/src/features/auth/session.ts` and
   `docs/development/local-setup.md`.
5. **OTP verification not implemented.** It appears in the roadmap but is out
   of the implemented Goal 02 scope.
6. **First-admin bootstrap is opt-in** via `APP_BOOTSTRAP_ADMIN_ENABLED`. It
   was exercised once on 2026-11-12 and is now `false` again in `.env`; the
   resulting single `SUPER_ADMIN` was verified to log in and reach privileged
   endpoints. No bootstrap credential is committed.
7. **`pubspec.lock` is ignored** by the existing broad `*.lock` rule — an
   accepted pre-existing repository policy; verified no lock file is tracked.
8. **No automated CI pipeline** — deferred to Goal 18; all commands run
   manually.
9. **Non-fatal warnings observed:** LF→CRLF normalization on `git add`, a
   generated Spring Security development password in test logs, and a Kotlin
   incremental-compilation cache notice during Gradle. None affect the build.
10. **Test accounts created in local MySQL.** The live verification registered
    throwaway CUSTOMER/RIDER/STORE_MANAGER/INVENTORY_OPERATOR rows and refresh
    tokens in the developer's own database (10 users, 16 refresh tokens at the
    time of the check). These are local test data, not committed credentials;
    the developer may delete them through phpMyAdmin at their discretion. The
    agent did not drop, recreate or alter any schema object.

### Goal 02 finalization (2026-11-12)

A separate developer brief — *"Goal 02 Finalization, Real-Device Verification &
GitHub Push"* — authorized finishing developer-run verification, fixing genuine
defects, preparing installable apps, a security review, documentation updates,
and a **normal fast-forward push** (no force-push, no history rewrite).

**Verified state at the start:** clean `main`, `HEAD = f5e8061`, two commits
ahead of `origin/main` (`2b828a6`), remote
`https://github.com/Shri7ul/BD-Quickmart-.git`. No user changes were present.

**What was done**

| Area | Result |
|---|---|
| MySQL (read-only) | `03_verify_auth_tables.sql` run; `users` + `refresh_tokens` present and matching the migration (columns, indexes, unique keys, FK, InnoDB/utf8mb4_unicode_ci). No schema object was dropped, created or altered. |
| JWT | A >=32-byte random secret was generated into the git-ignored `.env` (never committed, never printed). `JWT_ALLOW_EPHEMERAL_DEV_KEY=false`. Fail-safe startup without `JWT_SECRET` verified (`IllegalStateException`, exit 1). Live tokens: access 900s / refresh 604800s. |
| SUPER_ADMIN bootstrap | Opt-in runner used once; exactly one `SUPER_ADMIN` exists (`ACTIVE`), login + privileged endpoints verified, then `APP_BOOTSTRAP_ADMIN_ENABLED=false` again. No credential committed. |
| Backend | `mvn -B clean verify` `BUILD SUCCESS`, 27 tests. 64 live HTTP checks against real MySQL all passed (see the re-verification table above). |
| Customer / Rider apps | `pub get`, `analyze`, `test` (14 + 16) and `flutter build apk --debug` all passed; APKs parsed with `aapt`; distinct IDs, `INTERNET`, debug-only cleartext; no embedded production secrets; delivery copies in `installers/` with sizes + SHA-256. |
| Admin web | `npm install` (0 vulnerabilities), `npm run lint` (0/0), `npm run build` (98 modules). HttpOnly-cookie migration evaluated and deferred (see known issue 4). |
| Six roles | Verified consistent across MySQL schema, backend enum, JWT claims, Spring Security authorities, API authorization, admin TS types, Flutter gates and docs; least privilege preserved; public self-registration still CUSTOMER/RIDER only. |
| Security audit | 321 tracked files; no real secrets, no real `.env`, no APK/AAB/build/cache artifacts; `.gitignore` extended with `installers/`; `git diff --check` clean. |
| Device | `adb devices` returned no device → **no installation or on-device testing claimed**. |

**Remaining blockers before `ACCEPTED`**

1. Install both `installers/` APKs on a physical device or emulator and walk
   through customer registration/login/session-restore/logout and rider
   registration → Pending Approval → admin approval → dashboard.
2. Perform a browser-level admin-web walkthrough (privileged login,
   CUSTOMER/RIDER denial, expired session, refresh failure, logout).
3. Decide on the HttpOnly/Secure/SameSite refresh-cookie hardening for a
   production deployment (a larger architectural change).

**Status:** `READY_FOR_REVIEW` — unchanged. Every mandatory criterion that can
be evidenced without a device has evidence; the brief requires keeping
`READY_FOR_REVIEW` when manual device/browser testing is still outstanding.

**Git:** a normal fast-forward push to `origin/main` is explicitly authorized
for this task and is executed as the final gate after the reviewed
finalization commit. No force-push, no amend, no history rewrite, no
`git reset --hard`, no `git clean -fd`, no `git add .`. The resulting remote
commit hash and final Git status are reported in the Goal 02 final
deliverables report (not duplicated here, since this document is committed
before the push). No Goal 03 work was started.

### Change log

| Date | Change |
|---|---|
| 2026-11-10 | Goal 02 plan approved; implementation began from clean `main` (`2b828a6`) |
| 2026-11-11 | Backend auth/role layer, SQL migrations, bootstrap and H2 integration suite completed |
| 2026-11-11 | Admin web, customer app and rider app auth implemented; debug APKs built and verified |
| 2026-11-11 | Documentation set updated; full verification sweep passed (57 tests) |
| 2026-11-11 | Implementation committed as `3ca41ca`; status set to `READY_FOR_REVIEW` |
| 2026-11-11 | This progress record committed as a separate follow-up; **no push** (per the then-current policy) |
| 2026-11-12 | Finalization: live MySQL schema verification and 64 live HTTP checks against real MySQL all passed |
| 2026-11-12 | JWT secret configured + fail-safe verified; first `SUPER_ADMIN` bootstrapped and bootstrap disabled again |
| 2026-11-12 | Both Flutter apps re-verified and rebuilt; `installers/` delivery APKs with sizes + SHA-256; no secrets embedded |
| 2026-11-12 | Admin web `npm install`/lint/build clean; HttpOnly-cookie migration evaluated and deferred with documented trade-off |
| 2026-11-12 | Six-role consistency + final security audit passed; status kept `READY_FOR_REVIEW` |
