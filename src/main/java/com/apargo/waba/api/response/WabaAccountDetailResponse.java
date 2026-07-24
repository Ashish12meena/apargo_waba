package com.apargo.waba.api.response;

import com.apargo.waba.domain.enums.AccountReviewStatus;
import com.apargo.waba.domain.enums.BusinessVerificationStatus;
import com.apargo.waba.domain.enums.WabaStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

/**
 * Detailed view of a {@link com.apargo.waba.domain.entity.WabaAccount},
 * including every {@link com.apargo.waba.domain.entity.WabaPhoneNumber}
 * registered under it.
 * <p>
 * Deliberately kept separate from {@link WabaAccountResponse}. That DTO
 * backs the list endpoints ({@code GET /api/v1/waba-accounts}), which can
 * return many accounts for one organization/project and should stay
 * cheap — no nested collections. This DTO backs the single-account
 * detail endpoint, where eagerly loading phone numbers is expected.
 * <p>
 * Same field set as {@link WabaAccountResponse} otherwise; kept as a
 * standalone class rather than extending it to avoid mixing Lombok
 * {@code @Builder} with inheritance.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "A WhatsApp Business Account together with its registered phone numbers")
public class WabaAccountDetailResponse {

    private Long id;
    private Long organizationId;
    private String wabaId;
    private String businessManagerId;
    private WabaStatus status;
    private AccountReviewStatus accountReviewStatus;
    private BusinessVerificationStatus businessVerificationStatus;
    private String messageTemplateNamespace;
    private String timezoneId;
    private String currency;
    private Instant createdAt;
    private Instant updatedAt;

    @Builder.Default
    private List<WabaPhoneNumberResponse> phoneNumbers = List.of();
}