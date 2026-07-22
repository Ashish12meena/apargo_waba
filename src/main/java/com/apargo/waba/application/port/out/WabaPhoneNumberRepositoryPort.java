package com.apargo.waba.application.port.out;

import com.apargo.waba.domain.entity.WabaPhoneNumber;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for {@link WabaPhoneNumber} persistence.
 */
public interface WabaPhoneNumberRepositoryPort {

    WabaPhoneNumber save(WabaPhoneNumber phoneNumber);

    Optional<WabaPhoneNumber> findById(Long id);

    /** Lookup by Meta's Phone Number ID — used to resolve inbound webhooks/messages. */
    Optional<WabaPhoneNumber> findByWhatsappPhoneNumberId(String whatsappPhoneNumberId);

    List<WabaPhoneNumber> findByWabaAccountId(Long wabaAccountId);

    void deleteById(Long id);
}