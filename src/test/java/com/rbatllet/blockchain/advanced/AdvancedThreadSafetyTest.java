package com.rbatllet.blockchain.advanced;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.AuthorizedKey;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.security.UserRole;
import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.config.MemorySafetyConstants;
import com.rbatllet.blockchain.validation.ChainValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.security.KeyPair;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Advanced Thread-Safety Tests for Private Blockchain
 * Tests complex scenarios that combine multiple operations to detect hidden race conditions
 */
public class AdvancedThreadSafetyTest {
    private static final Logger logger = LoggerFactory.getLogger(AdvancedThreadSafetyTest.class);

    private Blockchain blockchain;
    private ExecutorService executorService;
    private KeyPair testBootstrapKeyPair; // RBAC FIX (v1.0.6): Shared bootstrap admin for helper methods
    private final int THREAD_COUNT = 20;
    private final int OPERATIONS_PER_THREAD = 10;

    @BeforeEach
    void setUp() {
        blockchain = new Blockchain();
        blockchain.clearAndReinitialize();
        executorService = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("TestWorker-", 0).factory()); // Java 25 Virtual Threads;

        // RBAC FIX (v1.0.6): Create bootstrap admin for tests
        testBootstrapKeyPair = CryptoUtil.generateKeyPair();
        String bootstrapPublicKey = CryptoUtil.publicKeyToString(testBootstrapKeyPair.getPublic());
        blockchain.createBootstrapAdmin(bootstrapPublicKey, "TestBootstrapAdmin");
    }

    @Test
    @DisplayName("🔄 Complex Mixed Operations - Read/Write/Key Management")
    @Timeout(60)
    void testComplexMixedOperations() throws InterruptedException {
        // Prepare initial keys
        Map<String, KeyPair> keyPairs = prepareInitialKeys(5);
        
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(THREAD_COUNT);
        
        AtomicInteger successfulOperations = new AtomicInteger(0);
        AtomicInteger failedOperations = new AtomicInteger(0);
        ConcurrentLinkedQueue<String> errors = new ConcurrentLinkedQueue<>();
        
        // Create mixed operation threads
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    
                    Random random = new Random(threadId);
                    
                    for (int op = 0; op < OPERATIONS_PER_THREAD; op++) {
                        try {
                            int operation = random.nextInt(6);
                            
                            switch (operation) {
                                case 0: // Add block
                                    addRandomBlock(keyPairs, threadId, op);
                                    break;
                                case 1: // Read chain state
                                    validateChainConsistency(threadId);
                                    break;
                                case 2: // Add new key
                                    addRandomKey(threadId, op);
                                    break;
                                case 3: // Revoke existing key
                                    revokeRandomKey(threadId);
                                    break;
                                case 4: // Search operations
                                    performSearchOperations(threadId);
                                    break;
                                case 5: // Chain validation
                                    validateFullChain(threadId);
                                    break;
                            }
                            
                            successfulOperations.incrementAndGet();
                            
                            // Random delay to increase chance of race conditions
                            Thread.sleep(random.nextInt(5));
                            
                        } catch (Exception e) {
                            failedOperations.incrementAndGet();
                            errors.offer("Thread " + threadId + " operation " + op + ": " + e.getMessage());
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }
        
        // Start all threads simultaneously
        startLatch.countDown();
        
        // Wait for completion
        boolean completed = endLatch.await(45, TimeUnit.SECONDS);
        assertTrue(completed, "Test should complete within timeout");
        
        // Verify results
        logger.info("🔍 Mixed Operations Results:");
        logger.info("   - Successful operations: " + successfulOperations.get());
        logger.info("   - Failed operations: " + failedOperations.get());
        logger.info("   - Total blocks: " + blockchain.getBlockCount());
        logger.info("   - Active keys: " + blockchain.getAuthorizedKeys().size());
        
        // Print any errors
        if (!errors.isEmpty()) {
            logger.info("❌ Errors encountered:");
            errors.forEach(System.out::println);
        }
        
        // Final consistency check
        ChainValidationResult mixedOpsResult = blockchain.validateChainDetailed();
        assertTrue(mixedOpsResult.isStructurallyIntact(), "Chain should remain structurally intact after mixed operations");
        // Note: May not be fully compliant due to concurrent key operations, but structure should be intact
        assertTrue(successfulOperations.get() > 0, "Should have successful operations");
    }

    @Test
    @DisplayName("⚡ High-Speed Block Creation Race")
    @Timeout(30)
    void testHighSpeedBlockCreationRace() throws InterruptedException {
        // RBAC FIX (v1.0.6): Use existing bootstrap admin to authorize race test user
        KeyPair keyPair = CryptoUtil.generateKeyPair();
        String publicKeyString = CryptoUtil.publicKeyToString(keyPair.getPublic());
        blockchain.addAuthorizedKey(publicKeyString, "RaceTestUser", testBootstrapKeyPair, UserRole.USER);
        
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(THREAD_COUNT);
        
        AtomicLong successfulBlocks = new AtomicLong(0);
        AtomicLong failedBlocks = new AtomicLong(0);
        Set<Long> blockNumbers = ConcurrentHashMap.newKeySet();
        
        // Create threads that rapidly try to add blocks
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    
                    for (int j = 0; j < 5; j++) {
                        String data = "RaceBlock-T" + threadId + "-" + j + "-" + System.nanoTime();
                        
                        // FIXED: Use addBlockAndReturn to avoid race condition
                        Block addedBlock = blockchain.addBlockAndReturn(data, keyPair.getPrivate(), keyPair.getPublic());
                        if (addedBlock != null) {
                            successfulBlocks.incrementAndGet();
                            blockNumbers.add(addedBlock.getBlockNumber()); // Direct access to created block
                        } else {
                            failedBlocks.incrementAndGet();
                        }
                        
                        // No delay - maximum race condition potential
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }
        
        startLatch.countDown();
        boolean completed = endLatch.await(25, TimeUnit.SECONDS);
        assertTrue(completed, "High-speed test should complete within timeout");
        
        // Verify block number sequence integrity
        List<Long> sortedNumbers = new ArrayList<>(blockNumbers);
        Collections.sort(sortedNumbers);
        
        logger.info("⚡ High-Speed Race Results:");
        logger.info("   - Successful blocks: " + successfulBlocks.get());
        logger.info("   - Failed blocks: " + failedBlocks.get());
        logger.info("   - Unique block numbers: " + blockNumbers.size());
        logger.info("   - Expected blocks: " + blockchain.getBlockCount());
        
        // Critical assertions
        assertEquals(successfulBlocks.get(), blockNumbers.size(), "Each successful block should have unique number");
        ChainValidationResult raceResult = blockchain.validateChainDetailed();
        assertTrue(raceResult.isFullyCompliant(), "Chain should be fully compliant after high-speed race");
        assertTrue(raceResult.isStructurallyIntact(), "Chain should be structurally intact after high-speed race");
        
        // Verify sequential block numbers (no gaps or duplicates)
        for (int i = 1; i < sortedNumbers.size(); i++) {
            assertEquals(sortedNumbers.get(i-1) + 1, sortedNumbers.get(i), 
                "Block numbers should be sequential: " + sortedNumbers);
        }
    }

    @Test
    @DisplayName("🔀 Key Lifecycle Stress Test")
    @Timeout(45)
    void testKeyLifecycleStress() throws InterruptedException {
        // RBAC FIX (v1.0.6): Use existing bootstrap admin from setUp()
        // Bootstrap admin was already created in setUp(), don't create it again

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(THREAD_COUNT);

        AtomicInteger keyOperations = new AtomicInteger(0);
        AtomicInteger blockOperations = new AtomicInteger(0);
        ConcurrentLinkedQueue<String> operationLog = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    Random random = new Random(threadId);

                    for (int cycle = 0; cycle < 3; cycle++) {
                        // Create a key
                        KeyPair keyPair = CryptoUtil.generateKeyPair();
                        String publicKeyString = CryptoUtil.publicKeyToString(keyPair.getPublic());
                        String owner = "StressUser-" + threadId + "-" + cycle;

                        // RBAC FIX (v1.0.6): Use testBootstrapKeyPair from setUp()
                        blockchain.addAuthorizedKey(publicKeyString, owner, testBootstrapKeyPair, UserRole.USER);
                        keyOperations.incrementAndGet();
                        operationLog.offer("T" + threadId + ": Added key " + owner);

                        // Use the key immediately to add blocks
                        for (int b = 0; b < 2; b++) {
                            String data = "StressBlock-" + owner + "-" + b;
                            if (blockchain.addBlock(data, keyPair.getPrivate(), keyPair.getPublic())) {
                                blockOperations.incrementAndGet();
                                operationLog.offer("T" + threadId + ": Added block with " + owner);
                            }

                            // Small random delay
                            Thread.sleep(random.nextInt(10));
                        }

                        // Sometimes revoke the key immediately after use
                        if (random.nextBoolean()) {
                            if (blockchain.revokeAuthorizedKey(publicKeyString)) {
                                operationLog.offer("T" + threadId + ": Revoked key " + owner);
                            }
                        }
                        
                        Thread.sleep(random.nextInt(5));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    operationLog.offer("T" + threadId + ": ERROR - " + e.getMessage());
                } finally {
                    endLatch.countDown();
                }
            });
        }
        
        startLatch.countDown();
        boolean completed = endLatch.await(40, TimeUnit.SECONDS);
        assertTrue(completed, "Key lifecycle test should complete within timeout");
        
        logger.info("🔀 Key Lifecycle Results:");
        logger.info("   - Key operations: " + keyOperations.get());
        logger.info("   - Block operations: " + blockOperations.get());
        logger.info("   - Final block count: " + blockchain.getBlockCount());
        logger.info("   - Final active keys: " + blockchain.getAuthorizedKeys().size());
        
        // Show some operation log for debugging
        logger.info("📝 Operation samples:");
        operationLog.stream().limit(10).forEach(System.out::println);
        
        ChainValidationResult stressResult = blockchain.validateChainDetailed();
        assertTrue(stressResult.isStructurallyIntact(), "Chain should remain structurally intact after key lifecycle stress");
        // Note: May not be fully compliant due to key lifecycle operations, but structure should be intact
        assertTrue(keyOperations.get() > 0, "Should have successful key operations");
        assertTrue(blockOperations.get() > 0, "Should have successful block operations");
    }

    @Test
    @DisplayName("🌊 Validation Flood Test")
    @Timeout(45)
    void testValidationFlood() throws InterruptedException {
        // RBAC FIX (v1.0.6): Use existing bootstrap admin to authorize validation user
        KeyPair keyPair = CryptoUtil.generateKeyPair();
        String publicKeyString = CryptoUtil.publicKeyToString(keyPair.getPublic());
        blockchain.addAuthorizedKey(publicKeyString, "ValidationUser", testBootstrapKeyPair, UserRole.USER);
        
        // Add several blocks to validate
        for (int i = 0; i < 10; i++) {
            blockchain.addBlock("ValidationBlock-" + i, keyPair.getPrivate(), keyPair.getPublic());
        }
        
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(THREAD_COUNT);
        
        AtomicInteger validationCount = new AtomicInteger(0);
        AtomicInteger readOperations = new AtomicInteger(0);
        AtomicInteger writeOperations = new AtomicInteger(0);
        
        // Add detailed timing and logging
        long startTime = System.currentTimeMillis();
        logger.info("🌊 Starting validation flood test at " + startTime);
        logger.info("   - Thread count: " + THREAD_COUNT);
        logger.info("   - Operations per thread: 15");
        logger.info("   - Total expected operations: " + (THREAD_COUNT * 15));
        
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    Random random = new Random(threadId);
                    
                    for (int op = 0; op < 15; op++) {
                        if (random.nextInt(3) == 0) {
                            // Write operation
                            String data = "FloodBlock-T" + threadId + "-" + op;
                            if (blockchain.addBlock(data, keyPair.getPrivate(), keyPair.getPublic())) {
                                writeOperations.incrementAndGet();
                            }
                        } else {
                            // Read/validation operations
                            if (random.nextBoolean()) {
                                // Using basic validation for performance during flood test
                                blockchain.validateChainDetailed();
                                validationCount.incrementAndGet();
                            } else {
                                blockchain.getBlockCount();
                                blockchain.getLastBlock();
                                readOperations.incrementAndGet();
                            }
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
        logger.info("🌊 All threads started, waiting for completion...");
        
        boolean completed = endLatch.await(40, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        logger.info("🌊 Test completed in " + duration + "ms, success: " + completed);
        
        if (!completed) {
            logger.error("❌ Test timeout reached. Remaining threads: " + endLatch.getCount());
            logger.error("Current block count: " + blockchain.getBlockCount());
            logger.error("Validations performed: " + validationCount.get());
            logger.error("Read operations: " + readOperations.get());  
            logger.error("Write operations: " + writeOperations.get());
        }
        
        logger.info("🌊 Validation Flood Results:");
        logger.info("   - Validations performed: " + validationCount.get());
        logger.info("   - Read operations: " + readOperations.get());
        logger.info("   - Write operations: " + writeOperations.get());
        logger.info("   - Final block count: " + blockchain.getBlockCount());
        
        assertTrue(completed, "Validation flood test should complete within timeout. Duration: " + duration + "ms");
        
        // Final validation should still pass
        ChainValidationResult floodResult = blockchain.validateChainDetailed();
        assertTrue(floodResult.isStructurallyIntact(), "Chain should remain structurally intact after validation flood");
        // During high concurrency, full compliance may vary but structure should be intact
    }

    // Helper methods
    
    private Map<String, KeyPair> prepareInitialKeys(int count) {
        Map<String, KeyPair> keyPairs = new HashMap<>();

        // RBAC FIX (v1.0.6): Use existing bootstrap admin from setUp() to authorize users
        // Bootstrap admin was already created in setUp(), don't create it again
        for (int i = 0; i < count; i++) {
            KeyPair keyPair = CryptoUtil.generateKeyPair();
            String publicKeyString = CryptoUtil.publicKeyToString(keyPair.getPublic());
            String owner = "InitialUser-" + i;

            // All users created by bootstrap admin
            blockchain.addAuthorizedKey(publicKeyString, owner, testBootstrapKeyPair, UserRole.USER);
            keyPairs.put(owner, keyPair);
        }

        return keyPairs;
    }
    
    private void addRandomBlock(Map<String, KeyPair> keyPairs, int threadId, int op) {
        if (!keyPairs.isEmpty()) {
            String randomOwner = keyPairs.keySet().iterator().next();
            KeyPair keyPair = keyPairs.get(randomOwner);
            String data = "MixedBlock-T" + threadId + "-" + op + "-" + System.currentTimeMillis();
            blockchain.addBlock(data, keyPair.getPrivate(), keyPair.getPublic());
        }
    }
    
    private void validateChainConsistency(int threadId) {
        long blockCount = blockchain.getBlockCount();
        Block lastBlock = blockchain.getLastBlock();

        // Verify consistency - get specific blocks to validate
        if (blockCount > 0 && lastBlock != null) {
            assertEquals(blockCount - 1, lastBlock.getBlockNumber(),
                "Last block number should match count minus 1 (genesis is 0)");
        }
    }
    
    private void addRandomKey(int threadId, int op) {
        KeyPair keyPair = CryptoUtil.generateKeyPair();
        String publicKeyString = CryptoUtil.publicKeyToString(keyPair.getPublic());
        String owner = "DynamicUser-T" + threadId + "-" + op;
        // RBAC FIX (v1.0.6): Use addAuthorizedKey with bootstrap admin credentials
        blockchain.addAuthorizedKey(publicKeyString, owner, testBootstrapKeyPair, UserRole.USER);
    }
    
    private void revokeRandomKey(int threadId) {
        List<AuthorizedKey> activeKeys = blockchain.getAuthorizedKeys();
        if (!activeKeys.isEmpty()) {
            Random random = new Random();
            AuthorizedKey keyToRevoke = activeKeys.get(random.nextInt(activeKeys.size()));
            blockchain.revokeAuthorizedKey(keyToRevoke.getPublicKey());
        }
    }
    
    private void performSearchOperations(int threadId) {
        blockchain.searchBlocksByContent("Block");
        // Stream version with try-with-resources for thread-safe concurrent access
        try (Stream<Block> stream = blockchain.streamBlocksByDateRange(
            LocalDateTime.now().minusDays(1).toLocalDate(),
            LocalDateTime.now().toLocalDate(),
            MemorySafetyConstants.DEFAULT_MAX_SEARCH_RESULTS
        )) {
            // Consume stream to trigger the query
            stream.count();
        }
    }
    
    private void validateFullChain(int threadId) {
        ChainValidationResult result = blockchain.validateChainDetailed();
        // During concurrent operations, we primarily check structural integrity
        assertTrue(result.isStructurallyIntact(), "Chain structure should remain intact during concurrent access");
    }
}
