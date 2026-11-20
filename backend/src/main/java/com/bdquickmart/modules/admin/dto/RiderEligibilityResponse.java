package com.bdquickmart.modules.admin.dto;

/** Admin view of a rider's approval and availability state. */
public record RiderEligibilityResponse(
        String publicId,
        String fullName,
        String phone,
        String status,
        boolean online) {
}
