package com.bdquickmart.common.api;

import com.bdquickmart.common.util.TraceIdFilter;
import jakarta.servlet.http.HttpServletRequest;

/** Convenience builders that fill {@code path} and {@code traceId} from a request. */
public final class ApiResponses {

    private ApiResponses() {
    }

    public static <T> ApiResponse<T> success(T data, HttpServletRequest request) {
        return ApiResponse.success(data, request.getRequestURI(), traceId(request));
    }

    public static <T> ApiResponse<T> success(T data, String message, HttpServletRequest request) {
        return ApiResponse.success(data, message, request.getRequestURI(), traceId(request));
    }

    public static <T> ApiResponse<T> error(String code, String message, HttpServletRequest request) {
        return ApiResponse.error(code, message, request.getRequestURI(), traceId(request));
    }

    /** Reads the correlation id set by {@link TraceIdFilter}. */
    public static String traceId(HttpServletRequest request) {
        Object value = request.getAttribute(TraceIdFilter.MDC_KEY);
        return value == null ? null : value.toString();
    }
}
