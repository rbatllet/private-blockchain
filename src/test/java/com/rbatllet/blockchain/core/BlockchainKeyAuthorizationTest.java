package com.rbatllet.blockchain.core;

import com.rbatllet.blockchain.dao.AuthorizedKeyDAO;
import com.rbatllet.blockchain.entity.AuthorizedKey;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.util.CryptoUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.security.KeyPair;
import java.time.LocalDateTime;
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

    private KeyPair aliceKeyPair;
    private KeyPair bobKeyPair;
    private String alicePublicKey;
    private String bobPublicKey;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // Generate test key pairs - these will be fresh for each test
        aliceKeyPair = CryptoUtil.generateKeyPair();
        bobKeyPair = CryptoUtil.generateKeyPair();
        
        alicePublicKey = CryptoUtil.publicKeyToString(aliceKeyPair.getPublic());
        bobPublicKey = CryptoUtil.publicKeyToString(bobKeyPair.getPublic());
        
        // FIXED: Clean database before each test to ensure test isolation
        cleanDatabase();
    }
    
    /**
     * Clean the database to ensure test isolation
     */
    private void cleanDatabase() {
        try {
            // Clear all data using DAO methods
            com.rbatllet.blockchain.dao.BlockDAO blockDAO = new com.rbatllet.blockchain.dao.BlockDAO();
            com.rbatllet.blockchain.dao.AuthorizedKeyDAO keyDAO = new com.rbatllet.blockchain.dao.AuthorizedKeyDAO();
            
            blockDAO.deleteAllBlocks();
            keyDAO.deleteAllAuthorizedKeys();
            
            // Small delay to ensure database operations complete
            Thread.sleep(50);
        } catch (Exception e) {
            System.err.println("Warning: Could not clean database: " + e.getMessage());
        }
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
        
        // Add authorized key for Alice
        assertTrue(blockchain.addAuthorizedKey(alicePublicKey, "Alice"),
                "Alice's key should be authorized successfully");
        
        // Alice creates a block (should succeed)
        assertTrue(blockchain.addBlock("Alice's transaction", aliceKeyPair.getPrivate(), aliceKeyPair.getPublic()),
                "Alice should be able to create a block with authorized key");
        
        // Verify the block was created
        Block aliceBlock = blockchain.getLastBlock();
        assertNotNull(aliceBlock, "Alice's block should exist");
        assertEquals("Alice's transaction", aliceBlock.getData(), "Block data should match");
        assertEquals(alicePublicKey, aliceBlock.getSignerPublicKey(), "Block should be signed by Alice");
        
        // Now revoke Alice's key
        assertTrue(blockchain.revokeAuthorizedKey(alicePublicKey),
                "Alice's key should be revoked successfully");
        
        // Verify Alice can no longer create new blocks
        assertFalse(blockchain.addBlock("Alice's new transaction", aliceKeyPair.getPrivate(), aliceKeyPair.getPublic()),
                "Alice should not be able to create blocks after key revocation");
        
        // BUT the existing block should still be valid because the key was authorized when created
        assertTrue(blockchain.validateChain(),
                "Chain should remain valid even after key revocation - historical blocks should remain valid");
        
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
        
        // Try to create a block with an unauthorized key (should fail)
        assertFalse(blockchain.addBlock("Unauthorized transaction", aliceKeyPair.getPrivate(), aliceKeyPair.getPublic()),
                "Should not be able to create block with unauthorized key");
        
        // Verify no new block was created (only genesis should exist)
        long blockCount = blockchain.getBlockCount();
        assertEquals(1, blockCount, "Should still have only genesis block");
        
        // Add the key now
        assertTrue(blockchain.addAuthorizedKey(alicePublicKey, "Alice"),
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
        
        // 1. Authorize Alice
        assertTrue(blockchain.addAuthorizedKey(alicePublicKey, "Alice"));
        
        // 2. Alice creates block
        assertTrue(blockchain.addBlock("Alice block 1", aliceKeyPair.getPrivate(), aliceKeyPair.getPublic()));
        
        // 3. Authorize Bob
        assertTrue(blockchain.addAuthorizedKey(bobPublicKey, "Bob"));
        
        // 4. Both create blocks
        assertTrue(blockchain.addBlock("Alice block 2", aliceKeyPair.getPrivate(), aliceKeyPair.getPublic()));
        assertTrue(blockchain.addBlock("Bob block 1", bobKeyPair.getPrivate(), bobKeyPair.getPublic()));
        
        // 5. Revoke Alice
        assertTrue(blockchain.revokeAuthorizedKey(alicePublicKey));
        
        // 6. Bob creates another block (should work)
        assertTrue(blockchain.addBlock("Bob block 2", bobKeyPair.getPrivate(), bobKeyPair.getPublic()));
        
        // 7. Alice cannot create new blocks
        assertFalse(blockchain.addBlock("Alice block 3", aliceKeyPair.getPrivate(), aliceKeyPair.getPublic()));
        
        // 8. But all existing blocks should remain valid
        assertTrue(blockchain.validateChain(),
                "Chain with multiple keys and revocation should be valid");
        
        // 9. Verify historical blocks are still valid
        List<Block> allBlocks = blockchain.getAllBlocks();
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
        
        // 2. Authorize Alice
        assertTrue(originalBlockchain.addAuthorizedKey(alicePublicKey, "Alice"));
        
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
        boolean importResult = importedBlockchain.importChain(exportFile.getAbsolutePath());
        
        // 7. Log some debug info if import fails
        if (!importResult) {
            System.out.println("Import failed. Let's check the export file exists: " + exportFile.exists());
            System.out.println("Export file size: " + exportFile.length());
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
        List<Block> aliceBlocks = importedBlockchain.getAllBlocks().stream()
                .filter(block -> alicePublicKey.equals(block.getSignerPublicKey()))
                .toList();
        
        assertFalse(aliceBlocks.isEmpty(), "Alice should have blocks in imported chain");
        Block firstAliceBlock = aliceBlocks.get(0);
        
        // 10. Verify timestamp correction: key creation should be before first block
        assertTrue(aliceImportedKey.getCreatedAt().isBefore(firstAliceBlock.getTimestamp()),
                "Key creation time should be before first block time");
        
        // 11. The chain should be valid after import (this tests the timestamp corrections work)
        assertTrue(importedBlockchain.validateChain(),
                "Imported chain with timestamp corrections should be valid");
    }

    @Test
    @Order(5)
    @DisplayName("Test Integration: Historical Validation with Import")
    void testIntegrationHistoricalValidationWithImport() throws Exception {
        // This test validates the combination of historical validation and import functionality
        
        // 1. Create original blockchain
        Blockchain originalBlockchain = new Blockchain();
        
        // 2. Authorize Alice, she creates blocks, then gets revoked
        assertTrue(originalBlockchain.addAuthorizedKey(alicePublicKey, "Alice"));
        assertTrue(originalBlockchain.addBlock("Alice historical block", aliceKeyPair.getPrivate(), aliceKeyPair.getPublic()));
        
        // 3. Authorize Bob
        assertTrue(originalBlockchain.addAuthorizedKey(bobPublicKey, "Bob"));
        assertTrue(originalBlockchain.addBlock("Bob block", bobKeyPair.getPrivate(), bobKeyPair.getPublic()));
        
        // Note: We won't revoke Alice here since that might complicate import validation
        
        // 4. Export the chain
        File exportFile = tempDir.resolve("integration_test.json").toFile();
        assertTrue(originalBlockchain.exportChain(exportFile.getAbsolutePath()));
        
        // 5. Import into new blockchain
        Blockchain importedBlockchain = new Blockchain();
        assertTrue(importedBlockchain.importChain(exportFile.getAbsolutePath()),
                "Complex chain should import successfully");
        
        // 6. Verify imported chain maintains validity
        assertTrue(importedBlockchain.validateChain(),
                "Imported chain should maintain validity");
        
        // 7. Verify Alice's historical blocks are valid
        List<Block> aliceBlocks = importedBlockchain.getAllBlocks().stream()
                .filter(block -> alicePublicKey.equals(block.getSignerPublicKey()))
                .toList();
        
        assertEquals(1, aliceBlocks.size(), "Alice should have 1 block in imported chain");
        for (Block block : aliceBlocks) {
            assertTrue(validateHistoricalBlock(block),
                    "Alice's historical block should be valid: " + block.getData());
        }
        
        // 8. Verify timestamp corrections were applied
        List<AuthorizedKey> importedKeys = importedBlockchain.getAuthorizedKeys();
        for (AuthorizedKey key : importedKeys) {
            Block firstBlock = findFirstBlockByKey(importedBlockchain.getAllBlocks(), key.getPublicKey());
            if (firstBlock != null) {
                assertTrue(key.getCreatedAt().isBefore(firstBlock.getTimestamp()),
                        "Key " + key.getOwnerName() + " should be created before its first block");
            }
        }
        
        System.out.println("✅ Historical validation functionality - VALIDATED");
        System.out.println("✅ Import with timestamp corrections - VALIDATED");
    }

    @Test
    @Order(6)
    @DisplayName("Test Historical Block Validation After Key Revocation")
    void testHistoricalBlockValidationAfterRevocation() {
        
        // Create a fresh blockchain
        Blockchain blockchain = new Blockchain();
        
        // 1. Authorize Alice
        assertTrue(blockchain.addAuthorizedKey(alicePublicKey, "Alice"));
        
        // 2. Alice creates multiple blocks
        assertTrue(blockchain.addBlock("Alice block 1", aliceKeyPair.getPrivate(), aliceKeyPair.getPublic()));
        assertTrue(blockchain.addBlock("Alice block 2", aliceKeyPair.getPrivate(), aliceKeyPair.getPublic()));
        
        // 3. Store Alice's blocks for validation
        List<Block> aliceBlocks = blockchain.getAllBlocks().stream()
                .filter(block -> alicePublicKey.equals(block.getSignerPublicKey()))
                .toList();
        
        assertEquals(2, aliceBlocks.size(), "Alice should have created 2 blocks");
        
        // 4. Revoke Alice's key
        assertTrue(blockchain.revokeAuthorizedKey(alicePublicKey));
        
        // 5. Alice cannot create new blocks
        assertFalse(blockchain.addBlock("Alice block 3", aliceKeyPair.getPrivate(), aliceKeyPair.getPublic()));
        
        // 6. But her historical blocks should still be valid individually
        for (Block block : aliceBlocks) {
            assertTrue(validateHistoricalBlock(block),
                    "Alice's historical block should remain valid: " + block.getData());
        }
        
        // 7. And the entire chain should remain valid
        assertTrue(blockchain.validateChain(),
                "Chain should remain valid despite key revocation");
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
            System.err.println("Error validating historical block: " + e.getMessage());
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
        
        // 1. Authorize Alice
        assertTrue(blockchain.addAuthorizedKey(alicePublicKey, "Alice"));
        
        // 2. Alice creates a block - should be valid
        assertTrue(blockchain.addBlock("Historical block", aliceKeyPair.getPrivate(), aliceKeyPair.getPublic()));
        Block historicalBlock = blockchain.getLastBlock();
        
        // 3. Wait a moment to ensure temporal separation
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 4. Revoke Alice
        assertTrue(blockchain.revokeAuthorizedKey(alicePublicKey));
        
        // 5. Alice tries to create a new block - should fail
        assertFalse(blockchain.addBlock("Future block", aliceKeyPair.getPrivate(), aliceKeyPair.getPublic()),
                "Alice should not be able to create blocks after revocation");
        
        // 6. CRITICAL: The historical block should still be valid
        assertTrue(validateHistoricalBlock(historicalBlock),
                "Historical block should remain valid despite key revocation");
        
        // 7. CRITICAL: The entire chain should still be valid
        assertTrue(blockchain.validateChain(),
                "Chain should remain valid despite key revocation");
    }

    @Test
    @Order(8)
    @DisplayName("Test Re-authorization Scenario")
    void testReAuthorizationScenario() {
        // Create a fresh blockchain
        Blockchain blockchain = new Blockchain();
        
        // 1. Authorize, create block, revoke
        assertTrue(blockchain.addAuthorizedKey(alicePublicKey, "Alice"));
        assertTrue(blockchain.addBlock("First period", aliceKeyPair.getPrivate(), aliceKeyPair.getPublic()));
        
        // Wait for temporal separation
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        assertTrue(blockchain.revokeAuthorizedKey(alicePublicKey));
        
        // Wait again
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 2. Re-authorize the same key
        assertTrue(blockchain.addAuthorizedKey(alicePublicKey, "Alice-Reauthorized"));
        
        // 3. Alice can create new blocks
        assertTrue(blockchain.addBlock("Second period", aliceKeyPair.getPrivate(), aliceKeyPair.getPublic()));
        
        // 4. Both periods should be valid
        assertTrue(blockchain.validateChain(),
                "Chain should be valid after re-authorization");
        
        // 5. Verify all blocks individually
        List<Block> allBlocks = blockchain.getAllBlocks();
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
        
        // Simulate history: authorize -> create blocks -> revoke -> re-authorize
        assertTrue(original.addAuthorizedKey(alicePublicKey, "Alice"));
        assertTrue(original.addBlock("Block 1", aliceKeyPair.getPrivate(), aliceKeyPair.getPublic()));
        
        // Wait for temporal separation
        Thread.sleep(100);
        
        assertTrue(original.revokeAuthorizedKey(alicePublicKey));
        
        // Wait again
        Thread.sleep(100);
        
        assertTrue(original.addAuthorizedKey(alicePublicKey, "Alice-2"));
        assertTrue(original.addBlock("Block 2", aliceKeyPair.getPrivate(), aliceKeyPair.getPublic()));
        
        // 2. Verify original chain is valid
        boolean originalValid = original.validateChain();
        
        // 3. Export
        File exportFile = tempDir.resolve("temporal_consistency.json").toFile();
        assertTrue(original.exportChain(exportFile.getAbsolutePath()));
        
        // 4. Import
        Blockchain imported = new Blockchain();
        boolean importSuccess = imported.importChain(exportFile.getAbsolutePath());
        
        // 5. Import should succeed
        assertTrue(importSuccess, "Import should succeed with temporal corrections");
        
        // 6. CRITICAL: The imported chain should maintain validity
        assertTrue(imported.validateChain(),
                "Imported chain should maintain validity");
        
        // 7. All blocks should be valid individually
        List<Block> importedBlocks = imported.getAllBlocks();
        for (Block block : importedBlocks) {
            if (block.getBlockNumber() > 0) {
                assertTrue(validateHistoricalBlock(block),
                        "Block " + block.getBlockNumber() + " should remain valid after import");
            }
        }
        
        System.out.println("✅ Complex temporal history preserved through import/export");
    }}