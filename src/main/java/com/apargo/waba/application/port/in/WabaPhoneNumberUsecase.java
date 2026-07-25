package com.apargo.waba.application.port.in;

import com.apargo.waba.api.request.CreateWabaPhoneNumberRequest;
import com.apargo.waba.api.request.UpdateWabaPhoneNumberRequest;
import com.apargo.waba.api.response.WabaPhoneNumberResponse;
import com.apargo.waba.domain.enums.PhoneNumberStatus;

import java.util.List;

/**
 * Inbound port for CRUD on {@link com.apargo.waba.domain.entity.WabaPhoneNumber}.
 * <p>
 * Soft-deleted rows are invisible throughout — the entity carries
 * {@code @SQLRestriction("deleted_at IS NULL")}, so every read below
 * silently excludes them and {@link #delete(Long)} only stamps
 * {@code deleted_at} rather than issuing a DELETE.
 */
public interface WabaPhoneNumberUsecase {

    /**
     * Registers a new phone number under an existing WABA.
     *
     * @throws com.apargo.waba.common.exception.ResourceNotFoundException
     *         if the parent WABA account does not exist
     * @throws com.apargo.waba.common.exception.DuplicateResourceException
     *         if a phone with the same Meta Phone Number ID is already registered
     */
    WabaPhoneNumberResponse create(CreateWabaPhoneNumberRequest request);

    /**
     * A single phone number by internal id.
     *
     * @throws com.apargo.waba.common.exception.ResourceNotFoundException if not found
     */
    WabaPhoneNumberResponse getById(Long id);

    /**
     * A single phone number by Meta's Phone Number ID — the lookup used to
     * resolve inbound webhooks to a local record.
     *
     * @throws com.apargo.waba.common.exception.ResourceNotFoundException if not found
     */
    WabaPhoneNumberResponse getByWhatsappPhoneNumberId(String whatsappPhoneNumberId);

    /**
     * Every phone number registered under a WABA, optionally narrowed to a
     * single operational status.
     *
     * @param status optional — when null, all statuses are returned
     * @throws com.apargo.waba.common.exception.ResourceNotFoundException
     *         if the WABA account does not exist
     */
    List<WabaPhoneNumberResponse> listByWabaAccount(Long wabaAccountId, PhoneNumberStatus status);

    /**
     * Applies a partial update. Null fields on the request are left
     * unchanged; {@code wabaAccountId} and {@code whatsappPhoneNumberId}
     * are immutable and cannot be updated.
     *
     * @throws com.apargo.waba.common.exception.ResourceNotFoundException if not found
     */
    WabaPhoneNumberResponse update(Long id, UpdateWabaPhoneNumberRequest request);

    /**
     * Soft delete — stamps {@code deleted_at}, keeping the row for audit
     * and for any historical usage records that reference it.
     *
     * @throws com.apargo.waba.common.exception.ResourceNotFoundException if not found
     */
    void delete(Long id);
}