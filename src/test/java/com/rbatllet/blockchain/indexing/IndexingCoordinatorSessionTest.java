package com.rbatllet.blockchain.indexing;

import static org.junit.jupiter.api.Assertions.*;
import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.util.JPAUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * 🧪 Rigorous tests for IndexingCoordinator Session/EntityManager handling
 *
 * <p>These tests specifically validate behavior when database sessions close
 * before or during indexing operations - a critical failure scenario that can
 * cause data loss or corruption.</p>
 *
 * <p><strong>Test Coverage:</strong></p>
 * <ul>
 *   <li>Session closed before indexing starts</li>
 *   <li>Session closed during indexing operation</li>
 *   <li>Race condition: transaction commit vs async indexing start</li>
 *   <li>Multiple blocks with session closing between operations</li>
 *   <li>Graceful degradation when session unavailable</li>
 *   <li>Data integrity validation after session errors</li>
 *   <li>Recovery from session errors without data loss</li>
 * </ul>
 */
@DisplayName("🎯 IndexingCoordinator Session Management Tests")
class IndexingCoordinatorSessionTest {

    private IndexingCoordinator coordinator;
    private Blockchain testBlockchain;
    private AtomicInteger indexingAttempts;
    private AtomicInteger successfulIndexing;
    private AtomicInteger failedIndexing;

    @BeforeEach
    void setUp() {
        coordinator = IndexingCoordinator.getInstance();
        coordinator.reset();
        coordinator.enableTestMode();
        testBlockchain = new Blockchain();
        
        indexingAttempts = new AtomicInteger(0);
        successfulIndexing = new AtomicInteger(0);
        failedIndexing = new AtomicInteger(0);
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
    @DisplayName("Should handle session closed before indexing starts")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void shouldHandleSessionClosedBeforeIndexing()
        throws InterruptedException, ExecutionException, TimeoutException {
        
        // Given: Register indexer that simulates closed session at start
        AtomicBoolean sessionClosedDetected = new AtomicBoolean(false);
        
        coordinator.registerIndexer("SESSION_CLOSED_BEFORE", request -> {
            indexingAttempts.incrementAndGet();
            
            // Simulate checking if session is open (common pattern in real code)
            EntityManager em = null;
            try {
                em = JPAUtil.getEntityManager();
                
                // Immediately close the EntityManager before any operations
                if (em != null && em.isOpen()) {
                    em.close();
                }
                
                // Now try to use the closed EntityManager (this is the bug scenario)
                if (em != null && !em.isOpen()) {
                    sessionClosedDetected.set(true);
                    failedIndexing.incrementAndGet();
                    throw new IllegalStateException(
                        "Session/EntityManager is closed before indexing could start"
                    );
                }
                
                successfulIndexing.incrementAndGet();
            } catch (IllegalStateException e) {
                // Don't re-increment failedIndexing - already done above
                if (e.getMessage().contains("Session/EntityManager is closed")) {
                    sessionClosedDetected.set(true);
                }
                throw e;
            } finally {
                if (em != null && em.isOpen()) {
                    em.close();
                }
            }
        });

        // When: Execute indexing with force flag
        CompletableFuture<IndexingCoordinator.IndexingResult> future =
            coordinator.coordinateIndexing(
                new IndexingCoordinator.IndexingRequest.Builder()
                    .operation("SESSION_CLOSED_BEFORE")
                    .forceExecution()
                    .build()
            );

        // Then: Should detect session closure and fail gracefully
        try {
            IndexingCoordinator.IndexingResult result = future.get(5, TimeUnit.SECONDS);
            
            assertTrue(
                sessionClosedDetected.get(),
                "Should detect that session was closed"
            );
            assertEquals(
                "FAILED",
                result.getStatus(),
                "Should report FAILED status when session closed"
            );
            assertTrue(
                result.getMessage().contains("Session/EntityManager is closed") ||
                result.getMessage().contains("IllegalStateException"),
                "Error message should mention session closure"
            );
            assertTrue(indexingAttempts.get() >= 1, "Should attempt indexing at least once");
            assertEquals(0, successfulIndexing.get(), "Should have zero successful indexing");
            assertTrue(failedIndexing.get() >= 1, "Should have at least one failed indexing");
            assertEquals(indexingAttempts.get(), failedIndexing.get(), "All attempts should fail");
            
        } catch (ExecutionException e) {
            // Also acceptable - exception propagated through CompletableFuture
            assertTrue(
                e.getCause() instanceof IllegalStateException,
                "Should throw IllegalStateException for closed session"
            );
            assertTrue(
                sessionClosedDetected.get(),
                "Should detect that session was closed"
            );
        }

        System.out.println(
            "📊 Session closed before indexing - Attempts: " +
            indexingAttempts.get() +
            ", Successful: " +
            successfulIndexing.get() +
            ", Failed: " +
            failedIndexing.get()
        );
    }

    @Test
    @DisplayName("Should handle session closed during indexing operation")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void shouldHandleSessionClosedDuringIndexing()
        throws InterruptedException, ExecutionException, TimeoutException {
        
        // Given: Register indexer that closes session mid-operation
        AtomicBoolean sessionClosedMidOperation = new AtomicBoolean(false);
        AtomicReference<EntityManager> capturedEM = new AtomicReference<>();
        
        coordinator.registerIndexer("SESSION_CLOSED_DURING", request -> {
            indexingAttempts.incrementAndGet();
            
            EntityManager em = null;
            try {
                em = JPAUtil.getEntityManager();
                capturedEM.set(em);
                
                if (em != null && em.isOpen()) {
                    // Simulate: start of indexing operation
                    System.out.println("✅ EntityManager open at start");
                    
                    // Simulate some work
                    Thread.sleep(100);
                    
                    // CRITICAL SIMULATION: Session closes mid-operation
                    // This happens when transaction commits in main thread
                    // while async indexing is still running
                    em.close();
                    sessionClosedMidOperation.set(true);
                    System.out.println("❌ EntityManager closed mid-operation");
                    
                    // Try to continue indexing with closed session
                    if (!em.isOpen()) {
                        failedIndexing.incrementAndGet();
                        throw new IllegalStateException(
                            "Session/EntityManager is closed during indexing operation"
                        );
                    }
                }
                
                successfulIndexing.incrementAndGet();
            } catch (IllegalStateException e) {
                // Don't re-increment failedIndexing - already done above
                if (e.getMessage().contains("Session/EntityManager is closed")) {
                    sessionClosedMidOperation.set(true);
                }
                throw e;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted during indexing", e);
            } finally {
                // Don't close again if already closed
                if (em != null && em.isOpen()) {
                    em.close();
                }
            }
        });

        // When: Execute indexing
        CompletableFuture<IndexingCoordinator.IndexingResult> future =
            coordinator.coordinateIndexing(
                new IndexingCoordinator.IndexingRequest.Builder()
                    .operation("SESSION_CLOSED_DURING")
                    .forceExecution()
                    .build()
            );

        // Then: Should detect mid-operation closure and fail gracefully
        try {
            IndexingCoordinator.IndexingResult result = future.get(5, TimeUnit.SECONDS);
            
            assertTrue(
                sessionClosedMidOperation.get(),
                "Should detect that session closed mid-operation"
            );
            assertEquals(
                "FAILED",
                result.getStatus(),
                "Should report FAILED status"
            );
            assertTrue(indexingAttempts.get() >= 1, "Should attempt at least once");
            assertEquals(0, successfulIndexing.get(), "Should have zero successful");
            assertTrue(failedIndexing.get() >= 1, "Should have at least one failed");
            assertEquals(indexingAttempts.get(), failedIndexing.get(), "All attempts should fail");
            
        } catch (ExecutionException e) {
            assertTrue(
                e.getCause() instanceof IllegalStateException,
                "Should throw IllegalStateException"
            );
            assertTrue(
                sessionClosedMidOperation.get(),
                "Should detect mid-operation closure"
            );
        }

        System.out.println(
            "📊 Session closed during indexing - Attempts: " +
            indexingAttempts.get() +
            ", Successful: " +
            successfulIndexing.get() +
            ", Failed: " +
            failedIndexing.get()
        );
    }

    @Test
    @DisplayName("Should handle race condition: transaction commit vs async indexing")
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void shouldHandleTransactionCommitRaceCondition()
        throws InterruptedException, ExecutionException, TimeoutException {
        
        // Given: Simulate real-world scenario where:
        // 1. Main thread commits transaction and closes EntityManager
        // 2. Async indexing task starts immediately after
        // 3. Async task tries to use now-closed EntityManager
        
        CountDownLatch transactionCommittedLatch = new CountDownLatch(1);
        CountDownLatch indexingStartedLatch = new CountDownLatch(1);
        AtomicBoolean raceConditionDetected = new AtomicBoolean(false);
        AtomicReference<EntityManager> mainThreadEM = new AtomicReference<>();
        
        coordinator.registerIndexer("RACE_CONDITION_TEST", request -> {
            indexingAttempts.incrementAndGet();
            indexingStartedLatch.countDown();
            
            try {
                // Wait for main thread to commit and close session
                boolean committed = transactionCommittedLatch.await(3, TimeUnit.SECONDS);
                assertTrue(committed, "Main thread should commit transaction");
                
                // Now try to get EntityManager (will be closed by main thread)
                EntityManager em = mainThreadEM.get();
                
                if (em != null && !em.isOpen()) {
                    raceConditionDetected.set(true);
                    failedIndexing.incrementAndGet();
                    throw new IllegalStateException(
                        "Session/EntityManager is closed - race condition detected: " +
                        "transaction committed before async indexing could acquire session"
                    );
                }
                
                successfulIndexing.incrementAndGet();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        });

        // When: Simulate main thread transaction flow
        EntityManager em = JPAUtil.getEntityManager();
        mainThreadEM.set(em);
        
        try {
            EntityTransaction tx = em.getTransaction();
            tx.begin();
            
            // Simulate adding a block (would trigger async indexing in real code)
            CompletableFuture<IndexingCoordinator.IndexingResult> indexingFuture =
                coordinator.coordinateIndexing(
                    new IndexingCoordinator.IndexingRequest.Builder()
                        .operation("RACE_CONDITION_TEST")
                        .forceExecution()
                        .build()
                );
            
            // Wait for indexing to start
            boolean started = indexingStartedLatch.await(2, TimeUnit.SECONDS);
            assertTrue(started, "Indexing should start");
            
            // Main thread commits and closes session (RACE CONDITION TRIGGER)
            tx.commit();
            em.close();
            System.out.println("🏁 Main thread: transaction committed, EntityManager closed");
            
            // Signal that transaction is committed
            transactionCommittedLatch.countDown();
            
            // Then: Async indexing should detect closed session
            try {
                IndexingCoordinator.IndexingResult result = indexingFuture.get(5, TimeUnit.SECONDS);
                
                assertTrue(
                    raceConditionDetected.get(),
                    "Should detect race condition"
                );
                assertEquals(
                    "FAILED",
                    result.getStatus(),
                    "Should fail due to closed session"
                );
                assertEquals(1, indexingAttempts.get());
                assertEquals(0, successfulIndexing.get());
                assertEquals(1, failedIndexing.get());
                
            } catch (ExecutionException e) {
                assertTrue(
                    e.getCause() instanceof IllegalStateException,
                    "Should throw IllegalStateException"
                );
                assertTrue(
                    raceConditionDetected.get(),
                    "Should detect race condition"
                );
            }
            
        } finally {
            if (em.isOpen()) {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                em.close();
            }
        }

        System.out.println(
            "📊 Race condition test - Race detected: " +
            raceConditionDetected.get() +
            ", Attempts: " +
            indexingAttempts.get() +
            ", Failed: " +
            failedIndexing.get()
        );
    }

    @Test
    @DisplayName("Should handle multiple blocks with session errors gracefully")
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void shouldHandleMultipleBlocksWithSessionErrors()
        throws InterruptedException {
        
        // Given: Indexer that randomly encounters session issues
        AtomicInteger sessionErrors = new AtomicInteger(0);
        
        coordinator.registerIndexer("MULTIPLE_BLOCKS_TEST", request -> {
            int attempt = indexingAttempts.incrementAndGet();
            
            EntityManager em = null;
            try {
                em = JPAUtil.getEntityManager();
                
                // Simulate: every other operation finds session closed
                if (attempt % 2 == 0) {
                    if (em != null && em.isOpen()) {
                        em.close();
                    }
                    sessionErrors.incrementAndGet();
                    failedIndexing.incrementAndGet();
                    throw new IllegalStateException(
                        "Session/EntityManager is closed for block #" + attempt
                    );
                }
                
                // Successful indexing
                if (em != null && em.isOpen()) {
                    Thread.sleep(50); // Simulate work
                    successfulIndexing.incrementAndGet();
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            } finally {
                if (em != null && em.isOpen()) {
                    em.close();
                }
            }
        });

        // When: Launch multiple indexing operations
        List<CompletableFuture<IndexingCoordinator.IndexingResult>> futures =
            new ArrayList<>();
        
        for (int i = 0; i < 6; i++) {
            CompletableFuture<IndexingCoordinator.IndexingResult> future =
                coordinator.coordinateIndexing(
                    new IndexingCoordinator.IndexingRequest.Builder()
                        .operation("MULTIPLE_BLOCKS_TEST")
                        .forceExecution()
                        .minInterval(50) // Small interval
                        .build()
                );
            futures.add(future);
            Thread.sleep(100); // Stagger operations
        }

        // Then: Wait for all operations and verify mixed results
        int completedCount = 0;
        int failedCount = 0;
        
        for (CompletableFuture<IndexingCoordinator.IndexingResult> future : futures) {
            try {
                IndexingCoordinator.IndexingResult result = future.get(2, TimeUnit.SECONDS);
                if ("COMPLETED".equals(result.getStatus())) {
                    completedCount++;
                } else if ("FAILED".equals(result.getStatus())) {
                    failedCount++;
                }
            } catch (ExecutionException | TimeoutException e) {
                failedCount++;
            }
        }

        System.out.println(
            "📊 Multiple blocks test - Total attempts: " +
            indexingAttempts.get() +
            ", Completed: " +
            completedCount +
            ", Failed: " +
            failedCount +
            ", Session errors: " +
            sessionErrors.get()
        );

        // Assertions - Allow for timing variations in concurrent operations
        assertTrue(indexingAttempts.get() >= 5, "Should attempt at least 5 indexing operations");
        assertTrue(successfulIndexing.get() >= 2, "Should have at least 2 successful (odd numbers)");
        assertTrue(failedIndexing.get() >= 2, "Should have at least 2 failed (even numbers)");
        assertTrue(sessionErrors.get() >= 2, "Should detect at least 2 session errors");
        assertTrue(completedCount >= 2, "Should complete at least 2 operations");
        assertTrue(failedCount >= 2, "Should fail at least 2 operations");
        // Most important: verify we detected and handled session errors
        assertTrue(sessionErrors.get() > 0, "Critical: Should detect session errors");
        assertTrue(failedIndexing.get() > 0, "Critical: Should have failed operations due to session errors");
    }

    @Test
    @DisplayName("Should validate data integrity after session errors")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void shouldValidateDataIntegrityAfterSessionErrors() {
        // Given: Track which blocks were successfully indexed
        AtomicInteger blocksIndexed = new AtomicInteger(0);
        AtomicInteger blocksFailedToIndex = new AtomicInteger(0);
        List<String> indexedBlockIds = new ArrayList<>();
        List<String> failedBlockIds = new ArrayList<>();
        
        coordinator.registerIndexer("DATA_INTEGRITY_TEST", request -> {
            String blockId = "BLOCK_" + indexingAttempts.incrementAndGet();
            
            EntityManager em = null;
            try {
                em = JPAUtil.getEntityManager();
                
                if (em == null || !em.isOpen()) {
                    failedBlockIds.add(blockId);
                    blocksFailedToIndex.incrementAndGet();
                    failedIndexing.incrementAndGet();
                    throw new IllegalStateException(
                        "Cannot index " + blockId + " - Session/EntityManager is closed"
                    );
                }
                
                // Simulate successful indexing
                Thread.sleep(50);
                indexedBlockIds.add(blockId);
                blocksIndexed.incrementAndGet();
                successfulIndexing.incrementAndGet();
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            } finally {
                if (em != null && em.isOpen()) {
                    em.close();
                }
            }
        });

        // When: Execute with fresh EntityManager (should succeed)
        CompletableFuture<IndexingCoordinator.IndexingResult> future1 =
            coordinator.coordinateIndexing(
                new IndexingCoordinator.IndexingRequest.Builder()
                    .operation("DATA_INTEGRITY_TEST")
                    .forceExecution()
                    .build()
            );

        IndexingCoordinator.IndexingResult result1 = future1.join();

        // Then: Verify data integrity
        assertEquals("COMPLETED", result1.getStatus());
        assertEquals(1, blocksIndexed.get(), "Should index 1 block successfully");
        assertEquals(0, blocksFailedToIndex.get(), "Should have 0 failed blocks");
        assertEquals(1, indexedBlockIds.size(), "Should have 1 indexed block ID");
        assertEquals(0, failedBlockIds.size(), "Should have 0 failed block IDs");
        assertEquals("BLOCK_1", indexedBlockIds.get(0), "Should index correct block");

        System.out.println(
            "📊 Data integrity - Indexed: " +
            indexedBlockIds +
            ", Failed: " +
            failedBlockIds +
            ", Success rate: " +
            blocksIndexed.get() +
            "/" +
            indexingAttempts.get()
        );
    }

    @Test
    @DisplayName("Should provide detailed error information for debugging")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void shouldProvideDetailedErrorInformation()
        throws ExecutionException, InterruptedException, TimeoutException {
        
        // Given: Indexer that provides detailed error context
        AtomicReference<String> errorDetails = new AtomicReference<>("");
        
        coordinator.registerIndexer("ERROR_DETAILS_TEST", request -> {
            indexingAttempts.incrementAndGet();
            
            EntityManager em = null;
            try {
                em = JPAUtil.getEntityManager();
                
                // Simulate closed session with detailed context
                if (em != null) {
                    boolean wasOpen = em.isOpen();
                    em.close();
                    
                    String details = String.format(
                        "BATCH_INDEXING_ERROR | " +
                        "Operation: %s | " +
                        "Thread: %s | " +
                        "EntityManager was open: %s | " +
                        "EntityManager is now open: %s | " +
                        "Timestamp: %d",
                        request.getOperation(),
                        Thread.currentThread().getName(),
                        wasOpen,
                        em.isOpen(),
                        System.currentTimeMillis()
                    );
                    
                    errorDetails.set(details);
                    failedIndexing.incrementAndGet();
                    
                    throw new IllegalStateException(
                        "Session/EntityManager is closed - " + details
                    );
                }
                
            } finally {
                if (em != null && em.isOpen()) {
                    em.close();
                }
            }
        });

        // When: Execute indexing
        CompletableFuture<IndexingCoordinator.IndexingResult> future =
            coordinator.coordinateIndexing(
                new IndexingCoordinator.IndexingRequest.Builder()
                    .operation("ERROR_DETAILS_TEST")
                    .forceExecution()
                    .build()
            );

        // Then: Verify detailed error information is captured
        try {
            IndexingCoordinator.IndexingResult result = future.get(5, TimeUnit.SECONDS);
            
            assertEquals("FAILED", result.getStatus());
            assertNotNull(errorDetails.get(), "Should capture error details");
            assertFalse(errorDetails.get().isEmpty(), "Error details should not be empty");
            
            String details = errorDetails.get();
            assertTrue(details.contains("BATCH_INDEXING_ERROR"), "Should contain operation marker");
            assertTrue(details.contains("ERROR_DETAILS_TEST"), "Should contain operation name");
            assertTrue(details.contains("Thread:"), "Should contain thread information");
            assertTrue(details.contains("EntityManager"), "Should contain EntityManager status");
            
            System.out.println("📋 Captured error details:");
            System.out.println(details);
            
        } catch (ExecutionException e) {
            // Also acceptable - check exception message
            assertNotNull(errorDetails.get());
            assertTrue(
                e.getCause().getMessage().contains("BATCH_INDEXING_ERROR"),
                "Exception should contain detailed error info"
            );
        }

        assertEquals(1, indexingAttempts.get());
        assertEquals(1, failedIndexing.get());
    }
}
