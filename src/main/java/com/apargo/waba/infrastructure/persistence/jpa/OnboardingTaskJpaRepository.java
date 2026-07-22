package com.apargo.waba.infrastructure.persistence.jpa;

import com.apargo.waba.domain.entity.OnboardingTask;
import com.apargo.waba.domain.enums.OnboardingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link OnboardingTask}.
 * <p>
 * Infrastructure-only — see
 * {@link com.apargo.waba.application.port.out.OnboardingTaskRepositoryPort}.
 */
public interface OnboardingTaskJpaRepository extends JpaRepository<OnboardingTask, Long> {

    Optional<OnboardingTask> findByIdempotencyKey(String idempotencyKey);

    List<OnboardingTask> findByOrganizationId(Long organizationId);

    List<OnboardingTask> findByOrganizationIdAndStatus(Long organizationId, OnboardingStatus status);
}