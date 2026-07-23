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

/**
 * Public view of a {@link com.apargo.waba.domain.entity.WabaAccount}.
 * <p>
 * Deliberately omits {@code metaOAuthTokenId} — an internal FK, not useful
 * to an API consumer and not necessary to expose.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "A WhatsApp Business Account known to this platform")
public class WabaAccountResponse {

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
}