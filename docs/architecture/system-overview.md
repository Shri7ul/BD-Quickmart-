# BD QuickMart — System Overview

## 1. What the platform is

BD QuickMart is an AI-powered hyperlocal quick-commerce platform for
Bangladesh. Customers order groceries and essentials from nearby dark stores,
riders deliver them, and store operators manage inventory and dispatch from a
web console.

The platform is one system with three client applications and one backend.

---

## 2. Architecture style: modular monolith

The backend is a **modular monolith**. All business capability lives in a
single deployable Spring Boot application, but the code is partitioned into
strictly owned modules that communicate through explicit service interfaces
rather than by reaching into each other's internals.

```
┌──────────────────────┐  ┌──────────────────────┐  ┌──────────────────────┐
│   Customer App       │  │    Rider App         │  │   Admin Web          │
│   Flutter (Android   │  │    Flutter (Android  │  │   React + TypeScript │
│   + iOS)             │  │    + iOS)            │  │   (Vite)             │
└──────────┬───────────┘  └──────────┬───────────┘  └──────────┬───────────┘
           │                         │                         │
           │  HTTPS / REST /api/v1   │                         │
           │  WSS  /ws/tracking       │                         │
           └────────────┬────────────┴────────────┬────────────┘
                        │                         │
                 ┌──────▼─────────────────────────▼──────┐
                 │      Spring Boot Modular Monolith    │
                 │      Java 21 · Port 8080              │
                 │                                        │
                 │  ┌──────────────────────────────────┐  │
                 │  │ modules/ (15 reserved packages)  │  │
                 │  │ auth user customer rider store   │  │
                 │  │ product inventory cart order     │  │
                 │  │ payment delivery tracking ai     │  │
                 │  │ notification analytics            │  │
                 │  └──────────────────────────────────┘  │
                 │                                        │
                 │  ┌──────────────────────────────────┐  │
                 │  │ common/  api · exception · util  │  │
                 │  │ config/  openapi · cors · ws     │  │
                 │  └──────────────────────────────────┘  │
                 └────────────────────┬───────────────────┘
                                      │
                    ┌──────────────▼──────────────┐
                    │  Supabase PostgreSQL 16     │
                    │  schema owned by Flyway V1  │
                    │  (Session Pooler + direct)  │
                    └─────────────────────────────┘
```

### Why a modular monolith

The product is broad, but the team is small and the domain is still being
discovered. A modular monolith gives three things at once: a single deployable
(cheap to run), clear module boundaries (cheap to refactor), and in-process
calls (fast). Microservices would add deployment, networking and distributed
tracing costs before a single business rule had been proven.

The module boundaries are designed so that extraction to a service later is
possible if the scale ever justifies it — but that is not a Goal 01
requirement and no infrastructure for it is created now.

### Explicitly out of scope

No Kubernetes, no Kafka, no message broker, no service mesh, no separate
database per module. The only infrastructure is one Supabase PostgreSQL
database (schema owned by versioned Flyway migrations) and one Spring Boot
process.

---

## 3. Backend module boundaries

Fifteen packages under `com.bdquickmart.modules` are reserved now and filled
in by the goals that own each feature.

| Module | Responsibility | Owning goal |
|---|---|---|
| `auth` | Login, JWT issuance, OTP, role enforcement | Goal 02 |
| `user` | Shared user identity, profiles, addresses | Goal 02 |
| `customer` | Customer-specific profile data | Goal 02 |
| `rider` | Rider profile, availability, vehicle | Goal 02 |
| `store` | Dark stores, service radius | Goal 03 |
| `product` | Catalogue, categories, SKU, pricing | Goal 03 |
| `inventory` | Stock levels, batches, expiry, FEFO | Goal 09 |
| `cart` | Cart and cart items | Goal 05 |
| `order` | Order lifecycle and status history | Goal 06 |
| `payment` | COD and online payment records | Goal 05 |
| `delivery` | Assignment, delivery records | Goal 07 / 08 |
| `tracking` | Live rider location and ETA | Goal 08 |
| `ai` | Recommendations, chatbot, forecasting | Goal 10–13 |
| `notification` | Push notifications, device tokens | Goal 15 |
| `analytics` | Aggregations and reporting | Goal 16 |

Cross-cutting concerns live outside `modules/`:

- `common/api` — the `ApiResponse` envelope, result codes, health endpoint.
- `common/exception` — exception types and the global handler.
- `common/util` — the trace-id filter.
- `config` — OpenAPI, CORS, security, WebSocket, typed properties.

---

## 4. API conventions

**Versioning.** Every business endpoint lives under `/api/v1`. The version is
a path segment, not a header, so that the contract is visible in logs and
browser history.

**Response envelope.** Every endpoint returns the same shape:

```json
{
  "success": true,
  "code": "BDQM-0000",
  "message": "Request completed successfully",
  "data": { },
  "path": "/api/v1/health",
  "timestamp": "2026-11-08T18:48:24.305651Z",
  "traceId": "3f2b...-..."
}
```

On error, `success` is `false`, `data` is absent, and `code` identifies the
failure class. The `code` values are stable and machine-readable, so clients
can branch on them without parsing message text.

| Code prefix | Meaning |
|---|---|
| `BDQM-0000` | Success |
| `BDQM-04xx` | Client error (validation, auth, not found, conflict) |
| `BDQM-05xx` | Server error |

**Error handling.** A single `GlobalExceptionHandler` translates every
exception into the envelope. Unhandled exceptions are logged with a full stack
trace and returned to the client as a generic `BDQM-0500`, so internal details
never leak.

**Tracing.** Every request receives a correlation id, taken from the inbound
`X-Trace-Id` header when present and generated otherwise. It appears in the
response header, in the response body, and in every log line for that request.

**Validation.** Request bodies are validated with Jakarta Bean Validation.
Failures produce a `BDQM-0400` response listing the offending fields.

**Authentication (Goal 02).** Public endpoints (`/api/v1/auth/register`,
`/login`, `/refresh`) need no credentials; everything else requires a bearer
JWT. Filter-level authentication and authorization failures are written as the
same standard envelope by a JSON `AuthenticationEntryPoint` and
`AccessDeniedHandler` — a `@RestControllerAdvice` cannot catch exceptions
thrown inside the filter chain. Goal 02 adds `BDQM-0402` (invalid credentials),
`BDQM-0410` (invalid token), `BDQM-0411` (expired token) and `BDQM-0412`
(inactive account). Six roles map to literal authorities such as
`ROLE_CUSTOMER` and `PERM_RIDER_ACTIVE`; there is no role hierarchy, so the
least-privilege model is auditable in one `SecurityConfig` file.

**Documentation.** Springdoc generates OpenAPI 3.1 from the annotated
controllers, served at `/v3/api-docs` with a Swagger UI at `/swagger-ui.html`.

---

## 5. Client applications

Each client is independently executable and talks to the backend over the
network. None of them share code with each other.

### Customer app (`customer-app/`)

Flutter, targeting Android and iOS. Feature-first layout: `features/` holds
`home`, `products`, `cart`, `orders`, `tracking`, `profile` and `auth`, each
with its own `presentation/` and `domain/` layers. `core/` holds the shared
infrastructure — theme, router, API client, localization, reusable widgets.

The visual identity is green, reflecting the grocery category.

### Rider app (`rider-app/`)

Flutter, independently scaffolded with its own package name, entry point,
theme, router, API client and localization table.

It is **not** a copy of the customer app with a different name. Its domain
model is rider-specific: `DeliveryAssignment` with an offer/accept/reject
lifecycle, `RiderStatus`, and `RiderLocation` with an explicit
freshness check so a stale GPS fix is never presented as a live position. Its
routes follow the shift loop (dashboard → assignments → delivery → earnings)
rather than a storefront. Its palette is deep indigo with orange accents, so
the two applications are distinguishable on a shared device, and its
localization table uses rider vocabulary (offers, pickups, payouts) rather
than shopping vocabulary.

### Admin web (`admin-web/`)

React 19 with TypeScript, built by Vite. `src/features/` reserves one folder
per admin capability, `src/components/` holds the reusable UI, `src/lib/api/`
the shared HTTP client, `src/lib/config/` the environment configuration.

The layout is a fixed sidebar on desktop that reflows to a horizontal scroller
below 900px.

---

## 6. Localization

All three clients support Bangla and English from day one. Bangla is the
default locale because it is the primary market language.

Flutter applications use `flutter_localizations` with a custom delegate;
`supportedLocales` lists `bn` first. The admin web includes
`Noto Sans Bengali` in its font stack.

Persisted content follows the same pairing in the database: every
user-visible text column is planned as `<field>` for English plus `<field>_bn`
for Bangla.

---

## 7. Real-time communication

Spring WebSocket is registered at `/ws/tracking`. A placeholder handler
acknowledges the connection today; the live-tracking implementation arrives
with Goal 08.

The design decision that matters: when a rider's uplink drops, the system
reports a **stale** location with its age. It never fabricates a live-looking
position. The rider app enforces this at the domain layer with
`RiderLocation.isFresh()`.

---

## 8. Database

Supabase **PostgreSQL 16** (`UTF8`), reached over TLS. The running application
uses the **Session Pooler** with a least-privilege DML-only role; a separate,
DDL-capable role applies versioned **Flyway** migrations over the **direct**
connection as a distinct deployment step:

```
backend/src/main/resources/db/migration/V1__init_auth_schema.sql
```

The application has `spring.flyway.enabled=false` and `ddl-auto: none`, so it
never creates or alters schema at startup. No legacy MySQL data was migrated —
this is a fresh database. The original MySQL 8 / XAMPP scripts are retained
as a historical, DO-NOT-RUN reference under
`infrastructure/database/legacy/mysql/` and are **not** an active schema path.

Liveness (`/api/v1/health`, `/actuator/health/liveness`) stays up during a DB
outage; readiness (`/actuator/health/readiness`) reports DB connectivity.

See [`docs/database/schema.md`](../database/schema.md) and
[`docs/development/local-setup.md`](../development/local-setup.md).

---

## 9. Configuration and secrets

No credential, API key or password is committed to the repository.

- The backend reads `DB_*`, `SERVER_*`, `APP_*` and AI provider keys from
  environment variables, with development defaults that contain no secrets.
- Each client has its own `.env.example`. The `.env` files are git-ignored.
- The AI keys (`GEMINI_API_KEY`, `GROQ_API_KEY`) are blank by default and
  `AI_ENABLED` defaults to `false`, so the platform runs without them.

---

## 10. What exists today versus what is planned

**Built and verified (Goal 01):** repository structure, backend foundation,
both Flutter applications, admin web console, database connection foundation,
documentation, and the verification evidence recorded in
[`docs/progress.md`](../progress.md).

**Built and verified (Goal 02):** the `auth` and `user` modules are now filled
in — `users` / `refresh_tokens` entities and repositories, JJWT-based access
and refresh tokens with rotation and replay detection, BCrypt password hashing,
stateless Spring Security with a JWT filter and JSON 401/403 handlers, six
canonical roles with explicit authority gates, public registration limited to
`CUSTOMER` / `RIDER`, a PENDING-rider approval workflow, privileged account
provisioning, and a concurrency-safe, opt-in first-`SUPER_ADMIN` bootstrap.
The schema is owned by the Flyway migration
`backend/src/main/resources/db/migration/V1__init_auth_schema.sql`
(Supabase PostgreSQL); the superseded MySQL scripts are kept only as a
historical reference under `infrastructure/database/legacy/mysql/`.

Both Flutter apps gained sign-in, registration, session restore, secure token
storage, a runtime server-URL override, and role/status-aware router guards
(the rider app adds an "awaiting approval" screen). The admin web gained a
guarded router, an `AuthProvider` with silent single-flight refresh, and role
guards. All four applications pass their verification commands and both
Flutter apps produce installable debug APKs with distinct application IDs.

**Not built:** every Goal 03+ business capability. All screens showing real
catalogue/order data remain placeholders that state which goal delivers them.
The admin dashboard shows `—` rather than invented numbers, because a
dashboard built on mock figures is worse than an empty one — it hides whether
the real thing works. OTP verification (listed in the original roadmap) is not
part of the implemented Goal 02 scope.
