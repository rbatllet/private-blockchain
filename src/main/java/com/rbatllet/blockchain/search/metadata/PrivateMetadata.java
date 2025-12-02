package com.rbatllet.blockchain.search.metadata;

import com.rbatllet.blockchain.config.SearchConstants;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.Map;
import java.util.HashSet;
import java.util.HashMap;

/**
 * Private Metadata Layer - Encrypted, Password Protected
 * 
 * Contains sensitive, detailed information that requires password
 * access for decryption. This layer enables detailed searches
 * for authorized users while protecting confidential data.
 * 
 * All data in this layer is encrypted using AES-256-GCM and
 * can only be accessed with the correct password.
 */
public class PrivateMetadata {
    
    private LocalDateTime exactTimestamp;
    private Set<String> specificKeywords;
    private String detailedCategory;
    private String ownerDetails;
    private Map<String, Object> technicalDetails;
    private Map<String, Object> contentStatistics;
    private Map<String, Object> validationInfo;
    
    // Additional fields for comprehensive search
    private Set<String> sensitiveTerms;
    private Set<String> identifiers;
    private String contentSummary;
    private String contentCategory;
    
    public PrivateMetadata() {
        this.specificKeywords = new HashSet<>();
        this.technicalDetails = new HashMap<>();
        this.contentStatistics = new HashMap<>();
        this.validationInfo = new HashMap<>();
        this.sensitiveTerms = new HashSet<>();
        this.identifiers = new HashSet<>();
    }
    
    // ===== GETTERS AND SETTERS =====
    
    public LocalDateTime getExactTimestamp() {
        return exactTimestamp;
    }
    
    public void setExactTimestamp(LocalDateTime exactTimestamp) {
        this.exactTimestamp = exactTimestamp;
    }
    
    public Set<String> getSpecificKeywords() {
        return specificKeywords;
    }
    
    public void setSpecificKeywords(Set<String> specificKeywords) {
        this.specificKeywords = specificKeywords != null ? specificKeywords : new HashSet<>();
    }
    
    public String getDetailedCategory() {
        return detailedCategory;
    }
    
    public void setDetailedCategory(String detailedCategory) {
        this.detailedCategory = detailedCategory;
    }
    
    public String getOwnerDetails() {
        return ownerDetails;
    }
    
    public void setOwnerDetails(String ownerDetails) {
        this.ownerDetails = ownerDetails;
    }
    
    public Map<String, Object> getTechnicalDetails() {
        return technicalDetails;
    }
    
    public void setTechnicalDetails(Map<String, Object> technicalDetails) {
        this.technicalDetails = technicalDetails != null ? technicalDetails : new HashMap<>();
    }
    
    public Map<String, Object> getContentStatistics() {
        return contentStatistics;
    }
    
    public void setContentStatistics(Map<String, Object> contentStatistics) {
        this.contentStatistics = contentStatistics != null ? contentStatistics : new HashMap<>();
    }
    
    public Map<String, Object> getValidationInfo() {
        return validationInfo;
    }
    
    public void setValidationInfo(Map<String, Object> validationInfo) {
        this.validationInfo = validationInfo != null ? validationInfo : new HashMap<>();
    }
    
    public Set<String> getSensitiveTerms() {
        return sensitiveTerms;
    }
    
    public void setSensitiveTerms(Set<String> sensitiveTerms) {
        this.sensitiveTerms = sensitiveTerms != null ? sensitiveTerms : new HashSet<>();
    }
    
    public Set<String> getIdentifiers() {
        return identifiers;
    }
    
    public void setIdentifiers(Set<String> identifiers) {
        this.identifiers = identifiers != null ? identifiers : new HashSet<>();
    }
    
    public String getContentSummary() {
        return contentSummary;
    }
    
    public void setContentSummary(String contentSummary) {
        this.contentSummary = contentSummary;
    }
    
    public String getContentCategory() {
        return contentCategory;
    }
    
    public void setContentCategory(String contentCategory) {
        this.contentCategory = contentCategory;
    }
    
    // ===== UTILITY METHODS =====
    
    /**
     * Check if this private metadata matches a search query
     * This is used when the metadata has been decrypted for search
     */
    public boolean matches(String query) {
        if (query == null || query.trim().isEmpty()) {
            return true;
        }
        
        String lowerQuery = query.toLowerCase();
        
        // Check specific keywords (most important)
        for (String keyword : specificKeywords) {
            if (keyword.toLowerCase().contains(lowerQuery)) {
                return true;
            }
        }
        
        // Check detailed category
        if (containsIgnoreCase(detailedCategory, lowerQuery)) {
            return true;
        }
        
        // Check owner details
        if (containsIgnoreCase(ownerDetails, lowerQuery)) {
            return true;
        }
        
        // Check technical details
        for (Map.Entry<String, Object> entry : technicalDetails.entrySet()) {
            if (containsIgnoreCase(entry.getKey(), lowerQuery) || 
                containsIgnoreCase(String.valueOf(entry.getValue()), lowerQuery)) {
                return true;
            }
        }
        
        return false;
    }
    
    private boolean containsIgnoreCase(String field, String query) {
        return field != null && field.toLowerCase().contains(query);
    }
    
    /**
     * Calculate detailed relevance score for private metadata search
     */
    public double calculateRelevanceScore(String query) {
        if (query == null || query.trim().isEmpty()) {
            return 1.0;
        }
        
        double score = 0.0;
        String lowerQuery = query.toLowerCase();
        
        // Specific keywords (highest weight - these are the most precise)
        for (String keyword : specificKeywords) {
            if (keyword.toLowerCase().equals(lowerQuery)) {
                score += SearchConstants.SENSITIVE_EXACT_MATCH_BONUS;
            } else if (keyword.toLowerCase().contains(lowerQuery)) {
                score += SearchConstants.SENSITIVE_PARTIAL_MATCH_BONUS;
            }
        }
        
        // Detailed category (high weight)
        if (containsIgnoreCase(detailedCategory, lowerQuery)) {
            score += 5.0;
        }
        
        // Owner details (medium-high weight)
        if (containsIgnoreCase(ownerDetails, lowerQuery)) {
            score += 4.0;
        }
        
        // Technical details (medium weight)
        for (Map.Entry<String, Object> entry : technicalDetails.entrySet()) {
            if (containsIgnoreCase(entry.getKey(), lowerQuery)) {
                score += 2.0;
            }
            if (containsIgnoreCase(String.valueOf(entry.getValue()), lowerQuery)) {
                score += 1.5;
            }
        }
        
        return score;
    }
    
    /**
     * Check if private metadata is empty
     */
    public boolean isEmpty() {
        return (specificKeywords == null || specificKeywords.isEmpty()) &&
               (detailedCategory == null || detailedCategory.trim().isEmpty()) &&
               (ownerDetails == null || ownerDetails.trim().isEmpty()) &&
               (technicalDetails == null || technicalDetails.isEmpty());
    }
    
    /**
     * Add a specific keyword to the collection
     */
    public void addSpecificKeyword(String keyword) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            this.specificKeywords.add(keyword.trim());
        }
    }
    
    /**
     * Add technical detail
     */
    public void addTechnicalDetail(String key, Object value) {
        if (key != null && value != null) {
            this.technicalDetails.put(key, value);
        }
    }
    
    /**
     * Add content statistic
     */
    public void addContentStatistic(String key, Object value) {
        if (key != null && value != null) {
            this.contentStatistics.put(key, value);
        }
    }
    
    /**
     * Get a technical detail by key
     */
    public Object getTechnicalDetail(String key) {
        return technicalDetails.get(key);
    }
    
    /**
     * Get a content statistic by key
     */
    public Object getContentStatistic(String key) {
        return contentStatistics.get(key);
    }
    
    /**
     * Get detailed keywords for encrypted search
     */
    public Set<String> getDetailedKeywords() {
        Set<String> allKeywords = new HashSet<>();
        if (specificKeywords != null) allKeywords.addAll(specificKeywords);
        if (sensitiveTerms != null) allKeywords.addAll(sensitiveTerms);
        return allKeywords;
    }
    
    /**
     * Set detailed keywords for encrypted search
     */
    public void setDetailedKeywords(Set<String> detailedKeywords) {
        if (detailedKeywords != null) {
            this.specificKeywords.clear();
            this.specificKeywords.addAll(detailedKeywords);
        }
    }
    
    /**
     * Add a sensitive term
     */
    public void addSensitiveTerm(String term) {
        if (term != null && !term.trim().isEmpty()) {
            this.sensitiveTerms.add(term.trim());
        }
    }
    
    /**
     * Add an identifier
     */
    public void addIdentifier(String identifier) {
        if (identifier != null && !identifier.trim().isEmpty()) {
            this.identifiers.add(identifier.trim());
        }
    }
    
    @Override
    public String toString() {
        return String.format("PrivateMetadata{timestamp=%s, keywords=%d, category=%s, owner=%s}", 
                           exactTimestamp, 
                           specificKeywords != null ? specificKeywords.size() : 0,
                           detailedCategory,
                           ownerDetails != null ? "[PROTECTED]" : null);
    }
}