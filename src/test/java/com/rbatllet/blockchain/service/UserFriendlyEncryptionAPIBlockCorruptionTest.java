package com.rbatllet.blockchain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.KeyPair;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.security.UserRole;
import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.util.TestGenesisKeyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Critical vulnerability demonstration test for UserFriendlyEncryptionAPI.
 * This test exposes how the repairBlockLink method can corrupt the entire blockchain
 * by modifying block hashes without updating subsequent blocks in the chain.
 *
 * WARNING: This demonstrates a CRITICAL security vulnerability that must be fixed
 * before production deployment.
 */
@DisplayName("🚨 CRITICAL: Blockchain Corruption Vulnerability Tests")
class UserFriendlyEncryptionAPIBlockCorruptionTest {
    private static final Logger logger = LoggerFactory.getLogger(UserFriendlyEncryptionAPIBlockCorruptionTest.class);


    private UserFriendlyEncryptionAPI api;
    private Blockchain blockchain;
    private KeyPair testKeyPair;
    private KeyPair bootstrapKeyPair;
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_PASSWORD = "SecurePassword123!";

    @BeforeEach
    void setUp() {
        blockchain = new Blockchain();

        // RBAC FIX (v1.0.6): Clear database before bootstrap to avoid "Existing users" error
        blockchain.clearAndReinitialize();

        // Load bootstrap admin keys (auto-generates if missing - test-only)
        bootstrapKeyPair = TestGenesisKeyManager.ensureGenesisKeysExist();

        // SECURITY (v1.0.6): Register bootstrap admin in blockchain (REQUIRED!)
        blockchain.createBootstrapAdmin(
            CryptoUtil.publicKeyToString(bootstrapKeyPair.getPublic()),
            "BOOTSTRAP_ADMIN"
        );

        testKeyPair = CryptoUtil.generateKeyPair();

        // SECURITY FIX (v1.0.6): Pre-authorize user before creating API
        String publicKeyString = CryptoUtil.publicKeyToString(testKeyPair.getPublic());
        blockchain.addAuthorizedKey(publicKeyString, TEST_USERNAME, bootstrapKeyPair, UserRole.USER);

        api = new UserFriendlyEncryptionAPI(
            blockchain,
            TEST_USERNAME,
            testKeyPair
        );
    }

    @AfterEach
    void tearDown() {
        if (blockchain != null) {
            blockchain.shutdown();
        }
    }

    @Test
    @DisplayName(
        "✅ SECURITY FIX: repairBlockLink properly rejects operations that would corrupt blockchain"
    )
    void testRepairBlockLinkCorruptsBlockchain() throws Exception {
        // Step 1: Create a valid blockchain with 3 blocks
        Block block1 = api.storeSecret("First block data", TEST_PASSWORD);
        Block block2 = api.storeSecret("Second block data", TEST_PASSWORD);
        Block block3 = api.storeSecret("Third block data", TEST_PASSWORD);

        assertNotNull(block1, "First block should be created");
        assertNotNull(block2, "Second block should be created");
        assertNotNull(block3, "Third block should be created");

        // Verify initial blockchain integrity
        List<Block> initialChain = blockchain.getValidChain();
        assertTrue(initialChain.size() >= 3, "Should have at least 3 blocks");

        // Verify the chain is initially valid
        assertTrue(
            blockchain.validateChainDetailed().isValid(),
            "Initial blockchain should be valid"
        );

        // Step 2: Get the blocks and verify their connections
        Block currentBlock = findBlockInChain(initialChain, block2.getHash());
        Block previousBlock = findBlockInChain(initialChain, block1.getHash());

        assertNotNull(currentBlock, "Current block should be found in chain");
        assertNotNull(previousBlock, "Previous block should be found in chain");

        // Store original hashes for comparison
        String originalCurrentHash = currentBlock.getHash();
        String originalPreviousHash = currentBlock.getPreviousHash();

        // Verify blocks are properly linked initially
        assertEquals(
            previousBlock.getHash(),
            currentBlock.getPreviousHash(),
            "Blocks should be properly linked initially"
        );

        // Step 3: Deliberately break the link to test repair
        currentBlock.setPreviousHash("deliberately_broken_link");

        // Step 4: Use reflection to call the private repairBlockLink method
        var repairMethod = UserFriendlyEncryptionAPI.class.getDeclaredMethod(
            "repairBlockLink",
            Block.class,
            Block.class
        );
        repairMethod.setAccessible(true);

        // Step 5: Call repairBlockLink - THIS IS WHERE THE VULNERABILITY OCCURS
        boolean repairResult = (boolean) repairMethod.invoke(
            api,
            currentBlock,
            previousBlock
        );
        assertFalse(
            repairResult,
            "Repair should properly reject operations that would corrupt the chain"
        );

        // Step 6: VERIFY SECURITY FIX - No corruption should occur

        // Since repair was properly rejected, the current block hash should remain unchanged
        String newCurrentHash = currentBlock.getHash();
        assertEquals(
            originalCurrentHash,
            newCurrentHash,
            "✅ SECURITY FIX: Current block hash should remain unchanged when repair is rejected"
        );

        // Verify the subsequent block still exists and is unchanged
        Block subsequentBlock = findBlockInChain(
            blockchain.getValidChain(),
            block3.getHash()
        );
        assertNotNull(subsequentBlock, "Subsequent block should exist");

        // The subsequent block should still point to the original hash (unchanged)
        assertEquals(
            originalCurrentHash,
            subsequentBlock.getPreviousHash(),
            "✅ Subsequent block should still point to original hash"
        );

        // The current block hash should match what the subsequent block expects
        assertEquals(
            subsequentBlock.getPreviousHash(),
            currentBlock.getHash(),
            "✅ SECURITY FIX: Chain integrity is maintained - hashes match"
        );

        // Verify the previous hash link was not changed since repair was rejected
        assertEquals(
            "deliberately_broken_link",
            currentBlock.getPreviousHash(),
            "Previous hash should remain as deliberately broken since repair was rejected"
        );

        // Step 7: Verify the blockchain integrity is maintained (since repair was rejected)
        assertTrue(
            blockchain.validateChainDetailed().isValid(),
            "✅ Chain should remain valid after repair was properly rejected"
        );

        // Step 8: Demonstrate the scope of corruption
        logger.info("🚨 BLOCKCHAIN CORRUPTION DEMONSTRATED:");
        logger.info("Original block2 hash: " + originalCurrentHash);
        logger.info("New block2 hash after repair: " + newCurrentHash);
        logger.info("Original previous hash: " + originalPreviousHash);
        logger.info(
            "Current previous hash: " + currentBlock.getPreviousHash()
        );
        logger.info(
            "Block3 still points to: " + subsequentBlock.getPreviousHash()
        );
        logger.info("Chain is now BROKEN!");
    }

    @Test
    @DisplayName(
        "✅ SECURITY FIX: repairSingleBlock properly rejects operations that would create invalid state"
    )
    void testRepairSingleBlockCreatesInvalidState() throws Exception {
        // Create a block
        Block testBlock = api.storeSecret(
            "Test data for corruption",
            TEST_PASSWORD
        );
        assertNotNull(testBlock, "Test block should be created");

        Long blockNumber = testBlock.getBlockNumber();
        String originalHash = testBlock.getHash();

        // Deliberately corrupt the block hash to trigger repair
        testBlock.setHash("corrupted_hash_value");

        // Use reflection to call the private repairSingleBlock method
        var repairMethod = UserFriendlyEncryptionAPI.class.getDeclaredMethod(
            "repairSingleBlock",
            Long.class
        );
        repairMethod.setAccessible(true);

        // Call repairSingleBlock
        boolean repairResult = (boolean) repairMethod.invoke(api, blockNumber);
        assertFalse(
            repairResult,
            "Repair should properly reject operations that would create invalid state"
        );

        // Since repair was properly rejected, the hash should remain the corrupted value
        String newHash = testBlock.getHash();
        assertEquals(
            "corrupted_hash_value",
            newHash,
            "Hash should remain corrupted since repair was properly rejected"
        );
        // The original hash should remain unchanged since repair failed
        assertEquals(
            "corrupted_hash_value",
            newHash,
            "Hash should remain as corrupted value since safe repair was rejected"
        );

        // VULNERABILITY: The new hash may not match the block's actual content
        // This could create subtle data integrity issues
        logger.info("🚨 BLOCK REPAIR INTEGRITY ISSUE:");
        logger.info("Original hash: " + originalHash);
        logger.info("Repaired hash: " + newHash);
        logger.info(
            "Hash regeneration may not match actual block content!"
        );
    }

    @Test
    @DisplayName("🔍 Test corruption detection performance")
    void testCorruptionDetectionPerformance() throws Exception {
        // Create multiple blocks to test performance
        for (int i = 0; i < 100; i++) {
            api.storeSecret("Performance test block " + i, TEST_PASSWORD);
        }

        // Measure time for corruption detection
        long startTime = System.currentTimeMillis();

        var method = UserFriendlyEncryptionAPI.class.getDeclaredMethod(
            "identifyCorruptedBlocks"
        );
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Long> corruptedBlocks = (List<Long>) method.invoke(api);

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Verify corruption detection works properly
        assertNotNull(corruptedBlocks, "Should return non-null list");
        // Note: With improved security, corruption detection may identify issues
        // that were previously missed. This is expected behavior for robust validation.

        // Performance warning if detection takes too long
        if (duration > 1000) {
            // More than 1 second for 100 blocks
            logger.info(
                "⚠️ PERFORMANCE WARNING: Corruption detection took " +
                duration +
                "ms for 100 blocks"
            );
            logger.info("This could timeout on large blockchains!");
        }
    }

    @Test
    @DisplayName("🛡️ Verify validation methods miss subtle corruption")
    void testValidationMissesSubtleCorruption() throws Exception {
        // Create a block
        Block testBlock = api.storeSecret(
            "Block to subtly corrupt",
            TEST_PASSWORD
        );
        assertNotNull(testBlock, "Test block should be created");

        // SECURITY FIX: After encryption security fix, encrypted blocks have data="[ENCRYPTED]"
        // and the actual content is in encryptionMetadata. The hash is calculated over
        // encryptionMetadata for encrypted blocks, not over the data field.
        // Therefore, we must corrupt the field that's actually used in hash calculation.

        String originalField;
        String corruptedField;

        if (testBlock.getIsEncrypted() != null && testBlock.getIsEncrypted() &&
            testBlock.getEncryptionMetadata() != null) {
            // For encrypted blocks: corrupt encryptionMetadata (which is used in hash)
            originalField = testBlock.getEncryptionMetadata();
            // Change first character to corrupt the encrypted data
            corruptedField = "X" + originalField.substring(1);
            testBlock.setEncryptionMetadata(corruptedField);
        } else {
            // For unencrypted blocks: corrupt data field (which is used in hash)
            originalField = testBlock.getData();
            corruptedField = originalField.replace('B', 'X'); // Change first 'B' to 'X'
            testBlock.setData(corruptedField);
        }

        // Test the isBlockCorrupted method
        var method = UserFriendlyEncryptionAPI.class.getDeclaredMethod(
            "isBlockCorrupted",
            Block.class
        );
        method.setAccessible(true);

        boolean isCorrupted = (boolean) method.invoke(api, testBlock);

        // The validation should now properly detect data corruption
        assertTrue(
            isCorrupted,
            "✅ SECURITY FIX: Validation now properly detects data corruption!"
        );

        logger.info("✅ CORRUPTION PROPERLY DETECTED:");
        String fieldType = (testBlock.getIsEncrypted() != null && testBlock.getIsEncrypted())
            ? "encryptionMetadata" : "data";
        logger.info("Corrupted field: " + fieldType);
        logger.info("Original value: " + originalField.substring(0, Math.min(50, originalField.length())) + "...");
        logger.info("Corrupted value: " + corruptedField.substring(0, Math.min(50, corruptedField.length())) + "...");
        logger.info(
            "Validation result: " + (isCorrupted ? "DETECTED" : "MISSED")
        );
    }

    /**
     * Helper method to find a block by hash in the chain
     */
    private Block findBlockInChain(List<Block> chain, String hash) {
        return chain
            .stream()
            .filter(block -> hash.equals(block.getHash()))
            .findFirst()
            .orElse(null);
    }
}
