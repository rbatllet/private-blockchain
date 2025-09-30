package com.rbatllet.blockchain.search;

import java.time.LocalDateTime;

/**
 * Common interface for all search result types in the blockchain system.
 *
 * This interface provides a unified API for different search result implementations,
 * allowing polymorphic usage and consistent access to core search result properties.
 *
 * Implementations:
 * - {@link com.rbatllet.blockchain.service.AdvancedSearchResult} - Advanced search with analytics
 * - {@link com.rbatllet.blockchain.service.SearchResults} - Standard search results
 * - {@link OffChainSearchResult} - Off-chain file search results
 *
 * Thread-safety: Implementations should be immutable or provide thread-safe access
 *
 * @since 1.0.0
 */
public interface SearchResultInterface {

    /**
     * Gets the search query or term that produced these results.
     *
     * @return the search query string, never null (may be empty)
     */
    String getSearchTerm();

    /**
     * Gets the total number of matches found in this search.
     *
     * @return the match count, always >= 0
     */
    int getMatchCount();

    /**
     * Checks if this search produced any results.
     *
     * @return true if at least one match was found, false otherwise
     */
    boolean hasResults();

    /**
     * Gets the timestamp when this search was executed.
     *
     * For implementations using long timestamps, convert to LocalDateTime.
     *
     * @return the search execution timestamp, never null
     */
    LocalDateTime getTimestamp();

    /**
     * Gets a human-readable summary of the search results.
     *
     * This method provides a formatted string representation suitable for
     * logging, debugging, or user display.
     *
     * @return a formatted summary string, never null
     */
    String getSearchSummary();
}