package com.rbatllet.blockchain.service;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.util.CryptoUtil;
import java.security.KeyPair;

/**
 * Test suite for recipient-specific encryption functionality in UserFriendlyEncryptionAPI
 * Tests the new recipientUsername feature in BlockCreationOptions
 */
class UserFriendlyEncryptionAPIRecipientTest {

    private Blockchain blockchain;
    private UserFriendlyEncryptionAPI api;
    private final String testUsername = "test-sender";
    private final String recipientUsername = "test-recipient";

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        blockchain = new Blockchain();
        // Clean database before each test to ensure test isolation
        blockchain.clearAndReinitialize();
        
        KeyPair defaultKeyPair = CryptoUtil.generateKeyPair();
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
    void testRecipientEncryption_Success() throws Exception {
        // Arrange: Create recipient user
        KeyPair recipientKeyPair = api.createUser(recipientUsername);
        assertNotNull(recipientKeyPair, "Recipient user creation should succeed");

        String testContent = "This is a secret message for the recipient";

        UserFriendlyEncryptionAPI.BlockCreationOptions options = 
            new UserFriendlyEncryptionAPI.BlockCreationOptions()
                .withEncryption(true)
                .withRecipient(recipientUsername)
                .withCategory("SECRET");

        // Act: Create block with recipient encryption
        Block encryptedBlock = api.createBlockWithOptions(testContent, options);

        // Assert: Verify block was created and encrypted
        assertNotNull(encryptedBlock, "Encrypted block should be created");
        assertTrue(encryptedBlock.getIsEncrypted(), "Block should be marked as encrypted");
        assertNotNull(encryptedBlock.getData(), "Block should have encrypted data");
        assertTrue(encryptedBlock.getData().startsWith("RECIPIENT_ENCRYPTED:" + recipientUsername + ":"), 
            "Block should contain recipient encryption marker");
        assertEquals("SECRET", encryptedBlock.getContentCategory(), "Category should be set correctly");
    }

    @Test
    void testRecipientEncryption_RecipientNotFound() throws Exception {
        // Arrange
        String testContent = "This message is for non-existent user";
        String nonExistentUser = "non-existent-user";

        UserFriendlyEncryptionAPI.BlockCreationOptions options = 
            new UserFriendlyEncryptionAPI.BlockCreationOptions()
                .withEncryption(true)
                .withRecipient(nonExistentUser);

        // Act & Assert: Should throw exception
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            api.createBlockWithOptions(testContent, options);
        });

        assertTrue(exception.getMessage().contains("Recipient user '" + nonExistentUser + "' not found"), 
            "Exception should mention recipient not found");
    }

    @Test
    void testRecipientEncryption_WithKeywords() throws Exception {
        // Arrange: Create recipient user
        KeyPair recipientKeyPair = api.createUser(recipientUsername);
        assertNotNull(recipientKeyPair, "Recipient user creation should succeed");

        String testContent = "Confidential project data";
        String[] keywords = {"project", "confidential", "data"};

        UserFriendlyEncryptionAPI.BlockCreationOptions options = 
            new UserFriendlyEncryptionAPI.BlockCreationOptions()
                .withEncryption(true)
                .withRecipient(recipientUsername)
                .withCategory("PROJECT")
                .withKeywords(keywords);

        // Act: Create block with recipient encryption and keywords
        Block encryptedBlock = api.createBlockWithOptions(testContent, options);

        // Assert: Verify block was created with all metadata
        assertNotNull(encryptedBlock, "Encrypted block should be created");
        assertTrue(encryptedBlock.getIsEncrypted(), "Block should be marked as encrypted");
        assertEquals("PROJECT", encryptedBlock.getContentCategory(), "Category should be set");
        assertNotNull(encryptedBlock.getManualKeywords(), "Manual keywords should be set");
        assertEquals("project confidential data", encryptedBlock.getManualKeywords(), 
            "Keywords should be joined correctly");
    }

    @Test
    void testEncryptionRequiresRecipientOrPassword() throws Exception {
        // Arrange
        String testContent = "This encryption should fail";

        UserFriendlyEncryptionAPI.BlockCreationOptions options = 
            new UserFriendlyEncryptionAPI.BlockCreationOptions()
                .withEncryption(true); // No recipient or password

        // Act & Assert: Should throw exception
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            api.createBlockWithOptions(testContent, options);
        });

        assertTrue(exception.getMessage().contains("Encryption requested but neither recipient username nor password provided"), 
            "Exception should mention missing encryption parameters");
    }

    @Test
    void testPasswordEncryptionStillWorks() throws Exception {
        // Arrange
        String testContent = "Password encrypted content";
        String password = "test-password-123";

        UserFriendlyEncryptionAPI.BlockCreationOptions options = 
            new UserFriendlyEncryptionAPI.BlockCreationOptions()
                .withEncryption(true)
                .withPassword(password)
                .withIdentifier("test-identifier");

        // Act: Create block with password encryption
        Block encryptedBlock = api.createBlockWithOptions(testContent, options);

        // Assert: Verify password encryption still works
        assertNotNull(encryptedBlock, "Password-encrypted block should be created");
        assertTrue(encryptedBlock.getIsEncrypted(), "Block should be marked as encrypted");
        assertNotNull(encryptedBlock.getData(), "Block should have encrypted data");
        // Password encrypted blocks don't have the recipient prefix
        assertFalse(encryptedBlock.getData().startsWith("RECIPIENT_ENCRYPTED:"), 
            "Password-encrypted blocks should not have recipient prefix");
    }

    @Test
    void testDecryptRecipientBlock_Success() throws Exception {
        // Arrange: Create recipient user and save keypair
        KeyPair recipientKeyPair = api.createUser(recipientUsername);
        assertNotNull(recipientKeyPair, "Recipient user creation should succeed");

        String originalContent = "This is a secret message for decryption test";

        UserFriendlyEncryptionAPI.BlockCreationOptions options = 
            new UserFriendlyEncryptionAPI.BlockCreationOptions()
                .withEncryption(true)
                .withRecipient(recipientUsername);

        // Act: Create recipient-encrypted block
        Block encryptedBlock = api.createBlockWithOptions(originalContent, options);
        assertNotNull(encryptedBlock, "Encrypted block should be created");

        // Act: Decrypt the block using recipient's private key
        String decryptedContent = api.decryptRecipientBlock(encryptedBlock, recipientKeyPair.getPrivate());

        // Assert: Verify decryption succeeded
        assertNotNull(decryptedContent, "Decrypted content should not be null");
        assertEquals(originalContent, decryptedContent, "Decrypted content should match original");
    }

    @Test
    void testDecryptRecipientBlock_WrongPrivateKey() throws Exception {
        // Arrange: Create recipient and another user
        KeyPair recipientKeyPair = api.createUser(recipientUsername);
        KeyPair wrongKeyPair = api.createUser("wrong-user");
        assertNotNull(recipientKeyPair, "Recipient user creation should succeed");
        assertNotNull(wrongKeyPair, "Wrong user creation should succeed");
        
        String originalContent = "Secret content";

        UserFriendlyEncryptionAPI.BlockCreationOptions options = 
            new UserFriendlyEncryptionAPI.BlockCreationOptions()
                .withEncryption(true)
                .withRecipient(recipientUsername);

        // Act: Create recipient-encrypted block
        Block encryptedBlock = api.createBlockWithOptions(originalContent, options);

        // Act & Assert: Should fail with wrong private key
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            api.decryptRecipientBlock(encryptedBlock, wrongKeyPair.getPrivate());
        });

        assertTrue(exception.getMessage().contains("Failed to decrypt recipient block"), 
            "Exception should indicate decryption failure");
    }

    @Test
    void testIsRecipientEncrypted() throws Exception {
        // Arrange: Create regular block
        String regularContent = "Regular content";
        Block regularBlock = api.createBlockWithOptions(regularContent, 
            new UserFriendlyEncryptionAPI.BlockCreationOptions());

        // Create password-encrypted block
        Block passwordBlock = api.createBlockWithOptions("Password content", 
            new UserFriendlyEncryptionAPI.BlockCreationOptions()
                .withEncryption(true)
                .withPassword("test-password"));

        // Create recipient user and recipient-encrypted block
        KeyPair recipientKeyPair = api.createUser(recipientUsername);
        assertNotNull(recipientKeyPair, "Recipient user creation should succeed");
        
        Block recipientBlock = api.createBlockWithOptions("Recipient content", 
            new UserFriendlyEncryptionAPI.BlockCreationOptions()
                .withEncryption(true)
                .withRecipient(recipientUsername));

        // Assert: Check isRecipientEncrypted() method
        assertFalse(api.isRecipientEncrypted(regularBlock), 
            "Regular block should not be recipient-encrypted");
        assertFalse(api.isRecipientEncrypted(passwordBlock), 
            "Password-encrypted block should not be recipient-encrypted");
        assertTrue(api.isRecipientEncrypted(recipientBlock), 
            "Recipient block should be identified as recipient-encrypted");
        assertFalse(api.isRecipientEncrypted(null), 
            "Null block should return false");
    }

    @Test
    void testGetRecipientUsername() throws Exception {
        // Arrange: Create recipient user
        KeyPair recipientKeyPair = api.createUser(recipientUsername);
        assertNotNull(recipientKeyPair, "Recipient user creation should succeed");

        // Create recipient-encrypted block
        Block recipientBlock = api.createBlockWithOptions("Test content", 
            new UserFriendlyEncryptionAPI.BlockCreationOptions()
                .withEncryption(true)
                .withRecipient(recipientUsername));

        // Create regular block
        Block regularBlock = api.createBlockWithOptions("Regular content", 
            new UserFriendlyEncryptionAPI.BlockCreationOptions());

        // Assert: Check getRecipientUsername() method
        assertEquals(recipientUsername, api.getRecipientUsername(recipientBlock), 
            "Should return correct recipient username");
        assertNull(api.getRecipientUsername(regularBlock), 
            "Should return null for non-recipient-encrypted block");
        assertNull(api.getRecipientUsername(null), 
            "Should return null for null block");
    }
}