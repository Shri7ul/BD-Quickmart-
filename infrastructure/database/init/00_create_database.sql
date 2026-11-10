-- =====================================================================
-- BD QuickMart - Database bootstrap script
-- =====================================================================
-- Target: MySQL 8 (XAMPP) on the developer's own machine.
--
-- HOW TO RUN (developer task - see docs/development/local-setup.md):
--   1. Start MySQL from the XAMPP Control Panel.
--   2. Open http://localhost/phpmyadmin
--   3. Click the "SQL" tab, paste this script, and press "Go".
--
-- This script ONLY creates the empty database with the correct character
-- set and collation. It deliberately creates NO tables: the schema is
-- designed and migrated in a later, separately approved goal.
--
-- IMPORTANT: This script is safe to run more than once. It does not drop,
-- truncate, or modify any other database on the server.
-- =====================================================================

-- Create the database if it does not already exist.
CREATE DATABASE IF NOT EXISTS `bd_quickmart`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

-- Select it for the remainder of the session.
USE `bd_quickmart`;

-- Confirm the database was created with the intended charset.
SELECT
    `SCHEMA_NAME`  AS `database_name`,
    `DEFAULT_CHARACTER_SET_NAME` AS `charset`,
    `DEFAULT_COLLATION_NAME`      AS `collation`
FROM `information_schema`.`SCHEMATA`
WHERE `SCHEMA_NAME` = 'bd_quickmart';

-- =====================================================================
-- Notes for the developer
-- =====================================================================
-- * utf8mb4 is required: Bangla script, emoji and other supplementary
--   characters are stored in product names, addresses and reviews.
-- * utf8mb4_unicode_ci is the collation used by MySQL 8 by default and
--   performs case- and accent-insensitive comparisons, which suits the
--   Bangla/English mixed search required by Goal 04.
-- * The application connects as the MySQL user configured via the
--   DB_USERNAME / DB_PASSWORD environment variables. See .env.example.
-- =====================================================================
