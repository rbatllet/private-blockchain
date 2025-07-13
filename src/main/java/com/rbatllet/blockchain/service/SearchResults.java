package com.rbatllet.blockchain.service;

import com.rbatllet.blockchain.entity.Block;
import java.time.LocalDateTime;
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
        this.metrics = new SearchMetrics();
        this.timestamp = LocalDateTime.now();
        this.searchDetails = new HashMap<>();
        this.warnings = new ArrayList<>();
    }
    
    // Getters
    public String getQuery() { return query; }
    public List<Block> getBlocks() { return blocks; }
    public SearchMetrics getMetrics() { return metrics; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public Map<String, Object> getSearchDetails() { return searchDetails; }
    public List<String> getWarnings() { return warnings; }
    
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
        this.metrics.setSearchTimeMs(searchTimeMs);
        this.metrics.setOnChainResults(onChainResults);
        this.metrics.setOffChainResults(offChainResults);
        this.metrics.setCacheHit(cacheHit);
        this.metrics.setSearchType(searchType);
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
    
    /**
     * Search performance metrics
     */
    public static class SearchMetrics {
        private long searchTimeMs = 0;
        private int onChainResults = 0;
        private int offChainResults = 0;
        private boolean cacheHit = false;
        private String searchType = "UNKNOWN";
        
        // Getters and setters
        public long getSearchTimeMs() { return searchTimeMs; }
        public void setSearchTimeMs(long searchTimeMs) { this.searchTimeMs = searchTimeMs; }
        
        public int getOnChainResults() { return onChainResults; }
        public void setOnChainResults(int onChainResults) { this.onChainResults = onChainResults; }
        
        public int getOffChainResults() { return offChainResults; }
        public void setOffChainResults(int offChainResults) { this.offChainResults = offChainResults; }
        
        public boolean isCacheHit() { return cacheHit; }
        public void setCacheHit(boolean cacheHit) { this.cacheHit = cacheHit; }
        
        public String getSearchType() { return searchType; }
        public void setSearchType(String searchType) { this.searchType = searchType; }
        
        public int getTotalResults() { return onChainResults + offChainResults; }
        
        public double getSearchSpeed() {
            return searchTimeMs > 0 ? (double) getTotalResults() / searchTimeMs * 1000 : 0;
        }
        
        @Override
        public String toString() {
            return String.format("Metrics: %dms (%s), %d on-chain + %d off-chain = %d total%s", 
                               searchTimeMs, searchType, onChainResults, offChainResults, 
                               getTotalResults(), cacheHit ? " [CACHED]" : "");
        }
    }
}