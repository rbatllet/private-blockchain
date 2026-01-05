package com.rbatllet.blockchain.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.rbatllet.blockchain.config.EncryptionConfig;
import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.indexing.IndexingCoordinator;
import com.rbatllet.blockchain.search.SearchFrameworkEngine.IndexingResult;
import com.rbatllet.blockchain.util.TestDatabaseUtils;
import com.rbatllet.blockchain.util.TestGenesisKeyManager;
import com.rbatllet.blockchain.util.CryptoUtil;

/**
 * Test suite for synchronous blockchain indexing (indexBlockchainSync)
 * 
 * Verifies that:
 * - indexBlockchainSync() blocks until completion
 * - Returns accurate block counts (not 0)
 * - Works correctly in test environment
 * - Is simpler than async + waitForCompletion pattern
 */
@DisplayName("🔄 SearchFrameworkEngine Synchronous Indexing Tests")
public class SearchFrameworkSyncIndexingTest {
    
    private SearchFrameworkEngine searchEngine;
    private Blockchain testBlockchain;
    private KeyPair bootstrapKeyPair;
    private String testPassword;
    private PrivateKey testPrivateKey;
    private PublicKey testPublicKey;
    
    @BeforeEach
    void setUp() throws Exception {
        // Clean database
        TestDatabaseUtils.setupTest();
        
        // Load bootstrap admin keys (auto-generates if missing - test-only)
        bootstrapKeyPair = TestGenesisKeyManager.ensureGenesisKeysExist();
        
        // Create blockchain
        testBlockchain = new Blockchain();
        testBlockchain.clearAndReinitialize();
        testBlockchain.createBootstrapAdmin(
            CryptoUtil.publicKeyToString(bootstrapKeyPair.getPublic()),
            "BOOTSTRAP_ADMIN"
        );
        
        // Generate test key pair and password
        testPassword = "TestPassword123!";
        KeyPair keyPair = CryptoUtil.generateKeyPair();
        testPrivateKey = keyPair.getPrivate();
        testPublicKey = keyPair.getPublic();
        
        // SECURITY FIX: Authorize test key before adding blocks
        String publicKeyString = CryptoUtil.publicKeyToString(testPublicKey);
        testBlockchain.addAuthorizedKey(
            publicKeyString, 
            "testuser", 
            bootstrapKeyPair, 
            com.rbatllet.blockchain.security.UserRole.USER
        );
        
        // Create search engine
        EncryptionConfig config = EncryptionConfig.createHighSecurityConfig();
        searchEngine = new SearchFrameworkEngine(config);
        
        // Add test blocks WITH KEYWORDS to make them searchable
        // Using addBlockWithKeywords to ensure search functionality works
        testBlockchain.addBlockWithKeywords(
            "Public data 1",
            new String[]{"test", "data", "block1"},
            "test",
            testPrivateKey,
            testPublicKey
        );
        testBlockchain.addBlockWithKeywords(
            "Public data 2",
            new String[]{"test", "data", "block2"},
            "test",
            testPrivateKey,
            testPublicKey
        );
        testBlockchain.addBlockWithKeywords(
            "Public data 3",
            new String[]{"test", "data", "block3"},
            "test",
            testPrivateKey,
            testPublicKey
        );
        
        // Wait for auto-indexing to complete
        try {
            IndexingCoordinator.getInstance().waitForCompletion();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Clear global state before manual indexing
        SearchFrameworkEngine.resetGlobalState();
    }
    
    @AfterEach
    void tearDown() {
        if (searchEngine != null) {
            searchEngine.shutdown();
        }
    }
    
    @Test
    @DisplayName("Should return accurate block count with indexBlockchainSync")
    void shouldReturnAccurateBlockCount() {
        // When
        IndexingResult result = searchEngine.indexBlockchainSync(
            testBlockchain, 
            testPassword, 
            testPrivateKey
        );
        
        // Then
        assertNotNull(result, "Result should not be null");
        assertTrue(result.getBlocksIndexed() > 0, 
            "Should have indexed blocks (not 0)");
        assertEquals(result.getBlocksProcessed(), result.getBlocksIndexed(),
            "Processed and indexed counts should match");
        
        System.out.printf("✅ Synchronous indexing: %d blocks in %.2fms%n",
            result.getBlocksIndexed(), result.getIndexingTimeMs());
    }
    
    @Test
    @DisplayName("Should index all blockchain blocks synchronously")
    void shouldIndexAllBlocks() {
        // Given
        long expectedBlocks = testBlockchain.getBlockCount();
        
        // When
        IndexingResult result = searchEngine.indexBlockchainSync(
            testBlockchain, 
            testPassword, 
            testPrivateKey
        );
        
        // Then
        assertEquals(expectedBlocks, result.getBlocksIndexed(),
            "Should index all blocks in blockchain");
    }
    
    @Test
    @DisplayName("Should be searchable immediately after indexBlockchainSync")
    void shouldBeSearchableImmediately() {
        // Given - Index synchronously
        System.out.println("🔍 Starting synchronous indexing...");
        IndexingResult indexResult = searchEngine.indexBlockchainSync(
            testBlockchain, 
            testPassword, 
            testPrivateKey
        );
        
        System.out.printf("📊 Indexing result: %d blocks indexed%n", indexResult.getBlocksIndexed());
        assertTrue(indexResult.getBlocksIndexed() > 0, "Should have indexed blocks");
        
        // When - Search immediately (no waitForCompletion needed!)
        System.out.println("🔎 Performing search...");
        SearchFrameworkEngine.SearchResult searchResult = 
            searchEngine.searchPublicOnly("test", 10);
        
        System.out.printf("📊 Search result: %d results found%n", searchResult.getResultCount());
        
        // Then
        assertNotNull(searchResult, "Search result should not be null");
        assertTrue(searchResult.getResultCount() > 0, 
            "Should find results immediately after sync indexing");
        
        System.out.printf("✅ Found %d results immediately after sync indexing%n",
            searchResult.getResultCount());
    }
    
    @Test
    @DisplayName("Should complete indexing before returning")
    void shouldCompleteBeforeReturning() {
        // When
        long startTime = System.currentTimeMillis();
        IndexingResult result = searchEngine.indexBlockchainSync(
            testBlockchain, 
            testPassword, 
            testPrivateKey
        );
        long endTime = System.currentTimeMillis();
        
        // Then
        assertTrue(result.getBlocksIndexed() > 0, "Should have completed indexing");
        
        // System.currentTimeMillis() has 1ms resolution, System.nanoTime() has nanosecond resolution
        // Allow 2ms tolerance for clock precision differences
        long actualDuration = endTime - startTime;
        double reportedDuration = result.getIndexingTimeMs();
        long tolerance = 2; // milliseconds
        
        assertTrue(actualDuration + tolerance >= reportedDuration,
            String.format("Method should block until indexing completes (actual: %dms, reported: %.2fms, tolerance: %dms)",
                actualDuration, reportedDuration, tolerance));
        
        // Verify immediately searchable (proves indexing is complete)
        SearchFrameworkEngine.SearchResult searchResult = 
            searchEngine.searchPublicOnly("data", 5);
        assertTrue(searchResult.getResultCount() > 0,
            "Should be searchable immediately (indexing completed)");
    }
    
    @Test
    @DisplayName("Should provide accurate timing information")
    void shouldProvideAccurateTiming() {
        // When
        IndexingResult result = searchEngine.indexBlockchainSync(
            testBlockchain, 
            testPassword, 
            testPrivateKey
        );
        
        // Then
        assertTrue(result.getIndexingTimeMs() > 0, 
            "Should report positive indexing time");
        assertTrue(result.getIndexingTimeMs() < 60000, 
            "Should complete in reasonable time (< 60s)");
    }
    
    @Test
    @DisplayName("Comparison: Sync is simpler than Async + waitForCompletion")
    void comparisonWithAsyncPattern() throws Exception {
        // ASYNC PATTERN (old way - 3 steps)
        Blockchain blockchain1 = createTestBlockchain();
        SearchFrameworkEngine engine1 = new SearchFrameworkEngine(
            EncryptionConfig.createHighSecurityConfig()
        );
        
        engine1.indexBlockchain(blockchain1, testPassword, testPrivateKey); // Step 1
        IndexingCoordinator.getInstance().waitForCompletion(); // Step 2
        SearchFrameworkEngine.SearchResult asyncResult = 
            engine1.searchPublicOnly("data", 10); // Step 3
        
        // SYNC PATTERN (new way - 2 steps)
        Blockchain blockchain2 = createTestBlockchain();
        SearchFrameworkEngine engine2 = new SearchFrameworkEngine(
            EncryptionConfig.createHighSecurityConfig()
        );
        
        engine2.indexBlockchainSync(blockchain2, testPassword, testPrivateKey); // Step 1
        SearchFrameworkEngine.SearchResult syncResult = 
            engine2.searchPublicOnly("data", 10); // Step 2
        
        // Both should work
        assertEquals(asyncResult.getResultCount(), syncResult.getResultCount(),
            "Both patterns should produce same results");
        
        System.out.println("✅ Sync pattern is simpler: 2 steps vs 3 steps");
        
        engine1.shutdown();
        engine2.shutdown();
    }
    
    private Blockchain createTestBlockchain() throws Exception {
        Blockchain bc = new Blockchain();
        bc.clearAndReinitialize();
        bc.createBootstrapAdmin(
            CryptoUtil.publicKeyToString(bootstrapKeyPair.getPublic()),
            "BOOTSTRAP_ADMIN"
        );
        
        // Authorize test key for this blockchain too
        String publicKeyString = CryptoUtil.publicKeyToString(testPublicKey);
        bc.addAuthorizedKey(
            publicKeyString, 
            "testuser", 
            bootstrapKeyPair, 
            com.rbatllet.blockchain.security.UserRole.USER
        );
        
        bc.addBlock("Test data 1", testPrivateKey, testPublicKey);
        bc.addBlock("Test data 2", testPrivateKey, testPublicKey);
        bc.addBlock("Test data 3", testPrivateKey, testPublicKey);
        return bc;
    }
}
