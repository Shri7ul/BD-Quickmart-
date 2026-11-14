-- #####################################################################
-- LEGACY / HISTORICAL -- DO NOT RUN.
-- MySQL 8 / XAMPP artifact from Goals 01-02. NOT the active schema path.
-- Active schema: backend/src/main/resources/db/migration/V1__init_auth_schema.sql
-- (Flyway, Supabase PostgreSQL). See README.md in this folder.
-- #####################################################################

-- =====================================================================
-- BD QuickMart - Connection verification script
-- =====================================================================
-- Run this AFTER the backend has been started at least once, to confirm
-- that the application can reach the database using the credentials from
-- your .env file.
--
-- HOW TO RUN:
--   1. Open phpMyAdmin > SQL tab
--   2. Paste and run this script
--   3. Then start the backend and call GET /api/v1/health
--
-- The "database" field in the health response must read "UP".
-- =====================================================================

USE `bd_quickmart`;

-- 1. Confirm the database exists and is reachable.
SELECT
    DATABASE()  AS `current_database`,
    VERSION()   AS `mysql_version`;

-- 2. List the tables created so far.
--    Expect an empty result at the end of Goal 01: the schema is designed
--    in a later goal. A non-empty result here means a later goal's
--    migrations have already been applied.
SELECT
    `TABLE_NAME`   AS `table_name`,
    `TABLE_ROWS`   AS `estimated_rows`,
    `ENGINE`       AS `engine`
FROM `information_schema`.`TABLES`
WHERE `TABLE_SCHEMA` = 'bd_quickmart'
ORDER BY `TABLE_NAME`;

-- 3. Confirm the charset is correct for Bangla text.
SELECT
    `DEFAULT_CHARACTER_SET_NAME` AS `charset`,
    `DEFAULT_COLLATION_NAME`      AS `collation`
FROM `information_schema`.`SCHEMATA`
WHERE `SCHEMA_NAME` = 'bd_quickmart';
