package com.rbatllet.blockchain.search;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.indexing.IndexingCoordinator;
import com.rbatllet.blockchain.service.UserFriendlyEncryptionAPI;
import com.rbatllet.blockchain.search.SearchFrameworkEngine.*;
import com.rbatllet.blockchain.config.EncryptionConfig;
import com.rbatllet.blockchain.security.UserRole;
import com.rbatllet.blockchain.util.TestGenesisKeyManager;
import com.rbatllet.blockchain.util.CryptoUtil;
import org.junit.jupiter.api.AfterEach;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Comprehensive test suite for SearchSpecialistAPI
 * Tests all search methods with different scenarios and validates performance
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SearchSpecialistAPIComprehensiveTest {
    private static final Logger logger = LoggerFactory.getLogger(SearchSpecialistAPIComprehensiveTest.class);

    
    private Blockchain blockchain;
    private KeyPair bootstrapKeyPair;
    private UserFriendlyEncryptionAPI api;
    private SearchSpecialistAPI searchAPI;
    private String testPassword;
    private KeyPair userKeys;
    
    @BeforeEach
    void setUp() throws Exception {
        blockchain = new Blockchain();

        // RBAC FIX (v1.0.6): Clear database before bootstrap to avoid "Existing users" error
        blockchain.clearAndReinitialize();

        // Load bootstrap admin keys (auto-generates if missing - test-only)
        bootstrapKeyPair = TestGenesisKeyManager.ensureGenesisKeysExist();

        // SECURITY (v1.0.6): Register bootstrap admin in blockchain (REQUIRED!)
        blockchain.createBootstrapAdmin(
            CryptoUtil.publicKeyToString(bootstrapKeyPair.getPublic()),
            "BOOTSTRAP_ADMIN"
        );

        api = new UserFriendlyEncryptionAPI(blockchain);
        testPassword = "ComprehensiveTest123!";

        // PRE-AUTHORIZATION REQUIRED (v1.0.6 RBAC security):
        // 1. Create admin to act as authorized user creator
        KeyPair adminKeys = CryptoUtil.generateKeyPair();
        String adminPublicKey = CryptoUtil.publicKeyToString(adminKeys.getPublic());
        blockchain.addAuthorizedKey(adminPublicKey, "Admin", bootstrapKeyPair, UserRole.ADMIN);
        api.setDefaultCredentials("Admin", adminKeys);  // Authenticate as admin
        
        // 2. Now admin can create test user (generates new keys internally)
        userKeys = api.createUser("comprehensive-test-user");
        api.setDefaultCredentials("comprehensive-test-user", userKeys);
        
        // Initialize search API FIRST (before creating test data)
        initializeSearchAPI();
        
        // Create test data AFTER SearchSpecialistAPI is initialized
        createTestData();
        
        // CRITICAL: Wait for async indexing to complete before running tests
        IndexingCoordinator.getInstance().waitForCompletion();
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        // Phase 5.4 FIX: Wait for async indexing to complete before cleanup
        IndexingCoordinator.getInstance().waitForCompletion();

        // CRITICAL: Clear database + search indexes to prevent state contamination
        if (blockchain != null) {
            blockchain.clearAndReinitialize();  // Also calls clearIndexes() + clearCache()
        }

        // Reset IndexingCoordinator singleton state
        IndexingCoordinator.getInstance().forceShutdown();
        IndexingCoordinator.getInstance().clearShutdownFlag();
        IndexingCoordinator.getInstance().disableTestMode();

        if (blockchain != null) {
            blockchain.shutdown();
        }
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
        // Initialize SearchSpecialistAPI in the blockchain first
        blockchain.getSearchSpecialistAPI().initializeWithBlockchain(blockchain, testPassword, userKeys.getPrivate());
        
        // Use new constructor that requires blockchain, password, and private key
        searchAPI = new SearchSpecialistAPI(blockchain, testPassword, userKeys.getPrivate());
        
        assertTrue(searchAPI.isReady(), "SearchSpecialistAPI should be ready after initialization");
    }
    
    @Test
    @Order(1)
    @DisplayName("Test 1: searchAll() - Generic search functionality")
    void testSearchAll() throws Exception {
        logger.info("=== TEST 1: searchAll() GENERIC FUNCTIONALITY ===");
        
        // Test with different search terms
        String[] testTerms = {"financial", "healthcare", "confidential", "nonexistent"};
        
        for (String term : testTerms) {
            List<EnhancedSearchResult> results = searchAPI.searchAll(term);
            logger.info("📊 searchAll('" + term + "'): " + results.size() + " results");
            
            if (term.equals("nonexistent")) {
                assertEquals(0, results.size(), "Should find no results for non-existent term");
            } else {
                assertTrue(results.size() > 0, "Should find results for existing term: " + term);
            }
        }
        
        // Test with custom result limit
        List<EnhancedSearchResult> limitedResults = searchAPI.searchAll("financial", 3);
        assertTrue(limitedResults.size() <= 3, "Should respect result limit");
        
        logger.info("✅ searchAll() generic functionality validated");
    }
    
    @Test
    @Order(2)
    @DisplayName("Test 2: searchSecure() - Encrypted content access")
    void testSearchSecure() throws Exception {
        logger.info("\n=== TEST 2: searchSecure() ENCRYPTED CONTENT ACCESS ===");
        
        // Test with correct password
        List<EnhancedSearchResult> correctResults = searchAPI.searchSecure("financial", testPassword);
        logger.info("📊 searchSecure with correct password: " + correctResults.size() + " results");
        assertTrue(correctResults.size() > 0, "Should find results with correct password");
        
        // Test with wrong password
        List<EnhancedSearchResult> wrongResults = searchAPI.searchSecure("financial", "wrongpassword");
        logger.info("📊 searchSecure with wrong password: " + wrongResults.size() + " results");
        // Wrong password should still return some results from public metadata
        
        // Test with custom limit
        List<EnhancedSearchResult> limitedSecureResults = searchAPI.searchSecure("healthcare", testPassword, 5);
        assertTrue(limitedSecureResults.size() <= 5, "Should respect custom limit");
        
        logger.info("✅ searchSecure() encrypted content access validated");
    }
    
    @Test
    @Order(3)
    @DisplayName("Test 3: searchIntelligent() - Adaptive search strategy")
    void testSearchIntelligent() throws Exception {
        logger.info("\n=== TEST 3: searchIntelligent() ADAPTIVE STRATEGY ===");
        
        // Test intelligent search with different complexities
        String[] queries = {
            "financial",                    // Simple term
            "healthcare report",            // Multiple terms
            "confidential AND financial",   // Boolean query
            "healthcare OR medical"         // OR query
        };
        
        for (String query : queries) {
            List<EnhancedSearchResult> results = searchAPI.searchIntelligent(query, testPassword, 10);
            logger.info("📊 searchIntelligent('" + query + "'): " + results.size() + " results");
            
            // Validate results have metadata
            for (EnhancedSearchResult result : results) {
                assertNotNull(result.getBlockHash(), "Result should have block hash");
                assertTrue(result.getRelevanceScore() > 0, "Result should have positive relevance score");
            }
        }
        
        logger.info("✅ searchIntelligent() adaptive strategy validated");
    }
    
    @Test
    @Order(4)
    @DisplayName("Test 4: searchAdvanced() - Expert-level control")
    void testSearchAdvanced() throws Exception {
        logger.info("\n=== TEST 4: searchAdvanced() EXPERT CONTROL ===");
        
        // Test with different encryption configs
        EncryptionConfig[] configs = {
            EncryptionConfig.createHighSecurityConfig(),
            EncryptionConfig.createBalancedConfig(),
            EncryptionConfig.createPerformanceConfig()
        };
        
        for (EncryptionConfig config : configs) {
            SearchResult result = searchAPI.searchAdvanced("financial", testPassword, config, 10);
            logger.info("📊 searchAdvanced with " + config.getClass().getSimpleName() + ":");
            logger.info("    Success: " + result.isSuccessful());
            logger.info("    Results: " + result.getResultCount());
            logger.info("    Strategy: " + result.getStrategyUsed());
            logger.info("    Time: " + result.getTotalTimeMs() + "ms");
            
            assertTrue(result.isSuccessful(), "Advanced search should be successful");
            assertTrue(result.getResultCount() >= 0, "Should have valid result count");
            assertTrue(result.getTotalTimeMs() > 0, "Should have positive execution time");
        }
        
        logger.info("✅ searchAdvanced() expert control validated");
    }
    
    @Test
    @Order(5)
    @DisplayName("Test 5: Performance comparison between methods")
    void testPerformanceComparison() throws Exception {
        logger.info("\n=== TEST 5: PERFORMANCE COMPARISON ===");
        
        String searchTerm = "financial";
        int iterations = 5;
        
        // Test searchAll performance
        long simpleTotal = 0;
        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            searchAPI.searchAll(searchTerm);
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
        
        logger.info("📊 Performance Results (average over " + iterations + " iterations):");
        logger.info("    searchAll: " + String.format("%.2f", simpleAvg) + "ms");
        logger.info("    searchSecure: " + String.format("%.2f", secureAvg) + "ms");
        logger.info("    searchIntelligent: " + String.format("%.2f", intelligentAvg) + "ms");
        
        // All methods should complete within reasonable time
        assertTrue(simpleAvg < 1000, "searchAll should complete within 1 second");
        assertTrue(secureAvg < 1000, "searchSecure should complete within 1 second");
        assertTrue(intelligentAvg < 1000, "searchIntelligent should complete within 1 second");
        
        logger.info("✅ Performance comparison validated");
    }
    
    @Test
    @Order(6)
    @DisplayName("Test 6: Result consistency between methods")
    void testResultConsistency() throws Exception {
        logger.info("\n=== TEST 6: RESULT CONSISTENCY ===");
        
        String searchTerm = "financial";
        
        // Get results from all methods
        List<EnhancedSearchResult> simpleResults = searchAPI.searchAll(searchTerm);
        List<EnhancedSearchResult> secureResults = searchAPI.searchSecure(searchTerm, testPassword);
        List<EnhancedSearchResult> intelligentResults = searchAPI.searchIntelligent(searchTerm, testPassword, 20);
        
        logger.info("📊 Result counts:");
        logger.info("    searchAll: " + simpleResults.size());
        logger.info("    searchSecure: " + secureResults.size());
        logger.info("    searchIntelligent: " + intelligentResults.size());
        
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
        
        logger.info("✅ Result consistency validated");
    }
    
    @Test
    @Order(7)
    @DisplayName("Test 7: API state management")
    void testAPIStateManagement() throws Exception {
        logger.info("\n=== TEST 7: API STATE MANAGEMENT ===");
        
        // Test isReady()
        assertTrue(searchAPI.isReady(), "API should be ready after initialization");
        
        // Test statistics
        SearchStats stats = searchAPI.getStatistics();
        logger.info("📊 Search statistics:");
        logger.info("    Total blocks indexed: " + stats.getTotalBlocksIndexed());
        logger.info("    Memory usage: " + stats.getEstimatedMemoryBytes() + " bytes");
        
        assertTrue(stats.getTotalBlocksIndexed() > 0, "Should have indexed blocks");
        assertTrue(stats.getEstimatedMemoryBytes() > 0, "Should have memory usage");
        
        // Test password registry
        BlockPasswordRegistry.RegistryStats registryStats = searchAPI.getPasswordRegistryStats();
        logger.info("📊 Password registry:");
        logger.info("    Registered blocks: " + registryStats.getRegisteredBlocks());
        logger.info("    Memory usage: " + registryStats.getEstimatedMemoryBytes() + " bytes");
        
        assertTrue(registryStats.getRegisteredBlocks() >= 0, "Should have valid registry count");
        
        // Test registered blocks
        Set<String> registeredBlocks = searchAPI.getRegisteredBlocks();
        logger.info("📊 Registered blocks count: " + registeredBlocks.size());
        
        // Test capabilities
        String capabilities = searchAPI.getCapabilitiesSummary();
        logger.info("📊 Capabilities: " + capabilities);
        assertNotNull(capabilities, "Should have capabilities summary");
        
        logger.info("✅ API state management validated");
    }
    
    @Test
    @Order(8)
    @DisplayName("Test 8: Constructor validation - proper initialization required")
    void testConstructorComparison() throws Exception {
        logger.info("\n=== TEST 8: CONSTRUCTOR VALIDATION ===");
        
        // Test that constructor requires proper initialization
        SearchSpecialistAPI api = new SearchSpecialistAPI(blockchain, testPassword, userKeys.getPrivate());
        assertTrue(api.isReady(), "API should be ready immediately after proper construction");
        
        // Test search with properly constructed API (should find results)
        List<EnhancedSearchResult> results = api.searchAll("financial");
        logger.info("📊 Constructor search results: " + results.size());
        assertTrue(results.size() > 0, "Properly constructed API should find results immediately");
        
        logger.info("✅ Constructor validation passed");
    }
    
    @Test
    @Order(9)
    @DisplayName("Test 9: Error handling and edge cases")
    void testErrorHandling() throws Exception {
        logger.info("\n=== TEST 9: ERROR HANDLING ===");
        
        // Test null/empty queries
        assertThrows(IllegalArgumentException.class, () -> {
            searchAPI.searchAll(null);
        }, "Should throw exception for null query");
        
        assertThrows(IllegalArgumentException.class, () -> {
            searchAPI.searchAll("");
        }, "Should throw exception for empty query");
        
        // Test invalid result limits
        assertThrows(IllegalArgumentException.class, () -> {
            searchAPI.searchAll("test", 0);
        }, "Should throw exception for zero limit");
        
        assertThrows(IllegalArgumentException.class, () -> {
            searchAPI.searchAll("test", -1);
        }, "Should throw exception for negative limit");
        
        // Test null password in secure search
        assertThrows(IllegalArgumentException.class, () -> {
            searchAPI.searchSecure("test", null);
        }, "Should throw exception for null password");
        
        // Test empty password in secure search
        assertThrows(IllegalArgumentException.class, () -> {
            searchAPI.searchSecure("test", "");
        }, "Should throw exception for empty password");
        
        logger.info("✅ Error handling validated");
    }
    
    @Test
    @Order(10)
    @DisplayName("Test 10: Search with special characters and unicode")
    void testSpecialCharactersAndUnicode() throws Exception {
        logger.info("\n=== TEST 10: SPECIAL CHARACTERS AND UNICODE ===");
        
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
                List<EnhancedSearchResult> results = searchAPI.searchAll(query);
                logger.info("📊 Special query '" + query + "': " + results.size() + " results");
                // Should not throw exceptions, even if no results found
                assertTrue(results.size() >= 0, "Should handle special characters gracefully");
            } catch (Exception e) {
                fail("Should not throw exception for special characters: " + query + " - " + e.getMessage());
            }
        }
        
        logger.info("✅ Special characters and unicode handling validated");
    }
}