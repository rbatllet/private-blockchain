package com.rbatllet.blockchain.util;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Utility class to handle System.exit() calls in a test-friendly way
 * FIXED: Complete thread-safety with proper synchronization
 */
public class ExitUtil {
    
    // FIXED: Thread-safe with volatile and synchronization
    private static volatile boolean exitDisabled = false;
    private static volatile int lastExitCode = 0;
    
    // FIXED: Lock for atomic operations
    private static final ReentrantLock EXIT_LOCK = new ReentrantLock();
    
    /**
     * Disable System.exit() calls (for testing)
     * FIXED: Thread-safe with lock
     */
    public static void disableExit() {
        EXIT_LOCK.lock();
        try {
            exitDisabled = true;
            lastExitCode = 0; // Reset exit code when entering test mode
        } finally {
            EXIT_LOCK.unlock();
        }
    }
    
    /**
     * Enable System.exit() calls (normal operation)
     * FIXED: Thread-safe with lock
     */
    public static void enableExit() {
        EXIT_LOCK.lock();
        try {
            exitDisabled = false;
            lastExitCode = 0; // Reset exit code when leaving test mode
        } finally {
            EXIT_LOCK.unlock();
        }
    }
    
    /**
     * Safe exit that can be disabled for testing
     * FIXED: Thread-safe with lock
     * @param exitCode the exit code
     */
    public static void exit(int exitCode) {
        EXIT_LOCK.lock();
        try {
            if (!exitDisabled) {
                // Release lock before calling System.exit() as it terminates the JVM
                EXIT_LOCK.unlock();
                System.exit(exitCode);
            } else {
                // In test mode, store the exit code instead of exiting
                lastExitCode = exitCode;
            }
        } finally {
            // Only unlock if we haven't called System.exit()
            if (exitDisabled && EXIT_LOCK.isHeldByCurrentThread()) {
                EXIT_LOCK.unlock();
            }
        }
    }
    
    /**
     * Check if exit is currently disabled
     * FIXED: Thread-safe read
     * @return true if exit is disabled (test mode)
     */
    public static boolean isExitDisabled() {
        return exitDisabled; // volatile ensures thread-safe read
    }
    
    /**
     * Get the last exit code that was attempted (only works in test mode)
     * FIXED: Thread-safe read
     * @return the last exit code, or 0 if none
     */
    public static int getLastExitCode() {
        return lastExitCode; // volatile ensures thread-safe read
    }
    
    /**
     * Reset exit code (useful for testing)
     * FIXED: Thread-safe with lock
     */
    public static void resetExitCode() {
        EXIT_LOCK.lock();
        try {
            lastExitCode = 0;
        } finally {
            EXIT_LOCK.unlock();
        }
    }
}