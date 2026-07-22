package com.apargo.waba.infrastructure.persistence.jpa;

import com.apargo.waba.domain.entity.MetaOAuthToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link MetaOAuthToken}.
 * <p>
 * Infrastructure-only — never injected outside {@code infrastructure.persistence}.
 * Callers in {@code application}/{@code api} depend on
 * {@link com.apargo.waba.application.port.out.MetaOAuthTokenRepositoryPort} instead.
 */
public interface MetaOAuthTokenJpaRepository extends JpaRepository<MetaOAuthToken, Long> {

    List<MetaOAuthToken> findByOrganizationId(Long organizationId);
}