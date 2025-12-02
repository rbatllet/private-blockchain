# Phase 5.4 Test Isolation Guide

## 📋 Overview

**Phase 5.4** introduces automatic background indexing using `IndexingCoordinator` singleton. This creates a critical test isolation challenge: **singleton state contamination between tests**.

**Problem Symptoms:**
- ✅ Individual tests pass (1 result found)
- ❌ Batch tests fail (0 results found)
- Tests after the first one cannot index blocks
- Tests with debug logs pass, without logs fail (timing-dependent)

**Root Causes:**
1. `IndexingCoordinator.forceShutdown()` sets `shutdownRequested=true`, which **blocks all future indexing operations** across all subsequent tests
2. `waitForCompletion()` race condition returns before async task starts, causing searches before indexing completes

---

## 🔍 Technical Analysis

### The Singleton State Problem

```java
// Test 1: setUp() → addBlock() → INDEXING WORKS ✅
// Test 1: tearDown() → forceShutdown() → shutdownRequested=true
// Test 2: setUp() → addBlock() → INDEXING BLOCKED ❌ (shutdownRequested=true)
// Test 2: Search returns 0 results (block was never indexed)
```

**Why `new Blockchain()` doesn't help:**
- `IndexingCoordinator` is a **global singleton** (shared across all Blockchain instances)
- Creating new Blockchain instances doesn't reset `IndexingCoordinator.shutdownRequested`
- Search indexes (`FastIndexSearch`, `EncryptedContentSearch`) were cleared, but executor services were shut down

---

## ✅ Complete Solution (7 coordinated fixes)

### 1. SearchStrategyRouter.clearIndexes() (NEW METHOD)

**Purpose:** Clear search indexes WITHOUT shutting down executor service.

**Location:** `src/main/java/com/rbatllet/blockchain/search/strategy/SearchStrategyRouter.java`

```java
/**
 * Clear all search indexes without shutting down the executor service.
 * Use this for test cleanup or database reinitialization.
 */
public void clearIndexes() {
    fastIndexSearch.clearAll();
    encryptedContentSearch.clearAll();
}

public void shutdown() {
    executorService.shutdown();  // ← Only shutdown() does this
    fastIndexSearch.clearAll();
    encryptedContentSearch.clearAll();
}
```

**Key Difference:**
- `clearIndexes()`: Clears indexes, keeps executor alive ✅ (for tests)
- `shutdown()`: Clears indexes + shuts down executor ❌ (for app shutdown)

---

### 2. SearchFrameworkEngine.clearIndexes() (NEW METHOD)

**Purpose:** Clear metadata cache + indexes WITHOUT shutting down search engine.

**Location:** `src/main/java/com/rbatllet/blockchain/search/SearchFrameworkEngine.java`

```java
/**
 * Clear all search indexes without shutting down the search engine.
 * Use this for test cleanup or database reinitialization.
 *
 * <p>Unlike {@link #clearAll()}, this method does NOT shut down the executor service,
 * allowing the search engine to continue operating after index cleanup.</p>
 */
public void clearIndexes() {
    blockMetadataIndex.clear();
    strategyRouter.clearIndexes();
}

public void clearAll() {
    blockMetadataIndex.clear();
    strategyRouter.shutdown();  // ← Shuts down executor (DANGEROUS in tests!)
}
```

**Usage:**
- Tests: Use `clearIndexes()`
- App shutdown: Use `clearAll()` or `shutdown()`

---

### 3. SearchSpecialistAPI.clearCache() (MODIFIED)

**Purpose:** Update to use `clearIndexes()` instead of `clearAll()`.

**Location:** `src/main/java/com/rbatllet/blockchain/search/SearchSpecialistAPI.java`

```java
/**
 * Clear search caches and password registry without shutting down the search engine.
 * Use this for test cleanup or database reinitialization.
 *
 * <p>Unlike {@link #shutdown()}, this method does NOT shut down the executor service,
 * allowing the search engine to continue operating after cache cleanup.</p>
 */
public void clearCache() {
    searchEngine.clearIndexes();  // ← Phase 5.4 FIX: was clearAll()
    passwordRegistry.clearAll();
    isInitialized = false;
}
```

**Before (BUG):**
```java
searchEngine.clearAll();  // ❌ Shuts down executor service!
```

**After (FIX):**
```java
searchEngine.clearIndexes();  // ✅ Keeps executor alive!
```

---

### 4. IndexingCoordinator.clearShutdownFlag() (NEW METHOD)

**Purpose:** Clear `shutdownRequested` flag WITHOUT enabling test mode.

**Location:** `src/main/java/com/rbatllet/blockchain/indexing/IndexingCoordinator.java`

```java
/**
 * Phase 5.4 FIX: Clear shutdown flag to allow indexing in next test.
 * Unlike reset(), this does NOT enable test mode, allowing normal async indexing.
 */
public void clearShutdownFlag() {
    shutdownRequested.set(false);
    logger.info("🔄 IndexingCoordinator shutdown flag cleared - async indexing re-enabled");
}
```

**Comparison:**

| Method | `shutdownRequested` | `testMode` | Use Case |
|--------|-------------------|-----------|----------|
| `reset()` | `false` | `true` | Manual indexing tests |
| `clearShutdownFlag()` | `false` | unchanged | Async indexing tests (Phase 5.4) |
| `forceShutdown()` | `true` | unchanged | Stop indexing |

---

### 5. Blockchain.clearAndReinitialize() (MODIFIED)

**Purpose:** Clear search indexes when database is cleared.

**Location:** `src/main/java/com/rbatllet/blockchain/core/Blockchain.java:6264-6273`

```java
// Phase 5.4 FIX: Clear search indexes to prevent references to deleted blocks
// This ensures SearchFrameworkEngine is synchronized with the cleared database
// Use clearIndexes() instead of clearAll() to avoid shutting down executor service
try {
    searchFrameworkEngine.clearIndexes();
    searchSpecialistAPI.clearCache();  // Also clear SearchSpecialistAPI cache (including BlockPasswordRegistry)
    logger.info("🔍 Cleared search indexes (FastIndexSearch + EncryptedContentSearch + BlockPasswordRegistry)");
} catch (Exception searchEx) {
    logger.warn("⚠️ Search index cleanup had issues (non-critical): {}", searchEx.getMessage());
}
```

**Why this matters:**
- `clearAndReinitialize()` deletes all blocks from DB
- Old search indexes still reference deleted block hashes → stale state
- Clearing indexes ensures search stays synchronized with DB

---

### 6. Test tearDown() Pattern (CRITICAL FIX)

**Purpose:** Properly reset IndexingCoordinator singleton between tests.

**Example:** `src/test/java/com/rbatllet/blockchain/search/SearchIntegrityFixTest.java:82-100`

```java
@AfterEach
void tearDown() {
    try {
        // Phase 5.4 FIX: Wait for async indexing BEFORE shutdown
        // This ensures background indexing tasks complete before coordinator shuts down
        logger.info("⏳ Waiting for any pending async indexing to complete before teardown...");
        IndexingCoordinator.getInstance().waitForCompletion();
        logger.info("✅ All async indexing completed, proceeding with shutdown");

        // CRITICAL: Use forceShutdown() + clearShutdownFlag() to properly reset singleton state
        // forceShutdown() sets shutdownRequested=true, blocking future indexing
        // clearShutdownFlag() clears it without enabling test mode, allowing next test to index
        IndexingCoordinator.getInstance().forceShutdown();
        IndexingCoordinator.getInstance().clearShutdownFlag();  // ← Phase 5.4 FIX!
        IndexingCoordinator.getInstance().disableTestMode();

        if (blockchain != null) {
            // Clean database after each test to ensure test isolation
            blockchain.clearAndReinitialize();  // ← Also clears search indexes (fix #5)
            blockchain.shutdown();
        }
    } catch (Exception e) {
        logger.warn("Teardown warning: {}", e.getMessage());
    }
}
```

**Before (BUG):**
```java
IndexingCoordinator.getInstance().forceShutdown();  // ❌ Sets shutdownRequested=true
IndexingCoordinator.getInstance().disableTestMode();
// Next test: shutdownRequested=true → indexing BLOCKED!
```

**After (FIX):**
```java
IndexingCoordinator.getInstance().forceShutdown();
IndexingCoordinator.getInstance().clearShutdownFlag();  // ✅ Clears shutdownRequested
IndexingCoordinator.getInstance().disableTestMode();
// Next test: shutdownRequested=false → indexing WORKS!
```

---

### 7. IndexingCoordinator.waitForCompletion() Race Condition Fix (CRITICAL)

**Problem:** `waitForCompletion()` checked semaphore availability BEFORE async task started, causing it to return immediately without waiting.

**Symptom:**
- Tests with debug logs: ✅ PASS (logs add ~50-100ms latency, async task starts in time)
- Tests without debug logs: ❌ FAIL (0 results, async task hasn't started yet when check happens)

**Root Cause:**
```java
// BEFORE (BUG):
public boolean waitForCompletion(long timeoutMs) {
    while (indexingSemaphore.availablePermits() == 0) {  // ❌ Race condition!
        Thread.sleep(50);
    }
    return true;
}

// Race condition scenario:
// 1. addBlock() launches async task (CompletableFuture.supplyAsync)
// 2. waitForCompletion() checks semaphore BEFORE async task acquires it
// 3. Semaphore still available → returns immediately
// 4. Search executes before indexing → 0 results ❌
```

**Solution:** Track active async tasks with atomic counter:

**Location:** `src/main/java/com/rbatllet/blockchain/indexing/IndexingCoordinator.java`

```java
// Phase 5.4 FIX: Track active async indexing tasks to prevent race condition
private final AtomicInteger activeIndexingTasks = new AtomicInteger(0);

public CompletableFuture<IndexingResult> coordinateIndexing(IndexingRequest request) {
    // ... (shutdown checks)

    // Phase 5.4 FIX: Increment BEFORE launching async task
    // This prevents race condition where waitForCompletion() checks before task starts
    activeIndexingTasks.incrementAndGet();

    return CompletableFuture.supplyAsync(() -> {
        try {
            try {
                // Acquire semaphore, execute indexing...
                return executeIndexing(request);
            } catch (InterruptedException e) {
                return IndexingResult.cancelled("Interrupted");
            }
        } finally {
            // Phase 5.4 FIX: Decrement when task completes
            // This allows waitForCompletion() to detect completion
            activeIndexingTasks.decrementAndGet();
        }
    });
}

public boolean waitForCompletion(long timeoutMs) throws InterruptedException {
    long startTime = System.currentTimeMillis();

    // Phase 5.4 FIX: Wait for active tasks counter instead of semaphore
    // This prevents race condition where we check before async task increments counter
    while (activeIndexingTasks.get() > 0) {
        if (timeoutMs > 0) {
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed > timeoutMs) {
                logger.warn("⏱️ Timeout reached ({}ms, {} tasks still active)",
                           elapsed, activeIndexingTasks.get());
                return false;
            }
        }
        Thread.sleep(50);  // Check every 50ms
    }

    return true;
}
```

**Key Points:**
1. **Increment BEFORE `supplyAsync()`** - Ensures counter is set when task is queued
2. **Decrement in outer `finally`** - Guarantees cleanup even if task fails
3. **Wait on counter, not semaphore** - Eliminates race condition window
4. **No fixed delays/workarounds** - Deterministic signal-based solution

**Why This Works:**
- Main thread increments counter synchronously before launching async task
- `waitForCompletion()` sees counter > 0 and waits
- Async task decrements counter in finally block
- `waitForCompletion()` detects counter == 0 and returns

**Before vs After:**

| Scenario | Before (BUG) | After (FIX) |
|----------|-------------|-------------|
| Fast execution (no debug logs) | Returns immediately (0 ms) ❌ | Waits for completion (~50-200ms) ✅ |
| Slow execution (with debug logs) | Waits by accident (~100ms) ✅ | Waits correctly (~100-300ms) ✅ |
| Test reliability | Flaky (timing-dependent) ❌ | Deterministic ✅ |

---

## 📝 Standard Test Pattern for Phase 5.4

> **🔑 KEY GENERATION**: Tests automatically generate genesis keys if missing (see [AUTO_GENESIS_KEY_GENERATION.md](AUTO_GENESIS_KEY_GENERATION.md)).
> For manual generation: `./tools/generate_genesis_keys.zsh`

### Complete Example

```java
@DisplayName("Phase 5.4 Async Indexing Test")
class MyAsyncIndexingTest {

    private Blockchain blockchain;
    private UserFriendlyEncryptionAPI api;
    private KeyPair bootstrapKeyPair;
    private KeyPair testKeyPair;

    @BeforeEach
    void setUp() {
        // Phase 5.4 FIX: DO NOT enable test mode - it skips async indexing!
        // IndexingCoordinator.getInstance().enableTestMode();  // ❌ DO NOT!

        blockchain = new Blockchain();
        blockchain.clearAndReinitialize();  // Clears DB + search indexes

        // Load bootstrap admin keys (auto-generated by tests if missing)
        bootstrapKeyPair = KeyFileLoader.loadKeyPairFromFiles(
            "./keys/genesis-admin.private",
            "./keys/genesis-admin.public"
        );

        // Register bootstrap admin in blockchain (RBAC v1.0.6)
        blockchain.createBootstrapAdmin(
            CryptoUtil.publicKeyToString(bootstrapKeyPair.getPublic()),
            "BOOTSTRAP_ADMIN"
        );

        testKeyPair = CryptoUtil.generateKeyPair();

        // Pre-authorize user
        String publicKeyString = CryptoUtil.publicKeyToString(testKeyPair.getPublic());
        blockchain.addAuthorizedKey(publicKeyString, "test_user", bootstrapKeyPair, UserRole.USER);

        api = new UserFriendlyEncryptionAPI(blockchain, "test_user", testKeyPair);

        // Phase 5.4: DO NOT initialize SearchSpecialistAPI in setUp() -
        // it must be initialized AFTER blocks are created and indexed
    }

    @AfterEach
    void tearDown() {
        try {
            // Phase 5.4 FIX: Wait for async indexing BEFORE shutdown
            logger.info("⏳ Waiting for any pending async indexing to complete before teardown...");
            IndexingCoordinator.getInstance().waitForCompletion();
            logger.info("✅ All async indexing completed, proceeding with shutdown");

            // CRITICAL: Reset IndexingCoordinator singleton state
            IndexingCoordinator.getInstance().forceShutdown();
            IndexingCoordinator.getInstance().clearShutdownFlag();  // ← Phase 5.4 FIX!
            IndexingCoordinator.getInstance().disableTestMode();

            if (blockchain != null) {
                blockchain.clearAndReinitialize();
                blockchain.shutdown();
            }
        } catch (Exception e) {
            logger.warn("Teardown warning: {}", e.getMessage());
        }
    }

    @Test
    @DisplayName("Test async indexing with searchable data")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testAsyncIndexing() {
        // Create block (triggers async indexing automatically)
        String[] keywords = {"medical", "patient", "record"};
        Block block = api.storeSearchableData("Medical data", "password123", keywords);
        assertNotNull(block);

        // Phase 5.4: Wait for async indexing to complete
        waitForIndexing();

        // Now search should work
        List<EnhancedSearchResult> results = blockchain.getSearchSpecialistAPI().searchAll("medical", 10);
        assertTrue(results.size() > 0, "Keywords should be searchable after async indexing");
    }

    /**
     * Phase 5.4: Wait for async indexing to complete using the official method.
     * This replaces the old Thread.sleep() pattern which had race conditions.
     */
    private void waitForIndexing() {
        try {
            IndexingCoordinator.getInstance().waitForCompletion();
            logger.info("✅ Async indexing completed");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Indexing was interrupted: " + e.getMessage());
        }
    }
}
```

---

## ⚠️ Common Mistakes to Avoid

### ❌ WRONG: Using clearAll() in tests

```java
// DON'T DO THIS IN TESTS!
searchFrameworkEngine.clearAll();  // Shuts down executor service
searchSpecialistAPI.clearCache();  // (old version called clearAll())
```

**Problem:** Executor service is shut down, future searches will fail.

### ✅ CORRECT: Using clearIndexes() in tests

```java
// DO THIS INSTEAD!
searchFrameworkEngine.clearIndexes();  // Keeps executor alive
searchSpecialistAPI.clearCache();      // (new version calls clearIndexes())
```

---

### ❌ WRONG: Forgetting clearShutdownFlag()

```java
// DON'T DO THIS IN TEARDOWN!
IndexingCoordinator.getInstance().forceShutdown();
IndexingCoordinator.getInstance().disableTestMode();
// Missing: clearShutdownFlag()
```

**Problem:** `shutdownRequested=true` blocks all future indexing.

### ✅ CORRECT: Using clearShutdownFlag()

```java
// DO THIS INSTEAD!
IndexingCoordinator.getInstance().forceShutdown();
IndexingCoordinator.getInstance().clearShutdownFlag();  // ← CRITICAL!
IndexingCoordinator.getInstance().disableTestMode();
```

---

### ❌ WRONG: Enabling test mode in Phase 5.4 tests

```java
// DON'T DO THIS IN PHASE 5.4 TESTS!
@BeforeEach
void setUp() {
    IndexingCoordinator.getInstance().enableTestMode();  // Blocks async indexing!
    // ...
}
```

**Problem:** Test mode disables async indexing, tests will get 0 results.

### ✅ CORRECT: Let async indexing work normally

```java
// DO THIS INSTEAD!
@BeforeEach
void setUp() {
    // Phase 5.4 FIX: DO NOT enable test mode
    // Test mode should only be used for tests that manually control indexing

    blockchain = new Blockchain();  // Async indexing works automatically
    // ...
}
```

---

## 🔧 Troubleshooting

### Problem: Tests pass individually, fail in batch

**Symptoms:**
```bash
mvn test -Dtest=MyTest#testOne        # ✅ PASS
mvn test -Dtest=MyTest                # ❌ FAIL (0 results)
```

**Diagnosis:**
```bash
# Check logs for:
grep "shutdownRequested" target/surefire-reports/*.txt
grep "Cleared search indexes" target/surefire-reports/*.txt
```

**Solution:** Add `clearShutdownFlag()` to tearDown() (see pattern above).

---

### Problem: "Executor service is shut down" errors

**Symptoms:**
```
RejectedExecutionException: Task rejected from java.util.concurrent.ThreadPoolExecutor
```

**Diagnosis:** Someone called `clearAll()` or `shutdown()` instead of `clearIndexes()`.

**Solution:** Replace all test cleanup code:
```java
// BEFORE (BUG)
searchFrameworkEngine.clearAll();

// AFTER (FIX)
searchFrameworkEngine.clearIndexes();
```

---

### Problem: Search returns 0 results after clearAndReinitialize()

**Symptoms:**
```java
blockchain.clearAndReinitialize();
// Create blocks...
// Search returns 0 results
```

**Diagnosis:** Search indexes weren't cleared when DB was cleared.

**Solution:** Upgrade to latest `clearAndReinitialize()` that calls:
```java
searchFrameworkEngine.clearIndexes();
searchSpecialistAPI.clearCache();
```

---

### Problem: Tests pass with debug logs, fail without them

**Symptoms:**
- Tests with verbose logging: ✅ PASS
- Tests after removing logs: ❌ FAIL (0 results)
- Timing-dependent failures

**Diagnosis:** `waitForCompletion()` race condition (fixed in v1.0.6+)

**Solution:** Upgrade to latest IndexingCoordinator with `activeIndexingTasks` counter (see Fix #7 above).

---

## 📊 Testing Checklist

Before committing Phase 5.4 tests:

- [ ] `setUp()` does NOT call `enableTestMode()`
- [ ] `tearDown()` calls `waitForCompletion()` BEFORE shutdown
- [ ] `tearDown()` calls `forceShutdown()` + `clearShutdownFlag()`
- [ ] Test uses `waitForIndexing()` after creating blocks
- [ ] Test runs successfully individually: `mvn test -Dtest=MyTest#testOne`
- [ ] Test runs successfully in batch: `mvn test -Dtest=MyTest`
- [ ] No `clearAll()` calls in test code (use `clearIndexes()` instead)

---

## 🎯 Quick Reference

| Scenario | Method to Use | Why |
|----------|---------------|-----|
| Test cleanup | `clearIndexes()` | Keeps executor alive for next test |
| App shutdown | `clearAll()` or `shutdown()` | Proper resource cleanup |
| Between tests | `clearShutdownFlag()` | Re-enable indexing for next test |
| Manual indexing tests | `reset()` + `enableTestMode()` | Control indexing manually |
| Phase 5.4 async tests | `waitForCompletion()` | Wait for background indexing (uses `activeIndexingTasks` counter) |
| Debugging timing issues | Check `activeIndexingTasks.get()` | See if async tasks are still running |

---

## 📚 Related Documentation

- [TESTING.md](TESTING.md) - General testing guide
- [API_GUIDE.md](../reference/API_GUIDE.md) - SearchFrameworkEngine API
- [SEARCH_APIS_COMPARISON.md](../search/SEARCH_APIS_COMPARISON.md) - Which search API to use

---

## 🔖 Version History

- **v1.0.6 (Phase 5.4)** - Initial guide created after fixing 18 test failures
  - Added `clearIndexes()`, `clearShutdownFlag()`, `clearCache()` fixes (6 fixes)
  - Documented standard test pattern for async indexing
  - Fixed state contamination between tests
- **v1.0.6+ (Phase 5.4.1)** - Race condition fix
  - Added `activeIndexingTasks` counter to prevent premature `waitForCompletion()` returns
  - Fixed timing-dependent test failures (7th coordinated fix)
  - All 19 previously failing tests now pass reliably

---

**Last Updated:** 2025-11-29
**Status:** ✅ Complete - Race condition fixed with deterministic signal-based solution
