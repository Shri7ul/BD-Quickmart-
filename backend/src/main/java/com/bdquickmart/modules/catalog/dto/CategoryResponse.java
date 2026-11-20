package com.bdquickmart.modules.catalog.dto;

import java.time.Instant;

public record CategoryResponse(
        Long id,
        String publicId,
        String name,
        String description,
        String imageUrl,
        boolean active,
        Instant createdAt) {
}
