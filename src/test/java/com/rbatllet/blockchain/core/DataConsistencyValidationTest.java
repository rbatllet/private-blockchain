package com.rbatllet.blockchain.core;

import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.security.UserRole;
import com.rbatllet.blockchain.util.TestGenesisKeyManager;
import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.util.JPAUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.File;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

import static org.junit.jupiter.api.Assertions.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Validation test for data consistency fixes
 */
public class DataConsistencyValidationTest {
    private static final Logger logger = LoggerFactory.getLogger(DataConsistencyValidationTest.class);


    private Blockchain blockchain;
    private KeyPair bootstrapKeyPair;
    private KeyPair testKeyPair;
    private PrivateKey testPrivateKey;
    private PublicKey testPublicKey;
    private String testPublicKeyString;

    @BeforeAll
    static void setUpDatabase() {
        // Initialize JPAUtil with default configuration (respects environment variables)
        JPAUtil.initializeDefault();
    }

    @BeforeEach
    void setUp() throws Exception {
        blockchain = new Blockchain();

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
    }
    
    @AfterEach
    void cleanUp() {
        // Clean blockchain and thread locals
        if (blockchain != null) {
            blockchain.clearAndReinitialize();
            JPAUtil.cleanupThreadLocals();
        }

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
    @DisplayName("Test basic rollback data consistency")
    void testBasicRollbackDataConsistency() throws Exception {
        // Generate large data for off-chain storage
        String largeData = generateLargeTestData(600 * 1024); // 600KB
        String smallData = "Small data for on-chain storage";
        
        // Add blocks
        Block block1 = blockchain.addBlockAndReturn(smallData, testPrivateKey, testPublicKey);
        Block block2 = blockchain.addBlockAndReturn(largeData, testPrivateKey, testPublicKey);
        
        // Verify initial state
        assertNotNull(block1);
        assertNotNull(block2);
        assertEquals(3, blockchain.getBlockCount()); // Genesis + 2 blocks
        
        if (block2 != null && block2.hasOffChainData()) {
            String offChainFilePath = block2.getOffChainData().getFilePath();
            assertTrue(new File(offChainFilePath).exists(), "Off-chain file should exist");
            
            // Rollback 1 block (should remove block2 and its off-chain file)
            boolean rollbackSuccess = blockchain.rollbackBlocks(1L);
            assertTrue(rollbackSuccess, "Rollback should succeed");
            
            // Verify off-chain file was deleted
            assertFalse(new File(offChainFilePath).exists(), 
                "Off-chain file should be deleted after rollback");
            
            // Verify blockchain state
            assertEquals(2, blockchain.getBlockCount()); // Genesis + 1 block
            assertNotNull(blockchain.getBlock(block1.getBlockNumber()));
            assertNull(blockchain.getBlock(block2.getBlockNumber()));
            
            // Show detailed validation after rollback
            logger.info("🔍 Detailed validation after rollback with off-chain cleanup:");
            blockchain.validateChainDetailed();
        }
    }
    
    @Test
    @DisplayName("Test orphaned files cleanup")
    void testOrphanedFilesCleanup() throws Exception {
        // Create off-chain directory if it doesn't exist
        File offChainDir = new File("off-chain-data");
        if (!offChainDir.exists()) {
            offChainDir.mkdirs();
        }
        
        // Create some orphaned files manually
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
    }
    
    @Test
    @DisplayName("Test export includes off-chain files")
    void testExportIncludesOffChainFiles() throws Exception {
        // Generate large data for off-chain storage
        String largeData = generateLargeTestData(600 * 1024); // 600KB
        
        // Add block with off-chain data
        Block block = blockchain.addBlockAndReturn(largeData, testPrivateKey, testPublicKey);
        
        if (block != null && block.hasOffChainData()) {
            // Export chain
            String exportPath = "test-export-validation.json";
            boolean exportSuccess = blockchain.exportChain(exportPath);
            assertTrue(exportSuccess, "Export should succeed");
            
            // Verify export file exists
            File exportFile = new File(exportPath);
            assertTrue(exportFile.exists(), "Export file should exist");
            
            // Verify backup directory was created
            File backupDir = new File(exportFile.getParent(), "off-chain-backup");
            assertTrue(backupDir.exists(), "Backup directory should exist");
            
            // Verify backup files exist
            File[] backupFiles = backupDir.listFiles();
            assertNotNull(backupFiles);
            assertTrue(backupFiles.length > 0, "Should have backup files");
            
            // Cleanup test files
            try {
                exportFile.delete();
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
    }
    
    @Test
    @DisplayName("Test clearAndReinitialize cleanup")
    void testClearAndReinitializeCleanup() throws Exception {
        // Generate large data for off-chain storage  
        String largeData = generateLargeTestData(600 * 1024); // 600KB
        
        // Add block with off-chain data
        Block block = blockchain.addBlockAndReturn(largeData, testPrivateKey, testPublicKey);
        
        if (block != null && block.hasOffChainData()) {
            String offChainFilePath = block.getOffChainData().getFilePath();
            assertTrue(new File(offChainFilePath).exists(), "Off-chain file should exist");
            
            // Clear and reinitialize
            blockchain.clearAndReinitialize();
            
            // Verify off-chain file was deleted
            assertFalse(new File(offChainFilePath).exists(), 
                "Off-chain file should be deleted after clearAndReinitialize");
            
            // Verify blockchain is reinitialized with only genesis block
            assertEquals(1, blockchain.getBlockCount());
            assertNotNull(blockchain.getBlock(0L)); // Genesis block
        }
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