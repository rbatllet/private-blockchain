package com.rbatllet.blockchain.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "blocks")
public class Block {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "block_number", unique = true, nullable = false)
    private int blockNumber;
    
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

    // Constructors
    public Block() {}

    public Block(int blockNumber, String previousHash, String data, 
                 LocalDateTime timestamp, String hash, String signature, 
                 String signerPublicKey) {
        this.blockNumber = blockNumber;
        this.previousHash = previousHash;
        this.data = data;
        this.timestamp = timestamp;
        this.hash = hash;
        this.signature = signature;
        this.signerPublicKey = signerPublicKey;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public int getBlockNumber() { return blockNumber; }
    public void setBlockNumber(int blockNumber) { this.blockNumber = blockNumber; }

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

    @Override
    public String toString() {
        return "Block{" +
                "id=" + id +
                ", blockNumber=" + blockNumber +
                ", previousHash='" + previousHash + '\'' +
                ", data='" + data + '\'' +
                ", timestamp=" + timestamp +
                ", hash='" + hash + '\'' +
                ", signature='" + (signature != null ? signature.substring(0, Math.min(20, signature.length())) + "..." : "null") + '\'' +
                ", signerPublicKey='" + (signerPublicKey != null ? signerPublicKey.substring(0, Math.min(20, signerPublicKey.length())) + "..." : "null") + '\'' +
                '}';
    }
}