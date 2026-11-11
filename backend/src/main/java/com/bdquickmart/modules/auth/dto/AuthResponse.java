package com.bdquickmart.modules.auth.dto;

import com.bdquickmart.modules.user.dto.UserResponse;

/**
 * Token pair returned by login and refresh.
 *
 * @param accessToken      short-lived bearer token
 * @param refreshToken     rotating refresh token (stored only as a hash)
 * @param tokenType        always {@code Bearer}
 * @param expiresInSeconds access token lifetime
 * @param user             authenticated profile
 */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds,
        UserResponse user) {
}
