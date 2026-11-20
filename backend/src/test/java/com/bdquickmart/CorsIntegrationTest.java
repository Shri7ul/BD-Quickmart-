package com.bdquickmart;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bdquickmart.modules.auth.dto.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/**
 * Verifies that the browser CORS contract is enforced by the SAME Spring
 * Security filter chain used in production (the MockMvc setup in
 * {@link BaseIntegrationTest} applies the real security filters).
 *
 * <p>These tests cover the exact failure seen in production: the admin web
 * origin is allowed, its OPTIONS preflight is answered by the CorsFilter
 * BEFORE any authentication filter can reject it, and a non-allowed origin is
 * still rejected (no wildcard origin with credentials).
 */
@DisplayName("CORS preflight and cross-origin responses")
class CorsIntegrationTest extends BaseIntegrationTest {

    private static final String PRODUCTION_ORIGIN = "https://bdquickmart.vercel.app";
    private static final String DISALLOWED_ORIGIN = "https://not-a-bdqm-client.example";
    private static final String LOGIN_PATH = "/api/v1/auth/login";

    @Test
    @DisplayName("1. Production-origin preflight on login returns 200 with the expected headers")
    void loginPreflightFromProductionOriginSucceeds() throws Exception {
        mockMvc.perform(options(LOGIN_PATH)
                        .header(HttpHeaders.ORIGIN, PRODUCTION_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS,
                                "content-type,authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        PRODUCTION_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                        "GET,POST,PUT,PATCH,DELETE,OPTIONS"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                        containsString("content-type")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                        containsString("authorization")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600"));
    }

    @Test
    @DisplayName("2. A non-allowed origin is rejected (no wildcard with credentials)")
    void preflightFromUnknownOriginIsForbidden() throws Exception {
        mockMvc.perform(options(LOGIN_PATH)
                        .header(HttpHeaders.ORIGIN, DISALLOWED_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    @DisplayName("3. A wildcard Origin literal is rejected when credentials are enabled")
    void wildcardOriginIsForbidden() throws Exception {
        mockMvc.perform(options(LOGIN_PATH)
                        .header(HttpHeaders.ORIGIN, "*")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("4. Only configured methods are allowed; TRACE preflight is rejected")
    void preflightForUnlistedMethodIsForbidden() throws Exception {
        mockMvc.perform(options(LOGIN_PATH)
                        .header(HttpHeaders.ORIGIN, PRODUCTION_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "TRACE"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("5. Preflight on a protected route is handled before authentication (no 401)")
    void preflightOnProtectedRouteBypassesAuthentication() throws Exception {
        // /api/v1/auth/me requires a JWT for GET, but its OPTIONS preflight
        // must succeed without any Authorization header.
        mockMvc.perform(options("/api/v1/auth/me")
                        .header(HttpHeaders.ORIGIN, PRODUCTION_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        PRODUCTION_ORIGIN));
    }

    @Test
    @DisplayName("6. A real cross-origin POST carries the CORS response headers")
    void actualCrossOriginPostCarriesCorsHeaders() throws Exception {
        String body = objectMapper.writeValueAsString(new RegisterRequest(
                "Cors Test", "cors@example.com", "01700000000", "password123", "CUSTOMER"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .header(HttpHeaders.ORIGIN, PRODUCTION_ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        PRODUCTION_ORIGIN));
    }

    @Test
    @DisplayName("7. Filter-level 401 responses still carry CORS headers")
    void authenticationFailureCarriesCorsHeaders() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.ORIGIN, PRODUCTION_ORIGIN))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        PRODUCTION_ORIGIN));
    }
}
