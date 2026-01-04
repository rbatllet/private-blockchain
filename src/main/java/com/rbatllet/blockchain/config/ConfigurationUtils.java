package com.rbatllet.blockchain.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Utility class for configuration serialization and deserialization
 * Provides common methods for converting between different configuration formats
 */
public class ConfigurationUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationUtils.class);
    
    private ConfigurationUtils() {
        // Utility class - no instantiation
    }
    
    /**
     * Create EncryptionConfig from Properties
     * @param props Properties containing encryption configuration
     * @return EncryptionConfig instance
     */
    public static EncryptionConfig createEncryptionConfigFromProperties(Properties props) {
        return createEncryptionConfigFromMap(propertiesToStringMap(props));
    }
    
    /**
     * Create EncryptionConfig from Map
     * @param props Map containing encryption configuration
     * @return EncryptionConfig instance
     */
    public static EncryptionConfig createEncryptionConfigFromMap(Map<String, String> props) {
        if (props.isEmpty()) {
            return new EncryptionConfig(); // Default config
        }
        
        EncryptionConfig.Builder builder = EncryptionConfig.builder();
        
        if (props.containsKey("algorithm")) {
            builder.encryptionAlgorithm(props.get("algorithm"));
        }
        if (props.containsKey("mode")) {
            builder.encryptionMode(props.get("mode"));
        }
        if (props.containsKey("padding")) {
            builder.padding(props.get("padding"));
        }
        if (props.containsKey("key.length")) {
            builder.keyLength(Integer.parseInt(props.get("key.length")));
        }
        if (props.containsKey("iv.length")) {
            builder.ivLength(Integer.parseInt(props.get("iv.length")));
        }
        if (props.containsKey("tag.length")) {
            builder.tagLength(Integer.parseInt(props.get("tag.length")));
        }
        if (props.containsKey("salt.length")) {
            builder.saltLength(Integer.parseInt(props.get("salt.length")));
        }
        if (props.containsKey("pbkdf2.iterations")) {
            builder.pbkdf2Iterations(Integer.parseInt(props.get("pbkdf2.iterations")));
        }
        if (props.containsKey("pbkdf2.algorithm")) {
            builder.pbkdf2Algorithm(props.get("pbkdf2.algorithm"));
        }
        if (props.containsKey("min.password.length")) {
            builder.minPasswordLength(Integer.parseInt(props.get("min.password.length")));
        }
        if (props.containsKey("max.data.size.bytes")) {
            builder.maxDataSizeBytes(Integer.parseInt(props.get("max.data.size.bytes")));
        }
        if (props.containsKey("validate.encryption.format")) {
            builder.validateEncryptionFormat(Boolean.parseBoolean(props.get("validate.encryption.format")));
        }
        if (props.containsKey("enable.compression")) {
            builder.enableCompression(Boolean.parseBoolean(props.get("enable.compression")));
        }
        if (props.containsKey("secure.random.enabled")) {
            builder.secureRandomEnabled(Boolean.parseBoolean(props.get("secure.random.enabled")));
        }
        if (props.containsKey("metadata.encryption.enabled")) {
            builder.metadataEncryptionEnabled(Boolean.parseBoolean(props.get("metadata.encryption.enabled")));
        }
        if (props.containsKey("corruption.detection.enabled")) {
            builder.corruptionDetectionEnabled(Boolean.parseBoolean(props.get("corruption.detection.enabled")));
        }
        
        return builder.build();
    }
    
    /**
     * Convert EncryptionConfig to Properties
     * @param config EncryptionConfig to convert
     * @return Properties containing encryption configuration
     */
    public static Properties encryptionConfigToProperties(EncryptionConfig config) {
        Properties props = new Properties();
        populatePropertiesFromEncryptionConfig(props, config);
        return props;
    }
    
    /**
     * Convert EncryptionConfig to Map
     * @param config EncryptionConfig to convert
     * @return Map containing encryption configuration
     */
    public static Map<String, String> encryptionConfigToMap(EncryptionConfig config) {
        Map<String, String> props = new HashMap<>();
        
        props.put("algorithm", config.getEncryptionAlgorithm());
        props.put("mode", config.getEncryptionMode());
        props.put("padding", config.getPadding());
        props.put("key.length", String.valueOf(config.getKeyLength()));
        props.put("iv.length", String.valueOf(config.getIvLength()));
        props.put("tag.length", String.valueOf(config.getTagLength()));
        props.put("salt.length", String.valueOf(config.getSaltLength()));
        props.put("pbkdf2.iterations", String.valueOf(config.getPbkdf2Iterations()));
        props.put("pbkdf2.algorithm", config.getPbkdf2Algorithm());
        props.put("min.password.length", String.valueOf(config.getMinPasswordLength()));
        props.put("max.data.size.bytes", String.valueOf(config.getMaxDataSizeBytes()));
        props.put("validate.encryption.format", String.valueOf(config.isValidateEncryptionFormat()));
        props.put("enable.compression", String.valueOf(config.isEnableCompression()));
        props.put("secure.random.enabled", String.valueOf(config.isSecureRandomEnabled()));
        props.put("metadata.encryption.enabled", String.valueOf(config.isMetadataEncryptionEnabled()));
        props.put("corruption.detection.enabled", String.valueOf(config.isCorruptionDetectionEnabled()));
        
        return props;
    }
    
    /**
     * Populate Properties from EncryptionConfig
     * @param props Properties to populate
     * @param config EncryptionConfig source
     */
    public static void populatePropertiesFromEncryptionConfig(Properties props, EncryptionConfig config) {
        props.setProperty("algorithm", config.getEncryptionAlgorithm());
        props.setProperty("mode", config.getEncryptionMode());
        props.setProperty("padding", config.getPadding());
        props.setProperty("key.length", String.valueOf(config.getKeyLength()));
        props.setProperty("iv.length", String.valueOf(config.getIvLength()));
        props.setProperty("tag.length", String.valueOf(config.getTagLength()));
        props.setProperty("salt.length", String.valueOf(config.getSaltLength()));
        props.setProperty("pbkdf2.iterations", String.valueOf(config.getPbkdf2Iterations()));
        props.setProperty("pbkdf2.algorithm", config.getPbkdf2Algorithm());
        props.setProperty("min.password.length", String.valueOf(config.getMinPasswordLength()));
        props.setProperty("max.data.size.bytes", String.valueOf(config.getMaxDataSizeBytes()));
        props.setProperty("validate.encryption.format", String.valueOf(config.isValidateEncryptionFormat()));
        props.setProperty("enable.compression", String.valueOf(config.isEnableCompression()));
        props.setProperty("secure.random.enabled", String.valueOf(config.isSecureRandomEnabled()));
        props.setProperty("metadata.encryption.enabled", String.valueOf(config.isMetadataEncryptionEnabled()));
        props.setProperty("corruption.detection.enabled", String.valueOf(config.isCorruptionDetectionEnabled()));
    }
    
    /**
     * Convert Properties to String Map
     * @param props Properties to convert
     * @return Map containing string key-value pairs
     */
    public static Map<String, String> propertiesToStringMap(Properties props) {
        Map<String, String> map = new HashMap<>();
        for (String key : props.stringPropertyNames()) {
            map.put(key, props.getProperty(key));
        }
        return map;
    }
    
    /**
     * Convert String Map to Properties
     * @param map Map to convert
     * @return Properties containing the map data
     */
    public static Properties stringMapToProperties(Map<String, String> map) {
        Properties props = new Properties();
        map.forEach(props::setProperty);
        return props;
    }
    
    /**
     * Merge two configuration maps, with priority map overriding base map
     * @param baseConfig Base configuration map
     * @param priorityConfig Priority configuration map (overrides base)
     * @return Merged configuration map
     */
    public static Map<String, String> mergeConfigMaps(Map<String, String> baseConfig, Map<String, String> priorityConfig) {
        Map<String, String> merged = new HashMap<>(baseConfig);
        merged.putAll(priorityConfig);
        return merged;
    }
    
    /**
     * Validate configuration map for required keys
     * @param config Configuration map to validate
     * @param requiredKeys Array of required keys
     * @return true if all required keys are present
     */
    public static boolean validateRequiredKeys(Map<String, String> config, String... requiredKeys) {
        for (String key : requiredKeys) {
            if (!config.containsKey(key) || config.get(key) == null || config.get(key).trim().isEmpty()) {
                logger.warn("Missing or empty required configuration key: {}", key);
                return false;
            }
        }
        return true;
    }
    
    /**
     * Get configuration value with default fallback
     * @param config Configuration map
     * @param key Configuration key
     * @param defaultValue Default value if key not found
     * @return Configuration value or default
     */
    public static String getConfigValue(Map<String, String> config, String key, String defaultValue) {
        return config.getOrDefault(key, defaultValue);
    }
    
    /**
     * Get configuration value as integer with default fallback
     * @param config Configuration map
     * @param key Configuration key
     * @param defaultValue Default value if key not found or invalid
     * @return Configuration value as integer or default
     */
    public static int getConfigValueAsInt(Map<String, String> config, String key, int defaultValue) {
        try {
            String value = config.get(key);
            return value != null ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            logger.warn("Invalid integer value for key {}: {}", key, config.get(key));
            return defaultValue;
        }
    }
    
    /**
     * Get configuration value as boolean with default fallback
     * @param config Configuration map
     * @param key Configuration key
     * @param defaultValue Default value if key not found
     * @return Configuration value as boolean or default
     */
    public static boolean getConfigValueAsBoolean(Map<String, String> config, String key, boolean defaultValue) {
        String value = config.get(key);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }
    
    /**
     * Get configuration value as long with default fallback
     * @param config Configuration map
     * @param key Configuration key
     * @param defaultValue Default value if key not found or invalid
     * @return Configuration value as long or default
     */
    public static long getConfigValueAsLong(Map<String, String> config, String key, long defaultValue) {
        try {
            String value = config.get(key);
            return value != null ? Long.parseLong(value) : defaultValue;
        } catch (NumberFormatException e) {
            logger.warn("Invalid long value for key {}: {}", key, config.get(key));
            return defaultValue;
        }
    }
}