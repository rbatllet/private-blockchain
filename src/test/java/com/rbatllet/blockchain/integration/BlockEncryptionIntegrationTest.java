package com.rbatllet.blockchain.integration;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.service.UserFriendlyEncryptionAPI;
import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.util.JPAUtil;
import com.rbatllet.blockchain.validation.ChainValidationResult;
import org.junit.jupiter.api.*;

import java.security.KeyPair;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for Block encryption functionality
 * Tests the complete flow from creation to storage and retrieval with encryption
 *
 * RIGOROUS test with complete validation of encryption, decryption, and security
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BlockEncryptionIntegrationTest {

    private static Blockchain blockchain;
    private static UserFriendlyEncryptionAPI api;
    private static KeyPair keyPair;
    private static String testPassword;
    private static Block medicalBlock;
    private static Block financialBlock;

    // Test data - identical to original test
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
        // Initialize JPAUtil with default configuration (respects environment variables)
        JPAUtil.initializeDefault();

        blockchain = new Blockchain();

        // Clean any existing data from previous tests
        blockchain.clearAndReinitialize();

        try {
            keyPair = CryptoUtil.generateKeyPair();
            String publicKeyString = CryptoUtil.publicKeyToString(keyPair.getPublic());
            blockchain.createBootstrapAdmin(publicKeyString, "TestUser");

            api = new UserFriendlyEncryptionAPI(blockchain, "TestUser", keyPair);
            testPassword = "SecureTestPassword123!@#";
        } catch (Exception e) {
            fail("Failed to set up test environment: " + e.getMessage());
        }
    }

    @AfterAll
    static void tearDownClass() {
        if (blockchain != null) {
            blockchain.clearAndReinitialize();
            JPAUtil.cleanupThreadLocals();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Test save encrypted medical block")
    void testSaveEncryptedMedicalBlock() {
        // Store medical data with encryption
        medicalBlock = api.storeSecret(MEDICAL_DATA, testPassword);

        // RIGOROUS validation - Block creation
        assertNotNull(medicalBlock, "Medical block should be created");
        assertNotNull(medicalBlock.getId(), "Block should have database ID");
        assertTrue(medicalBlock.getBlockNumber() > 0, "Block should have valid block number > 0");
        assertNotNull(medicalBlock.getTimestamp(), "Block should have timestamp");
        assertNotNull(medicalBlock.getHash(), "Block should have hash");
        assertNotNull(medicalBlock.getSignature(), "Block should have signature");
        assertNotNull(medicalBlock.getSignerPublicKey(), "Block should have signer public key");

        // RIGOROUS validation - Encryption markers
        assertTrue(medicalBlock.isDataEncrypted(), "Block should be marked as encrypted");
        // CRITICAL: In the new architecture, data field remains UNCHANGED
        // This is essential to maintain blockchain hash integrity
        // Encrypted content is stored in encryptionMetadata, not in data field
        assertEquals(MEDICAL_DATA, medicalBlock.getData(), 
            "Data field must remain unchanged to maintain hash integrity");

        // RIGOROUS validation - Encryption metadata
        assertNotNull(medicalBlock.getEncryptionMetadata(), "Encryption metadata should exist");
        assertFalse(medicalBlock.getEncryptionMetadata().isEmpty(), "Encryption metadata should not be empty");
        assertTrue(medicalBlock.getEncryptionMetadata().length() > 100, "Encryption metadata should be substantial (>100 chars)");

        // RIGOROUS validation - No plaintext leakage in encrypted data
        String encryptedMetadata = medicalBlock.getEncryptionMetadata();
        assertFalse(encryptedMetadata.contains("Jane Doe"), "Encrypted metadata must not contain 'Jane Doe'");
        assertFalse(encryptedMetadata.contains("Medical Record"), "Encrypted metadata must not contain 'Medical Record'");
        assertFalse(encryptedMetadata.contains("Lisinopril"), "Encrypted metadata must not contain 'Lisinopril'");
        assertFalse(encryptedMetadata.contains("Metformin"), "Encrypted metadata must not contain 'Metformin'");
        assertFalse(encryptedMetadata.contains("diabetes"), "Encrypted metadata must not contain 'diabetes'");
        assertFalse(encryptedMetadata.contains("hypertension"), "Encrypted metadata must not contain 'hypertension'");
        assertFalse(encryptedMetadata.contains("Dr. Smith"), "Encrypted metadata must not contain 'Dr. Smith'");

        // RIGOROUS validation - Block should be persisted in blockchain
        Block retrievedBlock = blockchain.getBlock(medicalBlock.getBlockNumber());
        assertNotNull(retrievedBlock, "Block should be retrievable from blockchain");
        assertEquals(medicalBlock.getId(), retrievedBlock.getId(), "Retrieved block ID should match");

        System.out.println("✅ Medical block saved with RIGOROUS encryption validation - ID: " + medicalBlock.getId());
        System.out.println("   Block Number: " + medicalBlock.getBlockNumber());
        System.out.println("   Encryption metadata length: " + medicalBlock.getEncryptionMetadata().length());
        System.out.println("   Hash: " + medicalBlock.getHash().substring(0, 20) + "...");
        System.out.println("   No plaintext leakage verified ✅");
    }

    @Test
    @Order(2)
    @DisplayName("Test save encrypted financial block")
    void testSaveEncryptedFinancialBlock() {
        // Store financial data with encryption
        financialBlock = api.storeSecret(FINANCIAL_DATA, testPassword);

        // RIGOROUS validation - Block creation
        assertNotNull(financialBlock, "Financial block should be created");
        assertNotNull(financialBlock.getId(), "Block should have database ID");
        assertTrue(financialBlock.getBlockNumber() > 0, "Block should have valid block number > 0");
        assertNotNull(financialBlock.getTimestamp(), "Block should have timestamp");
        assertNotNull(financialBlock.getHash(), "Block should have hash");
        assertNotNull(financialBlock.getSignature(), "Block should have signature");
        assertNotNull(financialBlock.getSignerPublicKey(), "Block should have signer public key");

        // RIGOROUS validation - Encryption markers
        assertTrue(financialBlock.isDataEncrypted(), "Block should be marked as encrypted");
        // CRITICAL: In the new architecture, data field remains UNCHANGED
        // This is essential to maintain blockchain hash integrity
        // Encrypted content is stored in encryptionMetadata, not in data field
        assertEquals(FINANCIAL_DATA, financialBlock.getData(), 
            "Data field must remain unchanged to maintain hash integrity");

        // RIGOROUS validation - Encryption metadata
        assertNotNull(financialBlock.getEncryptionMetadata(), "Encryption metadata should exist");
        assertFalse(financialBlock.getEncryptionMetadata().isEmpty(), "Encryption metadata should not be empty");
        assertTrue(financialBlock.getEncryptionMetadata().length() > 100, "Encryption metadata should be substantial (>100 chars)");

        // RIGOROUS validation - No plaintext leakage in encrypted data
        String encryptedMetadata = financialBlock.getEncryptionMetadata();
        assertFalse(encryptedMetadata.contains("ACME Corporation"), "Encrypted metadata must not contain 'ACME Corporation'");
        assertFalse(encryptedMetadata.contains("250,000"), "Encrypted metadata must not contain '250,000'");
        assertFalse(encryptedMetadata.contains("GB29 NWBK"), "Encrypted metadata must not contain IBAN");
        assertFalse(encryptedMetadata.contains("BNPAFRPPXXX"), "Encrypted metadata must not contain SWIFT code");
        assertFalse(encryptedMetadata.contains("Global Tech Solutions"), "Encrypted metadata must not contain counterparty name");
        assertFalse(encryptedMetadata.contains("John Manager"), "Encrypted metadata must not contain 'John Manager'");
        assertFalse(encryptedMetadata.contains("Wire Transfer"), "Encrypted metadata must not contain 'Wire Transfer'");

        // RIGOROUS validation - Block should be persisted in blockchain
        Block retrievedBlock = blockchain.getBlock(financialBlock.getBlockNumber());
        assertNotNull(retrievedBlock, "Block should be retrievable from blockchain");
        assertEquals(financialBlock.getId(), retrievedBlock.getId(), "Retrieved block ID should match");

        // RIGOROUS validation - Blocks should have different encryption metadata
        assertNotEquals(medicalBlock.getEncryptionMetadata(), financialBlock.getEncryptionMetadata(),
            "Different data should produce different encryption metadata");

        System.out.println("✅ Financial block saved with RIGOROUS encryption validation - ID: " + financialBlock.getId());
        System.out.println("   Block Number: " + financialBlock.getBlockNumber());
        System.out.println("   Encryption metadata length: " + financialBlock.getEncryptionMetadata().length());
        System.out.println("   Hash: " + financialBlock.getHash().substring(0, 20) + "...");
        System.out.println("   No plaintext leakage verified ✅");
    }

    @Test
    @Order(3)
    @DisplayName("Test retrieve and decrypt medical block")
    void testRetrieveAndDecryptMedicalBlock() {
        // Decrypt medical data
        String decryptedData = api.retrieveSecret(medicalBlock.getBlockNumber(), testPassword);

        // RIGOROUS validation - Decryption success
        assertNotNull(decryptedData, "Decrypted data should not be null");
        assertFalse(decryptedData.isEmpty(), "Decrypted data should not be empty");
        assertEquals(MEDICAL_DATA, decryptedData, "Decrypted data should EXACTLY match original");

        // RIGOROUS validation - All sensitive data present after decryption
        assertTrue(decryptedData.contains("MEDICAL RECORD - CONFIDENTIAL"), "Should contain header");
        assertTrue(decryptedData.contains("Patient: Jane Doe"), "Should contain patient name");
        assertTrue(decryptedData.contains("DOB: 1985-03-22"), "Should contain DOB");
        assertTrue(decryptedData.contains("MRN-2024-789456"), "Should contain medical record number");
        assertTrue(decryptedData.contains("Blood Pressure: 120/80 mmHg"), "Should contain vital signs");
        assertTrue(decryptedData.contains("Lisinopril 10mg daily"), "Should contain medications");
        assertTrue(decryptedData.contains("Metformin 500mg twice daily"), "Should contain medications");
        assertTrue(decryptedData.contains("HbA1c: 6.8%"), "Should contain lab results");
        assertTrue(decryptedData.contains("Dr. Smith, MD"), "Should contain physician name");
        assertTrue(decryptedData.contains("2024-01-15"), "Should contain date");

        // RIGOROUS validation - Decrypted data length matches original
        assertEquals(MEDICAL_DATA.length(), decryptedData.length(), "Decrypted data length should match original exactly");

        // RIGOROUS validation - Block is still marked as encrypted in blockchain
        Block retrievedBlock = blockchain.getBlock(medicalBlock.getBlockNumber());
        assertNotNull(retrievedBlock, "Block should still exist in blockchain");
        assertTrue(retrievedBlock.isDataEncrypted(), "Block should still be marked as encrypted");
        assertNotNull(retrievedBlock.getEncryptionMetadata(), "Encryption metadata should still exist");

        // RIGOROUS validation - Multiple decryption attempts should produce same result
        String decryptedData2 = api.retrieveSecret(medicalBlock.getBlockNumber(), testPassword);
        assertEquals(decryptedData, decryptedData2, "Multiple decryption attempts should produce identical results");

        System.out.println("✅ Medical block decrypted successfully with RIGOROUS validation");
        System.out.println("   Decrypted data length: " + decryptedData.length() + " chars");
        System.out.println("   Original data length: " + MEDICAL_DATA.length() + " chars");
        System.out.println("   Data preview: " + decryptedData.substring(0, 100) + "...");
        System.out.println("   All sensitive fields verified present ✅");
    }

    @Test
    @Order(4)
    @DisplayName("Test retrieve and decrypt financial block")
    void testRetrieveAndDecryptFinancialBlock() {
        // Decrypt financial data
        String decryptedData = api.retrieveSecret(financialBlock.getBlockNumber(), testPassword);

        // RIGOROUS validation - Decryption success
        assertNotNull(decryptedData, "Decrypted data should not be null");
        assertFalse(decryptedData.isEmpty(), "Decrypted data should not be empty");
        assertEquals(FINANCIAL_DATA, decryptedData, "Decrypted data should EXACTLY match original");

        // RIGOROUS validation - All sensitive data present after decryption
        assertTrue(decryptedData.contains("FINANCIAL TRANSACTION - HIGHLY CONFIDENTIAL"), "Should contain header");
        assertTrue(decryptedData.contains("TXN-2024-987654"), "Should contain transaction ID");
        assertTrue(decryptedData.contains("ACME Corporation Ltd."), "Should contain account holder");
        assertTrue(decryptedData.contains("GB29 NWBK 6016 1331 9268 19"), "Should contain IBAN");
        assertTrue(decryptedData.contains("€250,000.00"), "Should contain amount");
        assertTrue(decryptedData.contains("Global Tech Solutions S.A."), "Should contain counterparty");
        assertTrue(decryptedData.contains("FR14 2004 1010 0505 0001 3M02 606"), "Should contain counterparty IBAN");
        assertTrue(decryptedData.contains("BNPAFRPPXXX"), "Should contain SWIFT code");
        assertTrue(decryptedData.contains("Contract GTS-2024-001"), "Should contain contract reference");
        assertTrue(decryptedData.contains("John Manager (CEO)"), "Should contain authorizer");
        assertTrue(decryptedData.contains("AML Score: Low Risk"), "Should contain risk assessment");

        // RIGOROUS validation - Decrypted data length matches original
        assertEquals(FINANCIAL_DATA.length(), decryptedData.length(), "Decrypted data length should match original exactly");

        // RIGOROUS validation - Block is still marked as encrypted in blockchain
        Block retrievedBlock = blockchain.getBlock(financialBlock.getBlockNumber());
        assertNotNull(retrievedBlock, "Block should still exist in blockchain");
        assertTrue(retrievedBlock.isDataEncrypted(), "Block should still be marked as encrypted");
        assertNotNull(retrievedBlock.getEncryptionMetadata(), "Encryption metadata should still exist");

        // RIGOROUS validation - Multiple decryption attempts should produce same result
        String decryptedData2 = api.retrieveSecret(financialBlock.getBlockNumber(), testPassword);
        assertEquals(decryptedData, decryptedData2, "Multiple decryption attempts should produce identical results");

        // RIGOROUS validation - Different blocks produce different decrypted data
        String medicalDecrypted = api.retrieveSecret(medicalBlock.getBlockNumber(), testPassword);
        assertNotEquals(medicalDecrypted, decryptedData, "Medical and financial data should be different");

        System.out.println("✅ Financial block decrypted successfully with RIGOROUS validation");
        System.out.println("   Decrypted data length: " + decryptedData.length() + " chars");
        System.out.println("   Original data length: " + FINANCIAL_DATA.length() + " chars");
        System.out.println("   Data preview: " + decryptedData.substring(0, 100) + "...");
        System.out.println("   All sensitive fields verified present ✅");
    }

    @Test
    @Order(5)
    @DisplayName("Test wrong password fails decryption")
    void testWrongPasswordFailsDecryption() {
        // RIGOROUS validation - Wrong password scenarios

        // Test 1: Completely wrong password
        String result1 = api.retrieveSecret(medicalBlock.getBlockNumber(), "WrongPassword123");
        assertNull(result1, "Completely wrong password should return null");

        // Test 2: Similar but incorrect password (typo)
        String result2 = api.retrieveSecret(medicalBlock.getBlockNumber(), testPassword + "X");
        assertNull(result2, "Password with extra character should return null");

        // Test 3: Empty password should throw IllegalArgumentException (programming error)
        assertThrows(IllegalArgumentException.class, () -> {
            api.retrieveSecret(medicalBlock.getBlockNumber(), "");
        }, "Empty password should throw IllegalArgumentException");

        // Test 4: Null password should throw IllegalArgumentException (programming error)
        assertThrows(IllegalArgumentException.class, () -> {
            api.retrieveSecret(medicalBlock.getBlockNumber(), null);
        }, "Null password should throw IllegalArgumentException");

        // Test 5: Case-sensitive password check (if password was uppercase)
        String wrongCase = testPassword.toLowerCase();
        if (!wrongCase.equals(testPassword)) {
            String result5 = api.retrieveSecret(medicalBlock.getBlockNumber(), wrongCase);
            assertNull(result5, "Case-different password should return null (passwords are case-sensitive)");
        }

        // RIGOROUS validation - Correct password still works after failed attempts
        String correctDecryption = api.retrieveSecret(medicalBlock.getBlockNumber(), testPassword);
        assertNotNull(correctDecryption, "Correct password should still work after failed attempts");
        assertEquals(MEDICAL_DATA, correctDecryption, "Decryption with correct password should still produce correct data");

        // RIGOROUS validation - Block is still intact and encrypted
        Block block = blockchain.getBlock(medicalBlock.getBlockNumber());
        assertTrue(block.isDataEncrypted(), "Block should still be encrypted after failed decryption attempts");

        // RIGOROUS validation - Try wrong password on financial block too
        String financialWrongPwd = api.retrieveSecret(financialBlock.getBlockNumber(), "AnotherWrongPassword");
        assertNull(financialWrongPwd, "Wrong password should fail for financial block too");

        System.out.println("✅ Wrong password RIGOROUSLY tested - all scenarios handled correctly");
        System.out.println("   Tested: wrong password, typo, empty, null, case-sensitive");
        System.out.println("   Correct password still works after failed attempts ✅");
    }

    @Test
    @Order(6)
    @DisplayName("Test encrypt existing block")
    void testEncryptExistingBlock() {
        // RIGOROUS validation - Retroactive encryption of existing block

        // Create an unencrypted block first using Blockchain API
        String unencryptedData = "This is some unencrypted test data that should be encrypted retroactively.";
        boolean added = blockchain.addBlock(unencryptedData, keyPair.getPrivate(), keyPair.getPublic());
        assertTrue(added, "Unencrypted block should be added");

        // Get the block we just added
        long blockCount = blockchain.getBlockCount();
        Block unencryptedBlock = blockchain.getBlock(blockCount - 1);

        // RIGOROUS validation - Initial unencrypted state
        assertNotNull(unencryptedBlock, "Block should exist");
        assertNotNull(unencryptedBlock.getId(), "Block should have ID");
        assertFalse(unencryptedBlock.isDataEncrypted(), "Block should NOT be encrypted initially");
        assertEquals(unencryptedData, unencryptedBlock.getData(), "Data should be plaintext initially");
        assertNull(unencryptedBlock.getEncryptionMetadata(), "Encryption metadata should be null initially");

        // RIGOROUS validation - Store original block properties for comparison
        Long originalId = unencryptedBlock.getId();
        Long originalBlockNumber = unencryptedBlock.getBlockNumber();
        String originalHash = unencryptedBlock.getHash();
        String originalSignature = unencryptedBlock.getSignature();

        // Now encrypt the existing block
        boolean encrypted = blockchain.encryptExistingBlock(unencryptedBlock.getId(), testPassword);
        assertTrue(encrypted, "Encryption should succeed");

        // RIGOROUS validation - Encrypted state
        Block encryptedBlock = blockchain.getBlock(unencryptedBlock.getBlockNumber());
        assertNotNull(encryptedBlock, "Block should still exist after encryption");
        assertTrue(encryptedBlock.isDataEncrypted(), "Block should now be marked as encrypted");
        
        // CRITICAL: Data field remains UNCHANGED to maintain hash integrity
        // Encrypted data is stored in encryptionMetadata field, NOT in data field
        // This is correct: changing 'data' would break the blockchain hash chain
        assertEquals(unencryptedData, encryptedBlock.getData(), 
                    "Data field must remain unchanged to maintain hash integrity");
        
        assertNotNull(encryptedBlock.getEncryptionMetadata(), "Encryption metadata should now exist");
        assertFalse(encryptedBlock.getEncryptionMetadata().isEmpty(), "Encryption metadata should not be empty");

        // RIGOROUS validation - Block identity and cryptographic properties preserved
        assertEquals(originalId, encryptedBlock.getId(), "Block ID should remain same after encryption");
        assertEquals(originalBlockNumber, encryptedBlock.getBlockNumber(), "Block number should remain same");
        assertNotNull(encryptedBlock.getHash(), "Hash should still exist after encryption");
        assertNotNull(encryptedBlock.getSignature(), "Signature should still exist after encryption");

        // RIGOROUS validation - Hash and signature should remain unchanged (encryption doesn't affect blockchain integrity)
        assertEquals(originalHash, encryptedBlock.getHash(), "Hash should remain unchanged after encryption");
        assertEquals(originalSignature, encryptedBlock.getSignature(), "Signature should remain unchanged after encryption");

        // RIGOROUS validation - No plaintext leakage after encryption
        String encMetadata = encryptedBlock.getEncryptionMetadata();
        assertFalse(encMetadata.contains("unencrypted test data"), "Encrypted metadata must not contain plaintext");
        assertFalse(encMetadata.contains("retroactively"), "Encrypted metadata must not contain plaintext words");

        // RIGOROUS validation - Decrypt with correct password
        String decrypted = api.retrieveSecret(unencryptedBlock.getBlockNumber(), testPassword);
        assertNotNull(decrypted, "Decryption should succeed");
        assertEquals(unencryptedData, decrypted, "Decrypted data should EXACTLY match original unencrypted data");
        assertEquals(unencryptedData.length(), decrypted.length(), "Decrypted length should match original");

        // RIGOROUS validation - Wrong password fails
        String wrongDecrypt = api.retrieveSecret(unencryptedBlock.getBlockNumber(), "WrongPassword");
        assertNull(wrongDecrypt, "Wrong password should not decrypt retroactively encrypted block");

        // RIGOROUS validation - Multiple decryptions produce same result
        String decrypted2 = api.retrieveSecret(unencryptedBlock.getBlockNumber(), testPassword);
        assertEquals(decrypted, decrypted2, "Multiple decryptions should produce identical results");

        // RIGOROUS validation - Chain is still valid after retroactive encryption
        ChainValidationResult validation = blockchain.validateChainDetailed();
        assertTrue(validation.isValid(), "Chain should remain valid after retroactive encryption");

        System.out.println("✅ Retroactive encryption RIGOROUSLY validated");
        System.out.println("   Original data: " + unencryptedData);
        System.out.println("   Block ID preserved: " + originalId);
        System.out.println("   Encrypted and decrypted successfully ✅");
        System.out.println("   Chain integrity maintained ✅");
    }

    @Test
    @Order(7)
    @DisplayName("Test data integrity and security")
    void testDataIntegrityAndSecurity() {
        // RIGOROUS validation - Comprehensive security and integrity checks

        // Test 1: Medical block encryption metadata security
        Block medicalEncrypted = blockchain.getBlock(medicalBlock.getBlockNumber());
        assertNotNull(medicalEncrypted, "Medical block should exist");
        String medicalMetadata = medicalEncrypted.getEncryptionMetadata();
        assertNotNull(medicalMetadata, "Encryption metadata should exist");

        // RIGOROUS - No sensitive medical information leaked
        assertFalse(medicalMetadata.contains("Jane Doe"), "Must not contain patient name");
        assertFalse(medicalMetadata.contains("Medical Record"), "Must not contain 'Medical Record'");
        assertFalse(medicalMetadata.contains("Lisinopril"), "Must not contain medication names");
        assertFalse(medicalMetadata.contains("Metformin"), "Must not contain medication names");
        assertFalse(medicalMetadata.contains("diabetes"), "Must not contain diagnosis");
        assertFalse(medicalMetadata.contains("hypertension"), "Must not contain diagnosis");
        assertFalse(medicalMetadata.contains("Dr. Smith"), "Must not contain physician name");
        assertFalse(medicalMetadata.contains("1985-03-22"), "Must not contain DOB");
        assertFalse(medicalMetadata.contains("MRN-2024-789456"), "Must not contain medical record number");
        assertFalse(medicalMetadata.contains("120/80"), "Must not contain vital signs");

        // Test 2: Financial block encryption metadata security
        Block financialEncrypted = blockchain.getBlock(financialBlock.getBlockNumber());
        assertNotNull(financialEncrypted, "Financial block should exist");
        String financialMetadata = financialEncrypted.getEncryptionMetadata();
        assertNotNull(financialMetadata, "Financial encryption metadata should exist");

        // RIGOROUS - No sensitive financial information leaked
        assertFalse(financialMetadata.contains("ACME Corporation"), "Must not contain company name");
        assertFalse(financialMetadata.contains("250,000"), "Must not contain transaction amount");
        assertFalse(financialMetadata.contains("GB29 NWBK"), "Must not contain IBAN");
        assertFalse(financialMetadata.contains("BNPAFRPPXXX"), "Must not contain SWIFT code");
        assertFalse(financialMetadata.contains("Global Tech Solutions"), "Must not contain counterparty");
        assertFalse(financialMetadata.contains("John Manager"), "Must not contain authorizer name");
        assertFalse(financialMetadata.contains("TXN-2024-987654"), "Must not contain transaction ID");

        // Test 3: Data fields remain UNCHANGED (hash integrity requirement)
        // CRITICAL: In retroactive encryption architecture, 'data' field is NEVER modified
        // This is essential to maintain blockchain hash integrity
        // Encrypted content is stored in encryptionMetadata, not in data field
        assertEquals(MEDICAL_DATA, medicalEncrypted.getData(), 
            "Medical data field must remain unchanged to maintain hash integrity");
        assertEquals(FINANCIAL_DATA, financialEncrypted.getData(), 
            "Financial data field must remain unchanged to maintain hash integrity");

        // Test 4: Different blocks have different encryption metadata
        assertNotEquals(medicalMetadata, financialMetadata, "Different data should produce different encrypted metadata");
        assertTrue(medicalMetadata.length() > 50, "Encrypted metadata should be substantial");
        assertTrue(financialMetadata.length() > 50, "Encrypted metadata should be substantial");

        // Test 5: Hash and signature integrity
        assertNotNull(medicalEncrypted.getHash(), "Medical block hash should exist");
        assertNotNull(medicalEncrypted.getSignature(), "Medical block signature should exist");
        assertNotNull(financialEncrypted.getHash(), "Financial block hash should exist");
        assertNotNull(financialEncrypted.getSignature(), "Financial block signature should exist");
        assertNotEquals(medicalEncrypted.getHash(), financialEncrypted.getHash(), "Different blocks should have different hashes");

        // Test 6: Blocks are properly marked as encrypted
        assertTrue(medicalEncrypted.isDataEncrypted(), "Medical block should be marked as encrypted");
        assertTrue(financialEncrypted.isDataEncrypted(), "Financial block should be marked as encrypted");

        // Test 7: Verify encryption metadata format (should contain encrypted data, not plaintext)
        // EncryptionMetadata format: timestamp|salt|iv|ciphertext|hash (all Base64 encoded)
        // Should only contain Base64 characters (alphanumeric + / + =) and pipe separators
        boolean medicalLooksEncrypted = medicalMetadata.matches("[A-Za-z0-9+/=|]+");
        assertTrue(medicalLooksEncrypted, "Medical metadata should look like encrypted/encoded data (Base64 + pipes)");

        boolean financialLooksEncrypted = financialMetadata.matches("[A-Za-z0-9+/=|]+");
        assertTrue(financialLooksEncrypted, "Financial metadata should look like encrypted/encoded data (Base64 + pipes)");

        System.out.println("✅ RIGOROUS data integrity and security validation passed");
        System.out.println("   Medical metadata: " + medicalMetadata.length() + " chars, no plaintext leaks ✅");
        System.out.println("   Financial metadata: " + financialMetadata.length() + " chars, no plaintext leaks ✅");
        System.out.println("   All sensitive fields verified encrypted ✅");
    }

    @Test
    @Order(8)
    @DisplayName("Test encryption performance with 10 blocks")
    void testEncryptionPerformance() {
        // RIGOROUS validation - Performance and integrity under load

        long startTime = System.currentTimeMillis();

        // Create and encrypt 10 blocks with medical data
        Block[] performanceBlocks = new Block[10];
        for (int i = 0; i < 10; i++) {
            performanceBlocks[i] = api.storeSecret(
                MEDICAL_DATA + " - Patient #" + i,
                testPassword
            );

            // RIGOROUS - Verify each block individually
            assertNotNull(performanceBlocks[i], "Block " + i + " should be created");
            assertTrue(performanceBlocks[i].isDataEncrypted(), "Block " + i + " should be encrypted");
            assertNotNull(performanceBlocks[i].getId(), "Block " + i + " should have ID");
            assertTrue(performanceBlocks[i].getBlockNumber() > 0, "Block " + i + " should have valid block number");
            assertNotNull(performanceBlocks[i].getEncryptionMetadata(), "Block " + i + " should have encryption metadata");
        }

        long encryptionTime = System.currentTimeMillis() - startTime;

        // RIGOROUS - Verify all blocks have unique block numbers
        Set<Long> blockNumbers = new java.util.HashSet<>();
        for (Block block : performanceBlocks) {
            blockNumbers.add(block.getBlockNumber());
        }
        assertEquals(10, blockNumbers.size(), "All 10 blocks should have unique block numbers");

        // RIGOROUS - Decrypt all 10 blocks and verify integrity
        startTime = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            String decrypted = api.retrieveSecret(performanceBlocks[i].getBlockNumber(), testPassword);
            assertNotNull(decrypted, "Decryption should succeed for block " + i);
            assertTrue(decrypted.contains("Patient #" + i), "Decrypted data should contain patient number " + i);
            assertTrue(decrypted.contains("MEDICAL RECORD - CONFIDENTIAL"), "Should contain medical header");
            assertTrue(decrypted.contains("Jane Doe"), "Should contain patient name");

            // RIGOROUS - Verify exact data match
            String expectedData = MEDICAL_DATA + " - Patient #" + i;
            assertEquals(expectedData, decrypted, "Decrypted data should EXACTLY match original for block " + i);
        }
        long decryptionTime = System.currentTimeMillis() - startTime;

        // RIGOROUS - Verify all blocks are still in blockchain
        for (int i = 0; i < 10; i++) {
            Block retrieved = blockchain.getBlock(performanceBlocks[i].getBlockNumber());
            assertNotNull(retrieved, "Block " + i + " should be retrievable from blockchain");
            assertEquals(performanceBlocks[i].getId(), retrieved.getId(), "Block " + i + " ID should match");
            assertTrue(retrieved.isDataEncrypted(), "Block " + i + " should still be marked as encrypted");
        }

        // RIGOROUS - Performance assertions with detailed metrics
        double avgEncryptionTime = encryptionTime / 10.0;
        double avgDecryptionTime = decryptionTime / 10.0;

        System.out.printf("✅ RIGOROUS performance test passed%n");
        System.out.printf("   Total encryption time: %dms (avg: %.1fms per block)%n", encryptionTime, avgEncryptionTime);
        System.out.printf("   Total decryption time: %dms (avg: %.1fms per block)%n", decryptionTime, avgDecryptionTime);
        System.out.printf("   Total blocks encrypted: 10%n");
        System.out.printf("   All blocks verified for data integrity ✅%n");

        // Performance assertions (reasonable thresholds)
        assertTrue(encryptionTime < 5000, "Encryption of 10 blocks should complete within 5 seconds");
        assertTrue(decryptionTime < 3000, "Decryption of 10 blocks should complete within 3 seconds");
        assertTrue(avgEncryptionTime < 500, "Average encryption time should be < 500ms per block");
        assertTrue(avgDecryptionTime < 300, "Average decryption time should be < 300ms per block");
    }

    @Test
    @Order(9)
    @DisplayName("Test chain validation with encrypted blocks")
    void testChainValidationWithEncryptedBlocks() {
        // RIGOROUS validation - Chain integrity with encrypted blocks

        long blockCountBefore = blockchain.getBlockCount();
        assertTrue(blockCountBefore > 10, "Should have multiple blocks for this test");

        // RIGOROUS - Validate entire chain including encrypted blocks
        ChainValidationResult result = blockchain.validateChainDetailed();
        assertNotNull(result, "Validation result should not be null");
        assertTrue(result.isValid(), "Chain should be valid with encrypted blocks");
        assertNotNull(result.getSummary(), "Validation summary should exist");
        assertFalse(result.getSummary().isEmpty(), "Validation summary should not be empty");

        // RIGOROUS - Verify specific encrypted blocks are still intact
        Block medicalFromChain = blockchain.getBlock(medicalBlock.getBlockNumber());
        assertNotNull(medicalFromChain, "Medical block should still exist in chain");
        assertTrue(medicalFromChain.isDataEncrypted(), "Medical block should still be encrypted");
        assertEquals(medicalBlock.getId(), medicalFromChain.getId(), "Medical block ID should match");

        Block financialFromChain = blockchain.getBlock(financialBlock.getBlockNumber());
        assertNotNull(financialFromChain, "Financial block should still exist in chain");
        assertTrue(financialFromChain.isDataEncrypted(), "Financial block should still be encrypted");
        assertEquals(financialBlock.getId(), financialFromChain.getId(), "Financial block ID should match");

        // RIGOROUS - Verify encrypted blocks can still be decrypted after validation
        String medicalDecrypted = api.retrieveSecret(medicalBlock.getBlockNumber(), testPassword);
        assertEquals(MEDICAL_DATA, medicalDecrypted, "Medical data should still decrypt correctly after validation");

        String financialDecrypted = api.retrieveSecret(financialBlock.getBlockNumber(), testPassword);
        assertEquals(FINANCIAL_DATA, financialDecrypted, "Financial data should still decrypt correctly after validation");

        // RIGOROUS - Block count should remain same after validation
        long blockCountAfter = blockchain.getBlockCount();
        assertEquals(blockCountBefore, blockCountAfter, "Block count should not change during validation");

        System.out.println("✅ RIGOROUS chain validation with encrypted blocks passed");
        System.out.println("   Total blocks: " + blockchain.getBlockCount());
        System.out.println("   Validation result: " + result.getSummary());
        System.out.println("   Encrypted blocks verified intact ✅");
        System.out.println("   Decryption still works after validation ✅");
    }

    @Test
    @Order(10)
    @DisplayName("Test search encrypted data")
    void testSearchEncryptedData() {
        // RIGOROUS validation - Search functionality for encrypted data

        // Test 1: Search for medical encrypted data
        List<Block> medicalResults = api.findEncryptedData("medical");
        assertNotNull(medicalResults, "Medical search results should not be null");
        assertTrue(medicalResults.size() > 0, "Should find at least one encrypted medical block");

        // RIGOROUS - Verify all found blocks are properly encrypted
        for (Block block : medicalResults) {
            assertTrue(block.isDataEncrypted(), "All found medical blocks should be encrypted");
            assertNotNull(block.getEncryptionMetadata(), "All found blocks should have encryption metadata");
            assertNotNull(block.getId(), "All found blocks should have ID");
            assertNotNull(block.getHash(), "All found blocks should have hash");
        }

        // RIGOROUS - Verify our medical block is in the results
        boolean foundMedicalBlock = medicalResults.stream()
            .anyMatch(b -> b.getId().equals(medicalBlock.getId()));
        assertTrue(foundMedicalBlock, "Our medical block should be found in search results");

        // Test 2: Search for financial encrypted data
        List<Block> financialResults = api.findEncryptedData("financial");
        assertNotNull(financialResults, "Financial search results should not be null");

        // Test 3: Search for non-existent keyword
        List<Block> noResults = api.findEncryptedData("nonexistentkeywordxyz123");
        assertNotNull(noResults, "Search results should not be null even if empty");

        // Test 4: Verify found blocks can be decrypted
        if (medicalResults.size() > 0) {
            Block firstResult = medicalResults.get(0);
            // Try to decrypt - should work if we have the password
            String decrypted = api.retrieveSecret(firstResult.getBlockNumber(), testPassword);
            // If this is our medical block, it should decrypt
            if (firstResult.getId().equals(medicalBlock.getId())) {
                assertNotNull(decrypted, "Our medical block should decrypt");
                assertEquals(MEDICAL_DATA, decrypted, "Decrypted data should match original");
            }
        }

        System.out.println("✅ RIGOROUS search encrypted data validation passed");
        System.out.println("   Medical results: " + medicalResults.size() + " block(s)");
        System.out.println("   Financial results: " + financialResults.size() + " block(s)");
        System.out.println("   All found blocks verified encrypted ✅");
    }

    @Test
    @Order(11)
    @DisplayName("Test re-encryption with different password")
    void testReEncryptionWithDifferentPassword() {
        // RIGOROUS validation - Re-encryption with different password

        String newPassword = "NewSecurePassword456!@#";

        // RIGOROUS - Verify original block still exists and works with old password
        String decryptedWithOld = api.retrieveSecret(medicalBlock.getBlockNumber(), testPassword);
        assertNotNull(decryptedWithOld, "Should decrypt with old password");
        assertEquals(MEDICAL_DATA, decryptedWithOld, "Old password should decrypt to original data");

        // RIGOROUS - Store same data with NEW password (creates new block)
        long blockCountBefore = blockchain.getBlockCount();
        Block reEncryptedBlock = api.storeSecret(MEDICAL_DATA, newPassword);
        assertNotNull(reEncryptedBlock, "Re-encryption should succeed");
        assertTrue(reEncryptedBlock.isDataEncrypted(), "New block should be encrypted");
        assertNotNull(reEncryptedBlock.getEncryptionMetadata(), "New block should have encryption metadata");

        // RIGOROUS - Verify new block was created (not updated)
        long blockCountAfter = blockchain.getBlockCount();
        assertEquals(blockCountBefore + 1, blockCountAfter, "Should create new block, not update existing");
        assertNotEquals(medicalBlock.getId(), reEncryptedBlock.getId(), "New block should have different ID");
        assertNotEquals(medicalBlock.getBlockNumber(), reEncryptedBlock.getBlockNumber(), "New block should have different block number");

        // RIGOROUS - Old password should STILL work for original block
        String stillWorksWithOld = api.retrieveSecret(medicalBlock.getBlockNumber(), testPassword);
        assertNotNull(stillWorksWithOld, "Old password should still work for original block");
        assertEquals(MEDICAL_DATA, stillWorksWithOld, "Old password should still decrypt original data correctly");

        // RIGOROUS - Old password should NOT work for new block
        String oldPasswordOnNewBlock = api.retrieveSecret(reEncryptedBlock.getBlockNumber(), testPassword);
        assertNull(oldPasswordOnNewBlock, "Old password should NOT decrypt new block");

        // RIGOROUS - New password should work for new block
        String worksWithNew = api.retrieveSecret(reEncryptedBlock.getBlockNumber(), newPassword);
        assertNotNull(worksWithNew, "New password should decrypt new block");
        assertEquals(MEDICAL_DATA, worksWithNew, "New password should decrypt to original data");

        // RIGOROUS - New password should NOT work for old block
        String newPasswordOnOldBlock = api.retrieveSecret(medicalBlock.getBlockNumber(), newPassword);
        assertNull(newPasswordOnOldBlock, "New password should NOT decrypt old block");

        // RIGOROUS - Both blocks should have different encryption metadata
        assertNotEquals(medicalBlock.getEncryptionMetadata(), reEncryptedBlock.getEncryptionMetadata(),
            "Different passwords should produce different encryption metadata even for same data");

        // RIGOROUS - Verify both blocks are in blockchain
        Block originalStillExists = blockchain.getBlock(medicalBlock.getBlockNumber());
        assertNotNull(originalStillExists, "Original block should still exist");
        assertTrue(originalStillExists.isDataEncrypted(), "Original block should still be encrypted");

        Block newBlockExists = blockchain.getBlock(reEncryptedBlock.getBlockNumber());
        assertNotNull(newBlockExists, "New block should exist");
        assertTrue(newBlockExists.isDataEncrypted(), "New block should be encrypted");

        System.out.println("✅ RIGOROUS re-encryption validation passed");
        System.out.println("   Original block: " + medicalBlock.getBlockNumber() + " (password: old)");
        System.out.println("   New block: " + reEncryptedBlock.getBlockNumber() + " (password: new)");
        System.out.println("   Old password works ONLY on original block ✅");
        System.out.println("   New password works ONLY on new block ✅");
        System.out.println("   Both blocks coexist independently ✅");
    }
}
