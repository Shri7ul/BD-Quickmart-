package com.bdquickmart;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bdquickmart.modules.auth.dto.LoginRequest;
import com.bdquickmart.modules.auth.dto.ProvisionRequest;
import com.bdquickmart.modules.auth.dto.RefreshRequest;
import com.bdquickmart.modules.auth.dto.RegisterRequest;
import com.bdquickmart.modules.user.AccountStatus;
import com.bdquickmart.modules.user.UserEntity;
import com.bdquickmart.modules.user.UserRole;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * End-to-end authentication and authorization tests using the real Spring
 * Security filter chain, real BCrypt/JWT/JPA (H2) and a mutable clock.
 */
@DisplayName("Goal 02 authentication & role management")
class AuthIntegrationTest extends BaseIntegrationTest {

    private static final String PASSWORD = "password123";

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private void register(String role, String email, String phone) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegisterRequest("Test User", email, phone, PASSWORD, role))))
                .andExpect(status().isCreated());
    }

    private String[] login(String identifier, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest(identifier, password))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        return new String[]{data.get("accessToken").asText(), data.get("refreshToken").asText()};
    }

    private UserEntity createUser(String email, String phone, UserRole role,
                                  AccountStatus status, String password) {
        return userRepository.save(new UserEntity(
                UUID.randomUUID().toString(), email, phone,
                passwordEncoder.encode(password), "Test " + role.name(), role, status));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    // ------------------------------------------------------------------
    // Registration
    // ------------------------------------------------------------------

    @Test
    @DisplayName("1. Customer registration returns 201 and an ACTIVE account")
    void registerCustomer() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegisterRequest(
                                "Alice", "alice@example.com", "01700000001", PASSWORD, "CUSTOMER"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.email").value("alice@example.com"));
    }

    @Test
    @DisplayName("2. Rider registration returns 201 and a PENDING account")
    void registerRiderIsPending() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegisterRequest(
                                "Bob", "rider@example.com", "01700000002", PASSWORD, "RIDER"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.role").value("RIDER"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        UserEntity rider = userRepository.findByEmail("rider@example.com").orElseThrow();
        assertTrue(rider.getStatus() == AccountStatus.PENDING);
    }

    @Test
    @DisplayName("3. Public registration rejects privileged roles (no escalation)")
    void registerRejectsPrivilegedRole() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegisterRequest(
                                "Eve", "eve@example.com", "01700000003", PASSWORD, "SUPER_ADMIN"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BDQM-0400"));
        assertFalse(userRepository.existsByEmail("eve@example.com"));
    }

    @Test
    @DisplayName("4. Duplicate email/phone returns 409")
    void registerRejectsDuplicate() throws Exception {
        register("CUSTOMER", "dup@example.com", "01700000004");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegisterRequest(
                                "Clone", "dup@example.com", "01700000005", PASSWORD, "CUSTOMER"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BDQM-0409"));
    }

    @Test
    @DisplayName("5. Blank required fields return 400")
    void registerRejectsInvalidPayload() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegisterRequest(
                                "", "blank@example.com", "01700000006", PASSWORD, "CUSTOMER"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BDQM-0400"));
    }

    // ------------------------------------------------------------------
    // Login
    // ------------------------------------------------------------------

    @Test
    @DisplayName("6. Login by email returns a non-empty access/refresh pair")
    void loginWithEmail() throws Exception {
        register("CUSTOMER", "alice@example.com", "01700000001");
        String[] tokens = login("alice@example.com", PASSWORD);
        assertFalse(tokens[0].isBlank());
        assertFalse(tokens[1].isBlank());
    }

    @Test
    @DisplayName("7. Login by phone returns a token pair")
    void loginWithPhone() throws Exception {
        register("CUSTOMER", "alice@example.com", "01700000001");
        String[] tokens = login("01700000001", PASSWORD);
        assertFalse(tokens[0].isBlank());
    }

    @Test
    @DisplayName("8. Wrong password returns 401 BDQM-0402")
    void loginWrongPassword() throws Exception {
        register("CUSTOMER", "alice@example.com", "01700000001");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest("alice@example.com", "wrongpass1"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("BDQM-0402"));
    }

    @Test
    @DisplayName("9. Unknown identifier returns 401 BDQM-0402")
    void loginUnknownIdentifier() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest("nobody@example.com", PASSWORD))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("BDQM-0402"));
    }

    @Test
    @DisplayName("10. Disabled account returns 401 BDQM-0412")
    void loginDisabledAccount() throws Exception {
        createUser("disabled@example.com", "01700000099",
                UserRole.CUSTOMER, AccountStatus.DISABLED, PASSWORD);
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest("disabled@example.com", PASSWORD))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("BDQM-0412"));
    }

    // ------------------------------------------------------------------
    // Access tokens / protected endpoints
    // ------------------------------------------------------------------

    @Test
    @DisplayName("11. /auth/me with a valid access token returns the profile")
    void meWithValidToken() throws Exception {
        register("CUSTOMER", "alice@example.com", "01700000001");
        String[] tokens = login("alice@example.com", PASSWORD);
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", bearer(tokens[0])))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.data.email").value("alice@example.com"));
    }

    @Test
    @DisplayName("12. /auth/me without a token returns 401 BDQM-0401")
    void meWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("BDQM-0401"));
    }

    @Test
    @DisplayName("13. Using a refresh token as a bearer returns 401 BDQM-0410")
    void meWithRefreshTokenAsBearer() throws Exception {
        register("CUSTOMER", "alice@example.com", "01700000001");
        String[] tokens = login("alice@example.com", PASSWORD);
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", bearer(tokens[1])))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("BDQM-0410"));
    }

    @Test
    @DisplayName("14. Expired access token returns 401 BDQM-0411")
    void meWithExpiredToken() throws Exception {
        register("CUSTOMER", "alice@example.com", "01700000001");
        String[] tokens = login("alice@example.com", PASSWORD);
        clock.advance(Duration.ofSeconds(901));
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", bearer(tokens[0])))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("BDQM-0411"));
    }

    @Test
    @DisplayName("15. Malformed token returns 401 BDQM-0410")
    void meWithMalformedToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", bearer("not-a-real-jwt")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("BDQM-0410"));
    }

    // ------------------------------------------------------------------
    // Refresh rotation / replay / logout
    // ------------------------------------------------------------------

    @Test
    @DisplayName("16. Refresh rotates tokens; replay revokes the whole family")
    void refreshRotatesAndReplayRevokesFamily() throws Exception {
        register("CUSTOMER", "alice@example.com", "01700000001");
        String[] tokens = login("alice@example.com", PASSWORD);

        MvcResult refreshed = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RefreshRequest(tokens[1]))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode data = objectMapper.readTree(
                refreshed.getResponse().getContentAsString()).get("data");
        String newRefresh = data.get("refreshToken").asText();
        assertNotEquals(tokens[1], newRefresh);

        // Replaying the old (now revoked) token is a 401 AND kills the family.
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RefreshRequest(tokens[1]))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("BDQM-0401"));

        // The freshly issued token is now unusable too.
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RefreshRequest(newRefresh))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("17. Refresh with an access token returns 401 BDQM-0401")
    void refreshWithAccessToken() throws Exception {
        register("CUSTOMER", "alice@example.com", "01700000001");
        String[] tokens = login("alice@example.com", PASSWORD);
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RefreshRequest(tokens[0]))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("BDQM-0401"));
    }

    @Test
    @DisplayName("18. Logout revokes the user's refresh tokens")
    void logoutRevokesRefreshTokens() throws Exception {
        register("CUSTOMER", "alice@example.com", "01700000001");
        String[] tokens = login("alice@example.com", PASSWORD);
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", bearer(tokens[0])))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RefreshRequest(tokens[1]))))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------
    // Role-based authorization
    // ------------------------------------------------------------------

    @Test
    @DisplayName("19. Customer reaches /customer/ping but not /admin or /rider")
    void customerProbeIsolated() throws Exception {
        register("CUSTOMER", "alice@example.com", "01700000001");
        String[] tokens = login("alice@example.com", PASSWORD);
        mockMvc.perform(get("/api/v1/customer/ping")
                        .header("Authorization", bearer(tokens[0])))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/admin/ping")
                        .header("Authorization", bearer(tokens[0])))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/rider/ping")
                        .header("Authorization", bearer(tokens[0])))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("20. PENDING rider is blocked until an ADMIN approves")
    void pendingRiderBlockedUntilApproved() throws Exception {
        register("RIDER", "rider@example.com", "01700000002");
        String[] riderTokens = login("rider@example.com", PASSWORD);
        mockMvc.perform(get("/api/v1/rider/ping")
                        .header("Authorization", bearer(riderTokens[0])))
                .andExpect(status().isForbidden());

        createUser("admin@example.com", "01700000003",
                UserRole.ADMIN, AccountStatus.ACTIVE, PASSWORD);
        String[] adminTokens = login("admin@example.com", PASSWORD);
        String riderPublicId = userRepository.findByEmail("rider@example.com")
                .orElseThrow().getPublicId();

        mockMvc.perform(post("/api/v1/admin/riders/" + riderPublicId + "/approve")
                        .header("Authorization", bearer(adminTokens[0])))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        // Status is re-read from the DB, so the SAME token now works.
        mockMvc.perform(get("/api/v1/rider/ping")
                        .header("Authorization", bearer(riderTokens[0])))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("21. Rider approval endpoints require PERM_APPROVE_RIDERS")
    void approvalEndpointsRespectAuthorities() throws Exception {
        createUser("admin@example.com", "01700000003",
                UserRole.ADMIN, AccountStatus.ACTIVE, PASSWORD);
        createUser("store@example.com", "01700000004",
                UserRole.STORE_MANAGER, AccountStatus.ACTIVE, PASSWORD);
        String[] admin = login("admin@example.com", PASSWORD);
        String[] store = login("store@example.com", PASSWORD);

        mockMvc.perform(get("/api/v1/admin/riders/pending")
                        .header("Authorization", bearer(admin[0])))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/admin/riders/pending")
                        .header("Authorization", bearer(store[0])))
                .andExpect(status().isForbidden());
        // A store manager may still sign in and reach the admin probe.
        mockMvc.perform(get("/api/v1/admin/ping")
                        .header("Authorization", bearer(store[0])))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("22. Provisioning is SUPER_ADMIN-only and rejects CUSTOMER/RIDER")
    void provisionRequiresSuperAdmin() throws Exception {
        createUser("root@example.com", "01700000005",
                UserRole.SUPER_ADMIN, AccountStatus.ACTIVE, PASSWORD);
        createUser("admin@example.com", "01700000006",
                UserRole.ADMIN, AccountStatus.ACTIVE, PASSWORD);
        register("CUSTOMER", "customer@example.com", "01700000007");
        String[] root = login("root@example.com", PASSWORD);
        String[] admin = login("admin@example.com", PASSWORD);
        String[] customer = login("customer@example.com", PASSWORD);

        String body = json(new ProvisionRequest(
                "New Manager", "mgr@example.com", "01700000008", PASSWORD, "STORE_MANAGER"));

        mockMvc.perform(post("/api/v1/admin/users/provision")
                        .header("Authorization", bearer(root[0]))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.role").value("STORE_MANAGER"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        mockMvc.perform(post("/api/v1/admin/users/provision")
                        .header("Authorization", bearer(admin[0]))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/admin/users/provision")
                        .header("Authorization", bearer(customer[0]))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());

        // Provisioning a public role through the privileged endpoint is blocked.
        mockMvc.perform(post("/api/v1/admin/users/provision")
                        .header("Authorization", bearer(root[0]))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new ProvisionRequest(
                                "Fake", "fake@example.com", "01700000010", PASSWORD, "CUSTOMER"))))
                .andExpect(status().isBadRequest());

        // The newly provisioned manager can log in.
        String[] manager = login("mgr@example.com", PASSWORD);
        assertFalse(manager[0].isBlank());
    }

    @Test
    @DisplayName("23. Public health endpoint stays permitAll under the secured chain")
    void healthStaysPublic() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
