package com.bdquickmart.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Public self-registration. Only {@code CUSTOMER} and {@code RIDER} are
 * accepted; privileged roles must be provisioned by an administrator.
 */
public record RegisterRequest(
        @NotBlank @Size(max = 120) String fullName,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Pattern(regexp = "^[0-9+\\-() ]{7,20}$",
                message = "phone must contain 7-20 digits, +, -, parentheses or spaces") String phone,
        @NotBlank @Size(min = 8, max = 72,
                message = "password must be 8-72 characters (BCrypt truncates at 72 bytes)") String password,
        @NotBlank String role) {
}
