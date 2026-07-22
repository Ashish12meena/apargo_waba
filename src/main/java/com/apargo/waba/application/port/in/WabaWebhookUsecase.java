package com.apargo.waba.application.port.in;

import com.apargo.waba.api.request.WhatsAppWebhookPayload;

/**
 * Contract for handling inbound Meta WhatsApp Cloud API webhook traffic.
 *
 * <p><b>No implementation is provided here on purpose</b> - this interface
 * only exists so {@code WabaWebhookController} has something to compile
 * against and depend on. Provide a {@code @Service} implementation
 * separately.
 *
 * <p>Implementation notes for whoever builds {@code WabaWebhookServiceImpl}:
 * <ul>
 *     <li>{@link #isValidSignature} must use a constant-time comparison
 *         (e.g. {@code MessageDigest.isEqual}) - never {@code String.equals}
 *         - to avoid timing attacks, and must compute HMAC-SHA256 over the
 *         exact raw request bytes using {@code meta.app-secret} as the key,
 *         comparing against the {@code sha256=<hex>} value Meta sends in
 *         {@code X-Hub-Signature-256}.</li>
 *     <li>{@link #processWebhookEvent} should return to the controller as
 *         fast as possible. Meta expects a 200 within a few seconds and
 *         retries with backoff for up to 7 days on non-200 responses -
 *         heavy work (DB writes, downstream calls, fan-out to other
 *         services) should be handed off asynchronously (e.g. published to
 *         Kafka) rather than done inline on the request thread.</li>
 *     <li>Processing should switch on {@code WebhookChange.getField()} and
 *         deserialize {@code WebhookChange.getValue()} into a specific
 *         value DTO per field type, per the notes in
 *         {@code WebhookChange}'s Javadoc.</li>
 *     <li>Consider idempotency - Meta's retry behavior means the same event
 *         can arrive more than once.</li>
 * </ul>
 */
public interface WabaWebhookUsecase {

    /**
     * Verifies the {@code hub.verify_token} sent by Meta during the
     * one-time GET handshake against the token configured for this app.
     *
     * @param mode        the {@code hub.mode} query param, expected to be
     *                     {@code "subscribe"}
     * @param verifyToken the {@code hub.verify_token} query param
     * @return true if the handshake should be accepted
     */
    boolean isValidVerificationRequest(String mode, String verifyToken);

    /**
     * Verifies the HMAC-SHA256 signature Meta attaches to every POST
     * notification, proving the request actually originated from Meta and
     * was not tampered with in transit.
     *
     * @param rawPayload      the exact, unmodified request body bytes
     * @param signatureHeader the raw {@code X-Hub-Signature-256} header
     *                        value (format: {@code sha256=<hex digest>}),
     *                        may be {@code null} if the header was absent
     * @return true only if the signature is present and valid
     */
    boolean isValidSignature(byte[] rawPayload, String signatureHeader);

    /**
     * Hands a verified, parsed webhook payload off for processing.
     * <p>
     * Must not block the caller for long - see class-level Javadoc.
     *
     * @param payload the parsed notification body
     */
    void processWebhookEvent(WhatsAppWebhookPayload payload);

}
