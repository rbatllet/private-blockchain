package com.rbatllet.blockchain.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "authorized_keys")
public class AuthorizedKey {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "public_key", columnDefinition = "TEXT", unique = true, nullable = false)
    private String publicKey;
    
    @Column(name = "owner_name", length = 100)
    private String ownerName;
    
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
    
    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;

    // Constructors
    public AuthorizedKey() {
        this.createdAt = java.time.LocalDateTime.now();
    }

    public AuthorizedKey(String publicKey, String ownerName) {
        this();
        this.publicKey = publicKey;
        this.ownerName = ownerName;
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

    @Override
    public String toString() {
        return "AuthorizedKey{" +
                "id=" + id +
                ", ownerName='" + ownerName + '\'' +
                ", isActive=" + isActive +
                ", createdAt=" + createdAt +
                '}';
    }
}