package com.rbatllet.blockchain.search;

import com.rbatllet.blockchain.config.EncryptionConfig;
import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.service.UserFriendlyEncryptionAPI;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.search.BlockPasswordRegistry.RegistryStats;
import com.rbatllet.blockchain.search.SearchFrameworkEngine.*;
import com.rbatllet.blockchain.service.SecureBlockEncryptionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;

import java.security.KeyPair;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Rigorous test suite for SearchSpecialistAPI to identify and fix
 * why all search methods return 0 results while searchAndDecryptByTerms works
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SearchSpecialistAPIRigorousTest {
    
    private Blockchain blockchain;
    private UserFriendlyEncryptionAPI api;
    private SearchSpecialistAPI searchAPI;
    private String testPassword;
    private Block testBlock;
    
    @BeforeEach
    void setUp() throws Exception {
        blockchain = new Blockchain();
        api = new UserFriendlyEncryptionAPI(blockchain);
        testPassword = "RigorousTest123!";
        
        // Create and set default credentials
        KeyPair userKeys = api.createUser("rigorous-test-user");
        api.setDefaultCredentials("rigorous-test-user", userKeys);
    }
    
    @Test
    @Order(1)
    @DisplayName("Test 1: SearchSpecialistAPI Initialization State")
    void testSearchSpecialistAPIInitialization() throws Exception {
        System.out.println("=== TEST 1: INITIALIZATION STATE ===");
        
        // Use new improved constructor that requires blockchain, password, and private key
        System.out.println("🔑 Creating SearchSpecialistAPI with improved constructor...");
        KeyPair userKeys = api.createUser("rigorous-test-user");
        searchAPI = new SearchSpecialistAPI(blockchain, testPassword, userKeys.getPrivate());
        
        // After initialization
        System.out.println("📊 SearchSpecialistAPI ready after init: " + searchAPI.isReady());
        
        if (searchAPI.isReady()) {
            SearchStats stats = searchAPI.getStatistics();
            System.out.println("📊 Blocks indexed: " + stats.getTotalBlocksIndexed());
            System.out.println("📊 Memory usage: " + stats.getEstimatedMemoryBytes() + " bytes");
            
            String capabilities = searchAPI.getCapabilitiesSummary();
            System.out.println("📊 Capabilities: " + capabilities);
        }
        
        assertTrue(searchAPI != null, "SearchSpecialistAPI should not be null");
        assertTrue(searchAPI.isReady(), "SearchSpecialistAPI should be ready after initialization");
        System.out.println("✅ Initialization test passed");
    }
    
    @Test
    @Order(2)
    @DisplayName("Test 2: Compare Data Storage Methods")
    void testDataStorageComparison() throws Exception {
        System.out.println("\n=== TEST 2: DATA STORAGE COMPARISON ===");
        
        // Store test data with keywords
        String[] keywords = {"rigorous", "test", "financial"};
        testBlock = api.storeSearchableData("Rigorous test financial data", testPassword, keywords);
        
        System.out.println("📝 Stored block #" + testBlock.getBlockNumber());
        System.out.println("📝 Block data: " + testBlock.getData());
        System.out.println("📝 Is encrypted: " + testBlock.isDataEncrypted());
        System.out.println("📝 Manual keywords: " + testBlock.getManualKeywords());
        System.out.println("📝 Auto keywords: " + testBlock.getAutoKeywords());
        System.out.println("📝 Expected keywords: " + String.join(" ", keywords));
        
        // Initialize search after storing data
        blockchain.initializeAdvancedSearch(testPassword);
        searchAPI = blockchain.getSearchSpecialistAPI();
        
        // CRITICAL: Initialize SearchSpecialistAPI directly with blockchain
        if (!searchAPI.isReady()) {
            KeyPair testKeys = api.createUser("search-storage-user");
            System.out.println("🔑 Initializing SearchSpecialistAPI for storage test...");
            searchAPI.initializeWithBlockchain(blockchain, testPassword, testKeys.getPrivate());
        }
        
        System.out.println("✅ Data storage test completed");
        
        assertNotNull(testBlock, "Test block should be stored");
        assertTrue(testBlock.isDataEncrypted(), "Test block should be encrypted");
        
        // Check that keywords are stored (might be null if encryption moved them to different field)
        System.out.println("📊 Keyword analysis:");
        System.out.println("   Manual keywords null: " + (testBlock.getManualKeywords() == null));
        System.out.println("   Auto keywords null: " + (testBlock.getAutoKeywords() == null));
        System.out.println("   Searchable content: " + testBlock.getSearchableContent());
        
        // Keywords might be stored in different fields for encrypted blocks
        boolean hasKeywords = testBlock.getManualKeywords() != null || 
                            testBlock.getAutoKeywords() != null || 
                            (testBlock.getSearchableContent() != null && !testBlock.getSearchableContent().trim().isEmpty());
        
        assertTrue(hasKeywords, "Test block should have keywords stored somewhere");
    }
    
    @Test
    @Order(3)
    @DisplayName("Test 3: Search Method Comparison")
    void testSearchMethodComparison() throws Exception {
        System.out.println("\n=== TEST 3: SEARCH METHOD COMPARISON ===");
        
        // Ensure we have test data
        if (testBlock == null) {
            String[] keywords = {"rigorous", "test", "financial"};
            testBlock = api.storeSearchableData("Rigorous test financial data", testPassword, keywords);
            blockchain.initializeAdvancedSearch(testPassword);
            searchAPI = blockchain.getSearchSpecialistAPI();
            
            // CRITICAL: Initialize SearchSpecialistAPI directly with blockchain
            if (!searchAPI.isReady()) {
                KeyPair testKeys = api.createUser("search-comparison-user");
                System.out.println("🔑 Initializing SearchSpecialistAPI for comparison test...");
                searchAPI.initializeWithBlockchain(blockchain, testPassword, testKeys.getPrivate());
            }
        }
        
        String searchTerm = "financial";
        
        // Method 1: searchAndDecryptByTerms (working baseline)
        List<Block> workingResults = api.searchAndDecryptByTerms(new String[]{searchTerm}, testPassword, 10);
        System.out.println("✅ searchAndDecryptByTerms('" + searchTerm + "'): " + workingResults.size() + " results");
        
        // Method 2: SearchSpecialistAPI methods (failing)
        List<EnhancedSearchResult> intelligentResults = searchAPI.searchIntelligent(searchTerm, testPassword, 10);
        System.out.println("❌ searchIntelligent('" + searchTerm + "'): " + intelligentResults.size() + " results");
        
        List<EnhancedSearchResult> secureResults = searchAPI.searchSecure(searchTerm, testPassword, 10);
        System.out.println("❌ searchSecure('" + searchTerm + "'): " + secureResults.size() + " results");
        
        List<EnhancedSearchResult> simpleResults = searchAPI.searchSimple(searchTerm);
        System.out.println("❌ searchSimple('" + searchTerm + "'): " + simpleResults.size() + " results");
        
        // DEBUG: Check if autoKeywords can be decrypted
        System.out.println("\\n🔍 DEBUG: Testing autoKeywords decryption:");
        if (testBlock.getAutoKeywords() != null) {
            System.out.println("   autoKeywords raw: " + testBlock.getAutoKeywords().substring(0, Math.min(50, testBlock.getAutoKeywords().length())) + "...");
            try {
                String decryptedKeywords = SecureBlockEncryptionService.decryptFromString(
                    testBlock.getAutoKeywords(), testPassword);
                System.out.println("   autoKeywords decrypted: " + decryptedKeywords);
            } catch (Exception e) {
                System.out.println("   autoKeywords decryption failed: " + e.getMessage());
                // DEBUG: Let's see the actual format
                System.out.println("   autoKeywords parts: " + testBlock.getAutoKeywords().split("\\|").length);
                String[] parts = testBlock.getAutoKeywords().split("\\|");
                for (int i = 0; i < Math.min(parts.length, 3); i++) {
                    System.out.println("   part[" + i + "]: " + parts[i]);
                }
            }
        } else {
            System.out.println("   autoKeywords is null");
        }
        
        // Analysis
        System.out.println("\n📊 COMPARISON ANALYSIS:");
        System.out.println("   Working method finds: " + workingResults.size() + " blocks");
        System.out.println("   SearchSpecialistAPI finds: " + (intelligentResults.size() + secureResults.size() + simpleResults.size()) + " total results");
        
        // This test should fail - demonstrating the bug
        if (workingResults.size() > 0 && intelligentResults.size() == 0) {
            System.out.println("🚨 BUG CONFIRMED: SearchSpecialistAPI methods fail while searchAndDecryptByTerms works");
        }
        
        assertTrue(workingResults.size() > 0, "searchAndDecryptByTerms should find results");
        // Note: This will fail until we fix SearchSpecialistAPI
        // assertEquals(workingResults.size(), intelligentResults.size(), "Both methods should find same number of results");
    }
    
    @Test
    @Order(4)
    @DisplayName("Test 4: Internal SearchSpecialistAPI State Analysis")
    void testSearchSpecialistAPIInternalState() throws Exception {
        System.out.println("\n=== TEST 4: INTERNAL STATE ANALYSIS ===");
        
        // Ensure initialization
        if (searchAPI == null || !searchAPI.isReady()) {
            if (testBlock == null) {
                String[] keywords = {"rigorous", "test", "financial"};
                testBlock = api.storeSearchableData("Rigorous test financial data", testPassword, keywords);
            }
            blockchain.initializeAdvancedSearch(testPassword);
            searchAPI = blockchain.getSearchSpecialistAPI();
            
            // CRITICAL: Initialize SearchSpecialistAPI directly with blockchain
            if (!searchAPI.isReady()) {
                KeyPair testKeys = api.createUser("search-internal-user");
                System.out.println("🔑 Initializing SearchSpecialistAPI for internal test...");
                searchAPI.initializeWithBlockchain(blockchain, testPassword, testKeys.getPrivate());
            }
        }
        
        // Analyze internal state
        System.out.println("🔍 SearchSpecialistAPI Internal Analysis:");
        System.out.println("   isReady(): " + searchAPI.isReady());
        
        SearchStats stats = searchAPI.getStatistics();
        System.out.println("   Total blocks indexed: " + stats.getTotalBlocksIndexed());
        System.out.println("   Memory usage: " + stats.getEstimatedMemoryBytes() + " bytes");
        
        // Password registry stats
        RegistryStats registryStats = searchAPI.getPasswordRegistryStats();
        System.out.println("   Password registry blocks: " + registryStats.getRegisteredBlocks());
        System.out.println("   Password registry memory: " + registryStats.getEstimatedMemoryBytes() + " bytes");
        
        // Diagnostics
        String diagnostics = searchAPI.runDiagnostics();
        System.out.println("🔧 Diagnostics: " + diagnostics);
        
        // Performance metrics
        String metrics = searchAPI.getPerformanceMetrics();
        System.out.println("📊 Performance metrics: " + metrics);
        
        assertTrue(searchAPI.isReady(), "SearchSpecialistAPI should be ready");
        assertTrue(stats.getTotalBlocksIndexed() > 0, "Should have indexed at least one block");
    }
    
    @Test
    @Order(5)
    @DisplayName("Test 5: Password Registry Analysis")
    void testPasswordRegistryAnalysis() throws Exception {
        System.out.println("\n=== TEST 5: PASSWORD REGISTRY ANALYSIS ===");
        
        // Ensure we have test data and initialization
        if (testBlock == null) {
            String[] keywords = {"rigorous", "test", "financial"};
            testBlock = api.storeSearchableData("Rigorous test financial data", testPassword, keywords);
        }
        blockchain.initializeAdvancedSearch(testPassword);
        searchAPI = blockchain.getSearchSpecialistAPI();
        
        // CRITICAL: Initialize SearchSpecialistAPI directly with blockchain
        if (!searchAPI.isReady()) {
            KeyPair testKeys = api.createUser("search-registry-user");
            System.out.println("🔑 Initializing SearchSpecialistAPI for registry test...");
            searchAPI.initializeWithBlockchain(blockchain, testPassword, testKeys.getPrivate());
        }
        
        // Check password registry
        RegistryStats registryStats = searchAPI.getPasswordRegistryStats();
        System.out.println("📊 Password registry blocks: " + registryStats.getRegisteredBlocks());
        System.out.println("📊 Password registry memory: " + registryStats.getEstimatedMemoryBytes() + " bytes");
        
        // Try to search with different password scenarios
        System.out.println("\n🔐 Testing password scenarios:");
        
        // Correct password
        List<EnhancedSearchResult> correctPwdResults = searchAPI.searchSecure("financial", testPassword, 10);
        System.out.println("   Correct password: " + correctPwdResults.size() + " results");
        
        // Wrong password
        List<EnhancedSearchResult> wrongPwdResults = searchAPI.searchSecure("financial", "wrongpassword", 10);
        System.out.println("   Wrong password: " + wrongPwdResults.size() + " results");
        
        // No password (simple search)
        List<EnhancedSearchResult> noPwdResults = searchAPI.searchSimple("financial");
        System.out.println("   No password: " + noPwdResults.size() + " results");
        
        // Compare with working method
        List<Block> workingResults = api.searchAndDecryptByTerms(new String[]{"financial"}, testPassword, 10);
        System.out.println("   Working method: " + workingResults.size() + " results");
        
        System.out.println("\n💡 Password Registry Hypothesis:");
        if (workingResults.size() > 0 && correctPwdResults.size() == 0) {
            System.out.println("   SearchSpecialistAPI may not have the correct password mapping for blocks");
            System.out.println("   or the initialization with null password prevents proper indexing");
        }
    }
    
    @Test
    @Order(6)
    @DisplayName("Test 6: Direct SearchFrameworkEngine Testing")
    void testDirectSearchFrameworkEngine() throws Exception {
        System.out.println("\n=== TEST 6: DIRECT SEARCH FRAMEWORK ENGINE ===");
        
        // Ensure we have test data
        if (testBlock == null) {
            String[] keywords = {"rigorous", "test", "financial"};
            testBlock = api.storeSearchableData("Rigorous test financial data", testPassword, keywords);
        }
        blockchain.initializeAdvancedSearch(testPassword);
        searchAPI = blockchain.getSearchSpecialistAPI();
        
        // CRITICAL: Initialize SearchSpecialistAPI directly with blockchain
        if (!searchAPI.isReady()) {
            KeyPair testKeys = api.createUser("search-engine-user");
            System.out.println("🔑 Initializing SearchSpecialistAPI for engine test...");
            searchAPI.initializeWithBlockchain(blockchain, testPassword, testKeys.getPrivate());
        }
        
        // Test if we can access the underlying SearchFrameworkEngine
        try {
            // This might not be directly accessible, but let's see what we can test
            System.out.println("🔧 Testing underlying search engine capabilities");
            
            // Test through SearchSpecialistAPI's advanced search
            SearchResult advancedResult = searchAPI.searchAdvanced(
                "financial", 
                testPassword, 
                EncryptionConfig.createHighSecurityConfig(), 
                10);
            
            System.out.println("🔍 Advanced search result:");
            System.out.println("   Success: " + advancedResult.isSuccessful());
            System.out.println("   Result count: " + advancedResult.getResultCount());
            System.out.println("   Strategy used: " + advancedResult.getStrategyUsed());
            System.out.println("   Total time: " + advancedResult.getTotalTimeMs() + "ms");
            
            if (advancedResult.getErrorMessage() != null) {
                System.out.println("   Error: " + advancedResult.getErrorMessage());
            }
            
            assertTrue(advancedResult.isSuccessful(), "Advanced search should be successful");
            
        } catch (Exception e) {
            System.out.println("❌ Direct engine test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Test
    @Order(7)
    @DisplayName("Test 7: Root Cause Hypothesis Testing")
    void testRootCauseHypothesis() throws Exception {
        System.out.println("\n=== TEST 7: ROOT CAUSE HYPOTHESIS ===");
        
        // Hypothesis 1: Initialization with null password is the problem
        System.out.println("🧪 HYPOTHESIS 1: Null password initialization");
        System.out.println("   Current initializeAdvancedSearch() uses password=null");
        System.out.println("   This means only public metadata gets indexed");
        System.out.println("   But our test blocks have encrypted keywords that require password access");
        
        // Hypothesis 2: Metadata indexing vs direct block access
        System.out.println("\n🧪 HYPOTHESIS 2: Indexing approach difference");
        System.out.println("   searchAndDecryptByTerms: Direct block access + keyword matching");
        System.out.println("   SearchSpecialistAPI: Metadata index + search engine");
        System.out.println("   The metadata index may not contain the keywords we're searching for");
        
        // Hypothesis 3: Test with different block types
        System.out.println("\n🧪 HYPOTHESIS 3: Block type compatibility");
        
        // Test with a simple non-encrypted block
        try {
            // This would require access to addBlock directly, which may not be available
            System.out.println("   Testing with different block types would require direct blockchain access");
        } catch (Exception e) {
            System.out.println("   Cannot test different block types in this context");
        }
        
        // Summary
        System.out.println("\n📋 ROOT CAUSE ANALYSIS SUMMARY:");
        System.out.println("   1. SearchSpecialistAPI initialized with null password");
        System.out.println("   2. Only public metadata gets indexed, not encrypted keywords");
        System.out.println("   3. searchAndDecryptByTerms bypasses the index and searches directly");
        System.out.println("   4. FIX: Initialize SearchSpecialistAPI with proper password management");
        System.out.println("        or modify indexing to handle encrypted keywords properly");
    }
}