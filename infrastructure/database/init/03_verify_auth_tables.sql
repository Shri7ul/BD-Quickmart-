-- =====================================================================
-- BD QuickMart - Goal 02: read-only schema verification
-- =====================================================================
-- Run AFTER 02_create_auth_tables.sql. This script is entirely read-only:
-- it only SELECTs from information_schema. It changes no data and is safe
-- to run repeatedly.
-- =====================================================================

USE `bd_quickmart`;

-- 1. Both tables must exist, using InnoDB + utf8mb4_unicode_ci.
SELECT
    `TABLE_NAME`      AS `table_name`,
    `ENGINE`          AS `engine`,
    `TABLE_COLLATION` AS `collation`
FROM `information_schema`.`TABLES`
WHERE `TABLE_SCHEMA` = 'bd_quickmart'
  AND `TABLE_NAME` IN ('users', 'refresh_tokens')
ORDER BY `TABLE_NAME`;

-- 2. Column layout of `users`.
SELECT
    `COLUMN_NAME`  AS `column_name`,
    `COLUMN_TYPE`  AS `column_type`,
    `IS_NULLABLE`  AS `is_nullable`,
    `COLUMN_KEY`   AS `column_key`
FROM `information_schema`.`COLUMNS`
WHERE `TABLE_SCHEMA` = 'bd_quickmart'
  AND `TABLE_NAME` = 'users'
ORDER BY `ORDINAL_POSITION`;

-- 3. Column layout of `refresh_tokens`.
SELECT
    `COLUMN_NAME`  AS `column_name`,
    `COLUMN_TYPE`  AS `column_type`,
    `IS_NULLABLE`  AS `is_nullable`,
    `COLUMN_KEY`   AS `column_key`
FROM `information_schema`.`COLUMNS`
WHERE `TABLE_SCHEMA` = 'bd_quickmart'
  AND `TABLE_NAME` = 'refresh_tokens'
ORDER BY `ORDINAL_POSITION`;

-- 4. Indexes / uniqueness on both tables.
SELECT
    `TABLE_NAME`  AS `table_name`,
    `INDEX_NAME`  AS `index_name`,
    `NON_UNIQUE`  AS `non_unique`,
    `COLUMN_NAME` AS `column_name`
FROM `information_schema`.`STATISTICS`
WHERE `TABLE_SCHEMA` = 'bd_quickmart'
  AND `TABLE_NAME` IN ('users', 'refresh_tokens')
ORDER BY `TABLE_NAME`, `INDEX_NAME`, `SEQ_IN_INDEX`;

-- 5. Foreign key on refresh_tokens -> users.
SELECT
    `CONSTRAINT_NAME` AS `constraint_name`,
    `TABLE_NAME`      AS `table_name`,
    `COLUMN_NAME`     AS `column_name`,
    `REFERENCED_TABLE_NAME`  AS `referenced_table`,
    `REFERENCED_COLUMN_NAME` AS `referenced_column`
FROM `information_schema`.`KEY_COLUMN_USAGE`
WHERE `TABLE_SCHEMA` = 'bd_quickmart'
  AND `TABLE_NAME` = 'refresh_tokens'
  AND `REFERENCED_TABLE_NAME` IS NOT NULL;
