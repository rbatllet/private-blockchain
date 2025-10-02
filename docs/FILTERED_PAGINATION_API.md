# Filtered Pagination API

**Version 2.1.0** introduces specialized pagination methods for filtering specific block types, enabling memory-efficient retrieval of encrypted blocks and blocks with off-chain data.

These methods are available in both **`BlockDAO`** (low-level DAO layer) and **`Blockchain`** (high-level API with global locking).

## Available Methods

### 1. Get Blocks with Off-Chain Data (Paginated)

```java
// High-level API (Recommended - includes global blockchain lock)
Blockchain blockchain = new Blockchain();
List<Block> blocks = blockchain.getBlocksWithOffChainDataPaginated(offset, limit);

// Low-level DAO (Direct database access)
BlockDAO blockDAO = new BlockDAO();
List<Block> blocks = blockDAO.getBlocksWithOffChainDataPaginated(offset, limit);
```

**Method Signature:**
```java
/**
 * Retrieve blocks with off-chain data in paginated batches
 * Memory-efficient alternative to loading all off-chain blocks at once
 */
public List<Block> getBlocksWithOffChainDataPaginated(int offset, int limit)
```

**Use Cases:**
- Processing large files stored off-chain in batches
- Analyzing off-chain storage patterns
- Maintenance operations on off-chain data
- Generating reports on off-chain usage

**Example:**
```java
Blockchain blockchain = new Blockchain();

// Process all blocks with off-chain data in batches of 100
int batchSize = 100;
int offset = 0;
List<Block> batch;

do {
    batch = blockchain.getBlocksWithOffChainDataPaginated(offset, batchSize);

    for (Block block : batch) {
        // Process block with off-chain data
        OffChainData offChainData = block.getOffChainData();
        System.out.println("Block #" + block.getBlockNumber() +
                          " has off-chain file: " + offChainData.getFilePath());

        // Perform maintenance or analysis
        analyzeOffChainFile(offChainData);
    }

    offset += batchSize;
} while (!batch.isEmpty());
```

### 2. Get Encrypted Blocks (Paginated)

```java
// High-level API (Recommended - includes global blockchain lock)
Blockchain blockchain = new Blockchain();
List<Block> blocks = blockchain.getEncryptedBlocksPaginated(offset, limit);

// Low-level DAO (Direct database access)
BlockDAO blockDAO = new BlockDAO();
List<Block> blocks = blockDAO.getEncryptedBlocksPaginated(offset, limit);
```

**Method Signature:**
```java
/**
 * Retrieve encrypted blocks in paginated batches
 * Memory-efficient alternative to loading all encrypted blocks at once
 */
public List<Block> getEncryptedBlocksPaginated(int offset, int limit)
```

**Use Cases:**
- Auditing encrypted content across the blockchain
- Re-encryption operations with new keys
- Security compliance reporting
- Encrypted data migration

**Example:**
```java
Blockchain blockchain = new Blockchain();

// Audit all encrypted blocks in batches of 50
int batchSize = 50;
int offset = 0;
int totalEncrypted = 0;
List<Block> batch;

do {
    batch = blockchain.getEncryptedBlocksPaginated(offset, batchSize);
    totalEncrypted += batch.size();

    for (Block block : batch) {
        // Verify encryption metadata
        String metadata = block.getEncryptionMetadata();
        System.out.println("Block #" + block.getBlockNumber() +
                          " encrypted at: " + block.getTimestamp());

        // Perform security audit
        auditEncryptedBlock(block);
    }

    offset += batchSize;
} while (!batch.isEmpty());

System.out.println("Total encrypted blocks: " + totalEncrypted);
```

## Key Features

- ✅ **Memory Efficient**: Process blocks in small batches instead of loading thousands at once
- ✅ **Thread-Safe**: Full concurrent access support with read locks
- ✅ **Flexible Filtering**: Returns only blocks matching specific criteria (encrypted or with off-chain data)
- ✅ **Ordered Results**: Always returns blocks ordered by block number (ascending)
- ✅ **Graceful Handling**: Returns empty list for invalid parameters (negative offset/limit)

## Performance Guidelines

| Batch Size | Use Case | Memory Impact | Performance |
|------------|----------|---------------|-------------|
| **10-50** | High-security audits, detailed processing | Minimal | Slower (more queries) |
| **100-500** | Standard operations, maintenance tasks | Low | Balanced ⭐ Recommended |
| **1000+** | Bulk operations, data migrations | Moderate | Faster (fewer queries) |

## Integration Examples

### Re-encryption Operation

```java
/**
 * Re-encrypt all encrypted blocks with a new password
 * Uses pagination to avoid memory exhaustion
 */
public void reencryptAllBlocks(Blockchain blockchain, String oldPassword, String newPassword) {
    int batchSize = 100;
    int offset = 0;
    int processedCount = 0;
    List<Block> batch;

    logger.info("Starting re-encryption of all encrypted blocks...");

    do {
        // Retrieve encrypted blocks in batches using high-level API
        batch = blockchain.getEncryptedBlocksPaginated(offset, batchSize);

        for (Block block : batch) {
            try {
                // Decrypt with old password (using DAO for encryption operations)
                Block decrypted = blockchain.getBlockDAO().getBlockWithDecryption(
                    block.getId(), oldPassword);

                if (decrypted != null) {
                    // Re-encrypt with new password
                    blockchain.getBlockDAO().saveBlockWithEncryption(decrypted, newPassword);
                    processedCount++;

                    if (processedCount % 50 == 0) {
                        logger.info("Re-encrypted {} blocks...", processedCount);
                    }
                }
            } catch (Exception e) {
                logger.error("Failed to re-encrypt block #{}: {}",
                            block.getBlockNumber(), e.getMessage());
            }
        }

        offset += batchSize;
    } while (!batch.isEmpty());

    logger.info("✅ Re-encryption complete: {} blocks processed", processedCount);
}
```

### Off-Chain Storage Analysis

```java
/**
 * Analyze off-chain storage usage and generate report
 * Uses pagination for memory-efficient processing
 */
public OffChainStorageReport analyzeOffChainUsage(Blockchain blockchain) {
    int batchSize = 200;
    int offset = 0;
    long totalSize = 0;
    int fileCount = 0;
    Map<String, Integer> fileTypeDistribution = new HashMap<>();
    List<Block> batch;

    do {
        // Retrieve blocks with off-chain data using high-level API
        batch = blockchain.getBlocksWithOffChainDataPaginated(offset, batchSize);

        for (Block block : batch) {
            OffChainData data = block.getOffChainData();

            // Accumulate statistics
            File file = new File(data.getFilePath());
            if (file.exists()) {
                totalSize += file.length();
                fileCount++;

                // Track file type distribution
                String extension = getFileExtension(data.getFilePath());
                fileTypeDistribution.merge(extension, 1, Integer::sum);
            }
        }

        offset += batchSize;
    } while (!batch.isEmpty());

    return new OffChainStorageReport(totalSize, fileCount, fileTypeDistribution);
}
```

## Best Practices

1. **Choose Appropriate Batch Sizes**:
   - Use 100-500 for most operations
   - Smaller batches for memory-constrained environments
   - Larger batches for bulk operations with ample memory

2. **Handle Empty Results**:
   - Always check if batch is empty before processing
   - Use `while (!batch.isEmpty())` pattern for iteration

3. **Progress Tracking**:
   - Log progress periodically (e.g., every 50 or 100 blocks)
   - Display percentage complete for long operations

4. **Error Handling**:
   - Wrap processing in try/catch blocks
   - Continue processing remaining blocks on individual failures
   - Log failures for later review

5. **Transaction Management**:
   - For read operations, pagination methods handle transactions automatically
   - For write operations during processing, manage transactions appropriately

## Best Practices for Large Datasets

When working with large blockchain datasets, always use pagination to avoid memory issues:

```java
Blockchain blockchain = new Blockchain();
int offset = 0;
int batchSize = 100;
List<Block> batch;

do {
    batch = blockchain.getEncryptedBlocksPaginated(offset, batchSize);
    for (Block block : batch) {
        processBlock(block);
    }
    offset += batchSize;
} while (!batch.isEmpty());
```

## Thread Safety

Both pagination methods are fully thread-safe:
- Use `ReentrantReadWriteLock` for concurrent access
- Read locks allow multiple threads to query simultaneously
- No write locks needed as these are read-only operations
- Safe to call from multiple threads without synchronization

## Related Documentation

- [API Guide](API_GUIDE.md) - Complete API reference
- [Batch Optimization Guide](BATCH_OPTIMIZATION_GUIDE.md) - General batch processing patterns
- [Performance Guide](PERFORMANCE_GUIDE.md) - Performance optimization techniques
