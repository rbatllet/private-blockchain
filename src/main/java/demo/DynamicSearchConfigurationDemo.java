package demo;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.indexing.IndexingCoordinator;
import com.rbatllet.blockchain.config.EncryptionConfig;
import com.rbatllet.blockchain.search.SearchSpecialistAPI;
import com.rbatllet.blockchain.search.SearchFrameworkEngine.EnhancedSearchResult;
import com.rbatllet.blockchain.service.UserFriendlyEncryptionAPI;
import com.rbatllet.blockchain.util.CryptoUtil;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.security.KeyPair;
import java.util.List;

/**
 * Demonstrates TWO correct ways to use SearchSpecialistAPI:
 * 1. SINGLETON PATTERN: blockchain.getSearchSpecialistAPI() - shared index
 * 2. DIRECT CONSTRUCTOR: new SearchSpecialistAPI(blockchain, password, privateKey, config) - custom config
 */
public class DynamicSearchConfigurationDemo {
    
    private static final Logger logger = LogManager.getLogger(DynamicSearchConfigurationDemo.class);
    
    public static void main(String[] args) {
        System.out.println("=== 🔍 SEARCH API COMPARISON DEMO ===");
        System.out.println("Comparing TWO approaches to use SearchSpecialistAPI");
        System.out.println();
        
        try {
            Blockchain blockchain = new Blockchain();
            
            // Setup with CryptoUtil
            KeyPair bootstrapKeys = CryptoUtil.generateKeyPair();
            String publicKeyString = CryptoUtil.publicKeyToString(bootstrapKeys.getPublic());
            blockchain.createBootstrapAdmin(publicKeyString, "BOOTSTRAP_ADMIN");
            
            String password = "SecurePassword123!";
            
            UserFriendlyEncryptionAPI dataAPI = new UserFriendlyEncryptionAPI(blockchain);
            dataAPI.setDefaultCredentials("BOOTSTRAP_ADMIN", bootstrapKeys);

            KeyPair userKeys = dataAPI.createUser("search-user");
            dataAPI.setDefaultCredentials("search-user", userKeys);
            
            // Get the singleton SearchSpecialistAPI
            SearchSpecialistAPI searchAPI = blockchain.getSearchSpecialistAPI();
            searchAPI.initializeWithBlockchain(blockchain, password, userKeys.getPrivate());
            
            System.out.println("📊 STORING TEST DATA");
            System.out.println("===================");
            
            // Store various types of data
            dataAPI.storeSearchableData("High security financial data", password, 
                new String[]{"finance", "secure", "confidential"});
            
            dataAPI.storeSearchableData("Performance-critical real-time data", password, 
                new String[]{"performance", "realtime", "fast"});
            
            dataAPI.storeSearchableData("Standard business data", password, 
                new String[]{"business", "standard", "data"});
            
            // Wait for indexing to complete
            IndexingCoordinator.getInstance().waitForCompletion(5000);
            
            System.out.println("✅ Stored 3 blocks with searchable keywords");
            System.out.println();
            
            // Demonstrate correct usage - Method 1: Singleton pattern
            demonstrateSingletonUsage(searchAPI);
            
            System.out.println();
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println();
            
            // Demonstrate alternative - Method 2: Direct constructor with custom config
            demonstrateDirectConstructor(blockchain, password, userKeys);
            
            System.out.println();
            System.out.println("✅ SEARCH API COMPARISON DEMO COMPLETE");
            System.out.println();
            System.out.println("📝 Key Learnings:");
            System.out.println("   ✅ METHOD 1 (Singleton): blockchain.getSearchSpecialistAPI()");
            System.out.println("      • Shared index across blockchain");
            System.out.println("      • Best for general use cases");
            System.out.println("      • Automatic indexing integration");
            System.out.println();
            System.out.println("   ✅ METHOD 2 (Constructor): new SearchSpecialistAPI(blockchain, password, privateKey, config)");
            System.out.println("      • Custom EncryptionConfig flexibility");
            System.out.println("      • Independent instance with own index");
            System.out.println("      • Perfect for specialized search requirements");
            System.out.println();
            System.out.println("   ⚠️  IMPORTANT: Constructor requires blockchain with existing blocks!");
            
        } catch (Exception e) {
            System.err.println("❌ Demo failed: " + e.getMessage());
            logger.error("❌ Demo failed", e);
            e.printStackTrace();
        } finally {
            cleanup();
        }
        
        System.exit(0);
    }
    
    private static void demonstrateSingletonUsage(SearchSpecialistAPI searchAPI) throws Exception {
        System.out.println("🔍 SINGLETON SEARCH DEMONSTRATIONS");
        System.out.println("==================================");
        
        // 1. searchPublic() - Fast public search
        System.out.println("1️⃣  Public Search (searchPublic):");
        List<EnhancedSearchResult> publicResults = searchAPI.searchPublic("finance");
        System.out.println("   Query: 'finance'");
        System.out.println("   Results: " + publicResults.size());
        System.out.println("   ✅ Works: Searches public keywords");
        System.out.println();
        
        // 2. searchAll() - Hybrid search (public + encrypted)
        System.out.println("2️⃣  Hybrid Search (searchAll):");
        List<EnhancedSearchResult> hybridResults = searchAPI.searchAll("secure");
        System.out.println("   Query: 'secure'");
        System.out.println("   Results: " + hybridResults.size());
        System.out.println("   ✅ Works: Uses singleton index correctly");
        System.out.println();
        
        // 3. Multiple queries to show consistency
        System.out.println("3️⃣  Multiple Queries (consistency check):");
        
        List<EnhancedSearchResult> results1 = searchAPI.searchPublic("performance");
        System.out.println("   searchPublic('performance'): " + results1.size() + " results");
        
        List<EnhancedSearchResult> results2 = searchAPI.searchAll("realtime");
        System.out.println("   searchAll('realtime'): " + results2.size() + " results");
        
        List<EnhancedSearchResult> results3 = searchAPI.searchPublic("data");
        System.out.println("   searchPublic('data'): " + results3.size() + " results");
        
        System.out.println("   ✅ All searches use the same shared index");
        System.out.println();
        
        // 4. Show current configuration
        System.out.println("4️⃣  Current Configuration:");
        var config = searchAPI.getEncryptionConfig();
        System.out.println("   " + config.getSummary());
        System.out.println("   Security Level: " + config.getSecurityLevel());
        System.out.println("   ✅ Configuration retrieved successfully");
        System.out.println();
    }
    
    private static void demonstrateDirectConstructor(Blockchain blockchain, String password, KeyPair userKeys) throws Exception {
        System.out.println("🔧 ALTERNATIVE: DIRECT CONSTRUCTOR WITH CUSTOM CONFIG");
        System.out.println("====================================================");
        System.out.println("Using: new SearchSpecialistAPI(blockchain, password, privateKey, customConfig)");
        System.out.println();
        
        // Create custom EncryptionConfig for different use cases
        System.out.println("📋 Creating Custom Configurations:");
        System.out.println();
        
        // 1. High Security Config (maximum protection)
        EncryptionConfig highSecurityConfig = EncryptionConfig.createHighSecurityConfig();
        System.out.println("1️⃣  High Security Config:");
        System.out.println("   " + highSecurityConfig.getSummary());
        System.out.println("   Security Level: " + highSecurityConfig.getSecurityLevel());
        System.out.println();
        
        // 2. Balanced Config (good performance + security)
        EncryptionConfig balancedConfig = EncryptionConfig.createBalancedConfig();
        System.out.println("2️⃣  Balanced Config:");
        System.out.println("   " + balancedConfig.getSummary());
        System.out.println("   Security Level: " + balancedConfig.getSecurityLevel());
        System.out.println();
        
        // 3. Performance Config (speed priority)
        EncryptionConfig performanceConfig = EncryptionConfig.createPerformanceConfig();
        System.out.println("3️⃣  Performance Config:");
        System.out.println("   " + performanceConfig.getSummary());
        System.out.println("   Security Level: " + performanceConfig.getSecurityLevel());
        System.out.println();
        
        // Demonstrate using custom config with constructor
        System.out.println("🔍 Testing with Balanced Config:");
        SearchSpecialistAPI customSearchAPI = new SearchSpecialistAPI(
            blockchain, 
            password, 
            userKeys.getPrivate(), 
            balancedConfig
        );
        
        // Perform searches with custom-configured instance
        List<EnhancedSearchResult> results1 = customSearchAPI.searchPublic("finance");
        System.out.println("   searchPublic('finance'): " + results1.size() + " results");
        
        List<EnhancedSearchResult> results2 = customSearchAPI.searchAll("performance");
        System.out.println("   searchAll('performance'): " + results2.size() + " results");
        
        System.out.println("   ✅ Custom config works perfectly!");
        System.out.println();
        
        System.out.println("💡 Benefits of Direct Constructor:");
        System.out.println("   • Choose encryption config per use case");
        System.out.println("   • Performance vs Security trade-offs");
        System.out.println("   • Independent search instance");
        System.out.println("   • Full control over initialization");
        System.out.println();
    }
    
    private static void cleanup() {
        try {
            System.out.println("🧹 Cleaning up test environment...");
            
            File dbFile = new File("blockchain.db");
            if (dbFile.exists()) dbFile.delete();
            
            File shmFile = new File("blockchain.db-shm");
            if (shmFile.exists()) shmFile.delete();
            
            File walFile = new File("blockchain.db-wal");
            if (walFile.exists()) walFile.delete();
            
            File offChainDir = new File("off-chain-data");
            if (offChainDir.exists()) {
                deleteDirectory(offChainDir);
            }
            
            System.out.println("✅ Cleanup completed");
        } catch (Exception e) {
            logger.warn("⚠️ Cleanup failed: {}", e.getMessage());
        }
    }
    
    private static void deleteDirectory(File directory) {
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        directory.delete();
    }
}

