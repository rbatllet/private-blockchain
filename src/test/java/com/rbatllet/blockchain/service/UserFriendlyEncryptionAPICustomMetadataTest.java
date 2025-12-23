package com.rbatllet.blockchain.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.security.KeyFileLoader;
import com.rbatllet.blockchain.security.UserRole;
import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.util.CustomMetadataUtil;
import java.security.KeyPair;
import java.util.Map;
import java.util.HashMap;

/**
 * Test suite for custom metadata functionality in UserFriendlyEncryptionAPI
 * Tests the new custom metadata feature in BlockCreationOptions
 */
class UserFriendlyEncryptionAPICustomMetadataTest {

    private Blockchain blockchain;
    private UserFriendlyEncryptionAPI api;
    private KeyPair bootstrapKeyPair;
    private final String testUsername = "test-user";

    @BeforeEach
    void setUp() throws Exception {
        blockchain = new Blockchain();
        // Clean database before each test to ensure test isolation
        blockchain.clearAndReinitialize();

        // Load bootstrap admin keys
        bootstrapKeyPair = KeyFileLoader.loadKeyPairFromFiles(
            "./keys/genesis-admin.private",
            "./keys/genesis-admin.public"
        );

        // Register bootstrap admin in blockchain (RBAC v1.0.6)
        blockchain.createBootstrapAdmin(
            CryptoUtil.publicKeyToString(bootstrapKeyPair.getPublic()),
            "BOOTSTRAP_ADMIN"
        );

        KeyPair defaultKeyPair = CryptoUtil.generateKeyPair();

        // RBAC FIX (v1.0.6): testUsername needs ADMIN role to create other users via createUser()
        String publicKeyString = CryptoUtil.publicKeyToString(defaultKeyPair.getPublic());
        blockchain.addAuthorizedKey(publicKeyString, testUsername, bootstrapKeyPair, UserRole.ADMIN);

        api = new UserFriendlyEncryptionAPI(blockchain, testUsername, defaultKeyPair);
    }

    @AfterEach
    void tearDown() {
        // Clean database after each test to ensure test isolation
        if (blockchain != null) {
            blockchain.clearAndReinitialize();
        }
    }

    @Test
    void testCustomMetadataUtil_SerializationDeserialization() {
        // Arrange
        Map<String, String> originalMetadata = new HashMap<>();
        originalMetadata.put("author", "John Doe");
        originalMetadata.put("version", "1.0");
        originalMetadata.put("priority", "high");

        // Act: Serialize and deserialize
        String jsonString = CustomMetadataUtil.serializeMetadata(originalMetadata);
        Map<String, String> deserializedMetadata = CustomMetadataUtil.deserializeMetadata(jsonString);

        // Assert
        assertNotNull(jsonString, "Serialized JSON should not be null");
        assertFalse(jsonString.trim().isEmpty(), "Serialized JSON should not be empty");
        assertEquals(originalMetadata, deserializedMetadata, "Deserialized metadata should match original");
    }

    @Test
    void testCustomMetadataUtil_EmptyMetadata() {
        // Arrange
        Map<String, String> emptyMetadata = new HashMap<>();

        // Act: Serialize empty metadata
        String jsonString = CustomMetadataUtil.serializeMetadata(emptyMetadata);
        String nullString = CustomMetadataUtil.serializeMetadata(null);

        // Assert
        assertNull(jsonString, "Empty metadata should serialize to null");
        assertNull(nullString, "Null metadata should serialize to null");
        
        // Act: Deserialize null/empty strings
        Map<String, String> emptyResult = CustomMetadataUtil.deserializeMetadata("");
        Map<String, String> nullResult = CustomMetadataUtil.deserializeMetadata(null);

        // Assert
        assertTrue(emptyResult.isEmpty(), "Empty string should deserialize to empty map");
        assertTrue(nullResult.isEmpty(), "Null string should deserialize to empty map");
    }

    @Test
    void testCustomMetadataValidation_Success() {
        // Arrange
        Map<String, String> validMetadata = new HashMap<>();
        validMetadata.put("author", "John Doe");
        validMetadata.put("department", "Engineering");
        validMetadata.put("project_id", "PROJ-2024-001");

        // Act & Assert: Should not throw exception
        assertDoesNotThrow(() -> CustomMetadataUtil.validateMetadata(validMetadata));
        assertDoesNotThrow(() -> CustomMetadataUtil.validateMetadata(null));
        assertDoesNotThrow(() -> CustomMetadataUtil.validateMetadata(new HashMap<>()));
    }

    @Test
    void testCustomMetadataValidation_TooManyEntries() {
        // Arrange: Create metadata with too many entries
        Map<String, String> tooManyEntries = new HashMap<>();
        for (int i = 0; i < 51; i++) {  // MAX_METADATA_ENTRIES is 50
            tooManyEntries.put("key" + i, "value" + i);
        }

        // Act & Assert: Should throw exception
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            CustomMetadataUtil.validateMetadata(tooManyEntries);
        });
        assertTrue(exception.getMessage().contains("cannot have more than 50 entries"));
    }

    @Test
    void testCustomMetadataValidation_KeyTooLong() {
        // Arrange: Create metadata with key that's too long
        Map<String, String> longKeyMetadata = new HashMap<>();
        String longKey = "a".repeat(101); // MAX_KEY_LENGTH is 100
        longKeyMetadata.put(longKey, "value");

        // Act & Assert: Should throw exception
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            CustomMetadataUtil.validateMetadata(longKeyMetadata);
        });
        assertTrue(exception.getMessage().contains("key too long"));
    }

    @Test
    void testCustomMetadataValidation_InvalidCharacters() {
        // Arrange: Create metadata with invalid characters in key
        Map<String, String> invalidKeyMetadata = new HashMap<>();
        invalidKeyMetadata.put("key'with'quotes", "value");

        // Act & Assert: Should throw exception
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            CustomMetadataUtil.validateMetadata(invalidKeyMetadata);
        });
        assertTrue(exception.getMessage().contains("invalid characters"));
    }

    @Test
    void testBlockCreationWithCustomMetadata() throws Exception {
        // Arrange
        String testContent = "Test content with custom metadata";
        Map<String, String> customMetadata = new HashMap<>();
        customMetadata.put("author", "Alice Smith");
        customMetadata.put("document_type", "report");
        customMetadata.put("classification", "internal");

        UserFriendlyEncryptionAPI.BlockCreationOptions options = 
            new UserFriendlyEncryptionAPI.BlockCreationOptions()
                .withCategory("DOCUMENT")
                .withMetadata("author", "Alice Smith")
                .withMetadata("document_type", "report")
                .withMetadata("classification", "internal");

        // Act: Create block with custom metadata
        Block block = api.createBlockWithOptions(testContent, options);

        // Assert: Verify block was created with metadata
        assertNotNull(block, "Block should be created");
        assertEquals("DOCUMENT", block.getContentCategory(), "Category should be set");
        assertNotNull(block.getCustomMetadata(), "Custom metadata should be stored");
        
        // Deserialize and verify custom metadata
        Map<String, String> storedMetadata = CustomMetadataUtil.deserializeMetadata(block.getCustomMetadata());
        assertEquals("Alice Smith", storedMetadata.get("author"), "Author metadata should match");
        assertEquals("report", storedMetadata.get("document_type"), "Document type should match");
        assertEquals("internal", storedMetadata.get("classification"), "Classification should match");
    }

    @Test
    void testEncryptedBlockWithCustomMetadata() throws Exception {
        // Arrange
        String testContent = "Secret content with metadata";
        String password = "SecurePassword123!";

        UserFriendlyEncryptionAPI.BlockCreationOptions options = 
            new UserFriendlyEncryptionAPI.BlockCreationOptions()
                .withEncryption(true)
                .withPassword(password)
                .withCategory("SECRET")
                .withMetadata("security_level", "confidential")
                .withMetadata("clearance_required", "level_3")
                .withMetadata("expires", "2024-12-31");

        // Act: Create encrypted block with custom metadata
        Block encryptedBlock = api.createBlockWithOptions(testContent, options);

        // Assert: Verify encrypted block was created with metadata
        assertNotNull(encryptedBlock, "Encrypted block should be created");
        assertTrue(encryptedBlock.getIsEncrypted(), "Block should be marked as encrypted");
        assertEquals("SECRET", encryptedBlock.getContentCategory(), "Category should be set");
        assertNotNull(encryptedBlock.getCustomMetadata(), "Custom metadata should be stored");
        
        // Verify custom metadata is preserved in encrypted blocks
        Map<String, String> storedMetadata = CustomMetadataUtil.deserializeMetadata(encryptedBlock.getCustomMetadata());
        assertEquals("confidential", storedMetadata.get("security_level"), "Security level should match");
        assertEquals("level_3", storedMetadata.get("clearance_required"), "Clearance should match");
        assertEquals("2024-12-31", storedMetadata.get("expires"), "Expiry date should match");
    }

    @Test
    void testRecipientEncryptionWithCustomMetadata() throws Exception {
        // Arrange: Create recipient user
        String recipientUsername = "recipient-user";
        KeyPair recipientKeyPair = api.createUser(recipientUsername);
        assertNotNull(recipientKeyPair, "Recipient user creation should succeed");

        String testContent = "Secret message with metadata";

        UserFriendlyEncryptionAPI.BlockCreationOptions options = 
            new UserFriendlyEncryptionAPI.BlockCreationOptions()
                .withEncryption(true)
                .withRecipient(recipientUsername)
                .withCategory("CONFIDENTIAL")
                .withMetadata("sender", testUsername)
                .withMetadata("message_type", "direct")
                .withMetadata("urgent", "true");

        // Act: Create recipient-encrypted block with custom metadata
        Block encryptedBlock = api.createBlockWithOptions(testContent, options);

        // Assert: Verify recipient-encrypted block was created with metadata
        assertNotNull(encryptedBlock, "Recipient-encrypted block should be created");
        assertTrue(encryptedBlock.getIsEncrypted(), "Block should be marked as encrypted");
        assertEquals("CONFIDENTIAL", encryptedBlock.getContentCategory(), "Category should be set");
        assertNotNull(encryptedBlock.getCustomMetadata(), "Custom metadata should be stored");
        
        // Verify custom metadata includes recipient information
        Map<String, String> storedMetadata = CustomMetadataUtil.deserializeMetadata(encryptedBlock.getCustomMetadata());
        assertEquals(testUsername, storedMetadata.get("sender"), "Sender should match");
        assertEquals("direct", storedMetadata.get("message_type"), "Message type should match");
        assertEquals("true", storedMetadata.get("urgent"), "Urgent flag should match");
    }

    @Test
    void testBlockCreationOnlyCustomMetadata() throws Exception {
        // Arrange: Create block with only custom metadata (no category or keywords)
        String testContent = "Content with only metadata";

        UserFriendlyEncryptionAPI.BlockCreationOptions options = 
            new UserFriendlyEncryptionAPI.BlockCreationOptions()
                .withMetadata("created_by", "automated_system")
                .withMetadata("batch_id", "BATCH-2024-001")
                .withMetadata("source", "data_import");

        // Act: Create block with only custom metadata
        Block block = api.createBlockWithOptions(testContent, options);

        // Assert: Verify block was created with only metadata
        assertNotNull(block, "Block should be created");
        assertNull(block.getContentCategory(), "Category should be null");
        assertNull(block.getManualKeywords(), "Keywords should be null");
        assertNotNull(block.getCustomMetadata(), "Custom metadata should be stored");
        
        // Verify metadata content
        Map<String, String> storedMetadata = CustomMetadataUtil.deserializeMetadata(block.getCustomMetadata());
        assertEquals(3, storedMetadata.size(), "Should have 3 metadata entries");
        assertEquals("automated_system", storedMetadata.get("created_by"));
        assertEquals("BATCH-2024-001", storedMetadata.get("batch_id"));
        assertEquals("data_import", storedMetadata.get("source"));
    }

    @Test
    void testCustomMetadataValidationInBlockCreation() {
        // Arrange: Create options with invalid metadata
        String testContent = "Content that will fail";
        
        UserFriendlyEncryptionAPI.BlockCreationOptions options = 
            new UserFriendlyEncryptionAPI.BlockCreationOptions()
                .withMetadata("invalid'key", "value"); // Invalid character in key

        // Act & Assert: Should throw exception during block creation
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            api.createBlockWithOptions(testContent, options);
        });
        
        // Check the full exception chain for invalid characters message
        boolean foundInvalidCharactersMessage = false;
        Throwable current = exception;
        while (current != null) {
            if (current.getMessage() != null && current.getMessage().contains("invalid characters")) {
                foundInvalidCharactersMessage = true;
                break;
            }
            current = current.getCause();
        }
        
        assertTrue(foundInvalidCharactersMessage, 
                  "Exception chain should contain 'invalid characters' message. Root exception: " + exception.getMessage());
    }
}