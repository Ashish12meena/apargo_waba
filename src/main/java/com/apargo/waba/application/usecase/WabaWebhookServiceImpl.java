package com.apargo.waba.application.usecase;

import com.apargo.waba.api.request.WebhookAccountUpdateValue;
import com.apargo.waba.api.request.WebhookChange;
import com.apargo.waba.api.request.WebhookEntry;
import com.apargo.waba.api.request.WhatsAppWebhookPayload;
import com.apargo.waba.application.port.in.WabaWebhookUsecase;
import com.apargo.waba.application.port.out.WabaAccountRepositoryPort;
import com.apargo.waba.common.exception.WebhookConfigurationException;
import com.apargo.waba.domain.entity.WabaAccount;
import com.apargo.waba.infrastructure.config.MetaApiProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Implementation of {@link WabaWebhookUsecase}.
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *     <li>Verify the one-time {@code hub.verify_token} handshake.</li>
 *     <li>Verify the HMAC-SHA256 {@code X-Hub-Signature-256} signature on
 *         every event POST, in constant time.</li>
 *     <li>Route a verified, parsed payload to per-event-type handling,
 *         off the request thread (see {@code AsyncConfig}).</li>
 * </ul>
 *
 * <p>All Meta-supplied secrets are sourced exclusively from
 * {@link MetaApiProperties} - never hardcoded, never read via ad-hoc
 * {@code @Value}.
 *
 * <h2>Idempotency — not yet implemented</h2>
 * Meta's retry behavior means the same notification can be delivered more
 * than once. This implementation does not yet deduplicate by event
 * identity; each {@code WebhookChange} handler is a placeholder extension
 * point (logged, not yet persisted). A dedup store (e.g. a short-TTL cache
 * keyed on a stable hash of the change payload) should be added before any
 * handler performs a non-idempotent side effect (e.g. incrementing
 * {@code WabaDailyMessageUsage}).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WabaWebhookServiceImpl implements WabaWebhookUsecase {

    private static final String EXPECTED_MODE = "subscribe";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String SIGNATURE_PREFIX = "sha256=";

    /** Known {@code WebhookChange.field} values — see {@code WebhookChange} Javadoc. */
    private static final String FIELD_MESSAGES = "messages";
    private static final String FIELD_TEMPLATE_STATUS_UPDATE = "message_template_status_update";
    private static final String FIELD_TEMPLATE_QUALITY_UPDATE = "message_template_quality_update";
    private static final String FIELD_ACCOUNT_UPDATE = "account_update";
    private static final String FIELD_ACCOUNT_ALERTS = "account_alerts";
    private static final String FIELD_PHONE_QUALITY_UPDATE = "phone_number_quality_update";
    private static final String FIELD_PHONE_NAME_UPDATE = "phone_number_name_update";
    private static final String FIELD_CAPABILITY_UPDATE = "capability_update";
    private static final String FIELD_SECURITY = "security";

    private final MetaApiProperties metaApiProperties;
    private final WabaAccountRepositoryPort wabaAccountRepositoryPort;
    private final ObjectMapper objectMapper;

    @Override
    public boolean isValidVerificationRequest(String mode, String verifyToken) {

        String configuredToken = metaApiProperties.getWebhookVerifyToken();
        if (!StringUtils.hasText(configuredToken)) {
            // Fail loudly and distinctly from "Meta sent a bad token" — this
            // means meta.webhook-verify-token was never configured for this
            // environment, which is an operational bug, not routine traffic.
            log.error("Webhook verification requested but meta.webhook-verify-token "
                    + "is not configured for this environment");
            throw new WebhookConfigurationException(
                    "meta.webhook-verify-token is not configured");
        }

        boolean modeValid = EXPECTED_MODE.equals(mode);
        boolean tokenValid = constantTimeEquals(configuredToken, verifyToken);

        if (!modeValid || !tokenValid) {
            log.warn("Webhook verification rejected: modeValid={}, tokenValid={}", modeValid, tokenValid);
            return false;
        }

        return true;
    }

    @Override
    public boolean isValidSignature(byte[] rawPayload, String signatureHeader) {

        String appSecret = metaApiProperties.getAppSecret();
        if (!StringUtils.hasText(appSecret)) {
            log.error("Webhook signature check requested but meta.app-secret "
                    + "is not configured for this environment");
            throw new WebhookConfigurationException("meta.app-secret is not configured");
        }

        if (!StringUtils.hasText(signatureHeader) || !signatureHeader.startsWith(SIGNATURE_PREFIX)) {
            log.warn("Missing or malformed X-Hub-Signature-256 header");
            return false;
        }

        String providedHex = signatureHeader.substring(SIGNATURE_PREFIX.length());
        String expectedHex = hmacSha256Hex(rawPayload, appSecret);

        boolean valid = constantTimeEquals(expectedHex, providedHex);
        if (!valid) {
            log.warn("X-Hub-Signature-256 verification failed — payload rejected");
        }
        return valid;
    }

    /**
     * Routes a verified payload to per-field handling.
     * <p>
     * Runs on the {@code webhookTaskExecutor} pool (see {@link
     * com.apargo.waba.infrastructure.config.AsyncConfig}) so the controller
     * can return {@code 200 OK} to Meta immediately after this method is
     * invoked, without waiting for it to complete.
     */
    @Async("webhookTaskExecutor")
    @Override
    public void processWebhookEvent(WhatsAppWebhookPayload payload) {

        if (payload == null || payload.getEntry() == null) {
            log.warn("Received webhook payload with no entries — nothing to process");
            return;
        }

        for (WebhookEntry entry : payload.getEntry()) {
            String wabaId = entry.getId();
            List<WebhookChange> changes = entry.getChanges();

            if (changes == null || changes.isEmpty()) {
                log.warn("Webhook entry for wabaId={} has no changes", wabaId);
                continue;
            }

            for (WebhookChange change : changes) {
                dispatch(wabaId, change);
            }
        }
    }

    /**
     * Dispatches a single change to its handler based on {@link WebhookChange#getField()}.
     * <p>
     * Each branch is currently a logging placeholder. Wire in the real
     * per-field value DTO + downstream handler (e.g. persist to
     * {@code WabaAccount}, {@code WabaPhoneNumber}, publish an event) as
     * that functionality is built — see class-level Javadoc for the
     * idempotency note before adding any non-idempotent side effect here.
     */
    private void dispatch(String wabaId, WebhookChange change) {

        String field = change.getField();
        if (!StringUtils.hasText(field)) {
            log.warn("Webhook change for wabaId={} has no field — skipping", wabaId);
            return;
        }

        switch (field) {
            case FIELD_MESSAGES -> handleMessages(wabaId, change);
            case FIELD_TEMPLATE_STATUS_UPDATE -> handleTemplateStatusUpdate(wabaId, change);
            case FIELD_TEMPLATE_QUALITY_UPDATE -> handleTemplateQualityUpdate(wabaId, change);
            case FIELD_ACCOUNT_UPDATE -> handleAccountUpdate(wabaId, change);
            case FIELD_ACCOUNT_ALERTS -> handleAccountAlerts(wabaId, change);
            case FIELD_PHONE_QUALITY_UPDATE -> handlePhoneQualityUpdate(wabaId, change);
            case FIELD_PHONE_NAME_UPDATE -> handlePhoneNameUpdate(wabaId, change);
            case FIELD_CAPABILITY_UPDATE -> handleCapabilityUpdate(wabaId, change);
            case FIELD_SECURITY -> handleSecurity(wabaId, change);
            default -> log.info("Unrecognized webhook field '{}' for wabaId={} — "
                    + "ignoring (forward-compatible: Meta may add new fields without notice)", field, wabaId);
        }
    }

    // ---------------------------------------------------------------
    // Per-field placeholder handlers — see class-level Javadoc.
    // ---------------------------------------------------------------

    private void handleMessages(String wabaId, WebhookChange change) {
        log.info("Received 'messages' event for wabaId={}", wabaId);
        // TODO: deserialize change.getValue() into a WebhookMessageValue DTO,
        //  route inbound messages / status updates to the messaging module.
    }

    private void handleTemplateStatusUpdate(String wabaId, WebhookChange change) {
        log.info("Received 'message_template_status_update' event for wabaId={}", wabaId);
        // TODO: update local template approval/rejection state.
    }

    private void handleTemplateQualityUpdate(String wabaId, WebhookChange change) {
        log.info("Received 'message_template_quality_update' event for wabaId={}", wabaId);
        // TODO: update local template quality score.
    }

    /**
     * Handles {@code account_update} — the one webhook field directly tied
     * to Embedded Signup: Meta fires a {@code PARTNER_ADDED} event on
     * successful signup completion, and {@code PARTNER_REMOVED} when a
     * business disconnects the app from their WABA.
     *
     * <p><b>This is not required for {@code OnboardingWorkflowExecutor} to
     * complete</b> — that workflow resolves the WABA itself via direct
     * Graph API calls and does not wait on or depend on this webhook. This
     * handler is a parallel, independent signal: it keeps
     * {@link WabaAccount} state fresh for events that happen after (or
     * outside of) our own onboarding flow, and reacts to disconnects.
     *
     * <p><b>Known correlation gap:</b> on {@code PARTNER_ADDED}, if no
     * local {@link WabaAccount} exists yet for this {@code wabaId} (e.g.
     * this webhook arrives before {@code OnboardingWorkflowExecutor}
     * reaches {@code CREDENTIAL_PERSISTENCE}, or the WABA was never
     * onboarded through our flow at all), there's no {@code organizationId}
     * in this payload to create one — we can only log it. A reconciliation
     * job would be needed to backfill/alert on these instead of silently
     * dropping them.
     */
    private void handleAccountUpdate(String wabaId, WebhookChange change) {

        WebhookAccountUpdateValue value;
        try {
            value = objectMapper.treeToValue(change.getValue(), WebhookAccountUpdateValue.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse account_update value for wabaId={} — skipping", wabaId, e);
            return;
        }

        String event = value.getEvent();
        if (!StringUtils.hasText(event)) {
            log.warn("account_update event for wabaId={} has no 'event' field — skipping", wabaId);
            return;
        }

        log.info("Received 'account_update' event={} for wabaId={}", event, wabaId);

        switch (event) {
            case "PARTNER_ADDED" -> handlePartnerAdded(wabaId);
            case "PARTNER_REMOVED" -> handlePartnerRemoved(wabaId);
            default -> log.info("account_update event '{}' for wabaId={} has no handler yet — "
                    + "ignoring (forward-compatible: Meta may add new event types without notice)", event, wabaId);
        }
    }

    private void handlePartnerAdded(String wabaId) {

        Optional<WabaAccount> existing = wabaAccountRepositoryPort.findByWabaId(wabaId);

        if (existing.isPresent()) {
            // Idempotent re-delivery, or the webhook simply arrived after
            // our own onboarding flow already persisted the record —
            // either way, nothing to do.
            log.info("PARTNER_ADDED for wabaId={}: WabaAccount already exists locally (id={}) — no action needed",
                    wabaId, existing.get().getId());
            return;
        }

        log.warn("PARTNER_ADDED received for wabaId={} but no local WabaAccount exists yet. "
                + "This webhook carries no organizationId, so a record cannot be created from here — "
                + "either OnboardingWorkflowExecutor hasn't reached CREDENTIAL_PERSISTENCE yet (race, "
                + "will resolve itself), or this WABA was never onboarded through our own flow "
                + "(reconciliation gap — consider a periodic job that reconciles wabaId values seen "
                + "here against waba_accounts if this happens outside expected onboarding races).",
                wabaId);
    }

    private void handlePartnerRemoved(String wabaId) {

        Optional<WabaAccount> existing = wabaAccountRepositoryPort.findByWabaId(wabaId);

        if (existing.isEmpty()) {
            log.warn("PARTNER_REMOVED for wabaId={} but no local WabaAccount exists — nothing to disconnect", wabaId);
            return;
        }

        WabaAccount wabaAccount = existing.get();
        wabaAccount.disconnect();
        wabaAccountRepositoryPort.save(wabaAccount);

        log.info("PARTNER_REMOVED for wabaId={}: WabaAccount id={} marked DISCONNECTED",
                wabaId, wabaAccount.getId());
    }

    private void handleAccountAlerts(String wabaId, WebhookChange change) {
        log.info("Received 'account_alerts' event for wabaId={}", wabaId);
        // TODO: surface portfolio-level alerts (e.g. to an ops notification channel).
    }

    private void handlePhoneQualityUpdate(String wabaId, WebhookChange change) {
        log.info("Received 'phone_number_quality_update' event for wabaId={}", wabaId);
        // TODO: update WabaPhoneNumber.qualityRating.
    }

    private void handlePhoneNameUpdate(String wabaId, WebhookChange change) {
        log.info("Received 'phone_number_name_update' event for wabaId={}", wabaId);
        // TODO: update WabaPhoneNumber.nameStatus.
    }

    private void handleCapabilityUpdate(String wabaId, WebhookChange change) {
        log.info("Received 'capability_update' event for wabaId={}", wabaId);
        // TODO: update WabaPhoneNumber.messagingLimitTier / throughputTier.
    }

    private void handleSecurity(String wabaId, WebhookChange change) {
        log.info("Received 'security' event for wabaId={}", wabaId);
        // TODO: handle two-step verification code changes.
    }

    // ---------------------------------------------------------------
    // Crypto helpers
    // ---------------------------------------------------------------

    private String hmacSha256Hex(byte[] payload, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] digest = mac.doFinal(payload);
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            // Only thrown for an unsupported algorithm or a malformed key —
            // both are configuration/environment problems, not bad requests.
            log.error("Failed to compute HMAC-SHA256 signature", e);
            throw new WebhookConfigurationException("Unable to compute webhook signature", e);
        }
    }

    /**
     * Constant-time string comparison to avoid leaking information about
     * where a mismatch occurs via response-time timing analysis.
     */
    private boolean constantTimeEquals(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = actual.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expectedBytes, actualBytes)
                && Objects.equals(expected.length(), actual.length());
    }
}