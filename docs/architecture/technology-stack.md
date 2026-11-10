# BD QuickMart — Technology Stack

The stack is fixed by the project brief. This document records the exact
versions in use, why each choice was made, and the constraints that apply to
each.

---

## 1. Version matrix

| Layer | Technology | Version | Status |
|---|---|---|---|
| Backend runtime | Java (Eclipse Temurin) | 21.0.13 LTS | Verified |
| Backend framework | Spring Boot | 3.5.6 | Verified |
| Build tool | Apache Maven | 3.9.9 | Verified |
| Persistence | Spring Data JPA / Hibernate | Managed by Spring Boot 3.5.6 | Verified |
| Database | MySQL | 8 (XAMPP) | **Pending developer setup** |
| API documentation | Springdoc OpenAPI (WebMVC UI) | 2.8.13 | Verified |
| Security | Spring Security | Managed by Spring Boot 3.5.6 | Verified |
| Real-time | Spring WebSocket | Managed by Spring Boot 3.5.6 | Verified |
| Customer app | Flutter | 3.35.1 | Verified |
| Rider app | Flutter | 3.35.1 | Verified |
| Dart SDK | (bundled with Flutter) | 3.9.0 | Verified |
| Admin web | React | 19.3.1 | Verified |
| Admin web language | TypeScript | ~7.0.2 | Verified |
| Admin web build | Vite | 8.3.6 | Verified |
| Admin web routing | React Router | 7.x | Verified |
| Admin web HTTP | Axios | latest | Verified |
| Node.js | Node | 24.18.0 | Verified |

---

## 2. Backend

### Java 21 LTS

Chosen for virtual threads, which matter for a platform that will hold
WebSocket connections open for live tracking alongside ordinary REST traffic.
It is also the current LTS release, so it stays supported for years.

### Spring Boot 3.5.x

The Spring Boot 3.x line requires Java 17 or newer and Jakarta EE APIs, which
the project needs anyway. Version 3.5.6 is the current 3.5.x patch.

### Maven over Gradle

Both would work. Maven was chosen because its configuration is declarative and
its defaults are predictable across machines, which matters more than build
speed for a project of this size.

### Spring Data JPA / Hibernate

Chosen because the domain is relational and heavily transactional: orders must
update inventory atomically, and stock updates must be safe under concurrency
(Goal 18). JPA's transaction management and optimistic locking support that
directly.

**Note on `ddl-auto`:** pinned to `none`. Hibernate never creates or alters
tables. Schema ownership belongs to the developer-managed MySQL instance until
migrations are introduced in a later goal.

**Note on the Hibernate dialect:** pinned explicitly to
`org.hibernate.dialect.MySQLDialect`. Without it, Hibernate tries to read
dialect metadata over a live JDBC connection and fails to start when MySQL is
stopped. Pinning it lets the backend boot and report database state honestly
through `/api/v1/health` instead of crashing.

### Spring Security

Present from the start so that the security posture is deliberate rather than
retrofit. The current configuration is a stateless, CSRF-free baseline that
permits the public infrastructure endpoints (health, OpenAPI, Swagger UI) and
requires authentication for everything else. JWT issuance and role-based
authorization arrive in Goal 02.

### Springdoc OpenAPI

Chosen over hand-written documentation because it derives the contract from the
code, so the spec cannot drift from the implementation. Serves OpenAPI 3.1 at
`/v3/api-docs` and Swagger UI at `/swagger-ui.html`.

### Spring WebSocket

Chosen over Server-Sent Events because live tracking needs bidirectional
traffic: the rider app sends location updates, the customer app receives them.
Registered at `/ws/tracking`.

---

## 3. MySQL 8 via XAMPP

Chosen because it is already installed on the target machines, it is a
well-understood tool for the developer, and MySQL 8 is a production-reasonable
database for this workload.

**`utf8mb4` / `utf8mb4_unicode_ci`** is mandatory, not a default to accept
casually: the platform stores Bangla script and must support bilingual search
and notifications. `utf8mb3` cannot represent all of it.

The database is created and managed by the developer through phpMyAdmin. No
automation in this repository creates or modifies a database.

---

## 4. Flutter (both mobile apps)

### Flutter over React Native

Chosen for rendering consistency across a fragmented Android device base
common in Bangladesh, and for a single Dart codebase covering both Android and
iOS from one codebase.

### Riverpod for state management

Chosen over Provider because its providers are compile-time safe, testable
without a widget tree, and compose naturally. Over Bloc because Goal 01 has no
event-stream complexity to justify the extra indirection.

### go_router for navigation

Chosen over `Navigator 1.0` because it gives named routes, typed parameters
and declarative redirects — all needed for the guarded flows that authentication
(Goal 02) introduces.

### Dio over `http`

Chosen for interceptors, which is exactly where the bearer token, the trace id
and the response-envelope unwrapping belong. One interceptor location means
auth headers cannot be forgotten on a new endpoint.

### flutter_localizations with a custom delegate

Chosen so the translation table is a single reviewable class today and can
move to ARB files later without changing call sites.

### Distinct applications, not one with a flag

The rider app is a separate package with its own entry point, theme, router,
API client, localization table and domain model. A single codebase with a
role flag would couple the two products, ship customer-only code to riders,
and make it hard to give the rider app a different look and performance
profile.

The rider app is scaffolded as its own package so that location-sensitive
dependencies (for example `geolocator` and `connectivity_plus`) can be added
for its delivery workflow in a later goal without pulling them into the
customer app. Neither plugin is a dependency yet; both apps currently share
the same foundation dependency set.

---

## 5. React + TypeScript (admin web)

### React 19

The standard choice for a React console, with improved async rendering.

### TypeScript

Chosen because the admin console is the surface most likely to handle
heterogeneous backend data — order status unions, role enums, inventory
levels. A strict union such as `OrderStatus` catches a typo at compile time
that would otherwise surface as a wrong badge in production.

### Vite over Webpack

Chosen for fast dev-server startup and a simpler configuration surface. The
production build takes ~140ms on this project.

### React Router 7

Chosen for declarative, nested routing that matches the sidebar-plus-content
layout, and for the data-loading primitives it will serve once modules begin
fetching real data.

### Axios

Chosen for interceptors, matching the backend client's interceptor-based
design so that auth headers and envelope unwrapping are handled identically on
both sides.

---

## 6. AI providers

| Provider | Role | Model |
|---|---|---|
| Gemini | Product recommendation, chatbot, insight generation | `gemini-2.5-flash` (configurable) |
| Groq | Low-latency inference, chatbot fallback | configurable |

The API keys are environment variables, blank by default, with `AI_ENABLED`
defaulting to `false`. The platform runs fully without them.

**An important architectural constraint:** Gemini and Groq are language models.
They are appropriate for explaining data, generating insights, and driving the
conversational shopping assistant. They are **not** appropriate as the
quantitative engine for demand forecasting — a language model does not
reproduce the statistical reliability of a forecasting model trained on
historical sales. Goal 10 therefore stages the forecasting work: moving
averages first, then LLM-assisted analysis, then XGBoost/LightGBM, with the
statistical model as the source of truth and the LLM used to explain it.

---

## 7. Version control

Git on GitHub, one repository holding all four applications. Each application
is independently runnable and independently buildable, but a single repository
keeps the API contract and the three clients in one reviewable unit — which is
what makes a monorepo the right fit for a modular monolith.

---

## 8. Rules that follow from this stack

1. **No secrets in the repository.** Environment variables only, `.env` git-ignored,
   `.env.example` committed.
2. **No premature infrastructure.** No broker, no orchestrator, no service mesh.
   The stack grows when a goal requires it.
3. **The database is developer-managed.** The repository provides scripts and
   documentation, never a database mutation.
4. **One version of the API contract.** The OpenAPI document is generated from
   the backend and is the single source of truth for all three clients.
5. **Honest empty states.** A placeholder screen states which goal fills it.
   The admin dashboard shows `—` rather than invented numbers.
