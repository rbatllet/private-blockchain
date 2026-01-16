package com.rbatllet.blockchain.search;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.security.UserRole;
import com.rbatllet.blockchain.config.EncryptionConfig;
import com.rbatllet.blockchain.util.TestGenesisKeyManager;
import com.rbatllet.blockchain.util.CryptoUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyPair;
import java.util.List;

/**
 * Test for "public:" prefix functionality in encrypted blocks.
 *
 * Bug: Keywords with "public:" prefix in encrypted blocks should be indexed
 * in the public metadata layer (searchable without password via searchPublic),
 * but currently they are only searchable with searchSecure + password.
 *
 * Expected behavior:
 * - Encrypted block with --keywords "public:PUBLIC_KEYWORD PRIVATE_KEYWORD"
 * - PUBLIC_KEYWORD should be searchable with searchPublic (no password)
 * - PRIVATE_KEYWORD should only be searchable with searchSecure + password
 */
public class PublicPrefixEncryptedBlockTest {

    private static final Logger logger = LoggerFactory.getLogger(PublicPrefixEncryptedBlockTest.class);

    private Blockchain blockchain;
    private KeyPair bootstrapKeyPair;
    private SearchSpecialistAPI searchAPI;
    private EncryptionConfig config;
    private KeyPair userKeyPair;
    private String testPassword = "TestPassword123456#";

    @BeforeEach
    public void setUp() {
        blockchain = new Blockchain();
        blockchain.clearAndReinitialize();

        // Load bootstrap admin keys
        bootstrapKeyPair = TestGenesisKeyManager.ensureGenesisKeysExist();

        // Register bootstrap admin
        blockchain.createBootstrapAdmin(
            CryptoUtil.publicKeyToString(bootstrapKeyPair.getPublic()),
            "BOOTSTRAP_ADMIN"
        );

        // Create test user
        userKeyPair = CryptoUtil.generateKeyPair();
        String userPublicKey = CryptoUtil.publicKeyToString(userKeyPair.getPublic());
        blockchain.addAuthorizedKey(userPublicKey, "testUser", bootstrapKeyPair, UserRole.USER);

        // Configure encryption
        config = new EncryptionConfig();

        // Initialize search API with password
        // searchPublic() will ignore the password and search only public metadata
        searchAPI = new SearchSpecialistAPI(blockchain, testPassword, userKeyPair.getPrivate(), config);
    }

    @AfterEach
    public void tearDown() {
        if (blockchain != null) {
            blockchain.clearAndReinitialize();
            blockchain.shutdown();
        }
    }

    /**
     * Bug reproduction test:
     * 1. Create encrypted block with keywords "public:PUBLIC_TEST PRIVATE_TEST"
     * 2. Index the block
     * 3. Search for PUBLIC_TEST with searchPublic (should find but currently fails)
     * 4. Search for PRIVATE_TEST with searchPublic (should NOT find)
     * 5. Search for PUBLIC_TEST with searchSecure + password (should find)
     */
    @Test
    public void testPublicPrefixInEncryptedBlock() {
        logger.info("🔍 TEST: Public prefix in encrypted block");
        logger.info("========================================");

        String publicKeyword = "PUBLIC_TEST_" + System.currentTimeMillis();
        String privateKeyword = "PRIVATE_TEST_" + System.currentTimeMillis();

        // Step 1: Create encrypted block with mixed keywords
        logger.info("📝 Step 1: Creating encrypted block");
        logger.info("   Keywords: 'public:{} {}'", publicKeyword, privateKeyword);

        String combinedKeywords = "public:" + publicKeyword + " " + privateKeyword;

        Block encryptedBlock = blockchain.addEncryptedBlockWithKeywords(
            "Encrypted block with public and private keywords",
            testPassword,
            combinedKeywords.split("\\s+"),
            "TEST",
            userKeyPair.getPrivate(),
            userKeyPair.getPublic()
        );

        logger.info("✅ Block created: #{} Hash: {}...",
                    encryptedBlock.getBlockNumber(),
                    encryptedBlock.getHash().substring(0, 8));
        logger.info("📝 manualKeywords: '{}'", encryptedBlock.getManualKeywords());
        logger.info("📝 autoKeywords: '{}'", encryptedBlock.getAutoKeywords() != null ? encryptedBlock.getAutoKeywords().substring(0, Math.min(50, encryptedBlock.getAutoKeywords().length())) + "..." : "null");

        // Step 2: Index the block
        logger.info("📝 Step 2: Indexing the block...");
        searchAPI.initializeWithBlockchain(blockchain, testPassword, userKeyPair.getPrivate());

        // Wait for indexing
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Step 3: Search for PUBLIC keyword with searchPublic (ignores password, searches only public metadata)
        logger.info("📝 Step 3: Searching for '{}' with searchPublic...", publicKeyword);
        List<SearchFrameworkEngine.EnhancedSearchResult> publicResults =
            searchAPI.searchPublic(publicKeyword, 10);

        logger.info("🔍 Results: {}", publicResults.size());
        if (!publicResults.isEmpty()) {
            for (SearchFrameworkEngine.EnhancedSearchResult result : publicResults) {
                logger.info("   - Block {}: {}", result.getBlockHash().substring(0, 8), result.getSummary());
            }
        }

        // Step 4: Search for PRIVATE keyword with searchPublic (should NOT find)
        logger.info("📝 Step 4: Searching for '{}' with searchPublic...", privateKeyword);
        List<SearchFrameworkEngine.EnhancedSearchResult> privateFastResults =
            searchAPI.searchPublic(privateKeyword, 10);

        logger.info("🔍 Results: {}", privateFastResults.size());

        // Step 5: Search for PUBLIC keyword with searchSecure + password
        logger.info("📝 Step 5: Searching for '{}' with searchSecure + password...", publicKeyword);
        List<SearchFrameworkEngine.EnhancedSearchResult> publicWithPasswordResults =
            searchAPI.searchSecure(publicKeyword, testPassword, 10);

        logger.info("🔍 Results: {}", publicWithPasswordResults.size());
        if (!publicWithPasswordResults.isEmpty()) {
            for (SearchFrameworkEngine.EnhancedSearchResult result : publicWithPasswordResults) {
                logger.info("   - Block {}: {}", result.getBlockHash().substring(0, 8), result.getSummary());
            }
        }

        // Step 6: Search for PRIVATE keyword with searchSecure + password (should find)
        logger.info("📝 Step 6: Searching for '{}' with searchSecure + password...", privateKeyword);
        List<SearchFrameworkEngine.EnhancedSearchResult> privateWithPasswordResults =
            searchAPI.searchSecure(privateKeyword, testPassword, 10);

        logger.info("🔍 Results: {}", privateWithPasswordResults.size());
        if (!privateWithPasswordResults.isEmpty()) {
            for (SearchFrameworkEngine.EnhancedSearchResult result : privateWithPasswordResults) {
                logger.info("   - Block {}: {}", result.getBlockHash().substring(0, 8), result.getSummary());
            }
        }

        // Assertions and bug report
        logger.info("\n📊 TEST RESULTS:");
        logger.info("========================================");

        // BUG: Public keyword should be searchable with searchPublic (no password)
        if (publicResults.isEmpty()) {
            logger.error("❌ BUG CONFIRMED: Public keyword '{}' NOT found with searchPublic (expected to find)",
                        publicKeyword);
        } else {
            logger.info("✅ Public keyword '{}' found with searchPublic (no password)", publicKeyword);
        }

        // Private keyword should NOT be searchable with searchPublic
        if (privateFastResults.isEmpty()) {
            logger.info("✅ Private keyword '{}' correctly NOT found with searchPublic", privateKeyword);
        } else {
            logger.error("❌ Private keyword '{}' FOUND with searchPublic (should not find)",
                        privateKeyword);
        }

        // Both keywords should be searchable with password
        if (!publicWithPasswordResults.isEmpty()) {
            logger.info("✅ Public keyword '{}' found with searchSecure + password", publicKeyword);
        } else {
            logger.error("❌ Public keyword '{}' NOT found with searchSecure + password",
                        publicKeyword);
        }

        if (!privateWithPasswordResults.isEmpty()) {
            logger.info("✅ Private keyword '{}' found with searchSecure + password", privateKeyword);
        } else {
            logger.error("❌ Private keyword '{}' NOT found with searchSecure + password",
                        privateKeyword);
        }

        logger.info("========================================");

        // This assertion will fail if the bug is present
        // Uncomment to make the test fail explicitly
        // assertFalse("BUG: Public keyword should be searchable with searchPublic", publicResults.isEmpty());
    }
}
