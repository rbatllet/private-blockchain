package com.rbatllet.blockchain.core;

import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.util.JPAUtil;
import org.junit.jupiter.api.*;

import java.security.KeyPair;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test encryption functionality in the Blockchain core class
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BlockchainEncryptionTest {
    
    private static Blockchain blockchain;
    private static KeyPair authorizedKeyPair;
    private static final String ENCRYPTION_PASSWORD = "SecurePassword123!@#";
    
    // Test data
    private static final String PUBLIC_DATA = "This is public information that anyone can read";
    private static final String CONFIDENTIAL_DATA = """
        CONFIDENTIAL MEDICAL RECORD
        Patient: John Doe
        SSN: 123-45-6789
        Diagnosis: Hypertension, Type 2 Diabetes
        Treatment: Metformin 500mg, Lisinopril 10mg
        Lab Results: HbA1c: 7.2%, BP: 140/90
        Next Appointment: 2024-03-15
        Insurance: Blue Cross Blue Shield Policy #ABC123456
        """;
    
    @BeforeAll
    static void setUpClass() {
        blockchain = new Blockchain();
        authorizedKeyPair = CryptoUtil.generateKeyPair();
        
        // Clean ALL previous test data to ensure clean state using thread-safe DAO method
        blockchain.getBlockDAO().cleanupTestData();
        blockchain.getAuthorizedKeyDAO().cleanupTestData();
    }
    
    @AfterAll
    static void tearDownClass() {
        // Clean up test data using thread-safe DAO method
        blockchain.getBlockDAO().cleanupTestData();
        blockchain.getAuthorizedKeyDAO().cleanupTestData();
        JPAUtil.closeEntityManager();
    }
    
    @BeforeEach
    void cleanDatabase() {
        // Clean database before each test to ensure isolation - using thread-safe DAO method
        blockchain.getBlockDAO().cleanupTestData();
        blockchain.getAuthorizedKeyDAO().cleanupTestData();
        
        // Re-add authorized key after cleanup for test to work
        blockchain.addAuthorizedKey(
            CryptoUtil.publicKeyToString(authorizedKeyPair.getPublic()),
            "Test User"
        );
    }
    
    @Test
    @Order(1)
    void testAddPublicBlock() {
        // Add a public (unencrypted) block
        Block publicBlock = blockchain.addBlockAndReturn(
            PUBLIC_DATA,
            authorizedKeyPair.getPrivate(),
            authorizedKeyPair.getPublic()
        );
        
        assertNotNull(publicBlock);
        assertFalse(publicBlock.isDataEncrypted());
        assertEquals(PUBLIC_DATA, publicBlock.getData());
        assertNull(publicBlock.getEncryptionMetadata());
        
        System.out.println("✅ Public block added successfully - ID: " + publicBlock.getId());
    }
    
    @Test
    @Order(2)
    void testAddEncryptedBlock() {
        // Add an encrypted block
        Block encryptedBlock = blockchain.addEncryptedBlock(
            CONFIDENTIAL_DATA,
            ENCRYPTION_PASSWORD,
            authorizedKeyPair.getPrivate(),
            authorizedKeyPair.getPublic()
        );
        
        assertNotNull(encryptedBlock);
        assertTrue(encryptedBlock.isDataEncrypted());
        assertEquals("[ENCRYPTED]", encryptedBlock.getData());
        assertNotNull(encryptedBlock.getEncryptionMetadata());
        assertFalse(encryptedBlock.getEncryptionMetadata().isEmpty());
        
        // Verify the encrypted metadata doesn't contain plaintext
        assertFalse(encryptedBlock.getEncryptionMetadata().contains("John Doe"));
        assertFalse(encryptedBlock.getEncryptionMetadata().contains("123-45-6789"));
        assertFalse(encryptedBlock.getEncryptionMetadata().contains("Hypertension"));
        
        System.out.println("✅ Encrypted block added successfully - ID: " + encryptedBlock.getId());
    }
    
    @Test
    @Order(3)
    void testAddEncryptedBlockWithKeywords() {
        String[] keywords = {"medical", "confidential", "patient", "diabetes"};
        
        Block encryptedBlock = blockchain.addEncryptedBlockWithKeywords(
            CONFIDENTIAL_DATA,
            ENCRYPTION_PASSWORD,
            keywords,
            "MEDICAL",
            authorizedKeyPair.getPrivate(),
            authorizedKeyPair.getPublic()
        );
        
        assertNotNull(encryptedBlock);
        assertTrue(encryptedBlock.isDataEncrypted());
        assertEquals("MEDICAL", encryptedBlock.getContentCategory());
        
        // Keywords should be encrypted in autoKeywords (since no public: prefix)
        // manualKeywords should be null since no public keywords provided
        assertNull(encryptedBlock.getManualKeywords());
        
        // Auto keywords should be encrypted too
        assertNotNull(encryptedBlock.getAutoKeywords());
        assertFalse(encryptedBlock.getAutoKeywords().contains("John")); // Should be encrypted
        
        // Searchable content should be empty for privacy
        assertTrue(encryptedBlock.getSearchableContent().isEmpty());
        
        System.out.println("✅ Encrypted block with keywords added successfully - ID: " + encryptedBlock.getId());
    }
    
    @Test
    @Order(4)
    void testDecryptBlockData() {
        // First add an encrypted block
        Block encryptedBlock = blockchain.addEncryptedBlock(
            CONFIDENTIAL_DATA,
            ENCRYPTION_PASSWORD,
            authorizedKeyPair.getPrivate(),
            authorizedKeyPair.getPublic()
        );
        
        assertNotNull(encryptedBlock);
        
        // Now decrypt it
        String decryptedData = blockchain.getDecryptedBlockData(
            encryptedBlock.getId(),
            ENCRYPTION_PASSWORD
        );
        
        assertNotNull(decryptedData);
        assertEquals(CONFIDENTIAL_DATA, decryptedData);
        
        System.out.println("✅ Block data decrypted successfully");
    }
    
    @Test
    @Order(5)
    void testDecryptWithWrongPassword() {
        // Add encrypted block
        Block encryptedBlock = blockchain.addEncryptedBlock(
            CONFIDENTIAL_DATA,
            ENCRYPTION_PASSWORD,
            authorizedKeyPair.getPrivate(),
            authorizedKeyPair.getPublic()
        );
        
        assertNotNull(encryptedBlock);
        
        // Try to decrypt with wrong password
        String decryptedData = blockchain.getDecryptedBlockData(
            encryptedBlock.getId(),
            "WrongPassword123"
        );
        
        assertNull(decryptedData); // Should fail
        
        System.out.println("✅ Wrong password correctly rejected");
    }
    
    @Test
    @Order(6)
    void testIsBlockEncrypted() {
        // Add public block
        Block publicBlock = blockchain.addBlockAndReturn(
            PUBLIC_DATA,
            authorizedKeyPair.getPrivate(),
            authorizedKeyPair.getPublic()
        );
        
        // Add encrypted block
        Block encryptedBlock = blockchain.addEncryptedBlock(
            CONFIDENTIAL_DATA,
            ENCRYPTION_PASSWORD,
            authorizedKeyPair.getPrivate(),
            authorizedKeyPair.getPublic()
        );
        
        // Test encryption status
        assertFalse(blockchain.isBlockEncrypted(publicBlock.getBlockNumber()));
        assertTrue(blockchain.isBlockEncrypted(encryptedBlock.getBlockNumber()));
        
        System.out.println("✅ Block encryption status correctly detected");
    }
    
    @Test
    @Order(7)
    void testMixedBlockTypes() {
        // Add multiple blocks of different types
        Block publicBlock1 = blockchain.addBlockAndReturn(
            "Public announcement: System maintenance scheduled",
            authorizedKeyPair.getPrivate(),
            authorizedKeyPair.getPublic()
        );
        
        Block encryptedBlock1 = blockchain.addEncryptedBlock(
            "Secret financial data: Budget $1M",
            ENCRYPTION_PASSWORD,
            authorizedKeyPair.getPrivate(),
            authorizedKeyPair.getPublic()
        );
        
        Block publicBlock2 = blockchain.addBlockAndReturn(
            "Public notice: New features available",
            authorizedKeyPair.getPrivate(),
            authorizedKeyPair.getPublic()
        );
        
        Block encryptedBlock2 = blockchain.addEncryptedBlock(
            "Confidential HR data: Employee evaluations",
            ENCRYPTION_PASSWORD,
            authorizedKeyPair.getPrivate(),
            authorizedKeyPair.getPublic()
        );
        
        // Verify all blocks were created correctly
        assertNotNull(publicBlock1);
        assertNotNull(encryptedBlock1);
        assertNotNull(publicBlock2);
        assertNotNull(encryptedBlock2);
        
        // Verify encryption status
        assertFalse(publicBlock1.isDataEncrypted());
        assertTrue(encryptedBlock1.isDataEncrypted());
        assertFalse(publicBlock2.isDataEncrypted());
        assertTrue(encryptedBlock2.isDataEncrypted());
        
        // Verify public data is readable
        assertEquals("Public announcement: System maintenance scheduled", publicBlock1.getData());
        assertEquals("Public notice: New features available", publicBlock2.getData());
        
        // Verify encrypted data is protected
        assertEquals("[ENCRYPTED]", encryptedBlock1.getData());
        assertEquals("[ENCRYPTED]", encryptedBlock2.getData());
        
        // Verify encrypted data can be decrypted
        String decrypted1 = blockchain.getDecryptedBlockData(encryptedBlock1.getId(), ENCRYPTION_PASSWORD);
        String decrypted2 = blockchain.getDecryptedBlockData(encryptedBlock2.getId(), ENCRYPTION_PASSWORD);
        
        assertEquals("Secret financial data: Budget $1M", decrypted1);
        assertEquals("Confidential HR data: Employee evaluations", decrypted2);
        
        System.out.println("✅ Mixed block types (public and encrypted) working correctly");
    }
    
    @Test
    @Order(8)
    void testBlockchainIntegrity() {
        // Verify that adding encrypted blocks doesn't break blockchain integrity
        
        // Get current chain state
        var allBlocks = blockchain.getAllBlocks();
        int initialCount = allBlocks.size();
        
        // Add some more blocks
        blockchain.addBlockAndReturn("Public block", authorizedKeyPair.getPrivate(), authorizedKeyPair.getPublic());
        blockchain.addEncryptedBlock("Private block", ENCRYPTION_PASSWORD, authorizedKeyPair.getPrivate(), authorizedKeyPair.getPublic());
        
        // Verify chain grew
        var newBlocks = blockchain.getAllBlocks();
        assertEquals(initialCount + 2, newBlocks.size());
        
        // Verify chain integrity
        boolean isValid = blockchain.isStructurallyIntact();
        assertTrue(isValid, "Chain should remain valid after adding encrypted blocks");
        
        System.out.println("✅ Blockchain integrity maintained with encrypted blocks");
        System.out.println("   Chain is valid: " + isValid);
    }
    
    @Test
    @Order(9)
    void testEncryptionErrorHandling() {
        // Test with empty password
        assertThrows(IllegalArgumentException.class, () -> {
            blockchain.addEncryptedBlock(
                CONFIDENTIAL_DATA,
                "", // Empty password
                authorizedKeyPair.getPrivate(),
                authorizedKeyPair.getPublic()
            );
        });
        
        // Test with null password
        assertThrows(IllegalArgumentException.class, () -> {
            blockchain.addEncryptedBlock(
                CONFIDENTIAL_DATA,
                null, // Null password
                authorizedKeyPair.getPrivate(),
                authorizedKeyPair.getPublic()
            );
        });
        
        // Test decryption with invalid block ID
        String result = blockchain.getDecryptedBlockData(999999L, ENCRYPTION_PASSWORD);
        assertNull(result);
        
        System.out.println("✅ Encryption error handling working correctly");
    }
    
    @Test
    @Order(10)
    void testPerformanceComparison() {
        // Test performance difference between public and encrypted blocks
        String testData = "Test data for performance comparison. ".repeat(100); // ~3.7KB
        
        // Time public block creation
        long startTime = System.currentTimeMillis();
        Block publicBlock = blockchain.addBlockAndReturn(
            testData,
            authorizedKeyPair.getPrivate(),
            authorizedKeyPair.getPublic()
        );
        long publicTime = System.currentTimeMillis() - startTime;
        
        // Time encrypted block creation
        startTime = System.currentTimeMillis();
        Block encryptedBlock = blockchain.addEncryptedBlock(
            testData,
            ENCRYPTION_PASSWORD,
            authorizedKeyPair.getPrivate(),
            authorizedKeyPair.getPublic()
        );
        long encryptedTime = System.currentTimeMillis() - startTime;
        
        assertNotNull(publicBlock);
        assertNotNull(encryptedBlock);
        
        System.out.printf("✅ Performance comparison completed:%n");
        System.out.printf("   Public block creation: %d ms%n", publicTime);
        System.out.printf("   Encrypted block creation: %d ms%n", encryptedTime);
        System.out.printf("   Encryption overhead: %d ms (%.1fx)%n", 
                         encryptedTime - publicTime, 
                         publicTime > 0 ? (double) encryptedTime / publicTime : 0.0);
        
        // Performance should be reasonable (encrypted blocks shouldn't be >50x slower)
        // Higher threshold because hybrid encryption with ECDSA + AES-GCM is computationally intensive
        assertTrue(encryptedTime < publicTime * 50, "Encrypted blocks shouldn't be more than 50x slower");
    }
}