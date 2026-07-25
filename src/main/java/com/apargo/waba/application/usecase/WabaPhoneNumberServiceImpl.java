package com.apargo.waba.application.usecase;

import com.apargo.waba.api.request.CreateWabaPhoneNumberRequest;
import com.apargo.waba.api.request.UpdateWabaPhoneNumberRequest;
import com.apargo.waba.api.response.WabaPhoneNumberResponse;
import com.apargo.waba.application.mapper.WabaPhoneNumberMapper;
import com.apargo.waba.application.port.in.WabaPhoneNumberUsecase;
import com.apargo.waba.application.port.out.WabaAccountRepositoryPort;
import com.apargo.waba.application.port.out.WabaPhoneNumberRepositoryPort;
import com.apargo.waba.common.exception.DuplicateResourceException;
import com.apargo.waba.common.exception.ResourceNotFoundException;
import com.apargo.waba.domain.entity.WabaPhoneNumber;
import com.apargo.waba.domain.enums.PhoneNumberStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of {@link WabaPhoneNumberUsecase}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WabaPhoneNumberServiceImpl implements WabaPhoneNumberUsecase {

    private final WabaPhoneNumberRepositoryPort wabaPhoneNumberRepositoryPort;
    private final WabaAccountRepositoryPort wabaAccountRepositoryPort;
    private final WabaPhoneNumberMapper mapper;

    @Override
    @Transactional
    public WabaPhoneNumberResponse create(CreateWabaPhoneNumberRequest request) {
        log.info("Creating phone number wabaAccountId={} phoneNumberId={}",
                request.getWabaAccountId(), request.getWhatsappPhoneNumberId());

        requireWabaAccountExists(request.getWabaAccountId());

        // Pre-check gives a clean 409 for the common case. It is not a
        // substitute for the unique constraint: two concurrent creates can
        // both pass this check, so the catch below still has to handle the
        // race. The check also can't see soft-deleted rows (@SQLRestriction),
        // while the DB constraint can — re-registering a previously deleted
        // number therefore also lands in the catch.
        if (wabaPhoneNumberRepositoryPort.existsByWhatsappPhoneNumberId(request.getWhatsappPhoneNumberId())) {
            throw new DuplicateResourceException(
                    "A phone number is already registered for phoneNumberId="
                            + request.getWhatsappPhoneNumberId());
        }

        WabaPhoneNumber entity = mapper.toEntity(request);

        try {
            WabaPhoneNumber saved = wabaPhoneNumberRepositoryPort.save(entity);
            log.info("Created phone number id={} phoneNumberId={}",
                    saved.getId(), saved.getWhatsappPhoneNumberId());
            return mapper.toResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Unique constraint hit creating phoneNumberId={}: {}",
                    request.getWhatsappPhoneNumberId(), ex.getMessage());
            throw new DuplicateResourceException(
                    "A phone number is already registered for phoneNumberId="
                            + request.getWhatsappPhoneNumberId());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public WabaPhoneNumberResponse getById(Long id) {
        log.info("Fetching phone number id={}", id);
        return mapper.toResponse(requirePhoneNumber(id));
    }

    @Override
    @Transactional(readOnly = true)
    public WabaPhoneNumberResponse getByWhatsappPhoneNumberId(String whatsappPhoneNumberId) {
        log.info("Fetching phone number phoneNumberId={}", whatsappPhoneNumberId);

        WabaPhoneNumber phoneNumber = wabaPhoneNumberRepositoryPort
                .findByWhatsappPhoneNumberId(whatsappPhoneNumberId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Phone number not found for phoneNumberId=" + whatsappPhoneNumberId));

        return mapper.toResponse(phoneNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WabaPhoneNumberResponse> listByWabaAccount(Long wabaAccountId, PhoneNumberStatus status) {
        log.info("Listing phone numbers wabaAccountId={} status={}", wabaAccountId, status);

        requireWabaAccountExists(wabaAccountId);

        // An unknown WABA is a 404 (checked above); a known WABA with no
        // phones is an empty list, not an error.
        List<WabaPhoneNumber> phoneNumbers = status == null
                ? wabaPhoneNumberRepositoryPort.findByWabaAccountId(wabaAccountId)
                : wabaPhoneNumberRepositoryPort.findByWabaAccountIdAndStatus(wabaAccountId, status);

        return mapper.toResponses(phoneNumbers);
    }

    @Override
    @Transactional
    public WabaPhoneNumberResponse update(Long id, UpdateWabaPhoneNumberRequest request) {
        log.info("Updating phone number id={}", id);

        WabaPhoneNumber phoneNumber = requirePhoneNumber(id);
        mapper.applyUpdate(phoneNumber, request);

        WabaPhoneNumber saved = wabaPhoneNumberRepositoryPort.save(phoneNumber);
        log.info("Updated phone number id={} status={}", saved.getId(), saved.getStatus());

        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        log.info("Soft deleting phone number id={}", id);

        WabaPhoneNumber phoneNumber = requirePhoneNumber(id);

        // Soft delete, per docs/rules.md — the row stays for audit, and any
        // waba_daily_message_usage rows pointing at it keep resolving.
        // The @SQLRestriction on the entity makes it invisible to every
        // subsequent read.
        phoneNumber.markDeleted();
        wabaPhoneNumberRepositoryPort.save(phoneNumber);

        log.info("Soft deleted phone number id={} phoneNumberId={}",
                id, phoneNumber.getWhatsappPhoneNumberId());
    }

    // ----------------------------------------------------
    // Helpers
    // ----------------------------------------------------

    private WabaPhoneNumber requirePhoneNumber(Long id) {
        return wabaPhoneNumberRepositoryPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Phone number not found for id=" + id));
    }

    /**
     * Guards against orphaning a phone under a WABA that doesn't exist.
     * The FK would reject it anyway, but that surfaces as an opaque 500 —
     * this turns it into a 404 that names the missing account.
     */
    private void requireWabaAccountExists(Long wabaAccountId) {
        if (wabaAccountRepositoryPort.findById(wabaAccountId).isEmpty()) {
            throw new ResourceNotFoundException(
                    "WABA account not found for id=" + wabaAccountId);
        }
    }
}