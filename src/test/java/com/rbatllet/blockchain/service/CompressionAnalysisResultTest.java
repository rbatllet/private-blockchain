package com.rbatllet.blockchain.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.List;

/**
 * Comprehensive robustness tests for CompressionAnalysisResult class
 * Tests defensive programming patterns and edge case handling
 */
@DisplayName("CompressionAnalysisResult Robustness Tests")
class CompressionAnalysisResultTest {

    private CompressionAnalysisResult analysisResult;
    private CompressionAnalysisResult.CompressionMetrics testMetrics;

    @BeforeEach
    void setUp() {
        analysisResult = new CompressionAnalysisResult("test-data", "application/json", 1024);
        testMetrics = new CompressionAnalysisResult.CompressionMetrics(
            CompressionAnalysisResult.CompressionAlgorithm.GZIP,
            1024L,
            512L,
            Duration.ofMillis(100),
            Duration.ofMillis(50)
        );
    }

    @Test
    @DisplayName("Constructor should reject null dataIdentifier")
    void testConstructorNullDataIdentifier() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            new CompressionAnalysisResult(null, "text/plain", 1024));
        assertEquals("Data identifier cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("Constructor should reject empty dataIdentifier")
    void testConstructorEmptyDataIdentifier() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            new CompressionAnalysisResult("", "text/plain", 1024));
        assertEquals("Data identifier cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("Constructor should reject whitespace-only dataIdentifier")
    void testConstructorWhitespaceDataIdentifier() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            new CompressionAnalysisResult("   ", "text/plain", 1024));
        assertEquals("Data identifier cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("Constructor should reject null contentType")
    void testConstructorNullContentType() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            new CompressionAnalysisResult("test-data", null, 1024));
        assertEquals("Content type cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("Constructor should reject empty contentType")
    void testConstructorEmptyContentType() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            new CompressionAnalysisResult("test-data", "", 1024));
        assertEquals("Content type cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("Constructor should reject negative originalDataSize")
    void testConstructorNegativeDataSize() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            new CompressionAnalysisResult("test-data", "text/plain", -1));
        assertEquals("Original data size cannot be negative", exception.getMessage());
    }

    @Test
    @DisplayName("Constructor should accept zero originalDataSize")
    void testConstructorZeroDataSize() {
        assertDoesNotThrow(() -> 
            new CompressionAnalysisResult("test-data", "text/plain", 0));
    }

    @Test
    @DisplayName("getBestResult should return null when no results available")
    void testGetBestResultNoResults() {
        CompressionAnalysisResult.CompressionMetrics result = analysisResult.getBestResult();
        assertNull(result, "getBestResult should return null when no compression results are available");
    }

    @Test
    @DisplayName("getBestResult should return correct metrics after adding result")
    void testGetBestResultWithResults() {
        analysisResult.addCompressionResult(testMetrics);
        
        CompressionAnalysisResult.CompressionMetrics result = analysisResult.getBestResult();
        assertNotNull(result, "getBestResult should return metrics when results are available");
        assertEquals(testMetrics.getAlgorithm(), result.getAlgorithm());
    }

    @Test
    @DisplayName("getOptimizationRecommendations should return immutable list")
    void testOptimizationRecommendationsImmutability() {
        analysisResult.addCompressionResult(testMetrics);
        analysisResult.generateRecommendations();
        
        List<String> recommendations = analysisResult.getOptimizationRecommendations();
        assertNotNull(recommendations);
        
        // Test immutability - should throw UnsupportedOperationException
        assertThrows(UnsupportedOperationException.class, () -> 
            recommendations.add("This should fail"));
        assertThrows(UnsupportedOperationException.class, () -> 
            recommendations.clear());
    }

    @Test
    @DisplayName("getFormattedSummary should handle missing data gracefully")
    void testFormattedSummaryWithoutResults() {
        String summary = analysisResult.getFormattedSummary();
        
        assertNotNull(summary, "getFormattedSummary should never return null");
        assertTrue(summary.contains("Compression Analysis Report"), "Summary should contain report header");
        assertTrue(summary.contains("test-data"), "Summary should contain data identifier");
        assertTrue(summary.contains("application/json"), "Summary should contain content type");
        assertTrue(summary.contains("No compression results available"), "Summary should indicate no results");
    }

    @Test
    @DisplayName("getFormattedSummary should handle complete data properly")
    void testFormattedSummaryWithResults() {
        analysisResult.addCompressionResult(testMetrics);
        analysisResult.generateRecommendations();
        
        String summary = analysisResult.getFormattedSummary();
        
        assertNotNull(summary, "getFormattedSummary should never return null");
        assertTrue(summary.contains("Compression Analysis Report"), "Summary should contain report header");
        assertTrue(summary.contains("GZIP"), "Summary should contain algorithm name");
        assertTrue(summary.contains("Compression Ratio:"), "Summary should contain compression ratio");
        assertTrue(summary.contains("All Results:"), "Summary should contain results section");
    }

    @Test
    @DisplayName("Basic getters should return correct values")
    void testBasicGetters() {
        assertEquals("test-data", analysisResult.getDataIdentifier());
        assertEquals("application/json", analysisResult.getContentType());
        assertEquals(1024L, analysisResult.getOriginalDataSize());
        assertNotNull(analysisResult.getAnalysisTimestamp());
        assertNotNull(analysisResult.getResults());
        assertNotNull(analysisResult.getRecommendedAlgorithm());
    }

    @Test
    @DisplayName("getResults should return immutable map")
    void testResultsImmutability() {
        analysisResult.addCompressionResult(testMetrics);
        
        var results = analysisResult.getResults();
        assertNotNull(results);
        
        // Test immutability - should throw UnsupportedOperationException
        assertThrows(UnsupportedOperationException.class, () -> 
            results.clear());
    }

    @Test
    @DisplayName("addCompressionResult should work correctly")
    void testAddCompressionResult() {
        assertEquals(0, analysisResult.getResults().size(), "Initially should have no results");
        
        analysisResult.addCompressionResult(testMetrics);
        
        assertEquals(1, analysisResult.getResults().size(), "Should have one result after adding");
        assertTrue(analysisResult.getResults().containsKey(CompressionAnalysisResult.CompressionAlgorithm.GZIP));
    }

    @Test
    @DisplayName("generateRecommendations should populate recommendations list")
    void testGenerateRecommendations() {
        analysisResult.addCompressionResult(testMetrics);
        
        // Before generation
        assertTrue(analysisResult.getOptimizationRecommendations().isEmpty(), "Should start with empty recommendations");
        
        analysisResult.generateRecommendations();
        
        // After generation
        assertFalse(analysisResult.getOptimizationRecommendations().isEmpty(), "Should have recommendations after generation");
    }

    @Test
    @DisplayName("Edge case: large data size handling")
    void testLargeDataSize() {
        long largeSize = 50L * 1024L * 1024L; // 50MB
        CompressionAnalysisResult largeDataResult = new CompressionAnalysisResult("large-data", "binary/data", largeSize);
        
        assertEquals(largeSize, largeDataResult.getOriginalDataSize());
        assertNotNull(largeDataResult.getFormattedSummary());
    }

    @Test
    @DisplayName("Edge case: multiple compression algorithms")
    void testMultipleAlgorithms() {
        // Add multiple compression results
        analysisResult.addCompressionResult(testMetrics);
        
        CompressionAnalysisResult.CompressionMetrics zstdMetrics = new CompressionAnalysisResult.CompressionMetrics(
            CompressionAnalysisResult.CompressionAlgorithm.ZSTD,
            1024L,
            400L, // Better compression
            Duration.ofMillis(80),
            Duration.ofMillis(40)
        );
        analysisResult.addCompressionResult(zstdMetrics);
        
        assertEquals(2, analysisResult.getResults().size());
        
        String summary = analysisResult.getFormattedSummary();
        assertTrue(summary.contains("All Results:"), "Summary should show all results");
    }
}