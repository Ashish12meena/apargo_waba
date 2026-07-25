package com.apargo.waba.application.mapper;

import com.apargo.waba.api.request.CreateMetaOAuthTokenRequest;
import com.apargo.waba.api.request.UpdateMetaOAuthTokenRequest;
import com.apargo.waba.api.response.MetaOAuthTokenResponse;
import com.apargo.waba.domain.entity.MetaOAuthToken;
import com.apargo.waba.domain.enums.MetaTokenType;
import org.springframework.stereotype.Component;

/**
 * Structural mapping only — encryption/decryption happens in
 * {@code MetaOAuthTokenServiceImpl}, not here. The ciphertext/plaintext
 * is passed in already converted, so this class never needs
 * {@code TokenCipherPort}.
 */
@Component
public class MetaOAuthTokenMapper {

    public MetaOAuthTokenResponse toResponse(MetaOAuthToken token, String decryptedAccessToken) {
        if (token == null) {
            return null;
        }
        return MetaOAuthTokenResponse.builder()
                .id(token.getId())
                .organizationId(token.getOrganizationId())
                .accessToken(decryptedAccessToken)
                .tokenType(token.getTokenType())
                .expiresAt(token.getExpiresAt())
                .metaUserId(token.getMetaUserId())
                .systemUserId(token.getSystemUserId())
                .businessManagerId(token.getBusinessManagerId())
                .grantedScopes(token.getGrantedScopes())
                .grantedAt(token.getGrantedAt())
                .createdAt(token.getCreatedAt())
                .updatedAt(token.getUpdatedAt())
                .build();
    }

    public MetaOAuthToken toEntity(CreateMetaOAuthTokenRequest request, String encryptedAccessToken) {
        return MetaOAuthToken.builder()
                .organizationId(request.getOrganizationId())
                .accessToken(encryptedAccessToken)
                .tokenType(request.getTokenType() != null ? request.getTokenType() : MetaTokenType.USER_TOKEN)
                .expiresAt(request.getExpiresAt())
                .metaUserId(request.getMetaUserId())
                .systemUserId(request.getSystemUserId())
                .businessManagerId(request.getBusinessManagerId())
                .grantedScopes(request.getGrantedScopes())
                .grantedAt(request.getGrantedAt())
                .build();
    }

    /** newEncryptedAccessToken is null when the caller didn't request a token change. */
    public void applyUpdate(MetaOAuthToken target, UpdateMetaOAuthTokenRequest request, String newEncryptedAccessToken) {
        if (newEncryptedAccessToken != null) {
            target.setAccessToken(newEncryptedAccessToken);
        }
        if (request.getTokenType() != null) {
            target.setTokenType(request.getTokenType());
        }
        if (request.getExpiresAt() != null) {
            target.setExpiresAt(request.getExpiresAt());
        }
        if (request.getMetaUserId() != null) {
            target.setMetaUserId(request.getMetaUserId());
        }
        if (request.getSystemUserId() != null) {
            target.setSystemUserId(request.getSystemUserId());
        }
        if (request.getBusinessManagerId() != null) {
            target.setBusinessManagerId(request.getBusinessManagerId());
        }
        if (request.getGrantedScopes() != null) {
            target.setGrantedScopes(request.getGrantedScopes());
        }
        if (request.getGrantedAt() != null) {
            target.setGrantedAt(request.getGrantedAt());
        }
    }
}