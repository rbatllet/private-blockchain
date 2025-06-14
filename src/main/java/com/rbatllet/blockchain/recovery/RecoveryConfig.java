package com.rbatllet.blockchain.recovery;

/**
 * Configuration class for blockchain recovery operations
 * Provides configurable parameters for recovery strategies and behavior
 */
public class RecoveryConfig {
    
    // Strategy enablement flags
    private boolean reauthorizationEnabled = true;
    private boolean rollbackEnabled = true;
    private boolean partialExportEnabled = true;
    private boolean autoRecoveryEnabled = true;
    
    // Logging and audit configuration
    private boolean auditLoggingEnabled = true;
    private String auditLogFile = "recovery_audit.log";
    private boolean verboseLogging = false;
    
    // Recovery behavior parameters
    private String recoveryVersion = "2.0";
    private int maxOwnerNameLength = 100;
    private int maxRecoveryAttempts = 3;
    private long recoveryTimeoutMs = 30000;
    
    // Export configuration
    private String backupDirectory = "chain_backups";
    private String recoveryOwnerSuffix = "RECOVERED";
    private boolean includeMetadataInExport = true;
    
    // Rollback configuration
    private int maxRollbackBlocks = 100;
    private double rollbackSafetyMargin = 0.1; // 10% safety margin
    
    // Default constructor
    public RecoveryConfig() {}
    
    // Builder pattern for easy configuration
    public static class Builder {
        private RecoveryConfig config = new RecoveryConfig();
        
        public Builder enableReauthorization(boolean enabled) {
            config.reauthorizationEnabled = enabled;
            return this;
        }
        
        public Builder enableRollback(boolean enabled) {
            config.rollbackEnabled = enabled;
            return this;
        }
        
        public Builder enablePartialExport(boolean enabled) {
            config.partialExportEnabled = enabled;
            return this;
        }
        
        public Builder enableAutoRecovery(boolean enabled) {
            config.autoRecoveryEnabled = enabled;
            return this;
        }
        
        public Builder withAuditLogging(boolean enabled, String logFile) {
            config.auditLoggingEnabled = enabled;
            config.auditLogFile = logFile;
            return this;
        }
        
        public Builder withVerboseLogging(boolean enabled) {
            config.verboseLogging = enabled;
            return this;
        }
        
        public Builder withRecoveryLimits(int maxAttempts, long timeoutMs) {
            config.maxRecoveryAttempts = maxAttempts;
            config.recoveryTimeoutMs = timeoutMs;
            return this;
        }
        
        public Builder withBackupDirectory(String directory) {
            config.backupDirectory = directory;
            return this;
        }
        
        public Builder withRollbackLimits(int maxBlocks, double safetyMargin) {
            config.maxRollbackBlocks = maxBlocks;
            config.rollbackSafetyMargin = safetyMargin;
            return this;
        }
        
        public RecoveryConfig build() {
            return config;
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    // Preset configurations
    public static RecoveryConfig conservativeConfig() {
        return new Builder()
            .enableReauthorization(true)
            .enableRollback(false) // Conservative: no rollback
            .enablePartialExport(true)
            .enableAutoRecovery(false) // Manual only
            .withRecoveryLimits(1, 10000) // Single attempt, short timeout
            .build();
    }
    
    public static RecoveryConfig aggressiveConfig() {
        return new Builder()
            .enableReauthorization(true)
            .enableRollback(true)
            .enablePartialExport(true)
            .enableAutoRecovery(true)
            .withRecoveryLimits(5, 60000) // Multiple attempts, longer timeout
            .withRollbackLimits(200, 0.05) // Allow more rollback, smaller safety margin
            .build();
    }
    
    public static RecoveryConfig productionConfig() {
        return new Builder()
            .enableReauthorization(true)
            .enableRollback(true)
            .enablePartialExport(true)
            .enableAutoRecovery(true)
            .withAuditLogging(true, "production_recovery_audit.log")
            .withVerboseLogging(false)
            .withRecoveryLimits(3, 30000)
            .build();
    }
    
    // Getters and setters
    public boolean isReauthorizationEnabled() { return reauthorizationEnabled; }
    public void setReauthorizationEnabled(boolean reauthorizationEnabled) { 
        this.reauthorizationEnabled = reauthorizationEnabled; 
    }
    
    public boolean isRollbackEnabled() { return rollbackEnabled; }
    public void setRollbackEnabled(boolean rollbackEnabled) { 
        this.rollbackEnabled = rollbackEnabled; 
    }
    
    public boolean isPartialExportEnabled() { return partialExportEnabled; }
    public void setPartialExportEnabled(boolean partialExportEnabled) { 
        this.partialExportEnabled = partialExportEnabled; 
    }
    
    public boolean isAutoRecoveryEnabled() { return autoRecoveryEnabled; }
    public void setAutoRecoveryEnabled(boolean autoRecoveryEnabled) { 
        this.autoRecoveryEnabled = autoRecoveryEnabled; 
    }
    
    public boolean isAuditLoggingEnabled() { return auditLoggingEnabled; }
    public void setAuditLoggingEnabled(boolean auditLoggingEnabled) { 
        this.auditLoggingEnabled = auditLoggingEnabled; 
    }
    
    public String getAuditLogFile() { return auditLogFile; }
    public void setAuditLogFile(String auditLogFile) { 
        this.auditLogFile = auditLogFile; 
    }
    
    public boolean isVerboseLogging() { return verboseLogging; }
    public void setVerboseLogging(boolean verboseLogging) { 
        this.verboseLogging = verboseLogging; 
    }
    
    public String getRecoveryVersion() { return recoveryVersion; }
    public void setRecoveryVersion(String recoveryVersion) { 
        this.recoveryVersion = recoveryVersion; 
    }
    
    public int getMaxOwnerNameLength() { return maxOwnerNameLength; }
    public void setMaxOwnerNameLength(int maxOwnerNameLength) { 
        this.maxOwnerNameLength = maxOwnerNameLength; 
    }
    
    public int getMaxRecoveryAttempts() { return maxRecoveryAttempts; }
    public void setMaxRecoveryAttempts(int maxRecoveryAttempts) { 
        this.maxRecoveryAttempts = maxRecoveryAttempts; 
    }
    
    public long getRecoveryTimeoutMs() { return recoveryTimeoutMs; }
    public void setRecoveryTimeoutMs(long recoveryTimeoutMs) { 
        this.recoveryTimeoutMs = recoveryTimeoutMs; 
    }
    
    public String getBackupDirectory() { return backupDirectory; }
    public void setBackupDirectory(String backupDirectory) { 
        this.backupDirectory = backupDirectory; 
    }
    
    public String getRecoveryOwnerSuffix() { return recoveryOwnerSuffix; }
    public void setRecoveryOwnerSuffix(String recoveryOwnerSuffix) { 
        this.recoveryOwnerSuffix = recoveryOwnerSuffix; 
    }
    
    public boolean isIncludeMetadataInExport() { return includeMetadataInExport; }
    public void setIncludeMetadataInExport(boolean includeMetadataInExport) { 
        this.includeMetadataInExport = includeMetadataInExport; 
    }
    
    public int getMaxRollbackBlocks() { return maxRollbackBlocks; }
    public void setMaxRollbackBlocks(int maxRollbackBlocks) { 
        this.maxRollbackBlocks = maxRollbackBlocks; 
    }
    
    public double getRollbackSafetyMargin() { return rollbackSafetyMargin; }
    public void setRollbackSafetyMargin(double rollbackSafetyMargin) { 
        this.rollbackSafetyMargin = rollbackSafetyMargin; 
    }
    
    // Validation method
    public boolean isValid() {
        return maxRecoveryAttempts > 0 && 
               recoveryTimeoutMs > 0 && 
               maxOwnerNameLength > 0 && 
               maxRollbackBlocks >= 0 && 
               rollbackSafetyMargin >= 0.0 && rollbackSafetyMargin <= 1.0;
    }
    
    @Override
    public String toString() {
        return String.format("RecoveryConfig{reauth=%s, rollback=%s, export=%s, auto=%s, version=%s}", 
                           reauthorizationEnabled, rollbackEnabled, partialExportEnabled, 
                           autoRecoveryEnabled, recoveryVersion);
    }
}