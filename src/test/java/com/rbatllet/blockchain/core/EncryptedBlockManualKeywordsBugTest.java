package com.rbatllet.blockchain.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.KeyPair;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.indexing.IndexingCoordinator;
import com.rbatllet.blockchain.search.SearchFrameworkEngine;
import com.rbatllet.blockchain.security.UserRole;
import com.rbatllet.blockchain.service.UserFriendlyEncryptionAPI;
import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.util.TestGenesisKeyManager;

/**
 * Rigorous tests to verify manual keywords storage in encrypted blocks.
 *
 * BUG FIXED: Manual keywords in encrypted blocks were stored in autoKeywords
 * field instead of manualKeywords field.
 *
 * CORRECT BEHAVIOR (after fix):
 * - manualKeywords field: Contains public keywords WITH "public:" prefix (for encrypted blocks)
 *                         or without prefix (for non-encrypted blocks)
 * - autoKeywords field: Contains encrypted private keywords + auto-extracted terms
 *
 * Test scenarios:
 * 1. Encrypted block with manual keywords - verify storage location
 * 2. Verify public: prefix is present for encrypted blocks
 * 3. Verify encrypted block is actually encrypted
 * 4. Compare with non-encrypted block behavior
 * 5. Edge cases: empty keywords, special characters, single keyword
 */
public class EncryptedBlockManualKeywordsBugTest {

    // Test constants
    private static final String TEST_PASSWORD = "TestPassword2025#Strong";
    private static final String TEST_DATA = "Test content with manual keywords";
    private static final String[] MANUAL_KEYWORDS = {"KEYWORD_ONE", "KEYWORD_TWO", "KEYWORD_THREE"};

    private Blockchain blockchain;
    private KeyPair bootstrapKeyPair;
    private KeyPair userKeyPair;
    private UserFriendlyEncryptionAPI api;

    @BeforeEach
    void setUp() throws Exception {
        // Reset global state
        SearchFrameworkEngine.resetGlobalState();
        IndexingCoordinator.getInstance().reset();

        blockchain = new Blockchain();
        blockchain.clearAndReinitialize();

        bootstrapKeyPair = TestGenesisKeyManager.ensureGenesisKeysExist();
        blockchain.createBootstrapAdmin(
            CryptoUtil.publicKeyToString(bootstrapKeyPair.getPublic()),
            "BOOTSTRAP_ADMIN"
        );

        api = new UserFriendlyEncryptionAPI(blockchain);

        // Create admin user
        KeyPair adminKeys = CryptoUtil.generateKeyPair();
        String adminPublicKey = CryptoUtil.publicKeyToString(adminKeys.getPublic());
        blockchain.addAuthorizedKey(adminPublicKey, "Admin", bootstrapKeyPair, UserRole.ADMIN);
        api.setDefaultCredentials("Admin", adminKeys);

        // Create test user and store keypair
        userKeyPair = api.createUser("test-user");
        api.setDefaultCredentials("test-user", userKeyPair);
    }

    @AfterEach
    void tearDown() {
        try {
            IndexingCoordinator.getInstance().waitForCompletion();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (blockchain != null) {
            blockchain.clearAndReinitialize();
            blockchain.shutdown();
        }

        SearchFrameworkEngine.resetGlobalState();
        IndexingCoordinator.getInstance().reset();
    }

    // ===== CORE BUG FIX TESTS =====

    @Test
    @DisplayName("Encrypted block stores manual keywords in manualKeywords field (not autoKeywords)")
    void testEncryptedBlock_ManualKeywordsInCorrectField() throws Exception {
        // Act: Create encrypted block with manual keywords
        Block block = api.storeSearchableData(TEST_DATA, TEST_PASSWORD, MANUAL_KEYWORDS);
        waitForIndexing();

        // Retrieve block from database
        Block retrievedBlock = blockchain.getBlock(block.getBlockNumber());
        assertNotNull(retrievedBlock, "Block should be retrievable from database");

        // CRITICAL: Verify block is actually encrypted
        assertTrue(retrievedBlock.isDataEncrypted(),
            "Block MUST be encrypted when using storeSearchableData with password");

        // Verify manualKeywords field is populated
        String manualKeywordsField = retrievedBlock.getManualKeywords();
        assertNotNull(manualKeywordsField, "manualKeywords field should not be null");
        assertFalse(manualKeywordsField.trim().isEmpty(),
            "manualKeywords field should NOT be empty - this was the original bug");

        // Verify all manual keywords are present (case-insensitive)
        String lowerKeywords = manualKeywordsField.toLowerCase();
        assertTrue(lowerKeywords.contains("keyword_one"),
            "manualKeywords should contain 'keyword_one'. Got: " + manualKeywordsField);
        assertTrue(lowerKeywords.contains("keyword_two"),
            "manualKeywords should contain 'keyword_two'. Got: " + manualKeywordsField);
        assertTrue(lowerKeywords.contains("keyword_three"),
            "manualKeywords should contain 'keyword_three'. Got: " + manualKeywordsField);
    }

    @Test
    @DisplayName("Manual keywords in encrypted blocks are stored WITH 'public:' prefix")
    void testEncryptedBlock_PublicPrefixPresent() throws Exception {
        // Act
        Block block = api.storeSearchableData(TEST_DATA, TEST_PASSWORD, MANUAL_KEYWORDS);
        waitForIndexing();

        Block retrievedBlock = blockchain.getBlock(block.getBlockNumber());
        assertNotNull(retrievedBlock, "Block should be retrievable");

        String manualKeywordsField = retrievedBlock.getManualKeywords();
        assertNotNull(manualKeywordsField, "manualKeywords should not be null");

        // For encrypted blocks, manual keywords are stored WITH "public:" prefix
        // This distinguishes them as publicly searchable (not encrypted)
        assertTrue(manualKeywordsField.contains("public:"),
            "manualKeywords field SHOULD contain 'public:' prefix for encrypted blocks. Got: " + manualKeywordsField);

        // Keywords should be stored in lowercase with public: prefix
        assertTrue(manualKeywordsField.toLowerCase().contains("public:keyword_one"),
            "Keywords should be stored with 'public:' prefix. Got: " + manualKeywordsField);
    }

    @Test
    @DisplayName("autoKeywords field contains encrypted content (not manual keywords)")
    void testEncryptedBlock_AutoKeywordsContainsEncryptedContent() throws Exception {
        // Act
        Block block = api.storeSearchableData(TEST_DATA, TEST_PASSWORD, MANUAL_KEYWORDS);
        waitForIndexing();

        Block retrievedBlock = blockchain.getBlock(block.getBlockNumber());
        assertNotNull(retrievedBlock, "Block should be retrievable");

        String autoKeywordsField = retrievedBlock.getAutoKeywords();

        // autoKeywords may be null or empty if no auto-extraction was done
        // But if it exists, it should be encrypted (not plaintext)
        if (autoKeywordsField != null && !autoKeywordsField.trim().isEmpty()) {
            // Auto-extracted keywords are encrypted, so they should NOT be readable
            // They should NOT contain our manual keywords in plaintext
            assertFalse(autoKeywordsField.toLowerCase().contains("keyword_one"),
                "autoKeywords should NOT contain plaintext manual keywords. Got: " + autoKeywordsField);
            assertFalse(autoKeywordsField.toLowerCase().contains("keyword_two"),
                "autoKeywords should NOT contain plaintext manual keywords");
        }
        // If autoKeywords is null/empty, that's also acceptable
    }

    @Test
    @DisplayName("Retrieved block hash matches original block hash")
    void testEncryptedBlock_HashIntegrity() throws Exception {
        // Act
        Block block = api.storeSearchableData(TEST_DATA, TEST_PASSWORD, MANUAL_KEYWORDS);
        waitForIndexing();

        Block retrievedBlock = blockchain.getBlock(block.getBlockNumber());
        assertNotNull(retrievedBlock, "Block should be retrievable");

        // Verify hash integrity
        assertEquals(block.getHash(), retrievedBlock.getHash(),
            "Retrieved block hash should match original block hash");
        assertEquals(block.getBlockNumber(), retrievedBlock.getBlockNumber(),
            "Block numbers should match");
    }

    // ===== COMPARISON WITH NON-ENCRYPTED BLOCKS =====

    @Test
    @DisplayName("Non-encrypted block with keywords stores them in manualKeywords")
    void testNonEncryptedBlock_KeywordsInManualKeywordsField() throws Exception {
        // Act: Create non-encrypted block with keywords
        Block block = blockchain.addBlockWithKeywords(
            "Non-encrypted test data",
            MANUAL_KEYWORDS,
            "TEST_CATEGORY",
            userKeyPair.getPrivate(),
            userKeyPair.getPublic()
        );
        waitForIndexing();

        Block retrievedBlock = blockchain.getBlock(block.getBlockNumber());
        assertNotNull(retrievedBlock, "Block should be retrievable");

        // Verify block is NOT encrypted
        assertFalse(retrievedBlock.isDataEncrypted(),
            "Block should NOT be encrypted when using addBlockWithKeywords without password");

        // Verify manualKeywords field is populated
        String manualKeywordsField = retrievedBlock.getManualKeywords();
        assertNotNull(manualKeywordsField, "manualKeywords should not be null for non-encrypted block");
        assertFalse(manualKeywordsField.trim().isEmpty(),
            "manualKeywords should not be empty for non-encrypted block with keywords");

        // Verify keywords are present
        String lowerKeywords = manualKeywordsField.toLowerCase();
        assertTrue(lowerKeywords.contains("keyword_one"),
            "manualKeywords should contain 'keyword_one'. Got: " + manualKeywordsField);
    }

    @Test
    @DisplayName("Non-encrypted block without keywords has empty manualKeywords")
    void testNonEncryptedBlock_NoKeywords_EmptyManualKeywords() throws Exception {
        // Act: Create non-encrypted block WITHOUT keywords
        Block block = blockchain.addBlockWithKeywords(
            "Non-encrypted test data without keywords",
            null,  // No manual keywords
            "TEST_CATEGORY",
            userKeyPair.getPrivate(),
            userKeyPair.getPublic()
        );
        waitForIndexing();

        Block retrievedBlock = blockchain.getBlock(block.getBlockNumber());
        assertNotNull(retrievedBlock, "Block should be retrievable");

        // Verify block is NOT encrypted
        assertFalse(retrievedBlock.isDataEncrypted(),
            "Block should NOT be encrypted");

        // Verify manualKeywords field is null or empty (no keywords provided)
        String manualKeywordsField = retrievedBlock.getManualKeywords();
        assertTrue(manualKeywordsField == null || manualKeywordsField.trim().isEmpty(),
            "manualKeywords should be null or empty when no keywords provided. Got: " + manualKeywordsField);
    }

    // ===== EDGE CASES =====

    @Test
    @DisplayName("Encrypted block with empty keywords array has empty manualKeywords")
    void testEncryptedBlock_EmptyKeywordsArray() throws Exception {
        // Act: Create encrypted block with empty keywords array
        Block block = api.storeSearchableData(
            TEST_DATA,
            TEST_PASSWORD,
            new String[]{}  // Empty array
        );
        waitForIndexing();

        Block retrievedBlock = blockchain.getBlock(block.getBlockNumber());
        assertNotNull(retrievedBlock, "Block should be retrievable");

        // Block should still be encrypted
        assertTrue(retrievedBlock.isDataEncrypted(),
            "Block should be encrypted even with empty keywords");

        // manualKeywords should be null or empty
        String manualKeywordsField = retrievedBlock.getManualKeywords();
        assertTrue(manualKeywordsField == null || manualKeywordsField.trim().isEmpty(),
            "manualKeywords should be null or empty when empty keywords array provided. Got: " + manualKeywordsField);
    }

    @Test
    @DisplayName("Encrypted block with single keyword stores it correctly")
    void testEncryptedBlock_SingleKeyword() throws Exception {
        // Act
        Block block = api.storeSearchableData(
            TEST_DATA,
            TEST_PASSWORD,
            new String[]{"SINGLE_KEYWORD"}
        );
        waitForIndexing();

        Block retrievedBlock = blockchain.getBlock(block.getBlockNumber());
        assertNotNull(retrievedBlock, "Block should be retrievable");

        String manualKeywordsField = retrievedBlock.getManualKeywords();
        assertNotNull(manualKeywordsField, "manualKeywords should not be null");
        assertFalse(manualKeywordsField.trim().isEmpty(),
            "manualKeywords should not be empty");
        assertTrue(manualKeywordsField.toLowerCase().contains("single_keyword"),
            "manualKeywords should contain 'single_keyword'. Got: " + manualKeywordsField);
    }

    @Test
    @DisplayName("Keywords with special characters are stored correctly")
    void testEncryptedBlock_SpecialCharactersInKeywords() throws Exception {
        // Keywords with various characters (alphanumeric should work)
        String[] specialKeywords = {"KEYWORD_WITH_UNDERSCORE", "KEYWORD123", "TEST2025"};

        Block block = api.storeSearchableData(
            TEST_DATA,
            TEST_PASSWORD,
            specialKeywords
        );
        waitForIndexing();

        Block retrievedBlock = blockchain.getBlock(block.getBlockNumber());
        assertNotNull(retrievedBlock, "Block should be retrievable");

        String manualKeywordsField = retrievedBlock.getManualKeywords();
        assertNotNull(manualKeywordsField, "manualKeywords should not be null");

        // Verify keywords are stored
        String lowerKeywords = manualKeywordsField.toLowerCase();
        assertTrue(lowerKeywords.contains("keyword_with_underscore"),
            "Should contain 'keyword_with_underscore'. Got: " + manualKeywordsField);
        assertTrue(lowerKeywords.contains("keyword123"),
            "Should contain 'keyword123'. Got: " + manualKeywordsField);
        assertTrue(lowerKeywords.contains("test2025"),
            "Should contain 'test2025'. Got: " + manualKeywordsField);
    }

    @Test
    @DisplayName("Encrypted block data is not readable without decryption")
    void testEncryptedBlock_DataNotReadableWithoutDecryption() throws Exception {
        // Act
        Block block = api.storeSearchableData(TEST_DATA, TEST_PASSWORD, MANUAL_KEYWORDS);
        waitForIndexing();

        Block retrievedBlock = blockchain.getBlock(block.getBlockNumber());
        assertNotNull(retrievedBlock, "Block should be retrievable");

        // Verify encryption
        assertTrue(retrievedBlock.isDataEncrypted(), "Block should be encrypted");

        // The raw data field should NOT contain the original plaintext
        String rawData = retrievedBlock.getData();
        assertFalse(rawData.equals(TEST_DATA),
            "Encrypted block data should NOT be the original plaintext");
    }

    @Test
    @DisplayName("Block number is positive for encrypted blocks")
    void testEncryptedBlock_ValidBlockNumber() throws Exception {
        Block block = api.storeSearchableData(TEST_DATA, TEST_PASSWORD, MANUAL_KEYWORDS);

        assertTrue(block.getBlockNumber() > 0,
            "Block number should be positive. Got: " + block.getBlockNumber());
    }

    // ===== HELPER METHODS =====

    private void waitForIndexing() {
        try {
            IndexingCoordinator.getInstance().waitForCompletion(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
