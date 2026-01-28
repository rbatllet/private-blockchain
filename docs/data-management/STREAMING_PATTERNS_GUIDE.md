# Streaming APIs and Patterns Guide (Phase A.5+)

**Memory-Efficient Streaming Operations (Constant ~50MB Memory)**

The blockchain uses specialized streaming methods that maintain constant memory usage regardless of blockchain size. These are the recommended approaches for large-scale operations.

## 1. Batch Processing Pattern (Best for Most Use Cases)

```java
// Process entire blockchain in 1000-block batches without memory accumulation
// Memory usage: ~50MB constant, regardless of blockchain size
blockchain.processChainInBatches(batch -> {
    for (Block block : batch) {
        // Process individual block
        logger.info("Processing block {}", block.getBlockNumber());

        // Perform operations (search, validation, analysis)
        if (block.getData().contains("keyword")) {
            handleMatch(block);
        }
    }
}, MemorySafetyConstants.DEFAULT_BATCH_SIZE); // Batch size of 1000 blocks
```

**Advantages:**
- ✅ Constant memory ~50MB (verified in tests)
- ✅ Automatic pagination and batching
- ✅ No manual offset/limit tracking needed
- ✅ Perfect for blockchain validation, analysis, export
- ✅ Works with blockchains of any size (100, 100K, 1M+ blocks)

**Use Cases:**
- Chain validation: `blockchain.processChainInBatches(batch -> validateBatch(batch), 1000)`
- Analytics: `blockchain.processChainInBatches(batch -> analyzeEncryption(batch), 1000)`
- Exports: `blockchain.processChainInBatches(batch -> addToExport(batch), 1000)`
- Cleanup: `blockchain.processChainInBatches(batch -> cleanOffChainFiles(batch), 1000)`

## 2. Streaming Validation Pattern (Unlimited Size, with Callback)

```java
// Validate blockchain of ANY size with per-block callback
// No hard limit, processes one block at a time
blockchain.validateChainStreaming(block -> {
    // Invoked for each block as validation progresses
    if (isBlockInvalid(block)) {
        logger.error("Invalid block found: {}", block.getBlockNumber());
        recordError(block);
    }

    // Report progress at regular intervals
    if (block.getBlockNumber() % 5000 == 0) {
        logger.info("Validated {} blocks...", block.getBlockNumber());
    }
}, MemorySafetyConstants.MAX_JSON_METADATA_ITERATIONS); // 100 = unlimited (ignored)
```

**Advantages:**
- ✅ No block count limit (per-block processing)
- ✅ Direct access to blocks without pagination math
- ✅ Perfect for progressive validation with reporting
- ✅ Memory: ~1-2 blocks at a time
- ✅ Best for blockchains > 500K blocks

**Use Cases:**
- Progressive validation: `blockchain.validateChainStreaming(callback, maxResults)`
- Monitoring: `blockchain.validateChainStreaming(block -> updateMetrics(block), 100)`
- Recovery: `blockchain.validateChainStreaming(block -> attemptRepair(block), 100)`

## 3. Paginated Search Pattern (When You Need Result Sets)

```java
// Use for searches that need to return results
// Combine with memory limits for safety
long offset = 0;
int limit = MemorySafetyConstants.DEFAULT_BATCH_SIZE; // Per-page batch size (1000)
List<Block> allResults = new ArrayList<>();
List<Block> batch;

while ((batch = blockchain.getBlocksPaginated(offset, limit)).size() > 0) {
    // Process each page of results
    for (Block block : batch) {
        if (matchesCriteria(block)) {
            allResults.add(block);
        }
    }

    // Safety check: Cap accumulated results at 10K
    if (allResults.size() > MemorySafetyConstants.DEFAULT_MAX_SEARCH_RESULTS) {
        logger.warn("Search results capped at 10K (limit reached)");
        break;
    }

    offset += limit;
}

return allResults;
```

**Advantages:**
- ✅ Explicit pagination control
- ✅ Clear when to stop (batch.isEmpty())
- ✅ Can accumulate results up to memory limit
- ✅ Perfect for searches returning < 10K results

**Use Cases:**
- Content search: Limited results (< 1K pages = 1M blocks)
- Category searches: Typically return few matching blocks
- User-initiated queries: Users expect bounded results

## Comparison Table

| Operation | Method | Memory | Max Size | Best For |
|-----------|--------|--------|----------|----------|
| Chain validation | `processChainInBatches()` | ~50MB | Unlimited | General validation |
| Unlimited validation | `validateChainStreaming()` | ~1-2MB | Unlimited | Very large chains |
| Search with results | `getBlocksPaginated()` | Accumulates | Limited | Returning matches |
| Search metadata | `searchByCustomMetadataKeyValuePaginated()` | Bounded | 10K limit | JSON queries |

## 4. Breaking Changes: maxResults Parameter (Phase A.5+)

```java
// ❌ OLD CODE - maxResults=0 returned ALL results (REMOVED)
// This was dangerous and caused OutOfMemoryError on large chains
List<Block> results = blockchain.getBlocksBySignerPublicKey(pubKey, 0); // ❌ Now REJECTED!

// ✅ NEW CODE - maxResults must be > 0
// Option 1: Use reasonable limit (recommended)
List<Block> results = blockchain.getBlocksBySignerPublicKey(pubKey, 10000); // ✅ 10K limit

// Option 2: Use streaming for unlimited processing
blockchain.processChainInBatches(batch -> {
    for (Block block : batch) {
        if (block.getPublicKey().equals(pubKey)) {
            processBlock(block);
        }
    }
}, 1000); // ✅ No limit, constant memory

// Option 3: Use paginated search
long offset = 0;
while ((results = blockchain.getBlocksBySignerPublicKey(pubKey, 10000)).size() > 0) {
    processResults(results);
    offset += results.size();
} // ✅ Paginate through all results safely
```

### Migration Guide for Existing Code

If you have code using `maxResults ≤ 0`, update it:

```java
// Before: Dangerous unlimited search
public void analyzeAllBlocksOfUser(String username) {
    List<Block> allBlocks = blockchain.getBlocksBySignerPublicKey(pubKey, 0); // ❌ Memory bomb!
    for (Block block : allBlocks) {
        analyze(block);
    }
}

// After: Safe streaming pattern
public void analyzeAllBlocksOfUser(String username) {
    blockchain.processChainInBatches(batch -> {
        for (Block block : batch) {
            if (isFromUser(block, username)) {
                analyze(block);
            }
        }
    }, 1000); // ✅ Constant ~50MB memory
}
```

## 5. Database-Specific Optimization Notes

### PostgreSQL/MySQL (Recommended for Production)
```java
// These databases support ScrollableResults for true streaming
// The blockchain automatically detects database type and uses optimized queries
blockchain.processChainInBatches(batch -> {
    // Uses server-side pagination (forward-only cursor)
    // Memory: ~50MB regardless of chain size
    // Ideal for 100K+ block chains
}, 1000);
```

### SQLite (Development Only)
```java
// SQLite has limited cursor support, uses pagination internally
// Still maintains constant memory through HibernateScrollableResults fallback
// Single-writer limitation means avoid concurrent processChainInBatches() calls
blockchain.processChainInBatches(batch -> {
    // Internally paginated, still constant memory
}, 1000);
```

### H2 (Testing)
```java
// H2 supports scrollable results and large batch operations
// In PostgreSQL-compatible mode, uses optimized queries
DatabaseConfig h2Config = DatabaseConfig.createH2TestConfig();
JPAUtil.initialize(h2Config);
// Now uses same optimizations as PostgreSQL
```

## 6. Search API Streaming Variants

### Memory-Safe Search Methods with Limits

```java
// SearchFrameworkEngine - Best for exhaustive search
SearchFrameworkEngine engine = new SearchFrameworkEngine(blockchain);

// Exhaustive search with memory safety
List<Block> results = engine.searchExhaustiveOffChain("keyword", 10000);
// Returns max 10K results, uses processChainInBatches() internally

// UserFriendlyEncryptionAPI - High-level API (v1.0.6+ secure pattern)
// After secure initialization with genesis admin:
KeyPair userKeys = api.createUser("username");
api.setDefaultCredentials("username", userKeys);

// Multiple search levels with batching
List<Block> results = api.findBlocksByCategory("financial", 5000);
// Uses processChainInBatches() with 10K limit

// Metadata search with streaming
api.findBlocksByMetadata("confidential", 10000);
// Paginated, bounded to 10K
```

### Correct Search Pattern

```java
// ✅ GOOD - All search methods now enforce maxResults validation
List<Block> results = blockchain.searchByCategory("URGENT", 5000); // ✅ Works

// ❌ BAD - Negative or zero maxResults rejected
List<Block> results = blockchain.searchByCategory("URGENT", -1); // ❌ IllegalArgumentException
List<Block> results = blockchain.searchByCategory("URGENT", 0);  // ❌ IllegalArgumentException

// ✅ GOOD - Use streaming when you need all results
blockchain.processChainInBatches(batch -> {
    List<Block> matching = batch.stream()
        .filter(b -> b.getCategory().equals("URGENT"))
        .collect(Collectors.toList());
    processMatches(matching);
}, 1000); // ✅ Works with any chain size
```

## 7. Phase B.2 Streaming Alternatives (v1.0.6+)

Four specialized streaming methods added for common use cases:

### 7.1. streamBlocksByTimeRange() - Temporal Queries

```java
// Stream blocks within time range (unlimited, memory-safe)
try (Stream<Block> stream = blockchain.streamBlocksByTimeRange(
    LocalDateTime.of(2024, 1, 1, 0, 0),
    LocalDateTime.of(2024, 12, 31, 23, 59))) {
    stream.forEach(block -> {
        // Process blocks from 2024
        analyzeTemporalTrends(block);
    });
}
```

**Use cases:**
- Temporal audits and compliance reporting
- Time-based analytics and trend analysis
- Historical data processing
- Period-specific validation

**Performance:**
- Optimized with composite index: (timestamp, block_number)
- PostgreSQL/MySQL/H2: getResultStream() (⚡ 12-15s for 1M blocks)
- SQLite: Pagination (✅ 20-25s for 1M blocks)
- Memory: ~50MB constant

### 7.2. streamEncryptedBlocks() - Encryption Operations

```java
// Stream all encrypted blocks only (unlimited, memory-safe)
blockchain.streamEncryptedBlocks(block -> {
    // Process only encrypted blocks
    if (needsKeyRotation(block)) {
        rotateEncryptionKey(block, newPassword);
    }
});
```

**Use cases:**
- Mass re-encryption with new keys
- Encryption audits and compliance checks
- Key rotation operations across millions of blocks
- Encryption analytics and reporting

**Performance:**
- Filters at database level: `WHERE b.isEncrypted = true`
- Same performance as processChainInBatches() but with filtering
- Memory: ~50MB constant

### 7.3. streamBlocksWithOffChainData() - Off-Chain Management

```java
// Stream blocks with off-chain storage (unlimited, memory-safe)
blockchain.streamBlocksWithOffChainData(block -> {
    // Verify off-chain file integrity
    String offChainPath = block.getOffChainData();
    verifyOffChainFile(offChainPath);
});
```

**Use cases:**
- Off-chain data verification and integrity checks
- Storage migration (move off-chain data to new location)
- Off-chain cleanup and maintenance
- Storage analytics and capacity planning

**Performance:**
- Filters at database level: `WHERE b.offChainData IS NOT NULL`
- Efficient for sparse off-chain data
- Memory: ~50MB constant

### 7.4. streamBlocksAfter() - Incremental Processing

```java
// Stream blocks after specific block number (unlimited, memory-safe)
blockchain.streamBlocksAfter(500_000L, block -> {
    // Process blocks after #500,000
    if (needsRollback(block)) {
        markForDeletion(block);
    }
});
```

**Use cases:**
- Large rollbacks (>100K blocks) without memory issues
- Incremental processing of new blocks
- Chain recovery and synchronization
- Differential backups and exports

**Performance:**
- Efficient range query: Single index scan on blockNumber
- Perfect for large rollbacks (>100K blocks)
- Supports blockchains > 2.1B blocks (uses `long`)
- Memory: ~50MB constant

## Performance Summary - Phase B.2 Methods

| Database | Implementation | 1M Blocks | Memory |
|----------|----------------|-----------|---------|
| PostgreSQL | ScrollableResults | 12-15s | ~50MB |
| MySQL | ScrollableResults | 15-20s | ~50MB |
| H2 | ScrollableResults | 10-12s | ~50MB |
| SQLite | Pagination | 20-25s | ~50MB |

## When to Use Phase B.2 Methods

| Method | Use When | Don't Use When |
|--------|----------|----------------|
| `streamBlocksByTimeRange()` | Need temporal filtering | Need full chain scan |
| `streamEncryptedBlocks()` | Only need encrypted blocks | Need mixed results |
| `streamBlocksWithOffChainData()` | Only need off-chain blocks | Need on-chain blocks |
| `streamBlocksAfter()` | Need incremental processing | Need random access |

All 4 methods are thread-safe, use database-specific optimization, and maintain constant memory usage regardless of blockchain size.
