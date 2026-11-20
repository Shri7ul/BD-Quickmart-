package com.bdquickmart.modules.order.dto;

import java.time.Instant;

public record OrderStatusEventResponse(
        String status,
        Instant at,
        String note) {
}
