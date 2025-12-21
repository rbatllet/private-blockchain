package com.rbatllet.blockchain.service;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.security.UserRole;
import com.rbatllet.blockchain.util.CryptoUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for createBlockWithExistingUser() method.
 * This method allows creating blocks when the user is already created and authorized,
 * avoiding the RBAC permission issues when a USER role tries to create another user.
 *
 * @since 1.0.6
 */
public class UserFriendlyEncryptionAPIExistingUserTest {

    private Blockchain blockchain;
    private UserFriendlyEncryptionAPI api;
    private KeyPair bootstrapAdminKeys;
    private KeyPair existingUserKeys;
    private String existingUserPublicKey;

    @BeforeEach
    void setUp() throws Exception {
        blockchain = new Blockchain();
        blockchain.clearAndReinitialize();

        // Create bootstrap admin for RBAC (using createBootstrapAdmin)
        bootstrapAdminKeys = CryptoUtil.generateKeyPair();
        blockchain.createBootstrapAdmin(
            CryptoUtil.publicKeyToString(bootstrapAdminKeys.getPublic()),
            "BOOTSTRAP_ADMIN"
        );

        // Create API and set bootstrap admin credentials
        api = new UserFriendlyEncryptionAPI(blockchain);
        api.setDefaultCredentials("BOOTSTRAP_ADMIN", bootstrapAdminKeys);

        // Create an existing user with USER role (this simulates --generate-key scenario)
        existingUserKeys = api.createUser("block_user");
        existingUserPublicKey = CryptoUtil.publicKeyToString(existingUserKeys.getPublic());

        // Explicitly authorize the existing user
        blockchain.addAuthorizedKey(
            existingUserPublicKey,
            "block_user",
            bootstrapAdminKeys,
            UserRole.USER
        );

        // Verify the user is authorized
        assertTrue(blockchain.isKeyAuthorized(existingUserPublicKey),
            "Existing user should be authorized");
    }

    @AfterEach
    void tearDown() {
        if (blockchain != null) {
            blockchain.clearAndReinitialize();
        }
    }

    /**
     * TEST 1: Create regular block with existing user
     */
    @Test
    void testCreateRegularBlockWithExistingUser() {
        UserFriendlyEncryptionAPI.BlockCreationOptions options =
            new UserFriendlyEncryptionAPI.BlockCreationOptions();

        Block block = api.createBlockWithExistingUser(
            "Regular block content",
            existingUserKeys,
            options
        );

        assertNotNull(block, "Block should be created");
        assertEquals(2, blockchain.getBlockCount(), "Should have genesis + 1 block");
        assertTrue(block.getData().contains("Regular block content"),
            "Block should contain the content");
        assertEquals(existingUserPublicKey, block.getSignerPublicKey(),
            "Block should be signed by existing user");
    }

    /**
     * TEST 2: Create block with keywords and category using existing user
     */
    @Test
    void testCreateBlockWithKeywordsAndCategory() {
        UserFriendlyEncryptionAPI.BlockCreationOptions options =
            new UserFriendlyEncryptionAPI.BlockCreationOptions()
                .withKeywords(new String[]{"keyword1", "keyword2"})
                .withCategory("TECHNICAL");

        Block block = api.createBlockWithExistingUser(
            "Block with keywords",
            existingUserKeys,
            options
        );

        assertNotNull(block, "Block should be created");
        assertNotNull(block.getManualKeywords(), "Block should have keywords");
        assertTrue(block.getManualKeywords().contains("keyword1"),
            "Should contain keyword1");
        assertTrue(block.getManualKeywords().contains("keyword2"),
            "Should contain keyword2");
        assertEquals("TECHNICAL", block.getContentCategory(),
            "Should have category");
    }

    /**
     * TEST 3: Create block with custom metadata using existing user
     */
    @Test
    void testCreateBlockWithCustomMetadata() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("department", "Engineering");
        metadata.put("project", "Blockchain");

        UserFriendlyEncryptionAPI.BlockCreationOptions options =
            new UserFriendlyEncryptionAPI.BlockCreationOptions()
                .withMetadata("department", "Engineering")
                .withMetadata("project", "Blockchain");

        Block block = api.createBlockWithExistingUser(
            "Block with metadata",
            existingUserKeys,
            options
        );

        assertNotNull(block, "Block should be created");
        assertNotNull(block.getCustomMetadata(), "Block should have custom metadata");
        assertTrue(block.getCustomMetadata().contains("department"),
            "Should contain department metadata");
        assertTrue(block.getCustomMetadata().contains("Engineering"),
            "Should contain Engineering value");
    }

    /**
     * TEST 4: Create password-encrypted block with existing user
     * NOTE: Password encryption with existing user creates an identifier-based secret
     */
    @Test
    void testCreatePasswordEncryptedBlockWithExistingUser() {
        String password = "secure-password-123";
        String identifier = "test-secret-id";

        UserFriendlyEncryptionAPI.BlockCreationOptions options =
            new UserFriendlyEncryptionAPI.BlockCreationOptions()
                .withPassword(password)
                .withIdentifier(identifier)
                .withEncryption(true);

        Block block = api.createBlockWithExistingUser(
            "Secret content",
            existingUserKeys,
            options
        );

        assertNotNull(block, "Block should be created");
        assertTrue(block.getIsEncrypted(), "Block should be encrypted");

        // Verify we can decrypt it with password
        String decrypted = api.retrieveSecret(block.getBlockNumber(), password);
        assertEquals("Secret content", decrypted,
            "Decrypted content should match original");
    }

    /**
     * TEST 5: Create recipient-encrypted block with existing user
     */
    @Test
    void testCreateRecipientEncryptedBlockWithExistingUser() {
        // Create recipient user
        api.setDefaultCredentials("BOOTSTRAP_ADMIN", bootstrapAdminKeys);
        KeyPair recipientKeys = api.createUser("recipient_user");
        String recipientPublicKey = CryptoUtil.publicKeyToString(recipientKeys.getPublic());
        blockchain.addAuthorizedKey(
            recipientPublicKey,
            "recipient_user",
            bootstrapAdminKeys,
            UserRole.USER
        );

        // Create block with recipient encryption
        UserFriendlyEncryptionAPI.BlockCreationOptions options =
            new UserFriendlyEncryptionAPI.BlockCreationOptions()
                .withRecipient("recipient_user")
                .withEncryption(true);

        Block block = api.createBlockWithExistingUser(
            "Message for recipient",
            existingUserKeys,
            options
        );

        assertNotNull(block, "Block should be created");
        assertTrue(block.getIsEncrypted(), "Block should be encrypted");

        // Verify recipient information
        String recipient = api.getRecipientUsername(block);
        assertNotNull(recipient, "Block should have recipient");
        assertEquals("recipient_user", recipient,
            "Recipient should be recipient_user");

        // Verify sender is the existing user
        assertEquals(existingUserPublicKey, block.getSignerPublicKey(),
            "Block should be signed by existing user");
    }

    /**
     * TEST 6: Create block with all options combined
     */
    @Test
    void testCreateBlockWithAllOptionsCombined() {
        // Create recipient
        api.setDefaultCredentials("BOOTSTRAP_ADMIN", bootstrapAdminKeys);
        KeyPair recipientKeys = api.createUser("alice");
        blockchain.addAuthorizedKey(
            CryptoUtil.publicKeyToString(recipientKeys.getPublic()),
            "alice",
            bootstrapAdminKeys,
            UserRole.USER
        );

        UserFriendlyEncryptionAPI.BlockCreationOptions options =
            new UserFriendlyEncryptionAPI.BlockCreationOptions()
                .withRecipient("alice")
                .withEncryption(true)
                .withKeywords(new String[]{"confidential", "report"})
                .withCategory("FINANCIAL")
                .withMetadata("department", "Finance")
                .withMetadata("quarter", "Q4");

        Block block = api.createBlockWithExistingUser(
            "Quarterly financial report",
            existingUserKeys,
            options
        );

        assertNotNull(block, "Block should be created");
        assertTrue(block.getIsEncrypted(), "Block should be encrypted");

        String recipient = api.getRecipientUsername(block);
        assertEquals("alice", recipient, "Should have recipient");
        assertEquals("FINANCIAL", block.getContentCategory(), "Should have category");
        assertNotNull(block.getManualKeywords(), "Should have keywords");
        assertTrue(block.getManualKeywords().contains("confidential"),
            "Should contain keyword");
        assertNotNull(block.getCustomMetadata(), "Should have metadata");
        assertTrue(block.getCustomMetadata().contains("Finance"),
            "Should contain metadata value");
    }

    /**
     * TEST 7: Validation - null content should fail
     */
    @Test
    void testCreateBlockWithNullContentShouldFail() {
        UserFriendlyEncryptionAPI.BlockCreationOptions options =
            new UserFriendlyEncryptionAPI.BlockCreationOptions();

        assertThrows(IllegalArgumentException.class, () -> {
            api.createBlockWithExistingUser(null, existingUserKeys, options);
        }, "Should throw IllegalArgumentException for null content");
    }

    /**
     * TEST 8: Validation - empty content should fail
     */
    @Test
    void testCreateBlockWithEmptyContentShouldFail() {
        UserFriendlyEncryptionAPI.BlockCreationOptions options =
            new UserFriendlyEncryptionAPI.BlockCreationOptions();

        assertThrows(IllegalArgumentException.class, () -> {
            api.createBlockWithExistingUser("  ", existingUserKeys, options);
        }, "Should throw IllegalArgumentException for empty content");
    }

    /**
     * TEST 9: Validation - null KeyPair should fail
     */
    @Test
    void testCreateBlockWithNullKeyPairShouldFail() {
        UserFriendlyEncryptionAPI.BlockCreationOptions options =
            new UserFriendlyEncryptionAPI.BlockCreationOptions();

        assertThrows(IllegalArgumentException.class, () -> {
            api.createBlockWithExistingUser("Content", null, options);
        }, "Should throw IllegalArgumentException for null KeyPair");
    }

    /**
     * TEST 10: Verify method works identically to createBlockWithOptions()
     * but without creating a new user (avoiding RBAC permission issues)
     */
    @Test
    void testEquivalenceWithCreateBlockWithOptions() {
        // Set credentials as bootstrap admin to create blocks
        api.setDefaultCredentials("BOOTSTRAP_ADMIN", bootstrapAdminKeys);

        // Create block using createBlockWithOptions (creates new user automatically)
        UserFriendlyEncryptionAPI.BlockCreationOptions options1 =
            new UserFriendlyEncryptionAPI.BlockCreationOptions()
                .withUsername("auto_user")
                .withKeywords(new String[]{"test"})
                .withCategory("TEST");

        Block block1 = api.createBlockWithOptions("Test content 1", options1);

        // Create block using createBlockWithExistingUser (uses existing user)
        UserFriendlyEncryptionAPI.BlockCreationOptions options2 =
            new UserFriendlyEncryptionAPI.BlockCreationOptions()
                .withKeywords(new String[]{"test"})
                .withCategory("TEST");

        Block block2 = api.createBlockWithExistingUser(
            "Test content 2",
            existingUserKeys,
            options2
        );

        // Both blocks should have similar properties
        assertNotNull(block1, "Block 1 should be created");
        assertNotNull(block2, "Block 2 should be created");
        assertEquals(block1.getContentCategory(), block2.getContentCategory(),
            "Both should have same category");
        assertTrue(block1.getManualKeywords().contains("test"),
            "Block 1 should have keywords");
        assertTrue(block2.getManualKeywords().contains("test"),
            "Block 2 should have keywords");

        // Block 1 signed by auto_user, Block 2 signed by existing user
        assertNotEquals(block1.getSignerPublicKey(), block2.getSignerPublicKey(),
            "Different users should sign the blocks");
        assertEquals(existingUserPublicKey, block2.getSignerPublicKey(),
            "Block 2 should be signed by existing user");
    }

    /**
     * TEST 11: Verify that using an existing user simplifies the workflow
     * (no need to authorize again, avoiding RBAC permission issues)
     */
    @Test
    void testCreateBlockWithExistingUserAvoidsRBACIssues() {
        // Simulate scenario where a USER role user wants to create blocks
        // If they tried to use createBlockWithOptions(), it would fail because
        // USER role cannot create other users (RBAC permission denied)

        // But with createBlockWithExistingUser(), we use already-authorized keys
        // so no user creation is attempted, avoiding the RBAC permission issue

        UserFriendlyEncryptionAPI.BlockCreationOptions options =
            new UserFriendlyEncryptionAPI.BlockCreationOptions()
                .withKeywords(new String[]{"test"})
                .withCategory("TEST");

        // This succeeds because we're using existing authorized keys
        Block block = api.createBlockWithExistingUser(
            "Block created by existing user",
            existingUserKeys,
            options
        );

        assertNotNull(block, "Block should be created successfully");
        assertEquals(existingUserPublicKey, block.getSignerPublicKey(),
            "Block should be signed by existing user");
        assertEquals("TEST", block.getContentCategory(),
            "Block should have category");
    }
}
