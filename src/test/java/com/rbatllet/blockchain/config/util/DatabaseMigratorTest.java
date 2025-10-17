package com.rbatllet.blockchain.config.util;

import com.rbatllet.blockchain.config.DatabaseConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for DatabaseMigrator.
 */
@DisplayName("DatabaseMigrator Tests")
class DatabaseMigratorTest {

    /**
     * Creates a unique H2 in-memory database configuration for each test.
     * Uses separate database names to ensure isolation between tests.
     */
    private DatabaseConfig createUniqueTestConfig() {
        String uniqueDbName = "test_" + UUID.randomUUID().toString().replace("-", "");
        String jdbcUrl = "jdbc:h2:mem:" + uniqueDbName + ";DB_CLOSE_DELAY=-1;MODE=PostgreSQL";
        return DatabaseConfig.forDatabaseUrl(DatabaseConfig.DatabaseType.H2, jdbcUrl, "sa", "");
    }

    // ============================================================================
    // Constructor Tests
    // ============================================================================

    @Test
    @DisplayName("Constructor should accept valid database config")
    void testConstructor_ValidConfig() {
        DatabaseConfig config = createUniqueTestConfig();
        DatabaseMigrator migrator = new DatabaseMigrator(config);
        assertNotNull(migrator);
    }

    @Test
    @DisplayName("Constructor should throw IllegalArgumentException for null config")
    void testConstructor_NullConfig() {
        assertThrows(IllegalArgumentException.class, () ->
            new DatabaseMigrator(null)
        );
    }

    @Test
    @DisplayName("Constructor with custom schema table name should work")
    void testConstructor_CustomSchemaTable() {
        DatabaseConfig config = createUniqueTestConfig();
        DatabaseMigrator migrator = new DatabaseMigrator(config, "custom_schema_history", false);
        assertNotNull(migrator);
    }

    @Test
    @DisplayName("Constructor should throw for null schema table name")
    void testConstructor_NullSchemaTableName() {
        DatabaseConfig config = createUniqueTestConfig();
        assertThrows(IllegalArgumentException.class, () ->
            new DatabaseMigrator(config, null, false)
        );
    }

    @Test
    @DisplayName("Constructor should throw for empty schema table name")
    void testConstructor_EmptySchemaTableName() {
        DatabaseConfig config = createUniqueTestConfig();
        assertThrows(IllegalArgumentException.class, () ->
            new DatabaseMigrator(config, "  ", false)
        );
    }

    // ============================================================================
    // Migration Building Tests
    // ============================================================================

    @Test
    @DisplayName("Migration.builder() should create valid migration")
    void testMigrationBuilder_Valid() {
        DatabaseMigrator.Migration migration = DatabaseMigrator.Migration.builder()
            .version("V1")
            .description("Create users table")
            .sql("CREATE TABLE users (id INT PRIMARY KEY)")
            .build();

        assertNotNull(migration);
        assertEquals("V1", migration.getVersion());
        assertEquals("Create users table", migration.getDescription());
        assertEquals("CREATE TABLE users (id INT PRIMARY KEY)", migration.getSql());
    }

    @Test
    @DisplayName("Migration.builder() should throw for null version")
    void testMigrationBuilder_NullVersion() {
        assertThrows(IllegalArgumentException.class, () ->
            DatabaseMigrator.Migration.builder()
                .version(null)
                .description("Test")
                .sql("SELECT 1")
                .build()
        );
    }

    @Test
    @DisplayName("Migration.builder() should throw for empty version")
    void testMigrationBuilder_EmptyVersion() {
        assertThrows(IllegalArgumentException.class, () ->
            DatabaseMigrator.Migration.builder()
                .version("  ")
                .description("Test")
                .sql("SELECT 1")
                .build()
        );
    }

    @Test
    @DisplayName("Migration.builder() should throw for null description")
    void testMigrationBuilder_NullDescription() {
        assertThrows(IllegalArgumentException.class, () ->
            DatabaseMigrator.Migration.builder()
                .version("V1")
                .description(null)
                .sql("SELECT 1")
                .build()
        );
    }

    @Test
    @DisplayName("Migration.builder() should throw for null SQL")
    void testMigrationBuilder_NullSql() {
        assertThrows(IllegalArgumentException.class, () ->
            DatabaseMigrator.Migration.builder()
                .version("V1")
                .description("Test")
                .sql(null)
                .build()
        );
    }

    @Test
    @DisplayName("Migration.calculateChecksum() should return consistent value")
    void testMigration_Checksum() {
        DatabaseMigrator.Migration migration = DatabaseMigrator.Migration.builder()
            .version("V1")
            .description("Test")
            .sql("CREATE TABLE test (id INT)")
            .build();

        int checksum1 = migration.calculateChecksum();
        int checksum2 = migration.calculateChecksum();

        assertEquals(checksum1, checksum2, "Checksum should be consistent");
    }

    @Test
    @DisplayName("Migration.calculateChecksum() should differ for different SQL")
    void testMigration_ChecksumDifferent() {
        DatabaseMigrator.Migration migration1 = DatabaseMigrator.Migration.builder()
            .version("V1")
            .description("Test")
            .sql("CREATE TABLE test1 (id INT)")
            .build();

        DatabaseMigrator.Migration migration2 = DatabaseMigrator.Migration.builder()
            .version("V1")
            .description("Test")
            .sql("CREATE TABLE test2 (id INT)")
            .build();

        assertNotEquals(migration1.calculateChecksum(), migration2.calculateChecksum(),
            "Different SQL should have different checksums");
    }

    // ============================================================================
    // addMigration() Tests
    // ============================================================================

    @Test
    @DisplayName("addMigration() should add single migration")
    void testAddMigration_Single() {
        DatabaseConfig config = createUniqueTestConfig();
        DatabaseMigrator migrator = new DatabaseMigrator(config);

        DatabaseMigrator.Migration migration = DatabaseMigrator.Migration.builder()
            .version("V1")
            .description("Test")
            .sql("CREATE TABLE test (id INT)")
            .build();

        DatabaseMigrator result = migrator.addMigration(migration);
        assertSame(migrator, result, "Should return this for chaining");
    }

    @Test
    @DisplayName("addMigration() should throw for null migration")
    void testAddMigration_Null() {
        DatabaseConfig config = createUniqueTestConfig();
        DatabaseMigrator migrator = new DatabaseMigrator(config);

        assertThrows(IllegalArgumentException.class, () ->
            migrator.addMigration(null)
        );
    }

    @Test
    @DisplayName("addMigration() should support chaining")
    void testAddMigration_Chaining() {
        DatabaseConfig config = createUniqueTestConfig();
        DatabaseMigrator migrator = new DatabaseMigrator(config);

        DatabaseMigrator.Migration m1 = DatabaseMigrator.Migration.builder()
            .version("V1").description("Test1").sql("SELECT 1").build();

        DatabaseMigrator.Migration m2 = DatabaseMigrator.Migration.builder()
            .version("V2").description("Test2").sql("SELECT 2").build();

        DatabaseMigrator result = migrator.addMigration(m1).addMigration(m2);
        assertSame(migrator, result);
    }

    @Test
    @DisplayName("addMigrations() should add multiple migrations")
    void testAddMigrations_Multiple() {
        DatabaseConfig config = createUniqueTestConfig();
        DatabaseMigrator migrator = new DatabaseMigrator(config);

        DatabaseMigrator.Migration m1 = DatabaseMigrator.Migration.builder()
            .version("V1").description("Test1").sql("SELECT 1").build();

        DatabaseMigrator.Migration m2 = DatabaseMigrator.Migration.builder()
            .version("V2").description("Test2").sql("SELECT 2").build();

        migrator.addMigrations(List.of(m1, m2));
        // If no exception, test passed
    }

    @Test
    @DisplayName("addMigrations() should throw for null list")
    void testAddMigrations_NullList() {
        DatabaseConfig config = createUniqueTestConfig();
        DatabaseMigrator migrator = new DatabaseMigrator(config);

        assertThrows(IllegalArgumentException.class, () ->
            migrator.addMigrations(null)
        );
    }

    // ============================================================================
    // migrate() Tests - Success Cases
    // ============================================================================

    @Test
    @DisplayName("migrate() should execute single migration successfully")
    void testMigrate_SingleMigration() {
        DatabaseConfig config = createUniqueTestConfig();
        DatabaseMigrator migrator = new DatabaseMigrator(config);

        migrator.addMigration(DatabaseMigrator.Migration.builder()
            .version("V1")
            .description("Create test table")
            .sql("CREATE TABLE test_table (id INTEGER PRIMARY KEY, name TEXT)")
            .build());

        DatabaseMigrator.MigrationResult result = migrator.migrate();

        assertTrue(result.isSuccess(), () -> "Migration should succeed but failed with: " + result.getErrorMessage());
        assertEquals(1, result.getMigrationsApplied());
        assertEquals(List.of("V1"), result.getAppliedVersions());
        assertNull(result.getErrorMessage());
        assertTrue(result.getDurationMs() >= 0);
    }

    @Test
    @DisplayName("migrate() should execute multiple migrations in order")
    void testMigrate_MultipleMigrations() {
        DatabaseConfig config = createUniqueTestConfig();
        DatabaseMigrator migrator = new DatabaseMigrator(config);

        migrator.addMigration(DatabaseMigrator.Migration.builder()
            .version("V1")
            .description("Create table")
            .sql("CREATE TABLE test_table (id INTEGER PRIMARY KEY)")
            .build());

        migrator.addMigration(DatabaseMigrator.Migration.builder()
            .version("V2")
            .description("Add column")
            .sql("ALTER TABLE test_table ADD COLUMN name TEXT")
            .build());

        migrator.addMigration(DatabaseMigrator.Migration.builder()
            .version("V3")
            .description("Add another column")
            .sql("ALTER TABLE test_table ADD COLUMN email TEXT")
            .build());

        DatabaseMigrator.MigrationResult result = migrator.migrate();

        assertTrue(result.isSuccess());
        assertEquals(3, result.getMigrationsApplied());
        assertEquals(List.of("V1", "V2", "V3"), result.getAppliedVersions());
    }

    @Test
    @DisplayName("migrate() should sort migrations by version")
    void testMigrate_SortsByVersion() {
        DatabaseConfig config = createUniqueTestConfig();
        DatabaseMigrator migrator = new DatabaseMigrator(config);

        // Add out of order
        migrator.addMigration(DatabaseMigrator.Migration.builder()
            .version("V3")
            .description("Third")
            .sql("CREATE TABLE test_third (id INTEGER)")
            .build());

        migrator.addMigration(DatabaseMigrator.Migration.builder()
            .version("V1")
            .description("First")
            .sql("CREATE TABLE test_first (id INTEGER)")
            .build());

        migrator.addMigration(DatabaseMigrator.Migration.builder()
            .version("V2")
            .description("Second")
            .sql("CREATE TABLE test_second (id INTEGER)")
            .build());

        DatabaseMigrator.MigrationResult result = migrator.migrate();

        assertTrue(result.isSuccess(), () ->
            "Migration should succeed but failed with: " + result.getErrorMessage() +
            " | Applied: " + result.getAppliedVersions() +
            " | Migrations applied: " + result.getMigrationsApplied());
        assertEquals(List.of("V1", "V2", "V3"), result.getAppliedVersions(),
            "Migrations should be applied in version order");
    }

    @Test
    @DisplayName("migrate() should skip already-applied migrations")
    void testMigrate_SkipsApplied() {
        DatabaseConfig config = createUniqueTestConfig();

        // First migration
        DatabaseMigrator migrator1 = new DatabaseMigrator(config);
        migrator1.addMigration(DatabaseMigrator.Migration.builder()
            .version("V1")
            .description("Create table")
            .sql("CREATE TABLE test_table (id INTEGER)")
            .build());
        migrator1.migrate();

        // Second migration - should skip V1
        DatabaseMigrator migrator2 = new DatabaseMigrator(config);
        migrator2.addMigration(DatabaseMigrator.Migration.builder()
            .version("V1")
            .description("Create table")
            .sql("CREATE TABLE test_table (id INTEGER)")
            .build());
        migrator2.addMigration(DatabaseMigrator.Migration.builder()
            .version("V2")
            .description("Add column")
            .sql("ALTER TABLE test_table ADD COLUMN name TEXT")
            .build());

        DatabaseMigrator.MigrationResult result = migrator2.migrate();

        assertTrue(result.isSuccess());
        assertEquals(1, result.getMigrationsApplied(), "Should only apply V2");
        assertEquals(List.of("V2"), result.getAppliedVersions());
    }

    @Test
    @DisplayName("migrate() should create schema history table automatically")
    void testMigrate_CreatesSchemaTable() throws Exception {
        DatabaseConfig config = createUniqueTestConfig();
        DatabaseMigrator migrator = new DatabaseMigrator(config);

        migrator.addMigration(DatabaseMigrator.Migration.builder()
            .version("V1")
            .description("Test")
            .sql("CREATE TABLE test (id INTEGER)")
            .build());

        migrator.migrate();

        // Verify schema table exists (using H2 metadata)
        try (Connection conn = DriverManager.getConnection(config.getDatabaseUrl(), config.getUsername(), config.getPassword());
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME='SCHEMA_VERSION'")) {

            assertTrue(rs.next(), "Schema history table should exist");
        }
    }

    @Test
    @DisplayName("migrate() with no migrations should succeed")
    void testMigrate_NoMigrations() {
        DatabaseConfig config = createUniqueTestConfig();
        DatabaseMigrator migrator = new DatabaseMigrator(config);

        DatabaseMigrator.MigrationResult result = migrator.migrate();

        assertTrue(result.isSuccess());
        assertEquals(0, result.getMigrationsApplied());
        assertTrue(result.getAppliedVersions().isEmpty());
    }

    // ============================================================================
    // migrate() Tests - Failure Cases
    // ============================================================================

    @Test
    @DisplayName("migrate() should fail for invalid SQL")
    void testMigrate_InvalidSql() {
        DatabaseConfig config = createUniqueTestConfig();
        DatabaseMigrator migrator = new DatabaseMigrator(config);

        migrator.addMigration(DatabaseMigrator.Migration.builder()
            .version("V1")
            .description("Invalid SQL")
            .sql("CREATE INVALID SYNTAX HERE")
            .build());

        DatabaseMigrator.MigrationResult result = migrator.migrate();

        assertFalse(result.isSuccess(), "Migration should fail");
        assertTrue(result.isFailed());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("V1"), "Error should mention version");
    }

    @Test
    @DisplayName("migrate() should stop on first error")
    void testMigrate_StopsOnError() {
        DatabaseConfig config = createUniqueTestConfig();
        DatabaseMigrator migrator = new DatabaseMigrator(config);

        migrator.addMigration(DatabaseMigrator.Migration.builder()
            .version("V1")
            .description("Valid")
            .sql("CREATE TABLE test1 (id INTEGER)")
            .build());

        migrator.addMigration(DatabaseMigrator.Migration.builder()
            .version("V2")
            .description("Invalid")
            .sql("INVALID SQL")
            .build());

        migrator.addMigration(DatabaseMigrator.Migration.builder()
            .version("V3")
            .description("Should not execute")
            .sql("CREATE TABLE test3 (id INTEGER)")
            .build());

        DatabaseMigrator.MigrationResult result = migrator.migrate();

        assertFalse(result.isSuccess());
        assertEquals(1, result.getMigrationsApplied(), "Should only apply V1 before failure");
        assertEquals(List.of("V1"), result.getAppliedVersions());
    }

    @Test
    @DisplayName("migrate() should detect checksum mismatch")
    void testMigrate_ChecksumMismatch() throws Exception {
        DatabaseConfig config = createUniqueTestConfig();

        // First migration
        DatabaseMigrator migrator1 = new DatabaseMigrator(config);
        migrator1.addMigration(DatabaseMigrator.Migration.builder()
            .version("V1")
            .description("Original")
            .sql("CREATE TABLE test (id INTEGER)")
            .build());
        migrator1.migrate();

        // Try to re-apply with different SQL (checksum mismatch)
        DatabaseMigrator migrator2 = new DatabaseMigrator(config);
        migrator2.addMigration(DatabaseMigrator.Migration.builder()
            .version("V1")
            .description("Original")
            .sql("CREATE TABLE test (id INTEGER, name TEXT)")  // Modified SQL
            .build());

        DatabaseMigrator.MigrationResult result = migrator2.migrate();

        assertFalse(result.isSuccess(), "Should fail due to checksum mismatch");
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("checksum") ||
                  result.getErrorMessage().contains("modified"),
            "Error should mention checksum mismatch");
    }

    // ============================================================================
    // getCurrentVersion() Tests
    // ============================================================================

    @Test
    @DisplayName("getCurrentVersion() should return null for fresh database")
    void testGetCurrentVersion_FreshDatabase() {
        DatabaseConfig config = createUniqueTestConfig();
        DatabaseMigrator migrator = new DatabaseMigrator(config);

        String version = migrator.getCurrentVersion();
        assertNull(version, "Fresh database should have no version");
    }

    @Test
    @DisplayName("getCurrentVersion() should return latest version")
    void testGetCurrentVersion_AfterMigrations() {
        DatabaseConfig config = createUniqueTestConfig();
        DatabaseMigrator migrator = new DatabaseMigrator(config);

        migrator.addMigration(DatabaseMigrator.Migration.builder()
            .version("V1").description("First").sql("CREATE TABLE t1 (id INTEGER)").build());
        migrator.addMigration(DatabaseMigrator.Migration.builder()
            .version("V2").description("Second").sql("CREATE TABLE t2 (id INTEGER)").build());
        migrator.addMigration(DatabaseMigrator.Migration.builder()
            .version("V3").description("Third").sql("CREATE TABLE t3 (id INTEGER)").build());

        migrator.migrate();

        String version = migrator.getCurrentVersion();
        assertEquals("V3", version, "Should return latest applied version");
    }

    @Test
    @DisplayName("getCurrentVersion() should return last successful version after failure")
    void testGetCurrentVersion_AfterFailure() {
        DatabaseConfig config = createUniqueTestConfig();
        DatabaseMigrator migrator = new DatabaseMigrator(config);

        migrator.addMigration(DatabaseMigrator.Migration.builder()
            .version("V1").description("Valid").sql("CREATE TABLE t1 (id INTEGER)").build());
        migrator.addMigration(DatabaseMigrator.Migration.builder()
            .version("V2").description("Invalid").sql("INVALID SQL").build());

        migrator.migrate();  // Will fail on V2

        String version = migrator.getCurrentVersion();
        assertEquals("V1", version, "Should return last successful version");
    }

    // ============================================================================
    // getHistory() Tests
    // ============================================================================

    @Test
    @DisplayName("getHistory() should return empty list for fresh database")
    void testGetHistory_FreshDatabase() {
        DatabaseConfig config = createUniqueTestConfig();
        DatabaseMigrator migrator = new DatabaseMigrator(config);

        List<DatabaseMigrator.MigrationHistoryEntry> history = migrator.getHistory();
        assertNotNull(history);
        assertTrue(history.isEmpty());
    }

    @Test
    @DisplayName("getHistory() should return all applied migrations")
    void testGetHistory_AfterMigrations() {
        DatabaseConfig config = createUniqueTestConfig();
        DatabaseMigrator migrator = new DatabaseMigrator(config);

        migrator.addMigration(DatabaseMigrator.Migration.builder()
            .version("V1").description("First migration").sql("CREATE TABLE t1 (id INTEGER)").build());
        migrator.addMigration(DatabaseMigrator.Migration.builder()
            .version("V2").description("Second migration").sql("CREATE TABLE t2 (id INTEGER)").build());

        migrator.migrate();

        List<DatabaseMigrator.MigrationHistoryEntry> history = migrator.getHistory();
        assertEquals(2, history.size());

        DatabaseMigrator.MigrationHistoryEntry entry1 = history.get(0);
        assertEquals("V1", entry1.getVersion());
        assertEquals("First migration", entry1.getDescription());
        assertTrue(entry1.isSuccess());
        assertEquals("Success", entry1.getState());

        DatabaseMigrator.MigrationHistoryEntry entry2 = history.get(1);
        assertEquals("V2", entry2.getVersion());
        assertEquals("Second migration", entry2.getDescription());
        assertTrue(entry2.isSuccess());
    }

    @Test
    @DisplayName("getHistory() should include failed migrations")
    void testGetHistory_IncludesFailures() {
        DatabaseConfig config = createUniqueTestConfig();
        DatabaseMigrator migrator = new DatabaseMigrator(config);

        migrator.addMigration(DatabaseMigrator.Migration.builder()
            .version("V1").description("Valid").sql("CREATE TABLE t1 (id INTEGER)").build());
        migrator.addMigration(DatabaseMigrator.Migration.builder()
            .version("V2").description("Invalid").sql("INVALID SQL").build());

        migrator.migrate();  // Will fail on V2

        List<DatabaseMigrator.MigrationHistoryEntry> history = migrator.getHistory();
        assertEquals(2, history.size(), "History should include failed migration");

        DatabaseMigrator.MigrationHistoryEntry successEntry = history.get(0);
        assertTrue(successEntry.isSuccess());
        assertEquals("Success", successEntry.getState());

        DatabaseMigrator.MigrationHistoryEntry failedEntry = history.get(1);
        assertFalse(failedEntry.isSuccess());
        assertEquals("Failed", failedEntry.getState());
    }

    @Test
    @DisplayName("MigrationHistoryEntry should have all required fields")
    void testMigrationHistoryEntry_Fields() {
        DatabaseConfig config = createUniqueTestConfig();
        DatabaseMigrator migrator = new DatabaseMigrator(config);

        migrator.addMigration(DatabaseMigrator.Migration.builder()
            .version("V1").description("Test migration").sql("CREATE TABLE t (id INTEGER)").build());

        migrator.migrate();

        List<DatabaseMigrator.MigrationHistoryEntry> history = migrator.getHistory();
        DatabaseMigrator.MigrationHistoryEntry entry = history.get(0);

        assertNotNull(entry.getVersion());
        assertNotNull(entry.getDescription());
        assertNotNull(entry.getType());
        assertNotNull(entry.getInstalledBy());
        assertNotNull(entry.getInstalledOn());
        assertTrue(entry.getExecutionTime() >= 0);
        assertNotNull(entry.getState());
        assertNotNull(entry.toString());
    }

    // ============================================================================
    // validate() Tests
    // ============================================================================

    @Test
    @DisplayName("validate() should pass for fresh database")
    void testValidate_FreshDatabase() {
        DatabaseConfig config = createUniqueTestConfig();
        DatabaseMigrator migrator = new DatabaseMigrator(config);

        DatabaseMigrator.ValidationResult result = migrator.validate();

        assertTrue(result.isValid());
        assertNotNull(result.getMessage());
        assertFalse(result.hasIssues());
        assertTrue(result.getIssues().isEmpty());
    }

    @Test
    @DisplayName("validate() should pass for valid migrations")
    void testValidate_ValidMigrations() {
        DatabaseConfig config = createUniqueTestConfig();
        DatabaseMigrator migrator = new DatabaseMigrator(config);

        migrator.addMigration(DatabaseMigrator.Migration.builder()
            .version("V1").description("Test").sql("CREATE TABLE t1 (id INTEGER)").build());
        migrator.addMigration(DatabaseMigrator.Migration.builder()
            .version("V2").description("Test").sql("CREATE TABLE t2 (id INTEGER)").build());

        migrator.migrate();

        DatabaseMigrator.ValidationResult result = migrator.validate();

        assertTrue(result.isValid());
        assertFalse(result.hasIssues());
    }

    @Test
    @DisplayName("validate() should detect missing migrations")
    void testValidate_MissingMigrations() {
        DatabaseConfig config = createUniqueTestConfig();

        // Apply migration V1
        DatabaseMigrator migrator1 = new DatabaseMigrator(config);
        migrator1.addMigration(DatabaseMigrator.Migration.builder()
            .version("V1").description("Test").sql("CREATE TABLE t1 (id INTEGER)").build());
        migrator1.migrate();

        // Validate without registering V1
        DatabaseMigrator migrator2 = new DatabaseMigrator(config);
        DatabaseMigrator.ValidationResult result = migrator2.validate();

        assertFalse(result.isValid(), "Should fail when applied migration is missing");
        assertTrue(result.hasIssues());
        assertFalse(result.getIssues().isEmpty());
    }

    @Test
    @DisplayName("validate() should detect checksum mismatches")
    void testValidate_ChecksumMismatch() {
        DatabaseConfig config = createUniqueTestConfig();

        // Apply original migration
        DatabaseMigrator migrator1 = new DatabaseMigrator(config);
        migrator1.addMigration(DatabaseMigrator.Migration.builder()
            .version("V1").description("Test").sql("CREATE TABLE t1 (id INTEGER)").build());
        migrator1.migrate();

        // Validate with modified SQL
        DatabaseMigrator migrator2 = new DatabaseMigrator(config);
        migrator2.addMigration(DatabaseMigrator.Migration.builder()
            .version("V1").description("Test").sql("CREATE TABLE t1 (id INTEGER, name TEXT)").build());

        DatabaseMigrator.ValidationResult result = migrator2.validate();

        assertFalse(result.isValid(), "Should detect checksum mismatch");
        assertTrue(result.hasIssues());
        assertTrue(result.getIssues().stream().anyMatch(msg -> msg.contains("checksum") || msg.contains("mismatch")));
    }

    // ============================================================================
    // Custom Schema Table Tests
    // ============================================================================

    @Test
    @DisplayName("Custom schema table name should be used")
    void testCustomSchemaTable() throws Exception {
        DatabaseConfig config = createUniqueTestConfig();
        String customTableName = "my_custom_schema_history";

        DatabaseMigrator migrator = new DatabaseMigrator(config, customTableName, false);
        migrator.addMigration(DatabaseMigrator.Migration.builder()
            .version("V1").description("Test").sql("CREATE TABLE t1 (id INTEGER)").build());

        migrator.migrate();

        // Verify custom table exists (using H2 metadata)
        try (Connection conn = DriverManager.getConnection(config.getDatabaseUrl(), config.getUsername(), config.getPassword());
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE UPPER(TABLE_NAME)='" +
                 customTableName.toUpperCase() + "'")) {

            assertTrue(rs.next(), "Custom schema table should exist");
        }
    }

    // ============================================================================
    // Edge Cases and Integration Tests
    // ============================================================================

    @Test
    @DisplayName("Should handle migrations with special characters in SQL")
    void testMigrate_SpecialCharacters() {
        DatabaseConfig config = createUniqueTestConfig();
        DatabaseMigrator migrator = new DatabaseMigrator(config);

        migrator.addMigration(DatabaseMigrator.Migration.builder()
            .version("V1")
            .description("Create table with quotes")
            .sql("CREATE TABLE \"test-table\" (id INTEGER, \"user-name\" TEXT)")
            .build());

        DatabaseMigrator.MigrationResult result = migrator.migrate();
        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("MigrationResult should provide complete information")
    void testMigrationResult_CompleteInfo() {
        DatabaseConfig config = createUniqueTestConfig();
        DatabaseMigrator migrator = new DatabaseMigrator(config);

        migrator.addMigration(DatabaseMigrator.Migration.builder()
            .version("V1").description("Test").sql("CREATE TABLE t (id INTEGER)").build());

        DatabaseMigrator.MigrationResult result = migrator.migrate();

        assertNotNull(result.toString());
        assertNotNull(result.getStartTime());
        assertTrue(result.getDurationMs() >= 0);
        assertNotNull(result.getAppliedVersions());
    }

    @Test
    @DisplayName("ValidationResult should provide complete information")
    void testValidationResult_CompleteInfo() {
        DatabaseConfig config = createUniqueTestConfig();
        DatabaseMigrator migrator = new DatabaseMigrator(config);

        DatabaseMigrator.ValidationResult result = migrator.validate();

        assertNotNull(result.toString());
        assertNotNull(result.getMessage());
        assertNotNull(result.getIssues());
    }

    @Test
    @DisplayName("Integration: Complete migration workflow")
    void testIntegration_FullWorkflow() {
        DatabaseConfig config = createUniqueTestConfig();
        DatabaseMigrator migrator = new DatabaseMigrator(config);

        // Check initial state
        assertNull(migrator.getCurrentVersion());
        assertTrue(migrator.getHistory().isEmpty());

        // Add migrations
        migrator.addMigration(DatabaseMigrator.Migration.builder()
            .version("V1").description("Create users").sql("CREATE TABLE users (id INTEGER PRIMARY KEY)").build());
        migrator.addMigration(DatabaseMigrator.Migration.builder()
            .version("V2").description("Add name column").sql("ALTER TABLE users ADD COLUMN name TEXT").build());
        migrator.addMigration(DatabaseMigrator.Migration.builder()
            .version("V3").description("Create posts").sql("CREATE TABLE posts (id INTEGER PRIMARY KEY)").build());

        // Execute migrations
        DatabaseMigrator.MigrationResult result = migrator.migrate();
        assertTrue(result.isSuccess());
        assertEquals(3, result.getMigrationsApplied());

        // Check current version
        assertEquals("V3", migrator.getCurrentVersion());

        // Check history
        List<DatabaseMigrator.MigrationHistoryEntry> history = migrator.getHistory();
        assertEquals(3, history.size());

        // Validate
        DatabaseMigrator.ValidationResult validation = migrator.validate();
        assertTrue(validation.isValid());

        // Try to migrate again (should skip all)
        DatabaseMigrator.MigrationResult result2 = migrator.migrate();
        assertTrue(result2.isSuccess());
        assertEquals(0, result2.getMigrationsApplied(), "Should skip already-applied migrations");
    }
}
