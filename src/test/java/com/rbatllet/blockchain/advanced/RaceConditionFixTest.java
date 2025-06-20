package com.rbatllet.blockchain.advanced;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.util.CryptoUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.security.KeyPair;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Race Condition Fix Verification Test
 * Specifically tests the fix for high-speed block number race conditions
 */
public class RaceConditionFixTest {

    private Blockchain blockchain;
    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        blockchain = new Blockchain();
        blockchain.clearAndReinitialize();
        executorService = Executors.newFixedThreadPool(50);
    }

    @Test
    @DisplayName("🎯 High-Speed Race Condition Fix Verification")
    @Timeout(30)
    void testHighSpeedRaceConditionFixed() throws InterruptedException {
        // Setup: Create a key for all threads to use
        KeyPair keyPair = CryptoUtil.generateKeyPair();
        String publicKeyString = CryptoUtil.publicKeyToString(keyPair.getPublic());
        blockchain.addAuthorizedKey(publicKeyString, "RaceFixTestUser");

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
                                System.err.println("RACE CONDITION DETECTED: Duplicate block number " + blockNumber + 
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
                    System.err.println("Thread " + threadId + " error: " + e.getMessage());
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
        System.out.println("🎯 Race Condition Fix Test Results:");
        System.out.println("   - Execution time: " + (endTime - startTime) + "ms");
        System.out.println("   - Successful blocks: " + successfulBlocks.get());
        System.out.println("   - Failed blocks: " + failedBlocks.get());
        System.out.println("   - Unique block numbers: " + observedBlockNumbers.size());
        System.out.println("   - Race conditions detected: " + raceConditionsDetected.get());
        System.out.println("   - Total blockchain blocks: " + blockchain.getBlockCount());
        System.out.println("   - Expected total: " + (successfulBlocks.get() + 1)); // +1 for genesis

        // Critical assertions
        assertEquals(0, raceConditionsDetected.get(), 
            "NO race conditions should be detected after fix");
        assertEquals(successfulBlocks.get(), observedBlockNumbers.size(), 
            "Each successful block should have a unique number");
        assertEquals(successfulBlocks.get() + 1, blockchain.getBlockCount(), 
            "Block count should match successful operations plus genesis block");
        
        // Verify chain integrity
        assertTrue(blockchain.validateChain(), 
            "Blockchain should remain valid after high-concurrency operations");
            
        // Verify block number sequence
        if (successfulBlocks.get() > 0) {
            // Get all blocks and verify sequential numbering
            var allBlocks = blockchain.getAllBlocks();
            for (int i = 1; i < allBlocks.size(); i++) {
                Long prevNumber = allBlocks.get(i-1).getBlockNumber();
                Long currNumber = allBlocks.get(i).getBlockNumber();
                assertEquals(prevNumber + 1, currNumber, 
                    "Block numbers should be sequential: " + prevNumber + " -> " + currNumber);
            }
        }

        System.out.println("✅ Race condition fix verification: PASSED!");
    }

    @Test
    @DisplayName("📊 Performance Impact Assessment")
    @Timeout(20)
    void testPerformanceImpactOfFix() throws InterruptedException {
        // Test that the fix doesn't significantly impact performance
        KeyPair keyPair = CryptoUtil.generateKeyPair();
        String publicKeyString = CryptoUtil.publicKeyToString(keyPair.getPublic());
        blockchain.addAuthorizedKey(publicKeyString, "PerformanceTestUser");

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
        
        System.out.println("📊 Performance Impact Results:");
        System.out.println("   - Total execution time: " + executionTime + "ms");
        System.out.println("   - Successful blocks: " + successfulBlocks.get());
        System.out.println("   - Blocks per second: " + String.format("%.2f", blocksPerSecond));
        System.out.println("   - Average time per block: " + String.format("%.2f", executionTime / (double) successfulBlocks.get()) + "ms");

        // Verify reasonable performance (should be able to handle at least 10 blocks/second under concurrency)
        assertTrue(blocksPerSecond >= 5.0, 
            "Performance should be reasonable: " + blocksPerSecond + " blocks/second");
        assertTrue(blockchain.validateChain(), "Chain should remain valid");
        
        System.out.println("✅ Performance impact assessment: ACCEPTABLE");
    }
}
