package com.rbatllet.blockchain.logging;

import com.rbatllet.blockchain.service.SearchMetrics;
import com.rbatllet.blockchain.service.PerformanceMetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Central logging manager that coordinates all logging services
 * Provides unified interface for advanced logging capabilities
 */
public class LoggingManager {
    
    private static final Logger logger = LoggerFactory.getLogger(LoggingManager.class);
    
    // Service state
    private static final AtomicBoolean isInitialized = new AtomicBoolean(false);
    private static final AtomicBoolean isStarted = new AtomicBoolean(false);
    private static volatile ScheduledExecutorService scheduler;
    
    // Metrics integration
    private static volatile SearchMetrics searchMetrics;
    private static volatile PerformanceMetricsService performanceMetrics;
    
    // Configuration
    private static final long METRICS_REPORT_INTERVAL_MINUTES = 30;
    private static final long HEALTH_CHECK_INTERVAL_MINUTES = 5;
    
    /**
     * Initialize the logging manager
     */
    public static synchronized void initialize() {
        if (isInitialized.compareAndSet(false, true)) {
            logger.info("🚀 Initializing Advanced Logging Manager");
            
            // Initialize search metrics if available
            try {
                searchMetrics = new SearchMetrics();
                logger.info("✅ Search metrics integration enabled");
            } catch (Exception e) {
                logger.warn("⚠️ Search metrics integration failed: {}", e.getMessage());
            }
            
            // Initialize performance metrics service
            try {
                performanceMetrics = PerformanceMetricsService.getInstance();
                logger.info("✅ Performance metrics service initialized");
            } catch (Exception e) {
                logger.warn("⚠️ Performance metrics service initialization failed: {}", e.getMessage());
            }
            
            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(LoggingManager::shutdown));
            
            logger.info("✅ Advanced Logging Manager initialized");
        }
    }
    
    /**
     * Start the logging manager with scheduled tasks
     */
    public static synchronized void start() {
        if (!isInitialized.get()) {
            initialize();
        }
        
        if (isStarted.compareAndSet(false, true)) {
            logger.info("🚀 Starting Advanced Logging Manager");
            
            // Create scheduler
            // Java 25 Virtual Threads (Phase 2.2): Use virtual threads for log file I/O operations
            scheduler = Executors.newScheduledThreadPool(2, Thread.ofVirtual().factory());
            
            // Schedule periodic metrics reporting
            scheduler.scheduleAtFixedRate(
                LoggingManager::generatePeriodicReport,
                METRICS_REPORT_INTERVAL_MINUTES,
                METRICS_REPORT_INTERVAL_MINUTES,
                TimeUnit.MINUTES
            );
            
            // Schedule health checks
            scheduler.scheduleAtFixedRate(
                LoggingManager::performHealthCheck,
                HEALTH_CHECK_INTERVAL_MINUTES,
                HEALTH_CHECK_INTERVAL_MINUTES,
                TimeUnit.MINUTES
            );
            
            logger.info("✅ Advanced Logging Manager started");
        }
    }
    
    /**
     * Stop the logging manager
     */
    public static synchronized void shutdown() {
        if (isStarted.compareAndSet(true, false)) {
            logger.info("⏹️ Shutting down Advanced Logging Manager");
            
            // Generate final report
            generateFinalReport();
            
            // Shutdown scheduler
            if (scheduler != null && !scheduler.isShutdown()) {
                scheduler.shutdown();
                try {
                    if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                        scheduler.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    scheduler.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
            
            logger.info("✅ Advanced Logging Manager shutdown completed");
        }
    }
    
    /**
     * Log blockchain operation with comprehensive tracking
     * @param operationType Operation type
     * @param operationName Operation name
     * @param blockNumber Block number if applicable
     * @param dataSize Data size processed
     * @param methodCall Method to execute
     * @return Method result
     */
    public static <T> T logBlockchainOperation(String operationType, String operationName, 
                                              Long blockNumber, long dataSize, 
                                              OperationLoggingInterceptor.MethodCall<T> methodCall) {
        Map<String, String> context = new HashMap<>();
        if (blockNumber != null) {
            context.put("blockNumber", String.valueOf(blockNumber));
        }
        context.put("dataSize", String.valueOf(dataSize));
        
        String operationId = AdvancedLoggingService.startOperation(operationType, operationName, context);
        long startTime = System.currentTimeMillis();
        
        try {
            T result = methodCall.call();
            
            // Calculate metrics
            long duration = System.currentTimeMillis() - startTime;
            int resultCount = getResultCount(result);
            
            // Record performance metrics
            if (performanceMetrics != null) {
                performanceMetrics.recordResponseTime(operationType, duration);
                performanceMetrics.recordOperation(operationType, true);
                performanceMetrics.recordThroughput(operationType, resultCount);
                
                // Estimate memory usage based on data size
                long estimatedMemoryMB = dataSize / 1024 / 1024;
                if (estimatedMemoryMB > 0) {
                    performanceMetrics.recordMemoryUsage(operationType, estimatedMemoryMB);
                }
            }
            
            AdvancedLoggingService.endOperation(operationId, true, resultCount, null);
            
            return result;
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            
            // Record failed operation metrics
            if (performanceMetrics != null) {
                performanceMetrics.recordResponseTime(operationType, duration);
                performanceMetrics.recordOperation(operationType, false);
            }
            
            AdvancedLoggingService.endOperation(operationId, false, 0, "Error: " + e.getMessage());
            throw new RuntimeException("Blockchain operation failed: " + operationName, e);
        }
    }
    
    /**
     * Log encryption/decryption operations
     * @param operation Operation type (ENCRYPT/DECRYPT)
     * @param algorithm Algorithm used
     * @param dataSize Size of data processed
     * @param methodCall Method to execute
     * @return Method result
     */
    public static <T> T logCryptoOperation(String operation, String algorithm, long dataSize,
                                          OperationLoggingInterceptor.MethodCall<T> methodCall) {
        Map<String, String> context = new HashMap<>();
        context.put("algorithm", algorithm);
        context.put("dataSize", String.valueOf(dataSize));
        
        String operationId = AdvancedLoggingService.startOperation("CRYPTO_" + operation, operation, context);
        
        try {
            T result = methodCall.call();
            
            AdvancedLoggingService.endOperation(operationId, true, 1, "Algorithm: " + algorithm);
            
            return result;
            
        } catch (Exception e) {
            AdvancedLoggingService.endOperation(operationId, false, 0, "Error: " + e.getMessage());
            throw new RuntimeException("Crypto operation failed: " + operation, e);
        }
    }
    
    /**
     * Log search operations with integration to SearchMetrics
     * @param searchType Search type
     * @param query Search query (sanitized)
     * @param methodCall Method to execute
     * @return Method result
     */
    public static <T> T logSearchOperation(String searchType, String query,
                                          OperationLoggingInterceptor.MethodCall<T> methodCall) {
        Map<String, String> context = new HashMap<>();
        context.put("searchType", searchType);
        context.put("query", sanitizeSearchQuery(query));
        
        String operationId = AdvancedLoggingService.startOperation("SEARCH", searchType, context);
        long startTime = System.currentTimeMillis();
        
        try {
            T result = methodCall.call();
            
            long duration = System.currentTimeMillis() - startTime;
            int resultCount = getResultCount(result);
            
            // Record in search metrics if available
            if (searchMetrics != null) {
                searchMetrics.recordSearch(searchType, duration, resultCount, false);
            }
            
            // Record performance metrics
            if (performanceMetrics != null) {
                performanceMetrics.recordResponseTime("SEARCH_" + searchType, duration);
                performanceMetrics.recordOperation("SEARCH_" + searchType, true);
                performanceMetrics.recordThroughput("SEARCH_" + searchType, resultCount);
            }
            
            AdvancedLoggingService.endOperation(operationId, true, resultCount, null);
            
            return result;
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            
            // Record failed search
            if (searchMetrics != null) {
                searchMetrics.recordSearch(searchType, duration, 0, false);
            }
            
            // Record failed operation metrics
            if (performanceMetrics != null) {
                performanceMetrics.recordResponseTime("SEARCH_" + searchType, duration);
                performanceMetrics.recordOperation("SEARCH_" + searchType, false);
            }
            
            AdvancedLoggingService.endOperation(operationId, false, 0, "Error: " + e.getMessage());
            throw new RuntimeException("Search operation failed: " + searchType, e);
        }
    }
    
    /**
     * Log key management operations
     * @param operation Key operation type
     * @param keyType Key type
     * @param keyId Key identifier
     * @param methodCall Method to execute
     * @return Method result
     */
    public static <T> T logKeyOperation(String operation, String keyType, String keyId,
                                       OperationLoggingInterceptor.MethodCall<T> methodCall) {
        Map<String, String> context = new HashMap<>();
        context.put("keyType", keyType);
        context.put("keyId", keyId);
        
        String operationId = AdvancedLoggingService.startOperation("KEY_" + operation, operation, context);
        
        try {
            T result = methodCall.call();
            
            AdvancedLoggingService.endOperation(operationId, true, 1, "KeyType: " + keyType);
            
            // Log as security event
            AdvancedLoggingService.logSecurityEvent(
                "KEY_" + operation,
                "Key operation: " + operation + " for " + keyType,
                AdvancedLoggingService.SecuritySeverity.MEDIUM,
                "system"
            );
            
            return result;
            
        } catch (Exception e) {
            AdvancedLoggingService.endOperation(operationId, false, 0, "Error: " + e.getMessage());
            
            // Log as security event
            AdvancedLoggingService.logSecurityEvent(
                "KEY_" + operation + "_FAILED",
                "Key operation failed: " + operation + " for " + keyType + " - " + e.getMessage(),
                AdvancedLoggingService.SecuritySeverity.HIGH,
                "system"
            );
            
            throw new RuntimeException("Key operation failed: " + operation, e);
        }
    }
    
    /**
     * Get comprehensive system report
     * @return System report string
     */
    public static String getSystemReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("🔍 COMPREHENSIVE SYSTEM LOGGING REPORT\n");
        sb.append("=".repeat(50)).append("\n");
        sb.append("Generated: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        sb.append("Logging Manager Status: ").append(isStarted.get() ? "RUNNING" : "STOPPED").append("\n\n");
        
        // Advanced logging metrics
        sb.append(AdvancedLoggingService.getPerformanceReport()).append("\n");
        
        // Performance metrics if available
        if (performanceMetrics != null) {
            sb.append("🚀 PERFORMANCE METRICS:\n");
            sb.append(performanceMetrics.getPerformanceReport()).append("\n");
        }
        
        // Search metrics if available
        if (searchMetrics != null) {
            sb.append("🔍 SEARCH METRICS:\n");
            sb.append(searchMetrics.getPerformanceReport()).append("\n");
        }
        
        // System health
        sb.append("🏥 SYSTEM HEALTH:\n");
        sb.append("  Memory Usage: ").append(getMemoryUsageString()).append("\n");
        sb.append("  Thread Count: ").append(Thread.activeCount()).append("\n");
        sb.append("  Scheduler Status: ").append(scheduler != null && !scheduler.isShutdown() ? "ACTIVE" : "INACTIVE").append("\n");
        
        return sb.toString();
    }
    
    /**
     * Get system health summary
     * @return Health summary string
     */
    public static String getSystemHealthSummary() {
        if (performanceMetrics != null) {
            return performanceMetrics.getSystemHealthSummary();
        } else {
            return "🏥 SYSTEM HEALTH SUMMARY\n" +
                   "─".repeat(30) + "\n" +
                   "Performance Metrics: DISABLED\n" +
                   "Status: BASIC MONITORING\n";
        }
    }
    
    // Private helper methods
    
    private static void generatePeriodicReport() {
        try {
            logger.info("📊 PERIODIC LOGGING REPORT:\n{}", getSystemReport());
        } catch (Exception e) {
            logger.error("❌ Error generating periodic report", e);
        }
    }
    
    private static void performHealthCheck() {
        try {
            // Check memory usage
            long memoryUsage = getMemoryUsageInMB();
            if (memoryUsage > 500) { // 500MB threshold
                logger.warn("⚠️ High memory usage detected: {}MB", memoryUsage);
            }
            
            // Check active threads
            int threadCount = Thread.activeCount();
            if (threadCount > 100) { // 100 threads threshold
                logger.warn("⚠️ High thread count detected: {}", threadCount);
            }
            
            logger.debug("✅ Health check completed - Memory: {}MB, Threads: {}", memoryUsage, threadCount);
            
        } catch (Exception e) {
            logger.error("❌ Health check failed", e);
        }
    }
    
    private static void generateFinalReport() {
        try {
            logger.info("📋 FINAL LOGGING REPORT:\n{}", getSystemReport());
        } catch (Exception e) {
            logger.error("❌ Error generating final report", e);
        }
    }
    
    private static String sanitizeSearchQuery(String query) {
        if (query == null) return "null";
        
        // Truncate long queries
        if (query.length() > 100) {
            return query.substring(0, 100) + "...";
        }
        
        // Remove potentially sensitive content
        return query.replaceAll("(?i)password|secret|key", "[PROTECTED]");
    }
    
    private static int getResultCount(Object result) {
        if (result == null) return 0;
        
        if (result instanceof Collection) {
            return ((Collection<?>) result).size();
        }
        
        if (result instanceof Object[]) {
            return ((Object[]) result).length;
        }
        
        return 1;
    }
    
    private static long getMemoryUsageInMB() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        return usedMemory / (1024 * 1024);
    }
    
    private static String getMemoryUsageString() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        
        return String.format("%.1fMB / %.1fMB (%.1f%%)", 
                           usedMemory / (1024.0 * 1024.0),
                           maxMemory / (1024.0 * 1024.0),
                           (double) usedMemory / maxMemory * 100);
    }
}