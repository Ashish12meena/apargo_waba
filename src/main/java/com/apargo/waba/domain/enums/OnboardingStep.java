package com.apargo.waba.domain.enums;

/**
 * Represents the current checkpoint of the Meta Embedded Signup workflow.
 *
 * <h2>Why have this enum?</h2>
 *
 * The onboarding process consists of multiple independent operations.
 * Each operation can fail, be retried, or resume after an application restart.
 *
 * {@link OnboardingStatus} tells us the overall workflow state
 * (PROCESSING, FAILED, COMPLETED, etc.).
 *
 * This enum tells us exactly which step is currently executing.
 *
 * Example:
 *
 * Status = PROCESSING
 * Step   = PHONE_REGISTRATION
 *
 * If the application crashes, processing can resume from the last
 * successful checkpoint instead of starting over.
 */
public enum OnboardingStep {

    /**
     * Exchange the short-lived OAuth authorization code
     * for a Meta access token.
     */
    TOKEN_EXCHANGE,

    /**
     * Exchange the short-lived access token
     * for a long-lived user token.
     *
     * This step may be skipped when using
     * System User permanent tokens.
     */
    TOKEN_EXTENSION,

    /**
     * Verify that the token contains every
     * permission required by the application.
     */
    SCOPE_VERIFICATION,

    /**
     * Resolve the Meta Business Manager
     * (Business Portfolio) that owns the WABA.
     */
    BUSINESS_MANAGER_RESOLUTION,

    /**
     * Resolve the WhatsApp Business Account (WABA)
     * associated with the authenticated business.
     */
    WABA_RESOLUTION,

    /**
     * Discover the WhatsApp Phone Number IDs
     * belonging to the WABA.
     */
    PHONE_NUMBER_RESOLUTION,

    /**
     * Persist the Meta credentials and
     * discovered business assets into the database.
     */
    CREDENTIAL_PERSISTENCE,

    /**
     * Subscribe the application to the WABA's
     * webhook events.
     */
    WEBHOOK_SUBSCRIPTION,

    /**
     * Synchronize all phone numbers
     * from Meta into the local database.
     */
    PHONE_SYNC,

    /**
     * Register the selected phone number
     * with the WhatsApp Cloud API.
     */
    PHONE_REGISTRATION,

    /**
     * Synchronize SMB contacts,
     * conversation history and coexistence data
     * when applicable.
     */
    SMB_SYNC,

    /**
     * Optional second provisioning phase.
     *
     * May include:
     *
     * • System User creation
     * • Permanent token generation
     * • Asset assignment
     * • Future provisioning tasks
     */
    PHASE2_PROVISIONING;

    /**
     * Returns true if this step performs
     * a Meta Graph API request.
     */
    public boolean isExternalApiCall() {

        return switch (this) {

            case TOKEN_EXCHANGE,
                    TOKEN_EXTENSION,
                    SCOPE_VERIFICATION,
                    BUSINESS_MANAGER_RESOLUTION,
                    WABA_RESOLUTION,
                    PHONE_NUMBER_RESOLUTION,
                    WEBHOOK_SUBSCRIPTION,
                    PHONE_SYNC,
                    PHONE_REGISTRATION,
                    SMB_SYNC,
                    PHASE2_PROVISIONING -> true;

            default -> false;
        };

    }

    /**
     * Returns true if the workflow should
     * immediately persist its progress after
     * completing this step.
     *
     * These operations are either expensive
     * or difficult to repeat safely.
     */
    public boolean requiresImmediatePersistence() {

        return switch (this) {

            case TOKEN_EXCHANGE,
                    TOKEN_EXTENSION,
                    CREDENTIAL_PERSISTENCE -> true;

            default -> false;
        };

    }

    /**
     * Returns true if the step can normally
     * be retried after a transient failure.
     */
    public boolean isRetryable() {

        return this != CREDENTIAL_PERSISTENCE;

    }

    /**
     * Returns true if this step belongs
     * to the initial authentication phase.
     */
    public boolean isAuthenticationStep() {

        return switch (this) {

            case TOKEN_EXCHANGE,
                    TOKEN_EXTENSION,
                    SCOPE_VERIFICATION -> true;

            default -> false;
        };

    }

    /**
     * Returns true if this step is discovering
     * Meta resources.
     */
    public boolean isDiscoveryStep() {

        return switch (this) {

            case BUSINESS_MANAGER_RESOLUTION,
                    WABA_RESOLUTION,
                    PHONE_NUMBER_RESOLUTION -> true;

            default -> false;
        };

    }

    /**
     * Returns true if this step is provisioning
     * WhatsApp resources.
     */
    public boolean isProvisioningStep() {

        return switch (this) {

            case WEBHOOK_SUBSCRIPTION,
                    PHONE_SYNC,
                    PHONE_REGISTRATION,
                    SMB_SYNC,
                    PHASE2_PROVISIONING -> true;

            default -> false;
        };

    }

}
