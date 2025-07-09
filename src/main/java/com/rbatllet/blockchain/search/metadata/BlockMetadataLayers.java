package com.rbatllet.blockchain.search.metadata;

import com.rbatllet.blockchain.config.EncryptionConfig.SecurityLevel;

/**
 * Container for metadata layers of a block
 * 
 * Encapsulates the metadata architecture:
 * - PublicLayer: Always accessible for fast search
 * - PrivateLayer: Encrypted, requires password
 * 
 * This structure enables the search architecture where
 * different types of queries can access different layers based on
 * authorization and privacy requirements.
 */
public class BlockMetadataLayers {
    
    private final PublicMetadata publicLayer;
    private final String encryptedPrivateLayer; // JSON string, encrypted
    
    public BlockMetadataLayers(PublicMetadata publicLayer, 
                              String encryptedPrivateLayer) {
        this.publicLayer = publicLayer;
        this.encryptedPrivateLayer = encryptedPrivateLayer;
    }
    
    // ===== GETTERS =====
    
    public PublicMetadata getPublicLayer() {
        return publicLayer;
    }
    
    public String getEncryptedPrivateLayer() {
        return encryptedPrivateLayer;
    }
    
    
    // ===== UTILITY METHODS =====
    
    /**
     * Check if the block has a private layer
     */
    public boolean hasPrivateLayer() {
        return encryptedPrivateLayer != null && !encryptedPrivateLayer.trim().isEmpty();
    }
    
    
    /**
     * Check if the block has any searchable metadata
     */
    public boolean hasSearchableMetadata() {
        return (publicLayer != null && !publicLayer.isEmpty()) ||
               hasPrivateLayer();
    }
    
    /**
     * Get the security level of this metadata
     */
    public SecurityLevel getSecurityLevel() {
        if (hasPrivateLayer()) {
            return SecurityLevel.BALANCED;
        } else {
            return SecurityLevel.PERFORMANCE;
        }
    }
    
    /**
     * Calculate total metadata richness score
     */
    public double getMetadataRichness() {
        double score = 0.0;
        
        if (publicLayer != null && !publicLayer.isEmpty()) {
            score += 1.0; // Base score for public searchability
            score += publicLayer.getGeneralKeywords().size() * 0.1; // Keyword richness
        }
        
        if (hasPrivateLayer()) {
            score += 2.0; // Higher value for detailed private metadata
        }
        
        
        return score;
    }
    
    /**
     * Get summary information about the metadata layers
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        
        summary.append("Metadata Layers: ");
        
        if (publicLayer != null && !publicLayer.isEmpty()) {
            summary.append("Public(")
                   .append(publicLayer.getGeneralKeywords().size())
                   .append(" keywords) ");
        }
        
        if (hasPrivateLayer()) {
            summary.append("Private(encrypted) ");
        }
        
        
        summary.append("| Security: ").append(getSecurityLevel());
        summary.append(" | Richness: ").append(String.format("%.1f", getMetadataRichness()));
        
        return summary.toString();
    }
    
    
    @Override
    public String toString() {
        return getSummary();
    }
}