package com.rbatllet.blockchain.service;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

public class AdvancedSearchResultManualTest {
    
    @Test
    @DisplayName("Manual test for immutability verification")
    public void testImmutabilityManually() {
        // Test getSuggestedRefinements immutability
        AdvancedSearchResult result = new AdvancedSearchResult(
            "test", 
            AdvancedSearchResult.SearchType.KEYWORD_SEARCH, 
            Duration.ofMillis(100)
        );
        
        System.out.println("Testing getSuggestedRefinements immutability:");
        
        try {
            result.getSuggestedRefinements().add("test");
            System.out.println("ERROR: Could add to empty list - not immutable!");
        } catch (UnsupportedOperationException e) {
            System.out.println("SUCCESS: Empty list is immutable");
        }
        
        // Add a refinement and test again
        result.addSuggestedRefinement("existing refinement");
        
        try {
            result.getSuggestedRefinements().add("test");
            System.out.println("ERROR: Could add to non-empty list - not immutable!");
        } catch (UnsupportedOperationException e) {
            System.out.println("SUCCESS: Non-empty list is immutable");
        }
        
        System.out.println("Current refinements: " + result.getSuggestedRefinements());
        
        // Test getCategoryDistribution immutability
        System.out.println("\nTesting getCategoryDistribution immutability:");
        
        try {
            result.getCategoryDistribution().put("TEST", 1);
            System.out.println("ERROR: Could add to category distribution - not immutable!");
        } catch (UnsupportedOperationException e) {
            System.out.println("SUCCESS: Category distribution is immutable");
        }
        
        System.out.println("Category distribution: " + result.getCategoryDistribution());
        
        // Test getTopMatches immutability
        System.out.println("\nTesting getTopMatches immutability:");
        
        try {
            result.getTopMatches(5).add(null);
            System.out.println("ERROR: Could add to top matches - not immutable!");
        } catch (UnsupportedOperationException e) {
            System.out.println("SUCCESS: Top matches is immutable");
        }
        
        System.out.println("Top matches: " + result.getTopMatches(5).size());
    }
}