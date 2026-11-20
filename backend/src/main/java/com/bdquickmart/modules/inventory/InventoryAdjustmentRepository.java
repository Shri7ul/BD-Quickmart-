package com.bdquickmart.modules.inventory;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryAdjustmentRepository extends JpaRepository<InventoryAdjustmentEntity, Long> {
    List<InventoryAdjustmentEntity> findByProductIdOrderByCreatedAtDesc(Long productId);
}
