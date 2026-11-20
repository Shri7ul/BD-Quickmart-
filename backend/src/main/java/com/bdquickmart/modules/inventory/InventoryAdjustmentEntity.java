package com.bdquickmart.modules.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Immutable audit record of a stock change. */
@Entity
@Table(name = "inventory_adjustments")
public class InventoryAdjustmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    private String publicId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private int delta;

    @Column(name = "quantity_after", nullable = false)
    private int quantityAfter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AdjustmentReason reason;

    @Column(length = 80)
    private String reference;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected InventoryAdjustmentEntity() {
        // Required by JPA.
    }

    public InventoryAdjustmentEntity(String publicId, Long productId, int delta,
                                     int quantityAfter, AdjustmentReason reason,
                                     String reference, Instant createdAt) {
        this.publicId = publicId;
        this.productId = productId;
        this.delta = delta;
        this.quantityAfter = quantityAfter;
        this.reason = reason;
        this.reference = reference;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getPublicId() {
        return publicId;
    }

    public Long getProductId() {
        return productId;
    }

    public int getDelta() {
        return delta;
    }

    public int getQuantityAfter() {
        return quantityAfter;
    }

    public AdjustmentReason getReason() {
        return reason;
    }

    public String getReference() {
        return reference;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
