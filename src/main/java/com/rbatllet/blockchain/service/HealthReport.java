package com.rbatllet.blockchain.service;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

/**
 * Comprehensive health report for blockchain system diagnostics
 * Provides overall system health assessment and performance metrics
 */
public class HealthReport {
    
    public enum HealthStatus {
        EXCELLENT("✅ Excellent"),
        GOOD("🟢 Good"),
        HEALTHY("✅ Healthy"),
        WARNING("⚠️ Warning"),
        CRITICAL("❌ Critical");
        
        private final String displayName;
        
        HealthStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() { return displayName; }
    }
    
    private final HealthStatus overallHealth;
    private final String summary;
    private final LocalDateTime timestamp;
    private final List<HealthIssue> issues;
    private final HealthMetrics metrics;
    private final Map<String, Object> systemInfo;
    private String reportId;
    private Duration generationTime;
    private List<String> recommendations;
    
    public HealthReport(HealthStatus overallHealth, String summary) {
        this.overallHealth = overallHealth;
        this.summary = summary;
        this.timestamp = LocalDateTime.now();
        this.issues = new ArrayList<>();
        this.metrics = new HealthMetrics();
        this.systemInfo = new HashMap<>();
        this.reportId = "health_" + System.currentTimeMillis();
        this.generationTime = Duration.ZERO;
        this.recommendations = new ArrayList<>();
    }
    
    // Getters
    public HealthStatus getOverallHealth() { return overallHealth; }
    public HealthStatus getOverallStatus() { return overallHealth; } // Alias for compatibility
    public String getSummary() { return summary; }
    public String getMessage() { return summary; } // Alias for compatibility
    public LocalDateTime getTimestamp() { return timestamp; }
    public List<HealthIssue> getIssues() { return Collections.unmodifiableList(issues); }
    public HealthMetrics getMetrics() { return metrics; }
    public HealthMetrics getHealthMetrics() { return metrics; } // Alias for compatibility
    public Map<String, Object> getSystemInfo() { return Collections.unmodifiableMap(systemInfo); }
    public String getReportId() { return reportId; }
    public Duration getGenerationTime() { return generationTime; }
    public List<String> getRecommendations() { return Collections.unmodifiableList(recommendations); }
    
    // Builder methods
    public HealthReport addIssue(String component, String description, String severity, String recommendation) {
        this.issues.add(new HealthIssue(component, description, severity, recommendation));
        return this;
    }
    
    public HealthReport addSystemInfo(String key, Object value) {
        this.systemInfo.put(key, value);
        return this;
    }
    
    public HealthReport withMetrics(long chainLength, double performanceScore, 
                                   long memoryUsage, double diskUsage) {
        this.metrics.setChainLength(chainLength);
        this.metrics.setPerformanceScore(performanceScore);
        this.metrics.setMemoryUsage(memoryUsage);
        this.metrics.setDiskUsage(diskUsage);
        return this;
    }
    
    public HealthReport withReportId(String reportId) {
        this.reportId = reportId;
        return this;
    }
    
    public HealthReport withGenerationTime(Duration generationTime) {
        this.generationTime = generationTime;
        return this;
    }
    
    public HealthReport withHealthMetrics(HealthMetrics metrics) {
        this.metrics.setTotalBlocks(metrics.getTotalBlocks());
        this.metrics.setStorageUsageMB(metrics.getStorageUsageMB());
        this.metrics.setTimestamp(metrics.getTimestamp());
        this.metrics.setAvailableProcessors(metrics.getAvailableProcessors());
        return this;
    }
    
    public HealthReport withRecommendations(List<String> recommendations) {
        this.recommendations = new ArrayList<>(recommendations);
        return this;
    }
    
    public String getFormattedSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("🏥 Health Report Summary\n");
        sb.append("ID: ").append(reportId).append("\n");
        sb.append("Status: ").append(overallHealth.getDisplayName()).append("\n");
        sb.append("Generation Time: ").append(generationTime.toMillis()).append("ms\n");
        sb.append("Issues: ").append(issues.size()).append("\n");
        sb.append("Summary: ").append(summary);
        return sb.toString();
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("🏥 Blockchain Health Report\n");
        sb.append("Overall Health: ").append(overallHealth.getDisplayName()).append("\n");
        sb.append("Summary: ").append(summary).append("\n");
        sb.append("📊 ").append(metrics.toString()).append("\n");
        
        if (!issues.isEmpty()) {
            sb.append("\n🔍 Health Issues:\n");
            issues.forEach(issue -> sb.append("  ").append(issue.toString()).append("\n"));
        }
        
        if (!systemInfo.isEmpty()) {
            sb.append("\n💻 System Information:\n");
            systemInfo.forEach((key, value) -> 
                sb.append("  ").append(key).append(": ").append(value).append("\n"));
        }
        
        sb.append("\n📅 Generated: ").append(timestamp);
        
        return sb.toString();
    }
    
    /**
     * Individual health issue
     */
    public static class HealthIssue {
        private final String component;
        private final String description;
        private final String severity;
        private final String recommendation;
        private final LocalDateTime timestamp;
        
        public HealthIssue(String component, String description, String severity, String recommendation) {
            this.component = component;
            this.description = description;
            this.severity = severity;
            this.recommendation = recommendation;
            this.timestamp = LocalDateTime.now();
        }
        
        public String getComponent() { return component; }
        public String getType() { return component; } // Alias for compatibility
        public String getDescription() { return description; }
        public String getSeverity() { return severity; }
        public String getRecommendation() { return recommendation; }
        public LocalDateTime getTimestamp() { return timestamp; }
        
        @Override
        public String toString() {
            String icon = severity.equals("CRITICAL") ? "❌" : 
                         severity.equals("WARNING") ? "⚠️" : "ℹ️";
            return String.format("%s [%s] %s: %s\n    💡 Recommendation: %s", 
                               icon, severity, component, description, recommendation);
        }
    }
    
    /**
     * Health metrics
     */
    public static class HealthMetrics {
        private long chainLength = 0;
        private double performanceScore = 0.0;
        private long memoryUsage = 0;
        private double diskUsage = 0.0;
        private int totalBlocks = 0;
        private int storageUsageMB = 0;
        private long timestamp = 0;
        private int availableProcessors = 0;
        
        public HealthMetrics() {}
        
        public HealthMetrics(int totalBlocks, int storageUsageMB, long timestamp, int availableProcessors) {
            this.totalBlocks = totalBlocks;
            this.storageUsageMB = storageUsageMB;
            this.timestamp = timestamp;
            this.availableProcessors = availableProcessors;
        }
        
        // Getters and setters
        public long getChainLength() { return chainLength; }
        public void setChainLength(long chainLength) { this.chainLength = chainLength; }
        
        public double getPerformanceScore() { return performanceScore; }
        public void setPerformanceScore(double performanceScore) { this.performanceScore = performanceScore; }
        
        public long getMemoryUsage() { return memoryUsage; }
        public void setMemoryUsage(long memoryUsage) { this.memoryUsage = memoryUsage; }
        
        public double getDiskUsage() { return diskUsage; }
        public void setDiskUsage(double diskUsage) { this.diskUsage = diskUsage; }
        
        public int getTotalBlocks() { return totalBlocks; }
        public void setTotalBlocks(int totalBlocks) { this.totalBlocks = totalBlocks; }
        
        public int getStorageUsageMB() { return storageUsageMB; }
        public void setStorageUsageMB(int storageUsageMB) { this.storageUsageMB = storageUsageMB; }
        
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        
        public int getAvailableProcessors() { return availableProcessors; }
        public void setAvailableProcessors(int availableProcessors) { this.availableProcessors = availableProcessors; }
        
        @Override
        public String toString() {
            if (totalBlocks > 0) {
                return String.format("Blocks: %d, Storage: %dMB, Processors: %d", 
                                   totalBlocks, storageUsageMB, availableProcessors);
            } else {
                return String.format("Chain Length: %d blocks, Performance: %.1f%%, Memory: %dMB, Disk: %.1f%%", 
                                   chainLength, performanceScore, memoryUsage, diskUsage);
            }
        }
    }
}