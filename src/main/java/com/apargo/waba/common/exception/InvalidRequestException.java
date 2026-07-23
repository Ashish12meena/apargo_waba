package com.apargo.waba.common.exception;

/**
 * Thrown for request-shape problems that aren't expressible as simple
 * per-field bean validation — e.g. "exactly one of these two params must
 * be provided", cross-field constraints, or other structurally invalid
 * requests that {@code @NotNull}/{@code @Positive} etc. can't capture on
 * their own.
 * <p>
 * Mapped to {@code 400 Bad Request} by {@code GlobalExceptionHandler} —
 * distinct from {@link InvalidOnboardingStateException} (409, entity
 * state conflicts) and {@link IdempotencyKeyConflictException} (409,
 * replay conflicts). This is for malformed requests, not state conflicts.
 */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}