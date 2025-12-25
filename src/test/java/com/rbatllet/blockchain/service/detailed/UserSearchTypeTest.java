package com.rbatllet.blockchain.service.detailed;

import static org.junit.jupiter.api.Assertions.*;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.security.UserRole;
import com.rbatllet.blockchain.testutil.GenesisKeyManager;
import com.rbatllet.blockchain.service.UserFriendlyEncryptionAPI;
import com.rbatllet.blockchain.service.UserFriendlyEncryptionAPI.UserSearchType;
import com.rbatllet.blockchain.util.CryptoUtil;
import java.security.KeyPair;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Comprehensive tests for UserSearchType enum and findBlocksByUser method
 * focusing on edge cases, null handling, and robustness
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserSearchTypeTest {

    private static final Logger logger = LoggerFactory.getLogger(
        UserSearchTypeTest.class
    );

    private Blockchain blockchain;
    private UserFriendlyEncryptionAPI api;
    private KeyPair testUserKeys;
    private KeyPair otherUserKeys;
    private KeyPair bootstrapKeyPair;
    private String testPassword;

    @BeforeEach
    void setUp() throws Exception {
        // Clean database and initialize
        blockchain = new Blockchain();
        blockchain.clearAndReinitialize(); // CRITICAL: Clear all data from previous tests
        // BlockRepository now package-private - use clearAndReinitialize();
        blockchain.getAuthorizedKeyDAO().cleanupTestData();

        // Load bootstrap admin keys (auto-generates if missing - test-only)
        bootstrapKeyPair = GenesisKeyManager.ensureGenesisKeysExist();

        // Register bootstrap admin in blockchain (RBAC v1.0.6)
        blockchain.createBootstrapAdmin(
            CryptoUtil.publicKeyToString(bootstrapKeyPair.getPublic()),
            "BOOTSTRAP_ADMIN"
        );

        api = new UserFriendlyEncryptionAPI(blockchain);
        testPassword = "TestPassword123!";

        // PRE-AUTHORIZATION REQUIRED (v1.0.6 RBAC security):
        // 1. Create admin to act as authorized user creator
        KeyPair adminKeys = CryptoUtil.generateKeyPair();
        String adminPublicKey = CryptoUtil.publicKeyToString(adminKeys.getPublic());
        blockchain.addAuthorizedKey(adminPublicKey, "Admin", bootstrapKeyPair, UserRole.ADMIN);
        api.setDefaultCredentials("Admin", adminKeys);  // Authenticate as admin

        // 2. Now admin can create test users (generates new keys internally)
        testUserKeys = api.createUser("test-user");
        otherUserKeys = api.createUser("other-user");

        // Set default credentials to test-user
        api.setDefaultCredentials("test-user", testUserKeys);

        logger.info("Test setup completed");
    }

    /**
     * Helper method to create an encrypted block
     */
    private Block createEncryptedBlock(String content, String password)
        throws Exception {
        return blockchain.addEncryptedBlock(
            content,
            password,
            testUserKeys.getPrivate(),
            testUserKeys.getPublic()
        );
    }

    /**
     * Helper method to create a public block
     */
    private Block createPublicBlock(String content) throws Exception {
        return blockchain.addBlockAndReturn(
            content,
            testUserKeys.getPrivate(),
            testUserKeys.getPublic()
        );
    }

    @Test
    @Order(1)
    @DisplayName("Test UserSearchType enum values")
    void testUserSearchTypeEnumValues() {
        // Test all enum values exist
        UserSearchType[] types = UserSearchType.values();
        assertEquals(
            3,
            types.length,
            "Should have exactly 3 UserSearchType values"
        );

        // Test specific values
        assertTrue(
            java.util.Arrays.asList(types).contains(UserSearchType.CREATED_BY)
        );
        assertTrue(
            java.util.Arrays.asList(types).contains(UserSearchType.ACCESSIBLE)
        );
        assertTrue(
            java.util.Arrays.asList(types).contains(
                UserSearchType.ENCRYPTED_FOR
            )
        );

        // Test valueOf
        assertEquals(
            UserSearchType.CREATED_BY,
            UserSearchType.valueOf("CREATED_BY")
        );
        assertEquals(
            UserSearchType.ACCESSIBLE,
            UserSearchType.valueOf("ACCESSIBLE")
        );
        assertEquals(
            UserSearchType.ENCRYPTED_FOR,
            UserSearchType.valueOf("ENCRYPTED_FOR")
        );

        logger.info("UserSearchType enum validation passed");
    }

    @Test
    @Order(2)
    @DisplayName("Test findBlocksByUser with null username")
    void testFindBlocksByUserWithNullUsername() {
        // Test null username
        List<Block> result = api.findBlocksByUser(
            null,
            UserSearchType.CREATED_BY
        );
        assertNotNull(result, "Result should not be null");
        assertTrue(
            result.isEmpty(),
            "Result should be empty for null username"
        );

        logger.info("Null username handling test passed");
    }

    @Test
    @Order(3)
    @DisplayName("Test findBlocksByUser with empty username")
    void testFindBlocksByUserWithEmptyUsername() {
        // Test empty username
        List<Block> result1 = api.findBlocksByUser(
            "",
            UserSearchType.CREATED_BY
        );
        assertNotNull(result1, "Result should not be null");
        assertTrue(
            result1.isEmpty(),
            "Result should be empty for empty username"
        );

        // Test whitespace-only username
        List<Block> result2 = api.findBlocksByUser(
            "   ",
            UserSearchType.CREATED_BY
        );
        assertNotNull(result2, "Result should not be null");
        assertTrue(
            result2.isEmpty(),
            "Result should be empty for whitespace username"
        );

        logger.info("Empty username handling test passed");
    }

    @Test
    @Order(4)
    @DisplayName("Test findBlocksByUser with null searchType")
    void testFindBlocksByUserWithNullSearchType() {
        // This should not throw an exception but should be handled gracefully
        assertDoesNotThrow(
            () -> {
                List<Block> result = api.findBlocksByUser("test-user", null);
                assertNotNull(
                    result,
                    "Result should not be null even with null searchType"
                );
            },
            "Should handle null searchType gracefully"
        );

        logger.info("Null searchType handling test passed");
    }

    @Test
    @Order(5)
    @DisplayName("Test findBlocksByUser CREATED_BY with valid user")
    void testFindBlocksByUserCreatedByValid() throws Exception {
        // Create a block with test user
        Block block = createEncryptedBlock("Test content", testPassword);
        assertNotNull(block, "Block should be created successfully");

        // Search for blocks created by test-user
        List<Block> result = api.findBlocksByUser(
            "test-user",
            UserSearchType.CREATED_BY
        );
        assertNotNull(result, "Result should not be null");
        assertFalse(
            result.isEmpty(),
            "Should find blocks created by test-user"
        );

        // Verify the block is in results
        boolean foundBlock = result
            .stream()
            .anyMatch(b -> b.getBlockNumber().equals(block.getBlockNumber()));
        assertTrue(foundBlock, "Should find the created block");

        logger.info("CREATED_BY valid user test passed");
    }

    @Test
    @Order(6)
    @DisplayName("Test findBlocksByUser CREATED_BY with non-existent user")
    void testFindBlocksByUserCreatedByNonExistent() throws Exception {
        // Create a block first
        createEncryptedBlock("Test content", testPassword);

        // Search for blocks created by non-existent user
        List<Block> result = api.findBlocksByUser(
            "non-existent-user",
            UserSearchType.CREATED_BY
        );
        assertNotNull(result, "Result should not be null");
        assertTrue(
            result.isEmpty(),
            "Should not find blocks for non-existent user"
        );

        logger.info("CREATED_BY non-existent user test passed");
    }

    @Test
    @Order(7)
    @DisplayName("Test findBlocksByUser ACCESSIBLE with encrypted blocks")
    void testFindBlocksByUserAccessibleEncrypted() throws Exception {
        // Create encrypted block
        Block encryptedBlock = createEncryptedBlock(
            "Encrypted content",
            testPassword
        );
        assertNotNull(encryptedBlock, "Encrypted block should be created");

        // Search for accessible blocks
        List<Block> result = api.findBlocksByUser(
            "test-user",
            UserSearchType.ACCESSIBLE
        );
        assertNotNull(result, "Result should not be null");

        logger.info(
            "ACCESSIBLE encrypted blocks test passed - found {} blocks",
            result.size()
        );
    }

    @Test
    @Order(8)
    @DisplayName("Test findBlocksByUser ACCESSIBLE with public blocks")
    void testFindBlocksByUserAccessiblePublic() throws Exception {
        // Create public block
        Block publicBlock = createPublicBlock("Public content");
        assertNotNull(publicBlock, "Public block should be created");

        // Search for accessible blocks - public blocks should be accessible to all
        List<Block> result = api.findBlocksByUser(
            "any-user",
            UserSearchType.ACCESSIBLE
        );
        assertNotNull(result, "Result should not be null");

        // Public blocks should be found even for non-existent users
        boolean foundPublicBlock = result
            .stream()
            .anyMatch(b ->
                b.getBlockNumber().equals(publicBlock.getBlockNumber())
            );
        assertTrue(
            foundPublicBlock,
            "Public block should be accessible to any user"
        );

        logger.info("ACCESSIBLE public blocks test passed");
    }

    @Test
    @Order(9)
    @DisplayName("Test findBlocksByUser ENCRYPTED_FOR")
    void testFindBlocksByUserEncryptedFor() throws Exception {
        // Create encrypted block
        Block encryptedBlock = createEncryptedBlock(
            "Encrypted for user",
            testPassword
        );
        assertNotNull(encryptedBlock, "Encrypted block should be created");

        // Search for blocks encrypted for user
        List<Block> result = api.findBlocksByUser(
            "test-user",
            UserSearchType.ENCRYPTED_FOR
        );
        assertNotNull(result, "Result should not be null");

        // ENCRYPTED_FOR should only return encrypted blocks, not public ones
        if (!result.isEmpty()) {
            boolean allEncrypted = result
                .stream()
                .allMatch(
                    b -> b.getIsEncrypted() != null && b.getIsEncrypted()
                );
            assertTrue(
                allEncrypted,
                "ENCRYPTED_FOR should only return encrypted blocks"
            );
        }

        logger.info(
            "ENCRYPTED_FOR test passed - found {} blocks",
            result.size()
        );
    }

    @Test
    @Order(10)
    @DisplayName("Test findBlocksByUser with empty blockchain")
    void testFindBlocksByUserEmptyBlockchain() {
        // Create new empty blockchain
        Blockchain emptyBlockchain = new Blockchain();
        UserFriendlyEncryptionAPI emptyApi = new UserFriendlyEncryptionAPI(
            emptyBlockchain
        );

        // Test all search types with empty blockchain
        for (UserSearchType searchType : UserSearchType.values()) {
            List<Block> result = emptyApi.findBlocksByUser(
                "test-user",
                searchType
            );
            assertNotNull(
                result,
                "Result should not be null for " + searchType
            );
            assertTrue(
                result.isEmpty(),
                "Result should be empty for empty blockchain with " + searchType
            );
        }

        logger.info("Empty blockchain test passed");
    }

    @Test
    @Order(11)
    @DisplayName("Test findBlocksByUser with corrupted block data")
    void testFindBlocksByUserWithCorruptedBlocks() throws Exception {
        // Create a valid block first
        Block validBlock = createPublicBlock("Valid content");
        assertNotNull(validBlock, "Valid block should be created");

        // Test search still works even if some blocks have null fields
        List<Block> result = api.findBlocksByUser(
            "test-user",
            UserSearchType.ACCESSIBLE
        );
        assertNotNull(
            result,
            "Result should not be null even with potential corrupted data"
        );

        logger.info("Corrupted block data test passed");
    }

    @Test
    @Order(12)
    @DisplayName("Test findBlocksByUser performance with multiple blocks")
    void testFindBlocksByUserPerformance() throws Exception {
        // Create multiple blocks
        int numBlocks = 10;
        for (int i = 0; i < numBlocks; i++) {
            if (i % 2 == 0) {
                createPublicBlock("Public content " + i);
            } else {
                createEncryptedBlock("Encrypted content " + i, testPassword);
            }
        }

        long startTime = System.currentTimeMillis();

        // Test all search types
        for (UserSearchType searchType : UserSearchType.values()) {
            List<Block> result = api.findBlocksByUser("test-user", searchType);
            assertNotNull(
                result,
                "Result should not be null for " + searchType
            );
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        logger.info(
            "Performance test completed in {} ms with {} blocks",
            duration,
            numBlocks
        );
        assertTrue(
            duration < 5000,
            "Search should complete in reasonable time (< 5 seconds)"
        );
    }

    @Test
    @Order(13)
    @DisplayName("Test findBlocksByUser with special characters in username")
    void testFindBlocksByUserSpecialCharacters() throws Exception {
        // Create user with special characters
        String specialUser = "user@domain.com";

        // RBAC FIX (v1.0.6): Need admin credentials to create new user
        // setUp leaves us authenticated as test-user (USER), need to switch to admin
        KeyPair adminKeys = CryptoUtil.generateKeyPair();
        String adminPublicKey = CryptoUtil.publicKeyToString(adminKeys.getPublic());
        blockchain.addAuthorizedKey(adminPublicKey, "SpecialAdmin", bootstrapKeyPair, UserRole.ADMIN);
        api.setDefaultCredentials("SpecialAdmin", adminKeys);

        KeyPair specialKeys = api.createUser(specialUser);
        api.setDefaultCredentials(specialUser, specialKeys);

        // Create block with special user
        Block block = createPublicBlock("Content from special user");
        assertNotNull(block, "Block should be created with special username");

        // Search for blocks
        List<Block> result = api.findBlocksByUser(
            specialUser,
            UserSearchType.CREATED_BY
        );
        assertNotNull(result, "Result should not be null for special username");

        logger.info("Special characters username test passed");
    }

    @Test
    @Order(14)
    @DisplayName("Test findBlocksByUser consistency across multiple calls")
    void testFindBlocksByUserConsistency() throws Exception {
        // Create test data
        createPublicBlock("Public test");
        createEncryptedBlock("Encrypted test", testPassword);

        // Call same search multiple times
        for (int i = 0; i < 5; i++) {
            List<Block> result1 = api.findBlocksByUser(
                "test-user",
                UserSearchType.ACCESSIBLE
            );
            List<Block> result2 = api.findBlocksByUser(
                "test-user",
                UserSearchType.ACCESSIBLE
            );

            assertEquals(
                result1.size(),
                result2.size(),
                "Results should be consistent across calls (iteration " +
                i +
                ")"
            );
        }

        logger.info("Consistency test passed");
    }

    @Test
    @Order(15)
    @DisplayName("Test findBlocksByUser CREATED_BY with multiple users")
    void testFindBlocksByUserCreatedByMultipleUsers() throws Exception {
        // Create blocks with test-user
        Block block1 = createEncryptedBlock(
            "Content by test-user",
            testPassword
        );
        Block block2 = createPublicBlock("Public content by test-user");

        // Create blocks with other-user
        api.setDefaultCredentials("other-user", otherUserKeys);
        Block block3 = blockchain.addEncryptedBlock(
            "Content by other-user",
            testPassword,
            otherUserKeys.getPrivate(),
            otherUserKeys.getPublic()
        );
        Block block4 = blockchain.addBlockAndReturn(
            "Public content by other-user",
            otherUserKeys.getPrivate(),
            otherUserKeys.getPublic()
        );

        // Search for blocks created by test-user
        List<Block> testUserBlocks = api.findBlocksByUser(
            "test-user",
            UserSearchType.CREATED_BY
        );
        assertNotNull(testUserBlocks, "Result should not be null");
        assertEquals(
            2,
            testUserBlocks.size(),
            "Should find 2 blocks for test-user"
        );

        // Search for blocks created by other-user
        List<Block> otherUserBlocks = api.findBlocksByUser(
            "other-user",
            UserSearchType.CREATED_BY
        );
        assertNotNull(otherUserBlocks, "Result should not be null");
        assertEquals(
            2,
            otherUserBlocks.size(),
            "Should find 2 blocks for other-user"
        );

        // Verify test-user blocks are found in test-user results
        boolean hasBlock1 = testUserBlocks
            .stream()
            .anyMatch(b -> b.getBlockNumber().equals(block1.getBlockNumber()));
        assertTrue(hasBlock1, "test-user results should contain block1");

        boolean hasBlock2 = testUserBlocks
            .stream()
            .anyMatch(b -> b.getBlockNumber().equals(block2.getBlockNumber()));
        assertTrue(hasBlock2, "test-user results should contain block2");

        // Verify user separation - test-user blocks should not contain other-user blocks
        boolean hasOtherUserBlock = testUserBlocks
            .stream()
            .anyMatch(
                b ->
                    b.getBlockNumber().equals(block3.getBlockNumber()) ||
                    b.getBlockNumber().equals(block4.getBlockNumber())
            );
        assertFalse(
            hasOtherUserBlock,
            "test-user results should not contain other-user blocks"
        );

        logger.info("Multi-user CREATED_BY test passed");
    }

    @Test
    @Order(16)
    @DisplayName("Test findBlocksByUser ACCESSIBLE with user separation")
    void testFindBlocksByUserAccessibleSeparation() throws Exception {
        // Create encrypted block for test-user only
        Block testUserBlock = createEncryptedBlock(
            "Secret for test-user",
            testPassword
        );

        // Create encrypted block for other-user only
        api.setDefaultCredentials("other-user", otherUserKeys);
        Block otherUserBlock = blockchain.addEncryptedBlock(
            "Secret for other-user",
            "DifferentPassword123!",
            otherUserKeys.getPrivate(),
            otherUserKeys.getPublic()
        );

        // test-user should only access their own blocks
        api.setDefaultCredentials("test-user", testUserKeys);
        List<Block> testUserAccessible = api.findBlocksByUser(
            "test-user",
            UserSearchType.ACCESSIBLE
        );
        assertNotNull(testUserAccessible, "Result should not be null");

        // Verify test-user cannot access other-user's block
        boolean canAccessOtherBlock = testUserAccessible
            .stream()
            .anyMatch(b ->
                b.getBlockNumber().equals(otherUserBlock.getBlockNumber())
            );
        assertFalse(
            canAccessOtherBlock,
            "test-user should not access other-user's encrypted blocks"
        );

        // other-user should only access their own blocks
        api.setDefaultCredentials("other-user", otherUserKeys);
        List<Block> otherUserAccessible = api.findBlocksByUser(
            "other-user",
            UserSearchType.ACCESSIBLE
        );
        assertNotNull(otherUserAccessible, "Result should not be null");

        // Verify other-user cannot access test-user's block
        boolean canAccessTestBlock = otherUserAccessible
            .stream()
            .anyMatch(b ->
                b.getBlockNumber().equals(testUserBlock.getBlockNumber())
            );
        assertFalse(
            canAccessTestBlock,
            "other-user should not access test-user's encrypted blocks"
        );

        logger.info("User separation ACCESSIBLE test passed");
    }

    @Test
    @Order(17)
    @DisplayName("Test findBlocksByUser ENCRYPTED_FOR with multiple users")
    void testFindBlocksByUserEncryptedForMultipleUsers() throws Exception {
        // Create blocks encrypted for test-user
        Block testBlock = createEncryptedBlock(
            "Encrypted for test-user",
            testPassword
        );

        // Create block encrypted for other-user
        api.setDefaultCredentials("other-user", otherUserKeys);
        Block otherBlock = blockchain.addEncryptedBlock(
            "Encrypted for other-user",
            "OtherPassword123!",
            otherUserKeys.getPrivate(),
            otherUserKeys.getPublic()
        );

        // Search for blocks encrypted for test-user
        List<Block> testUserEncrypted = api.findBlocksByUser(
            "test-user",
            UserSearchType.ENCRYPTED_FOR
        );
        assertNotNull(testUserEncrypted, "Result should not be null");

        // Should find test-user's encrypted block
        boolean foundTestBlock = testUserEncrypted
            .stream()
            .anyMatch(b ->
                b.getBlockNumber().equals(testBlock.getBlockNumber())
            );
        assertTrue(foundTestBlock, "Should find block encrypted for test-user");

        // Should not find other-user's encrypted block
        boolean foundOtherBlock = testUserEncrypted
            .stream()
            .anyMatch(b ->
                b.getBlockNumber().equals(otherBlock.getBlockNumber())
            );
        assertFalse(
            foundOtherBlock,
            "Should not find block encrypted for other-user"
        );

        // Search for blocks encrypted for other-user
        List<Block> otherUserEncrypted = api.findBlocksByUser(
            "other-user",
            UserSearchType.ENCRYPTED_FOR
        );
        assertNotNull(otherUserEncrypted, "Result should not be null");

        // Should find other-user's encrypted block
        boolean foundOtherUserBlock = otherUserEncrypted
            .stream()
            .anyMatch(b ->
                b.getBlockNumber().equals(otherBlock.getBlockNumber())
            );
        assertTrue(
            foundOtherUserBlock,
            "Should find block encrypted for other-user"
        );

        // Should not find test-user's encrypted block
        boolean foundTestUserBlock = otherUserEncrypted
            .stream()
            .anyMatch(b ->
                b.getBlockNumber().equals(testBlock.getBlockNumber())
            );
        assertFalse(
            foundTestUserBlock,
            "Should not find block encrypted for test-user"
        );

        logger.info("Multi-user ENCRYPTED_FOR test passed");
    }
}
