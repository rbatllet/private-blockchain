package com.rbatllet.blockchain.core;

import com.rbatllet.blockchain.dao.AuthorizedKeyDAO;
import com.rbatllet.blockchain.entity.AuthorizedKey;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.security.UserRole;
import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.validation.ChainValidationResult;
import com.rbatllet.blockchain.util.TestDatabaseUtils;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for blockchain key authorization and import functionality
 * 
 * This class tests:
 * 1. Historical validation of blocks after key revocation
 * 2. Import functionality with timestamp corrections
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Blockchain Key Authorization Tests")
class BlockchainKeyAuthorizationTest {

        private static final Logger logger = LoggerFactory.getLogger(BlockchainKeyAuthorizationTest.class);

        private KeyPair aliceKeyPair;
    private KeyPair bobKeyPair;
    private String alicePublicKey;
    private String bobPublicKey;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // Generate test key pairs using the new hierarchical key system
        // Create root keys for each test user
        CryptoUtil.KeyInfo aliceKeyInfo = CryptoUtil.createRootKey();
        CryptoUtil.KeyInfo bobKeyInfo = CryptoUtil.createRootKey();
        
        // Convert the stored key info to key pairs
        aliceKeyPair = new KeyPair(
            CryptoUtil.stringToPublicKey(aliceKeyInfo.getPublicKeyEncoded()),
            CryptoUtil.stringToPrivateKey(aliceKeyInfo.getPrivateKeyEncoded())
        );
        
        bobKeyPair = new KeyPair(
            CryptoUtil.stringToPublicKey(bobKeyInfo.getPublicKeyEncoded()),
            CryptoUtil.stringToPrivateKey(bobKeyInfo.getPrivateKeyEncoded())
        );
        
        // Get public key strings
        alicePublicKey = aliceKeyInfo.getPublicKeyEncoded();
        bobPublicKey = bobKeyInfo.getPublicKeyEncoded();
        
        // Clean database and enable test mode before each test to ensure test isolation
        TestDatabaseUtils.setupTest();
    }
    
    @AfterEach
    void tearDown() {
        // Clean database and disable test mode after each test to ensure test isolation
        TestDatabaseUtils.teardownTest();
    }
    


    // ===============================
    // Historical Block Validation Tests
    // ===============================

    @Test
    @Order(1)
    @DisplayName("Test Historical Block Validation After Key Revocation")
    void testHistoricalBlockValidationAfterKeyRevocation() {
        // Create a fresh blockchain for this test
        Blockchain blockchain = new Blockchain();

        // RBAC FIX (v1.0.6): Clear database before bootstrap to avoid "Existing users" error
        blockchain.clearAndReinitialize();

        // Add authorized key for Alice (genesis admin - RBAC v1.0.6)
        assertTrue(blockchain.createBootstrapAdmin(alicePublicKey, "Alice"),
                "Alice's key should be authorized successfully");

        // RBAC v1.0.6: Create Bob as second SUPER_ADMIN to allow revoking Alice
        // (Cannot revoke last SUPER_ADMIN due to system lockout protection)
        assertTrue(blockchain.addAuthorizedKey(bobPublicKey, "Bob", aliceKeyPair, UserRole.SUPER_ADMIN),
                "Bob should be authorized as SUPER_ADMIN");

        // Alice creates a block (should succeed)
        Block aliceBlock = blockchain.addBlockAndReturn("Alice's transaction", aliceKeyPair.getPrivate(), aliceKeyPair.getPublic());

        // Verify the block was created
        assertNotNull(aliceBlock, "Alice's block should exist");
        assertEquals("Alice's transaction", aliceBlock.getData(), "Block data should match");
        assertEquals(alicePublicKey, aliceBlock.getSignerPublicKey(), "Block should be signed by Alice");

        // Now revoke Alice's key (Bob remains as SUPER_ADMIN)
        assertTrue(blockchain.revokeAuthorizedKey(alicePublicKey),
                "Alice's key should be revoked successfully");
        
        // Verify Alice can no longer create new blocks
        assertFalse(blockchain.addBlock("Alice's new transaction", aliceKeyPair.getPrivate(), aliceKeyPair.getPublic()),
                "Alice should not be able to create blocks after key revocation");
        
        // BUT the existing block should still be valid because the key was authorized when created
        ChainValidationResult result = blockchain.validateChainDetailed();
        assertTrue(result.isStructurallyIntact(),
                "Chain structure should remain intact even after key revocation");
        // Note: Chain may not be fully compliant due to revoked key, but structure should be intact
        
        // Verify the historical block specifically
        assertTrue(validateHistoricalBlock(aliceBlock),
                "Alice's historical block should remain valid despite key revocation");
    }

    @Test
    @Order(2)
    @DisplayName("Test Unauthorized Key Block Rejection")
    void testUnauthorizedKeyBlockRejection() {
        // Create a fresh blockchain for this test
        Blockchain blockchain = new Blockchain();

        // RBAC FIX (v1.0.6): Clear database before bootstrap to avoid "Existing users" error
        blockchain.clearAndReinitialize();

        // Try to create a block with an unauthorized key (should fail)
        assertFalse(blockchain.addBlock("Unauthorized transaction", aliceKeyPair.getPrivate(), aliceKeyPair.getPublic()),
                "Should not be able to create block with unauthorized key");
        
        // Verify no new block was created (only genesis should exist)
        long blockCount = blockchain.getBlockCount();
        assertEquals(1, blockCount, "Should still have only genesis block");
        
        // Add the key now (genesis admin - RBAC v1.0.6)
        assertTrue(blockchain.createBootstrapAdmin(alicePublicKey, "Alice"),
                "Alice's key should be authorized successfully");
        
        // Now Alice can create blocks
        assertTrue(blockchain.addBlock("Alice's authorized transaction", aliceKeyPair.getPrivate(), aliceKeyPair.getPublic()),
                "Alice should be able to create blocks after authorization");
        
        // Verify block count increased
        assertEquals(2, blockchain.getBlockCount(), "Should now have genesis + 1 block");
    }

    @Test
    @Order(3)
    @DisplayName("Test Multiple Keys with Revocation")
    void testMultipleKeysWithRevocation() {
        // Create a fresh blockchain for this test
        Blockchain blockchain = new Blockchain();

        // RBAC FIX (v1.0.6): Clear database before bootstrap to avoid "Existing users" error
        blockchain.clearAndReinitialize();

        // 1. Authorize Alice (genesis admin - RBAC v1.0.6)
        assertTrue(blockchain.createBootstrapAdmin(alicePublicKey, "Alice"));

        // 2. Alice creates block
        assertTrue(blockchain.addBlock("Alice block 1", aliceKeyPair.getPrivate(), aliceKeyPair.getPublic()));

        // 3. Authorize Bob as SUPER_ADMIN (RBAC v1.0.6: need another SUPER_ADMIN to allow revoking Alice)
        assertTrue(blockchain.addAuthorizedKey(bobPublicKey, "Bob", aliceKeyPair, UserRole.SUPER_ADMIN));

        // 4. Both create blocks
        assertTrue(blockchain.addBlock("Alice block 2", aliceKeyPair.getPrivate(), aliceKeyPair.getPublic()));
        assertTrue(blockchain.addBlock("Bob block 1", bobKeyPair.getPrivate(), bobKeyPair.getPublic()));

        // 5. Revoke Alice (Bob remains as SUPER_ADMIN)
        assertTrue(blockchain.revokeAuthorizedKey(alicePublicKey));
        
        // 6. Bob creates another block (should work)
        assertTrue(blockchain.addBlock("Bob block 2", bobKeyPair.getPrivate(), bobKeyPair.getPublic()));
        
        // 7. Alice cannot create new blocks
        assertFalse(blockchain.addBlock("Alice block 3", aliceKeyPair.getPrivate(), aliceKeyPair.getPublic()));
        
        // 8. But all existing blocks should remain valid
        ChainValidationResult multiKeyResult = blockchain.validateChainDetailed();
        assertTrue(multiKeyResult.isStructurallyIntact(),
                "Chain with multiple keys and revocation should be structurally intact");
        
        // 9. Verify historical blocks are still valid
        List<Block> allBlocks = new ArrayList<>();
        blockchain.processChainInBatches(batch -> allBlocks.addAll(batch), 1000);

        for (Block block : allBlocks) {
            if (block.getBlockNumber() > 0) { // Skip genesis
                assertTrue(validateHistoricalBlock(block),
                        "Each block should remain valid: " + block.getBlockNumber());
            }
        }
    }

    // ===============================
    // Import Functionality Tests
    // ===============================

    @Test
    @Order(4)
    @DisplayName("Test Import with Timestamp Corrections")
    void testImportWithTimestampCorrections() throws Exception {
        // 1. Create original blockchain with specific timing
        Blockchain originalBlockchain = new Blockchain();

        // RBAC FIX (v1.0.6): Clear database before bootstrap to avoid "Existing users" error
        originalBlockchain.clearAndReinitialize();

        // 2. Authorize Alice (genesis admin - RBAC v1.0.6)
        assertTrue(originalBlockchain.createBootstrapAdmin(alicePublicKey, "Alice"));
        
        // 3. Wait to create clear time separation
        Thread.sleep(100);
        
        // 4. Alice creates blocks
        assertTrue(originalBlockchain.addBlock("Alice block 1", aliceKeyPair.getPrivate(), aliceKeyPair.getPublic()));
        assertTrue(originalBlockchain.addBlock("Alice block 2", aliceKeyPair.getPrivate(), aliceKeyPair.getPublic()));
        
        // 5. Export the blockchain
        File exportFile = tempDir.resolve("timestamp_corrections_test.json").toFile();
        assertTrue(originalBlockchain.exportChain(exportFile.getAbsolutePath()),
                "Export should succeed");
        
        // 6. Import into new blockchain
        Blockchain importedBlockchain = new Blockchain();

        // RBAC FIX (v1.0.6): Clear database before import to avoid "Existing users" error
        importedBlockchain.clearAndReinitialize();

        boolean importResult = importedBlockchain.importChain(exportFile.getAbsolutePath());
        
        // 7. Log some debug info if import fails
                if (!importResult) {
                        logger.error("Import failed. Let's check the export file exists: {}", exportFile.exists());
                        logger.error("Export file size: {}", exportFile.length());
                }
        
        assertTrue(importResult, "Import should succeed");
        
        // 8. Verify that Alice's key was imported
        List<AuthorizedKey> importedKeys = importedBlockchain.getAuthorizedKeys();
        assertFalse(importedKeys.isEmpty(), "Should have imported keys");
        
        AuthorizedKey aliceImportedKey = importedKeys.stream()
                .filter(key -> key.getPublicKey().equals(alicePublicKey))
                .findFirst()
                .orElse(null);
        
        assertNotNull(aliceImportedKey, "Alice's key should be imported");

        // 9. Get Alice's first block in imported chain
        List<Block> allImportedBlocks = new ArrayList<>();
        importedBlockchain.processChainInBatches(batch -> allImportedBlocks.addAll(batch), 1000);

        List<Block> aliceBlocks = allImportedBlocks.stream()
                .filter(block -> alicePublicKey.equals(block.getSignerPublicKey()))
                .toList();

        assertFalse(aliceBlocks.isEmpty(), "Alice should have blocks in imported chain");
        Block firstAliceBlock = aliceBlocks.get(0);
        
        // 10. Verify timestamp correction: key creation should be before first block
        assertTrue(aliceImportedKey.getCreatedAt().isBefore(firstAliceBlock.getTimestamp()),
                "Key creation time should be before first block time");
        
        // 11. The chain should be valid after import (this tests the timestamp corrections work)
        ChainValidationResult validationResult = importedBlockchain.validateChainDetailed();
        assertTrue(validationResult.isFullyCompliant(),
                "Imported chain with timestamp corrections should be fully compliant");
        assertTrue(validationResult.isStructurallyIntact(),
                "Imported chain with timestamp corrections should be structurally intact");
    }

    @Test
    @Order(5)
    @DisplayName("Test Integration: Historical Validation with Import")
    void testIntegrationHistoricalValidationWithImport() throws Exception {
        // This test validates the combination of historical validation and import functionality

        // 1. Create original blockchain
        Blockchain originalBlockchain = new Blockchain();

        // RBAC FIX (v1.0.6): Clear database before bootstrap to avoid "Existing users" error
        originalBlockchain.clearAndReinitialize();

        // 2. Authorize Alice, she creates blocks, then gets revoked (genesis admin - RBAC v1.0.6)
        assertTrue(originalBlockchain.createBootstrapAdmin(alicePublicKey, "Alice"));
        assertTrue(originalBlockchain.addBlock("Alice historical block", aliceKeyPair.getPrivate(), aliceKeyPair.getPublic()));

        // 3. Authorize Bob (created by Alice - RBAC v1.0.6)
        assertTrue(originalBlockchain.addAuthorizedKey(bobPublicKey, "Bob", aliceKeyPair, UserRole.USER));
        assertTrue(originalBlockchain.addBlock("Bob block", bobKeyPair.getPrivate(), bobKeyPair.getPublic()));
        
        // Note: We won't revoke Alice here since that might complicate import validation
        
        // 4. Export the chain
        File exportFile = tempDir.resolve("integration_test.json").toFile();
        assertTrue(originalBlockchain.exportChain(exportFile.getAbsolutePath()));
        
        // 5. Import into new blockchain
        Blockchain importedBlockchain = new Blockchain();

        // RBAC FIX (v1.0.6): Clear database before import to avoid "Existing users" error
        importedBlockchain.clearAndReinitialize();

        assertTrue(importedBlockchain.importChain(exportFile.getAbsolutePath()),
                "Complex chain should import successfully");
        
        // 6. Verify imported chain maintains validity
        ChainValidationResult complexImportResult = importedBlockchain.validateChainDetailed();
        assertTrue(complexImportResult.isFullyCompliant(),
                "Imported chain should maintain full compliance");
        assertTrue(complexImportResult.isStructurallyIntact(),
                "Imported chain should maintain structural integrity");

        // 7. Verify Alice's historical blocks are valid
        List<Block> allBlocksForAlice = new ArrayList<>();
        importedBlockchain.processChainInBatches(batch -> allBlocksForAlice.addAll(batch), 1000);

        List<Block> aliceBlocks = allBlocksForAlice.stream()
                .filter(block -> alicePublicKey.equals(block.getSignerPublicKey()))
                .toList();

        assertEquals(1, aliceBlocks.size(), "Alice should have 1 block in imported chain");
        for (Block block : aliceBlocks) {
            assertTrue(validateHistoricalBlock(block),
                    "Alice's historical block should be valid: " + block.getData());
        }

        // 8. Verify timestamp corrections were applied
        List<AuthorizedKey> importedKeys = importedBlockchain.getAuthorizedKeys();
        List<Block> allBlocksForKeys = new ArrayList<>();
        importedBlockchain.processChainInBatches(batch -> allBlocksForKeys.addAll(batch), 1000);

        for (AuthorizedKey key : importedKeys) {
            Block firstBlock = findFirstBlockByKey(allBlocksForKeys, key.getPublicKey());
            if (firstBlock != null) {
                assertTrue(key.getCreatedAt().isBefore(firstBlock.getTimestamp()),
                        "Key " + key.getOwnerName() + " should be created before its first block");
            }
        }
        
        logger.info("✅ Historical validation functionality - VALIDATED");
        logger.info("✅ Import with timestamp corrections - VALIDATED");
    }

    @Test
    @Order(6)
    @DisplayName("Test Historical Block Validation After Key Revocation")
    void testHistoricalBlockValidationAfterRevocation() {

        // Create a fresh blockchain
        Blockchain blockchain = new Blockchain();

        // RBAC FIX (v1.0.6): Clear database before bootstrap to avoid "Existing users" error
        blockchain.clearAndReinitialize();

        // 1. Authorize Alice (genesis admin - RBAC v1.0.6)
        assertTrue(blockchain.createBootstrapAdmin(alicePublicKey, "Alice"));

        // 2. RBAC v1.0.6: Create Bob as second SUPER_ADMIN to allow revoking Alice
        assertTrue(blockchain.addAuthorizedKey(bobPublicKey, "Bob", aliceKeyPair, UserRole.SUPER_ADMIN));

        // 3. Alice creates multiple blocks
        assertTrue(blockchain.addBlock("Alice block 1", aliceKeyPair.getPrivate(), aliceKeyPair.getPublic()));
        assertTrue(blockchain.addBlock("Alice block 2", aliceKeyPair.getPrivate(), aliceKeyPair.getPublic()));

        // 4. Store Alice's blocks for validation
        List<Block> allChainBlocks = new ArrayList<>();
        blockchain.processChainInBatches(batch -> allChainBlocks.addAll(batch), 1000);

        List<Block> aliceBlocks = allChainBlocks.stream()
                .filter(block -> alicePublicKey.equals(block.getSignerPublicKey()))
                .toList();

        assertEquals(2, aliceBlocks.size(), "Alice should have created 2 blocks");

        // 5. Revoke Alice's key (Bob remains as SUPER_ADMIN)
        assertTrue(blockchain.revokeAuthorizedKey(alicePublicKey));
        
        // 5. Alice cannot create new blocks
        assertFalse(blockchain.addBlock("Alice block 3", aliceKeyPair.getPrivate(), aliceKeyPair.getPublic()));
        
        // 6. But her historical blocks should still be valid individually
        for (Block block : aliceBlocks) {
            assertTrue(validateHistoricalBlock(block),
                    "Alice's historical block should remain valid: " + block.getData());
        }
        
        // 7. And the entire chain should remain valid
        ChainValidationResult revokedKeyResult = blockchain.validateChainDetailed();
        assertTrue(revokedKeyResult.isStructurallyIntact(),
                "Chain should remain structurally intact despite key revocation");
    }

    // ===============================
    // Helper Methods
    // ===============================
    
    /**
     * Helper method to validate a specific block using the same logic as the main validation
     * This validates that the key was authorized when the block was created
     */
    private boolean validateHistoricalBlock(Block block) {
        try {
            if (block.getBlockNumber() == 0) {
                return true; // Genesis block is always valid
            }
            
            // Check if the key was authorized when the block was created
            AuthorizedKeyDAO authorizedKeyDAO = new AuthorizedKeyDAO();
            return authorizedKeyDAO.wasKeyAuthorizedAt(block.getSignerPublicKey(), block.getTimestamp());
            
        } catch (Exception e) {
            logger.error("Error validating historical block: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Helper method to find the first block signed by a specific key
     */
    private Block findFirstBlockByKey(List<Block> blocks, String publicKey) {
        return blocks.stream()
                .filter(block -> publicKey.equals(block.getSignerPublicKey()))
                .min((b1, b2) -> b1.getTimestamp().compareTo(b2.getTimestamp()))
                .orElse(null);
    }

    @Test
    @Order(7)
    @DisplayName("Test Key Revocation with Temporal Validation")
    void testKeyRevocationWithTemporalValidation() {
        // Create a fresh blockchain
        Blockchain blockchain = new Blockchain();

        // RBAC FIX (v1.0.6): Clear database before bootstrap to avoid "Existing users" error
        blockchain.clearAndReinitialize();

        // 1. Authorize Alice (genesis admin - RBAC v1.0.6)
        assertTrue(blockchain.createBootstrapAdmin(alicePublicKey, "Alice"));

        // 2. RBAC v1.0.6: Create Bob as second SUPER_ADMIN to allow revoking Alice
        assertTrue(blockchain.addAuthorizedKey(bobPublicKey, "Bob", aliceKeyPair, UserRole.SUPER_ADMIN));

        // 3. Alice creates a block - should be valid
        Block historicalBlock = blockchain.addBlockAndReturn("Historical block", aliceKeyPair.getPrivate(), aliceKeyPair.getPublic());
        assertNotNull(historicalBlock);

        // 4. Wait a moment to ensure temporal separation
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 5. Revoke Alice (Bob remains as SUPER_ADMIN)
        assertTrue(blockchain.revokeAuthorizedKey(alicePublicKey));
        
        // 5. Alice tries to create a new block - should fail
        assertFalse(blockchain.addBlock("Future block", aliceKeyPair.getPrivate(), aliceKeyPair.getPublic()),
                "Alice should not be able to create blocks after revocation");
        
        // 6. CRITICAL: The historical block should still be valid
        assertTrue(validateHistoricalBlock(historicalBlock),
                "Historical block should remain valid despite key revocation");
        
        // 7. CRITICAL: The entire chain should still be valid
        ChainValidationResult historicalResult = blockchain.validateChainDetailed();
        assertTrue(historicalResult.isStructurallyIntact(),
                "Chain should remain structurally intact despite key revocation");
    }

    @Test
    @Order(8)
    @DisplayName("Test Re-authorization Scenario")
    void testReAuthorizationScenario() {
        // Create a fresh blockchain
        Blockchain blockchain = new Blockchain();

        // RBAC FIX (v1.0.6): Clear database before bootstrap to avoid "Existing users" error
        blockchain.clearAndReinitialize();

        // 1. Create Alice as genesis admin (RBAC v1.0.6)
        assertTrue(blockchain.createBootstrapAdmin(alicePublicKey, "Alice"));
        assertTrue(blockchain.addBlock("First period", aliceKeyPair.getPrivate(), aliceKeyPair.getPublic()));

        // 2. Alice creates Bob as second SUPER_ADMIN (for re-authorization capability)
        assertTrue(blockchain.addAuthorizedKey(bobPublicKey, "Bob", aliceKeyPair, UserRole.SUPER_ADMIN));

        // Wait for temporal separation
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 3. Revoke Alice (Bob remains as active SUPER_ADMIN)
        assertTrue(blockchain.revokeAuthorizedKey(alicePublicKey));

        // Wait again
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 4. Bob re-authorizes Alice (RBAC v1.0.6 - requires active SUPER_ADMIN)
        assertTrue(blockchain.addAuthorizedKey(alicePublicKey, "Alice-Reauthorized", bobKeyPair, UserRole.SUPER_ADMIN));

        // 5. Alice can create new blocks after re-authorization
        assertTrue(blockchain.addBlock("Second period", aliceKeyPair.getPrivate(), aliceKeyPair.getPublic()));

        // 6. Both periods should be valid
        ChainValidationResult reAuthResult = blockchain.validateChainDetailed();
        assertTrue(reAuthResult.isFullyCompliant(),
                "Chain should be fully compliant after re-authorization");
        assertTrue(reAuthResult.isStructurallyIntact(),
                "Chain should be structurally intact after re-authorization");

        // 7. Verify all blocks individually
        List<Block> allBlocks = new ArrayList<>();
        blockchain.processChainInBatches(batch -> allBlocks.addAll(batch), 1000);

        for (Block block : allBlocks) {
            if (block.getBlockNumber() > 0) { // Skip genesis
                assertTrue(validateHistoricalBlock(block),
                        "Block " + block.getBlockNumber() + " should be valid");
            }
        }
    }

    @Test
    @Order(9)
    @DisplayName("Test Import Export with Complex Temporal History")
    void testImportExportWithComplexTemporalHistory() throws Exception {
        // Test that ensures import maintains temporal consistency

        // 1. Create chain with complex temporal history
        Blockchain original = new Blockchain();

        // RBAC FIX (v1.0.6): Clear database before bootstrap to avoid "Existing users" error
        original.clearAndReinitialize();

        // Simulate history: authorize -> create blocks -> revoke -> re-authorize (RBAC v1.0.6)
        // Alice is genesis admin, creates Bob as second SUPER_ADMIN
        assertTrue(original.createBootstrapAdmin(alicePublicKey, "Alice"));
        assertTrue(original.addAuthorizedKey(bobPublicKey, "Bob", aliceKeyPair, UserRole.SUPER_ADMIN));
        assertTrue(original.addBlock("Block 1", aliceKeyPair.getPrivate(), aliceKeyPair.getPublic()));

        // Wait for temporal separation
        Thread.sleep(100);

        // Bob revokes Alice (Bob remains as active SUPER_ADMIN)
        assertTrue(original.revokeAuthorizedKey(alicePublicKey));

        // Wait again
        Thread.sleep(100);

        // Bob re-authorizes Alice
        assertTrue(original.addAuthorizedKey(alicePublicKey, "Alice-2", bobKeyPair, UserRole.SUPER_ADMIN));
        assertTrue(original.addBlock("Block 2", aliceKeyPair.getPrivate(), aliceKeyPair.getPublic()));
        
        // 2. Verify original chain is valid
        ChainValidationResult originalValid = original.validateChainDetailed();
        assertTrue(originalValid.isFullyCompliant(), "Original chain must be fully compliant before export");
        assertTrue(originalValid.isStructurallyIntact(), "Original chain must be structurally intact before export");
        
        // 3. Export
        File exportFile = tempDir.resolve("temporal_consistency.json").toFile();
        assertTrue(original.exportChain(exportFile.getAbsolutePath()));
        
        // 4. Import
        Blockchain imported = new Blockchain();

        // RBAC FIX (v1.0.6): Clear database before import to avoid "Existing users" error
        imported.clearAndReinitialize();

        boolean importSuccess = imported.importChain(exportFile.getAbsolutePath());
        
        // 5. Import should succeed
        assertTrue(importSuccess, "Import should succeed with temporal corrections");
        
        // 6. CRITICAL: The imported chain should maintain validity
        ChainValidationResult importedValid = imported.validateChainDetailed();
        assertTrue(importedValid.isFullyCompliant(),
                "Imported chain should maintain full compliance");
        assertTrue(importedValid.isStructurallyIntact(),
                "Imported chain should maintain structural integrity");

        // 7. All blocks should be valid individually
        List<Block> importedBlocks = new ArrayList<>();
        imported.processChainInBatches(batch -> importedBlocks.addAll(batch), 1000);

        for (Block block : importedBlocks) {
            if (block.getBlockNumber() > 0) {
                assertTrue(validateHistoricalBlock(block),
                        "Block " + block.getBlockNumber() + " should remain valid after import");
            }
        }
        
        logger.info("✅ Complex temporal history preserved through import/export");
    }}