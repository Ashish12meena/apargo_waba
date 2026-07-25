package com.apargo.waba.common.exception;

/**
 * Thrown when a credential was located but cannot be handed out or used —
 * no Meta connection for the organization, an expired user token, or a
 * phone number that is not in a state to send.
 * <p>
 * Mapped to {@code 409 Conflict}: the request was well-formed and the
 * caller is entitled to the resource, but the resource's current state
 * blocks the operation. Deliberately distinct from
 * {@link ResourceNotFoundException} (404) — "your Meta connection has
 * expired, re-authorize" and "no such WABA" call for completely different
 * responses from the calling service, and collapsing them into one status
 * would hide that.
 */
public class CredentialUnavailableException extends RuntimeException {

    public CredentialUnavailableException(String message) {
        super(message);
    }
}