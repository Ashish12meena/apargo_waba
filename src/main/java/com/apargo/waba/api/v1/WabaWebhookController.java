package com.apargo.waba.api.v1;

import com.apargo.waba.api.request.WhatsAppWebhookPayload;
import com.apargo.waba.application.port.in.WabaWebhookUsecase;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * Entry point for Meta's WhatsApp Cloud API webhook.
 *
 * <p>
 * Meta talks to exactly two operations on this URL:
 *
 * <ol>
 *     <li><b>GET</b> - one-time verification handshake performed when the
 *         webhook URL is registered/changed in the App Dashboard (or via
 *         the Graph API). See
 *         <a href="https://developers.facebook.com/docs/whatsapp/cloud-api/guides/set-up-webhooks/">
 *         Set up webhooks</a>.</li>
 *     <li><b>POST</b> - actual event notifications (messages, statuses,
 *         template updates, quality/health changes, account alerts, etc.),
 *         sent for the lifetime of the subscription. Note that registering
 *         this URL alone is not enough - each individual WABA must also be
 *         separately subscribed via {@code POST /{waba-id}/subscribed_apps}
 *         (see {@code OnboardingWorkflowExecutor#executeWebhookSubscription}),
 *         or its events will never reach here even though the URL itself is
 *         verified and correct.</li>
 * </ol>
 *
 * <h2>Design notes</h2>
 * <ul>
 *     <li>This controller intentionally contains <b>no business logic</b>.
 *         Its only jobs are: authenticate the caller is really Meta,
 *         deserialize the payload, and delegate to
 *         {@link WabaWebhookUsecase}. All interpretation of event data
 *         belongs in the service layer ({@code WabaWebhookServiceImpl}).</li>
 *     <li>Nothing here is hardcoded - the verify token and app secret used
 *         by {@link WabaWebhookUsecase}'s implementation come from
 *         {@code meta.webhook-verify-token} / {@code meta.app-secret}
 *         (bound via {@code MetaApiProperties}), never a literal in code.</li>
 *     <li>The POST body is read as raw {@code byte[]} - not bound directly
 *         to {@link WhatsAppWebhookPayload} by Spring - because Meta's
 *         {@code X-Hub-Signature-256} signature is computed over the exact
 *         raw bytes on the wire. Deserializing first and re-serializing
 *         later to check the signature would not reliably reproduce the
 *         same bytes (whitespace/key-order differences), which would break
 *         verification. Deserialization happens explicitly, after the
 *         signature check passes.</li>
 *     <li>Meta expects a fast {@code 200 OK}. Do not do slow work here or
 *         in the synchronous part of the service call - see
 *         {@code WabaWebhookServiceImpl#processWebhookEvent}, which runs
 *         off the request thread via {@code @Async}.</li>
 *     <li>Malformed-body and missing-signature/token cases are handled
 *         inline (not via {@code GlobalExceptionHandler}) because the
 *         correct HTTP response here is a deliberate protocol decision
 *         driven by Meta's own retry semantics, not a generic error
 *         mapping - see the parse-failure comment below.</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/webhooks/whatsapp")
@RequiredArgsConstructor
@Tag(name = "WhatsApp Webhook", description = "Meta WhatsApp Cloud API webhook endpoint")
public class WabaWebhookController {

    private final WabaWebhookUsecase webhookUsecase;
    private final ObjectMapper objectMapper;

    /**
     * One-time webhook verification handshake.
     * <p>
     * Meta calls this with three query params when the webhook URL is
     * registered. If {@code hub.mode == "subscribe"} and
     * {@code hub.verify_token} matches the token configured for this app
     * ({@code meta.webhook-verify-token}), Meta requires the raw
     * {@code hub.challenge} value to be echoed back as the response body
     * with a {@code 200} status and {@code text/plain} content type.
     * Anything else must return a non-200 status so Meta rejects the
     * subscription attempt.
     */
    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(
            summary = "Meta webhook verification handshake",
            description = "Called once by Meta when the webhook URL is configured. "
                    + "Echoes back hub.challenge if hub.verify_token is valid."
    )
    @ApiResponse(responseCode = "200", description = "Verification succeeded — hub.challenge echoed back")
    @ApiResponse(responseCode = "403", description = "hub.mode or hub.verify_token did not match — subscription rejected")
    public ResponseEntity<String> verifyWebhookSubscription(
            @Parameter(description = "Should always be 'subscribe'")
            @RequestParam("hub.mode") String mode,

            @Parameter(description = "Secret token configured in the App Dashboard, "
                    + "must match meta.webhook-verify-token")
            @RequestParam("hub.verify_token") String verifyToken,

            @Parameter(description = "Opaque challenge string that must be echoed back verbatim")
            @RequestParam("hub.challenge") String challenge) {

        log.info("Webhook verification handshake received: mode={}", mode);

        // verifyToken is deliberately never logged, even at DEBUG — it is a
        // shared secret and logging it (even a "masked" partial form) adds
        // an unnecessary leak surface for a value that never needs to
        // appear in logs to be debugged (mismatch vs match is enough).
        boolean accepted = webhookUsecase.isValidVerificationRequest(mode, verifyToken);

        if (!accepted) {
            log.warn("Webhook verification rejected: mode={}", mode);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        log.info("Webhook verification succeeded: mode={}", mode);
        return ResponseEntity.ok(challenge);
    }

    /**
     * Receives an event notification batch from Meta.
     * <p>
     * Every call is expected to carry an {@code X-Hub-Signature-256} header.
     * Requests with a missing or invalid signature are rejected before the
     * body is even parsed.
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Receive a WhatsApp webhook event notification",
            description = "Called by Meta for every subscribed event (messages, statuses, "
                    + "template updates, quality/health changes, account alerts, etc.)."
    )
    @ApiResponse(responseCode = "200", description = "Accepted — event handed off for async processing "
            + "(also returned for a malformed body, deliberately, to avoid Meta's retry storm)")
    @ApiResponse(responseCode = "401", description = "X-Hub-Signature-256 missing or invalid")
    public ResponseEntity<Void> receiveWebhookEvent(
            @RequestBody byte[] rawPayload,

            @Parameter(description = "HMAC-SHA256 signature of the raw body, format sha256=<hex>")
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signatureHeader) {

        log.info("Webhook event notification received: {} bytes, signaturePresent={}",
                rawPayload.length, signatureHeader != null);

        boolean signatureValid = webhookUsecase.isValidSignature(rawPayload, signatureHeader);
        if (!signatureValid) {
            log.warn("Webhook event rejected: invalid or missing X-Hub-Signature-256");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        final WhatsAppWebhookPayload payload;
        try {
            payload = objectMapper.readValue(rawPayload, WhatsAppWebhookPayload.class);
        } catch (IOException e) {
            // Deliberately 200, not 400: a malformed payload is not something
            // Meta can fix by retrying, and Meta will retry non-200 responses
            // for up to 7 days. Log it and move on instead of causing a retry
            // storm. This is a protocol decision specific to this endpoint —
            // intentionally handled inline rather than via
            // GlobalExceptionHandler's generic 400 mapping.
            log.error("Failed to parse WhatsApp webhook payload after signature passed — "
                    + "returning 200 to prevent Meta retry storm", e);
            return ResponseEntity.ok().build();
        }

        log.info("Webhook payload verified and parsed: entryCount={}",
                payload.getEntry() != null ? payload.getEntry().size() : 0);

        webhookUsecase.processWebhookEvent(payload);

        return ResponseEntity.ok().build();
    }
}