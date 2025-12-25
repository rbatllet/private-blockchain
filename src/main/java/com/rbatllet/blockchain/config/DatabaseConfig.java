package com.rbatllet.blockchain.config;

/**
 * Configuration class for database settings
 * Provides centralized management of database connection parameters
 *
 * Follows the same pattern as EncryptionConfig.java with Builder pattern and Factory methods
 */
public class DatabaseConfig {

    /**
     * Default persistence unit name used when no database type is specified
     * This is the default SQLite configuration
     */
    public static final String DEFAULT_PERSISTENCE_UNIT = "blockchainPU";

    /**
     * Supported database types with their specific drivers and dialects
     */
    public enum DatabaseType {
        SQLITE(
            "org.sqlite.JDBC",
            "org.hibernate.community.dialect.SQLiteDialect",
            "SQLite embedded database (single writer limitation)"
        ),
        POSTGRESQL(
            "org.postgresql.Driver",
            "org.hibernate.dialect.PostgreSQLDialect",
            "PostgreSQL (recommended for production with high concurrency)"
        ),
        MYSQL(
            "com.mysql.cj.jdbc.Driver",
            "org.hibernate.dialect.MySQLDialect",
            "MySQL/MariaDB database"
        ),
        H2(
            "org.h2.Driver",
            "org.hibernate.dialect.H2Dialect",
            "H2 in-memory database (ideal for testing)"
        );

        private final String driver;
        private final String dialect;
        private final String description;

        DatabaseType(String driver, String dialect, String description) {
            this.driver = driver;
            this.dialect = dialect;
            this.description = description;
        }

        public String getDriver() { return driver; }
        public String getDialect() { return dialect; }
        public String getDescription() { return description; }
    }

    // Default configuration values
    public static final int DEFAULT_SQLITE_POOL_MIN = 2;
    public static final int DEFAULT_SQLITE_POOL_MAX = 5;
    public static final int DEFAULT_POSTGRESQL_POOL_MIN = 10;
    public static final int DEFAULT_POSTGRESQL_POOL_MAX = 60;
    public static final int DEFAULT_MYSQL_POOL_MIN = 10;
    public static final int DEFAULT_MYSQL_POOL_MAX = 50;
    public static final int DEFAULT_H2_POOL_MIN = 5;
    public static final int DEFAULT_H2_POOL_MAX = 10;

    public static final String DEFAULT_HBM2DDL_AUTO = "update";
    public static final boolean DEFAULT_SHOW_SQL = false;
    public static final boolean DEFAULT_FORMAT_SQL = false;
    public static final boolean DEFAULT_HIGHLIGHT_SQL = false;

    // Instance fields
    private DatabaseType databaseType;
    private String databaseUrl;
    private String username;
    private String password;
    private int poolMinSize;
    private int poolMaxSize;
    private int connectionTimeout;
    private int idleTimeout;
    private int maxLifetime;
    private String poolName;
    private String hbm2ddlAuto;
    private boolean showSql;
    private boolean formatSql;
    private boolean highlightSql;
    private boolean enableStatistics;

    /**
     * Create configuration with default settings
     */
    public DatabaseConfig() {
        // Defaults will be set by factory methods or builder
    }

    /**
     * Create SQLite configuration (default for development and demos)
     */
    public static DatabaseConfig createSQLiteConfig() {
        return builder()
            .databaseType(DatabaseType.SQLITE)
            .databaseUrl("jdbc:sqlite:blockchain.db?journal_mode=WAL&synchronous=NORMAL&cache_size=10000&temp_store=memory&mmap_size=268435456")
            .poolMinSize(DEFAULT_SQLITE_POOL_MIN)
            .poolMaxSize(DEFAULT_SQLITE_POOL_MAX)
            .connectionTimeout(20000)
            .idleTimeout(300000)
            .maxLifetime(900000)
            .poolName("BlockchainSQLitePool")
            .hbm2ddlAuto(DEFAULT_HBM2DDL_AUTO)
            .showSql(DEFAULT_SHOW_SQL)
            .formatSql(DEFAULT_FORMAT_SQL)
            .highlightSql(DEFAULT_HIGHLIGHT_SQL)
            .enableStatistics(false)
            .build();
    }

    /**
     * Create PostgreSQL configuration (recommended for production)
     * @param host Database host
     * @param port Database port (default: 5432)
     * @param dbName Database name
     * @param username Database username
     * @param password Database password
     */
    public static DatabaseConfig createPostgreSQLConfig(String host, int port, String dbName, String username, String password) {
        return builder()
            .databaseType(DatabaseType.POSTGRESQL)
            // SECURITY: SSL/TLS enabled for encrypted connections (ssl=true&sslmode=require)
            // WARNING: sslmode=require encrypts traffic but does NOT verify server certificate (vulnerable to MITM)
            // For DEVELOPMENT: sslmode=require is acceptable
            // For PRODUCTION: use sslmode=verify-full (requires valid CA certificates) for maximum security
            .databaseUrl("jdbc:postgresql://" + host + ":" + port + "/" + dbName + "?ssl=true&sslmode=require")
            .username(username)
            .password(password)
            .poolMinSize(DEFAULT_POSTGRESQL_POOL_MIN)
            .poolMaxSize(DEFAULT_POSTGRESQL_POOL_MAX)
            .connectionTimeout(30000)
            .idleTimeout(600000)
            .maxLifetime(1800000)
            .poolName("BlockchainPostgreSQLPool")
            .hbm2ddlAuto(DEFAULT_HBM2DDL_AUTO)
            .showSql(DEFAULT_SHOW_SQL)
            .formatSql(DEFAULT_FORMAT_SQL)
            .highlightSql(DEFAULT_HIGHLIGHT_SQL)
            .enableStatistics(false)
            .build();
    }

    /**
     * Create PostgreSQL configuration with default port 5432
     */
    public static DatabaseConfig createPostgreSQLConfig(String host, String dbName, String username, String password) {
        return createPostgreSQLConfig(host, 5432, dbName, username, password);
    }

    /**
     * Create MySQL configuration
     * @param host Database host
     * @param port Database port (default: 3306)
     * @param dbName Database name
     * @param username Database username
     * @param password Database password
     */
    public static DatabaseConfig createMySQLConfig(String host, int port, String dbName, String username, String password) {
        return builder()
            .databaseType(DatabaseType.MYSQL)
            // SECURITY: SSL/TLS enabled for encrypted connections (useSSL=true&requireSSL=true)
            // IMPORTANT: MySQL server MUST have SSL/TLS configured or connection will fail
            // For self-signed certificates: add &trustServerCertificate=true
            // For valid CA certificates: add &verifyServerCertificate=true for maximum security
            .databaseUrl("jdbc:mysql://" + host + ":" + port + "/" + dbName + "?useSSL=true&requireSSL=true&allowPublicKeyRetrieval=true")
            .username(username)
            .password(password)
            .poolMinSize(DEFAULT_MYSQL_POOL_MIN)
            .poolMaxSize(DEFAULT_MYSQL_POOL_MAX)
            .connectionTimeout(30000)
            .idleTimeout(600000)
            .maxLifetime(1800000)
            .poolName("BlockchainMySQLPool")
            .hbm2ddlAuto(DEFAULT_HBM2DDL_AUTO)
            .showSql(DEFAULT_SHOW_SQL)
            .formatSql(DEFAULT_FORMAT_SQL)
            .highlightSql(DEFAULT_HIGHLIGHT_SQL)
            .enableStatistics(false)
            .build();
    }

    /**
     * Create MySQL configuration with default port 3306
     */
    public static DatabaseConfig createMySQLConfig(String host, String dbName, String username, String password) {
        return createMySQLConfig(host, 3306, dbName, username, password);
    }

    /**
     * Create H2 in-memory configuration (ideal for testing)
     */
    public static DatabaseConfig createH2TestConfig() {
        return builder()
            .databaseType(DatabaseType.H2)
            .databaseUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL")
            .username("sa")
            .password("")
            .poolMinSize(DEFAULT_H2_POOL_MIN)
            .poolMaxSize(DEFAULT_H2_POOL_MAX)
            .connectionTimeout(10000)
            .idleTimeout(300000)
            .maxLifetime(600000)
            .poolName("BlockchainH2TestPool")
            .hbm2ddlAuto("create-drop")
            .showSql(false)
            .formatSql(false)
            .highlightSql(false)
            .enableStatistics(false)
            .build();
    }

    /**
     * Create H2 file-based configuration with default file path (persistent storage)
     * Uses "./blockchain.db" as the default database file
     */
    public static DatabaseConfig createH2Config() {
        return createH2FileConfig("./blockchain");
    }

    /**
     * Create H2 file-based configuration (for persistent testing)
     */
    public static DatabaseConfig createH2FileConfig(String filePath) {
        return builder()
            .databaseType(DatabaseType.H2)
            .databaseUrl("jdbc:h2:file:" + filePath + ";MODE=PostgreSQL")
            .username("sa")
            .password("")
            .poolMinSize(DEFAULT_H2_POOL_MIN)
            .poolMaxSize(DEFAULT_H2_POOL_MAX)
            .connectionTimeout(10000)
            .idleTimeout(300000)
            .maxLifetime(600000)
            .poolName("BlockchainH2FilePool")
            .hbm2ddlAuto(DEFAULT_HBM2DDL_AUTO)
            .showSql(false)
            .formatSql(false)
            .highlightSql(false)
            .enableStatistics(false)
            .build();
    }

    /**
     * Create development configuration (SQLite with verbose logging)
     */
    public static DatabaseConfig createDevelopmentConfig() {
        return builder()
            .databaseType(DatabaseType.SQLITE)
            .databaseUrl("jdbc:sqlite:blockchain-dev.db?journal_mode=WAL")
            .poolMinSize(DEFAULT_SQLITE_POOL_MIN)
            .poolMaxSize(DEFAULT_SQLITE_POOL_MAX)
            .connectionTimeout(20000)
            .poolName("BlockchainDevPool")
            .hbm2ddlAuto("update")
            .showSql(true)
            .formatSql(true)
            .highlightSql(true)
            .enableStatistics(true)
            .build();
    }

    /**
     * Create a database configuration with custom JDBC URL.
     *
     * @param databaseType the database type
     * @param jdbcUrl the JDBC URL
     * @return database configuration
     */
    public static DatabaseConfig forDatabaseUrl(DatabaseType databaseType, String jdbcUrl) {
        return builder()
            .databaseType(databaseType)
            .databaseUrl(jdbcUrl)
            .poolMinSize(5)
            .poolMaxSize(10)
            .connectionTimeout(10000)
            .idleTimeout(300000)
            .maxLifetime(600000)
            .poolName("CustomPool")
            .hbm2ddlAuto("update")
            .showSql(false)
            .formatSql(false)
            .highlightSql(false)
            .enableStatistics(false)
            .build();
    }

    /**
     * Create a database configuration with custom JDBC URL and credentials.
     *
     * @param databaseType the database type
     * @param jdbcUrl the JDBC URL
     * @param username username for database connection
     * @param password password for database connection
     * @return database configuration
     */
    public static DatabaseConfig forDatabaseUrl(DatabaseType databaseType, String jdbcUrl, String username, String password) {
        return builder()
            .databaseType(databaseType)
            .databaseUrl(jdbcUrl)
            .username(username)
            .password(password)
            .poolMinSize(5)
            .poolMaxSize(10)
            .connectionTimeout(10000)
            .idleTimeout(300000)
            .maxLifetime(600000)
            .poolName("CustomPool")
            .hbm2ddlAuto("update")
            .showSql(false)
            .formatSql(false)
            .highlightSql(false)
            .enableStatistics(false)
            .build();
    }

    /**
     * Create production configuration (requires environment variables)
     * Environment variables:
     * - DB_TYPE: sqlite, postgresql, mysql
     * - DB_HOST: database host
     * - DB_PORT: database port
     * - DB_NAME: database name
     * - DB_USER: database username
     * - DB_PASSWORD: database password
     */
    public static DatabaseConfig createProductionConfigFromEnv() {
        String dbType = System.getenv("DB_TYPE");
        if (dbType == null || dbType.isEmpty()) {
            dbType = "sqlite"; // Default to SQLite
        }

        switch (dbType.toLowerCase()) {
            case "postgresql":
                String pgHost = getEnvOrDefault("DB_HOST", "localhost");
                int pgPort = Integer.parseInt(getEnvOrDefault("DB_PORT", "5432"));
                String pgDbName = getEnvOrDefault("DB_NAME", "blockchain_prod");
                String pgUser = getEnvOrDefault("DB_USER", "blockchain_user");
                String pgPassword = getEnvOrDefault("DB_PASSWORD", "");
                return createPostgreSQLConfig(pgHost, pgPort, pgDbName, pgUser, pgPassword);

            case "mysql":
                String mysqlHost = getEnvOrDefault("DB_HOST", "localhost");
                int mysqlPort = Integer.parseInt(getEnvOrDefault("DB_PORT", "3306"));
                String mysqlDbName = getEnvOrDefault("DB_NAME", "blockchain_prod");
                String mysqlUser = getEnvOrDefault("DB_USER", "blockchain_user");
                String mysqlPassword = getEnvOrDefault("DB_PASSWORD", "");
                return createMySQLConfig(mysqlHost, mysqlPort, mysqlDbName, mysqlUser, mysqlPassword);

            case "sqlite":
            default:
                return createSQLiteConfig();
        }
    }

    private static String getEnvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }

    /**
     * Create a builder for custom configuration
     * @return Configuration builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Get the persistence unit name for this configuration
     * @return Persistence unit name (e.g., "blockchainPU-sqlite")
     */
    public String getPersistenceUnitName() {
        if (databaseType == null) {
            return DEFAULT_PERSISTENCE_UNIT;
        }
        return DEFAULT_PERSISTENCE_UNIT + "-" + databaseType.name().toLowerCase();
    }

    /**
     * Validate the configuration settings
     * @throws IllegalArgumentException if configuration is invalid
     */
    public void validate() {
        if (databaseType == null) {
            throw new IllegalArgumentException("Database type is required");
        }

        if (databaseUrl == null || databaseUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Database URL is required");
        }

        if (poolMaxSize < poolMinSize) {
            throw new IllegalArgumentException("Pool max size must be >= min size");
        }

        if (poolMinSize < 1) {
            throw new IllegalArgumentException("Pool min size must be at least 1");
        }

        if (poolMaxSize < 1) {
            throw new IllegalArgumentException("Pool max size must be at least 1");
        }

        if (connectionTimeout < 1000) {
            throw new IllegalArgumentException("Connection timeout must be at least 1000ms");
        }

        if (hbm2ddlAuto == null || hbm2ddlAuto.trim().isEmpty()) {
            throw new IllegalArgumentException("hbm2ddl.auto setting is required");
        }

        // Validate hbm2ddl.auto values
        String hbm2ddlLower = hbm2ddlAuto.toLowerCase();
        if (!hbm2ddlLower.equals("validate") &&
            !hbm2ddlLower.equals("update") &&
            !hbm2ddlLower.equals("create") &&
            !hbm2ddlLower.equals("create-drop") &&
            !hbm2ddlLower.equals("none")) {
            throw new IllegalArgumentException(
                "hbm2ddl.auto must be one of: validate, update, create, create-drop, none");
        }
    }

    /**
     * Get a human-readable summary of the configuration
     * @return Configuration summary
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("📊 Database Configuration Summary:\n");
        sb.append("   Type: ").append(databaseType != null ? databaseType.name() : "Not set").append("\n");
        sb.append("   Description: ").append(databaseType != null ? databaseType.getDescription() : "N/A").append("\n");
        sb.append("   URL: ").append(maskPassword(databaseUrl)).append("\n");
        sb.append("   Username: ").append(username != null ? username : "N/A").append("\n");
        sb.append("   Pool Size: ").append(poolMinSize).append(" - ").append(poolMaxSize).append("\n");
        sb.append("   Pool Name: ").append(poolName != null ? poolName : "Default").append("\n");
        sb.append("   Connection Timeout: ").append(connectionTimeout).append(" ms\n");
        sb.append("   Schema Management: ").append(hbm2ddlAuto).append("\n");
        sb.append("   Show SQL: ").append(showSql ? "✅ Enabled" : "❌ Disabled").append("\n");
        sb.append("   Format SQL: ").append(formatSql ? "✅ Enabled" : "❌ Disabled").append("\n");
        sb.append("   Statistics: ").append(enableStatistics ? "✅ Enabled" : "❌ Disabled");
        return sb.toString();
    }

    private String maskPassword(String url) {
        if (url == null) return "null";
        // Simple masking for URLs that might contain passwords
        return url.replaceAll("password=[^&;]*", "password=***");
    }

    @Override
    public String toString() {
        return getSummary();
    }

    // Getters

    public DatabaseType getDatabaseType() { return databaseType; }
    public String getDatabaseUrl() { return databaseUrl; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public int getPoolMinSize() { return poolMinSize; }
    public int getPoolMaxSize() { return poolMaxSize; }
    public int getConnectionTimeout() { return connectionTimeout; }
    public int getIdleTimeout() { return idleTimeout; }
    public int getMaxLifetime() { return maxLifetime; }
    public String getPoolName() { return poolName; }
    public String getHbm2ddlAuto() { return hbm2ddlAuto; }
    public boolean isShowSql() { return showSql; }
    public boolean isFormatSql() { return formatSql; }
    public boolean isHighlightSql() { return highlightSql; }
    public boolean isEnableStatistics() { return enableStatistics; }

    // Setters (allow modification after creation)

    public void setDatabaseType(DatabaseType databaseType) {
        this.databaseType = databaseType;
    }

    public void setDatabaseUrl(String databaseUrl) {
        this.databaseUrl = databaseUrl;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setPoolMinSize(int poolMinSize) {
        this.poolMinSize = poolMinSize;
    }

    public void setPoolMaxSize(int poolMaxSize) {
        this.poolMaxSize = poolMaxSize;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public void setIdleTimeout(int idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    public void setMaxLifetime(int maxLifetime) {
        this.maxLifetime = maxLifetime;
    }

    public void setPoolName(String poolName) {
        this.poolName = poolName;
    }

    public void setHbm2ddlAuto(String hbm2ddlAuto) {
        this.hbm2ddlAuto = hbm2ddlAuto;
    }

    public void setShowSql(boolean showSql) {
        this.showSql = showSql;
    }

    public void setFormatSql(boolean formatSql) {
        this.formatSql = formatSql;
    }

    public void setHighlightSql(boolean highlightSql) {
        this.highlightSql = highlightSql;
    }

    public void setEnableStatistics(boolean enableStatistics) {
        this.enableStatistics = enableStatistics;
    }

    /**
     * Builder class for creating custom database configurations
     */
    public static class Builder {
        private final DatabaseConfig config;

        public Builder() {
            this.config = new DatabaseConfig();
        }

        public Builder databaseType(DatabaseType type) {
            config.databaseType = type;
            return this;
        }

        public Builder databaseUrl(String url) {
            config.databaseUrl = url;
            return this;
        }

        public Builder username(String username) {
            config.username = username;
            return this;
        }

        public Builder password(String password) {
            config.password = password;
            return this;
        }

        public Builder poolMinSize(int size) {
            config.poolMinSize = size;
            return this;
        }

        public Builder poolMaxSize(int size) {
            config.poolMaxSize = size;
            return this;
        }

        public Builder connectionTimeout(int timeout) {
            config.connectionTimeout = timeout;
            return this;
        }

        public Builder idleTimeout(int timeout) {
            config.idleTimeout = timeout;
            return this;
        }

        public Builder maxLifetime(int lifetime) {
            config.maxLifetime = lifetime;
            return this;
        }

        public Builder poolName(String name) {
            config.poolName = name;
            return this;
        }

        public Builder hbm2ddlAuto(String auto) {
            config.hbm2ddlAuto = auto;
            return this;
        }

        public Builder showSql(boolean show) {
            config.showSql = show;
            return this;
        }

        public Builder formatSql(boolean format) {
            config.formatSql = format;
            return this;
        }

        public Builder highlightSql(boolean highlight) {
            config.highlightSql = highlight;
            return this;
        }

        public Builder enableStatistics(boolean enable) {
            config.enableStatistics = enable;
            return this;
        }

        public DatabaseConfig build() {
            config.validate();
            return config;
        }
    }
}
