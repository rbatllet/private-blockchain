package com.rbatllet.blockchain.search.strategy;

import com.rbatllet.blockchain.search.SearchLevel;
import com.rbatllet.blockchain.search.metadata.BlockMetadataLayers;
import com.rbatllet.blockchain.config.EncryptionConfig;
import com.rbatllet.blockchain.config.EncryptionConfig.SecurityLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Intelligent Search Strategy Router
 * 
 * Makes smart decisions about which search strategy to use based on:
 * - Query characteristics and complexity
 * - Available authentication (password presence)
 * - Security requirements and privacy level
 * - Performance requirements and time constraints
 * - Available metadata layers in the blockchain
 * 
 * The router can automatically escalate from fast public search to
 * encrypted content search when needed, providing the optimal balance
 * between speed, privacy, and completeness.
 */
public class SearchStrategyRouter {
    
    private static final Logger logger = LoggerFactory.getLogger(SearchStrategyRouter.class);
    
    private final FastIndexSearch fastIndexSearch;
    private final EncryptedContentSearch encryptedContentSearch;
    private final ExecutorService executorService;
    
    public SearchStrategyRouter() {
        this.fastIndexSearch = new FastIndexSearch();
        this.encryptedContentSearch = new EncryptedContentSearch();
        this.executorService = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "SearchStrategy-" + System.currentTimeMillis());
            t.setDaemon(true);
            return t;
        });
    }
    
    /**
     * Automatically route search to optimal strategy
     * @param query Search query string
     * @param password Optional password for encrypted content access
     * @param config Encryption configuration
     * @param maxResults Maximum number of results
     * @return Advanced search results from the optimal strategy
     */
    public SearchRoutingResult routeSearch(String query, String password, 
                                         EncryptionConfig config, int maxResults) {
        
        long startTime = System.nanoTime();
        
        // Validate input parameters
        if (query == null || query.trim().isEmpty()) {
            long endTime = System.nanoTime();
            double totalTimeMs = (endTime - startTime) / 1_000_000.0;
            
            // Create empty analysis for error case
            QueryAnalysis errorAnalysis = new QueryAnalysis(SearchStrategy.FAST_PUBLIC, 
                                                           QueryComplexity.SIMPLE, false, SecurityLevel.PERFORMANCE);
            
            // Create empty result with error
            AdvancedSearchResult errorResult = new AdvancedSearchResult(
                new ArrayList<>(), SearchLevel.FAST_ONLY, totalTimeMs);
            
            return new SearchRoutingResult(errorResult, SearchStrategy.FAST_PUBLIC, 
                                         errorAnalysis, totalTimeMs, "Query cannot be null or empty");
        }
        
        // Analyze query to determine optimal strategy
        QueryAnalysis analysis = analyzeQuery(query, password, config);
        SearchStrategy chosenStrategy = analysis.getRecommendedStrategy();
        
        try {
            AdvancedSearchResult result;
            
            switch (chosenStrategy) {
                case FAST_PUBLIC:
                    result = executePublicSearch(query, maxResults, analysis);
                    break;
                    
                case ENCRYPTED_CONTENT:
                    result = executeEncryptedSearch(query, password, maxResults, analysis);
                    break;
                    
                    
                case HYBRID_CASCADE:
                    result = executeHybridCascadeSearch(query, password, maxResults, analysis);
                    break;
                    
                case PARALLEL_MULTI:
                    result = executeParallelMultiSearch(query, password, maxResults, analysis);
                    break;
                    
                default:
                    // Fallback to fast public search
                    result = executePublicSearch(query, maxResults, analysis);
                    break;
            }
            
            long endTime = System.nanoTime();
            double totalTimeMs = (endTime - startTime) / 1_000_000.0;
            
            return new SearchRoutingResult(result, chosenStrategy, analysis, totalTimeMs);
            
        } catch (Exception e) {
            // If chosen strategy fails, try fast public search as fallback
            try {
                AdvancedSearchResult fallbackResult = executePublicSearch(query, maxResults, analysis);
                long endTime = System.nanoTime();
                double totalTimeMs = (endTime - startTime) / 1_000_000.0;
                
                return new SearchRoutingResult(fallbackResult, SearchStrategy.FAST_PUBLIC, 
                                             analysis, totalTimeMs, e.getMessage());
            } catch (Exception fallbackException) {
                throw new RuntimeException("All search strategies failed", fallbackException);
            }
        }
    }
    
    /**
     * Analyze query characteristics to determine optimal strategy
     */
    private QueryAnalysis analyzeQuery(String query, String password, EncryptionConfig config) {
        // Note: query validation is now handled in routeSearch method
        
        // Determine query complexity
        QueryComplexity complexity = analyzeQueryComplexity(query);
        
        // Check authentication availability
        boolean hasPassword = password != null && !password.trim().isEmpty();
        
        // Determine security requirements
        SecurityLevel securityLevel = config != null ? config.getSecurityLevel() : SecurityLevel.PERFORMANCE;
        
        // Determine recommended strategy
        SearchStrategy recommendedStrategy = determineOptimalStrategy(
            complexity, hasPassword, securityLevel);
        
        return new QueryAnalysis(recommendedStrategy, complexity, hasPassword, securityLevel);
    }
    
    /**
     * Analyze the complexity of a search query
     */
    private QueryComplexity analyzeQueryComplexity(String query) {
        String[] words = query.toLowerCase().split("\\s+");
        
        // Simple: 1-2 words, no special patterns
        if (words.length <= 2 && !hasSpecialPatterns(query)) {
            return QueryComplexity.SIMPLE;
        }
        
        // Complex: Many words, contains operators, or special patterns
        if (words.length > 5 || hasComplexPatterns(query)) {
            return QueryComplexity.COMPLEX;
        }
        
        // Medium: Everything else
        return QueryComplexity.MEDIUM;
    }
    
    /**
     * Check if query has special search patterns
     */
    private boolean hasSpecialPatterns(String query) {
        return query.contains("*") || query.contains("?") || 
               query.contains("\"") || query.contains(":");
    }
    
    /**
     * Check if query has complex search patterns
     */
    private boolean hasComplexPatterns(String query) {
        return query.toLowerCase().contains(" and ") || 
               query.toLowerCase().contains(" or ") ||
               query.toLowerCase().contains(" not ") ||
               query.contains("(") || query.contains(")") ||
               query.matches(".*\\d{4}-\\d{2}-\\d{2}.*"); // Date patterns
    }
    
    /**
     * Determine the optimal search strategy
     */
    private SearchStrategy determineOptimalStrategy(QueryComplexity complexity, 
                                                  boolean hasPassword, 
                                                  SecurityLevel securityLevel) {
        
        if (securityLevel == SecurityLevel.MAXIMUM && hasPassword) {
            return SearchStrategy.ENCRYPTED_CONTENT;
        }
        
        // Complex queries with password: Use hybrid cascade
        if (complexity == QueryComplexity.COMPLEX && hasPassword) {
            return SearchStrategy.HYBRID_CASCADE;
        }
        
        // Medium complexity with password: Use encrypted content search
        if (complexity == QueryComplexity.MEDIUM && hasPassword) {
            return SearchStrategy.ENCRYPTED_CONTENT;
        }
        
        // Balanced security with password: Use parallel multi-search
        if (securityLevel == SecurityLevel.BALANCED && hasPassword) {
            return SearchStrategy.PARALLEL_MULTI;
        }
        
        // Simple queries or no password: Use fast public search
        return SearchStrategy.FAST_PUBLIC;
    }
    
    // ===== STRATEGY EXECUTION METHODS =====
    
    /**
     * Execute fast public metadata search
     */
    private AdvancedSearchResult executePublicSearch(String query, int maxResults, QueryAnalysis analysis) {
        List<FastIndexSearch.FastSearchResult> fastResults = fastIndexSearch.searchFast(query, maxResults);
        
        List<SearchResultItem> items = fastResults.stream()
            .map(result -> new SearchResultItem(
                result.getBlockHash(),
                result.getRelevanceScore(),
                SearchResultSource.PUBLIC_METADATA,
                result.getPublicMetadata() != null ? 
                    result.getPublicMetadata().getGeneralKeywords().toString() : "",
                result.getSearchTimeMs()
            ))
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        
        return new AdvancedSearchResult(items, SearchLevel.FAST_ONLY, 
                                     fastResults.isEmpty() ? 0.0 : fastResults.get(0).getSearchTimeMs());
    }
    
    /**
     * Execute encrypted content search
     */
    private AdvancedSearchResult executeEncryptedSearch(String query, String password, 
                                                     int maxResults, QueryAnalysis analysis) {
        List<EncryptedContentSearch.EncryptedSearchResult> encryptedResults = 
            encryptedContentSearch.searchEncryptedContent(query, password, maxResults);
        
        List<SearchResultItem> items = encryptedResults.stream()
            .map(result -> new SearchResultItem(
                result.getBlockHash(),
                result.getRelevanceScore(),
                SearchResultSource.ENCRYPTED_CONTENT,
                result.getMatchingSummary(),
                result.getSearchTimeMs()
            ))
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        
        return new AdvancedSearchResult(items, SearchLevel.INCLUDE_DATA,
                                     encryptedResults.isEmpty() ? 0.0 : encryptedResults.get(0).getSearchTimeMs());
    }
    
    
    /**
     * Execute hybrid cascade search (fast -> encrypted)
     */
    private AdvancedSearchResult executeHybridCascadeSearch(String query, String password, 
                                                         int maxResults, QueryAnalysis analysis) {
        // Start with fast search
        AdvancedSearchResult fastResults = executePublicSearch(query, maxResults, analysis);
        
        // If fast search doesn't provide enough results, escalate to encrypted search
        if (fastResults.getResults().size() < maxResults / 2) {
            AdvancedSearchResult encryptedResults = executeEncryptedSearch(query, password, maxResults, analysis);
            
            // Merge results and deduplicate - use thread-safe collections
            Set<String> seenHashes = ConcurrentHashMap.newKeySet();
            List<SearchResultItem> mergedResults = Collections.synchronizedList(new ArrayList<>());
            
            // Add fast results first (higher priority)
            for (SearchResultItem item : fastResults.getResults()) {
                if (seenHashes.add(item.getBlockHash())) {
                    mergedResults.add(item);
                }
            }
            
            // Add encrypted results for unseen blocks
            for (SearchResultItem item : encryptedResults.getResults()) {
                if (seenHashes.add(item.getBlockHash()) && mergedResults.size() < maxResults) {
                    mergedResults.add(item);
                }
            }
            
            // Sort by relevance score
            mergedResults.sort((a, b) -> Double.compare(b.getRelevanceScore(), a.getRelevanceScore()));
            
            double totalTime = fastResults.getSearchTimeMs() + encryptedResults.getSearchTimeMs();
            return new AdvancedSearchResult(mergedResults, SearchLevel.INCLUDE_DATA, totalTime);
        }
        
        return fastResults;
    }
    
    /**
     * Execute parallel multi-strategy search
     */
    private AdvancedSearchResult executeParallelMultiSearch(String query, String password, 
                                                          int maxResults, QueryAnalysis analysis) {
        
        // Execute multiple search strategies in parallel
        CompletableFuture<AdvancedSearchResult> fastFuture = CompletableFuture
            .supplyAsync(() -> executePublicSearch(query, maxResults, analysis), executorService);
        
        CompletableFuture<AdvancedSearchResult> encryptedFuture = CompletableFuture
            .supplyAsync(() -> executeEncryptedSearch(query, password, maxResults, analysis), executorService);
        
        try {
            // Wait for both to complete
            AdvancedSearchResult fastResults = fastFuture.get();
            AdvancedSearchResult encryptedResults = encryptedFuture.get();
            
            // Merge and rank results - use thread-safe map for concurrent access
            Map<String, SearchResultItem> bestResults = new ConcurrentHashMap<>();
            
            // Process fast results
            for (SearchResultItem item : fastResults.getResults()) {
                bestResults.put(item.getBlockHash(), item);
            }
            
            // Process encrypted results, keeping higher scoring ones
            for (SearchResultItem item : encryptedResults.getResults()) {
                SearchResultItem existing = bestResults.get(item.getBlockHash());
                if (existing == null || item.getRelevanceScore() > existing.getRelevanceScore()) {
                    bestResults.put(item.getBlockHash(), item);
                }
            }
            
            // Sort and limit results
            List<SearchResultItem> finalResults = bestResults.values().stream()
                .sorted((a, b) -> Double.compare(b.getRelevanceScore(), a.getRelevanceScore()))
                .limit(maxResults)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
            
            double totalTime = Math.max(fastResults.getSearchTimeMs(), encryptedResults.getSearchTimeMs());
            return new AdvancedSearchResult(finalResults, SearchLevel.INCLUDE_DATA, totalTime);
            
        } catch (Exception e) {
            throw new RuntimeException("Parallel search execution failed", e);
        }
    }
    
    // ===== INDEX MANAGEMENT =====
    
    /**
     * Add block metadata to all relevant search indexes
     */
    public void indexBlock(String blockHash, BlockMetadataLayers metadata) {
        if (blockHash == null || metadata == null) {
            return;
        }
        
        logger.debug("🔍 SearchStrategyRouter.indexBlock called for {}...", blockHash.substring(0, 8));
        logger.debug("🔍 hasPrivateLayer={}, hasPublicLayer={}", metadata.hasPrivateLayer(), 
                    (metadata.getPublicLayer() != null));
        
        // Index in fast search if public metadata available
        if (metadata.getPublicLayer() != null && !metadata.getPublicLayer().isEmpty()) {
            logger.debug("🔍 Indexing in fast search");
            fastIndexSearch.indexBlock(blockHash, metadata);
        }
        
        // Index in encrypted search if private layer available
        if (metadata.hasPrivateLayer()) {
            String encryptedPrivateLayer = metadata.getEncryptedPrivateLayer();
            logger.debug("🔍 Indexing in encrypted search");
            logger.debug("🔍 encryptedPrivateLayer value: {}", (encryptedPrivateLayer != null ? "present (" + encryptedPrivateLayer.length() + " chars)" : "NULL"));
            if (encryptedPrivateLayer != null && !encryptedPrivateLayer.trim().isEmpty()) {
                logger.debug("🔍 encryptedPrivateLayer preview: {}...", encryptedPrivateLayer.substring(0, Math.min(50, encryptedPrivateLayer.length())));
            }
            encryptedContentSearch.indexEncryptedBlock(blockHash, encryptedPrivateLayer);
        } else {
            logger.debug("🔍 ℹ️ NO private layer - skipping encrypted search indexing");
        }
        
    }
    
    /**
     * Remove block from all search indexes
     */
    public void removeBlock(String blockHash) {
        fastIndexSearch.removeBlock(blockHash);
        encryptedContentSearch.removeBlock(blockHash);
    }
    
    /**
     * Get comprehensive search statistics
     */
    public SearchRouterStats getRouterStats() {
        return new SearchRouterStats(
            fastIndexSearch.getIndexStats(),
            encryptedContentSearch.getEncryptedIndexStats()
        );
    }
    
    /**
     * Get fast index search component
     */
    public FastIndexSearch getFastIndexSearch() {
        return fastIndexSearch;
    }
    
    /**
     * Get encrypted content search component
     */
    public EncryptedContentSearch getEncryptedContentSearch() {
        return encryptedContentSearch;
    }
    
    
    /**
     * Shutdown the router and clean up resources
     */
    public void shutdown() {
        executorService.shutdown();
        fastIndexSearch.clearAll();
        encryptedContentSearch.clearAll();
    }
    
    // ===== NESTED CLASSES AND ENUMS =====
    
    public enum SearchStrategy {
        FAST_PUBLIC("Fast public metadata search"),
        ENCRYPTED_CONTENT("Encrypted content search"),
        HYBRID_CASCADE("Hybrid cascade (fast -> encrypted)"),
        PARALLEL_MULTI("Parallel multi-strategy search");
        
        private final String description;
        
        SearchStrategy(String description) {
            this.description = description;
        }
        
        public String getDescription() { return description; }
    }
    
    public enum QueryComplexity {
        SIMPLE, MEDIUM, COMPLEX
    }
    
    public enum SearchResultSource {
        PUBLIC_METADATA, ENCRYPTED_CONTENT, OFF_CHAIN_CONTENT
    }
    
    /**
     * Query analysis result
     */
    public static class QueryAnalysis {
        private final SearchStrategy recommendedStrategy;
        private final QueryComplexity complexity;
        private final boolean hasPassword;
        private final SecurityLevel securityLevel;
        
        public QueryAnalysis(SearchStrategy recommendedStrategy, QueryComplexity complexity,
                           boolean hasPassword, SecurityLevel securityLevel) {
            this.recommendedStrategy = recommendedStrategy;
            this.complexity = complexity;
            this.hasPassword = hasPassword;
            this.securityLevel = securityLevel;
        }
        
        public SearchStrategy getRecommendedStrategy() { return recommendedStrategy; }
        public QueryComplexity getComplexity() { return complexity; }
        public boolean hasPassword() { return hasPassword; }
        public SecurityLevel getSecurityLevel() { return securityLevel; }
    }
    
    /**
     * Individual search result item
     */
    public static class SearchResultItem {
        private final String blockHash;
        private final double relevanceScore;
        private final SearchResultSource source;
        private final String summary;
        private final double searchTimeMs;
        
        public SearchResultItem(String blockHash, double relevanceScore, 
                              SearchResultSource source, String summary, double searchTimeMs) {
            this.blockHash = blockHash;
            this.relevanceScore = relevanceScore;
            this.source = source;
            this.summary = summary;
            this.searchTimeMs = searchTimeMs;
        }
        
        public String getBlockHash() { return blockHash; }
        public double getRelevanceScore() { return relevanceScore; }
        public SearchResultSource getSource() { return source; }
        public String getSummary() { return summary; }
        public double getSearchTimeMs() { return searchTimeMs; }
    }
    
    /**
     * Advanced search result container
     */
    public static class AdvancedSearchResult {
        private final List<SearchResultItem> results;
        private final SearchLevel searchLevel;
        private final double searchTimeMs;
        
        public AdvancedSearchResult(List<SearchResultItem> results, SearchLevel searchLevel, double searchTimeMs) {
            this.results = results != null ? results : new ArrayList<>();
            this.searchLevel = searchLevel;
            this.searchTimeMs = searchTimeMs;
        }
        
        public List<SearchResultItem> getResults() { return Collections.unmodifiableList(results); }
        public SearchLevel getSearchLevel() { return searchLevel; }
        public double getSearchTimeMs() { return searchTimeMs; }
    }
    
    /**
     * Complete search routing result
     */
    public static class SearchRoutingResult {
        private final AdvancedSearchResult result;
        private final SearchStrategy strategyUsed;
        private final QueryAnalysis analysis;
        private final double totalTimeMs;
        private final String errorMessage;
        
        public SearchRoutingResult(AdvancedSearchResult result, SearchStrategy strategyUsed,
                                 QueryAnalysis analysis, double totalTimeMs) {
            this(result, strategyUsed, analysis, totalTimeMs, null);
        }
        
        public SearchRoutingResult(AdvancedSearchResult result, SearchStrategy strategyUsed,
                                 QueryAnalysis analysis, double totalTimeMs, String errorMessage) {
            this.result = result;
            this.strategyUsed = strategyUsed;
            this.analysis = analysis;
            this.totalTimeMs = totalTimeMs;
            this.errorMessage = errorMessage;
        }
        
        public AdvancedSearchResult getResult() { return result; }
        public SearchStrategy getStrategyUsed() { return strategyUsed; }
        public QueryAnalysis getAnalysis() { return analysis; }
        public double getTotalTimeMs() { return totalTimeMs; }
        public String getErrorMessage() { return errorMessage; }
        public boolean hadError() { return errorMessage != null; }
    }
    
    /**
     * Search router statistics
     */
    public static class SearchRouterStats {
        private final FastIndexSearch.FastIndexStats fastStats;
        private final EncryptedContentSearch.EncryptedIndexStats encryptedStats;
        
        public SearchRouterStats(FastIndexSearch.FastIndexStats fastStats,
                               EncryptedContentSearch.EncryptedIndexStats encryptedStats) {
            this.fastStats = fastStats;
            this.encryptedStats = encryptedStats;
        }
        
        public FastIndexSearch.FastIndexStats getFastStats() { return fastStats; }
        public EncryptedContentSearch.EncryptedIndexStats getEncryptedStats() { return encryptedStats; }
    }
}