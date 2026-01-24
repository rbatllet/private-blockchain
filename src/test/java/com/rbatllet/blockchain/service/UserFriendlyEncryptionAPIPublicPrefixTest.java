package com.rbatllet.blockchain.service;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.security.UserRole;
import com.rbatllet.blockchain.config.EncryptionConfig;
import com.rbatllet.blockchain.util.TestGenesisKeyManager;
import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.search.SearchSpecialistAPI;
import com.rbatllet.blockchain.search.SearchFrameworkEngine;
import com.rbatllet.blockchain.indexing.IndexingCoordinator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyPair;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RIGOROUS TEST: "public:" prefix functionality in UserFriendlyEncryptionAPI.
 *
 * CRITICAL BEHAVIOR:
 * - Keywords with "public:" prefix are stored unencrypted in manualKeywords
 * - Keywords without prefix are encrypted and stored in autoKeywords
 * - searchPublic() finds public keywords WITHOUT requiring password
 * - searchPublic() does NOT find private keywords
 * - searchSecure() finds BOTH public and private keywords (requires password)
 *
 * Test coverage:
 * 1. Basic public prefix functionality
 * 2. Multiple public/private keywords
 * 3. Search behavior verification
 * 4. Non-encrypted block handling
 * 5. Edge cases (case sensitivity, special characters)
 * 6. Error handling
 */
@DisplayName("🔑 RIGOROUS: UserFriendlyEncryptionAPI Public Prefix Tests")
public class UserFriendlyEncryptionAPIPublicPrefixTest {

    private static final Logger logger = LoggerFactory.getLogger(UserFriendlyEncryptionAPIPublicPrefixTest.class);

    // Test constants
    private static final String TEST_PASSWORD = "TestPassword123456#Secure";
    private static final String WRONG_PASSWORD = "WrongPassword789#Invalid";

    private Blockchain blockchain;
    private KeyPair bootstrapKeyPair;
    private KeyPair userKeyPair;
    private UserFriendlyEncryptionAPI api;

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

        api = new UserFriendlyEncryptionAPI(blockchain);
        api.setDefaultCredentials("testUser", userKeyPair);
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

    // ========== BASIC FUNCTIONALITY TESTS ==========

    @Nested
    @DisplayName("📦 Basic Public Prefix Functionality")
    class BasicPublicPrefixTests {

        @Test
        @DisplayName("public: prefix keywords are stored unencrypted in manualKeywords")
        void testPublicPrefixStoredInManualKeywords() {
            // Arrange
            String publicKeyword = "PUBLIC_BASIC_" + System.currentTimeMillis();
            String privateKeyword = "PRIVATE_BASIC_" + System.currentTimeMillis();

            // Act
            Block block = api.createBlockWithOptions(
                "Test content for basic prefix test",
                new UserFriendlyEncryptionAPI.BlockCreationOptions()
                    .withEncryption(true)
                    .withPassword(TEST_PASSWORD)
                    .withKeywords(new String[]{"public:" + publicKeyword, privateKeyword})
                    .withCategory("BASIC_TEST")
            );

            // Assert: Block created correctly
            assertNotNull(block, "Block should be created");
            assertTrue(block.isDataEncrypted(), "Block should be encrypted");

            // Assert: manualKeywords contains public keyword with prefix
            String manualKeywords = block.getManualKeywords();
            assertNotNull(manualKeywords, "manualKeywords should not be null");
            assertTrue(manualKeywords.contains("public:" + publicKeyword.toLowerCase()),
                "manualKeywords should contain 'public:" + publicKeyword.toLowerCase() + "'");

            // Assert: autoKeywords contains encrypted data (private keywords)
            String autoKeywords = block.getAutoKeywords();
            assertNotNull(autoKeywords, "autoKeywords should not be null");
            assertTrue(autoKeywords.length() > 0, "autoKeywords should contain encrypted data");

            // Assert: private keyword is NOT visible in manualKeywords
            assertFalse(manualKeywords.contains(privateKeyword.toLowerCase()),
                "manualKeywords should NOT contain private keyword in plain text");

            logger.info("✅ manualKeywords: '{}'", manualKeywords);
            logger.info("✅ autoKeywords: [encrypted, length={}]", autoKeywords.length());
        }

        @Test
        @DisplayName("Multiple public: prefix keywords are all stored correctly")
        void testMultiplePublicPrefixKeywords() {
            // Arrange
            String public1 = "PUBLIC_ONE_" + System.currentTimeMillis();
            String public2 = "PUBLIC_TWO_" + System.currentTimeMillis();
            String public3 = "PUBLIC_THREE_" + System.currentTimeMillis();

            // Act
            Block block = api.createBlockWithOptions(
                "Test content with multiple public keywords",
                new UserFriendlyEncryptionAPI.BlockCreationOptions()
                    .withEncryption(true)
                    .withPassword(TEST_PASSWORD)
                    .withKeywords(new String[]{
                        "public:" + public1,
                        "public:" + public2,
                        "public:" + public3
                    })
                    .withCategory("MULTI_PUBLIC")
            );

            // Assert
            assertNotNull(block, "Block should be created");
            String manualKeywords = block.getManualKeywords();
            assertNotNull(manualKeywords, "manualKeywords should not be null");

            // All public keywords should be in manualKeywords
            assertTrue(manualKeywords.contains("public:" + public1.toLowerCase()),
                "Should contain first public keyword");
            assertTrue(manualKeywords.contains("public:" + public2.toLowerCase()),
                "Should contain second public keyword");
            assertTrue(manualKeywords.contains("public:" + public3.toLowerCase()),
                "Should contain third public keyword");

            logger.info("✅ All 3 public keywords stored: {}", manualKeywords);
        }

        @Test
        @DisplayName("Mixed public and private keywords are separated correctly")
        void testMixedPublicPrivateKeywords() {
            // Arrange
            String public1 = "MIXED_PUBLIC_" + System.currentTimeMillis();
            String private1 = "MIXED_PRIVATE_" + System.currentTimeMillis();
            String public2 = "MIXED_PUBLIC2_" + System.currentTimeMillis();
            String private2 = "MIXED_PRIVATE2_" + System.currentTimeMillis();

            // Act
            Block block = api.createBlockWithOptions(
                "Test content with mixed keywords",
                new UserFriendlyEncryptionAPI.BlockCreationOptions()
                    .withEncryption(true)
                    .withPassword(TEST_PASSWORD)
                    .withKeywords(new String[]{
                        "public:" + public1,
                        private1,
                        "public:" + public2,
                        private2
                    })
                    .withCategory("MIXED_KEYWORDS")
            );

            // Assert
            assertNotNull(block, "Block should be created");
            String manualKeywords = block.getManualKeywords();
            String autoKeywords = block.getAutoKeywords();

            assertNotNull(manualKeywords, "manualKeywords should not be null");
            assertNotNull(autoKeywords, "autoKeywords should not be null");

            // Public keywords should be in manualKeywords
            assertTrue(manualKeywords.contains("public:" + public1.toLowerCase()),
                "Should contain first public keyword");
            assertTrue(manualKeywords.contains("public:" + public2.toLowerCase()),
                "Should contain second public keyword");

            // Private keywords should NOT be visible in plain text
            assertFalse(manualKeywords.contains(private1.toLowerCase()),
                "Private keyword 1 should NOT be in manualKeywords");
            assertFalse(manualKeywords.contains(private2.toLowerCase()),
                "Private keyword 2 should NOT be in manualKeywords");

            logger.info("✅ Public keywords in manualKeywords: {}", manualKeywords);
            logger.info("✅ Private keywords encrypted in autoKeywords (length={})", autoKeywords.length());
        }
    }

    // ========== SEARCH BEHAVIOR TESTS ==========

    @Nested
    @DisplayName("🔍 Search Behavior Verification")
    class SearchBehaviorTests {

        @Test
        @DisplayName("searchPublic finds public keywords without password")
        void testSearchPublicFindsPublicKeywords() {
            // Arrange
            String publicKeyword = "SEARCHABLE_PUBLIC_" + System.currentTimeMillis();
            String privateKeyword = "SEARCHABLE_PRIVATE_" + System.currentTimeMillis();

            Block block = api.createBlockWithOptions(
                "Content for search test",
                new UserFriendlyEncryptionAPI.BlockCreationOptions()
                    .withEncryption(true)
                    .withPassword(TEST_PASSWORD)
                    .withKeywords(new String[]{"public:" + publicKeyword, privateKeyword})
                    .withCategory("SEARCH_TEST")
            );
            assertNotNull(block, "Block should be created");

            // Wait for indexing
            waitForIndexing();

            // Act: Search with searchPublic (no password needed)
            EncryptionConfig config = new EncryptionConfig();
            SearchSpecialistAPI searchAPI = new SearchSpecialistAPI(
                blockchain, TEST_PASSWORD, userKeyPair.getPrivate(), config
            );

            List<SearchFrameworkEngine.EnhancedSearchResult> publicResults =
                searchAPI.searchPublic(publicKeyword, 10);

            // Assert
            assertFalse(publicResults.isEmpty(),
                "searchPublic should find public keyword '" + publicKeyword + "'");
            assertEquals(block.getHash(), publicResults.get(0).getBlockHash(),
                "Found block should match created block");

            logger.info("✅ searchPublic found {} result(s) for '{}'",
                publicResults.size(), publicKeyword);
        }

        @Test
        @DisplayName("searchPublic does NOT find private keywords")
        void testSearchPublicDoesNotFindPrivateKeywords() {
            // Arrange
            String publicKeyword = "VISIBLE_" + System.currentTimeMillis();
            String privateKeyword = "HIDDEN_" + System.currentTimeMillis();

            Block block = api.createBlockWithOptions(
                "Content with hidden keyword",
                new UserFriendlyEncryptionAPI.BlockCreationOptions()
                    .withEncryption(true)
                    .withPassword(TEST_PASSWORD)
                    .withKeywords(new String[]{"public:" + publicKeyword, privateKeyword})
                    .withCategory("PRIVACY_TEST")
            );
            assertNotNull(block, "Block should be created");

            // Wait for indexing
            waitForIndexing();

            // Act: Search for private keyword with searchPublic
            EncryptionConfig config = new EncryptionConfig();
            SearchSpecialistAPI searchAPI = new SearchSpecialistAPI(
                blockchain, TEST_PASSWORD, userKeyPair.getPrivate(), config
            );

            List<SearchFrameworkEngine.EnhancedSearchResult> results =
                searchAPI.searchPublic(privateKeyword, 10);

            // Assert
            assertTrue(results.isEmpty(),
                "searchPublic should NOT find private keyword '" + privateKeyword + "'");

            logger.info("✅ searchPublic correctly returned 0 results for private keyword '{}'",
                privateKeyword);
        }

        @Test
        @DisplayName("searchSecure finds both public and private keywords")
        void testSearchSecureFindsBothKeywordTypes() {
            // Arrange
            String publicKeyword = "SECURE_PUBLIC_" + System.currentTimeMillis();
            String privateKeyword = "SECURE_PRIVATE_" + System.currentTimeMillis();

            Block block = api.createBlockWithOptions(
                "Content for secure search test",
                new UserFriendlyEncryptionAPI.BlockCreationOptions()
                    .withEncryption(true)
                    .withPassword(TEST_PASSWORD)
                    .withKeywords(new String[]{"public:" + publicKeyword, privateKeyword})
                    .withCategory("SECURE_SEARCH")
            );
            assertNotNull(block, "Block should be created");

            // Wait for indexing
            waitForIndexing();

            // Act: Search with searchSecure
            EncryptionConfig config = new EncryptionConfig();
            SearchSpecialistAPI searchAPI = new SearchSpecialistAPI(
                blockchain, TEST_PASSWORD, userKeyPair.getPrivate(), config
            );

            List<SearchFrameworkEngine.EnhancedSearchResult> publicResults =
                searchAPI.searchSecure(publicKeyword, TEST_PASSWORD, 10);
            List<SearchFrameworkEngine.EnhancedSearchResult> privateResults =
                searchAPI.searchSecure(privateKeyword, TEST_PASSWORD, 10);

            // Assert
            assertFalse(publicResults.isEmpty(),
                "searchSecure should find public keyword '" + publicKeyword + "'");
            assertFalse(privateResults.isEmpty(),
                "searchSecure should find private keyword '" + privateKeyword + "'");

            logger.info("✅ searchSecure found public keyword: {} result(s)", publicResults.size());
            logger.info("✅ searchSecure found private keyword: {} result(s)", privateResults.size());
        }

        @Test
        @DisplayName("searchSecure with wrong password does NOT find private keywords")
        void testSearchSecureWithWrongPasswordDoesNotFindPrivate() {
            // Arrange
            String privateKeyword = "WRONGPW_PRIVATE_" + System.currentTimeMillis();

            Block block = api.createBlockWithOptions(
                "Content for wrong password test",
                new UserFriendlyEncryptionAPI.BlockCreationOptions()
                    .withEncryption(true)
                    .withPassword(TEST_PASSWORD)
                    .withKeywords(new String[]{privateKeyword})
                    .withCategory("WRONG_PW_TEST")
            );
            assertNotNull(block, "Block should be created");

            // Wait for indexing
            waitForIndexing();

            // Act: Search with wrong password
            EncryptionConfig config = new EncryptionConfig();
            SearchSpecialistAPI searchAPI = new SearchSpecialistAPI(
                blockchain, WRONG_PASSWORD, userKeyPair.getPrivate(), config
            );

            List<SearchFrameworkEngine.EnhancedSearchResult> results =
                searchAPI.searchSecure(privateKeyword, WRONG_PASSWORD, 10);

            // Assert
            assertTrue(results.isEmpty(),
                "searchSecure with wrong password should NOT find private keyword");

            logger.info("✅ Wrong password correctly prevented finding private keyword");
        }
    }

    // ========== NON-ENCRYPTED BLOCK TESTS ==========

    @Nested
    @DisplayName("📝 Non-Encrypted Block Handling")
    class NonEncryptedBlockTests {

        @Test
        @DisplayName("Non-encrypted blocks with keywords are searchable with FAST_ONLY")
        void testNonEncryptedBlockSearchableWithFastOnly() {
            // Arrange
            String keyword = "NON_ENCRYPTED_" + System.currentTimeMillis();

            // Create NON-ENCRYPTED block (as CLI does without --password)
            Block block = blockchain.addBlockWithKeywords(
                "Public content with keyword",
                new String[]{keyword},
                "PUBLIC",
                userKeyPair.getPrivate(),
                userKeyPair.getPublic()
            );

            // Assert block created correctly
            assertNotNull(block, "Block should be created");
            assertFalse(block.isDataEncrypted(), "Block should NOT be encrypted");

            // Verify manualKeywords contains the keyword
            String manualKeywords = block.getManualKeywords();
            assertNotNull(manualKeywords, "manualKeywords should not be null");
            assertTrue(manualKeywords.contains(keyword.toLowerCase()),
                "manualKeywords should contain '" + keyword.toLowerCase() + "'");

            // Wait for indexing
            waitForIndexing();

            // Act: Search with searchPublic
            EncryptionConfig config = new EncryptionConfig();
            SearchSpecialistAPI searchAPI = new SearchSpecialistAPI(
                blockchain, "", userKeyPair.getPrivate(), config
            );

            List<SearchFrameworkEngine.EnhancedSearchResult> results =
                searchAPI.searchPublic(keyword, 10);

            // Assert
            assertFalse(results.isEmpty(),
                "searchPublic (FAST_ONLY) should find keyword from non-encrypted block");

            logger.info("✅ Non-encrypted block keyword found with FAST_ONLY");
        }

        @Test
        @DisplayName("public: prefix is rejected on non-encrypted blocks")
        void testRejectPublicPrefixOnNonEncryptedBlocks() {
            // Arrange
            String keyword = "REJECTED_PREFIX_" + System.currentTimeMillis();

            // Act & Assert: Should throw exception
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> blockchain.addBlockWithKeywords(
                    "Public content",
                    new String[]{"public:" + keyword},
                    "PUBLIC",
                    userKeyPair.getPrivate(),
                    userKeyPair.getPublic()
                ),
                "public: prefix should be rejected on non-encrypted blocks"
            );

            // Verify exception message
            String message = exception.getMessage();
            assertTrue(
                message.contains("public:") && message.contains("non-encrypted"),
                "Exception should mention 'public:' and 'non-encrypted': " + message
            );

            logger.info("✅ Correctly rejected public: prefix on non-encrypted block: {}",
                exception.getMessage());
        }

        @Test
        @DisplayName("Non-encrypted blocks store all keywords in manualKeywords")
        void testNonEncryptedBlocksStoreAllKeywordsPublicly() {
            // Arrange
            String keyword1 = "KEYWORD_ONE_" + System.currentTimeMillis();
            String keyword2 = "KEYWORD_TWO_" + System.currentTimeMillis();

            // Act
            Block block = blockchain.addBlockWithKeywords(
                "Public content with multiple keywords",
                new String[]{keyword1, keyword2},
                "PUBLIC",
                userKeyPair.getPrivate(),
                userKeyPair.getPublic()
            );

            // Assert
            assertNotNull(block, "Block should be created");
            assertFalse(block.isDataEncrypted(), "Block should NOT be encrypted");

            String manualKeywords = block.getManualKeywords();
            assertTrue(manualKeywords.contains(keyword1.toLowerCase()),
                "Should contain keyword1");
            assertTrue(manualKeywords.contains(keyword2.toLowerCase()),
                "Should contain keyword2");

            // For non-encrypted blocks, autoKeywords may contain auto-extracted terms
            // but the explicitly provided keywords should be in manualKeywords
            logger.info("✅ Non-encrypted block stores provided keywords in manualKeywords: {}",
                manualKeywords);
        }
    }

    // ========== EDGE CASES TESTS ==========

    @Nested
    @DisplayName("🔧 Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Keywords are case-insensitive for storage")
        void testKeywordsCaseInsensitive() {
            // Arrange
            String mixedCaseKeyword = "MiXeD_CaSe_KeYwOrD";

            // Act
            Block block = api.createBlockWithOptions(
                "Content with mixed case keyword",
                new UserFriendlyEncryptionAPI.BlockCreationOptions()
                    .withEncryption(true)
                    .withPassword(TEST_PASSWORD)
                    .withKeywords(new String[]{"public:" + mixedCaseKeyword})
                    .withCategory("CASE_TEST")
            );

            // Assert
            assertNotNull(block, "Block should be created");
            String manualKeywords = block.getManualKeywords();
            assertTrue(manualKeywords.contains(mixedCaseKeyword.toLowerCase()),
                "Keyword should be stored in lowercase: " + manualKeywords);

            logger.info("✅ Mixed case keyword stored as lowercase: {}", manualKeywords);
        }

        @Test
        @DisplayName("Special characters in keywords are handled correctly")
        void testSpecialCharactersInKeywords() {
            // Arrange - using allowed special characters
            String specialKeyword = "SPECIAL_keyword-123";

            // Act
            Block block = api.createBlockWithOptions(
                "Content with special characters in keyword",
                new UserFriendlyEncryptionAPI.BlockCreationOptions()
                    .withEncryption(true)
                    .withPassword(TEST_PASSWORD)
                    .withKeywords(new String[]{"public:" + specialKeyword})
                    .withCategory("SPECIAL_CHARS")
            );

            // Assert
            assertNotNull(block, "Block should be created");
            String manualKeywords = block.getManualKeywords();
            assertTrue(manualKeywords.contains(specialKeyword.toLowerCase()),
                "Special characters keyword should be stored");

            // Wait for indexing and verify searchable
            waitForIndexing();

            EncryptionConfig config = new EncryptionConfig();
            SearchSpecialistAPI searchAPI = new SearchSpecialistAPI(
                blockchain, TEST_PASSWORD, userKeyPair.getPrivate(), config
            );

            List<SearchFrameworkEngine.EnhancedSearchResult> results =
                searchAPI.searchPublic(specialKeyword, 10);

            assertFalse(results.isEmpty(),
                "Special characters keyword should be searchable");

            logger.info("✅ Special characters keyword handled correctly");
        }

        @Test
        @DisplayName("Only private keywords (no public) works correctly")
        void testOnlyPrivateKeywords() {
            // Arrange
            String privateOnly = "PRIVATE_ONLY_" + System.currentTimeMillis();

            // Act
            Block block = api.createBlockWithOptions(
                "Content with only private keywords",
                new UserFriendlyEncryptionAPI.BlockCreationOptions()
                    .withEncryption(true)
                    .withPassword(TEST_PASSWORD)
                    .withKeywords(new String[]{privateOnly})
                    .withCategory("PRIVATE_ONLY")
            );

            // Assert
            assertNotNull(block, "Block should be created");
            assertTrue(block.isDataEncrypted(), "Block should be encrypted");

            // manualKeywords may be null or empty (no public keywords)
            String manualKeywords = block.getManualKeywords();
            if (manualKeywords != null) {
                assertFalse(manualKeywords.contains(privateOnly.toLowerCase()),
                    "Private keyword should NOT be in manualKeywords");
            }

            // autoKeywords should contain encrypted data
            String autoKeywords = block.getAutoKeywords();
            assertNotNull(autoKeywords, "autoKeywords should not be null");
            assertTrue(autoKeywords.length() > 0, "autoKeywords should contain encrypted data");

            // Wait for indexing
            waitForIndexing();

            // Verify NOT searchable with searchPublic
            EncryptionConfig config = new EncryptionConfig();
            SearchSpecialistAPI searchAPI = new SearchSpecialistAPI(
                blockchain, TEST_PASSWORD, userKeyPair.getPrivate(), config
            );

            List<SearchFrameworkEngine.EnhancedSearchResult> publicResults =
                searchAPI.searchPublic(privateOnly, 10);
            assertTrue(publicResults.isEmpty(),
                "Private-only keyword should NOT be found with searchPublic");

            // Verify searchable with searchSecure
            List<SearchFrameworkEngine.EnhancedSearchResult> secureResults =
                searchAPI.searchSecure(privateOnly, TEST_PASSWORD, 10);
            assertFalse(secureResults.isEmpty(),
                "Private-only keyword should be found with searchSecure");

            logger.info("✅ Private-only keywords work correctly");
        }

        @Test
        @DisplayName("Only public keywords (no private) works correctly")
        void testOnlyPublicKeywords() {
            // Arrange
            String publicOnly = "PUBLIC_ONLY_" + System.currentTimeMillis();

            // Act
            Block block = api.createBlockWithOptions(
                "Content with only public keywords",
                new UserFriendlyEncryptionAPI.BlockCreationOptions()
                    .withEncryption(true)
                    .withPassword(TEST_PASSWORD)
                    .withKeywords(new String[]{"public:" + publicOnly})
                    .withCategory("PUBLIC_ONLY")
            );

            // Assert
            assertNotNull(block, "Block should be created");
            assertTrue(block.isDataEncrypted(), "Block should be encrypted");

            // manualKeywords should contain public keyword
            String manualKeywords = block.getManualKeywords();
            assertNotNull(manualKeywords, "manualKeywords should not be null");
            assertTrue(manualKeywords.contains("public:" + publicOnly.toLowerCase()),
                "manualKeywords should contain public keyword");

            // Wait for indexing
            waitForIndexing();

            // Verify searchable with searchPublic
            EncryptionConfig config = new EncryptionConfig();
            SearchSpecialistAPI searchAPI = new SearchSpecialistAPI(
                blockchain, TEST_PASSWORD, userKeyPair.getPrivate(), config
            );

            List<SearchFrameworkEngine.EnhancedSearchResult> publicResults =
                searchAPI.searchPublic(publicOnly, 10);
            assertFalse(publicResults.isEmpty(),
                "Public-only keyword should be found with searchPublic");

            logger.info("✅ Public-only keywords work correctly");
        }

        @Test
        @DisplayName("Empty keywords array creates block without keywords")
        void testEmptyKeywordsArray() {
            // Act
            Block block = api.createBlockWithOptions(
                "Content without keywords",
                new UserFriendlyEncryptionAPI.BlockCreationOptions()
                    .withEncryption(true)
                    .withPassword(TEST_PASSWORD)
                    .withKeywords(new String[]{})
                    .withCategory("NO_KEYWORDS")
            );

            // Assert
            assertNotNull(block, "Block should be created");
            assertTrue(block.isDataEncrypted(), "Block should be encrypted");

            logger.info("✅ Empty keywords array handled correctly");
        }

        @Test
        @DisplayName("Null keywords array creates block without keywords")
        void testNullKeywordsArray() {
            // Act
            Block block = api.createBlockWithOptions(
                "Content with null keywords",
                new UserFriendlyEncryptionAPI.BlockCreationOptions()
                    .withEncryption(true)
                    .withPassword(TEST_PASSWORD)
                    .withKeywords(null)
                    .withCategory("NULL_KEYWORDS")
            );

            // Assert
            assertNotNull(block, "Block should be created");
            assertTrue(block.isDataEncrypted(), "Block should be encrypted");

            logger.info("✅ Null keywords array handled correctly");
        }
    }

    // ========== MULTIPLE BLOCKS TESTS ==========

    @Nested
    @DisplayName("📚 Multiple Blocks with Keywords")
    class MultipleBlocksTests {

        @Test
        @DisplayName("Multiple blocks with same public keyword are all found")
        void testMultipleBlocksSamePublicKeyword() {
            // Arrange
            String sharedKeyword = "SHARED_PUBLIC_" + System.currentTimeMillis();

            // Create 3 blocks with same public keyword
            Block block1 = api.createBlockWithOptions(
                "Content 1",
                new UserFriendlyEncryptionAPI.BlockCreationOptions()
                    .withEncryption(true)
                    .withPassword(TEST_PASSWORD)
                    .withKeywords(new String[]{"public:" + sharedKeyword})
                    .withCategory("MULTI_1")
            );

            Block block2 = api.createBlockWithOptions(
                "Content 2",
                new UserFriendlyEncryptionAPI.BlockCreationOptions()
                    .withEncryption(true)
                    .withPassword(TEST_PASSWORD)
                    .withKeywords(new String[]{"public:" + sharedKeyword})
                    .withCategory("MULTI_2")
            );

            Block block3 = api.createBlockWithOptions(
                "Content 3",
                new UserFriendlyEncryptionAPI.BlockCreationOptions()
                    .withEncryption(true)
                    .withPassword(TEST_PASSWORD)
                    .withKeywords(new String[]{"public:" + sharedKeyword})
                    .withCategory("MULTI_3")
            );

            assertNotNull(block1, "Block 1 should be created");
            assertNotNull(block2, "Block 2 should be created");
            assertNotNull(block3, "Block 3 should be created");

            // Wait for indexing
            waitForIndexing();

            // Act
            EncryptionConfig config = new EncryptionConfig();
            SearchSpecialistAPI searchAPI = new SearchSpecialistAPI(
                blockchain, TEST_PASSWORD, userKeyPair.getPrivate(), config
            );

            List<SearchFrameworkEngine.EnhancedSearchResult> results =
                searchAPI.searchPublic(sharedKeyword, 10);

            // Assert
            assertEquals(3, results.size(),
                "Should find all 3 blocks with shared public keyword");

            logger.info("✅ Found {} blocks with shared public keyword '{}'",
                results.size(), sharedKeyword);
        }

        @Test
        @DisplayName("Blocks maintain keyword isolation")
        void testBlocksKeywordIsolation() {
            // Arrange
            String unique1 = "UNIQUE_ONE_" + System.currentTimeMillis();
            String unique2 = "UNIQUE_TWO_" + System.currentTimeMillis();

            Block block1 = api.createBlockWithOptions(
                "Content with unique1",
                new UserFriendlyEncryptionAPI.BlockCreationOptions()
                    .withEncryption(true)
                    .withPassword(TEST_PASSWORD)
                    .withKeywords(new String[]{"public:" + unique1})
                    .withCategory("ISOLATION_1")
            );

            Block block2 = api.createBlockWithOptions(
                "Content with unique2",
                new UserFriendlyEncryptionAPI.BlockCreationOptions()
                    .withEncryption(true)
                    .withPassword(TEST_PASSWORD)
                    .withKeywords(new String[]{"public:" + unique2})
                    .withCategory("ISOLATION_2")
            );

            assertNotNull(block1, "Block 1 should be created");
            assertNotNull(block2, "Block 2 should be created");

            // Wait for indexing
            waitForIndexing();

            // Act
            EncryptionConfig config = new EncryptionConfig();
            SearchSpecialistAPI searchAPI = new SearchSpecialistAPI(
                blockchain, TEST_PASSWORD, userKeyPair.getPrivate(), config
            );

            List<SearchFrameworkEngine.EnhancedSearchResult> results1 =
                searchAPI.searchPublic(unique1, 10);
            List<SearchFrameworkEngine.EnhancedSearchResult> results2 =
                searchAPI.searchPublic(unique2, 10);

            // Assert
            assertEquals(1, results1.size(), "unique1 should match exactly 1 block");
            assertEquals(1, results2.size(), "unique2 should match exactly 1 block");
            assertEquals(block1.getHash(), results1.get(0).getBlockHash(),
                "unique1 should match block1");
            assertEquals(block2.getHash(), results2.get(0).getBlockHash(),
                "unique2 should match block2");

            logger.info("✅ Blocks maintain keyword isolation correctly");
        }
    }

    // ========== HELPER METHODS ==========

    private void waitForIndexing() {
        try {
            IndexingCoordinator.getInstance().waitForCompletion();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Indexing interrupted", e);
        }
    }
}
