package com.apargo.waba.application.usecase;

import com.apargo.waba.api.request.CreateMetaOAuthTokenRequest;
import com.apargo.waba.api.request.UpdateMetaOAuthTokenRequest;
import com.apargo.waba.api.response.MetaOAuthTokenResponse;
import com.apargo.waba.application.mapper.MetaOAuthTokenMapper;
import com.apargo.waba.application.port.in.MetaOAuthTokenUsecase;
import com.apargo.waba.application.port.out.MetaOAuthTokenRepositoryPort;
import com.apargo.waba.application.port.out.TokenCipherPort;
import com.apargo.waba.common.exception.DuplicateResourceException;
import com.apargo.waba.common.exception.ResourceNotFoundException;
import com.apargo.waba.domain.entity.MetaOAuthToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Encrypts on write, decrypts on read. Nothing here logs the token itself. */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetaOAuthTokenServiceImpl implements MetaOAuthTokenUsecase {

    private final MetaOAuthTokenRepositoryPort repositoryPort;
    private final TokenCipherPort tokenCipherPort;
    private final MetaOAuthTokenMapper mapper;

    @Override
    @Transactional
    public MetaOAuthTokenResponse create(CreateMetaOAuthTokenRequest request) {
        log.info("Creating meta oauth token organizationId={}", request.getOrganizationId());

        if (repositoryPort.existsByOrganizationId(request.getOrganizationId())) {
            throw new DuplicateResourceException(
                    "A token already exists for organizationId=" + request.getOrganizationId());
        }

        String encrypted = tokenCipherPort.encrypt(request.getAccessToken());
        MetaOAuthToken saved = repositoryPort.save(mapper.toEntity(request, encrypted));

        log.info("Created meta oauth token id={} organizationId={}", saved.getId(), saved.getOrganizationId());
        return mapper.toResponse(saved, request.getAccessToken());
    }

    @Override
    @Transactional(readOnly = true)
    public MetaOAuthTokenResponse getById(Long id) {
        MetaOAuthToken token = requireToken(id);
        return mapper.toResponse(token, tokenCipherPort.decrypt(token.getAccessToken()));
    }

    @Override
    @Transactional(readOnly = true)
    public MetaOAuthTokenResponse getByOrganizationId(Long organizationId) {
        MetaOAuthToken token = repositoryPort.findByOrganizationId(organizationId).stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No token found for organizationId=" + organizationId));

        return mapper.toResponse(token, tokenCipherPort.decrypt(token.getAccessToken()));
    }

    @Override
    @Transactional
    public MetaOAuthTokenResponse update(Long id, UpdateMetaOAuthTokenRequest request) {
        MetaOAuthToken token = requireToken(id);

        String newEncrypted = request.getAccessToken() != null
                ? tokenCipherPort.encrypt(request.getAccessToken())
                : null;

        mapper.applyUpdate(token, request, newEncrypted);
        MetaOAuthToken saved = repositoryPort.save(token);

        log.info("Updated meta oauth token id={}", saved.getId());
        return mapper.toResponse(saved, tokenCipherPort.decrypt(saved.getAccessToken()));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        MetaOAuthToken token = requireToken(id);
        token.markDeleted();
        repositoryPort.save(token);
        log.info("Soft deleted meta oauth token id={}", id);
    }

    private MetaOAuthToken requireToken(Long id) {
        return repositoryPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Token not found for id=" + id));
    }
}