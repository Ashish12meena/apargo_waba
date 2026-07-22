package com.apargo.waba.infrastructure.persistence;

import com.apargo.waba.application.port.out.OnboardingTaskRepositoryPort;
import com.apargo.waba.domain.entity.OnboardingTask;
import com.apargo.waba.domain.enums.OnboardingStatus;
import com.apargo.waba.infrastructure.persistence.jpa.OnboardingTaskJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Implements {@link OnboardingTaskRepositoryPort} on top of Spring Data JPA.
 */
@Repository
@RequiredArgsConstructor
public class OnboardingTaskRepositoryAdapter implements OnboardingTaskRepositoryPort {

    private final OnboardingTaskJpaRepository jpaRepository;

    @Override
    public OnboardingTask save(OnboardingTask task) {
        return jpaRepository.save(task);
    }

    @Override
    public Optional<OnboardingTask> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<OnboardingTask> findByIdempotencyKey(String idempotencyKey) {
        return jpaRepository.findByIdempotencyKey(idempotencyKey);
    }

    @Override
    public List<OnboardingTask> findByOrganizationId(Long organizationId) {
        return jpaRepository.findByOrganizationId(organizationId);
    }

    @Override
    public List<OnboardingTask> findByOrganizationIdAndStatus(Long organizationId, OnboardingStatus status) {
        return jpaRepository.findByOrganizationIdAndStatus(organizationId, status);
    }
}