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
import com.rbatllet.blockchain.search.SearchSpecialistAPI;
import com.rbatllet.blockchain.security.UserRole;
import com.rbatllet.blockchain.service.UserFriendlyEncryptionAPI;
import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.util.TestGenesisKeyManager;

/**
 * Rigorous tests to verify INCLUDE_DATA level searches encrypted block content.
 *
 * BUG FIXED: INCLUDE_DATA level was NOT searching encrypted block content.
 * Even with correct password, searching for encrypted content returned 0 results.
 *
 * ROOT CAUSE: EncryptedContentSearch only indexed keywords, not block content.
 * The fix added query-time decryption of block content during search.
 *
 * CORRECT BEHAVIOR (after fix):
 * - INCLUDE_DATA with password decrypts and searches encrypted block content
 * - Limited to 500 most recent encrypted blocks (performance constraint)
 * - Batch processing (50 blocks per batch) with early termination
 *
 * Test scenarios:
 * 1. Encrypted content found with correct password
 * 2. Private keywords found with correct password
 * 3. Non-encrypted content found without password
 * 4. SECURITY: Encrypted content NOT found without password
 * 5. SECURITY: Encrypted content NOT found with wrong password
 * 6. Partial content search works
 * 7. Data decryption consistency
 */
public class IncludeDataEncryptedContentSearchBugTest {

    // Test constants
    private static final String TEST_PASSWORD = "BlockPassword123#";
    private static final String WRONG_PASSWORD = "WrongPassword123#";
    private static final String ENCRYPTED_CONTENT = "UniqueEncryptedContent_ABC123";
    private static final String NON_ENCRYPTED_CONTENT = "UniqueNonEncryptedContent_XYZ789";
    private static final String PRIVATE_KEYWORD = "PRIVATE_SEARCH_KEYWORD";

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
    @DisplayName("INCLUDE_DATA finds encrypted content with correct password (BUG FIX)")
    void testIncludeData_FindsEncryptedContent_WithPassword() throws Exception {
        // Arrange: Create encrypted block
        Block block = api.storeSearchableData(ENCRYPTED_CONTENT, TEST_PASSWORD, null);
        waitForIndexing();

        assertNotNull(block, "Block should be created");
        assertTrue(block.isDataEncrypted(), "Block MUST be encrypted");

        // Act: Search for encrypted content with password
        blockchain.initializeAdvancedSearch(TEST_PASSWORD);
        SearchSpecialistAPI searchAPI = new SearchSpecialistAPI(
            blockchain, TEST_PASSWORD, userKeyPair.getPrivate()
        );

        var results = searchAPI.searchIntelligent(ENCRYPTED_CONTENT, TEST_PASSWORD, 10);

        // Assert: Content MUST be found
        assertFalse(results.isEmpty(),
            "BUG: INCLUDE_DATA should find encrypted content with correct password");

        boolean foundCorrectBlock = results.stream()
            .anyMatch(r -> r.getBlockHash().equals(block.getHash()));
        assertTrue(foundCorrectBlock,
            "The correct encrypted block should be found. Expected hash: " + block.getHash().substring(0, 16) + "...");
    }

    @Test
    @DisplayName("INCLUDE_DATA finds private keywords with correct password")
    void testIncludeData_FindsPrivateKeywords_WithPassword() throws Exception {
        // Arrange: Create encrypted block with private keyword
        Block block = api.storeSearchableDataWithLayers(
            ENCRYPTED_CONTENT,
            TEST_PASSWORD,
            null, // no public keywords
            new String[]{PRIVATE_KEYWORD}
        );
        waitForIndexing();

        assertNotNull(block, "Block should be created");
        assertTrue(block.isDataEncrypted(), "Block MUST be encrypted");

        // Act: Search for private keyword with password
        blockchain.initializeAdvancedSearch(TEST_PASSWORD);
        SearchSpecialistAPI searchAPI = new SearchSpecialistAPI(
            blockchain, TEST_PASSWORD, userKeyPair.getPrivate()
        );

        var results = searchAPI.searchIntelligent(PRIVATE_KEYWORD, TEST_PASSWORD, 10);

        // Assert: Private keyword MUST be found
        assertFalse(results.isEmpty(),
            "Private keyword should be found with correct password");

        boolean foundCorrectBlock = results.stream()
            .anyMatch(r -> r.getBlockHash().equals(block.getHash()));
        assertTrue(foundCorrectBlock,
            "The correct block should be found by private keyword");
    }

    @Test
    @DisplayName("INCLUDE_DATA finds non-encrypted content without password")
    void testIncludeData_FindsNonEncryptedContent_WithoutPassword() throws Exception {
        // Arrange: Create NON-encrypted block
        KeyPair nonEncryptedUserKeys = CryptoUtil.generateKeyPair();
        String publicKey = CryptoUtil.publicKeyToString(nonEncryptedUserKeys.getPublic());
        blockchain.addAuthorizedKey(publicKey, "NonEncryptedUser", bootstrapKeyPair, UserRole.USER);

        Block block = blockchain.addBlockAndReturn(
            NON_ENCRYPTED_CONTENT,
            nonEncryptedUserKeys.getPrivate(),
            nonEncryptedUserKeys.getPublic()
        );
        waitForIndexing();

        assertNotNull(block, "Block should be created");
        assertFalse(block.isDataEncrypted(), "Block should NOT be encrypted");

        // Act: Search without password
        blockchain.initializeAdvancedSearch("");
        SearchSpecialistAPI searchAPI = new SearchSpecialistAPI(
            blockchain, "", nonEncryptedUserKeys.getPrivate()
        );

        var results = searchAPI.searchIntelligent(NON_ENCRYPTED_CONTENT, "", 10);

        // Assert: Non-encrypted content MUST be found
        assertFalse(results.isEmpty(),
            "Non-encrypted content should be found without password");

        boolean foundCorrectBlock = results.stream()
            .anyMatch(r -> r.getBlockHash().equals(block.getHash()));
        assertTrue(foundCorrectBlock,
            "The correct non-encrypted block should be found");
    }

    // ===== SECURITY TESTS =====

    @Test
    @DisplayName("SECURITY: Encrypted content NOT found without password")
    void testSecurity_EncryptedContentNotFound_WithoutPassword() throws Exception {
        // Arrange: Create encrypted block
        Block block = api.storeSearchableData(ENCRYPTED_CONTENT, TEST_PASSWORD, null);
        waitForIndexing();

        assertNotNull(block, "Block should be created");
        assertTrue(block.isDataEncrypted(), "Block MUST be encrypted");

        // Act: Search WITHOUT password
        blockchain.initializeAdvancedSearch("");
        SearchSpecialistAPI searchAPI = new SearchSpecialistAPI(
            blockchain, "", userKeyPair.getPrivate()
        );

        var results = searchAPI.searchIntelligent(ENCRYPTED_CONTENT, "", 10);

        // Assert: Encrypted content should NOT be found without password
        boolean foundEncryptedBlock = results.stream()
            .anyMatch(r -> r.getBlockHash().equals(block.getHash()));
        assertFalse(foundEncryptedBlock,
            "SECURITY: Encrypted content should NOT be found without password");
    }

    @Test
    @DisplayName("SECURITY: Encrypted content NOT found with wrong password")
    void testSecurity_EncryptedContentNotFound_WithWrongPassword() throws Exception {
        // Arrange: Create encrypted block
        Block block = api.storeSearchableData(ENCRYPTED_CONTENT, TEST_PASSWORD, null);
        waitForIndexing();

        assertNotNull(block, "Block should be created");
        assertTrue(block.isDataEncrypted(), "Block MUST be encrypted");

        // Act: Search with WRONG password
        blockchain.initializeAdvancedSearch(WRONG_PASSWORD);
        SearchSpecialistAPI searchAPI = new SearchSpecialistAPI(
            blockchain, WRONG_PASSWORD, userKeyPair.getPrivate()
        );

        var results = searchAPI.searchIntelligent(ENCRYPTED_CONTENT, WRONG_PASSWORD, 10);

        // Assert: Encrypted content should NOT be found with wrong password
        boolean foundEncryptedBlock = results.stream()
            .anyMatch(r -> r.getBlockHash().equals(block.getHash()));
        assertFalse(foundEncryptedBlock,
            "SECURITY: Encrypted content should NOT be found with wrong password");
    }

    @Test
    @DisplayName("SECURITY: Private keywords NOT found without password")
    void testSecurity_PrivateKeywordsNotFound_WithoutPassword() throws Exception {
        // Arrange: Create encrypted block with private keyword
        Block block = api.storeSearchableDataWithLayers(
            ENCRYPTED_CONTENT,
            TEST_PASSWORD,
            null,
            new String[]{PRIVATE_KEYWORD}
        );
        waitForIndexing();

        assertNotNull(block, "Block should be created");
        assertTrue(block.isDataEncrypted(), "Block MUST be encrypted");

        // Act: Search WITHOUT password
        blockchain.initializeAdvancedSearch("");
        SearchSpecialistAPI searchAPI = new SearchSpecialistAPI(
            blockchain, "", userKeyPair.getPrivate()
        );

        var results = searchAPI.searchIntelligent(PRIVATE_KEYWORD, "", 10);

        // Assert: Private keywords should NOT be found without password
        boolean foundEncryptedBlock = results.stream()
            .anyMatch(r -> r.getBlockHash().equals(block.getHash()));
        assertFalse(foundEncryptedBlock,
            "SECURITY: Private keywords should NOT be found without password");
    }

    // ===== CONTENT SEARCH VARIATIONS =====

    @Test
    @DisplayName("Partial content search finds encrypted block")
    void testPartialContentSearch_FindsEncryptedBlock() throws Exception {
        // Arrange: Create encrypted block with distinctive content
        String fullContent = "DistinctivePartialSearchContent_12345";
        Block block = api.storeSearchableData(fullContent, TEST_PASSWORD, null);
        waitForIndexing();

        assertNotNull(block, "Block should be created");

        // Act: Search for partial content
        blockchain.initializeAdvancedSearch(TEST_PASSWORD);
        SearchSpecialistAPI searchAPI = new SearchSpecialistAPI(
            blockchain, TEST_PASSWORD, userKeyPair.getPrivate()
        );

        var results = searchAPI.searchIntelligent("DistinctivePartialSearch", TEST_PASSWORD, 10);

        // Assert: Partial content should find the block
        assertFalse(results.isEmpty(),
            "Partial content search should find results");

        boolean foundCorrectBlock = results.stream()
            .anyMatch(r -> r.getBlockHash().equals(block.getHash()));
        assertTrue(foundCorrectBlock,
            "Partial content search should find the correct block");
    }

    @Test
    @DisplayName("Case-insensitive content search works")
    void testCaseInsensitiveContentSearch() throws Exception {
        // Arrange: Create encrypted block
        String content = "CaseSensitiveTestContent";
        Block block = api.storeSearchableData(content, TEST_PASSWORD, null);
        waitForIndexing();

        assertNotNull(block, "Block should be created");

        // Act: Search with different case
        blockchain.initializeAdvancedSearch(TEST_PASSWORD);
        SearchSpecialistAPI searchAPI = new SearchSpecialistAPI(
            blockchain, TEST_PASSWORD, userKeyPair.getPrivate()
        );

        var resultsLower = searchAPI.searchIntelligent(content.toLowerCase(), TEST_PASSWORD, 10);
        var resultsUpper = searchAPI.searchIntelligent(content.toUpperCase(), TEST_PASSWORD, 10);

        // Assert: At least one case variant should find results
        boolean foundWithLower = resultsLower.stream()
            .anyMatch(r -> r.getBlockHash().equals(block.getHash()));
        boolean foundWithUpper = resultsUpper.stream()
            .anyMatch(r -> r.getBlockHash().equals(block.getHash()));

        assertTrue(foundWithLower || foundWithUpper,
            "Case-insensitive search should find the block with at least one case variant");
    }

    // ===== DATA INTEGRITY TESTS =====

    @Test
    @DisplayName("Data decryption returns original content after search finds block")
    void testDataDecryptionConsistency() throws Exception {
        // Arrange: Create encrypted block
        Block block = api.storeSearchableData(ENCRYPTED_CONTENT, TEST_PASSWORD, null);
        waitForIndexing();

        assertNotNull(block, "Block should be created");

        // Act: Search and then decrypt
        blockchain.initializeAdvancedSearch(TEST_PASSWORD);
        SearchSpecialistAPI searchAPI = new SearchSpecialistAPI(
            blockchain, TEST_PASSWORD, userKeyPair.getPrivate()
        );

        var results = searchAPI.searchIntelligent(ENCRYPTED_CONTENT, TEST_PASSWORD, 10);

        // Assert: Search finds block
        assertFalse(results.isEmpty(), "Search should find the block");

        // Assert: Decryption returns original content
        String decryptedContent = api.retrieveSecret(block.getBlockNumber(), TEST_PASSWORD);
        assertEquals(ENCRYPTED_CONTENT, decryptedContent,
            "Decrypted content should match original");
    }

    @Test
    @DisplayName("Block hash integrity preserved after encryption")
    void testBlockHashIntegrity() throws Exception {
        // Arrange & Act
        Block block = api.storeSearchableData(ENCRYPTED_CONTENT, TEST_PASSWORD, null);
        waitForIndexing();

        // Retrieve block
        Block retrievedBlock = blockchain.getBlock(block.getBlockNumber());

        // Assert
        assertNotNull(retrievedBlock, "Block should be retrievable");
        assertEquals(block.getHash(), retrievedBlock.getHash(),
            "Block hash should be preserved");
        assertEquals(block.getBlockNumber(), retrievedBlock.getBlockNumber(),
            "Block number should match");
        assertTrue(retrievedBlock.isDataEncrypted(),
            "Block should remain encrypted");
    }

    // ===== EDGE CASES =====

    @Test
    @DisplayName("Search for non-existent content returns empty results")
    void testSearchNonExistentContent_ReturnsEmpty() throws Exception {
        // Arrange: Create encrypted block
        api.storeSearchableData(ENCRYPTED_CONTENT, TEST_PASSWORD, null);
        waitForIndexing();

        // Act: Search for content that doesn't exist
        blockchain.initializeAdvancedSearch(TEST_PASSWORD);
        SearchSpecialistAPI searchAPI = new SearchSpecialistAPI(
            blockchain, TEST_PASSWORD, userKeyPair.getPrivate()
        );

        var results = searchAPI.searchIntelligent("NONEXISTENT_CONTENT_XYZ999", TEST_PASSWORD, 10);

        // Assert: Should return empty results
        assertTrue(results.isEmpty(),
            "Search for non-existent content should return empty results");
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
