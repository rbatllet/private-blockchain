package demo;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.validation.ChainValidationResult;
import com.rbatllet.blockchain.security.UserRole;

import java.security.KeyPair;

/**
 * Enhanced demonstration of the new validation system
 * Shows how the improved validation provides granular information
 */
public class EnhancedDangerousDeleteDemo {
    
    public static void main(String[] args) {
        try {
            System.out.println("🚀 ENHANCED BLOCKCHAIN VALIDATION DEMO");
            System.out.println("=======================================\n");
            
            Blockchain blockchain = new Blockchain();

            // RBAC FIX (v1.0.6): Clear database before bootstrap to avoid "Existing users" error
            blockchain.clearAndReinitialize(); // Start clean

            // Create admin user (required for dangerous operations)
            KeyPair admin = CryptoUtil.generateKeyPair();
            String adminPublicKey = CryptoUtil.publicKeyToString(admin.getPublic());

            // Create test users
            KeyPair user1 = CryptoUtil.generateKeyPair();
            KeyPair user2 = CryptoUtil.generateKeyPair();
            KeyPair user3 = CryptoUtil.generateKeyPair();
            
            String publicKey1 = CryptoUtil.publicKeyToString(user1.getPublic());
            String publicKey2 = CryptoUtil.publicKeyToString(user2.getPublic());
            String publicKey3 = CryptoUtil.publicKeyToString(user3.getPublic());
            
            // Scenario 1: Setup
            System.out.println("📋 SCENARIO 1: Initial setup");
            System.out.println("=============================");

            // First user (admin) - Genesis bootstrap
            blockchain.createBootstrapAdmin(
                adminPublicKey,
                "Administrator"
            );
            // Subsequent users - Created by admin
            blockchain.addAuthorizedKey(
                publicKey1,
                "Alice - Active User",
                admin,  // Admin is the caller
                UserRole.USER
            );
            blockchain.addAuthorizedKey(
                publicKey2,
                "Bob - Inactive User",
                admin,  // Admin is the caller
                UserRole.USER
            );
            blockchain.addAuthorizedKey(
                publicKey3,
                "Charlie - Heavy User",
                admin,  // Admin is the caller
                UserRole.USER
            );
            
            // Add blocks with different signers
            blockchain.addBlock("Alice's first transaction", user1.getPrivate(), user1.getPublic());
            blockchain.addBlock("Charlie's business deal", user3.getPrivate(), user3.getPublic());
            blockchain.addBlock("Alice's second transaction", user1.getPrivate(), user1.getPublic());
            blockchain.addBlock("Charlie's contract signing", user3.getPrivate(), user3.getPublic());
            blockchain.addBlock("Charlie's final payment", user3.getPrivate(), user3.getPublic());
            
            System.out.println("✅ Initial setup complete");
            
            // Scenario 2: ENHANCED validation analysis
            System.out.println("\n🔍 SCENARIO 2: Enhanced validation analysis");
            System.out.println("============================================");
            
            ChainValidationResult initialResult = blockchain.validateChainDetailed();
            System.out.println("📊 Initial validation result:");
            System.out.println("   " + initialResult.getSummary());
            System.out.println("   Structurally intact: " + initialResult.isStructurallyIntact());
            System.out.println("   Fully compliant: " + initialResult.isFullyCompliant());
            System.out.println("   Total blocks: " + initialResult.getTotalBlocks());
            System.out.println("   Valid blocks: " + initialResult.getValidBlocks());
            System.out.println("   Revoked blocks: " + initialResult.getRevokedBlocks());
            
            // Scenario 3: Compare old vs new validation methods
            System.out.println("\n⚖️ SCENARIO 3: Old vs New validation methods");
            System.out.println("=============================================");
            
            boolean oldValidation = blockchain.isStructurallyIntact(); // Using new API equivalent
            boolean newStructuralValidation = blockchain.isStructurallyIntact();
            boolean newFullValidation = blockchain.isFullyCompliant();
            
            System.out.println("   Structural integrity: " + (oldValidation ? "✅ VALID" : "❌ INVALID"));
            System.out.println("   New isStructurallyIntact(): " + (newStructuralValidation ? "✅ INTACT" : "❌ COMPROMISED"));
            System.out.println("   New isFullyCompliant(): " + (newFullValidation ? "✅ COMPLIANT" : "⚠️ NON-COMPLIANT"));
            
            // Scenario 4: Safe deletion
            System.out.println("\n✅ SCENARIO 4: Safe deletion (Bob)");
            System.out.println("===================================");
            
            boolean bobDeleted = blockchain.deleteAuthorizedKey(publicKey2);
            System.out.println("   Bob deletion result: " + (bobDeleted ? "✅ SUCCESS" : "❌ FAILED"));
            
            ChainValidationResult beforeCharlieResult = blockchain.validateChainDetailed();
            System.out.println("   Before Charlie deletion: " + beforeCharlieResult.getSummary());

            // Scenario 5: FORCED dangerous deletion with detailed tracking
            System.out.println("\n💀 SCENARIO 5: Forced deletion with enhanced tracking");
            System.out.println("=====================================================");

            System.out.println("   🔍 Before Charlie deletion:");
            long orphanedBeforeCount = beforeCharlieResult.streamOrphanedBlocks().count();
            System.out.println("      Orphaned blocks before: " + orphanedBeforeCount);
            
            System.out.println("\n   🔥 Performing FORCED deletion of Charlie...");
            // Create admin signature for the forced operation
            String charlieSignature = CryptoUtil.createAdminSignature(publicKey3, true, "Security incident - compromised key", admin.getPrivate());
            boolean charlieForcedDeleted = blockchain.dangerouslyDeleteAuthorizedKey(
                publicKey3, true, "Security incident - compromised key", charlieSignature, adminPublicKey);
            
            System.out.println("   ✅ Deletion result: " + (charlieForcedDeleted ? "SUCCESS" : "FAILED"));
            
            // Scenario 6: ENHANCED post-deletion analysis
            System.out.println("\n📊 SCENARIO 6: Enhanced post-deletion analysis");
            System.out.println("===============================================");
            
            ChainValidationResult finalResult = blockchain.validateChainDetailed();
            System.out.println("📋 DETAILED VALIDATION REPORT:");
            System.out.println(finalResult.getDetailedReport());
            
            // Scenario 7: Different chain views
            System.out.println("\n🔍 SCENARIO 7: Different chain perspectives");
            System.out.println("===========================================");

            ChainValidationResult perspectivesResult = blockchain.validateChainDetailed();
            long fullChainSize = blockchain.getBlockCount();
            long validChainCount = perspectivesResult.streamValidBlocks().count();
            long orphanedCount = perspectivesResult.streamOrphanedBlocks().count();

            System.out.println("   📁 Full chain: " + fullChainSize + " blocks");
            System.out.println("   ✅ Valid chain: " + validChainCount + " blocks");
            System.out.println("   ⚠️ Orphaned blocks: " + orphanedCount + " blocks");

            // Show orphaned block details if any
            if (orphanedCount > 0) {
                System.out.println("\n   🔍 Orphaned block details:");
                perspectivesResult.streamOrphanedBlocks()
                    .limit(10) // Limit output for readability
                    .forEach(block -> {
                        System.out.println("      - Block #" + block.getBlockNumber() +
                                         " (timestamp: " + block.getTimestamp() + ")");
                    });
            }
            
            // Scenario 8: Validation method comparison
            System.out.println("\n⚖️ SCENARIO 8: Method comparison summary");
            System.out.println("========================================");
            
            System.out.println("   MODERN VALIDATION METHODS:");
            System.out.println("      isStructurallyIntact(): " + (blockchain.isStructurallyIntact() ? "✅ INTACT" : "❌ COMPROMISED"));
            System.out.println("      isFullyCompliant(): " + (blockchain.isFullyCompliant() ? "✅ COMPLIANT" : "⚠️ NON-COMPLIANT"));
            System.out.println("      👍 Result: Clear distinction between structural integrity and business rules");
            
            // Scenario 9: Audit report
            System.out.println("\n📋 SCENARIO 9: Complete audit report");
            System.out.println("====================================");
            String auditReport = blockchain.getValidationReport();
            System.out.println(auditReport);
            
            System.out.println("\n🎓 ENHANCED LESSONS LEARNED:");
            System.out.println("=============================");
            System.out.println("1. ✅ New validation system provides granular information");
            System.out.println("2. 🔍 Clear distinction between structural integrity and authorization compliance");
            System.out.println("3. 📊 Multiple chain views for different use cases");
            System.out.println("4. 📋 Detailed audit reports for compliance and debugging");
            System.out.println("5. ⚠️ Revoked blocks are clearly identified but preserve audit trail");
            System.out.println("6. 🎯 Legacy compatibility maintained while adding new capabilities");
            
        } catch (Exception e) {
            System.err.println("❌ Demo error: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Force exit to stop background threads
        System.exit(0);
    }
}
