package com.bdquickmart.modules.inventory.dto;

import jakarta.validation.constraints.Min;

/** Admin absolute stock/threshold update. */
public record StockUpdateRequest(
        @Min(0) int quantityOnHand,
        @Min(0) int reorderThreshold) {
}
