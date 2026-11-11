# BD QuickMart — Database Schema

> **Status:** Goal 02 authentication schema implemented as SQL migrations.
>
> The database itself was created by the developer on 2026-11-09 by running
> `infrastructure/database/init/00_create_database.sql` in phpMyAdmin, and the
> connection was verified against the live server:
> `GET /api/v1/health` reports `"database": "UP"`.
>
> Goal 02 ships `02_create_auth_tables.sql` defining `users` and
> `refresh_tokens`, plus a read-only verification script and a first-admin
> bootstrap template. **The agent verified the live MySQL server read-only and
> did not import these tables** — importing them is a manual developer action
> (see [`docs/development/local-setup.md`](../development/local-setup.md)).
> Until that import is run, the live `bd_quickmart` still contains zero tables.
>
> The remaining module tables are produced in later, separately approved
> goals. This document records what is fixed now so that later design work
> starts from a known baseline.

---

## 1. Database identity

| Property | Value | Notes |
|---|---|---|
| Database name | `bd_quickmart` | Created by the developer in XAMPP/phpMyAdmin |
| Engine | InnoDB | Required for foreign keys and transactions |
| Character set | `utf8mb4` | Stores Bangla script and emoji correctly |
| Collation | `utf8mb4_unicode_ci` | Case- and accent-insensitive comparison |
| Server | MySQL 8 (XAMPP) | Local development only |
| Port | `3306` | XAMPP default |

The database is **not** created by any script in this repository. Creating it is
a manual developer action — see [`docs/development/local-setup.md`](../development/local-setup.md).

---

## 2. Scripts provided

All scripts live in `infrastructure/database/init/` and are executed manually
through the phpMyAdmin **SQL** tab. They are numbered in run order.

| Script | Purpose | When to run |
|---|---|---|
| `00_create_database.sql` | Creates `bd_quickmart` with the correct charset and collation | Once, before first backend start |
| `01_verify_connection.sql` | Confirms the database exists, is reachable and has the right charset | After `00` |
| `02_create_auth_tables.sql` | Creates `users` and `refresh_tokens` (Goal 02 auth schema) | Before first Goal 02 backend start |
| `03_verify_auth_tables.sql` | Read-only `information_schema` check of both auth tables | After `02` |
| `04_admin_bootstrap_TEMPLATE.sql` | Commented `INSERT` template for the first `SUPER_ADMIN` | Optional alternative to the env bootstrap |

`00_create_database.sql` uses `CREATE DATABASE IF NOT EXISTS` and
`02_create_auth_tables.sql` uses `CREATE TABLE IF NOT EXISTS`, so both are safe
to run repeatedly. No script drops, truncates, deletes or modifies any other
database, table or row on the server. `04_admin_bootstrap_TEMPLATE.sql` is
intentionally **not runnable as-is**: its `INSERT` is commented out and the
password is a `<BCRYPT_HASH>` placeholder.

---

## 3. Current schema state

**Defined in migrations, not yet imported into the live server.** Goal 01
prepared only the connection foundation; Goal 02 adds the first two tables as
SQL migration scripts. A read-only check of the live server on 2026-11-11 still
returned `table_count = 0` for `bd_quickmart` — the developer must run
`02_create_auth_tables.sql` before the backend can serve auth requests.

`ddl-auto` is pinned to `none` in `backend/src/main/resources/application.yml`,
so Hibernate will never create or alter tables on its own. Schema ownership
stays with the developer-managed MySQL instance; there is no Flyway/Liquibase
automation.

### Goal 02 tables

**`users`** — one row per account across all six roles.

| Column | Type | Notes |
|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | Surrogate primary key |
| `public_id` | `CHAR(36)` | Unique external identifier (UUID), exposed in JWT `sub` and API responses |
| `email` | `VARCHAR(255)` | Unique, login identifier |
| `phone` | `VARCHAR(32)` | Unique, login identifier |
| `password_hash` | `VARCHAR(255)` | BCrypt hash; never returned by the API |
| `full_name` | `VARCHAR(120)` | |
| `role` | `VARCHAR(32)` | `CUSTOMER`, `RIDER`, `ADMIN`, `SUPER_ADMIN`, `STORE_MANAGER`, `INVENTORY_OPERATOR` |
| `status` | `VARCHAR(24)` | `ACTIVE`, `PENDING`, `REJECTED`, `DISABLED` |
| `version` | `BIGINT` | Optimistic-lock version |
| `created_at` / `updated_at` | `DATETIME(6)` | JPA-audited, UTC |
| `deleted_at` | `DATETIME(6) NULL` | Soft-delete marker |

Indexes: unique on `public_id`, `email`, `phone`; composite on
(`role`, `status`) for the pending-rider query.

**`refresh_tokens`** — persisted rotation state. **Only the SHA-256 hash of
each token is stored**, never the raw token.

| Column | Type | Notes |
|---|---|---|
| `id` | `BIGINT AUTO_INCREMENT` | Primary key |
| `user_id` | `BIGINT` | Foreign key to `users(id)` |
| `token_hash` | `CHAR(64)` | Unique SHA-256 hex digest |
| `family_id` | `CHAR(36)` | Groups a rotation chain; reuse of a revoked token revokes the whole family |
| `expires_at` | `DATETIME(6)` | Absolute expiry |
| `revoked_at` | `DATETIME(6) NULL` | Set on rotation, logout or replay detection |
| `replaced_by_hash` | `CHAR(64) NULL` | Links to the successor token |
| `version` | `BIGINT` | Optimistic-lock version |

Both tables are `ENGINE = InnoDB`, `DEFAULT CHARSET = utf8mb4`,
`COLLATE = utf8mb4_unicode_ci`. The JPA entities use portable annotations with
no MySQL-only `columnDefinition`, so the same mappings run unmodified against
the H2 in-memory database used by the integration tests (`MODE=MySQL`).

---

## 4. Planned module → table mapping

This is the intended ownership boundary for future goals. It is a **plan, not
an implemented schema** — no column, index or constraint is committed to yet.

| Backend module | Planned tables | Owning goal |
|---|---|---|
| `user` | `users` **(Goal 02 — implemented)**, `user_addresses` (deferred) | Goal 02 |
| `auth` | `refresh_tokens` **(Goal 02 — implemented)**, `otp_verifications` (deferred) | Goal 02 |
| `customer` | `customers` (deferred — no customer-specific table in Goal 02) | Goal 02 |
| `rider` | `riders`, `rider_documents` (deferred — rider state lives in `users.role`/`users.status` for Goal 02) | Goal 02 |
| `store` | `stores`, `store_service_areas` | Goal 03 |
| `product` | `products`, `categories`, `product_store_prices` | Goal 03 |
| `inventory` | `inventory_batches`, `stock_movements` | Goal 09 |
| `cart` | `carts`, `cart_items` | Goal 05 |
| `order` | `orders`, `order_items`, `order_status_history` | Goal 06 |
| `payment` | `payments` | Goal 05 |
| `delivery` | `deliveries`, `rider_assignments` | Goal 07 / 08 |
| `tracking` | `rider_locations` (time-series) | Goal 08 |
| `ai` | `product_embeddings`, `recommendation_cache` | Goal 12 |
| `notification` | `notifications`, `device_tokens` | Goal 15 |
| `analytics` | `demand_forecasts` | Goal 10 |

---

## 5. Cross-cutting schema requirements

These constraints apply to every table designed in later goals.

**Language.** Any user-visible text field must carry a paired
`<field>` (English) and `<field>_bn` (Bangla) column, for example
`products.name` and `products.name_bn`. This keeps bilingual search (Goal 04)
and bilingual notifications (Goal 15) indexable in a single table.

**Money.** Monetary amounts use `DECIMAL(12,2)`. `FLOAT`/`DOUBLE` are not used
for money, because binary floating point cannot represent values like `0.10`
exactly and would drift when summed across an order.

**Time.** All timestamps are stored in UTC and rendered in
`Asia/Dhaka` at the presentation layer. The JDBC connection and Jackson
serialization are already configured for this in the backend.

**Identity.** Primary keys are `BIGINT UNSIGNED` auto-increment surrogate keys.
Externally visible identifiers (order number, rider code) are separate columns
with a unique index, so internal key values are never leaked as business IDs.

**Audit columns.** Every table carries `created_at` and `updated_at`.
`@EnableJpaAuditing` is now enabled on the application class (Goal 02) with a
`DateTimeProvider` backed by an injectable `Clock`, so these columns are
populated automatically rather than by hand and are deterministic in tests.

**Soft deletes.** Records that are referenced by historical orders are never
hard-deleted; they carry a `deleted_at` column instead.

---

## 6. Connection configuration

The backend never hardcodes credentials. The datasource is configured entirely
through environment variables.

| Variable | Default | Purpose |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/bd_quickmart?...` | JDBC URL |
| `DB_USERNAME` | `root` | MySQL user |
| `DB_PASSWORD` | *(empty)* | MySQL password |
| `DB_DRIVER` | `com.mysql.cj.jdbc.Driver` | JDBC driver class |
| `DB_DDL_AUTO` | `none` | Hibernate schema handling |
| `DB_DIALECT` | `org.hibernate.dialect.MySQLDialect` | Hibernate dialect |
| `DB_POOL_MAX_SIZE` | `10` | HikariCP maximum pool size |
| `DB_POOL_MIN_IDLE` | `2` | HikariCP minimum idle connections |
| `DB_POOL_CONNECTION_TIMEOUT` | `5000` | HikariCP connection wait in ms. Bounded so a stopped MySQL cannot stall a request for the 30s driver default |
| `DB_POOL_INIT_FAIL_TIMEOUT` | `-1` | Let the app boot while MySQL is stopped |

The URL defaults include `serverTimezone=Asia/Dhaka`,
`characterEncoding=UTF-8` — both required for correct Bangla storage — plus
`connectTimeout=2000` and `socketTimeout=10000`, so the MySQL driver itself
fails fast instead of waiting on a dead socket.

---

## 7. Verifying the connection

Three checks, in order:

1. **Script check** — run `01_verify_connection.sql` in phpMyAdmin. It must
   return the database name, the MySQL version and `utf8mb4` / `utf8mb4_unicode_ci`.
2. **Backend check** — start the backend and call `GET /api/v1/health`.
   The `data.database` field must read `UP`. While MySQL is stopped it reads
   `DOWN` — this is expected and is the honest reported state.
3. **Pool check** — `GET /actuator/health` shows the same information in
   Spring Boot's own format.
