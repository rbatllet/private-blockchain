package com.rbatllet.blockchain.util;

/**
 * Utility class to handle System.exit() calls in a test-friendly way
 */
public class ExitUtil {
    
    private static boolean exitDisabled = false;
    private static int lastExitCode = 0;
    
    /**
     * Disable System.exit() calls (for testing)
     */
    public static void disableExit() {
        exitDisabled = true;
        lastExitCode = 0; // Reset exit code when entering test mode
    }
    
    /**
     * Enable System.exit() calls (normal operation)
     */
    public static void enableExit() {
        exitDisabled = false;
        lastExitCode = 0; // Reset exit code when leaving test mode
    }
    
    /**
     * Safe exit that can be disabled for testing
     * @param exitCode the exit code
     */
    public static void exit(int exitCode) {
        if (!exitDisabled) {
            System.exit(exitCode);
        } else {
            // In test mode, store the exit code instead of exiting
            lastExitCode = exitCode;
        }
    }
    
    /**
     * Check if exit is currently disabled
     * @return true if exit is disabled (test mode)
     */
    public static boolean isExitDisabled() {
        return exitDisabled;
    }
    
    /**
     * Get the last exit code that was attempted (only works in test mode)
     * @return the last exit code, or 0 if none
     */
    public static int getLastExitCode() {
        return lastExitCode;
    }
}
