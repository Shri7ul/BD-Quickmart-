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
<http://localhost/phpmyadmin> → **SQL** tab, and run
`infrastructure/database/init/00_create_database.sql`. This creates the
`bd_quickmart` database with `utf8mb4` / `utf8mb4_unicode_ci`.

**The repository never creates or modifies a database for you.** It ships the
script and the documentation; you run it.

### 2. Environment

```bash
cp .env.example .env
cp customer-app/.env.example customer-app/.env
cp rider-app/.env.example rider-app/.env
cp admin-web/.env.example admin-web/.env
```

Set `DB_USERNAME` / `DB_PASSWORD` in the root `.env` to match your XAMPP
MySQL account.

### 3. Run

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

---

## Verification

| Check | Command | Expected |
|---|---|---|
| Backend tests | `cd backend && mvn -B test` | `Tests run: 2, Failures: 0, Errors: 0` |
| Backend package | `cd backend && mvn -B package` | `BUILD SUCCESS` |
| Health endpoint | `curl http://localhost:8080/api/v1/health` | `"success": true` |
| Swagger UI | <http://localhost:8080/swagger-ui.html> | HTTP 200 |
| OpenAPI document | <http://localhost:8080/v3/api-docs> | HTTP 200, OpenAPI 3.1 |
| Customer analysis | `cd customer-app && flutter analyze` | `No issues found!` |
| Customer tests | `cd customer-app && flutter test` | `All tests passed!` |
| Rider analysis | `cd rider-app && flutter analyze` | `No issues found!` |
| Rider tests | `cd rider-app && flutter test` | `All tests passed!` |
| Admin lint | `cd admin-web && npm run lint` | `0 warnings and 0 errors` |
| Admin build | `cd admin-web && npm run build` | `✓ built` |

Latest recorded results: [docs/progress.md](docs/progress.md).

---

## API

- Base path: `/api/v1`
- Health: `GET /api/v1/health`
- OpenAPI: `GET /v3/api-docs`
- Swagger UI: `/swagger-ui.html`
- WebSocket: `/ws/tracking`

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

---

## Documentation

| Document | Contents |
|---|---|
| [docs/architecture/system-overview.md](docs/architecture/system-overview.md) | Architecture, module boundaries, API conventions |
| [docs/architecture/technology-stack.md](docs/architecture/technology-stack.md) | Exact versions, rationale, constraints |
| [docs/development/local-setup.md](docs/development/local-setup.md) | Toolchain, XAMPP database setup, troubleshooting |
| [docs/development/environment-variables.md](docs/development/environment-variables.md) | Every environment variable |
| [docs/database/schema.md](docs/database/schema.md) | Database configuration and planned schema |
| [docs/progress.md](docs/progress.md) | Goal-by-goal status and verification evidence |

---

## Current state

**Goal 01 — System Architecture & Project Foundation** is `ACCEPTED`.

The foundation is in place and verified: the monorepo structure, the backend
skeleton with API versioning, global error handling, OpenAPI documentation,
logging and trace correlation; both Flutter applications with independent
architecture, theming, routing and Bangla/English localization; the admin
console with routing, layout, API client and responsive design; and the
database connection foundation.

The database `bd_quickmart` was created by the developer in phpMyAdmin, and the
connection is verified against the live MySQL server — `GET /api/v1/health`
reports `"database": "UP"`.

**No business feature is implemented yet.** Screens that would show live data
are placeholders that state which goal delivers them. The admin dashboard shows
`—` rather than invented numbers.

---

## License

Proprietary. See [LICENSE](LICENSE).
