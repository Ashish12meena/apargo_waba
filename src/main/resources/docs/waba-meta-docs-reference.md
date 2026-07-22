# Meta WABA Documentation Reference

Curated links to Meta's official documentation, organized by which part of the
`waba-service` flow each one maps to. Use this as a jump-off point instead of
searching Meta's docs from scratch.

---

## 1. Onboarding / Embedded Signup
Maps to: `OnboardingTask`, `OnboardingStep`

- **Embedded Signup overview**
  https://developers.facebook.com/documentation/business-messaging/whatsapp/embedded-signup/overview
  Core flow this service is built around. Note: WABAs originally created via
  the developer app (not Embedded Signup) can't be re-onboarded through this
  flow — relevant edge case for `WABA_RESOLUTION`.

- **Embedded Signup implementation**
  https://developers.facebook.com/documentation/business-messaging/whatsapp/embedded-signup/implementation
  Step-by-step detail for `TOKEN_EXCHANGE` → `TOKEN_EXTENSION` →
  `WABA_RESOLUTION` → `PHONE_RESOLUTION`.

- **Hosted Embedded Signup**
  https://developers.facebook.com/documentation/business-messaging/whatsapp/embedded-signup/hosted-es/
  Meta-hosted variant of the same flow — compare against your implementation.

- **Onboard WhatsApp Business app users**
  https://developers.facebook.com/documentation/business-messaging/whatsapp/embedded-signup/onboarding-business-app-users
  Relevant to `SMB_SYNC` — coexistence + history sync for numbers already on
  the WhatsApp Business app.

- **Cloud API Get Started**
  https://developers.facebook.com/documentation/business-messaging/whatsapp/get-started
  Broader entry point: access tokens, webhook setup, quickstart. Good map of
  everything else below.

---

## 2. Token & System User Management
Maps to: `MetaOAuthToken`, `TokenType`, `PHASE2_PROVISIONING`

- **System Users overview**
  https://developers.facebook.com/docs/business-management-apis/system-users/overview
  Admin system user vs. scoped system user distinction. Confirms your
  `TokenType.SYSTEM_USER` should always represent the scoped kind. Also has
  the system-user-per-Business-Manager limits tied to your app's access tier.

- **Business Manager API (index)**
  https://developers.facebook.com/docs/business-management-apis/business-manager-api
  General landing page — mostly ads/Pages, but links out to the pages above.

---

## 3. WABA / Phone Number Lifecycle & Messaging Limits
Maps to: `WabaAccount`, `WabaPhoneNumber`, `MessagingLimitTier`,  
`WabaDailyMessageUsage`

- **Messaging Limits**
  https://developers.facebook.com/documentation/business-messaging/whatsapp/messaging-limits
  Important: `messaging_limit_tier` field is **deprecated** — use
  `whatsapp_business_manager_messaging_limit` instead. Confirms limits are
  portfolio-level (matches your `WabaDailyMessageUsage` design). Also
  describes a Tier 0 (250) for unverified portfolios, separate from the
  post-verification starting tier.

- **About the WhatsApp Business Platform**
  https://developers.facebook.com/documentation/business-messaging/whatsapp/about-the-platform
  Background on template quality scores (ties to `QualityRating`) and test
  WABAs/test phone numbers with relaxed limits — useful for dev/staging.

---

## 4. Webhooks
Maps to: `WEBHOOK_SUBSCRIBE` step (not yet implemented)

- **Webhooks overview**
  https://developers.facebook.com/documentation/business-messaging/whatsapp/webhooks/overview
  Subscription model — you subscribe to individual fields, not just once
  globally, via App Dashboard or API.

- **Set up webhooks (Cloud API guide)**
  https://developers.facebook.com/docs/whatsapp/cloud-api/guides/set-up-webhooks/
  The concrete "how" — endpoint verification (`hub.challenge`), payload
  structure, retry behavior (up to 7 days on non-200 responses).

- **Create a webhook endpoint**
  https://developers.facebook.com/documentation/business-messaging/whatsapp/webhooks/create-webhook-endpoint/
  Same topic via a newer doc path — cross-check both since Meta has
  overlapping doc trees here.

---

## Open items to verify against these docs

- [ ] Confirm `graph-api-version: v23.0` / `v18.0` split in `application.yaml`
      is still within Meta's supported window (the webhook field rename from
      `max_daily_conversation_per_phone` to
      `max_daily_conversations_per_business` only fully applies from v24.0;
      old field sunsets February 2026).
- [ ] Switch any polling/reads of `messaging_limit_tier` to
      `whatsapp_business_manager_messaging_limit`.
- [ ] Decide whether Phase 2 system user provisioning happens inside the
      *client's* Business Manager or your platform's — determines whether the
      system-user-per-BM cap is a per-client or platform-wide constraint.
