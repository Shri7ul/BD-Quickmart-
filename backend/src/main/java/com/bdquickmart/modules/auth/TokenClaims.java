package com.bdquickmart.modules.auth;

import com.bdquickmart.modules.user.UserRole;
import java.time.Instant;

/** Validated, typed view of the claims carried by a BD QuickMart JWT. */
public record TokenClaims(
        String subject,
        UserRole role,
        String purpose,
        String familyId,
        String tokenId,
        Instant expiresAt) {

    public boolean isAccess() {
        return "access".equals(purpose);
    }

    public boolean isRefresh() {
        return "refresh".equals(purpose);
    }
}
