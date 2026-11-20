package com.bdquickmart.modules.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/** COD checkout payload. The idempotency key makes retry safe. */
public record CheckoutRequest(
        @NotBlank @Size(max = 80) String idempotencyKey,
        @NotEmpty @Valid List<CartItemRequest> items,
        @NotBlank @Size(max = 500) String deliveryAddress,
        @NotBlank @Size(max = 32) String contactPhone,
        @Size(max = 500) String notes,
        @NotBlank @Pattern(regexp = "COD") String paymentMethod) {
}
