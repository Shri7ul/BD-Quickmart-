package com.bdquickmart.common.api;

/**
 * Application-level result codes carried in {@link ApiResponse#code()}.
 *
 * <p>These are stable, machine-readable identifiers that clients may branch on.
 * HTTP status codes remain the primary signal; these refine the outcome.
 */
public enum ApiCode {

    SUCCESS("BDQM-0000", "Request completed successfully"),

    // 4xx - client errors
    VALIDATION_ERROR("BDQM-0400", "Request validation failed"),
    UNAUTHORIZED("BDQM-0401", "Authentication is required"),
    INVALID_CREDENTIALS("BDQM-0402", "Invalid credentials"),
    FORBIDDEN("BDQM-0403", "Access is denied"),
    NOT_FOUND("BDQM-0404", "Resource not found"),
    CONFLICT("BDQM-0409", "Resource conflict"),
    TOKEN_INVALID("BDQM-0410", "Token is invalid"),
    TOKEN_EXPIRED("BDQM-0411", "Token has expired"),
    ACCOUNT_DISABLED("BDQM-0412", "Account is not active"),
    UNPROCESSABLE_ENTITY("BDQM-0422", "Request could not be processed"),

    // 5xx - server errors
    INTERNAL_ERROR("BDQM-0500", "Unexpected server error"),
    SERVICE_UNAVAILABLE("BDQM-0503", "Service temporarily unavailable");

    private final String code;
    private final String defaultMessage;

    ApiCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public String getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
