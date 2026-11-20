package com.bdquickmart.modules.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** Sellable product. Price is authoritative on the server. */
@Entity
@Table(name = "products")
@EntityListeners(AuditingEntityListener.class)
public class ProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    private String publicId;

    @Column(nullable = false, length = 180)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(length = 64, unique = true)
    private String sku;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "base_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "discount_price", precision = 12, scale = 2)
    private BigDecimal discountPrice;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "is_available", nullable = false)
    private boolean available = true;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected ProductEntity() {
        // Required by JPA.
    }

    public ProductEntity(String publicId, String name, String description, String sku,
                         Long categoryId, BigDecimal basePrice, BigDecimal discountPrice,
                         String imageUrl, boolean available) {
        this.publicId = publicId;
        this.name = name;
        this.description = description;
        this.sku = sku;
        this.categoryId = categoryId;
        this.basePrice = basePrice;
        this.discountPrice = discountPrice;
        this.imageUrl = imageUrl;
        this.available = available;
    }

    /** Effective unit price used by checkout; never taken from the client. */
    public BigDecimal effectivePrice() {
        return discountPrice != null ? discountPrice : basePrice;
    }

    public Long getId() {
        return id;
    }

    public String getPublicId() {
        return publicId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getSku() {
        return sku;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public BigDecimal getDiscountPrice() {
        return discountPrice;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public boolean isAvailable() {
        return available;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void update(String name, String description, String sku, Long categoryId,
                       BigDecimal basePrice, BigDecimal discountPrice, String imageUrl,
                       boolean available) {
        this.name = name;
        this.description = description;
        this.sku = sku;
        this.categoryId = categoryId;
        this.basePrice = basePrice;
        this.discountPrice = discountPrice;
        this.imageUrl = imageUrl;
        this.available = available;
    }

    public void softDelete(Instant at) {
        this.deletedAt = at;
        this.available = false;
    }
}
