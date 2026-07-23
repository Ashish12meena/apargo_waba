package com.apargo.waba.application.port.out;

import com.apargo.waba.domain.entity.MetaOAuthToken;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for {@link MetaOAuthToken} persistence.
 *
 * <p>Defined in terms of the domain entity only - no JPA/Spring Data type
 * ever appears in this interface's signature. {@code application} depends
 * only on {@code domain} (per {@code docs/rules.md}); the JPA-specific
 * implementation lives in {@code infrastructure.persistence} and implements
 * this interface, never the other way round.
 */
public interface MetaOAuthTokenRepositoryPort {

    MetaOAuthToken save(MetaOAuthToken token);

    Optional<MetaOAuthToken> findById(Long id);

    /**
     * An organization is restricted to exactly one Meta connection —
     * see {@code uq_meta_oauth_tokens_org}. This list will contain at
     * most one element in normal operation.
     */
    List<MetaOAuthToken> findByOrganizationId(Long organizationId);

    void deleteById(Long id);
}