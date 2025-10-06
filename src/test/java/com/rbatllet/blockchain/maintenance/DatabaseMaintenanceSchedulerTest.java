package com.rbatllet.blockchain.maintenance;

import com.rbatllet.blockchain.util.JPAUtil;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DatabaseMaintenanceScheduler.
 *
 * <p>These tests verify scheduler coordination and lifecycle management.
 */
@DisplayName("DatabaseMaintenanceScheduler Tests")
class DatabaseMaintenanceSchedulerTest {

    private DatabaseMaintenanceScheduler scheduler;

    @BeforeAll
    static void initializeDatabase() {
        // Initialize JPAUtil with default configuration (respects environment variables)
        JPAUtil.initializeDefault();
    }

    @BeforeEach
    void setUp() {
        scheduler = new DatabaseMaintenanceScheduler();
    }

    @AfterEach
    void tearDown() {
        if (scheduler != null && scheduler.isRunning()) {
            scheduler.stop();
        }
        JPAUtil.closeEntityManager();
    }

    @Test
    @DisplayName("Scheduler initializes successfully")
    void testInitialization() {
        assertNotNull(scheduler, "Scheduler should initialize");
        assertFalse(scheduler.isRunning(), "Scheduler should not be running initially");
    }

    @Test
    @DisplayName("Scheduler can start and stop")
    void testStartStop() {
        // Start scheduler
        boolean started = scheduler.start();
        assertTrue(started, "Scheduler should start successfully");
        assertTrue(scheduler.isRunning(), "Scheduler should be running after start");

        // Stop scheduler
        boolean stopped = scheduler.stop();
        assertTrue(stopped, "Scheduler should stop successfully");
        assertFalse(scheduler.isRunning(), "Scheduler should not be running after stop");
    }

    @Test
    @DisplayName("Cannot start scheduler twice")
    void testCannotStartTwice() {
        boolean firstStart = scheduler.start();
        assertTrue(firstStart, "First start should succeed");

        boolean secondStart = scheduler.start();
        assertFalse(secondStart, "Second start should fail (already running)");

        scheduler.stop();
    }

    @Test
    @DisplayName("Cannot stop scheduler twice")
    void testCannotStopTwice() {
        scheduler.start();

        boolean firstStop = scheduler.stop();
        assertTrue(firstStop, "First stop should succeed");

        boolean secondStop = scheduler.stop();
        assertFalse(secondStop, "Second stop should fail (not running)");
    }

    @Test
    @DisplayName("Manual maintenance tasks can be run")
    void testRunAllMaintenanceTasks() {
        // Should be able to run manual maintenance without starting scheduler
        assertDoesNotThrow(() -> {
            boolean result = scheduler.runAllMaintenanceTasks();
            // Result may be false on H2 in-memory (no file for size check),
            // but should not throw exception
            assertNotNull(result, "Result should not be null");
        }, "Manual maintenance should not throw exception");
    }

    @Test
    @DisplayName("Statistics are available")
    void testStatistics() {
        String stats = scheduler.getStatistics();

        assertNotNull(stats, "Statistics should not be null");
        assertTrue(stats.contains("Maintenance Scheduler Statistics"),
            "Statistics should have header");
        assertTrue(stats.contains("Status"),
            "Statistics should show status");
        assertTrue(stats.contains("Total Maintenance Cycles"),
            "Statistics should show total cycles");
    }

    @Test
    @DisplayName("Uptime is tracked correctly")
    void testUptime() throws InterruptedException {
        // Initially no uptime
        Duration uptime = scheduler.getUptime();
        assertNull(uptime, "Uptime should be null before starting");

        // Start scheduler
        scheduler.start();

        // Wait a bit
        Thread.sleep(100);

        // Should have uptime now
        uptime = scheduler.getUptime();
        assertNotNull(uptime, "Uptime should not be null after starting");
        assertTrue(uptime.toMillis() >= 100,
            "Uptime should be at least 100ms");

        scheduler.stop();
    }

    @Test
    @DisplayName("Component references are accessible")
    void testComponentReferences() {
        assertNotNull(scheduler.getSizeMonitor(),
            "Size monitor should be accessible");
        assertNotNull(scheduler.getVacuumService(),
            "Vacuum service should be accessible");
        assertNotNull(scheduler.getCleanupService(),
            "Cleanup service should be accessible");
    }

    @Nested
    @DisplayName("Configuration Tests")
    class ConfigurationTests {

        @Test
        @DisplayName("Can configure size monitoring interval")
        void testSetSizeMonitoringInterval() {
            assertDoesNotThrow(() -> {
                scheduler.setSizeMonitoringInterval(30, TimeUnit.MINUTES);
            }, "Should be able to set size monitoring interval");
        }

        @Test
        @DisplayName("Can configure VACUUM schedule")
        void testSetVacuumSchedule() {
            assertDoesNotThrow(() -> {
                scheduler.setVacuumSchedule(3, TimeUnit.DAYS);
            }, "Should be able to set VACUUM schedule");
        }

        @Test
        @DisplayName("Can configure cleanup schedule")
        void testSetCleanupSchedule() {
            assertDoesNotThrow(() -> {
                scheduler.setCleanupSchedule(12, TimeUnit.HOURS);
            }, "Should be able to set cleanup schedule");
        }

        @Test
        @DisplayName("Cannot change configuration while running")
        void testCannotChangeConfigWhileRunning() {
            scheduler.start();

            assertThrows(IllegalStateException.class, () -> {
                scheduler.setSizeMonitoringInterval(1, TimeUnit.HOURS);
            }, "Should not be able to change interval while running");

            scheduler.stop();
        }

        @Test
        @DisplayName("Rejects invalid intervals")
        void testRejectsInvalidIntervals() {
            assertThrows(IllegalArgumentException.class, () -> {
                scheduler.setSizeMonitoringInterval(0, TimeUnit.HOURS);
            }, "Should reject zero interval");

            assertThrows(IllegalArgumentException.class, () -> {
                scheduler.setSizeMonitoringInterval(-1, TimeUnit.HOURS);
            }, "Should reject negative interval");
        }
    }

    @Nested
    @DisplayName("Statistics Tests")
    class StatisticsTests {

        @Test
        @DisplayName("Success rate is tracked")
        void testSuccessRate() {
            // Initially 0%
            assertEquals(0.0, scheduler.getSuccessRate(), 0.01,
                "Success rate should be 0% initially");

            // Run manual maintenance
            scheduler.runAllMaintenanceTasks();

            // Success rate should be updated
            double successRate = scheduler.getSuccessRate();
            assertTrue(successRate >= 0.0 && successRate <= 100.0,
                "Success rate should be between 0% and 100%");
        }

        @Test
        @DisplayName("Time since last maintenance is tracked")
        void testTimeSinceLastMaintenance() throws InterruptedException {
            // Initially null
            Duration timeSince = scheduler.getTimeSinceLastMaintenance();
            assertNull(timeSince, "Time since last maintenance should be null initially");

            // Run maintenance
            scheduler.runAllMaintenanceTasks();

            // Wait a bit
            Thread.sleep(50);

            // Should have time now
            timeSince = scheduler.getTimeSinceLastMaintenance();
            assertNotNull(timeSince, "Time since last maintenance should not be null");
            assertTrue(timeSince.toMillis() >= 50,
                "Time since last maintenance should be at least 50ms");
        }

        @Test
        @DisplayName("Statistics report includes scheduler status")
        void testStatisticsReport() {
            scheduler.start();

            String stats = scheduler.getStatistics();

            assertTrue(stats.contains("RUNNING"),
                "Statistics should show RUNNING status");
            assertTrue(stats.contains("Scheduled Tasks"),
                "Statistics should show scheduled tasks");

            scheduler.stop();
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
        @DisplayName("Scheduler handles database errors gracefully")
        void testDatabaseErrors() {
            // Shutdown database
            JPAUtil.shutdown();

            // Scheduler should still be created (services handle errors internally)
            assertDoesNotThrow(() -> {
                DatabaseMaintenanceScheduler newScheduler = new DatabaseMaintenanceScheduler();
                assertNotNull(newScheduler, "Scheduler should be created despite database issues");
            }, "Scheduler creation should handle database errors");
        }

        @Test
        @DisplayName("Manual tasks handle errors gracefully")
        void testManualTasksWithErrors() {
            // Shutdown database
            JPAUtil.shutdown();

            // Manual tasks should not throw
            assertDoesNotThrow(() -> {
                boolean result = scheduler.runAllMaintenanceTasks();
                assertFalse(result, "Manual tasks should fail gracefully without database");
            }, "Manual tasks should handle errors gracefully");
        }
    }

    @Nested
    @DisplayName("Lifecycle Tests")
    class LifecycleTests {

        @Test
        @DisplayName("Scheduler lifecycle is properly managed")
        void testLifecycleManagement() {
            // Start
            scheduler.start();
            assertTrue(scheduler.isRunning(), "Should be running");

            // Stop
            scheduler.stop();
            assertFalse(scheduler.isRunning(), "Should be stopped");

            // Note: Scheduler cannot be restarted after stop() because
            // the thread pool is shut down. This is by design to prevent
            // resource leaks. Create a new scheduler instance to restart.
        }

        @Test
        @DisplayName("Scheduler stops gracefully under load")
        void testGracefulShutdown() throws InterruptedException {
            scheduler.start();

            // Start a background thread doing manual maintenance
            Thread workThread = new Thread(() -> {
                for (int i = 0; i < 5; i++) {
                    scheduler.runAllMaintenanceTasks();
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            });
            workThread.start();

            // Give it time to start
            Thread.sleep(100);

            // Stop scheduler (should wait for tasks)
            boolean stopped = scheduler.stop();
            assertTrue(stopped, "Scheduler should stop gracefully");

            // Wait for work thread
            workThread.join(2000);
        }
    }
}
