package demo;

import com.rbatllet.blockchain.core.Blockchain;
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
            
            // Store test data
            String[] keywords = {"financial", "healthcare", "confidential", "performance"};
            dataAPI.storeSearchableData("Test data for configuration comparison", password, keywords);
            
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
        
        try {
            long startTime = System.nanoTime();
            
            // Create SearchSpecialistAPI with high security config
            EncryptionConfig highSecConfig = EncryptionConfig.createHighSecurityConfig();
            SearchSpecialistAPI searchAPI = new SearchSpecialistAPI(blockchain, password, userKeys.getPrivate(), highSecConfig);
            
            // Test search
            List<EnhancedSearchResult> results = searchAPI.searchAll("financial");
            long endTime = System.nanoTime();
            double timeMs = (endTime - startTime) / 1_000_000.0;
            
            System.out.println("📊 Configuration: " + highSecConfig.getSummary());
            System.out.println("🔍 Search Results: " + results.size());
            System.out.println("⏱️ Total Time: " + String.format("%.2f", timeMs) + "ms");
            System.out.println("🔐 Security Level: " + highSecConfig.getSecurityLevel());
            System.out.println("🔑 Key Length: " + highSecConfig.getKeyLength() + " bits");
            System.out.println("🔄 PBKDF2 Iterations: " + highSecConfig.getPbkdf2Iterations());
            System.out.println();
            
        } catch (Exception e) {
            System.err.println("❌ High security demo failed: " + e.getMessage());
        }
    }
    
    private static void demonstratePerformanceConfig(Blockchain blockchain, String password, KeyPair userKeys) {
        System.out.println("🚀 PERFORMANCE CONFIGURATION");
        System.out.println("=============================");
        
        try {
            long startTime = System.nanoTime();
            
            // Create SearchSpecialistAPI with performance config
            EncryptionConfig perfConfig = EncryptionConfig.createPerformanceConfig();
            SearchSpecialistAPI searchAPI = new SearchSpecialistAPI(blockchain, password, userKeys.getPrivate(), perfConfig);
            
            // Test search
            List<EnhancedSearchResult> results = searchAPI.searchAll("healthcare");
            long endTime = System.nanoTime();
            double timeMs = (endTime - startTime) / 1_000_000.0;
            
            System.out.println("📊 Configuration: " + perfConfig.getSummary());
            System.out.println("🔍 Search Results: " + results.size());
            System.out.println("⏱️ Total Time: " + String.format("%.2f", timeMs) + "ms");
            System.out.println("🔐 Security Level: " + perfConfig.getSecurityLevel());
            System.out.println("🔑 Key Length: " + perfConfig.getKeyLength() + " bits");
            System.out.println("🔄 PBKDF2 Iterations: " + perfConfig.getPbkdf2Iterations());
            System.out.println("📦 Compression: " + (perfConfig.isEnableCompression() ? "Enabled" : "Disabled"));
            System.out.println();
            
        } catch (Exception e) {
            System.err.println("❌ Performance demo failed: " + e.getMessage());
        }
    }
    
    private static void demonstrateBalancedConfig(Blockchain blockchain, String password, KeyPair userKeys) {
        System.out.println("⚖️ BALANCED CONFIGURATION");
        System.out.println("=========================");
        
        try {
            long startTime = System.nanoTime();
            
            // Create SearchSpecialistAPI with balanced config
            EncryptionConfig balancedConfig = EncryptionConfig.createBalancedConfig();
            SearchSpecialistAPI searchAPI = new SearchSpecialistAPI(blockchain, password, userKeys.getPrivate(), balancedConfig);
            
            // Test search
            List<EnhancedSearchResult> results = searchAPI.searchAll("confidential");
            long endTime = System.nanoTime();
            double timeMs = (endTime - startTime) / 1_000_000.0;
            
            System.out.println("📊 Configuration: " + balancedConfig.getSummary());
            System.out.println("🔍 Search Results: " + results.size());
            System.out.println("⏱️ Total Time: " + String.format("%.2f", timeMs) + "ms");
            System.out.println("🔐 Security Level: " + balancedConfig.getSecurityLevel());
            System.out.println("🔑 Key Length: " + balancedConfig.getKeyLength() + " bits");
            System.out.println("🔄 PBKDF2 Iterations: " + balancedConfig.getPbkdf2Iterations());
            System.out.println();
            
        } catch (Exception e) {
            System.err.println("❌ Balanced demo failed: " + e.getMessage());
        }
    }
    
    private static void demonstrateCustomConfig(Blockchain blockchain, String password, KeyPair userKeys) {
        System.out.println("🔧 CUSTOM CONFIGURATION");
        System.out.println("=======================");
        
        try {
            long startTime = System.nanoTime();
            
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
            
            // Test search
            List<EnhancedSearchResult> results = searchAPI.searchAll("performance");
            long endTime = System.nanoTime();
            double timeMs = (endTime - startTime) / 1_000_000.0;
            
            System.out.println("📊 Configuration: " + customConfig.getSummary());
            System.out.println("🔍 Search Results: " + results.size());
            System.out.println("⏱️ Total Time: " + String.format("%.2f", timeMs) + "ms");
            System.out.println("🔐 Security Level: " + customConfig.getSecurityLevel());
            System.out.println("🔑 Key Length: " + customConfig.getKeyLength() + " bits");
            System.out.println("🔄 PBKDF2 Iterations: " + customConfig.getPbkdf2Iterations());
            System.out.println("📦 Compression: " + (customConfig.isEnableCompression() ? "Enabled" : "Disabled"));
            System.out.println("🛡️ Corruption Detection: " + (customConfig.isCorruptionDetectionEnabled() ? "Enabled" : "Disabled"));
            System.out.println();
            
        } catch (Exception e) {
            System.err.println("❌ Custom demo failed: " + e.getMessage());
        }
    }
}