package com.rbatllet.blockchain.advanced;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.util.CryptoUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.security.KeyPair;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safety test for the blockchain
 * This test verifies that concurrent operations work correctly
 */
public class ThreadSafetyTest {
    
    private static final int THREAD_COUNT = 10;
    private static final int BLOCKS_PER_THREAD = 5;
    
    @Test
    public void testBlockchainThreadSafety() {
        System.out.println("🧪 Starting Thread-Safety Test for Blockchain");
        System.out.println("Threads: " + THREAD_COUNT + ", Blocks per thread: " + BLOCKS_PER_THREAD);
        
        try {
            // Initialize blockchain
            Blockchain blockchain = new Blockchain();
            blockchain.clearAndReinitialize();
            
            // Generate test key pair
            KeyPair keyPair = CryptoUtil.generateKeyPair();
            String publicKeyString = CryptoUtil.publicKeyToString(keyPair.getPublic());
            
            // Add authorized key
            boolean keyAdded = blockchain.addAuthorizedKey(publicKeyString, "TestUser");
            if (!keyAdded) {
                System.err.println("❌ Failed to add authorized key");
                return;
            }
            System.out.println("✅ Authorized key added successfully");
            
            // Test concurrent block addition
            testConcurrentBlockAddition(blockchain, keyPair);
            
            // Test concurrent key operations
            testConcurrentKeyOperations(blockchain);
            
            // Test concurrent read operations
            testConcurrentReadOperations(blockchain);
            
            // Final validation
            boolean isValid = blockchain.validateChain();
            System.out.println("🔍 Final chain validation: " + (isValid ? "✅ SUCCESS" : "❌ FAILED"));
            
            System.out.println("📊 Final blockchain stats:");
            System.out.println("   - Total blocks: " + blockchain.getBlockCount());
            System.out.println("   - Expected blocks: " + (1 + THREAD_COUNT * BLOCKS_PER_THREAD)); // +1 for genesis
            
            if (isValid && blockchain.getBlockCount() == (1 + THREAD_COUNT * BLOCKS_PER_THREAD)) {
                System.out.println("🎉 Thread-safety test PASSED!");
            } else {
                System.out.println("💥 Thread-safety test FAILED!");
            }
            
        } catch (Exception e) {
            System.err.println("💥 Test failed with exception: " + e.getMessage());
            e.printStackTrace();
        }
    }    
    private static void testConcurrentBlockAddition(Blockchain blockchain, KeyPair keyPair) {
        System.out.println("\n🧪 Testing concurrent block addition...");
        
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
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
                            System.out.println("✅ Thread " + threadId + " added block " + j);
                        } else {
                            failureCount.incrementAndGet();
                            System.err.println("❌ Thread " + threadId + " failed to add block " + j);
                        }
                        
                        // Small delay to increase chance of race conditions
                        Thread.sleep(10);
                    }
                } catch (Exception e) {
                    System.err.println("💥 Thread " + threadId + " exception: " + e.getMessage());
                    failureCount.addAndGet(BLOCKS_PER_THREAD);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        try {
            latch.await();
            executor.shutdown();
            
            System.out.println("📊 Concurrent block addition results:");
            System.out.println("   - Successful blocks: " + successCount.get());
            System.out.println("   - Failed blocks: " + failureCount.get());
            System.out.println("   - Expected blocks: " + (THREAD_COUNT * BLOCKS_PER_THREAD));
            
        } catch (InterruptedException e) {
            System.err.println("Test interrupted: " + e.getMessage());
        }
    }
    
    private static void testConcurrentKeyOperations(Blockchain blockchain) {
        System.out.println("\n🧪 Testing concurrent key operations...");
        
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
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
                    boolean added = blockchain.addAuthorizedKey(publicKeyString, ownerName);
                    if (added) {
                        System.out.println("✅ Thread " + threadId + " added key for " + ownerName);
                        
                        // Wait a bit
                        Thread.sleep(50);
                        
                        // Revoke key
                        boolean revoked = blockchain.revokeAuthorizedKey(publicKeyString);
                        if (revoked) {
                            System.out.println("✅ Thread " + threadId + " revoked key for " + ownerName);
                            successCount.incrementAndGet();
                        } else {
                            System.err.println("❌ Thread " + threadId + " failed to revoke key");
                        }
                    } else {
                        System.err.println("❌ Thread " + threadId + " failed to add key");
                    }
                    
                } catch (Exception e) {
                    System.err.println("💥 Thread " + threadId + " key operation exception: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        
        try {
            latch.await();
            executor.shutdown();
            
            System.out.println("📊 Concurrent key operations results:");
            System.out.println("   - Successful operations: " + successCount.get());
            System.out.println("   - Expected operations: " + THREAD_COUNT);
            
        } catch (InterruptedException e) {
            System.err.println("Key operations test interrupted: " + e.getMessage());
        }
    }
    
    private static void testConcurrentReadOperations(Blockchain blockchain) {
        System.out.println("\n🧪 Testing concurrent read operations...");
        
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);
        
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    // Perform various read operations
                    long blockCount = blockchain.getBlockCount();
                    var allBlocks = blockchain.getAllBlocks();
                    var lastBlock = blockchain.getLastBlock();
                    var authorizedKeys = blockchain.getAuthorizedKeys();
                    
                    // Validate consistency
                    if (allBlocks.size() == blockCount && lastBlock != null) {
                        System.out.println("✅ Thread " + threadId + " read operations consistent");
                        successCount.incrementAndGet();
                    } else {
                        System.err.println("❌ Thread " + threadId + " read operations inconsistent");
                    }
                    
                } catch (Exception e) {
                    System.err.println("💥 Thread " + threadId + " read exception: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        
        try {
            latch.await();
            executor.shutdown();
            
            System.out.println("📊 Concurrent read operations results:");
            System.out.println("   - Consistent reads: " + successCount.get());
            System.out.println("   - Expected consistent reads: " + THREAD_COUNT);
            
        } catch (InterruptedException e) {
            System.err.println("Read operations test interrupted: " + e.getMessage());
        }
    }
    
    /**
     * Main method for running the test directly
     */
    public static void main(String[] args) {
        ThreadSafetyTest test = new ThreadSafetyTest();
        test.testBlockchainThreadSafety();
    }
}