package com.bdquickmart;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies the health endpoint is reachable and returns the standard response
 * envelope. The datasource is excluded from auto-configuration so that the
 * test does not require a running MySQL instance.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration"
})
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
    @DisplayName("GET /api/v1/health responds promptly even without a database")
    void healthEndpointRespondsQuicklyWithoutDatabase() throws Exception {
        long start = System.currentTimeMillis();

        mockMvc.perform(get("/api/v1/health")).andExpect(status().isOk());

        long elapsed = System.currentTimeMillis() - start;
        // The probe is bounded at ~2.5s; without the bound it would wait for
        // the full Hikari connection timeout (30s) on every call.
        org.junit.jupiter.api.Assertions.assertTrue(elapsed < 10_000,
                "Health endpoint took " + elapsed + "ms; it must not block on the database");
    }
}
