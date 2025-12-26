package com.rbatllet.blockchain.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Alert Service for structured logging of system alerts and anomalies
 * Provides JSON-formatted alerts for monitoring and observability
 */
public class AlertService {
    
    private static final Logger logger = LoggerFactory.getLogger(AlertService.class);
    private static final Logger alertLogger = LoggerFactory.getLogger("alerts.structured");
    
    private static AlertService instance;
    private final ObjectMapper objectMapper = new ObjectMapper();
    // Java 25 Virtual Threads (Phase 1.3): Use virtual threads for alert logging I/O
    private final ScheduledExecutorService scheduler =
        Executors.newScheduledThreadPool(1, Thread.ofVirtual().factory());
    
    // Alert statistics
    private final AtomicLong totalAlertsGenerated = new AtomicLong(0);
    private final Map<AlertSeverity, AtomicLong> alertCountsBySeverity = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> alertCountsByType = new ConcurrentHashMap<>();
    
    // Alert thresholds configuration
    private static final long SLOW_OPERATION_THRESHOLD_MS = 5000;
    private static final double MEMORY_WARNING_THRESHOLD = 70.0;
    private static final double MEMORY_CRITICAL_THRESHOLD = 85.0;
    private static final double ERROR_RATE_WARNING_THRESHOLD = 5.0;
    private static final double ERROR_RATE_CRITICAL_THRESHOLD = 10.0;
    private static final int HEALTH_SCORE_WARNING_THRESHOLD = 50;
    private static final int HEALTH_SCORE_CRITICAL_THRESHOLD = 25;
    
    private AlertService() {
        // Initialize counters
        for (AlertSeverity severity : AlertSeverity.values()) {
            alertCountsBySeverity.put(severity, new AtomicLong(0));
        }
        
        logger.info("🚨 Alert Service initialized with structured JSON logging");
        
        // Schedule periodic alert summary
        scheduler.scheduleAtFixedRate(this::logAlertSummary, 30, 30, TimeUnit.MINUTES);
    }
    
    public static synchronized AlertService getInstance() {
        if (instance == null) {
            instance = new AlertService();
        }
        return instance;
    }
    
    /**
     * Send performance alert
     */
    public void sendPerformanceAlert(String operationType, long responseTimeMs, double memoryUsageMB) {
        if (responseTimeMs > SLOW_OPERATION_THRESHOLD_MS) {
            alertSlowOperation(operationType, responseTimeMs);
        }
        
        double memoryPercentage = (memoryUsageMB / Runtime.getRuntime().maxMemory()) * 100.0;
        if (memoryPercentage > MEMORY_CRITICAL_THRESHOLD) {
            alertCriticalMemory(operationType, memoryPercentage, memoryUsageMB);
        } else if (memoryPercentage > MEMORY_WARNING_THRESHOLD) {
            alertHighMemory(operationType, memoryPercentage, memoryUsageMB);
        }
    }
    
    /**
     * Send system health alert
     */
    public void sendHealthAlert(double healthScore, double errorRate, String status) {
        if (healthScore <= HEALTH_SCORE_CRITICAL_THRESHOLD) {
            alertCriticalHealth(healthScore, errorRate, status);
        } else if (healthScore <= HEALTH_SCORE_WARNING_THRESHOLD) {
            alertPoorHealth(healthScore, errorRate, status);
        }
        
        if (errorRate >= ERROR_RATE_CRITICAL_THRESHOLD) {
            alertCriticalErrorRate(errorRate);
        } else if (errorRate >= ERROR_RATE_WARNING_THRESHOLD) {
            alertHighErrorRate(errorRate);
        }
    }
    
    /**
     * Send security alert
     */
    public void sendSecurityAlert(String alertType, String details, String sourceIp, String userId) {
        ObjectNode alert = createBaseAlert(AlertSeverity.CRITICAL, AlertType.SECURITY, alertType);
        alert.put("details", details);
        alert.put("source_ip", sourceIp != null ? sourceIp : "unknown");
        alert.put("user_id", userId != null ? userId : "anonymous");
        alert.put("requires_investigation", true);
        
        logAlert(alert);
    }
    
    /**
     * Send blockchain integrity alert
     */
    public void sendIntegrityAlert(String alertType, int blockNumber, String hash, String details) {
        ObjectNode alert = createBaseAlert(AlertSeverity.CRITICAL, AlertType.INTEGRITY, alertType);
        alert.put("block_number", blockNumber);
        alert.put("block_hash", hash);
        alert.put("details", details);
        alert.put("requires_immediate_action", true);
        
        logAlert(alert);
    }
    
    /**
     * Send custom alert
     */
    public void sendCustomAlert(AlertSeverity severity, AlertType type, String alertType, 
                               String message, Map<String, Object> additionalData) {
        ObjectNode alert = createBaseAlert(severity, type, alertType);
        alert.put("message", message);
        
        if (additionalData != null) {
            ObjectNode dataNode = alert.putObject("additional_data");
            additionalData.forEach((key, value) -> {
                if (value instanceof String) {
                    dataNode.put(key, (String) value);
                } else if (value instanceof Number) {
                    dataNode.put(key, value.toString());
                } else if (value instanceof Boolean) {
                    dataNode.put(key, (Boolean) value);
                } else {
                    dataNode.put(key, value.toString());
                }
            });
        }
        
        logAlert(alert);
    }
    
    // Private alert methods
    
    private void alertSlowOperation(String operationType, long responseTimeMs) {
        ObjectNode alert = createBaseAlert(AlertSeverity.WARNING, AlertType.PERFORMANCE, "SLOW_OPERATION");
        alert.put("operation_type", operationType);
        alert.put("response_time_ms", responseTimeMs);
        alert.put("threshold_ms", SLOW_OPERATION_THRESHOLD_MS);
        alert.put("slowness_factor", (double) responseTimeMs / SLOW_OPERATION_THRESHOLD_MS);
        
        logAlert(alert);
    }
    
    private void alertHighMemory(String operationType, double memoryPercentage, double memoryUsageMB) {
        ObjectNode alert = createBaseAlert(AlertSeverity.WARNING, AlertType.PERFORMANCE, "HIGH_MEMORY_USAGE");
        alert.put("operation_type", operationType);
        alert.put("memory_usage_mb", memoryUsageMB);
        alert.put("memory_percentage", memoryPercentage);
        alert.put("threshold_percentage", MEMORY_WARNING_THRESHOLD);
        
        logAlert(alert);
    }
    
    private void alertCriticalMemory(String operationType, double memoryPercentage, double memoryUsageMB) {
        ObjectNode alert = createBaseAlert(AlertSeverity.CRITICAL, AlertType.PERFORMANCE, "CRITICAL_MEMORY_USAGE");
        alert.put("operation_type", operationType);
        alert.put("memory_usage_mb", memoryUsageMB);
        alert.put("memory_percentage", memoryPercentage);
        alert.put("threshold_percentage", MEMORY_CRITICAL_THRESHOLD);
        alert.put("action_required", "immediate_investigation");
        
        logAlert(alert);
    }
    
    private void alertPoorHealth(double healthScore, double errorRate, String status) {
        ObjectNode alert = createBaseAlert(AlertSeverity.WARNING, AlertType.SYSTEM, "POOR_SYSTEM_HEALTH");
        alert.put("health_score", healthScore);
        alert.put("error_rate_percentage", errorRate);
        alert.put("health_status", status);
        alert.put("threshold_score", HEALTH_SCORE_WARNING_THRESHOLD);
        
        logAlert(alert);
    }
    
    private void alertCriticalHealth(double healthScore, double errorRate, String status) {
        ObjectNode alert = createBaseAlert(AlertSeverity.CRITICAL, AlertType.SYSTEM, "CRITICAL_SYSTEM_HEALTH");
        alert.put("health_score", healthScore);
        alert.put("error_rate_percentage", errorRate);
        alert.put("health_status", status);
        alert.put("threshold_score", HEALTH_SCORE_CRITICAL_THRESHOLD);
        alert.put("action_required", "immediate_investigation");
        
        logAlert(alert);
    }
    
    private void alertHighErrorRate(double errorRate) {
        ObjectNode alert = createBaseAlert(AlertSeverity.WARNING, AlertType.SYSTEM, "HIGH_ERROR_RATE");
        alert.put("error_rate_percentage", errorRate);
        alert.put("threshold_percentage", ERROR_RATE_WARNING_THRESHOLD);
        
        logAlert(alert);
    }
    
    private void alertCriticalErrorRate(double errorRate) {
        ObjectNode alert = createBaseAlert(AlertSeverity.CRITICAL, AlertType.SYSTEM, "CRITICAL_ERROR_RATE");
        alert.put("error_rate_percentage", errorRate);
        alert.put("threshold_percentage", ERROR_RATE_CRITICAL_THRESHOLD);
        alert.put("action_required", "immediate_investigation");
        
        logAlert(alert);
    }
    
    // Helper methods
    
    private ObjectNode createBaseAlert(AlertSeverity severity, AlertType type, String alertType) {
        ObjectNode alert = objectMapper.createObjectNode();
        
        // Standard alert fields
        alert.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        alert.put("severity", severity.name());
        alert.put("category", type.name());
        alert.put("alert_type", alertType);
        alert.put("alert_id", generateAlertId());
        alert.put("service", "blockchain-private");
        alert.put("version", "1.0");
        
        // System context
        alert.put("thread_name", Thread.currentThread().getName());
        alert.put("thread_id", Thread.currentThread().threadId());
        
        return alert;
    }
    
    private void logAlert(ObjectNode alert) {
        try {
            String jsonAlert = objectMapper.writeValueAsString(alert);
            
            // Log to structured alerts logger
            String severity = alert.get("severity").asString();
            switch (AlertSeverity.valueOf(severity)) {
                case CRITICAL:
                    alertLogger.error("🚨 {}", jsonAlert);
                    break;
                case WARNING:
                    alertLogger.warn("⚠️ {}", jsonAlert);
                    break;
                case INFO:
                    alertLogger.info("ℹ️ {}", jsonAlert);
                    break;
            }
            
            // Update statistics
            totalAlertsGenerated.incrementAndGet();
            alertCountsBySeverity.get(AlertSeverity.valueOf(severity)).incrementAndGet();
            alertCountsByType.computeIfAbsent(alert.get("alert_type").asString(), k -> new AtomicLong(0)).incrementAndGet();
            
        } catch (Exception e) {
            logger.error("Failed to log structured alert: {}", e.getMessage(), e);
        }
    }
    
    private String generateAlertId() {
        return String.format("alert_%s_%d", 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")),
            System.nanoTime() % 100000);
    }
    
    private void logAlertSummary() {
        try {
            ObjectNode summary = objectMapper.createObjectNode();
            summary.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            summary.put("summary_type", "ALERT_STATISTICS");
            summary.put("total_alerts", totalAlertsGenerated.get());
            
            // Alerts by severity
            ObjectNode severityStats = summary.putObject("alerts_by_severity");
            alertCountsBySeverity.forEach((severity, count) -> 
                severityStats.put(severity.name().toLowerCase(), count.get()));
            
            // Top alert types
            ObjectNode typeStats = summary.putObject("top_alert_types");
            alertCountsByType.entrySet().stream()
                .sorted(Map.Entry.<String, AtomicLong>comparingByValue((a, b) -> Long.compare(b.get(), a.get())))
                .limit(10)
                .forEach(entry -> typeStats.put(entry.getKey(), entry.getValue().get()));
            
            alertLogger.info("📊 {}", objectMapper.writeValueAsString(summary));
            
        } catch (Exception e) {
            logger.error("Failed to log alert summary: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Get alert statistics
     */
    public String getAlertStatistics() {
        try {
            ObjectNode stats = objectMapper.createObjectNode();
            stats.put("total_alerts", totalAlertsGenerated.get());
            
            ObjectNode severityBreakdown = stats.putObject("by_severity");
            alertCountsBySeverity.forEach((severity, count) -> 
                severityBreakdown.put(severity.name().toLowerCase(), count.get()));
            
            ObjectNode typeBreakdown = stats.putObject("by_type");
            alertCountsByType.forEach((type, count) -> 
                typeBreakdown.put(type, count.get()));
            
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(stats);
            
        } catch (Exception e) {
            logger.error("Failed to generate alert statistics: {}", e.getMessage(), e);
            return "Error generating statistics: " + e.getMessage();
        }
    }
    
    /**
     * Reset alert statistics
     */
    public void resetStatistics() {
        totalAlertsGenerated.set(0);
        alertCountsBySeverity.values().forEach(counter -> counter.set(0));
        alertCountsByType.clear();
        logger.info("🔄 Alert statistics reset");
    }
    
    /**
     * Shutdown alert service
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("🛑 Alert Service shutdown completed");
    }
    
    // Enums for alert categorization
    
    public enum AlertSeverity {
        INFO, WARNING, CRITICAL
    }
    
    public enum AlertType {
        PERFORMANCE, SYSTEM, SECURITY, INTEGRITY, CUSTOM
    }
}