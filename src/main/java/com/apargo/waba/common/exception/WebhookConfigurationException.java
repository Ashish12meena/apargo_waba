package com.apargo.waba.common.exception;

/**
 * Thrown when a security-related precondition of Meta's webhook contract is
 * violated in a way that indicates a configuration problem rather than a
 * routine "reject this request" case.
 *
 * <p>Routine signature/verify-token mismatches are handled as boolean
 * returns ({@code isValidSignature}, {@code isValidVerificationRequest})
 * and result in a 401/403 - they are expected, benign traffic (scanners,
 * misconfigured retries) and should not throw. This exception is reserved
 * for cases where the service itself is missing something it needs to even
 * perform the check, e.g. {@code meta.app-secret} not being configured -
 * that's an operational misconfiguration, not a bad request, and should
 * fail loudly at startup validation or be logged as an error, not treated
 * as "someone sent a bad signature."
 */
public class WebhookConfigurationException extends RuntimeException {

    public WebhookConfigurationException(String message) {
        super(message);
    }

    public WebhookConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}