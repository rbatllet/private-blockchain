package com.rbatllet.blockchain.search;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.util.JPAUtil;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.security.KeyPair;

/**
 * Debug test to understand encryption metadata format
 */
public class DebugEncryptionFormatTest {
    
    private static Blockchain blockchain;
    private static KeyPair authorizedKeyPair;
    private static final String ENCRYPTION_PASSWORD = "DebugFormat123!@#";
    
    @BeforeAll
    static void setUpClass() {
        blockchain = new Blockchain();
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
        
        // Re-add authorized key after cleanup for test to work
        blockchain.addAuthorizedKey(
            CryptoUtil.publicKeyToString(authorizedKeyPair.getPublic()),
            "Test User"
        );
    }
    
    @Test
    void debugEncryptionFormat() {
        System.out.println("\n=== Debug Encryption Format ===");
        
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
        System.out.println("Encrypted block created successfully");
        
        // Analyze the encryption metadata format
        String metadata = encryptedBlock.getEncryptionMetadata();
        System.out.println("\nEncryption Metadata Analysis:");
        System.out.println("Length: " + metadata.length() + " characters");
        System.out.println("First 100 chars: " + metadata.substring(0, Math.min(100, metadata.length())));
        System.out.println("Last 100 chars: " + metadata.substring(Math.max(0, metadata.length() - 100)));
        
        // Check format characteristics
        System.out.println("\nFormat Analysis:");
        System.out.println("Contains ':' separators: " + metadata.contains(":"));
        System.out.println("Starts with base64-like: " + metadata.substring(0, Math.min(50, metadata.length())).matches("^[A-Za-z0-9+/]*$"));
        System.out.println("Is pure base64: " + metadata.matches("^[A-Za-z0-9+/]+=*$"));
        
        // Check each component if there are separators
        if (metadata.contains(":")) {
            String[] parts = metadata.split(":");
            System.out.println("Number of parts separated by ':': " + parts.length);
            for (int i = 0; i < parts.length; i++) {
                System.out.printf("Part %d: length=%d, preview=%s%n", 
                    i, parts[i].length(), 
                    parts[i].substring(0, Math.min(30, parts[i].length())));
            }
        }
        
        // Test other fields
        System.out.println("\nOther Field Analysis:");
        System.out.println("Data field: " + encryptedBlock.getData());
        System.out.println("Manual keywords: " + encryptedBlock.getManualKeywords());
        System.out.println("Auto keywords: " + encryptedBlock.getAutoKeywords());
        System.out.println("Searchable content: " + encryptedBlock.getSearchableContent());
        System.out.println("Category: " + encryptedBlock.getContentCategory());
        System.out.println("Is encrypted: " + encryptedBlock.isDataEncrypted());
        
        // Test validation
        var validationResult = blockchain.validateChainDetailed();
        System.out.println("\nValidation Result:");
        System.out.println(validationResult.getSummary());
        
        if (!validationResult.isValid()) {
            for (var blockResult : validationResult.getBlockResults()) {
                if (!blockResult.isValid()) {
                    System.out.println("Block #" + blockResult.getBlock().getBlockNumber() + 
                                     " validation error: " + blockResult.getErrorMessage());
                }
            }
        }
    }
}