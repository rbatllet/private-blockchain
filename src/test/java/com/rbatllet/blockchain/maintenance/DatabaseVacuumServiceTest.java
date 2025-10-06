package com.rbatllet.blockchain.maintenance;

import com.rbatllet.blockchain.config.DatabaseConfig;
import com.rbatllet.blockchain.util.JPAUtil;
import org.junit.jupiter.api.*;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DatabaseVacuumService.
 *
 * <p>These tests verify VACUUM/OPTIMIZE operations across different database types.
 */
@DisplayName("DatabaseVacuumService Tests")
class DatabaseVacuumServiceTest {

    private DatabaseVacuumService vacuumService;

    @BeforeAll
    static void initializeDatabase() {
        // Initialize JPAUtil with default configuration (respects environment variables)
        JPAUtil.initializeDefault();
    }

    @BeforeEach
    void setUp() {
        vacuumService = new DatabaseVacuumService();
    }

    @AfterEach
    void tearDown() {
        JPAUtil.closeEntityManager();
    }

    @Test
    @DisplayName("Service initializes successfully")
    void testInitialization() {
        assertNotNull(vacuumService, "VacuumService should initialize");
    }

    @Test
    @DisplayName("VACUUM operation runs successfully")
    void testVacuum() {
        DatabaseConfig config = JPAUtil.getCurrentConfig();
        boolean result = vacuumService.forceVacuum();
        
        // H2 doesn't support VACUUM (automatic compaction), so it returns false
        // Other databases should succeed
        if (config != null && config.getDatabaseType() == DatabaseConfig.DatabaseType.H2) {
            assertFalse(result, "H2 should return false (automatic compaction, no VACUUM needed)");
        } else {
            assertTrue(result, "VACUUM should succeed for non-H2 databases");
        }
    }

    @Test
    @DisplayName("Statistics are properly tracked")
    void testStatistics() {
        DatabaseConfig config = JPAUtil.getCurrentConfig();
        boolean isH2 = config != null && config.getDatabaseType() == DatabaseConfig.DatabaseType.H2;
        
        // Initial state
        assertEquals(0.0, vacuumService.getSuccessRate(), 0.01,
            "Success rate should be 0% initially");

        // Run VACUUM
        vacuumService.forceVacuum();

        // Statistics should be updated
        // For H2, "success rate" will be 0 because VACUUM returns false (automatic compaction)
        // For other databases, success rate should be > 0
        if (isH2) {
            assertEquals(0.0, vacuumService.getSuccessRate(), 0.01,
                "H2 success rate should remain 0% (VACUUM not supported)");
        } else {
            assertTrue(vacuumService.getSuccessRate() > 0,
                "Success rate should be greater than 0 after operation");
        }

        String stats = vacuumService.getVacuumStatistics();
        assertNotNull(stats, "Statistics string should not be null");
        assertTrue(stats.contains("Total Operations"),
            "Statistics should contain operation count");
    }

    @Test
    @DisplayName("Time since last vacuum is tracked")
    void testTimeSinceLastVacuum() {
        DatabaseConfig config = JPAUtil.getCurrentConfig();
        boolean isH2 = config != null && config.getDatabaseType() == DatabaseConfig.DatabaseType.H2;
        
        // Initially, should be null
        Duration timeSince = vacuumService.getTimeSinceLastVacuum();
        assertNull(timeSince, "Time since last vacuum should be null initially");

        // Run VACUUM
        boolean success = vacuumService.forceVacuum();
        
        // H2 returns false (no VACUUM needed), other databases should succeed
        if (isH2) {
            assertFalse(success, "H2 should return false (automatic compaction)");
            // H2 doesn't update last vacuum time since operation is not performed
            timeSince = vacuumService.getTimeSinceLastVacuum();
            assertNull(timeSince, "H2 should not track vacuum time (operation not performed)");
        } else {
            assertTrue(success, "VACUUM should succeed for non-H2 databases");
            
            // Now should have a time
            timeSince = vacuumService.getTimeSinceLastVacuum();
            assertNotNull(timeSince, "Time since last vacuum should not be null after operation");

            // Should be very recent (less than 5 seconds)
            assertTrue(timeSince.getSeconds() < 5,
                "Time since last vacuum should be less than 5 seconds");
        }
    }

    @Test
    @DisplayName("Statistics report includes all required information")
    void testStatisticsReport() {
        vacuumService.forceVacuum();

        String report = vacuumService.getVacuumStatistics();

        // Verify report contains key sections
        assertTrue(report.contains("Database VACUUM Statistics"),
            "Report should have header");
        assertTrue(report.contains("Total Operations"),
            "Report should show total operations");
        assertTrue(report.contains("Successful"),
            "Report should show successful operations");
        assertTrue(report.contains("Failed"),
            "Report should show failed operations");
        assertTrue(report.contains("Success Rate"),
            "Report should show success rate");
        assertTrue(report.contains("Last Vacuum"),
            "Report should show last vacuum time");
    }

    @Test
    @DisplayName("Multiple VACUUM operations update statistics correctly")
    void testMultipleOperations() {
        DatabaseConfig config = JPAUtil.getCurrentConfig();
        boolean isH2 = config != null && config.getDatabaseType() == DatabaseConfig.DatabaseType.H2;
        
        // First operation
        boolean firstResult = vacuumService.forceVacuum();
        
        if (isH2) {
            // H2 doesn't support VACUUM, so first operation returns false
            assertFalse(firstResult, "H2 first VACUUM should return false (automatic compaction)");
            
            // Subsequent operations also return false
            boolean secondResult = vacuumService.forceVacuum();
            assertFalse(secondResult, "H2 second VACUUM should also return false");

            boolean thirdResult = vacuumService.forceVacuum();
            assertFalse(thirdResult, "H2 third VACUUM should also return false");

            // All operations "failed" (returned false) for H2
            assertEquals(0.0, vacuumService.getSuccessRate(), 0.01,
                "H2 success rate should be 0% (VACUUM not supported)");
        } else {
            // For other databases, first should succeed
            assertTrue(firstResult, "First VACUUM should succeed");

            // Subsequent operations within minimum interval are rejected
            // IMPORTANT: Use performVacuum() not forceVacuum() to test interval enforcement
            boolean secondResult = vacuumService.performVacuum();
            assertFalse(secondResult, "Second VACUUM within interval should be rejected");

            boolean thirdResult = vacuumService.performVacuum();
            assertFalse(thirdResult, "Third VACUUM within interval should be rejected");

            // Success rate should be 33.33% (1 success, 2 rejections)
            assertEquals(33.33, vacuumService.getSuccessRate(), 0.1,
                "Success rate should be ~33% with one success and two rejections");
        }
    }

    @Nested
    @DisplayName("Minimum Interval Tests")
    class MinimumIntervalTests {

        @Test
        @DisplayName("Second VACUUM within minimum interval is rejected")
        void testMinimumIntervalEnforcement() throws InterruptedException {
            DatabaseConfig config = JPAUtil.getCurrentConfig();
            boolean isH2 = config != null && config.getDatabaseType() == DatabaseConfig.DatabaseType.H2;
            
            // First VACUUM
            boolean firstResult = vacuumService.forceVacuum();
            
            if (isH2) {
                // H2 doesn't support VACUUM
                assertFalse(firstResult, "H2 first VACUUM should return false (automatic compaction)");
                
                // Second VACUUM also returns false
                boolean secondResult = vacuumService.forceVacuum();
                assertFalse(secondResult, "H2 second VACUUM should also return false");
            } else {
                // Other databases: first should succeed
                assertTrue(firstResult, "First VACUUM should succeed");

                // Immediate second VACUUM should be rejected (within 6 days)
                // IMPORTANT: Use performVacuum() not forceVacuum() to test interval enforcement
                boolean secondResult = vacuumService.performVacuum();
                assertFalse(secondResult, "Second VACUUM within minimum interval should be rejected");
            }
        }

        @Test
        @DisplayName("Statistics correctly track rejected operations")
        void testRejectedOperationsStatistics() {
            DatabaseConfig config = JPAUtil.getCurrentConfig();
            boolean isH2 = config != null && config.getDatabaseType() == DatabaseConfig.DatabaseType.H2;
            
            // First operation (forceVacuum to ensure it runs regardless of previous tests)
            vacuumService.forceVacuum();

            // Immediate second operation (will be rejected because we use performVacuum())
            // IMPORTANT: Use performVacuum() not forceVacuum() to test interval enforcement
            vacuumService.performVacuum();

            if (isH2) {
                // H2 doesn't support VACUUM, so both operations "fail"
                assertEquals(0.0, vacuumService.getSuccessRate(), 0.01,
                    "H2 success rate should be 0% (VACUUM not supported)");
            } else {
                // Success rate should be 50% (1 success, 1 rejection)
                assertEquals(50.0, vacuumService.getSuccessRate(), 0.01,
                    "Success rate should be 50% with one success and one rejection");
            }
        }
    }

    @Nested
    @DisplayName("Database-Specific Tests")
    class DatabaseSpecificTests {

        @AfterEach
        void reinitializeJPAUtil() {
            // Reinitialize JPAUtil after tests that intentionally shut it down
            JPAUtil.initializeDefault();
        }

        @Test
        @DisplayName("VACUUM operation executes successfully")
        void testVacuumBehavior() {
            DatabaseConfig config = JPAUtil.getCurrentConfig();
            boolean result = vacuumService.forceVacuum();
            
            // H2 doesn't support VACUUM (automatic compaction)
            // Other databases should succeed
            if (config != null && config.getDatabaseType() == DatabaseConfig.DatabaseType.H2) {
                assertFalse(result, "H2 should return false (automatic compaction)");
            } else {
                assertTrue(result, "VACUUM should succeed for non-H2 databases");
            }
        }

        @Test
        @DisplayName("Service handles null database configuration gracefully")
        void testNullConfigurationHandling() {
            // Shutdown JPAUtil to simulate missing configuration
            JPAUtil.shutdown();

            DatabaseVacuumService newService = new DatabaseVacuumService();
            boolean result = newService.performVacuum();

            assertFalse(result, "VACUUM should fail gracefully with null configuration");
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
        @DisplayName("Service handles exceptions gracefully")
        void testExceptionHandling() {
            // Force an error by shutting down database
            JPAUtil.shutdown();

            // VACUUM should fail but not throw exception
            assertDoesNotThrow(() -> {
                boolean result = vacuumService.forceVacuum();
                assertFalse(result, "VACUUM should fail when database is unavailable");
            }, "performVacuum should handle exceptions gracefully");
        }

        @Test
        @DisplayName("Statistics remain valid after errors")
        void testStatisticsAfterErrors() {
            // Successful operation
            vacuumService.forceVacuum();

            // Force error
            JPAUtil.shutdown();
            vacuumService.forceVacuum();

            // Statistics should still be accessible
            assertDoesNotThrow(() -> {
                String stats = vacuumService.getVacuumStatistics();
                assertNotNull(stats, "Statistics should be available after errors");
            }, "getVacuumStatistics should not throw after errors");
        }
    }

    @Nested
    @DisplayName("Concurrency Tests")
    class ConcurrencyTests {

        @Test
        @DisplayName("Multiple threads can call statistics methods safely")
        void testConcurrentStatisticsAccess() throws InterruptedException {
            vacuumService.forceVacuum();

            // Start multiple threads reading statistics
            Thread[] threads = new Thread[10];
            for (int i = 0; i < threads.length; i++) {
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < 100; j++) {
                        vacuumService.getSuccessRate();
                        vacuumService.getTimeSinceLastVacuum();
                        vacuumService.getVacuumStatistics();
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
