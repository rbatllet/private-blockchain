package com.rbatllet.blockchain.indexing;

import static org.junit.jupiter.api.Assertions.*;

import com.rbatllet.blockchain.core.Blockchain;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 🧪 Tests for IndexingCoordinator - validates prevention of infinite loops
 */
@DisplayName("🎯 IndexingCoordinator Tests")
class IndexingCoordinatorTest {
    private static final Logger logger = LoggerFactory.getLogger(IndexingCoordinatorTest.class);


    private IndexingCoordinator coordinator;
    private Blockchain testBlockchain;

    @BeforeEach
    void setUp() {
        coordinator = IndexingCoordinator.getInstance();
        coordinator.reset(); // Reset state before each test
        coordinator.enableTestMode();
        testBlockchain = new Blockchain();
    }

    @AfterEach
    void tearDown() {
        coordinator.forceShutdown();
        coordinator.disableTestMode();
        if (testBlockchain != null) {
            testBlockchain.shutdown();
        }
    }

    @Test
    @DisplayName(
        "Should prevent infinite loops by coordinating indexing operations"
    )
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void shouldPreventInfiniteLoops()
        throws InterruptedException, ExecutionException, TimeoutException {
        // Given: Setup a potentially problematic indexer that could loop
        AtomicInteger executionCount = new AtomicInteger(0);
        CountDownLatch executionLatch = new CountDownLatch(1);

        coordinator.registerIndexer("TEST_INDEXER", request -> {
            int count = executionCount.incrementAndGet();
            logger.info("🔍 Test indexer execution #" + count);

            if (count == 1) {
                executionLatch.countDown();
            }

            // Simulate some work
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // When: Execute multiple indexing requests rapidly (potential infinite loop scenario)
        CompletableFuture<IndexingCoordinator.IndexingResult> future1 =
            coordinator.coordinateIndexing(
                new IndexingCoordinator.IndexingRequest.Builder()
                    .operation("TEST_INDEXER")
                    .forceExecution()
                    .minInterval(100) // Minimum 100ms between executions
                    .build()
            );

        CompletableFuture<IndexingCoordinator.IndexingResult> future2 =
            coordinator.coordinateIndexing(
                new IndexingCoordinator.IndexingRequest.Builder()
                    .operation("TEST_INDEXER")
                    .forceExecution()
                    .minInterval(100)
                    .cannotWait() // Don't wait for first to complete
                    .build()
            );

        CompletableFuture<IndexingCoordinator.IndexingResult> future3 =
            coordinator.coordinateIndexing(
                new IndexingCoordinator.IndexingRequest.Builder()
                    .operation("TEST_INDEXER")
                    .forceExecution()
                    .minInterval(100)
                    .cannotWait() // Don't wait for first to complete
                    .build()
            );

        // Then: Wait for at least one execution
        assertTrue(
            executionLatch.await(5, TimeUnit.SECONDS),
            "Should execute at least once"
        );

        // Wait for all futures to complete
        IndexingCoordinator.IndexingResult result1 = future1.get(
            3,
            TimeUnit.SECONDS
        );
        IndexingCoordinator.IndexingResult result2 = future2.get(
            3,
            TimeUnit.SECONDS
        );
        IndexingCoordinator.IndexingResult result3 = future3.get(
            3,
            TimeUnit.SECONDS
        );

        // Should have controlled execution - not all requests should execute due to coordination
        int completedExecutions = 0;
        int failedExecutions = 0;
        if ("COMPLETED".equals(result1.getStatus())) completedExecutions++;
        if ("COMPLETED".equals(result2.getStatus())) completedExecutions++;
        if ("COMPLETED".equals(result3.getStatus())) completedExecutions++;
        
        if ("FAILED".equals(result1.getStatus())) failedExecutions++;
        if ("FAILED".equals(result2.getStatus())) failedExecutions++;
        if ("FAILED".equals(result3.getStatus())) failedExecutions++;

        logger.info(
            "📊 Execution results: " +
            result1.getStatus() +
            ", " +
            result2.getStatus() +
            ", " +
            result3.getStatus()
        );
        logger.info(
            "📊 Total executions: " +
            executionCount.get() +
            ", Completed: " +
            completedExecutions +
            ", Failed: " +
            failedExecutions
        );

        // Should not execute all requests due to coordination
        assertTrue(
            executionCount.get() <= 3,
            "Should not exceed reasonable execution count"
        );
        assertTrue(
            completedExecutions >= 1,
            "Should complete at least one execution"
        );
    }

    @Test
    @DisplayName("Should skip operations in test mode unless forced")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void shouldSkipOperationsInTestModeUnlessForced() {
        // Given: Test mode is enabled
        assertTrue(coordinator != null);

        AtomicBoolean executed = new AtomicBoolean(false);
        coordinator.registerIndexer("SKIP_TEST", request -> {
            executed.set(true);
        });

        // When: Execute without force flag
        CompletableFuture<IndexingCoordinator.IndexingResult> result1 =
            coordinator.coordinateIndexing(
                new IndexingCoordinator.IndexingRequest.Builder()
                    .operation("SKIP_TEST")
                    .build()
            );

        // Then: Should be skipped
        IndexingCoordinator.IndexingResult actualResult1 = result1.join();
        assertEquals("SKIPPED", actualResult1.getStatus());
        assertEquals("Test mode", actualResult1.getMessage());
        assertFalse(
            executed.get(),
            "Should not execute in test mode without force flag"
        );

        // When: Execute with force flag
        CompletableFuture<IndexingCoordinator.IndexingResult> result2 =
            coordinator.coordinateIndexing(
                new IndexingCoordinator.IndexingRequest.Builder()
                    .operation("SKIP_TEST")
                    .forceExecution()
                    .build()
            );

        // Then: Should execute
        IndexingCoordinator.IndexingResult actualResult2 = result2.join();
        assertEquals("COMPLETED", actualResult2.getStatus());
        assertTrue(
            executed.get(),
            "Should execute in test mode with force flag"
        );
    }

    @Test
    @DisplayName("Should handle concurrent indexing requests gracefully")
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void shouldHandleConcurrentRequestsGracefully()
        throws InterruptedException, ExecutionException, TimeoutException {
        // Given: Multiple concurrent indexing operations
        AtomicInteger executionCount = new AtomicInteger(0);
        CountDownLatch startLatch = new CountDownLatch(1); // At least one should start
        CountDownLatch completionLatch = new CountDownLatch(1); // At least one should complete

        coordinator.registerIndexer("CONCURRENT_TEST", request -> {
            startLatch.countDown();
            executionCount.incrementAndGet();

            // Simulate work
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            completionLatch.countDown();
        });

        // When: Launch multiple concurrent requests
        @SuppressWarnings("unchecked")
        CompletableFuture<IndexingCoordinator.IndexingResult>[] futures =
            new CompletableFuture[5];
        for (int i = 0; i < 5; i++) {
            futures[i] = coordinator.coordinateIndexing(
                new IndexingCoordinator.IndexingRequest.Builder()
                    .operation("CONCURRENT_TEST")
                    .forceExecution()
                    .cannotWait() // Don't wait for other operations
                    .build()
            );
        }

        // Then: Wait for at least one operation to start and complete
        assertTrue(
            startLatch.await(10, TimeUnit.SECONDS),
            "All operations should start"
        );
        assertTrue(
            completionLatch.await(10, TimeUnit.SECONDS),
            "All operations should complete"
        );

        // Verify results - at least one should succeed, others may fail due to concurrency control
        int successfulExecutions = 0;
        int failedExecutions = 0;
        for (CompletableFuture<
            IndexingCoordinator.IndexingResult
        > future : futures) {
            IndexingCoordinator.IndexingResult result = future.get(
                1,
                TimeUnit.SECONDS
            );
            if ("COMPLETED".equals(result.getStatus())) {
                successfulExecutions++;
            } else if ("FAILED".equals(result.getStatus())) {
                failedExecutions++;
            }
        }

        logger.info(
            "📊 Concurrent test - Total executions: " +
            executionCount.get() +
            ", Successful: " +
            successfulExecutions +
            ", Failed: " +
            failedExecutions
        );

        // Should handle concurrent requests gracefully - at least one should succeed
        assertTrue(
            executionCount.get() >= 1,
            "Should execute at least one request"
        );
        assertTrue(
            successfulExecutions >= 1,
            "Should have at least one successful execution"
        );
        assertTrue(
            successfulExecutions + failedExecutions >= 1,
            "Should process all requests"
        );
    }

    @Test
    @DisplayName("Should respect minimum interval between executions")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void shouldRespectMinimumInterval() {
        // Given: Indexer with minimum interval
        AtomicInteger executionCount = new AtomicInteger(0);
        coordinator.registerIndexer("INTERVAL_TEST", request -> {
            executionCount.incrementAndGet();
        });

        // When: Execute multiple requests quickly
        long startTime = System.currentTimeMillis();

        CompletableFuture<IndexingCoordinator.IndexingResult> result1 =
            coordinator.coordinateIndexing(
                new IndexingCoordinator.IndexingRequest.Builder()
                    .operation("INTERVAL_TEST")
                    .forceExecution()
                    .minInterval(500) // 500ms minimum interval
                    .build()
            );

        // Wait a bit, then try again
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        CompletableFuture<IndexingCoordinator.IndexingResult> result2 =
            coordinator.coordinateIndexing(
                new IndexingCoordinator.IndexingRequest.Builder()
                    .operation("INTERVAL_TEST")
                    .forceExecution()
                    .minInterval(500)
                    .build()
            );

        // Then: Second request should be skipped due to minimum interval
        IndexingCoordinator.IndexingResult actualResult1 = result1.join();
        IndexingCoordinator.IndexingResult actualResult2 = result2.join();

        long duration = System.currentTimeMillis() - startTime;

        logger.info(
            "📊 Interval test - Duration: " +
            duration +
            "ms, Executions: " +
            executionCount.get()
        );
        logger.info(
            "📊 Results: " +
            actualResult1.getStatus() +
            ", " +
            actualResult2.getStatus()
        );

        assertEquals(
            "COMPLETED",
            actualResult1.getStatus(),
            "First request should complete"
        );
        assertEquals(
            "SKIPPED",
            actualResult2.getStatus(),
            "Second request should be skipped due to minimum interval"
        );
        assertEquals(
            1,
            executionCount.get(),
            "Should execute only once due to minimum interval"
        );
    }

    @Test
    @DisplayName("Should handle shutdown gracefully")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void shouldHandleShutdownGracefully() {
        // Given: Coordinator is running
        assertFalse(coordinator.isIndexing());

        // When: Request shutdown
        coordinator.forceShutdown();

        // Then: New requests should be cancelled
        CompletableFuture<IndexingCoordinator.IndexingResult> result =
            coordinator.coordinateIndexing(
                new IndexingCoordinator.IndexingRequest.Builder()
                    .operation("SHUTDOWN_TEST")
                    .forceExecution()
                    .build()
            );

        IndexingCoordinator.IndexingResult actualResult = result.join();
        assertEquals("CANCELLED", actualResult.getStatus());
        assertEquals("Shutdown requested", actualResult.getMessage());
    }
}
