package com.apargo.waba.api.request;

import com.apargo.waba.domain.enums.MetaTokenType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** Partial update — null fields are left unchanged. organizationId is immutable. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Partial update of a Meta OAuth token; null fields are left unchanged")
public class UpdateMetaOAuthTokenRequest {

    @Schema(description = "Plaintext replacement token — re-encrypted before storage. Omit to leave unchanged.")
    private String accessToken;

    private MetaTokenType tokenType;
    private Instant expiresAt;
    private String metaUserId;
    private String systemUserId;
    private String businessManagerId;
    private String grantedScopes;
    private Instant grantedAt;
}