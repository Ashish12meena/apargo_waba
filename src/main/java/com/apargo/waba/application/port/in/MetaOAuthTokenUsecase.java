package com.apargo.waba.application.port.in;

import com.apargo.waba.api.request.CreateMetaOAuthTokenRequest;
import com.apargo.waba.api.request.UpdateMetaOAuthTokenRequest;
import com.apargo.waba.api.response.MetaOAuthTokenResponse;

public interface MetaOAuthTokenUsecase {

    /** @throws com.apargo.waba.common.exception.DuplicateResourceException if the org already has a token */
    MetaOAuthTokenResponse create(CreateMetaOAuthTokenRequest request);

    MetaOAuthTokenResponse getById(Long id);

    /** One row per org (uq_meta_oauth_tokens_org). */
    MetaOAuthTokenResponse getByOrganizationId(Long organizationId);

    MetaOAuthTokenResponse update(Long id, UpdateMetaOAuthTokenRequest request);

    void delete(Long id);
}