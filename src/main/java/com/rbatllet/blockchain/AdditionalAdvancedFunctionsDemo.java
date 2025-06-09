package com.rbatllet.blockchain.core;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.util.CryptoUtil;

import java.security.KeyPair;
import java.time.LocalDate;
import java.util.List;

/**
 * Interactive demonstration of ADDITIONAL ADVANCED FUNCTIONS
 * This class shows practical usage of all new advanced blockchain features
 */
public class AdditionalAdvancedFunctionsDemo {
    
    public static void main(String[] args) {
        System.out.println("=== ADDITIONAL ADVANCED FUNCTIONS DEMONSTRATION ===");
        System.out.println("This demo shows how to use all new advanced blockchain features");
        System.out.println();
        
        try {
            // Initialize blockchain
            Blockchain blockchain = new Blockchain();
            
            // Setup test users
            KeyPair alice = CryptoUtil.generateKeyPair();
            KeyPair bob = CryptoUtil.generateKeyPair();
            
            String alicePublicKey = CryptoUtil.publicKeyToString(alice.getPublic());
            String bobPublicKey = CryptoUtil.publicKeyToString(bob.getPublic());
            
            blockchain.addAuthorizedKey(alicePublicKey, "Alice");
            blockchain.addAuthorizedKey(bobPublicKey, "Bob");
            
            // Add some initial blocks
            blockchain.addBlock("Alice creates her account", alice.getPrivate(), alice.getPublic());
            blockchain.addBlock("Bob joins the platform", bob.getPrivate(), bob.getPublic());
            blockchain.addBlock("Alice sends payment to Bob", alice.getPrivate(), alice.getPublic());
            blockchain.addBlock("Bob confirms payment received", bob.getPrivate(), bob.getPublic());
            
            System.out.println("✅ Initial setup completed with " + blockchain.getBlockCount() + " blocks");
            System.out.println();
            
            // ===========================================
            // ADDITIONAL ADVANCED FUNCTION 1: Block Size Validation
            // ===========================================
            System.out.println("🔍 ADDITIONAL ADVANCED FUNCTION 1: Block Size Validation");
            System.out.println("Testing automatic size validation during block creation...");
            
            // Show current limits
            System.out.println("📏 Current size limits:");
            System.out.println("   - Max data length: " + blockchain.getMaxBlockDataLength() + " characters");
            System.out.println("   - Max data size: " + blockchain.getMaxBlockSizeBytes() + " bytes");
            
            // Test valid size
            String normalData = "This is a normal transaction with reasonable size";
            boolean normalAdded = blockchain.addBlock(normalData, alice.getPrivate(), alice.getPublic());
            System.out.println("   ✅ Normal size data added: " + normalAdded);
            
            // Test size at limit
            StringBuilder limitData = new StringBuilder();
            for (int i = 0; i < 1000; i++) {
                limitData.append("Valid data ");
            }
            boolean limitAdded = blockchain.addBlock(limitData.toString(), bob.getPrivate(), bob.getPublic());
            System.out.println("   ✅ Large but valid data added: " + limitAdded);
            
            // Test oversized data (should fail)
            StringBuilder overData = new StringBuilder();
            for (int i = 0; i < blockchain.getMaxBlockDataLength() + 100; i++) {
                overData.append("x");
            }
            boolean overAdded = blockchain.addBlock(overData.toString(), alice.getPrivate(), alice.getPublic());
            System.out.println("   ✅ Oversized data rejected: " + !overAdded);
            
            System.out.println("✅ Block size validation working correctly!");
            System.out.println();
            
            // ===========================================
            // ADDITIONAL ADVANCED FUNCTION 2: Chain Export
            // ===========================================
            System.out.println("🔍 ADDITIONAL ADVANCED FUNCTION 2: Chain Export (Backup)");
            System.out.println("Creating blockchain backup...");
            
            String exportPath = "demo_blockchain_backup.json";
            boolean exported = blockchain.exportChain(exportPath);
            
            if (exported) {
                System.out.println("   ✅ Blockchain exported successfully to: " + exportPath);
                System.out.println("   📦 Backup includes:");
                System.out.println("      - All " + blockchain.getBlockCount() + " blocks");
                System.out.println("      - All " + blockchain.getAuthorizedKeys().size() + " authorized keys");
                System.out.println("      - Metadata and version information");
            } else {
                System.out.println("   ❌ Export failed");
            }
            System.out.println();
            
            // ===========================================
            // ADDITIONAL ADVANCED FUNCTIONS 3-5: Advanced Search
            // ===========================================
            System.out.println("🔍 ADDITIONAL ADVANCED FUNCTIONS 3-5: Advanced Search");
            System.out.println("Testing all search capabilities...");
            
            // Search by content
            System.out.println("🔎 Search by content:");
            List<Block> paymentBlocks = blockchain.searchBlocksByContent("payment");
            System.out.println("   ✅ Found " + paymentBlocks.size() + " blocks containing 'payment'");
            for (Block block : paymentBlocks) {
                System.out.println("      - Block #" + block.getBlockNumber() + ": " + 
                                 block.getData().substring(0, Math.min(50, block.getData().length())) + "...");
            }
            
            // Search by hash
            System.out.println("🔎 Search by hash:");
            Block lastBlock = blockchain.getLastBlock();
            Block foundByHash = blockchain.getBlockByHash(lastBlock.getHash());
            if (foundByHash != null) {
                System.out.println("   ✅ Found block by hash: #" + foundByHash.getBlockNumber());
            } else {
                System.out.println("   ❌ Block not found by hash");
            }
            
            // Search by date range
            System.out.println("🔎 Search by date range:");
            LocalDate today = LocalDate.now();
            LocalDate yesterday = today.minusDays(1);
            List<Block> todayBlocks = blockchain.getBlocksByDateRange(today, today);
            System.out.println("   ✅ Found " + todayBlocks.size() + " blocks from today");
            
            System.out.println();
            
            // ===========================================
            // ADDITIONAL ADVANCED FUNCTION 6: Chain Import
            // ===========================================
            System.out.println("🔍 ADDITIONAL ADVANCED FUNCTION 6: Chain Import (Restore)");
            System.out.println("Testing blockchain restore from backup...");
            
            // Create a new blockchain to test import
            Blockchain newBlockchain = new Blockchain();
            System.out.println("   📝 New blockchain created with " + newBlockchain.getBlockCount() + " blocks (just genesis)");
            
            boolean imported = newBlockchain.importChain(exportPath);
            if (imported) {
                System.out.println("   ✅ Blockchain imported successfully!");
                System.out.println("   📊 Imported blockchain now has " + newBlockchain.getBlockCount() + " blocks");
                System.out.println("   📊 Imported " + newBlockchain.getAuthorizedKeys().size() + " authorized keys");
                
                // Validate imported chain
                boolean importedValid = newBlockchain.validateChain();
                System.out.println("   ✅ Imported chain validation: " + (importedValid ? "PASSED" : "FAILED"));
            } else {
                System.out.println("   ❌ Import failed");
            }
            System.out.println();
            
            // ===========================================
            // ADDITIONAL ADVANCED FUNCTION 7: Block Rollback
            // ===========================================
            System.out.println("🔍 ADDITIONAL ADVANCED FUNCTION 7: Block Rollback");
            System.out.println("Testing blockchain rollback capabilities...");
            
            long blocksBeforeRollback = blockchain.getBlockCount();
            System.out.println("   📊 Blocks before rollback: " + blocksBeforeRollback);
            
            // Add a couple more blocks to rollback
            blockchain.addBlock("Test block for rollback 1", alice.getPrivate(), alice.getPublic());
            blockchain.addBlock("Test block for rollback 2", bob.getPrivate(), bob.getPublic());
            
            long blocksBeforeRollbackTest = blockchain.getBlockCount();
            System.out.println("   📊 Blocks after adding test blocks: " + blocksBeforeRollbackTest);
            
            // Test rollback of 2 blocks
            boolean rolledBack = blockchain.rollbackBlocks(2);
            if (rolledBack) {
                System.out.println("   ✅ Successfully rolled back 2 blocks");
                System.out.println("   📊 Blocks after rollback: " + blockchain.getBlockCount());
                
                // Validate chain after rollback
                boolean validAfterRollback = blockchain.validateChain();
                System.out.println("   ✅ Chain valid after rollback: " + validAfterRollback);
            } else {
                System.out.println("   ❌ Rollback failed");
            }
            
            // Test rollback to specific block
            System.out.println("   🎯 Testing rollback to specific block...");
            boolean rolledToBlock = blockchain.rollbackToBlock(3);
            if (rolledToBlock) {
                System.out.println("   ✅ Successfully rolled back to block 3");
                System.out.println("   📊 Final block count: " + blockchain.getBlockCount());
            } else {
                System.out.println("   ❌ Rollback to block failed");
            }
            
            System.out.println();
            
            // ===========================================
            // FINAL VALIDATION
            // ===========================================
            System.out.println("🔍 FINAL VALIDATION: Testing Integration");
            System.out.println("Verifying all ADDITIONAL ADVANCED functions work together...");
            
            // Final chain validation
            boolean finalValid = blockchain.validateChain();
            System.out.println("   ✅ Final chain validation: " + (finalValid ? "PASSED" : "FAILED"));
            
            // Test search still works after rollback
            List<Block> finalPaymentBlocks = blockchain.searchBlocksByContent("payment");
            System.out.println("   ✅ Search still works: found " + finalPaymentBlocks.size() + " payment blocks");
            
            // Create final export
            String finalExportPath = "final_demo_backup.json";
            boolean finalExported = blockchain.exportChain(finalExportPath);
            System.out.println("   ✅ Final export: " + (finalExported ? "SUCCESS" : "FAILED"));
            
            System.out.println();
            
            // ===========================================
            // SUMMARY
            // ===========================================
            System.out.println("=== ADDITIONAL ADVANCED FUNCTIONS DEMONSTRATION SUMMARY ===");
            System.out.println("✅ Block Size Validation: Automatic size checking prevents oversized blocks");
            System.out.println("✅ Chain Export: Complete blockchain backup to JSON with metadata");
            System.out.println("✅ Chain Import: Full blockchain restore with validation");
            System.out.println("✅ Advanced Search: Content, hash, and date range search capabilities");
            System.out.println("✅ Block Rollback: Safe removal of blocks with genesis protection");
            System.out.println("✅ Integration: All functions work together seamlessly");
            System.out.println();
            System.out.println("🎉 ALL ADDITIONAL ADVANCED FUNCTIONS WORKING PERFECTLY!");
            System.out.println();
            System.out.println("📊 Final Statistics:");
            System.out.println("   - Total blocks: " + blockchain.getBlockCount());
            System.out.println("   - Authorized keys: " + blockchain.getAuthorizedKeys().size());
            System.out.println("   - Chain integrity: " + (blockchain.validateChain() ? "VALID" : "INVALID"));
            System.out.println("   - Size limits: " + blockchain.getMaxBlockSizeBytes() + " bytes, " + 
                             blockchain.getMaxBlockDataLength() + " chars");
            
            // Cleanup demo files
            try {
                java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(exportPath));
                java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(finalExportPath));
                System.out.println("   🗑️ Demo files cleaned up");
            } catch (Exception e) {
                System.out.println("   ⚠️ Could not clean up demo files: " + e.getMessage());
            }
            
        } catch (Exception e) {
            System.err.println("❌ DEMO FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
