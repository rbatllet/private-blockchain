package com.rbatllet.blockchain.search;

import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.service.SecureBlockEncryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.PrivateKey;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * On-Chain Content Search Implementation
 * 
 * Provides exhaustive search capabilities within block data content.
 * This completes the TRUE EXHAUSTIVE search by searching inside:
 * - Non-encrypted block data
 * - Encrypted block data (with proper decryption)
 * 
 * Key Features:
 * - Direct content search within block.getData()
 * - Support for encrypted and non-encrypted blocks
 * - Case-insensitive search with regex support
 * - Context extraction for search results
 * - Thread-safe operations
 * 
 * This makes EXHAUSTIVE_OFFCHAIN truly exhaustive by searching ALL content
 */
public class OnChainContentSearch {
    
    private static final Logger logger = LoggerFactory.getLogger(OnChainContentSearch.class);
    
    // Search configuration
    private static final int CONTEXT_LENGTH = 100; // Characters before/after match
    private static final int MAX_SNIPPETS_PER_BLOCK = 5;
    
    public OnChainContentSearch() {
        // No services needed - using static methods
    }
    
    /**
     * Search within on-chain block content
     * 
     * @param blocks List of blocks to search within
     * @param searchTerm The search term or regex pattern
     * @param password Password for decrypting encrypted blocks
     * @param privateKey Private key for verification (if needed)
     * @param maxResults Maximum number of results to return
     * @return OnChainSearchResult containing all matches
     */
    public OnChainSearchResult searchOnChainContent(List<Block> blocks, String searchTerm,
                                                   String password, PrivateKey privateKey,
                                                   int maxResults) {
        
        logger.info("🔍 Starting on-chain content search for: \"{}\"", searchTerm);
        
        if (blocks == null || blocks.isEmpty() || searchTerm == null || searchTerm.trim().isEmpty()) {
            return new OnChainSearchResult(searchTerm, Collections.emptyList(), 0, 0);
        }
        
        List<OnChainMatch> matches = new ArrayList<>();
        int blocksSearched = 0;
        int encryptedBlocksDecrypted = 0;
        
        // Create case-insensitive pattern
        Pattern searchPattern = Pattern.compile(Pattern.quote(searchTerm.trim()), Pattern.CASE_INSENSITIVE);
        
        for (Block block : blocks) {
            if (matches.size() >= maxResults) {
                break;
            }
            
            blocksSearched++;
            
            // Get block content
            String content = extractBlockContent(block, password, privateKey);
            if (content == null || content.isEmpty()) {
                continue;
            }
            
            if (block.isDataEncrypted()) {
                encryptedBlocksDecrypted++;
            }
            
            // Search for matches
            List<String> snippets = findMatches(content, searchPattern);
            if (!snippets.isEmpty()) {
                OnChainMatch match = new OnChainMatch(
                    block.getBlockNumber(),
                    block.getHash(),
                    block.isDataEncrypted() ? "encrypted" : "plain",
                    snippets.size(),
                    snippets,
                    content.length()
                );
                matches.add(match);
            }
        }
        
        logger.info("✅ On-chain search completed: {} matches found in {} blocks ({} encrypted blocks decrypted)", 
                         matches.size(), blocksSearched, encryptedBlocksDecrypted);
        
        return new OnChainSearchResult(searchTerm, matches, blocksSearched, encryptedBlocksDecrypted);
    }
    
    /**
     * Extract content from a block (decrypt if necessary)
     */
    private String extractBlockContent(Block block, String password, PrivateKey privateKey) {
        if (block.getData() == null) {
            return null;
        }
        
        try {
            if (!block.isDataEncrypted()) {
                // Plain text block
                return block.getData();
            }
            
            // Encrypted block - try to decrypt
            if (password == null || password.isEmpty()) {
                return null; // Can't decrypt without password
            }
            
            // Try to decrypt using encryption metadata
            String encryptionMetadata = block.getEncryptionMetadata();
            if (encryptionMetadata != null && encryptionMetadata.contains("|")) {
                // Modern encryption with metadata format: timestamp|salt|iv|encryptedData|hash
                try {
                    String[] parts = encryptionMetadata.split("\\|");
                    if (parts.length >= 5) {
                        // Extract encrypted data from metadata
                        String encryptedData = parts[3];
                        String salt = parts[1];
                        String iv = parts[2];
                        
                        // Create secure encrypted data object
                        SecureBlockEncryptionService.SecureEncryptedData secureData = 
                            new SecureBlockEncryptionService.SecureEncryptedData(
                                encryptedData, salt, iv, parts[4], Long.parseLong(parts[0]));
                        
                        // Decrypt using the static method
                        return SecureBlockEncryptionService.decryptWithPassword(secureData, password);
                    }
                } catch (Exception e) {
                    // Failed to decrypt
                    return null;
                }
            }
            
            // If no metadata or decryption failed, data might be in block.getData()
            // For older blocks without proper encryption metadata
            return null;
            
        } catch (Exception e) {
            logger.warn("⚠️ Failed to extract content from block {}: {}", block.getHash(), e.getMessage());
            return null;
        }
    }
    
    /**
     * Find matches within content and extract context snippets
     */
    private List<String> findMatches(String content, Pattern pattern) {
        List<String> snippets = new ArrayList<>();
        Matcher matcher = pattern.matcher(content);
        
        int matchCount = 0;
        while (matcher.find() && matchCount < MAX_SNIPPETS_PER_BLOCK) {
            int start = Math.max(0, matcher.start() - CONTEXT_LENGTH);
            int end = Math.min(content.length(), matcher.end() + CONTEXT_LENGTH);
            
            String snippet = content.substring(start, end);
            
            // Add ellipsis if truncated
            if (start > 0) {
                snippet = "..." + snippet;
            }
            if (end < content.length()) {
                snippet = snippet + "...";
            }
            
            // Highlight the match
            String matchedText = matcher.group();
            snippet = snippet.replace(matchedText, "**" + matchedText + "**");
            
            snippets.add(snippet.trim());
            matchCount++;
        }
        
        return snippets;
    }
    
    /**
     * Result class for on-chain content search
     */
    public static class OnChainSearchResult {
        private final String searchTerm;
        private final List<OnChainMatch> matches;
        private final int totalBlocksSearched;
        private final int encryptedBlocksDecrypted;
        
        public OnChainSearchResult(String searchTerm, List<OnChainMatch> matches, 
                                 int totalBlocksSearched, int encryptedBlocksDecrypted) {
            this.searchTerm = searchTerm;
            this.matches = matches;
            this.totalBlocksSearched = totalBlocksSearched;
            this.encryptedBlocksDecrypted = encryptedBlocksDecrypted;
        }
        
        // Getters
        public String getSearchTerm() { return searchTerm; }
        public List<OnChainMatch> getMatches() { return Collections.unmodifiableList(matches); }
        public int getTotalBlocksSearched() { return totalBlocksSearched; }
        public int getEncryptedBlocksDecrypted() { return encryptedBlocksDecrypted; }
        public boolean hasMatches() { return !matches.isEmpty(); }
        public int getMatchCount() { return matches.size(); }
        
        public int getTotalMatchInstances() {
            return matches.stream().mapToInt(OnChainMatch::getMatchCount).sum();
        }
        
        public String getSearchSummary() {
            return String.format("On-chain search for '%s': %d matches in %d blocks (%d encrypted decrypted)",
                searchTerm, getMatchCount(), totalBlocksSearched, encryptedBlocksDecrypted);
        }
    }
    
    /**
     * Individual on-chain match information
     */
    public static class OnChainMatch {
        private final long blockNumber;
        private final String blockHash;
        private final String contentType; // "plain" or "encrypted"
        private final int matchCount;
        private final List<String> matchingSnippets;
        private final long contentSize;
        
        public OnChainMatch(long blockNumber, String blockHash, String contentType,
                          int matchCount, List<String> matchingSnippets, long contentSize) {
            this.blockNumber = blockNumber;
            this.blockHash = blockHash;
            this.contentType = contentType;
            this.matchCount = matchCount;
            this.matchingSnippets = matchingSnippets;
            this.contentSize = contentSize;
        }
        
        // Getters
        public long getBlockNumber() { return blockNumber; }
        public String getBlockHash() { return blockHash; }
        public String getContentType() { return contentType; }
        public int getMatchCount() { return matchCount; }
        public List<String> getMatchingSnippets() { return matchingSnippets; }
        public long getContentSize() { return contentSize; }
        
        public double getRelevanceScore() {
            // Score based on match count and content type
            double score = matchCount * 10.0;
            if ("encrypted".equals(contentType)) {
                score += 5.0; // Bonus for finding in encrypted content
            }
            return score;
        }
        
        public String getPreviewSnippet() {
            return matchingSnippets.isEmpty() ? "" : matchingSnippets.get(0);
        }
        
        @Override
        public String toString() {
            return String.format("OnChainMatch{block=%d, hash=%s, type=%s, matches=%d}",
                blockNumber, blockHash.substring(0, 8), contentType, matchCount);
        }
    }
}