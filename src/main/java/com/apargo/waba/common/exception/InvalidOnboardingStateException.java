package com.apargo.waba.common.exception;

/**
 * Thrown when a requested transition on an
 * {@link com.apargo.waba.domain.entity.OnboardingTask} is not valid given
 * its current {@code status} — e.g. retrying a task that is not
 * {@code FAILED}, or cancelling one that already {@code COMPLETED}.
 * <p>
 * Mapped to {@code 409 Conflict} by {@code GlobalExceptionHandler}.
 */
public class InvalidOnboardingStateException extends RuntimeException {

    public InvalidOnboardingStateException(String message) {
        super(message);
    }
}