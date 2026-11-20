package com.bdquickmart.modules.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Admin/rider status transition payload. */
public record StatusUpdateRequest(
        @NotBlank String status,
        @Size(max = 300) String note) {
}
