package com.bdquickmart.modules.catalog.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Admin create/update payload for a product. Prices are authoritative here. */
public record ProductRequest(
        @NotBlank @Size(max = 180) String name,
        @Size(max = 1000) String description,
        @Size(max = 64) String sku,
        Long categoryId,
        @DecimalMin("0.01") BigDecimal basePrice,
        @DecimalMin("0.00") BigDecimal discountPrice,
        @Size(max = 500) String imageUrl,
        boolean available) {
}
