package com.rbatllet.blockchain.service;

import com.rbatllet.blockchain.entity.Block;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.*;

/**
 * Comprehensive test suite for AdvancedSearchResult robustness validation.
 * Tests defensive programming patterns, null safety, and edge case handling.
 */
class AdvancedSearchResultTest {

    private Block testBlock;

    @BeforeEach
    void setUp() {
        // Create test block
        testBlock = new Block();
        testBlock.setContentCategory("TEST_CATEGORY");
    }

    @Test
    @DisplayName("Constructor should handle null searchQuery defensively")
    void testConstructorNullSearchQuery() {
        AdvancedSearchResult result = new AdvancedSearchResult(
            null, // null searchQuery
            AdvancedSearchResult.SearchType.KEYWORD_SEARCH,
            Duration.ofMillis(100)
        );
        
        assertEquals("", result.getSearchQuery());
    }

    @Test
    @DisplayName("Constructor should handle null searchType defensively") 
    void testConstructorNullSearchType() {
        AdvancedSearchResult result = new AdvancedSearchResult(
            "test query",
            null, // null searchType
            Duration.ofMillis(100)
        );
        
        assertEquals(AdvancedSearchResult.SearchType.KEYWORD_SEARCH, result.getSearchType());
    }

    @Test
    @DisplayName("Constructor should handle null searchDuration defensively")
    void testConstructorNullSearchDuration() {
        AdvancedSearchResult result = new AdvancedSearchResult(
            "test query",
            AdvancedSearchResult.SearchType.KEYWORD_SEARCH,
            null // null searchDuration
        );
        
        assertEquals(Duration.ZERO, result.getSearchDuration());
    }

    @Test
    @DisplayName("getTopMatches should handle empty matches collection")
    void testGetTopMatchesEmptyCollection() {
        AdvancedSearchResult result = new AdvancedSearchResult(
            "test query",
            AdvancedSearchResult.SearchType.KEYWORD_SEARCH,
            Duration.ofMillis(100)
        );
        
        List<AdvancedSearchResult.SearchMatch> topMatches = result.getTopMatches(5);
        assertNotNull(topMatches);
        assertTrue(topMatches.isEmpty());
    }

    @Test
    @DisplayName("getTopMatches should handle negative limit")
    void testGetTopMatchesNegativeLimit() {
        // Create result with some matches
        AdvancedSearchResult result = createResultWithMatches();
        
        List<AdvancedSearchResult.SearchMatch> topMatches = result.getTopMatches(-5);
        assertNotNull(topMatches);
        assertTrue(topMatches.isEmpty());
    }

    @Test
    @DisplayName("groupByCategory should handle empty matches")
    void testGroupByCategoryEmptyMatches() {
        AdvancedSearchResult result = new AdvancedSearchResult(
            "test query",
            AdvancedSearchResult.SearchType.KEYWORD_SEARCH,
            Duration.ofMillis(100)
        );
        
        Map<String, List<AdvancedSearchResult.SearchMatch>> grouped = result.groupByCategory();
        assertNotNull(grouped);
        assertTrue(grouped.isEmpty());
    }

    @Test
    @DisplayName("getAverageRelevanceScore should handle empty matches")
    void testGetAverageRelevanceScoreEmptyMatches() {
        AdvancedSearchResult result = new AdvancedSearchResult(
            "test query",
            AdvancedSearchResult.SearchType.KEYWORD_SEARCH,
            Duration.ofMillis(100)
        );
        
        double avgScore = result.getAverageRelevanceScore();
        assertEquals(0.0, avgScore);
    }

    @Test
    @DisplayName("getSuggestedRefinements should handle empty collection")
    void testGetSuggestedRefinementsEmptyCollection() {
        AdvancedSearchResult result = new AdvancedSearchResult(
            "test query",
            AdvancedSearchResult.SearchType.KEYWORD_SEARCH,
            Duration.ofMillis(100)
        );
        
        List<String> refinements = result.getSuggestedRefinements();
        assertNotNull(refinements);
        assertTrue(refinements.isEmpty());
    }

    @Test
    @DisplayName("getCategoryDistribution should handle empty collection")
    void testGetCategoryDistributionEmptyCollection() {
        AdvancedSearchResult result = new AdvancedSearchResult(
            "test query",
            AdvancedSearchResult.SearchType.KEYWORD_SEARCH,
            Duration.ofMillis(100)
        );
        
        Map<String, Integer> distribution = result.getCategoryDistribution();
        assertNotNull(distribution);
        assertTrue(distribution.isEmpty());
    }

    @Test
    @DisplayName("All collections should return immutable views")
    void testImmutabilityOfReturnedCollections() {
        AdvancedSearchResult result = createResultWithMatches();
        
        // Test getTopMatches immutability
        List<AdvancedSearchResult.SearchMatch> topMatches = result.getTopMatches(5);
        assertThrows(UnsupportedOperationException.class, () -> {
            topMatches.add(null);
        });
        
        // Test getSuggestedRefinements immutability
        List<String> refinements = result.getSuggestedRefinements();
        assertThrows(UnsupportedOperationException.class, () -> {
            refinements.add("new refinement");
        });
        
        // Test getCategoryDistribution immutability
        Map<String, Integer> distribution = result.getCategoryDistribution();
        assertThrows(UnsupportedOperationException.class, () -> {
            distribution.put("NEW_CATEGORY", 1);
        });
    }

    @Test
    @DisplayName("Edge case: All null constructor parameters")
    void testAllNullConstructorParameters() {
        AdvancedSearchResult result = new AdvancedSearchResult(
            null, null, null
        );
        
        assertEquals("", result.getSearchQuery());
        assertEquals(AdvancedSearchResult.SearchType.KEYWORD_SEARCH, result.getSearchType());
        assertEquals(Duration.ZERO, result.getSearchDuration());
        assertTrue(result.getTopMatches(5).isEmpty());
        assertTrue(result.groupByCategory().isEmpty());
        assertEquals(0.0, result.getAverageRelevanceScore());
        assertTrue(result.getSuggestedRefinements().isEmpty());
        assertTrue(result.getCategoryDistribution().isEmpty());
    }

    @Test
    @DisplayName("getTopMatches should work with valid matches and positive limit")
    void testGetTopMatchesWithValidData() {
        AdvancedSearchResult result = createResultWithMatches();
        
        List<AdvancedSearchResult.SearchMatch> topMatches = result.getTopMatches(1);
        assertEquals(1, topMatches.size());
        assertNotNull(topMatches.get(0));
        assertEquals(0.8, topMatches.get(0).getRelevanceScore());
    }

    private AdvancedSearchResult createResultWithMatches() {
        AdvancedSearchResult result = new AdvancedSearchResult(
            "test query",
            AdvancedSearchResult.SearchType.KEYWORD_SEARCH,
            Duration.ofMillis(100)
        );
        
        // Add test match
        Map<String, String> snippets = new HashMap<>();
        snippets.put("content", "test snippet");
        
        AdvancedSearchResult.SearchMatch match = new AdvancedSearchResult.SearchMatch(
            testBlock, 0.8, Arrays.asList("test", "match"), 
            snippets, AdvancedSearchResult.SearchMatch.MatchLocation.BLOCK_DATA
        );
        
        result.addMatch(match);
        result.addSuggestedRefinement("refine by date");
        return result;
    }
}