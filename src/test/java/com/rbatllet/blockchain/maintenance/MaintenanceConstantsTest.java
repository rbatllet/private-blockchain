package com.rbatllet.blockchain.maintenance;

import com.rbatllet.blockchain.config.MaintenanceConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MaintenanceConstants configuration values.
 *
 * <p>Verifies that all maintenance constants are within reasonable ranges
 * and properly configured for production use.
 */
@DisplayName("MaintenanceConstants Tests")
class MaintenanceConstantsTest {

    @Test
    @DisplayName("Database size thresholds are properly configured")
    void testDatabaseSizeThresholds() {
        // Warning threshold should be 75% of max
        long expectedWarning = (long) (MaintenanceConstants.DATABASE_SIZE_MAX_RECOMMENDED_BYTES * 0.75);
        assertEquals(expectedWarning, MaintenanceConstants.DATABASE_SIZE_WARNING_THRESHOLD_BYTES,
            "Warning threshold should be 75% of max recommended");

        // Critical threshold should be 90% of max
        long expectedCritical = (long) (MaintenanceConstants.DATABASE_SIZE_MAX_RECOMMENDED_BYTES * 0.9);
        assertEquals(expectedCritical, MaintenanceConstants.DATABASE_SIZE_CRITICAL_THRESHOLD_BYTES,
            "Critical threshold should be 90% of max recommended");

        // All thresholds should be positive
        assertTrue(MaintenanceConstants.DATABASE_SIZE_WARNING_THRESHOLD_BYTES > 0,
            "Warning threshold must be positive");
        assertTrue(MaintenanceConstants.DATABASE_SIZE_CRITICAL_THRESHOLD_BYTES > 0,
            "Critical threshold must be positive");
        assertTrue(MaintenanceConstants.DATABASE_SIZE_MAX_RECOMMENDED_BYTES > 0,
            "Max recommended size must be positive");

        // Thresholds should be in ascending order
        assertTrue(MaintenanceConstants.DATABASE_SIZE_WARNING_THRESHOLD_BYTES <
            MaintenanceConstants.DATABASE_SIZE_CRITICAL_THRESHOLD_BYTES,
            "Warning threshold must be less than critical threshold");
        assertTrue(MaintenanceConstants.DATABASE_SIZE_CRITICAL_THRESHOLD_BYTES <
            MaintenanceConstants.DATABASE_SIZE_MAX_RECOMMENDED_BYTES,
            "Critical threshold must be less than max recommended");
    }

    @Test
    @DisplayName("Monitoring interval is reasonable")
    void testMonitoringInterval() {
        // Should be at least 5 minutes
        long minInterval = 5 * 60 * 1000L;
        assertTrue(MaintenanceConstants.DATABASE_SIZE_CHECK_INTERVAL_MS >= minInterval,
            "Monitoring interval should be at least 5 minutes");

        // Should be at most 24 hours
        long maxInterval = 24 * 60 * 60 * 1000L;
        assertTrue(MaintenanceConstants.DATABASE_SIZE_CHECK_INTERVAL_MS <= maxInterval,
            "Monitoring interval should be at most 24 hours");
    }

    @Test
    @DisplayName("VACUUM configuration is reasonable")
    void testVacuumConfiguration() {
        // Minimum interval should be at least 1 day
        long minInterval = 24 * 60 * 60 * 1000L;
        assertTrue(MaintenanceConstants.VACUUM_MIN_INTERVAL_MS >= minInterval,
            "VACUUM minimum interval should be at least 1 day");

        // Timeout should be at least 30 minutes
        long minTimeout = 30 * 60 * 1000L;
        assertTrue(MaintenanceConstants.VACUUM_TIMEOUT_MS >= minTimeout,
            "VACUUM timeout should be at least 30 minutes");

        // Timeout should be at most 24 hours
        long maxTimeout = 24 * 60 * 60 * 1000L;
        assertTrue(MaintenanceConstants.VACUUM_TIMEOUT_MS <= maxTimeout,
            "VACUUM timeout should be at most 24 hours");

        // Cron expression should be valid format
        assertNotNull(MaintenanceConstants.VACUUM_SCHEDULE_CRON,
            "VACUUM schedule cron must not be null");
        assertFalse(MaintenanceConstants.VACUUM_SCHEDULE_CRON.trim().isEmpty(),
            "VACUUM schedule cron must not be empty");
    }

    @Test
    @DisplayName("Cleanup configuration is reasonable")
    void testCleanupConfiguration() {
        // File compression age should be reasonable (between 7 and 365 days)
        assertTrue(MaintenanceConstants.FILE_COMPRESSION_AGE_DAYS >= 7,
            "File compression age should be at least 7 days");
        assertTrue(MaintenanceConstants.FILE_COMPRESSION_AGE_DAYS <= 365,
            "File compression age should be at most 365 days");

        // Max orphaned files per cleanup should be reasonable
        assertTrue(MaintenanceConstants.MAX_ORPHANED_FILES_PER_CLEANUP >= 100,
            "Max orphaned files should be at least 100");
        assertTrue(MaintenanceConstants.MAX_ORPHANED_FILES_PER_CLEANUP <= 100000,
            "Max orphaned files should be at most 100000");

        // Batch size should be reasonable
        assertTrue(MaintenanceConstants.ORPHANED_FILE_DETECTION_BATCH_SIZE >= 100,
            "Batch size should be at least 100");
        assertTrue(MaintenanceConstants.ORPHANED_FILE_DETECTION_BATCH_SIZE <= 50000,
            "Batch size should be at most 50000");

        // Minimum free disk space should be positive
        assertTrue(MaintenanceConstants.MIN_FREE_DISK_SPACE_BYTES > 0,
            "Minimum free disk space must be positive");

        // Cleanup schedule cron should be valid
        assertNotNull(MaintenanceConstants.CLEANUP_SCHEDULE_CRON,
            "Cleanup schedule cron must not be null");
        assertFalse(MaintenanceConstants.CLEANUP_SCHEDULE_CRON.trim().isEmpty(),
            "Cleanup schedule cron must not be empty");
    }

    @Test
    @DisplayName("Performance thresholds are reasonable")
    void testPerformanceThresholds() {
        // Warning threshold should be reasonable (between 1 minute and 2 hours)
        long minThreshold = 60 * 1000L;
        long maxThreshold = 2 * 60 * 60 * 1000L;
        assertTrue(MaintenanceConstants.MAINTENANCE_OPERATION_WARNING_THRESHOLD_MS >= minThreshold,
            "Warning threshold should be at least 1 minute");
        assertTrue(MaintenanceConstants.MAINTENANCE_OPERATION_WARNING_THRESHOLD_MS <= maxThreshold,
            "Warning threshold should be at most 2 hours");

        // Retry delay should be reasonable
        assertTrue(MaintenanceConstants.MAINTENANCE_RETRY_DELAY_MS >= 60000,
            "Retry delay should be at least 1 minute");
        assertTrue(MaintenanceConstants.MAINTENANCE_RETRY_DELAY_MS <= 3600000,
            "Retry delay should be at most 1 hour");

        // Max retries should be reasonable
        assertTrue(MaintenanceConstants.MAINTENANCE_MAX_RETRIES >= 1,
            "Max retries should be at least 1");
        assertTrue(MaintenanceConstants.MAINTENANCE_MAX_RETRIES <= 10,
            "Max retries should be at most 10");

        // Thread pool size should be positive
        assertTrue(MaintenanceConstants.CLEANUP_THREAD_POOL_SIZE > 0,
            "Thread pool size must be positive");
        assertTrue(MaintenanceConstants.CLEANUP_THREAD_POOL_SIZE <= 10,
            "Thread pool size should be at most 10 for I/O-bound tasks");
    }

    @Test
    @DisplayName("Logging configuration is valid")
    void testLoggingConfiguration() {
        // Progress log interval should be reasonable
        assertTrue(MaintenanceConstants.MAINTENANCE_PROGRESS_LOG_INTERVAL >= 10,
            "Progress log interval should be at least 10");
        assertTrue(MaintenanceConstants.MAINTENANCE_PROGRESS_LOG_INTERVAL <= 10000,
            "Progress log interval should be at most 10000");

        // Alert flag should be defined (true or false)
        assertNotNull(MaintenanceConstants.ENABLE_MAINTENANCE_ALERTS,
            "Maintenance alerts flag must be defined");

        // Detailed reports flag should be defined
        assertNotNull(MaintenanceConstants.ENABLE_DETAILED_REPORTS,
            "Detailed reports flag must be defined");
    }

    @Test
    @DisplayName("Cannot instantiate MaintenanceConstants")
    void testCannotInstantiate() {
        // MaintenanceConstants should have private constructor
        assertThrows(Exception.class, () -> {
            // Use reflection to try to instantiate (should fail)
            java.lang.reflect.Constructor<?> constructor =
                MaintenanceConstants.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
        }, "Should not be able to instantiate MaintenanceConstants");
    }

    @Test
    @DisplayName("Database size values are in expected ranges for production")
    void testProductionDatabaseSizeRanges() {
        // Max recommended should be at least 1 GB for production use
        long minProdSize = 1_000_000_000L; // 1 GB
        assertTrue(MaintenanceConstants.DATABASE_SIZE_MAX_RECOMMENDED_BYTES >= minProdSize,
            "Max recommended size should be at least 1 GB for production");

        // Should be at most 100 GB (reasonable upper limit)
        long maxProdSize = 100_000_000_000L; // 100 GB
        assertTrue(MaintenanceConstants.DATABASE_SIZE_MAX_RECOMMENDED_BYTES <= maxProdSize,
            "Max recommended size should be at most 100 GB");
    }

    @Test
    @DisplayName("Time intervals are consistent")
    void testTimeIntervalConsistency() {
        // VACUUM minimum interval should be much longer than monitoring interval
        assertTrue(MaintenanceConstants.VACUUM_MIN_INTERVAL_MS >
            MaintenanceConstants.DATABASE_SIZE_CHECK_INTERVAL_MS * 10,
            "VACUUM interval should be much longer than monitoring interval");

        // VACUUM timeout should be much shorter than minimum interval
        assertTrue(MaintenanceConstants.VACUUM_TIMEOUT_MS <
            MaintenanceConstants.VACUUM_MIN_INTERVAL_MS / 2,
            "VACUUM timeout should be shorter than half the minimum interval");
    }
}
