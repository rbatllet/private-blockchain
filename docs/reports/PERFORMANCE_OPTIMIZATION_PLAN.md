# Performance Best Practices

## 📊 Overview

This guide covers performance optimization strategies for the Private Blockchain implementation, focusing on memory-efficient operations and scalable data access patterns.

## 🚀 Core Performance Features

### 1. Pagination Support
- **Method**: `getBlocksPaginated(long offset, int limit)`
- **Purpose**: Retrieve blocks in manageable chunks
- **Benefit**: Constant memory usage regardless of blockchain size
- **Default Batch Size**: 1000 blocks
- **Note**: Offset is `long` to prevent integer overflow with large blockchains (>2.1B blocks)

### 2. Streaming API
- **Method**: `processChainInBatches(Consumer<List<Block>>, int batchSize)`
- **Purpose**: Functional programming interface for batch processing
- **Benefit**: Clean, readable code with automatic memory management
- **Use Cases**: Validation, analysis, export operations

### 3. Individual Block Access
- **Method**: `getBlock(long blockNumber)`
- **Purpose**: Retrieve specific blocks by number
- **Benefit**: O(1) memory usage, indexed database access

## 💡 Usage Patterns

### Pattern 1: Batch Processing (Recommended)

```java
// Process entire blockchain in memory-efficient batches
blockchain.processChainInBatches(batch -> {
    for (Block block : batch) {
        // Process each block
        analyzeBlock(block);
    }
}, MemorySafetyConstants.DEFAULT_BATCH_SIZE); // 1000 blocks per batch
```

**Best For**:
- Chain validation
- Data analysis
- Export operations
- Search operations

### Pattern 2: Manual Pagination

```java
// Manual control over pagination
long totalBlocks = blockchain.getBlockCount();
// ⚠️ Use long offset to prevent overflow with large blockchains
for (long offset = 0; offset < totalBlocks; offset += 1000) {
    List<Block> batch = blockchain.getBlocksPaginated(offset, 1000);

    for (Block block : batch) {
        processBlock(block);
    }
}
```

**Best For**:
- Custom progress tracking
- Conditional processing
- Complex filtering logic

### Pattern 3: Individual Access

```java
// Direct access to specific blocks
Block block = blockchain.getBlock(blockNumber);
if (block != null) {
    processBlock(block);
}
```

**Best For**:
- Single block operations
- Random access patterns
- Block verification

## 📈 Performance Characteristics

| Operation | Memory Usage | Time Complexity | Scalability |
|-----------|--------------|-----------------|-------------|
| `processChainInBatches()` | O(batch_size) | O(n) | Excellent |
| `getBlocksPaginated()` | O(limit) | O(n) | Excellent |
| `getBlock()` | O(1) | O(1) | Perfect |
| `getBlockCount()` | O(1) | O(1) | Perfect |

### Expected Performance

**Memory Efficiency**:
- Small chains (< 1,000 blocks): < 10MB
- Medium chains (< 10,000 blocks): < 50MB
- Large chains (> 100,000 blocks): < 100MB (constant)

**Processing Speed**:
- Batch processing: ~1,000 blocks/second
- Chain validation: < 5 seconds for 10,000 blocks
- Individual block access: < 1ms

## 🎯 Best Practices

### 1. Choose the Right Pattern

```java
// ✅ GOOD - Batch processing for iteration
blockchain.processChainInBatches(batch -> {
    batch.forEach(this::processBlock);
}, 1000);

// ❌ AVOID - Individual access in loops
for (long i = 0; i < blockchain.getBlockCount(); i++) {
    Block block = blockchain.getBlock(i); // Multiple DB queries
    processBlock(block);
}
```

### 2. Optimize Batch Size

```java
// Small batches for memory-constrained environments
blockchain.processChainInBatches(batch -> {
    // Process
}, 100); // 100 blocks per batch

// Large batches for high-performance systems
blockchain.processChainInBatches(batch -> {
    // Process
}, 5000); // 5000 blocks per batch
```

### 3. Use Progress Logging

```java
long totalBlocks = blockchain.getBlockCount();
AtomicLong processed = new AtomicLong(0);

blockchain.processChainInBatches(batch -> {
    batch.forEach(this::processBlock);
    long current = processed.addAndGet(batch.size());

    if (current % 10000 == 0) {
        System.out.printf("📊 Processed %d/%d blocks (%.1f%%)%n",
            current, totalBlocks, (current * 100.0) / totalBlocks);
    }
}, 1000);
```

## 🔍 Common Use Cases

### Use Case 1: Chain Validation

```java
public ChainValidationResult validateChainDetailed() {
    List<BlockValidationResult> results = new ArrayList<>();
    Block previousBlock = blockchain.getBlock(0); // Genesis

    blockchain.processChainInBatches(batch -> {
        for (Block currentBlock : batch) {
            BlockValidationResult result = validateBlock(
                currentBlock, previousBlock);
            results.add(result);
            previousBlock = currentBlock;
        }
    }, 1000);

    return new ChainValidationResult(results);
}
```

### Use Case 2: Data Export

```java
public void exportToJson(String outputPath) {
    try (FileWriter writer = new FileWriter(outputPath)) {
        writer.write("{\"blocks\":[");

        AtomicBoolean first = new AtomicBoolean(true);
        blockchain.processChainInBatches(batch -> {
            for (Block block : batch) {
                if (!first.getAndSet(false)) {
                    writer.write(",");
                }
                writer.write(blockToJson(block));
            }
        }, 1000);

        writer.write("]}");
    }
}
```

### Use Case 3: Search Operations

```java
public List<Block> searchByContent(String keyword) {
    List<Block> matches = Collections.synchronizedList(new ArrayList<>());

    blockchain.processChainInBatches(batch -> {
        batch.stream()
            .filter(block -> block.getData().contains(keyword))
            .forEach(matches::add);
    }, 1000);

    return matches;
}
```

## 📚 Related Documentation

- **[PAGINATION_AND_BATCH_PROCESSING_GUIDE.md](../data-management/PAGINATION_AND_BATCH_PROCESSING_GUIDE.md)** - Detailed pagination guide
- **[API_GUIDE.md](../reference/API_GUIDE.md)** - Complete API reference
- **[EXAMPLES.md](../getting-started/EXAMPLES.md)** - Code examples and patterns

## 🔧 Configuration

### Batch Size Tuning

```java
// Configure based on your environment
int batchSize;

if (Runtime.getRuntime().maxMemory() < 512 * 1024 * 1024) {
    batchSize = MemorySafetyConstants.FALLBACK_BATCH_SIZE;  // Low memory systems (100)
} else if (Runtime.getRuntime().maxMemory() < 2 * 1024 * 1024 * 1024) {
    batchSize = MemorySafetyConstants.DEFAULT_BATCH_SIZE; // Standard systems (1000)
} else {
    batchSize = 5000; // High-performance systems
}

blockchain.processChainInBatches(this::processBatch, batchSize);
```

### Database Optimization

```properties
# persistence.xml settings for optimal performance
hibernate.jdbc.fetch_size=1000
hibernate.jdbc.batch_size=50
hibernate.order_inserts=true
hibernate.order_updates=true
```

## 🎓 Summary

**Key Takeaways**:
1. Always use batch processing for blockchain iteration
2. Choose batch size based on available memory
3. Use `getBlock()` for individual access only
4. Monitor memory usage in production
5. Add progress logging for long operations

**Performance Goals**:
- Constant memory usage: O(batch_size)
- Scalable to millions of blocks
- Fast processing: ~1,000 blocks/second
- Minimal database overhead
