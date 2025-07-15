package com.rbatllet.blockchain.search;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.service.UserFriendlyEncryptionAPI;
import com.rbatllet.blockchain.search.SearchFrameworkEngine.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;

import java.security.KeyPair;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Compatibility tests between searchAndDecryptByTerms and SearchSpecialistAPI
 * Validates that both search methods produce consistent results
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SearchCompatibilityTest {
    
    private Blockchain blockchain;
    private UserFriendlyEncryptionAPI api;
    private SearchSpecialistAPI searchAPI;
    private String testPassword;
    private KeyPair userKeys;
    
    @BeforeEach
    void setUp() throws Exception {
        blockchain = new Blockchain();
        api = new UserFriendlyEncryptionAPI(blockchain);
        testPassword = "CompatibilityTest123!";
        
        // Create user and set credentials
        userKeys = api.createUser("compatibility-test-user");
        api.setDefaultCredentials("compatibility-test-user", userKeys);
        
        // Create diverse test data
        createTestData();
        
        // Initialize SearchSpecialistAPI
        blockchain.initializeAdvancedSearch(testPassword);
        searchAPI = blockchain.getSearchSpecialistAPI();
        
        assertTrue(searchAPI.isReady(), "SearchSpecialistAPI should be ready");
    }
    
    private void createTestData() throws Exception {
        // Create blocks with different types of content
        String[] keywords1 = {"blockchain", "technology", "innovation"};
        api.storeSearchableData("Revolutionary blockchain technology for secure transactions", testPassword, keywords1);
        
        String[] keywords2 = {"healthcare", "patient", "medical"};
        api.storeSearchableData("Patient healthcare data management system", testPassword, keywords2);
        
        String[] keywords3 = {"financial", "report", "analysis"};
        api.storeSearchableData("Financial analysis report for quarterly review", testPassword, keywords3);
        
        String[] keywords4 = {"education", "learning", "platform"};
        api.storeSearchableData("Educational learning platform for students", testPassword, keywords4);
        
        // Create public content
        String[] publicKeywords = {"public", "announcement", "news"};
        api.storeSearchableData("Public announcement for community news", testPassword, publicKeywords);
    }
    
    @Test
    @Order(1)
    @DisplayName("Test compatibility for simple search terms")
    void testSimpleSearchTermCompatibility() throws Exception {
        System.out.println("🔍 Testing simple search term compatibility...");
        
        String[] testTerms = {"blockchain", "healthcare", "financial", "education", "public"};
        
        for (String term : testTerms) {
            // Search using searchAndDecryptByTerms
            List<Block> directResults = api.searchAndDecryptByTerms(new String[]{term}, testPassword, 10);
            
            // Search using SearchSpecialistAPI
            List<EnhancedSearchResult> apiResults = searchAPI.searchSecure(term, testPassword);
            
            System.out.println("📊 Term '" + term + "': Direct=" + directResults.size() + ", API=" + apiResults.size());
            
            // Both should return results for valid terms
            assertFalse(directResults.isEmpty(), "Direct search should find results for: " + term);
            assertFalse(apiResults.isEmpty(), "API search should find results for: " + term);
            
            // Results should be consistent (may differ in count due to different algorithms)
            assertTrue(directResults.size() > 0 && apiResults.size() > 0, 
                "Both methods should return results for: " + term);
        }
    }
    
    @Test
    @Order(2)
    @DisplayName("Test compatibility for multi-word queries")
    void testMultiWordQueryCompatibility() throws Exception {
        System.out.println("🔍 Testing multi-word query compatibility...");
        
        String[] multiWordQueries = {
            "blockchain technology",
            "patient healthcare",
            "financial analysis",
            "learning platform",
            "public announcement"
        };
        
        for (String query : multiWordQueries) {
            // Search using searchAndDecryptByTerms
            List<Block> directResults = api.searchAndDecryptByTerms(query.split("\\s+"), testPassword, 10);
            
            // Search using SearchSpecialistAPI
            List<EnhancedSearchResult> apiResults = searchAPI.searchSecure(query, testPassword);
            
            System.out.println("📊 Query '" + query + "': Direct=" + directResults.size() + ", API=" + apiResults.size());
            
            // Both should handle multi-word queries
            assertNotNull(directResults, "Direct search should handle multi-word query: " + query);
            assertNotNull(apiResults, "API search should handle multi-word query: " + query);
        }
    }
    
    @Test
    @Order(3)
    @DisplayName("Test password sensitivity compatibility")
    void testPasswordSensitivityCompatibility() throws Exception {
        System.out.println("🔍 Testing password sensitivity compatibility...");
        
        String correctPassword = testPassword;
        String wrongPassword = "WrongPassword123!";
        String searchTerm = "blockchain";
        
        // Test with correct password
        List<Block> directCorrect = api.searchAndDecryptByTerms(new String[]{searchTerm}, correctPassword, 10);
        List<EnhancedSearchResult> apiCorrect = searchAPI.searchSecure(searchTerm, correctPassword);
        
        System.out.println("📊 Correct password: Direct=" + directCorrect.size() + ", API=" + apiCorrect.size());
        
        // Test with wrong password
        List<Block> directWrong = api.searchAndDecryptByTerms(new String[]{searchTerm}, wrongPassword, 10);
        List<EnhancedSearchResult> apiWrong = searchAPI.searchSecure(searchTerm, wrongPassword);
        
        System.out.println("📊 Wrong password: Direct=" + directWrong.size() + ", API=" + apiWrong.size());
        
        // Both should return more results with correct password
        assertTrue(directCorrect.size() > 0, "Direct search should find results with correct password");
        assertTrue(apiCorrect.size() > 0, "API search should find results with correct password");
        
        // Results with wrong password should be limited
        assertTrue(directWrong.size() <= directCorrect.size(), 
            "Direct search should return fewer/equal results with wrong password");
        assertTrue(apiWrong.size() <= apiCorrect.size(), 
            "API search should return fewer/equal results with wrong password");
    }
    
    @Test
    @Order(4)
    @DisplayName("Test search result content consistency")
    void testSearchResultContentConsistency() throws Exception {
        System.out.println("🔍 Testing search result content consistency...");
        
        String searchTerm = "technology";
        
        // Get results from both methods
        List<Block> directResults = api.searchAndDecryptByTerms(new String[]{searchTerm}, testPassword, 10);
        List<EnhancedSearchResult> apiResults = searchAPI.searchSecure(searchTerm, testPassword);
        
        System.out.println("📊 Found " + directResults.size() + " direct results and " + apiResults.size() + " API results");
        
        // Extract block hashes for comparison
        Set<String> directHashes = new HashSet<>();
        for (Block block : directResults) {
            directHashes.add(block.getHash());
            System.out.println("📋 Direct result: " + block.getHash().substring(0, 8) + "...");
        }
        
        Set<String> apiHashes = new HashSet<>();
        for (EnhancedSearchResult result : apiResults) {
            apiHashes.add(result.getBlockHash());
            System.out.println("📋 API result: " + result.getBlockHash().substring(0, 8) + "...");
        }
        
        // Check for overlap in results
        Set<String> intersection = new HashSet<>(directHashes);
        intersection.retainAll(apiHashes);
        
        System.out.println("📊 Common results: " + intersection.size());
        
        // Both methods should find some common blocks
        assertFalse(intersection.isEmpty(), "Both methods should find some common blocks");
    }
    
    @Test
    @Order(5)
    @DisplayName("Test performance comparison")
    void testPerformanceComparison() throws Exception {
        System.out.println("🔍 Testing performance comparison...");
        
        String searchTerm = "blockchain";
        int iterations = 10;
        
        // Test searchAndDecryptByTerms performance
        long directTotal = 0;
        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            api.searchAndDecryptByTerms(new String[]{searchTerm}, testPassword, 10);
            long end = System.nanoTime();
            directTotal += (end - start);
        }
        double directAvg = (directTotal / iterations) / 1_000_000.0;
        
        // Test SearchSpecialistAPI performance  
        long apiTotal = 0;
        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            searchAPI.searchSecure(searchTerm, testPassword);
            long end = System.nanoTime();
            apiTotal += (end - start);
        }
        double apiAvg = (apiTotal / iterations) / 1_000_000.0;
        
        System.out.println("📊 Performance comparison:");
        System.out.println("  Direct search average: " + String.format("%.2f", directAvg) + "ms");
        System.out.println("  API search average: " + String.format("%.2f", apiAvg) + "ms");
        
        // Both methods should complete within reasonable time
        assertTrue(directAvg < 1000, "Direct search should complete within 1 second");
        assertTrue(apiAvg < 1000, "API search should complete within 1 second");
    }
    
    @Test
    @Order(6)
    @DisplayName("Test edge case compatibility")
    void testEdgeCaseCompatibility() throws Exception {
        System.out.println("🔍 Testing edge case compatibility...");
        
        // Test empty query handling
        try {
            List<Block> directEmpty = api.searchAndDecryptByTerms(new String[]{""}, testPassword, 10);
            System.out.println("📊 Direct empty query: " + directEmpty.size() + " results");
        } catch (Exception e) {
            System.out.println("📊 Direct empty query threw: " + e.getClass().getSimpleName());
        }
        
        try {
            List<EnhancedSearchResult> apiEmpty = searchAPI.searchSecure("", testPassword);
            System.out.println("📊 API empty query: " + apiEmpty.size() + " results");
        } catch (Exception e) {
            System.out.println("📊 API empty query threw: " + e.getClass().getSimpleName());
        }
        
        // Test null password handling
        try {
            List<Block> directNull = api.searchAndDecryptByTerms(new String[]{"blockchain"}, null, 10);
            System.out.println("📊 Direct null password: " + directNull.size() + " results");
        } catch (Exception e) {
            System.out.println("📊 Direct null password threw: " + e.getClass().getSimpleName());
        }
        
        try {
            List<EnhancedSearchResult> apiNull = searchAPI.searchSecure("blockchain", null);
            System.out.println("📊 API null password: " + apiNull.size() + " results");
        } catch (Exception e) {
            System.out.println("📊 API null password threw: " + e.getClass().getSimpleName());
        }
        
        // Test non-existent term
        String nonExistentTerm = "NonExistentTerm12345";
        List<Block> directNonExistent = api.searchAndDecryptByTerms(new String[]{nonExistentTerm}, testPassword, 10);
        List<EnhancedSearchResult> apiNonExistent = searchAPI.searchSecure(nonExistentTerm, testPassword);
        
        System.out.println("📊 Non-existent term: Direct=" + directNonExistent.size() + ", API=" + apiNonExistent.size());
        
        // Both should return empty results for non-existent terms
        assertTrue(directNonExistent.isEmpty(), "Direct search should return empty for non-existent term");
        assertTrue(apiNonExistent.isEmpty(), "API search should return empty for non-existent term");
    }
}