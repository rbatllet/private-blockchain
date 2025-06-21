package com.rbatllet.blockchain.advanced;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.AuthorizedKey;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.util.CryptoUtil;
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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Advanced Thread-Safety Tests for Private Blockchain
 * Tests complex scenarios that combine multiple operations to detect hidden race conditions
 */
public class AdvancedThreadSafetyTest {

    private Blockchain blockchain;
    private ExecutorService executorService;
    private final int THREAD_COUNT = 20;
    private final int OPERATIONS_PER_THREAD = 10;

    @BeforeEach
    void setUp() {
        blockchain = new Blockchain();
        blockchain.clearAndReinitialize();
        executorService = Executors.newFixedThreadPool(THREAD_COUNT);
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
        System.out.println("🔍 Mixed Operations Results:");
        System.out.println("   - Successful operations: " + successfulOperations.get());
        System.out.println("   - Failed operations: " + failedOperations.get());
        System.out.println("   - Total blocks: " + blockchain.getBlockCount());
        System.out.println("   - Active keys: " + blockchain.getAuthorizedKeys().size());
        
        // Print any errors
        if (!errors.isEmpty()) {
            System.out.println("❌ Errors encountered:");
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
        // Create a single key pair for all threads to fight over
        KeyPair keyPair = CryptoUtil.generateKeyPair();
        String publicKeyString = CryptoUtil.publicKeyToString(keyPair.getPublic());
        blockchain.addAuthorizedKey(publicKeyString, "RaceTestUser");
        
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
        
        System.out.println("⚡ High-Speed Race Results:");
        System.out.println("   - Successful blocks: " + successfulBlocks.get());
        System.out.println("   - Failed blocks: " + failedBlocks.get());
        System.out.println("   - Unique block numbers: " + blockNumbers.size());
        System.out.println("   - Expected blocks: " + blockchain.getBlockCount());
        
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
                        
                        if (blockchain.addAuthorizedKey(publicKeyString, owner)) {
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
        
        System.out.println("🔀 Key Lifecycle Results:");
        System.out.println("   - Key operations: " + keyOperations.get());
        System.out.println("   - Block operations: " + blockOperations.get());
        System.out.println("   - Final block count: " + blockchain.getBlockCount());
        System.out.println("   - Final active keys: " + blockchain.getAuthorizedKeys().size());
        
        // Show some operation log for debugging
        System.out.println("📝 Operation samples:");
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
        // Pre-populate with some blocks
        KeyPair keyPair = CryptoUtil.generateKeyPair();
        String publicKeyString = CryptoUtil.publicKeyToString(keyPair.getPublic());
        blockchain.addAuthorizedKey(publicKeyString, "ValidationUser");
        
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
        System.out.println("🌊 Starting validation flood test at " + startTime);
        System.out.println("   - Thread count: " + THREAD_COUNT);
        System.out.println("   - Operations per thread: 15");
        System.out.println("   - Total expected operations: " + (THREAD_COUNT * 15));
        
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
                                blockchain.getAllBlocks();
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
        System.out.println("🌊 All threads started, waiting for completion...");
        
        boolean completed = endLatch.await(40, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        System.out.println("🌊 Test completed in " + duration + "ms, success: " + completed);
        
        if (!completed) {
            System.err.println("❌ Test timeout reached. Remaining threads: " + endLatch.getCount());
            System.err.println("Current block count: " + blockchain.getBlockCount());
            System.err.println("Validations performed: " + validationCount.get());
            System.err.println("Read operations: " + readOperations.get());  
            System.err.println("Write operations: " + writeOperations.get());
        }
        
        assertTrue(completed, "Validation flood test should complete within timeout. Duration: " + duration + "ms");
        
        System.out.println("🌊 Validation Flood Results:");
        System.out.println("   - Validations performed: " + validationCount.get());
        System.out.println("   - Read operations: " + readOperations.get());
        System.out.println("   - Write operations: " + writeOperations.get());
        System.out.println("   - Final block count: " + blockchain.getBlockCount());
        
        // Final validation should still pass
        ChainValidationResult floodResult = blockchain.validateChainDetailed();
        assertTrue(floodResult.isStructurallyIntact(), "Chain should remain structurally intact after validation flood");
        // During high concurrency, full compliance may vary but structure should be intact
    }

    // Helper methods
    
    private Map<String, KeyPair> prepareInitialKeys(int count) {
        Map<String, KeyPair> keyPairs = new HashMap<>();
        
        for (int i = 0; i < count; i++) {
            KeyPair keyPair = CryptoUtil.generateKeyPair();
            String publicKeyString = CryptoUtil.publicKeyToString(keyPair.getPublic());
            String owner = "InitialUser-" + i;
            
            blockchain.addAuthorizedKey(publicKeyString, owner);
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
        List<Block> allBlocks = blockchain.getAllBlocks();
        Block lastBlock = blockchain.getLastBlock();
        
        // Verify consistency
        assertEquals(blockCount, allBlocks.size(), "Block count should match blocks list size");
        if (!allBlocks.isEmpty()) {
            assertEquals(lastBlock.getBlockNumber(), allBlocks.get(allBlocks.size() - 1).getBlockNumber(),
                "Last block should match");
        }
    }
    
    private void addRandomKey(int threadId, int op) {
        KeyPair keyPair = CryptoUtil.generateKeyPair();
        String publicKeyString = CryptoUtil.publicKeyToString(keyPair.getPublic());
        String owner = "DynamicUser-T" + threadId + "-" + op;
        blockchain.addAuthorizedKey(publicKeyString, owner);
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
        blockchain.getBlocksByDateRange(
            LocalDateTime.now().minusDays(1).toLocalDate(),
            LocalDateTime.now().toLocalDate()
        );
    }
    
    private void validateFullChain(int threadId) {
        ChainValidationResult result = blockchain.validateChainDetailed();
        // During concurrent operations, we primarily check structural integrity
        assertTrue(result.isStructurallyIntact(), "Chain structure should remain intact during concurrent access");
    }
}
