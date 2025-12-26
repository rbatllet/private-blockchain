# Blockchain Indexing Guide

Comprehensive guide to indexing blockchain data for search in the Private Blockchain Search Framework Engine.

## 📋 Table of Contents

- [Overview](#-overview)
- [Indexing Methods](#-indexing-methods)
  - [Asynchronous Indexing](#asynchronous-indexing-async)
  - [Synchronous Indexing](#synchronous-indexing-sync)
- [Privacy-by-Design Model](#-privacy-by-design-model)
- [Practical Examples](#-practical-examples)
  - [Example 1: Unit Test Pattern](#example-1-unit-test-pattern)
  - [Example 2: Demo Script Pattern](#example-2-demo-script-pattern)
  - [Example 3: Background Job Pattern](#example-3-background-job-pattern)
  - [Example 4: CLI Tool Pattern](#example-4-cli-tool-pattern)
  - [Example 5: Web API Pattern](#example-5-web-api-with-async-indexing)
  - [Complete Working Demo](#complete-working-demo)
- [Best Practices](#-best-practices)
- [Performance Comparison](#-performance-comparison)
- [Troubleshooting](#-troubleshooting)

## 🚀 Overview

The Search Framework Engine provides two indexing methods:

1. **`indexBlockchain()`** - Asynchronous (non-blocking)
2. **`indexBlockchainSync()`** - Synchronous (blocking)

Both methods process blockchain blocks and create searchable metadata indexes, but differ in execution model and use cases.

## 🔄 Indexing Methods

### Asynchronous Indexing (Async)

**Signature:**
```java
public IndexingResult indexBlockchain(
    Blockchain blockchain,
    String password,
    PrivateKey privateKey
)
```

**Behavior:**
- Returns **immediately** without waiting for completion
- Indexing happens in **background thread pool**
- Initial counts may not reflect final state
- Ideal for **non-blocking** scenarios

**Use Cases:**
- ✅ Background indexing jobs
- ✅ Scheduled maintenance tasks
- ✅ UI operations (avoid blocking interface)
- ✅ Fire-and-forget scenarios
- ✅ Web server endpoints

**Example:**
```java
SearchFrameworkEngine engine = new SearchFrameworkEngine();

// Trigger async indexing - returns immediately
IndexingResult result = engine.indexBlockchain(
    blockchain,
    password,
    privateKey
);

System.out.println("Indexing started: " + result.getTotalProcessed() + " blocks queued");

// Continue with other work - indexing happens in background
performOtherTasks();

// Optional: Wait for completion later if needed
IndexingCoordinator.getInstance().waitForCompletion();
System.out.println("✅ Indexing completed");
```

**Characteristics:**
| Feature | Value |
|---------|-------|
| Blocking | ❌ No |
| Return Time | Immediate |
| Count Accuracy | ⚠️ Approximate |
| Search Availability | ⏳ Delayed |
| Thread Safety | ✅ Yes |
| Best For | Background jobs |

---

### Synchronous Indexing (Sync)

**Signature:**
```java
public IndexingResult indexBlockchainSync(
    Blockchain blockchain,
    String password,
    PrivateKey privateKey
) throws RuntimeException
```

**Behavior:**
- **Blocks** calling thread until completion
- Returns only after **all blocks indexed**
- Accurate final counts from `blockMetadataIndex.size()`
- Search index **immediately available** after return

**Use Cases:**
- ✅ Unit tests (verify completion before assertions)
- ✅ Integration tests (ensure index ready)
- ✅ Demo scripts (sequential execution)
- ✅ CLI tools (user expects completion)
- ✅ Synchronous APIs (caller needs confirmation)

**Example:**
```java
SearchFrameworkEngine engine = new SearchFrameworkEngine();

// Trigger sync indexing - BLOCKS until complete
IndexingResult result = engine.indexBlockchainSync(
    blockchain,
    password,
    privateKey
);

// Returns ONLY after all blocks indexed
System.out.println("✅ Indexed " + result.getTotalIndexed() + " blocks");
System.out.println("⏱️ Duration: " + result.getDurationMs() + "ms");

// Safe to search immediately - index is ready
SearchResult searchResult = engine.search("medical", password, privateKey);
```

**Characteristics:**
| Feature | Value |
|---------|-------|
| Blocking | ✅ Yes |
| Return Time | After completion |
| Count Accuracy | ✅ Exact |
| Search Availability | ✅ Immediate |
| Thread Safety | ✅ Yes |
| Interruption | ⚠️ RuntimeException |
| Best For | Tests, demos, CLI |

**Exception Handling:**
```java
try {
    IndexingResult result = engine.indexBlockchainSync(
        blockchain, password, privateKey);
} catch (RuntimeException e) {
    if (e.getCause() instanceof InterruptedException) {
        System.err.println("⚠️ Indexing interrupted");
        // Thread interrupt status preserved
    }
    throw e;
}
```

## 🔒 Privacy-by-Design Model

**IMPORTANT:** The Search Framework Engine follows a **privacy-by-design** philosophy:

### Keyword Requirement

- ✅ **Blocks WITH keywords** → Indexed and searchable
- ❌ **Blocks WITHOUT keywords** → NOT indexed (remains private)

This applies to **both encrypted and unencrypted** blocks.

### Why This Matters

In a **private blockchain**, the default should be **privacy**, not convenience:

- Users must **explicitly opt-in** to searchability
- Prevents **accidental exposure** of sensitive data
- Aligns with **privacy-first** principles
- No automatic keyword extraction from content

### Making Blocks Searchable

**✅ Correct - Block will be indexed:**
```java
String[] keywords = {"medical", "patient", "diagnosis"};
blockchain.addBlockWithKeywords(
    "Patient medical record",
    keywords,           // Public keywords
    null,              // No private keywords
    privateKey,
    publicKey
);
```

**❌ Incorrect - Block remains private (not searchable):**
```java
blockchain.addBlock(
    "Confidential internal memo",
    privateKey,
    publicKey
);
// No keywords provided → block will NOT be indexed
```

### Logging

When indexing blocks without keywords:
```
🔒 Block #42 has no manual keywords - will not be searchable (privacy by design)
```

This is **intentional behavior**, not an error.

## 💡 Practical Examples

### Example 1: Unit Test Pattern

```java
@Test
public void testSearchAfterIndexing() throws Exception {
    // Setup blockchain with bootstrap admin
    Blockchain blockchain = new Blockchain();
    
    // Load genesis admin keys
    KeyPair genesisKeys = KeyFileLoader.loadKeyPairFromFiles(
        "./keys/genesis-admin.private",
        "./keys/genesis-admin.public"
    );
    
    // Register bootstrap admin (REQUIRED!)
    blockchain.createBootstrapAdmin(
        CryptoUtil.publicKeyToString(genesisKeys.getPublic()),
        "BOOTSTRAP_ADMIN"
    );
    
    // Create API with genesis admin
    UserFriendlyEncryptionAPI api = new UserFriendlyEncryptionAPI(blockchain);
    api.setDefaultCredentials("BOOTSTRAP_ADMIN", genesisKeys);
    
    // Create test user
    KeyPair userKeys = api.createUser("testuser");
    api.setDefaultCredentials("testuser", userKeys);
    
    String password = "test123";
    String[] keywords = {"medical", "patient", "diagnosis"};
    
    // Add searchable block (skip auto-indexing for manual SYNC indexing)
    blockchain.addBlockWithKeywords(
        "Patient diagnosis record",
        keywords,
        "MEDICAL",
        userKeys.getPrivate(),
        userKeys.getPublic(),
        true  // skipAutoIndexing - we'll use SYNC indexing
    );
    
    // Use SYNC indexing for tests - ensure completion
    SearchFrameworkEngine engine = new SearchFrameworkEngine();
    IndexingResult indexResult = engine.indexBlockchainSync(
        blockchain,
        null,  // No password - block is unencrypted
        userKeys.getPrivate()
    );
    
    // Now safe to assert - indexing is complete
    assertEquals(2, indexResult.getTotalIndexed()); // Genesis + test block
    
    // Search works immediately
    SearchResult searchResult = engine.search("medical", null, userKeys.getPrivate());
    assertEquals(1, searchResult.getResults().size());
}
```

**Why sync?**
- Test needs to verify indexing completed
- Assertions depend on search index being ready
- Sequential execution is required

---

### Example 2: Demo Script Pattern

```java
public class BlockchainSearchDemo {
    public static void main(String[] args) throws Exception {
        System.out.println("🚀 Private Blockchain Search Demo");
        
        // Create blockchain with sample data
        BlockchainWithKeys blockchainData = createSampleBlockchain();
        Blockchain blockchain = blockchainData.blockchain;
        PrivateKey userPrivateKey = blockchainData.userPrivateKey;
        
        // SYNC indexing for demos - users see sequential execution
        System.out.println("⏳ Indexing blockchain...");
        SearchFrameworkEngine engine = new SearchFrameworkEngine();
        
        long startTime = System.currentTimeMillis();
        IndexingResult result = engine.indexBlockchainSync(
            blockchain,
            null,  // No password - blocks are unencrypted
            userPrivateKey
        );
        long duration = System.currentTimeMillis() - startTime;
        
        System.out.println("✅ Indexed " + result.getTotalIndexed() + 
                           " blocks in " + duration + "ms");
        
        // Immediate search demonstration (unencrypted blocks, use null password)
        System.out.println("\n🔍 Searching for 'medical' records...");
        SearchResult medicalResults = engine.search("medical", null, 10);
        
        System.out.println("📊 Found " + medicalResults.getResults().size() + " results");
        medicalResults.getResults().forEach(r -> 
            System.out.println("  - Block #" + r.getBlockNumber() + 
                               " (relevance: " + r.getRelevanceScore() + ")")
        );
    }
    
    // Helper class to return blockchain and user keys
    static class BlockchainWithKeys {
        Blockchain blockchain;
        PrivateKey userPrivateKey;
        
        BlockchainWithKeys(Blockchain blockchain, PrivateKey userPrivateKey) {
            this.blockchain = blockchain;
            this.userPrivateKey = userPrivateKey;
        }
    }
    
    private static BlockchainWithKeys createSampleBlockchain() throws Exception {
        Blockchain blockchain = new Blockchain();
        
        // Load genesis admin keys
        KeyPair genesisKeys = KeyFileLoader.loadKeyPairFromFiles(
            "./keys/genesis-admin.private",
            "./keys/genesis-admin.public"
        );
        
        // Register bootstrap admin
        blockchain.createBootstrapAdmin(
            CryptoUtil.publicKeyToString(genesisKeys.getPublic()),
            "BOOTSTRAP_ADMIN"
        );
        
        // Create API with genesis admin
        UserFriendlyEncryptionAPI api = new UserFriendlyEncryptionAPI(blockchain);
        api.setDefaultCredentials("BOOTSTRAP_ADMIN", genesisKeys);
        
        // Create demo user
        KeyPair keys = api.createUser("demo-user");
        api.setDefaultCredentials("demo-user", keys);
        
        // Add searchable blocks (UNENCRYPTED with public keywords, skip auto-indexing)
        blockchain.addBlockWithKeywords(
            "Medical research paper",
            new String[]{"medical", "research", "clinical"},
            "RESEARCH",
            keys.getPrivate(),
            keys.getPublic(),
            true  // skipAutoIndexing
        );
        
        blockchain.addBlockWithKeywords(
            "Financial transaction log",
            new String[]{"financial", "transaction", "payment"},
            "FINANCE",
            keys.getPrivate(),
            keys.getPublic(),
            true  // skipAutoIndexing
        );
        
        blockchain.addBlockWithKeywords(
            "Legal contract agreement",
            new String[]{"legal", "contract", "agreement"},
            "LEGAL",
            keys.getPrivate(),
            keys.getPublic(),
            true  // skipAutoIndexing
        );
        
        return new BlockchainWithKeys(blockchain, keys.getPrivate());
    }
}
```

**Why sync?**
- Demo needs predictable sequential flow
- Users expect to see "indexing → searching → results"
- Immediate search required for demonstration

---

### Example 3: Background Job Pattern

```java
public class BlockchainIndexingJob {
    private final ScheduledExecutorService scheduler =
        Executors.newScheduledThreadPool(1, Thread.ofVirtual().factory()); // Java 25 Virtual Threads
    
    private final Blockchain blockchain;
    private final String indexingPassword;
    private final PrivateKey indexingPrivateKey;
    
    // Constructor receives blockchain and indexing credentials
    public BlockchainIndexingJob(
            Blockchain blockchain,
            String indexingPassword,
            PrivateKey indexingPrivateKey) {
        this.blockchain = blockchain;
        this.indexingPassword = indexingPassword;
        this.indexingPrivateKey = indexingPrivateKey;
    }
    
    public void scheduleIndexing() {
        // Schedule async indexing every hour
        scheduler.scheduleAtFixedRate(() -> {
            try {
                System.out.println("⏰ Starting scheduled blockchain indexing...");
                
                SearchFrameworkEngine engine = new SearchFrameworkEngine();
                
                // Use ASYNC for background jobs - non-blocking
                IndexingResult result = engine.indexBlockchain(
                    blockchain,
                    indexingPassword,
                    indexingPrivateKey
                );
                
                System.out.println("📨 Indexing job submitted: " + 
                                   result.getTotalProcessed() + " blocks queued");
                
                // Job continues, indexing happens in background
                // Next job will pick up any new blocks
                
            } catch (Exception e) {
                System.err.println("❌ Indexing job failed: " + e.getMessage());
            }
        }, 0, 1, TimeUnit.HOURS);
    }
    
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
```

**Why async?**
- Background job shouldn't block scheduler
- Non-blocking execution allows other tasks
- Fire-and-forget pattern ideal for periodic jobs

---

### Example 4: CLI Tool Pattern

```java
import com.rbatllet.blockchain.security.KeyFileLoader;
import com.rbatllet.blockchain.util.CryptoUtil;

public class SearchCLI {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: search-cli <blockchain-file> <search-term>");
            System.exit(1);
        }
        
        String blockchainFile = args[0];
        String searchTerm = args[1];
        
        try {
            // Load blockchain from file
            System.out.println("📂 Loading blockchain from " + blockchainFile + "...");
            Blockchain blockchain = loadBlockchain(blockchainFile);
            
            // CLI tools need SYNC - user waits for completion
            System.out.println("⏳ Indexing blockchain...");
            SearchFrameworkEngine engine = new SearchFrameworkEngine();
            
            IndexingResult indexResult = engine.indexBlockchainSync(
                blockchain,
                getPassword(),
                getPrivateKey()
            );
            
            System.out.println("✅ Indexed " + indexResult.getTotalIndexed() + 
                               " blocks in " + indexResult.getDurationMs() + "ms");
            
            // Now search
            System.out.println("\n🔍 Searching for: " + searchTerm);
            SearchResult result = engine.search(
                searchTerm, getPassword(), getPrivateKey());
            
            System.out.println("📊 Results: " + result.getResults().size() + 
                               " blocks found\n");
            
            // Display results
            result.getResults().forEach(r -> displayResult(r));
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            System.exit(1);
        }
    }
    
    private static void displayResult(EnhancedSearchResult result) {
        System.out.println("─────────────────────────────────────");
        System.out.println("Block #" + result.getBlockNumber());
        System.out.println("Hash: " + result.getBlockHash());
        System.out.println("Relevance: " + result.getRelevanceScore());
        System.out.println("Summary: " + result.getSummary());
        System.out.println();
    }
    
    private static Blockchain loadBlockchain(String filename) throws Exception {
        // In a real implementation, load from file
        // For this example, create a sample blockchain
        Blockchain blockchain = new Blockchain();
        
        // Load genesis admin keys
        KeyPair genesisKeys = KeyFileLoader.loadKeyPairFromFiles(
            "./keys/genesis-admin.private",
            "./keys/genesis-admin.public"
        );
        
        // Register bootstrap admin
        blockchain.createBootstrapAdmin(
            CryptoUtil.publicKeyToString(genesisKeys.getPublic()),
            "BOOTSTRAP_ADMIN"
        );
        
        return blockchain;
    }
    
    private static String getPassword() {
        // Could read from env var or prompt user
        return System.getenv("BLOCKCHAIN_PASSWORD");
    }
    
    private static PrivateKey getPrivateKey() throws Exception {
        // Load user's private key from file
        KeyPair userKeys = KeyFileLoader.loadKeyPairFromFiles(
            "./keys/search-user.private",
            "./keys/search-user.public"
        );
        return userKeys.getPrivate();
    }
}
```

**Why sync?**
- CLI user expects operation to complete
- Next command depends on indexing being ready
- Sequential execution matches user expectations

---

### Example 5: Web API with Async Indexing

```java
@RestController
@RequestMapping("/api/blockchain")
public class BlockchainController {
    
    private final Blockchain blockchain;
    private final SearchFrameworkEngine searchEngine;
    
    @PostMapping("/index")
    public ResponseEntity<IndexingResponse> triggerIndexing(
            @RequestBody IndexingRequest request) {
        
        // Use ASYNC for web endpoints - don't block request
        IndexingResult result = searchEngine.indexBlockchain(
            blockchain,
            request.getPassword(),
            request.getPrivateKey()
        );
        
        // Return immediately
        return ResponseEntity.accepted()
            .body(new IndexingResponse(
                "Indexing started",
                result.getTotalProcessed() + " blocks queued",
                System.currentTimeMillis()
            ));
    }
    
    @GetMapping("/index/status")
    public ResponseEntity<IndexingStatus> getIndexingStatus() {
        boolean isComplete = IndexingCoordinator.getInstance().isComplete();
        
        return ResponseEntity.ok(new IndexingStatus(
            isComplete ? "completed" : "in-progress",
            searchEngine.getIndexedBlockCount()
        ));
    }
}
```

**Why async?**
- Web requests should not block
- Client can poll status endpoint
- Better user experience (immediate response)

## ✅ Best Practices

### 1. Choose the Right Method

| Scenario | Use | Reason |
|----------|-----|--------|
| **Unit tests** | `indexBlockchainSync()` | Need to verify completion |
| **Integration tests** | `indexBlockchainSync()` | Ensure index ready for assertions |
| **Demo scripts** | `indexBlockchainSync()` | Sequential execution expected |
| **CLI tools** | `indexBlockchainSync()` | User expects completion |
| **Background jobs** | `indexBlockchain()` | Non-blocking execution |
| **Web APIs** | `indexBlockchain()` | Don't block HTTP requests |
| **Scheduled tasks** | `indexBlockchain()` | Fire-and-forget pattern |
| **UI operations** | `indexBlockchain()` | Avoid freezing interface |

### 2. Always Provide Keywords

```java
// ✅ CORRECT - Block will be searchable
String[] keywords = {"medical", "patient", "urgent"};
blockchain.addBlockWithKeywords(
    "Emergency patient record",
    keywords,
    null,
    privateKey,
    publicKey
);

// ❌ WRONG - Block will NOT be searchable
blockchain.addBlock(
    "Emergency patient record",
    privateKey,
    publicKey
);
```

### 3. Handle Interruptions (Sync Only)

```java
try {
    IndexingResult result = engine.indexBlockchainSync(
        blockchain, password, privateKey);
} catch (RuntimeException e) {
    if (e.getCause() instanceof InterruptedException) {
        logger.warn("Indexing interrupted");
        // Thread interrupt status is preserved
    }
    throw e;
}
```

### 4. Wait for Completion (Async)

```java
// Start async indexing
engine.indexBlockchain(blockchain, password, privateKey);

// Do other work...
performOtherTasks();

// Wait for completion before searching
IndexingCoordinator.getInstance().waitForCompletion();

// Now safe to search
SearchResult result = engine.search("medical", password, privateKey);
```

### 5. Monitor Indexing Progress

```java
IndexingCoordinator coordinator = IndexingCoordinator.getInstance();

// Check if indexing is complete
if (coordinator.isComplete()) {
    System.out.println("✅ Indexing complete");
} else {
    System.out.println("⏳ Indexing in progress...");
}

// Get current index size
int indexed = searchEngine.getIndexedBlockCount();
System.out.println("Blocks indexed: " + indexed);
```

## 📊 Performance Comparison

### Async vs Sync Indexing

| Metric | Async (`indexBlockchain`) | Sync (`indexBlockchainSync`) |
|--------|---------------------------|------------------------------|
| **Return Time** | Immediate (~1ms) | After completion (50ms-5s) |
| **Blocking** | ❌ No | ✅ Yes |
| **Count Accuracy** | ⚠️ Approximate | ✅ Exact |
| **Search Ready** | ⏳ After background completion | ✅ Immediately |
| **Thread Pool** | ✅ Uses background threads | ✅ Uses background + waits |
| **Interruption** | ➖ N/A | ⚠️ RuntimeException |
| **Best For** | Background jobs | Tests, demos, CLI |

### Indexing Performance (1000 blocks)

| Block Type | Async Start | Async Complete | Sync Total |
|------------|-------------|----------------|------------|
| **Unencrypted** | ~1ms | ~200ms | ~200ms |
| **Encrypted** | ~1ms | ~800ms | ~800ms |
| **Mixed (50/50)** | ~1ms | ~500ms | ~500ms |

**Notes:**
- Times measured on MacBook Pro M1, 16GB RAM
- Async "start" is time to return from method call
- Actual indexing time similar for both methods
- Sync blocks caller thread until completion

### Memory Usage

| Scenario | Memory Impact |
|----------|---------------|
| **1,000 blocks** | ~50 MB |
| **10,000 blocks** | ~200 MB |
| **100,000 blocks** | ~1.5 GB |

**Recommendation:** For large blockchains (>10,000 blocks), use batch indexing or async with periodic completion checks.

## 🔧 Troubleshooting

### Problem: Search returns 0 results after indexing

**Symptoms:**
```java
engine.indexBlockchain(blockchain, password, privateKey);
SearchResult result = engine.search("medical", password, privateKey);
// result.getResults().size() == 0
```

**Cause:** Async indexing not complete before search.

**Solution 1 - Use sync indexing:**
```java
engine.indexBlockchainSync(blockchain, password, privateKey);
SearchResult result = engine.search("medical", password, privateKey);
// Now works correctly
```

**Solution 2 - Wait for completion:**
```java
engine.indexBlockchain(blockchain, password, privateKey);
IndexingCoordinator.getInstance().waitForCompletion();
SearchResult result = engine.search("medical", password, privateKey);
// Now works correctly
```

---

### Problem: Blocks not being indexed

**Symptoms:**
```
🔒 Block #42 has no manual keywords - will not be searchable (privacy by design)
```

**Cause:** Blocks created without keywords.

**Solution:** Use `addBlockWithKeywords()`:
```java
// ❌ WRONG
blockchain.addBlock(data, privateKey, publicKey);

// ✅ CORRECT
String[] keywords = {"medical", "patient", "diagnosis"};
blockchain.addBlockWithKeywords(data, keywords, null, privateKey, publicKey);
```

---

### Problem: RuntimeException during sync indexing

**Symptoms:**
```
RuntimeException: Blockchain indexing was interrupted
```

**Cause:** Thread interrupted during `indexBlockchainSync()`.

**Solution:** Handle interruption gracefully:
```java
try {
    engine.indexBlockchainSync(blockchain, password, privateKey);
} catch (RuntimeException e) {
    if (e.getCause() instanceof InterruptedException) {
        logger.warn("Indexing interrupted - retrying...");
        // Retry or fail gracefully
    }
    throw e;
}
```

---

### Problem: Slow indexing performance

**Symptoms:** Indexing takes longer than expected.

**Possible Causes:**
1. **Large encrypted blocks** - Decryption is CPU-intensive
2. **Too many keywords** - More terms = more index updates
3. **Slow disk I/O** - Database writes can be bottleneck

**Solutions:**
```java
// 1. Use async for large datasets
engine.indexBlockchain(blockchain, password, privateKey);
// Do other work while indexing

// 2. Limit keywords per block
String[] keywords = {"medical", "urgent", "patient"}; // 3-5 keywords ideal

// 3. Batch process large blockchains
int batchSize = 1000;
for (int i = 0; i < blockchain.size(); i += batchSize) {
    Blockchain batch = blockchain.getRange(i, i + batchSize);
    engine.indexBlockchainSync(batch, password, privateKey);
    System.out.println("Indexed batch " + (i / batchSize + 1));
}
```

## 🔗 Related Documentation

- [Search Framework Guide](SEARCH_FRAMEWORK_GUIDE.md) - Complete search documentation
- [SearchSpecialistAPI Guide](SEARCHSPECIALISTAPI_INITIALIZATION_GUIDE.md) - High-level search API
- [API Reference](../reference/API_GUIDE.md) - Complete API documentation
- [Thread Safety Guide](../monitoring/INDEXING_COORDINATOR_EXAMPLES.md) - Concurrent indexing patterns

## 📚 Quick Reference

### Method Signatures

```java
// Asynchronous indexing (non-blocking)
public IndexingResult indexBlockchain(
    Blockchain blockchain,
    String password,
    PrivateKey privateKey
)

// Synchronous indexing (blocking)
public IndexingResult indexBlockchainSync(
    Blockchain blockchain,
    String password,
    PrivateKey privateKey
) throws RuntimeException
```

### IndexingResult Fields

```java
class IndexingResult {
    int totalProcessed;      // Blocks processed
    int successfullyIndexed; // Blocks successfully indexed
    double durationMs;       // Total duration in milliseconds
    RouterStats stats;       // Strategy router statistics
}
```

### Common Patterns

```java
// Pattern 1: Test with sync
@Test
public void test() {
    engine.indexBlockchainSync(blockchain, pass, key);
    assertEquals(10, searchResults.size());
}

// Pattern 2: Background job with async
scheduler.schedule(() -> 
    engine.indexBlockchain(blockchain, pass, key), 
    1, TimeUnit.HOURS);

// Pattern 3: CLI with sync
public static void main(String[] args) {
    engine.indexBlockchainSync(blockchain, pass, key);
    // Now search
}

// Pattern 4: Web API with async
@PostMapping("/index")
public ResponseEntity<?> index() {
    engine.indexBlockchain(blockchain, pass, key);
    return ResponseEntity.accepted().build();
}
```

### Complete Working Demo

For a complete, runnable implementation of all 4 examples above, see:

📁 **`src/main/java/demo/IndexingSyncDemo.java`**

This demo includes:
- ✅ Example 1: Hospital Medical Records System (100 patient records)
- ✅ Example 2: Demo Script Pattern (sequential execution)
- ✅ Example 3: Background Job Pattern (async indexing)
- ✅ Example 4: CLI Tool Pattern (sync for user expectations)

**Run the demo:**
```bash
./scripts/run_indexing_sync_demo.zsh
```

**Key features demonstrated:**
- `skipAutoIndexing` parameter for manual control
- SYNC vs ASYNC indexing patterns
- Unencrypted blocks with public keywords
- Multi-department security (cardiology, neurology, emergency)
- Cross-department search isolation
- IndexingCoordinator coordination between examples

---

**Last Updated:** December 3, 2025  
**Version:** 1.0  
**Applies To:** Search Framework Engine v2.0+
