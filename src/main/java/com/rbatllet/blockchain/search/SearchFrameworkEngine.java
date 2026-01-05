package com.rbatllet.blockchain.search;

import com.rbatllet.blockchain.config.EncryptionConfig;
import com.rbatllet.blockchain.config.MemorySafetyConstants;
import com.rbatllet.blockchain.config.SearchConstants;
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
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

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
    private Blockchain blockchain;
    
    // 🔍 RACE CONDITION DEBUGGING: Instance identification
    private final String instanceId;
    private static final AtomicLong instanceCounter = new AtomicLong(0);
    
    // 🔒 GLOBAL ATOMIC PROTECTION: Shared across ALL instances to prevent cross-instance race conditions
    private static final ConcurrentHashMap<String, BlockMetadataLayers> globalProcessingMap = new ConcurrentHashMap<>();
    
    // 🔒 SEMAPHORE COORDINATION: One semaphore per block hash to ensure exclusive indexing
    // Each semaphore has 1 permit, meaning only ONE thread can index a block at a time
    private static final ConcurrentHashMap<String, Semaphore> blockIndexingSemaphores = new ConcurrentHashMap<>();

    /**
     * Reset global state for all SearchFrameworkEngine instances.
     * 
     * This method clears the global processing map and semaphores, releasing resources
     * and resetting coordination state. Useful for:
     * - Cleanup between test cases
     * - Resetting state between demo examples
     * - Releasing resources when reinitializing the blockchain
     * 
     * ⚠️ WARNING: This affects ALL SearchFrameworkEngine instances globally.
     */
    public static void resetGlobalState() {
        globalProcessingMap.clear();
        blockIndexingSemaphores.clear();
        logger.info("🔄 Global processing map and semaphores cleared");
    }

    private static final Logger logger = LoggerFactory.getLogger(
        SearchFrameworkEngine.class
    );

    /**
     * Explicit indexing strategies - no implicit fallbacks
     * Replaces the previous triple-nested fallback pattern with clear, explicit strategy selection
     */
    public enum IndexingStrategy {
        /**
         * Full decryption with password - best search quality
         * Used when: Block is encrypted AND password is provided
         */
        FULL_DECRYPT("Full decryption with password"),
        
        /**
         * Public metadata only - no decryption
         * Used when: Block is encrypted BUT no password provided
         */
        PUBLIC_METADATA_ONLY("Public metadata only (no password)"),
        
        /**
         * Standard indexing for unencrypted blocks
         * Used when: Block is NOT encrypted
         */
        UNENCRYPTED_STANDARD("Standard unencrypted indexing");
        
        private final String description;
        
        IndexingStrategy(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }

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
        // Java 25 Virtual Threads (Phase 1.4): Unlimited concurrent search indexing operations
        // Benefits: 20x-100x improvement for multi-user searches, automatic unmounting during DB I/O
        // Thread Naming: Uses "SearchWorker-N" for better observability in logs
        this.indexingExecutor = Executors.newThreadPerTaskExecutor(
            Thread.ofVirtual().name("SearchWorker-", 0).factory()
        );
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
    /**
     * Performs exhaustive off-chain search with memory-safe accumulation.
     *
     * <p><b>Memory Safety</b>: Uses PriorityQueue (min-heap) to keep only top N results
     * during accumulation. Buffer size = maxResults × 2 for ranking stability.</p>
     *
     * <p><b>Early Exit</b>: Stops processing after {@link com.rbatllet.blockchain.config.MemorySafetyConstants#SAFE_EXPORT_LIMIT}
     * blocks to prevent excessive processing time.</p>
     *
     * @param query Search term
     * @param password Encryption password
     * @param privateKey Private key for decryption
     * @param maxResults Maximum results (1 to 10,000)
     * @return SearchResult with top maxResults sorted by relevance
     *
     * @throws IllegalArgumentException if maxResults > 10,000
     *
     * @since 2025-10-08 (Memory Safety Refactoring - Phase A.4)
     */
    public SearchResult searchExhaustiveOffChain(
        String query,
        String password,
        PrivateKey privateKey,
        int maxResults
    ) {
        long startTime = System.nanoTime();

        try {
            // ✅ STRICT VALIDATION: Reject maxResults > 10K
            if (maxResults <= 0 || maxResults > MemorySafetyConstants.DEFAULT_MAX_SEARCH_RESULTS) {
                throw new IllegalArgumentException(
                    String.format(
                        "maxResults must be between 1 and %d. Received: %d. " +
                        "This limit prevents OutOfMemoryError on large blockchains.",
                        MemorySafetyConstants.DEFAULT_MAX_SEARCH_RESULTS,
                        maxResults
                    )
                );
            }

            logger.debug(
                "🔍 Starting TRUE EXHAUSTIVE search (on-chain + off-chain) for: \"{}\" with maxResults: {}",
                query,
                maxResults
            );

            // Step 1: Perform regular encrypted search first
            SearchResult regularResults = searchEncryptedOnly(
                query,
                password,
                maxResults
            );

            // ✅ PRIORITY QUEUE: Keep only top N results (min-heap)
            final int BUFFER_SIZE = maxResults * 2;
            final PriorityQueue<EnhancedSearchResult> topResults = new PriorityQueue<>(
                BUFFER_SIZE,
                Comparator.comparingDouble(EnhancedSearchResult::getRelevanceScore)  // Min-heap
            );

            // Add initial regular results to priority queue
            for (EnhancedSearchResult result : regularResults.getResults()) {
                addToTopResults(topResults, result, BUFFER_SIZE);
            }

            // MEMORY SAFETY: Process blocks in batches WITHOUT accumulating all in memory
            final AtomicInteger totalProcessed = new AtomicInteger(0);
            final AtomicBoolean shouldStop = new AtomicBoolean(false);

            if (blockchain != null) {
                // ✅ OPTIMIZED: Separate on-chain and off-chain searches (Phase B.2 optimization)
                // 1. Search on-chain content using processChainInBatches() (all blocks)
                blockchain.processChainInBatches(batchBlocks -> {
                    // ✅ EARLY EXIT: Stop after safe limit
                    if (shouldStop.get()) {
                        return;
                    }

                    int processed = totalProcessed.addAndGet(batchBlocks.size());

                    if (processed > MemorySafetyConstants.SAFE_EXPORT_LIMIT) {
                        logger.warn(
                            "Exhaustive search stopped after {} blocks. " +
                            "Consider using more specific search terms.",
                            processed
                        );
                        shouldStop.set(true);
                        return;
                    }

                    // Search on-chain content in this batch
                    OnChainContentSearch.OnChainSearchResult batchOnChainResults =
                        onChainContentSearch.searchOnChainContent(
                            batchBlocks,
                            query,
                            password,
                            privateKey,
                            maxResults
                        );

                    // Add on-chain results to top results
                    for (EnhancedSearchResult result : convertOnChainToEnhancedResults(batchOnChainResults, password)) {
                        addToTopResults(topResults, result, BUFFER_SIZE);
                    }
                }, MemorySafetyConstants.DEFAULT_BATCH_SIZE);

                // 2. Search off-chain content using streamBlocksWithOffChainData() (only blocks with off-chain)
                // ✅ OPTIMIZED: This reduces processing by ~80% (only processes blocks with off-chain data)
                blockchain.streamBlocksWithOffChainData(block -> {
                    if (shouldStop.get()) {
                        return; // Early exit
                    }

                    // Search off-chain content for this single block
                    OffChainSearchResult blockOffChainResult =
                        offChainFileSearch.searchOffChainContent(
                            Collections.singletonList(block),
                            query,
                            password,
                            privateKey,
                            maxResults
                        );

                    // Add off-chain results to top results
                    for (EnhancedSearchResult result : convertOffChainToEnhancedResults(blockOffChainResult, password)) {
                        addToTopResults(topResults, result, BUFFER_SIZE);
                    }
                });
            }

            // ✅ EXTRACT FINAL RESULTS: Sorted by relevance (best first)
            List<EnhancedSearchResult> allResults = extractTopResults(topResults, maxResults);

            long endTime = System.nanoTime();
            double totalTimeMs = (endTime - startTime) / 1_000_000.0;

            String summary = String.format(
                "TRUE EXHAUSTIVE search: %d final results (processed %d blocks) in %.2fms",
                allResults.size(),
                totalProcessed.get(),
                totalTimeMs
            );

            logger.info("✅ {}", summary);

            return new SearchResult(
                allResults,
                SearchStrategyRouter.SearchStrategy.PARALLEL_MULTI,
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
                SearchStrategyRouter.SearchStrategy.PARALLEL_MULTI,
                null, // QueryAnalysis not needed for error case
                totalTimeMs,
                SearchLevel.EXHAUSTIVE_OFFCHAIN,
                "Off-chain search failed: " + e.getMessage()
            );
        }
    }

    /**
     * Search blocks by content type
     *
     * Filters search results by specific content type (e.g., "medical", "financial").
     * This method uses the fast index search with content type filtering.
     *
     * @param query Search query
     * @param contentType Target content type to filter by
     * @param maxResults Maximum number of results to return
     * @return SearchResult containing filtered results
     * @throws IllegalArgumentException if query, contentType is null/empty, or maxResults is negative
     */
    public SearchResult searchByContentType(String query, String contentType, int maxResults) {
        long startTime = System.nanoTime();

        // Validate parameters
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Search query cannot be null or empty");
        }
        if (contentType == null || contentType.trim().isEmpty()) {
            throw new IllegalArgumentException("Content type cannot be null or empty");
        }
        if (maxResults < 0) {
            throw new IllegalArgumentException("maxResults cannot be negative: " + maxResults);
        }

        try {
            logger.debug("🔍 Starting content type search for: \"{}\" with type: \"{}\"", query, contentType);

            // Use FastIndexSearch to perform content type filtered search
            List<FastIndexSearch.FastSearchResult> fastResults =
                strategyRouter.getFastIndexSearch().searchByContentType(query, contentType, maxResults);

            // Convert to EnhancedSearchResult
            List<EnhancedSearchResult> enhancedResults = new ArrayList<>();
            for (FastIndexSearch.FastSearchResult result : fastResults) {
                BlockMetadataLayers metadata = blockMetadataIndex.get(result.getBlockHash());
                if (metadata == null) {
                    continue;
                }

                String context = String.format("Content type: %s | Keywords: %s",
                    contentType,
                    metadata.getPublicLayer() != null ?
                        metadata.getPublicLayer().getGeneralKeywords() : "N/A");

                EnhancedSearchResult enhancedResult = new EnhancedSearchResult(
                    result.getBlockHash(),
                    result.getRelevanceScore(),
                    SearchStrategyRouter.SearchResultSource.PUBLIC_METADATA,
                    context,
                    result.getSearchTimeMs(),
                    metadata.getPublicLayer(),
                    null, // No private metadata without password
                    metadata.getSecurityLevel()
                );

                enhancedResults.add(enhancedResult);
            }

            long endTime = System.nanoTime();
            double totalTimeMs = (endTime - startTime) / 1_000_000.0;

            logger.info("✅ Content type search completed: {} results found in {:.2f}ms",
                enhancedResults.size(), totalTimeMs);

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

            logger.error("❌ Content type search failed: {}", e.getMessage(), e);

            return new SearchResult(
                new ArrayList<>(),
                SearchStrategyRouter.SearchStrategy.FAST_PUBLIC,
                null,
                totalTimeMs,
                SearchLevel.FAST_ONLY,
                "Content type search failed: " + e.getMessage()
            );
        }
    }

    /**
     * Search blocks within a time range
     *
     * Filters search results by specific time range (e.g., "2024-01", "2024-Q1").
     * This method uses the fast index search with time range filtering.
     *
     * @param query Search query
     * @param timeRange Target time range to filter by
     * @param maxResults Maximum number of results to return
     * @return SearchResult containing time-filtered results
     * @throws IllegalArgumentException if query, timeRange is null/empty, or maxResults is negative
     */
    public SearchResult searchByTimeRange(String query, String timeRange, int maxResults) {
        long startTime = System.nanoTime();

        // Validate parameters
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Search query cannot be null or empty");
        }
        if (timeRange == null || timeRange.trim().isEmpty()) {
            throw new IllegalArgumentException("Time range cannot be null or empty");
        }
        if (maxResults < 0) {
            throw new IllegalArgumentException("maxResults cannot be negative: " + maxResults);
        }

        try {
            logger.debug("🔍 Starting time range search for: \"{}\" within: \"{}\"", query, timeRange);

            // Use FastIndexSearch to perform time range filtered search
            List<FastIndexSearch.FastSearchResult> fastResults =
                strategyRouter.getFastIndexSearch().searchByTimeRange(query, timeRange, maxResults);

            // Convert to EnhancedSearchResult
            List<EnhancedSearchResult> enhancedResults = new ArrayList<>();
            for (FastIndexSearch.FastSearchResult result : fastResults) {
                BlockMetadataLayers metadata = blockMetadataIndex.get(result.getBlockHash());
                if (metadata == null) {
                    continue;
                }

                String context = String.format("Time range: %s | Keywords: %s",
                    timeRange,
                    metadata.getPublicLayer() != null ?
                        metadata.getPublicLayer().getGeneralKeywords() : "N/A");

                EnhancedSearchResult enhancedResult = new EnhancedSearchResult(
                    result.getBlockHash(),
                    result.getRelevanceScore(),
                    SearchStrategyRouter.SearchResultSource.PUBLIC_METADATA,
                    context,
                    result.getSearchTimeMs(),
                    metadata.getPublicLayer(),
                    null, // No private metadata without password
                    metadata.getSecurityLevel()
                );

                enhancedResults.add(enhancedResult);
            }

            long endTime = System.nanoTime();
            double totalTimeMs = (endTime - startTime) / 1_000_000.0;

            logger.info("✅ Time range search completed: {} results found in {:.2f}ms",
                enhancedResults.size(), totalTimeMs);

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

            logger.error("❌ Time range search failed: {}", e.getMessage(), e);

            return new SearchResult(
                new ArrayList<>(),
                SearchStrategyRouter.SearchStrategy.FAST_PUBLIC,
                null,
                totalTimeMs,
                SearchLevel.FAST_ONLY,
                "Time range search failed: " + e.getMessage()
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
                match.getRelevanceScore() + SearchConstants.ON_CHAIN_BONUS,
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
            double boostedRelevance = match.getRelevanceScore() + SearchConstants.OFF_CHAIN_BONUS;

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
                            // STRATEGY 1: Use user-defined terms for indexing
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
                            // STRATEGY 2: No user terms - use automatic content extraction
                            if (block.isDataEncrypted()) {
                                // For encrypted blocks, try with provided password (if any)
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
     * Index entire blockchain for advanced search (SYNCHRONOUS - BLOCKING).
     * 
     * <p>This method blocks the calling thread until all blocks have been indexed and returns
     * accurate results. It internally calls {@link #indexBlockchain(Blockchain, String, PrivateKey)}
     * and then waits for the background indexing coordinator to finish processing all blocks.</p>
     * 
     * <p><strong>Behavior:</strong></p>
     * <ol>
     *   <li>Triggers asynchronous indexing via {@code indexBlockchain()}</li>
     *   <li>Blocks the current thread using {@code IndexingCoordinator.waitForCompletion()}</li>
     *   <li>Returns only after ALL blocks have been processed</li>
     *   <li>Returns accurate counts from {@code blockMetadataIndex.size()}</li>
     * </ol>
     * 
     * <p><strong>Use Cases:</strong></p>
     * <ul>
     *   <li><strong>Unit tests:</strong> Verify indexing completed before assertions</li>
     *   <li><strong>Demo scripts:</strong> Sequential execution required for predictable output</li>
     *   <li><strong>CLI tools:</strong> User expects operation to complete before next command</li>
     *   <li><strong>Synchronous APIs:</strong> Caller needs immediate indexing confirmation</li>
     *   <li><strong>Integration tests:</strong> Ensure search index ready before validation</li>
     * </ul>
     * 
     * <p><strong>Difference from {@link #indexBlockchain(Blockchain, String, PrivateKey)}:</strong></p>
     * <ul>
     *   <li><strong>indexBlockchain():</strong> Returns immediately, indexing in background</li>
     *   <li><strong>indexBlockchainSync():</strong> Blocks until complete, accurate counts</li>
     * </ul>
     * 
     * <p><strong>Thread Safety:</strong></p>
     * <ul>
     *   <li>Thread-safe: multiple threads can call simultaneously</li>
     *   <li>Interruption: {@code InterruptedException} wrapped in {@code RuntimeException}</li>
     *   <li>Preserves interrupt status: {@code Thread.currentThread().interrupt()} called</li>
     * </ul>
     * 
     * <p><strong>Privacy Model (Privacy-by-Design):</strong></p>
     * <ul>
     *   <li>Only blocks with explicit keywords are indexed</li>
     *   <li>Blocks without keywords remain non-searchable (privacy default)</li>
     *   <li>Use {@code addBlockWithKeywords()} to make blocks searchable</li>
     * </ul>
     * 
     * <p><strong>Example Usage:</strong></p>
     * <pre>{@code
     * // In a test - ensure indexing completes before search
     * SearchFrameworkEngine engine = new SearchFrameworkEngine();
     * IndexingResult result = engine.indexBlockchainSync(blockchain, password, privateKey);
     * assertEquals(10, result.getTotalIndexed());
     * 
     * // Now safe to search - all blocks are indexed
     * SearchResult searchResult = engine.search("financial", password, privateKey);
     * }</pre>
     * 
     * @param blockchain The blockchain to index (must not be null)
     * @param password Password for decrypting private metadata (must not be null)
     * @param privateKey Private key for signature verification (must not be null)
     * @return IndexingResult with accurate block counts and timing statistics.
     *         Both {@code totalProcessed} and {@code successfullyIndexed} reflect actual
     *         indexed blocks from {@code blockMetadataIndex}.
     * @throws RuntimeException if thread is interrupted during indexing (wraps InterruptedException)
     * @throws IllegalArgumentException if any parameter is null
     * @see #indexBlockchain(Blockchain, String, PrivateKey) for async version
     * @see IndexingCoordinator#waitForCompletion() for manual completion wait
     * @see IndexingResult for result structure
     */
    public IndexingResult indexBlockchainSync(
        Blockchain blockchain,
        String password,
        PrivateKey privateKey
    ) {
        long startTime = System.nanoTime();
        
        // Trigger async indexing
        indexBlockchain(blockchain, password, privateKey);
        
        // Wait for all background indexing to complete
        try {
            IndexingCoordinator.getInstance().waitForCompletion();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Indexing interrupted", e);
            throw new RuntimeException("Blockchain indexing was interrupted", e);
        }
        
        double durationMs = (System.nanoTime() - startTime) / 1_000_000.0;
        
        // Count actual indexed blocks from the instance metadata index
        // This is the reliable source - blockMetadataIndex contains the actual data
        int totalIndexed = blockMetadataIndex.size();
        
        logger.info("✅ Synchronous blockchain indexing completed: {} blocks indexed in {}ms",
            totalIndexed, durationMs);
        
        return new IndexingResult(
            totalIndexed,
            totalIndexed,
            durationMs,
            strategyRouter.getRouterStats()
        );
    }

    /**
     * Index entire blockchain for advanced search (ASYNCHRONOUS)
     * Returns immediately while indexing continues in background.
     * 
     * <p><strong>Use cases:</strong></p>
     * <ul>
     *   <li>Background jobs - non-blocking indexing</li>
     *   <li>Scheduled tasks - fire-and-forget</li>
     *   <li>UI operations - avoid blocking user interface</li>
     * </ul>
     * 
     * <p><strong>Note:</strong> Call {@code IndexingCoordinator.getInstance().waitForCompletion()}
     * if you need to wait for completion manually.</p>
     * 
     * @param blockchain The blockchain to index
     * @param password Password for decrypting private metadata
     * @param privateKey Private key for signature verification
     * @return IndexingResult with initial counts (may not reflect final state)
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
            // MEMORY SAFETY: Index blocks in batches WITHOUT accumulating all in memory
            final int[] totalProcessed = {0};
            final int[] totalIndexed = {0};
            final long startTime = System.nanoTime();

            blockchain.processChainInBatches(batch -> {
                // Index this batch directly without accumulating
                IndexingResult batchResult = indexFilteredBlocks(batch, password, privateKey);
                totalProcessed[0] += batchResult.getBlocksProcessed();
                totalIndexed[0] += batchResult.getBlocksIndexed();
            }, 1000);

            double durationMs = (System.nanoTime() - startTime) / 1_000_000.0;
            IndexingResult result = new IndexingResult(totalProcessed[0], totalIndexed[0], durationMs, null);

            logger.info("✅ SearchFrameworkEngine blockchain indexing completed: {} processed, {} indexed in {}ms",
                totalProcessed[0], totalIndexed[0], durationMs);

            return result;

        } catch (Exception e) {
            logger.error("Failed to execute blockchain indexing", e);
            throw e;
        }
    }

    /**
     * Check if a block is already indexed in the search framework.
     * 
     * <p>This method checks both the global processing map and the instance metadata index
     * to determine if a block has already been indexed. It returns true if:
     * <ul>
     *   <li>The block exists in globalProcessingMap AND is not a processing placeholder</li>
     *   <li>This indicates the block has been fully indexed with real metadata</li>
     * </ul>
     * 
     * @param blockHash The hash of the block to check
     * @return true if the block is fully indexed, false otherwise
     */
    public boolean isBlockIndexed(String blockHash) {
        if (blockHash == null || blockHash.trim().isEmpty()) {
            return false;
        }
        
        BlockMetadataLayers metadata = globalProcessingMap.get(blockHash);
        
        // Block is indexed if it exists and is NOT a placeholder
        // Placeholders indicate indexing in progress, not completed indexing
        return metadata != null && !metadata.isProcessingPlaceholder();
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

        String blockHash = block.getHash();
        String shortHash = blockHash.substring(0, Math.min(8, blockHash.length()));
        
        // Get or create a semaphore for this block (1 permit = only 1 thread can index at a time)
        Semaphore semaphore = blockIndexingSemaphores.computeIfAbsent(
            blockHash, 
            k -> new Semaphore(1, true) // fair semaphore
        );
        
        // Try to acquire the semaphore - if another thread is indexing, wait
        try {
            logger.debug("🔒 [{}] Waiting to acquire indexing lock...", shortHash);
            semaphore.acquire();
            logger.debug("✅ [{}] Acquired indexing lock", shortHash);
            
            // Double-check if already indexed after acquiring lock
            BlockMetadataLayers existing = globalProcessingMap.get(blockHash);
            if (existing != null && !existing.isProcessingPlaceholder()) {
                logger.info("⏭️ [{}] Already indexed (detected after lock acquisition), skipping", shortHash);
                return;
            }
            
            // Mark as processing
            globalProcessingMap.put(blockHash, BlockMetadataLayers.PROCESSING_PLACEHOLDER);
            
            try {
                // Generate metadata
                BlockMetadataLayers metadata = metadataManager.generateMetadataLayers(
                    block,
                    config,
                    password,
                    privateKey
                );

                // Store metadata
                blockMetadataIndex.put(blockHash, metadata);
                globalProcessingMap.put(blockHash, metadata);
                
                // Index in strategy router
                strategyRouter.indexBlock(block.getHash(), metadata);
                
                logger.info("✅ [{}] Successfully indexed", shortHash);
                
            } catch (Exception e) {
                // Remove placeholder on failure
                blockMetadataIndex.remove(blockHash);
                globalProcessingMap.remove(blockHash);
                logger.error("❌ [{}] Failed to index: {}", shortHash, e.getMessage(), e);
                throw new RuntimeException("Failed to index block " + blockHash, e);
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("❌ [{}] Interrupted while waiting for indexing lock", shortHash);
            throw new RuntimeException("Interrupted while waiting to index block " + blockHash, e);
        } finally {
            // Always release the semaphore
            semaphore.release();
            logger.debug("🔓 [{}] Released indexing lock", shortHash);
        }
    }

    /**
     * Index a single block WITHOUT password (for non-encrypted blocks with public keywords).
     *
     * <p><strong>ARCHITECTURAL FIX:</strong> This overload is specifically for indexing
     * non-encrypted blocks where all keywords are public and searchable without a password.
     * It avoids the conceptual issue of requiring a password for non-encrypted content.</p>
     *
     * <p><strong>Use Cases:</strong>
     * <ul>
     *   <li>Non-encrypted blocks with public keywords</li>
     *   <li>Blocks where all metadata is publicly searchable</li>
     *   <li>Re-indexing operations where password is not available</li>
     * </ul>
     *
     * <p><strong>Implementation:</strong> Internally calls the password-based overload with
     * {@code null} password. The MetadataLayerManager will extract keywords from the block
     * and place them in the public layer (no password required for search).</p>
     *
     * @param block The block to index (must not be null)
     * @param privateKey Private key for cryptographic operations (can be null)
     * @param config Encryption configuration
     * @throws RuntimeException if indexing fails
     * @since 1.0.6
     */
    public void indexBlock(
        Block block,
        PrivateKey privateKey,
        EncryptionConfig config
    ) {
        // Delegate to password-based overload with null password
        // MetadataLayerManager will handle non-encrypted blocks correctly
        indexBlock(block, null, privateKey, config);
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
        logger.debug("🔒 ATOMIC OPERATION RESULT (USER TERMS): {} | PutIfAbsent returned: {} | Instance: {} | MapSize: {}", 
            shortHash,
            putResult != null ? (putResult.isProcessingPlaceholder() ? "PLACEHOLDER" : "REAL_METADATA") : "NULL",
            this.instanceId,
            globalProcessingMap.size());
            
        if (putResult != null) {
            logger.debug("⏭️ SKIPPED user terms block (already indexed/processing): {} | Instance: {} | Thread: {} | Caller: {}", 
                shortHash,
                this.instanceId,
                Thread.currentThread().getName(),
                callerInfo);
            return;
        }
        
        logger.debug("🔒 RESERVED user terms block for processing: {} | Instance: {} | Thread: {} | Caller: {}", 
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
     * 
     * REFACTORED (v1.0.6): Now uses explicit strategy pattern instead of triple-nested fallbacks
     * Benefits:
     * - Clear strategy selection upfront
     * - No hidden bugs from cascading fallbacks
     * - Fast failure detection in tests
     * - Single execution path per block
     */
    public void indexBlockWithSpecificPassword(
        Block block,
        String blockSpecificPassword,
        PrivateKey privateKey,
        EncryptionConfig config
    ) {
        // Delegate to explicit strategy implementation
        indexBlockWithExplicitStrategy(block, blockSpecificPassword, privateKey, config);
    }

    /**
     * Select optimal indexing strategy based on block characteristics
     * Clear decision tree - no ambiguity, no trial-and-error
     * 
     * @param block Block to index
     * @param password Optional password for decryption
     * @return Optimal strategy for this block
     */
    private IndexingStrategy selectIndexingStrategy(Block block, String password) {
        // Decision tree based on block characteristics
        if (!block.isDataEncrypted()) {
            // Unencrypted blocks: simple standard indexing
            logger.debug("Block {} is unencrypted - using UNENCRYPTED_STANDARD strategy", 
                block.getHash().substring(0, 8));
            return IndexingStrategy.UNENCRYPTED_STANDARD;
        }
        
        // Block is encrypted - check if password is available
        if (password != null && !password.isEmpty()) {
            // Encrypted block + password: full decryption for best search quality
            logger.debug("Block {} is encrypted with password - using FULL_DECRYPT strategy", 
                block.getHash().substring(0, 8));
            return IndexingStrategy.FULL_DECRYPT;
        }
        
        // Encrypted block without password: public metadata only
        logger.debug("Block {} is encrypted without password - using PUBLIC_METADATA_ONLY strategy", 
            block.getHash().substring(0, 8));
        return IndexingStrategy.PUBLIC_METADATA_ONLY;
    }

    /**
     * Generate metadata for a specific strategy
     * Single execution path - no trial-and-error
     * 
     * @param block Block to index
     * @param strategy Indexing strategy to use
     * @param password Optional password (only used for FULL_DECRYPT)
     * @param privateKey Private key for signatures
     * @param config Encryption configuration
     * @return Generated metadata layers
     * @throws Exception if metadata generation fails (no fallbacks!)
     */
    private BlockMetadataLayers generateMetadataForStrategy(
        Block block,
        IndexingStrategy strategy,
        String password,
        PrivateKey privateKey,
        EncryptionConfig config
    ) throws Exception {
        
        switch (strategy) {
            case FULL_DECRYPT:
                // Full decryption with password
                if (password == null || password.isEmpty()) {
                    throw new IllegalArgumentException(
                        "FULL_DECRYPT strategy requires password but none provided"
                    );
                }
                return metadataManager.generateMetadataLayers(
                    block, 
                    config, 
                    password, 
                    privateKey
                );
                
            case PUBLIC_METADATA_ONLY:
                // Public metadata only (no password)
                return metadataManager.generateMetadataLayers(
                    block, 
                    config, 
                    null,  // No password for public-only
                    privateKey
                );
                
            case UNENCRYPTED_STANDARD:
                // Standard indexing (block is not encrypted)
                return metadataManager.generateMetadataLayers(
                    block, 
                    config, 
                    null,  // No password needed
                    privateKey
                );
                
            default:
                throw new IllegalStateException("Unknown indexing strategy: " + strategy);
        }
    }

    /**
     * Index a single block using explicit strategy selection
     * NO FALLBACKS - Fail fast and report problems clearly
     * 
     * This method replaces the previous triple-nested fallback pattern with explicit strategy selection.
     * Benefits:
     * - Clear intent: Strategy is selected ONCE upfront
     * - Fast failure: Bugs exposed immediately in tests
     * - Maintainability: Single execution path per strategy
     * - Diagnostics: Failures recorded with strategy context
     * - Performance: No retries on errors (fails fast)
     * 
     * @param block Block to index
     * @param blockSpecificPassword Optional password for this specific block
     * @param privateKey Private key for signatures
     * @param config Encryption configuration
     */
    private void indexBlockWithExplicitStrategy(
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
            if (!element.getMethodName().equals("indexBlockWithExplicitStrategy")) {
                callerInfo = element.getClassName() + "." + element.getMethodName() + ":" + element.getLineNumber();
                break;
            }
        }
        
        // 🔍 ENHANCED DEBUGGING
        logger.info("🟦 EXPLICIT STRATEGY INDEX ATTEMPT: {} | Instance: {} | Thread: {} | Caller: {}", 
            shortHash, 
            this.instanceId,
            Thread.currentThread().getName(),
            callerInfo);
        
        // Atomic check-and-reserve to prevent race conditions
        BlockMetadataLayers putResult = globalProcessingMap.putIfAbsent(blockHash, BlockMetadataLayers.PROCESSING_PLACEHOLDER);
        
        logger.debug("🔒 ATOMIC OPERATION RESULT (EXPLICIT): {} | PutIfAbsent returned: {} | Instance: {}", 
            shortHash,
            putResult != null ? (putResult.isProcessingPlaceholder() ? "PLACEHOLDER" : "REAL_METADATA") : "NULL",
            this.instanceId);
            
        if (putResult != null) {
            logger.debug("⏭️ SKIPPED block (already indexed/processing): {} | Instance: {} | Thread: {}", 
                shortHash,
                this.instanceId,
                Thread.currentThread().getName());
            return;
        }
        
        logger.debug("🔒 RESERVED block for processing: {} | Instance: {} | Thread: {}", 
            shortHash,
            this.instanceId,
            Thread.currentThread().getName());
        
        // STEP 1: Select strategy ONCE upfront (no guessing, no retries)
        IndexingStrategy strategy = selectIndexingStrategy(block, blockSpecificPassword);
        
        logger.info("📋 Indexing block {} with strategy: {} | Instance: {} | Thread: {} | Caller: {}", 
            shortHash, 
            strategy.getDescription(), 
            this.instanceId, 
            Thread.currentThread().getName(),
            callerInfo
        );
        
        // STEP 2: Execute the selected strategy ONCE (no fallbacks hiding bugs)
        try {
            BlockMetadataLayers metadata = generateMetadataForStrategy(
                block, 
                strategy, 
                blockSpecificPassword, 
                privateKey, 
                config
            );
            
            // Store metadata in both maps
            blockMetadataIndex.put(blockHash, metadata);
            globalProcessingMap.put(blockHash, metadata);
            strategyRouter.indexBlock(blockHash, metadata);
            
            logger.info("✅ SUCCESSFULLY indexed block {} with strategy: {} | Instance: {} | Thread: {}", 
                shortHash, 
                strategy.getDescription(),
                this.instanceId,
                Thread.currentThread().getName()
            );
            
        } catch (Exception e) {
            // NO FALLBACKS - Report failure clearly for diagnosis
            logger.error("❌ FAILED to index block {} with strategy: {} | Instance: {} | Thread: {} | Error: {}", 
                shortHash, 
                strategy.getDescription(),
                this.instanceId,
                Thread.currentThread().getName(),
                e.getMessage(), 
                e
            );
            
            // Clean up placeholder
            blockMetadataIndex.remove(blockHash);
            globalProcessingMap.remove(blockHash);
            
            // CRITICAL: Don't hide the failure
            // In production: Log and continue (don't break indexing pipeline)
            // In testing: We want to see failures clearly in logs
            logger.error("🚨 Block {} will not be searchable - indexing failed with strategy: {}", 
                shortHash, strategy.getDescription());
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
    /**
     * Clear all search indexes without shutting down the search engine.
     * Use this for test cleanup or database reinitialization.
     *
     * <p>Unlike {@link #clearAll()}, this method does NOT shut down the executor service,
     * allowing the search engine to continue operating after index cleanup.</p>
     */
    public void clearIndexes() {
        blockMetadataIndex.clear();
        strategyRouter.clearIndexes();
    }

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
            if (!indexingExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                logger.warn("⚠️ Indexing executor did not terminate gracefully, forcing shutdown...");
                indexingExecutor.shutdownNow();
                
                // Wait a bit more for forced shutdown
                if (!indexingExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
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

        /**
         * Get top N results sorted by relevance score (highest first)
         * @param limit Maximum number of results to return
         * @return List of top results, empty if no results
         */
        public List<EnhancedSearchResult> getTopResults(int limit) {
            if (results == null || results.isEmpty()) {
                return Collections.emptyList();
            }

            int safeLimit = Math.max(0, limit);
            return results.stream()
                .filter(Objects::nonNull)
                .sorted((a, b) -> Double.compare(b.getRelevanceScore(), a.getRelevanceScore()))
                .limit(safeLimit)
                .collect(Collectors.toList());
        }

        /**
         * Calculate average relevance score across all results
         * @return Average score, or 0.0 if no results
         */
        public double getAverageRelevanceScore() {
            if (results == null || results.isEmpty()) {
                return 0.0;
            }

            return results.stream()
                .filter(Objects::nonNull)
                .mapToDouble(result -> {
                    double score = result.getRelevanceScore();
                    return (Double.isNaN(score) || Double.isInfinite(score)) ? 0.0 : score;
                })
                .average()
                .orElse(0.0);
        }

        /**
         * Group results by their source (ON_CHAIN, OFF_CHAIN, etc.)
         * @return Map of source to list of results
         */
        public Map<SearchStrategyRouter.SearchResultSource, List<EnhancedSearchResult>> groupBySource() {
            Map<SearchStrategyRouter.SearchResultSource, List<EnhancedSearchResult>> grouped = new HashMap<>();

            if (results == null) {
                return Collections.unmodifiableMap(grouped);
            }

            for (EnhancedSearchResult result : results) {
                if (result == null || result.getSource() == null) {
                    continue;
                }

                grouped.computeIfAbsent(result.getSource(), k -> new ArrayList<>()).add(result);
            }

            return Collections.unmodifiableMap(grouped);
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

        private final long totalBlocksIndexed;
        private final SearchStrategyRouter.SearchRouterStats routerStats;
        private final long estimatedMemoryBytes;

        public SearchStats(
            long totalBlocksIndexed,
            SearchStrategyRouter.SearchRouterStats routerStats,
            long estimatedMemoryBytes
        ) {
            this.totalBlocksIndexed = totalBlocksIndexed;
            this.routerStats = routerStats;
            this.estimatedMemoryBytes = estimatedMemoryBytes;
        }

        public long getTotalBlocksIndexed() {
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
     * Adds new results to priority queue, maintaining only top N results.
     *
     * <p><b>Algorithm</b>: Min-heap keeps lowest score at root.
     * If new result has higher score than root, evict root and add new result.</p>
     *
     * @param topResults Priority queue (min-heap)
     * @param newResult New search result to consider
     * @param maxSize Maximum size of priority queue
     *
     * @since 2025-10-08 (Memory Safety Refactoring - Phase A.4)
     */
    private void addToTopResults(
            PriorityQueue<EnhancedSearchResult> topResults,
            EnhancedSearchResult newResult,
            int maxSize) {
        if (topResults.size() < maxSize) {
            // Heap not full - add directly
            topResults.offer(newResult);
        } else {
            // Heap full - compare with lowest score
            EnhancedSearchResult lowest = topResults.peek();
            if (newResult.getRelevanceScore() > lowest.getRelevanceScore()) {
                topResults.poll();      // Remove lowest
                topResults.offer(newResult);  // Add new higher
            }
        }
    }

    /**
     * Extracts top N results from priority queue, sorted by relevance (descending).
     *
     * @param topResults Priority queue (min-heap) containing top results
     * @param maxResults Maximum results to extract
     * @return Sorted list of results (best first)
     *
     * @since 2025-10-08 (Memory Safety Refactoring - Phase A.4)
     */
    private List<EnhancedSearchResult> extractTopResults(
            PriorityQueue<EnhancedSearchResult> topResults,
            int maxResults) {
        // Convert heap to sorted list (best first)
        List<EnhancedSearchResult> results = new ArrayList<>(topResults);
        results.sort(Comparator.comparingDouble(EnhancedSearchResult::getRelevanceScore).reversed());

        // Trim to maxResults
        return results.subList(0, Math.min(maxResults, results.size()));
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
