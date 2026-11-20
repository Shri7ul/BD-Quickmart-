package com.bdquickmart.modules.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Client sends only product identity and quantity — never a price. */
public record CartItemRequest(
        @NotBlank @Size(max = 36) String productPublicId,
        @Min(1) int quantity) {
}
