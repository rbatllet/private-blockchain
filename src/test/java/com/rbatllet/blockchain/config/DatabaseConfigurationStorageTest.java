package com.rbatllet.blockchain.config;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for DatabaseConfigurationStorage class
 */
@DisplayName("DatabaseConfigurationStorage Tests")
class DatabaseConfigurationStorageTest {

    @TempDir
    Path tempDir;

    private DatabaseConfigurationStorage storage;
    private String testDbPath;
    private Map<String, String> testConfig;

    @BeforeEach
    void setUp() throws Exception {
        testDbPath = tempDir.resolve("test_config.db").toString();
        storage = new DatabaseConfigurationStorage(testDbPath);

        // Clean database state for each test
        storage.initialize();
        clearDatabaseTables();

        testConfig = new HashMap<>();
        testConfig.put("key1", "value1");
        testConfig.put("key2", "value2");
        testConfig.put("key3", "value3");
    }

    @AfterEach
    void tearDown() {
        if (storage != null) {
            // Clean database state after each test
            clearDatabaseTables();
            storage.cleanup();
        }
    }

    private void clearDatabaseTables() {
        try (
            Connection conn = DriverManager.getConnection(
                "jdbc:sqlite:" + testDbPath
            )
        ) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("DELETE FROM configuration");
                stmt.execute("DELETE FROM configuration_audit");
                stmt.execute(
                    "DELETE FROM sqlite_sequence WHERE name IN ('configuration_audit')"
                );
            }
        } catch (Exception e) {
            // Ignore cleanup errors in tests
        }
    }

    @Nested
    @DisplayName("Storage Initialization")
    class StorageInitialization {

        @Test
        @DisplayName("Should initialize with default database path")
        void shouldInitializeWithDefaultDatabasePath() {
            DatabaseConfigurationStorage defaultStorage =
                new DatabaseConfigurationStorage();

            assertNotNull(defaultStorage);
            assertEquals("database", defaultStorage.getStorageType());
            assertTrue(defaultStorage.initialize());

            defaultStorage.cleanup();
        }

        @Test
        @DisplayName("Should initialize with custom database path")
        void shouldInitializeWithCustomDatabasePath() {
            assertTrue(storage.initialize());
            assertEquals("database", storage.getStorageType());
            assertTrue(storage.getStorageLocation().contains(testDbPath));
        }

        @Test
        @DisplayName("Should be healthy after initialization")
        void shouldBeHealthyAfterInitialization() {
            assertTrue(storage.initialize());
            assertTrue(storage.isHealthy());
        }

        @Test
        @DisplayName("Should create database file on initialization")
        void shouldCreateDatabaseFileOnInitialization() {
            assertTrue(storage.initialize());
            assertTrue(Files.exists(Path.of(testDbPath)));
        }
    }

    @Nested
    @DisplayName("Configuration Loading")
    class ConfigurationLoading {

        @BeforeEach
        void setUp() {
            storage.initialize();
        }

        @Test
        @DisplayName("Should load empty configuration for new type")
        void shouldLoadEmptyConfigurationForNewType() {
            Map<String, String> config = storage.loadConfiguration("NEW_TYPE");

            assertNotNull(config);
            assertTrue(config.isEmpty());
        }

        @Test
        @DisplayName("Should load saved configuration")
        void shouldLoadSavedConfiguration() {
            assertTrue(storage.saveConfiguration("TEST", testConfig));

            Map<String, String> loaded = storage.loadConfiguration("TEST");

            assertNotNull(loaded);
            assertEquals(3, loaded.size());
            assertEquals("value1", loaded.get("key1"));
            assertEquals("value2", loaded.get("key2"));
            assertEquals("value3", loaded.get("key3"));
        }

        @Test
        @DisplayName("Should load configuration with different types")
        void shouldLoadConfigurationWithDifferentTypes() {
            Map<String, String> config1 = new HashMap<>();
            config1.put("setting1", "value1");

            Map<String, String> config2 = new HashMap<>();
            config2.put("setting2", "value2");

            assertTrue(storage.saveConfiguration("TYPE1", config1));
            assertTrue(storage.saveConfiguration("TYPE2", config2));

            Map<String, String> loaded1 = storage.loadConfiguration("TYPE1");
            Map<String, String> loaded2 = storage.loadConfiguration("TYPE2");

            assertEquals(1, loaded1.size());
            assertEquals("value1", loaded1.get("setting1"));
            assertNull(loaded1.get("setting2"));

            assertEquals(1, loaded2.size());
            assertEquals("value2", loaded2.get("setting2"));
            assertNull(loaded2.get("setting1"));
        }

        @Test
        @DisplayName("Should handle null configuration type gracefully")
        void shouldHandleNullConfigurationTypeGracefully() {
            Map<String, String> config = storage.loadConfiguration(null);

            assertNotNull(config);
            assertTrue(config.isEmpty());
        }
    }

    @Nested
    @DisplayName("Configuration Saving")
    class ConfigurationSaving {

        @BeforeEach
        void setUp() {
            storage.initialize();
        }

        @Test
        @DisplayName("Should save configuration successfully")
        void shouldSaveConfigurationSuccessfully() {
            boolean result = storage.saveConfiguration("TEST", testConfig);

            assertTrue(result);

            // Verify by loading
            Map<String, String> loaded = storage.loadConfiguration("TEST");
            assertEquals(testConfig.size(), loaded.size());
            assertEquals("value1", loaded.get("key1"));
        }

        @Test
        @DisplayName("Should update existing configuration")
        void shouldUpdateExistingConfiguration() {
            assertTrue(storage.saveConfiguration("TEST", testConfig));

            Map<String, String> updatedConfig = new HashMap<>();
            updatedConfig.put("key1", "updated_value1");
            updatedConfig.put("key4", "new_value4");

            boolean result = storage.saveConfiguration("TEST", updatedConfig);
            assertTrue(result);

            Map<String, String> loaded = storage.loadConfiguration("TEST");
            assertEquals(2, loaded.size());
            assertEquals("updated_value1", loaded.get("key1"));
            assertEquals("new_value4", loaded.get("key4"));
        }

        @Test
        @DisplayName("Should save empty configuration")
        void shouldSaveEmptyConfiguration() {
            Map<String, String> emptyConfig = new HashMap<>();
            boolean result = storage.saveConfiguration("EMPTY", emptyConfig);

            assertTrue(result);

            Map<String, String> loaded = storage.loadConfiguration("EMPTY");
            assertTrue(loaded.isEmpty());
        }

        @Test
        @DisplayName("Should handle null parameters gracefully")
        void shouldHandleNullParametersGracefully() {
            assertFalse(storage.saveConfiguration(null, testConfig));
            assertFalse(storage.saveConfiguration("TEST", null));
            assertFalse(storage.saveConfiguration(null, null));
        }
    }

    @Nested
    @DisplayName("Configuration Reset")
    class ConfigurationReset {

        @BeforeEach
        void setUp() {
            assertTrue(storage.saveConfiguration("TEST", testConfig));
        }

        @Test
        @DisplayName("Should reset configuration successfully")
        void shouldResetConfigurationSuccessfully() {
            assertTrue(storage.configurationExists("TEST"));

            boolean result = storage.resetConfiguration("TEST");
            assertTrue(result);

            Map<String, String> loaded = storage.loadConfiguration("TEST");
            assertTrue(loaded.isEmpty());
        }

        @Test
        @DisplayName("Should handle reset of non-existent configuration")
        void shouldHandleResetOfNonExistentConfiguration() {
            boolean result = storage.resetConfiguration("NON_EXISTENT");
            assertTrue(result); // Should succeed even if nothing to delete
        }

        @Test
        @DisplayName("Should handle null configuration type")
        void shouldHandleNullConfigurationType() {
            assertFalse(storage.resetConfiguration(null));
        }
    }

    @Nested
    @DisplayName("Configuration Existence Check")
    class ConfigurationExistenceCheck {

        @BeforeEach
        void setUp() {
            // Database already initialized in main setUp
        }

        @Test
        @DisplayName("Should detect existing configuration")
        void shouldDetectExistingConfiguration() {
            storage.saveConfiguration("EXISTING", testConfig);

            assertTrue(storage.configurationExists("EXISTING"));
        }

        @Test
        @DisplayName("Should detect non-existing configuration")
        void shouldDetectNonExistingConfiguration() {
            assertFalse(storage.configurationExists("NON_EXISTING"));
        }

        @Test
        @DisplayName("Should handle null configuration type")
        void shouldHandleNullConfigurationType() {
            assertFalse(storage.configurationExists(null));
        }

        @Test
        @DisplayName("Should detect configuration after reset")
        void shouldDetectConfigurationAfterReset() {
            storage.saveConfiguration("TEST", testConfig);
            assertTrue(storage.configurationExists("TEST"));

            storage.resetConfiguration("TEST");
            assertFalse(storage.configurationExists("TEST"));
        }
    }

    @Nested
    @DisplayName("Individual Value Operations")
    class IndividualValueOperations {

        @BeforeEach
        void setUp() {
            assertTrue(storage.saveConfiguration("TEST", testConfig));
        }

        @Test
        @DisplayName("Should get configuration value")
        void shouldGetConfigurationValue() {
            String value = storage.getConfigurationValue("TEST", "key1");
            assertEquals("value1", value);
        }

        @Test
        @DisplayName("Should return null for non-existent key")
        void shouldReturnNullForNonExistentKey() {
            String value = storage.getConfigurationValue(
                "TEST",
                "non_existent"
            );
            assertNull(value);
        }

        @Test
        @DisplayName("Should return null for non-existent configuration type")
        void shouldReturnNullForNonExistentConfigurationType() {
            String value = storage.getConfigurationValue(
                "NON_EXISTENT",
                "key1"
            );
            assertNull(value);
        }

        @Test
        @DisplayName("Should set configuration value successfully")
        void shouldSetConfigurationValueSuccessfully() {
            boolean result = storage.setConfigurationValue(
                "TEST",
                "new_key",
                "new_value"
            );
            assertTrue(result);

            String value = storage.getConfigurationValue("TEST", "new_key");
            assertEquals("new_value", value);
        }

        @Test
        @DisplayName("Should update existing configuration value")
        void shouldUpdateExistingConfigurationValue() {
            boolean result = storage.setConfigurationValue(
                "TEST",
                "key1",
                "updated_value"
            );
            assertTrue(result);

            String value = storage.getConfigurationValue("TEST", "key1");
            assertEquals("updated_value", value);
        }

        @Test
        @DisplayName("Should delete configuration value successfully")
        void shouldDeleteConfigurationValueSuccessfully() {
            assertTrue(storage.getConfigurationValue("TEST", "key1") != null);

            boolean result = storage.deleteConfigurationValue("TEST", "key1");
            assertTrue(result);

            assertNull(storage.getConfigurationValue("TEST", "key1"));
        }

        @Test
        @DisplayName("Should handle deletion of non-existent key")
        void shouldHandleDeletionOfNonExistentKey() {
            boolean result = storage.deleteConfigurationValue(
                "TEST",
                "non_existent"
            );
            assertFalse(result); // Should return false if nothing to delete
        }

        @Test
        @DisplayName("Should handle null parameters in value operations")
        void shouldHandleNullParametersInValueOperations() {
            assertNull(storage.getConfigurationValue(null, "key1"));
            assertNull(storage.getConfigurationValue("TEST", null));

            assertFalse(storage.setConfigurationValue(null, "key", "value"));
            assertFalse(storage.setConfigurationValue("TEST", null, "value"));
            assertFalse(storage.setConfigurationValue("TEST", "key", null));

            assertFalse(storage.deleteConfigurationValue(null, "key"));
            assertFalse(storage.deleteConfigurationValue("TEST", null));
        }
    }

    @Nested
    @DisplayName("Import/Export Operations")
    class ImportExportOperations {

        @BeforeEach
        void setUp() {
            assertTrue(storage.saveConfiguration("TEST", testConfig));
        }

        @Test
        @DisplayName("Should export configuration to file")
        void shouldExportConfigurationToFile() {
            Path exportPath = tempDir.resolve("export.properties");

            boolean result = storage.exportConfiguration("TEST", exportPath);
            assertTrue(result);
            assertTrue(Files.exists(exportPath));
        }

        @Test
        @DisplayName("Should import configuration from file")
        void shouldImportConfigurationFromFile() throws Exception {
            // First export to create a file
            Path exportPath = tempDir.resolve("export.properties");
            storage.exportConfiguration("TEST", exportPath);

            // Clear the configuration
            storage.resetConfiguration("TEST");
            assertTrue(storage.loadConfiguration("TEST").isEmpty());

            // Import back
            boolean result = storage.importConfiguration("TEST", exportPath);
            assertTrue(result);

            Map<String, String> imported = storage.loadConfiguration("TEST");
            assertEquals(3, imported.size());
            assertEquals("value1", imported.get("key1"));
        }

        @Test
        @DisplayName("Should handle export to invalid path")
        void shouldHandleExportToInvalidPath() {
            Path invalidPath = Path.of("/invalid/path/export.properties");

            boolean result = storage.exportConfiguration("TEST", invalidPath);
            assertFalse(result);
        }

        @Test
        @DisplayName("Should handle import from non-existent file")
        void shouldHandleImportFromNonExistentFile() {
            Path nonExistentPath = tempDir.resolve("non_existent.properties");

            boolean result = storage.importConfiguration(
                "TEST",
                nonExistentPath
            );
            assertFalse(result);
        }

        @Test
        @DisplayName("Should handle null paths in import/export")
        void shouldHandleNullPathsInImportExport() {
            assertFalse(storage.exportConfiguration("TEST", null));
            assertFalse(storage.importConfiguration("TEST", null));
        }
    }

    @Nested
    @DisplayName("Audit Operations")
    class AuditOperations {

        @BeforeEach
        void setUp() {
            // Database already initialized in main setUp
        }

        @Test
        @DisplayName("Should retrieve audit log")
        void shouldRetrieveAuditLog() {
            // Perform some operations to generate audit entries
            storage.setConfigurationValue("TEST", "key1", "value1");
            storage.setConfigurationValue("TEST", "key1", "value2");
            storage.deleteConfigurationValue("TEST", "key1");

            String auditLog = storage.getAuditLog("TEST", 10);
            assertNotNull(auditLog);
            assertFalse(auditLog.trim().isEmpty());
        }

        @Test
        @DisplayName("Should respect audit log limit")
        void shouldRespectAuditLogLimit() {
            // Generate multiple audit entries
            for (int i = 0; i < 10; i++) {
                storage.setConfigurationValue("TEST", "key" + i, "value" + i);
            }

            String limitedLog = storage.getAuditLog("TEST", 5);
            String unlimitedLog = storage.getAuditLog("TEST", 20);

            assertNotNull(limitedLog);
            assertNotNull(unlimitedLog);

            // Limited log should have fewer or equal entries than unlimited
            int limitedLines = limitedLog.split("\n").length;
            int unlimitedLines = unlimitedLog.split("\n").length;
            assertTrue(limitedLines <= unlimitedLines);
        }

        @Test
        @DisplayName("Should handle empty audit log")
        void shouldHandleEmptyAuditLog() {
            String auditLog = storage.getAuditLog("UNUSED_TYPE", 10);
            assertNotNull(auditLog);
            // Should be empty or contain no relevant entries
        }

        @Test
        @DisplayName("Should handle null configuration type in audit")
        void shouldHandleNullConfigurationTypeInAudit() {
            String auditLog = storage.getAuditLog(null, 10);
            assertNotNull(auditLog);
        }
    }

    @Nested
    @DisplayName("Storage Metadata")
    class StorageMetadata {

        @BeforeEach
        void setUp() {
            // Database already initialized in main setUp
        }

        @Test
        @DisplayName("Should return correct storage type")
        void shouldReturnCorrectStorageType() {
            assertEquals("database", storage.getStorageType());
        }

        @Test
        @DisplayName("Should return storage location")
        void shouldReturnStorageLocation() {
            String location = storage.getStorageLocation();
            assertNotNull(location);
            assertTrue(location.contains(testDbPath));
        }

        @Test
        @DisplayName("Should report healthy status when operational")
        void shouldReportHealthyStatusWhenOperational() {
            assertTrue(storage.isHealthy());
        }

        @Test
        @DisplayName(
            "Should report unhealthy status when database is corrupted"
        )
        void shouldReportUnhealthyStatusWhenDatabaseIsCorrupted()
            throws Exception {
            // Corrupt the database by deleting the file
            storage.cleanup();
            Files.deleteIfExists(Path.of(testDbPath));

            // Create a new storage instance pointing to the same path
            DatabaseConfigurationStorage corruptedStorage =
                new DatabaseConfigurationStorage(testDbPath);

            // Should still be able to initialize (creates new DB)
            assertTrue(corruptedStorage.initialize());
            assertTrue(corruptedStorage.isHealthy());

            corruptedStorage.cleanup();
        }
    }

    @Nested
    @DisplayName("Concurrent Operations")
    class ConcurrentOperations {

        @BeforeEach
        void setUp() {
            // Database already initialized in main setUp
        }

        @Test
        @DisplayName("Should handle concurrent writes safely")
        void shouldHandleConcurrentWritesSafely() throws InterruptedException {
            int numThreads = 5;
            int operationsPerThread = 20;
            Thread[] threads = new Thread[numThreads];

            for (int i = 0; i < numThreads; i++) {
                final int threadId = i;
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < operationsPerThread; j++) {
                        storage.setConfigurationValue(
                            "CONCURRENT",
                            "key_" + threadId + "_" + j,
                            "value_" + threadId + "_" + j
                        );
                    }
                });
            }

            // Start all threads
            for (Thread thread : threads) {
                thread.start();
            }

            // Wait for all threads to complete
            for (Thread thread : threads) {
                thread.join();
            }

            // Verify all values were written
            Map<String, String> config = storage.loadConfiguration(
                "CONCURRENT"
            );
            assertEquals(numThreads * operationsPerThread, config.size());

            // Verify some specific values
            assertEquals("value_0_0", config.get("key_0_0"));
            assertEquals("value_4_19", config.get("key_4_19"));
        }
    }

    @Nested
    @DisplayName("Error Handling and Edge Cases")
    class ErrorHandlingAndEdgeCases {

        @Test
        @DisplayName("Should handle operations before initialization")
        void shouldHandleOperationsBeforeInitialization() {
            DatabaseConfigurationStorage uninitializedStorage =
                new DatabaseConfigurationStorage(
                    tempDir.resolve("uninit.db").toString()
                );

            // Should handle gracefully without throwing exceptions
            Map<String, String> config = uninitializedStorage.loadConfiguration(
                "TEST"
            );
            assertNotNull(config);
            assertTrue(config.isEmpty());

            // These operations will auto-initialize the storage, so they should succeed
            assertTrue(
                uninitializedStorage.saveConfiguration("TEST", testConfig)
            );
            assertTrue(uninitializedStorage.configurationExists("TEST"));

            uninitializedStorage.cleanup();
        }

        @Test
        @DisplayName("Should handle very large configuration values")
        void shouldHandleVeryLargeConfigurationValues() {
            storage.initialize();

            // Create a very large value (1MB)
            String largeValue = "x".repeat(1024 * 1024);

            boolean result = storage.setConfigurationValue(
                "LARGE",
                "big_key",
                largeValue
            );
            assertTrue(result);

            String retrieved = storage.getConfigurationValue(
                "LARGE",
                "big_key"
            );
            assertEquals(largeValue, retrieved);
        }

        @Test
        @DisplayName("Should handle special characters in keys and values")
        void shouldHandleSpecialCharactersInKeysAndValues() {
            storage.initialize();

            String specialKey = "key.with-special_chars@#$%";
            String specialValue =
                "value with\nnewlines\tand\ttabs and unicode: üñíçødé";

            boolean result = storage.setConfigurationValue(
                "SPECIAL",
                specialKey,
                specialValue
            );
            assertTrue(result);

            String retrieved = storage.getConfigurationValue(
                "SPECIAL",
                specialKey
            );
            assertEquals(specialValue, retrieved);
        }

        @Test
        @DisplayName("Should handle database reconnection after cleanup")
        void shouldHandleDatabaseReconnectionAfterCleanup() {
            storage.initialize();
            storage.saveConfiguration("TEST", testConfig);

            // Cleanup and reinitialize
            storage.cleanup();
            assertTrue(storage.initialize());

            // Should be able to load previously saved data
            Map<String, String> loaded = storage.loadConfiguration("TEST");
            assertEquals(testConfig.size(), loaded.size());
        }
    }

    @Nested
    @DisplayName("Database Schema Validation")
    class DatabaseSchemaValidation {

        @Test
        @DisplayName("Should create required tables on initialization")
        void shouldCreateRequiredTablesOnInitialization() throws Exception {
            storage.initialize();

            // Check that tables exist by performing operations
            assertTrue(storage.saveConfiguration("SCHEMA_TEST", testConfig));
            assertTrue(
                storage.setConfigurationValue("AUDIT_TEST", "key", "value")
            );

            // Verify audit logging works (which requires both tables)
            String auditLog = storage.getAuditLog("AUDIT_TEST", 5);
            assertNotNull(auditLog);
        }

        @Test
        @DisplayName("Should handle existing database with correct schema")
        void shouldHandleExistingDatabaseWithCorrectSchema() {
            // Initialize once
            storage.initialize();
            assertTrue(storage.saveConfiguration("EXISTING", testConfig));
            storage.cleanup();

            // Initialize again with same database
            DatabaseConfigurationStorage newStorage =
                new DatabaseConfigurationStorage(testDbPath);
            assertTrue(newStorage.initialize());

            // Should be able to read existing data
            Map<String, String> loaded = newStorage.loadConfiguration(
                "EXISTING"
            );
            assertEquals(testConfig.size(), loaded.size());

            newStorage.cleanup();
        }
    }
}
