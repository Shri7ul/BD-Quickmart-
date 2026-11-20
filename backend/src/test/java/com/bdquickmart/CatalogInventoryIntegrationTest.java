package com.bdquickmart;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bdquickmart.common.api.ApiCode;
import com.bdquickmart.modules.auth.dto.LoginRequest;
import com.bdquickmart.modules.catalog.dto.CategoryRequest;
import com.bdquickmart.modules.catalog.CategoryRepository;
import com.bdquickmart.modules.catalog.dto.ProductRequest;
import com.bdquickmart.modules.inventory.dto.StockAdjustmentRequest;
import com.bdquickmart.modules.inventory.dto.StockUpdateRequest;
import com.bdquickmart.modules.user.AccountStatus;
import com.bdquickmart.modules.user.UserEntity;
import com.bdquickmart.modules.user.UserRole;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/** End-to-end product, category and inventory flows through real security + JPA. */
@DisplayName("Phase 1 catalog and inventory")
class CatalogInventoryIntegrationTest extends BaseIntegrationTest {

    private static final String PASSWORD = "password123";

    @org.springframework.beans.factory.annotation.Autowired
    private CategoryRepository categoryRepository;

    private String token(UserRole role, AccountStatus status, String email) throws Exception {
        String phone = "01" + UUID.randomUUID().toString().replace("-", "").substring(0, 9);
        userRepository.save(new UserEntity(UUID.randomUUID().toString(), email, phone,
                passwordEncoder.encode(PASSWORD), "Test " + role, role, status));
        MvcResultHolder holder = login(email);
        return holder.token;
    }

    private MvcResultHolder login(String email) throws Exception {
        String body = objectMapper.writeValueAsString(new LoginRequest(email, PASSWORD));
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode data = objectMapper.readTree(response).get("data");
        return new MvcResultHolder(data.get("accessToken").asText());
    }

    private record MvcResultHolder(String token) {
    }

    private String auth(String token) {
        return "Bearer " + token;
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private String createCategory(String adminToken, String name) throws Exception {
        String response = mockMvc.perform(post("/api/v1/admin/categories")
                        .header(HttpHeaders.AUTHORIZATION, auth(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CategoryRequest(name, "Fresh produce", null, true))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("data").get("publicId").asText();
    }

    private String createProduct(String adminToken, Long categoryId, String name, String price)
            throws Exception {
        String response = mockMvc.perform(post("/api/v1/admin/products")
                        .header(HttpHeaders.AUTHORIZATION, auth(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new ProductRequest(name, "Desc", name.toUpperCase(),
                                categoryId, new BigDecimal(price), null, null, true))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("data").get("publicId").asText();
    }

    private Long categoryId(String adminToken, String publicId) throws Exception {
        String response = mockMvc.perform(get("/api/v1/admin/categories/{id}", publicId)
                        .header(HttpHeaders.AUTHORIZATION, auth(adminToken)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        // The category response is projected, so resolve the internal id from the repositories.
        String name = objectMapper.readTree(response).get("data").get("name").asText();
        return categoryRepository.findByNameIgnoreCase(name).orElseThrow().getId();
    }

    @Test
    @DisplayName("1. Admin creates category and product; both persist")
    void adminCreatesCatalog() throws Exception {
        String admin = token(UserRole.ADMIN, AccountStatus.ACTIVE, "admin@catalog.test");
        String categoryPublicId = createCategory(admin, "Fruits");
        Long categoryId = categoryId(admin, categoryPublicId);
        String productPublicId = createProduct(admin, categoryId, "Mango", "120.00");

        mockMvc.perform(get("/api/v1/admin/products/{id}", productPublicId)
                        .header(HttpHeaders.AUTHORIZATION, auth(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Mango"))
                .andExpect(jsonPath("$.data.basePrice").value(120.00));
    }

    @Test
    @DisplayName("2. Stock updates drive customer visibility and produce an audit trail")
    void stockDrivesCustomerCatalog() throws Exception {
        String admin = token(UserRole.ADMIN, AccountStatus.ACTIVE, "admin@stock.test");
        String customer = token(UserRole.CUSTOMER, AccountStatus.ACTIVE, "customer@stock.test");
        String categoryPublicId = createCategory(admin, "Vegetables");
        String productPublicId = createProduct(admin, categoryId(admin, categoryPublicId), "Tomato", "40.00");

        // No stock yet: product is hidden from customers.
        mockMvc.perform(get("/api/v1/customer/products")
                        .header(HttpHeaders.AUTHORIZATION, auth(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));

        mockMvc.perform(put("/api/v1/admin/inventory/{id}/stock", productPublicId)
                        .header(HttpHeaders.AUTHORIZATION, auth(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new StockUpdateRequest(10, 3))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quantityOnHand").value(10));

        mockMvc.perform(get("/api/v1/customer/products")
                        .header(HttpHeaders.AUTHORIZATION, auth(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].inStock").value(true))
                .andExpect(jsonPath("$.data[0].effectivePrice").value(40.00));

        mockMvc.perform(post("/api/v1/admin/inventory/{id}/adjust", productPublicId)
                        .header(HttpHeaders.AUTHORIZATION, auth(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new StockAdjustmentRequest(5, "delivery"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quantityOnHand").value(15));

        mockMvc.perform(get("/api/v1/admin/inventory/{id}/history", productPublicId)
                        .header(HttpHeaders.AUTHORIZATION, auth(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @DisplayName("3. Negative stock is rejected and category in use cannot be deleted")
    void safeguards() throws Exception {
        String admin = token(UserRole.ADMIN, AccountStatus.ACTIVE, "admin@safe.test");
        String categoryPublicId = createCategory(admin, "Dairy");
        String productPublicId = createProduct(admin, categoryId(admin, categoryPublicId), "Milk", "60.00");

        mockMvc.perform(post("/api/v1/admin/inventory/{id}/adjust", productPublicId)
                        .header(HttpHeaders.AUTHORIZATION, auth(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new StockAdjustmentRequest(-1, "mistake"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(ApiCode.CONFLICT.getCode()));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/v1/admin/categories/{id}", categoryPublicId)
                        .header(HttpHeaders.AUTHORIZATION, auth(admin)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("4. Customers cannot call admin catalog endpoints")
    void customerCannotUseAdminApi() throws Exception {
        String customer = token(UserRole.CUSTOMER, AccountStatus.ACTIVE, "customer@rbac.test");
        mockMvc.perform(get("/api/v1/admin/products")
                        .header(HttpHeaders.AUTHORIZATION, auth(customer)))
                .andExpect(status().isForbidden());
    }
}
