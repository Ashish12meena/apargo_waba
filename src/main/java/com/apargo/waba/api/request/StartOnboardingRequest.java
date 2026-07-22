package com.apargo.waba.api.request;

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
 * Request body to start a new Embedded Signup onboarding attempt.
 * <p>
 * Maps to the creation of an {@link com.apargo.waba.domain.entity.OnboardingTask}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Starts a new WABA onboarding (Embedded Signup) workflow")
public class StartOnboardingRequest {

    @NotNull
    @Positive
    @Schema(description = "Internal organization id requesting onboarding", example = "1024", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long organizationId;

    @Positive
    @Schema(description = "Project that initiated onboarding, used for automatic WABA assignment on completion", example = "55")
    private Long projectId;

    @NotBlank
    @Size(max = 500)
    @Schema(description = "OAuth authorization code returned by Meta's Embedded Signup flow", requiredMode = Schema.RequiredMode.REQUIRED)
    private String oauthCode;

    @NotBlank
    @Size(max = 200)
    @Schema(description = "Client-supplied idempotency key — replaying the same key with the same "
            + "oauthCode returns the existing task instead of starting a duplicate onboarding. "
            + "Generate once per genuine user action (e.g. on button click) and reuse it if retrying "
            + "after a network failure, so a retry is safely deduplicated instead of creating a "
            + "duplicate task.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String idempotencyKey;

    @Size(max = 100)
    @Schema(description = "Optional — Meta WABA ID, returned directly to the frontend by the Embedded "
            + "Signup JS SDK's postMessage 'FINISH' event (event.data.waba_id). If supplied, "
            + "WABA_RESOLUTION is skipped and this value is used as-is; if omitted, the backend "
            + "resolves it via the Graph API.")
    private String wabaId;

    @Size(max = 100)
    @Schema(description = "Optional — Meta Phone Number ID, returned directly to the frontend by the "
            + "Embedded Signup JS SDK's postMessage 'FINISH' event (event.data.phone_number_id). If "
            + "supplied, PHONE_NUMBER_RESOLUTION is skipped and this value is used as-is; if omitted, "
            + "the backend resolves it via the Graph API.")
    private String phoneNumberId;
}