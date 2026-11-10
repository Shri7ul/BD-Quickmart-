package com.bdquickmart.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Application-level configuration bound from environment variables.
 *
 * <p>Prefix: {@code app.}. Values are overridable via
 * {@code APP_*} environment variables, for example {@code APP_API_VERSION=v1}.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Api api,
        Cors cors,
        Ai ai) {

    public record Api(String version, String basePath) {
    }

    public record Cors(List<String> allowedOrigins) {
    }

    public record Ai(String geminiApiKey, String groqApiKey, String chatModel,
                     String embeddingModel, boolean enabled) {
    }
}
