package com.bdquickmart.common.exception;

import com.bdquickmart.common.api.ApiCode;
import com.bdquickmart.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates exceptions into the standard {@link ApiResponse} envelope.
 *
 * <p>Every unhandled exception is logged with its stack trace and returned as a
 * generic {@code BDQM-0500} response so that internal details never leak to
 * clients.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        log.debug("Resource not found: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ex.getApiCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(
            BusinessException ex, HttpServletRequest request) {
        log.debug("Business rule violation: {}", ex.getMessage());
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getApiCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.putIfAbsent(
                        error.getField(), error.getDefaultMessage()));
        log.debug("Validation failed on {}: {}", request.getRequestURI(), fieldErrors);
        return build(HttpStatus.BAD_REQUEST, ApiCode.VALIDATION_ERROR,
                "Validation failed for: " + fieldErrors, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        String detail = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                .collect(Collectors.joining(", "));
        log.debug("Constraint violation on {}: {}", request.getRequestURI(), detail);
        return build(HttpStatus.BAD_REQUEST, ApiCode.VALIDATION_ERROR, detail, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.debug("Malformed request body on {}", request.getRequestURI());
        return build(HttpStatus.BAD_REQUEST, ApiCode.VALIDATION_ERROR,
                "Request body is missing or malformed", request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthentication(
            AuthenticationException ex, HttpServletRequest request) {
        log.debug("Authentication failure on {}", request.getRequestURI());
        return build(HttpStatus.UNAUTHORIZED, ApiCode.UNAUTHORIZED,
                "Authentication is required", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        log.debug("Access denied on {}", request.getRequestURI());
        return build(HttpStatus.FORBIDDEN, ApiCode.FORBIDDEN, "Access is denied", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(
            Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ApiCode.INTERNAL_ERROR,
                ApiCode.INTERNAL_ERROR.getDefaultMessage(), request);
    }

    private ResponseEntity<ApiResponse<Void>> build(
            HttpStatus status, ApiCode code, String message, HttpServletRequest request) {
        ApiResponse<Void> body = ApiResponse.error(
                code.getCode(), message, request.getRequestURI(), request.getAttribute("traceId") != null
                        ? request.getAttribute("traceId").toString()
                        : null);
        return ResponseEntity.status(status).body(body);
    }
}
