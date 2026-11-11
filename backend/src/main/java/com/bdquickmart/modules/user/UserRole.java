package com.bdquickmart.modules.user;

/**
 * The six canonical roles of the platform.
 *
 * <p>Identifiers are shared verbatim across the database, JWT claims, Spring
 * Security authorities ({@code ROLE_<NAME>}), the admin-web TypeScript types,
 * and both Flutter apps. There is deliberately no {@code RoleHierarchy}: each
 * role is granted an explicit, least-privilege set of authorities.
 */
public enum UserRole {

    CUSTOMER,
    RIDER,
    ADMIN,
    SUPER_ADMIN,
    STORE_MANAGER,
    INVENTORY_OPERATOR;

    /** Roles a visitor may self-register as. Privileged roles are provisioned. */
    public boolean isPublicRegistration() {
        return this == CUSTOMER || this == RIDER;
    }

    /** Roles allowed to sign in to the admin web application. */
    public boolean isPrivileged() {
        return this == ADMIN || this == SUPER_ADMIN
                || this == STORE_MANAGER || this == INVENTORY_OPERATOR;
    }

    /** Roles permitted to approve or reject rider applications. */
    public boolean canApproveRiders() {
        return this == ADMIN || this == SUPER_ADMIN;
    }
}
