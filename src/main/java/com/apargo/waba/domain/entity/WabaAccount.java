package com.apargo.waba.domain.entity;

import com.apargo.waba.domain.enums.AccountReviewStatus;
import com.apargo.waba.domain.enums.BusinessVerificationStatus;
import com.apargo.waba.domain.enums.WabaStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a WhatsApp Business Account (WABA) inside Meta.
 *
 * <h2>What is a WABA?</h2>
 *
 * A WhatsApp Business Account (WABA) is the primary container used by
 * Meta to manage WhatsApp Business assets.
 *
 * A WABA owns:
 *
 * <ul>
 *     <li>Phone Numbers</li>
 *     <li>Message Templates</li>
 *     <li>Messaging Settings</li>
 *     <li>Business Profile Configuration</li>
 * </ul>
 *
 * Every phone number belongs to exactly one WABA.
 *
 * <pre>
 * Business Manager
 *        │
 *        ▼
 * WhatsApp Business Account
 *        │
 *        ├── Phone Number A
 *        ├── Phone Number B
 *        └── Phone Number C
 * </pre>
 *
 * <h2>Ownership inside Apargo</h2>
 *
 * WABAs belong to an Organization.
 *
 * Projects never own WABAs directly.
 *
 * Instead,
 * {@link ProjectWabaAssignment}
 * grants projects permission to use one or more WABAs.
 *
 * This allows multiple projects within the same organization
 * to share the same WhatsApp infrastructure.
 *
 * <h2>Why separate this from MetaOAuthToken?</h2>
 *
 * Authentication credentials change over time.
 *
 * WABAs do not.
 *
 * Keeping authentication in a dedicated table allows
 * token rotation without modifying WABA records.
 */
@Entity
@Table(
        name = "waba_accounts",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_waba_accounts_org_waba",
                        columnNames = {
                                "organization_id",
                                "waba_id"
                        }
                ),
                @UniqueConstraint(
                        name = "uq_waba_accounts_waba_id",
                        columnNames = "waba_id"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLRestriction("deleted_at IS NULL")
public class WabaAccount {

    /**
     * Internal database identifier.
     *
     * Never shared with Meta.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Internal organization that owns this WABA.
     *
     * This references our Organization Service,
     * not Meta.
     *
     * Keeping this value directly on the entity
     * allows efficient tenant filtering without
     * joining through authentication tables.
     */
    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    /**
     * Authentication credential used to manage this WABA.
     *
     * The referenced token supplies the Authorization header
     * for Graph API requests.
     *
     * Example:
     *
     * GET /{waba-id}/phone_numbers
     *
     * Authorization:
     * Bearer &lt;token&gt;
     */
    @Column(name = "meta_oauth_token_id", nullable = false)
    private Long metaOAuthTokenId;

    /**
     * Meta's globally unique WhatsApp Business Account identifier.
     *
     * Example:
     *
     * 123456789012345
     *
     * This value is returned by Embedded Signup
     * and is used in nearly every management API.
     *
     * Examples:
     *
     * GET /{waba-id}
     *
     * GET /{waba-id}/phone_numbers
     *
     * GET /{waba-id}/message_templates
     */
    @Column(name = "waba_id", nullable = false, length = 100)
    private String wabaId;

    /**
     * Meta Business Manager
     * (Business Portfolio)
     * that owns this WABA.
     *
     * A single Business Manager may own
     * multiple WhatsApp Business Accounts.
     *
     * This identifier allows us to:
     *
     * <ul>
     *     <li>Group WABAs belonging to the same business.</li>
     *     <li>Display ownership information.</li>
     *     <li>Support future Business Manager features.</li>
     * </ul>
     *
     * This value should never be confused with the WABA ID.
     */
    @Column(name = "business_manager_id", length = 100)
    private String businessManagerId;

    /**
     * Internal operational state.
     *
     * This status belongs entirely to Apargo.
     *
     * It does NOT represent Meta approval.
     *
     * Example:
     *
     * Meta may report an ACTIVE WABA
     * while Apargo temporarily suspends it
     * because billing failed.
     *
     * Therefore this field intentionally exists
     * independently of Meta's own statuses.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WabaStatus status;

    /**
     * Meta's account review result.
     *
     * During Embedded Signup Meta may require
     * manual review before messaging becomes available.
     *
     * This field mirrors Meta's review state
     * so it can be displayed in the UI.
     *
     * This value should never be modified manually.
     *
     * It must always reflect Meta.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "account_review_status")
    private AccountReviewStatus accountReviewStatus;

    /**
     * Current business verification status
     * reported by Meta.
     *
     * Verification affects the organization's
     * ability to unlock additional WhatsApp features.
     *
     * This information is synchronized from Meta
     * and should be treated as read-only.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "business_verification_status")
    private BusinessVerificationStatus businessVerificationStatus;

    /**
     * Template namespace assigned by Meta.
     *
     * Older versions of the WhatsApp Business API
     * required this namespace during template creation.
     *
     * Although modern Cloud API versions rely less
     * on namespaces, storing it preserves compatibility
     * with existing integrations.
     */
    @Column(name = "message_template_namespace")
    private String messageTemplateNamespace;

    /**
     * Configured timezone of the WABA.
     *
     * Used only for display and reporting.
     *
     * Example:
     *
     * Asia/Kolkata
     *
     * America/New_York
     */
    @Column(name = "timezone_id")
    private String timezoneId;

    /**
     * Billing currency configured in Meta.
     *
     * Example:
     *
     * INR
     *
     * USD
     *
     * EUR
     */
    @Column
    private String currency;

    /**
     * Entity creation timestamp.
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    /**
     * Last modification timestamp.
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * Soft delete timestamp.
     */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    // ---------------------------------------------------------
    // Relationships
    // ---------------------------------------------------------

    /**
     * Authentication credential used for this WABA.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "meta_oauth_token_id",
            insertable = false,
            updatable = false
    )
    private MetaOAuthToken metaOAuthToken;

    /**
     * Phone numbers registered under this WABA.
     *
     * Every outbound message ultimately
     * originates from one of these phone numbers.
     */
    @Builder.Default
    @OneToMany(
            mappedBy = "wabaAccount",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<WabaPhoneNumber> phoneNumbers = new ArrayList<>();

    /**
     * Projects permitted to use this WABA.
     */
    @Builder.Default
    @OneToMany(
            mappedBy = "wabaAccount",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<ProjectWabaAssignment> projectAssignments =
            new ArrayList<>();

    // ---------------------------------------------------------
    // Business Methods
    // ---------------------------------------------------------

    /**
     * Returns true when this WABA is operational.
     */
    public boolean isActive() {

        return status == WabaStatus.ACTIVE;

    }

    /**
     * Returns true if Meta has completed
     * account review successfully.
     */
    public boolean isApprovedByMeta() {

        return accountReviewStatus ==
                AccountReviewStatus.APPROVED;

    }

    /**
     * Activate the WABA inside Apargo.
     */
    public void activate() {

        status = WabaStatus.ACTIVE;

    }

    /**
     * Suspend the WABA inside Apargo.
     */
    public void suspend() {

        status = WabaStatus.SUSPENDED;

    }

    /**
     * Disconnect the WABA.
     *
     * Used when authentication is revoked
     * or the WABA is intentionally removed.
     */
    public void disconnect() {

        status = WabaStatus.DISCONNECTED;

    }

    /**
     * Soft delete.
     */
    public void markDeleted() {

        deletedAt = Instant.now();

    }

    /**
     * Returns true if this entity
     * has been soft deleted.
     */
    public boolean isDeleted() {

        return deletedAt != null;

    }

}