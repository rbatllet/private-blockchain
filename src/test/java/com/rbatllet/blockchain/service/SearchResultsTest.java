package com.rbatllet.blockchain.service;

import static org.junit.jupiter.api.Assertions.*;

import com.rbatllet.blockchain.entity.Block;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Comprehensive tests for SearchResults class robustness and defensive programming.
 * Tests all public methods, edge cases, and null safety patterns.
 */
@DisplayName("🔍 SearchResults Robustness Tests")
class SearchResultsTest {

    private static final Logger logger = LoggerFactory.getLogger(SearchResultsTest.class);
    
    private List<Block> testBlocks;
    private Block testBlock1;
    private Block testBlock2;

    @BeforeEach
    void setUp() {
        // Create test blocks for testing
        testBlock1 = new Block(1L, "prevHash1", "Test data 1", 
                              LocalDateTime.now(), "hash1", "sig1", "pubKey1");
        testBlock2 = new Block(2L, "prevHash2", "Test data 2", 
                              LocalDateTime.now(), "hash2", "sig2", "pubKey2");
        testBlocks = Arrays.asList(testBlock1, testBlock2);
        logger.info("Test setup completed with {} test blocks", testBlocks.size());
    }

    @Test
    @DisplayName("🛡️ Constructor: Defensive programming with null inputs")
    void testConstructorDefensiveProgramming() {
        // Test with null query
        SearchResults resultsNullQuery = new SearchResults(null, testBlocks);
        assertNotNull(resultsNullQuery.getQuery());
        assertEquals("", resultsNullQuery.getQuery());
        assertEquals(2, resultsNullQuery.getResultCount());
        
        // Test with null blocks
        SearchResults resultsNullBlocks = new SearchResults("test query", null);
        assertEquals("test query", resultsNullBlocks.getQuery());
        assertNotNull(resultsNullBlocks.getBlocks());
        assertTrue(resultsNullBlocks.getBlocks().isEmpty());
        assertEquals(0, resultsNullBlocks.getResultCount());
        
        // Test with both null
        SearchResults resultsBothNull = new SearchResults(null, null);
        assertEquals("", resultsBothNull.getQuery());
        assertNotNull(resultsBothNull.getBlocks());
        assertTrue(resultsBothNull.getBlocks().isEmpty());
        assertEquals(0, resultsBothNull.getResultCount());
        
        logger.info("✅ Constructor defensive programming test passed");
    }

    @Test
    @DisplayName("📄 getQuery(): Null safety validation")
    void testGetQueryNullSafety() {
        // Test normal case
        SearchResults normalResults = new SearchResults("blockchain search", testBlocks);
        assertEquals("blockchain search", normalResults.getQuery());
        
        // Test null query (handled by constructor)
        SearchResults nullQueryResults = new SearchResults(null, testBlocks);
        String query = nullQueryResults.getQuery();
        assertNotNull(query);
        assertEquals("", query);
        
        logger.info("✅ getQuery null safety test passed");
    }

    @Test
    @DisplayName("⏰ getTimestamp(): Always returns valid timestamp")
    void testGetTimestamp() {
        LocalDateTime beforeCreation = LocalDateTime.now().minusSeconds(1);
        SearchResults results = new SearchResults("test", testBlocks);
        LocalDateTime afterCreation = LocalDateTime.now().plusSeconds(1);
        
        LocalDateTime timestamp = results.getTimestamp();
        assertNotNull(timestamp);
        assertTrue(timestamp.isAfter(beforeCreation));
        assertTrue(timestamp.isBefore(afterCreation));
        
        logger.info("✅ getTimestamp test passed");
    }

    @Test
    @DisplayName("📊 getMetrics(): Always returns valid SearchMetrics")
    void testGetMetrics() {
        SearchResults results = new SearchResults("test", testBlocks);
        
        SearchMetrics metrics = results.getMetrics();
        assertNotNull(metrics);
        assertEquals(0, metrics.getTotalSearches()); // New instance, no searches recorded yet
        
        logger.info("✅ getMetrics test passed");
    }

    @Test
    @DisplayName("⚠️ getWarnings(): Immutable list access")
    void testGetWarnings() {
        SearchResults results = new SearchResults("test", testBlocks);
        
        // Test initial state
        List<String> warnings = results.getWarnings();
        assertNotNull(warnings);
        assertTrue(warnings.isEmpty());
        
        // Add warning and test immutability
        results.addWarning("Test warning");
        List<String> updatedWarnings = results.getWarnings();
        assertEquals(1, updatedWarnings.size());
        assertEquals("Test warning", updatedWarnings.get(0));
        
        // Test immutability - should throw exception
        assertThrows(UnsupportedOperationException.class, () -> {
            updatedWarnings.add("Should fail");
        });
        
        logger.info("✅ getWarnings immutability test passed");
    }

    @Test
    @DisplayName("📝 getSearchDetails(): Immutable map access")
    void testGetSearchDetails() {
        SearchResults results = new SearchResults("test", testBlocks);
        
        // Test initial state
        Map<String, Object> details = results.getSearchDetails();
        assertNotNull(details);
        assertTrue(details.isEmpty());
        
        // Add detail and test immutability
        results.addDetail("searchType", "KEYWORD");
        results.addDetail("maxResults", 100);
        
        Map<String, Object> updatedDetails = results.getSearchDetails();
        assertEquals(2, updatedDetails.size());
        assertEquals("KEYWORD", updatedDetails.get("searchType"));
        assertEquals(100, updatedDetails.get("maxResults"));
        
        // Test immutability - should throw exception
        assertThrows(UnsupportedOperationException.class, () -> {
            updatedDetails.put("shouldFail", "fail");
        });
        
        logger.info("✅ getSearchDetails immutability test passed");
    }

    @Test
    @DisplayName("✅ hasResults(): Null-safe block validation")
    void testHasResultsNullSafety() {
        // Test with blocks
        SearchResults withResults = new SearchResults("test", testBlocks);
        assertTrue(withResults.hasResults());
        
        // Test with empty blocks
        SearchResults emptyResults = new SearchResults("test", new ArrayList<>());
        assertFalse(emptyResults.hasResults());
        
        // Test with null blocks (handled by constructor)
        SearchResults nullBlocksResults = new SearchResults("test", null);
        assertFalse(nullBlocksResults.hasResults());
        
        logger.info("✅ hasResults null safety test passed");
    }

    @Test
    @DisplayName("📄 toString(): Comprehensive null safety")
    void testToStringNullSafety() {
        // Test normal case
        SearchResults normalResults = new SearchResults("blockchain search", testBlocks);
        normalResults.addWarning("Test warning");
        normalResults.addDetail("type", "KEYWORD");
        
        String normalOutput = normalResults.toString();
        assertNotNull(normalOutput);
        assertTrue(normalOutput.contains("blockchain search"));
        assertTrue(normalOutput.contains("2 blocks found"));
        assertTrue(normalOutput.contains("Test warning"));
        assertTrue(normalOutput.contains("type: KEYWORD"));
        
        // Test with null inputs (handled by constructor)
        SearchResults nullInputResults = new SearchResults(null, null);
        String nullOutput = nullInputResults.toString();
        assertNotNull(nullOutput);
        assertTrue(nullOutput.contains("Query: \"\""));
        assertTrue(nullOutput.contains("0 blocks found"));
        assertFalse(nullOutput.contains("null")); // Should not contain raw nulls
        
        logger.info("✅ toString null safety test passed");
    }

    @Test
    @DisplayName("🔧 Builder methods: Fluent API functionality")
    void testBuilderMethods() {
        SearchResults results = new SearchResults("test", testBlocks)
                .addDetail("searchType", "REGEX")
                .addDetail("maxResults", 50)
                .addWarning("Performance warning")
                .addWarning("Cache miss");
        
        // Test fluent API returns same instance
        assertNotNull(results);
        assertEquals("test", results.getQuery());
        assertEquals(2, results.getResultCount());
        
        // Test added details
        Map<String, Object> details = results.getSearchDetails();
        assertEquals("REGEX", details.get("searchType"));
        assertEquals(50, details.get("maxResults"));
        
        // Test added warnings
        List<String> warnings = results.getWarnings();
        assertEquals(2, warnings.size());
        assertTrue(warnings.contains("Performance warning"));
        assertTrue(warnings.contains("Cache miss"));
        
        logger.info("✅ Builder methods test passed");
    }

    @Test
    @DisplayName("📈 withMetrics(): Search metrics integration")
    void testWithMetricsIntegration() {
        SearchResults results = new SearchResults("test", testBlocks);
        
        // Test metrics recording
        SearchResults updatedResults = results.withMetrics(
                150L,      // searchTimeMs
                2,         // onChainResults  
                1,         // offChainResults
                true,      // cacheHit
                "KEYWORD"  // searchType
        );
        
        // Should return same instance (fluent API)
        assertSame(results, updatedResults);
        
        // Verify metrics were recorded
        SearchMetrics metrics = results.getMetrics();
        assertNotNull(metrics);
        assertEquals(1, metrics.getTotalSearches()); // One search recorded
        
        logger.info("✅ withMetrics integration test passed");
    }

    @Test
    @DisplayName("📊 Edge cases: Empty and extreme values")
    void testEdgeCases() {
        // Test empty query
        SearchResults emptyQuery = new SearchResults("", testBlocks);
        assertEquals("", emptyQuery.getQuery());
        assertTrue(emptyQuery.hasResults());
        
        // Test empty blocks
        SearchResults emptyBlocks = new SearchResults("test", new ArrayList<>());
        assertEquals("test", emptyBlocks.getQuery());
        assertFalse(emptyBlocks.hasResults());
        assertEquals(0, emptyBlocks.getResultCount());
        
        // Test very long query
        String longQuery = "a".repeat(10000);
        SearchResults longQueryResults = new SearchResults(longQuery, testBlocks);
        assertEquals(longQuery, longQueryResults.getQuery());
        
        // Test special characters in query
        SearchResults specialChars = new SearchResults("test@#$%^&*()", testBlocks);
        assertEquals("test@#$%^&*()", specialChars.getQuery());
        
        logger.info("✅ Edge cases test passed");
    }
}