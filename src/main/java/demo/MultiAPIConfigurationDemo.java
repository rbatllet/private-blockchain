package demo;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.search.SearchSpecialistAPI;
import com.rbatllet.blockchain.search.SearchFrameworkEngine.EnhancedSearchResult;
import com.rbatllet.blockchain.security.KeyFileLoader;
import com.rbatllet.blockchain.service.UserFriendlyEncryptionAPI;
import com.rbatllet.blockchain.config.EncryptionConfig;
import java.security.KeyPair;
import java.util.List;

/**
 * Demo showing how EncryptionConfig can be used across all three APIs:
 * - UserFriendlyEncryptionAPI
 * - SearchSpecialistAPI
 * - SearchFrameworkEngine (via SearchSpecialistAPI)
 */
public class MultiAPIConfigurationDemo {

    public static void main(String[] args) {
        System.out.println("🔐 MULTI-API ENCRYPTION CONFIGURATION DEMO");
        System.out.println("==========================================");
        System.out.println();

        try {
            // Demo 1: UserFriendlyEncryptionAPI with different configs
            demonstrateUserFriendlyAPIConfigurations();

            // Demo 2: SearchSpecialistAPI with different configs
            demonstrateSearchSpecialistAPIConfigurations();

            // Demo 3: All APIs working together with same config
            demonstrateUnifiedConfiguration();

        } catch (Exception e) {
            System.err.println("❌ Demo failed: " + e.getMessage());
            e.printStackTrace();
        }

        // Force exit to stop background threads
        System.exit(0);
    }

    private static void demonstrateUserFriendlyAPIConfigurations() throws Exception {
        System.out.println("👥 USER FRIENDLY API CONFIGURATIONS");
        System.out.println("===================================");

        Blockchain blockchain = new Blockchain();
        String password = "UserFriendlyDemo2024!";

        // Load genesis admin keys
        KeyPair genesisKeys = KeyFileLoader.loadKeyPairFromFiles(
            "./keys/genesis-admin.private",
            "./keys/genesis-admin.public"
        );

        // High Security Configuration
        System.out.println("🔒 High Security Configuration:");
        EncryptionConfig highSecConfig = EncryptionConfig.createHighSecurityConfig();
        UserFriendlyEncryptionAPI highSecAPI = new UserFriendlyEncryptionAPI(blockchain, highSecConfig);
        highSecAPI.setDefaultCredentials("GENESIS_ADMIN", genesisKeys);

        KeyPair userKeys = highSecAPI.createUser("high-sec-user");
        highSecAPI.setDefaultCredentials("high-sec-user", userKeys);
        
        // Initialize SearchSpecialistAPI to fix search operations
        String searchPassword = highSecAPI.generateSecurePassword(16);
        blockchain.getSearchSpecialistAPI().initializeWithBlockchain(blockchain, searchPassword, userKeys.getPrivate());
        
        // Store data with high security
        highSecAPI.storeSearchableData("High security financial data", password, new String[]{"financial", "secure"});
        
        System.out.println("   📊 Config: " + highSecConfig.getSummary());
        System.out.println("   🔐 Security Level: " + highSecConfig.getSecurityLevel());
        System.out.println("   🔑 Key Length: " + highSecConfig.getKeyLength() + " bits");
        System.out.println("   🔄 PBKDF2 Iterations: " + highSecConfig.getPbkdf2Iterations());
        System.out.println();
        
        // Performance Configuration
        System.out.println("🚀 Performance Configuration:");
        EncryptionConfig perfConfig = EncryptionConfig.createPerformanceConfig();
        UserFriendlyEncryptionAPI perfAPI = new UserFriendlyEncryptionAPI(blockchain, perfConfig);
        perfAPI.setDefaultCredentials("GENESIS_ADMIN", genesisKeys);

        KeyPair perfKeys = perfAPI.createUser("perf-user");
        perfAPI.setDefaultCredentials("perf-user", perfKeys);
        
        // Store data with performance optimization
        perfAPI.storeSearchableData("Performance optimized data", password, new String[]{"performance", "fast"});
        
        System.out.println("   📊 Config: " + perfConfig.getSummary());
        System.out.println("   🔐 Security Level: " + perfConfig.getSecurityLevel());
        System.out.println("   🔑 Key Length: " + perfConfig.getKeyLength() + " bits");
        System.out.println("   🔄 PBKDF2 Iterations: " + perfConfig.getPbkdf2Iterations());
        System.out.println("   📦 Compression: " + (perfConfig.isEnableCompression() ? "Enabled" : "Disabled"));
        System.out.println();
    }
    
    private static void demonstrateSearchSpecialistAPIConfigurations() throws Exception {
        System.out.println("🔍 SEARCH SPECIALIST API CONFIGURATIONS");
        System.out.println("======================================");
        
        Blockchain blockchain = new Blockchain();
        String password = "SearchSpecialistDemo2024!";
        
        // Setup test data
        UserFriendlyEncryptionAPI dataAPI = new UserFriendlyEncryptionAPI(blockchain);
        KeyPair userKeys = dataAPI.createUser("search-user");
        dataAPI.setDefaultCredentials("search-user", userKeys);
        
        // Initialize SearchSpecialistAPI to fix search operations
        String searchPassword = dataAPI.generateSecurePassword(16);
        blockchain.getSearchSpecialistAPI().initializeWithBlockchain(blockchain, searchPassword, userKeys.getPrivate());
        
        dataAPI.storeSearchableData("Search test data", password, new String[]{"search", "test", "data"});
        
        // High Security Search
        System.out.println("🔒 High Security Search:");
        EncryptionConfig highSecConfig = EncryptionConfig.createHighSecurityConfig();
        SearchSpecialistAPI highSecSearchAPI = new SearchSpecialistAPI(blockchain, password, userKeys.getPrivate(), highSecConfig);
        
        List<EnhancedSearchResult> highSecResults = highSecSearchAPI.searchAll("search");
        System.out.println("   📊 Config: " + highSecConfig.getSummary());
        System.out.println("   🔍 Search Results: " + highSecResults.size());
        System.out.println("   🔐 Security Level: " + highSecConfig.getSecurityLevel());
        System.out.println();
        
        // Performance Search
        System.out.println("🚀 Performance Search:");
        EncryptionConfig perfConfig = EncryptionConfig.createPerformanceConfig();
        SearchSpecialistAPI perfSearchAPI = new SearchSpecialistAPI(blockchain, password, userKeys.getPrivate(), perfConfig);
        
        List<EnhancedSearchResult> perfResults = perfSearchAPI.searchAll("data");
        System.out.println("   📊 Config: " + perfConfig.getSummary());
        System.out.println("   🔍 Search Results: " + perfResults.size());
        System.out.println("   🔐 Security Level: " + perfConfig.getSecurityLevel());
        System.out.println();
        
        // Custom Search Configuration
        System.out.println("🔧 Custom Search Configuration:");
        EncryptionConfig customConfig = EncryptionConfig.builder()
            .keyLength(192)
            .pbkdf2Iterations(80000)
            .enableCompression(true)
            .corruptionDetectionEnabled(true)
            .build();
        
        SearchSpecialistAPI customSearchAPI = new SearchSpecialistAPI(blockchain, password, userKeys.getPrivate(), customConfig);
        
        List<EnhancedSearchResult> customResults = customSearchAPI.searchAll("test");
        System.out.println("   📊 Config: " + customConfig.getSummary());
        System.out.println("   🔍 Search Results: " + customResults.size());
        System.out.println("   🔐 Security Level: " + customConfig.getSecurityLevel());
        System.out.println("   🔑 Key Length: " + customConfig.getKeyLength() + " bits");
        System.out.println();
    }
    
    private static void demonstrateUnifiedConfiguration() throws Exception {
        System.out.println("🤝 UNIFIED CONFIGURATION ACROSS ALL APIs");
        System.out.println("========================================");
        
        // Create a single configuration to use across all APIs
        EncryptionConfig unifiedConfig = EncryptionConfig.builder()
            .keyLength(256)
            .pbkdf2Iterations(120000)
            .enableCompression(false)
            .corruptionDetectionEnabled(true)
            .metadataEncryptionEnabled(true)
            .validateEncryptionFormat(true)
            .build();
        
        System.out.println("🔧 Unified Configuration:");
        System.out.println("   📊 Config: " + unifiedConfig.getSummary());
        System.out.println("   🔐 Security Level: " + unifiedConfig.getSecurityLevel());
        System.out.println("   🔑 Key Length: " + unifiedConfig.getKeyLength() + " bits");
        System.out.println("   🔄 PBKDF2 Iterations: " + unifiedConfig.getPbkdf2Iterations());
        System.out.println("   🛡️ Metadata Encryption: " + (unifiedConfig.isMetadataEncryptionEnabled() ? "Enabled" : "Disabled"));
        System.out.println("   🔍 Corruption Detection: " + (unifiedConfig.isCorruptionDetectionEnabled() ? "Enabled" : "Disabled"));
        System.out.println();
        
        // Step 1: Create blockchain and APIs with unified config
        Blockchain blockchain = new Blockchain();
        String password = "UnifiedDemo2024!";
        
        // UserFriendlyEncryptionAPI with unified config
        UserFriendlyEncryptionAPI dataAPI = new UserFriendlyEncryptionAPI(blockchain, unifiedConfig);
        KeyPair userKeys = dataAPI.createUser("unified-user");
        dataAPI.setDefaultCredentials("unified-user", userKeys);
        
        // Initialize SearchSpecialistAPI to fix search operations
        String searchPassword = dataAPI.generateSecurePassword(16);
        blockchain.getSearchSpecialistAPI().initializeWithBlockchain(blockchain, searchPassword, userKeys.getPrivate());
        
        // Store data using the unified config
        dataAPI.storeSearchableData("Unified configuration test data", password, new String[]{"unified", "config", "test"});
        
        // SearchSpecialistAPI with same unified config
        SearchSpecialistAPI searchAPI = new SearchSpecialistAPI(blockchain, password, userKeys.getPrivate(), unifiedConfig);
        
        // Test search
        List<EnhancedSearchResult> results = searchAPI.searchAll("unified");
        
        System.out.println("✅ UNIFIED CONFIGURATION RESULTS:");
        System.out.println("   📊 Data stored with UserFriendlyEncryptionAPI using unified config");
        System.out.println("   🔍 Search performed with SearchSpecialistAPI using same unified config");
        System.out.println("   📈 Search Results: " + results.size());
        System.out.println("   🎯 Configuration consistency maintained across all APIs");
        System.out.println();
        
        System.out.println("🎉 SUCCESS: All APIs can use the same EncryptionConfig!");
        System.out.println("💡 This ensures consistent security policies across the entire blockchain system.");
    }
}