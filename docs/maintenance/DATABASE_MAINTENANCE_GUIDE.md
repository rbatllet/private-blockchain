# Database Maintenance Guide

**Version:** 1.0.6
**Last Updated:** October 2025

## Overview

The Private Blockchain system includes comprehensive database and file system maintenance capabilities designed for production deployments. This guide covers automated and manual maintenance operations for optimal performance and reliability.

## Table of Contents

- [Features](#features)
- [Architecture](#architecture)
- [Quick Start](#quick-start)
- [Configuration](#configuration)
- [Operations](#operations)
- [Monitoring](#monitoring)
- [Troubleshooting](#troubleshooting)
- [Best Practices](#best-practices)

---

## Features

### Automated Maintenance

✅ **Database Size Monitoring**
- Continuous size tracking across all database types
- Configurable alert thresholds (warning: 75%, critical: 90%)
- Growth rate analysis and trend reporting
- Integration with PerformanceMetricsService

✅ **Database VACUUM/OPTIMIZE**
- PostgreSQL: `VACUUM FULL` + `REINDEX`
- MySQL: `OPTIMIZE TABLE`
- SQLite: `VACUUM` + `PRAGMA optimize`
- H2: Automatic compaction (no action needed)
- Configurable scheduling (default: weekly)

✅ **Off-Chain File Cleanup**
- Orphaned file detection and removal
- GZIP compression for files >90 days old
- Batch processing to limit I/O impact
- Disk space verification before operations

### Manual Operations

✅ **On-Demand Maintenance**
- Run all maintenance tasks immediately
- Individual component execution
- Comprehensive reporting and statistics
- Graceful error handling

---

## Architecture

### Component Overview

```
DatabaseMaintenanceScheduler (Coordinator)
├── DatabaseSizeMonitor
│   ├── Size tracking (PostgreSQL/MySQL/SQLite/H2)
│   ├── Alert generation
│   └── Growth rate analysis
├── DatabaseVacuumService
│   ├── Database-specific VACUUM commands
│   ├── Minimum interval enforcement
│   └── Timeout detection
└── OffChainCleanupService
    ├── Orphaned file cleanup
    ├── File compression
    └── Disk space management
```

### Integration Points

- **PerformanceMetricsService**: Metrics recording and alerting
- **AlertService**: Structured JSON alert notifications
- **JPAUtil**: Database-agnostic transaction management
- **MaintenanceConstants**: Centralized configuration

---

## Quick Start

### Basic Usage

```java
import com.rbatllet.blockchain.maintenance.DatabaseMaintenanceScheduler;

// Create and start scheduler
DatabaseMaintenanceScheduler scheduler = new DatabaseMaintenanceScheduler();
scheduler.start();

// Scheduler now runs automated maintenance:
// - Size monitoring: Every 1 hour
// - VACUUM operations: Weekly (Sunday 2 AM)
// - File cleanup: Daily (4 AM)

// View statistics
System.out.println(scheduler.getStatistics());

// Graceful shutdown
scheduler.stop();
```

### Manual Maintenance

```java
// Run all maintenance tasks immediately
boolean success = scheduler.runAllMaintenanceTasks();

// Or run individual components
scheduler.getSizeMonitor().checkDatabaseSize();
scheduler.getVacuumService().performVacuum();
scheduler.getCleanupService().cleanupOrphanedFiles();
```

---

## Configuration

### Default Settings

Default values from `MaintenanceConstants`:

```java
// Database Size Monitoring
DATABASE_SIZE_WARNING_THRESHOLD_BYTES = 7_500_000_000L  // 7.5 GB
DATABASE_SIZE_CRITICAL_THRESHOLD_BYTES = 9_000_000_000L // 9 GB
DATABASE_SIZE_MAX_RECOMMENDED_BYTES = 10_000_000_000L   // 10 GB
DATABASE_SIZE_CHECK_INTERVAL_MS = 3_600_000L            // 1 hour

// VACUUM Operations
VACUUM_SCHEDULE_CRON = "0 2 * * 0"                      // Sunday 2 AM
VACUUM_TIMEOUT_MS = 7_200_000L                          // 2 hours
VACUUM_MIN_INTERVAL_MS = 518_400_000L                   // 6 days

// File Cleanup
CLEANUP_SCHEDULE_CRON = "0 4 * * *"                     // Daily 4 AM
FILE_COMPRESSION_AGE_DAYS = 90                          // 3 months
MAX_ORPHANED_FILES_PER_CLEANUP = 1000                   // Per cycle
ORPHANED_FILE_DETECTION_BATCH_SIZE = 5000               // Query batch size
MIN_FREE_DISK_SPACE_BYTES = 1_000_000_000L              // 1 GB

// Performance
MAINTENANCE_OPERATION_WARNING_THRESHOLD_MS = 1_800_000L // 30 minutes
MAINTENANCE_RETRY_DELAY_MS = 300_000L                   // 5 minutes
MAINTENANCE_MAX_RETRIES = 3                             // Retry attempts
CLEANUP_THREAD_POOL_SIZE = 2                            // I/O threads

// Logging
MAINTENANCE_PROGRESS_LOG_INTERVAL = 100                 // Every 100 ops
ENABLE_MAINTENANCE_ALERTS = true                        // Production alerts
ENABLE_DETAILED_REPORTS = false                         // Verbose logging
```

### Custom Configuration

#### Size Monitoring Interval

```java
scheduler.setSizeMonitoringInterval(30, TimeUnit.MINUTES);
```

#### VACUUM Schedule

```java
// Run every 3 days
scheduler.setVacuumSchedule(3, TimeUnit.DAYS);
```

#### Cleanup Schedule

```java
// Run every 12 hours
scheduler.setCleanupSchedule(12, TimeUnit.HOURS);
```

**⚠️ Important:** Configuration changes require scheduler restart:

```java
scheduler.stop();
scheduler.setSizeMonitoringInterval(30, TimeUnit.MINUTES);
scheduler.start();
```

---

## Operations

### Database Size Monitoring

#### Automatic Monitoring

The scheduler checks database size hourly and triggers alerts at thresholds:

- **Warning (75%)**: Logged with WARNING severity
- **Critical (90%)**: Logged with CRITICAL severity + AlertService notification

#### Manual Size Check

```java
DatabaseSizeMonitor monitor = scheduler.getSizeMonitor();

// Check current size
long sizeBytes = monitor.checkDatabaseSize();
long sizeMB = monitor.getCurrentSizeMB();

// Get growth rate
double growthPercent = monitor.getGrowthRate();

// Generate detailed report
String report = monitor.getSizeReport();
System.out.println(report);
```

#### Example Output

```
📊 Database Size Report
==================================================
Database Type: POSTGRESQL
Current Size: 8523 MB (8941363200 bytes)
Utilization: 85.2% of max recommended
Warning Threshold: 7500 MB
Critical Threshold: 9000 MB
Last Check: 2025-10-06 11:00:00
Growth Since Last Check: 156 MB (1.87%)
==================================================
```

### VACUUM/OPTIMIZE Operations

#### Automatic VACUUM

The scheduler runs VACUUM operations weekly (Sunday 2 AM) with:

- Minimum interval enforcement (6 days)
- Timeout detection (2 hours)
- Database-specific commands

#### Manual VACUUM

```java
DatabaseVacuumService vacuumService = scheduler.getVacuumService();

// Production: Respect 6-day minimum interval
boolean success = vacuumService.performVacuum();
if (!success) {
    logger.warn("VACUUM skipped: minimum interval not reached");
}

// View statistics
String stats = vacuumService.getVacuumStatistics();
Duration timeSince = vacuumService.getTimeSinceLastVacuum();
double successRate = vacuumService.getSuccessRate();
```

#### Force VACUUM (Bypass Interval Check)

For testing, manual maintenance after major deletions, or emergency situations:

```java
DatabaseVacuumService vacuumService = scheduler.getVacuumService();

// Testing: Always execute VACUUM regardless of interval
boolean success = vacuumService.forceVacuum();
assertTrue(success, "VACUUM should succeed in tests");

// Manual admin: Force VACUUM after bulk deletion
if (deletedBlockCount > 10_000) {
    logger.info("Forcing VACUUM after deleting {} blocks", deletedBlockCount);
    boolean success = vacuumService.forceVacuum();
    if (success) {
        logger.info("✅ Database optimized after bulk deletion");
    }
}

// Emergency: Database performance degraded
if (queryResponseTime > 5000) {
    logger.warn("⚠️ Query response time {}ms - forcing emergency VACUUM", queryResponseTime);
    boolean success = vacuumService.forceVacuum();
    if (success) {
        logger.info("✅ Emergency VACUUM completed");
    }
}
```

**⚠️ Warning:** Frequent VACUUM operations can cause performance degradation. Use `forceVacuum()` sparingly in production:
- ✅ **Good use cases**: Testing, post-migration cleanup, emergency maintenance
- ❌ **Bad use cases**: Routine scheduled operations, automatic triggers on every deletion

#### Database-Specific Behavior

| Database | Operations | Notes |
|----------|-----------|-------|
| PostgreSQL | `VACUUM FULL` blocks, authorized_keys, off_chain_data, block_sequence<br>`REINDEX TABLE` blocks | Requires exclusive locks<br>Rewrites entire tables |
| MySQL | `OPTIMIZE TABLE` blocks, authorized_keys, off_chain_data, block_sequence | Locks tables during operation |
| SQLite | `VACUUM`<br>`PRAGMA optimize` | **⚠️ WAL Mode**: Requires closing all connections first<br>Creates temporary copy<br>Requires free disk space (2x) |
| H2 | *No operation* | Automatic compaction on shutdown |

#### SQLite WAL Mode and VACUUM Operations

**Problem 1: Connection Locking**

SQLite with WAL (Write-Ahead Logging) mode maintains `SQLITE_LOCK_SHARED` on all open connections in the HikariCP connection pool. VACUUM operations require `SQLITE_LOCK_EXCLUSIVE`, which cannot be obtained while connections are open.

**Problem 2: Transaction Limitation**

SQLite VACUUM **cannot execute inside a transaction**. Per the [SQLite official documentation](https://www.sqlite.org/lang_vacuum.html):

> "The VACUUM command works by copying the contents of the database into a temporary database file and then overwriting the original with the contents of the temporary file. This cannot be done in a transaction."

However, JPA's `EntityManager.createNativeQuery().executeUpdate()` **requires an active transaction**, creating a contradiction.

**Solution: Hibernate's `session.doWork()` Pattern**

The `DatabaseVacuumService` uses Hibernate's official `session.doWork()` callback to execute JDBC operations outside of JPA transaction management. This is the documented and elegant way to execute DDL operations that cannot run in transactions.

```java
private boolean vacuumSQLite(EntityManager em) {
    // Step 1: Close current EntityManager
    if (em != null && em.isOpen()) {
        em.close();
    }

    // Step 2: Close ALL connections from HikariCP pool to obtain exclusive lock
    JPAUtil.closeAllConnections();

    // Step 3: Create new connection for VACUUM (only connection)
    EntityManager vacuumEm = JPAUtil.getEntityManager();
    try {
        // Unwrap Hibernate Session (officially supported by JPA spec)
        org.hibernate.Session session = vacuumEm.unwrap(org.hibernate.Session.class);
        
        // Use session.doWork() to execute raw JDBC outside transactions
        // This is the elegant Hibernate pattern for DDL operations
        session.doWork(connection -> {
            // SQLite VACUUM requires autocommit=true (no transaction)
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(true);
            
            try (java.sql.Statement stmt = connection.createStatement()) {
                logger.info("  └─ VACUUM...");
                stmt.execute("VACUUM");
                
                logger.info("  └─ PRAGMA optimize...");
                stmt.execute("PRAGMA optimize");
            } finally {
                // Best practice: restore original connection state
                connection.setAutoCommit(originalAutoCommit);
            }
        });
        
        logger.info("✅ SQLite VACUUM + PRAGMA optimize completed");
        return true;
    } finally {
        if (vacuumEm != null && vacuumEm.isOpen()) {
            vacuumEm.close();
        }
    }
}
```

**Why This Approach is Elegant:**

1. ✅ **Official Hibernate API**: `session.doWork()` is [documented by Hibernate](https://docs.jboss.org/hibernate/orm/current/javadocs/org/hibernate/Session.html#doWork-org.hibernate.jdbc.Work-) for executing work outside transaction management
2. ✅ **Clean abstraction**: Callback interface keeps JDBC code isolated
3. ✅ **Proper lifecycle**: Connection state is preserved and restored
4. ✅ **Type-safe**: Compile-time verification vs. error-prone string queries
5. ✅ **Maintainable**: Future developers understand the pattern immediately

**Alternative: `forceVacuum()` Method**

For testing and manual administrative operations, `DatabaseVacuumService` provides a `forceVacuum()` method that bypasses the 6-day minimum interval enforcement:

```java
// Bypass interval enforcement (useful for testing)
boolean success = vacuumService.forceVacuum();

// Respect interval enforcement (production behavior)
boolean success = vacuumService.performVacuum();
```

**Key Points:**

- ✅ `JPAUtil.closeAllConnections()` closes the entire HikariCP pool for exclusive lock
- ✅ `session.doWork()` executes JDBC outside JPA transaction management
- ✅ `connection.setAutoCommit(true)` enables SQLite VACUUM to execute
- ✅ Autocommit state is restored in finally block (best practice)
- ✅ Other databases (PostgreSQL, MySQL, H2) don't need this workaround
- 📊 **Database-agnostic design**: Only SQLite case uses `session.doWork()` pattern

#### Example Output

```
📊 Database VACUUM Statistics
==================================================
Total Operations: 12
Successful: 12
Failed: 0
Success Rate: 100.0%
Last Vacuum: 2025-10-06 02:00:00
Hours Since Last Vacuum: 168
==================================================
```

### Off-Chain File Cleanup

#### Automatic Cleanup

The scheduler runs daily cleanup (4 AM) with:

- Orphaned file detection and removal
- GZIP compression for files >90 days old
- Batch processing (max 1000 files per cycle)

#### Manual Cleanup

```java
OffChainCleanupService cleanupService = scheduler.getCleanupService();

// Cleanup orphaned files
CleanupResult orphanResult = cleanupService.cleanupOrphanedFiles();
System.out.printf("Deleted: %d files\n", orphanResult.getDeletedCount());

// Compress old files
CleanupResult compressResult = cleanupService.compressOldFiles();
System.out.printf("Compressed: %d files\n", compressResult.getCompressedCount());
System.out.printf("Space saved: %d MB\n", compressResult.getSpaceSavedMB());
```

#### How It Works

**Orphaned File Detection:**
1. Query database for all referenced off-chain hashes
2. Scan `off-chain-data/` directory
3. Delete files without database references
4. Respect max cleanup limit (1000 files/cycle)

**File Compression:**
1. Scan `off-chain-data/` directory
2. Find uncompressed files >90 days old
3. GZIP compress files (preserves original temporarily)
4. Delete original after successful compression
5. Result: `.dat` → `.dat.gz`

---

## Monitoring

### Scheduler Statistics

```java
// Get comprehensive statistics
String stats = scheduler.getStatistics();
```

#### Example Output

```
📊 Maintenance Scheduler Statistics
==================================================
Status: RUNNING
Uptime: 15 days, 4 hours
Total Maintenance Cycles: 45
Successful Cycles: 44
Failed Cycles: 1
Success Rate: 97.8%
Last Maintenance: 2025-10-06 11:00:00
Hours Since Last Maintenance: 2

📋 Scheduled Tasks:
  • Size Monitoring: every 1 hours
  • VACUUM Operations: every 7 days
  • File Cleanup: every 24 hours
==================================================
```

### PerformanceMetricsService Integration

All maintenance operations are recorded in `PerformanceMetricsService`:

```java
// Recorded metrics
database_size_check          // Size monitoring duration
database_vacuum              // VACUUM duration
scheduled_size_monitoring    // Scheduled monitoring status
scheduled_vacuum             // Scheduled VACUUM status
scheduled_cleanup            // Scheduled cleanup status
full_maintenance_cycle       // Manual maintenance duration
```

### AlertService Integration

Critical events trigger structured JSON alerts:

```json
{
  "type": "CRITICAL_DATABASE_SIZE",
  "severity": "CRITICAL",
  "message": "Database size CRITICAL: 9125 MB (91.3% of max recommended). Database type: POSTGRESQL. Immediate action required.",
  "timestamp": "2025-10-06T11:00:00Z"
}
```

---

## Troubleshooting

### Common Issues

#### Issue: VACUUM Operations Skipped

**Symptoms:**
```
⚠️ VACUUM operation skipped: minimum interval not reached.
```

**Cause:** VACUUM minimum interval (6 days) not met.

**Solution:** This is expected behavior to prevent excessive vacuuming. Wait for minimum interval or adjust `VACUUM_MIN_INTERVAL_MS`.

#### Issue: Size Monitoring Returns -1

**Symptoms:**
```
❌ Failed to determine database size
```

**Causes:**
1. H2 in-memory database (no file)
2. Database not available
3. Invalid database configuration

**Solutions:**
- H2 in-memory: Expected behavior (no file to measure)
- Check `JPAUtil.getCurrentConfig()` is not null
- Verify database connection is active

#### Issue: Cleanup Finds No Orphaned Files

**Symptoms:**
```
ℹ️  File cleanup completed: no files to clean
```

**Cause:** All off-chain files are referenced in database (healthy state).

**Solution:** No action needed. This is the expected state.

#### Issue: VACUUM Timeout Warning

**Symptoms:**
```
⚠️ VACUUM operation exceeded timeout threshold: 7500000ms (threshold: 7200000ms)
```

**Causes:**
1. Large database requiring >2 hours
2. High disk I/O load

**Solutions:**
- Schedule VACUUM during low-traffic periods
- Increase `VACUUM_TIMEOUT_MS` if needed
- Consider database optimization

#### Issue: Scheduler Won't Restart

**Symptoms:**
```
RuntimeException: Failed to start maintenance scheduler
```

**Cause:** Scheduler cannot be restarted after `stop()` (thread pool is shut down).

**Solution:** Create new scheduler instance:

```java
scheduler.stop();
scheduler = new DatabaseMaintenanceScheduler();
scheduler.start();
```

---

## Best Practices

### Production Deployment

#### 1. Configure Maintenance Windows

Schedule maintenance during low-traffic periods:

```java
// Example: Sunday 2 AM for VACUUM (default)
// Example: Daily 4 AM for cleanup (default)
// Adjust as needed for your timezone and traffic patterns
```

#### 2. Set Appropriate Thresholds

Adjust database size thresholds based on available disk space:

```java
// Modify MaintenanceConstants for your environment:
// - Warning: 75% of available space
// - Critical: 90% of available space
// - Max: Total available space minus safety margin
```

#### 3. Enable Alerts

Ensure `ENABLE_MAINTENANCE_ALERTS = true` in production:

```java
// Alerts are sent to AlertService for monitoring integration
```

#### 4. Monitor Logs

Review maintenance logs regularly:

```bash
# Key log patterns:
✅ VACUUM operation completed successfully
❌ VACUUM operation failed
⚠️ Database size WARNING/CRITICAL
🧹 File cleanup completed
```

#### 5. Test Before Production

Test maintenance operations on staging environment:

```java
// Run manual maintenance to verify behavior
scheduler.runAllMaintenanceTasks();

// Review statistics
System.out.println(scheduler.getStatistics());
```

### Database-Specific Recommendations

#### PostgreSQL

- **VACUUM FULL**: Requires exclusive locks, schedule during maintenance windows
- **Autovacuum**: Enable for continuous maintenance between VACUUM FULL runs
- **Connection pool**: 10-60 connections recommended
- **Monitoring**: Track bloat ratio via `pg_stat_user_tables`

#### MySQL

- **OPTIMIZE TABLE**: Locks tables, schedule during low traffic
- **InnoDB**: `innodb_file_per_table=1` for better space management
- **Connection pool**: 10-60 connections recommended
- **Monitoring**: Track fragmentation via `INFORMATION_SCHEMA`

#### SQLite

- **VACUUM**: Creates temporary copy, requires 2x free disk space
- **Connection pool**: 2-5 connections (single writer limitation)
- **WAL mode**: Enabled by default for better concurrency
- **Production**: Use PostgreSQL/MySQL for high-concurrency deployments

#### H2

- **Auto-compact**: Occurs on shutdown (no manual action needed)
- **Connection pool**: 5-20 connections recommended
- **Testing only**: Not recommended for production deployments

### Performance Optimization

#### 1. Off-Chain Storage

- Keep off-chain directory on fast storage (SSD)
- Monitor directory size and disk space
- Consider compression for all historical data

#### 2. Database Tuning

- **PostgreSQL**: `shared_buffers`, `work_mem`, `maintenance_work_mem`
- **MySQL**: `innodb_buffer_pool_size`, `innodb_log_file_size`
- **SQLite**: `cache_size`, `temp_store`, `mmap_size`

#### 3. Index Maintenance

- Regular REINDEX (PostgreSQL)
- OPTIMIZE TABLE (MySQL)
- ANALYZE (all databases)

---

## API Reference

### DatabaseMaintenanceScheduler

```java
public class DatabaseMaintenanceScheduler {
    // Lifecycle
    public boolean start()
    public boolean stop()
    public boolean isRunning()

    // Manual execution
    public boolean runAllMaintenanceTasks()

    // Configuration (requires restart)
    public void setSizeMonitoringInterval(long interval, TimeUnit unit)
    public void setVacuumSchedule(long interval, TimeUnit unit)
    public void setCleanupSchedule(long interval, TimeUnit unit)

    // Statistics
    public String getStatistics()
    public Duration getUptime()
    public Duration getTimeSinceLastMaintenance()
    public double getSuccessRate()

    // Component access
    public DatabaseSizeMonitor getSizeMonitor()
    public DatabaseVacuumService getVacuumService()
    public OffChainCleanupService getCleanupService()
}
```

### DatabaseSizeMonitor

```java
public class DatabaseSizeMonitor {
    // Operations
    public long checkDatabaseSize()  // Returns bytes, -1 on error
    public long getCurrentSizeMB()   // Returns MB, -1 on error

    // Statistics
    public double getGrowthRate()    // Returns percentage
    public String getSizeReport()    // Returns formatted report
}
```

### DatabaseVacuumService

```java
public class DatabaseVacuumService {
    // Operations
    public boolean performVacuum()   // Respects 6-day minimum interval
    public boolean forceVacuum()     // Bypasses interval check (testing/manual admin)

    // Statistics
    public String getVacuumStatistics()
    public Duration getTimeSinceLastVacuum()
    public double getSuccessRate()
}
```

**Method Details:**

- **`performVacuum()`**: Production method that enforces 6-day minimum interval between VACUUM operations. Returns `false` if interval has not elapsed.

- **`forceVacuum()`**: Bypasses interval enforcement for testing and manual administrative operations. Always attempts VACUUM regardless of time since last execution. Useful for:
  - Automated testing
  - Manual maintenance after major deletions
  - Emergency database optimization

### OffChainCleanupService

```java
public class OffChainCleanupService {
    // Operations
    public CleanupResult cleanupOrphanedFiles()
    public CleanupResult compressOldFiles()

    public static class CleanupResult {
        public int getDeletedCount()
        public int getCompressedCount()
        public long getSpaceSaved()       // Bytes
        public long getSpaceSavedMB()     // Megabytes
        public String getMessage()
    }
}
```

---

## Related Documentation

- [API Guide](../reference/API_GUIDE.md) - Complete API reference
- [Database Configuration](../database/DATABASE_AGNOSTIC.md) - Database switching guide
- [Production Guide](../deployment/PRODUCTION_GUIDE.md) - Deployment best practices
- [Security Guide](../security/SECURITY_GUIDE.md) - Security considerations

## External References

**Technical Implementation:**
- [Hibernate Session.doWork() Documentation](https://docs.jboss.org/hibernate/orm/current/javadocs/org/hibernate/Session.html#doWork-org.hibernate.jdbc.Work-) - Official API for executing JDBC work outside transaction management
- [SQLite VACUUM Documentation](https://www.sqlite.org/lang_vacuum.html) - Official SQLite documentation explaining VACUUM transaction limitations

**Note:** The SQLite VACUUM implementation uses research-backed patterns from Hibernate ORM official documentation, ensuring maintainability and adherence to best practices.

---

## Support

For issues or questions:

1. Check [Troubleshooting](#troubleshooting) section
2. Review logs for specific error messages
3. Verify database connectivity and configuration
4. Check disk space availability
5. Review PerformanceMetricsService alerts

---

**Note:** This maintenance system is designed for production deployments with databases growing to multi-gigabyte sizes. For development environments with small databases (<100 MB), automated maintenance provides minimal benefit but can be useful for testing and validation.
