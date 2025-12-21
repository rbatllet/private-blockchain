package com.rbatllet.blockchain.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility class for secure file path validation and operations.
 *
 * <p>Provides methods to prevent path traversal attacks and ensure safe file operations
 * across the blockchain system.</p>
 *
 * <p><b>Security Features:</b></p>
 * <ul>
 *   <li>Path traversal attack detection (including URL-encoded variants)</li>
 *   <li>Path normalization and validation</li>
 *   <li>Directory containment verification</li>
 *   <li>Canonical path resolution</li>
 * </ul>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * // Validate user-provided file path
 * try {
 *     PathSecurityUtil.validateFilePath(userPath, "export");
 * } catch (SecurityException e) {
 *     System.err.println("Security violation: " + e.getMessage());
 * }
 *
 * // Verify file is within allowed directory
 * File baseDir = new File("off-chain-data");
 * File userFile = new File(userPath);
 * if (!PathSecurityUtil.isFileInDirectory(userFile, baseDir)) {
 *     throw new SecurityException("File outside allowed directory");
 * }
 * }</pre>
 *
 * @since 1.0.6
 * @see java.nio.file.Path
 */
public final class PathSecurityUtil {

    private static final Logger logger = LoggerFactory.getLogger(PathSecurityUtil.class);

    // Private constructor to prevent instantiation
    private PathSecurityUtil() {
        throw new AssertionError("Utility class cannot be instantiated");
    }

    /**
     * Validates a file path for security vulnerabilities.
     *
     * <p>This method performs comprehensive security checks including:</p>
     * <ul>
     *   <li>Null and empty path validation</li>
     *   <li>Path traversal detection (.. sequences)</li>
     *   <li>URL-encoded traversal detection (%2e%2e, %252e%252e)</li>
     *   <li>Path normalization verification</li>
     * </ul>
     *
     * <p><b>SECURITY:</b> Prevents attacks like:</p>
     * <pre>
     * ../../etc/passwd           (Unix path traversal)
     * ..\..\windows\system32     (Windows path traversal)
     * %2e%2e/etc/passwd          (URL-encoded traversal)
     * %252e%252e/etc/passwd      (Double URL-encoded traversal)
     * </pre>
     *
     * @param filePath The file path to validate
     * @param operation The operation being performed (for error messages)
     * @throws IllegalArgumentException if filePath is null or empty
     * @throws SecurityException if path traversal attempt detected
     * @since 1.0.6
     */
    public static void validateFilePath(String filePath, String operation) {
        // Fast validation checks first
        if (filePath == null || filePath.trim().isEmpty()) {
            String message = operation + " file path cannot be null or empty";
            logger.error("❌ {}", message);
            throw new IllegalArgumentException(message);
        }

        // SECURITY: Comprehensive path traversal prevention
        if (filePath.contains("..") || filePath.contains("%2e%2e") ||
            filePath.contains("%252e%252e") || filePath.contains("\\..") ||
            filePath.contains("/..")) {
            String message = "SECURITY: Path traversal attempt detected in " + operation + ": " + filePath;
            logger.error("❌ {}", message);
            throw new SecurityException(message);
        }

        // Additional security: normalize path and verify it doesn't escape
        try {
            Path normalizedPath = Paths.get(filePath).normalize();
            String normalizedString = normalizedPath.toString();

            // Double-check that normalization didn't reveal traversal
            if (normalizedString.contains("..")) {
                String message = "SECURITY: Path normalization revealed traversal in " + operation + ": " + filePath;
                logger.error("❌ {}", message);
                throw new SecurityException(message);
            }
        } catch (SecurityException e) {
            // Re-throw security exceptions
            throw e;
        } catch (Exception e) {
            String message = "Error validating " + operation + " path: " + e.getMessage();
            logger.error("❌ {}", message, e);
            throw new IllegalStateException(message, e);
        }
    }

    /**
     * Validates a file path without operation context.
     *
     * @param filePath The file path to validate
     * @throws IllegalArgumentException if filePath is null or empty
     * @throws SecurityException if path traversal attempt detected
     * @since 1.0.6
     */
    public static void validateFilePath(String filePath) {
        validateFilePath(filePath, "operation");
    }

    /**
     * Verifies that a file is contained within a base directory.
     *
     * <p>Uses canonical path resolution to prevent symbolic link attacks
     * and ensure the file truly resides within the allowed directory.</p>
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * File baseDir = new File("off-chain-data");
     * File userFile = new File("off-chain-data/../../etc/passwd");
     *
     * // Returns false - file escapes base directory
     * boolean safe = PathSecurityUtil.isFileInDirectory(userFile, baseDir);
     * }</pre>
     *
     * @param file The file to check
     * @param baseDir The allowed base directory
     * @return true if file is within baseDir, false otherwise
     * @since 1.0.6
     */
    public static boolean isFileInDirectory(File file, File baseDir) {
        if (file == null || baseDir == null) {
            logger.error("❌ File and baseDir cannot be null");
            return false;
        }

        try {
            String basePath = baseDir.getCanonicalPath();
            String filePath = file.getCanonicalPath();

            return filePath.startsWith(basePath + File.separator) ||
                   filePath.equals(basePath);
        } catch (IOException e) {
            logger.error("Error validating file path: {}", file.getAbsolutePath(), e);
            return false;
        }
    }

    /**
     * Verifies that a file path is contained within a base directory path.
     *
     * @param filePath The file path to check
     * @param baseDirPath The allowed base directory path
     * @return true if file is within base directory, false otherwise
     * @since 1.0.6
     */
    public static boolean isFileInDirectory(String filePath, String baseDirPath) {
        if (filePath == null || baseDirPath == null) {
            logger.error("❌ File path and base directory path cannot be null");
            return false;
        }

        return isFileInDirectory(new File(filePath), new File(baseDirPath));
    }

    /**
     * Validates and normalizes a file path.
     *
     * <p>Returns the normalized, absolute path after security validation.</p>
     *
     * @param filePath The file path to validate and normalize
     * @param operation The operation being performed
     * @return The normalized absolute path
     * @throws IllegalArgumentException if filePath is null or empty
     * @throws SecurityException if path traversal attempt detected
     * @since 1.0.6
     */
    public static String validateAndNormalizePath(String filePath, String operation) {
        validateFilePath(filePath, operation);

        try {
            Path normalizedPath = Paths.get(filePath).normalize().toAbsolutePath();
            return normalizedPath.toString();
        } catch (Exception e) {
            String message = "Error normalizing " + operation + " path: " + e.getMessage();
            logger.error("❌ {}", message, e);
            throw new IllegalStateException(message, e);
        }
    }

    /**
     * Checks if a path contains traversal sequences.
     *
     * <p>This is a quick check without normalization, useful for early validation.</p>
     *
     * @param filePath The file path to check
     * @return true if path contains traversal sequences, false otherwise
     * @since 1.0.6
     */
    public static boolean containsTraversalSequence(String filePath) {
        if (filePath == null) {
            return false;
        }

        return filePath.contains("..") ||
               filePath.contains("%2e%2e") ||
               filePath.contains("%252e%252e") ||
               filePath.contains("\\..") ||
               filePath.contains("/..");
    }

    /**
     * Validates file extension matches expected extension.
     *
     * @param filePath The file path to validate
     * @param expectedExtension The expected extension (e.g., ".json")
     * @param operation The operation being performed
     * @throws IllegalArgumentException if extension doesn't match
     * @since 1.0.6
     */
    public static void validateFileExtension(String filePath, String expectedExtension, String operation) {
        if (filePath == null || expectedExtension == null) {
            throw new IllegalArgumentException("File path and expected extension cannot be null");
        }

        if (!filePath.endsWith(expectedExtension)) {
            String message = "SECURITY: " + operation + " only allowed for " + expectedExtension + " files: " + filePath;
            logger.error("❌ {}", message);
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Gets the canonical path of a file safely.
     *
     * <p>Returns null if canonical path cannot be determined.</p>
     *
     * @param file The file to get canonical path for
     * @return The canonical path, or null if not available
     * @since 1.0.6
     */
    public static String getCanonicalPathSafely(File file) {
        if (file == null) {
            return null;
        }

        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            logger.warn("Cannot get canonical path for: {}", file.getAbsolutePath(), e);
            return null;
        }
    }
}
