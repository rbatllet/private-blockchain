package com.rbatllet.blockchain.service;

import com.rbatllet.blockchain.entity.Block;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * Enhanced search results container for comprehensive search operations
 * Provides detailed information about search performance and result quality
 */
public class SearchResults {
    
    private final String query;
    private final List<Block> blocks;
    private final SearchMetrics metrics;
    private final LocalDateTime timestamp;
    private final Map<String, Object> searchDetails;
    private final List<String> warnings;
    
    public SearchResults(String query, List<Block> blocks) {
        this.query = query;
        this.blocks = new ArrayList<>(blocks);
        this.metrics = new SearchMetrics(); // Using standalone SearchMetrics class
        this.timestamp = LocalDateTime.now();
        this.searchDetails = new HashMap<>();
        this.warnings = new ArrayList<>();
    }
    
    // Getters
    public String getQuery() { return query; }
    public List<Block> getBlocks() { return Collections.unmodifiableList(blocks); }
    public SearchMetrics getMetrics() { return metrics; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public Map<String, Object> getSearchDetails() { return Collections.unmodifiableMap(searchDetails); }
    public List<String> getWarnings() { return Collections.unmodifiableList(warnings); }
    
    public int getResultCount() { return blocks.size(); }
    public boolean hasResults() { return !blocks.isEmpty(); }
    
    // Builder methods
    public SearchResults addDetail(String key, Object value) {
        this.searchDetails.put(key, value);
        return this;
    }
    
    public SearchResults addWarning(String warning) {
        this.warnings.add(warning);
        return this;
    }
    
    public SearchResults withMetrics(long searchTimeMs, int onChainResults, int offChainResults, 
                                   boolean cacheHit, String searchType) {
        // Record the search using the new SearchMetrics API
        int totalResults = onChainResults + offChainResults;
        this.metrics.recordSearch(searchType, searchTimeMs, totalResults, cacheHit);
        return this;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("🔍 Search Results\n");
        sb.append("Query: \"").append(query).append("\"\n");
        sb.append("Results: ").append(blocks.size()).append(" blocks found\n");
        sb.append("📊 ").append(metrics.toString()).append("\n");
        
        if (!warnings.isEmpty()) {
            sb.append("⚠️ Warnings:\n");
            warnings.forEach(w -> sb.append("  - ").append(w).append("\n"));
        }
        
        if (!searchDetails.isEmpty()) {
            sb.append("📝 Search Details:\n");
            searchDetails.forEach((key, value) -> 
                sb.append("  ").append(key).append(": ").append(value).append("\n"));
        }
        
        sb.append("📅 Executed: ").append(timestamp);
        
        return sb.toString();
    }
    
}