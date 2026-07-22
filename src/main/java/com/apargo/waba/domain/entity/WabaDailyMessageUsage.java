package com.apargo.waba.domain.entity;



import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Tracks daily message volume per WABA to enforce Meta's messaging limits.
 *
 * <h3>Why is this table necessary?</h3>
 * Meta's Graph API does NOT provide an endpoint to query "How many messages
 * has this WABA sent today?" To enforce limits and prevent our customers from
 * getting blocked by Meta, we must track usage internally.
 *
 * <h3>How it works:</h3>
 * <ol>
 *   <li>Every time our system sends a message (or receives a delivery webhook),
 *       we increment {@code messagesSent} for that WABA for today's date.</li>
 *   <li>Before sending a new message, we query this table and compare
 *       {@code messagesSent} against the WABA's {@code businessMessagingLimit}.</li>
 *   <li>If the limit is reached, we block the send and return an error.</li>
 * </ol>
 *
 * <h3>Important: Shared Limits</h3>
 * Because Meta enforces limits at the Business Portfolio level, if an org has
 * multiple WABAs under the same {@code businessManagerId}, the application
 * layer must SUM the usage across all those WABAs for the current date.
 *
 * <h3>Data Retention:</h3>
 * Old records (e.g., > 90 days) can be archived or deleted via a scheduled
 * cleanup job. They are only needed for limit enforcement, not long-term analytics.
 */
@Entity
@Table(
    name = "waba_daily_message_usage",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_waba_daily_usage",
        columnNames = {"waba_account_id", "usage_date"}
    ),
    indexes = {
        @Index(name = "idx_waba_usage_date", columnList = "usage_date")
    }
)
@SQLRestriction("deleted_at IS NULL")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WabaDailyMessageUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The WABA whose usage is being tracked. */
    @Column(name = "waba_account_id", nullable = false)
    private Long wabaAccountId;

    /**
     * The date this usage record applies to (e.g., 2026-07-17).
     *
     * <p><b>Why LocalDate instead of Instant?</b> Meta's 24-hour rolling window
     * is date-based (aligned to the WABA's timezone). Using {@code LocalDate}
     * makes it easy to query "all usage for today" without timezone math.</p>
     *
     * <p><b>Note:</b> For multi-timezone orgs, the application layer must convert
     * the current time to the WABA's {@code timezoneId} before determining
     * "today's date".</p>
     */
    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate;

    /**
     * Number of messages sent on this date.
     *
     * <p><b>Why Integer and not Long?</b> Meta's maximum limit is 1,000,000
     * (or unlimited). Integer.MAX_VALUE (2.1 billion) is more than sufficient.
     * Using Integer saves storage and makes comparisons faster.</p>
     */
    @Column(name = "messages_sent", nullable = false)
    @Builder.Default
    private Integer messagesSent = 0;

    // ── Timestamps & Soft Delete ───────────────────────────────

    @Column(name = "created_at", updatable = false, insertable = false)
    @CreationTimestamp
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false)
    @UpdateTimestamp
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    // ── Relationships ──────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "waba_account_id", insertable = false, updatable = false)
    private WabaAccount wabaAccount;

    // ── Business Logic ─────────────────────────────────────────

    /**
     * Increments the message count by 1.
     * Called after every successful message send or delivery webhook.
     */
    public void incrementCount() {
        this.messagesSent++;
        this.updatedAt = Instant.now();
    }

    /**
     * Increments the message count by a specific amount.
     * Useful for batch operations or webhook backfills.
     */
    public void incrementCount(int amount) {
        this.messagesSent += amount;
        this.updatedAt = Instant.now();
    }

    /**
     * Checks if the given limit has been reached.
     *
     * @param limit The maximum allowed messages for the day.
     * @return true if {@code messagesSent >= limit}.
     */
    public boolean isLimitReached(int limit) {
        return this.messagesSent >= limit;
    }

    /**
     * Returns the number of messages remaining before hitting the limit.
     * Returns 0 if the limit is already reached.
     */
    public int getRemainingMessages(int limit) {
        return Math.max(0, limit - this.messagesSent);
    }

    // ── Soft Delete ────────────────────────────────────────────

    public void markAsDeleted() { this.deletedAt = Instant.now(); }
    public boolean isDeleted() { return this.deletedAt != null; }
}