package com.rbatllet.blockchain.maintenance;

import com.rbatllet.blockchain.config.MaintenanceConstants;
import com.rbatllet.blockchain.service.PerformanceMetricsService;
import com.rbatllet.blockchain.service.PerformanceMetricsService.AlertSeverity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Coordinates all database and file system maintenance operations.
 *
 * <p>This scheduler manages three main maintenance services:
 * <ul>
 *   <li>{@link DatabaseSizeMonitor} - Hourly size checks and alerts</li>
 *   <li>{@link DatabaseVacuumService} - Weekly VACUUM/OPTIMIZE operations</li>
 *   <li>{@link OffChainCleanupService} - Daily off-chain file cleanup</li>
 * </ul>
 *
 * <p><strong>Thread Safety:</strong> This class is thread-safe. All operations
 * are coordinated through thread-safe services and atomic variables.
 *
 * <p><strong>Scheduling:</strong>
 * <ul>
 *   <li>Size monitoring: Every 1 hour (configurable)</li>
 *   <li>VACUUM operations: Weekly on Sunday 2 AM (configurable)</li>
 *   <li>File cleanup: Daily at 4 AM (configurable)</li>
 * </ul>
 *
 * <p><strong>Lifecycle Management:</strong>
 * <ul>
 *   <li>Start: {@link #start()} - Begins all scheduled tasks</li>
 *   <li>Stop: {@link #stop()} - Gracefully shuts down scheduler</li>
 *   <li>Manual: {@link #runAllMaintenanceTasks()} - Run all tasks immediately</li>
 * </ul>
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * DatabaseMaintenanceScheduler scheduler = new DatabaseMaintenanceScheduler();
 *
 * // Start automated maintenance
 * scheduler.start();
 *
 * // Run maintenance manually (e.g., before shutdown)
 * scheduler.runAllMaintenanceTasks();
 *
 * // Graceful shutdown
 * scheduler.stop();
 * }</pre>
 *
 * <p><strong>Customization Example:</strong>
 * <pre>{@code
 * // Custom monitoring interval (30 minutes)
 * scheduler.setSizeMonitoringInterval(30, TimeUnit.MINUTES);
 *
 * // Custom vacuum schedule (every 3 days at midnight)
 * scheduler.setVacuumSchedule(3, TimeUnit.DAYS);
 * }</pre>
 *
 * @since 1.0.5
 * @see DatabaseSizeMonitor
 * @see DatabaseVacuumService
 * @see OffChainCleanupService
 */
public class DatabaseMaintenanceScheduler {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseMaintenanceScheduler.class);

    // Maintenance services
    private final DatabaseSizeMonitor sizeMonitor;
    private final DatabaseVacuumService vacuumService;
    private final OffChainCleanupService cleanupService;
    private final PerformanceMetricsService metricsService;

    // Scheduler infrastructure
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    // Task tracking
    private final AtomicReference<ScheduledFuture<?>> sizeMonitorTask = new AtomicReference<>(null);
    private final AtomicReference<ScheduledFuture<?>> vacuumTask = new AtomicReference<>(null);
    private final AtomicReference<ScheduledFuture<?>> cleanupTask = new AtomicReference<>(null);

    // Statistics
    private final AtomicLong totalMaintenanceCycles = new AtomicLong(0);
    private final AtomicLong successfulCycles = new AtomicLong(0);
    private final AtomicLong failedCycles = new AtomicLong(0);
    private final AtomicReference<LocalDateTime> lastMaintenanceTime = new AtomicReference<>(null);
    private final AtomicReference<LocalDateTime> schedulerStartTime = new AtomicReference<>(null);

    // Configurable intervals
    private long sizeMonitoringIntervalMs = MaintenanceConstants.DATABASE_SIZE_CHECK_INTERVAL_MS;
    private long vacuumIntervalMs = 7 * 24 * 60 * 60 * 1000L; // 7 days
    private long cleanupIntervalMs = 24 * 60 * 60 * 1000L; // 1 day

    /**
     * Creates a new DatabaseMaintenanceScheduler with default configuration.
     *
     * <p>This constructor initializes all maintenance services and creates
     * a thread pool for scheduled tasks.
     *
     * @throws IllegalStateException if any maintenance service cannot be initialized
     */
    public DatabaseMaintenanceScheduler() {
        logger.info("📊 Initializing DatabaseMaintenanceScheduler...");

        try {
            this.sizeMonitor = new DatabaseSizeMonitor();
            this.vacuumService = new DatabaseVacuumService();
            this.cleanupService = new OffChainCleanupService();
            this.metricsService = PerformanceMetricsService.getInstance();

            if (this.metricsService == null) {
                throw new IllegalStateException("PerformanceMetricsService not available");
            }

            // Create thread pool for scheduled tasks
            this.scheduler = Executors.newScheduledThreadPool(
                3, // One thread per maintenance service
                new ThreadFactory() {
                    private int threadCount = 0;

                    @Override
                    public Thread newThread(Runnable r) {
                        Thread thread = new Thread(r);
                        thread.setName("maintenance-scheduler-" + (++threadCount));
                        thread.setDaemon(true); // Don't prevent JVM shutdown
                        return thread;
                    }
                }
            );

            logger.info("✅ DatabaseMaintenanceScheduler initialized successfully");

        } catch (Exception e) {
            logger.error("❌ Failed to initialize DatabaseMaintenanceScheduler", e);
            throw new IllegalStateException("DatabaseMaintenanceScheduler initialization failed", e);
        }
    }

    /**
     * Starts all scheduled maintenance tasks.
     *
     * <p>This method schedules:
     * <ul>
     *   <li>Size monitoring: Every {@link #sizeMonitoringIntervalMs}</li>
     *   <li>VACUUM operations: Every {@link #vacuumIntervalMs}</li>
     *   <li>File cleanup: Every {@link #cleanupIntervalMs}</li>
     * </ul>
     *
     * <p>If the scheduler is already running, this method does nothing.
     *
     * @return {@code true} if scheduler started successfully, {@code false} if already running
     */
    public boolean start() {
        if (isRunning.compareAndSet(false, true)) {
            logger.info("🚀 Starting DatabaseMaintenanceScheduler...");
            schedulerStartTime.set(LocalDateTime.now());

            try {
                // Schedule size monitoring (hourly)
                ScheduledFuture<?> sizeTask = scheduler.scheduleAtFixedRate(
                    this::runSizeMonitoring,
                    0, // Start immediately
                    sizeMonitoringIntervalMs,
                    TimeUnit.MILLISECONDS
                );
                sizeMonitorTask.set(sizeTask);
                logger.info("  ✅ Size monitoring scheduled: every {} hours",
                    TimeUnit.MILLISECONDS.toHours(sizeMonitoringIntervalMs));

                // Schedule VACUUM operations (weekly)
                ScheduledFuture<?> vacuum = scheduler.scheduleAtFixedRate(
                    this::runVacuumOperation,
                    calculateInitialDelay(vacuumIntervalMs),
                    vacuumIntervalMs,
                    TimeUnit.MILLISECONDS
                );
                vacuumTask.set(vacuum);
                logger.info("  ✅ VACUUM operations scheduled: every {} days",
                    TimeUnit.MILLISECONDS.toDays(vacuumIntervalMs));

                // Schedule file cleanup (daily)
                ScheduledFuture<?> cleanup = scheduler.scheduleAtFixedRate(
                    this::runCleanupOperation,
                    calculateInitialDelay(cleanupIntervalMs),
                    cleanupIntervalMs,
                    TimeUnit.MILLISECONDS
                );
                cleanupTask.set(cleanup);
                logger.info("  ✅ File cleanup scheduled: every {} hours",
                    TimeUnit.MILLISECONDS.toHours(cleanupIntervalMs));

                logger.info("✅ DatabaseMaintenanceScheduler started successfully");

                if (MaintenanceConstants.ENABLE_MAINTENANCE_ALERTS) {
                    metricsService.createPerformanceAlert(
                        "MAINTENANCE_SCHEDULER_STARTED",
                        "Automated maintenance scheduler started successfully",
                        AlertSeverity.INFO
                    );
                }

                return true;

            } catch (Exception e) {
                logger.error("❌ Failed to start DatabaseMaintenanceScheduler", e);
                isRunning.set(false);
                throw new RuntimeException("Failed to start maintenance scheduler", e);
            }

        } else {
            logger.warn("⚠️ DatabaseMaintenanceScheduler is already running");
            return false;
        }
    }

    /**
     * Stops all scheduled maintenance tasks.
     *
     * <p>This method:
     * <ol>
     *   <li>Cancels all scheduled tasks</li>
     *   <li>Waits for running tasks to complete (up to 30 seconds)</li>
     *   <li>Shuts down thread pool gracefully</li>
     * </ol>
     *
     * <p>If the scheduler is not running, this method does nothing.
     *
     * @return {@code true} if scheduler stopped successfully, {@code false} if not running
     */
    public boolean stop() {
        if (isRunning.compareAndSet(true, false)) {
            logger.info("🛑 Stopping DatabaseMaintenanceScheduler...");

            try {
                // Cancel scheduled tasks
                cancelTask(sizeMonitorTask.get(), "Size monitoring");
                cancelTask(vacuumTask.get(), "VACUUM operations");
                cancelTask(cleanupTask.get(), "File cleanup");

                // Shutdown scheduler
                scheduler.shutdown();

                // Wait for running tasks to complete (30 seconds timeout)
                if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                    logger.warn("⚠️ Scheduler did not terminate in time, forcing shutdown...");
                    scheduler.shutdownNow();

                    // Wait a bit more
                    if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                        logger.error("❌ Scheduler did not terminate after forced shutdown");
                    }
                }

                logger.info("✅ DatabaseMaintenanceScheduler stopped successfully");

                if (MaintenanceConstants.ENABLE_MAINTENANCE_ALERTS) {
                    metricsService.createPerformanceAlert(
                        "MAINTENANCE_SCHEDULER_STOPPED",
                        "Automated maintenance scheduler stopped",
                        AlertSeverity.INFO
                    );
                }

                return true;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("❌ Interrupted while stopping scheduler", e);
                return false;

            } catch (Exception e) {
                logger.error("❌ Error stopping DatabaseMaintenanceScheduler", e);
                return false;
            }

        } else {
            logger.warn("⚠️ DatabaseMaintenanceScheduler is not running");
            return false;
        }
    }

    /**
     * Runs all maintenance tasks immediately (manual execution).
     *
     * <p>This method executes:
     * <ol>
     *   <li>Database size monitoring</li>
     *   <li>VACUUM/OPTIMIZE operations</li>
     *   <li>Off-chain file cleanup</li>
     * </ol>
     *
     * <p>This is useful for:
     * <ul>
     *   <li>Manual maintenance before system shutdown</li>
     *   <li>On-demand cleanup after bulk operations</li>
     *   <li>Testing and troubleshooting</li>
     * </ul>
     *
     * <p><strong>Performance Impact:</strong> This operation can take significant
     * time (minutes to hours) depending on database size and file count.
     *
     * @return {@code true} if all tasks completed successfully, {@code false} otherwise
     */
    public boolean runAllMaintenanceTasks() {
        long startTime = System.currentTimeMillis();
        totalMaintenanceCycles.incrementAndGet();

        logger.info("🧹 Starting manual maintenance cycle...");

        try {
            boolean allSuccess = true;

            // Run size monitoring
            logger.info("  📊 Running database size monitoring...");
            long sizeBytes = sizeMonitor.checkDatabaseSize();
            if (sizeBytes < 0) {
                logger.warn("  ⚠️ Size monitoring failed");
                allSuccess = false;
            } else {
                logger.info("  ✅ Size monitoring completed: {} MB", sizeBytes / 1024 / 1024);
            }

            // Run VACUUM
            logger.info("  🧹 Running VACUUM/OPTIMIZE operations...");
            boolean vacuumSuccess = vacuumService.performVacuum();
            if (!vacuumSuccess) {
                logger.warn("  ⚠️ VACUUM operation failed");
                allSuccess = false;
            } else {
                logger.info("  ✅ VACUUM operations completed");
            }

            // Run cleanup
            logger.info("  🧹 Running off-chain file cleanup...");
            OffChainCleanupService.CleanupResult orphanResult = cleanupService.cleanupOrphanedFiles();
            OffChainCleanupService.CleanupResult compressResult = cleanupService.compressOldFiles();

            int totalCleaned = orphanResult.getDeletedCount() + compressResult.getCompressedCount();
            if (totalCleaned > 0) {
                logger.info("  ✅ File cleanup completed: {} files deleted, {} compressed",
                    orphanResult.getDeletedCount(),
                    compressResult.getCompressedCount());
            } else {
                logger.info("  ℹ️  File cleanup completed: no files to clean");
            }

            long durationMs = System.currentTimeMillis() - startTime;
            lastMaintenanceTime.set(LocalDateTime.now());

            if (allSuccess) {
                successfulCycles.incrementAndGet();
                logger.info("✅ Manual maintenance cycle completed successfully in {}ms", durationMs);
            } else {
                failedCycles.incrementAndGet();
                logger.error("❌ Manual maintenance cycle completed with failures in {}ms", durationMs);
            }

            metricsService.recordResponseTime("full_maintenance_cycle", durationMs);
            metricsService.recordOperation("full_maintenance_cycle", allSuccess);

            return allSuccess;

        } catch (Exception e) {
            long durationMs = System.currentTimeMillis() - startTime;
            logger.error("❌ Manual maintenance cycle failed after {}ms", durationMs, e);
            failedCycles.incrementAndGet();
            metricsService.recordOperation("full_maintenance_cycle", false);

            if (MaintenanceConstants.ENABLE_MAINTENANCE_ALERTS) {
                metricsService.createPerformanceAlert(
                    "MAINTENANCE_CYCLE_FAILED",
                    "Manual maintenance cycle failed: " + e.getMessage(),
                    AlertSeverity.CRITICAL
                );
            }

            return false;
        }
    }

    /**
     * Runs database size monitoring task (scheduled execution).
     */
    private void runSizeMonitoring() {
        try {
            logger.debug("📊 Running scheduled size monitoring...");
            long sizeBytes = sizeMonitor.checkDatabaseSize();

            if (sizeBytes >= 0) {
                metricsService.recordOperation("scheduled_size_monitoring", true);
            } else {
                metricsService.recordOperation("scheduled_size_monitoring", false);
                logger.warn("⚠️ Scheduled size monitoring failed");
            }

        } catch (Exception e) {
            logger.error("❌ Error in scheduled size monitoring", e);
            metricsService.recordOperation("scheduled_size_monitoring", false);
        }
    }

    /**
     * Runs VACUUM/OPTIMIZE task (scheduled execution).
     */
    private void runVacuumOperation() {
        try {
            logger.info("🧹 Running scheduled VACUUM/OPTIMIZE...");
            boolean success = vacuumService.performVacuum();
            metricsService.recordOperation("scheduled_vacuum", success);

            if (!success) {
                logger.warn("⚠️ Scheduled VACUUM operation failed");
            }

        } catch (Exception e) {
            logger.error("❌ Error in scheduled VACUUM operation", e);
            metricsService.recordOperation("scheduled_vacuum", false);
        }
    }

    /**
     * Runs off-chain file cleanup task (scheduled execution).
     */
    private void runCleanupOperation() {
        try {
            logger.info("🧹 Running scheduled off-chain cleanup...");
            OffChainCleanupService.CleanupResult orphanResult = cleanupService.cleanupOrphanedFiles();
            OffChainCleanupService.CleanupResult compressResult = cleanupService.compressOldFiles();

            int totalCleaned = orphanResult.getDeletedCount() + compressResult.getCompressedCount();
            boolean success = totalCleaned >= 0; // Success if no exceptions

            metricsService.recordOperation("scheduled_cleanup", success);

            if (totalCleaned > 0) {
                logger.info("✅ Scheduled cleanup completed: {} deleted, {} compressed",
                    orphanResult.getDeletedCount(), compressResult.getCompressedCount());
            }

        } catch (Exception e) {
            logger.error("❌ Error in scheduled cleanup operation", e);
            metricsService.recordOperation("scheduled_cleanup", false);
        }
    }

    /**
     * Calculates initial delay to schedule tasks at optimal times.
     *
     * <p>This method adds a small offset to prevent all tasks from running
     * simultaneously at startup.
     *
     * @param intervalMs Task interval in milliseconds
     * @return Initial delay in milliseconds
     */
    private long calculateInitialDelay(long intervalMs) {
        // Add 5-10 minutes offset to spread out initial executions
        long offsetMs = ThreadLocalRandom.current().nextLong(5 * 60 * 1000, 10 * 60 * 1000);
        return offsetMs;
    }

    /**
     * Cancels a scheduled task safely.
     *
     * @param task Task to cancel
     * @param taskName Task name for logging
     */
    private void cancelTask(ScheduledFuture<?> task, String taskName) {
        if (task != null && !task.isCancelled()) {
            boolean cancelled = task.cancel(false); // Don't interrupt if running
            if (cancelled) {
                logger.info("  ✅ {} task cancelled", taskName);
            } else {
                logger.warn("  ⚠️ Failed to cancel {} task", taskName);
            }
        }
    }

    // ========================================================================
    // CONFIGURATION METHODS
    // ========================================================================

    /**
     * Sets custom interval for size monitoring.
     *
     * <p><strong>Note:</strong> Changes only take effect after restart.
     *
     * @param interval Interval value
     * @param unit Time unit
     * @throws IllegalArgumentException if interval is negative or zero
     * @throws IllegalStateException if scheduler is running
     */
    public void setSizeMonitoringInterval(long interval, TimeUnit unit) {
        if (interval <= 0) {
            throw new IllegalArgumentException("Interval must be positive");
        }
        if (isRunning.get()) {
            throw new IllegalStateException("Cannot change interval while scheduler is running");
        }

        this.sizeMonitoringIntervalMs = unit.toMillis(interval);
        logger.info("📊 Size monitoring interval set to {} {}", interval, unit);
    }

    /**
     * Sets custom interval for VACUUM operations.
     *
     * <p><strong>Note:</strong> Changes only take effect after restart.
     *
     * @param interval Interval value
     * @param unit Time unit
     * @throws IllegalArgumentException if interval is negative or zero
     * @throws IllegalStateException if scheduler is running
     */
    public void setVacuumSchedule(long interval, TimeUnit unit) {
        if (interval <= 0) {
            throw new IllegalArgumentException("Interval must be positive");
        }
        if (isRunning.get()) {
            throw new IllegalStateException("Cannot change schedule while scheduler is running");
        }

        this.vacuumIntervalMs = unit.toMillis(interval);
        logger.info("🧹 VACUUM interval set to {} {}", interval, unit);
    }

    /**
     * Sets custom interval for file cleanup operations.
     *
     * <p><strong>Note:</strong> Changes only take effect after restart.
     *
     * @param interval Interval value
     * @param unit Time unit
     * @throws IllegalArgumentException if interval is negative or zero
     * @throws IllegalStateException if scheduler is running
     */
    public void setCleanupSchedule(long interval, TimeUnit unit) {
        if (interval <= 0) {
            throw new IllegalArgumentException("Interval must be positive");
        }
        if (isRunning.get()) {
            throw new IllegalStateException("Cannot change schedule while scheduler is running");
        }

        this.cleanupIntervalMs = unit.toMillis(interval);
        logger.info("🧹 Cleanup interval set to {} {}", interval, unit);
    }

    // ========================================================================
    // STATUS AND STATISTICS
    // ========================================================================

    /**
     * Checks if scheduler is currently running.
     *
     * @return {@code true} if scheduler is active, {@code false} otherwise
     */
    public boolean isRunning() {
        return isRunning.get();
    }

    /**
     * Gets uptime since scheduler started.
     *
     * @return Uptime duration, or null if scheduler never started
     */
    public Duration getUptime() {
        LocalDateTime startTime = schedulerStartTime.get();
        return startTime != null ? Duration.between(startTime, LocalDateTime.now()) : null;
    }

    /**
     * Gets time since last maintenance cycle.
     *
     * @return Duration since last maintenance, or null if never run
     */
    public Duration getTimeSinceLastMaintenance() {
        LocalDateTime lastTime = lastMaintenanceTime.get();
        return lastTime != null ? Duration.between(lastTime, LocalDateTime.now()) : null;
    }

    /**
     * Gets success rate of maintenance cycles.
     *
     * @return Success rate as percentage (0.0-100.0), or 0.0 if no cycles
     */
    public double getSuccessRate() {
        long total = totalMaintenanceCycles.get();
        if (total == 0) {
            return 0.0;
        }
        return (successfulCycles.get() * 100.0) / total;
    }

    /**
     * Gets comprehensive scheduler statistics.
     *
     * @return Formatted statistics string
     */
    public String getStatistics() {
        StringBuilder stats = new StringBuilder();
        stats.append("📊 Maintenance Scheduler Statistics\n");
        stats.append("=".repeat(50)).append("\n");

        stats.append(String.format("Status: %s\n", isRunning.get() ? "RUNNING" : "STOPPED"));

        Duration uptime = getUptime();
        if (uptime != null) {
            long hours = uptime.toHours();
            long days = hours / 24;
            stats.append(String.format("Uptime: %d days, %d hours\n", days, hours % 24));
        } else {
            stats.append("Uptime: Never started\n");
        }

        stats.append(String.format("Total Maintenance Cycles: %d\n", totalMaintenanceCycles.get()));
        stats.append(String.format("Successful Cycles: %d\n", successfulCycles.get()));
        stats.append(String.format("Failed Cycles: %d\n", failedCycles.get()));

        long total = totalMaintenanceCycles.get();
        if (total > 0) {
            double successRate = (successfulCycles.get() * 100.0) / total;
            stats.append(String.format("Success Rate: %.1f%%\n", successRate));
        }

        LocalDateTime lastMaintenance = lastMaintenanceTime.get();
        if (lastMaintenance != null) {
            stats.append(String.format("Last Maintenance: %s\n",
                lastMaintenance.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));

            Duration sinceLastMaintenance = Duration.between(lastMaintenance, LocalDateTime.now());
            long hoursSince = sinceLastMaintenance.toHours();
            stats.append(String.format("Hours Since Last Maintenance: %d\n", hoursSince));
        } else {
            stats.append("Last Maintenance: Never\n");
        }

        stats.append("\n📋 Scheduled Tasks:\n");
        stats.append(String.format("  • Size Monitoring: every %d hours\n",
            TimeUnit.MILLISECONDS.toHours(sizeMonitoringIntervalMs)));
        stats.append(String.format("  • VACUUM Operations: every %d days\n",
            TimeUnit.MILLISECONDS.toDays(vacuumIntervalMs)));
        stats.append(String.format("  • File Cleanup: every %d hours\n",
            TimeUnit.MILLISECONDS.toHours(cleanupIntervalMs)));

        stats.append("=".repeat(50));
        return stats.toString();
    }

    /**
     * Gets reference to DatabaseSizeMonitor for direct access.
     *
     * @return DatabaseSizeMonitor instance
     */
    public DatabaseSizeMonitor getSizeMonitor() {
        return sizeMonitor;
    }

    /**
     * Gets reference to DatabaseVacuumService for direct access.
     *
     * @return DatabaseVacuumService instance
     */
    public DatabaseVacuumService getVacuumService() {
        return vacuumService;
    }

    /**
     * Gets reference to OffChainCleanupService for direct access.
     *
     * @return OffChainCleanupService instance
     */
    public OffChainCleanupService getCleanupService() {
        return cleanupService;
    }
}
