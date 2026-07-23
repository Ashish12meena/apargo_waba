package com.apargo.waba.infrastructure.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Wires {@link CorsProperties} into an actual CORS configuration.
 * <p>
 * {@code CorsProperties} being bound via {@code @ConfigurationProperties}
 * is not enough on its own - Spring MVC does not apply any CORS handling
 * unless a {@link CorsConfigurationSource} (or a {@code WebMvcConfigurer}
 * that registers CORS mappings) is actually present in the context. Without
 * this class, every non-"simple" cross-origin request (POST, PUT, PATCH,
 * DELETE, or GET with custom headers) fails its preflight {@code OPTIONS}
 * check and gets blocked by the browser - only plain GET requests (which
 * don't require a preflight) appear to work.
 */
@Configuration
@RequiredArgsConstructor
public class CorsConfig implements WebMvcConfigurer {

    private final CorsProperties corsProperties;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // setAllowedOriginPatterns supports "*" and wildcard subdomains
        // (e.g. "https://*.example.com") safely even when allowCredentials
        // is true - unlike setAllowedOrigins, which Spring rejects at
        // request time if allowCredentials=true and any entry is "*".
        configuration.setAllowedOriginPatterns(corsProperties.getAllowedOrigins());
        configuration.setAllowedMethods(corsProperties.getAllowedMethods());
        configuration.setAllowedHeaders(corsProperties.getAllowedHeaders());
        configuration.setExposedHeaders(corsProperties.getExposedHeaders());
        configuration.setAllowCredentials(corsProperties.isAllowCredentials());
        configuration.setMaxAge(corsProperties.getMaxAgeSeconds());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}