package com.rbatllet.blockchain.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Database-based configuration storage implementation
 * Stores configuration in the blockchain database with audit trail
 */
public class DatabaseConfigurationStorage implements ConfigurationStorage {

    private static final Logger logger = LoggerFactory.getLogger(
        DatabaseConfigurationStorage.class
    );

    private static final String TABLE_NAME = "configuration";
    private static final String AUDIT_TABLE_NAME = "configuration_audit";

    // SQL queries
    private static final String CREATE_CONFIG_TABLE =
        "CREATE TABLE IF NOT EXISTS " +
        TABLE_NAME +
        " (" +
        "    config_key VARCHAR(255) NOT NULL," +
        "    config_type VARCHAR(50) NOT NULL," +
        "    config_value TEXT NOT NULL," +
        "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
        "    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
        "    PRIMARY KEY (config_key, config_type)" +
        ")";

    private static final String CREATE_AUDIT_TABLE =
        "CREATE TABLE IF NOT EXISTS " +
        AUDIT_TABLE_NAME +
        " (" +
        "    id INTEGER PRIMARY KEY AUTOINCREMENT," +
        "    config_key VARCHAR(255) NOT NULL," +
        "    config_type VARCHAR(50) NOT NULL," +
        "    old_value TEXT," +
        "    new_value TEXT," +
        "    operation VARCHAR(20) NOT NULL," +
        "    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
        "    change_reason VARCHAR(255)" +
        ")";

    private static final String INSERT_OR_UPDATE_CONFIG =
        "INSERT OR REPLACE INTO " +
        TABLE_NAME +
        " (config_key, config_type, config_value, updated_at) " +
        "VALUES (?, ?, ?, CURRENT_TIMESTAMP)";

    private static final String SELECT_CONFIG =
        "SELECT config_value FROM " +
        TABLE_NAME +
        " " +
        "WHERE config_key = ? AND config_type = ?";

    private static final String SELECT_ALL_CONFIG =
        "SELECT config_key, config_value FROM " +
        TABLE_NAME +
        " " +
        "WHERE config_type = ?";

    private static final String DELETE_CONFIG =
        "DELETE FROM " +
        TABLE_NAME +
        " WHERE config_key = ? AND config_type = ?";

    private static final String DELETE_ALL_CONFIG =
        "DELETE FROM " + TABLE_NAME + " WHERE config_type = ?";

    private static final String INSERT_AUDIT_LOG =
        "INSERT INTO " +
        AUDIT_TABLE_NAME +
        " (config_key, config_type, old_value, new_value, operation, change_reason) " +
        "VALUES (?, ?, ?, ?, ?, ?)";

    private final String databasePath;
    private Connection connection;

    /**
     * Create database storage with default database path
     */
    public DatabaseConfigurationStorage() {
        this("blockchain.db");
    }

    /**
     * Create database storage with custom database path
     * @param databasePath Path to SQLite database
     */
    public DatabaseConfigurationStorage(String databasePath) {
        this.databasePath = databasePath;
        logger.debug(
            "Initialized database configuration storage: {}",
            databasePath
        );
    }

    @Override
    public Map<String, String> loadConfiguration(String configType) {
        // Handle null configType
        if (configType == null) {
            logger.warn("Configuration type cannot be null");
            return new HashMap<>();
        }

        logger.debug(
            "Loading configuration from database for type: {}",
            configType
        );

        try {
            ensureConnection();
            Map<String, String> config = new HashMap<>();

            try (
                PreparedStatement stmt = connection.prepareStatement(
                    SELECT_ALL_CONFIG
                )
            ) {
                stmt.setString(1, configType);
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    config.put(
                        rs.getString("config_key"),
                        rs.getString("config_value")
                    );
                }

                logger.debug(
                    "Loaded {} configuration entries for type: {}",
                    config.size(),
                    configType
                );
            }

            return config;
        } catch (SQLException e) {
            logger.error(
                "Failed to load configuration for type {}: {}",
                configType,
                e.getMessage()
            );
            return new HashMap<>();
        }
    }

    @Override
    public boolean saveConfiguration(
        String configType,
        Map<String, String> configuration
    ) {
        // Handle null parameters
        if (configType == null || configuration == null) {
            logger.warn(
                "Configuration type and configuration map cannot be null"
            );
            return false;
        }

        logger.debug(
            "Saving {} configuration entries for type: {}",
            configuration.size(),
            configType
        );

        try {
            ensureConnection();
            connection.setAutoCommit(false);

            // First, delete all existing configuration for this type
            try (
                PreparedStatement deleteStmt = connection.prepareStatement(
                    DELETE_ALL_CONFIG
                )
            ) {
                deleteStmt.setString(1, configType);
                deleteStmt.executeUpdate();
            }

            // Then insert the new configuration
            try (
                PreparedStatement stmt = connection.prepareStatement(
                    INSERT_OR_UPDATE_CONFIG
                )
            ) {
                for (Map.Entry<
                    String,
                    String
                > entry : configuration.entrySet()) {
                    String key = entry.getKey();
                    String newValue = entry.getValue();

                    stmt.setString(1, key);
                    stmt.setString(2, configType);
                    stmt.setString(3, newValue);
                    stmt.executeUpdate();

                    // Log change for audit
                    auditConfigChange(
                        key,
                        configType,
                        null,
                        newValue,
                        "SAVE",
                        "Batch configuration save"
                    );
                }
            }

            connection.commit();
            logger.info("Saved configuration for type: {}", configType);
            return true;
        } catch (SQLException e) {
            logger.error(
                "Failed to save configuration for type {}: {}",
                configType,
                e.getMessage()
            );
            try {
                if (connection != null) {
                    connection.rollback();
                }
            } catch (SQLException rollbackEx) {
                logger.error(
                    "Failed to rollback transaction: {}",
                    rollbackEx.getMessage()
                );
            }
            return false;
        } finally {
            try {
                if (connection != null) {
                    connection.setAutoCommit(true);
                }
            } catch (SQLException e) {
                logger.warn("Failed to reset auto-commit: {}", e.getMessage());
            }
        }
    }

    @Override
    public boolean resetConfiguration(String configType) {
        // Handle null configType
        if (configType == null) {
            logger.warn("Configuration type cannot be null");
            return false;
        }

        logger.debug("Resetting configuration for type: {}", configType);

        try {
            ensureConnection();

            try (
                PreparedStatement stmt = connection.prepareStatement(
                    DELETE_ALL_CONFIG
                )
            ) {
                stmt.setString(1, configType);
                int deleted = stmt.executeUpdate();

                auditConfigChange(
                    "*",
                    configType,
                    "various",
                    null,
                    "RESET",
                    "Configuration reset to defaults"
                );
                logger.info(
                    "Reset {} configuration entries for type: {}",
                    deleted,
                    configType
                );
            }

            return true;
        } catch (SQLException e) {
            logger.error(
                "Failed to reset configuration for type {}: {}",
                configType,
                e.getMessage()
            );
            return false;
        }
    }

    @Override
    public boolean configurationExists(String configType) {
        // Handle null configType
        if (configType == null) {
            logger.warn("Configuration type cannot be null");
            return false;
        }

        try {
            ensureConnection();

            try (
                PreparedStatement stmt = connection.prepareStatement(
                    "SELECT COUNT(*) FROM configuration WHERE config_type = ?"
                )
            ) {
                stmt.setString(1, configType);
                ResultSet rs = stmt.executeQuery();
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            logger.warn(
                "Failed to check if configuration exists for type {}: {}",
                configType,
                e.getMessage()
            );
            return false;
        }
    }

    @Override
    public String getConfigurationValue(String configType, String key) {
        // Handle null parameters
        if (configType == null || key == null) {
            logger.warn("Configuration type and key cannot be null");
            return null;
        }

        try {
            ensureConnection();

            try (
                PreparedStatement stmt = connection.prepareStatement(
                    SELECT_CONFIG
                )
            ) {
                stmt.setString(1, key);
                stmt.setString(2, configType);
                ResultSet rs = stmt.executeQuery();
                return rs.next() ? rs.getString("config_value") : null;
            }
        } catch (SQLException e) {
            logger.warn(
                "Failed to get config value for key {} and type {}: {}",
                key,
                configType,
                e.getMessage()
            );
            return null;
        }
    }

    @Override
    public boolean setConfigurationValue(
        String configType,
        String key,
        String value
    ) {
        // Handle null parameters
        if (configType == null || key == null || value == null) {
            logger.warn("Configuration type, key, and value cannot be null");
            return false;
        }

        logger.debug(
            "Setting configuration value: {} = {} for type: {}",
            key,
            value,
            configType
        );

        try {
            ensureConnection();
            String oldValue = getConfigurationValue(configType, key);

            try (
                PreparedStatement stmt = connection.prepareStatement(
                    INSERT_OR_UPDATE_CONFIG
                )
            ) {
                stmt.setString(1, key);
                stmt.setString(2, configType);
                stmt.setString(3, value);
                int updated = stmt.executeUpdate();

                if (updated > 0) {
                    auditConfigChange(
                        key,
                        configType,
                        oldValue,
                        value,
                        "SET",
                        "Individual configuration change"
                    );
                    return true;
                }
            }

            return false;
        } catch (SQLException e) {
            logger.error(
                "Failed to set configuration value for key {} and type {}: {}",
                key,
                configType,
                e.getMessage()
            );
            return false;
        }
    }

    @Override
    public boolean deleteConfigurationValue(String configType, String key) {
        // Handle null parameters
        if (configType == null || key == null) {
            logger.warn("Configuration type and key cannot be null");
            return false;
        }

        logger.debug(
            "Deleting configuration value: {} for type: {}",
            key,
            configType
        );

        try {
            ensureConnection();
            String oldValue = getConfigurationValue(configType, key);

            try (
                PreparedStatement stmt = connection.prepareStatement(
                    DELETE_CONFIG
                )
            ) {
                stmt.setString(1, key);
                stmt.setString(2, configType);
                int deleted = stmt.executeUpdate();

                if (deleted > 0) {
                    auditConfigChange(
                        key,
                        configType,
                        oldValue,
                        null,
                        "DELETE",
                        "Individual configuration key deletion"
                    );
                    logger.info(
                        "Deleted configuration value: {} for type: {}",
                        key,
                        configType
                    );
                    return true;
                }

                return false;
            }
        } catch (SQLException e) {
            logger.error(
                "Failed to delete configuration value for key {} and type {}: {}",
                key,
                configType,
                e.getMessage()
            );
            return false;
        }
    }

    @Override
    public boolean exportConfiguration(String configType, Path exportPath) {
        // Handle null parameters
        if (configType == null || exportPath == null) {
            logger.warn("Configuration type and export path cannot be null");
            return false;
        }

        logger.debug(
            "Exporting configuration for type {} to: {}",
            configType,
            exportPath
        );

        try {
            Map<String, String> config = loadConfiguration(configType);
            Properties props = new Properties();
            props.putAll(config);

            try (var output = Files.newOutputStream(exportPath)) {
                props.store(
                    output,
                    "Exported Configuration for type: " + configType
                );
                logger.info(
                    "Exported configuration for type {} to: {}",
                    configType,
                    exportPath
                );
            }

            return true;
        } catch (Exception e) {
            logger.error(
                "Failed to export configuration for type {}: {}",
                configType,
                e.getMessage()
            );
            return false;
        }
    }

    @Override
    public boolean importConfiguration(String configType, Path importPath) {
        // Handle null parameters
        if (configType == null || importPath == null) {
            logger.warn("Configuration type and import path cannot be null");
            return false;
        }

        logger.debug(
            "Importing configuration for type {} from: {}",
            configType,
            importPath
        );

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
                auditConfigChange(
                    "*",
                    configType,
                    "various",
                    "imported",
                    "IMPORT",
                    "Configuration imported from " + importPath
                );
                logger.info(
                    "Imported configuration for type {} from: {}",
                    configType,
                    importPath
                );
            }

            return success;
        } catch (Exception e) {
            logger.error(
                "Failed to import configuration for type {}: {}",
                configType,
                e.getMessage()
            );
            return false;
        }
    }

    @Override
    public String getStorageType() {
        return "database";
    }

    @Override
    public String getStorageLocation() {
        try {
            ensureConnection();
            return connection.getMetaData().getURL();
        } catch (SQLException e) {
            return "database (" + databasePath + ")";
        }
    }

    @Override
    public boolean isHealthy() {
        try {
            ensureConnection();

            // Test database connection with a simple query
            try (
                PreparedStatement stmt = connection.prepareStatement(
                    "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name=?"
                )
            ) {
                stmt.setString(1, TABLE_NAME);
                ResultSet rs = stmt.executeQuery();
                return rs.next();
            }
        } catch (SQLException e) {
            logger.warn(
                "Database storage health check failed: {}",
                e.getMessage()
            );
            return false;
        }
    }

    @Override
    public boolean initialize() {
        try {
            ensureConnection();

            // Create configuration tables
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(CREATE_CONFIG_TABLE);
                stmt.execute(CREATE_AUDIT_TABLE);
                logger.info(
                    "Database configuration storage initialized successfully"
                );
            }

            return true;
        } catch (SQLException e) {
            logger.error(
                "Failed to initialize database configuration storage: {}",
                e.getMessage()
            );
            return false;
        }
    }

    @Override
    public void cleanup() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                logger.debug(
                    "Database configuration storage cleanup completed"
                );
            }
        } catch (SQLException e) {
            logger.warn(
                "Error during database configuration storage cleanup: {}",
                e.getMessage()
            );
        }
    }

    @Override
    public String getAuditLog(String configType, int limit) {
        try {
            ensureConnection();

            StringBuilder log = new StringBuilder();
            log
                .append("📋 Configuration Audit Log (")
                .append(configType)
                .append(")\n");
            log.append("=".repeat(50)).append("\n");

            try (
                PreparedStatement stmt = connection.prepareStatement(
                    "SELECT * FROM " +
                    AUDIT_TABLE_NAME +
                    " WHERE config_type = ? ORDER BY changed_at DESC LIMIT ?"
                )
            ) {
                stmt.setString(1, configType);
                stmt.setInt(2, limit);
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    Timestamp changedAt = rs.getTimestamp("changed_at");
                    String operation = rs.getString("operation");
                    String key = rs.getString("config_key");
                    String reason = rs.getString("change_reason");

                    log.append(
                        String.format(
                            "📅 %s | %s | %s | %s\n",
                            changedAt,
                            operation,
                            key,
                            reason
                        )
                    );
                }
            }

            return log.toString();
        } catch (SQLException e) {
            logger.error(
                "Failed to get audit log for type {}: {}",
                configType,
                e.getMessage()
            );
            return "❌ Failed to retrieve audit log: " + e.getMessage();
        }
    }

    // Private helper methods

    private void ensureConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(
                "jdbc:sqlite:" + databasePath
            );
            initialize();
        }
    }

    private void auditConfigChange(
        String key,
        String configType,
        String oldValue,
        String newValue,
        String operation,
        String reason
    ) {
        try {
            try (
                PreparedStatement stmt = connection.prepareStatement(
                    INSERT_AUDIT_LOG
                )
            ) {
                stmt.setString(1, key);
                stmt.setString(2, configType);
                stmt.setString(3, oldValue);
                stmt.setString(4, newValue);
                stmt.setString(5, operation);
                stmt.setString(6, reason);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            logger.warn(
                "Failed to log configuration change: {}",
                e.getMessage()
            );
        }
    }
}
