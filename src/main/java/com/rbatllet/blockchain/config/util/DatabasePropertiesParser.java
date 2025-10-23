package com.rbatllet.blockchain.config.util;

import com.rbatllet.blockchain.config.DatabaseConfig;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Parser for converting Java Properties to DatabaseConfig.
 *
 * <p>This parser reads database configuration from .properties files and converts
 * them into strongly-typed DatabaseConfig objects. It supports all database types
 * (SQLite, PostgreSQL, MySQL, H2) and validates the configuration.</p>
 *
 * <p><b>Design Principle:</b> This class only handles parsing and validation logic.
 * It does NOT know about file paths, console output, or where properties come from.
 * The presentation layer (CLI/Web) is responsible for loading properties from specific
 * locations and displaying errors.</p>
 *
 * <p><b>Property Format:</b></p>
 * <pre>
 * # Database type selection
 * db.type=postgresql
 *
 * # PostgreSQL configuration
 * db.postgresql.host=localhost
 * db.postgresql.port=5432
 * db.postgresql.database=blockchain
 * db.postgresql.username=admin
 * db.postgresql.password=secret
 *
 * # Connection pool
 * db.pool.min=10
 * db.pool.max=60
 * db.pool.connection_timeout=30000
 * db.pool.idle_timeout=600000
 * db.pool.max_lifetime=1800000
 *
 * # Hibernate
 * db.hibernate.hbm2ddl.auto=update
 * db.hibernate.show_sql=false
 * </pre>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * // Parse from InputStream
 * try (InputStream in = Files.newInputStream(Paths.get("database.properties"))) {
 *     ParseResult result = DatabasePropertiesParser.parse(in);
 *
 *     if (result.isSuccess()) {
 *         DatabaseConfig config = result.getConfig();
 *         // Use config...
 *     } else {
 *         for (String error : result.getErrors()) {
 *             System.err.println("Error: " + error);
 *         }
 *     }
 * }
 *
 * // Parse from Properties object
 * Properties props = new Properties();
 * props.load(someInputStream);
 * ParseResult result = DatabasePropertiesParser.parse(props);
 * }</pre>
 *
 * @since 1.0.5
 * @see DatabaseConfig
 * @see Properties
 */
public final class DatabasePropertiesParser {

    // Private constructor to prevent instantiation
    private DatabasePropertiesParser() {
        throw new AssertionError("Utility class cannot be instantiated");
    }

    /**
     * Parses database configuration from an InputStream.
     *
     * @param inputStream the input stream containing properties
     * @return the parse result containing config or errors
     * @throws IOException if the stream cannot be read
     */
    public static ParseResult parse(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return ParseResult.error("InputStream cannot be null");
        }

        Properties properties = new Properties();
        properties.load(inputStream);

        return parse(properties);
    }

    /**
     * Parses database configuration from a Reader.
     *
     * @param reader the reader containing properties
     * @return the parse result containing config or errors
     * @throws IOException if the reader cannot be read
     */
    public static ParseResult parse(Reader reader) throws IOException {
        if (reader == null) {
            return ParseResult.error("Reader cannot be null");
        }

        Properties properties = new Properties();
        properties.load(reader);

        return parse(properties);
    }

    /**
     * Parses database configuration from a Properties object.
     *
     * <p>This is the main parsing method. All other parse methods delegate to this one.</p>
     *
     * @param properties the properties object
     * @return the parse result containing config or errors
     */
    public static ParseResult parse(Properties properties) {
        if (properties == null) {
            return ParseResult.error("Properties cannot be null");
        }

        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // Get database type (trim whitespace and handle whitespace-only values)
        String dbType = getRequired(properties, "db.type", "sqlite").toLowerCase();

        // If dbType is still empty after trimming, use default
        if (dbType.isEmpty()) {
            dbType = "sqlite";
        }

        try {
            DatabaseConfig config;

            switch (dbType) {
                case "sqlite":
                    config = parseSQLite(properties, errors, warnings);
                    break;

                case "postgresql":
                case "postgres":
                    config = parsePostgreSQL(properties, errors, warnings);
                    break;

                case "mysql":
                    config = parseMySQL(properties, errors, warnings);
                    break;

                case "h2":
                    config = parseH2(properties, errors, warnings);
                    break;

                default:
                    return ParseResult.error("Unknown database type: " + dbType);
            }

            if (!errors.isEmpty()) {
                return ParseResult.errors(errors);
            }

            return ParseResult.success(config, warnings);

        } catch (Exception e) {
            return ParseResult.error("Failed to parse configuration: " + e.getMessage());
        }
    }

    /**
     * Parses SQLite configuration.
     */
    private static DatabaseConfig parseSQLite(Properties props, List<String> errors, List<String> warnings) {
        // SQLite currently only supports default config
        // File path configuration can be added in future versions
        return DatabaseConfig.createSQLiteConfig();
    }

    /**
     * Parses PostgreSQL configuration.
     */
    private static DatabaseConfig parsePostgreSQL(Properties props, List<String> errors, List<String> warnings) {
        // Check if using generic db.url property
        String url = getRequired(props, "db.url", null);
        if (url != null && !url.isEmpty()) {
            String username = getRequired(props, "db.user", null);
            String password = getRequired(props, "db.password", null);

            // Validate username is provided
            if (username == null || username.isEmpty()) {
                errors.add("Username is required when using db.url (db.user)");
                return null;
            }

            // Password is optional - might be in environment variable
            if (password == null || password.isEmpty()) {
                warnings.add("Password not found in properties. Ensure DB_PASSWORD environment variable is set.");
            }

            // Build config with custom URL
            return DatabaseConfig.builder()
                    .databaseType(DatabaseConfig.DatabaseType.POSTGRESQL)
                    .databaseUrl(url)
                    .username(username)
                    .password(password)
                    .poolMinSize(10)
                    .poolMaxSize(60)
                    .connectionTimeout(30000)
                    .hbm2ddlAuto("update")
                    .build();
        }

        // Standard PostgreSQL properties
        String host = getRequired(props, "db.postgresql.host", "localhost");
        int port = getInt(props, "db.postgresql.port", 5432, errors);
        String database = getRequired(props, "db.postgresql.database", "blockchain");
        String username = getRequired(props, "db.postgresql.username", null);
        String password = getRequired(props, "db.postgresql.password", null);

        // Validate required fields
        if (username == null || username.isEmpty()) {
            errors.add("PostgreSQL username is required (db.postgresql.username)");
        }

        // Password is optional - might be in environment variable
        if (password == null || password.isEmpty()) {
            warnings.add("PostgreSQL password not found in properties. Ensure DB_PASSWORD environment variable is set.");
        }

        if (!errors.isEmpty()) {
            return null;
        }

        return DatabaseConfig.createPostgreSQLConfig(host, port, database, username, password);
    }

    /**
     * Parses MySQL configuration.
     */
    private static DatabaseConfig parseMySQL(Properties props, List<String> errors, List<String> warnings) {
        // Check if using generic db.url property
        String url = getRequired(props, "db.url", null);
        if (url != null && !url.isEmpty()) {
            String username = getRequired(props, "db.user", null);
            String password = getRequired(props, "db.password", null);

            // Validate username is provided
            if (username == null || username.isEmpty()) {
                errors.add("Username is required when using db.url (db.user)");
                return null;
            }

            // Password is optional - might be in environment variable
            if (password == null || password.isEmpty()) {
                warnings.add("Password not found in properties. Ensure DB_PASSWORD environment variable is set.");
            }

            // Build config with custom URL
            return DatabaseConfig.builder()
                    .databaseType(DatabaseConfig.DatabaseType.MYSQL)
                    .databaseUrl(url)
                    .username(username)
                    .password(password)
                    .poolMinSize(10)
                    .poolMaxSize(50)
                    .connectionTimeout(30000)
                    .hbm2ddlAuto("update")
                    .build();
        }

        // Standard MySQL properties
        String host = getRequired(props, "db.mysql.host", "localhost");
        int port = getInt(props, "db.mysql.port", 3306, errors);
        String database = getRequired(props, "db.mysql.database", "blockchain");
        String username = getRequired(props, "db.mysql.username", null);
        String password = getRequired(props, "db.mysql.password", null);

        // Validate required fields
        if (username == null || username.isEmpty()) {
            errors.add("MySQL username is required (db.mysql.username)");
        }

        // Password is optional - might be in environment variable
        if (password == null || password.isEmpty()) {
            warnings.add("MySQL password not found in properties. Ensure DB_PASSWORD environment variable is set.");
        }

        if (!errors.isEmpty()) {
            return null;
        }

        return DatabaseConfig.createMySQLConfig(host, port, database, username, password);
    }

    /**
     * Parses H2 configuration.
     */
    private static DatabaseConfig parseH2(Properties props, List<String> errors, List<String> warnings) {
        String mode = props.getProperty("db.h2.mode", "file");
        String file = props.getProperty("db.h2.file", "./test-blockchain");

        warnings.add("H2 database is for testing only. NOT recommended for production use.");

        if ("memory".equalsIgnoreCase(mode)) {
            return DatabaseConfig.createH2TestConfig();
        } else {
            return DatabaseConfig.createH2FileConfig(file);
        }
    }

    /**
     * Gets a required property with a default value.
     * Trims whitespace and treats whitespace-only values as empty.
     */
    private static String getRequired(Properties props, String key, String defaultValue) {
        String value = props.getProperty(key);
        if (value != null) {
            value = value.trim();
        }
        return (value == null || value.isEmpty()) ? defaultValue : value;
    }

    /**
     * Gets an integer property.
     */
    private static int getInt(Properties props, String key, int defaultValue, List<String> errors) {
        String value = props.getProperty(key);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            errors.add("Invalid integer value for " + key + ": " + value);
            return defaultValue;
        }
    }

    /**
     * Result of parsing database properties.
     */
    public static final class ParseResult {
        private final boolean success;
        private final DatabaseConfig config;
        private final List<String> errors;
        private final List<String> warnings;

        private ParseResult(boolean success, DatabaseConfig config, List<String> errors, List<String> warnings) {
            this.success = success;
            this.config = config;
            this.errors = errors != null ? new ArrayList<>(errors) : new ArrayList<>();
            this.warnings = warnings != null ? new ArrayList<>(warnings) : new ArrayList<>();
        }

        /**
         * Creates a successful parse result.
         */
        static ParseResult success(DatabaseConfig config, List<String> warnings) {
            return new ParseResult(true, config, new ArrayList<>(), warnings);
        }

        /**
         * Creates a failed parse result with a single error.
         */
        static ParseResult error(String error) {
            List<String> errors = new ArrayList<>();
            errors.add(error);
            return new ParseResult(false, null, errors, new ArrayList<>());
        }

        /**
         * Creates a failed parse result with multiple errors.
         */
        static ParseResult errors(List<String> errors) {
            return new ParseResult(false, null, errors, new ArrayList<>());
        }

        /**
         * Returns true if parsing was successful.
         */
        public boolean isSuccess() {
            return success;
        }

        /**
         * Returns true if parsing failed.
         */
        public boolean isFailure() {
            return !success;
        }

        /**
         * Gets the parsed database configuration.
         *
         * @return the config, or null if parsing failed
         */
        public DatabaseConfig getConfig() {
            return config;
        }

        /**
         * Gets the list of errors.
         *
         * @return unmodifiable list of errors (empty if no errors)
         */
        public List<String> getErrors() {
            return new ArrayList<>(errors);
        }

        /**
         * Gets the list of warnings.
         *
         * @return unmodifiable list of warnings (empty if no warnings)
         */
        public List<String> getWarnings() {
            return new ArrayList<>(warnings);
        }

        /**
         * Returns true if there are any warnings.
         */
        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }

        /**
         * Returns true if there are any errors.
         */
        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        @Override
        public String toString() {
            return "ParseResult{" +
                   "success=" + success +
                   ", hasConfig=" + (config != null) +
                   ", errors=" + errors.size() +
                   ", warnings=" + warnings.size() +
                   '}';
        }
    }
}
