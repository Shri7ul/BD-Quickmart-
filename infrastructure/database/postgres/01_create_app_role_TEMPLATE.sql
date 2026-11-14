-- =====================================================================
-- BD QuickMart — least-privilege RUNTIME role (TEMPLATE — DO NOT RUN AS-IS)
-- ---------------------------------------------------------------------
-- This is an operator template for a FRESH Supabase PostgreSQL database.
-- It is NOT a Flyway migration and is NOT executed by the application.
-- Run it ONCE, manually, as an administrator in the Supabase SQL editor,
-- AFTER V1__init_auth_schema.sql has been applied by Flyway.
--
-- * Substitute <DB_APP_ROLE> and <DB_APP_PASSWORD> OUT OF BAND.
-- * NEVER commit the real password. This file must stay a placeholder.
-- * Creating a role / granting privileges changes production permissions:
--   it requires the project owner's explicit approval and is deliberately
--   not automated by the backend.
--
-- The migration step uses the DDL-capable postgres connection; the runtime
-- app connects with the DML-only role below. Refresh-token rotation uses
-- SELECT ... FOR UPDATE, so use the Session Pooler (or direct) — never the
-- transaction-mode pooler — for DB_URL / SUPABASE_DATABASE_URL.
-- =====================================================================

-- 1. Create the login role (replace placeholders; keep the password secret).
--    The password literal must be substituted at execution time; do not store
--    it in this file or in Git.
CREATE ROLE <DB_APP_ROLE> LOGIN PASSWORD '<DB_APP_PASSWORD>';

-- 2. Schema access. No CREATE on public (the role must not be able to DDL).
GRANT USAGE ON SCHEMA public TO <DB_APP_ROLE>;
REVOKE CREATE ON SCHEMA public FROM <DB_APP_ROLE>;

-- 3. DML on the two implemented tables; identity sequences for INSERT.
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO <DB_APP_ROLE>;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO <DB_APP_ROLE>;

-- 4. Ensure FUTURE tables/sequences created by later versioned migrations
--    (applied by the migration role) are accessible to the runtime role too.
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO <DB_APP_ROLE>;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO <DB_APP_ROLE>;

-- 5. Verification (read-only): the role must NOT be superuser / DDL-capable.
-- SELECT rolname, rolsuper, rolcreatedb, rolbypassrls, rolcanlogin
--   FROM pg_roles WHERE rolname = '<DB_APP_ROLE>';
-- SELECT has_schema_privilege('<DB_APP_ROLE>', 'public', 'CREATE') AS can_ddl; -- expect false
-- SELECT has_table_privilege('<DB_APP_ROLE>', 'public.users', 'INSERT') AS can_insert; -- expect true
