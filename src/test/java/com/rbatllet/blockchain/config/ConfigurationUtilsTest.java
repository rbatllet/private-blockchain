package com.rbatllet.blockchain.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ConfigurationUtils utility class
 */
@DisplayName("ConfigurationUtils Tests")
class ConfigurationUtilsTest {

    private EncryptionConfig testConfig;
    private Properties testProperties;
    private Map<String, String> testMap;

    @BeforeEach
    void setUp() {
        testConfig = new EncryptionConfig();

        testProperties = new Properties();
        testProperties.setProperty("algorithm", "AES");
        testProperties.setProperty("mode", "GCM");
        testProperties.setProperty("padding", "NoPadding");
        testProperties.setProperty("key.length", "256");
        testProperties.setProperty("iv.length", "12");
        testProperties.setProperty("tag.length", "128");
        testProperties.setProperty("salt.length", "32");
        testProperties.setProperty("pbkdf2.iterations", "100000");
        testProperties.setProperty("pbkdf2.algorithm", "PBKDF2WithHmacSHA256");
        testProperties.setProperty("min.password.length", "12");
        testProperties.setProperty("max.data.size.bytes", "1048576");
        testProperties.setProperty("validate.encryption.format", "true");
        testProperties.setProperty("enable.compression", "false");
        testProperties.setProperty("secure.random.enabled", "true");
        testProperties.setProperty("metadata.encryption.enabled", "false");
        testProperties.setProperty("corruption.detection.enabled", "true");

        testMap = new HashMap<>();
        testMap.put("algorithm", "AES");
        testMap.put("mode", "GCM");
        testMap.put("padding", "NoPadding");
        testMap.put("key.length", "256");
        testMap.put("iv.length", "12");
        testMap.put("tag.length", "128");
        testMap.put("salt.length", "32");
        testMap.put("pbkdf2.iterations", "100000");
        testMap.put("pbkdf2.algorithm", "PBKDF2WithHmacSHA256");
        testMap.put("min.password.length", "12");
        testMap.put("max.data.size.bytes", "1048576");
        testMap.put("validate.encryption.format", "true");
        testMap.put("enable.compression", "false");
        testMap.put("secure.random.enabled", "true");
        testMap.put("metadata.encryption.enabled", "false");
        testMap.put("corruption.detection.enabled", "true");
    }

    @Nested
    @DisplayName("EncryptionConfig Creation")
    class EncryptionConfigCreation {

        @Test
        @DisplayName("Should create EncryptionConfig from Properties")
        void shouldCreateEncryptionConfigFromProperties() {
            EncryptionConfig config = ConfigurationUtils.createEncryptionConfigFromProperties(testProperties);

            assertNotNull(config);
            assertEquals("AES", config.getEncryptionAlgorithm());
            assertEquals("GCM", config.getEncryptionMode());
            assertEquals("NoPadding", config.getPadding());
            assertEquals(256, config.getKeyLength());
            assertEquals(12, config.getIvLength());
            assertEquals(128, config.getTagLength());
            assertEquals(32, config.getSaltLength());
            assertEquals(100000, config.getPbkdf2Iterations());
            assertEquals("PBKDF2WithHmacSHA256", config.getPbkdf2Algorithm());
            assertEquals(12, config.getMinPasswordLength());
            assertEquals(1048576, config.getMaxDataSizeBytes());
            assertTrue(config.isValidateEncryptionFormat());
            assertFalse(config.isEnableCompression());
            assertTrue(config.isSecureRandomEnabled());
            assertFalse(config.isMetadataEncryptionEnabled());
            assertTrue(config.isCorruptionDetectionEnabled());
        }

        @Test
        @DisplayName("Should create EncryptionConfig from Map")
        void shouldCreateEncryptionConfigFromMap() {
            EncryptionConfig config = ConfigurationUtils.createEncryptionConfigFromMap(testMap);

            assertNotNull(config);
            assertEquals("AES", config.getEncryptionAlgorithm());
            assertEquals("GCM", config.getEncryptionMode());
            assertEquals("NoPadding", config.getPadding());
            assertEquals(256, config.getKeyLength());
            assertEquals(12, config.getIvLength());
            assertEquals(128, config.getTagLength());
            assertEquals(32, config.getSaltLength());
            assertEquals(100000, config.getPbkdf2Iterations());
            assertEquals("PBKDF2WithHmacSHA256", config.getPbkdf2Algorithm());
            assertEquals(12, config.getMinPasswordLength());
            assertEquals(1048576, config.getMaxDataSizeBytes());
            assertTrue(config.isValidateEncryptionFormat());
            assertFalse(config.isEnableCompression());
            assertTrue(config.isSecureRandomEnabled());
            assertFalse(config.isMetadataEncryptionEnabled());
            assertTrue(config.isCorruptionDetectionEnabled());
        }

        @Test
        @DisplayName("Should create default EncryptionConfig from empty Properties")
        void shouldCreateDefaultEncryptionConfigFromEmptyProperties() {
            Properties emptyProps = new Properties();
            EncryptionConfig config = ConfigurationUtils.createEncryptionConfigFromProperties(emptyProps);

            assertNotNull(config);
            assertEquals("AES", config.getEncryptionAlgorithm()); // Default values
        }

        @Test
        @DisplayName("Should create default EncryptionConfig from empty Map")
        void shouldCreateDefaultEncryptionConfigFromEmptyMap() {
            Map<String, String> emptyMap = new HashMap<>();
            EncryptionConfig config = ConfigurationUtils.createEncryptionConfigFromMap(emptyMap);

            assertNotNull(config);
            assertEquals("AES", config.getEncryptionAlgorithm()); // Default values
        }

        @Test
        @DisplayName("Should handle partial configuration from Properties")
        void shouldHandlePartialConfigurationFromProperties() {
            Properties partialProps = new Properties();
            partialProps.setProperty("key.length", "192");
            partialProps.setProperty("pbkdf2.iterations", "50000");

            EncryptionConfig config = ConfigurationUtils.createEncryptionConfigFromProperties(partialProps);

            assertNotNull(config);
            assertEquals(192, config.getKeyLength());
            assertEquals(50000, config.getPbkdf2Iterations());
            assertEquals("AES", config.getEncryptionAlgorithm()); // Default value
        }

        @Test
        @DisplayName("Should handle partial configuration from Map")
        void shouldHandlePartialConfigurationFromMap() {
            Map<String, String> partialMap = new HashMap<>();
            partialMap.put("key.length", "192");
            partialMap.put("pbkdf2.iterations", "50000");

            EncryptionConfig config = ConfigurationUtils.createEncryptionConfigFromMap(partialMap);

            assertNotNull(config);
            assertEquals(192, config.getKeyLength());
            assertEquals(50000, config.getPbkdf2Iterations());
            assertEquals("AES", config.getEncryptionAlgorithm()); // Default value
        }
    }

    @Nested
    @DisplayName("EncryptionConfig Conversion")
    class EncryptionConfigConversion {

        @Test
        @DisplayName("Should convert EncryptionConfig to Properties")
        void shouldConvertEncryptionConfigToProperties() {
            Properties props = ConfigurationUtils.encryptionConfigToProperties(testConfig);

            assertNotNull(props);
            assertEquals("AES", props.getProperty("algorithm"));
            assertEquals("GCM", props.getProperty("mode"));
            assertEquals("NoPadding", props.getProperty("padding"));
            assertEquals("256", props.getProperty("key.length"));
            assertEquals("12", props.getProperty("iv.length"));
            assertEquals("128", props.getProperty("tag.length"));
            assertEquals("32", props.getProperty("salt.length"));
            assertEquals("100000", props.getProperty("pbkdf2.iterations"));
            assertEquals("PBKDF2WithHmacSHA256", props.getProperty("pbkdf2.algorithm"));
            assertEquals("12", props.getProperty("min.password.length"));
            assertEquals("1048576", props.getProperty("max.data.size.bytes"));
            assertEquals("true", props.getProperty("validate.encryption.format"));
            assertEquals("false", props.getProperty("enable.compression"));
            assertEquals("true", props.getProperty("secure.random.enabled"));
            assertEquals("false", props.getProperty("metadata.encryption.enabled"));
            assertEquals("true", props.getProperty("corruption.detection.enabled"));
        }

        @Test
        @DisplayName("Should convert EncryptionConfig to Map")
        void shouldConvertEncryptionConfigToMap() {
            Map<String, String> map = ConfigurationUtils.encryptionConfigToMap(testConfig);

            assertNotNull(map);
            assertEquals("AES", map.get("algorithm"));
            assertEquals("GCM", map.get("mode"));
            assertEquals("NoPadding", map.get("padding"));
            assertEquals("256", map.get("key.length"));
            assertEquals("12", map.get("iv.length"));
            assertEquals("128", map.get("tag.length"));
            assertEquals("32", map.get("salt.length"));
            assertEquals("100000", map.get("pbkdf2.iterations"));
            assertEquals("PBKDF2WithHmacSHA256", map.get("pbkdf2.algorithm"));
            assertEquals("12", map.get("min.password.length"));
            assertEquals("1048576", map.get("max.data.size.bytes"));
            assertEquals("true", map.get("validate.encryption.format"));
            assertEquals("false", map.get("enable.compression"));
            assertEquals("true", map.get("secure.random.enabled"));
            assertEquals("false", map.get("metadata.encryption.enabled"));
            assertEquals("true", map.get("corruption.detection.enabled"));
        }

        @Test
        @DisplayName("Should populate Properties from EncryptionConfig")
        void shouldPopulatePropertiesFromEncryptionConfig() {
            Properties props = new Properties();
            props.setProperty("existing.property", "existing.value");

            ConfigurationUtils.populatePropertiesFromEncryptionConfig(props, testConfig);

            // Should preserve existing property
            assertEquals("existing.value", props.getProperty("existing.property"));

            // Should add encryption config properties
            assertEquals("AES", props.getProperty("algorithm"));
            assertEquals("GCM", props.getProperty("mode"));
            assertEquals("256", props.getProperty("key.length"));
        }
    }

    @Nested
    @DisplayName("Properties and Map Conversion")
    class PropertiesAndMapConversion {

        @Test
        @DisplayName("Should convert Properties to String Map")
        void shouldConvertPropertiesToStringMap() {
            Map<String, String> map = ConfigurationUtils.propertiesToStringMap(testProperties);

            assertNotNull(map);
            assertEquals(testProperties.size(), map.size());
            assertEquals("AES", map.get("algorithm"));
            assertEquals("256", map.get("key.length"));
            assertEquals("true", map.get("validate.encryption.format"));
        }

        @Test
        @DisplayName("Should convert String Map to Properties")
        void shouldConvertStringMapToProperties() {
            Properties props = ConfigurationUtils.stringMapToProperties(testMap);

            assertNotNull(props);
            assertEquals(testMap.size(), props.size());
            assertEquals("AES", props.getProperty("algorithm"));
            assertEquals("256", props.getProperty("key.length"));
            assertEquals("true", props.getProperty("validate.encryption.format"));
        }

        @Test
        @DisplayName("Should handle empty Properties conversion")
        void shouldHandleEmptyPropertiesConversion() {
            Properties emptyProps = new Properties();
            Map<String, String> map = ConfigurationUtils.propertiesToStringMap(emptyProps);

            assertNotNull(map);
            assertTrue(map.isEmpty());
        }

        @Test
        @DisplayName("Should handle empty Map conversion")
        void shouldHandleEmptyMapConversion() {
            Map<String, String> emptyMap = new HashMap<>();
            Properties props = ConfigurationUtils.stringMapToProperties(emptyMap);

            assertNotNull(props);
            assertTrue(props.isEmpty());
        }
    }

    @Nested
    @DisplayName("Configuration Map Operations")
    class ConfigurationMapOperations {

        @Test
        @DisplayName("Should merge configuration maps with priority")
        void shouldMergeConfigurationMapsWithPriority() {
            Map<String, String> baseConfig = new HashMap<>();
            baseConfig.put("key1", "base_value1");
            baseConfig.put("key2", "base_value2");
            baseConfig.put("key3", "base_value3");

            Map<String, String> priorityConfig = new HashMap<>();
            priorityConfig.put("key2", "priority_value2");
            priorityConfig.put("key4", "priority_value4");

            Map<String, String> merged = ConfigurationUtils.mergeConfigMaps(baseConfig, priorityConfig);

            assertNotNull(merged);
            assertEquals(4, merged.size());
            assertEquals("base_value1", merged.get("key1")); // From base
            assertEquals("priority_value2", merged.get("key2")); // From priority (overridden)
            assertEquals("base_value3", merged.get("key3")); // From base
            assertEquals("priority_value4", merged.get("key4")); // From priority
        }

        @Test
        @DisplayName("Should merge with empty priority map")
        void shouldMergeWithEmptyPriorityMap() {
            Map<String, String> baseConfig = new HashMap<>();
            baseConfig.put("key1", "value1");

            Map<String, String> emptyPriority = new HashMap<>();

            Map<String, String> merged = ConfigurationUtils.mergeConfigMaps(baseConfig, emptyPriority);

            assertNotNull(merged);
            assertEquals(1, merged.size());
            assertEquals("value1", merged.get("key1"));
        }

        @Test
        @DisplayName("Should merge with empty base map")
        void shouldMergeWithEmptyBaseMap() {
            Map<String, String> emptyBase = new HashMap<>();

            Map<String, String> priorityConfig = new HashMap<>();
            priorityConfig.put("key1", "value1");

            Map<String, String> merged = ConfigurationUtils.mergeConfigMaps(emptyBase, priorityConfig);

            assertNotNull(merged);
            assertEquals(1, merged.size());
            assertEquals("value1", merged.get("key1"));
        }
    }

    @Nested
    @DisplayName("Configuration Validation")
    class ConfigurationValidation {

        @Test
        @DisplayName("Should validate required keys successfully")
        void shouldValidateRequiredKeysSuccessfully() {
            Map<String, String> config = new HashMap<>();
            config.put("required1", "value1");
            config.put("required2", "value2");
            config.put("optional", "value3");

            boolean result = ConfigurationUtils.validateRequiredKeys(config, "required1", "required2");

            assertTrue(result);
        }

        @Test
        @DisplayName("Should fail validation for missing required keys")
        void shouldFailValidationForMissingRequiredKeys() {
            Map<String, String> config = new HashMap<>();
            config.put("required1", "value1");

            boolean result = ConfigurationUtils.validateRequiredKeys(config, "required1", "missing_key");

            assertFalse(result);
        }

        @Test
        @DisplayName("Should fail validation for null required values")
        void shouldFailValidationForNullRequiredValues() {
            Map<String, String> config = new HashMap<>();
            config.put("required1", "value1");
            config.put("required2", null);

            boolean result = ConfigurationUtils.validateRequiredKeys(config, "required1", "required2");

            assertFalse(result);
        }

        @Test
        @DisplayName("Should fail validation for empty required values")
        void shouldFailValidationForEmptyRequiredValues() {
            Map<String, String> config = new HashMap<>();
            config.put("required1", "value1");
            config.put("required2", "");

            boolean result = ConfigurationUtils.validateRequiredKeys(config, "required1", "required2");

            assertFalse(result);
        }

        @Test
        @DisplayName("Should fail validation for whitespace-only required values")
        void shouldFailValidationForWhitespaceOnlyRequiredValues() {
            Map<String, String> config = new HashMap<>();
            config.put("required1", "value1");
            config.put("required2", "   ");

            boolean result = ConfigurationUtils.validateRequiredKeys(config, "required1", "required2");

            assertFalse(result);
        }

        @Test
        @DisplayName("Should validate with no required keys")
        void shouldValidateWithNoRequiredKeys() {
            Map<String, String> config = new HashMap<>();

            boolean result = ConfigurationUtils.validateRequiredKeys(config);

            assertTrue(result);
        }
    }

    @Nested
    @DisplayName("Configuration Value Retrieval")
    class ConfigurationValueRetrieval {

        private Map<String, String> config;

        @BeforeEach
        void setUp() {
            config = new HashMap<>();
            config.put("string_key", "string_value");
            config.put("int_key", "42");
            config.put("boolean_key", "true");
            config.put("long_key", "9876543210");
            config.put("invalid_int", "not_a_number");
            config.put("invalid_long", "not_a_long");
        }

        @Test
        @DisplayName("Should get configuration value with default")
        void shouldGetConfigurationValueWithDefault() {
            String result = ConfigurationUtils.getConfigValue(config, "string_key", "default");
            assertEquals("string_value", result);

            String defaultResult = ConfigurationUtils.getConfigValue(config, "missing_key", "default");
            assertEquals("default", defaultResult);
        }

        @Test
        @DisplayName("Should get configuration value as integer")
        void shouldGetConfigurationValueAsInteger() {
            int result = ConfigurationUtils.getConfigValueAsInt(config, "int_key", 0);
            assertEquals(42, result);

            int defaultResult = ConfigurationUtils.getConfigValueAsInt(config, "missing_key", 100);
            assertEquals(100, defaultResult);
        }

        @Test
        @DisplayName("Should handle invalid integer with default")
        void shouldHandleInvalidIntegerWithDefault() {
            int result = ConfigurationUtils.getConfigValueAsInt(config, "invalid_int", 999);
            assertEquals(999, result);
        }

        @Test
        @DisplayName("Should get configuration value as boolean")
        void shouldGetConfigurationValueAsBoolean() {
            boolean result = ConfigurationUtils.getConfigValueAsBoolean(config, "boolean_key", false);
            assertTrue(result);

            boolean defaultResult = ConfigurationUtils.getConfigValueAsBoolean(config, "missing_key", false);
            assertFalse(defaultResult);
        }

        @Test
        @DisplayName("Should handle various boolean values")
        void shouldHandleVariousBooleanValues() {
            config.put("false_key", "false");
            config.put("FALSE_key", "FALSE");
            config.put("other_key", "other");

            assertFalse(ConfigurationUtils.getConfigValueAsBoolean(config, "false_key", true));
            assertFalse(ConfigurationUtils.getConfigValueAsBoolean(config, "FALSE_key", true));
            assertFalse(ConfigurationUtils.getConfigValueAsBoolean(config, "other_key", true));
        }

        @Test
        @DisplayName("Should get configuration value as long")
        void shouldGetConfigurationValueAsLong() {
            long result = ConfigurationUtils.getConfigValueAsLong(config, "long_key", 0L);
            assertEquals(9876543210L, result);

            long defaultResult = ConfigurationUtils.getConfigValueAsLong(config, "missing_key", 123L);
            assertEquals(123L, defaultResult);
        }

        @Test
        @DisplayName("Should handle invalid long with default")
        void shouldHandleInvalidLongWithDefault() {
            long result = ConfigurationUtils.getConfigValueAsLong(config, "invalid_long", 999L);
            assertEquals(999L, result);
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("Should handle null Properties in conversion")
        void shouldHandleNullPropertiesInConversion() {
            assertThrows(NullPointerException.class, () ->
                ConfigurationUtils.createEncryptionConfigFromProperties(null));
        }

        @Test
        @DisplayName("Should handle null Map in conversion")
        void shouldHandleNullMapInConversion() {
            assertThrows(NullPointerException.class, () ->
                ConfigurationUtils.createEncryptionConfigFromMap(null));
        }

        @Test
        @DisplayName("Should handle null EncryptionConfig in conversion")
        void shouldHandleNullEncryptionConfigInConversion() {
            assertThrows(NullPointerException.class, () ->
                ConfigurationUtils.encryptionConfigToProperties(null));

            assertThrows(NullPointerException.class, () ->
                ConfigurationUtils.encryptionConfigToMap(null));
        }
    }

    @Nested
    @DisplayName("Round-trip Conversion")
    class RoundTripConversion {

        @Test
        @DisplayName("Should preserve data in Properties round-trip")
        void shouldPreserveDataInPropertiesRoundTrip() {
            // Config -> Properties -> Config
            Properties props = ConfigurationUtils.encryptionConfigToProperties(testConfig);
            EncryptionConfig roundTripConfig = ConfigurationUtils.createEncryptionConfigFromProperties(props);

            assertEquals(testConfig.getEncryptionAlgorithm(), roundTripConfig.getEncryptionAlgorithm());
            assertEquals(testConfig.getKeyLength(), roundTripConfig.getKeyLength());
            assertEquals(testConfig.getPbkdf2Iterations(), roundTripConfig.getPbkdf2Iterations());
            assertEquals(testConfig.isValidateEncryptionFormat(), roundTripConfig.isValidateEncryptionFormat());
        }

        @Test
        @DisplayName("Should preserve data in Map round-trip")
        void shouldPreserveDataInMapRoundTrip() {
            // Config -> Map -> Config
            Map<String, String> map = ConfigurationUtils.encryptionConfigToMap(testConfig);
            EncryptionConfig roundTripConfig = ConfigurationUtils.createEncryptionConfigFromMap(map);

            assertEquals(testConfig.getEncryptionAlgorithm(), roundTripConfig.getEncryptionAlgorithm());
            assertEquals(testConfig.getKeyLength(), roundTripConfig.getKeyLength());
            assertEquals(testConfig.getPbkdf2Iterations(), roundTripConfig.getPbkdf2Iterations());
            assertEquals(testConfig.isValidateEncryptionFormat(), roundTripConfig.isValidateEncryptionFormat());
        }

        @Test
        @DisplayName("Should preserve data in Properties-Map conversion")
        void shouldPreserveDataInPropertiesMapConversion() {
            // Properties -> Map -> Properties
            Map<String, String> map = ConfigurationUtils.propertiesToStringMap(testProperties);
            Properties roundTripProps = ConfigurationUtils.stringMapToProperties(map);

            assertEquals(testProperties.size(), roundTripProps.size());
            for (String key : testProperties.stringPropertyNames()) {
                assertEquals(testProperties.getProperty(key), roundTripProps.getProperty(key));
            }
        }
    }
}
