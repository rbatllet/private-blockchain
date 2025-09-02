package com.rbatllet.blockchain.service;

import com.rbatllet.blockchain.config.ConfigurationStorage;
import com.rbatllet.blockchain.config.DatabaseConfigurationStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Centralized configuration service for the blockchain system
 * Provides unified access to configuration storage with caching and validation
 */
public class ConfigurationService {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationService.class);
    
    private static volatile ConfigurationService instance;
    private static final Object lock = new Object();
    
    private final ConfigurationStorage storage;
    private final Map<String, Map<String, String>> configCache;
    private final boolean cachingEnabled;
    
    /**
     * Private constructor for singleton pattern
     */
    private ConfigurationService() {
        this(new DatabaseConfigurationStorage(), true);
    }
    
    /**
     * Constructor with custom storage and caching settings
     * @param storage Configuration storage implementation
     * @param cachingEnabled Whether to enable configuration caching
     */
    private ConfigurationService(ConfigurationStorage storage, boolean cachingEnabled) {
        this.storage = storage;
        this.cachingEnabled = cachingEnabled;
        this.configCache = cachingEnabled ? new ConcurrentHashMap<>() : null;
        
        if (!storage.initialize()) {
            logger.warn("Configuration storage initialization failed");
        }
        
        logger.info("Configuration service initialized with {} storage, caching: {}", 
            storage.getStorageType(), cachingEnabled);
    }
    
    /**
     * Get singleton instance with default configuration
     * @return ConfigurationService instance
     */
    public static ConfigurationService getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new ConfigurationService();
                }
            }
        }
        return instance;
    }
    
    /**
     * Get singleton instance with custom storage
     * @param storage Custom configuration storage
     * @return ConfigurationService instance
     */
    public static ConfigurationService getInstance(ConfigurationStorage storage) {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new ConfigurationService(storage, true);
                }
            }
        }
        return instance;
    }
    
    /**
     * Load configuration for a specific type
     * @param configType Configuration type (e.g., "CLI", "WEB", "API")
     * @return Configuration as key-value map
     */
    public Map<String, String> loadConfiguration(String configType) {
        logger.debug("Loading configuration for type: {}", configType);
        
        // Check cache first
        if (cachingEnabled && configCache.containsKey(configType)) {
            logger.debug("Returning cached configuration for type: {}", configType);
            return new ConcurrentHashMap<>(configCache.get(configType));
        }
        
        // Load from storage
        Map<String, String> config = storage.loadConfiguration(configType);
        
        // Cache the result
        if (cachingEnabled) {
            configCache.put(configType, new ConcurrentHashMap<>(config));
        }
        
        return config;
    }
    
    /**
     * Save configuration for a specific type
     * @param configType Configuration type
     * @param configuration Configuration as key-value map
     * @return true if saved successfully
     */
    public boolean saveConfiguration(String configType, Map<String, String> configuration) {
        logger.debug("Saving configuration for type: {}", configType);
        
        boolean success = storage.saveConfiguration(configType, configuration);
        
        if (success && cachingEnabled) {
            // Update cache
            configCache.put(configType, new ConcurrentHashMap<>(configuration));
            logger.debug("Updated cache for configuration type: {}", configType);
        }
        
        return success;
    }
    
    /**
     * Get a specific configuration value
     * @param configType Configuration type
     * @param key Configuration key
     * @return Configuration value or null if not found
     */
    public String getConfigurationValue(String configType, String key) {
        // Try cache first
        if (cachingEnabled && configCache.containsKey(configType)) {
            Map<String, String> cached = configCache.get(configType);
            if (cached.containsKey(key)) {
                return cached.get(key);
            }
        }
        
        // Fallback to storage
        return storage.getConfigurationValue(configType, key);
    }
    
    /**
     * Set a specific configuration value
     * @param configType Configuration type
     * @param key Configuration key
     * @param value Configuration value
     * @return true if set successfully
     */
    public boolean setConfigurationValue(String configType, String key, String value) {
        logger.debug("Setting configuration value: {} = {} for type: {}", key, value, configType);
        
        boolean success = storage.setConfigurationValue(configType, key, value);
        
        if (success && cachingEnabled) {
            // Update cache
            configCache.computeIfAbsent(configType, k -> new ConcurrentHashMap<>()).put(key, value);
        }
        
        return success;
    }
    
    /**
     * Delete a specific configuration value
     * @param configType Configuration type
     * @param key Configuration key
     * @return true if deleted successfully
     */
    public boolean deleteConfigurationValue(String configType, String key) {
        logger.debug("Deleting configuration value: {} = ? for type: {}", key, configType);
        
        boolean success = storage.deleteConfigurationValue(configType, key);
        
        if (success && cachingEnabled) {
            // Update cache by removing the key
            configCache.computeIfPresent(configType, (k, v) -> {
                v.remove(key);
                return v;
            });
        }
        
        return success;
    }
    
    /**
     * Reset configuration to defaults
     * @param configType Configuration type
     * @return true if reset successfully
     */
    public boolean resetConfiguration(String configType) {
        logger.info("Resetting configuration for type: {}", configType);
        
        boolean success = storage.resetConfiguration(configType);
        
        if (success && cachingEnabled) {
            // Clear cache
            configCache.remove(configType);
        }
        
        return success;
    }
    
    /**
     * Check if configuration exists
     * @param configType Configuration type
     * @return true if configuration exists
     */
    public boolean configurationExists(String configType) {
        return storage.configurationExists(configType);
    }
    
    /**
     * Export configuration to file
     * @param configType Configuration type
     * @param exportPath Path to export file
     * @return true if exported successfully
     */
    public boolean exportConfiguration(String configType, Path exportPath) {
        return storage.exportConfiguration(configType, exportPath);
    }
    
    /**
     * Import configuration from file
     * @param configType Configuration type
     * @param importPath Path to import file
     * @return true if imported successfully
     */
    public boolean importConfiguration(String configType, Path importPath) {
        boolean success = storage.importConfiguration(configType, importPath);
        
        if (success && cachingEnabled) {
            // Clear cache to force reload
            configCache.remove(configType);
        }
        
        return success;
    }
    
    /**
     * Get configuration audit log
     * @param configType Configuration type
     * @param limit Maximum number of entries
     * @return Audit log as formatted string
     */
    public String getAuditLog(String configType, int limit) {
        return storage.getAuditLog(configType, limit);
    }
    
    /**
     * Clear configuration cache
     * @param configType Configuration type to clear, or null for all
     */
    public void clearCache(String configType) {
        if (!cachingEnabled) {
            return;
        }
        
        if (configType == null) {
            configCache.clear();
            logger.debug("Cleared all configuration cache");
        } else {
            configCache.remove(configType);
            logger.debug("Cleared cache for configuration type: {}", configType);
        }
    }
    
    /**
     * Get cache statistics
     * @return Cache statistics as formatted string
     */
    public String getCacheStatistics() {
        if (!cachingEnabled) {
            return "📊 Cache Statistics: Caching disabled";
        }
        
        StringBuilder stats = new StringBuilder();
        stats.append("📊 Configuration Cache Statistics:\n");
        stats.append("-" .repeat(40)).append("\n");
        stats.append(String.format("Total cached types: %d\n", configCache.size()));
        
        for (Map.Entry<String, Map<String, String>> entry : configCache.entrySet()) {
            stats.append(String.format("  %s: %d entries\n", 
                entry.getKey(), entry.getValue().size()));
        }
        
        return stats.toString();
    }
    
    /**
     * Get service health status
     * @return Health status information
     */
    public String getHealthStatus() {
        StringBuilder health = new StringBuilder();
        health.append("🏥 Configuration Service Health:\n");
        health.append("-" .repeat(35)).append("\n");
        
        boolean storageHealthy = storage.isHealthy();
        health.append(String.format("%s Storage: %s\n", 
            storageHealthy ? "✅" : "❌",
            storageHealthy ? "Healthy" : "Unhealthy"));
        
        health.append(String.format("📁 Storage Type: %s\n", storage.getStorageType()));
        health.append(String.format("📂 Storage Location: %s\n", storage.getStorageLocation()));
        health.append(String.format("💾 Caching: %s\n", cachingEnabled ? "Enabled" : "Disabled"));
        
        if (cachingEnabled) {
            health.append(String.format("📊 Cached Types: %d\n", configCache.size()));
        }
        
        return health.toString();
    }
    
    /**
     * Get storage information
     * @return Storage information
     */
    public ConfigurationStorage getStorage() {
        return storage;
    }
    
    /**
     * Check if caching is enabled
     * @return true if caching is enabled
     */
    public boolean isCachingEnabled() {
        return cachingEnabled;
    }
    
    /**
     * Shutdown the configuration service
     */
    public void shutdown() {
        logger.info("Shutting down configuration service");
        
        if (cachingEnabled) {
            configCache.clear();
        }
        
        storage.cleanup();
        
        synchronized (lock) {
            instance = null;
        }
    }
}