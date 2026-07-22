package com.apargo.waba.domain.entity;



import com.apargo.waba.domain.enums.MetaTokenType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Stores the credentials used to authenticate requests to the Meta Graph API.
 *
 * <h2>What is this entity?</h2>
 *
 * Every request made to the WhatsApp Cloud API must include an access token.
 * Without a valid token Meta rejects every request with HTTP 401.
 *
 * This entity represents that authentication credential together with
 * metadata describing how it was obtained and when it expires.
 *
 * <h2>Why have a separate table?</h2>
 *
 * A WhatsApp Business Account (WABA) should never own the token directly.
 *
 * Tokens are issued for a Meta Business and can be reused by multiple WABAs
 * belonging to the same organization.
 *
 * Separating credentials from WABAs provides:
 *
 * <ul>
 *     <li>Credential rotation without modifying WABAs.</li>
 *     <li>Historical token tracking.</li>
 *     <li>Support for multiple token types.</li>
 *     <li>Future migration to permanent System User tokens.</li>
 * </ul>
 *
 * <h2>Token Types</h2>
 *
 * Meta currently supports several authentication mechanisms.
 *
 * <ul>
 *     <li>User Access Token</li>
 *     <li>Long Lived User Token</li>
 *     <li>System User (Permanent) Token</li>
 * </ul>
 *
 * The actual token type is stored in {@link #tokenType}.
 *
 * <h2>Security</h2>
 *
 * The access token is effectively a password for the organization's Meta assets.
 * It should always be encrypted before being persisted and must never appear
 * in application logs.
 */
@Entity
@Table(
    name = "meta_oauth_tokens",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_meta_oauth_tokens_org",
            columnNames = "organization_id"
        )
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLRestriction("deleted_at IS NULL")
public class MetaOAuthToken {

    /**
     * Internal surrogate key.
     *
     * Never exposed outside the service.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Internal organization that owns this Meta connection.
     *
     * <p>
     * This is NOT a Meta identifier.
     * It references our own Organization Service.
     * </p>
     *
     * <p>
     * Keeping the organization id directly in this table allows us to
     * locate credentials without performing joins.
     * </p>
     */
    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    /**
     * Encrypted Meta access token.
     *
     * <h3>Why store it?</h3>
     *
     * Every Graph API request requires:
     *
     * Authorization:
     * Bearer &lt;token&gt;
     *
     * Without this value we cannot:
     *
     * <ul>
     *     <li>Send messages</li>
     *     <li>Create templates</li>
     *     <li>Read phone numbers</li>
     *     <li>Manage WABAs</li>
     * </ul>
     *
     * <h3>Security Note</h3>
     *
     * The value stored in the database should always be encrypted.
     * Plaintext tokens should never be persisted.
     */
    @Lob
    @Column(name = "access_token", nullable = false)
    private String accessToken;

    /**
     * Time when Meta considers this token expired.
     *
     * <p>
     * Permanent System User tokens usually never expire and therefore
     * this field remains NULL.
     * </p>
     *
     * <p>
     * Before making Graph API calls the application may verify
     * whether the token is still valid.
     * </p>
     */
    @Column(name = "expires_at")
    private Instant expiresAt;

    /**
     * Meta User ID that granted this token.
     *
     * Present only for user-based OAuth flows.
     *
     * Permanent System User tokens normally don't populate this field.
     */
    @Column(name = "meta_user_id", length = 100)
    private String metaUserId;

    /**
     * Meta System User ID.
     *
     * Applicable only when using System User authentication.
     *
     * This identifies the automation account inside Meta Business Manager.
     */
    @Column(name = "system_user_id", length = 100)
    private String systemUserId;

    /**
     * Meta Business Manager (Business Portfolio) identifier.
     *
     * This business owns the authentication credential.
     *
     * A single Business Manager may own multiple WhatsApp Business Accounts.
     */
    @Column(name = "business_manager_id", length = 100)
    private String businessManagerId;

    /**
     * Permissions granted during OAuth.
     *
     * Example:
     *
     * whatsapp_business_management,
     * whatsapp_business_messaging,
     * business_management
     *
     * Useful for diagnostics when an API call fails because a permission
     * wasn't granted.
     */
    @Column(name = "granted_scopes", length = 500)
    private String grantedScopes;

    /**
     * Timestamp when this credential was issued by Meta.
     */
    @Column(name = "granted_at")
    private Instant grantedAt;

    /**
     * Authentication mechanism used to obtain this token.
     *
     * Examples:
     *
     * USER_TOKEN
     * SYSTEM_USER
     *
     * This determines whether the application should expect
     * token expiration.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "token_type", nullable = false)
    @Builder.Default
    private MetaTokenType tokenType = MetaTokenType.USER_TOKEN;

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
     *
     * Records are never physically removed from the database.
     */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    // ---------------------------------------------------------
    // Relationships
    // ---------------------------------------------------------

    /**
     * Every token may manage multiple WhatsApp Business Accounts.
     *
     * Example:
     *
     * Business Manager
     *      │
     *      └── Token
     *             │
     *             ├── WABA A
     *             ├── WABA B
     *             └── WABA C
     */
    @OneToMany(
            mappedBy = "metaOAuthToken",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<WabaAccount> wabaAccounts = new ArrayList<>();

    // ---------------------------------------------------------
    // Business Methods
    // ---------------------------------------------------------

    /**
     * Returns true when the token has expired.
     *
     * Permanent tokens always return false because
     * {@code expiresAt == null}.
     */
    public boolean isExpired() {

        return expiresAt != null &&
                Instant.now().isAfter(expiresAt);

    }

    /**
     * Returns true if this credential represents
     * a permanent System User token.
     */
    public boolean isPermanentToken() {

        return tokenType == MetaTokenType.SYSTEM_USER;

    }

    /**
     * Marks this credential as deleted.
     */
    public void markDeleted() {

        this.deletedAt = Instant.now();

    }

    /**
     * Returns whether the entity has been soft deleted.
     */
    public boolean isDeleted() {

        return deletedAt != null;

    }

}