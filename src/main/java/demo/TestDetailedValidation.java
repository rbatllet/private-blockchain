package demo;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.security.UserRole;
import com.rbatllet.blockchain.util.CryptoUtil;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * Test to demonstrate the enhanced detailed validation with off-chain data information
 */
public class TestDetailedValidation {
    
    public static void main(String[] args) {
        System.out.println("=== 🔍 DETAILED VALIDATION WITH OFF-CHAIN DATA TEST ===");
        System.out.println();
        
        try {
            // Initialize blockchain
            Blockchain blockchain = new Blockchain();
            
            // Generate key pairs
            KeyPair keyPair1 = CryptoUtil.generateKeyPair();
            PrivateKey privateKey1 = keyPair1.getPrivate();
            PublicKey publicKey1 = keyPair1.getPublic();
            String publicKeyString1 = CryptoUtil.publicKeyToString(publicKey1);
            
            KeyPair keyPair2 = CryptoUtil.generateKeyPair();
            PrivateKey privateKey2 = keyPair2.getPrivate();
            PublicKey publicKey2 = keyPair2.getPublic();
            String publicKeyString2 = CryptoUtil.publicKeyToString(publicKey2);
            
            // Authorize keys
            // First user - Genesis bootstrap
            blockchain.createBootstrapAdmin(
                publicKeyString1,
                "TestUser1"
            );
            // Second user - Created by first user
            blockchain.addAuthorizedKey(
                publicKeyString2,
                "TestUser2",
                keyPair1,  // TestUser1 is the caller
                UserRole.USER
            );
            
            // Add some regular blocks
            blockchain.addBlockAndReturn("Small block 1", privateKey1, publicKey1);
            blockchain.addBlockAndReturn("Small block 2", privateKey2, publicKey2);
            
            // Generate large data to trigger off-chain storage
            StringBuilder largeData1 = new StringBuilder();
            for (int i = 0; i < 50000; i++) {
                largeData1.append("This is large data block 1 that will be stored off-chain. ");
            }
            
            StringBuilder largeData2 = new StringBuilder();
            for (int i = 0; i < 60000; i++) {
                largeData2.append("This is large data block 2 that will be stored off-chain. ");
            }
            
            // Add blocks with off-chain data
            System.out.println("Adding large blocks with off-chain data...");
            Block offChainBlock1 = blockchain.addBlockAndReturn(largeData1.toString(), privateKey1, publicKey1);
            Block offChainBlock2 = blockchain.addBlockAndReturn(largeData2.toString(), privateKey2, publicKey2);
            
            // Show created off-chain blocks information
            if (offChainBlock1 != null && offChainBlock1.hasOffChainData()) {
                System.out.println("✅ Off-chain Block 1 created: #" + offChainBlock1.getBlockNumber() + 
                    " (Data size: " + String.format("%.1f KB", largeData1.length() / 1024.0) + ")");
            }
            if (offChainBlock2 != null && offChainBlock2.hasOffChainData()) {
                System.out.println("✅ Off-chain Block 2 created: #" + offChainBlock2.getBlockNumber() + 
                    " (Data size: " + String.format("%.1f KB", largeData2.length() / 1024.0) + ")");
            }
            
            // Add one more regular block
            blockchain.addBlockAndReturn("Small block 3", privateKey1, publicKey1);
            
            System.out.println("Blockchain created with " + blockchain.getBlockCount() + " blocks");
            System.out.println();
            
            // Perform detailed validation
            System.out.println("=== PERFORMING DETAILED VALIDATION ===");
            System.out.println();
            
            var validationResult = blockchain.validateChainDetailed();
            
            System.out.println();
            System.out.println("=== VALIDATION COMPLETED ===");
            System.out.println("Chain is valid: " + validationResult.isValid());
            System.out.println("Structurally intact: " + validationResult.isStructurallyIntact());
            
        } catch (Exception e) {
            System.err.println("❌ Test failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Cleanup
            System.out.println();
            System.out.println("🧹 Cleaning up test files...");
            cleanup();
        }
    }
    
    private static void cleanup() {
        try {
            // Clean up database files
            deleteFileIfExists("blockchain.db");
            deleteFileIfExists("blockchain.db-shm");
            deleteFileIfExists("blockchain.db-wal");
            
            // Clean up off-chain directory
            java.io.File offChainDir = new java.io.File("off-chain-data");
            if (offChainDir.exists()) {
                deleteDirectory(offChainDir);
            }
            
        } catch (Exception e) {
            System.err.println("Warning: Could not clean up all files: " + e.getMessage());
        }
    }
    
    private static void deleteFileIfExists(String fileName) {
        java.io.File file = new java.io.File(fileName);
        if (file.exists()) {
            file.delete();
        }
    }
    
    private static void deleteDirectory(java.io.File directory) {
        java.io.File[] files = directory.listFiles();
        if (files != null) {
            for (java.io.File file : files) {
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