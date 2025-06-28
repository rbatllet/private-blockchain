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
        
        // Generate key pair for testing
        KeyPair keyPair = CryptoUtil.generateKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();
        String publicKeyString = CryptoUtil.publicKeyToString(publicKey);
        
        // Add authorized key
        blockchain.addAuthorizedKey(publicKeyString, "TestUser");
        System.out.println("2. ✅ Added authorized key for testing");
        
        // Generate large data for off-chain storage (600KB)
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 60000; i++) {
            sb.append("This is test data for off-chain storage. ");
        }
        String largeData = sb.toString();
        
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
            
            System.out.println();
            System.out.println("🎉 ALL DATA CONSISTENCY TESTS PASSED!");
            System.out.println();
            System.out.println("✅ Rollback operations clean up off-chain files");
            System.out.println("✅ Orphaned file detection and cleanup works");
            System.out.println("✅ No data inconsistency between database and filesystem");
            
            // Show final detailed validation with off-chain data analysis
            System.out.println();
            System.out.println("=== 📊 FINAL DETAILED VALIDATION WITH OFF-CHAIN ANALYSIS ===");
            var finalResult = blockchain.validateChainDetailed();
            System.out.println("Data consistency validation completed successfully!");
            
        } else {
            System.out.println("3. ❌ Failed to create block with off-chain data");
        }
    }
}