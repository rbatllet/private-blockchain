package com.rbatllet.blockchain.search.metadata;

import com.rbatllet.blockchain.config.EncryptionConfig.SecurityLevel;

import java.util.Set;
import java.util.Map;
import java.util.HashSet;
import java.util.HashMap;

/**
 * Content Analysis Helper Class
 * 
 * Performs deep analysis of block content to extract characteristics
 * needed for intelligent metadata generation. This analysis drives
 * the decision-making process for what information goes into which
 * metadata layer.
 * 
 * Used internally by MetadataLayerManager to understand content
 * patterns, sensitivity levels, and structural elements.
 */
public class ContentAnalysis {
    
    private Set<String> allKeywords;
    private int contentLength;
    private int wordCount;
    private Set<String> sensitiveTerms;
    private Set<String> numericalValues;
    private Set<String> dateReferences;
    private Set<String> identifiers;
    private Set<String> technicalTerms;
    private Map<String, Object> structuralElements;
    
    public ContentAnalysis() {
        this.allKeywords = new HashSet<>();
        this.sensitiveTerms = new HashSet<>();
        this.numericalValues = new HashSet<>();
        this.dateReferences = new HashSet<>();
        this.identifiers = new HashSet<>();
        this.technicalTerms = new HashSet<>();
        this.structuralElements = new HashMap<>();
    }
    
    // ===== GETTERS AND SETTERS =====
    
    public Set<String> getAllKeywords() {
        return allKeywords;
    }
    
    public void setAllKeywords(Set<String> allKeywords) {
        this.allKeywords = allKeywords != null ? allKeywords : new HashSet<>();
    }
    
    public int getContentLength() {
        return contentLength;
    }
    
    public void setContentLength(int contentLength) {
        this.contentLength = contentLength;
    }
    
    public int getWordCount() {
        return wordCount;
    }
    
    public void setWordCount(int wordCount) {
        this.wordCount = wordCount;
    }
    
    public Set<String> getSensitiveTerms() {
        return sensitiveTerms;
    }
    
    public void setSensitiveTerms(Set<String> sensitiveTerms) {
        this.sensitiveTerms = sensitiveTerms != null ? sensitiveTerms : new HashSet<>();
    }
    
    public Set<String> getNumericalValues() {
        return numericalValues;
    }
    
    public void setNumericalValues(Set<String> numericalValues) {
        this.numericalValues = numericalValues != null ? numericalValues : new HashSet<>();
    }
    
    public Set<String> getDateReferences() {
        return dateReferences;
    }
    
    public void setDateReferences(Set<String> dateReferences) {
        this.dateReferences = dateReferences != null ? dateReferences : new HashSet<>();
    }
    
    public Set<String> getIdentifiers() {
        return identifiers;
    }
    
    public void setIdentifiers(Set<String> identifiers) {
        this.identifiers = identifiers != null ? identifiers : new HashSet<>();
    }
    
    public Set<String> getTechnicalTerms() {
        return technicalTerms;
    }
    
    public void setTechnicalTerms(Set<String> technicalTerms) {
        this.technicalTerms = technicalTerms != null ? technicalTerms : new HashSet<>();
    }
    
    public Map<String, Object> getStructuralElements() {
        return structuralElements;
    }
    
    public void setStructuralElements(Map<String, Object> structuralElements) {
        this.structuralElements = structuralElements != null ? structuralElements : new HashMap<>();
    }
    
    // ===== ANALYSIS METHODS =====
    
    /**
     * Calculate sensitivity score of the content
     * Higher score means more sensitive information
     */
    public double getSensitivityScore() {
        double score = 0.0;
        
        // Sensitive terms contribute most
        score += sensitiveTerms.size() * 3.0;
        
        // Identifiers are sensitive
        score += identifiers.size() * 2.0;
        
        // Numerical values can be sensitive (amounts, IDs, etc.)
        score += numericalValues.size() * 1.0;
        
        // Date references might indicate specific transactions
        score += dateReferences.size() * 0.5;
        
        return score;
    }
    
    /**
     * Calculate technical complexity score
     * Higher score means more technical content
     */
    public double getTechnicalComplexity() {
        double score = 0.0;
        
        // Technical terms are the main indicator
        score += technicalTerms.size() * 2.0;
        
        // Complex structure indicates technical content
        score += structuralElements.size() * 1.0;
        
        // Many numerical values might indicate data/calculations
        if (numericalValues.size() > 10) {
            score += 2.0;
        }
        
        return score;
    }
    
    // Removed automatic content categorization methods - users now define their own search terms
    
    /**
     * Check if content has personal information
     */
    public boolean hasPersonalInformation() {
        // Look for patterns that might indicate personal info
        for (String term : sensitiveTerms) {
            // Names (capitalized words)
            if (term.matches("[A-Z][a-z]+")) {
                return true;
            }
            // Email addresses
            if (term.contains("@")) {
                return true;
            }
        }
        
        // Phone numbers or similar
        for (String num : numericalValues) {
            if (num.length() >= 7 && num.matches(".*\\d{7,}.*")) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Get recommended security level based on analysis
     */
    public SecurityLevel getRecommendedSecurityLevel() {
        double sensitivityScore = getSensitivityScore();
        boolean hasPersonal = hasPersonalInformation();
        
        if (sensitivityScore > 10.0 || hasPersonal) {
            return SecurityLevel.MAXIMUM;
        } else if (sensitivityScore > 5.0) {
            return SecurityLevel.BALANCED;
        } else {
            return SecurityLevel.PERFORMANCE;
        }
    }
    
    /**
     * Add a keyword to all keywords set
     */
    public void addKeyword(String keyword) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            allKeywords.add(keyword.trim());
        }
    }
    
    /**
     * Add a sensitive term
     */
    public void addSensitiveTerm(String term) {
        if (term != null && !term.trim().isEmpty()) {
            sensitiveTerms.add(term.trim());
        }
    }
    
    /**
     * Add an identifier
     */
    public void addIdentifier(String identifier) {
        if (identifier != null && !identifier.trim().isEmpty()) {
            identifiers.add(identifier.trim());
        }
    }
    
    
    @Override
    public String toString() {
        return String.format("ContentAnalysis{words=%d, keywords=%d, sensitive=%d, technical=%.1f, security=%s}", 
                           wordCount,
                           allKeywords.size(),
                           sensitiveTerms.size(),
                           getTechnicalComplexity(),
                           getRecommendedSecurityLevel());
    }
}