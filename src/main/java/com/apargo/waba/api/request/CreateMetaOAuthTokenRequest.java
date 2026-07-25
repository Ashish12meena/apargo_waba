package com.apargo.waba.api.request;

import com.apargo.waba.domain.enums.MetaTokenType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** One WABA org has at most one token row (uq_meta_oauth_tokens_org). */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Stores a Meta access token for an organization. Plaintext in, encrypted at rest.")
public class CreateMetaOAuthTokenRequest {

    @NotNull
    @Positive
    private Long organizationId;

    @NotBlank
    @Schema(description = "Plaintext Meta access token — encrypted before storage")
    private String accessToken;

    @Schema(description = "Defaults to USER_TOKEN")
    private MetaTokenType tokenType;

    private Instant expiresAt;
    private String metaUserId;
    private String systemUserId;
    private String businessManagerId;
    private String grantedScopes;
    private Instant grantedAt;
}