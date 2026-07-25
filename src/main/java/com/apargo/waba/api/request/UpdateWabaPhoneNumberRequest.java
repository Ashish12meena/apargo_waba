package com.apargo.waba.api.request;

import com.apargo.waba.domain.enums.CodeVerificationStatus;
import com.apargo.waba.domain.enums.HealthStatus;
import com.apargo.waba.domain.enums.MessagingLimitTier;
import com.apargo.waba.domain.enums.MessagingThroughputTier;
import com.apargo.waba.domain.enums.NameStatus;
import com.apargo.waba.domain.enums.PhoneNumberQualityRating;
import com.apargo.waba.domain.enums.PhoneNumberStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request body to update an existing
 * {@link com.apargo.waba.domain.entity.WabaPhoneNumber}.
 *
 * <h2>Partial update semantics</h2>
 *
 * Every field is optional. A {@code null} field means "leave unchanged" —
 * it is never interpreted as "clear this value". That keeps a caller who
 * only wants to flip {@code status} from having to echo back the entire
 * Meta-owned metadata block and risk clobbering a concurrent sync.
 *
 * <h2>Immutable fields</h2>
 *
 * {@code wabaAccountId} and {@code whatsappPhoneNumberId} are deliberately
 * absent. A phone belongs to exactly one WABA for its lifetime, and Meta's
 * Phone Number ID never changes — re-pointing either would silently break
 * webhook resolution, so both are set once at creation.
 *
 * <h2>Meta-owned fields</h2>
 *
 * {@code verifiedName}, {@code qualityRating}, the tier fields,
 * {@code nameStatus}, {@code healthStatus}, {@code officialBusinessAccount}
 * and {@code verificationStatus} all originate from Meta. They are writable
 * here so the Graph API sync (and support/admin correction) can push updates
 * in, not so they can be edited by hand in normal operation.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Partial update of a registered WhatsApp phone number; null fields are left unchanged")
public class UpdateWabaPhoneNumberRequest {

    @Size(max = 255)
    @Schema(description = "Human-readable display number, shown in dashboards only",
            example = "+91 9876543210")
    private String displayPhoneNumber;

    @Schema(description = "Internal operational status", example = "ACTIVE")
    private PhoneNumberStatus status;

    @Size(max = 255)
    @Schema(description = "Verified business name approved by Meta", example = "Apargo Technologies")
    private String verifiedName;

    @Schema(description = "Messaging quality rating reported by Meta", example = "GREEN")
    private PhoneNumberQualityRating qualityRating;

    @Schema(description = "Messaging limit tier reported by Meta", example = "LIMIT_10K")
    private MessagingLimitTier messagingLimitTier;

    @Schema(description = "Messaging throughput tier reported by Meta", example = "STANDARD")
    private MessagingThroughputTier throughputTier;

    @Schema(description = "Display name approval status reported by Meta", example = "APPROVED")
    private NameStatus nameStatus;

    @Schema(description = "Overall health reported by Meta", example = "GREEN")
    private HealthStatus healthStatus;

    @Schema(description = "Whether Meta has granted Official Business Account (OBA) status",
            example = "false")
    private Boolean officialBusinessAccount;

    @Schema(description = "Phone ownership (OTP) verification state", example = "VERIFIED")
    private CodeVerificationStatus verificationStatus;
}