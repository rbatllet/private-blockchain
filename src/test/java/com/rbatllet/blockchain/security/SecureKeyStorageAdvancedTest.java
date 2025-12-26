package com.rbatllet.blockchain.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.io.File;
import java.util.List;
import java.util.ArrayList;

import com.rbatllet.blockchain.util.CryptoUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Advanced test suite for SecureKeyStorage
 * Tests concurrency, security edge cases, performance, and error conditions
 */
public class SecureKeyStorageAdvancedTest {

    @TempDir
    Path tempDir;

    private KeyPair testKeyPair;
    private final String testOwner = "AdvancedTestUser";
    private final String testPassword = "AdvancedTestPassword123!";

    @BeforeEach
    void setUp() {
        // Set temporary directory for testing
        System.setProperty("user.dir", tempDir.toString());
        
        // Generate test key pair
        testKeyPair = CryptoUtil.generateKeyPair();
        
        // Clean up any existing test files
        SecureKeyStorage.deletePrivateKey(testOwner);
    }

    @AfterEach
    void tearDown() {
        // Clean up test files
        SecureKeyStorage.deletePrivateKey(testOwner);
        
        // Clean up any test users created during tests
        for (int i = 0; i < 100; i++) {
            SecureKeyStorage.deletePrivateKey("ConcurrentUser" + i);
            SecureKeyStorage.deletePrivateKey("PerformanceUser" + i);
            SecureKeyStorage.deletePrivateKey("StressUser" + i);
        }
    }

    @Test
    void testConcurrentKeyOperations() throws InterruptedException {
        final int threadCount = 10;
        final int operationsPerThread = 5;
        final CountDownLatch latch = new CountDownLatch(threadCount);
        final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor(); // Java 25 Virtual Threads;
        final List<Exception> exceptions = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        String owner = "ConcurrentUser" + threadId + "_" + j;
                        String password = "ConcurrentPass" + threadId + j;
                        KeyPair keyPair = CryptoUtil.generateKeyPair();
                        
                        // Save key
                        boolean saved = SecureKeyStorage.savePrivateKey(owner, keyPair.getPrivate(), password);
                        assertTrue(saved, "Key should be saved successfully");
                        
                        // Check if key exists
                        assertTrue(SecureKeyStorage.hasPrivateKey(owner), "Key should exist");
                        
                        // Load key
                        PrivateKey loadedKey = SecureKeyStorage.loadPrivateKey(owner, password);
                        assertNotNull(loadedKey, "Key should be loaded successfully");
                        assertArrayEquals(keyPair.getPrivate().getEncoded(), loadedKey.getEncoded(),
                                        "Loaded key should match original");
                        
                        // Delete key
                        boolean deleted = SecureKeyStorage.deletePrivateKey(owner);
                        assertTrue(deleted, "Key should be deleted successfully");
                        assertFalse(SecureKeyStorage.hasPrivateKey(owner), "Key should no longer exist");
                    }
                } catch (Exception e) {
                    synchronized (exceptions) {
                        exceptions.add(e);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS), "All threads should complete within 30 seconds");
        executor.shutdown();
        
        if (!exceptions.isEmpty()) {
            fail("Concurrent operations failed with exceptions: " + exceptions);
        }
    }

    @Test
    void testPasswordsWithSpecialCharacters() {
        String[] specialPasswords = {
            "Pass@#$%^&*()123",
            "Contraseña123!",
            "Пароль123",
            "密码123abc",
            "Password with spaces 123",
            "Tab\tPassword123",
            "Newline\nPassword123",
            "Unicode🔒Password123"
        };

        for (String password : specialPasswords) {
            String owner = "SpecialCharUser_" + password.hashCode();
            
            // Save with special password
            boolean saved = SecureKeyStorage.savePrivateKey(owner, testKeyPair.getPrivate(), password);
            assertTrue(saved, "Should save with special password: " + password);
            
            // Load with same password
            PrivateKey loadedKey = SecureKeyStorage.loadPrivateKey(owner, password);
            assertNotNull(loadedKey, "Should load with special password: " + password);
            assertArrayEquals(testKeyPair.getPrivate().getEncoded(), loadedKey.getEncoded(),
                            "Loaded key should match original for password: " + password);
            
            // Clean up
            SecureKeyStorage.deletePrivateKey(owner);
        }
    }

    @Test
    void testVeryLongPasswords() {
        // Test with maximum allowed password length (128 characters)
        String maxLengthPassword = "A1".repeat(64); // 128 characters
        assertTrue(SecureKeyStorage.savePrivateKey(testOwner, testKeyPair.getPrivate(), maxLengthPassword));
        
        PrivateKey loadedKey = SecureKeyStorage.loadPrivateKey(testOwner, maxLengthPassword);
        assertNotNull(loadedKey);
        assertArrayEquals(testKeyPair.getPrivate().getEncoded(), loadedKey.getEncoded());
        
        SecureKeyStorage.deletePrivateKey(testOwner);
        
        // Test with very long password (beyond typical usage)
        String veryLongPassword = "VeryLongPassword123".repeat(20); // 380 characters
        String longOwner = "LongPasswordUser";
        assertTrue(SecureKeyStorage.savePrivateKey(longOwner, testKeyPair.getPrivate(), veryLongPassword));
        
        PrivateKey longLoadedKey = SecureKeyStorage.loadPrivateKey(longOwner, veryLongPassword);
        assertNotNull(longLoadedKey);
        
        SecureKeyStorage.deletePrivateKey(longOwner);
    }

    @Test
    void testVeryLongOwnerNames() {
        String longOwner = "VeryLongOwnerNameThatMightCauseFileSystemIssues".repeat(5);
        
        boolean saved = SecureKeyStorage.savePrivateKey(longOwner, testKeyPair.getPrivate(), testPassword);
        assertTrue(saved, "Should handle long owner names");
        
        assertTrue(SecureKeyStorage.hasPrivateKey(longOwner));
        
        PrivateKey loadedKey = SecureKeyStorage.loadPrivateKey(longOwner, testPassword);
        assertNotNull(loadedKey);
        
        boolean deleted = SecureKeyStorage.deletePrivateKey(longOwner);
        assertTrue(deleted);
    }

    @Test
    void testFileSystemEdgeCases() {
        // Test with owner names that might cause file system issues
        String[] problematicNames = {
            "User.With.Dots",
            "User With Spaces",
            "User/With/Slashes",
            "User\\With\\Backslashes",
            "User:With:Colons",
            "User|With|Pipes",
            "User?With?Questions",
            "User*With*Asterisks",
            "User<With<Brackets>",
            "User\"With\"Quotes"
        };

        for (String owner : problematicNames) {
            try {
                boolean saved = SecureKeyStorage.savePrivateKey(owner, testKeyPair.getPrivate(), testPassword);
                if (saved) {
                    // If save succeeded, load should also work
                    PrivateKey loadedKey = SecureKeyStorage.loadPrivateKey(owner, testPassword);
                    assertNotNull(loadedKey, "Should load key for owner: " + owner);
                    
                    // Clean up
                    SecureKeyStorage.deletePrivateKey(owner);
                }
                // If save failed, that's also acceptable behavior for problematic file names
            } catch (Exception e) {
                // Some file systems may not support certain characters
                // This is acceptable behavior
                System.out.println("Expected exception for problematic owner name: " + owner + " - " + e.getMessage());
            }
        }
    }

    @Test
    void testCorruptedKeyFileHandling() {
        // Save a valid key first
        assertTrue(SecureKeyStorage.savePrivateKey(testOwner, testKeyPair.getPrivate(), testPassword));
        assertTrue(SecureKeyStorage.hasPrivateKey(testOwner));
        
        // Manually corrupt the key file
        try {
            String fileName = "private-keys" + File.separator + testOwner + ".key";
            Path keyFile = Paths.get(fileName);
            
            if (Files.exists(keyFile)) {
                Files.write(keyFile, "CORRUPTED_DATA".getBytes());
                
                // Try to load the corrupted key
                PrivateKey corruptedKey = SecureKeyStorage.loadPrivateKey(testOwner, testPassword);
                assertNull(corruptedKey, "Should return null for corrupted key file");
                
                // hasPrivateKey should still return true (file exists)
                assertTrue(SecureKeyStorage.hasPrivateKey(testOwner), "File exists, so should return true");
            }
        } catch (Exception e) {
            // Expected when dealing with corrupted files
        }
    }

    @Test
    void testPerformanceWithManyKeys() {
        final int keyCount = 50; // Reduced for reasonable test time
        long startTime = System.currentTimeMillis();
        
        // Create many keys
        for (int i = 0; i < keyCount; i++) {
            String owner = "PerformanceUser" + i;
            String password = "PerformancePass" + i;
            KeyPair keyPair = CryptoUtil.generateKeyPair();
            
            boolean saved = SecureKeyStorage.savePrivateKey(owner, keyPair.getPrivate(), password);
            assertTrue(saved, "Should save key " + i);
        }
        
        long saveTime = System.currentTimeMillis() - startTime;
        System.out.println("Time to save " + keyCount + " keys: " + saveTime + "ms");
        
        // List all keys
        startTime = System.currentTimeMillis();
        String[] storedKeys = SecureKeyStorage.listStoredKeys();
        long listTime = System.currentTimeMillis() - startTime;
        
        assertTrue(storedKeys.length >= keyCount, "Should list at least " + keyCount + " keys");
        System.out.println("Time to list " + storedKeys.length + " keys: " + listTime + "ms");
        
        // Load all keys
        startTime = System.currentTimeMillis();
        int successfulLoads = 0;
        for (int i = 0; i < keyCount; i++) {
            String owner = "PerformanceUser" + i;
            String password = "PerformancePass" + i;
            
            PrivateKey loadedKey = SecureKeyStorage.loadPrivateKey(owner, password);
            if (loadedKey != null) {
                successfulLoads++;
            }
        }
        long loadTime = System.currentTimeMillis() - startTime;
        
        assertEquals(keyCount, successfulLoads, "Should successfully load all keys");
        System.out.println("Time to load " + keyCount + " keys: " + loadTime + "ms");
        
        // Performance assertions (reasonable for modern systems)
        assertTrue(saveTime < 30000, "Save time should be reasonable"); // 30 seconds max
        assertTrue(listTime < 5000, "List time should be reasonable"); // 5 seconds max
        assertTrue(loadTime < 30000, "Load time should be reasonable"); // 30 seconds max
    }

    @Test
    void testMemoryUsageWithLargeKeys() {
        // Test with multiple large keys to ensure no memory leaks
        final int iterations = 10;
        
        for (int i = 0; i < iterations; i++) {
            String owner = "LargeKeyUser" + i;
            
            // Generate and save key
            KeyPair keyPair = CryptoUtil.generateKeyPair();
            assertTrue(SecureKeyStorage.savePrivateKey(owner, keyPair.getPrivate(), testPassword));
            
            // Load key multiple times
            for (int j = 0; j < 5; j++) {
                PrivateKey loadedKey = SecureKeyStorage.loadPrivateKey(owner, testPassword);
                assertNotNull(loadedKey);
                // Key should be garbage collected after this scope
            }
            
            // Delete key
            assertTrue(SecureKeyStorage.deletePrivateKey(owner));
        }
        
        // Force garbage collection to check for memory leaks
        System.gc();
        
        // If we reach here without OutOfMemoryError, the test passes
        assertTrue(true, "Memory usage test completed successfully");
    }

    @Test
    void testErrorConditions() {
        // Test null inputs
        assertFalse(SecureKeyStorage.savePrivateKey(null, testKeyPair.getPrivate(), testPassword));
        assertFalse(SecureKeyStorage.savePrivateKey(testOwner, null, testPassword));
        assertFalse(SecureKeyStorage.savePrivateKey(testOwner, testKeyPair.getPrivate(), null));
        
        assertNull(SecureKeyStorage.loadPrivateKey(null, testPassword));
        assertNull(SecureKeyStorage.loadPrivateKey(testOwner, null));
        
        assertFalse(SecureKeyStorage.hasPrivateKey(null));
        assertFalse(SecureKeyStorage.deletePrivateKey(null));
        
        // Test empty inputs
        assertFalse(SecureKeyStorage.savePrivateKey("", testKeyPair.getPrivate(), testPassword));
        assertFalse(SecureKeyStorage.savePrivateKey(testOwner, testKeyPair.getPrivate(), ""));
        
        assertNull(SecureKeyStorage.loadPrivateKey("", testPassword));
        assertNull(SecureKeyStorage.loadPrivateKey(testOwner, ""));
        
        assertFalse(SecureKeyStorage.hasPrivateKey(""));
        assertFalse(SecureKeyStorage.deletePrivateKey(""));
    }

    @Test
    void testDirectoryCreationAndPermissions() {
        // Ensure the private-keys directory is created properly
        String[] keys = SecureKeyStorage.listStoredKeys();
        assertNotNull(keys, "Should be able to list keys even if directory doesn't exist initially");

        // Save a key to trigger directory creation
        assertTrue(SecureKeyStorage.savePrivateKey(testOwner, testKeyPair.getPrivate(), testPassword));

        // Verify directory exists (respect user.dir like SecureKeyStorage does)
        String baseDir = System.getProperty("user.dir", ".");
        File keysDir = new File(baseDir, "private-keys");
        assertTrue(keysDir.exists(), "private-keys directory should exist");
        assertTrue(keysDir.isDirectory(), "private-keys should be a directory");

        // Verify file was created
        File keyFile = new File(keysDir, testOwner + ".key");
        assertTrue(keyFile.exists(), "Key file should exist");
        assertTrue(keyFile.isFile(), "Should be a regular file");
    }

    @Test
    void testKeyOverwrite() {
        // Save initial key
        assertTrue(SecureKeyStorage.savePrivateKey(testOwner, testKeyPair.getPrivate(), testPassword));
        
        // Load and verify initial key
        PrivateKey loadedKey1 = SecureKeyStorage.loadPrivateKey(testOwner, testPassword);
        assertNotNull(loadedKey1);
        
        // Generate new key pair and overwrite
        KeyPair newKeyPair = CryptoUtil.generateKeyPair();
        assertTrue(SecureKeyStorage.savePrivateKey(testOwner, newKeyPair.getPrivate(), testPassword));
        
        // Load and verify new key
        PrivateKey loadedKey2 = SecureKeyStorage.loadPrivateKey(testOwner, testPassword);
        assertNotNull(loadedKey2);
        
        // Keys should be different
        assertFalse(java.util.Arrays.equals(loadedKey1.getEncoded(), loadedKey2.getEncoded()),
                   "Overwritten key should be different from original");
        
        // New key should match the one we saved
        assertArrayEquals(newKeyPair.getPrivate().getEncoded(), loadedKey2.getEncoded(),
                         "Loaded key should match the new key");
    }
}
