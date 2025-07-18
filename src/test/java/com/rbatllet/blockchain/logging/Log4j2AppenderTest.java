package com.rbatllet.blockchain.logging;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Test to verify Log4j2 appender configuration
 */
public class Log4j2AppenderTest {
    
    private static final Logger logger = LoggerFactory.getLogger(Log4j2AppenderTest.class);
    
    @Test
    void testLog4j2Configuration() {
        logger.info("=== LOG4J2 CONFIGURATION TEST ===");
        
        // Test alerts logger directly
        Logger alertsStructuredLogger = LoggerFactory.getLogger("alerts.structured");
        alertsStructuredLogger.error("🚨 TEST ALERT: This should go to structured-alerts.log file");
        
        // Test performance logger
        Logger performanceLogger = LoggerFactory.getLogger("performance.metrics");
        performanceLogger.info("📊 TEST METRIC: This should go to performance-metrics.log file");
        
        // Test security logger
        Logger securityLogger = LoggerFactory.getLogger("security.events");
        securityLogger.warn("🔐 TEST SECURITY: This should go to security-events.log file");
        
        // Wait a bit for logs to be written
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Check if files were created
        File alertsFile = new File("logs/structured-alerts.log");
        File performanceFile = new File("logs/performance-metrics.log");
        File securityFile = new File("logs/security-events.log");
        
        logger.info("=== FILE CREATION CHECK ===");
        logger.info("structured-alerts.log exists: {}", alertsFile.exists());
        logger.info("performance-metrics.log exists: {}", performanceFile.exists());
        logger.info("security-events.log exists: {}", securityFile.exists());
        
        if (alertsFile.exists()) {
            logger.info("Alerts file size: {} bytes", alertsFile.length());
        }
        if (performanceFile.exists()) {
            logger.info("Performance file size: {} bytes", performanceFile.length());
        }
        if (securityFile.exists()) {
            logger.info("Security file size: {} bytes", securityFile.length());
        }
    }
}