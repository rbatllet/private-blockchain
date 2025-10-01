package com.rbatllet.blockchain.config;

import com.rbatllet.blockchain.entity.ConfigurationAuditEntity;
import com.rbatllet.blockchain.entity.ConfigurationEntity;
import com.rbatllet.blockchain.util.JPAUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Database-agnostic configuration storage implementation using JPA
 * Replaces DatabaseConfigurationStorage with full database portability
 */
public class JPAConfigurationStorage implements ConfigurationStorage {

    private static final Logger logger = LoggerFactory.getLogger(JPAConfigurationStorage.class);

    /**
     * Create JPA-based configuration storage
     */
    public JPAConfigurationStorage() {
        logger.debug("Initialized JPA configuration storage (database-agnostic)");
    }

    @Override
    public Map<String, String> loadConfiguration(String configType) {
        if (configType == null) {
            logger.warn("Configuration type cannot be null");
            return new HashMap<>();
        }

        logger.debug("Loading configuration from database for type: {}", configType);

        return JPAUtil.executeInTransaction(em -> {
            TypedQuery<ConfigurationEntity> query = em.createQuery(
                "SELECT c FROM ConfigurationEntity c WHERE c.configType = :type",
                ConfigurationEntity.class
            );
            query.setParameter("type", configType);
            List<ConfigurationEntity> entities = query.getResultList();

            Map<String, String> config = new HashMap<>();
            for (ConfigurationEntity entity : entities) {
                if (entity != null) {  // Defensive programming
                    config.put(entity.getConfigKey(), entity.getConfigValue());
                }
            }

            logger.debug("Loaded {} configuration entries for type: {}", config.size(), configType);
            return config;
        });
    }

    @Override
    public boolean saveConfiguration(String configType, Map<String, String> configuration) {
        if (configType == null || configuration == null) {
            logger.warn("Configuration type and configuration map cannot be null");
            return false;
        }

        logger.debug("Saving {} configuration entries for type: {}", configuration.size(), configType);

        try {
            boolean result = JPAUtil.executeInTransaction(em -> {
                // Delete existing configuration for this type
                em.createQuery("DELETE FROM ConfigurationEntity c WHERE c.configType = :type")
                    .setParameter("type", configType)
                    .executeUpdate();

                // Insert new configuration
                for (Map.Entry<String, String> entry : configuration.entrySet()) {
                    ConfigurationEntity entity = new ConfigurationEntity(
                        entry.getKey(),
                        configType,
                        entry.getValue()
                    );
                    em.persist(entity);

                    // Audit log
                    auditConfigChange(em, entry.getKey(), configType, null,
                        entry.getValue(), "SAVE", "Batch configuration save");
                }

                // Flush to ensure entities are written to database
                em.flush();

                logger.info("Saved configuration for type: {}", configType);
                return true;
            });

            // Force cleanup of ThreadLocal EntityManager to ensure fresh reads
            JPAUtil.cleanupThreadLocals();

            return result;
        } catch (Exception e) {
            logger.error("Failed to save configuration for type {}: {}", configType, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean resetConfiguration(String configType) {
        if (configType == null) {
            logger.warn("Configuration type cannot be null");
            return false;
        }

        logger.debug("Resetting configuration for type: {}", configType);

        try {
            return JPAUtil.executeInTransaction(em -> {
                int deleted = em.createQuery("DELETE FROM ConfigurationEntity c WHERE c.configType = :type")
                    .setParameter("type", configType)
                    .executeUpdate();

                auditConfigChange(em, "*", configType, "various", null,
                    "RESET", "Configuration reset to defaults");

                logger.info("Reset {} configuration entries for type: {}", deleted, configType);
                return true;
            });
        } catch (Exception e) {
            logger.error("Failed to reset configuration for type {}: {}", configType, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean configurationExists(String configType) {
        if (configType == null) {
            logger.warn("Configuration type cannot be null");
            return false;
        }

        try {
            return JPAUtil.executeInTransaction(em -> {
                Long count = em.createQuery(
                    "SELECT COUNT(c) FROM ConfigurationEntity c WHERE c.configType = :type",
                    Long.class
                ).setParameter("type", configType)
                 .getSingleResult();
                return count > 0;
            });
        } catch (Exception e) {
            logger.warn("Failed to check if configuration exists for type {}: {}", configType, e.getMessage());
            return false;
        }
    }

    @Override
    public String getConfigurationValue(String configType, String key) {
        if (configType == null || key == null) {
            logger.warn("Configuration type and key cannot be null");
            return null;
        }

        try {
            return JPAUtil.executeInTransaction(em -> {
                TypedQuery<String> query = em.createQuery(
                    "SELECT c.configValue FROM ConfigurationEntity c WHERE c.configKey = :key AND c.configType = :type",
                    String.class
                );
                query.setParameter("key", key);
                query.setParameter("type", configType);
                List<String> results = query.getResultList();
                return results.isEmpty() ? null : results.get(0);
            });
        } catch (Exception e) {
            logger.warn("Failed to get config value for key {} and type {}: {}", key, configType, e.getMessage());
            return null;
        }
    }

    @Override
    public boolean setConfigurationValue(String configType, String key, String value) {
        if (configType == null || key == null || value == null) {
            logger.warn("Configuration type, key, and value cannot be null");
            return false;
        }

        logger.debug("Setting configuration value: {} = {} for type: {}", key, value, configType);

        try {
            return JPAUtil.executeInTransaction(em -> {
                String oldValue = getConfigurationValue(configType, key);

                // Find existing or create new
                TypedQuery<ConfigurationEntity> query = em.createQuery(
                    "SELECT c FROM ConfigurationEntity c WHERE c.configKey = :key AND c.configType = :type",
                    ConfigurationEntity.class
                );
                query.setParameter("key", key);
                query.setParameter("type", configType);
                List<ConfigurationEntity> results = query.getResultList();

                if (results.isEmpty()) {
                    // Create new
                    ConfigurationEntity entity = new ConfigurationEntity(key, configType, value);
                    em.persist(entity);
                } else {
                    // Update existing
                    ConfigurationEntity entity = results.get(0);
                    entity.setConfigValue(value);
                    em.merge(entity);
                }

                auditConfigChange(em, key, configType, oldValue, value,
                    "SET", "Individual configuration change");
                return true;
            });
        } catch (Exception e) {
            logger.error("Failed to set configuration value for key {} and type {}: {}", key, configType, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean deleteConfigurationValue(String configType, String key) {
        if (configType == null || key == null) {
            logger.warn("Configuration type and key cannot be null");
            return false;
        }

        logger.debug("Deleting configuration value: {} for type: {}", key, configType);

        try {
            return JPAUtil.executeInTransaction(em -> {
                String oldValue = getConfigurationValue(configType, key);

                int deleted = em.createQuery(
                    "DELETE FROM ConfigurationEntity c WHERE c.configKey = :key AND c.configType = :type"
                ).setParameter("key", key)
                 .setParameter("type", configType)
                 .executeUpdate();

                if (deleted > 0) {
                    auditConfigChange(em, key, configType, oldValue, null,
                        "DELETE", "Individual configuration key deletion");
                    logger.info("Deleted configuration value: {} for type: {}", key, configType);
                    return true;
                }
                return false;
            });
        } catch (Exception e) {
            logger.error("Failed to delete configuration value for key {} and type {}: {}", key, configType, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean exportConfiguration(String configType, Path exportPath) {
        if (configType == null || exportPath == null) {
            logger.warn("Configuration type and export path cannot be null");
            return false;
        }

        logger.debug("Exporting configuration for type {} to: {}", configType, exportPath);

        try {
            Map<String, String> config = loadConfiguration(configType);
            Properties props = new Properties();
            props.putAll(config);

            try (var output = Files.newOutputStream(exportPath)) {
                props.store(output, "Exported Configuration for type: " + configType);
                logger.info("Exported configuration for type {} to: {}", configType, exportPath);
            }

            return true;
        } catch (Exception e) {
            logger.error("Failed to export configuration for type {}: {}", configType, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean importConfiguration(String configType, Path importPath) {
        if (configType == null || importPath == null) {
            logger.warn("Configuration type and import path cannot be null");
            return false;
        }

        logger.debug("Importing configuration for type {} from: {}", configType, importPath);

        try {
            if (!Files.exists(importPath)) {
                logger.error("Import file does not exist: {}", importPath);
                return false;
            }

            Properties props = new Properties();
            try (var input = Files.newInputStream(importPath)) {
                props.load(input);
            }

            Map<String, String> config = new HashMap<>();
            for (String key : props.stringPropertyNames()) {
                config.put(key, props.getProperty(key));
            }

            boolean success = saveConfiguration(configType, config);
            if (success) {
                JPAUtil.executeInTransaction(em -> {
                    auditConfigChange(em, "*", configType, "various", "imported",
                        "IMPORT", "Configuration imported from " + importPath);
                    return null;
                });
                logger.info("Imported configuration for type {} from: {}", configType, importPath);
            }

            return success;
        } catch (Exception e) {
            logger.error("Failed to import configuration for type {}: {}", configType, e.getMessage());
            return false;
        }
    }

    @Override
    public String getStorageType() {
        return "jpa-database";
    }

    @Override
    public String getStorageLocation() {
        DatabaseConfig config = JPAUtil.getCurrentConfig();
        if (config != null) {
            return config.getDatabaseType() + " (" + config.getDatabaseUrl() + ")";
        }
        return "JPA Database (default)";
    }

    @Override
    public boolean isHealthy() {
        try {
            return JPAUtil.executeInTransaction(em -> {
                // Test query to check database connectivity
                em.createQuery(
                    "SELECT COUNT(c) FROM ConfigurationEntity c",
                    Long.class
                ).getSingleResult();
                return true;
            });
        } catch (Exception e) {
            logger.warn("Database storage health check failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean initialize() {
        try {
            // JPA handles table creation via hbm2ddl.auto
            // Just verify we can connect
            return isHealthy();
        } catch (Exception e) {
            logger.error("Failed to initialize JPA configuration storage: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void cleanup() {
        // JPAUtil handles connection cleanup
        logger.debug("JPA configuration storage cleanup completed");
    }

    @Override
    public String getAuditLog(String configType, int limit) {
        try {
            return JPAUtil.executeInTransaction(em -> {
                StringBuilder log = new StringBuilder();
                log.append("📋 Configuration Audit Log (").append(configType).append(")\n");
                log.append("=".repeat(50)).append("\n");

                TypedQuery<ConfigurationAuditEntity> query = em.createQuery(
                    "SELECT a FROM ConfigurationAuditEntity a WHERE a.configType = :type ORDER BY a.changedAt DESC",
                    ConfigurationAuditEntity.class
                );
                query.setParameter("type", configType);
                query.setMaxResults(limit);
                List<ConfigurationAuditEntity> auditEntries = query.getResultList();

                for (ConfigurationAuditEntity entry : auditEntries) {
                    log.append(String.format("📅 %s | %s | %s | %s\n",
                        entry.getChangedAt(),
                        entry.getOperation(),
                        entry.getConfigKey(),
                        entry.getChangeReason()
                    ));
                }

                return log.toString();
            });
        } catch (Exception e) {
            logger.error("Failed to get audit log for type {}: {}", configType, e.getMessage());
            return "❌ Failed to retrieve audit log: " + e.getMessage();
        }
    }

    // Private helper methods

    private void auditConfigChange(EntityManager em, String key, String configType,
                                   String oldValue, String newValue,
                                   String operation, String reason) {
        try {
            ConfigurationAuditEntity audit = new ConfigurationAuditEntity(
                key, configType, oldValue, newValue, operation, reason
            );
            em.persist(audit);
        } catch (Exception e) {
            logger.warn("Failed to log configuration change: {}", e.getMessage());
        }
    }
}
