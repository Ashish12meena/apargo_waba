package com.apargo.waba.infrastructure.persistence;

import com.apargo.waba.application.port.out.MetaOAuthTokenRepositoryPort;
import com.apargo.waba.domain.entity.MetaOAuthToken;
import com.apargo.waba.infrastructure.persistence.jpa.MetaOAuthTokenJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Implements {@link MetaOAuthTokenRepositoryPort} on top of Spring Data
 * JPA. Per {@code docs/rules.md}: infrastructure implements
 * application/port/out, never the reverse — nothing outside this package
 * should ever reference {@link MetaOAuthTokenJpaRepository} directly.
 */
@Repository
@RequiredArgsConstructor
public class MetaOAuthTokenRepositoryAdapter implements MetaOAuthTokenRepositoryPort {

    private final MetaOAuthTokenJpaRepository jpaRepository;

    @Override
    public MetaOAuthToken save(MetaOAuthToken token) {
        return jpaRepository.save(token);
    }

    @Override
    public Optional<MetaOAuthToken> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<MetaOAuthToken> findByOrganizationId(Long organizationId) {
        return jpaRepository.findByOrganizationId(organizationId);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }
}