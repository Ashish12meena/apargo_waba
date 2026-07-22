package com.apargo.waba.application.mapper;

import com.apargo.waba.api.response.OnboardingTaskResponse;
import com.apargo.waba.domain.entity.OnboardingTask;
import org.springframework.stereotype.Component;

/**
 * Maps {@link OnboardingTask} (domain) to {@link OnboardingTaskResponse} (api).
 * <p>
 * Kept as an explicit, hand-written mapper rather than a
 * MapStruct/reflection-based one — the mapping is small and the explicit
 * omission of {@code oauthCode} / {@code encryptedAccessToken} is a
 * security property worth being visible in code, not hidden behind
 * generated mapping code.
 */
@Component
public class OnboardingTaskMapper {

    public OnboardingTaskResponse toResponse(OnboardingTask task) {
        if (task == null) {
            return null;
        }
        return OnboardingTaskResponse.builder()
                .id(task.getId())
                .organizationId(task.getOrganizationId())
                .projectId(task.getProjectId())
                .status(task.getStatus())
                .currentStep(task.getCurrentStep())
                .retryCount(task.getRetryCount())
                .resolvedWabaId(task.getResolvedWabaId())
                .resolvedBusinessManagerId(task.getResolvedBusinessManagerId())
                .resolvedPhoneNumberId(task.getResolvedPhoneNumberId())
                .resultWabaAccountId(task.getResultWabaAccountId())
                .resultSummary(task.getResultSummary())
                .errorMessage(task.getErrorMessage())
                .startedAt(task.getStartedAt())
                .finishedAt(task.getFinishedAt())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
}