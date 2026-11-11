package com.bdquickmart.config;

import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Base64;
import java.util.Date;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

/**
 * Builds the singletons used to sign and verify JWTs.
 *
 * <p>The signing key is never hardcoded. A configured secret is validated at
 * startup; a missing secret is fatal in production but tolerated in test/dev
 * profiles (or when explicitly allowed) via an ephemeral key.
 */
@Configuration
public class JwtConfig {

    private static final Logger log = LoggerFactory.getLogger(JwtConfig.class);
    private static final int MIN_KEY_BYTES = 32;

    @Bean
    public SecretKey jwtSecretKey(JwtProperties props, Environment env) {
        String raw = props.secret();
        if (raw != null && !raw.isBlank()) {
            return Keys.hmacShaKeyFor(decodeSecret(raw));
        }

        boolean ephemeralAllowed = props.allowEphemeralDevKey()
                || env.acceptsProfiles(Profiles.of("test", "dev"));
        if (!ephemeralAllowed) {
            throw new IllegalStateException(
                    "JWT_SECRET is not configured. Set a >=32-byte secret, or set "
                            + "JWT_ALLOW_EPHEMERAL_DEV_KEY=true for local development only.");
        }

        log.warn("JWT secret is not configured: generating an EPHEMERAL HS256 key. "
                + "ALL TOKENS INVALIDATE ON RESTART. Never use this in production.");
        return Jwts.SIG.HS256.key().build();
    }

    /** Bridges the injectable {@link Clock} into jjwt's expiry validation. */
    @Bean
    public io.jsonwebtoken.Clock jwtParserClock(Clock clock) {
        return () -> Date.from(clock.instant());
    }

    @Bean
    public JwtParser jwtParser(SecretKey jwtSecretKey, io.jsonwebtoken.Clock jwtParserClock) {
        return Jwts.parser()
                .verifyWith(jwtSecretKey)
                .clock(jwtParserClock)
                .build();
    }

    /**
     * Accepts either a Base64-encoded secret or a raw passphrase of at least
     * {@value #MIN_KEY_BYTES} bytes. Base64 is preferred when it produces a
     * usable key, otherwise the raw UTF-8 bytes are used.
     */
    private byte[] decodeSecret(String raw) {
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(raw);
        } catch (IllegalArgumentException ex) {
            decoded = new byte[0];
        }
        if (decoded.length >= MIN_KEY_BYTES) {
            return decoded;
        }
        byte[] rawBytes = raw.getBytes(StandardCharsets.UTF_8);
        if (rawBytes.length >= MIN_KEY_BYTES) {
            return rawBytes;
        }
        throw new IllegalStateException("JWT_SECRET must be at least "
                + MIN_KEY_BYTES + " bytes when decoded (found " + rawBytes.length + ")");
    }
}
