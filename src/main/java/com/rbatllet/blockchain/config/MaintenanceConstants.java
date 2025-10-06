package com.rbatllet.blockchain.config;

/**
 * Centralized constants for database and file system maintenance operations.
 *
 * <p>These constants define thresholds, limits, and default values for:
 * <ul>
 *   <li>Database size monitoring and alerts</li>
 *   <li>VACUUM/OPTIMIZE scheduling</li>
 *   <li>Off-chain file cleanup operations</li>
 *   <li>Performance thresholds</li>
 * </ul>
 *
 * <p><strong>Thread Safety:</strong> All fields are static final and immutable.
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * if (databaseSizeBytes > MaintenanceConstants.DATABASE_SIZE_WARNING_THRESHOLD_BYTES) {
 *     logger.warn("Database size exceeds warning threshold");
 * }
 * }</pre>
 *
 * @since 1.0.5
 * @see com.rbatllet.blockchain.maintenance.DatabaseMaintenanceScheduler
 * @see com.rbatllet.blockchain.maintenance.DatabaseSizeMonitor
 */
public final class MaintenanceConstants {

    // Prevent instantiation
    private MaintenanceConstants() {
        throw new AssertionError("Cannot instantiate MaintenanceConstants");
    }

    // ========================================================================
    // DATABASE SIZE MONITORING
    // ========================================================================

    /**
     * Warning threshold for database size (75% of max recommended).
     * Alert will be triggered when database exceeds this size.
     *
     * <p>Default: 7.5 GB (suitable for typical production deployments)
     */
    public static final long DATABASE_SIZE_WARNING_THRESHOLD_BYTES = 7_500_000_000L; // 7.5 GB

    /**
     * Critical threshold for database size (90% of max recommended).
     * Critical alert will be triggered when database exceeds this size.
     *
     * <p>Default: 9 GB (requires immediate attention)
     */
    public static final long DATABASE_SIZE_CRITICAL_THRESHOLD_BYTES = 9_000_000_000L; // 9 GB

    /**
     * Maximum recommended database size before requiring intervention.
     *
     * <p>Default: 10 GB (suitable for most enterprise deployments)
     * <p>Exceeding this limit may require:
     * <ul>
     *   <li>Database optimization</li>
     *   <li>Hardware upgrade</li>
     *   <li>Data archiving (if implemented)</li>
     * </ul>
     */
    public static final long DATABASE_SIZE_MAX_RECOMMENDED_BYTES = 10_000_000_000L; // 10 GB

    /**
     * Monitoring interval in milliseconds (1 hour).
     * How frequently database size is checked and reported.
     */
    public static final long DATABASE_SIZE_CHECK_INTERVAL_MS = 3_600_000L; // 1 hour

    // ========================================================================
    // DATABASE VACUUM/OPTIMIZE
    // ========================================================================

    /**
     * Default cron expression for database VACUUM/OPTIMIZE operations.
     *
     * <p>Default: {@code "0 2 * * 0"} (Every Sunday at 2:00 AM)
     * <p>Low-traffic period recommended to minimize performance impact.
     */
    public static final String VACUUM_SCHEDULE_CRON = "0 2 * * 0"; // Sunday 2 AM

    /**
     * Timeout for VACUUM/OPTIMIZE operations in milliseconds (2 hours).
     * Operations exceeding this timeout will be logged as warnings.
     *
     * <p>Note: Actual database timeout may differ based on database configuration.
     */
    public static final long VACUUM_TIMEOUT_MS = 7_200_000L; // 2 hours

    /**
     * Minimum time between consecutive VACUUM operations in milliseconds (6 days).
     * Prevents excessive vacuuming which can degrade performance.
     */
    public static final long VACUUM_MIN_INTERVAL_MS = 518_400_000L; // 6 days

    // ========================================================================
    // OFF-CHAIN FILE CLEANUP
    // ========================================================================

    /**
     * Default cron expression for off-chain file cleanup operations.
     *
     * <p>Default: {@code "0 4 * * *"} (Every day at 4:00 AM)
     * <p>Runs after VACUUM to avoid interference.
     */
    public static final String CLEANUP_SCHEDULE_CRON = "0 4 * * *"; // Daily 4 AM

    /**
     * Age threshold for file compression in days.
     * Files older than this will be compressed to save disk space.
     *
     * <p>Default: 90 days (3 months)
     */
    public static final int FILE_COMPRESSION_AGE_DAYS = 90;

    /**
     * Maximum number of orphaned files to delete per cleanup cycle.
     * Prevents excessive I/O operations in a single run.
     *
     * <p>Default: 1000 files
     */
    public static final int MAX_ORPHANED_FILES_PER_CLEANUP = 1000;

    /**
     * Batch size for orphaned file detection queries.
     * Trades off memory usage vs. query overhead.
     *
     * <p>Default: 5000 hashes
     */
    public static final int ORPHANED_FILE_DETECTION_BATCH_SIZE = 5000;

    /**
     * Minimum free disk space required before cleanup operations (in bytes).
     * If available space is below this threshold, cleanup is skipped.
     *
     * <p>Default: 1 GB
     */
    public static final long MIN_FREE_DISK_SPACE_BYTES = 1_000_000_000L; // 1 GB

    // ========================================================================
    // PERFORMANCE AND SAFETY
    // ========================================================================

    /**
     * Maximum duration for maintenance operations before triggering warnings (30 minutes).
     * Operations exceeding this are logged for performance analysis.
     */
    public static final long MAINTENANCE_OPERATION_WARNING_THRESHOLD_MS = 1_800_000L; // 30 min

    /**
     * Delay between retry attempts for failed maintenance operations (5 minutes).
     */
    public static final long MAINTENANCE_RETRY_DELAY_MS = 300_000L; // 5 minutes

    /**
     * Maximum number of retry attempts for failed maintenance operations.
     */
    public static final int MAINTENANCE_MAX_RETRIES = 3;

    /**
     * Thread pool size for parallel cleanup operations.
     * Should be adjusted based on available CPU cores and I/O capacity.
     *
     * <p>Default: 2 threads (conservative for I/O-bound tasks)
     */
    public static final int CLEANUP_THREAD_POOL_SIZE = 2;

    // ========================================================================
    // LOGGING AND REPORTING
    // ========================================================================

    /**
     * Interval for logging maintenance progress (every 100 operations).
     */
    public static final int MAINTENANCE_PROGRESS_LOG_INTERVAL = 100;

    /**
     * Whether to generate detailed maintenance reports.
     * Disable in production to reduce log volume.
     */
    public static final boolean ENABLE_DETAILED_REPORTS = false;

    /**
     * Whether to send alerts for maintenance operations.
     * Should be enabled in production environments.
     */
    public static final boolean ENABLE_MAINTENANCE_ALERTS = true;
}
