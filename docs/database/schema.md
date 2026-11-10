# BD QuickMart — Database Schema

> **Status:** Foundation only. No tables exist yet.
>
> The database itself was created by the developer on 2026-11-09 by running
> `infrastructure/database/init/00_create_database.sql` in phpMyAdmin, and the
> connection was verified against the live server:
> `GET /api/v1/health` reports `"database": "UP"`.
>
> The full relational design, indexes, constraints and migrations are produced
> in a later, separately approved goal. This document records what is fixed
> now so that later design work starts from a known baseline.

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

Both scripts live in `infrastructure/database/init/` and are executed manually
through the phpMyAdmin **SQL** tab.

| Script | Purpose | When to run |
|---|---|---|
| `00_create_database.sql` | Creates `bd_quickmart` with the correct charset and collation | Once, before first backend start |
| `01_verify_connection.sql` | Confirms the database exists, is reachable and has the right charset | After the first backend start |

`00_create_database.sql` uses `CREATE DATABASE IF NOT EXISTS`, so it is safe to
run repeatedly. Neither script drops, truncates or modifies any other database
on the server.

---

## 3. Current schema state

**No tables.** Goal 01 prepares only the connection foundation.

`ddl-auto` is pinned to `none` in `backend/src/main/resources/application.yml`,
so Hibernate will never create or alter tables on its own. Schema ownership
stays with the developer-managed MySQL instance until migrations are introduced.

The empty-state expectation is asserted by `01_verify_connection.sql`, whose
table listing should return zero rows at the end of Goal 01. This was
confirmed against the live server on 2026-11-09: `information_schema.TABLES`
returned `table_count = 0` for `bd_quickmart`.

---

## 4. Planned module → table mapping

This is the intended ownership boundary for future goals. It is a **plan, not
an implemented schema** — no column, index or constraint is committed to yet.

| Backend module | Planned tables | Owning goal |
|---|---|---|
| `user` | `users`, `user_addresses` | Goal 02 |
| `auth` | `refresh_tokens`, `otp_verifications` | Goal 02 |
| `customer` | `customers` | Goal 02 |
| `rider` | `riders`, `rider_documents` | Goal 02 |
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

**Audit columns.** Every table carries `created_at` and `updated_at`. The
`@EnableJpaAuditing` annotation is re-enabled on the application class as soon
as the first entity is introduced, so these columns are populated
automatically rather than by hand.

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
