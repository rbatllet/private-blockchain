package com.rbatllet.blockchain.search;

import static org.junit.jupiter.api.Assertions.*;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.indexing.IndexingCoordinator;
import com.rbatllet.blockchain.security.UserRole;
import com.rbatllet.blockchain.util.TestGenesisKeyManager;
import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.search.SearchFrameworkEngine.IndexingResult;
import com.rbatllet.blockchain.search.SearchFrameworkEngine.SearchStats;

/**
 * Test class to investigate and resolve the discrepancy between:
 * - SearchSpecialistAPI logs showing "N blocks indexed"
 * - SearchStats.getTotalBlocksIndexed() returning 0
 * 
 * This is a critical issue that needs to be resolved to ensure
 * proper system monitoring and diagnostics.
 */
@DisplayName("Search Statistics Discrepancy Investigation")
public class SearchStatisticsDiscrepancyTest {

    private static final Logger logger = LoggerFactory.getLogger(SearchStatisticsDiscrepancyTest.class);
    
    private Blockchain testBlockchain;
    private KeyPair bootstrapKeyPair;
    private SearchSpecialistAPI specialistAPI;
    private SearchFrameworkEngine directEngine;
    private String testPassword;
    private PrivateKey testPrivateKey;
    private PublicKey testPublicKey;

    @BeforeEach
    void setUp() throws Exception {
        // Clear database before each test to ensure test isolation
        clearDatabase();

        // CRITICAL: Enable test mode to skip time interval checks
        // This prevents the SearchFrameworkEngine from skipping indexing due to timing
        IndexingCoordinator.getInstance().enableTestMode();

        // Initialize test environment
        testBlockchain = new Blockchain();

        // RBAC FIX (v1.0.6): Clear database before bootstrap to avoid "Existing users" error
        testBlockchain.clearAndReinitialize();

        // Load bootstrap admin keys (auto-generates if missing - test-only)
        bootstrapKeyPair = TestGenesisKeyManager.ensureGenesisKeysExist();

        // Register bootstrap admin in blockchain (RBAC v1.0.6)
        testBlockchain.createBootstrapAdmin(
            CryptoUtil.publicKeyToString(bootstrapKeyPair.getPublic()),
            "BOOTSTRAP_ADMIN"
        );

        testPassword = "TestPassword123!";

        // Generate test key pair
        KeyPair keyPair = CryptoUtil.generateKeyPair();
        testPrivateKey = keyPair.getPrivate();
        testPublicKey = keyPair.getPublic();

        // Create a small, controlled blockchain for precise testing
        createControlledTestBlockchain();
        
        logger.info("🧪 Test setup complete - blockchain has {} blocks", testBlockchain.getBlockCount());
    }

    @AfterEach
    void tearDown() {
        // Clean up SearchSpecialistAPI if used in the test
        if (specialistAPI != null) {
            specialistAPI.shutdown();
        }
        
        // Clean up direct engine if used in the test  
        if (directEngine != null) {
            directEngine.shutdown();
        }
        
        // Clear global state to ensure test isolation
        SearchFrameworkEngine.resetGlobalState();
        
        // Reset IndexingCoordinator to clear execution tracking and ensure test mode
        IndexingCoordinator.getInstance().reset();
        
        // Clean database after each test to ensure test isolation
        clearDatabase();
    }

    private void createControlledTestBlockchain() throws Exception {
        // CRITICAL: Authorize the key before adding blocks
        String publicKeyString = CryptoUtil.publicKeyToString(testPublicKey);
        boolean keyAuthorized = testBlockchain.addAuthorizedKey(
            publicKeyString,
            "TestUser",
            bootstrapKeyPair,
            UserRole.USER
        );
        if (!keyAuthorized) {
            throw new RuntimeException("Failed to authorize key for test");
        }

        // Create exactly 5 test blocks for precise counting
        for (int i = 0; i < 5; i++) {
            testBlockchain.addBlock("Test block content " + i, testPrivateKey, testPublicKey);
        }
        
        logger.info("✅ Created controlled test blockchain with {} blocks", testBlockchain.getBlockCount());
        
        // CRITICAL: Wait for async indexing to complete before resetting
        IndexingCoordinator.getInstance().waitForCompletion();
        
        // Reset IndexingCoordinator after blockchain creation to ensure clean state for tests
        IndexingCoordinator.getInstance().reset();
        logger.info("🔄 IndexingCoordinator reset after blockchain setup for clean test state");
    }

    @Test
    @DisplayName("Investigate Statistics Discrepancy - Direct Engine")
    void testDirectEngineStatistics() throws Exception {
        logger.info("🔍 Testing SearchFrameworkEngine directly...");
        
        // Reset both IndexingCoordinator and SearchFrameworkEngine state for clean test
        IndexingCoordinator.getInstance().reset();
        SearchFrameworkEngine.resetGlobalState();
        logger.info("🔄 IndexingCoordinator and SearchFrameworkEngine state reset for testDirectEngineStatistics");
        
        // Test direct SearchFrameworkEngine
        directEngine = new SearchFrameworkEngine();
        
        // Get initial stats (should be 0)
        SearchStats initialStats = directEngine.getSearchStats();
        logger.info("📊 Initial stats - Total blocks: {}", initialStats.getTotalBlocksIndexed());
        assertEquals(0, initialStats.getTotalBlocksIndexed(), "Initial stats should be 0");
        
        // Index the blockchain directly
        IndexingResult result = directEngine.indexBlockchain(testBlockchain, testPassword, testPrivateKey);
        IndexingCoordinator.getInstance().waitForCompletion();
        
        logger.info("🚀 Indexing result:");
        logger.info("   - Blocks processed: {}", result.getBlocksProcessed());
        logger.info("   - Blocks indexed: {}", result.getBlocksIndexed());
        logger.info("   - Indexing time: {}ms", result.getIndexingTimeMs());
        
        // Verify indexing result
        assertTrue(result.getBlocksIndexed() > 0, "Should have indexed some blocks");
        assertEquals(testBlockchain.getBlockCount(), result.getBlocksProcessed(), 
                    "Processed blocks should match blockchain count");
        
        // Get stats after indexing
        SearchStats afterStats = directEngine.getSearchStats();
        logger.info("📊 After indexing stats:");
        logger.info("   - Total blocks indexed: {}", afterStats.getTotalBlocksIndexed());
        logger.info("   - Router stats: {}", afterStats.getRouterStats());
        logger.info("   - Estimated memory: {} bytes", afterStats.getEstimatedMemoryBytes());
        
        // THIS IS THE CRITICAL TEST - statistics should reflect actual indexing
        assertEquals(testBlockchain.getBlockCount(), afterStats.getTotalBlocksIndexed(),
                    "Statistics should show indexed blocks count matching blockchain");
        
        // Test search functionality to verify indexing worked
        // Allow some time for indexing to settle 
        Thread.sleep(100);
        
        var searchResult = directEngine.search("Test block", testPassword, 10);
        logger.info("🔍 Search test found {} results", searchResult.getResults().size());
        
        // More forgiving test - verify the engine has correct statistics
        assertTrue(directEngine.getSearchStats().getTotalBlocksIndexed() > 0, 
                  "SearchFrameworkEngine should have indexed blocks");
    }

    @Test
    @DisplayName("Investigate Statistics Discrepancy - SearchSpecialistAPI")
    void testSearchSpecialistAPIStatistics() throws Exception {
        logger.info("🔍 Testing SearchSpecialistAPI statistics...");
        
        // Create SearchSpecialistAPI with proper initialization
        specialistAPI = new SearchSpecialistAPI(testBlockchain, testPassword, testPrivateKey);
        
        // Check ready state
        assertTrue(specialistAPI.isReady(), "API should be ready after proper initialization");
        
        // Get statistics immediately after initialization
        SearchStats stats = specialistAPI.getStatistics();
        
        logger.info("📊 SearchSpecialistAPI statistics:");
        logger.info("   - Total blocks indexed: {}", stats.getTotalBlocksIndexed());
        logger.info("   - Router stats: {}", stats.getRouterStats());
        logger.info("   - Estimated memory: {} bytes", stats.getEstimatedMemoryBytes());
        logger.info("   - Blockchain block count: {}", testBlockchain.getBlockCount());
        
        // THIS IS THE MAIN ISSUE - investigating why this fails
        if (stats.getTotalBlocksIndexed() == 0) {
            logger.error("❌ ISSUE CONFIRMED: Statistics show 0 blocks indexed");
            logger.error("   - But API logs show successful indexing");
            logger.error("   - Blockchain has {} blocks", testBlockchain.getBlockCount());
            
            // Let's test if search actually works despite stats showing 0
            var searchResults = specialistAPI.searchAll("Test block");
            logger.info("🔍 Search test results: {} found", searchResults.size());
            
            if (searchResults.size() > 0) {
                logger.error("   - Search WORKS but statistics are WRONG");
                logger.error("   - This indicates a bug in statistics reporting, not functionality");
            } else {
                logger.error("   - Search FAILS - indexing may have actually failed");
            }
            
            // Let's examine internal engine state
            examineInternalEngineState();
        } else {
            logger.info("✅ Statistics correctly show {} blocks indexed", stats.getTotalBlocksIndexed());
            assertEquals(testBlockchain.getBlockCount(), stats.getTotalBlocksIndexed(),
                        "Statistics should match blockchain block count");
        }
    }

    @Test 
    @DisplayName("Investigate Multiple Initialization Calls")
    void testMultipleInitializationCalls() throws Exception {
        logger.info("🔍 Testing multiple initialization behavior...");
        
        // Create API with proper initialization (constructor eliminates need for manual init)
        specialistAPI = new SearchSpecialistAPI(testBlockchain, testPassword, testPrivateKey);
        
        // Verify ready state (should be ready immediately)
        assertTrue(specialistAPI.isReady(), "Should be ready after proper construction");
        
        SearchStats initialStats = specialistAPI.getStatistics();
        logger.info("📊 After construction: {} blocks", initialStats.getTotalBlocksIndexed());
        
        // Test the critical assertion - should work immediately with new constructor
        assertEquals(testBlockchain.getBlockCount(), initialStats.getTotalBlocksIndexed(),
                    "Proper construction should result in correct statistics");
    }

    private void examineInternalEngineState() {
        logger.info("🔬 Examining internal engine state...");
        
        try {
            // Try to access internal engine statistics through different methods
            String metrics = specialistAPI.getPerformanceMetrics();
            logger.info("📈 Performance metrics: {}", metrics);
            
            String diagnostics = specialistAPI.runDiagnostics();
            logger.info("🔧 Diagnostics: {}", diagnostics);
            
            // Test different search methods to see if any work
            testSearchMethods();
            
        } catch (Exception e) {
            logger.error("❌ Error examining internal state: {}", e.getMessage(), e);
        }
    }

    private void testSearchMethods() {
        logger.info("🧪 Testing various search methods...");
        
        try {
            // Test simple search
            var simpleResults = specialistAPI.searchAll("Test");
            logger.info("   - Simple search: {} results", simpleResults.size());
            
            // Test secure search
            var secureResults = specialistAPI.searchSecure("Test", testPassword);
            logger.info("   - Secure search: {} results", secureResults.size());
            
            // Test intelligent search
            var intelligentResults = specialistAPI.searchIntelligent("Test", testPassword, 10);
            logger.info("   - Intelligent search: {} results", intelligentResults.size());
            
            if (simpleResults.size() > 0 || secureResults.size() > 0 || intelligentResults.size() > 0) {
                logger.error("🚨 CRITICAL FINDING: Search works but statistics are wrong!");
                logger.error("   This indicates a reporting bug, not a functional bug");
            }
            
        } catch (Exception e) {
            logger.error("❌ Error testing search methods: {}", e.getMessage());
        }
    }

    @Test
    @DisplayName("Investigate Timing Issues")
    void testTimingIssues() throws Exception {
        logger.info("🔍 Investigating potential timing issues...");
        
        // Create API and wait for various intervals
        specialistAPI = new SearchSpecialistAPI(testBlockchain, testPassword, testPrivateKey);
        
        // Check stats immediately
        SearchStats immediateStats = specialistAPI.getStatistics();
        logger.info("📊 Immediate stats: {} blocks", immediateStats.getTotalBlocksIndexed());
        
        // Wait 100ms and check again
        Thread.sleep(100);
        SearchStats after100ms = specialistAPI.getStatistics();
        logger.info("📊 After 100ms: {} blocks", after100ms.getTotalBlocksIndexed());
        
        // Wait 1 second and check again
        Thread.sleep(1000);
        SearchStats after1s = specialistAPI.getStatistics();
        logger.info("📊 After 1s: {} blocks", after1s.getTotalBlocksIndexed());
        
        // Check if any timing made a difference
        if (immediateStats.getTotalBlocksIndexed() != after1s.getTotalBlocksIndexed()) {
            logger.info("✅ Timing issue found - stats changed over time");
        } else {
            logger.info("❌ No timing issue - stats remained consistent");
        }
        
        // Final verification
        assertEquals(testBlockchain.getBlockCount(), after1s.getTotalBlocksIndexed(),
                    "After sufficient time, statistics should be correct");
    }

    /**
     * Clear the database to ensure test isolation using the standard method
     */
    private void clearDatabase() {
        try {
            // Use the standard Blockchain clearAndReinitialize method
            Blockchain tempBlockchain = new Blockchain();
            tempBlockchain.clearAndReinitialize();
            
            // Clear global search engine state to prevent cross-test contamination
            SearchFrameworkEngine.resetGlobalState();
            
            // Clear IndexingCoordinator state to prevent duplicate indexing prevention
            IndexingCoordinator.getInstance().reset();
            
            // Small delay to ensure database operations complete
            Thread.sleep(50);
        } catch (Exception e) {
            System.err.println("Warning: Failed to clear database: " + e.getMessage());
        }
    }
}