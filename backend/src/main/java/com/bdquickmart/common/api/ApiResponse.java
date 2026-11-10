package com.bdquickmart.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

/**
 * Standard envelope returned by every REST endpoint.
 *
 * @param success  whether the operation completed without error
 * @param code     application-level result code (see {@link ApiCode})
 * @param message  human readable message, safe to surface to end users
 * @param data     payload, {@code null} for error responses
 * @param path     request path that produced this response
 * @param timestamp server-side time the response was produced
 * @param traceId  correlation id, also emitted in the {@code X-Trace-Id} header
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        String code,
        String message,
        T data,
        String path,
        Instant timestamp,
        String traceId) {

    public static <T> ApiResponse<T> success(T data, String message, String path, String traceId) {
        return new ApiResponse<>(true, ApiCode.SUCCESS.getCode(), message, data, path, Instant.now(), traceId);
    }

    public static <T> ApiResponse<T> success(T data, String path, String traceId) {
        return success(data, ApiCode.SUCCESS.getDefaultMessage(), path, traceId);
    }

    public static <T> ApiResponse<T> error(
            String code, String message, String path, String traceId) {
        return new ApiResponse<>(false, code, message, null, path, Instant.now(), traceId);
    }
}
