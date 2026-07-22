package com.apargo.waba.infrastructure.persistence;

import com.apargo.waba.application.port.out.WabaAccountRepositoryPort;
import com.apargo.waba.domain.entity.WabaAccount;
import com.apargo.waba.domain.enums.WabaStatus;
import com.apargo.waba.infrastructure.persistence.jpa.WabaAccountJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Implements {@link WabaAccountRepositoryPort} on top of Spring Data JPA.
 */
@Repository
@RequiredArgsConstructor
public class WabaAccountRepositoryAdapter implements WabaAccountRepositoryPort {

    private final WabaAccountJpaRepository jpaRepository;

    @Override
    public WabaAccount save(WabaAccount wabaAccount) {
        return jpaRepository.save(wabaAccount);
    }

    @Override
    public Optional<WabaAccount> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<WabaAccount> findByWabaId(String wabaId) {
        return jpaRepository.findByWabaId(wabaId);
    }

    @Override
    public List<WabaAccount> findByOrganizationId(Long organizationId) {
        return jpaRepository.findByOrganizationId(organizationId);
    }

    @Override
    public List<WabaAccount> findByOrganizationIdAndStatus(Long organizationId, WabaStatus status) {
        return jpaRepository.findByOrganizationIdAndStatus(organizationId, status);
    }

    @Override
    public List<WabaAccount> findByBusinessManagerId(String businessManagerId) {
        return jpaRepository.findByBusinessManagerId(businessManagerId);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }
}