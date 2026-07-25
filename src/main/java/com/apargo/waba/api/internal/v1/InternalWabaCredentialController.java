package com.apargo.waba.api.internal.v1;

import com.apargo.waba.api.internal.InternalHeaders;
import com.apargo.waba.api.response.WabaCredentialResponse;
import com.apargo.waba.application.port.in.WabaCredentialUsecase;
import com.apargo.waba.infrastructure.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Service-to-service credential resolution. Not part of the public API.
 *
 * <h2>Shape</h2>
 *
 * <pre>
 * GET /internal/v1/waba-credentials/by-waba/{wabaId}
 * GET /internal/v1/waba-credentials/by-phone-number/{phoneNumberId}
 *
 * X-Internal-Api-Key: &lt;shared secret&gt;
 * X-Organization-Id:  1024
 * X-Project-Id:       55        (optional)
 * X-Internal-Caller:  messaging-service
 * X-Request-Id:       &lt;correlation id&gt;
 * </pre>
 *
 * The resource id sits in the path because it identifies what is being
 * fetched; the tenant sits in headers because it qualifies <em>who is
 * asking</em>. {@link InternalHeaders} sets out that reasoning in full.
 *
 * <h2>Why GET, and why the id is never a query parameter</h2>
 *
 * These are reads with no side effects, so GET is honest and lets callers
 * retry safely. The identifiers stay in the path rather than the query
 * string because query strings are written verbatim into access logs at
 * every proxy hop. The response is marked {@code no-store} for the same
 * class of reason: nothing between here and the caller should retain it.
 *
 * <h2>Visibility in Swagger</h2>
 *
 * Controlled by {@code springdoc.paths-to-match} in YAML: set it to
 * {@code /api/**} to hide these, leave it unset to show them. Nothing in
 * this class hides itself.
 *
 * <p>That is documentation hygiene, not security — the gateway deny rule
 * on {@code /internal/**} and {@code InternalApiAuthFilter} are what keep
 * outsiders out. An endpoint missing from Swagger still answers.
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/internal/v1/waba-credentials")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.INTERNAL_API_KEY_SCHEME)
public class InternalWabaCredentialController {

    private final WabaCredentialUsecase wabaCredentialUsecase;

    /**
     * Account-level credential — templates, WABA settings, anything
     * addressed at the WABA rather than at one number.
     */
    @GetMapping("/by-waba/{wabaId}")
    public ResponseEntity<WabaCredentialResponse> getByWabaId(
            @RequestHeader(InternalHeaders.ORGANIZATION_ID) @NotNull @Positive Long organizationId,
            @RequestHeader(value = InternalHeaders.PROJECT_ID, required = false) @Positive Long projectId,
            @RequestHeader(value = InternalHeaders.CALLER_SERVICE, required = false) String caller,
            @PathVariable @NotBlank String wabaId) {

        log.info("Internal credential request by wabaId={} organizationId={} projectId={} caller={}",
                wabaId, organizationId, projectId, caller);

        return noStore(wabaCredentialUsecase.resolveByWabaId(organizationId, projectId, wabaId));
    }

    /**
     * Send-path credential. Returns the token together with the
     * {@code phoneNumberId} for {@code POST /{phone-number-id}/messages},
     * so the messaging service needs one call rather than a lookup
     * followed by a credential fetch.
     */
    @GetMapping("/by-phone-number/{phoneNumberId}")
    public ResponseEntity<WabaCredentialResponse> getByPhoneNumberId(
            @RequestHeader(InternalHeaders.ORGANIZATION_ID) @NotNull @Positive Long organizationId,
            @RequestHeader(value = InternalHeaders.PROJECT_ID, required = false) @Positive Long projectId,
            @RequestHeader(value = InternalHeaders.CALLER_SERVICE, required = false) String caller,
            @PathVariable @NotBlank String phoneNumberId) {

        log.info("Internal credential request by phoneNumberId={} organizationId={} projectId={} caller={}",
                phoneNumberId, organizationId, projectId, caller);

        return noStore(wabaCredentialUsecase.resolveByPhoneNumberId(organizationId, projectId, phoneNumberId));
    }

    /**
     * {@code no-store} rather than {@code no-cache}: no-cache still permits
     * writing the response to disk as long as it is revalidated before
     * reuse, which is precisely what must not happen to a token.
     */
    private ResponseEntity<WabaCredentialResponse> noStore(WabaCredentialResponse body) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(body);
    }
}