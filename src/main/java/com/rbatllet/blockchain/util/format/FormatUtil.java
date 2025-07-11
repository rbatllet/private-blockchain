package com.rbatllet.blockchain.util.format;

import com.rbatllet.blockchain.entity.Block;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for formatting blockchain data
 * Migrated from CLI project to core for better reusability
 */
public class FormatUtil {
    
    private static final DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Format a timestamp using the default format
     * @param timestamp The timestamp to format
     * @return The formatted timestamp string
     */
    public static String formatTimestamp(LocalDateTime timestamp) {
        if (timestamp == null) return "null";
        return timestamp.format(DEFAULT_FORMATTER);
    }
    
    /**
     * Format a timestamp using a custom format
     * @param timestamp The timestamp to format
     * @param pattern The pattern to use for formatting
     * @return The formatted timestamp string
     */
    public static String formatTimestamp(LocalDateTime timestamp, String pattern) {
        if (timestamp == null) return "null";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return timestamp.format(formatter);
    }
    
    /**
     * Truncate a hash string for display purposes
     * @param hash The hash string to truncate
     * @return The truncated hash string
     */
    public static String truncateHash(String hash) {
        if (hash == null) return "null";
        return hash.length() > 32 ? 
            hash.substring(0, 16) + "..." + hash.substring(hash.length() - 20, hash.length()) :
            hash;
    }
    
    /**
     * Format block information for display
     * @param block The block to format
     * @return A formatted string with block information
     */
    public static String formatBlockInfo(Block block) {
        if (block == null) return "null";
        
        StringBuilder sb = new StringBuilder();
        sb.append("Block #").append(block.getBlockNumber()).append("\n");
        sb.append("Timestamp: ").append(formatTimestamp(block.getTimestamp())).append("\n");
        
        // Format hash values consistently using the truncateHash method
        String hash = block.getHash();
        String prevHash = block.getPreviousHash();
        
        sb.append("Hash: ").append(truncateHash(hash)).append("\n");
        sb.append("Previous Hash: ").append(truncateHash(prevHash)).append("\n");
        
        sb.append("Data Length: ").append(block.getData().length()).append(" chars");
        
        return sb.toString();
    }
    
    /**
     * Format a string to a fixed width by truncating or padding
     * @param input The input string
     * @param width The desired width
     * @return The formatted string
     */
    public static String fixedWidth(String input, int width) {
        if (input == null) return " ".repeat(width);
        
        if (input.length() > width) {
            // Calculate how much of the original string we can keep
            // We need to reserve 3 characters for the ellipsis
            // If width is very small (less than 4), we'll show at least 1 character
            int ellipsisLength = 3; // "..." is 3 characters
            int preserveLength = Math.max(1, width - ellipsisLength);
            
            // For aesthetics, try to break at a word boundary if possible
            // but only if we have enough space to make it worthwhile
            if (preserveLength > 5) {
                int lastSpace = input.substring(0, preserveLength).lastIndexOf(' ');
                if (lastSpace > preserveLength / 2) {
                    preserveLength = lastSpace;
                }
            }
            
            return input.substring(0, preserveLength) + "...";
        } else {
            return input + " ".repeat(width - input.length());
        }
    }
}
