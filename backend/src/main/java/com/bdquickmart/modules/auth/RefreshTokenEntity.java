package com.bdquickmart.modules.auth;

import com.bdquickmart.modules.user.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

/**
 * Persisted state for a single issued refresh token.
 *
 * <p>Only the SHA-256 hash of the raw token is stored, never the token itself.
 * Tokens belonging to one login session share a {@code familyId}; reusing a
 * revoked token (replay) revokes every live member of that family.
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshTokenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Hex SHA-256 of the raw refresh token. */
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "family_id", nullable = false, length = 36)
    private String familyId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    /** Hash of the token that replaced this one during rotation. */
    @Column(name = "replaced_by_hash", length = 64)
    private String replacedByHash;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    protected RefreshTokenEntity() {
        // Required by JPA.
    }

    public RefreshTokenEntity(String tokenHash, String familyId, UserEntity user,
                              Instant expiresAt, Instant createdAt) {
        this.tokenHash = tokenHash;
        this.familyId = familyId;
        this.user = user;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public String getFamilyId() {
        return familyId;
    }

    public UserEntity getUser() {
        return user;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    public String getReplacedByHash() {
        return replacedByHash;
    }

    public void setReplacedByHash(String replacedByHash) {
        this.replacedByHash = replacedByHash;
    }

    public Long getVersion() {
        return version;
    }
}
