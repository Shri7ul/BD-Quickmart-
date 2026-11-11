package com.bdquickmart.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Opt-in first-administrator bootstrap settings bound from
 * {@code app.bootstrap.admin.*} environment variables.
 *
 * @param enabled   master switch; disabled by default
 * @param email     login email for the first SUPER_ADMIN
 * @param password  plaintext password, hashed immediately and never logged
 * @param fullName  display name
 * @param phone     unique phone number
 */
@ConfigurationProperties(prefix = "app.bootstrap.admin")
public record BootstrapProperties(
        boolean enabled,
        String email,
        String password,
        String fullName,
        String phone) {

    /** @return true when any required field is missing while enabled. */
    public boolean hasMissingRequiredFields() {
        return isBlank(email) || isBlank(password) || isBlank(fullName) || isBlank(phone);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
