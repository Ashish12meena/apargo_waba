package com.apargo.waba.common.exception;

/**
 * Thrown when onboarding would overwrite an organization's existing
 * {@link com.apargo.waba.domain.entity.MetaOAuthToken} with a token
 * scoped to a <b>different</b> Business Manager than the one already
 * stored.
 *
 * <p>Product decision: an organization is restricted to exactly one Meta
 * Business Manager (and therefore one token) on this platform — see
 * {@code uq_meta_oauth_tokens_org} on {@code MetaOAuthToken}. Although
 * Meta itself permits a business to hold multiple Business Managers, this
 * platform does not support connecting more than one to the same
 * organization.
 *
 * <p>Rather than silently overwriting the stored token (which would
 * break API access for every WABA already onboarded under the original
 * Business Manager), {@code CREDENTIAL_PERSISTENCE} throws this and lets
 * the onboarding task fail clearly, with a message that explains what
 * happened, instead of corrupting existing data.
 *
 * <p>Mapped to {@code 409 Conflict} by {@code GlobalExceptionHandler} for
 * any endpoint that surfaces it directly; within the async onboarding
 * workflow, it results in {@code OnboardingTask.status = FAILED} with
 * this exception's message as {@code errorMessage}.
 */
public class OrganizationTokenConflictException extends RuntimeException {

    public OrganizationTokenConflictException(String message) {
        super(message);
    }
}