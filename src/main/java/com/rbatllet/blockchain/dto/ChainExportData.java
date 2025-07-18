package com.rbatllet.blockchain.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.rbatllet.blockchain.entity.AuthorizedKey;
import com.rbatllet.blockchain.entity.Block;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Transfer Object for blockchain export/import operations
 */
public class ChainExportData {
    
    private List<Block> blocks;
    private List<AuthorizedKey> authorizedKeys;
    private LocalDateTime exportTimestamp;
    private String version;
    private int totalBlocks;
    private String description;
    
    // Default constructor
    public ChainExportData() {}
    
    // Constructor with basic data
    public ChainExportData(List<Block> blocks, List<AuthorizedKey> authorizedKeys) {
        this.blocks = blocks;
        this.authorizedKeys = authorizedKeys;
        this.exportTimestamp = LocalDateTime.now();
        this.version = "1.0";
        this.totalBlocks = blocks != null ? blocks.size() : 0;
    }
    
    // Getters and Setters
    public List<Block> getBlocks() {
        return blocks;
    }
    
    public void setBlocks(List<Block> blocks) {
        this.blocks = blocks;
        this.totalBlocks = blocks != null ? blocks.size() : 0;
    }
    
    public List<AuthorizedKey> getAuthorizedKeys() {
        return authorizedKeys;
    }
    
    public void setAuthorizedKeys(List<AuthorizedKey> authorizedKeys) {
        this.authorizedKeys = authorizedKeys;
    }
    
    public LocalDateTime getExportTimestamp() {
        return exportTimestamp;
    }
    
    public void setExportTimestamp(LocalDateTime exportTimestamp) {
        this.exportTimestamp = exportTimestamp;
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    public int getTotalBlocks() {
        return totalBlocks;
    }
    
    public void setTotalBlocks(int totalBlocks) {
        this.totalBlocks = totalBlocks;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    // Utility methods
    @JsonIgnore
    public boolean isEmpty() {
        return blocks == null || blocks.isEmpty();
    }
    
    @JsonIgnore
    public int getAuthorizedKeysCount() {
        return authorizedKeys != null ? authorizedKeys.size() : 0;
    }
    
    @Override
    public String toString() {
        return "ChainExportData{" +
                "totalBlocks=" + totalBlocks +
                ", authorizedKeys=" + getAuthorizedKeysCount() +
                ", version='" + version + '\'' +
                ", exportTimestamp=" + exportTimestamp +
                '}';
    }
}
