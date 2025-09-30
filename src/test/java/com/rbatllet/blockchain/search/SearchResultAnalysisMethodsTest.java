package com.rbatllet.blockchain.search;

import com.rbatllet.blockchain.config.EncryptionConfig;
import com.rbatllet.blockchain.search.SearchFrameworkEngine.EnhancedSearchResult;
import com.rbatllet.blockchain.search.SearchFrameworkEngine.SearchResult;
import com.rbatllet.blockchain.search.strategy.SearchStrategyRouter;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

/**
 * Comprehensive tests for new analysis methods in SearchResult:
 * - getTopResults(int limit)
 * - getAverageRelevanceScore()
 * - groupBySource()
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SearchResultAnalysisMethodsTest {

    @Test
    @Order(1)
    @DisplayName("getTopResults() - Basic functionality with sorted results")
    void testGetTopResultsBasic() {
        // Create results with different scores
        List<EnhancedSearchResult> results = createTestResults(
            new double[]{0.9, 0.5, 0.8, 0.3, 0.7}
        );

        SearchResult searchResult = createSearchResult(results);

        // Get top 3
        List<EnhancedSearchResult> top3 = searchResult.getTopResults(3);

        assertEquals(3, top3.size(), "Should return exactly 3 results");
        assertEquals(0.9, top3.get(0).getRelevanceScore(), 0.001, "First should be highest score");
        assertEquals(0.8, top3.get(1).getRelevanceScore(), 0.001, "Second should be second highest");
        assertEquals(0.7, top3.get(2).getRelevanceScore(), 0.001, "Third should be third highest");
    }

    @Test
    @Order(2)
    @DisplayName("getTopResults() - Limit larger than result count")
    void testGetTopResultsLimitTooLarge() {
        List<EnhancedSearchResult> results = createTestResults(new double[]{0.5, 0.3});

        SearchResult searchResult = createSearchResult(results);

        List<EnhancedSearchResult> top10 = searchResult.getTopResults(10);

        assertEquals(2, top10.size(), "Should return all available results");
        assertEquals(0.5, top10.get(0).getRelevanceScore(), 0.001);
        assertEquals(0.3, top10.get(1).getRelevanceScore(), 0.001);
    }

    @Test
    @Order(3)
    @DisplayName("getTopResults() - Zero limit")
    void testGetTopResultsZeroLimit() {
        List<EnhancedSearchResult> results = createTestResults(new double[]{0.9, 0.8, 0.7});

        SearchResult searchResult = createSearchResult(results);

        List<EnhancedSearchResult> top0 = searchResult.getTopResults(0);

        assertTrue(top0.isEmpty(), "Should return empty list for zero limit");
    }

    @Test
    @Order(4)
    @DisplayName("getTopResults() - Negative limit (defensive programming)")
    void testGetTopResultsNegativeLimit() {
        List<EnhancedSearchResult> results = createTestResults(new double[]{0.9, 0.8});

        SearchResult searchResult = createSearchResult(results);

        List<EnhancedSearchResult> topNegative = searchResult.getTopResults(-5);

        assertTrue(topNegative.isEmpty(), "Should handle negative limit gracefully");
    }

    @Test
    @Order(5)
    @DisplayName("getTopResults() - Empty results")
    void testGetTopResultsEmptyResults() {
        SearchResult searchResult = createSearchResult(new ArrayList<>());

        List<EnhancedSearchResult> top5 = searchResult.getTopResults(5);

        assertTrue(top5.isEmpty(), "Should return empty list for empty results");
    }

    @Test
    @Order(6)
    @DisplayName("getTopResults() - Null result in list (defensive)")
    void testGetTopResultsWithNullElements() {
        List<EnhancedSearchResult> results = new ArrayList<>();
        results.add(createResult(0.9, SearchStrategyRouter.SearchResultSource.PUBLIC_METADATA));
        results.add(null);  // Null element
        results.add(createResult(0.7, SearchStrategyRouter.SearchResultSource.PUBLIC_METADATA));

        SearchResult searchResult = createSearchResult(results);

        List<EnhancedSearchResult> top3 = searchResult.getTopResults(3);

        assertEquals(2, top3.size(), "Should filter out null results");
        assertNotNull(top3.get(0), "All returned results should be non-null");
        assertNotNull(top3.get(1), "All returned results should be non-null");
    }

    @Test
    @Order(7)
    @DisplayName("getTopResults() - Equal scores (stable sort)")
    void testGetTopResultsEqualScores() {
        List<EnhancedSearchResult> results = createTestResults(
            new double[]{0.8, 0.8, 0.8}
        );

        SearchResult searchResult = createSearchResult(results);

        List<EnhancedSearchResult> top2 = searchResult.getTopResults(2);

        assertEquals(2, top2.size(), "Should return requested count");
        assertEquals(0.8, top2.get(0).getRelevanceScore(), 0.001);
        assertEquals(0.8, top2.get(1).getRelevanceScore(), 0.001);
    }

    @Test
    @Order(8)
    @DisplayName("getAverageRelevanceScore() - Basic calculation")
    void testGetAverageRelevanceScoreBasic() {
        List<EnhancedSearchResult> results = createTestResults(
            new double[]{0.9, 0.7, 0.5, 0.3}  // Average = 0.6
        );

        SearchResult searchResult = createSearchResult(results);

        double average = searchResult.getAverageRelevanceScore();

        assertEquals(0.6, average, 0.001, "Should calculate correct average");
    }

    @Test
    @Order(9)
    @DisplayName("getAverageRelevanceScore() - Single result")
    void testGetAverageRelevanceScoreSingleResult() {
        List<EnhancedSearchResult> results = createTestResults(new double[]{0.85});

        SearchResult searchResult = createSearchResult(results);

        double average = searchResult.getAverageRelevanceScore();

        assertEquals(0.85, average, 0.001, "Single result should return its own score");
    }

    @Test
    @Order(10)
    @DisplayName("getAverageRelevanceScore() - Empty results")
    void testGetAverageRelevanceScoreEmpty() {
        SearchResult searchResult = createSearchResult(new ArrayList<>());

        double average = searchResult.getAverageRelevanceScore();

        assertEquals(0.0, average, 0.001, "Empty results should return 0.0");
    }

    @Test
    @Order(11)
    @DisplayName("getAverageRelevanceScore() - NaN handling (defensive)")
    void testGetAverageRelevanceScoreNaN() {
        List<EnhancedSearchResult> results = new ArrayList<>();
        results.add(createResult(Double.NaN, SearchStrategyRouter.SearchResultSource.PUBLIC_METADATA));
        results.add(createResult(0.5, SearchStrategyRouter.SearchResultSource.PUBLIC_METADATA));
        results.add(createResult(0.7, SearchStrategyRouter.SearchResultSource.PUBLIC_METADATA));

        SearchResult searchResult = createSearchResult(results);

        double average = searchResult.getAverageRelevanceScore();

        // NaN treated as 0.0: (0.0 + 0.5 + 0.7) / 3 = 1.2 / 3 = 0.4
        assertEquals(0.4, average, 0.001, "Should handle NaN by treating as 0.0");
    }

    @Test
    @Order(12)
    @DisplayName("getAverageRelevanceScore() - Infinity handling (defensive)")
    void testGetAverageRelevanceScoreInfinity() {
        List<EnhancedSearchResult> results = new ArrayList<>();
        results.add(createResult(Double.POSITIVE_INFINITY, SearchStrategyRouter.SearchResultSource.PUBLIC_METADATA));
        results.add(createResult(0.5, SearchStrategyRouter.SearchResultSource.PUBLIC_METADATA));

        SearchResult searchResult = createSearchResult(results);

        double average = searchResult.getAverageRelevanceScore();

        assertEquals(0.25, average, 0.001, "Should handle Infinity by treating as 0.0");
    }

    @Test
    @Order(13)
    @DisplayName("getAverageRelevanceScore() - All zeros")
    void testGetAverageRelevanceScoreAllZeros() {
        List<EnhancedSearchResult> results = createTestResults(new double[]{0.0, 0.0, 0.0});

        SearchResult searchResult = createSearchResult(results);

        double average = searchResult.getAverageRelevanceScore();

        assertEquals(0.0, average, 0.001, "All zeros should average to 0.0");
    }

    @Test
    @Order(14)
    @DisplayName("groupBySource() - Basic grouping")
    void testGroupBySourceBasic() {
        List<EnhancedSearchResult> results = new ArrayList<>();
        results.add(createResult(0.9, SearchStrategyRouter.SearchResultSource.PUBLIC_METADATA));
        results.add(createResult(0.8, SearchStrategyRouter.SearchResultSource.OFF_CHAIN_CONTENT));
        results.add(createResult(0.7, SearchStrategyRouter.SearchResultSource.PUBLIC_METADATA));
        results.add(createResult(0.6, SearchStrategyRouter.SearchResultSource.ENCRYPTED_CONTENT));

        SearchResult searchResult = createSearchResult(results);

        Map<SearchStrategyRouter.SearchResultSource, List<EnhancedSearchResult>> grouped =
            searchResult.groupBySource();

        assertEquals(3, grouped.size(), "Should have 3 different sources");
        assertEquals(2, grouped.get(SearchStrategyRouter.SearchResultSource.PUBLIC_METADATA).size());
        assertEquals(1, grouped.get(SearchStrategyRouter.SearchResultSource.OFF_CHAIN_CONTENT).size());
        assertEquals(1, grouped.get(SearchStrategyRouter.SearchResultSource.ENCRYPTED_CONTENT).size());
    }

    @Test
    @Order(15)
    @DisplayName("groupBySource() - All same source")
    void testGroupBySourceAllSame() {
        List<EnhancedSearchResult> results = new ArrayList<>();
        results.add(createResult(0.9, SearchStrategyRouter.SearchResultSource.PUBLIC_METADATA));
        results.add(createResult(0.8, SearchStrategyRouter.SearchResultSource.PUBLIC_METADATA));
        results.add(createResult(0.7, SearchStrategyRouter.SearchResultSource.PUBLIC_METADATA));

        SearchResult searchResult = createSearchResult(results);

        Map<SearchStrategyRouter.SearchResultSource, List<EnhancedSearchResult>> grouped =
            searchResult.groupBySource();

        assertEquals(1, grouped.size(), "Should have only 1 source group");
        assertEquals(3, grouped.get(SearchStrategyRouter.SearchResultSource.PUBLIC_METADATA).size());
    }

    @Test
    @Order(16)
    @DisplayName("groupBySource() - Empty results")
    void testGroupBySourceEmpty() {
        SearchResult searchResult = createSearchResult(new ArrayList<>());

        Map<SearchStrategyRouter.SearchResultSource, List<EnhancedSearchResult>> grouped =
            searchResult.groupBySource();

        assertTrue(grouped.isEmpty(), "Should return empty map for empty results");
    }

    @Test
    @Order(17)
    @DisplayName("groupBySource() - Null result in list (defensive)")
    void testGroupBySourceWithNullElements() {
        List<EnhancedSearchResult> results = new ArrayList<>();
        results.add(createResult(0.9, SearchStrategyRouter.SearchResultSource.PUBLIC_METADATA));
        results.add(null);  // Null element
        results.add(createResult(0.7, SearchStrategyRouter.SearchResultSource.OFF_CHAIN_CONTENT));

        SearchResult searchResult = createSearchResult(results);

        Map<SearchStrategyRouter.SearchResultSource, List<EnhancedSearchResult>> grouped =
            searchResult.groupBySource();

        assertEquals(2, grouped.size(), "Should skip null results");
        assertEquals(1, grouped.get(SearchStrategyRouter.SearchResultSource.PUBLIC_METADATA).size());
        assertEquals(1, grouped.get(SearchStrategyRouter.SearchResultSource.OFF_CHAIN_CONTENT).size());
    }

    @Test
    @Order(18)
    @DisplayName("groupBySource() - Returns immutable map (defensive)")
    void testGroupBySourceImmutable() {
        List<EnhancedSearchResult> results = createTestResults(new double[]{0.9});

        SearchResult searchResult = createSearchResult(results);

        Map<SearchStrategyRouter.SearchResultSource, List<EnhancedSearchResult>> grouped =
            searchResult.groupBySource();

        assertThrows(UnsupportedOperationException.class, () -> {
            grouped.put(SearchStrategyRouter.SearchResultSource.ENCRYPTED_CONTENT, new ArrayList<>());
        }, "Returned map should be immutable");
    }

    // Helper methods

    private List<EnhancedSearchResult> createTestResults(double[] scores) {
        List<EnhancedSearchResult> results = new ArrayList<>();
        for (double score : scores) {
            results.add(createResult(score, SearchStrategyRouter.SearchResultSource.PUBLIC_METADATA));
        }
        return results;
    }

    private EnhancedSearchResult createResult(double score, SearchStrategyRouter.SearchResultSource source) {
        return new EnhancedSearchResult(
            "hash_" + UUID.randomUUID().toString(),
            score,
            source,
            "Test summary",
            10.0,
            null,  // publicMetadata
            null,  // privateMetadata
            EncryptionConfig.SecurityLevel.BALANCED
        );
    }

    private SearchResult createSearchResult(List<EnhancedSearchResult> results) {
        return new SearchResult(
            results,
            SearchStrategyRouter.SearchStrategy.FAST_PUBLIC,
            null,  // analysis
            100.0,  // totalTimeMs
            SearchLevel.FAST_ONLY,
            null  // errorMessage
        );
    }
}