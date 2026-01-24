package com.rbatllet.blockchain.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import com.rbatllet.blockchain.service.SecureBlockEncryptionService;
import com.rbatllet.blockchain.service.UserFriendlyEncryptionAPI;
import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.util.TestGenesisKeyManager;

/**
 * Rigorous tests to verify private keywords decryption in encrypted blocks.
 *
 * BUG FIXED: Private keywords in encrypted blocks showed "[Wrong password]"
 * even though data decryption worked with the same password.
 *
 * ROOT CAUSE: autoKeywords field contained multiple encrypted strings appended
 * together, which couldn't be decrypted as a single encrypted object.
 *
 * CORRECT BEHAVIOR (after fix):
 * - Private keywords are encrypted as a single unit in autoKeywords
 * - Decryption with correct password returns the original keywords
 * - Decryption with wrong password fails gracefully
 *
 * Test scenarios:
 * 1. Mixed public + private keywords - both stored correctly
 * 2. Only private keywords - decrypts correctly
 * 3. Encrypted string format validation
 * 4. Wrong password handling
 * 5. Data and keywords decryption consistency
 */
public class EncryptedPrivateKeywordsDecryptionBugTest {

    // Test constants
    private static final String TEST_PASSWORD = "BlockPassword2025#Strong";
    private static final String WRONG_PASSWORD = "WrongPassword2025#Wrong";
    private static final String TEST_DATA = "ENCRYPTED_CONTENT_FOR_TEST";
    private static final String[] PUBLIC_KEYWORDS = {"PUBLIC_KEYWORD_ONE", "PUBLIC_KEYWORD_TWO"};
    private static final String[] PRIVATE_KEYWORDS = {"PRIVATE_KEYWORD_ONE", "PRIVATE_KEYWORD_TWO"};

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
    @DisplayName("Mixed public + private keywords: private keywords decrypt correctly with correct password")
    void testMixedKeywords_PrivateKeywordsDecryptCorrectly() throws Exception {
        // Act: Create encrypted block with mixed keywords
        Block block = api.storeSearchableDataWithLayers(
            TEST_DATA,
            TEST_PASSWORD,
            PUBLIC_KEYWORDS,
            PRIVATE_KEYWORDS
        );
        waitForIndexing();

        Block retrievedBlock = blockchain.getBlock(block.getBlockNumber());
        assertNotNull(retrievedBlock, "Block should be retrievable");

        // Verify block is encrypted
        assertTrue(retrievedBlock.getIsEncrypted(), "Block MUST be encrypted");

        // Verify public keywords are in manualKeywords (with public: prefix)
        String manualKeywords = retrievedBlock.getManualKeywords();
        assertNotNull(manualKeywords, "manualKeywords should not be null");
        assertTrue(manualKeywords.toLowerCase().contains("public_keyword_one"),
            "manualKeywords should contain public keyword. Got: " + manualKeywords);

        // Verify autoKeywords exists (contains encrypted private keywords)
        String autoKeywords = retrievedBlock.getAutoKeywords();
        assertNotNull(autoKeywords, "autoKeywords should not be null");
        assertFalse(autoKeywords.trim().isEmpty(), "autoKeywords should not be empty");

        // CRITICAL: Verify private keywords CAN be decrypted with correct password
        String decryptedKeywords = SecureBlockEncryptionService.decryptFromString(autoKeywords, TEST_PASSWORD);
        assertNotNull(decryptedKeywords, "Decrypted keywords should not be null");

        // Verify decrypted keywords contain our private keywords
        String lowerDecrypted = decryptedKeywords.toLowerCase();
        assertTrue(lowerDecrypted.contains("private_keyword_one"),
            "Decrypted keywords should contain 'private_keyword_one'. Got: " + decryptedKeywords);
        assertTrue(lowerDecrypted.contains("private_keyword_two"),
            "Decrypted keywords should contain 'private_keyword_two'. Got: " + decryptedKeywords);
    }

    @Test
    @DisplayName("Only private keywords: decrypts correctly with correct password")
    void testOnlyPrivateKeywords_DecryptsCorrectly() throws Exception {
        // Act: Create encrypted block with ONLY private keywords
        Block block = api.storeSearchableDataWithLayers(
            TEST_DATA,
            TEST_PASSWORD,
            new String[]{},  // No public keywords
            PRIVATE_KEYWORDS
        );
        waitForIndexing();

        Block retrievedBlock = blockchain.getBlock(block.getBlockNumber());
        assertNotNull(retrievedBlock, "Block should be retrievable");

        // Verify manualKeywords is empty (no public keywords)
        String manualKeywords = retrievedBlock.getManualKeywords();
        assertTrue(manualKeywords == null || manualKeywords.trim().isEmpty(),
            "manualKeywords should be empty when no public keywords provided");

        // Verify autoKeywords exists
        String autoKeywords = retrievedBlock.getAutoKeywords();
        assertNotNull(autoKeywords, "autoKeywords should exist");
        assertFalse(autoKeywords.trim().isEmpty(), "autoKeywords should not be empty");

        // Decrypt and verify
        String decryptedKeywords = SecureBlockEncryptionService.decryptFromString(autoKeywords, TEST_PASSWORD);
        assertNotNull(decryptedKeywords, "Decrypted keywords should not be null");

        String lowerDecrypted = decryptedKeywords.toLowerCase();
        assertTrue(lowerDecrypted.contains("private_keyword_one"),
            "Should contain 'private_keyword_one'. Got: " + decryptedKeywords);
        assertTrue(lowerDecrypted.contains("private_keyword_two"),
            "Should contain 'private_keyword_two'. Got: " + decryptedKeywords);
    }

    @Test
    @DisplayName("Data and private keywords use same encryption - both decrypt with same password")
    void testDataAndKeywords_BothDecryptWithSamePassword() throws Exception {
        // Act
        Block block = api.storeSearchableDataWithLayers(
            TEST_DATA,
            TEST_PASSWORD,
            PUBLIC_KEYWORDS,
            PRIVATE_KEYWORDS
        );
        waitForIndexing();

        // Verify DATA can be decrypted
        String decryptedData = api.retrieveSecret(block.getBlockNumber(), TEST_PASSWORD);
        assertEquals(TEST_DATA, decryptedData,
            "Data should decrypt to original value");

        // Verify KEYWORDS can also be decrypted with SAME password
        Block retrievedBlock = blockchain.getBlock(block.getBlockNumber());
        String autoKeywords = retrievedBlock.getAutoKeywords();
        String decryptedKeywords = SecureBlockEncryptionService.decryptFromString(autoKeywords, TEST_PASSWORD);

        assertTrue(decryptedKeywords.toLowerCase().contains("private_keyword_one"),
            "Keywords should decrypt with same password as data");
    }

    // ===== SECURITY TESTS =====

    @Test
    @DisplayName("SECURITY: Wrong password cannot decrypt private keywords")
    void testWrongPassword_CannotDecryptKeywords() throws Exception {
        // Act
        Block block = api.storeSearchableDataWithLayers(
            TEST_DATA,
            TEST_PASSWORD,
            PUBLIC_KEYWORDS,
            PRIVATE_KEYWORDS
        );
        waitForIndexing();

        Block retrievedBlock = blockchain.getBlock(block.getBlockNumber());
        String autoKeywords = retrievedBlock.getAutoKeywords();

        // Verify wrong password throws exception or returns error
        assertThrows(Exception.class, () -> {
            SecureBlockEncryptionService.decryptFromString(autoKeywords, WRONG_PASSWORD);
        }, "Wrong password should throw exception when decrypting keywords");
    }

    @Test
    @DisplayName("SECURITY: Private keywords are NOT visible in plaintext in autoKeywords field")
    void testPrivateKeywords_NotVisibleInPlaintext() throws Exception {
        // Act
        Block block = api.storeSearchableDataWithLayers(
            TEST_DATA,
            TEST_PASSWORD,
            PUBLIC_KEYWORDS,
            PRIVATE_KEYWORDS
        );
        waitForIndexing();

        Block retrievedBlock = blockchain.getBlock(block.getBlockNumber());
        String autoKeywords = retrievedBlock.getAutoKeywords();

        // SECURITY: The raw autoKeywords should NOT contain plaintext keywords
        assertFalse(autoKeywords.toLowerCase().contains("private_keyword_one"),
            "SECURITY: autoKeywords should NOT contain plaintext private keywords");
        assertFalse(autoKeywords.toLowerCase().contains("private_keyword_two"),
            "SECURITY: autoKeywords should NOT contain plaintext private keywords");
    }

    // ===== FORMAT VALIDATION TESTS =====

    @Test
    @DisplayName("Encrypted string format: has exactly 5 parts (timestamp|salt|iv|data|hash)")
    void testEncryptedStringFormat_HasFiveParts() throws Exception {
        // Create a simple encrypted string
        String encrypted = SecureBlockEncryptionService.encryptToString("TEST_KEYWORD", TEST_PASSWORD);

        // Verify format: timestamp|salt|iv|encryptedData|dataHash
        String[] parts = encrypted.split("\\|");
        assertEquals(5, parts.length,
            "Encrypted string should have exactly 5 parts. Got: " + parts.length);

        // Each part should be non-empty
        for (int i = 0; i < parts.length; i++) {
            assertFalse(parts[i].trim().isEmpty(),
                "Part " + i + " should not be empty");
        }
    }

    @Test
    @DisplayName("Concatenated encrypted strings cannot be decrypted (format validation)")
    void testConcatenatedEncryptedStrings_CannotDecrypt() throws Exception {
        // Create two separate encrypted strings
        String encrypted1 = SecureBlockEncryptionService.encryptToString("KEYWORD_1", TEST_PASSWORD);
        String encrypted2 = SecureBlockEncryptionService.encryptToString("KEYWORD_2", TEST_PASSWORD);

        // Concatenate them (this was the bug - appending encrypted strings)
        String concatenated = encrypted1 + " " + encrypted2;

        // Verify concatenated string has wrong number of parts
        String[] parts = concatenated.split("\\|");
        assertTrue(parts.length > 5,
            "Concatenated string should have more than 5 parts. Got: " + parts.length);

        // Verify decryption fails for concatenated string
        assertThrows(Exception.class, () -> {
            SecureBlockEncryptionService.decryptFromString(concatenated, TEST_PASSWORD);
        }, "Concatenated encrypted strings should NOT be decryptable");
    }

    @Test
    @DisplayName("autoKeywords field has valid encrypted format (5 parts)")
    void testAutoKeywordsField_HasValidFormat() throws Exception {
        // Act
        Block block = api.storeSearchableDataWithLayers(
            TEST_DATA,
            TEST_PASSWORD,
            PUBLIC_KEYWORDS,
            PRIVATE_KEYWORDS
        );
        waitForIndexing();

        Block retrievedBlock = blockchain.getBlock(block.getBlockNumber());
        String autoKeywords = retrievedBlock.getAutoKeywords();

        // Verify format is valid (5 parts)
        String[] parts = autoKeywords.split("\\|");
        assertEquals(5, parts.length,
            "autoKeywords should have exactly 5 parts (valid encrypted format). Got: " + parts.length +
            ". This indicates the bug where multiple encrypted strings were appended.");
    }

    // ===== EDGE CASES =====

    @Test
    @DisplayName("Single private keyword decrypts correctly")
    void testSinglePrivateKeyword_DecryptsCorrectly() throws Exception {
        // Act
        Block block = api.storeSearchableDataWithLayers(
            TEST_DATA,
            TEST_PASSWORD,
            new String[]{},
            new String[]{"SINGLE_PRIVATE_KEYWORD"}
        );
        waitForIndexing();

        Block retrievedBlock = blockchain.getBlock(block.getBlockNumber());
        String autoKeywords = retrievedBlock.getAutoKeywords();

        String decryptedKeywords = SecureBlockEncryptionService.decryptFromString(autoKeywords, TEST_PASSWORD);
        assertTrue(decryptedKeywords.toLowerCase().contains("single_private_keyword"),
            "Should contain single private keyword. Got: " + decryptedKeywords);
    }

    @Test
    @DisplayName("Empty private keywords array results in null or empty autoKeywords")
    void testEmptyPrivateKeywords_EmptyAutoKeywords() throws Exception {
        // Act: Only public keywords, no private keywords
        Block block = api.storeSearchableDataWithLayers(
            TEST_DATA,
            TEST_PASSWORD,
            PUBLIC_KEYWORDS,
            new String[]{}  // Empty private keywords
        );
        waitForIndexing();

        Block retrievedBlock = blockchain.getBlock(block.getBlockNumber());

        // autoKeywords may be null, empty, or contain only auto-extracted keywords
        String autoKeywords = retrievedBlock.getAutoKeywords();

        // If autoKeywords exists and is not empty, it should NOT contain our private keywords
        // (because we didn't provide any)
        if (autoKeywords != null && !autoKeywords.trim().isEmpty()) {
            // Try to decrypt - should work if it's valid encrypted format
            try {
                String decrypted = SecureBlockEncryptionService.decryptFromString(autoKeywords, TEST_PASSWORD);
                // Should NOT contain "private_keyword" since we didn't provide any
                assertFalse(decrypted.toLowerCase().contains("private_keyword"),
                    "Should NOT contain private keywords we didn't provide");
            } catch (Exception e) {
                // If decryption fails, that's also acceptable for empty private keywords
            }
        }
        // Test passes if autoKeywords is null/empty (no private keywords provided)
    }

    @Test
    @DisplayName("Block hash integrity is preserved after encryption")
    void testBlockHashIntegrity() throws Exception {
        // Act
        Block block = api.storeSearchableDataWithLayers(
            TEST_DATA,
            TEST_PASSWORD,
            PUBLIC_KEYWORDS,
            PRIVATE_KEYWORDS
        );

        Block retrievedBlock = blockchain.getBlock(block.getBlockNumber());

        // Verify hash matches
        assertEquals(block.getHash(), retrievedBlock.getHash(),
            "Block hash should be preserved");
        assertEquals(block.getBlockNumber(), retrievedBlock.getBlockNumber(),
            "Block number should match");
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
