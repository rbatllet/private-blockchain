package com.rbatllet.blockchain.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "authorized_keys")
public class AuthorizedKey {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "public_key", columnDefinition = "TEXT", nullable = false)
    private String publicKey;
    
    @Column(name = "owner_name", length = 100)
    private String ownerName;
    
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
    
    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;
    
    @Column(name = "revoked_at")
    private java.time.LocalDateTime revokedAt;

    // Constructors
    public AuthorizedKey() {
        // Don't set timestamp here - it should be set explicitly for thread safety
        // this.createdAt = java.time.LocalDateTime.now();
    }

    public AuthorizedKey(String publicKey, String ownerName) {
        this.publicKey = publicKey;
        this.ownerName = ownerName;
        // Don't set timestamp here - it should be set explicitly for thread safety
    }
    
    /**
     * Thread-safe constructor with explicit timestamp
     * FIXED: Allows setting specific creation time to avoid race conditions
     */
    public AuthorizedKey(String publicKey, String ownerName, java.time.LocalDateTime createdAt) {
        this.publicKey = publicKey;
        this.ownerName = ownerName;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPublicKey() { return publicKey; }
    public void setPublicKey(String publicKey) { this.publicKey = publicKey; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public java.time.LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.time.LocalDateTime createdAt) { this.createdAt = createdAt; }

    public java.time.LocalDateTime getRevokedAt() { return revokedAt; }
    public void setRevokedAt(java.time.LocalDateTime revokedAt) { this.revokedAt = revokedAt; }

    /**
     * Checks if the key was active at a specific point in time
     * This is crucial for historical validation of blocks
     * FIXED: Correct temporal logic
     */
    public boolean wasActiveAt(java.time.LocalDateTime timestamp) {
        // Key must have been created before the timestamp
        if (createdAt == null || timestamp.isBefore(createdAt)) {
            return false;
        }
        
        // If never revoked, it was active from creation until revocation (or still active)
        if (revokedAt == null) {
            return true; // Key was active from creation onwards
        }
        
        // If revoked, check if timestamp was before revocation
        return timestamp.isBefore(revokedAt);
    }

    @Override
    public String toString() {
        return "AuthorizedKey{" +
                "id=" + id +
                ", publicKey='" + (publicKey != null ? publicKey.substring(0, Math.min(20, publicKey.length())) + "..." : "null") + '\'' +
                ", ownerName='" + ownerName + '\'' +
                ", isActive=" + isActive +
                ", createdAt=" + createdAt +
                ", revokedAt=" + revokedAt +
                '}';
    }
}