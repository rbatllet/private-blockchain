package demo;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.security.KeyFileLoader;
import com.rbatllet.blockchain.security.UserRole;
import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.validation.ChainValidationResult;

import java.security.KeyPair;

/**
 * Demonstration of basic blockchain functionality
 * Shows how to create a blockchain, add authorized users, and process transactions
 */
public class BlockchainDemo {
    
    public static void main(String[] args) {
        System.out.println("=== 📊 PRIVATE BLOCKCHAIN DEMO ===\n");
        
        try {
            // 1. Create blockchain instance
            Blockchain blockchain = new Blockchain();

            // RBAC FIX (v1.0.6): Clear database before bootstrap to avoid "Existing users" error
            blockchain.clearAndReinitialize();

            // 2. Load bootstrap admin keys (RBAC v1.0.6: Production pattern)
            System.out.println("🔐 Loading bootstrap admin credentials...");
            KeyPair bootstrapKeys = KeyFileLoader.loadKeyPairFromFiles(
                "./keys/genesis-admin.private",
                "./keys/genesis-admin.public"
            );
            System.out.println("✅ Bootstrap admin keys loaded");

            // 3. Register bootstrap admin in blockchain (REQUIRED!)
            System.out.println("🔑 Registering bootstrap admin in blockchain...");
            blockchain.createBootstrapAdmin(
                CryptoUtil.publicKeyToString(bootstrapKeys.getPublic()),
                "BOOTSTRAP_ADMIN"
            );
            System.out.println("✅ Bootstrap admin registered (SUPER_ADMIN)");

            // 4. Generate key pairs for demo users
            System.out.println("\n🔐 Generating key pairs for Alice and Bob...");
            KeyPair userAlice = CryptoUtil.generateKeyPair();
            KeyPair userBob = CryptoUtil.generateKeyPair();

            // 5. Create users with proper RBAC hierarchy
            System.out.println("\n🔑 Creating users with RBAC roles...");
            String alicePublicKey = CryptoUtil.publicKeyToString(userAlice.getPublic());
            String bobPublicKey = CryptoUtil.publicKeyToString(userBob.getPublic());

            // Alice - ADMIN created by SUPER_ADMIN (genesis)
            System.out.println("   Creating Alice as ADMIN (by SUPER_ADMIN)...");
            blockchain.addAuthorizedKey(
                alicePublicKey,
                "Alice",
                bootstrapKeys,  // Caller: genesis SUPER_ADMIN
                UserRole.ADMIN
            );
            System.out.println("   ✅ Alice created with ADMIN role");

            // Bob - USER created by ADMIN (Alice)
            System.out.println("   Creating Bob as USER (by ADMIN Alice)...");
            blockchain.addAuthorizedKey(
                bobPublicKey,
                "Bob",
                userAlice,  // Caller: Alice (ADMIN)
                UserRole.USER
            );
            System.out.println("   ✅ Bob created with USER role");
            
            // 4. Add some blocks to the chain
            System.out.println("\n🧱 Adding blocks to blockchain...");
            
            blockchain.addBlock("First transaction: Alice sends data", 
                              userAlice.getPrivate(), userAlice.getPublic());
            
            blockchain.addBlock("Second transaction: Bob receives data", 
                              userBob.getPrivate(), userBob.getPublic());
            
            blockchain.addBlock("Third transaction: Alice updates record", 
                              userAlice.getPrivate(), userAlice.getPublic());
            
            // 5. Enhanced validation with detailed information
            System.out.println("\n🔍 Enhanced blockchain validation...");
            
            // Modern API with comprehensive validation results
            System.out.println("📈 Comprehensive validation with detailed analysis:");
            ChainValidationResult result = blockchain.validateChainDetailed();
            System.out.println("   🏗️ Structural integrity: " + (result.isStructurallyIntact() ? "✅ Intact" : "❌ Compromised"));
            System.out.println("   ✅ Full compliance: " + (result.isFullyCompliant() ? "✅ Compliant" : "⚠️ Non-compliant"));
            System.out.println("   📋 Detailed summary: " + result.getSummary());
            
            // Show additional insights available with new API
            if (result.getRevokedBlocks() > 0) {
                System.out.println("   ⚠️ Found " + result.getRevokedBlocks() + " revoked blocks (audit trail preserved)");
            }
            
            // 6. Generate audit report
            System.out.println("\n📋 Generating audit report...");
            String auditReport = blockchain.getValidationReport();
            System.out.println("✅ Audit report generated (" + auditReport.split("\n").length + " lines)");
            
            // Show first few lines of audit report
            String[] reportLines = auditReport.split("\n");
            System.out.println("📄 Report preview:");
            for (int i = 0; i < Math.min(3, reportLines.length); i++) {
                System.out.println("   " + reportLines[i]);
            }
            if (reportLines.length > 3) {
                System.out.println("   ... (see full report for complete details)");
            }
            
            // 7. Display enhanced chain information
            System.out.println("\n=== 📊 ENHANCED BLOCKCHAIN STATUS ===");
            System.out.println("📦 Total blocks: " + result.getTotalBlocks());
            System.out.println("✅ Valid blocks: " + result.getValidBlocks());
            System.out.println("⚠️ Revoked blocks: " + result.getRevokedBlocks());
            System.out.println("🔑 Authorized keys: " + blockchain.getAuthorizedKeys().size());
            
            // Show different chain views available
            System.out.println("\n🔍 Available chain views:");
            System.out.println("   📁 Full chain: " + blockchain.getBlockCount() + " blocks (audit trail)");
            System.out.println("   ✅ Valid chain: " + blockchain.getValidChain().size() + " blocks (operational use)");
            System.out.println("   ⚠️ Orphaned blocks: " + blockchain.getOrphanedBlocks().size() + " blocks");
            
            System.out.println("\n💡 Modern API Benefits:");
            System.out.println("   ✅ Comprehensive validation with detailed diagnostics");
            System.out.println("   ✅ Clear separation of structural vs compliance issues");
            System.out.println("   ✅ Automatic audit report generation");
            System.out.println("   ✅ Multiple chain perspectives for different use cases");
            System.out.println("   ✅ Enhanced debugging and monitoring capabilities");
            
        } catch (Exception e) {
            System.err.println("❌ Demo error: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("\n=== DEMO COMPLETED ===");
        
        // Force exit to stop background threads
        System.exit(0);
    }
}
