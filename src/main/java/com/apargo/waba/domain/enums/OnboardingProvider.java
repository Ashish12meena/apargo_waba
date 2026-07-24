package com.apargo.waba.domain.enums;


/**
 * Identifies which onboarding channel provisioned a WABA.
 *
 * <ul>
 *     <li>{@link #META_DIRECT} – Embedded Signup directly with Meta.</li>
 *     <li>{@link #PINACLE} – Provisioned through Pinnacle Teleservices BSP.</li>
 * </ul>
 *
 * The value drives a CHECK constraint on {@code waba_accounts}:
 * exactly one credential FK ({@code meta_oauth_token_id} or
 * {@code bsp_credential_id}) must be populated, matching
 * the provider.
 */
public enum OnboardingProvider {

    META_DIRECT,

    PINNACLE

}