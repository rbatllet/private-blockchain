package com.rbatllet.blockchain.service;

import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.search.SearchResultInterface;
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
public class SearchResults implements SearchResultInterface {
    
    private final String query;
    private final List<Block> blocks;
    private final SearchMetrics metrics;
    private final LocalDateTime timestamp;
    private final Map<String, Object> searchDetails;
    private final List<String> warnings;
    
    public SearchResults(String query, List<Block> blocks) {
        // Defensive programming: sanitize and validate inputs
        this.query = (query != null) ? query : "";
        this.blocks = (blocks != null) ? new ArrayList<>(blocks) : new ArrayList<>();
        this.metrics = new SearchMetrics(); // Using standalone SearchMetrics class
        this.timestamp = LocalDateTime.now();
        this.searchDetails = new HashMap<>();
        this.warnings = new ArrayList<>();
    }
    
    // SearchResultInterface implementation
    @Override
    public String getSearchTerm() {
        return query != null ? query : "";
    }

    @Override
    public int getMatchCount() {
        return blocks != null ? blocks.size() : 0;
    }

    @Override
    public boolean hasResults() {
        return blocks != null && !blocks.isEmpty();
    }

    @Override
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String getSearchSummary() {
        return String.format("🔍 Search for '%s': %d blocks found at %s",
            getSearchTerm(),
            getMatchCount(),
            timestamp != null ? timestamp.toString() : "unknown time");
    }

    // Getters
    public String getQuery() { return (query != null) ? query : ""; }
    public List<Block> getBlocks() { return Collections.unmodifiableList(blocks); }
    public SearchMetrics getMetrics() { return metrics; }
    public Map<String, Object> getSearchDetails() { return Collections.unmodifiableMap(searchDetails); }
    public List<String> getWarnings() { return Collections.unmodifiableList(warnings); }

    public int getResultCount() { return (blocks != null) ? blocks.size() : 0; }
    
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
        
        // Defensive null handling for query
        String safeQuery = (query != null) ? query : "";
        sb.append("Query: \"").append(safeQuery).append("\"\n");
        
        // Defensive null handling for blocks
        int blockCount = (blocks != null) ? blocks.size() : 0;
        sb.append("Results: ").append(blockCount).append(" blocks found\n");
        
        // Metrics should always be initialized, but add safety
        if (metrics != null) {
            sb.append("📊 ").append(metrics.toString()).append("\n");
        }
        
        // Warnings list should always be initialized
        if (warnings != null && !warnings.isEmpty()) {
            sb.append("⚠️ Warnings:\n");
            warnings.forEach(w -> {
                if (w != null) {
                    sb.append("  - ").append(w).append("\n");
                }
            });
        }
        
        // Search details map should always be initialized
        if (searchDetails != null && !searchDetails.isEmpty()) {
            sb.append("📝 Search Details:\n");
            searchDetails.forEach((key, value) -> {
                if (key != null) {
                    sb.append("  ").append(key).append(": ");
                    sb.append((value != null) ? value.toString() : "null");
                    sb.append("\n");
                }
            });
        }
        
        // Timestamp should always be initialized
        sb.append("📅 Executed: ");
        sb.append((timestamp != null) ? timestamp.toString() : "Unknown");
        
        return sb.toString();
    }
    
}