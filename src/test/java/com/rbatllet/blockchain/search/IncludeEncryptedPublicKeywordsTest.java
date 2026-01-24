package com.rbatllet.blockchain.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.KeyPair;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.rbatllet.blockchain.config.EncryptionConfig;
import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.indexing.IndexingCoordinator;
import com.rbatllet.blockchain.security.UserRole;
import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.util.TestGenesisKeyManager;

/**
 * Rigorous tests for INCLUDE_OFFCHAIN behavior with public and private keywords.
 *
 * BUG FIXED: INCLUDE_OFFCHAIN (searchIntelligent with password) was NOT finding
 * public keywords that should be searchable.
 *
 * CORRECT BEHAVIOR (after fix):
 * - FAST_ONLY: Finds public keywords only (no password needed)
 * - INCLUDE_DATA/INCLUDE_OFFCHAIN: Finds BOTH public AND private keywords (with password)
 *
 * Test scenarios:
 * 1. FAST_ONLY finds public keywords
 * 2. FAST_ONLY does NOT find private keywords
 * 3. INCLUDE_OFFCHAIN finds public keywords
 * 4. INCLUDE_OFFCHAIN finds private keywords
 * 5. SECURITY: Private keywords NOT found without password
 * 6. SECURITY: Encrypted content NOT found without password
 */
public class IncludeEncryptedPublicKeywordsTest {

    // Test constants
    private static final String TEST_PASSWORD = "TestPassword123456#";
    private static final String WRONG_PASSWORD = "WrongPassword123456#";
    private static final String TEST_CONTENT = "EncryptedPublicKeywordsTestContent";
    private static final String PUBLIC_KEYWORD = "PUBLIC_KEYWORD_TEST";
    private static final String PRIVATE_KEYWORD = "PRIVATE_KEYWORD_TEST";

    private Blockchain blockchain;
    private KeyPair bootstrapKeyPair;
    private KeyPair userKeyPair;
    private SearchFrameworkEngine searchEngine;

    @BeforeEach
    void setUp() {
        // Reset ALL global state BEFORE creating new instances
        SearchFrameworkEngine.resetGlobalState();
        IndexingCoordinator.getInstance().reset();

        // Wait for any async tasks from previous tests
        try {
            IndexingCoordinator.getInstance().waitForCompletion();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        blockchain = new Blockchain();
        blockchain.clearAndReinitialize();

        bootstrapKeyPair = TestGenesisKeyManager.ensureGenesisKeysExist();
        blockchain.createBootstrapAdmin(
            CryptoUtil.publicKeyToString(bootstrapKeyPair.getPublic()),
            "BOOTSTRAP_ADMIN"
        );

        userKeyPair = CryptoUtil.generateKeyPair();
        String userPublicKey = CryptoUtil.publicKeyToString(userKeyPair.getPublic());
        blockchain.addAuthorizedKey(userPublicKey, "testUser", bootstrapKeyPair, UserRole.ADMIN);

        searchEngine = new SearchFrameworkEngine();
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

        if (searchEngine != null) {
            searchEngine.shutdown();
        }
    }

    // ===== FAST_ONLY TESTS =====

    @Test
    @DisplayName("FAST_ONLY finds public keywords in encrypted blocks")
    void testFastOnly_FindsPublicKeywords() {
        // Arrange
        Block block = createEncryptedBlockWithBothKeywords();
        indexBlockchain();

        // Act: Search with FAST_ONLY (no password needed for public keywords)
        SearchFrameworkEngine.SearchResult results =
            searchEngine.searchPublicOnly(PUBLIC_KEYWORD, 10);

        // Assert
        assertFalse(results.getResults().isEmpty(),
            "FAST_ONLY should find public keyword");

        boolean foundCorrectBlock = results.getResults().stream()
            .anyMatch(r -> r.getBlockHash().equals(block.getHash()));
        assertTrue(foundCorrectBlock,
            "FAST_ONLY should find the correct block by public keyword");
    }

    @Test
    @DisplayName("FAST_ONLY does NOT find private keywords")
    void testFastOnly_DoesNotFindPrivateKeywords() {
        // Arrange
        Block block = createEncryptedBlockWithBothKeywords();
        indexBlockchain();

        // Act: Search with FAST_ONLY for private keyword
        SearchFrameworkEngine.SearchResult results =
            searchEngine.searchPublicOnly(PRIVATE_KEYWORD, 10);

        // Assert: Private keywords should NOT be found with FAST_ONLY
        boolean foundEncryptedBlock = results.getResults().stream()
            .anyMatch(r -> r.getBlockHash().equals(block.getHash()));
        assertFalse(foundEncryptedBlock,
            "FAST_ONLY should NOT find private keywords");
    }

    // ===== INCLUDE_OFFCHAIN TESTS =====

    @Test
    @DisplayName("INCLUDE_OFFCHAIN finds public keywords with password")
    void testIncludeOffchain_FindsPublicKeywords_WithPassword() {
        // Arrange
        Block block = createEncryptedBlockWithBothKeywords();
        indexBlockchain();

        // Act: Search with INCLUDE_OFFCHAIN (search with password)
        EncryptionConfig config = new EncryptionConfig();
        SearchFrameworkEngine.SearchResult results =
            searchEngine.search(PUBLIC_KEYWORD, TEST_PASSWORD, 10, config);

        // Assert
        assertFalse(results.getResults().isEmpty(),
            "INCLUDE_OFFCHAIN should find public keyword with password");

        boolean foundCorrectBlock = results.getResults().stream()
            .anyMatch(r -> r.getBlockHash().equals(block.getHash()));
        assertTrue(foundCorrectBlock,
            "INCLUDE_OFFCHAIN should find the correct block by public keyword");
    }

    @Test
    @DisplayName("INCLUDE_OFFCHAIN finds private keywords with password")
    void testIncludeOffchain_FindsPrivateKeywords_WithPassword() {
        // Arrange
        Block block = createEncryptedBlockWithBothKeywords();
        indexBlockchain();

        // Act: Search with INCLUDE_OFFCHAIN for private keyword
        EncryptionConfig config = new EncryptionConfig();
        SearchFrameworkEngine.SearchResult results =
            searchEngine.search(PRIVATE_KEYWORD, TEST_PASSWORD, 10, config);

        // Assert
        assertFalse(results.getResults().isEmpty(),
            "INCLUDE_OFFCHAIN should find private keyword with password");

        boolean foundCorrectBlock = results.getResults().stream()
            .anyMatch(r -> r.getBlockHash().equals(block.getHash()));
        assertTrue(foundCorrectBlock,
            "INCLUDE_OFFCHAIN should find the correct block by private keyword");
    }

    @Test
    @DisplayName("INCLUDE_OFFCHAIN finds BOTH public AND private keywords")
    void testIncludeOffchain_FindsBothKeywordTypes() {
        // Arrange
        Block block = createEncryptedBlockWithBothKeywords();
        indexBlockchain();

        EncryptionConfig config = new EncryptionConfig();

        // Act & Assert: Public keyword found
        SearchFrameworkEngine.SearchResult publicResults =
            searchEngine.search(PUBLIC_KEYWORD, TEST_PASSWORD, 10, config);
        boolean foundByPublic = publicResults.getResults().stream()
            .anyMatch(r -> r.getBlockHash().equals(block.getHash()));
        assertTrue(foundByPublic,
            "Block should be found by public keyword");

        // Act & Assert: Private keyword found
        SearchFrameworkEngine.SearchResult privateResults =
            searchEngine.search(PRIVATE_KEYWORD, TEST_PASSWORD, 10, config);
        boolean foundByPrivate = privateResults.getResults().stream()
            .anyMatch(r -> r.getBlockHash().equals(block.getHash()));
        assertTrue(foundByPrivate,
            "Block should be found by private keyword");
    }

    // ===== SECURITY TESTS =====

    @Test
    @DisplayName("SECURITY: Private keywords NOT found without password")
    void testSecurity_PrivateKeywordsNotFound_WithoutPassword() {
        // Arrange
        Block block = createEncryptedBlockWithBothKeywords();

        // Index WITHOUT password
        searchEngine.indexBlockchainSync(blockchain, "", userKeyPair.getPrivate());
        waitForIndexing();

        // Act: Search without password
        EncryptionConfig config = new EncryptionConfig();
        SearchFrameworkEngine.SearchResult results =
            searchEngine.search(PRIVATE_KEYWORD, "", 10, config);

        // Assert: Private keywords should NOT be found without password
        boolean foundEncryptedBlock = results.getResults().stream()
            .anyMatch(r -> r.getBlockHash().equals(block.getHash()));
        assertFalse(foundEncryptedBlock,
            "SECURITY: Private keywords should NOT be found without password");
    }

    @Test
    @DisplayName("SECURITY: Private keywords NOT found with wrong password")
    void testSecurity_PrivateKeywordsNotFound_WithWrongPassword() {
        // Arrange
        Block block = createEncryptedBlockWithBothKeywords();
        indexBlockchain();

        // Act: Search with WRONG password
        EncryptionConfig config = new EncryptionConfig();
        SearchFrameworkEngine.SearchResult results =
            searchEngine.search(PRIVATE_KEYWORD, WRONG_PASSWORD, 10, config);

        // Assert: Private keywords should NOT be found with wrong password
        boolean foundEncryptedBlock = results.getResults().stream()
            .anyMatch(r -> r.getBlockHash().equals(block.getHash()));
        assertFalse(foundEncryptedBlock,
            "SECURITY: Private keywords should NOT be found with wrong password");
    }

    @Test
    @DisplayName("SECURITY: Encrypted content NOT found without password")
    void testSecurity_EncryptedContentNotFound_WithoutPassword() {
        // Arrange
        Block block = createEncryptedBlockWithBothKeywords();

        // Index WITHOUT password
        searchEngine.indexBlockchainSync(blockchain, "", userKeyPair.getPrivate());
        waitForIndexing();

        // Act: Search for encrypted content without password
        EncryptionConfig config = new EncryptionConfig();
        SearchFrameworkEngine.SearchResult results =
            searchEngine.search(TEST_CONTENT, "", 10, config);

        // Assert: Encrypted content should NOT be found without password
        boolean foundEncryptedBlock = results.getResults().stream()
            .anyMatch(r -> r.getBlockHash().equals(block.getHash()));
        assertFalse(foundEncryptedBlock,
            "SECURITY: Encrypted content should NOT be found without password");
    }

    // ===== EDGE CASES =====

    @Test
    @DisplayName("Search for non-existent keyword returns empty results")
    void testSearchNonExistentKeyword_ReturnsEmpty() {
        // Arrange
        createEncryptedBlockWithBothKeywords();
        indexBlockchain();

        // Act: Search for keyword that doesn't exist
        EncryptionConfig config = new EncryptionConfig();
        SearchFrameworkEngine.SearchResult results =
            searchEngine.search("NONEXISTENT_KEYWORD_XYZ999", TEST_PASSWORD, 10, config);

        // Assert
        assertEquals(0, results.getResultCount(),
            "Search for non-existent keyword should return 0 results");
    }

    @Test
    @DisplayName("Public keywords found even when indexed without password")
    void testPublicKeywords_FoundWhenIndexedWithoutPassword() {
        // Arrange
        Block block = createEncryptedBlockWithBothKeywords();

        // Index WITHOUT password (public keywords should still be indexed)
        searchEngine.indexBlockchainSync(blockchain, "", userKeyPair.getPrivate());
        waitForIndexing();

        // Act: Search for public keyword (FAST_ONLY)
        SearchFrameworkEngine.SearchResult results =
            searchEngine.searchPublicOnly(PUBLIC_KEYWORD, 10);

        // Assert: Public keywords should be found even without password
        boolean foundCorrectBlock = results.getResults().stream()
            .anyMatch(r -> r.getBlockHash().equals(block.getHash()));
        assertTrue(foundCorrectBlock,
            "Public keywords should be found even when indexed without password");
    }

    @Test
    @DisplayName("Block hash integrity preserved after search")
    void testBlockHashIntegrity_PreservedAfterSearch() {
        // Arrange
        Block block = createEncryptedBlockWithBothKeywords();
        String originalHash = block.getHash();
        Long originalBlockNumber = block.getBlockNumber();

        indexBlockchain();

        // Act: Search and retrieve
        EncryptionConfig config = new EncryptionConfig();
        SearchFrameworkEngine.SearchResult results =
            searchEngine.search(PUBLIC_KEYWORD, TEST_PASSWORD, 10, config);

        // Verify search found the block
        assertFalse(results.getResults().isEmpty(),
            "Search should find the block");
        boolean foundCorrectBlock = results.getResults().stream()
            .anyMatch(r -> r.getBlockHash().equals(originalHash));
        assertTrue(foundCorrectBlock,
            "Search should find the correct block");

        Block retrievedBlock = blockchain.getBlock(originalBlockNumber);

        // Assert
        assertNotNull(retrievedBlock, "Block should be retrievable");
        assertEquals(originalHash, retrievedBlock.getHash(),
            "Block hash should be preserved after search");
        assertTrue(retrievedBlock.isDataEncrypted(),
            "Block should remain encrypted");
    }

    // ===== HELPER METHODS =====

    private Block createEncryptedBlockWithBothKeywords() {
        Block block = blockchain.addEncryptedBlockWithKeywords(
            TEST_CONTENT,
            TEST_PASSWORD,
            new String[]{"public:" + PUBLIC_KEYWORD, PRIVATE_KEYWORD},
            "TEST",
            userKeyPair.getPrivate(),
            userKeyPair.getPublic(),
            true  // skipAutoIndexing - required for proper encrypted block indexing
        );

        assertNotNull(block, "Block should be created");
        assertTrue(block.isDataEncrypted(), "Block MUST be encrypted");

        return block;
    }

    private void indexBlockchain() {
        searchEngine.indexBlockchainSync(blockchain, TEST_PASSWORD, userKeyPair.getPrivate());
        waitForIndexing();
    }

    private void waitForIndexing() {
        try {
            IndexingCoordinator.getInstance().waitForCompletion(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
