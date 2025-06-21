package demo;

import com.rbatllet.blockchain.core.Blockchain;
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
            
            // 2. Generate key pairs for two users
            System.out.println("🔐 Generating key pairs...");
            KeyPair userAlice = CryptoUtil.generateKeyPair();
            KeyPair userBob = CryptoUtil.generateKeyPair();
            
            // 3. Authorize the keys
            System.out.println("\n🔑 Authorizing keys...");
            String alicePublicKey = CryptoUtil.publicKeyToString(userAlice.getPublic());
            String bobPublicKey = CryptoUtil.publicKeyToString(userBob.getPublic());
            
            blockchain.addAuthorizedKey(alicePublicKey, "Alice");
            blockchain.addAuthorizedKey(bobPublicKey, "Bob");
            
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
            
            // Show the evolution from old to new API
            System.out.println("📊 Old API (deprecated):");
            boolean isValid = blockchain.validateChain();
            System.out.println("   Result: " + (isValid ? "✅ Valid" : "❌ Invalid") + " (limited information)");
            
            System.out.println("\n📈 New API (recommended):");
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
            System.out.println("   📁 Full chain: " + blockchain.getFullChain().size() + " blocks (audit trail)");
            System.out.println("   ✅ Valid chain: " + blockchain.getValidChain().size() + " blocks (operational use)");
            System.out.println("   ⚠️ Orphaned blocks: " + blockchain.getOrphanedBlocks().size() + " blocks");
            
            System.out.println("\n💡 Migration Benefits Demonstrated:");
            System.out.println("   ✅ More informative validation results");
            System.out.println("   ✅ Clear separation of structural vs compliance issues");
            System.out.println("   ✅ Automatic audit report generation");
            System.out.println("   ✅ Multiple chain perspectives for different use cases");
            System.out.println("   ✅ Better debugging and monitoring capabilities");
            
        } catch (Exception e) {
            System.err.println("❌ Demo error: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("\n=== DEMO COMPLETED ===");
    }
}
