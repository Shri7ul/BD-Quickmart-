# PostgreSQL / Supabase operational scripts

These are **operator templates**, not application migrations. The authoritative
schema is applied by Flyway from
`backend/src/main/resources/db/migration/V1__init_auth_schema.sql` as a separate
deploy step. Nothing in this folder is executed automatically by the backend or
by `mvn verify`.

| File | Purpose | Run when |
|---|---|---|
| `01_create_app_role_TEMPLATE.sql` | Create the **least-privilege runtime role** (DML only) and grant it access to the migrated tables. **TEMPLATE** — contains a placeholder password and is NOT runnable as-is. | Once, by an administrator in the Supabase SQL editor, after `V1` has been applied. Requires the project owner's approval. |
| `02_verify_schema.sql` | Read-only verification queries (tables, columns, indexes, constraints, FK) using PostgreSQL catalogs. | After migrations, to confirm the fresh schema. |

## Separation of privileges

- **Migration role** (`DB_MIGRATION_*` / the Supabase `postgres` connection
  used by `mvn flyway:migrate`): owns the schema, has DDL rights, used **only**
  during the deploy step, and is never placed in the long-running application.
- **Runtime role** (`DB_*` / `SUPABASE_DATABASE_URL` when pointed at a
  least-privilege login): `SELECT, INSERT, UPDATE, DELETE` on tables and
  `USAGE, SELECT` on sequences only. **No** `CREATEDB`, `SUPERUSER`,
  `BYPASSRLS`, and no `CREATE` on the `public` schema. The running app has
  `spring.flyway.enabled=false`, so it never performs DDL.

> Creating or altering roles and granting privileges changes production
> permissions. It is intentionally **NOT** automated by the application. Run
> these statements yourself in the Supabase SQL editor, substituting real
> values out-of-band, and never commit the resulting secrets.

See `docs/development/DEPLOYMENT.md` and
`docs/development/CLOUD_MIGRATION_PLAN.md` §6.5 for the full procedure.
