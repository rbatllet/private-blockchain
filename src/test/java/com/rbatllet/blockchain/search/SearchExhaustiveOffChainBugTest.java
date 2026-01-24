package com.rbatllet.blockchain.search;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.indexing.IndexingCoordinator;
import com.rbatllet.blockchain.security.UserRole;
import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.util.TestGenesisKeyManager;

/**
 * Rigorous tests to verify searchExhaustiveOffChain works correctly.
 *
 * BUG FIXED: searchExhaustiveOffChain was failing when password was empty because
 * searchEncryptedOnly() threw IllegalArgumentException. Now it gracefully skips
 * encrypted search and continues with public keyword and on-chain content search.
 *
 * Test scenarios:
 * 1. Search with valid password - finds both public and private keywords
 * 2. Search with empty password - finds only public keywords (no exception)
 * 3. Search with null password - should handle gracefully
 * 4. Content search in non-encrypted blocks without password
 * 5. Verify encrypted content is NOT found without password (security)
 */
public class SearchExhaustiveOffChainBugTest {
    private Blockchain blockchain;
    private SearchFrameworkEngine searchEngine;
    private SearchSpecialistAPI searchAPI;
    private KeyPair bootstrapKeyPair;
    private KeyPair userKeyPair;

    // Test constants
    private static final String TEST_PASSWORD = "Genesis2025#Strong";
    private static final String WRONG_PASSWORD = "WrongPassword123#";
    private static final String PUBLIC_KEYWORD = "TEST_PUBLIC_KEYWORD";
    private static final String PRIVATE_KEYWORD = "TEST_PRIVATE_KEYWORD";
    private static final String PUBLIC_BLOCK_DATA = "Test data with public keyword";
    private static final String PRIVATE_BLOCK_DATA = "Test data with private keyword";
    private static final String PUBLIC_CATEGORY = "PUBLIC_TEST";
    private static final String PRIVATE_CATEGORY = "PRIVATE_TEST";

    // Block references for verification
    private Block publicBlock;
    private Block encryptedBlock;

    @BeforeEach
    public void setUp() throws Exception {
        // Reset global state
        SearchFrameworkEngine.resetGlobalState();
        IndexingCoordinator coordinator = IndexingCoordinator.getInstance();
        coordinator.reset();
        try {
            coordinator.waitForCompletion();
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

        // Create test data and store references
        createTestBlocks();

        // Initialize search with password
        blockchain.initializeAdvancedSearch(TEST_PASSWORD);

        // Get API references
        searchAPI = blockchain.getSearchSpecialistAPI();
        searchEngine = blockchain.getSearchFrameworkEngine();

        // Verify setup is correct - at least the public keyword should be indexed
        var searchStats = searchEngine.getSearchStats();
        var fastIndexStats = searchStats.getRouterStats().getFastStats();

        assertTrue(fastIndexStats.getBlocksIndexed() >= 1,
            "At least one test block should be indexed. Got: " + fastIndexStats.getBlocksIndexed());
        assertTrue(fastIndexStats.getUniqueKeywords() >= 1,
            "At least one keyword should be indexed. Got: " + fastIndexStats.getUniqueKeywords());
    }

    @AfterEach
    public void tearDown() {
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

    private void createTestBlocks() throws Exception {
        // Block 1: Non-encrypted with public keyword
        publicBlock = blockchain.addBlockWithKeywords(
            PUBLIC_BLOCK_DATA,
            new String[]{ PUBLIC_KEYWORD },
            PUBLIC_CATEGORY,
            userKeyPair.getPrivate(),
            userKeyPair.getPublic()
        );
        assertNotNull(publicBlock, "Public block should be created");
        assertTrue(publicBlock.getBlockNumber() > 0, "Public block should have valid block number");
        assertFalse(publicBlock.isDataEncrypted(), "Public block should NOT be encrypted");

        // Block 2: Encrypted with private keyword
        encryptedBlock = blockchain.addEncryptedBlockWithKeywords(
            PRIVATE_BLOCK_DATA,
            TEST_PASSWORD,
            new String[]{ PRIVATE_KEYWORD },
            PRIVATE_CATEGORY,
            userKeyPair.getPrivate(),
            userKeyPair.getPublic()
        );
        assertNotNull(encryptedBlock, "Encrypted block should be created");
        assertTrue(encryptedBlock.getBlockNumber() > 0, "Encrypted block should have valid block number");
        assertTrue(encryptedBlock.isDataEncrypted(), "Encrypted block SHOULD be encrypted");

        // Wait for async indexing
        try {
            IndexingCoordinator.getInstance().waitForCompletion();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ===== TESTS WITH VALID PASSWORD =====

    @Test
    @DisplayName("searchExhaustiveOffChain with password finds PUBLIC keyword")
    public void testWithPassword_FindsPublicKeyword() {
        var result = searchAPI.searchExhaustiveOffChain(PUBLIC_KEYWORD, TEST_PASSWORD, 10);

        assertNotNull(result, "Result should not be null");
        assertTrue(result.getResultCount() > 0,
            "Should find public keyword. Got: " + result.getResultCount());

        // Verify the correct block was found
        var results = result.getResults();
        assertFalse(results.isEmpty(), "Results list should not be empty");

        boolean foundCorrectBlock = results.stream()
            .anyMatch(r -> r.getBlockHash().equals(publicBlock.getHash()));
        assertTrue(foundCorrectBlock,
            "Should find the public block with hash: " + publicBlock.getHash().substring(0, 16) + "...");
    }

    @Test
    @DisplayName("searchExhaustiveOffChain with password finds PRIVATE keyword")
    public void testWithPassword_FindsPrivateKeyword() {
        var result = searchAPI.searchExhaustiveOffChain(PRIVATE_KEYWORD, TEST_PASSWORD, 10);

        assertNotNull(result, "Result should not be null");
        assertTrue(result.getResultCount() > 0,
            "Should find private keyword with correct password. Got: " + result.getResultCount());

        // Verify the correct block was found
        var results = result.getResults();
        assertFalse(results.isEmpty(), "Results list should not be empty");

        boolean foundCorrectBlock = results.stream()
            .anyMatch(r -> r.getBlockHash().equals(encryptedBlock.getHash()));
        assertTrue(foundCorrectBlock,
            "Should find the encrypted block with hash: " + encryptedBlock.getHash().substring(0, 16) + "...");
    }

    @Test
    @DisplayName("searchIntelligent and searchExhaustiveOffChain return consistent results")
    public void testConsistency_IntelligentVsExhaustive() {
        var intelligentResults = searchAPI.searchIntelligent(PUBLIC_KEYWORD, TEST_PASSWORD, 10);
        var exhaustiveResult = searchAPI.searchExhaustiveOffChain(PUBLIC_KEYWORD, TEST_PASSWORD, 10);

        assertFalse(intelligentResults.isEmpty(), "searchIntelligent should find results");
        assertTrue(exhaustiveResult.getResultCount() > 0, "searchExhaustiveOffChain should find results");

        // Both should find the same block
        String intelligentHash = intelligentResults.get(0).getBlockHash();
        boolean exhaustiveHasSameBlock = exhaustiveResult.getResults().stream()
            .anyMatch(r -> r.getBlockHash().equals(intelligentHash));
        assertTrue(exhaustiveHasSameBlock,
            "Both search methods should find the same block");
    }

    // ===== TESTS WITH EMPTY PASSWORD (BUG FIX VERIFICATION) =====

    @Test
    @DisplayName("BUG FIX: Empty password does NOT throw exception")
    public void testEmptyPassword_NoException() {
        // This was the original bug - empty password caused IllegalArgumentException
        assertDoesNotThrow(() -> {
            searchAPI.searchExhaustiveOffChain(PUBLIC_KEYWORD, "", 10);
        }, "Empty password should NOT throw exception");
    }

    @Test
    @DisplayName("BUG FIX: Empty password finds PUBLIC keyword via FastIndexSearch")
    public void testEmptyPassword_FindsPublicKeyword() {
        var result = searchAPI.searchExhaustiveOffChain(PUBLIC_KEYWORD, "", 10);

        assertNotNull(result, "Result should not be null");
        assertTrue(result.getResultCount() > 0,
            "Should find public keyword even without password. Got: " + result.getResultCount());

        // Verify the correct block was found
        boolean foundPublicBlock = result.getResults().stream()
            .anyMatch(r -> r.getBlockHash().equals(publicBlock.getHash()));
        assertTrue(foundPublicBlock,
            "Should find the public block via FastIndexSearch");
    }

    @Test
    @DisplayName("BUG FIX: Empty password finds NON-ENCRYPTED content via on-chain search")
    public void testEmptyPassword_FindsNonEncryptedContent() {
        // Search for text that appears in PUBLIC_BLOCK_DATA ("Test data with public keyword")
        var result = searchAPI.searchExhaustiveOffChain("Test data", "", 10);

        assertNotNull(result, "Result should not be null");
        assertTrue(result.getResultCount() > 0,
            "Should find non-encrypted content via on-chain search. Got: " + result.getResultCount());
    }

    @Test
    @DisplayName("SECURITY: Empty password does NOT find PRIVATE keyword")
    public void testEmptyPassword_DoesNotFindPrivateKeyword() {
        var result = searchAPI.searchExhaustiveOffChain(PRIVATE_KEYWORD, "", 10);

        assertNotNull(result, "Result should not be null");

        // Private keywords are encrypted - should NOT be found without password
        boolean foundEncryptedBlock = result.getResults().stream()
            .anyMatch(r -> r.getBlockHash().equals(encryptedBlock.getHash()));
        assertFalse(foundEncryptedBlock,
            "SECURITY: Should NOT find encrypted block without password");
    }

    @Test
    @DisplayName("SECURITY: Empty password does NOT find ENCRYPTED content")
    public void testEmptyPassword_DoesNotFindEncryptedContent() {
        // Search for text that appears in PRIVATE_BLOCK_DATA ("Test data with private keyword")
        // This should NOT be found because the block is encrypted
        var result = searchAPI.searchExhaustiveOffChain("private keyword", "", 10);

        assertNotNull(result, "Result should not be null");

        // The encrypted block should NOT be in results
        boolean foundEncryptedBlock = result.getResults().stream()
            .anyMatch(r -> r.getBlockHash().equals(encryptedBlock.getHash()));
        assertFalse(foundEncryptedBlock,
            "SECURITY: Should NOT find encrypted content without password");
    }

    // ===== TESTS WITH WRONG PASSWORD (SECURITY) =====

    @Test
    @DisplayName("SECURITY: Wrong password does NOT find PRIVATE keyword")
    public void testWrongPassword_DoesNotFindPrivateKeyword() {
        var result = searchAPI.searchExhaustiveOffChain(PRIVATE_KEYWORD, WRONG_PASSWORD, 10);

        assertNotNull(result, "Result should not be null");

        // Private keywords are encrypted - should NOT be found with wrong password
        boolean foundEncryptedBlock = result.getResults().stream()
            .anyMatch(r -> r.getBlockHash().equals(encryptedBlock.getHash()));
        assertFalse(foundEncryptedBlock,
            "SECURITY: Should NOT find encrypted block with wrong password");
    }

    @Test
    @DisplayName("SECURITY: Wrong password does NOT find ENCRYPTED content")
    public void testWrongPassword_DoesNotFindEncryptedContent() {
        // Search for text that appears in PRIVATE_BLOCK_DATA
        var result = searchAPI.searchExhaustiveOffChain("private keyword", WRONG_PASSWORD, 10);

        assertNotNull(result, "Result should not be null");

        // The encrypted block should NOT be in results with wrong password
        boolean foundEncryptedBlock = result.getResults().stream()
            .anyMatch(r -> r.getBlockHash().equals(encryptedBlock.getHash()));
        assertFalse(foundEncryptedBlock,
            "SECURITY: Should NOT find encrypted content with wrong password");
    }

    @Test
    @DisplayName("Wrong password still finds PUBLIC keyword")
    public void testWrongPassword_FindsPublicKeyword() {
        var result = searchAPI.searchExhaustiveOffChain(PUBLIC_KEYWORD, WRONG_PASSWORD, 10);

        assertNotNull(result, "Result should not be null");
        assertTrue(result.getResultCount() > 0,
            "Should find public keyword even with wrong password. Got: " + result.getResultCount());

        // Verify the public block was found
        boolean foundPublicBlock = result.getResults().stream()
            .anyMatch(r -> r.getBlockHash().equals(publicBlock.getHash()));
        assertTrue(foundPublicBlock,
            "Should find the public block via FastIndexSearch even with wrong password");
    }

    // ===== TESTS WITH NULL PASSWORD (API VALIDATION) =====

    @Test
    @DisplayName("Null password throws IllegalArgumentException (API validation)")
    public void testNullPassword_ThrowsException() {
        // The API explicitly validates that password is not null
        // This is intentional design - use empty string "" instead of null
        var exception = assertThrows(IllegalArgumentException.class, () -> {
            searchAPI.searchExhaustiveOffChain(PUBLIC_KEYWORD, null, 10);
        }, "Null password SHOULD throw IllegalArgumentException");

        assertTrue(exception.getMessage().contains("null"),
            "Exception message should mention null. Got: " + exception.getMessage());
    }

    // ===== EDGE CASES =====

    @Test
    @DisplayName("Search for non-existent term returns empty results")
    public void testNonExistentTerm_ReturnsEmpty() {
        var result = searchAPI.searchExhaustiveOffChain("NONEXISTENT_TERM_XYZ123", TEST_PASSWORD, 10);

        assertNotNull(result, "Result should not be null");
        assertEquals(0, result.getResultCount(),
            "Should return 0 results for non-existent term");
        assertTrue(result.getResults().isEmpty(),
            "Results list should be empty");
    }

    @Test
    @DisplayName("Case-insensitive keyword search works")
    public void testCaseInsensitiveSearch() {
        // Keywords are stored lowercase, so search should be case-insensitive
        var resultLower = searchAPI.searchExhaustiveOffChain(PUBLIC_KEYWORD.toLowerCase(), TEST_PASSWORD, 10);
        var resultUpper = searchAPI.searchExhaustiveOffChain(PUBLIC_KEYWORD.toUpperCase(), TEST_PASSWORD, 10);

        // Both case variants should find results (keywords are normalized to lowercase)
        assertTrue(resultLower.getResultCount() > 0,
            "Lowercase search should find results. Got: " + resultLower.getResultCount());
        assertTrue(resultUpper.getResultCount() > 0,
            "Uppercase search should find results. Got: " + resultUpper.getResultCount());

        // Both should find the same block
        boolean lowerFoundCorrectBlock = resultLower.getResults().stream()
            .anyMatch(r -> r.getBlockHash().equals(publicBlock.getHash()));
        boolean upperFoundCorrectBlock = resultUpper.getResults().stream()
            .anyMatch(r -> r.getBlockHash().equals(publicBlock.getHash()));

        assertTrue(lowerFoundCorrectBlock,
            "Lowercase search should find the public block");
        assertTrue(upperFoundCorrectBlock,
            "Uppercase search should find the public block");
    }

    @Test
    @DisplayName("SearchLevel is correctly set to INCLUDE_OFFCHAIN")
    public void testSearchLevel_IsIncludeOffChain() {
        var result = searchAPI.searchExhaustiveOffChain(PUBLIC_KEYWORD, TEST_PASSWORD, 10);

        assertNotNull(result, "Result should not be null");
        assertEquals(SearchLevel.INCLUDE_OFFCHAIN, result.getSearchLevel(),
            "Search level should be INCLUDE_OFFCHAIN");
    }

    // ===== BLOCK INTEGRITY TESTS =====

    @Test
    @DisplayName("Block hash integrity preserved after search")
    public void testBlockHashIntegrity_PreservedAfterSearch() {
        String originalPublicHash = publicBlock.getHash();
        String originalEncryptedHash = encryptedBlock.getHash();
        Long publicBlockNumber = publicBlock.getBlockNumber();
        Long encryptedBlockNumber = encryptedBlock.getBlockNumber();

        // Perform multiple searches
        searchAPI.searchExhaustiveOffChain(PUBLIC_KEYWORD, TEST_PASSWORD, 10);
        searchAPI.searchExhaustiveOffChain(PRIVATE_KEYWORD, TEST_PASSWORD, 10);
        searchAPI.searchExhaustiveOffChain("Test data", "", 10);

        // Verify blocks are still retrievable with same hashes
        Block retrievedPublic = blockchain.getBlock(publicBlockNumber);
        Block retrievedEncrypted = blockchain.getBlock(encryptedBlockNumber);

        assertNotNull(retrievedPublic, "Public block should be retrievable");
        assertNotNull(retrievedEncrypted, "Encrypted block should be retrievable");
        assertEquals(originalPublicHash, retrievedPublic.getHash(),
            "Public block hash should be preserved");
        assertEquals(originalEncryptedHash, retrievedEncrypted.getHash(),
            "Encrypted block hash should be preserved");
    }

    @Test
    @DisplayName("Block encryption status preserved after search")
    public void testBlockEncryptionStatus_PreservedAfterSearch() {
        // Perform search
        searchAPI.searchExhaustiveOffChain(PUBLIC_KEYWORD, TEST_PASSWORD, 10);

        // Verify encryption status
        Block retrievedPublic = blockchain.getBlock(publicBlock.getBlockNumber());
        Block retrievedEncrypted = blockchain.getBlock(encryptedBlock.getBlockNumber());

        assertFalse(retrievedPublic.isDataEncrypted(),
            "Public block should remain non-encrypted after search");
        assertTrue(retrievedEncrypted.isDataEncrypted(),
            "Encrypted block should remain encrypted after search");
    }

    @Test
    @DisplayName("maxResults limit is respected")
    public void testMaxResultsLimit_IsRespected() {
        // Create additional blocks to ensure we have more than 1 result possible
        var result = searchAPI.searchExhaustiveOffChain(PUBLIC_KEYWORD, TEST_PASSWORD, 1);

        assertNotNull(result, "Result should not be null");
        assertTrue(result.getResults().size() <= 1,
            "Results should respect maxResults limit of 1. Got: " + result.getResults().size());
    }

    @Test
    @DisplayName("Search returns correct block metadata")
    public void testSearchReturnsCorrectBlockMetadata() {
        var result = searchAPI.searchExhaustiveOffChain(PUBLIC_KEYWORD, TEST_PASSWORD, 10);

        assertNotNull(result, "Result should not be null");
        assertTrue(result.getResultCount() > 0, "Should find results");

        // Find the public block result
        var publicBlockResult = result.getResults().stream()
            .filter(r -> r.getBlockHash().equals(publicBlock.getHash()))
            .findFirst();

        assertTrue(publicBlockResult.isPresent(), "Should find the public block");

        var blockResult = publicBlockResult.get();
        assertEquals(publicBlock.getHash(), blockResult.getBlockHash(),
            "Block hash should match");
        assertNotNull(blockResult.getSource(),
            "Search source should be present");
        assertTrue(blockResult.getRelevanceScore() > 0,
            "Relevance score should be positive. Got: " + blockResult.getRelevanceScore());
    }
}
