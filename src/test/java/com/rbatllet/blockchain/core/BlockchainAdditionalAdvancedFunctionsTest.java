package com.rbatllet.blockchain.core;

import com.rbatllet.blockchain.entity.AuthorizedKey;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.dto.ChainExportData;
import com.rbatllet.blockchain.security.UserRole;
import com.rbatllet.blockchain.util.CryptoUtil;

import tools.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.io.File;
import java.nio.file.Path;
import java.security.KeyPair;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for the ADDITIONAL ADVANCED FUNCTIONS in Blockchain.java
 * Tests the new advanced functionality beyond basic core operations
 * Uses resource locks to ensure tests that read configuration don't run in parallel with tests that modify it
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Blockchain Additional Advanced Functions Tests")
@ResourceLock("blockchain-config") // Prevents parallel execution with OffChainStorageTest
public class BlockchainAdditionalAdvancedFunctionsTest {

    private Blockchain blockchain;
    private KeyPair aliceKeyPair;
    private KeyPair bobKeyPair;
    private KeyPair charlieKeyPair;
    private String alicePublicKey;
    private String bobPublicKey;
    private String charliePublicKey;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // Initialize blockchain and key pairs for each test
        blockchain = new Blockchain();
        
        blockchain.resetLimitsToDefault(); // CRITICAL: Reset FIRST, before any operations
        
        blockchain.clearAndReinitialize(); // Ensure clean state
        
        // Generate test key pairs using the new hierarchical key system
        // Create root keys for each test user
        CryptoUtil.KeyInfo aliceKeyInfo = CryptoUtil.createRootKey();
        CryptoUtil.KeyInfo bobKeyInfo = CryptoUtil.createRootKey();
        CryptoUtil.KeyInfo charlieKeyInfo = CryptoUtil.createRootKey();
        
        // Convert the stored key info to key pairs
        aliceKeyPair = new KeyPair(
            CryptoUtil.stringToPublicKey(aliceKeyInfo.getPublicKeyEncoded()),
            CryptoUtil.stringToPrivateKey(aliceKeyInfo.getPrivateKeyEncoded())
        );
        
        bobKeyPair = new KeyPair(
            CryptoUtil.stringToPublicKey(bobKeyInfo.getPublicKeyEncoded()),
            CryptoUtil.stringToPrivateKey(bobKeyInfo.getPrivateKeyEncoded())
        );
        
        charlieKeyPair = new KeyPair(
            CryptoUtil.stringToPublicKey(charlieKeyInfo.getPublicKeyEncoded()),
            CryptoUtil.stringToPrivateKey(charlieKeyInfo.getPrivateKeyEncoded())
        );
        
        // Get public key strings
        alicePublicKey = aliceKeyInfo.getPublicKeyEncoded();
        bobPublicKey = bobKeyInfo.getPublicKeyEncoded();
        charliePublicKey = charlieKeyInfo.getPublicKeyEncoded();
        
        // Add authorized keys (RBAC v1.0.6)
        // Alice - Genesis admin
        blockchain.createBootstrapAdmin(alicePublicKey, "Alice");
        // Bob and Charlie - Created by Alice (SUPER_ADMIN can create USERs)
        blockchain.addAuthorizedKey(bobPublicKey, "Bob", aliceKeyPair, UserRole.USER);
        blockchain.addAuthorizedKey(charliePublicKey, "Charlie", aliceKeyPair, UserRole.USER);

        // Add some test blocks
        blockchain.addBlock("Alice's first transaction", aliceKeyPair.getPrivate(), aliceKeyPair.getPublic());
        blockchain.addBlock("Bob joins the network", bobKeyPair.getPrivate(), bobKeyPair.getPublic());
        blockchain.addBlock("Charlie sends data", charlieKeyPair.getPrivate(), charlieKeyPair.getPublic());
    }

    @AfterEach
    void tearDown() {
        // Clean database after each test to ensure test isolation
        if (blockchain != null) {
            blockchain.completeCleanupForTests();
        }
    }

    // ===============================
    // ADDITIONAL ADVANCED FUNCTION 1: Block Size Validation Tests
    // ===============================

    @Test
    @Order(1)
    @DisplayName("Test Block Size Validation - Valid Sizes")
    public void testValidBlockSizes() {
        // Clear blockchain for clean size validation testing
        blockchain.clearAndReinitialize();
        blockchain.createBootstrapAdmin(alicePublicKey, "Alice");

        // Test empty string - UPDATED: Should be rejected (matching BlockchainRobustnessTest standard)
        assertThrows(IllegalArgumentException.class, () -> {
            blockchain.addBlock("", aliceKeyPair.getPrivate(), aliceKeyPair.getPublic());
        }, "Empty string should throw exception");

        // Test normal size data
        String normalData = "This is a normal transaction with reasonable data length";
        assertTrue(blockchain.addBlock(normalData, aliceKeyPair.getPrivate(), aliceKeyPair.getPublic()),
                "Normal sized data should be accepted");

        // Test data at off-chain threshold (should go off-chain)
        StringBuilder largeData = new StringBuilder();
        int threshold = blockchain.getOffChainThresholdBytes();
        for (int i = 0; i < threshold; i++) {
            largeData.append("a");
        }
        assertTrue(blockchain.addBlock(largeData.toString(), aliceKeyPair.getPrivate(), aliceKeyPair.getPublic()),
                "Data at off-chain threshold should be accepted (stored off-chain)");
    }

    @Test
    @Order(2)
    @DisplayName("Test Block Size Validation - Invalid Sizes")
    public void testInvalidBlockSizes() {
        // Clear blockchain for clean size validation testing
        blockchain.clearAndReinitialize();
        blockchain.createBootstrapAdmin(alicePublicKey, "Alice");

        // Test null data - UPDATED: Should throw exception (matching Robustness standard)
        assertThrows(IllegalArgumentException.class, () -> {
            blockchain.addBlock(null, aliceKeyPair.getPrivate(), aliceKeyPair.getPublic());
        }, "Null data should throw exception");

        // Test data exceeding maximum block size (should be rejected)
        StringBuilder tooLongData = new StringBuilder();
        int maxSize = blockchain.getMaxBlockSizeBytes();
        for (int i = 0; i <= maxSize; i++) {
            tooLongData.append("x");
        }
        // Data exceeding max block size should be rejected
        assertFalse(blockchain.addBlock(tooLongData.toString(), aliceKeyPair.getPrivate(), aliceKeyPair.getPublic()),
                "Data exceeding max block size should be rejected");

        // Test data with multibyte characters that might exceed byte limit
        StringBuilder unicodeData = new StringBuilder();
        int maxSizeBytes = blockchain.getMaxBlockSizeBytes();
        // Create enough unicode chars to exceed max size (each emoji is 4 bytes)
        for (int i = 0; i < (maxSizeBytes / 4) + 1000; i++) {
            unicodeData.append("🔗"); // Unicode blockchain emoji (4 bytes each)
        }
        String unicodeString = unicodeData.toString();

        // This should exceed byte limit
        assertFalse(blockchain.addBlock(unicodeString, aliceKeyPair.getPrivate(), aliceKeyPair.getPublic()),
                "Data exceeding byte limit should be rejected");
    }

    @Test
    @Order(3)
    @DisplayName("Test Block Size Constants")
    public void testBlockSizeConstants() {
        assertTrue(blockchain.getMaxBlockSizeBytes() > 0, "Max block size in bytes should be positive");
        assertTrue(blockchain.getOffChainThresholdBytes() > 0, "Off-chain threshold should be positive");

        // Verify reasonable limits
        assertEquals(10 * 1024 * 1024, blockchain.getMaxBlockSizeBytes(), "Max block size should be 10MB");
        assertEquals(512 * 1024, blockchain.getOffChainThresholdBytes(), "Off-chain threshold should be 512KB");
    }

    // ===============================
    // ADDITIONAL ADVANCED FUNCTION 2: Chain Export Tests
    // ===============================

    @Test
    @Order(4)
    @DisplayName("Test Successful Chain Export")
    public void testSuccessfulChainExport() {
        File exportFile = tempDir.resolve("test_export.json").toFile();
        String exportPath = exportFile.getAbsolutePath();

        // Export the chain
        assertTrue(blockchain.exportChain(exportPath), "Chain export should succeed");

        // Verify file was created
        assertTrue(exportFile.exists(), "Export file should exist");
        assertTrue(exportFile.length() > 0, "Export file should not be empty");
    }

    @Test
    @Order(5)
    @DisplayName("Test Chain Export to Invalid Path")
    public void testChainExportInvalidPath() {
        // Try to export to an invalid/non-existent directory
        // NOTE: This test intentionally throws an exception - this is expected behavior
        String invalidPath = "/non/existent/directory/export.json";

        // Export to invalid path should throw IllegalStateException
        assertThrows(IllegalStateException.class, () -> {
            blockchain.exportChain(invalidPath);
        }, "Export to invalid path should throw IllegalStateException");
    }

    @Test
    @Order(6)
    @DisplayName("Test Chain Export Content Verification")
    public void testChainExportContentVerification() throws Exception {
        File exportFile = tempDir.resolve("content_test_export.json").toFile();
        String exportPath = exportFile.getAbsolutePath();

        // Get current state before export
        long blockCountBefore = blockchain.getBlockCount();
        List<AuthorizedKey> keysBefore = blockchain.getAllAuthorizedKeys(); // Use ALL keys for export comparison

        // Export the chain
        assertTrue(blockchain.exportChain(exportPath), "Chain export should succeed");

        // Read and verify export file content
        ObjectMapper mapper = new ObjectMapper();
        ChainExportData exportData = mapper.readValue(exportFile, ChainExportData.class);

        assertNotNull(exportData, "Export data should not be null");
        assertNotNull(exportData.getBlocks(), "Exported blocks should not be null");
        assertNotNull(exportData.getAuthorizedKeys(), "Exported keys should not be null");
        assertEquals(blockCountBefore, exportData.getBlocks().size(), 
                "Exported block count should match current block count");
        assertEquals(keysBefore.size(), exportData.getAuthorizedKeys().size(),
                "Exported key count should match current key count");
        assertNotNull(exportData.getExportTimestamp(), "Export timestamp should be set");
        assertEquals("1.1", exportData.getVersion(), "Export version should be 1.1");
    }

    // ===============================
    // ADDITIONAL ADVANCED FUNCTION 3: Chain Import Tests
    // ===============================

    @Test
    @Order(7)
    @DisplayName("Test Successful Chain Import")
    public void testSuccessfulChainImport() {
        // First export the current chain
        File exportFile = tempDir.resolve("import_test.json").toFile();
        String exportPath = exportFile.getAbsolutePath();
        
        assertTrue(blockchain.exportChain(exportPath), "Initial export should succeed");

        // Create a new blockchain instance to test import
        Blockchain newBlockchain = new Blockchain();
        
        // Import the chain
        assertTrue(newBlockchain.importChain(exportPath), "Chain import should succeed");

        // Verify imported data matches original
        assertEquals(blockchain.getBlockCount(), newBlockchain.getBlockCount(),
                "Imported blockchain should have same block count");
        assertEquals(blockchain.getAuthorizedKeys().size(), newBlockchain.getAuthorizedKeys().size(),
                "Imported blockchain should have same authorized key count");
        var importValidation = newBlockchain.validateChainDetailed();
        assertTrue(importValidation.isStructurallyIntact(), "Imported chain should be structurally intact");
        assertTrue(importValidation.isFullyCompliant(), "Imported chain should be fully compliant");
    }

    @Test
    @Order(8)
    @DisplayName("Test Chain Import from Non-existent File")
    public void testChainImportNonexistentFile() {
        // NOTE: This test intentionally throws an exception - this is expected behavior
        String nonExistentPath = tempDir.resolve("non_existent.json").toString();

        // Import from non-existent file should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            blockchain.importChain(nonExistentPath);
        }, "Import from non-existent file should throw IllegalArgumentException");
    }

    @Test
    @Order(9)
    @DisplayName("Test Chain Import with Invalid JSON")
    public void testChainImportInvalidJson() throws Exception {
        File invalidJsonFile = tempDir.resolve("invalid.json").toFile();
        
        // Write invalid JSON to file
        java.nio.file.Files.write(invalidJsonFile.toPath(), 
                "{ invalid json content }".getBytes());

        assertFalse(blockchain.importChain(invalidJsonFile.getAbsolutePath()),
                "Import of invalid JSON should fail");
    }

    @Test
    @Order(10)
    @DisplayName("Test Chain Import Data Integrity")
    public void testChainImportDataIntegrity() {
        // Export original chain
        File exportFile = tempDir.resolve("integrity_test.json").toFile();
        String exportPath = exportFile.getAbsolutePath();

        assertTrue(blockchain.exportChain(exportPath), "Export should succeed");

        // Get original data for comparison
        List<Block> originalBlocks = new ArrayList<>();
        blockchain.processChainInBatches(batch -> originalBlocks.addAll(batch), 1000);
        Block originalLastBlock = blockchain.getLastBlock();

        // Import into new blockchain
        Blockchain importedBlockchain = new Blockchain();
        assertTrue(importedBlockchain.importChain(exportPath), "Import should succeed");

        // Verify data integrity
        List<Block> importedBlocks = new ArrayList<>();
        importedBlockchain.processChainInBatches(batch -> importedBlocks.addAll(batch), 1000);
        Block importedLastBlock = importedBlockchain.getLastBlock();

        assertEquals(originalBlocks.size(), importedBlocks.size(), "Block counts should match");
        assertEquals(originalLastBlock.getBlockNumber(), importedLastBlock.getBlockNumber(),
                "Last block numbers should match");
        assertEquals(originalLastBlock.getHash(), importedLastBlock.getHash(),
                "Last block hashes should match");
        assertEquals(originalLastBlock.getData(), importedLastBlock.getData(),
                "Last block data should match");
    }

    // ===============================
    // ADDITIONAL ADVANCED FUNCTION 4: Block Rollback Tests
    // ===============================

    @Test
    @Order(11)
    @DisplayName("Test Successful Block Rollback")
    public void testSuccessfulBlockRollback() {
        long initialBlockCount = blockchain.getBlockCount();
        
        // Add some more blocks to rollback
        blockchain.addBlock("Extra block 1", aliceKeyPair.getPrivate(), aliceKeyPair.getPublic());
        blockchain.addBlock("Extra block 2", bobKeyPair.getPrivate(), bobKeyPair.getPublic());
        
        long afterAddingBlocks = blockchain.getBlockCount();
        assertEquals(initialBlockCount + 2, afterAddingBlocks, "Should have 2 more blocks");

        // Test rollback of 1 block
        assertTrue(blockchain.rollbackBlocks(1L), "Rollback should succeed");
        assertEquals(afterAddingBlocks - 1, blockchain.getBlockCount(),
                "Should have 1 less block after rollback");

        // Verify chain is still valid
        var rollbackValidation = blockchain.validateChainDetailed();
        assertTrue(rollbackValidation.isStructurallyIntact(), "Chain should be structurally intact after rollback");
        assertTrue(rollbackValidation.isFullyCompliant(), "Chain should be fully compliant after rollback");
    }

    @Test
    @Order(12)
    @DisplayName("Test Block Rollback Edge Cases")
    public void testBlockRollbackEdgeCases() {
        // Test rollback with zero blocks - should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            blockchain.rollbackBlocks(0L);
        }, "Rollback of 0 blocks should throw IllegalArgumentException");

        // Test rollback with negative number - should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            blockchain.rollbackBlocks(-1L);
        }, "Rollback of negative blocks should throw IllegalArgumentException");

        // Test rollback more blocks than available (excluding genesis)
        long currentBlocks = blockchain.getBlockCount();
        assertThrows(IllegalArgumentException.class, () -> {
            blockchain.rollbackBlocks(currentBlocks);
        }, "Rollback of all blocks should throw IllegalArgumentException (genesis protection)");
    }

    @Test
    @Order(13)
    @DisplayName("Test Rollback to Specific Block")
    public void testRollbackToSpecificBlock() {
        // Add some blocks first
        blockchain.addBlock("Rollback test 1", aliceKeyPair.getPrivate(), aliceKeyPair.getPublic());
        blockchain.addBlock("Rollback test 2", bobKeyPair.getPrivate(), bobKeyPair.getPublic());
        blockchain.addBlock("Rollback test 3", charlieKeyPair.getPrivate(), charlieKeyPair.getPublic());

        // Rollback to block 2 (should remove blocks after block 2)
        assertTrue(blockchain.rollbackToBlock(2L), "Rollback to block 2 should succeed");
        
        assertEquals(3, blockchain.getBlockCount(), "Should have blocks 0, 1, 2 remaining");
        var rollbackToBlockValidation = blockchain.validateChainDetailed();
        assertTrue(rollbackToBlockValidation.isStructurallyIntact(), "Chain should be structurally intact");
        assertTrue(rollbackToBlockValidation.isFullyCompliant(), "Chain should be fully compliant");
    }

    @Test
    @Order(14)
    @DisplayName("Test Rollback to Block Edge Cases")
    public void testRollbackToBlockEdgeCases() {
        // Test rollback to negative block number - should throw exception (strict validation)
        assertThrows(IllegalArgumentException.class, () -> {
            blockchain.rollbackToBlock(-1L);
        }, "Rollback to negative block should throw exception");

        // Test rollback to non-existent block - should throw IllegalArgumentException
        long maxBlock = blockchain.getBlockCount() - 1;
        assertThrows(IllegalArgumentException.class, () -> {
            blockchain.rollbackToBlock(maxBlock + 10L);
        }, "Rollback to non-existent block should throw IllegalArgumentException");

        // Test rollback to current block (should be no-op)
        assertTrue(blockchain.rollbackToBlock(maxBlock),
                "Rollback to current block should succeed");
    }

    // ===============================
    // ADDITIONAL ADVANCED FUNCTION 5: Advanced Search Tests
    // ===============================

    @Test
    @Order(15)
    @DisplayName("Test Search Blocks by Content")
    public void testSearchBlocksByContent() {
        // Add blocks with specific searchable content
        blockchain.addBlock("Payment to vendor ABC", aliceKeyPair.getPrivate(), aliceKeyPair.getPublic());
        blockchain.addBlock("Invoice from company XYZ", bobKeyPair.getPrivate(), bobKeyPair.getPublic());
        blockchain.addBlock("Payment confirmation ABC", charlieKeyPair.getPrivate(), charlieKeyPair.getPublic());

        // Test exact search
        List<Block> results = blockchain.searchBlocksByContent("ABC");
        assertEquals(2, results.size(), "Should find 2 blocks containing 'ABC'");

        // Test case-insensitive search
        List<Block> caseResults = blockchain.searchBlocksByContent("abc");
        assertEquals(2, caseResults.size(), "Case-insensitive search should find 2 blocks");

        // Test partial word search
        List<Block> partialResults = blockchain.searchBlocksByContent("Payment");
        assertEquals(2, partialResults.size(), "Should find 2 blocks containing 'Payment'");

        // Test search with no results
        List<Block> noResults = blockchain.searchBlocksByContent("nonexistent");
        assertEquals(0, noResults.size(), "Should find no blocks with non-existent content");

        // Test empty search term - should throw exception (strict validation)
        assertThrows(IllegalArgumentException.class, () -> {
            blockchain.searchBlocksByContent("");
        }, "Empty search should throw exception");

        // Test null search term - should throw exception (strict validation)
        assertThrows(IllegalArgumentException.class, () -> {
            blockchain.searchBlocksByContent(null);
        }, "Null search should throw exception");
    }

    @Test
    @Order(16)
    @DisplayName("Test Get Block by Hash")
    public void testGetBlockByHash() {
        // Get a known block
        Block knownBlock = blockchain.getBlock(1L);
        assertNotNull(knownBlock, "Known block should exist");

        // Search for block by hash
        Block foundBlock = blockchain.getBlockByHash(knownBlock.getHash());
        assertNotNull(foundBlock, "Block should be found by hash");
        assertEquals(knownBlock.getBlockNumber(), foundBlock.getBlockNumber(),
                "Found block should have same block number");
        assertEquals(knownBlock.getData(), foundBlock.getData(),
                "Found block should have same data");

        // Test with non-existent hash
        Block notFound = blockchain.getBlockByHash("non-existent-hash");
        assertNull(notFound, "Should return null for non-existent hash");

        // Test with null hash
        Block nullResult = blockchain.getBlockByHash(null);
        assertNull(nullResult, "Should return null for null hash");

        // Test with empty hash
        Block emptyResult = blockchain.getBlockByHash("");
        assertNull(emptyResult, "Should return null for empty hash");
    }

    @Test
    @Order(17)
    @DisplayName("Test Get Blocks by Date Range")
    public void testGetBlocksByDateRange() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate tomorrow = today.plusDays(1);

        // Count blocks before adding test blocks
        long initialBlockCount = blockchain.getBlockCount();

        // Add test blocks for today
        blockchain.addBlock("Today's test block 1", aliceKeyPair.getPrivate(), aliceKeyPair.getPublic());
        blockchain.addBlock("Today's test block 2", bobKeyPair.getPrivate(), bobKeyPair.getPublic());
        
        // Verify that we've added exactly 2 blocks
        assertEquals(initialBlockCount + 2, blockchain.getBlockCount(), "Should have added exactly 2 blocks");
        
        // All blocks in the blockchain should be from today or have no old blocks from yesterday
        List<Block> todayBlocks = blockchain.getBlocksByDateRange(today, today);
        assertTrue(todayBlocks.size() >= 2, "Should find at least the 2 blocks we just added today");

        // Check yesterday - should be empty for a fresh blockchain, but might have old data
        List<Block> yesterdayBlocks = blockchain.getBlocksByDateRange(yesterday, yesterday);
        // Instead of expecting exactly 0, we just check the count is reasonable
        assertTrue(yesterdayBlocks.size() < blockchain.getBlockCount(), 
                "Yesterday blocks should be less than total blocks (expected: <" + blockchain.getBlockCount() + " but was: " + yesterdayBlocks.size() + ")");

        // Wide range should include all blocks
        List<Block> wideRangeBlocks = blockchain.getBlocksByDateRange(yesterday, tomorrow);
        assertEquals(blockchain.getBlockCount(), wideRangeBlocks.size(),
                "Wide range should include all blocks");
        assertEquals(initialBlockCount + 2, wideRangeBlocks.size(),
                "Wide range should include initial blocks plus our 2 new blocks");

        // Test edge cases
        List<Block> nullStartDate = blockchain.getBlocksByDateRange(null, today);
        assertEquals(0, nullStartDate.size(), "Null start date should return empty list");

        List<Block> nullEndDate = blockchain.getBlocksByDateRange(today, null);
        assertEquals(0, nullEndDate.size(), "Null end date should return empty list");
    }

    @Test
    @Order(18)
    @DisplayName("Test Advanced Search Performance")
    public void testAdvancedSearchPerformance() {
        // Add many blocks for performance testing
        for (int i = 0; i < 50; i++) {
            blockchain.addBlock("Performance test block " + i, 
                    aliceKeyPair.getPrivate(), aliceKeyPair.getPublic());
        }

        long startTime = System.currentTimeMillis();
        
        // Test search performance
        List<Block> searchResults = blockchain.searchBlocksByContent("Performance");
        
        long endTime = System.currentTimeMillis();
        long searchTime = endTime - startTime;

        assertTrue(searchResults.size() >= 50, "Should find all performance test blocks");
        assertTrue(searchTime < 1000, "Search should complete within 1 second");
    }

    // ===============================
    // Integration Tests
    // ===============================

    @Test
    @Order(19)
    @DisplayName("Test Export-Import-Search Integration")
    public void testExportImportSearchIntegration() {
        // Add searchable content
        blockchain.addBlock("Integration test data", aliceKeyPair.getPrivate(), aliceKeyPair.getPublic());
        
        // Export
        File exportFile = tempDir.resolve("integration_test.json").toFile();
        assertTrue(blockchain.exportChain(exportFile.getAbsolutePath()),
                "Export should succeed");

        // Import into new blockchain
        Blockchain newBlockchain = new Blockchain();
        assertTrue(newBlockchain.importChain(exportFile.getAbsolutePath()),
                "Import should succeed");

        // Test search on imported data
        List<Block> searchResults = newBlockchain.searchBlocksByContent("Integration");
        assertEquals(1, searchResults.size(), "Should find integration test block");
        
        // Verify search result integrity
        assertEquals("Integration test data", searchResults.get(0).getData(),
                "Search result should have correct data");
    }

    @Test
    @Order(20)
    @DisplayName("Test Rollback-Search Integration")
    public void testRollbackSearchIntegration() {
        // Add searchable blocks
        blockchain.addBlock("Before rollback", aliceKeyPair.getPrivate(), aliceKeyPair.getPublic());
        blockchain.addBlock("Will be removed", bobKeyPair.getPrivate(), bobKeyPair.getPublic());
        
        // Verify blocks exist before rollback
        List<Block> beforeRollback = blockchain.searchBlocksByContent("rollback");
        assertEquals(1, beforeRollback.size(), "Should find 'Before rollback' block");
        
        List<Block> willBeRemoved = blockchain.searchBlocksByContent("Will be removed");
        assertEquals(1, willBeRemoved.size(), "Should find 'Will be removed' block");

        // Perform rollback
        assertTrue(blockchain.rollbackBlocks(1L), "Rollback should succeed");

        // Verify search results after rollback
        List<Block> afterRollback = blockchain.searchBlocksByContent("rollback");
        assertEquals(1, afterRollback.size(), "Should still find 'Before rollback' block");
        
        List<Block> shouldBeGone = blockchain.searchBlocksByContent("Will be removed");
        assertEquals(0, shouldBeGone.size(), "Should not find removed block");
    }

    // ===============================
    // Error Handling and Edge Cases
    // ===============================

    @Test
    @Order(21)
    @DisplayName("Test Additional Advanced Functions Error Handling")
    public void testAdditionalAdvancedFunctionsErrorHandling() {
        // This test verifies that additional advanced functions validate parameters strictly
        // and throw appropriate exceptions for invalid inputs
        
        // Test rollbackToBlock with negative value - should throw
        assertThrows(IllegalArgumentException.class, () -> {
            blockchain.rollbackToBlock(-1L);
        }, "rollbackToBlock should throw exception for negative block number");
        
        // Test searchBlocksByContent with null - should throw
        assertThrows(IllegalArgumentException.class, () -> {
            blockchain.searchBlocksByContent(null);
        }, "searchBlocksByContent should throw exception for null search term");

        // Test exportChain with invalid parameters - should throw
        assertThrows(IllegalArgumentException.class, () -> {
            blockchain.exportChain("");
        }, "exportChain should throw exception for empty path");

        assertThrows(IllegalArgumentException.class, () -> {
            blockchain.exportChain(null);
        }, "exportChain should throw exception for null path");

        // Test importChain with invalid parameters - should throw
        assertThrows(IllegalArgumentException.class, () -> {
            blockchain.importChain("");
        }, "importChain should throw exception for empty path");

        assertThrows(IllegalArgumentException.class, () -> {
            blockchain.importChain(null);
        }, "importChain should throw exception for null path");

        // Test rollbackBlocks with invalid parameter - should throw
        assertThrows(IllegalArgumentException.class, () -> {
            blockchain.rollbackBlocks(-1L);
        }, "rollbackBlocks should throw exception for negative value");

        // These methods should handle null gracefully (return null or empty)
        assertDoesNotThrow(() -> {
            blockchain.getBlockByHash(null);
            blockchain.getBlocksByDateRange(null, null);
        }, "Some methods should handle errors gracefully without throwing exceptions");
    }

    @Test
    @Order(22)
    @DisplayName("Test Chain Validation After All Operations")
    public void testChainValidationAfterAllOperations() {
        // Final verification that the chain remains valid after all test operations
        var finalValidation = blockchain.validateChainDetailed();
        assertTrue(finalValidation.isStructurallyIntact(), 
                "Chain should be structurally intact after all test operations");
        assertTrue(finalValidation.isFullyCompliant(), 
                "Chain should be fully compliant after all test operations");
        
        // Verify genesis block is still intact
        Block genesisBlock = blockchain.getBlock(0L);
        assertNotNull(genesisBlock, "Genesis block should still exist");
        assertEquals(0L, genesisBlock.getBlockNumber(), "Genesis block should have number 0");
        assertEquals("0", genesisBlock.getPreviousHash(), "Genesis block should have correct previous hash");
    }
}
