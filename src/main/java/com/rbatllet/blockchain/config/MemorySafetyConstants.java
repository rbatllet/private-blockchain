package com.rbatllet.blockchain.config;

/**
 * Memory safety constants for blockchain operations.
 * Centralizes all memory-related limits to prevent OutOfMemoryError on large blockchains.
 *
 * These constants are used across the codebase to ensure consistent memory management:
 * - Batch operations have maximum size limits
 * - Search results have configurable limits with safe defaults
 * - Export operations validate chain size before loading
 * - All operations use pagination or streaming for large datasets
 */
public final class MemorySafetyConstants {

    // Prevent instantiation
    private MemorySafetyConstants() {
        throw new AssertionError("Cannot instantiate MemorySafetyConstants");
    }

    /**
     * Maximum number of items for batch retrieve operations.
     * Used by batchRetrieveBlocks() and batchRetrieveBlocksByHash().
     * Operations exceeding this limit will throw IllegalArgumentException.
     */
    public static final int MAX_BATCH_SIZE = 10000;

    /**
     * Default maximum results for search operations.
     * Used by search methods without explicit limit parameter.
     */
    public static final int DEFAULT_MAX_SEARCH_RESULTS = 10000;

    /**
     * Safe export limit - warns user above this threshold.
     * Blockchains with more blocks should consider range exports or increase heap size.
     */
    public static final int SAFE_EXPORT_LIMIT = 100000;

    /**
     * Absolute maximum for export operations.
     * Export will fail above this limit to prevent OutOfMemoryError.
     */
    public static final int MAX_EXPORT_LIMIT = 500000;

    /**
     * Large rollback warning threshold.
     * Rollback operations above this size will log a warning.
     */
    public static final int LARGE_ROLLBACK_THRESHOLD = 100000;

    /**
     * Default batch size for processing operations.
     * Used by processChainInBatches() and similar streaming operations.
     */
    public static final int DEFAULT_BATCH_SIZE = 1000;

    /**
     * Fallback batch size for linear search operations.
     * Smaller batch size used when optimized indexes are unavailable.
     * Used by fallback search methods in UserFriendlyEncryptionAPI.
     */
    public static final int FALLBACK_BATCH_SIZE = 100;

    /**
     * Progress reporting interval for batch operations.
     * Log progress every N blocks processed.
     */
    public static final int PROGRESS_REPORT_INTERVAL = 5000;

    /**
     * Maximum iterations for JSON metadata search operations.
     * Prevents excessive iteration over large blockchains.
     * Used by searchByCustomMetadata*, searchByCustomMetadataKeyValue*, and searchByCustomMetadataMultipleCriteria*.
     * For unlimited results, use the streaming variants (streamByCustomMetadata*).
     */
    public static final int MAX_JSON_METADATA_ITERATIONS = 100;
}
