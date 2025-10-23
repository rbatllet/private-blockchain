package com.rbatllet.blockchain.config.util;

import com.rbatllet.blockchain.config.DatabaseConfig;

import java.util.*;

/**
 * Resolves database configuration from multiple sources with priority handling.
 *
 * <p><b>Priority Order (highest to lowest):</b></p>
 * <ol>
 *   <li>CLI Arguments - Explicit user overrides</li>
 *   <li>Environment Variables - System-wide settings</li>
 *   <li>Configuration File - Project-specific defaults</li>
 *   <li>Application Defaults - Hardcoded fallbacks</li>
 * </ol>
 *
 * <p><b>Design Principle:</b> This class implements the "configuration layering"
 * pattern where higher-priority sources override lower-priority ones. Non-null
 * values from higher-priority sources always win, even if empty strings.</p>
 *
 * <p><b>Merging Strategy:</b></p>
 * <ul>
 *   <li>Non-null values from higher priority sources override lower priority</li>
 *   <li>Null values are skipped (fallback to next priority level)</li>
 *   <li>Empty strings are considered valid values (not null)</li>
 *   <li>Each configuration component (host, port, database, etc.) is resolved independently</li>
 * </ul>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * // Build resolver with multiple sources
 * ConfigurationPriorityResolver resolver = ConfigurationPriorityResolver.builder()
 *     .withCliArgs(cliConfig)              // Highest priority
 *     .withEnvironmentVars(envConfig)      // Second priority
 *     .withConfigFile(fileConfig)          // Third priority
 *     .withDefaults(defaultConfig)         // Lowest priority
 *     .build();
 *
 * // Resolve final configuration
 * ResolvedConfiguration resolved = resolver.resolve();
 *
 * DatabaseConfig finalConfig = resolved.getConfig();
 * Map<String, ConfigSource> sources = resolved.getSourceMap();
 *
 * // Check where 'password' came from
 * ConfigSource passwordSource = sources.get("password");
 * System.out.println("Password from: " + passwordSource); // e.g., ENVIRONMENT
 * }</pre>
 *
 * <p><b>Thread Safety:</b> This class is immutable after build() and thread-safe.
 * The builder is NOT thread-safe and should not be shared between threads.</p>
 *
 * @since 1.0.5
 * @see DatabaseConfig
 * @see ConfigurationSecurityAnalyzer
 */
public final class ConfigurationPriorityResolver {

    /**
     * Configuration sources in priority order (highest to lowest).
     */
    public enum ConfigSource {
        /** Command-line arguments (highest priority) */
        CLI_ARGS,

        /** Environment variables */
        ENVIRONMENT,

        /** Configuration file */
        FILE,

        /** Application defaults (lowest priority) */
        DEFAULT,

        /** Value not set in any source */
        NONE
    }

    private final DatabaseConfig cliConfig;
    private final DatabaseConfig envConfig;
    private final DatabaseConfig fileConfig;
    private final DatabaseConfig defaultConfig;

    /**
     * Private constructor - use builder().
     */
    private ConfigurationPriorityResolver(Builder builder) {
        this.cliConfig = builder.cliConfig;
        this.envConfig = builder.envConfig;
        this.fileConfig = builder.fileConfig;
        this.defaultConfig = builder.defaultConfig;
    }

    /**
     * Creates a new builder for ConfigurationPriorityResolver.
     *
     * @return new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Resolves configuration by merging all sources according to priority.
     *
     * <p>Each configuration property (host, port, database, username, password)
     * is resolved independently. The first non-null value wins, starting from
     * the highest priority source (CLI args) down to defaults.</p>
     *
     * @return resolved configuration with source tracking
     * @throws IllegalStateException if no valid configuration could be resolved
     */
    public ResolvedConfiguration resolve() {
        // Determine database type first (it affects which fields are needed)
        DatabaseConfig.DatabaseType type = resolveDatabaseType();

        if (type == null) {
            throw new IllegalStateException(
                "Cannot resolve database type. At least one configuration source must specify a database type."
            );
        }

        // Track where each value came from
        Map<String, ConfigSource> sourceMap = new LinkedHashMap<>();

        // Resolve based on database type
        DatabaseConfig resolvedConfig;

        switch (type) {
            case SQLITE:
                resolvedConfig = resolveSQLite(sourceMap);
                break;

            case POSTGRESQL:
                resolvedConfig = resolvePostgreSQL(sourceMap);
                break;

            case MYSQL:
                resolvedConfig = resolveMySQL(sourceMap);
                break;

            case H2:
                resolvedConfig = resolveH2(sourceMap);
                break;

            default:
                throw new IllegalStateException("Unsupported database type: " + type);
        }

        return new ResolvedConfiguration(resolvedConfig, sourceMap);
    }

    /**
     * Resolves database type from all sources.
     */
    private DatabaseConfig.DatabaseType resolveDatabaseType() {
        // Check CLI args first
        if (cliConfig != null && cliConfig.getDatabaseType() != null) {
            return cliConfig.getDatabaseType();
        }
        // Then environment
        if (envConfig != null && envConfig.getDatabaseType() != null) {
            return envConfig.getDatabaseType();
        }
        // Then file
        if (fileConfig != null && fileConfig.getDatabaseType() != null) {
            return fileConfig.getDatabaseType();
        }
        // Finally defaults
        if (defaultConfig != null && defaultConfig.getDatabaseType() != null) {
            return defaultConfig.getDatabaseType();
        }

        return null;
    }

    /**
     * Resolves SQLite configuration.
     */
    private DatabaseConfig resolveSQLite(Map<String, ConfigSource> sourceMap) {
        sourceMap.put("databaseType", resolveDatabaseTypeSource());
        // SQLite doesn't require additional configuration
        return DatabaseConfig.createSQLiteConfig();
    }

    /**
     * Resolves PostgreSQL configuration.
     */
    private DatabaseConfig resolvePostgreSQL(Map<String, ConfigSource> sourceMap) {
        String host = resolveString("host", sourceMap, "localhost");
        int port = resolveInt("port", sourceMap, 5432);
        String database = resolveString("database", sourceMap, "blockchain");
        String username = resolveString("username", sourceMap, null);
        String password = resolveString("password", sourceMap, null);

        sourceMap.put("databaseType", resolveDatabaseTypeSource());

        return DatabaseConfig.createPostgreSQLConfig(host, port, database, username, password);
    }

    /**
     * Resolves MySQL configuration.
     */
    private DatabaseConfig resolveMySQL(Map<String, ConfigSource> sourceMap) {
        String host = resolveString("host", sourceMap, "localhost");
        int port = resolveInt("port", sourceMap, 3306);
        String database = resolveString("database", sourceMap, "blockchain");
        String username = resolveString("username", sourceMap, null);
        String password = resolveString("password", sourceMap, null);

        sourceMap.put("databaseType", resolveDatabaseTypeSource());

        return DatabaseConfig.createMySQLConfig(host, port, database, username, password);
    }

    /**
     * Resolves H2 configuration.
     */
    private DatabaseConfig resolveH2(Map<String, ConfigSource> sourceMap) {
        // Check if it's memory or file mode
        String mode = resolveString("h2.mode", sourceMap, "file");
        String file = resolveString("h2.file", sourceMap, "./test-blockchain");

        sourceMap.put("databaseType", resolveDatabaseTypeSource());

        if ("memory".equalsIgnoreCase(mode)) {
            return DatabaseConfig.createH2TestConfig();
        } else {
            return DatabaseConfig.createH2FileConfig(file);
        }
    }

    /**
     * Resolves a string property from all sources.
     */
    private String resolveString(String property, Map<String, ConfigSource> sourceMap, String defaultValue) {
        String value = null;
        ConfigSource source = ConfigSource.NONE;

        // Try CLI args
        if (cliConfig != null) {
            value = getStringProperty(cliConfig, property);
            if (value != null) {
                source = ConfigSource.CLI_ARGS;
            }
        }

        // Try environment
        if (value == null && envConfig != null) {
            value = getStringProperty(envConfig, property);
            if (value != null) {
                source = ConfigSource.ENVIRONMENT;
            }
        }

        // Try config file
        if (value == null && fileConfig != null) {
            value = getStringProperty(fileConfig, property);
            if (value != null) {
                source = ConfigSource.FILE;
            }
        }

        // Try defaults
        if (value == null && defaultConfig != null) {
            value = getStringProperty(defaultConfig, property);
            if (value != null) {
                source = ConfigSource.DEFAULT;
            }
        }

        // Use provided default if still null
        if (value == null) {
            value = defaultValue;
            if (value != null) {
                source = ConfigSource.DEFAULT;
            }
        }

        sourceMap.put(property, source);
        return value;
    }

    /**
     * Resolves an integer property from all sources.
     */
    private int resolveInt(String property, Map<String, ConfigSource> sourceMap, int defaultValue) {
        Integer value = null;
        ConfigSource source = ConfigSource.NONE;

        // Try CLI args
        if (cliConfig != null) {
            value = getIntProperty(cliConfig, property);
            if (value != null) {
                source = ConfigSource.CLI_ARGS;
            }
        }

        // Try environment
        if (value == null && envConfig != null) {
            value = getIntProperty(envConfig, property);
            if (value != null) {
                source = ConfigSource.ENVIRONMENT;
            }
        }

        // Try config file
        if (value == null && fileConfig != null) {
            value = getIntProperty(fileConfig, property);
            if (value != null) {
                source = ConfigSource.FILE;
            }
        }

        // Try defaults
        if (value == null && defaultConfig != null) {
            value = getIntProperty(defaultConfig, property);
            if (value != null) {
                source = ConfigSource.DEFAULT;
            }
        }

        sourceMap.put(property, source);
        return value != null ? value : defaultValue;
    }

    /**
     * Gets database type source.
     */
    private ConfigSource resolveDatabaseTypeSource() {
        if (cliConfig != null && cliConfig.getDatabaseType() != null) {
            return ConfigSource.CLI_ARGS;
        }
        if (envConfig != null && envConfig.getDatabaseType() != null) {
            return ConfigSource.ENVIRONMENT;
        }
        if (fileConfig != null && fileConfig.getDatabaseType() != null) {
            return ConfigSource.FILE;
        }
        if (defaultConfig != null && defaultConfig.getDatabaseType() != null) {
            return ConfigSource.DEFAULT;
        }
        return ConfigSource.NONE;
    }

    /**
     * Extracts string property from DatabaseConfig.
     * Returns null if property doesn't exist or can't be extracted.
     */
    private String getStringProperty(DatabaseConfig config, String property) {
        if (config == null) {
            return null;
        }

        try {
            switch (property) {
                case "host":
                    // Extract from JDBC URL if possible
                    String url = config.getDatabaseUrl();
                    if (url != null && url.contains("//")) {
                        int start = url.indexOf("//") + 2;
                        int end = url.indexOf(":", start);
                        if (end > start) {
                            return url.substring(start, end);
                        }
                    }
                    return null;

                case "database":
                    // Extract database name from URL
                    url = config.getDatabaseUrl();
                    if (url != null && url.contains("/")) {
                        int start = url.lastIndexOf("/") + 1;
                        int end = url.indexOf("?", start);
                        if (end > 0) {
                            return url.substring(start, end);
                        } else if (start < url.length()) {
                            return url.substring(start);
                        }
                    }
                    return null;

                case "username":
                    return config.getUsername();

                case "password":
                    return config.getPassword();

                case "h2.mode":
                case "h2.file":
                    // H2 specific properties would need to be extracted from URL
                    // For now, return null (can be enhanced later)
                    return null;

                default:
                    return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extracts integer property from DatabaseConfig.
     * Returns null if property doesn't exist or can't be extracted.
     */
    private Integer getIntProperty(DatabaseConfig config, String property) {
        if (config == null) {
            return null;
        }

        try {
            if ("port".equals(property)) {
                // Extract port from JDBC URL
                String url = config.getDatabaseUrl();
                if (url != null && url.contains(":")) {
                    int start = url.indexOf("//");
                    if (start >= 0) {
                        start = url.indexOf(":", start + 2);
                        if (start >= 0) {
                            int end = url.indexOf("/", start);
                            if (end > start) {
                                String portStr = url.substring(start + 1, end);
                                return Integer.parseInt(portStr);
                            }
                        }
                    }
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Builder for ConfigurationPriorityResolver.
     */
    public static final class Builder {
        private DatabaseConfig cliConfig;
        private DatabaseConfig envConfig;
        private DatabaseConfig fileConfig;
        private DatabaseConfig defaultConfig;

        private Builder() {
        }

        /**
         * Sets configuration from CLI arguments (highest priority).
         *
         * @param cliConfig configuration from command-line arguments
         * @return this builder
         */
        public Builder withCliArgs(DatabaseConfig cliConfig) {
            this.cliConfig = cliConfig;
            return this;
        }

        /**
         * Sets configuration from environment variables.
         *
         * @param envConfig configuration from environment variables
         * @return this builder
         */
        public Builder withEnvironmentVars(DatabaseConfig envConfig) {
            this.envConfig = envConfig;
            return this;
        }

        /**
         * Sets configuration from config file.
         *
         * @param fileConfig configuration from file
         * @return this builder
         */
        public Builder withConfigFile(DatabaseConfig fileConfig) {
            this.fileConfig = fileConfig;
            return this;
        }

        /**
         * Sets default configuration (lowest priority).
         *
         * @param defaultConfig default configuration
         * @return this builder
         */
        public Builder withDefaults(DatabaseConfig defaultConfig) {
            this.defaultConfig = defaultConfig;
            return this;
        }

        /**
         * Builds the ConfigurationPriorityResolver.
         *
         * @return new resolver instance
         * @throws IllegalStateException if no configuration sources provided
         */
        public ConfigurationPriorityResolver build() {
            // At least one source must be provided
            if (cliConfig == null && envConfig == null && fileConfig == null && defaultConfig == null) {
                throw new IllegalStateException(
                    "At least one configuration source must be provided"
                );
            }

            return new ConfigurationPriorityResolver(this);
        }
    }

    /**
     * Represents a resolved configuration with source tracking.
     */
    public static final class ResolvedConfiguration {
        private final DatabaseConfig config;
        private final Map<String, ConfigSource> sourceMap;

        private ResolvedConfiguration(DatabaseConfig config, Map<String, ConfigSource> sourceMap) {
            this.config = config;
            // Create immutable copy
            this.sourceMap = Collections.unmodifiableMap(new LinkedHashMap<>(sourceMap));
        }

        /**
         * Gets the resolved database configuration.
         *
         * @return final merged configuration
         */
        public DatabaseConfig getConfig() {
            return config;
        }

        /**
         * Gets the source map showing where each property came from.
         *
         * @return immutable map of property name to config source
         */
        public Map<String, ConfigSource> getSourceMap() {
            return sourceMap;
        }

        /**
         * Gets the source for a specific property.
         *
         * @param property property name (e.g., "host", "port", "password")
         * @return config source, or NONE if not found
         */
        public ConfigSource getSource(String property) {
            return sourceMap.getOrDefault(property, ConfigSource.NONE);
        }

        /**
         * Checks if a property came from a high-priority source (CLI or Environment).
         *
         * @param property property name
         * @return true if from CLI or Environment
         */
        public boolean isHighPrioritySource(String property) {
            ConfigSource source = getSource(property);
            return source == ConfigSource.CLI_ARGS || source == ConfigSource.ENVIRONMENT;
        }

        /**
         * Returns a formatted string showing all properties and their sources.
         *
         * @return human-readable configuration report
         */
        public String toDetailedString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Resolved Configuration:\n");
            sb.append("Database Type: ").append(config.getDatabaseType())
              .append(" (from ").append(getSource("databaseType")).append(")\n");

            for (Map.Entry<String, ConfigSource> entry : sourceMap.entrySet()) {
                if (!"databaseType".equals(entry.getKey())) {
                    sb.append("  ").append(entry.getKey()).append(": ");

                    // Mask password
                    if ("password".equals(entry.getKey())) {
                        sb.append("********");
                    } else {
                        sb.append("<value>");
                    }

                    sb.append(" (from ").append(entry.getValue()).append(")\n");
                }
            }

            return sb.toString();
        }

        @Override
        public String toString() {
            return "ResolvedConfiguration{" +
                   "type=" + config.getDatabaseType() +
                   ", properties=" + sourceMap.size() +
                   '}';
        }
    }
}
