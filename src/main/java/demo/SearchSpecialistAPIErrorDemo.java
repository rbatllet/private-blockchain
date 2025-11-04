package demo;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.search.SearchSpecialistAPI;
import com.rbatllet.blockchain.search.SearchFrameworkEngine.EnhancedSearchResult;
import com.rbatllet.blockchain.security.KeyFileLoader;
import com.rbatllet.blockchain.service.UserFriendlyEncryptionAPI;
import java.security.KeyPair;
import java.util.List;

/**
 * Demo to show proper SearchSpecialistAPI usage after API simplification.
 *
 * The SearchSpecialistAPI has been simplified - the empty constructor has been removed
 * and all instances are now automatically initialized upon creation.
 *
 * Key changes:
 * - No empty constructor available
 * - All constructors auto-initialize the API
 * - No manual initialization required
 * - No IllegalStateException for uninitialized state
 */
public class SearchSpecialistAPIErrorDemo {

    public static void main(String[] args) {
        System.out.println("🔍 SIMPLIFIED SEARCHSPECIALISTAPI DEMO");
        System.out.println("=========================================");
        System.out.println("After API simplification - no empty constructor!");
        System.out.println();

        // Show the ONLY way to create SearchSpecialistAPI (simplified API)
        System.out.println("✅ Creating SearchSpecialistAPI with required parameters...");

        try {
            // Create blockchain (auto-creates genesis admin)
            Blockchain blockchain = new Blockchain();
            String password = "DemoPassword123!";

            // Load genesis admin keys
            KeyPair genesisKeys = KeyFileLoader.loadKeyPairFromFiles(
                "./keys/genesis-admin.private",
                "./keys/genesis-admin.public"
            );

            SearchSpecialistAPI api = new SearchSpecialistAPI(blockchain, password, genesisKeys.getPrivate());
            System.out.println("✅ Instance created and initialized automatically");

            System.out.println();
            System.out.println("🔍 Testing search with properly initialized API...");
            List<EnhancedSearchResult> results = api.searchAll("test");
            System.out.println("Results: " + results.size() + " (works correctly)");

            System.out.println();
            System.out.println("🔧 Adding some test data and searching again...");

            // Set up some test data with genesis admin
            UserFriendlyEncryptionAPI dataAPI = new UserFriendlyEncryptionAPI(blockchain);
            dataAPI.setDefaultCredentials("GENESIS_ADMIN", genesisKeys);

            // Create demo user (authorized by genesis admin)
            KeyPair userKeys = dataAPI.createUser("demo-user");
            dataAPI.setDefaultCredentials("demo-user", userKeys);
            dataAPI.storeSearchableData("Test searchable data", password, new String[]{"test", "demo"});
            
            // Create new SearchSpecialistAPI with the updated blockchain
            SearchSpecialistAPI updatedAPI = new SearchSpecialistAPI(blockchain, password, userKeys.getPrivate());
            System.out.println("✅ SearchSpecialistAPI created with test data");
            
            // Test search with data
            System.out.println("🔍 Searching for 'test' in blockchain with data...");
            List<EnhancedSearchResult> dataResults = updatedAPI.searchAll("test");
            System.out.println("Results with data: " + dataResults.size());
            
            System.out.println();
            System.out.println("📋 API Simplification Summary:");
            System.out.println("   ❌ Empty constructor removed - was causing initialization issues");
            System.out.println("   ✅ Required: SearchSpecialistAPI(blockchain, password, privateKey)");
            System.out.println("   ✅ Automatic initialization - no manual setup required");
            System.out.println("   ✅ Immediate usage - ready to search right away");
            System.out.println("   ✅ No more IllegalStateException for uninitialized API");
            
        } catch (Exception e) {
            System.err.println("❌ Error in demo: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.exit(0);
    }
}