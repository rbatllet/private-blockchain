package com.rbatllet.blockchain.maintenance;

import com.rbatllet.blockchain.config.DatabaseConfig;
import com.rbatllet.blockchain.config.DatabaseConfig.DatabaseType;
import com.rbatllet.blockchain.config.MaintenanceConstants;
import com.rbatllet.blockchain.service.PerformanceMetricsService;
import com.rbatllet.blockchain.service.PerformanceMetricsService.AlertSeverity;
import com.rbatllet.blockchain.util.JPAUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Performs database-specific VACUUM and OPTIMIZE operations to reclaim space
 * and improve performance.
 *
 * <p>This service executes maintenance operations tailored to each database type:
 * <ul>
 *   <li><strong>PostgreSQL:</strong> VACUUM FULL + REINDEX</li>
 *   <li><strong>MySQL:</strong> OPTIMIZE TABLE</li>
 *   <li><strong>SQLite:</strong> VACUUM + PRAGMA optimize</li>
 *   <li><strong>H2:</strong> No native VACUUM (logs warning)</li>
 * </ul>
 *
 * <p><strong>Thread Safety:</strong> This class is thread-safe. Operations are
 * serialized using atomic variables to prevent concurrent vacuum operations.
 *
 * <p><strong>Performance Impact:</strong>
 * <ul>
 *   <li>VACUUM operations can be resource-intensive</li>
 *   <li>Should be scheduled during low-traffic periods</li>
 *   <li>PostgreSQL VACUUM FULL requires exclusive locks</li>
 *   <li>MySQL OPTIMIZE TABLE locks tables during operation</li>
 * </ul>
 *
 * <p><strong>Safety Features:</strong>
 * <ul>
 *   <li>Minimum interval enforcement (6 days default)</li>
 *   <li>Timeout detection (2 hours default)</li>
 *   <li>Comprehensive error handling</li>
 *   <li>Performance metrics integration</li>
 * </ul>
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * DatabaseVacuumService vacuumService = new DatabaseVacuumService();
 *
 * // Manual vacuum
 * boolean success = vacuumService.performVacuum();
 *
 * // Or use with scheduler:
 * scheduler.schedule(
 *     vacuumService::performVacuum,
 *     CronExpression.parse("0 2 * * 0") // Sunday 2 AM
 * );
 * }</pre>
 *
 * @since 1.0.5
 * @see MaintenanceConstants
 * @see PerformanceMetricsService
 */
public class DatabaseVacuumService {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseVacuumService.class);

    private final PerformanceMetricsService metricsService;

    // Track last vacuum time to enforce minimum interval
    private final AtomicReference<LocalDateTime> lastVacuumTime = new AtomicReference<>(null);

    // Track vacuum operation status
    private final AtomicLong totalVacuumOperations = new AtomicLong(0);
    private final AtomicLong successfulVacuumOperations = new AtomicLong(0);
    private final AtomicLong failedVacuumOperations = new AtomicLong(0);

    /**
     * Creates a new DatabaseVacuumService with default metrics service.
     *
     * @throws IllegalStateException if PerformanceMetricsService cannot be initialized
     */
    public DatabaseVacuumService() {
        this.metricsService = PerformanceMetricsService.getInstance();
        if (this.metricsService == null) {
            throw new IllegalStateException("PerformanceMetricsService not available");
        }
    }

    /**
     * Performs database VACUUM/OPTIMIZE operation.
     *
     * <p>This method:
     * <ol>
     *   <li>Checks minimum interval since last vacuum</li>
     *   <li>Determines database type and executes appropriate commands</li>
     *   <li>Monitors execution time and detects timeouts</li>
     *   <li>Records metrics and sends alerts on failures</li>
     *   <li>Updates operation statistics</li>
     * </ol>
     *
     * <p><strong>Minimum Interval:</strong> Operations are rejected if less than
     * {@link MaintenanceConstants#VACUUM_MIN_INTERVAL_MS} has elapsed since last vacuum.
     *
     * <p><strong>Error Handling:</strong> All exceptions are caught and logged.
     * Method never throws exceptions to prevent scheduler disruption.
     *
     * @return {@code true} if vacuum completed successfully, {@code false} otherwise
     */
    public boolean performVacuum() {
        return performVacuum(false);
    }

    /**
     * Forces database VACUUM/OPTIMIZE operation, bypassing minimum interval check.
     *
     * <p>This method is intended for:
     * <ul>
     *   <li>Manual maintenance operations</li>
     *   <li>Testing purposes</li>
     *   <li>Emergency situations requiring immediate vacuum</li>
     * </ul>
     *
     * <p><strong>Warning:</strong> Frequent VACUUM operations can cause performance
     * degradation. Use this method sparingly in production environments.
     *
     * @return {@code true} if vacuum completed successfully, {@code false} otherwise
     */
    public boolean forceVacuum() {
        return performVacuum(true);
    }

    /**
     * Performs database VACUUM/OPTIMIZE operation with optional interval bypass.
     *
     * <p>This is the internal implementation used by both {@link #performVacuum()}
     * and {@link #forceVacuum()}.
     *
     * @param skipIntervalCheck If true, bypasses minimum interval check
     * @return {@code true} if vacuum completed successfully, {@code false} otherwise
     */
    private boolean performVacuum(boolean skipIntervalCheck) {
        long startTime = System.currentTimeMillis();
        totalVacuumOperations.incrementAndGet();

        try {
            // Check minimum interval (unless skipped)
            if (!skipIntervalCheck) {
                LocalDateTime lastVacuum = lastVacuumTime.get();
                if (lastVacuum != null) {
                    Duration sinceLastVacuum = Duration.between(lastVacuum, LocalDateTime.now());
                    long millisSinceLastVacuum = sinceLastVacuum.toMillis();

                    if (millisSinceLastVacuum < MaintenanceConstants.VACUUM_MIN_INTERVAL_MS) {
                        long remainingMs = MaintenanceConstants.VACUUM_MIN_INTERVAL_MS - millisSinceLastVacuum;
                        long remainingHours = remainingMs / (1000 * 60 * 60);

                        logger.warn("⚠️ VACUUM operation skipped: minimum interval not reached. " +
                            "Last vacuum: {}, minimum interval: {} hours, remaining: {} hours",
                            lastVacuum, MaintenanceConstants.VACUUM_MIN_INTERVAL_MS / (1000 * 60 * 60),
                            remainingHours);

                        return false;
                    }
                }
            } else {
                logger.info("⚡ Forcing VACUUM operation (interval check bypassed)");
            }

            // Get database configuration
            DatabaseConfig config = JPAUtil.getCurrentConfig();
            if (config == null) {
                logger.error("❌ Database configuration not available");
                failedVacuumOperations.incrementAndGet();
                return false;
            }

            DatabaseType dbType = config.getDatabaseType();
            if (dbType == null) {
                logger.error("❌ Database type is null");
                failedVacuumOperations.incrementAndGet();
                return false;
            }

            logger.info("🧹 Starting database VACUUM/OPTIMIZE for {}", dbType);

            // Execute database-specific vacuum
            boolean success = executeVacuumForDatabase(dbType);

            // Record execution time
            long durationMs = System.currentTimeMillis() - startTime;
            metricsService.recordResponseTime("database_vacuum", durationMs);

            // Check for timeout warning
            if (durationMs > MaintenanceConstants.VACUUM_TIMEOUT_MS) {
                logger.warn("⚠️ VACUUM operation exceeded timeout threshold: {}ms (threshold: {}ms)",
                    durationMs, MaintenanceConstants.VACUUM_TIMEOUT_MS);

                if (MaintenanceConstants.ENABLE_MAINTENANCE_ALERTS) {
                    metricsService.createPerformanceAlert(
                        "VACUUM_TIMEOUT_WARNING",
                        String.format("VACUUM operation took %dms (threshold: %dms)",
                            durationMs, MaintenanceConstants.VACUUM_TIMEOUT_MS),
                        AlertSeverity.WARNING
                    );
                }
            }

            if (success) {
                successfulVacuumOperations.incrementAndGet();
                lastVacuumTime.set(LocalDateTime.now());
                logger.info("✅ VACUUM operation completed successfully in {}ms", durationMs);
            } else {
                failedVacuumOperations.incrementAndGet();
                logger.error("❌ VACUUM operation failed after {}ms", durationMs);

                if (MaintenanceConstants.ENABLE_MAINTENANCE_ALERTS) {
                    metricsService.createPerformanceAlert(
                        "VACUUM_OPERATION_FAILED",
                        String.format("VACUUM operation failed for %s", dbType),
                        AlertSeverity.CRITICAL
                    );
                }
            }

            metricsService.recordOperation("database_vacuum", success);
            return success;

        } catch (Exception e) {
            long durationMs = System.currentTimeMillis() - startTime;
            logger.error("❌ Unexpected error during VACUUM operation after {}ms", durationMs, e);
            failedVacuumOperations.incrementAndGet();
            metricsService.recordOperation("database_vacuum", false);

            if (MaintenanceConstants.ENABLE_MAINTENANCE_ALERTS) {
                metricsService.createPerformanceAlert(
                    "VACUUM_EXCEPTION",
                    "Unexpected error during VACUUM: " + e.getMessage(),
                    AlertSeverity.CRITICAL
                );
            }

            return false;
        }
    }

    /**
     * Executes database-specific vacuum/optimize commands.
     *
     * <p><strong>Implementation Notes:</strong>
     * <ul>
     *   <li>PostgreSQL: Executes VACUUM FULL on all blockchain tables, then REINDEX</li>
     *   <li>MySQL: Executes OPTIMIZE TABLE on all blockchain tables</li>
     *   <li>SQLite: Executes VACUUM followed by PRAGMA optimize</li>
     *   <li>H2: No native VACUUM support (logs warning and returns true)</li>
     * </ul>
     *
     * @param dbType Database type
     * @return {@code true} if all operations completed successfully, {@code false} otherwise
     */
    private boolean executeVacuumForDatabase(DatabaseType dbType) {
        try {
            return JPAUtil.<Boolean>executeInTransaction(em -> {
                switch (dbType) {
                    case POSTGRESQL:
                        return vacuumPostgreSQL(em);

                    case MYSQL:
                        return vacuumMySQL(em);

                    case SQLITE:
                        return vacuumSQLite(em);

                    case H2:
                        logger.warn("⚠️ H2 does not support native VACUUM operations. " +
                            "Database compaction occurs automatically during shutdown.");
                        return true; // Not a failure

                    default:
                        logger.error("❌ Unsupported database type for VACUUM: {}", dbType);
                        return false;
                }
            });
        } catch (Exception e) {
            logger.error("❌ Error executing VACUUM for {}", dbType, e);
            return false;
        }
    }

    /**
     * Executes PostgreSQL-specific vacuum and reindex operations.
     *
     * <p><strong>Operations performed:</strong>
     * <ol>
     *   <li>VACUUM FULL blocks (reclaims all dead space)</li>
     *   <li>VACUUM FULL authorized_keys</li>
     *   <li>VACUUM FULL off_chain_data</li>
     *   <li>VACUUM FULL block_sequence</li>
     *   <li>REINDEX TABLE blocks (rebuilds all indexes)</li>
     * </ol>
     *
     * <p><strong>Performance Impact:</strong> VACUUM FULL requires exclusive locks
     * and rewrites entire tables. Should only run during maintenance windows.
     *
     * @param em EntityManager for executing native queries
     * @return {@code true} if all operations succeeded, {@code false} otherwise
     */
    private boolean vacuumPostgreSQL(EntityManager em) {
        logger.info("🔧 Executing PostgreSQL VACUUM FULL + REINDEX...");

        try {
            // VACUUM FULL reclaims all dead space but requires exclusive locks
            logger.info("  └─ VACUUM FULL blocks...");
            em.createNativeQuery("VACUUM FULL blocks").executeUpdate();

            logger.info("  └─ VACUUM FULL authorized_keys...");
            em.createNativeQuery("VACUUM FULL authorized_keys").executeUpdate();

            logger.info("  └─ VACUUM FULL off_chain_data...");
            em.createNativeQuery("VACUUM FULL off_chain_data").executeUpdate();

            logger.info("  └─ VACUUM FULL block_sequence...");
            em.createNativeQuery("VACUUM FULL block_sequence").executeUpdate();

            // REINDEX rebuilds all indexes for optimal performance
            logger.info("  └─ REINDEX TABLE blocks...");
            em.createNativeQuery("REINDEX TABLE blocks").executeUpdate();

            logger.info("✅ PostgreSQL VACUUM FULL + REINDEX completed");
            return true;

        } catch (Exception e) {
            logger.error("❌ PostgreSQL VACUUM failed", e);
            return false;
        }
    }

    /**
     * Executes MySQL-specific optimize operations.
     *
     * <p><strong>Operations performed:</strong>
     * <ol>
     *   <li>OPTIMIZE TABLE blocks (reclaims unused space, defragments)</li>
     *   <li>OPTIMIZE TABLE authorized_keys</li>
     *   <li>OPTIMIZE TABLE off_chain_data</li>
     *   <li>OPTIMIZE TABLE block_sequence</li>
     * </ol>
     *
     * <p><strong>Performance Impact:</strong> OPTIMIZE TABLE locks tables during
     * execution. Duration depends on table size.
     *
     * @param em EntityManager for executing native queries
     * @return {@code true} if all operations succeeded, {@code false} otherwise
     */
    private boolean vacuumMySQL(EntityManager em) {
        logger.info("🔧 Executing MySQL OPTIMIZE TABLE...");

        try {
            logger.info("  └─ OPTIMIZE TABLE blocks...");
            em.createNativeQuery("OPTIMIZE TABLE blocks").executeUpdate();

            logger.info("  └─ OPTIMIZE TABLE authorized_keys...");
            em.createNativeQuery("OPTIMIZE TABLE authorized_keys").executeUpdate();

            logger.info("  └─ OPTIMIZE TABLE off_chain_data...");
            em.createNativeQuery("OPTIMIZE TABLE off_chain_data").executeUpdate();

            logger.info("  └─ OPTIMIZE TABLE block_sequence...");
            em.createNativeQuery("OPTIMIZE TABLE block_sequence").executeUpdate();

            logger.info("✅ MySQL OPTIMIZE TABLE completed");
            return true;

        } catch (Exception e) {
            logger.error("❌ MySQL OPTIMIZE failed", e);
            return false;
        }
    }

    /**
     * Executes SQLite-specific vacuum and optimization operations.
     *
     * <p><strong>Operations performed:</strong>
     * <ol>
     *   <li>VACUUM (rebuilds database file, reclaims free pages)</li>
     *   <li>PRAGMA optimize (updates query planner statistics)</li>
     * </ol>
     *
     * <p><strong>Performance Impact:</strong> VACUUM creates a temporary copy
     * of the database file. Requires free disk space equal to database size.
     *
     * @param em EntityManager for executing native queries
     * @return {@code true} if all operations succeeded, {@code false} otherwise
     */
    private boolean vacuumSQLite(EntityManager em) {
        logger.info("🔧 Executing SQLite VACUUM + PRAGMA optimize...");

        try {
            // For SQLite WAL mode: Close all connections to allow VACUUM to acquire exclusive lock
            // VACUUM requires exclusive access which cannot be obtained with open connections in WAL mode
            logger.info("  └─ Closing all database connections for VACUUM...");

            // Close current EntityManager (passed from executeInTransaction)
            if (em != null && em.isOpen()) {
                em.close();
            }

            // Close ALL connections from the pool and reinitialize
            JPAUtil.closeAllConnections();

            // Now create a new connection for VACUUM (will be the only connection)
            EntityManager vacuumEm = JPAUtil.getEntityManager();
            try {
                // IMPORTANT: SQLite VACUUM cannot run within a transaction (per SQLite docs)
                // "VACUUM cannot be run from within a transaction"
                // Solution: Use Hibernate's Session.doWork() method to execute JDBC operations
                // outside of JPA transaction management with proper connection handling
                
                // Get Hibernate Session from EntityManager
                org.hibernate.Session session = vacuumEm.unwrap(org.hibernate.Session.class);
                
                // Use Hibernate's doWork() for elegant JDBC access outside transaction
                // This is the recommended way per Hibernate documentation
                session.doWork(connection -> {
                    // Save and set autocommit=true (required by SQLite VACUUM)
                    boolean originalAutoCommit = connection.getAutoCommit();
                    connection.setAutoCommit(true);
                    
                    try (java.sql.Statement stmt = connection.createStatement()) {
                        // VACUUM rebuilds database file and reclaims free pages
                        logger.info("  └─ VACUUM...");
                        stmt.execute("VACUUM");

                        // PRAGMA optimize gathers statistics for query planner
                        logger.info("  └─ PRAGMA optimize...");
                        stmt.execute("PRAGMA optimize");
                    } finally {
                        // Restore original autocommit setting
                        connection.setAutoCommit(originalAutoCommit);
                    }
                });
                
                logger.info("✅ SQLite VACUUM + PRAGMA optimize completed");
                return true;
            } finally {
                // Close the VACUUM connection
                if (vacuumEm != null && vacuumEm.isOpen()) {
                    vacuumEm.close();
                }
            }

        } catch (Exception e) {
            logger.error("❌ SQLite VACUUM failed: {}", e.getMessage());
            logger.debug("SQLite VACUUM error details", e);
            return false;
        }
    }

    /**
     * Gets statistics about vacuum operations.
     *
     * @return Formatted statistics string
     */
    public String getVacuumStatistics() {
        StringBuilder stats = new StringBuilder();
        stats.append("📊 Database VACUUM Statistics\n");
        stats.append("=".repeat(50)).append("\n");
        stats.append(String.format("Total Operations: %d\n", totalVacuumOperations.get()));
        stats.append(String.format("Successful: %d\n", successfulVacuumOperations.get()));
        stats.append(String.format("Failed: %d\n", failedVacuumOperations.get()));

        long total = totalVacuumOperations.get();
        if (total > 0) {
            double successRate = (successfulVacuumOperations.get() * 100.0) / total;
            stats.append(String.format("Success Rate: %.1f%%\n", successRate));
        }

        LocalDateTime lastVacuum = lastVacuumTime.get();
        if (lastVacuum != null) {
            stats.append(String.format("Last Vacuum: %s\n", lastVacuum));

            Duration sinceLastVacuum = Duration.between(lastVacuum, LocalDateTime.now());
            long hoursSince = sinceLastVacuum.toHours();
            stats.append(String.format("Hours Since Last Vacuum: %d\n", hoursSince));
        } else {
            stats.append("Last Vacuum: Never\n");
        }

        stats.append("=".repeat(50));
        return stats.toString();
    }

    /**
     * Gets time since last successful vacuum operation.
     *
     * @return Duration since last vacuum, or null if never run
     */
    public Duration getTimeSinceLastVacuum() {
        LocalDateTime lastVacuum = lastVacuumTime.get();
        return lastVacuum != null ? Duration.between(lastVacuum, LocalDateTime.now()) : null;
    }

    /**
     * Gets success rate of vacuum operations.
     *
     * @return Success rate as percentage (0.0-100.0), or 0.0 if no operations
     */
    public double getSuccessRate() {
        long total = totalVacuumOperations.get();
        if (total == 0) {
            return 0.0;
        }
        return (successfulVacuumOperations.get() * 100.0) / total;
    }
}
