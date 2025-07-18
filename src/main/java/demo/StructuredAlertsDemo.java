package demo;

import com.rbatllet.blockchain.service.AlertService;
import com.rbatllet.blockchain.service.PerformanceMetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Demo showcasing the Structured Alerts System
 * Demonstrates various types of alerts and monitoring capabilities
 */
public class StructuredAlertsDemo {
    
    private static final Logger logger = LoggerFactory.getLogger(StructuredAlertsDemo.class);
    
    public static void main(String[] args) {
        logger.info("🚨 Starting Structured Alerts System Demo");
        
        try {
            runAlertsDemo();
        } catch (Exception e) {
            logger.error("❌ Demo failed: {}", e.getMessage(), e);
            System.exit(1);
        }
        
        logger.info("✅ Structured Alerts Demo completed successfully");
        
        // Force exit to stop background threads
        System.exit(0);
    }
    
    private static void runAlertsDemo() throws InterruptedException {
        AlertService alertService = AlertService.getInstance();
        PerformanceMetricsService performanceMetrics = PerformanceMetricsService.getInstance();
        
        logger.info("📋 DEMO SECTION 1: Performance Alerts");
        demonstratePerformanceAlerts(alertService, performanceMetrics);
        
        if (!"true".equals(System.getProperty("demo.skip.sleep"))) {
            Thread.sleep(2000);
        }
        
        logger.info("📋 DEMO SECTION 2: System Health Alerts");
        demonstrateHealthAlerts(alertService);
        
        if (!"true".equals(System.getProperty("demo.skip.sleep"))) {
            Thread.sleep(2000);
        }
        
        logger.info("📋 DEMO SECTION 3: Security Alerts");
        demonstrateSecurityAlerts(alertService);
        
        if (!"true".equals(System.getProperty("demo.skip.sleep"))) {
            Thread.sleep(2000);
        }
        
        logger.info("📋 DEMO SECTION 4: Blockchain Integrity Alerts");
        demonstrateIntegrityAlerts(alertService);
        
        if (!"true".equals(System.getProperty("demo.skip.sleep"))) {
            Thread.sleep(2000);
        }
        
        logger.info("📋 DEMO SECTION 5: Custom Alerts");
        demonstrateCustomAlerts(alertService);
        
        if (!"true".equals(System.getProperty("demo.skip.sleep"))) {
            Thread.sleep(2000);
        }
        
        logger.info("📋 DEMO SECTION 6: Alert Statistics and Monitoring");
        demonstrateAlertStatistics(alertService);
        
        if (!"true".equals(System.getProperty("demo.skip.sleep"))) {
            Thread.sleep(2000);
        }
        
        logger.info("📋 DEMO SECTION 7: Performance Metrics Integration");
        demonstrateMetricsIntegration(performanceMetrics);
    }
    
    private static void demonstratePerformanceAlerts(AlertService alertService, PerformanceMetricsService performanceMetrics) {
        logger.info("🔧 Generating performance alerts...");
        
        // Simulate slow operations
        logger.info("   Simulating slow blockchain validation operation...");
        performanceMetrics.recordResponseTime("BLOCKCHAIN_VALIDATION", 7500L);
        
        logger.info("   Simulating slow search operation...");
        performanceMetrics.recordResponseTime("ENCRYPTED_SEARCH", 8200L);
        
        // Simulate high memory usage
        logger.info("   Simulating high memory usage operation...");
        performanceMetrics.recordMemoryUsage("LARGE_BLOCK_PROCESSING", 750L);
        
        // Simulate critical memory usage
        logger.info("   Simulating critical memory usage operation...");
        performanceMetrics.recordMemoryUsage("MASSIVE_ENCRYPTION", 1200L);
        
        // Direct performance alerts
        logger.info("   Sending direct performance alerts...");
        alertService.sendPerformanceAlert("DEMO_OPERATION", 6000L, 600.0);
        alertService.sendPerformanceAlert("CRITICAL_DEMO", 9000L, 1100.0);
        
        logger.info("✅ Performance alerts demonstration completed");
    }
    
    private static void demonstrateHealthAlerts(AlertService alertService) {
        logger.info("🏥 Generating system health alerts...");
        
        // Simulate declining system health
        logger.info("   Simulating fair system health...");
        alertService.sendHealthAlert(65.0, 3.2, "🟡 GOOD");
        
        logger.info("   Simulating poor system health...");
        alertService.sendHealthAlert(45.0, 6.5, "🟠 FAIR");
        
        logger.info("   Simulating critical system health...");
        alertService.sendHealthAlert(20.0, 12.8, "🚨 CRITICAL");
        
        // Simulate high error rates
        logger.info("   Simulating high error rate...");
        alertService.sendHealthAlert(75.0, 8.2, "🟢 EXCELLENT");
        
        logger.info("   Simulating critical error rate...");
        alertService.sendHealthAlert(80.0, 15.5, "🟢 EXCELLENT");
        
        logger.info("✅ Health alerts demonstration completed");
    }
    
    private static void demonstrateSecurityAlerts(AlertService alertService) {
        logger.info("🔐 Generating security alerts...");
        
        // Simulate various security threats
        logger.info("   Simulating unauthorized access attempt...");
        alertService.sendSecurityAlert(
            "UNAUTHORIZED_ACCESS",
            "Multiple failed login attempts from suspicious IP",
            "192.168.1.100",
            "potential_attacker"
        );
        
        logger.info("   Simulating data tampering attempt...");
        alertService.sendSecurityAlert(
            "DATA_TAMPERING",
            "Attempt to modify blockchain data detected",
            "10.0.0.5",
            "suspicious_admin"
        );
        
        logger.info("   Simulating privilege escalation...");
        alertService.sendSecurityAlert(
            "PRIVILEGE_ESCALATION",
            "User attempting to access restricted blockchain operations",
            "172.16.0.10",
            "test_user"
        );
        
        logger.info("   Simulating crypto attack...");
        alertService.sendSecurityAlert(
            "CRYPTO_ATTACK",
            "Brute force attack on encryption keys detected",
            "203.0.113.1",
            "anonymous"
        );
        
        logger.info("✅ Security alerts demonstration completed");
    }
    
    private static void demonstrateIntegrityAlerts(AlertService alertService) {
        logger.info("🔗 Generating blockchain integrity alerts...");
        
        // Simulate blockchain integrity issues
        logger.info("   Simulating hash mismatch...");
        alertService.sendIntegrityAlert(
            "HASH_MISMATCH",
            142,
            "0x1a2b3c4d5e6f7890",
            "Block hash does not match expected SHA-256 value"
        );
        
        logger.info("   Simulating chain corruption...");
        alertService.sendIntegrityAlert(
            "CHAIN_CORRUPTION",
            205,
            "0xfedcba9876543210",
            "Previous block reference points to invalid block"
        );
        
        logger.info("   Simulating timestamp anomaly...");
        alertService.sendIntegrityAlert(
            "TIMESTAMP_ANOMALY",
            67,
            "0x9876543210abcdef",
            "Block timestamp is significantly out of sequence"
        );
        
        logger.info("   Simulating merkle root mismatch...");
        alertService.sendIntegrityAlert(
            "MERKLE_ROOT_MISMATCH",
            98,
            "0xabcdef1234567890",
            "Calculated Merkle root does not match stored value"
        );
        
        logger.info("✅ Integrity alerts demonstration completed");
    }
    
    private static void demonstrateCustomAlerts(AlertService alertService) {
        logger.info("🔧 Generating custom alerts...");
        
        // Custom business logic alerts
        Map<String, Object> businessData = new HashMap<>();
        businessData.put("transaction_count", 1500);
        businessData.put("daily_limit", 1000);
        businessData.put("excess_percentage", 50.0);
        
        logger.info("   Sending business rule violation alert...");
        alertService.sendCustomAlert(
            AlertService.AlertSeverity.WARNING,
            AlertService.AlertType.CUSTOM,
            "DAILY_LIMIT_EXCEEDED",
            "Daily transaction limit exceeded by 50%",
            businessData
        );
        
        // Custom maintenance alerts
        Map<String, Object> maintenanceData = new HashMap<>();
        maintenanceData.put("scheduled_time", "2024-01-15T02:00:00");
        maintenanceData.put("duration_hours", 4);
        maintenanceData.put("affected_services", "blockchain,search,validation");
        
        logger.info("   Sending maintenance window alert...");
        alertService.sendCustomAlert(
            AlertService.AlertSeverity.INFO,
            AlertService.AlertType.SYSTEM,
            "MAINTENANCE_SCHEDULED",
            "Scheduled maintenance window for blockchain services",
            maintenanceData
        );
        
        // Custom performance optimization alert
        Map<String, Object> optimizationData = new HashMap<>();
        optimizationData.put("operation", "batch_encryption");
        optimizationData.put("old_time_ms", 5000);
        optimizationData.put("new_time_ms", 2000);
        optimizationData.put("improvement_percentage", 60.0);
        
        logger.info("   Sending performance optimization alert...");
        alertService.sendCustomAlert(
            AlertService.AlertSeverity.INFO,
            AlertService.AlertType.PERFORMANCE,
            "PERFORMANCE_IMPROVEMENT",
            "Significant performance improvement detected in batch encryption",
            optimizationData
        );
        
        // Critical system failure alert
        Map<String, Object> failureData = new HashMap<>();
        failureData.put("service", "blockchain_validator");
        failureData.put("error_code", "VALIDATION_ENGINE_FAILURE");
        failureData.put("recovery_time_estimate", "15_minutes");
        
        logger.info("   Sending critical system failure alert...");
        alertService.sendCustomAlert(
            AlertService.AlertSeverity.CRITICAL,
            AlertService.AlertType.SYSTEM,
            "SYSTEM_FAILURE",
            "Critical blockchain validation service failure",
            failureData
        );
        
        logger.info("✅ Custom alerts demonstration completed");
    }
    
    private static void demonstrateAlertStatistics(AlertService alertService) {
        logger.info("📊 Demonstrating alert statistics and monitoring...");
        
        // Generate some additional alerts for statistics
        logger.info("   Generating sample alerts for statistics...");
        for (int i = 0; i < 5; i++) {
            alertService.sendPerformanceAlert("STATS_TEST_" + i, 6000L + (i * 500), 200.0 + (i * 50));
            
            if (!"true".equals(System.getProperty("demo.skip.sleep"))) {
                try {
                    Thread.sleep(100); // Small delay to show progression
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        // Display comprehensive statistics
        logger.info("   Retrieving alert statistics...");
        String statistics = alertService.getAlertStatistics();
        
        logger.info("📊 ALERT STATISTICS:");
        logger.info("\n{}", statistics);
        
        // Demonstrate statistics analysis
        if (statistics.contains("\"total_alerts\"")) {
            logger.info("✅ Statistics contain total alert count");
        }
        
        if (statistics.contains("\"by_severity\"")) {
            logger.info("✅ Statistics contain severity breakdown");
        }
        
        if (statistics.contains("\"by_type\"")) {
            logger.info("✅ Statistics contain type breakdown");
        }
        
        logger.info("✅ Alert statistics demonstration completed");
    }
    
    private static void demonstrateMetricsIntegration(PerformanceMetricsService performanceMetrics) {
        logger.info("🔗 Demonstrating performance metrics integration...");
        
        // Generate various performance metrics that should trigger alerts
        logger.info("   Recording slow operations...");
        performanceMetrics.recordResponseTime("INTEGRATION_TEST_SLOW", 8500L);
        performanceMetrics.recordResponseTime("INTEGRATION_TEST_VERY_SLOW", 12000L);
        
        logger.info("   Recording high memory operations...");
        performanceMetrics.recordMemoryUsage("INTEGRATION_TEST_MEMORY", 800L);
        performanceMetrics.recordMemoryUsage("INTEGRATION_TEST_CRITICAL_MEMORY", 1500L);
        
        logger.info("   Recording failed operations...");
        performanceMetrics.recordOperation("INTEGRATION_TEST_FAILURE", false);
        performanceMetrics.recordOperation("INTEGRATION_TEST_FAILURE", false);
        performanceMetrics.recordOperation("INTEGRATION_TEST_SUCCESS", true);
        
        // Get system health summary (which triggers health alerts)
        logger.info("   Checking system health...");
        String healthSummary = performanceMetrics.getSystemHealthSummary();
        logger.info("🏥 SYSTEM HEALTH SUMMARY:\n{}", healthSummary);
        
        // Get performance report
        logger.info("   Generating performance report...");
        String performanceReport = performanceMetrics.getPerformanceReport();
        logger.info("📊 PERFORMANCE REPORT:\n{}", performanceReport);
        
        logger.info("✅ Metrics integration demonstration completed");
    }
}