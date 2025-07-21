package com.rbatllet.blockchain.service;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.*;

/**
 * Result container for compression analysis operations
 * Provides detailed compression statistics and recommendations
 */
public class CompressionAnalysisResult {
    
    public enum CompressionAlgorithm {
        GZIP("🗜️ GZIP", "Standard compression, balanced speed/size"),
        ZSTD("⚡ ZSTD", "High-performance, excellent compression"),
        LZ4("🚀 LZ4", "Ultra-fast compression, good for real-time"),
        BROTLI("🎯 Brotli", "Web-optimized, excellent for text"),
        SNAPPY("💨 Snappy", "Google's fast compression library");
        
        private final String displayName;
        private final String description;
        
        CompressionAlgorithm(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }
    
    public static class CompressionMetrics {
        private final CompressionAlgorithm algorithm;
        private final long originalSize;
        private final long compressedSize;
        private final Duration compressionTime;
        private final Duration decompressionTime;
        private final double compressionRatio;
        private final double speedMbps;
        
        public CompressionMetrics(CompressionAlgorithm algorithm, long originalSize, 
                                long compressedSize, Duration compressionTime,
                                Duration decompressionTime) {
            this.algorithm = algorithm;
            this.originalSize = originalSize;
            this.compressedSize = compressedSize;
            this.compressionTime = compressionTime;
            this.decompressionTime = decompressionTime;
            this.compressionRatio = originalSize > 0 ? 
                (1.0 - (double)compressedSize / originalSize) * 100 : 0.0;
            this.speedMbps = originalSize > 0 && compressionTime.toNanos() > 0 ?
                (originalSize / (1024.0 * 1024.0)) / (compressionTime.toNanos() / 1_000_000_000.0) : 0.0;
        }
        
        // Getters
        public CompressionAlgorithm getAlgorithm() { return algorithm; }
        public long getOriginalSize() { return originalSize; }
        public long getCompressedSize() { return compressedSize; }
        public Duration getCompressionTime() { return compressionTime; }
        public Duration getDecompressionTime() { return decompressionTime; }
        public double getCompressionRatio() { return compressionRatio; }
        public double getSpeedMbps() { return speedMbps; }
        
        public double getEfficiencyScore() {
            // Balanced score: 60% compression ratio + 40% speed
            double normalizedRatio = Math.min(compressionRatio / 70.0, 1.0); // Max expected 70%
            double normalizedSpeed = Math.min(speedMbps / 100.0, 1.0); // Max expected 100MB/s
            return (normalizedRatio * 0.6 + normalizedSpeed * 0.4) * 100;
        }
    }
    
    private final String dataIdentifier;
    private final LocalDateTime analysisTimestamp;
    private final Map<CompressionAlgorithm, CompressionMetrics> results;
    private final CompressionAlgorithm recommendedAlgorithm;
    private final String contentType;
    private final long originalDataSize;
    private final List<String> optimizationRecommendations;
    
    public CompressionAnalysisResult(String dataIdentifier, String contentType, long originalDataSize) {
        this.dataIdentifier = dataIdentifier;
        this.contentType = contentType;
        this.originalDataSize = originalDataSize;
        this.analysisTimestamp = LocalDateTime.now();
        this.results = new EnumMap<>(CompressionAlgorithm.class);
        this.optimizationRecommendations = new ArrayList<>();
        this.recommendedAlgorithm = calculateRecommendedAlgorithm();
    }
    
    public void addCompressionResult(CompressionMetrics metrics) {
        results.put(metrics.getAlgorithm(), metrics);
    }
    
    public CompressionAlgorithm calculateRecommendedAlgorithm() {
        if (results.isEmpty()) {
            return getDefaultAlgorithmForContent(contentType);
        }
        
        return results.values().stream()
            .max(Comparator.comparingDouble(CompressionMetrics::getEfficiencyScore))
            .map(CompressionMetrics::getAlgorithm)
            .orElse(CompressionAlgorithm.GZIP);
    }
    
    private CompressionAlgorithm getDefaultAlgorithmForContent(String contentType) {
        if (contentType == null) return CompressionAlgorithm.GZIP;
        
        String type = contentType.toLowerCase();
        if (type.contains("text") || type.contains("json") || type.contains("xml")) {
            return CompressionAlgorithm.BROTLI;
        } else if (type.contains("binary") || type.contains("encrypted")) {
            return CompressionAlgorithm.ZSTD;
        } else if (type.contains("realtime") || type.contains("stream")) {
            return CompressionAlgorithm.LZ4;
        }
        
        return CompressionAlgorithm.GZIP;
    }
    
    public void generateRecommendations() {
        optimizationRecommendations.clear();
        
        CompressionMetrics bestMetrics = results.get(recommendedAlgorithm);
        if (bestMetrics == null) return;
        
        // Size-based recommendations
        if (originalDataSize > 10 * 1024 * 1024) { // > 10MB
            optimizationRecommendations.add("💾 Large data detected - consider splitting into chunks for better streaming");
        }
        
        // Compression ratio recommendations
        if (bestMetrics.getCompressionRatio() < 20) {
            optimizationRecommendations.add("⚠️ Low compression ratio - data may already be compressed or encrypted");
        } else if (bestMetrics.getCompressionRatio() > 70) {
            optimizationRecommendations.add("🎯 Excellent compression - ideal for long-term storage");
        }
        
        // Speed recommendations
        if (bestMetrics.getSpeedMbps() < 10) {
            optimizationRecommendations.add("🐌 Slow compression - consider LZ4 for real-time applications");
        } else if (bestMetrics.getSpeedMbps() > 50) {
            optimizationRecommendations.add("🚀 Fast compression - excellent for high-throughput scenarios");
        }
        
        // Algorithm-specific recommendations
        switch (recommendedAlgorithm) {
            case GZIP:
                optimizationRecommendations.add("🗜️ GZIP selected - standard compression with wide compatibility");
                break;
            case ZSTD:
                optimizationRecommendations.add("💡 ZSTD selected - excellent choice for balanced performance");
                break;
            case LZ4:
                optimizationRecommendations.add("⚡ LZ4 selected - optimized for speed over compression ratio");
                break;
            case BROTLI:
                optimizationRecommendations.add("🌐 Brotli selected - ideal for web content and text data");
                break;
            case SNAPPY:
                optimizationRecommendations.add("💨 Snappy selected - Google's fast compression for real-time use");
                break;
        }
    }
    
    // Getters
    public String getDataIdentifier() { return dataIdentifier; }
    public LocalDateTime getAnalysisTimestamp() { return analysisTimestamp; }
    public Map<CompressionAlgorithm, CompressionMetrics> getResults() { return Collections.unmodifiableMap(results); }
    public CompressionAlgorithm getRecommendedAlgorithm() { return recommendedAlgorithm; }
    public String getContentType() { return contentType; }
    public long getOriginalDataSize() { return originalDataSize; }
    public List<String> getOptimizationRecommendations() { return optimizationRecommendations; }
    
    public CompressionMetrics getBestResult() {
        return results.get(recommendedAlgorithm);
    }
    
    public String getFormattedSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("🗜️ Compression Analysis Report\n");
        sb.append("=====================================\n\n");
        
        sb.append(String.format("📊 Data: %s\n", dataIdentifier));
        sb.append(String.format("📁 Content Type: %s\n", contentType));
        sb.append(String.format("📏 Original Size: %.2f MB\n", originalDataSize / (1024.0 * 1024.0)));
        sb.append(String.format("📅 Analysis Time: %s\n\n", analysisTimestamp));
        
        sb.append("🏆 Recommended Algorithm: ").append(recommendedAlgorithm.getDisplayName()).append("\n");
        
        CompressionMetrics best = getBestResult();
        if (best != null) {
            sb.append(String.format("   • Compression Ratio: %.2f%%\n", best.getCompressionRatio()));
            sb.append(String.format("   • Compressed Size: %.2f MB\n", best.getCompressedSize() / (1024.0 * 1024.0)));
            sb.append(String.format("   • Speed: %.2f MB/s\n", best.getSpeedMbps()));
            sb.append(String.format("   • Efficiency Score: %.2f/100\n\n", best.getEfficiencyScore()));
        }
        
        if (!results.isEmpty()) {
            sb.append("📈 All Results:\n");
            results.values().stream()
                .sorted((a, b) -> Double.compare(b.getEfficiencyScore(), a.getEfficiencyScore()))
                .forEach(metrics -> {
                    sb.append(String.format("   %s: %.2f%% ratio, %.2f MB/s, score: %.2f\n",
                        metrics.getAlgorithm().getDisplayName(),
                        metrics.getCompressionRatio(),
                        metrics.getSpeedMbps(),
                        metrics.getEfficiencyScore()));
                });
            sb.append("\n");
        }
        
        if (!optimizationRecommendations.isEmpty()) {
            sb.append("💡 Optimization Recommendations:\n");
            optimizationRecommendations.forEach(rec -> sb.append("   ").append(rec).append("\n"));
        }
        
        return sb.toString();
    }
}