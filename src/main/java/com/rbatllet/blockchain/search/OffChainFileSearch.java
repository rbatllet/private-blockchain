package com.rbatllet.blockchain.search;

import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.entity.OffChainData;
import com.rbatllet.blockchain.service.OffChainStorageService;

import tools.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Advanced Off-Chain File Search Implementation
 * 
 * Provides exhaustive search capabilities within encrypted off-chain files.
 * This completes the INCLUDE_OFFCHAIN search level by actually searching
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
    
    // Recursion control for JSON search
    private static final int MAX_RECURSION_DEPTH = 50;
    
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
     * @param maxResults Maximum number of results to return
     * @return Search results with matching off-chain content
     */
    public OffChainSearchResult searchOffChainContent(List<Block> blocks, String searchTerm,
                                                     String password, int maxResults) {
        
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
                block, searchTerm, password);
            
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
                                                       String password) {
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
        // Defensive parameter validation
        if (offChainData == null) {
            logger.warn("⚠️ OffChainData is null, cannot perform search");
            return null;
        }
        
        if (block == null) {
            logger.warn("⚠️ Block is null, cannot perform search");
            return null;
        }
        
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            logger.warn("⚠️ SearchTerm is null or empty, cannot perform search");
            return null;
        }
        
        try {
            // Verify file exists
            if (!offChainService.fileExists(offChainData)) {
                logger.warn("⚠️ Off-chain file not found: {}", offChainData.getFilePath());
                return null;
            }
            
            // Decrypt and retrieve file content
            byte[] decryptedData = offChainService.retrieveData(offChainData, password);

            logger.info("🔍 Off-chain file decrypted: {} bytes, contentType={}, searchTerm={}, file={}",
                decryptedData != null ? decryptedData.length : 0,
                offChainData.getContentType(),
                searchTerm,
                offChainData.getFilePath());

            if (decryptedData == null || decryptedData.length == 0) {
                logger.warn("⚠️ Decrypted data is null or empty for file: {}", offChainData.getFilePath());
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
            String filePath = (offChainData != null) ? offChainData.getFilePath() : "null";
            logger.warn("⚠️ Error searching off-chain file {}: {}", filePath, e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Search for terms within decrypted content based on content type
     */
    private List<String> searchContent(byte[] data, String contentType, String searchTerm) {
        // Defensive parameter validation
        if (data == null || data.length == 0) {
            logger.warn("⚠️ Search content: data is null or empty");
            return new ArrayList<>();
        }
        if (contentType == null || contentType.trim().isEmpty()) {
            logger.warn("⚠️ Search content: contentType is null or empty, defaulting to binary search");
            contentType = "application/octet-stream";
        }
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            logger.warn("⚠️ Search content: searchTerm is null or empty");
            return new ArrayList<>();
        }
        
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
        // Defensive parameter validation
        if (content == null) {
            logger.warn("⚠️ Text search: content is null");
            return new ArrayList<>();
        }
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            logger.warn("⚠️ Text search: searchTerm is null or empty");
            return new ArrayList<>();
        }
        
        List<String> matches = new ArrayList<>();
        
        try {
            Pattern pattern = Pattern.compile(Pattern.quote(searchTerm.trim()), Pattern.CASE_INSENSITIVE);
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
        
        } catch (Exception e) {
            logger.error("❌ Error during text search: {}", e.getMessage());
        }
        
        return matches;
    }
    
    /**
     * Perform JSON-specific search (search in values and keys)
     */
    private List<String> performJsonSearch(String jsonContent, String searchTerm) {
        // Defensive parameter validation
        if (jsonContent == null) {
            logger.warn("⚠️ JSON search: jsonContent is null");
            return new ArrayList<>();
        }
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            logger.warn("⚠️ JSON search: searchTerm is null or empty");
            return new ArrayList<>();
        }
        
        List<String> matches = new ArrayList<>();
        
        try {
            // First, try regular text search (safe due to our validation above)
            matches.addAll(performTextSearch(jsonContent, searchTerm));
            
            // Then, try structured JSON search if content is not empty
            if (!jsonContent.trim().isEmpty()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> jsonMap = objectMapper.readValue(jsonContent, Map.class);
                if (jsonMap != null) {
                    searchJsonObject(jsonMap, searchTerm, "", matches);
                }
            }
            
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
        searchJsonObject(obj, searchTerm, path, matches, 0);
    }
    
    /**
     * Recursively search within JSON objects with depth control
     */
    private void searchJsonObject(Object obj, String searchTerm, String path, List<String> matches, int depth) {
        // Defensive parameter validation
        if (obj == null || matches == null) {
            return;
        }
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            logger.warn("⚠️ JSON object search: searchTerm is null or empty");
            return;
        }
        if (path == null) {
            path = "";
        }
        
        // Prevent infinite recursion and stack overflow
        if (depth > MAX_RECURSION_DEPTH) {
            logger.warn("⚠️ JSON recursion depth limit reached ({}), stopping search at path: {}", 
                       MAX_RECURSION_DEPTH, path);
            return;
        }
        
        try {
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
                
                // Recurse into nested objects with depth control
                if (key != null) { // Additional safety check
                    searchJsonObject(value, searchTerm, newPath, matches, depth + 1);
                }
            }
        } else if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            for (int i = 0; i < list.size(); i++) {
                searchJsonObject(list.get(i), searchTerm, path + "[" + i + "]", matches, depth + 1);
            }
        }
        
        } catch (Exception e) {
            logger.error("❌ Error during JSON object search at path '{}': {}", path, e.getMessage());
        }
    }
    
    /**
     * Perform binary search (look for text patterns in binary data)
     */
    private List<String> performBinarySearch(String content, String searchTerm) {
        // Defensive parameter validation
        if (content == null) {
            logger.warn("⚠️ Binary search: content is null");
            return new ArrayList<>();
        }
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            logger.warn("⚠️ Binary search: searchTerm is null or empty");
            return new ArrayList<>();
        }
        
        List<String> matches = new ArrayList<>();
        
        try {
            // Look for readable text patterns (safe due to validation above)
            String lowerContent = content.toLowerCase();
            String lowerSearchTerm = searchTerm.toLowerCase();
            
            if (lowerContent.contains(lowerSearchTerm)) {
                // Extract context around the match
                int index = lowerContent.indexOf(lowerSearchTerm);
                int start = Math.max(0, index - 50);
                int end = Math.min(content.length(), index + searchTerm.length() + 50);
            
                String snippet = content.substring(start, end);
                snippet = snippet.replaceAll("[\\p{Cntrl}]", ""); // Remove control characters
                matches.add("Binary content match: ..." + snippet + "...");
            }
        
        } catch (Exception e) {
            logger.error("❌ Error during binary search: {}", e.getMessage());
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
     * Clean up expired cache entries with enhanced safety
     */
    private synchronized void cleanupCache() {
        try {
            long currentTime = System.currentTimeMillis();
            List<String> expiredKeys = new ArrayList<>();
            
            // Defensive check for cache state
            if (cacheTimestamps == null || cacheTimestamps.isEmpty()) {
                return;
            }
            
            // Create snapshot for safe iteration
            Map<String, Long> timestampSnapshot = new HashMap<>(cacheTimestamps);
            
            // Memory protection: if cache is too large, clean more aggressively
            boolean forceCleanup = searchCache.size() > 500;
            
            for (Map.Entry<String, Long> entry : timestampSnapshot.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    // Remove entries with null keys or values
                    expiredKeys.add(entry.getKey());
                } else if (forceCleanup || (currentTime - entry.getValue()) > CACHE_EXPIRY_MS) {
                    expiredKeys.add(entry.getKey());
                }
            }
            
            // Remove expired entries atomically
            int removedCount = 0;
            for (String key : expiredKeys) {
                if (searchCache.remove(key) != null) {
                    removedCount++;
                }
                cacheTimestamps.remove(key);
            }
            
            if (removedCount > 0) {
                logger.debug("🧹 Cache cleanup: removed {} expired entries, cache size: {}", 
                           removedCount, searchCache.size());
            }
            
        } catch (Exception e) {
            logger.error("❌ Error during cache cleanup: {}", e.getMessage());
            // In case of error, clear cache to prevent corruption
            searchCache.clear();
            cacheTimestamps.clear();
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