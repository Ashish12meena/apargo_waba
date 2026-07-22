package com.apargo.waba.infrastructure.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

/**
 * Tunables for inbound Meta webhook processing.
 *
 * <p>Bound from the {@code webhook:} block in {@code application.yaml}.
 * Currently backs the async thread pool that
 * {@code WabaWebhookServiceImpl#processWebhookEvent} runs on - see
 * {@code AsyncConfig}. Meta expects a fast {@code 200 OK} on every POST and
 * retries with backoff for up to 7 days on non-200 responses, so the actual
 * event handling must never run on the request thread.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "webhook")
public class WebhookProperties {

    @NestedConfigurationProperty
    private final Async async = new Async();

    @Getter
    @Setter
    public static class Async {

        /** Minimum number of threads kept alive in the webhook executor pool. */
        @Min(1)
        private int corePoolSize = 4;

        /** Maximum number of threads the webhook executor pool may grow to. */
        @Min(1)
        private int maxPoolSize = 16;

        /** Bounded queue capacity before new tasks start rejecting/blocking. */
        @Min(0)
        private int queueCapacity = 500;

        /** Thread name prefix, useful for correlating log lines / thread dumps. */
        @NotBlank
        private String threadNamePrefix = "waba-webhook-";
    }
}