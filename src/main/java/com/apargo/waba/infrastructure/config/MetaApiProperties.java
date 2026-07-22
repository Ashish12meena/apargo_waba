package com.apargo.waba.infrastructure.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

/**
 * Central, typed configuration for everything the service needs to talk to
 * Meta's WhatsApp Cloud API (Graph API).
 *
 * <h2>Why this class exists</h2>
 *
 * Nothing in this service should ever hardcode a Meta app secret, a graph
 * API version, a webhook verify token, or a base URL. Every one of those
 * values is environment-specific (dev/stage/prod each talk to different
 * Meta apps) and several of them are secrets that must never appear in
 * source control.
 *
 * All of it is bound here, once, from {@code application.yaml} under the
 * {@code meta} prefix, and injected wherever it's needed via constructor
 * injection - never via scattered {@code @Value} annotations.
 *
 * <h2>Binding source</h2>
 *
 * See {@code meta:} block in {@code application.yaml}. Secrets
 * ({@code app-secret}, {@code token.encryption-key}) are expected to be
 * supplied via environment variables in every real environment; the yaml
 * only carries safe local-dev fallbacks (or blanks).
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "meta")
public class MetaApiProperties {

    /**
     * Graph API version this service targets, e.g. {@code v23.0}.
     *
     * <p>Kept centralized (not scattered across URL strings) because Meta
     * periodically deprecates/renames fields between versions - see the
     * {@code max_daily_conversation_per_phone} -> {@code
     * max_daily_conversations_per_business} rename effective from v24.0
     * noted in {@code waba-meta-docs-reference.md}. Changing this in one
     * place should be enough to move the whole service to a new version.
     */
    @NotBlank
    private String graphApiVersion;

    /**
     * Base URL for the Graph API, e.g. {@code https://graph.facebook.com}.
     * Separated from the version so tests can point at a mock server
     * without touching the version.
     */
    @NotBlank
    private String baseUrl;

    /** Meta App ID backing this integration (App Dashboard). */
    private String appId;

    /**
     * Meta App Secret.
     *
     * <p>Used as the HMAC-SHA256 key when verifying the
     * {@code X-Hub-Signature-256} header on every inbound webhook POST -
     * see {@code WabaWebhookUsecase#isValidSignature}. Must be sourced from
     * an environment variable / secret store in every real environment,
     * never committed as a literal.
     */
    @NotBlank
    private String appSecret;

    /**
     * Shared secret Meta echoes back during the one-time webhook GET
     * handshake ({@code hub.verify_token}) - see
     * {@code WabaWebhookUsecase#isValidVerificationRequest}.
     */
    @NotBlank
    private String webhookVerifyToken;

    /**
     * Embedded Signup configuration ID created in the App Dashboard.
     * Passed to the Meta JS SDK on the frontend and referenced when
     * resolving which onboarding flow variant was used.
     */
    private String configId;

    @NestedConfigurationProperty
    private final Oauth oauth = new Oauth();

    @NestedConfigurationProperty
    private final Token token = new Token();

    @NestedConfigurationProperty
    private final Http http = new Http();

    /**
     * OAuth-specific settings used during the {@code TOKEN_EXCHANGE} /
     * {@code TOKEN_EXTENSION} onboarding steps.
     */
    @Getter
    @Setter
    public static class Oauth {

        /** Redirect URI registered for the Embedded Signup OAuth flow. */
        private String redirectUri;

        /**
         * Comma-separated permission scopes requested during Embedded
         * Signup, bound as a {@code List<String>} for direct use when
         * building the authorization URL.
         *
         * Example: {@code whatsapp_business_management,
         * whatsapp_business_messaging, business_management}
         */
        private java.util.List<String> scopes = new java.util.ArrayList<>();
    }

    /**
     * Settings governing how {@link com.apargo.waba.domain.entity.MetaOAuthToken#accessToken}
     * is protected at rest.
     */
    @Getter
    @Setter
    public static class Token {

        /**
         * Symmetric encryption key used to encrypt/decrypt the access
         * token before persistence. Per the entity Javadoc, plaintext
         * tokens must never be persisted - this key is what makes that
         * possible. Must come from a secret store in production.
         *
         * <p>Must be a Base64-encoded 256-bit (32-byte) key - see
         * {@code AesGcmTokenCipherAdapter}, e.g. generated with
         * {@code openssl rand -base64 32}.
         */
        private String encryptionKey;
    }

    /**
     * HTTP client tuning for outbound Graph API calls.
     */
    @Getter
    @Setter
    public static class Http {

        /** Connect timeout in milliseconds. */
        private int connectTimeoutMs = 5000;

        /** Read/response timeout in milliseconds. */
        private int readTimeoutMs = 15000;
    }
}