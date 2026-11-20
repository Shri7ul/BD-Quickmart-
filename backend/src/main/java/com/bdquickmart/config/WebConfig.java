package com.bdquickmart.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Web-layer CORS configuration.
 *
 * <p>Goal 02 consolidates CORS into a single {@link CorsConfigurationSource}
 * bean that the Spring Security filter chain consumes. The previous MVC
 * {@code addCorsMappings} approach could not attach CORS headers to responses
 * produced by the security filter chain (401/403 from the entry point / denied
 * handler), which broke browser clients. Registering for {@code /**} covers
 * both MVC responses and filter-level responses.
 */
@Configuration
public class WebConfig {

    /**
     * Explicit origins only: credentials are enabled, so a wildcard origin is
     * invalid. The production admin origin is part of the default so a fresh
     * deployment allows it even before APP_CORS_ALLOWED_ORIGINS is set. Keep
     * this default in sync with {@code app.cors.allowed-origins} in
     * application.yml.
     */
    @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:3000,https://bdquickmart.vercel.app}")
    private List<String> allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("X-Trace-Id"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
