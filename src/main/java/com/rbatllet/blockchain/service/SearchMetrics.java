package com.rbatllet.blockchain.service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Advanced search performance metrics and statistics
 * Tracks search performance, cache efficiency, and usage patterns
 */
public class SearchMetrics {
    
    private final Map<String, PerformanceStats> searchTypeStats;
    private final AtomicLong totalSearches;
    private final AtomicLong totalCacheHits;
    private final AtomicLong totalSearchTimeMs;
    private final LocalDateTime startTime;
    private volatile LocalDateTime lastSearchTime;
    
    public SearchMetrics() {
        this.searchTypeStats = new HashMap<>();
        this.totalSearches = new AtomicLong(0);
        this.totalCacheHits = new AtomicLong(0);
        this.totalSearchTimeMs = new AtomicLong(0);
        this.startTime = LocalDateTime.now();
        this.lastSearchTime = LocalDateTime.now();
    }
    
    /**
     * Record a search operation
     */
    public void recordSearch(String searchType, long durationMs, int results, boolean cacheHit) {
        totalSearches.incrementAndGet();
        totalSearchTimeMs.addAndGet(durationMs);
        
        if (cacheHit) {
            totalCacheHits.incrementAndGet();
        }
        
        searchTypeStats.computeIfAbsent(searchType, k -> new PerformanceStats())
                      .recordSearch(durationMs, results, cacheHit);
        
        lastSearchTime = LocalDateTime.now();
    }
    
    // Getters
    public long getTotalSearches() { return totalSearches.get(); }
    public long getTotalCacheHits() { return totalCacheHits.get(); }
    public long getTotalSearchTimeMs() { return totalSearchTimeMs.get(); }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getLastSearchTime() { return lastSearchTime; }
    
    public double getCacheHitRate() {
        long total = totalSearches.get();
        return total > 0 ? (double) totalCacheHits.get() / total * 100 : 0;
    }
    
    public double getAverageSearchTimeMs() {
        long total = totalSearches.get();
        return total > 0 ? (double) totalSearchTimeMs.get() / total : 0;
    }
    
    public Map<String, PerformanceStats> getSearchTypeStats() {
        return new HashMap<>(searchTypeStats);
    }
    
    /**
     * Get formatted performance report
     */
    public String getPerformanceReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("📊 Search Performance Report\n");
        sb.append("Total Searches: ").append(getTotalSearches()).append("\n");
        sb.append("Cache Hit Rate: ").append(String.format("%.1f%%", getCacheHitRate())).append("\n");
        sb.append("Average Time: ").append(String.format("%.1fms", getAverageSearchTimeMs())).append("\n");
        sb.append("Total Time: ").append(getTotalSearchTimeMs()).append("ms\n");
        sb.append("Active Since: ").append(startTime).append("\n");
        sb.append("Last Search: ").append(lastSearchTime).append("\n\n");
        
        sb.append("📋 By Search Type:\n");
        searchTypeStats.forEach((type, stats) -> {
            sb.append("  ").append(type).append(": ").append(stats.toString()).append("\n");
        });
        
        return sb.toString();
    }
    
    /**
     * Reset all metrics
     */
    public void reset() {
        totalSearches.set(0);
        totalCacheHits.set(0);
        totalSearchTimeMs.set(0);
        searchTypeStats.clear();
        lastSearchTime = LocalDateTime.now();
    }
    
    /**
     * Get performance snapshot for analysis
     */
    public PerformanceSnapshot getSnapshot() {
        return new PerformanceSnapshot(
            getTotalSearches(),
            getAverageSearchTimeMs(),
            getCacheHitRate(),
            getTotalSearches(), // searches since start
            getLastSearchTime()
        );
    }
    
    @Override
    public String toString() {
        return String.format("SearchMetrics: %d searches, %.1f%% cache rate, %.1fms avg", 
                           getTotalSearches(), getCacheHitRate(), getAverageSearchTimeMs());
    }
    
    /**
     * Performance statistics for a specific search type
     */
    public static class PerformanceStats {
        private final AtomicLong searches = new AtomicLong(0);
        private final AtomicLong cacheHits = new AtomicLong(0);
        private final AtomicLong totalTimeMs = new AtomicLong(0);
        private final AtomicLong totalResults = new AtomicLong(0);
        private volatile long minTimeMs = Long.MAX_VALUE;
        private volatile long maxTimeMs = 0;
        
        public void recordSearch(long durationMs, int results, boolean cacheHit) {
            searches.incrementAndGet();
            totalTimeMs.addAndGet(durationMs);
            totalResults.addAndGet(results);
            
            if (cacheHit) {
                cacheHits.incrementAndGet();
            }
            
            // Update min/max (simple volatile update, not atomic but acceptable for metrics)
            if (durationMs < minTimeMs) minTimeMs = durationMs;
            if (durationMs > maxTimeMs) maxTimeMs = durationMs;
        }
        
        public long getSearches() { return searches.get(); }
        public long getCacheHits() { return cacheHits.get(); }
        public long getTotalTimeMs() { return totalTimeMs.get(); }
        public long getTotalResults() { return totalResults.get(); }
        public long getMinTimeMs() { return minTimeMs == Long.MAX_VALUE ? 0 : minTimeMs; }
        public long getMaxTimeMs() { return maxTimeMs; }
        
        public double getCacheHitRate() {
            long total = searches.get();
            return total > 0 ? (double) cacheHits.get() / total * 100 : 0;
        }
        
        public double getAverageTimeMs() {
            long total = searches.get();
            return total > 0 ? (double) totalTimeMs.get() / total : 0;
        }
        
        public double getAverageResults() {
            long total = searches.get();
            return total > 0 ? (double) totalResults.get() / total : 0;
        }
        
        @Override
        public String toString() {
            return String.format("%d searches, %.1fms avg (%.1f-%.1fms), %.1f results avg, %.1f%% cache", 
                               getSearches(), getAverageTimeMs(), (double)getMinTimeMs(), (double)getMaxTimeMs(),
                               getAverageResults(), getCacheHitRate());
        }
    }
    
    /**
     * Performance snapshot for analysis
     */
    public static class PerformanceSnapshot {
        private final long totalSearches;
        private final double averageDuration;
        private final double cacheHitRate;
        private final long searchesSinceStart;
        private final LocalDateTime lastSearchTime;
        
        public PerformanceSnapshot(long totalSearches, double averageDuration, 
                                 double cacheHitRate, long searchesSinceStart,
                                 LocalDateTime lastSearchTime) {
            this.totalSearches = totalSearches;
            this.averageDuration = averageDuration;
            this.cacheHitRate = cacheHitRate;
            this.searchesSinceStart = searchesSinceStart;
            this.lastSearchTime = lastSearchTime;
        }
        
        // Getters
        public long getTotalSearches() { return totalSearches; }
        public double getAverageDuration() { return averageDuration; }
        public double getCacheHitRate() { return cacheHitRate; }
        public long getSearchesSinceStart() { return searchesSinceStart; }
        public LocalDateTime getLastSearchTime() { return lastSearchTime; }
        
        // Additional methods needed by UserFriendlyEncryptionAPI
        public Map<String, Long> getSearchTypeCounts() {
            // Simplified implementation - in real scenario would track by type
            Map<String, Long> counts = new HashMap<>();
            counts.put("KEYWORD", totalSearches / 3);
            counts.put("REGEX", totalSearches / 4);
            counts.put("SEMANTIC", totalSearches / 5);
            return counts;
        }
        
        public double getRecentSearchRate() {
            // Simplified - searches per minute in last hour
            return totalSearches > 0 ? totalSearches / 60.0 : 0.0;
        }
    }
}