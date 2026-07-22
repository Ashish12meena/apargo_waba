package com.apargo.waba.infrastructure.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Builds the {@link RestClient} used to talk to Meta's Graph API.
 *
 * <p>Timeouts and the base URL are entirely driven by
 * {@link MetaApiProperties} (bound from {@code meta.*} in
 * {@code application.yaml}) - nothing here is hardcoded. The base URL
 * already includes the configured API version
 * (e.g. {@code https://graph.facebook.com/v23.0}), so
 * {@code MetaGraphApiAdapter} only ever supplies the resource path.
 *
 * <p>Uses {@link RestClient} (available on the classpath via
 * {@code spring-boot-starter-web} since Spring Framework 6.1 / Boot 3.2)
 * rather than {@code WebClient}, since this service has no other reactive
 * workload and {@code spring-webflux} is intentionally not a dependency
 * here.
 */
@Configuration
@RequiredArgsConstructor
public class MetaGraphApiClientConfig {

    private static final String REST_CLIENT_BEAN_NAME = "metaGraphApiRestClient";

    private final MetaApiProperties metaApiProperties;

    @Bean(name = REST_CLIENT_BEAN_NAME)
    public RestClient metaGraphApiRestClient() {
        MetaApiProperties.Http http = metaApiProperties.getHttp();

        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(Duration.ofMillis(http.getConnectTimeoutMs()))
                .withReadTimeout(Duration.ofMillis(http.getReadTimeoutMs()));

        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactoryBuilder.detect().build(settings);

        String baseUrl = metaApiProperties.getBaseUrl() + "/" + metaApiProperties.getGraphApiVersion();

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}