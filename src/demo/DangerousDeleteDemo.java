package com.rbatllet.demo;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.util.CryptoUtil;

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
            System.out.println("   - Chain valid: " + blockchain.validateChain());
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
            System.out.println("Chain still valid: " + blockchain.validateChain());
            System.out.println("Active keys remaining: " + blockchain.getAuthorizedKeys().size());
            System.out.println();
            
            // Scenario 4: Blocked deletion
            System.out.println("🚫 SCENARIO 4: Blocked deletion (Alice - has signed blocks)");
            System.out.println("============================================================");
            
            System.out.println("🎯 Attempting to safely delete Alice's key...");
            boolean aliceDeleted = blockchain.deleteAuthorizedKey(publicKey1);
            System.out.println("Result: " + (aliceDeleted ? "✅ SUCCESS" : "❌ BLOCKED (as expected)"));
            System.out.println("Chain still valid: " + blockchain.validateChain());
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
            System.out.println("   - Chain valid: " + blockchain.validateChain());
            System.out.println("   - Charlie's blocks: " + charlieImpact.getAffectedBlocks());
            
            System.out.println("\n🔥 Performing FORCED deletion of Charlie's key...");
            boolean charlieForcedDeleted = blockchain.dangerouslyDeleteAuthorizedKey(
                publicKey3, true, "Security incident - compromised key");
            
            System.out.println("\n🎯 After forced deletion:");
            System.out.println("   - Deletion result: " + (charlieForcedDeleted ? "✅ SUCCESS" : "❌ FAILED"));
            System.out.println("   - Chain valid: " + blockchain.validateChain());
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
            
            System.out.println("\nBlockchain integrity: " + (blockchain.validateChain() ? "✅ VALID" : "❌ COMPROMISED"));
            System.out.println("Total blocks: " + blockchain.getBlockCount());
            
            System.out.println("\n🎓 LESSONS LEARNED:");
            System.out.println("===================");
            System.out.println("1. ✅ Safe deletion works for keys without historical blocks");
            System.out.println("2. 🚫 Regular deletion is blocked for keys with signed blocks");
            System.out.println("3. ⚠️ Dangerous deletion without force is still safely blocked");
            System.out.println("4. 💀 Forced deletion works but BREAKS blockchain validation");
            System.out.println("5. 🔍 Always use canDeleteAuthorizedKey() to assess impact first");
            System.out.println("6. 🛡️ Use force=true only in extreme circumstances (GDPR, security incidents)");
            
        } catch (Exception e) {
            System.err.println("❌ Demo error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
