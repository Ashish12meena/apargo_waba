package com.apargo.waba.common.exception;

/**
 * Thrown when a requested resource (by id, by key) does not exist, or
 * exists but is soft-deleted (and therefore invisible via
 * {@code @SQLRestriction("deleted_at IS NULL")}).
 * <p>
 * Mapped to {@code 404 Not Found} by {@code GlobalExceptionHandler}.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}