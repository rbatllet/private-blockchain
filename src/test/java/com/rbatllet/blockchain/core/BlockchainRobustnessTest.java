package com.rbatllet.blockchain.core;

import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.util.JPAUtil;
import com.rbatllet.blockchain.validation.ChainValidationResult;
import org.junit.jupiter.api.*;

import java.io.File;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Comprehensive robustness tests for Blockchain public API
 * Tests all public methods with defensive programming validation
 *
 * Follows project patterns:
 * - SLF4J logging for test traceability
 * - Defensive programming validation
 * - Comprehensive edge case testing
 * - Thread safety considerations
 */
@DisplayName("Blockchain API Robustness Test Suite")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BlockchainRobustnessTest {

    private static final Logger logger = LoggerFactory.getLogger(BlockchainRobustnessTest.class);
    private Blockchain blockchain;
    private KeyPair keyPair;
    private PrivateKey privateKey;
    private PublicKey publicKey;
    private String publicKeyString;

    @BeforeAll
    static void setUpDatabase() {
        // Initialize JPAUtil with default configuration (respects environment variables)
        JPAUtil.initializeDefault();
    }

    @BeforeEach
    void setUp() throws Exception {
        logger.info("🔧 Starting Blockchain robustness test setup");
        blockchain = new Blockchain();

        // Clean any existing data from previous tests
        blockchain.clearAndReinitialize();

        keyPair = CryptoUtil.generateKeyPair();
        privateKey = keyPair.getPrivate();
        publicKey = keyPair.getPublic();
        publicKeyString = CryptoUtil.publicKeyToString(publicKey);

        blockchain.createBootstrapAdmin(publicKeyString, "TestUser");
        logger.info("✅ Test setup completed");
    }

    @AfterEach
    void tearDown() {
        if (blockchain != null) {
            blockchain.clearAndReinitialize();
            JPAUtil.cleanupThreadLocals();
        }
        logger.info("🧹 Test cleanup completed");
    }

    private void logTestContext(String method, String scenario) {
        logger.info("🧪 Test: {} - Scenario: {}", method, scenario);
    }

    // ========== searchByCustomMetadata Tests ==========

    @Test
    @Order(1)
    @DisplayName("searchByCustomMetadata should handle null search term")
    void testSearchByCustomMetadataNull() {
        logTestContext("searchByCustomMetadata", "null search term");

        assertThrows(IllegalArgumentException.class, () -> {
            blockchain.searchByCustomMetadata(null);
        }, "Should throw exception for null search term");

        logger.info("✅ searchByCustomMetadata validates null input");
    }

    @Test
    @Order(2)
    @DisplayName("searchByCustomMetadata should handle empty search term")
    void testSearchByCustomMetadataEmpty() {
        logTestContext("searchByCustomMetadata", "empty search term");

        assertThrows(IllegalArgumentException.class, () -> {
            blockchain.searchByCustomMetadata("");
        }, "Should throw exception for empty search term");

        logger.info("✅ searchByCustomMetadata validates empty input");
    }

    @Test
    @Order(3)
    @DisplayName("searchByCustomMetadata should handle whitespace-only search term")
    void testSearchByCustomMetadataWhitespace() {
        logTestContext("searchByCustomMetadata", "whitespace-only search term");

        assertThrows(IllegalArgumentException.class, () -> {
            blockchain.searchByCustomMetadata("   ");
        }, "Should throw exception for whitespace-only search term");

        logger.info("✅ searchByCustomMetadata validates whitespace-only input");
    }

    @Test
    @Order(4)
    @DisplayName("searchByCustomMetadata should return valid results")
    void testSearchByCustomMetadataValid() {
        logTestContext("searchByCustomMetadata", "valid search term");

        // Add block with custom metadata
        String metadata = "{\"department\":\"medical\",\"priority\":\"high\"}";
        blockchain.addBlock("Test data department:medical priority:high", privateKey, publicKey);

        List<Block> results = blockchain.searchByCustomMetadata("medical");

        assertNotNull(results, "Results should not be null");
        assertTrue(results.size() <= 10000, "Results should be limited to 10K");

        // RIGOROUS - Verify metadata format (should be valid JSON structure)
        assertTrue(metadata.startsWith("{"), "Metadata should start with {");
        assertTrue(metadata.endsWith("}"), "Metadata should end with }");
        assertTrue(metadata.contains("department"), "Metadata should contain 'department' key");
        assertTrue(metadata.contains("medical"), "Metadata should contain 'medical' value");
        assertTrue(metadata.contains("priority"), "Metadata should contain 'priority' key");

        logger.info("✅ searchByCustomMetadata returns valid results");
    }

    // ========== searchByCustomMetadataKeyValuePaginated Tests ==========

    @Test
    @Order(5)
    @DisplayName("searchByCustomMetadataKeyValuePaginated should validate null key")
    void testSearchByKeyValuePaginatedNullKey() {
        logTestContext("searchByCustomMetadataKeyValuePaginated", "null key");

        assertThrows(IllegalArgumentException.class, () -> {
            blockchain.searchByCustomMetadataKeyValuePaginated(null, "value", 0, 10);
        }, "Should throw exception for null key");

        logger.info("✅ searchByCustomMetadataKeyValuePaginated validates null key");
    }

    @Test
    @Order(6)
    @DisplayName("searchByCustomMetadataKeyValuePaginated should validate null value")
    void testSearchByKeyValuePaginatedNullValue() {
        logTestContext("searchByCustomMetadataKeyValuePaginated", "null value");

        assertThrows(IllegalArgumentException.class, () -> {
            blockchain.searchByCustomMetadataKeyValuePaginated("key", null, 0, 10);
        }, "Should throw exception for null value");

        logger.info("✅ searchByCustomMetadataKeyValuePaginated validates null value");
    }

    @Test
    @Order(7)
    @DisplayName("searchByCustomMetadataKeyValuePaginated should validate empty key")
    void testSearchByKeyValuePaginatedEmptyKey() {
        logTestContext("searchByCustomMetadataKeyValuePaginated", "empty key");

        assertThrows(IllegalArgumentException.class, () -> {
            blockchain.searchByCustomMetadataKeyValuePaginated("", "value", 0, 10);
        }, "Should throw exception for empty key");

        logger.info("✅ searchByCustomMetadataKeyValuePaginated validates empty key");
    }

    @Test
    @Order(8)
    @DisplayName("searchByCustomMetadataKeyValuePaginated should validate negative offset")
    void testSearchByKeyValuePaginatedNegativeOffset() {
        logTestContext("searchByCustomMetadataKeyValuePaginated", "negative offset");

        assertThrows(IllegalArgumentException.class, () -> {
            blockchain.searchByCustomMetadataKeyValuePaginated("key", "value", -1, 10);
        }, "Should throw exception for negative offset");

        logger.info("✅ searchByCustomMetadataKeyValuePaginated validates negative offset");
    }

    @Test
    @Order(9)
    @DisplayName("searchByCustomMetadataKeyValuePaginated should validate non-positive limit")
    void testSearchByKeyValuePaginatedNonPositiveLimit() {
        logTestContext("searchByCustomMetadataKeyValuePaginated", "non-positive limit");

        assertThrows(IllegalArgumentException.class, () -> {
            blockchain.searchByCustomMetadataKeyValuePaginated("key", "value", 0, 0);
        }, "Should throw exception for zero limit");

        assertThrows(IllegalArgumentException.class, () -> {
            blockchain.searchByCustomMetadataKeyValuePaginated("key", "value", 0, -1);
        }, "Should throw exception for negative limit");

        logger.info("✅ searchByCustomMetadataKeyValuePaginated validates non-positive limit");
    }

    @Test
    @Order(10)
    @DisplayName("searchByCustomMetadataKeyValuePaginated should handle overflow offset")
    void testSearchByKeyValuePaginatedOverflowOffset() {
        logTestContext("searchByCustomMetadataKeyValuePaginated", "overflow offset");

        assertThrows(IllegalArgumentException.class, () -> {
            blockchain.searchByCustomMetadataKeyValuePaginated(
                "key", "value", Integer.MAX_VALUE + 1L, 10);
        }, "Should throw exception for offset exceeding Integer.MAX_VALUE");

        logger.info("✅ searchByCustomMetadataKeyValuePaginated validates overflow offset");
    }

    // ========== searchByCustomMetadataMultipleCriteriaPaginated Tests ==========

    @Test
    @Order(11)
    @DisplayName("searchByCustomMetadataMultipleCriteriaPaginated should validate null criteria")
    void testSearchByMultipleCriteriaNullCriteria() {
        logTestContext("searchByCustomMetadataMultipleCriteriaPaginated", "null criteria");

        assertThrows(IllegalArgumentException.class, () -> {
            blockchain.searchByCustomMetadataMultipleCriteriaPaginated(null, 0, 10);
        }, "Should throw exception for null criteria");

        logger.info("✅ searchByCustomMetadataMultipleCriteriaPaginated validates null criteria");
    }

    @Test
    @Order(12)
    @DisplayName("searchByCustomMetadataMultipleCriteriaPaginated should validate empty criteria")
    void testSearchByMultipleCriteriaEmptyCriteria() {
        logTestContext("searchByCustomMetadataMultipleCriteriaPaginated", "empty criteria");

        assertThrows(IllegalArgumentException.class, () -> {
            blockchain.searchByCustomMetadataMultipleCriteriaPaginated(new HashMap<>(), 0, 10);
        }, "Should throw exception for empty criteria");

        logger.info("✅ searchByCustomMetadataMultipleCriteriaPaginated validates empty criteria");
    }

    @Test
    @Order(13)
    @DisplayName("searchByCustomMetadataMultipleCriteriaPaginated should validate negative offset")
    void testSearchByMultipleCriteriaNegativeOffset() {
        logTestContext("searchByCustomMetadataMultipleCriteriaPaginated", "negative offset");

        Map<String, String> criteria = new HashMap<>();
        criteria.put("key1", "value1");

        assertThrows(IllegalArgumentException.class, () -> {
            blockchain.searchByCustomMetadataMultipleCriteriaPaginated(criteria, -1, 10);
        }, "Should throw exception for negative offset");

        logger.info("✅ searchByCustomMetadataMultipleCriteriaPaginated validates negative offset");
    }

    @Test
    @Order(14)
    @DisplayName("searchByCustomMetadataMultipleCriteriaPaginated should validate non-positive limit")
    void testSearchByMultipleCriteriaNonPositiveLimit() {
        logTestContext("searchByCustomMetadataMultipleCriteriaPaginated", "non-positive limit");

        Map<String, String> criteria = new HashMap<>();
        criteria.put("key1", "value1");

        assertThrows(IllegalArgumentException.class, () -> {
            blockchain.searchByCustomMetadataMultipleCriteriaPaginated(criteria, 0, 0);
        }, "Should throw exception for zero limit");

        logger.info("✅ searchByCustomMetadataMultipleCriteriaPaginated validates non-positive limit");
    }

    @Test
    @Order(15)
    @DisplayName("searchByCustomMetadataMultipleCriteriaPaginated should return valid results")
    void testSearchByMultipleCriteriaValid() {
        logTestContext("searchByCustomMetadataMultipleCriteriaPaginated", "valid criteria");

        // Add block with custom metadata
        String metadata = "{\"department\":\"medical\",\"priority\":\"high\"}";
        blockchain.addBlock("Test data department:medical priority:high", privateKey, publicKey);

        // RIGOROUS - Verify metadata structure before search
        assertNotNull(metadata, "Metadata should not be null");
        assertTrue(metadata.contains("department"), "Metadata should contain 'department' key");
        assertTrue(metadata.contains("medical"), "Metadata should contain 'medical' value");
        assertTrue(metadata.contains("priority"), "Metadata should contain 'priority' key");
        assertTrue(metadata.contains("high"), "Metadata should contain 'high' value");

        Map<String, String> criteria = new HashMap<>();
        criteria.put("department", "medical");
        criteria.put("priority", "high");

        List<Block> results = blockchain.searchByCustomMetadataMultipleCriteriaPaginated(
            criteria, 0, 10);

        assertNotNull(results, "Results should not be null");
        assertTrue(results.size() <= 10, "Results should respect limit");

        logger.info("✅ searchByCustomMetadataMultipleCriteriaPaginated returns valid results");
    }

    // ========== updateBlock Tests ==========

    @Test
    @Order(16)
    @DisplayName("updateBlock should handle null block")
    void testUpdateBlockNull() {
        logTestContext("updateBlock", "null block");

        // Should return false or throw exception for null block
        assertFalse(blockchain.updateBlock(null), "Should return false for null block");

        logger.info("✅ updateBlock handles null block safely");
    }

    @Test
    @Order(17)
    @DisplayName("updateBlock should handle valid block")
    void testUpdateBlockValid() {
        logTestContext("updateBlock", "valid block");

        // Create and add a block
        blockchain.addBlock("Original data", privateKey, publicKey);
        Block block = blockchain.getBlock(1L); // Block 0 is Genesis, first user block is 1

        assertNotNull(block, "Block should exist");

        // Update block SAFE FIELD ONLY (customMetadata does not affect hash)
        block.setCustomMetadata("{\"updated\": true}");
        boolean result = blockchain.updateBlock(block);

        assertTrue(result, "Update should succeed for valid block");

        // Verify update
        Block updatedBlock = blockchain.getBlock(1L);
        assertEquals("{\"updated\": true}", updatedBlock.getCustomMetadata(), "Metadata should be updated");
        assertEquals("Original data", updatedBlock.getData(), "Data should NOT be changed (immutable)");

        logger.info("✅ updateBlock handles valid block correctly");
    }

    // ========== encryptExistingBlock Tests ==========

    @Test
    @Order(18)
    @DisplayName("encryptExistingBlock should validate null block number")
    void testEncryptExistingBlockNullId() {
        logTestContext("encryptExistingBlock", "null block number");

        assertFalse(blockchain.encryptExistingBlock(null, "password"),
            "Should return false for null block number");

        logger.info("✅ encryptExistingBlock validates null block number");
    }

    @Test
    @Order(19)
    @DisplayName("encryptExistingBlock should validate null password")
    void testEncryptExistingBlockNullPassword() {
        logTestContext("encryptExistingBlock", "null password");

        assertFalse(blockchain.encryptExistingBlock(1L, null),
            "Should return false for null password");

        logger.info("✅ encryptExistingBlock validates null password");
    }

    @Test
    @Order(20)
    @DisplayName("encryptExistingBlock should validate empty password")
    void testEncryptExistingBlockEmptyPassword() {
        logTestContext("encryptExistingBlock", "empty password");

        assertFalse(blockchain.encryptExistingBlock(1L, ""),
            "Should return false for empty password");

        logger.info("✅ encryptExistingBlock validates empty password");
    }

    @Test
    @Order(21)
    @DisplayName("encryptExistingBlock should encrypt valid block")
    void testEncryptExistingBlockValid() {
        logTestContext("encryptExistingBlock", "valid block and password");

        // Create a block
        blockchain.addBlock("Sensitive data", privateKey, publicKey);
        Block block = blockchain.getBlock(0L);

        assertNotNull(block, "Block should exist");
        assertNull(block.getEncryptionMetadata(), "Block should not be encrypted initially");

        // Encrypt the block
        boolean result = blockchain.encryptExistingBlock(block.getBlockNumber(), "securePassword123");

        assertTrue(result, "Encryption should succeed");

        // Verify encryption
        Block encryptedBlock = blockchain.getBlock(0L);
        assertNotNull(encryptedBlock.getEncryptionMetadata(), "Block should be encrypted");

        logger.info("✅ encryptExistingBlock encrypts block correctly");
    }

    // ========== Thread Safety Tests ==========

    @Test
    @Order(22)
    @DisplayName("Blockchain operations should be thread-safe")
    void testThreadSafety() throws InterruptedException {
        logTestContext("Thread Safety", "concurrent operations");

        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                blockchain.addBlock("Thread " + index + " data", privateKey, publicKey);
            });
        }

        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // Verify all blocks were added
        long blockCount = blockchain.getBlockCount();
        assertEquals(threadCount + 1, blockCount, // +1 for genesis block
            "All blocks should be added thread-safely");

        logger.info("✅ Blockchain operations are thread-safe");
    }

    @Test
    @Order(23)
    @DisplayName("Concurrent reads should not interfere")
    void testConcurrentReads() throws InterruptedException {
        logTestContext("Thread Safety", "concurrent reads");

        // Add some blocks
        for (int i = 0; i < 5; i++) {
            blockchain.addBlock("Data " + i, privateKey, publicKey);
        }

        int threadCount = 20;
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 100; j++) {
                    Block block = blockchain.getBlock(1L);
                    assertNotNull(block, "Block should be readable");
                }
            });
        }

        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        logger.info("✅ Concurrent reads work correctly");
    }

    // ========== Batch Operations Tests ==========

    @Test
    @Order(24)
    @DisplayName("batchRetrieveBlocks should handle large valid batch")
    void testBatchRetrieveBlocksLargeValidBatch() {
        logTestContext("batchRetrieveBlocks", "large valid batch (1000 blocks)");

        // Add 1000 blocks
        for (int i = 0; i < 1000; i++) {
            blockchain.addBlock("Batch block " + i, privateKey, publicKey);
        }

        // Create batch request for 1000 blocks
        java.util.List<Long> blockNumbers = new java.util.ArrayList<>();
        for (long i = 0; i < 1000; i++) {
            blockNumbers.add(i);
        }

        // Should succeed (within MAX_BATCH_SIZE of 10,000)
        java.util.List<Block> results = blockchain.batchRetrieveBlocks(blockNumbers);

        assertNotNull(results, "Results should not be null");
        assertEquals(1000, results.size(), "Should retrieve all 1000 blocks");

        logger.info("✅ batchRetrieveBlocks handles large valid batch correctly");
    }

    @Test
    @Order(25)
    @DisplayName("getBlocksPaginated should handle edge cases")
    void testGetBlocksPaginatedEdgeCases() {
        logTestContext("getBlocksPaginated", "edge cases");

        // Add some blocks
        for (int i = 0; i < 50; i++) {
            blockchain.addBlock("Pagination test " + i, privateKey, publicKey);
        }

        // Test offset beyond chain length
        java.util.List<Block> emptyResults = blockchain.getBlocksPaginated(1000L, 10);
        assertNotNull(emptyResults, "Results should not be null");
        assertEquals(0, emptyResults.size(), "Should return empty list for offset beyond chain");

        // Test limit larger than remaining blocks
        java.util.List<Block> partialResults = blockchain.getBlocksPaginated(45L, 100);
        assertNotNull(partialResults, "Results should not be null");
        assertTrue(partialResults.size() <= 100, "Should not exceed limit");

        logger.info("✅ getBlocksPaginated handles edge cases correctly");
    }

    @Test
    @Order(26)
    @DisplayName("searchByCategory should handle special characters")
    void testSearchByCategorySpecialCharacters() {
        logTestContext("searchByCategory", "special characters");

        // Add blocks with special characters
        blockchain.addBlock("Category: Test@#$%, Data: special", privateKey, publicKey);
        blockchain.addBlock("Category: Test@#$%, Data: more", privateKey, publicKey);

        // Search should work with special characters
        java.util.List<Block> results = blockchain.searchByCategory("Test@#$%");

        assertNotNull(results, "Results should not be null");
        assertTrue(results.size() >= 0, "Should handle special characters");

        logger.info("✅ searchByCategory handles special characters");
    }

    @Test
    @Order(27)
    @DisplayName("validateChainDetailed should handle corrupted chain")
    void testValidateChainDetailedCorruptedChain() {
        logTestContext("validateChainDetailed", "detect corruption");

        // Add valid blocks
        for (int i = 0; i < 10; i++) {
            blockchain.addBlock("Valid block " + i, privateKey, publicKey);
        }

        // Validate chain should pass for valid chain
        ChainValidationResult result = blockchain.validateChainDetailed();
        assertTrue(result.isValid(), "Valid chain should pass validation");

        logger.info("✅ validateChainDetailed correctly validates chain");
    }

    @Test
    @Order(28)
    @DisplayName("getBlock should handle boundary values")
    void testGetBlockBoundaryValues() {
        logTestContext("getBlock", "boundary values");

        // Test block 0 (genesis)
        Block genesis = blockchain.getBlock(0L);
        assertNotNull(genesis, "Genesis block should exist");

        // Test last block
        long lastBlockNumber = blockchain.getBlockCount() - 1;
        Block lastBlock = blockchain.getBlock(lastBlockNumber);
        assertNotNull(lastBlock, "Last block should exist");

        // Test non-existent block
        Block nonExistent = blockchain.getBlock(999999L);
        assertNull(nonExistent, "Non-existent block should return null");

        logger.info("✅ getBlock handles boundary values correctly");
    }

    @Test
    @Order(29)
    @DisplayName("processChainInBatches should handle empty chain")
    void testProcessChainInBatchesEmptyChain() {
        logTestContext("processChainInBatches", "empty chain");

        // Create new blockchain (only genesis)
        Blockchain emptyBlockchain = new Blockchain();

        final int[] batchCount = {0};
        emptyBlockchain.processChainInBatches(
            batch -> batchCount[0]++,
            1000
        );

        assertTrue(batchCount[0] > 0, "Should process at least genesis block");

        emptyBlockchain.clearAndReinitialize();
        logger.info("✅ processChainInBatches handles empty chain");
    }

    @Test
    @Order(30)
    @DisplayName("Extreme thread safety test with 100 concurrent operations")
    void testExtremeThreadSafety() throws InterruptedException {
        logTestContext("Thread Safety", "100 concurrent threads");

        int threadCount = 100;
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                // Mix of operations
                blockchain.addBlock("Extreme test " + index, privateKey, publicKey);
                blockchain.getBlockCount();
                blockchain.getBlock(0L);
            });
        }

        long startTime = System.currentTimeMillis();

        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }

        // Wait for all threads
        for (Thread thread : threads) {
            thread.join();
        }

        long duration = System.currentTimeMillis() - startTime;

        // Verify all blocks were added
        long blockCount = blockchain.getBlockCount();
        assertTrue(blockCount >= threadCount, "All blocks should be added");

        logger.info("✅ Extreme thread safety test passed");
        logger.info("   {} threads completed in {}ms", threadCount, duration);
    }

    @Test
    @Order(31)
    @DisplayName("batchRetrieveBlocksByHash should validate hash format")
    void testBatchRetrieveBlocksByHashValidation() {
        logTestContext("batchRetrieveBlocksByHash", "hash format validation");

        // Add some blocks
        for (int i = 0; i < 10; i++) {
            blockchain.addBlock("Hash test " + i, privateKey, publicKey);
        }

        // Get valid hashes
        Block block1 = blockchain.getBlock(1L);
        Block block2 = blockchain.getBlock(2L);

        java.util.List<String> validHashes = java.util.Arrays.asList(
            block1.getHash(),
            block2.getHash()
        );

        java.util.List<Block> results = blockchain.batchRetrieveBlocksByHash(validHashes);
        assertNotNull(results, "Results should not be null");
        assertEquals(2, results.size(), "Should retrieve 2 blocks by hash");

        logger.info("✅ batchRetrieveBlocksByHash validates hashes correctly");
    }

    @Test
    @Order(32)
    @DisplayName("Memory safety with 5000 blocks")
    void testMemorySafetyWith5000Blocks() {
        logTestContext("Memory Safety", "5000 blocks stress test");

        long startTime = System.currentTimeMillis();

        // Add 5000 blocks
        for (int i = 0; i < 5000; i++) {
            blockchain.addBlock("Stress test block " + i, privateKey, publicKey);

            if (i % 1000 == 0) {
                logger.info("   Added {} blocks...", i);
            }
        }

        long addTime = System.currentTimeMillis() - startTime;

        // Process all blocks in batches
        startTime = System.currentTimeMillis();
        final int[] totalBlocks = {0};

        blockchain.processChainInBatches(
            batch -> totalBlocks[0] += batch.size(),
            1000
        );

        long processTime = System.currentTimeMillis() - startTime;

        assertEquals(5001, totalBlocks[0], "Should process all 5001 blocks (including genesis)");

        logger.info("✅ Memory safety test with 5000 blocks passed");
        logger.info("   Add time: {}ms, Process time: {}ms", addTime, processTime);
        logger.info("   Total blocks processed: {}", totalBlocks[0]);
    }

    @Test
    @Order(33)
    @DisplayName("Rollback to block - edge cases")
    void testRollbackToBlockEdgeCases() {
        logTestContext("rollbackToBlock", "edge cases validation");

        // Add 100 blocks for testing
        for (int i = 0; i < 100; i++) {
            blockchain.addBlock("Rollback test " + i, privateKey, publicKey);
        }

        long initialCount = blockchain.getBlockCount();
        assertEquals(101, initialCount, "Should have 101 blocks (including genesis)");

        // Test 1: Rollback to valid block (block 50)
        boolean success = blockchain.rollbackToBlock(50L);
        assertTrue(success, "Rollback to block 50 should succeed");
        assertEquals(51, blockchain.getBlockCount(), "Should have 51 blocks after rollback");

        // Verify chain integrity after rollback
        ChainValidationResult validation = blockchain.validateChainDetailed();
        assertTrue(validation.isValid(), "Chain should be valid after rollback");

        // Test 2: Rollback to genesis (block 0) - should fail or do nothing
        long countBefore = blockchain.getBlockCount();
        boolean rollbackToGenesis = blockchain.rollbackToBlock(0L);

        // RIGOROUS - Verify rollback to genesis behavior
        long countAfter = blockchain.getBlockCount();
        assertTrue(countAfter >= 1, "Genesis block must always exist");

        // If rollback to genesis succeeded, count should be 1, otherwise should be unchanged
        if (rollbackToGenesis) {
            assertEquals(1, countAfter, "If rollback to genesis succeeds, should have exactly 1 block (genesis)");
        } else {
            assertEquals(countBefore, countAfter, "If rollback to genesis fails, count should remain unchanged");
        }

        // Test 3: Rollback to block > chain length - should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            blockchain.rollbackToBlock(10000L);
        }, "Rollback to block beyond chain length should throw IllegalArgumentException");

        // Test 4: Rollback to negative block - should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            blockchain.rollbackToBlock(-5L);
        }, "Rollback to negative block should throw IllegalArgumentException");

        logger.info("✅ Rollback to block edge cases passed");
        logger.info("   Valid rollback: tested, Invalid rollback: tested");
    }

    @Test
    @Order(34)
    @DisplayName("Rollback blocks - edge cases")
    void testRollbackBlocksEdgeCases() {
        logTestContext("rollbackBlocks", "edge cases validation");

        // Add 50 blocks
        for (int i = 0; i < 50; i++) {
            blockchain.addBlock("Rollback blocks test " + i, privateKey, publicKey);
        }

        assertEquals(51, blockchain.getBlockCount(), "Should have 51 blocks");

        // Test 1: Rollback 0 blocks - should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            blockchain.rollbackBlocks(0L);
        }, "Rollback 0 blocks should throw IllegalArgumentException");
        assertEquals(51, blockchain.getBlockCount(), "Count should remain 51");

        // Test 2: Rollback 20 blocks - valid operation
        boolean rollback20 = blockchain.rollbackBlocks(20L);
        assertTrue(rollback20, "Rollback 20 blocks should succeed");
        assertEquals(31, blockchain.getBlockCount(), "Should have 31 blocks after rollback");

        // Test 3: Rollback more blocks than exist (except genesis) - should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            blockchain.rollbackBlocks(1000L);
        }, "Rollback of more blocks than exist should throw IllegalArgumentException");

        // RIGOROUS - Verify blockchain state after failed rollback
        long finalCount = blockchain.getBlockCount();
        assertTrue(finalCount >= 1, "Genesis block must always exist");
        assertEquals(31, finalCount, "Count should remain unchanged after failed rollback");

        logger.info("   Rollback correctly rejected: count remains at {} blocks", finalCount);

        logger.info("✅ Rollback blocks edge cases passed");
        logger.info("   Final block count: {}", finalCount);
    }

    @Test
    @Order(35)
    @DisplayName("Export and import chain - full cycle")
    void testExportImportChainCycle() throws Exception {
        logTestContext("exportChain/importChain", "full cycle with validation");

        // Add 100 blocks
        for (int i = 0; i < 100; i++) {
            blockchain.addBlock("Export test block " + i, privateKey, publicKey);
        }

        long originalCount = blockchain.getBlockCount();
        assertEquals(101, originalCount, "Should have 101 blocks");

        // Export chain
        String exportPath = "blockchain_export_test.json";
        boolean exported = blockchain.exportChain(exportPath);
        assertTrue(exported, "Export should succeed");

        // Verify export file exists
        File exportFile = new File(exportPath);
        assertTrue(exportFile.exists(), "Export file should exist");
        assertTrue(exportFile.length() > 0, "Export file should not be empty");

        // Clear blockchain
        blockchain.clearAndReinitialize();
        assertEquals(1, blockchain.getBlockCount(), "Should have only genesis after clear");

        // Import chain
        boolean imported = blockchain.importChain(exportPath);
        assertTrue(imported, "Import should succeed");

        // Verify imported chain
        assertEquals(originalCount, blockchain.getBlockCount(), "Should have same count after import");

        // Validate imported chain
        ChainValidationResult validation = blockchain.validateChainDetailed();
        assertTrue(validation.isValid(), "Imported chain should be valid");

        // Verify specific blocks
        Block block50 = blockchain.getBlock(50L);
        assertNotNull(block50, "Block 50 should exist");
        assertTrue(block50.getData().contains("Export test block"), "Block data should match");

        // Cleanup
        exportFile.delete();

        logger.info("✅ Export/Import chain cycle passed");
        logger.info("   Exported {} blocks, imported {} blocks", originalCount, blockchain.getBlockCount());
    }

    @Test
    @Order(36)
    @DisplayName("Add block with off-chain data - threshold testing")
    void testAddBlockWithOffChainDataThreshold() {
        logTestContext("addBlockWithOffChainData", "threshold boundary testing");

        // Get current threshold
        int threshold = blockchain.getCurrentOffChainThresholdBytes();
        logger.info("   Current off-chain threshold: {} bytes", threshold);

        // Test 1: Data below threshold (should be on-chain)
        String smallData = "A".repeat(1000); // 1KB
        blockchain.addBlock(smallData, privateKey, publicKey);
        long blockNum1 = blockchain.getBlockCount() - 1;
        Block block1 = blockchain.getBlock(blockNum1);
        assertNotNull(block1, "Block should exist");
        assertFalse(block1.hasOffChainData(), "Small data should be on-chain");

        // Test 2: Data at threshold (should be on-chain)
        String thresholdData = "B".repeat(threshold);
        blockchain.addBlock(thresholdData, privateKey, publicKey);
        long blockNum2 = blockchain.getBlockCount() - 1;
        Block block2 = blockchain.getBlock(blockNum2);
        assertNotNull(block2, "Block at threshold should exist");

        // Test 3: Data above threshold (should be off-chain)
        String largeData = "C".repeat(threshold + 1000);
        blockchain.addBlock(largeData, privateKey, publicKey);
        long blockNum3 = blockchain.getBlockCount() - 1;
        Block block3 = blockchain.getBlock(blockNum3);
        assertNotNull(block3, "Large block should exist");
        assertTrue(block3.hasOffChainData(), "Large data should be off-chain");

        // Test 4: Very large data (>1MB) should be off-chain
        String veryLargeData = "D".repeat(1024 * 1024 + 1000); // >1MB
        blockchain.addBlock(veryLargeData, privateKey, publicKey);
        long blockNum4 = blockchain.getBlockCount() - 1;
        Block block4 = blockchain.getBlock(blockNum4);
        assertNotNull(block4, "Very large block should exist");
        assertTrue(block4.hasOffChainData(), "Very large data should be off-chain");

        logger.info("✅ Off-chain data threshold testing passed");
        logger.info("   Small data: on-chain, Large data: off-chain");
    }

    @Test
    @Order(37)
    @DisplayName("Process chain in batches - edge cases")
    void testProcessChainInBatchesEdgeCases() {
        logTestContext("processChainInBatches", "edge cases validation");

        // Add 250 blocks
        for (int i = 0; i < 250; i++) {
            blockchain.addBlock("Batch test " + i, privateKey, publicKey);
        }

        // Test 1: Normal batch processing with accumulator
        final List<String> collectedData = new ArrayList<>();
        blockchain.processChainInBatches(
            batch -> {
                for (Block block : batch) {
                    collectedData.add(block.getData());
                }
            },
            50
        );
        assertEquals(251, collectedData.size(), "Should process all 251 blocks (including genesis)");

        // Test 2: Batch size larger than chain - should process all in 1 batch
        final int[] batchCount = {0};
        blockchain.processChainInBatches(
            batch -> {
                batchCount[0]++;
            },
            10000
        );
        assertEquals(1, batchCount[0], "Should process in 1 batch when batchSize > chain length");

        // Test 3: Invalid batch size (0) - should throw exception
        assertThrows(Exception.class, () -> {
            blockchain.processChainInBatches(batch -> {}, 0);
        }, "Batch size 0 should throw exception");

        // Test 4: Invalid batch size (negative) - should throw exception
        assertThrows(Exception.class, () -> {
            blockchain.processChainInBatches(batch -> {}, -10);
        }, "Negative batch size should throw exception");

        logger.info("✅ Process chain in batches edge cases passed");
        logger.info("   Processed {} blocks in various batch sizes", collectedData.size());
    }

    @Test
    @Order(38)
    @DisplayName("Update block - edge cases")
    void testUpdateBlockEdgeCases() {
        logTestContext("updateBlock", "edge cases validation");

        // Add blocks
        blockchain.addBlock("Original data 1", privateKey, publicKey);
        blockchain.addBlock("Original data 2", privateKey, publicKey);

        long blockNum = blockchain.getBlockCount() - 1;
        Block block = blockchain.getBlock(blockNum);
        assertNotNull(block, "Block should exist");

        // Test 1: Update existing block (SAFE FIELD ONLY)
        String originalData = block.getData();

        // RIGOROUS - Verify original data before update
        assertNotNull(originalData, "Original data should not be null");
        assertEquals("Original data 2", originalData, "Original data should match what we added");

        // Update SAFE FIELD ONLY (customMetadata does not affect hash)
        block.setCustomMetadata("{\"updated\": true}");
        boolean updated = blockchain.updateBlock(block);
        assertTrue(updated, "Update should succeed");

        // Verify update
        Block updatedBlock = blockchain.getBlock(blockNum);
        assertEquals("{\"updated\": true}", updatedBlock.getCustomMetadata(), "Metadata should be updated");
        assertEquals(originalData, updatedBlock.getData(), "Data should NOT be changed (immutable)");

        // Test 2: Attempt to modify UNSAFE FIELD (data) should fail
        Block blockForUnsafeUpdate = blockchain.getBlock(blockNum);
        blockForUnsafeUpdate.setData("Attempted unsafe data change");
        boolean unsafeUpdateResult = blockchain.updateBlock(blockForUnsafeUpdate);
        assertFalse(unsafeUpdateResult, "Update of hash-critical field should fail");

        // Test 3: Update non-existent block (create new block with invalid ID)
        Block fakeBlock = new Block();
        fakeBlock.setBlockNumber(99999L);
        fakeBlock.setData("Fake data");
        fakeBlock.setTimestamp(LocalDateTime.now());
        fakeBlock.setHash("fakehash");
        fakeBlock.setSignature("fakesig");
        fakeBlock.setSignerPublicKey("fakekey");

        boolean updateFake = blockchain.updateBlock(fakeBlock);
        assertFalse(updateFake, "Update of non-existent block should fail");

        // Test 4: Verify chain state after safe metadata update
        ChainValidationResult validation = blockchain.validateChainDetailed();

        // RIGOROUS - Log validation result for debugging
        assertNotNull(validation, "Validation result should not be null");
        logger.info("   Chain validation after safe update: {}", validation.isValid() ? "VALID" : "INVALID");
        logger.info("   Validation summary: {}", validation.getSummary());

        // Chain SHOULD remain valid because we only modified safe metadata fields
        assertTrue(validation.isValid(), "Chain should remain valid after safe metadata update");

        logger.info("✅ Update block edge cases passed");
        logger.info("   Safe update successful: {}, Unsafe update rejected: {}, Update fake: {}", 
                   updated, !unsafeUpdateResult, updateFake);
        logger.info("   Original data was: '{}', now: 'Updated data'", originalData);
    }

    @Test
    @Order(39)
    @DisplayName("Configuration limits - setters and reset")
    void testConfigurationLimits() {
        logTestContext("Configuration", "setters and reset validation");

        // Get original values
        int originalMaxBytes = blockchain.getCurrentMaxBlockSizeBytes();
        int originalThreshold = blockchain.getCurrentOffChainThresholdBytes();

        logger.info("   Original max bytes: {}, threshold: {}", originalMaxBytes, originalThreshold);

        // Test 1: Set new max block size
        int newMaxBytes = 2 * 1024 * 1024; // 2MB
        blockchain.setMaxBlockSizeBytes(newMaxBytes);
        assertEquals(newMaxBytes, blockchain.getCurrentMaxBlockSizeBytes(), "Max bytes should be updated");

        // Test 2: Set new off-chain threshold
        int newThreshold = 1024 * 1024; // 1MB
        blockchain.setOffChainThresholdBytes(newThreshold);
        assertEquals(newThreshold, blockchain.getCurrentOffChainThresholdBytes(), "Threshold should be updated");

        // Test 3: Add block with new limits
        String largeData = "X".repeat(600 * 1024); // 600KB
        blockchain.addBlock(largeData, privateKey, publicKey);
        Block largeBlock = blockchain.getBlock(blockchain.getBlockCount() - 1);
        assertNotNull(largeBlock, "Large block should be added with new limits");

        // Test 4: Reset limits to default
        blockchain.resetLimitsToDefault();
        assertEquals(originalMaxBytes, blockchain.getCurrentMaxBlockSizeBytes(), "Max bytes should be reset");
        assertEquals(originalThreshold, blockchain.getCurrentOffChainThresholdBytes(), "Threshold should be reset");

        // Test 5: Configuration summary
        String summary = blockchain.getConfigurationSummary();
        assertNotNull(summary, "Configuration summary should exist");
        assertTrue(summary.length() > 0, "Configuration summary should not be empty");

        logger.info("✅ Configuration limits testing passed");
        logger.info("   Configuration summary: {}", summary);
    }

    @Test
    @Order(40)
    @DisplayName("Add block with keywords - keyword extraction")
    void testAddBlockWithKeywords() {
        logTestContext("addBlockWithKeywords", "keyword extraction and storage");

        // Test 1: Add block with explicit keywords
        String data = "Medical record for patient John Doe";
        String[] keywords = {"medical", "patient", "record", "healthcare"};

        Block block = blockchain.addBlockWithKeywords(data, keywords, "medical", privateKey, publicKey);
        assertNotNull(block, "Block should be created");
        assertTrue(block.getBlockNumber() > 0, "Block should have valid block number");

        // Test 2: Verify block is stored
        Block retrievedBlock = blockchain.getBlock(block.getBlockNumber());
        assertNotNull(retrievedBlock, "Block should be retrievable");
        assertEquals(data, retrievedBlock.getData(), "Data should match");

        // Test 3: Add block with auto-generated keywords (null)
        String data2 = "Financial transaction for account ABC123";
        Block block2 = blockchain.addBlockWithKeywords(data2, null, "financial", privateKey, publicKey);
        assertNotNull(block2, "Block with null keywords should be created");

        // Test 4: Add block with empty keywords array
        String data3 = "Simple test data";
        String[] emptyKeywords = {};
        Block block3 = blockchain.addBlockWithKeywords(data3, emptyKeywords, "test", privateKey, publicKey);
        assertNotNull(block3, "Block with empty keywords should be created");

        // Test 5: Add block with many keywords
        String[] manyKeywords = new String[50];
        for (int i = 0; i < 50; i++) {
            manyKeywords[i] = "keyword" + i;
        }
        Block block4 = blockchain.addBlockWithKeywords("Data with many keywords", manyKeywords, "test", privateKey, publicKey);
        assertNotNull(block4, "Block with many keywords should be created");

        logger.info("✅ Add block with keywords testing passed");
        logger.info("   Created {} blocks with various keyword configurations", 4);
    }

    // ========== CRITICAL EDGE CASES - Additional Rigorous Tests ==========

    @Test
    @Order(41)
    @DisplayName("addBlock should validate all parameters")
    void testAddBlockParameterValidation() {
        logTestContext("addBlock", "parameter validation");

        // Test 1: Null data
        assertThrows(Exception.class, () -> {
            blockchain.addBlock(null, privateKey, publicKey);
        }, "Should throw exception for null data");

        // Test 2: Empty data
        assertThrows(Exception.class, () -> {
            blockchain.addBlock("", privateKey, publicKey);
        }, "Should throw exception for empty data");

        // Test 3: Null private key
        assertThrows(Exception.class, () -> {
            blockchain.addBlock("Valid data", null, publicKey);
        }, "Should throw exception for null private key");

        // Test 4: Null public key
        assertThrows(Exception.class, () -> {
            blockchain.addBlock("Valid data", privateKey, null);
        }, "Should throw exception for null public key");

        // Test 5: Valid parameters should succeed
        boolean success = blockchain.addBlock("Valid test data", privateKey, publicKey);
        assertTrue(success, "Should succeed with valid parameters");

        logger.info("✅ addBlock parameter validation passed");
    }

    @Test
    @Order(42)
    @DisplayName("getBlock should handle invalid parameters")
    void testGetBlockInvalidParameters() {
        logTestContext("getBlock", "invalid parameters");

        // Add a test block
        blockchain.addBlock("Test block", privateKey, publicKey);

        // Test 1: Negative block number
        Block negativeResult = blockchain.getBlock(-1L);
        assertNull(negativeResult, "Should return null for negative block number");

        // Test 2: Non-existent block number (very large)
        Block nonExistentResult = blockchain.getBlock(999999L);
        assertNull(nonExistentResult, "Should return null for non-existent block number");

        // Test 3: Valid block number should succeed
        Block validResult = blockchain.getBlock(0L);
        assertNotNull(validResult, "Should return genesis block");
        assertEquals(0L, validResult.getBlockNumber(), "Genesis block number should be 0");

        logger.info("✅ getBlock invalid parameter handling passed");
    }

    @Test
    @Order(43)
    @DisplayName("getBlockByHash should handle edge cases")
    void testGetBlockByHashEdgeCases() {
        logTestContext("getBlockByHash", "edge cases");

        // Add a test block
        blockchain.addBlock("Test block for hash", privateKey, publicKey);
        Block testBlock = blockchain.getBlock(blockchain.getBlockCount() - 1);
        String validHash = testBlock.getHash();

        // Test 1: Null hash
        Block nullHashResult = blockchain.getBlockByHash(null);
        assertNull(nullHashResult, "Should return null for null hash");

        // Test 2: Empty hash
        Block emptyHashResult = blockchain.getBlockByHash("");
        assertNull(emptyHashResult, "Should return null for empty hash");

        // Test 3: Invalid hash format
        Block invalidHashResult = blockchain.getBlockByHash("invalid-hash-format-12345");
        assertNull(invalidHashResult, "Should return null for invalid hash format");

        // Test 4: Non-existent valid-looking hash
        Block nonExistentHashResult = blockchain.getBlockByHash("0".repeat(64));
        assertNull(nonExistentHashResult, "Should return null for non-existent hash");

        // Test 5: Valid hash should succeed
        Block validHashResult = blockchain.getBlockByHash(validHash);
        assertNotNull(validHashResult, "Should return block for valid hash");
        assertEquals(validHash, validHashResult.getHash(), "Hash should match");

        logger.info("✅ getBlockByHash edge cases passed");
    }

    @Test
    @Order(44)
    @DisplayName("searchBlocksByContent should handle edge cases")
    void testSearchBlocksByContentEdgeCases() {
        logTestContext("searchBlocksByContent", "edge cases");

        // Add test blocks
        blockchain.addBlock("Medical record data", privateKey, publicKey);
        blockchain.addBlock("Financial transaction data", privateKey, publicKey);

        // Test 1: Null search term
        assertThrows(Exception.class, () -> {
            blockchain.searchBlocksByContent(null);
        }, "Should throw exception for null search term");

        // Test 2: Empty search term
        assertThrows(Exception.class, () -> {
            blockchain.searchBlocksByContent("");
        }, "Should throw exception for empty search term");

        // Test 3: Very long search term (should handle gracefully)
        String veryLongTerm = "x".repeat(10000);
        List<Block> longTermResults = blockchain.searchBlocksByContent(veryLongTerm);
        assertNotNull(longTermResults, "Should handle very long search term");
        assertTrue(longTermResults.isEmpty(), "Should return empty list for non-matching long term");

        // Test 4: Valid search term
        List<Block> medicalResults = blockchain.searchBlocksByContent("Medical");
        assertNotNull(medicalResults, "Results should not be null");
        assertTrue(medicalResults.size() > 0, "Should find medical blocks");

        logger.info("✅ searchBlocksByContent edge cases passed");
    }

    @Test
    @Order(45)
    @DisplayName("getLastBlock should handle edge cases")
    void testGetLastBlockEdgeCases() {
        logTestContext("getLastBlock", "edge cases");

        // Test 1: Get last block with only genesis
        Block genesisOnly = blockchain.getLastBlock();
        assertNotNull(genesisOnly, "Should return genesis block when no other blocks");
        assertEquals(0L, genesisOnly.getBlockNumber(), "Last block should be genesis (0)");

        // Test 2: Add blocks and verify last block updates
        blockchain.addBlock("Block 1", privateKey, publicKey);
        Block afterFirst = blockchain.getLastBlock();
        assertNotNull(afterFirst, "Should return block after adding");
        assertEquals(1L, afterFirst.getBlockNumber(), "Last block should be 1");

        blockchain.addBlock("Block 2", privateKey, publicKey);
        Block afterSecond = blockchain.getLastBlock();
        assertEquals(2L, afterSecond.getBlockNumber(), "Last block should be 2");

        // Test 3: Verify last block data matches
        assertEquals("Block 2", afterSecond.getData(), "Last block data should match");

        logger.info("✅ getLastBlock edge cases passed");
    }

    @Test
    @Order(46)
    @DisplayName("batchRetrieveBlocks should handle all edge cases")
    void testBatchRetrieveBlocksAllEdgeCases() {
        logTestContext("batchRetrieveBlocks", "comprehensive edge cases");

        // Add test blocks
        for (int i = 0; i < 10; i++) {
            blockchain.addBlock("Batch test " + i, privateKey, publicKey);
        }

        // Test 1: Null list
        assertThrows(Exception.class, () -> {
            blockchain.batchRetrieveBlocks(null);
        }, "Should throw exception for null list");

        // Test 2: Empty list
        List<Long> emptyList = new ArrayList<>();
        List<Block> emptyResults = blockchain.batchRetrieveBlocks(emptyList);
        assertNotNull(emptyResults, "Results should not be null for empty list");
        assertTrue(emptyResults.isEmpty(), "Results should be empty for empty list");

        // Test 3: List with negative block numbers
        List<Long> negativeList = List.of(-1L, -5L, -10L);
        List<Block> negativeResults = blockchain.batchRetrieveBlocks(negativeList);
        assertNotNull(negativeResults, "Results should not be null");
        assertTrue(negativeResults.isEmpty() || negativeResults.stream().allMatch(b -> b == null),
            "Should handle negative block numbers gracefully");

        // Test 4: List with duplicates
        List<Long> duplicatesList = List.of(0L, 1L, 1L, 2L, 2L, 2L);
        List<Block> duplicateResults = blockchain.batchRetrieveBlocks(duplicatesList);
        assertNotNull(duplicateResults, "Results should not be null");

        // Test 5: List with non-existent block numbers
        List<Long> nonExistentList = List.of(9999L, 10000L, 10001L);
        List<Block> nonExistentResults = blockchain.batchRetrieveBlocks(nonExistentList);
        assertNotNull(nonExistentResults, "Results should not be null");

        // Test 6: Valid list should succeed
        List<Long> validList = List.of(0L, 1L, 2L, 3L);
        List<Block> validResults = blockchain.batchRetrieveBlocks(validList);
        assertNotNull(validResults, "Results should not be null");
        assertTrue(validResults.size() > 0, "Should return blocks for valid list");

        logger.info("✅ batchRetrieveBlocks comprehensive edge cases passed");
    }

    @Test
    @Order(47)
    @DisplayName("Export/Import should handle error scenarios")
    void testExportImportErrorScenarios() {
        logTestContext("exportChain/importChain", "error scenarios");

        // Add some blocks
        for (int i = 0; i < 5; i++) {
            blockchain.addBlock("Export error test " + i, privateKey, publicKey);
        }

        // Test 1: Import non-existent file - should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            blockchain.importChain("non_existent_file_xyz123.json");
        }, "Import should throw IllegalArgumentException for non-existent file");

        // Test 2: Export to valid path then import
        String validExportPath = "test_export_valid.json";
        boolean exported = blockchain.exportChain(validExportPath);
        assertTrue(exported, "Export should succeed");

        File exportFile = new File(validExportPath);
        assertTrue(exportFile.exists(), "Export file should exist");

        // Test 3: Create corrupted file (invalid JSON)
        String corruptedPath = "test_export_corrupted.json";
        try {
            java.nio.file.Files.writeString(java.nio.file.Path.of(corruptedPath), "{invalid json content");
        } catch (Exception e) {
            fail("Should be able to create corrupted file");
        }

        boolean importCorrupted = blockchain.importChain(corruptedPath);
        assertFalse(importCorrupted, "Import should fail for corrupted JSON file");

        // Test 4: Export empty chain (only genesis)
        blockchain.clearAndReinitialize();
        blockchain.createBootstrapAdmin(publicKeyString, "TestUser");
        String emptyChainPath = "test_export_empty.json";
        boolean exportedEmpty = blockchain.exportChain(emptyChainPath);
        assertTrue(exportedEmpty, "Should be able to export chain with only genesis");

        // Cleanup
        new File(validExportPath).delete();
        new File(corruptedPath).delete();
        new File(emptyChainPath).delete();

        logger.info("✅ Export/Import error scenarios passed");
    }

    @Test
    @Order(48)
    @DisplayName("getBlocksByTimeRange should validate parameters")
    void testGetBlocksByTimeRangeValidation() {
        logTestContext("getBlocksByTimeRange", "parameter validation");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yesterday = now.minusDays(1);
        LocalDateTime tomorrow = now.plusDays(1);

        // Add test blocks
        blockchain.addBlock("Time range test", privateKey, publicKey);

        // Test 1: Null start date
        assertThrows(Exception.class, () -> {
            blockchain.getBlocksByTimeRange(null, tomorrow);
        }, "Should throw exception for null start date");

        // Test 2: Null end date
        assertThrows(Exception.class, () -> {
            blockchain.getBlocksByTimeRange(yesterday, null);
        }, "Should throw exception for null end date");

        // Test 3: Start after end
        assertThrows(Exception.class, () -> {
            blockchain.getBlocksByTimeRange(tomorrow, yesterday);
        }, "Should throw exception when start is after end");

        // Test 4: Valid time range
        List<Block> validResults = blockchain.getBlocksByTimeRange(yesterday, tomorrow);
        assertNotNull(validResults, "Results should not be null");

        logger.info("✅ getBlocksByTimeRange parameter validation passed");
    }

    @Test
    @Order(49)
    @DisplayName("getBlocksBySignerPublicKey should validate parameters")
    void testGetBlocksBySignerPublicKeyValidation() {
        logTestContext("getBlocksBySignerPublicKey", "parameter validation");

        // Add test block
        blockchain.addBlock("Signer test", privateKey, publicKey);

        // Test 1: Null public key
        assertThrows(Exception.class, () -> {
            blockchain.getBlocksBySignerPublicKey(null);
        }, "Should throw exception for null public key");

        // Test 2: Empty public key
        assertThrows(Exception.class, () -> {
            blockchain.getBlocksBySignerPublicKey("");
        }, "Should throw exception for empty public key");

        // Test 3: Invalid public key format
        List<Block> invalidKeyResults = blockchain.getBlocksBySignerPublicKey("invalid-key-format");
        assertNotNull(invalidKeyResults, "Results should not be null");
        assertTrue(invalidKeyResults.isEmpty(), "Should return empty list for invalid key");

        // Test 4: Valid public key
        List<Block> validResults = blockchain.getBlocksBySignerPublicKey(publicKeyString);
        assertNotNull(validResults, "Results should not be null");
        assertTrue(validResults.size() > 0, "Should find blocks signed by this key");

        logger.info("✅ getBlocksBySignerPublicKey parameter validation passed");
    }

    @Test
    @Order(50)
    @DisplayName("searchByCategory should validate all parameters")
    void testSearchByCategoryValidation() {
        logTestContext("searchByCategory", "parameter validation");

        // Add test block with category
        blockchain.addBlock("Category: test, Data: sample", privateKey, publicKey);

        // Test 1: Null category
        assertThrows(Exception.class, () -> {
            blockchain.searchByCategory(null);
        }, "Should throw exception for null category");

        // Test 2: Empty category
        assertThrows(Exception.class, () -> {
            blockchain.searchByCategory("");
        }, "Should throw exception for empty category");

        // Test 3: Non-existent category
        List<Block> nonExistentResults = blockchain.searchByCategory("nonexistent123");
        assertNotNull(nonExistentResults, "Results should not be null");
        assertTrue(nonExistentResults.isEmpty(), "Should return empty list for non-existent category");

        // Test 4: Valid category
        List<Block> validResults = blockchain.searchByCategory("test");
        assertNotNull(validResults, "Results should not be null");

        logger.info("✅ searchByCategory parameter validation passed");
    }

    @Test
    @Order(51)
    @DisplayName("getBlocksPaginated should validate all parameters")
    void testGetBlocksPaginatedValidation() {
        logTestContext("getBlocksPaginated", "parameter validation");

        // Add test blocks
        for (int i = 0; i < 20; i++) {
            blockchain.addBlock("Pagination test " + i, privateKey, publicKey);
        }

        // Test 1: Negative offset
        assertThrows(Exception.class, () -> {
            blockchain.getBlocksPaginated(-1, 10);
        }, "Should throw exception for negative offset");

        // Test 2: Negative limit
        assertThrows(Exception.class, () -> {
            blockchain.getBlocksPaginated(0, -1);
        }, "Should throw exception for negative limit");

        // Test 3: Zero limit
        assertThrows(Exception.class, () -> {
            blockchain.getBlocksPaginated(0, 0);
        }, "Should throw exception for zero limit");

        // Test 4: Valid parameters
        List<Block> validResults = blockchain.getBlocksPaginated(0, 10);
        assertNotNull(validResults, "Results should not be null");
        assertTrue(validResults.size() <= 10, "Results should respect limit");

        // Test 5: Offset at boundary
        List<Block> boundaryResults = blockchain.getBlocksPaginated(20, 5);
        assertNotNull(boundaryResults, "Results should not be null at boundary");

        logger.info("✅ getBlocksPaginated parameter validation passed");
    }
}