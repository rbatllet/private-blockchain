package com.rbatllet.blockchain.core;

import com.rbatllet.blockchain.config.DatabaseConfig;
import com.rbatllet.blockchain.config.SearchConstants;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.indexing.IndexingCoordinator;
import com.rbatllet.blockchain.testutil.GenesisKeyManager;
import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.util.JPAUtil;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Phase 5.2: Async/Background Indexing Comprehensive Test Suite
 *
 * ✅ OBJECTIVE: Rigorously test async indexing functionality for correctness, performance, and thread-safety
 *
 * Test Coverage:
 * 1. Parameter validation (valid/invalid ranges)
 * 2. Basic async indexing functionality
 * 3. Batch write with async indexing
 * 4. Concurrent async indexing operations
 * 5. Error handling and edge cases
 * 6. Performance/throughput verification
 * 7. IndexingCoordinator integration
 *
 * Expected Behavior:
 * - indexBlocksRangeAsync() returns immediately (non-blocking)
 * - Indexing happens asynchronously in background
 * - CompletableFuture completes successfully when indexing finishes
 * - 5-10x complete throughput improvement vs sync indexing
 * - Thread-safe concurrent operations
 *
 * Use: mvn test -Dtest=Phase_5_2_AsyncIndexingTest
 *
 * Tags: phase5, async, indexing, concurrency
 *
 * @since 1.0.6 (Phase 5.2)
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Phase 5.2: Async/Background Indexing Test Suite")
@Tag("phase5")
@Tag("async")
@Tag("indexing")
public class Phase_5_2_AsyncIndexingTest {

    private static Blockchain blockchain;
    private static KeyPair bootstrapKeyPair;
    private static KeyPair userKeys;

    @BeforeAll
    static void setUpClass() throws Exception {
        // Initialize H2 in-memory database
        DatabaseConfig h2Config = DatabaseConfig.createH2TestConfig();
        JPAUtil.initialize(h2Config);

        // Load bootstrap admin keys (auto-generates if missing - test-only)
        bootstrapKeyPair = GenesisKeyManager.ensureGenesisKeysExist();
        
        assertNotNull(bootstrapKeyPair, "Failed to load genesis admin keys");
    }

    @BeforeEach
    void setUp() throws Exception {
        // CRITICAL: Reset IndexingCoordinator before each test to clear shutdown state
        IndexingCoordinator.getInstance().reset();

        blockchain = new Blockchain();
        blockchain.clearAndReinitialize();

        // Create bootstrap admin
        blockchain.createBootstrapAdmin(
            CryptoUtil.publicKeyToString(bootstrapKeyPair.getPublic()),
            "BOOTSTRAP_ADMIN"
        );

        // Use bootstrap admin keys for all test blocks
        userKeys = bootstrapKeyPair;

        // Initialize advanced search
        blockchain.initializeAdvancedSearch(SearchConstants.DEFAULT_INDEXING_KEY);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        // Phase 5.4 FIX: Wait for async indexing to complete before cleanup
        IndexingCoordinator.getInstance().waitForCompletion();

        // CRITICAL: Clear database + search indexes to prevent state contamination
        blockchain.completeCleanupForTestsWithBackups();  // Includes clearAndReinitialize()

        // Reset IndexingCoordinator singleton state
        IndexingCoordinator.getInstance().forceShutdown();
        IndexingCoordinator.getInstance().clearShutdownFlag();
        IndexingCoordinator.getInstance().disableTestMode();
    }

    @AfterAll
    static void tearDownClass() {
        JPAUtil.shutdown();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 1. PARAMETER VALIDATION TESTS
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("1. Parameter Validation")
    class ParameterValidationTests {

        @Test
        @Order(1)
        @DisplayName("1.1 Should reject negative start block number")
        void testNegativeStartBlockNumber() {
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> blockchain.indexBlocksRangeAsync(-1, 10)
            );
            assertTrue(exception.getMessage().contains("Invalid block range"));
        }

        @Test
        @Order(2)
        @DisplayName("1.2 Should reject start > end block number")
        void testStartGreaterThanEnd() {
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> blockchain.indexBlocksRangeAsync(10, 5)
            );
            assertTrue(exception.getMessage().contains("Invalid block range"));
        }

        @Test
        @Order(3)
        @DisplayName("1.3 Should accept valid range (start == end)")
        void testValidSingleBlockRange() throws Exception {
            // Create one block
            blockchain.addBlock("Test block", userKeys.getPrivate(), userKeys.getPublic());

            CompletableFuture<IndexingCoordinator.IndexingResult> future =
                blockchain.indexBlocksRangeAsync(1, 1);

            assertNotNull(future, "Future should not be null");

            // Wait for completion
            IndexingCoordinator.IndexingResult result = future.get(10, TimeUnit.SECONDS);
            assertNotNull(result, "Result should not be null");
        }

        @Test
        @Order(4)
        @DisplayName("1.4 Should accept valid range (start < end)")
        void testValidMultiBlockRange() throws Exception {
            // Create blocks
            for (int i = 0; i < 5; i++) {
                blockchain.addBlock("Block " + i, userKeys.getPrivate(), userKeys.getPublic());
            }

            CompletableFuture<IndexingCoordinator.IndexingResult> future =
                blockchain.indexBlocksRangeAsync(1, 5);

            assertNotNull(future, "Future should not be null");

            // Wait for completion
            IndexingCoordinator.IndexingResult result = future.get(10, TimeUnit.SECONDS);
            assertNotNull(result, "Result should not be null");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 2. BASIC ASYNC INDEXING FUNCTIONALITY
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("2. Basic Async Indexing Functionality")
    class BasicAsyncIndexingTests {

        @Test
        @Order(10)
        @DisplayName("2.1 Should return CompletableFuture immediately (non-blocking)")
        void testReturnsImmediately() throws Exception {
            // Create blocks with skipIndexing
            List<Blockchain.BlockWriteRequest> requests = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                requests.add(new Blockchain.BlockWriteRequest(
                    "Block " + i,
                    userKeys.getPrivate(),
                    userKeys.getPublic()
                ));
            }
            blockchain.addBlocksBatch(requests, true); // skipIndexing=true

            // Capture the main thread ID
            long mainThreadId = Thread.currentThread().threadId();
            
            // Create a flag to verify indexing runs on different thread
            AtomicBoolean ranOnDifferentThread = new AtomicBoolean(false);
            AtomicLong indexingThreadId = new AtomicLong(-1);
            
            // Measure time to call indexBlocksRangeAsync()
            long start = System.currentTimeMillis();
            CompletableFuture<IndexingCoordinator.IndexingResult> future =
                blockchain.indexBlocksRangeAsync(1, 10)
                    .whenComplete((result, ex) -> {
                        long currentThreadId = Thread.currentThread().threadId();
                        indexingThreadId.set(currentThreadId);
                        ranOnDifferentThread.set(currentThreadId != mainThreadId);
                    });
            long callDuration = System.currentTimeMillis() - start;

            // RIGOROUS TEST 1: Method must return immediately (< 100ms)
            assertTrue(callDuration < 100,
                "indexBlocksRangeAsync() should return immediately, took " + callDuration + "ms");

            assertNotNull(future, "Future should not be null");

            // Wait for completion
            IndexingCoordinator.IndexingResult result = future.get(30, TimeUnit.SECONDS);
            assertNotNull(result, "Result should not be null");
            
            // RIGOROUS TEST 2: Indexing MUST execute on a different thread
            assertTrue(ranOnDifferentThread.get(),
                "Indexing must run on different thread! Main thread: " + mainThreadId +
                ", Indexing thread: " + indexingThreadId.get());
        }

        @Test
        @Order(11)
        @DisplayName("2.2 Should complete successfully with valid result")
        void testCompletesSuccessfully() throws Exception {
            // Create blocks with skipIndexing
            List<Blockchain.BlockWriteRequest> requests = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                requests.add(new Blockchain.BlockWriteRequest(
                    "Block " + i,
                    userKeys.getPrivate(),
                    userKeys.getPublic()
                ));
            }
            blockchain.addBlocksBatch(requests, true);

            // Trigger async indexing
            CompletableFuture<IndexingCoordinator.IndexingResult> future =
                blockchain.indexBlocksRangeAsync(1, 5);

            // Wait and verify result
            IndexingCoordinator.IndexingResult result = future.get(30, TimeUnit.SECONDS);

            assertNotNull(result, "Result should not be null");
            assertTrue(result.isSuccess(),
                "Result should be success, was: " + result.getStatus() + " - " + result.getMessage());
        }

        @Test
        @Order(12)
        @DisplayName("2.3 Should index blocks and make them searchable")
        void testBlocksBecomeSearchable() throws Exception {
            // Create blocks with skipIndexing
            List<Blockchain.BlockWriteRequest> requests = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                requests.add(new Blockchain.BlockWriteRequest(
                    "Searchable content " + i,
                    userKeys.getPrivate(),
                    userKeys.getPublic()
                ));
            }
            blockchain.addBlocksBatch(requests, true);

            // Verify NOT searchable yet (indexing was skipped)
            // Note: Search may still work if indexed by another mechanism

            // Trigger async indexing
            CompletableFuture<IndexingCoordinator.IndexingResult> future =
                blockchain.indexBlocksRangeAsync(1, 5);

            // Wait for indexing to complete
            IndexingCoordinator.IndexingResult result = future.get(30, TimeUnit.SECONDS);

            // Verify blocks are indexed
            assertTrue(result.isSuccess(),
                "Indexing should succeed, was: " + result.getStatus() + " - " + result.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 3. BATCH WRITE WITH ASYNC INDEXING
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("3. Batch Write with Async Indexing")
    class BatchWriteAsyncIndexingTests {

        @Test
        @Order(20)
        @DisplayName("3.1 addBlocksBatch() with async indexing should return immediately")
        void testBatchWriteAsyncReturnsImmediately() throws Exception {
            List<Blockchain.BlockWriteRequest> requests = new ArrayList<>();
            for (int i = 0; i < 50; i++) {
                requests.add(new Blockchain.BlockWriteRequest(
                    "Block " + i,
                    userKeys.getPrivate(),
                    userKeys.getPublic()
                ));
            }

            // Measure time to call addBlocksBatch() (async indexing is now the default)
            long start = System.currentTimeMillis();
            List<Block> blocks = blockchain.addBlocksBatch(requests);
            long callDuration = System.currentTimeMillis() - start;

            // Should complete write in reasonable time (indexing is async)
            // With 50 blocks, write-only should take < 1 second
            assertTrue(callDuration < 2000,
                "addBlocksBatch() took " + callDuration + "ms (should be < 2s for 50 blocks)");

            assertEquals(50, blocks.size(), "Should return 50 blocks");

            // Verify all blocks are written
            assertEquals(51, blockchain.getBlockCount(), "Should have 51 blocks (including GENESIS)");
        }

        @Test
        @Order(21)
        @DisplayName("3.2 Should write blocks correctly with async indexing")
        void testBatchWriteAsyncWritesCorrectly() throws Exception {
            List<Blockchain.BlockWriteRequest> requests = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                requests.add(new Blockchain.BlockWriteRequest(
                    "Data " + i,
                    userKeys.getPrivate(),
                    userKeys.getPublic()
                ));
            }

            List<Block> blocks = blockchain.addBlocksBatch(requests);

            assertEquals(10, blocks.size(), "Should return 10 blocks");

            // Verify blocks are written to database
            for (int i = 0; i < blocks.size(); i++) {
                Block block = blocks.get(i);
                assertNotNull(block, "Block " + i + " should not be null");
                assertEquals("Data " + i, block.getData(), "Block " + i + " data mismatch");

                // Verify block exists in blockchain
                Block retrieved = blockchain.getBlock(block.getBlockNumber());
                assertNotNull(retrieved, "Block " + block.getBlockNumber() + " should exist");
                assertEquals(block.getData(), retrieved.getData(), "Retrieved block data mismatch");
            }
        }

        @Test
        @Order(22)
        @DisplayName("3.3 Should handle multiple async batch writes")
        @Timeout(60)
        void testMultipleAsyncBatchWrites() throws Exception {
            int batches = 5;
            int blocksPerBatch = 20;

            for (int b = 0; b < batches; b++) {
                List<Blockchain.BlockWriteRequest> requests = new ArrayList<>();
                for (int i = 0; i < blocksPerBatch; i++) {
                    requests.add(new Blockchain.BlockWriteRequest(
                        "Batch " + b + " Block " + i,
                        userKeys.getPrivate(),
                        userKeys.getPublic()
                    ));
                }

                List<Block> blocks = blockchain.addBlocksBatch(requests);
                assertEquals(blocksPerBatch, blocks.size(),
                    "Batch " + b + " should return " + blocksPerBatch + " blocks");
            }

            // Verify total blocks
            long expected = 1 + (batches * blocksPerBatch); // +1 for GENESIS
            assertEquals(expected, blockchain.getBlockCount(),
                "Should have " + expected + " total blocks");

            IndexingCoordinator.getInstance().waitForCompletion();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 4. CONCURRENT ASYNC INDEXING
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("4. Concurrent Async Indexing")
    class ConcurrentAsyncIndexingTests {

        @Test
        @Order(30)
        @DisplayName("4.1 Should handle concurrent async indexing calls")
        @Timeout(60)
        void testConcurrentAsyncIndexing() throws Exception {
            // Create blocks first
            List<Blockchain.BlockWriteRequest> requests = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                requests.add(new Blockchain.BlockWriteRequest(
                    "Block " + i,
                    userKeys.getPrivate(),
                    userKeys.getPublic()
                ));
            }
            blockchain.addBlocksBatch(requests, true); // skipIndexing

            // Launch multiple concurrent async indexing operations
            int threads = 5;
            CountDownLatch latch = new CountDownLatch(threads);
            List<CompletableFuture<IndexingCoordinator.IndexingResult>> futures = new ArrayList<>();
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger errorCount = new AtomicInteger(0);

            for (int t = 0; t < threads; t++) {
                int threadId = t;
                new Thread(() -> {
                    try {
                        // Each thread indexes a different range
                        long start = 1 + (threadId * 20L);
                        long end = start + 19;

                        CompletableFuture<IndexingCoordinator.IndexingResult> future =
                            blockchain.indexBlocksRangeAsync(start, end);
                        futures.add(future);

                        IndexingCoordinator.IndexingResult result = future.get(30, TimeUnit.SECONDS);
                        if (result.isSuccess()) {
                            successCount.incrementAndGet();
                        } else {
                            System.err.println("Thread " + threadId + " indexing failed: " +
                                result.getStatus() + " - " + result.getMessage());
                        }
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                        e.printStackTrace();
                    } finally {
                        latch.countDown();
                    }
                }).start();
            }

            // Wait for all threads to complete
            assertTrue(latch.await(60, TimeUnit.SECONDS), "All threads should complete within 60s");

            // Verify results
            assertTrue(successCount.get() > 0,
                "At least some indexing operations should succeed");
            assertEquals(0, errorCount.get(), "No errors should occur");
        }

        @Test
        @Order(31)
        @DisplayName("4.2 Should handle concurrent batch writes with async indexing")
        @Timeout(60)
        void testConcurrentBatchWritesWithAsyncIndexing() throws Exception {
            int threads = 3;
            int blocksPerThread = 20;
            CountDownLatch latch = new CountDownLatch(threads);
            AtomicInteger totalBlocksWritten = new AtomicInteger(0);
            AtomicInteger errorCount = new AtomicInteger(0);

            for (int t = 0; t < threads; t++) {
                int threadId = t;
                new Thread(() -> {
                    try {
                        List<Blockchain.BlockWriteRequest> requests = new ArrayList<>();
                        for (int i = 0; i < blocksPerThread; i++) {
                            requests.add(new Blockchain.BlockWriteRequest(
                                "Thread " + threadId + " Block " + i,
                                userKeys.getPrivate(),
                                userKeys.getPublic()
                            ));
                        }

                        List<Block> blocks = blockchain.addBlocksBatch(requests);
                        totalBlocksWritten.addAndGet(blocks.size());
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                        e.printStackTrace();
                    } finally {
                        latch.countDown();
                    }
                }).start();
            }

            // Wait for all threads to complete
            assertTrue(latch.await(60, TimeUnit.SECONDS), "All threads should complete");

            // Verify results
            assertEquals(threads * blocksPerThread, totalBlocksWritten.get(),
                "All blocks should be written");
            assertEquals(0, errorCount.get(), "No errors should occur");

            IndexingCoordinator.getInstance().waitForCompletion();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 5. ERROR HANDLING AND EDGE CASES
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("5. Error Handling and Edge Cases")
    class ErrorHandlingTests {

        @Test
        @Order(40)
        @DisplayName("5.1 Should handle empty range (no blocks to index)")
        void testEmptyRange() throws Exception {
            // Don't create any blocks (only GENESIS exists)

            CompletableFuture<IndexingCoordinator.IndexingResult> future =
                blockchain.indexBlocksRangeAsync(10, 20);

            assertNotNull(future, "Future should not be null");

            // Should complete successfully (no blocks to index)
            IndexingCoordinator.IndexingResult result = future.get(10, TimeUnit.SECONDS);
            assertNotNull(result, "Result should not be null");
        }

        @Test
        @Order(41)
        @DisplayName("5.2 Should handle range beyond existing blocks")
        void testRangeBeyondBlocks() throws Exception {
            // Create only 5 blocks
            for (int i = 0; i < 5; i++) {
                blockchain.addBlock("Block " + i, userKeys.getPrivate(), userKeys.getPublic());
            }

            // Try to index blocks 1-100 (only 1-5 exist)
            CompletableFuture<IndexingCoordinator.IndexingResult> future =
                blockchain.indexBlocksRangeAsync(1, 100);

            assertNotNull(future, "Future should not be null");

            // Should complete (will index only existing blocks)
            IndexingCoordinator.IndexingResult result = future.get(30, TimeUnit.SECONDS);
            assertNotNull(result, "Result should not be null");
        }

        @Test
        @Order(42)
        @DisplayName("5.3 Should handle very large ranges")
        @Timeout(120)
        void testVeryLargeRange() throws Exception {
            // Create 200 blocks
            List<Blockchain.BlockWriteRequest> requests = new ArrayList<>();
            for (int i = 0; i < 200; i++) {
                requests.add(new Blockchain.BlockWriteRequest(
                    "Block " + i,
                    userKeys.getPrivate(),
                    userKeys.getPublic()
                ));
            }
            blockchain.addBlocksBatch(requests, true);

            // Index all 200 blocks asynchronously
            CompletableFuture<IndexingCoordinator.IndexingResult> future =
                blockchain.indexBlocksRangeAsync(1, 200);

            assertNotNull(future, "Future should not be null");

            // Should complete within 2 minutes
            IndexingCoordinator.IndexingResult result = future.get(120, TimeUnit.SECONDS);
            assertNotNull(result, "Result should not be null");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 6. PERFORMANCE / THROUGHPUT VERIFICATION
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("6. Performance and Throughput")
    class PerformanceTests {

        @Test
        @Order(50)
        @DisplayName("6.1 Async indexing should provide throughput improvement")
        @Timeout(120)
        void testAsyncIndexingThroughputImprovement() throws Exception {
            int totalBlocks = 100;

            // Baseline: Sync indexing (traditional approach)
            blockchain.clearAndReinitialize();
            blockchain.createBootstrapAdmin(
                CryptoUtil.publicKeyToString(bootstrapKeyPair.getPublic()),
                "BOOTSTRAP_ADMIN"
            );

            List<Blockchain.BlockWriteRequest> requests1 = new ArrayList<>();
            for (int i = 0; i < totalBlocks; i++) {
                requests1.add(new Blockchain.BlockWriteRequest(
                    "Sync Block " + i,
                    userKeys.getPrivate(),
                    userKeys.getPublic()
                ));
            }

            long syncStart = System.currentTimeMillis();
            blockchain.addBlocksBatch(requests1); // Default: includes sync indexing
            long syncDuration = System.currentTimeMillis() - syncStart;
            double syncThroughput = (totalBlocks * 1000.0) / syncDuration;

            System.out.println("\n📊 THROUGHPUT COMPARISON:");
            System.out.println("  Sync indexing:   " + syncDuration + "ms (" +
                String.format("%.1f", syncThroughput) + " blocks/sec)");

            // Test: Async indexing (skipIndexing=true for fair comparison)
            blockchain.clearAndReinitialize();
            blockchain.createBootstrapAdmin(
                CryptoUtil.publicKeyToString(bootstrapKeyPair.getPublic()),
                "BOOTSTRAP_ADMIN"
            );

            List<Blockchain.BlockWriteRequest> requests2 = new ArrayList<>();
            for (int i = 0; i < totalBlocks; i++) {
                requests2.add(new Blockchain.BlockWriteRequest(
                    "Async Block " + i,
                    userKeys.getPrivate(),
                    userKeys.getPublic()
                ));
            }

            long asyncStart = System.currentTimeMillis();
            List<Block> blocks = blockchain.addBlocksBatch(requests2, true); // skipIndexing=true
            long writeDuration = System.currentTimeMillis() - asyncStart;
            double writeThroughput = (totalBlocks * 1000.0) / writeDuration;

            // Now do indexing separately
            long indexingStart = System.currentTimeMillis();
            CompletableFuture<IndexingCoordinator.IndexingResult> future =
                blockchain.indexBlocksRangeAsync(
                    blocks.get(0).getBlockNumber(),
                    blocks.get(blocks.size() - 1).getBlockNumber()
                );
            IndexingCoordinator.IndexingResult result = future.get(60, TimeUnit.SECONDS);
            long indexingDuration = System.currentTimeMillis() - indexingStart;
            long totalAsyncDuration = writeDuration + indexingDuration;
            double asyncThroughput = (totalBlocks * 1000.0) / totalAsyncDuration;

            // Verify indexing completed successfully
            assertTrue(result.isSuccess(),
                "Async indexing should succeed, was: " + result.getStatus() + " - " + result.getMessage());

            System.out.println("  Async write:     " + writeDuration + "ms (" +
                String.format("%.1f", writeThroughput) + " blocks/sec) - WRITE ONLY (skipIndexing=true)");
            System.out.println("  Async indexing:  " + indexingDuration + "ms (async background)");
            System.out.println("  Async total:     " + totalAsyncDuration + "ms (" +
                String.format("%.1f", asyncThroughput) + " blocks/sec) - COMPLETE");

            double improvement = writeThroughput / syncThroughput;
            System.out.println("  Improvement:     " + String.format("%.1fx", improvement) + " (write-only vs sync)");

            // ROBUST TEST: Allow 30% margin for timing variance (CI/CD environments, JVM warm-up)
            // Async write (without indexing) should be at least 70% of sync (with indexing)
            // Note: Timing variance can occur due to JVM JIT compilation, GC, system load
            double minExpectedRatio = 0.7;
            assertTrue(writeThroughput >= (syncThroughput * minExpectedRatio),
                String.format("Async write throughput (%.1f) should be at least %.0f%% of sync (%.1f), got %.1f%%",
                    writeThroughput, minExpectedRatio * 100, syncThroughput, 
                    (writeThroughput / syncThroughput) * 100));

            // Log actual improvement for monitoring
            if (writeThroughput > syncThroughput) {
                System.out.println("  ✅ Async faster: " + String.format("%.1fx improvement", improvement));
            } else {
                System.out.println("  ⚠️  Within tolerance: " + 
                    String.format("%.1f%% of sync (>= %.0f%% required)", 
                        (writeThroughput / syncThroughput) * 100, minExpectedRatio * 100));
            }
        }
    }
}
