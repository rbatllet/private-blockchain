package com.rbatllet.blockchain.service;

import com.rbatllet.blockchain.entity.Block;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.*;

/**
 * Advanced search result container with rich metadata and analytics
 * Provides comprehensive search results with performance metrics and insights
 */
public class AdvancedSearchResult {
    
    private final String searchQuery;
    private final SearchType searchType;
    private final List<SearchMatch> matches;
    private SearchStatistics statistics;
    private final LocalDateTime searchTimestamp;
    private final Duration searchDuration;
    private final Map<String, Integer> categoryDistribution;
    private final List<String> suggestedRefinements;
    
    public enum SearchType {
        KEYWORD_SEARCH("🔍 Keyword Search"),
        REGEX_SEARCH("🔤 Regex Search"),
        SEMANTIC_SEARCH("🧠 Semantic Search"),
        TIME_RANGE_SEARCH("📅 Time Range Search"),
        COMBINED_SEARCH("🔗 Combined Search");
        
        private final String displayName;
        
        SearchType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() { return displayName; }
    }
    
    public static class SearchMatch {
        private final Block block;
        private final double relevanceScore;
        private final List<String> matchedTerms;
        private final Map<String, String> highlightedSnippets;
        private final MatchLocation location;
        
        public enum MatchLocation {
            BLOCK_DATA("📝 Block Data"),
            MANUAL_KEYWORDS("🏷️ Manual Keywords"),
            AUTO_KEYWORDS("🤖 Auto Keywords"),
            OFF_CHAIN_DATA("💾 Off-Chain Data"),
            METADATA("📋 Metadata");
            
            private final String displayName;
            
            MatchLocation(String displayName) {
                this.displayName = displayName;
            }
            
            public String getDisplayName() { return displayName; }
        }
        
        public SearchMatch(Block block, double relevanceScore, List<String> matchedTerms, 
                          Map<String, String> highlightedSnippets, MatchLocation location) {
            this.block = block;
            this.relevanceScore = relevanceScore;
            this.matchedTerms = new ArrayList<>(matchedTerms);
            this.highlightedSnippets = new HashMap<>(highlightedSnippets);
            this.location = location;
        }
        
        // Getters
        public Block getBlock() { return block; }
        public double getRelevanceScore() { return relevanceScore; }
        public List<String> getMatchedTerms() { return Collections.unmodifiableList(matchedTerms); }
        public Map<String, String> getHighlightedSnippets() { return Collections.unmodifiableMap(highlightedSnippets); }
        public MatchLocation getLocation() { return location; }
    }
    
    public static class SearchStatistics {
        private int totalBlocksSearched;
        private int encryptedBlocksDecrypted;
        private int offChainFilesAccessed;
        private long totalBytesProcessed;
        private Map<String, Integer> performanceMetrics;
        
        public SearchStatistics() {
            this.performanceMetrics = new HashMap<>();
        }
        
        // Builder methods
        public SearchStatistics withBlocksSearched(int count) {
            this.totalBlocksSearched = count;
            return this;
        }
        
        public SearchStatistics withEncryptedBlocks(int count) {
            this.encryptedBlocksDecrypted = count;
            return this;
        }
        
        public SearchStatistics withOffChainFiles(int count) {
            this.offChainFilesAccessed = count;
            return this;
        }
        
        public SearchStatistics withBytesProcessed(long bytes) {
            this.totalBytesProcessed = bytes;
            return this;
        }
        
        public SearchStatistics addMetric(String metricName, int value) {
            this.performanceMetrics.put(metricName, value);
            return this;
        }
        
        // Getters
        public int getTotalBlocksSearched() { return totalBlocksSearched; }
        public int getEncryptedBlocksDecrypted() { return encryptedBlocksDecrypted; }
        public int getOffChainFilesAccessed() { return offChainFilesAccessed; }
        public long getTotalBytesProcessed() { return totalBytesProcessed; }
        public Map<String, Integer> getPerformanceMetrics() { return Collections.unmodifiableMap(performanceMetrics); }
    }
    
    public AdvancedSearchResult(String searchQuery, SearchType searchType, Duration searchDuration) {
        this.searchQuery = searchQuery;
        this.searchType = searchType;
        this.matches = new ArrayList<>();
        this.statistics = new SearchStatistics();
        this.searchTimestamp = LocalDateTime.now();
        this.searchDuration = searchDuration;
        this.categoryDistribution = new HashMap<>();
        this.suggestedRefinements = new ArrayList<>();
    }
    
    // Builder methods
    public AdvancedSearchResult addMatch(SearchMatch match) {
        this.matches.add(match);
        
        // Update category distribution
        String category = match.getBlock().getContentCategory();
        if (category != null) {
            categoryDistribution.merge(category, 1, Integer::sum);
        }
        
        return this;
    }
    
    public AdvancedSearchResult withStatistics(SearchStatistics stats) {
        this.statistics = stats;
        return this;
    }
    
    public AdvancedSearchResult addSuggestedRefinement(String refinement) {
        this.suggestedRefinements.add(refinement);
        return this;
    }
    
    // Analysis methods
    public List<SearchMatch> getTopMatches(int limit) {
        return matches.stream()
            .sorted((a, b) -> Double.compare(b.getRelevanceScore(), a.getRelevanceScore()))
            .limit(limit)
            .collect(java.util.stream.Collectors.toList());
    }
    
    public Map<String, List<SearchMatch>> groupByCategory() {
        Map<String, List<SearchMatch>> grouped = new HashMap<>();
        for (SearchMatch match : matches) {
            String category = match.getBlock().getContentCategory();
            if (category == null) category = "UNCATEGORIZED";
            grouped.computeIfAbsent(category, k -> new ArrayList<>()).add(match);
        }
        return grouped;
    }
    
    public double getAverageRelevanceScore() {
        if (matches.isEmpty()) return 0.0;
        return matches.stream()
            .mapToDouble(SearchMatch::getRelevanceScore)
            .average()
            .orElse(0.0);
    }
    
    // Getters
    public String getSearchQuery() { return searchQuery; }
    public SearchType getSearchType() { return searchType; }
    public List<SearchMatch> getMatches() { return Collections.unmodifiableList(matches); }
    public SearchStatistics getStatistics() { return statistics; }
    public LocalDateTime getSearchTimestamp() { return searchTimestamp; }
    public Duration getSearchDuration() { return searchDuration; }
    public Map<String, Integer> getCategoryDistribution() { return Collections.unmodifiableMap(categoryDistribution); }
    public List<String> getSuggestedRefinements() { return Collections.unmodifiableList(suggestedRefinements); }
    public int getTotalMatches() { return matches.size(); }
    
    // Formatted output
    public String getFormattedSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("🔍 Search Results for: \"%s\"\n", searchQuery));
        sb.append(String.format("📊 Type: %s\n", searchType.getDisplayName()));
        sb.append(String.format("📈 Total Matches: %d\n", getTotalMatches()));
        sb.append(String.format("⏱️ Search Duration: %dms\n", searchDuration.toMillis()));
        
        if (!categoryDistribution.isEmpty()) {
            sb.append("\n📂 Category Distribution:\n");
            categoryDistribution.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(entry -> sb.append(String.format("  - %s: %d matches\n", 
                    entry.getKey(), entry.getValue())));
        }
        
        if (!suggestedRefinements.isEmpty()) {
            sb.append("\n💡 Suggested Refinements:\n");
            suggestedRefinements.forEach(ref -> sb.append("  - ").append(ref).append("\n"));
        }
        
        return sb.toString();
    }
}