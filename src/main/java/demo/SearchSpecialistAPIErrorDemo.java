package demo;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.indexing.IndexingCoordinator;
import com.rbatllet.blockchain.search.SearchSpecialistAPI;
import com.rbatllet.blockchain.search.SearchFrameworkEngine.EnhancedSearchResult;
import com.rbatllet.blockchain.security.KeyFileLoader;
import com.rbatllet.blockchain.service.UserFriendlyEncryptionAPI;
import com.rbatllet.blockchain.util.CryptoUtil;
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
            // Create blockchain (auto-creates bootstrap admin)
            Blockchain blockchain = new Blockchain();

            // RBAC FIX (v1.0.6): Clear database before bootstrap to avoid "Existing users" error
            blockchain.clearAndReinitialize();
            
            String password = "DemoPassword123!";

            // Load bootstrap admin keys
            KeyPair bootstrapKeys = KeyFileLoader.loadKeyPairFromFiles(
                "./keys/genesis-admin.private",
                "./keys/genesis-admin.public"
            );

            // Register bootstrap admin in blockchain (REQUIRED!)
            blockchain.createBootstrapAdmin(
                CryptoUtil.publicKeyToString(bootstrapKeys.getPublic()),
                "BOOTSTRAP_ADMIN"
            );

            SearchSpecialistAPI api = new SearchSpecialistAPI(blockchain, password, bootstrapKeys.getPrivate());
            System.out.println("✅ Instance created and initialized automatically");

            System.out.println();
            System.out.println("🔍 Testing search with properly initialized API...");
            List<EnhancedSearchResult> results = api.searchAll("test");
            System.out.println("Results: " + results.size() + " (works correctly)");

            System.out.println();
            System.out.println("🔧 Adding some test data and searching again...");

            // Set up some test data with bootstrap admin
            UserFriendlyEncryptionAPI dataAPI = new UserFriendlyEncryptionAPI(blockchain);
            dataAPI.setDefaultCredentials("BOOTSTRAP_ADMIN", bootstrapKeys);

            // Create demo user (authorized by bootstrap admin)
            KeyPair userKeys = dataAPI.createUser("demo-user");
            dataAPI.setDefaultCredentials("demo-user", userKeys);
            dataAPI.storeSearchableData("Test searchable data", password, new String[]{"test", "demo"});
            
            // Wait for background indexing to complete
            try {
                System.out.println("\n⏳ Waiting for background indexing to complete...");
                IndexingCoordinator.getInstance().waitForCompletion();
                System.out.println("✅ Background indexing completed - all blocks indexed\n");
            } catch (InterruptedException e) {
                System.err.println("⚠️ Indexing wait interrupted: " + e.getMessage());
            }
            
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