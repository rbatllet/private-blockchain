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
import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.util.TestGenesisKeyManager;

/**
 * Rigorous tests for "public:" prefix functionality in indexFilteredBlocks.
 *
 * BUG FIXED: Keywords with "public:" prefix in encrypted blocks were not searchable
 * with FAST_ONLY (password-less) searches.
 *
 * ROOT CAUSE: indexFilteredBlocks() was putting ALL user terms into privateTerms
 * when the block is encrypted, regardless of the "public:" prefix.
 *
 * CORRECT BEHAVIOR (after fix):
 * - Keywords with "public:" prefix → publicTerms (searchable without password)
 * - Keywords without prefix → privateTerms (require password)
 *
 * Test scenarios:
 * 1. FAST_ONLY finds public keywords in encrypted blocks
 * 2. FAST_ONLY does NOT find private keywords
 * 3. Password search finds BOTH public AND private keywords
 * 4. SECURITY: Private keywords NOT found without password
 * 5. SECURITY: Private keywords NOT found with wrong password
 */
public class IndexFilteredBlocksPublicPrefixTest {

    // Test constants
    private static final String TEST_PASSWORD = "TestPassword123456#";
    private static final String WRONG_PASSWORD = "WrongPassword123456#";
    private static final String TEST_CONTENT = "IndexFilteredBlocksTestContent";
    private static final String PUBLIC_KEYWORD = "INDEX_PUBLIC_KEYWORD";
    private static final String PRIVATE_KEYWORD = "INDEX_PRIVATE_KEYWORD";

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

    // ===== CORE BUG FIX TESTS =====

    @Test
    @DisplayName("FAST_ONLY finds public: prefix keywords in encrypted blocks (BUG FIX)")
    void testFastOnly_FindsPublicPrefixKeywords_InEncryptedBlock() {
        // Arrange
        Block block = createEncryptedBlockWithBothKeywords();
        indexBlockWithPassword(block);

        // Act: FAST_ONLY search (no password)
        SearchFrameworkEngine.SearchResult results =
            searchEngine.searchPublicOnly(PUBLIC_KEYWORD, 10);

        // Assert: Public keyword MUST be found
        assertFalse(results.getResults().isEmpty(),
            "BUG FIX: FAST_ONLY should find public: prefix keyword in encrypted block");

        boolean foundCorrectBlock = results.getResults().stream()
            .anyMatch(r -> r.getBlockHash().equals(block.getHash()));
        assertTrue(foundCorrectBlock,
            "The correct encrypted block should be found by public keyword");
    }

    @Test
    @DisplayName("FAST_ONLY does NOT find private keywords (no public: prefix)")
    void testFastOnly_DoesNotFindPrivateKeywords() {
        // Arrange
        Block block = createEncryptedBlockWithBothKeywords();
        indexBlockWithPassword(block);

        // Act: FAST_ONLY search for private keyword
        SearchFrameworkEngine.SearchResult results =
            searchEngine.searchPublicOnly(PRIVATE_KEYWORD, 10);

        // Assert: Private keyword should NOT be found
        boolean foundEncryptedBlock = results.getResults().stream()
            .anyMatch(r -> r.getBlockHash().equals(block.getHash()));
        assertFalse(foundEncryptedBlock,
            "FAST_ONLY should NOT find private keywords (no public: prefix)");
    }

    @Test
    @DisplayName("Password search finds public: prefix keywords")
    void testPasswordSearch_FindsPublicKeywords() {
        // Arrange
        Block block = createEncryptedBlockWithBothKeywords();
        indexBlockWithPassword(block);

        // Act: Search with password
        SearchFrameworkEngine.SearchResult results =
            searchEngine.search(PUBLIC_KEYWORD, TEST_PASSWORD, 10);

        // Assert
        assertFalse(results.getResults().isEmpty(),
            "Password search should find public keyword");

        boolean foundCorrectBlock = results.getResults().stream()
            .anyMatch(r -> r.getBlockHash().equals(block.getHash()));
        assertTrue(foundCorrectBlock,
            "The correct block should be found by public keyword with password");
    }

    @Test
    @DisplayName("Password search finds private keywords")
    void testPasswordSearch_FindsPrivateKeywords() {
        // Arrange
        Block block = createEncryptedBlockWithBothKeywords();
        indexBlockWithPassword(block);

        // Act: Search with password
        SearchFrameworkEngine.SearchResult results =
            searchEngine.search(PRIVATE_KEYWORD, TEST_PASSWORD, 10);

        // Assert
        assertFalse(results.getResults().isEmpty(),
            "Password search should find private keyword");

        boolean foundCorrectBlock = results.getResults().stream()
            .anyMatch(r -> r.getBlockHash().equals(block.getHash()));
        assertTrue(foundCorrectBlock,
            "The correct block should be found by private keyword with password");
    }

    @Test
    @DisplayName("Password search finds BOTH public AND private keywords")
    void testPasswordSearch_FindsBothKeywordTypes() {
        // Arrange
        Block block = createEncryptedBlockWithBothKeywords();
        indexBlockWithPassword(block);

        // Act & Assert: Public keyword found
        SearchFrameworkEngine.SearchResult publicResults =
            searchEngine.search(PUBLIC_KEYWORD, TEST_PASSWORD, 10);
        boolean foundByPublic = publicResults.getResults().stream()
            .anyMatch(r -> r.getBlockHash().equals(block.getHash()));
        assertTrue(foundByPublic,
            "Block should be found by public keyword");

        // Act & Assert: Private keyword found
        SearchFrameworkEngine.SearchResult privateResults =
            searchEngine.search(PRIVATE_KEYWORD, TEST_PASSWORD, 10);
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
        searchEngine.indexFilteredBlocks(List.of(block), "", userKeyPair.getPrivate());
        waitForIndexing();

        // Act: Search without password
        SearchFrameworkEngine.SearchResult results =
            searchEngine.search(PRIVATE_KEYWORD, "", 10);

        // Assert: Private keywords should NOT be found
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
        indexBlockWithPassword(block);

        // Act: Search with WRONG password
        SearchFrameworkEngine.SearchResult results =
            searchEngine.search(PRIVATE_KEYWORD, WRONG_PASSWORD, 10);

        // Assert: Private keywords should NOT be found
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
        searchEngine.indexFilteredBlocks(List.of(block), "", userKeyPair.getPrivate());
        waitForIndexing();

        // Act: Search for encrypted content
        SearchFrameworkEngine.SearchResult results =
            searchEngine.search(TEST_CONTENT, "", 10);

        // Assert: Encrypted content should NOT be found
        boolean foundEncryptedBlock = results.getResults().stream()
            .anyMatch(r -> r.getBlockHash().equals(block.getHash()));
        assertFalse(foundEncryptedBlock,
            "SECURITY: Encrypted content should NOT be found without password");
    }

    // ===== MANUAL KEYWORDS VERIFICATION =====

    @Test
    @DisplayName("manualKeywords field contains public: prefix keywords")
    void testManualKeywordsField_ContainsPublicPrefixKeywords() {
        // Arrange & Act
        Block block = createEncryptedBlockWithBothKeywords();

        // Assert: Verify manualKeywords contains the public keyword with prefix
        String manualKeywords = block.getManualKeywords();
        assertNotNull(manualKeywords, "manualKeywords should not be null");
        assertFalse(manualKeywords.trim().isEmpty(),
            "manualKeywords should not be empty for encrypted block with public: prefix");

        // The public keyword should be stored (case-insensitive check)
        assertTrue(manualKeywords.toLowerCase().contains(PUBLIC_KEYWORD.toLowerCase()),
            "manualKeywords should contain the public keyword. Got: " + manualKeywords);
    }

    @Test
    @DisplayName("IndexingResult reports correct number of blocks processed")
    void testIndexingResult_ReportsCorrectBlockCount() {
        // Arrange
        Block block1 = createEncryptedBlockWithBothKeywords();
        Block block2 = blockchain.addEncryptedBlockWithKeywords(
            "Second block content",
            TEST_PASSWORD,
            new String[]{"public:SECOND_PUBLIC", "SECOND_PRIVATE"},
            "TEST",
            userKeyPair.getPrivate(),
            userKeyPair.getPublic()
        );

        // Act
        SearchFrameworkEngine.IndexingResult result = searchEngine.indexFilteredBlocks(
            List.of(block1, block2),
            TEST_PASSWORD,
            userKeyPair.getPrivate()
        );
        waitForIndexing();

        // Assert
        assertEquals(2, result.getBlocksProcessed(),
            "Should process exactly 2 blocks");
        assertTrue(result.getBlocksIndexed() >= 2,
            "Should index at least 2 blocks. Got: " + result.getBlocksIndexed());
    }

    // ===== EDGE CASES =====

    @Test
    @DisplayName("Search for non-existent keyword returns empty results")
    void testSearchNonExistentKeyword_ReturnsEmpty() {
        // Arrange
        Block block = createEncryptedBlockWithBothKeywords();
        indexBlockWithPassword(block);

        // Act
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
        Block block = createEncryptedBlockWithBothKeywords();
        String originalHash = block.getHash();
        Long originalBlockNumber = block.getBlockNumber();

        indexBlockWithPassword(block);

        // Act
        Block retrievedBlock = blockchain.getBlock(originalBlockNumber);

        // Assert
        assertNotNull(retrievedBlock, "Block should be retrievable after indexing");
        assertEquals(originalHash, retrievedBlock.getHash(),
            "Block hash should be preserved after indexing");
        assertTrue(retrievedBlock.isDataEncrypted(),
            "Block should remain encrypted after indexing");
    }

    @Test
    @DisplayName("Public keywords found even when indexed without password")
    void testPublicKeywords_FoundWhenIndexedWithoutPassword() {
        // Arrange
        Block block = createEncryptedBlockWithBothKeywords();

        // Index WITHOUT password (public keywords should still be indexed)
        searchEngine.indexFilteredBlocks(List.of(block), "", userKeyPair.getPrivate());
        waitForIndexing();

        // Act: FAST_ONLY search
        SearchFrameworkEngine.SearchResult results =
            searchEngine.searchPublicOnly(PUBLIC_KEYWORD, 10);

        // Assert: Public keywords should still be found
        boolean foundCorrectBlock = results.getResults().stream()
            .anyMatch(r -> r.getBlockHash().equals(block.getHash()));
        assertTrue(foundCorrectBlock,
            "Public keywords should be found even when indexed without password");
    }

    // ===== HELPER METHODS =====

    private Block createEncryptedBlockWithBothKeywords() {
        Block block = blockchain.addEncryptedBlockWithKeywords(
            TEST_CONTENT,
            TEST_PASSWORD,
            new String[]{"public:" + PUBLIC_KEYWORD, PRIVATE_KEYWORD},
            "TEST",
            userKeyPair.getPrivate(),
            userKeyPair.getPublic()
        );

        assertNotNull(block, "Block should be created");
        assertTrue(block.isDataEncrypted(), "Block MUST be encrypted");

        return block;
    }

    private void indexBlockWithPassword(Block block) {
        searchEngine.indexFilteredBlocks(
            List.of(block),
            TEST_PASSWORD,
            userKeyPair.getPrivate()
        );
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
