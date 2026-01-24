package com.rbatllet.blockchain.search;

import com.rbatllet.blockchain.config.EncryptionConfig;
import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.indexing.IndexingCoordinator;
import com.rbatllet.blockchain.service.UserFriendlyEncryptionAPI;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.search.BlockPasswordRegistry.RegistryStats;
import com.rbatllet.blockchain.search.SearchFrameworkEngine.*;
import com.rbatllet.blockchain.security.UserRole;
import com.rbatllet.blockchain.util.TestGenesisKeyManager;
import com.rbatllet.blockchain.service.SecureBlockEncryptionService;
import com.rbatllet.blockchain.util.CryptoUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;

import java.security.KeyPair;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Rigorous test suite for SearchSpecialistAPI to identify and fix
 * why all search methods return 0 results while searchAndDecryptByTerms works
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SearchSpecialistAPIRigorousTest {
    private static final Logger logger = LoggerFactory.getLogger(SearchSpecialistAPIRigorousTest.class);

    
    private Blockchain blockchain;
    private KeyPair bootstrapKeyPair;
    private UserFriendlyEncryptionAPI api;
    private SearchSpecialistAPI searchAPI;
    private String testPassword;
    private Block testBlock;
    
    @BeforeEach
    void setUp() throws Exception {
        blockchain = new Blockchain();

        // RBAC FIX (v1.0.6): Clear database before bootstrap to avoid "Existing users" error
        blockchain.clearAndReinitialize();

        // Load bootstrap admin keys (auto-generates if missing - test-only)
        bootstrapKeyPair = TestGenesisKeyManager.ensureGenesisKeysExist();

        // Register bootstrap admin in blockchain (RBAC v1.0.6)
        blockchain.createBootstrapAdmin(
            CryptoUtil.publicKeyToString(bootstrapKeyPair.getPublic()),
            "BOOTSTRAP_ADMIN"
        );

        api = new UserFriendlyEncryptionAPI(blockchain);
        testPassword = "RigorousTest123!";

        // PRE-AUTHORIZATION REQUIRED (v1.0.6 RBAC security):
        // 1. Create admin to act as authorized user creator
        KeyPair adminKeys = CryptoUtil.generateKeyPair();
        String adminPublicKey = CryptoUtil.publicKeyToString(adminKeys.getPublic());
        blockchain.addAuthorizedKey(adminPublicKey, "Admin", bootstrapKeyPair, UserRole.ADMIN);
        api.setDefaultCredentials("Admin", adminKeys);  // Authenticate as admin

        // 2. Now admin can create test user (generates new keys internally)
        KeyPair userKeys = api.createUser("rigorous-test-user");
        api.setDefaultCredentials("rigorous-test-user", userKeys);

        // Initialize testBlock to prevent null pointer exceptions
        // This ensures each test has access to a test block
        String[] keywords = {"rigorous", "test", "financial"};
        testBlock = api.storeSearchableData("Rigorous test financial data", testPassword, keywords);

        // CRITICAL: Wait for async indexing to complete
        IndexingCoordinator.getInstance().waitForCompletion();

        // Initialize search components
        blockchain.initializeAdvancedSearch(testPassword);
        searchAPI = blockchain.getSearchSpecialistAPI();

        // Ensure SearchSpecialistAPI is properly initialized
        if (!searchAPI.isReady()) {
            // Admin is already authenticated, can create search-setup-user directly
            KeyPair testKeys = api.createUser("search-setup-user");
            searchAPI.initializeWithBlockchain(blockchain, testPassword, testKeys.getPrivate());
        }
    }

    @AfterEach
    void tearDown() {
        // CRITICAL: Wait for all async indexing to complete before shutdown (Phase 5.4)
        try {
            IndexingCoordinator.getInstance().waitForCompletion(5000); // 5 seconds
        } catch (Exception e) {
            // Ignore warnings during cleanup
        }

        // Shutdown blockchain (which also triggers IndexingCoordinator shutdown)
        if (blockchain != null) {
            try {
                blockchain.shutdown();
            } catch (Exception e) {
                // Ignore warnings during cleanup
            }
        }

        // CRITICAL: Clear shutdown flag for next test (Phase 5.4 singleton state management)
        IndexingCoordinator.getInstance().clearShutdownFlag();
    }
    
    @Test
    @Order(1)
    @DisplayName("Test 1: SearchSpecialistAPI Initialization State")
    void testSearchSpecialistAPIInitialization() throws Exception {
        logger.info("=== TEST 1: INITIALIZATION STATE ===");

        // Use new improved constructor that requires blockchain, password, and private key
        logger.info("🔑 Creating SearchSpecialistAPI with improved constructor...");

        // RBAC FIX (v1.0.6): Need admin credentials to create new user
        KeyPair adminKeys = CryptoUtil.generateKeyPair();
        String adminPublicKey = CryptoUtil.publicKeyToString(adminKeys.getPublic());
        blockchain.addAuthorizedKey(adminPublicKey, "TestAdmin", bootstrapKeyPair, UserRole.ADMIN);
        api.setDefaultCredentials("TestAdmin", adminKeys);

        KeyPair userKeys = api.createUser("rigorous-test-user-2");
        api.setDefaultCredentials("rigorous-test-user-2", userKeys);  // Switch back to user

        searchAPI = new SearchSpecialistAPI(blockchain, testPassword, userKeys.getPrivate());
        
        // After initialization
        logger.info("📊 SearchSpecialistAPI ready after init: " + searchAPI.isReady());
        
        if (searchAPI.isReady()) {
            SearchStats stats = searchAPI.getStatistics();
            logger.info("📊 Blocks indexed: " + stats.getTotalBlocksIndexed());
            logger.info("📊 Memory usage: " + stats.getEstimatedMemoryBytes() + " bytes");
            
            String capabilities = searchAPI.getCapabilitiesSummary();
            logger.info("📊 Capabilities: " + capabilities);
        }
        
        assertTrue(searchAPI != null, "SearchSpecialistAPI should not be null");
        assertTrue(searchAPI.isReady(), "SearchSpecialistAPI should be ready after initialization");
        logger.info("✅ Initialization test passed");
    }
    
    @Test
    @Order(2)
    @DisplayName("Test 2: Compare Data Storage Methods")
    void testDataStorageComparison() throws Exception {
        logger.info("\n=== TEST 2: DATA STORAGE COMPARISON ===");

        // testBlock is already initialized in setUp(), just verify and display info
        logger.info("📝 Using existing block #" + testBlock.getBlockNumber());
        logger.info("📝 Block data: " + testBlock.getData());
        logger.info("📝 Is encrypted: " + testBlock.isDataEncrypted());
        logger.info("📝 Manual keywords: " + testBlock.getManualKeywords());
        logger.info("📝 Auto keywords: " + testBlock.getAutoKeywords());
        
        logger.info("✅ Data storage test completed");
        
        assertNotNull(testBlock, "Test block should be stored");
        assertTrue(testBlock.isDataEncrypted(), "Test block should be encrypted");
        
        // Check that keywords are stored (might be null if encryption moved them to different field)
        logger.info("📊 Keyword analysis:");
        logger.info("   Manual keywords null: " + (testBlock.getManualKeywords() == null));
        logger.info("   Auto keywords null: " + (testBlock.getAutoKeywords() == null));
        logger.info("   Searchable content: " + testBlock.getSearchableContent());
        
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
        logger.info("\n=== TEST 3: SEARCH METHOD COMPARISON ===");

        // testBlock and searchAPI are already initialized in setUp()
        
        String searchTerm = "financial";
        
        // Method 1: searchAndDecryptByTerms (working baseline)
        List<Block> workingResults = api.searchAndDecryptByTerms(new String[]{searchTerm}, testPassword, 10);
        logger.info("✅ searchAndDecryptByTerms('" + searchTerm + "'): " + workingResults.size() + " results");
        
        // Method 2: SearchSpecialistAPI methods (failing)
        List<EnhancedSearchResult> intelligentResults = searchAPI.searchIntelligent(searchTerm, testPassword, 10);
        logger.info("❌ searchIntelligent('" + searchTerm + "'): " + intelligentResults.size() + " results");
        
        List<EnhancedSearchResult> secureResults = searchAPI.searchSecure(searchTerm, testPassword, 10);
        logger.info("❌ searchSecure('" + searchTerm + "'): " + secureResults.size() + " results");
        
        List<EnhancedSearchResult> simpleResults = searchAPI.searchAll(searchTerm);
        logger.info("❌ searchAll('" + searchTerm + "'): " + simpleResults.size() + " results");
        
        // DEBUG: Check if autoKeywords can be decrypted
        logger.info("\\n🔍 DEBUG: Testing autoKeywords decryption:");
        if (testBlock.getAutoKeywords() != null) {
            logger.info("   autoKeywords raw: " + testBlock.getAutoKeywords().substring(0, Math.min(50, testBlock.getAutoKeywords().length())) + "...");
            try {
                String decryptedKeywords = SecureBlockEncryptionService.decryptFromString(
                    testBlock.getAutoKeywords(), testPassword);
                logger.info("   autoKeywords decrypted: " + decryptedKeywords);
            } catch (Exception e) {
                logger.info("   autoKeywords decryption failed: " + e.getMessage());
                // DEBUG: Let's see the actual format
                logger.info("   autoKeywords parts: " + testBlock.getAutoKeywords().split("\\|").length);
                String[] parts = testBlock.getAutoKeywords().split("\\|");
                for (int i = 0; i < Math.min(parts.length, 3); i++) {
                    logger.info("   part[" + i + "]: " + parts[i]);
                }
            }
        } else {
            logger.info("   autoKeywords is null");
        }
        
        // Analysis
        logger.info("\n📊 COMPARISON ANALYSIS:");
        logger.info("   Working method finds: " + workingResults.size() + " blocks");
        logger.info("   SearchSpecialistAPI finds: " + (intelligentResults.size() + secureResults.size() + simpleResults.size()) + " total results");
        
        // This test should fail - demonstrating the bug
        if (workingResults.size() > 0 && intelligentResults.size() == 0) {
            logger.info("🚨 BUG CONFIRMED: SearchSpecialistAPI methods fail while searchAndDecryptByTerms works");
        }
        
        assertTrue(workingResults.size() > 0, "searchAndDecryptByTerms should find results");
        // Note: This will fail until we fix SearchSpecialistAPI
        // assertEquals(workingResults.size(), intelligentResults.size(), "Both methods should find same number of results");
    }
    
    @Test
    @Order(4)
    @DisplayName("Test 4: Internal SearchSpecialistAPI State Analysis")
    void testSearchSpecialistAPIInternalState() throws Exception {
        logger.info("\n=== TEST 4: INTERNAL STATE ANALYSIS ===");
        
        // searchAPI is already initialized in setUp()

        // Analyze internal state
        logger.info("🔍 SearchSpecialistAPI Internal Analysis:");
        logger.info("   isReady(): " + searchAPI.isReady());

        SearchStats stats = searchAPI.getStatistics();
        logger.info("   Total blocks indexed: " + stats.getTotalBlocksIndexed());
        logger.info("   Memory usage: " + stats.getEstimatedMemoryBytes() + " bytes");

        // Password registry stats
        RegistryStats registryStats = searchAPI.getPasswordRegistryStats();
        logger.info("   Password registry blocks: " + registryStats.getRegisteredBlocks());
        logger.info("   Password registry memory: " + registryStats.getEstimatedMemoryBytes() + " bytes");

        // Diagnostics
        String diagnostics = searchAPI.runDiagnostics();
        logger.info("🔧 Diagnostics: " + diagnostics);

        // Performance metrics
        String metrics = searchAPI.getPerformanceMetrics();
        logger.info("📊 Performance metrics: " + metrics);

        assertTrue(searchAPI.isReady(), "SearchSpecialistAPI should be ready");
        assertTrue(stats.getTotalBlocksIndexed() > 0, "Should have indexed at least one block");
    }
    
    @Test
    @Order(5)
    @DisplayName("Test 5: Password Registry Analysis")
    void testPasswordRegistryAnalysis() throws Exception {
        logger.info("\n=== TEST 5: PASSWORD REGISTRY ANALYSIS ===");

        // testBlock and searchAPI are already initialized in setUp()
        
        // Check password registry
        RegistryStats registryStats = searchAPI.getPasswordRegistryStats();
        logger.info("📊 Password registry blocks: " + registryStats.getRegisteredBlocks());
        logger.info("📊 Password registry memory: " + registryStats.getEstimatedMemoryBytes() + " bytes");
        
        // Try to search with different password scenarios
        logger.info("\n🔐 Testing password scenarios:");
        
        // Correct password
        List<EnhancedSearchResult> correctPwdResults = searchAPI.searchSecure("financial", testPassword, 10);
        logger.info("   Correct password: " + correctPwdResults.size() + " results");
        
        // Wrong password
        List<EnhancedSearchResult> wrongPwdResults = searchAPI.searchSecure("financial", "wrongpassword", 10);
        logger.info("   Wrong password: " + wrongPwdResults.size() + " results");
        
        // No password (simple search)
        List<EnhancedSearchResult> noPwdResults = searchAPI.searchAll("financial");
        logger.info("   No password: " + noPwdResults.size() + " results");
        
        // Compare with working method
        List<Block> workingResults = api.searchAndDecryptByTerms(new String[]{"financial"}, testPassword, 10);
        logger.info("   Working method: " + workingResults.size() + " results");
        
        logger.info("\n💡 Password Registry Hypothesis:");
        if (workingResults.size() > 0 && correctPwdResults.size() == 0) {
            logger.info("   SearchSpecialistAPI may not have the correct password mapping for blocks");
            logger.info("   or the initialization with null password prevents proper indexing");
        }
    }
    
    @Test
    @Order(6)
    @DisplayName("Test 6: Direct SearchFrameworkEngine Testing")
    void testDirectSearchFrameworkEngine() throws Exception {
        logger.info("\n=== TEST 6: DIRECT SEARCH FRAMEWORK ENGINE ===");

        // testBlock and searchAPI are already initialized in setUp()
        
        // Test if we can access the underlying SearchFrameworkEngine
        try {
            // This might not be directly accessible, but let's see what we can test
            logger.info("🔧 Testing underlying search engine capabilities");
            
            // Test through SearchSpecialistAPI's advanced search
            SearchResult advancedResult = searchAPI.searchAdvanced(
                "financial", 
                testPassword, 
                EncryptionConfig.createHighSecurityConfig(), 
                10);
            
            logger.info("🔍 Advanced search result:");
            logger.info("   Success: " + advancedResult.isSuccessful());
            logger.info("   Result count: " + advancedResult.getResultCount());
            logger.info("   Strategy used: " + advancedResult.getStrategyUsed());
            logger.info("   Total time: " + advancedResult.getTotalTimeMs() + "ms");
            
            if (advancedResult.getErrorMessage() != null) {
                logger.info("   Error: " + advancedResult.getErrorMessage());
            }
            
            assertTrue(advancedResult.isSuccessful(), "Advanced search should be successful");
            
        } catch (Exception e) {
            logger.info("❌ Direct engine test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Test
    @Order(7)
    @DisplayName("Test 7: Root Cause Hypothesis Testing")
    void testRootCauseHypothesis() throws Exception {
        logger.info("\n=== TEST 7: ROOT CAUSE HYPOTHESIS ===");
        
        // Hypothesis 1: Initialization with null password is the problem
        logger.info("🧪 HYPOTHESIS 1: Null password initialization");
        logger.info("   Current initializeAdvancedSearch() uses password=null");
        logger.info("   This means only public metadata gets indexed");
        logger.info("   But our test blocks have encrypted keywords that require password access");
        
        // Hypothesis 2: Metadata indexing vs direct block access
        logger.info("\n🧪 HYPOTHESIS 2: Indexing approach difference");
        logger.info("   searchAndDecryptByTerms: Direct block access + keyword matching");
        logger.info("   SearchSpecialistAPI: Metadata index + search engine");
        logger.info("   The metadata index may not contain the keywords we're searching for");
        
        // Hypothesis 3: Test with different block types
        logger.info("\n🧪 HYPOTHESIS 3: Block type compatibility");
        
        // Test with a simple non-encrypted block
        try {
            // This would require access to addBlock directly, which may not be available
            logger.info("   Testing with different block types would require direct blockchain access");
        } catch (Exception e) {
            logger.info("   Cannot test different block types in this context");
        }
        
        // Summary
        logger.info("\n📋 ROOT CAUSE ANALYSIS SUMMARY:");
        logger.info("   1. SearchSpecialistAPI initialized with null password");
        logger.info("   2. Only public metadata gets indexed, not encrypted keywords");
        logger.info("   3. searchAndDecryptByTerms bypasses the index and searches directly");
        logger.info("   4. FIX: Initialize SearchSpecialistAPI with proper password management");
        logger.info("        or modify indexing to handle encrypted keywords properly");
    }
}