package com.rbatllet.blockchain.core;

import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.util.JPAUtil;
import org.junit.jupiter.api.*;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test thread-safety of block sequence generation
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BlockSequenceThreadSafetyTest {
    
    private static Blockchain blockchain;
    private static KeyPair authorizedKeyPair;
    private static final String ENCRYPTION_PASSWORD = "ThreadSafetyTest123!@#";
    
    @BeforeAll
    static void setUpClass() {
        blockchain = new Blockchain();
        authorizedKeyPair = CryptoUtil.generateKeyPair();
    }
    
    @AfterAll
    static void tearDownClass() {
        JPAUtil.closeEntityManager();
    }
    
    @BeforeEach
    void cleanDatabase() {
        // Clean database before each test to ensure isolation - using thread-safe DAO method
        blockchain.getBlockDAO().cleanupTestData();
        blockchain.getAuthorizedKeyDAO().cleanupTestData();
        
        // Re-add authorized key after cleanup for test to work
        blockchain.addAuthorizedKey(
            CryptoUtil.publicKeyToString(authorizedKeyPair.getPublic()),
            "Test User"
        );
    }
    
    @Test
    @Order(1)
    void testSequentialBlockCreation() {
        System.out.println("\n=== Testing Sequential Block Creation ===");
        
        // Create 5 blocks sequentially
        List<Block> blocks = new ArrayList<>();
        
        for (int i = 1; i <= 5; i++) {
            Block block = blockchain.addBlockAndReturn(
                "Test block #" + i,
                authorizedKeyPair.getPrivate(),
                authorizedKeyPair.getPublic()
            );
            
            assertNotNull(block, "Block #" + i + " should not be null");
            assertEquals(Long.valueOf(i), block.getBlockNumber(), "Block number should be " + i);
            blocks.add(block);
            
            System.out.println("✅ Block #" + i + " created successfully with ID: " + block.getId());
        }
        
        // Verify all blocks have sequential numbers
        for (int i = 0; i < blocks.size(); i++) {
            assertEquals(Long.valueOf(i + 1), blocks.get(i).getBlockNumber());
        }
        
        System.out.println("✅ Sequential block creation test passed");
    }
    
    @Test
    @Order(2)
    void testSequentialEncryptedBlockCreation() {
        System.out.println("\n=== Testing Sequential Encrypted Block Creation ===");
        
        // Create 5 encrypted blocks sequentially
        List<Block> blocks = new ArrayList<>();
        
        for (int i = 1; i <= 5; i++) {
            Block block = blockchain.addEncryptedBlock(
                "Encrypted test block #" + i + " with sensitive data",
                ENCRYPTION_PASSWORD,
                authorizedKeyPair.getPrivate(),
                authorizedKeyPair.getPublic()
            );
            
            assertNotNull(block, "Encrypted block #" + i + " should not be null");
            assertEquals(Long.valueOf(i), block.getBlockNumber(), "Block number should be " + i);
            assertTrue(block.isDataEncrypted(), "Block should be encrypted");
            blocks.add(block);
            
            System.out.println("✅ Encrypted Block #" + i + " created successfully with ID: " + block.getId());
        }
        
        System.out.println("✅ Sequential encrypted block creation test passed");
    }
    
    @Test
    @Order(3)
    void testMixedBlockCreation() {
        System.out.println("\n=== Testing Mixed Block Creation ===");
        
        // Create mixed public and encrypted blocks
        List<Block> blocks = new ArrayList<>();
        
        // Block 1: Public
        Block block1 = blockchain.addBlockAndReturn(
            "Public block #1",
            authorizedKeyPair.getPrivate(),
            authorizedKeyPair.getPublic()
        );
        assertNotNull(block1, "Public block #1 should not be null");
        assertEquals(Long.valueOf(1), block1.getBlockNumber());
        assertFalse(block1.isDataEncrypted());
        blocks.add(block1);
        System.out.println("✅ Public Block #1 created");
        
        // Block 2: Encrypted
        Block block2 = blockchain.addEncryptedBlock(
            "Encrypted block #2",
            ENCRYPTION_PASSWORD,
            authorizedKeyPair.getPrivate(),
            authorizedKeyPair.getPublic()
        );
        assertNotNull(block2, "Encrypted block #2 should not be null");
        assertEquals(Long.valueOf(2), block2.getBlockNumber());
        assertTrue(block2.isDataEncrypted());
        blocks.add(block2);
        System.out.println("✅ Encrypted Block #2 created");
        
        // Block 3: Public
        Block block3 = blockchain.addBlockAndReturn(
            "Public block #3",
            authorizedKeyPair.getPrivate(),
            authorizedKeyPair.getPublic()
        );
        assertNotNull(block3, "Public block #3 should not be null");
        assertEquals(Long.valueOf(3), block3.getBlockNumber());
        assertFalse(block3.isDataEncrypted());
        blocks.add(block3);
        System.out.println("✅ Public Block #3 created");
        
        // Block 4: Encrypted
        Block block4 = blockchain.addEncryptedBlock(
            "Encrypted block #4",
            ENCRYPTION_PASSWORD,
            authorizedKeyPair.getPrivate(),
            authorizedKeyPair.getPublic()
        );
        assertNotNull(block4, "Encrypted block #4 should not be null");
        assertEquals(Long.valueOf(4), block4.getBlockNumber());
        assertTrue(block4.isDataEncrypted());
        blocks.add(block4);
        System.out.println("✅ Encrypted Block #4 created");
        
        // Verify blockchain integrity
        boolean isValid = blockchain.isStructurallyIntact();
        assertTrue(isValid, "Blockchain should be valid after mixed block creation");
        
        System.out.println("✅ Mixed block creation test passed");
    }
    
    @Test
    @Order(4)
    void testConcurrentBlockCreation() {
        System.out.println("\n=== Testing Concurrent Block Creation ===");
        
        int numberOfThreads = 5;
        int blocksPerThread = 3;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        List<Future<List<Block>>> futures = new ArrayList<>();
        
        // Submit tasks to create blocks concurrently
        for (int threadId = 0; threadId < numberOfThreads; threadId++) {
            final int finalThreadId = threadId;
            Future<List<Block>> future = executor.submit(() -> {
                List<Block> threadBlocks = new ArrayList<>();
                String threadName = "Thread-" + finalThreadId;
                
                for (int i = 1; i <= blocksPerThread; i++) {
                    try {
                        Block block = blockchain.addBlockAndReturn(
                            threadName + " block #" + i,
                            authorizedKeyPair.getPrivate(),
                            authorizedKeyPair.getPublic()
                        );
                        
                        if (block != null) {
                            threadBlocks.add(block);
                            System.out.println("✅ " + threadName + " created block #" + block.getBlockNumber());
                        } else {
                            System.err.println("❌ " + threadName + " failed to create block #" + i);
                        }
                        
                        // Small delay to allow thread interleaving
                        Thread.sleep(10);
                        
                    } catch (Exception e) {
                        System.err.println("❌ " + threadName + " exception: " + e.getMessage());
                    }
                }
                
                return threadBlocks;
            });
            
            futures.add(future);
        }
        
        // Collect all results
        List<Block> allBlocks = new ArrayList<>();
        for (Future<List<Block>> future : futures) {
            try {
                List<Block> threadBlocks = future.get(30, TimeUnit.SECONDS);
                allBlocks.addAll(threadBlocks);
            } catch (Exception e) {
                System.err.println("❌ Error getting thread results: " + e.getMessage());
            }
        }
        
        executor.shutdown();
        
        // Verify results
        assertFalse(allBlocks.isEmpty(), "Should have created some blocks");
        System.out.println("📊 Total blocks created: " + allBlocks.size());
        
        // Check for duplicate block numbers
        List<Long> blockNumbers = new ArrayList<>();
        for (Block block : allBlocks) {
            blockNumbers.add(block.getBlockNumber());
        }
        
        List<Long> sortedNumbers = new ArrayList<>(blockNumbers);
        Collections.sort(sortedNumbers);
        
        System.out.println("📋 Block numbers created: " + sortedNumbers);
        
        // Verify no duplicates
        for (int i = 1; i < sortedNumbers.size(); i++) {
            assertNotEquals(sortedNumbers.get(i-1), sortedNumbers.get(i), 
                "No duplicate block numbers should exist");
        }
        
        // Verify blockchain integrity
        boolean isValid = blockchain.isStructurallyIntact();
        assertTrue(isValid, "Blockchain should be valid after concurrent block creation");
        
        System.out.println("✅ Concurrent block creation test passed");
    }
}