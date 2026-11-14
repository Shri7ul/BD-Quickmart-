-- =====================================================================
-- BD QuickMart — PostgreSQL schema verification (READ-ONLY)
-- ---------------------------------------------------------------------
-- Run in the Supabase SQL editor / psql after Flyway applies V1. These are
-- the PostgreSQL equivalents of the legacy MySQL information_schema checks;
-- they use pg_catalog / information_schema views available on Supabase.
-- No DROP / TRUNCATE / ALTER / DELETE / UPDATE appears in this file.
-- =====================================================================

-- Connection / server facts.
SELECT current_database() AS database,
       current_user       AS connected_role,
       version()          AS server_version,
       current_setting('server_encoding') AS server_encoding, -- expect UTF8
       current_setting('TimeZone')        AS server_timezone;

-- Tables in the public schema (expect users, refresh_tokens and
-- flyway_schema_history after a successful migration).
SELECT table_name, table_type
FROM information_schema.tables
WHERE table_schema = 'public'
ORDER BY table_name;

-- Columns, types and nullability of the two implemented tables.
-- TIMESTAMPTZ columns appear as 'timestamp with time zone' (UTC storage).
SELECT table_name, column_name, data_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name IN ('users', 'refresh_tokens')
ORDER BY table_name, ordinal_position;

-- Indexes, including the functional lower() and partial (WHERE) indexes.
SELECT indexname, indexdef
FROM pg_indexes
WHERE schemaname = 'public'
ORDER BY indexname;

-- The two functional unique indexes and the partial live-family index must
-- be present (defence for case-insensitive uniqueness + replay detection).
SELECT indexname,
       indexdef LIKE '%lower(%'  AS is_functional,
       indexdef LIKE '%WHERE%'   AS is_partial
FROM pg_indexes
WHERE schemaname = 'public'
  AND indexname IN ('uk_users_email_lower', 'uk_users_phone_lower',
                    'idx_refresh_family_live');

-- CHECK constraints (six roles / four states) and the FK to users.
SELECT conname, contype, pg_get_constraintdef(oid) AS definition
FROM pg_constraint
WHERE conrelid IN ('public.users'::regclass, 'public.refresh_tokens'::regclass)
ORDER BY conrelid::regclass::text, contype, conname;

-- Flyway history (expect exactly one successful V1 on a fresh database).
SELECT installed_rank, version, description, type, success, installed_on
FROM flyway_schema_history
ORDER BY installed_rank;

-- Least-privilege role check: the runtime role must NOT be able to CREATE
-- in public and must NOT be a superuser.
-- SELECT has_schema_privilege('<DB_APP_ROLE>', 'public', 'CREATE') AS can_ddl; -- expect false
-- SELECT rolsuper FROM pg_roles WHERE rolname = '<DB_APP_ROLE>';               -- expect false
