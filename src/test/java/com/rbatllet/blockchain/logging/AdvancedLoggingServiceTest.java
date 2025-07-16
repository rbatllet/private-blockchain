package com.rbatllet.blockchain.logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for AdvancedLoggingService
 * Tests operation tracking, performance monitoring, and security logging
 */
public class AdvancedLoggingServiceTest {
    
    private static final Logger logger = LoggerFactory.getLogger(AdvancedLoggingServiceTest.class);
    
    @BeforeEach
    void setUp() {
        // Reset metrics before each test
        AdvancedLoggingService.resetMetrics();
    }
    
    @AfterEach
    void tearDown() {
        // Clean up after each test
        AdvancedLoggingService.resetMetrics();
    }
    
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testBasicOperationTracking() {
        // Test basic operation start/end cycle
        Map<String, String> context = new HashMap<>();
        context.put("testKey", "testValue");
        
        String operationId = AdvancedLoggingService.startOperation("TEST_OPERATION", "basic_test", context);
        
        assertNotNull(operationId, "Operation ID should not be null");
        assertTrue(operationId.startsWith("TEST_OPERATION_"), "Operation ID should start with operation type");
        
        // Simulate some work
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // End operation
        AdvancedLoggingService.endOperation(operationId, true, 5, "Test completed successfully");
        
        // Verify metrics were recorded
        String report = AdvancedLoggingService.getPerformanceReport();
        assertTrue(report.contains("TEST_OPERATION"), "Report should contain operation type");
        
        logger.info("✅ Basic operation tracking test completed");
    }
    
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testOperationProgress() {
        // Test operation progress logging
        Map<String, String> context = new HashMap<>();
        context.put("totalSteps", "10");
        
        String operationId = AdvancedLoggingService.startOperation("PROGRESS_TEST", "progress_operation", context);
        
        // Log progress updates
        for (int i = 0; i <= 100; i += 25) {
            AdvancedLoggingService.logProgress(operationId, i, "Processing step " + (i/25 + 1));
        }
        
        AdvancedLoggingService.endOperation(operationId, true, 10, "All steps completed");
        
        logger.info("✅ Operation progress test completed");
    }
    
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testPerformanceMetricsLogging() {
        // Test performance metrics logging
        Map<String, Object> details = new HashMap<>();
        details.put("cacheHits", 8);
        details.put("cacheMisses", 2);
        details.put("averageResponseTime", 150.5);
        
        AdvancedLoggingService.logPerformanceMetrics(
            "SEARCH_PERFORMANCE", 
            "keyword_search", 
            500, // 500ms duration
            1024 * 1024, // 1MB data
            details
        );
        
        logger.info("✅ Performance metrics logging test completed");
    }
    
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testSecurityEventLogging() {
        // Test different security severity levels
        AdvancedLoggingService.logSecurityEvent(
            "UNAUTHORIZED_ACCESS",
            "Attempted access to restricted resource",
            AdvancedLoggingService.SecuritySeverity.HIGH,
            "user123"
        );
        
        AdvancedLoggingService.logSecurityEvent(
            "SUSPICIOUS_ACTIVITY",
            "Multiple failed login attempts",
            AdvancedLoggingService.SecuritySeverity.MEDIUM,
            "user456"
        );
        
        AdvancedLoggingService.logSecurityEvent(
            "ROUTINE_AUDIT",
            "Regular security audit completed",
            AdvancedLoggingService.SecuritySeverity.LOW,
            "system"
        );
        
        logger.info("✅ Security event logging test completed");
    }
    
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testDatabaseOperationLogging() {
        // Test database operation logging
        AdvancedLoggingService.logDatabaseOperation("SELECT", "blocks", 50, 100);
        AdvancedLoggingService.logDatabaseOperation("INSERT", "transactions", 200, 1);
        AdvancedLoggingService.logDatabaseOperation("UPDATE", "users", 75, 3);
        AdvancedLoggingService.logDatabaseOperation("DELETE", "old_logs", 30, 25);
        
        // Test slow query detection
        AdvancedLoggingService.logDatabaseOperation("SELECT", "complex_query", 6000, 1000);
        
        logger.info("✅ Database operation logging test completed");
    }
    
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testMemoryEventLogging() {
        // Test memory event logging
        AdvancedLoggingService.logMemoryEvent("GARBAGE_COLLECTION", 512, 256, "Manual GC triggered");
        AdvancedLoggingService.logMemoryEvent("MEMORY_CLEANUP", 300, 200, "Expired objects removed");
        AdvancedLoggingService.logMemoryEvent("MEMORY_ALLOCATION", 100, 400, "Large object allocated");
        
        // Test high memory usage alert
        AdvancedLoggingService.logMemoryEvent("HIGH_USAGE_DETECTED", 800, 750, "Memory usage above threshold");
        
        logger.info("✅ Memory event logging test completed");
    }
    
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testMultipleOperationsConcurrency() {
        // Test concurrent operations
        Map<String, String> context1 = new HashMap<>();
        context1.put("thread", "thread1");
        
        Map<String, String> context2 = new HashMap<>();
        context2.put("thread", "thread2");
        
        String op1 = AdvancedLoggingService.startOperation("CONCURRENT_TEST", "operation1", context1);
        String op2 = AdvancedLoggingService.startOperation("CONCURRENT_TEST", "operation2", context2);
        
        // Simulate concurrent work
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        AdvancedLoggingService.endOperation(op1, true, 3, "First operation completed");
        AdvancedLoggingService.endOperation(op2, true, 7, "Second operation completed");
        
        // Verify metrics
        String report = AdvancedLoggingService.getPerformanceReport();
        assertTrue(report.contains("CONCURRENT_TEST"), "Report should contain concurrent operations");
        
        logger.info("✅ Concurrent operations test completed");
    }
    
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testFailedOperationHandling() {
        // Test failed operation handling
        Map<String, String> context = new HashMap<>();
        context.put("expectedToFail", "true");
        
        String operationId = AdvancedLoggingService.startOperation("FAILURE_TEST", "failing_operation", context);
        
        // Simulate operation failure
        AdvancedLoggingService.endOperation(operationId, false, 0, "Operation failed due to invalid input");
        
        // Verify failure was recorded
        String report = AdvancedLoggingService.getPerformanceReport();
        assertTrue(report.contains("FAILURE_TEST"), "Report should contain failed operation");
        
        logger.info("✅ Failed operation handling test completed");
    }
    
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testPerformanceReportGeneration() {
        // Create various operations to test report generation
        for (int i = 0; i < 5; i++) {
            Map<String, String> context = new HashMap<>();
            context.put("iteration", String.valueOf(i));
            
            String operationId = AdvancedLoggingService.startOperation("REPORT_TEST", "operation_" + i, context);
            
            // Simulate variable processing times
            try {
                Thread.sleep(10 + (i * 5));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            AdvancedLoggingService.endOperation(operationId, i % 4 != 0, i * 2, "Operation " + i + " completed");
        }
        
        // Generate and verify report
        String report = AdvancedLoggingService.getPerformanceReport();
        
        assertNotNull(report, "Report should not be null");
        assertTrue(report.contains("REPORT_TEST"), "Report should contain test operations");
        assertTrue(report.contains("Operation Metrics"), "Report should contain metrics section");
        
        logger.info("📊 Performance report:\n{}", report);
        logger.info("✅ Performance report generation test completed");
    }
    
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testMetricsReset() {
        // Create some operations
        String op1 = AdvancedLoggingService.startOperation("RESET_TEST", "before_reset", null);
        AdvancedLoggingService.endOperation(op1, true, 1, "Before reset");
        
        String reportBefore = AdvancedLoggingService.getPerformanceReport();
        assertTrue(reportBefore.contains("RESET_TEST"), "Report should contain operations before reset");
        
        // Reset metrics
        AdvancedLoggingService.resetMetrics();
        
        String reportAfter = AdvancedLoggingService.getPerformanceReport();
        assertFalse(reportAfter.contains("RESET_TEST"), "Report should not contain operations after reset");
        
        // Create new operation after reset
        String op2 = AdvancedLoggingService.startOperation("RESET_TEST", "after_reset", null);
        AdvancedLoggingService.endOperation(op2, true, 1, "After reset");
        
        String reportFinal = AdvancedLoggingService.getPerformanceReport();
        assertTrue(reportFinal.contains("RESET_TEST"), "Report should contain new operations after reset");
        
        logger.info("✅ Metrics reset test completed");
    }
    
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testSlowOperationDetection() {
        // Test slow operation detection
        Map<String, String> context = new HashMap<>();
        context.put("expectedSlow", "true");
        
        String operationId = AdvancedLoggingService.startOperation("SLOW_TEST", "slow_operation", context);
        
        // Simulate slow operation (sleep for more than threshold)
        try {
            Thread.sleep(100); // Simulate processing time
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        AdvancedLoggingService.endOperation(operationId, true, 1, "Slow operation completed");
        
        logger.info("✅ Slow operation detection test completed");
    }
}