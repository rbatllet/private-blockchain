package com.rbatllet.blockchain.search.metadata;

import com.rbatllet.blockchain.config.SearchConstants;

import java.util.Set;
import java.util.HashSet;

/**
 * Public Metadata Layer - Always Searchable
 * 
 * Contains general, non-sensitive information that can be searched
 * without requiring a password. Designed to enable fast searches
 * while protecting sensitive information.
 * 
 * This layer enables FastIndex search capabilities with sub-50ms
 * response times for millions of blocks.
 */
public class PublicMetadata {
    
    private Set<String> generalKeywords;
    private String timeRange;
    private String contentType;
    private String blockCategory;
    private String sizeRange;
    private String hashFingerprint;
    
    public PublicMetadata() {
        this.generalKeywords = new HashSet<>();
    }
    
    // ===== GETTERS AND SETTERS =====
    
    public Set<String> getGeneralKeywords() {
        return generalKeywords;
    }
    
    public void setGeneralKeywords(Set<String> generalKeywords) {
        this.generalKeywords = generalKeywords != null ? generalKeywords : new HashSet<>();
    }
    
    public String getTimeRange() {
        return timeRange;
    }
    
    public void setTimeRange(String timeRange) {
        this.timeRange = timeRange;
    }
    
    public String getContentType() {
        return contentType;
    }
    
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    
    public String getBlockCategory() {
        return blockCategory;
    }
    
    public void setBlockCategory(String blockCategory) {
        this.blockCategory = blockCategory;
    }
    
    public String getSizeRange() {
        return sizeRange;
    }
    
    public void setSizeRange(String sizeRange) {
        this.sizeRange = sizeRange;
    }
    
    public String getHashFingerprint() {
        return hashFingerprint;
    }
    
    public void setHashFingerprint(String hashFingerprint) {
        this.hashFingerprint = hashFingerprint;
    }
    
    
    // ===== UTILITY METHODS =====
    
    /**
     * Check if this metadata matches a search query
     */
    public boolean matches(String query) {
        if (query == null || query.trim().isEmpty()) {
            return true;
        }
        
        String lowerQuery = query.toLowerCase();
        
        // Check general keywords
        for (String keyword : generalKeywords) {
            if (keyword.toLowerCase().contains(lowerQuery)) {
                return true;
            }
        }
        
        // Check other fields
        return containsIgnoreCase(contentType, lowerQuery) ||
               containsIgnoreCase(blockCategory, lowerQuery) ||
               containsIgnoreCase(timeRange, lowerQuery) ||
               containsIgnoreCase(sizeRange, lowerQuery);
    }
    
    private boolean containsIgnoreCase(String field, String query) {
        return field != null && field.toLowerCase().contains(query);
    }
    
    /**
     * Calculate relevance score for ranking search results
     */
    public double calculateRelevanceScore(String query) {
        if (query == null || query.trim().isEmpty()) {
            return 1.0;
        }
        
        double score = 0.0;
        String lowerQuery = query.toLowerCase();
        
        // Keyword matches (highest weight)
        for (String keyword : generalKeywords) {
            if (keyword.toLowerCase().equals(lowerQuery)) {
                score += SearchConstants.EXACT_MATCH_BONUS;
            } else if (keyword.toLowerCase().contains(lowerQuery)) {
                score += SearchConstants.PARTIAL_MATCH_BONUS;
            }
        }
        
        // Category matches (medium weight)
        if (containsIgnoreCase(blockCategory, lowerQuery)) {
            score += 3.0;
        }
        
        if (containsIgnoreCase(contentType, lowerQuery)) {
            score += 2.0;
        }
        
        // Other field matches (lower weight)
        if (containsIgnoreCase(timeRange, lowerQuery)) {
            score += 1.0;
        }
        
        return score;
    }
    
    /**
     * Check if metadata is empty (no searchable content)
     */
    public boolean isEmpty() {
        return (generalKeywords == null || generalKeywords.isEmpty()) &&
               (contentType == null || contentType.trim().isEmpty()) &&
               (blockCategory == null || blockCategory.trim().isEmpty());
    }
    
    @Override
    public String toString() {
        return String.format("PublicMetadata{keywords=%s, type=%s, category=%s, time=%s, size=%s}", 
                           generalKeywords, contentType, blockCategory, timeRange, sizeRange);
    }
}