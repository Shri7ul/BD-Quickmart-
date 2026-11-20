package com.bdquickmart.modules.order.dto;

import jakarta.validation.constraints.NotBlank;

/** Admin rider assignment payload. */
public record AssignRiderRequest(@NotBlank String riderPublicId) {
}
