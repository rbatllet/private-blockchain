package com.rbatllet.blockchain.logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for LoggingManager
 * Tests integration of all logging services and coordination
 */
public class LoggingManagerTest {
    
    private static final Logger logger = LoggerFactory.getLogger(LoggingManagerTest.class);
    
    @BeforeEach
    void setUp() {
        // Initialize logging manager
        LoggingManager.initialize();
    }
    
    @AfterEach
    void tearDown() {
        // Shutdown logging manager
        LoggingManager.shutdown();
    }
    
    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void testLoggingManagerInitialization() {
        // Test initialization
        LoggingManager.initialize();
        
        // Get initial system report
        String report = LoggingManager.getSystemReport();
        assertNotNull(report, "System report should not be null");
        assertTrue(report.contains("COMPREHENSIVE SYSTEM LOGGING REPORT"), "Report should contain header");
        
        logger.info("✅ Logging manager initialization test completed");
    }
    
    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void testLoggingManagerStartStop() {
        // Test start/stop cycle
        LoggingManager.start();
        
        // Simulate some activity
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        LoggingManager.shutdown();
        
        logger.info("✅ Logging manager start/stop test completed");
    }
    
    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void testBlockchainOperationLogging() {
        // Test blockchain operation logging
        List<String> result = LoggingManager.logBlockchainOperation(
            "BLOCK_CREATION", 
            "create_genesis_block", 
            0L,
            1024L,
            () -> Arrays.asList("block1", "block2", "block3")
        );
        
        assertNotNull(result, "Result should not be null");
        assertEquals(3, result.size(), "Should have 3 blocks");
        
        logger.info("✅ Blockchain operation logging test completed");
    }
    
    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void testCryptoOperationLogging() {
        // Test crypto operation logging
        String result = LoggingManager.logCryptoOperation(
            "ENCRYPT", 
            "AES-256-GCM",
            512L,
            () -> "encrypted_data_base64"
        );
        
        assertNotNull(result, "Result should not be null");
        assertEquals("encrypted_data_base64", result, "Should return encrypted data");
        
        logger.info("✅ Crypto operation logging test completed");
    }
    
    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void testSearchOperationLogging() {
        // Test search operation logging
        List<String> result = LoggingManager.logSearchOperation(
            "KEYWORD_SEARCH",
            "blockchain transaction",
            () -> Arrays.asList("result1", "result2", "result3", "result4", "result5")
        );
        
        assertNotNull(result, "Result should not be null");
        assertEquals(5, result.size(), "Should have 5 search results");
        
        logger.info("✅ Search operation logging test completed");
    }
    
    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void testKeyOperationLogging() {
        // Test key operation logging
        String result = LoggingManager.logKeyOperation(
            "GENERATE",
            "RSA_2048",
            "key_123",
            () -> "public_key_data"
        );
        
        assertNotNull(result, "Result should not be null");
        assertEquals("public_key_data", result, "Should return key data");
        
        logger.info("✅ Key operation logging test completed");
    }
    
    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void testFailedOperationLogging() {
        // Test failed operation logging
        assertThrows(RuntimeException.class, () -> {
            LoggingManager.logBlockchainOperation(
                "BLOCK_VALIDATION",
                "validate_corrupted_block",
                123L,
                2048L,
                () -> {
                    throw new IllegalArgumentException("Block validation failed");
                }
            );
        });
        
        logger.info("✅ Failed operation logging test completed");
    }
    
    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void testConcurrentOperationLogging() {
        // Test concurrent operation logging
        Thread thread1 = new Thread(() -> {
            try {
                LoggingManager.logBlockchainOperation(
                    "CONCURRENT_TEST",
                    "thread1_operation",
                    1L,
                    100L,
                    () -> Arrays.asList("thread1_result")
                );
            } catch (Exception e) {
                logger.error("Thread1 error", e);
            }
        });
        
        Thread thread2 = new Thread(() -> {
            try {
                LoggingManager.logCryptoOperation(
                    "DECRYPT",
                    "AES-256-GCM",
                    200L,
                    () -> "thread2_decrypted_data"
                );
            } catch (Exception e) {
                logger.error("Thread2 error", e);
            }
        });
        
        thread1.start();
        thread2.start();
        
        try {
            thread1.join(5000);
            thread2.join(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        logger.info("✅ Concurrent operation logging test completed");
    }
    
    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void testSystemReportGeneration() {
        // Perform various operations to populate the report
        LoggingManager.logBlockchainOperation("REPORT_TEST", "block_creation", 1L, 1024L, () -> "block_data");
        LoggingManager.logCryptoOperation("ENCRYPT", "AES-256", 512L, () -> "encrypted_data");
        LoggingManager.logSearchOperation("KEYWORD", "test query", () -> Arrays.asList("result1", "result2"));
        LoggingManager.logKeyOperation("ROTATE", "EC_256", "key_456", () -> "rotated_key");
        
        // Generate system report
        String report = LoggingManager.getSystemReport();
        
        assertNotNull(report, "System report should not be null");
        assertTrue(report.contains("COMPREHENSIVE SYSTEM LOGGING REPORT"), "Report should contain header");
        assertTrue(report.contains("SYSTEM HEALTH"), "Report should contain health section");
        assertTrue(report.contains("Memory Usage"), "Report should contain memory information");
        
        logger.info("📊 System report:\n{}", report);
        logger.info("✅ System report generation test completed");
    }
    
    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void testSensitiveDataSanitization() {
        // Test that sensitive data is properly sanitized
        List<String> result = LoggingManager.logSearchOperation(
            "SENSITIVE_SEARCH",
            "password=secret123 AND key=privatekey456",
            () -> Arrays.asList("sanitized_result")
        );
        
        assertNotNull(result, "Result should not be null");
        assertEquals(1, result.size(), "Should have 1 result");
        
        logger.info("✅ Sensitive data sanitization test completed");
    }
    
    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void testPerformanceThresholdDetection() {
        // Test slow operation detection
        LoggingManager.logBlockchainOperation(
            "SLOW_OPERATION",
            "slow_block_processing",
            999L,
            10240L,
            () -> {
                try {
                    Thread.sleep(200); // Simulate slow operation
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return Arrays.asList("slow_result");
            }
        );
        
        logger.info("✅ Performance threshold detection test completed");
    }
    
    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void testDifferentDataSizes() {
        // Test different data sizes
        LoggingManager.logBlockchainOperation("SIZE_TEST", "small_data", 1L, 100L, () -> "small");
        LoggingManager.logBlockchainOperation("SIZE_TEST", "medium_data", 2L, 10240L, () -> "medium_data");
        LoggingManager.logBlockchainOperation("SIZE_TEST", "large_data", 3L, 1048576L, () -> "large_data_content");
        
        String report = LoggingManager.getSystemReport();
        assertTrue(report.contains("SIZE_TEST"), "Report should contain size test operations");
        
        logger.info("✅ Different data sizes test completed");
    }
    
    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void testMultipleOperationTypes() {
        // Test multiple operation types in sequence
        LoggingManager.logBlockchainOperation("MULTI_TEST", "blockchain_op", 1L, 1024L, () -> "blockchain_result");
        LoggingManager.logCryptoOperation("ENCRYPT", "AES-256", 512L, () -> "crypto_result");
        LoggingManager.logSearchOperation("SEMANTIC", "test query", () -> Arrays.asList("search_result"));
        LoggingManager.logKeyOperation("VALIDATE", "RSA_2048", "key_789", () -> "key_result");
        
        String report = LoggingManager.getSystemReport();
        assertTrue(report.contains("MULTI_TEST"), "Report should contain multiple operation types");
        
        logger.info("✅ Multiple operation types test completed");
    }
}