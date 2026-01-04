package com.rbatllet.blockchain.service.detailed;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;

import com.rbatllet.blockchain.service.SearchMetrics;
import com.rbatllet.blockchain.service.SearchMetrics.PerformanceInsights;
import com.rbatllet.blockchain.service.SearchMetrics.ReportFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Comprehensive tests for SearchMetrics ReportFormat enum and PerformanceInsights class
 * focusing on edge cases, null handling, and robustness
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SearchMetricsDetailedTest {

    private static final Logger logger = LoggerFactory.getLogger(
        SearchMetricsDetailedTest.class
    );

    private SearchMetrics metrics;

    @BeforeEach
    void setUp() {
        metrics = new SearchMetrics();
        logger.info("Test setup completed");
    }

    @Test
    @Order(1)
    @DisplayName("Test ReportFormat enum values")
    void testReportFormatEnumValues() {
        // Test all enum values exist
        ReportFormat[] formats = ReportFormat.values();
        assertEquals(
            4,
            formats.length,
            "Should have exactly 4 ReportFormat values"
        );

        // Test specific values
        assertTrue(
            Arrays.asList(formats).contains(ReportFormat.SUMMARY)
        );
        assertTrue(
            Arrays.asList(formats).contains(ReportFormat.DETAILED)
        );
        assertTrue(
            Arrays.asList(formats).contains(ReportFormat.JSON)
        );
        assertTrue(Arrays.asList(formats).contains(ReportFormat.CSV));

        // Test valueOf
        assertEquals(ReportFormat.SUMMARY, ReportFormat.valueOf("SUMMARY"));
        assertEquals(ReportFormat.DETAILED, ReportFormat.valueOf("DETAILED"));
        assertEquals(ReportFormat.JSON, ReportFormat.valueOf("JSON"));
        assertEquals(ReportFormat.CSV, ReportFormat.valueOf("CSV"));

        logger.info("ReportFormat enum validation passed");
    }

    @Test
    @Order(2)
    @DisplayName("Test getFormattedReport with null format")
    void testGetFormattedReportWithNullFormat() {
        // Test null format - should default to DETAILED
        String result = metrics.getFormattedReport(null);
        assertNotNull(result, "Result should not be null for null format");
        assertFalse(
            result.isEmpty(),
            "Result should not be empty for null format"
        );

        // Should contain performance report elements
        assertTrue(
            result.contains("Performance"),
            "Should contain performance information"
        );

        logger.info("Null format handling test passed");
    }

    @Test
    @Order(3)
    @DisplayName("Test getFormattedReport with all valid formats")
    void testGetFormattedReportAllFormats() {
        // Add some test data first
        metrics.recordSearch("test-search", 100, 5, true);

        for (ReportFormat format : ReportFormat.values()) {
            String result = metrics.getFormattedReport(format);
            assertNotNull(
                result,
                "Result should not be null for format: " + format
            );
            assertFalse(
                result.isEmpty(),
                "Result should not be empty for format: " + format
            );

            // Verify format-specific content
            switch (format) {
                case JSON:
                    assertTrue(
                        result.contains("{") && result.contains("}"),
                        "JSON format should contain braces"
                    );
                    break;
                case CSV:
                    assertTrue(
                        result.contains(","),
                        "CSV format should contain commas"
                    );
                    break;
                case SUMMARY:
                case DETAILED:
                    assertTrue(
                        result.contains("Performance"),
                        "Summary/Detailed should contain performance info"
                    );
                    break;
            }
        }

        logger.info("All format types test passed");
    }

    @Test
    @Order(4)
    @DisplayName("Test PerformanceInsights constructor with null values")
    void testPerformanceInsightsConstructorNulls() {
        // Test with all null string values
        PerformanceInsights insights = new PerformanceInsights(
            null, // fastestSearchType
            null, // slowestSearchType
            50.0, // overallCacheHitRate
            100.5, // averageSearchTime
            10L, // totalOperations
            null // performanceRating
        );

        assertNotNull(insights, "PerformanceInsights should be created");
        assertEquals(
            "N/A",
            insights.getFastestSearchType(),
            "Null fastest should become N/A"
        );
        assertEquals(
            "N/A",
            insights.getSlowestSearchType(),
            "Null slowest should become N/A"
        );
        assertEquals(
            "Unknown",
            insights.getPerformanceRating(),
            "Null rating should become Unknown"
        );
        assertEquals(
            50.0,
            insights.getOverallCacheHitRate(),
            0.01,
            "Cache hit rate should be preserved"
        );
        assertEquals(
            100.5,
            insights.getAverageSearchTime(),
            0.01,
            "Average time should be preserved"
        );
        assertEquals(
            10L,
            insights.getTotalOperations(),
            "Total operations should be preserved"
        );

        logger.info("PerformanceInsights null constructor test passed");
    }

    @Test
    @Order(5)
    @DisplayName("Test PerformanceInsights constructor with NaN values")
    void testPerformanceInsightsConstructorNaN() {
        // Test with NaN double values
        PerformanceInsights insights = new PerformanceInsights(
            "fast-search",
            "slow-search",
            Double.NaN, // overallCacheHitRate
            Double.NaN, // averageSearchTime
            5L,
            "Good"
        );

        assertNotNull(insights, "PerformanceInsights should be created");
        assertEquals(
            0.0,
            insights.getOverallCacheHitRate(),
            0.01,
            "NaN cache hit rate should become 0.0"
        );
        assertEquals(
            0.0,
            insights.getAverageSearchTime(),
            0.01,
            "NaN average time should become 0.0"
        );
        assertEquals(
            "fast-search",
            insights.getFastestSearchType(),
            "Valid strings should be preserved"
        );
        assertEquals(
            "slow-search",
            insights.getSlowestSearchType(),
            "Valid strings should be preserved"
        );
        assertEquals(
            "Good",
            insights.getPerformanceRating(),
            "Valid rating should be preserved"
        );

        logger.info("PerformanceInsights NaN constructor test passed");
    }

    @Test
    @Order(6)
    @DisplayName("Test PerformanceInsights constructor with negative values")
    void testPerformanceInsightsConstructorNegative() {
        // Test with negative totalOperations
        PerformanceInsights insights = new PerformanceInsights(
            "test-fast",
            "test-slow",
            -10.0, // negative cache hit rate
            -50.0, // negative average time
            -100L, // negative total operations
            "Bad"
        );

        assertNotNull(insights, "PerformanceInsights should be created");
        assertEquals(
            0L,
            insights.getTotalOperations(),
            "Negative operations should become 0"
        );
        // Note: negative rates and times are allowed as they might represent calculation errors
        assertEquals(
            -10.0,
            insights.getOverallCacheHitRate(),
            0.01,
            "Negative rates preserved for debugging"
        );
        assertEquals(
            -50.0,
            insights.getAverageSearchTime(),
            0.01,
            "Negative times preserved for debugging"
        );

        logger.info("PerformanceInsights negative values test passed");
    }

    @Test
    @Order(7)
    @DisplayName("Test PerformanceInsights getSummary")
    void testPerformanceInsightsGetSummary() {
        PerformanceInsights insights = new PerformanceInsights(
            "hash-search",
            "linear-search",
            75.5,
            25.3,
            150L,
            "Excellent"
        );

        String summary = insights.getSummary();
        assertNotNull(summary, "Summary should not be null");
        assertFalse(summary.isEmpty(), "Summary should not be empty");

        // Verify content
        assertTrue(
            summary.contains("hash-search"),
            "Should contain fastest search type"
        );
        assertTrue(
            summary.contains("linear-search"),
            "Should contain slowest search type"
        );
        assertTrue(summary.contains("75.5%"), "Should contain cache hit rate");
        assertTrue(summary.contains("25.30 ms"), "Should contain average time");
        assertTrue(summary.contains("150"), "Should contain total operations");
        assertTrue(
            summary.contains("Excellent"),
            "Should contain performance rating"
        );

        logger.info("PerformanceInsights getSummary test passed");
    }

    @Test
    @Order(8)
    @DisplayName("Test PerformanceInsights with edge case values")
    void testPerformanceInsightsEdgeCases() {
        // Test with very large numbers
        PerformanceInsights insights1 = new PerformanceInsights(
            "type1",
            "type2",
            999.99,
            Double.MAX_VALUE,
            Long.MAX_VALUE,
            "rating"
        );

        assertNotNull(insights1, "Should handle large numbers");
        assertEquals(Double.MAX_VALUE, insights1.getAverageSearchTime());
        assertEquals(Long.MAX_VALUE, insights1.getTotalOperations());

        // Test with very small numbers
        PerformanceInsights insights2 = new PerformanceInsights(
            "type1",
            "type2",
            0.001,
            0.0001,
            1L,
            "minimal"
        );

        assertNotNull(insights2, "Should handle small numbers");
        assertEquals(0.001, insights2.getOverallCacheHitRate(), 0.0001);
        assertEquals(0.0001, insights2.getAverageSearchTime(), 0.00001);

        logger.info("PerformanceInsights edge cases test passed");
    }

    @Test
    @Order(9)
    @DisplayName("Test getPerformanceInsights with no data")
    void testGetPerformanceInsightsNoData() {
        // Test with empty metrics
        PerformanceInsights insights = metrics.getPerformanceInsights();
        assertNotNull(
            insights,
            "PerformanceInsights should not be null even with no data"
        );

        assertEquals(
            "N/A",
            insights.getFastestSearchType(),
            "Should show N/A for fastest with no data"
        );
        assertEquals(
            "N/A",
            insights.getSlowestSearchType(),
            "Should show N/A for slowest with no data"
        );
        assertEquals(
            0.0,
            insights.getOverallCacheHitRate(),
            0.01,
            "Should show 0.0 cache hit rate with no data"
        );
        assertEquals(
            0.0,
            insights.getAverageSearchTime(),
            0.01,
            "Should show 0.0 average time with no data"
        );
        assertEquals(
            0L,
            insights.getTotalOperations(),
            "Should show 0 operations with no data"
        );
        assertEquals(
            "No data",
            insights.getPerformanceRating(),
            "Should show 'No data' rating"
        );

        logger.info("PerformanceInsights no data test passed");
    }

    @Test
    @Order(10)
    @DisplayName("Test getPerformanceInsights with single data point")
    void testGetPerformanceInsightsSingleData() {
        // Record single search
        metrics.recordSearch("single-search", 50, 3, true);

        PerformanceInsights insights = metrics.getPerformanceInsights();
        assertNotNull(insights, "PerformanceInsights should not be null");

        assertEquals(
            "single-search",
            insights.getFastestSearchType(),
            "Single search should be fastest"
        );
        assertEquals(
            "single-search",
            insights.getSlowestSearchType(),
            "Single search should be slowest"
        );
        assertTrue(
            insights.getOverallCacheHitRate() > 0,
            "Should have positive cache hit rate"
        );
        assertTrue(
            insights.getAverageSearchTime() > 0,
            "Should have positive average time"
        );
        assertEquals(
            1L,
            insights.getTotalOperations(),
            "Should have 1 operation"
        );

        logger.info("PerformanceInsights single data test passed");
    }

    @Test
    @Order(11)
    @DisplayName("Test report format consistency")
    void testReportFormatConsistency() {
        // Add test data
        metrics.recordSearch("search-1", 100, 5, true);
        metrics.recordSearch("search-2", 200, 10, false);

        // Get reports multiple times and verify consistency
        for (ReportFormat format : ReportFormat.values()) {
            String report1 = metrics.getFormattedReport(format);
            String report2 = metrics.getFormattedReport(format);

            assertEquals(
                report1,
                report2,
                "Report format " + format + " should be consistent across calls"
            );
        }

        logger.info("Report format consistency test passed");
    }

    @Test
    @Order(12)
    @DisplayName("Test JSON report format structure")
    void testJsonReportStructure() {
        // Add test data
        metrics.recordSearch("json-test", 150, 8, true);

        String jsonReport = metrics.getFormattedReport(ReportFormat.JSON);
        assertNotNull(jsonReport, "JSON report should not be null");

        // Basic JSON structure validation
        assertTrue(jsonReport.startsWith("{"), "JSON should start with {");
        assertTrue(jsonReport.endsWith("}"), "JSON should end with }");
        assertTrue(jsonReport.contains("\""), "JSON should contain quotes");

        // Check for expected JSON fields
        assertTrue(
            jsonReport.contains("insights"),
            "JSON should contain insights field"
        );

        logger.info("JSON report structure test passed");
    }

    @Test
    @Order(13)
    @DisplayName("Test CSV report format structure")
    void testCsvReportStructure() {
        // Add test data
        metrics.recordSearch("csv-test", 75, 12, false);

        String csvReport = metrics.getFormattedReport(ReportFormat.CSV);
        assertNotNull(csvReport, "CSV report should not be null");

        // Basic CSV structure validation
        assertTrue(csvReport.contains(","), "CSV should contain commas");

        // Should have header row and data rows
        String[] lines = csvReport.split("\n");
        assertTrue(
            lines.length >= 2,
            "CSV should have at least header and one data row"
        );

        logger.info("CSV report structure test passed");
    }

    @Test
    @Order(14)
    @DisplayName("Test performance insights with mixed search performance")
    void testPerformanceInsightsMixedPerformance() {
        // Record searches with varying performance
        metrics.recordSearch("fast-search", 10, 100, true); // Fast search
        metrics.recordSearch("slow-search", 500, 5, false); // Slow search
        metrics.recordSearch("medium-search", 100, 20, true); // Medium search

        PerformanceInsights insights = metrics.getPerformanceInsights();
        assertNotNull(insights, "PerformanceInsights should not be null");

        // Verify fastest/slowest detection
        assertEquals(
            "fast-search",
            insights.getFastestSearchType(),
            "Should identify fastest search"
        );
        assertEquals(
            "slow-search",
            insights.getSlowestSearchType(),
            "Should identify slowest search"
        );

        // Verify aggregated metrics
        assertTrue(
            insights.getTotalOperations() > 0,
            "Should have positive total operations"
        );
        assertTrue(
            insights.getAverageSearchTime() > 0,
            "Should have positive average time"
        );
        assertTrue(
            insights.getOverallCacheHitRate() >= 0 &&
            insights.getOverallCacheHitRate() <= 100,
            "Cache hit rate should be between 0 and 100"
        );

        logger.info(
            "Mixed performance test passed - fastest: {}, slowest: {}",
            insights.getFastestSearchType(),
            insights.getSlowestSearchType()
        );
    }
}
