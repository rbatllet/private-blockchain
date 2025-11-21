# 🔐 Role-Based Access Control (RBAC) System

**Document Version:** 1.1
**Date:** 2025-01-07
**Status:** ✅ **IMPLEMENTED** (v1.0.6)
**Version:** 1.0.6

---

> **🔄 CODE UPDATE (v1.0.6+)**: Code examples in this document use `revokeAuthorizedKey()` which now throws exceptions instead of returning `false`. Wrap calls in try-catch blocks. See [Exception-Based Error Handling Guide](EXCEPTION_BASED_ERROR_HANDLING_V1_0_6.md).

---

## Table of Contents

1. [Overview](#overview)
2. [Role Definitions](#role-definitions)
3. [Permission Matrix](#permission-matrix)
4. [Implementation Details](#implementation-details)
5. [Migration Strategy](#migration-strategy)
6. [Security Considerations](#security-considerations)
7. [Use Cases](#use-cases)

---

## Overview

### 🔒 CRITICAL SECURITY: Bootstrap Admin Protection (v1.0.6)

**IMPORTANT:** The bootstrap mechanism has strict security validation to prevent multiple SUPER_ADMIN creation:

```java
// ✅ CORRECT: Bootstrap (ONLY when no users exist)
blockchain.addAuthorizedKey(
    publicKeyString,
    "BOOTSTRAP_ADMIN",
    null,  // callerKeyPair=null ONLY allowed for bootstrap
    UserRole.SUPER_ADMIN  // MUST be SUPER_ADMIN
);
```

**Security Rules:**

1. **`callerKeyPair=null` is ONLY valid for bootstrap**
   - ✅ Allowed ONLY when `getAuthorizedKeyCount() == 0` (no users exist)
   - ❌ After bootstrap, ALL operations require caller credentials
   - ❌ `SecurityException` if attempted when users exist

2. **Bootstrap MUST create SUPER_ADMIN**
   - ✅ `targetRole` MUST be `UserRole.SUPER_ADMIN`
   - ❌ Any other role (`ADMIN`, `USER`, `READ_ONLY`) → `SecurityException`
   - ❌ Cannot create ADMIN with `callerKeyPair=null` (prevents unauthorized admin creation)

3. **Multiple SUPER_ADMINs allowed with protection (v1.0.6+)**
   - ✅ Bootstrap admin is the first SUPER_ADMIN
   - ✅ SUPER_ADMIN can create additional SUPER_ADMINs
   - ✅ At least ONE active SUPER_ADMIN must exist at all times (security protection)
   - ❌ Cannot revoke the last active SUPER_ADMIN (prevents lockout)
   - ✅ Bootstrap admin keys stored in `./keys/genesis-admin.{private,public}`

**Example (INCORRECT - will throw SecurityException):**

```java
// ❌ WRONG: Attempting to create ADMIN with bootstrap
blockchain.addAuthorizedKey(
    publicKey,
    "Alice",
    null,  // callerKeyPair=null not allowed for ADMIN!
    UserRole.ADMIN  // SecurityException: Bootstrap ONLY allows SUPER_ADMIN
);

// ❌ WRONG: Attempting to create USER with bootstrap
blockchain.addAuthorizedKey(
    publicKey,
    "Bob",
    null,  // callerKeyPair=null not allowed for USER!
    UserRole.USER  // SecurityException: Bootstrap ONLY allows SUPER_ADMIN
);
```

**Correct Production Pattern:**

```java
// 1. Load bootstrap admin keys (created automatically at first init)
KeyPair bootstrapKeys = KeyFileLoader.loadKeyPairFromFiles(
    "./keys/genesis-admin.private",
    "./keys/genesis-admin.public"
);

// 2. Register bootstrap admin in blockchain (REQUIRED!)
blockchain.createBootstrapAdmin(
    CryptoUtil.publicKeyToString(bootstrapKeys.getPublic()),
    "BOOTSTRAP_ADMIN"
);

// 3. Use bootstrap credentials to create ADMIN
blockchain.addAuthorizedKey(
    alicePublicKey,
    "Alice",
    bootstrapKeys,  // ✅ Valid caller credentials
    UserRole.ADMIN
);

// 3. Use ADMIN credentials to create USER
blockchain.addAuthorizedKey(
    bobPublicKey,
    "Bob",
    aliceKeys,  // ✅ Valid ADMIN credentials
    UserRole.USER
);
```

---

### Current Problem (Before RBAC)

The blockchain currently uses a **flat authorization model** where any authorized user can create new users. This violates the **Principle of Least Privilege** and creates security risks:

```
Bootstrap Admin → creates Alice (authorized)
    ↓
Alice → creates Bob (authorized)
    ↓
Bob → creates Charlie (authorized)
    ↓
Charlie → creates unlimited attackers ⚠️
```

**Problem:** If any authorized user is compromised (stolen keys, insider threat), they can:
- Create unlimited malicious users
- Propagate attacks without containment
- Difficult to trace authorization chain

### Proposed Solution: Role-Based Access Control

Implement a **3-role hierarchical system** with clear permission boundaries:

```
SUPER_ADMIN (Bootstrap)
    ├─ Full system control
    ├─ Can create ADMIN users
    └─ Can revoke any user

ADMIN (Delegated administrators)
    ├─ Can create USER accounts
    ├─ Can revoke USER accounts (not ADMIN)
    ├─ Operational management
    └─ Cannot create other ADMINs

USER (Regular users)
    ├─ Create and manage blocks
    ├─ Search and decrypt data
    ├─ Cannot manage users
    └─ Limited administrative access
```

---

## Role Definitions

### 1. SUPER_ADMIN (Bootstrap Administrator)

**Description:** The founding administrator with complete system control. Multiple SUPER_ADMINs are allowed, but at least one must remain active at all times (v1.0.6+).

**Characteristics:**
- ✅ Bootstrap admin is the first SUPER_ADMIN (created at initialization)
- ✅ SUPER_ADMIN can create additional SUPER_ADMINs for redundancy
- ✅ At least ONE active SUPER_ADMIN must exist (system protection)
- ❌ Cannot revoke the last active SUPER_ADMIN (prevents lockout)
- ✅ Keys stored in `./keys/genesis-admin.{private,public}` (bootstrap admin)
- ✅ Username: `BOOTSTRAP_ADMIN` (bootstrap admin, by convention)
- ✅ Can delegate admin privileges

**Use Cases:**
- System initialization and bootstrap
- Creating first administrators
- Emergency recovery operations
- System-wide configuration changes
- Ultimate authority for critical operations

**Security Notes:**
- ⚠️ Private key must be stored in HSM or secure vault
- ⚠️ Should be used sparingly (delegate to ADMIN for routine tasks)
- ⚠️ Compromise of SUPER_ADMIN = complete system compromise
- ℹ️ **IMPORTANT (v1.0.6+ BMEK):** SUPER_ADMIN keys are for **AUTHORIZATION only** (RBAC), NOT encryption
- ℹ️ Blockchain encryption uses **BMEK** (Blockchain Master Encryption Key), independent of all users
- ℹ️ Revoking SUPER_ADMIN does **NOT** affect data encryption
- ℹ️ See `BLOCKCHAIN_MASTER_ENCRYPTION_KEY.md` for encryption architecture

---

### 2. ADMIN (Administrator)

**Description:** Delegated administrators who manage day-to-day operations and user accounts.

**Characteristics:**
- ✅ Created by SUPER_ADMIN
- ✅ Can be revoked by SUPER_ADMIN
- ✅ Multiple ADMINs can exist
- ✅ Cannot create other ADMINs (prevents privilege escalation)
- ✅ Suitable for IT staff, system managers

**Use Cases:**
- Creating user accounts for employees
- Revoking compromised user accounts
- System maintenance and monitoring
- Backup and restore operations
- Limited rollback operations (max 100 blocks)

**Security Notes:**
- ⚠️ ADMIN keys should be rotated periodically
- ⚠️ Each admin should have individual credentials (no shared accounts)
- ⚠️ Compromise of ADMIN = limited blast radius (cannot create admins)

---

### 3. USER (Regular User)

**Description:** Standard blockchain users who create and interact with data.

**Characteristics:**
- ✅ Created by SUPER_ADMIN or ADMIN
- ✅ Can be revoked by SUPER_ADMIN or ADMIN
- ✅ Default role for new users
- ✅ No user management privileges
- ✅ Suitable for employees, application users

**Use Cases:**
- Creating blockchain blocks
- Storing encrypted data
- Searching and retrieving data
- Decrypting own encrypted blocks
- Viewing blockchain analytics

**Security Notes:**
- ⚠️ Compromised USER cannot create new users (attack containment)
- ⚠️ Can only decrypt own encrypted blocks (requires password)
- ⚠️ Cannot perform administrative operations

---

### 4. READ_ONLY (Future - Optional)

**Description:** Audit and compliance role with view-only access.

**Status:** 📋 **OPTIONAL** - Can be added in future versions

**Characteristics:**
- ✅ Can view all blockchain data
- ✅ Can export blockchain (backup)
- ❌ Cannot create or modify blocks
- ❌ Cannot create users
- ✅ Suitable for auditors, compliance officers

**Use Cases:**
- Compliance audits
- Read-only reporting
- System monitoring
- Data analysis
- Backup operations

---

## Permission Matrix

### User Management Operations

| Operation | SUPER_ADMIN | ADMIN | USER | READ_ONLY |
|-----------|-------------|-------|------|-----------|
| **Create USER** | ✅ Yes | ✅ Yes | ❌ No | ❌ No |
| **Create ADMIN** | ✅ Yes | ❌ No | ❌ No | ❌ No |
| **Create SUPER_ADMIN** | ✅ Yes | ❌ No | ❌ No | ❌ No |
| **Revoke USER** | ✅ Yes | ✅ Yes | ❌ No | ❌ No |
| **Revoke ADMIN** | ✅ Yes | ❌ No | ❌ No | ❌ No |
| **Revoke SUPER_ADMIN** | ⚠️ Yes* | ❌ No | ❌ No | ❌ No |
| **View authorized keys** | ✅ Yes | ✅ Yes | ✅ Yes** | ✅ Yes** |
| **Load credentials** | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes |

**\*** SUPER_ADMIN can revoke another SUPER_ADMIN, but cannot revoke the last active SUPER_ADMIN (system protection)
**\*\*** Users can only view, not modify

---

### Block Operations

| Operation | SUPER_ADMIN | ADMIN | USER | READ_ONLY |
|-----------|-------------|-------|------|-----------|
| **Add block** | ✅ Yes | ✅ Yes | ✅ Yes | ❌ No |
| **Add encrypted block** | ✅ Yes | ✅ Yes | ✅ Yes | ❌ No |
| **Store secret** | ✅ Yes | ✅ Yes | ✅ Yes | ❌ No |
| **Store off-chain data** | ✅ Yes | ✅ Yes | ✅ Yes | ❌ No |
| **Update block metadata** | ✅ Yes | ✅ Yes | ⚠️ Own only** | ❌ No |
| **Get block** | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes |
| **Search blocks** | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes |
| **Decrypt block** | ✅ Yes | ✅ Yes | ⚠️ Own only*** | ❌ No |

**\*\*** USER can only update metadata of blocks they created
**\*\*\*** USER can only decrypt blocks they encrypted (requires password)

---

### Administrative Operations

| Operation | SUPER_ADMIN | ADMIN | USER | READ_ONLY |
|-----------|-------------|-------|------|-----------|
| **Export blockchain** | ✅ Yes | ✅ Yes | ❌ No | ✅ Yes (read-only) |
| **Import blockchain** | ✅ Yes | ✅ Yes | ❌ No | ❌ No |
| **Rollback blocks** | ✅ Unlimited | ⚠️ Max 100**** | ❌ No | ❌ No |
| **Validate chain** | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes |
| **Verify off-chain integrity** | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes |
| **Clear and reinitialize** | ✅ Yes | ❌ No | ❌ No | ❌ No |
| **Recover corrupted chain** | ✅ Yes | ⚠️ Requires approval | ❌ No | ❌ No |
| **Delete authorized key** | ✅ Yes | ❌ No | ❌ No | ❌ No |

**\*\*\*\*** ADMIN can rollback maximum 100 blocks (configurable). SUPER_ADMIN has no limit.

---

### Search and Query Operations

| Operation | SUPER_ADMIN | ADMIN | USER | READ_ONLY |
|-----------|-------------|-------|------|-----------|
| **Search by content** | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes |
| **Search by category** | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes |
| **Search by metadata** | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes |
| **Search by time range** | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes |
| **Search by signer** | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes |
| **Exhaustive search** | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes |
| **Search encrypted content** | ✅ Yes | ✅ Yes | ⚠️ Own only | ❌ No |

---

### System Monitoring

| Operation | SUPER_ADMIN | ADMIN | USER | READ_ONLY |
|-----------|-------------|-------|------|-----------|
| **Get blockchain summary** | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes |
| **Get validation report** | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes |
| **Get performance metrics** | ✅ Yes | ✅ Yes | ⚠️ Limited | ✅ Yes |
| **Generate status report** | ✅ Yes | ✅ Yes | ❌ No | ✅ Yes |
| **View audit logs** | ✅ Yes | ✅ Yes | ❌ No | ✅ Yes |

---

## Implementation Details

### Database Schema Changes

#### AuthorizedKey Entity Update

```java
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

    // ✨ NEW FIELD
    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 20, nullable = false)
    private UserRole role = UserRole.USER;  // Default role

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "created_by")  // Optional: track who created this user
    private String createdBy;

    // Constructors
    public AuthorizedKey() {
        // Default constructor for JPA
    }

    // Full constructor with RBAC support (v1.0.6+)
    public AuthorizedKey(String publicKey, String ownerName, UserRole role,
                        String createdBy, LocalDateTime createdAt) {
        this.publicKey = publicKey;
        this.ownerName = ownerName;
        this.role = role;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    // Getters and setters...
}
```

#### UserRole Enum

```java
package com.rbatllet.blockchain.security;

/**
 * Role-based access control levels for blockchain users.
 *
 * @since 1.0.6
 */
public enum UserRole {
    /**
     * Super Administrator - Bootstrap admin with full system control.
     * Only one per blockchain.
     */
    SUPER_ADMIN("Super Administrator", "Full system control", 100),

    /**
     * Administrator - Delegated admin who can manage users.
     * Can create USER accounts but not other ADMINs.
     */
    ADMIN("Administrator", "User management and operations", 50),

    /**
     * Regular User - Standard blockchain user.
     * Can create blocks and search data, but cannot manage users.
     */
    USER("User", "Create and search blocks", 10),

    /**
     * Read-Only - View-only access for auditing.
     * Can view and export data, but cannot create or modify.
     */
    READ_ONLY("Read Only", "View and audit only", 1);

    private final String displayName;
    private final String description;
    private final int privilegeLevel;  // For comparison (higher = more privileges)

    UserRole(String displayName, String description, int privilegeLevel) {
        this.displayName = displayName;
        this.description = description;
        this.privilegeLevel = privilegeLevel;
    }

    // Getters
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public int getPrivilegeLevel() { return privilegeLevel; }

    // Permission check methods

    /**
     * Check if this role can create users of the specified target role.
     */
    public boolean canCreateRole(UserRole targetRole) {
        if (this == SUPER_ADMIN) {
            // SUPER_ADMIN can create SUPER_ADMIN, ADMIN, USER, and READ_ONLY (v1.0.6+)
            return targetRole == SUPER_ADMIN || targetRole == ADMIN || targetRole == USER || targetRole == READ_ONLY;
        }
        if (this == ADMIN) {
            // ADMIN can only create USER and READ_ONLY
            return targetRole == USER || targetRole == READ_ONLY;
        }
        return false;  // USER and READ_ONLY cannot create users
    }

    /**
     * Check if this role can revoke the specified target role.
     *
     * <p><strong>Note:</strong> This checks role permissions only. Additional validation
     * (e.g., preventing revocation of the last active SUPER_ADMIN) is performed at the
     * blockchain level in {@code Blockchain.revokeAuthorizedKey()}.</p>
     */
    public boolean canRevokeRole(UserRole targetRole) {
        if (this == SUPER_ADMIN) {
            // SUPER_ADMIN can revoke anyone (including SUPER_ADMIN) - v1.0.6+
            // Additional protection: Cannot revoke last active SUPER_ADMIN (checked in Blockchain)
            return true;
        }
        if (this == ADMIN) {
            // ADMIN can only revoke USER and READ_ONLY
            return targetRole == USER || targetRole == READ_ONLY;
        }
        return false;
    }

    /**
     * Check if this role can modify blocks.
     */
    public boolean canModifyBlocks() {
        return this != READ_ONLY;
    }

    /**
     * Check if this role can perform rollback operations.
     *
     * @param blockCount Number of blocks to rollback
     * @return true if this role can rollback the specified number of blocks
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
     * Check if this role can perform administrative operations.
     */
    public boolean isAdmin() {
        return this == SUPER_ADMIN || this == ADMIN;
    }

    /**
     * Check if this role has higher privileges than the target role.
     */
    public boolean hasHigherPrivilegesThan(UserRole other) {
        return this.privilegeLevel > other.privilegeLevel;
    }
}
```

---

### API Method Updates

#### Blockchain.java

```java
/**
 * Get the role of a user by their public key.
 *
 * @param publicKey The user's public key
 * @return The user's role, or null if not authorized
 * @since 1.0.6
 */
public UserRole getUserRole(String publicKey) {
    long stamp = GLOBAL_BLOCKCHAIN_LOCK.readLock();
    try {
        AuthorizedKey key = authorizedKeyDAO.getAuthorizedKeyByPublicKey(publicKey);
        return (key != null && key.isActive()) ? key.getRole() : null;
    } finally {
        GLOBAL_BLOCKCHAIN_LOCK.unlockRead(stamp);
    }
}

/**
 * Check if a user has a specific role.
 *
 * @param publicKey The user's public key
 * @param requiredRole The required role
 * @return true if the user has the required role or higher
 * @since 1.0.6
 */
public boolean hasRole(String publicKey, UserRole requiredRole) {
    UserRole userRole = getUserRole(publicKey);
    return userRole != null && userRole.getPrivilegeLevel() >= requiredRole.getPrivilegeLevel();
}
```

#### UserFriendlyEncryptionAPI.java

```java
/**
 * Create a new user with USER role.
 *
 * <p><strong>🔒 Security (v1.0.6+):</strong> Requires caller to be ADMIN or SUPER_ADMIN.</p>
 *
 * @param username Username for the new user
 * @return KeyPair for the new user
 * @throws SecurityException if caller is not ADMIN or SUPER_ADMIN
 * @since 1.0.0
 */
public KeyPair createUser(String username) {
    return createUserWithRole(username, UserRole.USER);
}

/**
 * Create a new administrator.
 *
 * <p><strong>🔒 Security (v1.0.6+):</strong> Requires caller to be SUPER_ADMIN.</p>
 *
 * @param username Username for the new admin
 * @return KeyPair for the new admin
 * @throws SecurityException if caller is not SUPER_ADMIN
 * @since 1.0.6
 */
public KeyPair createAdmin(String username) {
    return createUserWithRole(username, UserRole.ADMIN);
}

/**
 * Internal method to create user with specific role.
 */
private KeyPair createUserWithRole(String username, UserRole targetRole) {
    validateInputData(username, 256, "Username");

    synchronized (credentialsLock) {
        // Get caller's credentials
        if (defaultKeyPair.get() == null || defaultUsername.get() == null) {
            throw new SecurityException(
                "❌ AUTHORIZATION REQUIRED: Must set authorized credentials before creating users."
            );
        }

        // Get caller's role
        String callerPublicKey = CryptoUtil.publicKeyToString(defaultKeyPair.get().getPublic());
        UserRole callerRole = blockchain.getUserRole(callerPublicKey);

        if (callerRole == null) {
            throw new SecurityException(
                "❌ AUTHORIZATION REQUIRED: Only authorized users can create new users.\n" +
                "Current user '" + defaultUsername.get() + "' is not authorized."
            );
        }

        // Check if caller can create this role
        if (!callerRole.canCreateRole(targetRole)) {
            throw new SecurityException(
                "❌ AUTHORIZATION REQUIRED: User with role '" + callerRole +
                "' cannot create users with role '" + targetRole + "'.\n" +
                "Current user: " + defaultUsername.get()
            );
        }
    }

    // Create user with specified role
    try {
        KeyPair keyPair = CryptoUtil.generateKeyPair();
        String publicKeyString = CryptoUtil.publicKeyToString(keyPair.getPublic());
        blockchain.addAuthorizedKey(publicKeyString, username, defaultKeyPair.get(), targetRole);
        logger.info("✅ User '{}' created with role {} by '{}'",
                    username, targetRole, defaultUsername.get());
        return keyPair;
    } catch (Exception e) {
        throw new RuntimeException("Failed to create user: " + e.getMessage(), e);
    }
}
```

---

## Migration Strategy

### Phase 1: Database Migration

**Step 1: Add `role` column to `authorized_keys` table**

```sql
-- SQLite
ALTER TABLE authorized_keys
ADD COLUMN role VARCHAR(20) DEFAULT 'USER' NOT NULL;

-- PostgreSQL
ALTER TABLE authorized_keys
ADD COLUMN role VARCHAR(20) DEFAULT 'USER' NOT NULL;

-- MySQL
ALTER TABLE authorized_keys
ADD COLUMN role VARCHAR(20) DEFAULT 'USER' NOT NULL;
```

**Step 2: Assign roles to existing users**

```sql
-- Set bootstrap admin as SUPER_ADMIN
UPDATE authorized_keys
SET role = 'SUPER_ADMIN'
WHERE owner_name = 'BOOTSTRAP_ADMIN'
  AND id = 1;  -- First authorized user

-- Set all other existing users as USER (conservative approach)
UPDATE authorized_keys
SET role = 'USER'
WHERE role IS NULL OR role = 'USER';
```

**Alternative (more permissive):**
```sql
-- Set all existing users as ADMIN (if they should retain privileges)
UPDATE authorized_keys
SET role = 'ADMIN'
WHERE id > 1 AND is_active = true;
```

---

### Phase 2: Code Updates

**Required Changes:**

1. ✅ Create `UserRole` enum
2. ✅ Update `AuthorizedKey` entity with `role` field
3. ✅ Update `Blockchain.addAuthorizedKey()` to accept role parameter
4. ✅ Add `Blockchain.getUserRole()` method
5. ✅ Update `UserFriendlyEncryptionAPI.createUser()` with role validation
6. ✅ Add `UserFriendlyEncryptionAPI.createAdmin()` method
7. ✅ Bootstrap admin now requires EXPLICIT `createBootstrapAdmin()` call (removed automatic creation for security)
8. ✅ Update all authorization checks to use role-based validation

---

### Phase 3: Testing

**Test Coverage Required:**

1. ✅ **Role Assignment Tests**
   - Bootstrap admin gets SUPER_ADMIN role
   - New users get USER role by default
   - createAdmin() assigns ADMIN role

2. ✅ **Permission Tests**
   - SUPER_ADMIN can create ADMIN
   - ADMIN can create USER (not ADMIN)
   - USER cannot create users
   - Role-specific operation tests

3. ✅ **Migration Tests**
   - Existing users retain access after migration
   - Role assignment is correct
   - No authorization loss

4. ✅ **Security Tests**
   - Privilege escalation prevention
   - Role validation bypass attempts
   - Cross-role operation denial

---

### Phase 4: Documentation Updates

**Documents to Update:**

1. ✅ API_GUIDE.md - Add role-based examples
2. ✅ SECURITY_GUIDE.md - Document role system
3. ✅ GETTING_STARTED.md - Update user creation examples
4. ✅ CLAUDE.md - Update Common Pitfalls section
5. ✅ VULNERABILITY_REPORT.md - Reference role system
6. ✅ All demo applications - Update to use role-aware methods

---

## Security Considerations

### 1. Privilege Escalation Prevention

**Threat:** Compromised ADMIN attempts to create another ADMIN to maintain persistence.

**Mitigation:**
```java
// UserRole.canCreateRole() prevents this
if (this == ADMIN) {
    return targetRole == USER || targetRole == READ_ONLY;  // Cannot create ADMIN
}
```

**Test Case:**
```java
@Test
void testAdminCannotCreateAdmin() {
    // Setup: Admin user
    KeyPair adminKeys = api.createAdmin("admin1");
    api.setDefaultCredentials("admin1", adminKeys);

    // Test: Admin tries to create another admin
    assertThrows(SecurityException.class, () -> {
        api.createAdmin("admin2");  // Should fail
    });
}
```

---

### 2. Attack Containment

**Before RBAC:**
```
Compromised USER → creates 100 malicious users → spreads attack
```

**After RBAC:**
```
Compromised USER → cannot create users → attack contained ✅
Compromised ADMIN → can create USER (not ADMIN) → limited spread ⚠️
Compromised SUPER_ADMIN → full compromise (protect keys!) 🔴
```

---

### 3. SUPER_ADMIN Protection

**Critical:** SUPER_ADMIN compromise = complete system compromise.

**Best Practices:**

1. ✅ **Hardware Security Module (HSM)**
   - Store SUPER_ADMIN private key in HSM
   - Require physical presence for key usage

2. ✅ **Key Ceremony**
   - Generate SUPER_ADMIN keys offline
   - Split key using Shamir's Secret Sharing
   - Require M-of-N key holders for operations

3. ✅ **Operational Security**
   - Use SUPER_ADMIN only for critical operations
   - Delegate routine tasks to ADMIN
   - Audit all SUPER_ADMIN operations

4. ✅ **Backup and Recovery**
   - Multiple secure backups of SUPER_ADMIN keys
   - Tested recovery procedures
   - Geographically distributed backups

---

### 4. Role Transition and Demotion

**Scenario:** Need to demote ADMIN to USER or revoke entirely.

**⚠️ NOT IMPLEMENTED (v1.0.6):** Role transition functionality is planned but not yet implemented.

**Proposed Implementation:**
```java
/**
 * Change a user's role (SUPER_ADMIN only).
 * ⚠️ NOT IMPLEMENTED - Proposed for future version
 */
public boolean changeUserRole(String username, UserRole newRole) {
    // Only SUPER_ADMIN can change roles
    UserRole callerRole = getCurrentUserRole();
    if (callerRole != UserRole.SUPER_ADMIN) {
        throw new SecurityException("Only SUPER_ADMIN can change user roles");
    }

    // Cannot change SUPER_ADMIN role
    AuthorizedKey user = blockchain.getAuthorizedKeyByOwner(username);
    if (user.getRole() == UserRole.SUPER_ADMIN) {
        throw new SecurityException("Cannot change SUPER_ADMIN role");
    }

    // Update role
    user.setRole(newRole);
    // ⚠️ updateAuthorizedKey() method does not exist - needs to be implemented
    return blockchain.updateAuthorizedKey(user);
}
```

**Current Workaround (v1.0.6):**
To change a user's role, you must:
1. Revoke the old authorization: `blockchain.revokeAuthorizedKey(publicKey)`
2. Create new authorization with new role: `blockchain.addAuthorizedKey(publicKey, username, callerKeyPair, newRole)`

---

### 5. Audit Trail

**Recommendation:** Track all role changes and user creations.

**Implementation:**
```java
@Entity
@Table(name = "role_change_audit")
public class RoleChangeAudit {
    @Id
    @GeneratedValue
    private Long id;

    private String username;
    private UserRole oldRole;
    private UserRole newRole;
    private String changedBy;
    private LocalDateTime changedAt;
    private String reason;  // Optional
}
```

---

## Use Cases

### Use Case 1: Enterprise Deployment

**Organization:** FinTech company with 500 employees

**Role Distribution:**
- 1 SUPER_ADMIN (CTO)
- 5 ADMIN (IT managers)
- 450 USER (employees)
- 44 READ_ONLY (compliance officers, auditors)

**Workflow:**
1. CTO (SUPER_ADMIN) creates 5 IT managers as ADMIN
2. IT managers create employee accounts as USER
3. IT managers grant READ_ONLY access to compliance team
4. Employees create business transactions (blocks)
5. Auditors review transactions via READ_ONLY access

**Security Benefit:**
- Compromised employee account → cannot create malicious users
- Compromised IT manager → can create USER (not ADMIN), limited damage
- CTO keys stored in HSM, rarely used

---

### Use Case 2: Healthcare Records

**Organization:** Hospital with patient records on blockchain

**Role Distribution:**
- 1 SUPER_ADMIN (Hospital CIO)
- 3 ADMIN (IT security team)
- 200 USER (doctors, nurses)
- 50 READ_ONLY (insurance companies, external auditors)

**Workflow:**
1. Doctors (USER) create encrypted patient records
2. Doctors can only decrypt their own patient records (password-based)
3. IT team (ADMIN) manages doctor accounts
4. Insurance (READ_ONLY) can view transaction hashes for auditing
5. CIO (SUPER_ADMIN) handles emergency recovery

**Compliance:**
- GDPR Article 32: Role-based access control implemented
- HIPAA: Minimum necessary access enforced
- Audit trail: All user creations and role changes logged

---

### Use Case 3: Supply Chain Tracking

**Organization:** Logistics company tracking shipments

**Role Distribution:**
- 1 SUPER_ADMIN (System owner)
- 10 ADMIN (Warehouse managers)
- 1000 USER (Drivers, warehouse staff)
- 20 READ_ONLY (Customers, partners)

**Workflow:**
1. Drivers (USER) scan packages and create blocks
2. Warehouse managers (ADMIN) create driver accounts
3. Customers (READ_ONLY) track shipments
4. System owner (SUPER_ADMIN) manages warehouse manager accounts

**Security Benefit:**
- Compromised driver account → cannot create fake driver accounts
- Customer access limited to viewing (cannot tamper)

---

## FAQ

### Q1: Can we have multiple SUPER_ADMINs?

**A:** Yes! (v1.0.6+) Multiple SUPER_ADMINs are supported with important safeguards:

**How it works:**
- ✅ Bootstrap admin is the first SUPER_ADMIN (created at initialization)
- ✅ SUPER_ADMIN can create additional SUPER_ADMINs using `addAuthorizedKey(publicKey, name, callerKeyPair, UserRole.SUPER_ADMIN)`
- ✅ At least ONE active SUPER_ADMIN must exist at all times
- ❌ Cannot revoke the last active SUPER_ADMIN (system protection to prevent lockout)

**Use cases:**
- **High availability:** Multiple SUPER_ADMINs for redundancy
- **Disaster recovery:** If one SUPER_ADMIN key is lost, others can continue operations
- **Geographic distribution:** Different SUPER_ADMINs in different locations

**Best practices:**
1. Keep 2-3 active SUPER_ADMINs maximum (avoid proliferation)
2. Store SUPER_ADMIN keys in secure locations (HSM, vaults)
3. Before revoking a SUPER_ADMIN, ensure at least one other is active
4. Document which SUPER_ADMINs are active and their key locations

---

### Q2: What happens if SUPER_ADMIN keys are lost?

**A:** This is catastrophic! Mitigation strategies:
1. ✅ **Backup keys** in multiple secure locations
2. ✅ **Key ceremony** with M-of-N threshold (Shamir's Secret Sharing)
3. ✅ **Emergency recovery procedure** (requires manual database access)
4. ✅ **Disaster recovery plan** documented and tested

**Emergency Recovery:**
```sql
-- EMERGENCY ONLY: Manually promote an ADMIN to SUPER_ADMIN
UPDATE authorized_keys
SET role = 'SUPER_ADMIN'
WHERE owner_name = 'emergency_admin'
  AND is_active = true;
```

⚠️ This bypasses security but may be necessary for business continuity.

---

### Q3: Can a USER decrypt blocks created by another USER?

**A:** Only if they have the encryption password.

- Blocks are signed by the creator (public key)
- Encrypted blocks require the password to decrypt
- Role system controls **who can create users**, not **who can decrypt data**

---

### Q4: Can we add custom roles (e.g., AUDITOR, OPERATOR)?

**A:** Yes! The enum design is extensible:

```java
public enum UserRole {
    SUPER_ADMIN(...),
    ADMIN(...),
    AUDITOR("Auditor", "Read-only with audit logs", 30),  // NEW
    OPERATOR("Operator", "Create blocks only", 15),       // NEW
    USER(...),
    READ_ONLY(...)
}
```

Then update permission methods accordingly.

---

### Q5: Performance impact of role checks?

**A:** Minimal. Role check = single database query with indexed lookup.

**Optimization:** Cache user roles in-memory (with TTL).

```java
// Cache roles for 5 minutes
private final LoadingCache<String, UserRole> roleCache = Caffeine.newBuilder()
    .expireAfterWrite(5, TimeUnit.MINUTES)
    .build(publicKey -> fetchRoleFromDatabase(publicKey));
```

---

## Next Steps

1. ✅ **Review and approve** this document
2. ✅ **Design caller identification** mechanism (ThreadLocal vs explicit parameter)
3. ✅ **Implement** UserRole enum and AuthorizedKey entity changes
4. ✅ **Database migration** script (Flyway/Liquibase)
5. ✅ **Update** Blockchain and UserFriendlyEncryptionAPI
6. ✅ **Test** comprehensive role-based scenarios
7. ✅ **Update** all documentation and demos
8. ✅ **Release** as version 1.0.6

---

## References

- [NIST RBAC Standard (INCITS 359-2012)](https://csrc.nist.gov/projects/role-based-access-control)
- [OWASP Access Control Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Access_Control_Cheat_Sheet.html)
- [Principle of Least Privilege (POLP)](https://en.wikipedia.org/wiki/Principle_of_least_privilege)

---

**Document Status:** 📋 Pending approval
**Next Review:** After implementation design for caller identification
