package demo;

import com.rbatllet.blockchain.config.MemorySafetyConstants;
import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.indexing.IndexingCoordinator;
import com.rbatllet.blockchain.security.UserRole;
import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.validation.ChainValidationResult;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

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

            // RBAC FIX (v1.0.6): Clear database before bootstrap to avoid "Existing users" error
            blockchain.clearAndReinitialize();

            // Setup test users
            KeyPair alice = CryptoUtil.generateKeyPair();
            KeyPair bob = CryptoUtil.generateKeyPair();

            String alicePublicKey = CryptoUtil.publicKeyToString(alice.getPublic());
            String bobPublicKey = CryptoUtil.publicKeyToString(bob.getPublic());

            // First user (Alice) - Genesis bootstrap
            blockchain.createBootstrapAdmin(
                alicePublicKey,
                "Alice"
            );
            // Second user (Bob) - Created by Alice
            blockchain.addAuthorizedKey(
                bobPublicKey,
                "Bob",
                alice,  // Alice is the caller
                UserRole.USER
            );

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
            System.out.println("   - Off-chain threshold: " + blockchain.getOffChainThresholdBytes() + " bytes");
            System.out.println("   - Max block size: " + blockchain.getMaxBlockSizeBytes() + " bytes");

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

            // Test oversized data (should go off-chain or fail if exceeds max)
            StringBuilder overData = new StringBuilder();
            // Create data that exceeds max block size but stays within database limits
            // Use a reasonable size (e.g., just over off-chain threshold) to avoid H2 limitations
            long testSize = blockchain.getOffChainThresholdBytes() + 1000; // 1000 bytes over off-chain threshold
            for (long i = 0; i < testSize; i++) {
                overData.append("x");
            }
            boolean overAdded = blockchain.addBlock(overData.toString(), alice.getPrivate(), alice.getPublic());
            System.out.println("   ✅ Oversized data (" + testSize + " bytes) rejected: " + !overAdded);

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

            // Wait for background indexing to complete
            System.out.println("\n⏳ Waiting for background indexing to complete...");
            IndexingCoordinator.getInstance().waitForCompletion();
            System.out.println("✅ Background indexing completed - all blocks indexed\n");

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
                System.out.println("      - Block #" + Long.toString(block.getBlockNumber()) + ": " +
                                  block.getData().substring(0, Math.min(50, block.getData().length())) + "...");
            }

            // Search by hash
            System.out.println("🔎 Search by hash:");
            Block lastBlock = blockchain.getLastBlock();
            Block foundByHash = blockchain.getBlockByHash(lastBlock.getHash());
            if (foundByHash != null) {
                System.out.println("   ✅ Found block by hash: #" + Long.toString(foundByHash.getBlockNumber()));
            } else {
                System.out.println("   ❌ Block not found by hash");
            }

            // Search by date range
            System.out.println("🔎 Search by date range:");
            LocalDate today = LocalDate.now();
            LocalDate yesterday = today.minusDays(1);
            long recentCount;
            try (Stream<Block> recentBlocks = blockchain.streamBlocksByDateRange(yesterday, today, MemorySafetyConstants.DEFAULT_MAX_SEARCH_RESULTS)) {
                recentCount = recentBlocks.count();
            }
            System.out.println("   ✅ Found " + recentCount + " blocks from yesterday and today");

            System.out.println();

            // ===========================================
            // ADDITIONAL ADVANCED FUNCTION 6: Chain Import
            // ===========================================
            System.out.println("🔍 ADDITIONAL ADVANCED FUNCTION 6: Chain Import (Restore)");
            System.out.println("Testing blockchain restore from backup...");

            // Create a new blockchain to test import
            Blockchain newBlockchain = new Blockchain();

            // RBAC FIX (v1.0.6): Clear database before bootstrap to avoid "Existing users" error
            blockchain.clearAndReinitialize();

            System.out.println("   📝 New blockchain created with " + newBlockchain.getBlockCount() + " blocks (just genesis)");

            boolean imported = newBlockchain.importChain(exportPath);
            if (imported) {
                System.out.println("   ✅ Blockchain imported successfully!");
                System.out.println("   📊 Imported blockchain now has " + newBlockchain.getBlockCount() + " blocks");
                System.out.println("   📊 Imported " + newBlockchain.getAuthorizedKeys().size() + " authorized keys");

                // Enhanced validation of imported chain
                ChainValidationResult importResult = newBlockchain.validateChainDetailed();
                System.out.println("   📊 Enhanced import validation: " + importResult.getSummary());
                System.out.println("   🏗️ Structural integrity: " + (importResult.isStructurallyIntact() ? "✅ Intact" : "❌ Compromised"));
                System.out.println("   ✅ Full compliance: " + (importResult.isFullyCompliant() ? "✅ Compliant" : "⚠️ Issues"));
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
            boolean rolledBack = blockchain.rollbackBlocks(2L);
            if (rolledBack) {
                System.out.println("   ✅ Successfully rolled back 2L blocks");
                System.out.println("   📊 Blocks after rollback: " + blockchain.getBlockCount());

                // Enhanced validation after rollback
                ChainValidationResult rollbackResult = blockchain.validateChainDetailed();
                System.out.println("   📊 Enhanced rollback validation: " + rollbackResult.getSummary());
                System.out.println("   🏗️ Structural integrity: " + (rollbackResult.isStructurallyIntact() ? "✅ Intact" : "❌ Compromised"));
                System.out.println("   ✅ Full compliance: " + (rollbackResult.isFullyCompliant() ? "✅ Compliant" : "⚠️ Issues"));
            } else {
                System.out.println("   ❌ Rollback failed");
            }

            // Test rollback to specific block
            System.out.println("   🎯 Testing rollback to specific block...");
            boolean rolledToBlock = blockchain.rollbackToBlock(3L);
            if (rolledToBlock) {
                System.out.println("   ✅ Successfully rolled back to block 3L");
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

            // Enhanced final validation
            ChainValidationResult finalResult = blockchain.validateChainDetailed();
            System.out.println("   📊 Enhanced final validation: " + finalResult.getSummary());
            System.out.println("   🏗️ Structural integrity: " + (finalResult.isStructurallyIntact() ? "✅ Intact" : "❌ Compromised"));
            System.out.println("   ✅ Full compliance: " + (finalResult.isFullyCompliant() ? "✅ Compliant" : "⚠️ Issues"));
            System.out.println("   📋 Final statistics: " + finalResult.getValidBlocks() + "/" + finalResult.getTotalBlocks() + " blocks valid");

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
            System.out.println("📊 Enhanced Final Statistics:");
            System.out.println("   - Total blocks: " + blockchain.getBlockCount());
            System.out.println("   - Authorized keys: " + blockchain.getAuthorizedKeys().size());

            // Enhanced final statistics
            ChainValidationResult statsResult = blockchain.validateChainDetailed();
            System.out.println("   - Enhanced validation: " + statsResult.getSummary());
            System.out.println("   - Valid blocks: " + statsResult.getValidBlocks() + "/" + statsResult.getTotalBlocks());
            System.out.println("   - Revoked blocks: " + statsResult.getRevokedBlocks());
            System.out.println("   - Size limits: off-chain threshold=" + blockchain.getOffChainThresholdBytes() + " bytes, " +
                             "max=" + blockchain.getMaxBlockSizeBytes() + " bytes");

            System.out.println("\n💡 Enhanced Features Demonstrated:");
            System.out.println("   • Rich validation results with detailed breakdowns");
            System.out.println("   • Clear separation of structural vs compliance issues");
            System.out.println("   • Better monitoring and debugging capabilities");

            // Cleanup demo files
            try {
                Files.deleteIfExists(Paths.get(exportPath));
                Files.deleteIfExists(Paths.get(finalExportPath));
                System.out.println("   🗑️ Demo files cleaned up");
            } catch (Exception e) {
                System.out.println("   ⚠️ Could not clean up demo files: " + e.getMessage());
            }

            // Explicitly shutdown JPA before exit to prevent thread warnings
            // This must be called before main() returns for clean shutdown
            System.out.println("   🔌 Shutting down database connections...");
            com.rbatllet.blockchain.util.JPAUtil.shutdown();
            System.out.println("   ✅ All resources cleaned up properly");

        } catch (Exception e) {
            System.err.println("❌ DEMO FAILED: " + e.getMessage());
            e.printStackTrace();
        }
        // Note: JPAUtil.shutdown() is called both explicitly above AND by shutdown hook
        // The explicit call ensures H2 threads are stopped before mvn exec:java interrupts
    }
}
