# Pre-Authorization Security Model

**Status:** ✅ Active (v1.0.6+)  
**Category:** Security / User Management  
**Criticality:** HIGH - Required for all user creation operations

---

## 📋 Table of Contents

1. [Overview](#overview)
2. [Security Rationale](#security-rationale)
3. [Architecture](#architecture)
4. [Workflow Steps](#workflow-steps)
5. [Code Examples](#code-examples)
6. [Testing Pattern](#testing-pattern)
7. [Common Mistakes](#common-mistakes)
8. [Troubleshooting](#troubleshooting)

---

## Overview

### What Changed in v1.0.6?

Starting from **v1.0.6**, the `UserFriendlyEncryptionAPI.createUser()` method **requires authenticated credentials** before creating new users. This prevents unauthorized user creation attacks.

### Key Points

- ✅ **Mandatory Authentication**: Must call `setDefaultCredentials()` before `createUser()`
- ✅ **Authorized Keys Only**: Only authorized users can create new users
- ✅ **Security by Design**: Prevents self-authorization exploits
- ✅ **Test Pattern Updated**: All tests must follow the pre-authorization workflow

---

## Security Rationale

### The Problem (Pre-v1.0.6)

Before v1.0.6, tests and applications could call `createUser()` without authentication:

```java
❌ INSECURE (Pre-v1.0.6):
api = new UserFriendlyEncryptionAPI(blockchain);
KeyPair userKeys = api.createUser("malicious-user");  // ⚠️ No auth check!
```

**Vulnerabilities:**
- Anyone could create users without authorization
- Self-authorization attacks possible
- No audit trail of who created whom

### The Solution (v1.0.6+)

Now `createUser()` validates caller credentials:

```java
✅ SECURE (v1.0.6+):
// 1. Authenticate first
api.setDefaultCredentials("admin", adminKeys);

// 2. Only authenticated users can create others
KeyPair userKeys = api.createUser("new-user");  // ✅ Validated!
```

**Security Benefits:**
- Explicit authorization required
- Clear audit trail (who created whom)
- Prevents unauthorized user proliferation
- Aligns with enterprise security standards

---

## Architecture

### How createUser() Validates Credentials

```java
public KeyPair createUser(String username) {
    // SECURITY CHECK 1: Credentials must be set
    synchronized (credentialsLock) {
        if (defaultKeyPair.get() == null || defaultUsername.get() == null) {
            throw new SecurityException(
                "❌ AUTHORIZATION REQUIRED: Must set authorized credentials..."
            );
        }
        
        // SECURITY CHECK 2: Caller must be authorized
        String callerPublicKey = CryptoUtil.publicKeyToString(
            defaultKeyPair.get().getPublic()
        );
        if (!blockchain.isKeyAuthorized(callerPublicKey)) {
            throw new SecurityException(
                "❌ AUTHORIZATION REQUIRED: Only authorized users can create..."
            );
        }
    }
    
    // If checks pass, generate NEW keys for the new user
    KeyPair newUserKeys = CryptoUtil.generateKeyPair();
    blockchain.addAuthorizedKey(
        CryptoUtil.publicKeyToString(newUserKeys.getPublic()),
        username
    );
    return newUserKeys;
}
```

### Key Concepts

1. **Caller Authentication**: The API checks `defaultKeyPair` and `defaultUsername`
2. **Authorization Validation**: The caller's public key must be in authorized keys
3. **New Key Generation**: `createUser()` generates **NEW** keys (not using caller's keys)
4. **Automatic Authorization**: New user is automatically added to authorized keys

---

## Workflow Steps

### Complete Pre-Authorization Workflow

```
┌─────────────────────────────────────────────────────────────┐
│  Step 1: Generate Admin Keypair                             │
│  ┌────────────────────────────────────┐                     │
│  │ KeyPair adminKeys =                │                     │
│  │   CryptoUtil.generateKeyPair();    │                     │
│  └────────────────────────────────────┘                     │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│  Step 2: Authorize Admin's Public Key                       │
│  ┌────────────────────────────────────┐                     │
│  │ String publicKey =                 │                     │
│  │   CryptoUtil.publicKeyToString(    │                     │
│  │     adminKeys.getPublic());        │                     │
│  │ blockchain.addAuthorizedKey(       │                     │
│  │   publicKey, "Admin");             │                     │
│  └────────────────────────────────────┘                     │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│  Step 3: Authenticate as Admin (CRITICAL!)                  │
│  ┌────────────────────────────────────┐                     │
│  │ api.setDefaultCredentials(         │                     │
│  │   "Admin", adminKeys);             │ ← Sets caller identity
│  └────────────────────────────────────┘                     │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│  Step 4: Create New User (Validated!)                       │
│  ┌────────────────────────────────────┐                     │
│  │ KeyPair userKeys =                 │                     │
│  │   api.createUser("test-user");     │ ← Checks caller auth
│  └────────────────────────────────────┘                     │
│                                                              │
│  Internally:                                                 │
│  • Validates admin is authenticated                          │
│  • Validates admin is authorized                             │
│  • Generates NEW keys for "test-user"                        │
│  • Authorizes "test-user" automatically                      │
└─────────────────────────────────────────────────────────────┘
```

---

## Code Examples

### Example 1: Basic User Creation (Production)

```java
// Initialize blockchain and API
Blockchain blockchain = new Blockchain();
UserFriendlyEncryptionAPI api = new UserFriendlyEncryptionAPI(blockchain);

// Load bootstrap admin keys (RBAC v1.0.6+)
KeyPair bootstrapKeys = KeyFileLoader.loadKeyPairFromFiles(
    "./keys/genesis-admin.private",
    "./keys/genesis-admin.public"
);

// STEP 1: Register bootstrap admin in blockchain (REQUIRED!)
blockchain.createBootstrapAdmin(
    CryptoUtil.publicKeyToString(bootstrapKeys.getPublic()),
    "BOOTSTRAP_ADMIN"
);

// STEP 2: Create and authorize admin
KeyPair adminKeys = CryptoUtil.generateKeyPair();
String adminPublicKey = CryptoUtil.publicKeyToString(adminKeys.getPublic());
blockchain.addAuthorizedKey(
    adminPublicKey,
    "Admin",
    bootstrapKeys,      // Caller credentials (bootstrap admin)
    UserRole.ADMIN      // Target role
);

// STEP 3: Authenticate as admin (enables createUser)
api.setDefaultCredentials("Admin", adminKeys);

// STEP 4: Create new users (validated by createUser)
KeyPair aliceKeys = api.createUser("alice");
KeyPair bobKeys = api.createUser("bob");

// Switch to alice's context
api.setDefaultCredentials("alice", aliceKeys);

// Now alice can create more users (because alice is authorized)
KeyPair charlieKeys = api.createUser("charlie");
```

### Example 2: Using Genesis Admin (Recommended)

```java
Blockchain blockchain = new Blockchain();

// Load genesis admin keys
KeyPair genesisKeys = KeyFileLoader.loadKeyPairFromFiles(
    "./keys/genesis-admin.private",
    "./keys/genesis-admin.public"
);

// Register bootstrap admin in blockchain (REQUIRED!)
blockchain.createBootstrapAdmin(
    CryptoUtil.publicKeyToString(genesisKeys.getPublic()),
    "BOOTSTRAP_ADMIN"
);

// Authenticate as genesis admin
UserFriendlyEncryptionAPI api = new UserFriendlyEncryptionAPI(blockchain);
api.setDefaultCredentials("BOOTSTRAP_ADMIN", genesisKeys);

// Now can create users
KeyPair userKeys = api.createUser("production-user");
```

### Example 3: Constructor with Keys (Alternative Pattern)

```java
// Alternative: Use constructor that accepts keys directly
// Load bootstrap admin keys (RBAC v1.0.6+)
KeyPair bootstrapKeys = KeyFileLoader.loadKeyPairFromFiles(
    "./keys/genesis-admin.private",
    "./keys/genesis-admin.public"
);

// Register bootstrap admin in blockchain (REQUIRED!)
blockchain.createBootstrapAdmin(
    CryptoUtil.publicKeyToString(bootstrapKeys.getPublic()),
    "BOOTSTRAP_ADMIN"
);

KeyPair adminKeys = CryptoUtil.generateKeyPair();
String adminPublicKey = CryptoUtil.publicKeyToString(adminKeys.getPublic());
blockchain.addAuthorizedKey(
    adminPublicKey,
    "Admin",
    bootstrapKeys,      // Caller credentials (bootstrap admin)
    UserRole.ADMIN      // Target role
);

// This constructor automatically calls setDefaultCredentials()
UserFriendlyEncryptionAPI api = new UserFriendlyEncryptionAPI(
    blockchain, 
    "Admin", 
    adminKeys
);

// Admin is already authenticated, can create users immediately
KeyPair userKeys = api.createUser("test-user");
```

---

## Testing Pattern

### Standard Test Setup Pattern

All tests using `createUser()` must follow this pattern:

```java
@BeforeEach
void setUp() throws Exception {
    blockchain = new Blockchain();
    api = new UserFriendlyEncryptionAPI(blockchain);
    
    // ========================================================================
    // PRE-AUTHORIZATION WORKFLOW (v1.0.6 Security Model)
    // ========================================================================
    // Load bootstrap admin keys (RBAC v1.0.6+)
    KeyPair bootstrapKeys = KeyFileLoader.loadKeyPairFromFiles(
        "./keys/genesis-admin.private",
        "./keys/genesis-admin.public"
    );

    // Step 1: Register bootstrap admin in blockchain (REQUIRED!)
    blockchain.createBootstrapAdmin(
        CryptoUtil.publicKeyToString(bootstrapKeys.getPublic()),
        "BOOTSTRAP_ADMIN"
    );

    // Step 2: Create and authorize admin
    KeyPair adminKeys = CryptoUtil.generateKeyPair();
    String adminPublicKey = CryptoUtil.publicKeyToString(adminKeys.getPublic());
    blockchain.addAuthorizedKey(
        adminPublicKey,
        "Admin",
        bootstrapKeys,      // Caller credentials (bootstrap admin)
        UserRole.ADMIN      // Target role
    );
    
    // Step 3: Authenticate as admin (enables createUser() calls)
    api.setDefaultCredentials("Admin", adminKeys);
    
    // Step 4: Admin creates test user (generates new keys internally)
    testUserKeys = api.createUser("test-user");
    api.setDefaultCredentials("test-user", testUserKeys);
}
```

### Tests Fixed in v1.0.6 Migration

The following tests were updated to use the pre-authorization pattern:

1. ✅ `SearchCompatibilityTest.java`
2. ✅ `EncryptionAnalysisTest.java`
3. ✅ `SearchInvestigationTest.java`
4. ✅ `DebugPublicSearchTest.java`
5. ✅ `SearchSpecialistAPIComprehensiveTest.java`
6. ✅ `SearchSpecialistAPIRigorousTest.java`
7. ✅ `SearchSpecialistAPIOnOffChainTest.java`
8. ✅ `UserFriendlyEncryptionAPITest.java`
9. ✅ `UserSearchTypeTest.java`
10. ✅ `UserFriendlyEncryptionAPIRecipientTest.java`

**Total:** 339 test cases updated to comply with v1.0.6 security model.

---

## Common Mistakes

### ❌ Mistake 1: Creating User Without Authentication

```java
❌ WRONG:
Blockchain blockchain = new Blockchain();
UserFriendlyEncryptionAPI api = new UserFriendlyEncryptionAPI(blockchain);

// This will throw SecurityException!
KeyPair userKeys = api.createUser("test-user");
```

**Error:**
```
SecurityException: ❌ AUTHORIZATION REQUIRED: Must set authorized
credentials before creating users.

Solution:
  1. Load genesis admin keys:
     KeyPair genesisKeys = KeyFileLoader.loadKeyPairFromFiles(
         "./keys/genesis-admin.private",
         "./keys/genesis-admin.public"
     );
  2. Register bootstrap admin (REQUIRED!):
     blockchain.createBootstrapAdmin(
         CryptoUtil.publicKeyToString(genesisKeys.getPublic()),
         "BOOTSTRAP_ADMIN"
     );
  3. Set credentials: api.setDefaultCredentials("BOOTSTRAP_ADMIN", genesisKeys)
  4. Then create new users: api.createUser("newuser")
```

### ❌ Mistake 2: Authorizing Keys Without Authenticating

```java
❌ WRONG:
// Load bootstrap admin keys (RBAC v1.0.6+)
KeyPair bootstrapKeys = KeyFileLoader.loadKeyPairFromFiles(
    "./keys/genesis-admin.private",
    "./keys/genesis-admin.public"
);

// Register bootstrap admin in blockchain (REQUIRED!)
blockchain.createBootstrapAdmin(
    CryptoUtil.publicKeyToString(bootstrapKeys.getPublic()),
    "BOOTSTRAP_ADMIN"
);

KeyPair tempKeys = CryptoUtil.generateKeyPair();
String publicKey = CryptoUtil.publicKeyToString(tempKeys.getPublic());
blockchain.addAuthorizedKey(
    publicKey,
    "Test User",
    bootstrapKeys,      // Caller credentials (bootstrap admin)
    UserRole.USER       // Target role
);

// Missing: api.setDefaultCredentials("Test User", tempKeys);

KeyPair userKeys = api.createUser("Test User");  // ❌ SecurityException!
```

**Problem:** Authorized the key but didn't authenticate. `createUser()` checks `defaultKeyPair` which is still `null`.

### ❌ Mistake 3: Thinking Authorized Key = Can Create Self

```java
❌ WRONG ASSUMPTION:
// User thinks: "I authorized 'alice' so alice can create herself"
// Load bootstrap admin keys (RBAC v1.0.6+)
KeyPair bootstrapKeys = KeyFileLoader.loadKeyPairFromFiles(
    "./keys/genesis-admin.private",
    "./keys/genesis-admin.public"
);

// Register bootstrap admin in blockchain (REQUIRED!)
blockchain.createBootstrapAdmin(
    CryptoUtil.publicKeyToString(bootstrapKeys.getPublic()),
    "BOOTSTRAP_ADMIN"
);

blockchain.addAuthorizedKey(
    alicePublicKey,
    "alice",
    bootstrapKeys,      // Caller credentials (bootstrap admin)
    UserRole.USER       // Target role
);

// This will fail because NO ONE is authenticated yet
KeyPair aliceKeys = api.createUser("alice");  // ❌ SecurityException!
```

**Correct:** Someone ELSE (an admin) must be authenticated to create alice.

---

## Troubleshooting

### Error: "AUTHORIZATION REQUIRED: Must set authorized credentials"

**Cause:** `setDefaultCredentials()` was never called.

**Solution:**
```java
api.setDefaultCredentials("admin", adminKeys);  // ← Add this before createUser()
```

### Error: "Only authorized users can create new users"

**Cause:** The authenticated user's public key is not in authorized keys.

**Solution:**
```java
// Load bootstrap admin keys (RBAC v1.0.6+)
KeyPair bootstrapKeys = KeyFileLoader.loadKeyPairFromFiles(
    "./keys/genesis-admin.private",
    "./keys/genesis-admin.public"
);

// Register bootstrap admin in blockchain (REQUIRED!)
blockchain.createBootstrapAdmin(
    CryptoUtil.publicKeyToString(bootstrapKeys.getPublic()),
    "BOOTSTRAP_ADMIN"
);

String publicKey = CryptoUtil.publicKeyToString(adminKeys.getPublic());
blockchain.addAuthorizedKey(
    publicKey,
    "admin",
    bootstrapKeys,      // Caller credentials (bootstrap admin)
    UserRole.ADMIN      // Target role
);  // ← Authorize BEFORE authenticate
api.setDefaultCredentials("admin", adminKeys);
```

### Error: "User already exists" or Duplicate Key Violation

**Cause:** Trying to create a user with a username that already exists.

**Solution:**
- Use unique usernames per test (e.g., `"test-user-" + System.currentTimeMillis()`)
- Clean database with `blockchain.clearAndReinitialize()` in `@BeforeEach`

### Tests Pass Individually But Fail in Suite

**Cause:** Database state contamination between tests.

**Solution:**
```java
@BeforeEach
void setUp() {
    blockchain = new Blockchain();
    blockchain.clearAndReinitialize();  // ← Clean DB before each test
    // ... rest of setup
}
```

---

## Migration Checklist

### Upgrading Code to v1.0.6

- [ ] Identify all `api.createUser()` calls
- [ ] Ensure `setDefaultCredentials()` is called before each `createUser()`
- [ ] Verify caller is in authorized keys (`blockchain.addAuthorizedKey()`)
- [ ] Update tests to use admin authentication pattern
- [ ] Test that SecurityException is thrown when not authenticated
- [ ] Document the pre-authorization workflow in test setup

### Verifying Migration

```bash
# Run all tests to verify 339 tests pass
./scripts/run_all_tests.zsh

# Look for SecurityException errors (should be none)
grep -r "AUTHORIZATION REQUIRED" logs/
```

---

## Related Documentation

- [KEY_MANAGEMENT_GUIDE.md](./KEY_MANAGEMENT_GUIDE.md) - Key storage and loading
- [SECURITY_GUIDE.md](./SECURITY_GUIDE.md) - General security practices
- [AUTO_AUTHORIZATION_SECURITY_FIX_PLAN.md](./AUTO_AUTHORIZATION_SECURITY_FIX_PLAN.md) - Original security plan
- [TESTING.md](../testing/TESTING.md) - Test patterns and standards

---

## References

- **Security Model:** v1.0.6 Pre-Authorization Requirement
- **Affected API:** `UserFriendlyEncryptionAPI.createUser()`
- **Implementation:** `UserFriendlyEncryptionAPI.java` lines 1457-1477
- **Test Pattern:** All test files using `createUser()`

---

**Last Updated:** 2025-11-04  
**Version:** 1.0.6+  
**Author:** Security Team  
**Status:** ✅ Production Ready
