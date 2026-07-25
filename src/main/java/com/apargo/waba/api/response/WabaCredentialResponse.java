package com.apargo.waba.api.response;

import com.apargo.waba.domain.enums.MetaTokenType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * A resolved Meta credential, returned only on the internal
 * ({@code /internal/**}) surface.
 *
 * <h2>Handling rules for callers</h2>
 *
 * <ul>
 *   <li>Hold {@code accessToken} in memory for the duration of the call.
 *       Do not write it to a database, a cache with disk persistence, a
 *       log line, or an error report.</li>
 *   <li>Re-fetch rather than store long-term. A token can be revoked or
 *       rotated at any time by the customer or by Meta; a copy held
 *       elsewhere goes stale silently and turns into a second place that
 *       has to be cleaned up on offboarding.</li>
 *   <li>If short-lived caching is unavoidable, key it by
 *       {@code phoneNumberId} and bound it well under {@code expiresAt}.</li>
 * </ul>
 *
 * <p>There is deliberately no Lombok {@code @ToString}: the default
 * {@link Object#toString()} is overridden below to redact the token, so an
 * accidental {@code log.info("{}", credential)} cannot leak it.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Resolved Meta access token and the identifiers needed to call the Graph API")
public class WabaCredentialResponse {

    @Schema(description = "Organization that owns the credential", example = "1024")
    private Long organizationId;

    @Schema(description = "Internal id of the WABA account", example = "42")
    private Long wabaAccountId;

    @Schema(description = "Meta's globally unique WABA id", example = "123456789012345")
    private String wabaId;

    @Schema(description = "Meta Phone Number ID — populated only when the credential was "
            + "resolved by phone number. Use it directly as POST /{phone-number-id}/messages.",
            example = "108745612345678")
    private String phoneNumberId;

    @Schema(description = "Display number, for logging and support only", example = "+91 9876543210")
    private String displayPhoneNumber;

    @Schema(description = "Decrypted Meta access token — treat as a secret")
    private String accessToken;

    @Schema(description = "USER_TOKEN expires and must be refreshed; SYSTEM_USER is permanent")
    private MetaTokenType tokenType;

    @Schema(description = "Token expiry, null for permanent system-user tokens")
    private Instant expiresAt;

    /**
     * Redacted on purpose — see the class Javadoc. The token is the one
     * field on this object that must never reach a log.
     */
    @Override
    public String toString() {
        return "WabaCredentialResponse(organizationId=" + organizationId
                + ", wabaAccountId=" + wabaAccountId
                + ", wabaId=" + wabaId
                + ", phoneNumberId=" + phoneNumberId
                + ", tokenType=" + tokenType
                + ", expiresAt=" + expiresAt
                + ", accessToken=***REDACTED***)";
    }
}