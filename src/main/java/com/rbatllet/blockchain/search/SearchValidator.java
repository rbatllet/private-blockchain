package com.rbatllet.blockchain.search;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates search terms with minimum length requirement and intelligent exceptions
 */
public class SearchValidator {
    
    private static final int MIN_SEARCH_LENGTH = 4;
    
    public static SearchValidationResult validateSearchTerm(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return new SearchValidationResult(false, "Search term cannot be empty", null);
        }
        
        String cleanTerm = searchTerm.trim();
        
        // Longitud mínima amb excepcions intel·ligents
        if (cleanTerm.length() < MIN_SEARCH_LENGTH && !isValidShortTerm(cleanTerm)) {
            return new SearchValidationResult(
                false, 
                "Search term must be at least " + MIN_SEARCH_LENGTH + " characters", 
                generateSuggestions(cleanTerm)
            );
        }
        
        return new SearchValidationResult(true, null, null);
    }
    
    /**
     * Simple validation method for testing - returns true if search term is valid
     * @param searchTerm the term to validate
     * @return true if the term is valid for searching
     */
    public static boolean isValidSearchTerm(String searchTerm) {
        SearchValidationResult result = validateSearchTerm(searchTerm);
        return result.isValid();
    }
    
    /**
     * Check if a short term is valid for searching
     * @param term the search term to validate
     * @return true if the short term is acceptable for searching
     */
    private static boolean isValidShortTerm(String term) {
        // Excepcions per termes curts útils
        return term.matches("\\d+") ||                    // Números: 123, 2024
               term.matches("[A-Z]{2,}") ||               // Acrònims: API, SQL, XML
               term.matches("ID[\\-_]?\\d*") ||           // IDs: ID, ID-1, ID_001
               term.matches("[A-Z]+\\d+") ||              // Codis: A1, B23, ABC123
               term.matches("\\d{4}") ||                  // Anys: 2024
               term.matches("[A-Z]{3}") ||                // Codis 3 chars: EUR, USD, CAT
               term.toLowerCase().matches("pdf|xml|json|csv|api|sql|url|uri|id"); // Extensions/termes tècnics útils
    }
    
    /**
     * Generate helpful suggestions for invalid search terms
     */
    private static List<String> generateSuggestions(String shortTerm) {
        List<String> suggestions = new ArrayList<>();
        
        if (shortTerm.length() == 3) {
            suggestions.add("Try adding more characters: '" + shortTerm + "*'");
        } else if (shortTerm.length() == 2) {
            suggestions.add("Use at least 4 characters for better results");
        } else {
            suggestions.add("Search terms should be more specific");
        }
        
        return suggestions;
    }
    
    /**
     * Result of search term validation
     */
    public static class SearchValidationResult {
        private final boolean valid;
        private final String message;
        private final List<String> suggestions;
        
        public SearchValidationResult(boolean valid, String message, List<String> suggestions) {
            this.valid = valid;
            this.message = message;
            this.suggestions = suggestions;
        }
        
        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
        public List<String> getSuggestions() { return suggestions; }
    }
}