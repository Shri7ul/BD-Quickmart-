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
import com.bdquickmart.modules.order.dto.CartItemRequest;
import com.bdquickmart.modules.order.dto.CheckoutRequest;
import java.util.List;
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
        return createProduct(adminToken, categoryId, name, price, true);
    }

    private String createProduct(String adminToken, Long categoryId, String name, String price,
                                 boolean available) throws Exception {
        String response = mockMvc.perform(post("/api/v1/admin/products")
                        .header(HttpHeaders.AUTHORIZATION, auth(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new ProductRequest(name, "Desc", name.toUpperCase(),
                                categoryId, new BigDecimal(price), null, null, available))))
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
    @DisplayName("2. Admin-created product is listed immediately; stock only drives the inStock flag")
    void stockDrivesCustomerCatalog() throws Exception {
        String admin = token(UserRole.ADMIN, AccountStatus.ACTIVE, "admin@stock.test");
        String customer = token(UserRole.CUSTOMER, AccountStatus.ACTIVE, "customer@stock.test");
        String categoryPublicId = createCategory(admin, "Vegetables");
        String productPublicId = createProduct(admin, categoryId(admin, categoryPublicId), "Tomato", "40.00");

        // REGRESSION: a product created from Admin is visible at once. It starts with
        // zero stock, so inStock is false — the app renders its "Out of stock" state.
        mockMvc.perform(get("/api/v1/customer/products")
                        .header(HttpHeaders.AUTHORIZATION, auth(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].publicId").value(productPublicId))
                .andExpect(jsonPath("$.data[0].stockQuantity").value(0))
                .andExpect(jsonPath("$.data[0].inStock").value(false))
                .andExpect(jsonPath("$.data[0].available").value(true));

        // The detail endpoint must also load so the client can show the unavailable state.
        mockMvc.perform(get("/api/v1/customer/products/{id}", productPublicId)
                        .header(HttpHeaders.AUTHORIZATION, auth(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.inStock").value(false));

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
                .andExpect(jsonPath("$.data[0].stockQuantity").value(10))
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
    @DisplayName("5. Unavailable products stay hidden from customers without exposing stock")
    void unavailableProductsRemainHidden() throws Exception {
        String admin = token(UserRole.ADMIN, AccountStatus.ACTIVE, "admin@hidden.test");
        String customer = token(UserRole.CUSTOMER, AccountStatus.ACTIVE, "customer@hidden.test");
        String categoryPublicId = createCategory(admin, "Hidden goods");
        Long categoryId = categoryId(admin, categoryPublicId);
        createProduct(admin, categoryId, "Visible", "10.00", true);
        String hidden = createProduct(admin, categoryId, "Hidden", "20.00", false);

        mockMvc.perform(get("/api/v1/customer/products")
                        .header(HttpHeaders.AUTHORIZATION, auth(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Visible"));

        // Still an admin record; only the customer projection suppresses it.
        mockMvc.perform(get("/api/v1/admin/products/{id}", hidden)
                        .header(HttpHeaders.AUTHORIZATION, auth(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(false));
    }

    @Test
    @DisplayName("6. Out-of-stock checkout is still rejected by the server")
    void outOfStockCheckoutRejected() throws Exception {
        String admin = token(UserRole.ADMIN, AccountStatus.ACTIVE, "admin@oos.test");
        String customer = token(UserRole.CUSTOMER, AccountStatus.ACTIVE, "customer@oos.test");
        String categoryPublicId = createCategory(admin, "Out of stock goods");
        String product = createProduct(admin, categoryId(admin, categoryPublicId), "Empty", "30.00");

        CheckoutRequest request = new CheckoutRequest("idem-oos-1",
                List.of(new CartItemRequest(product, 1)),
                "123 Main Street, Dhaka", "01700000000", null, "COD");
        mockMvc.perform(post("/api/v1/customer/checkout")
                        .header(HttpHeaders.AUTHORIZATION, auth(customer))
                        .contentType(MediaType.APPLICATION_JSON).content(json(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(ApiCode.CONFLICT.getCode()));
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
