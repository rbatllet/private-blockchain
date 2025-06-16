package com.rbatllet.blockchain.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.security.KeyPair;
import java.security.PrivateKey;

import com.rbatllet.blockchain.util.CryptoUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for SecureKeyStorage
 */
public class SecureKeyStorageTest {

    @TempDir
    Path tempDir;

    private KeyPair testKeyPair;
    private String testOwner = "TestUser";
    private String testPassword = "securepassword123";

    @BeforeEach
    void setUp() {
        // Set temporary directory for testing
        System.setProperty("user.dir", tempDir.toString());
        
        // Generate test key pair
        testKeyPair = CryptoUtil.generateKeyPair();
    }

    @AfterEach
    void tearDown() {
        // Clean up any test files
        SecureKeyStorage.deletePrivateKey(testOwner);
    }

    @Test
    void testSaveAndLoadPrivateKey() {
        // Save private key
        boolean saved = SecureKeyStorage.savePrivateKey(testOwner, testKeyPair.getPrivate(), testPassword);
        assertTrue(saved, "Should successfully save private key");

        // Load private key
        PrivateKey loadedKey = SecureKeyStorage.loadPrivateKey(testOwner, testPassword);
        assertNotNull(loadedKey, "Should successfully load private key");

        // Verify keys are equivalent
        assertArrayEquals(testKeyPair.getPrivate().getEncoded(), loadedKey.getEncoded(), 
                         "Loaded key should match original key");
    }

    @Test
    void testLoadWithWrongPassword() {
        // Save private key
        SecureKeyStorage.savePrivateKey(testOwner, testKeyPair.getPrivate(), testPassword);

        // Try to load with wrong password
        PrivateKey loadedKey = SecureKeyStorage.loadPrivateKey(testOwner, "wrongpassword");
        assertNull(loadedKey, "Should return null for wrong password");
    }

    @Test
    void testHasPrivateKey() {
        // Initially should not have key
        assertFalse(SecureKeyStorage.hasPrivateKey(testOwner), "Should not have key initially");

        // Save key
        SecureKeyStorage.savePrivateKey(testOwner, testKeyPair.getPrivate(), testPassword);

        // Should now have key
        assertTrue(SecureKeyStorage.hasPrivateKey(testOwner), "Should have key after saving");
    }

    @Test
    void testDeletePrivateKey() {
        // Save key
        SecureKeyStorage.savePrivateKey(testOwner, testKeyPair.getPrivate(), testPassword);
        assertTrue(SecureKeyStorage.hasPrivateKey(testOwner), "Should have key after saving");

        // Delete key
        boolean deleted = SecureKeyStorage.deletePrivateKey(testOwner);
        assertTrue(deleted, "Should successfully delete key");

        // Should no longer have key
        assertFalse(SecureKeyStorage.hasPrivateKey(testOwner), "Should not have key after deletion");
    }

    @Test
    void testListStoredKeys() {
        // Clean up any existing keys first
        String[] existingKeys = SecureKeyStorage.listStoredKeys();
        for (String key : existingKeys) {
            SecureKeyStorage.deletePrivateKey(key);
        }
        
        // Initially should be empty
        String[] keys = SecureKeyStorage.listStoredKeys();
        assertEquals(0, keys.length, "Should be no stored keys initially");

        // Save multiple keys
        String owner1 = "User1";
        String owner2 = "User2";
        KeyPair keyPair2 = CryptoUtil.generateKeyPair();

        SecureKeyStorage.savePrivateKey(owner1, testKeyPair.getPrivate(), testPassword);
        SecureKeyStorage.savePrivateKey(owner2, keyPair2.getPrivate(), testPassword);

        // Should list both keys
        keys = SecureKeyStorage.listStoredKeys();
        assertEquals(2, keys.length, "Should have 2 stored keys");

        // Clean up
        SecureKeyStorage.deletePrivateKey(owner1);
        SecureKeyStorage.deletePrivateKey(owner2);
    }

    @Test
    void testLoadNonExistentKey() {
        PrivateKey loadedKey = SecureKeyStorage.loadPrivateKey("NonExistentUser", testPassword);
        assertNull(loadedKey, "Should return null for non-existent key");
    }

    @Test
    void testDeleteNonExistentKey() {
        boolean deleted = SecureKeyStorage.deletePrivateKey("NonExistentUser");
        assertFalse(deleted, "Should return false when deleting non-existent key");
    }
}
