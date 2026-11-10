# BD QuickMart — Environment Variables

Every configuration value that varies between machines is read from the
environment. **No credential is committed to the repository.** Each application
has an `.env.example`; the matching `.env` is git-ignored.

---

## 1. Backend (`backend/`)

Read via `application.yml` placeholders and the `app.*` configuration
properties.

### Database

| Variable | Default | Required | Description |
|---|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/bd_quickmart?...` | No | JDBC URL. `serverTimezone=Asia/Dhaka` and `characterEncoding=UTF-8` are required for correct Bangla storage. |
| `DB_USERNAME` | `root` | No | MySQL user. |
| `DB_PASSWORD` | *(empty)* | No | MySQL password. Set this if your XAMPP root account has one. |
| `DB_DRIVER` | `com.mysql.cj.jdbc.Driver` | No | JDBC driver class. |
| `DB_DDL_AUTO` | `none` | No | Hibernate schema handling. Keep `none` — Hibernate must never alter the developer-managed schema. |
| `DB_DIALECT` | `org.hibernate.dialect.MySQLDialect` | No | Hibernate dialect. Pinned so the context starts while MySQL is stopped. |
| `DB_DIALECT_VERSION` | `8.0.0` | No | Minimum server version the dialect targets. Without it Hibernate assumes MySQL 5.5.5 and logs a compatibility warning. |
| `DB_POOL_MAX_SIZE` | `10` | No | HikariCP maximum pool size. |
| `DB_POOL_MIN_IDLE` | `2` | No | HikariCP minimum idle connections. |
| `DB_POOL_INIT_FAIL_TIMEOUT` | `-1` | No | `-1` lets the backend boot while MySQL is stopped. |
| `DB_SHOW_SQL` | `false` | No | Logs generated SQL. Enable only for debugging. |

### Server

| Variable | Default | Description |
|---|---|---|
| `SERVER_PORT` | `8080` | HTTP port. |
| `SERVER_CONTEXT_PATH` | `/` | Context path prefix. Leave as `/`. |
| `LOG_LEVEL_BACKEND` | `INFO` | Log level for `com.bdquickmart`. |
| `LOG_LEVEL_SQL` | `INFO` | Log level for `org.hibernate.SQL`. |
| `LOG_FILE` | `logs/bd-quickmart-backend.log` | Rolling log file path. |

### API

| Variable | Default | Description |
|---|---|---|
| `APP_API_VERSION` | `v1` | API version segment. |
| `APP_API_BASE_PATH` | `/api` | API base path. |
| `APP_CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:3000` | Comma-separated origins allowed to call the API from a browser. |

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

---

## 6. Security rules

1. **`.env` is never committed.** It is listed in `.gitignore`. Only
   `.env.example` is tracked.
2. **No key belongs in a client.** The Flutter and web clients are distributed
   to end users; anything in them is extractable. API keys stay on the
   backend, which calls Gemini and Groq server-side.
3. **Verify before committing.** `git status` should never show a `.env` file
   staged for commit. If it does, unstage it and check `.gitignore`.
4. **Rotate anything exposed.** If a key reaches a commit, revoke it with the
   provider and issue a new one. Removing it from the history afterwards does
   not make it secret again.
