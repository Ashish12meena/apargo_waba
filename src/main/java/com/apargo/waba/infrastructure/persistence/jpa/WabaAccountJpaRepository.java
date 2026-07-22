package com.apargo.waba.infrastructure.persistence.jpa;

import com.apargo.waba.domain.entity.WabaAccount;
import com.apargo.waba.domain.enums.WabaStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link WabaAccount}.
 * <p>
 * Infrastructure-only — see {@link com.apargo.waba.application.port.out.WabaAccountRepositoryPort}.
 */
public interface WabaAccountJpaRepository extends JpaRepository<WabaAccount, Long> {

    Optional<WabaAccount> findByWabaId(String wabaId);

    List<WabaAccount> findByOrganizationId(Long organizationId);

    List<WabaAccount> findByOrganizationIdAndStatus(Long organizationId, WabaStatus status);

    List<WabaAccount> findByBusinessManagerId(String businessManagerId);
}