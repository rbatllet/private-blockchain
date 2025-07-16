package com.rbatllet.blockchain.service;

import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.util.JPAUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class MemoryManagementServiceTest {
    
    private static final Logger logger = LoggerFactory.getLogger(MemoryManagementServiceTest.class);
    
    @BeforeEach
    void setUp() {
        // Ensure service is stopped before each test
        MemoryManagementService.shutdown();
    }
    
    @AfterEach
    void tearDown() {
        // Clean up after each test
        MemoryManagementService.shutdown();
        CryptoUtil.forceCleanupKeyStore();
        JPAUtil.forceCleanupAllThreadLocals();
    }
    
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testMemoryStatsRetrieval() {
        // Test memory stats retrieval
        MemoryManagementService.MemoryStats stats = MemoryManagementService.getMemoryStats();
        
        assertNotNull(stats, "Memory stats should not be null");
        assertTrue(stats.getUsedMemory() > 0, "Used memory should be positive");
        assertTrue(stats.getTotalMemory() > 0, "Total memory should be positive");
        assertTrue(stats.getMaxMemory() > 0, "Max memory should be positive");
        assertTrue(stats.getUsagePercentage() >= 0, "Usage percentage should be non-negative");
        assertTrue(stats.getUsagePercentage() <= 100, "Usage percentage should not exceed 100%");
        
        logger.info("✅ Memory stats: {}", stats);
    }
    
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testForceCleanup() {
        // Create some keys to test cleanup
        for (int i = 0; i < 10; i++) {
            CryptoUtil.generateKeyPair();
        }
        
        // Get initial stats
        var keyStoreStats = CryptoUtil.getKeyStoreStats();
        int initialKeyCount = (Integer) keyStoreStats.get("totalKeys");
        
        // Perform force cleanup
        MemoryManagementService.forceCleanup();
        
        // Verify cleanup occurred
        var newKeyStoreStats = CryptoUtil.getKeyStoreStats();
        int finalKeyCount = (Integer) newKeyStoreStats.get("totalKeys");
        
        logger.info("📊 Key count before cleanup: {}, after cleanup: {}", initialKeyCount, finalKeyCount);
        assertEquals(0, finalKeyCount, "Key store should be empty after force cleanup");
    }
    
    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void testMemoryServiceStartStop() {
        // Test starting the service
        MemoryManagementService.start();
        
        var stats = MemoryManagementService.getMemoryStats();
        assertTrue(stats.isServiceRunning(), "Service should be running after start");
        
        // Test stopping the service
        MemoryManagementService.shutdown();
        
        stats = MemoryManagementService.getMemoryStats();
        assertFalse(stats.isServiceRunning(), "Service should be stopped after shutdown");
        
        logger.info("✅ Service start/stop test completed");
    }
    
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testCryptoUtilCleanup() {
        // Test CryptoUtil cleanup functionality
        
        // Create some keys
        for (int i = 0; i < 5; i++) {
            CryptoUtil.generateKeyPair();
        }
        
        var initialStats = CryptoUtil.getKeyStoreStats();
        int initialCount = (Integer) initialStats.get("totalKeys");
        assertTrue(initialCount > 0, "Should have created some keys");
        
        // Test cleanup
        CryptoUtil.cleanupExpiredKeys();
        
        // For this test, keys are not expired, so count should remain the same
        var afterCleanupStats = CryptoUtil.getKeyStoreStats();
        int afterCleanupCount = (Integer) afterCleanupStats.get("totalKeys");
        assertEquals(initialCount, afterCleanupCount, "Active keys should not be removed");
        
        // Test force cleanup
        CryptoUtil.forceCleanupKeyStore();
        
        var afterForceStats = CryptoUtil.getKeyStoreStats();
        int afterForceCount = (Integer) afterForceStats.get("totalKeys");
        assertEquals(0, afterForceCount, "All keys should be removed after force cleanup");
        
        logger.info("✅ CryptoUtil cleanup test completed");
    }
    
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testJPAUtilCleanup() {
        // Test JPAUtil cleanup functionality
        
        // This test mainly verifies that cleanup methods don't throw exceptions
        assertDoesNotThrow(() -> {
            JPAUtil.cleanupThreadLocals();
        }, "ThreadLocal cleanup should not throw exceptions");
        
        assertDoesNotThrow(() -> {
            JPAUtil.forceCleanupAllThreadLocals();
        }, "Force ThreadLocal cleanup should not throw exceptions");
        
        logger.info("✅ JPAUtil cleanup test completed");
    }
    
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testMemoryPressureHandling() {
        // Test behavior under simulated memory pressure
        
        // Create many keys to simulate memory usage
        for (int i = 0; i < 50; i++) {
            CryptoUtil.generateKeyPair();
        }
        
        // Get memory stats
        var stats = MemoryManagementService.getMemoryStats();
        logger.info("📊 Memory usage after creating 50 keys: {:.1f}%", stats.getUsagePercentage());
        
        // Force cleanup to handle memory pressure
        MemoryManagementService.forceCleanup();
        
        // Verify cleanup reduced key count
        var keyStoreStats = CryptoUtil.getKeyStoreStats();
        int finalKeyCount = (Integer) keyStoreStats.get("totalKeys");
        assertEquals(0, finalKeyCount, "Keys should be cleaned up under memory pressure");
        
        logger.info("✅ Memory pressure handling test completed");
    }
    
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testPeriodicGCConfiguration() {
        // Test that the periodic GC feature is properly configured
        
        // Start the service
        MemoryManagementService.start();
        
        // Get stats to verify GC timestamp is tracked
        var stats = MemoryManagementService.getMemoryStats();
        
        assertNotNull(stats, "Memory stats should not be null");
        assertTrue(stats.getLastForceGCTime() > 0, "Last force GC time should be initialized");
        assertTrue(stats.isServiceRunning(), "Service should be running");
        
        // Note: We can't test the actual GC execution in unit tests due to timing,
        // but we can verify the configuration is correct
        
        logger.info("📊 GC Configuration verified - last GC time: {}", stats.getLastForceGCTime());
        
        // Stop the service
        MemoryManagementService.shutdown();
        
        logger.info("✅ Periodic GC configuration test completed");
    }
    
    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testMemoryStatsAccuracy() {
        // Test that memory stats are reasonably accurate
        
        Runtime runtime = Runtime.getRuntime();
        long systemUsedMemory = runtime.totalMemory() - runtime.freeMemory();
        
        var stats = MemoryManagementService.getMemoryStats();
        
        // Allow some variance in memory measurements
        long variance = Math.abs(stats.getUsedMemory() - systemUsedMemory);
        long maxVariance = systemUsedMemory / 10; // 10% variance allowed
        
        assertTrue(variance <= maxVariance, 
                  String.format("Memory stats variance too high: %d vs %d (variance: %d)", 
                               stats.getUsedMemory(), systemUsedMemory, variance));
        
        logger.info("✅ Memory stats accuracy test completed");
    }
}