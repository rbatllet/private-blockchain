package com.rbatllet.blockchain.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Advanced performance metrics service for comprehensive system monitoring
 * Provides detailed response times, memory usage, and system performance tracking
 */
public class PerformanceMetricsService {
    
    private static final Logger logger = LoggerFactory.getLogger(PerformanceMetricsService.class);
    private static final Logger performanceLogger = LoggerFactory.getLogger("performance.metrics");
    
    private static PerformanceMetricsService instance;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    
    // Performance thresholds (configurable)
    private static final long SLOW_OPERATION_THRESHOLD_MS = 5000;  // 5 seconds
    private static final long MEMORY_WARNING_THRESHOLD_MB = 512;   // 512 MB
    private static final long MEMORY_CRITICAL_THRESHOLD_MB = 1024; // 1 GB
    
    // Metrics storage
    private final Map<String, ResponseTimeMetrics> responseTimeMetrics = new ConcurrentHashMap<>();
    private final Map<String, MemoryUsageMetrics> memoryMetrics = new ConcurrentHashMap<>();
    private final Map<String, ThroughputMetrics> throughputMetrics = new ConcurrentHashMap<>();
    private final List<PerformanceAlert> activeAlerts = new java.util.concurrent.CopyOnWriteArrayList<>();
    
    // System metrics
    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    private final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
    private final AtomicLong totalOperations = new AtomicLong(0);
    private final AtomicLong totalErrors = new AtomicLong(0);
    private final long startTime = System.currentTimeMillis();
    
    private PerformanceMetricsService() {
        logger.info("🚀 Performance Metrics Service initialized");
    }
    
    public static synchronized PerformanceMetricsService getInstance() {
        if (instance == null) {
            instance = new PerformanceMetricsService();
        }
        return instance;
    }
    
    /**
     * Record response time for an operation
     */
    public void recordResponseTime(String operationType, long responseTimeMs) {
        // Use concurrent structures to avoid locks where possible
        ResponseTimeMetrics metrics = responseTimeMetrics.computeIfAbsent(
            operationType, k -> new ResponseTimeMetrics()
        );
        metrics.recordTime(responseTimeMs);
        
        // Check for slow operations
        if (responseTimeMs > SLOW_OPERATION_THRESHOLD_MS) {
            createPerformanceAlert(
                "SLOW_OPERATION",
                "Operation " + operationType + " took " + responseTimeMs + "ms",
                AlertSeverity.WARNING
            );
        }
        
        // Log to performance metrics logger
        performanceLogger.info("📊 RESPONSE_TIME [{}] - {}ms", operationType, responseTimeMs);
    }
    
    /**
     * Record memory usage for an operation
     */
    public void recordMemoryUsage(String operationType, long memoryUsedMB) {
        // Use concurrent structures to avoid locks where possible
        MemoryUsageMetrics metrics = memoryMetrics.computeIfAbsent(
            operationType, k -> new MemoryUsageMetrics()
        );
        metrics.recordUsage(memoryUsedMB);
        
        // Check memory thresholds
        if (memoryUsedMB > MEMORY_CRITICAL_THRESHOLD_MB) {
            createPerformanceAlert(
                "CRITICAL_MEMORY_USAGE",
                "Operation " + operationType + " used " + memoryUsedMB + "MB",
                AlertSeverity.CRITICAL
            );
        } else if (memoryUsedMB > MEMORY_WARNING_THRESHOLD_MB) {
            createPerformanceAlert(
                "HIGH_MEMORY_USAGE",
                "Operation " + operationType + " used " + memoryUsedMB + "MB",
                AlertSeverity.WARNING
            );
        }
        
        // Log to performance metrics logger
        performanceLogger.info("📊 MEMORY_USAGE [{}] - {}MB", operationType, memoryUsedMB);
    }
    
    /**
     * Record throughput for an operation
     */
    public void recordThroughput(String operationType, int itemsProcessed) {
        // Use concurrent structures to avoid locks where possible
        ThroughputMetrics metrics = throughputMetrics.computeIfAbsent(
            operationType, k -> new ThroughputMetrics()
        );
        metrics.recordThroughput(itemsProcessed);
        
        // Log to performance metrics logger
        performanceLogger.info("📊 THROUGHPUT [{}] - {} items", operationType, itemsProcessed);
    }
    
    /**
     * Record operation completion
     */
    public void recordOperation(String operationType, boolean success) {
        totalOperations.incrementAndGet();
        if (!success) {
            totalErrors.incrementAndGet();
        }
        
        // Log to performance metrics logger
        performanceLogger.info("📊 OPERATION [{}] - {}", operationType, success ? "SUCCESS" : "FAILURE");
    }
    
    /**
     * Get comprehensive performance report
     */
    public String getPerformanceReport() {
        lock.readLock().lock();
        try {
            StringBuilder report = new StringBuilder();
            report.append("🚀 COMPREHENSIVE PERFORMANCE METRICS REPORT\n");
            report.append("=" .repeat(60)).append("\n");
            report.append("Generated: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
            report.append("Uptime: ").append(formatDuration(System.currentTimeMillis() - startTime)).append("\n\n");
            
            // System overview
            report.append("📊 SYSTEM OVERVIEW\n");
            report.append("─" .repeat(30)).append("\n");
            report.append("Total Operations: ").append(totalOperations.get()).append("\n");
            report.append("Total Errors: ").append(totalErrors.get()).append("\n");
            report.append("Error Rate: ").append(String.format("%.2f%%", getErrorRate())).append("\n");
            report.append("Active Threads: ").append(threadBean.getThreadCount()).append("\n");
            report.append("Peak Threads: ").append(threadBean.getPeakThreadCount()).append("\n\n");
            
            // Memory information
            report.append("💾 MEMORY METRICS\n");
            report.append("─" .repeat(30)).append("\n");
            MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
            report.append("Heap Used: ").append(heapUsage.getUsed() / 1024 / 1024).append("MB\n");
            report.append("Heap Max: ").append(heapUsage.getMax() / 1024 / 1024).append("MB\n");
            report.append("Heap Utilization: ").append(String.format("%.2f%%", (double)heapUsage.getUsed() / heapUsage.getMax() * 100)).append("\n\n");
            
            // Response time metrics
            if (!responseTimeMetrics.isEmpty()) {
                report.append("⏱️ RESPONSE TIME METRICS\n");
                report.append("─" .repeat(30)).append("\n");
                responseTimeMetrics.forEach((operation, metrics) -> {
                    report.append("  ").append(operation).append(":\n");
                    report.append("    Count: ").append(metrics.getCount()).append("\n");
                    report.append("    Avg: ").append(metrics.getAverageTime()).append("ms\n");
                    report.append("    Min: ").append(metrics.getMinTime()).append("ms\n");
                    report.append("    Max: ").append(metrics.getMaxTime()).append("ms\n");
                    report.append("    95th percentile: ").append(metrics.get95thPercentile()).append("ms\n");
                });
                report.append("\n");
            }
            
            // Memory usage metrics
            if (!memoryMetrics.isEmpty()) {
                report.append("💾 MEMORY USAGE BY OPERATION\n");
                report.append("─" .repeat(30)).append("\n");
                memoryMetrics.forEach((operation, metrics) -> {
                    report.append("  ").append(operation).append(":\n");
                    report.append("    Avg: ").append(metrics.getAverageUsage()).append("MB\n");
                    report.append("    Min: ").append(metrics.getMinUsage()).append("MB\n");
                    report.append("    Max: ").append(metrics.getMaxUsage()).append("MB\n");
                    report.append("    Peak: ").append(metrics.getPeakUsage()).append("MB\n");
                });
                report.append("\n");
            }
            
            // Throughput metrics
            if (!throughputMetrics.isEmpty()) {
                report.append("⚡ THROUGHPUT METRICS\n");
                report.append("─" .repeat(30)).append("\n");
                throughputMetrics.forEach((operation, metrics) -> {
                    report.append("  ").append(operation).append(":\n");
                    report.append("    Total Items: ").append(metrics.getTotalItems()).append("\n");
                    report.append("    Avg/Operation: ").append(metrics.getAverageItemsPerOperation()).append("\n");
                    report.append("    Peak/Operation: ").append(metrics.getPeakItemsPerOperation()).append("\n");
                });
                report.append("\n");
            }
            
            // Active alerts
            if (!activeAlerts.isEmpty()) {
                report.append("⚠️ ACTIVE PERFORMANCE ALERTS\n");
                report.append("─" .repeat(30)).append("\n");
                activeAlerts.forEach(alert -> {
                    report.append("  ").append(alert.getSeverity()).append(" [").append(alert.getType()).append("]: ").append(alert.getMessage()).append("\n");
                    report.append("    Time: ").append(alert.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss"))).append("\n");
                });
                report.append("\n");
            }
            
            return report.toString();
            
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Get system health summary
     */
    public String getSystemHealthSummary() {
        lock.readLock().lock();
        try {
            StringBuilder summary = new StringBuilder();
            summary.append("🏥 SYSTEM HEALTH SUMMARY\n");
            summary.append("─" .repeat(30)).append("\n");
            
            // Calculate health score
            double healthScore = calculateHealthScore();
            String healthStatus = getHealthStatus(healthScore);
            
            summary.append("Health Score: ").append(String.format("%.1f/100", healthScore)).append("\n");
            summary.append("Status: ").append(healthStatus).append("\n");
            summary.append("Error Rate: ").append(String.format("%.2f%%", getErrorRate())).append("\n");
            summary.append("Memory Usage: ").append(getCurrentMemoryUsage()).append("MB\n");
            summary.append("Active Alerts: ").append(activeAlerts.size()).append("\n");
            
            return summary.toString();
            
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Get alerts summary by type
     */
    public String getAlertsSummary() {
        if (activeAlerts.isEmpty()) {
            return "🟢 No active performance alerts";
        }
        
        StringBuilder summary = new StringBuilder();
        summary.append("⚠️ PERFORMANCE ALERTS SUMMARY\n");
        summary.append("─" .repeat(30)).append("\n");
        
        // Group alerts by type
        Map<String, Long> alertsByType = activeAlerts.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                PerformanceAlert::getType,
                java.util.stream.Collectors.counting()
            ));
        
        alertsByType.forEach((type, count) -> {
            summary.append("  ").append(type).append(": ").append(count).append(" alerts\n");
        });
        
        // Group by severity
        Map<AlertSeverity, Long> alertsBySeverity = activeAlerts.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                PerformanceAlert::getSeverity,
                java.util.stream.Collectors.counting()
            ));
        
        summary.append("\nBy Severity:\n");
        alertsBySeverity.forEach((severity, count) -> {
            summary.append("  ").append(severity).append(": ").append(count).append(" alerts\n");
        });
        
        return summary.toString();
    }
    
    /**
     * Reset all metrics
     */
    public void resetMetrics() {
        lock.writeLock().lock();
        try {
            responseTimeMetrics.clear();
            memoryMetrics.clear();
            throughputMetrics.clear();
            activeAlerts.clear();
            totalOperations.set(0);
            totalErrors.set(0);
            
            logger.info("🔄 Performance metrics reset");
            
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    // Helper methods
    private void createPerformanceAlert(String type, String message, AlertSeverity severity) {
        PerformanceAlert alert = new PerformanceAlert(type, message, severity);
        
        // Use CopyOnWriteArrayList to avoid locking
        activeAlerts.add(alert);
        
        // Keep only last 50 alerts (simple cleanup without locking)
        if (activeAlerts.size() > 50) {
            // Remove oldest alerts
            while (activeAlerts.size() > 50) {
                if (!activeAlerts.isEmpty()) {
                    activeAlerts.remove(0);
                }
            }
        }
        
        // Log the alert
        switch (severity) {
            case CRITICAL:
                logger.error("🚨 CRITICAL PERFORMANCE ALERT: {}", message);
                break;
            case WARNING:
                logger.warn("⚠️ PERFORMANCE WARNING: {}", message);
                break;
            case INFO:
                logger.info("ℹ️ PERFORMANCE INFO: {}", message);
                break;
        }
    }
    
    private double getErrorRate() {
        long total = totalOperations.get();
        return total > 0 ? (double) totalErrors.get() / total * 100 : 0.0;
    }
    
    private long getCurrentMemoryUsage() {
        return memoryBean.getHeapMemoryUsage().getUsed() / 1024 / 1024;
    }
    
    private double calculateHealthScore() {
        double score = 100.0;
        
        // Deduct for error rate
        double errorRate = getErrorRate();
        if (errorRate > 10) score -= 30;
        else if (errorRate > 5) score -= 20;
        else if (errorRate > 1) score -= 10;
        
        // Deduct for memory usage
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        double memoryUtilization = (double) heapUsage.getUsed() / heapUsage.getMax();
        if (memoryUtilization > 0.9) score -= 25;
        else if (memoryUtilization > 0.8) score -= 15;
        else if (memoryUtilization > 0.7) score -= 10;
        
        // Deduct for active alerts
        long criticalAlerts = activeAlerts.stream()
            .filter(alert -> alert.getSeverity() == AlertSeverity.CRITICAL)
            .count();
        score -= criticalAlerts * 15;
        
        long warningAlerts = activeAlerts.stream()
            .filter(alert -> alert.getSeverity() == AlertSeverity.WARNING)
            .count();
        score -= warningAlerts * 5;
        
        // Additional deduction for specific alert types
        long slowOperationAlerts = activeAlerts.stream()
            .filter(alert -> "SLOW_OPERATION".equals(alert.getType()))
            .count();
        score -= slowOperationAlerts * 5;
        
        long memoryAlerts = activeAlerts.stream()
            .filter(alert -> alert.getType().contains("MEMORY"))
            .count();
        score -= memoryAlerts * 8;
        
        return Math.max(0, score);
    }
    
    private String getHealthStatus(double healthScore) {
        if (healthScore >= 90) return "🟢 EXCELLENT";
        if (healthScore >= 75) return "🟡 GOOD";
        if (healthScore >= 50) return "🟠 FAIR";
        if (healthScore >= 25) return "🔴 POOR";
        return "🚨 CRITICAL";
    }
    
    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }
    
    // Inner classes for metrics storage
    private static class ResponseTimeMetrics {
        private final LongAdder count = new LongAdder();
        private final LongAdder totalTime = new LongAdder();
        private volatile long minTime = Long.MAX_VALUE;
        private volatile long maxTime = Long.MIN_VALUE;
        private final List<Long> recentTimes = new ArrayList<>();
        
        synchronized void recordTime(long timeMs) {
            count.increment();
            totalTime.add(timeMs);
            
            if (timeMs < minTime) minTime = timeMs;
            if (timeMs > maxTime) maxTime = timeMs;
            
            recentTimes.add(timeMs);
            if (recentTimes.size() > 100) {
                recentTimes.remove(0);
            }
        }
        
        long getCount() { return count.sum(); }
        long getAverageTime() { 
            long total = totalTime.sum();
            long cnt = count.sum();
            return cnt > 0 ? total / cnt : 0; 
        }
        long getMinTime() { return minTime == Long.MAX_VALUE ? 0 : minTime; }
        long getMaxTime() { return maxTime == Long.MIN_VALUE ? 0 : maxTime; }
        
        synchronized long get95thPercentile() {
            if (recentTimes.isEmpty()) return 0;
            List<Long> sorted = new ArrayList<>(recentTimes);
            sorted.sort(null);
            int index = (int) (sorted.size() * 0.95);
            return sorted.get(Math.min(index, sorted.size() - 1));
        }
    }
    
    private static class MemoryUsageMetrics {
        private final LongAdder totalUsage = new LongAdder();
        private final LongAdder count = new LongAdder();
        private volatile long minUsage = Long.MAX_VALUE;
        private volatile long maxUsage = Long.MIN_VALUE;
        private volatile long peakUsage = 0;
        
        synchronized void recordUsage(long usageMB) {
            count.increment();
            totalUsage.add(usageMB);
            
            if (usageMB < minUsage) minUsage = usageMB;
            if (usageMB > maxUsage) maxUsage = usageMB;
            if (usageMB > peakUsage) peakUsage = usageMB;
        }
        
        long getAverageUsage() { 
            long total = totalUsage.sum();
            long cnt = count.sum();
            return cnt > 0 ? total / cnt : 0; 
        }
        long getMinUsage() { return minUsage == Long.MAX_VALUE ? 0 : minUsage; }
        long getMaxUsage() { return maxUsage == Long.MIN_VALUE ? 0 : maxUsage; }
        long getPeakUsage() { return peakUsage; }
    }
    
    private static class ThroughputMetrics {
        private final LongAdder totalItems = new LongAdder();
        private final LongAdder operationCount = new LongAdder();
        private volatile long peakItems = 0;
        
        synchronized void recordThroughput(int items) {
            operationCount.increment();
            totalItems.add(items);
            
            if (items > peakItems) {
                peakItems = items;
            }
        }
        
        long getTotalItems() { return totalItems.sum(); }
        long getAverageItemsPerOperation() { 
            long total = totalItems.sum();
            long ops = operationCount.sum();
            return ops > 0 ? total / ops : 0; 
        }
        long getPeakItemsPerOperation() { return peakItems; }
    }
    
    private static class PerformanceAlert {
        private final String type;
        private final String message;
        private final AlertSeverity severity;
        private final LocalDateTime timestamp;
        
        PerformanceAlert(String type, String message, AlertSeverity severity) {
            this.type = type;
            this.message = message;
            this.severity = severity;
            this.timestamp = LocalDateTime.now();
        }
        
        String getType() { return type; }
        String getMessage() { return message; }
        AlertSeverity getSeverity() { return severity; }
        LocalDateTime getTimestamp() { return timestamp; }
    }
    
    private enum AlertSeverity {
        INFO, WARNING, CRITICAL
    }
}