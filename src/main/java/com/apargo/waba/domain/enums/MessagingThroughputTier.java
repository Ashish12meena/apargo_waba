package com.apargo.waba.domain.enums;

/**
 * Meta-defined phone number throughput tiers for the Cloud API.
 *
 * <h3>Why an enum instead of a raw Integer?</h3>
 * Throughput is not a user-supplied number — it is one of a small, fixed set
 * of values Meta assigns to a phone number. Modeling it the same way as
 * {@link MessagingLimitTier} keeps both "fixed Meta-defined numeric ceiling"
 * concepts consistent, and makes an invalid value (e.g. {@code 500}) a
 * compile-time impossibility instead of a silent bad write to the DB.
 *
 * <p>
 * <b>Important:</b> Meta does not expose an API to read a number's current
 * throughput tier. This must be set manually here when a number is upgraded
 * (e.g., confirmed via Meta support, or inferred from observed throttling
 * behavior at a given rate).
 * </p>
 */
public enum MessagingThroughputTier {

    /**
     * Default throughput assigned to every newly registered Cloud API
     * phone number.
     */
    STANDARD(80),

    /**
     * Upgraded throughput tier available once a phone number meets Meta's
     * eligibility requirements (quality rating, messaging tier, volume
     * history, etc.).
     */
    HIGH(1000);

    private final int mps;

    MessagingThroughputTier(int mps) {
        this.mps = mps;
    }

    /**
     * The raw messages-per-second ceiling this tier represents.
     * Bulk dispatch must throttle sends to this value, independent of
     * the daily unique-recipient limit on {@link WabaAccount}.
     */
    public int getMps() {
        return mps;
    }
}