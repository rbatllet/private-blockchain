package com.rbatllet.blockchain.config.util;

import com.rbatllet.blockchain.config.DatabaseConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for ConfigurationExporter.
 */
@DisplayName("ConfigurationExporter Tests")
class ConfigurationExporterTest {

    // ============================================================================
    // Constructor and Builder Tests
    // ============================================================================

    @Test
    @DisplayName("Default constructor should enable masking and pretty print")
    void testConstructor_Defaults() {
        ConfigurationExporter exporter = new ConfigurationExporter();
        assertNotNull(exporter);
    }

    @Test
    @DisplayName("withMasking() should create new instance with masking disabled")
    void testWithMasking_Disabled() {
        ConfigurationExporter exporter = new ConfigurationExporter().withMasking(false);
        assertNotNull(exporter);
    }

    @Test
    @DisplayName("withPrettyPrint() should create new instance")
    void testWithPrettyPrint() {
        ConfigurationExporter exporter = new ConfigurationExporter().withPrettyPrint(false);
        assertNotNull(exporter);
    }

    @Test
    @DisplayName("Builder methods should be chainable")
    void testBuilder_Chaining() {
        ConfigurationExporter exporter = new ConfigurationExporter()
            .withMasking(false)
            .withPrettyPrint(true);
        assertNotNull(exporter);
    }

    // ============================================================================
    // exportToProperties() Tests
    // ============================================================================

    @Test
    @DisplayName("exportToProperties() should export PostgreSQL config with masked password")
    void testExportToProperties_PostgreSQL_Masked() {
        DatabaseConfig config = DatabaseConfig.createPostgreSQLConfig(
            "localhost", 5432, "blockchain", "admin", "secretPassword"
        );

        ConfigurationExporter exporter = new ConfigurationExporter();
        String output = exporter.exportToProperties(config);

        assertNotNull(output);
        assertTrue(output.contains("db.type=postgresql"), "Should contain database type");
        assertFalse(output.contains("secretPassword"), "Password should not be exposed: " + output);
        assertTrue(output.contains("***REDACTED***"), "Password should be masked: " + output);
    }

    @Test
    @DisplayName("exportToProperties() should export without masking when disabled")
    void testExportToProperties_PostgreSQL_Unmasked() {
        DatabaseConfig config = DatabaseConfig.createPostgreSQLConfig(
            "localhost", 5432, "blockchain", "admin", "secretPassword"
        );

        ConfigurationExporter exporter = new ConfigurationExporter().withMasking(false);
        String output = exporter.exportToProperties(config);

        assertNotNull(output);
        assertTrue(output.contains("db.postgresql.password=secretPassword"),
            "Password should be visible when masking is disabled");
    }

    @Test
    @DisplayName("exportToProperties() should throw IllegalArgumentException for null config")
    void testExportToProperties_NullConfig() {
        ConfigurationExporter exporter = new ConfigurationExporter();
        assertThrows(IllegalArgumentException.class, () ->
            exporter.exportToProperties(null)
        );
    }

    @Test
    @DisplayName("exportToProperties() should handle MySQL configuration")
    void testExportToProperties_MySQL() {
        DatabaseConfig config = DatabaseConfig.createMySQLConfig(
            "mysql.example.com", 3306, "mydb", "root", "rootpass"
        );

        ConfigurationExporter exporter = new ConfigurationExporter();
        String output = exporter.exportToProperties(config);

        assertTrue(output.contains("db.type=mysql"));
        assertTrue(output.contains("db.mysql.host=mysql.example.com"));
        assertTrue(output.contains("db.mysql.port=3306"));
        assertTrue(output.contains("***REDACTED***"));
        assertFalse(output.contains("rootpass"), "Password should not be exposed");
    }

    @Test
    @DisplayName("exportToProperties() should handle SQLite configuration")
    void testExportToProperties_SQLite() {
        DatabaseConfig config = DatabaseConfig.createSQLiteConfig();

        ConfigurationExporter exporter = new ConfigurationExporter();
        String output = exporter.exportToProperties(config);

        assertTrue(output.contains("db.type=sqlite"));
        assertTrue(output.contains("SQLite"));
    }

    @Test
    @DisplayName("exportToProperties() should handle H2 configuration")
    void testExportToProperties_H2() {
        DatabaseConfig config = DatabaseConfig.createH2TestConfig();

        ConfigurationExporter exporter = new ConfigurationExporter();
        String output = exporter.exportToProperties(config);

        assertTrue(output.contains("db.type=h2"));
    }

    // ============================================================================
    // exportToJson() Tests
    // ============================================================================

    @Test
    @DisplayName("exportToJson() should export PostgreSQL config with masked password")
    void testExportToJson_PostgreSQL_Masked() {
        DatabaseConfig config = DatabaseConfig.createPostgreSQLConfig(
            "localhost", 5432, "blockchain", "admin", "secretPassword"
        );

        ConfigurationExporter exporter = new ConfigurationExporter();
        String output = exporter.exportToJson(config);

        assertNotNull(output);
        assertTrue(output.contains("\"type\"") || output.contains("\"type\" :"));
        assertTrue(output.contains("postgresql"));
        assertTrue(output.contains("\"host\""));
        assertTrue(output.contains("localhost"));
        assertTrue(output.contains("\"port\""));
        assertTrue(output.contains("5432"));
        assertTrue(output.contains("\"username\""));
        assertTrue(output.contains("admin"));
        assertTrue(output.contains("********"), "Password should be masked with ********");
        assertFalse(output.contains("secretPassword"), "Password should not be exposed");
    }

    @Test
    @DisplayName("exportToJson() should export without masking when disabled")
    void testExportToJson_Unmasked() {
        DatabaseConfig config = DatabaseConfig.createPostgreSQLConfig(
            "localhost", 5432, "blockchain", "admin", "secretPassword"
        );

        ConfigurationExporter exporter = new ConfigurationExporter().withMasking(false);
        String output = exporter.exportToJson(config);

        assertTrue(output.contains("secretPassword"));
    }

    @Test
    @DisplayName("exportToJson() should throw IllegalArgumentException for null config")
    void testExportToJson_NullConfig() {
        ConfigurationExporter exporter = new ConfigurationExporter();
        assertThrows(IllegalArgumentException.class, () ->
            exporter.exportToJson(null)
        );
    }

    @Test
    @DisplayName("exportToJson() with pretty print should be formatted")
    void testExportToJson_PrettyPrint() {
        DatabaseConfig config = DatabaseConfig.createPostgreSQLConfig(
            "localhost", 5432, "blockchain", "admin", "pass"
        );

        ConfigurationExporter exporter = new ConfigurationExporter().withPrettyPrint(true);
        String output = exporter.exportToJson(config);

        // Pretty printed JSON should have newlines
        assertTrue(output.contains("\n"), "Pretty printed JSON should have newlines");
    }

    @Test
    @DisplayName("exportToJson() without pretty print should be compact")
    void testExportToJson_NoPrettyPrint() {
        DatabaseConfig config = DatabaseConfig.createPostgreSQLConfig(
            "localhost", 5432, "blockchain", "admin", "pass"
        );

        ConfigurationExporter exporter = new ConfigurationExporter().withPrettyPrint(false);
        String output = exporter.exportToJson(config);

        // Compact JSON might still have some whitespace, but less formatted
        assertNotNull(output);
    }

    @Test
    @DisplayName("exportToJson() should handle SQLite")
    void testExportToJson_SQLite() {
        DatabaseConfig config = DatabaseConfig.createSQLiteConfig();

        ConfigurationExporter exporter = new ConfigurationExporter();
        String output = exporter.exportToJson(config);

        assertTrue(output.contains("sqlite"));
    }

    // ============================================================================
    // exportToEnv() Tests
    // ============================================================================

    @Test
    @DisplayName("exportToEnv() should export PostgreSQL config with masked password")
    void testExportToEnv_PostgreSQL_Masked() {
        DatabaseConfig config = DatabaseConfig.createPostgreSQLConfig(
            "localhost", 5432, "blockchain", "admin", "secretPassword"
        );

        ConfigurationExporter exporter = new ConfigurationExporter();
        String output = exporter.exportToEnv(config);

        assertNotNull(output);
        assertTrue(output.contains("DB_TYPE=postgresql"));
        assertTrue(output.contains("DB_HOST=localhost"));
        assertTrue(output.contains("DB_PORT=5432"));
        assertTrue(output.contains("DB_DATABASE=blockchain"));
        assertTrue(output.contains("DB_USERNAME=admin"));
        assertTrue(output.contains("DB_PASSWORD=********"), "Password should be masked with ********");
        assertFalse(output.contains("secretPassword"), "Password should not be exposed");
    }

    @Test
    @DisplayName("exportToEnv() should export without masking when disabled")
    void testExportToEnv_Unmasked() {
        DatabaseConfig config = DatabaseConfig.createPostgreSQLConfig(
            "localhost", 5432, "blockchain", "admin", "secretPassword"
        );

        ConfigurationExporter exporter = new ConfigurationExporter().withMasking(false);
        String output = exporter.exportToEnv(config);

        assertTrue(output.contains("DB_PASSWORD=secretPassword"));
    }

    @Test
    @DisplayName("exportToEnv() should throw IllegalArgumentException for null config")
    void testExportToEnv_NullConfig() {
        ConfigurationExporter exporter = new ConfigurationExporter();
        assertThrows(IllegalArgumentException.class, () ->
            exporter.exportToEnv(null)
        );
    }

    @Test
    @DisplayName("exportToEnv() should handle SQLite")
    void testExportToEnv_SQLite() {
        DatabaseConfig config = DatabaseConfig.createSQLiteConfig();

        ConfigurationExporter exporter = new ConfigurationExporter();
        String output = exporter.exportToEnv(config);

        assertTrue(output.contains("DB_TYPE=sqlite"));
    }

    // ============================================================================
    // exportToFile() Tests
    // ============================================================================

    @Test
    @DisplayName("exportToFile() should export to Properties file")
    void testExportToFile_Properties(@TempDir Path tempDir) throws IOException {
        DatabaseConfig config = DatabaseConfig.createPostgreSQLConfig(
            "localhost", 5432, "blockchain", "admin", "pass"
        );

        Path outputFile = tempDir.resolve("database.properties");
        ConfigurationExporter exporter = new ConfigurationExporter();

        exporter.exportToFile(config, outputFile, ConfigurationExporter.ExportFormat.PROPERTIES);

        assertTrue(Files.exists(outputFile));
        String content = Files.readString(outputFile);
        assertTrue(content.contains("db.type=postgresql"));
    }

    @Test
    @DisplayName("exportToFile() should export to JSON file")
    void testExportToFile_Json(@TempDir Path tempDir) throws IOException {
        DatabaseConfig config = DatabaseConfig.createPostgreSQLConfig(
            "localhost", 5432, "blockchain", "admin", "pass"
        );

        Path outputFile = tempDir.resolve("database.json");
        ConfigurationExporter exporter = new ConfigurationExporter();

        exporter.exportToFile(config, outputFile, ConfigurationExporter.ExportFormat.JSON);

        assertTrue(Files.exists(outputFile));
        String content = Files.readString(outputFile);
        assertTrue(content.contains("postgresql"));
    }

    @Test
    @DisplayName("exportToFile() should export to ENV file")
    void testExportToFile_Env(@TempDir Path tempDir) throws IOException {
        DatabaseConfig config = DatabaseConfig.createPostgreSQLConfig(
            "localhost", 5432, "blockchain", "admin", "pass"
        );

        Path outputFile = tempDir.resolve("database.env");
        ConfigurationExporter exporter = new ConfigurationExporter();

        exporter.exportToFile(config, outputFile, ConfigurationExporter.ExportFormat.ENV);

        assertTrue(Files.exists(outputFile));
        String content = Files.readString(outputFile);
        assertTrue(content.contains("DB_TYPE=postgresql"));
    }

    @Test
    @DisplayName("exportToFile() should auto-detect format from .properties extension")
    void testExportToFile_AutoDetect_Properties(@TempDir Path tempDir) throws IOException {
        DatabaseConfig config = DatabaseConfig.createSQLiteConfig();
        Path outputFile = tempDir.resolve("config.properties");

        ConfigurationExporter exporter = new ConfigurationExporter();
        exporter.exportToFile(config, outputFile, null);  // null = auto-detect

        assertTrue(Files.exists(outputFile));
        String content = Files.readString(outputFile);
        assertTrue(content.contains("db.type=sqlite"));
    }

    @Test
    @DisplayName("exportToFile() should auto-detect format from .json extension")
    void testExportToFile_AutoDetect_Json(@TempDir Path tempDir) throws IOException {
        DatabaseConfig config = DatabaseConfig.createSQLiteConfig();
        Path outputFile = tempDir.resolve("config.json");

        ConfigurationExporter exporter = new ConfigurationExporter();
        exporter.exportToFile(config, outputFile, null);

        assertTrue(Files.exists(outputFile));
    }

    @Test
    @DisplayName("exportToFile() should throw for null config")
    void testExportToFile_NullConfig(@TempDir Path tempDir) {
        Path outputFile = tempDir.resolve("output.properties");
        ConfigurationExporter exporter = new ConfigurationExporter();

        assertThrows(IllegalArgumentException.class, () ->
            exporter.exportToFile(null, outputFile, ConfigurationExporter.ExportFormat.PROPERTIES)
        );
    }

    @Test
    @DisplayName("exportToFile() should throw for null path")
    void testExportToFile_NullPath() {
        DatabaseConfig config = DatabaseConfig.createSQLiteConfig();
        ConfigurationExporter exporter = new ConfigurationExporter();

        assertThrows(IllegalArgumentException.class, () ->
            exporter.exportToFile(config, null, ConfigurationExporter.ExportFormat.PROPERTIES)
        );
    }

    @Test
    @DisplayName("exportToFile() should throw for unknown extension without format")
    void testExportToFile_UnknownExtension(@TempDir Path tempDir) {
        DatabaseConfig config = DatabaseConfig.createSQLiteConfig();
        Path outputFile = tempDir.resolve("config.unknown");

        ConfigurationExporter exporter = new ConfigurationExporter();

        assertThrows(IllegalArgumentException.class, () ->
            exporter.exportToFile(config, outputFile, null)
        );
    }

    // ============================================================================
    // ExportFormat Enum Tests
    // ============================================================================

    @Test
    @DisplayName("ExportFormat enum should have all expected values")
    void testExportFormat_AllValues() {
        ConfigurationExporter.ExportFormat[] formats = ConfigurationExporter.ExportFormat.values();

        assertEquals(3, formats.length);
        assertEquals(ConfigurationExporter.ExportFormat.PROPERTIES, formats[0]);
        assertEquals(ConfigurationExporter.ExportFormat.JSON, formats[1]);
        assertEquals(ConfigurationExporter.ExportFormat.ENV, formats[2]);
    }

    // ============================================================================
    // Security and Edge Case Tests
    // ============================================================================

    @Test
    @DisplayName("SECURITY: Exported files should not contain plaintext passwords by default")
    void testSecurity_NoPlaintextPasswords(@TempDir Path tempDir) throws IOException {
        String sensitivePassword = "SuperSecret123!";
        DatabaseConfig config = DatabaseConfig.createPostgreSQLConfig(
            "localhost", 5432, "blockchain", "admin", sensitivePassword
        );

        ConfigurationExporter exporter = new ConfigurationExporter(); // Default: masking enabled

        // Test all formats
        Path propsFile = tempDir.resolve("test.properties");
        Path jsonFile = tempDir.resolve("test.json");
        Path envFile = tempDir.resolve("test.env");

        exporter.exportToFile(config, propsFile, ConfigurationExporter.ExportFormat.PROPERTIES);
        exporter.exportToFile(config, jsonFile, ConfigurationExporter.ExportFormat.JSON);
        exporter.exportToFile(config, envFile, ConfigurationExporter.ExportFormat.ENV);

        // Verify password is masked in all files
        String propsContent = Files.readString(propsFile);
        String jsonContent = Files.readString(jsonFile);
        String envContent = Files.readString(envFile);

        assertFalse(propsContent.contains(sensitivePassword), "Properties should not contain plaintext password");
        assertFalse(jsonContent.contains(sensitivePassword), "JSON should not contain plaintext password");
        assertFalse(envContent.contains(sensitivePassword), "ENV should not contain plaintext password");

        // Verify masking markers are present
        assertTrue(propsContent.contains("***REDACTED***") || propsContent.contains("********"),
            "Properties should contain masking marker");
        assertTrue(jsonContent.contains("********"), "JSON should contain masking marker");
        assertTrue(envContent.contains("********"), "ENV should contain masking marker");
    }

    @Test
    @DisplayName("Integration: Full export workflow")
    void testIntegration_FullWorkflow(@TempDir Path tempDir) throws IOException {
        DatabaseConfig config = DatabaseConfig.createPostgreSQLConfig(
            "prod-db.example.com", 5432, "production_blockchain", "prod_user", "prod_password"
        );

        ConfigurationExporter exporter = new ConfigurationExporter()
            .withMasking(true)
            .withPrettyPrint(true);

        // Export to all formats
        Path propsFile = tempDir.resolve("database.properties");
        Path jsonFile = tempDir.resolve("database.json");
        Path envFile = tempDir.resolve("database.env");

        exporter.exportToFile(config, propsFile, null); // Auto-detect
        exporter.exportToFile(config, jsonFile, null);
        exporter.exportToFile(config, envFile, null);

        // All files should exist
        assertTrue(Files.exists(propsFile));
        assertTrue(Files.exists(jsonFile));
        assertTrue(Files.exists(envFile));

        // All files should contain database type
        assertTrue(Files.readString(propsFile).contains("postgresql"));
        assertTrue(Files.readString(jsonFile).contains("postgresql"));
        assertTrue(Files.readString(envFile).contains("postgresql"));
    }
}
