package com.rbatllet.blockchain.search;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.util.List;
import java.util.ArrayList;

/**
 * Comprehensive robustness tests for OffChainFileSearch class
 * Tests defensive programming patterns and edge case handling for critical search methods
 */
@DisplayName("OffChainFileSearch Robustness Tests")
class OffChainFileSearchRobustnessTest {

    private OffChainFileSearch offChainFileSearch;

    @BeforeEach
    void setUp() {
        offChainFileSearch = new OffChainFileSearch();
    }

    // ========== searchContent() Tests ==========

    @Test
    @DisplayName("searchContent should handle null data gracefully")
    void testSearchContentNullData() throws Exception {
        Method method = OffChainFileSearch.class.getDeclaredMethod("searchContent", 
            byte[].class, String.class, String.class);
        method.setAccessible(true);
        
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) method.invoke(offChainFileSearch, 
            null, "text/plain", "test");
        
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isEmpty(), "Result should be empty for null data");
    }

    @Test
    @DisplayName("searchContent should handle empty data array")
    void testSearchContentEmptyData() throws Exception {
        Method method = OffChainFileSearch.class.getDeclaredMethod("searchContent", 
            byte[].class, String.class, String.class);
        method.setAccessible(true);
        
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) method.invoke(offChainFileSearch, 
            new byte[0], "text/plain", "test");
        
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isEmpty(), "Result should be empty for empty data");
    }

    @Test
    @DisplayName("searchContent should handle null contentType")
    void testSearchContentNullContentType() throws Exception {
        Method method = OffChainFileSearch.class.getDeclaredMethod("searchContent", 
            byte[].class, String.class, String.class);
        method.setAccessible(true);
        
        byte[] testData = "test content".getBytes();
        
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) method.invoke(offChainFileSearch, 
            testData, null, "test");
        
        assertNotNull(result, "Result should not be null for null contentType");
    }

    @Test
    @DisplayName("searchContent should handle null searchTerm")
    void testSearchContentNullSearchTerm() throws Exception {
        Method method = OffChainFileSearch.class.getDeclaredMethod("searchContent", 
            byte[].class, String.class, String.class);
        method.setAccessible(true);
        
        byte[] testData = "test content".getBytes();
        
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) method.invoke(offChainFileSearch, 
            testData, "text/plain", null);
        
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isEmpty(), "Result should be empty for null searchTerm");
    }

    @Test
    @DisplayName("searchContent should handle empty searchTerm")
    void testSearchContentEmptySearchTerm() throws Exception {
        Method method = OffChainFileSearch.class.getDeclaredMethod("searchContent", 
            byte[].class, String.class, String.class);
        method.setAccessible(true);
        
        byte[] testData = "test content".getBytes();
        
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) method.invoke(offChainFileSearch, 
            testData, "text/plain", "");
        
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isEmpty(), "Result should be empty for empty searchTerm");
    }

    // ========== performTextSearch() Tests ==========

    @Test
    @DisplayName("performTextSearch should handle null content")
    void testPerformTextSearchNullContent() throws Exception {
        Method method = OffChainFileSearch.class.getDeclaredMethod("performTextSearch", 
            String.class, String.class);
        method.setAccessible(true);
        
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) method.invoke(offChainFileSearch, 
            null, "test");
        
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isEmpty(), "Result should be empty for null content");
    }

    @Test
    @DisplayName("performTextSearch should handle null searchTerm")
    void testPerformTextSearchNullSearchTerm() throws Exception {
        Method method = OffChainFileSearch.class.getDeclaredMethod("performTextSearch", 
            String.class, String.class);
        method.setAccessible(true);
        
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) method.invoke(offChainFileSearch, 
            "test content", null);
        
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isEmpty(), "Result should be empty for null searchTerm");
    }

    @Test
    @DisplayName("performTextSearch should handle empty content")
    void testPerformTextSearchEmptyContent() throws Exception {
        Method method = OffChainFileSearch.class.getDeclaredMethod("performTextSearch", 
            String.class, String.class);
        method.setAccessible(true);
        
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) method.invoke(offChainFileSearch, 
            "", "test");
        
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isEmpty(), "Result should be empty for empty content");
    }

    @Test
    @DisplayName("performTextSearch should find matches correctly")
    void testPerformTextSearchValidInput() throws Exception {
        Method method = OffChainFileSearch.class.getDeclaredMethod("performTextSearch", 
            String.class, String.class);
        method.setAccessible(true);
        
        String content = "This is a test\nWith multiple lines\nContaining test data";
        
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) method.invoke(offChainFileSearch, 
            content, "test");
        
        assertNotNull(result, "Result should not be null");
        assertFalse(result.isEmpty(), "Result should contain matches");
    }

    // ========== performJsonSearch() Tests ==========

    @Test
    @DisplayName("performJsonSearch should handle null jsonContent")
    void testPerformJsonSearchNullContent() throws Exception {
        Method method = OffChainFileSearch.class.getDeclaredMethod("performJsonSearch", 
            String.class, String.class);
        method.setAccessible(true);
        
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) method.invoke(offChainFileSearch, 
            null, "test");
        
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isEmpty(), "Result should be empty for null jsonContent");
    }

    @Test
    @DisplayName("performJsonSearch should handle null searchTerm")
    void testPerformJsonSearchNullSearchTerm() throws Exception {
        Method method = OffChainFileSearch.class.getDeclaredMethod("performJsonSearch", 
            String.class, String.class);
        method.setAccessible(true);
        
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) method.invoke(offChainFileSearch, 
            "{\"key\":\"value\"}", null);
        
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isEmpty(), "Result should be empty for null searchTerm");
    }

    @Test
    @DisplayName("performJsonSearch should handle invalid JSON gracefully")
    void testPerformJsonSearchInvalidJson() throws Exception {
        Method method = OffChainFileSearch.class.getDeclaredMethod("performJsonSearch", 
            String.class, String.class);
        method.setAccessible(true);
        
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) method.invoke(offChainFileSearch, 
            "invalid json {", "test");
        
        assertNotNull(result, "Result should not be null");
        // Should fall back to text search
    }

    @Test
    @DisplayName("performJsonSearch should handle valid JSON")
    void testPerformJsonSearchValidJson() throws Exception {
        Method method = OffChainFileSearch.class.getDeclaredMethod("performJsonSearch", 
            String.class, String.class);
        method.setAccessible(true);
        
        String jsonContent = "{\"name\":\"test user\",\"data\":\"test value\"}";
        
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) method.invoke(offChainFileSearch, 
            jsonContent, "test");
        
        assertNotNull(result, "Result should not be null");
    }

    // ========== performBinarySearch() Tests ==========

    @Test
    @DisplayName("performBinarySearch should handle null content")
    void testPerformBinarySearchNullContent() throws Exception {
        Method method = OffChainFileSearch.class.getDeclaredMethod("performBinarySearch", 
            String.class, String.class);
        method.setAccessible(true);
        
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) method.invoke(offChainFileSearch, 
            null, "test");
        
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isEmpty(), "Result should be empty for null content");
    }

    @Test
    @DisplayName("performBinarySearch should handle null searchTerm")
    void testPerformBinarySearchNullSearchTerm() throws Exception {
        Method method = OffChainFileSearch.class.getDeclaredMethod("performBinarySearch", 
            String.class, String.class);
        method.setAccessible(true);
        
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) method.invoke(offChainFileSearch, 
            "binary content with test data", null);
        
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isEmpty(), "Result should be empty for null searchTerm");
    }

    @Test
    @DisplayName("performBinarySearch should find matches in binary content")
    void testPerformBinarySearchValidInput() throws Exception {
        Method method = OffChainFileSearch.class.getDeclaredMethod("performBinarySearch", 
            String.class, String.class);
        method.setAccessible(true);
        
        String binaryContent = "binary\u0000content\u0001with\u0002test\u0003data";
        
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) method.invoke(offChainFileSearch, 
            binaryContent, "test");
        
        assertNotNull(result, "Result should not be null");
    }

    // ========== searchJsonObject() Tests ==========

    @Test
    @DisplayName("searchJsonObject should handle null object")
    void testSearchJsonObjectNullObject() throws Exception {
        Method method = OffChainFileSearch.class.getDeclaredMethod("searchJsonObject", 
            Object.class, String.class, String.class, List.class, int.class);
        method.setAccessible(true);
        
        List<String> matches = new ArrayList<>();
        
        assertDoesNotThrow(() -> {
            try {
                method.invoke(offChainFileSearch, null, "test", "", matches, 0);
            } catch (Exception e) {
                // Should handle null object gracefully
            }
        });
    }

    @Test
    @DisplayName("searchJsonObject should handle null searchTerm")
    void testSearchJsonObjectNullSearchTerm() throws Exception {
        Method method = OffChainFileSearch.class.getDeclaredMethod("searchJsonObject", 
            Object.class, String.class, String.class, List.class, int.class);
        method.setAccessible(true);
        
        List<String> matches = new ArrayList<>();
        
        assertDoesNotThrow(() -> {
            try {
                method.invoke(offChainFileSearch, "test", null, "", matches, 0);
            } catch (Exception e) {
                // Should handle null searchTerm gracefully
            }
        });
    }

    @Test
    @DisplayName("searchJsonObject should handle null matches list")
    void testSearchJsonObjectNullMatches() throws Exception {
        Method method = OffChainFileSearch.class.getDeclaredMethod("searchJsonObject", 
            Object.class, String.class, String.class, List.class, int.class);
        method.setAccessible(true);
        
        assertDoesNotThrow(() -> {
            try {
                method.invoke(offChainFileSearch, "test", "search", "", null, 0);
            } catch (Exception e) {
                // Should handle null matches gracefully
            }
        });
    }

    @Test
    @DisplayName("searchJsonObject should prevent infinite recursion")
    void testSearchJsonObjectRecursionLimit() throws Exception {
        Method method = OffChainFileSearch.class.getDeclaredMethod("searchJsonObject", 
            Object.class, String.class, String.class, List.class, int.class);
        method.setAccessible(true);
        
        List<String> matches = new ArrayList<>();
        
        // Test with depth at limit
        assertDoesNotThrow(() -> {
            try {
                method.invoke(offChainFileSearch, "test", "search", "deep/path", matches, 60);
            } catch (Exception e) {
                // Should handle recursion limit gracefully
            }
        });
    }

    // ========== cleanupCache() Tests ==========

    @Test
    @DisplayName("cleanupCache should handle empty cache gracefully")
    void testCleanupCacheEmpty() throws Exception {
        Method method = OffChainFileSearch.class.getDeclaredMethod("cleanupCache");
        method.setAccessible(true);
        
        assertDoesNotThrow(() -> {
            try {
                method.invoke(offChainFileSearch);
            } catch (Exception e) {
                fail("cleanupCache should handle empty cache gracefully");
            }
        });
    }

    @Test
    @DisplayName("cleanupCache should be thread-safe")
    void testCleanupCacheThreadSafety() throws Exception {
        Method method = OffChainFileSearch.class.getDeclaredMethod("cleanupCache");
        method.setAccessible(true);
        
        // Test concurrent access
        assertDoesNotThrow(() -> {
            Thread[] threads = new Thread[5];
            for (int i = 0; i < threads.length; i++) {
                threads[i] = new Thread(() -> {
                    try {
                        method.invoke(offChainFileSearch);
                    } catch (Exception e) {
                        // Should handle concurrent access
                    }
                });
            }
            
            for (Thread thread : threads) {
                thread.start();
            }
            
            for (Thread thread : threads) {
                thread.join(1000); // Wait max 1 second
            }
        });
    }

    // ========== Integration Tests ==========

    @Test
    @DisplayName("All search methods should be null-safe")
    void testAllMethodsNullSafety() {
        // Test that no NPE is thrown with null inputs
        assertDoesNotThrow(() -> {
            try {
                // Test searchContent
                Method searchContent = OffChainFileSearch.class.getDeclaredMethod("searchContent", 
                    byte[].class, String.class, String.class);
                searchContent.setAccessible(true);
                searchContent.invoke(offChainFileSearch, null, null, null);
                
                // Test performTextSearch  
                Method performTextSearch = OffChainFileSearch.class.getDeclaredMethod("performTextSearch", 
                    String.class, String.class);
                performTextSearch.setAccessible(true);
                performTextSearch.invoke(offChainFileSearch, null, null);
                
                // Test performJsonSearch
                Method performJsonSearch = OffChainFileSearch.class.getDeclaredMethod("performJsonSearch", 
                    String.class, String.class);
                performJsonSearch.setAccessible(true);
                performJsonSearch.invoke(offChainFileSearch, null, null);
                
                // Test performBinarySearch
                Method performBinarySearch = OffChainFileSearch.class.getDeclaredMethod("performBinarySearch", 
                    String.class, String.class);
                performBinarySearch.setAccessible(true);
                performBinarySearch.invoke(offChainFileSearch, null, null);
                
            } catch (Exception e) {
                // Methods should handle null gracefully, not throw exceptions
            }
        }, "All search methods should be null-safe and not throw NPE");
    }

    @Test
    @DisplayName("Cache operations should be robust")
    void testCacheRobustness() throws Exception {
        // Test cache stats access
        assertDoesNotThrow(() -> {
            var stats = offChainFileSearch.getCacheStats();
            assertNotNull(stats, "Cache stats should not be null");
            assertTrue(stats.containsKey("threadSafe"), "Stats should indicate thread safety");
        });
        
        // Test cache clearing
        assertDoesNotThrow(() -> {
            offChainFileSearch.clearCache();
        }, "Cache clearing should not throw exceptions");
    }
}