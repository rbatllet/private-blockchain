package com.rbatllet.blockchain.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for the secure block encryption service
 */
public class SecureBlockEncryptionServiceTest {
    
    private String testData;
    private String testPassword;
    
    @BeforeEach
    void setUp() {
        testData = "CONFIDENTIAL MEDICAL RECORD: Patient ID: 12345, SSN: 987-65-4321, Diagnosis: Hypertension, Blood Pressure: 150/95";
        testPassword = "VerySecurePassword123!@#$%";
    }
    
    @Test
    void testBasicEncryptionDecryption() throws Exception {
        // Encrypt
        SecureBlockEncryptionService.SecureEncryptedData encrypted = 
            SecureBlockEncryptionService.encryptWithPassword(testData, testPassword);
        
        assertNotNull(encrypted);
        assertNotNull(encrypted.getEncryptedData());
        assertNotNull(encrypted.getSalt());
        assertNotNull(encrypted.getIv());
        assertNotNull(encrypted.getDataHash());
        assertTrue(encrypted.getTimestamp() > 0);
        
        // Decrypt
        String decrypted = SecureBlockEncryptionService.decryptWithPassword(encrypted, testPassword);
        assertEquals(testData, decrypted);
        
        System.out.println("✅ Basic encryption/decryption successful");
    }
    
    @Test
    void testStringConvenienceMethods() throws Exception {
        // Encrypt to string
        String encryptedString = SecureBlockEncryptionService.encryptToString(testData, testPassword);
        assertNotNull(encryptedString);
        assertFalse(encryptedString.isEmpty());
        assertTrue(encryptedString.contains("|")); // Should contain delimiters
        
        // Decrypt from string
        String decrypted = SecureBlockEncryptionService.decryptFromString(encryptedString, testPassword);
        assertEquals(testData, decrypted);
        
        System.out.println("✅ String convenience methods working");
    }
    
    @Test
    void testUniqueEncryptions() throws Exception {
        // Same data + password should produce different encrypted results (due to random salt/IV)
        String encrypted1 = SecureBlockEncryptionService.encryptToString(testData, testPassword);
        String encrypted2 = SecureBlockEncryptionService.encryptToString(testData, testPassword);
        
        assertNotEquals(encrypted1, encrypted2);
        
        // But both should decrypt to same plaintext
        String decrypted1 = SecureBlockEncryptionService.decryptFromString(encrypted1, testPassword);
        String decrypted2 = SecureBlockEncryptionService.decryptFromString(encrypted2, testPassword);
        
        assertEquals(testData, decrypted1);
        assertEquals(testData, decrypted2);
        
        System.out.println("✅ Unique encryptions (random salt/IV) working");
    }
    
    @Test
    void testWrongPasswordFails() throws Exception {
        String encrypted = SecureBlockEncryptionService.encryptToString(testData, testPassword);
        
        // Wrong password should fail
        assertThrows(Exception.class, () -> {
            SecureBlockEncryptionService.decryptFromString(encrypted, "WrongPassword123");
        });
        
        System.out.println("✅ Wrong password correctly rejected");
    }
    
    @Test
    void testDataIntegrityVerification() throws Exception {
        String encrypted = SecureBlockEncryptionService.encryptToString(testData, testPassword);
        
        // Corrupt the encrypted data
        String corrupted = encrypted.substring(0, encrypted.length() - 10) + "CORRUPTED!";
        
        assertThrows(Exception.class, () -> {
            SecureBlockEncryptionService.decryptFromString(corrupted, testPassword);
        });
        
        System.out.println("✅ Data corruption correctly detected");
    }
    
    @Test
    void testIntegrityVerification() throws Exception {
        SecureBlockEncryptionService.SecureEncryptedData encrypted = 
            SecureBlockEncryptionService.encryptWithPassword(testData, testPassword);
        
        // Correct password should verify
        assertTrue(SecureBlockEncryptionService.verifyIntegrity(encrypted, testPassword));
        
        // Wrong password should not verify
        assertFalse(SecureBlockEncryptionService.verifyIntegrity(encrypted, "WrongPassword"));
        
        System.out.println("✅ Integrity verification working");
    }
    
    @Test
    void testLargeDataEncryption() throws Exception {
        // Test with large data
        StringBuilder largeData = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeData.append("CONFIDENTIAL PATIENT RECORD #").append(i)
                    .append(": Name: Patient_").append(i)
                    .append(", SSN: ").append(String.format("%03d", i)).append("-12-3456")
                    .append(", Diagnosis: Various medical conditions and treatments. ");
        }
        
        String largeDataString = largeData.toString();
        
        String encrypted = SecureBlockEncryptionService.encryptToString(largeDataString, testPassword);
        String decrypted = SecureBlockEncryptionService.decryptFromString(encrypted, testPassword);
        
        assertEquals(largeDataString, decrypted);
        
        System.out.println("✅ Large data encryption successful - Size: " + largeDataString.length() + " chars");
    }
    
    @Test
    void testNoPlaintextLeakage() throws Exception {
        String sensitiveData = "TOP_SECRET_PATIENT_DATA_SSN_123456789_CREDIT_CARD_4532123456789012";
        
        String encrypted = SecureBlockEncryptionService.encryptToString(sensitiveData, testPassword);
        
        // Encrypted data should not contain any plaintext fragments
        assertFalse(encrypted.contains("TOP_SECRET"));
        assertFalse(encrypted.contains("PATIENT"));
        assertFalse(encrypted.contains("123456789"));
        assertFalse(encrypted.contains("4532123456789012"));
        assertFalse(encrypted.contains("SSN"));
        assertFalse(encrypted.contains("CREDIT_CARD"));
        
        System.out.println("✅ No plaintext leakage detected in encrypted data");
    }
    
    @Test
    void testSerializationDeserialization() throws Exception {
        SecureBlockEncryptionService.SecureEncryptedData original = 
            SecureBlockEncryptionService.encryptWithPassword(testData, testPassword);
        
        // Serialize
        String serialized = original.serialize();
        assertNotNull(serialized);
        assertFalse(serialized.isEmpty());
        
        // Deserialize
        SecureBlockEncryptionService.SecureEncryptedData deserialized = 
            SecureBlockEncryptionService.SecureEncryptedData.deserialize(serialized);
        
        // Verify all components match
        assertEquals(original.getEncryptedData(), deserialized.getEncryptedData());
        assertEquals(original.getSalt(), deserialized.getSalt());
        assertEquals(original.getIv(), deserialized.getIv());
        assertEquals(original.getDataHash(), deserialized.getDataHash());
        assertEquals(original.getTimestamp(), deserialized.getTimestamp());
        
        // Verify decryption still works
        String decrypted = SecureBlockEncryptionService.decryptWithPassword(deserialized, testPassword);
        assertEquals(testData, decrypted);
        
        System.out.println("✅ Serialization/deserialization working correctly");
    }
    
    @Test
    void testSecurityParameters() throws Exception {
        // Verify that security parameters are reasonable
        SecureBlockEncryptionService.SecureEncryptedData encrypted = 
            SecureBlockEncryptionService.encryptWithPassword(testData, testPassword);
        
        // Salt should be present and reasonably long (base64 encoded)
        String salt = encrypted.getSalt();
        assertNotNull(salt);
        assertTrue(salt.length() >= 20); // 16 bytes salt = ~22 chars base64
        
        // IV should be present and reasonably long
        String iv = encrypted.getIv();
        assertNotNull(iv);
        assertTrue(iv.length() >= 15); // 12 bytes IV = ~16 chars base64
        
        // Hash should be present and correct length for SHA-3-256
        String hash = encrypted.getDataHash();
        assertNotNull(hash);
        assertEquals(64, hash.length()); // SHA-3-256 = 64 hex chars
        
        System.out.println("✅ Security parameters validated");
        System.out.println("   Salt length: " + salt.length() + " chars");
        System.out.println("   IV length: " + iv.length() + " chars");
        System.out.println("   Hash length: " + hash.length() + " chars");
    }
}