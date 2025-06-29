package com.rbatllet.blockchain.search;

/**
 * Search level enumeration for hybrid search functionality
 */
public enum SearchLevel {
    /**
     * Search only in keywords (manual + auto) - fastest option
     */
    FAST_ONLY,
    
    /**
     * Search in keywords + block data - medium performance
     */
    INCLUDE_DATA,
    
    /**
     * Search everything including off-chain content - comprehensive but slower
     */
    EXHAUSTIVE_OFFCHAIN
}