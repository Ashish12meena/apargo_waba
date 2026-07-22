package com.apargo.waba.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Maps a Project to a WhatsApp Business Account (WABA).
 *
 * <h2>Why does this table exist?</h2>
 *
 * A WABA belongs to an Organization, not to a Project.
 *
 * Multiple projects inside the same organization may need
 * to send messages using the same WhatsApp Business Account.
 *
 * Instead of storing {@code project_id} inside
 * {@link WabaAccount}, we introduce this mapping table.
 *
 * This creates a many-to-many relationship:
 *
 * <pre>
 *                  Organization
 *                        │
 *                        ▼
 *                 WhatsApp Business Account
 *                        ▲
 *                        │
 *      ┌─────────────────┼──────────────────┐
 *      │                 │                  │
 *      ▼                 ▼                  ▼
 *   Project A        Project B         Project C
 * </pre>
 *
 * <h2>Benefits</h2>
 *
 * <ul>
 *     <li>One WABA can serve many projects.</li>
 *     <li>Projects can switch WABAs without modifying the WABA itself.</li>
 *     <li>Supports future routing strategies.</li>
 *     <li>Keeps ownership and usage separate.</li>
 * </ul>
 *
 * <h2>Future Extensibility</h2>
 *
 * Additional routing configuration may later be added here:
 *
 * <ul>
 *     <li>Traffic percentage</li>
 *     <li>Priority</li>
 *     <li>Fallback WABA</li>
 *     <li>Allowed message categories</li>
 *     <li>Rate limits</li>
 * </ul>
 */
@Entity
@Table(
        name = "project_waba_assignments",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_project_waba_assignment",
                        columnNames = {
                                "project_id",
                                "waba_account_id"
                        }
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLRestriction("deleted_at IS NULL")
public class ProjectWabaAssignment {

    /**
     * Internal database identifier.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Internal Project identifier.
     *
     * This value belongs to the Project Service.
     *
     * No foreign key is declared because Projects
     * are managed by another microservice.
     *
     * This field simply stores the external reference.
     */
    @Column(name = "project_id", nullable = false)
    private Long projectId;

    /**
     * Associated WhatsApp Business Account.
     *
     * Every assignment connects one project
     * with one WABA.
     */
    @Column(name = "waba_account_id", nullable = false)
    private Long wabaAccountId;

    /**
     * Indicates whether this is the project's
     * preferred WABA.
     *
     * A project may be assigned multiple WABAs:
     *
     * Marketing WABA
     *
     * Support WABA
     *
     * Sales WABA
     *
     * One of them may be marked as the default
     * when no explicit routing decision is made.
     *
     * The application should ensure that
     * only one assignment per project
     * has this value set to TRUE.
     */
    @Builder.Default
    @Column(name = "is_default", nullable = false)
    private Boolean defaultAssignment = false;

    /**
     * Optional daily sending limit
     * imposed by our application.
     *
     * This value is NOT provided by Meta.
     *
     * It allows organizations to reserve
     * only part of a WABA's capacity
     * for a specific project.
     *
     * Example:
     *
     * Meta limit:
     * 100,000 conversations/day
     *
     * Project A:
     * 20,000
     *
     * Project B:
     * 40,000
     *
     * Project C:
     * Unlimited
     *
     * NULL means:
     *
     * No application-level restriction.
     */
    @Column(name = "custom_daily_limit")
    private Integer customDailyLimit;

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

    // ----------------------------------------------------
    // Relationships
    // ----------------------------------------------------

    /**
     * Associated WhatsApp Business Account.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "waba_account_id",
            insertable = false,
            updatable = false
    )
    private WabaAccount wabaAccount;

    // ----------------------------------------------------
    // Business Methods
    // ----------------------------------------------------

    /**
     * Marks this assignment as the
     * project's default WABA.
     */
    public void makeDefault() {

        this.defaultAssignment = true;

    }

    /**
     * Removes default assignment.
     */
    public void removeDefault() {

        this.defaultAssignment = false;

    }

    /**
     * Returns whether this WABA
     * is the project's preferred
     * routing destination.
     */
    public boolean isDefaultAssignment() {

        return Boolean.TRUE.equals(defaultAssignment);

    }

    /**
     * Returns whether this project
     * has an application-level
     * sending quota.
     */
    public boolean hasCustomLimit() {

        return customDailyLimit != null;

    }

    /**
     * Soft delete.
     */
    public void markDeleted() {

        deletedAt = Instant.now();

    }

    /**
     * Returns true if this record
     * has been soft deleted.
     */
    public boolean isDeleted() {

        return deletedAt != null;

    }

}