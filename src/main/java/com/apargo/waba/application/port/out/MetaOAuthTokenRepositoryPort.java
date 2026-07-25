package com.apargo.waba.application.port.out;

import com.apargo.waba.domain.entity.MetaOAuthToken;

import java.util.List;
import java.util.Optional;

public interface MetaOAuthTokenRepositoryPort {

    MetaOAuthToken save(MetaOAuthToken token);

    Optional<MetaOAuthToken> findById(Long id);

    List<MetaOAuthToken> findByOrganizationId(Long organizationId);

    /** Pre-check for create — uq_meta_oauth_tokens_org still enforces it at the DB level. */
    boolean existsByOrganizationId(Long organizationId);

    void deleteById(Long id);
}