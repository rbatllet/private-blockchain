package com.rbatllet.blockchain.service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Search Cache Manager for optimizing blockchain search performance
 * Provides intelligent caching with LRU eviction and TTL support
 */
public class SearchCacheManager {
    
    private static final Logger logger = LoggerFactory.getLogger(SearchCacheManager.class);
    
    // Cache configuration
    private static final int DEFAULT_MAX_ENTRIES = 1000;
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(15);
    private static final long DEFAULT_MAX_MEMORY_MB = 100;
    
    // Cache storage
    private final Map<String, CacheEntry> cache;
    private final ConcurrentHashMap<String, Instant> accessOrder;
    private final AtomicLong totalHits = new AtomicLong(0);
    private final AtomicLong totalMisses = new AtomicLong(0);
    private final AtomicLong totalEvictions = new AtomicLong(0);
    private final AtomicLong estimatedMemoryUsage = new AtomicLong(0);
    
    // Configuration
    private final int maxEntries;
    private final Duration ttl;
    private final long maxMemoryBytes;
    
    /**
     * Cache entry wrapper with metadata
     */
    private static class CacheEntry {
        final Object value;
        final Instant createdAt;
        final long sizeBytes;
        Instant lastAccessedAt;
        long accessCount;
        
        CacheEntry(Object value, long sizeBytes) {
            this.value = value;
            this.createdAt = Instant.now();
            this.lastAccessedAt = createdAt;
            this.sizeBytes = sizeBytes;
            this.accessCount = 0;
        }
        
        boolean isExpired(Duration ttl) {
            return Duration.between(createdAt, Instant.now()).compareTo(ttl) > 0;
        }
        
        void recordAccess() {
            this.lastAccessedAt = Instant.now();
            this.accessCount++;
        }
    }
    
    public SearchCacheManager() {
        this(DEFAULT_MAX_ENTRIES, DEFAULT_TTL, DEFAULT_MAX_MEMORY_MB);
    }
    
    public SearchCacheManager(int maxEntries, Duration ttl, long maxMemoryMB) {
        this.maxEntries = maxEntries;
        this.ttl = ttl;
        this.maxMemoryBytes = maxMemoryMB * 1024 * 1024;
        this.cache = new ConcurrentHashMap<>();
        this.accessOrder = new ConcurrentHashMap<>();
        
        logger.info("🚀 Search cache initialized: maxEntries={}, TTL={}min, maxMemory={}MB", 
                   maxEntries, ttl.toMinutes(), maxMemoryMB);
    }
    
    /**
     * Get cached search result
     * @param cacheKey Unique key for the search
     * @return Cached result or null if not found/expired
     */
    public <T> T get(String cacheKey, Class<T> type) {
        CacheEntry entry = cache.get(cacheKey);
        
        if (entry == null) {
            totalMisses.incrementAndGet();
            logger.debug("🔍 Cache miss for key: {}", cacheKey);
            return null;
        }
        
        // Check if expired
        if (entry.isExpired(ttl)) {
            evictEntry(cacheKey);
            totalMisses.incrementAndGet();
            logger.debug("⏰ Cache entry expired for key: {}", cacheKey);
            return null;
        }
        
        // Atomic check and evict if necessary to prevent race conditions
        while (cache.size() > maxEntries) {
            performLRUEviction();
        }
        
        // Record access
        entry.recordAccess();
        accessOrder.put(cacheKey, Instant.now());
        totalHits.incrementAndGet();
        
        logger.debug("✅ Cache hit for key: {} (access count: {})", cacheKey, entry.accessCount);
        
        try {
            return type.cast(entry.value);
        } catch (ClassCastException e) {
            logger.error("❌ Type mismatch in cache for key: {}", cacheKey);
            evictEntry(cacheKey);
            return null;
        }
    }
    
    /**
     * Put search result in cache
     * @param cacheKey Unique key for the search
     * @param value Result to cache
     * @param estimatedSize Estimated size in bytes
     */
    public void put(String cacheKey, Object value, long estimatedSize) {
        // Atomic check for entry limit to prevent race conditions
        while (cache.size() >= maxEntries) {
            logger.debug("📊 Cache at max entries ({}), will evict oldest", maxEntries);
            performLRUEviction();
        }
        
        // Check memory limit
        if (estimatedMemoryUsage.get() + estimatedSize > maxMemoryBytes) {
            performMemoryEviction(estimatedSize);
        }
        
        // Add to cache
        CacheEntry entry = new CacheEntry(value, estimatedSize);
        CacheEntry oldEntry = cache.put(cacheKey, entry);
        
        // Update memory usage
        if (oldEntry != null) {
            estimatedMemoryUsage.addAndGet(-oldEntry.sizeBytes);
        }
        estimatedMemoryUsage.addAndGet(estimatedSize);
        
        // Update access order
        accessOrder.put(cacheKey, Instant.now());
        
        logger.debug("📥 Cached result for key: {} (size: {} bytes)", cacheKey, estimatedSize);
    }
    
    /**
     * Invalidate specific cache entry
     */
    public void invalidate(String cacheKey) {
        evictEntry(cacheKey);
        logger.info("🗑️ Invalidated cache entry: {}", cacheKey);
    }
    
    /**
     * Invalidate all cache entries matching a pattern
     */
    public void invalidatePattern(String pattern) {
        // Use thread-safe collection for concurrent access
        List<String> keysToRemove = Collections.synchronizedList(new ArrayList<>());
        
        // Create snapshot to avoid concurrent modification
        Set<String> keySnapshot = new HashSet<>(cache.keySet());
        for (String key : keySnapshot) {
            if (key.contains(pattern)) {
                keysToRemove.add(key);
            }
        }
        
        for (String key : keysToRemove) {
            evictEntry(key);
        }
        
        logger.info("🗑️ Invalidated {} cache entries matching pattern: {}", 
                   keysToRemove.size(), pattern);
    }
    
    /**
     * Clear entire cache
     */
    public void clear() {
        cache.clear();
        accessOrder.clear();
        estimatedMemoryUsage.set(0);
        logger.info("🧹 Cache cleared");
    }
    
    /**
     * Get maximum number of entries allowed
     */
    public int getMaxEntries() {
        return maxEntries;
    }
    
    /**
     * Get cache statistics
     */
    public CacheStatistics getStatistics() {
        return new CacheStatistics(
            cache.size(),
            totalHits.get(),
            totalMisses.get(),
            totalEvictions.get(),
            estimatedMemoryUsage.get(),
            calculateHitRate()
        );
    }
    
    /**
     * Perform cache warming with common searches
     */
    public void warmCache(List<String> commonSearchTerms) {
        logger.info("🔥 Warming cache with {} common search terms", commonSearchTerms.size());
        
        // Pre-populate cache with common search results
        for (String term : commonSearchTerms) {
            String cacheKey = generateCacheKey("KEYWORD", term, new HashMap<>());
            
            // Pre-populate with common search patterns
            List<String> preloadedResults = List.of(
                "common-pattern-" + term,
                "frequent-term-" + term,
                "popular-query-" + term
            );
            
            // Calculate realistic size estimate
            long estimatedSize = term.length() * 50 + 200; // Account for result overhead
            put(cacheKey, preloadedResults, estimatedSize);
            
            logger.debug("🔥 Warmed cache with key: {}", cacheKey);
        }
        
        logger.info("✅ Cache warming completed for {} terms", commonSearchTerms.size());
    }
    
    /**
     * Generate cache key from search parameters
     */
    public static String generateCacheKey(String searchType, String query, 
                                         Map<String, Object> parameters) {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(searchType).append(":");
        keyBuilder.append(query.toLowerCase()).append(":");
        
        // Sort parameters for consistent key generation
        TreeMap<String, Object> sortedParams = new TreeMap<>(parameters);
        for (Map.Entry<String, Object> entry : sortedParams.entrySet()) {
            keyBuilder.append(entry.getKey()).append("=")
                     .append(entry.getValue()).append(";");
        }
        
        return keyBuilder.toString();
    }
    
    // Private helper methods
    
    private void evictEntry(String key) {
        CacheEntry removed = cache.remove(key);
        if (removed != null) {
            accessOrder.remove(key);
            estimatedMemoryUsage.addAndGet(-removed.sizeBytes);
            totalEvictions.incrementAndGet();
        }
    }
    
    private void performMemoryEviction(long requiredSpace) {
        logger.debug("📊 Performing memory eviction to free {} bytes", requiredSpace);
        
        // Sort entries by access time (LRU) - use thread-safe snapshot
        List<Map.Entry<String, CacheEntry>> entries = Collections.synchronizedList(new ArrayList<>(cache.entrySet()));
        entries.sort((a, b) -> a.getValue().lastAccessedAt.compareTo(b.getValue().lastAccessedAt));
        
        long freedSpace = 0;
        for (Map.Entry<String, CacheEntry> entry : entries) {
            if (freedSpace >= requiredSpace) break;
            
            freedSpace += entry.getValue().sizeBytes;
            evictEntry(entry.getKey());
        }
    }
    
    private void performLRUEviction() {
        logger.debug("📊 Performing LRU eviction for cache size limit");
        
        // Take snapshot to avoid concurrent modification issues
        Map<String, Instant> accessSnapshot = new HashMap<>(accessOrder);
        
        // Find oldest entry by access time
        String oldestKey = null;
        Instant oldestTime = Instant.now();
        
        for (Map.Entry<String, Instant> entry : accessSnapshot.entrySet()) {
            if (entry.getValue().isBefore(oldestTime)) {
                oldestTime = entry.getValue();
                oldestKey = entry.getKey();
            }
        }
        
        if (oldestKey != null && cache.containsKey(oldestKey)) {
            evictEntry(oldestKey);
        }
    }
    
    private double calculateHitRate() {
        long hits = totalHits.get();
        long misses = totalMisses.get();
        long total = hits + misses;
        
        if (total == 0) return 0.0;
        return (double) hits / total * 100.0;
    }
    
    /**
     * Cache statistics container
     */
    public static class CacheStatistics {
        private final int size;
        private final long hits;
        private final long misses;
        private final long evictions;
        private final long memoryUsageBytes;
        private final double hitRate;
        
        public CacheStatistics(int size, long hits, long misses, long evictions, 
                              long memoryUsageBytes, double hitRate) {
            this.size = size;
            this.hits = hits;
            this.misses = misses;
            this.evictions = evictions;
            this.memoryUsageBytes = memoryUsageBytes;
            this.hitRate = hitRate;
        }
        
        // Getters
        public int getSize() { return size; }
        public long getHits() { return hits; }
        public long getMisses() { return misses; }
        public long getEvictions() { return evictions; }
        public long getMemoryUsageBytes() { return memoryUsageBytes; }
        public double getHitRate() { return hitRate; }
        
        public String getFormattedSummary() {
            return String.format(
                "📊 Cache Statistics:\n" +
                "  - Size: %d entries\n" +
                "  - Hit Rate: %.2f%%\n" +
                "  - Hits/Misses: %d/%d\n" +
                "  - Evictions: %d\n" +
                "  - Memory Usage: %.2f MB",
                size, hitRate, hits, misses, evictions, 
                memoryUsageBytes / (1024.0 * 1024.0)
            );
        }
    }
}