package com.apargo.waba.infrastructure.persistence;

import com.apargo.waba.application.port.out.MetaOAuthTokenRepositoryPort;
import com.apargo.waba.domain.entity.MetaOAuthToken;
import com.apargo.waba.infrastructure.persistence.jpa.MetaOAuthTokenJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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
    public boolean existsByOrganizationId(Long organizationId) {
        return jpaRepository.existsByOrganizationId(organizationId);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }
}