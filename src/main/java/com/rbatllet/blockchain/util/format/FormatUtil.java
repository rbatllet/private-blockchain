package com.rbatllet.blockchain.util.format;

import com.rbatllet.blockchain.entity.Block;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for formatting blockchain data
 * Migrated from CLI project to core for better reusability
 *
 * This class has been robustified with:
 * - Comprehensive input validation
 * - Consistent null handling
 * - Proper error messages
 * - Private constructor to prevent instantiation
 */
public class FormatUtil {

    private static final DateTimeFormatter DEFAULT_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_ONLY_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Constants for input validation
    private static final int MAX_STRING_LENGTH = 100_000;
    private static final int MAX_SEPARATOR_LENGTH = 10_000;
    private static final int MAX_WIDTH = 10_000;
    private static final long MAX_BYTES = Long.MAX_VALUE / 2; // Avoid overflow in calculations
    private static final long MAX_NANOS = Long.MAX_VALUE / 1_000_000; // Avoid overflow in conversion

    /**
     * Private constructor to prevent instantiation of utility class
     * @throws AssertionError if instantiation is attempted
     */
    private FormatUtil() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * Format a timestamp using the default format
     * @param timestamp The timestamp to format
     * @return The formatted timestamp string, or "null" if timestamp is null
     */
    public static String formatTimestamp(LocalDateTime timestamp) {
        if (timestamp == null) return "null";
        return timestamp.format(DEFAULT_FORMATTER);
    }

    /**
     * Format a timestamp using a custom format
     * @param timestamp The timestamp to format
     * @param pattern The pattern to use for formatting (cannot be null or empty)
     * @return The formatted timestamp string, or "null" if timestamp is null
     * @throws IllegalArgumentException if pattern is null or empty
     */
    public static String formatTimestamp(
        LocalDateTime timestamp,
        String pattern
    ) {
        if (pattern == null || pattern.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Pattern cannot be null or empty"
            );
        }
        if (pattern.length() > 1000) {
            throw new IllegalArgumentException(
                "Pattern too long (max 1000 chars): " + pattern.length()
            );
        }
        if (timestamp == null) return "null";

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            return timestamp.format(formatter);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                "Invalid date pattern: " + pattern,
                e
            );
        }
    }

    /**
     * Truncate a hash string for display purposes
     * Shows first 16 and last 16 characters with "..." in between for long hashes
     * @param hash The hash string to truncate
     * @return The truncated hash string, or "null" if hash is null
     */
    public static String truncateHash(String hash) {
        if (hash == null) return "null";
        if (hash.length() <= 32) return hash;

        // Limit input length for memory safety
        if (hash.length() > MAX_STRING_LENGTH) {
            hash = hash.substring(0, MAX_STRING_LENGTH);
        }

        return (
            hash.substring(0, 16) + "..." + hash.substring(hash.length() - 16)
        );
    }

    /**
     * Format block information for display
     * @param block The block to format
     * @return A formatted string with block information, or "null" if block is null
     */
    public static String formatBlockInfo(Block block) {
        if (block == null) return "null";

        try {
            StringBuilder sb = new StringBuilder(256); // Pre-allocate reasonable size

            // Validate block number
            long blockNumber = block.getBlockNumber();
            if (blockNumber < 0) {
                sb.append("Block #INVALID(").append(blockNumber).append(")");
            } else {
                sb.append("Block #").append(blockNumber);
            }
            sb.append("\n");

            // Format timestamp with error handling
            LocalDateTime timestamp = block.getTimestamp();
            sb.append("Timestamp: ");
            if (
                timestamp != null &&
                timestamp.getYear() > 1900 &&
                timestamp.getYear() < 3000
            ) {
                sb.append(formatTimestamp(timestamp));
            } else {
                sb.append("INVALID/NULL");
            }
            sb.append("\n");

            // Format hash values with validation
            String hash = block.getHash();
            String prevHash = block.getPreviousHash();

            sb.append("Hash: ");
            if (hash != null && !hash.trim().isEmpty()) {
                sb.append(truncateHash(hash));
            } else {
                sb.append("MISSING");
            }
            sb.append("\n");

            sb.append("Previous Hash: ");
            if (prevHash != null && !prevHash.trim().isEmpty()) {
                sb.append(truncateHash(prevHash));
            } else {
                sb.append("MISSING");
            }
            sb.append("\n");

            // Handle data with size limits
            String data = block.getData();
            int dataLength = 0;
            if (data != null) {
                dataLength = data.length();
                if (dataLength > 1_000_000) {
                    sb
                        .append("Data Length: ")
                        .append(dataLength)
                        .append(" chars (WARNING: Very large)");
                } else {
                    sb
                        .append("Data Length: ")
                        .append(dataLength)
                        .append(" chars");
                }
            } else {
                sb.append("Data Length: 0 chars (null)");
            }

            return sb.toString();
        } catch (Exception e) {
            return "Error formatting block: " + e.getMessage();
        }
    }

    /**
     * Format a string to a fixed width by truncating or padding
     * @param input The input string
     * @param width The desired width (must be non-negative)
     * @return The formatted string
     * @throws IllegalArgumentException if width is negative
     */
    public static String fixedWidth(String input, int width) {
        if (width < 0) {
            throw new IllegalArgumentException(
                "Width cannot be negative: " + width
            );
        }
        if (width > MAX_WIDTH) {
            throw new IllegalArgumentException(
                "Width too large (max " + MAX_WIDTH + "): " + width
            );
        }

        if (input == null) return " ".repeat(width);

        // Limit input length for memory safety
        if (input.length() > MAX_STRING_LENGTH) {
            input = input.substring(0, MAX_STRING_LENGTH);
        }

        if (input.length() > width) {
            if (width <= 3) {
                return input.substring(0, width);
            }

            // Calculate how much of the original string we can keep
            int ellipsisLength = 3; // "..." is 3 characters
            int preserveLength = Math.max(1, width - ellipsisLength);

            // For aesthetics, try to break at a word boundary if possible
            // but only if we have enough space to make it worthwhile
            if (preserveLength > 5) {
                int lastSpace = input
                    .substring(0, preserveLength)
                    .lastIndexOf(' ');
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
     * @return The truncated key string, or "null" if key is null
     */
    public static String truncateKey(String key) {
        if (key == null) return "null";
        if (key.length() <= 40) return key;

        // Limit input length for memory safety
        if (key.length() > MAX_STRING_LENGTH) {
            key = key.substring(0, MAX_STRING_LENGTH);
        }

        return key.substring(0, 20) + "..." + key.substring(key.length() - 20);
    }

    /**
     * Escape a string for safe JSON output
     * Handles quotes, newlines, carriage returns, tabs, and backslashes
     * @param str The string to escape
     * @return The escaped string safe for JSON, or "null" if str is null
     */
    public static String escapeJson(String str) {
        if (str == null) return "null"; // Consistent with other methods

        // Limit input length for memory safety
        if (str.length() > MAX_STRING_LENGTH) {
            str = str.substring(0, MAX_STRING_LENGTH - 3) + "...";
        }

        StringBuilder sb = new StringBuilder(str.length() * 2); // Pre-allocate for efficiency
        for (char c : str.toCharArray()) {
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '"' -> sb.append("\\\"");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case
                    '\u0000',
                    '\u0001',
                    '\u0002',
                    '\u0003',
                    '\u0004',
                    '\u0005',
                    '\u0006',
                    '\u0007',
                    '\u000E',
                    '\u000F',
                    '\u0010',
                    '\u0011',
                    '\u0012',
                    '\u0013',
                    '\u0014',
                    '\u0015',
                    '\u0016',
                    '\u0017',
                    '\u0018',
                    '\u0019',
                    '\u001A',
                    '\u001B',
                    '\u001C',
                    '\u001D',
                    '\u001E',
                    '\u001F' -> sb.append(String.format("\\u%04x", (int) c));
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Format byte count as human-readable string
     * Uses appropriate units: B, KB, MB, GB
     * @param bytes The number of bytes to format (must be non-negative)
     * @return The formatted string with appropriate units
     * @throws IllegalArgumentException if bytes is negative
     */
    public static String formatBytes(long bytes) {
        if (bytes < 0) {
            throw new IllegalArgumentException(
                "Bytes cannot be negative: " + bytes
            );
        }
        if (bytes > MAX_BYTES) {
            throw new IllegalArgumentException(
                "Bytes value too large (max " + MAX_BYTES + "): " + bytes
            );
        }

        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(
            "%.1f KB",
            bytes / 1024.0
        );
        if (bytes < 1024L * 1024L * 1024L) return String.format(
            "%.1f MB",
            bytes / (1024.0 * 1024.0)
        );
        if (bytes < 1024L * 1024L * 1024L * 1024L) return String.format(
            "%.1f GB",
            bytes / (1024.0 * 1024.0 * 1024.0)
        );
        return String.format(
            "%.1f TB",
            bytes / (1024.0 * 1024.0 * 1024.0 * 1024.0)
        );
    }

    /**
     * Format a timestamp for date-only display
     * @param timestamp The timestamp to format
     * @return The formatted date string (yyyy-MM-dd), or "null" if timestamp is null
     */
    public static String formatDate(LocalDateTime timestamp) {
        if (timestamp == null) return "null";
        return timestamp.format(DATE_ONLY_FORMATTER);
    }

    /**
     * Format a duration in nanoseconds to milliseconds with appropriate precision
     * @param nanos Duration in nanoseconds (must be non-negative)
     * @return The formatted duration string
     * @throws IllegalArgumentException if nanos is negative
     */
    public static String formatDuration(long nanos) {
        if (nanos < 0) {
            throw new IllegalArgumentException(
                "Duration cannot be negative: " + nanos
            );
        }
        if (nanos > MAX_NANOS) {
            throw new IllegalArgumentException(
                "Duration value too large (max " +
                MAX_NANOS +
                " nanos): " +
                nanos
            );
        }

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
     * @param value The value to format as percentage (can be any finite number)
     * @return The formatted percentage string
     * @throws IllegalArgumentException if value is NaN
     */
    public static String formatPercentage(double value) {
        if (Double.isNaN(value)) {
            return "N/A";
        }
        if (Double.isInfinite(value)) {
            return value > 0 ? "∞%" : "-∞%";
        }
        if (Math.abs(value) > 1_000_000) {
            return value > 0 ? ">1M%" : "<-1M%";
        }

        if (value == 0) return "0%";
        if (value == 100) return "100%";

        double absValue = Math.abs(value);
        if (absValue < 0.01) return String.format("%.3f%%", value);
        if (absValue < 1) return String.format("%.2f%%", value);
        if (absValue < 10) return String.format("%.1f%%", value);
        return String.format("%.0f%%", value);
    }

    /**
     * Format blockchain state information for table display
     * @param blockCount Number of blocks (must be non-negative)
     * @param validChain Whether the chain is valid
     * @param lastBlockTime Time of the last block (can be null)
     * @return The formatted state string
     * @throws IllegalArgumentException if blockCount is negative or too large
     */
    public static String formatBlockchainState(
        long blockCount,
        boolean validChain,
        LocalDateTime lastBlockTime
    ) {
        if (blockCount < 0) {
            throw new IllegalArgumentException(
                "Block count cannot be negative: " + blockCount
            );
        }
        if (blockCount > 1_000_000_000L) {
            throw new IllegalArgumentException(
                "Block count too large (max 1 billion): " + blockCount
            );
        }

        try {
            StringBuilder sb = new StringBuilder(128);
            sb.append(
                String.format("%-20s: %,d%n", "Total Blocks", blockCount)
            );
            sb.append(
                String.format(
                    "%-20s: %s%n",
                    "Chain Valid",
                    validChain ? "Yes" : "No"
                )
            );

            if (lastBlockTime != null) {
                // Validate timestamp is reasonable
                if (
                    lastBlockTime.getYear() > 1900 &&
                    lastBlockTime.getYear() < 3000
                ) {
                    sb.append(
                        String.format(
                            "%-20s: %s%n",
                            "Last Block Time",
                            formatTimestamp(lastBlockTime)
                        )
                    );
                } else {
                    sb.append(
                        String.format(
                            "%-20s: %s%n",
                            "Last Block Time",
                            "INVALID DATE"
                        )
                    );
                }
            } else {
                sb.append(
                    String.format("%-20s: %s%n", "Last Block Time", "N/A")
                );
            }

            return sb.toString();
        } catch (Exception e) {
            return "Error formatting blockchain state: " + e.getMessage();
        }
    }

    /**
     * Create a separator line of a specified length
     * @param length The length of the separator (must be non-negative and reasonable)
     * @return A string of equals signs
     * @throws IllegalArgumentException if length is negative or too large
     */
    public static String createSeparator(int length) {
        if (length < 0) {
            throw new IllegalArgumentException(
                "Length cannot be negative: " + length
            );
        }
        if (length > MAX_SEPARATOR_LENGTH) {
            throw new IllegalArgumentException(
                "Length too large (max " + MAX_SEPARATOR_LENGTH + "): " + length
            );
        }

        return "=".repeat(length);
    }
}
