package com.apargo.waba.common.exception;

/**
 * Thrown when creating a resource would violate a natural-key uniqueness
 * rule — e.g. registering a {@code WabaPhoneNumber} whose Meta Phone
 * Number ID is already taken.
 * <p>
 * Mapped to {@code 409 Conflict} by {@code GlobalExceptionHandler}.
 * Distinct from {@link IdempotencyKeyConflictException} (also 409), which
 * is about replaying a client-supplied key rather than colliding on the
 * resource's own identity.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}