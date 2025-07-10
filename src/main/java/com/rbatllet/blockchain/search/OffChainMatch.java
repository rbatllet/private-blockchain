package com.rbatllet.blockchain.search;

import java.util.List;

/**
 * Represents a match found within an off-chain file
 * 
 * Contains details about where the match was found and context snippets
 * showing the matched content within the decrypted off-chain file.
 */
public class OffChainMatch {
    
    private final long blockNumber;
    private final String blockHash;
    private final String filePath;
    private final String contentType;
    private final int matchCount;
    private final List<String> matchingSnippets;
    private final long fileSize;
    
    public OffChainMatch(long blockNumber, String blockHash, String filePath, 
                        String contentType, int matchCount, List<String> matchingSnippets,
                        long fileSize) {
        this.blockNumber = blockNumber;
        this.blockHash = blockHash;
        this.filePath = filePath;
        this.contentType = contentType;
        this.matchCount = matchCount;
        this.matchingSnippets = matchingSnippets;
        this.fileSize = fileSize;
    }
    
    public long getBlockNumber() {
        return blockNumber;
    }
    
    public String getBlockHash() {
        return blockHash;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public String getContentType() {
        return contentType;
    }
    
    public int getMatchCount() {
        return matchCount;
    }
    
    public List<String> getMatchingSnippets() {
        return matchingSnippets;
    }
    
    public long getFileSize() {
        return fileSize;
    }
    
    /**
     * Get the filename from the full path
     */
    public String getFileName() {
        if (filePath == null) return null;
        int lastSlash = filePath.lastIndexOf('/');
        if (lastSlash == -1) lastSlash = filePath.lastIndexOf('\\');
        return lastSlash >= 0 ? filePath.substring(lastSlash + 1) : filePath;
    }
    
    /**
     * Check if this is a text-based content type
     */
    public boolean isTextContent() {
        return contentType != null && 
               (contentType.startsWith("text/") || 
                contentType.equals("application/json") ||
                contentType.equals("application/xml") ||
                contentType.equals("application/yaml"));
    }
    
    /**
     * Get a preview of the first matching snippet
     */
    public String getPreviewSnippet() {
        if (matchingSnippets == null || matchingSnippets.isEmpty()) {
            return null;
        }
        
        String snippet = matchingSnippets.get(0);
        if (snippet.length() > 150) {
            return snippet.substring(0, 147) + "...";
        }
        return snippet;
    }
    
    /**
     * Get relevance score based on match count and content type
     */
    public double getRelevanceScore() {
        double score = matchCount * 10.0; // Base score from match count
        
        // Bonus for text content (easier to read/more relevant)
        if (isTextContent()) {
            score += 5.0;
        }
        
        // Small bonus for larger files (more comprehensive data)
        if (fileSize > 1024) {
            score += Math.log(fileSize / 1024.0);
        }
        
        return score;
    }
    
    @Override
    public String toString() {
        return "OffChainMatch{" +
               "blockNumber=" + blockNumber +
               ", blockHash='" + blockHash + '\'' +
               ", fileName='" + getFileName() + '\'' +
               ", contentType='" + contentType + '\'' +
               ", matchCount=" + matchCount +
               ", fileSize=" + fileSize +
               ", relevanceScore=" + String.format("%.2f", getRelevanceScore()) +
               '}';
    }
    
    /**
     * Get a detailed description of the match for display
     */
    public String getDetailedDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("📄 File: ").append(getFileName()).append("\n");
        sb.append("🔗 Block: #").append(blockNumber).append(" (").append(blockHash.substring(0, 8)).append("...)\n");
        sb.append("📋 Type: ").append(contentType).append("\n");
        sb.append("🎯 Matches: ").append(matchCount).append("\n");
        sb.append("📊 Size: ").append(formatFileSize(fileSize)).append("\n");
        
        if (matchingSnippets != null && !matchingSnippets.isEmpty()) {
            sb.append("📖 Preview: ").append(getPreviewSnippet()).append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * Format file size in human-readable format
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}