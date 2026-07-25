package com.apargo.waba.application.mapper;

import com.apargo.waba.api.request.CreateWabaPhoneNumberRequest;
import com.apargo.waba.api.request.UpdateWabaPhoneNumberRequest;
import com.apargo.waba.api.response.WabaPhoneNumberResponse;
import com.apargo.waba.domain.entity.WabaPhoneNumber;
import com.apargo.waba.domain.enums.PhoneNumberStatus;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Maps {@link WabaPhoneNumber} between its API request/response views and
 * the domain entity.
 * <p>
 * Deliberately separate from {@link WabaAccountMapper}: that one owns the
 * account aggregate and only needs a read-only phone projection, whereas
 * this one owns the full write path (create + partial update) for the
 * standalone phone-number endpoints.
 */
@Component
public class WabaPhoneNumberMapper {

    public WabaPhoneNumberResponse toResponse(WabaPhoneNumber phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }
        return WabaPhoneNumberResponse.builder()
                .id(phoneNumber.getId())
                .wabaAccountId(phoneNumber.getWabaAccountId())
                .whatsappPhoneNumberId(phoneNumber.getWhatsappPhoneNumberId())
                .displayPhoneNumber(phoneNumber.getDisplayPhoneNumber())
                .status(phoneNumber.getStatus())
                .verifiedName(phoneNumber.getVerifiedName())
                .qualityRating(phoneNumber.getQualityRating())
                .messagingLimitTier(phoneNumber.getMessagingLimitTier())
                .throughputTier(phoneNumber.getThroughputTier())
                .nameStatus(phoneNumber.getNameStatus())
                .healthStatus(phoneNumber.getHealthStatus())
                .officialBusinessAccount(phoneNumber.getOfficialBusinessAccount())
                .verificationStatus(phoneNumber.getVerificationStatus())
                .createdAt(phoneNumber.getCreatedAt())
                .updatedAt(phoneNumber.getUpdatedAt())
                .build();
    }

    public List<WabaPhoneNumberResponse> toResponses(List<WabaPhoneNumber> phoneNumbers) {
        if (phoneNumbers == null || phoneNumbers.isEmpty()) {
            return List.of();
        }
        return phoneNumbers.stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Builds a new entity from a create request.
     * <p>
     * {@code status} and {@code officialBusinessAccount} fall back to the
     * entity's own defaults (ACTIVE / false) when the caller omits them,
     * rather than being written as null — both columns are NOT NULL.
     */
    public WabaPhoneNumber toEntity(CreateWabaPhoneNumberRequest request) {
        if (request == null) {
            return null;
        }
        return WabaPhoneNumber.builder()
                .wabaAccountId(request.getWabaAccountId())
                .whatsappPhoneNumberId(request.getWhatsappPhoneNumberId())
                .displayPhoneNumber(request.getDisplayPhoneNumber())
                .status(request.getStatus() != null ? request.getStatus() : PhoneNumberStatus.ACTIVE)
                .verifiedName(request.getVerifiedName())
                .qualityRating(request.getQualityRating())
                .messagingLimitTier(request.getMessagingLimitTier())
                .throughputTier(request.getThroughputTier())
                .nameStatus(request.getNameStatus())
                .healthStatus(request.getHealthStatus())
                .officialBusinessAccount(request.getOfficialBusinessAccount() != null
                        ? request.getOfficialBusinessAccount()
                        : Boolean.FALSE)
                .verificationStatus(request.getVerificationStatus())
                .build();
    }

    /**
     * Applies a partial update onto a managed entity, in place.
     * <p>
     * Null means "leave unchanged" — see
     * {@link UpdateWabaPhoneNumberRequest} for why. Neither
     * {@code wabaAccountId} nor {@code whatsappPhoneNumberId} is touched:
     * both are immutable for the lifetime of the record.
     */
    public void applyUpdate(WabaPhoneNumber target, UpdateWabaPhoneNumberRequest request) {
        if (target == null || request == null) {
            return;
        }
        if (request.getDisplayPhoneNumber() != null) {
            target.setDisplayPhoneNumber(request.getDisplayPhoneNumber());
        }
        if (request.getStatus() != null) {
            target.setStatus(request.getStatus());
        }
        if (request.getVerifiedName() != null) {
            target.setVerifiedName(request.getVerifiedName());
        }
        if (request.getQualityRating() != null) {
            target.setQualityRating(request.getQualityRating());
        }
        if (request.getMessagingLimitTier() != null) {
            target.setMessagingLimitTier(request.getMessagingLimitTier());
        }
        if (request.getThroughputTier() != null) {
            target.setThroughputTier(request.getThroughputTier());
        }
        if (request.getNameStatus() != null) {
            target.setNameStatus(request.getNameStatus());
        }
        if (request.getHealthStatus() != null) {
            target.setHealthStatus(request.getHealthStatus());
        }
        if (request.getOfficialBusinessAccount() != null) {
            target.setOfficialBusinessAccount(request.getOfficialBusinessAccount());
        }
        if (request.getVerificationStatus() != null) {
            target.setVerificationStatus(request.getVerificationStatus());
        }
    }
}