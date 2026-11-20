package com.bdquickmart.modules.order;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderStatusEventRepository extends JpaRepository<OrderStatusEventEntity, Long> {
    List<OrderStatusEventEntity> findByOrderIdOrderByCreatedAtAsc(Long orderId);
}
