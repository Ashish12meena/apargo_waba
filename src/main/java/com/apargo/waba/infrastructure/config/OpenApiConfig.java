package com.apargo.waba.infrastructure.config;

import com.apargo.waba.api.internal.InternalHeaders;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger wiring.
 * <p>
 * Which paths appear in the docs is controlled entirely by
 * {@code springdoc.paths-to-match} in YAML — nothing here filters anything.
 */
@Configuration
public class OpenApiConfig {

    /** Referenced by {@code @SecurityRequirement} on the internal controller. */
    public static final String INTERNAL_API_KEY_SCHEME = "internal-api-key";

    @Bean
    public OpenAPI wabaOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("WABA Service API")
                        .description("Meta credential and WhatsApp Business Account management "
                                + "for Apargo/Aigreentick modules")
                        .version("v1")
                        .contact(new Contact().name("Apargo Platform Team")))
                .components(new Components()
                        .addSecuritySchemes(INTERNAL_API_KEY_SCHEME, internalApiKeyScheme()));
    }

    /**
     * Declares {@code X-Internal-Api-Key} so Swagger UI shows an Authorize
     * button for it. The key is checked by a servlet filter, not bound as a
     * controller argument, so springdoc cannot infer it — without this,
     * "Try it out" on the internal endpoints returns 401 with no hint why.
     */
    private SecurityScheme internalApiKeyScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name(InternalHeaders.API_KEY)
                .description("Value of INTERNAL_API_KEY on the server.");
    }
}