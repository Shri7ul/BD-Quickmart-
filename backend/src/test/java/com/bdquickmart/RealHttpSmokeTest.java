package com.bdquickmart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

/**
 * A real HTTP round-trip through the embedded servlet container and the full
 * security filter chain (MockMvc exercises the same chain in-process).
 *
 * <p>Uses its own H2 database name and an explicit {@code server.port=0} so it
 * cannot clash with the other cached test contexts or with an injected
 * {@code SERVER__PORT} environment variable.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "server.port=0",
                "spring.datasource.url=jdbc:h2:mem:bdqm_http;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
        })
@ActiveProfiles("test")
@DisplayName("Goal 02 real-HTTP smoke test")
class RealHttpSmokeTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("GET /api/v1/health is reachable over real HTTP without a token")
    void healthIsPublicOverHttp() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/v1/health", String.class);
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody() != null && response.getBody().contains("\"success\":true"));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me without a token returns a JSON 401 envelope")
    void protectedEndpointReturnsJson401() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/v1/auth/me", String.class);
        assertEquals(401, response.getStatusCode().value());
        assertTrue(response.getBody() != null && response.getBody().contains("BDQM-0401"));
    }
}
