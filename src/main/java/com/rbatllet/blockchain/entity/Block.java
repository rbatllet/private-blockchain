package com.rbatllet.blockchain.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "blocks")
public class Block {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "block_number", unique = true, nullable = false)
    private Long blockNumber;
    
    @Column(name = "previous_hash", length = 64)
    private String previousHash;
    
    @Column(name = "data", columnDefinition = "TEXT")
    private String data;
    
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
    
    @Column(name = "hash", length = 64, nullable = false)
    private String hash;
    
    @Column(name = "signature", columnDefinition = "TEXT")
    private String signature;
    
    @Column(name = "signer_public_key", columnDefinition = "TEXT")
    private String signerPublicKey;
    
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "off_chain_data_id")
    private OffChainData offChainData;
    
    // Search-related fields
    @Column(name = "manual_keywords", length = 1024)
    private String manualKeywords;      // Keywords specified by user
    
    @Column(name = "auto_keywords", length = 1024) 
    private String autoKeywords;        // Automatically extracted (universal)
    
    @Column(name = "searchable_content", length = 2048)
    private String searchableContent;   // Combination of manual + auto
    
    @Column(name = "content_category", length = 50)
    private String contentCategory;     // "MEDICAL", "FINANCE", "LEGAL", etc.
    
    // Encryption-related fields
    @Column(name = "is_encrypted", nullable = false)
    private Boolean isEncrypted = false;  // Flag to indicate if data is encrypted
    
    @Column(name = "encryption_metadata", columnDefinition = "TEXT")
    private String encryptionMetadata;    // Serialized EncryptedBlockData (when encrypted)
    
    @Column(name = "custom_metadata", columnDefinition = "TEXT")
    private String customMetadata;        // Custom metadata in JSON format

    // Constructors
    public Block() {}

    public Block(Long blockNumber, String previousHash, String data, 
                 LocalDateTime timestamp, String hash, String signature, 
                 String signerPublicKey) {
        this.blockNumber = blockNumber;
        this.previousHash = previousHash;
        this.data = data;
        this.timestamp = timestamp;
        this.hash = hash;
        this.signature = signature;
        this.signerPublicKey = signerPublicKey;
        this.isEncrypted = false; // Default to unencrypted for compatibility
    }
    
    public Block(Long blockNumber, String previousHash, String data, 
                 LocalDateTime timestamp, String hash, String signature, 
                 String signerPublicKey, OffChainData offChainData) {
        this(blockNumber, previousHash, data, timestamp, hash, signature, signerPublicKey);
        this.offChainData = offChainData;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getBlockNumber() { return blockNumber; }
    public void setBlockNumber(Long blockNumber) { this.blockNumber = blockNumber; }

    public String getPreviousHash() { return previousHash; }
    public void setPreviousHash(String previousHash) { this.previousHash = previousHash; }

    public String getData() { return data; }
    public void setData(String data) { this.data = data; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getHash() { return hash; }
    public void setHash(String hash) { this.hash = hash; }

    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }

    public String getSignerPublicKey() { return signerPublicKey; }
    public void setSignerPublicKey(String signerPublicKey) { this.signerPublicKey = signerPublicKey; }
    
    public OffChainData getOffChainData() { return offChainData; }
    public void setOffChainData(OffChainData offChainData) { this.offChainData = offChainData; }
    
    public boolean hasOffChainData() { return offChainData != null; }
    
    // Search-related getters and setters
    public String getManualKeywords() { return manualKeywords; }
    public void setManualKeywords(String manualKeywords) { this.manualKeywords = manualKeywords; }
    
    public String getAutoKeywords() { return autoKeywords; }
    public void setAutoKeywords(String autoKeywords) { this.autoKeywords = autoKeywords; }
    
    public String getSearchableContent() { return searchableContent; }
    public void setSearchableContent(String searchableContent) { this.searchableContent = searchableContent; }
    
    public String getContentCategory() { return contentCategory; }
    public void setContentCategory(String contentCategory) { this.contentCategory = contentCategory; }
    
    // Encryption-related getters and setters
    public Boolean getIsEncrypted() { return isEncrypted; }
    public void setIsEncrypted(Boolean isEncrypted) { this.isEncrypted = isEncrypted; }
    
    public String getEncryptionMetadata() { return encryptionMetadata; }
    public void setEncryptionMetadata(String encryptionMetadata) { this.encryptionMetadata = encryptionMetadata; }
    
    public String getCustomMetadata() { return customMetadata; }
    public void setCustomMetadata(String customMetadata) { this.customMetadata = customMetadata; }
    
    /**
     * Returns true if this block contains encrypted data
     */
    @JsonIgnore
    public boolean isDataEncrypted() {
        return isEncrypted != null && isEncrypted;
    }
    
    /**
     * Updates searchableContent by combining manual and auto keywords
     */
    public void updateSearchableContent() {
        java.util.List<String> allKeywords = new java.util.ArrayList<>();
        if (manualKeywords != null && !manualKeywords.trim().isEmpty()) {
            allKeywords.add(manualKeywords.toLowerCase());
        }
        if (autoKeywords != null && !autoKeywords.trim().isEmpty()) {
            allKeywords.add(autoKeywords.toLowerCase());
        }
        this.searchableContent = String.join(" ", allKeywords);
    }

    @Override
    public String toString() {
        return "Block{" +
                "id=" + id +
                ", blockNumber=" + blockNumber +
                ", previousHash='" + previousHash + '\'' +
                ", data='" + (isDataEncrypted() ? "[ENCRYPTED]" : data) + '\'' +
                ", timestamp=" + timestamp +
                ", hash='" + hash + '\'' +
                ", signature='" + (signature != null ? signature.substring(0, Math.min(20, signature.length())) + "..." : "null") + '\'' +
                ", signerPublicKey='" + (signerPublicKey != null ? signerPublicKey.substring(0, Math.min(20, signerPublicKey.length())) + "..." : "null") + '\'' +
                ", hasOffChainData=" + hasOffChainData() +
                ", isEncrypted=" + isEncrypted +
                '}';
    }
}