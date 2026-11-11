package com.bdquickmart.modules.auth;

import com.bdquickmart.common.api.ApiCode;
import com.bdquickmart.common.api.ApiResponse;
import com.bdquickmart.common.api.ApiResponses;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * Writes the standard {@link ApiResponse} envelope for 401s raised inside the
 * security filter chain. {@code @RestControllerAdvice} does not intercept
 * filter-level exceptions, so the JSON must be produced here.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        String code = (String) request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR);
        if (code == null) {
            code = ApiCode.UNAUTHORIZED.getCode();
        }
        String message = switch (code) {
            case "BDQM-0410" -> "Access token is invalid";
            case "BDQM-0411" -> "Access token has expired";
            default -> "Authentication is required";
        };

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        ApiResponse<Void> body = ApiResponses.error(code, message, request);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
