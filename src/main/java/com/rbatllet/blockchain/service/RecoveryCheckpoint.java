package com.rbatllet.blockchain.service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Recovery checkpoint for blockchain state preservation
 * Provides point-in-time snapshots for quick recovery operations
 */
public class RecoveryCheckpoint {
    
    public enum CheckpointType {
        AUTOMATIC("🤖 Automatic", "System-generated checkpoint"),
        MANUAL("👤 Manual", "User-created checkpoint"),
        EMERGENCY("🚨 Emergency", "Emergency backup before risky operation"),
        SCHEDULED("📅 Scheduled", "Regularly scheduled checkpoint"),
        PRE_OPERATION("⚙️ Pre-Operation", "Checkpoint before major operation");
        
        private final String displayName;
        private final String description;
        
        CheckpointType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }
    
    public enum CheckpointStatus {
        ACTIVE("✅ Active", "Checkpoint is valid and ready for use"),
        EXPIRED("⌛ Expired", "Checkpoint has exceeded its validity period"),
        CORRUPTED("❌ Corrupted", "Checkpoint data is corrupted"),
        ARCHIVED("📦 Archived", "Checkpoint moved to long-term storage");
        
        private final String displayName;
        private final String description;
        
        CheckpointStatus(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }
    
    private final String checkpointId;
    private final CheckpointType type;
    private final LocalDateTime createdAt;
    private final Long lastBlockNumber;
    private final String lastBlockHash;
    private final long totalBlocks;
    private final Map<String, Object> chainState;
    private final List<String> criticalHashes;
    private final long dataSize;
    private final String description;
    private CheckpointStatus status;
    private LocalDateTime expiresAt;
    
    public RecoveryCheckpoint(String checkpointId, CheckpointType type, String description,
                            Long lastBlockNumber, String lastBlockHash, long totalBlocks) {
        this.checkpointId = checkpointId;
        this.type = type;
        this.description = description;
        this.lastBlockNumber = lastBlockNumber;
        this.lastBlockHash = lastBlockHash;
        this.totalBlocks = totalBlocks;
        this.createdAt = LocalDateTime.now();
        this.chainState = new HashMap<>();
        this.criticalHashes = new ArrayList<>();
        this.dataSize = 0;
        this.status = CheckpointStatus.ACTIVE;
        
        // Set default expiration (30 days for manual, 7 days for automatic)
        this.expiresAt = type == CheckpointType.MANUAL ? 
            createdAt.plusDays(30) : createdAt.plusDays(7);
    }
    
    public RecoveryCheckpoint addChainState(String key, Object value) {
        this.chainState.put(key, value);
        return this;
    }
    
    public RecoveryCheckpoint addCriticalHash(String hash) {
        this.criticalHashes.add(hash);
        return this;
    }
    
    public RecoveryCheckpoint setExpirationDate(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
        return this;
    }
    
    public RecoveryCheckpoint updateStatus(CheckpointStatus newStatus) {
        this.status = newStatus;
        return this;
    }
    
    public boolean isValid() {
        return status == CheckpointStatus.ACTIVE && 
               (expiresAt == null || LocalDateTime.now().isBefore(expiresAt));
    }
    
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
    
    public long getAgeInHours() {
        return java.time.Duration.between(createdAt, LocalDateTime.now()).toHours();
    }
    
    public double getDataSizeMB() {
        return dataSize / (1024.0 * 1024.0);
    }
    
    // Getters
    public String getCheckpointId() { return checkpointId; }
    public CheckpointType getType() { return type; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Long getLastBlockNumber() { return lastBlockNumber; }
    public String getLastBlockHash() { return lastBlockHash; }
    public long getTotalBlocks() { return totalBlocks; }
    public Map<String, Object> getChainState() { return chainState; }
    public List<String> getCriticalHashes() { return criticalHashes; }
    public long getDataSize() { return dataSize; }
    public String getDescription() { return description; }
    public CheckpointStatus getStatus() { return status; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    
    public String getFormattedInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("📍 Recovery Checkpoint\n");
        sb.append("=====================\n\n");
        
        sb.append(String.format("🆔 ID: %s\n", checkpointId));
        sb.append(String.format("📊 Type: %s\n", type.getDisplayName()));
        sb.append(String.format("📝 Description: %s\n", description));
        sb.append(String.format("📈 Status: %s\n", status.getDisplayName()));
        sb.append(String.format("📅 Created: %s\n", createdAt));
        sb.append(String.format("⏰ Age: %d hours\n", getAgeInHours()));
        
        if (expiresAt != null) {
            sb.append(String.format("⌛ Expires: %s\n", expiresAt));
        }
        
        sb.append(String.format("\n🔗 Chain State:\n"));
        sb.append(String.format("  - Last Block: #%d\n", lastBlockNumber));
        sb.append(String.format("  - Last Hash: %s\n", lastBlockHash != null ? 
            lastBlockHash.substring(0, Math.min(16, lastBlockHash.length())) + "..." : "null"));
        sb.append(String.format("  - Total Blocks: %d\n", totalBlocks));
        sb.append(String.format("  - Critical Hashes: %d\n", criticalHashes.size()));
        sb.append(String.format("  - Data Size: %.2f MB\n", getDataSizeMB()));
        
        if (!chainState.isEmpty()) {
            sb.append(String.format("\n⚙️ Chain State Properties: %d\n", chainState.size()));
        }
        
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return String.format("RecoveryCheckpoint{id='%s', type=%s, block=#%d, status=%s}", 
                           checkpointId, type, lastBlockNumber, status);
    }
}