# BD QuickMart — Environment Variables

Every configuration value that varies between machines is read from the
environment. **No credential is committed to the repository.** Each application
has an `.env.example`; the matching `.env` is git-ignored.

---

## 1. Backend (`backend/`)

Read via `application.yml` placeholders and the `app.*` configuration
properties.

> **How the values reach the process.** Spring Boot does not read `.env` files.
> On Windows PowerShell, start the backend from the repository root with
> `.\start-backend.ps1`, which loads the root `.env` into the process
> environment (values are never printed, variables already set in the
> environment are not overridden, and the shell is restored on exit). On
> Linux / macOS / Git Bash, export the variables first, or set them in an IDE
> run configuration. See
> [local-setup.md](local-setup.md#3-running-the-backend). Production keeps
> using real environment variables with the packaged jar — this launcher is a
> local-development convenience only.

### Database — single connection URI (recommended)

The backend can be driven by the **complete Supabase connection URI** directly,
so no host/port/user/password need to be extracted by hand. The
`DatabaseUrlEnvironmentPostProcessor` parses the URI at startup, percent-decodes
a URL-encoded password, and injects `spring.datasource.*` at the highest
precedence. `sslmode=require` is enforced when the URI omits it.

| Variable | Default | Required | Description |
|---|---|---|---|
| `SUPABASE_DATABASE_URL` | *(blank)* | No | Complete Supabase **Session Pooler** URI, e.g. `postgresql://postgres.<PROJECT_REF>:<URL_ENCODED_PASSWORD>@<POOLER_HOST>:5432/postgres`. **Secret** — never commit. When set it is **authoritative** and overrides `DB_URL`/`DB_USERNAME`/`DB_PASSWORD`/`DB_DIALECT` below. |
| `DATABASE_URL` | *(blank)* | No | Generic alias (Heroku/Render convention). Used only when it is a PostgreSQL URI and `SUPABASE_DATABASE_URL` is blank. |

**Precedence:** `SUPABASE_DATABASE_URL` > `DATABASE_URL` (if PostgreSQL) >
split `DB_*` variables. Leaving both URI variables blank falls back to the
`DB_*` split form below (used by `render.yaml` secret env vars). If the local
`.env` still contains stale MySQL-era `DB_*` values, a present
`SUPABASE_DATABASE_URL` safely overrides them. Credentials are never placed in
the JDBC URL (which is safe to log); they go to Hikari as separate fields.

### Database — runtime (split variables, least-privilege role, DML only)

These configure the connection the **running application** uses when no
connection URI is set. The schema is owned by Flyway (see the migration
variables below); the runtime role has no DDL rights and
`spring.flyway.enabled` defaults to `false`.

| Variable | Default | Required | Description |
|---|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/postgres` | No | Supabase **Session Pooler** (or direct) JDBC URL with `?sslmode=require`. MySQL-only URL parameters are gone. |
| `DB_USERNAME` | `postgres` | No | Least-privilege application role (`<DB_APP_ROLE>`). Never the `postgres` superuser in production. |
| `DB_PASSWORD` | *(empty)* | No | Application-role password. Secret; never committed. |
| `DB_DRIVER` | `org.postgresql.Driver` | No | JDBC driver class. |
| `DB_DDL_AUTO` | `none` | No | Hibernate schema handling. Keep `none` — set `validate` in CI/staging to assert entity/schema alignment; Hibernate must never create or alter tables. |
| `DB_DIALECT` | `org.hibernate.dialect.PostgreSQLDialect` | No | Pinned so the Spring context builds without a live DB connection. |
| `DB_POOL_MAX_SIZE` | `10` | No | HikariCP maximum pool size. Keep small (~5–10) for Supabase connection limits. |
| `DB_POOL_MIN_IDLE` | `2` | No | HikariCP minimum idle connections. |
| `DB_POOL_CONNECTION_TIMEOUT` | `5000` | No | Bounded connection wait in ms. |
| `DB_POOL_INIT_FAIL_TIMEOUT` | `-1` | No | `-1` lets the backend boot while PostgreSQL is temporarily unavailable; readiness reports the real state. |
| `DB_SHOW_SQL` | `false` | No | Logs generated SQL. Enable only for debugging. |

> `DB_DIALECT_VERSION` (the old MySQL server-version workaround) is **removed**;
> it has no meaning on PostgreSQL.

### Database — Flyway migration step (DDL-capable role, separate)

These are used **only** by the out-of-band migration step
(`mvn flyway:migrate`, or the optional in-app path). Use the Supabase **direct**
connection, not the pooler, and a role with schema DDL privileges. They do not
belong in the long-running application process and must never be committed.

| Variable | Default | Required | Description |
|---|---|---|---|
| `DB_MIGRATION_URL` | *(empty)* | Only for migration | Direct-connection JDBC URL with `sslmode=require`. |
| `DB_MIGRATION_USERNAME` | *(empty)* | Only for migration | DDL-capable migration role. |
| `DB_MIGRATION_PASSWORD` | *(empty)* | Only for migration | Migration-role password. Secret; never committed. |
| `SPRING_FLYWAY_ENABLED` | `false` | No | Leave `false` in production (separate migration step). If `true`, all three `DB_MIGRATION_*` variables **must** be set so Flyway never uses the runtime role. |
| `SPRING_FLYWAY_VALIDATE_ON_MIGRATE` | `true` | No | Flyway checksum validation. |
| `SPRING_FLYWAY_BASELINE_ON_MIGRATE` | `false` | No | Never baseline a fresh database silently. |

### Server / deployment

| Variable | Default | Description |
|---|---|---|
| `PORT` | *(unset)* | Injected by Render; the app binds via `${PORT:${SERVER_PORT:8080}}`. Do not set it to an empty value locally. |
| `SERVER_PORT` | `8080` | Local HTTP port fallback. |
| `SERVER_CONTEXT_PATH` | `/` | Context path prefix. Leave as `/`. |
| `APP_PUBLIC_URL` | `http://localhost:8080` | Public base URL advertised in the OpenAPI server list (no secret). |
| `LOG_LEVEL_BACKEND` | `INFO` | Log level for `com.bdquickmart`. |
| `LOG_LEVEL_SQL` | `INFO` | Log level for `org.hibernate.SQL`. |
| `LOG_FILE` | `logs/bd-quickmart-backend.log` | Rolling log file path. |

### Probes

`/api/v1/health` and `/actuator/health/liveness` are **liveness** (always 200
while the app is up, even during a PostgreSQL outage; the `db` indicator is
deliberately excluded from the liveness group).
`/actuator/health/readiness` is **readiness** (`readinessState` + `db`) and
returns **503** when PostgreSQL is unreachable.

### API

| Variable | Default | Description |
|---|---|---|
| `APP_API_VERSION` | `v1` | API version segment. |
| `APP_API_BASE_PATH` | `/api` | API base path. |
| `APP_CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:3000` | Comma-separated origins allowed to call the API from a browser. Add the deployed admin-web (Vercel) origin in production. |

### AI providers

| Variable | Default | Description |
|---|---|---|
| `GEMINI_API_KEY` | *(blank)* | Gemini API key. Leave blank to run without it. |
| `GROQ_API_KEY` | *(blank)* | Groq API key. Leave blank to run without it. |
| `AI_CHAT_MODEL` | `gemini-2.5-flash` | Chat/recommendation model. |
| `AI_EMBEDDING_MODEL` | `text-embedding-004` | Embedding model for product search. |
| `AI_ENABLED` | `false` | Master switch for AI features. |

With `AI_ENABLED=false` and blank keys, the platform runs fully and no AI
feature attempts a network call.

### Authentication / JWT (Goal 02)

| Variable | Default | Description |
|---|---|---|
| `JWT_SECRET` | *(blank)* | HS256 signing secret, Base64-encoded or a raw passphrase of **at least 32 bytes**. Generate with `openssl rand -base64 48`. A blank value makes the backend fail fast outside `dev`/`test`. Never commit a real secret. |
| `JWT_ISSUER` | `bd-quickmart-backend` | `iss` claim, validated on parse. |
| `JWT_ACCESS_EXPIRATION` | `900` | Access-token lifetime in seconds (15 min). |
| `JWT_REFRESH_EXPIRATION` | `604800` | Refresh-token lifetime in seconds (7 days). |
| `JWT_ALLOW_EPHEMERAL_DEV_KEY` | `false` | Emergency local-dev escape hatch. Generates an ephemeral key when `JWT_SECRET` is blank; **all tokens invalidate on restart**. Never enable in production. |

### First-administrator bootstrap (Goal 02)

There is no public admin registration. This opt-in runner creates the first
`SUPER_ADMIN` on startup and is **disabled by default**.

| Variable | Default | Description |
|---|---|---|
| `APP_BOOTSTRAP_ADMIN_ENABLED` | `false` | Set `true` for exactly one startup, then disable again. Env vars are visible through `ps`. |
| `APP_BOOTSTRAP_ADMIN_EMAIL` | *(blank)* | Required when enabled. |
| `APP_BOOTSTRAP_ADMIN_PASSWORD` | *(blank)* | Required when enabled. Hashed with BCrypt immediately; never logged. |
| `APP_BOOTSTRAP_ADMIN_FULL_NAME` | *(blank)* | Required when enabled. |
| `APP_BOOTSTRAP_ADMIN_PHONE` | *(blank)* | Required when enabled. |

The runner skips creation when any `SUPER_ADMIN` already exists. A historical,
commented-out SQL template for the old MySQL schema is kept at
`infrastructure/database/legacy/mysql/04_admin_bootstrap_TEMPLATE.sql` — it is
intentionally not runnable as-is and is **not** the active path; the
environment-variable runner above is supported.

> **Verified configuration on this machine (2026-11-12):** a >=32-byte random
> `JWT_SECRET` is set in the git-ignored root `.env` (never committed, never
> printed), `JWT_ALLOW_EPHEMERAL_DEV_KEY=false`, access TTL `900` / refresh TTL
> `604800`. The backend was confirmed to **fail fast** when `JWT_SECRET` is
> absent and the ephemeral escape hatch is off. The opt-in bootstrap was run
> once, produced exactly one `SUPER_ADMIN`, and
> `APP_BOOTSTRAP_ADMIN_ENABLED` is `false` again. These values are local
> developer settings — do not copy the real `.env` into the repository.

---

## 2. Customer app (`customer-app/`)

| Variable | Default | Description |
|---|---|---|
| `API_BASE_URL` | `http://10.0.2.2:8080/api/v1` | Backend base URL. `10.0.2.2` reaches the host from the Android emulator. |
| `WS_BASE_URL` | `ws://10.0.2.2:8080/ws/tracking` | WebSocket endpoint for live tracking. |
| `IS_DEVELOPMENT` | `true` | Development flag. |

---

## 3. Rider app (`rider-app/`)

| Variable | Default | Description |
|---|---|---|
| `API_BASE_URL` | `http://10.0.2.2:8080/api/v1` | Backend base URL. |
| `WS_BASE_URL` | `ws://10.0.2.2:8080/ws/tracking` | WebSocket endpoint for assignment pushes and location relay. |
| `IS_DEVELOPMENT` | `true` | Development flag. |

---

## 4. Admin web (`admin-web/`)

Only variables prefixed with `VITE_` are exposed to the browser bundle.
**Never place a secret in this file** — anything here is public to anyone who
loads the page.

| Variable | Default | Description |
|---|---|---|
| `VITE_API_BASE_URL` | `http://localhost:8080/api/v1` | Backend base URL. |
| `VITE_WS_BASE_URL` | `ws://localhost:8080/ws/tracking` | WebSocket endpoint. |
| `VITE_REQUEST_TIMEOUT` | `30000` | Request timeout in milliseconds. |
| `VITE_IS_DEVELOPMENT` | `true` | Development flag. |

---

## 5. Which base URL to use

The three clients run in different places, so "localhost" means different
things to each.

| Running on | Backend URL | WebSocket URL |
|---|---|---|
| Android emulator | `http://10.0.2.2:8080/api/v1` | `ws://10.0.2.2:8080/ws/tracking` |
| iOS simulator | `http://localhost:8080/api/v1` | `ws://localhost:8080/ws/tracking` |
| Browser (admin web) | `http://localhost:8080/api/v1` | `ws://localhost:8080/ws/tracking` |
| Physical device | `http://<LAN-IP>:8080/api/v1` | `ws://<LAN-IP>:8080/ws/tracking` |

Both Flutter apps also support a **runtime** override: the sign-in screen's
**Change server URL** dialog persists a new base URL in local storage
(`ServerConfig`), so a physical device can be pointed at the PC's LAN IP
without rebuilding the APK. A compile-time override is also available via
`flutter run --dart-define API_BASE_URL=http://<LAN-IP>:8080/api/v1`. Never
ship an unverified hardcoded PC IP — the shipped default is the emulator-only
`10.0.2.2`.

---

## 6. Security rules

1. **`.env` is never committed.** It is listed in `.gitignore`. Only
   `.env.example` is tracked.
2. **No key belongs in a client.** The Flutter and web clients are distributed
   to end users; anything in them is extractable. API keys stay on the
   backend, which calls Gemini and Groq server-side.
3. **Separate the migration role from the runtime role.** `DB_MIGRATION_*`
   credentials have schema DDL rights and belong only to the deployment step /
   CI secret store; the running app uses the DML-only `DB_*` credentials and
   never runs Flyway at startup in production.
4. **Verify before committing.** `git status` should never show a `.env` file
   staged for commit. If it does, unstage it and check `.gitignore`.
5. **Rotate anything exposed.** If a key reaches a commit, revoke it with the
   provider and issue a new one. Removing it from the history afterwards does
   not make it secret again.
