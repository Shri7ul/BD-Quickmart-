package com.bdquickmart.modules.order.dto;

import java.math.BigDecimal;

public record OrderItemResponse(
        String productPublicId,
        String productName,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal) {
}
