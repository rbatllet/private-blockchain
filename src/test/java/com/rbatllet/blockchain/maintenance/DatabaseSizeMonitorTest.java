package com.rbatllet.blockchain.maintenance;

import com.rbatllet.blockchain.config.DatabaseConfig;
import com.rbatllet.blockchain.util.JPAUtil;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DatabaseSizeMonitor.
 *
 * <p>These tests verify database size monitoring and alerting functionality.
 */
@DisplayName("DatabaseSizeMonitor Tests")
class DatabaseSizeMonitorTest {

    private DatabaseSizeMonitor sizeMonitor;

    @BeforeAll
    static void initializeDatabase() {
        // Initialize JPAUtil with default configuration (respects environment variables)
        JPAUtil.initializeDefault();
    }

    @BeforeEach
    void setUp() {
        sizeMonitor = new DatabaseSizeMonitor();
    }

    @AfterEach
    void tearDown() {
        JPAUtil.closeEntityManager();
    }

    @Test
    @DisplayName("Service initializes successfully")
    void testInitialization() {
        assertNotNull(sizeMonitor, "SizeMonitor should initialize");
    }

    @Test
    @DisplayName("Can check database size")
    void testCheckDatabaseSize() {
        long sizeBytes = sizeMonitor.checkDatabaseSize();

        // Should return valid size or -1 if unavailable
        assertTrue(sizeBytes >= -1,
            "Database size should be valid or -1 if unavailable");
    }

    @Test
    @DisplayName("Growth rate is zero initially")
    void testInitialGrowthRate() {
        double growthRate = sizeMonitor.getGrowthRate();
        assertEquals(0.0, growthRate, 0.01,
            "Initial growth rate should be 0%");
    }

    @Test
    @DisplayName("Size report is generated")
    void testSizeReport() {
        String report = sizeMonitor.getSizeReport();

        assertNotNull(report, "Size report should not be null");
        assertTrue(report.contains("Database Size Report"),
            "Report should have header");
        assertTrue(report.contains("Database Type"),
            "Report should show database type");
        assertTrue(report.contains("Current Size"),
            "Report should show current size");
        assertTrue(report.contains("Utilization"),
            "Report should show utilization percentage");
    }

    @Nested
    @DisplayName("Statistics Tests")
    class StatisticsTests {

        @Test
        @DisplayName("getCurrentSizeMB handles missing database gracefully")
        void testGetCurrentSizeMB() {
            DatabaseConfig config = JPAUtil.getCurrentConfig();
            long sizeMB = sizeMonitor.getCurrentSizeMB();

            // H2 in-memory returns -1, other databases return actual size (0 or more)
            if (config != null && config.getDatabaseType() == DatabaseConfig.DatabaseType.H2 && 
                config.getDatabaseUrl() != null && config.getDatabaseUrl().contains(":mem:")) {
                assertEquals(-1, sizeMB,
                    "H2 in-memory should return -1 MB");
            } else {
                // SQLite, H2 file-based, etc. return actual size (0 or more)
                assertTrue(sizeMB >= 0,
                    "File-based databases should return size >= 0 MB");
            }
        }

        @Test
        @DisplayName("Size report includes all required sections")
        void testSizeReportSections() {
            String report = sizeMonitor.getSizeReport();

            assertTrue(report.contains("Warning Threshold"),
                "Report should show warning threshold");
            assertTrue(report.contains("Critical Threshold"),
                "Report should show critical threshold");
            
            // Note: "Scheduled Tasks" is part of DatabaseMaintenanceScheduler report,
            // not DatabaseSizeMonitor. Removed this assertion.
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @AfterEach
        void reinitializeJPAUtil() {
            // Reinitialize JPAUtil after tests that intentionally shut it down
            JPAUtil.initializeDefault();
        }

        @Test
        @DisplayName("Service handles null database configuration gracefully")
        void testNullConfigurationHandling() {
            // Shutdown JPAUtil to simulate missing configuration
            JPAUtil.shutdown();

            DatabaseSizeMonitor newMonitor = new DatabaseSizeMonitor();
            long size = newMonitor.checkDatabaseSize();

            assertEquals(-1, size,
                "Size check should return -1 with null configuration");
        }

        @Test
        @DisplayName("Service handles exceptions gracefully")
        void testExceptionHandling() {
            // Force an error by shutting down database
            JPAUtil.shutdown();

            // Should not throw exception
            assertDoesNotThrow(() -> {
                long size = sizeMonitor.checkDatabaseSize();
                assertEquals(-1, size,
                    "Should return -1 when database is unavailable");
            }, "checkDatabaseSize should handle exceptions gracefully");
        }

        @Test
        @DisplayName("Report generation handles errors gracefully")
        void testReportAfterErrors() {
            // Shutdown database
            JPAUtil.shutdown();

            // Report should still be generated
            assertDoesNotThrow(() -> {
                String report = sizeMonitor.getSizeReport();
                assertNotNull(report, "Report should be available even after errors");
            }, "getSizeReport should not throw after database shutdown");
        }
    }

    @Nested
    @DisplayName("Concurrency Tests")
    class ConcurrencyTests {

        @Test
        @DisplayName("Multiple threads can access statistics safely")
        void testConcurrentAccess() throws InterruptedException {
            // Perform one check to initialize data
            sizeMonitor.checkDatabaseSize();

            // Start multiple threads
            Thread[] threads = new Thread[10];
            for (int i = 0; i < threads.length; i++) {
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < 100; j++) {
                        sizeMonitor.getGrowthRate();
                        sizeMonitor.getCurrentSizeMB();
                    }
                });
                threads[i].start();
            }

            // Wait for all threads
            for (Thread thread : threads) {
                thread.join(5000);
                assertFalse(thread.isAlive(), "Thread should complete within timeout");
            }
        }

        @Test
        @DisplayName("Report generation is thread-safe")
        void testConcurrentReportGeneration() throws InterruptedException {
            // Start multiple threads generating reports
            Thread[] threads = new Thread[5];
            for (int i = 0; i < threads.length; i++) {
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < 50; j++) {
                        String report = sizeMonitor.getSizeReport();
                        assertNotNull(report, "Report should not be null");
                    }
                });
                threads[i].start();
            }

            // Wait for all threads
            for (Thread thread : threads) {
                thread.join(5000);
                assertFalse(thread.isAlive(), "Thread should complete within timeout");
            }
        }
    }
}
