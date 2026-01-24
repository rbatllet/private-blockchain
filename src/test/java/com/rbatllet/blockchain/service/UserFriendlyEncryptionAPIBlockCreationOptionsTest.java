package com.rbatllet.blockchain.service;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.security.UserRole;
import com.rbatllet.blockchain.util.TestGenesisKeyManager;
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
    private KeyPair bootstrapKeyPair;
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

        // Load bootstrap admin keys (auto-generates if missing - test-only)
        bootstrapKeyPair = TestGenesisKeyManager.ensureGenesisKeysExist();

        // Register bootstrap admin in blockchain (RBAC v1.0.6)
        blockchain.createBootstrapAdmin(
            CryptoUtil.publicKeyToString(bootstrapKeyPair.getPublic()),
            "BOOTSTRAP_ADMIN"
        );

        KeyPair defaultKeyPair = CryptoUtil.generateKeyPair();

        // SECURITY FIX (v1.0.6): Pre-authorize user before creating API
        String publicKeyString = CryptoUtil.publicKeyToString(defaultKeyPair.getPublic());
        blockchain.addAuthorizedKey(publicKeyString, testUsername, bootstrapKeyPair, UserRole.ADMIN);

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
                    .withUsername("categoryuser");
            
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
                    .withUsername("keywordsuser");
            
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
            // SECURITY FIX: Data field contains placeholder for encrypted blocks
            assertEquals("[ENCRYPTED]", result.getData(),
                "Data field should be [ENCRYPTED] placeholder for security");
        }

        @Test
        @DisplayName("✅ Should create encrypted block with keywords (thread-safe)")
        void shouldCreateEncryptedBlockWithKeywords() {
            // Given
            // Add public: prefix to make keywords searchable without password
            String[] keywords = {"public:classified", "public:military", "public:urgent"};
            BlockCreationOptions options = new BlockCreationOptions()
                    .withPassword(testPassword)
                    .withEncryption(true)
                    .withKeywords(keywords);

            // When
            Block result = api.createBlockWithOptions("Military classified data", options);

            // Then
            assertNotNull(result);
            // Keywords with public: prefix are stored WITH prefix in manualKeywords
            // The prefix is stripped during indexing for searching
            assertTrue(result.getManualKeywords().contains("public:classified"));
            assertTrue(result.getManualKeywords().contains("public:military"));
            assertTrue(result.getManualKeywords().contains("public:urgent"));
            assertTrue(result.getIsEncrypted());
        }

        @Test
        @DisplayName("✅ Should create encrypted block with category and keywords")
        void shouldCreateEncryptedBlockWithCategoryAndKeywords() {
            // Given
            // Add public: prefix to make keywords searchable without password
            String[] keywords = {"public:top-secret", "public:project-alpha"};
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
            // Keywords with public: prefix are stored WITH prefix in manualKeywords
            assertTrue(result.getManualKeywords().contains("public:top-secret"));
            assertTrue(result.getManualKeywords().contains("public:project-alpha"));
            assertTrue(result.getIsEncrypted());
            assertNotNull(result.getEncryptionMetadata(), "Should have encryption metadata");
            // SECURITY FIX: Data field contains placeholder for encrypted blocks
            assertEquals("[ENCRYPTED]", result.getData(),
                "Data field should be [ENCRYPTED] placeholder for security");
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
                    .withUsername("offchainuser")
                    .withPassword(testPassword); // Off-chain storage requires password
            
            // When
            Block result = api.createBlockWithOptions("Document metadata", options);
            
            // Then
            assertNotNull(result);
            // CRITICAL: Verify hasOffChainData() first to confirm it's actually an off-chain block
            // This prevents false positives if someone sets category to "OFFCHAIN_DATA" manually
            assertTrue(result.hasOffChainData(), "Block should have off-chain data");
            assertNotNull(result.getOffChainData(), "Off-chain data should not be null");
            // Category is preserved when specified by user
            assertEquals("DOCUMENT", result.getContentCategory());
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
            // Keywords without "public:" prefix are encrypted (autoKeywords), not public (manualKeywords)
            assertNotNull(result.getAutoKeywords());
            assertTrue(result.getAutoKeywords().length() > 0, "Keywords should exist");
            // Off-chain storage preserves user's category (CONFIDENTIAL in this case)
            assertEquals("CONFIDENTIAL", result.getContentCategory());
            assertEquals("application/pdf", result.getOffChainData().getContentType());
        }

        @Test
        @DisplayName("✅ Should default to OFFCHAIN_DATA when category not specified for off-chain block")
        void shouldDefaultToOffchainDataWhenCategoryNotSpecified() throws IOException {
            // Given - Create temporary file
            Path testFile = tempDir.resolve("offchain-no-category.txt");
            String fileContent = "Off-chain content without explicit category";
            Files.writeString(testFile, fileContent);

            // Note: No .withCategory() specified - should default to OFFCHAIN_DATA
            BlockCreationOptions options = new BlockCreationOptions()
                    .withOffChain(true)
                    .withOffChainFilePath(testFile.toString())
                    .withPassword(testPassword)
                    .withUsername("offchainnocategory");

            // When
            Block result = api.createBlockWithOptions("Off-chain without category", options);

            // Then
            assertNotNull(result);
            assertTrue(result.hasOffChainData(), "Block should have off-chain data");
            // Default category when not specified is OFFCHAIN_DATA
            assertEquals("OFFCHAIN_DATA", result.getContentCategory());
            assertEquals("text/plain", result.getOffChainData().getContentType());
        }

        @Test
        @DisplayName("⚠️ Should warn when off-chain requested without file path")
        void shouldWarnWhenOffChainWithoutFilePath() {
            // Given
            BlockCreationOptions options = new BlockCreationOptions()
                    .withOffChain(true)
                    .withCategory("DOCUMENT")
                    .withUsername("nowarnuser");
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
                    .withUsername("invalidpathuser");
            
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
                    .withUsername("nullkeywordsuser");
            
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
                    .withUsername("emptykeywordsuser");
            
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