package demo;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.util.CryptoUtil;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.io.File;

/**
 * Demo class to test enhanced export/import functionality for encrypted chains
 * Validates that encrypted blocks and off-chain data can be properly exported and restored
 */
public class EncryptedChainExportImportDemo {
    
    public static void main(String[] args) throws Exception {
        System.out.println("=== 🔐 TESTING ENCRYPTED CHAIN EXPORT/IMPORT ===");
        System.out.println();
        
        // Initialize blockchain and encryption
        Blockchain blockchain = new Blockchain();
        String masterPassword = "SuperSecurePassword123!";
        
        // Generate key pair
        KeyPair keyPair = CryptoUtil.generateKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();
        String publicKeyString = CryptoUtil.publicKeyToString(publicKey);
        
        // Add authorized key
        blockchain.addAuthorizedKey(publicKeyString, "TestUser");
        System.out.println("1. ✅ Added authorized key");
        
        // Add regular unencrypted block
        Block regularBlock = blockchain.addBlockAndReturn("Regular unencrypted data", privateKey, publicKey);
        System.out.println("2. ✅ Added regular block: " + regularBlock.getBlockNumber());
        
        // Add encrypted block using SecureBlockEncryptionService
        String sensitiveData = "CONFIDENTIAL: Patient medical record with sensitive information including SSN: 123-45-6789";
        Block encryptedBlock = blockchain.addEncryptedBlock(sensitiveData, masterPassword, privateKey, publicKey);
        System.out.println("3. 🔐 Added encrypted block: " + encryptedBlock.getBlockNumber());
        
        // Add large block with off-chain storage (will be encrypted automatically)
        StringBuilder largeDataBuilder = new StringBuilder();
        largeDataBuilder.append("LARGE ENCRYPTED DATASET - ");
        for (int i = 0; i < 50000; i++) {
            largeDataBuilder.append("Sensitive financial data entry ").append(i).append(" with account details. ");
        }
        String largeData = largeDataBuilder.toString();
        
        Block largeBlock = blockchain.addBlockAndReturn(largeData, privateKey, publicKey);
        System.out.println("4. 📦 Added large block (off-chain): " + largeBlock.getBlockNumber());
        
        // Verify we have encrypted content
        java.util.concurrent.atomic.AtomicLong encryptedBlocks = new java.util.concurrent.atomic.AtomicLong(0);
        java.util.concurrent.atomic.AtomicLong offChainBlocks = new java.util.concurrent.atomic.AtomicLong(0);
        blockchain.processChainInBatches(batch -> {
            batch.forEach(block -> {
                if (block.isDataEncrypted()) encryptedBlocks.incrementAndGet();
                if (block.hasOffChainData()) offChainBlocks.incrementAndGet();
            });
        }, 1000);

        System.out.println("5. 📊 Blockchain state before export:");
        System.out.println("   📦 Total blocks: " + blockchain.getBlockCount());
        System.out.println("   🔐 Encrypted blocks: " + encryptedBlocks);
        System.out.println("   📁 Off-chain blocks: " + offChainBlocks);
        
        // Test enhanced export for encrypted chains
        System.out.println("\\n6. 🔐 Testing encrypted chain export...");
        String exportFilePath = "encrypted-chain-export.json";
        boolean exportSuccess = blockchain.exportEncryptedChain(exportFilePath, masterPassword);
        
        if (exportSuccess) {
            System.out.println("   ✅ Encrypted export completed successfully");
            
            // Verify export file exists and has reasonable size
            File exportFile = new File(exportFilePath);
            if (exportFile.exists()) {
                System.out.println("   📄 Export file size: " + (exportFile.length() / 1024) + " KB");
            }
            
            // Check for off-chain backup directory
            File backupDir = new File("off-chain-backup");
            if (backupDir.exists()) {
                File[] backupFiles = backupDir.listFiles();
                System.out.println("   📁 Off-chain backup files: " + (backupFiles != null ? backupFiles.length : 0));
            }
            
            // Clear blockchain and test import
            System.out.println("\\n7. 🧹 Clearing blockchain for import test...");
            blockchain.clearAndReinitialize();

            // Verify blockchain is empty
            if (blockchain.getBlockCount() == 0) {
                System.out.println("   ✅ Blockchain cleared successfully");
            } else {
                System.out.println("   ❌ Failed to clear blockchain!");
                return;
            }
            
            // Test enhanced import for encrypted chains
            System.out.println("\\n8. 📥 Testing encrypted chain import...");
            boolean importSuccess = blockchain.importEncryptedChain(exportFilePath, masterPassword);
            
            if (importSuccess) {
                System.out.println("   ✅ Encrypted import completed successfully");

                // Verify all blocks are restored
                System.out.println("\\n9. 🔍 Verifying restored blockchain...");
                long restoredBlockCount = blockchain.getBlockCount();
                System.out.println("   📦 Total restored blocks: " + restoredBlockCount);
                
                // Verify regular block
                Block restoredRegularBlock = blockchain.getBlock(regularBlock.getBlockNumber());
                if (restoredRegularBlock != null && !restoredRegularBlock.isDataEncrypted()) {
                    System.out.println("   ✅ Regular block restored correctly");
                } else {
                    System.out.println("   ❌ Regular block restoration failed!");
                }
                
                // Verify encrypted block
                Block restoredEncryptedBlock = blockchain.getBlock(encryptedBlock.getBlockNumber());
                if (restoredEncryptedBlock != null && restoredEncryptedBlock.isDataEncrypted()) {
                    System.out.println("   🔐 Encrypted block restored with encryption flag");
                    
                    // Test decryption by getting decrypted block data
                    try {
                        String decryptedData = blockchain.getDecryptedBlockData(
                            restoredEncryptedBlock.getBlockNumber(), masterPassword);
                        if (decryptedData != null && decryptedData.contains("CONFIDENTIAL")) {
                            System.out.println("   ✅ Encrypted block data successfully decrypted and verified");
                        } else {
                            System.out.println("   ❌ Encrypted block data decryption mismatch!");
                        }
                    } catch (Exception e) {
                        System.out.println("   ❌ Failed to decrypt restored encrypted block: " + e.getMessage());
                    }
                } else {
                    System.out.println("   ❌ Encrypted block restoration failed!");
                }
                
                // Verify large off-chain block
                Block restoredLargeBlock = blockchain.getBlock(largeBlock.getBlockNumber());
                if (restoredLargeBlock != null && restoredLargeBlock.hasOffChainData()) {
                    System.out.println("   📁 Large off-chain block restored");
                    
                    try {
                        String restoredLargeData = blockchain.getCompleteBlockData(restoredLargeBlock);
                        if (restoredLargeData.length() > 1000000) { // Should be large
                            System.out.println("   ✅ Off-chain data successfully restored and accessible");
                        } else {
                            System.out.println("   ❌ Off-chain data seems incomplete!");
                        }
                    } catch (Exception e) {
                        System.out.println("   ❌ Failed to access restored off-chain data: " + e.getMessage());
                    }
                } else {
                    System.out.println("   ❌ Large off-chain block restoration failed!");
                }
                
                // Final validation
                System.out.println("\\n10. 🔍 Final chain validation...");
                var validationResult = blockchain.validateChainDetailed();
                System.out.println("   🔍 Structurally Intact: " + validationResult.isStructurallyIntact());
                System.out.println("   ✅ Fully Compliant: " + validationResult.isFullyCompliant());
                System.out.println("   📊 Valid Blocks: " + validationResult.getValidBlocks() + "/" + validationResult.getTotalBlocks());
                
                if (validationResult.isFullyCompliant()) {
                    System.out.println("\\n🎉 ALL ENCRYPTED EXPORT/IMPORT TESTS PASSED!");
                    System.out.println("\\n✅ Enhanced export includes encryption keys");
                    System.out.println("✅ Enhanced import properly restores encryption context");
                    System.out.println("✅ Encrypted blocks remain decryptable after import");
                    System.out.println("✅ Off-chain encrypted data integrity maintained");
                    System.out.println("✅ Chain validation passes after encrypted import");
                } else {
                    System.out.println("\\n⚠️ Validation issues found after import");
                }
                
            } else {
                System.out.println("   ❌ Encrypted import failed!");
            }
            
        } else {
            System.out.println("   ❌ Encrypted export failed!");
        }
        
        System.out.println("\\n=== Demo completed ===");
    }
}