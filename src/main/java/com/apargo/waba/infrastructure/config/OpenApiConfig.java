package com.apargo.waba.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI auditLogOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Waba  Service API")
                        .description("Meta Credentail Management service for  Apargo/Aigreentick modules ")
                        .version("v1")
                        .contact(new Contact().name("Apargo Platform Team")));
    }
}
