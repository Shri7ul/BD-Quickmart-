package com.bdquickmart.modules.user;

/**
 * Lifecycle state of a user account.
 *
 * <p>New rider registrations start {@link #PENDING} and may only reach the
 * rider-protected API once an administrator sets them {@link #ACTIVE}. The JWT
 * filter re-reads this value from the database on every request, so a status
 * change takes effect immediately rather than after token expiry.
 */
public enum AccountStatus {
    ACTIVE,
    PENDING,
    REJECTED,
    DISABLED
}
