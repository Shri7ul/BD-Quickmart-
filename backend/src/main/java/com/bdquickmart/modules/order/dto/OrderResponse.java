package com.bdquickmart.modules.order.dto;

import com.bdquickmart.modules.order.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Full order projection for admin, customer and rider clients. */
public record OrderResponse(
        String publicId,
        String status,
        BigDecimal subtotalAmount,
        BigDecimal discountAmount,
        BigDecimal deliveryFee,
        BigDecimal totalAmount,
        String paymentMethod,
        String deliveryAddress,
        String contactPhone,
        String notes,
        String customerPublicId,
        String customerName,
        String riderPublicId,
        String riderName,
        List<OrderItemResponse> items,
        List<OrderStatusEventResponse> events,
        Instant createdAt,
        Instant updatedAt) {
}
