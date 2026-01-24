package com.rbatllet.blockchain.service;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AdvancedSearchResultManualTest {
    private static final Logger logger = LoggerFactory.getLogger(AdvancedSearchResultManualTest.class);

    
    @Test
    @DisplayName("Manual test for immutability verification")
    public void testImmutabilityManually() {
        // Test getSuggestedRefinements immutability
        AdvancedSearchResult result = new AdvancedSearchResult(
            "test", 
            AdvancedSearchResult.SearchType.KEYWORD_SEARCH, 
            Duration.ofMillis(100)
        );
        
        logger.info("Testing getSuggestedRefinements immutability:");
        
        try {
            result.getSuggestedRefinements().add("test");
            logger.info("ERROR: Could add to empty list - not immutable!");
        } catch (UnsupportedOperationException e) {
            logger.info("SUCCESS: Empty list is immutable");
        }
        
        // Add a refinement and test again
        result.addSuggestedRefinement("existing refinement");
        
        try {
            result.getSuggestedRefinements().add("test");
            logger.info("ERROR: Could add to non-empty list - not immutable!");
        } catch (UnsupportedOperationException e) {
            logger.info("SUCCESS: Non-empty list is immutable");
        }
        
        logger.info("Current refinements: " + result.getSuggestedRefinements());
        
        // Test getCategoryDistribution immutability
        logger.info("\nTesting getCategoryDistribution immutability:");
        
        try {
            result.getCategoryDistribution().put("TEST", 1);
            logger.info("ERROR: Could add to category distribution - not immutable!");
        } catch (UnsupportedOperationException e) {
            logger.info("SUCCESS: Category distribution is immutable");
        }
        
        logger.info("Category distribution: " + result.getCategoryDistribution());
        
        // Test getTopMatches immutability
        logger.info("\nTesting getTopMatches immutability:");
        
        try {
            result.getTopMatches(5).add(null);
            logger.info("ERROR: Could add to top matches - not immutable!");
        } catch (UnsupportedOperationException e) {
            logger.info("SUCCESS: Top matches is immutable");
        }
        
        logger.info("Top matches: " + result.getTopMatches(5).size());
    }
}