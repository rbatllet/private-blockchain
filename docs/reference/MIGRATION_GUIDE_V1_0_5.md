# Migration Guide: v1.0.6+ Breaking Changes (Memory Safety Refactoring - Phase A.5)

## Overview

Version 1.0.5 introduces critical memory safety improvements to prevent OutOfMemoryError on large blockchain instances. This document guides you through the breaking changes and provides migration strategies.

**Status:** ✅ All breaking changes documented and tested (Phase A.5 complete)

## Breaking Changes Summary

| Change | Severity | Impact | Migration |
|--------|----------|--------|-----------|
| `maxResults ≤ 0` now rejected | **HIGH** | Search methods throw `IllegalArgumentException` | Use streaming or specific limit |
| Default batch size is 1000 | **MEDIUM** | Batch operations use smaller batches | Adjust if needed for performance |
| Iteration limit = 100 | **MEDIUM** | Search capped at 100,000 results | Use streaming for larger sets |
| Memory limits enforced | **HIGH** | Export/rollback limited to 500K blocks | Use batch operations |

---

## 1. maxResults Parameter Breaking Changes

### 🔴 CRITICAL: maxResults Must Be > 0

**What Changed:**
- Before: `maxResults = 0` or negative returned **ALL results** (dangerous, caused OutOfMemoryError)
- After: `maxResults ≤ 0` **throws `IllegalArgumentException`** immediately

**Why:**
```java
// OLD CODE (BROKEN):
List<Block> results = blockchain.getBlocksBySignerPublicKey(pubKey, 0);
// Problem: On 1M block chain, tries to load 1M blocks into memory → OutOfMemoryError!

// NEW CODE (SAFE):
List<Block> results = blockchain.getBlocksBySignerPublicKey(pubKey, 10000);
// Maximum 10K blocks returned, no memory explosion
```

### Affected Methods

**Direct maxResults parameters:**
- `searchByCategory(String category, int maxResults)`
- `getBlocksBySignerPublicKey(String publicKey, int maxResults)`
- `searchByCustomMetadataWithLimit(String searchTerm, int maxResults)`
- `searchByContentWithLimit(String content, int maxResults)`
- `searchByCategoryWithLimit(String category, int maxResults)`
- `validateChainStreaming(Consumer<Block>, int maxResults)`
- All `SearchFrameworkEngine` search methods
- All `UserFriendlyEncryptionAPI` search methods
- `SearchSpecialistAPI` methods

### Migration Path 1: Use Reasonable Limit (Recommended for Most Cases)

**Problem Code:**
```java
// ❌ BEFORE: Dangerous unbounded search
public void analyzeAllUserBlocks(String username) {
    List<Block> allBlocks = blockchain.getBlocksBySignerPublicKey(pubKey, 0);

    for (Block block : allBlocks) {
        analyzeBlock(block);
    }

    logger.info("Analyzed {} blocks", allBlocks.size());
}
```

**Solution Code (Using maxResults limit):**
```java
// ✅ AFTER: Safe with reasonable limit
public void analyzeAllUserBlocks(String username) {
    List<Block> allBlocks = blockchain.getBlocksBySignerPublicKey(pubKey, 10000);

    for (Block block : allBlocks) {
        analyzeBlock(block);
    }

    logger.info("Analyzed {} blocks", allBlocks.size());
}
// Safe: Maximum 10K blocks in memory at once
```

**When to use:**
- You expect < 10K matching blocks
- Results fit in memory
- Simple one-time queries
- API limits are acceptable (pagination is manual if you need more)

### Migration Path 2: Use Streaming Pattern (Recommended for Large Datasets)

**Problem Code:**
```java
// ❌ BEFORE: Memory bomb on large chains
public void processAllBlocks() {
    List<Block> allBlocks = blockchain.getAllBlocks(); // REMOVED!

    for (Block block : allBlocks) {
        processBlock(block);
    }
}
```

**Solution Code (Using batch processing):**
```java
// ✅ AFTER: Constant memory regardless of chain size
public void processAllBlocks() {
    blockchain.processChainInBatches(batch -> {
        for (Block block : batch) {
            processBlock(block);
        }
    }, 1000); // Processes 1000 blocks at a time, constant ~50MB memory
}
// Safe: Works with 100 blocks, 100K blocks, or 1M+ blocks
```

**When to use:**
- Processing entire blockchain (no filtering)
- Analyzing large result sets
- Validation and integrity checks
- Chain maintenance operations
- Avoid manual pagination math

**Code Examples:**
```java
// Example 1: Find and process all blocks from specific user
blockchain.processChainInBatches(batch -> {
    List<Block> userBlocks = batch.stream()
        .filter(b -> isFromUser(b, username))
        .collect(Collectors.toList());

    userBlocks.forEach(this::analyzeBlock);
}, 1000);

// Example 2: Validate all blocks with progress reporting
blockchain.processChainInBatches(batch -> {
    for (Block block : batch) {
        if (!validateBlock(block)) {
            recordValidationError(block);
        }

        if (block.getBlockNumber() % 5000 == 0) {
            logger.info("Validated blocks up to {}", block.getBlockNumber());
        }
    }
}, 1000);

// Example 3: Export blocks with memory safety
List<String> exportedHashes = new ArrayList<>();
blockchain.processChainInBatches(batch -> {
    batch.forEach(block -> exportedHashes.add(block.getHash()));
}, 1000);
logger.info("Exported {} block hashes", exportedHashes.size());
```

### Migration Path 3: Use Paginated Search (Manual Iteration)

**Problem Code:**
```java
// ❌ BEFORE: Accumulates unlimited results
public List<Block> searchBlocksByCategory(String category) {
    return blockchain.searchByCategory(category, 0); // ❌ Now REJECTED!
}
```

**Solution Code (Using pagination loop):**
```java
// ✅ AFTER: Safe pagination with limit
public List<Block> searchBlocksByCategory(String category) {
    List<Block> allResults = new ArrayList<>();
    long offset = 0L;
    int limit = 1000;
    List<Block> batch;

    while ((batch = blockchain.getBlocksPaginated(offset, limit)).size() > 0) {
        // Filter by category
        List<Block> matching = batch.stream()
            .filter(b -> category.equals(b.getCategory()))
            .collect(Collectors.toList());

        allResults.addAll(matching);

        // Safety: Stop if too many results
        if (allResults.size() > MemorySafetyConstants.DEFAULT_MAX_SEARCH_RESULTS) {
            logger.warn("Search capped at {} results (limit exceeded)",
                allResults.size());
            break;
        }

        offset += limit;
    }

    return allResults;
}
```

**When to use:**
- Granular control over pagination
- Need to accumulate results (but limited)
- Implementing custom search logic
- Server-side filtering per page

### Detection and Error Handling

**Catching the new exception:**
```java
try {
    List<Block> results = blockchain.searchByCategory("URGENT", 0);
} catch (IllegalArgumentException e) {
    logger.error("Invalid maxResults parameter: {}", e.getMessage());
    // Handle error: suggest using streaming or reasonable limit
    // Example response: "Use processChainInBatches() for unlimited results"
}
```

**Validation before calling:**
```java
public List<Block> safeSearch(String category, int maxResults) {
    // Validate before calling blockchain method
    if (maxResults <= 0) {
        throw new IllegalArgumentException(
            "maxResults must be > 0. Use processChainInBatches() for unlimited results.");
    }

    return blockchain.searchByCategory(category, maxResults);
}
```

---

## 2. Batch Operations Default Size: 1000 blocks

### 🟡 What Changed

- Default batch size for `processChainInBatches()` is now **1000 blocks** (configurable)
- Smaller batches = less memory per iteration, safer defaults
- Can be customized per call if needed

### Before and After

```java
// ❌ OLD: Unclear batch size
blockchain.processChainInBatches(batch -> {
    // Process batch of unknown size?
    processBatch(batch);
});

// ✅ NEW: Explicit batch size
blockchain.processChainInBatches(batch -> {
    // Process batch of 1000 blocks
    processBatch(batch);
}, MemorySafetyConstants.DEFAULT_BATCH_SIZE); // Batch size is explicit

// ✅ CUSTOM: Can customize if needed
blockchain.processChainInBatches(batch -> {
    // Process batch of 5000 blocks for better throughput
    processBatch(batch);
}, 5000); // Custom batch size
```

### Migration Notes

**No code changes needed** if you're using default batch size of 1000.

**If you need different batch size:**
```java
// Before: Relying on default
blockchain.processChainInBatches(batch -> { ... });

// After: Specify batch size
blockchain.processChainInBatches(batch -> { ... },
    5000); // Or whatever size you need (max 10K)
```

**Performance tuning:**
- Larger batches (5K-10K): Better throughput, more memory
- Smaller batches (100-1K): Less memory, more round-trips
- Default 1000: Good balance for most use cases

---

## 3. JSON Metadata Search Iteration Limit: 100

### 🟡 What Changed

- `MAX_JSON_METADATA_ITERATIONS = 100` (new constant)
- Metadata searches limited to 100 iterations
- Each iteration processes ~1000 blocks
- **Total limit: ~100,000 blocks per search**

### Affected Methods

- `searchByCustomMetadata(String searchTerm)` → Limited to 100K blocks
- `searchByCustomMetadataWithLimit(String searchTerm, int maxResults)`
- `searchByCustomMetadataKeyValuePaginated(String key, String value, long offset, int limit)`
- `searchByCustomMetadataMultipleCriteriaPaginated(Map<String, String> criteria, long offset, int limit)`

### Migration

```java
// ❌ OLD: Would try to search entire chain (unlimited)
List<Block> results = blockchain.searchByCustomMetadata("budget");

// ✅ NEW: Limited to 100K blocks automatically
List<Block> results = blockchain.searchByCustomMetadata("budget");
// Returns up to 100K matching blocks from first 100 iterations

// ✅ ALTERNATIVE: Use paginated search for larger datasets
public void searchLargeMetadataSet(String key, String value) {
    long offset = 0L;
    int limit = 1000;
    List<Block> batch;

    while ((batch = blockchain.searchByCustomMetadataKeyValuePaginated(
            key, value, offset, limit)).size() > 0) {

        processBatch(batch);
        offset += batch.size();

        // No automatic limit, but you control when to stop
        if (offset > 500000) {
            logger.warn("Search stopped at 500K blocks");
            break;
        }
    }
}
```

**When to use:**
- Default search: Fine for most use cases (100K limit is generous)
- Custom limit: Use `searchByCustomMetadataWithLimit(term, maxResults)`
- Unlimited: Use paginated variant with manual loop

---

## 4. Export and Rollback Size Limits

### 🟡 What Changed

- `MAX_EXPORT_LIMIT = 500,000` - Hard limit on export size
- `SAFE_EXPORT_LIMIT = 100,000` - Warning threshold (logs warning)
- `LARGE_ROLLBACK_THRESHOLD = 100,000` - Warning threshold for rollback

### Affected Methods

- `exportChain()` → Max 500K blocks, warns at 100K+
- `exportEncryptedChain()` → Max 500K blocks, warns at 100K+
- `rollbackChain()` → Warns at 100K+ blocks affected

### Migration

```java
// ❌ BEFORE: No limits (could cause OutOfMemoryError on 1M+ blockchains)
String exported = blockchain.exportChain();

// ✅ AFTER: Automatic size validation
String exported = blockchain.exportChain();
// If chain > 500K blocks: throws ChainSizeException
// If chain > 100K blocks: logs WARNING

// ✅ SAFE PATTERN: Check size first
public String safeExportChain() {
    long blockCount = blockchain.getBlockCount();

    if (blockCount > 500000) {
        logger.error("Chain too large to export ({}M blocks). " +
            "Use batch export instead.", blockCount / 1_000_000);
        throw new IllegalStateException("Chain too large to export");
    }

    if (blockCount > 100000) {
        logger.warn("Exporting large chain ({} blocks). " +
            "This may take time and use significant memory.", blockCount);
    }

    return blockchain.exportChain();
}

// ✅ BATCH EXPORT: For very large chains
public void batchExportChain(Consumer<String> pageConsumer) {
    long offset = 0L;
    int pageSize = 10000;
    List<Block> batch;

    while ((batch = blockchain.getBlocksPaginated(offset, pageSize)).size() > 0) {
        String pageJson = serializeBlocks(batch);
        pageConsumer.accept(pageJson); // Save/process page
        offset += pageSize;
    }
}
```

---

## 5. Complete Migration Examples

### Example 1: User Activity Analysis (Large Dataset)

**OLD CODE (Broken):**
```java
public Map<String, Long> analyzeUserActivity() {
    // ❌ BROKEN: Tries to load ALL blocks
    List<Block> allBlocks = blockchain.getAllBlocks(); // REMOVED!

    Map<String, Long> blocksByUser = new HashMap<>();
    for (Block block : allBlocks) {
        String signer = block.getSignerPublicKey();
        blocksByUser.put(signer, blocksByUser.getOrDefault(signer, 0L) + 1);
    }

    return blocksByUser;
}
```

**NEW CODE (Safe):**
```java
public Map<String, Long> analyzeUserActivity() {
    // ✅ FIXED: Uses streaming with constant memory
    Map<String, Long> blocksByUser = new ConcurrentHashMap<>();

    blockchain.processChainInBatches(batch -> {
        for (Block block : batch) {
            String signer = block.getSignerPublicKey();
            blocksByUser.merge(signer, 1L, Long::sum);
        }
    }, 1000); // Process 1000 blocks at a time

    return blocksByUser;
}
```

### Example 2: Search with Results Accumulation

**OLD CODE (Broken):**
```java
public List<Block> findAllBlocksOfUser(String username) {
    // ❌ BROKEN: Unlimited search
    return blockchain.getBlocksBySignerPublicKey(userPublicKey, 0);
}
```

**NEW CODE (Safe - Pick One):**

**Option A: Reasonable limit (if < 10K blocks expected):**
```java
public List<Block> findAllBlocksOfUser(String username) {
    // ✅ Limit to 10K blocks (safe default)
    return blockchain.getBlocksBySignerPublicKey(userPublicKey, 10000);
}
```

**Option B: Streaming (for any size):**
```java
public void processAllBlocksOfUser(String username, Consumer<Block> processor) {
    // ✅ Stream all blocks with callback (constant memory)
    blockchain.processChainInBatches(batch -> {
        batch.stream()
            .filter(b -> isFromUser(b, username))
            .forEach(processor);
    }, 1000);
}

// Usage:
processAllBlocksOfUser(username, block -> {
    updateAnalytics(block);
});
```

**Option C: Paginated with accumulation:**
```java
public List<Block> findAllBlocksOfUser(String username, int maxAccumulate) {
    // ✅ Paginated search with limit
    List<Block> results = new ArrayList<>();
    long offset = 0L;
    int limit = 1000;
    List<Block> batch;

    while ((batch = blockchain.getBlocksPaginated(offset, limit)).size() > 0) {
        List<Block> matching = batch.stream()
            .filter(b -> isFromUser(b, username))
            .collect(Collectors.toList());

        results.addAll(matching);
        if (results.size() >= maxAccumulate) {
            logger.warn("Results capped at {} (limit reached)", maxAccumulate);
            break;
        }

        offset += limit;
    }

    return results;
}
```

### Example 3: Chain Validation with Progress Reporting

**OLD CODE (Broken):**
```java
public boolean validateChain() {
    // ❌ BROKEN: Loads all blocks into memory
    List<Block> allBlocks = blockchain.getAllBlocks(); // REMOVED!

    for (Block block : allBlocks) {
        if (!validateSingleBlock(block)) {
            return false;
        }
    }

    return true;
}
```

**NEW CODE (Safe):**
```java
public boolean validateChain() {
    // ✅ FIXED: Streaming validation with progress reporting
    AtomicBoolean isValid = new AtomicBoolean(true);
    AtomicLong processedCount = new AtomicLong(0);

    blockchain.processChainInBatches(batch -> {
        for (Block block : batch) {
            if (!validateSingleBlock(block)) {
                isValid.set(false);
            }

            long count = processedCount.incrementAndGet();
            if (count % 10000 == 0) {
                logger.info("Validated {} blocks...", count);
            }
        }
    }, 1000); // Process 1000 blocks at a time

    logger.info("Validation complete: {}",
        isValid.get() ? "ALL VALID" : "ERRORS FOUND");

    return isValid.get();
}
```

---

## 6. Testing Migration

### Unit Tests: Mock maxResults Validation

```java
@Test
void testMaxResultsValidation() {
    // New behavior: maxResults ≤ 0 throws exception
    assertThrows(IllegalArgumentException.class, () ->
        blockchain.searchByCategory("TEST", 0));

    assertThrows(IllegalArgumentException.class, () ->
        blockchain.searchByCategory("TEST", -1));

    // Valid usage
    assertDoesNotThrow(() ->
        blockchain.searchByCategory("TEST", 100));
}
```

### Integration Tests: Memory Safety

```java
@Test
void testStreamingMemorySafety() throws Exception {
    // Add 50K blocks
    for (int i = 0; i < 50000; i++) {
        blockchain.addBlock("Data " + i, keyPair.getPrivate(), keyPair.getPublic());
    }

    // Measure memory during streaming
    long memBefore = getMemoryUsage();

    blockchain.processChainInBatches(batch -> {
        // Process 50K blocks
        for (Block b : batch) {
            validateBlock(b);
        }
    }, 1000);

    long memAfter = getMemoryUsage();
    long memDelta = memAfter - memBefore;

    // Should use < 100MB (constant, not 50K blocks)
    assertThat(memDelta).isLessThan(100_000_000);
}
```

---

## 7. FAQ and Troubleshooting

### Q: "I got `IllegalArgumentException: maxResults must be > 0`"

**A:** You're passing `maxResults ≤ 0`. Use one of these patterns:

1. **Use limit:** `searchByCategory("X", 10000)`
2. **Use streaming:** `blockchain.processChainInBatches(..., 1000)`
3. **Use pagination:** Manual loop with `getBlocksPaginated()`

### Q: "How do I process ALL blocks efficiently?"

**A:** Use `processChainInBatches()`:

```java
blockchain.processChainInBatches(batch -> {
    // Your logic here
}, 1000);
```

This works regardless of chain size (100 or 1M+ blocks).

### Q: "Can I increase batch size for better performance?"

**A:** Yes, adjust the batch size parameter:

```java
blockchain.processChainInBatches(batch -> {
    // Process larger batches
}, 10000); // 10K blocks per batch (max is MAX_BATCH_SIZE)
```

Trade-off: Larger batches = more memory, better throughput

### Q: "My export fails with ChainSizeException"

**A:** Your chain is > 500K blocks. Use batch export:

```java
long offset = 0;
while ((batch = blockchain.getBlocksPaginated(offset, 10000)).size() > 0) {
    String pageJson = exportBatch(batch);
    savePage(pageJson);
    offset += 10000;
}
```

### Q: "How do I check if maxResults is valid before calling?"

**A:** Add validation:

```java
public static void validateMaxResults(int maxResults) {
    if (maxResults <= 0) {
        throw new IllegalArgumentException(
            "maxResults must be > 0. " +
            "Use processChainInBatches() for unlimited results.");
    }
    if (maxResults > MemorySafetyConstants.MAX_BATCH_SIZE) {
        throw new IllegalArgumentException(
            "maxResults exceeded maximum (" +
            MemorySafetyConstants.MAX_BATCH_SIZE + ")");
    }
}
```

---

## 8. Summary: Key Takeaways

| Old Way | New Way | Use Case |
|---------|---------|----------|
| `getBlocksBySignerPublicKey(key, 0)` | `processChainInBatches()` | Process all blocks |
| `getAllBlocks()` | `processChainInBatches()` | Iterate all blocks |
| `searchByCategory(cat, 0)` | `searchByCategory(cat, 10000)` | Limited results |
| Manual pagination | `processChainInBatches()` | Batch operations |
| Collect to List | `Consumer<List<Block>>` | Streaming processing |

**Memory Safety Guarantees:**
- ✅ `processChainInBatches()`: ~50MB constant (verified)
- ✅ Search methods: Limited to 100K results (iteration limit)
- ✅ Exports: Limited to 500K blocks (hard limit)
- ✅ All streaming: Per-block or per-batch processing

**Next Steps:**
1. Review your code for `maxResults ≤ 0` calls
2. Replace with appropriate streaming or limited-result pattern
3. Test with large blockchain instances (10K+ blocks)
4. Monitor memory usage: Should stay ~50MB during batch operations

---

## References

- **API_GUIDE.md**: Memory Safety & Streaming APIs (v1.0.6+)
- **TESTING.md**: Memory Safety Testing Patterns (v1.0.6+)
- **MemorySafetyConstants.java**: Configuration constants
- **Phase_A5_PostgreSQL_OptimizationsTest.java**: Practical examples
