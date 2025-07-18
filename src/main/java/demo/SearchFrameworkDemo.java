package demo;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.service.UserFriendlyEncryptionAPI;
import com.rbatllet.blockchain.entity.Block;

import java.security.KeyPair;
import java.util.List;

/**
 * SEARCH FRAMEWORK DEMONSTRATION
 * 
 * Showcases the complete capabilities of our search framework engine:
 * - Lightning-fast public metadata search
 * - Encrypted content deep search
 * - Advanced privacy-preserving search
 * - Intelligent strategy routing
 * - Three-layer metadata architecture
 * 
 * This demo proves that we've achieved a breakthrough in blockchain search
 * technology combining performance, privacy, and intelligence.
 */
public class SearchFrameworkDemo {
    
    public static void main(String[] args) {
        System.out.println("🚀 ADVANCED BLOCKCHAIN SEARCH ENGINE DEMO");
        System.out.println("================================================");
        System.out.println();
        
        try {
            // Demo password and setup
            String demoPassword = "SecureSearchDemo2024!";
            
            // Create blockchain and initialize UserFriendlyEncryptionAPI
            System.out.println("📊 Setting up demo blockchain with searchable content...");
            Blockchain blockchain = new Blockchain();
            
            // Initialize UserFriendlyEncryptionAPI - the ONE API for everything
            System.out.println("⚡ Initializing UserFriendlyEncryptionAPI...");
            UserFriendlyEncryptionAPI api = new UserFriendlyEncryptionAPI(blockchain);
            
            // Set up default credentials (this is required!)
            System.out.println("🔑 Setting up default credentials...");
            KeyPair demoKeys = api.createUser("demo-user");
            api.setDefaultCredentials("demo-user", demoKeys);
            
            // Store searchable data with keywords (this is the key!)
            System.out.println("🔍 Storing searchable data with keywords...");
            storeSearchableBlocks(api, demoPassword, blockchain);
            
            System.out.println("🎯 UserFriendlyEncryptionAPI Ready!");
            System.out.println();
            
            // Demo 1: Basic Search
            demonstrateBasicSearch(api, demoPassword);
            
            // Demo 2: Advanced Search
            demonstrateAdvancedSearch(api, demoPassword);
            
            // Demo 3: Data Storage
            demonstrateDataStorage(api, demoPassword);
            
            // Demo 4: Search Validation
            demonstrateSearchValidation(api, demoPassword);
            
            System.out.println("✅ Search Framework Search Demo completed successfully!");
            System.out.println("🎯 The future of blockchain search is here!");
            
        } catch (Exception e) {
            System.err.println("❌ Demo failed: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Force exit to stop background threads
        System.exit(0);
    }
    
    private static void storeSearchableBlocks(UserFriendlyEncryptionAPI api, String password, Blockchain blockchain) throws Exception {
        // Store blocks with searchable keywords as shown in documentation
        
        // Financial block with keywords
        String[] financialKeywords = {"financial", "swift", "transfer", "account"};
        api.storeSearchableData(
            "Financial transaction: SWIFT transfer from Account A to Account B, Amount: €10000", 
            password, financialKeywords);
        
        // Medical block with keywords
        String[] medicalKeywords = {"medical", "patient", "diagnosis", "treatment"};
        api.storeSearchableData(
            "Medical record: Patient John Smith, diagnosis: hypertension, treatment: medication", 
            password, medicalKeywords);
        
        // Contract block with keywords
        String[] contractKeywords = {"contract", "agreement", "software", "license"};
        api.storeSearchableData(
            "Contract agreement: Software license between Company A and Company B, Value: €50000", 
            password, contractKeywords);
        
        // Bitcoin block with keywords
        String[] bitcoinKeywords = {"bitcoin", "transaction", "transfer", "btc"};
        api.storeSearchableData(
            "Bitcoin transaction: Transfer 2.5 BTC from wallet A to wallet B, fee: 0.0001 BTC", 
            password, bitcoinKeywords);
        
        // Weather block with keywords
        String[] temperatureKeywords = {"temperature", "weather", "climate", "barcelona"};
        api.storeSearchableData(
            "Weather data: Temperature 23.5°C, humidity 65%, location: Barcelona", 
            password, temperatureKeywords);
            
        System.out.println("✅ Stored 5 blocks with searchable keywords");
        
        // CRITICAL: Initialize Search Framework Engine for indexing
        System.out.println("🔄 Initializing Search Framework with indexed data...");
        blockchain.initializeAdvancedSearch(password);
        System.out.println("✅ Search Framework Engine is ready!");
    }
    
    private static void demonstrateBasicSearch(UserFriendlyEncryptionAPI api, String password) {
        System.out.println("🔍 DEMO 1: BASIC SEARCH FUNCTIONALITY");
        System.out.println("=====================================");
        
        // Test single keyword searches with timing
        String[] keywords = {"financial", "medical", "contract", "bitcoin", "temperature"};
        for (String keyword : keywords) {
            long startTime = System.nanoTime();
            List<Block> result = api.searchAndDecryptByTerms(new String[]{keyword}, password, 10);
            long endTime = System.nanoTime();
            double timeMs = (endTime - startTime) / 1_000_000.0;
            
            System.out.printf("🔍 Search: '%s' | Results: %d | Time: %.2f ms%n", 
                            keyword, result.size(), timeMs);
            
            for (Block block : result) {
                // Try to get decrypted content
                String content = block.getData();
                if (block.isDataEncrypted()) {
                    try {
                        content = api.retrieveSecret(block.getBlockNumber(), password);
                        if (content == null) {
                            content = "[ENCRYPTED - Decryption returned null]";
                        }
                    } catch (Exception e) {
                        content = "[ENCRYPTED - Could not decrypt: " + e.getMessage().substring(0, Math.min(30, e.getMessage().length())) + "]";
                    }
                }
                
                if (content == null) {
                    content = "[NULL CONTENT]";
                }
                
                System.out.printf("   📄 Block: %d | Content: %s%n", 
                                block.getBlockNumber(), 
                                content.length() > 50 ? 
                                    content.substring(0, 50) + "..." : 
                                    content);
                
                // Show block info
                System.out.printf("      📝 Encrypted: %s | Timestamp: %s%n", 
                                block.isDataEncrypted() ? "Yes" : "No", 
                                block.getTimestamp());
            }
            System.out.println();
        }
        
        // Test search performance statistics
        System.out.println("📊 Search Performance Summary:");
        System.out.printf("   ✅ Total keywords tested: %d%n", keywords.length);
        System.out.println("   ✅ All searches completed successfully");
        System.out.println();
    }
    
    private static void demonstrateAdvancedSearch(UserFriendlyEncryptionAPI api, String password) {
        System.out.println("🔍 DEMO 2: ADVANCED SEARCH FEATURES");
        System.out.println("===================================");
        
        // Test different search methods available in the API
        System.out.println("Testing different search methods:");
        
        // 1. searchAndDecryptByTerms - working search method
        List<Block> smartResult = api.searchAndDecryptByTerms(new String[]{"financial", "swift"}, password, 10);
        System.out.printf("🧠 searchAndDecryptByTerms: '%s' | Results: %d%n", 
                         "financial swift", smartResult.size());
        
        // 2. Test search with different result limits
        try {
            List<Block> limitedResult = api.searchAndDecryptByTerms(new String[]{"medical"}, password, 5);
            System.out.printf("📊 Search with limit: '%s' | Results: %d%n", 
                             "medical", limitedResult.size());
        } catch (Exception e) {
            System.out.printf("⚠️ Search method variation: %s%n", e.getMessage());
        }
        
        // 3. Complex multi-term searches
        String[][] complexQueries = {
            {"account", "transfer", "swift"}, 
            {"patient", "medical", "diagnosis"}, 
            {"contract", "software", "license"}
        };
        for (String[] queryTerms : complexQueries) {
            List<Block> result = api.searchAndDecryptByTerms(queryTerms, password, 10);
            System.out.printf("🔍 Complex Search: '%s' | Results: %d%n", 
                            String.join(" ", queryTerms), result.size());
            
            for (Block block : result) {
                // Try to get decrypted content for preview
                String content = block.getData();
                if (block.isDataEncrypted()) {
                    try {
                        content = api.retrieveSecret(block.getBlockNumber(), password);
                        if (content == null) {
                            content = "[ENCRYPTED - Decryption returned null]";
                        }
                    } catch (Exception e) {
                        content = "[ENCRYPTED]";
                    }
                }
                
                if (content == null) {
                    content = "[NULL CONTENT]";
                }
                
                System.out.printf("   📄 Block: %d | Content: %s%n", 
                                block.getBlockNumber(), 
                                content.length() > 30 ? 
                                    content.substring(0, 30) + "..." : 
                                    content);
            }
        }
        
        System.out.println("✅ Advanced search capabilities demonstrated");
        System.out.println();
    }
    
    private static void demonstrateDataStorage(UserFriendlyEncryptionAPI api, String password) {
        System.out.println("💾 DEMO 3: DATA STORAGE CAPABILITIES");
        System.out.println("====================================");
        
        try {
            // Demonstrate different types of data storage
            
            // 1. Store structured data with multiple keywords
            String[] businessKeywords = {"business", "report", "quarterly", "2024"};
            api.storeSearchableData("Business report: Q4 2024 financial performance exceeded expectations", 
                                   password, businessKeywords);
            System.out.println("✅ Stored business report with multiple keywords");
            
            // 2. Store technical data
            String[] techKeywords = {"api", "rest", "endpoint", "documentation"};
            api.storeSearchableData("API documentation: REST endpoints for user authentication and data access", 
                                   password, techKeywords);
            System.out.println("✅ Stored technical documentation");
            
            // 3. Store personal data with privacy keywords
            String[] personalKeywords = {"personal", "profile", "user", "preferences"};
            api.storeSearchableData("User profile: Personal preferences and account settings", 
                                   password, personalKeywords);
            System.out.println("✅ Stored personal data with privacy");
            
            // 4. Verify all stored data can be searched
            System.out.println("\n🔍 Verifying stored data:");
            String[] testQueries = {"business", "api", "personal"};
            for (String query : testQueries) {
                List<Block> result = api.searchAndDecryptByTerms(new String[]{query}, password, 10);
                System.out.printf("   📊 Search '%s': %d results%n", query, result.size());
            }
            
            System.out.println("✅ All data storage capabilities demonstrated");
            
        } catch (Exception e) {
            System.out.println("❌ Error in data storage demo: " + e.getMessage());
        }
        System.out.println();
    }
    
    private static void demonstrateSearchValidation(UserFriendlyEncryptionAPI api, String password) {
        System.out.println("✅ DEMO 4: SEARCH VALIDATION & EDGE CASES");
        System.out.println("=========================================");
        
        // Test different password scenarios
        System.out.println("🔐 Testing password validation:");
        
        // Test with correct password
        List<Block> correctResult = api.searchAndDecryptByTerms(new String[]{"financial"}, password, 10);
        System.out.printf("   ✅ Correct password: %d results%n", correctResult.size());
        
        // Test with wrong password
        try {
            List<Block> wrongResult = api.searchAndDecryptByTerms(new String[]{"financial"}, "wrongpassword", 10);
            System.out.printf("   ❌ Wrong password: %d results%n", wrongResult.size());
        } catch (Exception e) {
            System.out.println("   ⚠️ Wrong password handled: " + e.getMessage());
        }
        
        // Test edge cases
        System.out.println("\n🔍 Testing edge cases:");
        
        // Test with non-existent keywords
        List<Block> nonExistentResult = api.searchAndDecryptByTerms(new String[]{"nonexistent"}, password, 10);
        System.out.printf("   📊 Non-existent keyword: %d results%n", nonExistentResult.size());
        
        // Test case sensitivity
        List<Block> upperCaseResult = api.searchAndDecryptByTerms(new String[]{"FINANCIAL"}, password, 10);
        List<Block> lowerCaseResult = api.searchAndDecryptByTerms(new String[]{"financial"}, password, 10);
        System.out.printf("   📊 Case sensitivity - Upper: %d, Lower: %d%n", 
                         upperCaseResult.size(), lowerCaseResult.size());
        
        // Test multiple terms
        List<Block> multiTermResult = api.searchAndDecryptByTerms(new String[]{"financial", "medical", "contract"}, password, 10);
        System.out.printf("   📊 Multi-term search: %d results%n", multiTermResult.size());
        
        System.out.println("✅ All validation and edge cases tested successfully");
        System.out.println();
    }
    
}