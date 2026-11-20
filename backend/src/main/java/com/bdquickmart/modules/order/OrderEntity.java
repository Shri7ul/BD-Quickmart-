package com.bdquickmart.modules.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

/** Customer order with server-computed totals and an immutable idempotency key. */
@Entity
@Table(name = "orders")
@EntityListeners(AuditingEntityListener.class)
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    private String publicId;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 80)
    private String idempotencyKey;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "rider_id")
    private Long riderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OrderStatus status = OrderStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 24)
    private PaymentMethod paymentMethod = PaymentMethod.COD;

    @Column(name = "delivery_address", nullable = false, length = 500)
    private String deliveryAddress;

    @Column(name = "contact_phone", nullable = false, length = 32)
    private String contactPhone;

    @Column(length = 500)
    private String notes;

    @Column(name = "subtotal_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotalAmount;

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "delivery_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal deliveryFee = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "assigned_at")
    private Instant assignedAt;

    @Column(name = "picked_up_at")
    private Instant pickedUpAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected OrderEntity() {
        // Required by JPA.
    }

    public OrderEntity(String publicId, String idempotencyKey, Long customerId,
                       String deliveryAddress, String contactPhone, String notes,
                       BigDecimal subtotalAmount, BigDecimal discountAmount,
                       BigDecimal deliveryFee, BigDecimal totalAmount) {
        this.publicId = publicId;
        this.idempotencyKey = idempotencyKey;
        this.customerId = customerId;
        this.deliveryAddress = deliveryAddress;
        this.contactPhone = contactPhone;
        this.notes = notes;
        this.subtotalAmount = subtotalAmount;
        this.discountAmount = discountAmount;
        this.deliveryFee = deliveryFee;
        this.totalAmount = totalAmount;
    }

    public Long getId() {
        return id;
    }

    public String getPublicId() {
        return publicId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public Long getRiderId() {
        return riderId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public String getNotes() {
        return notes;
    }

    public BigDecimal getSubtotalAmount() {
        return subtotalAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public BigDecimal getDeliveryFee() {
        return deliveryFee;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }

    public Instant getPickedUpAt() {
        return pickedUpAt;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void transitionTo(OrderStatus target, Instant now, Long riderId) {
        if (!status.canTransitionTo(target)) {
            throw new IllegalStateException("Invalid order status transition: " + status + " -> " + target);
        }
        this.status = target;
        if (target == OrderStatus.ASSIGNED) {
            this.riderId = riderId;
            this.assignedAt = now;
        } else if (target == OrderStatus.PICKED_UP) {
            this.pickedUpAt = now;
        } else if (target == OrderStatus.DELIVERED) {
            this.deliveredAt = now;
        } else if (target.isCancellation()) {
            this.riderId = null;
        }
    }

    public void setRiderId(Long riderId) {
        this.riderId = riderId;
    }
}
