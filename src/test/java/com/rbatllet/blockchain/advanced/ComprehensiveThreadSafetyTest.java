package com.rbatllet.blockchain.advanced;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.util.CryptoUtil;
import org.junit.jupiter.api.*;

import java.security.KeyPair;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive Thread Safety Test Suite
 * Designed to discover hidden race conditions and threading issues
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ComprehensiveThreadSafetyTest {

    private Blockchain blockchain;
    private ExecutorService executorService;
    
    private static final int EXTREME_THREAD_COUNT = 100;
    
    @BeforeEach
    void setUp() {
        blockchain = new Blockchain();
        blockchain.clearAndReinitialize();  // Ensure clean state for each test
        // Use cached thread pool for maximum thread contention testing
        executorService = Executors.newCachedThreadPool();
    }
    
    @AfterEach
    void tearDown() {
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
            }
        }
    }

    @Test
    @Order(1)
    @DisplayName("🎯 Sequential Block Number Verification")
    @Timeout(60)
    void testSequentialBlockNumberGeneration() throws InterruptedException {
        System.out.println("🎯 Testing sequential block number generation under extreme concurrency...");
        
        KeyPair keyPair = CryptoUtil.generateKeyPair();
        String publicKeyString = CryptoUtil.publicKeyToString(keyPair.getPublic());
        blockchain.addAuthorizedKey(publicKeyString, "SequentialTestUser");
        
        int totalBlocks = 200;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(EXTREME_THREAD_COUNT);
        
        AtomicInteger successfulBlocks = new AtomicInteger(0);
        AtomicInteger failedBlocks = new AtomicInteger(0);
        ConcurrentLinkedQueue<Long> generatedNumbers = new ConcurrentLinkedQueue<>();
        
        // Create extreme thread contention
        for (int i = 0; i < EXTREME_THREAD_COUNT; i++) {
            final int threadId = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    
                    // Each thread tries to add multiple blocks rapidly
                    for (int j = 0; j < totalBlocks / EXTREME_THREAD_COUNT; j++) {
                        String data = "Sequential-T" + threadId + "-B" + j + "-" + System.nanoTime();
                        
                        // FIXED: Use addBlockAndReturn to get the specific block created by this thread
                        Block addedBlock = blockchain.addBlockAndReturn(data, keyPair.getPrivate(), keyPair.getPublic());
                        
                        if (addedBlock != null) {
                            successfulBlocks.incrementAndGet();
                            
                            // Get the actual block number from the created block
                            generatedNumbers.offer(addedBlock.getBlockNumber());
                        } else {
                            failedBlocks.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Thread " + threadId + " error: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    endLatch.countDown();
                }
            });
        }
        
        long startTime = System.currentTimeMillis();
        startLatch.countDown();
        
        assertTrue(endLatch.await(55, TimeUnit.SECONDS), "Test should complete within timeout");
        long endTime = System.currentTimeMillis();
        
        // Analyze results
        List<Long> sortedNumbers = new ArrayList<>(generatedNumbers);
        Collections.sort(sortedNumbers);
        
        System.out.println("📊 Sequential Test Results:");
        System.out.println("   - Execution time: " + (endTime - startTime) + "ms");
        System.out.println("   - Successful blocks: " + successfulBlocks.get());
        System.out.println("   - Failed blocks: " + failedBlocks.get());
        System.out.println("   - Unique numbers generated: " + new HashSet<>(generatedNumbers).size());
        System.out.println("   - Total blockchain blocks: " + blockchain.getBlockCount());
        
        // Critical verifications
        assertEquals(successfulBlocks.get(), new HashSet<>(generatedNumbers).size(), 
            "All successful blocks must have unique numbers");
        
        // Verify sequential nature
        for (int i = 1; i < sortedNumbers.size(); i++) {
            assertTrue(sortedNumbers.get(i) > sortedNumbers.get(i-1), 
                "Block numbers must be in ascending order");
        }
        
        // Verify no gaps in the sequence (allowing for genesis block)
        if (!sortedNumbers.isEmpty()) {
            assertEquals(1L, (long) sortedNumbers.get(0), "First block should be number 1");
            assertEquals(sortedNumbers.size(), (long) sortedNumbers.get(sortedNumbers.size() - 1), 
                "Last block number should equal total blocks created");
        }
    }

    @Test
    @Order(2)
    @DisplayName("🎯 Block Number Uniqueness Under Extreme Load")
    @Timeout(300)
    void testBlockNumberUniquenessExtremeLoad() throws InterruptedException {
        System.out.println("🎯 Testing block number uniqueness under EXTREME load...");

        KeyPair keyPair = CryptoUtil.generateKeyPair();
        String publicKey = CryptoUtil.publicKeyToString(keyPair.getPublic());
        blockchain.addAuthorizedKey(publicKey, "UniquenessUser");

        // EXTREME load: 200 threads each trying to add 5 blocks (1000 total)
        int threadCount = 200;
        int blocksPerThread = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        
        // Use ConcurrentLinkedQueue to capture block numbers in order of creation
        ConcurrentLinkedQueue<Long> blockCreationOrder = new ConcurrentLinkedQueue<>();
        AtomicInteger successfulBlocks = new AtomicInteger(0);
        AtomicInteger failedBlocks = new AtomicInteger(0);
        
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    
                    for (int j = 0; j < blocksPerThread; j++) {
                        try {
                            String data = "UniqueLoad-T" + threadId + "-B" + j + "-" + System.nanoTime();
                            
                            // FIXED: Use addBlockAndReturn to get the specific block created by this thread
                            Block addedBlock = blockchain.addBlockAndReturn(data, keyPair.getPrivate(), keyPair.getPublic());
                            
                            if (addedBlock != null) {
                                successfulBlocks.incrementAndGet();
                                
                                // Capture the block number from the specific block created by this thread
                                blockCreationOrder.offer(addedBlock.getBlockNumber());
                            } else {
                                failedBlocks.incrementAndGet();
                            }
                        } catch (Exception e) {
                            failedBlocks.incrementAndGet();
                            System.err.println("Thread " + threadId + " error: " + e.getMessage());
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
        
        assertTrue(endLatch.await(280, TimeUnit.SECONDS), "Extreme load test should complete within extended timeout");
        long endTime = System.currentTimeMillis();
        
        // Analysis
        List<Long> createdNumbers = new ArrayList<>(blockCreationOrder);
        Set<Long> uniqueNumbers = new HashSet<>(createdNumbers);
        
        // Find duplicates if any
        Set<Long> seen = new HashSet<>();
        Set<Long> duplicates = new HashSet<>();
        for (Long num : createdNumbers) {
            if (!seen.add(num)) {
                duplicates.add(num);
            }
        }
        
        System.out.println("📊 Extreme Load Uniqueness Results:");
        System.out.println("   - Execution time: " + (endTime - startTime) + "ms");
        System.out.println("   - Total threads: " + threadCount);
        System.out.println("   - Blocks per thread: " + blocksPerThread);
        System.out.println("   - Expected total blocks: " + (threadCount * blocksPerThread));
        System.out.println("   - Successful blocks: " + successfulBlocks.get());
        System.out.println("   - Failed blocks: " + failedBlocks.get());
        System.out.println("   - Created block numbers: " + createdNumbers.size());
        System.out.println("   - Unique block numbers: " + uniqueNumbers.size());
        System.out.println("   - Duplicate numbers found: " + duplicates.size());
        System.out.println("   - Final blockchain size: " + blockchain.getBlockCount());
        
        if (!duplicates.isEmpty()) {
            System.err.println("   - Duplicate numbers: " + duplicates);
        }
        
        // Critical assertions
        assertEquals(0, duplicates.size(), "No duplicate block numbers should exist: " + duplicates);
        assertEquals(successfulBlocks.get(), uniqueNumbers.size(), 
            "Number of successful blocks must equal unique numbers");
        assertEquals(successfulBlocks.get(), createdNumbers.size(), 
            "All successful operations should be captured");
        
        // Verify blockchain integrity
        Set<Long> chainNumbers = new HashSet<>();
        List<Block> finalChain = new ArrayList<>();
        blockchain.processChainInBatches(batch -> {
            for (Block block : batch) {
                assertFalse(chainNumbers.contains(block.getBlockNumber()),
                    "Blockchain should contain no duplicate numbers");
                chainNumbers.add(block.getBlockNumber());
                finalChain.add(block);
            }
        }, 1000);

        var validationResult = blockchain.validateChainDetailed();
        assertTrue(validationResult.isStructurallyIntact(), "Chain must be structurally intact after extreme load");
        assertTrue(validationResult.isFullyCompliant(), "Chain must be fully compliant after extreme load");
    }
}
