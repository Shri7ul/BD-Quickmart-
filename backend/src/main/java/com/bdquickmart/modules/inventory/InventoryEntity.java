package com.bdquickmart.modules.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

/** Current stock level for exactly one product. */
@Entity
@Table(name = "inventory")
public class InventoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false, unique = true)
    private Long productId;

    @Column(name = "quantity_on_hand", nullable = false)
    private int quantityOnHand;

    @Column(name = "reorder_threshold", nullable = false)
    private int reorderThreshold = 5;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected InventoryEntity() {
        // Required by JPA.
    }

    public InventoryEntity(Long productId, int quantityOnHand, int reorderThreshold,
                           Instant updatedAt) {
        this.productId = productId;
        this.quantityOnHand = quantityOnHand;
        this.reorderThreshold = reorderThreshold;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getProductId() {
        return productId;
    }

    public int getQuantityOnHand() {
        return quantityOnHand;
    }

    public int getReorderThreshold() {
        return reorderThreshold;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean isLowStock() {
        return quantityOnHand <= reorderThreshold;
    }

    public void setQuantityOnHand(int quantityOnHand, Instant updatedAt) {
        if (quantityOnHand < 0) {
            throw new IllegalArgumentException("Stock cannot be negative");
        }
        this.quantityOnHand = quantityOnHand;
        this.updatedAt = updatedAt;
    }

    public void setReorderThreshold(int reorderThreshold) {
        this.reorderThreshold = Math.max(0, reorderThreshold);
    }
}
