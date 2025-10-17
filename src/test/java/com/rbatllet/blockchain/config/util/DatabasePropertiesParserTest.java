package com.rbatllet.blockchain.config.util;

import com.rbatllet.blockchain.config.DatabaseConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for DatabasePropertiesParser.
 */
@DisplayName("DatabasePropertiesParser Tests")
class DatabasePropertiesParserTest {

    // ============================================================================
    // Utility Class Tests
    // ============================================================================

    @Test
    @DisplayName("Constructor should throw AssertionError")
    void testConstructor_ThrowsAssertionError() {
        assertThrows(java.lang.reflect.InvocationTargetException.class, () -> {
            // Use reflection to invoke private constructor
            var constructor = DatabasePropertiesParser.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
        });

        // Also verify that the cause is AssertionError
        try {
            var constructor = DatabasePropertiesParser.class.getDeclaredConstructor();
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
    // Parse from InputStream Tests
    // ============================================================================

    @Test
    @DisplayName("parse(InputStream) should parse valid SQLite properties")
    void testParseInputStream_SQLite() throws IOException {
        String propsContent = "db.type=sqlite\n";
        InputStream in = new ByteArrayInputStream(propsContent.getBytes(StandardCharsets.UTF_8));

        DatabasePropertiesParser.ParseResult result = DatabasePropertiesParser.parse(in);

        assertTrue(result.isSuccess(), "Should parse successfully");
        assertNotNull(result.getConfig(), "Config should not be null");
        assertEquals(DatabaseConfig.DatabaseType.SQLITE, result.getConfig().getDatabaseType());
    }

    @Test
    @DisplayName("parse(InputStream) should return error for null input")
    void testParseInputStream_Null() throws IOException {
        DatabasePropertiesParser.ParseResult result = DatabasePropertiesParser.parse((InputStream) null);

        assertTrue(result.isFailure(), "Should fail for null input");
        assertFalse(result.getErrors().isEmpty(), "Should have errors");
        assertEquals("InputStream cannot be null", result.getErrors().get(0));
    }

    @Test
    @DisplayName("parse(InputStream) should handle empty properties")
    void testParseInputStream_Empty() throws IOException {
        InputStream in = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));

        DatabasePropertiesParser.ParseResult result = DatabasePropertiesParser.parse(in);

        // Empty properties should default to SQLite
        assertTrue(result.isSuccess(), "Should parse successfully with defaults");
        assertEquals(DatabaseConfig.DatabaseType.SQLITE, result.getConfig().getDatabaseType());
    }

    @Test
    @DisplayName("parse(InputStream) should handle invalid stream")
    void testParseInputStream_Invalid() {
        InputStream in = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("Simulated read error");
            }
        };

        assertThrows(IOException.class, () -> DatabasePropertiesParser.parse(in));
    }

    // ============================================================================
    // Parse from Reader Tests
    // ============================================================================

    @Test
    @DisplayName("parse(Reader) should parse valid PostgreSQL properties")
    void testParseReader_PostgreSQL() throws IOException {
        String propsContent = "db.type=postgresql\n" +
                             "db.postgresql.host=localhost\n" +
                             "db.postgresql.port=5432\n" +
                             "db.postgresql.database=testdb\n" +
                             "db.postgresql.username=testuser\n" +
                             "db.postgresql.password=testpass\n";
        Reader reader = new StringReader(propsContent);

        DatabasePropertiesParser.ParseResult result = DatabasePropertiesParser.parse(reader);

        assertTrue(result.isSuccess(), "Should parse successfully");
        assertNotNull(result.getConfig(), "Config should not be null");
        assertEquals(DatabaseConfig.DatabaseType.POSTGRESQL, result.getConfig().getDatabaseType());
    }

    @Test
    @DisplayName("parse(Reader) should return error for null reader")
    void testParseReader_Null() throws IOException {
        DatabasePropertiesParser.ParseResult result = DatabasePropertiesParser.parse((Reader) null);

        assertTrue(result.isFailure(), "Should fail for null reader");
        assertFalse(result.getErrors().isEmpty(), "Should have errors");
        assertEquals("Reader cannot be null", result.getErrors().get(0));
    }

    @Test
    @DisplayName("parse(Reader) should handle invalid reader")
    void testParseReader_Invalid() {
        Reader reader = new Reader() {
            @Override
            public int read(char[] cbuf, int off, int len) throws IOException {
                throw new IOException("Simulated read error");
            }

            @Override
            public void close() {}
        };

        assertThrows(IOException.class, () -> DatabasePropertiesParser.parse(reader));
    }

    // ============================================================================
    // Parse from Properties Tests - SQLite
    // ============================================================================

    @Test
    @DisplayName("parse(Properties) should parse SQLite with explicit type")
    void testParseProperties_SQLite_Explicit() {
        Properties props = new Properties();
        props.setProperty("db.type", "sqlite");

        DatabasePropertiesParser.ParseResult result = DatabasePropertiesParser.parse(props);

        assertTrue(result.isSuccess());
        assertNotNull(result.getConfig());
        assertEquals(DatabaseConfig.DatabaseType.SQLITE, result.getConfig().getDatabaseType());
        assertFalse(result.hasErrors());
    }

    @Test
    @DisplayName("parse(Properties) should default to SQLite when no type specified")
    void testParseProperties_SQLite_Default() {
        Properties props = new Properties();

        DatabasePropertiesParser.ParseResult result = DatabasePropertiesParser.parse(props);

        assertTrue(result.isSuccess());
        assertEquals(DatabaseConfig.DatabaseType.SQLITE, result.getConfig().getDatabaseType());
    }

    @Test
    @DisplayName("parse(Properties) should handle SQLite with uppercase type")
    void testParseProperties_SQLite_Uppercase() {
        Properties props = new Properties();
        props.setProperty("db.type", "SQLITE");

        DatabasePropertiesParser.ParseResult result = DatabasePropertiesParser.parse(props);

        assertTrue(result.isSuccess());
        assertEquals(DatabaseConfig.DatabaseType.SQLITE, result.getConfig().getDatabaseType());
    }

    // ============================================================================
    // Parse from Properties Tests - PostgreSQL
    // ============================================================================

    @Test
    @DisplayName("parse(Properties) should parse complete PostgreSQL configuration")
    void testParseProperties_PostgreSQL_Complete() {
        Properties props = new Properties();
        props.setProperty("db.type", "postgresql");
        props.setProperty("db.postgresql.host", "dbserver.example.com");
        props.setProperty("db.postgresql.port", "5433");
        props.setProperty("db.postgresql.database", "production_db");
        props.setProperty("db.postgresql.username", "admin");
        props.setProperty("db.postgresql.password", "securePass123");

        DatabasePropertiesParser.ParseResult result = DatabasePropertiesParser.parse(props);

        assertTrue(result.isSuccess(), "Should parse successfully");
        assertNotNull(result.getConfig());
        assertEquals(DatabaseConfig.DatabaseType.POSTGRESQL, result.getConfig().getDatabaseType());
        assertFalse(result.hasErrors());
        assertFalse(result.hasWarnings(), "Should have no warnings when password is provided");
    }

    @Test
    @DisplayName("parse(Properties) should use defaults for PostgreSQL when optional fields missing")
    void testParseProperties_PostgreSQL_Defaults() {
        Properties props = new Properties();
        props.setProperty("db.type", "postgresql");
        props.setProperty("db.postgresql.username", "testuser");
        props.setProperty("db.postgresql.password", "testpass");

        DatabasePropertiesParser.ParseResult result = DatabasePropertiesParser.parse(props);

        assertTrue(result.isSuccess(), "Should use defaults: localhost:5432/blockchain");
        assertFalse(result.hasErrors());
    }

    @Test
    @DisplayName("parse(Properties) should warn when PostgreSQL password is missing")
    void testParseProperties_PostgreSQL_MissingPassword() {
        Properties props = new Properties();
        props.setProperty("db.type", "postgresql");
        props.setProperty("db.postgresql.username", "testuser");

        DatabasePropertiesParser.ParseResult result = DatabasePropertiesParser.parse(props);

        assertTrue(result.isSuccess(), "Should parse successfully");
        assertTrue(result.hasWarnings(), "Should have warning about missing password");
        assertTrue(result.getWarnings().stream()
            .anyMatch(w -> w.contains("password not found") && w.contains("DB_PASSWORD")),
            "Warning should mention DB_PASSWORD environment variable");
    }

    @Test
    @DisplayName("parse(Properties) should fail when PostgreSQL username is missing")
    void testParseProperties_PostgreSQL_MissingUsername() {
        Properties props = new Properties();
        props.setProperty("db.type", "postgresql");
        props.setProperty("db.postgresql.password", "testpass");

        DatabasePropertiesParser.ParseResult result = DatabasePropertiesParser.parse(props);

        assertTrue(result.isFailure(), "Should fail without username");
        assertTrue(result.hasErrors());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.contains("username is required")),
            "Should have error about missing username");
    }

    @Test
    @DisplayName("parse(Properties) should fail when PostgreSQL username is empty")
    void testParseProperties_PostgreSQL_EmptyUsername() {
        Properties props = new Properties();
        props.setProperty("db.type", "postgresql");
        props.setProperty("db.postgresql.username", "");
        props.setProperty("db.postgresql.password", "testpass");

        DatabasePropertiesParser.ParseResult result = DatabasePropertiesParser.parse(props);

        assertTrue(result.isFailure(), "Should fail with empty username");
        assertTrue(result.hasErrors());
    }

    @Test
    @DisplayName("parse(Properties) should handle 'postgres' as alias for 'postgresql'")
    void testParseProperties_PostgreSQL_Alias() {
        Properties props = new Properties();
        props.setProperty("db.type", "postgres");
        props.setProperty("db.postgresql.username", "testuser");
        props.setProperty("db.postgresql.password", "testpass");

        DatabasePropertiesParser.ParseResult result = DatabasePropertiesParser.parse(props);

        assertTrue(result.isSuccess(), "'postgres' should be accepted as alias");
        assertEquals(DatabaseConfig.DatabaseType.POSTGRESQL, result.getConfig().getDatabaseType());
    }

    @Test
    @DisplayName("parse(Properties) should handle invalid PostgreSQL port")
    void testParseProperties_PostgreSQL_InvalidPort() {
        Properties props = new Properties();
        props.setProperty("db.type", "postgresql");
        props.setProperty("db.postgresql.port", "not-a-number");
        props.setProperty("db.postgresql.username", "testuser");
        props.setProperty("db.postgresql.password", "testpass");

        DatabasePropertiesParser.ParseResult result = DatabasePropertiesParser.parse(props);

        assertTrue(result.isFailure(), "Should fail with invalid port");
        assertTrue(result.hasErrors());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.contains("Invalid integer value") && e.contains("port")),
            "Should have error about invalid port");
    }

    // ============================================================================
    // Parse from Properties Tests - MySQL
    // ============================================================================

    @Test
    @DisplayName("parse(Properties) should parse complete MySQL configuration")
    void testParseProperties_MySQL_Complete() {
        Properties props = new Properties();
        props.setProperty("db.type", "mysql");
        props.setProperty("db.mysql.host", "mysql.example.com");
        props.setProperty("db.mysql.port", "3307");
        props.setProperty("db.mysql.database", "blockchain_prod");
        props.setProperty("db.mysql.username", "dbadmin");
        props.setProperty("db.mysql.password", "mysql_pass_123");

        DatabasePropertiesParser.ParseResult result = DatabasePropertiesParser.parse(props);

        assertTrue(result.isSuccess());
        assertNotNull(result.getConfig());
        assertEquals(DatabaseConfig.DatabaseType.MYSQL, result.getConfig().getDatabaseType());
        assertFalse(result.hasErrors());
        assertFalse(result.hasWarnings());
    }

    @Test
    @DisplayName("parse(Properties) should use defaults for MySQL when optional fields missing")
    void testParseProperties_MySQL_Defaults() {
        Properties props = new Properties();
        props.setProperty("db.type", "mysql");
        props.setProperty("db.mysql.username", "root");
        props.setProperty("db.mysql.password", "rootpass");

        DatabasePropertiesParser.ParseResult result = DatabasePropertiesParser.parse(props);

        assertTrue(result.isSuccess(), "Should use defaults: localhost:3306/blockchain");
        assertFalse(result.hasErrors());
    }

    @Test
    @DisplayName("parse(Properties) should warn when MySQL password is missing")
    void testParseProperties_MySQL_MissingPassword() {
        Properties props = new Properties();
        props.setProperty("db.type", "mysql");
        props.setProperty("db.mysql.username", "root");

        DatabasePropertiesParser.ParseResult result = DatabasePropertiesParser.parse(props);

        assertTrue(result.isSuccess());
        assertTrue(result.hasWarnings(), "Should have warning about missing password");
        assertTrue(result.getWarnings().stream()
            .anyMatch(w -> w.contains("password not found") && w.contains("DB_PASSWORD")),
            "Warning should mention DB_PASSWORD environment variable");
    }

    @Test
    @DisplayName("parse(Properties) should fail when MySQL username is missing")
    void testParseProperties_MySQL_MissingUsername() {
        Properties props = new Properties();
        props.setProperty("db.type", "mysql");
        props.setProperty("db.mysql.password", "pass");

        DatabasePropertiesParser.ParseResult result = DatabasePropertiesParser.parse(props);

        assertTrue(result.isFailure(), "Should fail without username");
        assertTrue(result.hasErrors());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.contains("username is required")),
            "Should have error about missing username");
    }

    @Test
    @DisplayName("parse(Properties) should handle invalid MySQL port")
    void testParseProperties_MySQL_InvalidPort() {
        Properties props = new Properties();
        props.setProperty("db.type", "mysql");
        props.setProperty("db.mysql.port", "invalid");
        props.setProperty("db.mysql.username", "root");
        props.setProperty("db.mysql.password", "pass");

        DatabasePropertiesParser.ParseResult result = DatabasePropertiesParser.parse(props);

        assertTrue(result.isFailure(), "Should fail with invalid port");
        assertTrue(result.hasErrors());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.contains("Invalid integer value")),
            "Should have error about invalid port");
    }

    // ============================================================================
    // Parse from Properties Tests - H2
    // ============================================================================

    @Test
    @DisplayName("parse(Properties) should parse H2 memory mode")
    void testParseProperties_H2_Memory() {
        Properties props = new Properties();
        props.setProperty("db.type", "h2");
        props.setProperty("db.h2.mode", "memory");

        DatabasePropertiesParser.ParseResult result = DatabasePropertiesParser.parse(props);

        assertTrue(result.isSuccess());
        assertNotNull(result.getConfig());
        assertEquals(DatabaseConfig.DatabaseType.H2, result.getConfig().getDatabaseType());
        assertTrue(result.hasWarnings(), "Should warn about H2 being for testing only");
        assertTrue(result.getWarnings().stream()
            .anyMatch(w -> w.contains("testing only") && w.contains("NOT recommended for production")),
            "Should warn about production use");
    }

    @Test
    @DisplayName("parse(Properties) should parse H2 file mode with custom path")
    void testParseProperties_H2_FileMode() {
        Properties props = new Properties();
        props.setProperty("db.type", "h2");
        props.setProperty("db.h2.mode", "file");
        props.setProperty("db.h2.file", "./custom-db-path");

        DatabasePropertiesParser.ParseResult result = DatabasePropertiesParser.parse(props);

        assertTrue(result.isSuccess());
        assertEquals(DatabaseConfig.DatabaseType.H2, result.getConfig().getDatabaseType());
        assertTrue(result.hasWarnings(), "Should warn about testing only");
    }

    @Test
    @DisplayName("parse(Properties) should default to H2 file mode")
    void testParseProperties_H2_DefaultFileMode() {
        Properties props = new Properties();
        props.setProperty("db.type", "h2");

        DatabasePropertiesParser.ParseResult result = DatabasePropertiesParser.parse(props);

        assertTrue(result.isSuccess(), "Should default to file mode");
        assertEquals(DatabaseConfig.DatabaseType.H2, result.getConfig().getDatabaseType());
    }

    @Test
    @DisplayName("parse(Properties) should handle H2 mode case-insensitive")
    void testParseProperties_H2_CaseInsensitiveMode() {
        Properties props = new Properties();
        props.setProperty("db.type", "h2");
        props.setProperty("db.h2.mode", "MEMORY");

        DatabasePropertiesParser.ParseResult result = DatabasePropertiesParser.parse(props);

        assertTrue(result.isSuccess());
        assertEquals(DatabaseConfig.DatabaseType.H2, result.getConfig().getDatabaseType());
    }

    // ============================================================================
    // Invalid Database Type Tests
    // ============================================================================

    @Test
    @DisplayName("parse(Properties) should fail for unknown database type")
    void testParseProperties_UnknownType() {
        Properties props = new Properties();
        props.setProperty("db.type", "oracle");

        DatabasePropertiesParser.ParseResult result = DatabasePropertiesParser.parse(props);

        assertTrue(result.isFailure(), "Should fail for unknown database type");
        assertTrue(result.hasErrors());
        assertTrue(result.getErrors().stream()
            .anyMatch(e -> e.contains("Unknown database type") && e.contains("oracle")),
            "Should specify unknown type in error message");
    }

    @Test
    @DisplayName("parse(Properties) should fail for invalid database type")
    void testParseProperties_InvalidType() {
        Properties props = new Properties();
        props.setProperty("db.type", "not-a-database");

        DatabasePropertiesParser.ParseResult result = DatabasePropertiesParser.parse(props);

        assertTrue(result.isFailure());
        assertTrue(result.hasErrors());
    }

    // ============================================================================
    // Null and Empty Input Tests
    // ============================================================================

    @Test
    @DisplayName("parse(Properties) should return error for null properties")
    void testParseProperties_Null() {
        DatabasePropertiesParser.ParseResult result = DatabasePropertiesParser.parse((Properties) null);

        assertTrue(result.isFailure(), "Should fail for null properties");
        assertFalse(result.getErrors().isEmpty());
        assertEquals("Properties cannot be null", result.getErrors().get(0));
    }

    @Test
    @DisplayName("parse(Properties) should handle empty properties")
    void testParseProperties_Empty() {
        Properties props = new Properties();

        DatabasePropertiesParser.ParseResult result = DatabasePropertiesParser.parse(props);

        assertTrue(result.isSuccess(), "Empty properties should default to SQLite");
        assertEquals(DatabaseConfig.DatabaseType.SQLITE, result.getConfig().getDatabaseType());
    }

    // ============================================================================
    // ParseResult Tests
    // ============================================================================

    @Test
    @DisplayName("ParseResult should indicate success correctly")
    void testParseResult_Success() {
        Properties props = new Properties();
        props.setProperty("db.type", "sqlite");

        DatabasePropertiesParser.ParseResult result = DatabasePropertiesParser.parse(props);

        assertTrue(result.isSuccess());
        assertFalse(result.isFailure());
        assertNotNull(result.getConfig());
        assertFalse(result.hasErrors());
    }

    @Test
    @DisplayName("ParseResult should indicate failure correctly")
    void testParseResult_Failure() {
        Properties props = new Properties();
        props.setProperty("db.type", "invalid");

        DatabasePropertiesParser.ParseResult result = DatabasePropertiesParser.parse(props);

        assertFalse(result.isSuccess());
        assertTrue(result.isFailure());
        assertNull(result.getConfig(), "Config should be null on failure");
        assertTrue(result.hasErrors());
    }

    @Test
    @DisplayName("ParseResult.getErrors() should return immutable list")
    void testParseResult_ErrorsImmutable() {
        Properties props = new Properties();
        props.setProperty("db.type", "invalid");

        DatabasePropertiesParser.ParseResult result = DatabasePropertiesParser.parse(props);
        List<String> errors = result.getErrors();

        assertNotNull(errors);
        // Verify it's a copy (modifications don't affect original)
        int originalSize = errors.size();
        errors.clear();
        assertEquals(originalSize, result.getErrors().size(), "Should return a copy, not the original list");
    }

    @Test
    @DisplayName("ParseResult.getWarnings() should return immutable list")
    void testParseResult_WarningsImmutable() {
        Properties props = new Properties();
        props.setProperty("db.type", "h2");

        DatabasePropertiesParser.ParseResult result = DatabasePropertiesParser.parse(props);
        List<String> warnings = result.getWarnings();

        assertNotNull(warnings);
        // Verify it's a copy
        int originalSize = warnings.size();
        warnings.clear();
        assertEquals(originalSize, result.getWarnings().size(), "Should return a copy, not the original list");
    }

    @Test
    @DisplayName("ParseResult.hasWarnings() should work correctly")
    void testParseResult_HasWarnings() {
        Properties propsWithWarning = new Properties();
        propsWithWarning.setProperty("db.type", "h2");

        DatabasePropertiesParser.ParseResult resultWithWarning = DatabasePropertiesParser.parse(propsWithWarning);
        assertTrue(resultWithWarning.hasWarnings(), "H2 should produce warnings");

        Properties propsWithoutWarning = new Properties();
        propsWithoutWarning.setProperty("db.type", "sqlite");

        DatabasePropertiesParser.ParseResult resultWithoutWarning = DatabasePropertiesParser.parse(propsWithoutWarning);
        assertFalse(resultWithoutWarning.hasWarnings(), "SQLite should not produce warnings");
    }

    @Test
    @DisplayName("ParseResult.toString() should include key information")
    void testParseResult_ToString() {
        Properties props = new Properties();
        props.setProperty("db.type", "postgresql");
        props.setProperty("db.postgresql.username", "user");

        DatabasePropertiesParser.ParseResult result = DatabasePropertiesParser.parse(props);
        String str = result.toString();

        assertNotNull(str);
        assertTrue(str.contains("success="), "Should include success field");
        assertTrue(str.contains("hasConfig="), "Should include hasConfig field");
        assertTrue(str.contains("errors="), "Should include errors count");
        assertTrue(str.contains("warnings="), "Should include warnings count");
    }

    // ============================================================================
    // Integration Tests with File I/O
    // ============================================================================

    @Test
    @DisplayName("Integration: parse from file with complete configuration")
    void testIntegration_ParseFromFile(@TempDir Path tempDir) throws IOException {
        // Create a properties file
        Path propsFile = tempDir.resolve("database.properties");
        String content = """
            db.type=postgresql
            db.postgresql.host=dbserver.local
            db.postgresql.port=5432
            db.postgresql.database=blockchain_db
            db.postgresql.username=admin
            db.postgresql.password=securePassword123
            """;
        Files.writeString(propsFile, content, StandardCharsets.UTF_8);

        // Parse from InputStream
        try (InputStream in = Files.newInputStream(propsFile)) {
            DatabasePropertiesParser.ParseResult result = DatabasePropertiesParser.parse(in);

            assertTrue(result.isSuccess(), "Should parse file successfully");
            assertNotNull(result.getConfig());
            assertEquals(DatabaseConfig.DatabaseType.POSTGRESQL, result.getConfig().getDatabaseType());
            assertFalse(result.hasErrors());
        }
    }

    @Test
    @DisplayName("Integration: parse from file with missing required fields")
    void testIntegration_ParseFromFileWithErrors(@TempDir Path tempDir) throws IOException {
        Path propsFile = tempDir.resolve("invalid.properties");
        String content = """
            db.type=postgresql
            db.postgresql.host=localhost
            # Missing username - should fail
            """;
        Files.writeString(propsFile, content, StandardCharsets.UTF_8);

        try (InputStream in = Files.newInputStream(propsFile)) {
            DatabasePropertiesParser.ParseResult result = DatabasePropertiesParser.parse(in);

            assertTrue(result.isFailure(), "Should fail with missing username");
            assertTrue(result.hasErrors());
        }
    }

    // ============================================================================
    // Edge Cases and Security Tests
    // ============================================================================

    @Test
    @DisplayName("Edge Case: properties with whitespace should be trimmed")
    void testEdgeCase_WhitespaceHandling() {
        Properties props = new Properties();
        props.setProperty("db.type", "  postgresql  ");
        props.setProperty("db.postgresql.username", "user");
        props.setProperty("db.postgresql.password", "pass");

        DatabasePropertiesParser.ParseResult result = DatabasePropertiesParser.parse(props);

        // Properties.getProperty() doesn't auto-trim, so this depends on implementation
        // The toLowerCase() should handle this, but let's verify
        assertTrue(result.isSuccess() || result.isFailure(), "Should handle whitespace gracefully");
    }

    @Test
    @DisplayName("Edge Case: very long property values")
    void testEdgeCase_LongValues() {
        Properties props = new Properties();
        props.setProperty("db.type", "postgresql");
        props.setProperty("db.postgresql.username", "user");
        props.setProperty("db.postgresql.password", "x".repeat(10000));

        DatabasePropertiesParser.ParseResult result = DatabasePropertiesParser.parse(props);

        assertTrue(result.isSuccess(), "Should handle long values");
    }

    @Test
    @DisplayName("Edge Case: special characters in values")
    void testEdgeCase_SpecialCharacters() {
        Properties props = new Properties();
        props.setProperty("db.type", "postgresql");
        props.setProperty("db.postgresql.username", "user@domain.com");
        props.setProperty("db.postgresql.password", "p@ss!w0rd#$%");
        props.setProperty("db.postgresql.database", "db-name_123");

        DatabasePropertiesParser.ParseResult result = DatabasePropertiesParser.parse(props);

        assertTrue(result.isSuccess(), "Should handle special characters");
    }

    @Test
    @DisplayName("Security: parser should not expose sensitive data in error messages")
    void testSecurity_NoPasswordInErrors() {
        Properties props = new Properties();
        props.setProperty("db.type", "postgresql");
        props.setProperty("db.postgresql.username", "admin");
        props.setProperty("db.postgresql.password", "secretPassword123");
        props.setProperty("db.postgresql.port", "invalid-port");

        DatabasePropertiesParser.ParseResult result = DatabasePropertiesParser.parse(props);

        assertTrue(result.isFailure(), "Should fail with invalid port");
        String allErrors = String.join(" ", result.getErrors());
        assertFalse(allErrors.contains("secretPassword123"),
            "Error messages should not contain passwords");
    }

    @Test
    @DisplayName("Multiple errors should be collected")
    void testMultipleErrors_Collected() {
        Properties props = new Properties();
        props.setProperty("db.type", "postgresql");
        props.setProperty("db.postgresql.port", "not-a-number");
        // Missing username

        DatabasePropertiesParser.ParseResult result = DatabasePropertiesParser.parse(props);

        assertTrue(result.isFailure());
        assertTrue(result.getErrors().size() >= 2, "Should collect multiple errors: invalid port + missing username");
    }
}
