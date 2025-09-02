package com.rbatllet.blockchain.config;

import java.nio.file.Path;
import java.util.Map;

/**
 * Interface for configuration storage implementations
 * Supports different storage backends (files, database, hybrid)
 */
public interface ConfigurationStorage {
    
    /**
     * Load configuration from storage
     * @param configType Type of configuration (e.g., "CLI", "WEB", "API")
     * @return Configuration as key-value map
     */
    Map<String, String> loadConfiguration(String configType);
    
    /**
     * Save configuration to storage
     * @param configType Type of configuration
     * @param configuration Configuration as key-value map
     * @return true if saved successfully
     */
    boolean saveConfiguration(String configType, Map<String, String> configuration);
    
    /**
     * Reset configuration to defaults
     * @param configType Type of configuration
     * @return true if reset successfully
     */
    boolean resetConfiguration(String configType);
    
    /**
     * Check if configuration exists in storage
     * @param configType Type of configuration
     * @return true if configuration exists
     */
    boolean configurationExists(String configType);
    
    /**
     * Get a specific configuration value
     * @param configType Type of configuration
     * @param key Configuration key
     * @return Configuration value or null if not found
     */
    String getConfigurationValue(String configType, String key);
    
    /**
     * Set a specific configuration value
     * @param configType Type of configuration
     * @param key Configuration key
     * @param value Configuration value
     * @return true if set successfully
     */
    boolean setConfigurationValue(String configType, String key, String value);
    
    /**
     * Delete a specific configuration value
     * @param configType Type of configuration
     * @param key Configuration key
     * @return true if deleted successfully
     */
    boolean deleteConfigurationValue(String configType, String key);
    
    /**
     * Export configuration to a specific file
     * @param configType Type of configuration
     * @param exportPath Path to export file
     * @return true if exported successfully
     */
    boolean exportConfiguration(String configType, Path exportPath);
    
    /**
     * Import configuration from a specific file
     * @param configType Type of configuration
     * @param importPath Path to import file
     * @return true if imported successfully
     */
    boolean importConfiguration(String configType, Path importPath);
    
    /**
     * Get storage type identifier
     * @return Storage type (e.g., "file", "database", "hybrid")
     */
    String getStorageType();
    
    /**
     * Get storage location description
     * @return Human-readable storage location
     */
    String getStorageLocation();
    
    /**
     * Get storage health status
     * @return true if storage is healthy and accessible
     */
    boolean isHealthy();
    
    /**
     * Initialize storage (create tables, directories, etc.)
     * @return true if initialization successful
     */
    boolean initialize();
    
    /**
     * Cleanup storage resources
     */
    void cleanup();
    
    /**
     * Get configuration audit log
     * @param configType Type of configuration
     * @param limit Maximum number of entries to return
     * @return Audit log entries as formatted string
     */
    String getAuditLog(String configType, int limit);
}