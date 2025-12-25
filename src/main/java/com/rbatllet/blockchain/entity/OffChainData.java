package com.rbatllet.blockchain.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity to store metadata for off-chain data storage
 * Contains hash, signature, and location information for data integrity verification
 */
@Entity
@Table(name = "off_chain_data")
public class OffChainData {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 64)
    private String dataHash;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String signature;
    
    @Column(nullable = false)
    private String filePath;
    
    @Column(nullable = false)
    private Long fileSize;
    
    @Column(nullable = false, length = 32)
    private String encryptionIV;

    @Column(nullable = false, length = 32)
    private String encryptionSalt;

    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false, length = 100)
    private String contentType;
    
    @Column(columnDefinition = "TEXT")
    private String signerPublicKey;
    
    public OffChainData() {
        this.createdAt = LocalDateTime.now();
    }
    
    public OffChainData(String dataHash, String signature, String filePath,
                       Long fileSize, String encryptionIV, String encryptionSalt, String contentType,
                       String signerPublicKey) {
        this();
        this.dataHash = dataHash;
        this.signature = signature;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.encryptionIV = encryptionIV;
        this.encryptionSalt = encryptionSalt;
        this.contentType = contentType;
        this.signerPublicKey = signerPublicKey;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getDataHash() {
        return dataHash;
    }
    
    public void setDataHash(String dataHash) {
        this.dataHash = dataHash;
    }
    
    public String getSignature() {
        return signature;
    }
    
    public void setSignature(String signature) {
        this.signature = signature;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    public Long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }
    
    public String getEncryptionIV() {
        return encryptionIV;
    }

    public void setEncryptionIV(String encryptionIV) {
        this.encryptionIV = encryptionIV;
    }

    public String getEncryptionSalt() {
        return encryptionSalt;
    }

    public void setEncryptionSalt(String encryptionSalt) {
        this.encryptionSalt = encryptionSalt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        // Validate database column limit (100 chars)
        if (contentType != null && contentType.length() > 100) {
            throw new IllegalArgumentException(
                "contentType exceeds maximum length of 100 characters (got: " +
                contentType.length() + "). Please use standard MIME types."
            );
        }
        this.contentType = contentType;
    }
    
    public String getSignerPublicKey() {
        return signerPublicKey;
    }
    
    public void setSignerPublicKey(String signerPublicKey) {
        this.signerPublicKey = signerPublicKey;
    }
    
    @Override
    public String toString() {
        return "OffChainData{" +
                "id=" + id +
                ", dataHash='" + dataHash + '\'' +
                ", filePath='" + filePath + '\'' +
                ", fileSize=" + fileSize +
                ", contentType='" + contentType + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}