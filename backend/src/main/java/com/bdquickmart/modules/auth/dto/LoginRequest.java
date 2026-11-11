package com.bdquickmart.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** Login accepts an email address or a phone number as {@code identifier}. */
public record LoginRequest(
        @NotBlank String identifier,
        @NotBlank String password) {
}
