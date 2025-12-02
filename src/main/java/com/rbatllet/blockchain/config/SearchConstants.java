package com.rbatllet.blockchain.config;

/**
 * Search-related constants for blockchain operations.
 * Centralizes all search configuration values to ensure consistency across the codebase.
 *
 * These constants are used by:
 * - SearchFrameworkEngine for indexing operations
 * - Blockchain batch operations (addBlocksBatch, indexBlocksRange)
 * - Search demos and utilities
 *
 * Note: For memory-safety limits on search results, see {@link MemorySafetyConstants#DEFAULT_MAX_SEARCH_RESULTS}
 *
 * @since v1.0.6 (Phase 5.2)
 */
public final class SearchConstants {

    // Prevent instantiation
    private SearchConstants() {
        throw new AssertionError("Cannot instantiate SearchConstants");
    }

    /**
     * Default key identifier used for search metadata indexing operations.
     * This is NOT a security password - it's a system identifier for the indexing scheme.
     *
     * Used by:
     * - {@code Blockchain.addBlocksBatch()} - indexes blocks with this key
     * - {@code Blockchain.indexBlocksRange()} - re-indexes blocks with this key
     * - {@code SearchFrameworkEngine.indexBlock()} - encrypts metadata with this key
     *
     * This ensures consistency between indexing and re-indexing operations,
     * allowing the search system to decrypt indexed metadata using the same key.
     */
    public static final String DEFAULT_INDEXING_KEY = "search-index-key";

    /**
     * Minimum relevance score threshold for quality search results.
     * Results with scores below this threshold are considered low-quality matches.
     *
     * Used for filtering and quality assessment in search operations.
     */
    public static final double MIN_QUALITY_SCORE = 0.7;

    /**
     * Relevance score bonus for exact keyword matches in public metadata.
     * Added to base score when search term exactly matches a general keyword.
     */
    public static final double EXACT_MATCH_BONUS = 10.0;

    /**
     * Relevance score bonus for partial keyword matches in public metadata.
     * Added to base score when search term partially matches a keyword.
     */
    public static final double PARTIAL_MATCH_BONUS = 5.0;

    /**
     * Relevance score bonus for exact keyword matches in private/sensitive metadata.
     * Higher than public matches due to the sensitivity and importance of private keywords.
     */
    public static final double SENSITIVE_EXACT_MATCH_BONUS = 15.0;

    /**
     * Relevance score bonus for partial keyword matches in private/sensitive metadata.
     * Higher than public partial matches due to the sensitivity of private keywords.
     */
    public static final double SENSITIVE_PARTIAL_MATCH_BONUS = 8.0;

    /**
     * Relevance score bonus for on-chain content matches.
     * On-chain data is more valuable than off-chain data for search.
     */
    public static final double ON_CHAIN_BONUS = 15.0;

    /**
     * Relevance score bonus for off-chain content matches.
     * Applied when match is found in off-chain encrypted content.
     */
    public static final double OFF_CHAIN_BONUS = 20.0;

    /**
     * Maximum allowed length for a single search word.
     * Prevents single huge words from causing overflow in search operations.
     * Search terms longer than this will be rejected with an IllegalArgumentException.
     */
    public static final int MAX_SEARCH_WORD_LENGTH = 50;
}
