package com.rbatllet.blockchain.search.metadata;

import java.util.*;

/**
 * Term Visibility Map for Granular Privacy Control
 * 
 * Allows precise control over which search terms are exposed at which metadata layer.
 * This enables users to have fine-grained privacy control, exposing some terms publicly
 * while keeping sensitive terms encrypted.
 * 
 * Example usage:
 * TermVisibilityMap visibility = new TermVisibilityMap()
 *     .setPublic("patient", "diagnosis", "medical")    // Searchable without password
 *     .setPrivate("John", "Smith", "cancer");          // Requires password to search
 */
public class TermVisibilityMap {
    
    public enum VisibilityLevel {
        PUBLIC,     // Visible in public metadata (no password required)
        PRIVATE     // Only visible in private metadata (password required)
    }
    
    private final Map<String, VisibilityLevel> termVisibility;
    private VisibilityLevel defaultLevel;
    
    /**
     * Create new term visibility map with default level
     * @param defaultLevel Default visibility for terms not explicitly set
     */
    public TermVisibilityMap(VisibilityLevel defaultLevel) {
        this.termVisibility = new HashMap<>();
        this.defaultLevel = defaultLevel;
    }
    
    /**
     * Create new term visibility map with PUBLIC as default
     */
    public TermVisibilityMap() {
        this(VisibilityLevel.PUBLIC);
    }
    
    /**
     * Set multiple terms as public (visible without password)
     * @param terms Terms to make publicly visible
     * @return this for method chaining
     */
    public TermVisibilityMap setPublic(String... terms) {
        if (terms != null) {
            for (String term : terms) {
                if (term != null && !term.trim().isEmpty()) {
                    termVisibility.put(term.toLowerCase().trim(), VisibilityLevel.PUBLIC);
                }
            }
        }
        return this;
    }
    
    /**
     * Set multiple terms as private (requires password)
     * @param terms Terms to make private
     * @return this for method chaining
     */
    public TermVisibilityMap setPrivate(String... terms) {
        if (terms != null) {
            for (String term : terms) {
                if (term != null && !term.trim().isEmpty()) {
                    termVisibility.put(term.toLowerCase().trim(), VisibilityLevel.PRIVATE);
                }
            }
        }
        return this;
    }
    
    /**
     * Set visibility for a single term
     * @param term The search term
     * @param level Visibility level
     * @return this for method chaining
     */
    public TermVisibilityMap setTerm(String term, VisibilityLevel level) {
        if (term != null && !term.trim().isEmpty() && level != null) {
            termVisibility.put(term.toLowerCase().trim(), level);
        }
        return this;
    }
    
    /**
     * Get visibility level for a term
     * @param term The search term
     * @return Visibility level (or default if not explicitly set)
     */
    public VisibilityLevel getVisibility(String term) {
        if (term == null) {
            return defaultLevel;
        }
        return termVisibility.getOrDefault(term.toLowerCase().trim(), defaultLevel);
    }
    
    /**
     * Check if a term should be visible in public metadata
     * @param term The search term
     * @return true if publicly visible
     */
    public boolean isPublic(String term) {
        return getVisibility(term) == VisibilityLevel.PUBLIC;
    }
    
    /**
     * Check if a term should be visible only in private metadata
     * @param term The search term
     * @return true if private only
     */
    public boolean isPrivate(String term) {
        return getVisibility(term) == VisibilityLevel.PRIVATE;
    }
    
    /**
     * Get all terms that should be public
     * @param allTerms All available terms
     * @return Set of terms that should be public
     */
    public Set<String> getPublicTerms(Collection<String> allTerms) {
        Set<String> publicTerms = new HashSet<>();
        for (String term : allTerms) {
            if (isPublic(term)) {
                publicTerms.add(term);
            }
        }
        return publicTerms;
    }
    
    /**
     * Get all terms that should be private
     * @param allTerms All available terms
     * @return Set of terms that should be private
     */
    public Set<String> getPrivateTerms(Collection<String> allTerms) {
        Set<String> privateTerms = new HashSet<>();
        for (String term : allTerms) {
            if (isPrivate(term)) {
                privateTerms.add(term);
            }
        }
        return privateTerms;
    }
    
    /**
     * Set default visibility level for terms not explicitly configured
     * @param defaultLevel New default level
     */
    public void setDefaultLevel(VisibilityLevel defaultLevel) {
        this.defaultLevel = defaultLevel != null ? defaultLevel : VisibilityLevel.PUBLIC;
    }
    
    /**
     * Get current default visibility level
     * @return Default visibility level
     */
    public VisibilityLevel getDefaultLevel() {
        return defaultLevel;
    }
    
    /**
     * Get all explicitly configured terms
     * @return Set of terms with explicit visibility settings
     */
    public Set<String> getExplicitlyConfiguredTerms() {
        return new HashSet<>(termVisibility.keySet());
    }
    
    /**
     * Clear all term visibility settings
     */
    public void clear() {
        termVisibility.clear();
    }
    
    /**
     * Get total number of configured terms
     * @return Number of terms with explicit visibility settings
     */
    public int size() {
        return termVisibility.size();
    }
    
    /**
     * Check if no terms are explicitly configured
     * @return true if no explicit configurations exist
     */
    public boolean isEmpty() {
        return termVisibility.isEmpty();
    }
    
    /**
     * Create copy of this visibility map
     * @return New TermVisibilityMap with same settings
     */
    public TermVisibilityMap copy() {
        TermVisibilityMap copy = new TermVisibilityMap(this.defaultLevel);
        copy.termVisibility.putAll(this.termVisibility);
        return copy;
    }
    
    @Override
    public String toString() {
        return String.format("TermVisibilityMap{default=%s, explicit=%d terms}", 
                           defaultLevel, termVisibility.size());
    }
    
    /**
     * Get detailed string representation
     * @return Detailed view of all term visibility settings
     */
    public String toDetailedString() {
        StringBuilder sb = new StringBuilder();
        sb.append("TermVisibilityMap:\n");
        sb.append("  Default Level: ").append(defaultLevel).append("\n");
        sb.append("  Explicit Terms:\n");
        
        if (termVisibility.isEmpty()) {
            sb.append("    (none)\n");
        } else {
            termVisibility.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> sb.append("    ")
                    .append(entry.getKey())
                    .append(" -> ")
                    .append(entry.getValue())
                    .append("\n"));
        }
        
        return sb.toString();
    }
}