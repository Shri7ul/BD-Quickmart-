# Legacy MySQL scripts — HISTORICAL ONLY, DO NOT RUN

> **These five scripts are retained for historical reference only. They are
> MySQL 8 / XAMPP artifacts from Goals 01–02 and are NOT part of the active
> schema path. Do not run them against the Supabase PostgreSQL database. They
> will fail (MySQL-only syntax such as `USE`, backtick quoting, `AUTO_INCREMENT`,
> `ENGINE=InnoDB`, `utf8mb4_unicode_ci`, `DATETIME(6)`), and even a successful
> run would create the wrong, conflicting schema.**

The active, authoritative schema is the versioned Flyway migration:

```
backend/src/main/resources/db/migration/V1__init_auth_schema.sql
```

It is applied as a **separate, out-of-band step** with dedicated DDL-capable
migration credentials (`DB_MIGRATION_*`), before the backend starts. The running
application connects with a least-privilege DML-only role and has
`spring.flyway.enabled=false` by default. See
[`docs/development/local-setup.md`](../../../../docs/development/local-setup.md)
for the exact setup, role and migration commands, and
[`docs/development/CLOUD_MIGRATION_PLAN.md`](../../../../docs/development/CLOUD_MIGRATION_PLAN.md)
for the design rationale (§3.8 recommends retaining these as a historical
reference; nothing was deleted).

## Why they are kept

Goal 02's local verification evidence (recorded in
[`docs/progress.md`](../../../../docs/progress.md)) was produced against a real
MySQL 8 instance using these scripts. They document that original schema and
the pre-migration baseline. No legacy MySQL data is migrated to PostgreSQL —
the cloud database is fresh.

## Former run order (obsolete)

| Script | Former purpose |
|---|---|
| `00_create_database.sql` | Created `bd_quickmart` with `utf8mb4` / `utf8mb4_unicode_ci` |
| `01_verify_connection.sql` | Read-only connection/charset check |
| `02_create_auth_tables.sql` | Created `users` and `refresh_tokens` |
| `03_verify_auth_tables.sql` | Read-only `information_schema` check |
| `04_admin_bootstrap_TEMPLATE.sql` | Commented-out first-`SUPER_ADMIN` `INSERT` template (never runnable as-is) |

All five contain only `CREATE ... IF NOT EXISTS`, `SELECT`, or commented SQL —
none contains `DROP`, `TRUNCATE`, `ALTER` or `DELETE`. They are inert unless a
human manually executes them, which is exactly what must not happen on the
PostgreSQL deployment.
