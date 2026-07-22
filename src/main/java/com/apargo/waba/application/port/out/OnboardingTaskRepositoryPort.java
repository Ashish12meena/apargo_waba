package com.apargo.waba.application.port.out;

import com.apargo.waba.domain.entity.OnboardingTask;
import com.apargo.waba.domain.enums.OnboardingStatus;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for {@link OnboardingTask} persistence.
 */
public interface OnboardingTaskRepositoryPort {

    OnboardingTask save(OnboardingTask task);

    Optional<OnboardingTask> findById(Long id);

    /** Enforces idempotent onboarding starts — see entity's {@code idempotencyKey}. */
    Optional<OnboardingTask> findByIdempotencyKey(String idempotencyKey);

    List<OnboardingTask> findByOrganizationId(Long organizationId);

    List<OnboardingTask> findByOrganizationIdAndStatus(Long organizationId, OnboardingStatus status);
}