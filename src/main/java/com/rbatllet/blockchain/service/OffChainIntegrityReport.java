package com.rbatllet.blockchain.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.StampedLock;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Robust and thread-safe comprehensive integrity report for off-chain data verification.
 * Provides detailed analysis of data consistency and health with extensive validation
 * and error handling capabilities.
 *
 * This class is thread-safe and can be used in concurrent environments.
 * All public methods validate their inputs and handle edge cases gracefully.
 *
 * @author rbatllet
 * @version 1.0.5
 */
public class OffChainIntegrityReport {

    private static final Logger logger = LoggerFactory.getLogger(
        OffChainIntegrityReport.class
    );

    // Constants for validation and limits
    private static final int MAX_REPORT_ID_LENGTH = 255;
    private static final int MAX_DATA_ID_LENGTH = 500;
    private static final int MAX_CHECK_TYPE_LENGTH = 100;
    private static final int MAX_DETAILS_LENGTH = 2000;
    private static final int MAX_METADATA_ENTRIES = 50;
    private static final int MAX_CHECK_RESULTS = 100000;
    private static final long MAX_BYTES_CHECKED = Long.MAX_VALUE / 2; // Avoid overflow
    private static final Duration MAX_CHECK_DURATION = Duration.ofHours(24);

    public enum IntegrityStatus {
        HEALTHY("✅ Healthy", "All integrity checks passed"),
        WARNING("⚠️ Warning", "Minor issues detected"),
        CORRUPTED("❌ Corrupted", "Data corruption detected"),
        MISSING("🔍 Missing", "Referenced data not found"),
        UNKNOWN("❓ Unknown", "Unable to verify integrity");

        private final String displayName;
        private final String description;

        IntegrityStatus(String displayName, String description) {
            this.displayName = Objects.requireNonNull(
                displayName,
                "Display name cannot be null"
            );
            this.description = Objects.requireNonNull(
                description,
                "Description cannot be null"
            );
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Thread-safe and immutable integrity check result.
     * All fields are validated during construction.
     */
    public static class IntegrityCheckResult {

        private final String dataId;
        private final String checkType;
        private final IntegrityStatus status;
        private final String details;
        private final LocalDateTime checkTime;
        private final Duration checkDuration;
        private final Map<String, Object> metadata;

        /**
         * Creates a new IntegrityCheckResult with comprehensive validation.
         *
         * @param dataId The unique identifier for the data being checked (required, non-empty, max 500 chars)
         * @param checkType The type of check performed (required, non-empty, max 100 chars)
         * @param status The result status (required)
         * @param details Additional details about the check (required, max 2000 chars)
         * @param checkDuration How long the check took (required, positive, max 24 hours)
         * @throws IllegalArgumentException if any parameter is invalid
         */
        public IntegrityCheckResult(
            String dataId,
            String checkType,
            IntegrityStatus status,
            String details,
            Duration checkDuration
        ) {
            this.dataId = validateDataId(dataId);
            this.checkType = validateCheckType(checkType);
            this.status = Objects.requireNonNull(
                status,
                "Status cannot be null"
            );
            this.details = validateDetails(details);
            this.checkTime = LocalDateTime.now();
            this.checkDuration = validateCheckDuration(checkDuration);
            this.metadata = new ConcurrentHashMap<>();

            logger.debug(
                "Created IntegrityCheckResult for dataId: {}, status: {}",
                this.dataId,
                this.status
            );
        }

        private static String validateDataId(String dataId) {
            if (dataId == null) {
                throw new IllegalArgumentException("Data ID cannot be null");
            }
            if (dataId.trim().isEmpty()) {
                throw new IllegalArgumentException(
                    "Data ID cannot be empty or whitespace"
                );
            }
            if (dataId.length() > MAX_DATA_ID_LENGTH) {
                throw new IllegalArgumentException(
                    "Data ID length cannot exceed " +
                    MAX_DATA_ID_LENGTH +
                    " characters"
                );
            }
            return dataId.trim();
        }

        private static String validateCheckType(String checkType) {
            if (checkType == null) {
                throw new IllegalArgumentException("Check type cannot be null");
            }
            if (checkType.trim().isEmpty()) {
                throw new IllegalArgumentException(
                    "Check type cannot be empty or whitespace"
                );
            }
            if (checkType.length() > MAX_CHECK_TYPE_LENGTH) {
                throw new IllegalArgumentException(
                    "Check type length cannot exceed " +
                    MAX_CHECK_TYPE_LENGTH +
                    " characters"
                );
            }
            return checkType.trim().toUpperCase(); // Normalize to uppercase
        }

        private static String validateDetails(String details) {
            if (details == null) {
                throw new IllegalArgumentException("Details cannot be null");
            }
            if (details.trim().isEmpty()) {
                throw new IllegalArgumentException(
                    "Details cannot be empty or whitespace"
                );
            }
            if (details.length() > MAX_DETAILS_LENGTH) {
                throw new IllegalArgumentException(
                    "Details length cannot exceed " +
                    MAX_DETAILS_LENGTH +
                    " characters"
                );
            }
            return details.trim();
        }

        private static Duration validateCheckDuration(Duration checkDuration) {
            if (checkDuration == null) {
                throw new IllegalArgumentException(
                    "Check duration cannot be null"
                );
            }
            if (checkDuration.isNegative()) {
                throw new IllegalArgumentException(
                    "Check duration cannot be negative"
                );
            }
            if (checkDuration.compareTo(MAX_CHECK_DURATION) > 0) {
                throw new IllegalArgumentException(
                    "Check duration cannot exceed " + MAX_CHECK_DURATION
                );
            }
            return checkDuration;
        }

        /**
         * Adds metadata to this check result with validation.
         *
         * @param key The metadata key (required, non-empty)
         * @param value The metadata value (required, non-null)
         * @return this instance for method chaining
         * @throws IllegalArgumentException if parameters are invalid or too many metadata entries
         */
        public IntegrityCheckResult addMetadata(String key, Object value) {
            if (key == null || key.trim().isEmpty()) {
                throw new IllegalArgumentException(
                    "Metadata key cannot be null or empty"
                );
            }
            if (value == null) {
                throw new IllegalArgumentException(
                    "Metadata value cannot be null"
                );
            }
            if (metadata.size() >= MAX_METADATA_ENTRIES) {
                throw new IllegalArgumentException(
                    "Cannot exceed " +
                    MAX_METADATA_ENTRIES +
                    " metadata entries"
                );
            }

            synchronized (this) {
                metadata.put(key.trim(), value);
            }

            logger.debug(
                "Added metadata to check result {}: {} = {}",
                dataId,
                key,
                value
            );
            return this;
        }

        // Getters with defensive copying where needed
        public String getDataId() {
            return dataId;
        }

        public String getCheckType() {
            return checkType;
        }

        public IntegrityStatus getStatus() {
            return status;
        }

        public String getDetails() {
            return details;
        }

        public LocalDateTime getCheckTime() {
            return checkTime;
        }

        public Duration getCheckDuration() {
            return checkDuration;
        }

        public Map<String, Object> getMetadata() {
            return Collections.unmodifiableMap(new HashMap<>(metadata));
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            IntegrityCheckResult that = (IntegrityCheckResult) obj;
            return (
                Objects.equals(dataId, that.dataId) &&
                Objects.equals(checkType, that.checkType) &&
                Objects.equals(status, that.status) &&
                Objects.equals(checkTime, that.checkTime)
            );
        }

        @Override
        public int hashCode() {
            return Objects.hash(dataId, checkType, status, checkTime);
        }

        /**
         * Checks if this integrity check result indicates healthy status.
         *
         * @return true if status is HEALTHY, false otherwise
         */
        public boolean isHealthy() {
            return status == IntegrityStatus.HEALTHY;
        }

        @Override
        public String toString() {
            return String.format(
                "IntegrityCheckResult{dataId='%s', checkType='%s', status=%s, checkTime=%s}",
                dataId,
                checkType,
                status,
                checkTime
            );
        }
    }

    /**
     * Thread-safe statistics collector for integrity checks.
     */
    public static class IntegrityStatistics {

        private final AtomicLong totalChecks = new AtomicLong(0);
        private final AtomicLong healthyCount = new AtomicLong(0);
        private final AtomicLong warningCount = new AtomicLong(0);
        private final AtomicLong corruptedCount = new AtomicLong(0);
        private final AtomicLong missingCount = new AtomicLong(0);
        private final AtomicLong unknownCount = new AtomicLong(0);
        private final AtomicLong totalBytesChecked = new AtomicLong(0);
        private final AtomicLong totalCheckDurationMs = new AtomicLong(0);

        /**
         * Records a check with validation.
         *
         * @param status The check status (required)
         * @param bytesChecked Number of bytes checked (non-negative, reasonable limit)
         * @param duration Check duration (required, non-null, positive)
         * @throws IllegalArgumentException if parameters are invalid
         */
        public void recordCheck(
            IntegrityStatus status,
            long bytesChecked,
            Duration duration
        ) {
            Objects.requireNonNull(status, "Status cannot be null");
            Objects.requireNonNull(duration, "Duration cannot be null");

            if (bytesChecked < 0) {
                throw new IllegalArgumentException(
                    "Bytes checked cannot be negative"
                );
            }
            if (bytesChecked > MAX_BYTES_CHECKED) {
                throw new IllegalArgumentException(
                    "Bytes checked exceeds maximum allowed: " +
                    MAX_BYTES_CHECKED
                );
            }
            if (duration.isNegative()) {
                throw new IllegalArgumentException(
                    "Duration cannot be negative"
                );
            }

            totalChecks.incrementAndGet();

            // Use compareAndSet loop to safely add large values and detect overflow
            long currentBytes, newBytes;
            do {
                currentBytes = totalBytesChecked.get();
                newBytes = currentBytes + bytesChecked;
                if (newBytes < currentBytes) {
                    // Overflow detection
                    logger.warn(
                        "Bytes checked counter overflow detected, resetting to maximum safe value"
                    );
                    totalBytesChecked.set(MAX_BYTES_CHECKED);
                    break;
                }
            } while (!totalBytesChecked.compareAndSet(currentBytes, newBytes));

            // Similar overflow protection for duration
            long currentDuration, newDuration;
            long durationMs = duration.toMillis();
            do {
                currentDuration = totalCheckDurationMs.get();
                newDuration = currentDuration + durationMs;
                if (newDuration < currentDuration) {
                    // Overflow detection
                    logger.warn(
                        "Duration counter overflow detected, resetting to maximum safe value"
                    );
                    totalCheckDurationMs.set(Long.MAX_VALUE / 2);
                    break;
                }
            } while (
                !totalCheckDurationMs.compareAndSet(
                    currentDuration,
                    newDuration
                )
            );

            // Update status counts
            switch (status) {
                case HEALTHY:
                    healthyCount.incrementAndGet();
                    break;
                case WARNING:
                    warningCount.incrementAndGet();
                    break;
                case CORRUPTED:
                    corruptedCount.incrementAndGet();
                    break;
                case MISSING:
                    missingCount.incrementAndGet();
                    break;
                case UNKNOWN:
                    unknownCount.incrementAndGet();
                    break;
            }

            logger.debug(
                "Recorded check: status={}, bytes={}, duration={}ms",
                status,
                bytesChecked,
                durationMs
            );
        }

        public double getHealthyPercentage() {
            long total = totalChecks.get();
            return total > 0 ? (healthyCount.get() * 100.0) / total : 100.0; // Default to 100% if no checks
        }

        public double getAverageCheckSpeedMbps() {
            long totalMs = totalCheckDurationMs.get();
            long totalBytes = totalBytesChecked.get();
            if (totalMs == 0 || totalBytes == 0) return 0.0;

            double seconds = totalMs / 1000.0;
            double megabytes = totalBytes / (1024.0 * 1024.0);
            return megabytes / seconds;
        }

        // Thread-safe getters
        public long getTotalChecks() {
            return totalChecks.get();
        }

        public long getHealthyCount() {
            return healthyCount.get();
        }

        public long getWarningCount() {
            return warningCount.get();
        }

        public long getCorruptedCount() {
            return corruptedCount.get();
        }

        public long getMissingCount() {
            return missingCount.get();
        }

        public long getUnknownCount() {
            return unknownCount.get();
        }

        public long getTotalBytesChecked() {
            return totalBytesChecked.get();
        }

        public long getTotalCheckDurationMs() {
            return totalCheckDurationMs.get();
        }

        /**
         * Creates a snapshot of current statistics for safe reading.
         */
        public Map<String, Object> getSnapshot() {
            Map<String, Object> snapshot = new HashMap<>();
            snapshot.put("totalChecks", getTotalChecks());
            snapshot.put("healthyCount", getHealthyCount());
            snapshot.put("warningCount", getWarningCount());
            snapshot.put("corruptedCount", getCorruptedCount());
            snapshot.put("missingCount", getMissingCount());
            snapshot.put("unknownCount", getUnknownCount());
            snapshot.put("totalBytesChecked", getTotalBytesChecked());
            snapshot.put("totalCheckDurationMs", getTotalCheckDurationMs());
            snapshot.put("healthyPercentage", getHealthyPercentage());
            snapshot.put("averageSpeedMbps", getAverageCheckSpeedMbps());
            return Collections.unmodifiableMap(snapshot);
        }
    }

    // Thread-safe fields
    private final String reportId;
    private final LocalDateTime reportTimestamp;
    private final List<IntegrityCheckResult> checkResults;
    private final IntegrityStatistics statistics;
    private final Map<String, List<String>> issuesByCategory;
    private final List<String> recommendations;
    private volatile IntegrityStatus overallStatus;
    private final StampedLock lock = new StampedLock(); // StampedLock (no fair mode)

    /**
     * Creates a new OffChainIntegrityReport with validation.
     *
     * @param reportId The unique report identifier (required, non-empty, max 255 chars)
     * @throws IllegalArgumentException if reportId is invalid
     */
    public OffChainIntegrityReport(String reportId) {
        this.reportId = validateReportId(reportId);
        this.reportTimestamp = LocalDateTime.now();
        this.checkResults = new CopyOnWriteArrayList<>(); // Thread-safe list
        this.statistics = new IntegrityStatistics();
        this.issuesByCategory = new ConcurrentHashMap<>();
        this.recommendations = new CopyOnWriteArrayList<>(); // Thread-safe list
        this.overallStatus = IntegrityStatus.HEALTHY;

        logger.info(
            "Created new OffChainIntegrityReport with ID: {}",
            this.reportId
        );
    }

    private static String validateReportId(String reportId) {
        if (reportId == null) {
            throw new IllegalArgumentException("Report ID cannot be null");
        }
        if (reportId.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Report ID cannot be empty or whitespace"
            );
        }
        if (reportId.length() > MAX_REPORT_ID_LENGTH) {
            throw new IllegalArgumentException(
                "Report ID length cannot exceed " +
                MAX_REPORT_ID_LENGTH +
                " characters"
            );
        }
        return reportId.trim();
    }

    /**
     * Adds a check result with comprehensive validation and thread safety.
     *
     * @param result The check result to add (required, non-null)
     * @throws IllegalArgumentException if result is null or limits exceeded
     * @throws IllegalStateException if too many check results
     */
    public void addCheckResult(IntegrityCheckResult result) {
        Objects.requireNonNull(result, "Check result cannot be null");

        long stamp = lock.writeLock();
        try {
            if (checkResults.size() >= MAX_CHECK_RESULTS) {
                throw new IllegalStateException(
                    "Cannot exceed " + MAX_CHECK_RESULTS + " check results"
                );
            }

            checkResults.add(result);

            // Update statistics
            Object bytesObj = result.getMetadata().get("bytesChecked");
            long bytesChecked = 0L;
            if (bytesObj instanceof Number) {
                long bytes = ((Number) bytesObj).longValue();
                if (bytes >= 0) {
                    bytesChecked = bytes;
                }
            }

            try {
                statistics.recordCheck(
                    result.getStatus(),
                    bytesChecked,
                    result.getCheckDuration()
                );
            } catch (Exception e) {
                logger.warn(
                    "Failed to record statistics for check result: {}",
                    e.getMessage()
                );
                // Continue processing even if statistics fail
            }

            // Categorize issues
            if (result.getStatus() != IntegrityStatus.HEALTHY) {
                String category = result.getCheckType();
                String issue = result.getDataId() + ": " + result.getDetails();

                issuesByCategory
                    .computeIfAbsent(category, k ->
                        new CopyOnWriteArrayList<>()
                    )
                    .add(issue);
            }

            // Update overall status
            updateOverallStatus(result.getStatus());

            logger.debug(
                "Added check result for dataId: {}, new total: {}",
                result.getDataId(),
                checkResults.size()
            );
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    private void updateOverallStatus(IntegrityStatus newStatus) {
        // Priority: CORRUPTED > MISSING > WARNING > UNKNOWN > HEALTHY
        IntegrityStatus currentStatus = overallStatus;

        if (
            newStatus == IntegrityStatus.CORRUPTED ||
            (newStatus == IntegrityStatus.MISSING &&
                currentStatus != IntegrityStatus.CORRUPTED) ||
            (newStatus == IntegrityStatus.WARNING &&
                currentStatus != IntegrityStatus.CORRUPTED &&
                currentStatus != IntegrityStatus.MISSING) ||
            (newStatus == IntegrityStatus.UNKNOWN &&
                currentStatus == IntegrityStatus.HEALTHY)
        ) {
            overallStatus = newStatus;
            logger.debug("Updated overall status to: {}", newStatus);
        }
    }

    /**
     * Generates comprehensive recommendations based on current data.
     * This method is thread-safe and can be called concurrently.
     */
    public void generateRecommendations() {
        long stamp = lock.writeLock();
        try {
            recommendations.clear();

            double healthyPercentage = statistics.getHealthyPercentage();
            long totalChecks = statistics.getTotalChecks();

            // Health-based recommendations
            if (totalChecks == 0) {
                recommendations.add(
                    "ℹ️ No integrity checks have been performed yet - run initial verification"
                );
                recommendations.add(
                    "🔍 Schedule comprehensive integrity scan to establish baseline"
                );
            } else if (healthyPercentage < 50) {
                recommendations.add(
                    "🚨 CRITICAL: Data integrity severely compromised (<50%) - immediate intervention required"
                );
                recommendations.add(
                    "📞 Contact system administrator and initiate emergency recovery procedures"
                );
            } else if (healthyPercentage < 80) {
                recommendations.add(
                    "🚨 Data integrity below 80% - immediate action required"
                );
                recommendations.add(
                    "🔧 Run comprehensive system diagnostic and repair tools"
                );
            } else if (healthyPercentage < 95) {
                recommendations.add(
                    "⚠️ Data integrity below 95% - consider verification and repair"
                );
                recommendations.add(
                    "📊 Schedule detailed analysis to identify root causes"
                );
            } else {
                recommendations.add(
                    "✅ Excellent data integrity - maintain current backup and verification strategy"
                );
            }

            // Issue-specific recommendations
            if (statistics.getCorruptedCount() > 0) {
                recommendations.add(
                    "❌ Corrupted data detected - restore from backup or repair immediately"
                );
                if (statistics.getCorruptedCount() > totalChecks * 0.05) {
                    // More than 5%
                    recommendations.add(
                        "⚠️ High corruption rate - investigate storage hardware and software integrity"
                    );
                }
            }

            if (statistics.getMissingCount() > 0) {
                recommendations.add(
                    "🔍 Missing data detected - check backup systems and restore missing files"
                );
                if (statistics.getMissingCount() > totalChecks * 0.1) {
                    // More than 10%
                    recommendations.add(
                        "📂 Significant data loss - audit file system and backup procedures"
                    );
                }
            }

            if (statistics.getWarningCount() > totalChecks * 0.1) {
                recommendations.add(
                    "⚠️ High warning count - investigate potential storage issues"
                );
            }

            // Performance recommendations
            double avgSpeed = statistics.getAverageCheckSpeedMbps();
            if (totalChecks > 0) {
                if (avgSpeed < 1) {
                    recommendations.add(
                        "🐌 Very slow verification speed - check disk health and consider hardware upgrade"
                    );
                } else if (avgSpeed < 10) {
                    recommendations.add(
                        "🐌 Slow verification speed - consider optimizing storage access or upgrading hardware"
                    );
                } else if (avgSpeed > 100) {
                    recommendations.add(
                        "🚀 Excellent verification speed - current system performing optimally"
                    );
                }
            }

            // Category-specific recommendations
            issuesByCategory.forEach((category, issues) -> {
                int issueCount = issues.size();
                if (issueCount > 5) {
                    recommendations.add(
                        String.format(
                            "📊 Multiple %s issues (%d) - review %s verification process",
                            category,
                            issueCount,
                            category.toLowerCase()
                        )
                    );
                }
            });

            // Scale-based recommendations
            if (totalChecks > 10000) {
                recommendations.add(
                    "🎯 Large dataset detected - consider implementing automated integrity monitoring"
                );
                recommendations.add(
                    "📈 Implement trending analysis to detect degradation patterns"
                );
            } else if (totalChecks > 1000) {
                recommendations.add(
                    "🎯 Consider implementing automated integrity monitoring for growing dataset"
                );
            }

            // Preventive recommendations
            recommendations.add(
                "🔄 Schedule regular integrity checks to maintain data health"
            );
            recommendations.add(
                "💾 Ensure backup systems are functioning and up-to-date"
            );
            recommendations.add(
                "📋 Document and review integrity check procedures regularly"
            );

            logger.info(
                "Generated {} recommendations for report {}",
                recommendations.size(),
                reportId
            );
        } catch (Exception e) {
            logger.error(
                "Error generating recommendations for report {}: {}",
                reportId,
                e.getMessage()
            );
            recommendations.clear();
            recommendations.add(
                "❌ Error generating recommendations - contact system administrator"
            );
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    /**
     * Returns all failed checks (non-healthy status) in a thread-safe manner.
     */
    public List<IntegrityCheckResult> getFailedChecks() {
        long stamp = lock.readLock();
        try {
            return checkResults
                .stream()
                .filter(result -> result.getStatus() != IntegrityStatus.HEALTHY)
                .collect(Collectors.toList());
        } finally {
            lock.unlockRead(stamp);
        }
    }

    /**
     * Groups check results by status in a thread-safe manner.
     */
    public Map<IntegrityStatus, List<IntegrityCheckResult>> groupByStatus() {
        long stamp = lock.readLock();
        try {
            Map<IntegrityStatus, List<IntegrityCheckResult>> grouped =
                new EnumMap<>(IntegrityStatus.class);
            for (IntegrityCheckResult result : checkResults) {
                grouped
                    .computeIfAbsent(result.getStatus(), k -> new ArrayList<>())
                    .add(result);
            }
            return grouped;
        } finally {
            lock.unlockRead(stamp);
        }
    }

    /**
     * Returns the current check results count.
     */
    public int getCheckResultsCount() {
        return checkResults.size();
    }

    /**
     * Validates the internal state of the report.
     * @return true if the report is in a valid state
     */
    public boolean validateInternalState() {
        long stamp = lock.readLock();
        try {
            // Check that statistics match actual data
            Map<IntegrityStatus, Long> actualCounts = checkResults
                .stream()
                .collect(
                    Collectors.groupingBy(
                        IntegrityCheckResult::getStatus,
                        Collectors.counting()
                    )
                );

            long expectedTotal = statistics.getTotalChecks();
            long actualTotal = checkResults.size();

            boolean isValid = expectedTotal == actualTotal;

            // Validate individual status counts
            if (isValid) {
                long expectedHealthy = statistics.getHealthyCount();
                long actualHealthy = actualCounts.getOrDefault(
                    IntegrityStatus.HEALTHY,
                    0L
                );

                long expectedWarning = statistics.getWarningCount();
                long actualWarning = actualCounts.getOrDefault(
                    IntegrityStatus.WARNING,
                    0L
                );

                long expectedCorrupted = statistics.getCorruptedCount();
                long actualCorrupted = actualCounts.getOrDefault(
                    IntegrityStatus.CORRUPTED,
                    0L
                );

                long expectedMissing = statistics.getMissingCount();
                long actualMissing = actualCounts.getOrDefault(
                    IntegrityStatus.MISSING,
                    0L
                );

                long expectedUnknown = statistics.getUnknownCount();
                long actualUnknown = actualCounts.getOrDefault(
                    IntegrityStatus.UNKNOWN,
                    0L
                );

                isValid =
                    expectedHealthy == actualHealthy &&
                    expectedWarning == actualWarning &&
                    expectedCorrupted == actualCorrupted &&
                    expectedMissing == actualMissing &&
                    expectedUnknown == actualUnknown;

                if (!isValid) {
                    logger.warn(
                        "Status counts validation failed - Expected: H={}, W={}, C={}, M={}, U={} | Actual: H={}, W={}, C={}, M={}, U={}",
                        expectedHealthy,
                        expectedWarning,
                        expectedCorrupted,
                        expectedMissing,
                        expectedUnknown,
                        actualHealthy,
                        actualWarning,
                        actualCorrupted,
                        actualMissing,
                        actualUnknown
                    );
                }
            }

            if (!isValid && expectedTotal != actualTotal) {
                logger.warn(
                    "Total counts validation failed: expected {} checks, found {}",
                    expectedTotal,
                    actualTotal
                );
            }

            return isValid;
        } finally {
            lock.unlockRead(stamp);
        }
    }

    // Thread-safe getters with defensive copying
    public String getReportId() {
        return reportId;
    }

    public LocalDateTime getReportTimestamp() {
        return reportTimestamp;
    }

    public List<IntegrityCheckResult> getCheckResults() {
        return Collections.unmodifiableList(new ArrayList<>(checkResults));
    }

    public IntegrityStatistics getStatistics() {
        return statistics;
    }

    public Map<String, List<String>> getIssuesByCategory() {
        long stamp = lock.readLock();
        try {
            Map<String, List<String>> copy = new HashMap<>();
            issuesByCategory.forEach((key, value) ->
                copy.put(
                    key,
                    Collections.unmodifiableList(new ArrayList<>(value))
                )
            );
            return Collections.unmodifiableMap(copy);
        } finally {
            lock.unlockRead(stamp);
        }
    }

    public List<String> getRecommendations() {
        return Collections.unmodifiableList(new ArrayList<>(recommendations));
    }

    public IntegrityStatus getOverallStatus() {
        return overallStatus;
    }

    /**
     * Generates a comprehensive formatted summary with error handling.
     */
    public String getFormattedSummary() {
        long stamp = lock.readLock();
        try {
            // Ensure recommendations are up to date
            if (recommendations.isEmpty() && statistics.getTotalChecks() > 0) {
                lock.unlockRead(stamp);
                generateRecommendations();
                stamp = lock.readLock(); // Reacquire lock
            }

            StringBuilder sb = new StringBuilder();
            sb.append("🔐 Off-Chain Integrity Report\n");
            sb.append("==============================\n\n");

            sb.append(String.format("📋 Report ID: %s\n", reportId));
            sb.append(String.format("📅 Generated: %s\n", reportTimestamp));
            sb.append(
                String.format(
                    "🎯 Overall Status: %s\n\n",
                    overallStatus.getDisplayName()
                )
            );

            // Statistics summary with safe formatting
            long totalChecks = statistics.getTotalChecks();
            if (totalChecks > 0) {
                sb.append("📊 Verification Statistics:\n");
                sb.append(
                    String.format("   • Total Checks: %,d\n", totalChecks)
                );
                sb.append(
                    String.format(
                        "   • Healthy: %,d (%.2f%%)\n",
                        statistics.getHealthyCount(),
                        statistics.getHealthyPercentage()
                    )
                );
                sb.append(
                    String.format(
                        "   • Warnings: %,d\n",
                        statistics.getWarningCount()
                    )
                );
                sb.append(
                    String.format(
                        "   • Corrupted: %,d\n",
                        statistics.getCorruptedCount()
                    )
                );
                sb.append(
                    String.format(
                        "   • Missing: %,d\n",
                        statistics.getMissingCount()
                    )
                );
                sb.append(
                    String.format(
                        "   • Unknown: %,d\n",
                        statistics.getUnknownCount()
                    )
                );
                sb.append(
                    String.format(
                        "   • Data Processed: %.2f MB\n",
                        statistics.getTotalBytesChecked() / (1024.0 * 1024.0)
                    )
                );
                sb.append(
                    String.format(
                        "   • Average Speed: %.2f MB/s\n\n",
                        statistics.getAverageCheckSpeedMbps()
                    )
                );
            } else {
                sb.append("📊 No verification statistics available yet\n\n");
            }

            // Issues by category with safe iteration
            if (!issuesByCategory.isEmpty()) {
                sb.append("⚠️ Issues by Category:\n");
                issuesByCategory.forEach((category, issues) -> {
                    sb.append(
                        String.format(
                            "   📂 %s (%d issues):\n",
                            category,
                            issues.size()
                        )
                    );
                    issues
                        .stream()
                        .limit(3) // Show only first 3 issues per category
                        .forEach(issue ->
                            sb.append("      - ").append(issue).append("\n")
                        );
                    if (issues.size() > 3) {
                        sb.append(
                            String.format(
                                "      ... and %d more\n",
                                issues.size() - 3
                            )
                        );
                    }
                });
                sb.append("\n");
            }

            // Recommendations with safe iteration
            if (!recommendations.isEmpty()) {
                sb.append("💡 Recommendations:\n");
                recommendations.forEach(rec ->
                    sb.append("   ").append(rec).append("\n")
                );
            } else {
                sb.append("💡 No specific recommendations available\n");
            }

            sb.append(
                String.format(
                    "\n📈 Report generated with %d check results\n",
                    checkResults.size()
                )
            );
            sb.append("🔒 This report is thread-safe and validated\n");

            return sb.toString();
        } catch (Exception e) {
            logger.error(
                "Error generating formatted summary for report {}: {}",
                reportId,
                e.getMessage()
            );
            return String.format(
                "❌ Error generating summary for report %s: %s\n" +
                "Please contact system administrator.",
                reportId,
                e.getMessage()
            );
        } finally {
            lock.unlockRead(stamp);
        }
    }

    @Override
    public String toString() {
        return String.format(
            "OffChainIntegrityReport{id='%s', timestamp=%s, checks=%d, status=%s}",
            reportId,
            reportTimestamp,
            checkResults.size(),
            overallStatus
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        OffChainIntegrityReport that = (OffChainIntegrityReport) obj;
        return (
            Objects.equals(reportId, that.reportId) &&
            Objects.equals(reportTimestamp, that.reportTimestamp)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(reportId, reportTimestamp);
    }
}
