package com.rbatllet.blockchain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.HashMap;

/**
 * Test class for AlertService
 * Validates structured alert generation and logging functionality
 */
public class AlertServiceTest {
    
    private static final Logger logger = LoggerFactory.getLogger(AlertServiceTest.class);
    private AlertService alertService;
    
    @BeforeEach
    void setUp() {
        alertService = AlertService.getInstance();
        alertService.resetStatistics();
        logger.info("✅ Alert service reset for test");
    }
    
    @Test
    @DisplayName("Test singleton pattern")
    void testSingletonPattern() {
        AlertService instance1 = AlertService.getInstance();
        AlertService instance2 = AlertService.getInstance();
        
        assertSame(instance1, instance2);
        assertSame(alertService, instance1);
        
        logger.info("✅ Singleton pattern test completed");
    }
    
    @Test
    @DisplayName("Test performance alert generation")
    void testPerformanceAlerts() {
        // Test slow operation alert
        alertService.sendPerformanceAlert("TEST_OPERATION", 6000L, 100.0);
        
        // Test high memory alert
        alertService.sendPerformanceAlert("MEMORY_OPERATION", 1000L, 600.0);
        
        // Test critical memory alert
        alertService.sendPerformanceAlert("CRITICAL_OPERATION", 2000L, 1200.0);
        
        // Verify statistics
        String stats = alertService.getAlertStatistics();
        assertNotNull(stats);
        assertTrue(stats.contains("total_alerts"));
        assertTrue(stats.contains("by_severity"));
        assertTrue(stats.contains("by_type"));
        
        logger.info("✅ Performance alerts test completed");
    }
    
    @Test
    @DisplayName("Test health alert generation")
    void testHealthAlerts() {
        // Test poor health
        alertService.sendHealthAlert(45.0, 3.0, "🟠 FAIR");
        
        // Test critical health
        alertService.sendHealthAlert(20.0, 15.0, "🚨 CRITICAL");
        
        // Test high error rate
        alertService.sendHealthAlert(70.0, 8.0, "🟡 GOOD");
        
        // Test critical error rate
        alertService.sendHealthAlert(80.0, 12.0, "🟢 EXCELLENT");
        
        String stats = alertService.getAlertStatistics();
        assertNotNull(stats);
        
        logger.info("✅ Health alerts test completed");
    }
    
    @Test
    @DisplayName("Test security alert generation")
    void testSecurityAlerts() {
        // Test unauthorized access alert
        alertService.sendSecurityAlert(
            "UNAUTHORIZED_ACCESS",
            "Failed login attempt with invalid credentials",
            "192.168.1.100",
            "suspicious_user"
        );
        
        // Test data tampering alert
        alertService.sendSecurityAlert(
            "DATA_TAMPERING",
            "Block hash mismatch detected",
            "10.0.0.5",
            "admin"
        );
        
        String stats = alertService.getAlertStatistics();
        assertTrue(stats.contains("UNAUTHORIZED_ACCESS") || stats.contains("DATA_TAMPERING"));
        
        logger.info("✅ Security alerts test completed");
    }
    
    @Test
    @DisplayName("Test blockchain integrity alerts")
    void testIntegrityAlerts() {
        // Test hash mismatch alert
        alertService.sendIntegrityAlert(
            "HASH_MISMATCH",
            42,
            "0x1234567890abcdef",
            "Block hash does not match expected value"
        );
        
        // Test chain corruption alert
        alertService.sendIntegrityAlert(
            "CHAIN_CORRUPTION",
            100,
            "0xfedcba0987654321",
            "Previous block reference is invalid"
        );
        
        String stats = alertService.getAlertStatistics();
        assertTrue(stats.contains("HASH_MISMATCH") || stats.contains("CHAIN_CORRUPTION"));
        
        logger.info("✅ Integrity alerts test completed");
    }
    
    @Test
    @DisplayName("Test custom alert generation")
    void testCustomAlerts() {
        Map<String, Object> additionalData = new HashMap<>();
        additionalData.put("custom_field", "custom_value");
        additionalData.put("numeric_field", 123);
        additionalData.put("boolean_field", true);
        
        // Test info level custom alert
        alertService.sendCustomAlert(
            AlertService.AlertSeverity.INFO,
            AlertService.AlertType.CUSTOM,
            "CUSTOM_INFO_ALERT",
            "This is a custom informational alert",
            additionalData
        );
        
        // Test warning level custom alert
        alertService.sendCustomAlert(
            AlertService.AlertSeverity.WARNING,
            AlertService.AlertType.CUSTOM,
            "CUSTOM_WARNING_ALERT",
            "This is a custom warning alert",
            null
        );
        
        // Test critical level custom alert
        alertService.sendCustomAlert(
            AlertService.AlertSeverity.CRITICAL,
            AlertService.AlertType.CUSTOM,
            "CUSTOM_CRITICAL_ALERT",
            "This is a custom critical alert",
            additionalData
        );
        
        String stats = alertService.getAlertStatistics();
        assertTrue(stats.contains("CUSTOM_INFO_ALERT") || 
                  stats.contains("CUSTOM_WARNING_ALERT") || 
                  stats.contains("CUSTOM_CRITICAL_ALERT"));
        
        logger.info("✅ Custom alerts test completed");
    }
    
    @Test
    @DisplayName("Test alert statistics tracking")
    void testAlertStatistics() {
        // Generate various alerts
        alertService.sendPerformanceAlert("STATS_TEST", 6000L, 200.0);
        alertService.sendHealthAlert(30.0, 8.0, "🔴 POOR");
        alertService.sendSecurityAlert("STATS_SECURITY", "Test security alert", "127.0.0.1", "test_user");
        
        String stats = alertService.getAlertStatistics();
        assertNotNull(stats);
        assertFalse(stats.isEmpty());
        
        // Verify JSON structure
        assertTrue(stats.contains("total_alerts"));
        assertTrue(stats.contains("by_severity"));
        assertTrue(stats.contains("by_type"));
        
        // Test statistics reset
        alertService.resetStatistics();
        String resetStats = alertService.getAlertStatistics();
        assertTrue(resetStats.contains("\"total_alerts\" : 0"));
        
        logger.info("✅ Alert statistics test completed");
    }
    
    @Test
    @DisplayName("Test alert threshold validation")
    void testAlertThresholds() {
        // Test operations below thresholds (should not generate alerts)
        alertService.sendPerformanceAlert("NORMAL_OPERATION", 2000L, 100.0);
        alertService.sendHealthAlert(85.0, 2.0, "🟢 EXCELLENT");
        
        // Test operations above thresholds (should generate alerts)
        alertService.sendPerformanceAlert("SLOW_OPERATION", 8000L, 800.0);
        alertService.sendHealthAlert(15.0, 12.0, "🚨 CRITICAL");
        
        String stats = alertService.getAlertStatistics();
        
        // Should have alerts for threshold violations
        assertTrue(stats.contains("total_alerts"));
        assertFalse(stats.contains("\"total_alerts\" : 0"));
        
        logger.info("✅ Alert thresholds test completed");
    }
    
    @Test
    @DisplayName("Test concurrent alert generation")
    void testConcurrentAlerts() throws InterruptedException {
        final int threadCount = 5;
        final int alertsPerThread = 10;
        
        Thread[] threads = new Thread[threadCount];
        
        // Create threads that generate alerts concurrently
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < alertsPerThread; j++) {
                    alertService.sendPerformanceAlert(
                        "CONCURRENT_TEST_T" + threadId, 
                        6000L + j, 
                        200.0 + j
                    );
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
        
        String stats = alertService.getAlertStatistics();
        assertNotNull(stats);
        
        // Verify concurrent operations were recorded
        assertTrue(stats.contains("total_alerts"));
        
        logger.info("✅ Concurrent alerts test completed");
    }
    
    @Test
    @DisplayName("Test alert service lifecycle")
    void testAlertServiceLifecycle() {
        // Test service initialization
        assertNotNull(alertService);
        
        // Test normal operation
        alertService.sendCustomAlert(
            AlertService.AlertSeverity.INFO,
            AlertService.AlertType.SYSTEM,
            "LIFECYCLE_TEST",
            "Testing service lifecycle",
            null
        );
        
        // Test statistics generation
        String stats = alertService.getAlertStatistics();
        assertNotNull(stats);
        assertFalse(stats.isEmpty());
        
        // Test statistics reset
        alertService.resetStatistics();
        String resetStats = alertService.getAlertStatistics();
        assertTrue(resetStats.contains("\"total_alerts\" : 0"));
        
        logger.info("✅ Alert service lifecycle test completed");
    }
}