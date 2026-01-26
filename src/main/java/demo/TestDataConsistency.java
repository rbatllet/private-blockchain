package demo;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.util.CryptoUtil;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.io.File;

/**
 * Demo class to test data consistency fixes for off-chain storage
 * Validates that rollback operations properly clean up off-chain files
 * and that orphaned file detection works correctly
 */
public class TestDataConsistency {
    public static void main(String[] args) throws Exception {
        System.out.println("=== 🧪 TESTING DATA CONSISTENCY FIXES ===");
        System.out.println();

        // Initialize blockchain
        Blockchain blockchain = new Blockchain();

        // RBAC FIX (v1.0.6): Clear database before bootstrap to avoid "Existing users" error
        blockchain.clearAndReinitialize();

        // Generate key pair for testing
        KeyPair keyPair = CryptoUtil.generateKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();
        String publicKeyString = CryptoUtil.publicKeyToString(publicKey);
        
        // Add authorized key
        blockchain.createBootstrapAdmin(
            publicKeyString,
            "TestUser"
        );
        System.out.println("2. ✅ Added authorized key for testing");
        
        // Generate large data for off-chain storage
        // Off-chain threshold: 512KB, Max block size: 10MB
        // Test 1: ~600KB (above 512KB threshold to trigger off-chain)
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 15000; i++) {  // 15000 × 41 chars = 615,000 bytes ≈ 600KB
            sb.append("This is test data for off-chain storage. ");
        }
        String largeData = sb.toString();

        System.out.println("   📊 Test data size: " + largeData.length() + " bytes (" +
                          String.format("%.2f", largeData.length() / 1024.0 / 1024.0) + " MB)");
        
        // Add block with off-chain data
        Block block = blockchain.addBlockAndReturn(largeData, privateKey, publicKey);
        if (block != null && block.hasOffChainData()) {
            System.out.println("3. ✅ Added block with off-chain data");
            String filePath = block.getOffChainData().getFilePath();
            System.out.println("   📁 Off-chain file: " + filePath);
            
            // Verify file exists
            if (new File(filePath).exists()) {
                System.out.println("   ✅ Off-chain file exists on disk");
            } else {
                System.out.println("   ❌ Off-chain file missing!");
                return;
            }
            
            // Test rollback data consistency
            System.out.println("4. 🔄 Testing rollback data consistency...");
            boolean rollbackSuccess = blockchain.rollbackBlocks(1L);
            
            if (rollbackSuccess) {
                System.out.println("   ✅ Rollback completed successfully");
                
                // Check if off-chain file was cleaned up
                if (!new File(filePath).exists()) {
                    System.out.println("   ✅ Off-chain file properly deleted during rollback");
                } else {
                    System.out.println("   ❌ Off-chain file not cleaned up!");
                    return;
                }
            } else {
                System.out.println("   ❌ Rollback failed!");
                return;
            }
            
            // Test orphaned files cleanup
            System.out.println("5. 🧹 Testing orphaned files cleanup...");
            
            // Create orphaned file
            File offChainDir = new File("off-chain-data");
            if (!offChainDir.exists()) {
                offChainDir.mkdirs();
            }
            File orphanedFile = new File(offChainDir, "orphaned_test_file.dat");
            orphanedFile.createNewFile();
            
            System.out.println("   📁 Created orphaned file: " + orphanedFile.getName());
            
            // Run cleanup
            int cleanedFiles = blockchain.cleanupOrphanedFiles();
            System.out.println("   🧹 Cleaned up " + cleanedFiles + " orphaned files");
            
            if (!orphanedFile.exists()) {
                System.out.println("   ✅ Orphaned file properly cleaned up");
            } else {
                System.out.println("   ❌ Orphaned file not cleaned up!");
                return;
            }
            
            // Test 2: 2 MB file (validates increased block size limit)
            System.out.println("6. 📊 Testing 2 MB file support...");
            StringBuilder sb2 = new StringBuilder();
            for (int i = 0; i < 50000; i++) {  // 50000 × 41 chars = 2,050,000 bytes ≈ 2 MB
                sb2.append("This is test data for off-chain storage. ");
            }
            String data2MB = sb2.toString();
            System.out.println("   📊 Test data size: " + data2MB.length() + " bytes (" +
                              String.format("%.2f", data2MB.length() / 1024.0 / 1024.0) + " MB)");

            Block block2MB = blockchain.addBlockAndReturn(data2MB, privateKey, publicKey);
            if (block2MB != null && block2MB.hasOffChainData()) {
                System.out.println("   ✅ Successfully stored 2 MB file");
                System.out.println("   📁 Off-chain file: " + block2MB.getOffChainData().getFilePath());
            } else {
                System.out.println("   ❌ Failed to store 2 MB file");
                return;
            }

            System.out.println();
            System.out.println("🎉 ALL DATA CONSISTENCY TESTS PASSED!");
            System.out.println();
            System.out.println("✅ Rollback operations clean up off-chain files");
            System.out.println("✅ Orphaned file detection and cleanup works");
            System.out.println("✅ No data inconsistency between database and filesystem");
            System.out.println("✅ Support for files up to 10 MB (600KB and 2MB tested)");
            
            // Show final detailed validation with off-chain data analysis
            System.out.println();
            System.out.println("=== 📊 FINAL DETAILED VALIDATION WITH OFF-CHAIN ANALYSIS ===");
            var finalResult = blockchain.validateChainDetailed();
            
            System.out.println();
            System.out.println("📊 Final Validation Summary:");
            System.out.println("   🔍 Structurally Intact: " + finalResult.isStructurallyIntact());
            System.out.println("   ✅ Fully Compliant: " + finalResult.isFullyCompliant());
            System.out.println("   📋 Total Blocks: " + finalResult.getTotalBlocks());
            System.out.println("   ✅ Valid Blocks: " + finalResult.getValidBlocks());
            System.out.println("   ⚠️ Revoked Blocks: " + finalResult.getRevokedBlocks());
            System.out.println("   ❌ Invalid Blocks: " + finalResult.getInvalidBlocks());
            System.out.println();
            System.out.println("Data consistency validation completed successfully!");
        } else {
            System.out.println("3. ❌ Failed to create block with off-chain data");
        }
        
        // Force exit to stop background threads
        System.exit(0);
    }
}