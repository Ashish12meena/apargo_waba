package com.apargo.waba.application.port.out;

import com.apargo.waba.domain.entity.WabaAccount;
import com.apargo.waba.domain.enums.WabaStatus;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for {@link WabaAccount} persistence.
 */
public interface WabaAccountRepositoryPort {

    WabaAccount save(WabaAccount wabaAccount);

    Optional<WabaAccount> findById(Long id);

    /** Lookup by Meta's globally unique WABA id — used to resolve webhook events. */
    Optional<WabaAccount> findByWabaId(String wabaId);

    List<WabaAccount> findByOrganizationId(Long organizationId);

    List<WabaAccount> findByOrganizationIdAndStatus(Long organizationId, WabaStatus status);

    /**
     * WABAs sharing the same Meta Business Manager — needed to sum daily
     * usage at the portfolio level (Meta enforces limits per Business
     * Manager, not per WABA).
     */
    List<WabaAccount> findByBusinessManagerId(String businessManagerId);

    void deleteById(Long id);
}