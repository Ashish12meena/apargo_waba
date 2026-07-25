package com.apargo.waba.api.response;

import com.apargo.waba.domain.enums.CodeVerificationStatus;
import com.apargo.waba.domain.enums.HealthStatus;
import com.apargo.waba.domain.enums.MessagingLimitTier;
import com.apargo.waba.domain.enums.MessagingThroughputTier;
import com.apargo.waba.domain.enums.NameStatus;
import com.apargo.waba.domain.enums.PhoneNumberQualityRating;
import com.apargo.waba.domain.enums.PhoneNumberStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Public view of a {@link com.apargo.waba.domain.entity.WabaPhoneNumber}.
 * <p>
 * {@code wabaAccountId} was previously omitted because every instance was
 * returned nested inside its parent {@link WabaAccountDetailResponse}. Now
 * that {@code /api/v1/waba-phone-numbers} returns these standalone, the
 * parent id is the only way a caller can tell which WABA a number belongs
 * to, so it is included. It stays populated in the nested case too —
 * redundant there, but a single consistent shape beats two near-identical
 * DTOs.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "A WhatsApp phone number registered under a WABA")
public class WabaPhoneNumberResponse {

    private Long id;
    private Long wabaAccountId;
    private String whatsappPhoneNumberId;
    private String displayPhoneNumber;
    private PhoneNumberStatus status;
    private String verifiedName;
    private PhoneNumberQualityRating qualityRating;
    private MessagingLimitTier messagingLimitTier;
    private MessagingThroughputTier throughputTier;
    private NameStatus nameStatus;
    private HealthStatus healthStatus;
    private Boolean officialBusinessAccount;
    private CodeVerificationStatus verificationStatus;
    private Instant createdAt;
    private Instant updatedAt;
}