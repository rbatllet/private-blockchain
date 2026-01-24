package com.rbatllet.blockchain.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.security.KeyPair;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.rbatllet.blockchain.config.EncryptionConfig;
import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.indexing.IndexingCoordinator;
import com.rbatllet.blockchain.security.UserRole;
import com.rbatllet.blockchain.service.UserFriendlyEncryptionAPI;
import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.util.TestGenesisKeyManager;

/**
 * Rigorous tests for off-chain file content search functionality.
 *
 * BUG FIXED: INCLUDE_OFFCHAIN search level was NOT searching within
 * off-chain file content.
 *
 * CORRECT BEHAVIOR (after fix):
 * - INCLUDE_OFFCHAIN with password searches off-chain file content
 * - Public keywords in off-chain blocks are searchable without password
 * - Encrypted off-chain content requires password to search
 *
 * Test scenarios:
 * 1. Off-chain file content found with INCLUDE_OFFCHAIN + password
 * 2. Public keywords searchable in off-chain blocks
 * 3. SECURITY: Off-chain content NOT found without password
 * 4. SECURITY: Off-chain content NOT found with wrong password
 */
public class OffChainContentSearchBugTest {

    // Test constants
    private static final String TEST_PASSWORD = "BugTestPassword123#";
    private static final String WRONG_PASSWORD = "WrongPassword123#";
    private static final String UNIQUE_OFFCHAIN_WORD = "XYZUNIQUEOFFCHAINWORD123";
    private static final String PUBLIC_KEYWORD = "OFFCHAIN_PUBLIC_KEYWORD";
    private static final String PRIVATE_KEYWORD = "OFFCHAIN_PRIVATE_KEYWORD";
    private static final String OFFCHAIN_FILE_CONTENT = "This file contains the unique word: " + UNIQUE_OFFCHAIN_WORD;
    private static final String BLOCK_PLACEHOLDER = "off-chain-placeholder-data";

    private Blockchain blockchain;
    private KeyPair bootstrapKeyPair;
    private KeyPair userKeyPair;
    private SearchFrameworkEngine searchEngine;
    private UserFriendlyEncryptionAPI encryptionAPI;

    @BeforeEach
    void setUp() {
        // Reset global state
        SearchFrameworkEngine.resetGlobalState();
        IndexingCoordinator.getInstance().reset();

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

        encryptionAPI = new UserFriendlyEncryptionAPI(blockchain);
        encryptionAPI.setDefaultCredentials("testUser", userKeyPair);

        EncryptionConfig config = EncryptionConfig.createHighSecurityConfig();
        searchEngine = new SearchFrameworkEngine(config);
        searchEngine.setBlockchain(blockchain);  // Required for off-chain content search
        // NOTE: Do NOT pre-index here. Each test will create blocks and index them.
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
    @DisplayName("INCLUDE_OFFCHAIN finds unique word in off-chain file content (BUG FIX)")
    void testIncludeOffchain_FindsOffChainFileContent() throws Exception {
        // Arrange: Create off-chain block
        Block block = createOffChainBlockWithPublicKeyword();

        // Index block with password (sync)
        searchEngine.indexFilteredBlocks(List.of(block), TEST_PASSWORD, userKeyPair.getPrivate());
        waitForIndexing();

        // Act: Search for UNIQUE off-chain word
        SearchFrameworkEngine.SearchResult results = searchEngine.searchExhaustiveOffChain(
            UNIQUE_OFFCHAIN_WORD,
            TEST_PASSWORD,
            10
        );

        // Assert: The unique word ONLY exists in off-chain file
        // If INCLUDE_OFFCHAIN searches file content correctly, it should find the block
        assertTrue(results.getResultCount() > 0,
            "BUG FIX: INCLUDE_OFFCHAIN should find unique word in off-chain file content. " +
            "Word '" + UNIQUE_OFFCHAIN_WORD + "' only exists in off-chain file.");

        boolean foundCorrectBlock = results.getResults().stream()
            .anyMatch(r -> r.getBlockHash().equals(block.getHash()));
        assertTrue(foundCorrectBlock,
            "The correct off-chain block should be found");
    }

    @Test
    @DisplayName("Public keywords searchable in off-chain blocks with FAST_ONLY")
    void testPublicKeywords_SearchableInOffChainBlocks() throws Exception {
        // Arrange
        Block block = createOffChainBlockWithPublicKeyword();

        // Index block (public keywords don't need password)
        searchEngine.indexFilteredBlocks(List.of(block), "", userKeyPair.getPrivate());
        waitForIndexing();

        // Act: Search for public keyword with FAST_ONLY
        SearchFrameworkEngine.SearchResult results =
            searchEngine.searchPublicOnly(PUBLIC_KEYWORD, 10);

        // Assert
        assertTrue(results.getResultCount() > 0,
            "Public keywords should be searchable in off-chain blocks");

        boolean foundCorrectBlock = results.getResults().stream()
            .anyMatch(r -> r.getBlockHash().equals(block.getHash()));
        assertTrue(foundCorrectBlock,
            "The correct off-chain block should be found by public keyword");
    }

    @Test
    @DisplayName("Public keywords searchable with INCLUDE_OFFCHAIN search")
    void testPublicKeywords_SearchableWithPassword() throws Exception {
        // Arrange
        Block block = createOffChainBlockWithPublicKeyword();

        // Index block with password
        searchEngine.indexFilteredBlocks(List.of(block), TEST_PASSWORD, userKeyPair.getPrivate());
        waitForIndexing();

        // Act: Search for public keyword with INCLUDE_OFFCHAIN
        SearchFrameworkEngine.SearchResult results = searchEngine.searchExhaustiveOffChain(
            PUBLIC_KEYWORD,
            TEST_PASSWORD,
            10
        );

        // Assert: INCLUDE_OFFCHAIN should find keywords too
        assertTrue(results.getResultCount() > 0,
            "INCLUDE_OFFCHAIN should find public keywords. Got: " + results.getResultCount());

        boolean foundCorrectBlock = results.getResults().stream()
            .anyMatch(r -> r.getBlockHash().equals(block.getHash()));
        assertTrue(foundCorrectBlock,
            "The correct off-chain block should be found by public keyword");
    }

    // ===== SECURITY TESTS =====

    @Test
    @DisplayName("SECURITY: Off-chain file content NOT found without password")
    void testSecurity_OffChainContentNotFound_WithoutPassword() throws Exception {
        // Arrange
        Block block = createOffChainBlockWithPublicKeyword();

        // Index WITHOUT password (only public keywords indexed)
        searchEngine.indexFilteredBlocks(List.of(block), "", userKeyPair.getPrivate());
        waitForIndexing();

        // Act: Search for off-chain content without password
        SearchFrameworkEngine.SearchResult results = searchEngine.searchExhaustiveOffChain(
            UNIQUE_OFFCHAIN_WORD,
            "",
            10
        );

        // Assert: Off-chain content should NOT be found without password
        boolean foundOffChainBlock = results.getResults().stream()
            .anyMatch(r -> r.getBlockHash().equals(block.getHash()));
        assertFalse(foundOffChainBlock,
            "SECURITY: Off-chain file content should NOT be found without password");
    }

    @Test
    @DisplayName("SECURITY: Off-chain file content NOT found with wrong password")
    void testSecurity_OffChainContentNotFound_WithWrongPassword() throws Exception {
        // Arrange
        Block block = createOffChainBlockWithPublicKeyword();

        // Index with correct password
        searchEngine.indexFilteredBlocks(List.of(block), TEST_PASSWORD, userKeyPair.getPrivate());
        waitForIndexing();

        // Act: Search with WRONG password
        SearchFrameworkEngine.SearchResult results = searchEngine.searchExhaustiveOffChain(
            UNIQUE_OFFCHAIN_WORD,
            WRONG_PASSWORD,
            10
        );

        // Assert: Off-chain content should NOT be found with wrong password
        boolean foundOffChainBlock = results.getResults().stream()
            .anyMatch(r -> r.getBlockHash().equals(block.getHash()));
        assertFalse(foundOffChainBlock,
            "SECURITY: Off-chain file content should NOT be found with wrong password");
    }

    @Test
    @DisplayName("SECURITY: Block placeholder NOT equal to off-chain content")
    void testSecurity_PlaceholderNotEqualToOffChainContent() throws Exception {
        // Arrange
        Block block = createOffChainBlockWithPublicKeyword();

        // Assert: The placeholder in block.data should NOT contain the unique word
        String blockData = block.getData();
        assertFalse(blockData.contains(UNIQUE_OFFCHAIN_WORD),
            "Block placeholder should NOT contain off-chain unique word (it's encrypted)");

        // The unique word only exists in the off-chain file
        assertFalse(BLOCK_PLACEHOLDER.contains(UNIQUE_OFFCHAIN_WORD),
            "Placeholder constant should NOT contain off-chain unique word");
    }

    // ===== BLOCK VERIFICATION TESTS =====

    @Test
    @DisplayName("Off-chain block has correct structure")
    void testOffChainBlock_HasCorrectStructure() throws Exception {
        // Arrange & Act
        Block block = createOffChainBlockWithPublicKeyword();

        // Assert: Block structure
        assertNotNull(block, "Block should be created");
        assertTrue(block.isDataEncrypted(), "Block should be encrypted");
        assertTrue(block.hasOffChainData(), "Block should have off-chain data");

        // Assert: manualKeywords contains public keyword
        String manualKeywords = block.getManualKeywords();
        assertNotNull(manualKeywords, "manualKeywords should not be null");
        assertTrue(manualKeywords.toLowerCase().contains(PUBLIC_KEYWORD.toLowerCase()),
            "manualKeywords should contain public keyword. Got: " + manualKeywords);
    }

    @Test
    @DisplayName("Block hash integrity preserved after INCLUDE_OFFCHAIN search")
    void testBlockHashIntegrity_PreservedAfterSearch() throws Exception {
        // Arrange
        Block block = createOffChainBlockWithPublicKeyword();
        String originalHash = block.getHash();
        Long originalBlockNumber = block.getBlockNumber();

        // Index block with password
        searchEngine.indexFilteredBlocks(List.of(block), TEST_PASSWORD, userKeyPair.getPrivate());
        waitForIndexing();

        // Act: Search using searchExhaustiveOffChain (INCLUDE_OFFCHAIN level)
        SearchFrameworkEngine.SearchResult results = searchEngine.searchExhaustiveOffChain(
            PUBLIC_KEYWORD,
            TEST_PASSWORD,
            10
        );

        // Assert: Search found the correct block
        assertTrue(results.getResultCount() > 0,
            "INCLUDE_OFFCHAIN should find public keyword. Results: " + results.getResultCount());

        boolean foundCorrectBlock = results.getResults().stream()
            .anyMatch(r -> r.getBlockHash().equals(originalHash));
        assertTrue(foundCorrectBlock,
            "Search should find the correct block by public keyword");

        // Assert: Block integrity preserved
        Block retrievedBlock = blockchain.getBlock(originalBlockNumber);
        assertNotNull(retrievedBlock, "Block should be retrievable");
        assertEquals(originalHash, retrievedBlock.getHash(),
            "Block hash should be preserved after search");
        assertTrue(retrievedBlock.hasOffChainData(),
            "Block should still have off-chain data");
    }

    // ===== EDGE CASES =====

    @Test
    @DisplayName("Search for non-existent word in off-chain returns empty")
    void testSearchNonExistentWord_ReturnsEmpty() throws Exception {
        // Arrange
        Block block = createOffChainBlockWithPublicKeyword();

        // Index block with password
        searchEngine.indexFilteredBlocks(List.of(block), TEST_PASSWORD, userKeyPair.getPrivate());
        waitForIndexing();

        // Act
        SearchFrameworkEngine.SearchResult results = searchEngine.searchExhaustiveOffChain(
            "NONEXISTENT_WORD_XYZ999",
            TEST_PASSWORD,
            10
        );

        // Assert
        assertEquals(0, results.getResultCount(),
            "Search for non-existent word should return 0 results");
    }

    @Test
    @DisplayName("SearchLevel is correctly set to INCLUDE_OFFCHAIN")
    void testSearchLevel_IsIncludeOffChain() throws Exception {
        // Arrange
        Block block = createOffChainBlockWithPublicKeyword();

        // Index block with password
        searchEngine.indexFilteredBlocks(List.of(block), TEST_PASSWORD, userKeyPair.getPrivate());
        waitForIndexing();

        // Act
        SearchFrameworkEngine.SearchResult results = searchEngine.searchExhaustiveOffChain(
            PUBLIC_KEYWORD,
            TEST_PASSWORD,
            10
        );

        // Assert
        assertEquals(SearchLevel.INCLUDE_OFFCHAIN, results.getSearchLevel(),
            "Search level should be INCLUDE_OFFCHAIN");
    }

    // ===== PRIVATE KEYWORD TESTS =====

    @Test
    @DisplayName("Private keyword found with correct password")
    void testPrivateKeyword_FoundWithCorrectPassword() throws Exception {
        // Arrange
        Block block = createOffChainBlockWithBothKeywords();

        // Index block with password (required to index private keywords)
        searchEngine.indexFilteredBlocks(List.of(block), TEST_PASSWORD, userKeyPair.getPrivate());
        waitForIndexing();

        // Act: Search for private keyword with correct password
        SearchFrameworkEngine.SearchResult results = searchEngine.searchExhaustiveOffChain(
            PRIVATE_KEYWORD,
            TEST_PASSWORD,
            10
        );

        // Assert: Private keyword should be found
        assertTrue(results.getResultCount() > 0,
            "Private keyword should be found with correct password. Got: " + results.getResultCount());

        boolean foundCorrectBlock = results.getResults().stream()
            .anyMatch(r -> r.getBlockHash().equals(block.getHash()));
        assertTrue(foundCorrectBlock,
            "The correct block should be found by private keyword");
    }

    @Test
    @DisplayName("SECURITY: Private keyword NOT found without password")
    void testPrivateKeyword_NotFoundWithoutPassword() throws Exception {
        // Arrange
        Block block = createOffChainBlockWithBothKeywords();

        // Index block WITHOUT password (private keywords not accessible)
        searchEngine.indexFilteredBlocks(List.of(block), "", userKeyPair.getPrivate());
        waitForIndexing();

        // Act: Search for private keyword without password
        SearchFrameworkEngine.SearchResult results = searchEngine.searchExhaustiveOffChain(
            PRIVATE_KEYWORD,
            "",
            10
        );

        // Assert: Private keyword should NOT be found without password
        boolean foundBlock = results.getResults().stream()
            .anyMatch(r -> r.getBlockHash().equals(block.getHash()));
        assertFalse(foundBlock,
            "SECURITY: Private keyword should NOT be found without password");
    }

    @Test
    @DisplayName("SECURITY: Private keyword NOT found with wrong password")
    void testPrivateKeyword_NotFoundWithWrongPassword() throws Exception {
        // Arrange
        Block block = createOffChainBlockWithBothKeywords();

        // Index block with correct password
        searchEngine.indexFilteredBlocks(List.of(block), TEST_PASSWORD, userKeyPair.getPrivate());
        waitForIndexing();

        // Act: Search for private keyword with WRONG password
        SearchFrameworkEngine.SearchResult results = searchEngine.searchExhaustiveOffChain(
            PRIVATE_KEYWORD,
            WRONG_PASSWORD,
            10
        );

        // Assert: Private keyword should NOT be found with wrong password
        boolean foundBlock = results.getResults().stream()
            .anyMatch(r -> r.getBlockHash().equals(block.getHash()));
        assertFalse(foundBlock,
            "SECURITY: Private keyword should NOT be found with wrong password");
    }

    @Test
    @DisplayName("Both public and private keywords work together in same block")
    void testBothKeywords_WorkTogetherInSameBlock() throws Exception {
        // Arrange
        Block block = createOffChainBlockWithBothKeywords();

        // Index block with password
        searchEngine.indexFilteredBlocks(List.of(block), TEST_PASSWORD, userKeyPair.getPrivate());
        waitForIndexing();

        // Act: Search for both keywords
        SearchFrameworkEngine.SearchResult publicResults = searchEngine.searchExhaustiveOffChain(
            PUBLIC_KEYWORD,
            TEST_PASSWORD,
            10
        );
        SearchFrameworkEngine.SearchResult privateResults = searchEngine.searchExhaustiveOffChain(
            PRIVATE_KEYWORD,
            TEST_PASSWORD,
            10
        );

        // Assert: Both should find the same block
        assertTrue(publicResults.getResultCount() > 0,
            "Public keyword should find results");
        assertTrue(privateResults.getResultCount() > 0,
            "Private keyword should find results");

        boolean publicFoundBlock = publicResults.getResults().stream()
            .anyMatch(r -> r.getBlockHash().equals(block.getHash()));
        boolean privateFoundBlock = privateResults.getResults().stream()
            .anyMatch(r -> r.getBlockHash().equals(block.getHash()));

        assertTrue(publicFoundBlock, "Public keyword should find the block");
        assertTrue(privateFoundBlock, "Private keyword should find the block");
    }

    // ===== HELPER METHODS =====

    private Block createOffChainBlockWithPublicKeyword() throws Exception {
        // Create temp file with unique off-chain content
        File tempFile = File.createTempFile("offchain-test-", ".txt");
        tempFile.deleteOnExit();
        Files.writeString(tempFile.toPath(), OFFCHAIN_FILE_CONTENT);

        // Create off-chain block with only PUBLIC keyword
        Block block = encryptionAPI.storeSearchableDataWithOffChainFile(
            BLOCK_PLACEHOLDER,
            Files.readAllBytes(tempFile.toPath()),
            TEST_PASSWORD,
            "text/plain",
            new String[]{"public:" + PUBLIC_KEYWORD},
            null,
            "OFFCHAIN_TEST_CATEGORY"
        );

        assertNotNull(block, "Block should be created");
        assertTrue(block.isDataEncrypted(), "Block MUST be encrypted");
        assertTrue(block.hasOffChainData(), "Block MUST have off-chain data");

        return block;
    }

    private Block createOffChainBlockWithBothKeywords() throws Exception {
        // Create temp file with unique off-chain content
        File tempFile = File.createTempFile("offchain-test-both-", ".txt");
        tempFile.deleteOnExit();
        Files.writeString(tempFile.toPath(), OFFCHAIN_FILE_CONTENT);

        // Create off-chain block with BOTH public and private keywords
        // public: prefix → manualKeywords (searchable without password)
        // no prefix → autoKeywords (requires password)
        Block block = encryptionAPI.storeSearchableDataWithOffChainFile(
            BLOCK_PLACEHOLDER,
            Files.readAllBytes(tempFile.toPath()),
            TEST_PASSWORD,
            "text/plain",
            new String[]{"public:" + PUBLIC_KEYWORD, PRIVATE_KEYWORD},
            null,
            "OFFCHAIN_TEST_CATEGORY"
        );

        assertNotNull(block, "Block should be created");
        assertTrue(block.isDataEncrypted(), "Block MUST be encrypted");
        assertTrue(block.hasOffChainData(), "Block MUST have off-chain data");

        // Verify public keyword is in manualKeywords
        String manualKeywords = block.getManualKeywords();
        assertNotNull(manualKeywords, "manualKeywords should not be null");
        assertTrue(manualKeywords.toLowerCase().contains(PUBLIC_KEYWORD.toLowerCase()),
            "manualKeywords should contain public keyword");

        return block;
    }

    private void waitForIndexing() {
        try {
            IndexingCoordinator.getInstance().waitForCompletion(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
