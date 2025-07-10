package com.rbatllet.blockchain.search;

import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.config.EncryptionConfig;
import com.rbatllet.blockchain.search.metadata.*;
import com.rbatllet.blockchain.search.strategy.*;

import java.util.*;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.security.PrivateKey;

/**
 * REVOLUTIONARY SEARCH ENGINE - Complete Implementation
 * 
 * The ultimate blockchain search system combining:
 * - Lightning-fast public metadata search (<50ms)
 * - Password-protected encrypted content search
 * - Advanced privacy-preserving search
 * - Intelligent strategy routing
 * - Three-layer metadata architecture
 */
public class RevolutionarySearchEngine {
    
    private final MetadataLayerManager metadataManager;
    private final SearchStrategyRouter strategyRouter;
    private final EncryptionConfig defaultConfig;
    private final ExecutorService indexingExecutor;
    private final Map<String, BlockMetadataLayers> blockMetadataIndex;
    private final OffChainFileSearch offChainFileSearch;
    private final OnChainContentSearch onChainContentSearch;
    
    // Reference to blockchain for block hash lookups (for EXHAUSTIVE_OFFCHAIN search)
    private com.rbatllet.blockchain.core.Blockchain blockchain;
    
    public RevolutionarySearchEngine() {
        this(EncryptionConfig.createHighSecurityConfig());
    }
    
    public RevolutionarySearchEngine(EncryptionConfig config) {
        this.metadataManager = new MetadataLayerManager();
        this.strategyRouter = new SearchStrategyRouter();
        this.defaultConfig = config;
        this.indexingExecutor = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "RevolutionarySearchIndexer-" + System.currentTimeMillis());
            t.setDaemon(true);
            return t;
        });
        this.blockMetadataIndex = new ConcurrentHashMap<>();
        this.offChainFileSearch = new OffChainFileSearch();
        this.onChainContentSearch = new OnChainContentSearch();
        this.blockchain = null; // Will be set when blockchain indexing is performed
    }
    
    // ===== CORE SEARCH METHODS =====
    
    /**
     * Perform revolutionary search with automatic strategy selection
     */
    public RevolutionarySearchResult search(String query, String password, int maxResults) {
        return search(query, password, maxResults, defaultConfig);
    }
    
    /**
     * Perform revolutionary search with custom configuration
     */
    public RevolutionarySearchResult search(String query, String password, int maxResults, EncryptionConfig config) {
        long startTime = System.nanoTime();
        
        try {
            // Route search to optimal strategy
            SearchStrategyRouter.SearchRoutingResult routingResult = 
                strategyRouter.routeSearch(query, password, config, maxResults);
            
            // Extract block information for results
            List<EnhancedSearchResult> enhancedResults = enhanceSearchResults(
                routingResult.getResult().getResults(), password);
            
            long endTime = System.nanoTime();
            double totalTimeMs = (endTime - startTime) / 1_000_000.0;
            
            return new RevolutionarySearchResult(
                enhancedResults,
                routingResult.getStrategyUsed(),
                routingResult.getAnalysis(),
                totalTimeMs,
                routingResult.getResult().getSearchLevel(),
                routingResult.hadError() ? routingResult.getErrorMessage() : null
            );
            
        } catch (Exception e) {
            long endTime = System.nanoTime();
            double totalTimeMs = (endTime - startTime) / 1_000_000.0;
            
            return new RevolutionarySearchResult(
                new ArrayList<>(),
                SearchStrategyRouter.SearchStrategy.FAST_PUBLIC,
                null,
                totalTimeMs,
                SearchLevel.FAST_ONLY,
                "Search failed: " + e.getMessage()
            );
        }
    }
    
    /**
     * Fast public search only (no password required)
     */
    public RevolutionarySearchResult searchPublicOnly(String query, int maxResults) {
        long startTime = System.nanoTime();
        
        try {
            // Execute fast public search directly
            List<FastIndexSearch.FastSearchResult> fastResults = 
                strategyRouter.getFastIndexSearch().searchFast(query, maxResults);
            
            List<EnhancedSearchResult> enhancedResults = new ArrayList<>();
            for (FastIndexSearch.FastSearchResult result : fastResults) {
                BlockMetadataLayers metadata = blockMetadataIndex.get(result.getBlockHash());
                enhancedResults.add(new EnhancedSearchResult(
                    result.getBlockHash(),
                    result.getRelevanceScore(),
                    SearchStrategyRouter.SearchResultSource.PUBLIC_METADATA,
                    result.getPublicMetadata() != null ? 
                        result.getPublicMetadata().getGeneralKeywords().toString() : "",
                    result.getSearchTimeMs(),
                    metadata != null ? metadata.getPublicLayer() : null,
                    null, // No private access
                    metadata != null ? metadata.getSecurityLevel() : null
                ));
            }
            
            long endTime = System.nanoTime();
            double totalTimeMs = (endTime - startTime) / 1_000_000.0;
            
            return new RevolutionarySearchResult(
                enhancedResults,
                SearchStrategyRouter.SearchStrategy.FAST_PUBLIC,
                null,
                totalTimeMs,
                SearchLevel.FAST_ONLY,
                null
            );
            
        } catch (Exception e) {
            long endTime = System.nanoTime();
            double totalTimeMs = (endTime - startTime) / 1_000_000.0;
            
            return new RevolutionarySearchResult(
                new ArrayList<>(),
                SearchStrategyRouter.SearchStrategy.FAST_PUBLIC,
                null,
                totalTimeMs,
                SearchLevel.FAST_ONLY,
                "Public search failed: " + e.getMessage()
            );
        }
    }
    
    /**
     * Deep encrypted search (requires password)
     */
    public RevolutionarySearchResult searchEncryptedOnly(String query, String password, int maxResults) {
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password required for encrypted search");
        }
        
        long startTime = System.nanoTime();
        
        try {
            // Execute encrypted content search directly
            List<EncryptedContentSearch.EncryptedSearchResult> encryptedResults = 
                strategyRouter.getEncryptedContentSearch().searchEncryptedContent(query, password, maxResults);
            
            List<EnhancedSearchResult> enhancedResults = new ArrayList<>();
            for (EncryptedContentSearch.EncryptedSearchResult result : encryptedResults) {
                BlockMetadataLayers metadata = blockMetadataIndex.get(result.getBlockHash());
                
                // Get private metadata if available
                PrivateMetadata privateMetadata = null;
                if (metadata != null && metadata.hasPrivateLayer()) {
                    try {
                        privateMetadata = metadataManager.decryptPrivateMetadata(
                            metadata.getEncryptedPrivateLayer(), password);
                    } catch (Exception e) {
                        // Ignore decryption failures
                    }
                }
                
                enhancedResults.add(new EnhancedSearchResult(
                    result.getBlockHash(),
                    result.getRelevanceScore(),
                    SearchStrategyRouter.SearchResultSource.ENCRYPTED_CONTENT,
                    result.getMatchingSummary(),
                    result.getSearchTimeMs(),
                    metadata != null ? metadata.getPublicLayer() : null,
                    privateMetadata,
                    metadata != null ? metadata.getSecurityLevel() : null
                ));
            }
            
            long endTime = System.nanoTime();
            double totalTimeMs = (endTime - startTime) / 1_000_000.0;
            
            return new RevolutionarySearchResult(
                enhancedResults,
                SearchStrategyRouter.SearchStrategy.ENCRYPTED_CONTENT,
                null,
                totalTimeMs,
                SearchLevel.INCLUDE_DATA,
                null
            );
            
        } catch (Exception e) {
            throw new RuntimeException("Encrypted search failed", e);
        }
    }
    
    /**
     * EXHAUSTIVE OFF-CHAIN SEARCH - NEW IMPLEMENTATION
     * 
     * Performs comprehensive search including encrypted off-chain files.
     * This is the most thorough search level that actually examines
     * the content of encrypted off-chain files.
     */
    public RevolutionarySearchResult searchExhaustiveOffChain(String query, String password, 
                                                             PrivateKey privateKey, int maxResults) {
        long startTime = System.nanoTime();
        
        try {
            System.out.println("🔍 Starting TRUE EXHAUSTIVE search (on-chain + off-chain) for: \"" + query + "\"");
            
            // Step 1: Perform regular encrypted search first
            RevolutionarySearchResult regularResults = searchEncryptedOnly(query, password, maxResults);
            List<EnhancedSearchResult> allResults = new ArrayList<>(regularResults.getResults());
            
            // Step 2: Get ALL blocks for exhaustive search (not just matches)
            List<Block> allBlocks = blockchain != null ? blockchain.getAllBlocks() : new ArrayList<>();
            
            // Step 3: Perform ON-CHAIN content search
            OnChainContentSearch.OnChainSearchResult onChainResults = onChainContentSearch.searchOnChainContent(
                allBlocks, query, password, privateKey, maxResults);
            
            // Step 4: Perform OFF-CHAIN file search (only for blocks with off-chain data)
            List<Block> blocksWithOffChainData = allBlocks.stream()
                .filter(b -> b.getOffChainData() != null)
                .collect(java.util.stream.Collectors.toList());
            
            OffChainSearchResult offChainResults = offChainFileSearch.searchOffChainContent(
                blocksWithOffChainData, query, password, privateKey, maxResults);
            
            // Step 5: Convert on-chain matches to enhanced search results
            List<EnhancedSearchResult> onChainEnhancedResults = convertOnChainToEnhancedResults(
                onChainResults, password);
            
            // Step 6: Convert off-chain matches to enhanced search results
            List<EnhancedSearchResult> offChainEnhancedResults = convertOffChainToEnhancedResults(
                offChainResults, password);
            
            // Step 7: Merge all results (regular + on-chain + off-chain)
            allResults.addAll(onChainEnhancedResults);
            allResults.addAll(offChainEnhancedResults);
            
            // Remove duplicates (same block might appear in multiple searches)
            Set<String> seenHashes = new HashSet<>();
            allResults = allResults.stream()
                .filter(r -> seenHashes.add(r.getBlockHash()))
                .collect(java.util.stream.Collectors.toList());
            
            // Sort by relevance (on-chain and off-chain matches get bonus scores)
            allResults.sort((a, b) -> Double.compare(b.getRelevanceScore(), a.getRelevanceScore()));
            
            // Limit final results
            if (allResults.size() > maxResults) {
                allResults = allResults.subList(0, maxResults);
            }
            
            long endTime = System.nanoTime();
            double totalTimeMs = (endTime - startTime) / 1_000_000.0;
            
            String summary = String.format(
                "TRUE EXHAUSTIVE search: %d total results (%d on-chain, %d off-chain) in %.2fms",
                allResults.size(), onChainEnhancedResults.size(), offChainEnhancedResults.size(), totalTimeMs);
            
            System.out.println("✅ " + summary);
            
            return new RevolutionarySearchResult(
                allResults,
                SearchStrategyRouter.SearchStrategy.EXHAUSTIVE_COMBINED,
                null, // QueryAnalysis not needed for this method
                totalTimeMs,
                SearchLevel.EXHAUSTIVE_OFFCHAIN,
                null // Success - no error message
            );
            
        } catch (Exception e) {
            long endTime = System.nanoTime();
            double totalTimeMs = (endTime - startTime) / 1_000_000.0;
            
            System.err.println("❌ EXHAUSTIVE_OFFCHAIN search failed: " + e.getMessage());
            
            return new RevolutionarySearchResult(
                new ArrayList<>(),
                SearchStrategyRouter.SearchStrategy.EXHAUSTIVE_COMBINED,
                null, // QueryAnalysis not needed for error case
                totalTimeMs,
                SearchLevel.EXHAUSTIVE_OFFCHAIN,
                "Off-chain search failed: " + e.getMessage()
            );
        }
    }
    
    /**
     * Convert on-chain matches to enhanced search results
     */
    private List<EnhancedSearchResult> convertOnChainToEnhancedResults(
            OnChainContentSearch.OnChainSearchResult onChainResult, String password) {
        
        List<EnhancedSearchResult> enhancedResults = new ArrayList<>();
        
        for (OnChainContentSearch.OnChainMatch match : onChainResult.getMatches()) {
            String blockHash = match.getBlockHash();
            BlockMetadataLayers metadata = blockMetadataIndex.get(blockHash);
            
            // Build context summary from snippets
            StringBuilder contextBuilder = new StringBuilder();
            contextBuilder.append("On-chain content match (")
                         .append(match.getContentType())
                         .append("): ");
            
            if (!match.getMatchingSnippets().isEmpty()) {
                contextBuilder.append(match.getMatchingSnippets().get(0));
            }
            
            // Create enhanced result with on-chain source
            EnhancedSearchResult enhancedResult = new EnhancedSearchResult(
                blockHash,
                match.getRelevanceScore() + 15.0, // On-chain content bonus
                SearchStrategyRouter.SearchResultSource.ENCRYPTED_CONTENT, // Uses encrypted content source
                contextBuilder.toString(),
                0.0, // Search time already accounted for
                metadata != null ? metadata.getPublicLayer() : null,
                null, // Private layer would need decryption
                metadata != null ? metadata.getSecurityLevel() : null
            );
            
            enhancedResults.add(enhancedResult);
        }
        
        return enhancedResults;
    }
    
    
    /**
     * Convert off-chain search results to enhanced search results
     */
    private List<EnhancedSearchResult> convertOffChainToEnhancedResults(OffChainSearchResult offChainResults, 
                                                                        String password) {
        List<EnhancedSearchResult> enhancedResults = new ArrayList<>();
        
        for (OffChainMatch match : offChainResults.getMatches()) {
            String blockHash = match.getBlockHash();
            BlockMetadataLayers metadata = blockMetadataIndex.get(blockHash);
            
            // Create enhanced result with off-chain content info
            StringBuilder contextBuilder = new StringBuilder();
            contextBuilder.append("📄 Off-chain file: ").append(match.getFileName()).append("\\n");
            contextBuilder.append("📋 Content type: ").append(match.getContentType()).append("\\n");
            contextBuilder.append("🎯 Matches found: ").append(match.getMatchCount()).append("\\n");
            
            if (match.getPreviewSnippet() != null) {
                contextBuilder.append("📖 Preview: ").append(match.getPreviewSnippet());
            }
            
            // Boost relevance score for off-chain matches
            double boostedRelevance = match.getRelevanceScore() + 20.0; // Off-chain bonus
            
            EnhancedSearchResult enhancedResult = new EnhancedSearchResult(
                blockHash,
                boostedRelevance,
                SearchStrategyRouter.SearchResultSource.OFF_CHAIN_CONTENT,
                contextBuilder.toString(),
                0.0, // Search time already accounted for
                metadata != null ? metadata.getPublicLayer() : null,
                null, // Private layer access not implemented yet
                metadata != null ? metadata.getSecurityLevel() : null,
                match // Now we can include the actual OffChainMatch object!
            );
            
            // ✅ Off-chain match information now properly stored in dedicated field
            
            enhancedResults.add(enhancedResult);
        }
        
        return enhancedResults;
    }
    
    /**
     * Get off-chain search statistics
     */
    public Map<String, Object> getOffChainSearchStats() {
        return offChainFileSearch.getCacheStats();
    }
    
    /**
     * Clear off-chain search cache
     */
    public void clearOffChainCache() {
        offChainFileSearch.clearCache();
    }
    
    // ===== BLOCKCHAIN INTEGRATION =====
    
    /**
     * Index filtered list of blocks (used for selective indexing)
     */
    public IndexingResult indexFilteredBlocks(List<Block> blocks, String password, PrivateKey privateKey) {
        long startTime = System.nanoTime();
        
        List<CompletableFuture<Void>> indexingTasks = new ArrayList<>();
        
        System.out.println("Starting filtered blockchain indexing for " + blocks.size() + " blocks...");
        
        for (Block block : blocks) {
            CompletableFuture<Void> task = CompletableFuture.runAsync(() -> {
                try {
                    // Extract user-defined search terms from the block
                    Set<String> userTerms = extractUserSearchTerms(block, password);
                    
                    if (!userTerms.isEmpty()) {
                        // Use user-defined terms for indexing
                        Set<String> publicTerms = new HashSet<>();
                        Set<String> privateTerms = new HashSet<>();
                        
                        // Simple distribution: user has full control over term placement
                        if (block.isDataEncrypted()) {
                            // For encrypted blocks, put all user terms in private layer for maximum privacy
                            // Users can explicitly use the UserFriendlyEncryptionAPI methods to control 
                            // public vs private term distribution if needed
                            privateTerms.addAll(userTerms);
                        } else {
                            // For unencrypted blocks, all terms go to public layer
                            publicTerms.addAll(userTerms);
                        }
                        
                        indexBlockWithUserTerms(block, password, privateKey, defaultConfig, 
                                              publicTerms.isEmpty() ? null : publicTerms,
                                              privateTerms.isEmpty() ? null : privateTerms);
                    } else {
                        // Fallback to traditional indexing
                        if (block.isDataEncrypted()) {
                            // For encrypted blocks, try with provided password (if any), fallback to public metadata only
                            if (password != null) {
                                indexBlockWithSpecificPassword(block, password, privateKey, defaultConfig);
                            } else {
                                // No password provided - index with public metadata only
                                indexBlock(block, null, privateKey, defaultConfig);
                            }
                        } else {
                            // For unencrypted blocks, use standard indexing
                            indexBlock(block, null, privateKey, defaultConfig);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Failed to index block " + block.getHash() + ": " + e.getMessage());
                }
            }, indexingExecutor);
            
            indexingTasks.add(task);
        }
        
        // Wait for all indexing tasks to complete
        CompletableFuture.allOf(indexingTasks.toArray(new CompletableFuture[0])).join();
        
        long endTime = System.nanoTime();
        double indexingTimeMs = (endTime - startTime) / 1_000_000.0;
        
        int successfullyIndexed = blockMetadataIndex.size();
        System.out.println("Filtered blockchain indexing completed: " + successfullyIndexed + "/" + blocks.size() + 
                          " blocks indexed in " + String.format("%.2f", indexingTimeMs) + "ms");
        
        return new IndexingResult(
            blocks.size(),
            successfullyIndexed,
            indexingTimeMs,
            strategyRouter.getRouterStats()
        );
    }

    /**
     * Index entire blockchain for revolutionary search
     * Enhanced with better error handling and progress tracking
     */
    public IndexingResult indexBlockchain(com.rbatllet.blockchain.core.Blockchain blockchain, String password, PrivateKey privateKey) {
        // Store blockchain reference for EXHAUSTIVE_OFFCHAIN searches
        this.blockchain = blockchain;
        return indexFilteredBlocks(blockchain.getAllBlocks(), password, privateKey);
    }
    
    /**
     * Index a single block for revolutionary search
     */
    public void indexBlock(Block block, String password, PrivateKey privateKey, EncryptionConfig config) {
        if (block == null || block.getHash() == null) {
            return;
        }
        
        try {
            // Generate complete metadata layers
            BlockMetadataLayers metadata = metadataManager.generateMetadataLayers(
                block, config, password, privateKey);
            
            // Store in local index
            blockMetadataIndex.put(block.getHash(), metadata);
            
            // Index in search strategy router
            strategyRouter.indexBlock(block.getHash(), metadata);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to index block " + block.getHash(), e);
        }
    }
    
    /**
     * Index a single block with user-defined search terms
     * @param block The block to index
     * @param password Password for encryption
     * @param privateKey Private key for signing
     * @param config Encryption configuration
     * @param publicSearchTerms Terms searchable without password
     * @param privateSearchTerms Terms requiring password for search
     */
    public void indexBlockWithUserTerms(Block block, String password, PrivateKey privateKey, 
                                       EncryptionConfig config, Set<String> publicSearchTerms, 
                                       Set<String> privateSearchTerms) {
        if (block == null || block.getHash() == null) {
            return;
        }
        
        try {
            // Generate metadata layers with user-defined terms
            BlockMetadataLayers metadata = metadataManager.generateMetadataLayers(
                block, config, password, privateKey, publicSearchTerms, privateSearchTerms);
            
            // Store in local index
            blockMetadataIndex.put(block.getHash(), metadata);
            
            // Index in search strategy router
            strategyRouter.indexBlock(block.getHash(), metadata);
            
            System.out.println("🔍 Debug: Indexed block with user-defined terms - Public: " + 
                (publicSearchTerms != null ? publicSearchTerms.size() : 0) + 
                ", Private: " + (privateSearchTerms != null ? privateSearchTerms.size() : 0));
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to index block with user terms " + block.getHash(), e);
        }
    }
    
    /**
     * Extract user-defined search terms from block's manual keywords
     * Parses encrypted manual keywords and extracts them as search terms
     * @param block The block to extract terms from
     * @param password Password to decrypt manual keywords
     * @return Set of extracted search terms
     */
    public Set<String> extractUserSearchTerms(Block block, String password) {
        Set<String> terms = new HashSet<>();
        
        if (block == null) {
            return terms;
        }
        
        try {
            // Extract from manual keywords
            if (block.getManualKeywords() != null && !block.getManualKeywords().trim().isEmpty()) {
                if (block.isDataEncrypted() && password != null) {
                    // Try to decrypt manual keywords
                    try {
                        String decryptedKeywords = com.rbatllet.blockchain.service.SecureBlockEncryptionService
                            .decryptFromString(block.getManualKeywords(), password);
                        if (decryptedKeywords != null && !decryptedKeywords.trim().isEmpty()) {
                            String[] keywordArray = decryptedKeywords.split("\\s+");
                            for (String keyword : keywordArray) {
                                if (keyword != null && !keyword.trim().isEmpty()) {
                                    terms.add(keyword.trim().toLowerCase());
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Warning: Could not decrypt manual keywords for block " + 
                            block.getHash() + ": " + e.getMessage());
                    }
                } else {
                    // Unencrypted keywords
                    String[] keywordArray = block.getManualKeywords().split("\\s+");
                    for (String keyword : keywordArray) {
                        if (keyword != null && !keyword.trim().isEmpty()) {
                            terms.add(keyword.trim().toLowerCase());
                        }
                    }
                }
            }
            
            // Extract from content category as a term
            if (block.getContentCategory() != null && !block.getContentCategory().trim().isEmpty()) {
                String category = block.getContentCategory().toLowerCase();
                if (!"user_defined".equals(category) && !"general".equals(category)) {
                    terms.add(category);
                }
            }
            
        } catch (Exception e) {
            System.err.println("Warning: Error extracting search terms from block " + 
                block.getHash() + ": " + e.getMessage());
        }
        
        return terms;
    }
    
    
    /**
     * Index a single block with specific password for that block
     * This allows different blocks to have different passwords
     * Enhanced with robust error handling and graceful degradation
     */
    public void indexBlockWithSpecificPassword(Block block, String blockSpecificPassword, PrivateKey privateKey, EncryptionConfig config) {
        if (block == null || block.getHash() == null) {
            System.err.println("Cannot index block: block or hash is null");
            return;
        }
        
        String blockHash = block.getHash();
        boolean indexedSuccessfully = false;
        
        // Strategy 1: Try indexing with specific password (if block is encrypted)
        if (block.isDataEncrypted() && blockSpecificPassword != null) {
            try {
                BlockMetadataLayers metadata = metadataManager.generateMetadataLayers(
                    block, config, blockSpecificPassword, privateKey);
                
                blockMetadataIndex.put(blockHash, metadata);
                strategyRouter.indexBlock(blockHash, metadata);
                
                indexedSuccessfully = true;
                System.out.println("Successfully indexed encrypted block " + blockHash.substring(0, 8) + "... with specific password");
                
            } catch (Exception e) {
                System.err.println("Failed to index block " + blockHash + " with specific password: " + e.getMessage());
                // Continue to fallback strategy
            }
        }
        
        // Strategy 2: Fallback to public metadata only (if not already indexed)
        if (!indexedSuccessfully) {
            try {
                BlockMetadataLayers publicMetadata = metadataManager.generateMetadataLayers(
                    block, config, null, privateKey);
                
                blockMetadataIndex.put(blockHash, publicMetadata);
                strategyRouter.indexBlock(blockHash, publicMetadata);
                
                indexedSuccessfully = true;
                System.out.println("Indexed block " + blockHash.substring(0, 8) + "... with public metadata only");
                
            } catch (Exception e2) {
                System.err.println("Failed to index block " + blockHash + " even with public metadata: " + e2.getMessage());
            }
        }
        
        // Strategy 3: Emergency fallback - minimal indexing
        if (!indexedSuccessfully) {
            try {
                // Create minimal metadata for at least basic indexing
                createMinimalBlockIndex(block, config);
                System.out.println("Created minimal index for block " + blockHash.substring(0, 8) + "... as emergency fallback");
                
            } catch (Exception e3) {
                System.err.println("Complete indexing failure for block " + blockHash + ": " + e3.getMessage());
                // This is a complete failure - block won't be searchable
            }
        }
    }
    
    /**
     * Remove block from all search indexes
     */
    public void removeBlock(String blockHash) {
        blockMetadataIndex.remove(blockHash);
        strategyRouter.removeBlock(blockHash);
    }
    
    // ===== METADATA ACCESS =====
    
    // Removed unused metadata access methods - metadata is now accessed through search results instead
    
    // ===== SYSTEM MANAGEMENT =====
    
    /**
     * Get comprehensive search engine statistics
     */
    public RevolutionarySearchStats getSearchStats() {
        return new RevolutionarySearchStats(
            blockMetadataIndex.size(),
            strategyRouter.getRouterStats(),
            calculateMemoryUsage()
        );
    }
    
    /**
     * Clear all search indexes and caches
     */
    public void clearAll() {
        blockMetadataIndex.clear();
        strategyRouter.shutdown();
    }
    
    /**
     * Shutdown the search engine and clean up resources
     */
    public void shutdown() {
        indexingExecutor.shutdown();
        strategyRouter.shutdown();
        blockMetadataIndex.clear();
    }
    
    // ===== HELPER METHODS =====
    
    /**
     * Enhance search results with additional block information
     */
    private List<EnhancedSearchResult> enhanceSearchResults(
            List<SearchStrategyRouter.SearchResultItem> results, String password) {
        
        List<EnhancedSearchResult> enhanced = new ArrayList<>();
        
        for (SearchStrategyRouter.SearchResultItem item : results) {
            BlockMetadataLayers metadata = blockMetadataIndex.get(item.getBlockHash());
            
            // Get private metadata if password provided
            PrivateMetadata privateMetadata = null;
            if (password != null && metadata != null && metadata.hasPrivateLayer()) {
                try {
                    privateMetadata = metadataManager.decryptPrivateMetadata(
                        metadata.getEncryptedPrivateLayer(), password);
                } catch (Exception e) {
                    // Ignore decryption failures
                }
            }
            
            enhanced.add(new EnhancedSearchResult(
                item.getBlockHash(),
                item.getRelevanceScore(),
                item.getSource(),
                item.getSummary(),
                item.getSearchTimeMs(),
                metadata != null ? metadata.getPublicLayer() : null,
                privateMetadata,
                metadata != null ? metadata.getSecurityLevel() : null
            ));
        }
        
        return enhanced;
    }
    
    /**
     * Create minimal block index for emergency fallback
     * Used when full metadata generation fails
     */
    private void createMinimalBlockIndex(Block block, EncryptionConfig config) throws Exception {
        // Create a very basic metadata layer with just essential information
        PublicMetadata minimalPublic = new PublicMetadata();
        
        // Set minimal metadata
        Set<String> minimalKeywords = new HashSet<>();
        minimalKeywords.add("block");
        minimalKeywords.add("indexed");
        minimalPublic.setGeneralKeywords(minimalKeywords);
        minimalPublic.setBlockCategory("GENERAL");
        minimalPublic.setContentType("unknown");
        minimalPublic.setSizeRange("medium");
        minimalPublic.setHashFingerprint(block.getHash());
        
        if (block.getTimestamp() != null) {
            minimalPublic.setTimeRange(block.getTimestamp().toLocalDate().toString());
        } else {
            minimalPublic.setTimeRange(java.time.LocalDate.now().toString());
        }
        
        BlockMetadataLayers minimalMetadata = new BlockMetadataLayers(
            minimalPublic,
            null // No encrypted private layer
        );
        
        blockMetadataIndex.put(block.getHash(), minimalMetadata);
        strategyRouter.indexBlock(block.getHash(), minimalMetadata);
    }
    
    /**
     * Calculate approximate memory usage
     */
    private long calculateMemoryUsage() {
        long size = 0;
        
        // Estimate metadata index size
        size += blockMetadataIndex.size() * 2048; // Approximate per block metadata
        
        // Add router memory usage
        SearchStrategyRouter.SearchRouterStats routerStats = strategyRouter.getRouterStats();
        if (routerStats.getFastStats() != null) {
            size += routerStats.getFastStats().getEstimatedMemoryBytes();
        }
        if (routerStats.getEncryptedStats() != null) {
            size += routerStats.getEncryptedStats().getEstimatedMemoryBytes();
        }
        
        return size;
    }
    
    // ===== RESULT CLASSES =====
    
    /**
     * Enhanced search result with complete metadata access
     */
    public static class EnhancedSearchResult {
        private final String blockHash;
        private final double relevanceScore;
        private final SearchStrategyRouter.SearchResultSource source;
        private final String summary;
        private final double searchTimeMs;
        private final PublicMetadata publicMetadata;
        private final PrivateMetadata privateMetadata;
        private final EncryptionConfig.SecurityLevel securityLevel;
        private final OffChainMatch offChainMatch; // Off-chain match information (if applicable)
        
        public EnhancedSearchResult(String blockHash, double relevanceScore,
                                  SearchStrategyRouter.SearchResultSource source, String summary,
                                  double searchTimeMs, PublicMetadata publicMetadata,
                                  PrivateMetadata privateMetadata,
                                  EncryptionConfig.SecurityLevel securityLevel) {
            this.blockHash = blockHash;
            this.relevanceScore = relevanceScore;
            this.source = source;
            this.summary = summary;
            this.searchTimeMs = searchTimeMs;
            this.publicMetadata = publicMetadata;
            this.privateMetadata = privateMetadata;
            this.securityLevel = securityLevel;
            this.offChainMatch = null; // No off-chain match for regular results
        }
        
        /**
         * Constructor with off-chain match information
         */
        public EnhancedSearchResult(String blockHash, double relevanceScore,
                                  SearchStrategyRouter.SearchResultSource source, String summary,
                                  double searchTimeMs, PublicMetadata publicMetadata,
                                  PrivateMetadata privateMetadata,
                                  EncryptionConfig.SecurityLevel securityLevel,
                                  OffChainMatch offChainMatch) {
            this.blockHash = blockHash;
            this.relevanceScore = relevanceScore;
            this.source = source;
            this.summary = summary;
            this.searchTimeMs = searchTimeMs;
            this.publicMetadata = publicMetadata;
            this.privateMetadata = privateMetadata;
            this.securityLevel = securityLevel;
            this.offChainMatch = offChainMatch;
        }
        
        // Getters
        public String getBlockHash() { return blockHash; }
        public double getRelevanceScore() { return relevanceScore; }
        public SearchStrategyRouter.SearchResultSource getSource() { return source; }
        public String getSummary() { return summary; }
        public double getSearchTimeMs() { return searchTimeMs; }
        public PublicMetadata getPublicMetadata() { return publicMetadata; }
        public PrivateMetadata getPrivateMetadata() { return privateMetadata; }
        public EncryptionConfig.SecurityLevel getSecurityLevel() { return securityLevel; }
        public OffChainMatch getOffChainMatch() { return offChainMatch; }
        
        public boolean hasPrivateAccess() { return privateMetadata != null; }
        public boolean hasOffChainMatch() { return offChainMatch != null; }
        public boolean isOffChainResult() { return source == SearchStrategyRouter.SearchResultSource.OFF_CHAIN_CONTENT; }
        
        @Override
        public String toString() {
            String offChainInfo = hasOffChainMatch() ? ", offChain=" + offChainMatch.getFileName() : "";
            return String.format("Enhanced{hash=%s, score=%.2f, source=%s, security=%s%s}", 
                               blockHash.substring(0, Math.min(8, blockHash.length())), 
                               relevanceScore, source, securityLevel, offChainInfo);
        }
    }
    
    /**
     * Complete revolutionary search result
     */
    public static class RevolutionarySearchResult {
        private final List<EnhancedSearchResult> results;
        private final SearchStrategyRouter.SearchStrategy strategyUsed;
        private final SearchStrategyRouter.QueryAnalysis analysis;
        private final double totalTimeMs;
        private final SearchLevel searchLevel;
        private final String errorMessage;
        
        public RevolutionarySearchResult(List<EnhancedSearchResult> results,
                                       SearchStrategyRouter.SearchStrategy strategyUsed,
                                       SearchStrategyRouter.QueryAnalysis analysis,
                                       double totalTimeMs, SearchLevel searchLevel,
                                       String errorMessage) {
            this.results = results != null ? results : new ArrayList<>();
            this.strategyUsed = strategyUsed;
            this.analysis = analysis;
            this.totalTimeMs = totalTimeMs;
            this.searchLevel = searchLevel;
            this.errorMessage = errorMessage;
        }
        
        // Getters
        public List<EnhancedSearchResult> getResults() { return results; }
        public SearchStrategyRouter.SearchStrategy getStrategyUsed() { return strategyUsed; }
        public SearchStrategyRouter.QueryAnalysis getAnalysis() { return analysis; }
        public double getTotalTimeMs() { return totalTimeMs; }
        public SearchLevel getSearchLevel() { return searchLevel; }
        public String getErrorMessage() { return errorMessage; }
        
        public boolean isSuccessful() { return errorMessage == null; }
        public int getResultCount() { return results.size(); }
        
        @Override
        public String toString() {
            return String.format("Revolutionary{results=%d, strategy=%s, time=%.2fms, level=%s}", 
                               results.size(), strategyUsed, totalTimeMs, searchLevel);
        }
    }
    
    /**
     * Blockchain indexing result
     */
    public static class IndexingResult {
        private final int blocksProcessed;
        private final int blocksIndexed;
        private final double indexingTimeMs;
        private final SearchStrategyRouter.SearchRouterStats routerStats;
        
        public IndexingResult(int blocksProcessed, int blocksIndexed, double indexingTimeMs,
                            SearchStrategyRouter.SearchRouterStats routerStats) {
            this.blocksProcessed = blocksProcessed;
            this.blocksIndexed = blocksIndexed;
            this.indexingTimeMs = indexingTimeMs;
            this.routerStats = routerStats;
        }
        
        public int getBlocksProcessed() { return blocksProcessed; }
        public int getBlocksIndexed() { return blocksIndexed; }
        public double getIndexingTimeMs() { return indexingTimeMs; }
        public SearchStrategyRouter.SearchRouterStats getRouterStats() { return routerStats; }
        
        public double getIndexingRate() {
            return indexingTimeMs > 0 ? (blocksProcessed * 1000.0) / indexingTimeMs : 0.0;
        }
        
        @Override
        public String toString() {
            return String.format("Indexing{processed=%d, indexed=%d, time=%.2fms, rate=%.1f blocks/s}", 
                               blocksProcessed, blocksIndexed, indexingTimeMs, getIndexingRate());
        }
    }
    
    /**
     * Complete search engine statistics
     */
    public static class RevolutionarySearchStats {
        private final int totalBlocksIndexed;
        private final SearchStrategyRouter.SearchRouterStats routerStats;
        private final long estimatedMemoryBytes;
        
        public RevolutionarySearchStats(int totalBlocksIndexed,
                                      SearchStrategyRouter.SearchRouterStats routerStats,
                                      long estimatedMemoryBytes) {
            this.totalBlocksIndexed = totalBlocksIndexed;
            this.routerStats = routerStats;
            this.estimatedMemoryBytes = estimatedMemoryBytes;
        }
        
        public int getTotalBlocksIndexed() { return totalBlocksIndexed; }
        public SearchStrategyRouter.SearchRouterStats getRouterStats() { return routerStats; }
        public long getEstimatedMemoryBytes() { return estimatedMemoryBytes; }
        
        @Override
        public String toString() {
            return String.format("Revolutionary{blocks=%d, memory=%dMB}", 
                               totalBlocksIndexed, estimatedMemoryBytes / (1024 * 1024));
        }
    }
}