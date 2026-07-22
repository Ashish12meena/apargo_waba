-- =====================================================================
-- WABA (WhatsApp Business Account) Service - MySQL Schema
-- Database: apargo_wa_messaging
-- Generated from JPA entities in com.apargo.waba.domain
--
-- Table creation order respects foreign key dependencies:
--   1. meta_oauth_tokens        (no dependencies)
--   2. waba_accounts            (depends on meta_oauth_tokens)
--   3. waba_phone_numbers       (depends on waba_accounts)
--   4. waba_daily_message_usage (depends on waba_accounts)
--   5. project_waba_assignments (depends on waba_accounts)
--   6. onboarding_tasks         (no FK dependency - project/org ids are external)
-- =====================================================================

CREATE DATABASE IF NOT EXISTS apargo_wa_messaging
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE apargo_wa_messaging;

SET FOREIGN_KEY_CHECKS = 0;

-- =====================================================================
-- 1. meta_oauth_tokens
-- Stores credentials used to authenticate requests to the Meta Graph API.
-- =====================================================================
DROP TABLE IF EXISTS meta_oauth_tokens;

CREATE TABLE meta_oauth_tokens (
    id                      BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id         BIGINT NOT NULL,
    access_token            LONGTEXT NOT NULL,
    expires_at              DATETIME(6) NULL,
    meta_user_id            VARCHAR(100) NULL,
    system_user_id          VARCHAR(100) NULL,
    business_manager_id     VARCHAR(100) NULL,
    granted_scopes          VARCHAR(500) NULL,
    granted_at              DATETIME(6) NULL,
    token_type              ENUM('USER_TOKEN','SYSTEM_USER') NOT NULL DEFAULT 'USER_TOKEN',
    created_at              DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at              DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at              DATETIME(6) NULL,

    CONSTRAINT uq_meta_oauth_tokens_org UNIQUE (organization_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;


-- =====================================================================
-- 2. waba_accounts
-- Represents a WhatsApp Business Account (WABA) inside Meta.
-- with creadit line boolean for pinacle  (for  update)
-- =====================================================================
DROP TABLE IF EXISTS waba_accounts;

CREATE TABLE waba_accounts (
    id                              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id                 BIGINT NOT NULL,
    meta_oauth_token_id             BIGINT UNSIGNED NOT NULL,
    waba_id                         VARCHAR(100) NOT NULL,
    business_manager_id             VARCHAR(100) NULL,
    status                          ENUM('ACTIVE','SUSPENDED','DISCONNECTED') NOT NULL,
    account_review_status           ENUM('UNVERIFIED','PENDING','APPROVED','REJECTED','DISABLED','PERMANENTLY_DISABLED') NULL,
    business_verification_status    ENUM('NOT_VERIFIED','PENDING','PENDING_NEED_MORE_INFO','VERIFIED','REJECTED') NULL,
    message_template_namespace      VARCHAR(255) NULL,
    timezone_id                     VARCHAR(255) NULL,
    currency                        VARCHAR(255) NULL,
    created_at                      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at                      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at                      DATETIME(6) NULL,

    CONSTRAINT uq_waba_accounts_org_waba UNIQUE (organization_id, waba_id),
    CONSTRAINT uq_waba_accounts_waba_id UNIQUE (waba_id),

    CONSTRAINT fk_waba_accounts_meta_oauth_token
        FOREIGN KEY (meta_oauth_token_id) REFERENCES meta_oauth_tokens (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;


-- =====================================================================
-- 3. waba_phone_numbers
-- Represents a WhatsApp phone number registered under a WABA.
-- =====================================================================
DROP TABLE IF EXISTS waba_phone_numbers;

CREATE TABLE waba_phone_numbers (
    id                              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    waba_account_id                 BIGINT UNSIGNED NOT NULL,
    phone_number_id                 VARCHAR(100) NOT NULL,
    display_phone_number            VARCHAR(255) NULL,
    status                          ENUM('ACTIVE','PENDING','REGISTRATION_FAILED','DISABLED','BLOCKED') NOT NULL DEFAULT 'ACTIVE',
    verified_name                   VARCHAR(255) NULL,
    quality_rating                  ENUM('GREEN','YELLOW','RED','UNKNOWN') NULL,
    messaging_limit_tier            ENUM('LIMIT_250','LIMIT_2K','LIMIT_10K','LIMIT_100K','LIMIT_UNLIMITED') NULL,
    messaging_throughput_tier       ENUM('STANDARD','HIGH') NULL,
    name_status                     ENUM('APPROVED','AVAILABLE_WITHOUT_REVIEW','REJECTED','PENDING','PENDING_DELETION','DELETED','DISABLED') NULL,
    health_status                   ENUM('GREEN','YELLOW','RED','PAUSED','BLOCKED','UNKNOWN') NULL,
    is_official_business_account    BOOLEAN NOT NULL DEFAULT FALSE,
    code_verification_status        ENUM('NOT_VERIFIED','PENDING','VERIFIED','EXPIRED','FAILED') NULL,
    created_at                      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at                      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at                      DATETIME(6) NULL,

    CONSTRAINT uq_waba_phone_number_id UNIQUE (phone_number_id),

    CONSTRAINT fk_waba_phone_numbers_waba_account
        FOREIGN KEY (waba_account_id) REFERENCES waba_accounts (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;


-- =====================================================================
-- 4. waba_daily_message_usage
-- Tracks daily message volume per WABA to enforce Meta's messaging limits.
-- =====================================================================
DROP TABLE IF EXISTS waba_daily_message_usage;

CREATE TABLE waba_daily_message_usage (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    waba_account_id     BIGINT UNSIGNED NOT NULL,
    usage_date          DATE NOT NULL,
    messages_sent       INT NOT NULL DEFAULT 0,
    created_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at          DATETIME(6) NULL,

    CONSTRAINT uq_waba_daily_usage UNIQUE (waba_account_id, usage_date),

    CONSTRAINT fk_waba_daily_usage_waba_account
        FOREIGN KEY (waba_account_id) REFERENCES waba_accounts (id),

    INDEX idx_waba_usage_date (usage_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;


-- =====================================================================
-- 5. project_waba_assignments
-- Maps a Project to a WhatsApp Business Account (WABA). Many-to-many
-- relationship between projects (external service) and WABAs.
-- =====================================================================
DROP TABLE IF EXISTS project_waba_assignments;

CREATE TABLE project_waba_assignments (
    id                      BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    project_id              BIGINT NOT NULL,
    waba_account_id         BIGINT UNSIGNED NOT NULL,
    is_default              BOOLEAN NOT NULL DEFAULT FALSE,
    custom_daily_limit      INT NULL,
    created_at              DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at              DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at              DATETIME(6) NULL,

    CONSTRAINT uq_project_waba_assignment UNIQUE (project_id, waba_account_id),

    CONSTRAINT fk_project_waba_assignments_waba_account
        FOREIGN KEY (waba_account_id) REFERENCES waba_accounts (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;


-- =====================================================================
-- 6. onboarding_tasks
-- Tracks the state of a Meta Embedded Signup onboarding workflow.
-- No FK constraints: organization_id / project_id / result_waba_account_id
-- reference other tables/services but are not enforced with FKs
-- (result_waba_account_id may point at waba_accounts, kept loose since
-- the task can exist/fail before the WABA row is created).
-- =====================================================================
DROP TABLE IF EXISTS onboarding_tasks;

CREATE TABLE onboarding_tasks (
    id                              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id                 BIGINT NOT NULL,
    project_id                      BIGINT NULL,
    oauth_code                      VARCHAR(500) NOT NULL,
    status                          ENUM('FAILED','PROCESSING','COMPLETED','CANCELLED','PENDING') NOT NULL DEFAULT 'PENDING',
    current_step                    ENUM(
                                        'TOKEN_EXCHANGE',
                                        'TOKEN_EXTENSION',
                                        'SCOPE_VERIFICATION',
                                        'BUSINESS_MANAGER_RESOLUTION',
                                        'WABA_RESOLUTION',
                                        'PHONE_NUMBER_RESOLUTION',
                                        'CREDENTIAL_PERSISTENCE',
                                        'WEBHOOK_SUBSCRIPTION',
                                        'PHONE_SYNC',
                                        'PHONE_REGISTRATION',
                                        'SMB_SYNC',
                                        'PHASE2_PROVISIONING'
                                     ) NULL,
    retry_count                     INT NOT NULL DEFAULT 0,
    idempotency_key                 VARCHAR(200) NOT NULL,
    encrypted_access_token          LONGTEXT NULL,
    token_expires_in                BIGINT NULL,
    resolved_waba_id                VARCHAR(255) NULL,
    resolved_business_manager_id    VARCHAR(255) NULL,
    resolved_phone_number_id        VARCHAR(255) NULL,
    result_waba_account_id          BIGINT NULL,
    completed_steps                 LONGTEXT NULL,
    result_summary                  LONGTEXT NULL,
    error_message                   LONGTEXT NULL,
    started_at                      DATETIME(6) NULL,
    finished_at                     DATETIME(6) NULL,
    created_at                      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at                      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at                      DATETIME(6) NULL,

    CONSTRAINT uq_onboarding_idempotency UNIQUE (idempotency_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;



-- Credit line WabaAccounts 

-- CREATE TABLE credit_line_waba_accounts (
--     id BIGINT NOT NULL AUTO_INCREMENT,
--     user_id BIGINT UNSIGNED NOT NULL,
--     firstname VARCHAR(100) NULL,
--     lastname VARCHAR(100) NULL,
--     billingname VARCHAR(150) NULL,
--     email VARCHAR(150) NULL,
--     mobileno VARCHAR(20) NULL,
--     waba_id VARCHAR(50) NULL, 
--     api_key VARCHAR(255) NULL,
--     template_namespace VARCHAR(150) NULL,
--     account_type TINYINT NULL,
--     billing_type TINYINT NULL,
--     billingon TINYINT NULL,
--     minimum_balance_limit DECIMAL(10,2) DEFAULT 0.00,
--     credit_limit_assign TINYINT DEFAULT 0,
--     gst_no VARCHAR(50) NULL,
--     credit_limit DECIMAL(10,2) NULL,
--     created_at TIMESTAMP NULL,
--     updated_at TIMESTAMP NULL,
--     username VARCHAR(100) NULL,
--     password VARCHAR(255) NULL,
--     PRIMARY KEY (id),
--     INDEX (user_id)
-- );