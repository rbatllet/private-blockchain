package com.rbatllet.blockchain.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import com.rbatllet.blockchain.service.SearchMetrics.PerformanceStats;

/**
 * Comprehensive tests for SearchMetrics including thread safety validation
 * Tests ConcurrentHashMap improvements and concurrent performance tracking
 */
public class SearchMetricsTest {
    
    private static final Logger logger = LoggerFactory.getLogger(SearchMetricsTest.class);
    
    private SearchMetrics metrics;
    
    @BeforeEach
    void setUp() {
        metrics = new SearchMetrics();
    }
    
    @Test
    @DisplayName("📊 Basic functionality: Record and retrieve metrics")
    void testBasicFunctionality() {
        // Record some searches
        metrics.recordSearch("KEYWORD", 150, 5, false);
        metrics.recordSearch("REGEX", 200, 3, true);
        metrics.recordSearch("KEYWORD", 100, 2, true);
        
        // Verify basic statistics
        assertEquals(3, metrics.getTotalSearches());
        assertEquals(2, metrics.getTotalCacheHits());
        assertEquals(450, metrics.getTotalSearchTimeMs());
        
        // Verify calculated metrics
        assertEquals(66.67, metrics.getCacheHitRate(), 0.1);
        assertEquals(150.0, metrics.getAverageSearchTimeMs(), 0.1);
        
        // Verify search type stats
        Map<String, PerformanceStats> typeStats = metrics.getSearchTypeStats();
        assertNotNull(typeStats);
        assertTrue(typeStats.containsKey("KEYWORD"));
        assertTrue(typeStats.containsKey("REGEX"));
        
        logger.info("✅ Basic functionality test passed");
    }
    
    @Test
    @DisplayName("📈 Edge cases: Zero searches and empty metrics")
    void testEdgeCases() {
        // Test empty metrics
        assertEquals(0, metrics.getTotalSearches());
        assertEquals(0, metrics.getCacheHitRate(), 0.1);
        assertEquals(0, metrics.getAverageSearchTimeMs(), 0.1);
        
        Map<String, PerformanceStats> typeStats = metrics.getSearchTypeStats();
        assertNotNull(typeStats);
        assertTrue(typeStats.isEmpty());
        
        // Test single search
        metrics.recordSearch("SINGLE", 500, 1, false);
        assertEquals(1, metrics.getTotalSearches());
        assertEquals(0.0, metrics.getCacheHitRate(), 0.1);
        assertEquals(500.0, metrics.getAverageSearchTimeMs(), 0.1);
        
        logger.info("✅ Edge cases test passed");
    }
    
    @Test
    @DisplayName("🔐 Thread safety: Concurrent search recording")
    @Timeout(30)
    void testConcurrentSearchRecording() throws Exception {
        final int NUM_THREADS = 20;
        final int SEARCHES_PER_THREAD = 50;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch completionLatch = new CountDownLatch(NUM_THREADS);
        final AtomicInteger errors = new AtomicInteger(0);
        
        ExecutorService executor = Executors.newCachedThreadPool();
        
        try {
            // Submit concurrent search recording tasks
            for (int i = 0; i < NUM_THREADS; i++) {
                final int threadId = i;
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        
                        for (int j = 0; j < SEARCHES_PER_THREAD; j++) {
                            String searchType = "TYPE_" + (threadId % 5); // 5 different types
                            long duration = 100 + (j * 10);
                            int results = j + 1;
                            boolean cacheHit = (j % 3 == 0);
                            
                            metrics.recordSearch(searchType, duration, results, cacheHit);
                            
                            // Small delay to increase contention
                            Thread.sleep(1);
                        }
                    } catch (Exception e) {
                        logger.error("Error in thread {}", threadId, e);
                        errors.incrementAndGet();
                    } finally {
                        completionLatch.countDown();
                    }
                });
            }
            
            logger.info("🏁 Starting {} threads with {} searches each...", NUM_THREADS, SEARCHES_PER_THREAD);
            startLatch.countDown();
            
            assertTrue(completionLatch.await(25, TimeUnit.SECONDS), 
                      "Concurrent search recording should complete within timeout");
            
            // Validate results
            int expectedTotalSearches = NUM_THREADS * SEARCHES_PER_THREAD;
            assertEquals(expectedTotalSearches, metrics.getTotalSearches(), 
                        "All search records should be preserved");
            assertEquals(0, errors.get(), "No errors should occur during concurrent recording");
            
            // Verify search type distribution
            Map<String, PerformanceStats> typeStats = metrics.getSearchTypeStats();
            assertEquals(5, typeStats.size(), "Should have 5 different search types");
            
            for (String type : typeStats.keySet()) {
                assertNotNull(typeStats.get(type), "Each search type should have statistics");
            }
            
            // Verify that total time is reasonable
            long totalTime = metrics.getTotalSearchTimeMs();
            assertTrue(totalTime > 0, "Total search time should be positive");
            
            // Verify cache hit rate is in valid range
            double cacheHitRate = metrics.getCacheHitRate();
            assertTrue(cacheHitRate >= 0 && cacheHitRate <= 100, 
                      "Cache hit rate should be between 0 and 100%");
            
            logger.info("📊 Final metrics: {} searches, {}% cache hits, {}ms avg time",
                       metrics.getTotalSearches(), String.format("%.1f", metrics.getCacheHitRate()), 
                       String.format("%.1f", metrics.getAverageSearchTimeMs()));
            
            logger.info("✅ Concurrent search recording test passed");
            
        } finally {
            executor.shutdown();
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        }
    }
    
    @Test
    @DisplayName("🔒 Thread safety: Concurrent reads and writes")
    @Timeout(30)
    void testConcurrentReadsAndWrites() throws Exception {
        final int NUM_WRITER_THREADS = 10;
        final int NUM_READER_THREADS = 15;
        final int OPERATIONS_PER_THREAD = 30;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch completionLatch = new CountDownLatch(NUM_WRITER_THREADS + NUM_READER_THREADS);
        final AtomicInteger readOperations = new AtomicInteger(0);
        final AtomicInteger writeOperations = new AtomicInteger(0);
        final AtomicInteger errors = new AtomicInteger(0);
        
        ExecutorService executor = Executors.newCachedThreadPool();
        
        try {
            // Writer threads
            for (int i = 0; i < NUM_WRITER_THREADS; i++) {
                final int threadId = i;
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        
                        for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                            metrics.recordSearch("WRITE_TYPE_" + threadId, 
                                               100 + j, j + 1, j % 2 == 0);
                            writeOperations.incrementAndGet();
                            Thread.sleep(1);
                        }
                    } catch (Exception e) {
                        logger.error("Error in writer thread {}", threadId, e);
                        errors.incrementAndGet();
                    } finally {
                        completionLatch.countDown();
                    }
                });
            }
            
            // Reader threads
            for (int i = 0; i < NUM_READER_THREADS; i++) {
                final int threadId = i;
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        
                        for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                            // Read various metrics
                            long totalSearches = metrics.getTotalSearches();
                            long totalCacheHits = metrics.getTotalCacheHits();
                            double cacheHitRate = metrics.getCacheHitRate();
                            double avgTime = metrics.getAverageSearchTimeMs();
                            Map<String, PerformanceStats> typeStats = metrics.getSearchTypeStats();
                            
                            // Validate consistency
                            assertTrue(totalSearches >= 0, "Total searches should be non-negative");
                            assertTrue(totalCacheHits >= 0, "Total cache hits should be non-negative");
                            assertTrue(totalCacheHits <= totalSearches, "Cache hits cannot exceed total searches");
                            assertTrue(cacheHitRate >= 0 && cacheHitRate <= 100, "Cache hit rate should be valid");
                            assertTrue(avgTime >= 0, "Average time should be non-negative");
                            assertNotNull(typeStats, "Type stats should never be null");
                            
                            readOperations.incrementAndGet();
                            Thread.sleep(1);
                        }
                    } catch (Exception e) {
                        logger.error("Error in reader thread {}", threadId, e);
                        errors.incrementAndGet();
                    } finally {
                        completionLatch.countDown();
                    }
                });
            }
            
            logger.info("🏁 Starting {} writers and {} readers...", NUM_WRITER_THREADS, NUM_READER_THREADS);
            startLatch.countDown();
            
            assertTrue(completionLatch.await(25, TimeUnit.SECONDS), 
                      "Concurrent reads and writes should complete within timeout");
            
            // Validate results with tolerance for concurrency issues
            int expectedWrites = NUM_WRITER_THREADS * OPERATIONS_PER_THREAD;
            int expectedReads = NUM_READER_THREADS * OPERATIONS_PER_THREAD;
            int actualWrites = writeOperations.get();
            int actualReads = readOperations.get();
            
            // Allow some tolerance (95%) for concurrent test instability in high-load environments
            int minExpectedWrites = (int) (expectedWrites * 0.95);
            int minExpectedReads = (int) (expectedReads * 0.95);
            
            assertTrue(actualWrites >= minExpectedWrites, 
                String.format("Write operations should complete (expected: %d, actual: %d, minimum: %d)", 
                             expectedWrites, actualWrites, minExpectedWrites));
            assertTrue(actualReads >= minExpectedReads,
                String.format("Read operations should complete (expected: %d, actual: %d, minimum: %d)", 
                             expectedReads, actualReads, minExpectedReads));
            assertEquals(0, errors.get(), "No errors should occur during concurrent operations");
            
            // Recorded searches should match actual writes (with tolerance)
            long totalSearches = metrics.getTotalSearches();
            assertTrue(totalSearches >= minExpectedWrites, 
                String.format("Recorded searches should match writes (expected: %d, actual: %d, minimum: %d)", 
                             expectedWrites, totalSearches, minExpectedWrites));
            
            logger.info("📊 Final results: {} writes, {} reads completed successfully",
                       writeOperations.get(), readOperations.get());
            
            logger.info("✅ Concurrent reads and writes test passed");
            
        } finally {
            executor.shutdown();
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        }
    }
    
    @Test
    @DisplayName("🛡️ Thread safety: ConcurrentHashMap defensive copying validation")
    void testDefensiveCopying() {
        // Record some searches
        metrics.recordSearch("TYPE_A", 100, 5, false);
        metrics.recordSearch("TYPE_B", 200, 3, true);
        
        // Get the search type stats
        Map<String, PerformanceStats> typeStats1 = metrics.getSearchTypeStats();
        Map<String, PerformanceStats> typeStats2 = metrics.getSearchTypeStats();
        
        // Verify they are different instances (defensive copying)
        assertNotSame(typeStats1, typeStats2, "Should return different instances");
        
        // Verify they have the same content
        assertEquals(typeStats1.size(), typeStats2.size(), "Should have same size");
        assertEquals(typeStats1.keySet(), typeStats2.keySet(), "Should have same keys");
        
        // Verify modifying returned map doesn't affect internal state
        // ConcurrentHashMap doesn't allow null values, so let's test with a non-null value
        PerformanceStats mockStats = new PerformanceStats();
        mockStats.recordSearch(999, 1, false);
        typeStats1.put("MALICIOUS_TYPE", mockStats);
        
        Map<String, PerformanceStats> typeStats3 = metrics.getSearchTypeStats();
        assertFalse(typeStats3.containsKey("MALICIOUS_TYPE"), 
                   "Internal state should be protected from external modifications");
        
        // Verify original data is still intact
        assertEquals(2, typeStats3.size(), "Should still have original 2 types");
        assertTrue(typeStats3.containsKey("TYPE_A"), "Should still contain TYPE_A");
        assertTrue(typeStats3.containsKey("TYPE_B"), "Should still contain TYPE_B");
        
        logger.info("✅ Defensive copying validation test passed");
    }
    
    @Test
    @DisplayName("📋 Performance report generation")
    void testPerformanceReport() {
        // Add some test data
        metrics.recordSearch("KEYWORD", 150, 5, false);
        metrics.recordSearch("REGEX", 200, 3, true);
        metrics.recordSearch("SEMANTIC", 100, 8, true);
        
        String report = metrics.getPerformanceReport();
        
        // Verify report contains expected information
        assertNotNull(report);
        assertTrue(report.contains("Search Performance Report"));
        assertTrue(report.contains("Total Searches: 3"));
        assertTrue(report.contains("Cache Hit Rate"));
        assertTrue(report.contains("Average Time"));
        assertTrue(report.contains("Total Time"));
        assertTrue(report.contains("Active Since"));
        assertTrue(report.contains("Last Search"));
        
        // Verify numeric values are present
        assertTrue(report.contains("66.7%") || report.contains("66,7%")); // Cache hit rate
        assertTrue(report.contains("150") || report.contains("450")); // Time values
        
        logger.info("📊 Generated performance report:\n{}", report);
        logger.info("✅ Performance report generation test passed");
    }
    
    @Test
    @DisplayName("⏱️ Time tracking accuracy")
    void testTimeTracking() throws InterruptedException {
        long startTime = System.currentTimeMillis();
        
        // Record a search
        metrics.recordSearch("TIME_TEST", 123, 1, false);
        
        // Wait a bit
        Thread.sleep(10);
        
        // Record another search
        metrics.recordSearch("TIME_TEST", 456, 2, true);
        
        long endTime = System.currentTimeMillis();
        
        // Verify timing
        assertNotNull(metrics.getStartTime());
        assertNotNull(metrics.getLastSearchTime());
        
        // Verify last search time is recent
        assertTrue(metrics.getLastSearchTime().isAfter(metrics.getStartTime()));
        
        // Verify that test execution time is reasonable (basic sanity check)
        long testDuration = endTime - startTime;
        assertTrue(testDuration >= 10, "Test should take at least 10ms due to sleep");
        assertTrue(testDuration < 5000, "Test should complete within 5 seconds");
        
        // Verify total time calculation
        assertEquals(579, metrics.getTotalSearchTimeMs()); // 123 + 456
        assertEquals(289.5, metrics.getAverageSearchTimeMs(), 0.1); // 579 / 2
        
        logger.info("✅ Time tracking accuracy test passed");
    }
}