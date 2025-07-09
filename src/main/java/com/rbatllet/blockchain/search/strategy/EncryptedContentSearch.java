package com.rbatllet.blockchain.search.strategy;

import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.util.CompressionUtil;
import com.rbatllet.blockchain.search.metadata.PrivateMetadata;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Encrypted Content Deep Search Engine
 * 
 * Performs comprehensive search on encrypted private metadata layers
 * requiring password authentication. This search strategy provides
 * detailed results by decrypting and analyzing private content.
 * 
 * Features:
 * - Password-protected deep content search
 * - Encrypted metadata decryption and analysis
 * - Advanced pattern matching on private content
 * - Intelligent caching for performance
 * - Detailed result summaries with content snippets
 * 
 * Security: All operations require valid password authentication
 */
public class EncryptedContentSearch {
    
    private final Map<String, String> encryptedMetadataCache;
    private final Map<String, Long> lastAccessTime;
    private final Map<String, PrivateMetadata> decryptedCache;
    private static final long CACHE_EXPIRY_MS = 300000; // 5 minutes
    
    private final ObjectMapper objectMapper;
    
    public EncryptedContentSearch() {
        this.encryptedMetadataCache = new ConcurrentHashMap<>();
        this.lastAccessTime = new ConcurrentHashMap<>();
        this.decryptedCache = new ConcurrentHashMap<>();
        
        // Initialize Jackson ObjectMapper for JSON parsing
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    /**
     * Add encrypted block metadata to search index
     * @param blockHash The block's hash identifier
     * @param encryptedPrivateMetadata Encrypted private metadata JSON
     */
    public void indexEncryptedBlock(String blockHash, String encryptedPrivateMetadata) {
        System.out.println("🔍 Debug: indexEncryptedBlock called with blockHash=" + (blockHash != null ? blockHash.substring(0, 8) + "..." : "null") + 
                          ", encryptedPrivateMetadata=" + (encryptedPrivateMetadata != null ? "present (" + encryptedPrivateMetadata.length() + " chars)" : "null"));
        
        if (blockHash == null || encryptedPrivateMetadata == null) {
            System.out.println("🔍 Debug: ⚠️ Skipping indexEncryptedBlock - null parameter");
            return;
        }
        
        encryptedMetadataCache.put(blockHash, encryptedPrivateMetadata);
        lastAccessTime.put(blockHash, System.currentTimeMillis());
        System.out.println("🔍 Debug: ✅ Indexed encrypted block. Cache size now: " + encryptedMetadataCache.size());
        
        // Clean up expired cache entries periodically
        cleanupExpiredCache();
    }
    
    /**
     * Search encrypted content with password authentication
     * @param query Search query string
     * @param password Password for decrypting private metadata
     * @param maxResults Maximum number of results
     * @return List of encrypted search results
     */
    public List<EncryptedSearchResult> searchEncryptedContent(String query, String password, int maxResults) {
        if (query == null || query.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        long startTime = System.nanoTime();
        System.out.println("🔍 Debug: EncryptedContentSearch.searchEncryptedContent() called with query='" + query + "'");
        System.out.println("🔍 Debug: encryptedMetadataCache size: " + encryptedMetadataCache.size());
        
        // Debug: Show all cached block hashes
        if (encryptedMetadataCache.size() > 0) {
            System.out.println("🔍 Debug: Cached block hashes:");
            for (String blockHash : encryptedMetadataCache.keySet()) {
                System.out.println("🔍 Debug:   - " + blockHash.substring(0, 8) + "...");
            }
        } else {
            System.out.println("🔍 Debug: ⚠️ encryptedMetadataCache is EMPTY! This explains why no results are found.");
        }
        
        // Parse and prepare query
        Set<String> queryKeywords = parseQuery(query);
        System.out.println("🔍 Debug: parsed query keywords: " + queryKeywords);
        List<EncryptedSearchResult> results = new ArrayList<>();
        
        // Search through all encrypted metadata
        for (Map.Entry<String, String> entry : encryptedMetadataCache.entrySet()) {
            String blockHash = entry.getKey();
            String encryptedMetadata = entry.getValue();
            System.out.println("🔍 Debug: processing block " + blockHash.substring(0, 8) + "...");
            
            try {
                // Try to decrypt and search the private metadata
                System.out.println("🔍 Debug: attempting to decrypt private metadata for block " + blockHash.substring(0, 8));
                System.out.println("🔍 Debug: encryptedMetadata preview: " + encryptedMetadata.substring(0, Math.min(50, encryptedMetadata.length())) + "...");
                PrivateMetadata privateMetadata = decryptPrivateMetadata(blockHash, encryptedMetadata, password);
                System.out.println("🔍 Debug: privateMetadata decrypted: " + (privateMetadata != null ? "yes" : "null"));
                
                if (privateMetadata != null) {
                    System.out.println("🔍 Debug: privateMetadata content - detailedKeywords: " + privateMetadata.getDetailedKeywords());
                    System.out.println("🔍 Debug: privateMetadata content - sensitiveTerms: " + privateMetadata.getSensitiveTerms());
                    System.out.println("🔍 Debug: privateMetadata content - identifiers: " + privateMetadata.getIdentifiers());
                    
                    // Perform search on decrypted content
                    EncryptedSearchResult result = searchPrivateMetadata(blockHash, privateMetadata, 
                                                                        queryKeywords, query, startTime);
                    System.out.println("🔍 Debug: search result: " + (result != null ? "score=" + result.getRelevanceScore() : "null"));
                    if (result != null && result.getRelevanceScore() > 0.0) {
                        results.add(result);
                        System.out.println("🔍 Debug: ✅ Added result to final list!");
                    } else {
                        System.out.println("🔍 Debug: ❌ No match found in this block's metadata");
                    }
                } else {
                    System.out.println("🔍 Debug: ⚠️ Could not decrypt private metadata - wrong password or format issue");
                }
                
            } catch (Exception e) {
                // Skip blocks that can't be decrypted (wrong password or corrupted data)
                // This is expected behavior when password doesn't match
                System.out.println("🔍 Debug: ❌ Failed to decrypt block " + blockHash.substring(0, 8) + ": " + e.getMessage());
                e.printStackTrace();
                continue;
            }
        }
        
        // Sort by relevance and limit results
        return results.stream()
                     .sorted((a, b) -> Double.compare(b.getRelevanceScore(), a.getRelevanceScore()))
                     .limit(maxResults)
                     .collect(Collectors.toList());
    }
    
    
    // ===== PRIVATE HELPER METHODS =====
    
    /**
     * Decrypt private metadata using password
     */
    private PrivateMetadata decryptPrivateMetadata(String blockHash, String encryptedMetadata, String password) {
        // Check cache first
        String cacheKey = blockHash + ":" + password.hashCode();
        PrivateMetadata cached = decryptedCache.get(cacheKey);
        if (cached != null) {
            lastAccessTime.put(cacheKey, System.currentTimeMillis());
            return cached;
        }
        
        try {
            // Decrypt the metadata JSON
            String decryptedJson = CryptoUtil.decryptWithGCM(encryptedMetadata, password);
            
            // Decompress if necessary
            if (CompressionUtil.isCompressed(decryptedJson)) {
                System.out.println("🔍 Debug: Decompressing metadata JSON...");
                decryptedJson = CompressionUtil.decompressString(decryptedJson);
                System.out.println("🔍 Debug: Decompressed JSON: " + decryptedJson.substring(0, Math.min(100, decryptedJson.length())) + "...");
            }
            
            // Parse JSON into PrivateMetadata object
            PrivateMetadata privateMetadata = parsePrivateMetadataJson(decryptedJson);
            
            // Cache the result
            decryptedCache.put(cacheKey, privateMetadata);
            lastAccessTime.put(cacheKey, System.currentTimeMillis());
            
            return privateMetadata;
            
        } catch (Exception e) {
            // Decryption failed - likely wrong password
            return null;
        }
    }
    
    /**
     * Parse JSON string into PrivateMetadata object using Jackson
     */
    private PrivateMetadata parsePrivateMetadataJson(String json) {
        PrivateMetadata metadata = new PrivateMetadata();
        
        if (json == null || json.trim().isEmpty()) {
            System.out.println("🔍 Debug: parsePrivateMetadataJson - JSON is null or empty");
            return metadata;
        }
        
        System.out.println("🔍 Debug: parsePrivateMetadataJson - JSON content: " + json.substring(0, Math.min(200, json.length())) + "...");
        
        try {
            JsonNode jsonNode = objectMapper.readTree(json);
            
            // Extract keywords (check both field names for compatibility)
            JsonNode keywordsNode = jsonNode.get("specificKeywords");
            if (keywordsNode == null) {
                keywordsNode = jsonNode.get("detailedKeywords");
            }
            
            if (keywordsNode != null && keywordsNode.isArray()) {
                Set<String> keywords = new HashSet<>();
                for (JsonNode keyword : keywordsNode) {
                    if (keyword.isTextual()) {
                        keywords.add(keyword.asText());
                    }
                }
                metadata.setDetailedKeywords(keywords);
                System.out.println("🔍 Debug: Extracted keywords: " + keywords);
            } else {
                System.out.println("🔍 Debug: No keywords found in JSON");
            }
            
            // Extract sensitive terms
            JsonNode sensitiveNode = jsonNode.get("sensitiveTerms");
            if (sensitiveNode != null && sensitiveNode.isArray()) {
                Set<String> sensitiveTerms = new HashSet<>();
                for (JsonNode term : sensitiveNode) {
                    if (term.isTextual()) {
                        sensitiveTerms.add(term.asText());
                    }
                }
                metadata.setSensitiveTerms(sensitiveTerms);
                System.out.println("🔍 Debug: Extracted sensitive terms: " + sensitiveTerms);
            }
            
            // Extract identifiers
            JsonNode identifiersNode = jsonNode.get("identifiers");
            if (identifiersNode != null && identifiersNode.isArray()) {
                Set<String> identifiers = new HashSet<>();
                for (JsonNode id : identifiersNode) {
                    if (id.isTextual()) {
                        identifiers.add(id.asText());
                    }
                }
                metadata.setIdentifiers(identifiers);
                System.out.println("🔍 Debug: Extracted identifiers: " + identifiers);
            }
            
            // Extract content summary
            JsonNode summaryNode = jsonNode.get("contentSummary");
            if (summaryNode != null && summaryNode.isTextual()) {
                metadata.setContentSummary(summaryNode.asText());
            }
            
            // Extract content category
            JsonNode categoryNode = jsonNode.get("detailedCategory");
            if (categoryNode != null && categoryNode.isTextual()) {
                metadata.setContentCategory(categoryNode.asText());
            }
            
            return metadata;
            
        } catch (Exception e) {
            System.out.println("🔍 Debug: Failed to parse JSON: " + e.getMessage());
            throw new RuntimeException("Failed to parse private metadata JSON", e);
        }
    }
    
    /**
     * Search within decrypted private metadata
     */
    private EncryptedSearchResult searchPrivateMetadata(String blockHash, PrivateMetadata metadata, 
                                                       Set<String> queryKeywords, String originalQuery, 
                                                       long startTime) {
        System.out.println("🔍 Debug: searchPrivateMetadata called for block " + blockHash.substring(0, 8));
        System.out.println("🔍 Debug: queryKeywords: " + queryKeywords);
        System.out.println("🔍 Debug: metadata.getDetailedKeywords(): " + metadata.getDetailedKeywords());
        System.out.println("🔍 Debug: metadata.getContentSummary(): " + metadata.getContentSummary());
        
        double relevanceScore = 0.0;
        List<String> matchingTerms = new ArrayList<>();
        
        // Search in detailed keywords
        for (String keyword : metadata.getDetailedKeywords()) {
            for (String queryKeyword : queryKeywords) {
                if (keyword.toLowerCase().contains(queryKeyword.toLowerCase())) {
                    relevanceScore += 2.0; // High score for keyword match
                    matchingTerms.add(keyword);
                }
            }
        }
        
        // Search in content summary
        if (metadata.getContentSummary() != null) {
            String summary = metadata.getContentSummary().toLowerCase();
            for (String queryKeyword : queryKeywords) {
                if (summary.contains(queryKeyword.toLowerCase())) {
                    relevanceScore += 1.5; // Medium score for summary match
                    matchingTerms.add("summary:" + queryKeyword);
                }
            }
        }
        
        // Search in identifiers
        for (String identifier : metadata.getIdentifiers()) {
            for (String queryKeyword : queryKeywords) {
                if (identifier.toLowerCase().contains(queryKeyword.toLowerCase())) {
                    relevanceScore += 3.0; // Very high score for identifier match
                    matchingTerms.add("id:" + identifier);
                }
            }
        }
        
        // Check for sensitive content match
        boolean hasSensitiveMatch = false;
        for (String sensitive : metadata.getSensitiveTerms()) {
            for (String queryKeyword : queryKeywords) {
                if (sensitive.toLowerCase().contains(queryKeyword.toLowerCase())) {
                    relevanceScore += 2.5; // High score for sensitive match
                    matchingTerms.add("sensitive:" + sensitive);
                    hasSensitiveMatch = true;
                }
            }
        }
        
        if (relevanceScore > 0.0) {
            long endTime = System.nanoTime();
            double searchTimeMs = (endTime - startTime) / 1_000_000.0;
            
            return new EncryptedSearchResult(
                blockHash,
                relevanceScore,
                searchTimeMs,
                createMatchingSummary(matchingTerms, originalQuery),
                metadata.getContentCategory(),
                hasSensitiveMatch || !metadata.getSensitiveTerms().isEmpty(),
                metadata.getDetailedKeywords().size(),
                matchingTerms
            );
        }
        
        return null; // No matches found
    }
    
    /**
     * Create a summary of matching terms
     */
    private String createMatchingSummary(List<String> matchingTerms, String originalQuery) {
        if (matchingTerms.isEmpty()) {
            return "No specific matches found";
        }
        
        StringBuilder summary = new StringBuilder();
        summary.append("Found ").append(matchingTerms.size()).append(" matches for '").append(originalQuery).append("': ");
        
        // Limit summary length and show most relevant matches
        int maxMatches = Math.min(3, matchingTerms.size());
        for (int i = 0; i < maxMatches; i++) {
            if (i > 0) summary.append(", ");
            summary.append(matchingTerms.get(i));
        }
        
        if (matchingTerms.size() > maxMatches) {
            summary.append(" and ").append(matchingTerms.size() - maxMatches).append(" more...");
        }
        
        return summary.toString();
    }
    
    /**
     * Parse query into individual keywords
     */
    private Set<String> parseQuery(String query) {
        return Arrays.stream(query.toLowerCase().split("\\s+"))
                     .filter(word -> word.length() > 1)
                     .collect(Collectors.toSet());
    }
    
    /**
     * Clean up expired cache entries
     */
    private void cleanupExpiredCache() {
        long currentTime = System.currentTimeMillis();
        
        // Clean up encrypted metadata cache
        lastAccessTime.entrySet().removeIf(entry -> {
            boolean expired = (currentTime - entry.getValue()) > CACHE_EXPIRY_MS;
            if (expired) {
                encryptedMetadataCache.remove(entry.getKey());
            }
            return expired;
        });
        
        // Clean up decrypted cache
        decryptedCache.entrySet().removeIf(entry -> {
            Long lastAccess = lastAccessTime.get(entry.getKey());
            boolean expired = lastAccess == null || (currentTime - lastAccess) > CACHE_EXPIRY_MS;
            if (expired) {
                lastAccessTime.remove(entry.getKey());
            }
            return expired;
        });
    }
    
    // ===== INDEX MANAGEMENT =====
    
    /**
     * Remove block from encrypted search index
     */
    public void removeBlock(String blockHash) {
        if (blockHash == null) {
            return;
        }
        
        encryptedMetadataCache.remove(blockHash);
        lastAccessTime.remove(blockHash);
        
        // Remove from decrypted cache (all password variants)
        decryptedCache.entrySet().removeIf(entry -> entry.getKey().startsWith(blockHash + ":"));
    }
    
    /**
     * Get encrypted search index statistics
     */
    public EncryptedIndexStats getEncryptedIndexStats() {
        cleanupExpiredCache();
        
        return new EncryptedIndexStats(
            encryptedMetadataCache.size(),
            decryptedCache.size(),
            calculateIndexMemoryUsage()
        );
    }
    
    /**
     * Calculate approximate memory usage
     */
    private long calculateIndexMemoryUsage() {
        long size = 0;
        
        // Encrypted metadata cache
        for (String encrypted : encryptedMetadataCache.values()) {
            size += encrypted.length() * 2; // Approximate UTF-16 encoding
        }
        
        // Decrypted cache (approximate)
        size += decryptedCache.size() * 1024; // Rough estimate per PrivateMetadata object
        
        return size;
    }
    
    /**
     * Clear all caches
     */
    public void clearAll() {
        encryptedMetadataCache.clear();
        lastAccessTime.clear();
        decryptedCache.clear();
    }
    
    // ===== RESULT CLASSES =====
    
    /**
     * Encrypted search result with detailed information
     */
    public static class EncryptedSearchResult {
        private final String blockHash;
        private final double relevanceScore;
        private final double searchTimeMs;
        private final String matchingSummary;
        private final String contentCategory;
        private final boolean hasSensitiveContent;
        private final int totalKeywords;
        private final List<String> matchingTerms;
        
        public EncryptedSearchResult(String blockHash, double relevanceScore, double searchTimeMs,
                                   String matchingSummary, String contentCategory, 
                                   boolean hasSensitiveContent, int totalKeywords,
                                   List<String> matchingTerms) {
            this.blockHash = blockHash;
            this.relevanceScore = relevanceScore;
            this.searchTimeMs = searchTimeMs;
            this.matchingSummary = matchingSummary;
            this.contentCategory = contentCategory;
            this.hasSensitiveContent = hasSensitiveContent;
            this.totalKeywords = totalKeywords;
            this.matchingTerms = matchingTerms != null ? matchingTerms : new ArrayList<>();
        }
        
        public String getBlockHash() { return blockHash; }
        public double getRelevanceScore() { return relevanceScore; }
        public double getSearchTimeMs() { return searchTimeMs; }
        public String getMatchingSummary() { return matchingSummary; }
        public String getContentCategory() { return contentCategory; }
        public boolean hasSensitiveContent() { return hasSensitiveContent; }
        public int getTotalKeywords() { return totalKeywords; }
        public List<String> getMatchingTerms() { return matchingTerms; }
        
        @Override
        public String toString() {
            return String.format("EncryptedResult{hash=%s, score=%.2f, category=%s, sensitive=%s}", 
                               blockHash.substring(0, Math.min(8, blockHash.length())), 
                               relevanceScore, contentCategory, hasSensitiveContent);
        }
    }
    
    /**
     * Encrypted index statistics
     */
    public static class EncryptedIndexStats {
        private final int encryptedBlocksIndexed;
        private final int decryptedCacheSize;
        private final long estimatedMemoryBytes;
        
        public EncryptedIndexStats(int encryptedBlocksIndexed, int decryptedCacheSize, long estimatedMemoryBytes) {
            this.encryptedBlocksIndexed = encryptedBlocksIndexed;
            this.decryptedCacheSize = decryptedCacheSize;
            this.estimatedMemoryBytes = estimatedMemoryBytes;
        }
        
        public int getEncryptedBlocksIndexed() { return encryptedBlocksIndexed; }
        public int getDecryptedCacheSize() { return decryptedCacheSize; }
        public long getEstimatedMemoryBytes() { return estimatedMemoryBytes; }
        
        @Override
        public String toString() {
            return String.format("EncryptedIndexStats{encrypted=%d, cached=%d, memory=%dKB}", 
                               encryptedBlocksIndexed, decryptedCacheSize, estimatedMemoryBytes / 1024);
        }
    }
}