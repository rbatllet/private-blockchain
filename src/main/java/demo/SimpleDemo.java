package demo;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.indexing.IndexingCoordinator;
import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.validation.ChainValidationResult;

import java.security.KeyPair;

public class SimpleDemo {
    
    public static void main(String[] args) {
        System.out.println("=== 📊 SIMPLE BLOCKCHAIN DEMO ===\n");
        
        try {
            // 1. Create blockchain instance
            Blockchain blockchain = new Blockchain();
            System.out.println("✅ Blockchain created");
            
            // 2. Generate key pair
            KeyPair keyPair = CryptoUtil.generateKeyPair();
            String publicKeyString = CryptoUtil.publicKeyToString(keyPair.getPublic());

            // 3. Create bootstrap admin (RBAC v1.0.6)
            blockchain.createBootstrapAdmin(publicKeyString, "TestUser");
            System.out.println("🔑 Bootstrap admin created");
            
            // 4. Add one block
            boolean added = blockchain.addBlock("Test data", keyPair.getPrivate(), keyPair.getPublic());
            System.out.println("🧱 Block added: " + added);
            
            // 5. Iterate blocks and print details
            System.out.println("\n=== 📦 BLOCKS ===");
            blockchain.processChainInBatches(batch -> {
                for (Block block : batch) {
                    System.out.println("🧱 Block #" + Long.toString(block.getBlockNumber()) + ": " + block.getData());
                    System.out.println("  🔢 Hash: " + block.getHash());
                    System.out.println("  ⬅️ Previous: " + block.getPreviousHash());
                    System.out.println("  🔎 Signer: " + (block.getSignerPublicKey() != null ?
                        (block.getSignerPublicKey().length() > 20 ?
                            block.getSignerPublicKey().substring(0, 20) + "..." :
                            block.getSignerPublicKey()) : "null"));
                }
            }, 1000);
            
            // 6. Enhanced chain validation
            System.out.println("\n=== 🔍 ENHANCED VALIDATION ===");
            
            // Simple validation check
            ChainValidationResult quickCheck = blockchain.validateChainDetailed();
            boolean isValid = quickCheck.isStructurallyIntact();
            System.out.println("📊 Quick validation: " + (isValid ? "✅ Valid" : "❌ Invalid"));
            
            // Detailed validation information
            ChainValidationResult result = blockchain.validateChainDetailed();
            System.out.println("📈 New API results:");
            System.out.println("   🏗️ Structurally intact: " + (result.isStructurallyIntact() ? "✅ Yes" : "❌ No"));
            System.out.println("   ✅ Fully compliant: " + (result.isFullyCompliant() ? "✅ Yes" : "⚠️ No"));
            System.out.println("   📊 Summary: " + result.getSummary());
            System.out.println("   📋 Total blocks: " + result.getTotalBlocks());
            System.out.println("   ✅ Valid blocks: " + result.getValidBlocks());
            System.out.println("   ⚠️ Revoked blocks: " + result.getRevokedBlocks());
            
            // Show detailed validation report for debugging
            System.out.println("\n📋 Detailed Validation Report:");
            System.out.println(result.getDetailedReport());
            
            // Wait for background indexing to complete
            System.out.println("\n⏳ Waiting for background indexing to complete...");
            IndexingCoordinator.getInstance().waitForCompletion();
            System.out.println("✅ Background indexing completed - all blocks indexed\n");
            
            // Show search statistics for monitoring
            System.out.println("\n📊 Search Statistics:");
            System.out.println(blockchain.getSearchStatistics());
            
            System.out.println("\n💡 Benefits of detailed validation API:");
            System.out.println("   • Clear distinction between structural and compliance issues");
            System.out.println("   • Detailed statistics for monitoring and debugging");
            System.out.println("   • Better decision-making for applications");
            System.out.println("   • Comprehensive audit trail information");
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Force exit to stop background threads
        System.exit(0);
    }
}
