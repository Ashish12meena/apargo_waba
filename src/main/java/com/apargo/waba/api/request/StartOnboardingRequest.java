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
    @Schema(description = "Client-supplied idempotency key — replaying the same key returns the existing task instead of starting a duplicate onboarding", requiredMode = Schema.RequiredMode.REQUIRED)
    private String idempotencyKey;
}