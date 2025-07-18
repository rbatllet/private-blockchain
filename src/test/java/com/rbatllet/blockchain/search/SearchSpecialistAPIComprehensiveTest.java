package com.rbatllet.blockchain.search;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.service.UserFriendlyEncryptionAPI;
import com.rbatllet.blockchain.search.SearchFrameworkEngine.*;
import com.rbatllet.blockchain.config.EncryptionConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;

import java.security.KeyPair;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for SearchSpecialistAPI
 * Tests all search methods with different scenarios and validates performance
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SearchSpecialistAPIComprehensiveTest {
    
    private Blockchain blockchain;
    private UserFriendlyEncryptionAPI api;
    private SearchSpecialistAPI searchAPI;
    private String testPassword;
    private KeyPair userKeys;
    
    @BeforeEach
    void setUp() throws Exception {
        blockchain = new Blockchain();
        api = new UserFriendlyEncryptionAPI(blockchain);
        testPassword = "ComprehensiveTest123!";
        
        // Create user and set credentials
        userKeys = api.createUser("comprehensive-test-user");
        api.setDefaultCredentials("comprehensive-test-user", userKeys);
        
        // Create test data
        createTestData();
        
        // Initialize search API
        initializeSearchAPI();
    }
    
    private void createTestData() throws Exception {
        // Create encrypted block with searchable keywords
        String[] encryptedKeywords = {"financial", "healthcare", "confidential", "report"};
        api.storeSearchableData(
            "Confidential financial healthcare report with sensitive data", 
            testPassword, 
            encryptedKeywords
        );
        
        // Create another encrypted block with different keywords
        api.storeSearchableData("Public announcement data", testPassword, new String[]{"public", "announcement"});
    }
    
    private void initializeSearchAPI() throws Exception {
        // Use new constructor that requires blockchain, password, and private key
        searchAPI = new SearchSpecialistAPI(blockchain, testPassword, userKeys.getPrivate());
        
        assertTrue(searchAPI.isReady(), "SearchSpecialistAPI should be ready after initialization");
    }
    
    @Test
    @Order(1)
    @DisplayName("Test 1: searchSimple() - Generic search functionality")
    void testSearchSimpleGeneric() throws Exception {
        System.out.println("=== TEST 1: searchSimple() GENERIC FUNCTIONALITY ===");
        
        // Test with different search terms
        String[] testTerms = {"financial", "healthcare", "confidential", "nonexistent"};
        
        for (String term : testTerms) {
            List<EnhancedSearchResult> results = searchAPI.searchSimple(term);
            System.out.println("📊 searchSimple('" + term + "'): " + results.size() + " results");
            
            if (term.equals("nonexistent")) {
                assertEquals(0, results.size(), "Should find no results for non-existent term");
            } else {
                assertTrue(results.size() > 0, "Should find results for existing term: " + term);
            }
        }
        
        // Test with custom result limit
        List<EnhancedSearchResult> limitedResults = searchAPI.searchSimple("financial", 3);
        assertTrue(limitedResults.size() <= 3, "Should respect result limit");
        
        System.out.println("✅ searchSimple() generic functionality validated");
    }
    
    @Test
    @Order(2)
    @DisplayName("Test 2: searchSecure() - Encrypted content access")
    void testSearchSecure() throws Exception {
        System.out.println("\n=== TEST 2: searchSecure() ENCRYPTED CONTENT ACCESS ===");
        
        // Test with correct password
        List<EnhancedSearchResult> correctResults = searchAPI.searchSecure("financial", testPassword);
        System.out.println("📊 searchSecure with correct password: " + correctResults.size() + " results");
        assertTrue(correctResults.size() > 0, "Should find results with correct password");
        
        // Test with wrong password
        List<EnhancedSearchResult> wrongResults = searchAPI.searchSecure("financial", "wrongpassword");
        System.out.println("📊 searchSecure with wrong password: " + wrongResults.size() + " results");
        // Wrong password should still return some results from public metadata
        
        // Test with custom limit
        List<EnhancedSearchResult> limitedSecureResults = searchAPI.searchSecure("healthcare", testPassword, 5);
        assertTrue(limitedSecureResults.size() <= 5, "Should respect custom limit");
        
        System.out.println("✅ searchSecure() encrypted content access validated");
    }
    
    @Test
    @Order(3)
    @DisplayName("Test 3: searchIntelligent() - Adaptive search strategy")
    void testSearchIntelligent() throws Exception {
        System.out.println("\n=== TEST 3: searchIntelligent() ADAPTIVE STRATEGY ===");
        
        // Test intelligent search with different complexities
        String[] queries = {
            "financial",                    // Simple term
            "healthcare report",            // Multiple terms
            "confidential AND financial",   // Boolean query
            "healthcare OR medical"         // OR query
        };
        
        for (String query : queries) {
            List<EnhancedSearchResult> results = searchAPI.searchIntelligent(query, testPassword, 10);
            System.out.println("📊 searchIntelligent('" + query + "'): " + results.size() + " results");
            
            // Validate results have metadata
            for (EnhancedSearchResult result : results) {
                assertNotNull(result.getBlockHash(), "Result should have block hash");
                assertTrue(result.getRelevanceScore() > 0, "Result should have positive relevance score");
            }
        }
        
        System.out.println("✅ searchIntelligent() adaptive strategy validated");
    }
    
    @Test
    @Order(4)
    @DisplayName("Test 4: searchAdvanced() - Expert-level control")
    void testSearchAdvanced() throws Exception {
        System.out.println("\n=== TEST 4: searchAdvanced() EXPERT CONTROL ===");
        
        // Test with different encryption configs
        EncryptionConfig[] configs = {
            EncryptionConfig.createHighSecurityConfig(),
            EncryptionConfig.createBalancedConfig(),
            EncryptionConfig.createPerformanceConfig()
        };
        
        for (EncryptionConfig config : configs) {
            SearchResult result = searchAPI.searchAdvanced("financial", testPassword, config, 10);
            System.out.println("📊 searchAdvanced with " + config.getClass().getSimpleName() + ":");
            System.out.println("    Success: " + result.isSuccessful());
            System.out.println("    Results: " + result.getResultCount());
            System.out.println("    Strategy: " + result.getStrategyUsed());
            System.out.println("    Time: " + result.getTotalTimeMs() + "ms");
            
            assertTrue(result.isSuccessful(), "Advanced search should be successful");
            assertTrue(result.getResultCount() >= 0, "Should have valid result count");
            assertTrue(result.getTotalTimeMs() > 0, "Should have positive execution time");
        }
        
        System.out.println("✅ searchAdvanced() expert control validated");
    }
    
    @Test
    @Order(5)
    @DisplayName("Test 5: Performance comparison between methods")
    void testPerformanceComparison() throws Exception {
        System.out.println("\n=== TEST 5: PERFORMANCE COMPARISON ===");
        
        String searchTerm = "financial";
        int iterations = 5;
        
        // Test searchSimple performance
        long simpleTotal = 0;
        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            searchAPI.searchSimple(searchTerm);
            long end = System.nanoTime();
            simpleTotal += (end - start);
        }
        double simpleAvg = (simpleTotal / iterations) / 1_000_000.0;
        
        // Test searchSecure performance  
        long secureTotal = 0;
        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            searchAPI.searchSecure(searchTerm, testPassword);
            long end = System.nanoTime();
            secureTotal += (end - start);
        }
        double secureAvg = (secureTotal / iterations) / 1_000_000.0;
        
        // Test searchIntelligent performance
        long intelligentTotal = 0;
        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            searchAPI.searchIntelligent(searchTerm, testPassword, 10);
            long end = System.nanoTime();
            intelligentTotal += (end - start);
        }
        double intelligentAvg = (intelligentTotal / iterations) / 1_000_000.0;
        
        System.out.println("📊 Performance Results (average over " + iterations + " iterations):");
        System.out.println("    searchSimple: " + String.format("%.2f", simpleAvg) + "ms");
        System.out.println("    searchSecure: " + String.format("%.2f", secureAvg) + "ms");
        System.out.println("    searchIntelligent: " + String.format("%.2f", intelligentAvg) + "ms");
        
        // All methods should complete within reasonable time
        assertTrue(simpleAvg < 1000, "searchSimple should complete within 1 second");
        assertTrue(secureAvg < 1000, "searchSecure should complete within 1 second");
        assertTrue(intelligentAvg < 1000, "searchIntelligent should complete within 1 second");
        
        System.out.println("✅ Performance comparison validated");
    }
    
    @Test
    @Order(6)
    @DisplayName("Test 6: Result consistency between methods")
    void testResultConsistency() throws Exception {
        System.out.println("\n=== TEST 6: RESULT CONSISTENCY ===");
        
        String searchTerm = "financial";
        
        // Get results from all methods
        List<EnhancedSearchResult> simpleResults = searchAPI.searchSimple(searchTerm);
        List<EnhancedSearchResult> secureResults = searchAPI.searchSecure(searchTerm, testPassword);
        List<EnhancedSearchResult> intelligentResults = searchAPI.searchIntelligent(searchTerm, testPassword, 20);
        
        System.out.println("📊 Result counts:");
        System.out.println("    searchSimple: " + simpleResults.size());
        System.out.println("    searchSecure: " + secureResults.size());
        System.out.println("    searchIntelligent: " + intelligentResults.size());
        
        // Since all methods now use the same defaultPassword, they should return similar results
        // Allow for small variations due to different search strategies
        int minResults = Math.min(Math.min(simpleResults.size(), secureResults.size()), intelligentResults.size());
        int maxResults = Math.max(Math.max(simpleResults.size(), secureResults.size()), intelligentResults.size());
        
        assertTrue(maxResults - minResults <= 2, "Result counts should be similar across methods");
        
        // Verify all results have valid data
        for (EnhancedSearchResult result : simpleResults) {
            assertNotNull(result.getBlockHash(), "Simple result should have block hash");
            assertTrue(result.getRelevanceScore() > 0, "Simple result should have positive relevance");
        }
        
        System.out.println("✅ Result consistency validated");
    }
    
    @Test
    @Order(7)
    @DisplayName("Test 7: API state management")
    void testAPIStateManagement() throws Exception {
        System.out.println("\n=== TEST 7: API STATE MANAGEMENT ===");
        
        // Test isReady()
        assertTrue(searchAPI.isReady(), "API should be ready after initialization");
        
        // Test statistics
        SearchStats stats = searchAPI.getStatistics();
        System.out.println("📊 Search statistics:");
        System.out.println("    Total blocks indexed: " + stats.getTotalBlocksIndexed());
        System.out.println("    Memory usage: " + stats.getEstimatedMemoryBytes() + " bytes");
        
        assertTrue(stats.getTotalBlocksIndexed() > 0, "Should have indexed blocks");
        assertTrue(stats.getEstimatedMemoryBytes() > 0, "Should have memory usage");
        
        // Test password registry
        BlockPasswordRegistry.RegistryStats registryStats = searchAPI.getPasswordRegistryStats();
        System.out.println("📊 Password registry:");
        System.out.println("    Registered blocks: " + registryStats.getRegisteredBlocks());
        System.out.println("    Memory usage: " + registryStats.getEstimatedMemoryBytes() + " bytes");
        
        assertTrue(registryStats.getRegisteredBlocks() >= 0, "Should have valid registry count");
        
        // Test registered blocks
        Set<String> registeredBlocks = searchAPI.getRegisteredBlocks();
        System.out.println("📊 Registered blocks count: " + registeredBlocks.size());
        
        // Test capabilities
        String capabilities = searchAPI.getCapabilitiesSummary();
        System.out.println("📊 Capabilities: " + capabilities);
        assertNotNull(capabilities, "Should have capabilities summary");
        
        System.out.println("✅ API state management validated");
    }
    
    @Test
    @Order(8)
    @DisplayName("Test 8: Constructor comparison - old vs new")
    void testConstructorComparison() throws Exception {
        System.out.println("\n=== TEST 8: CONSTRUCTOR COMPARISON ===");
        
        // Test old constructor (should work but with warning)
        SearchSpecialistAPI oldAPI = new SearchSpecialistAPI();
        assertFalse(oldAPI.isReady(), "Old API should not be ready without initialization");
        
        // Test search with old API (should return empty results)
        List<EnhancedSearchResult> oldResults = oldAPI.searchSimple("financial");
        System.out.println("📊 Old constructor search results: " + oldResults.size());
        assertEquals(0, oldResults.size(), "Old API without initialization should return no results");
        
        // Test new constructor (should work correctly)
        SearchSpecialistAPI newAPI = new SearchSpecialistAPI(blockchain, testPassword, userKeys.getPrivate());
        assertTrue(newAPI.isReady(), "New API should be ready immediately");
        
        // Test search with new API (should find results)
        List<EnhancedSearchResult> newResults = newAPI.searchSimple("financial");
        System.out.println("📊 New constructor search results: " + newResults.size());
        assertTrue(newResults.size() > 0, "New API should find results immediately");
        
        System.out.println("✅ Constructor comparison validated");
    }
    
    @Test
    @Order(9)
    @DisplayName("Test 9: Error handling and edge cases")
    void testErrorHandling() throws Exception {
        System.out.println("\n=== TEST 9: ERROR HANDLING ===");
        
        // Test null/empty queries
        assertThrows(IllegalArgumentException.class, () -> {
            searchAPI.searchSimple(null);
        }, "Should throw exception for null query");
        
        assertThrows(IllegalArgumentException.class, () -> {
            searchAPI.searchSimple("");
        }, "Should throw exception for empty query");
        
        // Test invalid result limits
        assertThrows(IllegalArgumentException.class, () -> {
            searchAPI.searchSimple("test", 0);
        }, "Should throw exception for zero limit");
        
        assertThrows(IllegalArgumentException.class, () -> {
            searchAPI.searchSimple("test", -1);
        }, "Should throw exception for negative limit");
        
        // Test null password in secure search
        assertThrows(IllegalArgumentException.class, () -> {
            searchAPI.searchSecure("test", null);
        }, "Should throw exception for null password");
        
        // Test empty password in secure search
        assertThrows(IllegalArgumentException.class, () -> {
            searchAPI.searchSecure("test", "");
        }, "Should throw exception for empty password");
        
        System.out.println("✅ Error handling validated");
    }
    
    @Test
    @Order(10)
    @DisplayName("Test 10: Search with special characters and unicode")
    void testSpecialCharactersAndUnicode() throws Exception {
        System.out.println("\n=== TEST 10: SPECIAL CHARACTERS AND UNICODE ===");
        
        // Test with special characters
        String[] specialQueries = {
            "test@example.com",
            "file.txt",
            "user-name",
            "test_case",
            "café",
            "naïve",
            "データ"
        };
        
        for (String query : specialQueries) {
            try {
                List<EnhancedSearchResult> results = searchAPI.searchSimple(query);
                System.out.println("📊 Special query '" + query + "': " + results.size() + " results");
                // Should not throw exceptions, even if no results found
                assertTrue(results.size() >= 0, "Should handle special characters gracefully");
            } catch (Exception e) {
                fail("Should not throw exception for special characters: " + query + " - " + e.getMessage());
            }
        }
        
        System.out.println("✅ Special characters and unicode handling validated");
    }
}