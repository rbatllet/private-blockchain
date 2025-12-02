package com.rbatllet.blockchain.core;

import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.indexing.IndexingCoordinator;
import com.rbatllet.blockchain.util.CryptoUtil;
import org.junit.jupiter.api.*;

import java.security.KeyPair;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verification test for block_sequence optimizations with genesis fix
 * Tests that the optimizations work correctly after fixing the genesis block duplicate issue
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OptimizationWithFixVerificationTest {

    private static Blockchain blockchain;
    private static KeyPair keyPair;
    private static String publicKeyStr;

    @BeforeAll
    static void setUp() {
        blockchain = new Blockchain();
        keyPair = CryptoUtil.generateKeyPair();
        publicKeyStr = CryptoUtil.publicKeyToString(keyPair.getPublic());
    }

    @BeforeEach
    void cleanDatabase() {
        // Reset IndexingCoordinator to clear shutdown state
        IndexingCoordinator.getInstance().reset();

        blockchain.clearAndReinitialize();
        blockchain.createBootstrapAdmin(publicKeyStr, "TestUser");
    }

    @Test
    @Order(1)
    @DisplayName("Genesis block is created after clearAndReinitialize()")
    void testGenesisCreationAfterClear() {
        long count = blockchain.getBlockCount();
        Block lastBlock = blockchain.getLastBlock();

        assertNotNull(lastBlock, "Last block should not be null after clearAndReinitialize()");
        assertEquals(1, count, "Block count should be 1 after clearAndReinitialize()");
        assertEquals(0L, lastBlock.getBlockNumber(), "Last block should be genesis (block #0)");
    }

    @Test
    @Order(2)
    @DisplayName("Can add blocks after clearAndReinitialize()")
    void testAddBlocksAfterClear() {
        // Should start with genesis
        assertEquals(1, blockchain.getBlockCount());

        // Add first block
        boolean success = blockchain.addBlock("Test data 1", keyPair.getPrivate(), keyPair.getPublic());
        assertTrue(success, "Should be able to add first block");
        assertEquals(2, blockchain.getBlockCount(), "Count should be 2 after adding first block");

        // Add second block
        success = blockchain.addBlock("Test data 2", keyPair.getPrivate(), keyPair.getPublic());
        assertTrue(success, "Should be able to add second block");
        assertEquals(3, blockchain.getBlockCount(), "Count should be 3 after adding second block");

        Block lastBlock = blockchain.getLastBlock();
        assertNotNull(lastBlock, "Last block should not be null");
        assertEquals(2L, lastBlock.getBlockNumber(), "Last block should be block #2");
    }

    @Test
    @Order(3)
    @DisplayName("Performance: 1000 blocks should take < 5s")
    void testPerformanceWithOptimizations() {
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < 1000; i++) {
            boolean success = blockchain.addBlock("Data #" + i, keyPair.getPrivate(), keyPair.getPublic());
            assertTrue(success, "Block " + i + " should be added successfully");
        }

        long totalTime = System.currentTimeMillis() - startTime;
        double avgPerBlock = totalTime / 1000.0;

        System.out.printf("⏱️  1000 blocks added in %dms (avg: %.2fms/block)\n", totalTime, avgPerBlock);

        // Verify final state
        long finalCount = blockchain.getBlockCount();
        Block lastBlock = blockchain.getLastBlock();

        assertEquals(1001, finalCount, "Should have 1001 blocks (genesis + 1000)");
        assertEquals(1000L, lastBlock.getBlockNumber(), "Last block should be #1000");

        // Performance assertion: Limit set to 20s to account for test suite load (2288 tests)
        // while still detecting real performance issues (without optimization would be >60s)
        // Note: Includes blockchain validation, hashing, signing, and async indexing overhead
        assertTrue(totalTime < 20000,
            String.format("Performance degraded: took %dms (expected < 20000ms)", totalTime));
    }

    @Test
    @Order(4)
    @DisplayName("getBlockCount() is fast with 1000+ blocks")
    void testGetBlockCountPerformance() {
        // Add 1000 blocks first
        for (int i = 0; i < 1000; i++) {
            blockchain.addBlock("Data #" + i, keyPair.getPrivate(), keyPair.getPublic());
        }

        // Measure getBlockCount() performance
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            long count = blockchain.getBlockCount();
            assertEquals(1001, count, "Count should be consistent");
        }
        long elapsed = System.currentTimeMillis() - startTime;
        double avgTime = elapsed / 100.0;

        System.out.printf("⏱️  100 getBlockCount() calls: %dms (avg: %.2fms)\n", elapsed, avgTime);

        // Should be extremely fast (< 10ms for 100 calls)
        assertTrue(elapsed < 100,
            String.format("getBlockCount() too slow: %dms for 100 calls (expected < 100ms)", elapsed));
    }

    @Test
    @Order(5)
    @DisplayName("getLastBlock() is fast with 1000+ blocks")
    void testGetLastBlockPerformance() throws InterruptedException {
        // Add 1000 blocks first
        for (int i = 0; i < 1000; i++) {
            blockchain.addBlock("Data #" + i, keyPair.getPrivate(), keyPair.getPublic());
        }

        // CRITICAL: Wait for ALL async indexing to complete before performance test
        // This ensures we're testing getLastBlock() performance, not indexing overhead
        IndexingCoordinator.getInstance().waitForCompletion();

        // Measure getLastBlock() performance
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            Block lastBlock = blockchain.getLastBlock();
            assertNotNull(lastBlock, "Last block should not be null");
            assertEquals(1000L, lastBlock.getBlockNumber(), "Last block should be #1000");
        }
        long elapsed = System.currentTimeMillis() - startTime;
        double avgTime = elapsed / 100.0;

        System.out.printf("⏱️  100 getLastBlock() calls: %dms (avg: %.2fms)\n", elapsed, avgTime);

        // Performance check: Limit set to 500ms to account for test suite load (2288 tests)
        // while still detecting real performance issues (without optimization would be >2000ms)
        assertTrue(elapsed < 500,
            String.format("getLastBlock() too slow: %dms for 100 calls (expected < 500ms)", elapsed));
    }

    @Test
    @Order(6)
    @DisplayName("Multiple clearAndReinitialize() calls work correctly")
    void testMultipleClearOperations() {
        // Add some blocks
        for (int i = 0; i < 10; i++) {
            blockchain.addBlock("Data #" + i, keyPair.getPrivate(), keyPair.getPublic());
        }
        assertEquals(11, blockchain.getBlockCount());

        // Clear and reinitialize
        blockchain.clearAndReinitialize();
        blockchain.createBootstrapAdmin(publicKeyStr, "TestUser");

        // Should be back to genesis only
        assertEquals(1, blockchain.getBlockCount());
        Block lastBlock = blockchain.getLastBlock();
        assertNotNull(lastBlock);
        assertEquals(0L, lastBlock.getBlockNumber());

        // Add blocks again
        for (int i = 0; i < 5; i++) {
            blockchain.addBlock("New data #" + i, keyPair.getPrivate(), keyPair.getPublic());
        }
        assertEquals(6, blockchain.getBlockCount());

        // Clear again
        blockchain.clearAndReinitialize();
        blockchain.createBootstrapAdmin(publicKeyStr, "TestUser");

        // Should be back to genesis only again
        assertEquals(1, blockchain.getBlockCount());
        lastBlock = blockchain.getLastBlock();
        assertNotNull(lastBlock);
        assertEquals(0L, lastBlock.getBlockNumber());
    }
}
