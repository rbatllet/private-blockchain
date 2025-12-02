package demo;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.indexing.IndexingCoordinator;
import com.rbatllet.blockchain.search.SearchSpecialistAPI;
import com.rbatllet.blockchain.search.SearchFrameworkEngine.EnhancedSearchResult;
import com.rbatllet.blockchain.security.KeyFileLoader;
import com.rbatllet.blockchain.service.UserFriendlyEncryptionAPI;
import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.config.EncryptionConfig;
import java.security.KeyPair;
import java.util.List;

/**
 * Demo that showcases different EncryptionConfig options with SearchSpecialistAPI
 */
public class SearchConfigurationDemo {

    public static void main(String[] args) {
        System.out.println("🔐 SEARCH CONFIGURATION DEMO");
        System.out.println("============================");
        System.out.println();

        try {
            // Setup blockchain and data
            Blockchain blockchain = new Blockchain();

            // RBAC FIX (v1.0.6): Clear database before bootstrap to avoid "Existing users" error
            blockchain.clearAndReinitialize();

            String password = "ConfigDemo2024!";

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

            UserFriendlyEncryptionAPI dataAPI = new UserFriendlyEncryptionAPI(blockchain);
            dataAPI.setDefaultCredentials("BOOTSTRAP_ADMIN", bootstrapKeys);

            // Create user and store test data (authorized by bootstrap admin)
            KeyPair userKeys = dataAPI.createUser("config-demo-user");
            dataAPI.setDefaultCredentials("config-demo-user", userKeys);
            
            // Store test data with BOTH public and private keywords
            System.out.println("📝 Storing searchable data with:");
            System.out.println("   🔓 Public keywords: financial, healthcare");
            System.out.println("   🔐 Private keywords: confidential, performance");
            System.out.println();
            
            String[] publicKeywords = {"public:financial", "public:healthcare"};
            String[] privateKeywords = {"confidential", "performance"};
            dataAPI.storeSearchableDataWithLayers(
                "Financial healthcare data with confidential performance metrics", 
                password, 
                publicKeywords,
                privateKeywords
            );
            
            // Wait for background indexing to complete
            System.out.println("\n⏳ Waiting for background indexing to complete...");
            IndexingCoordinator.getInstance().waitForCompletion();
            System.out.println("✅ Background indexing completed - all blocks indexed\n");
            
            System.out.println("📊 CONFIGURATION COMPARISON");
            System.out.println("===========================");
            System.out.println();
            
            // Demo 1: High Security Configuration
            demonstrateHighSecurityConfig(blockchain, password, userKeys);
            
            // Demo 2: Performance Configuration
            demonstratePerformanceConfig(blockchain, password, userKeys);
            
            // Demo 3: Balanced Configuration
            demonstrateBalancedConfig(blockchain, password, userKeys);
            
            // Demo 4: Custom Configuration
            demonstrateCustomConfig(blockchain, password, userKeys);
            
            System.out.println("📊 CONFIGURATION SUMMARY");
            System.out.println("========================");
            System.out.println("✅ High Security: Maximum protection, slower performance");
            System.out.println("🚀 Performance: Optimized speed, reduced security");
            System.out.println("⚖️ Balanced: Good compromise between security and speed");
            System.out.println("🔧 Custom: Tailored for specific requirements");
            
        } catch (Exception e) {
            System.err.println("❌ Demo failed: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Force exit to stop background threads
        System.exit(0);
    }
    
    private static void demonstrateHighSecurityConfig(Blockchain blockchain, String password, KeyPair userKeys) {
        System.out.println("🔒 HIGH SECURITY CONFIGURATION");
        System.out.println("===============================");
        System.out.println("ℹ️  Testing all 3 search modes with maximum security");
        System.out.println();
        
        try {
            // Create SearchSpecialistAPI with high security config
            EncryptionConfig highSecConfig = EncryptionConfig.createHighSecurityConfig();
            SearchSpecialistAPI searchAPI = new SearchSpecialistAPI(blockchain, password, userKeys.getPrivate(), highSecConfig);
            
            // MODE 1: PUBLIC search (no password, fast)
            long start1 = System.nanoTime();
            List<EnhancedSearchResult> publicResults = searchAPI.searchPublic("financial");
            double time1 = (System.nanoTime() - start1) / 1_000_000.0;
            
            System.out.println("🔓 MODE 1: PUBLIC Search (no password)");
            System.out.println("   Query: 'financial'");
            System.out.println("   Results: " + publicResults.size());
            System.out.println("   Time: " + String.format("%.2f", time1) + "ms");
            System.out.println();
            
            // MODE 2: PRIVATE search (with password, decrypts)
            long start2 = System.nanoTime();
            List<EnhancedSearchResult> privateResults = searchAPI.searchSecure("confidential", password);
            double time2 = (System.nanoTime() - start2) / 1_000_000.0;
            
            System.out.println("🔐 MODE 2: PRIVATE Search (with password)");
            System.out.println("   Query: 'confidential'");
            System.out.println("   Results: " + privateResults.size());
            System.out.println("   Time: " + String.format("%.2f", time2) + "ms");
            System.out.println();
            
            // MODE 3: HYBRID search (tries both)
            long start3 = System.nanoTime();
            List<EnhancedSearchResult> hybridResults = searchAPI.searchAll("healthcare");
            double time3 = (System.nanoTime() - start3) / 1_000_000.0;
            
            System.out.println("🔄 MODE 3: HYBRID Search (public + private)");
            System.out.println("   Query: 'healthcare'");
            System.out.println("   Results: " + hybridResults.size());
            System.out.println("   Time: " + String.format("%.2f", time3) + "ms");
            System.out.println();
            
            System.out.println("📊 Configuration: " + highSecConfig.getSummary());
            System.out.println("🔐 Security Level: " + highSecConfig.getSecurityLevel());
            System.out.println("🔑 Key Length: " + highSecConfig.getKeyLength() + " bits");
            System.out.println("🔄 PBKDF2 Iterations: " + highSecConfig.getPbkdf2Iterations());
            System.out.println();
            
        } catch (Exception e) {
            System.err.println("❌ High security demo failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void demonstratePerformanceConfig(Blockchain blockchain, String password, KeyPair userKeys) {
        System.out.println("🚀 PERFORMANCE CONFIGURATION");
        System.out.println("=============================");
        System.out.println("ℹ️  Optimized for speed - comparing search mode performance");
        System.out.println();
        
        try {
            // Create SearchSpecialistAPI with performance config
            EncryptionConfig perfConfig = EncryptionConfig.createPerformanceConfig();
            SearchSpecialistAPI searchAPI = new SearchSpecialistAPI(blockchain, password, userKeys.getPrivate(), perfConfig);
            
            // MODE 1: PUBLIC search (fastest)
            long start1 = System.nanoTime();
            List<EnhancedSearchResult> publicResults = searchAPI.searchPublic("financial");
            double time1 = (System.nanoTime() - start1) / 1_000_000.0;
            
            System.out.println("⚡ MODE 1: PUBLIC Search (fastest)");
            System.out.println("   Results: " + publicResults.size() + " | Time: " + String.format("%.2f", time1) + "ms");
            
            // MODE 2: PRIVATE search (slower, needs decryption)
            long start2 = System.nanoTime();
            List<EnhancedSearchResult> privateResults = searchAPI.searchSecure("performance", password);
            double time2 = (System.nanoTime() - start2) / 1_000_000.0;
            
            System.out.println("🔐 MODE 2: PRIVATE Search (with decryption)");
            System.out.println("   Results: " + privateResults.size() + " | Time: " + String.format("%.2f", time2) + "ms");
            
            // MODE 3: HYBRID search (combines both)
            long start3 = System.nanoTime();
            List<EnhancedSearchResult> hybridResults = searchAPI.searchAll("healthcare");
            double time3 = (System.nanoTime() - start3) / 1_000_000.0;
            
            System.out.println("🔄 MODE 3: HYBRID Search (comprehensive)");
            System.out.println("   Results: " + hybridResults.size() + " | Time: " + String.format("%.2f", time3) + "ms");
            System.out.println();
            
            System.out.println("📊 Performance Comparison:");
            System.out.println("   🥇 Fastest: PUBLIC (" + String.format("%.2f", time1) + "ms)");
            System.out.println("   🥈 Medium: " + (time2 < time3 ? "PRIVATE" : "HYBRID") + " (" + String.format("%.2f", Math.min(time2, time3)) + "ms)");
            System.out.println("   🥉 Slowest: " + (time2 > time3 ? "PRIVATE" : "HYBRID") + " (" + String.format("%.2f", Math.max(time2, time3)) + "ms)");
            System.out.println();
            System.out.println("📦 Compression: " + (perfConfig.isEnableCompression() ? "Enabled" : "Disabled"));
            System.out.println();
            
        } catch (Exception e) {
            System.err.println("❌ Performance demo failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void demonstrateBalancedConfig(Blockchain blockchain, String password, KeyPair userKeys) {
        System.out.println("⚖️ BALANCED CONFIGURATION");
        System.out.println("=========================");
        System.out.println("ℹ️  Demonstrating 3 search modes with balanced security/performance");
        System.out.println();
        
        try {
            // Create SearchSpecialistAPI with balanced config
            EncryptionConfig balancedConfig = EncryptionConfig.createBalancedConfig();
            SearchSpecialistAPI searchAPI = new SearchSpecialistAPI(blockchain, password, userKeys.getPrivate(), balancedConfig);
            
            System.out.println("📊 Configuration: " + balancedConfig.getSummary());
            System.out.println("🔐 Security Level: " + balancedConfig.getSecurityLevel());
            System.out.println("🔑 Key Length: " + balancedConfig.getKeyLength() + " bits");
            System.out.println("🔄 PBKDF2 Iterations: " + balancedConfig.getPbkdf2Iterations());
            System.out.println();
            
            // MODE 1: PUBLIC search (no password)
            long start1 = System.nanoTime();
            List<EnhancedSearchResult> publicResults = searchAPI.searchPublic("financial");
            double time1 = (System.nanoTime() - start1) / 1_000_000.0;
            
            System.out.println("🔓 MODE 1: PUBLIC Search (no password)");
            System.out.println("   Query: 'financial'");
            System.out.println("   Results: " + publicResults.size());
            System.out.println("   Time: " + String.format("%.2f", time1) + "ms");
            if (!publicResults.isEmpty()) {
                System.out.println("   ✅ Found in PUBLIC layer");
            } else {
                System.out.println("   ❌ No match found");
            }
            System.out.println();
            
            // MODE 2: PRIVATE search (with password)
            long start2 = System.nanoTime();
            List<EnhancedSearchResult> privateResults = searchAPI.searchSecure("confidential", password);
            double time2 = (System.nanoTime() - start2) / 1_000_000.0;
            
            System.out.println("🔐 MODE 2: PRIVATE Search (with password)");
            System.out.println("   Query: 'confidential'");
            System.out.println("   Results: " + privateResults.size());
            System.out.println("   Time: " + String.format("%.2f", time2) + "ms (includes decryption)");
            if (!privateResults.isEmpty()) {
                System.out.println("   ✅ Found in ENCRYPTED layer");
            } else {
                System.out.println("   ❌ No match found");
            }
            System.out.println();
            
            // MODE 3: HYBRID search (combines both)
            long start3 = System.nanoTime();
            List<EnhancedSearchResult> hybridResults = searchAPI.searchAll("healthcare");
            double time3 = (System.nanoTime() - start3) / 1_000_000.0;
            
            System.out.println("🔀 MODE 3: HYBRID Search (public + encrypted)");
            System.out.println("   Query: 'healthcare'");
            System.out.println("   Results: " + hybridResults.size());
            System.out.println("   Time: " + String.format("%.2f", time3) + "ms (both layers)");
            if (!hybridResults.isEmpty()) {
                System.out.println("   ✅ Found in combined search");
            } else {
                System.out.println("   ❌ No match found");
            }
            System.out.println();
            
        } catch (Exception e) {
            System.err.println("❌ Balanced demo failed: " + e.getMessage());
        }
    }
    
    private static void demonstrateCustomConfig(Blockchain blockchain, String password, KeyPair userKeys) {
        System.out.println("🔧 CUSTOM CONFIGURATION");
        System.out.println("=======================");
        System.out.println("ℹ️  Demonstrating 3 search modes with custom settings");
        System.out.println();
        
        try {
            // Create custom configuration
            EncryptionConfig customConfig = EncryptionConfig.builder()
                .keyLength(192)  // Custom key length
                .pbkdf2Iterations(75000)  // Custom iterations
                .minPasswordLength(10)  // Custom password requirements
                .enableCompression(true)  // Enable compression
                .corruptionDetectionEnabled(true)  // Enable corruption detection
                .validateEncryptionFormat(true)  // Enable format validation
                .build();
            
            SearchSpecialistAPI searchAPI = new SearchSpecialistAPI(blockchain, password, userKeys.getPrivate(), customConfig);
            
            System.out.println("📊 Configuration: " + customConfig.getSummary());
            System.out.println("🔐 Security Level: " + customConfig.getSecurityLevel());
            System.out.println("🔑 Key Length: " + customConfig.getKeyLength() + " bits");
            System.out.println("🔄 PBKDF2 Iterations: " + customConfig.getPbkdf2Iterations());
            System.out.println("📦 Compression: " + (customConfig.isEnableCompression() ? "Enabled" : "Disabled"));
            System.out.println("🛡️ Corruption Detection: " + (customConfig.isCorruptionDetectionEnabled() ? "Enabled" : "Disabled"));
            System.out.println();
            
            // MODE 1: PUBLIC search (fast)
            long start1 = System.nanoTime();
            List<EnhancedSearchResult> publicResults = searchAPI.searchPublic("healthcare");
            double time1 = (System.nanoTime() - start1) / 1_000_000.0;
            
            System.out.println("🔓 MODE 1: PUBLIC Search (no password)");
            System.out.println("   Query: 'healthcare'");
            System.out.println("   Results: " + publicResults.size());
            System.out.println("   Time: " + String.format("%.2f", time1) + "ms");
            if (!publicResults.isEmpty()) {
                System.out.println("   ✅ Found in PUBLIC layer");
            } else {
                System.out.println("   ❌ No match found");
            }
            System.out.println();
            
            // MODE 2: PRIVATE search (with password)
            long start2 = System.nanoTime();
            List<EnhancedSearchResult> privateResults = searchAPI.searchSecure("performance", password);
            double time2 = (System.nanoTime() - start2) / 1_000_000.0;
            
            System.out.println("🔐 MODE 2: PRIVATE Search (with password)");
            System.out.println("   Query: 'performance'");
            System.out.println("   Results: " + privateResults.size());
            System.out.println("   Time: " + String.format("%.2f", time2) + "ms (includes decryption)");
            if (!privateResults.isEmpty()) {
                System.out.println("   ✅ Found in ENCRYPTED layer");
            } else {
                System.out.println("   ❌ No match found");
            }
            System.out.println();
            
            // MODE 3: HYBRID search (combines both)
            long start3 = System.nanoTime();
            List<EnhancedSearchResult> hybridResults = searchAPI.searchAll("financial");
            double time3 = (System.nanoTime() - start3) / 1_000_000.0;
            
            System.out.println("🔀 MODE 3: HYBRID Search (public + encrypted)");
            System.out.println("   Query: 'financial'");
            System.out.println("   Results: " + hybridResults.size());
            System.out.println("   Time: " + String.format("%.2f", time3) + "ms (both layers)");
            if (!hybridResults.isEmpty()) {
                System.out.println("   ✅ Found in combined search");
            } else {
                System.out.println("   ❌ No match found");
            }
            System.out.println();
            
        } catch (Exception e) {
            System.err.println("❌ Custom demo failed: " + e.getMessage());
        }
    }
}