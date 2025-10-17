package com.rbatllet.blockchain.config.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

/**
 * Utility class for managing POSIX file permissions.
 *
 * <p>Provides methods to check and set file permissions for security-sensitive files
 * such as configuration files containing passwords or private keys.</p>
 *
 * <p><b>Security Best Practice:</b> Configuration files containing sensitive data
 * should have permissions 600 (rw-------) to ensure only the owner can read/write.</p>
 *
 * <p><b>Platform Compatibility:</b> POSIX permissions are supported on Unix-like
 * systems (Linux, macOS, BSD). On Windows, these operations may have limited effect
 * or throw UnsupportedOperationException.</p>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * Path configFile = Paths.get("/path/to/database.properties");
 *
 * // Check permissions
 * PermissionStatus status = FilePermissionsUtil.checkPermissions(configFile);
 * if (!status.isSecure()) {
 *     System.err.println("Warning: " + status.getMessage());
 * }
 *
 * // Set secure permissions
 * FilePermissionsUtil.setSecurePermissions(configFile);
 * }</pre>
 *
 * @since 1.0.6
 * @see java.nio.file.attribute.PosixFilePermissions
 */
public final class FilePermissionsUtil {

    /**
     * Standard secure permissions for sensitive files: 600 (rw-------)
     * Owner can read and write, no permissions for group or others.
     */
    public static final String SECURE_PERMISSIONS = "rw-------";

    /**
     * POSIX permission set for secure files (600).
     */
    private static final Set<PosixFilePermission> SECURE_PERMISSION_SET =
        PosixFilePermissions.fromString(SECURE_PERMISSIONS);

    // Private constructor to prevent instantiation
    private FilePermissionsUtil() {
        throw new AssertionError("Utility class cannot be instantiated");
    }

    /**
     * Checks if a file has secure permissions (600 or more restrictive).
     *
     * <p>Secure permissions mean:</p>
     * <ul>
     *   <li>Owner can read and write</li>
     *   <li>Group has no permissions</li>
     *   <li>Others have no permissions</li>
     * </ul>
     *
     * @param path the file path to check
     * @return PermissionStatus indicating if permissions are secure
     * @throws IOException if file permissions cannot be read
     * @throws UnsupportedOperationException if POSIX permissions are not supported
     */
    public static PermissionStatus checkPermissions(Path path) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("Path cannot be null");
        }

        if (!Files.exists(path)) {
            return PermissionStatus.notFound(path);
        }

        try {
            Set<PosixFilePermission> perms = Files.getPosixFilePermissions(path);
            String permString = PosixFilePermissions.toString(perms);

            // Check if group or others have any permissions
            boolean groupHasPermissions = perms.contains(PosixFilePermission.GROUP_READ) ||
                                         perms.contains(PosixFilePermission.GROUP_WRITE) ||
                                         perms.contains(PosixFilePermission.GROUP_EXECUTE);

            boolean othersHavePermissions = perms.contains(PosixFilePermission.OTHERS_READ) ||
                                           perms.contains(PosixFilePermission.OTHERS_WRITE) ||
                                           perms.contains(PosixFilePermission.OTHERS_EXECUTE);

            if (groupHasPermissions || othersHavePermissions) {
                return PermissionStatus.insecure(path, permString,
                    "File has permissions for group or others. Recommended: " + SECURE_PERMISSIONS);
            }

            return PermissionStatus.secure(path, permString);

        } catch (UnsupportedOperationException e) {
            // POSIX permissions not supported (e.g., Windows)
            return PermissionStatus.unsupported(path);
        }
    }

    /**
     * Sets secure permissions (600) on a file.
     *
     * <p>After calling this method, only the file owner will be able to read and write.
     * Group and others will have no permissions.</p>
     *
     * @param path the file path to set permissions on
     * @throws IOException if permissions cannot be set
     * @throws UnsupportedOperationException if POSIX permissions are not supported
     */
    public static void setSecurePermissions(Path path) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("Path cannot be null");
        }

        if (!Files.exists(path)) {
            throw new IOException("File does not exist: " + path);
        }

        try {
            Files.setPosixFilePermissions(path, SECURE_PERMISSION_SET);
        } catch (UnsupportedOperationException e) {
            throw new UnsupportedOperationException("POSIX permissions not supported on this system", e);
        }
    }

    /**
     * Sets specific POSIX permissions on a file.
     *
     * @param path the file path to set permissions on
     * @param permissionString the permission string (e.g., "rw-r--r--", "rwx------")
     * @throws IOException if permissions cannot be set
     * @throws IllegalArgumentException if permission string is invalid
     * @throws UnsupportedOperationException if POSIX permissions are not supported
     */
    public static void setPermissions(Path path, String permissionString) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("Path cannot be null");
        }

        if (permissionString == null || permissionString.isEmpty()) {
            throw new IllegalArgumentException("Permission string cannot be null or empty");
        }

        if (!Files.exists(path)) {
            throw new IOException("File does not exist: " + path);
        }

        try {
            Set<PosixFilePermission> perms = PosixFilePermissions.fromString(permissionString);
            Files.setPosixFilePermissions(path, perms);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid permission string: " + permissionString, e);
        }
    }

    /**
     * Gets the current permissions of a file as a string.
     *
     * @param path the file path
     * @return the permission string (e.g., "rw-r--r--"), or null if not supported
     * @throws IOException if permissions cannot be read
     */
    public static String getPermissionsString(Path path) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("Path cannot be null");
        }

        if (!Files.exists(path)) {
            return null;
        }

        try {
            Set<PosixFilePermission> perms = Files.getPosixFilePermissions(path);
            return PosixFilePermissions.toString(perms);
        } catch (UnsupportedOperationException e) {
            return null;
        }
    }

    /**
     * Checks if the current system supports POSIX file permissions.
     *
     * @return true if POSIX permissions are supported, false otherwise
     */
    public static boolean isPosixSupported() {
        try {
            Path tempFile = Files.createTempFile("posix-test", ".tmp");
            try {
                Files.getPosixFilePermissions(tempFile);
                return true;
            } finally {
                Files.deleteIfExists(tempFile);
            }
        } catch (IOException | UnsupportedOperationException e) {
            return false;
        }
    }

    /**
     * Represents the result of a permission check.
     */
    public static final class PermissionStatus {
        private final boolean secure;
        private final boolean supported;
        private final boolean exists;
        private final String currentPermissions;
        private final String message;
        private final Path path;

        private PermissionStatus(Path path, boolean secure, boolean supported, boolean exists,
                                String currentPermissions, String message) {
            this.path = path;
            this.secure = secure;
            this.supported = supported;
            this.exists = exists;
            this.currentPermissions = currentPermissions;
            this.message = message;
        }

        /**
         * Creates a secure permission status.
         */
        static PermissionStatus secure(Path path, String permissions) {
            return new PermissionStatus(path, true, true, true, permissions,
                "File has secure permissions");
        }

        /**
         * Creates an insecure permission status.
         */
        static PermissionStatus insecure(Path path, String permissions, String message) {
            return new PermissionStatus(path, false, true, true, permissions, message);
        }

        /**
         * Creates a not-found status.
         */
        static PermissionStatus notFound(Path path) {
            return new PermissionStatus(path, false, true, false, null,
                "File does not exist");
        }

        /**
         * Creates an unsupported status (e.g., Windows).
         */
        static PermissionStatus unsupported(Path path) {
            return new PermissionStatus(path, true, false, true, null,
                "POSIX permissions not supported on this system");
        }

        /**
         * Returns true if the file has secure permissions.
         */
        public boolean isSecure() {
            return secure && supported && exists;
        }

        /**
         * Returns true if POSIX permissions are supported.
         */
        public boolean isSupported() {
            return supported;
        }

        /**
         * Returns true if the file exists.
         */
        public boolean exists() {
            return exists;
        }

        /**
         * Returns the current permission string, or null if not available.
         */
        public String getCurrentPermissions() {
            return currentPermissions;
        }

        /**
         * Returns a human-readable message about the permission status.
         */
        public String getMessage() {
            return message;
        }

        /**
         * Returns the file path.
         */
        public Path getPath() {
            return path;
        }

        @Override
        public String toString() {
            return "PermissionStatus{" +
                   "path=" + path +
                   ", secure=" + secure +
                   ", supported=" + supported +
                   ", exists=" + exists +
                   ", currentPermissions='" + currentPermissions + '\'' +
                   ", message='" + message + '\'' +
                   '}';
        }
    }
}
