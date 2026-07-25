package com.apargo.waba.application.mapper;

import com.apargo.waba.api.response.WabaAccountDetailResponse;
import com.apargo.waba.api.response.WabaAccountResponse;
import com.apargo.waba.api.response.WabaPhoneNumberResponse;
import com.apargo.waba.domain.entity.WabaAccount;
import com.apargo.waba.domain.entity.WabaPhoneNumber;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Maps {@link WabaAccount} (domain) to its API views —
 * {@link WabaAccountResponse} (summary, used by list endpoints) and
 * {@link WabaAccountDetailResponse} (summary + {@link WabaPhoneNumber}s,
 * used by single-account detail endpoints).
 */
@Component
public class WabaAccountMapper {

    public WabaAccountResponse toResponse(WabaAccount account) {
        if (account == null) {
            return null;
        }
        return WabaAccountResponse.builder()
                .id(account.getId())
                .organizationId(account.getOrganizationId())
                .wabaId(account.getWabaId())
                .businessManagerId(account.getBusinessManagerId())
                .status(account.getStatus())
                .accountReviewStatus(account.getAccountReviewStatus())
                .businessVerificationStatus(account.getBusinessVerificationStatus())
                .messageTemplateNamespace(account.getMessageTemplateNamespace())
                .timezoneId(account.getTimezoneId())
                .currency(account.getCurrency())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }

    /**
     * Maps a {@link WabaAccount} together with its already-fetched phone
     * numbers into a {@link WabaAccountDetailResponse}. Phone numbers are
     * passed in rather than read off {@code account.getPhoneNumbers()} —
     * callers fetch them explicitly (single query or batched), keeping
     * this mapper free of any persistence/lazy-loading concerns.
     */
    public WabaAccountDetailResponse toDetailResponse(WabaAccount account, List<WabaPhoneNumber> phoneNumbers) {
        if (account == null) {
            return null;
        }
        return WabaAccountDetailResponse.builder()
                .id(account.getId())
                .organizationId(account.getOrganizationId())
                .wabaId(account.getWabaId())
                .businessManagerId(account.getBusinessManagerId())
                .status(account.getStatus())
                .accountReviewStatus(account.getAccountReviewStatus())
                .businessVerificationStatus(account.getBusinessVerificationStatus())
                .messageTemplateNamespace(account.getMessageTemplateNamespace())
                .timezoneId(account.getTimezoneId())
                .currency(account.getCurrency())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .phoneNumbers(toPhoneNumberResponses(phoneNumbers))
                .build();
    }

    public WabaPhoneNumberResponse toPhoneNumberResponse(WabaPhoneNumber phoneNumber) {
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

    private List<WabaPhoneNumberResponse> toPhoneNumberResponses(List<WabaPhoneNumber> phoneNumbers) {
        if (phoneNumbers == null || phoneNumbers.isEmpty()) {
            return List.of();
        }
        return phoneNumbers.stream()
                .map(this::toPhoneNumberResponse)
                .toList();
    }
}