package com.rbatllet.blockchain.maintenance;

import com.rbatllet.blockchain.config.DatabaseConfig;
import com.rbatllet.blockchain.config.DatabaseConfig.DatabaseType;
import com.rbatllet.blockchain.config.MaintenanceConstants;
import com.rbatllet.blockchain.service.PerformanceMetricsService;
import com.rbatllet.blockchain.service.PerformanceMetricsService.AlertSeverity;
import com.rbatllet.blockchain.service.AlertService;
import com.rbatllet.blockchain.util.JPAUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Monitors database size and sends alerts when thresholds are exceeded.
 *
 * <p>This service periodically checks the database size across all supported
 * database types (PostgreSQL, MySQL, SQLite, H2) and integrates with the
 * existing {@link PerformanceMetricsService} for metrics recording and alerting.
 *
 * <p><strong>Supported Databases:</strong>
 * <ul>
 *   <li>PostgreSQL: Uses {@code pg_database_size()} function</li>
 *   <li>MySQL: Queries {@code information_schema.tables}</li>
 *   <li>SQLite: Filesystem-based size detection</li>
 *   <li>H2: Filesystem-based size detection</li>
 * </ul>
 *
 * <p><strong>Thread Safety:</strong> This class is thread-safe. All mutable state
 * is protected by atomic variables and uses thread-safe services.
 *
 * <p><strong>Alert Thresholds:</strong>
 * <ul>
 *   <li>Warning: 75% of max recommended size</li>
 *   <li>Critical: 90% of max recommended size</li>
 * </ul>
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * DatabaseSizeMonitor monitor = new DatabaseSizeMonitor();
 * monitor.checkDatabaseSize(); // Manual check
 *
 * // Or use with scheduler:
 * scheduler.scheduleAtFixedRate(
 *     monitor::checkDatabaseSize,
 *     0, 1, TimeUnit.HOURS
 * );
 * }</pre>
 *
 * @since 1.0.5
 * @see PerformanceMetricsService
 * @see MaintenanceConstants
 */
public class DatabaseSizeMonitor {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseSizeMonitor.class);

    private final PerformanceMetricsService metricsService;
    private final AlertService alertService;

    // Track last alert times to prevent alert spam
    private final AtomicLong lastWarningAlertTime = new AtomicLong(0);
    private final AtomicLong lastCriticalAlertTime = new AtomicLong(0);

    // Minimum time between duplicate alerts (1 hour)
    private static final long ALERT_COOLDOWN_MS = 3_600_000L;

    // Track previous size for trend analysis
    private final AtomicLong previousSizeBytes = new AtomicLong(0);
    private final AtomicReference<LocalDateTime> lastCheckTime = new AtomicReference<>(null);

    /**
     * Creates a new DatabaseSizeMonitor with default services.
     *
     * @throws IllegalStateException if PerformanceMetricsService cannot be initialized
     */
    public DatabaseSizeMonitor() {
        this.metricsService = PerformanceMetricsService.getInstance();
        if (this.metricsService == null) {
            throw new IllegalStateException("PerformanceMetricsService not available");
        }

        try {
            this.alertService = AlertService.getInstance();
        } catch (Exception e) {
            logger.warn("AlertService not available, alerts will be disabled: {}", e.getMessage());
            throw new IllegalStateException("AlertService initialization failed", e);
        }
    }

    /**
     * Checks current database size and triggers alerts if thresholds are exceeded.
     *
     * <p>This method:
     * <ol>
     *   <li>Queries database size using database-specific method</li>
     *   <li>Records metrics via PerformanceMetricsService</li>
     *   <li>Compares against warning/critical thresholds</li>
     *   <li>Sends alerts if thresholds exceeded (with cooldown)</li>
     *   <li>Logs size trends and growth rate</li>
     * </ol>
     *
     * <p><strong>Error Handling:</strong> All exceptions are caught and logged.
     * Method never throws exceptions to prevent scheduler disruption.
     *
     * @return Current database size in bytes, or -1 if size cannot be determined
     */
    public long checkDatabaseSize() {
        long startTime = System.currentTimeMillis();

        try {
            // Get current database configuration
            DatabaseConfig config = JPAUtil.getCurrentConfig();
            if (config == null) {
                logger.error("❌ Database configuration not available");
                return -1;
            }

            // Query database size
            long sizeBytes = getDatabaseSize(config);
            if (sizeBytes < 0) {
                logger.error("❌ Failed to determine database size");
                return -1;
            }

            long sizeMB = sizeBytes / 1024 / 1024;

            // Record metrics
            metricsService.recordMemoryUsage("database_size", sizeMB);

            // Calculate growth rate if we have previous data
            long previousSize = previousSizeBytes.get();
            LocalDateTime lastCheck = lastCheckTime.get();

            if (previousSize > 0 && lastCheck != null) {
                long growthBytes = sizeBytes - previousSize;
                double growthPercent = (growthBytes * 100.0) / previousSize;

                logger.info("📊 Database size: {} MB (growth: {} MB, {:.2f}% since last check)",
                    sizeMB, growthBytes / 1024 / 1024, growthPercent);
            } else {
                logger.info("📊 Database size: {} MB", sizeMB);
            }

            // Update tracking variables
            previousSizeBytes.set(sizeBytes);
            lastCheckTime.set(LocalDateTime.now());

            // Check thresholds and send alerts
            checkThresholdsAndAlert(sizeBytes, sizeMB, config.getDatabaseType());

            // Log execution time
            long durationMs = System.currentTimeMillis() - startTime;
            metricsService.recordResponseTime("database_size_check", durationMs);

            return sizeBytes;

        } catch (Exception e) {
            logger.error("❌ Error checking database size", e);
            metricsService.recordOperation("database_size_check", false);
            return -1;
        }
    }

    /**
     * Gets database size using database-specific query or filesystem check.
     *
     * <p><strong>Implementation Notes:</strong>
     * <ul>
     *   <li>PostgreSQL: Uses {@code pg_database_size(current_database())}</li>
     *   <li>MySQL: Sums {@code data_length + index_length} from information_schema</li>
     *   <li>SQLite/H2: Reads file size from filesystem</li>
     * </ul>
     *
     * @param config Database configuration
     * @return Database size in bytes, or -1 if unable to determine
     * @throws IllegalArgumentException if config is null
     */
    private long getDatabaseSize(DatabaseConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("Database configuration cannot be null");
        }

        DatabaseType dbType = config.getDatabaseType();
        if (dbType == null) {
            logger.error("❌ Database type is null in configuration");
            return -1;
        }

        try {
            return JPAUtil.<Long>executeInTransaction(em -> {
                switch (dbType) {
                    case POSTGRESQL: {
                        Object result = em.createNativeQuery(
                            "SELECT pg_database_size(current_database())"
                        ).getSingleResult();
                        return ((Number) result).longValue();
                    }

                    case MYSQL: {
                        Object result = em.createNativeQuery(
                            "SELECT SUM(data_length + index_length) " +
                            "FROM information_schema.tables " +
                            "WHERE table_schema = DATABASE()"
                        ).getSingleResult();
                        return result != null ? ((Number) result).longValue() : -1L;
                    }

                    case SQLITE:
                    case H2: {
                        // Filesystem-based size detection
                        String url = config.getDatabaseUrl();
                        if (url == null) {
                            logger.error("❌ Database URL is null");
                            return -1L;
                        }

                        // Extract file path from JDBC URL
                        String filePath = extractFilePathFromUrl(url, dbType);
                        if (filePath == null) {
                            logger.error("❌ Could not extract file path from URL: {}", url);
                            return -1L;
                        }

                        File dbFile = new File(filePath);
                        if (!dbFile.exists()) {
                            logger.error("❌ Database file does not exist: {}", filePath);
                            return -1L;
                        }

                        return dbFile.length();
                    }

                    default:
                        logger.error("❌ Unsupported database type: {}", dbType);
                        return -1L;
                }
            });
        } catch (Exception e) {
            logger.error("❌ Error querying database size for {}", dbType, e);
            return -1;
        }
    }

    /**
     * Extracts file path from JDBC URL for filesystem-based databases.
     *
     * <p><strong>Supported URL patterns:</strong>
     * <ul>
     *   <li>SQLite: {@code jdbc:sqlite:path/to/file.db?options}</li>
     *   <li>H2: {@code jdbc:h2:file:path/to/file;options}</li>
     *   <li>H2 (mem): {@code jdbc:h2:mem:testdb} (returns null)</li>
     * </ul>
     *
     * @param url JDBC URL
     * @param dbType Database type
     * @return File path or null if URL is invalid or database is in-memory
     */
    private String extractFilePathFromUrl(String url, DatabaseType dbType) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }

        try {
            if (dbType == DatabaseType.SQLITE) {
                // jdbc:sqlite:blockchain.db?journal_mode=WAL&...
                String path = url.replace("jdbc:sqlite:", "");
                // Remove query parameters
                int queryIndex = path.indexOf('?');
                if (queryIndex > 0) {
                    path = path.substring(0, queryIndex);
                }
                return path.trim();
            } else if (dbType == DatabaseType.H2) {
                // jdbc:h2:file:path/to/file;MODE=PostgreSQL
                // jdbc:h2:mem:testdb (in-memory, no file)
                if (url.contains(":mem:")) {
                    logger.warn("⚠️ H2 in-memory database detected, cannot determine file size");
                    return null;
                }

                String path = url.replace("jdbc:h2:file:", "");
                // Remove options (separated by ;)
                int optionsIndex = path.indexOf(';');
                if (optionsIndex > 0) {
                    path = path.substring(0, optionsIndex);
                }
                // H2 creates files with .mv.db extension
                return path.trim() + ".mv.db";
            }
        } catch (Exception e) {
            logger.error("❌ Error extracting file path from URL: {}", url, e);
        }

        return null;
    }

    /**
     * Checks database size against thresholds and sends appropriate alerts.
     *
     * <p><strong>Alert Logic:</strong>
     * <ul>
     *   <li>Critical: Size >= 90% of max recommended</li>
     *   <li>Warning: Size >= 75% of max recommended</li>
     *   <li>Cooldown: 1 hour between duplicate alerts</li>
     * </ul>
     *
     * @param sizeBytes Current database size in bytes
     * @param sizeMB Current database size in MB (for logging)
     * @param dbType Database type (for context in alerts)
     */
    private void checkThresholdsAndAlert(long sizeBytes, long sizeMB, DatabaseType dbType) {
        long now = System.currentTimeMillis();

        // Critical threshold check (90%)
        if (sizeBytes >= MaintenanceConstants.DATABASE_SIZE_CRITICAL_THRESHOLD_BYTES) {
            // Check cooldown
            long lastCritical = lastCriticalAlertTime.get();
            if (now - lastCritical >= ALERT_COOLDOWN_MS) {
                double percent = (sizeBytes * 100.0) / MaintenanceConstants.DATABASE_SIZE_MAX_RECOMMENDED_BYTES;

                String message = String.format(
                    "Database size CRITICAL: %d MB (%.1f%% of max recommended). " +
                    "Database type: %s. Immediate action required.",
                    sizeMB, percent, dbType
                );

                logger.error("🚨 {}", message);

                if (MaintenanceConstants.ENABLE_MAINTENANCE_ALERTS) {
                    metricsService.createPerformanceAlert(
                        "CRITICAL_DATABASE_SIZE",
                        message,
                        AlertSeverity.CRITICAL
                    );

                    if (alertService != null) {
                        alertService.sendPerformanceAlert("database_size", 0, sizeMB);
                    }
                }

                lastCriticalAlertTime.set(now);
            }
        }
        // Warning threshold check (75%)
        else if (sizeBytes >= MaintenanceConstants.DATABASE_SIZE_WARNING_THRESHOLD_BYTES) {
            // Check cooldown
            long lastWarning = lastWarningAlertTime.get();
            if (now - lastWarning >= ALERT_COOLDOWN_MS) {
                double percent = (sizeBytes * 100.0) / MaintenanceConstants.DATABASE_SIZE_MAX_RECOMMENDED_BYTES;

                String message = String.format(
                    "Database size WARNING: %d MB (%.1f%% of max recommended). " +
                    "Database type: %s. Consider optimization or cleanup.",
                    sizeMB, percent, dbType
                );

                logger.warn("⚠️ {}", message);

                if (MaintenanceConstants.ENABLE_MAINTENANCE_ALERTS) {
                    metricsService.createPerformanceAlert(
                        "WARNING_DATABASE_SIZE",
                        message,
                        AlertSeverity.WARNING
                    );
                }

                lastWarningAlertTime.set(now);
            }
        }
    }

    /**
     * Gets the current database size in MB.
     *
     * <p>This is a convenience method that performs a size check and returns
     * the result in megabytes. Returns -1 if size cannot be determined.
     *
     * @return Database size in MB, or -1 on error
     */
    public long getCurrentSizeMB() {
        long sizeBytes = checkDatabaseSize();
        return sizeBytes >= 0 ? sizeBytes / 1024 / 1024 : -1;
    }

    /**
     * Gets the database growth rate since last check.
     *
     * @return Growth rate as percentage, or 0.0 if no previous data
     */
    public double getGrowthRate() {
        long current = previousSizeBytes.get();
        if (current <= 0) {
            return 0.0;
        }

        DatabaseConfig config = JPAUtil.getCurrentConfig();
        if (config == null) {
            return 0.0;
        }

        long newSize = getDatabaseSize(config);
        if (newSize <= 0) {
            return 0.0;
        }

        long growth = newSize - current;
        return (growth * 100.0) / current;
    }

    /**
     * Generates a detailed size report with trend analysis.
     *
     * @return Formatted size report as string
     */
    public String getSizeReport() {
        DatabaseConfig config = JPAUtil.getCurrentConfig();
        if (config == null) {
            return "Database configuration not available";
        }

        long sizeBytes = getDatabaseSize(config);
        if (sizeBytes < 0) {
            return "Database size cannot be determined";
        }

        long sizeMB = sizeBytes / 1024 / 1024;
        double percentOfMax = (sizeBytes * 100.0) / MaintenanceConstants.DATABASE_SIZE_MAX_RECOMMENDED_BYTES;

        StringBuilder report = new StringBuilder();
        report.append("📊 Database Size Report\n");
        report.append("=".repeat(50)).append("\n");
        report.append(String.format("Database Type: %s\n", config.getDatabaseType()));
        report.append(String.format("Current Size: %d MB (%d bytes)\n", sizeMB, sizeBytes));
        report.append(String.format("Utilization: %.1f%% of max recommended\n", percentOfMax));
        report.append(String.format("Warning Threshold: %d MB\n",
            MaintenanceConstants.DATABASE_SIZE_WARNING_THRESHOLD_BYTES / 1024 / 1024));
        report.append(String.format("Critical Threshold: %d MB\n",
            MaintenanceConstants.DATABASE_SIZE_CRITICAL_THRESHOLD_BYTES / 1024 / 1024));

        LocalDateTime lastCheck = lastCheckTime.get();
        if (lastCheck != null) {
            report.append(String.format("Last Check: %s\n",
                lastCheck.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        }

        long previousSize = previousSizeBytes.get();
        if (previousSize > 0) {
            long growth = sizeBytes - previousSize;
            double growthPercent = (growth * 100.0) / previousSize;
            report.append(String.format("Growth Since Last Check: %d MB (%.2f%%)\n",
                growth / 1024 / 1024, growthPercent));
        }

        report.append("=".repeat(50));

        return report.toString();
    }
}
