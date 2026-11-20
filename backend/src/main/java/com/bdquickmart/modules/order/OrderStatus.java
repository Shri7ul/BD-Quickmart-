package com.bdquickmart.modules.order;

/**
 * Canonical order lifecycle shared by the admin console, customer app and
 * rider app. Status names intentionally match the existing admin StatusBadge
 * contract; no client may invent a new status.
 */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    PREPARING,
    READY_FOR_PICKUP,
    ASSIGNED,
    PICKED_UP,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED,
    REJECTED,
    FAILED_DELIVERY;

    /** Server-side state machine. Admin and rider actions are validated here. */
    public boolean canTransitionTo(OrderStatus target) {
        if (target == null || target == this) {
            return false;
        }
        return switch (this) {
            case PENDING -> target == CONFIRMED || target == CANCELLED || target == REJECTED;
            case CONFIRMED -> target == PREPARING || target == CANCELLED;
            case PREPARING -> target == READY_FOR_PICKUP || target == CANCELLED;
            case READY_FOR_PICKUP -> target == ASSIGNED || target == CANCELLED;
            case ASSIGNED -> target == PICKED_UP || target == CANCELLED;
            case PICKED_UP -> target == OUT_FOR_DELIVERY || target == FAILED_DELIVERY;
            case OUT_FOR_DELIVERY -> target == DELIVERED || target == FAILED_DELIVERY;
            case CANCELLED, REJECTED, DELIVERED, FAILED_DELIVERY -> false;
        };
    }

    public boolean isTerminal() {
        return switch (this) {
            case DELIVERED, CANCELLED, REJECTED, FAILED_DELIVERY -> true;
            default -> false;
        };
    }

    public boolean isCancellation() {
        return this == CANCELLED || this == REJECTED || this == FAILED_DELIVERY;
    }
}
