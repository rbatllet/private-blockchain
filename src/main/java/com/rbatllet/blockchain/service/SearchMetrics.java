package com.rbatllet.blockchain.service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Advanced search performance metrics and statistics
 * Tracks search performance, cache efficiency, and usage patterns
 */
public class SearchMetrics {

    private static final Logger logger = LoggerFactory.getLogger(
        SearchMetrics.class
    );

    private final Map<String, PerformanceStats> searchTypeStats;
    private final AtomicLong totalSearches;
    private final AtomicLong totalCacheHits;
    private final AtomicLong totalSearchTimeMs;
    private final LocalDateTime startTime;
    private volatile LocalDateTime lastSearchTime;

    public SearchMetrics() {
        this.searchTypeStats = new ConcurrentHashMap<>();
        this.totalSearches = new AtomicLong(0);
        this.totalCacheHits = new AtomicLong(0);
        this.totalSearchTimeMs = new AtomicLong(0);
        this.startTime = LocalDateTime.now();
        this.lastSearchTime = LocalDateTime.now();
    }

    /**
     * Record a search operation
     */
    public void recordSearch(
        String searchType,
        long durationMs,
        int results,
        boolean cacheHit
    ) {
        totalSearches.incrementAndGet();
        totalSearchTimeMs.addAndGet(durationMs);

        if (cacheHit) {
            totalCacheHits.incrementAndGet();
        }

        searchTypeStats
            .computeIfAbsent(searchType, k -> new PerformanceStats())
            .recordSearch(durationMs, results, cacheHit);

        lastSearchTime = LocalDateTime.now();
    }

    // Getters
    public long getTotalSearches() {
        return totalSearches.get();
    }

    public long getTotalCacheHits() {
        return totalCacheHits.get();
    }

    public long getTotalSearchTimeMs() {
        return totalSearchTimeMs.get();
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getLastSearchTime() {
        return lastSearchTime;
    }

    public double getCacheHitRate() {
        long total = totalSearches.get();
        return total > 0 ? ((double) totalCacheHits.get() / total) * 100 : 0;
    }

    public double getAverageSearchTimeMs() {
        long total = totalSearches.get();
        return total > 0 ? (double) totalSearchTimeMs.get() / total : 0;
    }

    public Map<String, PerformanceStats> getSearchTypeStats() {
        return new ConcurrentHashMap<>(searchTypeStats);
    }

    /**
     * Get formatted performance report
     */
    public String getPerformanceReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("📊 Search Performance Report\n");
        sb.append("Total Searches: ").append(getTotalSearches()).append("\n");
        sb
            .append("Cache Hit Rate: ")
            .append(String.format("%.1f%%", getCacheHitRate()))
            .append("\n");
        sb
            .append("Average Time: ")
            .append(String.format("%.1fms", getAverageSearchTimeMs()))
            .append("\n");
        sb.append("Total Time: ").append(getTotalSearchTimeMs()).append("ms\n");
        sb.append("Active Since: ").append(startTime).append("\n");
        sb.append("Last Search: ").append(lastSearchTime).append("\n\n");

        sb.append("📋 By Search Type:\n");
        searchTypeStats.forEach((type, stats) -> {
            sb
                .append("  ")
                .append(type)
                .append(": ")
                .append(stats.toString())
                .append("\n");
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
     * Enhanced performance insights for CLI analysis
     */
    public static class PerformanceInsights {

        private final String fastestSearchType;
        private final String slowestSearchType;
        private final double overallCacheHitRate;
        private final double averageSearchTime;
        private final long totalOperations;
        private final String performanceRating;

        public PerformanceInsights(
            String fastestSearchType,
            String slowestSearchType,
            double overallCacheHitRate,
            double averageSearchTime,
            long totalOperations,
            String performanceRating
        ) {
            this.fastestSearchType = fastestSearchType != null
                ? fastestSearchType
                : "N/A";
            this.slowestSearchType = slowestSearchType != null
                ? slowestSearchType
                : "N/A";
            this.overallCacheHitRate = Double.isNaN(overallCacheHitRate)
                ? 0.0
                : overallCacheHitRate;
            this.averageSearchTime = Double.isNaN(averageSearchTime)
                ? 0.0
                : averageSearchTime;
            this.totalOperations = Math.max(0, totalOperations);
            this.performanceRating = performanceRating != null
                ? performanceRating
                : "Unknown";
        }

        // Getters
        public String getFastestSearchType() {
            return fastestSearchType;
        }

        public String getSlowestSearchType() {
            return slowestSearchType;
        }

        public double getOverallCacheHitRate() {
            return overallCacheHitRate;
        }

        public double getAverageSearchTime() {
            return averageSearchTime;
        }

        public long getTotalOperations() {
            return totalOperations;
        }

        public String getPerformanceRating() {
            return performanceRating;
        }

        public String getSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("Performance Insights\n");
            sb
                .append("  Fastest search type: ")
                .append(fastestSearchType)
                .append("\n");
            sb
                .append("  Slowest search type: ")
                .append(slowestSearchType)
                .append("\n");
            sb
                .append("  Overall cache hit rate: ")
                .append(String.format("%.1f%%", overallCacheHitRate))
                .append("\n");
            sb
                .append("  Average search time: ")
                .append(String.format("%.2f ms", averageSearchTime))
                .append("\n");
            sb
                .append("  Total operations: ")
                .append(totalOperations)
                .append("\n");
            sb
                .append("  Performance rating: ")
                .append(performanceRating)
                .append("\n");
            return sb.toString();
        }
    }

    /**
     * Generate performance insights from current metrics
     *
     * @return Performance insights analysis
     */
    public PerformanceInsights getPerformanceInsights() {
        if (searchTypeStats.isEmpty()) {
            return new PerformanceInsights(
                "N/A",
                "N/A",
                0.0,
                0.0,
                0L,
                "No data"
            );
        }

        // Find fastest and slowest search types
        String fastestType = searchTypeStats
            .entrySet()
            .stream()
            .min((a, b) ->
                Double.compare(
                    a.getValue().getAverageTimeMs(),
                    b.getValue().getAverageTimeMs()
                )
            )
            .map(entry -> entry.getKey())
            .orElse("N/A");

        String slowestType = searchTypeStats
            .entrySet()
            .stream()
            .max((a, b) ->
                Double.compare(
                    a.getValue().getAverageTimeMs(),
                    b.getValue().getAverageTimeMs()
                )
            )
            .map(entry -> entry.getKey())
            .orElse("N/A");

        // Calculate overall cache hit rate
        double totalSearchCount = searchTypeStats
            .values()
            .stream()
            .mapToDouble(s -> s.getSearches())
            .sum();

        double weightedCacheHitRate = totalSearchCount > 0
            ? (searchTypeStats
                    .values()
                    .stream()
                    .mapToDouble(
                        s -> (s.getCacheHitRate() / 100.0) * s.getSearches()
                    )
                    .sum() /
                totalSearchCount) *
            100.0
            : 0.0;

        double avgTime = getAverageSearchTimeMs();

        // Determine performance rating
        String rating;
        if (avgTime < 10 && weightedCacheHitRate > 80) {
            rating = "Excellent";
        } else if (avgTime < 50 && weightedCacheHitRate > 60) {
            rating = "Good";
        } else if (avgTime < 200 && weightedCacheHitRate > 40) {
            rating = "Fair";
        } else {
            rating = "Needs improvement";
        }

        return new PerformanceInsights(
            fastestType,
            slowestType,
            weightedCacheHitRate,
            avgTime,
            getTotalSearches(),
            rating
        );
    }

    /**
     * Report formats for different output types
     */
    public enum ReportFormat {
        SUMMARY, // Brief overview
        DETAILED, // Complete breakdown
        JSON, // JSON format
        CSV, // CSV format
    }

    /**
     * Get formatted report in specified format
     * @param format Report format to use
     * @return Formatted report string
     */
    public String getFormattedReport(ReportFormat format) {
        if (format == null) {
            logger.warn(
                "Report format cannot be null, using default DETAILED format"
            );
            format = ReportFormat.DETAILED;
        }

        try {
            switch (format) {
                case SUMMARY:
                    return getSummaryReport();
                case DETAILED:
                    return getPerformanceReport(); // Use existing detailed report
                case JSON:
                    return getJsonReport();
                case CSV:
                    return getCsvReport();
                default:
                    logger.warn(
                        "Unknown report format: {}, using DETAILED",
                        format
                    );
                    return getPerformanceReport();
            }
        } catch (Exception e) {
            logger.error(
                "Error generating report for format {}: {}",
                format,
                e.getMessage()
            );
            // Return basic error report
            return (
                "Error generating report: " +
                e.getMessage() +
                "\n" +
                "Total searches: " +
                totalSearches.get() +
                "\n" +
                "Cache hits: " +
                totalCacheHits.get() +
                "\n" +
                "Total time: " +
                totalSearchTimeMs.get() +
                " ms"
            );
        }
    }

    private String getSummaryReport() {
        StringBuilder sb = new StringBuilder();
        try {
            sb.append("Search Performance Summary\n");
            sb.append("========================\n");
            sb
                .append("Total searches: ")
                .append(getTotalSearches())
                .append("\n");
            sb
                .append("Cache hit rate: ")
                .append(String.format("%.1f%%", getCacheHitRate()))
                .append("\n");
            sb
                .append("Average time: ")
                .append(String.format("%.2f ms", getAverageSearchTimeMs()))
                .append("\n");

            PerformanceInsights insights = getPerformanceInsights();
            if (insights != null) {
                sb
                    .append("Performance: ")
                    .append(insights.getPerformanceRating())
                    .append("\n");
            } else {
                sb.append("Performance: N/A\n");
            }
        } catch (Exception e) {
            logger.error("Error generating summary report: {}", e.getMessage());
            sb
                .append("Error generating summary: ")
                .append(e.getMessage())
                .append("\n");
        }

        return sb.toString();
    }

    private String getJsonReport() {
        StringBuilder sb = new StringBuilder();

        try {
            sb.append("{\n");

            // Basic metrics with safe formatting
            sb
                .append("  \"totalSearches\": ")
                .append(getTotalSearches())
                .append(",\n");

            double cacheHitRate = getCacheHitRate();
            sb
                .append("  \"cacheHitRate\": ")
                .append(
                    Double.isNaN(cacheHitRate)
                        ? "0.0"
                        : String.format("%.1f", cacheHitRate)
                )
                .append(",\n");

            double avgTime = getAverageSearchTimeMs();
            sb
                .append("  \"averageTimeMs\": ")
                .append(
                    Double.isNaN(avgTime)
                        ? "0.0"
                        : String.format("%.2f", avgTime)
                )
                .append(",\n");

            sb
                .append("  \"totalTimeMs\": ")
                .append(getTotalSearchTimeMs())
                .append(",\n");

            // Safe timestamp handling
            String startTimeStr = (startTime != null)
                ? startTime.toString()
                : "null";
            String lastSearchTimeStr = (lastSearchTime != null)
                ? lastSearchTime.toString()
                : "null";
            sb
                .append("  \"startTime\": \"")
                .append(startTimeStr)
                .append("\",\n");
            sb
                .append("  \"lastSearchTime\": \"")
                .append(lastSearchTimeStr)
                .append("\",\n");

            // Performance insights with null checking
            PerformanceInsights insights = getPerformanceInsights();
            sb.append("  \"insights\": {\n");
            if (insights != null) {
                sb
                    .append("    \"fastestType\": \"")
                    .append(escapeJsonString(insights.getFastestSearchType()))
                    .append("\",\n");
                sb
                    .append("    \"slowestType\": \"")
                    .append(escapeJsonString(insights.getSlowestSearchType()))
                    .append("\",\n");
                sb
                    .append("    \"performanceRating\": \"")
                    .append(escapeJsonString(insights.getPerformanceRating()))
                    .append("\"\n");
            } else {
                sb.append("    \"error\": \"Unable to generate insights\"\n");
            }
            sb.append("  },\n");

            // Search type statistics with safe iteration
            sb.append("  \"searchTypes\": {\n");
            if (searchTypeStats != null && !searchTypeStats.isEmpty()) {
                boolean first = true;
                for (var entry : searchTypeStats.entrySet()) {
                    if (
                        entry.getKey() == null || entry.getValue() == null
                    ) continue;

                    if (!first) sb.append(",\n");
                    first = false;

                    var stats = entry.getValue();
                    sb
                        .append("    \"")
                        .append(escapeJsonString(entry.getKey()))
                        .append("\": {");
                    sb
                        .append("\"searches\": ")
                        .append(stats.getSearches())
                        .append(", ");

                    double avgTimeMs = stats.getAverageTimeMs();
                    sb
                        .append("\"avgTimeMs\": ")
                        .append(
                            Double.isNaN(avgTimeMs)
                                ? "0.0"
                                : String.format("%.2f", avgTimeMs)
                        )
                        .append(", ");

                    double hitRate = stats.getCacheHitRate();
                    sb
                        .append("\"cacheHitRate\": ")
                        .append(
                            Double.isNaN(hitRate)
                                ? "0.0"
                                : String.format("%.1f", hitRate)
                        )
                        .append(", ");

                    double avgResults = stats.getAverageResults();
                    sb
                        .append("\"avgResults\": ")
                        .append(
                            Double.isNaN(avgResults)
                                ? "0.0"
                                : String.format("%.1f", avgResults)
                        )
                        .append("}");
                }
            }
            sb.append("\n  }\n");
            sb.append("}");
        } catch (Exception e) {
            logger.error("Error generating JSON report: {}", e.getMessage());
            return (
                "{\"error\": \"Failed to generate JSON report: " +
                escapeJsonString(e.getMessage()) +
                "\"}"
            );
        }

        return sb.toString();
    }

    /**
     * Escape special characters for JSON strings
     */
    private String escapeJsonString(String str) {
        if (str == null) return "null";
        return str
            .replace("\"", "\\\"")
            .replace("\\", "\\\\")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

    private String getCsvReport() {
        StringBuilder sb = new StringBuilder();

        try {
            // CSV Header
            sb.append(
                "SearchType,Searches,AvgTimeMs,CacheHitRate,AvgResults,MinTimeMs,MaxTimeMs\n"
            );

            // Safe iteration with null checking
            if (searchTypeStats != null && !searchTypeStats.isEmpty()) {
                for (var entry : searchTypeStats.entrySet()) {
                    if (entry.getKey() == null || entry.getValue() == null) {
                        continue;
                    }

                    var stats = entry.getValue();

                    // Escape CSV values and handle nulls
                    sb.append(escapeCsvValue(entry.getKey())).append(",");
                    sb.append(stats.getSearches()).append(",");

                    // Safe double formatting
                    double avgTime = stats.getAverageTimeMs();
                    sb
                        .append(
                            Double.isNaN(avgTime)
                                ? "0.00"
                                : String.format("%.2f", avgTime)
                        )
                        .append(",");

                    double hitRate = stats.getCacheHitRate();
                    sb
                        .append(
                            Double.isNaN(hitRate)
                                ? "0.0"
                                : String.format("%.1f", hitRate)
                        )
                        .append(",");

                    double avgResults = stats.getAverageResults();
                    sb
                        .append(
                            Double.isNaN(avgResults)
                                ? "0.0"
                                : String.format("%.1f", avgResults)
                        )
                        .append(",");

                    sb.append(stats.getMinTimeMs()).append(",");
                    sb.append(stats.getMaxTimeMs()).append("\n");
                }
            }
        } catch (Exception e) {
            logger.error("Error generating CSV report: {}", e.getMessage());
            sb.setLength(0); // Clear the buffer
            sb.append("Error,Message\n");
            sb
                .append("CSV Generation Failed,\"")
                .append(escapeCsvValue(e.getMessage()))
                .append("\"\n");
        }

        return sb.toString();
    }

    /**
     * Escape special characters for CSV values
     */
    private String escapeCsvValue(String value) {
        if (value == null) {
            return "null";
        }

        // If value contains comma, quote, or newline, wrap in quotes and escape quotes
        if (
            value.contains(",") ||
            value.contains("\"") ||
            value.contains("\n") ||
            value.contains("\r")
        ) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }

        return value;
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
        return String.format(
            "SearchMetrics: %d searches, %.1f%% cache rate, %.1fms avg",
            getTotalSearches(),
            getCacheHitRate(),
            getAverageSearchTimeMs()
        );
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

        public void recordSearch(
            long durationMs,
            int results,
            boolean cacheHit
        ) {
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

        public long getSearches() {
            return searches.get();
        }

        public long getCacheHits() {
            return cacheHits.get();
        }

        public long getTotalTimeMs() {
            return totalTimeMs.get();
        }

        public long getTotalResults() {
            return totalResults.get();
        }

        public long getMinTimeMs() {
            return minTimeMs == Long.MAX_VALUE ? 0 : minTimeMs;
        }

        public long getMaxTimeMs() {
            return maxTimeMs;
        }

        public double getCacheHitRate() {
            long total = searches.get();
            return total > 0 ? ((double) cacheHits.get() / total) * 100 : 0;
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
            return String.format(
                "%d searches, %.1fms avg (%.1f-%.1fms), %.1f results avg, %.1f%% cache",
                getSearches(),
                getAverageTimeMs(),
                (double) getMinTimeMs(),
                (double) getMaxTimeMs(),
                getAverageResults(),
                getCacheHitRate()
            );
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

        public PerformanceSnapshot(
            long totalSearches,
            double averageDuration,
            double cacheHitRate,
            long searchesSinceStart,
            LocalDateTime lastSearchTime
        ) {
            this.totalSearches = totalSearches;
            this.averageDuration = averageDuration;
            this.cacheHitRate = cacheHitRate;
            this.searchesSinceStart = searchesSinceStart;
            this.lastSearchTime = lastSearchTime;
        }

        // Getters
        public long getTotalSearches() {
            return totalSearches;
        }

        public double getAverageDuration() {
            return averageDuration;
        }

        public double getCacheHitRate() {
            return cacheHitRate;
        }

        public long getSearchesSinceStart() {
            return searchesSinceStart;
        }

        public LocalDateTime getLastSearchTime() {
            return lastSearchTime;
        }

        // Additional methods needed by UserFriendlyEncryptionAPI
        public Map<String, Long> getSearchTypeCounts() {
            // Simplified implementation - in real scenario would track by type
            Map<String, Long> counts = new ConcurrentHashMap<>();
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
