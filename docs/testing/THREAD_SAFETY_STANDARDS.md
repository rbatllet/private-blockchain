# 🔒 Thread Safety Coding Standards

## 📋 Overview

This document establishes comprehensive thread safety standards for the Private Blockchain project. Following these guidelines ensures robust, concurrent-safe code that maintains data integrity under high-load conditions.

> **🔄 CODE UPDATE (v1.0.6+)**: Methods like `revokeAuthorizedKey()`, `rollbackToBlock()` now throw exceptions instead of returning `false`. Update test assertions accordingly. See [Exception-Based Error Handling Guide](../security/EXCEPTION_BASED_ERROR_HANDLING_V1_0_6.md).

---

## 🎯 Core Principles

### 1. **Defensive Programming**
- ✅ **Always assume concurrent access** to shared data structures
- ✅ **Implement defensive copying** for all collection returns
- ✅ **Use immutable objects** when possible
- ✅ **Protect internal state** from external modification

### 2. **Thread-Safe Collections First**
- ✅ **Prefer ConcurrentHashMap** over HashMap for shared data
- ✅ **Use Collections.synchronizedList()** for shared lists
- ✅ **Use CopyOnWriteArrayList** for read-heavy scenarios
- ✅ **Use AtomicReference** for single shared objects

### 3. **Clear Synchronization Strategy**
- ✅ **Document thread safety** in JavaDoc comments
- ✅ **Use consistent locking patterns** across similar operations
- ✅ **Minimize lock scope** to reduce contention
- ✅ **Avoid nested locks** to prevent deadlocks

---

## 📐 Collection Usage Rules

### 🟢 **Recommended Thread-Safe Collections**

#### **Maps:**
```java
// ✅ CORRECT - Thread-safe for concurrent access
private final Map<String, Object> cache = new ConcurrentHashMap<>();

// ❌ WRONG - Not thread-safe
private final Map<String, Object> cache = new HashMap<>();
```

#### **Lists:**
```java
// ✅ CORRECT - For moderate concurrent writes
private final List<String> items = Collections.synchronizedList(new ArrayList<>());

// ✅ CORRECT - For read-heavy, infrequent writes
private final List<String> items = new CopyOnWriteArrayList<>();

// ❌ WRONG - Not thread-safe
private final List<String> items = new ArrayList<>();
```

#### **Sets:**
```java
// ✅ CORRECT - Thread-safe set
private final Set<String> keys = Collections.synchronizedSet(new HashSet<>());
private final Set<String> keys = ConcurrentHashMap.newKeySet();

// ❌ WRONG - Not thread-safe
private final Set<String> keys = new HashSet<>();
```

### 🔒 **Defensive Copying Pattern**

#### **Returning Collections:**
```java
// ✅ CORRECT - Immutable view protects internal state
public List<Item> getItems() {
    return Collections.unmodifiableList(items);
}

public Map<String, Object> getMetadata() {
    return Collections.unmodifiableMap(metadata);
}

// ❌ WRONG - Exposes mutable internal state
public List<Item> getItems() {
    return items; // External code can modify this!
}
```

#### **Accepting Collections:**
```java
// ✅ CORRECT - Defensive copying on input
public void setItems(List<Item> newItems) {
    this.items.clear();
    this.items.addAll(newItems);
}

// ✅ CORRECT - For constructor
public MyClass(List<Item> items) {
    this.items = new ArrayList<>(items);
}
```

---

## 🔐 Synchronization Patterns

### **1. AtomicReference for Single Objects**
```java
// ✅ CORRECT - Thread-safe single object reference
private final AtomicReference<KeyPair> defaultKeyPair = new AtomicReference<>();

public void setDefaultKeyPair(KeyPair keyPair) {
    defaultKeyPair.set(keyPair);
}

public KeyPair getDefaultKeyPair() {
    return defaultKeyPair.get();
}
```

### **2. StampedLock for Complex Operations**
```java
// ✅ CORRECT - StampedLock for read-heavy scenarios (used in Blockchain.java)
private final StampedLock lock = new StampedLock();

// Optimistic read (best performance, lock-free)
public Block readOperationOptimistic(long id) {
    long stamp = lock.tryOptimisticRead();
    Block result = dao.getById(id);

    if (!lock.validate(stamp)) {
        // Concurrent write detected - retry with read lock
        stamp = lock.readLock();
        try {
            result = dao.getById(id);
        } finally {
            lock.unlockRead(stamp);
        }
    }
    return result;
}

// Conservative read lock
public List<Block> readOperation() {
    long stamp = lock.readLock();
    try {
        return dao.getAll();
    } finally {
        lock.unlockRead(stamp);
    }
}

// Write lock
public void writeOperation(Block block) {
    long stamp = lock.writeLock();
    try {
        dao.save(block);
    } finally {
        lock.unlockWrite(stamp);
    }
}

// ⚠️ WARNING: StampedLock is NOT reentrant - avoid nested locks!
```

### **3. Synchronized Methods for Simple Cases**
```java
// ✅ CORRECT - Simple synchronized access
public synchronized void addItem(Item item) {
    items.add(item);
}

public synchronized List<Item> getAllItems() {
    return new ArrayList<>(items);
}
```

---

## 📝 Documentation Standards

### **JavaDoc Requirements**

#### **Thread-Safe Classes:**
```java
/**
 * Search metrics tracking with comprehensive thread safety.
 * 
 * <p><strong>Thread Safety:</strong> This class is fully thread-safe.
 * All public methods can be called concurrently from multiple threads
 * without external synchronization.</p>
 * 
 * <p><strong>Implementation:</strong> Uses ConcurrentHashMap for internal
 * storage and defensive copying for all collection returns.</p>
 * 
 * @since 1.0
 */
public class SearchMetrics {
    // Implementation
}
```

#### **Thread-Unsafe Classes:**
```java
/**
 * Block validation result container.
 * 
 * <p><strong>Thread Safety:</strong> This class is NOT thread-safe.
 * It is designed as an immutable result object. Do not modify
 * after construction.</p>
 * 
 * @since 1.0
 */
public class BlockValidationResult {
    // Implementation
}
```

#### **Getter Methods:**
```java
/**
 * Returns an immutable view of the current metrics data.
 * 
 * <p><strong>Thread Safety:</strong> This method is thread-safe and
 * returns an immutable collection that cannot be modified by callers.</p>
 * 
 * @return unmodifiable map of search type statistics
 */
public Map<String, PerformanceStats> getSearchTypeStats() {
    return Collections.unmodifiableMap(searchTypeStats);
}
```

---

## ✅ Code Review Checklist

### **🔍 Thread Safety Review Points**

#### **1. Collection Usage:**
- [ ] **No HashMap/ArrayList** in shared/static fields
- [ ] **ConcurrentHashMap** used for shared maps
- [ ] **Collections.synchronizedList()** used for shared lists
- [ ] **Defensive copying** implemented for all getters

#### **2. Method Safety:**
- [ ] **Collections.unmodifiableList/Map/Set()** used in getters
- [ ] **No direct field exposure** in public methods
- [ ] **Proper synchronization** for complex operations
- [ ] **AtomicReference** used for single shared objects

#### **3. Documentation:**
- [ ] **Thread safety documented** in class JavaDoc
- [ ] **Synchronization strategy** explained
- [ ] **Usage examples** provided for complex patterns
- [ ] **Warnings** included for non-thread-safe classes

#### **4. Testing:**
- [ ] **Thread safety tests** included for concurrent classes
- [ ] **Stress tests** implemented for high-load scenarios
- [ ] **Race condition testing** for critical operations
- [ ] **Defensive copying** validated in tests

---

## 🧪 Testing Requirements

### **Mandatory Tests for Thread-Safe Classes:**

#### **1. Basic Concurrent Operations:**
```java
@Test
@DisplayName("🔐 Thread safety: Concurrent operations")
@Timeout(30)
void testConcurrentOperations() throws Exception {
    final int NUM_THREADS = 20;
    final int OPERATIONS_PER_THREAD = 50;

    ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor(); // Java 25 Virtual Threads
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch completionLatch = new CountDownLatch(NUM_THREADS);
    
    // Test implementation
}
```

#### **2. Defensive Copying Validation:**
```java
@Test
@DisplayName("🛡️ Defensive copying: Collection immutability")
void testDefensiveCopying() {
    // Add data to object
    MyClass obj = new MyClass();
    obj.addItem("test");
    
    // Get collection
    List<String> items = obj.getItems();
    
    // Attempt modification (should fail or not affect internal state)
    assertThrows(UnsupportedOperationException.class, () -> {
        items.add("malicious");
    });
    
    // Verify internal state unchanged
    assertEquals(1, obj.getItems().size());
}
```

#### **3. Race Condition Testing:**
```java
@Test
@DisplayName("🏃‍♂️ Race condition: Concurrent modifications")
void testRaceConditionProtection() throws Exception {
    // High-contention scenario testing
}
```

---

## 🚫 Common Anti-Patterns

### **❌ What NOT to Do:**

#### **1. Unsafe Collection Sharing:**
```java
// ❌ WRONG - Exposes mutable internal state
public class BadExample {
    private List<String> items = new ArrayList<>();
    
    public List<String> getItems() {
        return items; // Dangerous!
    }
}
```

#### **2. Inconsistent Synchronization:**
```java
// ❌ WRONG - Some methods synchronized, others not
public class InconsistentExample {
    private List<String> items = new ArrayList<>();
    
    public synchronized void addItem(String item) { 
        items.add(item); 
    }
    
    public List<String> getItems() { 
        return items; // Not synchronized!
    }
}
```

#### **3. Nested Lock Risks:**
```java
// ❌ WRONG - Potential deadlock
public synchronized void methodA() {
    synchronized(otherObject) {
        // Nested locks = deadlock risk
    }
}
```

---

## 🔧 Migration Guide

### **Converting Existing Code:**

#### **Step 1: Identify Non-Thread-Safe Collections**
```bash
# Search for problematic patterns
grep -r "new HashMap<>" src/main/java/
grep -r "new ArrayList<>" src/main/java/
grep -r "return.*List.*;" src/main/java/
```

#### **Step 2: Replace with Thread-Safe Alternatives**
```java
// Before
private final Map<String, Object> cache = new HashMap<>();

// After  
private final Map<String, Object> cache = new ConcurrentHashMap<>();
```

#### **Step 3: Add Defensive Copying**
```java
// Before
public List<Item> getItems() {
    return items;
}

// After
public List<Item> getItems() {
    return Collections.unmodifiableList(items);
}
```

#### **Step 4: Update Documentation**
```java
// Add thread safety documentation to class and methods
```

#### **Step 5: Add Tests**
```java
// Implement concurrent testing for modified classes
```

---

## 📊 Performance Considerations

### **Thread-Safe Collection Performance:**

| Collection Type | Read Performance | Write Performance | Memory Overhead | Best For |
|----------------|------------------|-------------------|-----------------|----------|
| `ConcurrentHashMap` | High | High | Low | General shared maps |
| `Collections.synchronizedMap()` | Medium | Low | Low | Light concurrent access |
| `CopyOnWriteArrayList` | Very High | Low | High | Read-heavy scenarios |
| `Collections.synchronizedList()` | Medium | Medium | Low | Moderate concurrent access |

### **Best Practices:**
- ✅ **Use ConcurrentHashMap** for most shared map scenarios
- ✅ **Use CopyOnWriteArrayList** for configuration/reference lists
- ✅ **Use Collections.synchronizedList()** for moderate concurrent writes
- ✅ **Profile performance** for high-throughput scenarios

---

## 🏆 Success Examples

### **SearchMetrics Class** (Reference Implementation)
```java
public class SearchMetrics {
    // ✅ Thread-safe storage
    private final Map<String, PerformanceStats> searchTypeStats = new ConcurrentHashMap<>();
    
    // ✅ Atomic counters
    private final AtomicLong totalSearches = new AtomicLong(0);
    
    // ✅ Defensive copying
    public Map<String, PerformanceStats> getSearchTypeStats() {
        return Collections.unmodifiableMap(searchTypeStats);
    }
    
    // ✅ Thread-safe operations
    public void recordSearch(String searchType, long duration, int results, boolean cacheHit) {
        totalSearches.incrementAndGet();
        searchTypeStats.computeIfAbsent(searchType, k -> new PerformanceStats())
                      .recordSearch(duration, results, cacheHit);
    }
}
```

### **PerformanceSnapshot Class** (Enhanced Defensive Implementation)

The `PerformanceSnapshot` inner class demonstrates advanced defensive programming and thread safety patterns:

```java
/**
 * Thread-safe performance snapshot with comprehensive defensive programming.
 * 
 * <p><strong>Thread Safety:</strong> Immutable after construction with thread-safe collections.</p>
 * <p><strong>Defensive Programming:</strong> Validates and sanitizes all input parameters.</p>
 * <p><strong>Robustness:</strong> Handles NaN, null, and negative values gracefully.</p>
 */
public static class PerformanceSnapshot {
    // ✅ Immutable fields after construction
    private final long totalSearches;
    private final double averageDuration;
    private final double cacheHitRate;
    private final LocalDateTime lastSearchTime;
    private final Map<String, PerformanceStats> searchTypeStats;
    private final LocalDateTime startTime;
    
    // ✅ Defensive constructor with comprehensive validation
    PerformanceSnapshot(long totalSearches, double averageDuration, double cacheHitRate,
                       long searchesSinceStart, LocalDateTime lastSearchTime,
                       Map<String, PerformanceStats> searchTypeStats, LocalDateTime startTime) {
        
        // Sanitize numeric inputs
        this.totalSearches = Math.max(0, totalSearches);
        this.averageDuration = Double.isNaN(averageDuration) ? 0.0 : Math.max(0.0, averageDuration);
        this.cacheHitRate = Double.isNaN(cacheHitRate) ? 0.0 : Math.max(0.0, Math.min(100.0, cacheHitRate));
        
        // Handle null references safely
        this.lastSearchTime = lastSearchTime;
        this.startTime = (startTime != null) ? startTime : LocalDateTime.now();
        
        // Thread-safe defensive copying
        this.searchTypeStats = (searchTypeStats != null) ? 
            new ConcurrentHashMap<>(searchTypeStats) : new ConcurrentHashMap<>();
    }
    
    // ✅ Thread-safe access methods with defensive programming
    public Map<String, Long> getSearchTypeCounts() {
        return searchTypeStats.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> Math.max(0L, entry.getValue().getCount()),
                (a, b) -> a,
                ConcurrentHashMap::new  // Thread-safe result collection
            ));
    }
    
    // ✅ Robust validation with null safety
    public boolean hasValidData() {
        return totalSearches > 0 && lastSearchTime != null && startTime != null &&
               !Double.isInfinite(averageDuration) && !Double.isInfinite(cacheHitRate);
    }
}
```

**Thread Safety Guarantees:**
- **Immutable State**: All fields are final and immutable after construction
- **Thread-Safe Collections**: Uses ConcurrentHashMap for defensive copying
- **Null Safety**: Comprehensive null checking and default value provision
- **Data Validation**: Input sanitization prevents invalid state
- **Lock-Free Access**: All getter methods are lock-free and thread-safe

---

## � StampedLock Deadlock Prevention (Critical!)

### **Problem: StampedLock is NOT Reentrant**

Unlike `ReentrantReadWriteLock`, `StampedLock` **does not** support nested lock acquisition. This caused 13 deadlocks during migration.

### **Dual-Mode Pattern Solution**

For methods that need to call blockchain operations while holding a lock:

```java
// Public method with lock (normal external use) - RBAC v1.0.6+
public boolean addAuthorizedKey(String publicKey, String owner,
                                KeyPair callerKeyPair, UserRole role) {
    long stamp = GLOBAL_BLOCKCHAIN_LOCK.writeLock();
    try {
        // RBAC validation happens here
        return addAuthorizedKeyInternal(publicKey, owner, role, createdBy);
    } finally {
        GLOBAL_BLOCKCHAIN_LOCK.unlockWrite(stamp);
    }
}

// Private internal method (single source of truth) - RBAC v1.0.6+
private boolean addAuthorizedKeyInternal(String publicKey, String owner,
                                        UserRole role, String createdBy) {
    // Actual implementation here - NO LOCK
    return JPAUtil.executeInTransaction(em -> {
        // ... business logic ...
    });
}

// Public method WITHOUT lock (for calling from within existing lock) - RBAC v1.0.6+
public boolean addAuthorizedKeyWithoutLock(String publicKey, String owner,
                                          UserRole role, String createdBy) {
    // ⚠️ WARNING: Only call this from within an existing lock!
    return addAuthorizedKeyInternal(publicKey, owner, role, createdBy);
}
```

### **Methods with Dual-Mode Pattern**

| Method | Usage Count | Context |
|--------|-------------|---------|
| `validateSingleBlock()` / `WithoutLock()` / `Internal()` | 6 | Block validation |
| `validateChainDetailed()` / `WithoutLock()` / `Internal()` | 3 | Chain validation |
| `getAuthorizedKeys()` / `WithoutLock()` / `Internal()` | 2 | Key retrieval |
| `addAuthorizedKey()` / `WithoutLock()` / `Internal()` | 1 | Key authorization |
| `revokeAuthorizedKey()` / `WithoutLock()` / `Internal()` | 1 | Key revocation |
| `rollbackToBlock()` / `WithoutLock()` / `Internal()` | 1 | Chain rollback |

### **Usage Example (ChainRecoveryManager)**

```java
public class ChainRecoveryManager {
    private final boolean calledWithinLock;
    
    public ChainRecoveryManager(Blockchain blockchain, boolean calledWithinLock) {
        this.blockchain = blockchain;
        this.calledWithinLock = calledWithinLock;
    }
    
    public void diagnoseCorruption() {
        // Conditional call based on lock context
        boolean valid = calledWithinLock 
            ? blockchain.validateSingleBlockWithoutLock(block)
            : blockchain.validateSingleBlock(block);
    }
}
```

### **LockTracer Debugging Utility**

All locks wrapped with `LockTracer` for automatic debugging:

```java
private static final LockTracer GLOBAL_BLOCKCHAIN_LOCK =
    new LockTracer(new StampedLock(), "GLOBAL_BLOCKCHAIN");
```

**Features:**
- Logs: `ACQUIRING → ACQUIRED → RELEASING → RELEASED`
- Thread name, lock name, stamp value
- Only active in DEBUG mode (zero production overhead)
- Enable: Set `com.rbatllet.blockchain.util.LockTracer` to DEBUG

**Example Output:**
```
🔒 [pool-3-thread-5] ACQUIRING writeLock on GLOBAL_BLOCKCHAIN
✅ [pool-3-thread-5] ACQUIRED writeLock on GLOBAL_BLOCKCHAIN (stamp=508800)
🔓 [pool-3-thread-5] RELEASING writeLock on GLOBAL_BLOCKCHAIN (stamp=508800)
✅ [pool-3-thread-5] RELEASED writeLock on GLOBAL_BLOCKCHAIN (stamp=508800)
```

### **Fixed Deadlocks Summary**

Total: **13 deadlocks** fixed during StampedLock migration

**Day 1 (5 deadlocks):**
- Various ReentrantReadWriteLock to StampedLock conversion issues

**Day 2 (8 deadlocks):**
- #6-7: `diagnoseCorruption()` and `processChainInBatches()` readLock reentrancy
- #8-13: `recoverCorruptedChain()` calling methods that try to acquire locks:
  - validateSingleBlock (readLock)
  - validateChainDetailed (readLock)
  - getAuthorizedKeys (readLock)
  - addAuthorizedKey (writeLock)
  - revokeAuthorizedKey (writeLock)
  - rollbackToBlock (writeLock)

**Key Learnings:**
1. ✅ StampedLock requires dual-mode pattern for internal calls
2. ✅ LockTracer is essential for identifying deadlock locations
3. ✅ Always document which methods are safe to call from within locks
4. ✅ Use `calledWithinLock` flag for conditional lock acquisition

---

## �📚 Additional Resources

### **Internal References:**
- [Thread Safety Tests Guide](THREAD_SAFETY_TESTS.md)
- [Thread Safety & Concurrent Indexing](../monitoring/INDEXING_COORDINATOR_EXAMPLES.md#thread-safety--concurrent-indexing)
- [Semaphore-Based Block Indexing Implementation](../search/SEMAPHORE_INDEXING_IMPLEMENTATION.md)
- [Security Classes Guide](../security/SECURITY_CLASSES_GUIDE.md)
- [Performance Optimization](../reports/PERFORMANCE_OPTIMIZATION_SUMMARY.md)

### **External Resources:**
- [Java Concurrency in Practice](https://jcip.net/)
- [Oracle Java Concurrency Tutorial](https://docs.oracle.com/javase/tutorial/essential/concurrency/)
- [Java Memory Model](https://shipilev.net/blog/2016/close-encounters-of-jmm-kind/)

---

*Last updated: 2025-10-04*  
*Version: 2.0*  
*Maintainer: Private Blockchain Development Team*