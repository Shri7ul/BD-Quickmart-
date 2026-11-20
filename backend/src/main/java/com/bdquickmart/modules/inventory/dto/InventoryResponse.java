package com.bdquickmart.modules.inventory.dto;

import java.time.Instant;

public record InventoryResponse(
        String productPublicId,
        String productName,
        String sku,
        int quantityOnHand,
        int reorderThreshold,
        boolean lowStock,
        Instant updatedAt) {
}
