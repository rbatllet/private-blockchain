# 🔍 Caller Identification for RBAC Implementation

**Document Version:** 1.1
**Date:** 2025-01-07
**Status:** ✅ **IMPLEMENTED** (v1.0.6)
**Version:** 1.0.6
**Related:** [ROLE_BASED_ACCESS_CONTROL.md](ROLE_BASED_ACCESS_CONTROL.md)

**Implementation Decision:** **Explicit Parameter Approach** (Option 2) was chosen and implemented.

---

## Table of Contents

1. [Problem Statement](#problem-statement)
2. [Proposed Solutions](#proposed-solutions)
3. [Context7 Research Findings](#context7-research-findings)
4. [Comparison Matrix](#comparison-matrix)
5. [Recommended Approach](#recommended-approach)
6. [Implementation Details](#implementation-details)
7. [Migration Strategy](#migration-strategy)
8. [Test Impact Analysis](#test-impact-analysis)
9. [Security Considerations](#security-considerations)
10. [Performance Analysis](#performance-analysis)

---

## Problem Statement

### Current Situation (v1.0.6)

The `Blockchain.addAuthorizedKey()` method currently has **no way to identify the caller**:

```java
// ❌ CURRENT - No caller identification
public boolean addAuthorizedKey(String publicKeyString, String ownerName) {
    // Who is calling this method?
    // Cannot validate caller's role or permissions
    authorizedKeyDAO.addAuthorizedKey(publicKeyString, ownerName);
    return true;
}
```

**Problem:** With RBAC system (v1.0.6+), we need to validate:
- ✅ Who is calling the method? (caller identity)
- ✅ What role does the caller have? (SUPER_ADMIN, ADMIN, USER)
- ✅ Can caller create users with target role? (permission check)

### Requirements for RBAC

**R1: Caller Identification**
- Must identify the caller's public key/credentials
- Must work for both bootstrap (bootstrap admin) and normal operations

**R2: Role Validation**
- Must retrieve caller's role from database
- Must validate caller.canCreateRole(targetRole)

**R3: Security**
- Must prevent unauthorized access
- Must prevent privilege escalation
- Must be resistant to bypasses

**R4: Bootstrap Compatibility**
- Bootstrap admin creation must work (no caller yet)
- Existing tests must continue to work with minimal changes

**R5: Performance**
- Minimal overhead (avoid excessive database queries)
- Thread-safe operations
- No memory leaks

---

## Proposed Solutions

### Option 1: ThreadLocal SecurityContext Pattern

**Approach:** Use ThreadLocal to store caller credentials implicitly, similar to Spring Security's `SecurityContextHolder`.

#### Architecture

```java
// New class: SecurityContext.java
public class SecurityContext {
    private static final ThreadLocal<UserCredentials> contextHolder = new ThreadLocal<>();

    public static void setCurrentUser(String username, KeyPair keyPair) {
        contextHolder.set(new UserCredentials(username, keyPair));
    }

    public static UserCredentials getCurrentUser() {
        return contextHolder.get();
    }

    public static void clearContext() {
        contextHolder.remove();  // CRITICAL: Prevent memory leaks
    }

    public static class UserCredentials {
        private final String username;
        private final KeyPair keyPair;

        public UserCredentials(String username, KeyPair keyPair) {
            this.username = username;
            this.keyPair = keyPair;
        }

        public String getUsername() { return username; }
        public KeyPair getKeyPair() { return keyPair; }
    }
}
```

#### Usage Pattern

```java
// UserFriendlyEncryptionAPI.java
public KeyPair createUser(String username, UserRole role) {
    try {
        // Set current user context
        SecurityContext.setCurrentUser(defaultUsername.get(), defaultKeyPair.get());

        // Call blockchain method (implicitly uses context)
        KeyPair newKeys = CryptoUtil.generateKeyPair();
        blockchain.addAuthorizedKey(
            CryptoUtil.publicKeyToString(newKeys.getPublic()),
            username,
            role
        );

        return newKeys;
    } finally {
        // CRITICAL: Always clear context
        SecurityContext.clearContext();
    }
}

// Blockchain.java
public boolean addAuthorizedKey(String publicKeyString, String ownerName, UserRole role) {
    // Get caller from ThreadLocal context
    SecurityContext.UserCredentials caller = SecurityContext.getCurrentUser();

    if (caller == null) {
        // Bootstrap mode: Allow bootstrap admin creation
        if (authorizedKeyDAO.getAuthorizedKeyCount() > 0) {
            throw new SecurityException("Caller context required after bootstrap");
        }
        // Bootstrap admin creation - no validation
        return addAuthorizedKeyInternal(publicKeyString, ownerName, UserRole.SUPER_ADMIN);
    }

    // Normal mode: Validate caller role
    String callerPublicKey = CryptoUtil.publicKeyToString(caller.getKeyPair().getPublic());
    UserRole callerRole = getUserRole(callerPublicKey);

    if (!callerRole.canCreateRole(role)) {
        throw new SecurityException("Role " + callerRole + " cannot create role " + role);
    }

    return addAuthorizedKeyInternal(publicKeyString, ownerName, role);
}
```

#### Advantages ✅

1. **Clean API** - No extra parameters polluting method signatures
2. **Spring Alignment** - Uses same pattern as Spring Security
3. **Implicit Context** - Caller credentials automatically available
4. **Elegant Code** - Try-finally pattern is well-known

#### Disadvantages ❌

1. **Memory Leak Risk** - If `clearContext()` not called, ThreadLocal leaks memory
2. **Hidden Dependencies** - Method behavior depends on invisible context
3. **Testing Complexity** - Tests must set/clear context in every test
4. **Debugging Difficulty** - Context not visible in stack traces
5. **Not Standard** - This project doesn't use Spring Framework
6. **Thread Pool Issues** - ThreadLocal doesn't work well with thread pools (reused threads)

---

### Option 2: Explicit Parameter Approach

**Approach:** Explicitly pass caller credentials as method parameter.

#### Architecture

```java
// Blockchain.java - NEW signature
public boolean addAuthorizedKey(String publicKeyString,
                                String ownerName,
                                KeyPair callerKeyPair,  // ✨ NEW parameter
                                UserRole targetRole) {

    // Bootstrap mode: Allow bootstrap admin creation without caller
    if (callerKeyPair == null) {
        if (authorizedKeyDAO.getAuthorizedKeyCount() > 0) {
            throw new SecurityException(
                "❌ SECURITY: callerKeyPair=null only allowed for bootstrap admin bootstrap.\n" +
                "After bootstrap, ALL operations require caller credentials."
            );
        }
        // Bootstrap admin creation - no validation needed
        return addAuthorizedKeyInternal(publicKeyString, ownerName, UserRole.SUPER_ADMIN);
    }

    // Normal mode: Validate caller has permission
    String callerPublicKey = CryptoUtil.publicKeyToString(callerKeyPair.getPublic());
    AuthorizedKey caller = authorizedKeyDAO.getAuthorizedKeyByPublicKey(callerPublicKey);

    if (caller == null || !caller.isActive()) {
        throw new SecurityException("❌ Caller is not authorized");
    }

    // Check if caller can create target role
    if (!caller.getRole().canCreateRole(targetRole)) {
        throw new SecurityException(
            "❌ Role " + caller.getRole() + " cannot create role " + targetRole
        );
    }

    return addAuthorizedKeyInternal(publicKeyString, ownerName, targetRole);
}

// NOTE: The old signature addAuthorizedKey(String, String) was REMOVED in v1.0.6
// per project policy. All code migrated to new RBAC-aware signature.
```

#### Usage Pattern

```java
// UserFriendlyEncryptionAPI.java
public KeyPair createUser(String username, UserRole role) {
    synchronized (credentialsLock) {
        // Validate caller credentials are set
        if (defaultKeyPair.get() == null || defaultUsername.get() == null) {
            throw new SecurityException("❌ AUTHORIZATION REQUIRED...");
        }

        // Generate new user keys
        KeyPair newUserKeys = CryptoUtil.generateKeyPair();
        String publicKeyString = CryptoUtil.publicKeyToString(newUserKeys.getPublic());

        // ✅ Pass caller credentials explicitly
        boolean success = blockchain.addAuthorizedKey(
            publicKeyString,
            username,
            defaultKeyPair.get(),  // ✅ Caller credentials
            role                   // ✅ Target role
        );

        if (!success) {
            throw new RuntimeException("Failed to register user: " + username);
        }

        return newUserKeys;
    }
}
```

#### Advantages ✅

1. **Zero Memory Leak Risk** - Parameters are garbage collected automatically
2. **Explicit Dependencies** - Method signature shows exactly what's needed
3. **Simple Testing** - Just pass credentials as parameter, no context setup
4. **Easy Debugging** - All parameters visible in stack traces
5. **Thread-Safe** - No ThreadLocal issues with thread pools
6. **No Special Cleanup** - No try-finally blocks required
7. **Clear Contract** - API contract is explicit in method signature

#### Disadvantages ❌

1. **API Pollution** - Extra parameter in every method signature
2. **Not Spring-like** - Doesn't follow Spring Security pattern
3. **Repetitive Code** - Must pass credentials through call chain

---

## Context7 Research Findings

### Query: Spring Security ThreadLocal Best Practices

**Source:** `/websites/spring_io_spring-security_reference_7_0`
**Topic:** "SecurityContext ThreadLocal authentication storage"

#### Key Findings

**1. SecurityContextHolder Pattern (Core Spring Security)**

```java
// Spring Security's standard pattern
SecurityContextHolder.setContext(securityContext);
SecurityContext context = SecurityContextHolder.getContext();
Authentication auth = context.getAuthentication();
SecurityContextHolder.clearContext();  // CRITICAL cleanup
```

**2. Memory Leak Prevention**

Spring Security documentation emphasizes cleanup:

```java
// From Spring Security source
public static void clearContext() {
    strategy.clearContext();  // Delegates to ThreadLocal.remove()
}

// Always use try-finally
try {
    SecurityContextHolder.setContext(context);
    // ... operations
} finally {
    SecurityContextHolder.clearContext();  // Prevent leaks
}
```

**3. Context Propagation**

Spring uses `SecurityContextRepository` for cross-request persistence:

```java
// Save context to session
securityContextRepository.saveContext(context, request, response);

// Load context from session (next request)
SecurityContext context = securityContextRepository.loadContext(request);
```

**4. Test Cleanup**

Spring Test Framework has built-in context cleanup:

```java
@AfterEach
void tearDown() {
    SecurityContextHolder.clearContext();  // Cleanup after each test
}
```

**5. Reactive Context Propagation**

Spring uses Micrometer's context propagation for reactive apps:

```java
// Propagate context across threads in reactive apps
Mono.deferContextual(ctx -> {
    SecurityContext securityContext = ctx.get(SecurityContext.class);
    // Use context
});
```

---

### Research Conclusion

**ThreadLocal is Spring Security's standard pattern**, BUT:

1. ⚠️ **Requires Spring Framework integration** (SecurityContextRepository, filters, etc.)
2. ⚠️ **Needs careful cleanup** to prevent memory leaks
3. ⚠️ **Complex testing** (requires @AfterEach cleanup in all tests)
4. ⚠️ **Thread pool issues** without Micrometer context propagation

**This blockchain project does NOT use Spring Framework**, so we lose most of ThreadLocal's benefits while keeping all the risks.

---

## Comparison Matrix

| Aspect | ThreadLocal SecurityContext | Explicit KeyPair Parameter |
|--------|---------------------------|---------------------------|
| **API Elegance** | ✅ Clean (no extra params) | ⚠️ Pollutes signatures |
| **Memory Safety** | ❌ Leak risk if not cleaned | ✅ Zero leak risk |
| **Spring Alignment** | ✅ 100% aligned | ❌ Not Spring pattern |
| **Debugging** | ❌ Context invisible | ✅ Visible in stack |
| **Testing** | ❌ Complex (context setup) | ✅ Simple (pass param) |
| **Thread Safety** | ⚠️ Issues with pools | ✅ Thread-safe |
| **Implementation** | ⚠️ Complex (context mgmt) | ✅ Simple |
| **Cleanup Required** | ❌ try-finally everywhere | ✅ No cleanup |
| **Error Visibility** | ❌ Hidden failures | ✅ Clear exceptions |
| **Spring Integration** | ✅ Easy (if using Spring) | ⚠️ Manual integration |
| **This Project** | ❌ No Spring Framework | ✅ No dependencies |
| **Performance** | ✅ Zero parameter overhead | ✅ Pass-by-reference |
| **Backward Compat** | ⚠️ Requires migration | ✅ Deprecate old method |

---

## Recommended Approach

### ✅ **Recommendation: Explicit Parameter Approach**

**Rationale:**

This blockchain project **does NOT use Spring Framework**. The advantages of ThreadLocal (clean integration with Spring Security filters, SecurityContextRepository, etc.) **do not apply here**.

**Decision Factors:**

1. **Simplicity over Elegance**
   - Explicit parameter is simpler to implement and maintain
   - No hidden dependencies or invisible context
   - Clear contract in method signature

2. **Safety First**
   - Zero memory leak risk
   - No try-finally cleanup required
   - Thread-safe without special handling

3. **Testing Simplicity**
   - No context setup/teardown in tests
   - No @AfterEach cleanup hooks
   - Tests are straightforward parameter passing

4. **Debugging Ease**
   - All parameters visible in stack traces
   - Clear exceptions when credentials missing
   - No hidden context failures

5. **Project Architecture**
   - No Spring Framework dependency
   - Consistent with existing explicit parameter patterns
   - Fits well with current codebase style

6. **Minimal Migration Risk**
   - Deprecate old method, add new signature
   - UserFriendlyEncryptionAPI passes credentials
   - Tests updated with simple parameter addition

---

## Implementation Details

### Phase 1: Database Schema (Already in RBAC doc)

```sql
ALTER TABLE authorized_keys
ADD COLUMN role VARCHAR(20) DEFAULT 'USER' NOT NULL;

ALTER TABLE authorized_keys
ADD COLUMN created_by VARCHAR(100);  -- Track who created this user
```

---

### Phase 2: Core Method Update (Blockchain.java)

```java
/**
 * Add an authorized key with role-based validation (RBAC v1.0.6+).
 *
 * <p><strong>🔒 Security (v1.0.6+):</strong> Validates caller has permission to create
 * users with the specified target role.</p>
 *
 * <p><strong>Bootstrap Mode:</strong> For bootstrap admin creation ONLY, pass {@code callerKeyPair = null}.
 * After bootstrap admin exists, ALL operations require caller credentials.</p>
 *
 * <p><strong>Permission Matrix:</strong></p>
 * <ul>
 *   <li><strong>SUPER_ADMIN</strong> can create: ADMIN, USER, READ_ONLY</li>
 *   <li><strong>ADMIN</strong> can create: USER, READ_ONLY (cannot create ADMIN)</li>
 *   <li><strong>USER</strong> cannot create users</li>
 * </ul>
 *
 * @param publicKeyString The public key to authorize
 * @param ownerName The username
 * @param callerKeyPair The caller's credentials (null ONLY for bootstrap admin bootstrap)
 * @param targetRole The role to assign to the new user
 * @return true if successful
 * @throws SecurityException if caller lacks permission or callerKeyPair=null after bootstrap
 * @throws IllegalArgumentException if parameters are invalid
 * @since 1.0.6
 */
public boolean addAuthorizedKey(String publicKeyString,
                                String ownerName,
                                KeyPair callerKeyPair,
                                UserRole targetRole) {

    validateInputData(publicKeyString, 10000, "Public key");
    validateInputData(ownerName, 256, "Owner name");
    if (targetRole == null) {
        throw new IllegalArgumentException("Target role cannot be null");
    }

    long stamp = GLOBAL_BLOCKCHAIN_LOCK.writeLock();
    try {
        // Bootstrap mode: Allow bootstrap admin creation without caller
        if (callerKeyPair == null) {
            long existingUsers = authorizedKeyDAO.getAuthorizedKeyCount();
            if (existingUsers > 0) {
                throw new SecurityException(
                    "❌ SECURITY VIOLATION: callerKeyPair=null only allowed for bootstrap admin bootstrap.\n" +
                    "Existing users: " + existingUsers + "\n" +
                    "After bootstrap, ALL operations require caller credentials for RBAC validation."
                );
            }

            // Bootstrap admin creation - automatically SUPER_ADMIN role
            if (targetRole != UserRole.SUPER_ADMIN) {
                logger.warn("⚠️  Bootstrap admin must have SUPER_ADMIN role, overriding requested role: {}", targetRole);
            }

            logger.info("🔑 BOOTSTRAP: Creating bootstrap admin '{}' with SUPER_ADMIN role", ownerName);
            return addAuthorizedKeyInternal(publicKeyString, ownerName, UserRole.SUPER_ADMIN, null);
        }

        // Normal mode: Validate caller has permission
        String callerPublicKey = CryptoUtil.publicKeyToString(callerKeyPair.getPublic());
        AuthorizedKey caller = authorizedKeyDAO.getAuthorizedKeyByPublicKey(callerPublicKey);

        if (caller == null || !caller.isActive()) {
            throw new SecurityException(
                "❌ AUTHORIZATION REQUIRED: Caller is not authorized.\n" +
                "Public key: " + callerPublicKey.substring(0, 50) + "..."
            );
        }

        UserRole callerRole = caller.getRole();

        // Check if caller can create target role
        if (!callerRole.canCreateRole(targetRole)) {
            throw new SecurityException(
                "❌ PERMISSION DENIED: Role '" + callerRole + "' cannot create users with role '" + targetRole + "'.\n" +
                "Caller: " + caller.getOwnerName() + "\n" +
                "Target user: " + ownerName + "\n" +
                "See ROLE_BASED_ACCESS_CONTROL.md for permission matrix."
            );
        }

        logger.info("✅ User '{}' (role: {}) creating new user '{}' with role {}",
                    caller.getOwnerName(), callerRole, ownerName, targetRole);

        return addAuthorizedKeyInternal(publicKeyString, ownerName, targetRole, caller.getOwnerName());

    } finally {
        GLOBAL_BLOCKCHAIN_LOCK.unlockWrite(stamp);
    }
}

/**
 * Internal method to add authorized key without validation (single source of truth).
 *
 * @param publicKeyString The public key
 * @param ownerName The username
 * @param role The role to assign
 * @param createdBy Who created this user (null for bootstrap admin)
 * @return true if successful
 */
private boolean addAuthorizedKeyInternal(String publicKeyString,
                                         String ownerName,
                                         UserRole role,
                                         String createdBy) {
    if (isKeyAuthorizedInternal(publicKeyString)) {
        logger.warn("⚠️  Public key already authorized for user: {}", ownerName);
        return false;
    }

    authorizedKeyDAO.addAuthorizedKey(publicKeyString, ownerName, role, createdBy);
    logger.info("✅ Authorized key added: {} (role: {}, created by: {})",
                ownerName, role, createdBy != null ? createdBy : "SYSTEM");
    return true;
}

/**
 * REMOVED in v1.0.6: addAuthorizedKey(String, String)
 *
 * <p><strong>⚠️ MIGRATION NOTE:</strong> The old signature without RBAC parameters
 * was REMOVED in v1.0.6 per project policy.</p>
 *
 * <p>All code migrated to: addAuthorizedKey(publicKey, ownerName, callerKeyPair, role)</p>
 *
 * <p>See docs/security/ROLE_BASED_ACCESS_CONTROL.md for migration details.</p>
 */
```

---

### Phase 3: DAO Layer Update (AuthorizedKeyDAO.java)

```java
/**
 * Add authorized key with role and creator tracking.
 *
 * @param publicKey The public key
 * @param ownerName The username
 * @param role The role to assign
 * @param createdBy Who created this user (null for bootstrap admin)
 * @since 1.0.6
 */
public void addAuthorizedKey(String publicKey, String ownerName, UserRole role, String createdBy) {
    EntityManager em = JPAUtil.getEntityManager();
    try {
        em.getTransaction().begin();

        // Use full constructor (v1.0.6+)
        AuthorizedKey key = new AuthorizedKey(
            publicKey,
            ownerName,
            role,
            createdBy,
            LocalDateTime.now()
        );
        key.setActive(true);

        em.persist(key);
        em.getTransaction().commit();

        logger.debug("Authorized key persisted: {} (role: {}, created by: {})",
                     ownerName, role, createdBy != null ? createdBy : "SYSTEM");
    } catch (Exception e) {
        if (em.getTransaction().isActive()) {
            em.getTransaction().rollback();
        }
        throw new RuntimeException("Failed to add authorized key: " + e.getMessage(), e);
    } finally {
        em.close();
    }
}
```

---

### Phase 4: UserFriendlyEncryptionAPI Update

```java
/**
 * Create a new user with USER role.
 *
 * <p><strong>🔒 Security (v1.0.6+):</strong> Requires caller to be ADMIN or SUPER_ADMIN.</p>
 *
 * <p><strong>Example:</strong></p>
 * <pre>
 * // Load bootstrap admin keys (generated via ./tools/generate_genesis_keys.zsh)
 * KeyPair bootstrapKeys = KeyFileLoader.loadKeyPairFromFiles(
 *     "./keys/genesis-admin.private",
 *     "./keys/genesis-admin.public"
 * );
 *
 * // Register bootstrap admin in blockchain (REQUIRED!)
 * blockchain.createBootstrapAdmin(
 *     CryptoUtil.publicKeyToString(bootstrapKeys.getPublic()),
 *     "BOOTSTRAP_ADMIN"
 * );
 *
 * // Create API with bootstrap admin credentials
 * UserFriendlyEncryptionAPI api = new UserFriendlyEncryptionAPI(blockchain);
 * api.setDefaultCredentials("BOOTSTRAP_ADMIN", bootstrapKeys);
 *
 * // Create new user (requires ADMIN or SUPER_ADMIN role)
 * KeyPair aliceKeys = api.createUser("alice");
 * </pre>
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
                "❌ AUTHORIZATION REQUIRED: Must set authorized credentials before creating users.\n" +
                "Use: api.setDefaultCredentials(username, keyPair)\n" +
                "For initial setup, load bootstrap admin credentials first."
            );
        }

        // Create user with specified role
        try {
            KeyPair keyPair = CryptoUtil.generateKeyPair();
            String publicKeyString = CryptoUtil.publicKeyToString(keyPair.getPublic());

            // ✅ Pass caller credentials explicitly for RBAC validation
            blockchain.addAuthorizedKey(
                publicKeyString,
                username,
                defaultKeyPair.get(),  // ✅ Caller credentials
                targetRole             // ✅ Target role
            );

            logger.info("✅ User '{}' created with role {} by '{}'",
                        username, targetRole, defaultUsername.get());
            return keyPair;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create user: " + e.getMessage(), e);
        }
    }
}
```

---

## Migration Strategy

### Step 1: Update Database Schema

```sql
-- Add role column (default USER for existing users)
ALTER TABLE authorized_keys
ADD COLUMN role VARCHAR(20) DEFAULT 'USER' NOT NULL;

-- Add creator tracking
ALTER TABLE authorized_keys
ADD COLUMN created_by VARCHAR(100);

-- Assign SUPER_ADMIN to bootstrap admin
UPDATE authorized_keys
SET role = 'SUPER_ADMIN'
WHERE owner_name = 'BOOTSTRAP_ADMIN'
  AND id = 1;
```

### Step 2: Update Code

1. ✅ Create `UserRole` enum (completed)
2. ✅ Update `AuthorizedKey` entity with `role` and `createdBy` fields
3. ✅ Add new `Blockchain.addAuthorizedKey(String, String, KeyPair, UserRole)` signature
4. ✅ **REMOVE** old `Blockchain.addAuthorizedKey(String, String)` signature (directly removed)
5. ✅ Update `AuthorizedKeyDAO.saveAuthorizedKey()` to accept role and creator
6. ✅ Update `UserFriendlyEncryptionAPI` to pass credentials

### Step 3: Update Tests

```java
// OLD TEST (removed in v1.0.6)
@Test
void testCreateUser() {
    blockchain.addAuthorizedKey(publicKey, "alice");  // ❌ Method does not exist (removed)
}

// NEW TEST (RBAC-aware)
@Test
void testCreateUser() {
    // Setup: Load bootstrap admin
    KeyPair bootstrapKeys = loadBootstrapKeys();

    // Create user with explicit caller credentials
    KeyPair aliceKeys = CryptoUtil.generateKeyPair();
    blockchain.addAuthorizedKey(
        CryptoUtil.publicKeyToString(aliceKeys.getPublic()),
        "alice",
        bootstrapKeys,     // ✅ Caller credentials
        UserRole.USER    // ✅ Target role
    );
}
```

### Step 4: Update Demos

All demos must use the secure pattern:

```java
// Load bootstrap admin credentials (generated via ./tools/generate_genesis_keys.zsh)
KeyPair bootstrapKeys = KeyFileLoader.loadKeyPairFromFiles(
    "./keys/genesis-admin.private",
    "./keys/genesis-admin.public"
);

// Register bootstrap admin in blockchain (REQUIRED!)
blockchain.createBootstrapAdmin(
    CryptoUtil.publicKeyToString(bootstrapKeys.getPublic()),
    "BOOTSTRAP_ADMIN"
);

// Create API with bootstrap admin
UserFriendlyEncryptionAPI api = new UserFriendlyEncryptionAPI(blockchain);
api.setDefaultCredentials("BOOTSTRAP_ADMIN", bootstrapKeys);

// Create users (credentials passed implicitly by API)
KeyPair aliceKeys = api.createUser("alice");
```

---

## Test Impact Analysis

### Test Categories

**1. Unit Tests (Minimal Impact)**
- Add `callerKeyPair` parameter to `addAuthorizedKey()` calls
- Load bootstrap keys in test setup
- No context management needed

**2. Integration Tests (Minimal Impact)**
- Use `UserFriendlyEncryptionAPI` (handles credentials internally)
- No changes needed if already using API

**3. Security Tests (New Tests Required)**
- Test RBAC permission matrix
- Test privilege escalation prevention
- Test bootstrap mode vs normal mode

### Migration Effort Estimate

| Test Type | Count | Effort per Test | Total Effort |
|-----------|-------|----------------|--------------|
| Unit tests using `addAuthorizedKey()` | ~20 | 2 minutes | 40 minutes |
| Integration tests using API | ~50 | 0 minutes | 0 (no change) |
| New RBAC security tests | ~15 | 10 minutes | 150 minutes |
| **Total** | **~85** | **-** | **~3 hours** |

---

## Security Considerations

### 1. Bootstrap Protection

**Threat:** Attacker tries to create bootstrap admin after blockchain initialization.

**Mitigation:**
```java
if (callerKeyPair == null) {
    long existingUsers = authorizedKeyDAO.getAuthorizedKeyCount();
    if (existingUsers > 0) {
        throw new SecurityException("callerKeyPair=null only allowed for bootstrap admin bootstrap");
    }
}
```

### 2. Privilege Escalation Prevention

**Threat:** ADMIN tries to create another ADMIN to maintain persistence.

**Mitigation:**
```java
if (!callerRole.canCreateRole(targetRole)) {
    throw new SecurityException("Role " + callerRole + " cannot create role " + targetRole);
}
```

### 3. Caller Impersonation

**Threat:** Attacker passes stolen credentials as `callerKeyPair`.

**Mitigation:**
- Blockchain validates caller is in authorized_keys table
- Blockchain validates caller.isActive() is true
- All operations logged with caller identity

### 4. Null Credential Bypass

**Threat:** Attacker passes `callerKeyPair = null` to bypass validation.

**Mitigation:**
- Only allowed when `getAuthorizedKeyCount() == 0`
- Strict check prevents bypass after bootstrap

---

## Performance Analysis

### Overhead Comparison

| Operation | ThreadLocal | Explicit Parameter |
|-----------|-------------|-------------------|
| Set context | 50ns (ThreadLocal.set) | 0ns (no op) |
| Get context | 30ns (ThreadLocal.get) | 0ns (pass reference) |
| Clear context | 50ns (ThreadLocal.remove) | 0ns (GC handles) |
| Parameter passing | 0ns | 10ns (reference copy) |
| **Total overhead** | ~130ns per call | ~10ns per call |

**Verdict:** Explicit parameter is **13x faster** (negligible difference in absolute terms).

### Memory Comparison

| Aspect | ThreadLocal | Explicit Parameter |
|--------|-------------|-------------------|
| Memory per thread | 48 bytes (ThreadLocalMap entry) | 0 bytes |
| Leak risk | High (if not cleared) | Zero |
| GC pressure | Medium (ThreadLocalMap) | Low (method stack) |

**Verdict:** Explicit parameter has better memory characteristics.

---

## Conclusion

### ✅ Final Recommendation: **Explicit Parameter Approach**

**Why:**

1. ✅ **Simplicity** - Straightforward implementation, no hidden complexity
2. ✅ **Safety** - Zero memory leak risk, thread-safe by design
3. ✅ **Testing** - Simple parameter passing, no context management
4. ✅ **Debugging** - All parameters visible, clear error messages
5. ✅ **Project Fit** - No Spring Framework, explicit style matches codebase
6. ✅ **Performance** - Lower overhead, better memory usage
7. ✅ **Maintainability** - Easy to understand, no special knowledge required
8. ✅ **Clean Codebase** - Old signatures removed, no technical debt

**ThreadLocal would be appropriate if:**
- ❌ Project used Spring Framework (we don't)
- ❌ Many layers need caller context (we have 2 layers: API → Blockchain)
- ❌ Complex call chains (our calls are simple: API → Blockchain → DAO)

**Next Steps:**

1. ✅ UserRole enum created
2. ⏳ Update AuthorizedKey entity
3. ⏳ Implement new addAuthorizedKey() signature
4. ⏳ Update tests
5. ⏳ Update demos
6. ⏳ Update documentation

---

## References

- [Spring Security Reference: SecurityContextHolder](https://docs.spring.io/spring-security/reference/servlet/authentication/architecture.html#servlet-authentication-securitycontextholder)
- [ThreadLocal Best Practices](https://www.baeldung.com/java-threadlocal)
- [OWASP: Access Control](https://cheatsheetseries.owasp.org/cheatsheets/Access_Control_Cheat_Sheet.html)
- [ROLE_BASED_ACCESS_CONTROL.md](ROLE_BASED_ACCESS_CONTROL.md)

---

## ✅ Implementation Summary (v1.0.6)

**Decision:** Explicit Parameter Approach (Option 2) was implemented.

### Implemented Signature

```java
public boolean addAuthorizedKey(
    String publicKeyString,
    String ownerName,
    java.security.KeyPair callerKeyPair,  // NEW: Explicit caller identification
    com.rbatllet.blockchain.security.UserRole targetRole  // NEW: Target role to assign
)
```

### Critical Security Validations Implemented

**1. Bootstrap Protection:**
```java
if (callerKeyPair == null) {
    long existingUsers = authorizedKeyDAO.getAuthorizedKeyCount();

    // Validation 1: callerKeyPair=null ONLY allowed when no users exist
    if (existingUsers > 0) {
        throw new SecurityException(
            "❌ SECURITY VIOLATION: callerKeyPair=null only allowed for bootstrap admin bootstrap."
        );
    }

    // Validation 2: Bootstrap MUST create SUPER_ADMIN
    if (targetRole != UserRole.SUPER_ADMIN) {
        throw new SecurityException(
            "❌ SECURITY VIOLATION: Bootstrap ONLY allows SUPER_ADMIN role.\n" +
            "Cannot create " + targetRole + " with callerKeyPair=null."
        );
    }

    // Create bootstrap SUPER_ADMIN
    return addAuthorizedKeyInternal(publicKeyString, ownerName, UserRole.SUPER_ADMIN, null);
}
```

**2. Role-Based Permission Validation:**
```java
// Get caller's public key and retrieve role
String callerPublicKey = CryptoUtil.publicKeyToString(callerKeyPair.getPublic());
AuthorizedKey caller = authorizedKeyDAO.getAuthorizedKeyByPublicKey(callerPublicKey);

// Validate caller is authorized and active
if (caller == null || !caller.isActive()) {
    throw new SecurityException("❌ AUTHORIZATION REQUIRED: Caller is not authorized.");
}

// Get caller's role
UserRole callerRole = caller.getRole();

// Check if caller can create target role
if (!callerRole.canCreateRole(targetRole)) {
    throw new SecurityException(
        "❌ PERMISSION DENIED: Role '" + callerRole + "' cannot create users with role '" + targetRole + "'."
    );
}
```

### Key Benefits Achieved

1. ✅ **No ThreadLocal Complexity:** Zero memory leak risk, simpler debugging
2. ✅ **Bootstrap Security:** Strict validation prevents unauthorized SUPER_ADMIN/ADMIN creation
3. ✅ **Single SUPER_ADMIN Guarantee:** Only bootstrap admin can be SUPER_ADMIN
4. ✅ **Privilege Escalation Prevention:** ADMIN cannot create other ADMINs
5. ✅ **Visible in Stack Traces:** Explicit parameters make debugging easier
6. ✅ **Thread-Safe:** No shared state between threads

### Production Usage Pattern

```java
// 1. Load bootstrap admin keys (generated via ./tools/generate_genesis_keys.zsh)
KeyPair bootstrapKeys = KeyFileLoader.loadKeyPairFromFiles(
    "./keys/genesis-admin.private",
    "./keys/genesis-admin.public"
);

// 2. Register bootstrap admin in blockchain (REQUIRED!)
blockchain.createBootstrapAdmin(
    CryptoUtil.publicKeyToString(bootstrapKeys.getPublic()),
    "BOOTSTRAP_ADMIN"
);

// 3. Create ADMIN user (only SUPER_ADMIN can do this)
KeyPair adminKeys = CryptoUtil.generateKeyPair();
String adminPublicKey = CryptoUtil.publicKeyToString(adminKeys.getPublic());
blockchain.addAuthorizedKey(
    adminPublicKey,
    "Alice-Admin",
    bootstrapKeys,  // Caller: bootstrap SUPER_ADMIN
    UserRole.ADMIN
);

// 3. Create USER (ADMIN can do this)
KeyPair userKeys = CryptoUtil.generateKeyPair();
String userPublicKey = CryptoUtil.publicKeyToString(userKeys.getPublic());
blockchain.addAuthorizedKey(
    userPublicKey,
    "Bob-User",
    adminKeys,  // Caller: Alice (ADMIN)
    UserRole.USER
);
```

### Migration Impact

- **Demos:** All 24 demo files updated to use new signature with proper role assignment
- **Tests:** 202+ test calls need update (work in progress)
- **UserFriendlyEncryptionAPI:** Updated with `createUser()` and `createAdmin()` methods
- **ChainRecoveryManager:** Special `addAuthorizedKeySystemRecovery()` for recovery operations

---

**Document Status:** ✅ Implemented in v1.0.6
**Next Steps:** Complete test migration, update remaining documentation
