package com.rbatllet.blockchain.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Data Transfer Object for exporting encryption keys and context
 * Enables secure export/import of encrypted blockchain data
 */
public class EncryptionExportData {
    
    private final Map<Long, String> offChainPasswords;
    private final Map<Long, String> blockEncryptionKeys;
    private final Map<String, String> userEncryptionKeys;
    private String masterPassword;
    private LocalDateTime timestamp;
    private String version;
    private boolean isEncrypted;
    
    public EncryptionExportData() {
        this.offChainPasswords = new ConcurrentHashMap<>();
        this.blockEncryptionKeys = new ConcurrentHashMap<>();
        this.userEncryptionKeys = new ConcurrentHashMap<>();
        this.timestamp = LocalDateTime.now();
        this.version = "1.0";
        this.isEncrypted = false;
    }
    
    /**
     * Add off-chain password for a specific block
     */
    public void addOffChainPassword(Long blockNumber, String password) {
        if (blockNumber != null && password != null) {
            offChainPasswords.put(blockNumber, password);
        }
    }
    
    /**
     * Add block encryption key for a specific block
     */
    public void addBlockEncryptionKey(Long blockNumber, String encryptionKey) {
        if (blockNumber != null && encryptionKey != null) {
            blockEncryptionKeys.put(blockNumber, encryptionKey);
        }
    }
    
    /**
     * Add user encryption key
     */
    public void addUserEncryptionKey(String keyId, String encryptionKey) {
        if (keyId != null && encryptionKey != null) {
            userEncryptionKeys.put(keyId, encryptionKey);
        }
    }
    
    /**
     * Get off-chain password for a specific block
     */
    @JsonIgnore
    public String getOffChainPassword(Long blockNumber) {
        return offChainPasswords.get(blockNumber);
    }
    
    /**
     * Get block encryption key for a specific block
     */
    @JsonIgnore
    public String getBlockEncryptionKey(Long blockNumber) {
        return blockEncryptionKeys.get(blockNumber);
    }
    
    /**
     * Get user encryption key
     */
    @JsonIgnore
    public String getUserEncryptionKey(String keyId) {
        return userEncryptionKeys.get(keyId);
    }
    
    /**
     * Check if encryption data is available for a block
     */
    @JsonIgnore
    public boolean hasEncryptionDataForBlock(Long blockNumber) {
        return offChainPasswords.containsKey(blockNumber) || 
               blockEncryptionKeys.containsKey(blockNumber);
    }
    
    /**
     * Check if export data is empty
     */
    @JsonIgnore
    public boolean isEmpty() {
        return offChainPasswords.isEmpty() && 
               blockEncryptionKeys.isEmpty() && 
               userEncryptionKeys.isEmpty();
    }
    
    /**
     * Get total count of encryption entries
     */
    @JsonIgnore
    public int getTotalEncryptionEntries() {
        return offChainPasswords.size() + 
               blockEncryptionKeys.size() + 
               userEncryptionKeys.size();
    }
    
    // Getters and Setters
    
    public Map<Long, String> getOffChainPasswords() {
        return offChainPasswords;
    }
    
    public void setOffChainPasswords(Map<Long, String> offChainPasswords) {
        this.offChainPasswords.clear();
        if (offChainPasswords != null) {
            this.offChainPasswords.putAll(offChainPasswords);
        }
    }
    
    public Map<Long, String> getBlockEncryptionKeys() {
        return blockEncryptionKeys;
    }
    
    public void setBlockEncryptionKeys(Map<Long, String> blockEncryptionKeys) {
        this.blockEncryptionKeys.clear();
        if (blockEncryptionKeys != null) {
            this.blockEncryptionKeys.putAll(blockEncryptionKeys);
        }
    }
    
    public Map<String, String> getUserEncryptionKeys() {
        return userEncryptionKeys;
    }
    
    public void setUserEncryptionKeys(Map<String, String> userEncryptionKeys) {
        this.userEncryptionKeys.clear();
        if (userEncryptionKeys != null) {
            this.userEncryptionKeys.putAll(userEncryptionKeys);
        }
    }
    
    public String getMasterPassword() {
        return masterPassword;
    }
    
    public void setMasterPassword(String masterPassword) {
        this.masterPassword = masterPassword;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    public boolean isEncrypted() {
        return isEncrypted;
    }
    
    public void setEncrypted(boolean encrypted) {
        isEncrypted = encrypted;
    }
    
    @Override
    public String toString() {
        return "EncryptionExportData{" +
                "offChainPasswords=" + offChainPasswords.size() +
                ", blockEncryptionKeys=" + blockEncryptionKeys.size() +
                ", userEncryptionKeys=" + userEncryptionKeys.size() +
                ", version='" + version + '\'' +
                ", timestamp=" + timestamp +
                ", isEncrypted=" + isEncrypted +
                '}';
    }
}