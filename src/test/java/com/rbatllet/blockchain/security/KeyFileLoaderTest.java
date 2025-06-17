package com.rbatllet.blockchain.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for the KeyFileLoader class
 */
public class KeyFileLoaderTest {

    @TempDir
    Path tempDir;

    /**
     * Helper method to generate a test key pair
     */
    private KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        return keyPairGenerator.generateKeyPair();
    }

    /**
     * Helper method to create a PEM format private key file
     */
    private Path createPemPrivateKeyFile(PrivateKey privateKey) throws IOException {
        Path keyFile = tempDir.resolve("private_key.pem");
        String base64Key = Base64.getEncoder().encodeToString(privateKey.getEncoded());
        
        // Format as PEM
        StringBuilder pemBuilder = new StringBuilder();
        pemBuilder.append("-----BEGIN PRIVATE KEY-----\n");
        
        // Add line breaks every 64 characters
        for (int i = 0; i < base64Key.length(); i += 64) {
            pemBuilder.append(base64Key.substring(i, Math.min(i + 64, base64Key.length())));
            pemBuilder.append("\n");
        }
        
        pemBuilder.append("-----END PRIVATE KEY-----\n");
        
        Files.writeString(keyFile, pemBuilder.toString());
        return keyFile;
    }

    /**
     * Helper method to create a PEM format public key file
     */
    private Path createPemPublicKeyFile(PublicKey publicKey) throws IOException {
        Path keyFile = tempDir.resolve("public_key.pem");
        String base64Key = Base64.getEncoder().encodeToString(publicKey.getEncoded());
        
        // Format as PEM
        StringBuilder pemBuilder = new StringBuilder();
        pemBuilder.append("-----BEGIN PUBLIC KEY-----\n");
        
        // Add line breaks every 64 characters
        for (int i = 0; i < base64Key.length(); i += 64) {
            pemBuilder.append(base64Key.substring(i, Math.min(i + 64, base64Key.length())));
            pemBuilder.append("\n");
        }
        
        pemBuilder.append("-----END PUBLIC KEY-----\n");
        
        Files.writeString(keyFile, pemBuilder.toString());
        return keyFile;
    }

    /**
     * Helper method to create a Base64 format private key file
     */
    private Path createBase64PrivateKeyFile(PrivateKey privateKey) throws IOException {
        Path keyFile = tempDir.resolve("private_key.b64");
        String base64Key = Base64.getEncoder().encodeToString(privateKey.getEncoded());
        Files.writeString(keyFile, base64Key);
        return keyFile;
    }

    /**
     * Helper method to create a DER format private key file
     */
    private Path createDerPrivateKeyFile(PrivateKey privateKey) throws IOException {
        Path keyFile = tempDir.resolve("private_key.der");
        Files.write(keyFile, privateKey.getEncoded());
        return keyFile;
    }

    @Test
    @DisplayName("Should load private key from PEM file")
    void shouldLoadPrivateKeyFromPemFile() throws Exception {
        // Generate a key pair
        KeyPair keyPair = generateKeyPair();
        
        // Create a PEM file
        Path pemFile = createPemPrivateKeyFile(keyPair.getPrivate());
        
        // Load the key
        PrivateKey loadedKey = KeyFileLoader.loadPrivateKeyFromFile(pemFile.toString());
        
        // Verify
        assertNotNull(loadedKey, "Should load private key from PEM file");
        assertEquals(keyPair.getPrivate().getAlgorithm(), loadedKey.getAlgorithm());
        assertEquals(keyPair.getPrivate().getFormat(), loadedKey.getFormat());
    }

    @Test
    @DisplayName("Should load private key from Base64 file")
    void shouldLoadPrivateKeyFromBase64File() throws Exception {
        // Generate a key pair
        KeyPair keyPair = generateKeyPair();
        
        // Create a Base64 file
        Path base64File = createBase64PrivateKeyFile(keyPair.getPrivate());
        
        // Load the key
        PrivateKey loadedKey = KeyFileLoader.loadPrivateKeyFromFile(base64File.toString());
        
        // Verify
        assertNotNull(loadedKey, "Should load private key from Base64 file");
        assertEquals(keyPair.getPrivate().getAlgorithm(), loadedKey.getAlgorithm());
        assertEquals(keyPair.getPrivate().getFormat(), loadedKey.getFormat());
    }

    @Test
    @DisplayName("Should load private key from DER file")
    void shouldLoadPrivateKeyFromDerFile() throws Exception {
        // Generate a key pair
        KeyPair keyPair = generateKeyPair();
        
        // Create a DER file
        Path derFile = createDerPrivateKeyFile(keyPair.getPrivate());
        
        // Load the key
        PrivateKey loadedKey = KeyFileLoader.loadPrivateKeyFromFile(derFile.toString());
        
        // Verify
        assertNotNull(loadedKey, "Should load private key from DER file");
        assertEquals(keyPair.getPrivate().getAlgorithm(), loadedKey.getAlgorithm());
        assertEquals(keyPair.getPrivate().getFormat(), loadedKey.getFormat());
    }

    @Test
    @DisplayName("Should load public key from PEM file")
    void shouldLoadPublicKeyFromPemFile() throws Exception {
        // Generate a key pair
        KeyPair keyPair = generateKeyPair();
        
        // Create a PEM file
        Path pemFile = createPemPublicKeyFile(keyPair.getPublic());
        
        // Load the key
        PublicKey loadedKey = KeyFileLoader.loadPublicKeyFromFile(pemFile.toString());
        
        // Verify
        assertNotNull(loadedKey, "Should load public key from PEM file");
        assertEquals(keyPair.getPublic().getAlgorithm(), loadedKey.getAlgorithm());
        assertEquals(keyPair.getPublic().getFormat(), loadedKey.getFormat());
    }

    @Test
    @DisplayName("Should detect PEM format")
    void shouldDetectPemFormat() throws Exception {
        // Generate a key pair
        KeyPair keyPair = generateKeyPair();
        
        // Create a PEM file
        Path pemFile = createPemPrivateKeyFile(keyPair.getPrivate());
        
        // Detect format
        String format = KeyFileLoader.detectKeyFileFormat(pemFile.toString());
        
        // Verify
        assertEquals("PEM (PKCS#8)", format, "Should detect PEM format");
    }

    @Test
    @DisplayName("Should detect Base64 format")
    void shouldDetectBase64Format() throws Exception {
        // Generate a key pair
        KeyPair keyPair = generateKeyPair();
        
        // Create a Base64 file
        Path base64File = createBase64PrivateKeyFile(keyPair.getPrivate());
        
        // Detect format
        String format = KeyFileLoader.detectKeyFileFormat(base64File.toString());
        
        // Verify
        assertEquals("Raw Base64", format, "Should detect Base64 format");
    }

    @Test
    @DisplayName("Should detect DER format by extension")
    void shouldDetectDerFormatByExtension() throws Exception {
        // Generate a key pair
        KeyPair keyPair = generateKeyPair();
        
        // Create a DER file
        Path derFile = createDerPrivateKeyFile(keyPair.getPrivate());
        
        // Detect format
        String format = KeyFileLoader.detectKeyFileFormat(derFile.toString());
        
        // Verify
        assertEquals("DER (Binary)", format, "Should detect DER format by extension");
    }

    @Test
    @DisplayName("Should validate key file path")
    void shouldValidateKeyFilePath() throws Exception {
        // Generate a key pair
        KeyPair keyPair = generateKeyPair();
        
        // Create a PEM file
        Path pemFile = createPemPrivateKeyFile(keyPair.getPrivate());
        
        // Validate path
        boolean isValid = KeyFileLoader.isValidKeyFilePath(pemFile.toString());
        
        // Verify
        assertTrue(isValid, "Should validate existing key file path");
    }

    @Test
    @DisplayName("Should handle null input for loadPrivateKeyFromFile")
    void shouldHandleNullInputForLoadPrivateKey() {
        PrivateKey key = KeyFileLoader.loadPrivateKeyFromFile(null);
        assertNull(key, "Should return null for null input");
    }

    @Test
    @DisplayName("Should handle null input for loadPublicKeyFromFile")
    void shouldHandleNullInputForLoadPublicKey() {
        PublicKey key = KeyFileLoader.loadPublicKeyFromFile(null);
        assertNull(key, "Should return null for null input");
    }

    @Test
    @DisplayName("Should handle null input for isValidKeyFilePath")
    void shouldHandleNullInputForIsValidKeyFilePath() {
        boolean isValid = KeyFileLoader.isValidKeyFilePath(null);
        assertFalse(isValid, "Should return false for null input");
    }

    @Test
    @DisplayName("Should handle null input for detectKeyFileFormat")
    void shouldHandleNullInputForDetectKeyFileFormat() {
        String format = KeyFileLoader.detectKeyFileFormat(null);
        assertEquals("Error detecting format: null", format, "Should handle null input gracefully");
    }

    @Test
    @DisplayName("Should handle empty input for loadPrivateKeyFromFile")
    void shouldHandleEmptyInputForLoadPrivateKey() {
        PrivateKey key = KeyFileLoader.loadPrivateKeyFromFile("");
        assertNull(key, "Should return null for empty input");
    }

    @Test
    @DisplayName("Should handle empty input for loadPublicKeyFromFile")
    void shouldHandleEmptyInputForLoadPublicKey() {
        PublicKey key = KeyFileLoader.loadPublicKeyFromFile("");
        assertNull(key, "Should return null for empty input");
    }

    @Test
    @DisplayName("Should handle empty input for isValidKeyFilePath")
    void shouldHandleEmptyInputForIsValidKeyFilePath() {
        boolean isValid = KeyFileLoader.isValidKeyFilePath("");
        assertFalse(isValid, "Should return false for empty input");
    }

    @Test
    @DisplayName("Should handle non-existent file for loadPrivateKeyFromFile")
    void shouldHandleNonExistentFileForLoadPrivateKey() {
        PrivateKey key = KeyFileLoader.loadPrivateKeyFromFile("/non/existent/file.pem");
        assertNull(key, "Should return null for non-existent file");
    }

    @Test
    @DisplayName("Should handle non-existent file for loadPublicKeyFromFile")
    void shouldHandleNonExistentFileForLoadPublicKey() {
        PublicKey key = KeyFileLoader.loadPublicKeyFromFile("/non/existent/file.pem");
        assertNull(key, "Should return null for non-existent file");
    }

    @Test
    @DisplayName("Should handle non-existent file for isValidKeyFilePath")
    void shouldHandleNonExistentFileForIsValidKeyFilePath() {
        boolean isValid = KeyFileLoader.isValidKeyFilePath("/non/existent/file.pem");
        assertFalse(isValid, "Should return false for non-existent file");
    }

    @Test
    @DisplayName("Should handle non-existent file for detectKeyFileFormat")
    void shouldHandleNonExistentFileForDetectKeyFileFormat() {
        String format = KeyFileLoader.detectKeyFileFormat("/non/existent/file.pem");
        assertEquals("File not found", format, "Should return 'File not found' for non-existent file");
    }

    @Test
    @DisplayName("Should handle empty file for loadPrivateKeyFromFile")
    void shouldHandleEmptyFileForLoadPrivateKey() throws IOException {
        // Create empty file
        Path emptyFile = tempDir.resolve("empty.pem");
        Files.createFile(emptyFile);
        
        // Try to load key
        PrivateKey key = KeyFileLoader.loadPrivateKeyFromFile(emptyFile.toString());
        
        // Verify
        assertNull(key, "Should return null for empty file");
    }

    @Test
    @DisplayName("Should handle invalid content for loadPrivateKeyFromFile")
    void shouldHandleInvalidContentForLoadPrivateKey() throws IOException {
        // Create file with invalid content
        Path invalidFile = tempDir.resolve("invalid.pem");
        Files.writeString(invalidFile, "This is not a valid key file");
        
        // Try to load key
        PrivateKey key = KeyFileLoader.loadPrivateKeyFromFile(invalidFile.toString());
        
        // Verify
        assertNull(key, "Should return null for invalid content");
    }

    @Test
    @DisplayName("Should handle invalid content for loadPublicKeyFromFile")
    void shouldHandleInvalidContentForLoadPublicKey() throws IOException {
        // Create file with invalid content
        Path invalidFile = tempDir.resolve("invalid.pem");
        Files.writeString(invalidFile, "This is not a valid key file");
        
        // Try to load key
        PublicKey key = KeyFileLoader.loadPublicKeyFromFile(invalidFile.toString());
        
        // Verify
        assertNull(key, "Should return null for invalid content");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/etc/passwd", "/bin/bash", "/usr/bin/python", "/sbin/init", "/usr/sbin/sshd"})
    @DisplayName("Should reject system paths for isValidKeyFilePath")
    void shouldRejectSystemPathsForIsValidKeyFilePath(String systemPath) {
        boolean isValid = KeyFileLoader.isValidKeyFilePath(systemPath);
        assertFalse(isValid, "Should reject system path: " + systemPath);
    }

    @Test
    @DisplayName("Should handle unreadable file for loadPrivateKeyFromFile")
    void shouldHandleUnreadableFileForLoadPrivateKey() throws Exception {
        // Skip this test on Windows as file permissions work differently
        if (!System.getProperty("os.name").toLowerCase().contains("win")) {
            // Generate a key pair
            KeyPair keyPair = generateKeyPair();
            
            // Create a PEM file
            Path pemFile = createPemPrivateKeyFile(keyPair.getPrivate());
            
            try {
                // Make file unreadable
                Set<PosixFilePermission> perms = new HashSet<>();
                Files.setPosixFilePermissions(pemFile, perms);
                
                // Try to load key
                PrivateKey key = KeyFileLoader.loadPrivateKeyFromFile(pemFile.toString());
                
                // Verify
                assertNull(key, "Should return null for unreadable file");
            } catch (UnsupportedOperationException e) {
                // Some file systems don't support POSIX permissions
                System.out.println("Skipping unreadable file test due to filesystem limitations");
            }
        }
    }

    @Test
    @DisplayName("Should handle corrupted PEM file for loadPrivateKeyFromFile")
    void shouldHandleCorruptedPemFileForLoadPrivateKey() throws IOException {
        // Create corrupted PEM file
        Path corruptedFile = tempDir.resolve("corrupted.pem");
        Files.writeString(corruptedFile, 
            "-----BEGIN PRIVATE KEY-----\n" +
            "ThisIsNotValidBase64Content\n" +
            "-----END PRIVATE KEY-----\n");
        
        // Try to load key
        PrivateKey key = KeyFileLoader.loadPrivateKeyFromFile(corruptedFile.toString());
        
        // Verify
        assertNull(key, "Should return null for corrupted PEM file");
    }

    @Test
    @DisplayName("Should handle corrupted Base64 file for loadPrivateKeyFromFile")
    void shouldHandleCorruptedBase64FileForLoadPrivateKey() throws IOException {
        // Create corrupted Base64 file
        Path corruptedFile = tempDir.resolve("corrupted.b64");
        Files.writeString(corruptedFile, "ThisIsNotValidBase64Content");
        
        // Try to load key
        PrivateKey key = KeyFileLoader.loadPrivateKeyFromFile(corruptedFile.toString());
        
        // Verify
        assertNull(key, "Should return null for corrupted Base64 file");
    }

    @Test
    @DisplayName("Should handle corrupted DER file for loadPrivateKeyFromFile")
    void shouldHandleCorruptedDerFileForLoadPrivateKey() throws IOException {
        // Create corrupted DER file
        Path corruptedFile = tempDir.resolve("corrupted.der");
        Files.writeString(corruptedFile, "ThisIsNotValidDERContent");
        
        // Try to load key
        PrivateKey key = KeyFileLoader.loadPrivateKeyFromFile(corruptedFile.toString());
        
        // Verify
        assertNull(key, "Should return null for corrupted DER file");
    }
}
