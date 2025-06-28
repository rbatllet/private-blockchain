package com.rbatllet.blockchain.util.validation;

import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.entity.OffChainData;
import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.util.CryptoUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.security.KeyPair;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test class for BlockValidationUtil
 * Tests all validation methods including off-chain data validation
 */
@DisplayName("BlockValidationUtil Tests")
public class BlockValidationUtilTest {
    
    // Test implementations instead of mocks
    private Block testGenesisBlock;
    private TestBlockchain testBlockchain;
    private KeyPair testKeyPair;
    private String testPublicKey;
    
    @TempDir
    Path tempDir;
    
    // Test implementation of Block
    private static class TestBlock extends Block {
        public TestBlock(Long blockNumber, String previousHash) {
            setBlockNumber(blockNumber);
            setPreviousHash(previousHash);
            setHash("hash" + blockNumber);
            setTimestamp(LocalDateTime.now());
            setData("Test data for block " + blockNumber);
        }
    }
    
    // Test implementation of Blockchain
    private static class TestBlockchain extends Blockchain {
        private boolean keyAuthorized;
        private RuntimeException keyAuthorizedException;
        
        public TestBlockchain() {
            // Call default constructor of Blockchain
            super();
            this.keyAuthorized = false;
            this.keyAuthorizedException = null;
        }
        
        public void setKeyAuthorized(boolean authorized) {
            this.keyAuthorized = authorized;
        }
        
        public void setKeyAuthorizedException(RuntimeException exception) {
            this.keyAuthorizedException = exception;
        }
        
        // Override wasKeyAuthorizedAt for testing
        public boolean wasKeyAuthorizedAt(String publicKey, LocalDateTime timestamp) {
            if (keyAuthorizedException != null) {
                throw keyAuthorizedException;
            }
            return keyAuthorized;
        }
    }
    
    @BeforeEach
    public void setUp() {
        // Initialize test objects
        testGenesisBlock = new TestBlock(0L, "0");
        testBlockchain = new TestBlockchain();
        testKeyPair = CryptoUtil.generateKeyPair();
        testPublicKey = CryptoUtil.publicKeyToString(testKeyPair.getPublic());
    }
    
    @Test
    public void testValidateGenesisBlock_Valid() {
        assertTrue(BlockValidationUtil.validateGenesisBlock(testGenesisBlock), 
                "Valid genesis block should pass validation");
    }
    
    @Test
    public void testValidateGenesisBlock_InvalidBlockNumber() {
        // Create a test block with invalid block number for genesis
        Block invalidBlock = new TestBlock(1L, "0");
        
        assertFalse(BlockValidationUtil.validateGenesisBlock(invalidBlock), 
                "Genesis block with non-zero block number should fail validation");
    }
    
    @Test
    public void testValidateGenesisBlock_InvalidPreviousHash() {
        // Create a test block with invalid previous hash for genesis
        Block invalidBlock = new TestBlock(0L, "not-zero-hash");
        
        assertFalse(BlockValidationUtil.validateGenesisBlock(invalidBlock), 
                "Genesis block with non-zero previous hash should fail validation");
    }
    
    @Test
    public void testWasKeyAuthorizedAt_Authorized() {
        String publicKey = "test-public-key";
        LocalDateTime timestamp = LocalDateTime.now();
        
        // Set up the test blockchain to return true for key authorization
        testBlockchain.setKeyAuthorized(true);
        
        assertTrue(BlockValidationUtil.wasKeyAuthorizedAt(testBlockchain, publicKey, timestamp),
                "Should return true when key was authorized");
    }
    
    @Test
    public void testWasKeyAuthorizedAt_NotAuthorized() {
        String publicKey = "test-public-key";
        LocalDateTime timestamp = LocalDateTime.now();
        
        // Set up the test blockchain to return false for key authorization
        testBlockchain.setKeyAuthorized(false);
        
        assertFalse(BlockValidationUtil.wasKeyAuthorizedAt(testBlockchain, publicKey, timestamp),
                "Should return false when key was not authorized");
    }
    
    @Test
    public void testWasKeyAuthorizedAt_Exception() {
        String publicKey = "test-public-key";
        LocalDateTime timestamp = LocalDateTime.now();
        
        // Set up the test blockchain to throw an exception for key authorization
        testBlockchain.setKeyAuthorizedException(new RuntimeException("Test exception"));
        
        assertFalse(BlockValidationUtil.wasKeyAuthorizedAt(testBlockchain, publicKey, timestamp),
                "Should return false when an exception occurs");
    }
    
    @Test
    public void testTruncateHash_NullHash() {
        assertEquals("null", BlockValidationUtil.truncateHash(null),
                "Should return 'null' for null hash");
    }
    
    @Test
    public void testTruncateHash_ShortHash() {
        String shortHash = "abcdef";
        assertEquals(shortHash, BlockValidationUtil.truncateHash(shortHash),
                "Short hash should not be truncated");
    }
    
    @Test
    public void testTruncateHash_LongHash() {
        String longHash = "abcdef1234567890abcdef1234567890abcdef1234567890";
        String expected = "abcdef1234567890...7890abcdef1234567890";
        
        assertEquals(expected, BlockValidationUtil.truncateHash(longHash),
                "Long hash should be truncated with ellipsis in the middle");
    }

    @Nested
    @DisplayName("Off-Chain Data Validation Tests")
    class OffChainDataValidationTests {

        @Test
        @DisplayName("Should validate block with no off-chain data")
        public void testValidateOffChainData_NoOffChainData() {
            Block regularBlock = new TestBlock(1L, "previousHash");
            
            assertTrue(BlockValidationUtil.validateOffChainData(testBlockchain, regularBlock),
                    "Block with no off-chain data should pass validation");
            assertTrue(BlockValidationUtil.offChainFileExists(regularBlock),
                    "Block with no off-chain data should pass file existence check");
            assertTrue(BlockValidationUtil.validateOffChainMetadata(regularBlock),
                    "Block with no off-chain data should pass metadata validation");
        }

        @Test
        @DisplayName("Should validate block with valid off-chain data file")
        public void testValidateOffChainData_ValidFile() throws IOException {
            // Create a temporary file
            File tempFile = tempDir.resolve("test-offchain.dat").toFile();
            try (FileWriter writer = new FileWriter(tempFile)) {
                writer.write("Test off-chain data content");
            }

            // Create off-chain data
            OffChainData offChainData = new OffChainData();
            offChainData.setFilePath(tempFile.getAbsolutePath());
            offChainData.setDataHash("test-hash");
            offChainData.setSignature("test-signature");
            offChainData.setEncryptionIV("test-iv");
            offChainData.setFileSize(tempFile.length());
            offChainData.setCreatedAt(LocalDateTime.now());

            Block blockWithOffChain = new TestBlock(1L, "previousHash");
            blockWithOffChain.setOffChainData(offChainData);

            assertTrue(BlockValidationUtil.offChainFileExists(blockWithOffChain),
                    "Block with valid off-chain file should pass file existence check");
            assertTrue(BlockValidationUtil.validateOffChainMetadata(blockWithOffChain),
                    "Block with valid off-chain metadata should pass validation");
        }

        @Test
        @DisplayName("Should reject block with missing off-chain file")
        public void testValidateOffChainData_MissingFile() {
            OffChainData offChainData = new OffChainData();
            offChainData.setFilePath("/nonexistent/file.dat");
            offChainData.setDataHash("test-hash");
            offChainData.setSignature("test-signature");
            offChainData.setEncryptionIV("test-iv");
            offChainData.setFileSize(100L);
            offChainData.setCreatedAt(LocalDateTime.now());

            Block blockWithOffChain = new TestBlock(1L, "previousHash");
            blockWithOffChain.setOffChainData(offChainData);

            assertFalse(BlockValidationUtil.offChainFileExists(blockWithOffChain),
                    "Block with missing off-chain file should fail file existence check");
        }

        @Test
        @DisplayName("Should reject block with incomplete off-chain metadata")
        public void testValidateOffChainMetadata_IncompleteData() {
            OffChainData incompleteData = new OffChainData();
            incompleteData.setFilePath("test.dat"); // Missing other required fields

            Block blockWithIncomplete = new TestBlock(1L, "previousHash");
            blockWithIncomplete.setOffChainData(incompleteData);

            assertFalse(BlockValidationUtil.validateOffChainMetadata(blockWithIncomplete),
                    "Block with incomplete off-chain metadata should fail validation");
        }

        @Test
        @DisplayName("Should detect file size mismatch beyond AES padding tolerance")
        public void testValidateOffChainMetadata_FileSizeMismatch() throws IOException {
            // Create a temporary file
            File tempFile = tempDir.resolve("test-mismatch.dat").toFile();
            try (FileWriter writer = new FileWriter(tempFile)) {
                writer.write("Short content");
            }

            // Create off-chain data with wrong file size (beyond 16 bytes tolerance)
            OffChainData offChainData = new OffChainData();
            offChainData.setFilePath(tempFile.getAbsolutePath());
            offChainData.setDataHash("test-hash");
            offChainData.setSignature("test-signature");
            offChainData.setEncryptionIV("test-iv");
            offChainData.setFileSize(tempFile.length() + 100); // Much larger difference
            offChainData.setCreatedAt(LocalDateTime.now());

            Block blockWithMismatch = new TestBlock(1L, "previousHash");
            blockWithMismatch.setOffChainData(offChainData);

            assertFalse(BlockValidationUtil.validateOffChainMetadata(blockWithMismatch),
                    "Block with significant file size mismatch should fail validation");
        }

        @Test
        @DisplayName("Should allow file size difference within AES padding tolerance")
        public void testValidateOffChainMetadata_AESPaddingTolerance() throws IOException {
            // Create a temporary file
            File tempFile = tempDir.resolve("test-padding.dat").toFile();
            try (FileWriter writer = new FileWriter(tempFile)) {
                writer.write("Test content for AES padding test");
            }

            // Create off-chain data with file size difference within 16 bytes tolerance
            OffChainData offChainData = new OffChainData();
            offChainData.setFilePath(tempFile.getAbsolutePath());
            offChainData.setDataHash("test-hash");
            offChainData.setSignature("test-signature");
            offChainData.setEncryptionIV("test-iv");
            offChainData.setFileSize(tempFile.length() + 10); // Within 16 bytes tolerance
            offChainData.setCreatedAt(LocalDateTime.now());

            Block blockWithPadding = new TestBlock(1L, "previousHash");
            blockWithPadding.setOffChainData(offChainData);

            assertTrue(BlockValidationUtil.validateOffChainMetadata(blockWithPadding),
                    "Block with file size difference within AES padding tolerance should pass validation");
        }
    }

    @Nested
    @DisplayName("Detailed Off-Chain Validation Tests")
    class DetailedOffChainValidationTests {

        @Test
        @DisplayName("Should return valid result for block with no off-chain data")
        public void testValidateOffChainDataDetailed_NoOffChainData() {
            Block regularBlock = new TestBlock(1L, "previousHash");
            
            var result = BlockValidationUtil.validateOffChainDataDetailed(regularBlock);
            
            assertTrue(result.isValid(), "Block with no off-chain data should be valid");
            assertEquals("No off-chain data to validate", result.getMessage());
        }

        @Test
        @DisplayName("Should return valid result for complete and valid off-chain data")
        public void testValidateOffChainDataDetailed_ValidData() throws IOException {
            File tempFile = tempDir.resolve("test-detailed.dat").toFile();
            try (FileWriter writer = new FileWriter(tempFile)) {
                writer.write("Test content for detailed validation");
            }

            OffChainData offChainData = new OffChainData();
            offChainData.setFilePath(tempFile.getAbsolutePath());
            offChainData.setDataHash("test-hash");
            offChainData.setSignature("test-signature");
            offChainData.setEncryptionIV("test-iv");
            offChainData.setFileSize(tempFile.length());
            offChainData.setCreatedAt(LocalDateTime.now());

            Block blockWithOffChain = new TestBlock(1L, "previousHash");
            blockWithOffChain.setOffChainData(offChainData);

            var result = BlockValidationUtil.validateOffChainDataDetailed(blockWithOffChain);
            
            assertTrue(result.isValid(), "Block with valid off-chain data should be valid");
            assertEquals("Off-chain data validation passed", result.getMessage());
        }

        @Test
        @DisplayName("Should return detailed error for missing required fields")
        public void testValidateOffChainDataDetailed_MissingFields() {
            OffChainData incompleteData = new OffChainData();
            incompleteData.setFilePath("test.dat");
            // Missing hash, signature, and IV

            Block blockWithIncomplete = new TestBlock(1L, "previousHash");
            blockWithIncomplete.setOffChainData(incompleteData);

            var result = BlockValidationUtil.validateOffChainDataDetailed(blockWithIncomplete);
            
            assertFalse(result.isValid(), "Block with incomplete off-chain data should be invalid");
            assertTrue(result.getMessage().contains("Missing data hash"), 
                    "Error message should mention missing data hash");
            assertTrue(result.getMessage().contains("Missing signature"), 
                    "Error message should mention missing signature");
            assertTrue(result.getMessage().contains("Missing encryption IV"), 
                    "Error message should mention missing encryption IV");
        }

        @Test
        @DisplayName("Should detect empty file")
        public void testValidateOffChainDataDetailed_EmptyFile() throws IOException {
            File emptyFile = tempDir.resolve("empty-file.dat").toFile();
            emptyFile.createNewFile(); // Creates empty file

            OffChainData offChainData = new OffChainData();
            offChainData.setFilePath(emptyFile.getAbsolutePath());
            offChainData.setDataHash("test-hash");
            offChainData.setSignature("test-signature");
            offChainData.setEncryptionIV("test-iv");
            offChainData.setFileSize(0L);
            offChainData.setCreatedAt(LocalDateTime.now());

            Block blockWithEmpty = new TestBlock(1L, "previousHash");
            blockWithEmpty.setOffChainData(offChainData);

            var result = BlockValidationUtil.validateOffChainDataDetailed(blockWithEmpty);
            
            assertFalse(result.isValid(), "Block with empty off-chain file should be invalid");
            assertTrue(result.getMessage().contains("File is empty"), 
                    "Error message should mention empty file");
        }
    }

    @Nested
    @DisplayName("Tampering Detection Tests")
    class TamperingDetectionTests {

        @Test
        @DisplayName("Should not detect tampering for block with no off-chain data")
        public void testDetectOffChainTampering_NoOffChainData() {
            Block regularBlock = new TestBlock(1L, "previousHash");
            
            assertTrue(BlockValidationUtil.detectOffChainTampering(regularBlock),
                    "Block with no off-chain data should not show tampering");
        }

        @Test
        @DisplayName("Should detect tampering when file is missing")
        public void testDetectOffChainTampering_MissingFile() {
            OffChainData offChainData = new OffChainData();
            offChainData.setFilePath("/nonexistent/file.dat");
            offChainData.setDataHash("test-hash");
            offChainData.setSignature("test-signature");
            offChainData.setEncryptionIV("test-iv");
            offChainData.setCreatedAt(LocalDateTime.now());

            Block blockWithMissing = new TestBlock(1L, "previousHash");
            blockWithMissing.setOffChainData(offChainData);

            assertFalse(BlockValidationUtil.detectOffChainTampering(blockWithMissing),
                    "Block with missing off-chain file should show tampering");
        }

        @Test
        @DisplayName("Should not detect tampering for recently created file")
        public void testDetectOffChainTampering_RecentFile() throws IOException {
            File tempFile = tempDir.resolve("recent-file.dat").toFile();
            try (FileWriter writer = new FileWriter(tempFile)) {
                writer.write("Recent content");
            }

            OffChainData offChainData = new OffChainData();
            offChainData.setFilePath(tempFile.getAbsolutePath());
            offChainData.setDataHash("test-hash");
            offChainData.setSignature("test-signature");
            offChainData.setEncryptionIV("test-iv");
            offChainData.setCreatedAt(LocalDateTime.now()); // Same time as file creation

            Block blockWithRecent = new TestBlock(1L, "previousHash");
            blockWithRecent.setOffChainData(offChainData);

            assertTrue(BlockValidationUtil.detectOffChainTampering(blockWithRecent),
                    "Block with recently created off-chain file should not show tampering");
        }
    }

    @Nested
    @DisplayName("OffChainValidationResult Tests")
    class OffChainValidationResultTests {

        @Test
        @DisplayName("Should create valid result correctly")
        public void testOffChainValidationResult_Valid() {
            var result = new BlockValidationUtil.OffChainValidationResult(true, "All good");
            
            assertTrue(result.isValid(), "Result should be valid");
            assertEquals("All good", result.getMessage(), "Message should match");
        }

        @Test
        @DisplayName("Should create invalid result correctly")
        public void testOffChainValidationResult_Invalid() {
            var result = new BlockValidationUtil.OffChainValidationResult(false, "Error found");
            
            assertFalse(result.isValid(), "Result should be invalid");
            assertEquals("Error found", result.getMessage(), "Message should match");
        }

        @Test
        @DisplayName("Should have correct toString representation")
        public void testOffChainValidationResult_ToString() {
            var result = new BlockValidationUtil.OffChainValidationResult(true, "Test message");
            String toString = result.toString();
            
            assertTrue(toString.contains("valid=true"), "toString should contain valid=true");
            assertTrue(toString.contains("message='Test message'"), "toString should contain the message");
        }
    }

    @Test
    @DisplayName("Should validate genesis block with null block number")
    public void testValidateGenesisBlock_NullBlockNumber() {
        Block invalidBlock = new TestBlock(null, "0");
        
        assertFalse(BlockValidationUtil.validateGenesisBlock(invalidBlock),
                "Genesis block with null block number should fail validation");
    }
}
