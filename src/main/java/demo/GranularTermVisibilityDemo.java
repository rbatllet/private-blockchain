package demo;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.search.metadata.TermVisibilityMap;
import com.rbatllet.blockchain.service.UserFriendlyEncryptionAPI;
import com.rbatllet.blockchain.util.CryptoUtil;

import java.security.KeyPair;
import java.util.List;
import java.util.Set;

/**
 * GRANULAR TERM VISIBILITY DEMONSTRATION
 * 
 * Showcases the advanced granular term visibility control feature that allows
 * users to specify exactly which search terms should be public vs private.
 * 
 * This enables fine-grained privacy control where general terms can be
 * publicly searchable while sensitive details require password authentication.
 */
public class GranularTermVisibilityDemo {
    
    public static void main(String[] args) {
        try {
            System.out.println("🔐 GRANULAR TERM VISIBILITY DEMO");
            System.out.println("=================================");
            
            // Setup
            Blockchain blockchain = new Blockchain();
            KeyPair keyPair = CryptoUtil.generateKeyPair();
            UserFriendlyEncryptionAPI api = new UserFriendlyEncryptionAPI(blockchain, "demo_user", keyPair);
            String password = "demo_password_123";
            
            // Demo scenarios
            demonstrateBasicGranularControl(api, password);
            demonstrateMedicalRecordExample(api, password);
            demonstrateFinancialDataExample(api, password);
            demonstrateSearchBehavior(api, password, blockchain);
            
            System.out.println("✅ Granular term visibility demo completed successfully!");
            
        } catch (Exception e) {
            System.err.println("❌ Demo failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void demonstrateBasicGranularControl(UserFriendlyEncryptionAPI api, String password) {
        System.out.println("\\n📋 DEMO 1: BASIC GRANULAR CONTROL");
        System.out.println("==================================");
        
        // Create a visibility map
        Set<String> allTerms = Set.of("patient", "john", "smith", "diagnosis", "cancer", "treatment");
        
        TermVisibilityMap visibility = new TermVisibilityMap()
            .setPublic("patient", "diagnosis", "treatment")     // Medical terms - public
            .setPrivate("john", "smith", "cancer");            // Personal info - private
        
        System.out.println("📊 Term Visibility Configuration:");
        System.out.println(visibility.toDetailedString());
        
        // Store data with granular control
        String medicalData = "Patient John Smith diagnosed with cancer, starting treatment protocol.";
        Block block = api.storeDataWithGranularTermControl(medicalData, password, allTerms, visibility);
        
        if (block != null) {
            System.out.printf("✅ Stored block %s with granular term control\\n", 
                            block.getHash().substring(0, 8));
        }
    }
    
    private static void demonstrateMedicalRecordExample(UserFriendlyEncryptionAPI api, String password) {
        System.out.println("\\n🏥 DEMO 2: MEDICAL RECORD PRIVACY");
        System.out.println("==================================");
        
        String medicalRecord = "Dr. Sarah Johnson examined patient Maria Rodriguez for chronic diabetes. " +
                             "Blood sugar levels elevated. Prescribed insulin therapy and dietary changes.";
        
        // Medical terms public, personal identifiers private
        String[] publicTerms = {"medical", "doctor", "patient", "diabetes", "blood", "sugar", "insulin", "therapy"};
        String[] privateTerms = {"sarah", "johnson", "maria", "rodriguez"};
        
        System.out.println("🔍 Public searchable terms: " + String.join(", ", publicTerms));
        System.out.println("🔐 Private searchable terms: " + String.join(", ", privateTerms));
        
        Block block = api.storeDataWithSeparatedTerms(medicalRecord, password, publicTerms, privateTerms);
        
        if (block != null) {
            System.out.printf("✅ Medical record stored: %s\\n", block.getHash().substring(0, 8));
            System.out.println("   📋 Anyone can search for 'diabetes' or 'insulin'");
            System.out.println("   🔐 Only authorized users can search for 'Maria Rodriguez'");
        }
    }
    
    private static void demonstrateFinancialDataExample(UserFriendlyEncryptionAPI api, String password) {
        System.out.println("\\n💰 DEMO 3: FINANCIAL DATA PRIVACY");
        System.out.println("==================================");
        
        String financialData = "SWIFT transfer of $50,000 from account 123-456-789 to John Smith. " +
                             "Transaction ID: TX789012. Purpose: real estate purchase.";
        
        Set<String> allTerms = Set.of("swift", "transfer", "50000", "account", "123-456-789", 
                                    "john", "smith", "tx789012", "real", "estate");
        
        TermVisibilityMap visibility = new TermVisibilityMap(TermVisibilityMap.VisibilityLevel.PRIVATE)
            .setPublic("swift", "transfer", "real", "estate")           // Transaction type - public
            .setPrivate("50000", "123-456-789", "john", "smith", "tx789012"); // Sensitive data - private
        
        System.out.println("💡 Configuration: Default PRIVATE, selected terms PUBLIC");
        System.out.println("📊 Visibility Map:");
        System.out.println(visibility.toDetailedString());
        
        Block block = api.storeDataWithGranularTermControl(financialData, password, allTerms, visibility);
        
        if (block != null) {
            System.out.printf("✅ Financial record stored: %s\\n", block.getHash().substring(0, 8));
            System.out.println("   📋 Public: Can search for 'swift transfer' or 'real estate'");
            System.out.println("   🔐 Private: Amounts, account numbers, names need password");
        }
    }
    
    private static void demonstrateSearchBehavior(UserFriendlyEncryptionAPI api, String password, Blockchain blockchain) {
        System.out.println("\\n🔍 DEMO 4: SEARCH BEHAVIOR WITH GRANULAR CONTROL");
        System.out.println("================================================");
        
        // Ensure indexing is complete by checking search readiness
        System.out.println("🔍 Verifying search system readiness...");
        
        // Actually verify that the search system is ready by checking if we can perform a search
        try {
            var searchResults = api.searchAndDecryptByTerms(new String[]{"test"}, password, 1);
            System.out.println("   Search system ready: ✅ Yes (test search returned " + searchResults.size() + " results)");
        } catch (Exception e) {
            System.out.println("   Search system ready: ❌ No (" + e.getMessage() + ")");
        }
        
        // Verify that we have the expected number of blocks by checking the blockchain state
        try {
            var allBlocks = blockchain.getAllBlocks();
            int blockCount = allBlocks.size();
            System.out.println("   Total blocks created: " + blockCount);
            
            if (blockCount < 4) {
                System.out.println("   ⚠️  Warning: Expected at least 4 blocks, got " + blockCount);
            }
        } catch (Exception e) {
            System.out.println("   ⚠️  Could not retrieve block count: " + e.getMessage());
        }
        
        // Test public searches (no password)
        System.out.println("🌍 PUBLIC SEARCHES (no password required):");
        
        List<Block> publicResults = api.searchByTerms(new String[]{"patient", "diagnosis"}, null, 10);
        System.out.printf("   'patient diagnosis': %d results\\n", publicResults.size());
        
        publicResults = api.searchByTerms(new String[]{"swift", "transfer"}, null, 10);
        System.out.printf("   'swift transfer': %d results\\n", publicResults.size());
        
        publicResults = api.searchByTerms(new String[]{"real", "estate"}, null, 10);
        System.out.printf("   'real estate': %d results\\n", publicResults.size());
        
        // Test searches that should require password
        System.out.println("\\n🔐 PRIVATE SEARCHES (password protected):");
        
        publicResults = api.searchByTerms(new String[]{"john", "smith"}, null, 10);
        System.out.printf("   'john smith' (no password): %d results\\n", publicResults.size());
        
        List<Block> privateResults = api.searchAndDecryptByTerms(new String[]{"john", "smith"}, password, 10);
        System.out.printf("   'john smith' (with password): %d results\\n", privateResults.size());
        
        privateResults = api.searchAndDecryptByTerms(new String[]{"cancer"}, password, 10);
        System.out.printf("   'cancer' (with password): %d results\\n", privateResults.size());
        
        privateResults = api.searchAndDecryptByTerms(new String[]{"50000"}, password, 10);
        System.out.printf("   '50000' (with password): %d results\\n", privateResults.size());
        
        System.out.println("\\n💡 Notice how sensitive terms require password authentication!");
    }
}