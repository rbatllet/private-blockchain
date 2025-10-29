package com.rbatllet.blockchain.service;

import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.search.SearchResultInterface;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.*;
import java.util.Objects;

/**
 * Advanced search result container with rich metadata and analytics
 * Provides comprehensive search results with performance metrics and insights
 */
public class AdvancedSearchResult implements SearchResultInterface {
    
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
        private long totalBlocksSearched;
        private long encryptedBlocksDecrypted;
        private long offChainFilesAccessed;
        private long totalBytesProcessed;
        private Map<String, Integer> performanceMetrics;

        public SearchStatistics() {
            this.performanceMetrics = new HashMap<>();
        }

        // Builder methods
        public SearchStatistics withBlocksSearched(long count) {
            this.totalBlocksSearched = count;
            return this;
        }

        public SearchStatistics withEncryptedBlocks(long count) {
            this.encryptedBlocksDecrypted = count;
            return this;
        }

        public SearchStatistics withOffChainFiles(long count) {
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
        public long getTotalBlocksSearched() { return totalBlocksSearched; }
        public long getEncryptedBlocksDecrypted() { return encryptedBlocksDecrypted; }
        public long getOffChainFilesAccessed() { return offChainFilesAccessed; }
        public long getTotalBytesProcessed() { return totalBytesProcessed; }
        public Map<String, Integer> getPerformanceMetrics() { return Collections.unmodifiableMap(performanceMetrics); }
    }
    
    public AdvancedSearchResult(String searchQuery, SearchType searchType, Duration searchDuration) {
        // Defensive programming: sanitize and validate inputs
        this.searchQuery = (searchQuery != null) ? searchQuery : "";
        this.searchType = (searchType != null) ? searchType : SearchType.KEYWORD_SEARCH;
        this.matches = new ArrayList<>();
        this.statistics = new SearchStatistics();
        this.searchTimestamp = LocalDateTime.now();
        this.searchDuration = (searchDuration != null) ? searchDuration : Duration.ZERO;
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

    /**
     * Sort matches by relevance score in descending order (highest score first)
     * Modifies the internal matches list to ensure results are properly ordered
     *
     * @return this instance for method chaining
     */
    public AdvancedSearchResult sortByRelevance() {
        if (matches != null && !matches.isEmpty()) {
            matches.sort((a, b) -> Double.compare(b.getRelevanceScore(), a.getRelevanceScore()));
        }
        return this;
    }

    // Analysis methods
    public List<SearchMatch> getTopMatches(int limit) {
        // Defensive programming: validate inputs and handle null scenarios
        if (matches == null || matches.isEmpty()) {
            return Collections.emptyList();
        }
        
        int safeLimit = Math.max(0, limit);  // Prevent negative limit
        List<SearchMatch> result = matches.stream()
            .filter(Objects::nonNull)  // Filter out null matches
            .sorted((a, b) -> Double.compare(b.getRelevanceScore(), a.getRelevanceScore()))
            .limit(safeLimit)
            .collect(java.util.stream.Collectors.toList());
        
        return Collections.unmodifiableList(result);
    }
    
    public Map<String, List<SearchMatch>> groupByCategory() {
        Map<String, List<SearchMatch>> grouped = new HashMap<>();
        
        // Defensive programming: handle null matches collection
        if (matches == null) {
            return Collections.unmodifiableMap(grouped);
        }
        
        for (SearchMatch match : matches) {
            // Handle null match or null block
            if (match == null || match.getBlock() == null) {
                continue;
            }
            
            String category = match.getBlock().getContentCategory();
            if (category == null || category.trim().isEmpty()) {
                category = "UNCATEGORIZED";
            }
            
            grouped.computeIfAbsent(category, k -> new ArrayList<>()).add(match);
        }
        
        return Collections.unmodifiableMap(grouped);  // Return immutable map
    }
    
    public double getAverageRelevanceScore() {
        // Defensive programming: handle null matches and empty collections
        if (matches == null || matches.isEmpty()) {
            return 0.0;
        }
        
        return matches.stream()
            .filter(Objects::nonNull)  // Filter out null matches
            .mapToDouble(match -> {
                double score = match.getRelevanceScore();
                // Handle NaN and infinite values
                return (Double.isNaN(score) || Double.isInfinite(score)) ? 0.0 : score;
            })
            .average()
            .orElse(0.0);
    }
    
    // SearchResultInterface implementation
    @Override
    public String getSearchTerm() {
        return searchQuery != null ? searchQuery : "";
    }

    @Override
    public int getMatchCount() {
        return matches != null ? matches.size() : 0;
    }

    @Override
    public boolean hasResults() {
        return matches != null && !matches.isEmpty();
    }

    @Override
    public LocalDateTime getTimestamp() {
        return searchTimestamp;
    }

    @Override
    public String getSearchSummary() {
        return String.format("🔍 %s: %d matches found in %dms",
            searchType != null ? searchType.getDisplayName() : "Search",
            getMatchCount(),
            searchDuration != null ? searchDuration.toMillis() : 0);
    }

    // Getters
    public String getSearchQuery() { return searchQuery; }
    public SearchType getSearchType() { return searchType; }
    public List<SearchMatch> getMatches() { return Collections.unmodifiableList(matches); }
    public SearchStatistics getStatistics() { return statistics; }
    public LocalDateTime getSearchTimestamp() { return searchTimestamp; }
    public Duration getSearchDuration() { return searchDuration; }
    public Map<String, Integer> getCategoryDistribution() {
        if (categoryDistribution == null) {
            return Collections.emptyMap();
        }
        // Return filtered immutable copy without null keys/values
        Map<String, Integer> filtered = new HashMap<>();
        categoryDistribution.entrySet().stream()
            .filter(entry -> entry.getKey() != null && entry.getValue() != null)
            .filter(entry -> !entry.getKey().trim().isEmpty())
            .filter(entry -> entry.getValue() >= 0)  // Ensure non-negative counts
            .forEach(entry -> filtered.put(entry.getKey(), entry.getValue()));
        
        return Collections.unmodifiableMap(filtered);
    }
    public List<String> getSuggestedRefinements() {
        if (suggestedRefinements == null) {
            return Collections.emptyList();
        }
        // Return filtered immutable copy without null elements
        return Collections.unmodifiableList(
            suggestedRefinements.stream()
                .filter(Objects::nonNull)
                .filter(s -> !s.trim().isEmpty())
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll)
        );
    }
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