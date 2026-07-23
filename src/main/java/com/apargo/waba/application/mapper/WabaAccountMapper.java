package com.apargo.waba.application.mapper;

import com.apargo.waba.api.response.WabaAccountResponse;
import com.apargo.waba.domain.entity.WabaAccount;
import org.springframework.stereotype.Component;

/**
 * Maps {@link WabaAccount} (domain) to {@link WabaAccountResponse} (api).
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
}