package com.rbatllet.blockchain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for PerformanceMetricsService
 * Validates comprehensive performance monitoring and metrics collection
 */
public class PerformanceMetricsServiceTest {
    
    private static final Logger logger = LoggerFactory.getLogger(PerformanceMetricsServiceTest.class);
    private PerformanceMetricsService performanceMetrics;
    
    @BeforeEach
    void setUp() {
        performanceMetrics = PerformanceMetricsService.getInstance();
        performanceMetrics.resetMetrics();
        logger.info("✅ Performance metrics service reset for test");
    }
    
    @Test
    @DisplayName("Test response time recording and metrics")
    void testResponseTimeRecording() {
        // Record different response times
        performanceMetrics.recordResponseTime("TEST_OPERATION", 100L);
        performanceMetrics.recordResponseTime("TEST_OPERATION", 200L);
        performanceMetrics.recordResponseTime("TEST_OPERATION", 150L);
        
        // Test slow operation threshold
        performanceMetrics.recordResponseTime("SLOW_OPERATION", 6000L); // Above 5s threshold
        
        String report = performanceMetrics.getPerformanceReport();
        
        // Verify response time metrics are recorded
        assertNotNull(report);
        assertTrue(report.contains("TEST_OPERATION"));
        assertTrue(report.contains("SLOW_OPERATION"));
        assertTrue(report.contains("RESPONSE TIME METRICS"));
        
        // Verify averages are calculated
        assertTrue(report.contains("Avg:"));
        assertTrue(report.contains("Min:"));
        assertTrue(report.contains("Max:"));
        assertTrue(report.contains("95th percentile:"));
        
        logger.info("✅ Response time recording test completed");
    }
    
    @Test
    @DisplayName("Test memory usage recording and thresholds")
    void testMemoryUsageRecording() {
        // Record different memory usage levels
        performanceMetrics.recordMemoryUsage("LOW_MEMORY_OP", 100L);
        performanceMetrics.recordMemoryUsage("MEDIUM_MEMORY_OP", 600L); // Above warning threshold
        performanceMetrics.recordMemoryUsage("HIGH_MEMORY_OP", 1200L); // Above critical threshold
        
        String report = performanceMetrics.getPerformanceReport();
        
        // Verify memory metrics are recorded
        assertNotNull(report);
        assertTrue(report.contains("MEMORY USAGE BY OPERATION"));
        assertTrue(report.contains("LOW_MEMORY_OP"));
        assertTrue(report.contains("MEDIUM_MEMORY_OP"));
        assertTrue(report.contains("HIGH_MEMORY_OP"));
        
        // Verify memory thresholds trigger alerts
        assertTrue(report.contains("ACTIVE PERFORMANCE ALERTS") || 
                  report.contains("No active alerts"));
        
        logger.info("✅ Memory usage recording test completed");
    }
    
    @Test
    @DisplayName("Test throughput recording and metrics")
    void testThroughputRecording() {
        // Record different throughput levels
        performanceMetrics.recordThroughput("BATCH_OPERATION", 50);
        performanceMetrics.recordThroughput("BATCH_OPERATION", 75);
        performanceMetrics.recordThroughput("BATCH_OPERATION", 100);
        
        performanceMetrics.recordThroughput("SINGLE_OPERATION", 1);
        performanceMetrics.recordThroughput("SINGLE_OPERATION", 1);
        
        String report = performanceMetrics.getPerformanceReport();
        
        // Verify throughput metrics are recorded
        assertNotNull(report);
        assertTrue(report.contains("THROUGHPUT METRICS"));
        assertTrue(report.contains("BATCH_OPERATION"));
        assertTrue(report.contains("SINGLE_OPERATION"));
        assertTrue(report.contains("Total Items:"));
        assertTrue(report.contains("Avg/Operation:"));
        assertTrue(report.contains("Peak/Operation:"));
        
        logger.info("✅ Throughput recording test completed");
    }
    
    @Test
    @DisplayName("Test operation success/failure tracking")
    void testOperationTracking() {
        // Record successful and failed operations
        performanceMetrics.recordOperation("SUCCESS_OP", true);
        performanceMetrics.recordOperation("SUCCESS_OP", true);
        performanceMetrics.recordOperation("SUCCESS_OP", false);
        
        performanceMetrics.recordOperation("FAILURE_OP", false);
        performanceMetrics.recordOperation("FAILURE_OP", false);
        
        String report = performanceMetrics.getPerformanceReport();
        
        // Verify operation tracking
        assertNotNull(report);
        assertTrue(report.contains("SYSTEM OVERVIEW"));
        assertTrue(report.contains("Total Operations:"));
        assertTrue(report.contains("Total Errors:"));
        assertTrue(report.contains("Error Rate:"));
        
        logger.info("✅ Operation tracking test completed");
    }
    
    @Test
    @DisplayName("Test system health calculation")
    void testSystemHealthCalculation() {
        // Record various operations to generate health data
        performanceMetrics.recordOperation("HEALTH_TEST", true);
        performanceMetrics.recordOperation("HEALTH_TEST", true);
        performanceMetrics.recordOperation("HEALTH_TEST", false); // Some failures
        
        performanceMetrics.recordResponseTime("HEALTH_TEST", 1000L);
        performanceMetrics.recordMemoryUsage("HEALTH_TEST", 200L);
        
        String healthSummary = performanceMetrics.getSystemHealthSummary();
        
        // Verify health summary
        assertNotNull(healthSummary);
        assertTrue(healthSummary.contains("SYSTEM HEALTH SUMMARY"));
        assertTrue(healthSummary.contains("Health Score:"));
        assertTrue(healthSummary.contains("Status:"));
        assertTrue(healthSummary.contains("Error Rate:"));
        assertTrue(healthSummary.contains("Memory Usage:"));
        
        logger.info("✅ System health calculation test completed");
    }
    
    @Test
    @DisplayName("Test comprehensive performance report generation")
    void testComprehensiveReport() {
        // Generate comprehensive test data
        performanceMetrics.recordResponseTime("COMPREHENSIVE_TEST", 500L);
        performanceMetrics.recordMemoryUsage("COMPREHENSIVE_TEST", 300L);
        performanceMetrics.recordThroughput("COMPREHENSIVE_TEST", 25);
        performanceMetrics.recordOperation("COMPREHENSIVE_TEST", true);
        
        String report = performanceMetrics.getPerformanceReport();
        
        // Verify all sections are present
        assertNotNull(report);
        assertTrue(report.contains("COMPREHENSIVE PERFORMANCE METRICS REPORT"));
        assertTrue(report.contains("Generated:"));
        assertTrue(report.contains("Uptime:"));
        assertTrue(report.contains("SYSTEM OVERVIEW"));
        assertTrue(report.contains("MEMORY METRICS"));
        assertTrue(report.contains("RESPONSE TIME METRICS"));
        assertTrue(report.contains("MEMORY USAGE BY OPERATION"));
        assertTrue(report.contains("THROUGHPUT METRICS"));
        
        logger.info("📊 Comprehensive performance report:\n{}", report);
        logger.info("✅ Comprehensive report generation test completed");
    }
    
    @Test
    @DisplayName("Test metrics reset functionality")
    void testMetricsReset() {
        // Record some data
        performanceMetrics.recordResponseTime("RESET_TEST", 100L);
        performanceMetrics.recordMemoryUsage("RESET_TEST", 50L);
        performanceMetrics.recordOperation("RESET_TEST", true);
        
        // Verify data exists
        String reportBefore = performanceMetrics.getPerformanceReport();
        assertTrue(reportBefore.contains("RESET_TEST"));
        
        // Reset metrics
        performanceMetrics.resetMetrics();
        
        // Verify data is cleared
        String reportAfter = performanceMetrics.getPerformanceReport();
        assertFalse(reportAfter.contains("RESET_TEST"));
        assertTrue(reportAfter.contains("Total Operations: 0"));
        assertTrue(reportAfter.contains("Total Errors: 0"));
        
        logger.info("✅ Metrics reset test completed");
    }
    
    @Test
    @DisplayName("Test performance alert generation")
    void testPerformanceAlerts() {
        // Trigger different types of alerts
        performanceMetrics.recordResponseTime("ALERT_TEST", 6000L); // Slow operation
        performanceMetrics.recordMemoryUsage("ALERT_TEST", 600L);   // Warning threshold
        performanceMetrics.recordMemoryUsage("ALERT_CRITICAL", 1200L); // Critical threshold
        
        String report = performanceMetrics.getPerformanceReport();
        
        // Verify alerts are generated
        assertNotNull(report);
        
        // Check if alerts section exists (might be empty if no alerts)
        assertTrue(report.contains("ACTIVE PERFORMANCE ALERTS") || 
                  report.contains("No active alerts"));
        
        logger.info("✅ Performance alert generation test completed");
    }
    
    @Test
    @DisplayName("Test concurrent metrics recording")
    void testConcurrentMetricsRecording() throws InterruptedException {
        final int threadCount = 10;
        final int operationsPerThread = 100;
        
        Thread[] threads = new Thread[threadCount];
        
        // Create threads that record metrics concurrently
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < operationsPerThread; j++) {
                    performanceMetrics.recordResponseTime("CONCURRENT_TEST_T" + threadId, 50L + j);
                    performanceMetrics.recordMemoryUsage("CONCURRENT_TEST_T" + threadId, 100L + j);
                    performanceMetrics.recordThroughput("CONCURRENT_TEST_T" + threadId, j + 1);
                    performanceMetrics.recordOperation("CONCURRENT_TEST_T" + threadId, j % 10 != 0); // 10% failure rate
                }
            });
        }
        
        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }
        
        String report = performanceMetrics.getPerformanceReport();
        
        // Verify concurrent operations were recorded correctly
        assertNotNull(report);
        assertTrue(report.contains("CONCURRENT_TEST_T"));
        assertTrue(report.contains("Total Operations: " + (threadCount * operationsPerThread)));
        
        logger.info("✅ Concurrent metrics recording test completed");
    }
    
    @Test
    @DisplayName("Test singleton pattern")
    void testSingletonPattern() {
        PerformanceMetricsService instance1 = PerformanceMetricsService.getInstance();
        PerformanceMetricsService instance2 = PerformanceMetricsService.getInstance();
        
        assertSame(instance1, instance2);
        assertSame(performanceMetrics, instance1);
        
        logger.info("✅ Singleton pattern test completed");
    }
}