package com.bdquickmart;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bdquickmart.common.api.ApiCode;
import com.bdquickmart.modules.auth.dto.LoginRequest;
import com.bdquickmart.modules.catalog.CategoryRepository;
import com.bdquickmart.modules.catalog.dto.CategoryRequest;
import com.bdquickmart.modules.catalog.dto.ProductRequest;
import com.bdquickmart.modules.inventory.dto.StockUpdateRequest;
import com.bdquickmart.modules.order.dto.AssignRiderRequest;
import com.bdquickmart.modules.order.dto.CartItemRequest;
import com.bdquickmart.modules.order.dto.CheckoutRequest;
import com.bdquickmart.modules.order.dto.StatusUpdateRequest;
import com.bdquickmart.modules.user.AccountStatus;
import com.bdquickmart.modules.user.UserEntity;
import com.bdquickmart.modules.user.UserRole;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/** End-to-end Phase 1 order lifecycle: checkout → confirm → assign → deliver. */
@DisplayName("Phase 1 order lifecycle")
class OrderLifecycleIntegrationTest extends BaseIntegrationTest {

    private static final String PASSWORD = "password123";

    @Autowired
    private CategoryRepository categoryRepository;

    private String userToken(UserRole role, AccountStatus status, String email) throws Exception {
        String phone = "01" + UUID.randomUUID().toString().replace("-", "").substring(0, 9);
        UserEntity user = userRepository.save(new UserEntity(UUID.randomUUID().toString(), email, phone,
                passwordEncoder.encode(PASSWORD), "Test " + role, role, status));
        String body = objectMapper.writeValueAsString(new LoginRequest(email, PASSWORD));
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode data = objectMapper.readTree(response).get("data");
        return data.get("accessToken").asText() + "|" + user.getPublicId();
    }

    private String bearer(String tokenWithId) {
        return "Bearer " + tokenWithId.split("\\|")[0];
    }

    private String publicId(String tokenWithId) {
        return tokenWithId.split("\\|")[1];
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private String setupProduct(String admin) throws Exception {
        String categoryResponse = mockMvc.perform(post("/api/v1/admin/categories")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new CategoryRequest("Grocery", null, null, true))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String categoryName = objectMapper.readTree(categoryResponse).get("data").get("name").asText();
        Long categoryId = categoryRepository.findByNameIgnoreCase(categoryName).orElseThrow().getId();

        String productResponse = mockMvc.perform(post("/api/v1/admin/products")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new ProductRequest("Rice", "Premium rice", "RICE-1",
                                categoryId, new BigDecimal("100.00"), null, null, true))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String productPublicId = objectMapper.readTree(productResponse).get("data").get("publicId").asText();

        mockMvc.perform(put("/api/v1/admin/inventory/{id}/stock", productPublicId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new StockUpdateRequest(10, 2))))
                .andExpect(status().isOk());
        return productPublicId;
    }

    private String checkout(String customer, String key, String productPublicId, int quantity)
            throws Exception {
        CheckoutRequest request = new CheckoutRequest(key,
                List.of(new CartItemRequest(productPublicId, quantity)),
                "123 Main Street, Dhaka", "01700000000", "Leave at door", "COD");
        String response = mockMvc.perform(post("/api/v1/customer/checkout")
                        .header(HttpHeaders.AUTHORIZATION, bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON).content(json(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("data").get("publicId").asText();
    }

    private void transition(String tokenWithId, String orderPublicId, String status, boolean rider)
            throws Exception {
        String path = rider ? "/api/v1/rider/orders/{id}/status" : "/api/v1/admin/orders/{id}/status";
        mockMvc.perform(post(path, orderPublicId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenWithId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new StatusUpdateRequest(status, "test"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("1. Checkout computes authoritative totals, deducts stock once, and is idempotent")
    void checkoutIsAuthoritativeAndIdempotent() throws Exception {
        String admin = userToken(UserRole.ADMIN, AccountStatus.ACTIVE, "admin@order.test");
        String customer = userToken(UserRole.CUSTOMER, AccountStatus.ACTIVE, "customer@order.test");
        String product = setupProduct(admin);

        String first = checkout(customer, "idem-1", product, 2);
        mockMvc.perform(get("/api/v1/customer/orders/{id}", first)
                        .header(HttpHeaders.AUTHORIZATION, bearer(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.totalAmount").value(240.00))
                .andExpect(jsonPath("$.data.paymentMethod").value("COD"))
                .andExpect(jsonPath("$.data.items.length()").value(1));

        // Same key returns the SAME order; no extra stock deduction.
        String second = checkout(customer, "idem-1", product, 2);
        org.junit.jupiter.api.Assertions.assertEquals(first, second);
        mockMvc.perform(get("/api/v1/customer/products")
                        .header(HttpHeaders.AUTHORIZATION, bearer(customer)))
                .andExpect(jsonPath("$.data[0].stockQuantity").value(8));
    }

    @Test
    @DisplayName("2. Insufficient stock is rejected without changing stock or creating an order")
    void insufficientStockRejected() throws Exception {
        String admin = userToken(UserRole.ADMIN, AccountStatus.ACTIVE, "admin@stock2.test");
        String customer = userToken(UserRole.CUSTOMER, AccountStatus.ACTIVE, "customer@stock2.test");
        String product = setupProduct(admin);

        CheckoutRequest request = new CheckoutRequest("idem-oversell",
                List.of(new CartItemRequest(product, 100)),
                "123 Main Street", "01700000000", null, "COD");
        mockMvc.perform(post("/api/v1/customer/checkout")
                        .header(HttpHeaders.AUTHORIZATION, bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON).content(json(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(ApiCode.CONFLICT.getCode()));

        mockMvc.perform(get("/api/v1/customer/products")
                        .header(HttpHeaders.AUTHORIZATION, bearer(customer)))
                .andExpect(jsonPath("$.data[0].stockQuantity").value(10));
        mockMvc.perform(get("/api/v1/admin/orders")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("3. Customer cannot read another customer's order")
    void customerIsolation() throws Exception {
        String admin = userToken(UserRole.ADMIN, AccountStatus.ACTIVE, "admin@iso.test");
        String owner = userToken(UserRole.CUSTOMER, AccountStatus.ACTIVE, "owner@iso.test");
        String other = userToken(UserRole.CUSTOMER, AccountStatus.ACTIVE, "other@iso.test");
        String product = setupProduct(admin);
        String order = checkout(owner, "idem-iso", product, 1);

        mockMvc.perform(get("/api/v1/customer/orders/{id}", order)
                        .header(HttpHeaders.AUTHORIZATION, bearer(other)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("4. Full lifecycle: approve rider, assign, pickup, deliver, dashboard reflects it")
    void fullLifecycle() throws Exception {
        String admin = userToken(UserRole.ADMIN, AccountStatus.ACTIVE, "admin@life.test");
        String customer = userToken(UserRole.CUSTOMER, AccountStatus.ACTIVE, "customer@life.test");
        String rider = userToken(UserRole.RIDER, AccountStatus.PENDING, "rider@life.test");
        String product = setupProduct(admin);
        String order = checkout(customer, "idem-life", product, 1);

        // Rider cannot use the rider API while PENDING.
        mockMvc.perform(get("/api/v1/rider/orders/assigned")
                        .header(HttpHeaders.AUTHORIZATION, bearer(rider)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/admin/riders/{id}/approve", publicId(rider))
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk());

        transition(admin, order, "CONFIRMED", false);
        transition(admin, order, "PREPARING", false);
        transition(admin, order, "READY_FOR_PICKUP", false);

        mockMvc.perform(post("/api/v1/admin/orders/{id}/assign", order)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new AssignRiderRequest(publicId(rider)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.data.riderPublicId").value(publicId(rider)));

        mockMvc.perform(get("/api/v1/rider/orders/assigned")
                        .header(HttpHeaders.AUTHORIZATION, bearer(rider)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        transition(rider, order, "PICKED_UP", true);
        transition(rider, order, "OUT_FOR_DELIVERY", true);
        transition(rider, order, "DELIVERED", true);

        mockMvc.perform(get("/api/v1/customer/orders/{id}", order)
                        .header(HttpHeaders.AUTHORIZATION, bearer(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DELIVERED"));

        mockMvc.perform(get("/api/v1/admin/dashboard/summary")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalOrders").value(1))
                .andExpect(jsonPath("$.data.revenue").value(140.00))
                .andExpect(jsonPath("$.data.activeRiders").value(1));
    }

    @Test
    @DisplayName("5. Invalid status transitions are rejected by the server")
    void invalidTransitionRejected() throws Exception {
        String admin = userToken(UserRole.ADMIN, AccountStatus.ACTIVE, "admin@trans.test");
        String customer = userToken(UserRole.CUSTOMER, AccountStatus.ACTIVE, "customer@trans.test");
        String product = setupProduct(admin);
        String order = checkout(customer, "idem-trans", product, 1);

        mockMvc.perform(post("/api/v1/admin/orders/{id}/status", order)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new StatusUpdateRequest("DELIVERED", null))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("6. Cancelling a pending order restores stock exactly once")
    void cancellationRestoresStock() throws Exception {
        String admin = userToken(UserRole.ADMIN, AccountStatus.ACTIVE, "admin@cancel.test");
        String customer = userToken(UserRole.CUSTOMER, AccountStatus.ACTIVE, "customer@cancel.test");
        String product = setupProduct(admin);
        checkout(customer, "idem-cancel", product, 3);

        mockMvc.perform(get("/api/v1/customer/products")
                        .header(HttpHeaders.AUTHORIZATION, bearer(customer)))
                .andExpect(jsonPath("$.data[0].stockQuantity").value(7));

        String orderPublicId = objectMapper.readTree(mockMvc.perform(get("/api/v1/customer/orders")
                        .header(HttpHeaders.AUTHORIZATION, bearer(customer)))
                .andReturn().getResponse().getContentAsString()).get("data").get(0).get("publicId").asText();
        transition(admin, orderPublicId, "CANCELLED", false);

        mockMvc.perform(get("/api/v1/customer/products")
                        .header(HttpHeaders.AUTHORIZATION, bearer(customer)))
                .andExpect(jsonPath("$.data[0].stockQuantity").value(10));
    }
}
