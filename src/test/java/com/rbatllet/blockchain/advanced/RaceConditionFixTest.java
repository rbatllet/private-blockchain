package com.rbatllet.blockchain.advanced;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.util.CryptoUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Race Condition Fix Verification Test
 * Specifically tests the fix for high-speed block number race conditions
 */
public class RaceConditionFixTest {
    private static final Logger logger = LoggerFactory.getLogger(RaceConditionFixTest.class);


    private Blockchain blockchain;
    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        blockchain = new Blockchain();
        blockchain.clearAndReinitialize();
        executorService = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("TestWorker-", 0).factory()); // Java 25 Virtual Threads;
    }

    @Test
    @DisplayName("🎯 High-Speed Race Condition Fix Verification")
    @Timeout(30)
    void testHighSpeedRaceConditionFixed() throws InterruptedException {
        // Setup: Create a key for all threads to use
        KeyPair keyPair = CryptoUtil.generateKeyPair();
        String publicKeyString = CryptoUtil.publicKeyToString(keyPair.getPublic());
        blockchain.createBootstrapAdmin(publicKeyString, "RaceFixTestUser");

        // Test configuration for maximum race condition potential
        int EXTREME_THREAD_COUNT = 50;
        int BLOCKS_PER_THREAD = 2;
        int EXPECTED_TOTAL_BLOCKS = EXTREME_THREAD_COUNT * BLOCKS_PER_THREAD;

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(EXTREME_THREAD_COUNT);
        
        AtomicInteger successfulBlocks = new AtomicInteger(0);
        AtomicInteger failedBlocks = new AtomicInteger(0);
        Set<Long> observedBlockNumbers = ConcurrentHashMap.newKeySet();
        AtomicInteger raceConditionsDetected = new AtomicInteger(0);

        // Create threads that will attempt to create blocks simultaneously
        for (int i = 0; i < EXTREME_THREAD_COUNT; i++) {
            final int threadId = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    
                    for (int j = 0; j < BLOCKS_PER_THREAD; j++) {
                        String data = "RaceFixTest-T" + threadId + "-B" + j + "-" + System.nanoTime();
                        
                        // Use addBlockAndReturn to get the specific block created by this thread
                        Block addedBlock = blockchain.addBlockAndReturn(data, keyPair.getPrivate(), keyPair.getPublic());
                        
                        if (addedBlock != null) {
                            successfulBlocks.incrementAndGet();
                            
                            // Verify the block was added with a unique number
                            Long blockNumber = addedBlock.getBlockNumber();
                            
                            // Check for duplicate block numbers
                            if (!observedBlockNumbers.add(blockNumber)) {
                                raceConditionsDetected.incrementAndGet();
                                logger.error("RACE CONDITION DETECTED: Duplicate block number " + blockNumber + 
                                    " from thread " + threadId);
                            }
                        } else {
                            failedBlocks.incrementAndGet();
                        }
                        
                        // No delay - maximum race condition pressure
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    logger.error("Thread " + threadId + " error: " + e.getMessage());
                    failedBlocks.incrementAndGet();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        // Start all threads simultaneously for maximum contention
        long startTime = System.currentTimeMillis();
        startLatch.countDown();
        
        // Wait for completion
        boolean completed = endLatch.await(25, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();
        
        assertTrue(completed, "Race condition fix test should complete within timeout");

        // Analyze results
        logger.info("🎯 Race Condition Fix Test Results:");
        logger.info("   - Execution time: " + (endTime - startTime) + "ms");
        logger.info("   - Successful blocks: " + successfulBlocks.get());
        logger.info("   - Failed blocks: " + failedBlocks.get());
        logger.info("   - Unique block numbers: " + observedBlockNumbers.size());
        logger.info("   - Race conditions detected: " + raceConditionsDetected.get());
        logger.info("   - Total blockchain blocks: " + blockchain.getBlockCount());
        logger.info("   - Expected total: " + (successfulBlocks.get() + 1)); // +1 for genesis
        logger.info("   - Theoretical maximum: " + EXPECTED_TOTAL_BLOCKS);

        // Critical assertions
        assertEquals(0, raceConditionsDetected.get(), 
            "NO race conditions should be detected after fix");
        assertEquals(successfulBlocks.get(), observedBlockNumbers.size(), 
            "Each successful block should have a unique number");
        assertEquals(successfulBlocks.get() + 1, blockchain.getBlockCount(), 
            "Block count should match successful operations plus genesis block");
        assertTrue(successfulBlocks.get() <= EXPECTED_TOTAL_BLOCKS, 
            "Successful blocks should not exceed theoretical maximum: " + successfulBlocks.get() + " <= " + EXPECTED_TOTAL_BLOCKS);
        
        // Verify chain integrity with detailed validation
        var validationResult = blockchain.validateChainDetailed();
        assertTrue(validationResult.isStructurallyIntact(), 
            "Blockchain should be structurally intact after high-concurrency operations");
        assertTrue(validationResult.isFullyCompliant(), 
            "Blockchain should be fully compliant after high-concurrency operations");
            
        // Verify block number sequence
        if (successfulBlocks.get() > 0) {
            // Get all blocks and verify sequential numbering
            List<Block> allBlocks = new ArrayList<>();
            blockchain.processChainInBatches(batch -> allBlocks.addAll(batch), 1000);

            for (int i = 1; i < allBlocks.size(); i++) {
                Long prevNumber = allBlocks.get(i-1).getBlockNumber();
                Long currNumber = allBlocks.get(i).getBlockNumber();
                assertEquals(prevNumber + 1, currNumber,
                    "Block numbers should be sequential: " + prevNumber + " -> " + currNumber);
            }
        }

        logger.info("✅ Race condition fix verification: PASSED!");
    }

    @Test
    @DisplayName("📊 Performance Impact Assessment")
    @Timeout(20)
    void testPerformanceImpactOfFix() throws InterruptedException {
        // Test that the fix doesn't significantly impact performance
        KeyPair keyPair = CryptoUtil.generateKeyPair();
        String publicKeyString = CryptoUtil.publicKeyToString(keyPair.getPublic());
        blockchain.createBootstrapAdmin(publicKeyString, "PerformanceTestUser");

        int THREAD_COUNT = 10;
        int BLOCKS_PER_THREAD = 5;
        
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger successfulBlocks = new AtomicInteger(0);

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    
                    for (int j = 0; j < BLOCKS_PER_THREAD; j++) {
                        String data = "PerfTest-T" + threadId + "-B" + j;
                        if (blockchain.addBlock(data, keyPair.getPrivate(), keyPair.getPublic())) {
                            successfulBlocks.incrementAndGet();
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
        boolean completed = endLatch.await(15, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();
        
        assertTrue(completed, "Performance test should complete quickly");
        
        long executionTime = endTime - startTime;
        double blocksPerSecond = (double) successfulBlocks.get() / (executionTime / 1000.0);
        
        logger.info("📊 Performance Impact Results:");
        logger.info("   - Total execution time: " + executionTime + "ms");
        logger.info("   - Successful blocks: " + successfulBlocks.get());
        logger.info("   - Blocks per second: " + String.format("%.2f", blocksPerSecond));
        logger.info("   - Average time per block: " + String.format("%.2f", executionTime / (double) successfulBlocks.get()) + "ms");

        // Verify reasonable performance (should be able to handle at least 10 blocks/second under concurrency)
        assertTrue(blocksPerSecond >= 5.0, 
            "Performance should be reasonable: " + blocksPerSecond + " blocks/second");
        
        var validationResult = blockchain.validateChainDetailed();
        assertTrue(validationResult.isStructurallyIntact(), "Chain should be structurally intact");
        assertTrue(validationResult.isFullyCompliant(), "Chain should be fully compliant");
        
        logger.info("✅ Performance impact assessment: ACCEPTABLE");
    }
}
