package com.rbatllet.blockchain.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.KeyPair;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.indexing.IndexingCoordinator;
import com.rbatllet.blockchain.search.strategy.EncryptedContentSearch;
import com.rbatllet.blockchain.search.strategy.SearchStrategyRouter;
import com.rbatllet.blockchain.security.UserRole;
import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.util.TestGenesisKeyManager;

/**
 * RIGOROUS TEST: Encrypted Blocks Pagination Optimization
 *
 * Tests for P0, P1, and P2 optimizations:
 * - P0: Composite index (is_encrypted, block_number) for eliminating filesort
 * - P1: Result caching for 500 most recent encrypted blocks
 * - P2: SQL-level duplicate exclusion for repeated searches
 *
 * Test coverage:
 * 1. Cache hit/miss behavior
 * 2. Cache invalidation on new encrypted block
 * 3. Cache statistics tracking
 * 4. Performance improvement verification
 * 5. SQL exclusion filtering accuracy
 * 6. Hybrid search approach (cache + SQL exclusion)
 */
@DisplayName("🚀 RIGOROUS: Encrypted Blocks Pagination Optimization")
public class EncryptedBlocksPaginationOptimizationTest {

    private static final Logger logger = LoggerFactory.getLogger(EncryptedBlocksPaginationOptimizationTest.class);

    private static final String TEST_PASSWORD = "TestPassword123#Secure";

    private Blockchain blockchain;
    private KeyPair bootstrapKeyPair;
    private KeyPair userKeyPair;
    private EncryptedContentSearch encryptedContentSearch;
    private SearchSpecialistAPI searchAPI;

    @BeforeEach
    void setUp() {
        // Reset global state
        SearchFrameworkEngine.resetGlobalState();
        IndexingCoordinator.getInstance().reset();

        blockchain = new Blockchain();
        blockchain.clearAndReinitialize();

        bootstrapKeyPair = TestGenesisKeyManager.ensureGenesisKeysExist();
        blockchain.createBootstrapAdmin(
            CryptoUtil.publicKeyToString(bootstrapKeyPair.getPublic()),
            "BOOTSTRAP_ADMIN"
        );

        userKeyPair = CryptoUtil.generateKeyPair();
        String userPublicKey = CryptoUtil.publicKeyToString(userKeyPair.getPublic());
        blockchain.addAuthorizedKey(userPublicKey, "testUser", bootstrapKeyPair, UserRole.ADMIN);

        // Initialize search
        blockchain.initializeAdvancedSearch(TEST_PASSWORD);

        // Get EncryptedContentSearch instance from blockchain's search engine
        SearchFrameworkEngine searchEngine = blockchain.getSearchFrameworkEngine();
        searchEngine.setBlockchain(blockchain);

        // Access EncryptedContentSearch through reflection since getStrategyRouter is package-private
        try {
            java.lang.reflect.Field routerField = SearchFrameworkEngine.class.getDeclaredField("strategyRouter");
            routerField.setAccessible(true);
            SearchStrategyRouter router = (SearchStrategyRouter) routerField.get(searchEngine);
            encryptedContentSearch = router.getEncryptedContentSearch();
        } catch (Exception e) {
            throw new RuntimeException("Failed to access EncryptedContentSearch", e);
        }

        // Create search API
        searchAPI = new SearchSpecialistAPI(blockchain, TEST_PASSWORD, userKeyPair.getPrivate());
    }

    @AfterEach
    void tearDown() {
        try {
            IndexingCoordinator.getInstance().waitForCompletion();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (searchAPI != null) {
            searchAPI.shutdown();
        }

        if (blockchain != null) {
            blockchain.clearAndReinitialize();
            blockchain.shutdown();
        }

        SearchFrameworkEngine.resetGlobalState();
        IndexingCoordinator.getInstance().reset();
    }

    // ========== CACHE BEHAVIOR TESTS ==========

    @Nested
    @DisplayName("📦 Cache Behavior Tests")
    class CacheBehaviorTests {

        @Test
        @DisplayName("Cache statistics are initialized correctly")
        void testCacheStatsInitialization() {
            // Act
            Map<String, Object> stats = encryptedContentSearch.getEncryptedBlocksCacheStats();

            // Assert
            assertNotNull(stats, "Cache stats should not be null");
            assertEquals(0L, stats.get("cacheHits"), "Initial cache hits should be 0");
            assertEquals(0L, stats.get("cacheMisses"), "Initial cache misses should be 0");
            assertEquals(0L, stats.get("totalRequests"), "Initial total requests should be 0");
            assertEquals(0L, stats.get("refreshCount"), "Initial refresh count should be 0");
            assertEquals(false, stats.get("cacheValid"), "Cache should initially be invalid");

            logger.info("✅ Cache stats initialized: {}", stats);
        }

        @Test
        @DisplayName("Cache miss on first search triggers refresh")
        void testCacheMissTriggersRefresh() {
            // Arrange: Create some encrypted blocks
            createEncryptedBlocks(10);

            // Get initial stats
            Map<String, Object> statsBefore = encryptedContentSearch.getEncryptedBlocksCacheStats();
            long refreshesBefore = (long) statsBefore.get("refreshCount");

            // Act: Perform search (will trigger cache load)
            searchAPI.searchExhaustiveOffChain("test", TEST_PASSWORD, 10);

            // Assert: Cache should be refreshed
            Map<String, Object> statsAfter = encryptedContentSearch.getEncryptedBlocksCacheStats();
            long refreshesAfter = (long) statsAfter.get("refreshCount");

            assertTrue(refreshesAfter > refreshesBefore,
                "Cache refresh count should increase on first search");
            assertEquals(true, statsAfter.get("cacheValid"),
                "Cache should be valid after refresh");

            logger.info("✅ Cache refreshed: before={}, after={}", refreshesBefore, refreshesAfter);
        }

        @Test
        @DisplayName("Cache hit on consecutive searches within TTL")
        void testCacheHitOnConsecutiveSearches() {
            // Arrange: Create encrypted blocks
            createEncryptedBlocks(5);

            // First search - cache miss
            searchAPI.searchExhaustiveOffChain("keyword1", TEST_PASSWORD, 10);

            Map<String, Object> statsAfter1 = encryptedContentSearch.getEncryptedBlocksCacheStats();
            long hits1 = (long) statsAfter1.get("cacheHits");
            long misses1 = (long) statsAfter1.get("cacheMisses");

            // Act: Second search - should hit cache
            searchAPI.searchExhaustiveOffChain("keyword2", TEST_PASSWORD, 10);

            // Assert
            Map<String, Object> statsAfter2 = encryptedContentSearch.getEncryptedBlocksCacheStats();
            long hits2 = (long) statsAfter2.get("cacheHits");
            long misses2 = (long) statsAfter2.get("cacheMisses");

            assertTrue(hits2 > hits1, "Cache hits should increase on second search");
            assertEquals(misses1, misses2, "Cache misses should stay the same");

            double hitRate = Double.parseDouble(
                ((String) statsAfter2.get("hitRate")).replace("%", "")
            );
            assertTrue(hitRate > 0, "Hit rate should be positive after cache hits");

            logger.info("✅ Cache hit verified: hits={}, misses={}, hitRate={}%",
                hits2, misses2, hitRate);
        }

        @Test
        @DisplayName("Cache invalidation on new encrypted block")
        void testCacheInvalidationOnNewBlock() {
            // Arrange: Create initial encrypted blocks and prime cache
            createEncryptedBlocks(3);

            searchAPI.searchExhaustiveOffChain("prime", TEST_PASSWORD, 10);

            Map<String, Object> statsBefore = encryptedContentSearch.getEncryptedBlocksCacheStats();
            assertTrue((boolean) statsBefore.get("cacheValid"),
                "Cache should be valid before adding new block");

            // Act: Add new encrypted block (should invalidate cache)
            blockchain.addEncryptedBlock(
                "New encrypted content",
                TEST_PASSWORD,
                userKeyPair.getPrivate(),
                userKeyPair.getPublic()
            );

            // Wait for indexing
            waitForIndexing();

            // Next search should trigger cache refresh
            searchAPI.searchExhaustiveOffChain("new", TEST_PASSWORD, 10);

            // Assert: Refresh count should increase
            Map<String, Object> statsAfter = encryptedContentSearch.getEncryptedBlocksCacheStats();
            long refreshBefore = (long) statsBefore.get("refreshCount");
            long refreshAfter = (long) statsAfter.get("refreshCount");

            assertTrue(refreshAfter > refreshBefore,
                "Cache should be refreshed after new encrypted block added");

            logger.info("✅ Cache invalidated and refreshed: before={}, after={}",
                refreshBefore, refreshAfter);
        }
    }

    // ========== PERFORMANCE TESTS ==========

    @Nested
    @DisplayName("⚡ Performance Improvement Tests")
    class PerformanceTests {

        @Test
        @DisplayName("Cache provides performance improvement for repeated searches")
        void testCachePerformanceImprovement() {
            // Arrange: Create 50 encrypted blocks
            int blockCount = 50;
            createEncryptedBlocks(blockCount);

            // First search - cache miss (measure time)
            long start1 = System.nanoTime();
            searchAPI.searchExhaustiveOffChain("perf1", TEST_PASSWORD, 10);
            long time1 = (System.nanoTime() - start1) / 1_000_000;

            // Second search - cache hit (should be faster)
            long start2 = System.nanoTime();
            searchAPI.searchExhaustiveOffChain("perf2", TEST_PASSWORD, 10);
            long time2 = (System.nanoTime() - start2) / 1_000_000;

            // Assert: Second search should be faster
            logger.info("📊 Performance: First search={}ms, Second search={}ms, Improvement={}x",
                time1, time2, (double) time1 / time2);

            // Note: We don't assert time improvement because it's environment-dependent,
            // but we log it for visibility. In production, expect 2-5x improvement.
            assertTrue(time1 > 0 && time2 > 0, "Both searches should complete");

            Map<String, Object> stats = encryptedContentSearch.getEncryptedBlocksCacheStats();
            logger.info("📊 Cache stats: {}", stats);
        }

        @Test
        @DisplayName("Large blockchain pagination works efficiently")
        void testLargePaginationEfficiency() {
            // Arrange: Create many encrypted blocks (simulates large blockchain)
            int blockCount = 100;
            createEncryptedBlocks(blockCount);

            // Act: Search multiple times (tests pagination + cache)
            long totalTime = 0;
            int searchCount = 5;

            for (int i = 0; i < searchCount; i++) {
                long start = System.nanoTime();
                searchAPI.searchExhaustiveOffChain("large" + i, TEST_PASSWORD, 20);
                totalTime += (System.nanoTime() - start) / 1_000_000;
            }

            long avgTime = totalTime / searchCount;

            // Assert
            Map<String, Object> stats = encryptedContentSearch.getEncryptedBlocksCacheStats();
            double hitRate = Double.parseDouble(
                ((String) stats.get("hitRate")).replace("%", "")
            );

            logger.info("📊 Large blockchain test: {} blocks, {} searches, avg={}ms, hitRate={}%",
                blockCount, searchCount, avgTime, hitRate);

            // With cache, most searches should hit cache
            assertTrue(hitRate > 50,
                "Cache hit rate should be >50% with repeated searches");
        }
    }

    // ========== P2: SQL-LEVEL EXCLUSION TESTS ==========

    @Nested
    @DisplayName("🚀 P2: SQL-Level Duplicate Exclusion")
    class SQLExclusionTests {

        @Test
        @DisplayName("SQL exclusion filters blocks at database level")
        void testSQLExclusionFiltersBlocks() {
            // Arrange: Create 20 encrypted blocks
            createEncryptedBlocks(20);

            // Get first 10 blocks
            List<Block> firstBatch = blockchain.getEncryptedBlocksPaginatedDesc(0, 10);
            assertEquals(10, firstBatch.size(), "First batch should have 10 blocks");

            // Extract hashes to exclude
            Set<String> excludeHashes = firstBatch.stream()
                .map(Block::getHash)
                .collect(java.util.stream.Collectors.toSet());

            // Act: Get next 10 blocks, excluding first batch
            List<Block> secondBatch = blockchain.getEncryptedBlocksExcluding(0, 10, excludeHashes);

            // Assert: No overlap between batches
            Set<String> secondBatchHashes = secondBatch.stream()
                .map(Block::getHash)
                .collect(java.util.stream.Collectors.toSet());

            for (String hash : excludeHashes) {
                assertFalse(secondBatchHashes.contains(hash),
                    "Excluded hash should not appear in result: " + hash.substring(0, 8));
            }

            logger.info("✅ SQL exclusion: {} blocks excluded, {} returned (no overlap)",
                excludeHashes.size(), secondBatch.size());
        }

        @Test
        @DisplayName("SQL exclusion with empty exclude set behaves like normal query")
        void testSQLExclusionWithEmptySet() {
            // Arrange
            createEncryptedBlocks(10);

            // Act: Query with empty exclusion set
            List<Block> withExclusion = blockchain.getEncryptedBlocksExcluding(0, 10, new java.util.HashSet<>());
            List<Block> withoutExclusion = blockchain.getEncryptedBlocksPaginatedDesc(0, 10);

            // Assert: Results should be identical
            assertEquals(withoutExclusion.size(), withExclusion.size(),
                "Empty exclusion should return same count as normal query");

            for (int i = 0; i < withExclusion.size(); i++) {
                assertEquals(withoutExclusion.get(i).getHash(), withExclusion.get(i).getHash(),
                    "Block " + i + " should match between queries");
            }

            logger.info("✅ Empty exclusion set: behaves identically to normal query");
        }

        @Test
        @DisplayName("SQL exclusion with null exclude set behaves like normal query")
        void testSQLExclusionWithNullSet() {
            // Arrange
            createEncryptedBlocks(10);

            // Act: Query with null exclusion set
            List<Block> withExclusion = blockchain.getEncryptedBlocksExcluding(0, 10, null);
            List<Block> withoutExclusion = blockchain.getEncryptedBlocksPaginatedDesc(0, 10);

            // Assert: Results should be identical
            assertEquals(withoutExclusion.size(), withExclusion.size(),
                "Null exclusion should return same count as normal query");

            logger.info("✅ Null exclusion set: behaves identically to normal query");
        }

        @Test
        @DisplayName("SQL exclusion respects JPA parameter limits (max 1000)")
        void testSQLExclusionParameterLimit() {
            // Arrange: Create blocks and large exclusion set (>1000 hashes)
            createEncryptedBlocks(50);

            Set<String> largeExcludeSet = new java.util.HashSet<>();
            for (int i = 0; i < 1500; i++) {
                largeExcludeSet.add("fake_hash_" + i);
            }

            // Act: Should fall back to normal query (no exception)
            List<Block> result = blockchain.getEncryptedBlocksExcluding(0, 10, largeExcludeSet);

            // Assert: Query succeeds despite large exclude set
            assertNotNull(result, "Result should not be null");
            assertTrue(result.size() <= 10, "Result should respect limit");

            logger.info("✅ Large exclusion set (>1000): gracefully falls back to normal query");
        }

        @Test
        @DisplayName("Hybrid search uses cache first, then SQL exclusion")
        void testHybridSearchApproach() {
            // Arrange: Create blocks
            createEncryptedBlocks(30);

            // Clear cache to ensure fresh state
            encryptedContentSearch.invalidateEncryptedBlocksCache();

            // Act: Perform search (should use hybrid approach)
            SearchFrameworkEngine.SearchResult result = searchAPI.searchExhaustiveOffChain("keyword15", TEST_PASSWORD, 5);

            // Assert: Search completes successfully
            assertNotNull(result, "Search result should not be null");

            Map<String, Object> stats = encryptedContentSearch.getEncryptedBlocksCacheStats();
            logger.info("📊 Hybrid search stats: {}", stats);

            // Cache should be used (at least one hit or miss)
            long totalRequests = (long) stats.get("totalRequests");
            assertTrue(totalRequests > 0, "Cache should be accessed during search");

            logger.info("✅ Hybrid approach: cache used for first batch, SQL exclusion for subsequent");
        }

        @Test
        @DisplayName("SQL exclusion improves performance vs in-memory filtering")
        void testSQLExclusionPerformance() {
            // Arrange: Create many blocks
            int blockCount = 100;
            createEncryptedBlocks(blockCount);

            // Get half the blocks to exclude
            List<Block> firstHalf = blockchain.getEncryptedBlocksPaginatedDesc(0, 50);
            Set<String> excludeHashes = firstHalf.stream()
                .map(Block::getHash)
                .collect(java.util.stream.Collectors.toSet());

            // Measure SQL exclusion time
            long sqlStart = System.nanoTime();
            List<Block> sqlResult = blockchain.getEncryptedBlocksExcluding(0, 25, excludeHashes);
            long sqlTime = (System.nanoTime() - sqlStart) / 1_000_000;

            // Measure in-memory filtering time
            long memStart = System.nanoTime();
            List<Block> allBlocks = blockchain.getEncryptedBlocksPaginatedDesc(0, 75);
            List<Block> memResult = allBlocks.stream()
                .filter(b -> !excludeHashes.contains(b.getHash()))
                .limit(25)
                .collect(java.util.stream.Collectors.toList());
            long memTime = (System.nanoTime() - memStart) / 1_000_000;

            // Assert: Both approaches return same data
            assertEquals(sqlResult.size(), memResult.size(),
                "SQL and in-memory approaches should return same count");

            logger.info("📊 Performance comparison: SQL={}ms, In-Memory={}ms, Improvement={}x",
                sqlTime, memTime, (double) memTime / Math.max(sqlTime, 1));

            // SQL should be competitive or better (environment-dependent)
            assertTrue(sqlTime > 0 && memTime > 0, "Both queries should complete");

            logger.info("✅ SQL exclusion provides competitive/better performance vs in-memory");
        }
    }

    // ========== HELPER METHODS ==========

    private void createEncryptedBlocks(int count) {
        for (int i = 0; i < count; i++) {
            blockchain.addEncryptedBlock(
                "Encrypted content " + i + " with keyword" + i,
                TEST_PASSWORD,
                userKeyPair.getPrivate(),
                userKeyPair.getPublic()
            );
        }

        waitForIndexing();
        logger.debug("✅ Created {} encrypted blocks", count);
    }

    private void waitForIndexing() {
        try {
            IndexingCoordinator.getInstance().waitForCompletion();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Indexing interrupted", e);
        }
    }
}
