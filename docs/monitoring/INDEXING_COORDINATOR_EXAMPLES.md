# 🎯 IndexingCoordinator - Practical Usage Examples

---

## ⚠️ IMPORTANT: Manual Indexing Tests Only (Phase 5.4 Update)

> **NOTICE**: This document shows examples for **MANUAL indexing control** using `enableTestMode()`.
>
> **For Phase 5.4 ASYNC indexing tests**, use the patterns in [PHASE_5_4_TEST_ISOLATION_GUIDE.md](../testing/PHASE_5_4_TEST_ISOLATION_GUIDE.md) instead:
> - ❌ DO NOT use `enableTestMode()` in Phase 5.4 tests
> - ✅ Use `clearShutdownFlag()` in tearDown()
> - ✅ Use `clearIndexes()` instead of `clearAll()`
> - ✅ Use `waitForCompletion()` before assertions
>
> **Use this document only if:**
> - You need to manually control when indexing happens
> - You want to benchmark indexing performance separately
> - You're testing IndexingCoordinator internals

---

## ⚠️ SECURITY UPDATE (v1.0.6)

> **CRITICAL**: All UserFriendlyEncryptionAPI usage now requires **mandatory pre-authorization**. Users must be authorized before performing any operations.

### Required Secure Initialization

> **🔑 PREREQUISITE**: Generate genesis-admin keys first:
> ```bash
> ./tools/generate_genesis_keys.zsh
> ```
> This creates `./keys/genesis-admin.*` required for all examples below. **Backup securely!**

All code examples assume this initialization pattern:

```java
// 1. Create blockchain (only genesis block is automatic)
Blockchain blockchain = new Blockchain();

// 2. Load genesis admin keys (generated via ./tools/generate_genesis_keys.zsh)
KeyPair genesisKeys = KeyFileLoader.loadKeyPairFromFiles(
    "./keys/genesis-admin.private",
    "./keys/genesis-admin.public"
);

// 3. Register bootstrap admin in blockchain (REQUIRED!)
blockchain.createBootstrapAdmin(
    CryptoUtil.publicKeyToString(genesisKeys.getPublic()),
    "BOOTSTRAP_ADMIN"
);

// 4. Create API with genesis admin credentials
UserFriendlyEncryptionAPI api = new UserFriendlyEncryptionAPI(blockchain);
api.setDefaultCredentials("BOOTSTRAP_ADMIN", genesisKeys);

// 5. Create user for operations
KeyPair userKeys = api.createUser("username");
api.setDefaultCredentials("username", userKeys);
```

> **💡 NOTE**: See [../reference/API_GUIDE.md](../reference/API_GUIDE.md#-secure-initialization--authorization) for complete security details.

---

## Overview

The `IndexingCoordinator` is a powerful component that prevents infinite indexing loops and coordinates all blockchain indexing operations. This document provides practical examples for different use cases.

## Table of Contents
- [Thread Safety & Concurrent Indexing](#thread-safety--concurrent-indexing)
- [Basic Usage](#basic-usage)
- [Test Environment Setup](#test-environment-setup)
- [Advanced Configuration](#advanced-configuration)
- [Production Scenarios](#production-scenarios)
- [Troubleshooting](#troubleshooting)

---

## Thread Safety & Concurrent Indexing

### 🔒 Semaphore-Based Block Indexing Coordination

**Since v1.0.6**, `SearchFrameworkEngine` uses **per-block semaphores** to ensure thread-safe indexing in highly concurrent environments. This prevents race conditions and duplicate indexing when multiple threads attempt to index the same block simultaneously.

#### The Problem (Before Semaphores)

In concurrent scenarios (e.g., multiple threads creating encrypted blocks), race conditions occurred:

```
Thread 1: Check if block #274 indexed → NO → Start indexing
Thread 2: Check if block #274 indexed → NO → Start indexing  ❌ DUPLICATE!
Thread 1: putIfAbsent() → SUCCESS
Thread 2: putIfAbsent() → FAILS (already exists) → Returns without indexing
Result: indexed = 0, RuntimeException thrown ❌
```

**Issues:**
- ❌ Multiple threads competed to index the same block
- ❌ `putIfAbsent()` race condition caused `indexed == 0` errors
- ❌ Spurious "Indexing failed: 0 blocks indexed" exceptions
- ❌ Confusing logs with concurrent attempts

#### The Solution (Semaphore Coordination)

Each block hash has its own **fair semaphore** (`Semaphore(1, true)`) that coordinates exclusive access:

```java
// Inside SearchFrameworkEngine.indexBlock()
Semaphore semaphore = blockIndexingSemaphores.computeIfAbsent(
    blockHash, 
    k -> new Semaphore(1, true)  // 1 permit = exclusive access, fair = FIFO
);

try {
    semaphore.acquire();  // Wait if another thread is indexing
    
    // Double-check after acquiring lock
    if (globalProcessingMap.get(blockHash) != null) {
        logger.info("⏭️ [{}] Already indexed, skipping", shortHash);
        return;  // Another thread completed while we waited
    }
    
    // Mark as processing
    globalProcessingMap.put(blockHash, PROCESSING_PLACEHOLDER);
    
    // Generate and store metadata
    BlockMetadataLayers metadata = metadataManager.generateMetadataLayers(...);
    blockMetadataIndex.put(blockHash, metadata);
    globalProcessingMap.put(blockHash, metadata);
    
} finally {
    semaphore.release();  // Always release
}
```

**Benefits:**
- ✅ **Zero race conditions**: Only one thread indexes each block
- ✅ **Fair scheduling**: Threads wait in FIFO order (no starvation)
- ✅ **Double-check optimization**: After acquiring lock, verify if still needed
- ✅ **Guaranteed cleanup**: `finally` block always releases semaphore
- ✅ **Clean logs**: Clear `[hash] Waiting → Acquired → Released` messages

#### Concurrent Indexing Flow

```
Time  Thread 1           Thread 2           Thread 3
----  -----------------  -----------------  -----------------
T1    indexBlock(#274)   indexBlock(#274)   indexBlock(#275)
      acquire() ✅        acquire() ⏳       acquire() ✅
      
T2    [Indexing #274]    [WAITING]          [Indexing #275]
      generateMetadata   (blocked)          generateMetadata
      
T3    release() 🔓       acquire() ✅        release() 🔓
                         check: indexed? ✓
                         
T4                       release() 🔓
                         (skipped, already done)

Result: ✅ Both blocks indexed exactly once, no errors
```

#### Integration with Blockchain.indexBlocksRange()

The `Blockchain.indexBlocksRange()` method relies on semaphores for thread safety:

```java
for (Block block : blocks) {
    try {
        // SearchFrameworkEngine uses semaphores internally
        // Only one thread will actually index each block
        searchFrameworkEngine.indexBlock(block, password, privateKey, config);
        indexed++;
        
    } catch (Exception e) {
        // Individual failures don't stop the batch
        logger.warn("⚠️ Failed to index block {}: {}", 
            block.getBlockNumber(), e.getMessage());
        skipped++;
    }
}
```

**Key Points:**
- No need to check `isBlockIndexed()` beforehand - semaphores handle it
- Failures are logged but don't fail the entire batch
- `indexed == 0` is acceptable (blocks already indexed by other threads)

#### Testing Thread Safety

Tests with **high concurrency** (10+ writer threads, 15+ reader threads) validate:

```java
@Test
void testConcurrentBlockIndexing() {
    ExecutorService executor = Executors.newFixedThreadPool(25);
    CountDownLatch startLatch = new CountDownLatch(1);
    
    // 10 threads creating encrypted blocks concurrently
    for (int i = 0; i < 10; i++) {
        executor.submit(() -> {
            startLatch.await();
            Block block = blockchain.addEncryptedBlockWithKeywords(
                "data", 
                "password-" + threadId,  // Different passwords per thread
                keywords, 
                category, 
                privateKey, 
                publicKey
            );
            // Background indexing coordinated by semaphores ✅
        });
    }
    
    startLatch.countDown();  // Start all threads simultaneously
    
    // Wait for background indexing to complete
    IndexingCoordinator.getInstance().waitForCompletion();
    
    // Verify: All blocks indexed exactly once, no duplicates
    assertEquals(10, searchFrameworkEngine.getIndexedBlockCount());
}
```

**Results:**
- ✅ 2287 tests pass (including high-concurrency tests)
- ✅ Zero "0 blocks indexed" errors
- ✅ Clean shutdown with proper semaphore cleanup

#### Performance Characteristics

- **Overhead**: Minimal (~1-2ms per semaphore acquire/release)
- **Scalability**: Excellent - each block has its own semaphore (no global bottleneck)
- **Fairness**: FIFO ordering prevents thread starvation
- **Memory**: One semaphore per unique block hash (cleaned up automatically)

#### Best Practices

1. **Let semaphores do their job**: Don't add extra synchronization
2. **Trust the double-check**: After acquiring lock, check if work is still needed
3. **Accept `indexed == 0`**: Normal in concurrent scenarios
4. **Use `waitForCompletion()`**: In tests, wait for background indexing before assertions
5. **Clean up in tests**: Call `resetGlobalState()` in tearDown

---

## Basic Usage

### 🚀 Manual Setup (Current Implementation)

The IndexingCoordinator requires manual registration of indexers before use:

```java
public class BasicBlockchainApp {
    public static void main(String[] args) throws Exception {
        // 1. Secure initialization (see security section above)
        Blockchain blockchain = new Blockchain();
        KeyPair genesisKeys = KeyFileLoader.loadKeyPairFromFiles(
            "./keys/genesis-admin.private",
            "./keys/genesis-admin.public"
        );

        // Register bootstrap admin in blockchain (REQUIRED!)
        blockchain.createBootstrapAdmin(
            CryptoUtil.publicKeyToString(genesisKeys.getPublic()),
            "BOOTSTRAP_ADMIN"
        );

        UserFriendlyEncryptionAPI api = new UserFriendlyEncryptionAPI(blockchain);
        api.setDefaultCredentials("BOOTSTRAP_ADMIN", genesisKeys);

        // 2. Create application user
        KeyPair appKeys = api.createUser("myapp");
        api.setDefaultCredentials("myapp", appKeys);

        // 3. REQUIRED: Register indexers before using them
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

You must register the appropriate indexers before using IndexingCoordinator with UserFriendlyEncryptionAPI methods:

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
    void setUp() throws Exception {
        // Enable test mode to prevent automatic indexing
        coordinator = IndexingCoordinator.getInstance();
        coordinator.enableTestMode();

        // Secure initialization for tests
        blockchain = new Blockchain();
        KeyPair genesisKeys = KeyFileLoader.loadKeyPairFromFiles(
            "./keys/genesis-admin.private",
            "./keys/genesis-admin.public"
        );

        // Register bootstrap admin in blockchain (REQUIRED!)
        blockchain.createBootstrapAdmin(
            CryptoUtil.publicKeyToString(genesisKeys.getPublic()),
            "BOOTSTRAP_ADMIN"
        );

        api = new UserFriendlyEncryptionAPI(blockchain);
        api.setDefaultCredentials("BOOTSTRAP_ADMIN", genesisKeys);

        // Create test user
        KeyPair testKeys = generateKeyPair();
        api.addAuthorizedKey(testKeys.getPublic().getEncoded(), "testuser");
        api.setDefaultCredentials("testuser", testKeys);

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

> **Note**: Examples use constructor syntax. `IndexingRequest` parameters: operation, blocks, blockchain, forceRebuild, forceExecution, canWait, minIntervalMs

```java
@Test
@DisplayName("Should perform controlled indexing operations")
void shouldPerformControlledIndexing() {
    // Given: Create test data
    createTestBlocks(10);

    // When: Force indexing operations in test mode
    CompletableFuture<IndexingCoordinator.IndexingResult> metadataResult = 
        coordinator.coordinateIndexing(
            new IndexingCoordinator.IndexingRequest(
                "METADATA_INDEX_REBUILD", null, blockchain, false, true, true, 1000L)
        );

    CompletableFuture<IndexingCoordinator.IndexingResult> cacheResult = 
        coordinator.coordinateIndexing(
            new IndexingCoordinator.IndexingRequest(
                "ENCRYPTED_BLOCKS_CACHE_REBUILD", null, blockchain, false, true, true, 1000L)
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
    // When: Execute same operation twice
    CompletableFuture<IndexingCoordinator.IndexingResult> result1 = 
        coordinator.coordinateIndexing(
            new IndexingCoordinator.IndexingRequest(
                "TEST_OPERATION", null, null, false, true, true, 500L)
        );

    // Immediately try again
    CompletableFuture<IndexingCoordinator.IndexingResult> result2 = 
        coordinator.coordinateIndexing(
            new IndexingCoordinator.IndexingRequest(
                "TEST_OPERATION", null, null, false, true, true, 500L)
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
            new IndexingCoordinator.IndexingRequest(
                "CUSTOM_ANALYTICS_INDEX", blocks, null, false, false, false, 60000L);
        
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
            
            int indexed = 0;
            int failed = 0;
            
            // Batch processing for large datasets
            List<Block> blocks = request.getBlocks();
            if (blocks != null && blocks.size() > 1000) {
                for (Block block : blocks) {
                    try {
                        processBlock(block);
                        indexed++;
                    } catch (Exception e) {
                        // Individual failures don't stop batch processing
                        logger.warn("⚠️ Failed to index block {}: {}", 
                            block.getBlockNumber(), e.getMessage());
                        failed++;
                    }
                }
            } else {
                processStandardIndexing(blocks);
            }
            
            logger.info("✅ Production metadata indexing completed: {} indexed, {} failed", 
                indexed, failed);
        });
    }
    
    @Scheduled(fixedDelay = 300000) // Every 5 minutes
    public void scheduledMaintenance() {
        coordinator.coordinateIndexing(
            new IndexingCoordinator.IndexingRequest(
                "PRODUCTION_METADATA_INDEX", null, api.getBlockchain(), 
                false, false, false, 300000L)
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
                new IndexingCoordinator.IndexingRequest(
                    "BULK_METADATA_INDEX", newBlocks, null, false, false, true, 30000L)
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
                new IndexingCoordinator.IndexingRequest(
                    request.getOperation(), null, null, false, true, false, 1000L)
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
    new IndexingCoordinator.IndexingRequest(
        "MY_OPERATION", null, null, false, false, true, 300000L)
);

// ✅ Solution: Adjust interval or use forceRebuild
coordinator.coordinateIndexing(
    new IndexingCoordinator.IndexingRequest(
        "MY_OPERATION", null, null, true, false, true, 1000L)
);
```

#### Issue 3: Indexing Never Completes
```java
// ❌ Problem: Not registering custom indexer
coordinator.coordinateIndexing(
    new IndexingCoordinator.IndexingRequest("CUSTOM_OPERATION", null, null, false, false, true, 1000L)
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
                new IndexingCoordinator.IndexingRequest(
                    operation, null, null, true, true, true, 0L)
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

- **[Semaphore-Based Block Indexing Implementation](../development/SEMAPHORE_INDEXING_IMPLEMENTATION.md)** - Complete technical guide on per-block semaphores for thread-safe concurrent indexing
- **[Search Framework Guide](../search/SEARCH_FRAMEWORK_GUIDE.md)** - Advanced search engine with indexing coordination
- **[Thread Safety Standards](../testing/THREAD_SAFETY_STANDARDS.md)** - General thread safety guidelines and best practices
- **[Performance Optimization](../reports/PERFORMANCE_OPTIMIZATION_SUMMARY.md)** - Performance tuning for indexing and search operations

---

*For more information, see the main [IndexingCoordinator source code](../src/main/java/com/rbatllet/blockchain/indexing/IndexingCoordinator.java) and related tests.*