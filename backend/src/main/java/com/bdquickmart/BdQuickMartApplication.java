package com.bdquickmart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Main entry point for the BD QuickMart backend.
 *
 * <p>Architecture: modular monolith. Feature code lives under
 * {@code com.bdquickmart.modules.<feature>} and is only reachable through the
 * API layer exposed under {@code /api/v1}.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class BdQuickMartApplication {

    public static void main(String[] args) {
        SpringApplication.run(BdQuickMartApplication.class, args);
    }
}
