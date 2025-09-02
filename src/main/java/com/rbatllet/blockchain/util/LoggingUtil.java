package com.rbatllet.blockchain.util;

import org.slf4j.Logger;

/**
 * Centralized logging utilities for consistent logging patterns across the application.
 * Provides common logging functions used throughout the blockchain system.
 */
public final class LoggingUtil {
    
    private LoggingUtil() {
        // Utility class - no instantiation
    }
    
    /**
     * Log a verbose message both to console (if verbose mode enabled) and to debug log
     * @param logger The logger instance to use
     * @param message The message to log
     * @param isVerbose Whether verbose mode is enabled
     */
    public static void verboseLog(Logger logger, String message, boolean isVerbose) {
        if (isVerbose) {
            System.out.println("🔍 " + message);
        }
        logger.debug("🔍 {}", message);
    }
    
    /**
     * Log a verbose message with global verbose check
     * @param logger The logger instance to use
     * @param message The message to log
     * @param localVerbose Local verbose flag
     * @param globalVerbose Global verbose flag
     */
    public static void verboseLog(Logger logger, String message, boolean localVerbose, boolean globalVerbose) {
        verboseLog(logger, message, localVerbose || globalVerbose);
    }
    
    /**
     * Log an operation start message
     * @param logger The logger instance to use
     * @param operation The operation being started
     * @param isVerbose Whether verbose mode is enabled
     */
    public static void logOperationStart(Logger logger, String operation, boolean isVerbose) {
        verboseLog(logger, "Starting " + operation + "...", isVerbose);
    }
    
    /**
     * Log an operation completion message
     * @param logger The logger instance to use
     * @param operation The operation that completed
     * @param isVerbose Whether verbose mode is enabled
     */
    public static void logOperationComplete(Logger logger, String operation, boolean isVerbose) {
        verboseLog(logger, operation + " completed successfully", isVerbose);
    }
    
    /**
     * Log an operation with timing information
     * @param logger The logger instance to use
     * @param operation The operation name
     * @param duration Duration in milliseconds
     * @param isVerbose Whether verbose mode is enabled
     */
    public static void logOperationTiming(Logger logger, String operation, long duration, boolean isVerbose) {
        verboseLog(logger, operation + " completed in " + duration + "ms", isVerbose);
    }
    
    /**
     * Log configuration changes
     * @param logger The logger instance to use
     * @param key Configuration key
     * @param value Configuration value
     * @param isVerbose Whether verbose mode is enabled
     */
    public static void logConfigChange(Logger logger, String key, String value, boolean isVerbose) {
        verboseLog(logger, "Setting configuration: " + key + " = " + value, isVerbose);
    }
    
    /**
     * Log search operations
     * @param logger The logger instance to use
     * @param searchType Type of search
     * @param query Search query
     * @param resultCount Number of results found
     * @param duration Search duration in milliseconds
     * @param isVerbose Whether verbose mode is enabled
     */
    public static void logSearchOperation(Logger logger, String searchType, String query, 
                                        int resultCount, long duration, boolean isVerbose) {
        verboseLog(logger, searchType + " search for '" + query + "' found " + 
                  resultCount + " results in " + duration + "ms", isVerbose);
    }
    
    /**
     * Log block operations
     * @param logger The logger instance to use
     * @param operation Operation type (create, encrypt, etc.)
     * @param blockInfo Block information
     * @param isVerbose Whether verbose mode is enabled
     */
    public static void logBlockOperation(Logger logger, String operation, String blockInfo, boolean isVerbose) {
        verboseLog(logger, operation + " block: " + blockInfo, isVerbose);
    }
    
    /**
     * Log data size information
     * @param logger The logger instance to use
     * @param operation Operation name
     * @param dataSize Size in bytes
     * @param isVerbose Whether verbose mode is enabled
     */
    public static void logDataSize(Logger logger, String operation, long dataSize, boolean isVerbose) {
        verboseLog(logger, operation + " with " + formatBytes(dataSize) + " of data", isVerbose);
    }
    
    /**
     * Format bytes for human-readable display
     * @param bytes Number of bytes
     * @return Formatted string
     */
    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " bytes";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
}