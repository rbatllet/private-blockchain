# 🎯 IndexingCoordinator - Practical Usage Examples

## Overview

The `IndexingCoordinator` is a powerful component that prevents infinite indexing loops and coordinates all blockchain indexing operations. This document provides practical examples for different use cases.

## Table of Contents
- [Basic Usage](#basic-usage)
- [Test Environment Setup](#test-environment-setup)
- [Advanced Configuration](#advanced-configuration)
- [Production Scenarios](#production-scenarios)
- [Troubleshooting](#troubleshooting)

---

## Basic Usage

### 🚀 Manual Setup (Current Implementation)

The IndexingCoordinator requires manual registration of indexers before use:

```java
public class BasicBlockchainApp {
    public static void main(String[] args) {
        Blockchain blockchain = new Blockchain();
        UserFriendlyEncryptionAPI api = new UserFriendlyEncryptionAPI(
            blockchain, "myapp", generateKeyPair()
        );
        
        // 📝 REQUIRED: Register indexers before using them
        setupIndexers(api, blockchain);
        
        // Now these operations will be coordinated
        api.rebuildMetadataIndex();      // ✅ Coordinated (if indexer registered)
        api.rebuildRecipientIndex();     // ✅ Coordinated (if indexer registered)
        api.rebuildEncryptedBlocksCache(); // ✅ Coordinated (if indexer registered)
    }
    
    private static void setupIndexers(UserFriendlyEncryptionAPI api, Blockchain blockchain) {
        IndexingCoordinator coordinator = IndexingCoordinator.getInstance();
        
        // Register metadata indexer
        coordinator.registerIndexer("METADATA_INDEX_REBUILD", request -> {
            // Call the actual indexing method
            api.fallbackRebuildMetadataIndex(); // Private method - implement your own
        });
        
        // Register encrypted blocks cache indexer  
        coordinator.registerIndexer("ENCRYPTED_BLOCKS_CACHE_REBUILD", request -> {
            // Implement cache rebuilding logic
            rebuildEncryptedCache(blockchain);
        });
        
        // Register recipient indexer
        coordinator.registerIndexer("RECIPIENT_INDEX_REBUILD", request -> {
            // Implement recipient indexing logic
            rebuildRecipientIndex(blockchain);
        });
    }
}
```

**Note**: Currently, if indexers are not registered, the API methods will fail and fall back to direct execution.

### ⚠️ Important Setup Requirements

Before using IndexingCoordinator with UserFriendlyEncryptionAPI methods, you must register the appropriate indexers:

```java
public void setupStandardIndexers() {
    IndexingCoordinator coordinator = IndexingCoordinator.getInstance();
    
    // Required for api.rebuildMetadataIndex()
    coordinator.registerIndexer("METADATA_INDEX_REBUILD", request -> {
        // Implement metadata rebuilding
        logger.info("🔄 Rebuilding metadata index...");
        // Your implementation here
    });
    
    // Required for api.rebuildEncryptedBlocksCache()
    coordinator.registerIndexer("ENCRYPTED_BLOCKS_CACHE_REBUILD", request -> {
        // Implement cache rebuilding
        logger.info("🔄 Rebuilding encrypted blocks cache...");
        // Your implementation here
    });
    
    // Required for api.rebuildRecipientIndex()
    coordinator.registerIndexer("RECIPIENT_INDEX_REBUILD", request -> {
        // Implement recipient index rebuilding
        logger.info("🔄 Rebuilding recipient index...");
        // Your implementation here
    });
}
```

### 📊 Checking Coordination Status

```java
IndexingCoordinator coordinator = IndexingCoordinator.getInstance();

// Check if any indexing operation is currently running
if (coordinator.isIndexing()) {
    System.out.println("⏳ Indexing in progress...");
} else {
    System.out.println("✅ Ready for new operations");
}

// Check last execution time for specific operations
long lastMetadataIndexing = coordinator.getLastExecutionTime("METADATA_INDEX_REBUILD");
if (lastMetadataIndexing > 0) {
    long timeSince = System.currentTimeMillis() - lastMetadataIndexing;
    System.out.println("📊 Metadata indexed " + timeSince + "ms ago");
}
```

---

## Test Environment Setup

### 🧪 Basic Test Setup

```java
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MyBlockchainTests {

    private IndexingCoordinator coordinator;
    private Blockchain blockchain;
    private UserFriendlyEncryptionAPI api;

    @BeforeEach
    void setUp() {
        // Enable test mode to prevent automatic indexing
        coordinator = IndexingCoordinator.getInstance();
        coordinator.enableTestMode();
        
        blockchain = new Blockchain();
        api = new UserFriendlyEncryptionAPI(blockchain, "testuser", generateKeyPair());
        
        // Setup controlled indexing for tests
        setupControlledIndexing();
    }

    @AfterEach
    void tearDown() {
        coordinator.shutdown();
        coordinator.disableTestMode();
        if (blockchain != null) {
            blockchain.shutdown();
        }
    }

    private void setupControlledIndexing() {
        // Register controlled indexing operations
        coordinator.registerIndexer("METADATA_INDEX_REBUILD", request -> {
            // Minimal delay for realistic testing
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        coordinator.registerIndexer("ENCRYPTED_BLOCKS_CACHE_REBUILD", request -> {
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
}
```

### 🎯 Controlled Indexing in Tests

```java
@Test
@DisplayName("Should perform controlled indexing operations")
void shouldPerformControlledIndexing() {
    // Given: Create test data
    createTestBlocks(10);

    // When: Force indexing operations in test mode
    CompletableFuture<IndexingCoordinator.IndexingResult> metadataResult = 
        coordinator.coordinateIndexing(
            new IndexingCoordinator.IndexingRequest.Builder()
                .operation("METADATA_INDEX_REBUILD")
                .blockchain(blockchain)
                .forceExecution() // Required in test mode
                .build()
        );

    CompletableFuture<IndexingCoordinator.IndexingResult> cacheResult = 
        coordinator.coordinateIndexing(
            new IndexingCoordinator.IndexingRequest.Builder()
                .operation("ENCRYPTED_BLOCKS_CACHE_REBUILD")
                .blockchain(blockchain)
                .forceExecution()
                .build()
        );

    // Then: Wait for completion and verify results
    IndexingCoordinator.IndexingResult metadataRes = metadataResult.join();
    IndexingCoordinator.IndexingResult cacheRes = cacheResult.join();

    assertEquals("COMPLETED", metadataRes.getStatus());
    assertEquals("COMPLETED", cacheRes.getStatus());
    
    // Verify no infinite loops occurred
    assertTrue(metadataRes.getDurationMs() < 1000);
    assertTrue(cacheRes.getDurationMs() < 1000);
}
```

### 🔄 Testing Interval Management

```java
@Test
@DisplayName("Should respect minimum intervals between operations")
void shouldRespectMinimumIntervals() {
    // When: Execute same operation twice quickly
    CompletableFuture<IndexingCoordinator.IndexingResult> result1 = 
        coordinator.coordinateIndexing(
            new IndexingCoordinator.IndexingRequest.Builder()
                .operation("TEST_OPERATION")
                .forceExecution()
                .minInterval(500) // 500ms minimum interval
                .build()
        );

    // Immediately try again
    CompletableFuture<IndexingCoordinator.IndexingResult> result2 = 
        coordinator.coordinateIndexing(
            new IndexingCoordinator.IndexingRequest.Builder()
                .operation("TEST_OPERATION")
                .forceExecution()
                .minInterval(500)
                .build()
        );

    // Then: First should complete, second should be skipped
    IndexingCoordinator.IndexingResult res1 = result1.join();
    IndexingCoordinator.IndexingResult res2 = result2.join();

    assertEquals("COMPLETED", res1.getStatus());
    assertEquals("SKIPPED", res2.getStatus());
    assertEquals("Recently executed", res2.getMessage());
}
```

---

## Advanced Configuration

### ⚙️ Custom Indexing Operations

```java
public class CustomIndexingExample {
    
    public void setupCustomIndexing() {
        IndexingCoordinator coordinator = IndexingCoordinator.getInstance();
        
        // Register custom indexing operation
        coordinator.registerIndexer("CUSTOM_ANALYTICS_INDEX", request -> {
            System.out.println("🔍 Building analytics index...");
            
            // Custom indexing logic
            List<Block> blocks = request.getBlocks();
            if (blocks != null) {
                processAnalyticsData(blocks);
            }
            
            System.out.println("✅ Analytics index completed");
        });
    }
    
    public void triggerCustomIndexing(List<Block> blocks) {
        IndexingCoordinator coordinator = IndexingCoordinator.getInstance();
        
        IndexingCoordinator.IndexingRequest request = 
            new IndexingCoordinator.IndexingRequest.Builder()
                .operation("CUSTOM_ANALYTICS_INDEX")
                .blocks(blocks)
                .minInterval(60000) // 1 minute minimum interval
                .cannotWait() // Don't wait if another operation is running
                .build();
        
        coordinator.coordinateIndexing(request)
            .thenAccept(result -> {
                switch (result.getStatus()) {
                    case "COMPLETED":
                        System.out.println("✅ Custom indexing completed in " + 
                                         result.getDurationMs() + "ms");
                        break;
                    case "SKIPPED":
                        System.out.println("⏭️ Custom indexing skipped: " + 
                                         result.getMessage());
                        break;
                    case "FAILED":
                        System.err.println("❌ Custom indexing failed: " + 
                                         result.getMessage());
                        break;
                }
            });
    }
    
    private void processAnalyticsData(List<Block> blocks) {
        // Custom analytics processing
        blocks.stream()
            .filter(Block::isDataEncrypted)
            .forEach(block -> {
                // Process encrypted blocks for analytics
                analyzeBlock(block);
            });
    }
    
    private void analyzeBlock(Block block) {
        // Analytics logic here
    }
}
```

### 🔧 Production Configuration

```java
@Service
public class ProductionBlockchainService {
    
    private final IndexingCoordinator coordinator;
    private final UserFriendlyEncryptionAPI api;
    
    @PostConstruct
    public void initializeIndexing() {
        coordinator = IndexingCoordinator.getInstance();
        
        // Configure for production workload
        setupProductionIndexers();
        
        // Schedule periodic maintenance
        scheduleMaintenanceIndexing();
    }
    
    private void setupProductionIndexers() {
        // Heavy-duty metadata indexing
        coordinator.registerIndexer("PRODUCTION_METADATA_INDEX", request -> {
            logger.info("🔄 Starting production metadata indexing");
            
            try {
                // Batch processing for large datasets
                List<Block> blocks = request.getBlocks();
                if (blocks != null && blocks.size() > 1000) {
                    processBatches(blocks, 100); // 100 blocks per batch
                } else {
                    processStandardIndexing(blocks);
                }
                
                logger.info("✅ Production metadata indexing completed");
            } catch (Exception e) {
                logger.error("❌ Production indexing failed", e);
                throw new RuntimeException("Indexing failed", e);
            }
        });
    }
    
    @Scheduled(fixedDelay = 300000) // Every 5 minutes
    public void scheduledMaintenance() {
        coordinator.coordinateIndexing(
            new IndexingCoordinator.IndexingRequest.Builder()
                .operation("PRODUCTION_METADATA_INDEX")
                .blockchain(api.getBlockchain())
                .minInterval(300000) // 5 minutes minimum
                .cannotWait() // Skip if busy
                .build()
        );
    }
    
    private void processBatches(List<Block> blocks, int batchSize) {
        for (int i = 0; i < blocks.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, blocks.size());
            List<Block> batch = blocks.subList(i, endIndex);
            processBatch(batch);
            
            // Small delay between batches to prevent overwhelming
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
```

---

## Production Scenarios

### 🚀 High-Throughput Application

```java
@Component
public class HighThroughputBlockchainApp {
    
    private final IndexingCoordinator coordinator;
    private final ExecutorService executorService;
    
    public HighThroughputBlockchainApp() {
        this.coordinator = IndexingCoordinator.getInstance();
        this.executorService = Executors.newFixedThreadPool(4);
    }
    
    public void handleBulkOperations(List<Block> newBlocks) {
        // Process blocks in parallel, but coordinate indexing
        CompletableFuture<Void> processingFuture = CompletableFuture.runAsync(() -> {
            // Heavy processing work
            processBlocks(newBlocks);
        }, executorService);
        
        // Coordinate indexing after processing
        processingFuture.thenRun(() -> {
            coordinator.coordinateIndexing(
                new IndexingCoordinator.IndexingRequest.Builder()
                    .operation("BULK_METADATA_INDEX")
                    .blocks(newBlocks)
                    .minInterval(30000) // 30 seconds minimum
                    .build()
            ).thenAccept(result -> {
                if (result.isSuccess()) {
                    notifyIndexingComplete(newBlocks.size(), result.getDurationMs());
                } else {
                    handleIndexingFailure(result.getMessage());
                }
            });
        });
    }
    
    private void notifyIndexingComplete(int blockCount, long durationMs) {
        logger.info("📊 Indexed {} blocks in {}ms", blockCount, durationMs);
        // Send metrics to monitoring system
        meterRegistry.counter("indexing.completed", "blocks", String.valueOf(blockCount))
                    .increment();
        meterRegistry.timer("indexing.duration").record(durationMs, TimeUnit.MILLISECONDS);
    }
}
```

### 🔄 Microservices Integration

```java
@RestController
public class BlockchainMicroserviceController {
    
    private final IndexingCoordinator coordinator;
    
    @PostMapping("/api/v1/reindex")
    public ResponseEntity<IndexingStatusDto> triggerReindexing(
            @RequestBody ReindexRequestDto request) {
        
        CompletableFuture<IndexingCoordinator.IndexingResult> future = 
            coordinator.coordinateIndexing(
                new IndexingCoordinator.IndexingRequest.Builder()
                    .operation(request.getOperation())
                    .forceExecution()
                    .cannotWait() // Return immediately if busy
                    .build()
            );
        
        try {
            IndexingCoordinator.IndexingResult result = future.get(5, TimeUnit.SECONDS);
            
            IndexingStatusDto status = new IndexingStatusDto();
            status.setStatus(result.getStatus());
            status.setMessage(result.getMessage());
            status.setDurationMs(result.getDurationMs());
            
            return ResponseEntity.ok(status);
            
        } catch (TimeoutException e) {
            // Still processing
            IndexingStatusDto status = new IndexingStatusDto();
            status.setStatus("IN_PROGRESS");
            status.setMessage("Indexing operation is still running");
            return ResponseEntity.accepted().body(status);
            
        } catch (Exception e) {
            IndexingStatusDto status = new IndexingStatusDto();
            status.setStatus("ERROR");
            status.setMessage(e.getMessage());
            return ResponseEntity.status(500).body(status);
        }
    }
    
    @GetMapping("/api/v1/indexing/status")
    public ResponseEntity<IndexingStatusDto> getIndexingStatus() {
        IndexingStatusDto status = new IndexingStatusDto();
        
        if (coordinator.isIndexing()) {
            status.setStatus("IN_PROGRESS");
            status.setMessage("Indexing operation in progress");
        } else {
            status.setStatus("IDLE");
            status.setMessage("No indexing operations running");
        }
        
        return ResponseEntity.ok(status);
    }
}
```

---

## Troubleshooting

### 🔍 Common Issues and Solutions

#### Issue 1: Tests Still Taking Too Long
```java
// ❌ Problem: Forgot to enable test mode
@BeforeEach
void setUp() {
    // Missing: IndexingCoordinator.getInstance().enableTestMode();
    blockchain = new Blockchain();
}

// ✅ Solution: Always enable test mode
@BeforeEach
void setUp() {
    IndexingCoordinator.getInstance().enableTestMode();
    setupControlledIndexing();
    blockchain = new Blockchain();
}
```

#### Issue 2: Operations Being Skipped Unexpectedly
```java
// ❌ Problem: Minimum interval too high
coordinator.coordinateIndexing(
    new IndexingCoordinator.IndexingRequest.Builder()
        .operation("MY_OPERATION")
        .minInterval(300000) // 5 minutes - too long for frequent operations!
        .build()
);

// ✅ Solution: Adjust interval or use forceRebuild
coordinator.coordinateIndexing(
    new IndexingCoordinator.IndexingRequest.Builder()
        .operation("MY_OPERATION")
        .minInterval(1000) // 1 second
        .forceRebuild() // Or force if needed
        .build()
);
```

#### Issue 3: Indexing Never Completes
```java
// ❌ Problem: Not registering custom indexer
coordinator.coordinateIndexing(
    new IndexingCoordinator.IndexingRequest.Builder()
        .operation("CUSTOM_OPERATION") // No indexer registered!
        .build()
);

// ✅ Solution: Always register before using
coordinator.registerIndexer("CUSTOM_OPERATION", request -> {
    // Implementation here
});
```

### 📊 Monitoring and Debugging

```java
public class IndexingMonitor {
    
    public void monitorIndexingHealth() {
        IndexingCoordinator coordinator = IndexingCoordinator.getInstance();
        
        // Check current status
        boolean isIndexing = coordinator.isIndexing();
        logger.info("📊 Indexing status: {}", isIndexing ? "ACTIVE" : "IDLE");
        
        // Check last execution times
        String[] operations = {
            "METADATA_INDEX_REBUILD",
            "ENCRYPTED_BLOCKS_CACHE_REBUILD",
            "RECIPIENT_INDEX_REBUILD"
        };
        
        for (String operation : operations) {
            long lastExecution = coordinator.getLastExecutionTime(operation);
            if (lastExecution > 0) {
                long ageMs = System.currentTimeMillis() - lastExecution;
                logger.info("📅 {}: {}ms ago", operation, ageMs);
            } else {
                logger.info("❓ {}: never executed", operation);
            }
        }
    }
    
    @Scheduled(fixedDelay = 60000) // Every minute
    public void logIndexingStats() {
        monitorIndexingHealth();
    }
}
```

### 🆘 Emergency Procedures

```java
public class IndexingEmergencyProcedures {
    
    public void forceShutdownIndexing() {
        logger.warn("🚨 Force shutting down all indexing operations");
        IndexingCoordinator coordinator = IndexingCoordinator.getInstance();
        coordinator.forceShutdown();
        
        // Wait a moment then reinitialize
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Get fresh instance
        coordinator = IndexingCoordinator.getInstance();
        logger.info("✅ IndexingCoordinator reinitialized");
    }
    
    public void emergencyRebuildAllIndexes() {
        logger.warn("🚨 Emergency rebuild of all indexes");
        
        IndexingCoordinator coordinator = IndexingCoordinator.getInstance();
        
        // Force rebuild all critical indexes
        String[] criticalOperations = {
            "METADATA_INDEX_REBUILD",
            "ENCRYPTED_BLOCKS_CACHE_REBUILD"
        };
        
        for (String operation : criticalOperations) {
            coordinator.coordinateIndexing(
                new IndexingCoordinator.IndexingRequest.Builder()
                    .operation(operation)
                    .forceRebuild()
                    .forceExecution()
                    .minInterval(0) // No minimum interval
                    .build()
            ).thenAccept(result -> {
                logger.info("🔄 Emergency rebuild {}: {}", operation, result.getStatus());
            });
        }
    }
}
```

---

## Best Practices Summary

### ✅ Do's
- **Always enable test mode** in unit tests
- **Register custom indexers** before using them
- **Use appropriate minimum intervals** for your use case
- **Monitor indexing health** in production
- **Handle async results** properly with `thenAccept()` or `join()`

### ❌ Don'ts
- **Don't call indexing methods directly** in production (use coordinator)
- **Don't set minimum intervals too low** (< 100ms) for heavy operations
- **Don't ignore indexing results** - always handle success/failure
- **Don't create multiple IndexingCoordinator instances** (it's a singleton)
- **Don't forget to shutdown** in test teardown methods

### 🎯 Performance Tips
- Use `cannotWait()` for operations that should skip if system is busy
- Set realistic `minInterval` values based on your data size
- Consider batch processing for large datasets
- Monitor memory usage during heavy indexing operations

---

---

## ⚠️ Current Implementation Status

**Important**: The IndexingCoordinator is partially implemented. Current behavior:

1. **Manual Registration Required**: You must manually register indexers for each operation before using them
2. **Fallback Execution**: If indexers are not registered, UserFriendlyEncryptionAPI methods will log a warning and execute fallback methods directly
3. **No Auto-Configuration**: Unlike the examples above suggest, there is no automatic indexer registration

**To use IndexingCoordinator effectively**, ensure you register all required indexers during application startup:

```java
// Required setup before using UserFriendlyEncryptionAPI indexing methods
IndexingCoordinator coordinator = IndexingCoordinator.getInstance();

coordinator.registerIndexer("METADATA_INDEX_REBUILD", request -> {
    // Your metadata indexing implementation
});

coordinator.registerIndexer("ENCRYPTED_BLOCKS_CACHE_REBUILD", request -> {
    // Your cache rebuilding implementation  
});

coordinator.registerIndexer("RECIPIENT_INDEX_REBUILD", request -> {
    // Your recipient indexing implementation
});
```

## 🔗 Related Documentation

- **[Atomic Protection & Multi-Instance Coordination](ATOMIC_PROTECTION_MULTI_INSTANCE_GUIDE.md)** - Thread-safe multi-instance operations and global protection systems
- **[Search Framework Guide](SEARCH_FRAMEWORK_GUIDE.md)** - Advanced search engine with indexing coordination
- **[Thread Safety Standards](THREAD_SAFETY_STANDARDS.md)** - General thread safety guidelines and best practices
- **[Performance Optimization](PERFORMANCE_OPTIMIZATION_SUMMARY.md)** - Performance tuning for indexing and search operations

---

*For more information, see the main [IndexingCoordinator source code](../src/main/java/com/rbatllet/blockchain/indexing/IndexingCoordinator.java) and related tests.*