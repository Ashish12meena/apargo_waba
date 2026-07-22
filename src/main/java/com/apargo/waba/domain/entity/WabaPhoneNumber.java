package com.apargo.waba.domain.entity;

import com.apargo.waba.domain.enums.CodeVerificationStatus;
import com.apargo.waba.domain.enums.HealthStatus;
import com.apargo.waba.domain.enums.MessagingLimitTier;
import com.apargo.waba.domain.enums.MessagingThroughputTier;
import com.apargo.waba.domain.enums.NameStatus;
import com.apargo.waba.domain.enums.PhoneNumberQualityRating;
import com.apargo.waba.domain.enums.PhoneNumberStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Represents a WhatsApp phone number registered under a
 * WhatsApp Business Account (WABA).
 *
 * <h2>Most Important Entity</h2>
 *
 * Although WABAs are used for administration,
 * almost every operational WhatsApp API call
 * ultimately targets a phone number.
 *
 * Examples:
 *
 * POST /{phone-number-id}/messages
 *
 * POST /{phone-number-id}/register
 *
 * GET /{phone-number-id}
 *
 * Webhooks also identify the receiving number
 * using the Meta Phone Number ID.
 *
 * <h2>Meta IDs vs Real Phone Numbers</h2>
 *
 * Developers often confuse:
 *
 * Display Number
 *
 * +91 9876543210
 *
 * with
 *
 * Phone Number ID
 *
 * 123456789012345
 *
 * The display number is shown to customers.
 *
 * The Phone Number ID is Meta's internal identifier
 * and is used in every Graph API endpoint.
 *
 * Never attempt to send messages using
 * the display phone number.
 *
 * Always use the Meta Phone Number ID.
 *
 * <h2>Relationship</h2>
 *
 * One WABA
 * ├── Phone A
 * ├── Phone B
 * └── Phone C
 *
 * Every phone belongs to exactly one WABA.
 */
@Entity
@Table(name = "waba_phone_numbers", uniqueConstraints = {
        @UniqueConstraint(name = "uq_waba_phone_number_id", columnNames = "phone_number_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLRestriction("deleted_at IS NULL")
public class WabaPhoneNumber {

    /**
     * Internal database identifier.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Parent WABA.
     *
     * Every phone number belongs to exactly one
     * WhatsApp Business Account.
     */
    @Column(name = "waba_account_id", nullable = false)
    private Long wabaAccountId;

    /**
     * Meta Phone Number ID.
     *
     * Example:
     *
     * 108745612345678
     *
     * This is NOT the customer's phone number.
     *
     * It is Meta's globally unique identifier
     * for this WhatsApp phone.
     *
     * Every messaging endpoint uses this ID.
     *
     * Example:
     *
     * POST /{phone-number-id}/messages
     *
     * Webhooks also include this identifier
     * allowing us to determine which
     * phone received the event.
     *
     * This value never changes.
     */
    @Column(name = "phone_number_id", nullable = false, unique = true, length = 100)
    private String whatsappPhoneNumberId;

    /**
     * Human-readable phone number.
     *
     * Example:
     *
     * +91 9876543210
     *
     * This is displayed inside dashboards,
     * logs and administration screens.
     *
     * Never use this value for Graph API calls.
     */
    @Column(name = "display_phone_number")
    private String displayPhoneNumber;

    /**
     * Internal operational status.
     *
     * This describes whether our application
     * currently allows the phone to be used.
     *
     * This status is separate from Meta's
     * verification state.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PhoneNumberStatus status = PhoneNumberStatus.ACTIVE;

    /**
     * Verified business name approved by Meta.
     *
     * Customers see this name inside WhatsApp.
     *
     * Example:
     *
     * Apargo Technologies
     *
     * This value originates from Meta
     * and should never be manually edited.
     */
    @Column(name = "verified_name")
    private String verifiedName;

    /**
     * Current messaging quality rating.
     *
     * Meta continuously monitors user feedback.
     *
     * Poor quality ratings can lead to:
     *
     * • Lower messaging limits
     * • Reduced throughput
     * • Number restrictions
     *
     * Typical values:
     *
     * GREEN
     * YELLOW
     * RED
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "quality_rating")
    private PhoneNumberQualityRating qualityRating;

    /**
     * Current messaging limit tier.
     *
     * Unlike older assumptions,
     * messaging limits are effectively
     * tracked per phone number.
     *
     * Typical tiers:
     *
     * 250
     * 1K
     * 10K
     * 100K
     * Unlimited
     *
     * This value is synchronized from Meta
     * and displayed to administrators.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "messaging_limit_tier")
    private MessagingLimitTier messagingLimitTier;

    /**
     * Maximum message throughput supported
     * by Meta for this phone.
     *
     * Higher throughput allows larger
     * message volumes to be processed
     * simultaneously.
     *
     * This information is useful for
     * traffic distribution.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "messaging_throughput_tier")
    private MessagingThroughputTier throughputTier;

    /**
     * Current display name approval status.
     *
     * Meta reviews every display name
     * before customers can see it.
     *
     * Example:
     *
     * PENDING
     *
     * APPROVED
     *
     * REJECTED
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "name_status")
    private NameStatus nameStatus;

    /**
     * Overall health reported by Meta.
     *
     * Health combines multiple operational
     * signals beyond messaging quality.
     *
     * Used primarily for dashboards
     * and monitoring.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "health_status")
    private HealthStatus healthStatus;

    /**
     * Indicates whether Meta has granted
     * Official Business Account (OBA) status.
     *
     * Official Business Accounts receive
     * the verified blue checkmark.
     *
     * Most businesses will have FALSE.
     */
    @Column(name = "is_official_business_account")
    @Builder.Default
    private Boolean officialBusinessAccount = false;

    /**
     * Current phone verification state.
     *
     * Before a phone number becomes usable,
     * Meta requires ownership verification
     * using SMS or Voice OTP.
     *
     * This field tracks that process.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "code_verification_status")
    private CodeVerificationStatus verificationStatus;

    /**
     * Entity creation timestamp.
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    /**
     * Last update timestamp.
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * Soft delete timestamp.
     */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    // ----------------------------------------------------
    // Relationships
    // ----------------------------------------------------

    /**
     * Parent WhatsApp Business Account.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "waba_account_id", insertable = false, updatable = false)
    private WabaAccount wabaAccount;

    // ----------------------------------------------------
    // Business Methods
    // ----------------------------------------------------

    /**
     * Returns true if this phone
     * can currently send messages.
     *
     * A phone must satisfy both:
     *
     * • Active inside Apargo
     * • Successfully verified by Meta
     */
    public boolean canSendMessages() {

        return status == PhoneNumberStatus.ACTIVE
                && verificationStatus == CodeVerificationStatus.VERIFIED;

    }

    /**
     * Returns true when the number
     * has poor messaging quality.
     */
    public boolean hasPoorQuality() {

        return qualityRating == PhoneNumberQualityRating.RED;

    }

    /**
     * Returns whether Meta has granted
     * Official Business Account status.
     */
    public boolean isOfficialBusinessAccount() {

        return Boolean.TRUE.equals(officialBusinessAccount);

    }

    /**
     * Soft delete.
     */
    public void markDeleted() {

        deletedAt = Instant.now();

    }

    /**
     * Returns whether this record
     * has been soft deleted.
     */
    public boolean isDeleted() {

        return deletedAt != null;

    }

}