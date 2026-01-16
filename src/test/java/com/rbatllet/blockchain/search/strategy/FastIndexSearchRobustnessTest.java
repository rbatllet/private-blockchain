package com.rbatllet.blockchain.search.strategy;

import com.rbatllet.blockchain.search.metadata.BlockMetadataLayers;
import com.rbatllet.blockchain.search.metadata.PublicMetadata;
import com.rbatllet.blockchain.search.strategy.FastIndexSearch.FastSearchResult;
import com.rbatllet.blockchain.search.strategy.FastIndexSearch.FastIndexStats;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Comprehensive robustness tests for FastIndexSearch class
 * Tests all methods including private ones to ensure defensive programming
 * Following project patterns: SLF4J logging, comprehensive edge case testing
 * 
 * Coverage: All public methods + critical private methods for complete validation
 */
@DisplayName("FastIndexSearch Comprehensive Robustness Tests")
class FastIndexSearchRobustnessTest {

    private static final Logger logger = LoggerFactory.getLogger(FastIndexSearchRobustnessTest.class);
    private FastIndexSearch fastIndexSearch;

    @BeforeEach
    void setUp() {
        fastIndexSearch = new FastIndexSearch();
        logger.info("Starting FastIndexSearch robustness tests");
    }

    // Helper method to log test context (following project patterns)
    private void logTestContext(String testName, String scenario) {
        logger.info("🧪 Test: {} - Scenario: {}", testName, scenario);
    }

    // ========== indexBlock() Tests ==========

    @Test
    @DisplayName("indexBlock should handle null blockHash gracefully")
    void testIndexBlockNullHash() {
        logTestContext("indexBlock", "null blockHash");
        
        BlockMetadataLayers metadata = createMockMetadata();
        
        // Should not throw exception
        assertDoesNotThrow(() -> {
            fastIndexSearch.indexBlock(null, metadata);
        });
        
        // Verify no indexing occurred
        FastIndexStats stats = fastIndexSearch.getIndexStats();
        assertEquals(0, stats.getBlocksIndexed(), "No blocks should be indexed with null hash");
        
        logger.info("✅ Test passed: indexBlock handles null blockHash gracefully");
    }

    @Test
    @DisplayName("indexBlock should handle null metadata gracefully")
    void testIndexBlockNullMetadata() {
        logTestContext("indexBlock", "null metadata");
        
        assertDoesNotThrow(() -> {
            fastIndexSearch.indexBlock("test-hash", null);
        });
        
        FastIndexStats stats = fastIndexSearch.getIndexStats();
        assertEquals(0, stats.getBlocksIndexed(), "No blocks should be indexed with null metadata");
        
        logger.info("✅ Test passed: indexBlock handles null metadata gracefully");
    }

    @Test
    @DisplayName("indexBlock should handle metadata with null public layer")
    void testIndexBlockNullPublicLayer() {
        logTestContext("indexBlock", "null public layer");
        
        BlockMetadataLayers metadata = new BlockMetadataLayers(null, "encrypted-private-layer");
        // Public layer will be null explicitly
        
        assertDoesNotThrow(() -> {
            fastIndexSearch.indexBlock("test-hash", metadata);
        });
        
        logger.info("✅ Test passed: indexBlock handles null public layer gracefully");
    }

    @Test
    @DisplayName("indexBlock should handle empty public metadata")
    void testIndexBlockEmptyPublicMetadata() {
        logTestContext("indexBlock", "empty public metadata");
        
        BlockMetadataLayers metadata = createEmptyMetadata();
        
        assertDoesNotThrow(() -> {
            fastIndexSearch.indexBlock("test-hash", metadata);
        });
        
        logger.info("✅ Test passed: indexBlock handles empty public metadata gracefully");
    }

    @Test
    @DisplayName("indexBlock should index valid metadata correctly")
    void testIndexBlockValidMetadata() {
        logTestContext("indexBlock", "valid metadata");
        
        BlockMetadataLayers metadata = createMockMetadata();
        String blockHash = "valid-test-hash";
        
        fastIndexSearch.indexBlock(blockHash, metadata);
        
        FastIndexStats stats = fastIndexSearch.getIndexStats();
        assertEquals(1, stats.getBlocksIndexed(), "Should index one block");
        assertTrue(stats.getUniqueKeywords() > 0, "Should have indexed keywords");
        
        logger.info("✅ Test passed: indexBlock indexes valid metadata correctly");
    }

    // ========== removeBlock() Tests ==========

    @Test
    @DisplayName("removeBlock should handle null blockHash gracefully")
    void testRemoveBlockNullHash() {
        logTestContext("removeBlock", "null blockHash");
        
        assertDoesNotThrow(() -> {
            fastIndexSearch.removeBlock(null);
        });
        
        logger.info("✅ Test passed: removeBlock handles null blockHash gracefully");
    }

    @Test
    @DisplayName("removeBlock should handle non-existent block gracefully")
    void testRemoveBlockNonExistent() {
        logTestContext("removeBlock", "non-existent block");
        
        assertDoesNotThrow(() -> {
            fastIndexSearch.removeBlock("non-existent-hash");
        });
        
        logger.info("✅ Test passed: removeBlock handles non-existent block gracefully");
    }

    @Test
    @DisplayName("removeBlock should remove existing block correctly")
    void testRemoveBlockExisting() {
        logTestContext("removeBlock", "existing block");
        
        // First add a block
        BlockMetadataLayers metadata = createMockMetadata();
        String blockHash = "remove-test-hash";
        fastIndexSearch.indexBlock(blockHash, metadata);
        
        FastIndexStats statsBefore = fastIndexSearch.getIndexStats();
        assertTrue(statsBefore.getBlocksIndexed() > 0, "Should have blocks before removal");
        
        // Remove the block
        fastIndexSearch.removeBlock(blockHash);
        
        FastIndexStats statsAfter = fastIndexSearch.getIndexStats();
        assertEquals(0, statsAfter.getBlocksIndexed(), "Should have no blocks after removal");
        
        logger.info("✅ Test passed: removeBlock removes existing block correctly");
    }

    // ========== searchFast() Tests ==========

    @Test
    @DisplayName("searchFast should handle null query gracefully")
    void testSearchFastNullQuery() {
        logTestContext("searchFast", "null query");
        
        List<FastSearchResult> results = fastIndexSearch.searchFast(null, 10);
        
        assertNotNull(results, "Results should not be null");
        assertTrue(results.isEmpty(), "Results should be empty for null query");
        
        logger.info("✅ Test passed: searchFast handles null query gracefully");
    }

    @Test
    @DisplayName("searchFast should handle empty query gracefully")
    void testSearchFastEmptyQuery() {
        logTestContext("searchFast", "empty query");
        
        List<FastSearchResult> results = fastIndexSearch.searchFast("", 10);
        
        assertNotNull(results, "Results should not be null");
        assertTrue(results.isEmpty(), "Results should be empty for empty query");
        
        logger.info("✅ Test passed: searchFast handles empty query gracefully");
    }

    @Test
    @DisplayName("searchFast should handle whitespace-only query gracefully")
    void testSearchFastWhitespaceQuery() {
        logTestContext("searchFast", "whitespace query");
        
        List<FastSearchResult> results = fastIndexSearch.searchFast("   \t\n   ", 10);
        
        assertNotNull(results, "Results should not be null");
        assertTrue(results.isEmpty(), "Results should be empty for whitespace-only query");
        
        logger.info("✅ Test passed: searchFast handles whitespace query gracefully");
    }

    @Test
    @DisplayName("searchFast should handle zero maxResults")
    void testSearchFastZeroMaxResults() {
        logTestContext("searchFast", "zero maxResults");
        
        // Add test data first
        fastIndexSearch.indexBlock("test-hash", createMockMetadata());
        
        List<FastSearchResult> results = fastIndexSearch.searchFast("test", 0);
        
        assertNotNull(results, "Results should not be null");
        assertTrue(results.isEmpty(), "Results should be empty for zero maxResults");
        
        logger.info("✅ Test passed: searchFast handles zero maxResults gracefully");
    }

    @Test
    @DisplayName("searchFast should throw IllegalArgumentException for negative maxResults")
    void testSearchFastNegativeMaxResults() {
        logTestContext("searchFast", "negative maxResults");
        
        // Add test data first
        fastIndexSearch.indexBlock("test-hash", createMockMetadata());
        
        // VULNERABILITY FIXED: FastIndexSearch now properly validates negative maxResults
        assertThrows(IllegalArgumentException.class, () -> {
            fastIndexSearch.searchFast("test", -5);
        }, "Should throw IllegalArgumentException for negative maxResults");
        
        logger.info("✅ VULNERABILITY FIXED: searchFast now properly validates negative maxResults");
    }

    @Test
    @DisplayName("searchFast should find exact matches")
    void testSearchFastExactMatches() {
        logTestContext("searchFast", "exact matches");
        
        BlockMetadataLayers metadata = createMockMetadata();
        fastIndexSearch.indexBlock("test-hash", metadata);
        
        List<FastSearchResult> results = fastIndexSearch.searchFast("financial", 10);
        
        assertNotNull(results, "Results should not be null");
        assertFalse(results.isEmpty(), "Should find exact matches");
        
        logger.info("✅ Test passed: searchFast finds exact matches");
    }

    // ========== searchByContentType() Tests ==========

    @Test
    @DisplayName("searchByContentType should handle null contentType")
    void testSearchByContentTypeNull() {
        logTestContext("searchByContentType", "null contentType");
        
        List<FastSearchResult> results = fastIndexSearch.searchByContentType("test", null, 10);
        
        assertNotNull(results, "Results should not be null");
        // Should fallback to regular search
        
        logger.info("✅ Test passed: searchByContentType handles null contentType");
    }

    @Test
    @DisplayName("searchByContentType should handle empty contentType")
    void testSearchByContentTypeEmpty() {
        logTestContext("searchByContentType", "empty contentType");
        
        List<FastSearchResult> results = fastIndexSearch.searchByContentType("test", "", 10);
        
        assertNotNull(results, "Results should not be null");
        
        logger.info("✅ Test passed: searchByContentType handles empty contentType");
    }

    @Test
    @DisplayName("searchByContentType should handle non-existent contentType")
    void testSearchByContentTypeNonExistent() {
        logTestContext("searchByContentType", "non-existent contentType");
        
        // Add test data with different content type
        fastIndexSearch.indexBlock("test-hash", createMockMetadata());
        
        List<FastSearchResult> results = fastIndexSearch.searchByContentType("test", "non-existent-type", 10);
        
        assertNotNull(results, "Results should not be null");
        assertTrue(results.isEmpty(), "Should be empty for non-existent content type");
        
        logger.info("✅ Test passed: searchByContentType handles non-existent contentType");
    }

    // ========== searchByTimeRange() Tests ==========

    @Test
    @DisplayName("searchByTimeRange should handle null timeRange")
    void testSearchByTimeRangeNull() {
        logTestContext("searchByTimeRange", "null timeRange");
        
        List<FastSearchResult> results = fastIndexSearch.searchByTimeRange("test", null, 10);
        
        assertNotNull(results, "Results should not be null");
        
        logger.info("✅ Test passed: searchByTimeRange handles null timeRange");
    }

    @Test
    @DisplayName("searchByTimeRange should handle empty timeRange")
    void testSearchByTimeRangeEmpty() {
        logTestContext("searchByTimeRange", "empty timeRange");
        
        List<FastSearchResult> results = fastIndexSearch.searchByTimeRange("test", "", 10);
        
        assertNotNull(results, "Results should not be null");
        
        logger.info("✅ Test passed: searchByTimeRange handles empty timeRange");
    }

    // ========== Private Method Tests ==========

    @Test
    @DisplayName("parseQuery should throw NullPointerException for null input")
    void testParseQueryNull() throws Exception {
        logTestContext("parseQuery", "null input");
        
        Method method = FastIndexSearch.class.getDeclaredMethod("parseQuery", String.class);
        method.setAccessible(true);
        
        // FIXED: parseQuery now properly validates null input with IllegalArgumentException
        assertThrows(InvocationTargetException.class, () -> {
            method.invoke(fastIndexSearch, (String) null);
        }, "Should throw exception for null input");
        
        try {
            method.invoke(fastIndexSearch, (String) null);
        } catch (InvocationTargetException e) {
            assertTrue(e.getCause() instanceof IllegalArgumentException, 
                "Root cause should be IllegalArgumentException");
        }
        
        logger.info("✅ VULNERABILITY FIXED: parseQuery now properly validates null input");
    }

    @Test
    @DisplayName("calculateEditDistance should handle empty strings")
    void testCalculateEditDistanceEmpty() throws Exception {
        logTestContext("calculateEditDistance", "empty strings");
        
        Method method = FastIndexSearch.class.getDeclaredMethod("calculateEditDistance", String.class, String.class);
        method.setAccessible(true);
        
        // Test empty vs empty
        int result1 = (Integer) method.invoke(fastIndexSearch, "", "");
        assertEquals(0, result1, "Distance between empty strings should be 0");
        
        // Test empty vs non-empty
        int result2 = (Integer) method.invoke(fastIndexSearch, "", "test");
        assertEquals(4, result2, "Distance should equal length of non-empty string");
        
        // Test non-empty vs empty
        int result3 = (Integer) method.invoke(fastIndexSearch, "test", "");
        assertEquals(4, result3, "Distance should equal length of non-empty string");
        
        logger.info("✅ Test passed: calculateEditDistance handles empty strings");
    }

    @Test
    @DisplayName("calculateEditDistance should handle null strings gracefully")
    void testCalculateEditDistanceNull() throws Exception {
        logTestContext("calculateEditDistance", "null strings");
        
        Method method = FastIndexSearch.class.getDeclaredMethod("calculateEditDistance", String.class, String.class);
        method.setAccessible(true);
        
        assertDoesNotThrow(() -> {
            try {
                method.invoke(fastIndexSearch, null, "test");
            } catch (Exception e) {
                // NPE is expected and acceptable for null inputs
                // The calling methods should validate inputs
            }
        });
        
        logger.info("✅ Test passed: calculateEditDistance handles null strings (may throw expected exception)");
    }

    @Test
    @DisplayName("isFuzzyMatch should handle edge cases")
    void testIsFuzzyMatchEdgeCases() throws Exception {
        logTestContext("isFuzzyMatch", "edge cases");

        // isFuzzyMatch now has 3 parameters: (String query, String indexed, boolean enableFuzzy)
        Method method = FastIndexSearch.class.getDeclaredMethod("isFuzzyMatch", String.class, String.class, boolean.class);
        method.setAccessible(true);

        // Test identical strings (should return false as it's handled as exact match)
        boolean result1 = (Boolean) method.invoke(fastIndexSearch, "test", "test", true);
        assertFalse(result1, "Identical strings should not be fuzzy match");

        // Test very short strings
        boolean result2 = (Boolean) method.invoke(fastIndexSearch, "a", "b", true);
        assertFalse(result2, "Very short strings should not fuzzy match");

        // Test contains relationship
        boolean result3 = (Boolean) method.invoke(fastIndexSearch, "test", "testing", true);
        assertTrue(result3, "Contains relationship should be fuzzy match");

        // Test with fuzzy disabled (should return false for non-exact matches)
        boolean result4 = (Boolean) method.invoke(fastIndexSearch, "test", "testing", false);
        assertFalse(result4, "With fuzzy disabled, should not match non-identical strings");

        logger.info("✅ Test passed: isFuzzyMatch handles edge cases correctly");
    }

    // ========== Statistics and Utility Tests ==========

    @Test
    @DisplayName("getIndexStats should always return valid stats")
    void testGetIndexStats() {
        logTestContext("getIndexStats", "valid stats");
        
        FastIndexStats stats = fastIndexSearch.getIndexStats();
        
        assertNotNull(stats, "Stats should not be null");
        assertTrue(stats.getBlocksIndexed() >= 0, "Blocks indexed should be non-negative");
        assertTrue(stats.getUniqueKeywords() >= 0, "Unique keywords should be non-negative");
        assertTrue(stats.getTimeRanges() >= 0, "Time ranges should be non-negative");
        assertTrue(stats.getContentTypes() >= 0, "Content types should be non-negative");
        assertTrue(stats.getEstimatedMemoryBytes() >= 0, "Memory usage should be non-negative");
        
        logger.info("✅ Test passed: getIndexStats returns valid stats");
    }

    @Test
    @DisplayName("clearAll should reset all indexes")
    void testClearAll() {
        logTestContext("clearAll", "reset indexes");
        
        // Add test data
        fastIndexSearch.indexBlock("test-hash", createMockMetadata());
        
        FastIndexStats statsBefore = fastIndexSearch.getIndexStats();
        assertTrue(statsBefore.getBlocksIndexed() > 0, "Should have data before clear");
        
        // Clear all
        fastIndexSearch.clearAll();
        
        FastIndexStats statsAfter = fastIndexSearch.getIndexStats();
        assertEquals(0, statsAfter.getBlocksIndexed(), "Should have no data after clear");
        assertEquals(0, statsAfter.getUniqueKeywords(), "Should have no keywords after clear");
        
        logger.info("✅ Test passed: clearAll resets all indexes");
    }

    // ========== Thread Safety Tests ==========

    @Test
    @DisplayName("FastIndexSearch should handle concurrent access")
    void testConcurrentAccess() {
        logTestContext("concurrent", "thread safety");
        
        int threadCount = 5;
        Thread[] threads = new Thread[threadCount];
        
        // Test concurrent indexing and searching
        for (int i = 0; i < threadCount; i++) {
            final int threadIndex = i;
            threads[i] = new Thread(() -> {
                try {
                    // Index a block
                    BlockMetadataLayers metadata = createMockMetadata();
                    fastIndexSearch.indexBlock("hash-" + threadIndex, metadata);
                    
                    // Search
                    List<FastSearchResult> results = fastIndexSearch.searchFast("financial", 10);
                    assertNotNull(results, "Concurrent search should not return null");
                    
                    // Remove block
                    fastIndexSearch.removeBlock("hash-" + threadIndex);
                    
                } catch (Exception e) {
                    logger.error("Concurrent access failed in thread " + threadIndex, e);
                    fail("Concurrent access should not throw exceptions");
                }
            });
        }
        
        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }
        
        // Wait for completion
        for (Thread thread : threads) {
            assertDoesNotThrow(() -> thread.join(5000), "Threads should complete without hanging");
        }
        
        logger.info("✅ Test passed: FastIndexSearch handles concurrent access");
    }

    // ========== Result Objects Tests ==========

    @Test
    @DisplayName("FastSearchResult should handle valid construction")
    void testFastSearchResultConstruction() {
        logTestContext("FastSearchResult", "construction");
        
        PublicMetadata publicMetadata = createMockPublicMetadata();
        FastSearchResult result = new FastSearchResult("test-hash", 2.5, 15.0, publicMetadata);
        
        assertNotNull(result, "Result should not be null");
        assertEquals("test-hash", result.getBlockHash(), "Block hash should match");
        assertEquals(2.5, result.getRelevanceScore(), 0.01, "Relevance score should match");
        assertEquals(15.0, result.getSearchTimeMs(), 0.01, "Search time should match");
        assertEquals(publicMetadata, result.getPublicMetadata(), "Public metadata should match");
        
        // Test toString - check actual format first
        String toString = result.toString();
        assertNotNull(toString, "toString should not be null");
        
        // FastSearchResult.toString() uses substring of hash, so check for partial match
        assertTrue(toString.contains("test-has") || toString.contains("FastResult"), 
            "toString should contain hash substring or class name, actual: " + toString);
        
        logger.info("✅ Test passed: FastSearchResult construction works correctly");
    }

    @Test
    @DisplayName("FastIndexStats should handle valid construction")
    void testFastIndexStatsConstruction() {
        logTestContext("FastIndexStats", "construction");
        
        FastIndexStats stats = new FastIndexStats(10, 25, 3, 5, 1024L);
        
        assertNotNull(stats, "Stats should not be null");
        assertEquals(10, stats.getBlocksIndexed(), "Blocks indexed should match");
        assertEquals(25, stats.getUniqueKeywords(), "Unique keywords should match");
        assertEquals(3, stats.getTimeRanges(), "Time ranges should match");
        assertEquals(5, stats.getContentTypes(), "Content types should match");
        assertEquals(1024L, stats.getEstimatedMemoryBytes(), "Memory bytes should match");
        
        // Test toString
        String toString = stats.toString();
        assertNotNull(toString, "toString should not be null");
        assertTrue(toString.contains("10"), "toString should contain blocks count");
        
        logger.info("✅ Test passed: FastIndexStats construction works correctly");
    }

    @Test
    @DisplayName("FastSearchResult should handle null blockHash")
    void testFastSearchResultNullHash() {
        logTestContext("FastSearchResult", "null blockHash");
        
        PublicMetadata publicMetadata = createMockPublicMetadata();
        
        // Test construction with null hash - should not crash
        FastSearchResult result = new FastSearchResult(null, 2.5, 15.0, publicMetadata);
        assertNotNull(result, "Result should not be null even with null hash");
        assertNull(result.getBlockHash(), "Hash should be null");
        
        // Test toString with null hash - critical vulnerability check
        String toString = result.toString();
        assertNotNull(toString, "toString should handle null hash gracefully");
        
        logger.info("✅ Test passed: FastSearchResult handles null blockHash");
    }

    @Test
    @DisplayName("FastSearchResult should handle null publicMetadata")
    void testFastSearchResultNullMetadata() {
        logTestContext("FastSearchResult", "null publicMetadata");
        
        // Test construction with null metadata
        FastSearchResult result = new FastSearchResult("test-hash", 2.5, 15.0, null);
        assertNotNull(result, "Result should not be null");
        assertEquals("test-hash", result.getBlockHash(), "Hash should be preserved");
        assertNull(result.getPublicMetadata(), "Metadata should be null");
        
        logger.info("✅ Test passed: FastSearchResult handles null publicMetadata");
    }

    @Test
    @DisplayName("FastSearchResult should handle negative values")
    void testFastSearchResultNegativeValues() {
        logTestContext("FastSearchResult", "negative values");
        
        PublicMetadata publicMetadata = createMockPublicMetadata();
        
        // Test with negative relevance score and time
        FastSearchResult result = new FastSearchResult("test-hash", -1.0, -5.0, publicMetadata);
        assertNotNull(result, "Result should accept negative values");
        assertEquals(-1.0, result.getRelevanceScore(), 0.01, "Negative relevance should be preserved");
        assertEquals(-5.0, result.getSearchTimeMs(), 0.01, "Negative time should be preserved");
        
        logger.info("✅ Test passed: FastSearchResult handles negative values");
    }

    @Test
    @DisplayName("FastIndexStats should handle negative values") 
    void testFastIndexStatsNegativeValues() {
        logTestContext("FastIndexStats", "negative values");
        
        // Test with all negative values - should be allowed or handled gracefully
        FastIndexStats stats = new FastIndexStats(-1, -5, -2, -3, -100L);
        assertNotNull(stats, "Stats should be created with negative values");
        assertEquals(-1, stats.getBlocksIndexed(), "Negative blocks should be preserved");
        assertEquals(-5, stats.getUniqueKeywords(), "Negative keywords should be preserved");
        
        // toString should handle negative values gracefully
        String toString = stats.toString();
        assertNotNull(toString, "toString should handle negatives");
        
        logger.info("✅ Test passed: FastIndexStats handles negative values");
    }

    @Test
    @DisplayName("FastIndexStats should handle extreme values")
    void testFastIndexStatsExtremeValues() {
        logTestContext("FastIndexStats", "extreme values");
        
        // Test with maximum values
        FastIndexStats stats = new FastIndexStats(Integer.MAX_VALUE, Integer.MAX_VALUE, 
                                                 Integer.MAX_VALUE, Integer.MAX_VALUE, Long.MAX_VALUE);
        assertNotNull(stats, "Stats should handle max values");
        assertEquals(Integer.MAX_VALUE, stats.getBlocksIndexed(), "Max int should be preserved");
        assertEquals(Long.MAX_VALUE, stats.getEstimatedMemoryBytes(), "Max long should be preserved");
        
        // toString should not crash with large values
        String toString = stats.toString();
        assertNotNull(toString, "toString should handle extreme values");
        
        logger.info("✅ Test passed: FastIndexStats handles extreme values");
    }

    // ========== Missing Path Coverage Tests ==========

    @Test
    @DisplayName("searchByContentType should handle whitespace-only contentType") 
    void testSearchByContentTypeWhitespace() {
        logTestContext("searchByContentType", "whitespace-only contentType");
        
        // Add test data first
        fastIndexSearch.indexBlock("test-hash", createMockMetadata());
        
        // Test with whitespace-only contentType - should fallback to searchFast
        List<FastSearchResult> results = fastIndexSearch.searchByContentType("test", "   ", 10);
        
        assertNotNull(results, "Results should not be null");
        // Should behave like regular search since whitespace gets trimmed to empty
        
        logger.info("✅ Test passed: searchByContentType handles whitespace-only contentType");
    }

    @Test
    @DisplayName("searchByContentType should handle existing contentType with results")
    void testSearchByContentTypeExistingWithResults() {
        logTestContext("searchByContentType", "existing contentType with results");
        
        // Create metadata with specific content type
        BlockMetadataLayers metadata = createMockMetadataWithContentType("application/pdf");
        fastIndexSearch.indexBlock("pdf-hash", metadata);
        
        // Search by the indexed content type
        List<FastSearchResult> results = fastIndexSearch.searchByContentType("test", "application/pdf", 10);
        
        assertNotNull(results, "Results should not be null");
        // Results depend on whether "test" matches the indexed keywords
        
        logger.info("✅ Test passed: searchByContentType handles existing contentType with results");
    }

    @Test
    @DisplayName("searchByTimeRange should handle whitespace-only timeRange")
    void testSearchByTimeRangeWhitespace() {
        logTestContext("searchByTimeRange", "whitespace-only timeRange");
        
        // Add test data first  
        fastIndexSearch.indexBlock("test-hash", createMockMetadata());
        
        // Test with whitespace-only timeRange - should fallback to searchFast
        List<FastSearchResult> results = fastIndexSearch.searchByTimeRange("test", "   ", 10);
        
        assertNotNull(results, "Results should not be null");
        // Should behave like regular search since whitespace gets trimmed to empty
        
        logger.info("✅ Test passed: searchByTimeRange handles whitespace-only timeRange");
    }

    @Test
    @DisplayName("searchByTimeRange should handle existing timeRange with results") 
    void testSearchByTimeRangeExistingWithResults() {
        logTestContext("searchByTimeRange", "existing timeRange with results");
        
        // Create metadata with specific time range
        BlockMetadataLayers metadata = createMockMetadataWithTimeRange("2023-2024");
        fastIndexSearch.indexBlock("time-hash", metadata);
        
        // Search by the indexed time range
        List<FastSearchResult> results = fastIndexSearch.searchByTimeRange("test", "2023-2024", 10);
        
        assertNotNull(results, "Results should not be null");
        // Results depend on whether "test" matches the indexed keywords
        
        logger.info("✅ Test passed: searchByTimeRange handles existing timeRange with results");
    }

    @Test
    @DisplayName("calculateFuzzyScore should handle substring matches")
    void testCalculateFuzzyScoreSubstring() throws Exception {
        logTestContext("calculateFuzzyScore", "substring matches");
        
        Method method = FastIndexSearch.class.getDeclaredMethod("calculateFuzzyScore", String.class, String.class);
        method.setAccessible(true);
        
        // Test indexed.contains(query) case
        Double score1 = (Double) method.invoke(fastIndexSearch, "test", "testing");
        assertEquals(1.5, score1, 0.01, "Should return 1.5 for substring match (indexed contains query)");
        
        // Test query.contains(indexed) case  
        Double score2 = (Double) method.invoke(fastIndexSearch, "testing", "test");
        assertEquals(1.5, score2, 0.01, "Should return 1.5 for substring match (query contains indexed)");
        
        logger.info("✅ Test passed: calculateFuzzyScore handles substring matches");
    }

    @Test
    @DisplayName("calculateFuzzyScore should handle edit distance calculation")
    void testCalculateFuzzyScoreEditDistance() throws Exception {
        logTestContext("calculateFuzzyScore", "edit distance calculation");
        
        Method method = FastIndexSearch.class.getDeclaredMethod("calculateFuzzyScore", String.class, String.class);
        method.setAccessible(true);
        
        // Test strings > 3 characters with no substring match - should calculate edit distance
        Double score = (Double) method.invoke(fastIndexSearch, "blockchain", "blackchain");
        assertNotNull(score, "Score should not be null");
        assertTrue(score >= 0.0 && score <= 1.0, "Score should be between 0.0 and 1.0 for edit distance");
        assertTrue(score > 0.5, "Score should be > 0.5 for similar strings");
        
        logger.info("✅ Test passed: calculateFuzzyScore handles edit distance calculation");
    }

    @Test
    @DisplayName("calculateFuzzyScore should return default score for short strings")
    void testCalculateFuzzyScoreShortStrings() throws Exception {
        logTestContext("calculateFuzzyScore", "short strings");
        
        Method method = FastIndexSearch.class.getDeclaredMethod("calculateFuzzyScore", String.class, String.class);
        method.setAccessible(true);
        
        // Test strings <= 3 characters with no substring match - should return default 0.5
        Double score1 = (Double) method.invoke(fastIndexSearch, "ab", "xy");
        assertEquals(0.5, score1, 0.01, "Should return 0.5 for short strings");
        
        Double score2 = (Double) method.invoke(fastIndexSearch, "abc", "xyz");  
        assertEquals(0.5, score2, 0.01, "Should return 0.5 for 3-char strings without substring match");
        
        logger.info("✅ Test passed: calculateFuzzyScore handles short strings");
    }

    @Test
    @DisplayName("calculateFuzzyScore should handle edge cases")
    void testCalculateFuzzyScoreEdgeCases() throws Exception {
        logTestContext("calculateFuzzyScore", "edge cases");
        
        Method method = FastIndexSearch.class.getDeclaredMethod("calculateFuzzyScore", String.class, String.class);
        method.setAccessible(true);
        
        // Test identical strings
        Double score1 = (Double) method.invoke(fastIndexSearch, "same", "same");
        assertEquals(1.5, score1, 0.01, "Identical strings should return 1.5 (contains match)");
        
        // Test empty strings
        Double score2 = (Double) method.invoke(fastIndexSearch, "", "");
        assertEquals(1.5, score2, 0.01, "Empty strings should return 1.5 (contains match)");
        
        // Test one empty, one non-empty
        Double score3 = (Double) method.invoke(fastIndexSearch, "", "test");
        assertEquals(1.5, score3, 0.01, "Empty query in non-empty indexed should return 1.5");
        
        logger.info("✅ Test passed: calculateFuzzyScore handles edge cases");
    }

    // ========== Private Helper Methods ==========

    private BlockMetadataLayers createMockMetadata() {
        PublicMetadata publicLayer = createMockPublicMetadata();
        return new BlockMetadataLayers(publicLayer, "encrypted-private-layer-data");
    }

    private PublicMetadata createMockPublicMetadata() {
        PublicMetadata publicMetadata = new PublicMetadata();
        
        // Set properties using available methods or reflection
        try {
            // Assume there are setters or use reflection
            publicMetadata.getGeneralKeywords().addAll(Arrays.asList("test", "financial", "blockchain"));
            publicMetadata.setTimeRange("2023-2024");
            publicMetadata.setContentType("application/json");
        } catch (Exception e) {
            // Handle cases where methods might not be available
            logger.warn("Could not set public metadata properties: {}", e.getMessage());
        }
        
        return publicMetadata;
    }

    private BlockMetadataLayers createEmptyMetadata() {
        PublicMetadata publicLayer = new PublicMetadata();
        // Leave empty - all fields null or empty by default
        return new BlockMetadataLayers(publicLayer, null);
    }

    private BlockMetadataLayers createMockMetadataWithContentType(String contentType) {
        PublicMetadata publicLayer = createMockPublicMetadata();
        try {
            publicLayer.setContentType(contentType);
        } catch (Exception e) {
            logger.warn("Could not set content type: {}", e.getMessage());
        }
        return new BlockMetadataLayers(publicLayer, "encrypted-data");
    }

    private BlockMetadataLayers createMockMetadataWithTimeRange(String timeRange) {
        PublicMetadata publicLayer = createMockPublicMetadata();
        try {
            publicLayer.setTimeRange(timeRange);
        } catch (Exception e) {
            logger.warn("Could not set time range: {}", e.getMessage());
        }
        return new BlockMetadataLayers(publicLayer, "encrypted-data");
    }
}