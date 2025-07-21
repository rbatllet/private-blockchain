package com.rbatllet.blockchain.service;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Comprehensive integrity report for off-chain data verification
 * Provides detailed analysis of data consistency and health
 */
public class OffChainIntegrityReport {
    
    public enum IntegrityStatus {
        HEALTHY("✅ Healthy", "All integrity checks passed"),
        WARNING("⚠️ Warning", "Minor issues detected"),
        CORRUPTED("❌ Corrupted", "Data corruption detected"),
        MISSING("🔍 Missing", "Referenced data not found"),
        UNKNOWN("❓ Unknown", "Unable to verify integrity");
        
        private final String displayName;
        private final String description;
        
        IntegrityStatus(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }
    
    public static class IntegrityCheckResult {
        private final String dataId;
        private final String checkType;
        private final IntegrityStatus status;
        private final String details;
        private final LocalDateTime checkTime;
        private final Duration checkDuration;
        private final Map<String, Object> metadata;
        
        public IntegrityCheckResult(String dataId, String checkType, IntegrityStatus status, 
                                  String details, Duration checkDuration) {
            this.dataId = dataId;
            this.checkType = checkType;
            this.status = status;
            this.details = details;
            this.checkTime = LocalDateTime.now();
            this.checkDuration = checkDuration;
            this.metadata = new HashMap<>();
        }
        
        public IntegrityCheckResult addMetadata(String key, Object value) {
            metadata.put(key, value);
            return this;
        }
        
        // Getters
        public String getDataId() { return dataId; }
        public String getCheckType() { return checkType; }
        public IntegrityStatus getStatus() { return status; }
        public String getDetails() { return details; }
        public LocalDateTime getCheckTime() { return checkTime; }
        public Duration getCheckDuration() { return checkDuration; }
        public Map<String, Object> getMetadata() { return Collections.unmodifiableMap(metadata); }
    }
    
    public static class IntegrityStatistics {
        private final AtomicLong totalChecks = new AtomicLong(0);
        private final AtomicLong healthyCount = new AtomicLong(0);
        private final AtomicLong warningCount = new AtomicLong(0);
        private final AtomicLong corruptedCount = new AtomicLong(0);
        private final AtomicLong missingCount = new AtomicLong(0);
        private final AtomicLong unknownCount = new AtomicLong(0);
        private final AtomicLong totalBytesChecked = new AtomicLong(0);
        private final AtomicLong totalCheckDurationMs = new AtomicLong(0);
        
        public void recordCheck(IntegrityStatus status, long bytesChecked, Duration duration) {
            totalChecks.incrementAndGet();
            totalBytesChecked.addAndGet(bytesChecked);
            totalCheckDurationMs.addAndGet(duration.toMillis());
            
            switch (status) {
                case HEALTHY: healthyCount.incrementAndGet(); break;
                case WARNING: warningCount.incrementAndGet(); break;
                case CORRUPTED: corruptedCount.incrementAndGet(); break;
                case MISSING: missingCount.incrementAndGet(); break;
                case UNKNOWN: unknownCount.incrementAndGet(); break;
            }
        }
        
        public double getHealthyPercentage() {
            long total = totalChecks.get();
            return total > 0 ? (healthyCount.get() * 100.0) / total : 0.0;
        }
        
        public double getAverageCheckSpeedMbps() {
            long totalMs = totalCheckDurationMs.get();
            long totalBytes = totalBytesChecked.get();
            if (totalMs == 0 || totalBytes == 0) return 0.0;
            
            double seconds = totalMs / 1000.0;
            double megabytes = totalBytes / (1024.0 * 1024.0);
            return megabytes / seconds;
        }
        
        // Getters
        public long getTotalChecks() { return totalChecks.get(); }
        public long getHealthyCount() { return healthyCount.get(); }
        public long getWarningCount() { return warningCount.get(); }
        public long getCorruptedCount() { return corruptedCount.get(); }
        public long getMissingCount() { return missingCount.get(); }
        public long getUnknownCount() { return unknownCount.get(); }
        public long getTotalBytesChecked() { return totalBytesChecked.get(); }
        public long getTotalCheckDurationMs() { return totalCheckDurationMs.get(); }
    }
    
    private final String reportId;
    private final LocalDateTime reportTimestamp;
    private final List<IntegrityCheckResult> checkResults;
    private final IntegrityStatistics statistics;
    private final Map<String, List<String>> issuesByCategory;
    private final List<String> recommendations;
    private IntegrityStatus overallStatus;
    
    public OffChainIntegrityReport(String reportId) {
        this.reportId = reportId;
        this.reportTimestamp = LocalDateTime.now();
        this.checkResults = new ArrayList<>();
        this.statistics = new IntegrityStatistics();
        this.issuesByCategory = new HashMap<>();
        this.recommendations = new ArrayList<>();
        this.overallStatus = IntegrityStatus.HEALTHY;
    }
    
    public void addCheckResult(IntegrityCheckResult result) {
        checkResults.add(result);
        
        // Update statistics
        Object bytesObj = result.getMetadata().get("bytesChecked");
        long bytesChecked = bytesObj instanceof Long ? (Long) bytesObj : 0L;
        statistics.recordCheck(result.getStatus(), bytesChecked, result.getCheckDuration());
        
        // Categorize issues
        if (result.getStatus() != IntegrityStatus.HEALTHY) {
            String category = result.getCheckType();
            issuesByCategory.computeIfAbsent(category, k -> new ArrayList<>())
                           .add(result.getDataId() + ": " + result.getDetails());
        }
        
        // Update overall status
        updateOverallStatus(result.getStatus());
    }
    
    private void updateOverallStatus(IntegrityStatus newStatus) {
        // Priority: CORRUPTED > MISSING > WARNING > UNKNOWN > HEALTHY
        if (newStatus == IntegrityStatus.CORRUPTED || 
            (newStatus == IntegrityStatus.MISSING && overallStatus != IntegrityStatus.CORRUPTED) ||
            (newStatus == IntegrityStatus.WARNING && 
             overallStatus != IntegrityStatus.CORRUPTED && overallStatus != IntegrityStatus.MISSING) ||
            (newStatus == IntegrityStatus.UNKNOWN && overallStatus == IntegrityStatus.HEALTHY)) {
            overallStatus = newStatus;
        }
    }
    
    public void generateRecommendations() {
        recommendations.clear();
        
        double healthyPercentage = statistics.getHealthyPercentage();
        
        // Health-based recommendations
        if (healthyPercentage < 80) {
            recommendations.add("🚨 Data integrity below 80% - immediate action required");
        } else if (healthyPercentage < 95) {
            recommendations.add("⚠️ Data integrity below 95% - consider verification and repair");
        } else {
            recommendations.add("✅ Excellent data integrity - maintain current backup strategy");
        }
        
        // Issue-specific recommendations
        if (statistics.getCorruptedCount() > 0) {
            recommendations.add("❌ Corrupted data detected - restore from backup or repair immediately");
        }
        
        if (statistics.getMissingCount() > 0) {
            recommendations.add("🔍 Missing data detected - check backup systems and restore missing files");
        }
        
        if (statistics.getWarningCount() > statistics.getTotalChecks() * 0.1) {
            recommendations.add("⚠️ High warning count - investigate potential storage issues");
        }
        
        // Performance recommendations
        double avgSpeed = statistics.getAverageCheckSpeedMbps();
        if (avgSpeed < 10) {
            recommendations.add("🐌 Slow verification speed - consider optimizing storage access or upgrading hardware");
        }
        
        // Category-specific recommendations
        for (Map.Entry<String, List<String>> entry : issuesByCategory.entrySet()) {
            String category = entry.getKey();
            int issueCount = entry.getValue().size();
            
            if (issueCount > 5) {
                recommendations.add(String.format("📊 Multiple %s issues (%d) - review %s verification process", 
                                                category, issueCount, category));
            }
        }
        
        // Preventive recommendations
        recommendations.add("🔄 Schedule regular integrity checks to maintain data health");
        recommendations.add("💾 Ensure backup systems are functioning and up-to-date");
        
        if (statistics.getTotalChecks() > 1000) {
            recommendations.add("🎯 Consider implementing automated integrity monitoring for large datasets");
        }
    }
    
    public List<IntegrityCheckResult> getFailedChecks() {
        return checkResults.stream()
            .filter(result -> result.getStatus() != IntegrityStatus.HEALTHY)
            .collect(java.util.stream.Collectors.toList());
    }
    
    public Map<IntegrityStatus, List<IntegrityCheckResult>> groupByStatus() {
        Map<IntegrityStatus, List<IntegrityCheckResult>> grouped = new EnumMap<>(IntegrityStatus.class);
        for (IntegrityCheckResult result : checkResults) {
            grouped.computeIfAbsent(result.getStatus(), k -> new ArrayList<>()).add(result);
        }
        return grouped;
    }
    
    // Getters
    public String getReportId() { return reportId; }
    public LocalDateTime getReportTimestamp() { return reportTimestamp; }
    public List<IntegrityCheckResult> getCheckResults() { return Collections.unmodifiableList(checkResults); }
    public IntegrityStatistics getStatistics() { return statistics; }
    public Map<String, List<String>> getIssuesByCategory() { return Collections.unmodifiableMap(issuesByCategory); }
    public List<String> getRecommendations() { return Collections.unmodifiableList(recommendations); }
    public IntegrityStatus getOverallStatus() { return overallStatus; }
    
    public String getFormattedSummary() {
        generateRecommendations();
        
        StringBuilder sb = new StringBuilder();
        sb.append("🔐 Off-Chain Integrity Report\n");
        sb.append("==============================\n\n");
        
        sb.append(String.format("📋 Report ID: %s\n", reportId));
        sb.append(String.format("📅 Generated: %s\n", reportTimestamp));
        sb.append(String.format("🎯 Overall Status: %s\n\n", overallStatus.getDisplayName()));
        
        // Statistics summary
        sb.append("📊 Verification Statistics:\n");
        sb.append(String.format("   • Total Checks: %d\n", statistics.getTotalChecks()));
        sb.append(String.format("   • Healthy: %d (%.2f%%)\n", 
                                statistics.getHealthyCount(), statistics.getHealthyPercentage()));
        sb.append(String.format("   • Warnings: %d\n", statistics.getWarningCount()));
        sb.append(String.format("   • Corrupted: %d\n", statistics.getCorruptedCount()));
        sb.append(String.format("   • Missing: %d\n", statistics.getMissingCount()));
        sb.append(String.format("   • Data Processed: %.2f MB\n", 
                                statistics.getTotalBytesChecked() / (1024.0 * 1024.0)));
        sb.append(String.format("   • Average Speed: %.2f MB/s\n\n", statistics.getAverageCheckSpeedMbps()));
        
        // Issues by category
        if (!issuesByCategory.isEmpty()) {
            sb.append("⚠️ Issues by Category:\n");
            for (Map.Entry<String, List<String>> entry : issuesByCategory.entrySet()) {
                sb.append(String.format("   📂 %s (%d issues):\n", entry.getKey(), entry.getValue().size()));
                entry.getValue().stream()
                     .limit(3) // Show only first 3 issues per category
                     .forEach(issue -> sb.append("      - ").append(issue).append("\n"));
                if (entry.getValue().size() > 3) {
                    sb.append(String.format("      ... and %d more\n", entry.getValue().size() - 3));
                }
            }
            sb.append("\n");
        }
        
        // Recommendations
        if (!recommendations.isEmpty()) {
            sb.append("💡 Recommendations:\n");
            recommendations.forEach(rec -> sb.append("   ").append(rec).append("\n"));
        }
        
        return sb.toString();
    }
}