package com.rbatllet.blockchain.service;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

/**
 * Comprehensive validation report for blockchain health and integrity
 * Provides detailed analysis of chain validation, block integrity, and off-chain data
 */
public class ValidationReport {
    
    private final boolean valid;
    private final String summary;
    private final LocalDateTime timestamp;
    private final List<ValidationIssue> issues;
    private final ValidationMetrics metrics;
    private final Map<String, Object> details;
    private String validationId;
    private Duration validationTime;
    private double validationScore;
    private int errorCount;
    
    public ValidationReport(boolean valid, String summary) {
        this.valid = valid;
        this.summary = summary;
        this.timestamp = LocalDateTime.now();
        this.issues = new ArrayList<>();
        this.metrics = new ValidationMetrics();
        this.details = new HashMap<>();
        this.validationId = "validation_" + System.currentTimeMillis();
        this.validationTime = Duration.ZERO;
        this.validationScore = 0.0;
        this.errorCount = 0;
    }
    
    // Getters
    public boolean isValid() { return valid; }
    public String getSummary() { return summary; }
    public String getMessage() { return summary; } // Alias for compatibility
    public LocalDateTime getTimestamp() { return timestamp; }
    public List<ValidationIssue> getIssues() { return Collections.unmodifiableList(issues); }
    public ValidationMetrics getMetrics() { return metrics; }
    public ValidationMetrics getValidationMetrics() { return metrics; } // Alias for compatibility
    public Map<String, Object> getDetails() { return Collections.unmodifiableMap(details); }
    public String getValidationId() { return validationId; }
    public Duration getValidationTime() { return validationTime; }
    public double getValidationScore() { return validationScore; }
    public int getErrorCount() { return errorCount; }
    
    // Builder methods
    public ValidationReport addIssue(String type, String description, String severity) {
        this.issues.add(new ValidationIssue(type, description, severity));
        return this;
    }
    
    public ValidationReport addDetail(String key, Object value) {
        this.details.put(key, value);
        return this;
    }
    
    public ValidationReport withMetrics(long totalBlocks, long validBlocks, long invalidBlocks, 
                                       long offChainFiles, long corruptFiles) {
        this.metrics.setTotalBlocks(totalBlocks);
        this.metrics.setValidBlocks(validBlocks);
        this.metrics.setInvalidBlocks(invalidBlocks);
        this.metrics.setOffChainFiles(offChainFiles);
        this.metrics.setCorruptFiles(corruptFiles);
        return this;
    }
    
    public ValidationReport withValidationId(String validationId) {
        this.validationId = validationId;
        return this;
    }
    
    public ValidationReport withValidationTime(Duration validationTime) {
        this.validationTime = validationTime;
        return this;
    }
    
    public ValidationReport withValidationScore(double validationScore) {
        this.validationScore = validationScore;
        return this;
    }
    
    public ValidationReport withErrorCount(int errorCount) {
        this.errorCount = errorCount;
        return this;
    }
    
    public ValidationReport withValidationMetrics(ValidationMetrics metrics) {
        this.metrics.setTotalChecks(metrics.getTotalChecks());
        this.metrics.setPassedChecks(metrics.getPassedChecks());
        this.metrics.setFailedChecks(metrics.getFailedChecks());
        this.metrics.setValidationDuration(metrics.getValidationDuration());
        return this;
    }
    
    public String getFormattedSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("📋 Validation Summary\n");
        sb.append("ID: ").append(validationId).append("\n");
        sb.append("Status: ").append(valid ? "✅ Valid" : "❌ Invalid").append("\n");
        sb.append("Score: ").append(String.format("%.2f", validationScore)).append("\n");
        sb.append("Duration: ").append(validationTime.toMillis()).append("ms\n");
        sb.append("Errors: ").append(errorCount).append("\n");
        sb.append("Summary: ").append(summary);
        return sb.toString();
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("🔍 Comprehensive Validation Report\n");
        sb.append("Overall Status: ").append(valid ? "✅ Valid" : "❌ Invalid").append("\n");
        sb.append("Summary: ").append(summary).append("\n");
        sb.append("📊 ").append(metrics.toString()).append("\n");
        
        if (!issues.isEmpty()) {
            sb.append("⚠️ Issues Found:\n");
            issues.forEach(issue -> sb.append("  ").append(issue.toString()).append("\n"));
        }
        
        if (!details.isEmpty()) {
            sb.append("📝 Additional Details:\n");
            details.forEach((key, value) -> 
                sb.append("  ").append(key).append(": ").append(value).append("\n"));
        }
        
        sb.append("📅 Generated: ").append(timestamp);
        
        return sb.toString();
    }
    
    /**
     * Individual validation issue
     */
    public static class ValidationIssue {
        private final String type;
        private final String description;
        private final String severity;
        private final LocalDateTime timestamp;
        
        public ValidationIssue(String type, String description, String severity) {
            this.type = type;
            this.description = description;
            this.severity = severity;
            this.timestamp = LocalDateTime.now();
        }
        
        public String getType() { return type; }
        public String getDescription() { return description; }
        public String getSeverity() { return severity; }
        public LocalDateTime getTimestamp() { return timestamp; }
        
        @Override
        public String toString() {
            String icon = severity.equals("ERROR") ? "❌" : 
                         severity.equals("WARNING") ? "⚠️" : "ℹ️";
            return String.format("%s [%s] %s: %s", icon, severity, type, description);
        }
    }
    
    /**
     * Validation metrics
     */
    public static class ValidationMetrics {
        private long totalBlocks = 0;
        private long validBlocks = 0;
        private long invalidBlocks = 0;
        private long offChainFiles = 0;
        private long corruptFiles = 0;
        private long totalChecks = 0;
        private long passedChecks = 0;
        private long failedChecks = 0;
        private long validationDuration = 0;
        
        public ValidationMetrics() {}
        
        public ValidationMetrics(long totalChecks, long passedChecks, long failedChecks, long validationDuration) {
            this.totalChecks = totalChecks;
            this.passedChecks = passedChecks;
            this.failedChecks = failedChecks;
            this.validationDuration = validationDuration;
        }
        
        // Getters and setters
        public long getTotalBlocks() { return totalBlocks; }
        public void setTotalBlocks(long totalBlocks) { this.totalBlocks = totalBlocks; }
        
        public long getValidBlocks() { return validBlocks; }
        public void setValidBlocks(long validBlocks) { this.validBlocks = validBlocks; }
        
        public long getInvalidBlocks() { return invalidBlocks; }
        public void setInvalidBlocks(long invalidBlocks) { this.invalidBlocks = invalidBlocks; }
        
        public long getOffChainFiles() { return offChainFiles; }
        public void setOffChainFiles(long offChainFiles) { this.offChainFiles = offChainFiles; }
        
        public long getCorruptFiles() { return corruptFiles; }
        public void setCorruptFiles(long corruptFiles) { this.corruptFiles = corruptFiles; }
        
        public long getTotalChecks() { return totalChecks; }
        public void setTotalChecks(long totalChecks) { this.totalChecks = totalChecks; }
        
        public long getPassedChecks() { return passedChecks; }
        public void setPassedChecks(long passedChecks) { this.passedChecks = passedChecks; }
        
        public long getFailedChecks() { return failedChecks; }
        public void setFailedChecks(long failedChecks) { this.failedChecks = failedChecks; }
        
        public long getValidationDuration() { return validationDuration; }
        public void setValidationDuration(long validationDuration) { this.validationDuration = validationDuration; }
        
        public double getValidationSuccessRate() {
            return totalBlocks > 0 ? (double) validBlocks / totalBlocks * 100 : 0;
        }
        
        public double getOffChainIntegrityRate() {
            return offChainFiles > 0 ? (double) (offChainFiles - corruptFiles) / offChainFiles * 100 : 100;
        }
        
        @Override
        public String toString() {
            if (totalChecks > 0) {
                return String.format("Metrics: %d checks (%d passed, %d failed), %dms", 
                                   totalChecks, passedChecks, failedChecks, validationDuration);
            } else {
                return String.format("Metrics: %d blocks (%d valid, %d invalid), %d off-chain files (%d corrupt)", 
                                   totalBlocks, validBlocks, invalidBlocks, offChainFiles, corruptFiles);
            }
        }
    }
}