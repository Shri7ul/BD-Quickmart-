package com.bdquickmart.modules.inventory.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Admin stock adjustment. Positive increases, negative decreases. */
public record StockAdjustmentRequest(
        @Min(-100000) @Max(100000) int delta,
        @NotBlank @Size(max = 300) String reason) {
}
