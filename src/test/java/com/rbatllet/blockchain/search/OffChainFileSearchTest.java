package com.rbatllet.blockchain.search;

import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.util.CryptoUtil;
import org.junit.jupiter.api.*;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for OffChainFileSearch functionality
 * 
 * Tests the new EXHAUSTIVE_OFFCHAIN search capability that searches
 * within encrypted off-chain files stored by OffChainStorageService.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OffChainFileSearchTest {
    
    private OffChainFileSearch offChainFileSearch;
    private PrivateKey testPrivateKey;
    private String testPassword;
    
    @BeforeEach
    void setUp() throws Exception {
        offChainFileSearch = new OffChainFileSearch();
        
        // Generate test key pair
        KeyPair keyPair = CryptoUtil.generateKeyPair();
        testPrivateKey = keyPair.getPrivate();
        testPassword = "testPassword123";
    }
    
    @Test
    @Order(1)
    @DisplayName("Basic Off-Chain File Search Initialization")
    void testInitialization() {
        assertNotNull(offChainFileSearch);
        assertNotNull(offChainFileSearch.getCacheStats());
        
        // Verify cache stats
        var stats = offChainFileSearch.getCacheStats();
        assertEquals(0, stats.get("cacheSize"));
        assertTrue(stats.containsKey("cacheExpiryMs"));
        assertTrue(stats.containsKey("supportedContentTypes"));
    }
    
    @Test
    @Order(2)
    @DisplayName("Search With Empty Block List")
    void testSearchWithEmptyBlockList() {
        List<Block> emptyBlocks = new ArrayList<>();
        
        OffChainSearchResult result = offChainFileSearch.searchOffChainContent(
            emptyBlocks, "test", testPassword, testPrivateKey, 10);
        
        assertNotNull(result);
        assertEquals("test", result.getSearchTerm());
        assertFalse(result.hasMatches());
        assertEquals(0, result.getMatchCount());
        assertEquals(0, result.getTotalFilesSearched());
    }
    
    @Test
    @Order(3)
    @DisplayName("Search With Null or Empty Query")
    void testSearchWithEmptyQuery() {
        List<Block> blocks = new ArrayList<>();
        
        // Test with null query
        OffChainSearchResult result1 = offChainFileSearch.searchOffChainContent(
            blocks, null, testPassword, testPrivateKey, 10);
        
        assertNotNull(result1);
        assertFalse(result1.hasMatches());
        
        // Test with empty query
        OffChainSearchResult result2 = offChainFileSearch.searchOffChainContent(
            blocks, "", testPassword, testPrivateKey, 10);
        
        assertNotNull(result2);
        assertFalse(result2.hasMatches());
        
        // Test with whitespace query
        OffChainSearchResult result3 = offChainFileSearch.searchOffChainContent(
            blocks, "   ", testPassword, testPrivateKey, 10);
        
        assertNotNull(result3);
        assertFalse(result3.hasMatches());
    }
    
    @Test
    @Order(4)
    @DisplayName("Search With Block Without Off-Chain Data")
    void testSearchWithBlockWithoutOffChainData() {
        List<Block> blocks = new ArrayList<>();
        
        // Create a block without off-chain data
        Block block = new Block();
        block.setBlockNumber(1L);
        block.setHash("testhash123");
        block.setData("test data");
        block.setOffChainData(null); // No off-chain data
        
        blocks.add(block);
        
        OffChainSearchResult result = offChainFileSearch.searchOffChainContent(
            blocks, "test", testPassword, testPrivateKey, 10);
        
        assertNotNull(result);
        assertEquals("test", result.getSearchTerm());
        assertFalse(result.hasMatches());
        assertEquals(0, result.getMatchCount());
        assertEquals(0, result.getTotalFilesSearched());
    }
    
    @Test
    @Order(5)
    @DisplayName("Cache Operations")
    void testCacheOperations() {
        // Initially empty cache
        var stats = offChainFileSearch.getCacheStats();
        assertEquals(0, stats.get("cacheSize"));
        
        // Clear cache (should not throw error even when empty)
        assertDoesNotThrow(() -> offChainFileSearch.clearCache());
        
        // Cache should still be empty after clear
        stats = offChainFileSearch.getCacheStats();
        assertEquals(0, stats.get("cacheSize"));
    }
    
    @Test
    @Order(6)
    @DisplayName("Search Result Summary")
    void testSearchResultSummary() {
        List<Block> blocks = new ArrayList<>();
        
        OffChainSearchResult result = offChainFileSearch.searchOffChainContent(
            blocks, "test query", testPassword, testPrivateKey, 5);
        
        assertNotNull(result);
        assertNotNull(result.getSearchSummary());
        assertTrue(result.getSearchSummary().contains("test query"));
        assertTrue(result.getSearchSummary().contains("0 matches"));
        assertTrue(result.getSearchSummary().contains("0 files"));
    }
    
    @Test
    @Order(7)
    @DisplayName("Search With Various Max Results")
    void testSearchWithVariousMaxResults() {
        List<Block> blocks = new ArrayList<>();
        
        // Test with different max results values
        for (int maxResults : new int[]{1, 5, 10, 100}) {
            OffChainSearchResult result = offChainFileSearch.searchOffChainContent(
                blocks, "test", testPassword, testPrivateKey, maxResults);
            
            assertNotNull(result);
            assertTrue(result.getMatchCount() <= maxResults);
        }
    }
    
    @Test
    @Order(8)
    @DisplayName("Off-Chain Match Properties")
    void testOffChainMatchProperties() {
        // Test OffChainMatch creation and properties
        List<String> snippets = List.of("test snippet 1", "test snippet 2");
        
        OffChainMatch match = new OffChainMatch(
            1L, "testhash", "/path/to/file.txt", "text/plain", 
            2, snippets, 1024L);
        
        assertEquals(1L, match.getBlockNumber());
        assertEquals("testhash", match.getBlockHash());
        assertEquals("/path/to/file.txt", match.getFilePath());
        assertEquals("text/plain", match.getContentType());
        assertEquals(2, match.getMatchCount());
        assertEquals(snippets, match.getMatchingSnippets());
        assertEquals(1024L, match.getFileSize());
        
        // Test derived properties
        assertEquals("file.txt", match.getFileName());
        assertTrue(match.isTextContent());
        assertNotNull(match.getPreviewSnippet());
        assertTrue(match.getRelevanceScore() > 0);
        
        // Test formatting
        assertNotNull(match.toString());
        assertNotNull(match.getDetailedDescription());
    }
    
    @AfterEach
    void tearDown() {
        if (offChainFileSearch != null) {
            offChainFileSearch.clearCache();
        }
    }
}