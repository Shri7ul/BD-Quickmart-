package com.bdquickmart.modules.order.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Live dashboard counters; every value is computed from the database. */
public record DashboardSummaryResponse(
        long totalOrders,
        long pendingOrders,
        BigDecimal revenue,
        long activeRiders,
        long lowStockItems,
        List<RecentOrder> recentOrders) {

    public record RecentOrder(
            String publicId,
            String customerName,
            String status,
            BigDecimal totalAmount,
            Instant createdAt) {
    }
}
