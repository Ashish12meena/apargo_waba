package com.apargo.waba.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Single registration point for all {@code @ConfigurationProperties}
 * classes in this service.
 *
 * <h2>Why register them here instead of {@code @Component}?</h2>
 *
 * Keeping properties classes free of {@code @Component}/{@code @Configuration}
 * annotations means they stay plain, easily-constructed POJOs for unit
 * tests (e.g. {@code new MetaApiProperties()} with fields set directly),
 * while this single class is the one place that wires them into the Spring
 * context. Anyone auditing "what config does this service bind" only needs
 * to look here.
 *
 * <p>Add every new {@code @ConfigurationProperties} class to the
 * {@code @EnableConfigurationProperties} list below - do not scatter
 * {@code @Component} on the properties classes themselves.
 */
@Configuration
@EnableConfigurationProperties({
        MetaApiProperties.class,
        OnboardingProperties.class,
        UsageProperties.class,
        WebhookProperties.class
})
public class AppPropertiesConfig {
}