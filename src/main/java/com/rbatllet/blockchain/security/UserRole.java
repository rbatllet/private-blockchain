package com.rbatllet.blockchain.security;

/**
 * Role-based access control levels for blockchain users.
 *
 * <p>This enum defines the hierarchical role system for the blockchain, implementing
 * the Principle of Least Privilege. Each role has specific permissions defined by
 * the permission matrix in the RBAC documentation.</p>
 *
 * <p><strong>Role Hierarchy (by privilege level):</strong></p>
 * <ol>
 *   <li><strong>SUPER_ADMIN (100)</strong> - Genesis admin with full system control</li>
 *   <li><strong>ADMIN (50)</strong> - Delegated administrators who manage users</li>
 *   <li><strong>USER (10)</strong> - Regular users who create and search blocks</li>
 *   <li><strong>READ_ONLY (1)</strong> - View-only access for auditing</li>
 * </ol>
 *
 * @see com.rbatllet.blockchain.entity.AuthorizedKey
 * @see com.rbatllet.blockchain.service.UserFriendlyEncryptionAPI
 * @since 1.0.6
 */
public enum UserRole {
    /**
     * Super Administrator - Genesis admin with full system control.
     * Only one per blockchain.
     *
     * <p><strong>Permissions:</strong></p>
     * <ul>
     *   <li>✅ Create ADMIN, USER, READ_ONLY users</li>
     *   <li>✅ Revoke ADMIN, USER, READ_ONLY users</li>
     *   <li>✅ Unlimited rollback operations</li>
     *   <li>✅ All block and administrative operations</li>
     * </ul>
     */
    SUPER_ADMIN("Super Administrator", "Full system control", 100),

    /**
     * Administrator - Delegated admin who can manage users.
     * Can create USER accounts but not other ADMINs.
     *
     * <p><strong>Permissions:</strong></p>
     * <ul>
     *   <li>✅ Create USER, READ_ONLY users (cannot create ADMIN)</li>
     *   <li>✅ Revoke USER, READ_ONLY users (cannot revoke ADMIN)</li>
     *   <li>⚠️ Limited rollback (max 100 blocks)</li>
     *   <li>✅ Most block and administrative operations</li>
     * </ul>
     */
    ADMIN("Administrator", "User management and operations", 50),

    /**
     * Regular User - Standard blockchain user.
     * Can create blocks and search data, but cannot manage users.
     *
     * <p><strong>Permissions:</strong></p>
     * <ul>
     *   <li>✅ Create and manage own blocks</li>
     *   <li>✅ Search and decrypt own encrypted blocks</li>
     *   <li>❌ Cannot create or revoke users</li>
     *   <li>❌ Cannot perform administrative operations</li>
     * </ul>
     */
    USER("User", "Create and search blocks", 10),

    /**
     * Read-Only - View-only access for auditing.
     * Can view and export data, but cannot create or modify.
     *
     * <p><strong>Permissions:</strong></p>
     * <ul>
     *   <li>✅ View all blockchain data</li>
     *   <li>✅ Export blockchain (read-only)</li>
     *   <li>❌ Cannot create or modify blocks</li>
     *   <li>❌ Cannot create or revoke users</li>
     * </ul>
     *
     * <p><strong>Status:</strong> 📋 OPTIONAL - Can be added in future versions</p>
     */
    READ_ONLY("Read Only", "View and audit only", 1);

    private final String displayName;
    private final String description;
    private final int privilegeLevel;  // For comparison (higher = more privileges)

    /**
     * Creates a UserRole with specified properties.
     *
     * @param displayName Human-readable role name
     * @param description Brief description of role capabilities
     * @param privilegeLevel Numeric privilege level (higher = more privileges)
     */
    UserRole(String displayName, String description, int privilegeLevel) {
        this.displayName = displayName;
        this.description = description;
        this.privilegeLevel = privilegeLevel;
    }

    /**
     * Gets the human-readable display name for this role.
     *
     * @return Display name (e.g., "Super Administrator")
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets the description of this role's capabilities.
     *
     * @return Description (e.g., "Full system control")
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the numeric privilege level for this role.
     * Higher values indicate more privileges.
     *
     * @return Privilege level (1-100)
     */
    public int getPrivilegeLevel() {
        return privilegeLevel;
    }

    // ========================================================================
    // Permission Check Methods
    // ========================================================================

    /**
     * Check if this role can create users of the specified target role.
     *
     * <p><strong>Permission Matrix (v1.0.6+):</strong></p>
     * <ul>
     *   <li><strong>SUPER_ADMIN</strong> can create: SUPER_ADMIN, ADMIN, USER, READ_ONLY</li>
     *   <li><strong>ADMIN</strong> can create: USER, READ_ONLY (cannot create ADMIN or SUPER_ADMIN)</li>
     *   <li><strong>USER</strong> cannot create any users</li>
     *   <li><strong>READ_ONLY</strong> cannot create any users</li>
     * </ul>
     *
     * <p><strong>Security Note:</strong> ADMIN cannot create other ADMINs or SUPER_ADMINs
     * to prevent privilege escalation attacks.</p>
     *
     * @param targetRole The role to assign to the new user
     * @return true if this role can create users with targetRole
     * @since 1.0.6
     */
    public boolean canCreateRole(UserRole targetRole) {
        if (this == SUPER_ADMIN) {
            // SUPER_ADMIN can create SUPER_ADMIN, ADMIN, USER, READ_ONLY (v1.0.6+)
            return targetRole == SUPER_ADMIN || targetRole == ADMIN || targetRole == USER || targetRole == READ_ONLY;
        }
        if (this == ADMIN) {
            // ADMIN can only create USER and READ_ONLY (cannot create ADMIN)
            return targetRole == USER || targetRole == READ_ONLY;
        }
        return false;  // USER and READ_ONLY cannot create users
    }

    /**
     * Check if this role can revoke the specified target role.
     *
     * <p><strong>Permission Matrix (v1.0.6+):</strong></p>
     * <ul>
     *   <li><strong>SUPER_ADMIN</strong> can revoke: SUPER_ADMIN, ADMIN, USER, READ_ONLY</li>
     *   <li><strong>ADMIN</strong> can revoke: USER, READ_ONLY (cannot revoke ADMIN or SUPER_ADMIN)</li>
     *   <li><strong>USER</strong> cannot revoke any users</li>
     *   <li><strong>READ_ONLY</strong> cannot revoke any users</li>
     * </ul>
     *
     * <p><strong>Security Note (v1.0.6+):</strong> While SUPER_ADMIN can revoke other SUPER_ADMINs,
     * the blockchain enforces an additional check in {@code Blockchain.revokeAuthorizedKey()} to prevent
     * revoking the last active SUPER_ADMIN (system lockout protection).</p>
     *
     * @param targetRole The role of the user to revoke
     * @return true if this role can revoke users with targetRole
     * @since 1.0.6
     */
    public boolean canRevokeRole(UserRole targetRole) {
        if (this == SUPER_ADMIN) {
            // SUPER_ADMIN can revoke anyone (including SUPER_ADMIN) - v1.0.6+
            // Additional protection: Cannot revoke last active SUPER_ADMIN (checked in Blockchain)
            return true;
        }
        if (this == ADMIN) {
            // ADMIN can only revoke USER and READ_ONLY (cannot revoke ADMIN)
            return targetRole == USER || targetRole == READ_ONLY;
        }
        return false;  // USER and READ_ONLY cannot revoke users
    }

    /**
     * Check if this role can modify blocks (create, update, delete).
     *
     * <p><strong>Permission Matrix:</strong></p>
     * <ul>
     *   <li><strong>SUPER_ADMIN</strong> - ✅ Yes</li>
     *   <li><strong>ADMIN</strong> - ✅ Yes</li>
     *   <li><strong>USER</strong> - ✅ Yes</li>
     *   <li><strong>READ_ONLY</strong> - ❌ No (view-only)</li>
     * </ul>
     *
     * @return true if this role can modify blocks
     * @since 1.0.6
     */
    public boolean canModifyBlocks() {
        return this != READ_ONLY;
    }

    /**
     * Check if this role can perform rollback operations.
     *
     * <p><strong>Permission Matrix:</strong></p>
     * <ul>
     *   <li><strong>SUPER_ADMIN</strong> - ✅ Unlimited rollback</li>
     *   <li><strong>ADMIN</strong> - ⚠️ Limited to 100 blocks</li>
     *   <li><strong>USER</strong> - ❌ No rollback</li>
     *   <li><strong>READ_ONLY</strong> - ❌ No rollback</li>
     * </ul>
     *
     * <p><strong>Example Usage:</strong></p>
     * <pre>
     * UserRole role = blockchain.getUserRole(publicKey);
     * if (!role.canRollback(150)) {
     *     throw new SecurityException("Cannot rollback 150 blocks with role " + role);
     * }
     * </pre>
     *
     * @param blockCount Number of blocks to rollback
     * @return true if this role can rollback the specified number of blocks
     * @since 1.0.6
     */
    public boolean canRollback(long blockCount) {
        if (this == SUPER_ADMIN) {
            return true;  // No limit
        }
        if (this == ADMIN) {
            return blockCount <= 100;  // Limited to 100 blocks
        }
        return false;  // USER and READ_ONLY cannot rollback
    }

    /**
     * Check if this role has administrative privileges.
     *
     * <p>Administrative roles (SUPER_ADMIN and ADMIN) have access to user management
     * and system configuration operations.</p>
     *
     * @return true if this role is SUPER_ADMIN or ADMIN
     * @since 1.0.6
     */
    public boolean isAdmin() {
        return this == SUPER_ADMIN || this == ADMIN;
    }

    /**
     * Check if this role has higher privileges than the target role.
     *
     * <p>Uses the numeric privilegeLevel for comparison:
     * SUPER_ADMIN (100) &gt; ADMIN (50) &gt; USER (10) &gt; READ_ONLY (1)</p>
     *
     * <p><strong>Example Usage:</strong></p>
     * <pre>
     * if (callerRole.hasHigherPrivilegesThan(targetRole)) {
     *     // Caller can perform operation on target
     * }
     * </pre>
     *
     * @param other The other role to compare against
     * @return true if this role has higher privilege level than other
     * @since 1.0.6
     */
    public boolean hasHigherPrivilegesThan(UserRole other) {
        return this.privilegeLevel > other.privilegeLevel;
    }

    @Override
    public String toString() {
        return displayName + " (" + name() + ")";
    }
}
