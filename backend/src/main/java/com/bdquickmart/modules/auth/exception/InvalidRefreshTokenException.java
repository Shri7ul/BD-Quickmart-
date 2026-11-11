package com.bdquickmart.modules.auth.exception;

/**
 * Generic refresh-token failure.
 *
 * <p>All refresh failures (not found, expired, invalid, replayed, wrong
 * purpose) collapse to this single 401 result so the API cannot be used to
 * distinguish revoked tokens from valid ones.
 */
public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException() {
        super("Refresh token is invalid or expired");
    }
}
