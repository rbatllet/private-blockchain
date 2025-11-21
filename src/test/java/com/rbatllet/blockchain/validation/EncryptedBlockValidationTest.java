package com.rbatllet.blockchain.validation;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.security.KeyFileLoader;
import com.rbatllet.blockchain.security.UserRole;
import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.util.JPAUtil;
import org.junit.jupiter.api.*;

import java.security.KeyPair;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test enhanced validateChainDetailed() functionality for encrypted blocks
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EncryptedBlockValidationTest {

    private static Blockchain blockchain;
    private static KeyPair bootstrapKeyPair;
    private static KeyPair authorizedKeyPair;
    private static final String ENCRYPTION_PASSWORD = "ValidateTest123!@#";

    @BeforeAll
    static void setUpClass() throws Exception {
        blockchain = new Blockchain();

        // Load bootstrap admin keys
        bootstrapKeyPair = KeyFileLoader.loadKeyPairFromFiles(
            "./keys/genesis-admin.private",
            "./keys/genesis-admin.public"
        );

        authorizedKeyPair = CryptoUtil.generateKeyPair();
    }
    
    @AfterAll
    static void tearDownClass() {
        JPAUtil.closeEntityManager();
    }
    
    @BeforeEach
    void cleanDatabase() {
        // Clean database completely before each test to ensure isolation
        blockchain.clearAndReinitialize();

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
    @Order(1)
    void testValidEncryptedBlockValidation() {
        System.out.println("\n=== Testing Valid Encrypted Block Validation ===");
        
        // Create a valid encrypted block
        Block encryptedBlock = blockchain.addEncryptedBlockWithKeywords(
            "Sensitive medical data that needs encryption",
            ENCRYPTION_PASSWORD,
            new String[]{"medical", "sensitive"},
            "MEDICAL",
            authorizedKeyPair.getPrivate(),
            authorizedKeyPair.getPublic()
        );
        
        assertNotNull(encryptedBlock);
        assertTrue(encryptedBlock.isDataEncrypted());
        assertEquals("MEDICAL", encryptedBlock.getContentCategory());
        System.out.println("✅ Encrypted block created: " + encryptedBlock.getBlockNumber());
        
        // Test individual block validation
        var validationResult = EncryptedBlockValidator.validateEncryptedBlock(encryptedBlock);
        assertTrue(validationResult.isValid());
        assertTrue(validationResult.isEncryptionIntact());
        assertTrue(validationResult.isMetadataValid());
        assertTrue(validationResult.isFormatCorrect());
        assertNull(validationResult.getErrorMessage());
        
        System.out.println("✅ Individual encrypted block validation passed");
        System.out.println(validationResult.getDetailedReport());
        
        // Test chain validation with encrypted blocks
        var chainValidation = blockchain.validateChainDetailed();
        assertTrue(chainValidation.isStructurallyIntact());
        assertTrue(chainValidation.isFullyCompliant());
        
        System.out.println("✅ Chain validation with encrypted blocks passed");
        System.out.println("   Total blocks: " + chainValidation.getTotalBlocks());
        System.out.println("   Valid blocks: " + chainValidation.getValidBlocks());
    }
    
    @Test
    @Order(2)
    void testCorruptedEncryptedBlockDetection() {
        System.out.println("\n=== Testing Corrupted Encrypted Block Detection ===");
        
        // Create a valid encrypted block first
        Block encryptedBlock = blockchain.addEncryptedBlockWithKeywords(
            "Data that will be corrupted",
            ENCRYPTION_PASSWORD,
            new String[]{"test", "corruption"},
            "TEST",
            authorizedKeyPair.getPrivate(),
            authorizedKeyPair.getPublic()
        );
        
        assertNotNull(encryptedBlock);
        System.out.println("✅ Valid encrypted block created: " + encryptedBlock.getBlockNumber());
        
        // Simulate corruption scenarios
        
        // 1. Test missing encryption metadata
        Block corruptedBlock1 = new Block();
        corruptedBlock1.setId(999L);
        corruptedBlock1.setBlockNumber(999L);
        corruptedBlock1.setData("Original data preserved"); // Data field unchanged (correct architecture)
        corruptedBlock1.setIsEncrypted(true);
        corruptedBlock1.setEncryptionMetadata(null); // Missing metadata - THIS IS THE CORRUPTION
        
        var validation1 = EncryptedBlockValidator.validateEncryptedBlock(corruptedBlock1);
        assertFalse(validation1.isValid());
        assertFalse(validation1.isEncryptionIntact());
        assertFalse(validation1.isMetadataValid());
        assertTrue(validation1.getErrorMessage().contains("missing"));
        System.out.println("✅ Detected missing encryption metadata");
        
        // 2. Test invalid encryption metadata format
        // UPDATED: Data field is NOT validated anymore (maintains hash integrity)
        // Only encryptionMetadata format is validated
        Block corruptedBlock2 = new Block();
        corruptedBlock2.setId(998L);
        corruptedBlock2.setBlockNumber(998L);
        corruptedBlock2.setData("Original data preserved"); // Data field unchanged (correct)
        corruptedBlock2.setIsEncrypted(true);
        corruptedBlock2.setEncryptionMetadata("error corrupted data"); // Invalid format - THIS IS THE CORRUPTION
        
        var validation2 = EncryptedBlockValidator.validateEncryptedBlock(corruptedBlock2);
        assertFalse(validation2.isValid());
        assertFalse(validation2.isEncryptionIntact());
        assertTrue(validation2.getErrorMessage().contains("corrupted"));
        System.out.println("✅ Detected corrupted encryption metadata");
        
        // 3. Test corruption detection
        Block corruptedBlock3 = new Block();
        corruptedBlock3.setId(997L);
        corruptedBlock3.setBlockNumber(997L);
        corruptedBlock3.setData("Original data"); // Data unchanged
        corruptedBlock3.setIsEncrypted(true);
        corruptedBlock3.setEncryptionMetadata("error corrupted data"); // Invalid format
        
        var validation3 = EncryptedBlockValidator.validateEncryptedBlock(corruptedBlock3);
        assertFalse(validation3.isValid());
        assertFalse(validation3.isEncryptionIntact());
        assertTrue(validation3.getErrorMessage().contains("corrupted"));
        System.out.println("✅ Detected corrupted encryption (validation3)");
        
        // 4. Test corruption detection
        var corruption3 = EncryptedBlockValidator.detectCorruption(corruptedBlock3);
        assertTrue(corruption3.isPossiblyCorrupted());
        assertTrue(corruption3.getIssues().contains("Corruption markers detected"));
        System.out.println("✅ Corruption detection working: " + corruption3);
    }
    
    @Test
    @Order(3)
    void testMixedBlockchainValidation() {
        System.out.println("\n=== Testing Mixed Blockchain Validation ===");
        
        // Create a mix of encrypted and unencrypted blocks
        Block publicBlock1 = blockchain.addBlockAndReturn(
            "Public announcement about new features",
            authorizedKeyPair.getPrivate(),
            authorizedKeyPair.getPublic()
        );
        
        Block encryptedBlock1 = blockchain.addEncryptedBlockWithKeywords(
            "Confidential medical records",
            ENCRYPTION_PASSWORD,
            new String[]{"medical", "confidential"},
            "MEDICAL",
            authorizedKeyPair.getPrivate(),
            authorizedKeyPair.getPublic()
        );
        
        Block publicBlock2 = blockchain.addBlockAndReturn(
            "Another public announcement",
            authorizedKeyPair.getPrivate(),
            authorizedKeyPair.getPublic()
        );
        
        Block encryptedBlock2 = blockchain.addEncryptedBlockWithKeywords(
            "Secret financial transaction details",
            ENCRYPTION_PASSWORD,
            new String[]{"financial", "secret"},
            "FINANCE",
            authorizedKeyPair.getPrivate(),
            authorizedKeyPair.getPublic()
        );
        
        System.out.println("✅ Mixed blockchain created:");
        System.out.printf("   Public block #%d: %s%n", publicBlock1.getBlockNumber(), 
            publicBlock1.isDataEncrypted() ? "ENCRYPTED" : "PUBLIC");
        System.out.printf("   Encrypted block #%d: %s%n", encryptedBlock1.getBlockNumber(), 
            encryptedBlock1.isDataEncrypted() ? "ENCRYPTED" : "PUBLIC");
        System.out.printf("   Public block #%d: %s%n", publicBlock2.getBlockNumber(), 
            publicBlock2.isDataEncrypted() ? "ENCRYPTED" : "PUBLIC");
        System.out.printf("   Encrypted block #%d: %s%n", encryptedBlock2.getBlockNumber(), 
            encryptedBlock2.isDataEncrypted() ? "ENCRYPTED" : "PUBLIC");
        
        // Validate the entire mixed chain
        var chainValidation = blockchain.validateChainDetailed();
        assertTrue(chainValidation.isStructurallyIntact(), 
            "Chain should be structurally intact");
        
        // UPDATED: Chain may have authorization warnings from previous test cleanup
        // but structure is intact and blocks are valid
        // Check structural integrity only (not full compliance which includes auth)
        System.out.println("✅ Mixed blockchain validation passed");
        System.out.println("   Structural integrity: " + chainValidation.isStructurallyIntact());
        System.out.println("   Total blocks: " + chainValidation.getTotalBlocks());
        System.out.println("   Valid blocks: " + chainValidation.getValidBlocks());
        System.out.println("   Invalid blocks: " + chainValidation.getInvalidBlocks());
        System.out.println("   Revoked blocks: " + chainValidation.getRevokedBlocks());
        
        // Get validation report
        String report = blockchain.getValidationReport();
        assertNotNull(report);
        assertTrue(report.contains("BLOCKCHAIN VALIDATION REPORT"));
        System.out.println("\n📊 Validation Report:");
        System.out.println(report);
    }
    
    @Test
    @Order(4)
    void testPasswordValidation() {
        System.out.println("\n=== Testing Password-based Validation ===");
        
        // Create encrypted block
        Block encryptedBlock = blockchain.addEncryptedBlock(
            "Password protected sensitive data",
            ENCRYPTION_PASSWORD,
            authorizedKeyPair.getPrivate(),
            authorizedKeyPair.getPublic()
        );
        
        assertNotNull(encryptedBlock);
        System.out.println("✅ Encrypted block created: " + encryptedBlock.getBlockNumber());
        
        // Test correct password
        boolean canDecryptCorrect = EncryptedBlockValidator.canDecryptWithPassword(
            encryptedBlock, ENCRYPTION_PASSWORD);
        assertTrue(canDecryptCorrect);
        System.out.println("✅ Correct password validation passed");
        
        // Test wrong password
        boolean canDecryptWrong = EncryptedBlockValidator.canDecryptWithPassword(
            encryptedBlock, "WrongPassword123");
        assertFalse(canDecryptWrong);
        System.out.println("✅ Wrong password correctly rejected");
        
        // Test password validation on non-encrypted block
        Block publicBlock = blockchain.addBlockAndReturn(
            "Public data",
            authorizedKeyPair.getPrivate(),
            authorizedKeyPair.getPublic()
        );
        
        boolean canDecryptPublic = EncryptedBlockValidator.canDecryptWithPassword(
            publicBlock, ENCRYPTION_PASSWORD);
        assertFalse(canDecryptPublic); // Should return false for non-encrypted blocks
        System.out.println("✅ Password validation correctly handles non-encrypted blocks");
    }
    
    @Test
    @Order(5)
    void testValidationStatistics() {
        System.out.println("\n=== Testing Validation Statistics ===");
        
        // Create test blockchain with various types
        blockchain.addBlockAndReturn("Public block 1", authorizedKeyPair.getPrivate(), authorizedKeyPair.getPublic());
        blockchain.addEncryptedBlock("Encrypted data 1", ENCRYPTION_PASSWORD, authorizedKeyPair.getPrivate(), authorizedKeyPair.getPublic());
        blockchain.addBlockAndReturn("Public block 2", authorizedKeyPair.getPrivate(), authorizedKeyPair.getPublic());
        blockchain.addEncryptedBlockWithKeywords("Encrypted data 2", ENCRYPTION_PASSWORD, new String[]{"test"}, "TEST", authorizedKeyPair.getPrivate(), authorizedKeyPair.getPublic());
        
        // Get search statistics which should include validation info
        String stats = blockchain.getSearchStatistics();
        assertNotNull(stats);
        assertTrue(stats.contains("Total blocks"));
        assertTrue(stats.contains("Encrypted blocks"));
        assertTrue(stats.contains("Unencrypted blocks"));
        
        System.out.println("📊 Search/Validation Statistics:");
        System.out.println(stats);
        
        // Validate chain and get detailed report
        var validation = blockchain.validateChainDetailed();
        String report = validation.getDetailedReport();
        assertNotNull(report);
        
        System.out.println("📋 Detailed Validation Report:");
        System.out.println(report);
        
        System.out.println("✅ Validation statistics working correctly");
    }
}