package com.rbatllet.blockchain.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.entity.OffChainData;
import com.rbatllet.blockchain.security.UserRole;
import com.rbatllet.blockchain.service.OffChainStorageService;
import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.util.TestGenesisKeyManager;

/**
 * Test suite for off-chain storage functionality
 * Uses resource locks to ensure tests that modify configuration don't run in parallel
 */
@ResourceLock("blockchain-config") // Prevents parallel execution with other tests using same lock
public class OffChainStorageTest {
    private static final Logger logger = LoggerFactory.getLogger(OffChainStorageTest.class);

    private Blockchain blockchain;
    private OffChainStorageService offChainService;
    private KeyPair bootstrapKeyPair;
    private KeyPair testKeyPair;
    private PrivateKey testPrivateKey;
    private PublicKey testPublicKey;
    private String testPublicKeyString;

    @BeforeEach
    void setUp() throws Exception {
        blockchain = new Blockchain();
        offChainService = new OffChainStorageService();

        // Ensure clean state for each test
        blockchain.clearAndReinitialize();

        // Load bootstrap admin keys (created automatically)
        bootstrapKeyPair = TestGenesisKeyManager.ensureGenesisKeysExist();

        // Register bootstrap admin in blockchain (RBAC v1.0.6)
        blockchain.createBootstrapAdmin(
            CryptoUtil.publicKeyToString(bootstrapKeyPair.getPublic()),
            "BOOTSTRAP_ADMIN"
        );

        // Generate test key pair
        testKeyPair = CryptoUtil.generateKeyPair();
        testPrivateKey = testKeyPair.getPrivate();
        testPublicKey = testKeyPair.getPublic();
        testPublicKeyString = CryptoUtil.publicKeyToString(testPublicKey);

        // Add authorized key to blockchain
        blockchain.addAuthorizedKey(testPublicKeyString, "TestUser", bootstrapKeyPair, UserRole.USER);

        // Show initial blockchain state with detailed validation
        logger.info("🔍 Initial blockchain state:");
        blockchain.validateChainDetailed();
    }
    
    @AfterEach
    void cleanUp() {
        // CRITICAL: Reset configuration to defaults to avoid contaminating other tests
        blockchain.resetLimitsToDefault();
        
        // Clean up off-chain data directory
        try {
            File offChainDir = new File("off-chain-data");
            if (offChainDir.exists()) {
                File[] files = offChainDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        file.delete();
                    }
                }
                offChainDir.delete();
            }
        } catch (Exception e) {
            logger.error("Cleanup error: " + e.getMessage());
        }
    }
    
    @Test
    @DisplayName("Test block size validation and storage decision")
    void testBlockSizeValidation() {
        // Test small data (should store on-chain)
        String smallData = "Small data for on-chain storage";
        assertEquals(1, blockchain.validateAndDetermineStorage(smallData));
        
        // Test large data (should store off-chain)
        // Create data between 512KB and 1MB to go off-chain
        String largeData = "a".repeat(600 * 1024); // 600KB exactly
        assertEquals(2, blockchain.validateAndDetermineStorage(largeData));
        
        // Test invalid data
        assertEquals(0, blockchain.validateAndDetermineStorage(null));
        
        // Test empty data (should store on-chain)
        assertEquals(1, blockchain.validateAndDetermineStorage(""));
    }
    
    @Test
    @DisplayName("Test off-chain data storage and retrieval")
    void testOffChainDataStorageAndRetrieval() throws Exception {
        String largeData = generateLargeTestData(600 * 1024); // 600KB data
        
        // Store data off-chain
        OffChainData offChainData = offChainService.storeData(
            largeData.getBytes(),
            "testPassword123",
            testPrivateKey,
            testPublicKeyString,
            "text/plain"
        );
        
        assertNotNull(offChainData);
        assertNotNull(offChainData.getDataHash());
        assertNotNull(offChainData.getSignature());
        assertNotNull(offChainData.getFilePath());
        assertTrue(offChainData.getFileSize() > 0);
        
        // Verify file was created
        assertTrue(Files.exists(Paths.get(offChainData.getFilePath())));
        
        // Retrieve and verify data
        byte[] retrievedData = offChainService.retrieveData(offChainData, "testPassword123");
        String retrievedString = new String(retrievedData);
        
        assertEquals(largeData, retrievedString);
        
        // Test integrity verification
        assertTrue(offChainService.verifyIntegrity(offChainData, "testPassword123"));
        
        // Test with wrong password (should fail)
        assertThrows(Exception.class, () -> {
            offChainService.retrieveData(offChainData, "wrongPassword");
        });
    }
    
    @Test
    @DisplayName("Test blockchain integration with off-chain storage")
    void testBlockchainOffChainIntegration() throws Exception {
        String largeData = generateLargeTestData(600 * 1024); // 600KB data
        
        // Add block with large data (should trigger off-chain storage)
        Block block = blockchain.addBlockAndReturn(largeData, testPrivateKey, testPublicKey);
        
        assertNotNull(block);
        assertTrue(block.hasOffChainData());
        assertNotNull(block.getOffChainData());
        
        // Block data should contain reference, not original data
        assertTrue(block.getData().startsWith("OFF_CHAIN_REF:"));
        
        // Retrieve complete data
        String completeData = blockchain.getCompleteBlockData(block);
        assertEquals(largeData, completeData);
        
        // Verify off-chain integrity
        assertTrue(blockchain.verifyOffChainIntegrity(block));
        
        // Test integrity check for all blocks
        assertTrue(blockchain.verifyAllOffChainIntegrity());
        
        // Show detailed validation with off-chain data analysis
        logger.info("🔍 Detailed validation after off-chain integration:");
        blockchain.validateChainDetailed();
    }
    
    @Test
    @DisplayName("Test dynamic configuration")
    void testDynamicConfiguration() {
        // Test initial configuration
        String config = blockchain.getConfigurationSummary();
        assertNotNull(config);
        assertTrue(config.contains("10,485,760 bytes")); // 10MB default

        // Test setting new limits
        blockchain.setMaxBlockSizeBytes(2 * 1024 * 1024); // 2MB
        blockchain.setOffChainThresholdBytes(1024 * 1024); // 1MB threshold

        assertEquals(2 * 1024 * 1024, blockchain.getCurrentMaxBlockSizeBytes());
        assertEquals(1024 * 1024, blockchain.getCurrentOffChainThresholdBytes());

        // Test reset to defaults
        blockchain.resetLimitsToDefault();
        assertEquals(10 * 1024 * 1024, blockchain.getCurrentMaxBlockSizeBytes());
        assertEquals(512 * 1024, blockchain.getCurrentOffChainThresholdBytes());

        // Test invalid configurations
        assertThrows(IllegalArgumentException.class, () -> {
            blockchain.setMaxBlockSizeBytes(-1);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            blockchain.setOffChainThresholdBytes(0);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            blockchain.setOffChainThresholdBytes(100 * 1024 * 1024); // Too large
        });
    }
    
    @Test
    @DisplayName("Test on-chain storage still works for small data")
    void testOnChainStorageStillWorks() throws Exception {
        String smallData = "This is small data that should remain on-chain";
        
        // Add block with small data
        Block block = blockchain.addBlockAndReturn(smallData, testPrivateKey, testPublicKey);
        
        assertNotNull(block);
        assertFalse(block.hasOffChainData());
        assertNull(block.getOffChainData());
        assertEquals(smallData, block.getData());
        
        // Complete data should be same as block data
        String completeData = blockchain.getCompleteBlockData(block);
        assertEquals(smallData, completeData);
    }
    
    @Test
    @DisplayName("Test file cleanup after deletion")
    void testFileCleanup() throws Exception {
        String largeData = generateLargeTestData(600 * 1024);
        
        // Store data off-chain
        OffChainData offChainData = offChainService.storeData(
            largeData.getBytes(),
            "testPassword123",
            testPrivateKey,
            testPublicKeyString,
            "text/plain"
        );
        
        // Verify file exists
        assertTrue(offChainService.fileExists(offChainData));
        
        // Delete data
        assertTrue(offChainService.deleteData(offChainData));
        
        // Verify file is deleted
        assertFalse(offChainService.fileExists(offChainData));
    }
    
    @Test
    @DisplayName("Test data consistency during rollback operations")
    void testDataConsistencyDuringRollback() throws Exception {
        String largeData1 = generateLargeTestData(600 * 1024);
        String largeData2 = generateLargeTestData(700 * 1024);
        String smallData = "Small data for on-chain storage";
        
        // Add multiple blocks with different storage types
        Block block1 = blockchain.addBlockAndReturn(smallData, testPrivateKey, testPublicKey);
        Block block2 = blockchain.addBlockAndReturn(largeData1, testPrivateKey, testPublicKey);
        Block block3 = blockchain.addBlockAndReturn(largeData2, testPrivateKey, testPublicKey);
        Block block4 = blockchain.addBlockAndReturn(smallData, testPrivateKey, testPublicKey);
        
        // Verify initial state
        assertFalse(block1.hasOffChainData());
        assertTrue(block2.hasOffChainData());
        assertTrue(block3.hasOffChainData());
        assertFalse(block4.hasOffChainData());
        
        // Store file paths for verification
        String file2Path = block2.getOffChainData().getFilePath();
        String file3Path = block3.getOffChainData().getFilePath();
        
        // Verify files exist
        assertTrue(new File(file2Path).exists());
        assertTrue(new File(file3Path).exists());
        
        // Rollback 2 blocks (should delete blocks 3 and 4, including off-chain file for block 3)
        assertTrue(blockchain.rollbackBlocks(2L));
        
        // Verify block 3's off-chain file is deleted
        assertFalse(new File(file3Path).exists());
        
        // Verify block 2's off-chain file still exists
        assertTrue(new File(file2Path).exists());
        
        // Verify blockchain state
        assertEquals(2, blockchain.getBlockCount() - 1); // -1 for genesis block
        assertNotNull(blockchain.getBlock(block2.getBlockNumber()));
        assertNull(blockchain.getBlock(block3.getBlockNumber()));
        assertNull(blockchain.getBlock(block4.getBlockNumber()));
    }
    
    @Test
    @DisplayName("Test data consistency during export/import operations")
    void testDataConsistencyDuringExportImport() throws Exception {
        String largeData = generateLargeTestData(600 * 1024);
        String smallData = "Small data for testing";
        
        // Add blocks with different storage types
        Block smallBlock = blockchain.addBlockAndReturn(smallData, testPrivateKey, testPublicKey);
        Block largeBlock = blockchain.addBlockAndReturn(largeData, testPrivateKey, testPublicKey);
        
        // Verify initial state
        assertFalse(smallBlock.hasOffChainData());
        assertTrue(largeBlock.hasOffChainData());
        
        String originalOffChainFile = largeBlock.getOffChainData().getFilePath();
        assertTrue(new File(originalOffChainFile).exists());
        
        // Export chain
        String exportPath = "test-export.json";
        assertTrue(blockchain.exportChain(exportPath));
        
        // Verify export includes off-chain backup
        File exportFile = new File(exportPath);
        File backupDir = new File(exportFile.getParent(), "off-chain-backup");
        assertTrue(exportFile.exists());
        assertTrue(backupDir.exists());
        assertTrue(backupDir.listFiles().length > 0);
        
        // Clear and reinitialize blockchain
        blockchain.clearAndReinitialize();
        
        // Verify original off-chain file was deleted
        assertFalse(new File(originalOffChainFile).exists());
        
        // Import chain
        assertTrue(blockchain.importChain(exportPath));
        
        // Verify data integrity after import
        Block importedSmallBlock = blockchain.getBlock(smallBlock.getBlockNumber());
        Block importedLargeBlock = blockchain.getBlock(largeBlock.getBlockNumber());
        
        assertNotNull(importedSmallBlock);
        assertNotNull(importedLargeBlock);
        assertFalse(importedSmallBlock.hasOffChainData());
        assertTrue(importedLargeBlock.hasOffChainData());
        
        // Verify imported off-chain data is accessible
        String importedCompleteData = blockchain.getCompleteBlockData(importedLargeBlock);
        assertEquals(largeData, importedCompleteData);
        
        // Verify new off-chain file exists
        String newOffChainFile = importedLargeBlock.getOffChainData().getFilePath();
        assertTrue(new File(newOffChainFile).exists());
        
        // Cleanup test files
        try {
            exportFile.delete();
            File[] backupFiles = backupDir.listFiles();
            if (backupFiles != null) {
                for (File file : backupFiles) {
                    file.delete();
                }
            }
            backupDir.delete();
        } catch (Exception e) {
            logger.error("Cleanup warning: " + e.getMessage());
        }
    }
    
    @Test
    @DisplayName("Test orphaned files cleanup")
    void testOrphanedFilesCleanup() throws Exception {
        String largeData = generateLargeTestData(600 * 1024);
        
        // Create a block with off-chain data
        Block block = blockchain.addBlockAndReturn(largeData, testPrivateKey, testPublicKey);
        assertTrue(block.hasOffChainData());
        
        String offChainFilePath = block.getOffChainData().getFilePath();
        assertTrue(new File(offChainFilePath).exists());
        
        // Clean up any existing orphaned files first to ensure clean state
        blockchain.cleanupOrphanedFiles();
        
        // Create some orphaned files manually
        File offChainDir = new File("off-chain-data");
        File orphanedFile1 = new File(offChainDir, "orphaned_test_file_1.dat");
        File orphanedFile2 = new File(offChainDir, "orphaned_test_file_2.enc");
        
        orphanedFile1.createNewFile();
        orphanedFile2.createNewFile();
        
        assertTrue(orphanedFile1.exists());
        assertTrue(orphanedFile2.exists());
        
        // Run orphaned files cleanup
        int cleanedFiles = blockchain.cleanupOrphanedFiles();
        
        // Verify orphaned files were deleted
        assertFalse(orphanedFile1.exists());
        assertFalse(orphanedFile2.exists());
        assertEquals(2, cleanedFiles);
        
        // Verify valid off-chain file still exists
        assertTrue(new File(offChainFilePath).exists());
        
        // Verify block data is still accessible
        String retrievedData = blockchain.getCompleteBlockData(block);
        assertEquals(largeData, retrievedData);
    }
    
    @Test
    @DisplayName("Test clearAndReinitialize cleans up all off-chain data")
    void testClearAndReinitializeCleanup() throws Exception {
        String largeData1 = generateLargeTestData(600 * 1024);
        String largeData2 = generateLargeTestData(700 * 1024);
        
        // Add multiple blocks with off-chain data
        Block block1 = blockchain.addBlockAndReturn(largeData1, testPrivateKey, testPublicKey);
        Block block2 = blockchain.addBlockAndReturn(largeData2, testPrivateKey, testPublicKey);
        
        assertTrue(block1.hasOffChainData());
        assertTrue(block2.hasOffChainData());
        
        String file1Path = block1.getOffChainData().getFilePath();
        String file2Path = block2.getOffChainData().getFilePath();
        
        assertTrue(new File(file1Path).exists());
        assertTrue(new File(file2Path).exists());
        
        // Clear and reinitialize
        blockchain.clearAndReinitialize();
        
        // Verify all off-chain files are deleted
        assertFalse(new File(file1Path).exists());
        assertFalse(new File(file2Path).exists());
        
        // Verify blockchain is reinitialized with only genesis block
        assertEquals(1, blockchain.getBlockCount());
        assertNotNull(blockchain.getBlock(0L)); // Genesis block
    }
    
    @Test
    @DisplayName("Test attachOffChainDataToBlock method")
    void testAttachOffChainDataToBlock() throws Exception {
        String largeData = generateLargeTestData(600 * 1024);
        String smallData = "Small on-chain data";

        // Create off-chain data first
        OffChainData offChainData = offChainService.storeData(
            largeData.getBytes(),
            "testPassword123",
            testPrivateKey,
            testPublicKeyString,
            "text/plain"
        );

        // Create block WITHOUT off-chain data (encrypted block)
        Block block = blockchain.addEncryptedBlockWithKeywords(
            smallData,
            "testPassword123",
            new String[]{"test", "attach"},
            "TEST",
            testPrivateKey,
            testPublicKey
        );

        assertNotNull(block);
        assertFalse(block.hasOffChainData(), "Block should not have off-chain data initially");

        // Now attach off-chain data using new method
        boolean success = blockchain.attachOffChainDataToBlock(block.getBlockNumber(), offChainData);

        assertTrue(success, "Attaching off-chain data should succeed");

        // Retrieve the block again to verify persistence
        Block updatedBlock = blockchain.getBlock(block.getBlockNumber());
        assertNotNull(updatedBlock);
        assertTrue(updatedBlock.hasOffChainData(), "Block should now have off-chain data");
        assertNotNull(updatedBlock.getOffChainData());
        assertEquals(offChainData.getDataHash(), updatedBlock.getOffChainData().getDataHash());

        // Verify we can retrieve the complete data
        String completeData = blockchain.getCompleteBlockData(updatedBlock);
        assertNotNull(completeData);
        // The block's on-chain data should still be the encrypted small data
        assertNotEquals(largeData, completeData);

        // Verify off-chain file exists
        assertTrue(offChainService.fileExists(updatedBlock.getOffChainData()));

        // Test error cases
        assertThrows(IllegalArgumentException.class, () -> {
            blockchain.attachOffChainDataToBlock(null, offChainData);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            blockchain.attachOffChainDataToBlock(block.getBlockNumber(), null);
        });

        // Test attaching to non-existent block
        boolean failResult = blockchain.attachOffChainDataToBlock(999999L, offChainData);
        assertFalse(failResult, "Attaching to non-existent block should fail");
    }

    /**
     * Helper method to generate large test data
     */
    private String generateLargeTestData(int sizeBytes) {
        StringBuilder sb = new StringBuilder();
        String pattern = "This is test data for off-chain storage testing. ";

        while (sb.length() < sizeBytes) {
            sb.append(pattern);
        }

        return sb.substring(0, sizeBytes);
    }
}