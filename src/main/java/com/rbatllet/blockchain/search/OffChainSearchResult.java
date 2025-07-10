package com.rbatllet.blockchain.search;

import java.util.List;

/**
 * Result container for off-chain file search operations
 * 
 * Contains search results from EXHAUSTIVE_OFFCHAIN searches that
 * examine the actual content of encrypted off-chain files.
 */
public class OffChainSearchResult {
    
    private final String searchTerm;
    private final List<OffChainMatch> matches;
    private final int totalFilesSearched;
    private final long searchTimestamp;
    
    public OffChainSearchResult(String searchTerm, List<OffChainMatch> matches, int totalFilesSearched) {
        this.searchTerm = searchTerm;
        this.matches = matches;
        this.totalFilesSearched = totalFilesSearched;
        this.searchTimestamp = System.currentTimeMillis();
    }
    
    public String getSearchTerm() {
        return searchTerm;
    }
    
    public List<OffChainMatch> getMatches() {
        return matches;
    }
    
    public int getMatchCount() {
        return matches.size();
    }
    
    public int getTotalFilesSearched() {
        return totalFilesSearched;
    }
    
    public long getSearchTimestamp() {
        return searchTimestamp;
    }
    
    public boolean hasMatches() {
        return matches != null && !matches.isEmpty();
    }
    
    /**
     * Get total number of individual matches across all files
     */
    public int getTotalMatchInstances() {
        return matches.stream()
                     .mapToInt(OffChainMatch::getMatchCount)
                     .sum();
    }
    
    /**
     * Get summary statistics
     */
    public String getSearchSummary() {
        return String.format("Found %d matches in %d files (searched %d total files) for term: '%s'",
                           getTotalMatchInstances(), getMatchCount(), totalFilesSearched, searchTerm);
    }
    
    @Override
    public String toString() {
        return "OffChainSearchResult{" +
               "searchTerm='" + searchTerm + '\'' +
               ", matchCount=" + getMatchCount() +
               ", totalMatchInstances=" + getTotalMatchInstances() +
               ", totalFilesSearched=" + totalFilesSearched +
               ", searchTimestamp=" + searchTimestamp +
               '}';
    }
}