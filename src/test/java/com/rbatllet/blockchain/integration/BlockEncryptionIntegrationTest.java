package com.rbatllet.blockchain.integration;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.dao.BlockDAO;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.util.JPAUtil;
import org.junit.jupiter.api.*;

import java.security.KeyPair;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for Block encryption functionality
 * Tests the complete flow from creation to storage and retrieval with encryption
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BlockEncryptionIntegrationTest {
    
    private static Blockchain blockchain;
    private static BlockDAO blockDAO;
    private static KeyPair keyPair;
    private static String testPassword;
    private static Long medicalBlockId;
    private static Long financialBlockId;
    
    // Test data
    private static final String MEDICAL_DATA = """
        MEDICAL RECORD - CONFIDENTIAL
        Patient: Jane Doe
        DOB: 1985-03-22
        Medical Record Number: MRN-2024-789456
        
        Chief Complaint: Annual physical examination
        
        Vital Signs:
        - Blood Pressure: 120/80 mmHg
        - Heart Rate: 72 bpm
        - Temperature: 98.6°F
        - Weight: 68 kg
        - Height: 165 cm
        
        Current Medications:
        - Lisinopril 10mg daily for hypertension
        - Metformin 500mg twice daily for Type 2 diabetes
        
        Lab Results:
        - HbA1c: 6.8% (target <7.0%)
        - Cholesterol: 180 mg/dL (normal)
        - Triglycerides: 150 mg/dL (borderline)
        
        Assessment: Well-controlled diabetes and hypertension
        Plan: Continue current medications, follow-up in 6 months
        
        Physician: Dr. Smith, MD
        Date: 2024-01-15
        """;
    
    private static final String FINANCIAL_DATA = """
        FINANCIAL TRANSACTION - HIGHLY CONFIDENTIAL
        
        Transaction ID: TXN-2024-987654
        Date: 2024-01-15 14:30:22
        
        Account Details:
        - Account Number: ****-****-****-3456
        - Account Holder: ACME Corporation Ltd.
        - IBAN: GB29 NWBK 6016 1331 9268 19
        
        Transaction Details:
        - Type: Wire Transfer (International)
        - Amount: €250,000.00
        - Currency: EUR
        - Exchange Rate: 1.0845 EUR/USD
        - USD Equivalent: $271,125.00
        
        Counterparty:
        - Name: Global Tech Solutions S.A.
        - Account: FR14 2004 1010 0505 0001 3M02 606
        - SWIFT: BNPAFRPPXXX
        
        Purpose: Software licensing and development services Q1 2024
        Reference: Contract GTS-2024-001, Invoice INV-2024-0123
        
        Risk Assessment:
        - AML Score: Low Risk (Score: 15/100)
        - Sanctions Check: Clear
        - PEP Check: Negative
        
        Authorized by: John Manager (CEO)
        Processed by: Finance Dept.
        Status: Completed
        """;
    
    @BeforeAll
    static void setUpClass() {
        blockchain = new Blockchain();
        blockDAO = new BlockDAO();
        keyPair = CryptoUtil.generateKeyPair();
        testPassword = "SecureTestPassword123!@#";
        
        // Ensure clean database state using thread-safe DAO method
        blockchain.getBlockDAO().cleanupTestData();
        blockchain.getAuthorizedKeyDAO().cleanupTestData();
    }
    
    @AfterAll
    static void tearDownClass() {
        JPAUtil.closeEntityManager();
    }
    
    @Test
    @Order(1)
    void testSaveEncryptedMedicalBlock() {
        // Create a block with medical data
        Block medicalBlock = new Block(
            1L,
            "0000000000000000000000000000000000000000000000000000000000000000",
            MEDICAL_DATA,
            LocalDateTime.now(),
            "test_hash_medical",
            CryptoUtil.signData(MEDICAL_DATA, keyPair.getPrivate()),
            CryptoUtil.publicKeyToString(keyPair.getPublic())
        );
        
        medicalBlock.setContentCategory("MEDICAL");
        medicalBlock.setManualKeywords("medical record, patient, confidential, diabetes, hypertension");
        
        // Save with encryption
        assertDoesNotThrow(() -> {
            blockDAO.saveBlockWithEncryption(medicalBlock, testPassword);
        });
        
        // Verify the block was saved and encrypted
        assertTrue(medicalBlock.isDataEncrypted());
        assertEquals("[ENCRYPTED]", medicalBlock.getData());
        assertNotNull(medicalBlock.getEncryptionMetadata());
        assertFalse(medicalBlock.getEncryptionMetadata().isEmpty());
        
        medicalBlockId = medicalBlock.getId();
        System.out.println("✅ Medical block saved with encryption - ID: " + medicalBlockId);
    }
    
    @Test
    @Order(2)
    void testSaveEncryptedFinancialBlock() {
        // Create a block with financial data
        Block financialBlock = new Block(
            2L,
            "test_hash_medical",
            FINANCIAL_DATA,
            LocalDateTime.now(),
            "test_hash_financial",
            CryptoUtil.signData(FINANCIAL_DATA, keyPair.getPrivate()),
            CryptoUtil.publicKeyToString(keyPair.getPublic())
        );
        
        financialBlock.setContentCategory("FINANCE");
        financialBlock.setManualKeywords("financial transaction, wire transfer, confidential, international, EUR");
        
        // Save with encryption
        assertDoesNotThrow(() -> {
            blockDAO.saveBlockWithEncryption(financialBlock, testPassword);
        });
        
        // Verify the block was saved and encrypted
        assertTrue(financialBlock.isDataEncrypted());
        assertEquals("[ENCRYPTED]", financialBlock.getData());
        assertNotNull(financialBlock.getEncryptionMetadata());
        
        financialBlockId = financialBlock.getId();
        System.out.println("✅ Financial block saved with encryption - ID: " + financialBlockId);
    }
    
    @Test
    @Order(3)
    void testRetrieveAndDecryptMedicalBlock() {
        // Retrieve the first block (medical data)
        Block retrievedBlock = blockDAO.getBlockWithDecryption(medicalBlockId, testPassword);
        
        assertNotNull(retrievedBlock);
        assertEquals(MEDICAL_DATA, retrievedBlock.getData());
        assertEquals("MEDICAL", retrievedBlock.getContentCategory());
        assertTrue(retrievedBlock.isDataEncrypted()); // Should still be marked as encrypted
        
        System.out.println("✅ Medical block decrypted successfully");
        System.out.println("Data preview: " + retrievedBlock.getData().substring(0, 100) + "...");
    }
    
    @Test
    @Order(4)
    void testRetrieveAndDecryptFinancialBlock() {
        // Retrieve the second block (financial data)
        Block retrievedBlock = blockDAO.getBlockWithDecryption(financialBlockId, testPassword);
        
        assertNotNull(retrievedBlock);
        assertEquals(FINANCIAL_DATA, retrievedBlock.getData());
        assertEquals("FINANCE", retrievedBlock.getContentCategory());
        assertTrue(retrievedBlock.isDataEncrypted());
        
        System.out.println("✅ Financial block decrypted successfully");
        System.out.println("Data preview: " + retrievedBlock.getData().substring(0, 100) + "...");
    }
    
    @Test
    @Order(5)
    void testWrongPasswordFailsDecryption() {
        // Try to decrypt with wrong password - should return null instead of throwing exception
        Block result = blockDAO.getBlockWithDecryption(medicalBlockId, "WrongPassword123");

        // Wrong password should return null (graceful handling of Tag mismatch)
        assertNull(result, "Wrong password should return null instead of throwing exception");

        System.out.println("✅ Wrong password correctly returns null (graceful Tag mismatch handling)");
    }
    
    @Test
    @Order(6)
    void testPasswordVerification() {
        // Test correct password
        assertTrue(blockDAO.verifyBlockPassword(medicalBlockId, testPassword));
        assertTrue(blockDAO.verifyBlockPassword(financialBlockId, testPassword));
        
        // Test wrong password
        assertFalse(blockDAO.verifyBlockPassword(medicalBlockId, "WrongPassword"));
        assertFalse(blockDAO.verifyBlockPassword(financialBlockId, "AnotherWrongPassword"));
        
        // Test non-existent block
        assertFalse(blockDAO.verifyBlockPassword(999L, testPassword));
        
        System.out.println("✅ Password verification working correctly");
    }


    @Test
    @Order(8)
    void testEncryptExistingBlock() {
        // Create an unencrypted block first
        Block unencryptedBlock = new Block(
            3L,
            "test_hash_financial",
            "This is some unencrypted test data that should be encrypted retroactively.",
            LocalDateTime.now(),
            "test_hash_unencrypted",
            CryptoUtil.signData("test data", keyPair.getPrivate()),
            CryptoUtil.publicKeyToString(keyPair.getPublic())
        );
        
        unencryptedBlock.setContentCategory("TEST");
        
        // Save without encryption
        blockDAO.saveBlock(unencryptedBlock);
        assertFalse(unencryptedBlock.isDataEncrypted());
        
        // Now encrypt the existing block
        boolean encrypted = blockDAO.encryptExistingBlock(unencryptedBlock.getId(), testPassword);
        assertTrue(encrypted);

        // Verify it's now encrypted
        Block encryptedBlock = blockDAO.getBlockByNumber(unencryptedBlock.getBlockNumber());
        assertTrue(encryptedBlock.isDataEncrypted());
        assertEquals("[ENCRYPTED]", encryptedBlock.getData());
        
        // Verify we can decrypt it
        Block decryptedBlock = blockDAO.getBlockWithDecryption(unencryptedBlock.getId(), testPassword);
        assertEquals("This is some unencrypted test data that should be encrypted retroactively.", 
                    decryptedBlock.getData());
        
        System.out.println("✅ Successfully encrypted existing unencrypted block");
    }
    
    @Test
    @Order(9)
    void testDataIntegrityAndSecurity() {
        // Test that encrypted data cannot be read without proper decryption
        Block encryptedBlock = blockDAO.getBlockByNumber(1L);

        // The raw encrypted data should not contain any plaintext
        String encryptionMetadata = encryptedBlock.getEncryptionMetadata();
        assertNotNull(encryptionMetadata);
        
        // Check that sensitive information is not visible in encrypted form
        assertFalse(encryptionMetadata.contains("Jane Doe"));
        assertFalse(encryptionMetadata.contains("Medical Record"));
        assertFalse(encryptionMetadata.contains("Lisinopril"));
        assertFalse(encryptionMetadata.contains("diabetes"));
        
        System.out.println("✅ Data integrity verified - no plaintext leaked in encrypted data");
    }
    
    @Test
    @Order(10)
    void testEncryptionPerformance() {
        long startTime = System.currentTimeMillis();
        
        // Create and encrypt 10 blocks with medical data
        for (int i = 10; i < 20; i++) {
            Block block = new Block(
                (long) i,
                "previous_hash_" + (i-1),
                MEDICAL_DATA + " - Patient #" + i,
                LocalDateTime.now(),
                "hash_" + i,
                CryptoUtil.signData("data_" + i, keyPair.getPrivate()),
                CryptoUtil.publicKeyToString(keyPair.getPublic())
            );
            
            block.setContentCategory("MEDICAL");
            blockDAO.saveBlockWithEncryption(block, testPassword);
        }
        
        long encryptionTime = System.currentTimeMillis() - startTime;
        
        // Now decrypt all 10 blocks
        startTime = System.currentTimeMillis();
        for (long i = 10; i < 20; i++) {
            // First get the block by number to find its ID
            Block block = blockDAO.getBlockByNumber(i);
            assertNotNull(block, "Block with number " + i + " should exist");

            Block decrypted = blockDAO.getBlockWithDecryption(block.getId(), testPassword);
            assertNotNull(decrypted);
            assertTrue(decrypted.getData().contains("Patient #" + i));
        }
        long decryptionTime = System.currentTimeMillis() - startTime;
        
        System.out.printf("✅ Performance test completed - Encryption: %dms, Decryption: %dms%n", 
                         encryptionTime, decryptionTime);
        
        // Performance assertions (reasonable thresholds)
        assertTrue(encryptionTime < 5000, "Encryption should complete within 5 seconds");
        assertTrue(decryptionTime < 3000, "Decryption should complete within 3 seconds");
    }
}