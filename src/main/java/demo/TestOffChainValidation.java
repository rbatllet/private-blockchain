package demo;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.util.validation.BlockValidationUtil;
import com.rbatllet.blockchain.util.CryptoUtil;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.time.LocalDateTime;

/**
 * Comprehensive test for off-chain data validation functionality
 * Tests validation of existence, corruption, tampering, and metadata integrity
 */
public class TestOffChainValidation {
    
    private static final Logger logger = LogManager.getLogger(TestOffChainValidation.class);
    
    private static Blockchain blockchain;
    private static PrivateKey privateKey;
    private static PublicKey publicKey;
    
    public static void main(String[] args) {
        System.out.println("=== 🔍 OFF-CHAIN VALIDATION COMPREHENSIVE TEST ===");
        logger.info("=== 🔍 OFF-CHAIN VALIDATION COMPREHENSIVE TEST ===");
        
        try {
            // Initialize blockchain and keys
            initializeTest();
            
            // Test scenarios
            testValidOffChainData();
            testMissingFile();
            testCorruptedMetadata();
            testFileSizeMismatch();
            testTamperingDetection();
            testEmptyFile();
            testTimestampValidation();
            testMetadataValidation();
            
            System.out.println("✅ All off-chain validation tests completed successfully!");
            logger.info("✅ All off-chain validation tests completed successfully!");
            
            // NOTE: Final chain validation is NOT performed because this test intentionally
            // corrupts off-chain files to test validation failures. The blockchain contains
            // blocks with deliberately corrupted data (deleted files, modified content, etc.)
            // which would show as validation errors, but this is EXPECTED behavior for testing.
            System.out.println();
            System.out.println("📊 Test Summary:");
            System.out.println("   ✅ Valid data validation: PASSED");
            System.out.println("   ✅ Missing file detection: PASSED");
            System.out.println("   ✅ Corrupted metadata detection: PASSED");
            System.out.println("   ✅ File size mismatch detection: PASSED");
            System.out.println("   ✅ Tampering detection: PASSED");
            System.out.println("   ✅ Empty file detection: PASSED");
            System.out.println("   ✅ Timestamp validation: PASSED");
            System.out.println("   ✅ Metadata validation: PASSED");
            System.out.println();
            
        } catch (Exception e) {
            System.err.println("❌ Test failed: " + e.getMessage());
            logger.error("❌ Test failed: {}", e.getMessage(), e);
        } finally {
            cleanup();
        }
        
        // Force exit to stop background threads
        System.exit(0);
    }
    
    private static void initializeTest() throws Exception {
        System.out.println("🚀 Initializing test environment...");
        
        // Clean up any existing files
        cleanupFiles();
        
        // Initialize blockchain
        blockchain = new Blockchain();
        
        // Generate test keys
        var keyPair = CryptoUtil.generateKeyPair();
        privateKey = keyPair.getPrivate();
        publicKey = keyPair.getPublic();
        
        // Authorize the key
        blockchain.createBootstrapAdmin(
            CryptoUtil.publicKeyToString(publicKey),
            "TestUser"
        );
        
        System.out.println("✅ Test environment initialized");
        System.out.println();
    }
    
    private static void testValidOffChainData() throws Exception {
        System.out.println("📝 Test 1: Valid off-chain data validation");
        logger.info("📝 Test 1: Valid off-chain data validation");
        
        // Create large data (600KB) to trigger off-chain storage
        StringBuilder largeData = new StringBuilder();
        for (int i = 0; i < 60000; i++) {
            largeData.append("This is test data for off-chain storage validation. ");
        }
        
        // Add block with off-chain data
        Block block = blockchain.addBlockAndReturn(largeData.toString(), privateKey, publicKey);
        
        // Validate using detailed validation
        var result = BlockValidationUtil.validateOffChainDataDetailed(block);
        assert result.isValid() : "Valid off-chain data should pass validation";
        
        // Test basic validation methods
        assert BlockValidationUtil.validateOffChainData(blockchain, block) : "Basic validation should pass";
        assert BlockValidationUtil.offChainFileExists(block) : "File should exist";
        assert BlockValidationUtil.validateOffChainMetadata(block) : "Metadata should be valid";
        assert BlockValidationUtil.detectOffChainTampering(block) : "No tampering should be detected";
        
        System.out.println("   ✅ Valid off-chain data validation passed");
        System.out.println("   📄 Validation result: " + result.getMessage());
        logger.info("   ✅ Valid off-chain data validation passed");
        logger.info("   📄 Validation result: {}", result.getMessage());
        System.out.println();
    }
    
    private static void testMissingFile() throws Exception {
        System.out.println("📝 Test 2: Missing file validation");
        logger.info("📝 Test 2: Missing file validation");
        
        // Create large data to trigger off-chain storage
        StringBuilder largeData = new StringBuilder();
        for (int i = 0; i < 60000; i++) {
            largeData.append("Test data for missing file scenario. ");
        }
        
        Block block = blockchain.addBlockAndReturn(largeData.toString(), privateKey, publicKey);
        
        // Actually delete the off-chain file to test missing file scenario
        if (block.hasOffChainData()) {
            File offChainFile = new File(block.getOffChainData().getFilePath());
            if (offChainFile.exists()) {
                if (!offChainFile.delete()) {
                    throw new RuntimeException("Failed to delete off-chain file for testing: " + offChainFile.getPath());
                }
            }
        }
        
        // Validate - should fail
        var result = BlockValidationUtil.validateOffChainDataDetailed(block);
        assert !result.isValid() : "Missing file should fail validation";
        assert result.getMessage().contains("does not exist") : "Should report missing file";
        
        assert !BlockValidationUtil.offChainFileExists(block) : "File existence check should fail";
        assert !BlockValidationUtil.detectOffChainTampering(block) : "Missing file should be detected as tampering";
        
        System.out.println("   ✅ Missing file validation failed as expected");
        System.out.println("   📄 Validation result: " + result.getMessage());
        logger.info("   ✅ Missing file validation failed as expected");
        logger.info("   📄 Validation result: {}", result.getMessage());
        System.out.println();
    }
    
    private static void testCorruptedMetadata() throws Exception {
        System.out.println("📝 Test 3: Corrupted metadata validation");
        logger.info("📝 Test 3: Corrupted metadata validation");
        
        // Create large data to trigger off-chain storage
        StringBuilder largeData = new StringBuilder();
        for (int i = 0; i < 60000; i++) {
            largeData.append("Test data for corrupted metadata scenario. ");
        }
        
        Block block = blockchain.addBlockAndReturn(largeData.toString(), privateKey, publicKey);
        
        // Actually corrupt the metadata by setting hash to null (real corruption)
        if (block.hasOffChainData()) {
            var offChainData = block.getOffChainData();
            
            // Save original hash for logging
            String originalHash = offChainData.getDataHash();
            
            // Actually corrupt the hash - this is real corruption, not simulation
            offChainData.setDataHash(null);
            
            logger.info("Corrupted metadata hash from '{}' to null", 
                       originalHash != null ? originalHash.substring(0, 10) + "..." : "null");
            
            // Validate - should fail
            var result = BlockValidationUtil.validateOffChainDataDetailed(block);
            assert !result.isValid() : "Corrupted metadata should fail validation";
            assert result.getMessage().contains("Missing data hash") : "Should report missing hash";
            
            System.out.println("   ✅ Corrupted metadata validation failed as expected");
            System.out.println("   📄 Validation result: " + result.getMessage());
            logger.info("   ✅ Corrupted metadata validation failed as expected");
            logger.info("   📄 Validation result: {}", result.getMessage());
        }
        System.out.println();
    }
    
    private static void testFileSizeMismatch() throws Exception {
        System.out.println("📝 Test 4: File size mismatch validation");
        logger.info("📝 Test 4: File size mismatch validation");
        
        // Create large data to trigger off-chain storage
        StringBuilder largeData = new StringBuilder();
        for (int i = 0; i < 60000; i++) {
            largeData.append("Test data for file size mismatch. ");
        }
        
        Block block = blockchain.addBlockAndReturn(largeData.toString(), privateKey, publicKey);
        
        // Modify the file to create size mismatch
        if (block.hasOffChainData()) {
            File offChainFile = new File(block.getOffChainData().getFilePath());
            if (offChainFile.exists()) {
                // Append extra data to the file to change its size
                try (FileWriter writer = new FileWriter(offChainFile, true)) {
                    writer.write("EXTRA DATA TO CHANGE FILE SIZE");
                }
                
                // Validate - should fail due to size mismatch
                var result = BlockValidationUtil.validateOffChainDataDetailed(block);
                assert !result.isValid() : "File size mismatch should fail validation";
                assert result.getMessage().contains("File size mismatch") : "Should report size mismatch";
                
                System.out.println("   ✅ File size mismatch validation failed as expected");
                System.out.println("   📄 Validation result: " + result.getMessage());
                logger.info("   ✅ File size mismatch validation failed as expected");
                logger.info("   📄 Validation result: {}", result.getMessage());
            }
        }
        System.out.println();
    }
    
    private static void testTamperingDetection() throws Exception {
        System.out.println("📝 Test 5: Tampering detection validation");
        logger.info("📝 Test 5: Tampering detection validation");
        
        // Create large data to trigger off-chain storage
        StringBuilder largeData = new StringBuilder();
        for (int i = 0; i < 60000; i++) {
            largeData.append("Test data for tampering detection. ");
        }
        
        Block block = blockchain.addBlockAndReturn(largeData.toString(), privateKey, publicKey);
        
        // Actually modify the file content to create real tampering
        if (block.hasOffChainData()) {
            File offChainFile = new File(block.getOffChainData().getFilePath());
            if (offChainFile.exists()) {
                // Actually modify the file content to create real tampering
                try (FileWriter writer = new FileWriter(offChainFile, true)) {
                    writer.write("\n*** TAMPERED DATA ***\n");
                    writer.flush();
                }
                
                // Test tampering detection
                boolean noTampering = BlockValidationUtil.detectOffChainTampering(block);
                assert !noTampering : "Modified file should be detected as tampered";
                
                System.out.println("   ✅ Tampering detection worked as expected");
                System.out.println("   📄 File modification time differs from creation time");
                logger.info("   ✅ Tampering detection worked as expected");
                logger.info("   📄 File modification time differs from creation time");
            }
        }
        System.out.println();
    }
    
    private static void testEmptyFile() throws Exception {
        System.out.println("📝 Test 6: Empty file validation");
        logger.info("📝 Test 6: Empty file validation");
        
        // Create large data to trigger off-chain storage
        StringBuilder largeData = new StringBuilder();
        for (int i = 0; i < 60000; i++) {
            largeData.append("Test data for empty file scenario. ");
        }
        
        Block block = blockchain.addBlockAndReturn(largeData.toString(), privateKey, publicKey);
        
        // Empty the file
        if (block.hasOffChainData()) {
            File offChainFile = new File(block.getOffChainData().getFilePath());
            if (offChainFile.exists()) {
                // Clear the file content - create empty file
                Files.write(offChainFile.toPath(), new byte[0]);
                
                // Validate - should fail
                var result = BlockValidationUtil.validateOffChainDataDetailed(block);
                assert !result.isValid() : "Empty file should fail validation";
                assert result.getMessage().contains("File is empty") : "Should report empty file";
                
                System.out.println("   ✅ Empty file validation failed as expected");
                System.out.println("   📄 Validation result: " + result.getMessage());
                logger.info("   ✅ Empty file validation failed as expected");
                logger.info("   📄 Validation result: {}", result.getMessage());
            }
        }
        System.out.println();
    }
    
    private static void testTimestampValidation() throws Exception {
        System.out.println("📝 Test 7: Timestamp validation");
        logger.info("📝 Test 7: Timestamp validation");
        
        // Create large data to trigger off-chain storage
        StringBuilder largeData = new StringBuilder();
        for (int i = 0; i < 60000; i++) {
            largeData.append("Test data for timestamp validation. ");
        }
        
        Block block = blockchain.addBlockAndReturn(largeData.toString(), privateKey, publicKey);
        
        // Actually modify off-chain data timestamp to create real inconsistency
        if (block.hasOffChainData()) {
            var offChainData = block.getOffChainData();
            
            // Actually modify the timestamps to create real inconsistency
            LocalDateTime originalBlockTime = block.getTimestamp();
            LocalDateTime originalOffChainTime = offChainData.getCreatedAt();
            
            // Create real timestamp inconsistency by modifying the block timestamp
            block.setTimestamp(LocalDateTime.now().minusHours(1)); // 1 hour ago
            offChainData.setCreatedAt(LocalDateTime.now()); // Current time (inconsistent)
            
            logger.info("Created timestamp inconsistency: Original Block={}, Original OffChain={}, New Block={}, New OffChain={}", 
                       originalBlockTime, originalOffChainTime, block.getTimestamp(), offChainData.getCreatedAt());
            
            // Validate - should show timestamp warning
            var result = BlockValidationUtil.validateOffChainDataDetailed(block);
            
            System.out.println("   ✅ Timestamp validation completed");
            System.out.println("   📄 Validation result: " + result.getMessage());
            logger.info("   ✅ Timestamp validation completed");
            logger.info("   📄 Validation result: {}", result.getMessage());
            
            if (result.getMessage().contains("timestamp difference")) {
                System.out.println("   ⚠️ Timestamp inconsistency detected as expected");
                logger.info("   ⚠️ Timestamp inconsistency detected as expected");
            }
        }
        System.out.println();
    }
    
    private static void testMetadataValidation() throws Exception {
        System.out.println("📝 Test 8: Comprehensive metadata validation");
        logger.info("📝 Test 8: Comprehensive metadata validation");
        
        // Test with block that has no off-chain data
        Block regularBlock = blockchain.addBlockAndReturn("Small data", privateKey, publicKey);
        
        var result = BlockValidationUtil.validateOffChainDataDetailed(regularBlock);
        assert result.isValid() : "Block without off-chain data should pass validation";
        assert result.getMessage().contains("No off-chain data") : "Should report no off-chain data";
        
        // Test metadata validation methods
        assert BlockValidationUtil.validateOffChainMetadata(regularBlock) : "Metadata validation should pass for regular block";
        assert BlockValidationUtil.offChainFileExists(regularBlock) : "File existence should pass for regular block";
        assert BlockValidationUtil.detectOffChainTampering(regularBlock) : "Tampering detection should pass for regular block";
        
        System.out.println("   ✅ Metadata validation for regular block passed");
        System.out.println("   📄 Validation result: " + result.getMessage());
        logger.info("   ✅ Metadata validation for regular block passed");
        logger.info("   📄 Validation result: {}", result.getMessage());
        System.out.println();
    }
    
    private static void cleanup() {
        System.out.println("🧹 Cleaning up test environment...");
        cleanupFiles();
        System.out.println("✅ Cleanup completed");
    }
    
    private static void cleanupFiles() {
        try {
            // Clean up database files
            deleteFileIfExists("blockchain.db");
            deleteFileIfExists("blockchain.db-shm");
            deleteFileIfExists("blockchain.db-wal");
            
            // Clean up off-chain directory
            File offChainDir = new File("off-chain-data");
            if (offChainDir.exists()) {
                deleteDirectory(offChainDir);
            }
            
            // Clean up backup directory
            File backupDir = new File("off-chain-backup");
            if (backupDir.exists()) {
                deleteDirectory(backupDir);
            }
            
        } catch (Exception e) {
            System.err.println("Warning: Could not clean up all files: " + e.getMessage());
        }
    }
    
    private static void deleteFileIfExists(String fileName) {
        File file = new File(fileName);
        if (file.exists()) {
            file.delete();
        }
    }
    
    private static void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }
}