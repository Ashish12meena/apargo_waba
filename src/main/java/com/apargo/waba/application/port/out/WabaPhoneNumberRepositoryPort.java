package com.apargo.waba.application.port.out;

import com.apargo.waba.domain.entity.WabaPhoneNumber;
import com.apargo.waba.domain.enums.PhoneNumberStatus;

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

    /**
     * Uniqueness pre-check for registration, so a duplicate Meta Phone
     * Number ID surfaces as a clean 409 instead of a constraint violation.
     * <p>
     * Note this cannot see soft-deleted rows (the entity carries
     * {@code @SQLRestriction("deleted_at IS NULL")}) whereas the database
     * unique index can — callers must still handle the constraint failing.
     */
    boolean existsByWhatsappPhoneNumberId(String whatsappPhoneNumberId);

    List<WabaPhoneNumber> findByWabaAccountId(Long wabaAccountId);

    /** Phone numbers under one WABA narrowed to a single operational status. */
    List<WabaPhoneNumber> findByWabaAccountIdAndStatus(Long wabaAccountId, PhoneNumberStatus status);

    /**
     * Batch lookup — used when listing phone numbers for several WABAs at
     * once (e.g. all WABAs in an organization) in a single query instead
     * of N individual {@link #findByWabaAccountId(Long)} calls.
     */
    List<WabaPhoneNumber> findByWabaAccountIdIn(List<Long> wabaAccountIds);

    /**
     * Hard delete. Prefer {@code entity.markDeleted()} + {@link #save} for
     * anything user-facing — {@code docs/rules.md} mandates soft delete;
     * this stays for maintenance/cleanup paths.
     */
    void deleteById(Long id);
}