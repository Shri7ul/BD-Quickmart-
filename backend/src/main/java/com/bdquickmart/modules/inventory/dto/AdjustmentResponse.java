package com.bdquickmart.modules.inventory.dto;

import java.time.Instant;

public record AdjustmentResponse(
        String publicId,
        int delta,
        int quantityAfter,
        String reason,
        String reference,
        Instant createdAt) {
}
