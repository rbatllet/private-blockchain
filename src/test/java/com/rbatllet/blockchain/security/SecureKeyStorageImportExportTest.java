package com.rbatllet.blockchain.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.Base64;

import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.core.Blockchain;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for SecureKeyStorage import/export functionality
 */
public class SecureKeyStorageImportExportTest {

    @TempDir
    Path tempDir;

    private Blockchain blockchain;
    private KeyPair testBootstrapKeyPair;
    private KeyPair testKeyPair;
    private String testOwner = "TestImportUser";
    private String testPassword = "SecurePassword123!";
    private Path privateKeyFile;
    private Path publicKeyFile;

    @BeforeEach
    void setUp() throws Exception {
        // Set temporary directory for testing
        System.setProperty("user.dir", tempDir.toString());

        // Initialize blockchain
        blockchain = new Blockchain();
        blockchain.clearAndReinitialize();

        // Create bootstrap admin for tests
        testBootstrapKeyPair = CryptoUtil.generateKeyPair();
        String bootstrapPublicKey = CryptoUtil.publicKeyToString(testBootstrapKeyPair.getPublic());
        blockchain.createBootstrapAdmin(bootstrapPublicKey, "TestBootstrapAdmin");

        // Generate test key pair
        testKeyPair = CryptoUtil.generateKeyPair();

        // Create PEM files for testing
        privateKeyFile = tempDir.resolve("test_private.pem");
        publicKeyFile = tempDir.resolve("test_public.pem");

        writePEMFile(privateKeyFile, testKeyPair.getPrivate(), "PRIVATE KEY");
        writePEMFile(publicKeyFile, testKeyPair.getPublic(), "PUBLIC KEY");
    }

    @AfterEach
    void tearDown() {
        // Clean up test files
        SecureKeyStorage.deletePrivateKey(testOwner);
    }

    private void writePEMFile(Path file, java.security.Key key, String type) throws Exception {
        String keyBase64 = Base64.getEncoder().encodeToString(key.getEncoded());
        StringBuilder pemContent = new StringBuilder();
        pemContent.append("-----BEGIN ").append(type).append("-----\n");

        // Split into 64-character lines
        int index = 0;
        while (index < keyBase64.length()) {
            int endIndex = Math.min(index + 64, keyBase64.length());
            pemContent.append(keyBase64, index, endIndex).append("\n");
            index = endIndex;
        }

        pemContent.append("-----END ").append(type).append("-----\n");
        Files.write(file, pemContent.toString().getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void testImportPrivateKey_Success() throws Exception {
        // Add authorized key to blockchain
        String publicKeyString = CryptoUtil.publicKeyToString(testKeyPair.getPublic());
        blockchain.addAuthorizedKey(publicKeyString, testOwner, testBootstrapKeyPair, UserRole.USER);

        // Import the private key
        boolean success = SecureKeyStorage.importPrivateKey(
            testOwner,
            privateKeyFile.toString(),
            publicKeyFile.toString(),
            testPassword
        );

        assertTrue(success, "Import should succeed with valid keys");
        assertTrue(SecureKeyStorage.hasPrivateKey(testOwner), "Private key should be stored");

        // Verify we can load it
        PrivateKey loadedKey = SecureKeyStorage.loadPrivateKey(testOwner, testPassword);
        assertNotNull(loadedKey, "Should be able to load imported key");
        assertArrayEquals(testKeyPair.getPrivate().getEncoded(), loadedKey.getEncoded(),
                         "Loaded key should match original");
    }

    @Test
    void testImportPrivateKey_OwnerNotAuthorized() {
        // Don't add owner to blockchain
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> SecureKeyStorage.importPrivateKey(
                testOwner,
                privateKeyFile.toString(),
                publicKeyFile.toString(),
                testPassword
            ),
            "Should throw IllegalArgumentException for unauthorized owner"
        );

        assertTrue(exception.getMessage().contains("not authorized on blockchain"),
                  "Error message should mention authorization");
    }

    @Test
    void testImportPrivateKey_PublicKeyMismatch() throws Exception {
        // Add authorized key to blockchain with DIFFERENT public key
        KeyPair differentKeyPair = CryptoUtil.generateKeyPair();
        String differentPublicKey = CryptoUtil.publicKeyToString(differentKeyPair.getPublic());
        blockchain.addAuthorizedKey(differentPublicKey, testOwner, testBootstrapKeyPair, UserRole.USER);

        // Try to import with mismatched public key
        SecurityException exception = assertThrows(
            SecurityException.class,
            () -> SecureKeyStorage.importPrivateKey(
                testOwner,
                privateKeyFile.toString(),
                publicKeyFile.toString(),
                testPassword
            ),
            "Should throw SecurityException for public key mismatch"
        );

        assertTrue(exception.getMessage().contains("Public key mismatch"),
                  "Error message should mention public key mismatch");
        assertTrue(exception.getMessage().contains("attack"),
                  "Error message should mention potential attack");
    }

    @Test
    void testImportPrivateKey_InvalidPrivateKeyFile() throws Exception {
        // Add authorized key to blockchain
        String publicKeyString = CryptoUtil.publicKeyToString(testKeyPair.getPublic());
        blockchain.addAuthorizedKey(publicKeyString, testOwner, testBootstrapKeyPair, UserRole.USER);

        // Create invalid private key file
        Path invalidFile = tempDir.resolve("invalid_private.pem");
        Files.write(invalidFile, "INVALID KEY DATA".getBytes(StandardCharsets.UTF_8));

        // Try to import with invalid file
        boolean success = SecureKeyStorage.importPrivateKey(
            testOwner,
            invalidFile.toString(),
            publicKeyFile.toString(),
            testPassword
        );

        assertFalse(success, "Import should fail with invalid private key file");
    }

    @Test
    void testImportPrivateKey_NullInputs() {
        assertThrows(
            IllegalArgumentException.class,
            () -> SecureKeyStorage.importPrivateKey(null, privateKeyFile.toString(),
                                                    publicKeyFile.toString(), testPassword),
            "Should throw for null owner"
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> SecureKeyStorage.importPrivateKey(testOwner, null,
                                                    publicKeyFile.toString(), testPassword),
            "Should throw for null private key file"
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> SecureKeyStorage.importPrivateKey(testOwner, privateKeyFile.toString(),
                                                    null, testPassword),
            "Should throw for null public key file"
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> SecureKeyStorage.importPrivateKey(testOwner, privateKeyFile.toString(),
                                                    publicKeyFile.toString(), null),
            "Should throw for null password"
        );
    }

    @Test
    void testExportPrivateKey_Success() throws Exception {
        // Save a private key first
        SecureKeyStorage.savePrivateKey(testOwner, testKeyPair.getPrivate(), testPassword);

        // Export it
        Path exportFile = tempDir.resolve("exported_key.pem");
        boolean success = SecureKeyStorage.exportPrivateKey(
            testOwner,
            exportFile.toString(),
            testPassword
        );

        assertTrue(success, "Export should succeed");
        assertTrue(Files.exists(exportFile), "Export file should exist");

        // Verify the exported file is valid PEM
        String content = new String(Files.readAllBytes(exportFile), StandardCharsets.UTF_8);
        assertTrue(content.contains("-----BEGIN PRIVATE KEY-----"), "Should have PEM header");
        assertTrue(content.contains("-----END PRIVATE KEY-----"), "Should have PEM footer");

        // Verify we can import it back
        Path publicExportFile = tempDir.resolve("exported_public.pem");
        writePEMFile(publicExportFile, testKeyPair.getPublic(), "PUBLIC KEY");

        // Add to blockchain for import verification
        String publicKeyString = CryptoUtil.publicKeyToString(testKeyPair.getPublic());
        blockchain.addAuthorizedKey(publicKeyString, testOwner, testBootstrapKeyPair, UserRole.USER);

        // Delete the stored key
        SecureKeyStorage.deletePrivateKey(testOwner);

        // Import the exported key
        boolean imported = SecureKeyStorage.importPrivateKey(
            testOwner,
            exportFile.toString(),
            publicExportFile.toString(),
            testPassword
        );

        assertTrue(imported, "Should be able to import exported key");
    }

    @Test
    void testExportPrivateKey_WrongPassword() {
        // Save a private key first
        SecureKeyStorage.savePrivateKey(testOwner, testKeyPair.getPrivate(), testPassword);

        // Try to export with wrong password
        Path exportFile = tempDir.resolve("exported_key.pem");
        boolean success = SecureKeyStorage.exportPrivateKey(
            testOwner,
            exportFile.toString(),
            "WrongPassword123!"
        );

        assertFalse(success, "Export should fail with wrong password");
        assertFalse(Files.exists(exportFile), "Export file should not be created");
    }

    @Test
    void testExportPrivateKey_KeyNotFound() {
        // Try to export non-existent key
        Path exportFile = tempDir.resolve("exported_key.pem");
        boolean success = SecureKeyStorage.exportPrivateKey(
            "NonExistentUser",
            exportFile.toString(),
            testPassword
        );

        assertFalse(success, "Export should fail for non-existent key");
        assertFalse(Files.exists(exportFile), "Export file should not be created");
    }

    @Test
    void testExportPrivateKey_NullInputs() {
        assertThrows(
            IllegalArgumentException.class,
            () -> SecureKeyStorage.exportPrivateKey(null, "output.pem", testPassword),
            "Should throw for null owner"
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> SecureKeyStorage.exportPrivateKey(testOwner, null, testPassword),
            "Should throw for null output file"
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> SecureKeyStorage.exportPrivateKey(testOwner, "output.pem", null),
            "Should throw for null password"
        );
    }

    @Test
    void testExportImportRoundTrip() throws Exception {
        // Save a private key
        SecureKeyStorage.savePrivateKey(testOwner, testKeyPair.getPrivate(), testPassword);

        // Export it
        Path exportFile = tempDir.resolve("roundtrip_key.pem");
        boolean exported = SecureKeyStorage.exportPrivateKey(
            testOwner,
            exportFile.toString(),
            testPassword
        );
        assertTrue(exported, "Export should succeed");

        // Delete the original
        SecureKeyStorage.deletePrivateKey(testOwner);
        assertFalse(SecureKeyStorage.hasPrivateKey(testOwner), "Key should be deleted");

        // Add to blockchain for import verification
        String publicKeyString = CryptoUtil.publicKeyToString(testKeyPair.getPublic());
        blockchain.addAuthorizedKey(publicKeyString, testOwner, testBootstrapKeyPair, UserRole.USER);

        // Import it back
        boolean imported = SecureKeyStorage.importPrivateKey(
            testOwner,
            exportFile.toString(),
            publicKeyFile.toString(),
            testPassword
        );
        assertTrue(imported, "Import should succeed");
        assertTrue(SecureKeyStorage.hasPrivateKey(testOwner), "Key should be restored");

        // Verify the imported key works
        PrivateKey restoredKey = SecureKeyStorage.loadPrivateKey(testOwner, testPassword);
        assertNotNull(restoredKey, "Should be able to load restored key");
        assertArrayEquals(testKeyPair.getPrivate().getEncoded(), restoredKey.getEncoded(),
                         "Restored key should match original");
    }
}
