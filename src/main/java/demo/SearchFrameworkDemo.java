package demo;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.config.EncryptionConfig;
import com.rbatllet.blockchain.search.SearchSpecialistAPI;
import com.rbatllet.blockchain.search.SearchFrameworkEngine.*;
import com.rbatllet.blockchain.util.CryptoUtil;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
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
            // Create demo blockchain with varied content
            System.out.println("📊 Setting up demo blockchain with encrypted content...");
            Blockchain blockchain = createDemoBlockchain();
            
            // Initialize advanced search engine
            System.out.println("⚡ Initializing Search Framework Search Engine...");
            SearchSpecialistAPI searchAPI = new SearchSpecialistAPI(
                EncryptionConfig.createHighSecurityConfig());
            
            // Demo password and crypto setup
            String demoPassword = "SecureSearchDemo2024!";
            KeyPair keyPair = CryptoUtil.generateKeyPair();
            PrivateKey privateKey = keyPair.getPrivate();
            
            // Index the blockchain
            System.out.println("🔍 Indexing blockchain for advanced search...");
            IndexingResult indexingResult = searchAPI.initializeWithBlockchain(blockchain, demoPassword, privateKey);
            System.out.println("   " + indexingResult);
            System.out.println();
            
            // Show engine capabilities
            System.out.println(searchAPI.getCapabilitiesSummary());
            System.out.println();
            
            // Demo 1: Fast Public Search
            demonstrateFastPublicSearch(searchAPI);
            
            // Demo 2: Encrypted Content Search
            demonstrateEncryptedSearch(searchAPI, demoPassword);
            
            
            // Demo 3: Intelligent Auto-Routing
            demonstrateIntelligentRouting(searchAPI, demoPassword);
            
            // Demo 5: Specialized Searches
            demonstrateSpecializedSearches(searchAPI, demoPassword);
            
            // Show performance metrics
            System.out.println("📈 PERFORMANCE METRICS");
            System.out.println("=====================");
            System.out.println(searchAPI.getPerformanceMetrics());
            System.out.println();
            
            // Run diagnostics
            System.out.println("🔧 SYSTEM DIAGNOSTICS");
            System.out.println("====================");
            System.out.println(searchAPI.runDiagnostics());
            
            // Cleanup
            searchAPI.shutdown();
            
            System.out.println("✅ Search Framework Search Demo completed successfully!");
            System.out.println("🎯 The future of blockchain search is here!");
            
        } catch (Exception e) {
            System.err.println("❌ Demo failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static Blockchain createDemoBlockchain() throws Exception {
        Blockchain blockchain = new Blockchain();
        
        // Generate key pair for demo
        KeyPair keyPair = CryptoUtil.generateKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();
        
        // Add diverse blocks for comprehensive search testing
        
        // Financial block
        blockchain.addBlock("SWIFT MT103 transfer from Account ES1234567890 to DE0987654321. " +
                          "Amount: €50000.00. Reference: TXN-FIN-2024-001. " +
                          "Purpose: Commercial invoice payment for medical equipment.", privateKey, publicKey);
        
        // Medical block  
        blockchain.addBlock("Patient John Smith (ID: P-12345) diagnosed with hypertension. " +
                          "Treatment: Lisinopril 10mg daily. Doctor: Dr. Sarah Johnson. " +
                          "Hospital: Central Medical Center. Date: 2024-01-15.", privateKey, publicKey);
        
        // Legal contract
        blockchain.addBlock("Software License Agreement between TechCorp Ltd and MediSoft Inc. " +
                          "Contract ID: LEGAL-2024-SLA-789. Value: $250,000. " +
                          "Terms: 3-year enterprise license with support.", privateKey, publicKey);
        
        // Technical data
        blockchain.addBlock("API Response: {\"status\":\"success\",\"data\":{\"users\":1250,\"transactions\":5847," +
                          "\"hash\":\"SHA256-abc123def456\"},\"timestamp\":\"2024-01-15T10:30:00Z\"," +
                          "\"version\":\"2.1.0\"}", privateKey, publicKey);
        
        // Personal information
        blockchain.addBlock("Employee record: Maria Garcia, email: maria.garcia@company.com, " +
                          "phone: +34-912-345-678, department: Finance, salary: €65000, " +
                          "start_date: 2023-06-01.", privateKey, publicKey);
        
        // Scientific research
        blockchain.addBlock("Research findings: DNA sequence analysis ATCGTACGTATCG shows 95% correlation " +
                          "with target protein. Lab: BioTech Research Center. " +
                          "Publication ID: PMID-789123456.", privateKey, publicKey);
        
        // Cryptocurrency transaction
        blockchain.addBlock("Bitcoin transaction: from 1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa " +
                          "to 3J98t1WpEZ73CNmQviecrnyiWrnqRhWNLy amount: 2.5 BTC " +
                          "fee: 0.0001 BTC block_height: 820000", privateKey, publicKey);
        
        // Manufacturing data
        blockchain.addBlock("Production batch B-2024-156: 500 units of Model X-200. " +
                          "Quality score: 98.5%. Defects: 2 units. " +
                          "Materials: Steel grade A300, Plastic PVC-401.", privateKey, publicKey);
        
        // Audit log
        blockchain.addBlock("System audit: User admin_user performed backup operation. " +
                          "Files: 15,847 documents (2.3GB). Success: true. " +
                          "Duration: 45 minutes. Checksum: MD5-9a8b7c6d5e4f.", privateKey, publicKey);
        
        // Environmental data
        blockchain.addBlock("Weather station WS-001: Temperature 23.5°C, Humidity 65%, " +
                          "Wind speed 12 km/h, Pressure 1013.2 hPa. " +
                          "Air quality index: 42 (Good). Location: Barcelona, Spain.", privateKey, publicKey);
        
        return blockchain;
    }
    
    private static void demonstrateFastPublicSearch(SearchSpecialistAPI searchAPI) {
        System.out.println("⚡ DEMO 1: LIGHTNING-FAST PUBLIC SEARCH");
        System.out.println("======================================");
        
        long startTime = System.nanoTime();
        List<EnhancedSearchResult> results = searchAPI.searchSimple("financial");
        long endTime = System.nanoTime();
        double timeMs = (endTime - startTime) / 1_000_000.0;
        
        System.out.printf("🔍 Search: 'financial' | Results: %d | Time: %.2f ms%n", results.size(), timeMs);
        
        for (EnhancedSearchResult result : results) {
            System.out.printf("   📄 Block: %s | Score: %.1f | Source: %s%n", 
                            result.getBlockHash().substring(0, 8), 
                            result.getRelevanceScore(),
                            result.getSource());
        }
        System.out.println();
        
        // Test multiple fast searches
        String[] fastQueries = {"medical", "contract", "bitcoin", "temperature"};
        for (String query : fastQueries) {
            startTime = System.nanoTime();
            results = searchAPI.searchSimple(query, 3);
            endTime = System.nanoTime();
            timeMs = (endTime - startTime) / 1_000_000.0;
            
            System.out.printf("🔍 Search: '%s' | Results: %d | Time: %.2f ms%n", 
                            query, results.size(), timeMs);
        }
        System.out.println();
    }
    
    private static void demonstrateEncryptedSearch(SearchSpecialistAPI searchAPI, String password) {
        System.out.println("🔐 DEMO 2: ENCRYPTED CONTENT DEEP SEARCH");
        System.out.println("=======================================");
        
        long startTime = System.nanoTime();
        List<EnhancedSearchResult> results = searchAPI.searchSecure("account transfer amount", password);
        long endTime = System.nanoTime();
        double timeMs = (endTime - startTime) / 1_000_000.0;
        
        System.out.printf("🔍 Encrypted Search: 'account transfer amount' | Results: %d | Time: %.2f ms%n", 
                        results.size(), timeMs);
        
        for (EnhancedSearchResult result : results) {
            System.out.printf("   📄 Block: %s | Score: %.1f | Has Private: %s%n", 
                            result.getBlockHash().substring(0, 8), 
                            result.getRelevanceScore(),
                            result.hasPrivateAccess() ? "✅" : "❌");
            
            if (result.hasPrivateAccess()) {
                System.out.printf("      🔓 Private Keywords: %d | Category: %s%n",
                                result.getPrivateMetadata().getDetailedKeywords().size(),
                                result.getPrivateMetadata().getContentCategory());
            }
        }
        System.out.println();
        
        // Test sensitive data search
        results = searchAPI.searchSecure("john smith patient medical", password);
        System.out.printf("🏥 Medical Search Results: %d blocks found%n", results.size());
        for (EnhancedSearchResult result : results) {
            if (result.hasPrivateAccess()) {
                System.out.printf("   📋 Medical Block: %s | Sensitive Terms: %d%n", 
                                result.getBlockHash().substring(0, 8),
                                result.getPrivateMetadata().getSensitiveTerms().size());
            }
        }
        System.out.println();
    }
    
    
    private static void demonstrateIntelligentRouting(SearchSpecialistAPI searchAPI, String password) {
        System.out.println("🧠 DEMO 4: INTELLIGENT STRATEGY ROUTING");
        System.out.println("======================================");
        
        // Simple query -> Fast public search
        SearchResult result = searchAPI.searchAdvanced("medical", null, EncryptionConfig.createBalancedConfig(), 10);
        System.out.printf("🔍 Simple Query 'medical' -> Strategy: %s | Time: %.2f ms%n", 
                        result.getStrategyUsed(), result.getTotalTimeMs());
        
        // Complex query with password -> Encrypted search
        result = searchAPI.searchAdvanced("john smith patient diagnosis treatment", password, EncryptionConfig.createBalancedConfig(), 10);
        System.out.printf("🔍 Complex Query + Password -> Strategy: %s | Time: %.2f ms%n", 
                        result.getStrategyUsed(), result.getTotalTimeMs());
        
        // Very complex query -> Hybrid cascade
        result = searchAPI.searchAdvanced("financial swift transaction amount account transfer AND medical patient", password, EncryptionConfig.createBalancedConfig(), 10);
        System.out.printf("🔍 Very Complex Query -> Strategy: %s | Time: %.2f ms%n", 
                        result.getStrategyUsed(), result.getTotalTimeMs());
        
        // Show query analysis
        if (result.getAnalysis() != null) {
            System.out.printf("   📊 Query Complexity: %s | Security Level: %s%n",
                            result.getAnalysis().getComplexity(),
                            result.getAnalysis().getSecurityLevel());
        }
        System.out.println();
    }
    
    private static void demonstrateSpecializedSearches(SearchSpecialistAPI searchAPI, String password) {
        System.out.println("🎯 DEMO 5: USER-DEFINED TERM SEARCHES");
        System.out.println("======================================");
        
        // Search by user-defined terms (content-agnostic approach)
        List<EnhancedSearchResult> results = searchAPI.searchIntelligent("transfer", password, 10);
        System.out.printf("💰 Search for 'transfer': %d results%n", results.size());
        
        // Search for patient-related content
        results = searchAPI.searchIntelligent("patient", password, 10);
        System.out.printf("🏥 Search for 'patient': %d results%n", results.size());
        
        // Search for contract-related content
        results = searchAPI.searchIntelligent("contract", password, 10);
        System.out.printf("⚖️ Search for 'contract': %d results%n", results.size());
        
        // High-value search (using user-defined terms instead of hardcoded categories)
        results = searchAPI.searchIntelligent("amount value financial bitcoin", password, 10);
        System.out.printf("💎 High-Value Search (user-defined terms): %d results%n", results.size());
        
        // Comprehensive multi-strategy search
        SearchResult comprehensive = searchAPI.searchAdvanced("data information", password, EncryptionConfig.createBalancedConfig(), 50);
        System.out.printf("🔄 Comprehensive Search: %d results | Strategy: %s | Time: %.2f ms%n", 
                        comprehensive.getResultCount(), 
                        comprehensive.getStrategyUsed(),
                        comprehensive.getTotalTimeMs());
        System.out.println();
    }
}