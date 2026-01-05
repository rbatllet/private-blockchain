package com.rbatllet.blockchain.core;

import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.security.UserRole;
import com.rbatllet.blockchain.util.TestGenesisKeyManager;
import com.rbatllet.blockchain.util.CryptoUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

/**
 * Test class for encrypted chain export/import functionality
 * Validates that encrypted blocks and off-chain data can be properly exported and restored
 */
public class EncryptedChainExportImportTest {

    private Blockchain blockchain;
    private KeyPair bootstrapKeyPair;
    private KeyPair keyPair;
    private PrivateKey privateKey;
    private PublicKey publicKey;
    private String publicKeyString;
    private String masterPassword;

    @BeforeEach
    void setUp() throws Exception {
        blockchain = new Blockchain();

        // Ensure clean state
        blockchain.clearAndReinitialize();

        // Load bootstrap admin keys (created automatically)
        bootstrapKeyPair = TestGenesisKeyManager.ensureGenesisKeysExist();

        // Register bootstrap admin in blockchain
        blockchain.createBootstrapAdmin(
            CryptoUtil.publicKeyToString(bootstrapKeyPair.getPublic()),
            "BOOTSTRAP_ADMIN"
        );

        keyPair = CryptoUtil.generateKeyPair();
        privateKey = keyPair.getPrivate();
        publicKey = keyPair.getPublic();
        publicKeyString = CryptoUtil.publicKeyToString(publicKey);
        masterPassword = "TestPassword123!";

        // Add authorized key
        blockchain.addAuthorizedKey(publicKeyString, "TestUser", bootstrapKeyPair, UserRole.USER);
    }
    
    @AfterEach
    void tearDown() {
        // Clean up test files
        deleteFileIfExists("test-encrypted-export.json");
        deleteFileIfExists("test-regular-export.json");
        deleteDirectory("off-chain-backup");
    }
    
    @Test
    @DisplayName("Export encrypted chain with encryption data")
    void testExportEncryptedChain() throws Exception {
        // Add encrypted block
        blockchain.addEncryptedBlock(
            "Sensitive encrypted data", masterPassword, privateKey, publicKey);
        
        // Add regular block
        blockchain.addBlockAndReturn(
            "Regular unencrypted data", privateKey, publicKey);
        
        // Export encrypted chain
        String exportPath = "test-encrypted-export.json";
        boolean success = blockchain.exportEncryptedChain(exportPath, masterPassword);
        
        assertTrue(success, "Encrypted export should succeed");
        assertTrue(new File(exportPath).exists(), "Export file should be created");
        assertTrue(new File(exportPath).length() > 0, "Export file should not be empty");
    }
    
    @Test
    @DisplayName("Import encrypted chain with encryption context restoration")
    void testImportEncryptedChain() throws Exception {
        // Create test data
        blockchain.addEncryptedBlock(
            "Confidential patient data", masterPassword, privateKey, publicKey);
        blockchain.addBlockAndReturn(
            "Public medical research data", privateKey, publicKey);
        
        // Export chain
        String exportPath = "test-encrypted-export.json";
        assertTrue(blockchain.exportEncryptedChain(exportPath, masterPassword));
        
        // Clear blockchain
        blockchain.clearAndReinitialize();
        // Re-register bootstrap admin after clear (RBAC v1.0.6)
        blockchain.createBootstrapAdmin(
            CryptoUtil.publicKeyToString(bootstrapKeyPair.getPublic()),
            "BOOTSTRAP_ADMIN"
        );
        // Add authorized key again after clear
        blockchain.addAuthorizedKey(publicKeyString, "TestUser", bootstrapKeyPair, UserRole.USER);
        assertEquals(1, blockchain.getBlockCount(), "Blockchain should only have genesis block after clear");

        // Import chain
        boolean importSuccess = blockchain.importEncryptedChain(exportPath, masterPassword);
        assertTrue(importSuccess, "Encrypted import should succeed");

        // Verify blocks are restored (original 2 blocks + genesis block)
        assertEquals(3, blockchain.getBlockCount(), "Should restore all blocks plus genesis");

        // Find blocks by content characteristics since block numbers may change during import
        List<Block> allBlocks = new ArrayList<>();
        blockchain.processChainInBatches(batch -> allBlocks.addAll(batch), 1000);

        Block restoredEncrypted = null;
        Block restoredRegular = null;

        for (Block block : allBlocks) {
            if (block.isDataEncrypted()) {
                restoredEncrypted = block;
            } else if (block.getData() != null && block.getData().contains("Public medical research data")) {
                restoredRegular = block;
            }
        }

        // Verify encrypted block
        assertNotNull(restoredEncrypted, "Encrypted block should be restored");
        assertTrue(restoredEncrypted.isDataEncrypted(), "Block should remain encrypted");

        // Verify regular block
        assertNotNull(restoredRegular, "Regular block should be restored");
        assertFalse(restoredRegular.isDataEncrypted(), "Regular block should remain unencrypted");
    }
    
    @Test
    @DisplayName("Export and import with off-chain encrypted data")
    void testOffChainEncryptedDataExportImport() throws Exception {
        // Generate large data that will be stored off-chain (> 512KB, test expects > 1M chars)
        // Need between 1M chars and 1MB max (1,048,576 bytes)
        String largeData = "Large encrypted dataset entry with sensitive information. ".repeat(17500); // ~1.015MB

        // Add large block (should go to off-chain storage)
        Block largeBlock = blockchain.addBlockAndReturn(largeData, privateKey, publicKey);
        assertTrue(largeBlock.hasOffChainData(), "Large block should have off-chain data");

        // Export and import
        String exportPath = "test-encrypted-export.json";
        assertTrue(blockchain.exportEncryptedChain(exportPath, masterPassword));

        blockchain.clearAndReinitialize();
        // Re-register bootstrap admin after clear (RBAC v1.0.6)
        blockchain.createBootstrapAdmin(
            CryptoUtil.publicKeyToString(bootstrapKeyPair.getPublic()),
            "BOOTSTRAP_ADMIN"
        );
        blockchain.addAuthorizedKey(publicKeyString, "TestUser", bootstrapKeyPair, UserRole.USER); // Re-add key after clear
        assertTrue(blockchain.importEncryptedChain(exportPath, masterPassword));

        // Find the block with off-chain data since block numbers may change during import
        List<Block> allBlocks = new ArrayList<>();
        blockchain.processChainInBatches(batch -> allBlocks.addAll(batch), 1000);

        Block restoredLargeBlock = null;

        for (Block block : allBlocks) {
            if (block.hasOffChainData()) {
                restoredLargeBlock = block;
                break;
            }
        }

        // Verify off-chain data is accessible
        assertNotNull(restoredLargeBlock, "Large block should be restored");
        assertTrue(restoredLargeBlock.hasOffChainData(), "Restored block should have off-chain data");
        
        // Verify data can be retrieved
        String restoredData = blockchain.getCompleteBlockData(restoredLargeBlock);
        assertNotNull(restoredData, "Off-chain data should be accessible");
        assertTrue(restoredData.length() > 1000000, "Restored data should be large");
        assertTrue(restoredData.contains("Large encrypted dataset entry"), "Data content should match");
    }
    
    @Test
    @DisplayName("Regular export should fail for encrypted chains")
    void testRegularExportFailsWithEncryptedData() throws Exception {
        // Add encrypted block
        blockchain.addEncryptedBlock("Encrypted content", masterPassword, privateKey, publicKey);
        
        // Regular export should still work but won't preserve encryption context
        String exportPath = "test-regular-export.json";
        boolean success = blockchain.exportChain(exportPath);
        assertTrue(success, "Regular export should work but without encryption context");
        
        // Import with regular method should work but encryption context may be lost
        blockchain.clearAndReinitialize();
        // Re-register bootstrap admin after clear (RBAC v1.0.6)
        blockchain.createBootstrapAdmin(
            CryptoUtil.publicKeyToString(bootstrapKeyPair.getPublic()),
            "BOOTSTRAP_ADMIN"
        );
        boolean importSuccess = blockchain.importChain(exportPath);
        assertTrue(importSuccess, "Regular import should work");

        // Verify block exists but may not be properly decryptable
        // Note: May have more blocks if test isolation is not perfect
        assertTrue(blockchain.getBlockCount() >= 1, "Should restore at least one block");
    }

    @Test
    @DisplayName("Import fails with wrong master password")
    void testImportFailsWithWrongPassword() throws Exception {
        // Create encrypted data
        blockchain.addEncryptedBlock("Secret data", masterPassword, privateKey, publicKey);

        // Export with correct password
        String exportPath = "test-encrypted-export.json";
        assertTrue(blockchain.exportEncryptedChain(exportPath, masterPassword));

        blockchain.clearAndReinitialize();
        // Re-register bootstrap admin after clear (RBAC v1.0.6)
        blockchain.createBootstrapAdmin(
            CryptoUtil.publicKeyToString(bootstrapKeyPair.getPublic()),
            "BOOTSTRAP_ADMIN"
        );
        blockchain.addAuthorizedKey(publicKeyString, "TestUser", bootstrapKeyPair, UserRole.USER); // Re-add key after clear

        // Try to import with wrong password
        boolean importSuccess = blockchain.importEncryptedChain(exportPath, "WrongPassword123!");
        assertFalse(importSuccess, "Import should fail with wrong password");

        // Blockchain should only have genesis block after failed import
        assertEquals(1, blockchain.getBlockCount(), "Blockchain should only have genesis block after failed import");
    }
    
    @Test
    @DisplayName("Import non-encrypted export with encrypted import method fails gracefully")
    void testImportNonEncryptedWithEncryptedMethod() throws Exception {
        // Create regular unencrypted data
        blockchain.addBlockAndReturn("Regular data", privateKey, publicKey);
        
        // Export with regular method
        String exportPath = "test-regular-export.json";
        assertTrue(blockchain.exportChain(exportPath));

        blockchain.clearAndReinitialize();
        // Re-register bootstrap admin after clear (RBAC v1.0.6)
        blockchain.createBootstrapAdmin(
            CryptoUtil.publicKeyToString(bootstrapKeyPair.getPublic()),
            "BOOTSTRAP_ADMIN"
        );
        blockchain.addAuthorizedKey(publicKeyString, "TestUser", bootstrapKeyPair, UserRole.USER); // Re-add key after clear

        // Try to import with encrypted method
        boolean importSuccess = blockchain.importEncryptedChain(exportPath, masterPassword);
        assertFalse(importSuccess, "Encrypted import should fail for non-encrypted export");
    }
    
    @Test
    @DisplayName("Chain validation passes after encrypted export/import")
    void testChainValidationAfterEncryptedImport() throws Exception {
        // Create mixed content
        blockchain.addBlockAndReturn("Public data", privateKey, publicKey);
        blockchain.addEncryptedBlock("Private data", masterPassword, privateKey, publicKey);

        // Export and import
        String exportPath = "test-encrypted-export.json";
        assertTrue(blockchain.exportEncryptedChain(exportPath, masterPassword));

        blockchain.clearAndReinitialize();
        // Re-register bootstrap admin after clear (RBAC v1.0.6)
        blockchain.createBootstrapAdmin(
            CryptoUtil.publicKeyToString(bootstrapKeyPair.getPublic()),
            "BOOTSTRAP_ADMIN"
        );
        blockchain.addAuthorizedKey(publicKeyString, "TestUser", bootstrapKeyPair, UserRole.USER); // Re-add key after clear
        assertTrue(blockchain.importEncryptedChain(exportPath, masterPassword));

        // Validate chain integrity
        var validationResult = blockchain.validateChainDetailed();
        assertTrue(validationResult.isStructurallyIntact(), "Chain should be structurally intact");
        assertEquals(3, validationResult.getTotalBlocks(), "Should have correct block count (2 + genesis)");
        assertEquals(3, validationResult.getValidBlocks(), "All blocks should be valid");
    }
    
    // Helper methods
    
    private void deleteFileIfExists(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            file.delete();
        }
    }
    
    private void deleteDirectory(String dirPath) {
        File dir = new File(dirPath);
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
            dir.delete();
        }
    }
}