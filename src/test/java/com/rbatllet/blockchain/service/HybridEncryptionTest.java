package com.rbatllet.blockchain.service;

import com.rbatllet.blockchain.util.CryptoUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.security.KeyPair;

/**
 * Test class for hybrid encryption functionality using public/private keys
 * Tests the BlockDataEncryptionService with ML-DSA-87 key pairs
 */
public class HybridEncryptionTest {
    
    private String testData;
    private KeyPair userKeyPair;
    private KeyPair signerKeyPair;
    
    @BeforeEach
    void setUp() {
        testData = "Confidential medical data: Patient John Smith, SSN: 123-45-6789, Diagnosis: Hypertension";
        userKeyPair = CryptoUtil.generateKeyPair();
        signerKeyPair = CryptoUtil.generateKeyPair();
    }
    
    @Test
    void testHybridEncryptionDecryption() throws Exception {
        // Test basic hybrid encryption/decryption
        BlockDataEncryptionService.EncryptedBlockData encrypted = 
            BlockDataEncryptionService.encryptBlockData(
                testData, 
                userKeyPair.getPublic(), 
                signerKeyPair.getPrivate()
            );
        
        assertNotNull(encrypted);
        assertNotNull(encrypted.getEncryptedData());
        assertNotNull(encrypted.getEncryptedDEK());
        assertNotNull(encrypted.getIv());
        assertNotNull(encrypted.getAuthTag());
        assertNotNull(encrypted.getDataHash());
        assertEquals("GCM-v1.0", encrypted.getEncryptionVersion());
        
        // Decrypt and verify
        String decrypted = BlockDataEncryptionService.decryptBlockData(
            encrypted, 
            userKeyPair.getPrivate()
        );
        
        assertEquals(testData, decrypted);
        
        System.out.println("✅ Hybrid encryption/decryption successful");
    }
    
    @Test
    void testDifferentUsersCannotDecrypt() throws Exception {
        // Create another user with different keys
        KeyPair anotherUserKeyPair = CryptoUtil.generateKeyPair();
        
        // Encrypt data for first user
        BlockDataEncryptionService.EncryptedBlockData encrypted = 
            BlockDataEncryptionService.encryptBlockData(
                testData, 
                userKeyPair.getPublic(), 
                signerKeyPair.getPrivate()
            );
        
        // Try to decrypt with another user's private key - should fail
        assertThrows(Exception.class, () -> {
            BlockDataEncryptionService.decryptBlockData(
                encrypted, 
                anotherUserKeyPair.getPrivate()
            );
        });
        
        System.out.println("✅ Different users correctly cannot decrypt each other's data");
    }
    
    @Test
    void testSerializationDeserialization() throws Exception {
        // Test serialization and deserialization of encrypted data
        BlockDataEncryptionService.EncryptedBlockData encrypted = 
            BlockDataEncryptionService.encryptBlockData(
                testData, 
                userKeyPair.getPublic(), 
                signerKeyPair.getPrivate()
            );
        
        // Serialize to string
        String serialized = encrypted.serialize();
        assertNotNull(serialized);
        assertFalse(serialized.isEmpty());
        assertTrue(serialized.contains("|")); // Should contain delimiters
        
        // Deserialize back
        BlockDataEncryptionService.EncryptedBlockData deserialized = 
            BlockDataEncryptionService.EncryptedBlockData.deserialize(serialized);
        
        // Verify all components match
        assertEquals(encrypted.getEncryptedData(), deserialized.getEncryptedData());
        assertEquals(encrypted.getEncryptedDEK(), deserialized.getEncryptedDEK());
        assertEquals(encrypted.getIv(), deserialized.getIv());
        assertEquals(encrypted.getAuthTag(), deserialized.getAuthTag());
        assertEquals(encrypted.getDataHash(), deserialized.getDataHash());
        assertEquals(encrypted.getEncryptionVersion(), deserialized.getEncryptionVersion());
        assertEquals(encrypted.getTimestamp(), deserialized.getTimestamp());
        
        // Verify decryption still works
        String decrypted = BlockDataEncryptionService.decryptBlockData(
            deserialized, 
            userKeyPair.getPrivate()
        );
        assertEquals(testData, decrypted);
        
        System.out.println("✅ Serialization/deserialization working correctly");
    }
    
    @Test
    void testIntegrityVerification() throws Exception {
        BlockDataEncryptionService.EncryptedBlockData encrypted = 
            BlockDataEncryptionService.encryptBlockData(
                testData, 
                userKeyPair.getPublic(), 
                signerKeyPair.getPrivate()
            );
        
        // Test integrity verification without full decryption
        assertTrue(BlockDataEncryptionService.verifyEncryptedDataIntegrity(
            encrypted, 
            userKeyPair.getPrivate()
        ));
        
        // Test with wrong private key
        KeyPair wrongKeyPair = CryptoUtil.generateKeyPair();
        assertFalse(BlockDataEncryptionService.verifyEncryptedDataIntegrity(
            encrypted, 
            wrongKeyPair.getPrivate()
        ));
        
        System.out.println("✅ Integrity verification working correctly");
    }
    
    @Test
    void testLargeDataEncryption() throws Exception {
        // Test with larger data set
        StringBuilder largeData = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeData.append("Large confidential medical record entry ").append(i)
                    .append(". Patient data includes sensitive information. ");
        }
        
        String largeDataString = largeData.toString();
        
        BlockDataEncryptionService.EncryptedBlockData encrypted = 
            BlockDataEncryptionService.encryptBlockData(
                largeDataString, 
                userKeyPair.getPublic(), 
                signerKeyPair.getPrivate()
            );
        
        String decrypted = BlockDataEncryptionService.decryptBlockData(
            encrypted, 
            userKeyPair.getPrivate()
        );
        
        assertEquals(largeDataString, decrypted);
        
        System.out.println("✅ Large data encryption successful - Size: " + largeDataString.length() + " chars");
    }
    
    @Test
    void testMultipleUsers() throws Exception {
        // Test encrypting same data for multiple users
        KeyPair user1KeyPair = CryptoUtil.generateKeyPair();
        KeyPair user2KeyPair = CryptoUtil.generateKeyPair();
        KeyPair user3KeyPair = CryptoUtil.generateKeyPair();
        
        String sensitiveData = "Highly confidential financial transaction: $1,000,000 wire transfer";
        
        // Encrypt for each user
        BlockDataEncryptionService.EncryptedBlockData encryptedForUser1 = 
            BlockDataEncryptionService.encryptBlockData(
                sensitiveData, 
                user1KeyPair.getPublic(), 
                signerKeyPair.getPrivate()
            );
        
        BlockDataEncryptionService.EncryptedBlockData encryptedForUser2 = 
            BlockDataEncryptionService.encryptBlockData(
                sensitiveData, 
                user2KeyPair.getPublic(), 
                signerKeyPair.getPrivate()
            );
        
        BlockDataEncryptionService.EncryptedBlockData encryptedForUser3 = 
            BlockDataEncryptionService.encryptBlockData(
                sensitiveData, 
                user3KeyPair.getPublic(), 
                signerKeyPair.getPrivate()
            );
        
        // Each user should be able to decrypt their version
        String decrypted1 = BlockDataEncryptionService.decryptBlockData(
            encryptedForUser1, user1KeyPair.getPrivate());
        String decrypted2 = BlockDataEncryptionService.decryptBlockData(
            encryptedForUser2, user2KeyPair.getPrivate());
        String decrypted3 = BlockDataEncryptionService.decryptBlockData(
            encryptedForUser3, user3KeyPair.getPrivate());
        
        assertEquals(sensitiveData, decrypted1);
        assertEquals(sensitiveData, decrypted2);
        assertEquals(sensitiveData, decrypted3);
        
        // But users should not be able to decrypt others' versions
        assertThrows(Exception.class, () -> 
            BlockDataEncryptionService.decryptBlockData(encryptedForUser1, user2KeyPair.getPrivate()));
        assertThrows(Exception.class, () -> 
            BlockDataEncryptionService.decryptBlockData(encryptedForUser2, user3KeyPair.getPrivate()));
        assertThrows(Exception.class, () -> 
            BlockDataEncryptionService.decryptBlockData(encryptedForUser3, user1KeyPair.getPrivate()));
        
        System.out.println("✅ Multiple users encryption working correctly");
    }
    
    @Test
    void testCorruptedDataDetection() throws Exception {
        BlockDataEncryptionService.EncryptedBlockData encrypted = 
            BlockDataEncryptionService.encryptBlockData(
                testData, 
                userKeyPair.getPublic(), 
                signerKeyPair.getPrivate()
            );
        
        // Test with corrupted encrypted data
        String originalSerialized = encrypted.serialize();
        String corruptedSerialized = originalSerialized.substring(0, originalSerialized.length() - 10) + "CORRUPTED_";
        
        assertThrows(Exception.class, () -> {
            BlockDataEncryptionService.EncryptedBlockData corruptedData = 
                BlockDataEncryptionService.EncryptedBlockData.deserialize(corruptedSerialized);
            BlockDataEncryptionService.decryptBlockData(corruptedData, userKeyPair.getPrivate());
        });
        
        System.out.println("✅ Corrupted data correctly detected and rejected");
    }
    
    @Test
    void testDeterministicKeyDerivation() throws Exception {
        // Test that the same private key always derives the same virtual public key
        String data1 = "Test data 1";
        String data2 = "Test data 2";
        
        // Encrypt both with same user key pair
        BlockDataEncryptionService.EncryptedBlockData encrypted1 = 
            BlockDataEncryptionService.encryptBlockData(
                data1, userKeyPair.getPublic(), signerKeyPair.getPrivate());
        
        BlockDataEncryptionService.EncryptedBlockData encrypted2 = 
            BlockDataEncryptionService.encryptBlockData(
                data2, userKeyPair.getPublic(), signerKeyPair.getPrivate());
        
        // Both should be decryptable with the same private key
        String decrypted1 = BlockDataEncryptionService.decryptBlockData(
            encrypted1, userKeyPair.getPrivate());
        String decrypted2 = BlockDataEncryptionService.decryptBlockData(
            encrypted2, userKeyPair.getPrivate());
        
        assertEquals(data1, decrypted1);
        assertEquals(data2, decrypted2);
        
        System.out.println("✅ Deterministic key derivation working correctly");
    }
}