package com.apargo.waba.application.port.in;

import com.apargo.waba.api.request.StartOnboardingRequest;
import com.apargo.waba.api.response.OnboardingTaskResponse;
import com.apargo.waba.domain.enums.OnboardingStatus;

import java.util.List;

/**
 * Inbound port for the Embedded Signup onboarding workflow.
 * <p>
 * {@code api} depends only on this interface — never on
 * {@code OnboardingServiceImpl} or on {@code OnboardingTaskRepositoryPort}
 * directly, per {@code docs/rules.md}.
 */
public interface OnboardingUsecase {

    /**
     * Starts a new onboarding attempt, or returns the existing task
     * unchanged if {@code idempotencyKey} was already used — replaying a
     * start request must never create a duplicate task.
     */
    OnboardingTaskResponse startOnboarding(StartOnboardingRequest request);

    OnboardingTaskResponse getTask(Long taskId);

    List<OnboardingTaskResponse> listTasks(Long organizationId, OnboardingStatus status);

    /**
     * Resumes a {@code FAILED} task from its last checkpoint.
     *
     * @throws com.apargo.waba.common.exception.InvalidOnboardingStateException
     *         if the task is not retryable (wrong status, or retry limit reached)
     */
    OnboardingTaskResponse retryTask(Long taskId);

    /**
     * Cancels a task that has not yet completed.
     *
     * @throws com.apargo.waba.common.exception.InvalidOnboardingStateException
     *         if the task already {@code COMPLETED}
     */
    OnboardingTaskResponse cancelTask(Long taskId);

    /** Soft-deletes a task record. */
    void deleteTask(Long taskId);
}