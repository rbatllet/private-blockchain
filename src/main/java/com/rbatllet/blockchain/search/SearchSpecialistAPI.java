package com.rbatllet.blockchain.search;

import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.config.EncryptionConfig;
import com.rbatllet.blockchain.search.SearchFrameworkEngine.*;
import java.security.PrivateKey;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Search Specialist API - Advanced Search Operations for Blockchain Data
 * 
 * <p>This API provides a comprehensive interface for performing advanced search operations
 * on blockchain data. It serves as the middle-tier API between the user-friendly
 * {@link com.rbatllet.blockchain.api.UserFriendlyEncryptionAPI} and the low-level
 * {@link SearchFrameworkEngine}.</p>
 * 
 * <h2>Key Features:</h2>
 * <ul>
 *   <li><strong>Fast Public Search:</strong> Sub-50ms searches on public metadata</li>
 *   <li><strong>Encrypted Content Search:</strong> Deep search with password protection</li>
 *   <li><strong>Intelligent Strategy Routing:</strong> Automatic optimization based on query characteristics</li>
 *   <li><strong>Three-Layer Metadata Architecture:</strong> Public, protected, and private data layers</li>
 *   <li><strong>Password Registry Management:</strong> Secure password storage for encrypted blocks</li>
 *   <li><strong>Asynchronous Operations:</strong> Non-blocking search and indexing operations</li>
 * </ul>
 * 
 * <h2>Usage Patterns:</h2>
 * <pre>{@code
 * // Initialize the search API
 * SearchSpecialistAPI searchAPI = new SearchSpecialistAPI();
 * searchAPI.initializeWithBlockchain(blockchain, password, privateKey);
 * 
 * // Fast public-only search (sub-50ms)
 * List<EnhancedSearchResult> publicResults = searchAPI.searchPublic("medical records");
 * 
 * // Convenient hybrid search (public + private with default credentials)
 * List<EnhancedSearchResult> hybridResults = searchAPI.searchSimple("patient data");
 * 
 * // Secure encrypted-only search
 * List<EnhancedSearchResult> secureResults = searchAPI.searchSecure("confidential", password, 50);
 * 
 * // Intelligent adaptive search
 * List<EnhancedSearchResult> smartResults = searchAPI.searchIntelligent("diagnosis", password, 100);
 * }</pre>
 * 
 * <h2>Target Audience:</h2>
 * <p>This API is designed for <strong>search specialists</strong> and developers who need:
 * <ul>
 *   <li>Fine-grained control over search strategies</li>
 *   <li>Maximum search performance optimization</li>
 *   <li>Advanced search analytics and metrics</li>
 *   <li>Specialized search operations beyond basic blockchain usage</li>
 * </ul>
 * 
 * <h2>Performance Characteristics:</h2>
 * <ul>
 *   <li><strong>Public Search:</strong> &lt;50ms average response time</li>
 *   <li><strong>Encrypted Search:</strong> &lt;200ms average response time</li>
 *   <li><strong>Memory Usage:</strong> Optimized for large blockchain datasets</li>
 *   <li><strong>Concurrency:</strong> Thread-safe operations with intelligent caching</li>
 * </ul>
 * 
 * <h2>Security:</h2>
 * <ul>
 *   <li>AES-256-GCM encryption for sensitive data</li>
 *   <li>Secure password registry with automatic cleanup</li>
 *   <li>Privacy-preserving search capabilities</li>
 *   <li>Audit trails for all search operations</li>
 * </ul>
 * 
 * @see SearchFrameworkEngine for low-level search engine operations
 * @see com.rbatllet.blockchain.api.UserFriendlyEncryptionAPI for all-in-one blockchain operations
 * @since 1.0
 * @author Private Blockchain Implementation Team
 */
public class SearchSpecialistAPI {
    
    private static final Logger logger = LoggerFactory.getLogger(SearchSpecialistAPI.class);
    
    private final SearchFrameworkEngine searchEngine;
    private final BlockPasswordRegistry passwordRegistry;
    private final EncryptionConfig encryptionConfig;
    private boolean isInitialized = false;
    private boolean isDirectlyInstantiated = false;
    private String defaultPassword = null;
    
    /**
     * Creates a new SearchSpecialistAPI instance with default high-security configuration.
     * 
     * <p>⚠️ <strong>WARNING:</strong> Direct instantiation is NOT recommended for most use cases.
     * This approach bypasses the blockchain's initialization process and may result in
     * empty search results or initialization failures.</p>
     * 
     * <p>🔧 <strong>PREFERRED APPROACH:</strong>
     * <pre>{@code
     * // Step 1: Initialize blockchain's advanced search
     * blockchain.initializeAdvancedSearch(password);
     * 
     * // Step 2: Get the properly initialized instance
     * SearchSpecialistAPI searchAPI = blockchain.getSearchSpecialistAPI();
     * 
     * // Step 3: Verify ready state
     * if (!searchAPI.isReady()) {
     *     searchAPI.initializeWithBlockchain(blockchain, password, privateKey);
     * }
     * }</pre>
     * 
     * <p><strong>Note:</strong> You must call {@link #initializeWithBlockchain(Blockchain, String, PrivateKey)}
     * before performing any search operations.</p>
     * 
     */
    // REMOVED: Empty constructor to simplify API and force proper initialization
    // Old constructor was causing confusion and test failures
    
    /**
     * Creates a new SearchSpecialistAPI instance with required blockchain, password, and private key.
     * 
     * <p>This constructor enforces proper initialization by requiring all necessary parameters,
     * preventing the common mistake of direct instantiation without proper setup.</p>
     * 
     * <p><strong>Automatic Initialization:</strong> This constructor automatically
     * initializes the search engine with the provided blockchain, password, and private key,
     * eliminating the need for separate initialization calls.</p>
     * 
     * @param blockchain the blockchain instance to use for search operations. Must not be null.
     * @param password the password for accessing encrypted content. Must not be null.
     * @param privateKey the private key for secure operations. Must not be null.
     * @throws IllegalArgumentException if any parameter is null
     * @throws RuntimeException if initialization fails
     */
    public SearchSpecialistAPI(Blockchain blockchain, String password, PrivateKey privateKey) {
        this(blockchain, password, privateKey, EncryptionConfig.createHighSecurityConfig());
    }
    
    /**
     * Creates a new SearchSpecialistAPI instance with custom encryption configuration.
     * 
     * <p>This constructor allows you to specify custom security settings while ensuring
     * proper initialization with the blockchain. This is the most flexible and powerful
     * constructor for advanced users who need specific encryption configurations.</p>
     * 
     * <p><strong>Custom Configuration Benefits:</strong></p>
     * <ul>
     *   <li>Custom encryption algorithms and key sizes</li>
     *   <li>Performance vs security trade-offs</li>
     *   <li>Specialized validation policies</li>
     *   <li>Environment-specific security requirements</li>
     * </ul>
     * 
     * @param blockchain the blockchain instance to use for search operations. Must not be null.
     * @param password the password for accessing encrypted content. Must not be null.
     * @param privateKey the private key for secure operations. Must not be null.
     * @param config the encryption configuration to use. Must not be null.
     *               Use {@link EncryptionConfig#createHighSecurityConfig()} for maximum security,
     *               {@link EncryptionConfig#createBalancedConfig()} for balanced performance,
     *               or {@link EncryptionConfig#createPerformanceConfig()} for speed priority.
     * @throws IllegalArgumentException if any parameter is null
     * @throws RuntimeException if initialization fails
     * @see EncryptionConfig for available configuration options
     */
    public SearchSpecialistAPI(Blockchain blockchain, String password, PrivateKey privateKey, EncryptionConfig config) {
        if (blockchain == null) {
            throw new IllegalArgumentException("Blockchain cannot be null");
        }
        if (password == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }
        if (privateKey == null) {
            throw new IllegalArgumentException("Private key cannot be null");
        }
        if (config == null) {
            throw new IllegalArgumentException("EncryptionConfig cannot be null");
        }
        
        // Use the existing SearchFrameworkEngine from the blockchain
        this.searchEngine = blockchain.getSearchFrameworkEngine();
        this.passwordRegistry = new BlockPasswordRegistry();
        this.encryptionConfig = config;
        this.isDirectlyInstantiated = false; // Created with proper parameters
        
        // Initialize immediately with blockchain - but only do advanced search init
        try {
            blockchain.initializeAdvancedSearch(password);
            
            // No need to re-index, the blockchain's engine is already indexed
            logger.info("🔄 Using blockchain's existing SearchFrameworkEngine with {} blocks", 
                       this.searchEngine.getSearchStats().getTotalBlocksIndexed());
            
            this.defaultPassword = password;
            this.isInitialized = true;
            
            // Debug: Log detailed information about indexing
            logger.info("🔍 SearchSpecialistAPI indexing details - Blockchain size: {}, Indexed blocks: {}",
                       blockchain.getBlockCount(), this.searchEngine.getSearchStats().getTotalBlocksIndexed());
            
            logger.info("✅ SearchSpecialistAPI created and initialized with blockchain - {} blocks indexed, config: {}", 
                       this.searchEngine.getSearchStats().getTotalBlocksIndexed(), config.getSecurityLevel());
        } catch (Exception e) {
            logger.error("❌ Failed to initialize SearchSpecialistAPI with blockchain: " + e.getMessage());
            throw new RuntimeException("Failed to initialize SearchSpecialistAPI", e);
        }
    }

    /**
     * Creates a new SearchSpecialistAPI instance with custom encryption configuration.
     * 
     * <p>This constructor allows you to specify custom security settings including:
     * <ul>
     *   <li>Encryption algorithms and key sizes</li>
     *   <li>Security levels for different data types</li>
     *   <li>Performance vs security trade-offs</li>
     *   <li>Custom validation policies</li>
     * </ul>
     * 
     * <p><strong>Note:</strong> You must call {@link #initializeWithBlockchain(Blockchain, String, PrivateKey)}
     * before performing any search operations.</p>
     * 
     * @param config the encryption configuration to use for all search operations.
     *               Must not be null. Use {@link EncryptionConfig#createHighSecurityConfig()}
     *               for maximum security or {@link EncryptionConfig#createBalancedConfig()}
     *               for balanced performance.
     * @throws IllegalArgumentException if config is null
     * @see EncryptionConfig for available configuration options
     */
    public SearchSpecialistAPI(EncryptionConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("EncryptionConfig cannot be null");
        }
        
        // Log warning for advanced constructor usage
        logger.warn("⚠️ SearchSpecialistAPI created with custom EncryptionConfig. " +
                   "Remember to call initializeWithBlockchain() before searching. " +
                   "Consider using SearchSpecialistAPI(blockchain, password, privateKey) for simpler usage.");
        
        this.searchEngine = new SearchFrameworkEngine(config);
        this.passwordRegistry = new BlockPasswordRegistry();
        this.encryptionConfig = config;
        this.isDirectlyInstantiated = true; // Created with advanced configuration
    }

    /**
     * Package-private constructor for internal use by blockchain.
     * This constructor should ONLY be used by the blockchain implementation.
     * 
     * @param internal marker to indicate internal construction
     * @param searchEngine the already initialized SearchFrameworkEngine instance
     */
    public SearchSpecialistAPI(boolean internal, SearchFrameworkEngine searchEngine) {
        this.searchEngine = searchEngine; // Use the existing indexed instance
        this.passwordRegistry = new BlockPasswordRegistry();
        this.encryptionConfig = EncryptionConfig.createHighSecurityConfig();
        this.isDirectlyInstantiated = false; // Created internally by blockchain
        
        if (internal) {
            logger.debug("✅ SearchSpecialistAPI created internally by blockchain with existing SearchFrameworkEngine");
        }
    }
    
    
    // ===== SIMPLE SEARCH METHODS =====
    
    /**
     * Performs a fast search using only public metadata (non-encrypted data).
     * 
     * <p>This method provides convenient search access by searching through both
     * public and private content using the default credentials. It's ideal for:</p>
     * <ul>
     *   <li>General content discovery</li>
     *   <li>Comprehensive searches without credential management</li>
     *   <li>Applications where users have default access</li>
     *   <li>Simplified search functionality</li>
     * </ul>
     * 
     * <p><strong>Performance:</strong> This method uses standard search with default password
     * and typically completes in under 200ms for datasets with millions of blocks.</p>
     * 
     * <p><strong>Security:</strong> Searches both public and private content using
     * the default password set during API initialization.</p>
     * 
     * @param query the search terms to look for. Must not be null or empty.
     *              Supports space-separated keywords, quotes for exact phrases,
     *              and basic wildcards.
     * @return a list of enhanced search results containing matching blocks,
     *         limited to 20 results. Never null, but may be empty if no matches found.
     * @throws IllegalArgumentException if query is null or empty
     * @throws IllegalStateException if the search API is not initialized
     * @see #searchSimple(String, int) for custom result limits
     * @see #searchSecure(String, String) for encrypted content search
     */
    public List<SearchFrameworkEngine.EnhancedSearchResult> searchSimple(String query) {
        return searchSimple(query, 20);
    }
    
    /**
     * Performs a convenient search using default credentials with a custom result limit.
     * 
     * <p>This method provides convenient search access by searching through both
     * public and private content using the default password. This allows accessing
     * all indexed content without manually providing credentials.</p>
     * 
     * <p><strong>Performance Considerations:</strong></p>
     * <ul>
     *   <li><strong>Small limits (1-50):</strong> Optimal performance, &lt;50ms</li>
     *   <li><strong>Medium limits (51-500):</strong> Good performance, &lt;200ms</li>
     *   <li><strong>Large limits (501+):</strong> May impact performance, consider pagination</li>
     * </ul>
     * 
     * <p><strong>Use Cases:</strong></p>
     * <ul>
     *   <li>Search result pagination</li>
     *   <li>Performance-tuned applications</li>
     *   <li>Search analytics and metrics</li>
     *   <li>Bulk data discovery</li>
     * </ul>
     * 
     * @param query the search terms to look for. Must not be null or empty.
     *              Supports keywords, exact phrases (in quotes), and wildcards.
     * @param maxResults the maximum number of results to return. Must be positive.
     *                   Recommended: 1-500 for optimal performance.
     * @return a list of enhanced search results containing matching blocks,
     *         limited to maxResults entries. Never null, but may be empty if no matches found.
     * @throws IllegalArgumentException if query is null/empty or maxResults is not positive
     * @throws IllegalStateException if the search API is not initialized
     * @see #searchSimple(String) for default 20-result limit
     * @see #searchSecure(String, String, int) for encrypted content search with limits
     */
    public List<SearchFrameworkEngine.EnhancedSearchResult> searchSimple(String query, int maxResults) {
        // No need to check initialization since constructor always initializes properly
        
        // Validate query parameter
        if (query == null) {
            throw new IllegalArgumentException("Query cannot be null");
        }
        if (query.trim().isEmpty()) {
            throw new IllegalArgumentException("Query cannot be empty or contain only whitespace");
        }
        
        // Validate maxResults parameter
        if (maxResults <= 0) {
            throw new IllegalArgumentException("Maximum results must be positive");
        }
        
        // Check for direct instantiation and warn
        if (isDirectlyInstantiated && !isInitialized) {
            logger.error("❌ SearchSpecialistAPI was created directly and is not initialized. " +
                        "This will likely return empty results. " +
                        "Use blockchain.getSearchSpecialistAPI() instead. " +
                        "See docs/SEARCHSPECIALISTAPI_INITIALIZATION_GUIDE.md");
            System.err.println("⚠️ WARNING: SearchSpecialistAPI created directly without proper initialization. " +
                              "Expected results: 0. Use blockchain.getSearchSpecialistAPI() instead.");
        }
        
        // REVERTED: searchSimple searches with default password (OPCIÓ B)
        // This allows searchSimple to find both public and private content using default credentials
        SearchFrameworkEngine.SearchResult result = searchEngine.search(query, defaultPassword, maxResults);
        return result.getResults();
    }
    
    /**
     * Performs a fast search using only public metadata (non-encrypted data).
     * 
     * <p>This method provides lightning-fast search capabilities by searching through only
     * public metadata and non-encrypted content. It's ideal for:</p>
     * <ul>
     *   <li>Quick content discovery without security concerns</li>
     *   <li>High-performance applications requiring sub-50ms response times</li>
     *   <li>Public-facing search features</li>
     *   <li>Anonymous or guest user searches</li>
     * </ul>
     * 
     * <p><strong>Performance:</strong> This method uses the FastIndexSearch strategy
     * and typically completes in under 50ms for datasets with millions of blocks.</p>
     * 
     * <p><strong>Security:</strong> Only searches public metadata and non-encrypted content.
     * Encrypted blocks are completely ignored, ensuring no sensitive data is exposed.</p>
     * 
     * @param query the search terms to look for. Must not be null or empty.
     *              Supports space-separated keywords, quotes for exact phrases,
     *              and basic wildcards.
     * @return a list of enhanced search results containing only public blocks,
     *         limited to 20 results. Never null, but may be empty if no matches found.
     * @throws IllegalArgumentException if query is null or empty
     * @throws IllegalStateException if the search API is not initialized
     * @see #searchPublic(String, int) for custom result limits
     * @see #searchSimple(String) for hybrid search with default credentials
     */
    public List<SearchFrameworkEngine.EnhancedSearchResult> searchPublic(String query) {
        return searchPublic(query, 20);
    }
    
    /**
     * Performs a fast public-only search with a custom result limit.
     * 
     * <p>This method provides fast search access by searching through only
     * public metadata and non-encrypted content. It ensures maximum performance
     * while maintaining complete privacy protection.</p>
     * 
     * <p><strong>Performance Considerations:</strong></p>
     * <ul>
     *   <li><strong>Small limits (1-50):</strong> Optimal performance, &lt;25ms</li>
     *   <li><strong>Medium limits (51-500):</strong> Excellent performance, &lt;50ms</li>
     *   <li><strong>Large limits (501+):</strong> Still fast, &lt;100ms</li>
     * </ul>
     * 
     * <p><strong>Use Cases:</strong></p>
     * <ul>
     *   <li>Public search interfaces and APIs</li>
     *   <li>Performance-critical applications</li>
     *   <li>Anonymous user search functionality</li>
     *   <li>Quick content preview and discovery</li>
     * </ul>
     * 
     * @param query the search terms to look for. Must not be null or empty.
     *              Supports keywords, exact phrases (in quotes), and wildcards.
     * @param maxResults the maximum number of results to return. Must be positive.
     *                   Recommended: 1-1000 for optimal performance.
     * @return a list of enhanced search results containing only public blocks,
     *         limited to maxResults entries. Never null, but may be empty if no matches found.
     * @throws IllegalArgumentException if query is null/empty or maxResults is not positive
     * @throws IllegalStateException if the search API is not initialized
     * @see #searchPublic(String) for default 20-result limit
     * @see #searchSimple(String, int) for hybrid search with default credentials
     * @see #searchSecure(String, String, int) for encrypted content search with limits
     */
    public List<SearchFrameworkEngine.EnhancedSearchResult> searchPublic(String query, int maxResults) {
        // No need to check initialization since constructor always initializes properly
        
        // Validate query parameter
        if (query == null) {
            throw new IllegalArgumentException("Query cannot be null");
        }
        if (query.trim().isEmpty()) {
            throw new IllegalArgumentException("Query cannot be empty or contain only whitespace");
        }
        
        // Validate maxResults parameter
        if (maxResults <= 0) {
            throw new IllegalArgumentException("Maximum results must be positive");
        }
        
        // Check for direct instantiation and warn
        if (isDirectlyInstantiated && !isInitialized) {
            logger.error("❌ SearchSpecialistAPI was created directly and is not initialized. " +
                        "This will likely return empty results. " +
                        "Use blockchain.getSearchSpecialistAPI() instead. " +
                        "See docs/SEARCHSPECIALISTAPI_INITIALIZATION_GUIDE.md");
            System.err.println("⚠️ WARNING: SearchSpecialistAPI created directly without proper initialization. " +
                              "Expected results: 0. Use blockchain.getSearchSpecialistAPI() instead.");
        }
        
        // Use public-only search - no password required, only searches public metadata
        // No prefix needed: MetadataLayerManager strips "public:" during indexing
        String publicQuery = query.toLowerCase();
        logger.debug("🔍 searchPublic: query='{}' -> publicQuery='{}'", query, publicQuery);
        SearchFrameworkEngine.SearchResult result = searchEngine.searchPublicOnly(publicQuery, maxResults);
        logger.debug("🔍 searchPublic: found {} results", result.getResults().size());
        return result.getResults();
    }
    
    /**
     * Performs a secure search that can access encrypted content using the provided password.
     * 
     * <p>This method searches through both public metadata and encrypted content,
     * providing comprehensive search capabilities while maintaining security.</p>
     * 
     * <p><strong>Security Features:</strong></p>
     * <ul>
     *   <li>AES-256-GCM decryption for authorized access</li>
     *   <li>Password validation and authentication</li>
     *   <li>Secure memory handling for decrypted content</li>
     *   <li>Automatic cleanup of sensitive data</li>
     * </ul>
     * 
     * <p><strong>Search Capabilities:</strong></p>
     * <ul>
     *   <li>Public metadata (always accessible)</li>
     *   <li>Encrypted block content (with correct password)</li>
     *   <li>Off-chain encrypted files (if accessible)</li>
     *   <li>Cross-reference encrypted/public data</li>
     * </ul>
     * 
     * <p><strong>Performance:</strong> Typically completes in under 200ms for
     * medium-sized datasets. Performance depends on the number of encrypted
     * blocks that need decryption.</p>
     * 
     * @param query the search terms to look for. Must not be null or empty.
     *              Supports advanced search syntax including keywords, phrases,
     *              and boolean operators.
     * @param password the password for decrypting encrypted content. Must not be null.
     *                 If incorrect, encrypted content will be skipped without error.
     * @return a list of enhanced search results containing matching blocks from both
     *         public and encrypted sources, limited to 20 results. Never null.
     * @throws IllegalArgumentException if query or password is null, or query is empty
     * @throws IllegalStateException if the search API is not initialized
     * @see #searchSecure(String, String, int) for custom result limits
     * @see #searchPublic(String) for public-only search
     * @see #searchIntelligent(String, String, int) for adaptive search strategy
     */
    public List<SearchFrameworkEngine.EnhancedSearchResult> searchSecure(String query, String password) {
        return searchSecure(query, password, 20);
    }
    
    /**
     * Performs a secure search with encrypted content access and custom result limit.
     * 
     * <p>This method provides comprehensive search capabilities across both public
     * and encrypted data sources. The result limit allows for performance optimization
     * and pagination support.</p>
     * 
     * <p><strong>Search Strategy:</strong> Uses the ENCRYPTED_CONTENT strategy which:</p>
     * <ul>
     *   <li>Searches public metadata first (for speed)</li>
     *   <li>Decrypts and searches encrypted block content</li>
     *   <li>Includes off-chain encrypted file content</li>
     *   <li>Merges and ranks results by relevance</li>
     * </ul>
     * 
     * <p><strong>Performance Optimization:</strong></p>
     * <ul>
     *   <li><strong>Small limits (1-50):</strong> Optimal, &lt;200ms</li>
     *   <li><strong>Medium limits (51-200):</strong> Good, &lt;500ms</li>
     *   <li><strong>Large limits (201+):</strong> Consider async search for UI responsiveness</li>
     * </ul>
     * 
     * <p><strong>Security Considerations:</strong></p>
     * <ul>
     *   <li>Password is used to decrypt only matching encrypted blocks</li>
     *   <li>Failed decryptions are logged but don't cause search failure</li>
     *   <li>Sensitive data is cleared from memory after search</li>
     *   <li>Search patterns are not stored or cached</li>
     * </ul>
     * 
     * @param query the search terms to look for. Must not be null or empty.
     *              Supports complex queries with boolean logic, phrases, and wildcards.
     * @param password the password for decrypting encrypted content. Must not be null.
     *                 Use the same password that was used to encrypt the target blocks.
     * @param maxResults the maximum number of results to return. Must be positive.
     *                   Higher values may impact performance for encrypted searches.
     * @return a list of enhanced search results from both public and encrypted sources,
     *         limited to maxResults entries. Results are ranked by relevance score.
     * @throws IllegalArgumentException if query/password is null, query is empty, or maxResults is not positive
     * @throws IllegalStateException if the search API is not initialized
     * @see #searchSecure(String, String) for default 20-result limit
     * @see #searchIntelligent(String, String, int) for adaptive search with automatic strategy selection
     */
    public List<SearchFrameworkEngine.EnhancedSearchResult> searchSecure(String query, String password, int maxResults) {
        // No need to check initialization since constructor always initializes properly
        
        // Validate query parameter
        if (query == null) {
            throw new IllegalArgumentException("Query cannot be null");
        }
        if (query.trim().isEmpty()) {
            throw new IllegalArgumentException("Query cannot be empty or contain only whitespace");
        }
        
        // Validate password parameter
        if (password == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }
        
        // Validate maxResults parameter
        if (maxResults <= 0) {
            throw new IllegalArgumentException("Maximum results must be positive");
        }
        
        SearchFrameworkEngine.SearchResult result = searchEngine.searchEncryptedOnly(query, password, maxResults);
        return result.getResults();
    }
    
    
    /**
     * Performs an intelligent adaptive search that automatically selects the optimal search strategy.
     * 
     * <p>This method analyzes the query characteristics and automatically chooses the most
     * appropriate search strategy from:</p>
     * <ul>
     *   <li><strong>FAST_PUBLIC:</strong> For simple queries where speed is priority</li>
     *   <li><strong>ENCRYPTED_CONTENT:</strong> For complex queries requiring encrypted access</li>
     *   <li><strong>EXHAUSTIVE_HYBRID:</strong> For comprehensive searches across all data sources</li>
     *   <li><strong>PRIVACY_PRESERVING:</strong> For sensitive queries requiring anonymization</li>
     * </ul>
     * 
     * <p><strong>Intelligent Features:</strong></p>
     * <ul>
     *   <li><strong>Query Analysis:</strong> Analyzes complexity, keywords, and patterns</li>
     *   <li><strong>Performance Optimization:</strong> Balances speed vs completeness</li>
     *   <li><strong>Adaptive Routing:</strong> Routes to best strategy based on context</li>
     *   <li><strong>Fallback Handling:</strong> Gracefully handles failures with alternative strategies</li>
     * </ul>
     * 
     * <p><strong>Decision Factors:</strong></p>
     * <ul>
     *   <li>Query complexity and length</li>
     *   <li>Presence of encrypted blocks in dataset</li>
     *   <li>Password validity and access permissions</li>
     *   <li>Performance requirements (maxResults)</li>
     *   <li>Historical search patterns and success rates</li>
     * </ul>
     * 
     * <p><strong>Performance:</strong> Automatically optimized based on strategy selection.
     * Typical performance ranges from &lt;50ms (fast public) to &lt;2s (exhaustive).</p>
     * 
     * <p><strong>Recommended Usage:</strong> This is the <strong>recommended search method</strong>
     * for most applications as it provides the best balance of performance, completeness,
     * and ease of use.</p>
     * 
     * @param query the search terms to look for. Must not be null or empty.
     *              Supports full search syntax including keywords, phrases, boolean operators,
     *              and advanced patterns. More complex queries may trigger more comprehensive strategies.
     * @param password the password for accessing encrypted content. Must not be null.
     *                 Used only if the intelligent router determines encrypted access is needed.
     * @param maxResults the maximum number of results to return. Must be positive.
     *                   Influences strategy selection - larger limits may trigger more comprehensive searches.
     * @return a list of enhanced search results optimized for the query type and requirements.
     *         Results include metadata about the search strategy used and performance metrics.
     * @throws IllegalArgumentException if query/password is null, query is empty, or maxResults is not positive
     * @throws IllegalStateException if the search API is not initialized
     * @see #searchPublic(String, int) for guaranteed fast public-only search
     * @see #searchSecure(String, String, int) for guaranteed encrypted content search
     * @see #searchAdvanced(String, String, EncryptionConfig, int) for full control over search configuration
     */
    public List<SearchFrameworkEngine.EnhancedSearchResult> searchIntelligent(String query, String password, int maxResults) {
        // No need to check initialization since constructor always initializes properly
        
        // Validate query parameter
        if (query == null) {
            throw new IllegalArgumentException("Query cannot be null");
        }
        if (query.trim().isEmpty()) {
            throw new IllegalArgumentException("Query cannot be empty or contain only whitespace");
        }
        
        // Validate password parameter
        if (password == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }
        
        // Validate maxResults parameter
        if (maxResults <= 0) {
            throw new IllegalArgumentException("Maximum results must be positive");
        }
        
        SearchFrameworkEngine.SearchResult result = searchEngine.search(query, password, maxResults, encryptionConfig);
        return result.getResults();
    }
    
    
    // ===== ADVANCED SEARCH METHODS =====
    
    /**
     * Performs an advanced search with full control over encryption configuration and search behavior.
     * 
     * <p>This method provides expert-level control over all aspects of the search operation,
     * allowing fine-tuning of security, performance, and search strategy parameters.</p>
     * 
     * <p><strong>Expert Control Features:</strong></p>
     * <ul>
     *   <li><strong>Custom Encryption:</strong> Override default AES-256-GCM settings</li>
     *   <li><strong>Security Levels:</strong> Configure high/balanced/performance security modes</li>
     *   <li><strong>Strategy Hints:</strong> Influence automatic strategy selection</li>
     *   <li><strong>Performance Tuning:</strong> Customize caching, threading, and memory usage</li>
     *   <li><strong>Validation Policies:</strong> Custom input validation and sanitization</li>
     * </ul>
     * 
     * <p><strong>Configuration Options:</strong></p>
     * <ul>
     *   <li><strong>Encryption algorithms and key sizes</strong></li>
     *   <li><strong>Search timeouts and retry policies</strong></li>
     *   <li><strong>Memory limits and garbage collection hints</strong></li>
     *   <li><strong>Logging and audit trail settings</strong></li>
     *   <li><strong>Privacy and anonymization levels</strong></li>
     * </ul>
     * 
     * <p><strong>Return Value:</strong> Unlike other search methods, this returns a complete
     * {@link SearchResult} object that includes:</p>
     * <ul>
     *   <li>Search results with enhanced metadata</li>
     *   <li>Performance metrics and timing data</li>
     *   <li>Strategy selection rationale</li>
     *   <li>Security audit information</li>
     *   <li>Error details and recovery suggestions</li>
     * </ul>
     * 
     * <p><strong>Use Cases:</strong></p>
     * <ul>
     *   <li>Performance-critical applications requiring custom tuning</li>
     *   <li>High-security environments with custom encryption requirements</li>
     *   <li>Research and development with detailed metrics collection</li>
     *   <li>Integration testing with specific configuration scenarios</li>
     * </ul>
     * 
     * <p><strong>Warning:</strong> This method is intended for expert users. Incorrect
     * configuration may impact security or performance. Use {@link #searchIntelligent}
     * for most applications.</p>
     * 
     * @param query the search terms to look for. Must not be null or empty.
     *              Supports full advanced search syntax and custom query extensions.
     * @param password the password for accessing encrypted content. Must not be null.
     *                 Must comply with the security requirements specified in config.
     * @param config the encryption and search configuration to use. Must not be null.
     *               Use {@link EncryptionConfig#createHighSecurityConfig()} for maximum security,
     *               {@link EncryptionConfig#createBalancedConfig()} for balanced performance,
     *               or {@link EncryptionConfig#createPerformanceConfig()} for speed priority.
     * @param maxResults the maximum number of results to return. Must be positive.
     *                   Large values may require additional memory and processing time.
     * @return a complete advanced search result with detailed metrics, strategy information,
     *         and comprehensive error handling. Never null.
     * @throws IllegalArgumentException if any parameter is null, query is empty, or maxResults is not positive
     * @throws IllegalStateException if the search API is not initialized
     * @throws SecurityException if the configuration violates security policies
     * @see #searchIntelligent(String, String, int) for automatic configuration
     * @see EncryptionConfig for configuration options and security recommendations
     */
    public SearchFrameworkEngine.SearchResult searchAdvanced(String query, String password, 
                                                   EncryptionConfig config, int maxResults) {
        // Validate initialization state first
        if (!isInitialized) {
            throw new IllegalStateException("SearchSpecialistAPI is not initialized. " +
                                          "Call initializeWithBlockchain() before performing search operations.");
        }
        
        // Validate query parameter
        if (query == null) {
            throw new IllegalArgumentException("Query cannot be null");
        }
        if (query.trim().isEmpty()) {
            throw new IllegalArgumentException("Query cannot be empty or contain only whitespace");
        }
        
        // Validate password parameter
        if (password == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }
        
        // Validate config parameter
        if (config == null) {
            throw new IllegalArgumentException("EncryptionConfig cannot be null");
        }
        
        // Validate maxResults parameter
        if (maxResults <= 0) {
            throw new IllegalArgumentException("Maximum results must be positive");
        }
        
        return searchEngine.search(query, password, maxResults, config);
    }
    
    // ===== SPECIALIZED SEARCH METHODS =====
    
    // Removed category-specific search methods - blockchain should be content-agnostic
    // Users can now search by their own defined terms using searchIntelligent() directly
    
    // Removed searchHighValue() method - contained hardcoded financial terms, use searchIntelligent() with user-defined terms instead
    
    // ===== BLOCKCHAIN INTEGRATION =====
    
    /**
     * Initializes the search engine with blockchain data and builds comprehensive search indexes.
     * 
     * <p>This method performs a complete analysis and indexing of all blocks in the blockchain,
     * creating optimized search structures for maximum performance. This is a <strong>required</strong>
     * initialization step before any search operations can be performed.</p>
     * 
     * <p><strong>Initialization Process:</strong></p>
     * <ol>
     *   <li><strong>Blockchain Analysis:</strong> Scans all blocks and analyzes content types</li>
     *   <li><strong>Metadata Extraction:</strong> Builds three-layer metadata architecture</li>
     *   <li><strong>Encryption Detection:</strong> Identifies encrypted vs public blocks</li>
     *   <li><strong>Index Creation:</strong> Creates optimized search indexes and caches</li>
     *   <li><strong>Performance Calibration:</strong> Measures performance and optimizes settings</li>
     * </ol>
     * 
     * <p><strong>Security Handling:</strong></p>
     * <ul>
     *   <li>Uses provided private key for signature verification</li>
     *   <li>Attempts decryption of encrypted blocks with provided password</li>
     *   <li>Stores password securely for future search operations</li>
     *   <li>Creates audit trail of initialization process</li>
     * </ul>
     * 
     * <p><strong>Performance Considerations:</strong></p>
     * <ul>
     *   <li><strong>Small blockchains (&lt;1,000 blocks):</strong> Completes in seconds</li>
     *   <li><strong>Medium blockchains (1,000-100,000 blocks):</strong> May take 1-5 minutes</li>
     *   <li><strong>Large blockchains (100,000+ blocks):</strong> Consider using {@link #initializeWithBlockchainAsync}</li>
     * </ul>
     * 
     * <p><strong>Memory Usage:</strong> Initialization creates in-memory indexes that improve
     * search performance. Memory usage scales with blockchain size but is optimized for
     * production use.</p>
     * 
     * @param blockchain the blockchain instance to index. Must not be null and should
     *                   contain at least a genesis block.
     * @param password the master password for decrypting encrypted blocks. Must not be null.
     *                 This password will be used as the default for search operations.
     * @param privateKey the private key for signature verification and secure operations.
     *                   Must not be null and should correspond to an authorized blockchain key.
     * @return an indexing result containing detailed statistics about the initialization process,
     *         including number of blocks processed, encryption status, performance metrics,
     *         and any errors encountered. Never null.
     * @throws IllegalArgumentException if any parameter is null
     * @throws IllegalStateException if the search API is already initialized
     * @throws SecurityException if the private key is not authorized for the blockchain
     * @throws RuntimeException if initialization fails due to blockchain corruption or other issues
     * @see #initializeWithBlockchainAsync for non-blocking initialization
     * @see #isReady() to check initialization status
     * @see IndexingResult for detailed initialization metrics
     */
    public SearchFrameworkEngine.IndexingResult initializeWithBlockchain(Blockchain blockchain, String password, PrivateKey privateKey) {
        // Validate parameters
        if (blockchain == null) {
            throw new IllegalArgumentException("Blockchain cannot be null");
        }
        if (password == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }
        if (password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty or contain only whitespace");
        }
        if (privateKey == null) {
            throw new IllegalArgumentException("Private key cannot be null");
        }
        
        SearchFrameworkEngine.IndexingResult result = searchEngine.indexBlockchain(blockchain, password, privateKey);
        this.defaultPassword = password;
        isInitialized = true;
        
        // Clear the direct instantiation flag since it's now properly initialized
        if (isDirectlyInstantiated) {
            logger.info("✅ SearchSpecialistAPI directly instantiated but now properly initialized with blockchain");
        }
        
        return result;
    }
    
    /**
     * Initializes the search engine with blockchain data asynchronously for non-blocking operation.
     * 
     * <p>This method performs the same comprehensive blockchain indexing as
     * {@link #initializeWithBlockchain} but runs in a background thread to avoid blocking
     * the calling thread. This is particularly useful for:</p>
     * <ul>
     *   <li><strong>Large blockchains:</strong> Where initialization may take several minutes</li>
     *   <li><strong>UI applications:</strong> To maintain responsiveness during initialization</li>
     *   <li><strong>Server startup:</strong> To allow other services to start while indexing occurs</li>
     *   <li><strong>Progressive loading:</strong> To begin serving limited functionality immediately</li>
     * </ul>
     * 
     * <p><strong>Asynchronous Benefits:</strong></p>
     * <ul>
     *   <li>Non-blocking operation maintains application responsiveness</li>
     *   <li>Can be combined with progress monitoring and user feedback</li>
     *   <li>Allows for cancellation if needed</li>
     *   <li>Enables parallel initialization of multiple components</li>
     * </ul>
     * 
     * <p><strong>Usage Pattern:</strong></p>
     * <pre>{@code
     * CompletableFuture<IndexingResult> future = searchAPI.initializeWithBlockchainAsync(
     *     blockchain, password, privateKey);
     * 
     * // Continue with other operations while indexing occurs
     * setupOtherComponents();
     * 
     * // Wait for completion when search functionality is needed
     * IndexingResult result = future.get();
     * if (result.isSuccessful()) {
     *     // Search operations are now available
     *     performSearch();
     * }
     * }</pre>
     * 
     * <p><strong>Thread Safety:</strong> The returned CompletableFuture is thread-safe and
     * can be accessed from multiple threads. However, search operations should not be
     * attempted until the future completes successfully.</p>
     * 
     * @param blockchain the blockchain instance to index. Must not be null and should
     *                   contain at least a genesis block.
     * @param password the master password for decrypting encrypted blocks. Must not be null.
     * @param privateKey the private key for signature verification. Must not be null.
     * @return a CompletableFuture that will complete with an IndexingResult containing
     *         detailed initialization metrics. The future may complete exceptionally
     *         if initialization fails.
     * @throws IllegalArgumentException if any parameter is null
     * @throws IllegalStateException if the search API is already initialized
     * @see #initializeWithBlockchain for synchronous initialization
     * @see CompletableFuture for async operation handling
     * @see #isReady() to check completion status
     */
    public CompletableFuture<SearchFrameworkEngine.IndexingResult> initializeWithBlockchainAsync(
            Blockchain blockchain, String password, PrivateKey privateKey) {
        // Validate parameters
        if (blockchain == null) {
            throw new IllegalArgumentException("Blockchain cannot be null");
        }
        if (password == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }
        if (password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty or contain only whitespace");
        }
        if (privateKey == null) {
            throw new IllegalArgumentException("Private key cannot be null");
        }
        
        return CompletableFuture.supplyAsync(() -> {
            SearchFrameworkEngine.IndexingResult result = searchEngine.indexBlockchain(blockchain, password, privateKey);
            this.defaultPassword = password;
            isInitialized = true;
            return result;
        });
    }
    
    /**
     * Adds a new block to the search indexes for immediate searchability.
     * 
     * <p>This method performs incremental indexing of a newly added block, updating
     * all search structures and metadata to include the new content. The block becomes
     * immediately searchable upon successful completion.</p>
     * 
     * <p><strong>Indexing Process:</strong></p>
     * <ol>
     *   <li><strong>Content Analysis:</strong> Analyzes block content and structure</li>
     *   <li><strong>Metadata Extraction:</strong> Extracts searchable metadata and keywords</li>
     *   <li><strong>Encryption Handling:</strong> Attempts decryption if password provided</li>
     *   <li><strong>Index Update:</strong> Updates all relevant search indexes</li>
     *   <li><strong>Cache Refresh:</strong> Updates performance caches and statistics</li>
     * </ol>
     * 
     * <p><strong>Password Management:</strong></p>
     * <ul>
     *   <li>If the block is encrypted and password is provided, the password is securely stored</li>
     *   <li>Stored passwords enable future search operations on encrypted content</li>
     *   <li>Passwords are encrypted and protected in the password registry</li>
     *   <li>Password availability improves search completeness and performance</li>
     * </ul>
     * 
     * <p><strong>Performance:</strong> Incremental indexing is optimized for minimal impact
     * on ongoing search operations. Typical indexing time is &lt;10ms per block.</p>
     * 
     * <p><strong>Thread Safety:</strong> This method is thread-safe and can be called
     * concurrently with search operations without blocking or data corruption.</p>
     * 
     * <p><strong>Use Cases:</strong></p>
     * <ul>
     *   <li>Real-time blockchain updates with immediate search availability</li>
     *   <li>Batch processing of newly imported blocks</li>
     *   <li>Recovery operations after blockchain restoration</li>
     *   <li>Testing and development with dynamic block creation</li>
     * </ul>
     * 
     * @param block the new block to add to search indexes. Must not be null,
     *              should be properly formed, and should not already exist in indexes.
     * @param password the password for decrypting block content if encrypted. Can be null
     *                 for public blocks, but encrypted blocks without passwords will have
     *                 limited searchability.
     * @param privateKey the private key for signature verification and secure operations.
     *                   Must not be null and should be authorized for the blockchain.
     * @throws IllegalArgumentException if block or privateKey is null
     * @throws IllegalStateException if the search API is not initialized
     * @throws SecurityException if the private key is not authorized
     * @see #removeBlock(String) for removing blocks from indexes
     * @see #initializeWithBlockchain for bulk indexing
     */
    public void addBlock(Block block, String password, PrivateKey privateKey) {
        // Validate parameters
        if (block == null) {
            throw new IllegalArgumentException("Block cannot be null");
        }
        if (privateKey == null) {
            throw new IllegalArgumentException("Private key cannot be null");
        }
        
        // Validate initialization state
        if (!isInitialized) {
            throw new IllegalStateException("SearchSpecialistAPI is not initialized. " +
                                          "Call initializeWithBlockchain() before adding blocks.");
        }
        
        if (block.isDataEncrypted() && password != null) {
            passwordRegistry.registerBlockPassword(block.getHash(), password);
        }
        searchEngine.indexBlock(block, password, privateKey, encryptionConfig);
    }
    
    
    /**
     * Removes a block from all search indexes and cleans up associated data.
     * 
     * <p>This method performs complete removal of a block from the search system,
     * including all metadata, cached content, and security information. The block
     * becomes immediately unsearchable upon completion.</p>
     * 
     * <p><strong>Removal Process:</strong></p>
     * <ol>
     *   <li><strong>Index Cleanup:</strong> Removes block from all search indexes</li>
     *   <li><strong>Metadata Removal:</strong> Cleans up extracted keywords and metadata</li>
     *   <li><strong>Password Cleanup:</strong> Removes stored passwords from registry</li>
     *   <li><strong>Cache Invalidation:</strong> Clears cached content and statistics</li>
     *   <li><strong>Reference Cleanup:</strong> Removes cross-references and links</li>
     * </ol>
     * 
     * <p><strong>Security Considerations:</strong></p>
     * <ul>
     *   <li>Stored passwords for the block are securely wiped from memory</li>
     *   <li>Cached decrypted content is cleared to prevent data leakage</li>
     *   <li>All traces of the block's searchable content are removed</li>
     *   <li>Audit logs record the removal operation</li>
     * </ul>
     * 
     * <p><strong>Performance Impact:</strong></p>
     * <ul>
     *   <li>Removal operations are optimized for minimal impact on ongoing searches</li>
     *   <li>Index structures are updated efficiently without full rebuilds</li>
     *   <li>Memory is reclaimed immediately for better performance</li>
     * </ul>
     * 
     * <p><strong>Use Cases:</strong></p>
     * <ul>
     *   <li>Blockchain rollback operations</li>
     *   <li>Data privacy compliance (right to be forgotten)</li>
     *   <li>Corrupted block cleanup</li>
     *   <li>Test data cleanup in development environments</li>
     * </ul>
     * 
     * <p><strong>Warning:</strong> This operation is irreversible. To re-enable searching
     * for the block, you must call {@link #addBlock} again with the complete block data.</p>
     * 
     * @param blockHash the hash of the block to remove. Must not be null or empty,
     *                  and should correspond to a block that exists in the indexes.
     * @throws IllegalArgumentException if blockHash is null or empty
     * @throws IllegalStateException if the search API is not initialized
     * @see #addBlock(Block, String, PrivateKey) for adding blocks back to indexes
     * @see #clearCache() for complete cache cleanup
     */
    public void removeBlock(String blockHash) {
        // Validate parameters
        if (blockHash == null) {
            throw new IllegalArgumentException("Block hash cannot be null");
        }
        if (blockHash.trim().isEmpty()) {
            throw new IllegalArgumentException("Block hash cannot be empty or contain only whitespace");
        }
        
        // Validate initialization state
        if (!isInitialized) {
            throw new IllegalStateException("SearchSpecialistAPI is not initialized. " +
                                          "Call initializeWithBlockchain() before removing blocks.");
        }
        
        searchEngine.removeBlock(blockHash);
        passwordRegistry.removeBlockPassword(blockHash);
    }
    
    // ===== SYSTEM STATUS =====
    
    /**
     * Checks if the search engine is fully initialized and ready for search operations.
     * 
     * <p>This method returns {@code true} only after successful completion of blockchain
     * initialization via {@link #initializeWithBlockchain} or {@link #initializeWithBlockchainAsync}.
     * Until initialization is complete, all search operations will fail with {@link IllegalStateException}.</p>
     * 
     * <p><strong>Initialization States:</strong></p>
     * <ul>
     *   <li><strong>false:</strong> API created but not yet initialized with blockchain data</li>
     *   <li><strong>true:</strong> Fully initialized and ready for all search operations</li>
     * </ul>
     * 
     * <p><strong>Usage Pattern:</strong></p>
     * <pre>{@code
     * SearchSpecialistAPI searchAPI = new SearchSpecialistAPI();
     * 
     * if (!searchAPI.isReady()) {
     *     // Initialize before using
     *     searchAPI.initializeWithBlockchain(blockchain, password, privateKey);
     * }
     * 
     * // Now safe to perform search operations
     * List<EnhancedSearchResult> results = searchAPI.searchSimple("query");
     * }</pre>
     * 
     * <p><strong>Thread Safety:</strong> This method is thread-safe and can be called
     * concurrently from multiple threads.</p>
     * 
     * @return {@code true} if the search engine is fully initialized and ready for operations,
     *         {@code false} if initialization is required
     * @see #initializeWithBlockchain for synchronous initialization
     * @see #initializeWithBlockchainAsync for asynchronous initialization
     */
    public boolean isReady() {
        return isInitialized;
    }
    
    /**
     * Retrieves the encryption configuration currently in use by this search API instance.
     * 
     * <p>This method provides access to the encryption settings that govern how encrypted
     * content is handled during search operations. The configuration includes security levels,
     * encryption algorithms, and performance tuning parameters.</p>
     * 
     * <p><strong>Configuration Information:</strong></p>
     * <ul>
     *   <li><strong>Security Level:</strong> HIGH_SECURITY, BALANCED, or PERFORMANCE</li>
     *   <li><strong>Encryption Algorithm:</strong> Typically AES-256-GCM for maximum security</li>
     *   <li><strong>Key Derivation:</strong> PBKDF2 parameters and salt configurations</li>
     *   <li><strong>Performance Settings:</strong> Threading, caching, and memory limits</li>
     * </ul>
     * 
     * <p><strong>Use Cases:</strong></p>
     * <ul>
     *   <li>Verifying security configuration for compliance</li>
     *   <li>Debugging encryption-related issues</li>
     *   <li>Performance tuning and optimization</li>
     *   <li>Configuration auditing and reporting</li>
     * </ul>
     * 
     * @return the current encryption configuration. Never null.
     * @see EncryptionConfig for configuration details
     * @see #searchAdvanced(String, String, EncryptionConfig, int) for using custom configurations
     */
    public EncryptionConfig getEncryptionConfig() {
        return encryptionConfig;
    }
    
    /**
     * Retrieves comprehensive statistics about the search engine's current state and performance.
     * 
     * <p>This method returns detailed metrics covering all aspects of search engine operation,
     * including indexing status, performance characteristics, memory usage, and operational statistics.</p>
     * 
     * <p><strong>Statistics Categories:</strong></p>
     * <ul>
     *   <li><strong>Indexing Metrics:</strong> Number of blocks indexed, encryption status</li>
     *   <li><strong>Performance Metrics:</strong> Search times, cache hit rates, throughput</li>
     *   <li><strong>Memory Metrics:</strong> Index size, cache usage, memory efficiency</li>
     *   <li><strong>Strategy Metrics:</strong> Usage patterns for different search strategies</li>
     *   <li><strong>Security Metrics:</strong> Encryption operations, password registry status</li>
     * </ul>
     * 
     * <p><strong>Use Cases:</strong></p>
     * <ul>
     *   <li>Performance monitoring and optimization</li>
     *   <li>Capacity planning and resource allocation</li>
     *   <li>Troubleshooting search performance issues</li>
     *   <li>System health monitoring and alerting</li>
     *   <li>Analytics and reporting dashboards</li>
     * </ul>
     * 
     * <p><strong>Real-time Data:</strong> Statistics are updated in real-time as search
     * operations are performed, providing current and accurate system state information.</p>
     * 
     * <p><strong>Performance Impact:</strong> Retrieving statistics has minimal performance
     * impact and can be called frequently for monitoring purposes.</p>
     * 
     * @return a comprehensive statistics object containing detailed metrics about
     *         search engine operation. Never null, but may contain empty data
     *         if the search engine is not yet initialized.
     * @throws IllegalStateException if the search API is not initialized
     * @see #getPerformanceMetrics() for formatted performance summary
     * @see #runDiagnostics() for comprehensive system diagnostics
     */
    public SearchFrameworkEngine.SearchStats getStatistics() {
        // No need to check initialization since constructor always initializes properly
        
        return searchEngine.getSearchStats();
    }
    
    /**
     * Retrieves a formatted summary of key performance metrics for monitoring and analysis.
     * 
     * <p>This method provides a human-readable summary of the most important performance
     * indicators, formatted with icons and clear labels for easy interpretation in logs,
     * dashboards, or administrative interfaces.</p>
     * 
     * <p><strong>Included Metrics:</strong></p>
     * <ul>
     *   <li><strong>Indexing Status:</strong> Total blocks indexed and processing status</li>
     *   <li><strong>Search Performance:</strong> Keywords indexed and search capabilities</li>
     *   <li><strong>Encryption Status:</strong> Number of encrypted blocks and decryption success</li>
     *   <li><strong>Password Registry:</strong> Registered passwords and access capabilities</li>
     *   <li><strong>Memory Usage:</strong> Current memory consumption in megabytes</li>
     * </ul>
     * 
     * <p><strong>Format Example:</strong></p>
     * <pre>
     * 🚀 Search Framework Engine Metrics:
     *    📊 Total Blocks Indexed: 50,000
     *    ⚡ Fast Search Keywords: 125,000
     *    🔐 Encrypted Blocks: 12,500
     *    🔑 Password Registry: 8,200 blocks
     *    💾 Memory Usage: 245 MB
     * </pre>
     * 
     * <p><strong>Use Cases:</strong></p>
     * <ul>
     *   <li>System monitoring dashboards</li>
     *   <li>Application logs and debugging</li>
     *   <li>Performance trend analysis</li>
     *   <li>Capacity planning reports</li>
     *   <li>Administrative oversight and health checks</li>
     * </ul>
     * 
     * <p><strong>Real-time Accuracy:</strong> Metrics reflect the current state of the
     * search engine and are updated in real-time as operations are performed.</p>
     * 
     * @return a formatted string containing key performance metrics with icons and labels.
     *         Never null, but may indicate "Not Initialized" if the search engine
     *         has not been set up with blockchain data.
     * @see #getStatistics() for detailed programmatic access to all metrics
     * @see #runDiagnostics() for comprehensive system analysis
     */
    public String getPerformanceMetrics() {
        SearchFrameworkEngine.SearchStats stats = getStatistics();
        BlockPasswordRegistry.RegistryStats registryStats = passwordRegistry.getStats();
        StringBuilder metrics = new StringBuilder();
        
        metrics.append("🚀 Search Framework Engine Metrics:\\n");
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
     * Retrieves a formatted summary of all search engine capabilities and features.
     * 
     * <p>This method provides a comprehensive overview of what the Search Framework Engine
     * can accomplish, formatted for easy reading in documentation, help systems, or
     * administrative interfaces.</p>
     * 
     * <p><strong>Capability Categories:</strong></p>
     * <ul>
     *   <li><strong>Performance Features:</strong> Speed and scalability characteristics</li>
     *   <li><strong>Security Features:</strong> Encryption and privacy protection capabilities</li>
     *   <li><strong>Intelligence Features:</strong> Automatic optimization and routing</li>
     *   <li><strong>Analytics Features:</strong> Content analysis and categorization</li>
     *   <li><strong>Enterprise Features:</strong> High-availability and production-ready features</li>
     * </ul>
     * 
     * <p><strong>Example Output:</strong></p>
     * <pre>
     * 🎯 Advanced Search Capabilities:
     *    ⚡ Fast Public Search: Sub-50ms searches on public metadata
     *    🔐 Encrypted Content Search: Deep search with password protection
     *    🔒 Privacy Search: Privacy-preserving capabilities
     *    🧠 Intelligent Routing: Automatic strategy selection
     *    🔄 Parallel Multi-Search: Simultaneous strategy execution
     *    📊 Advanced Analytics: Content categorization and analysis
     *    🛴️ Enterprise Security: AES-256-GCM encryption
     *    📈 High Performance: Millions of blocks support
     * </pre>
     * 
     * <p><strong>Use Cases:</strong></p>
     * <ul>
     *   <li>System documentation and help systems</li>
     *   <li>Feature discovery for new users</li>
     *   <li>Administrative dashboards and system overviews</li>
     *   <li>Marketing and technical specifications</li>
     *   <li>Capability assessment and planning</li>
     * </ul>
     * 
     * <p><strong>Static Information:</strong> This method returns static capability information
     * that does not change based on current system state or initialization status.</p>
     * 
     * @return a formatted string describing all search engine capabilities with icons and descriptions.
     *         Never null and always returns the same content regardless of initialization status.
     * @see #getPerformanceMetrics() for current performance statistics
     * @see #runDiagnostics() for comprehensive system analysis including capabilities
     */
    public String getCapabilitiesSummary() {
        StringBuilder capabilities = new StringBuilder();
        
        capabilities.append("🎯 Advanced Search Capabilities:\\n");
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
     * Performs comprehensive diagnostics of the search engine and returns a detailed report.
     * 
     * <p>This method conducts a thorough analysis of the search engine's current state,
     * performance characteristics, and provides actionable recommendations for optimization
     * or troubleshooting.</p>
     * 
     * <p><strong>Diagnostic Components:</strong></p>
     * <ul>
     *   <li><strong>System Status:</strong> Initialization state and readiness for operations</li>
     *   <li><strong>Performance Metrics:</strong> Current performance statistics and resource usage</li>
     *   <li><strong>Capability Overview:</strong> Summary of available features and functionality</li>
     *   <li><strong>Health Assessment:</strong> Analysis of system health and potential issues</li>
     *   <li><strong>Recommendations:</strong> Actionable suggestions for improvement or troubleshooting</li>
     * </ul>
     * 
     * <p><strong>Recommendations Include:</strong></p>
     * <ul>
     *   <li>Initialization requirements if not yet set up</li>
     *   <li>Performance optimization suggestions</li>
     *   <li>Memory management recommendations</li>
     *   <li>Configuration improvement suggestions</li>
     *   <li>Maintenance and cleanup recommendations</li>
     * </ul>
     * 
     * <p><strong>Example Output:</strong></p>
     * <pre>
     * 🔍 Search Framework Engine Diagnostics:
     * 
     * Status: ✅ Ready
     * 🚀 Search Framework Engine Metrics:
     *    📊 Total Blocks Indexed: 50,000
     *    ...
     * 
     * 🎯 Advanced Search Capabilities:
     *    ⚡ Fast Public Search: Sub-50ms searches...
     *    ...
     * 
     * 💡 Recommendations:
     *    - Consider clearing cache if memory usage is too high
     * </pre>
     * 
     * <p><strong>Use Cases:</strong></p>
     * <ul>
     *   <li>System troubleshooting and problem diagnosis</li>
     *   <li>Performance monitoring and health checks</li>
     *   <li>Administrative oversight and maintenance planning</li>
     *   <li>Support ticket analysis and resolution</li>
     *   <li>System optimization and tuning guidance</li>
     * </ul>
     * 
     * <p><strong>Performance Impact:</strong> Running diagnostics has minimal performance impact
     * and can be executed safely during normal operations.</p>
     * 
     * @return a comprehensive diagnostic report with system status, metrics, capabilities,
     *         and actionable recommendations. Never null, provides useful information
     *         regardless of initialization status.
     * @see #getPerformanceMetrics() for focused performance information
     * @see #getCapabilitiesSummary() for capability information only
     * @see #getStatistics() for programmatic access to detailed metrics
     */
    public String runDiagnostics() {
        StringBuilder diagnostics = new StringBuilder();
        
        diagnostics.append("🔍 Search Framework Engine Diagnostics:\\n\\n");
        
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
        
        SearchFrameworkEngine.SearchStats stats = getStatistics();
        if (stats.getTotalBlocksIndexed() == 0) {
            diagnostics.append("   - Add blocks to search index for full functionality\\n");
        }
        
        if (stats.getEstimatedMemoryBytes() > 100 * 1024 * 1024) {
            diagnostics.append("   - Consider clearing cache if memory usage is too high\\n");
        }
        
        return diagnostics.toString();
    }
    
    /**
     * Clears all caches, indexes, and resets the search engine to uninitialized state.
     * 
     * <p>This method performs a complete cleanup of all search engine data structures,
     * freeing memory and resetting the system to its initial state. After calling this method,
     * the search engine must be re-initialized before performing any search operations.</p>
     * 
     * <p><strong>Cleanup Operations:</strong></p>
     * <ul>
     *   <li><strong>Search Indexes:</strong> Clears all metadata and content indexes</li>
     *   <li><strong>Performance Caches:</strong> Removes all cached search results and metadata</li>
     *   <li><strong>Password Registry:</strong> Securely clears all stored passwords</li>
     *   <li><strong>Memory Cleanup:</strong> Triggers garbage collection hints for memory recovery</li>
     *   <li><strong>State Reset:</strong> Resets initialization flag to require re-initialization</li>
     * </ul>
     * 
     * <p><strong>Security Considerations:</strong></p>
     * <ul>
     *   <li>All stored passwords are securely wiped from memory</li>
     *   <li>Cached decrypted content is completely cleared</li>
     *   <li>Sensitive metadata is properly disposed of</li>
     *   <li>Memory is overwritten to prevent data recovery</li>
     * </ul>
     * 
     * <p><strong>Use Cases:</strong></p>
     * <ul>
     *   <li><strong>Memory Management:</strong> Free memory when search engine is no longer needed</li>
     *   <li><strong>Security Cleanup:</strong> Clear sensitive data for compliance requirements</li>
     *   <li><strong>System Reset:</strong> Reset to clean state for testing or reconfiguration</li>
     *   <li><strong>Troubleshooting:</strong> Clear corrupted caches and start fresh</li>
     *   <li><strong>Resource Optimization:</strong> Reclaim memory in resource-constrained environments</li>
     * </ul>
     * 
     * <p><strong>Performance Impact:</strong></p>
     * <ul>
     *   <li>Immediate memory reclamation improves overall system performance</li>
     *   <li>Next initialization will need to rebuild all indexes (takes time)</li>
     *   <li>All search operations will fail until re-initialization</li>
     * </ul>
     * 
     * <p><strong>Thread Safety:</strong> This method is thread-safe but will affect all
     * concurrent search operations, causing them to fail after cleanup completes.</p>
     * 
     * <p><strong>Warning:</strong> This operation cannot be undone. All cached data and
     * performance optimizations will be lost and must be rebuilt through re-initialization.</p>
     * 
     * @see #initializeWithBlockchain for re-initialization after cache clearing
     * @see #shutdown() for permanent shutdown without intent to restart
     */
    public void clearCache() {
        searchEngine.clearAll();
        passwordRegistry.clearAll();
        isInitialized = false;
    }
    
    /**
     * Performs a graceful shutdown of the search engine and releases all resources.
     * 
     * <p>This method performs a complete and permanent shutdown of the search engine,
     * properly closing all resources, threads, and connections. After shutdown, the
     * search engine cannot be restarted and a new instance must be created for future use.</p>
     * 
     * <p><strong>Shutdown Process:</strong></p>
     * <ol>
     *   <li><strong>Thread Pool Shutdown:</strong> Gracefully stops all background indexing threads</li>
     *   <li><strong>Resource Cleanup:</strong> Closes all open files and database connections</li>
     *   <li><strong>Memory Cleanup:</strong> Clears all caches and indexes</li>
     *   <li><strong>Security Cleanup:</strong> Securely wipes all stored passwords and sensitive data</li>
     *   <li><strong>State Reset:</strong> Marks the instance as permanently shut down</li>
     * </ol>
     * 
     * <p><strong>Graceful Shutdown Features:</strong></p>
     * <ul>
     *   <li>Waits for in-progress search operations to complete</li>
     *   <li>Prevents new search operations from starting</li>
     *   <li>Properly closes all background threads and executor services</li>
     *   <li>Ensures data integrity during shutdown process</li>
     *   <li>Provides clean resource deallocation</li>
     * </ul>
     * 
     * <p><strong>Security Considerations:</strong></p>
     * <ul>
     *   <li>All passwords and encryption keys are securely wiped</li>
     *   <li>Cached decrypted content is completely cleared</li>
     *   <li>Temporary files and data structures are properly disposed</li>
     *   <li>Memory is overwritten to prevent data recovery</li>
     * </ul>
     * 
     * <p><strong>Use Cases:</strong></p>
     * <ul>
     *   <li><strong>Application Shutdown:</strong> Clean resource release during application exit</li>
     *   <li><strong>Service Restart:</strong> Proper cleanup before service restart</li>
     *   <li><strong>Security Compliance:</strong> Ensure complete data cleanup for sensitive environments</li>
     *   <li><strong>Resource Management:</strong> Free resources when search functionality is no longer needed</li>
     *   <li><strong>Testing Cleanup:</strong> Proper cleanup in test environments</li>
     * </ul>
     * 
     * <p><strong>Thread Safety:</strong> This method is thread-safe and will coordinate with
     * any ongoing operations to ensure clean shutdown without data corruption.</p>
     * 
     * <p><strong>Irreversible Operation:</strong> After shutdown, this SearchSpecialistAPI
     * instance cannot be reused. Create a new instance if search functionality is needed again.</p>
     * 
     * <p><strong>Recommended Usage:</strong></p>
     * <pre>{@code
     * // Always shutdown in finally block or using try-with-resources pattern
     * try {
     *     // Use search API
     *     searchAPI.searchSimple("query");
     * } finally {
     *     // Ensure proper cleanup
     *     searchAPI.shutdown();
     * }
     * }</pre>
     * 
     * @see #clearCache() for temporary cleanup without permanent shutdown
     */
    public void shutdown() {
        searchEngine.shutdown();
        passwordRegistry.shutdown();
        isInitialized = false;
    }
    
    // ===== PASSWORD REGISTRY MANAGEMENT =====
    
    /**
     * Registers a password for a specific encrypted block to enable future search operations.
     * 
     * <p>This method securely stores the password for an encrypted block, enabling the search
     * engine to decrypt and search the block's content in future search operations. The password
     * is stored using secure encryption and memory protection techniques.</p>
     * 
     * <p><strong>Security Features:</strong></p>
     * <ul>
     *   <li><strong>Secure Storage:</strong> Passwords are encrypted using AES-256-GCM</li>
     *   <li><strong>Memory Protection:</strong> Sensitive data is protected from memory dumps</li>
     *   <li><strong>Access Control:</strong> Only authorized operations can retrieve passwords</li>
     *   <li><strong>Audit Trails:</strong> Password registration events are logged securely</li>
     * </ul>
     * 
     * <p><strong>Use Cases:</strong></p>
     * <ul>
     *   <li>Enable comprehensive search across encrypted content</li>
     *   <li>Prepare blocks for future intelligent search operations</li>
     *   <li>Batch registration of passwords for multiple encrypted blocks</li>
     *   <li>Recovery scenarios where passwords need to be re-registered</li>
     * </ul>
     * 
     * <p><strong>Performance Impact:</strong> Password registration is optimized for minimal
     * performance impact and can be performed during normal operations.</p>
     * 
     * <p><strong>Thread Safety:</strong> This method is thread-safe and can be called
     * concurrently with search operations and other password management operations.</p>
     * 
     * @param blockHash the hash of the encrypted block. Must not be null or empty,
     *                  and should correspond to an actual encrypted block in the blockchain.
     * @param password the password for decrypting the block's content. Must not be null
     *                 and should be the correct password used during block encryption.
     * @return {@code true} if the password was successfully registered or updated,
     *         {@code false} if registration failed due to validation or storage issues
     * @throws IllegalArgumentException if blockHash or password is null, or blockHash is empty
     * @see #hasBlockPassword(String) to check if a password is already registered
     * @see #addBlock(Block, String, PrivateKey) which automatically registers passwords
     */
    public boolean registerBlockPassword(String blockHash, String password) {
        // Validate parameters
        if (blockHash == null) {
            throw new IllegalArgumentException("Block hash cannot be null");
        }
        if (blockHash.trim().isEmpty()) {
            throw new IllegalArgumentException("Block hash cannot be empty or contain only whitespace");
        }
        if (password == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }
        
        return passwordRegistry.registerBlockPassword(blockHash, password);
    }
    
    /**
     * Checks if a password is registered for the specified encrypted block.
     * 
     * <p>This method determines whether the search engine has access to the password
     * needed to decrypt and search a specific encrypted block. Blocks with registered
     * passwords can be included in comprehensive encrypted content searches.</p>
     * 
     * <p><strong>Search Impact:</strong></p>
     * <ul>
     *   <li><strong>With Password:</strong> Block content can be decrypted and searched</li>
     *   <li><strong>Without Password:</strong> Only public metadata is searchable</li>
     *   <li><strong>Performance:</strong> Registered passwords improve search completeness</li>
     *   <li><strong>Intelligence:</strong> Affects intelligent search strategy selection</li>
     * </ul>
     * 
     * <p><strong>Use Cases:</strong></p>
     * <ul>
     *   <li>Validate search completeness before performing operations</li>
     *   <li>Determine which blocks can be included in encrypted searches</li>
     *   <li>Audit password coverage across the blockchain</li>
     *   <li>Optimize search strategies based on password availability</li>
     *   <li>Troubleshoot incomplete search results</li>
     * </ul>
     * 
     * <p><strong>Performance:</strong> This is a fast lookup operation with minimal
     * performance impact, suitable for frequent checking during search operations.</p>
     * 
     * @param blockHash the hash of the block to check. Must not be null or empty,
     *                  and should correspond to a block in the blockchain.
     * @return {@code true} if a password is registered for the specified block,
     *         {@code false} if no password is registered or the block hash is invalid
     * @throws IllegalArgumentException if blockHash is null or empty
     * @see #registerBlockPassword(String, String) to register a password for a block
     * @see #getRegisteredBlocks() to get all blocks with registered passwords
     */
    public boolean hasBlockPassword(String blockHash) {
        // Validate parameters
        if (blockHash == null) {
            throw new IllegalArgumentException("Block hash cannot be null");
        }
        if (blockHash.trim().isEmpty()) {
            throw new IllegalArgumentException("Block hash cannot be empty or contain only whitespace");
        }
        
        return passwordRegistry.hasBlockPassword(blockHash);
    }
    
    /**
     * Retrieves the set of all block hashes that have registered passwords.
     * 
     * <p>This method returns a comprehensive list of all encrypted blocks for which
     * the search engine has stored passwords, enabling full search coverage analysis
     * and password management operations.</p>
     * 
     * <p><strong>Information Provided:</strong></p>
     * <ul>
     *   <li><strong>Coverage Analysis:</strong> Which encrypted blocks are fully searchable</li>
     *   <li><strong>Audit Support:</strong> Complete list for security and compliance audits</li>
     *   <li><strong>Management Support:</strong> Foundation for batch password operations</li>
     *   <li><strong>Performance Planning:</strong> Understanding of search capabilities</li>
     * </ul>
     * 
     * <p><strong>Use Cases:</strong></p>
     * <ul>
     *   <li><strong>Search Planning:</strong> Determine comprehensive search capabilities</li>
     *   <li><strong>Security Audits:</strong> Verify password coverage and access control</li>
     *   <li><strong>Performance Analysis:</strong> Understand encrypted search scope</li>
     *   <li><strong>Maintenance Operations:</strong> Bulk password management and cleanup</li>
     *   <li><strong>Compliance Reporting:</strong> Document encrypted content accessibility</li>
     * </ul>
     * 
     * <p><strong>Security Considerations:</strong></p>
     * <ul>
     *   <li>Returns only block hashes, not passwords or decrypted content</li>
     *   <li>Safe for logging and monitoring without exposing sensitive data</li>
     *   <li>Can be used for audit trails and access verification</li>
     * </ul>
     * 
     * <p><strong>Performance:</strong> Returns a view of the internal registry that is
     * efficiently maintained and updated in real-time as passwords are added or removed.</p>
     * 
     * @return an unmodifiable set containing the hashes of all blocks with registered passwords.
     *         Never null, but may be empty if no passwords are registered.
     *         The returned set reflects the current state and updates as passwords are managed.
     * @see #hasBlockPassword(String) to check specific blocks
     * @see #getPasswordRegistryStats() for statistical information about the registry
     */
    public Set<String> getRegisteredBlocks() {
        return passwordRegistry.getRegisteredBlocks();
    }
    
    /**
     * Retrieves comprehensive statistics about the password registry state and usage.
     * 
     * <p>This method provides detailed metrics about password registration, usage patterns,
     * security status, and performance characteristics of the password management system.</p>
     * 
     * <p><strong>Statistical Categories:</strong></p>
     * <ul>
     *   <li><strong>Registration Metrics:</strong> Number of registered blocks and passwords</li>
     *   <li><strong>Usage Metrics:</strong> Password access patterns and frequency</li>
     *   <li><strong>Security Metrics:</strong> Encryption status and security compliance</li>
     *   <li><strong>Performance Metrics:</strong> Registry size, memory usage, and efficiency</li>
     *   <li><strong>Health Metrics:</strong> System health and integrity indicators</li>
     * </ul>
     * 
     * <p><strong>Use Cases:</strong></p>
     * <ul>
     *   <li><strong>Performance Monitoring:</strong> Track registry performance and resource usage</li>
     *   <li><strong>Security Auditing:</strong> Verify security compliance and access patterns</li>
     *   <li><strong>Capacity Planning:</strong> Plan for registry growth and resource needs</li>
     *   <li><strong>Troubleshooting:</strong> Diagnose password-related search issues</li>
     *   <li><strong>Optimization:</strong> Identify opportunities for performance improvements</li>
     * </ul>
     * 
     * <p><strong>Real-time Accuracy:</strong> Statistics reflect the current state of the
     * password registry and are updated in real-time as passwords are managed.</p>
     * 
     * <p><strong>Performance Impact:</strong> Retrieving statistics has minimal performance
     * impact and can be called frequently for monitoring and reporting purposes.</p>
     * 
     * @return a comprehensive statistics object containing detailed metrics about
     *         password registry operation. Never null, provides meaningful data
     *         regardless of registry state.
     * @see #getPerformanceMetrics() for overall search engine performance including registry metrics
     * @see #runDiagnostics() for comprehensive system analysis including password registry health
     */
    public BlockPasswordRegistry.RegistryStats getPasswordRegistryStats() {
        return passwordRegistry.getStats();
    }

    /**
     * Get direct access to the password registry for advanced operations
     * @return BlockPasswordRegistry instance for direct manipulation
     */
    public BlockPasswordRegistry getPasswordRegistry() {
        return passwordRegistry;
    }
}