package demo;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.search.SearchSpecialistAPI;
import com.rbatllet.blockchain.search.SearchFrameworkEngine.EnhancedSearchResult;
import com.rbatllet.blockchain.service.UserFriendlyEncryptionAPI;
import java.security.KeyPair;
import java.util.List;

/**
 * Demo to show the difference between old incorrect usage and new correct usage
 */
public class SearchSpecialistAPIErrorDemo {
    
    public static void main(String[] args) {
        System.out.println("🔍 SEARCHSPECIALISTAPI USAGE COMPARISON DEMO");
        System.out.println("=============================================");
        System.out.println();
        
        // Show the old incorrect way (still works but deprecated)
        System.out.println("❌ OLD WAY: Creating SearchSpecialistAPI without parameters...");
        SearchSpecialistAPI wrongAPI = new SearchSpecialistAPI();
        System.out.println("✅ Instance created (warning should be logged)");
        
        System.out.println();
        System.out.println("🔍 Attempting to search with uninitialized API...");
        List<EnhancedSearchResult> results = wrongAPI.searchSimple("test");
        System.out.println("Results: " + results.size() + " (expected: 0)");
        
        System.out.println();
        System.out.println("==============================================");
        System.out.println();
        
        // Show the new correct way
        System.out.println("✅ NEW WAY: Creating SearchSpecialistAPI with blockchain and password...");
        try {
            Blockchain blockchain = new Blockchain();
            String password = "Demo2024!";
            
            // Set up some test data
            UserFriendlyEncryptionAPI dataAPI = new UserFriendlyEncryptionAPI(blockchain);
            KeyPair userKeys = dataAPI.createUser("demo-user");
            dataAPI.setDefaultCredentials("demo-user", userKeys);
            dataAPI.storeSearchableData("Test data", password, new String[]{"test", "demo"});
            
            // Create SearchSpecialistAPI with proper constructor
            SearchSpecialistAPI correctAPI = new SearchSpecialistAPI(blockchain, password, userKeys.getPrivate());
            System.out.println("✅ SearchSpecialistAPI created and automatically initialized");
            
            // Test search - should work correctly
            System.out.println("🔍 Searching with properly initialized API...");
            List<EnhancedSearchResult> correctResults = correctAPI.searchSimple("test");
            System.out.println("Results: " + correctResults.size() + " (expected: > 0)");
            
            System.out.println();
            System.out.println("📋 Summary:");
            System.out.println("   ❌ Old way: SearchSpecialistAPI() - warns and likely returns 0 results");
            System.out.println("   ✅ New way: SearchSpecialistAPI(blockchain, password) - works correctly");
            
        } catch (Exception e) {
            System.err.println("❌ Error in new way demo: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Force exit to stop background threads
        System.exit(0);
    }
}