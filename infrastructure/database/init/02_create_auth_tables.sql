-- =====================================================================
-- BD QuickMart - Goal 02: authentication & role management schema
-- =====================================================================
-- Target: MySQL 8 (XAMPP) on the developer's own machine.
--
-- HOW TO RUN (developer task - see docs/development/local-setup.md):
--   1. Run 00_create_database.sql first (creates the empty database).
--   2. Start MySQL from the XAMPP Control Panel.
--   3. Open http://localhost/phpmyadmin, select database `bd_quickmart`.
--   4. Click the "SQL" tab, paste this script, and press "Go".
--   5. Run 03_verify_auth_tables.sql to confirm the result.
--
-- SAFETY: This script contains ONLY `CREATE TABLE IF NOT EXISTS`. It does
-- not DROP, TRUNCATE, ALTER, or DELETE anything, and it is safe to run
-- more than once. It never modifies credentials or other databases.
--
-- These tables are owned by the developer-managed MySQL instance. The
-- backend runs with `spring.jpa.hibernate.ddl-auto=none`, so it will NEVER
-- create or change this schema at runtime.
-- =====================================================================

USE `bd_quickmart`;

-- ---------------------------------------------------------------------
-- users
-- ---------------------------------------------------------------------
-- One row per platform account (customer, rider, or privileged staff).
-- `public_id` is the stable external identifier used as the JWT `sub`.
-- Passwords are stored only as BCrypt hashes.
CREATE TABLE IF NOT EXISTS `users` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `public_id`     CHAR(36)     NOT NULL,
    `email`         VARCHAR(255) NOT NULL,
    `phone`         VARCHAR(32)  NOT NULL,
    `password_hash` VARCHAR(255) NOT NULL,
    `full_name`     VARCHAR(120) NOT NULL,
    `role`          VARCHAR(32)  NOT NULL,
    `status`        VARCHAR(24)  NOT NULL,
    `version`       BIGINT       NOT NULL DEFAULT 0,
    `created_at`    DATETIME(6)  NOT NULL,
    `updated_at`    DATETIME(6)  NOT NULL,
    `deleted_at`    DATETIME(6)  NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_users_public_id` (`public_id`),
    UNIQUE KEY `uk_users_email`     (`email`),
    UNIQUE KEY `uk_users_phone`     (`phone`),
    KEY `idx_users_role_status`     (`role`, `status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- refresh_tokens
-- ---------------------------------------------------------------------
-- Only the SHA-256 hex hash of each raw refresh token is stored.
-- Tokens from one login session share `family_id`; replay of a revoked
-- token revokes the whole family. `user_id` is a real foreign key.
CREATE TABLE IF NOT EXISTS `refresh_tokens` (
    `id`               BIGINT      NOT NULL AUTO_INCREMENT,
    `token_hash`       CHAR(64)    NOT NULL,
    `family_id`        CHAR(36)    NOT NULL,
    `user_id`          BIGINT      NOT NULL,
    `expires_at`       DATETIME(6) NOT NULL,
    `created_at`       DATETIME(6) NOT NULL,
    `revoked_at`       DATETIME(6) NULL,
    `replaced_by_hash` CHAR(64)    NULL,
    `version`          BIGINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_refresh_token_hash` (`token_hash`),
    KEY `idx_refresh_family` (`family_id`),
    KEY `idx_refresh_user`   (`user_id`),
    CONSTRAINT `fk_refresh_user`
        FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- =====================================================================
-- Design notes
-- =====================================================================
-- * `role`    : CUSTOMER | RIDER | ADMIN | SUPER_ADMIN | STORE_MANAGER |
--               INVENTORY_OPERATOR
-- * `status`  : ACTIVE | PENDING | REJECTED | DISABLED
--               New riders start PENDING; only ACTIVE riders reach the
--               rider API. The JWT filter re-reads status on every request.
-- * `version` : optimistic-lock column managed by JPA.
-- * `created_at` / `updated_at`: set by JPA auditing from an injectable
--               Clock; stored in UTC (`hibernate.jdbc.time_zone=UTC`).
-- * No MySQL-only `ON UPDATE CURRENT_TIMESTAMP` clause is used, so the
--   JPA entities stay portable to the H2 test database.
-- =====================================================================
