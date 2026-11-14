# BD QuickMart — Infrastructure

Local infrastructure assets and references for the platform.

---

## Database (active): Supabase PostgreSQL + Flyway

The authoritative schema is a **fresh Supabase PostgreSQL** database owned by
versioned Flyway migrations:

```
backend/src/main/resources/db/migration/
└── V1__init_auth_schema.sql    users, refresh_tokens, constraints, indexes
```

Key points:

- **No legacy MySQL data is migrated** — the cloud database starts empty.
- **Schema ownership belongs to Flyway**, applied as a *separate* step with a
  dedicated DDL-capable migration role (`DB_MIGRATION_URL`,
  `DB_MIGRATION_USERNAME`, `DB_MIGRATION_PASSWORD`). The running application
  connects with a **least-privilege DML-only role** (`DB_URL`, `DB_USERNAME`,
  `DB_PASSWORD`) and has `spring.flyway.enabled=false` by default, so it never
  performs DDL at startup.
- Hibernate uses `ddl-auto: none` (set `DB_DDL_AUTO=validate` in CI/staging to
  assert entity/schema alignment); it never creates or alters tables.
- The app uses the Supabase **Session Pooler** (or direct connection) with
  `sslmode=require`; migrations use the **direct** connection. Avoid the
  transaction-mode pooler because refresh-token rotation uses
  `SELECT ... FOR UPDATE`.

Full setup (project creation, roles, SSL, pooler, exact `flyway:migrate`
command, liveness/readiness probes and verification queries):
[`docs/development/local-setup.md`](../docs/development/local-setup.md) and
[`docs/development/CLOUD_MIGRATION_PLAN.md`](../docs/development/CLOUD_MIGRATION_PLAN.md).

See [`docs/database/schema.md`](../docs/database/schema.md) for the schema
baseline and the module-to-table plan.

### Operational templates (`infrastructure/database/postgres/`)

- `01_create_app_role_TEMPLATE.sql` — prepared (not executed) least-privilege
  runtime role + grants. Run once, manually, with approval, after `V1` is applied.
- `02_verify_schema.sql` — read-only PostgreSQL catalog checks for the migrated
  schema (the PostgreSQL equivalents of the legacy MySQL verification scripts).

These are operator templates; the application never runs them and no real
credential is stored in them. See the folder's `README.md`.

---

## Legacy MySQL scripts — historical only

```
infrastructure/database/legacy/mysql/
├── README.md                        Historical-only notice (read this first)
├── 00_create_database.sql
├── 01_verify_connection.sql
├── 02_create_auth_tables.sql
├── 03_verify_auth_tables.sql
└── 04_admin_bootstrap_TEMPLATE.sql
```

These are the original MySQL 8 / XAMPP scripts from Goals 01–02, kept solely
as a historical reference for the pre-migration baseline. **They are not an
active schema initialization path and must not be run against PostgreSQL.**
They were relocated out of the old `infrastructure/database/init/` path so that
no conflicting initialization path remains. See the folder's `README.md`.

---

## Planned contents

| Path | Purpose | Goal |
|---|---|---|
| `docker/` | Container definitions for a portable local stack | Goal 18 |
| `monitoring/` | Metrics and alerting configuration | Goal 18 |
| `ci/` | GitHub Actions workflows (real-PostgreSQL test suite) | Goal 18 |
| `backup/` | Database backup and restore procedures | Goal 18 |

These are intentionally absent for now; the current scope uses the managed
Supabase PostgreSQL instance and adds no local database server.
