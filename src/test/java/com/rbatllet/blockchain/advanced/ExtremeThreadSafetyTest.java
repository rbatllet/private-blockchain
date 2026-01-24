package com.rbatllet.blockchain.advanced;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.security.UserRole;
import com.rbatllet.blockchain.util.CryptoUtil;
import org.junit.jupiter.api.*;

import java.security.KeyPair;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Extreme Thread Safety Test
 * Combines insert, rollback, and delete operations under extreme concurrency
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ExtremeThreadSafetyTest {
    private static final Logger logger = LoggerFactory.getLogger(ExtremeThreadSafetyTest.class);


    private Blockchain blockchain;
    private ExecutorService executorService;
    private KeyPair[] testKeyPairs;
    private String[] publicKeys;

    @BeforeEach
    void setUp() {
        blockchain = new Blockchain();
        blockchain.clearAndReinitialize();
        executorService = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("TestWorker-", 0).factory()); // Java 25 Virtual Threads;
        
        // Pre-generate keys to avoid delays during test execution
        testKeyPairs = new KeyPair[5];
        publicKeys = new String[5];

        // RBAC FIX (v1.0.6): First user is bootstrap admin, rest are created by admin
        for (int i = 0; i < 5; i++) {
            testKeyPairs[i] = CryptoUtil.generateKeyPair();
            publicKeys[i] = CryptoUtil.publicKeyToString(testKeyPairs[i].getPublic());

            if (i == 0) {
                blockchain.createBootstrapAdmin(publicKeys[i], "TestUser" + i);
            } else {
                blockchain.addAuthorizedKey(publicKeys[i], "TestUser" + i, testKeyPairs[0], UserRole.USER);
            }
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
        logger.info("🔥 Starting extreme insertion and rollback concurrency test...");
        
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
                            var validationResult = blockchain.validateChainDetailed();
                            boolean isValid = validationResult.isStructurallyIntact() && validationResult.isFullyCompliant();
                            if (isValid) {
                                validationPasses.incrementAndGet();
                            } else {
                                validationFails.incrementAndGet();
                                String details = !validationResult.isStructurallyIntact() ? 
                                    "structural issues" : "compliance issues";
                                inconsistencies.offer("VALIDATION-FAIL-T" + threadId + "-Iter" + j + 
                                    ": Chain invalid (" + details + ") at " + System.currentTimeMillis());
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
        
        logger.info("🔥 EXTREME TEST RESULTS:");
        logger.info("   ⏱️  Execution time: " + (endTime - startTime) + "ms");
        logger.info("   📊 INSERTIONS:");
        logger.info("      ✅ Successful: " + successfulInsertions.get());
        logger.info("      ❌ Failed: " + failedInsertions.get());
        logger.info("   🔄 ROLLBACKS:");
        logger.info("      ✅ Successful: " + successfulRollbacks.get());
        logger.info("      ❌ Failed: " + failedRollbacks.get());
        logger.info("   🔍 VALIDATIONS:");
        logger.info("      ✅ Valid: " + validationPasses.get());
        logger.info("      ❌ Invalid: " + validationFails.get());
        logger.info("   🏗️  Final blockchain state:");
        logger.info("      📦 Total blocks: " + blockchain.getBlockCount());
        var finalValidation = blockchain.validateChainDetailed();
        logger.info("      🔗 Chain structurally intact: " + finalValidation.isStructurallyIntact());
        logger.info("      🔗 Chain fully compliant: " + finalValidation.isFullyCompliant());
        if (!finalValidation.isFullyCompliant()) {
            logger.info("      ⚠️  Revoked blocks: " + finalValidation.getRevokedBlocks());
        }
        if (!finalValidation.isStructurallyIntact()) {
            logger.info("      ❌ Invalid blocks: " + finalValidation.getInvalidBlocks());
        }
        logger.info("   ⚠️  Inconsistencies detected: " + inconsistencies.size());
        
        // Show inconsistencies if they exist
        if (!inconsistencies.isEmpty()) {
            logger.info("   🚨 INCONSISTENCY DETAILS:");
            inconsistencies.stream().limit(10).forEach(inc -> 
                logger.info("      - " + inc));
            if (inconsistencies.size() > 10) {
                logger.info("      ... and " + (inconsistencies.size() - 10) + " more");
            }
        }
        
        // CRITICAL VALIDATIONS
        assertTrue(inconsistencies.isEmpty(), 
            "No inconsistencies should be detected: " + inconsistencies.size() + " found");
        
        var finalValidationResult = blockchain.validateChainDetailed();
        assertTrue(finalValidationResult.isStructurallyIntact(), 
            "Chain must be structurally intact at the end of test");
        assertTrue(finalValidationResult.isFullyCompliant(), 
            "Chain must be fully compliant at the end of test");
        
        // At least some operations should be successful
        assertTrue(successfulInsertions.get() > 0, 
            "Should have at least some successful insertions");
        
        // If there were successful rollbacks, validate they didn't break integrity
        if (successfulRollbacks.get() > 0) {
            // Verify sequence of block numbers
            List<Block> allBlocks = new ArrayList<>();
            blockchain.processChainInBatches(batch -> allBlocks.addAll(batch), 1000);

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
        
        logger.info("✅ EXTREME TEST PASSED: Thread-safety confirmed under extreme conditions!");
    }

    @Test
    @Order(2)
    @DisplayName("🎯 Complex Test: Insert + Authorization/Revocation + Rollback")
    @Timeout(120)
    void testComplexKeyManagementAndRollback() throws InterruptedException {
        logger.info("🎯 Complex key management and rollback test...");
        
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
                                        try {
                                            success = blockchain.rollbackBlocks(1L);
                                            operationLog.offer("T" + threadId + ": ROLLBACK-1 " + (success ? "✅" : "❌"));
                                        } catch (IllegalArgumentException e) {
                                            // Expected validation exception - treat as failed operation, not critical error
                                            success = false;
                                            operationLog.offer("T" + threadId + ": ROLLBACK-1 ❌ (validation)");
                                        }
                                    } else {
                                        success = true; // Can't rollback, but not an error
                                    }
                                    break;

                                case 2: // Medium rollback
                                    long blockCount = blockchain.getBlockCount();
                                    if (blockCount > 5) {
                                        long rollbackCount = ThreadLocalRandom.current().nextLong(2,
                                            Math.min(4, blockCount - 2));
                                        try {
                                            success = blockchain.rollbackBlocks(rollbackCount);
                                            operationLog.offer("T" + threadId + ": ROLLBACK-" + rollbackCount +
                                                " " + (success ? "✅" : "❌"));
                                        } catch (IllegalArgumentException e) {
                                            // Expected validation exception - treat as failed operation, not critical error
                                            success = false;
                                            operationLog.offer("T" + threadId + ": ROLLBACK-" + rollbackCount + " ❌ (validation)");
                                        }
                                    } else {
                                        success = true;
                                    }
                                    break;
                                    
                                case 3: // Validate chain
                                    var validationResult = blockchain.validateChainDetailed();
                                    success = validationResult.isStructurallyIntact() && validationResult.isFullyCompliant();
                                    operationLog.offer("T" + threadId + ": VALIDATE " + (success ? "✅" : "❌"));
                                    if (!success) {
                                        criticalError.set(true);
                                        String details = !validationResult.isStructurallyIntact() ? 
                                            "structural" : "compliance";
                                        operationLog.offer("T" + threadId + ": 🚨 CRITICAL: Chain invalid (" + details + ")!");
                                    }
                                    break;
                                    
                                case 4: // Authorize new key
                                    // RBAC FIX (v1.0.6): Use addAuthorizedKey instead of createBootstrapAdmin
                                    KeyPair newKey = CryptoUtil.generateKeyPair();
                                    String newPublicKey = CryptoUtil.publicKeyToString(newKey.getPublic());
                                    try {
                                        blockchain.addAuthorizedKey(newPublicKey, "DynamicUser" + threadId + j, testKeyPairs[0], UserRole.USER);
                                        success = true;
                                    } catch (Exception e) {
                                        success = false;
                                    }
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
        logger.info("🎯 COMPLEX TEST RESULTS:");
        logger.info("   ⏱️  Time: " + (endTime - startTime) + "ms");
        logger.info("   📊 Total operations: " + totalOperations.get());
        logger.info("   ✅ Successful operations: " + successfulOperations.get());
        logger.info("   📈 Success rate: " + 
            String.format("%.2f%%", 100.0 * successfulOperations.get() / totalOperations.get()));
        logger.info("   🏗️  Final blocks: " + blockchain.getBlockCount());
        var finalValidationResult = blockchain.validateChainDetailed();
        logger.info("   🔗 Chain structurally intact: " + finalValidationResult.isStructurallyIntact());
        logger.info("   🔗 Chain fully compliant: " + finalValidationResult.isFullyCompliant());
        logger.info("   🔑 Authorized keys: " + blockchain.getAuthorizedKeys().size());
        
        // Show operation log (last 20)
        logger.info("   📝 Last operations:");
        operationLog.stream().skip(Math.max(0, operationLog.size() - 20))
            .forEach(log -> logger.info("      " + log));
        
        // Critical validations
        assertTrue(completed, "Test must complete on time");
        assertFalse(criticalError.get(), "No critical errors should occur");
        var finalValidation = blockchain.validateChainDetailed();
        assertTrue(finalValidation.isStructurallyIntact(), "Final chain must be structurally intact");
        assertTrue(finalValidation.isFullyCompliant(), "Final chain must be fully compliant");
        assertTrue(successfulOperations.get() > totalOperations.get() * 0.7, 
            "At least 70% of operations must be successful");
        
        logger.info("✅ COMPLEX TEST PASSED: Thread-safety confirmed in mixed operations!");
    }
}
