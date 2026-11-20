package com.bdquickmart.modules.inventory;

/** Reason codes recorded for every stock mutation. */
public enum AdjustmentReason {
    INITIAL,
    ADMIN_ADJUSTMENT,
    ORDER_PLACED,
    ORDER_CANCELLED
}
