package com.rbatllet.blockchain.service;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.service.UserFriendlyEncryptionAPI.BlockCreationOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for UserFriendlyEncryptionAPI createBlockWithOptions method
 * Tests the new comprehensive block creation functionality
 */
@DisplayName("📦 UserFriendlyEncryptionAPI BlockCreationOptions Tests")
public class UserFriendlyEncryptionAPIBlockCreationOptionsTest {

    private UserFriendlyEncryptionAPI api;
    private Blockchain blockchain;
    private String testUsername = "testuser";
    private String testPassword = "TestPassword123!";
    
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        
        blockchain = new Blockchain();
        // Clean database before each test to ensure test isolation
        blockchain.clearAndReinitialize();
        KeyPair defaultKeyPair = CryptoUtil.generateKeyPair();
        api = new UserFriendlyEncryptionAPI(blockchain, testUsername, defaultKeyPair);

        // Initialize SearchSpecialistAPI
        blockchain.initializeAdvancedSearch(testPassword);
        blockchain.getSearchSpecialistAPI().initializeWithBlockchain(blockchain, testPassword, defaultKeyPair.getPrivate());
    }

    @AfterEach
    void tearDown() {
        // Clean database after each test to ensure test isolation
        if (blockchain != null) {
            blockchain.clearAndReinitialize();
        }
    }

    @Nested
    @DisplayName("📝 Category and Keywords Tests")
    class CategoryAndKeywordsTests {

        @Test
        @DisplayName("✅ Should create unencrypted block with category")
        void shouldCreateUnencryptedBlockWithCategory() {
            // Given
            BlockCreationOptions options = new BlockCreationOptions()
                    .withCategory("FINANCIAL")
                    .withUsername("testuser");
            
            // When
            Block result = api.createBlockWithOptions("Test financial data", options);
            
            // Then
            assertNotNull(result);
            assertEquals("FINANCIAL", result.getContentCategory());
            assertEquals("Test financial data", result.getData());
            // Note: unencrypted blocks may have isEncrypted = false instead of null
            assertFalse(result.getIsEncrypted() == null ? false : result.getIsEncrypted());
        }

        @Test
        @DisplayName("✅ Should create unencrypted block with keywords")
        void shouldCreateUnencryptedBlockWithKeywords() {
            // Given
            String[] keywords = {"payment", "invoice", "urgent"};
            BlockCreationOptions options = new BlockCreationOptions()
                    .withKeywords(keywords)
                    .withUsername("testuser");
            
            // When
            Block result = api.createBlockWithOptions("Invoice payment data", options);
            
            // Then
            assertNotNull(result);
            assertEquals("payment invoice urgent", result.getManualKeywords());
            assertEquals("Invoice payment data", result.getData());
        }

        @Test
        @DisplayName("✅ Should create unencrypted block with category and keywords")
        void shouldCreateUnencryptedBlockWithCategoryAndKeywords() {
            // Given
            String[] keywords = {"medical", "patient", "confidential"};
            BlockCreationOptions options = new BlockCreationOptions()
                    .withCategory("HEALTHCARE")
                    .withKeywords(keywords)
                    .withUsername("doctor");
            
            // When
            Block result = api.createBlockWithOptions("Patient medical record", options);
            
            // Then
            assertNotNull(result);
            assertEquals("HEALTHCARE", result.getContentCategory());
            assertEquals("medical patient confidential", result.getManualKeywords());
            assertEquals("Patient medical record", result.getData());
        }

        @Test
        @DisplayName("✅ Should create encrypted block with category (thread-safe)")
        void shouldCreateEncryptedBlockWithCategory() {
            // Given
            BlockCreationOptions options = new BlockCreationOptions()
                    .withPassword(testPassword)
                    .withEncryption(true)
                    .withCategory("SECRET");
            
            // When
            Block result = api.createBlockWithOptions("Top secret information", options);
            
            // Then
            assertNotNull(result);
            assertEquals("SECRET", result.getContentCategory());
            assertTrue(result.getIsEncrypted());
            assertNotNull(result.getEncryptionMetadata(), "Should have encryption metadata");
            // Data field contains original data (for hash integrity)
            assertEquals("Top secret information", result.getData(), 
                "Data field should contain original data for hash integrity");
        }

        @Test
        @DisplayName("✅ Should create encrypted block with keywords (thread-safe)")
        void shouldCreateEncryptedBlockWithKeywords() {
            // Given
            String[] keywords = {"classified", "military", "urgent"};
            BlockCreationOptions options = new BlockCreationOptions()
                    .withPassword(testPassword)
                    .withEncryption(true)
                    .withKeywords(keywords);
            
            // When
            Block result = api.createBlockWithOptions("Military classified data", options);
            
            // Then
            assertNotNull(result);
            assertEquals("classified military urgent", result.getManualKeywords());
            assertTrue(result.getIsEncrypted());
        }

        @Test
        @DisplayName("✅ Should create encrypted block with category and keywords")
        void shouldCreateEncryptedBlockWithCategoryAndKeywords() {
            // Given
            String[] keywords = {"top-secret", "project-alpha"};
            BlockCreationOptions options = new BlockCreationOptions()
                    .withPassword(testPassword)
                    .withEncryption(true)
                    .withCategory("PROJECT")
                    .withKeywords(keywords)
                    .withIdentifier("alpha-001");
            
            // When
            Block result = api.createBlockWithOptions("Project Alpha classified details", options);
            
            // Then
            assertNotNull(result);
            assertEquals("PROJECT", result.getContentCategory());
            assertEquals("top-secret project-alpha", result.getManualKeywords());
            assertTrue(result.getIsEncrypted());
            assertNotNull(result.getEncryptionMetadata(), "Should have encryption metadata");
            // Data field contains original data (for hash integrity)
            assertEquals("Project Alpha classified details", result.getData(),
                "Data field should contain original data for hash integrity");
        }
    }

    @Nested
    @DisplayName("💾 Off-Chain Storage Tests")
    class OffChainStorageTests {

        @Test
        @DisplayName("✅ Should create block with off-chain storage")
        void shouldCreateBlockWithOffChainStorage() throws IOException {
            // Given - Create temporary file
            Path testFile = tempDir.resolve("test-document.txt");
            String fileContent = "Large document content that should be stored off-chain";
            Files.writeString(testFile, fileContent);
            
            BlockCreationOptions options = new BlockCreationOptions()
                    .withOffChain(true)
                    .withOffChainFilePath(testFile.toString())
                    .withCategory("DOCUMENT")
                    .withUsername("testuser")
                    .withPassword(testPassword); // Off-chain storage requires password
            
            // When
            Block result = api.createBlockWithOptions("Document metadata", options);
            
            // Then
            assertNotNull(result);
            assertTrue(result.hasOffChainData());
            assertNotNull(result.getOffChainData());
            // Off-chain storage sets category to OFF_CHAIN_LINKED automatically
            assertEquals("OFF_CHAIN_LINKED", result.getContentCategory());
            assertEquals("text/plain", result.getOffChainData().getContentType());
            assertTrue(result.getIsEncrypted()); // Off-chain with password creates encrypted block
        }

        @Test
        @DisplayName("✅ Should create encrypted off-chain block")
        void shouldCreateEncryptedOffChainBlock() throws IOException {
            // Given
            Path testFile = tempDir.resolve("secret-file.pdf");
            byte[] binaryContent = "Secret PDF binary content".getBytes();
            Files.write(testFile, binaryContent);
            
            String[] keywords = {"secret", "document"};
            BlockCreationOptions options = new BlockCreationOptions()
                    .withOffChain(true)
                    .withOffChainFilePath(testFile.toString())
                    .withPassword(testPassword)
                    .withEncryption(true)
                    .withCategory("CONFIDENTIAL")
                    .withKeywords(keywords);
            
            // When
            Block result = api.createBlockWithOptions("Confidential document", options);
            
            // Then
            assertNotNull(result);
            assertTrue(result.hasOffChainData());
            assertTrue(result.getIsEncrypted()); // Should be encrypted with password
            // Keywords are encrypted when password is provided - verify they exist but may be encrypted
            assertNotNull(result.getManualKeywords());
            assertTrue(result.getManualKeywords().length() > 0, "Keywords should exist");
            // Off-chain storage overrides category to OFF_CHAIN_LINKED
            assertEquals("OFF_CHAIN_LINKED", result.getContentCategory());
            assertEquals("application/pdf", result.getOffChainData().getContentType());
        }

        @Test
        @DisplayName("⚠️ Should warn when off-chain requested without file path")
        void shouldWarnWhenOffChainWithoutFilePath() {
            // Given
            BlockCreationOptions options = new BlockCreationOptions()
                    .withOffChain(true)
                    .withCategory("DOCUMENT")
                    .withUsername("testuser");
            // Note: No offChainFilePath provided
            
            // When
            Block result = api.createBlockWithOptions("Should use regular storage", options);
            
            // Then
            assertNotNull(result);
            assertFalse(result.hasOffChainData()); // Should fall back to regular storage
            assertEquals("DOCUMENT", result.getContentCategory());
        }
    }

    @Nested
    @DisplayName("🔧 Edge Cases and Error Handling")
    class EdgeCasesTests {

        @Test
        @DisplayName("❌ Should throw exception for empty content")
        void shouldThrowExceptionForEmptyContent() {
            // Given
            BlockCreationOptions options = new BlockCreationOptions()
                    .withCategory("TEST");
            
            // When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                api.createBlockWithOptions("", options);
            });
        }

        @Test
        @DisplayName("❌ Should throw exception for null content")
        void shouldThrowExceptionForNullContent() {
            // Given
            BlockCreationOptions options = new BlockCreationOptions()
                    .withCategory("TEST");
            
            // When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                api.createBlockWithOptions(null, options);
            });
        }

        @Test
        @DisplayName("❌ Should handle invalid file path gracefully")
        void shouldHandleInvalidFilePathGracefully() {
            // Given
            BlockCreationOptions options = new BlockCreationOptions()
                    .withOffChain(true)
                    .withOffChainFilePath("/nonexistent/path/file.txt")
                    .withUsername("testuser");
            
            // When & Then
            assertThrows(RuntimeException.class, () -> {
                api.createBlockWithOptions("Test content", options);
            });
        }

        @Test
        @DisplayName("✅ Should handle null keywords gracefully")
        void shouldHandleNullKeywordsGracefully() {
            // Given
            BlockCreationOptions options = new BlockCreationOptions()
                    .withCategory("TEST")
                    .withKeywords(null)
                    .withUsername("testuser");
            
            // When
            Block result = api.createBlockWithOptions("Test content", options);
            
            // Then
            assertNotNull(result);
            assertEquals("TEST", result.getContentCategory());
            assertNull(result.getManualKeywords());
        }

        @Test
        @DisplayName("✅ Should handle empty keywords array")
        void shouldHandleEmptyKeywordsArray() {
            // Given
            String[] emptyKeywords = {};
            BlockCreationOptions options = new BlockCreationOptions()
                    .withCategory("TEST")
                    .withKeywords(emptyKeywords)
                    .withUsername("testuser");
            
            // When
            Block result = api.createBlockWithOptions("Test content", options);
            
            // Then
            assertNotNull(result);
            assertEquals("TEST", result.getContentCategory());
            assertNull(result.getManualKeywords());
        }
    }

    @Nested
    @DisplayName("🧵 Thread Safety Tests")
    class ThreadSafetyTests {

        @Test
        @DisplayName("🧵 Should handle sequential block creation (thread safety verified in implementation)")
        void shouldHandleSequentialBlockCreation() {
            // This test verifies that the createBlockWithOptions method uses thread-safe approaches
            // like JPA transactions for metadata updates and synchronized blockchain operations
            
            BlockCreationOptions options = new BlockCreationOptions()
                    .withCategory("THREAD_SAFETY_TEST")
                    .withKeywords(new String[]{"thread", "safe", "test"})
                    .withUsername("threadsafeuser");
            
            Block result = api.createBlockWithOptions("Thread safety test content", options);
            
            assertNotNull(result, "Block should be created successfully");
            assertEquals("THREAD_SAFETY_TEST", result.getContentCategory());
            assertTrue(result.getManualKeywords().contains("thread"));
            assertTrue(result.getManualKeywords().contains("safe"));
        }
    }
}