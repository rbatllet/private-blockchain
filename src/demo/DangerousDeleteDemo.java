import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.validation.ChainValidationResult;

import java.security.KeyPair;

/**
 * Demonstration of the new dangerous key deletion functionality
 * Shows safe deletion, blocked deletion, and forced deletion scenarios
 */
public class DangerousDeleteDemo {
    
    public static void main(String[] args) {
        try {
            System.out.println("🚀 DANGEROUS DELETE AUTHORIZATION KEY DEMO");
            System.out.println("============================================\n");
            
            Blockchain blockchain = new Blockchain();
            blockchain.clearAndReinitialize(); // Start clean
            
            // Create test users
            KeyPair user1 = CryptoUtil.generateKeyPair();
            KeyPair user2 = CryptoUtil.generateKeyPair();
            KeyPair user3 = CryptoUtil.generateKeyPair();
            
            String publicKey1 = CryptoUtil.publicKeyToString(user1.getPublic());
            String publicKey2 = CryptoUtil.publicKeyToString(user2.getPublic());
            String publicKey3 = CryptoUtil.publicKeyToString(user3.getPublic());
            
            // Scenario 1: Add users and create some blockchain activity
            System.out.println("📋 SCENARIO 1: Setting up blockchain with multiple users");
            System.out.println("========================================================");
            
            blockchain.addAuthorizedKey(publicKey1, "Alice - Active User");
            blockchain.addAuthorizedKey(publicKey2, "Bob - Inactive User");  
            blockchain.addAuthorizedKey(publicKey3, "Charlie - Heavy User");
            
            // Add blocks with different signers
            blockchain.addBlock("Alice's first transaction", user1.getPrivate(), user1.getPublic());
            blockchain.addBlock("Charlie's business deal", user3.getPrivate(), user3.getPublic());
            blockchain.addBlock("Alice's second transaction", user1.getPrivate(), user1.getPublic());
            blockchain.addBlock("Charlie's contract signing", user3.getPrivate(), user3.getPublic());
            blockchain.addBlock("Charlie's final payment", user3.getPrivate(), user3.getPublic());
            
            System.out.println("✅ Initial setup complete:");
            System.out.println("   - Alice: 2 blocks signed");
            System.out.println("   - Bob: 0 blocks signed");  
            System.out.println("   - Charlie: 3 blocks signed");
            System.out.println("   - Total blocks: " + blockchain.getBlockCount());
            
            // Enhanced validation for initial state
            ChainValidationResult initialResult = blockchain.validateChainDetailed();
            System.out.println("   - Enhanced validation: " + initialResult.getSummary());
            System.out.println("   - Structural integrity: " + (initialResult.isStructurallyIntact() ? "✅ Intact" : "❌ Compromised"));
            System.out.println("   - Full compliance: " + (initialResult.isFullyCompliant() ? "✅ Compliant" : "⚠️ Issues"));
            System.out.println();
            
            // Scenario 2: Analysis before deletion
            System.out.println("🔍 SCENARIO 2: Impact analysis before deletion");
            System.out.println("==============================================");
            
            System.out.println("🔍 Checking deletion impact for each user:\n");
            
            Blockchain.KeyDeletionImpact aliceImpact = blockchain.canDeleteAuthorizedKey(publicKey1);
            System.out.println("👩 Alice: " + aliceImpact);
            System.out.println("   Safe to delete: " + aliceImpact.canSafelyDelete());
            
            Blockchain.KeyDeletionImpact bobImpact = blockchain.canDeleteAuthorizedKey(publicKey2);
            System.out.println("👨 Bob: " + bobImpact);  
            System.out.println("   Safe to delete: " + bobImpact.canSafelyDelete());
            
            Blockchain.KeyDeletionImpact charlieImpact = blockchain.canDeleteAuthorizedKey(publicKey3);
            System.out.println("👨‍💼 Charlie: " + charlieImpact);
            System.out.println("   Safe to delete: " + charlieImpact.canSafelyDelete());
            System.out.println();
            
            // Scenario 3: Safe deletion
            System.out.println("✅ SCENARIO 3: Safe deletion (Bob - no blocks signed)");
            System.out.println("=====================================================");
            
            System.out.println("🎯 Attempting to safely delete Bob's key...");
            boolean bobDeleted = blockchain.deleteAuthorizedKey(publicKey2);
            System.out.println("Result: " + (bobDeleted ? "✅ SUCCESS" : "❌ FAILED"));
            
            // Enhanced validation after Bob's deletion
            ChainValidationResult bobResult = blockchain.validateChainDetailed();
            System.out.println("Enhanced validation: " + bobResult.getSummary());
            System.out.println("Structural integrity: " + (bobResult.isStructurallyIntact() ? "✅ Intact" : "❌ Compromised"));
            System.out.println("Active keys remaining: " + blockchain.getAuthorizedKeys().size());
            System.out.println();
            
            // Scenario 4: Blocked deletion
            System.out.println("🚫 SCENARIO 4: Blocked deletion (Alice - has signed blocks)");
            System.out.println("============================================================");
            
            System.out.println("🎯 Attempting to safely delete Alice's key...");
            boolean aliceDeleted = blockchain.deleteAuthorizedKey(publicKey1);
            System.out.println("Result: " + (aliceDeleted ? "✅ SUCCESS" : "❌ BLOCKED (as expected)"));
            
            // Enhanced validation after Alice's blocked deletion
            ChainValidationResult aliceResult = blockchain.validateChainDetailed();
            System.out.println("Enhanced validation: " + aliceResult.getSummary());
            System.out.println("Structural integrity: " + (aliceResult.isStructurallyIntact() ? "✅ Intact" : "❌ Compromised"));
            System.out.println();
            
            // Scenario 5: Dangerous deletion without force (should fail)
            System.out.println("⚠️ SCENARIO 5: Dangerous deletion without force");
            System.out.println("===============================================");
            
            System.out.println("🎯 Attempting dangerous deletion of Alice without force...");
            boolean aliceDangerousDeleted = blockchain.dangerouslyDeleteAuthorizedKey(publicKey1, "GDPR compliance request");
            System.out.println("Result: " + (aliceDangerousDeleted ? "✅ SUCCESS" : "❌ BLOCKED (safety engaged)"));
            System.out.println();
            
            // Scenario 6: Forced dangerous deletion
            System.out.println("💀 SCENARIO 6: FORCED dangerous deletion (IRREVERSIBLE!)");
            System.out.println("=========================================================");
            
            System.out.println("🎯 Before forced deletion:");
            
            // Enhanced validation before forced deletion
            ChainValidationResult beforeForced = blockchain.validateChainDetailed();
            System.out.println("   - Enhanced validation: " + beforeForced.getSummary());
            System.out.println("   - Structural integrity: " + (beforeForced.isStructurallyIntact() ? "✅ Intact" : "❌ Compromised"));
            System.out.println("   - Full compliance: " + (beforeForced.isFullyCompliant() ? "✅ Compliant" : "⚠️ Issues"));
            System.out.println("   - Charlie's blocks: " + charlieImpact.getAffectedBlocks());
            
            System.out.println("\n🔥 Performing FORCED deletion of Charlie's key...");
            boolean charlieForcedDeleted = blockchain.dangerouslyDeleteAuthorizedKey(
                publicKey3, true, "Security incident - compromised key");
            
            System.out.println("\n🎯 After forced deletion:");
            System.out.println("   - Deletion result: " + (charlieForcedDeleted ? "✅ SUCCESS" : "❌ FAILED"));
            
            // Enhanced validation after forced deletion
            ChainValidationResult afterForced = blockchain.validateChainDetailed();
            System.out.println("   - Enhanced validation: " + afterForced.getSummary());
            System.out.println("   - Structural integrity: " + (afterForced.isStructurallyIntact() ? "✅ Intact" : "❌ Compromised"));
            System.out.println("   - Full compliance: " + (afterForced.isFullyCompliant() ? "✅ Compliant" : "⚠️ Issues"));
            System.out.println("   - Valid blocks: " + afterForced.getValidBlocks() + "/" + afterForced.getTotalBlocks());
            System.out.println("   - Revoked blocks: " + afterForced.getRevokedBlocks());
            System.out.println("   - Active keys remaining: " + blockchain.getAuthorizedKeys().size());
            
            // Show the damage
            System.out.println("\n💥 Impact assessment:");
            System.out.println("   - Charlie's 3 blocks are now ORPHANED");
            System.out.println("   - Blockchain validation FAILS for these blocks");
            System.out.println("   - Historical integrity is COMPROMISED");
            System.out.println("   - This demonstrates why the safety checks exist!");
            
            // Scenario 7: Final state
            System.out.println("\n📊 SCENARIO 7: Final blockchain state");
            System.out.println("=====================================");
            
            System.out.println("Remaining authorized keys:");
            blockchain.getAuthorizedKeys().forEach(key -> 
                System.out.println("   - " + key.getOwnerName() + " (created: " + key.getCreatedAt() + ")"));
            
            // Enhanced final state analysis
            ChainValidationResult finalState = blockchain.validateChainDetailed();
            System.out.println("\nEnhanced final blockchain analysis:");
            System.out.println("   📊 Summary: " + finalState.getSummary());
            System.out.println("   🏗️ Structural integrity: " + (finalState.isStructurallyIntact() ? "✅ Intact" : "❌ Compromised"));
            System.out.println("   ✅ Full compliance: " + (finalState.isFullyCompliant() ? "✅ Compliant" : "⚠️ Issues"));
            System.out.println("   📋 Total blocks: " + finalState.getTotalBlocks());
            System.out.println("   ✅ Valid blocks: " + finalState.getValidBlocks());
            System.out.println("   ⚠️ Revoked blocks: " + finalState.getRevokedBlocks());
            
            // Show audit report capability
            System.out.println("\n📋 Generated audit report:");
            String auditReport = blockchain.getValidationReport();
            String[] reportLines = auditReport.split("\n");
            for (int i = 0; i < Math.min(5, reportLines.length); i++) {
                System.out.println("   " + reportLines[i]);
            }
            if (reportLines.length > 5) {
                System.out.println("   ... (report continues)");
            }
            
            System.out.println("\n🎓 ENHANCED LESSONS LEARNED:");
            System.out.println("===================");
            System.out.println("1. ✅ Safe deletion works for keys without historical blocks");
            System.out.println("2. 🚫 Regular deletion is blocked for keys with signed blocks");
            System.out.println("3. ⚠️ Dangerous deletion without force is still safely blocked");
            System.out.println("4. 💀 Forced deletion works but impacts validation compliance");
            System.out.println("5. 🔍 Always use canDeleteAuthorizedKey() to assess impact first");
            System.out.println("6. 🛡️ Use force=true only in extreme circumstances (GDPR, security incidents)");
            System.out.println("7. 📊 NEW: Enhanced validation API provides granular impact analysis");
            System.out.println("8. 🏗️ NEW: Structural integrity vs compliance distinction is crucial");
            System.out.println("9. 📋 NEW: Automatic audit reports help with compliance and debugging");
            System.out.println("10. ⚠️ NEW: Revoked blocks are tracked but chain structure remains intact");
            
        } catch (Exception e) {
            System.err.println("❌ Demo error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
