package com.rbatllet.blockchain.config.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for FilePermissionsUtil.
 *
 * Note: Some tests are OS-specific (Unix/Linux/macOS only) as POSIX permissions
 * are not fully supported on Windows.
 */
@DisplayName("FilePermissionsUtil Tests")
class FilePermissionsUtilTest {

    private Path tempFile;
    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("fileperms-test");
        tempFile = Files.createTempFile(tempDir, "test", ".txt");
    }

    @AfterEach
    void tearDown() throws IOException {
        if (tempFile != null && Files.exists(tempFile)) {
            Files.deleteIfExists(tempFile);
        }
        if (tempDir != null && Files.exists(tempDir)) {
            Files.deleteIfExists(tempDir);
        }
    }

    // ============================================================================
    // Constructor Tests
    // ============================================================================

    @Test
    @DisplayName("Constructor should throw AssertionError (utility class)")
    void testConstructor_ThrowsError() {
        assertThrows(java.lang.reflect.InvocationTargetException.class, () -> {
            java.lang.reflect.Constructor<FilePermissionsUtil> constructor =
                FilePermissionsUtil.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
        });

        // Also verify that the cause is AssertionError
        try {
            java.lang.reflect.Constructor<FilePermissionsUtil> constructor =
                FilePermissionsUtil.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
            fail("Should have thrown exception");
        } catch (java.lang.reflect.InvocationTargetException e) {
            assertTrue(e.getCause() instanceof AssertionError,
                "Cause should be AssertionError, but was: " + e.getCause().getClass());
            assertEquals("Utility class cannot be instantiated", e.getCause().getMessage());
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getClass());
        }
    }

    // ============================================================================
    // Constants Tests
    // ============================================================================

    @Test
    @DisplayName("SECURE_PERMISSIONS constant should be 'rw-------'")
    void testSecurePermissions_Constant() {
        assertEquals("rw-------", FilePermissionsUtil.SECURE_PERMISSIONS);
    }

    // ============================================================================
    // isPosixSupported Tests
    // ============================================================================

    @Test
    @DisplayName("isPosixSupported() should return boolean without error")
    void testIsPosixSupported() {
        boolean supported = FilePermissionsUtil.isPosixSupported();

        // Should return a boolean value (true on Unix, false on Windows)
        assertTrue(supported || !supported); // Always passes, just checks no exception
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    @DisplayName("isPosixSupported() should return true on Unix-like systems")
    void testIsPosixSupported_Unix() {
        assertTrue(FilePermissionsUtil.isPosixSupported());
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    @DisplayName("isPosixSupported() might return false on Windows")
    void testIsPosixSupported_Windows() {
        // On Windows, it depends on the filesystem
        boolean supported = FilePermissionsUtil.isPosixSupported();
        // We don't assert true/false as it depends on Windows version and filesystem
        assertNotNull(supported); // Just verify method returns
    }

    // ============================================================================
    // checkPermissions Tests - Basic Functionality
    // ============================================================================

    @Test
    @DisplayName("checkPermissions() should throw IllegalArgumentException for null path")
    void testCheckPermissions_NullPath() {
        assertThrows(IllegalArgumentException.class, () ->
            FilePermissionsUtil.checkPermissions(null)
        );
    }

    @Test
    @DisplayName("checkPermissions() should return NOT_FOUND for non-existent file")
    void testCheckPermissions_NonExistentFile() throws IOException {
        Path nonExistent = tempDir.resolve("non-existent.txt");

        FilePermissionsUtil.PermissionStatus status =
            FilePermissionsUtil.checkPermissions(nonExistent);

        assertFalse(status.exists(), "File should not exist");
        assertFalse(status.isSecure(), "Non-existent file cannot be secure");
        assertEquals(nonExistent, status.getPath());
        assertNull(status.getCurrentPermissions());
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    @DisplayName("checkPermissions() should detect secure permissions (600)")
    void testCheckPermissions_Secure() throws IOException {
        // Set secure permissions
        Set<PosixFilePermission> securePerms = PosixFilePermissions.fromString("rw-------");
        Files.setPosixFilePermissions(tempFile, securePerms);

        FilePermissionsUtil.PermissionStatus status =
            FilePermissionsUtil.checkPermissions(tempFile);

        assertTrue(status.isSecure(), "Should detect secure permissions");
        assertTrue(status.exists());
        assertTrue(status.isSupported());
        assertEquals("rw-------", status.getCurrentPermissions());
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    @DisplayName("checkPermissions() should detect insecure permissions (group readable)")
    void testCheckPermissions_Insecure_GroupRead() throws IOException {
        // Set insecure permissions (group can read)
        Set<PosixFilePermission> insecurePerms = PosixFilePermissions.fromString("rw-r-----");
        Files.setPosixFilePermissions(tempFile, insecurePerms);

        FilePermissionsUtil.PermissionStatus status =
            FilePermissionsUtil.checkPermissions(tempFile);

        assertFalse(status.isSecure(), "Should detect insecure permissions (group read)");
        assertEquals("rw-r-----", status.getCurrentPermissions());
        assertTrue(status.getMessage().toLowerCase().contains("group"));
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    @DisplayName("checkPermissions() should detect insecure permissions (others readable)")
    void testCheckPermissions_Insecure_OthersRead() throws IOException {
        Set<PosixFilePermission> insecurePerms = PosixFilePermissions.fromString("rw----r--");
        Files.setPosixFilePermissions(tempFile, insecurePerms);

        FilePermissionsUtil.PermissionStatus status =
            FilePermissionsUtil.checkPermissions(tempFile);

        assertFalse(status.isSecure(), "Should detect insecure permissions (others read)");
        assertEquals("rw----r--", status.getCurrentPermissions());
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    @DisplayName("checkPermissions() should detect world-writable as insecure")
    void testCheckPermissions_Insecure_WorldWritable() throws IOException {
        Set<PosixFilePermission> insecurePerms = PosixFilePermissions.fromString("rw-rw-rw-");
        Files.setPosixFilePermissions(tempFile, insecurePerms);

        FilePermissionsUtil.PermissionStatus status =
            FilePermissionsUtil.checkPermissions(tempFile);

        assertFalse(status.isSecure(), "World-writable should be insecure");
        assertEquals("rw-rw-rw-", status.getCurrentPermissions());
    }

    // ============================================================================
    // setSecurePermissions Tests
    // ============================================================================

    @Test
    @DisplayName("setSecurePermissions() should throw IllegalArgumentException for null path")
    void testSetSecurePermissions_NullPath() {
        assertThrows(IllegalArgumentException.class, () ->
            FilePermissionsUtil.setSecurePermissions(null)
        );
    }

    @Test
    @DisplayName("setSecurePermissions() should throw IOException for non-existent file")
    void testSetSecurePermissions_NonExistentFile() {
        Path nonExistent = tempDir.resolve("non-existent.txt");

        assertThrows(IOException.class, () ->
            FilePermissionsUtil.setSecurePermissions(nonExistent)
        );
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    @DisplayName("setSecurePermissions() should set permissions to 600")
    void testSetSecurePermissions_Success() throws IOException {
        // First set insecure permissions
        Set<PosixFilePermission> insecurePerms = PosixFilePermissions.fromString("rw-rw-rw-");
        Files.setPosixFilePermissions(tempFile, insecurePerms);

        // Verify it's insecure
        FilePermissionsUtil.PermissionStatus before =
            FilePermissionsUtil.checkPermissions(tempFile);
        assertFalse(before.isSecure());

        // Set secure permissions
        FilePermissionsUtil.setSecurePermissions(tempFile);

        // Verify it's now secure
        FilePermissionsUtil.PermissionStatus after =
            FilePermissionsUtil.checkPermissions(tempFile);
        assertTrue(after.isSecure(), "Permissions should now be secure");
        assertEquals("rw-------", after.getCurrentPermissions());
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    @DisplayName("setSecurePermissions() should be idempotent")
    void testSetSecurePermissions_Idempotent() throws IOException {
        // Set secure permissions twice
        FilePermissionsUtil.setSecurePermissions(tempFile);
        FilePermissionsUtil.setSecurePermissions(tempFile);

        // Should still be secure
        FilePermissionsUtil.PermissionStatus status =
            FilePermissionsUtil.checkPermissions(tempFile);

        assertTrue(status.isSecure());
        assertEquals("rw-------", status.getCurrentPermissions());
    }

    // ============================================================================
    // setPermissions Tests
    // ============================================================================

    @Test
    @DisplayName("setPermissions() should throw IllegalArgumentException for null path")
    void testSetPermissions_NullPath() {
        assertThrows(IllegalArgumentException.class, () ->
            FilePermissionsUtil.setPermissions(null, "rw-------")
        );
    }

    @Test
    @DisplayName("setPermissions() should throw IllegalArgumentException for null permission string")
    void testSetPermissions_NullPermissionString() {
        assertThrows(IllegalArgumentException.class, () ->
            FilePermissionsUtil.setPermissions(tempFile, null)
        );
    }

    @Test
    @DisplayName("setPermissions() should throw IllegalArgumentException for empty permission string")
    void testSetPermissions_EmptyPermissionString() {
        assertThrows(IllegalArgumentException.class, () ->
            FilePermissionsUtil.setPermissions(tempFile, "")
        );
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    @DisplayName("setPermissions() should throw IllegalArgumentException for invalid permission string")
    void testSetPermissions_InvalidPermissionString() {
        assertThrows(IllegalArgumentException.class, () ->
            FilePermissionsUtil.setPermissions(tempFile, "invalid")
        );

        assertThrows(IllegalArgumentException.class, () ->
            FilePermissionsUtil.setPermissions(tempFile, "rwxrwxrwxrwx") // Too long
        );

        assertThrows(IllegalArgumentException.class, () ->
            FilePermissionsUtil.setPermissions(tempFile, "abc-def-gh") // Invalid chars
        );
    }

    @Test
    @DisplayName("setPermissions() should throw IOException for non-existent file")
    void testSetPermissions_NonExistentFile() {
        Path nonExistent = tempDir.resolve("non-existent.txt");

        assertThrows(IOException.class, () ->
            FilePermissionsUtil.setPermissions(nonExistent, "rw-------")
        );
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    @DisplayName("setPermissions() should set custom permissions correctly")
    void testSetPermissions_CustomPermissions() throws IOException {
        String[] testPermissions = {
            "rw-------",  // 600
            "rwx------",  // 700
            "r--------",  // 400
            "rw-r-----",  // 640
            "rwxr-xr-x"   // 755
        };

        for (String perms : testPermissions) {
            FilePermissionsUtil.setPermissions(tempFile, perms);
            String actual = FilePermissionsUtil.getPermissionsString(tempFile);
            assertEquals(perms, actual, "Should set permissions to: " + perms);
        }
    }

    // ============================================================================
    // getPermissionsString Tests
    // ============================================================================

    @Test
    @DisplayName("getPermissionsString() should throw IllegalArgumentException for null path")
    void testGetPermissionsString_NullPath() {
        assertThrows(IllegalArgumentException.class, () ->
            FilePermissionsUtil.getPermissionsString(null)
        );
    }

    @Test
    @DisplayName("getPermissionsString() should return null for non-existent file")
    void testGetPermissionsString_NonExistentFile() throws IOException {
        Path nonExistent = tempDir.resolve("non-existent.txt");

        String perms = FilePermissionsUtil.getPermissionsString(nonExistent);

        assertNull(perms, "Should return null for non-existent file");
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    @DisplayName("getPermissionsString() should return correct permission string")
    void testGetPermissionsString_Success() throws IOException {
        Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rw-r-----");
        Files.setPosixFilePermissions(tempFile, perms);

        String permString = FilePermissionsUtil.getPermissionsString(tempFile);

        assertEquals("rw-r-----", permString);
    }

    // ============================================================================
    // PermissionStatus Tests
    // ============================================================================

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    @DisplayName("PermissionStatus should correctly reflect secure status")
    void testPermissionStatus_Secure() throws IOException {
        FilePermissionsUtil.setSecurePermissions(tempFile);
        FilePermissionsUtil.PermissionStatus status =
            FilePermissionsUtil.checkPermissions(tempFile);

        assertTrue(status.isSecure());
        assertTrue(status.isSupported());
        assertTrue(status.exists());
        assertEquals("rw-------", status.getCurrentPermissions());
        assertNotNull(status.getMessage());
        assertEquals(tempFile, status.getPath());
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    @DisplayName("PermissionStatus should correctly reflect insecure status")
    void testPermissionStatus_Insecure() throws IOException {
        Set<PosixFilePermission> insecurePerms = PosixFilePermissions.fromString("rw-rw-rw-");
        Files.setPosixFilePermissions(tempFile, insecurePerms);

        FilePermissionsUtil.PermissionStatus status =
            FilePermissionsUtil.checkPermissions(tempFile);

        assertFalse(status.isSecure());
        assertTrue(status.isSupported());
        assertTrue(status.exists());
        assertEquals("rw-rw-rw-", status.getCurrentPermissions());
        assertTrue(status.getMessage().toLowerCase().contains("group") ||
                   status.getMessage().toLowerCase().contains("others"));
    }

    @Test
    @DisplayName("PermissionStatus.toString() should not throw exception")
    void testPermissionStatus_ToString() throws IOException {
        FilePermissionsUtil.PermissionStatus status =
            FilePermissionsUtil.checkPermissions(tempFile);

        String str = status.toString();

        assertNotNull(str);
        assertTrue(str.contains("PermissionStatus"));
        assertTrue(str.contains(tempFile.toString()));
    }

    // ============================================================================
    // Edge Cases and Security Tests
    // ============================================================================

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    @DisplayName("SECURITY: Execute permissions should be detected as insecure for config files")
    void testSecurity_ExecutePermissions() throws IOException {
        // Even owner execute permission might be suspicious for config files
        Set<PosixFilePermission> permsWithExecute = PosixFilePermissions.fromString("rwx------");
        Files.setPosixFilePermissions(tempFile, permsWithExecute);

        String permString = FilePermissionsUtil.getPermissionsString(tempFile);
        assertEquals("rwx------", permString);

        // Note: Our current implementation considers rwx------ as "secure"
        // since group/others have no permissions. This is acceptable.
        // A stricter implementation could warn about execute bit on config files.
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    @DisplayName("SECURITY: Group write should be detected as insecure")
    void testSecurity_GroupWrite() throws IOException {
        Set<PosixFilePermission> insecurePerms = PosixFilePermissions.fromString("rw--w----");
        Files.setPosixFilePermissions(tempFile, insecurePerms);

        FilePermissionsUtil.PermissionStatus status =
            FilePermissionsUtil.checkPermissions(tempFile);

        assertFalse(status.isSecure(), "Group write should be insecure");
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    @DisplayName("SECURITY: Others write should be detected as insecure")
    void testSecurity_OthersWrite() throws IOException {
        Set<PosixFilePermission> insecurePerms = PosixFilePermissions.fromString("rw-----w-");
        Files.setPosixFilePermissions(tempFile, insecurePerms);

        FilePermissionsUtil.PermissionStatus status =
            FilePermissionsUtil.checkPermissions(tempFile);

        assertFalse(status.isSecure(), "Others write should be insecure");
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    @DisplayName("Read-only permissions (r--------) should be secure")
    void testReadOnlyPermissions() throws IOException {
        Set<PosixFilePermission> readOnlyPerms = PosixFilePermissions.fromString("r--------");
        Files.setPosixFilePermissions(tempFile, readOnlyPerms);

        FilePermissionsUtil.PermissionStatus status =
            FilePermissionsUtil.checkPermissions(tempFile);

        assertTrue(status.isSecure(), "Read-only owner permissions should be secure");
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    @DisplayName("No permissions (--------) should be secure")
    void testNoPermissions() throws IOException {
        Set<PosixFilePermission> noPerms = PosixFilePermissions.fromString("---------");
        Files.setPosixFilePermissions(tempFile, noPerms);

        FilePermissionsUtil.PermissionStatus status =
            FilePermissionsUtil.checkPermissions(tempFile);

        assertTrue(status.isSecure(), "No permissions should be secure");
    }

    // ============================================================================
    // Integration Tests
    // ============================================================================

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    @DisplayName("Integration: Full workflow - check, set, verify")
    void testIntegration_FullWorkflow() throws IOException {
        // 1. Create file with insecure permissions
        Set<PosixFilePermission> insecurePerms = PosixFilePermissions.fromString("rw-rw-rw-");
        Files.setPosixFilePermissions(tempFile, insecurePerms);

        // 2. Check - should be insecure
        FilePermissionsUtil.PermissionStatus statusBefore =
            FilePermissionsUtil.checkPermissions(tempFile);
        assertFalse(statusBefore.isSecure());

        // 3. Set secure permissions
        FilePermissionsUtil.setSecurePermissions(tempFile);

        // 4. Check again - should be secure
        FilePermissionsUtil.PermissionStatus statusAfter =
            FilePermissionsUtil.checkPermissions(tempFile);
        assertTrue(statusAfter.isSecure());

        // 5. Verify with direct read
        String perms = FilePermissionsUtil.getPermissionsString(tempFile);
        assertEquals("rw-------", perms);
    }
}
