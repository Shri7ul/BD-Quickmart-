package com.bdquickmart.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT configuration bound from {@code app.jwt.*} environment variables.
 *
 * <p>Kept as a dedicated record (rather than extending {@link AppProperties})
 * so that a missing group can never leave an unrelated {@code null} component
 * that causes a startup NPE.
 *
 * @param secret                Base64 or raw HS256 secret (>= 32 bytes)
 * @param issuer                value written into the {@code iss} claim
 * @param accessExpiration      access token lifetime in seconds
 * @param refreshExpiration     refresh token lifetime in seconds
 * @param allowEphemeralDevKey  permit an ephemeral key when no secret is set
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String secret,
        String issuer,
        long accessExpiration,
        long refreshExpiration,
        boolean allowEphemeralDevKey) {
}
