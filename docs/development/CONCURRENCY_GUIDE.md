# StampedLock Concurrency Guide

The blockchain uses `StampedLock` (Java 8+) for thread synchronization, providing superior read performance through optimistic reads.

> **🔄 CODE UPDATE (v1.0.6+)**: Some methods shown in examples now throw exceptions instead of returning `false` (e.g., `revokeAuthorizedKey()`, `rollbackToBlock()`). See [Exception-Based Error Handling Guide](../security/EXCEPTION_BASED_ERROR_HANDLING_V1_0_6.md).

## Critical: StampedLock is NOT Reentrant

**⚠️ CRITICAL WARNING**

Unlike traditional locks, StampedLock does **not** support nested lock acquisition. Attempting to acquire a lock while already holding one **will cause deadlock**.

## LockTracer Debugging Utility

All locks are wrapped with `LockTracer` for automatic debugging:
- Logs: ACQUIRING → ACQUIRED → RELEASING → RELEASED
- Thread name, lock name, stamp value
- Only active in DEBUG mode (zero production overhead)
- Enable: Set `com.rbatllet.blockchain.util.LockTracer` to DEBUG in log4j2-test.xml

## Dual-Mode Pattern for Internal Calls

To prevent deadlocks in methods that need to call blockchain operations while holding a lock, we use the dual-mode pattern:

```java
// Public method with lock
public List<AuthorizedKey> getAuthorizedKeys() {
    long stamp = GLOBAL_BLOCKCHAIN_LOCK.readLock();
    try {
        return getAuthorizedKeysInternal();
    } finally {
        GLOBAL_BLOCKCHAIN_LOCK.unlockRead(stamp);
    }
}

// Internal method without lock (single source of truth)
private List<AuthorizedKey> getAuthorizedKeysInternal() {
    // Actual implementation here
}

// Public method for calling from within existing lock (with WARNING!)
public List<AuthorizedKey> getAuthorizedKeysWithoutLock() {
    return getAuthorizedKeysInternal();
}
```

### Methods with Dual-Mode Pattern

- `validateSingleBlock()` / `WithoutLock()` / `Internal()`
- `validateChainDetailed()` / `WithoutLock()` / `Internal()`
- `getAuthorizedKeys()` / `WithoutLock()` / `Internal()`
- `addAuthorizedKey()` / `WithoutLock()` / `Internal()`
- `revokeAuthorizedKey()` / `WithoutLock()` / `Internal()`
- `rollbackToBlock()` / `WithoutLock()` / `Internal()`

### Example Usage (ChainRecoveryManager)

```java
// Called from within Blockchain.recoverCorruptedChain() which holds writeLock
boolean valid = calledWithinLock
    ? blockchain.validateSingleBlockWithoutLock(block)
    : blockchain.validateSingleBlock(block);
```

## Lock Patterns

### Pattern 1: Optimistic Read (Best Performance)

```java
// ✅ GOOD - Optimistic read (lock-free, best performance)
// Core methods: getBlock(), getBlockCount(), getLastBlock()
long stamp = GLOBAL_BLOCKCHAIN_LOCK.tryOptimisticRead();
Block block = blockRepository.getBlockByNumber(blockNumber);

if (!GLOBAL_BLOCKCHAIN_LOCK.validate(stamp)) {
    // Concurrent write detected - retry with read lock
    stamp = GLOBAL_BLOCKCHAIN_LOCK.readLock();
    try {
        block = blockRepository.getBlockByNumber(blockNumber);
    } finally {
        GLOBAL_BLOCKCHAIN_LOCK.unlockRead(stamp);
    }
}
return block;
```

### Pattern 2: Read Lock (Conservative, Safe)

```java
// ✅ GOOD - Read lock (conservative, safe)
// Most read operations use this pattern
long stamp = GLOBAL_BLOCKCHAIN_LOCK.readLock();
try {
    return blockRepository.someReadOperation();
} finally {
    GLOBAL_BLOCKCHAIN_LOCK.unlockRead(stamp);
}
```

### Pattern 3: Write Lock

```java
// ✅ GOOD - Write lock
long stamp = GLOBAL_BLOCKCHAIN_LOCK.writeLock();
try {
    blockRepository.someWriteOperation();
} finally {
    GLOBAL_BLOCKCHAIN_LOCK.unlockWrite(stamp);
}
```

## Anti-Patterns (NEVER DO THIS!)

### Anti-Pattern 1: Nested Lock Acquisition

```java
// ❌ BAD - Nested lock acquisition (DEADLOCK!)
long stamp1 = GLOBAL_BLOCKCHAIN_LOCK.readLock();
try {
    long stamp2 = GLOBAL_BLOCKCHAIN_LOCK.readLock();  // DEADLOCK!
} finally {
    GLOBAL_BLOCKCHAIN_LOCK.unlockRead(stamp1);
}
```

### Anti-Pattern 2: Calling Blockchain Methods from Within Locked Code

```java
// ❌ BAD - Calling blockchain methods from within locked code
long stamp = GLOBAL_BLOCKCHAIN_LOCK.readLock();
try {
    blockchain.getBlock(0);  // DEADLOCK! getBlock() tries to acquire lock again
} finally {
    GLOBAL_BLOCKCHAIN_LOCK.unlockRead(stamp);
}
```

## Performance Characteristics

- **Optimistic reads**: Lock-free, zero cache invalidation
- **Read locks**: Shared access, multiple concurrent readers
- **Write locks**: Exclusive access, blocks all readers and writers

## Best Practices

1. ✅ Use optimistic reads for hot-path operations (e.g., frequent getBlock() calls)
2. ✅ Never call blockchain public methods from within locked code
3. ✅ Keep critical sections as short as possible
4. ✅ Prefer blockchain's public API over direct DAO access

## UserFriendlyEncryptionAPI Thread-Safety

### Critical: Credential Atomicity

The `UserFriendlyEncryptionAPI` uses `AtomicReference` for credentials, but **setting username and keyPair must be atomic together**:

```java
// ✅ CORRECT - Synchronized credentials update
synchronized (credentialsLock) {
    this.defaultUsername.set(username);
    this.defaultKeyPair.set(keyPair);
}

// ❌ INCORRECT - Race condition (username/keyPair mismatch)
this.defaultUsername.set(username);  // Thread B can enter here!
this.defaultKeyPair.set(keyPair);    // Result: username from B, keyPair from A
```

### Fixed Methods

- `setDefaultCredentials()` - Now synchronized with `credentialsLock`
- `getDefaultUsername()` - Now synchronized with `credentialsLock`
- `getDefaultKeyPair()` - Now synchronized with `credentialsLock`

**Impact:** Fixed critical bug where concurrent credential changes caused username/keyPair mismatches (99% failure rate → 100% success rate in stress tests).

## Recipient Encryption Storage (Oct 2024)

**⚠️ CRITICAL: Recipient encryption storage mechanism changed**

Recipient-encrypted blocks now store recipient information in the **mutable `encryptionMetadata` field** (as JSON), NOT in the immutable `data` field.

### Problem

Originally, recipient info was stored as prefix in `data` field:
```java
block.setData("RECIPIENT_ENCRYPTED:username:" + encryptedContent);  // ❌ IGNORED by JPA!
```

This failed because `data` has `@Column(updatable=false)` for blockchain integrity protection. JPA silently ignored all `setData()` calls on existing blocks.

### Solution

Store recipient info in `encryptionMetadata` field as JSON:
```java
// Correct format stored in encryptionMetadata field
{"type":"RECIPIENT_ENCRYPTED","recipient":"username"}
```

### Affected Methods (UserFriendlyEncryptionAPI.java)

- `createRecipientEncryptedBlock()` - Stores JSON in encryptionMetadata
- `decryptRecipientBlock()` - Reads from encryptionMetadata
- `isRecipientEncrypted()` - Checks encryptionMetadata.contains("RECIPIENT_ENCRYPTED")
- `getRecipientUsername()` - Extracts recipient from JSON

### Key Insight

The two-layer blockchain integrity protection worked as designed:
1. **JPA Layer**: `@Column(updatable=false)` prevents accidental data modification
2. **API Layer**: `Blockchain.updateBlock()` validates intentional modifications

This architecture prevented the recipient encryption bug from corrupting the blockchain, but also required moving recipient metadata to a mutable field.

## Additional Documentation

- See [THREAD_SAFETY_STANDARDS.md](../testing/THREAD_SAFETY_STANDARDS.md) for concurrency patterns
- See [GLOBAL_BLOCKCHAIN_LOCK_ANALYSIS.md](../reports/GLOBAL_BLOCKCHAIN_LOCK_ANALYSIS.md) for lock architecture
