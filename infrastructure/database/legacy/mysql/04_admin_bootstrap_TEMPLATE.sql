-- #####################################################################
-- LEGACY / HISTORICAL -- DO NOT RUN.
-- MySQL 8 / XAMPP artifact from Goals 01-02. NOT the active schema path.
-- Active schema: backend/src/main/resources/db/migration/V1__init_auth_schema.sql
-- (Flyway, Supabase PostgreSQL). Use APP_BOOTSTRAP_ADMIN_* instead of this
-- template. See README.md in this folder.
-- #####################################################################

-- =====================================================================
-- BD QuickMart - Goal 02: first-admin bootstrap TEMPLATE
-- =====================================================================
-- *** THIS FILE IS A TEMPLATE. IT IS INTENTIONALLY NOT RUNNABLE AS-IS. ***
--
-- The INSERT below is commented out and contains a literal placeholder
-- <BCRYPT_HASH> that is NOT valid SQL. Running this file unchanged does
-- nothing (and cannot accidentally create an account). You MUST replace
-- the placeholder with a real BCrypt hash and uncomment the INSERT before
-- use, or use the opt-in APP_BOOTSTRAP_ADMIN_* environment variables
-- instead (recommended, see below).
--
-- HOW TO GENERATE A BCRYPT HASH (never store the plaintext password):
--   Use a trusted BCrypt tool / `htpasswd -bnBC 10 "" 'your-password'`,
--   or let the application hash it for you via the env-var bootstrap.
--
-- SAFETY: This script contains no DROP / TRUNCATE / DELETE / UPDATE.
-- =====================================================================

USE `bd_quickmart`;

-- Guidance only - this SELECT always runs and is harmless.
SELECT
    'Replace <BCRYPT_HASH> and <values> below, then uncomment the INSERT. '
    'Prefer the APP_BOOTSTRAP_ADMIN_* env switch instead.' AS `instructions`;

-- ---------------------------------------------------------------------
-- Uncomment ONLY after substituting every <...> placeholder.
-- role MUST be one of the four privileged roles; SUPER_ADMIN is used here.
-- ---------------------------------------------------------------------
-- INSERT INTO `users`
--     (`public_id`, `email`, `phone`, `password_hash`, `full_name`,
--      `role`, `status`, `version`, `created_at`, `updated_at`, `deleted_at`)
-- VALUES
--     (UUID(), '<ADMIN_EMAIL>', '<ADMIN_PHONE>', '<BCRYPT_HASH>',
--      '<ADMIN_FULL_NAME>', 'SUPER_ADMIN', 'ACTIVE', 0,
--      UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), NULL);

-- =====================================================================
-- Recommended alternative: environment-variable bootstrap
-- =====================================================================
--   APP_BOOTSTRAP_ADMIN_ENABLED=true
--   APP_BOOTSTRAP_ADMIN_EMAIL=admin@example.com
--   APP_BOOTSTRAP_ADMIN_PASSWORD=<a strong one-off password>
--   APP_BOOTSTRAP_ADMIN_FULL_NAME=Initial Administrator
--   APP_BOOTSTRAP_ADMIN_PHONE=+8801XXXXXXXXX
--
-- The runner creates the first SUPER_ADMIN only when none exists, hashes
-- the password immediately, and never logs it. Environment variables are
-- visible via `ps`, so turn APP_BOOTSTRAP_ADMIN_ENABLED off again right
-- after the first successful start.
-- =====================================================================
