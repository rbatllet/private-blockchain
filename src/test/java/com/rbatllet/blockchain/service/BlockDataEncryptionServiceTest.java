package com.rbatllet.blockchain.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for BlockDataEncryptionService
 * Tests AES-256-GCM encryption functionality
 */
public class BlockDataEncryptionServiceTest {
    
    private String testData;
    private String testPassword;
    
    @BeforeEach
    void setUp() {
        testData = "Patient John Doe underwent ECG examination on 2024-01-15. Results show normal cardiac rhythm.";
        testPassword = "SecurePassword123!";
    }
    
    @Test
    void testPasswordBasedEncryptionDecryption() throws Exception {
        // Test basic password-based encryption/decryption
        String encrypted = BlockDataEncryptionService.encryptWithPassword(testData, testPassword);
        assertNotNull(encrypted);
        assertFalse(encrypted.isEmpty());
        assertNotEquals(testData, encrypted);
        
        // Decrypt and verify
        String decrypted = BlockDataEncryptionService.decryptWithPassword(encrypted, testPassword);
        assertEquals(testData, decrypted);
    }
    
    @Test
    void testEncryptionProducesDifferentResultsWithSameInput() throws Exception {
        // Same input should produce different encrypted output due to random IV
        String encrypted1 = BlockDataEncryptionService.encryptWithPassword(testData, testPassword);
        String encrypted2 = BlockDataEncryptionService.encryptWithPassword(testData, testPassword);
        
        assertNotEquals(encrypted1, encrypted2);
        
        // But both should decrypt to same plaintext
        String decrypted1 = BlockDataEncryptionService.decryptWithPassword(encrypted1, testPassword);
        String decrypted2 = BlockDataEncryptionService.decryptWithPassword(encrypted2, testPassword);
        
        assertEquals(testData, decrypted1);
        assertEquals(testData, decrypted2);
    }
    
    @Test
    void testWrongPasswordFailsDecryption() {
        assertThrows(Exception.class, () -> {
            String encrypted = BlockDataEncryptionService.encryptWithPassword(testData, testPassword);
            BlockDataEncryptionService.decryptWithPassword(encrypted, "WrongPassword");
        });
    }
    
    @Test
    void testEmptyDataValidation() {
        assertThrows(IllegalArgumentException.class, () -> {
            BlockDataEncryptionService.encryptWithPassword("", testPassword);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            BlockDataEncryptionService.encryptWithPassword(null, testPassword);
        });
    }
    
    @Test
    void testEmptyPasswordValidation() {
        assertThrows(IllegalArgumentException.class, () -> {
            BlockDataEncryptionService.encryptWithPassword(testData, "");
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            BlockDataEncryptionService.encryptWithPassword(testData, null);
        });
    }
    
    @Test
    void testLargeDataEncryption() throws Exception {
        // Test with larger data set
        StringBuilder largeData = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeData.append("Large medical record data entry ").append(i).append(". ");
        }
        
        String encrypted = BlockDataEncryptionService.encryptWithPassword(largeData.toString(), testPassword);
        String decrypted = BlockDataEncryptionService.decryptWithPassword(encrypted, testPassword);
        
        assertEquals(largeData.toString(), decrypted);
    }
    
    @Test
    void testSpecialCharactersEncryption() throws Exception {
        String specialData = "Special chars: áéíóú ñ €$£¥ 中文 русский العربية 🔐🛡️⚡";
        
        String encrypted = BlockDataEncryptionService.encryptWithPassword(specialData, testPassword);
        String decrypted = BlockDataEncryptionService.decryptWithPassword(encrypted, testPassword);
        
        assertEquals(specialData, decrypted);
    }
    
    @Test
    void testMedicalDataEncryption() throws Exception {
        String medicalData = """
            MEDICAL RECORD - CONFIDENTIAL
            Patient: John Doe
            DOB: 1980-05-15
            SSN: 123-45-6789
            Diagnosis: Hypertension, Type 2 Diabetes
            Medications: Metformin 500mg, Lisinopril 10mg
            Lab Results: HbA1c: 7.2%, BP: 140/90
            Next Appointment: 2024-03-15
            """;
        
        String encrypted = BlockDataEncryptionService.encryptWithPassword(medicalData, testPassword);
        String decrypted = BlockDataEncryptionService.decryptWithPassword(encrypted, testPassword);
        
        assertEquals(medicalData, decrypted);
        assertTrue(encrypted.length() > medicalData.length()); // Encrypted should be longer due to metadata
    }
    
    @Test
    void testFinancialDataEncryption() throws Exception {
        String financialData = """
            FINANCIAL TRANSACTION - CONFIDENTIAL
            Account: 4532-1234-5678-9012
            Amount: €50,000.00
            Transaction ID: TXN-2024-001234
            Counterparty: ACME Corp Ltd
            Reference: Invoice INV-2024-0456
            Risk Score: Low
            """;
        
        String encrypted = BlockDataEncryptionService.encryptWithPassword(financialData, testPassword);
        String decrypted = BlockDataEncryptionService.decryptWithPassword(encrypted, testPassword);
        
        assertEquals(financialData, decrypted);
    }
    
    @Test
    void testCorruptedDataDetection() {
        assertThrows(Exception.class, () -> {
            String encrypted = BlockDataEncryptionService.encryptWithPassword(testData, testPassword);
            
            // Corrupt the encrypted data by changing a character
            String corrupted = encrypted.substring(0, encrypted.length() - 5) + "XXXXX";
            
            BlockDataEncryptionService.decryptWithPassword(corrupted, testPassword);
        });
    }
}