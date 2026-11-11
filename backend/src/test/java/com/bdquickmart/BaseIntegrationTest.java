package com.bdquickmart;

import com.bdquickmart.modules.auth.RefreshTokenRepository;
import com.bdquickmart.modules.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Shared wiring for the full-stack MockMvc integration tests: real Spring
 * Security filter chain, real BCrypt/JWT/JPA against H2, and a mutable clock.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestClockConfiguration.class)
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected RefreshTokenRepository refreshTokenRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected TestClockConfiguration.MutableClock clock;

    @BeforeEach
    void resetState() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        clock.setInstant(java.time.Instant.parse("2026-01-01T00:00:00Z"));
    }
}
