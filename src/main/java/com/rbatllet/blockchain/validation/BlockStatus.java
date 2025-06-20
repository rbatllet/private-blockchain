package com.rbatllet.blockchain.validation;

/**
 * Represents the different states a block can be in
 * 
 * VALID: Block is completely valid (structure, cryptography, and authorization)
 * REVOKED: Block is structurally and cryptographically valid, but signer key was revoked post-creation
 * INVALID: Block has structural or cryptographic problems
 */
public enum BlockStatus {
    /**
     * Block is completely valid - passes all validation checks
     */
    VALID("Block is valid", "✅"),
    
    /**
     * Block is structurally and cryptographically valid, 
     * but the signer's key has been revoked after block creation
     */
    REVOKED("Block signed by revoked key", "⚠️"),
    
    /**
     * Block has structural or cryptographic problems
     */
    INVALID("Block is invalid", "❌");
    
    private final String description;
    private final String icon;
    
    BlockStatus(String description, String icon) {
        this.description = description;
        this.icon = icon;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getIcon() {
        return icon;
    }
    
    @Override
    public String toString() {
        return icon + " " + description;
    }
}
