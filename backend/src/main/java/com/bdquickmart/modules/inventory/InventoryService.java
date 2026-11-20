package com.bdquickmart.modules.inventory;

import com.bdquickmart.common.exception.BusinessException;
import com.bdquickmart.common.exception.ResourceNotFoundException;
import com.bdquickmart.common.api.ApiCode;
import com.bdquickmart.modules.catalog.ProductEntity;
import com.bdquickmart.modules.catalog.ProductRepository;
import com.bdquickmart.modules.inventory.dto.AdjustmentResponse;
import com.bdquickmart.modules.inventory.dto.InventoryResponse;
import com.bdquickmart.modules.inventory.dto.StockAdjustmentRequest;
import com.bdquickmart.modules.inventory.dto.StockUpdateRequest;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Stock levels, adjustments and the immutable adjustment history. */
@Service
@Transactional(readOnly = true)
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryAdjustmentRepository adjustmentRepository;
    private final ProductRepository productRepository;
    private final Clock clock;

    public InventoryService(InventoryRepository inventoryRepository,
                            InventoryAdjustmentRepository adjustmentRepository,
                            ProductRepository productRepository,
                            Clock clock) {
        this.inventoryRepository = inventoryRepository;
        this.adjustmentRepository = adjustmentRepository;
        this.productRepository = productRepository;
        this.clock = clock;
    }

    public List<InventoryResponse> list() {
        return inventoryRepository.findAll().stream().map(this::toResponse).toList();
    }

    public InventoryResponse getByProductPublicId(String productPublicId) {
        ProductEntity product = productRepository.findByPublicId(productPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "publicId", productPublicId));
        InventoryEntity inventory = inventoryRepository.findByProductId(product.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", "productId", productPublicId));
        return toResponse(inventory);
    }

    public List<AdjustmentResponse> history(String productPublicId) {
        ProductEntity product = productRepository.findByPublicId(productPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "publicId", productPublicId));
        return adjustmentRepository.findByProductIdOrderByCreatedAtDesc(product.getId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public InventoryResponse adjust(String productPublicId, StockAdjustmentRequest request) {
        ProductEntity product = productRepository.findByPublicId(productPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "publicId", productPublicId));
        Instant now = clock.instant();
        InventoryEntity inventory = inventoryRepository.findByProductIdForUpdate(product.getId())
                .orElseGet(() -> inventoryRepository.save(
                        new InventoryEntity(product.getId(), 0, 5, now)));
        int target = inventory.getQuantityOnHand() + request.delta();
        if (target < 0) {
            throw new BusinessException(ApiCode.CONFLICT, "Stock cannot become negative");
        }
        inventory.setQuantityOnHand(target, now);
        inventoryRepository.save(inventory);
        adjustmentRepository.save(new InventoryAdjustmentEntity(UUID.randomUUID().toString(),
                product.getId(), request.delta(), target, AdjustmentReason.ADMIN_ADJUSTMENT,
                request.reason(), now));
        return toResponse(inventory);
    }

    @Transactional
    public InventoryResponse setStock(String productPublicId, StockUpdateRequest request) {
        ProductEntity product = productRepository.findByPublicId(productPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "publicId", productPublicId));
        Instant now = clock.instant();
        InventoryEntity inventory = inventoryRepository.findByProductIdForUpdate(product.getId())
                .orElseGet(() -> inventoryRepository.save(
                        new InventoryEntity(product.getId(), 0, 5, now)));
        int delta = request.quantityOnHand() - inventory.getQuantityOnHand();
        inventory.setQuantityOnHand(request.quantityOnHand(), now);
        inventory.setReorderThreshold(request.reorderThreshold());
        inventoryRepository.save(inventory);
        adjustmentRepository.save(new InventoryAdjustmentEntity(UUID.randomUUID().toString(),
                product.getId(), delta, request.quantityOnHand(), AdjustmentReason.ADMIN_ADJUSTMENT,
                "Manual stock set", now));
        return toResponse(inventory);
    }

    /** Called inside the order transaction; lock prevents overselling. */
    public void deductForOrder(Long productId, int quantity, String orderPublicId, Instant now) {
        InventoryEntity inventory = inventoryRepository.findByProductIdForUpdate(productId)
                .orElseThrow(() -> new BusinessException(ApiCode.CONFLICT,
                        "Product has no inventory record"));
        if (inventory.getQuantityOnHand() < quantity) {
            throw new BusinessException(ApiCode.CONFLICT, "Insufficient stock for product");
        }
        inventory.setQuantityOnHand(inventory.getQuantityOnHand() - quantity, now);
        inventoryRepository.save(inventory);
        adjustmentRepository.save(new InventoryAdjustmentEntity(UUID.randomUUID().toString(),
                productId, -quantity, inventory.getQuantityOnHand(), AdjustmentReason.ORDER_PLACED,
                orderPublicId, now));
    }

    /** Restores stock when an order is cancelled before pickup. */
    public void restoreForCancellation(Long productId, int quantity, String orderPublicId, Instant now) {
        InventoryEntity inventory = inventoryRepository.findByProductIdForUpdate(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", "productId",
                        String.valueOf(productId)));
        inventory.setQuantityOnHand(inventory.getQuantityOnHand() + quantity, now);
        inventoryRepository.save(inventory);
        adjustmentRepository.save(new InventoryAdjustmentEntity(UUID.randomUUID().toString(),
                productId, quantity, inventory.getQuantityOnHand(), AdjustmentReason.ORDER_CANCELLED,
                orderPublicId, now));
    }

    public long countLowStock() {
        return inventoryRepository.findLowStock().size();
    }

    private InventoryResponse toResponse(InventoryEntity inventory) {
        ProductEntity product = productRepository.findById(inventory.getProductId()).orElse(null);
        return new InventoryResponse(
                product == null ? null : product.getPublicId(),
                product == null ? null : product.getName(),
                product == null ? null : product.getSku(),
                inventory.getQuantityOnHand(),
                inventory.getReorderThreshold(),
                inventory.isLowStock(),
                inventory.getUpdatedAt());
    }

    private AdjustmentResponse toResponse(InventoryAdjustmentEntity entity) {
        return new AdjustmentResponse(entity.getPublicId(), entity.getDelta(),
                entity.getQuantityAfter(), entity.getReason().name(), entity.getReference(),
                entity.getCreatedAt());
    }
}
