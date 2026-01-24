package com.rbatllet.blockchain.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.KeyPair;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.indexing.IndexingCoordinator;
import com.rbatllet.blockchain.security.UserRole;
import com.rbatllet.blockchain.service.UserFriendlyEncryptionAPI;
import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.util.TestGenesisKeyManager;

/**
 * Rigorous tests for INCLUDE_DATA search with non-encrypted blocks and empty password.
 *
 * BUG FIXED: INCLUDE_DATA search with empty password was not finding non-encrypted content
 * because EncryptedContentSearch.searchEncryptedContent() didn't handle empty password correctly.
 *
 * CORRECT BEHAVIOR (after fix):
 * - Non-encrypted block + empty password → finds content via contentCache
 * - Non-encrypted block + password → finds content
 * - Encrypted block + correct password → finds decrypted content
 * - Encrypted block + empty/wrong password → does NOT find content (security)
 *
 * Test scenarios:
 * 1. Non-encrypted content found with empty password (BUG FIX)
 * 2. Non-encrypted content found with password
 * 3. Encrypted content found with correct password
 * 4. SECURITY: Encrypted content NOT found with empty password
 * 5. SECURITY: Encrypted content NOT found with wrong password
 */
public class SearchNonEncryptedContentWithNullPasswordTest {

    // Test constants
    private static final String TEST_PASSWORD = "TestPassword123#Strong";
    private static final String WRONG_PASSWORD = "WrongPassword123#Strong";
    private static final String NON_ENCRYPTED_UNIQUE = "NONENCRYPTED_UNIQUE_ABC123";
    private static final String ENCRYPTED_UNIQUE = "ENCRYPTED_UNIQUE_XYZ789";
    private static final String ENCRYPTED_KEYWORD = "encryptedkeyword";

    private Blockchain blockchain;
    private KeyPair bootstrapKeyPair;
    private KeyPair userKeyPair;
    private UserFriendlyEncryptionAPI encryptionAPI;
    private SearchSpecialistAPI searchAPI;
    private SearchFrameworkEngine searchEngine;

    // Block references for verification
    private Block nonEncryptedBlock;
    private Block encryptedBlock;

    @BeforeEach
    void setUp() throws Exception {
        // Reset ALL global state BEFORE creating new instances
        SearchFrameworkEngine.resetGlobalState();
        IndexingCoordinator.getInstance().reset();

        try {
            IndexingCoordinator.getInstance().waitForCompletion();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Initialize blockchain
        blockchain = new Blockchain();
        blockchain.clearAndReinitialize();

        // Create bootstrap admin
        bootstrapKeyPair = TestGenesisKeyManager.ensureGenesisKeysExist();
        blockchain.createBootstrapAdmin(
            CryptoUtil.publicKeyToString(bootstrapKeyPair.getPublic()),
            "BOOTSTRAP_ADMIN"
        );

        // Create admin user
        KeyPair adminKeys = CryptoUtil.generateKeyPair();
        String adminPublicKey = CryptoUtil.publicKeyToString(adminKeys.getPublic());
        blockchain.addAuthorizedKey(adminPublicKey, "Admin", bootstrapKeyPair, UserRole.ADMIN);

        encryptionAPI = new UserFriendlyEncryptionAPI(blockchain);
        encryptionAPI.setDefaultCredentials("Admin", adminKeys);

        // Create test user
        userKeyPair = encryptionAPI.createUser("TestUser");
        encryptionAPI.setDefaultCredentials("TestUser", userKeyPair);

        // Create test blocks
        createTestBlocks();

        // Initialize advanced search
        blockchain.initializeAdvancedSearch(TEST_PASSWORD);

        // Initialize search APIs
        searchAPI = new SearchSpecialistAPI(blockchain, TEST_PASSWORD, userKeyPair.getPrivate());
        searchEngine = blockchain.getSearchFrameworkEngine();
        searchEngine.setBlockchain(blockchain);

        // Wait for indexing to complete
        IndexingCoordinator.getInstance().waitForCompletion();
    }

    @AfterEach
    void tearDown() {
        try {
            IndexingCoordinator.getInstance().waitForCompletion();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (searchAPI != null) {
            searchAPI.shutdown();
        }

        if (blockchain != null) {
            blockchain.clearAndReinitialize();
            blockchain.shutdown();
        }

        SearchFrameworkEngine.resetGlobalState();
        IndexingCoordinator.getInstance().reset();

        if (searchEngine != null) {
            searchEngine.shutdown();
        }
    }

    private void createTestBlocks() throws Exception {
        // Block 1: NON-ENCRYPTED block with UNIQUE content (NOT in keywords!)
        nonEncryptedBlock = blockchain.addBlockAndReturn(
            "This is a non-encrypted block with unique content: " + NON_ENCRYPTED_UNIQUE,
            userKeyPair.getPrivate(),
            userKeyPair.getPublic()
        );
        assertNotNull(nonEncryptedBlock, "Non-encrypted block should be created");
        assertFalse(nonEncryptedBlock.isDataEncrypted(), "Block should NOT be encrypted");

        // Block 2: ENCRYPTED block with unique content and public keyword
        encryptedBlock = encryptionAPI.storeSearchableData(
            "This is an encrypted block with unique content: " + ENCRYPTED_UNIQUE,
            TEST_PASSWORD,
            new String[]{"public:" + ENCRYPTED_KEYWORD}  // public: prefix makes keyword searchable without password
        );
        assertNotNull(encryptedBlock, "Encrypted block should be created");
        assertTrue(encryptedBlock.isDataEncrypted(), "Block should be encrypted");

        // Wait for indexing
        IndexingCoordinator.getInstance().waitForCompletion();
    }

    private void waitForIndexing() {
        try {
            IndexingCoordinator.getInstance().waitForCompletion(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ===== BUG FIX TESTS =====

    @Test
    @DisplayName("BUG FIX: searchExhaustiveOffChain with empty password finds NON-ENCRYPTED content")
    void testSearchExhaustiveOffChain_EmptyPassword_FindsNonEncryptedContent() {
        // Re-index the non-encrypted block
        searchEngine.indexFilteredBlocks(List.of(nonEncryptedBlock), "", userKeyPair.getPrivate());
        waitForIndexing();

        // Act: Search for non-encrypted content with empty password
        var results = searchAPI.searchExhaustiveOffChain(NON_ENCRYPTED_UNIQUE, "", 10);

        // Assert: Should find the non-encrypted block
        assertTrue(results.getResultCount() > 0,
            "BUG FIX: Should find non-encrypted content with empty password. Got: " + results.getResultCount());

        boolean foundCorrectBlock = results.getResults().stream()
            .anyMatch(r -> r.getBlockHash().equals(nonEncryptedBlock.getHash()));
        assertTrue(foundCorrectBlock,
            "Should find the correct non-encrypted block");
    }

    @Test
    @DisplayName("Non-encrypted content found with password")
    void testSearchExhaustiveOffChain_WithPassword_FindsNonEncryptedContent() {
        // Re-index the non-encrypted block
        searchEngine.indexFilteredBlocks(List.of(nonEncryptedBlock), TEST_PASSWORD, userKeyPair.getPrivate());
        waitForIndexing();

        // Act: Search for non-encrypted content with password
        var results = searchAPI.searchExhaustiveOffChain(NON_ENCRYPTED_UNIQUE, TEST_PASSWORD, 10);

        // Assert: Should find the non-encrypted block
        assertTrue(results.getResultCount() > 0,
            "Should find non-encrypted content with password. Got: " + results.getResultCount());

        boolean foundCorrectBlock = results.getResults().stream()
            .anyMatch(r -> r.getBlockHash().equals(nonEncryptedBlock.getHash()));
        assertTrue(foundCorrectBlock,
            "Should find the correct non-encrypted block");
    }

    // ===== ENCRYPTED CONTENT TESTS =====

    @Test
    @DisplayName("Encrypted content found with correct password")
    void testSearchIntelligent_CorrectPassword_FindsEncryptedContent() {
        // Act: Search for encrypted content with correct password
        var results = searchAPI.searchIntelligent(ENCRYPTED_UNIQUE, TEST_PASSWORD, 10);

        // Assert: Should find the encrypted block
        assertFalse(results.isEmpty(),
            "Should find encrypted content with correct password. Got: " + results.size());

        boolean foundCorrectBlock = results.stream()
            .anyMatch(r -> r.getBlockHash().equals(encryptedBlock.getHash()));
        assertTrue(foundCorrectBlock,
            "Should find the correct encrypted block");
    }

    @Test
    @DisplayName("Encrypted keyword found via searchEngine.searchExhaustiveOffChain")
    void testSearchEngine_SearchExhaustiveOffChain_FindsEncryptedKeyword() {
        // Verify manualKeywords contains the keyword
        String manualKeywords = encryptedBlock.getManualKeywords();
        assertNotNull(manualKeywords, "manualKeywords should not be null");
        assertTrue(manualKeywords.toLowerCase().contains(ENCRYPTED_KEYWORD.toLowerCase()),
            "manualKeywords should contain the keyword. Got: " + manualKeywords);

        // FIX: Clear global state and re-index to ensure FastIndexSearch gets the keywords
        // This is needed because async indexing triggered by addEncryptedBlockWithKeywords
        // may not properly index to FastIndexSearch due to race conditions.
        SearchFrameworkEngine.resetGlobalState();
        blockchain.initializeAdvancedSearch(TEST_PASSWORD);
        waitForIndexing();

        // Act: Search for keyword using searchEngine.searchExhaustiveOffChain
        var results = searchEngine.searchExhaustiveOffChain(ENCRYPTED_KEYWORD, TEST_PASSWORD, 10);

        // Assert: Should find the encrypted block by keyword
        assertTrue(results.getResultCount() > 0,
            "Should find encrypted block by keyword. Got: " + results.getResultCount() +
            ". manualKeywords=" + manualKeywords + ", blockHash=" + encryptedBlock.getHash().substring(0, 16));

        boolean foundCorrectBlock = results.getResults().stream()
            .anyMatch(r -> r.getBlockHash().equals(encryptedBlock.getHash()));
        assertTrue(foundCorrectBlock,
            "Should find the correct encrypted block by keyword");
    }

    // ===== SECURITY TESTS =====

    @Test
    @DisplayName("SECURITY: Encrypted content NOT found with empty password")
    void testSecurity_EncryptedContentNotFound_EmptyPassword() {
        // Act: Search for encrypted content with empty password
        var results = searchAPI.searchIntelligent(ENCRYPTED_UNIQUE, "", 10);

        // Assert: Should NOT find the encrypted block by content
        boolean foundEncryptedBlock = results.stream()
            .anyMatch(r -> r.getBlockHash().equals(encryptedBlock.getHash()));
        assertFalse(foundEncryptedBlock,
            "SECURITY: Encrypted content should NOT be found with empty password");
    }

    @Test
    @DisplayName("SECURITY: Encrypted content NOT found with wrong password")
    void testSecurity_EncryptedContentNotFound_WrongPassword() {
        // Act: Search for encrypted content with wrong password
        var results = searchAPI.searchIntelligent(ENCRYPTED_UNIQUE, WRONG_PASSWORD, 10);

        // Assert: Should NOT find the encrypted block by content
        boolean foundEncryptedBlock = results.stream()
            .anyMatch(r -> r.getBlockHash().equals(encryptedBlock.getHash()));
        assertFalse(foundEncryptedBlock,
            "SECURITY: Encrypted content should NOT be found with wrong password");
    }

    @Test
    @DisplayName("SECURITY: searchIntelligent requires password for encrypted content")
    void testSecurity_SearchIntelligentRequiresPassword() {
        // Act: Search for encrypted unique content with empty password
        var results = searchAPI.searchIntelligent(ENCRYPTED_UNIQUE, "", 10);

        // Assert: Should NOT find encrypted content
        boolean foundBlock = results.stream()
            .anyMatch(r -> r.getBlockHash().equals(encryptedBlock.getHash()));
        assertFalse(foundBlock,
            "SECURITY: searchIntelligent should NOT find encrypted content without password");
    }

    // ===== BLOCK VERIFICATION TESTS =====

    @Test
    @DisplayName("Non-encrypted block has correct structure")
    void testNonEncryptedBlock_CorrectStructure() {
        assertNotNull(nonEncryptedBlock, "Non-encrypted block should exist");
        assertFalse(nonEncryptedBlock.isDataEncrypted(), "Block should not be encrypted");

        // Verify content contains the unique phrase
        String data = nonEncryptedBlock.getData();
        assertTrue(data.contains(NON_ENCRYPTED_UNIQUE),
            "Block data should contain unique content. Got: " + data);
    }

    @Test
    @DisplayName("Encrypted block has correct structure")
    void testEncryptedBlock_CorrectStructure() {
        assertNotNull(encryptedBlock, "Encrypted block should exist");
        assertTrue(encryptedBlock.isDataEncrypted(), "Block should be encrypted");

        // Verify raw data does NOT contain the unique phrase (it's encrypted)
        String data = encryptedBlock.getData();
        assertFalse(data.contains(ENCRYPTED_UNIQUE),
            "Encrypted block data should NOT contain plaintext unique content");
    }

    @Test
    @DisplayName("Block hash integrity preserved after searches")
    void testBlockHashIntegrity_PreservedAfterSearches() {
        String originalNonEncryptedHash = nonEncryptedBlock.getHash();
        String originalEncryptedHash = encryptedBlock.getHash();

        // Re-index blocks
        searchEngine.indexFilteredBlocks(List.of(nonEncryptedBlock, encryptedBlock),
            TEST_PASSWORD, userKeyPair.getPrivate());
        waitForIndexing();

        // Perform multiple searches
        searchAPI.searchExhaustiveOffChain(NON_ENCRYPTED_UNIQUE, "", 10);
        searchAPI.searchIntelligent(ENCRYPTED_UNIQUE, TEST_PASSWORD, 10);
        searchAPI.searchIntelligent(ENCRYPTED_KEYWORD, TEST_PASSWORD, 10);

        // Verify hashes are preserved
        Block retrievedNonEncrypted = blockchain.getBlock(nonEncryptedBlock.getBlockNumber());
        Block retrievedEncrypted = blockchain.getBlock(encryptedBlock.getBlockNumber());

        assertEquals(originalNonEncryptedHash, retrievedNonEncrypted.getHash(),
            "Non-encrypted block hash should be preserved");
        assertEquals(originalEncryptedHash, retrievedEncrypted.getHash(),
            "Encrypted block hash should be preserved");
    }

    // ===== SEARCH LEVEL TESTS =====

    @Test
    @DisplayName("SearchLevel is INCLUDE_OFFCHAIN for searchExhaustiveOffChain")
    void testSearchLevel_IncludeOffChain() {
        // Re-index
        searchEngine.indexFilteredBlocks(List.of(nonEncryptedBlock), "", userKeyPair.getPrivate());
        waitForIndexing();

        // Act
        var results = searchAPI.searchExhaustiveOffChain(NON_ENCRYPTED_UNIQUE, "", 10);

        // Assert
        assertEquals(SearchLevel.INCLUDE_OFFCHAIN, results.getSearchLevel(),
            "Search level should be INCLUDE_OFFCHAIN");
    }

    // ===== EDGE CASES =====

    @Test
    @DisplayName("Search for non-existent content returns empty")
    void testSearchNonExistent_ReturnsEmpty() {
        // Act
        var results = searchAPI.searchExhaustiveOffChain("NONEXISTENT_CONTENT_999", TEST_PASSWORD, 10);

        // Assert
        assertEquals(0, results.getResultCount(),
            "Search for non-existent content should return 0 results");
    }

    @Test
    @DisplayName("Search respects maxResults limit")
    void testMaxResultsLimit_Respected() {
        // Re-index
        searchEngine.indexFilteredBlocks(List.of(nonEncryptedBlock, encryptedBlock),
            TEST_PASSWORD, userKeyPair.getPrivate());
        waitForIndexing();

        // Act: Search with maxResults=1
        var results = searchAPI.searchExhaustiveOffChain(ENCRYPTED_KEYWORD, TEST_PASSWORD, 1);

        // Assert
        assertTrue(results.getResults().size() <= 1,
            "Results should respect maxResults limit. Got: " + results.getResults().size());
    }
}
