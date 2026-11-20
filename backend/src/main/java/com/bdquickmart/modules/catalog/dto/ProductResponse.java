package com.bdquickmart.modules.catalog.dto;

import java.math.BigDecimal;

/** Customer-facing and admin-facing product projection. */
public record ProductResponse(
        String publicId,
        String name,
        String description,
        String sku,
        Long categoryId,
        String categoryName,
        BigDecimal basePrice,
        BigDecimal discountPrice,
        BigDecimal effectivePrice,
        String imageUrl,
        boolean available,
        int stockQuantity,
        boolean inStock) {
}
