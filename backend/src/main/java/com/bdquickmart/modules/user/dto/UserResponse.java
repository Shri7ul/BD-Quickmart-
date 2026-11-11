package com.bdquickmart.modules.user.dto;

/**
 * Public projection of a user account. Never contains the password hash.
 *
 * @param publicId stable external id
 * @param fullName display name
 * @param email    login email
 * @param phone    login phone
 * @param role     canonical role name
 * @param status   account lifecycle state
 */
public record UserResponse(
        String publicId,
        String fullName,
        String email,
        String phone,
        String role,
        String status) {
}
