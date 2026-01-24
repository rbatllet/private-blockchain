# Memory-Efficient Blockchain Access Guide

> **🔄 CODE UPDATE (v1.0.6+)**: Methods like `rollbackToBlock()` now throw exceptions instead of returning `false`. Wrap calls in try-catch blocks. See [Exception-Based Error Handling Guide](../security/EXCEPTION_BASED_ERROR_HANDLING_V1_0_6.md).

## Critical Rule: NEVER Load All Blocks Into Memory

**⚠️ NEVER load all blocks into memory at once!**

The blockchain can grow to millions of blocks. Always use pagination or batch processing.

## Quick Reference

```java
// ❌ BAD - Memory bomb with large chains
List<Block> allBlocks = blockchain.getAllBlocks();  // REMOVED - No longer exists!

// ✅ GOOD - Paginated access
long offset = 0;
int limit = 1000;
List<Block> batch = blockchain.getBlocksPaginated(offset, limit);

// ✅ BEST - Batch processing with automatic pagination
blockchain.processChainInBatches(batch -> {
    // Process each batch of 1000 blocks
    for (Block block : batch) {
        // Your processing logic here
    }
}, 1000);
```

## Key Methods

### Core Pagination Methods
- `getBlocksPaginated(offset, limit)` - Get specific page of blocks
- `processChainInBatches(Consumer<List<Block>>, batchSize)` - Stream processing with constant memory
- `validateChainStreaming(Consumer<Block>, maxResults)` - Streaming validation (unlimited size)
- `getBlockCount()` - Total blocks for pagination calculation
- `getBlockByNumber(long)` - Fetch individual block efficiently

### Refactored Memory-Safe Methods
- ✅ `validateChainDetailed()` - Validates in 1000-block batches
- ✅ `verifyAllOffChainIntegrity()` - Processes off-chain data in batches
- ✅ `ChainRecoveryManager` - Uses batch processing for recovery operations
- ✅ `rollbackToBlock()` - Processes rollback with paginated off-chain cleanup

## BlockRepository Memory-Efficient Search Methods

- ✅ `getBlocksAfterPaginated(blockNumber, offset, limit)` - Rollback/cleanup operations
- ✅ `getBlocksByTimeRangePaginated(start, end, offset, limit)` - Temporal queries
- ✅ `searchBlocksByContentWithLimit(content, maxResults)` - Content search with limit
- ✅ `searchBlocksByContentWithLevel(searchTerm, level)` / `searchBlocksByContentWithLevel(searchTerm, level, maxResults)` - Multi-level search (default 10K)
- ✅ `getBlocksBySignerPublicKeyWithLimit(publicKey, maxResults)` - Key-based queries (0 = unlimited)
- ✅ `searchByCategoryWithLimit(category, maxResults)` - Category filtering
- ✅ `searchByCustomMetadata(searchTerm)` / `searchByCustomMetadataWithLimit(searchTerm, maxResults)` - General metadata search (default 10K)
- ✅ `searchByCustomMetadataKeyValuePaginated(key, value, offset, limit)` - JSON metadata search
- ✅ `searchByCustomMetadataMultipleCriteriaPaginated(criteria, offset, limit)` - Complex JSON queries

## Blockchain Public API (with configurable limits)

- ✅ `searchByCategory(category)` / `searchByCategory(category, maxResults)` - Default 10K limit, customizable
- ✅ `getBlocksBySignerPublicKey(publicKey)` / `getBlocksBySignerPublicKey(publicKey, maxResults)` - Default 10K, customizable (⚠️ maxResults=0 is unlimited and memory-unsafe!)

## Batch Methods with Size Validation

**⚠️ Maximum batch size: 10K items**

- ✅ `batchRetrieveBlocks(List<Long>)` - Max 10K block numbers, throws IllegalArgumentException if exceeded
- ✅ `batchRetrieveBlocksByHash(List<String>)` - Max 10K hashes, throws IllegalArgumentException if exceeded

## Search Engines - Memory-Safe

All search engines have been refactored to use memory-safe patterns:

- ✅ `SearchFrameworkEngine.searchExhaustiveOffChain()` - Uses processChainInBatches() instead of loading all blocks
- ✅ `SearchFrameworkEngine.indexBlockchain()` - Uses processChainInBatches() instead of accumulating all blocks
- ✅ `SearchSpecialistAPI.*` - All methods have maxResults parameters with defaults (20-50)
- ✅ `UserFriendlyEncryptionAPI.findBlocksByMetadata()` - Limits to 10K before batch retrieve
- ✅ `UserFriendlyEncryptionAPI.findBlocksByCategory()` - Uses processChainInBatches() with 10K limit
- ✅ `UserFriendlyEncryptionAPI.findBlocksByUser()` - CREATED_BY/ENCRYPTED_FOR use native O(1) queries, ACCESSIBLE uses processChainInBatches() with 10K limit (P0 optimization)
- ✅ `UserFriendlyEncryptionAPI.analyzeEncryption()` - Uses processChainInBatches() for analysis
- ✅ `UserFriendlyEncryptionAPI.getEncryptedBlocksOnlyLinear()` - Uses processChainInBatches() with 10K limit
- ✅ `UserFriendlyEncryptionAPI.repairBrokenChain()` - Processes chain repair in 1000-block batches with progress reporting

## Core Blockchain Operations - Memory-Safe

- ✅ `Blockchain.exportChain()` - Validates chain size (max 500K blocks), warns at 100K+ blocks
- ✅ `Blockchain.exportEncryptedChain()` - Validates chain size (max 500K blocks), warns at 100K+ blocks
- ✅ `Blockchain.rollbackBlocks()` - Processes rollback in 1000-block batches without loading all blocks into memory
- ✅ `Blockchain.validateChainDetailed()` - Validates chain size (max 500K blocks), throws exception >500K blocks
- ✅ `Blockchain.validateChainStreaming()` - **NEW!** Streaming validation for unlimited blockchain size, processes in batches with consumer callback

## Removed Memory-Inefficient Methods

**⚠️ Breaking Change:** All methods that loaded entire result sets into memory have been removed. Use the paginated/limited versions above instead.

## Memory Safety Configuration

All memory limits are centralized in `MemorySafetyConstants`:

```java
MAX_BATCH_SIZE = 10000                    // Maximum items for batch operations
DEFAULT_MAX_SEARCH_RESULTS = 10000        // Default search result limit
SAFE_EXPORT_LIMIT = 100000                // Warning threshold for exports
MAX_EXPORT_LIMIT = 500000                 // Hard limit for exports (prevents OutOfMemoryError)
LARGE_ROLLBACK_THRESHOLD = 100000         // Warning threshold for rollbacks
DEFAULT_BATCH_SIZE = 1000                 // Default batch size for streaming operations
PROGRESS_REPORT_INTERVAL = 5000           // Progress logging interval
```

## See Also

- [Streaming Patterns Guide](../data-management/STREAMING_PATTERNS_GUIDE.md) - Detailed streaming API patterns
- [API Guide](../reference/API_GUIDE.md) - Complete API reference
