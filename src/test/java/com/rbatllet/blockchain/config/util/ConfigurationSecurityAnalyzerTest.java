package com.rbatllet.blockchain.config.util;

import com.rbatllet.blockchain.config.DatabaseConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for ConfigurationSecurityAnalyzer.
 */
@DisplayName("ConfigurationSecurityAnalyzer Tests")
class ConfigurationSecurityAnalyzerTest {

    private ConfigurationSecurityAnalyzer analyzer;
    private Path tempConfigFile;
    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        analyzer = new ConfigurationSecurityAnalyzer();
        tempDir = Files.createTempDirectory("security-analyzer-test");
        tempConfigFile = Files.createTempFile(tempDir, "config", ".properties");
    }

    @AfterEach
    void tearDown() throws IOException {
        if (tempConfigFile != null && Files.exists(tempConfigFile)) {
            Files.deleteIfExists(tempConfigFile);
        }
        if (tempDir != null && Files.exists(tempDir)) {
            Files.deleteIfExists(tempDir);
        }
    }

    // ============================================================================
    // Null Configuration Tests
    // ============================================================================

    @Test
    @DisplayName("analyze() should return CRITICAL warning for null config")
    void testAnalyze_NullConfig() {
        List<SecurityWarning> warnings = analyzer.analyze(null);

        assertEquals(1, warnings.size());
        assertEquals(SecurityWarning.Severity.CRITICAL, warnings.get(0).getSeverity());
        assertTrue(warnings.get(0).getMessage().toLowerCase().contains("null"));
    }

    // ============================================================================
    // SQLite Configuration Tests (no password needed)
    // ============================================================================

    @Test
    @DisplayName("analyze() should return no warnings for SQLite (no password)")
    void testAnalyze_SQLite_NoWarnings() {
        DatabaseConfig config = DatabaseConfig.createSQLiteConfig();

        List<SecurityWarning> warnings = analyzer.analyze(config);

        // SQLite doesn't need passwords, should be clean
        assertTrue(warnings.isEmpty() || warnings.stream()
            .noneMatch(w -> w.getSeverity() == SecurityWarning.Severity.CRITICAL ||
                           w.getSeverity() == SecurityWarning.Severity.HIGH));
    }

    // ============================================================================
    // PostgreSQL Password Storage Tests
    // ============================================================================

    @Test
    @DisplayName("analyze() should warn HIGH for password in FILE source")
    void testAnalyze_PostgreSQL_PasswordInFile() {
        DatabaseConfig config = DatabaseConfig.createPostgreSQLConfig(
            "localhost", 5432, "blockchain", "admin", "password123"
        );

        analyzer.withConfigSource(ConfigurationSecurityAnalyzer.ConfigSource.FILE);

        List<SecurityWarning> warnings = analyzer.analyze(config);

        // Should have HIGH severity warning about password in file
        assertTrue(warnings.stream().anyMatch(w ->
            w.getSeverity() == SecurityWarning.Severity.HIGH &&
            w.getMessage().toLowerCase().contains("password") &&
            w.getMessage().toLowerCase().contains("file")
        ), "Should warn about password in file");

        // Should have remediation steps
        SecurityWarning passwordWarning = warnings.stream()
            .filter(w -> w.getCode() != null && w.getCode().equals("PASSWORD_IN_FILE"))
            .findFirst()
            .orElse(null);

        assertNotNull(passwordWarning, "Should have PASSWORD_IN_FILE warning");
        assertFalse(passwordWarning.getRemediationSteps().isEmpty(),
            "Should provide remediation steps");
    }

    @Test
    @DisplayName("analyze() should warn CRITICAL for password in CLI_ARGS")
    void testAnalyze_PostgreSQL_PasswordInCliArgs() {
        DatabaseConfig config = DatabaseConfig.createPostgreSQLConfig(
            "localhost", 5432, "blockchain", "admin", "password123"
        );

        analyzer.withConfigSource(ConfigurationSecurityAnalyzer.ConfigSource.CLI_ARGS);

        List<SecurityWarning> warnings = analyzer.analyze(config);

        // Should have CRITICAL severity warning
        assertTrue(warnings.stream().anyMatch(w ->
            w.getSeverity() == SecurityWarning.Severity.CRITICAL &&
            w.getMessage().toLowerCase().contains("password") &&
            w.getMessage().toLowerCase().contains("command")
        ), "Should have CRITICAL warning for password in CLI args");

        // Should mention process list visibility
        SecurityWarning cliWarning = warnings.stream()
            .filter(w -> w.getCode() != null && w.getCode().equals("PASSWORD_IN_CLI_ARGS"))
            .findFirst()
            .orElse(null);

        assertNotNull(cliWarning);
        assertTrue(cliWarning.getRemediationSteps().stream()
            .anyMatch(step -> step.toLowerCase().contains("process") ||
                             step.toLowerCase().contains("history")));
    }

    @Test
    @DisplayName("analyze() should provide INFO for password from ENVIRONMENT")
    void testAnalyze_PostgreSQL_PasswordFromEnvironment() {
        DatabaseConfig config = DatabaseConfig.createPostgreSQLConfig(
            "localhost", 5432, "blockchain", "admin", "password123"
        );

        analyzer.withConfigSource(ConfigurationSecurityAnalyzer.ConfigSource.ENVIRONMENT);

        List<SecurityWarning> warnings = analyzer.analyze(config);

        // Should not have HIGH or CRITICAL warnings about password storage
        assertFalse(warnings.stream().anyMatch(w ->
            (w.getSeverity() == SecurityWarning.Severity.HIGH ||
             w.getSeverity() == SecurityWarning.Severity.CRITICAL) &&
            w.getMessage().toLowerCase().contains("password")
        ), "Environment variables should not trigger password warnings");
    }

    // ============================================================================
    // File Permissions Tests
    // ============================================================================

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    @DisplayName("analyze() should warn HIGH for insecure file permissions")
    void testAnalyze_InsecureFilePermissions() throws IOException {
        // Set insecure permissions
        Files.setPosixFilePermissions(tempConfigFile,
            PosixFilePermissions.fromString("rw-rw-rw-"));

        DatabaseConfig config = DatabaseConfig.createPostgreSQLConfig(
            "localhost", 5432, "blockchain", "admin", "password123"
        );

        analyzer.withConfigFile(tempConfigFile)
               .withConfigSource(ConfigurationSecurityAnalyzer.ConfigSource.FILE);

        List<SecurityWarning> warnings = analyzer.analyze(config);

        // Should warn about insecure file permissions
        assertTrue(warnings.stream().anyMatch(w ->
            w.getSeverity() == SecurityWarning.Severity.HIGH &&
            w.getCode() != null &&
            w.getCode().equals("INSECURE_FILE_PERMISSIONS")
        ), "Should warn about insecure file permissions");
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    @DisplayName("analyze() should NOT warn for secure file permissions")
    void testAnalyze_SecureFilePermissions() throws IOException {
        // Set secure permissions
        Files.setPosixFilePermissions(tempConfigFile,
            PosixFilePermissions.fromString("rw-------"));

        DatabaseConfig config = DatabaseConfig.createPostgreSQLConfig(
            "localhost", 5432, "blockchain", "admin", "password123"
        );

        analyzer.withConfigFile(tempConfigFile)
               .withConfigSource(ConfigurationSecurityAnalyzer.ConfigSource.FILE);

        List<SecurityWarning> warnings = analyzer.analyze(config);

        // Should NOT warn about file permissions
        assertFalse(warnings.stream().anyMatch(w ->
            w.getCode() != null &&
            w.getCode().equals("INSECURE_FILE_PERMISSIONS")
        ), "Should not warn about file permissions when secure");
    }

    @Test
    @DisplayName("analyze() should handle non-existent config file gracefully")
    void testAnalyze_NonExistentConfigFile() throws IOException {
        Path nonExistent = tempDir.resolve("non-existent.properties");

        DatabaseConfig config = DatabaseConfig.createPostgreSQLConfig(
            "localhost", 5432, "blockchain", "admin", "password123"
        );

        analyzer.withConfigFile(nonExistent);

        List<SecurityWarning> warnings = analyzer.analyze(config);

        // Should not crash, and should not warn about non-existent file
        assertNotNull(warnings);
    }

    // ============================================================================
    // Connection Security Tests
    // ============================================================================

    @Test
    @DisplayName("analyze() should warn MEDIUM when SSL not detected in PostgreSQL URL")
    void testAnalyze_NoSSL_PostgreSQL() {
        DatabaseConfig config = DatabaseConfig.createPostgreSQLConfig(
            "localhost", 5432, "blockchain", "admin", "password123"
        );

        List<SecurityWarning> warnings = analyzer.analyze(config);

        // Should warn about SSL (PostgreSQL without SSL in URL)
        long sslWarnings = warnings.stream()
            .filter(w -> w.getCode() != null && w.getCode().equals("NO_SSL_DETECTED"))
            .count();

        assertTrue(sslWarnings >= 1, "Should have at least 1 SSL warning for PostgreSQL without SSL");

        // SSL warning should be MEDIUM or lower, never CRITICAL/HIGH
        warnings.stream()
            .filter(w -> w.getCode() != null && w.getCode().equals("NO_SSL_DETECTED"))
            .forEach(w -> {
                assertTrue(w.getSeverity() == SecurityWarning.Severity.MEDIUM ||
                          w.getSeverity() == SecurityWarning.Severity.LOW ||
                          w.getSeverity() == SecurityWarning.Severity.INFO,
                    "SSL warning should be MEDIUM/LOW/INFO, but was: " + w.getSeverity());
            });
    }

    @Test
    @DisplayName("analyze() should not warn about SSL for SQLite")
    void testAnalyze_SQLite_NoSSLWarning() {
        DatabaseConfig config = DatabaseConfig.createSQLiteConfig();

        List<SecurityWarning> warnings = analyzer.analyze(config);

        // Should not warn about SSL for SQLite
        assertFalse(warnings.stream().anyMatch(w ->
            w.getCode() != null && w.getCode().equals("NO_SSL_DETECTED")
        ));
    }

    // ============================================================================
    // Credential Strength Tests
    // ============================================================================

    @Test
    @DisplayName("analyze() should warn MEDIUM for weak password (too short)")
    void testAnalyze_WeakPassword_TooShort() {
        DatabaseConfig config = DatabaseConfig.createPostgreSQLConfig(
            "localhost", 5432, "blockchain", "admin", "short"
        );

        analyzer.withConfigSource(ConfigurationSecurityAnalyzer.ConfigSource.ENVIRONMENT);

        List<SecurityWarning> warnings = analyzer.analyze(config);

        // Should warn about weak password
        assertTrue(warnings.stream().anyMatch(w ->
            w.getSeverity() == SecurityWarning.Severity.MEDIUM &&
            w.getCode() != null &&
            w.getCode().equals("WEAK_PASSWORD_LENGTH")
        ), "Should warn about password length");
    }

    @Test
    @DisplayName("analyze() should warn CRITICAL for common passwords")
    void testAnalyze_CommonPassword() {
        String[] commonPasswords = {"password", "admin", "root", "123456", "test"};

        for (String commonPass : commonPasswords) {
            DatabaseConfig config = DatabaseConfig.createPostgreSQLConfig(
                "localhost", 5432, "blockchain", "admin", commonPass
            );

            analyzer = new ConfigurationSecurityAnalyzer();
            analyzer.withConfigSource(ConfigurationSecurityAnalyzer.ConfigSource.ENVIRONMENT);

            List<SecurityWarning> warnings = analyzer.analyze(config);

            assertTrue(warnings.stream().anyMatch(w ->
                w.getSeverity() == SecurityWarning.Severity.CRITICAL &&
                w.getCode() != null &&
                w.getCode().equals("COMMON_PASSWORD")
            ), "Should warn CRITICAL for common password: " + commonPass);
        }
    }

    @Test
    @DisplayName("analyze() should not warn for strong passwords")
    void testAnalyze_StrongPassword_NoWarning() {
        DatabaseConfig config = DatabaseConfig.createPostgreSQLConfig(
            "localhost", 5432, "blockchain", "admin", "Str0ng!P@ssw0rd#2025"
        );

        analyzer.withConfigSource(ConfigurationSecurityAnalyzer.ConfigSource.ENVIRONMENT);

        List<SecurityWarning> warnings = analyzer.analyze(config);

        // Should not warn about password strength
        assertFalse(warnings.stream().anyMatch(w ->
            w.getCode() != null &&
            (w.getCode().equals("WEAK_PASSWORD_LENGTH") ||
             w.getCode().equals("COMMON_PASSWORD"))
        ), "Should not warn about strong password");
    }

    @Test
    @DisplayName("analyze() should handle case-insensitive common password detection")
    void testAnalyze_CommonPassword_CaseInsensitive() {
        String[] variations = {"Password", "PASSWORD", "PaSsWoRd", "ADMIN", "Admin"};

        for (String pass : variations) {
            DatabaseConfig config = DatabaseConfig.createPostgreSQLConfig(
                "localhost", 5432, "blockchain", "admin", pass
            );

            analyzer = new ConfigurationSecurityAnalyzer();
            analyzer.withConfigSource(ConfigurationSecurityAnalyzer.ConfigSource.ENVIRONMENT);

            List<SecurityWarning> warnings = analyzer.analyze(config);

            assertTrue(warnings.stream().anyMatch(w ->
                w.getSeverity() == SecurityWarning.Severity.CRITICAL &&
                w.getCode() != null &&
                w.getCode().equals("COMMON_PASSWORD")
            ), "Should detect common password regardless of case: " + pass);
        }
    }

    // ============================================================================
    // MySQL Tests
    // ============================================================================

    @Test
    @DisplayName("analyze() should work correctly with MySQL config")
    void testAnalyze_MySQL() {
        DatabaseConfig config = DatabaseConfig.createMySQLConfig(
            "localhost", 3306, "blockchain", "root", "password123"
        );

        analyzer.withConfigSource(ConfigurationSecurityAnalyzer.ConfigSource.FILE);

        List<SecurityWarning> warnings = analyzer.analyze(config);

        // Should warn about password in file (same as PostgreSQL)
        assertTrue(warnings.stream().anyMatch(w ->
            w.getSeverity() == SecurityWarning.Severity.HIGH &&
            w.getMessage().toLowerCase().contains("password")
        ));
    }

    // ============================================================================
    // Fluent API Tests
    // ============================================================================

    @Test
    @DisplayName("withConfigFile() should support method chaining")
    void testFluentAPI_WithConfigFile() {
        ConfigurationSecurityAnalyzer result = analyzer.withConfigFile(tempConfigFile);

        assertSame(analyzer, result, "Should return same instance for chaining");
    }

    @Test
    @DisplayName("withConfigFile(String) should support method chaining")
    void testFluentAPI_WithConfigFileString() {
        ConfigurationSecurityAnalyzer result = analyzer.withConfigFile(tempConfigFile.toString());

        assertSame(analyzer, result);
    }

    @Test
    @DisplayName("withConfigSource() should support method chaining")
    void testFluentAPI_WithConfigSource() {
        ConfigurationSecurityAnalyzer result = analyzer
            .withConfigSource(ConfigurationSecurityAnalyzer.ConfigSource.ENVIRONMENT);

        assertSame(analyzer, result);
    }

    @Test
    @DisplayName("withConfigFile() should handle null gracefully")
    void testWithConfigFile_Null() {
        // Should not throw
        assertDoesNotThrow(() -> analyzer.withConfigFile((String) null));
    }

    @Test
    @DisplayName("withConfigFile() should handle empty string gracefully")
    void testWithConfigFile_Empty() {
        // Should not throw
        assertDoesNotThrow(() -> analyzer.withConfigFile(""));
    }

    @Test
    @DisplayName("withConfigSource() should handle null gracefully")
    void testWithConfigSource_Null() {
        // Should not throw and should use UNKNOWN as default
        assertDoesNotThrow(() -> analyzer.withConfigSource(null));
    }

    // ============================================================================
    // Integration Tests
    // ============================================================================

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    @DisplayName("Integration: Multiple warnings for highly insecure config")
    void testIntegration_MultipleWarnings() throws IOException {
        // Create worst-case scenario
        Files.setPosixFilePermissions(tempConfigFile,
            PosixFilePermissions.fromString("rw-rw-rw-"));

        DatabaseConfig config = DatabaseConfig.createPostgreSQLConfig(
            "localhost", 5432, "blockchain", "admin", "password"
        );

        analyzer.withConfigFile(tempConfigFile)
               .withConfigSource(ConfigurationSecurityAnalyzer.ConfigSource.FILE);

        List<SecurityWarning> warnings = analyzer.analyze(config);

        // Should have multiple warnings:
        // 1. Password in file (HIGH)
        // 2. Insecure file permissions (HIGH)
        // 3. Common password (CRITICAL)
        // 4. Possibly SSL warning (MEDIUM)

        long criticalWarnings = warnings.stream()
            .filter(w -> w.getSeverity() == SecurityWarning.Severity.CRITICAL)
            .count();
        long highWarnings = warnings.stream()
            .filter(w -> w.getSeverity() == SecurityWarning.Severity.HIGH)
            .count();

        assertTrue(criticalWarnings >= 1, "Should have at least 1 CRITICAL warning");
        assertTrue(highWarnings >= 2, "Should have at least 2 HIGH warnings");
        assertTrue(warnings.size() >= 3, "Should have at least 3 warnings total");
    }

    @Test
    @DisplayName("Integration: Clean config with environment variables")
    void testIntegration_CleanConfig() {
        DatabaseConfig config = DatabaseConfig.createPostgreSQLConfig(
            "localhost", 5432, "blockchain", "admin", "Str0ng!P@ssw0rd#2025"
        );

        analyzer.withConfigSource(ConfigurationSecurityAnalyzer.ConfigSource.ENVIRONMENT);

        List<SecurityWarning> warnings = analyzer.analyze(config);

        // Should have minimal warnings (maybe SSL)
        long criticalOrHighWarnings = warnings.stream()
            .filter(w -> w.getSeverity() == SecurityWarning.Severity.CRITICAL ||
                        w.getSeverity() == SecurityWarning.Severity.HIGH)
            .count();

        assertEquals(0, criticalOrHighWarnings,
            "Clean config from environment should have no CRITICAL/HIGH warnings");
    }

    // ============================================================================
    // ConfigSource Enum Tests
    // ============================================================================

    @Test
    @DisplayName("ConfigSource enum should have all expected values")
    void testConfigSource_AllValues() {
        ConfigurationSecurityAnalyzer.ConfigSource[] sources =
            ConfigurationSecurityAnalyzer.ConfigSource.values();

        assertEquals(5, sources.length);
        assertEquals(ConfigurationSecurityAnalyzer.ConfigSource.ENVIRONMENT, sources[0]);
        assertEquals(ConfigurationSecurityAnalyzer.ConfigSource.FILE, sources[1]);
        assertEquals(ConfigurationSecurityAnalyzer.ConfigSource.CLI_ARGS, sources[2]);
        assertEquals(ConfigurationSecurityAnalyzer.ConfigSource.DEFAULT, sources[3]);
        assertEquals(ConfigurationSecurityAnalyzer.ConfigSource.UNKNOWN, sources[4]);
    }
}
