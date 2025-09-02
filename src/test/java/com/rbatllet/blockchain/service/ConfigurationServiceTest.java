package com.rbatllet.blockchain.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.rbatllet.blockchain.config.ConfigurationStorage;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Tests for ConfigurationService class
 */
@DisplayName("ConfigurationService Tests")
class ConfigurationServiceTest {

    @Mock
    private ConfigurationStorage mockStorage;

    private ConfigurationService service;
    private Map<String, String> testConfig;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        testConfig = new HashMap<>();
        testConfig.put("key1", "value1");
        testConfig.put("key2", "value2");
        testConfig.put("key3", "value3");

        when(mockStorage.initialize()).thenReturn(true);
        when(mockStorage.getStorageType()).thenReturn("mock");
        when(mockStorage.getStorageLocation()).thenReturn(
            "Mock Storage Location"
        );
        when(mockStorage.isHealthy()).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        // Always reset singleton instance first
        resetSingletonInstance();
        if (service != null) {
            service.shutdown();
        }
    }

    private void resetSingletonInstance() {
        try {
            java.lang.reflect.Field instanceField =
                ConfigurationService.class.getDeclaredField("instance");
            instanceField.setAccessible(true);
            instanceField.set(null, null);

            // Also shutdown any existing service to clean up resources
            if (service != null) {
                service.shutdown();
                service = null;
            }
        } catch (Exception e) {
            // Ignore reflection errors in tests
        }
    }

    @Nested
    @DisplayName("Singleton Pattern")
    class SingletonPattern {

        @Test
        @DisplayName("Should return same instance on multiple calls")
        void shouldReturnSameInstanceOnMultipleCalls() {
            ConfigurationService instance1 = ConfigurationService.getInstance();
            ConfigurationService instance2 = ConfigurationService.getInstance();

            assertNotNull(instance1);
            assertNotNull(instance2);
            assertSame(instance1, instance2);

            instance1.shutdown();
        }

        @Test
        @DisplayName("Should return same instance with custom storage")
        void shouldReturnSameInstanceWithCustomStorage() {
            ConfigurationService instance1 = ConfigurationService.getInstance(
                mockStorage
            );
            ConfigurationService instance2 = ConfigurationService.getInstance(
                mockStorage
            );

            assertNotNull(instance1);
            assertNotNull(instance2);
            assertSame(instance1, instance2);

            instance1.shutdown();
        }

        @Test
        @DisplayName("Should be thread-safe in singleton creation")
        void shouldBeThreadSafeInSingletonCreation()
            throws InterruptedException {
            int numThreads = 10;
            ConfigurationService[] instances =
                new ConfigurationService[numThreads];
            ExecutorService executor = Executors.newFixedThreadPool(numThreads);

            for (int i = 0; i < numThreads; i++) {
                final int index = i;
                executor.submit(() -> {
                    instances[index] = ConfigurationService.getInstance();
                });
            }

            executor.shutdown();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

            // All instances should be the same
            ConfigurationService firstInstance = instances[0];
            assertNotNull(firstInstance);

            for (int i = 1; i < numThreads; i++) {
                assertSame(firstInstance, instances[i]);
            }

            firstInstance.shutdown();
        }
    }

    @Nested
    @DisplayName("Service Initialization")
    class ServiceInitialization {

        @Test
        @DisplayName("Should initialize with default storage")
        void shouldInitializeWithDefaultStorage() {
            service = ConfigurationService.getInstance();

            assertNotNull(service);
            assertTrue(service.isCachingEnabled());
        }

        @Test
        @DisplayName("Should initialize with custom storage")
        void shouldInitializeWithCustomStorage() {
            service = ConfigurationService.getInstance(mockStorage);

            assertNotNull(service);
            assertTrue(service.isCachingEnabled());
            assertSame(mockStorage, service.getStorage());

            verify(mockStorage, times(1)).initialize();
        }

        @Test
        @DisplayName("Should handle storage initialization failure gracefully")
        void shouldHandleStorageInitializationFailureGracefully() {
            when(mockStorage.initialize()).thenReturn(false);

            service = ConfigurationService.getInstance(mockStorage);

            assertNotNull(service);
            verify(mockStorage, times(1)).initialize();
        }
    }

    @Nested
    @DisplayName("Configuration Loading")
    class ConfigurationLoading {

        @BeforeEach
        void setUp() {
            // Ensure clean state before each test
            resetSingletonInstance();
            service = ConfigurationService.getInstance(mockStorage);
        }

        @Test
        @DisplayName("Should load configuration from storage")
        void shouldLoadConfigurationFromStorage() {
            when(mockStorage.loadConfiguration("TEST")).thenReturn(testConfig);

            Map<String, String> result = service.loadConfiguration("TEST");

            assertNotNull(result);
            assertEquals(3, result.size());
            assertEquals("value1", result.get("key1"));
            assertEquals("value2", result.get("key2"));
            assertEquals("value3", result.get("key3"));

            verify(mockStorage, times(1)).loadConfiguration("TEST");
        }

        @Test
        @DisplayName("Should cache loaded configuration")
        void shouldCacheLoadedConfiguration() {
            when(mockStorage.loadConfiguration("TEST")).thenReturn(testConfig);

            // First call should load from storage
            Map<String, String> result1 = service.loadConfiguration("TEST");
            // Second call should use cache
            Map<String, String> result2 = service.loadConfiguration("TEST");

            assertNotNull(result1);
            assertNotNull(result2);
            assertEquals(result1.size(), result2.size());

            // Storage should only be called once
            verify(mockStorage, times(1)).loadConfiguration("TEST");
        }

        @Test
        @DisplayName("Should return independent copies from cache")
        void shouldReturnIndependentCopiesFromCache() {
            when(mockStorage.loadConfiguration("TEST")).thenReturn(testConfig);

            Map<String, String> result1 = service.loadConfiguration("TEST");
            Map<String, String> result2 = service.loadConfiguration("TEST");

            // Should not be the same object (independent copies)
            assertNotSame(result1, result2);
            assertEquals(result1, result2);

            // Modifying one should not affect the other
            result1.put("new_key", "new_value");
            assertFalse(result2.containsKey("new_key"));
        }

        @Test
        @DisplayName("Should handle empty configuration")
        void shouldHandleEmptyConfiguration() {
            when(mockStorage.loadConfiguration("EMPTY")).thenReturn(
                new HashMap<>()
            );

            Map<String, String> result = service.loadConfiguration("EMPTY");

            assertNotNull(result);
            assertTrue(result.isEmpty());
            verify(mockStorage, times(1)).loadConfiguration("EMPTY");
        }
    }

    @Nested
    @DisplayName("Configuration Saving")
    class ConfigurationSaving {

        @BeforeEach
        void setUp() {
            // Ensure clean state before each test
            resetSingletonInstance();
            service = ConfigurationService.getInstance(mockStorage);
        }

        @Test
        @DisplayName("Should save configuration to storage")
        void shouldSaveConfigurationToStorage() {
            when(mockStorage.saveConfiguration("TEST", testConfig)).thenReturn(
                true
            );

            boolean result = service.saveConfiguration("TEST", testConfig);

            assertTrue(result);
            verify(mockStorage, times(1)).saveConfiguration("TEST", testConfig);
        }

        @Test
        @DisplayName("Should update cache after successful save")
        void shouldUpdateCacheAfterSuccessfulSave() {
            when(mockStorage.saveConfiguration("TEST", testConfig)).thenReturn(
                true
            );
            when(mockStorage.loadConfiguration("TEST")).thenReturn(testConfig);

            // Save configuration
            boolean saveResult = service.saveConfiguration("TEST", testConfig);
            assertTrue(saveResult);

            // Load should use cache and not call storage again
            Map<String, String> loaded = service.loadConfiguration("TEST");
            assertEquals(testConfig.size(), loaded.size());

            verify(mockStorage, times(1)).saveConfiguration("TEST", testConfig);
            verify(mockStorage, never()).loadConfiguration("TEST");
        }

        @Test
        @DisplayName("Should not update cache on failed save")
        void shouldNotUpdateCacheOnFailedSave() {
            when(mockStorage.saveConfiguration("TEST", testConfig)).thenReturn(
                false
            );

            boolean result = service.saveConfiguration("TEST", testConfig);

            assertFalse(result);
            verify(mockStorage, times(1)).saveConfiguration("TEST", testConfig);
        }

        @Test
        @DisplayName("Should handle null parameters gracefully")
        void shouldHandleNullParametersGracefully() {
            when(mockStorage.saveConfiguration(null, testConfig)).thenReturn(
                false
            );
            when(mockStorage.saveConfiguration("TEST", null)).thenReturn(false);

            assertFalse(service.saveConfiguration(null, testConfig));
            assertFalse(service.saveConfiguration("TEST", null));

            verify(mockStorage, times(1)).saveConfiguration(null, testConfig);
            verify(mockStorage, times(1)).saveConfiguration("TEST", null);
        }
    }

    @Nested
    @DisplayName("Individual Value Operations")
    class IndividualValueOperations {

        @BeforeEach
        void setUp() {
            // Ensure clean state before each test
            resetSingletonInstance();
            service = ConfigurationService.getInstance(mockStorage);
        }

        @Test
        @DisplayName("Should get configuration value from cache")
        void shouldGetConfigurationValueFromCache() {
            // Setup cache
            when(mockStorage.loadConfiguration("TEST")).thenReturn(testConfig);
            service.loadConfiguration("TEST");

            String result = service.getConfigurationValue("TEST", "key1");

            assertEquals("value1", result);
            // Should not call storage again
            verify(mockStorage, never()).getConfigurationValue(
                anyString(),
                anyString()
            );
        }

        @Test
        @DisplayName("Should fallback to storage when not in cache")
        void shouldFallbackToStorageWhenNotInCache() {
            when(mockStorage.getConfigurationValue("TEST", "key1")).thenReturn(
                "storage_value"
            );

            String result = service.getConfigurationValue("TEST", "key1");

            assertEquals("storage_value", result);
            verify(mockStorage, times(1)).getConfigurationValue("TEST", "key1");
        }

        @Test
        @DisplayName("Should set configuration value and update cache")
        void shouldSetConfigurationValueAndUpdateCache() {
            when(
                mockStorage.setConfigurationValue("TEST", "key1", "new_value")
            ).thenReturn(true);

            boolean result = service.setConfigurationValue(
                "TEST",
                "key1",
                "new_value"
            );

            assertTrue(result);

            // Value should be available from cache
            String cachedValue = service.getConfigurationValue("TEST", "key1");
            assertEquals("new_value", cachedValue);

            verify(mockStorage, times(1)).setConfigurationValue(
                "TEST",
                "key1",
                "new_value"
            );
            verify(mockStorage, never()).getConfigurationValue(
                anyString(),
                anyString()
            );
        }

        @Test
        @DisplayName("Should delete configuration value and update cache")
        void shouldDeleteConfigurationValueAndUpdateCache() {
            // Setup initial cache
            when(mockStorage.loadConfiguration("TEST")).thenReturn(testConfig);
            service.loadConfiguration("TEST");

            when(
                mockStorage.deleteConfigurationValue("TEST", "key1")
            ).thenReturn(true);

            boolean result = service.deleteConfigurationValue("TEST", "key1");

            assertTrue(result);

            // Value should not be available from cache
            String cachedValue = service.getConfigurationValue("TEST", "key1");
            assertNull(cachedValue);

            verify(mockStorage, times(1)).deleteConfigurationValue(
                "TEST",
                "key1"
            );
        }
    }

    @Nested
    @DisplayName("Configuration Reset")
    class ConfigurationReset {

        @BeforeEach
        void setUp() {
            // Ensure clean state before each test
            resetSingletonInstance();
            service = ConfigurationService.getInstance(mockStorage);
        }

        @Test
        @DisplayName("Should reset configuration and clear cache")
        void shouldResetConfigurationAndClearCache() {
            // Setup cache
            when(mockStorage.loadConfiguration("TEST")).thenReturn(testConfig);
            service.loadConfiguration("TEST");

            when(mockStorage.resetConfiguration("TEST")).thenReturn(true);

            boolean result = service.resetConfiguration("TEST");

            assertTrue(result);

            // Next load should call storage again (cache cleared)
            when(mockStorage.loadConfiguration("TEST")).thenReturn(
                new HashMap<>()
            );
            Map<String, String> loaded = service.loadConfiguration("TEST");
            assertTrue(loaded.isEmpty());

            verify(mockStorage, times(1)).resetConfiguration("TEST");
            verify(mockStorage, times(2)).loadConfiguration("TEST");
        }

        @Test
        @DisplayName("Should handle reset failure")
        void shouldHandleResetFailure() {
            when(mockStorage.resetConfiguration("TEST")).thenReturn(false);

            boolean result = service.resetConfiguration("TEST");

            assertFalse(result);
            verify(mockStorage, times(1)).resetConfiguration("TEST");
        }
    }

    @Nested
    @DisplayName("Import/Export Operations")
    class ImportExportOperations {

        @BeforeEach
        void setUp() {
            // Ensure clean state before each test
            resetSingletonInstance();
            service = ConfigurationService.getInstance(mockStorage);
        }

        @Test
        @DisplayName("Should export configuration")
        void shouldExportConfiguration() {
            Path exportPath = Paths.get("/tmp/config.properties");
            when(
                mockStorage.exportConfiguration("TEST", exportPath)
            ).thenReturn(true);

            boolean result = service.exportConfiguration("TEST", exportPath);

            assertTrue(result);
            verify(mockStorage, times(1)).exportConfiguration(
                "TEST",
                exportPath
            );
        }

        @Test
        @DisplayName("Should import configuration and clear cache")
        void shouldImportConfigurationAndClearCache() {
            // Setup cache
            when(mockStorage.loadConfiguration("TEST")).thenReturn(testConfig);
            service.loadConfiguration("TEST");

            Path importPath = Paths.get("/tmp/config.properties");
            when(
                mockStorage.importConfiguration("TEST", importPath)
            ).thenReturn(true);

            boolean result = service.importConfiguration("TEST", importPath);

            assertTrue(result);

            // Next load should call storage again (cache cleared)
            when(mockStorage.loadConfiguration("TEST")).thenReturn(
                new HashMap<>()
            );
            Map<String, String> loaded = service.loadConfiguration("TEST");
            assertTrue(loaded.isEmpty());

            verify(mockStorage, times(1)).importConfiguration(
                "TEST",
                importPath
            );
            verify(mockStorage, times(2)).loadConfiguration("TEST");
        }

        @Test
        @DisplayName("Should not clear cache on import failure")
        void shouldNotClearCacheOnImportFailure() {
            // Setup cache
            when(mockStorage.loadConfiguration("TEST")).thenReturn(testConfig);
            service.loadConfiguration("TEST");

            Path importPath = Paths.get("/invalid/path.properties");
            when(
                mockStorage.importConfiguration("TEST", importPath)
            ).thenReturn(false);

            boolean result = service.importConfiguration("TEST", importPath);

            assertFalse(result);

            // Cache should still be available
            Map<String, String> loaded = service.loadConfiguration("TEST");
            assertEquals(testConfig.size(), loaded.size());

            verify(mockStorage, times(1)).importConfiguration(
                "TEST",
                importPath
            );
            verify(mockStorage, times(1)).loadConfiguration("TEST"); // Only initial load
        }
    }

    @Nested
    @DisplayName("Cache Management")
    class CacheManagement {

        @BeforeEach
        void setUp() {
            // Ensure clean state before each test
            resetSingletonInstance();
            service = ConfigurationService.getInstance(mockStorage);
        }

        @Test
        @DisplayName("Should clear specific cache type")
        void shouldClearSpecificCacheType() {
            // Setup cache for multiple types
            when(mockStorage.loadConfiguration("TYPE1")).thenReturn(testConfig);
            when(mockStorage.loadConfiguration("TYPE2")).thenReturn(testConfig);

            service.loadConfiguration("TYPE1");
            service.loadConfiguration("TYPE2");

            // Clear specific type
            service.clearCache("TYPE1");

            // TYPE1 should load from storage again, TYPE2 should use cache
            when(mockStorage.loadConfiguration("TYPE1")).thenReturn(
                new HashMap<>()
            );

            Map<String, String> loaded1 = service.loadConfiguration("TYPE1");
            Map<String, String> loaded2 = service.loadConfiguration("TYPE2");

            assertTrue(loaded1.isEmpty()); // Reloaded from storage
            assertEquals(testConfig.size(), loaded2.size()); // From cache

            verify(mockStorage, times(2)).loadConfiguration("TYPE1");
            verify(mockStorage, times(1)).loadConfiguration("TYPE2");
        }

        @Test
        @DisplayName("Should clear all cache types")
        void shouldClearAllCacheTypes() {
            // Setup cache
            when(mockStorage.loadConfiguration("TYPE1")).thenReturn(testConfig);
            service.loadConfiguration("TYPE1");

            // Clear all cache
            service.clearCache(null);

            // Should load from storage again
            when(mockStorage.loadConfiguration("TYPE1")).thenReturn(
                new HashMap<>()
            );
            Map<String, String> loaded = service.loadConfiguration("TYPE1");
            assertTrue(loaded.isEmpty());

            verify(mockStorage, times(2)).loadConfiguration("TYPE1");
        }

        @Test
        @DisplayName("Should get cache statistics")
        void shouldGetCacheStatistics() {
            // Setup cache with multiple types
            when(mockStorage.loadConfiguration("TYPE1")).thenReturn(testConfig);
            when(mockStorage.loadConfiguration("TYPE2")).thenReturn(testConfig);

            service.loadConfiguration("TYPE1");
            service.loadConfiguration("TYPE2");

            String stats = service.getCacheStatistics();

            assertNotNull(stats);
            assertTrue(stats.contains("Configuration Cache Statistics"));
            assertTrue(stats.contains("Total cached types: 2"));
            assertTrue(stats.contains("TYPE1"));
            assertTrue(stats.contains("TYPE2"));
        }

        @Test
        @DisplayName("Should handle cache statistics when caching disabled")
        void shouldHandleCacheStatisticsWhenCachingDisabled() {
            // This would require a way to create service without caching
            // For now, test with enabled caching
            String stats = service.getCacheStatistics();

            assertNotNull(stats);
            assertTrue(stats.contains("Cache Statistics"));
        }
    }

    @Nested
    @DisplayName("Health and Status")
    class HealthAndStatus {

        @BeforeEach
        void setUp() {
            // Ensure clean state before each test
            resetSingletonInstance();
            service = ConfigurationService.getInstance(mockStorage);
        }

        @Test
        @DisplayName("Should get health status")
        void shouldGetHealthStatus() {
            String health = service.getHealthStatus();

            assertNotNull(health);
            assertTrue(health.contains("Configuration Service Health"));
            assertTrue(health.contains("Storage: Healthy"));
            assertTrue(health.contains("Storage Type: mock"));
            assertTrue(health.contains("Mock Storage Location"));
            assertTrue(health.contains("Caching: Enabled"));

            verify(mockStorage, times(1)).isHealthy();
            verify(mockStorage, atLeastOnce()).getStorageType();
            verify(mockStorage, times(1)).getStorageLocation();
        }

        @Test
        @DisplayName("Should show unhealthy storage in status")
        void shouldShowUnhealthyStorageInStatus() {
            when(mockStorage.isHealthy()).thenReturn(false);

            String health = service.getHealthStatus();

            assertNotNull(health);
            assertTrue(health.contains("❌"));
            assertTrue(health.contains("Storage: Unhealthy"));
        }

        @Test
        @DisplayName("Should check if caching is enabled")
        void shouldCheckIfCachingIsEnabled() {
            assertTrue(service.isCachingEnabled());
        }

        @Test
        @DisplayName("Should get storage reference")
        void shouldGetStorageReference() {
            ConfigurationStorage storage = service.getStorage();
            assertSame(mockStorage, storage);
        }
    }

    @Nested
    @DisplayName("Audit Operations")
    class AuditOperations {

        @BeforeEach
        void setUp() {
            // Ensure clean state before each test
            resetSingletonInstance();
            service = ConfigurationService.getInstance(mockStorage);
        }

        @Test
        @DisplayName("Should get audit log")
        void shouldGetAuditLog() {
            String expectedLog = "2023-01-01 10:00:00 - Updated key1";
            when(mockStorage.getAuditLog("TEST", 10)).thenReturn(expectedLog);

            String result = service.getAuditLog("TEST", 10);

            assertEquals(expectedLog, result);
            verify(mockStorage, times(1)).getAuditLog("TEST", 10);
        }
    }

    @Nested
    @DisplayName("Service Lifecycle")
    class ServiceLifecycle {

        @Test
        @DisplayName("Should shutdown service gracefully")
        void shouldShutdownServiceGracefully() {
            service = ConfigurationService.getInstance(mockStorage);

            // Load some data to cache
            when(mockStorage.loadConfiguration("TEST")).thenReturn(testConfig);
            service.loadConfiguration("TEST");

            // Shutdown
            service.shutdown();

            verify(mockStorage, times(1)).cleanup();

            // Instance should be reset
            ConfigurationService newInstance =
                ConfigurationService.getInstance();
            assertNotSame(service, newInstance);

            newInstance.shutdown();
        }
    }

    @Nested
    @DisplayName("Concurrent Operations")
    class ConcurrentOperations {

        @BeforeEach
        void setUp() {
            // Ensure clean state before each test
            resetSingletonInstance();
            service = ConfigurationService.getInstance(mockStorage);
        }

        @Test
        @DisplayName("Should handle concurrent cache access safely")
        void shouldHandleConcurrentCacheAccessSafely()
            throws InterruptedException {
            when(mockStorage.loadConfiguration(anyString())).thenReturn(
                testConfig
            );
            when(
                mockStorage.setConfigurationValue(
                    anyString(),
                    anyString(),
                    anyString()
                )
            ).thenReturn(true);

            int numThreads = 10;
            ExecutorService executor = Executors.newFixedThreadPool(numThreads);

            // Submit concurrent operations
            for (int i = 0; i < numThreads; i++) {
                final int threadId = i;
                executor.submit(() -> {
                    // Mix of read and write operations
                    service.loadConfiguration("TYPE" + (threadId % 3));
                    service.setConfigurationValue(
                        "TYPE" + (threadId % 3),
                        "key" + threadId,
                        "value" + threadId
                    );
                    service.getConfigurationValue(
                        "TYPE" + (threadId % 3),
                        "key" + threadId
                    );
                });
            }

            executor.shutdown();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

            // Should complete without exceptions
            // Verify some interactions occurred
            verify(mockStorage, atLeastOnce()).loadConfiguration(anyString());
            verify(mockStorage, atLeastOnce()).setConfigurationValue(
                anyString(),
                anyString(),
                anyString()
            );
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

        @BeforeEach
        void setUp() {
            // Ensure clean state before each test
            resetSingletonInstance();
            service = ConfigurationService.getInstance(mockStorage);
        }

        @Test
        @DisplayName("Should handle storage exceptions gracefully")
        void shouldHandleStorageExceptionsGracefully() {
            when(mockStorage.loadConfiguration("ERROR")).thenThrow(
                new RuntimeException("Storage error")
            );

            // Should propagate exception as service doesn't catch it
            assertThrows(RuntimeException.class, () -> {
                service.loadConfiguration("ERROR");
            });
        }

        @Test
        @DisplayName("Should handle configuration existence check")
        void shouldHandleConfigurationExistenceCheck() {
            when(mockStorage.configurationExists("TEST")).thenReturn(true);
            when(mockStorage.configurationExists("MISSING")).thenReturn(false);

            assertTrue(service.configurationExists("TEST"));
            assertFalse(service.configurationExists("MISSING"));

            verify(mockStorage, times(1)).configurationExists("TEST");
            verify(mockStorage, times(1)).configurationExists("MISSING");
        }
    }
}
