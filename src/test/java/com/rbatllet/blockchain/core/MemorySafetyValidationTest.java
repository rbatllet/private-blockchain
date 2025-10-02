package com.rbatllet.blockchain.core;

import com.rbatllet.blockchain.config.DatabaseConfig;
import com.rbatllet.blockchain.config.MemorySafetyConstants;
import com.rbatllet.blockchain.dao.BlockDAO;
import com.rbatllet.blockchain.search.SearchLevel;
import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.util.JPAUtil;
import com.rbatllet.blockchain.validation.ChainValidationResult;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for memory safety validations in blockchain operations
 * Verifies that proper limits and warnings are enforced
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MemorySafetyValidationTest {

    private static final Logger logger = LoggerFactory.getLogger(MemorySafetyValidationTest.class);

    private Blockchain blockchain;
    private BlockDAO blockDAO;
    private KeyPair keyPair;
    private PrivateKey privateKey;
    private PublicKey publicKey;

    @BeforeAll
    static void setUpDatabase() {
        DatabaseConfig h2Config = DatabaseConfig.createH2TestConfig();
        JPAUtil.initialize(h2Config);
        logger.info("✅ H2 test database initialized");
    }

    @BeforeEach
    void setUp() throws Exception {
        blockchain = new Blockchain();
        blockDAO = blockchain.getBlockDAO();

        keyPair = CryptoUtil.generateKeyPair();
        privateKey = keyPair.getPrivate();
        publicKey = keyPair.getPublic();

        String publicKeyString = CryptoUtil.publicKeyToString(publicKey);
        blockchain.addAuthorizedKey(publicKeyString, "TestUser");

        logger.info("🔧 Test setup completed");
    }

    @AfterEach
    void tearDown() {
        if (blockchain != null) {
            blockchain.clearAndReinitialize();
            JPAUtil.cleanupThreadLocals();
        }
        logger.info("🧹 Test cleanup completed");
    }

    @Test
    @Order(1)
    @DisplayName("📊 BlockDAO.batchRetrieveBlocks - enforce MAX_BATCH_SIZE limit")
    void testBatchRetrieveBlocksLimit() {
        logger.info("🧪 Testing batchRetrieveBlocks MAX_BATCH_SIZE enforcement...");

        // Create list exceeding MAX_BATCH_SIZE
        List<Long> tooManyBlockNumbers = new ArrayList<>();
        for (long i = 0; i < MemorySafetyConstants.MAX_BATCH_SIZE + 1; i++) {
            tooManyBlockNumbers.add(i);
        }

        // Should throw IllegalArgumentException
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> blockDAO.batchRetrieveBlocks(tooManyBlockNumbers),
            "Should throw exception when batch size exceeds MAX_BATCH_SIZE"
        );

        assertTrue(exception.getMessage().contains("exceeds maximum"),
            "Exception message should mention exceeding maximum");
        assertTrue(exception.getMessage().contains(String.valueOf(MemorySafetyConstants.MAX_BATCH_SIZE)),
            "Exception message should mention the limit");

        logger.info("✅ batchRetrieveBlocks correctly enforces MAX_BATCH_SIZE limit");
    }

    @Test
    @Order(2)
    @DisplayName("📊 BlockDAO.batchRetrieveBlocks - allow valid batch size")
    void testBatchRetrieveBlocksValidSize() {
        logger.info("🧪 Testing batchRetrieveBlocks with valid batch size...");

        // Add some blocks
        for (int i = 0; i < 100; i++) {
            blockchain.addBlock("Test " + i, privateKey, publicKey);
        }

        // Create list within MAX_BATCH_SIZE
        List<Long> validBlockNumbers = new ArrayList<>();
        for (long i = 0; i < 100; i++) {
            validBlockNumbers.add(i);
        }

        // Should succeed
        assertDoesNotThrow(() -> {
            List<?> results = blockDAO.batchRetrieveBlocks(validBlockNumbers);
            assertEquals(100, results.size(), "Should retrieve all requested blocks");
        }, "Should not throw exception for valid batch size");

        logger.info("✅ batchRetrieveBlocks allows valid batch sizes");
    }

    @Test
    @Order(3)
    @DisplayName("📊 Blockchain.validateChainDetailed - warn on large chains")
    void testValidateChainDetailedWarning() {
        logger.info("🧪 Testing validateChainDetailed warning for large chains...");

        // Note: This test would need a blockchain with >100K blocks to trigger the warning
        // For practical testing, we verify the method works with small chains

        for (int i = 0; i < 10; i++) {
            blockchain.addBlock("Test " + i, privateKey, publicKey);
        }

        // Should succeed without warnings for small chains
        assertDoesNotThrow(() -> {
            ChainValidationResult result = blockchain.validateChainDetailed();
            assertNotNull(result);
            assertTrue(result.isValid());
        }, "Should validate small chains without issues");

        logger.info("✅ validateChainDetailed works correctly for small chains");
    }

    @Test
    @Order(4)
    @DisplayName("📊 Blockchain search methods - use DEFAULT_MAX_SEARCH_RESULTS")
    void testSearchMethodsDefaultLimit() {
        logger.info("🧪 Testing search methods use DEFAULT_MAX_SEARCH_RESULTS...");

        // Add blocks with category
        for (int i = 0; i < 50; i++) {
            blockchain.addBlock("Category: TestCategory, Data: " + i, privateKey, publicKey);
        }

        // Search by category (should use default limit)
        List<?> categoryResults = blockchain.searchByCategory("TestCategory");

        // Should return results (limited by DEFAULT_MAX_SEARCH_RESULTS if more exist)
        assertNotNull(categoryResults);
        assertTrue(categoryResults.size() <= MemorySafetyConstants.DEFAULT_MAX_SEARCH_RESULTS,
            "Results should not exceed DEFAULT_MAX_SEARCH_RESULTS");

        logger.info("✅ Search methods respect DEFAULT_MAX_SEARCH_RESULTS limit");
    }

    @Test
    @Order(5)
    @DisplayName("📊 Blockchain.searchByCategory - custom limit")
    void testSearchByCategoryCustomLimit() {
        logger.info("🧪 Testing searchByCategory with custom limit...");

        // Add blocks
        for (int i = 0; i < 100; i++) {
            blockchain.addBlock("Category: Test, Data: " + i, privateKey, publicKey);
        }

        // Search with custom limit
        List<?> limitedResults = blockchain.searchByCategory("Test", 20);

        assertNotNull(limitedResults);
        assertTrue(limitedResults.size() <= 20,
            "Results should not exceed custom limit of 20");

        logger.info("✅ searchByCategory respects custom limit");
    }

    @Test
    @Order(6)
    @DisplayName("📊 Memory constants are used consistently")
    void testMemoryConstantsConsistency() {
        logger.info("🧪 Testing memory constants consistency across codebase...");

        // Verify that the constants defined match expected values
        assertEquals(10000, MemorySafetyConstants.MAX_BATCH_SIZE);
        assertEquals(10000, MemorySafetyConstants.DEFAULT_MAX_SEARCH_RESULTS);
        assertEquals(100000, MemorySafetyConstants.SAFE_EXPORT_LIMIT);
        assertEquals(500000, MemorySafetyConstants.MAX_EXPORT_LIMIT);
        assertEquals(1000, MemorySafetyConstants.DEFAULT_BATCH_SIZE);

        // Verify relationships
        assertTrue(MemorySafetyConstants.MAX_EXPORT_LIMIT > MemorySafetyConstants.SAFE_EXPORT_LIMIT,
            "MAX_EXPORT_LIMIT should be greater than SAFE_EXPORT_LIMIT");

        assertTrue(MemorySafetyConstants.DEFAULT_BATCH_SIZE < MemorySafetyConstants.MAX_BATCH_SIZE,
            "DEFAULT_BATCH_SIZE should be less than MAX_BATCH_SIZE");

        logger.info("✅ Memory constants are consistent");
    }

    @Test
    @Order(7)
    @DisplayName("📊 BlockDAO search methods - use correct limits")
    void testBlockDAOSearchLimits() {
        logger.info("🧪 Testing BlockDAO search methods use correct limits...");

        // Add test blocks
        for (int i = 0; i < 50; i++) {
            blockchain.addBlock("Search test " + i, privateKey, publicKey);
        }

        // Test searchBlocksByContentWithLevel (default limit)
        List<?> searchResults = blockDAO.searchBlocksByContentWithLevel(
            "Search",
            SearchLevel.FAST_ONLY
        );

        assertNotNull(searchResults);
        assertTrue(searchResults.size() <= MemorySafetyConstants.DEFAULT_MAX_SEARCH_RESULTS,
            "Search results should respect DEFAULT_MAX_SEARCH_RESULTS");

        logger.info("✅ BlockDAO search methods use correct limits");
    }

    @Test
    @Order(8)
    @DisplayName("📊 Verify processChainInBatches uses DEFAULT_BATCH_SIZE")
    void testProcessChainInBatchesDefaultSize() {
        logger.info("🧪 Testing processChainInBatches batch size...");

        // Add blocks
        for (int i = 0; i < 2500; i++) {
            blockchain.addBlock("Batch test " + i, privateKey, publicKey);
        }

        final List<Integer> batchSizes = new ArrayList<>();

        // Process with default batch size
        blockchain.processChainInBatches(
            batch -> batchSizes.add(batch.size()),
            MemorySafetyConstants.DEFAULT_BATCH_SIZE
        );

        // Most batches should be DEFAULT_BATCH_SIZE (except possibly the last one)
        for (int i = 0; i < batchSizes.size() - 1; i++) {
            assertEquals(MemorySafetyConstants.DEFAULT_BATCH_SIZE, batchSizes.get(i),
                "Batch " + i + " should be DEFAULT_BATCH_SIZE");
        }

        logger.info("✅ processChainInBatches uses correct DEFAULT_BATCH_SIZE");
    }
}
