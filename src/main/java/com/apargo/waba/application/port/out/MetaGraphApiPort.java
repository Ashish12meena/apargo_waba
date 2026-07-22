package com.apargo.waba.application.port.out;

import com.fasterxml.jackson.databind.JsonNode;
import com.apargo.waba.common.exception.MetaGraphApiException;

import java.util.Map;

/**
 * Outbound port for all calls to Meta's WhatsApp Cloud API (Graph API).
 *
 * <h2>Why generic (path + JsonNode) instead of a method per endpoint</h2>
 *
 * The Graph API surface this service needs grows with almost every new
 * feature (token exchange, WABA resolution, phone sync, registration,
 * template management, ...), and Meta's response shapes are already
 * handled generically elsewhere in this codebase (see
 * {@code WebhookChange.value}, deliberately a raw {@link JsonNode} for the
 * same forward-compatibility reason). Modeling every Graph API response as
 * its own DTO up front would mean touching this port for every new call
 * site. Instead: this port does auth header injection, base
 * URL/version prefixing, timeout handling, and error normalization once;
 * callers (application services) parse the specific fields they need out
 * of the returned {@link JsonNode} and are free to introduce typed value
 * objects at the service layer when a call site stabilizes.
 *
 * <h2>Auth</h2>
 * The bearer token is passed explicitly per call - this port holds no
 * credential state. Callers are responsible for resolving the correct
 * token (see {@code MetaOAuthTokenRepositoryPort}) before calling.
 *
 * <h2>Versioning</h2>
 * Implementations prefix every {@code path} with the configured
 * {@code meta.graph-api-version} (see {@code MetaApiProperties}) - callers
 * pass paths like {@code "/{waba-id}/phone_numbers"}, never a full URL.
 */
public interface MetaGraphApiPort {

    /**
     * Performs a {@code GET} request against the Graph API.
     *
     * @param path        path relative to the versioned base URL, e.g. {@code "/{waba-id}/phone_numbers"}
     * @param accessToken bearer token to authenticate the call
     * @param queryParams query parameters to append (may be empty, never null)
     * @return the parsed JSON response body
     * @throws MetaGraphApiException on a non-2xx response, timeout, or unparseable body
     */
    JsonNode get(String path, String accessToken, Map<String, String> queryParams);

    /**
     * Performs a {@code POST} request against the Graph API with a JSON body.
     *
     * @param path        path relative to the versioned base URL
     * @param accessToken bearer token to authenticate the call
     * @param body        request body, serialized as JSON (may be a Map, record, or POJO)
     * @return the parsed JSON response body
     * @throws MetaGraphApiException on a non-2xx response, timeout, or unparseable body
     */
    JsonNode post(String path, String accessToken, Object body);

    /**
     * Performs a {@code POST} request against the Graph API using
     * form-encoded parameters instead of a JSON body - required by a
     * handful of Meta endpoints (notably OAuth token exchange).
     *
     * @param path        path relative to the versioned base URL
     * @param accessToken bearer token, may be {@code null} for endpoints
     *                    (like the initial token exchange) that
     *                    authenticate via form parameters instead
     * @param formParams  form fields to send
     * @return the parsed JSON response body
     * @throws MetaGraphApiException on a non-2xx response, timeout, or unparseable body
     */
    JsonNode postForm(String path, String accessToken, Map<String, String> formParams);

    /**
     * Performs a {@code DELETE} request against the Graph API.
     *
     * @param path        path relative to the versioned base URL
     * @param accessToken bearer token to authenticate the call
     * @return the parsed JSON response body
     * @throws MetaGraphApiException on a non-2xx response, timeout, or unparseable body
     */
    JsonNode delete(String path, String accessToken);
}