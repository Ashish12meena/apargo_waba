package com.apargo.waba.domain.enums;



import lombok.Getter;

/**
 * Meta's official messaging limits for WhatsApp Business Portfolios.
 *
 * <p>These are the ONLY valid values Meta returns via the Graph API field
 * {@code whatsapp_business_manager_messaging_limit}. Meta does NOT issue
 * arbitrary limits (e.g., 11,000) — the system operates as a rigid state machine.</p>
 *
 * <h3>Reference:</h3>
 * <a href="https://developers.facebook.com/documentation/business-messaging/whatsapp/messaging-limits">
 *     Meta Messaging Limits Documentation
 * </a>
 *
 * <h3>Important Facts:</h3>
 * <ul>
 *   <li>The old {@code messaging_limit_tier} field is DEPRECATED by Meta.</li>
 *   <li>Limits are calculated at the <b>Business Portfolio</b> level and shared
 *       by ALL WABAs and phone numbers within that portfolio.</li>
 *   <li>All NEW portfolios start at {@link #LIMIT_250}.</li>
 *   <li>Portfolios graduate to higher tiers via Meta's automatic scaling
 *       or by completing the "scaling path" (business verification + 2,000
 *       delivered template messages in 30 days).</li>
 * </ul>
 */
@Getter
public enum MessagingLimitTier {

    /** Starting limit for ALL newly created Business Portfolios. */
    LIMIT_250(250),

    /**
     * Achieved via the "scaling path":
     *   - Business verification completed, OR
     *   - 2,000 delivered template messages within 30 days.
     */
    LIMIT_2K(2_000),

    /** Achieved via automatic scaling (quality + volume). */
    LIMIT_10K(10_000),

    /** Achieved via automatic scaling. */
    LIMIT_100K(100_000),

    /** Final tier for top-quality businesses. */
    LIMIT_UNLIMITED(Integer.MAX_VALUE);

    /** The absolute maximum number of messages allowed in a 24-hour rolling window. */
    private final int limit;

    MessagingLimitTier(int limit) {
        this.limit = limit;
    }

    /** True if this is the starting limit for new portfolios. */
    public boolean isStartingLimit() {
        return this == LIMIT_250;
    }

    /** True if this is the maximum (unlimited) tier. */
    public boolean isUnlimited() {
        return this == LIMIT_UNLIMITED;
    }

    /**
     * Returns the next tier in Meta's graduation path.
     * Returns {@link #LIMIT_UNLIMITED} if already at the highest tier.
     */
    public MessagingLimitTier getNextTier() {
        return switch (this) {
            case LIMIT_250   -> LIMIT_2K;
            case LIMIT_2K    -> LIMIT_10K;
            case LIMIT_10K   -> LIMIT_100K;
            case LIMIT_100K  -> LIMIT_UNLIMITED;
            case LIMIT_UNLIMITED -> LIMIT_UNLIMITED;
        };
    }
}