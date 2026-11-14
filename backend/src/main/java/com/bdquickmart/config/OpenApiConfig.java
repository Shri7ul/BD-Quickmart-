package com.bdquickmart.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Springdoc / OpenAPI configuration for the versioned REST API.
 */
@Configuration
public class OpenApiConfig {

    @Value("${app.api.version:v1}")
    private String apiVersion;

    /** Public base URL of the deployed backend (Render), injected via env. */
    @Value("${app.public-url:http://localhost:8080}")
    private String publicUrl;

    @Bean
    public OpenAPI bdQuickMartOpenAPI() {
        final String schemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("BD QuickMart API")
                        .version(apiVersion)
                        .description("""
                                Backend for the BD QuickMart hyperlocal quick-commerce platform.

                                Serves three client applications: the Customer mobile app, the Rider
                                mobile app, and the Inventory & Admin web application.

                                All business endpoints are versioned under /api/v1.
                                Authentication uses Bearer JWT tokens (issued from Goal 02).
                                """)
                        .contact(new Contact()
                                .name("BD QuickMart Engineering")
                                .email("engineering@bdquickmart.com"))
                        .license(new License().name("Proprietary")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local development"),
                        new Server().url(publicUrl).description("Deployed (APP_PUBLIC_URL)")))
                .components(new Components().addSecuritySchemes(schemeName,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT access token issued by the auth module")))
                .addSecurityItem(new SecurityRequirement().addList(schemeName));
    }
}
