package com.apargo.waba.domain.entity;

import com.apargo.waba.domain.enums.OnboardingStatus;
import com.apargo.waba.domain.enums.OnboardingStep;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "onboarding_tasks",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_onboarding_idempotency",
                        columnNames = "idempotency_key")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLRestriction("deleted_at IS NULL")
public class OnboardingTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Organization requesting the onboarding.
     */
    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    /**
     * Project that initiated the onboarding.
     * Used for automatic WABA assignment after completion.
     */
    @Column(name = "project_id")
    private Long projectId;

    /**
     * OAuth authorization code returned by Meta.
     * Used once to obtain an access token.
     */
    @Column(name = "oauth_code", nullable = false, length = 500)
    private String oauthCode;

    /**
     * Current workflow state.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OnboardingStatus status = OnboardingStatus.PENDING;

    /**
     * Current execution checkpoint.
     * Allows the workflow to resume after failures.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "current_step")
    private OnboardingStep currentStep;

    /**
     * Number of retry attempts.
     */
    @Builder.Default
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    /**
     * Prevents duplicate onboarding requests.
     */
    @Column(name = "idempotency_key", nullable = false, length = 200)
    private String idempotencyKey;

    /**
     * Temporary encrypted access token obtained during onboarding.
     */
    @Lob
    @Column(name = "encrypted_access_token")
    private String encryptedAccessToken;

    /**
     * Token lifetime returned by Meta.
     */
    @Column(name = "token_expires_in")
    private Long tokenExpiresIn;

    /**
     * WABA discovered during onboarding.
     */
    @Column(name = "resolved_waba_id")
    private String resolvedWabaId;

    /**
     * Business Manager discovered during onboarding.
     */
    @Column(name = "resolved_business_manager_id")
    private String resolvedBusinessManagerId;

    /**
     * Phone Number ID discovered during onboarding.
     */
    @Column(name = "resolved_phone_number_id")
    private String resolvedPhoneNumberId;

    /**
     * Local WABA created after successful onboarding.
     */
    @Column(name = "result_waba_account_id")
    private Long resultWabaAccountId;

    /**
     * Completed workflow steps.
     * Prefer storing as JSON.
     */
    @Lob
    @Column(name = "completed_steps")
    private String completedSteps;

    /**
     * Human-readable success summary.
     */
    @Lob
    @Column(name = "result_summary")
    private String resultSummary;

    /**
     * Last error encountered during onboarding.
     */
    @Lob
    @Column(name = "error_message")
    private String errorMessage;

    /**
     * Processing start time.
     */
    @Column(name = "started_at")
    private Instant startedAt;

    /**
     * Processing completion time.
     */
    @Column(name = "finished_at")
    private Instant finishedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    // ----------------------------------------------------
    // Business Methods
    // ----------------------------------------------------

    public void start(OnboardingStep step) {
        this.status = OnboardingStatus.PROCESSING;
        this.currentStep = step;
        this.startedAt = Instant.now();
    }

    public void moveToStep(OnboardingStep step) {
        this.currentStep = step;
    }

    public void complete(Long wabaAccountId) {
        this.status = OnboardingStatus.COMPLETED;
        this.resultWabaAccountId = wabaAccountId;
        this.finishedAt = Instant.now();
    }

    public void fail(String error) {
        this.status = OnboardingStatus.FAILED;
        this.errorMessage = error;
        this.finishedAt = Instant.now();
    }

    public void cancel() {
        this.status = OnboardingStatus.CANCELLED;
        this.finishedAt = Instant.now();
    }

    public void incrementRetry() {
        this.retryCount++;
    }

    public boolean canRetry(int maxRetries) {
        return retryCount < maxRetries
                && status == OnboardingStatus.FAILED;
    }

    public boolean isCompleted() {
        return status == OnboardingStatus.COMPLETED;
    }

    public boolean isProcessing() {
        return status == OnboardingStatus.PROCESSING;
    }

    public boolean isFailed() {
        return status == OnboardingStatus.FAILED;
    }

    public void markDeleted() {
        this.deletedAt = Instant.now();
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

}