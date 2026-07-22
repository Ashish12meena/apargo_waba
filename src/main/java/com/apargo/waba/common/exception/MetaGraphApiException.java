package com.apargo.waba.common.exception;

import lombok.Getter;

/**
 * Thrown when a call to the Meta Graph API fails - either a non-2xx HTTP
 * response, a network/timeout failure, or an unparseable response body.
 *
 * <p>Carries the raw HTTP status (when available) and Meta's own error
 * payload so callers can distinguish retryable failures (5xx, timeouts)
 * from permanent ones (4xx - bad token, insufficient permission, invalid
 * request) without re-parsing the body themselves.
 */
@Getter
public class MetaGraphApiException extends RuntimeException {

    /** HTTP status code returned by Meta, or -1 if the request never got a response (timeout/network error). */
    private final int statusCode;

    /**
     * Raw error body Meta returned, typically shaped like
     * {@code {"error": {"message": "...", "type": "...", "code": ..., "fbtrace_id": "..."}}}.
     * Null when the failure happened before any response was received.
     */
    private final String responseBody;

    public MetaGraphApiException(String message, int statusCode, String responseBody) {
        super(message);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public MetaGraphApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
        this.responseBody = null;
    }

    /** 5xx or no-response failures are generally safe to retry; 4xx generally are not. */
    public boolean isRetryable() {
        return statusCode == -1 || statusCode >= 500;
    }
}