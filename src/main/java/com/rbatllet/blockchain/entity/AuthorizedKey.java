package com.rbatllet.blockchain.entity;

import com.rbatllet.blockchain.security.UserRole;
import jakarta.persistence.*;

/**
 * JPA Entity representing an authorized key in the blockchain.
 *
 * <p><strong>RBAC (v1.0.6+):</strong> Each authorized key has a {@link UserRole} that
 * determines their permissions in the system.</p>
 *
 * @since 1.0.0
 */
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

    /**
     * Role-based access control level (v1.0.6+).
     * Defaults to USER for regular users.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 20, nullable = false)
    private UserRole role = UserRole.USER;  // Default role

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;

    @Column(name = "revoked_at")
    private java.time.LocalDateTime revokedAt;

    /**
     * Username of who created this authorized key (v1.0.6+).
     * Null for genesis admin (created by system).
     */
    @Column(name = "created_by", length = 100)
    private String createdBy;

    // Constructors

    /**
     * Default constructor for JPA.
     */
    public AuthorizedKey() {
        // Don't set timestamp here - it should be set explicitly for thread safety
        // Default role is USER (set in field declaration)
    }

    /**
     * Full constructor with RBAC support (v1.0.6+).
     *
     * @param publicKey The public key string
     * @param ownerName The owner name
     * @param role The user's role
     * @param createdBy Who created this user (null for genesis admin)
     * @param createdAt Explicit creation timestamp
     * @since 1.0.6
     */
    public AuthorizedKey(String publicKey, String ownerName, UserRole role, String createdBy, java.time.LocalDateTime createdAt) {
        this.publicKey = publicKey;
        this.ownerName = ownerName;
        this.role = role;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPublicKey() { return publicKey; }
    public void setPublicKey(String publicKey) { this.publicKey = publicKey; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) {
        // Validate database column limit (100 chars)
        if (ownerName != null && ownerName.length() > 100) {
            throw new IllegalArgumentException(
                "ownerName exceeds maximum length of 100 characters (got: " +
                ownerName.length() + "). Please use shorter owner names."
            );
        }
        this.ownerName = ownerName;
    }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public java.time.LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.time.LocalDateTime createdAt) { this.createdAt = createdAt; }

    public java.time.LocalDateTime getRevokedAt() { return revokedAt; }
    public void setRevokedAt(java.time.LocalDateTime revokedAt) { this.revokedAt = revokedAt; }

    /**
     * Gets the user's role.
     *
     * @return The user's role (never null, defaults to USER)
     * @since 1.0.6
     */
    public UserRole getRole() { return role; }

    /**
     * Sets the user's role.
     *
     * @param role The role to set (must not be null)
     * @throws IllegalArgumentException if role is null
     * @since 1.0.6
     */
    public void setRole(UserRole role) {
        if (role == null) {
            throw new IllegalArgumentException("Role cannot be null. Use UserRole.USER as default.");
        }
        this.role = role;
    }

    /**
     * Gets who created this authorized key.
     *
     * @return The creator's username, or null for genesis admin (created by system)
     * @since 1.0.6
     */
    public String getCreatedBy() { return createdBy; }

    /**
     * Sets who created this authorized key.
     *
     * @param createdBy The creator's username (null for genesis admin)
     * @since 1.0.6
     */
    public void setCreatedBy(String createdBy) {
        // Validate database column limit (100 chars)
        if (createdBy != null && createdBy.length() > 100) {
            throw new IllegalArgumentException(
                "createdBy exceeds maximum length of 100 characters (got: " +
                createdBy.length() + "). Please use shorter usernames."
            );
        }
        this.createdBy = createdBy;
    }

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
                ", role=" + role +
                ", isActive=" + isActive +
                ", createdAt=" + createdAt +
                ", revokedAt=" + revokedAt +
                ", createdBy='" + createdBy + '\'' +
                '}';
    }
}