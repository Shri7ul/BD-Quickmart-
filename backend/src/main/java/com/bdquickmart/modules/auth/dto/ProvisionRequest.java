package com.bdquickmart.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Privileged account provisioning, restricted to SUPER_ADMIN. Only the four
 * privileged roles are accepted here; customers and riders self-register.
 */
public record ProvisionRequest(
        @NotBlank @Size(max = 120) String fullName,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Pattern(regexp = "^[0-9+\\-() ]{7,20}$",
                message = "phone must contain 7-20 digits, +, -, parentheses or spaces") String phone,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank String role) {
}
