package com.rbatllet.blockchain.service;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

/**
 * Result container for key management operations
 * Provides detailed information about hierarchical key setup and operations
 */
public class KeyManagementResult {
    
    private final boolean success;
    private final String message;
    private final String rootKeyId;
    private final String intermediateKeyId;
    private final String operationalKeyId;
    private final String generatedKeyId; // For single key operations
    private final LocalDateTime timestamp;
    private final List<String> warnings;
    private final KeyStatistics statistics;
    private Duration operationDuration;
    private final Map<String, Object> details;
    
    public KeyManagementResult(boolean success, String message, String rootKeyId, 
                              String intermediateKeyId, String operationalKeyId) {
        this.success = success;
        this.message = message;
        this.rootKeyId = rootKeyId;
        this.intermediateKeyId = intermediateKeyId;
        this.operationalKeyId = operationalKeyId;
        this.generatedKeyId = null;
        this.timestamp = LocalDateTime.now();
        this.warnings = new ArrayList<>();
        this.statistics = new KeyStatistics();
        this.operationDuration = Duration.ZERO;
        this.details = new HashMap<>();
    }
    
    // Constructor for single key operations
    public KeyManagementResult(boolean success, String message, String generatedKeyId) {
        this.success = success;
        this.message = message;
        this.rootKeyId = null;
        this.intermediateKeyId = null;
        this.operationalKeyId = null;
        this.generatedKeyId = generatedKeyId;
        this.timestamp = LocalDateTime.now();
        this.warnings = new ArrayList<>();
        this.statistics = new KeyStatistics();
        this.operationDuration = Duration.ZERO;
        this.details = new HashMap<>();
    }
    
    // Getters
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getRootKeyId() { return rootKeyId; }
    public String getIntermediateKeyId() { return intermediateKeyId; }
    public String getOperationalKeyId() { return operationalKeyId; }
    public String getGeneratedKeyId() { return generatedKeyId; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public List<String> getWarnings() { return Collections.unmodifiableList(warnings); }
    public KeyStatistics getStatistics() { return statistics; }
    public KeyStatistics getKeyStatistics() { return statistics; } // Alias for compatibility
    public Duration getOperationDuration() { return operationDuration; }
    public Map<String, Object> getDetails() { return Collections.unmodifiableMap(details); }
    
    // Builder methods
    public KeyManagementResult addWarning(String warning) {
        this.warnings.add(warning);
        return this;
    }
    
    public KeyManagementResult withStatistics(int totalKeys, int activeKeys, int expiredKeys) {
        this.statistics.setTotalKeys(totalKeys);
        this.statistics.setActiveKeys(activeKeys);
        this.statistics.setExpiredKeys(expiredKeys);
        return this;
    }
    
    public KeyManagementResult withStatistics(KeyStatistics stats) {
        this.statistics.setTotalKeysGenerated(stats.getTotalKeysGenerated());
        this.statistics.setKeyStrength(stats.getKeyStrength());
        this.statistics.setGenerationTime(stats.getGenerationTime());
        this.statistics.setAlgorithm(stats.getAlgorithm());
        return this;
    }
    
    public KeyManagementResult withOperationDuration(Duration duration) {
        this.operationDuration = duration;
        return this;
    }
    
    public KeyManagementResult addDetail(String key, Object value) {
        this.details.put(key, value);
        return this;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("🔑 Key Management Result\n");
        sb.append("Status: ").append(success ? "✅ Success" : "❌ Failed").append("\n");
        sb.append("Message: ").append(message).append("\n");
        
        if (rootKeyId != null) {
            sb.append("Root Key: ").append(rootKeyId).append("\n");
        }
        if (intermediateKeyId != null) {
            sb.append("Intermediate Key: ").append(intermediateKeyId).append("\n");
        }
        if (operationalKeyId != null) {
            sb.append("Operational Key: ").append(operationalKeyId).append("\n");
        }
        
        if (!warnings.isEmpty()) {
            sb.append("⚠️ Warnings:\n");
            warnings.forEach(w -> sb.append("  - ").append(w).append("\n"));
        }
        
        sb.append("📊 ").append(statistics.toString());
        sb.append("📅 Timestamp: ").append(timestamp);
        
        return sb.toString();
    }
    
    /**
     * Inner class for key statistics
     */
    public static class KeyStatistics {
        private int totalKeys = 0;
        private int activeKeys = 0;
        private int expiredKeys = 0;
        private int totalKeysGenerated = 0;
        private int keyStrength = 256;
        private long generationTime = 0;
        private String algorithm = "ECDSA";
        
        public KeyStatistics() {}
        
        public KeyStatistics(int totalKeysGenerated, int keyStrength, long generationTime, String algorithm) {
            this.totalKeysGenerated = totalKeysGenerated;
            this.keyStrength = keyStrength;
            this.generationTime = generationTime;
            this.algorithm = algorithm;
        }
        
        // Getters and setters
        public int getTotalKeys() { return totalKeys; }
        public void setTotalKeys(int totalKeys) { this.totalKeys = totalKeys; }
        
        public int getActiveKeys() { return activeKeys; }
        public void setActiveKeys(int activeKeys) { this.activeKeys = activeKeys; }
        
        public int getExpiredKeys() { return expiredKeys; }
        public void setExpiredKeys(int expiredKeys) { this.expiredKeys = expiredKeys; }
        
        public int getTotalKeysGenerated() { return totalKeysGenerated; }
        public void setTotalKeysGenerated(int totalKeysGenerated) { this.totalKeysGenerated = totalKeysGenerated; }
        
        public int getKeyStrength() { return keyStrength; }
        public void setKeyStrength(int keyStrength) { this.keyStrength = keyStrength; }
        
        public long getGenerationTime() { return generationTime; }
        public void setGenerationTime(long generationTime) { this.generationTime = generationTime; }
        
        public String getAlgorithm() { return algorithm; }
        public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
        
        @Override
        public String toString() {
            if (totalKeysGenerated > 0) {
                return String.format("Statistics: %d generated, %d-bit strength, %dms, %s\n", 
                                   totalKeysGenerated, keyStrength, generationTime, algorithm);
            } else {
                return String.format("Statistics: %d total, %d active, %d expired\n", 
                                   totalKeys, activeKeys, expiredKeys);
            }
        }
    }
}