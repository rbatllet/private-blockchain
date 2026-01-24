package com.rbatllet.blockchain.advanced;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.util.CryptoUtil;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Thread-safety test for the blockchain
 * This test verifies that concurrent operations work correctly
 */
public class ThreadSafetyTest {
    private static final Logger logger = LoggerFactory.getLogger(ThreadSafetyTest.class);

    
    private static final int THREAD_COUNT = 10;
    private static final int BLOCKS_PER_THREAD = 5;
    
    @Test
    public void testBlockchainThreadSafety() {
        logger.info("🧪 Starting Thread-Safety Test for Blockchain");
        logger.info("Threads: " + THREAD_COUNT + ", Blocks per thread: " + BLOCKS_PER_THREAD);
        
        try {
            // Initialize blockchain
            Blockchain blockchain = new Blockchain();
            blockchain.clearAndReinitialize();
            
            // Generate test key pair
            KeyPair keyPair = CryptoUtil.generateKeyPair();
            String publicKeyString = CryptoUtil.publicKeyToString(keyPair.getPublic());
            
            // Add authorized key
            boolean keyAdded = blockchain.createBootstrapAdmin(publicKeyString, "TestUser");
            if (!keyAdded) {
                logger.error("❌ Failed to add authorized key");
                return;
            }
            logger.info("✅ Authorized key added successfully");
            
            // Test concurrent block addition
            testConcurrentBlockAddition(blockchain, keyPair);
            
            // Test concurrent key operations
            testConcurrentKeyOperations(blockchain);
            
            // Test concurrent read operations
            testConcurrentReadOperations(blockchain);
            
            // Final validation with detailed results
            var validationResult = blockchain.validateChainDetailed();
            boolean isStructurallyIntact = validationResult.isStructurallyIntact();
            boolean isFullyCompliant = validationResult.isFullyCompliant();
            
            logger.info("🔍 Final chain validation:");
            logger.info("   - Structurally intact: " + (isStructurallyIntact ? "✅ YES" : "❌ NO"));
            logger.info("   - Fully compliant: " + (isFullyCompliant ? "✅ YES" : "❌ NO"));
            if (!isFullyCompliant) {
                logger.info("   - Revoked blocks: " + validationResult.getRevokedBlocks());
            }
            if (!isStructurallyIntact) {
                logger.info("   - Invalid blocks: " + validationResult.getInvalidBlocks());
            }
            
            logger.info("📊 Final blockchain stats:");
            logger.info("   - Total blocks: " + blockchain.getBlockCount());
            logger.info("   - Expected blocks: " + (1 + THREAD_COUNT * BLOCKS_PER_THREAD)); // +1 for genesis
            
            if (isStructurallyIntact && blockchain.getBlockCount() == (1 + THREAD_COUNT * BLOCKS_PER_THREAD)) {
                logger.info("🎉 Thread-safety test PASSED!");
            } else {
                logger.info("💥 Thread-safety test FAILED!");
            }
            
        } catch (Exception e) {
            logger.error("💥 Test failed with exception: " + e.getMessage());
            e.printStackTrace();
        }
    }    
    private static void testConcurrentBlockAddition(Blockchain blockchain, KeyPair keyPair) {
        logger.info("\n🧪 Testing concurrent block addition...");
        
        ExecutorService executor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("TestWorker-", 0).factory()); // Java 25 Virtual Threads;
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < BLOCKS_PER_THREAD; j++) {
                        String data = "Thread-" + threadId + "-Block-" + j + "-Data";
                        boolean success = blockchain.addBlock(data, keyPair.getPrivate(), keyPair.getPublic());
                        
                        if (success) {
                            successCount.incrementAndGet();
                            logger.info("✅ Thread " + threadId + " added block " + j);
                        } else {
                            failureCount.incrementAndGet();
                            logger.error("❌ Thread " + threadId + " failed to add block " + j);
                        }
                        
                        // Small delay to increase chance of race conditions
                        Thread.sleep(10);
                    }
                } catch (Exception e) {
                    logger.error("💥 Thread " + threadId + " exception: " + e.getMessage());
                    failureCount.addAndGet(BLOCKS_PER_THREAD);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        try {
            latch.await();
            executor.shutdown();
            
            logger.info("📊 Concurrent block addition results:");
            logger.info("   - Successful blocks: " + successCount.get());
            logger.info("   - Failed blocks: " + failureCount.get());
            logger.info("   - Expected blocks: " + (THREAD_COUNT * BLOCKS_PER_THREAD));
            
        } catch (InterruptedException e) {
            logger.error("Test interrupted: " + e.getMessage());
        }
    }
    
    private static void testConcurrentKeyOperations(Blockchain blockchain) {
        logger.info("\n🧪 Testing concurrent key operations...");
        
        ExecutorService executor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("TestWorker-", 0).factory()); // Java 25 Virtual Threads;
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);
        
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    // Generate unique key for each thread
                    KeyPair keyPair = CryptoUtil.generateKeyPair();
                    String publicKeyString = CryptoUtil.publicKeyToString(keyPair.getPublic());
                    String ownerName = "ConcurrentUser-" + threadId;
                    
                    // Add key
                    boolean added = blockchain.createBootstrapAdmin(publicKeyString, ownerName);
                    if (added) {
                        logger.info("✅ Thread " + threadId + " added key for " + ownerName);
                        
                        // Wait a bit
                        Thread.sleep(50);
                        
                        // Revoke key
                        boolean revoked = blockchain.revokeAuthorizedKey(publicKeyString);
                        if (revoked) {
                            logger.info("✅ Thread " + threadId + " revoked key for " + ownerName);
                            successCount.incrementAndGet();
                        } else {
                            logger.error("❌ Thread " + threadId + " failed to revoke key");
                        }
                    } else {
                        logger.error("❌ Thread " + threadId + " failed to add key");
                    }
                    
                } catch (Exception e) {
                    logger.error("💥 Thread " + threadId + " key operation exception: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        
        try {
            latch.await();
            executor.shutdown();
            
            logger.info("📊 Concurrent key operations results:");
            logger.info("   - Successful operations: " + successCount.get());
            logger.info("   - Expected operations: " + THREAD_COUNT);
            
        } catch (InterruptedException e) {
            logger.error("Key operations test interrupted: " + e.getMessage());
        }
    }
    
    private static void testConcurrentReadOperations(Blockchain blockchain) {
        logger.info("\n🧪 Testing concurrent read operations...");

        ExecutorService executor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("TestWorker-", 0).factory()); // Java 25 Virtual Threads;
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    // Perform various read operations
                    long blockCount = blockchain.getBlockCount();
                    var lastBlock = blockchain.getLastBlock();
                    var authorizedKeys = blockchain.getAuthorizedKeys();

                    // Validate consistency
                    if (blockCount > 0 && lastBlock != null && authorizedKeys != null) {
                        logger.info("✅ Thread " + threadId + " read operations consistent");
                        successCount.incrementAndGet();
                    } else {
                        logger.error("❌ Thread " + threadId + " read operations inconsistent");
                    }

                } catch (Exception e) {
                    logger.error("💥 Thread " + threadId + " read exception: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        
        try {
            latch.await();
            executor.shutdown();
            
            logger.info("📊 Concurrent read operations results:");
            logger.info("   - Consistent reads: " + successCount.get());
            logger.info("   - Expected consistent reads: " + THREAD_COUNT);
            
        } catch (InterruptedException e) {
            logger.error("Read operations test interrupted: " + e.getMessage());
        }
    }
    
    /**
     * Main method for running the test directly
     */
    public static void main(String[] args) {
        ThreadSafetyTest test = new ThreadSafetyTest();
        test.testBlockchainThreadSafety();
        
        // Force exit to stop background threads
        System.exit(0);
    }
}