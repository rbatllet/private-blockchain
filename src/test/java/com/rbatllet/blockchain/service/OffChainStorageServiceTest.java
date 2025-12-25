package com.rbatllet.blockchain.service;

import com.rbatllet.blockchain.entity.OffChainData;
import com.rbatllet.blockchain.util.CryptoUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.security.KeyPair;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test class for OffChainStorageService
 * Tests the new verifyFileStructure() method covering all execution paths
 */
public class OffChainStorageServiceTest {

    private OffChainStorageService service;
    private KeyPair testKeyPair;
    private String testPassword;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        service = new OffChainStorageService();
        testKeyPair = CryptoUtil.generateKeyPair();
        testPassword = "TestPassword123!";
    }

    @AfterEach
    void tearDown() throws IOException {
        // Clean up off-chain-data directory if it exists
        Path offChainDir = Paths.get("off-chain-data");
        if (Files.exists(offChainDir)) {
            Files.walk(offChainDir)
                .sorted((a, b) -> b.compareTo(a)) // Delete files before directories
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException e) {
                        // Ignore cleanup errors
                    }
                });
        }
    }

    // ==================== Tests for verifyFileStructure() ====================

    @Test
    void testVerifyFileStructure_ValidFile() throws Exception {
        // Create valid off-chain data
        byte[] testData = "Test data for structure verification".getBytes();
        OffChainData offChainData = service.storeData(
            testData,
            testPassword,
            testKeyPair.getPrivate(),
            CryptoUtil.publicKeyToString(testKeyPair.getPublic()),
            "text/plain"
        );

        // Should pass all checks
        boolean result = service.verifyFileStructure(offChainData);
        assertTrue(result, "Valid file structure should pass verification");
    }

    @Test
    void testVerifyFileStructure_FileDoesNotExist() {
        // Create metadata pointing to non-existent file
        OffChainData offChainData = new OffChainData(
            "hash123",
            "signature123",
            tempDir.resolve("nonexistent.dat").toString(),
            100L,
            Base64.getEncoder().encodeToString(new byte[12]),
            Base64.getEncoder().encodeToString(new byte[16]),  // dummy salt
            "text/plain",
            "publicKey123"
        );

        boolean result = service.verifyFileStructure(offChainData);
        assertFalse(result, "Non-existent file should fail verification");
    }

    @Test
    void testVerifyFileStructure_FileNotReadable() throws IOException {
        // Create a file and make it non-readable (Unix-only test)
        String os = System.getProperty("os.name").toLowerCase();
        if (!os.contains("win")) { // Skip on Windows
            Path testFile = tempDir.resolve("unreadable.dat");
            Files.write(testFile, new byte[100]);

            // Remove read permissions
            Set<PosixFilePermission> perms = new HashSet<>();
            perms.add(PosixFilePermission.OWNER_WRITE);
            Files.setPosixFilePermissions(testFile, perms);

            OffChainData offChainData = new OffChainData(
                "hash123",
                "signature123",
                testFile.toString(),
                100L,
                Base64.getEncoder().encodeToString(new byte[12]),
                Base64.getEncoder().encodeToString(new byte[16]),  // dummy salt
                "text/plain",
                "publicKey123"
            );

            boolean result = service.verifyFileStructure(offChainData);
            assertFalse(result, "Non-readable file should fail verification");

            // Restore permissions for cleanup
            perms.add(PosixFilePermission.OWNER_READ);
            Files.setPosixFilePermissions(testFile, perms);
        }
    }

    @Test
    void testVerifyFileStructure_EmptyFile() throws IOException {
        // Create empty file
        Path emptyFile = tempDir.resolve("empty.dat");
        Files.createFile(emptyFile);

        OffChainData offChainData = new OffChainData(
            "hash123",
            "signature123",
            emptyFile.toString(),
            100L,
            Base64.getEncoder().encodeToString(new byte[12]),
            Base64.getEncoder().encodeToString(new byte[16]),  // dummy salt
            "text/plain",
            "publicKey123"
        );

        boolean result = service.verifyFileStructure(offChainData);
        assertFalse(result, "Empty file should fail verification");
    }

    @Test
    void testVerifyFileStructure_FileTooSmall() throws IOException {
        // Create file smaller than GCM_TAG_LENGTH (16 bytes)
        Path smallFile = tempDir.resolve("small.dat");
        Files.write(smallFile, new byte[10]); // Only 10 bytes

        OffChainData offChainData = new OffChainData(
            "hash123",
            "signature123",
            smallFile.toString(),
            100L,
            Base64.getEncoder().encodeToString(new byte[12]),
            Base64.getEncoder().encodeToString(new byte[16]),  // dummy salt
            "text/plain",
            "publicKey123"
        );

        boolean result = service.verifyFileStructure(offChainData);
        assertFalse(result, "File smaller than GCM_TAG_LENGTH should fail");
    }

    @Test
    void testVerifyFileStructure_SizeMismatchTooSmall() throws IOException {
        // Create file with actual size much smaller than expected
        // Expected: 1000 bytes original + 16 GCM tag = ~1016 bytes
        // Actual: 500 bytes (way too small)
        Path mismatchFile = tempDir.resolve("mismatch_small.dat");
        Files.write(mismatchFile, new byte[500]);

        OffChainData offChainData = new OffChainData(
            "hash123",
            "signature123",
            mismatchFile.toString(),
            1000L, // Expected original size
            Base64.getEncoder().encodeToString(new byte[12]),
            Base64.getEncoder().encodeToString(new byte[16]),  // dummy salt
            "text/plain",
            "publicKey123"
        );

        boolean result = service.verifyFileStructure(offChainData);
        assertFalse(result, "File size much smaller than expected should fail");
    }

    @Test
    void testVerifyFileStructure_SizeMismatchTooLarge() throws IOException {
        // Create file with actual size much larger than expected
        // Expected: 100 bytes original + 16 GCM tag = ~116 bytes
        // Actual: 200 bytes (way too large, beyond tolerance)
        Path mismatchFile = tempDir.resolve("mismatch_large.dat");
        Files.write(mismatchFile, new byte[200]);

        OffChainData offChainData = new OffChainData(
            "hash123",
            "signature123",
            mismatchFile.toString(),
            100L, // Expected original size
            Base64.getEncoder().encodeToString(new byte[12]),
            Base64.getEncoder().encodeToString(new byte[16]),  // dummy salt
            "text/plain",
            "publicKey123"
        );

        boolean result = service.verifyFileStructure(offChainData);
        assertFalse(result, "File size much larger than expected should fail");
    }

    @Test
    void testVerifyFileStructure_InvalidBase64IV() throws IOException {
        // Create valid file but with invalid Base64 IV
        Path validFile = tempDir.resolve("valid.dat");
        Files.write(validFile, new byte[100]);

        OffChainData offChainData = new OffChainData(
            "hash123",
            "signature123",
            validFile.toString(),
            84L, // 100 - 16 = 84 bytes original
            "INVALID_BASE64!!!!", // Invalid Base64
            "AAAAAAAAAAAAAAAAAAAAAA==",  // dummy salt
            "text/plain",
            "publicKey123"
        );

        boolean result = service.verifyFileStructure(offChainData);
        assertFalse(result, "Invalid Base64 IV should fail verification");
    }

    @Test
    void testVerifyFileStructure_InvalidIVLength() throws IOException {
        // Create valid file but with IV of wrong length
        Path validFile = tempDir.resolve("valid.dat");
        Files.write(validFile, new byte[100]);

        // IV should be 12 bytes, but we provide 16 bytes
        byte[] wrongLengthIV = new byte[16];

        OffChainData offChainData = new OffChainData(
            "hash123",
            "signature123",
            validFile.toString(),
            84L,
            Base64.getEncoder().encodeToString(wrongLengthIV),
            Base64.getEncoder().encodeToString(new byte[16]),  // dummy salt
            "text/plain",
            "publicKey123"
        );

        boolean result = service.verifyFileStructure(offChainData);
        assertFalse(result, "IV with wrong length should fail verification");
    }

    @Test
    void testVerifyFileStructure_CannotReadFileContent() throws IOException {
        // This is hard to test portably, but we can verify the exception path
        // by testing with a directory instead of a file
        Path directory = tempDir.resolve("testdir");
        Files.createDirectory(directory);

        OffChainData offChainData = new OffChainData(
            "hash123",
            "signature123",
            directory.toString(), // Directory, not a file
            100L,
            Base64.getEncoder().encodeToString(new byte[12]),
            Base64.getEncoder().encodeToString(new byte[16]),  // dummy salt
            "text/plain",
            "publicKey123"
        );

        boolean result = service.verifyFileStructure(offChainData);
        assertFalse(result, "Directory instead of file should fail verification");
    }

    @Test
    void testVerifyFileStructure_WithinSizeTolerance() throws IOException {
        // Test that files within tolerance pass
        // Expected: 100 bytes original
        // minEncryptedSize = 100 + 16 - 8 = 108
        // maxEncryptedSize = 100 + 16 + 32 = 148
        // Actual: 120 bytes (within range)
        Path validFile = tempDir.resolve("tolerance.dat");
        Files.write(validFile, new byte[120]);

        OffChainData offChainData = new OffChainData(
            "hash123",
            "signature123",
            validFile.toString(),
            100L, // Expected original size
            Base64.getEncoder().encodeToString(new byte[12]),
            Base64.getEncoder().encodeToString(new byte[16]),  // dummy salt
            "text/plain",
            "publicKey123"
        );

        boolean result = service.verifyFileStructure(offChainData);
        assertTrue(result, "File within size tolerance should pass verification");
    }

    @Test
    void testVerifyFileStructure_EdgeCaseMinimumValidSize() throws IOException {
        // Test with exactly GCM_TAG_LENGTH bytes (minimum valid size)
        Path minFile = tempDir.resolve("minimum.dat");
        Files.write(minFile, new byte[16]); // Exactly 16 bytes

        OffChainData offChainData = new OffChainData(
            "hash123",
            "signature123",
            minFile.toString(),
            0L, // Expected original size 0 (edge case)
            Base64.getEncoder().encodeToString(new byte[12]),
            Base64.getEncoder().encodeToString(new byte[16]),  // dummy salt
            "text/plain",
            "publicKey123"
        );

        boolean result = service.verifyFileStructure(offChainData);
        assertTrue(result, "File with exactly GCM_TAG_LENGTH should pass when fileSize is 0");
    }

    @Test
    void testVerifyFileStructure_LargeFile() throws Exception {
        // Test with a larger file
        byte[] largeData = new byte[1024 * 100]; // 100KB
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) (i % 256);
        }

        OffChainData offChainData = service.storeData(
            largeData,
            testPassword,
            testKeyPair.getPrivate(),
            CryptoUtil.publicKeyToString(testKeyPair.getPublic()),
            "application/octet-stream"
        );

        boolean result = service.verifyFileStructure(offChainData);
        assertTrue(result, "Large file should pass structure verification");
    }

    @Test
    void testVerifyFileStructure_CorrectIVLength() throws IOException {
        // Verify that correct IV length (12 bytes) passes
        Path validFile = tempDir.resolve("correct_iv.dat");
        Files.write(validFile, new byte[100]);

        byte[] correctIV = new byte[12]; // Correct length
        new java.security.SecureRandom().nextBytes(correctIV);

        OffChainData offChainData = new OffChainData(
            "hash123",
            "signature123",
            validFile.toString(),
            84L,
            Base64.getEncoder().encodeToString(correctIV),
            Base64.getEncoder().encodeToString(new byte[16]),  // dummy salt
            "text/plain",
            "publicKey123"
        );

        boolean result = service.verifyFileStructure(offChainData);
        assertTrue(result, "File with correct IV length should pass");
    }

    // ==================== Integration Tests ====================

    @Test
    void testStoreAndVerifyStructure() throws Exception {
        // Full integration test: store data and verify structure
        String testData = "Integration test data for off-chain storage";

        OffChainData offChainData = service.storeData(
            testData.getBytes(),
            testPassword,
            testKeyPair.getPrivate(),
            CryptoUtil.publicKeyToString(testKeyPair.getPublic()),
            "text/plain"
        );

        // Verify file structure
        assertTrue(service.verifyFileStructure(offChainData),
            "Stored file should have valid structure");

        // Verify we can retrieve the data
        byte[] retrievedData = service.retrieveData(offChainData, testPassword);
        assertArrayEquals(testData.getBytes(), retrievedData,
            "Retrieved data should match original");
    }

    @Test
    void testVerifyStructure_AfterFileCorruption() throws Exception {
        // Store valid data
        String testData = "Data that will be corrupted";
        OffChainData offChainData = service.storeData(
            testData.getBytes(),
            testPassword,
            testKeyPair.getPrivate(),
            CryptoUtil.publicKeyToString(testKeyPair.getPublic()),
            "text/plain"
        );

        // Verify initial structure is valid
        assertTrue(service.verifyFileStructure(offChainData),
            "Initial structure should be valid");

        // Corrupt the file by truncating it
        Path filePath = Paths.get(offChainData.getFilePath());
        byte[] corrupted = new byte[5]; // Make it too small
        Files.write(filePath, corrupted);

        // Verify structure now fails
        assertFalse(service.verifyFileStructure(offChainData),
            "Corrupted file should fail structure verification");
    }

    @Test
    void testVerifyStructure_MultipleFiles() throws Exception {
        // Test verifying structure of multiple files
        for (int i = 0; i < 5; i++) {
            String data = "Test data " + i;
            OffChainData offChainData = service.storeData(
                data.getBytes(),
                testPassword,
                testKeyPair.getPrivate(),
                CryptoUtil.publicKeyToString(testKeyPair.getPublic()),
                "text/plain"
            );

            assertTrue(service.verifyFileStructure(offChainData),
                "File " + i + " should have valid structure");
        }
    }
}
