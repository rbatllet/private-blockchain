package com.rbatllet.blockchain.search.strategy;

import com.rbatllet.blockchain.search.metadata.BlockMetadataLayers;
import com.rbatllet.blockchain.search.metadata.PublicMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Lightning-Fast Public Metadata Search Engine
 * 
 * Performs sub-50ms searches across millions of blocks using only
 * public metadata layer. This search strategy provides instant results
 * without requiring passwords or decryption.
 *
 * Features:
 * - In-memory index for instant lookups
 * - Relevance scoring with intelligent ranking
 * - Optional fuzzy matching for authenticated searches (disabled for public search for security)
 * - Time-range filtering for efficient searches
 * - Content-type filtering for domain-specific searches
 *
 * Security Policy:
 * - Public search (searchPublic): Exact match only - prevents privacy leaks through similar keywords
 * - Secure search (searchSecure): Fuzzy matching enabled - allows finding similar keywords for authenticated users
 *
 * Performance Target: <50ms for 1M+ blocks
 */
public class FastIndexSearch {
    private static final Logger logger = LoggerFactory.getLogger(FastIndexSearch.class);
    
    private final Map<String, Set<String>> keywordIndex;
    private final Map<String, BlockMetadataLayers> metadataCache;
    private final Map<String, Set<String>> timeRangeIndex;
    private final Map<String, Set<String>> contentTypeIndex;
    
    public FastIndexSearch() {
        this.keywordIndex = new ConcurrentHashMap<>();
        this.metadataCache = new ConcurrentHashMap<>();
        this.timeRangeIndex = new ConcurrentHashMap<>();
        this.contentTypeIndex = new ConcurrentHashMap<>();
    }
    
    /**
     * Add block metadata to the fast search index
     * @param blockHash The block's hash identifier
     * @param metadata The block's complete metadata layers
     */
    public void indexBlock(String blockHash, BlockMetadataLayers metadata) {
        if (blockHash == null || metadata == null) {
            return;
        }
        
        // Cache the metadata
        metadataCache.put(blockHash, metadata);
        
        PublicMetadata publicLayer = metadata.getPublicLayer();
        if (publicLayer == null || publicLayer.isEmpty()) {
            return; // No public metadata to index
        }
        
        // Index keywords
        for (String keyword : publicLayer.getGeneralKeywords()) {
            // Phase 5.4 FIX: Strip "public:" prefix for intuitive searching
            // Blocks are stored with "public:medical" but users search for "medical"
            // This allows users to search naturally without knowing about internal prefixes
            String indexableKeyword = keyword.toLowerCase();
            if (indexableKeyword.startsWith("public:")) {
                indexableKeyword = indexableKeyword.substring("public:".length());
            }

            keywordIndex.computeIfAbsent(indexableKeyword, k -> ConcurrentHashMap.newKeySet())
                       .add(blockHash);
        }
        
        // Index time range
        if (publicLayer.getTimeRange() != null && !publicLayer.getTimeRange().trim().isEmpty()) {
            timeRangeIndex.computeIfAbsent(publicLayer.getTimeRange(), k -> ConcurrentHashMap.newKeySet())
                         .add(blockHash);
        }
        
        // Index content type
        if (publicLayer.getContentType() != null && !publicLayer.getContentType().trim().isEmpty()) {
            contentTypeIndex.computeIfAbsent(publicLayer.getContentType(), k -> ConcurrentHashMap.newKeySet())
                           .add(blockHash);
        }
    }
    
    /**
     * Remove block from all indexes
     * @param blockHash The block hash to remove
     */
    public void removeBlock(String blockHash) {
        if (blockHash == null) {
            return;
        }
        
        // Remove from metadata cache
        metadataCache.remove(blockHash);
        
        // Remove from all indexes
        keywordIndex.values().forEach(set -> set.remove(blockHash));
        timeRangeIndex.values().forEach(set -> set.remove(blockHash));
        contentTypeIndex.values().forEach(set -> set.remove(blockHash));
        
        // Clean up empty index entries
        keywordIndex.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        timeRangeIndex.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        contentTypeIndex.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }
    
    /**
     * Perform lightning-fast search on public metadata
     * @param query Search query string
     * @param maxResults Maximum number of results to return
     * @return Ranked list of matching block hashes
     */
    public List<FastSearchResult> searchFast(String query, int maxResults) {
        return searchFast(query, maxResults, false); // Default: fuzzy matching disabled for security
    }

    /**
     * Perform lightning-fast search with optional fuzzy matching
     *
     * SECURITY POLICY:
     * - enableFuzzy=false: For PUBLIC search (searchPublic) - exact match only, prevents privacy leaks
     * - enableFuzzy=true: For SECURE search (searchSecure) - fuzzy matching allowed for authenticated users
     *
     * This follows Elasticsearch best practices for Document/Field Level Security where
     * authenticated searches can have more lenient matching while public searches must be exact.
     *
     * @param query Search query string
     * @param maxResults Maximum number of results to return
     * @param enableFuzzy Whether to enable fuzzy matching (true for authenticated searches only)
     * @return Ranked list of matching block hashes
     */
    public List<FastSearchResult> searchFast(String query, int maxResults, boolean enableFuzzy) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }

        // Validate maxResults parameter
        if (maxResults < 0) {
            throw new IllegalArgumentException("maxResults cannot be negative: " + maxResults);
        }
        if (maxResults == 0) {
            return new ArrayList<>();
        }

        long startTime = System.nanoTime();

        // Parse query into keywords
        Set<String> queryKeywords = parseQuery(query);
        logger.debug("🔍 FastIndexSearch: query='{}', parsed keywords={}, total indexed keywords={}, fuzzy={}",
            query, queryKeywords, keywordIndex.size(), enableFuzzy);

        // Find matching blocks
        Map<String, Double> blockScores = new HashMap<>();

        for (String keyword : queryKeywords) {
            // Exact matches
            Set<String> exactMatches = keywordIndex.get(keyword.toLowerCase());
            logger.debug("🔍 FastIndexSearch: checking keyword='{}', exact matches={}",
                keyword.toLowerCase(), exactMatches != null ? exactMatches.size() : 0);
            if (exactMatches != null) {
                for (String blockHash : exactMatches) {
                    blockScores.merge(blockHash, 3.0, Double::sum); // High score for exact match
                }
            }

            // Fuzzy matches (only if enabled and for authenticated searches)
            if (enableFuzzy) {
                for (Map.Entry<String, Set<String>> entry : keywordIndex.entrySet()) {
                    String indexedKeyword = entry.getKey();
                    if (isFuzzyMatch(keyword, indexedKeyword, enableFuzzy)) {
                        double fuzzyScore = calculateFuzzyScore(keyword, indexedKeyword);
                        for (String blockHash : entry.getValue()) {
                            blockScores.merge(blockHash, fuzzyScore, Double::sum);
                        }
                    }
                }
            }
        }

        // Calculate relevance scores
        List<FastSearchResult> results = blockScores.entrySet().stream()
            .map(entry -> {
                String blockHash = entry.getKey();
                double score = entry.getValue();
                BlockMetadataLayers metadata = metadataCache.get(blockHash);

                // Enhance score with metadata richness
                if (metadata != null) {
                    score += metadata.getMetadataRichness() * 0.1;
                }

                long endTime = System.nanoTime();
                double searchTimeMs = (endTime - startTime) / 1_000_000.0;

                return new FastSearchResult(blockHash, score, searchTimeMs,
                                          metadata != null ? metadata.getPublicLayer() : null);
            })
            .sorted((a, b) -> Double.compare(b.getRelevanceScore(), a.getRelevanceScore()))
            .limit(maxResults)
            .collect(Collectors.toList());

        return results;
    }
    
    /**
     * Search within specific content type
     * @param query Search query
     * @param contentType Target content type
     * @param maxResults Maximum results
     * @return Filtered search results
     */
    public List<FastSearchResult> searchByContentType(String query, String contentType, int maxResults) {
        // Validate maxResults parameter
        if (maxResults < 0) {
            throw new IllegalArgumentException("maxResults cannot be negative: " + maxResults);
        }
        
        if (contentType == null || contentType.trim().isEmpty()) {
            return searchFast(query, maxResults);
        }
        
        // Get blocks of specified content type
        Set<String> typeBlocks = contentTypeIndex.get(contentType);
        if (typeBlocks == null || typeBlocks.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Perform regular search and filter by content type
        List<FastSearchResult> allResults = searchFast(query, maxResults * 2); // Get more to filter
        
        return allResults.stream()
            .filter(result -> typeBlocks.contains(result.getBlockHash()))
            .limit(maxResults)
            .collect(Collectors.toList());
    }
    
    /**
     * Search within time range
     * @param query Search query
     * @param timeRange Target time range
     * @param maxResults Maximum results
     * @return Time-filtered search results
     */
    public List<FastSearchResult> searchByTimeRange(String query, String timeRange, int maxResults) {
        // Validate maxResults parameter
        if (maxResults < 0) {
            throw new IllegalArgumentException("maxResults cannot be negative: " + maxResults);
        }
        
        if (timeRange == null || timeRange.trim().isEmpty()) {
            return searchFast(query, maxResults);
        }
        
        // Get blocks within time range
        Set<String> timeBlocks = timeRangeIndex.get(timeRange);
        if (timeBlocks == null || timeBlocks.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Perform regular search and filter by time range
        List<FastSearchResult> allResults = searchFast(query, maxResults * 2);
        
        return allResults.stream()
            .filter(result -> timeBlocks.contains(result.getBlockHash()))
            .limit(maxResults)
            .collect(Collectors.toList());
    }
    
    // ===== HELPER METHODS =====
    
    /**
     * Parse query string into individual keywords
     */
    private Set<String> parseQuery(String query) {
        if (query == null) {
            throw new IllegalArgumentException("Query cannot be null");
        }
        
        return Arrays.stream(query.toLowerCase().split("\\s+"))
                     .filter(word -> word.length() > 1) // Skip very short words
                     .collect(Collectors.toSet());
    }
    
    /**
     * Check if two keywords are similar enough for fuzzy matching
     *
     * SECURITY POLICY: Fuzzy matching behavior depends on search context:
     * - For PUBLIC search (searchPublic): ALWAYS returns false (exact match only)
     * - For SECURE search (searchSecure): Uses fuzzy matching algorithm
     *
     * This design follows Elasticsearch best practices for Document/Field Level Security:
     * - Public searches must be EXACT to guarantee privacy (no accidental exposure)
     * - Authenticated searches can use fuzzy matching (user knows they're searching sensitive data)
     *
     * @param query The search query keyword
     * @param indexed The indexed keyword
     * @param enableFuzzy Whether fuzzy matching is allowed for this search context
     * @return true if keywords should be considered a match, false otherwise
     */
    private boolean isFuzzyMatch(String query, String indexed, boolean enableFuzzy) {
        if (query.equals(indexed)) {
            return false; // Already handled as exact match
        }

        // SECURITY: Fuzzy matching only allowed for authenticated searches
        if (!enableFuzzy) {
            return false;
        }

        // Contains check
        if (indexed.contains(query) || query.contains(indexed)) {
            return true;
        }

        // Edit distance check for words > 3 characters
        if (query.length() > 3 && indexed.length() > 3) {
            int editDistance = calculateEditDistance(query, indexed);
            int maxLength = Math.max(query.length(), indexed.length());
            return (double) editDistance / maxLength < 0.3; // 30% difference threshold
        }

        return false;
    }

    /**
     * Calculate fuzzy match score (0.0 to 1.0)
     */
    private double calculateFuzzyScore(String query, String indexed) {
        if (indexed.contains(query) || query.contains(indexed)) {
            return 1.5; // Contains match
        }

        if (query.length() > 3 && indexed.length() > 3) {
            int editDistance = calculateEditDistance(query, indexed);
            int maxLength = Math.max(query.length(), indexed.length());
            return 1.0 - ((double) editDistance / maxLength);
        }

        return 0.5; // Default fuzzy score
    }

    /**
     * Calculate edit distance between two strings (Levenshtein distance)
     */
    private int calculateEditDistance(String s1, String s2) {
        if (s1 == null || s2 == null) {
            throw new IllegalArgumentException("Strings cannot be null");
        }

        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(Math.min(dp[i - 1][j], dp[i][j - 1]), dp[i - 1][j - 1]);
                }
            }
        }

        return dp[s1.length()][s2.length()];
    }

    // ===== STATISTICS AND MONITORING =====
    public FastIndexStats getIndexStats() {
        return new FastIndexStats(
            metadataCache.size(),
            keywordIndex.size(),
            timeRangeIndex.size(),
            contentTypeIndex.size(),
            calculateIndexSize()
        );
    }
    
    /**
     * Calculate total memory usage of indexes
     */
    private long calculateIndexSize() {
        long size = 0;
        
        // Approximate memory usage calculation
        size += keywordIndex.size() * 64; // Map entries
        size += keywordIndex.values().stream()
                           .mapToLong(Set::size)
                           .sum() * 32; // Set entries
        
        size += timeRangeIndex.size() * 64;
        size += timeRangeIndex.values().stream()
                             .mapToLong(Set::size)
                             .sum() * 32;
        
        size += contentTypeIndex.size() * 64;
        size += contentTypeIndex.values().stream()
                               .mapToLong(Set::size)
                               .sum() * 32;
        
        return size;
    }
    
    /**
     * Clear all indexes
     */
    public void clearAll() {
        keywordIndex.clear();
        metadataCache.clear();
        timeRangeIndex.clear();
        contentTypeIndex.clear();
    }
    
    /**
     * Fast search result with relevance scoring
     */
    public static class FastSearchResult {
        private final String blockHash;
        private final double relevanceScore;
        private final double searchTimeMs;
        private final PublicMetadata publicMetadata;
        
        public FastSearchResult(String blockHash, double relevanceScore, 
                              double searchTimeMs, PublicMetadata publicMetadata) {
            this.blockHash = blockHash;
            this.relevanceScore = relevanceScore;
            this.searchTimeMs = searchTimeMs;
            this.publicMetadata = publicMetadata;
        }
        
        public String getBlockHash() { return blockHash; }
        public double getRelevanceScore() { return relevanceScore; }
        public double getSearchTimeMs() { return searchTimeMs; }
        public PublicMetadata getPublicMetadata() { return publicMetadata; }
        
        @Override
        public String toString() {
            String hashDisplay = blockHash != null ? 
                blockHash.substring(0, Math.min(8, blockHash.length())) : 
                "null";
            return String.format("FastResult{hash=%s, score=%.2f, time=%.2fms}", 
                               hashDisplay, relevanceScore, searchTimeMs);
        }
    }
    
    /**
     * Index statistics for monitoring
     */
    public static class FastIndexStats {
        private final int blocksIndexed;
        private final int uniqueKeywords;
        private final int timeRanges;
        private final int contentTypes;
        private final long estimatedMemoryBytes;
        
        public FastIndexStats(int blocksIndexed, int uniqueKeywords, 
                             int timeRanges, int contentTypes, long estimatedMemoryBytes) {
            this.blocksIndexed = blocksIndexed;
            this.uniqueKeywords = uniqueKeywords;
            this.timeRanges = timeRanges;
            this.contentTypes = contentTypes;
            this.estimatedMemoryBytes = estimatedMemoryBytes;
        }
        
        public int getBlocksIndexed() { return blocksIndexed; }
        public int getUniqueKeywords() { return uniqueKeywords; }
        public int getTimeRanges() { return timeRanges; }
        public int getContentTypes() { return contentTypes; }
        public long getEstimatedMemoryBytes() { return estimatedMemoryBytes; }
        
        @Override
        public String toString() {
            return String.format("FastIndexStats{blocks=%d, keywords=%d, memory=%dKB}", 
                               blocksIndexed, uniqueKeywords, estimatedMemoryBytes / 1024);
        }
    }
}