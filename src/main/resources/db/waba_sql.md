# WABA Service — Schema Documentation

Database: `apargo_wa_messaging`
Companion file: `waba_v2_schema.sql`

This document explains **why each table exists**, **what problem it solves**, and **how tables relate to each other**. Field-level notes are kept in the SQL file itself as inline comments.

---

## Overview

The schema supports onboarding a business onto WhatsApp through two possible providers:

- **META_DIRECT** — business connects straight to Meta via OAuth / Embedded Signup.
- **PINACLE** — business is onboarded through Pinacle, a third-party BSP (Business Solution Provider), using Pinacle's own API-key/partner auth instead of Meta OAuth.

Every WABA (WhatsApp Business Account) in the system belongs to exactly one of these two providers, and the schema is built around that split.

---

## Table Creation Order

Order matters because of foreign keys:

1. `meta_oauth_tokens`
2. `pinacle_credentials`
3. `waba_accounts`
4. `pinacle_credit_line_attachments`
5. `pinacle_billing_config`
6. `pinacle_credit_ledger`
7. `waba_phone_numbers`
8. `waba_daily_message_usage`
9. `project_waba_assignments`
10. `onboarding_tasks`

---

## 1. `meta_oauth_tokens`

**Use case:** Stores the OAuth token Meta issues once an organization connects via Facebook Login for Business. This token is what the backend uses to call the Meta Graph API on the organization's behalf.

One token can manage **many WABAs** (see `waba_accounts.meta_oauth_token_id`, a many-to-one relationship) — a business connects once, and that single system-user token is later used to create/manage multiple WhatsApp numbers/brands.

Currently, an organization is allowed to have **multiple** `meta_oauth_tokens` rows (e.g. if it connects more than one separate Meta Business Manager). If you need to restrict this to one token per org, there's a commented-out `UNIQUE (organization_id)` constraint ready to enable.

---

## 2. `pinacle_credentials`

**Use case:** Stores the credentials needed to authenticate against Pinacle's API — an API key (always), and optionally a username/password if Pinacle's partner dashboard requires it. Pinacle doesn't use OAuth like Meta does, so this is a separate table rather than reusing `meta_oauth_tokens`.

Also stores `meta_credit_line_id` — this is Meta's Extended-Credit-Line-ID that **Pinacle itself holds** as a Meta Solution Partner. Pinacle uses this ID to share credit *out* to each client WABA it onboards (see table 4 below).

Currently, an organization is allowed to have **multiple** `pinacle_credentials` rows (e.g. if it holds more than one separate Pinacle partner account). If you need to restrict this to one credential set per org, there's a commented-out `UNIQUE (organization_id)` constraint ready to enable — same pattern as `meta_oauth_tokens`.

Note: since `pinacle_billing_config` and `pinacle_credit_ledger` both key off `pinacle_credential_id` (not `organization_id` directly), allowing multiple credential rows per org naturally gives each Pinacle account its own independent billing config and ledger — which is usually what you want if an org genuinely has separate Pinacle partner relationships.

---

## 3. `waba_accounts`

**Use case:** The central table — represents an actual WhatsApp Business Account, regardless of which provider onboarded it.

`onboarding_provider` decides which of the two credential foreign keys is populated:
- `META_DIRECT` → `meta_oauth_token_id` is set, `pinacle_credential_id` is NULL
- `PINACLE` → `pinacle_credential_id` is set, `meta_oauth_token_id` is NULL

This is enforced with a `CHECK` constraint (`chk_waba_accounts_provider_credential`) so bad data can't slip in — you can never have a WABA row with both or neither credential set.

**Important business rule (not enforced in SQL, must be handled in app code):** `timezone_id` and `currency` on a WABA become **immutable once a Meta credit line is attached** to it (see table 4). Meta's API rejects changes to these fields after that point.

---

## 4. `pinacle_credit_line_attachments`

**Use case:** Tracks the actual Meta API call that shares Pinacle's credit line with a specific client WABA:

```
POST /{Credit-Line-ID}/whatsapp_credit_sharing_and_attach
     ?waba_id={client_waba_id}&waba_currency={currency}
Authorization: Bearer {System-User-Access-Token}
```

**Why this exists:** when a client is onboarded through Embedded Signup, they *cannot* attach their own payment method to their WABA. The BSP (Pinacle, acting as a Meta "Solution Partner") must share its own line of credit with the client's WABA instead. This table is the audit record of that specific API call and its outcome.

One row per `waba_account_id` — the credit line is attached to one specific client WABA, not to the organization as a whole (an org's Pinacle account may have many client WABAs, each individually attached).

Key fields:
- `allocation_config_id` — returned by Meta after the attach call succeeds; used to verify/poll status later.
- `status` — tracks whether the share is pending, succeeded, failed, or was later revoked.

---

## 5. `pinacle_billing_config`

**Use case:** This is **Pinacle's own reseller billing** toward the organization — completely separate from the Meta credit-line mechanism in table 4.

Here's the distinction that matters:
- Meta bills **Pinacle** (prepaid, as a Solution Partner).
- Pinacle then independently decides how to bill **its own end clients** — prepaid or postpaid, with its own credit limits and minimum balance thresholds. This is a business decision Pinacle makes per client, unrelated to how Meta's API works.

This table holds that reseller-level billing configuration: billing type, credit limit, running balance, minimum balance floor, and invoicing details (GST number, billing contact).

One row per `pinacle_credential_id` (i.e. one billing config per organization).

**Note:** `current_balance` here is just the current snapshot. The full transaction history lives in `pinacle_credit_ledger` (table 6) — don't treat this column as the source of truth for auditing, only as a fast-read cache.

---

## 6. `pinacle_credit_ledger`

**Use case:** Append-only audit trail of every balance change for an organization's Pinacle billing account — recharges, per-message debits, manual adjustments, and refunds.

This exists so `pinacle_billing_config.current_balance` isn't just silently overwritten with no history — every change that affects the balance should insert a row here, with `balance_after` capturing the running total at that point in time (useful for generating statements without recomputing from scratch).

**Known gap (flagged for future work):** `entry_type = 'MESSAGE_DEBIT'` entries currently have no formal link back to `waba_daily_message_usage` (table 8), because that table only tracks message *volume*, not *cost*. If/when accurate per-message cost tracking is needed, a `waba_message_cost_log` table (broken down by conversation category — marketing/utility/service/authentication, since that's how Meta actually prices messages) should be added and referenced from here.

---

## 7. `waba_phone_numbers`

**Use case:** A WABA can have multiple registered phone numbers. This table tracks each number's Meta-issued phone number ID plus operational health signals Meta reports back: quality rating, messaging limit tier, throughput tier, name approval status, and whether it carries the Official Business Account (green tick) badge.

This data matters operationally — a `RED` quality rating or `BLOCKED` health status directly affects whether messages can be sent, so this table is what a monitoring/alerting layer would poll.

---

## 8. `waba_daily_message_usage`

**Use case:** Tracks how many messages were sent per WABA per day, used to enforce Meta's daily messaging limits (tied to `waba_phone_numbers.messaging_limit_tier`).

**Explicitly does NOT track cost** — it's a volume counter only. See the note under table 6 for why this matters for billing reconciliation.

---

## 9. `project_waba_assignments`

**Use case:** A many-to-many mapping between external Projects and WABAs. An organization might run multiple projects (e.g. different product lines or campaigns) that each need their own WhatsApp number, or share one.

`is_default` marks which WABA a project should use by default if multiple are assigned. `custom_daily_limit` lets a specific project be capped below the WABA's actual Meta-allowed tier (useful for cost control or fair-usage between projects sharing one WABA).

---

## 10. `onboarding_tasks`

**Use case:** This is the state machine / saga table that drives the entire onboarding process, for either provider. Embedded Signup onboarding is a multi-step, failure-prone process (token exchange, resolving IDs, persisting credentials, subscribing webhooks, etc.), so this table exists to make that process resumable and auditable rather than a single opaque API call.

`current_step` is an ordered enum tracking exactly where in the flow a given onboarding attempt is. Steps prefixed `PINACLE_` are provider-specific and only apply when `provider = PINACLE`. `PINACLE_CREDIT_LINE_ATTACH` is the step that triggers the API call recorded in table 4.

`idempotency_key` prevents the same onboarding request from being processed twice (important for a flow that can be retried after partial failure).

**Deliberately no foreign keys** on `organization_id`, `project_id`, or `result_waba_account_id` — these reference other services/tables but are kept loose because a task can legitimately exist (and fail) *before* the corresponding `waba_accounts` row is ever created. This is a reasonable tradeoff, but it does mean a periodic reconciliation job is worth having to catch orphaned or stuck tasks.

---

## Cross-Cutting Notes

- **Soft deletes:** every table has a `deleted_at` column. Nothing is hard-deleted by default.
- **Timestamps:** `DATETIME(6)` (microsecond precision) is used throughout for consistency with the JPA entities this schema is generated from.
- **Encryption:** any column suffixed `_encrypted` (API keys, tokens, passwords) is expected to be encrypted at the application layer before being written — the database does not perform encryption itself.
- **Known limitation:** there is currently no table modeling Meta's actual per-conversation-category pricing (marketing/utility/service/authentication rates, which vary by country). If accurate cost-based billing becomes a requirement, this is the next schema gap to close, alongside the `waba_message_cost_log` idea mentioned under table 6.