package com.rbatllet.blockchain.advanced;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.util.CryptoUtil;
import org.junit.jupiter.api.*;

import java.security.KeyPair;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Extreme Thread Safety Test
 * Combines insert, rollback, and delete operations under extreme concurrency
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ExtremeThreadSafetyTest {

    private Blockchain blockchain;
    private ExecutorService executorService;
    private KeyPair[] testKeyPairs;
    private String[] publicKeys;

    @BeforeEach
    void setUp() {
        blockchain = new Blockchain();
        blockchain.clearAndReinitialize();
        executorService = Executors.newFixedThreadPool(50);
        
        // Pre-generate keys to avoid delays during test execution
        testKeyPairs = new KeyPair[5];
        publicKeys = new String[5];
        for (int i = 0; i < 5; i++) {
            testKeyPairs[i] = CryptoUtil.generateKeyPair();
            publicKeys[i] = CryptoUtil.publicKeyToString(testKeyPairs[i].getPublic());
            blockchain.addAuthorizedKey(publicKeys[i], "TestUser" + i);
        }
    }

    @AfterEach
    void tearDown() {
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
            }
        }
    }

    @Test
    @Order(1)
    @DisplayName("🔥 Extreme Test: Insert + Rollback + Validation Concurrency")
    @Timeout(120)
    void testExtremeInsertionRollbackConcurrency() throws InterruptedException {
        System.out.println("🔥 Starting extreme insertion and rollback concurrency test...");
        
        // Test configuration
        int INSERTION_THREADS = 15;      // 15 threads inserting blocks
        int ROLLBACK_THREADS = 5;        // 5 threads doing rollbacks
        int VALIDATION_THREADS = 5;      // 5 threads validating chain
        int BLOCKS_PER_INSERTION_THREAD = 10;
        int TOTAL_THREADS = INSERTION_THREADS + ROLLBACK_THREADS + VALIDATION_THREADS;
        
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(TOTAL_THREADS);
        
        // Thread-safe counters
        AtomicInteger successfulInsertions = new AtomicInteger(0);
        AtomicInteger failedInsertions = new AtomicInteger(0);
        AtomicInteger successfulRollbacks = new AtomicInteger(0);
        AtomicInteger failedRollbacks = new AtomicInteger(0);
        AtomicInteger validationPasses = new AtomicInteger(0);
        AtomicInteger validationFails = new AtomicInteger(0);
        
        // To detect inconsistencies
        ConcurrentLinkedQueue<String> inconsistencies = new ConcurrentLinkedQueue<>();
        AtomicBoolean stopOperations = new AtomicBoolean(false);
        
        // 1. INSERTION THREADS
        for (int i = 0; i < INSERTION_THREADS; i++) {
            final int threadId = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    
                    for (int j = 0; j < BLOCKS_PER_INSERTION_THREAD && !stopOperations.get(); j++) {
                        try {
                            // Use different keys to simulate realistic operations
                            int keyIndex = threadId % testKeyPairs.length;
                            String data = String.format("INSERT-T%d-B%d-%d", threadId, j, System.nanoTime());
                            
                            boolean success = blockchain.addBlock(data, 
                                testKeyPairs[keyIndex].getPrivate(), 
                                testKeyPairs[keyIndex].getPublic());
                            
                            if (success) {
                                successfulInsertions.incrementAndGet();
                            } else {
                                failedInsertions.incrementAndGet();
                            }
                            
                            // Small random pause to create realistic concurrency patterns
                            Thread.sleep(ThreadLocalRandom.current().nextInt(1, 5));
                            
                        } catch (Exception e) {
                            failedInsertions.incrementAndGet();
                            inconsistencies.offer("INSERT-ERROR-T" + threadId + ": " + e.getMessage());
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }
        
        // 2. ROLLBACK THREADS
        for (int i = 0; i < ROLLBACK_THREADS; i++) {
            final int threadId = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    
                    // Wait a bit for some blocks to be inserted
                    Thread.sleep(100);
                    
                    for (int j = 0; j < 8 && !stopOperations.get(); j++) {
                        try {
                            // Do rollbacks of variable sizes
                            long currentBlocks = blockchain.getBlockCount();
                            if (currentBlocks > 3) { // Keep at least 3 blocks (genesis + 2)
                                long rollbackCount = ThreadLocalRandom.current().nextLong(1, 
                                    Math.min(4, currentBlocks - 2));
                                
                                boolean success = blockchain.rollbackBlocks(rollbackCount);
                                if (success) {
                                    successfulRollbacks.incrementAndGet();
                                } else {
                                    failedRollbacks.incrementAndGet();
                                }
                            }
                            
                            Thread.sleep(ThreadLocalRandom.current().nextInt(50, 150));
                            
                        } catch (Exception e) {
                            failedRollbacks.incrementAndGet();
                            inconsistencies.offer("ROLLBACK-ERROR-T" + threadId + ": " + e.getMessage());
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }
        
        // 3. CONTINUOUS VALIDATION THREADS
        for (int i = 0; i < VALIDATION_THREADS; i++) {
            final int threadId = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    
                    for (int j = 0; j < 20 && !stopOperations.get(); j++) {
                        try {
                            boolean isValid = blockchain.validateChain();
                            if (isValid) {
                                validationPasses.incrementAndGet();
                            } else {
                                validationFails.incrementAndGet();
                                inconsistencies.offer("VALIDATION-FAIL-T" + threadId + "-Iter" + j + 
                                    ": Chain invalid at " + System.currentTimeMillis());
                            }
                            
                            Thread.sleep(ThreadLocalRandom.current().nextInt(25, 75));
                            
                        } catch (Exception e) {
                            validationFails.incrementAndGet();
                            inconsistencies.offer("VALIDATION-ERROR-T" + threadId + ": " + e.getMessage());
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }
        
        // Start the chaos
        long startTime = System.currentTimeMillis();
        startLatch.countDown();
        
        // Wait for all operations to complete
        boolean completed = endLatch.await(110, TimeUnit.SECONDS);
        stopOperations.set(true);
        long endTime = System.currentTimeMillis();
        
        // RESULTS ANALYSIS
        assertTrue(completed, "Test should complete within timeout");
        
        System.out.println("🔥 EXTREME TEST RESULTS:");
        System.out.println("   ⏱️  Execution time: " + (endTime - startTime) + "ms");
        System.out.println("   📊 INSERTIONS:");
        System.out.println("      ✅ Successful: " + successfulInsertions.get());
        System.out.println("      ❌ Failed: " + failedInsertions.get());
        System.out.println("   🔄 ROLLBACKS:");
        System.out.println("      ✅ Successful: " + successfulRollbacks.get());
        System.out.println("      ❌ Failed: " + failedRollbacks.get());
        System.out.println("   🔍 VALIDATIONS:");
        System.out.println("      ✅ Valid: " + validationPasses.get());
        System.out.println("      ❌ Invalid: " + validationFails.get());
        System.out.println("   🏗️  Final blockchain state:");
        System.out.println("      📦 Total blocks: " + blockchain.getBlockCount());
        System.out.println("      🔗 Chain valid: " + blockchain.validateChain());
        System.out.println("   ⚠️  Inconsistencies detected: " + inconsistencies.size());
        
        // Show inconsistencies if they exist
        if (!inconsistencies.isEmpty()) {
            System.out.println("   🚨 INCONSISTENCY DETAILS:");
            inconsistencies.stream().limit(10).forEach(inc -> 
                System.out.println("      - " + inc));
            if (inconsistencies.size() > 10) {
                System.out.println("      ... and " + (inconsistencies.size() - 10) + " more");
            }
        }
        
        // CRITICAL VALIDATIONS
        assertTrue(inconsistencies.isEmpty(), 
            "No inconsistencies should be detected: " + inconsistencies.size() + " found");
        
        assertTrue(blockchain.validateChain(), 
            "Chain must be valid at the end of test");
        
        // At least some operations should be successful
        assertTrue(successfulInsertions.get() > 0, 
            "Should have at least some successful insertions");
        
        // If there were successful rollbacks, validate they didn't break integrity
        if (successfulRollbacks.get() > 0) {
            // Verify sequence of block numbers
            List<Block> allBlocks = blockchain.getAllBlocks();
            for (int i = 1; i < allBlocks.size(); i++) {
                Long prevNumber = allBlocks.get(i-1).getBlockNumber();
                Long currNumber = allBlocks.get(i).getBlockNumber();
                assertEquals(prevNumber + 1, currNumber, 
                    "Block numbers must be sequential after rollbacks");
            }
        }
        
        // Most validations should pass
        double validationSuccessRate = (double) validationPasses.get() / 
            (validationPasses.get() + validationFails.get());
        assertTrue(validationSuccessRate > 0.8, 
            "At least 80% of validations must pass: " + validationSuccessRate);
        
        System.out.println("✅ EXTREME TEST PASSED: Thread-safety confirmed under extreme conditions!");
    }

    @Test
    @Order(2)
    @DisplayName("🎯 Complex Test: Insert + Authorization/Revocation + Rollback")
    @Timeout(120)
    void testComplexKeyManagementAndRollback() throws InterruptedException {
        System.out.println("🎯 Complex key management and rollback test...");
        
        int OPERATION_THREADS = 12;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(OPERATION_THREADS);
        
        AtomicInteger totalOperations = new AtomicInteger(0);
        AtomicInteger successfulOperations = new AtomicInteger(0);
        ConcurrentLinkedQueue<String> operationLog = new ConcurrentLinkedQueue<>();
        AtomicBoolean criticalError = new AtomicBoolean(false);
        
        // Create threads that mix all operations
        for (int i = 0; i < OPERATION_THREADS; i++) {
            final int threadId = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    
                    for (int j = 0; j < 15 && !criticalError.get(); j++) {
                        try {
                            totalOperations.incrementAndGet();
                            int operation = ThreadLocalRandom.current().nextInt(6);
                            boolean success = false;
                            
                            switch (operation) {
                                case 0: // Insert block
                                    int keyIndex = ThreadLocalRandom.current().nextInt(testKeyPairs.length);
                                    String data = String.format("COMPLEX-T%d-J%d-%d", 
                                        threadId, j, System.nanoTime());
                                    success = blockchain.addBlock(data, 
                                        testKeyPairs[keyIndex].getPrivate(), 
                                        testKeyPairs[keyIndex].getPublic());
                                    operationLog.offer("T" + threadId + ": INSERT " + (success ? "✅" : "❌"));
                                    break;
                                    
                                case 1: // Small rollback
                                    if (blockchain.getBlockCount() > 3) {
                                        success = blockchain.rollbackBlocks(1L);
                                        operationLog.offer("T" + threadId + ": ROLLBACK-1 " + (success ? "✅" : "❌"));
                                    } else {
                                        success = true; // Can't rollback, but not an error
                                    }
                                    break;
                                    
                                case 2: // Medium rollback
                                    long blockCount = blockchain.getBlockCount();
                                    if (blockCount > 5) {
                                        long rollbackCount = ThreadLocalRandom.current().nextLong(2, 
                                            Math.min(4, blockCount - 2));
                                        success = blockchain.rollbackBlocks(rollbackCount);
                                        operationLog.offer("T" + threadId + ": ROLLBACK-" + rollbackCount + 
                                            " " + (success ? "✅" : "❌"));
                                    } else {
                                        success = true;
                                    }
                                    break;
                                    
                                case 3: // Validate chain
                                    success = blockchain.validateChain();
                                    operationLog.offer("T" + threadId + ": VALIDATE " + (success ? "✅" : "❌"));
                                    if (!success) {
                                        criticalError.set(true);
                                        operationLog.offer("T" + threadId + ": 🚨 CRITICAL: Chain invalid!");
                                    }
                                    break;
                                    
                                case 4: // Authorize new key
                                    KeyPair newKey = CryptoUtil.generateKeyPair();
                                    String newPublicKey = CryptoUtil.publicKeyToString(newKey.getPublic());
                                    success = blockchain.addAuthorizedKey(newPublicKey, "DynamicUser" + threadId + j);
                                    operationLog.offer("T" + threadId + ": ADD_KEY " + (success ? "✅" : "❌"));
                                    break;
                                    
                                case 5: // Combined operation: insert several blocks in sequence
                                    int insertCount = ThreadLocalRandom.current().nextInt(2, 4);
                                    int successCount = 0;
                                    for (int k = 0; k < insertCount; k++) {
                                        keyIndex = ThreadLocalRandom.current().nextInt(testKeyPairs.length);
                                        data = String.format("BATCH-T%d-J%d-K%d-%d", 
                                            threadId, j, k, System.nanoTime());
                                        if (blockchain.addBlock(data, 
                                            testKeyPairs[keyIndex].getPrivate(), 
                                            testKeyPairs[keyIndex].getPublic())) {
                                            successCount++;
                                        }
                                    }
                                    success = successCount > 0;
                                    operationLog.offer("T" + threadId + ": BATCH_INSERT " + successCount + 
                                        "/" + insertCount + " " + (success ? "✅" : "❌"));
                                    break;
                            }
                            
                            if (success) {
                                successfulOperations.incrementAndGet();
                            }
                            
                            // Variable pause to create realistic patterns
                            Thread.sleep(ThreadLocalRandom.current().nextInt(5, 25));
                            
                        } catch (Exception e) {
                            operationLog.offer("T" + threadId + ": EXCEPTION " + e.getMessage());
                            criticalError.set(true);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }
        
        long startTime = System.currentTimeMillis();
        startLatch.countDown();
        
        boolean completed = endLatch.await(110, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();
        
        // Final analysis
        System.out.println("🎯 COMPLEX TEST RESULTS:");
        System.out.println("   ⏱️  Time: " + (endTime - startTime) + "ms");
        System.out.println("   📊 Total operations: " + totalOperations.get());
        System.out.println("   ✅ Successful operations: " + successfulOperations.get());
        System.out.println("   📈 Success rate: " + 
            String.format("%.2f%%", 100.0 * successfulOperations.get() / totalOperations.get()));
        System.out.println("   🏗️  Final blocks: " + blockchain.getBlockCount());
        System.out.println("   🔗 Chain valid: " + blockchain.validateChain());
        System.out.println("   🔑 Authorized keys: " + blockchain.getAuthorizedKeys().size());
        
        // Show operation log (last 20)
        System.out.println("   📝 Last operations:");
        operationLog.stream().skip(Math.max(0, operationLog.size() - 20))
            .forEach(log -> System.out.println("      " + log));
        
        // Critical validations
        assertTrue(completed, "Test must complete on time");
        assertFalse(criticalError.get(), "No critical errors should occur");
        assertTrue(blockchain.validateChain(), "Final chain must be valid");
        assertTrue(successfulOperations.get() > totalOperations.get() * 0.7, 
            "At least 70% of operations must be successful");
        
        System.out.println("✅ COMPLEX TEST PASSED: Thread-safety confirmed in mixed operations!");
    }
}
