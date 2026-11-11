package com.bdquickmart.modules.auth;

import com.bdquickmart.config.JwtProperties;
import com.bdquickmart.modules.user.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/**
 * Issues and verifies HS256 JWTs, and hashes refresh tokens for storage.
 *
 * <p>Claims: {@code sub} = public id, {@code iss}, {@code iat}, {@code exp},
 * {@code jti}, plus private {@code purpose} ({@code access}|{@code refresh}),
 * {@code role}, and {@code familyId} on refresh tokens. Timestamps come from
 * the injectable {@link Clock} so expiry can be tested deterministically.
 */
@Service
public class JwtService {

    private static final String PURPOSE_ACCESS = "access";
    private static final String PURPOSE_REFRESH = "refresh";

    private final JwtProperties props;
    private final SecretKey key;
    private final Clock clock;
    private final JwtParser parser;

    public JwtService(JwtProperties props, SecretKey key, Clock clock, JwtParser parser) {
        this.props = props;
        this.key = key;
        this.clock = clock;
        this.parser = parser;
    }

    public String generateAccessToken(String publicId, UserRole role) {
        return build(publicId, role, PURPOSE_ACCESS, null, props.accessExpiration());
    }

    public String generateRefreshToken(String publicId, UserRole role, String familyId) {
        return build(publicId, role, PURPOSE_REFRESH, familyId, props.refreshExpiration());
    }

    private String build(String publicId, UserRole role, String purpose,
                         String familyId, long ttlSeconds) {
        Instant now = clock.instant();
        JwtBuilder builder = Jwts.builder()
                .subject(publicId)
                .issuer(props.issuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttlSeconds)))
                .id(UUID.randomUUID().toString())
                .claim("purpose", purpose)
                .claim("role", role.name())
                .signWith(key);
        if (familyId != null) {
            builder.claim("familyId", familyId);
        }
        return builder.compact();
    }

    /**
     * Parses and cryptographically validates a token. Expiry is evaluated
     * against the injectable clock. Throws a jjwt {@code JwtException} (or
     * {@link IllegalArgumentException}) when the token is unusable.
     */
    public TokenClaims parse(String token) {
        Claims claims = parser.parseSignedClaims(token).getPayload();
        String roleRaw = claims.get("role", String.class);
        UserRole role = roleRaw == null ? null : UserRole.valueOf(roleRaw);
        Date expiration = claims.getExpiration();
        return new TokenClaims(
                claims.getSubject(),
                role,
                claims.get("purpose", String.class),
                claims.get("familyId", String.class),
                claims.getId(),
                expiration == null ? null : expiration.toInstant());
    }

    /** SHA-256 hex digest of a raw token; only this value is ever persisted. */
    public String hashToken(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }
}
