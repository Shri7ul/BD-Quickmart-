package com.bdquickmart;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies the health endpoint is reachable and returns the standard response
 * envelope.
 *
 * <p>Goal 02 note: this test used to exclude DataSource/JPA auto-configuration.
 * Once JPA repositories exist that breaks the application context, so it now
 * uses the H2-backed {@code test} profile like every other integration test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/v1/health returns the standard success envelope")
    void healthEndpointReturnsSuccessEnvelope() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("BDQM-0000"))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.application").value("bd-quickmart-backend"));
    }

    @Test
    @DisplayName("GET /api/v1/health responds promptly")
    void healthEndpointRespondsQuickly() throws Exception {
        long start = System.currentTimeMillis();

        mockMvc.perform(get("/api/v1/health")).andExpect(status().isOk());

        long elapsed = System.currentTimeMillis() - start;
        Assertions.assertTrue(elapsed < 10_000,
                "Health endpoint took " + elapsed + "ms; it must not block on the database");
    }
}
