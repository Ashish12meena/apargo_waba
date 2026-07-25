package com.apargo.waba.infrastructure.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code internal.api.*} — configuration for the service-to-service
 * surface under {@code /internal/**}.
 * <p>
 * Registered in {@link AppPropertiesConfig} alongside the other
 * {@code @ConfigurationProperties} classes; no class should read these
 * values via {@code @Value}.
 */
@Getter
@Setter
@Slf4j
@ConfigurationProperties(prefix = "internal.api")
public class InternalApiProperties {

    /**
     * Path prefix that the auth filter guards, and that the API gateway
     * must refuse to route from the public internet. Keeping every
     * internal endpoint under one prefix means the gateway needs exactly
     * one deny rule, rather than an allowlist that drifts as endpoints
     * are added.
     */
    private String pathPrefix = "/internal";

    /**
     * Shared secret every internal caller must present in
     * {@code X-Internal-Api-Key}.
     * <p>
     * There is deliberately no default. A default would ship a working
     * key in the jar, and this endpoint hands out credentials that grant
     * control of a customer's Meta Business Manager.
     */
    private String apiKey;

    /**
     * Escape hatch for local development and integration tests only.
     * When false the filter is bypassed entirely — never set this in an
     * environment holding real tokens.
     */
    private boolean authEnabled = true;

    /**
     * Fails fast at startup rather than at the first request. A service
     * that boots with a missing or trivially short key looks healthy in
     * Eureka while its credential endpoint is effectively unguarded.
     */
    @PostConstruct
    void validate() {
        if (!authEnabled) {
            log.warn("internal.api.auth-enabled=false — the {} surface is UNAUTHENTICATED. "
                    + "This must never be the case outside local development.", pathPrefix);
            return;
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "internal.api.api-key must be set (env INTERNAL_API_KEY) — refusing to start "
                            + "with an unguarded credential endpoint.");
        }
        if (apiKey.length() < 32) {
            throw new IllegalStateException(
                    "internal.api.api-key must be at least 32 characters of high-entropy secret.");
        }
    }
}