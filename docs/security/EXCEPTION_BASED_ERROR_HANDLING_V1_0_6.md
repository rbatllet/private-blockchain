# Exception-Based Error Handling (v1.0.6+)

**Version:** 1.0.6
**Status:** ✅ IMPLEMENTED
**Breaking Change:** YES - Methods now throw exceptions instead of returning `false`

---

## Overview

Starting with v1.0.6, critical security and operational methods in `Blockchain.java` have migrated from returning `false` on errors to throwing specific exceptions. This **fail-fast** pattern ensures that security violations and critical errors cannot be silently ignored.

### Why This Change?

**Problem:** Methods returning `false` allowed critical errors to be ignored:
```java
// ❌ OLD (insecure - error easily ignored)
boolean deleted = blockchain.dangerouslyDeleteAuthorizedKey(key, true, reason, sig, admin);
// If deleted is false, why? Security violation? Key not found? Backup failed?
```

**Solution:** Exceptions force explicit error handling:
```java
// ✅ NEW (secure - must handle errors)
try {
    blockchain.dangerouslyDeleteAuthorizedKey(key, true, reason, sig, admin);
} catch (SecurityException e) {
    // Invalid admin authorization
} catch (IllegalStateException e) {
    // Safety block or backup failed
} catch (IllegalArgumentException e) {
    // Key does not exist
}
```

---

## Affected Methods

### 🔐 Security Methods (Critical)

#### 1. `revokeAuthorizedKey(String publicKeyString)`

**Throws:**
- `IllegalArgumentException` - Public key is null/empty or key not found/already inactive
- `IllegalStateException` - Cannot revoke the last active SUPER_ADMIN (system lockout protection)

**Example:**
```java
try {
    blockchain.revokeAuthorizedKey(publicKey);
    System.out.println("✅ Key revoked successfully");
} catch (IllegalArgumentException e) {
    System.err.println("❌ Invalid key: " + e.getMessage());
} catch (IllegalStateException e) {
    System.err.println("❌ Cannot revoke: " + e.getMessage());
    // This is the last SUPER_ADMIN - create another one first
}
```

---

#### 2. `deleteAuthorizedKey(String publicKey)`

**Throws:**
- `IllegalArgumentException` - Key does not exist
- `IllegalStateException` - Key has severe impact (signed historical blocks)

**Example:**
```java
try {
    blockchain.deleteAuthorizedKey(publicKey);
    System.out.println("✅ Key deleted successfully");
} catch (IllegalArgumentException e) {
    System.err.println("❌ Key not found: " + e.getMessage());
} catch (IllegalStateException e) {
    System.err.println("❌ Safety block: " + e.getMessage());
    // Key signed historical blocks - use dangerouslyDeleteAuthorizedKey() with force=true
}
```

---

### 🔄 Operational Methods

#### 3. `rollbackBlocks(Long numberOfBlocks)`

**Throws:**
- `IllegalArgumentException` - numberOfBlocks is <=0 or >= total blocks

**Example:**
```java
try {
    blockchain.rollbackBlocks(5L);
    System.out.println("✅ Rollback completed");
} catch (IllegalArgumentException e) {
    System.err.println("❌ Invalid rollback: " + e.getMessage());
    // numberOfBlocks is invalid or exceeds chain length
}
```

---

#### 4. `rollbackToBlock(Long targetBlockNumber)`

**Throws:**
- `IllegalArgumentException` - targetBlockNumber is null, negative, or doesn't exist

**Example:**
```java
try {
    blockchain.rollbackToBlock(100L);
    System.out.println("✅ Rolled back to block 100");
} catch (IllegalArgumentException e) {
    System.err.println("❌ Invalid target block: " + e.getMessage());
}
```

---

### 📁 Export/Import Methods

#### 5. `exportChain(String filePath)`
#### 6. `exportChain(String filePath, boolean includeOffChainFiles)`

**Throws:**
- `IllegalArgumentException` - filePath is null/empty or not a .json file
- `SecurityException` - Path traversal attempt detected
- `IllegalStateException` - Cannot create export directory

**Example:**
```java
try {
    blockchain.exportChain("backup.json");
    System.out.println("✅ Chain exported");
} catch (IllegalArgumentException e) {
    System.err.println("❌ Invalid file path: " + e.getMessage());
} catch (SecurityException e) {
    System.err.println("❌ SECURITY: " + e.getMessage());
    // Path traversal attack detected
} catch (IllegalStateException e) {
    System.err.println("❌ Cannot create directory: " + e.getMessage());
}
```

---

#### 7. `importChain(String filePath)`

**Throws:**
- `IllegalArgumentException` - filePath is null/empty, not a .json file, or file doesn't exist
- `SecurityException` - Path traversal attempt detected
- `IllegalStateException` - File is not readable

**Example:**
```java
try {
    blockchain.importChain("backup.json");
    System.out.println("✅ Chain imported");
} catch (IllegalArgumentException e) {
    System.err.println("❌ Invalid file: " + e.getMessage());
} catch (SecurityException e) {
    System.err.println("❌ SECURITY: " + e.getMessage());
} catch (IllegalStateException e) {
    System.err.println("❌ File not accessible: " + e.getMessage());
}
```

---

## Migration Guide

### For Application Code

**Before v1.0.6:**
```java
boolean result = blockchain.revokeAuthorizedKey(publicKey);
if (!result) {
    System.err.println("Failed to revoke key");
}
```

**After v1.0.6:**
```java
try {
    blockchain.revokeAuthorizedKey(publicKey);
    System.out.println("✅ Key revoked");
} catch (IllegalArgumentException | IllegalStateException e) {
    System.err.println("❌ Revocation failed: " + e.getMessage());
}
```

### For Test Code

**Before v1.0.6:**
```java
boolean deleted = blockchain.deleteAuthorizedKey(publicKey);
assertFalse(deleted, "Should fail when key has historical impact");
```

**After v1.0.6:**
```java
IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
    blockchain.deleteAuthorizedKey(publicKey);
}, "Should throw when key has historical impact");
assertTrue(exception.getMessage().contains("SAFETY BLOCK"));
```

---

## Exception Hierarchy

```
Exception
├── RuntimeException
│   ├── IllegalArgumentException (invalid parameters, key not found, file not found)
│   ├── IllegalStateException (safety blocks, system protection, file not accessible)
│   └── SecurityException (path traversal, invalid signatures)
```

### When to Catch Each Exception

- **`IllegalArgumentException`**: Invalid input (null, empty, out of range, not found)
- **`IllegalStateException`**: System protection or state prevents operation (last SUPER_ADMIN, safety blocks, file issues)
- **`SecurityException`**: Security violation detected (path traversal, invalid authorization)

---

## Backward Compatibility

⚠️ **BREAKING CHANGE**: This is a breaking API change. Code that relies on boolean returns must be updated to handle exceptions.

**Methods that still return boolean:**
- These methods still return `true`/`false` on success/failure:
  - `addBlock()`
  - `validateBlock()`
  - `isKeyAuthorized()`
  - All validation methods

**Methods that now throw exceptions (v1.0.6+):**
- `dangerouslyDeleteAuthorizedKey()` (v1.0.6)
- `revokeAuthorizedKey()` (v1.0.6+)
- `deleteAuthorizedKey()` (v1.0.6+)
- `rollbackBlocks()` (v1.0.6+)
- `rollbackToBlock()` (v1.0.6+)
- `exportChain()` (v1.0.6+)
- `importChain()` (v1.0.6+)

---

## Benefits

✅ **Security**: Cannot ignore security violations
✅ **Debugging**: Clear error messages with context
✅ **Type Safety**: Different exceptions for different error types
✅ **Fail-Fast**: Errors are detected immediately
✅ **Best Practice**: Aligns with Java exception handling conventions

---

## See Also

- [Security Guide](SECURITY_GUIDE.md)
- [API Guide](../reference/API_GUIDE.md)
- [Technical Details](../reference/TECHNICAL_DETAILS.md)
- [Vulnerability Report: dangerouslyDeleteAuthorizedKey](VULNERABILITY_REPORT_CVE_addAuthorizedKey.md)
