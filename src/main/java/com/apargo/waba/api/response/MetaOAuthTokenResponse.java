package com.apargo.waba.api.response;

import com.apargo.waba.domain.enums.MetaTokenType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "A Meta OAuth token — internal only")
public class MetaOAuthTokenResponse {

    private Long id;
    private Long organizationId;

    @Schema(description = "Decrypted — treat as a secret")
    private String accessToken;

    private MetaTokenType tokenType;
    private Instant expiresAt;
    private String metaUserId;
    private String systemUserId;
    private String businessManagerId;
    private String grantedScopes;
    private Instant grantedAt;
    private Instant createdAt;
    private Instant updatedAt;

    /** Redacted so an accidental log.info("{}", response) doesn't leak the token. */
    @Override
    public String toString() {
        return "MetaOAuthTokenResponse(id=" + id + ", organizationId=" + organizationId
                + ", tokenType=" + tokenType + ", expiresAt=" + expiresAt
                + ", accessToken=***REDACTED***)";
    }
}