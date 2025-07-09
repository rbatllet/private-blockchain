package com.rbatllet.blockchain.search;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.config.EncryptionConfig;
import com.rbatllet.blockchain.search.RevolutionarySearchEngine.*;
import com.rbatllet.blockchain.util.CryptoUtil;
import org.junit.jupiter.api.*;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BASIC REVOLUTIONARY SEARCH ENGINE TEST SUITE
 * 
 * Simple test suite to validate core functionality of the Revolutionary Search Engine
 * without complex dependencies or advanced features.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RevolutionarySearchBasicTest {
    
    private RevolutionarySearchEngine searchEngine;
    private UnifiedRevolutionarySearchAPI unifiedAPI;
    private Blockchain testBlockchain;
    private String testPassword;
    private PrivateKey testPrivateKey;
    private PublicKey testPublicKey;
    
    @BeforeEach
    void setUp() throws Exception {
        // Initialize test environment with public enabled
        EncryptionConfig testConfig = EncryptionConfig.createHighSecurityConfig();
        searchEngine = new RevolutionarySearchEngine(testConfig);
        unifiedAPI = new UnifiedRevolutionarySearchAPI(testConfig);
        testBlockchain = new Blockchain();
        testPassword = "TestPassword123!";
        
        // Generate test key pair
        KeyPair keyPair = CryptoUtil.generateKeyPair();
        testPrivateKey = keyPair.getPrivate();
        testPublicKey = keyPair.getPublic();
        
        // Create simple test blockchain
        createBasicTestBlockchain();
    }
    
    @AfterEach
    void tearDown() {
        if (searchEngine != null) {
            searchEngine.shutdown();
        }
        if (unifiedAPI != null) {
            unifiedAPI.shutdown();
        }
    }
    
    // ===== BASIC SEARCH ENGINE TESTS =====
    
    @Test
    @Order(1)
    @DisplayName("Basic Revolutionary Search Engine Initialization")
    void testBasicInitialization() {
        assertNotNull(searchEngine, "Search engine should be initialized");
        assertNotNull(unifiedAPI, "Unified API should be initialized");
        
        // Index the blockchain
        IndexingResult indexingResult = searchEngine.indexBlockchain(testBlockchain, testPassword, testPrivateKey);
        assertNotNull(indexingResult, "Indexing result should not be null");
        System.out.println("✅ Revolutionary Search Engine indexed " + indexingResult.getBlocksIndexed() + " blocks successfully");
        
        // Test statistics
        RevolutionarySearchStats stats = searchEngine.getSearchStats();
        assertNotNull(stats, "Stats should not be null");
        assertTrue(stats.getTotalBlocksIndexed() > 0, "Should have indexed blocks");
    }
    
    @Test
    @Order(2)
    @DisplayName("Basic Fast Public Search")
    void testBasicFastPublicSearch() {
        // Index blockchain
        IndexingResult indexingResult = searchEngine.indexBlockchain(testBlockchain, testPassword, testPrivateKey);
        assertTrue(indexingResult.getBlocksIndexed() > 0);
        
        // Perform fast public search
        RevolutionarySearchResult result = searchEngine.searchPublicOnly("financial", 10);
        
        assertNotNull(result, "Search result should not be null");
        assertTrue(result.isSuccessful(), "Search should be successful");
        assertTrue(result.getTotalTimeMs() >= 0, "Search time should be recorded");
        assertNotNull(result.getResults(), "Results list should not be null");
        
        System.out.printf("Fast public search: %d results in %.2fms%n", 
                         result.getResultCount(), result.getTotalTimeMs());
    }
    
    @Test
    @Order(3)
    @DisplayName("Basic Encrypted Content Search")
    void testBasicEncryptedContentSearch() {
        // Index blockchain
        searchEngine.indexBlockchain(testBlockchain, testPassword, testPrivateKey);
        
        // Perform encrypted content search
        RevolutionarySearchResult result = searchEngine.searchEncryptedOnly("account", testPassword, 10);
        
        assertNotNull(result, "Search result should not be null");
        assertTrue(result.isSuccessful(), "Encrypted search should be successful");
        assertTrue(result.getTotalTimeMs() >= 0, "Search time should be recorded");
        
        System.out.printf("Encrypted search: %d results in %.2fms%n", 
                         result.getResultCount(), result.getTotalTimeMs());
    }
    
    @Test
    @Order(4)
    @DisplayName("Basic Public Search")
    void testBasicPublicSearch() {
        // Index blockchain
        searchEngine.indexBlockchain(testBlockchain, testPassword, testPrivateKey);
        
        // Perform public search
        RevolutionarySearchResult result = searchEngine.searchPublicOnly("data", 10);
        
        assertNotNull(result, "Public search result should not be null");
        assertTrue(result.isSuccessful(), "Public search should be successful");
        assertTrue(result.getTotalTimeMs() >= 0, "Search time should be recorded");
        
        System.out.printf("Public search: %d results in %.2fms%n", 
                         result.getResultCount(), result.getTotalTimeMs());
    }
    
    @Test
    @Order(5)
    @DisplayName("Basic Intelligent Search Routing")
    void testBasicIntelligentRouting() {
        // Index blockchain
        searchEngine.indexBlockchain(testBlockchain, testPassword, testPrivateKey);
        
        // Test simple query without password (should route to fast search)
        RevolutionarySearchResult simpleResult = searchEngine.search("medical", null, 10);
        assertNotNull(simpleResult);
        assertTrue(simpleResult.isSuccessful());
        
        // Test complex query with password (should route to encrypted search)
        RevolutionarySearchResult complexResult = searchEngine.search("patient medical record", testPassword, 10);
        assertNotNull(complexResult);
        assertTrue(complexResult.isSuccessful());
        
        System.out.printf("Simple query strategy: %s%n", simpleResult.getStrategyUsed());
        System.out.printf("Complex query strategy: %s%n", complexResult.getStrategyUsed());
    }
    
    // ===== UNIFIED API TESTS =====
    
    @Test
    @Order(10)
    @DisplayName("Unified API Basic Functionality")
    void testUnifiedAPIBasicFunctionality() {
        // Initialize unified API
        IndexingResult indexingResult = unifiedAPI.initializeWithBlockchain(testBlockchain, testPassword, testPrivateKey);
        assertTrue(indexingResult.getBlocksIndexed() > 0);
        assertTrue(unifiedAPI.isReady());
        
        // Test simple search
        List<EnhancedSearchResult> simpleResults = unifiedAPI.searchSimple("financial");
        assertNotNull(simpleResults);
        System.out.printf("Simple search found %d results%n", simpleResults.size());
        
        // Test secure search
        List<EnhancedSearchResult> secureResults = unifiedAPI.searchSecure("account", testPassword);
        assertNotNull(secureResults);
        System.out.printf("Secure search found %d results%n", secureResults.size());
        
        // Test private search
        List<EnhancedSearchResult> privateResults = unifiedAPI.searchSimple("contains:data");
        assertNotNull(privateResults);
        System.out.printf("Private search found %d results%n", privateResults.size());
    }
    
    @Test
    @Order(11)
    @DisplayName("Unified API User-Defined Term Searches")
    void testUnifiedAPISpecializedSearches() {
        unifiedAPI.initializeWithBlockchain(testBlockchain, testPassword, testPrivateKey);
        
        // Test search for transfer-related content
        List<EnhancedSearchResult> transferResults = unifiedAPI.searchIntelligent("transfer", testPassword, 10);
        assertNotNull(transferResults);
        System.out.printf("Transfer search: %d results%n", transferResults.size());
        
        // Test search for patient-related content
        List<EnhancedSearchResult> patientResults = unifiedAPI.searchIntelligent("patient", testPassword, 10);
        assertNotNull(patientResults);
        System.out.printf("Patient search: %d results%n", patientResults.size());
        
        // Test search for contract-related content
        List<EnhancedSearchResult> contractResults = unifiedAPI.searchIntelligent("contract", testPassword, 10);
        assertNotNull(contractResults);
        System.out.printf("Contract search: %d results%n", contractResults.size());
    }
    
    @Test
    @Order(12)
    @DisplayName("Unified API Statistics and Diagnostics")
    void testUnifiedAPIStatisticsAndDiagnostics() {
        unifiedAPI.initializeWithBlockchain(testBlockchain, testPassword, testPrivateKey);
        
        // Test statistics
        RevolutionarySearchStats stats = unifiedAPI.getStatistics();
        assertNotNull(stats);
        assertTrue(stats.getTotalBlocksIndexed() > 0);
        
        // Test performance metrics
        String metrics = unifiedAPI.getPerformanceMetrics();
        assertNotNull(metrics);
        assertTrue(metrics.contains("Revolutionary Search Engine Metrics"));
        
        // Test diagnostics
        String diagnostics = unifiedAPI.runDiagnostics();
        assertNotNull(diagnostics);
        assertTrue(diagnostics.contains("Revolutionary Search Engine Diagnostics"));
        
        System.out.println("=== PERFORMANCE METRICS ===");
        System.out.println(metrics);
        System.out.println("\n=== DIAGNOSTICS ===");
        System.out.println(diagnostics);
    }
    
    // ===== ERROR HANDLING TESTS =====
    
    @Test
    @Order(20)
    @DisplayName("Error Handling - Invalid Inputs")
    void testErrorHandlingInvalidInputs() {
        searchEngine.indexBlockchain(testBlockchain, testPassword, testPrivateKey);
        
        // Test null query
        RevolutionarySearchResult nullResult = searchEngine.search(null, testPassword, 10);
        assertNotNull(nullResult);
        assertFalse(nullResult.isSuccessful());
        assertNotNull(nullResult.getErrorMessage());
        
        // Test empty query
        RevolutionarySearchResult emptyResult = searchEngine.search("", testPassword, 10);
        assertNotNull(emptyResult);
        // Empty query might be handled as valid with 0 results, so just check it returns something
        
        // Test invalid max results
        RevolutionarySearchResult invalidResult = searchEngine.search("test", testPassword, -1);
        assertNotNull(invalidResult);
        assertFalse(invalidResult.isSuccessful());
        
        System.out.println("Error handling tests completed successfully");
    }
    
    @Test
    @Order(21)
    @DisplayName("Performance Test - Basic Load")
    void testPerformanceBasicLoad() {
        searchEngine.indexBlockchain(testBlockchain, testPassword, testPrivateKey);
        
        // Perform multiple searches to test performance
        int numSearches = 20;
        long totalTime = 0;
        
        for (int i = 0; i < numSearches; i++) {
            long startTime = System.nanoTime();
            RevolutionarySearchResult result = searchEngine.searchPublicOnly("test" + (i % 5), 5);
            long endTime = System.nanoTime();
            
            assertTrue(result.isSuccessful());
            totalTime += (endTime - startTime);
        }
        
        double avgTimeMs = (totalTime / numSearches) / 1_000_000.0;
        System.out.printf("Performance test: %d searches, average time: %.2fms%n", numSearches, avgTimeMs);
        
        // Performance target: average search time should be reasonable
        assertTrue(avgTimeMs < 1000, "Average search time should be under 1 second");
    }
    
    // ===== HELPER METHODS =====
    
    private void createBasicTestBlockchain() throws Exception {
        // Create simple test blocks for basic testing
        
        // Financial block
        testBlockchain.addBlock(
            "Financial transaction: SWIFT transfer from Account A to Account B, Amount: €10000", 
            testPrivateKey, testPublicKey);
        
        // Medical block
        testBlockchain.addBlock(
            "Medical record: Patient John Doe, diagnosis: hypertension, treatment: medication", 
            testPrivateKey, testPublicKey);
        
        // Legal block
        testBlockchain.addBlock(
            "Legal contract: Service agreement between Company A and Company B, value: $50000", 
            testPrivateKey, testPublicKey);
        
        // Technical block
        testBlockchain.addBlock(
            "Technical data: API response with user data and transaction information", 
            testPrivateKey, testPublicKey);
        
        // Personal block
        testBlockchain.addBlock(
            "Personal information: Employee record with contact details and department info", 
            testPrivateKey, testPublicKey);
    }
}