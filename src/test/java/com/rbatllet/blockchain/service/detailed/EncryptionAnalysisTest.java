package com.rbatllet.blockchain.service.detailed;

import static org.junit.jupiter.api.Assertions.*;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.security.KeyFileLoader;
import com.rbatllet.blockchain.security.UserRole;
import com.rbatllet.blockchain.service.UserFriendlyEncryptionAPI;
import com.rbatllet.blockchain.service.UserFriendlyEncryptionAPI.EncryptionAnalysis;
import com.rbatllet.blockchain.util.CryptoUtil;
import java.security.KeyPair;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Comprehensive tests for EncryptionAnalysis class
 * focusing on edge cases, null handling, and robustness
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EncryptionAnalysisTest {

    private static final Logger logger = LoggerFactory.getLogger(
        EncryptionAnalysisTest.class
    );

    private Blockchain blockchain;
    private UserFriendlyEncryptionAPI api;
    private KeyPair testUserKeys;
    private KeyPair bootstrapKeyPair;
    private String testPassword;

    @BeforeEach
    void setUp() throws Exception {
        // Clean database and initialize
        blockchain = new Blockchain();
        blockchain.clearAndReinitialize(); // CRITICAL: Clear all data from previous tests
        // BlockRepository now package-private - use clearAndReinitialize();
        blockchain.getAuthorizedKeyDAO().cleanupTestData();

        // Load bootstrap admin keys
        bootstrapKeyPair = KeyFileLoader.loadKeyPairFromFiles(
            "./keys/genesis-admin.private",
            "./keys/genesis-admin.public"
        );

        // Register bootstrap admin in blockchain
        blockchain.createBootstrapAdmin(
            CryptoUtil.publicKeyToString(bootstrapKeyPair.getPublic()),
            "BOOTSTRAP_ADMIN"
        );

        api = new UserFriendlyEncryptionAPI(blockchain);
        testPassword = "TestPassword123!";

        // PRE-AUTHORIZATION REQUIRED (v1.0.6 RBAC security):
        // 1. Create admin to act as authorized user creator
        KeyPair adminKeys = CryptoUtil.generateKeyPair();
        String adminPublicKey = CryptoUtil.publicKeyToString(adminKeys.getPublic());
        blockchain.addAuthorizedKey(adminPublicKey, "Admin", bootstrapKeyPair, UserRole.ADMIN);
        api.setDefaultCredentials("Admin", adminKeys);  // Authenticate as admin

        // 2. Now admin can create test user (generates new keys internally)
        testUserKeys = api.createUser("test-user");
        api.setDefaultCredentials("test-user", testUserKeys);

        logger.info("Test setup completed");
    }

    /**
     * Helper method to create an encrypted block
     */
    private Block createEncryptedBlock(String content, String password)
        throws Exception {
        return blockchain.addEncryptedBlock(
            content,
            password,
            testUserKeys.getPrivate(),
            testUserKeys.getPublic()
        );
    }

    /**
     * Helper method to create a public block
     */
    private Block createPublicBlock(String content) throws Exception {
        return blockchain.addBlockAndReturn(
            content,
            testUserKeys.getPrivate(),
            testUserKeys.getPublic()
        );
    }

    @Test
    @Order(1)
    @DisplayName("Test EncryptionAnalysis constructor with valid values")
    void testEncryptionAnalysisConstructorValid() {
        Map<String, Long> categoryBreakdown = new HashMap<>();
        categoryBreakdown.put("documents", 5L);
        categoryBreakdown.put("images", 3L);

        EncryptionAnalysis analysis = new EncryptionAnalysis(
            10L, // totalBlocks
            6L, // encryptedBlocks
            4L, // unencryptedBlocks
            2L, // offChainBlocks
            categoryBreakdown
        );

        assertNotNull(analysis, "EncryptionAnalysis should be created");
        assertEquals(
            10L,
            analysis.getTotalBlocks(),
            "Total blocks should match"
        );
        assertEquals(
            6L,
            analysis.getEncryptedBlocks(),
            "Encrypted blocks should match"
        );
        assertEquals(
            4L,
            analysis.getUnencryptedBlocks(),
            "Unencrypted blocks should match"
        );
        assertEquals(
            2L,
            analysis.getOffChainBlocks(),
            "Off-chain blocks should match"
        );
        assertEquals(
            0.6,
            analysis.getEncryptionRate(),
            0.01,
            "Encryption rate should be 0.6"
        );
        assertNotNull(
            analysis.getAnalysisTime(),
            "Analysis time should be set"
        );
        assertEquals(
            2,
            analysis.getCategoryBreakdown().size(),
            "Should have 2 categories"
        );

        logger.info("Valid constructor test passed");
    }

    @Test
    @Order(2)
    @DisplayName("Test EncryptionAnalysis constructor with negative values")
    void testEncryptionAnalysisConstructorNegative() {
        Map<String, Long> categoryBreakdown = new HashMap<>();
        categoryBreakdown.put("test", 1L);

        EncryptionAnalysis analysis = new EncryptionAnalysis(
            -5L, // negative totalBlocks
            -3L, // negative encryptedBlocks
            -2L, // negative unencryptedBlocks
            -1L, // negative offChainBlocks
            categoryBreakdown
        );

        assertNotNull(analysis, "EncryptionAnalysis should be created");
        assertEquals(
            0L,
            analysis.getTotalBlocks(),
            "Negative total should become 0"
        );
        assertEquals(
            0L,
            analysis.getEncryptedBlocks(),
            "Negative encrypted should become 0"
        );
        assertEquals(
            0L,
            analysis.getUnencryptedBlocks(),
            "Negative unencrypted should become 0"
        );
        assertEquals(
            0L,
            analysis.getOffChainBlocks(),
            "Negative off-chain should become 0"
        );
        assertEquals(
            0.0,
            analysis.getEncryptionRate(),
            0.01,
            "Encryption rate should be 0.0 when total is 0"
        );

        logger.info("Negative values constructor test passed");
    }

    @Test
    @Order(3)
    @DisplayName(
        "Test EncryptionAnalysis constructor with null categoryBreakdown"
    )
    void testEncryptionAnalysisConstructorNullCategory() {
        EncryptionAnalysis analysis = new EncryptionAnalysis(
            5L, // totalBlocks
            3L, // encryptedBlocks
            2L, // unencryptedBlocks
            1L, // offChainBlocks
            null // null categoryBreakdown
        );

        assertNotNull(analysis, "EncryptionAnalysis should be created");
        assertNotNull(
            analysis.getCategoryBreakdown(),
            "Category breakdown should not be null"
        );
        assertTrue(
            analysis.getCategoryBreakdown().isEmpty(),
            "Category breakdown should be empty when null passed"
        );

        logger.info("Null category breakdown test passed");
    }

    @Test
    @Order(4)
    @DisplayName("Test EncryptionAnalysis with zero total blocks")
    void testEncryptionAnalysisZeroTotal() {
        EncryptionAnalysis analysis = new EncryptionAnalysis(
            0L, // zero totalBlocks
            0L, // encryptedBlocks
            0L, // unencryptedBlocks
            0L, // offChainBlocks
            new HashMap<>()
        );

        assertNotNull(analysis, "EncryptionAnalysis should be created");
        assertEquals(0L, analysis.getTotalBlocks(), "Total blocks should be 0");
        assertEquals(
            0.0,
            analysis.getEncryptionRate(),
            0.01,
            "Encryption rate should be 0.0 when no blocks"
        );

        logger.info("Zero total blocks test passed");
    }

    @Test
    @Order(5)
    @DisplayName("Test EncryptionAnalysis getSummary")
    void testEncryptionAnalysisGetSummary() {
        Map<String, Long> categoryBreakdown = new HashMap<>();
        categoryBreakdown.put("documents", 3L);
        categoryBreakdown.put("media", 2L);

        EncryptionAnalysis analysis = new EncryptionAnalysis(
            10L,
            7L,
            3L,
            2L,
            categoryBreakdown
        );

        String summary = analysis.getSummary();
        assertNotNull(summary, "Summary should not be null");
        assertFalse(summary.isEmpty(), "Summary should not be empty");

        // Verify content
        assertTrue(summary.contains("10"), "Should contain total blocks");
        assertTrue(summary.contains("7"), "Should contain encrypted blocks");
        assertTrue(summary.contains("70.0%"), "Should contain encryption rate");
        assertTrue(summary.contains("3"), "Should contain unencrypted blocks");
        assertTrue(summary.contains("2"), "Should contain off-chain blocks");
        assertTrue(summary.contains("documents"), "Should contain category");
        assertTrue(summary.contains("media"), "Should contain category");

        logger.info("getSummary test passed");
    }

    @Test
    @Order(6)
    @DisplayName(
        "Test EncryptionAnalysis getSummary with null values in breakdown"
    )
    void testEncryptionAnalysisGetSummaryNullValues() {
        Map<String, Long> categoryBreakdown = new HashMap<>();
        categoryBreakdown.put(null, 5L); // null key
        categoryBreakdown.put("valid", null); // null value
        categoryBreakdown.put("normal", 3L); // valid entry

        EncryptionAnalysis analysis = new EncryptionAnalysis(
            8L,
            5L,
            3L,
            1L,
            categoryBreakdown
        );

        String summary = analysis.getSummary();
        assertNotNull(summary, "Summary should not be null");

        // Should handle null keys and values gracefully
        assertTrue(
            summary.contains("Unknown"),
            "Should show 'Unknown' for null category"
        );
        assertTrue(summary.contains("0"), "Should show '0' for null count");
        assertTrue(summary.contains("normal"), "Should show valid categories");

        logger.info("getSummary with null values test passed");
    }

    @Test
    @Order(7)
    @DisplayName("Test EncryptionAnalysis getEncryptionRate edge cases")
    void testEncryptionAnalysisGetEncryptionRateEdgeCases() {
        // Test when calculation might result in NaN
        EncryptionAnalysis analysis1 = new EncryptionAnalysis(
            0L,
            0L,
            0L,
            0L,
            new HashMap<>()
        );
        double rate1 = analysis1.getEncryptionRate();
        assertFalse(Double.isNaN(rate1), "Encryption rate should not be NaN");
        assertEquals(0.0, rate1, 0.01, "Encryption rate should be 0.0");

        // Test with all encrypted
        EncryptionAnalysis analysis2 = new EncryptionAnalysis(
            5L,
            5L,
            0L,
            0L,
            new HashMap<>()
        );
        double rate2 = analysis2.getEncryptionRate();
        assertEquals(
            1.0,
            rate2,
            0.01,
            "Encryption rate should be 1.0 when all encrypted"
        );

        // Test with none encrypted
        EncryptionAnalysis analysis3 = new EncryptionAnalysis(
            5L,
            0L,
            5L,
            0L,
            new HashMap<>()
        );
        double rate3 = analysis3.getEncryptionRate();
        assertEquals(
            0.0,
            rate3,
            0.01,
            "Encryption rate should be 0.0 when none encrypted"
        );

        logger.info("getEncryptionRate edge cases test passed");
    }

    @Test
    @Order(8)
    @DisplayName("Test EncryptionAnalysis getCategoryBreakdown immutability")
    void testEncryptionAnalysisCategoryBreakdownImmutability() {
        Map<String, Long> originalCategories = new HashMap<>();
        originalCategories.put("test", 1L);

        EncryptionAnalysis analysis = new EncryptionAnalysis(
            5L,
            3L,
            2L,
            1L,
            originalCategories
        );

        // Get category breakdown and try to modify it
        Map<String, Long> retrieved = analysis.getCategoryBreakdown();
        retrieved.put("malicious", 999L);

        // Original analysis should not be affected
        Map<String, Long> retrievedAgain = analysis.getCategoryBreakdown();
        assertFalse(
            retrievedAgain.containsKey("malicious"),
            "External modification should not affect internal state"
        );
        assertEquals(
            1,
            retrievedAgain.size(),
            "Should still have only original category"
        );

        logger.info("Category breakdown immutability test passed");
    }

    @Test
    @Order(9)
    @DisplayName("Test analyzeEncryption handles blockchain errors gracefully")
    void testAnalyzeEncryptionBlockchainErrors() {
        // Test that the method handles blockchain errors gracefully
        // We can't test with null blockchain since constructor prevents it,
        // but we can test the error handling logic by testing with empty blockchain
        // and verify the method doesn't crash with various blockchain states

        EncryptionAnalysis analysis = api.analyzeEncryption();
        assertNotNull(
            analysis,
            "Analysis should not be null even with minimal blockchain"
        );

        // The method should handle blockchain errors gracefully
        // and return valid analysis object with zero values when appropriate
        assertTrue(
            analysis.getTotalBlocks() >= 0,
            "Total blocks should be non-negative"
        );
        assertTrue(
            analysis.getEncryptedBlocks() >= 0,
            "Encrypted blocks should be non-negative"
        );
        assertTrue(
            analysis.getUnencryptedBlocks() >= 0,
            "Unencrypted blocks should be non-negative"
        );
        assertTrue(
            analysis.getOffChainBlocks() >= 0,
            "Off-chain blocks should be non-negative"
        );

        logger.info("Blockchain error handling test passed");
    }

    @Test
    @Order(10)
    @DisplayName("Test analyzeEncryption with empty blockchain")
    void testAnalyzeEncryptionEmptyBlockchain() {
        EncryptionAnalysis analysis = api.analyzeEncryption();
        assertNotNull(analysis, "Analysis should not be null");

        // Empty blockchain should have genesis block (index 0) only
        assertTrue(
            analysis.getTotalBlocks() >= 0,
            "Should have non-negative total blocks"
        );
        assertEquals(
            0.0,
            analysis.getEncryptionRate(),
            0.01,
            "Empty blockchain should have 0 encryption rate"
        );

        logger.info(
            "Empty blockchain test passed - total blocks: {}",
            analysis.getTotalBlocks()
        );
    }

    @Test
    @Order(11)
    @DisplayName("Test analyzeEncryption with mixed block types")
    void testAnalyzeEncryptionMixedBlocks() throws Exception {
        // Create different types of blocks
        Block publicBlock = createPublicBlock("Public content");
        Block encryptedBlock = createEncryptedBlock(
            "Encrypted content",
            testPassword
        );

        assertNotNull(publicBlock, "Public block should be created");
        assertNotNull(encryptedBlock, "Encrypted block should be created");

        EncryptionAnalysis analysis = api.analyzeEncryption();
        assertNotNull(analysis, "Analysis should not be null");

        assertTrue(
            analysis.getTotalBlocks() >= 2,
            "Should have at least 2 blocks"
        );
        assertTrue(
            analysis.getEncryptedBlocks() >= 1,
            "Should have at least 1 encrypted block"
        );
        assertTrue(
            analysis.getUnencryptedBlocks() >= 1,
            "Should have at least 1 unencrypted block"
        );

        // Encryption rate should be reasonable
        double rate = analysis.getEncryptionRate();
        assertTrue(
            rate >= 0.0 && rate <= 1.0,
            "Encryption rate should be between 0 and 1"
        );

        logger.info(
            "Mixed blocks test passed - encryption rate: {}",
            String.format("%.2f", rate)
        );
    }

    @Test
    @Order(12)
    @DisplayName("Test analyzeEncryption performance with multiple blocks")
    void testAnalyzeEncryptionPerformance() throws Exception {
        // Create multiple blocks for performance testing
        int numBlocks = 20;
        for (int i = 0; i < numBlocks; i++) {
            if (i % 3 == 0) {
                createPublicBlock("Public content " + i);
            } else {
                createEncryptedBlock("Encrypted content " + i, testPassword);
            }
        }

        long startTime = System.currentTimeMillis();
        EncryptionAnalysis analysis = api.analyzeEncryption();
        long endTime = System.currentTimeMillis();

        assertNotNull(analysis, "Analysis should not be null");
        long duration = endTime - startTime;

        logger.info(
            "Performance test completed in {} ms for {} blocks",
            duration,
            analysis.getTotalBlocks()
        );
        assertTrue(
            duration < 5000,
            "Analysis should complete in reasonable time (< 5 seconds)"
        );
    }

    @Test
    @Order(13)
    @DisplayName("Test analyzeEncryption consistency")
    void testAnalyzeEncryptionConsistency() throws Exception {
        // Create test blocks
        createPublicBlock("Consistency test public");
        createEncryptedBlock("Consistency test encrypted", testPassword);

        // Run analysis multiple times
        EncryptionAnalysis analysis1 = api.analyzeEncryption();
        EncryptionAnalysis analysis2 = api.analyzeEncryption();

        // Results should be consistent
        assertEquals(
            analysis1.getTotalBlocks(),
            analysis2.getTotalBlocks(),
            "Total blocks should be consistent"
        );
        assertEquals(
            analysis1.getEncryptedBlocks(),
            analysis2.getEncryptedBlocks(),
            "Encrypted blocks should be consistent"
        );
        assertEquals(
            analysis1.getUnencryptedBlocks(),
            analysis2.getUnencryptedBlocks(),
            "Unencrypted blocks should be consistent"
        );
        assertEquals(
            analysis1.getEncryptionRate(),
            analysis2.getEncryptionRate(),
            0.01,
            "Encryption rate should be consistent"
        );

        logger.info("Consistency test passed");
    }

    @Test
    @Order(14)
    @DisplayName("Test EncryptionAnalysis with large numbers")
    void testEncryptionAnalysisLargeNumbers() {
        EncryptionAnalysis analysis = new EncryptionAnalysis(
            Long.MAX_VALUE, // Very large total
            Long.MAX_VALUE / 2, // Large encrypted count
            Long.MAX_VALUE / 2, // Large unencrypted count
            1000L, // Regular off-chain count
            new HashMap<>()
        );

        assertNotNull(analysis, "Should handle large numbers");
        assertEquals(Long.MAX_VALUE, analysis.getTotalBlocks());
        assertTrue(
            analysis.getEncryptionRate() > 0 &&
            analysis.getEncryptionRate() < 1,
            "Should calculate reasonable encryption rate for large numbers"
        );

        logger.info(
            "Large numbers test passed - rate: {}",
            analysis.getEncryptionRate()
        );
    }

    @Test
    @Order(15)
    @DisplayName("Test EncryptionAnalysis analysisTime accuracy")
    void testEncryptionAnalysisTimeAccuracy() {
        LocalDateTime beforeCreation = LocalDateTime.now();

        EncryptionAnalysis analysis = new EncryptionAnalysis(
            5L,
            3L,
            2L,
            1L,
            new HashMap<>()
        );

        LocalDateTime afterCreation = LocalDateTime.now();
        LocalDateTime analysisTime = analysis.getAnalysisTime();

        assertNotNull(analysisTime, "Analysis time should not be null");
        assertTrue(
            analysisTime.isAfter(beforeCreation) ||
            analysisTime.isEqual(beforeCreation),
            "Analysis time should be after or equal to before creation time"
        );
        assertTrue(
            analysisTime.isBefore(afterCreation) ||
            analysisTime.isEqual(afterCreation),
            "Analysis time should be before or equal to after creation time"
        );

        logger.info(
            "Analysis time accuracy test passed - time: {}",
            analysisTime
        );
    }
}
