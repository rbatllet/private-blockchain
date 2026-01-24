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
 * Rigorous tests to verify auto-indexing works correctly for encrypted blocks.
 *
 * BUG FIXED: Auto-indexing (skipAutoIndexing=false) was causing encrypted blocks
 * to not be properly indexed due to race condition between two indexing systems.
 *
 * ROOT CAUSE: Blockchain auto-indexing would mark blocks as processed but skip
 * encrypted blocks (no password), then indexBlockchainSync() would skip re-indexing
 * due to globalProcessingMap coordination.
 *
 * CORRECT BEHAVIOR (after fix):
 * - skipAutoIndexing=false (default): Encrypted blocks indexed correctly
 * - skipAutoIndexing=true: Encrypted blocks indexed correctly
 * - Public keywords searchable after indexing
 * - Private keywords searchable with correct password
 *
 * Test scenarios:
 * 1. Default auto-indexing works for encrypted blocks
 * 2. Skip auto-indexing works for encrypted blocks
 * 3. Public keywords found with and without password
 * 4. Private keywords found only with correct password
 * 5. SECURITY: Encrypted content NOT found without password
 */
public class AutoIndexingBugReproductionTest {

    // Test constants
    private static final String TEST_PASSWORD = "TestPassword123456#";
    private static final String WRONG_PASSWORD = "WrongPassword123456#";
    private static final String TEST_CONTENT = "AutoIndexingTestContent";
    private static final String PUBLIC_KEYWORD = "AUTOINDEX_PUBLIC_KEYWORD";
    private static final String PRIVATE_KEYWORD = "AUTOINDEX_PRIVATE_KEYWORD";

    private Blockchain blockchain;
    private KeyPair bootstrapKeyPair;
    private KeyPair userKeyPair;
    private SearchFrameworkEngine searchEngine;

    @BeforeEach
    void setUp() {
        // Reset ALL global state BEFORE creating new instances
        SearchFrameworkEngine.resetGlobalState();
        IndexingCoordinator.getInstance().reset();

        // Wait for any async indexing tasks from previous tests
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

        EncryptionConfig config = EncryptionConfig.createHighSecurityConfig();
        searchEngine = new SearchFrameworkEngine(config);
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

    // ===== CORE BUG FIX TESTS =====

    @Test
    @DisplayName("Default auto-indexing (skipAutoIndexing=false) indexes encrypted blocks correctly")
    void testDefaultAutoIndexing_EncryptedBlocksIndexedCorrectly() {
        // Arrange: Add encrypted block with skipAutoIndexing=false (DEFAULT)
        Block block = blockchain.addEncryptedBlockWithKeywords(
            TEST_CONTENT,
            TEST_PASSWORD,
            new String[]{"public:" + PUBLIC_KEYWORD},
            "TEST",
            userKeyPair.getPrivate(),
            userKeyPair.getPublic()
            // skipAutoIndexing=false is default
        );

        assertNotNull(block, "Block should be created");
        assertTrue(block.isDataEncrypted(), "Block MUST be encrypted");

        // Wait for auto-indexing to complete
        waitForIndexing();

        // Act: Index with indexBlockchainSync
        SearchFrameworkEngine.IndexingResult indexResult =
            searchEngine.indexBlockchainSync(blockchain, TEST_PASSWORD, userKeyPair.getPrivate());

        assertTrue(indexResult.getBlocksIndexed() >= 1,
            "At least 1 block should be indexed. Got: " + indexResult.getBlocksIndexed());

        // Assert: Search for public keyword
        SearchFrameworkEngine.SearchResult results =
            searchEngine.search(PUBLIC_KEYWORD, TEST_PASSWORD, 10);

        assertTrue(results.getResultCount() > 0,
            "BUG FIX: Default auto-indexing should find encrypted block. Got: " + results.getResultCount());

        boolean foundCorrectBlock = results.getResults().stream()
            .anyMatch(r -> r.getBlockHash().equals(block.getHash()));
        assertTrue(foundCorrectBlock,
            "The correct encrypted block should be found");
    }

    @Test
    @DisplayName("Skip auto-indexing (skipAutoIndexing=true) indexes encrypted blocks correctly")
    void testSkipAutoIndexing_EncryptedBlocksIndexedCorrectly() {
        // Arrange: Add encrypted block with skipAutoIndexing=true
        Block block = blockchain.addEncryptedBlockWithKeywords(
            TEST_CONTENT,
            TEST_PASSWORD,
            new String[]{"public:" + PUBLIC_KEYWORD},
            "TEST",
            userKeyPair.getPrivate(),
            userKeyPair.getPublic(),
            true  // skipAutoIndexing=true
        );

        assertNotNull(block, "Block should be created");
        assertTrue(block.isDataEncrypted(), "Block MUST be encrypted");

        // Act: Index with indexBlockchainSync
        SearchFrameworkEngine.IndexingResult indexResult =
            searchEngine.indexBlockchainSync(blockchain, TEST_PASSWORD, userKeyPair.getPrivate());

        waitForIndexing();

        assertTrue(indexResult.getBlocksIndexed() >= 1,
            "At least 1 block should be indexed. Got: " + indexResult.getBlocksIndexed());

        // Assert: Search for public keyword
        SearchFrameworkEngine.SearchResult results =
            searchEngine.search(PUBLIC_KEYWORD, TEST_PASSWORD, 10);

        assertTrue(results.getResultCount() > 0,
            "skipAutoIndexing=true should allow proper indexing. Got: " + results.getResultCount());

        boolean foundCorrectBlock = results.getResults().stream()
            .anyMatch(r -> r.getBlockHash().equals(block.getHash()));
        assertTrue(foundCorrectBlock,
            "The correct encrypted block should be found");
    }

    // ===== PUBLIC KEYWORD TESTS =====

    @Test
    @DisplayName("Public keywords searchable with password after auto-indexing")
    void testPublicKeywords_SearchableWithPassword() {
        // Arrange
        Block block = blockchain.addEncryptedBlockWithKeywords(
            TEST_CONTENT,
            TEST_PASSWORD,
            new String[]{"public:" + PUBLIC_KEYWORD},
            "TEST",
            userKeyPair.getPrivate(),
            userKeyPair.getPublic()
        );

        assertNotNull(block, "Block should be created");
        waitForIndexing();

        searchEngine.indexBlockchainSync(blockchain, TEST_PASSWORD, userKeyPair.getPrivate());

        // Act: Search with password
        SearchFrameworkEngine.SearchResult results =
            searchEngine.search(PUBLIC_KEYWORD, TEST_PASSWORD, 10);

        // Assert
        assertTrue(results.getResultCount() > 0,
            "Public keywords should be searchable with password");

        boolean foundCorrectBlock = results.getResults().stream()
            .anyMatch(r -> r.getBlockHash().equals(block.getHash()));
        assertTrue(foundCorrectBlock,
            "The correct block should be found by public keyword");
    }

    @Test
    @DisplayName("Public keywords searchable without password after auto-indexing")
    void testPublicKeywords_SearchableWithoutPassword() {
        // Arrange
        Block block = blockchain.addEncryptedBlockWithKeywords(
            TEST_CONTENT,
            TEST_PASSWORD,
            new String[]{"public:" + PUBLIC_KEYWORD},
            "TEST",
            userKeyPair.getPrivate(),
            userKeyPair.getPublic()
        );

        assertNotNull(block, "Block should be created");
        waitForIndexing();

        // Index WITHOUT password (public keywords should still be indexed)
        searchEngine.indexBlockchainSync(blockchain, "", userKeyPair.getPrivate());

        // Act: Search without password
        SearchFrameworkEngine.SearchResult results =
            searchEngine.search(PUBLIC_KEYWORD, "", 10);

        // Assert: Public keywords should be searchable without password
        assertTrue(results.getResultCount() > 0,
            "Public keywords should be searchable without password");

        boolean foundCorrectBlock = results.getResults().stream()
            .anyMatch(r -> r.getBlockHash().equals(block.getHash()));
        assertTrue(foundCorrectBlock,
            "The correct block should be found by public keyword without password");
    }

    // ===== PRIVATE KEYWORD TESTS =====

    @Test
    @DisplayName("Private keywords searchable with correct password")
    void testPrivateKeywords_SearchableWithCorrectPassword() {
        // Arrange: Create block with private keyword (no public: prefix)
        Block block = blockchain.addEncryptedBlockWithKeywords(
            TEST_CONTENT,
            TEST_PASSWORD,
            new String[]{PRIVATE_KEYWORD},  // No public: prefix = private
            "TEST",
            userKeyPair.getPrivate(),
            userKeyPair.getPublic()
        );

        assertNotNull(block, "Block should be created");
        waitForIndexing();

        searchEngine.indexBlockchainSync(blockchain, TEST_PASSWORD, userKeyPair.getPrivate());

        // Act: Search with correct password
        SearchFrameworkEngine.SearchResult results =
            searchEngine.search(PRIVATE_KEYWORD, TEST_PASSWORD, 10);

        // Assert
        assertTrue(results.getResultCount() > 0,
            "Private keywords should be searchable with correct password");

        boolean foundCorrectBlock = results.getResults().stream()
            .anyMatch(r -> r.getBlockHash().equals(block.getHash()));
        assertTrue(foundCorrectBlock,
            "The correct block should be found by private keyword");
    }

    // ===== SECURITY TESTS =====

    @Test
    @DisplayName("SECURITY: Private keywords NOT searchable without password")
    void testSecurity_PrivateKeywordsNotSearchableWithoutPassword() {
        // Arrange
        Block block = blockchain.addEncryptedBlockWithKeywords(
            TEST_CONTENT,
            TEST_PASSWORD,
            new String[]{PRIVATE_KEYWORD},  // Private keyword
            "TEST",
            userKeyPair.getPrivate(),
            userKeyPair.getPublic()
        );

        assertNotNull(block, "Block should be created");
        waitForIndexing();

        // Index without password
        searchEngine.indexBlockchainSync(blockchain, "", userKeyPair.getPrivate());

        // Act: Search without password
        SearchFrameworkEngine.SearchResult results =
            searchEngine.search(PRIVATE_KEYWORD, "", 10);

        // Assert: Private keywords should NOT be found without password
        boolean foundEncryptedBlock = results.getResults().stream()
            .anyMatch(r -> r.getBlockHash().equals(block.getHash()));
        assertFalse(foundEncryptedBlock,
            "SECURITY: Private keywords should NOT be searchable without password");
    }

    @Test
    @DisplayName("SECURITY: Private keywords NOT searchable with wrong password")
    void testSecurity_PrivateKeywordsNotSearchableWithWrongPassword() {
        // Arrange
        Block block = blockchain.addEncryptedBlockWithKeywords(
            TEST_CONTENT,
            TEST_PASSWORD,
            new String[]{PRIVATE_KEYWORD},
            "TEST",
            userKeyPair.getPrivate(),
            userKeyPair.getPublic()
        );

        assertNotNull(block, "Block should be created");
        waitForIndexing();

        // Index with correct password
        searchEngine.indexBlockchainSync(blockchain, TEST_PASSWORD, userKeyPair.getPrivate());

        // Act: Search with WRONG password
        SearchFrameworkEngine.SearchResult results =
            searchEngine.search(PRIVATE_KEYWORD, WRONG_PASSWORD, 10);

        // Assert: Private keywords should NOT be found with wrong password
        boolean foundEncryptedBlock = results.getResults().stream()
            .anyMatch(r -> r.getBlockHash().equals(block.getHash()));
        assertFalse(foundEncryptedBlock,
            "SECURITY: Private keywords should NOT be searchable with wrong password");
    }

    @Test
    @DisplayName("SECURITY: Encrypted content NOT searchable without password")
    void testSecurity_EncryptedContentNotSearchableWithoutPassword() {
        // Arrange
        Block block = blockchain.addEncryptedBlockWithKeywords(
            TEST_CONTENT,
            TEST_PASSWORD,
            new String[]{"public:" + PUBLIC_KEYWORD},
            "TEST",
            userKeyPair.getPrivate(),
            userKeyPair.getPublic()
        );

        assertNotNull(block, "Block should be created");
        waitForIndexing();

        // Index without password
        searchEngine.indexBlockchainSync(blockchain, "", userKeyPair.getPrivate());

        // Act: Search for encrypted content without password
        SearchFrameworkEngine.SearchResult results =
            searchEngine.search(TEST_CONTENT, "", 10);

        // Assert: Encrypted content should NOT be found without password
        boolean foundEncryptedBlock = results.getResults().stream()
            .anyMatch(r -> r.getBlockHash().equals(block.getHash()));
        assertFalse(foundEncryptedBlock,
            "SECURITY: Encrypted content should NOT be searchable without password");
    }

    // ===== INDEXING RESULT TESTS =====

    @Test
    @DisplayName("IndexingResult reports correct number of blocks indexed")
    void testIndexingResult_ReportsCorrectBlockCount() {
        // Arrange: Add multiple encrypted blocks
        Block block1 = blockchain.addEncryptedBlockWithKeywords(
            "Content 1",
            TEST_PASSWORD,
            new String[]{"public:KEYWORD_ONE"},
            "TEST",
            userKeyPair.getPrivate(),
            userKeyPair.getPublic()
        );

        Block block2 = blockchain.addEncryptedBlockWithKeywords(
            "Content 2",
            TEST_PASSWORD,
            new String[]{"public:KEYWORD_TWO"},
            "TEST",
            userKeyPair.getPrivate(),
            userKeyPair.getPublic()
        );

        assertNotNull(block1, "Block 1 should be created");
        assertNotNull(block2, "Block 2 should be created");
        waitForIndexing();

        // Act: Index blockchain
        SearchFrameworkEngine.IndexingResult indexResult =
            searchEngine.indexBlockchainSync(blockchain, TEST_PASSWORD, userKeyPair.getPrivate());

        // Assert: At least 2 blocks indexed (plus genesis)
        assertTrue(indexResult.getBlocksIndexed() >= 2,
            "At least 2 encrypted blocks should be indexed. Got: " + indexResult.getBlocksIndexed());
    }

    // ===== EDGE CASES =====

    @Test
    @DisplayName("Search for non-existent keyword returns empty results")
    void testSearchNonExistentKeyword_ReturnsEmpty() {
        // Arrange
        blockchain.addEncryptedBlockWithKeywords(
            TEST_CONTENT,
            TEST_PASSWORD,
            new String[]{"public:" + PUBLIC_KEYWORD},
            "TEST",
            userKeyPair.getPrivate(),
            userKeyPair.getPublic()
        );

        waitForIndexing();
        searchEngine.indexBlockchainSync(blockchain, TEST_PASSWORD, userKeyPair.getPrivate());

        // Act: Search for keyword that doesn't exist
        SearchFrameworkEngine.SearchResult results =
            searchEngine.search("NONEXISTENT_KEYWORD_XYZ999", TEST_PASSWORD, 10);

        // Assert
        assertEquals(0, results.getResultCount(),
            "Search for non-existent keyword should return 0 results");
    }

    @Test
    @DisplayName("Block hash integrity preserved after indexing")
    void testBlockHashIntegrity_PreservedAfterIndexing() {
        // Arrange
        Block block = blockchain.addEncryptedBlockWithKeywords(
            TEST_CONTENT,
            TEST_PASSWORD,
            new String[]{"public:" + PUBLIC_KEYWORD},
            "TEST",
            userKeyPair.getPrivate(),
            userKeyPair.getPublic()
        );

        String originalHash = block.getHash();
        Long originalBlockNumber = block.getBlockNumber();

        waitForIndexing();
        searchEngine.indexBlockchainSync(blockchain, TEST_PASSWORD, userKeyPair.getPrivate());

        // Act: Retrieve block after indexing
        Block retrievedBlock = blockchain.getBlock(originalBlockNumber);

        // Assert
        assertNotNull(retrievedBlock, "Block should be retrievable after indexing");
        assertEquals(originalHash, retrievedBlock.getHash(),
            "Block hash should be preserved after indexing");
        assertTrue(retrievedBlock.isDataEncrypted(),
            "Block should remain encrypted after indexing");
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
