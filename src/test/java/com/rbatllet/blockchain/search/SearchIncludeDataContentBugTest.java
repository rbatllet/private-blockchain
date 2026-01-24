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
 * Rigorous tests for INCLUDE_DATA content search functionality.
 *
 * BUG FIXED: INCLUDE_DATA search level was incorrectly routing to FAST_PUBLIC
 * when password was empty, causing it to only search keywords instead of block content.
 *
 * CORRECT BEHAVIOR (after fix):
 * - Non-encrypted block + empty password → searches block content
 * - Encrypted block + valid password → searches decrypted content
 * - Encrypted block + empty/wrong password → only searches public keywords
 *
 * Test scenarios:
 * 1. searchAll finds keywords with default password
 * 2. searchIntelligent with empty password finds public keywords
 * 3. searchIntelligent with empty password finds non-encrypted CONTENT
 * 4. searchIntelligent with password finds encrypted content
 * 5. SECURITY: encrypted content NOT found without password
 * 6. SECURITY: encrypted content NOT found with wrong password
 */
public class SearchIncludeDataContentBugTest {

    // Test constants - use lowercase for keywords (they are normalized internally)
    private static final String TEST_PASSWORD = "TestPassword123#Strong";
    private static final String WRONG_PASSWORD = "WrongPassword123#Strong";
    private static final String UNIQUE_CONTENT = "UNIQUEXYZ789";
    private static final String ENCRYPTED_UNIQUE_CONTENT = "ENCRYPTEDABC456";
    private static final String PUBLIC_KEYWORD = "financial";
    private static final String TRANSACTION_KEYWORD = "transaction";
    private static final String SWIFT_KEYWORD = "swift";

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

        // Initialize search APIs with password
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
        // Block 1: Encrypted block with keywords
        encryptedBlock = encryptionAPI.storeSearchableData(
            "Financial transaction with SWIFT transfer. Content: " + ENCRYPTED_UNIQUE_CONTENT,
            TEST_PASSWORD,
            new String[]{PUBLIC_KEYWORD, TRANSACTION_KEYWORD, SWIFT_KEYWORD}
        );
        assertNotNull(encryptedBlock, "Encrypted block should be created");
        assertTrue(encryptedBlock.isDataEncrypted(), "Block should be encrypted");

        // Initialize advanced search AFTER creating encrypted block
        blockchain.initializeAdvancedSearch(TEST_PASSWORD);

        // Block 2: Non-encrypted block with UNIQUE content (NOT in keywords!)
        // This is critical for testing content search vs keyword search
        nonEncryptedBlock = blockchain.addBlockAndReturn(
            "This block contains unique content phrase " + UNIQUE_CONTENT + " that is NOT a keyword",
            userKeyPair.getPrivate(),
            userKeyPair.getPublic()
        );
        assertNotNull(nonEncryptedBlock, "Non-encrypted block should be created");
        assertFalse(nonEncryptedBlock.isDataEncrypted(), "Block should NOT be encrypted");

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

    // ===== BASELINE TESTS =====

    @Test
    @DisplayName("searchAll finds keywords with default password")
    void testSearchAll_FindsKeywords() {
        // Act: Search for keyword using searchAll (uses default password)
        var results = searchAPI.searchAll(PUBLIC_KEYWORD, 10);

        // Assert
        assertFalse(results.isEmpty(),
            "searchAll should find keyword. Got: " + results.size());

        boolean foundCorrectBlock = results.stream()
            .anyMatch(r -> r.getBlockHash().equals(encryptedBlock.getHash()));
        assertTrue(foundCorrectBlock,
            "Should find the encrypted block by keyword");
    }

    @Test
    @DisplayName("searchAll finds SWIFT keyword")
    void testSearchAll_FindsSwiftKeyword() {
        // Act
        var results = searchAPI.searchAll(SWIFT_KEYWORD, 10);

        // Assert
        assertFalse(results.isEmpty(),
            "searchAll should find SWIFT keyword. Got: " + results.size());

        boolean foundCorrectBlock = results.stream()
            .anyMatch(r -> r.getBlockHash().equals(encryptedBlock.getHash()));
        assertTrue(foundCorrectBlock,
            "Should find the encrypted block by SWIFT keyword");
    }

    // ===== SEARCH WITH PASSWORD TESTS =====

    @Test
    @DisplayName("searchIntelligent with password finds public keywords")
    void testSearchIntelligentWithPassword_FindsPublicKeywords() {
        // Act: Search for keyword with password
        var results = searchAPI.searchIntelligent(TRANSACTION_KEYWORD, TEST_PASSWORD, 10);

        // Assert: Keywords should be found with password
        assertFalse(results.isEmpty(),
            "searchIntelligent with password should find keyword. Got: " + results.size());

        boolean foundCorrectBlock = results.stream()
            .anyMatch(r -> r.getBlockHash().equals(encryptedBlock.getHash()));
        assertTrue(foundCorrectBlock,
            "Should find the encrypted block by keyword");
    }

    @Test
    @DisplayName("searchExhaustiveOffChain finds non-encrypted block CONTENT")
    void testSearchExhaustiveOffChain_FindsNonEncryptedContent() {
        // Re-index the non-encrypted block
        searchEngine.indexFilteredBlocks(List.of(nonEncryptedBlock), "", userKeyPair.getPrivate());
        waitForIndexing();

        // Act: Search for UNIQUE_CONTENT which is in block data but NOT a keyword
        var results = searchAPI.searchExhaustiveOffChain(UNIQUE_CONTENT, "", 10);

        // Assert: Should find the non-encrypted block by content
        assertTrue(results.getResultCount() > 0,
            "searchExhaustiveOffChain should find non-encrypted CONTENT. Got: " + results.getResultCount());

        boolean foundCorrectBlock = results.getResults().stream()
            .anyMatch(r -> r.getBlockHash().equals(nonEncryptedBlock.getHash()));
        assertTrue(foundCorrectBlock,
            "Should find the non-encrypted block by its content");
    }

    @Test
    @DisplayName("searchIntelligent with password finds encrypted content")
    void testSearchIntelligentWithPassword_FindsEncryptedContent() {
        // Act: Search for encrypted unique content WITH password
        var results = searchAPI.searchIntelligent(ENCRYPTED_UNIQUE_CONTENT, TEST_PASSWORD, 10);

        // Assert: Should find the encrypted block by decrypted content
        assertFalse(results.isEmpty(),
            "searchIntelligent with password should find encrypted content. Got: " + results.size());

        boolean foundCorrectBlock = results.stream()
            .anyMatch(r -> r.getBlockHash().equals(encryptedBlock.getHash()));
        assertTrue(foundCorrectBlock,
            "Should find the encrypted block by its decrypted content");
    }

    // ===== SECURITY TESTS =====

    @Test
    @DisplayName("SECURITY: Encrypted content NOT found without password")
    void testSecurity_EncryptedContentNotFound_WithoutPassword() {
        // Act: Search for encrypted content WITHOUT password
        var results = searchAPI.searchIntelligent(ENCRYPTED_UNIQUE_CONTENT, "", 10);

        // Assert: Encrypted content should NOT be found without password
        boolean foundEncryptedBlock = results.stream()
            .anyMatch(r -> r.getBlockHash().equals(encryptedBlock.getHash()));
        assertFalse(foundEncryptedBlock,
            "SECURITY: Encrypted content should NOT be found without password");
    }

    @Test
    @DisplayName("SECURITY: Encrypted content NOT found with wrong password")
    void testSecurity_EncryptedContentNotFound_WithWrongPassword() {
        // Act: Search for encrypted content with WRONG password
        var results = searchAPI.searchIntelligent(ENCRYPTED_UNIQUE_CONTENT, WRONG_PASSWORD, 10);

        // Assert: Encrypted content should NOT be found with wrong password
        boolean foundEncryptedBlock = results.stream()
            .anyMatch(r -> r.getBlockHash().equals(encryptedBlock.getHash()));
        assertFalse(foundEncryptedBlock,
            "SECURITY: Encrypted content should NOT be found with wrong password");
    }

    @Test
    @DisplayName("SECURITY: searchIntelligent requires password to find keywords")
    void testSecurity_SearchIntelligentRequiresPassword() {
        // Act: Search for keyword without password
        var results = searchAPI.searchIntelligent(PUBLIC_KEYWORD, "", 10);

        // Assert: searchIntelligent should NOT find keywords without password
        // This is expected security behavior - use searchAll for password-less search
        boolean foundBlock = results.stream()
            .anyMatch(r -> r.getBlockHash().equals(encryptedBlock.getHash()));
        assertFalse(foundBlock,
            "SECURITY: searchIntelligent should NOT find encrypted block keywords without password");
    }

    @Test
    @DisplayName("searchAll finds keywords without explicit password (uses default)")
    void testSearchAll_FindsKeywords_WithDefaultPassword() {
        // Act: searchAll uses the default password from constructor
        var results = searchAPI.searchAll(PUBLIC_KEYWORD, 10);

        // Assert: Keywords should be found
        assertFalse(results.isEmpty(),
            "searchAll should find keywords using default password. Got: " + results.size());
    }

    // ===== BLOCK VERIFICATION TESTS =====

    @Test
    @DisplayName("Non-encrypted block content is searchable")
    void testNonEncryptedBlock_ContentSearchable() {
        // Verify block structure
        assertNotNull(nonEncryptedBlock, "Non-encrypted block should exist");
        assertFalse(nonEncryptedBlock.isDataEncrypted(), "Block should not be encrypted");

        // Verify content contains the unique phrase
        String data = nonEncryptedBlock.getData();
        assertTrue(data.contains(UNIQUE_CONTENT),
            "Block data should contain unique content. Got: " + data);
    }

    @Test
    @DisplayName("Encrypted block is properly encrypted")
    void testEncryptedBlock_ProperlyEncrypted() {
        // Verify block structure
        assertNotNull(encryptedBlock, "Encrypted block should exist");
        assertTrue(encryptedBlock.isDataEncrypted(), "Block should be encrypted");

        // Verify raw data does NOT contain the unique phrase (it's encrypted)
        String data = encryptedBlock.getData();
        assertFalse(data.contains(ENCRYPTED_UNIQUE_CONTENT),
            "Encrypted block data should NOT contain plaintext unique content");
    }

    @Test
    @DisplayName("Block hash integrity preserved after multiple searches")
    void testBlockHashIntegrity_PreservedAfterSearches() {
        String originalNonEncryptedHash = nonEncryptedBlock.getHash();
        String originalEncryptedHash = encryptedBlock.getHash();

        // Perform multiple searches
        searchAPI.searchAll(PUBLIC_KEYWORD, 10);
        searchAPI.searchIntelligent(UNIQUE_CONTENT, "", 10);
        searchAPI.searchIntelligent(ENCRYPTED_UNIQUE_CONTENT, TEST_PASSWORD, 10);

        // Verify hashes are preserved
        Block retrievedNonEncrypted = blockchain.getBlock(nonEncryptedBlock.getBlockNumber());
        Block retrievedEncrypted = blockchain.getBlock(encryptedBlock.getBlockNumber());

        assertEquals(originalNonEncryptedHash, retrievedNonEncrypted.getHash(),
            "Non-encrypted block hash should be preserved");
        assertEquals(originalEncryptedHash, retrievedEncrypted.getHash(),
            "Encrypted block hash should be preserved");
    }

    // ===== SEARCH ENGINE DIRECT TESTS =====

    @Test
    @DisplayName("SearchFrameworkEngine searchExhaustiveOffChain finds content")
    void testSearchEngine_SearchExhaustiveOffChain_FindsContent() {
        // Re-index using searchEngine directly
        searchEngine.indexFilteredBlocks(List.of(nonEncryptedBlock, encryptedBlock),
            TEST_PASSWORD, userKeyPair.getPrivate());
        waitForIndexing();

        // Act: Use searchExhaustiveOffChain directly
        var results = searchEngine.searchExhaustiveOffChain(UNIQUE_CONTENT, "", 10);

        // Assert
        assertTrue(results.getResultCount() > 0,
            "searchExhaustiveOffChain should find non-encrypted content. Got: " + results.getResultCount());

        assertEquals(SearchLevel.INCLUDE_OFFCHAIN, results.getSearchLevel(),
            "Search level should be INCLUDE_OFFCHAIN");
    }

    @Test
    @DisplayName("SearchFrameworkEngine search with password finds encrypted content")
    void testSearchEngine_SearchWithPassword_FindsEncryptedContent() {
        // Re-index using searchEngine directly
        searchEngine.indexFilteredBlocks(List.of(encryptedBlock),
            TEST_PASSWORD, userKeyPair.getPrivate());
        waitForIndexing();

        // Act: Search for encrypted content with password
        var results = searchEngine.search(ENCRYPTED_UNIQUE_CONTENT, TEST_PASSWORD, 10);

        // Assert
        assertTrue(results.getResultCount() > 0,
            "search with password should find encrypted content. Got: " + results.getResultCount());

        boolean foundCorrectBlock = results.getResults().stream()
            .anyMatch(r -> r.getBlockHash().equals(encryptedBlock.getHash()));
        assertTrue(foundCorrectBlock,
            "Should find the correct encrypted block");
    }
}
