package demo;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.util.JPAUtil;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.io.File;

/**
 * Demo class to test export/import data consistency fixes
 * Validates that export creates off-chain backups and import properly restores them
 * while maintaining data integrity and cleaning up existing files
 */
public class TestExportImport {
    public static void main(String[] args) throws Exception {
        System.out.println("=== 📦 TESTING EXPORT/IMPORT CONSISTENCY ===");
        System.out.println();

        // Initialize blockchain
        Blockchain blockchain = new Blockchain();

        // RBAC FIX (v1.0.6): Clear database before bootstrap to avoid "Existing users" error
        blockchain.clearAndReinitialize();

        // Generate key pair
        KeyPair keyPair = CryptoUtil.generateKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();
        String publicKeyString = CryptoUtil.publicKeyToString(publicKey);
        
        // Add authorized key
        blockchain.createBootstrapAdmin(
            publicKeyString,
            "TestUser"
        );
        System.out.println("2. ✅ Added authorized key");
        
        // Add small block (on-chain)
        Block smallBlock = blockchain.addBlockAndReturn("Small data", privateKey, publicKey);
        System.out.println("3. ✅ Added small block (on-chain): " + smallBlock.getBlockNumber());
        
        // Generate large data for off-chain storage
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 60000; i++) {
            sb.append("Large test data for off-chain storage. ");
        }
        String largeData = sb.toString();
        
        // Add large block (off-chain)
        Block largeBlock = blockchain.addBlockAndReturn(largeData, privateKey, publicKey);
        System.out.println("4. ✅ Added large block (off-chain): " + largeBlock.getBlockNumber());
        
        if (largeBlock != null && largeBlock.hasOffChainData()) {
            String originalFilePath = largeBlock.getOffChainData().getFilePath();
            System.out.println("   📁 Original off-chain file: " + originalFilePath);
            
            // Export chain
            System.out.println("5. 📤 Exporting blockchain...");
            boolean exportSuccess = blockchain.exportChain("export-test.json");
            
            if (exportSuccess) {
                System.out.println("   ✅ Export completed successfully");
                
                // Check for backup directory
                File backupDir = new File("off-chain-backup");
                if (backupDir.exists()) {
                    System.out.println("   📁 Off-chain backup directory created");
                    File[] backupFiles = backupDir.listFiles();
                    if (backupFiles != null && backupFiles.length > 0) {
                        System.out.println("   📄 Backup files: " + backupFiles.length);
                        for (File file : backupFiles) {
                            System.out.println("     - " + file.getName());
                        }
                    }
                } else {
                    System.out.println("   ❌ Backup directory not created!");
                    return;
                }
                
                // Clear and reinitialize blockchain
                System.out.println("6. 🧹 Clearing blockchain for import test...");
                blockchain.clearAndReinitialize();
                
                // Verify original off-chain file was deleted
                if (!new File(originalFilePath).exists()) {
                    System.out.println("   ✅ Original off-chain file properly deleted");
                } else {
                    System.out.println("   ❌ Original off-chain file not cleaned up!");
                    return;
                }
                
                // Import chain
                System.out.println("7. 📥 Importing blockchain...");
                boolean importSuccess = blockchain.importChain("export-test.json");
                
                if (importSuccess) {
                    System.out.println("   ✅ Import completed successfully");
                    
                    // Verify blocks are restored
                    Block restoredSmallBlock = blockchain.getBlock(smallBlock.getBlockNumber());
                    Block restoredLargeBlock = blockchain.getBlock(largeBlock.getBlockNumber());
                    
                    if (restoredSmallBlock != null && restoredLargeBlock != null) {
                        System.out.println("   ✅ All blocks restored");
                        
                        // Verify off-chain data is accessible
                        if (restoredLargeBlock.hasOffChainData()) {
                            String newFilePath = restoredLargeBlock.getOffChainData().getFilePath();
                            System.out.println("   📁 New off-chain file: " + newFilePath);
                            
                            if (new File(newFilePath).exists()) {
                                System.out.println("   ✅ Off-chain file properly restored");
                                
                                // Verify data integrity
                                String restoredData = blockchain.getCompleteBlockData(restoredLargeBlock);
                                if (restoredData.equals(largeData)) {
                                    System.out.println("   ✅ Off-chain data integrity verified");
                                } else {
                                    System.out.println("   ❌ Off-chain data integrity failed!");
                                    return;
                                }
                            } else {
                                System.out.println("   ❌ Off-chain file not restored!");
                                return;
                            }
                        } else {
                            System.out.println("   ❌ Large block missing off-chain data!");
                            return;
                        }
                    } else {
                        System.out.println("   ❌ Blocks not properly restored!");
                        return;
                    }
                } else {
                    System.out.println("   ❌ Import failed!");
                    return;
                }
                
                System.out.println();
                System.out.println("🎉 ALL EXPORT/IMPORT TESTS PASSED!");
                System.out.println();
                System.out.println("✅ Export includes off-chain file backups");
                System.out.println("✅ Import properly restores off-chain files");
                System.out.println("✅ Data integrity maintained across export/import");
                System.out.println("✅ Original files cleaned up during import");
                
                // Show final detailed validation with off-chain data analysis
                System.out.println();
                System.out.println("=== 📊 FINAL DETAILED VALIDATION AFTER EXPORT/IMPORT ===");
                var finalResult = blockchain.validateChainDetailed();
                System.out.println();
                System.out.println("📊 Post Export/Import Validation Summary:");
                System.out.println("   🔍 Structurally Intact: " + finalResult.isStructurallyIntact());
                System.out.println("   ✅ Fully Compliant: " + finalResult.isFullyCompliant());
                System.out.println("   📋 Total Blocks: " + finalResult.getTotalBlocks());
                System.out.println("   ✅ Valid Blocks: " + finalResult.getValidBlocks());
                System.out.println("   ⚠️ Revoked Blocks: " + finalResult.getRevokedBlocks());
                System.out.println("   ❌ Invalid Blocks: " + finalResult.getInvalidBlocks());
                System.out.println();
                System.out.println("Export/import validation completed successfully!");
                
            } else {
                System.out.println("   ❌ Export failed!");
            }
        } else {
            System.out.println("4. ❌ Failed to create large block with off-chain data");
        }
        JPAUtil.shutdown();
    }
}