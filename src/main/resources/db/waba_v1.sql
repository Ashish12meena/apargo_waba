-- =====================================================================
-- WABA (WhatsApp Business Account) Service - MySQL Schema v2
-- Database: apargo_wa_messaging
-- Full documentation: see waba_sql.md
-- =====================================================================

CREATE DATABASE IF NOT EXISTS apargo_wa_messaging
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE apargo_wa_messaging;

SET FOREIGN_KEY_CHECKS = 0;

-- =====================================================================
-- 1. meta_oauth_tokens
-- =====================================================================
DROP TABLE IF EXISTS meta_oauth_tokens;

CREATE TABLE meta_oauth_tokens (
    id                      BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id         BIGINT NOT NULL,                                  -- external org id
    access_token            LONGTEXT NOT NULL,                                -- Meta access token (encrypted at rest)
    expires_at              DATETIME(6) NULL,                                 -- NULL for long-lived system-user tokens
    meta_user_id            VARCHAR(100) NULL,                                -- Meta user ID that granted the token
    system_user_id          VARCHAR(100) NULL,                                -- Meta Business system user ID
    business_manager_id     VARCHAR(100) NULL,                                -- Meta Business Manager ID
    granted_scopes          VARCHAR(500) NULL,                                -- comma-separated OAuth scopes granted (e.g. whatsapp_business_management)
    granted_at               DATETIME(6) NULL,                                 -- when the OAuth grant happened
    token_type               ENUM('USER_TOKEN','SYSTEM_USER') NOT NULL DEFAULT 'USER_TOKEN', -- USER_TOKEN or SYSTEM_USER
    created_at                DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at                DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at                 DATETIME(6) NULL

    -- multiple meta_oauth_tokens rows per org are currently allowed
    -- (org can connect more than one Meta Business Manager separately)
    -- to restrict an org to ONE Meta auth account, apply this:
    -- , CONSTRAINT uq_meta_oauth_tokens_org UNIQUE (organization_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;


-- =====================================================================
-- 2. pinacle_credentials
-- =====================================================================
DROP TABLE IF EXISTS pinacle_credentials;

CREATE TABLE pinacle_credentials (
    id                      BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id         BIGINT NOT NULL,                                  -- external org id
    partner_id               VARCHAR(100) NOT NULL,                            -- Pinacle-assigned partner/reseller id
    api_key_encrypted        VARCHAR(500) NOT NULL,                            -- Pinacle API key (encrypted at rest)
    password_encrypted       VARCHAR(500) NULL,                                -- Pinacle dashboard password, if used (encrypted at rest)
    username                 VARCHAR(150) NULL,                                -- Pinacle dashboard username
    webhook_verify_token     VARCHAR(255) NULL,                                -- verifies inbound Pinacle webhooks
    meta_credit_line_id      VARCHAR(100) NULL,                                -- Meta Credit-Line-ID Pinacle holds as a Solution Partner
    status                    ENUM('ACTIVE','REVOKED','EXPIRED') NOT NULL DEFAULT 'ACTIVE',
    granted_at                DATETIME(6) NULL,
    created_at                DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at                DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at                 DATETIME(6) NULL

    -- multiple pinacle_credentials rows per org are currently allowed
    -- (org can hold more than one Pinacle partner account separately)
    -- to restrict an org to ONE Pinacle credential set, apply this:
    -- , CONSTRAINT uq_pinacle_credentials_org UNIQUE (organization_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;


-- =====================================================================
-- 3. waba_accounts
-- =====================================================================
DROP TABLE IF EXISTS waba_accounts;

CREATE TABLE waba_accounts (
    id                                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id                   BIGINT NOT NULL,                        -- external org id
    onboarding_provider               ENUM('META_DIRECT','PINACLE') NOT NULL DEFAULT 'META_DIRECT',
    meta_oauth_token_id               BIGINT UNSIGNED NULL,                   -- set when onboarding_provider = META_DIRECT
    pinacle_credential_id             BIGINT UNSIGNED NULL,                   -- set when onboarding_provider = PINACLE
    waba_id                           VARCHAR(100) NOT NULL,                  -- Meta-issued WABA ID
    business_manager_id               VARCHAR(100) NULL,
    status                            ENUM('ACTIVE','SUSPENDED','DISCONNECTED') NOT NULL,
    account_review_status             ENUM('UNVERIFIED','PENDING','APPROVED','REJECTED','DISABLED','PERMANENTLY_DISABLED') NULL,
    business_verification_status      ENUM('NOT_VERIFIED','PENDING','PENDING_NEED_MORE_INFO','VERIFIED','REJECTED') NULL,
    message_template_namespace        VARCHAR(255) NULL,                      -- required prefix for template messages
    timezone_id                       VARCHAR(255) NULL,                      -- immutable once a credit line is attached
    currency                          VARCHAR(255) NULL,                      -- immutable once a credit line is attached
    created_at                        DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at                        DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at                         DATETIME(6) NULL,

    CONSTRAINT uq_waba_accounts_org_waba UNIQUE (organization_id, waba_id),
    CONSTRAINT uq_waba_accounts_waba_id UNIQUE (waba_id),

    CONSTRAINT fk_waba_accounts_meta_oauth_token
        FOREIGN KEY (meta_oauth_token_id) REFERENCES meta_oauth_tokens (id),

    CONSTRAINT fk_waba_accounts_pinacle_credential
        FOREIGN KEY (pinacle_credential_id) REFERENCES pinacle_credentials (id),

    CONSTRAINT chk_waba_accounts_provider_credential                          -- exactly one credential FK must match onboarding_provider
        CHECK (
            (onboarding_provider = 'META_DIRECT'
                AND meta_oauth_token_id IS NOT NULL
                AND pinacle_credential_id IS NULL)
            OR
            (onboarding_provider = 'PINACLE'
                AND pinacle_credential_id IS NOT NULL
                AND meta_oauth_token_id IS NULL)
        )
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;


-- =====================================================================
-- 4. pinacle_credit_line_attachments
-- =====================================================================
DROP TABLE IF EXISTS pinacle_credit_line_attachments;

CREATE TABLE pinacle_credit_line_attachments (
    id                      BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    waba_account_id         BIGINT UNSIGNED NOT NULL,                         -- the client WABA this credit line was attached to
    credit_line_id           VARCHAR(100) NOT NULL,                            -- Meta Credit-Line-ID used for this attach call
    allocation_config_id     VARCHAR(100) NULL,                                -- returned by whatsapp_credit_sharing_and_attach
    waba_currency             VARCHAR(10) NOT NULL,                             -- must match waba_accounts.currency
    status                    ENUM('PENDING','ATTACHED','FAILED','REVOKED') NOT NULL DEFAULT 'PENDING',
    shared_at                 DATETIME(6) NULL,
    revoked_at                DATETIME(6) NULL,
    error_message             LONGTEXT NULL,
    created_at                DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at                DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT uq_pinacle_credit_line_waba UNIQUE (waba_account_id),          -- one attachment record per WABA

    CONSTRAINT fk_pinacle_credit_line_waba_account
        FOREIGN KEY (waba_account_id) REFERENCES waba_accounts (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;


-- =====================================================================
-- 5. pinacle_billing_config
-- =====================================================================
DROP TABLE IF EXISTS pinacle_billing_config;

CREATE TABLE pinacle_billing_config (
    id                          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    pinacle_credential_id       BIGINT UNSIGNED NOT NULL,

    billing_type                 ENUM('PREPAID','POSTPAID') NOT NULL DEFAULT 'PREPAID',
    billing_enabled               BOOLEAN NOT NULL DEFAULT FALSE,

    minimum_balance_limit        DECIMAL(12,2) NOT NULL DEFAULT 0.00,          -- floor balance before messaging blocks / alert fires
    credit_limit_assigned        BOOLEAN NOT NULL DEFAULT FALSE,
    credit_limit                 DECIMAL(12,2) NULL,                           -- set only when credit_limit_assigned = TRUE
    current_balance              DECIMAL(12,2) NOT NULL DEFAULT 0.00,          -- history lives in pinacle_credit_ledger

    billing_contact_name         VARCHAR(150) NULL,
    billing_email                 VARCHAR(150) NULL,
    billing_mobile                VARCHAR(20) NULL,
    gst_no                       VARCHAR(50) NULL,                            -- GSTIN for invoicing (India)

    created_at                   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at                   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at                    DATETIME(6) NULL,

    CONSTRAINT uq_pinacle_billing_config_credential UNIQUE (pinacle_credential_id),

    CONSTRAINT fk_pinacle_billing_config_credential
        FOREIGN KEY (pinacle_credential_id) REFERENCES pinacle_credentials (id),

    CONSTRAINT chk_pinacle_billing_credit_limit
        CHECK (
            (credit_limit_assigned = FALSE AND credit_limit IS NULL)
            OR
            (credit_limit_assigned = TRUE AND credit_limit IS NOT NULL)
        )
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;


-- =====================================================================
-- 6. pinacle_credit_ledger
-- =====================================================================
DROP TABLE IF EXISTS pinacle_credit_ledger;

CREATE TABLE pinacle_credit_ledger (
    id                          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    pinacle_credential_id       BIGINT UNSIGNED NOT NULL,
    entry_type                   ENUM('RECHARGE','MESSAGE_DEBIT','ADJUSTMENT','REFUND') NOT NULL,
    amount                       DECIMAL(12,2) NOT NULL,
    balance_after                DECIMAL(12,2) NOT NULL,                       -- running balance snapshot after this entry
    reference                    VARCHAR(255) NULL,                            -- payment gateway txn id, message id, etc.
    remarks                      VARCHAR(500) NULL,
    created_at                   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_pinacle_credit_ledger_credential
        FOREIGN KEY (pinacle_credential_id) REFERENCES pinacle_credentials (id),

    INDEX idx_pinacle_credit_ledger_credential_date (pinacle_credential_id, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;


-- =====================================================================
-- 7. waba_phone_numbers
-- =====================================================================
DROP TABLE IF EXISTS waba_phone_numbers;

CREATE TABLE waba_phone_numbers (
    id                              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    waba_account_id                 BIGINT UNSIGNED NOT NULL,
    phone_number_id                 VARCHAR(100) NOT NULL,                    -- Meta-issued phone number ID
    display_phone_number             VARCHAR(255) NULL,
    status                           ENUM('ACTIVE','PENDING','REGISTRATION_FAILED','DISABLED','BLOCKED') NOT NULL DEFAULT 'ACTIVE',
    verified_name                    VARCHAR(255) NULL,
    quality_rating                   ENUM('GREEN','YELLOW','RED','UNKNOWN') NULL,
    messaging_limit_tier             ENUM('LIMIT_250','LIMIT_2K','LIMIT_10K','LIMIT_100K','LIMIT_UNLIMITED') NULL,
    messaging_throughput_tier        ENUM('STANDARD','HIGH') NULL,
    name_status                      ENUM('APPROVED','AVAILABLE_WITHOUT_REVIEW','REJECTED','PENDING','PENDING_DELETION','DELETED','DISABLED') NULL,
    health_status                    ENUM('GREEN','YELLOW','RED','PAUSED','BLOCKED','UNKNOWN') NULL,
    is_official_business_account     BOOLEAN NOT NULL DEFAULT FALSE,           -- Meta's green-tick OBA badge
    code_verification_status         ENUM('NOT_VERIFIED','PENDING','VERIFIED','EXPIRED','FAILED') NULL,
    created_at                       DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at                       DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at                        DATETIME(6) NULL,

    CONSTRAINT uq_waba_phone_number_id UNIQUE (phone_number_id),

    CONSTRAINT fk_waba_phone_numbers_waba_account
        FOREIGN KEY (waba_account_id) REFERENCES waba_accounts (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;


-- =====================================================================
-- 8. waba_daily_message_usage
-- =====================================================================
DROP TABLE IF EXISTS waba_daily_message_usage;

CREATE TABLE waba_daily_message_usage (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    waba_account_id     BIGINT UNSIGNED NOT NULL,
    usage_date           DATE NOT NULL,
    messages_sent         INT NOT NULL DEFAULT 0,                               -- volume only, not cost
    created_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at            DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at             DATETIME(6) NULL,

    CONSTRAINT uq_waba_daily_usage UNIQUE (waba_account_id, usage_date),

    CONSTRAINT fk_waba_daily_usage_waba_account
        FOREIGN KEY (waba_account_id) REFERENCES waba_accounts (id),

    INDEX idx_waba_usage_date (usage_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;


-- =====================================================================
-- 9. project_waba_assignments
-- =====================================================================
DROP TABLE IF EXISTS project_waba_assignments;

CREATE TABLE project_waba_assignments (
    id                      BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    project_id               BIGINT NOT NULL,
    waba_account_id          BIGINT UNSIGNED NOT NULL,
    is_default                BOOLEAN NOT NULL DEFAULT FALSE,
    custom_daily_limit        INT NULL,                                         -- overrides WABA's daily limit for this project
    created_at                DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at                DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at                 DATETIME(6) NULL,

    CONSTRAINT uq_project_waba_assignment UNIQUE (project_id, waba_account_id),

    CONSTRAINT fk_project_waba_assignments_waba_account
        FOREIGN KEY (waba_account_id) REFERENCES waba_accounts (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;


-- =====================================================================
-- 10. onboarding_tasks
-- =====================================================================
DROP TABLE IF EXISTS onboarding_tasks;

CREATE TABLE onboarding_tasks (
    id                              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id                 BIGINT NOT NULL,
    project_id                       BIGINT NULL,
    provider                         ENUM('META_DIRECT','PINACLE') NOT NULL DEFAULT 'META_DIRECT',
    oauth_code                       VARCHAR(500) NOT NULL,                    -- code returned by the embedded signup popup
    status                           ENUM('FAILED','PROCESSING','COMPLETED','CANCELLED','PENDING') NOT NULL DEFAULT 'PENDING',
    current_step                     ENUM(
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
                                        'PHASE2_PROVISIONING',
                                        'PINACLE_API_KEY_VALIDATION',
                                        'PINACLE_PARTNER_RESOLUTION',
                                        'PINACLE_CREDIT_LINE_ATTACH'
                                     ) NULL,
    retry_count                     INT NOT NULL DEFAULT 0,
    idempotency_key                  VARCHAR(200) NOT NULL,                    -- prevents duplicate onboarding runs
    encrypted_access_token           LONGTEXT NULL,
    token_expires_in                 BIGINT NULL,
    resolved_waba_id                 VARCHAR(255) NULL,
    resolved_business_manager_id     VARCHAR(255) NULL,
    resolved_phone_number_id         VARCHAR(255) NULL,
    result_waba_account_id           BIGINT NULL,                              -- loose reference, no FK
    completed_steps                  LONGTEXT NULL,                            -- JSON log of completed steps
    result_summary                   LONGTEXT NULL,
    error_message                    LONGTEXT NULL,
    started_at                       DATETIME(6) NULL,
    finished_at                      DATETIME(6) NULL,
    created_at                       DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at                       DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at                        DATETIME(6) NULL,

    CONSTRAINT uq_onboarding_idempotency UNIQUE (idempotency_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;