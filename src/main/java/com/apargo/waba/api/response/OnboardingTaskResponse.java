package com.apargo.waba.api.response;

import com.apargo.waba.domain.enums.OnboardingStatus;
import com.apargo.waba.domain.enums.OnboardingStep;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Public view of an {@link com.apargo.waba.domain.entity.OnboardingTask}.
 * <p>
 * Deliberately omits {@code oauthCode} and {@code encryptedAccessToken} —
 * neither should ever leave the service, even to an authenticated caller.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Current state of an onboarding task")
public class OnboardingTaskResponse {

    private Long id;
    private Long organizationId;
    private Long projectId;
    private OnboardingStatus status;
    private OnboardingStep currentStep;
    private Integer retryCount;
    private String resolvedWabaId;
    private String resolvedBusinessManagerId;
    private String resolvedPhoneNumberId;
    private Long resultWabaAccountId;
    private String resultSummary;
    private String errorMessage;
    private Instant startedAt;
    private Instant finishedAt;
    private Instant createdAt;
    private Instant updatedAt;
}