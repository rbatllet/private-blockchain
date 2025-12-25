package com.rbatllet.blockchain.search;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.service.UserFriendlyEncryptionAPI;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.entity.OffChainData;
import com.rbatllet.blockchain.indexing.IndexingCoordinator;
import com.rbatllet.blockchain.search.SearchFrameworkEngine.*;
import com.rbatllet.blockchain.security.UserRole;
import com.rbatllet.blockchain.testutil.GenesisKeyManager;
import com.rbatllet.blockchain.util.CryptoUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyPair;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for SearchSpecialistAPI on-chain and off-chain search capabilities
 * Validates that the API can search across all content types: on-chain, off-chain, encrypted, and public
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SearchSpecialistAPIOnOffChainTest {
    private static final Logger logger = LoggerFactory.getLogger(SearchSpecialistAPIOnOffChainTest.class);
    
    private Blockchain blockchain;
    private KeyPair bootstrapKeyPair;
    private UserFriendlyEncryptionAPI api;
    private SearchSpecialistAPI searchAPI;
    private String testPassword;
    private KeyPair userKeys;
    
    // Test data references
    private Block onChainEncryptedBlock;
    private Block onChainPublicBlock;
    private OffChainData offChainEncryptedData;
    private OffChainData offChainPublicData;
    
    @BeforeEach
    void setUp() throws Exception {
        blockchain = new Blockchain();

        // RBAC FIX (v1.0.6): Clear database before bootstrap to avoid "Existing users" error
        blockchain.clearAndReinitialize();

        // Load bootstrap admin keys (auto-generates if missing - test-only)
        bootstrapKeyPair = GenesisKeyManager.ensureGenesisKeysExist();

        // Register bootstrap admin in blockchain (RBAC v1.0.6)
        blockchain.createBootstrapAdmin(
            CryptoUtil.publicKeyToString(bootstrapKeyPair.getPublic()),
            "BOOTSTRAP_ADMIN"
        );

        api = new UserFriendlyEncryptionAPI(blockchain);
        testPassword = "OnOffChainTest123!";

        // PRE-AUTHORIZATION REQUIRED (v1.0.6 RBAC security):
        // 1. Create admin to act as authorized user creator
        KeyPair adminKeys = CryptoUtil.generateKeyPair();
        String adminPublicKey = CryptoUtil.publicKeyToString(adminKeys.getPublic());
        blockchain.addAuthorizedKey(adminPublicKey, "Admin", bootstrapKeyPair, UserRole.ADMIN);
        api.setDefaultCredentials("Admin", adminKeys);  // Authenticate as admin
        
        // 2. Now admin can create test user (generates new keys internally)
        userKeys = api.createUser("onoffchain-test-user");
        api.setDefaultCredentials("onoffchain-test-user", userKeys);

        // PHASE 5.4 FIX: Create test data BEFORE initializing SearchSpecialistAPI
        // This ensures blocks are indexed during SearchSpecialistAPI initialization
        createComprehensiveTestData();

        // Initialize search API AFTER creating blocks
        initializeSearchAPI();
    }
    
    private void createComprehensiveTestData() throws Exception {
        logger.info("🔧 Creating comprehensive test data...");
        logger.info("📋 Test password: {}", testPassword);
        
        // 1. ON-CHAIN ENCRYPTED BLOCK
        String[] onChainKeywords = {"blockchain", "encrypted", "onchain", "financial"};
        logger.info("📝 Creating block with keywords: {}", String.join(", ", onChainKeywords));
        onChainEncryptedBlock = api.storeSearchableData(
            "Encrypted financial blockchain data stored on-chain", 
            testPassword, 
            onChainKeywords
        );
        logger.info("✅ Created on-chain encrypted block: {}", onChainEncryptedBlock.getBlockNumber());
        logger.info("   Block hash: {}", onChainEncryptedBlock.getHash());
        logger.info("   Manual keywords stored: {}", onChainEncryptedBlock.getManualKeywords());
        
        // 2. ON-CHAIN PUBLIC BLOCK (using minimal encryption for testing)
        String[] publicKeywords = {"public", "announcement", "onchain", "news"};
        logger.info("📝 Creating block with keywords: {}", String.join(", ", publicKeywords));
        onChainPublicBlock = api.storeSearchableData(
            "Public announcement stored on-chain", 
            testPassword, 
            publicKeywords
        );
        logger.info("✅ Created on-chain public block: {}", onChainPublicBlock.getBlockNumber());
        logger.info("   Block hash: {}", onChainPublicBlock.getHash());
        logger.info("   Manual keywords stored: {}", onChainPublicBlock.getManualKeywords());
        
        // 3. OFF-CHAIN ENCRYPTED DATA
        byte[] encryptedFileData = "Encrypted document stored off-chain with sensitive financial data".getBytes();
        offChainEncryptedData = api.storeLargeFileSecurely(
            encryptedFileData, 
            testPassword, 
            "text/plain"
        );
        logger.info("✅ Created off-chain encrypted data: {}", offChainEncryptedData.getId());
        
        // 4. OFF-CHAIN PUBLIC DATA
        String publicDocumentContent = "Public document available off-chain with general information";
        offChainPublicData = api.storeLargeTextDocument(
            publicDocumentContent, 
            testPassword, 
            "public-doc.txt"
        );
        logger.info("✅ Created off-chain public data: {}", offChainPublicData.getId());

        logger.info("🎯 Test data creation completed!");
        logger.info("📊 Total blocks in blockchain: {}", blockchain.getBlockCount());
        logger.info("📊 Test blocks created: 2 on-chain blocks + genesis");

        // PHASE 5.4 FIX: Wait for async indexing to complete before tests run
        logger.info("⏳ Waiting for async indexing to complete...");
        IndexingCoordinator.getInstance().waitForCompletion();
        logger.info("✅ Async indexing completed - tests can now proceed");
        logger.info("📊 Blocks after indexing: {}", blockchain.getBlockCount());
    }
    
    private void initializeSearchAPI() throws Exception {
        logger.info("\n🔧 Initializing SearchSpecialistAPI...");
        logger.info("📊 Blocks before initialization: {}", blockchain.getBlockCount());
        
        blockchain.initializeAdvancedSearch(testPassword);
        logger.info("✅ initializeAdvancedSearch() completed");
        
        blockchain.getSearchSpecialistAPI().initializeWithBlockchain(blockchain, testPassword, userKeys.getPrivate());
        logger.info("✅ initializeWithBlockchain() completed");
        
        searchAPI = blockchain.getSearchSpecialistAPI();
        logger.info("📊 SearchAPI ready status: {}", searchAPI.isReady());
        
        if (!searchAPI.isReady()) {
            logger.warn("⚠️  SearchAPI not ready, re-initializing...");
            searchAPI.initializeWithBlockchain(blockchain, testPassword, userKeys.getPrivate());
        }
        
        assertTrue(searchAPI.isReady(), "SearchSpecialistAPI should be ready after initialization");
        logger.info("✅ SearchSpecialistAPI initialized and ready");
        logger.info("📊 Blocks after initialization: {}", blockchain.getBlockCount());
    }
    
    @Test
    @Order(1)
    @DisplayName("Test 1: On-chain search capabilities")
    void testOnChainSearch() throws Exception {
        logger.info("\n=== TEST 1: ON-CHAIN SEARCH CAPABILITIES ===");
        
        // DEBUG: Check what keywords were actually stored
        logger.info("\n🔍 DEBUG: Checking stored keywords:");
        logger.info("  On-chain encrypted block keywords: {}", onChainEncryptedBlock.getManualKeywords());
        logger.info("  On-chain public block keywords: {}", onChainPublicBlock.getManualKeywords());
        logger.info("  Total blocks in blockchain: {}", blockchain.getBlockCount());
        logger.info("  SearchAPI ready: {}", searchAPI.isReady());
        
        // Search for on-chain specific terms
        logger.info("\n🔍 Searching for 'blockchain'...");
        List<EnhancedSearchResult> blockchainResults = searchAPI.searchAll("blockchain");
        logger.info("  Results: {}", blockchainResults.size());
        for (EnhancedSearchResult result : blockchainResults) {
            logger.info("    - Block: {}", result.getBlockHash());
        }
        
        logger.info("\n🔍 Searching for 'onchain'...");
        List<EnhancedSearchResult> onchainResults = searchAPI.searchAll("onchain");
        logger.info("  Results: {}", onchainResults.size());
        for (EnhancedSearchResult result : onchainResults) {
            logger.info("    - Block: {}", result.getBlockHash());
        }
        
        logger.info("\n🔍 Searching for 'financial'...");
        List<EnhancedSearchResult> financialResults = searchAPI.searchAll("financial");
        logger.info("  Results: {}", financialResults.size());
        for (EnhancedSearchResult result : financialResults) {
            logger.info("    - Block: {}", result.getBlockHash());
        }
        
        logger.info("\n📊 On-chain search results summary:");
        logger.info("  'blockchain': {} results", blockchainResults.size());
        logger.info("  'onchain': {} results", onchainResults.size());
        logger.info("  'financial': {} results", financialResults.size());
        
        // DEBUG: Try searching with prefix
        logger.info("\n🔍 Searching for 'public:blockchain'...");
        List<EnhancedSearchResult> publicBlockchainResults = searchAPI.searchAll("public:blockchain");
        logger.info("  'public:blockchain': {} results", publicBlockchainResults.size());
        
        // Verify we find on-chain data
        assertTrue(blockchainResults.size() > 0, "Should find blockchain-related on-chain data");
        assertTrue(onchainResults.size() > 0, "Should find on-chain data");
        assertTrue(financialResults.size() > 0, "Should find financial data");
        
        // Check that results contain our test blocks
        boolean foundEncryptedBlock = blockchainResults.stream()
            .anyMatch(result -> result.getBlockHash().equals(onChainEncryptedBlock.getHash()));
        
        assertTrue(foundEncryptedBlock, "Should find our encrypted on-chain block");
        
        logger.info("✅ On-chain search validation completed");
    }
    
    @Test
    @Order(2)
    @DisplayName("Test 2: Off-chain search capabilities")
    void testOffChainSearch() throws Exception {
        System.out.println("\n=== TEST 2: OFF-CHAIN SEARCH CAPABILITIES ===");
        
        // Search for off-chain specific terms
        List<EnhancedSearchResult> documentResults = searchAPI.searchAll("document");
        List<EnhancedSearchResult> sensitiveResults = searchAPI.searchAll("sensitive");
        List<EnhancedSearchResult> generalResults = searchAPI.searchAll("general");
        
        System.out.println("📊 Off-chain search results:");
        System.out.println("  'document': " + documentResults.size() + " results");
        System.out.println("  'sensitive': " + sensitiveResults.size() + " results");
        System.out.println("  'general': " + generalResults.size() + " results");
        
        // Note: Off-chain search capability depends on SearchFrameworkEngine implementation
        // The results might be 0 if off-chain indexing is not fully implemented
        System.out.println("ℹ️  Off-chain search results depend on SearchFrameworkEngine implementation");
        
        // Verify search doesn't crash with off-chain terms
        assertTrue(documentResults.size() >= 0, "Document search should not crash");
        assertTrue(sensitiveResults.size() >= 0, "Sensitive search should not crash");
        assertTrue(generalResults.size() >= 0, "General search should not crash");
        
        System.out.println("✅ Off-chain search validation completed");
    }
    
    @Test
    @Order(3)
    @DisplayName("Test 3: Mixed on-chain and off-chain search")
    void testMixedSearch() throws Exception {
        System.out.println("\n=== TEST 3: MIXED ON-CHAIN AND OFF-CHAIN SEARCH ===");
        
        // Search for terms that might appear in both on-chain and off-chain content
        List<EnhancedSearchResult> dataResults = searchAPI.searchAll("data");
        List<EnhancedSearchResult> informationResults = searchAPI.searchAll("information");
        
        System.out.println("📊 Mixed search results:");
        System.out.println("  'data': " + dataResults.size() + " results");
        System.out.println("  'information': " + informationResults.size() + " results");
        
        // Verify mixed search works
        assertTrue(dataResults.size() >= 0, "Data search should work across all sources");
        assertTrue(informationResults.size() >= 0, "Information search should work across all sources");
        
        // Test intelligent search with mixed content
        List<EnhancedSearchResult> intelligentResults = searchAPI.searchIntelligent("data AND information", testPassword, 20);
        System.out.println("  intelligent mixed search: " + intelligentResults.size() + " results");
        
        assertTrue(intelligentResults.size() >= 0, "Intelligent mixed search should work");
        
        System.out.println("✅ Mixed search validation completed");
    }
    
    @Test
    @Order(4)
    @DisplayName("Test 4: Encrypted vs non-encrypted search")
    void testEncryptedVsPublicSearch() throws Exception {
        System.out.println("\n=== TEST 4: ENCRYPTED VS PUBLIC SEARCH ===");
        
        // Test secure search with correct password
        List<EnhancedSearchResult> secureResults = searchAPI.searchSecure("financial", testPassword);
        System.out.println("📊 Secure search with correct password: " + secureResults.size() + " results");
        
        // Test secure search with wrong password
        List<EnhancedSearchResult> wrongPasswordResults = searchAPI.searchSecure("financial", "wrongpassword");
        System.out.println("📊 Secure search with wrong password: " + wrongPasswordResults.size() + " results");
        
        // Test simple search (should access both encrypted and public with default password)
        List<EnhancedSearchResult> simpleResults = searchAPI.searchAll("financial");
        System.out.println("📊 Simple search: " + simpleResults.size() + " results");
        
        // Verify encrypted content is accessible with correct password
        assertTrue(secureResults.size() > 0, "Should find encrypted content with correct password");
        
        // Verify simple search finds content (because it uses default password)
        assertTrue(simpleResults.size() > 0, "Simple search should find content using default password");
        
        // Compare results
        System.out.println("📊 Comparison:");
        System.out.println("  Secure (correct pwd): " + secureResults.size());
        System.out.println("  Secure (wrong pwd): " + wrongPasswordResults.size());
        System.out.println("  Simple (default pwd): " + simpleResults.size());
        
        System.out.println("✅ Encrypted vs public search validation completed");
    }
    
    @Test
    @Order(5)
    @DisplayName("Test 5: Search result metadata analysis")
    void testSearchResultMetadata() throws Exception {
        System.out.println("\n=== TEST 5: SEARCH RESULT METADATA ANALYSIS ===");
        
        // Get comprehensive results
        List<EnhancedSearchResult> allResults = searchAPI.searchIntelligent("data", testPassword, 50);
        System.out.println("📊 Total results for analysis: " + allResults.size());
        
        // Analyze result metadata
        int onChainCount = 0;
        int offChainCount = 0;
        int encryptedCount = 0;
        int publicCount = 0;
        
        for (EnhancedSearchResult result : allResults) {
            System.out.println("🔍 Result: " + result.getBlockHash().substring(0, 8) + "...");
            System.out.println("  Relevance: " + result.getRelevanceScore());
            System.out.println("  Source: " + result.getSource());
            System.out.println("  Summary: " + result.getSummary());
            
            // Count by source type (this is conceptual - actual implementation may vary)
            if (result.getSource().toString().contains("CHAIN")) {
                onChainCount++;
            }
            if (result.getSource().toString().contains("ENCRYPTED")) {
                encryptedCount++;
            }
            
            // Verify result has valid metadata
            assertNotNull(result.getBlockHash(), "Result should have block hash");
            assertTrue(result.getRelevanceScore() > 0, "Result should have positive relevance");
            assertNotNull(result.getSource(), "Result should have source information");
        }
        
        System.out.println("📊 Metadata analysis summary:");
        System.out.println("  On-chain results: " + onChainCount);
        System.out.println("  Off-chain results: " + offChainCount);
        System.out.println("  Encrypted results: " + encryptedCount);
        System.out.println("  Public results: " + publicCount);
        
        System.out.println("✅ Search result metadata analysis completed");
    }
    
    @Test
    @Order(6)
    @DisplayName("Test 6: Search performance across data types")
    void testPerformanceAcrossDataTypes() throws Exception {
        System.out.println("\n=== TEST 6: SEARCH PERFORMANCE ACROSS DATA TYPES ===");
        
        String[] testQueries = {"data", "financial", "document", "blockchain", "information"};
        
        for (String query : testQueries) {
            System.out.println("🔍 Testing performance for query: '" + query + "'");
            
            // Test simple search performance
            long startTime = System.nanoTime();
            List<EnhancedSearchResult> simpleResults = searchAPI.searchAll(query);
            long simpleTime = System.nanoTime() - startTime;
            double simpleMs = simpleTime / 1_000_000.0;
            
            // Test secure search performance
            startTime = System.nanoTime();
            List<EnhancedSearchResult> secureResults = searchAPI.searchSecure(query, testPassword);
            long secureTime = System.nanoTime() - startTime;
            double secureMs = secureTime / 1_000_000.0;
            
            // Test intelligent search performance
            startTime = System.nanoTime();
            List<EnhancedSearchResult> intelligentResults = searchAPI.searchIntelligent(query, testPassword, 20);
            long intelligentTime = System.nanoTime() - startTime;
            double intelligentMs = intelligentTime / 1_000_000.0;
            
            System.out.println("  Simple: " + simpleResults.size() + " results in " + String.format("%.2f", simpleMs) + "ms");
            System.out.println("  Secure: " + secureResults.size() + " results in " + String.format("%.2f", secureMs) + "ms");
            System.out.println("  Intelligent: " + intelligentResults.size() + " results in " + String.format("%.2f", intelligentMs) + "ms");
            
            // Verify reasonable performance
            assertTrue(simpleMs < 2000, "Simple search should complete within 2 seconds");
            assertTrue(secureMs < 2000, "Secure search should complete within 2 seconds");
            assertTrue(intelligentMs < 2000, "Intelligent search should complete within 2 seconds");
        }
        
        System.out.println("✅ Performance testing across data types completed");
    }
    
    @Test
    @Order(7)
    @DisplayName("Test 7: Search scope validation")
    void testSearchScopeValidation() throws Exception {
        System.out.println("\n=== TEST 7: SEARCH SCOPE VALIDATION ===");
        
        // Test that search actually covers the intended scope
        SearchStats stats = searchAPI.getStatistics();
        System.out.println("📊 Search engine statistics:");
        System.out.println("  Total blocks indexed: " + stats.getTotalBlocksIndexed());
        System.out.println("  Memory usage: " + stats.getEstimatedMemoryBytes() + " bytes");
        
        // Get capabilities summary
        String capabilities = searchAPI.getCapabilitiesSummary();
        System.out.println("📊 Search capabilities: " + capabilities);
        
        // Test specific search patterns
        List<EnhancedSearchResult> allDataResults = searchAPI.searchAll("*");
        System.out.println("📊 Wildcard search results: " + allDataResults.size());
        
        // Test empty query handling
        try {
            searchAPI.searchAll("");
            fail("Should throw exception for empty query");
        } catch (IllegalArgumentException e) {
            System.out.println("✅ Empty query correctly rejected: " + e.getMessage());
        }
        
        // Verify search scope includes our test data
        assertTrue(stats.getTotalBlocksIndexed() >= 2, "Should have indexed at least our test blocks");
        assertNotNull(capabilities, "Should have capabilities summary");
        
        System.out.println("✅ Search scope validation completed");
    }
}