package com.rbatllet.blockchain.search;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.security.UserRole;
import com.rbatllet.blockchain.util.TestGenesisKeyManager;
import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.util.JPAUtil;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.security.KeyPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Debug test to understand encryption metadata format
 */
public class DebugEncryptionFormatTest {
    private static final Logger logger = LoggerFactory.getLogger(DebugEncryptionFormatTest.class);


    private static Blockchain blockchain;
    private static KeyPair bootstrapKeyPair;
    private static KeyPair authorizedKeyPair;
    private static final String ENCRYPTION_PASSWORD = "DebugFormat123!@#";

    @BeforeAll
    static void setUpClass() {
        blockchain = new Blockchain();

        // Load bootstrap admin keys (auto-generates if missing - test-only)
        bootstrapKeyPair = TestGenesisKeyManager.ensureGenesisKeysExist();

        authorizedKeyPair = CryptoUtil.generateKeyPair();
    }
    
    @AfterAll
    static void tearDownClass() {
        JPAUtil.closeEntityManager();
    }
    
    @BeforeEach
    void cleanDatabase() {
        // Clean database before each test to ensure isolation - using thread-safe DAO method
        // BlockRepository now package-private - use clearAndReinitialize();
        blockchain.getAuthorizedKeyDAO().cleanupTestData();

        // Register bootstrap admin in blockchain (RBAC v1.0.6)
        blockchain.createBootstrapAdmin(
            CryptoUtil.publicKeyToString(bootstrapKeyPair.getPublic()),
            "BOOTSTRAP_ADMIN"
        );

        // Re-add authorized key after cleanup for test to work
        blockchain.addAuthorizedKey(
            CryptoUtil.publicKeyToString(authorizedKeyPair.getPublic()),
            "Test User",
            bootstrapKeyPair,
            UserRole.USER
        );
    }
    
    @Test
    void debugEncryptionFormat() {
        logger.info("\n=== Debug Encryption Format ===");
        
        // Create an encrypted block with category
        Block encryptedBlock = blockchain.addEncryptedBlockWithKeywords(
            "Sample medical data for format testing",
            ENCRYPTION_PASSWORD,
            new String[]{"medical", "testing"},
            "MEDICAL",
            authorizedKeyPair.getPrivate(),
            authorizedKeyPair.getPublic()
        );
        
        assertNotNull(encryptedBlock);
        logger.info("Encrypted block created successfully");
        
        // Analyze the encryption metadata format
        String metadata = encryptedBlock.getEncryptionMetadata();
        logger.info("\nEncryption Metadata Analysis:");
        logger.info("Length: " + metadata.length() + " characters");
        logger.info("First 100 chars: " + metadata.substring(0, Math.min(100, metadata.length())));
        logger.info("Last 100 chars: " + metadata.substring(Math.max(0, metadata.length() - 100)));
        
        // Check format characteristics
        logger.info("\nFormat Analysis:");
        logger.info("Contains ':' separators: " + metadata.contains(":"));
        logger.info("Starts with base64-like: " + metadata.substring(0, Math.min(50, metadata.length())).matches("^[A-Za-z0-9+/]*$"));
        logger.info("Is pure base64: " + metadata.matches("^[A-Za-z0-9+/]+=*$"));
        
        // Check each component if there are separators
        if (metadata.contains(":")) {
            String[] parts = metadata.split(":");
            logger.info("Number of parts separated by ':': " + parts.length);
            for (int i = 0; i < parts.length; i++) {
                System.out.printf("Part %d: length=%d, preview=%s%n", 
                    i, parts[i].length(), 
                    parts[i].substring(0, Math.min(30, parts[i].length())));
            }
        }
        
        // Test other fields
        logger.info("\nOther Field Analysis:");
        logger.info("Data field: " + encryptedBlock.getData());
        logger.info("Manual keywords: " + encryptedBlock.getManualKeywords());
        logger.info("Auto keywords: " + encryptedBlock.getAutoKeywords());
        logger.info("Searchable content: " + encryptedBlock.getSearchableContent());
        logger.info("Category: " + encryptedBlock.getContentCategory());
        logger.info("Is encrypted: " + encryptedBlock.isDataEncrypted());
        
        // Test validation
        var validationResult = blockchain.validateChainDetailed();
        logger.info("\nValidation Result:");
        logger.info(validationResult.getSummary());
        
        if (!validationResult.isValid()) {
            for (var blockResult : validationResult.getBlockResults()) {
                if (!blockResult.isValid()) {
                    logger.info("Block #" + blockResult.getBlock().getBlockNumber() + 
                                     " validation error: " + blockResult.getErrorMessage());
                }
            }
        }
    }
}