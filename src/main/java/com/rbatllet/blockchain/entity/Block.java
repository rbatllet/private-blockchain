package com.rbatllet.blockchain.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Block entity representing a single block in the blockchain.
 * 
 * <h2>🔒 SECURITY: Blockchain Integrity Protection (2-Layer Defense)</h2>
 * <p>This class implements a robust protection system to prevent modification
 * of hash-critical fields that would compromise blockchain integrity.</p>
 * 
 * <h3>⚠️ IMMUTABLE Fields (Hash-Critical) - Protected by 2 Layers:</h3>
 * 
 * <h4>Layer 1: Database Level Protection (JPA)</h4>
 * <p>All hash-critical fields are marked {@code @Column(updatable=false)}:</p>
 * <ul>
 *   <li><b>data</b> - Block content (part of hash calculation)</li>
 *   <li><b>blockNumber</b> - Block position in chain</li>
 *   <li><b>previousHash</b> - Link to previous block</li>
 *   <li><b>timestamp</b> - Block creation time</li>
 *   <li><b>hash</b> - Block's cryptographic hash</li>
 *   <li><b>signature</b> - Cryptographic signature</li>
 *   <li><b>signerPublicKey</b> - Signer identity</li>
 * </ul>
 * <p><b>JPA will silently ignore any UPDATE statements</b> for these fields,
 * providing the final enforcement layer at the database level.</p>
 * 
 * <h4>Layer 2: API Level Validation + Logging</h4>
 * <p>{@link com.rbatllet.blockchain.core.Blockchain#updateBlock(Block)} provides:</p>
 * <ul>
 *   <li><b>Validation:</b> Rejects update requests if immutable fields are modified</li>
 *   <li><b>Logging:</b> Logs {@code logger.error()} for EACH attempted modification</li>
 *   <li><b>Clear Feedback:</b> Returns {@code false} to indicate rejection</li>
 *   <li><b>Safe Updates:</b> Only copies mutable fields to existing block</li>
 * </ul>
 * <p><b>All modification attempts are logged</b> for security auditing.</p>
 * 
 * <h3>✅ MUTABLE Fields (Safe for updates):</h3>
 * <p>These fields can be safely modified without affecting blockchain integrity:</p>
 * <ul>
 *   <li><b>customMetadata</b> - User-defined JSON metadata</li>
 *   <li><b>encryptionMetadata</b> - Encryption/recipient metadata</li>
 *   <li><b>manualKeywords</b> - User-specified search keywords</li>
 *   <li><b>autoKeywords</b> - Auto-extracted keywords</li>
 *   <li><b>searchableContent</b> - Combined searchable text</li>
 *   <li><b>contentCategory</b> - Content classification</li>
 *   <li><b>isEncrypted</b> - Encryption flag</li>
 *   <li><b>offChainData</b> - Off-chain data reference</li>
 * </ul>
 * 
 * <h3>Safe Update API:</h3>
 * <p>Use {@code Blockchain.updateBlock()} to safely update mutable fields.</p>
 * <p>The method will log and reject any attempts to modify immutable fields.</p>
 * 
 * @author rbatllet
 * @since 1.0.0
 * @see com.rbatllet.blockchain.core.Blockchain#updateBlock(Block)
 */
@Entity
@Table(name = "blocks", indexes = {
    @Index(name = "idx_blocks_timestamp", columnList = "timestamp"),
    @Index(name = "idx_blocks_is_encrypted", columnList = "is_encrypted"),
    @Index(name = "idx_blocks_signer_public_key", columnList = "signer_public_key"),
    @Index(name = "idx_blocks_content_category", columnList = "content_category")
})
public class Block {

    // ========== IMMUTABLE FIELDS (Hash-Critical) - updatable=false ==========

    /**
     * Block number (position in the chain).
     * Phase 5.0: Manually assigned before persist() to allow hash calculation.
     * JDBC batching enabled via persistence.xml configuration.
     */
    @Id
    @Column(name = "block_number", unique = true, nullable = false, updatable = false)
    private Long blockNumber;
    
    @Column(name = "previous_hash", length = 64, updatable = false)
    private String previousHash;
    
    @Column(name = "data", columnDefinition = "TEXT", updatable = false)
    private String data;
    
    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;
    
    @Column(name = "hash", length = 64, nullable = false, updatable = false)
    private String hash;
    
    @Column(name = "signature", columnDefinition = "TEXT", updatable = false)
    private String signature;
    
    @Column(name = "signer_public_key", length = 5000, updatable = false)
    private String signerPublicKey;
    
    // ========== MUTABLE FIELDS (Safe to update) ==========
    
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "off_chain_data_id")
    private OffChainData offChainData;
    
    // Search-related fields (safe to update)
    @Column(name = "manual_keywords", length = 1024)
    private String manualKeywords;      // Keywords specified by user
    
    @Column(name = "auto_keywords", length = 1024) 
    private String autoKeywords;        // Automatically extracted (universal)
    
    @Column(name = "searchable_content", length = 2048)
    private String searchableContent;   // Combination of manual + auto
    
    @Column(name = "content_category", length = 50)
    private String contentCategory;     // "MEDICAL", "FINANCE", "LEGAL", etc.
    
    // Encryption-related fields (safe to update)
    @Column(name = "is_encrypted", nullable = false)
    private Boolean isEncrypted = false;  // Flag to indicate if data is encrypted
    
    @Column(name = "encryption_metadata", columnDefinition = "TEXT")
    private String encryptionMetadata;    // Serialized EncryptedBlockData (when encrypted)
    
    // Custom metadata (safe to update)
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
    public Long getBlockNumber() { return blockNumber; }
    public void setBlockNumber(Long blockNumber) { this.blockNumber = blockNumber; }

    public String getPreviousHash() { return previousHash; }
    public void setPreviousHash(String previousHash) {
        // Validate database column limit (64 chars for SHA-256 hex)
        if (previousHash != null && previousHash.length() > 64) {
            throw new IllegalArgumentException(
                "previousHash exceeds maximum length of 64 characters (got: " +
                previousHash.length() + "). Expected SHA-256 hex format."
            );
        }
        this.previousHash = previousHash;
    }

    public String getData() { return data; }
    public void setData(String data) { this.data = data; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getHash() { return hash; }
    public void setHash(String hash) {
        // Validate database column limit (64 chars for SHA-256 hex)
        if (hash != null && hash.length() > 64) {
            throw new IllegalArgumentException(
                "hash exceeds maximum length of 64 characters (got: " +
                hash.length() + "). Expected SHA-256 hex format."
            );
        }
        this.hash = hash;
    }

    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }

    public String getSignerPublicKey() { return signerPublicKey; }
    public void setSignerPublicKey(String signerPublicKey) { this.signerPublicKey = signerPublicKey; }
    
    public OffChainData getOffChainData() { return offChainData; }
    public void setOffChainData(OffChainData offChainData) { this.offChainData = offChainData; }
    
    public boolean hasOffChainData() { return offChainData != null; }
    
    // Search-related getters and setters
    public String getManualKeywords() { return manualKeywords; }
    public void setManualKeywords(String manualKeywords) {
        // Validate database column limit (1024 chars)
        if (manualKeywords != null && manualKeywords.length() > 1024) {
            throw new IllegalArgumentException(
                "manualKeywords exceeds maximum length of 1024 characters (got: " +
                manualKeywords.length() + "). Please provide fewer or shorter keywords."
            );
        }
        this.manualKeywords = manualKeywords;
    }
    
    public String getAutoKeywords() { return autoKeywords; }
    public void setAutoKeywords(String autoKeywords) {
        // Validate database column limit (1024 chars)
        if (autoKeywords != null && autoKeywords.length() > 1024) {
            throw new IllegalArgumentException(
                "autoKeywords exceeds maximum length of 1024 characters (got: " +
                autoKeywords.length() + "). Automatic keyword generation produced too much content."
            );
        }
        this.autoKeywords = autoKeywords;
    }
    
    public String getSearchableContent() { return searchableContent; }
    public void setSearchableContent(String searchableContent) {
        // Validate database column limit (2048 chars)
        if (searchableContent != null && searchableContent.length() > 2048) {
            throw new IllegalArgumentException(
                "searchableContent exceeds maximum length of 2048 characters (got: " +
                searchableContent.length() + "). Please reduce content size or use off-chain storage."
            );
        }
        this.searchableContent = searchableContent;
    }
    
    public String getContentCategory() { return contentCategory; }
    public void setContentCategory(String contentCategory) {
        // Validate database column limit (50 chars)
        if (contentCategory != null && contentCategory.length() > 50) {
            throw new IllegalArgumentException(
                "contentCategory exceeds maximum length of 50 characters (got: " +
                contentCategory.length() + "). Please use shorter category names."
            );
        }
        this.contentCategory = contentCategory;
    }
    
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
        String combined = String.join(" ", allKeywords);

        // Validate combined length before setting
        if (combined.length() > 2048) {
            throw new IllegalStateException(
                "Combined searchable content exceeds 2048 character limit (got: " +
                combined.length() + "). Reduce manual or auto keywords."
            );
        }
        this.searchableContent = combined;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Block block = (Block) o;
        return blockNumber != null && blockNumber.equals(block.blockNumber);
    }

    @Override
    public int hashCode() {
        return blockNumber != null ? blockNumber.hashCode() : 0;
    }

    @Override
    public String toString() {
        // For encrypted blocks, show a preview of the original data (for debugging/logging)
        // The data field is never null and contains the original unencrypted data
        String dataPreview = data != null 
            ? (data.length() > 50 ? data.substring(0, 50) + "..." : data)
            : "null";
            
        return "Block{" +
                "blockNumber=" + blockNumber +
                ", previousHash='" + previousHash + '\'' +
                ", data='" + dataPreview + '\'' +
                ", timestamp=" + timestamp +
                ", hash='" + hash + '\'' +
                ", signature='" + (signature != null ? signature.substring(0, Math.min(20, signature.length())) + "..." : "null") + '\'' +
                ", signerPublicKey='" + (signerPublicKey != null ? signerPublicKey.substring(0, Math.min(20, signerPublicKey.length())) + "..." : "null") + '\'' +
                ", hasOffChainData=" + hasOffChainData() +
                ", isEncrypted=" + isEncrypted +
                '}';
    }
}