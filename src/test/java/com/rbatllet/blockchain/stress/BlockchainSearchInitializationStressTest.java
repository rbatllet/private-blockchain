package com.rbatllet.blockchain.stress;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.security.KeyFileLoader;
import com.rbatllet.blockchain.security.UserRole;
import com.rbatllet.blockchain.util.CryptoUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyPair;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Stress tests for Blockchain search initialization thread safety
 * Validates read lock synchronization in initializeAdvancedSearch() methods
 */
public class BlockchainSearchInitializationStressTest {
    
    private static final Logger logger = LoggerFactory.getLogger(BlockchainSearchInitializationStressTest.class);
    
    private Blockchain blockchain;
    private ExecutorService executorService;
    private KeyPair testKeyPair;
    private KeyPair bootstrapKeyPair;
    private String testUsername = "searchTestUser";
    
    @BeforeEach
    void setUp() throws Exception {
        blockchain = new Blockchain();

        // RBAC FIX (v1.0.6): Clear database before bootstrap to avoid "Existing users" error
        blockchain.clearAndReinitialize();

        executorService = Executors.newCachedThreadPool();
        testKeyPair = CryptoUtil.generateKeyPair();

        // Load bootstrap admin keys
        bootstrapKeyPair = KeyFileLoader.loadKeyPairFromFiles(
            "./keys/genesis-admin.private",
            "./keys/genesis-admin.public"
        );

        // SECURITY (v1.0.6): Register bootstrap admin in blockchain (REQUIRED!)
        blockchain.createBootstrapAdmin(
            CryptoUtil.publicKeyToString(bootstrapKeyPair.getPublic()),
            "BOOTSTRAP_ADMIN"
        );

        // SECURITY FIX (v1.0.6): Pre-authorize user before creating blocks
        String publicKeyString = CryptoUtil.publicKeyToString(testKeyPair.getPublic());
        blockchain.addAuthorizedKey(publicKeyString, testUsername, bootstrapKeyPair, UserRole.USER);

        // Create some test data for search initialization
        for (int i = 0; i < 5; i++) {
            blockchain.addEncryptedBlock(
                "Test data for search " + i,
                "testPassword" + i,
                testKeyPair.getPrivate(),
                testKeyPair.getPublic()
            );
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
                Thread.currentThread().interrupt();
                executorService.shutdownNow();
            }
        }
    }
    
    @Test
    @DisplayName("🧪 Stress test: Concurrent search initialization calls")
    @Timeout(120)
    void testConcurrentSearchInitialization() throws Exception {
        logger.info("🚀 Starting concurrent search initialization stress test...");
        
        final int NUM_THREADS = 10;
        final int OPERATIONS_PER_THREAD = 5;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch completionLatch = new CountDownLatch(NUM_THREADS);
        final AtomicInteger successfulInitializations = new AtomicInteger(0);
        final AtomicInteger errors = new AtomicInteger(0);
        
        // Submit all tasks
        for (int i = 0; i < NUM_THREADS; i++) {
            final int threadId = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    
                    for (int op = 0; op < OPERATIONS_PER_THREAD; op++) {
                        try {
                            // Alternate between the two initialization methods
                            if (op % 2 == 0) {
                                blockchain.initializeAdvancedSearch();
                            } else {
                                blockchain.initializeAdvancedSearch("testPassword" + (op % 5));
                            }
                            
                            successfulInitializations.incrementAndGet();
                            
                            // Small delay to increase contention
                            Thread.sleep(1);
                            
                        } catch (Exception e) {
                            logger.error("❌ Error in search initialization thread {}, operation {}", threadId, op, e);
                            errors.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    errors.incrementAndGet();
                } finally {
                    completionLatch.countDown();
                }
            });
        }
        
        // Start all threads simultaneously
        logger.info("🏁 Starting {} threads with {} initialization calls each...", NUM_THREADS, OPERATIONS_PER_THREAD);
        startLatch.countDown();
        
        // Wait for completion
        assertTrue(completionLatch.await(60, TimeUnit.SECONDS), 
                  "Search initialization stress test should complete within timeout");
        
        // Validate results
        int expectedOperations = NUM_THREADS * OPERATIONS_PER_THREAD;
        logger.info("📊 Results: {} successful initializations, {} errors, {} total operations", 
                   successfulInitializations.get(), errors.get(), expectedOperations);
        
        // Assertions
        assertEquals(0, errors.get(), "No errors should occur during search initialization");
        assertEquals(expectedOperations, successfulInitializations.get(), 
                    "All search initializations should succeed");
        
        // Verify blockchain integrity after stress test
        assertTrue(blockchain.validateChainDetailed().isValid(), "Blockchain should remain valid after stress test");
        assertTrue(blockchain.getBlockCount() >= 5, "Block count should be at least the initial 5 blocks");
        
        logger.info("✅ Concurrent search initialization stress test passed!");
    }
    
    @Test
    @DisplayName("🧪 Stress test: Search initialization concurrent with block operations")
    @Timeout(120)
    void testSearchInitializationWithBlockOperations() throws Exception {
        logger.info("🚀 Starting search initialization with block operations stress test...");
        
        final int NUM_INIT_THREADS = 4;
        final int NUM_BLOCK_THREADS = 3;
        final int OPERATIONS_PER_THREAD = 8;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch completionLatch = new CountDownLatch(NUM_INIT_THREADS + NUM_BLOCK_THREADS);
        final AtomicInteger successfulInitializations = new AtomicInteger(0);
        final AtomicInteger successfulBlockOperations = new AtomicInteger(0);
        final AtomicInteger errors = new AtomicInteger(0);
        final List<String> createdBlockHashes = new ArrayList<>();
        
        // Search initialization threads
        for (int i = 0; i < NUM_INIT_THREADS; i++) {
            final int threadId = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    
                    for (int op = 0; op < OPERATIONS_PER_THREAD; op++) {
                        try {
                            blockchain.initializeAdvancedSearch("testPassword" + (op % 3));
                            successfulInitializations.incrementAndGet();
                            Thread.sleep(2);
                            
                        } catch (Exception e) {
                            logger.error("❌ Error in init thread {}", threadId, e);
                            errors.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    errors.incrementAndGet();
                } finally {
                    completionLatch.countDown();
                }
            });
        }
        
        // Block operation threads
        for (int i = 0; i < NUM_BLOCK_THREADS; i++) {
            final int threadId = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    
                    for (int op = 0; op < OPERATIONS_PER_THREAD; op++) {
                        try {
                            String data = "Concurrent block data from thread " + threadId + " op " + op;
                            
                            Block block = blockchain.addEncryptedBlock(
                                data,
                                "blockPassword" + op,
                                testKeyPair.getPrivate(),
                                testKeyPair.getPublic()
                            );
                            
                            if (block != null) {
                                successfulBlockOperations.incrementAndGet();
                                synchronized (createdBlockHashes) {
                                    createdBlockHashes.add(block.getHash());
                                }
                            }
                            
                            Thread.sleep(3);
                            
                        } catch (Exception e) {
                            logger.error("❌ Error in block thread {}", threadId, e);
                            errors.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    errors.incrementAndGet();
                } finally {
                    completionLatch.countDown();
                }
            });
        }
        
        // Start all threads
        logger.info("🏁 Starting {} init threads and {} block threads...", NUM_INIT_THREADS, NUM_BLOCK_THREADS);
        startLatch.countDown();
        
        // Wait for completion
        assertTrue(completionLatch.await(90, TimeUnit.SECONDS), 
                  "Mixed operations stress test should complete within timeout");
        
        // Validate results
        int expectedInitializations = NUM_INIT_THREADS * OPERATIONS_PER_THREAD;
        int expectedBlockOperations = NUM_BLOCK_THREADS * OPERATIONS_PER_THREAD;
        
        logger.info("📊 Results: {} successful initializations, {} successful block ops, {} errors", 
                   successfulInitializations.get(), successfulBlockOperations.get(), errors.get());
        logger.info("📊 Created {} new block hashes", createdBlockHashes.size());
        
        // Assertions
        assertEquals(0, errors.get(), "No errors should occur during mixed operations");
        assertEquals(expectedInitializations, successfulInitializations.get(), 
                    "All search initializations should succeed");
        assertEquals(expectedBlockOperations, successfulBlockOperations.get(), 
                    "All block operations should succeed");
        assertEquals(createdBlockHashes.size(), successfulBlockOperations.get(), 
                    "All successful blocks should have unique hashes");
        
        // Verify blockchain integrity
        assertTrue(blockchain.validateChainDetailed().isValid(), "Blockchain should remain valid after stress test");
        int expectedMinBlockCount = 5 + expectedBlockOperations; // Initial 5 + new blocks
        assertTrue(blockchain.getBlockCount() >= expectedMinBlockCount, 
                    "Block count should include at least all expected blocks");
        
        logger.info("✅ Search initialization with block operations stress test passed!");
    }
    
    @Test
    @DisplayName("🧪 Stress test: High-contention read lock validation")
    @Timeout(60)
    void testHighContentionReadLockValidation() throws Exception {
        logger.info("🚀 Starting high-contention read lock validation stress test...");
        
        final int NUM_THREADS = 20;
        final int QUICK_OPERATIONS = 25;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch completionLatch = new CountDownLatch(NUM_THREADS);
        final AtomicInteger totalOperations = new AtomicInteger(0);
        final AtomicInteger errors = new AtomicInteger(0);
        
        // Submit all tasks for maximum contention
        for (int i = 0; i < NUM_THREADS; i++) {
            final int threadId = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    
                    for (int op = 0; op < QUICK_OPERATIONS; op++) {
                        try {
                            // Rapid-fire search initializations to test lock contention
                            blockchain.initializeAdvancedSearch();
                            totalOperations.incrementAndGet();
                            
                            // No sleep - maximum contention
                            
                        } catch (Exception e) {
                            logger.error("❌ Error in high-contention thread {}", threadId, e);
                            errors.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    errors.incrementAndGet();
                } finally {
                    completionLatch.countDown();
                }
            });
        }
        
        // Start all threads for maximum contention
        logger.info("🏁 Starting {} threads with {} rapid operations each for high contention...", 
                   NUM_THREADS, QUICK_OPERATIONS);
        startLatch.countDown();
        
        // Wait for completion
        assertTrue(completionLatch.await(45, TimeUnit.SECONDS), 
                  "High-contention test should complete within timeout");
        
        // Validate results
        int expectedOperations = NUM_THREADS * QUICK_OPERATIONS;
        logger.info("📊 Results: {} total operations completed, {} errors", 
                   totalOperations.get(), errors.get());
        
        // Assertions
        assertEquals(0, errors.get(), "No errors should occur under high contention");
        assertEquals(expectedOperations, totalOperations.get(), 
                    "All operations should complete successfully");
        
        // Verify blockchain state integrity
        assertTrue(blockchain.validateChainDetailed().isValid(), "Blockchain should remain valid under high contention");
        
        logger.info("✅ High-contention read lock validation stress test passed!");
    }
}