-- =====================================================================
-- waba-service — dummy/test data
-- =====================================================================
-- Derived from the JPA entities (domain/entity/*.java), NOT from any
-- existing .sql file in the project. Insert order follows the real
-- @ManyToOne / @JoinColumn relationships found on those entities —
-- the same relationships Hibernate uses to generate FK constraints
-- under `ddl-auto: update` (per application.yaml, this is MySQL):
--
--   meta_oauth_tokens
--         ^  (waba_accounts.meta_oauth_token_id)
--   waba_accounts
--         ^  (waba_phone_numbers.waba_account_id)
--         ^  (project_waba_assignments.waba_account_id)
--         ^  (waba_daily_message_usage.waba_account_id)
--   waba_phone_numbers / project_waba_assignments / waba_daily_message_usage
--
-- organization_id, project_id, bsp_credential_id and
-- onboarding_tasks.result_waba_account_id are plain @Column longs with
-- no @ManyToOne on the entity (Organization/Project/BSP-credential live
-- in other services' schemas per docs/rules.md) — no FK exists for
-- these, so they need no ordering, but result_waba_account_id values
-- below still point at real seeded WABAs for realism.
--
-- Scope, as requested:
--   organization_id = 1, with project_id 767 and 768 inside it.
-- Added on top (clearly separate IDs) for isolation/negative testing:
--   organization_id = 2 (project 900) and organization_id = 3 (no
--   project assignment at all) — so you can confirm org/project-scoped
--   queries never leak another org's rows.
--
-- Idempotent: the DELETE block at the top clears exactly these seeded
-- rows (by id) first, so this file can be re-run safely.
-- =====================================================================

use apargo_wa_waba;

START TRANSACTION;

-- ---------------------------------------------------------------------
-- Clean slate for these specific seeded rows (children first, so this
-- delete block itself never violates an FK either).
-- ---------------------------------------------------------------------
DELETE FROM waba_daily_message_usage WHERE id BETWEEN 1 AND 7;
DELETE FROM project_waba_assignments WHERE id BETWEEN 1 AND 5;
DELETE FROM waba_phone_numbers WHERE id BETWEEN 1 AND 12;
DELETE FROM onboarding_tasks WHERE id BETWEEN 1 AND 7;
DELETE FROM waba_accounts WHERE id BETWEEN 1 AND 7;
DELETE FROM meta_oauth_tokens WHERE id BETWEEN 1 AND 2;

-- =====================================================================
-- 1. meta_oauth_tokens  (no dependencies)
-- =====================================================================
-- One row per organization (uq_meta_oauth_tokens_org) — org 3 is
-- PINNACLE-only in this dataset, so it deliberately gets no token row.
INSERT INTO meta_oauth_tokens
    (id, organization_id, access_token, expires_at, meta_user_id, system_user_id,
     business_manager_id, granted_scopes, granted_at, token_type,
     created_at, updated_at, deleted_at)
VALUES
-- 1. Org 1 — user token, expires in 60 days
(1, 1, 'DUMMY_ENC_TOKEN_ORG1_QWxhZGRpbjpvcGVuIHNlc2FtZQ==',
 DATE_ADD(NOW(), INTERVAL 60 DAY), '10123456789011', NULL, 'BM_1000001',
 'whatsapp_business_management,whatsapp_business_messaging,business_management',
 DATE_SUB(NOW(), INTERVAL 45 DAY), 'USER_TOKEN',
 DATE_SUB(NOW(), INTERVAL 45 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), NULL),
-- 2. Org 2 — permanent system-user token (expires_at NULL)
(2, 2, 'DUMMY_ENC_TOKEN_ORG2_U3lzdGVtVXNlclBlcm1hbmVudA==',
 NULL, NULL, '10123456789022', 'BM_2000001',
 'whatsapp_business_management,whatsapp_business_messaging,business_management',
 DATE_SUB(NOW(), INTERVAL 90 DAY), 'SYSTEM_USER',
 DATE_SUB(NOW(), INTERVAL 90 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY), NULL);

-- =====================================================================
-- 2. waba_accounts  (FK -> meta_oauth_tokens)
-- =====================================================================
INSERT INTO waba_accounts
    (id, organization_id, onboarding_provider, meta_oauth_token_id, bsp_credential_id,
     waba_id, business_manager_id, status, account_review_status,
     business_verification_status, message_template_namespace, timezone_id, currency,
     created_at, updated_at, deleted_at)
VALUES
-- 1. Org 1 — flagship, fully healthy META_DIRECT WABA. Shared by projects 767 & 768.
(1, 1, 'META_DIRECT', 1, NULL, '109876543211001', 'BM_1000001', 'ACTIVE', 'APPROVED',
 'VERIFIED', 'ns_waba_1001', 'Asia/Kolkata', 'INR',
 DATE_SUB(NOW(), INTERVAL 40 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), NULL),
-- 2. Org 1 — still under Meta review. Assigned to project 767 only (non-default).
(2, 1, 'META_DIRECT', 1, NULL, '109876543211002', 'BM_1000001', 'ACTIVE', 'PENDING',
 'PENDING_NEED_MORE_INFO', NULL, 'Asia/Kolkata', 'INR',
 DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), NULL),
-- 3. Org 1 — BSP-managed (Pinnacle), no meta_oauth_token_id, has bsp_credential_id instead.
(3, 1, 'PINNACLE', NULL, 5001, '109876543211003', 'BM_1000001', 'ACTIVE', 'APPROVED',
 'VERIFIED', 'ns_waba_1003', 'Asia/Kolkata', 'INR',
 DATE_SUB(NOW(), INTERVAL 25 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), NULL),
-- 4. Org 1 — SUSPENDED (e.g. billing failure). Unassigned to any project.
(4, 1, 'META_DIRECT', 1, NULL, '109876543211004', 'BM_1000001', 'SUSPENDED', 'APPROVED',
 'VERIFIED', 'ns_waba_1004', 'Asia/Kolkata', 'INR',
 DATE_SUB(NOW(), INTERVAL 60 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), NULL),
-- 5. Org 1 — DISCONNECTED (lost Meta auth). Unassigned to any project.
(5, 1, 'META_DIRECT', 1, NULL, '109876543211005', 'BM_1000001', 'DISCONNECTED', 'DISABLED',
 'REJECTED', NULL, 'Asia/Kolkata', 'INR',
 DATE_SUB(NOW(), INTERVAL 80 DAY), DATE_SUB(NOW(), INTERVAL 20 DAY), NULL),
-- 6. Org 2 — isolation/negative-test WABA, own token, own project (900).
(6, 2, 'META_DIRECT', 2, NULL, '109876543212001', 'BM_2000001', 'ACTIVE', 'APPROVED',
 'VERIFIED', 'ns_waba_2001', 'America/New_York', 'USD',
 DATE_SUB(NOW(), INTERVAL 88 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY), NULL),
-- 7. Org 3 — PINNACLE-only org, brand new / unverified, no project assignment.
(7, 3, 'PINNACLE', NULL, 5002, '109876543213001', 'BM_3000001', 'ACTIVE', 'UNVERIFIED',
 'NOT_VERIFIED', NULL, 'Europe/London', 'GBP',
 DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), NULL);

-- =====================================================================
-- 3. waba_phone_numbers  (FK -> waba_accounts)
-- =====================================================================
INSERT INTO waba_phone_numbers
    (id, waba_account_id, phone_number_id, display_phone_number, status, verified_name,
     quality_rating, messaging_limit_tier, messaging_throughput_tier, name_status,
     health_status, is_official_business_account, code_verification_status,
     created_at, updated_at, deleted_at)
VALUES
-- WABA 1 (healthy flagship) — 4 numbers, spanning the common states
(1, 1, '209876543221001', '+91 90000 01001', 'ACTIVE', 'Apargo Retail Support',
 'GREEN', 'LIMIT_10K', 'HIGH', 'APPROVED', 'GREEN', TRUE, 'VERIFIED',
 DATE_SUB(NOW(), INTERVAL 40 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), NULL),
(2, 1, '209876543221002', '+91 90000 01002', 'ACTIVE', 'Apargo Retail Sales',
 'YELLOW', 'LIMIT_2K', 'STANDARD', 'APPROVED', 'YELLOW', FALSE, 'VERIFIED',
 DATE_SUB(NOW(), INTERVAL 38 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), NULL),
(3, 1, '209876543221003', '+91 90000 01003', 'PENDING', NULL,
 'UNKNOWN', NULL, 'STANDARD', 'PENDING', 'UNKNOWN', FALSE, 'PENDING',
 DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), NULL),
(4, 1, '209876543221004', '+91 90000 01004', 'REGISTRATION_FAILED', NULL,
 'UNKNOWN', NULL, 'STANDARD', 'REJECTED', 'UNKNOWN', FALSE, 'FAILED',
 DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 14 DAY), NULL),
-- WABA 2 (under review) — 1 number, still pending
(5, 2, '209876543221005', '+91 90000 01005', 'PENDING', NULL,
 'UNKNOWN', NULL, 'STANDARD', 'PENDING', 'UNKNOWN', FALSE, 'PENDING',
 DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), NULL),
-- WABA 3 (BSP-managed) — 2 numbers, both healthy
(6, 3, '209876543221006', '+91 90000 01006', 'ACTIVE', 'Apargo Support (BSP)',
 'GREEN', 'LIMIT_250', 'STANDARD', 'APPROVED', 'GREEN', FALSE, 'VERIFIED',
 DATE_SUB(NOW(), INTERVAL 25 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), NULL),
(7, 3, '209876543221007', '+91 90000 01007', 'ACTIVE', 'Apargo Alerts (BSP)',
 'GREEN', 'LIMIT_250', 'STANDARD', 'AVAILABLE_WITHOUT_REVIEW', 'GREEN', FALSE, 'VERIFIED',
 DATE_SUB(NOW(), INTERVAL 25 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), NULL),
-- WABA 4 (suspended) — 2 numbers, one red, one blocked
(8, 4, '209876543221008', '+91 90000 01008', 'ACTIVE', 'Apargo Marketing',
 'RED', 'LIMIT_250', 'STANDARD', 'APPROVED', 'RED', FALSE, 'VERIFIED',
 DATE_SUB(NOW(), INTERVAL 60 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), NULL),
(9, 4, '209876543221009', '+91 90000 01009', 'BLOCKED', 'Apargo Blocked Number',
 'RED', 'LIMIT_250', 'STANDARD', 'APPROVED', 'BLOCKED', FALSE, 'VERIFIED',
 DATE_SUB(NOW(), INTERVAL 58 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), NULL),
-- WABA 5 (disconnected) — 1 number, disabled
(10, 5, '209876543221010', '+91 90000 01010', 'DISABLED', 'Apargo Old Number',
 'UNKNOWN', NULL, 'STANDARD', 'DISABLED', 'BLOCKED', FALSE, 'EXPIRED',
 DATE_SUB(NOW(), INTERVAL 80 DAY), DATE_SUB(NOW(), INTERVAL 20 DAY), NULL),
-- WABA 6 (org 2) — 1 number, official business account
(11, 6, '209876543221011', '+1 555 010 1001', 'ACTIVE', 'Acme Corp Support',
 'GREEN', 'LIMIT_100K', 'HIGH', 'APPROVED', 'GREEN', TRUE, 'VERIFIED',
 DATE_SUB(NOW(), INTERVAL 88 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY), NULL),
-- WABA 7 (org 3) — 1 number, brand new / pending
(12, 7, '209876543221012', '+44 7700 900123', 'PENDING', NULL,
 'UNKNOWN', NULL, 'STANDARD', 'PENDING', 'UNKNOWN', FALSE, 'PENDING',
 DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), NULL);

-- =====================================================================
-- 4. project_waba_assignments  (FK -> waba_accounts)
-- =====================================================================
INSERT INTO project_waba_assignments
    (id, project_id, waba_account_id, is_default, custom_daily_limit,
     created_at, updated_at, deleted_at)
VALUES
-- Project 767 -> WABA 1 (default) + WABA 2 (secondary, capped at 5000/day)
(1, 767, 1, TRUE,  NULL, DATE_SUB(NOW(), INTERVAL 40 DAY), DATE_SUB(NOW(), INTERVAL 40 DAY), NULL),
(2, 767, 2, FALSE, 5000, DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY), NULL),
-- Project 768 -> WABA 1 (default, shared with 767 — tests one-WABA-many-projects)
--             -> WABA 3 (secondary, BSP-managed)
(3, 768, 1, TRUE,  20000, DATE_SUB(NOW(), INTERVAL 40 DAY), DATE_SUB(NOW(), INTERVAL 40 DAY), NULL),
(4, 768, 3, FALSE, NULL,  DATE_SUB(NOW(), INTERVAL 25 DAY), DATE_SUB(NOW(), INTERVAL 25 DAY), NULL),
-- Project 900, org 2 — isolation check: must never show up under org 1 / projects 767 / 768
(5, 900, 6, TRUE,  NULL, DATE_SUB(NOW(), INTERVAL 88 DAY), DATE_SUB(NOW(), INTERVAL 88 DAY), NULL);

-- =====================================================================
-- 5. waba_daily_message_usage  (FK -> waba_accounts)
-- =====================================================================
INSERT INTO waba_daily_message_usage
    (id, waba_account_id, usage_date, messages_sent, created_at, updated_at, deleted_at)
VALUES
(1, 1, CURDATE(),                             1450, NOW(), NOW(), NULL),
(2, 1, DATE_SUB(CURDATE(), INTERVAL 1 DAY),   1875, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), NULL),
(3, 1, DATE_SUB(CURDATE(), INTERVAL 2 DAY),   1620, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), NULL),
(4, 3, CURDATE(),                              180, NOW(), NOW(), NULL),
(5, 3, DATE_SUB(CURDATE(), INTERVAL 1 DAY),    210, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), NULL),
(6, 4, CURDATE(),                                0, NOW(), NOW(), NULL),
(7, 6, CURDATE(),                             3200, NOW(), NOW(), NULL);

-- =====================================================================
-- 6. onboarding_tasks  (no real FK — see header note — placed last so
--    result_waba_account_id can reference real seeded WABAs)
-- =====================================================================
INSERT INTO onboarding_tasks
    (id, organization_id, project_id, oauth_code, status, current_step, retry_count,
     idempotency_key, encrypted_access_token, token_expires_in, resolved_waba_id,
     resolved_business_manager_id, resolved_phone_number_id, result_waba_account_id,
     completed_steps, result_summary, error_message, started_at, finished_at,
     created_at, updated_at, deleted_at)
VALUES
-- 1. Completed — produced WABA 1
(1, 1, 767, 'DUMMY_OAUTH_CODE_USED_001', 'COMPLETED', 'PHASE2_PROVISIONING', 0,
 'onb-org1-767-001', NULL, 5184000, '109876543211001', 'BM_1000001', '209876543221001', 1,
 '["TOKEN_EXCHANGE","TOKEN_EXTENSION","SCOPE_VERIFICATION","BUSINESS_MANAGER_RESOLUTION","WABA_RESOLUTION","PHONE_NUMBER_RESOLUTION","CREDENTIAL_PERSISTENCE","WEBHOOK_SUBSCRIPTION","PHONE_SYNC","PHONE_REGISTRATION","PHASE2_PROVISIONING"]',
 'Onboarding completed successfully for WABA 109876543211001', NULL,
 DATE_SUB(NOW(), INTERVAL 40 DAY), DATE_ADD(DATE_SUB(NOW(), INTERVAL 40 DAY), INTERVAL 6 MINUTE),
 DATE_SUB(NOW(), INTERVAL 40 DAY), DATE_ADD(DATE_SUB(NOW(), INTERVAL 40 DAY), INTERVAL 6 MINUTE), NULL),
-- 2. Completed — produced WABA 2 (still pending Meta review, but our workflow finished)
(2, 1, 767, 'DUMMY_OAUTH_CODE_USED_002', 'COMPLETED', 'PHASE2_PROVISIONING', 0,
 'onb-org1-767-002', NULL, 5184000, '109876543211002', 'BM_1000001', '209876543221005', 2,
 '["TOKEN_EXCHANGE","TOKEN_EXTENSION","SCOPE_VERIFICATION","BUSINESS_MANAGER_RESOLUTION","WABA_RESOLUTION","PHONE_NUMBER_RESOLUTION","CREDENTIAL_PERSISTENCE","WEBHOOK_SUBSCRIPTION","PHONE_SYNC","PHONE_REGISTRATION","PHASE2_PROVISIONING"]',
 'Onboarding completed successfully for WABA 109876543211002', NULL,
 DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_ADD(DATE_SUB(NOW(), INTERVAL 10 DAY), INTERVAL 5 MINUTE),
 DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_ADD(DATE_SUB(NOW(), INTERVAL 10 DAY), INTERVAL 5 MINUTE), NULL),
-- 3. Failed — died during PHONE_NUMBER_RESOLUTION, no local WABA ever created
(3, 1, 768, 'DUMMY_OAUTH_CODE_USED_003_FAILED', 'FAILED', 'PHONE_NUMBER_RESOLUTION', 3,
 'onb-org1-768-003', NULL, NULL, '109876543219999', 'BM_1000001', NULL, NULL,
 '["TOKEN_EXCHANGE","TOKEN_EXTENSION","SCOPE_VERIFICATION","BUSINESS_MANAGER_RESOLUTION","WABA_RESOLUTION"]',
 NULL, 'Meta Graph API returned 400: no phone numbers found for this WABA',
 DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_ADD(DATE_SUB(NOW(), INTERVAL 15 DAY), INTERVAL 4 MINUTE),
 DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_ADD(DATE_SUB(NOW(), INTERVAL 15 DAY), INTERVAL 4 MINUTE), NULL),
-- 4. Still processing right now — no project (not project-initiated)
(4, 1, NULL, 'DUMMY_OAUTH_CODE_INFLIGHT_004', 'PROCESSING', 'WEBHOOK_SUBSCRIPTION', 0,
 'onb-org1-004-inflight', NULL, 5184000, '109876543211003', 'BM_1000001', NULL, NULL,
 '["TOKEN_EXCHANGE","TOKEN_EXTENSION","SCOPE_VERIFICATION","BUSINESS_MANAGER_RESOLUTION","WABA_RESOLUTION","PHONE_NUMBER_RESOLUTION","CREDENTIAL_PERSISTENCE"]',
 NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 2 HOUR), NULL,
 DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 2 HOUR), NULL),
-- 5. Freshly created, not started yet
(5, 1, 767, 'DUMMY_OAUTH_CODE_FRESH_005', 'PENDING', NULL, 0,
 'onb-org1-767-005-fresh', NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL,
 NULL, NULL,
 NOW(), NOW(), NULL),
-- 6. Org 2 — completed, produced WABA 6
(6, 2, 900, 'DUMMY_OAUTH_CODE_USED_006', 'COMPLETED', 'PHASE2_PROVISIONING', 0,
 'onb-org2-900-006', NULL, NULL, '109876543212001', 'BM_2000001', '209876543221011', 6,
 '["TOKEN_EXCHANGE","SCOPE_VERIFICATION","BUSINESS_MANAGER_RESOLUTION","WABA_RESOLUTION","PHONE_NUMBER_RESOLUTION","CREDENTIAL_PERSISTENCE","WEBHOOK_SUBSCRIPTION","PHONE_SYNC","PHONE_REGISTRATION","PHASE2_PROVISIONING"]',
 'Onboarding completed successfully for WABA 109876543212001', NULL,
 DATE_SUB(NOW(), INTERVAL 88 DAY), DATE_ADD(DATE_SUB(NOW(), INTERVAL 88 DAY), INTERVAL 5 MINUTE),
 DATE_SUB(NOW(), INTERVAL 88 DAY), DATE_ADD(DATE_SUB(NOW(), INTERVAL 88 DAY), INTERVAL 5 MINUTE), NULL),
-- 7. Cancelled by the user partway through
(7, 1, 768, 'DUMMY_OAUTH_CODE_CANCELLED_007', 'CANCELLED', 'SCOPE_VERIFICATION', 1,
 'onb-org1-768-007-cancelled', NULL, NULL, NULL, NULL, NULL, NULL,
 '["TOKEN_EXCHANGE"]', NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_ADD(DATE_SUB(NOW(), INTERVAL 7 DAY), INTERVAL 2 MINUTE),
 DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_ADD(DATE_SUB(NOW(), INTERVAL 7 DAY), INTERVAL 2 MINUTE), NULL);

COMMIT;