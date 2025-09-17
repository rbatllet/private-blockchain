package com.rbatllet.blockchain.service;

import static org.junit.jupiter.api.Assertions.*;

import com.rbatllet.blockchain.service.SearchMetrics.PerformanceSnapshot;
import com.rbatllet.blockchain.service.SearchMetrics.PerformanceStats;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Comprehensive tests for SearchMetrics.PerformanceSnapshot class
 * Tests robustness, edge cases, and all public methods
 */
public class SearchMetricsPerformanceSnapshotTest {

    private static final Logger logger = LoggerFactory.getLogger(
        SearchMetricsPerformanceSnapshotTest.class
    );

    private SearchMetrics metrics;
    private PerformanceSnapshot snapshot;

    @BeforeEach
    void setUp() {
        metrics = new SearchMetrics();
        logger.info("Test setup completed");
    }

    @Test
    @DisplayName("✅ Basic functionality: Valid snapshot creation and getters")
    void testBasicFunctionality() {
        // Add some test data
        metrics.recordSearch("KEYWORD", 150, 5, true);
        metrics.recordSearch("REGEX", 200, 3, false);
        metrics.recordSearch("SEMANTIC", 100, 8, true);

        // Get snapshot
        snapshot = metrics.getSnapshot();

        // Test basic getters
        assertEquals(3, snapshot.getTotalSearches());
        assertEquals(150.0, snapshot.getAverageDuration(), 0.1); // (150+200+100)/3
        assertEquals(66.67, snapshot.getCacheHitRate(), 0.1); // 2/3 * 100
        assertEquals(3, snapshot.getSearchesSinceStart());
        assertNotNull(snapshot.getLastSearchTime());
        
        logger.info("✅ Basic functionality test passed");
    }

    @Test
    @DisplayName("🔒 Constructor robustness: Invalid parameters handling")
    void testConstructorRobustness() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime earlier = now.minusMinutes(10);
        Map<String, PerformanceStats> validStats = new ConcurrentHashMap<>();
        
        // Test with negative values
        snapshot = new PerformanceSnapshot(
            -5, // negative searches
            -100.0, // negative duration
            -20.0, // negative cache hit rate
            -10, // negative searches since start
            now,
            validStats,
            earlier
        );

        // Verify defensive handling
        assertEquals(0, snapshot.getTotalSearches()); // Should be 0, not negative
        assertEquals(0.0, snapshot.getAverageDuration()); // Should be 0, not negative
        assertEquals(0.0, snapshot.getCacheHitRate()); // Should be 0, not negative
        assertEquals(0, snapshot.getSearchesSinceStart()); // Should be 0, not negative

        logger.info("✅ Constructor robustness test passed");
    }

    @Test
    @DisplayName("🔢 Edge cases: NaN and extreme values")
    void testNaNAndExtremeValues() {
        LocalDateTime now = LocalDateTime.now();
        
        // Test with NaN values
        snapshot = new PerformanceSnapshot(
            100,
            Double.NaN, // NaN duration
            Double.NaN, // NaN cache hit rate
            100,
            now,
            null, // null stats
            null // null start time
        );

        // Verify NaN handling - constructor sanitizes NaN to 0.0
        assertEquals(0.0, snapshot.getAverageDuration()); // NaN should become 0
        assertEquals(0.0, snapshot.getCacheHitRate()); // NaN should become 0
        assertNotNull(snapshot.getSearchTypeCounts()); // Should handle null stats
        assertTrue(snapshot.getSearchTypeCounts().isEmpty()); // Should be empty

        // Test cache hit rate boundary values
        snapshot = new PerformanceSnapshot(
            100,
            50.0,
            150.0, // > 100%
            100,
            now,
            new ConcurrentHashMap<>(),
            now.minusMinutes(5)
        );

        assertEquals(100.0, snapshot.getCacheHitRate()); // Should be clamped to 100%

        logger.info("✅ NaN and extreme values test passed");
    }    @Test
    @DisplayName("🔍 getSearchTypeCounts(): Accurate real data counting")
    void testGetSearchTypeCounts() {
        // Test empty snapshot
        snapshot = new PerformanceSnapshot(0, 0, 0, 0, null, null, null);
        Map<String, Long> counts = snapshot.getSearchTypeCounts();
        assertNotNull(counts);
        assertTrue(counts.isEmpty());

        // Add diverse search types
        metrics.recordSearch("KEYWORD", 100, 5, true);
        metrics.recordSearch("KEYWORD", 150, 3, false);
        metrics.recordSearch("REGEX", 200, 2, true);
        metrics.recordSearch("SEMANTIC", 300, 1, false);
        metrics.recordSearch("SEMANTIC", 250, 4, true);

        snapshot = metrics.getSnapshot();
        counts = snapshot.getSearchTypeCounts();

        // Verify accurate counts
        assertEquals(3, counts.size()); // 3 different types
        assertEquals(2L, counts.get("KEYWORD")); // 2 KEYWORD searches
        assertEquals(1L, counts.get("REGEX")); // 1 REGEX search
        assertEquals(2L, counts.get("SEMANTIC")); // 2 SEMANTIC searches
        
        // Verify total matches
        long totalFromCounts = counts.values().stream().mapToLong(Long::longValue).sum();
        assertEquals(5, totalFromCounts); // Should equal total searches

        logger.info("✅ getSearchTypeCounts test passed");
    }

    @Test
    @DisplayName("⏱️ getRecentSearchRate(): Time-based calculation accuracy")
    void testGetRecentSearchRate() throws InterruptedException {
        // Test empty snapshot
        snapshot = new PerformanceSnapshot(0, 0, 0, 0, null, null, null);
        assertEquals(0.0, snapshot.getRecentSearchRate());

        // Test with invalid time data
        LocalDateTime now = LocalDateTime.now();
        snapshot = new PerformanceSnapshot(5, 100, 50, 5, null, null, now);
        assertEquals(0.0, snapshot.getRecentSearchRate()); // null lastSearchTime

        snapshot = new PerformanceSnapshot(5, 100, 50, 5, now, null, null);
        assertEquals(0.0, snapshot.getRecentSearchRate()); // null startTime        // Test realistic timing scenario
        LocalDateTime start = LocalDateTime.now().minusMinutes(10); // 10 minutes ago
        LocalDateTime end = LocalDateTime.now();
        snapshot = new PerformanceSnapshot(20, 100, 50, 20, end, null, start);
        
        double rate = snapshot.getRecentSearchRate();
        assertTrue(rate > 0); // Should be positive
        assertEquals(2.0, rate, 0.5); // 20 searches in 10 minutes = 2 per minute (with tolerance)

        // Test same time scenario (no elapsed time)
        snapshot = new PerformanceSnapshot(5, 100, 50, 5, start, null, start);
        rate = snapshot.getRecentSearchRate();
        assertEquals(0.0, rate, 0.1); // Should return 0 when no time elapsed

        logger.info("✅ getRecentSearchRate test passed");
    }

    @Test
    @DisplayName("🎯 getMostActiveSearchType(): Find most frequent search type")
    void testGetMostActiveSearchType() {
        // Test empty data
        snapshot = new PerformanceSnapshot(0, 0, 0, 0, null, null, null);
        assertNull(snapshot.getMostActiveSearchType());

        // Add searches with different frequencies
        metrics.recordSearch("KEYWORD", 100, 5, true); // 1 time
        metrics.recordSearch("REGEX", 200, 3, false); // 1 time
        metrics.recordSearch("SEMANTIC", 150, 2, true); // 1 time
        metrics.recordSearch("KEYWORD", 120, 4, false); // 2nd time
        metrics.recordSearch("KEYWORD", 180, 1, true); // 3rd time

        snapshot = metrics.getSnapshot();
        String mostActive = snapshot.getMostActiveSearchType();
        assertEquals("KEYWORD", mostActive); // KEYWORD has 3 searches, others have 1

        logger.info("✅ getMostActiveSearchType test passed");
    }    @Test
    @DisplayName("⏳ getRuntimeMinutes(): Duration calculation accuracy")
    void testGetRuntimeMinutes() {
        LocalDateTime now = LocalDateTime.now();
        
        // Test null values
        snapshot = new PerformanceSnapshot(5, 100, 50, 5, null, null, null);
        assertEquals(0, snapshot.getRuntimeMinutes());

        snapshot = new PerformanceSnapshot(5, 100, 50, 5, now, null, null);
        assertEquals(0, snapshot.getRuntimeMinutes());

        // Test real duration
        LocalDateTime start = now.minusMinutes(30);
        snapshot = new PerformanceSnapshot(10, 100, 50, 10, now, null, start);
        assertEquals(30, snapshot.getRuntimeMinutes());

        // Test zero duration (same time)
        snapshot = new PerformanceSnapshot(1, 100, 50, 1, now, null, now);
        assertEquals(0, snapshot.getRuntimeMinutes());

        logger.info("✅ getRuntimeMinutes test passed");
    }

    @Test
    @DisplayName("✔️ hasValidData(): Data validation logic")
    void testHasValidData() {
        LocalDateTime now = LocalDateTime.now();
        Map<String, PerformanceStats> stats = new ConcurrentHashMap<>();

        // Test invalid data scenarios
        snapshot = new PerformanceSnapshot(0, 100, 50, 0, now, stats, now);
        assertFalse(snapshot.hasValidData()); // zero searches

        snapshot = new PerformanceSnapshot(5, 100, 50, 5, null, stats, now);
        assertFalse(snapshot.hasValidData()); // null lastSearchTime

        // Constructor provides default startTime for null, so this will pass
        // Testing actual invalid case: Infinity values
        snapshot = new PerformanceSnapshot(5, Double.POSITIVE_INFINITY, 50, 5, now, stats, now);
        assertFalse(snapshot.hasValidData()); // Infinity duration

        // Note: Constructor sanitizes NaN to 0.0, so these will pass hasValidData()
        // Test instead with scenarios that should truly fail validation
        
        // Test with valid data that should pass
        snapshot = new PerformanceSnapshot(5, 100, 50, 5, now, stats, now.minusMinutes(5));
        assertTrue(snapshot.hasValidData()); // All valid, should pass

        // Test another valid data scenario
        snapshot = new PerformanceSnapshot(10, 200, 75, 10, now, stats, now.minusMinutes(1));
        assertTrue(snapshot.hasValidData()); // All valid

        logger.info("✅ hasValidData test passed");
    }    @Test
    @DisplayName("📊 getSummary(): Formatted output generation")
    void testGetSummary() {
        // Test empty data
        snapshot = new PerformanceSnapshot(0, 0, 0, 0, null, null, null);
        String summary = snapshot.getSummary();
        assertEquals("No search data available", summary);

        // Test with real data
        metrics.recordSearch("KEYWORD", 150, 5, true);
        metrics.recordSearch("REGEX", 200, 3, false);
        metrics.recordSearch("KEYWORD", 100, 2, true);

        snapshot = metrics.getSnapshot();
        summary = snapshot.getSummary();

        // Verify summary contains expected elements
        assertNotNull(summary);
        assertTrue(summary.contains("Searches: 3"));
        assertTrue(summary.contains("Avg Time:"));
        assertTrue(summary.contains("Cache:"));
        assertTrue(summary.contains("Rate:"));
        assertTrue(summary.contains("Top: KEYWORD")); // Most active type

        logger.info("Generated summary: {}", summary);
        logger.info("✅ getSummary test passed");
    }

    @Test
    @DisplayName("🧪 Integration test: Real SearchMetrics snapshot accuracy")
    void testIntegrationWithSearchMetrics() throws InterruptedException {
        // Record searches over time
        metrics.recordSearch("SEARCH_A", 100, 10, true);
        Thread.sleep(5); // Small delay
        metrics.recordSearch("SEARCH_B", 200, 5, false);
        Thread.sleep(5);
        metrics.recordSearch("SEARCH_A", 150, 8, true);

        snapshot = metrics.getSnapshot();

        // Verify integration accuracy
        assertEquals(3, snapshot.getTotalSearches());
        assertEquals(metrics.getTotalSearches(), snapshot.getTotalSearches());
        assertEquals(metrics.getCacheHitRate(), snapshot.getCacheHitRate(), 0.1);
        assertEquals(metrics.getAverageSearchTimeMs(), snapshot.getAverageDuration(), 0.1);
        assertEquals(metrics.getLastSearchTime(), snapshot.getLastSearchTime());

        // Verify search type counts match real data
        Map<String, Long> counts = snapshot.getSearchTypeCounts();
        assertEquals(2L, counts.get("SEARCH_A")); // 2 SEARCH_A
        assertEquals(1L, counts.get("SEARCH_B")); // 1 SEARCH_B

        // Verify most active type
        assertEquals("SEARCH_A", snapshot.getMostActiveSearchType());

        assertTrue(snapshot.hasValidData());
        assertTrue(snapshot.getRuntimeMinutes() >= 0);
        assertTrue(snapshot.getRecentSearchRate() >= 0); // Can be 0 if no time elapsed

        logger.info("✅ Integration test passed");
    }
}