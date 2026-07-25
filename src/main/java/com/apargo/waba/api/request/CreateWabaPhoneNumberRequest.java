package com.apargo.waba.api.request;

import com.apargo.waba.domain.enums.CodeVerificationStatus;
import com.apargo.waba.domain.enums.HealthStatus;
import com.apargo.waba.domain.enums.MessagingLimitTier;
import com.apargo.waba.domain.enums.MessagingThroughputTier;
import com.apargo.waba.domain.enums.NameStatus;
import com.apargo.waba.domain.enums.PhoneNumberQualityRating;
import com.apargo.waba.domain.enums.PhoneNumberStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request body to register a {@link com.apargo.waba.domain.entity.WabaPhoneNumber}
 * under an existing WABA.
 * <p>
 * Only {@code wabaAccountId} and {@code whatsappPhoneNumberId} are required —
 * everything else is Meta-owned metadata that is normally populated by the
 * onboarding workflow or a later Graph API sync, and may therefore be omitted
 * at creation time.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Registers a WhatsApp phone number under a WABA")
public class CreateWabaPhoneNumberRequest {

    @NotNull
    @Positive
    @Schema(description = "Internal id of the parent WABA account",
            example = "42", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long wabaAccountId;

    @NotBlank
    @Size(max = 100)
    @Schema(description = "Meta Phone Number ID — Meta's globally unique identifier for this "
            + "phone, used by every Graph API messaging endpoint. This is NOT the customer's "
            + "phone number and it never changes.",
            example = "108745612345678", requiredMode = Schema.RequiredMode.REQUIRED)
    private String whatsappPhoneNumberId;

    @Size(max = 255)
    @Schema(description = "Human-readable display number, shown in dashboards only",
            example = "+91 9876543210")
    private String displayPhoneNumber;

    @Schema(description = "Internal operational status. Defaults to ACTIVE when omitted.",
            example = "PENDING")
    private PhoneNumberStatus status;

    @Size(max = 255)
    @Schema(description = "Verified business name approved by Meta", example = "Apargo Technologies")
    private String verifiedName;

    @Schema(description = "Messaging quality rating reported by Meta", example = "GREEN")
    private PhoneNumberQualityRating qualityRating;

    @Schema(description = "Messaging limit tier reported by Meta", example = "LIMIT_250")
    private MessagingLimitTier messagingLimitTier;

    @Schema(description = "Messaging throughput tier reported by Meta", example = "STANDARD")
    private MessagingThroughputTier throughputTier;

    @Schema(description = "Display name approval status reported by Meta", example = "PENDING")
    private NameStatus nameStatus;

    @Schema(description = "Overall health reported by Meta", example = "GREEN")
    private HealthStatus healthStatus;

    @Schema(description = "Whether Meta has granted Official Business Account (OBA) status. "
            + "Defaults to false when omitted.", example = "false")
    private Boolean officialBusinessAccount;

    @Schema(description = "Phone ownership (OTP) verification state", example = "NOT_VERIFIED")
    private CodeVerificationStatus verificationStatus;
}