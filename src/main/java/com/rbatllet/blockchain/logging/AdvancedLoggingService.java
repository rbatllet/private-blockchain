package com.rbatllet.blockchain.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Advanced logging service with operation tracking, performance monitoring, and detailed metrics
 * Provides comprehensive logging capabilities for blockchain operations
 */
public class AdvancedLoggingService {
    
    private static final Logger logger = LoggerFactory.getLogger(AdvancedLoggingService.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    // Operation tracking
    private static final Map<String, OperationContext> activeOperations = new ConcurrentHashMap<>();
    private static final Map<String, OperationMetrics> operationMetrics = new ConcurrentHashMap<>();
    private static final AtomicLong operationCounter = new AtomicLong(0);
    
    // Performance thresholds (configurable)
    private static final long SLOW_OPERATION_THRESHOLD_MS = 5000; // 5 seconds
    private static final long VERY_SLOW_OPERATION_THRESHOLD_MS = 15000; // 15 seconds
    private static final long MEMORY_USAGE_THRESHOLD_MB = 100; // 100MB
    
    /**
     * Start tracking an operation
     * @param operationType Type of operation (e.g., "BLOCK_CREATION", "SEARCH", "ENCRYPTION")
     * @param operationName Specific operation name
     * @param context Additional context information
     * @return Operation ID for tracking
     */
    public static String startOperation(String operationType, String operationName, Map<String, String> context) {
        String operationId = generateOperationId(operationType);
        
        OperationContext opContext = new OperationContext(
            operationId, operationType, operationName, 
            System.currentTimeMillis(), context
        );
        
        activeOperations.put(operationId, opContext);
        
        // Set MDC for correlation
        MDC.put("operationId", operationId);
        MDC.put("operationType", operationType);
        MDC.put("operationName", operationName);
        
        logger.info("🚀 {} started - {} [{}]", operationType, operationName, operationId);
        
        return operationId;
    }
    
    /**
     * End an operation and record metrics
     * @param operationId Operation ID returned from startOperation
     * @param success Whether operation succeeded
     * @param resultCount Number of results/items processed
     * @param additionalInfo Additional information to log
     */
    public static void endOperation(String operationId, boolean success, int resultCount, String additionalInfo) {
        OperationContext context = activeOperations.remove(operationId);
        if (context == null) {
            logger.warn("⚠️ Attempted to end unknown operation: {}", operationId);
            return;
        }
        
        long duration = System.currentTimeMillis() - context.getStartTime();
        long memoryUsed = getMemoryUsageInMB();
        
        // Record metrics
        recordOperationMetrics(context.getOperationType(), duration, success, resultCount, memoryUsed);
        
        // Log completion
        String status = success ? "✅" : "❌";
        String durationStr = formatDuration(duration);
        
        logger.info("{} {} completed - {} [{}] - Duration: {}, Results: {}, Memory: {}MB{}",
                   status, context.getOperationType(), context.getOperationName(), 
                   operationId, durationStr, resultCount, memoryUsed,
                   additionalInfo != null ? ", " + additionalInfo : "");
        
        // Check for performance issues
        checkPerformanceThresholds(context, duration, memoryUsed);
        
        // Clear MDC
        MDC.clear();
    }
    
    /**
     * Log operation progress
     * @param operationId Operation ID
     * @param progress Progress percentage (0-100)
     * @param message Progress message
     */
    public static void logProgress(String operationId, int progress, String message) {
        OperationContext context = activeOperations.get(operationId);
        if (context == null) {
            logger.warn("⚠️ Attempted to log progress for unknown operation: {}", operationId);
            return;
        }
        
        long elapsed = System.currentTimeMillis() - context.getStartTime();
        logger.info("📊 {} progress: {}% - {} [{}] - Elapsed: {}", 
                   context.getOperationType(), progress, message, operationId, formatDuration(elapsed));
    }
    
    /**
     * Log performance metrics for specific operation
     * @param operationType Type of operation
     * @param operationName Operation name
     * @param duration Duration in milliseconds
     * @param dataSize Size of data processed
     * @param details Additional performance details
     */
    public static void logPerformanceMetrics(String operationType, String operationName, 
                                           long duration, long dataSize, Map<String, Object> details) {
        StringBuilder sb = new StringBuilder();
        sb.append("📈 Performance - ").append(operationType).append(" [").append(operationName).append("]");
        sb.append(" - Duration: ").append(formatDuration(duration));
        sb.append(", Data: ").append(formatDataSize(dataSize));
        
        if (details != null && !details.isEmpty()) {
            sb.append(", Details: ");
            details.forEach((key, value) -> sb.append(key).append("=").append(value).append(" "));
        }
        
        logger.info(sb.toString());
    }
    
    /**
     * Log security event
     * @param eventType Security event type
     * @param description Event description
     * @param severity Severity level (HIGH, MEDIUM, LOW)
     * @param userId User ID if applicable
     */
    public static void logSecurityEvent(String eventType, String description, 
                                      SecuritySeverity severity, String userId) {
        String icon = getSecurityIcon(severity);
        
        MDC.put("eventType", eventType);
        MDC.put("severity", severity.name());
        if (userId != null) {
            MDC.put("userId", userId);
        }
        
        switch (severity) {
            case HIGH:
                logger.error("{} SECURITY {} - {} - User: {}", icon, eventType, description, userId);
                break;
            case MEDIUM:
                logger.warn("{} SECURITY {} - {} - User: {}", icon, eventType, description, userId);
                break;
            case LOW:
                logger.info("{} SECURITY {} - {} - User: {}", icon, eventType, description, userId);
                break;
        }
        
        MDC.clear();
    }
    
    /**
     * Log database operation
     * @param operation Database operation (SELECT, INSERT, UPDATE, DELETE)
     * @param table Table name
     * @param duration Duration in milliseconds
     * @param rowsAffected Number of rows affected
     */
    public static void logDatabaseOperation(String operation, String table, long duration, int rowsAffected) {
        String icon = duration > 1000 ? "⚠️" : "📋";
        
        logger.info("{} DB {} {} - Duration: {}ms, Rows: {}", 
                   icon, operation, table, duration, rowsAffected);
        
        // Record metrics
        recordOperationMetrics("DATABASE_" + operation, duration, true, rowsAffected, 0);
        
        // Warn about slow queries
        if (duration > 5000) {
            logger.warn("🐌 Slow database query detected: {} {} took {}ms", operation, table, duration);
        }
    }
    
    /**
     * Log memory usage and cleanup events
     * @param event Memory event type
     * @param beforeMB Memory before operation
     * @param afterMB Memory after operation
     * @param details Additional details
     */
    public static void logMemoryEvent(String event, long beforeMB, long afterMB, String details) {
        long freed = beforeMB - afterMB;
        String icon = freed > 0 ? "🧹" : "📊";
        
        logger.info("{} MEMORY {} - Before: {}MB, After: {}MB, Freed: {}MB - {}", 
                   icon, event, beforeMB, afterMB, freed, details);
        
        // Alert on high memory usage
        if (afterMB > MEMORY_USAGE_THRESHOLD_MB) {
            logger.warn("⚠️ High memory usage detected: {}MB", afterMB);
        }
    }
    
    /**
     * Get comprehensive performance report
     * @return Performance report string
     */
    public static String getPerformanceReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("📊 Advanced Logging Performance Report\n");
        sb.append("Generated: ").append(LocalDateTime.now().format(TIMESTAMP_FORMAT)).append("\n");
        sb.append("Active Operations: ").append(activeOperations.size()).append("\n\n");
        
        sb.append("📈 Operation Metrics:\n");
        operationMetrics.forEach((type, metrics) -> {
            sb.append("  ").append(type).append(": ").append(metrics.toString()).append("\n");
        });
        
        sb.append("\n🔄 Active Operations:\n");
        activeOperations.forEach((id, context) -> {
            long elapsed = System.currentTimeMillis() - context.getStartTime();
            sb.append("  ").append(context.getOperationType()).append(" [").append(context.getOperationId()).append("]")
              .append(" - Running: ").append(formatDuration(elapsed));
            
            // Add context information if available
            Map<String, String> contextInfo = context.getContext();
            if (contextInfo != null && !contextInfo.isEmpty()) {
                sb.append(" - Context: ");
                contextInfo.forEach((key, value) -> sb.append(key).append("=").append(value).append(" "));
            }
            sb.append("\n");
        });
        
        return sb.toString();
    }
    
    /**
     * Clear all metrics and reset counters
     */
    public static void resetMetrics() {
        operationMetrics.clear();
        activeOperations.clear();
        operationCounter.set(0);
        logger.info("🔄 Advanced logging metrics reset");
    }
    
    // Private helper methods
    
    private static String generateOperationId(String operationType) {
        return operationType + "_" + operationCounter.incrementAndGet() + "_" + System.currentTimeMillis();
    }
    
    private static void recordOperationMetrics(String operationType, long duration, boolean success, 
                                             int resultCount, long memoryUsed) {
        operationMetrics.computeIfAbsent(operationType, k -> new OperationMetrics())
                        .recordOperation(duration, success, resultCount, memoryUsed);
    }
    
    private static void checkPerformanceThresholds(OperationContext context, long duration, long memoryUsed) {
        if (duration > VERY_SLOW_OPERATION_THRESHOLD_MS) {
            logger.error("🐌 VERY SLOW operation detected: {} took {}ms", 
                        context.getOperationType(), duration);
        } else if (duration > SLOW_OPERATION_THRESHOLD_MS) {
            logger.warn("⚠️ Slow operation detected: {} took {}ms", 
                       context.getOperationType(), duration);
        }
        
        if (memoryUsed > MEMORY_USAGE_THRESHOLD_MB) {
            logger.warn("⚠️ High memory usage during {}: {}MB", 
                       context.getOperationType(), memoryUsed);
        }
    }
    
    private static String formatDuration(long milliseconds) {
        if (milliseconds < 1000) {
            return milliseconds + "ms";
        } else if (milliseconds < 60000) {
            return String.format("%.1fs", milliseconds / 1000.0);
        } else {
            return String.format("%.1fm", milliseconds / 60000.0);
        }
    }
    
    private static String formatDataSize(long bytes) {
        if (bytes < 1024) {
            return bytes + "B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1fKB", bytes / 1024.0);
        } else {
            return String.format("%.1fMB", bytes / (1024.0 * 1024.0));
        }
    }
    
    private static long getMemoryUsageInMB() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        return usedMemory / (1024 * 1024);
    }
    
    private static String getSecurityIcon(SecuritySeverity severity) {
        switch (severity) {
            case HIGH: return "🔴";
            case MEDIUM: return "🟡";
            case LOW: return "🟢";
            default: return "🔐";
        }
    }
    
    /**
     * Security severity levels
     */
    public enum SecuritySeverity {
        HIGH, MEDIUM, LOW
    }
    
    /**
     * Operation context for tracking
     */
    private static class OperationContext {
        private final String operationId;
        private final String operationType;
        private final String operationName;
        private final long startTime;
        private final Map<String, String> context;
        
        public OperationContext(String operationId, String operationType, String operationName, 
                               long startTime, Map<String, String> context) {
            this.operationId = operationId;
            this.operationType = operationType;
            this.operationName = operationName;
            this.startTime = startTime;
            this.context = context != null ? new HashMap<>(context) : new HashMap<>();
        }
        
        // Getters
        public String getOperationId() { return operationId; }
        public String getOperationType() { return operationType; }
        public String getOperationName() { return operationName; }
        public long getStartTime() { return startTime; }
        public Map<String, String> getContext() { return context; }
    }
    
    /**
     * Operation metrics tracking
     */
    private static class OperationMetrics {
        private final AtomicLong totalOperations = new AtomicLong(0);
        private final AtomicLong successfulOperations = new AtomicLong(0);
        private final AtomicLong totalDuration = new AtomicLong(0);
        private final AtomicLong totalResults = new AtomicLong(0);
        private final AtomicLong totalMemoryUsed = new AtomicLong(0);
        private volatile long minDuration = Long.MAX_VALUE;
        private volatile long maxDuration = 0;
        
        public void recordOperation(long duration, boolean success, int resultCount, long memoryUsed) {
            totalOperations.incrementAndGet();
            totalDuration.addAndGet(duration);
            totalResults.addAndGet(resultCount);
            totalMemoryUsed.addAndGet(memoryUsed);
            
            if (success) {
                successfulOperations.incrementAndGet();
            }
            
            if (duration < minDuration) minDuration = duration;
            if (duration > maxDuration) maxDuration = duration;
        }
        
        public double getSuccessRate() {
            long total = totalOperations.get();
            return total > 0 ? (double) successfulOperations.get() / total * 100 : 0;
        }
        
        public double getAverageDuration() {
            long total = totalOperations.get();
            return total > 0 ? (double) totalDuration.get() / total : 0;
        }
        
        public double getAverageResults() {
            long total = totalOperations.get();
            return total > 0 ? (double) totalResults.get() / total : 0;
        }
        
        public double getAverageMemoryUsed() {
            long total = totalOperations.get();
            return total > 0 ? (double) totalMemoryUsed.get() / total : 0;
        }
        
        @Override
        public String toString() {
            return String.format("%d ops, %.1f%% success, %.1fms avg (%.1f-%.1fms), %.1f results avg, %.1fMB memory avg",
                               totalOperations.get(), getSuccessRate(), getAverageDuration(),
                               minDuration == Long.MAX_VALUE ? 0.0 : (double)minDuration, (double)maxDuration,
                               getAverageResults(), getAverageMemoryUsed());
        }
    }
}