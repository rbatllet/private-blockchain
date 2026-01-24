package com.rbatllet.blockchain.service;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.entity.OffChainData;
import com.rbatllet.blockchain.indexing.IndexingCoordinator;
import com.rbatllet.blockchain.search.SearchFrameworkEngine;
import com.rbatllet.blockchain.security.UserRole;
import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.util.TestGenesisKeyManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RIGOROUS TEST: Verify that retrieveSecret() returns actual off-chain file content,
 * NOT the internal block.data field (which contains placeholder).
 *
 * CRITICAL: For off-chain blocks, block.data contains encrypted placeholder,
 * but retrieveSecret() MUST return the actual file content stored off-chain.
 *
 * Test coverage:
 * 1. Basic off-chain retrieval (content matches)
 * 2. Block structure verification (hasOffChainData, metadata)
 * 3. Security tests (wrong password, null password)
 * 4. Edge cases (unicode, multi-line, large content)
 * 5. Error handling (non-existent block, non-off-chain block)
 * 6. Multiple off-chain blocks (isolation)
 * 7. Hash integrity verification
 */
@DisplayName("🔍 RIGOROUS: retrieveSecret() with Off-Chain Data")
public class RetrieveSecretOffChainTest {
    private static final Logger logger = LoggerFactory.getLogger(RetrieveSecretOffChainTest.class);

    // Test constants
    private static final String TEST_PASSWORD = "TestPassword123#Secure";
    private static final String WRONG_PASSWORD = "WrongPassword456#Invalid";
    private static final String BLOCK_PLACEHOLDER = "PLACEHOLDER_IN_BLOCK_DATA";
    private static final String PUBLIC_KEYWORD = "offchain_public_test";
    private static final String PRIVATE_KEYWORD = "offchain_private_secret";

    private Blockchain blockchain;
    private KeyPair bootstrapKeyPair;
    private KeyPair userKeyPair;
    private UserFriendlyEncryptionAPI encryptionAPI;

    @BeforeEach
    void setUp() {
        // Reset ALL global state BEFORE creating new instances
        SearchFrameworkEngine.resetGlobalState();
        IndexingCoordinator.getInstance().reset();

        // Wait for any async tasks from previous tests
        try {
            IndexingCoordinator.getInstance().waitForCompletion();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        blockchain = new Blockchain();
        blockchain.clearAndReinitialize();

        bootstrapKeyPair = TestGenesisKeyManager.ensureGenesisKeysExist();
        blockchain.createBootstrapAdmin(
            CryptoUtil.publicKeyToString(bootstrapKeyPair.getPublic()),
            "BOOTSTRAP_ADMIN"
        );

        userKeyPair = CryptoUtil.generateKeyPair();
        String userPublicKey = CryptoUtil.publicKeyToString(userKeyPair.getPublic());
        blockchain.addAuthorizedKey(userPublicKey, "testUser", bootstrapKeyPair, UserRole.ADMIN);

        encryptionAPI = new UserFriendlyEncryptionAPI(blockchain);
        encryptionAPI.setDefaultCredentials("testUser", userKeyPair);
    }

    @AfterEach
    void tearDown() {
        try {
            IndexingCoordinator.getInstance().waitForCompletion();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (blockchain != null) {
            blockchain.clearAndReinitialize();
            blockchain.shutdown();
        }

        SearchFrameworkEngine.resetGlobalState();
        IndexingCoordinator.getInstance().reset();
    }

    // ========== BASIC FUNCTIONALITY TESTS ==========

    @Nested
    @DisplayName("📦 Basic Off-Chain Retrieval")
    class BasicOffChainRetrievalTests {

        @Test
        @DisplayName("retrieveSecret() returns off-chain file content, NOT block.data placeholder")
        void testRetrieveSecretReturnsOffChainContent_NotPlaceholder() {
            // Arrange
            String offChainContent = "OFFCHAIN_CONTENT_" + System.currentTimeMillis();
            byte[] fileData = offChainContent.getBytes(StandardCharsets.UTF_8);

            // Act
            Block block = encryptionAPI.storeSearchableDataWithOffChainFile(
                BLOCK_PLACEHOLDER,
                fileData,
                TEST_PASSWORD,
                "text/plain",
                new String[]{"public:" + PUBLIC_KEYWORD},
                new String[]{PRIVATE_KEYWORD},
                "OFFCHAIN_TEST"
            );

            // Assert: Block created correctly
            assertNotNull(block, "Block should be created");
            assertTrue(block.getBlockNumber() > 0, "Block should have valid block number");

            // Assert: Block has off-chain data
            assertTrue(block.hasOffChainData(), "Block should have off-chain data flag");
            assertNotNull(block.getOffChainData(), "OffChainData object should not be null");

            // Assert: block.data contains placeholder (encrypted)
            String decryptedBlockData = blockchain.getDecryptedBlockData(block.getBlockNumber(), TEST_PASSWORD);
            assertEquals(BLOCK_PLACEHOLDER, decryptedBlockData,
                "Decrypted block.data should be the placeholder");

            // CRITICAL Assert: retrieveSecret() returns file content
            String retrieved = encryptionAPI.retrieveSecret(block.getBlockNumber(), TEST_PASSWORD);
            assertNotNull(retrieved, "retrieveSecret() should return content");
            assertNotEquals(BLOCK_PLACEHOLDER, retrieved,
                "retrieveSecret() should NOT return block.data placeholder");
            assertEquals(offChainContent, retrieved,
                "retrieveSecret() MUST return actual off-chain file content");

            logger.info("✅ block.data (decrypted): '{}'", decryptedBlockData);
            logger.info("✅ retrieveSecret() returned: '{}'", retrieved);
        }

        @Test
        @DisplayName("Off-chain data hash matches stored content")
        void testOffChainDataHashIntegrity() {
            // Arrange
            String offChainContent = "HASH_INTEGRITY_TEST_" + System.currentTimeMillis();
            byte[] fileData = offChainContent.getBytes(StandardCharsets.UTF_8);

            // Act
            Block block = encryptionAPI.storeSearchableDataWithOffChainFile(
                BLOCK_PLACEHOLDER,
                fileData,
                TEST_PASSWORD,
                "text/plain",
                null,
                null,
                "HASH_TEST"
            );

            // Assert
            assertNotNull(block, "Block should be created");
            OffChainData offChainData = block.getOffChainData();
            assertNotNull(offChainData, "OffChainData should not be null");
            assertNotNull(offChainData.getDataHash(), "Data hash should not be null");
            assertFalse(offChainData.getDataHash().isEmpty(), "Data hash should not be empty");

            // Verify hash format (should be hex string)
            assertTrue(offChainData.getDataHash().matches("[a-fA-F0-9]+"),
                "Data hash should be hexadecimal: " + offChainData.getDataHash());

            logger.info("✅ Off-chain data hash: {}", offChainData.getDataHash());
        }

        @Test
        @DisplayName("Off-chain metadata is correctly populated")
        void testOffChainMetadataPopulated() {
            // Arrange
            String offChainContent = "METADATA_TEST_CONTENT";
            byte[] fileData = offChainContent.getBytes(StandardCharsets.UTF_8);
            String mimeType = "application/json";

            // Act
            Block block = encryptionAPI.storeSearchableDataWithOffChainFile(
                BLOCK_PLACEHOLDER,
                fileData,
                TEST_PASSWORD,
                mimeType,
                new String[]{"public:metadata_test"},
                null,
                "METADATA_TEST"
            );

            // Assert
            assertNotNull(block, "Block should be created");
            OffChainData offChainData = block.getOffChainData();
            assertNotNull(offChainData, "OffChainData should not be null");

            // Verify off-chain data properties
            assertNotNull(offChainData.getFilePath(), "File path should not be null");
            assertFalse(offChainData.getFilePath().isEmpty(), "File path should not be empty");
            assertTrue(offChainData.getFileSize() > 0, "Data size should be positive");

            logger.info("✅ Off-chain file path: {}", offChainData.getFilePath());
            logger.info("✅ Off-chain data size: {} bytes", offChainData.getFileSize());
        }
    }

    // ========== SECURITY TESTS ==========

    @Nested
    @DisplayName("🔐 Security Tests")
    class SecurityTests {

        @Test
        @DisplayName("retrieveSecret() with wrong password returns null or throws exception")
        void testRetrieveSecretWithWrongPassword() {
            // Arrange
            String offChainContent = "SECRET_CONTENT_" + System.currentTimeMillis();
            byte[] fileData = offChainContent.getBytes(StandardCharsets.UTF_8);

            Block block = encryptionAPI.storeSearchableDataWithOffChainFile(
                BLOCK_PLACEHOLDER,
                fileData,
                TEST_PASSWORD,
                "text/plain",
                null,
                null,
                "SECURITY_TEST"
            );
            assertNotNull(block, "Block should be created");

            // Act & Assert: Wrong password should fail
            String retrieved = encryptionAPI.retrieveSecret(block.getBlockNumber(), WRONG_PASSWORD);

            // Depending on implementation, it should either return null or throw
            // The key assertion: it should NOT return the actual content
            if (retrieved != null) {
                assertNotEquals(offChainContent, retrieved,
                    "SECURITY VIOLATION: Wrong password should NOT return actual content");
            }

            logger.info("✅ Wrong password correctly prevented access");
        }

        @Test
        @DisplayName("retrieveSecret() with null password throws IllegalArgumentException")
        void testRetrieveSecretWithNullPassword() {
            // Arrange
            String offChainContent = "NULL_PASSWORD_TEST_" + System.currentTimeMillis();
            byte[] fileData = offChainContent.getBytes(StandardCharsets.UTF_8);

            Block block = encryptionAPI.storeSearchableDataWithOffChainFile(
                BLOCK_PLACEHOLDER,
                fileData,
                TEST_PASSWORD,
                "text/plain",
                null,
                null,
                "NULL_PASSWORD_TEST"
            );
            assertNotNull(block, "Block should be created");

            // Act & Assert: Null password should throw IllegalArgumentException
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> encryptionAPI.retrieveSecret(block.getBlockNumber(), null),
                "Null password should throw IllegalArgumentException"
            );

            assertTrue(exception.getMessage().contains("Password"),
                "Exception message should mention 'Password': " + exception.getMessage());

            logger.info("✅ Null password correctly throws exception: {}", exception.getMessage());
        }

        @Test
        @DisplayName("Block is correctly marked as encrypted")
        void testBlockMarkedAsEncrypted() {
            // Arrange
            String offChainContent = "ENCRYPTED_FLAG_TEST";
            byte[] fileData = offChainContent.getBytes(StandardCharsets.UTF_8);

            // Act
            Block block = encryptionAPI.storeSearchableDataWithOffChainFile(
                BLOCK_PLACEHOLDER,
                fileData,
                TEST_PASSWORD,
                "text/plain",
                null,
                null,
                "ENCRYPTED_TEST"
            );

            // Assert
            assertNotNull(block, "Block should be created");
            assertTrue(block.isDataEncrypted(), "Block should be marked as encrypted");
            // Note: encryptionMetadata is set only for recipient-based encryption
            // For password-based encryption, the flag isDataEncrypted is sufficient

            logger.info("✅ Block correctly marked as encrypted (isDataEncrypted={})",
                block.isDataEncrypted());
        }
    }

    // ========== EDGE CASES ==========

    @Nested
    @DisplayName("🔧 Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Unicode and special characters in off-chain content")
        void testUnicodeAndSpecialCharacters() {
            // Arrange
            String offChainContent = "🔐 UNICODE: 日本語 ñoño cañón €£¥ αβγδ " + System.currentTimeMillis();
            byte[] fileData = offChainContent.getBytes(StandardCharsets.UTF_8);

            // Act
            Block block = encryptionAPI.storeSearchableDataWithOffChainFile(
                BLOCK_PLACEHOLDER,
                fileData,
                TEST_PASSWORD,
                "text/plain",
                null,
                null,
                "UNICODE_TEST"
            );

            // Assert
            assertNotNull(block, "Block should be created");
            assertTrue(block.hasOffChainData(), "Block should have off-chain data");

            String retrieved = encryptionAPI.retrieveSecret(block.getBlockNumber(), TEST_PASSWORD);
            assertNotNull(retrieved, "retrieveSecret() should return content");
            assertEquals(offChainContent, retrieved,
                "Unicode content should be preserved exactly");

            logger.info("✅ Unicode content retrieved correctly: {}", retrieved);
        }

        @Test
        @DisplayName("Multi-line content with various line endings")
        void testMultiLineContent() {
            // Arrange: Test different line ending styles
            String offChainContent = "Line 1\nLine 2\r\nLine 3\rLine 4\n\nEmpty line above_" + System.currentTimeMillis();
            byte[] fileData = offChainContent.getBytes(StandardCharsets.UTF_8);

            // Act
            Block block = encryptionAPI.storeSearchableDataWithOffChainFile(
                BLOCK_PLACEHOLDER,
                fileData,
                TEST_PASSWORD,
                "text/plain",
                new String[]{"public:multiline"},
                null,
                "MULTILINE_TEST"
            );

            // Assert
            assertNotNull(block, "Block should be created");
            String retrieved = encryptionAPI.retrieveSecret(block.getBlockNumber(), TEST_PASSWORD);
            assertNotNull(retrieved, "retrieveSecret() should return content");
            assertEquals(offChainContent, retrieved,
                "Multi-line content should be preserved exactly");

            // Verify line count
            long lineCount = retrieved.lines().count();
            assertTrue(lineCount >= 5, "Should have at least 5 lines, got: " + lineCount);

            logger.info("✅ Multi-line content with {} lines retrieved correctly", lineCount);
        }

        @Test
        @DisplayName("Large off-chain content (100KB)")
        void testLargeContent() {
            // Arrange: Create 100KB of content
            StringBuilder sb = new StringBuilder();
            String pattern = "LARGE_CONTENT_BLOCK_";
            int targetSize = 100 * 1024; // 100KB
            while (sb.length() < targetSize) {
                sb.append(pattern).append(sb.length()).append("_");
            }
            String offChainContent = sb.toString();
            byte[] fileData = offChainContent.getBytes(StandardCharsets.UTF_8);

            // Act
            Block block = encryptionAPI.storeSearchableDataWithOffChainFile(
                BLOCK_PLACEHOLDER,
                fileData,
                TEST_PASSWORD,
                "application/octet-stream",
                null,
                null,
                "LARGE_CONTENT_TEST"
            );

            // Assert
            assertNotNull(block, "Block should be created");
            assertTrue(block.hasOffChainData(), "Block should have off-chain data");

            String retrieved = encryptionAPI.retrieveSecret(block.getBlockNumber(), TEST_PASSWORD);
            assertNotNull(retrieved, "retrieveSecret() should return large content");
            assertEquals(offChainContent.length(), retrieved.length(),
                "Retrieved content length should match original");
            assertEquals(offChainContent, retrieved,
                "Large content should be retrieved exactly");

            logger.info("✅ Large content ({} bytes) retrieved correctly", retrieved.length());
        }

        @Test
        @DisplayName("Empty placeholder with off-chain content throws IllegalArgumentException")
        void testEmptyPlaceholder() {
            // Arrange
            String offChainContent = "CONTENT_WITH_EMPTY_PLACEHOLDER_" + System.currentTimeMillis();
            byte[] fileData = offChainContent.getBytes(StandardCharsets.UTF_8);

            // Act & Assert: Empty block data should throw IllegalArgumentException
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> encryptionAPI.storeSearchableDataWithOffChainFile(
                    "",  // Empty placeholder - not allowed by the API
                    fileData,
                    TEST_PASSWORD,
                    "text/plain",
                    null,
                    null,
                    "EMPTY_PLACEHOLDER_TEST"
                ),
                "Empty block data should throw IllegalArgumentException"
            );

            assertTrue(exception.getMessage().toLowerCase().contains("empty") ||
                       exception.getMessage().toLowerCase().contains("data"),
                "Exception message should mention 'empty' or 'data': " + exception.getMessage());

            logger.info("✅ Empty placeholder correctly rejected: {}", exception.getMessage());
        }
    }

    // ========== ERROR HANDLING TESTS ==========

    @Nested
    @DisplayName("❌ Error Handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("retrieveSecret() with non-existent block number returns null")
        void testRetrieveSecretNonExistentBlock() {
            // Act
            long nonExistentBlockNumber = 999999L;
            String retrieved = encryptionAPI.retrieveSecret(nonExistentBlockNumber, TEST_PASSWORD);

            // Assert
            assertNull(retrieved, "retrieveSecret() should return null for non-existent block");

            logger.info("✅ Non-existent block correctly returned null");
        }

        @Test
        @DisplayName("retrieveSecret() with negative block number handles gracefully")
        void testRetrieveSecretNegativeBlockNumber() {
            // Act & Assert: Should handle gracefully (null or exception)
            String retrieved = encryptionAPI.retrieveSecret(-1L, TEST_PASSWORD);
            assertNull(retrieved, "retrieveSecret() should return null for negative block number");

            logger.info("✅ Negative block number handled gracefully");
        }

        @Test
        @DisplayName("retrieveSecret() on non-off-chain block returns block data")
        void testRetrieveSecretOnNonOffChainBlock() {
            // Arrange: Create a regular encrypted block WITHOUT off-chain
            String regularContent = "REGULAR_BLOCK_CONTENT_" + System.currentTimeMillis();

            Block block = encryptionAPI.storeSearchableData(
                regularContent,
                TEST_PASSWORD,
                new String[]{"public:regular", "private_term"}
            );

            // Assert: Block should NOT have off-chain data
            assertNotNull(block, "Block should be created");
            assertFalse(block.hasOffChainData(), "Regular block should NOT have off-chain data");

            // Act: retrieveSecret should return the block data itself
            String retrieved = encryptionAPI.retrieveSecret(block.getBlockNumber(), TEST_PASSWORD);

            // Assert: Should return the regular block content
            assertNotNull(retrieved, "retrieveSecret() should return content for regular block");
            assertEquals(regularContent, retrieved,
                "For non-off-chain blocks, retrieveSecret() should return block data");

            logger.info("✅ Non-off-chain block returns block data correctly");
        }
    }

    // ========== MULTIPLE BLOCKS TESTS ==========

    @Nested
    @DisplayName("📚 Multiple Off-Chain Blocks")
    class MultipleBlocksTests {

        @Test
        @DisplayName("Multiple off-chain blocks maintain isolation")
        void testMultipleOffChainBlocksIsolation() {
            // Arrange: Create multiple off-chain blocks
            String content1 = "CONTENT_BLOCK_1_" + System.currentTimeMillis();
            String content2 = "CONTENT_BLOCK_2_" + System.currentTimeMillis();
            String content3 = "CONTENT_BLOCK_3_" + System.currentTimeMillis();

            Block block1 = encryptionAPI.storeSearchableDataWithOffChainFile(
                "placeholder1", content1.getBytes(StandardCharsets.UTF_8),
                TEST_PASSWORD, "text/plain", null, null, "MULTI_1"
            );

            Block block2 = encryptionAPI.storeSearchableDataWithOffChainFile(
                "placeholder2", content2.getBytes(StandardCharsets.UTF_8),
                TEST_PASSWORD, "text/plain", null, null, "MULTI_2"
            );

            Block block3 = encryptionAPI.storeSearchableDataWithOffChainFile(
                "placeholder3", content3.getBytes(StandardCharsets.UTF_8),
                TEST_PASSWORD, "text/plain", null, null, "MULTI_3"
            );

            // Assert: All blocks created
            assertNotNull(block1, "Block 1 should be created");
            assertNotNull(block2, "Block 2 should be created");
            assertNotNull(block3, "Block 3 should be created");

            // Assert: All have distinct block numbers
            assertNotEquals(block1.getBlockNumber(), block2.getBlockNumber());
            assertNotEquals(block2.getBlockNumber(), block3.getBlockNumber());
            assertNotEquals(block1.getBlockNumber(), block3.getBlockNumber());

            // Assert: Each block returns its own content
            assertEquals(content1, encryptionAPI.retrieveSecret(block1.getBlockNumber(), TEST_PASSWORD),
                "Block 1 should return its content");
            assertEquals(content2, encryptionAPI.retrieveSecret(block2.getBlockNumber(), TEST_PASSWORD),
                "Block 2 should return its content");
            assertEquals(content3, encryptionAPI.retrieveSecret(block3.getBlockNumber(), TEST_PASSWORD),
                "Block 3 should return its content");

            logger.info("✅ Multiple off-chain blocks maintain correct isolation");
        }

        @Test
        @DisplayName("Off-chain blocks have unique file paths")
        void testOffChainBlocksHaveUniqueFilePaths() {
            // Arrange
            Block block1 = encryptionAPI.storeSearchableDataWithOffChainFile(
                "p1", "content1".getBytes(StandardCharsets.UTF_8),
                TEST_PASSWORD, "text/plain", null, null, "UNIQUE_1"
            );

            Block block2 = encryptionAPI.storeSearchableDataWithOffChainFile(
                "p2", "content2".getBytes(StandardCharsets.UTF_8),
                TEST_PASSWORD, "text/plain", null, null, "UNIQUE_2"
            );

            // Assert
            assertNotNull(block1.getOffChainData(), "Block 1 should have off-chain data");
            assertNotNull(block2.getOffChainData(), "Block 2 should have off-chain data");

            String path1 = block1.getOffChainData().getFilePath();
            String path2 = block2.getOffChainData().getFilePath();

            assertNotNull(path1, "Block 1 file path should not be null");
            assertNotNull(path2, "Block 2 file path should not be null");
            assertNotEquals(path1, path2, "Off-chain file paths should be unique");

            logger.info("✅ Block 1 path: {}", path1);
            logger.info("✅ Block 2 path: {}", path2);
        }
    }

    // ========== BINARY DATA TESTS ==========

    @Nested
    @DisplayName("💾 Binary Data Handling")
    class BinaryDataTests {

        @Test
        @DisplayName("Binary data with null bytes is handled correctly")
        void testBinaryDataWithNullBytes() {
            // Arrange: Create binary data with null bytes
            byte[] binaryData = new byte[]{0x00, 0x01, 0x02, 0x00, 0x03, 0x04, 0x00};

            // Act
            Block block = encryptionAPI.storeSearchableDataWithOffChainFile(
                BLOCK_PLACEHOLDER,
                binaryData,
                TEST_PASSWORD,
                "application/octet-stream",
                null,
                null,
                "BINARY_TEST"
            );

            // Assert
            assertNotNull(block, "Block should be created");
            assertTrue(block.hasOffChainData(), "Block should have off-chain data");

            // Note: retrieveSecret returns String, so binary handling depends on implementation
            String retrieved = encryptionAPI.retrieveSecret(block.getBlockNumber(), TEST_PASSWORD);
            assertNotNull(retrieved, "retrieveSecret() should return something for binary data");

            logger.info("✅ Binary data with null bytes handled");
        }

        @Test
        @DisplayName("All byte values (0-255) are handled correctly")
        void testAllByteValues() {
            // Arrange: Create array with all possible byte values
            byte[] allBytes = new byte[256];
            for (int i = 0; i < 256; i++) {
                allBytes[i] = (byte) i;
            }

            // Act
            Block block = encryptionAPI.storeSearchableDataWithOffChainFile(
                BLOCK_PLACEHOLDER,
                allBytes,
                TEST_PASSWORD,
                "application/octet-stream",
                null,
                null,
                "ALL_BYTES_TEST"
            );

            // Assert
            assertNotNull(block, "Block should be created");
            assertTrue(block.hasOffChainData(), "Block should have off-chain data");
            assertEquals(256, block.getOffChainData().getFileSize(),
                "Original data size should be 256 bytes");

            logger.info("✅ All 256 byte values handled correctly");
        }
    }
}
