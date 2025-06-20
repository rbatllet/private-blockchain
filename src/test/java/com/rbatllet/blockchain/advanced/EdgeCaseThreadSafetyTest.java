package com.rbatllet.blockchain.advanced;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.util.CryptoUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Advanced Thread-Safety Tests focusing on Edge Cases and Recovery Scenarios
 * Tests complex scenarios that could expose subtle race conditions
 */
public class EdgeCaseThreadSafetyTest {

    private Blockchain blockchain;
    private ExecutorService executorService;
    private final int THREAD_COUNT = 15;

    @BeforeEach
    void setUp() {
        blockchain = new Blockchain();
        blockchain.clearAndReinitialize();
        executorService = Executors.newFixedThreadPool(THREAD_COUNT);
    }

    @Test
    @DisplayName("💥 Simultaneous Chain Operations Under Stress")
    @Timeout(45)
    void testSimultaneousChainOperationsStress() throws InterruptedException {
        // Setup multiple key pairs
        List<KeyPair> keyPairs = new ArrayList<>();
        List<String> publicKeyStrings = new ArrayList<>();
        
        for (int i = 0; i < 5; i++) {
            KeyPair keyPair = CryptoUtil.generateKeyPair();
            keyPairs.add(keyPair);
            String publicKeyString = CryptoUtil.publicKeyToString(keyPair.getPublic());
            publicKeyStrings.add(publicKeyString);
            blockchain.addAuthorizedKey(publicKeyString, "StressUser-" + i);
        }

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(THREAD_COUNT);
        
        AtomicInteger addBlockOps = new AtomicInteger(0);
        AtomicInteger rollbackOps = new AtomicInteger(0);
        AtomicInteger validationOps = new AtomicInteger(0);
        AtomicInteger searchOps = new AtomicInteger(0);
        
        ConcurrentLinkedQueue<String> errors = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    Random random = new Random(threadId);
                    
                    for (int op = 0; op < 8; op++) {
                        try {
                            int operation = random.nextInt(4);
                            
                            switch (operation) {
                                case 0: // Add blocks rapidly
                                    addBlocksRapidly(keyPairs, threadId, op, random);
                                    addBlockOps.incrementAndGet();
                                    break;
                                    
                                case 1: // Perform rollback operations
                                    performRollback(threadId, random);
                                    rollbackOps.incrementAndGet();
                                    break;
                                    
                                case 2: // Validation under load
                                    performValidationUnderLoad(threadId);
                                    validationOps.incrementAndGet();
                                    break;
                                    
                                case 3: // Search operations
                                    performComplexSearch(threadId);
                                    searchOps.incrementAndGet();
                                    break;
                            }
                            
                            // Random micro-delay to increase contention
                            if (random.nextInt(10) == 0) {
                                Thread.sleep(random.nextInt(5));
                            }
                            
                        } catch (Exception e) {
                            errors.offer("Thread " + threadId + ", Op " + op + ": " + e.getMessage());
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = endLatch.await(40, TimeUnit.SECONDS);
        assertTrue(completed, "Stress test should complete within timeout");

        System.out.println("💥 Simultaneous Operations Results:");
        System.out.println("   - Add block operations: " + addBlockOps.get());
        System.out.println("   - Rollback operations: " + rollbackOps.get());
        System.out.println("   - Validation operations: " + validationOps.get());
        System.out.println("   - Search operations: " + searchOps.get());
        System.out.println("   - Final block count: " + blockchain.getBlockCount());
        System.out.println("   - Errors: " + errors.size());

        if (!errors.isEmpty()) {
            System.out.println("❌ Errors encountered:");
            errors.stream().limit(5).forEach(System.out::println);
        }

        // Final consistency check
        var validationResult = blockchain.validateChainDetailed();
        assertTrue(validationResult.isStructurallyIntact(), "Chain should be structurally intact after stress operations");
        assertTrue(validationResult.isFullyCompliant(), "Chain should be fully compliant after stress operations");
    }

    @Test
    @DisplayName("⚡ Extreme Concurrency Burst")
    @Timeout(20)
    void testExtremeConcurrencyBurst() throws InterruptedException {
        KeyPair keyPair = CryptoUtil.generateKeyPair();
        String publicKeyString = CryptoUtil.publicKeyToString(keyPair.getPublic());
        blockchain.addAuthorizedKey(publicKeyString, "BurstUser");

        // Use more threads for extreme concurrency
        int EXTREME_THREAD_COUNT = 30;
        ExecutorService extremeExecutor = Executors.newFixedThreadPool(EXTREME_THREAD_COUNT);
        
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(EXTREME_THREAD_COUNT);
        
        AtomicInteger successfulBursts = new AtomicInteger(0);
        AtomicInteger failedBursts = new AtomicInteger(0);
        AtomicReference<Exception> criticalError = new AtomicReference<>();

        for (int i = 0; i < EXTREME_THREAD_COUNT; i++) {
            final int threadId = i;
            extremeExecutor.submit(() -> {
                try {
                    startLatch.await();
                    
                    // Each thread tries to add 2 blocks as fast as possible
                    for (int j = 0; j < 2; j++) {
                        String data = "BurstBlock-T" + threadId + "-" + j + "-" + System.nanoTime();
                        
                        if (blockchain.addBlock(data, keyPair.getPrivate(), keyPair.getPublic())) {
                            successfulBursts.incrementAndGet();
                        } else {
                            failedBursts.incrementAndGet();
                        }
                        
                        // No delay - maximum stress
                    }
                } catch (Exception e) {
                    criticalError.set(e);
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = endLatch.await(15, TimeUnit.SECONDS);
        extremeExecutor.shutdown();
        
        assertTrue(completed, "Extreme concurrency test should complete within timeout");
        assertNull(criticalError.get(), "No critical errors should occur: " + 
            (criticalError.get() != null ? criticalError.get().getMessage() : ""));

        System.out.println("⚡ Extreme Concurrency Results:");
        System.out.println("   - Successful bursts: " + successfulBursts.get());
        System.out.println("   - Failed bursts: " + failedBursts.get());
        System.out.println("   - Total blocks: " + blockchain.getBlockCount());
        System.out.println("   - Expected blocks: " + (successfulBursts.get() + 1)); // +1 for genesis

        var validationResult = blockchain.validateChainDetailed();
        assertTrue(validationResult.isStructurallyIntact(), "Chain should be structurally intact after extreme concurrency");
        assertTrue(validationResult.isFullyCompliant(), "Chain should be fully compliant after extreme concurrency");
        assertEquals(successfulBursts.get() + 1, blockchain.getBlockCount(), 
            "Block count should match successful operations plus genesis");
    }

    // Helper methods
    
    private void addBlocksRapidly(List<KeyPair> keyPairs, int threadId, int op, Random random) {
        if (!keyPairs.isEmpty()) {
            KeyPair keyPair = keyPairs.get(random.nextInt(keyPairs.size()));
            for (int i = 0; i < 2; i++) {
                String data = "RapidBlock-T" + threadId + "-" + op + "-" + i;
                blockchain.addBlock(data, keyPair.getPrivate(), keyPair.getPublic());
            }
        }
    }
    
    private void performRollback(int threadId, Random random) {
        long blockCount = blockchain.getBlockCount();
        if (blockCount > 5) { // Keep some blocks
            long rollbackCount = 1 + random.nextInt(3);
            blockchain.rollbackBlocks(rollbackCount);
        }
    }
    
    private void performValidationUnderLoad(int threadId) {
        // Multiple validation calls to stress the system
        var validationResult = blockchain.validateChainDetailed();
        // Just checking the boolean values without asserting to avoid interference
        validationResult.isStructurallyIntact();
        validationResult.isFullyCompliant();
        blockchain.getBlockCount();
        blockchain.getAllBlocks();
        blockchain.getLastBlock();
    }
    
    private void performComplexSearch(int threadId) {
        blockchain.searchBlocksByContent("Block");
        blockchain.searchBlocksByContent("T" + threadId);
        blockchain.getBlocksByDateRange(
            java.time.LocalDateTime.now().minusHours(1).toLocalDate(),
            java.time.LocalDateTime.now().toLocalDate()
        );
    }
}
