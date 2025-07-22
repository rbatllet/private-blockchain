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
    private static final DateTimeFormatter DATE_ONLY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
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
     * Shows first 16 and last 16 characters with "..." in between for long hashes
     * @param hash The hash string to truncate
     * @return The truncated hash string
     */
    public static String truncateHash(String hash) {
        if (hash == null) return "null";
        return hash.length() > 32 ? 
            hash.substring(0, 16) + "..." + hash.substring(hash.length() - 16) :
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
    
    /**
     * Truncate a public key string for display purposes
     * Shows first 20 and last 20 characters with "..." in between for long keys
     * @param key The key string to truncate
     * @return The truncated key string
     */
    public static String truncateKey(String key) {
        if (key == null) return "null";
        return key.length() > 40 ? 
            key.substring(0, 20) + "..." + key.substring(key.length() - 20) :
            key;
    }
    
    /**
     * Escape a string for safe JSON output
     * Handles quotes, newlines, carriage returns, and tabs
     * @param str The string to escape
     * @return The escaped string safe for JSON
     */
    public static String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\"", "\\\"")
                 .replace("\n", "\\n")
                 .replace("\r", "\\r")
                 .replace("\t", "\\t");
    }
    
    /**
     * Format byte count as human-readable string
     * Uses appropriate units: B, KB, MB, GB
     * @param bytes The number of bytes to format
     * @return The formatted string with appropriate units
     */
    public static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
    
    /**
     * Format a timestamp for date-only display
     * @param timestamp The timestamp to format
     * @return The formatted date string (yyyy-MM-dd)
     */
    public static String formatDate(LocalDateTime timestamp) {
        if (timestamp == null) return "null";
        return timestamp.format(DATE_ONLY_FORMATTER);
    }
    
    /**
     * Format a duration in nanoseconds to milliseconds with appropriate precision
     * @param nanos Duration in nanoseconds
     * @return The formatted duration string
     */
    public static String formatDuration(long nanos) {
        double millis = nanos / 1_000_000.0;
        if (millis < 0.1) {
            return String.format("%.3f ms", millis);
        } else if (millis < 10) {
            return String.format("%.2f ms", millis);
        } else if (millis < 100) {
            return String.format("%.1f ms", millis);
        } else {
            return String.format("%.0f ms", millis);
        }
    }
    
    /**
     * Format a percentage with appropriate precision
     * @param value The value to format as percentage (0-100)
     * @return The formatted percentage string
     */
    public static String formatPercentage(double value) {
        if (value == 0) return "0%";
        if (value == 100) return "100%";
        if (value < 1) return String.format("%.2f%%", value);
        if (value < 10) return String.format("%.1f%%", value);
        return String.format("%.0f%%", value);
    }
    
    /**
     * Format blockchain state information for table display
     * @param blockCount Number of blocks
     * @param validChain Whether the chain is valid
     * @param lastBlockTime Time of the last block
     * @return The formatted state string
     */
    public static String formatBlockchainState(long blockCount, boolean validChain, LocalDateTime lastBlockTime) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-20s: %d%n", "Total Blocks", blockCount));
        sb.append(String.format("%-20s: %s%n", "Chain Valid", validChain ? "Yes" : "No"));
        sb.append(String.format("%-20s: %s%n", "Last Block Time", 
            lastBlockTime != null ? formatTimestamp(lastBlockTime) : "N/A"));
        return sb.toString();
    }
}
