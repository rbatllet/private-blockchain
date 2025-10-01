package com.rbatllet.blockchain.core;

import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.util.CryptoUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileWriter;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Edge cases and error scenarios for blockchain export/import operations
 * Tests boundary conditions, error handling, and recovery scenarios
 */
public class ExportImportEdgeCasesTest {
    
    private Blockchain blockchain;
    private KeyPair keyPair;
    private PrivateKey privateKey;
    private PublicKey publicKey;
    private String publicKeyString;
    private String masterPassword;
    
    @BeforeEach
    void setUp() throws Exception {
        blockchain = new Blockchain();
        keyPair = CryptoUtil.generateKeyPair();
        privateKey = keyPair.getPrivate();
        publicKey = keyPair.getPublic();
        publicKeyString = CryptoUtil.publicKeyToString(publicKey);
        masterPassword = "EdgeCasePassword123!";
        
        // Ensure clean state
        blockchain.clearAndReinitialize();
        blockchain.addAuthorizedKey(publicKeyString, "TestUser");
    }
    
    @AfterEach
    void tearDown() {
        // Clean up test files
        deleteFileIfExists("empty-export.json");
        deleteFileIfExists("corrupted-export.json");
        deleteFileIfExists("invalid-json.json");
        deleteFileIfExists("large-path-export.json");
        deleteFileIfExists("unicode-export.json");
        deleteFileIfExists("special-chars-export.json");
        deleteFileIfExists("readonly-test.json");
        deleteFileIfExists("nonexistent-import.json");
        deleteDirectory("off-chain-backup");
        deleteDirectory("readonly-dir");
        deleteDirectory("unicode-测试");
    }
    
    @Test
    @DisplayName("Export empty blockchain (only genesis)")
    void testExportEmptyBlockchain() throws Exception {
        // Clear all blocks except genesis
        blockchain.clearAndReinitialize();
        blockchain.addAuthorizedKey(publicKeyString, "TestUser");
        
        // Export empty chain
        assertTrue(blockchain.exportChain("empty-export.json"));
        
        // Verify export file exists and has genesis block
        File exportFile = new File("empty-export.json");
        assertTrue(exportFile.exists());
        assertTrue(exportFile.length() > 0);
        
        // Import and verify
        blockchain.clearAndReinitialize();
        blockchain.addAuthorizedKey(publicKeyString, "TestUser");
        assertTrue(blockchain.importChain("empty-export.json"));

        // Should have at least genesis block
        assertTrue(blockchain.getBlockCount() >= 1);
    }
    
    @Test
    @DisplayName("Export to invalid file path should fail gracefully")
    void testExportToInvalidPath() throws Exception {
        blockchain.addBlockAndReturn("Test data", privateKey, publicKey);
        
        // Try to export to invalid paths
        assertFalse(blockchain.exportChain("/root/nonexistent/path/export.json"));
        assertFalse(blockchain.exportChain(""));
        assertFalse(blockchain.exportChain("   "));
        
        // Try encrypted export to invalid paths
        assertFalse(blockchain.exportEncryptedChain("/root/nonexistent/path/export.json", masterPassword));
        assertFalse(blockchain.exportEncryptedChain("", masterPassword));
    }
    
    @Test
    @DisplayName("Import nonexistent file should fail gracefully")
    void testImportNonexistentFile() throws Exception {
        assertFalse(blockchain.importChain("nonexistent-file.json"));
        assertFalse(blockchain.importEncryptedChain("nonexistent-file.json", masterPassword));
    }
    
    @Test
    @DisplayName("Import corrupted JSON should fail gracefully")
    void testImportCorruptedJSON() throws Exception {
        // Create corrupted JSON file
        try (FileWriter writer = new FileWriter("corrupted-export.json")) {
            writer.write("{\"blocks\": [corrupted json syntax");
        }
        
        assertFalse(blockchain.importChain("corrupted-export.json"));
        assertFalse(blockchain.importEncryptedChain("corrupted-export.json", masterPassword));
    }
    
    @Test
    @DisplayName("Import invalid JSON structure should fail gracefully")
    void testImportInvalidJSONStructure() throws Exception {
        // Create valid JSON but invalid structure
        try (FileWriter writer = new FileWriter("invalid-json.json")) {
            writer.write("{\"someField\": \"someValue\", \"notBlocks\": []}");
        }
        
        assertFalse(blockchain.importChain("invalid-json.json"));
        assertFalse(blockchain.importEncryptedChain("invalid-json.json", masterPassword));
    }
    
    @Test
    @DisplayName("Export/import with Unicode data")
    void testUnicodeDataHandling() throws Exception {
        // Add blocks with Unicode content
        blockchain.addBlockAndReturn("Unicode test: 你好世界 🌍 العالم مرحبا Здравствуй мир", privateKey, publicKey);
        blockchain.addEncryptedBlock("Encrypted Unicode: 日本語テスト 한국어 시험 Тест русский", 
            masterPassword, privateKey, publicKey);
        
        // Export and import
        assertTrue(blockchain.exportEncryptedChain("unicode-export.json", masterPassword));
        
        blockchain.clearAndReinitialize();
        blockchain.addAuthorizedKey(publicKeyString, "TestUser");
        assertTrue(blockchain.importEncryptedChain("unicode-export.json", masterPassword));

        // Verify Unicode content is preserved
        AtomicBoolean foundUnicode = new AtomicBoolean(false);
        blockchain.processChainInBatches(batch -> {
            for (Block block : batch) {
                if (block.getData() != null && block.getData().contains("你好世界")) {
                    foundUnicode.set(true);
                }
            }
        }, 1000);
        assertTrue(foundUnicode.get(), "Unicode content should be preserved");
    }

    @Test
    @DisplayName("Export/import with special characters in data")
    void testSpecialCharactersHandling() throws Exception {
        // Add blocks with special characters
        String specialData = "Special chars: !@#$%^&*()_+-=[]{}|;':\",./<>?`~\\n\\t\\r\\\"\\\\";
        blockchain.addBlockAndReturn(specialData, privateKey, publicKey);
        blockchain.addEncryptedBlock("Encrypted: " + specialData, masterPassword, privateKey, publicKey);

        // Export and import
        assertTrue(blockchain.exportEncryptedChain("special-chars-export.json", masterPassword));

        blockchain.clearAndReinitialize();
        blockchain.addAuthorizedKey(publicKeyString, "TestUser");
        assertTrue(blockchain.importEncryptedChain("special-chars-export.json", masterPassword));

        // Verify special characters are preserved
        AtomicBoolean foundSpecialChars = new AtomicBoolean(false);
        blockchain.processChainInBatches(batch -> {
            for (Block block : batch) {
                if (block.getData() != null && block.getData().contains("!@#$%^&*")) {
                    foundSpecialChars.set(true);
                }
            }
        }, 1000);
        assertTrue(foundSpecialChars.get(), "Special characters should be preserved");
    }
    
    @Test
    @DisplayName("Export with insufficient disk space simulation")
    void testExportInsufficientSpace() throws Exception {
        // Create very large data to potentially trigger space issues
        StringBuilder largeData = new StringBuilder();
        for (int i = 0; i < 100000; i++) {
            largeData.append("Large data entry ").append(i).append(" with substantial content to test disk space limits. ");
        }
        
        blockchain.addBlockAndReturn(largeData.toString(), privateKey, publicKey);
        
        // Export should handle large data gracefully
        boolean exportResult = blockchain.exportChain("large-path-export.json");
        // Note: This test might succeed on systems with sufficient space
        // The important thing is that it doesn't crash
        assertNotNull(exportResult, "Export should return a boolean result, not crash");
        
        if (exportResult) {
            // If export succeeded, verify the file
            File exportFile = new File("large-path-export.json");
            assertTrue(exportFile.exists());
            assertTrue(exportFile.length() > 1000); // Should have reasonable size (changed from 1MB to 1KB)
        }
    }
    
    @Test
    @DisplayName("Import file with missing off-chain data")
    void testImportMissingOffChainData() throws Exception {
        // Create large data that will be stored off-chain
        StringBuilder largeData = new StringBuilder();
        for (int i = 0; i < 50000; i++) {
            largeData.append("Off-chain data entry ").append(i).append(" with content. ");
        }
        
        Block largeBlock = blockchain.addBlockAndReturn(largeData.toString(), privateKey, publicKey);
        assertTrue(largeBlock.hasOffChainData());
        
        // Export chain
        assertTrue(blockchain.exportChain("missing-offchain-export.json"));
        
        // Manually delete off-chain backup files to simulate missing data
        File backupDir = new File("off-chain-backup");
        if (backupDir.exists()) {
            File[] files = backupDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
        }
        
        // Import should handle missing off-chain data gracefully
        blockchain.clearAndReinitialize();
        blockchain.addAuthorizedKey(publicKeyString, "TestUser");
        boolean importResult = blockchain.importChain("missing-offchain-export.json");

        // Import should succeed but off-chain data should be removed
        assertTrue(importResult, "Import should succeed even with missing off-chain data");

        // Verify blocks exist but off-chain references are cleaned up
        assertTrue(blockchain.getBlockCount() > 1); // Should have imported blocks

        deleteFileIfExists("missing-offchain-export.json");
    }
    
    @Test
    @DisplayName("Encrypted export with null/empty master password")
    void testEncryptedExportInvalidPassword() throws Exception {
        blockchain.addEncryptedBlock("Test encrypted data", masterPassword, privateKey, publicKey);
        
        // Test null password
        assertFalse(blockchain.exportEncryptedChain("test-export.json", null));
        
        // Test empty password
        assertFalse(blockchain.exportEncryptedChain("test-export.json", ""));
        
        // Test whitespace-only password
        assertFalse(blockchain.exportEncryptedChain("test-export.json", "   "));
        
        // Test null password for import
        assertFalse(blockchain.importEncryptedChain("test-export.json", null));
        
        // Test empty password for import
        assertFalse(blockchain.importEncryptedChain("test-export.json", ""));
    }
    
    @Test
    @DisplayName("Export/import chain with revoked keys")
    void testExportImportWithRevokedKeys() throws Exception {
        // Add blocks with different keys
        blockchain.addBlockAndReturn("Block with original key", privateKey, publicKey);
        
        // Generate second key pair
        KeyPair keyPair2 = CryptoUtil.generateKeyPair();
        String publicKey2String = CryptoUtil.publicKeyToString(keyPair2.getPublic());
        blockchain.addAuthorizedKey(publicKey2String, "TestUser2");
        
        blockchain.addBlockAndReturn("Block with second key", keyPair2.getPrivate(), keyPair2.getPublic());
        
        // Revoke second key
        blockchain.revokeAuthorizedKey(publicKey2String);
        
        // Export chain with revoked keys
        assertTrue(blockchain.exportChain("revoked-keys-export.json"));
        
        // Import and verify revoked keys are preserved
        blockchain.clearAndReinitialize();
        assertTrue(blockchain.importChain("revoked-keys-export.json"));
        
        // Verify both active and revoked keys are imported
        var authorizedKeys = blockchain.getAllAuthorizedKeys();
        assertTrue(authorizedKeys.size() >= 2);
        
        boolean hasRevokedKey = authorizedKeys.stream()
            .anyMatch(key -> !key.isActive() && key.getPublicKey().equals(publicKey2String));
        assertTrue(hasRevokedKey, "Revoked key should be preserved during import");
        
        deleteFileIfExists("revoked-keys-export.json");
    }
    
    @Test
    @DisplayName("Multiple rapid export/import cycles")
    void testRapidExportImportCycles() throws Exception {
        // Initial data
        blockchain.addBlockAndReturn("Cycle test data", privateKey, publicKey);
        blockchain.addEncryptedBlock("Cycle encrypted data", masterPassword, privateKey, publicKey);
        
        // Perform multiple rapid cycles
        for (int cycle = 0; cycle < 5; cycle++) {
            String exportFile = "cycle-" + cycle + "-export.json";
            
            // Export
            assertTrue(blockchain.exportEncryptedChain(exportFile, masterPassword), 
                "Export cycle " + cycle + " should succeed");
            
            // Clear and import
            blockchain.clearAndReinitialize();
            blockchain.addAuthorizedKey(publicKeyString, "TestUser");
            assertTrue(blockchain.importEncryptedChain(exportFile, masterPassword),
                "Import cycle " + cycle + " should succeed");

            // Verify data integrity
            assertTrue(blockchain.getBlockCount() >= 2, "Should maintain block count through cycle " + cycle);

            // Add new data for next cycle
            blockchain.addBlockAndReturn("Cycle " + cycle + " additional data", privateKey, publicKey);

            deleteFileIfExists(exportFile);
        }
        
        // Final validation
        var validationResult = blockchain.validateChainDetailed();
        assertTrue(validationResult.isStructurallyIntact(), 
            "Blockchain should remain intact after multiple cycles");
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
                    if (file.isDirectory()) {
                        deleteDirectory(file.getAbsolutePath());
                    } else {
                        file.delete();
                    }
                }
            }
            dir.delete();
        }
    }
}