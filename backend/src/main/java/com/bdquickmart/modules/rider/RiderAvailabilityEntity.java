package com.bdquickmart.modules.rider;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Rider's explicit online/offline availability flag. */
@Entity
@Table(name = "rider_availability")
public class RiderAvailabilityEntity {

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private boolean online;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected RiderAvailabilityEntity() {
        // Required by JPA.
    }

    public RiderAvailabilityEntity(Long userId, boolean online, Instant updatedAt) {
        this.userId = userId;
        this.online = online;
        this.updatedAt = updatedAt;
    }

    public Long getUserId() {
        return userId;
    }

    public boolean isOnline() {
        return online;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setOnline(boolean online, Instant updatedAt) {
        this.online = online;
        this.updatedAt = updatedAt;
    }
}
