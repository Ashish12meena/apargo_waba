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
     * An organization may hold more than one Meta connection (e.g. more
     * than one Business Manager) — see {@code waba_sql.md} note on
     * {@code meta_oauth_tokens}.
     */
    List<MetaOAuthToken> findByOrganizationId(Long organizationId);

    void deleteById(Long id);
}