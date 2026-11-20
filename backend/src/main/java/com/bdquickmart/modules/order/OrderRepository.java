package com.bdquickmart.modules.order;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    Optional<OrderEntity> findByPublicId(String publicId);

    Optional<OrderEntity> findByIdempotencyKey(String idempotencyKey);

    List<OrderEntity> findAllByOrderByCreatedAtDesc();

    List<OrderEntity> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<OrderEntity> findByRiderIdOrderByCreatedAtDesc(Long riderId);

    List<OrderEntity> findByStatusOrderByCreatedAtDesc(OrderStatus status);

    long countByStatus(OrderStatus status);

    long countByRiderIdAndStatus(Long riderId, OrderStatus status);
}
