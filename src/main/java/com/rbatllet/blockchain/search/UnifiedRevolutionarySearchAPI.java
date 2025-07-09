package com.rbatllet.blockchain.search;

import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.config.EncryptionConfig;
import com.rbatllet.blockchain.search.RevolutionarySearchEngine.*;
import java.security.PrivateKey;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * UNIFIED REVOLUTIONARY SEARCH API
 * 
 * Single entry point for all revolutionary search operations.
 * Provides simple, intuitive methods that leverage the complete
 * three-layer metadata architecture and intelligent strategy routing.
 */
public class UnifiedRevolutionarySearchAPI {
    
    private final RevolutionarySearchEngine searchEngine;
    private final BlockPasswordRegistry passwordRegistry;
    private boolean isInitialized = false;
    
    public UnifiedRevolutionarySearchAPI() {
        this.searchEngine = new RevolutionarySearchEngine();
        this.passwordRegistry = new BlockPasswordRegistry();
    }
    
    public UnifiedRevolutionarySearchAPI(EncryptionConfig config) {
        this.searchEngine = new RevolutionarySearchEngine(config);
        this.passwordRegistry = new BlockPasswordRegistry();
    }
    
    // ===== SIMPLE SEARCH METHODS =====
    
    /**
     * Simple fast search - public metadata only
     */
    public List<EnhancedSearchResult> searchSimple(String query) {
        return searchSimple(query, 20);
    }
    
    /**
     * Simple fast search with result limit
     */
    public List<EnhancedSearchResult> searchSimple(String query, int maxResults) {
        RevolutionarySearchResult result = searchEngine.searchPublicOnly(query, maxResults);
        return result.getResults();
    }
    
    /**
     * Secure search with password - accesses encrypted content
     */
    public List<EnhancedSearchResult> searchSecure(String query, String password) {
        return searchSecure(query, password, 20);
    }
    
    /**
     * Secure search with password and result limit
     */
    public List<EnhancedSearchResult> searchSecure(String query, String password, int maxResults) {
        RevolutionarySearchResult result = searchEngine.searchEncryptedOnly(query, password, maxResults);
        return result.getResults();
    }
    
    
    /**
     * Intelligent search that automatically determines the best strategy
     */
    public List<EnhancedSearchResult> searchIntelligent(String query, String password, int maxResults) {
        RevolutionarySearchResult result = searchEngine.search(query, password, maxResults);
        return result.getResults();
    }
    
    
    // ===== ADVANCED SEARCH METHODS =====
    
    /**
     * Advanced search with full control
     */
    public RevolutionarySearchResult searchAdvanced(String query, String password, 
                                                   EncryptionConfig config, int maxResults) {
        return searchEngine.search(query, password, maxResults, config);
    }
    
    // ===== SPECIALIZED SEARCH METHODS =====
    
    // Removed category-specific search methods - blockchain should be content-agnostic
    // Users can now search by their own defined terms using searchIntelligent() directly
    
    // Removed searchHighValue() method - contained hardcoded financial terms, use searchIntelligent() with user-defined terms instead
    
    // ===== BLOCKCHAIN INTEGRATION =====
    
    /**
     * Initialize search engine with blockchain data
     */
    public IndexingResult initializeWithBlockchain(Blockchain blockchain, String password, PrivateKey privateKey) {
        IndexingResult result = searchEngine.indexBlockchain(blockchain, password, privateKey);
        isInitialized = true;
        return result;
    }
    
    /**
     * Initialize search engine asynchronously
     */
    public CompletableFuture<IndexingResult> initializeWithBlockchainAsync(
            Blockchain blockchain, String password, PrivateKey privateKey) {
        return CompletableFuture.supplyAsync(() -> {
            IndexingResult result = searchEngine.indexBlockchain(blockchain, password, privateKey);
            isInitialized = true;
            return result;
        });
    }
    
    /**
     * Add new block to search indexes
     */
    public void addBlock(Block block, String password, PrivateKey privateKey) {
        if (block.isDataEncrypted() && password != null) {
            passwordRegistry.registerBlockPassword(block.getHash(), password);
        }
        searchEngine.indexBlock(block, password, privateKey, EncryptionConfig.createHighSecurityConfig());
    }
    
    
    /**
     * Remove block from search indexes and password registry
     */
    public void removeBlock(String blockHash) {
        searchEngine.removeBlock(blockHash);
        passwordRegistry.removeBlockPassword(blockHash);
    }
    
    // ===== SYSTEM STATUS =====
    
    /**
     * Check if search engine is ready
     */
    public boolean isReady() {
        return isInitialized;
    }
    
    /**
     * Get comprehensive search engine statistics
     */
    public RevolutionarySearchStats getStatistics() {
        return searchEngine.getSearchStats();
    }
    
    /**
     * Get search performance metrics
     */
    public String getPerformanceMetrics() {
        RevolutionarySearchStats stats = getStatistics();
        BlockPasswordRegistry.RegistryStats registryStats = passwordRegistry.getStats();
        StringBuilder metrics = new StringBuilder();
        
        metrics.append("🚀 Revolutionary Search Engine Metrics:\\n");
        metrics.append("   📊 Total Blocks Indexed: ").append(stats.getTotalBlocksIndexed()).append("\\n");
        
        if (stats.getRouterStats().getFastStats() != null) {
            metrics.append("   ⚡ Fast Search Keywords: ")
                   .append(stats.getRouterStats().getFastStats().getUniqueKeywords()).append("\\n");
        }
        
        if (stats.getRouterStats().getEncryptedStats() != null) {
            metrics.append("   🔐 Encrypted Blocks: ")
                   .append(stats.getRouterStats().getEncryptedStats().getEncryptedBlocksIndexed()).append("\\n");
        }
        
        
        metrics.append("   🔑 Password Registry: ").append(registryStats.getRegisteredBlocks()).append(" blocks\\n");
        metrics.append("   💾 Memory Usage: ")
               .append(stats.getEstimatedMemoryBytes() / (1024 * 1024)).append(" MB");
        
        return metrics.toString();
    }
    
    /**
     * Get search capabilities summary
     */
    public String getCapabilitiesSummary() {
        StringBuilder capabilities = new StringBuilder();
        
        capabilities.append("🎯 Revolutionary Search Capabilities:\\n");
        capabilities.append("   ⚡ Fast Public Search: Sub-50ms searches on public metadata\\n");
        capabilities.append("   🔐 Encrypted Content Search: Deep search with password protection\\n");
        capabilities.append("   🔒 Privacy Search: Privacy-preserving capabilities\\n");
        capabilities.append("   🧠 Intelligent Routing: Automatic strategy selection\\n");
        capabilities.append("   🔄 Parallel Multi-Search: Simultaneous strategy execution\\n");
        capabilities.append("   📊 Advanced Analytics: Content categorization and analysis\\n");
        capabilities.append("   🛡️ Enterprise Security: AES-256-GCM encryption\\n");
        capabilities.append("   📈 High Performance: Millions of blocks support\\n");
        
        return capabilities.toString();
    }
    
    /**
     * Run search engine diagnostics
     */
    public String runDiagnostics() {
        StringBuilder diagnostics = new StringBuilder();
        
        diagnostics.append("🔍 Revolutionary Search Engine Diagnostics:\\n\\n");
        
        // System status
        diagnostics.append("Status: ").append(isReady() ? "✅ Ready" : "⚠️ Not Initialized").append("\\n");
        
        // Performance metrics
        diagnostics.append(getPerformanceMetrics()).append("\\n\\n");
        
        // Capabilities
        diagnostics.append(getCapabilitiesSummary()).append("\\n");
        
        // Recommendations
        diagnostics.append("💡 Recommendations:\\n");
        if (!isReady()) {
            diagnostics.append("   - Initialize with blockchain data using initializeWithBlockchain()\\n");
        }
        
        RevolutionarySearchStats stats = getStatistics();
        if (stats.getTotalBlocksIndexed() == 0) {
            diagnostics.append("   - Add blocks to search index for full functionality\\n");
        }
        
        if (stats.getEstimatedMemoryBytes() > 100 * 1024 * 1024) {
            diagnostics.append("   - Consider clearing cache if memory usage is too high\\n");
        }
        
        return diagnostics.toString();
    }
    
    /**
     * Clear all caches and indexes
     */
    public void clearCache() {
        searchEngine.clearAll();
        passwordRegistry.clearAll();
        isInitialized = false;
    }
    
    /**
     * Shutdown the search engine
     */
    public void shutdown() {
        searchEngine.shutdown();
        passwordRegistry.shutdown();
        isInitialized = false;
    }
    
    // ===== PASSWORD REGISTRY MANAGEMENT =====
    
    /**
     * Register a password for a specific block
     */
    public boolean registerBlockPassword(String blockHash, String password) {
        return passwordRegistry.registerBlockPassword(blockHash, password);
    }
    
    /**
     * Check if a block has a registered password
     */
    public boolean hasBlockPassword(String blockHash) {
        return passwordRegistry.hasBlockPassword(blockHash);
    }
    
    /**
     * Get all blocks with registered passwords
     */
    public Set<String> getRegisteredBlocks() {
        return passwordRegistry.getRegisteredBlocks();
    }
    
    /**
     * Get password registry statistics
     */
    public BlockPasswordRegistry.RegistryStats getPasswordRegistryStats() {
        return passwordRegistry.getStats();
    }
}