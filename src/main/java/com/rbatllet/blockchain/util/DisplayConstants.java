package com.rbatllet.blockchain.util;

/**
 * Centralized display constants for consistent UI across the blockchain system
 * Contains emojis, icons, and formatting symbols used throughout the application
 */
public final class DisplayConstants {
    
    private DisplayConstants() {
        // Utility class - no instantiation
    }
    
    // Status icons
    public static final String SUCCESS = "✅";
    public static final String ERROR = "❌";
    public static final String WARNING = "⚠️";
    public static final String INFO = "ℹ️";
    
    // Data and content icons
    public static final String STATS = "📊";
    public static final String LIST = "📋";
    public static final String DATA = "📝";
    public static final String SEARCH = "🔍";
    public static final String VALIDATION = "🔍";
    public static final String CLEANUP = "🧹";
    
    // Security and crypto icons
    public static final String KEY = "🔑";
    public static final String BLOCKCHAIN = "🔗";
    public static final String SECURITY = "🔐";
    public static final String BLOCK = "📦";
    
    // Time and calendar icons
    public static final String DATE = "📅";
    public static final String TIMESTAMP = "📅";
    
    // File and storage icons
    public static final String FILE = "📁";
    public static final String STORAGE = "💾";
    public static final String DATABASE = "🗄️";
    public static final String EXPORT = "📤";
    public static final String IMPORT = "📥";
    
    // System and performance icons
    public static final String MEMORY = "💾";
    public static final String CPU = "⚡";
    public static final String PERFORMANCE = "⚡";
    public static final String HEALTH = "🏥";
    public static final String CONFIG = "⚙️";
    
    // User and identity icons
    public static final String USER = "👤";
    public static final String ADMIN = "👑";
    public static final String GROUP = "👥";
    
    // Network and communication icons
    public static final String NETWORK = "🌐";
    public static final String API = "🔌";
    public static final String SERVICE = "🔧";
    
    // Process and operation icons
    public static final String LOADING = "🔄";
    public static final String PROCESS = "⚙️";
    public static final String TASK = "📋";
    public static final String QUEUE = "📑";
    
    // Notification and alert icons
    public static final String NOTIFICATION = "🔔";
    public static final String ALERT = "🚨";
    public static final String MESSAGE = "💬";
    
    // Development and debug icons
    public static final String DEBUG = "🐛";
    public static final String TEST = "🧪";
    public static final String LOG = "📄";
    
    // Common separators and formatting
    public static final String SEPARATOR_LIGHT = "─";
    public static final String SEPARATOR_HEAVY = "═";
    public static final String BULLET = "•";
    public static final String ARROW_RIGHT = "→";
    public static final String ARROW_DOWN = "↓";
    
    // Color indicators (for terminals that support color)
    public static final String GREEN_DOT = "🟢";
    public static final String RED_DOT = "🔴";
    public static final String YELLOW_DOT = "🟡";
    public static final String BLUE_DOT = "🔵";
    
    // Special characters for boxes and borders
    public static final String BOX_TOP_LEFT = "┌";
    public static final String BOX_TOP_RIGHT = "┐";
    public static final String BOX_BOTTOM_LEFT = "└";
    public static final String BOX_BOTTOM_RIGHT = "┘";
    public static final String BOX_HORIZONTAL = "─";
    public static final String BOX_VERTICAL = "│";
    
    /**
     * Create a separator line with specified character and length
     * @param character Character to use for separator
     * @param length Length of the separator
     * @return Separator string
     */
    public static String separator(String character, int length) {
        return character.repeat(Math.max(0, length));
    }
    
    /**
     * Create a light separator line of specified length
     * @param length Length of the separator
     * @return Light separator string
     */
    public static String lightSeparator(int length) {
        return separator(SEPARATOR_LIGHT, length);
    }
    
    /**
     * Create a heavy separator line of specified length
     * @param length Length of the separator
     * @return Heavy separator string
     */
    public static String heavySeparator(int length) {
        return separator(SEPARATOR_HEAVY, length);
    }
    
    /**
     * Format a status message with appropriate icon
     * @param success Whether the operation was successful
     * @param message The message to display
     * @return Formatted status message
     */
    public static String statusMessage(boolean success, String message) {
        return (success ? SUCCESS : ERROR) + " " + message;
    }
    
    /**
     * Format a warning message
     * @param message The warning message
     * @return Formatted warning message
     */
    public static String warningMessage(String message) {
        return WARNING + " " + message;
    }
    
    /**
     * Format an info message
     * @param message The info message
     * @return Formatted info message
     */
    public static String infoMessage(String message) {
        return INFO + " " + message;
    }
    
    /**
     * Create a titled section with separator
     * @param title The section title
     * @param separatorLength Length of the separator line
     * @return Formatted section header
     */
    public static String sectionHeader(String title, int separatorLength) {
        return title + "\n" + heavySeparator(separatorLength);
    }
    
    /**
     * Create a subsection with light separator
     * @param title The subsection title
     * @param separatorLength Length of the separator line
     * @return Formatted subsection header
     */
    public static String subsectionHeader(String title, int separatorLength) {
        return title + "\n" + lightSeparator(separatorLength);
    }
}