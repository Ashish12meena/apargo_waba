package com.apargo.waba.infrastructure.client;

import com.apargo.waba.application.port.out.MetaGraphApiPort;
import com.apargo.waba.common.exception.MetaGraphApiException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

/**
 * Implements {@link MetaGraphApiPort} on top of a Spring {@link RestClient}.
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *     <li>Attach {@code Authorization: Bearer <token>} when a token is supplied.</li>
 *     <li>Append query parameters / form fields.</li>
 *     <li>Normalize every failure mode (non-2xx, timeout, unparseable body)
 *         into a single {@link MetaGraphApiException} so callers never need
 *         to catch Spring's HTTP client exceptions directly.</li>
 *     <li>Never log request/response bodies at a level enabled in
 *         production - Graph API payloads routinely include access tokens
 *         and PII (phone numbers). Only status codes and paths are logged
 *         at INFO; bodies only appear at DEBUG, matching the "never log
 *         tokens" rule already called out on {@code MetaOAuthToken}.</li>
 * </ul>
 *
 * <p>No retry/circuit-breaker logic here by design - see
 * {@link MetaGraphApiException#isRetryable()}, which lets a future
 * resilience layer (e.g. Resilience4j, currently not a dependency of this
 * service) decide what to do with a failure without this adapter needing
 * to change.
 */
@Slf4j
@org.springframework.stereotype.Component
public class MetaGraphApiAdapter implements MetaGraphApiPort {

    private final RestClient restClient;

    public MetaGraphApiAdapter(RestClient metaGraphApiRestClient) {
        this.restClient = metaGraphApiRestClient;
    }

    @Override
    public JsonNode get(String path, String accessToken, Map<String, String> queryParams) {
        log.info("Meta Graph API GET {}", path);
        try {
            return restClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder.path(path);
                        if (queryParams != null) {
                            queryParams.forEach(builder::queryParam);
                        }
                        return builder.build();
                    })
                    .headers(headers -> applyBearerToken(headers, accessToken))
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException e) {
            throw toGraphApiException("GET", path, e);
        } catch (RestClientException e) {
            throw new MetaGraphApiException("Meta Graph API GET " + path + " failed", e);
        }
    }

    @Override
    public JsonNode post(String path, String accessToken, Object body) {
        log.info("Meta Graph API POST {}", path);
        try {
            return restClient.post()
                    .uri(path)
                    .headers(headers -> {
                        applyBearerToken(headers, accessToken);
                        headers.setContentType(MediaType.APPLICATION_JSON);
                    })
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException e) {
            throw toGraphApiException("POST", path, e);
        } catch (RestClientException e) {
            throw new MetaGraphApiException("Meta Graph API POST " + path + " failed", e);
        }
    }

    @Override
    public JsonNode postForm(String path, String accessToken, Map<String, String> formParams) {
        log.info("Meta Graph API POST (form) {}", path);
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        if (formParams != null) {
            formParams.forEach(body::add);
        }
        try {
            return restClient.post()
                    .uri(path)
                    .headers(headers -> {
                        applyBearerToken(headers, accessToken);
                        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                    })
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException e) {
            throw toGraphApiException("POST(form)", path, e);
        } catch (RestClientException e) {
            throw new MetaGraphApiException("Meta Graph API POST(form) " + path + " failed", e);
        }
    }

    @Override
    public JsonNode delete(String path, String accessToken) {
        log.info("Meta Graph API DELETE {}", path);
        try {
            return restClient.delete()
                    .uri(path)
                    .headers(headers -> applyBearerToken(headers, accessToken))
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException e) {
            throw toGraphApiException("DELETE", path, e);
        } catch (RestClientException e) {
            throw new MetaGraphApiException("Meta Graph API DELETE " + path + " failed", e);
        }
    }

    private void applyBearerToken(HttpHeaders headers, String accessToken) {
        if (accessToken != null && !accessToken.isBlank()) {
            headers.setBearerAuth(accessToken);
        }
    }

    private MetaGraphApiException toGraphApiException(String method, String path, RestClientResponseException e) {
        String responseBody = e.getResponseBodyAsString();
        log.warn("Meta Graph API {} {} failed with status={}", method, path, e.getStatusCode().value());
        log.debug("Meta Graph API error body: {}", responseBody);
        return new MetaGraphApiException(
                "Meta Graph API " + method + " " + path + " returned " + e.getStatusCode().value(),
                e.getStatusCode().value(),
                responseBody);
    }
}