package com.bdquickmart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bdquickmart.modules.auth.dto.LoginRequest;
import com.bdquickmart.modules.auth.dto.RegisterRequest;
import com.bdquickmart.modules.user.UserEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * Explicit email/phone normalisation contract (migration requirement §6).
 *
 * <p>MySQL's {@code utf8mb4_unicode_ci} was case-insensitive; PostgreSQL is
 * case-sensitive by default. The application therefore normalises email to
 * {@code trim().toLowerCase(Locale.ROOT)} and phone to {@code trim()}, and the
 * schema adds functional {@code lower(...)} unique indexes as defence. These
 * tests pin the APPLICATION-level behaviour on the fast H2 suite; the DB-level
 * functional indexes are verified against real PostgreSQL by
 * {@link PostgresMigrationIntegrationTest}. Accent-insensitive behaviour is
 * deliberately NOT implemented.
 */
@DisplayName("Email / phone normalisation")
class NormalizationIntegrationTest extends BaseIntegrationTest {

    private static final String PASSWORD = "password123";

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private void register(String email, String phone) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegisterRequest(
                                "Normalised User", email, phone, PASSWORD, "CUSTOMER"))))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Email is stored lowercase; phone is stored trimmed")
    void emailAndPhoneAreNormalised() throws Exception {
        register("Alice@Example.COM", " 01700000001 ");

        UserEntity stored = userRepository.findByEmail("alice@example.com").orElseThrow();
        assertEquals("alice@example.com", stored.getEmail());
        assertEquals("01700000001", stored.getPhone());
    }

    @Test
    @DisplayName("Response exposes the normalised email")
    void responseExposesNormalisedEmail() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegisterRequest(
                                "Alice", "Alice@Example.COM", "01700000001", PASSWORD, "CUSTOMER"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email").value("alice@example.com"));
    }

    @Test
    @DisplayName("Login accepts the original mixed-case email (normalised on lookup)")
    void loginAcceptsMixedCaseEmail() throws Exception {
        register("Alice@Example.COM", "01700000001");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest("ALICE@EXAMPLE.COM", PASSWORD))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("A differently-cased duplicate email is rejected (409)")
    void differentlyCasedDuplicateEmailIsRejected() throws Exception {
        register("Alice@Example.COM", "01700000001");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegisterRequest(
                                "Clone", "alice@example.com", "01700000002", PASSWORD, "CUSTOMER"))))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("A padded phone collides with its trimmed form (409)")
    void trimmedPhoneDuplicateIsRejected() throws Exception {
        register("a@example.com", " 01700000001 ");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegisterRequest(
                                "Clone", "b@example.com", "01700000001", PASSWORD, "CUSTOMER"))))
                .andExpect(status().isConflict());
    }
}
