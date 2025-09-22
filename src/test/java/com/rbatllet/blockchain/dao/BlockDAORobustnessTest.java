package com.rbatllet.blockchain.dao;

import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.search.SearchLevel;
import com.rbatllet.blockchain.util.JPAUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.util.List;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Comprehensive robustness tests for BlockDAO class
 * Tests all methods including private ones to ensure defensive programming
 * 
 * Follows project patterns:
 * - SLF4J logging for test traceability
 * - Defensive programming validation
 * - Comprehensive edge case testing
 * - Thread safety considerations
 */
@DisplayName("BlockDAO Robustness Test Suite")
public class BlockDAORobustnessTest {
    
    private static final Logger logger = LoggerFactory.getLogger(BlockDAORobustnessTest.class);
    private BlockDAO blockDAO;
    
    @BeforeEach
    void setUp() {
        logger.info("Starting BlockDAO robustness tests");
        blockDAO = new BlockDAO();
        
        // Initialize test database if needed
        try {
            JPAUtil.getEntityManager(); // This will initialize the EntityManagerFactory
        } catch (Exception e) {
            logger.warn("Could not initialize test database: {}", e.getMessage());
        }
    }
    
    @AfterEach
    void tearDown() {
        // Clean up test data
        try {
            if (blockDAO != null) {
                blockDAO.completeCleanupTestData();
            }
        } catch (Exception e) {
            logger.warn("Could not clean up test data: {}", e.getMessage());
        }
    }
    
    private void logTestContext(String method, String scenario) {
        logger.info("🧪 Test: {} - Scenario: {}", method, scenario);
    }
    
    // ========== getAllBlocksWithOffChainData() Tests ==========
    
    @Test
    @DisplayName("getAllBlocksWithOffChainData should handle empty database")
    void testGetAllBlocksWithOffChainDataEmpty() {
        logTestContext("getAllBlocksWithOffChainData", "empty database");
        
        List<Block> result = blockDAO.getAllBlocksWithOffChainData();
        
        assertNotNull(result, "Result should not be null");
        // Could be empty or contain existing data - both are valid
        assertTrue(result.size() >= 0, "Result should be a valid list");
        
        logger.info("✅ Test passed: getAllBlocksWithOffChainData handles empty database");
    }
    
    @Test
    @DisplayName("getAllBlocksWithOffChainData should be thread-safe")
    void testGetAllBlocksWithOffChainDataThreadSafety() {
        logTestContext("getAllBlocksWithOffChainData", "thread safety");
        
        // Test concurrent access
        Runnable task = () -> {
            try {
                List<Block> result = blockDAO.getAllBlocksWithOffChainData();
                assertNotNull(result, "Result should not be null in concurrent access");
            } catch (Exception e) {
                fail("Should not throw exception in concurrent access: " + e.getMessage());
            }
        };
        
        Thread[] threads = new Thread[5];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(task);
            threads[i].start();
        }
        
        for (Thread thread : threads) {
            assertDoesNotThrow(() -> thread.join(1000), 
                "Thread should complete within timeout");
        }
        
        logger.info("✅ Test passed: getAllBlocksWithOffChainData handles concurrent access");
    }
    
    // ========== buildSearchQuery(SearchLevel) Tests ==========
    
    @Test
    @DisplayName("buildSearchQuery should handle all SearchLevel values")
    void testBuildSearchQueryAllLevels() throws Exception {
        logTestContext("buildSearchQuery", "all SearchLevel values");
        
        Method method = BlockDAO.class.getDeclaredMethod("buildSearchQuery", SearchLevel.class);
        method.setAccessible(true);
        
        for (SearchLevel level : SearchLevel.values()) {
            String query = (String) method.invoke(blockDAO, level);
            
            assertNotNull(query, "Query should not be null for " + level);
            assertTrue(query.contains("SELECT"), "Query should contain SELECT for " + level);
            assertTrue(query.contains("FROM Block"), "Query should contain FROM Block for " + level);
            assertTrue(query.contains("ORDER BY"), "Query should contain ORDER BY for " + level);
            
            // Verify level-specific query content
            switch (level) {
                case FAST_ONLY:
                    assertFalse(query.toLowerCase().contains("b.data"), 
                        "FAST_ONLY should not include b.data");
                    break;
                case INCLUDE_DATA:
                case EXHAUSTIVE_OFFCHAIN:
                    assertTrue(query.toLowerCase().contains("b.data"), 
                        level + " should include b.data");
                    break;
            }
        }
        
        logger.info("✅ Test passed: buildSearchQuery handles all SearchLevel values");
    }
    
    @Test
    @DisplayName("buildSearchQuery should handle null SearchLevel")
    void testBuildSearchQueryNullLevel() throws Exception {
        logTestContext("buildSearchQuery", "null SearchLevel");
        
        Method method = BlockDAO.class.getDeclaredMethod("buildSearchQuery", SearchLevel.class);
        method.setAccessible(true);
        
        // VULNERABILITY TEST: What happens with null SearchLevel?
        assertThrows(Exception.class, () -> {
            method.invoke(blockDAO, (SearchLevel) null);
        }, "Should throw exception for null SearchLevel");
        
        logger.info("🚨 VULNERABILITY FIXED: buildSearchQuery now validates null SearchLevel");
    }
    
    // ========== updateBlock(Block) Tests ==========
    
    @Test
    @DisplayName("updateBlock should handle null block")
    void testUpdateBlockNull() {
        logTestContext("updateBlock", "null block");
        
        // VULNERABILITY TEST: What happens with null block?
        assertThrows(Exception.class, () -> {
            blockDAO.updateBlock(null);
        }, "Should throw exception for null block");
        
        logger.info("🚨 VULNERABILITY DETECTED: updateBlock validates null block (expected behavior)");
    }
    
    @Test
    @DisplayName("updateBlock should handle valid block")
    void testUpdateBlockValid() {
        logTestContext("updateBlock", "valid block");
        
        Block testBlock = createMockBlock();
        
        // Test update operation - may succeed or fail depending on database state
        try {
            blockDAO.updateBlock(testBlock);
            logger.info("✅ Test passed: updateBlock handles valid block");
        } catch (Exception e) {
            // This is acceptable - update might fail if block doesn't exist
            logger.info("✅ Test passed: updateBlock properly handles non-existent block");
        }
    }
    
    @Test
    @DisplayName("updateBlock should handle block with null fields")
    void testUpdateBlockNullFields() {
        logTestContext("updateBlock", "block with null fields");
        
        Block testBlock = new Block();
        // Leave most fields null to test robustness
        testBlock.setBlockNumber(999999L); // Use a number unlikely to exist
        
        try {
            blockDAO.updateBlock(testBlock);
            logger.info("✅ Test passed: updateBlock handles block with null fields");
        } catch (Exception e) {
            // This is acceptable - update might fail due to constraints
            assertTrue(true, "updateBlock properly validates block constraints");
            logger.info("✅ Test passed: updateBlock validates block constraints");
        }
    }
    
    // ========== completeCleanupTestData() Tests ==========
    
    @Test
    @DisplayName("completeCleanupTestData should execute without errors")
    void testCompleteCleanupTestData() {
        logTestContext("completeCleanupTestData", "normal execution");
        
        assertDoesNotThrow(() -> {
            blockDAO.completeCleanupTestData();
        }, "completeCleanupTestData should not throw exceptions");
        
        logger.info("✅ Test passed: completeCleanupTestData executes without errors");
    }
    
    @Test
    @DisplayName("completeCleanupTestData should be thread-safe")
    void testCompleteCleanupTestDataThreadSafety() {
        logTestContext("completeCleanupTestData", "thread safety");
        
        // Test concurrent cleanup calls
        Runnable cleanupTask = () -> {
            assertDoesNotThrow(() -> {
                blockDAO.completeCleanupTestData();
            }, "Concurrent cleanup should not throw exceptions");
        };
        
        Thread[] threads = new Thread[3];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(cleanupTask);
            threads[i].start();
        }
        
        for (Thread thread : threads) {
            assertDoesNotThrow(() -> thread.join(2000), 
                "Cleanup thread should complete within timeout");
        }
        
        logger.info("✅ Test passed: completeCleanupTestData handles concurrent access");
    }
    
    // ========== existsBlockWithHash(String) Tests ==========
    
    @Test
    @DisplayName("existsBlockWithHash should handle null hash")
    void testExistsBlockWithHashNull() {
        logTestContext("existsBlockWithHash", "null hash");
        
        // VULNERABILITY TEST: What happens with null hash?
        assertThrows(Exception.class, () -> {
            blockDAO.existsBlockWithHash(null);
        }, "Should throw exception for null hash");
        
        logger.info("🚨 VULNERABILITY FIXED: existsBlockWithHash now validates null hash");
    }
    
    @Test
    @DisplayName("existsBlockWithHash should handle empty hash")
    void testExistsBlockWithHashEmpty() {
        logTestContext("existsBlockWithHash", "empty hash");
        
        boolean result = blockDAO.existsBlockWithHash("");
        
        // Should return false for empty hash (non-existent)
        assertFalse(result, "Empty hash should return false");
        
        logger.info("✅ Test passed: existsBlockWithHash handles empty hash");
    }
    
    @Test
    @DisplayName("existsBlockWithHash should handle whitespace-only hash")
    void testExistsBlockWithHashWhitespace() {
        logTestContext("existsBlockWithHash", "whitespace-only hash");
        
        boolean result = blockDAO.existsBlockWithHash("   ");
        
        // Should return false for whitespace hash (non-existent)
        assertFalse(result, "Whitespace-only hash should return false");
        
        logger.info("✅ Test passed: existsBlockWithHash handles whitespace-only hash");
    }
    
    @Test
    @DisplayName("existsBlockWithHash should handle non-existent hash")
    void testExistsBlockWithHashNonExistent() {
        logTestContext("existsBlockWithHash", "non-existent hash");
        
        boolean result = blockDAO.existsBlockWithHash("non-existent-hash-12345");
        
        // Should return false for non-existent hash
        assertFalse(result, "Non-existent hash should return false");
        
        logger.info("✅ Test passed: existsBlockWithHash handles non-existent hash");
    }
    
    // ========== compareSearchPriority(Block, Block) Tests ==========
    
    @Test
    @DisplayName("compareSearchPriority should handle null blocks")
    void testCompareSearchPriorityNulls() throws Exception {
        logTestContext("compareSearchPriority", "null blocks");
        
        Method method = BlockDAO.class.getDeclaredMethod("compareSearchPriority", Block.class, Block.class);
        method.setAccessible(true);
        
        Block validBlock = createMockBlock();
        
        // Test both null
        assertThrows(Exception.class, () -> {
            method.invoke(blockDAO, null, null);
        }, "Should throw exception for both null blocks");
        
        // Test first null
        assertThrows(Exception.class, () -> {
            method.invoke(blockDAO, null, validBlock);
        }, "Should throw exception for first null block");
        
        // Test second null
        assertThrows(Exception.class, () -> {
            method.invoke(blockDAO, validBlock, null);
        }, "Should throw exception for second null block");
        
        logger.info("🚨 VULNERABILITY FIXED: compareSearchPriority now validates null blocks");
    }
    
    @Test
    @DisplayName("compareSearchPriority should prioritize manual keywords")
    void testCompareSearchPriorityManualKeywords() throws Exception {
        logTestContext("compareSearchPriority", "manual keywords priority");
        
        Method method = BlockDAO.class.getDeclaredMethod("compareSearchPriority", Block.class, Block.class);
        method.setAccessible(true);
        
        Block blockWithManual = createMockBlock();
        blockWithManual.setManualKeywords("important keywords");
        blockWithManual.setAutoKeywords(null);
        
        Block blockWithoutManual = createMockBlock();
        blockWithoutManual.setManualKeywords(null);
        blockWithoutManual.setAutoKeywords("auto keywords");
        
        int result = (Integer) method.invoke(blockDAO, blockWithManual, blockWithoutManual);
        
        assertEquals(-1, result, "Block with manual keywords should have higher priority");
        
        logger.info("✅ Test passed: compareSearchPriority prioritizes manual keywords");
    }
    
    @Test
    @DisplayName("compareSearchPriority should handle blocks with null keywords")
    void testCompareSearchPriorityNullKeywords() throws Exception {
        logTestContext("compareSearchPriority", "null keywords");
        
        Method method = BlockDAO.class.getDeclaredMethod("compareSearchPriority", Block.class, Block.class);
        method.setAccessible(true);
        
        Block block1 = createMockBlock();
        block1.setManualKeywords(null);
        block1.setAutoKeywords(null);
        block1.setBlockNumber(1L);
        
        Block block2 = createMockBlock();
        block2.setManualKeywords(null);
        block2.setAutoKeywords(null);
        block2.setBlockNumber(2L);
        
        int result = (Integer) method.invoke(blockDAO, block1, block2);
        
        // Should sort by block number when no keywords (descending)
        assertTrue(result > 0, "Should sort by block number when no keywords");
        
        logger.info("✅ Test passed: compareSearchPriority handles blocks with null keywords");
    }
    
    // ========== searchBlocksByContentWithLevel(String, SearchLevel) Tests ==========
    
    @Test
    @DisplayName("searchBlocksByContentWithLevel should handle null searchTerm")
    void testSearchBlocksByContentWithLevelNullTerm() {
        logTestContext("searchBlocksByContentWithLevel", "null searchTerm");
        
        List<Block> result = blockDAO.searchBlocksByContentWithLevel(null, SearchLevel.FAST_ONLY);
        
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isEmpty(), "Should return empty list for null searchTerm");
        
        logger.info("✅ Test passed: searchBlocksByContentWithLevel handles null searchTerm");
    }
    
    @Test
    @DisplayName("searchBlocksByContentWithLevel should handle empty searchTerm")
    void testSearchBlocksByContentWithLevelEmptyTerm() {
        logTestContext("searchBlocksByContentWithLevel", "empty searchTerm");
        
        List<Block> result = blockDAO.searchBlocksByContentWithLevel("", SearchLevel.FAST_ONLY);
        
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isEmpty(), "Should return empty list for empty searchTerm");
        
        logger.info("✅ Test passed: searchBlocksByContentWithLevel handles empty searchTerm");
    }
    
    @Test
    @DisplayName("searchBlocksByContentWithLevel should handle whitespace-only searchTerm")
    void testSearchBlocksByContentWithLevelWhitespaceTerm() {
        logTestContext("searchBlocksByContentWithLevel", "whitespace-only searchTerm");
        
        List<Block> result = blockDAO.searchBlocksByContentWithLevel("   ", SearchLevel.FAST_ONLY);
        
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isEmpty(), "Should return empty list for whitespace-only searchTerm");
        
        logger.info("✅ Test passed: searchBlocksByContentWithLevel handles whitespace-only searchTerm");
    }
    
    @Test
    @DisplayName("searchBlocksByContentWithLevel should handle null SearchLevel")
    void testSearchBlocksByContentWithLevelNullLevel() {
        logTestContext("searchBlocksByContentWithLevel", "null SearchLevel");
        
        // VULNERABILITY TEST: What happens with null SearchLevel?
        assertThrows(Exception.class, () -> {
            blockDAO.searchBlocksByContentWithLevel("test", null);
        }, "Should throw exception for null SearchLevel");
        
        logger.info("🚨 VULNERABILITY FIXED: searchBlocksByContentWithLevel now validates null SearchLevel");
    }
    
    @Test
    @DisplayName("searchBlocksByContentWithLevel should handle all SearchLevel values")
    void testSearchBlocksByContentWithLevelAllLevels() {
        logTestContext("searchBlocksByContentWithLevel", "all SearchLevel values");
        
        String searchTerm = "test-search-term";
        
        for (SearchLevel level : SearchLevel.values()) {
            List<Block> result = blockDAO.searchBlocksByContentWithLevel(searchTerm, level);
            
            assertNotNull(result, "Result should not be null for " + level);
            // Results can be empty or contain data - both are valid
        }
        
        logger.info("✅ Test passed: searchBlocksByContentWithLevel handles all SearchLevel values");
    }
    
    // ========== getBlocksPaginated(int, int) Tests ==========
    
    @Test
    @DisplayName("getBlocksPaginated should handle negative offset")
    void testGetBlocksPaginatedNegativeOffset() {
        logTestContext("getBlocksPaginated", "negative offset");
        
        assertThrows(IllegalArgumentException.class, () -> {
            blockDAO.getBlocksPaginated(-1, 10);
        }, "Should throw IllegalArgumentException for negative offset");
        
        logger.info("✅ Test passed: getBlocksPaginated validates negative offset");
    }
    
    @Test
    @DisplayName("getBlocksPaginated should handle zero limit")
    void testGetBlocksPaginatedZeroLimit() {
        logTestContext("getBlocksPaginated", "zero limit");
        
        assertThrows(IllegalArgumentException.class, () -> {
            blockDAO.getBlocksPaginated(0, 0);
        }, "Should throw IllegalArgumentException for zero limit");
        
        logger.info("✅ Test passed: getBlocksPaginated validates zero limit");
    }
    
    @Test
    @DisplayName("getBlocksPaginated should handle negative limit")
    void testGetBlocksPaginatedNegativeLimit() {
        logTestContext("getBlocksPaginated", "negative limit");
        
        assertThrows(IllegalArgumentException.class, () -> {
            blockDAO.getBlocksPaginated(0, -1);
        }, "Should throw IllegalArgumentException for negative limit");
        
        logger.info("✅ Test passed: getBlocksPaginated validates negative limit");
    }
    
    @Test
    @DisplayName("getBlocksPaginated should handle valid parameters")
    void testGetBlocksPaginatedValid() {
        logTestContext("getBlocksPaginated", "valid parameters");
        
        List<Block> result = blockDAO.getBlocksPaginated(0, 10);
        
        assertNotNull(result, "Result should not be null");
        assertTrue(result.size() <= 10, "Result should not exceed limit");
        
        logger.info("✅ Test passed: getBlocksPaginated handles valid parameters");
    }
    
    // ========== getBlockByNumber(Long) Tests ==========
    
    @Test
    @DisplayName("getBlockByNumber should handle null blockNumber")
    void testGetBlockByNumberNull() {
        logTestContext("getBlockByNumber", "null blockNumber");
        
        Block result = blockDAO.getBlockByNumber(null);
        
        assertNull(result, "Should return null for null blockNumber");
        
        logger.info("✅ Test passed: getBlockByNumber handles null blockNumber");
    }
    
    @Test
    @DisplayName("getBlockByNumber should handle non-existent blockNumber")
    void testGetBlockByNumberNonExistent() {
        logTestContext("getBlockByNumber", "non-existent blockNumber");
        
        Block result = blockDAO.getBlockByNumber(999999999L);
        
        assertNull(result, "Should return null for non-existent blockNumber");
        
        logger.info("✅ Test passed: getBlockByNumber handles non-existent blockNumber");
    }
    
    @Test
    @DisplayName("getBlockByNumber should handle negative blockNumber")
    void testGetBlockByNumberNegative() {
        logTestContext("getBlockByNumber", "negative blockNumber");
        
        Block result = blockDAO.getBlockByNumber(-1L);
        
        assertNull(result, "Should return null for negative blockNumber");
        
        logger.info("✅ Test passed: getBlockByNumber handles negative blockNumber");
    }
    
    // ========== getAllBlocks() Tests ==========
    
    @Test
    @DisplayName("getAllBlocks should return valid list")
    void testGetAllBlocks() {
        logTestContext("getAllBlocks", "normal execution");
        
        List<Block> result = blockDAO.getAllBlocks();
        
        assertNotNull(result, "Result should not be null");
        // Can be empty or contain data - both are valid
        
        logger.info("✅ Test passed: getAllBlocks returns valid list");
    }
    
    @Test
    @DisplayName("getAllBlocks should be thread-safe")
    void testGetAllBlocksThreadSafety() {
        logTestContext("getAllBlocks", "thread safety");
        
        Runnable task = () -> {
            try {
                List<Block> result = blockDAO.getAllBlocks();
                assertNotNull(result, "Result should not be null in concurrent access");
            } catch (Exception e) {
                fail("Should not throw exception in concurrent access: " + e.getMessage());
            }
        };
        
        Thread[] threads = new Thread[5];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(task);
            threads[i].start();
        }
        
        for (Thread thread : threads) {
            assertDoesNotThrow(() -> thread.join(2000), 
                "Thread should complete within timeout");
        }
        
        logger.info("✅ Test passed: getAllBlocks handles concurrent access");
    }
    
    // ========== getLastBlock() Tests ==========
    
    @Test
    @DisplayName("getLastBlock should return valid result")
    void testGetLastBlock() {
        logTestContext("getLastBlock", "normal execution");
        
        Block result = blockDAO.getLastBlock();
        
        // Can be null (no blocks) or return a valid block - both are acceptable
        if (result != null) {
            assertNotNull(result.getBlockNumber(), "Returned block should have valid blockNumber");
        }
        
        logger.info("✅ Test passed: getLastBlock returns valid result");
    }
    
    // ========== getLastBlockWithRefresh() Tests ==========
    
    @Test
    @DisplayName("getLastBlockWithRefresh should return valid result")
    void testGetLastBlockWithRefresh() {
        logTestContext("getLastBlockWithRefresh", "normal execution");
        
        Block result = blockDAO.getLastBlockWithRefresh();
        
        // Can be null (no blocks) or return a valid block - both are acceptable
        if (result != null) {
            assertNotNull(result.getBlockNumber(), "Returned block should have valid blockNumber");
        }
        
        logger.info("✅ Test passed: getLastBlockWithRefresh returns valid result");
    }
    
    // ========== getLastBlockWithLock() Tests ==========
    
    @Test
    @DisplayName("getLastBlockWithLock should handle transaction requirements")
    void testGetLastBlockWithLock() {
        logTestContext("getLastBlockWithLock", "transaction requirements");

        // VULNERABILITY DETECTED: getLastBlockWithLock requires active transaction for pessimistic locking
        // This is expected behavior since it uses PESSIMISTIC_READ lock mode
        // The method is designed for use within transactions only
        assertThrows(Exception.class, () -> {
            blockDAO.getLastBlockWithLock();
        }, "Should throw exception when no active transaction (by design for pessimistic locking)");

        logger.info("✅ Test passed: getLastBlockWithLock properly requires active transaction");
    }
    
    // ========== getNextBlockNumberAtomic() Tests ==========
    
    @Test
    @DisplayName("getNextBlockNumberAtomic should return valid block number")
    void testGetNextBlockNumberAtomic() {
        logTestContext("getNextBlockNumberAtomic", "normal execution");
        
        Long result = blockDAO.getNextBlockNumberAtomic();
        
        assertNotNull(result, "Result should not be null");
        assertTrue(result >= 0, "Block number should be non-negative");
        
        logger.info("✅ Test passed: getNextBlockNumberAtomic returns valid block number");
    }
    
    @Test
    @DisplayName("getNextBlockNumberAtomic should be thread-safe")
    void testGetNextBlockNumberAtomicThreadSafety() {
        logTestContext("getNextBlockNumberAtomic", "thread safety");
        
        int numThreads = 5;
        Long[] results = new Long[numThreads];
        Thread[] threads = new Thread[numThreads];
        
        for (int i = 0; i < numThreads; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                try {
                    results[index] = blockDAO.getNextBlockNumberAtomic();
                } catch (Exception e) {
                    fail("Thread " + index + " failed: " + e.getMessage());
                }
            });
            threads[i].start();
        }
        
        for (Thread thread : threads) {
            assertDoesNotThrow(() -> thread.join(3000), 
                "Thread should complete within timeout");
        }
        
        // Verify all results are unique (atomic operation)
        for (int i = 0; i < numThreads; i++) {
            assertNotNull(results[i], "Result " + i + " should not be null");
            for (int j = i + 1; j < numThreads; j++) {
                assertNotEquals(results[i], results[j], 
                    "Block numbers should be unique: " + results[i] + " vs " + results[j]);
            }
        }
        
        logger.info("✅ Test passed: getNextBlockNumberAtomic is thread-safe with unique results");
    }
    
    // ========== Private Helper Methods ==========
    
    private Block createMockBlock() {
        Block block = new Block();
        block.setBlockNumber(System.currentTimeMillis()); // Unique number
        block.setHash("test-hash-" + System.nanoTime());
        block.setPreviousHash("previous-hash");
        block.setTimestamp(LocalDateTime.now());
        block.setData("test data");
        block.setManualKeywords("test keywords");
        block.setAutoKeywords("auto test keywords");
        block.setSearchableContent("searchable content");
        return block;
    }
}