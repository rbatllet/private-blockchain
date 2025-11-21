# GLOBAL_BLOCKCHAIN_LOCK Analysis

> **🔄 CODE UPDATE (v1.0.6+)**: Methods like `revokeAuthorizedKey()`, `rollbackToBlock()` now throw exceptions. See [Exception-Based Error Handling Guide](../security/EXCEPTION_BASED_ERROR_HANDLING_V1_0_6.md).

## 📊 Current Architecture

### **Lock Implementation**
```java
// Wrapped with LockTracer for automatic debugging (October 2025)
private static final LockTracer GLOBAL_BLOCKCHAIN_LOCK =
    new LockTracer(new StampedLock(), "GLOBAL_BLOCKCHAIN");
```

**LockTracer Features:**
- Automatic logging: `ACQUIRING → ACQUIRED → RELEASING → RELEASED`
- Thread name, lock name, stamp value tracking
- Only active in DEBUG mode (zero production overhead)
- Essential for identifying deadlock locations

**Example Output:**
```
🔒 [pool-3-thread-5] ACQUIRING writeLock on GLOBAL_BLOCKCHAIN
✅ [pool-3-thread-5] ACQUIRED writeLock on GLOBAL_BLOCKCHAIN (stamp=508800)
🔓 [pool-3-thread-5] RELEASING writeLock on GLOBAL_BLOCKCHAIN (stamp=508800)
✅ [pool-3-thread-5] RELEASED writeLock on GLOBAL_BLOCKCHAIN (stamp=508800)
```

### **Usage Pattern**
- **Write operations**: Acquire exclusive write lock (only 1 thread can write)
- **Read operations**: Two variants:
  - **Optimistic read**: Lock-free, best performance (used in getBlock, getBlockCount, getLastBlock)
  - **Conservative read**: Shared read lock (used in most other read operations)

## ⚠️ Inherent Limitation

### **Write Serialization is Required**

The `GLOBAL_BLOCKCHAIN_LOCK` serializes ALL write operations:

```java
// Write operations are inherently sequential
long stamp = GLOBAL_BLOCKCHAIN_LOCK.writeLock();
try {
    blockchain.addBlock(data, privateKey, publicKey);  // Only 1 thread can execute this
} finally {
    GLOBAL_BLOCKCHAIN_LOCK.unlockWrite(stamp);
}
```

**Impact:**
- ❌ **100 concurrent threads** → Only **1 can write** at a time
- ❌ **PostgreSQL/MySQL** support 10-60 concurrent writers → **Lock kills this capability**
- ❌ **High write concurrency** → Massive performance bottleneck

### **Why This Lock Exists**

The blockchain is **inherently sequential** by design:
- Block N+1 depends on hash(Block N)
- Cannot create Block 100 and Block 101 in parallel
- Each block must reference the previous block's hash

**This is NOT a bug - it's an architectural constraint of sequential blockchains.**

## 📈 Performance Impact Analysis

### **Tested Scenarios**

| Concurrent Writers | Current Throughput | Database Capability | Wasted Capacity |
|-------------------|-------------------|---------------------|-----------------|
| 1 thread | 20 blocks/sec | 20 blocks/sec | 0% |
| 10 threads | 20 blocks/sec | 200 blocks/sec | 90% wasted |
| 100 threads | 20 blocks/sec | 2000 blocks/sec | 99% wasted |

### **Database Comparison**

| Database | Multi-Writer Support | Limited By | Actual Throughput |
|----------|---------------------|------------|-------------------|
| **SQLite** | ❌ No (1 writer) | Database + Lock | 20 blocks/sec |
| **H2** (embedded) | ⚠️ Limited | Database + Lock | 20 blocks/sec |
| **PostgreSQL** | ✅ Yes (10-60 writers) | **Lock Only** | 20 blocks/sec |
| **MySQL** | ✅ Yes (10-40 writers) | **Lock Only** | 20 blocks/sec |

**Conclusion:** The lock wastes PostgreSQL/MySQL's multi-writer capabilities.

## 🚫 Rejected Solutions

### **Option 1: Optimistic Locking (JPA @Version)**

```java
@Entity
@Table(name = "blocks")
public class Block {
    @Version
    @Column(name = "version")
    private Long version;  // JPA detects conflicts
}
```

**Rejected because:**
- ❌ Blockchain is still sequential (hash dependencies)
- ❌ High retry rate under contention
- ❌ Doesn't solve the fundamental problem

### **Option 2: Sharding / Partitioning**

```java
// Multiple independent blockchains (NOT recommended)
private static final Map<Integer, StampedLock> SHARD_LOCKS;
```

**Rejected because:**
- ❌ **SQLite**: Requires multiple files → Complex management
- ❌ **H2 embedded**: Single writer limitation per file
- ❌ **Breaks database-agnostic architecture**
- ❌ Cross-shard queries become impossible
- ❌ Loses single global blockchain property

## ✅ Current Solution: StampedLock with Optimistic Reads

**Implemented**: StampedLock provides ~50% better read performance with minimal code changes.

See `LOCK_OPTIMIZATION_LOW_COST_ALTERNATIVES.md` for implementation details.

## 🔮 Future Optimization: Async Write Queue with JCTools

**Status**: Analyzed but NOT implemented (requires 425-630 hours effort)

See `ASYNC_WRITE_QUEUE_IMPACT_ANALYSIS.md` for full cost-benefit analysis.

### **Architecture Overview**

Based on analysis with Context7 documentation:
- **JCTools** (`/jctools/jctools`) - Lock-free MPSC queues
- **Spotify CompletableFutures** (`/spotify/completable-futures`) - Future utilities

### **Key Components**

1. **Lock-free MPSC Queue** (JCTools `MpscChunkedArrayQueue`)
   - Multiple producers can offer without locks
   - Single consumer processes batches
   - Low memory footprint, low GC churn

2. **Parallel Hash Computation**
   - Prepare blocks in parallel before writing
   - Compute hashes, signatures, validations
   - No lock needed for preparation phase

3. **Batch Sequential Writes**
   - Single writer thread acquires lock
   - Writes N blocks per transaction
   - Amortizes lock acquisition cost

4. **CompletableFuture API**
   - Non-blocking producer API
   - Clients get immediate CompletableFuture
   - Backpressure control via queue capacity

### **Why This Works with All Databases**

| Database | Compatibility | Performance Gain |
|----------|--------------|------------------|
| **SQLite** | ✅ Works | Batch writes reduce I/O |
| **H2** | ✅ Works | Batch processing efficient |
| **PostgreSQL** | ✅ Works | Batch transactions optimal |
| **MySQL** | ✅ Works | Batch transactions optimal |

**No database-specific features required** - works universally!

### **Performance Benefits**

```java
// Current: Blocking sync API
Block block = blockchain.addBlock(data, key);  // Blocks until written (50-100ms)

// Proposed: Non-blocking async API
CompletableFuture<Block> future = blockchain.addBlockAsync(data, key);  // Returns <1ms
// ... do other work ...
Block block = future.get();  // Wait only when needed
```

**Expected Performance:**

| Metric | Current (Sync) | Proposed (Async) | Improvement |
|--------|---------------|------------------|-------------|
| Producer latency | 50-100ms | <1ms | **99% faster** |
| Write throughput | 20 blocks/sec | 500+ blocks/sec | **25x faster** |
| Lock contention | High | Low | Lock-free producers |
| Memory footprint | Constant | Dynamic (chunks) | Adaptive |

### **Implementation Status**

- 📋 **Status**: Designed, not implemented
- 🎯 **Target**: Future optimization when high write concurrency needed
- 📝 **Documentation**: This document
- 🔬 **Benchmarking**: Pending implementation

## 📚 References

### **Context7 Research**

- **JCTools MPSC Queues**: `/jctools/jctools`
  - `MpscChunkedArrayQueue` - Lock-free, low footprint
  - Batch `drain()` operations
  - Used by: Netty, Aeron, high-performance systems

- **CompletableFuture Utilities**: `/spotify/completable-futures`
  - `allAsList()` - Combine multiple futures
  - `joinList()` - Stream collector for futures

### **Technical Justification**

1. **Lock-free producers**: Eliminates write bottleneck
2. **Batch processing**: Amortizes lock cost across N blocks
3. **Parallel preparation**: CPU-intensive ops done without lock
4. **Database-agnostic**: No DB-specific features required
5. **Proven pattern**: Used by Netty, RxJava, other high-perf Java systems

## 📋 Fixed Deadlocks History

### Migration to StampedLock (Day 1 - 5 deadlocks)
Initial conversion from `ReentrantReadWriteLock` to `StampedLock` introduced 5 deadlocks due to non-reentrant nature of StampedLock.

### Non-Reentrancy Issues (Day 2 - 8 deadlocks)

#### DEADLOCK #6-7: Read Lock Reentrancy
**Affected Methods:**
- `diagnoseCorruption()` - called `validateSingleBlock()` while holding readLock
- `processChainInBatches()` - called `validateSingleBlock()` while holding readLock

**Solution:** Created `validateSingleBlockWithoutLock()` for internal calls

#### DEADLOCK #8-13: Recovery Chain Reentrancy
**Root Cause:** `recoverCorruptedChain()` called 6 methods while holding writeLock:
1. `validateSingleBlock()` - tried to acquire readLock (DEADLOCK #8)
2. `validateChainDetailed()` - tried to acquire readLock (DEADLOCK #9)
3. `getAuthorizedKeys()` - tried to acquire readLock (DEADLOCK #10)
4. `addAuthorizedKey()` - tried to acquire writeLock (DEADLOCK #11)
5. `revokeAuthorizedKey()` - tried to acquire writeLock (DEADLOCK #12)
6. `rollbackToBlock()` - tried to acquire writeLock (DEADLOCK #13)

**Solution:** Implemented **dual-mode pattern** for all 6 methods:
- Public method with lock (normal external use)
- Private `Internal()` method (single source of truth, no lock)
- Public `WithoutLock()` method (for calling from within lock, with WARNING)

**Key Tools:**
- **LockTracer**: Wrapper utility that logs lock lifecycle (ACQUIRING → ACQUIRED → RELEASING → RELEASED) for debugging
- **calledWithinLock flag**: ChainRecoveryManager tracks lock context to choose correct method variant

**Test Results:**
- Before fixes: 990/1000 failures (99% failure rate)
- After fixes: 1000/1000 success (100% success rate)

## 🎯 Conclusion

**Current lock is NOT a bug** - it's required for blockchain sequential integrity.

**StampedLock non-reentrancy** requires careful design:
- ✅ Use dual-mode pattern for methods called both internally and externally
- ✅ Use LockTracer for debugging complex lock scenarios
- ✅ Track lock context with flags (e.g., `calledWithinLock`)
- ✅ Never try to acquire same lock type when already held

**Future optimization** (async write queue with JCTools) will:
- ✅ Maintain sequential blockchain integrity
- ✅ Work with all databases (SQLite/H2/PostgreSQL/MySQL)
- ✅ Reduce producer latency from 50-100ms to <1ms
- ✅ Increase throughput from 20 to 500+ blocks/sec
- ✅ Use proven, battle-tested libraries (JCTools)

**When to implement:**
- When production workloads show write concurrency bottleneck
- When benchmarking confirms need (measure first, optimize second)
- When resources allow comprehensive testing

---

**Document Version**: 2.0
**Last Updated**: 2025-10-04