package com.apargo.waba.api.internal.v1;

import com.apargo.waba.api.request.CreateMetaOAuthTokenRequest;
import com.apargo.waba.api.request.UpdateMetaOAuthTokenRequest;
import com.apargo.waba.api.response.MetaOAuthTokenResponse;
import com.apargo.waba.application.port.in.MetaOAuthTokenUsecase;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * CRUD for {@code MetaOAuthToken}. Under {@code /internal/**} — guarded by
 * {@code InternalApiAuthFilter}, same as the credential-resolution
 * endpoints — because GET here returns a decrypted access token.
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/internal/v1/meta-oauth-tokens")
@RequiredArgsConstructor
@Tag(name = "Meta OAuth Tokens (internal)")
public class InternalMetaOAuthTokenController {

    private final MetaOAuthTokenUsecase metaOAuthTokenUsecase;

    @PostMapping
    public ResponseEntity<MetaOAuthTokenResponse> create(@Valid @RequestBody CreateMetaOAuthTokenRequest request) {
        log.info("POST /internal/v1/meta-oauth-tokens organizationId={}", request.getOrganizationId());
        return noStore(metaOAuthTokenUsecase.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MetaOAuthTokenResponse> getById(@PathVariable @NotNull @Positive Long id) {
        return noStore(metaOAuthTokenUsecase.getById(id));
    }

    @GetMapping("/by-organization/{organizationId}")
    public ResponseEntity<MetaOAuthTokenResponse> getByOrganizationId(
            @PathVariable @NotNull @Positive Long organizationId) {
        return noStore(metaOAuthTokenUsecase.getByOrganizationId(organizationId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MetaOAuthTokenResponse> update(
            @PathVariable @NotNull @Positive Long id,
            @Valid @RequestBody UpdateMetaOAuthTokenRequest request) {
        return noStore(metaOAuthTokenUsecase.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable @NotNull @Positive Long id) {
        metaOAuthTokenUsecase.delete(id);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<MetaOAuthTokenResponse> noStore(MetaOAuthTokenResponse body) {
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store").body(body);
    }
}