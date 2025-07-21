package com.rbatllet.blockchain.search;

import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.entity.OffChainData;
import com.rbatllet.blockchain.service.OffChainStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.security.PrivateKey;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Advanced Off-Chain File Search Implementation
 * 
 * Provides exhaustive search capabilities within encrypted off-chain files.
 * This completes the EXHAUSTIVE_OFFCHAIN search level by actually searching
 * inside encrypted files stored via OffChainStorageService.
 * 
 * Key Features:
 * - Content-based search within decrypted off-chain files
 * - Support for various content types (text, JSON, binary)
 * - Thread-safe search operations with caching
 * - Graceful handling of decryption failures
 * - Performance optimization with search result caching
 * 
 * Thread Safety (v2.0):
 * - All cache operations are synchronized to prevent race conditions
 * - Cache cleanup uses snapshot iteration to avoid ConcurrentModificationException
 * - Atomic cache get/put operations ensure consistent state
 * - No external dependencies - uses synchronized methods for simplicity
 */
public class OffChainFileSearch {
    
    private static final Logger logger = LoggerFactory.getLogger(OffChainFileSearch.class);
    
    private final OffChainStorageService offChainService;
    private final ObjectMapper objectMapper;
    
    // Cache for search results to improve performance
    private final Map<String, OffChainSearchResult> searchCache = new ConcurrentHashMap<>();
    private final Map<String, Long> cacheTimestamps = new ConcurrentHashMap<>();
    private static final long CACHE_EXPIRY_MS = 300_000; // 5 minutes
    
    // Supported content types for text search
    private static final Set<String> TEXT_CONTENT_TYPES = Set.of(
        "text/plain", "text/html", "text/xml", "text/csv",
        "application/json", "application/xml", "application/yaml"
    );
    
    public OffChainFileSearch() {
        this.offChainService = new OffChainStorageService();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Search for terms within off-chain files associated with blocks
     * 
     * @param blocks List of blocks to search within
     * @param searchTerm The term to search for
     * @param password Password for decrypting off-chain files
     * @param privateKey Private key for verification
     * @param maxResults Maximum number of results to return
     * @return Search results with matching off-chain content
     */
    public OffChainSearchResult searchOffChainContent(List<Block> blocks, String searchTerm, 
                                                     String password, PrivateKey privateKey, 
                                                     int maxResults) {
        
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return new OffChainSearchResult(searchTerm, new ArrayList<>(), 0);
        }
        
        // Check cache first
        String cacheKey = generateCacheKey(blocks, searchTerm, password);
        OffChainSearchResult cachedResult = getCachedResult(cacheKey);
        if (cachedResult != null) {
            return cachedResult;
        }
        
        List<OffChainMatch> matches = new ArrayList<>();
        int totalFilesSearched = 0;
        
        logger.info("🔍 Starting exhaustive off-chain search for: \"{}\"", searchTerm);
        
        for (Block block : blocks) {
            if (matches.size() >= maxResults) {
                break;
            }
            
            List<OffChainMatch> blockMatches = searchBlockOffChainData(
                block, searchTerm, password, privateKey);
            
            matches.addAll(blockMatches);
            totalFilesSearched += getOffChainFileCount(block);
            
            // Limit results
            if (matches.size() > maxResults) {
                matches = matches.subList(0, maxResults);
            }
        }
        
        // Sort by relevance (number of matches, then block number)
        matches.sort((a, b) -> {
            int relevanceCompare = Integer.compare(b.getMatchCount(), a.getMatchCount());
            if (relevanceCompare != 0) return relevanceCompare;
            return Long.compare(a.getBlockNumber(), b.getBlockNumber());
        });
        
        OffChainSearchResult result = new OffChainSearchResult(searchTerm, matches, totalFilesSearched);
        
        // Cache the result
        cacheResult(cacheKey, result);
        
        logger.info("✅ Off-chain search completed: {} matches found in {} files", matches.size(), totalFilesSearched);
        
        return result;
    }
    
    /**
     * Search within off-chain data of a specific block
     */
    private List<OffChainMatch> searchBlockOffChainData(Block block, String searchTerm, 
                                                       String password, PrivateKey privateKey) {
        List<OffChainMatch> matches = new ArrayList<>();
        
        try {
            // Extract off-chain data references from block
            List<OffChainData> offChainDataList = extractOffChainReferences(block);
            
            for (OffChainData offChainData : offChainDataList) {
                OffChainMatch match = searchSingleOffChainFile(
                    offChainData, block, searchTerm, password);
                
                if (match != null && match.getMatchCount() > 0) {
                    matches.add(match);
                }
            }
            
        } catch (Exception e) {
            logger.warn("⚠️ Error searching off-chain data for block {}: {}", 
                              block.getBlockNumber(), e.getMessage());
        }
        
        return matches;
    }
    
    /**
     * Search within a single off-chain file
     */
    private OffChainMatch searchSingleOffChainFile(OffChainData offChainData, Block block, 
                                                  String searchTerm, String password) {
        try {
            // Verify file exists
            if (!offChainService.fileExists(offChainData)) {
                logger.warn("⚠️ Off-chain file not found: {}", offChainData.getFilePath());
                return null;
            }
            
            // Decrypt and retrieve file content
            byte[] decryptedData = offChainService.retrieveData(offChainData, password);
            
            if (decryptedData == null || decryptedData.length == 0) {
                return null;
            }
            
            // Search within the decrypted content
            List<String> matchingSnippets = searchContent(decryptedData, offChainData.getContentType(), searchTerm);
            
            if (!matchingSnippets.isEmpty()) {
                return new OffChainMatch(
                    block.getBlockNumber(),
                    block.getHash(),
                    offChainData.getFilePath(),
                    offChainData.getContentType(),
                    matchingSnippets.size(),
                    matchingSnippets,
                    offChainData.getFileSize()
                );
            }
            
        } catch (SecurityException e) {
            logger.error("🔐 Security error accessing off-chain file: {}", e.getMessage());
        } catch (Exception e) {
            logger.warn("⚠️ Error searching off-chain file {}: {}", offChainData.getFilePath(), 
                              e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Search for terms within decrypted content based on content type
     */
    private List<String> searchContent(byte[] data, String contentType, String searchTerm) {
        List<String> matches = new ArrayList<>();
        
        try {
            if (TEXT_CONTENT_TYPES.contains(contentType) || contentType.startsWith("text/")) {
                // Text-based search
                String content = new String(data, "UTF-8");
                matches.addAll(performTextSearch(content, searchTerm));
                
            } else if ("application/json".equals(contentType)) {
                // JSON-specific search
                String content = new String(data, "UTF-8");
                matches.addAll(performJsonSearch(content, searchTerm));
                
            } else {
                // Binary search (search for UTF-8 encoded strings)
                String content = new String(data, "UTF-8");
                matches.addAll(performBinarySearch(content, searchTerm));
            }
            
        } catch (Exception e) {
            logger.error("❌ Error during content search: {}", e.getMessage());
        }
        
        return matches;
    }
    
    /**
     * Perform text-based search with context snippets
     */
    private List<String> performTextSearch(String content, String searchTerm) {
        List<String> matches = new ArrayList<>();
        Pattern pattern = Pattern.compile(Pattern.quote(searchTerm), Pattern.CASE_INSENSITIVE);
        
        String[] lines = content.split("\\r?\\n");
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (pattern.matcher(line).find()) {
                // Create context snippet (current line + context)
                StringBuilder snippet = new StringBuilder();
                
                // Add preceding context
                if (i > 0) {
                    snippet.append("...").append(lines[i-1].trim()).append("\\n");
                }
                
                // Add matching line (highlight the match)
                String highlightedLine = pattern.matcher(line).replaceAll("**$0**");
                snippet.append(highlightedLine.trim()).append("\\n");
                
                // Add following context
                if (i < lines.length - 1) {
                    snippet.append(lines[i+1].trim()).append("...");
                }
                
                matches.add(snippet.toString());
            }
        }
        
        return matches;
    }
    
    /**
     * Perform JSON-specific search (search in values and keys)
     */
    private List<String> performJsonSearch(String jsonContent, String searchTerm) {
        List<String> matches = new ArrayList<>();
        
        try {
            // First, try regular text search
            matches.addAll(performTextSearch(jsonContent, searchTerm));
            
            // Then, try structured JSON search
            @SuppressWarnings("unchecked")
            Map<String, Object> jsonMap = objectMapper.readValue(jsonContent, Map.class);
            searchJsonObject(jsonMap, searchTerm, "", matches);
            
        } catch (Exception e) {
            // If JSON parsing fails, fall back to text search
            return performTextSearch(jsonContent, searchTerm);
        }
        
        return matches;
    }
    
    /**
     * Recursively search within JSON objects
     */
    private void searchJsonObject(Object obj, String searchTerm, String path, List<String> matches) {
        if (obj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) obj;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                String newPath = path.isEmpty() ? key : path + "." + key;
                
                // Search in key
                if (key.toLowerCase().contains(searchTerm.toLowerCase())) {
                    matches.add("JSON key match: " + newPath + " = " + value);
                }
                
                // Search in value
                if (value != null && value.toString().toLowerCase().contains(searchTerm.toLowerCase())) {
                    matches.add("JSON value match: " + newPath + " = **" + value + "**");
                }
                
                // Recurse into nested objects
                searchJsonObject(value, searchTerm, newPath, matches);
            }
        } else if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            for (int i = 0; i < list.size(); i++) {
                searchJsonObject(list.get(i), searchTerm, path + "[" + i + "]", matches);
            }
        }
    }
    
    /**
     * Perform binary search (look for text patterns in binary data)
     */
    private List<String> performBinarySearch(String content, String searchTerm) {
        List<String> matches = new ArrayList<>();
        
        // Look for readable text patterns
        if (content.toLowerCase().contains(searchTerm.toLowerCase())) {
            // Extract context around the match
            int index = content.toLowerCase().indexOf(searchTerm.toLowerCase());
            int start = Math.max(0, index - 50);
            int end = Math.min(content.length(), index + searchTerm.length() + 50);
            
            String snippet = content.substring(start, end);
            snippet = snippet.replaceAll("[\\p{Cntrl}]", ""); // Remove control characters
            matches.add("Binary content match: ..." + snippet + "...");
        }
        
        return matches;
    }
    
    /**
     * Extract off-chain data references from a block
     */
    private List<OffChainData> extractOffChainReferences(Block block) {
        List<OffChainData> offChainDataList = new ArrayList<>();
        
        try {
            // Check if block has off-chain data references
            if (block.getOffChainData() != null) {
                // Use the existing OffChainData object
                offChainDataList.add(block.getOffChainData());
            }
            
        } catch (Exception e) {
            logger.error("❌ Error extracting off-chain references: {}", e.getMessage());
        }
        
        return offChainDataList;
    }
    
    /**
     * Get the number of off-chain files associated with a block
     */
    private int getOffChainFileCount(Block block) {
        return extractOffChainReferences(block).size();
    }
    
    /**
     * Generate cache key for search results
     */
    private String generateCacheKey(List<Block> blocks, String searchTerm, String password) {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(searchTerm).append("|");
        keyBuilder.append(password.hashCode()).append("|");
        
        // Add block hashes to cache key
        for (Block block : blocks) {
            if (block.getHash() != null) {
                keyBuilder.append(block.getHash()).append(",");
            }
        }
        
        return Integer.toString(keyBuilder.toString().hashCode());
    }
    
    /**
     * Get cached search result if still valid
     */
    private synchronized OffChainSearchResult getCachedResult(String cacheKey) {
        Long timestamp = cacheTimestamps.get(cacheKey);
        if (timestamp != null && (System.currentTimeMillis() - timestamp) < CACHE_EXPIRY_MS) {
            OffChainSearchResult result = searchCache.get(cacheKey);
            if (result != null) {
                return result;
            }
        }
        
        // Remove expired or missing cache entries atomically
        searchCache.remove(cacheKey);
        cacheTimestamps.remove(cacheKey);
        return null;
    }
    
    /**
     * Cache search result
     */
    private synchronized void cacheResult(String cacheKey, OffChainSearchResult result) {
        // Atomic cache operations
        searchCache.put(cacheKey, result);
        cacheTimestamps.put(cacheKey, System.currentTimeMillis());
        
        // Clean up old cache entries periodically
        if (searchCache.size() > 100) {
            cleanupCache();
        }
    }
    
    /**
     * Clean up expired cache entries
     */
    private synchronized void cleanupCache() {
        long currentTime = System.currentTimeMillis();
        List<String> expiredKeys = new ArrayList<>();
        
        // Create snapshot for safe iteration
        Map<String, Long> timestampSnapshot = new HashMap<>(cacheTimestamps);
        
        for (Map.Entry<String, Long> entry : timestampSnapshot.entrySet()) {
            if ((currentTime - entry.getValue()) > CACHE_EXPIRY_MS) {
                expiredKeys.add(entry.getKey());
            }
        }
        
        // Remove expired entries atomically
        for (String key : expiredKeys) {
            searchCache.remove(key);
            cacheTimestamps.remove(key);
        }
    }
    
    /**
     * Clear all cached results
     */
    public synchronized void clearCache() {
        searchCache.clear();
        cacheTimestamps.clear();
    }
    
    /**
     * Get cache statistics for monitoring
     */
    public synchronized Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cacheSize", searchCache.size());
        stats.put("cacheExpiryMs", CACHE_EXPIRY_MS);
        stats.put("supportedContentTypes", TEXT_CONTENT_TYPES);
        // Add thread safety info
        stats.put("threadSafe", true);
        stats.put("synchronizationMethod", "synchronized");
        return Collections.unmodifiableMap(stats);
    }
}