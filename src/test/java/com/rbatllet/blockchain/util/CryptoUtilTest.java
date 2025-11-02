package com.rbatllet.blockchain.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the enhanced CryptoUtil class with SHA-3, ML-DSA-87, and key management features
 */
public class CryptoUtilTest {

    private String testData;
    private KeyPair keyPair;
    private KeyPair mldsaKeyPair;

    @BeforeEach
    public void setUp() {
        testData = "Test data for cryptographic operations";
        keyPair = CryptoUtil.generateHierarchicalKeyPair(); // ML-DSA hierarchical key pair
        mldsaKeyPair = CryptoUtil.generateKeyPair(); // Direct ML-DSA key pair
    }

    @Test
    @DisplayName("Test SHA-3 hash calculation")
    public void testSHA3Hash() {
        String hash = CryptoUtil.calculateHash(testData);
        assertNotNull(hash);
        assertEquals(64, hash.length()); // SHA-3-256 produces 64 hex chars
        
        // Same input should produce same hash
        String hash2 = CryptoUtil.calculateHash(testData);
        assertEquals(hash, hash2);
        
        // Different input should produce different hash
        String hash3 = CryptoUtil.calculateHash(testData + "modified");
        assertNotEquals(hash, hash3);
    }
    
    // Legacy SHA-256 test removed as part of modernization
    
    @Test
    @DisplayName("Test " + CryptoUtil.ALGORITHM_DISPLAY_NAME + " signature and verification")
    public void testMLDSASignature() {
        String signature = CryptoUtil.signData(testData, mldsaKeyPair.getPrivate());
        assertNotNull(signature);

        boolean verified = CryptoUtil.verifySignature(testData, signature, mldsaKeyPair.getPublic());
        assertTrue(verified, CryptoUtil.ALGORITHM_DISPLAY_NAME + " signature verification should succeed with correct key");

        // Verification should fail with wrong key
        KeyPair anotherKeyPair = CryptoUtil.generateKeyPair();
        boolean verifiedWithWrongKey = CryptoUtil.verifySignature(testData, signature, anotherKeyPair.getPublic());
        assertFalse(verifiedWithWrongKey, CryptoUtil.ALGORITHM_DISPLAY_NAME + " signature verification should fail with incorrect key");

        // Verification should fail with tampered data
        boolean verifiedWithTamperedData = CryptoUtil.verifySignature(testData + "tampered", signature, mldsaKeyPair.getPublic());
        assertFalse(verifiedWithTamperedData, CryptoUtil.ALGORITHM_DISPLAY_NAME + " signature verification should fail with tampered data");
    }
    
    // Legacy RSA signature test removed as part of modernization
    
    @Test
    @DisplayName("Test key conversion between string and object formats")
    public void testKeyConversion() {
        // Test ML-DSA-87 key conversion from hierarchical key system
        String publicKeyString = CryptoUtil.publicKeyToString(keyPair.getPublic());
        String privateKeyString = CryptoUtil.privateKeyToString(keyPair.getPrivate());
        
        PublicKey recoveredPublicKey = CryptoUtil.stringToPublicKey(publicKeyString);
        PrivateKey recoveredPrivateKey = CryptoUtil.stringToPrivateKey(privateKeyString);
        
        assertEquals(keyPair.getPublic().getAlgorithm(), recoveredPublicKey.getAlgorithm());
        assertEquals(keyPair.getPrivate().getAlgorithm(), recoveredPrivateKey.getAlgorithm());

        // Test direct ML-DSA key conversion
        String mldsaPublicKeyString = CryptoUtil.publicKeyToString(mldsaKeyPair.getPublic());
        String mldsaPrivateKeyString = CryptoUtil.privateKeyToString(mldsaKeyPair.getPrivate());

        PublicKey recoveredMldsaPublicKey = CryptoUtil.stringToPublicKey(mldsaPublicKeyString);
        PrivateKey recoveredMldsaPrivateKey = CryptoUtil.stringToPrivateKey(mldsaPrivateKeyString);

        assertEquals(mldsaKeyPair.getPublic().getAlgorithm(), recoveredMldsaPublicKey.getAlgorithm());
        assertEquals(mldsaKeyPair.getPrivate().getAlgorithm(), recoveredMldsaPrivateKey.getAlgorithm());
    }
    
    @Test
    @DisplayName("Test hierarchical key management")
    public void testKeyHierarchy() {
        // Create root key
        CryptoUtil.KeyInfo rootKey = CryptoUtil.createRootKey();
        assertNotNull(rootKey);
        assertEquals(CryptoUtil.KeyType.ROOT, rootKey.getKeyType());
        assertNull(rootKey.getIssuerId());
        
        // Create intermediate key
        CryptoUtil.KeyInfo intermediateKey = CryptoUtil.createIntermediateKey(rootKey.getKeyId());
        assertNotNull(intermediateKey);
        assertEquals(CryptoUtil.KeyType.INTERMEDIATE, intermediateKey.getKeyType());
        assertEquals(rootKey.getKeyId(), intermediateKey.getIssuerId());
        assertTrue(rootKey.getIssuedKeyIds().contains(intermediateKey.getKeyId()));
        
        // Create operational key
        CryptoUtil.KeyInfo operationalKey = CryptoUtil.createOperationalKey(intermediateKey.getKeyId());
        assertNotNull(operationalKey);
        assertEquals(CryptoUtil.KeyType.OPERATIONAL, operationalKey.getKeyType());
        assertEquals(intermediateKey.getKeyId(), operationalKey.getIssuerId());
        assertTrue(intermediateKey.getIssuedKeyIds().contains(operationalKey.getKeyId()));
        
        // Verify key validity
        assertTrue(CryptoUtil.isKeyValid(rootKey.getKeyId()));
        assertTrue(CryptoUtil.isKeyValid(intermediateKey.getKeyId()));
        assertTrue(CryptoUtil.isKeyValid(operationalKey.getKeyId()));
    }
    
    @Test
    @DisplayName("Test key revocation")
    public void testKeyRevocation() {
        // Create key hierarchy
        CryptoUtil.KeyInfo rootKey = CryptoUtil.createRootKey();
        CryptoUtil.KeyInfo intermediateKey = CryptoUtil.createIntermediateKey(rootKey.getKeyId());
        CryptoUtil.KeyInfo operationalKey = CryptoUtil.createOperationalKey(intermediateKey.getKeyId());
        
        // Revoke intermediate key with cascade
        CryptoUtil.revokeKey(intermediateKey.getKeyId(), true);
        
        // Check that intermediate and operational keys are revoked
        assertFalse(CryptoUtil.isKeyValid(intermediateKey.getKeyId()));
        assertFalse(CryptoUtil.isKeyValid(operationalKey.getKeyId()));
        
        // Root key should still be valid
        assertTrue(CryptoUtil.isKeyValid(rootKey.getKeyId()));
    }
    
    @Test
    @DisplayName("Test key rotation")
    public void testKeyRotation() {
        // Create key hierarchy
        CryptoUtil.KeyInfo rootKey = CryptoUtil.createRootKey();
        CryptoUtil.KeyInfo intermediateKey = CryptoUtil.createIntermediateKey(rootKey.getKeyId());
        
        // Rotate intermediate key
        CryptoUtil.KeyInfo newIntermediateKey = CryptoUtil.rotateKey(intermediateKey.getKeyId());
        
        // Check that old key is in rotating status
        assertEquals(CryptoUtil.KeyStatus.ROTATING, intermediateKey.getStatus());
        
        // Check that new key is valid
        assertTrue(CryptoUtil.isKeyValid(newIntermediateKey.getKeyId()));
        assertEquals(CryptoUtil.KeyType.INTERMEDIATE, newIntermediateKey.getKeyType());
        assertEquals(rootKey.getKeyId(), newIntermediateKey.getIssuerId());
    }
    
    @Test
    @DisplayName("Test key listing by type and status")
    public void testKeyListing() {
        // Create various keys
        CryptoUtil.KeyInfo rootKey1 = CryptoUtil.createRootKey();
        CryptoUtil.KeyInfo rootKey2 = CryptoUtil.createRootKey();
        CryptoUtil.KeyInfo intermediateKey = CryptoUtil.createIntermediateKey(rootKey1.getKeyId());
        CryptoUtil.KeyInfo operationalKey1 = CryptoUtil.createOperationalKey(intermediateKey.getKeyId());
        CryptoUtil.KeyInfo operationalKey2 = CryptoUtil.createOperationalKey(intermediateKey.getKeyId());
        
        // Revoke one operational key
        CryptoUtil.revokeKey(operationalKey1.getKeyId(), false);
        
        // Test getting keys by type
        List<CryptoUtil.KeyInfo> rootKeys = CryptoUtil.getKeysByType(CryptoUtil.KeyType.ROOT);
        assertTrue(rootKeys.size() >= 2);
        
        List<CryptoUtil.KeyInfo> operationalKeys = CryptoUtil.getKeysByType(CryptoUtil.KeyType.OPERATIONAL);
        assertTrue(operationalKeys.size() >= 2);
        
        // Test getting active keys
        List<CryptoUtil.KeyInfo> activeKeys = CryptoUtil.getActiveKeys();
        assertTrue(activeKeys.contains(rootKey1));
        assertTrue(activeKeys.contains(rootKey2));
        assertTrue(activeKeys.contains(intermediateKey));
        assertTrue(activeKeys.contains(operationalKey2));
        assertFalse(activeKeys.contains(operationalKey1));
    }
}
