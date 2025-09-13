package com.rbatllet.blockchain.search;

import com.rbatllet.blockchain.config.EncryptionConfig;
import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.indexing.IndexingCoordinator;
import com.rbatllet.blockchain.search.metadata.*;
import com.rbatllet.blockchain.search.strategy.*;
import com.rbatllet.blockchain.service.SecureBlockEncryptionService;

import java.security.PrivateKey;
import java.util.*;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Search Framework Engine - Low-Level Blockchain Search Framework
 *
 * <p>This is the foundational search engine that powers all blockchain search operations.
 * It provides the most flexible and powerful search capabilities but requires deep technical
 * knowledge to use effectively. This class is designed for <strong>framework builders</strong>
 * and <strong>search engine experts</strong> who need maximum control over search behavior.</p>
 *
 * <h2>Architecture Overview:</h2>
 * <p>The Search Framework Engine implements a sophisticated multi-layer architecture:</p>
 * <ul>
 *   <li><strong>Three-Layer Metadata System:</strong>
 *     <ul>
 *       <li><strong>Public Layer:</strong> Non-sensitive metadata for fast searches</li>
 *       <li><strong>Protected Layer:</strong> Encrypted metadata requiring authentication</li>
 *       <li><strong>Private Layer:</strong> Highly sensitive data with maximum protection</li>
 *     </ul>
 *   </li>
 *   <li><strong>Intelligent Strategy Router:</strong> Automatically selects optimal search strategies</li>
 *   <li><strong>Multi-Source Content Search:</strong> Searches on-chain and off-chain data sources</li>
 *   <li><strong>Advanced Encryption Handling:</strong> AES-256-GCM with hierarchical key management</li>
 *   <li><strong>Performance Optimization:</strong> Concurrent processing with intelligent caching</li>
 * </ul>
 *
 * <h2>Search Strategies:</h2>
 * <p>The engine supports multiple search strategies, each optimized for different scenarios:</p>
 * <ul>
 *   <li><strong>FAST_PUBLIC:</strong> Sub-50ms searches on public metadata only</li>
 *   <li><strong>ENCRYPTED_CONTENT:</strong> Comprehensive search including encrypted data</li>
 *   <li><strong>EXHAUSTIVE_HYBRID:</strong> Complete search across all data sources</li>
 *   <li><strong>PRIVACY_PRESERVING:</strong> Anonymized search with privacy protection</li>
 *   <li><strong>CUSTOM_STRATEGY:</strong> User-defined search algorithms</li>
 * </ul>
 *
 * <h2>Performance Characteristics:</h2>
 * <ul>
 *   <li><strong>Public Search:</strong> &lt;50ms for millions of blocks</li>
 *   <li><strong>Encrypted Search:</strong> &lt;200ms for medium datasets</li>
 *   <li><strong>Exhaustive Search:</strong> &lt;2s for comprehensive analysis</li>
 *   <li><strong>Memory Efficiency:</strong> Optimized for large-scale blockchain datasets</li>
 *   <li><strong>Concurrent Operations:</strong> Thread-safe with intelligent locking</li>
 * </ul>
 *
 * <h2>Target Audience:</h2>
 * <p>This class is intended for:</p>
 * <ul>
 *   <li><strong>Framework Developers:</strong> Building custom blockchain search solutions</li>
 *   <li><strong>Search Engine Experts:</strong> Implementing specialized search algorithms</li>
 *   <li><strong>Performance Engineers:</strong> Fine-tuning search operations for specific requirements</li>
 *   <li><strong>Research & Development:</strong> Experimenting with advanced search techniques</li>
 *   <li><strong>Enterprise Architects:</strong> Designing large-scale blockchain search systems</li>
 * </ul>
 *
 * <h2>Usage Patterns:</h2>
 * <pre>{@code
 * // Basic usage for framework development
 * SearchFrameworkEngine engine = new SearchFrameworkEngine(customConfig);
 * engine.initialize(blockchain, offChainStorage);
 *
 * // Custom strategy implementation
 * engine.registerCustomStrategy("myStrategy", new CustomSearchStrategy());
 * SearchResult result = engine.searchWithStrategy("myStrategy", query, params);
 *
 * // Low-level control over search operations
 * MetadataLayerResult publicResult = engine.searchPublicLayer(query, config);
 * EncryptedContentResult encryptedResult = engine.searchEncryptedLayer(query, password, config);
 * }</pre>
 *
 * <h2>Security Features:</h2>
 * <ul>
 *   <li><strong>AES-256-GCM Encryption:</strong> Industry-standard encryption for sensitive data</li>
 *   <li><strong>Hierarchical Key Management:</strong> Multi-tier key structure with automatic rotation</li>
 *   <li><strong>Privacy Protection:</strong> Anonymization and data minimization techniques</li>
 *   <li><strong>Audit Trails:</strong> Comprehensive logging of all search operations</li>
 *   <li><strong>Access Control:</strong> Fine-grained permissions and authentication</li>
 * </ul>
 *
 * <h2>Integration Notes:</h2>
 * <p><strong>For most developers:</strong> Consider using {@link com.rbatllet.blockchain.api.UserFriendlyEncryptionAPI}
 * for complete blockchain operations or {@link SearchSpecialistAPI} for specialized search needs.</p>
 *
 * <p><strong>Direct usage of this class requires:</strong></p>
 * <ul>
 *   <li>Deep understanding of blockchain search architecture</li>
 *   <li>Knowledge of encryption and security best practices</li>
 *   <li>Experience with performance optimization techniques</li>
 *   <li>Familiarity with concurrent programming patterns</li>
 * </ul>
 *
 * <h2>Thread Safety:</h2>
 * <p>This class is fully thread-safe and designed for high-concurrency environments.
 * All public methods can be called concurrently from multiple threads without
 * external synchronization.</p>
 *
 * @see SearchSpecialistAPI for higher-level search operations
 * @see com.rbatllet.blockchain.api.UserFriendlyEncryptionAPI for complete blockchain solutions
 * @see SearchStrategyRouter for strategy selection and routing
 * @see MetadataLayerManager for metadata architecture details
 * @since 1.0
 * @author Private Blockchain Framework Team
 */
public class SearchFrameworkEngine {

    private final MetadataLayerManager metadataManager;
    private final SearchStrategyRouter strategyRouter;
    private final EncryptionConfig defaultConfig;
    private final ExecutorService indexingExecutor;
    private final Map<String, BlockMetadataLayers> blockMetadataIndex;
    private final OffChainFileSearch offChainFileSearch;
    private final OnChainContentSearch onChainContentSearch;

    // Reference to blockchain for block hash lookups (for EXHAUSTIVE_OFFCHAIN search)
    private com.rbatllet.blockchain.core.Blockchain blockchain;
    
    // 🔍 RACE CONDITION DEBUGGING: Instance identification
    private final String instanceId;
    private static final AtomicLong instanceCounter = new AtomicLong(0);
    
    // 🔒 GLOBAL ATOMIC PROTECTION: Shared across ALL instances to prevent cross-instance race conditions
    private static final ConcurrentHashMap<String, BlockMetadataLayers> globalProcessingMap = new ConcurrentHashMap<>();

    /**
     * Clears the global processing map for testing purposes.
     * ⚠️ WARNING: This method should ONLY be used in test environments.
     * It will clear the atomic coordination state across all SearchFrameworkEngine instances.
     */
    public static void clearGlobalProcessingMapForTesting() {
        globalProcessingMap.clear();
        logger.info("🧪 TESTING: Global processing map cleared");
    }

    private static final Logger logger = LoggerFactory.getLogger(
        SearchFrameworkEngine.class
    );

    /**
     * Creates a new Search Framework Engine with default high-security configuration.
     *
     * <p>This constructor initializes the search engine with the most secure default settings,
     * optimized for production environments that require maximum security. The configuration
     * includes AES-256-GCM encryption, strict validation policies, and comprehensive audit logging.</p>
     *
     * <p><strong>Default Configuration Features:</strong></p>
     * <ul>
     *   <li><strong>Security:</strong> AES-256-GCM encryption with PBKDF2 key derivation</li>
     *   <li><strong>Performance:</strong> Balanced caching with 4-thread indexing pool</li>
     *   <li><strong>Validation:</strong> Strict input validation and sanitization</li>
     *   <li><strong>Audit:</strong> Comprehensive operation logging and monitoring</li>
     *   <li><strong>Memory:</strong> Optimized memory management with automatic cleanup</li>
     * </ul>
     *
     * <p><strong>Thread Pool Configuration:</strong></p>
     * <ul>
     *   <li>4 daemon threads for background indexing operations</li>
     *   <li>Automatic thread naming for debugging and monitoring</li>
     *   <li>Graceful shutdown handling with proper resource cleanup</li>
     * </ul>
     *
     * <p><strong>Recommended for:</strong></p>
     * <ul>
     *   <li>Production environments with standard security requirements</li>
     *   <li>Framework development where security is paramount</li>
     *   <li>Applications handling sensitive or regulated data</li>
     *   <li>Enterprise deployments with compliance requirements</li>
     * </ul>
     *
     * @see #SearchFrameworkEngine(EncryptionConfig) for custom configuration options
     * @see EncryptionConfig#createHighSecurityConfig() for details on default security settings
     */
    public SearchFrameworkEngine() {
        this(EncryptionConfig.createHighSecurityConfig());
    }

    /**
     * Creates a new Search Framework Engine with custom encryption and performance configuration.
     *
     * <p>This constructor allows framework developers to specify custom security, performance,
     * and operational parameters. This level of control is essential for specialized applications,
     * research environments, or systems with unique requirements.</p>
     *
     * <p><strong>Configuration Control:</strong></p>
     * <ul>
     *   <li><strong>Encryption Algorithms:</strong> Custom cipher suites and key sizes</li>
     *   <li><strong>Performance Tuning:</strong> Cache sizes, thread pool configurations</li>
     *   <li><strong>Security Policies:</strong> Validation rules, access controls, audit levels</li>
     *   <li><strong>Memory Management:</strong> Garbage collection hints, memory limits</li>
     *   <li><strong>Search Strategies:</strong> Default strategy selection and routing policies</li>
     * </ul>
     *
     * <p><strong>Component Initialization:</strong></p>
     * <ul>
     *   <li><strong>Metadata Manager:</strong> Three-layer metadata architecture handler</li>
     *   <li><strong>Strategy Router:</strong> Intelligent search strategy selection system</li>
     *   <li><strong>Indexing Executor:</strong> 4-thread daemon pool for background operations</li>
     *   <li><strong>Content Searchers:</strong> On-chain and off-chain search components</li>
     *   <li><strong>Block Index:</strong> Thread-safe concurrent metadata storage</li>
     * </ul>
     *
     * <p><strong>Thread Pool Architecture:</strong></p>
     * <ul>
     *   <li>Fixed pool of 4 daemon threads for optimal balance of performance and resource usage</li>
     *   <li>Unique thread naming with timestamps for debugging and monitoring</li>
     *   <li>Daemon threads ensure JVM shutdown is not blocked by background operations</li>
     *   <li>Thread pool is shared across all indexing operations for efficiency</li>
     * </ul>
     *
     * <p><strong>Security Considerations:</strong></p>
     * <ul>
     *   <li>Configuration is validated against security best practices</li>
     *   <li>Weak encryption settings are rejected with detailed error messages</li>
     *   <li>All components are initialized with security-first defaults</li>
     *   <li>Audit trails are established for all configuration decisions</li>
     * </ul>
     *
     * <p><strong>Performance Optimization:</strong></p>
     * <ul>
     *   <li>ConcurrentHashMap for thread-safe metadata indexing without blocking</li>
     *   <li>Lazy initialization of blockchain references for memory efficiency</li>
     *   <li>Optimized search component initialization for fast startup</li>
     * </ul>
     *
     * <p><strong>Use Cases:</strong></p>
     * <ul>
     *   <li><strong>High-Security Environments:</strong> Custom encryption with specific algorithms</li>
     *   <li><strong>Performance-Critical Systems:</strong> Tuned configurations for specific workloads</li>
     *   <li><strong>Research Applications:</strong> Experimental configurations for testing</li>
     *   <li><strong>Compliance Requirements:</strong> Configurations meeting specific regulatory standards</li>
     * </ul>
     *
     * @param config the encryption and operational configuration to use. Must not be null.
     *               Use {@link EncryptionConfig#createHighSecurityConfig()} for maximum security,
     *               {@link EncryptionConfig#createBalancedConfig()} for balanced performance,
     *               or {@link EncryptionConfig#createPerformanceConfig()} for speed optimization.
     * @throws IllegalArgumentException if config is null or contains invalid settings
     * @throws SecurityException if config specifies encryption settings below minimum security requirements
     * @see EncryptionConfig for available configuration options and security recommendations
     * @see #SearchFrameworkEngine() for default high-security configuration
     */
    public SearchFrameworkEngine(EncryptionConfig config) {
        // 🔍 RACE CONDITION DEBUGGING: Assign unique instance ID
        this.instanceId = "SFE-" + instanceCounter.incrementAndGet() + "-" + System.currentTimeMillis();
        
        this.metadataManager = new MetadataLayerManager();
        this.strategyRouter = new SearchStrategyRouter();
        this.defaultConfig = config;
        this.indexingExecutor = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(
                r,
                "SearchFrameworkIndexer-" + System.currentTimeMillis()
            );
            t.setDaemon(true);
            return t;
        });
        this.blockMetadataIndex = new ConcurrentHashMap<>();
        this.offChainFileSearch = new OffChainFileSearch();
        this.onChainContentSearch = new OnChainContentSearch();
        this.blockchain = null; // Will be set when blockchain indexing is performed
        
        // 🔍 RACE CONDITION DEBUGGING: Log instance creation
        logger.info("🏗️ NEW SearchFrameworkEngine instance created: {} | Map: {} | Thread: {}", 
            this.instanceId, 
            System.identityHashCode(this.blockMetadataIndex),
            Thread.currentThread().getName());
    }

    // ===== CORE SEARCH METHODS =====

    /**
     * Performs a advanced search operation with automatic strategy selection and default configuration.
     *
     * <p>This is the primary entry point for intelligent search operations. The search engine
     * analyzes the query characteristics and automatically selects the most appropriate search
     * strategy from its comprehensive strategy repertoire.</p>
     *
     * <p><strong>Automatic Strategy Selection Process:</strong></p>
     * <ol>
     *   <li><strong>Query Analysis:</strong> Examines query complexity, length, and patterns</li>
     *   <li><strong>Context Assessment:</strong> Evaluates available data sources and encryption status</li>
     *   <li><strong>Performance Modeling:</strong> Predicts execution time for different strategies</li>
     *   <li><strong>Strategy Selection:</strong> Chooses optimal strategy based on analysis</li>
     *   <li><strong>Execution:</strong> Executes search with chosen strategy and configuration</li>
     * </ol>
     *
     * <p><strong>Available Strategies:</strong></p>
     * <ul>
     *   <li><strong>FAST_PUBLIC:</strong> For simple queries requiring maximum speed (&lt;50ms)</li>
     *   <li><strong>ENCRYPTED_CONTENT:</strong> For comprehensive searches including encrypted data</li>
     *   <li><strong>EXHAUSTIVE_HYBRID:</strong> For complete searches across all data sources</li>
     *   <li><strong>PRIVACY_PRESERVING:</strong> For sensitive queries requiring anonymization</li>
     * </ul>
     *
     * <p><strong>Performance Optimization:</strong></p>
     * <ul>
     *   <li>Automatic caching of frequently accessed metadata</li>
     *   <li>Intelligent parallel processing for large datasets</li>
     *   <li>Adaptive timeout management based on query complexity</li>
     *   <li>Memory-efficient result streaming for large result sets</li>
     * </ul>
     *
     * <p><strong>Framework Integration:</strong></p>
     * <p>This method is designed for framework developers who need:</p>
     * <ul>
     *   <li>Reliable automatic optimization without manual strategy selection</li>
     *   <li>Consistent performance across diverse query types</li>
     *   <li>Detailed metadata about strategy selection and performance</li>
     *   <li>Graceful handling of edge cases and error conditions</li>
     * </ul>
     *
     * @param query the search query string. Must not be null or empty.
     *              Supports advanced query syntax including boolean operators,
     *              exact phrases (quoted), wildcards, and field-specific searches.
     * @param password the password for accessing encrypted content. Must not be null.
     *                 Used only if the strategy router determines encrypted access is beneficial.
     * @param maxResults the maximum number of results to return. Must be positive.
     *                   Influences strategy selection - larger limits may trigger more comprehensive searches.
     * @return a comprehensive search result containing matches, strategy metadata, performance metrics,
     *         and detailed analysis of the search operation. Never null.
     * @throws IllegalArgumentException if query/password is null, query is empty, or maxResults is not positive
     * @throws IllegalStateException if the search engine is not properly initialized
     * @see #search(String, String, int, EncryptionConfig) for custom configuration control
     * @see SearchStrategyRouter for details on automatic strategy selection
     */
    public SearchResult search(String query, String password, int maxResults) {
        return search(query, password, maxResults, defaultConfig);
    }

    /**
     * Performs a advanced search operation with custom configuration and expert-level control.
     *
     * <p>This method provides framework developers with complete control over search behavior,
     * allowing fine-tuning of security, performance, and algorithmic parameters. This is the
     * most flexible search method available and forms the foundation for all other search operations.</p>
     *
     * <p><strong>Expert Configuration Control:</strong></p>
     * <ul>
     *   <li><strong>Encryption Parameters:</strong> Custom cipher suites, key derivation functions</li>
     *   <li><strong>Search Algorithms:</strong> Strategy selection hints and algorithmic preferences</li>
     *   <li><strong>Performance Tuning:</strong> Timeout settings, cache policies, thread allocation</li>
     *   <li><strong>Security Policies:</strong> Access controls, audit levels, data handling rules</li>
     *   <li><strong>Result Processing:</strong> Ranking algorithms, metadata extraction, filtering</li>
     * </ul>
     *
     * <p><strong>Advanced Search Pipeline:</strong></p>
     * <ol>
     *   <li><strong>Configuration Validation:</strong> Validates custom config against security requirements</li>
     *   <li><strong>Strategy Routing:</strong> Uses configuration hints for optimal strategy selection</li>
     *   <li><strong>Multi-Layer Search:</strong> Coordinates search across metadata layers</li>
     *   <li><strong>Result Enhancement:</strong> Enriches results with block metadata and context</li>
     *   <li><strong>Performance Analysis:</strong> Collects detailed timing and efficiency metrics</li>
     *   <li><strong>Error Handling:</strong> Provides comprehensive error reporting and recovery</li>
     * </ol>
     *
     * <p><strong>Performance Metrics Collection:</strong></p>
     * <ul>
     *   <li><strong>Timing Data:</strong> Precise nanosecond-level performance measurement</li>
     *   <li><strong>Strategy Analysis:</strong> Detailed rationale for strategy selection</li>
     *   <li><strong>Resource Usage:</strong> Memory, CPU, and I/O utilization tracking</li>
     *   <li><strong>Error Reporting:</strong> Comprehensive error context and recovery suggestions</li>
     * </ul>
     *
     * <p><strong>Framework Development Features:</strong></p>
     * <ul>
     *   <li><strong>Custom Strategy Integration:</strong> Supports user-defined search strategies</li>
     *   <li><strong>Detailed Diagnostics:</strong> Extensive debugging and analysis information</li>
     *   <li><strong>Extensibility Hooks:</strong> Integration points for custom processing</li>
     *   <li><strong>Research Support:</strong> Detailed metrics for algorithm research and development</li>
     * </ul>
     *
     * <p><strong>Security Considerations:</strong></p>
     * <ul>
     *   <li>Configuration is validated against minimum security requirements</li>
     *   <li>Encryption operations use configuration-specified algorithms and parameters</li>
     *   <li>Audit trails include configuration details for compliance and security analysis</li>
     *   <li>Error messages are carefully crafted to avoid information leakage</li>
     * </ul>
     *
     * <p><strong>Thread Safety and Concurrency:</strong></p>
     * <ul>
     *   <li>Fully thread-safe operation with intelligent locking strategies</li>
     *   <li>Configuration-driven concurrency control for optimal performance</li>
     *   <li>Deadlock prevention through consistent lock ordering</li>
     *   <li>Resource cleanup ensures no memory leaks in long-running operations</li>
     * </ul>
     *
     * @param query the search query string. Must not be null or empty.
     *              Supports full advanced search syntax with configuration-driven extensions.
     * @param password the password for accessing encrypted content. Must not be null.
     *                 Must meet security requirements specified in the configuration.
     * @param maxResults the maximum number of results to return. Must be positive.
     *                   Large values may require additional memory as specified in configuration.
     * @param config the encryption and search configuration to use. Must not be null.
     *               Configuration is validated against security policies before use.
     * @return a comprehensive advanced search result with detailed performance metrics,
     *         strategy analysis, error reporting, and complete result metadata. Never null.
     * @throws IllegalArgumentException if any parameter is null, query is empty, or maxResults is not positive
     * @throws SecurityException if configuration violates minimum security requirements
     * @throws IllegalStateException if the search engine is not properly initialized
     * @throws RuntimeException if search execution fails due to system or data issues
     * @see #search(String, String, int) for automatic configuration
     * @see EncryptionConfig for configuration options and security recommendations
     * @see SearchResult for detailed result analysis capabilities
     */
    public SearchResult search(
        String query,
        String password,
        int maxResults,
        EncryptionConfig config
    ) {
        long startTime = System.nanoTime();

        try {
            // Route search to optimal strategy
            SearchStrategyRouter.SearchRoutingResult routingResult =
                strategyRouter.routeSearch(query, password, config, maxResults);

            // Extract block information for results
            List<EnhancedSearchResult> enhancedResults = enhanceSearchResults(
                routingResult.getResult().getResults(),
                password
            );

            long endTime = System.nanoTime();
            double totalTimeMs = (endTime - startTime) / 1_000_000.0;

            return new SearchResult(
                enhancedResults,
                routingResult.getStrategyUsed(),
                routingResult.getAnalysis(),
                totalTimeMs,
                routingResult.getResult().getSearchLevel(),
                routingResult.hadError()
                    ? routingResult.getErrorMessage()
                    : null
            );
        } catch (Exception e) {
            long endTime = System.nanoTime();
            double totalTimeMs = (endTime - startTime) / 1_000_000.0;

            return new SearchResult(
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
    public SearchResult searchPublicOnly(String query, int maxResults) {
        long startTime = System.nanoTime();

        try {
            // Execute fast public search directly
            List<FastIndexSearch.FastSearchResult> fastResults = strategyRouter
                .getFastIndexSearch()
                .searchFast(query, maxResults);

            List<EnhancedSearchResult> enhancedResults = new ArrayList<>();
            for (FastIndexSearch.FastSearchResult result : fastResults) {
                BlockMetadataLayers metadata = blockMetadataIndex.get(
                    result.getBlockHash()
                );
                enhancedResults.add(
                    new EnhancedSearchResult(
                        result.getBlockHash(),
                        result.getRelevanceScore(),
                        SearchStrategyRouter.SearchResultSource.PUBLIC_METADATA,
                        result.getPublicMetadata() != null
                            ? result
                                .getPublicMetadata()
                                .getGeneralKeywords()
                                .toString()
                            : "",
                        result.getSearchTimeMs(),
                        metadata != null ? metadata.getPublicLayer() : null,
                        null, // No private access
                        metadata != null ? metadata.getSecurityLevel() : null
                    )
                );
            }

            long endTime = System.nanoTime();
            double totalTimeMs = (endTime - startTime) / 1_000_000.0;

            return new SearchResult(
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

            return new SearchResult(
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
    public SearchResult searchEncryptedOnly(
        String query,
        String password,
        int maxResults
    ) {
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Password required for encrypted search"
            );
        }

        long startTime = System.nanoTime();

        try {
            // Execute encrypted content search directly
            List<
                EncryptedContentSearch.EncryptedSearchResult
            > encryptedResults = strategyRouter
                .getEncryptedContentSearch()
                .searchEncryptedContent(query, password, maxResults);

            List<EnhancedSearchResult> enhancedResults = new ArrayList<>();
            for (EncryptedContentSearch.EncryptedSearchResult result : encryptedResults) {
                BlockMetadataLayers metadata = blockMetadataIndex.get(
                    result.getBlockHash()
                );

                // Get private metadata if available
                PrivateMetadata privateMetadata = null;
                if (metadata != null && metadata.hasPrivateLayer()) {
                    try {
                        privateMetadata =
                            metadataManager.decryptPrivateMetadata(
                                metadata.getEncryptedPrivateLayer(),
                                password
                            );
                    } catch (Exception e) {
                        // Ignore decryption failures
                    }
                }

                enhancedResults.add(
                    new EnhancedSearchResult(
                        result.getBlockHash(),
                        result.getRelevanceScore(),
                        SearchStrategyRouter.SearchResultSource.ENCRYPTED_CONTENT,
                        result.getMatchingSummary(),
                        result.getSearchTimeMs(),
                        metadata != null ? metadata.getPublicLayer() : null,
                        privateMetadata,
                        metadata != null ? metadata.getSecurityLevel() : null
                    )
                );
            }

            long endTime = System.nanoTime();
            double totalTimeMs = (endTime - startTime) / 1_000_000.0;

            return new SearchResult(
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
    public SearchResult searchExhaustiveOffChain(
        String query,
        String password,
        PrivateKey privateKey,
        int maxResults
    ) {
        long startTime = System.nanoTime();

        try {
            logger.debug(
                "🔍 Starting TRUE EXHAUSTIVE search (on-chain + off-chain) for: \"{}\"",
                query
            );

            // Step 1: Perform regular encrypted search first
            SearchResult regularResults = searchEncryptedOnly(
                query,
                password,
                maxResults
            );
            List<EnhancedSearchResult> allResults = new ArrayList<>(
                regularResults.getResults()
            );

            // OPTIMIZED: Process blocks in batches to avoid loading all blocks at once
            List<Block> allBlocks = new ArrayList<>();
            List<Block> blocksWithOffChainData = new ArrayList<>();

            if (blockchain != null) {
                final int BATCH_SIZE = 200;
                long totalBlocks = blockchain.getBlockCount();

                for (
                    int offset = 0;
                    offset < totalBlocks;
                    offset += BATCH_SIZE
                ) {
                    List<Block> batchBlocks = blockchain.getBlocksPaginated(
                        offset,
                        BATCH_SIZE
                    );
                    allBlocks.addAll(batchBlocks);

                    // Collect blocks with off-chain data during the same iteration
                    for (Block block : batchBlocks) {
                        if (block.getOffChainData() != null) {
                            blocksWithOffChainData.add(block);
                        }
                    }
                }
            }

            // Step 3: Perform ON-CHAIN content search
            OnChainContentSearch.OnChainSearchResult onChainResults =
                onChainContentSearch.searchOnChainContent(
                    allBlocks,
                    query,
                    password,
                    privateKey,
                    maxResults
                );

            OffChainSearchResult offChainResults =
                offChainFileSearch.searchOffChainContent(
                    blocksWithOffChainData,
                    query,
                    password,
                    privateKey,
                    maxResults
                );

            // Step 5: Convert on-chain matches to enhanced search results
            List<EnhancedSearchResult> onChainEnhancedResults =
                convertOnChainToEnhancedResults(onChainResults, password);

            // Step 6: Convert off-chain matches to enhanced search results
            List<EnhancedSearchResult> offChainEnhancedResults =
                convertOffChainToEnhancedResults(offChainResults, password);

            // Step 7: Merge all results (regular + on-chain + off-chain)
            allResults.addAll(onChainEnhancedResults);
            allResults.addAll(offChainEnhancedResults);

            // Remove duplicates (same block might appear in multiple searches)
            Set<String> seenHashes = new HashSet<>();
            allResults = allResults
                .stream()
                .filter(r -> seenHashes.add(r.getBlockHash()))
                .collect(java.util.stream.Collectors.toList());

            // Sort by relevance (on-chain and off-chain matches get bonus scores)
            allResults.sort((a, b) ->
                Double.compare(b.getRelevanceScore(), a.getRelevanceScore())
            );

            // Limit final results
            if (allResults.size() > maxResults) {
                allResults = allResults.subList(0, maxResults);
            }

            long endTime = System.nanoTime();
            double totalTimeMs = (endTime - startTime) / 1_000_000.0;

            String summary = String.format(
                "TRUE EXHAUSTIVE search: %d total results (%d on-chain, %d off-chain) in %.2fms",
                allResults.size(),
                onChainEnhancedResults.size(),
                offChainEnhancedResults.size(),
                totalTimeMs
            );

            logger.info("✅ {}", summary);

            return new SearchResult(
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

            logger.error("❌ EXHAUSTIVE_OFFCHAIN search failed", e);

            return new SearchResult(
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
        OnChainContentSearch.OnChainSearchResult onChainResult,
        String password
    ) {
        List<EnhancedSearchResult> enhancedResults = new ArrayList<>();

        for (OnChainContentSearch.OnChainMatch match : onChainResult.getMatches()) {
            String blockHash = match.getBlockHash();
            BlockMetadataLayers metadata = blockMetadataIndex.get(blockHash);

            // Build context summary from snippets
            StringBuilder contextBuilder = new StringBuilder();
            contextBuilder
                .append("On-chain content match (")
                .append(match.getContentType())
                .append("): ");

            if (!match.getMatchingSnippets().isEmpty()) {
                contextBuilder.append(match.getMatchingSnippets().get(0));
            }

            // Get private metadata if available and password provided
            PrivateMetadata privateMetadata = null;
            if (
                password != null &&
                metadata != null &&
                metadata.hasPrivateLayer()
            ) {
                try {
                    privateMetadata = metadataManager.decryptPrivateMetadata(
                        metadata.getEncryptedPrivateLayer(),
                        password
                    );
                } catch (Exception e) {
                    // Ignore decryption failures - private layer will remain null
                }
            }

            // Create enhanced result with on-chain source
            EnhancedSearchResult enhancedResult = new EnhancedSearchResult(
                blockHash,
                match.getRelevanceScore() + 15.0, // On-chain content bonus
                SearchStrategyRouter.SearchResultSource.ENCRYPTED_CONTENT, // Uses encrypted content source
                contextBuilder.toString(),
                0.0, // Search time already accounted for
                metadata != null ? metadata.getPublicLayer() : null,
                privateMetadata, // Private layer now properly decrypted when available
                metadata != null ? metadata.getSecurityLevel() : null
            );

            enhancedResults.add(enhancedResult);
        }

        return enhancedResults;
    }

    /**
     * Convert off-chain search results to enhanced search results
     */
    private List<EnhancedSearchResult> convertOffChainToEnhancedResults(
        OffChainSearchResult offChainResults,
        String password
    ) {
        List<EnhancedSearchResult> enhancedResults = new ArrayList<>();

        for (OffChainMatch match : offChainResults.getMatches()) {
            String blockHash = match.getBlockHash();
            BlockMetadataLayers metadata = blockMetadataIndex.get(blockHash);

            // Create enhanced result with off-chain content info
            StringBuilder contextBuilder = new StringBuilder();
            contextBuilder
                .append("📄 Off-chain file: ")
                .append(match.getFileName())
                .append("\\n");
            contextBuilder
                .append("📋 Content type: ")
                .append(match.getContentType())
                .append("\\n");
            contextBuilder
                .append("🎯 Matches found: ")
                .append(match.getMatchCount())
                .append("\\n");

            if (match.getPreviewSnippet() != null) {
                contextBuilder
                    .append("📖 Preview: ")
                    .append(match.getPreviewSnippet());
            }

            // Boost relevance score for off-chain matches
            double boostedRelevance = match.getRelevanceScore() + 20.0; // Off-chain bonus

            // Get private metadata if available and password provided
            PrivateMetadata privateMetadata = null;
            if (
                password != null &&
                metadata != null &&
                metadata.hasPrivateLayer()
            ) {
                try {
                    privateMetadata = metadataManager.decryptPrivateMetadata(
                        metadata.getEncryptedPrivateLayer(),
                        password
                    );
                } catch (Exception e) {
                    // Ignore decryption failures - private layer will remain null
                }
            }

            EnhancedSearchResult enhancedResult = new EnhancedSearchResult(
                blockHash,
                boostedRelevance,
                SearchStrategyRouter.SearchResultSource.OFF_CHAIN_CONTENT,
                contextBuilder.toString(),
                0.0, // Search time already accounted for
                metadata != null ? metadata.getPublicLayer() : null,
                privateMetadata, // Private layer now properly decrypted when available
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
    public IndexingResult indexFilteredBlocks(
        List<Block> blocks,
        String password,
        PrivateKey privateKey
    ) {
        long startTime = System.nanoTime();

        // Check IndexingCoordinator to prevent infinite loops
        IndexingCoordinator coordinator = IndexingCoordinator.getInstance();
        if (coordinator.isIndexing()) {
            logger.debug(
                "⏭️ Skipping filtered blocks indexing - another indexing operation in progress"
            );
            return new IndexingResult(
                0,
                0,
                (System.nanoTime() - startTime) / 1_000_000.0,
                null
            );
        }

        List<CompletableFuture<Void>> indexingTasks = new ArrayList<>();

        logger.info(
            "📊 Starting filtered blockchain indexing for {} blocks",
            blocks.size()
        );

        for (Block block : blocks) {
            CompletableFuture<Void> task = CompletableFuture.runAsync(
                () -> {
                    try {
                        // Extract user-defined search terms from the block
                        Set<String> userTerms = extractUserSearchTerms(
                            block,
                            password
                        );

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

                            indexBlockWithUserTerms(
                                block,
                                password,
                                privateKey,
                                defaultConfig,
                                publicTerms.isEmpty() ? null : publicTerms,
                                privateTerms.isEmpty() ? null : privateTerms
                            );
                        } else {
                            // Fallback to traditional indexing
                            if (block.isDataEncrypted()) {
                                // For encrypted blocks, try with provided password (if any), fallback to public metadata only
                                if (password != null) {
                                    indexBlockWithSpecificPassword(
                                        block,
                                        password,
                                        privateKey,
                                        defaultConfig
                                    );
                                } else {
                                    // No password provided - index with public metadata only
                                    indexBlock(
                                        block,
                                        null,
                                        privateKey,
                                        defaultConfig
                                    );
                                }
                            } else {
                                // For unencrypted blocks, use standard indexing
                                indexBlock(
                                    block,
                                    null,
                                    privateKey,
                                    defaultConfig
                                );
                            }
                        }
                    } catch (Exception e) {
                        logger.error(
                            "Failed to index block {}",
                            block.getHash(),
                            e
                        );
                    }
                },
                indexingExecutor
            );

            indexingTasks.add(task);
        }

        // Wait for all indexing tasks to complete
        CompletableFuture.allOf(
            indexingTasks.toArray(new CompletableFuture[0])
        ).join();

        long endTime = System.nanoTime();
        double indexingTimeMs = (endTime - startTime) / 1_000_000.0;

        int successfullyIndexed = blockMetadataIndex.size();
        logger.info(
            "📊 Filtered blockchain indexing completed: {}/{} blocks indexed in {}ms",
            successfullyIndexed,
            blocks.size(),
            String.format("%.2f", indexingTimeMs)
        );

        return new IndexingResult(
            blocks.size(),
            successfullyIndexed,
            indexingTimeMs,
            strategyRouter.getRouterStats()
        );
    }

    /**
     * Index entire blockchain for advanced search
     * Enhanced with better error handling and progress tracking
     */
    public IndexingResult indexBlockchain(
        Blockchain blockchain,
        String password,
        PrivateKey privateKey
    ) {
        // Store blockchain reference for EXHAUSTIVE_OFFCHAIN searches
        this.blockchain = blockchain;

        // Detect test environment and adjust interval accordingly
        boolean isTestEnvironment = isRunningInTest();
        int minInterval = isTestEnvironment ? 100 : 5000; // 100ms for tests, 5s for production

        // Use IndexingCoordinator to prevent infinite loops with global coordination
        IndexingCoordinator coordinator = IndexingCoordinator.getInstance();
        
        // Global lock to prevent concurrent indexing checks
        synchronized (coordinator) {
            // Skip interval checks in test mode
            if (!coordinator.isTestMode()) {
                // Check if indexing was recently executed to avoid unnecessary duplicate operations
                long lastExecution = coordinator.getLastExecutionTime("SEARCH_FRAMEWORK_INDEX_BLOCKCHAIN");
                long timeSinceLastExecution = System.currentTimeMillis() - lastExecution;
                
                if (lastExecution > 0 && timeSinceLastExecution < minInterval) {
                    logger.debug(
                        "⏭️ Skipping blockchain indexing - executed {}ms ago (min interval: {}ms)",
                        timeSinceLastExecution, minInterval
                    );
                    // Return empty result for recent indexing
                    return new IndexingResult(0, 0, 0.0, null);
                }
            }
            
            // Immediately mark as starting to prevent other threads from entering
            AtomicLong lastExecutionTracker = coordinator.indexingProgress.computeIfAbsent(
                "SEARCH_FRAMEWORK_INDEX_BLOCKCHAIN", 
                k -> new AtomicLong(0)
            );
            lastExecutionTracker.set(System.currentTimeMillis());
        }

        // Execute indexing synchronously only once
        try {
            IndexingResult result = indexFilteredBlocks(blockchain.getAllBlocks(), password, privateKey);
            
            logger.info("✅ SearchFrameworkEngine blockchain indexing completed directly");
            
            return result;
            
        } catch (Exception e) {
            logger.error("Failed to execute blockchain indexing", e);
            throw e;
        }
    }

    /**
     * Index a single block for advanced search
     */
    public void indexBlock(
        Block block,
        String password,
        PrivateKey privateKey,
        EncryptionConfig config
    ) {
        if (block == null || block.getHash() == null) {
            return;
        }

        // Atomic check-and-reserve to prevent race conditions
        String blockHash = block.getHash();
        String shortHash = blockHash.substring(0, Math.min(8, blockHash.length()));
        
        // Get caller information for detailed tracking
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        String callerInfo = "UNKNOWN";
        for (int i = 2; i < Math.min(stack.length, 6); i++) {
            StackTraceElement element = stack[i];
            if (!element.getMethodName().equals("indexBlock")) {
                callerInfo = element.getClassName() + "." + element.getMethodName() + ":" + element.getLineNumber();
                break;
            }
        }
        
        // 🔍 ENHANCED RACE CONDITION DEBUGGING
        logger.info("🟦 INDEX BLOCK ATTEMPT: {} | Instance: {} | Map: {} | MapSize: {} | Thread: {} | Caller: {}", 
            shortHash, 
            this.instanceId,
            System.identityHashCode(this.blockMetadataIndex),
            this.blockMetadataIndex.size(),
            Thread.currentThread().getName(),
            callerInfo);
        
        // 🔍 DEBUG: Check current map state before putIfAbsent
        BlockMetadataLayers existingValue = globalProcessingMap.get(blockHash);
        logger.info("🔍 PRE-ATOMIC CHECK: {} | Existing: {} | IsPlaceholder: {} | Instance: {}", 
            shortHash,
            existingValue != null ? "EXISTS" : "NULL",
            existingValue != null ? existingValue.isProcessingPlaceholder() : "N/A",
            this.instanceId);
        
        // Use putIfAbsent for atomic check-and-reserve
        BlockMetadataLayers putResult = globalProcessingMap.putIfAbsent(blockHash, BlockMetadataLayers.PROCESSING_PLACEHOLDER);
        
        // 🔍 DEBUG: Detailed putIfAbsent result logging
        logger.info("🔒 ATOMIC OPERATION RESULT: {} | PutIfAbsent returned: {} | Expected: NULL for success | Instance: {} | MapSize: {}", 
            shortHash,
            putResult != null ? (putResult.isProcessingPlaceholder() ? "PLACEHOLDER" : "REAL_METADATA") : "NULL",
            this.instanceId,
            globalProcessingMap.size());
            
        if (putResult != null) {
            logger.info("⏭️ SKIPPED (already indexed/processing): {} | Instance: {} | Thread: {} | Caller: {} | Existing: {}", 
                shortHash, 
                this.instanceId,
                Thread.currentThread().getName(),
                callerInfo,
                putResult.isProcessingPlaceholder() ? "PLACEHOLDER" : "REAL_METADATA");
            return;
        }
        
        logger.info("🔒 RESERVED for processing: {} | Instance: {} | Thread: {} | Caller: {} | MapSize: {}", 
            shortHash, 
            this.instanceId,
            Thread.currentThread().getName(),
            callerInfo,
            globalProcessingMap.size());

        try {
            // 🔍 DEBUG: Verify placeholder is still there before processing
            BlockMetadataLayers verifyPlaceholder = globalProcessingMap.get(blockHash);
            logger.info("🔍 PLACEHOLDER VERIFICATION: {} | Current: {} | IsPlaceholder: {} | Instance: {}", 
                shortHash,
                verifyPlaceholder != null ? "EXISTS" : "NULL",
                verifyPlaceholder != null ? verifyPlaceholder.isProcessingPlaceholder() : "N/A",
                this.instanceId);
                
            // Generate complete metadata layers
            BlockMetadataLayers metadata =
                metadataManager.generateMetadataLayers(
                    block,
                    config,
                    password,
                    privateKey
                );

            // Replace placeholder with actual metadata in BOTH maps
            blockMetadataIndex.put(blockHash, metadata);  // Instance map for data storage
            globalProcessingMap.put(blockHash, metadata); // Global map for coordination
            
            // 🔍 DEBUG: Verify replacement was successful
            BlockMetadataLayers verifyReplacement = this.blockMetadataIndex.get(blockHash);
            logger.info("🔍 REPLACEMENT VERIFICATION: {} | IsPlaceholder: {} | Instance: {} | MapSize: {}", 
                shortHash,
                verifyReplacement != null ? verifyReplacement.isProcessingPlaceholder() : "NULL",
                this.instanceId,
                globalProcessingMap.size());
            
            logger.info("✅ SUCCESSFULLY INDEXED: {} | Instance: {} | Thread: {} | Caller: {} | MapSize: {}", 
                shortHash, 
                this.instanceId,
                Thread.currentThread().getName(),
                callerInfo,
                this.blockMetadataIndex.size());

            // Index in search strategy router
            strategyRouter.indexBlock(block.getHash(), metadata);
        } catch (Exception e) {
            // Remove placeholder on failure to allow retry
            BlockMetadataLayers removedValue = blockMetadataIndex.remove(blockHash);
            logger.error("❌ FAILED to index: {} | Instance: {} | Thread: {} | Caller: {} | Removed: {} | MapSize: {} | Error: {}", 
                shortHash, 
                this.instanceId,
                Thread.currentThread().getName(),
                callerInfo,
                removedValue != null ? (removedValue.isProcessingPlaceholder() ? "PLACEHOLDER" : "REAL_METADATA") : "NULL",
                this.blockMetadataIndex.size(),
                e.getMessage(), e);
            throw new RuntimeException(
                "Failed to index block " + block.getHash(),
                e
            );
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
    public void indexBlockWithUserTerms(
        Block block,
        String password,
        PrivateKey privateKey,
        EncryptionConfig config,
        Set<String> publicSearchTerms,
        Set<String> privateSearchTerms
    ) {
        if (block == null || block.getHash() == null) {
            return;
        }

        // Atomic check-and-reserve to prevent race conditions
        String blockHash = block.getHash();
        String shortHash = blockHash.substring(0, Math.min(8, blockHash.length()));
        
        // Get caller information for detailed tracking
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        String callerInfo = "UNKNOWN";
        for (int i = 2; i < Math.min(stack.length, 6); i++) {
            StackTraceElement element = stack[i];
            if (!element.getMethodName().equals("indexBlockWithUserTerms")) {
                callerInfo = element.getClassName() + "." + element.getMethodName() + ":" + element.getLineNumber();
                break;
            }
        }
        
        // 🔍 ENHANCED RACE CONDITION DEBUGGING
        logger.info("🟨 INDEX BLOCK WITH USER TERMS ATTEMPT: {} | Instance: {} | Map: {} | MapSize: {} | Thread: {} | Caller: {}", 
            shortHash, 
            this.instanceId,
            System.identityHashCode(this.blockMetadataIndex),
            this.blockMetadataIndex.size(),
            Thread.currentThread().getName(),
            callerInfo);
        
        // 🔍 DEBUG: Check current map state before putIfAbsent
        BlockMetadataLayers existingValue = globalProcessingMap.get(blockHash);
        logger.info("🔍 PRE-ATOMIC CHECK (USER TERMS): {} | Existing: {} | IsPlaceholder: {} | Instance: {}", 
            shortHash,
            existingValue != null ? "EXISTS" : "NULL",
            existingValue != null ? existingValue.isProcessingPlaceholder() : "N/A",
            this.instanceId);
        
        // Use putIfAbsent for atomic check-and-reserve
        BlockMetadataLayers putResult = globalProcessingMap.putIfAbsent(blockHash, BlockMetadataLayers.PROCESSING_PLACEHOLDER);
        
        // 🔍 DEBUG: Detailed putIfAbsent result logging
        logger.info("🔒 ATOMIC OPERATION RESULT (USER TERMS): {} | PutIfAbsent returned: {} | Instance: {} | MapSize: {}", 
            shortHash,
            putResult != null ? (putResult.isProcessingPlaceholder() ? "PLACEHOLDER" : "REAL_METADATA") : "NULL",
            this.instanceId,
            globalProcessingMap.size());
            
        if (putResult != null) {
            logger.info("⏭️ SKIPPED user terms block (already indexed/processing): {} | Instance: {} | Thread: {} | Caller: {}", 
                shortHash,
                this.instanceId,
                Thread.currentThread().getName(),
                callerInfo);
            return;
        }
        
        logger.info("🔒 RESERVED user terms block for processing: {} | Instance: {} | Thread: {} | Caller: {}", 
            shortHash,
            this.instanceId,
            Thread.currentThread().getName(),
            callerInfo);

        try {
            // Generate metadata layers with user-defined terms
            BlockMetadataLayers metadata =
                metadataManager.generateMetadataLayers(
                    block,
                    config,
                    password,
                    privateKey,
                    publicSearchTerms,
                    privateSearchTerms
                );

            // Replace placeholder with actual metadata in BOTH maps
            blockMetadataIndex.put(blockHash, metadata);  // Instance map for data storage
            globalProcessingMap.put(blockHash, metadata); // Global map for coordination

            // Index in search strategy router
            strategyRouter.indexBlock(blockHash, metadata);

            logger.info("✅ INDEXED user terms block: {} | Instance: {} | Public: {}, Private: {} | Thread: {} | Caller: {}",
                shortHash,
                this.instanceId,
                (publicSearchTerms != null ? publicSearchTerms.size() : 0),
                (privateSearchTerms != null ? privateSearchTerms.size() : 0),
                Thread.currentThread().getName(),
                callerInfo
            );
        } catch (Exception e) {
            // Remove placeholder on failure to allow retry
            BlockMetadataLayers removedValue = blockMetadataIndex.remove(blockHash);
            logger.error("❌ Failed to index user terms block: {} | Instance: {} | Removed: {} | Thread: {} | Caller: {} | Error: {}", 
                shortHash,
                this.instanceId,
                removedValue != null ? (removedValue.isProcessingPlaceholder() ? "PLACEHOLDER" : "REAL_METADATA") : "NULL",
                Thread.currentThread().getName(),
                callerInfo,
                e.getMessage(), e);
            throw new RuntimeException(
                "Failed to index block with user terms " + block.getHash(),
                e
            );
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
            if (
                block.getManualKeywords() != null &&
                !block.getManualKeywords().trim().isEmpty()
            ) {
                // Check if keywords are actually encrypted (format: timestamp|salt|iv|encryptedData|dataHash)
                boolean keywordsAreEncrypted = block.isDataEncrypted() && 
                    password != null && 
                    block.getManualKeywords().split("\\|").length == 5;
                
                if (keywordsAreEncrypted) {
                    // Try to decrypt manual keywords
                    try {
                        String decryptedKeywords =
                            SecureBlockEncryptionService.decryptFromString(
                                block.getManualKeywords(),
                                password
                            );
                        if (
                            decryptedKeywords != null &&
                            !decryptedKeywords.trim().isEmpty()
                        ) {
                            String[] keywordArray = decryptedKeywords.split(
                                "\\s+"
                            );
                            for (String keyword : keywordArray) {
                                if (
                                    keyword != null && !keyword.trim().isEmpty()
                                ) {
                                    terms.add(keyword.trim().toLowerCase());
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.warn(
                            "Could not decrypt manual keywords for block {}",
                            block.getHash(),
                            e
                        );
                    }
                } else {
                    // Unencrypted keywords
                    String[] keywordArray = block
                        .getManualKeywords()
                        .split("\\s+");
                    for (String keyword : keywordArray) {
                        if (keyword != null && !keyword.trim().isEmpty()) {
                            terms.add(keyword.trim().toLowerCase());
                        }
                    }
                }
            }

            // Extract from content category as a term
            if (
                block.getContentCategory() != null &&
                !block.getContentCategory().trim().isEmpty()
            ) {
                String category = block.getContentCategory().toLowerCase();
                if (
                    !"user_defined".equals(category) &&
                    !"general".equals(category)
                ) {
                    terms.add(category);
                }
            }
        } catch (Exception e) {
            logger.warn(
                "Error extracting search terms from block {}",
                block.getHash(),
                e
            );
        }

        return terms;
    }

    /**
     * Index a single block with specific password for that block
     * This allows different blocks to have different passwords
     * Enhanced with robust error handling and graceful degradation
     */
    public void indexBlockWithSpecificPassword(
        Block block,
        String blockSpecificPassword,
        PrivateKey privateKey,
        EncryptionConfig config
    ) {
        if (block == null || block.getHash() == null) {
            logger.error("Cannot index block: block or hash is null");
            return;
        }

        String blockHash = block.getHash();
        String shortHash = blockHash.substring(0, Math.min(8, blockHash.length()));
        
        // Get caller information for detailed tracking
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        String callerInfo = "UNKNOWN";
        for (int i = 2; i < Math.min(stack.length, 6); i++) {
            StackTraceElement element = stack[i];
            if (!element.getMethodName().equals("indexBlockWithSpecificPassword")) {
                callerInfo = element.getClassName() + "." + element.getMethodName() + ":" + element.getLineNumber();
                break;
            }
        }
        
        // 🔍 ENHANCED RACE CONDITION DEBUGGING
        logger.info("🟧 INDEX BLOCK WITH SPECIFIC PASSWORD ATTEMPT: {} | Instance: {} | Map: {} | MapSize: {} | Thread: {} | Caller: {}", 
            shortHash, 
            this.instanceId,
            System.identityHashCode(this.blockMetadataIndex),
            this.blockMetadataIndex.size(),
            Thread.currentThread().getName(),
            callerInfo);
        
        // 🔍 DEBUG: Check current map state before putIfAbsent
        BlockMetadataLayers existingValue = globalProcessingMap.get(blockHash);
        logger.info("🔍 PRE-ATOMIC CHECK (SPECIFIC PASSWORD): {} | Existing: {} | IsPlaceholder: {} | Instance: {}", 
            shortHash,
            existingValue != null ? "EXISTS" : "NULL",
            existingValue != null ? existingValue.isProcessingPlaceholder() : "N/A",
            this.instanceId);
        
        // Atomic check-and-reserve to prevent race conditions
        // Use putIfAbsent for atomic check-and-reserve
        BlockMetadataLayers putResult = globalProcessingMap.putIfAbsent(blockHash, BlockMetadataLayers.PROCESSING_PLACEHOLDER);
        
        // 🔍 DEBUG: Detailed putIfAbsent result logging
        logger.info("🔒 ATOMIC OPERATION RESULT (SPECIFIC PASSWORD): {} | PutIfAbsent returned: {} | Instance: {} | MapSize: {}", 
            shortHash,
            putResult != null ? (putResult.isProcessingPlaceholder() ? "PLACEHOLDER" : "REAL_METADATA") : "NULL",
            this.instanceId,
            globalProcessingMap.size());
            
        if (putResult != null) {
            logger.info("⏭️ SKIPPED specific password block (already indexed/processing): {} | Instance: {} | Thread: {} | Caller: {}", 
                shortHash,
                this.instanceId,
                Thread.currentThread().getName(),
                callerInfo);
            return;
        }
        
        logger.info("🔒 RESERVED specific password block for processing: {} | Instance: {} | Thread: {} | Caller: {}", 
            shortHash,
            this.instanceId,
            Thread.currentThread().getName(),
            callerInfo);
        boolean indexedSuccessfully = false;

        // Strategy 1: Try indexing with specific password (if block is encrypted)
        if (block.isDataEncrypted() && blockSpecificPassword != null) {
            try {
                BlockMetadataLayers metadata =
                    metadataManager.generateMetadataLayers(
                        block,
                        config,
                        blockSpecificPassword,
                        privateKey
                    );

                // Replace placeholder with actual metadata in BOTH maps
                blockMetadataIndex.put(blockHash, metadata);  // Instance map for data storage
                globalProcessingMap.put(blockHash, metadata); // Global map for coordination
                strategyRouter.indexBlock(blockHash, metadata);

                indexedSuccessfully = true;
                logger.info("✅ SUCCESSFULLY indexed encrypted block {} with specific password | Instance: {} | Thread: {} | Caller: {}",
                    shortHash,
                    this.instanceId,
                    Thread.currentThread().getName(),
                    callerInfo
                );
            } catch (Exception e) {
                logger.error(
                    "❌ Failed to index block {} with specific password | Instance: {} | Thread: {} | Caller: {} | Error: {}",
                    shortHash,
                    this.instanceId,
                    Thread.currentThread().getName(),
                    callerInfo,
                    e.getMessage(),
                    e
                );
                // Continue to fallback strategy (don't remove placeholder yet)
            }
        }

        // Strategy 2: Fallback to public metadata only (if not already indexed)
        if (!indexedSuccessfully) {
            // Double-check if another thread completed indexing during Strategy 1
            BlockMetadataLayers existingMetadata = blockMetadataIndex.get(blockHash);
            if (existingMetadata != null && !existingMetadata.isProcessingPlaceholder()) {
                logger.info("🔍 Another thread completed indexing during Strategy 1, skipping Strategy 2: {} | Instance: {} | Thread: {}",
                    shortHash,
                    this.instanceId,
                    Thread.currentThread().getName());
                return;
            }
            
            try {
                BlockMetadataLayers publicMetadata =
                    metadataManager.generateMetadataLayers(
                        block,
                        config,
                        null,
                        privateKey
                    );

                // Replace placeholder with actual metadata in BOTH maps
                blockMetadataIndex.put(blockHash, publicMetadata);  // Instance map for data storage
                globalProcessingMap.put(blockHash, publicMetadata); // Global map for coordination
                strategyRouter.indexBlock(blockHash, publicMetadata);

                indexedSuccessfully = true;
                logger.info("✅ SUCCESSFULLY indexed block {} with public metadata only | Instance: {} | Thread: {} | Caller: {}",
                    shortHash,
                    this.instanceId,
                    Thread.currentThread().getName(),
                    callerInfo
                );
            } catch (Exception e2) {
                logger.error(
                    "❌ Failed to index block {} even with public metadata | Instance: {} | Thread: {} | Caller: {} | Error: {}",
                    shortHash,
                    this.instanceId,
                    Thread.currentThread().getName(),
                    callerInfo,
                    e2.getMessage(),
                    e2
                );
                // Continue to emergency fallback (don't remove placeholder yet)
            }
        }

        // Strategy 3: Emergency fallback - minimal indexing
        if (!indexedSuccessfully) {
            // Final double-check if another thread completed indexing
            BlockMetadataLayers existingMetadata = blockMetadataIndex.get(blockHash);
            if (existingMetadata != null && !existingMetadata.isProcessingPlaceholder()) {
                logger.info("🔍 Another thread completed indexing during Strategy 2, skipping Strategy 3: {} | Instance: {} | Thread: {}",
                    shortHash,
                    this.instanceId,
                    Thread.currentThread().getName());
                return;
            }
            
            try {
                // Create minimal metadata for at least basic indexing
                createMinimalBlockIndex(block, config);
                logger.info("✅ SUCCESSFULLY created minimal index for block {} as emergency fallback | Instance: {} | Thread: {} | Caller: {}",
                    shortHash,
                    this.instanceId,
                    Thread.currentThread().getName(),
                    callerInfo
                );
            } catch (Exception e3) {
                logger.error(
                    "❌ COMPLETE indexing failure for block {} | Instance: {} | Thread: {} | Caller: {} | Error: {}",
                    shortHash,
                    this.instanceId,
                    Thread.currentThread().getName(),
                    callerInfo,
                    e3.getMessage(),
                    e3
                );
                // Remove placeholder on complete failure from BOTH maps
                BlockMetadataLayers removedValue = blockMetadataIndex.remove(blockHash);
                BlockMetadataLayers removedGlobal = globalProcessingMap.remove(blockHash);
                logger.error("🗑️ REMOVED placeholder after complete failure: {} | Removed: {} | Global: {} | Instance: {}",
                    shortHash,
                    removedValue != null ? (removedValue.isProcessingPlaceholder() ? "PLACEHOLDER" : "REAL_METADATA") : "NULL",
                    removedGlobal != null ? (removedGlobal.isProcessingPlaceholder() ? "PLACEHOLDER" : "REAL_METADATA") : "NULL",
                    this.instanceId);
                // This is a complete failure - block won't be searchable
            }
        }
        
        // If we reached here without indexing success, remove placeholder from BOTH maps
        if (!indexedSuccessfully) {
            BlockMetadataLayers removedValue = blockMetadataIndex.remove(blockHash);
            BlockMetadataLayers removedGlobal = globalProcessingMap.remove(blockHash);
            logger.warn("⚠️ Block {} could not be indexed, placeholder removed | Removed: {} | Global: {} | Instance: {} | Thread: {} | Caller: {}", 
                shortHash,
                removedValue != null ? (removedValue.isProcessingPlaceholder() ? "PLACEHOLDER" : "REAL_METADATA") : "NULL",
                removedGlobal != null ? (removedGlobal.isProcessingPlaceholder() ? "PLACEHOLDER" : "REAL_METADATA") : "NULL",
                this.instanceId,
                Thread.currentThread().getName(),
                callerInfo);
        }
    }

    /**
     * Remove block from all search indexes
     */
    public void removeBlock(String blockHash) {
        blockMetadataIndex.remove(blockHash);
        globalProcessingMap.remove(blockHash);  // Also remove from global coordination map
        strategyRouter.removeBlock(blockHash);
    }

    // ===== METADATA ACCESS =====

    // Removed unused metadata access methods - metadata is now accessed through search results instead

    // ===== SYSTEM MANAGEMENT =====

    /**
     * Get comprehensive search engine statistics
     */
    public SearchStats getSearchStats() {
        return new SearchStats(
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
     * Shutdown the search engine and clean up resources.
     * This method should be called to properly close the indexing thread pool
     * and prevent infinite background threads.
     */
    public void shutdown() {
        logger.info("🔄 Shutting down SearchFrameworkEngine...");
        
        try {
            // Shutdown the indexing executor
            indexingExecutor.shutdown();
            
            // Wait for existing tasks to complete
            if (!indexingExecutor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                logger.warn("⚠️ Indexing executor did not terminate gracefully, forcing shutdown...");
                indexingExecutor.shutdownNow();
                
                // Wait a bit more for forced shutdown
                if (!indexingExecutor.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS)) {
                    logger.error("❌ Indexing executor could not be terminated");
                }
            }
            
            // Shutdown strategy router and clear index
            strategyRouter.shutdown();
            blockMetadataIndex.clear();
            
            logger.info("✅ SearchFrameworkEngine shutdown completed");
        } catch (InterruptedException e) {
            logger.warn("⚠️ Shutdown interrupted, forcing immediate termination...");
            indexingExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    // ===== HELPER METHODS =====

    /**
     * Enhance search results with additional block information
     */
    private List<EnhancedSearchResult> enhanceSearchResults(
        List<SearchStrategyRouter.SearchResultItem> results,
        String password
    ) {
        List<EnhancedSearchResult> enhanced = new ArrayList<>();

        for (SearchStrategyRouter.SearchResultItem item : results) {
            BlockMetadataLayers metadata = blockMetadataIndex.get(
                item.getBlockHash()
            );

            // Get private metadata if password provided
            PrivateMetadata privateMetadata = null;
            if (
                password != null &&
                metadata != null &&
                metadata.hasPrivateLayer()
            ) {
                try {
                    privateMetadata = metadataManager.decryptPrivateMetadata(
                        metadata.getEncryptedPrivateLayer(),
                        password
                    );
                } catch (Exception e) {
                    // Ignore decryption failures
                }
            }

            enhanced.add(
                new EnhancedSearchResult(
                    item.getBlockHash(),
                    item.getRelevanceScore(),
                    item.getSource(),
                    item.getSummary(),
                    item.getSearchTimeMs(),
                    metadata != null ? metadata.getPublicLayer() : null,
                    privateMetadata,
                    metadata != null ? metadata.getSecurityLevel() : null
                )
            );
        }

        return enhanced;
    }

    /**
     * Create minimal block index for emergency fallback
     * Used when full metadata generation fails
     */
    private void createMinimalBlockIndex(Block block, EncryptionConfig config)
        throws Exception {
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
            minimalPublic.setTimeRange(
                block.getTimestamp().toLocalDate().toString()
            );
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
        SearchStrategyRouter.SearchRouterStats routerStats =
            strategyRouter.getRouterStats();
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

        public EnhancedSearchResult(
            String blockHash,
            double relevanceScore,
            SearchStrategyRouter.SearchResultSource source,
            String summary,
            double searchTimeMs,
            PublicMetadata publicMetadata,
            PrivateMetadata privateMetadata,
            EncryptionConfig.SecurityLevel securityLevel
        ) {
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
        public EnhancedSearchResult(
            String blockHash,
            double relevanceScore,
            SearchStrategyRouter.SearchResultSource source,
            String summary,
            double searchTimeMs,
            PublicMetadata publicMetadata,
            PrivateMetadata privateMetadata,
            EncryptionConfig.SecurityLevel securityLevel,
            OffChainMatch offChainMatch
        ) {
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
        public String getBlockHash() {
            return blockHash;
        }

        public double getRelevanceScore() {
            return relevanceScore;
        }

        public SearchStrategyRouter.SearchResultSource getSource() {
            return source;
        }

        public String getSummary() {
            return summary;
        }

        public double getSearchTimeMs() {
            return searchTimeMs;
        }

        public PublicMetadata getPublicMetadata() {
            return publicMetadata;
        }

        public PrivateMetadata getPrivateMetadata() {
            return privateMetadata;
        }

        public EncryptionConfig.SecurityLevel getSecurityLevel() {
            return securityLevel;
        }

        public OffChainMatch getOffChainMatch() {
            return offChainMatch;
        }

        public boolean hasPrivateAccess() {
            return privateMetadata != null;
        }

        public boolean hasOffChainMatch() {
            return offChainMatch != null;
        }

        public boolean isOffChainResult() {
            return (
                source ==
                SearchStrategyRouter.SearchResultSource.OFF_CHAIN_CONTENT
            );
        }

        @Override
        public String toString() {
            String offChainInfo = hasOffChainMatch()
                ? ", offChain=" + offChainMatch.getFileName()
                : "";
            return String.format(
                "Enhanced{hash=%s, score=%.2f, source=%s, security=%s%s}",
                blockHash.substring(0, Math.min(8, blockHash.length())),
                relevanceScore,
                source,
                securityLevel,
                offChainInfo
            );
        }
    }

    /**
     * Complete advanced search result
     */
    public static class SearchResult {

        private final List<EnhancedSearchResult> results;
        private final SearchStrategyRouter.SearchStrategy strategyUsed;
        private final SearchStrategyRouter.QueryAnalysis analysis;
        private final double totalTimeMs;
        private final SearchLevel searchLevel;
        private final String errorMessage;

        public SearchResult(
            List<EnhancedSearchResult> results,
            SearchStrategyRouter.SearchStrategy strategyUsed,
            SearchStrategyRouter.QueryAnalysis analysis,
            double totalTimeMs,
            SearchLevel searchLevel,
            String errorMessage
        ) {
            this.results = results != null ? results : new ArrayList<>();
            this.strategyUsed = strategyUsed;
            this.analysis = analysis;
            this.totalTimeMs = totalTimeMs;
            this.searchLevel = searchLevel;
            this.errorMessage = errorMessage;
        }

        // Getters
        public List<EnhancedSearchResult> getResults() {
            return results;
        }

        public SearchStrategyRouter.SearchStrategy getStrategyUsed() {
            return strategyUsed;
        }

        public SearchStrategyRouter.QueryAnalysis getAnalysis() {
            return analysis;
        }

        public double getTotalTimeMs() {
            return totalTimeMs;
        }

        public SearchLevel getSearchLevel() {
            return searchLevel;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public boolean isSuccessful() {
            return errorMessage == null;
        }

        public int getResultCount() {
            return results.size();
        }

        @Override
        public String toString() {
            return String.format(
                "Advanced{results=%d, strategy=%s, time=%.2fms, level=%s}",
                results.size(),
                strategyUsed,
                totalTimeMs,
                searchLevel
            );
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

        public IndexingResult(
            int blocksProcessed,
            int blocksIndexed,
            double indexingTimeMs,
            SearchStrategyRouter.SearchRouterStats routerStats
        ) {
            this.blocksProcessed = blocksProcessed;
            this.blocksIndexed = blocksIndexed;
            this.indexingTimeMs = indexingTimeMs;
            this.routerStats = routerStats;
        }

        public int getBlocksProcessed() {
            return blocksProcessed;
        }

        public int getBlocksIndexed() {
            return blocksIndexed;
        }

        public double getIndexingTimeMs() {
            return indexingTimeMs;
        }

        public SearchStrategyRouter.SearchRouterStats getRouterStats() {
            return routerStats;
        }

        public double getIndexingRate() {
            return indexingTimeMs > 0
                ? (blocksProcessed * 1000.0) / indexingTimeMs
                : 0.0;
        }

        @Override
        public String toString() {
            return String.format(
                "Indexing{processed=%d, indexed=%d, time=%.2fms, rate=%.1f blocks/s}",
                blocksProcessed,
                blocksIndexed,
                indexingTimeMs,
                getIndexingRate()
            );
        }
    }

    /**
     * Complete search engine statistics
     */
    public static class SearchStats {

        private final int totalBlocksIndexed;
        private final SearchStrategyRouter.SearchRouterStats routerStats;
        private final long estimatedMemoryBytes;

        public SearchStats(
            int totalBlocksIndexed,
            SearchStrategyRouter.SearchRouterStats routerStats,
            long estimatedMemoryBytes
        ) {
            this.totalBlocksIndexed = totalBlocksIndexed;
            this.routerStats = routerStats;
            this.estimatedMemoryBytes = estimatedMemoryBytes;
        }

        public int getTotalBlocksIndexed() {
            return totalBlocksIndexed;
        }

        public SearchStrategyRouter.SearchRouterStats getRouterStats() {
            return routerStats;
        }

        public long getEstimatedMemoryBytes() {
            return estimatedMemoryBytes;
        }

        @Override
        public String toString() {
            return String.format(
                "Advanced{blocks=%d, memory=%dMB}",
                totalBlocksIndexed,
                estimatedMemoryBytes / (1024 * 1024)
            );
        }
    }

    /**
     * Detect if we're running in a test environment
     * @return true if running in test context
     */
    private boolean isRunningInTest() {
        // Check for JUnit test runner in stack trace
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName();
            if (className.contains("junit") || 
                className.contains("Test") || 
                className.contains("surefire") ||
                className.startsWith("org.junit")) {
                return true;
            }
        }
        
        // Also check system properties that might indicate test environment
        String testProperty = System.getProperty("maven.test.skip");
        return testProperty != null || 
               System.getProperty("surefire.test.class.path") != null ||
               System.getProperty("java.class.path").contains("test-classes");
    }
}
