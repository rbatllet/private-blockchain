package com.rbatllet.blockchain.config.util;

import com.rbatllet.blockchain.config.DatabaseConfig;

import java.sql.*;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tests database connections and validates configuration.
 *
 * <p>This utility performs comprehensive connection testing including:</p>
 * <ul>
 *   <li>Basic connectivity test</li>
 *   <li>Authentication verification</li>
 *   <li>Database existence check</li>
 *   <li>Permission validation (read/write)</li>
 *   <li>Response time measurement</li>
 *   <li>Version detection</li>
 * </ul>
 *
 * <p><b>Design Principle:</b> This class only performs connection testing
 * and validation. It does NOT create databases, modify schemas, or change
 * permissions. All operations are read-only (except for permission checks).</p>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * DatabaseConfig config = DatabaseConfig.createPostgreSQLConfig(
 *     "localhost", 5432, "blockchain", "admin", "password"
 * );
 *
 * // Test with default timeout (10 seconds)
 * DatabaseConnectionTester tester = new DatabaseConnectionTester();
 * ConnectionTestResult result = tester.testConnection(config);
 *
 * if (result.isSuccessful()) {
 *     System.out.println("✅ Connection successful!");
 *     System.out.println("Database version: " + result.getDatabaseVersion());
 *     System.out.println("Response time: " + result.getResponseTime().toMillis() + "ms");
 * } else {
 *     System.err.println("❌ Connection failed: " + result.getErrorMessage());
 *     for (String recommendation : result.getRecommendations()) {
 *         System.err.println("  → " + recommendation);
 *     }
 * }
 * }</pre>
 *
 * <p><b>Thread Safety:</b> This class is stateless and thread-safe.</p>
 *
 * @since 1.0.5
 * @see DatabaseConfig
 * @see ConnectionTestResult
 */
public final class DatabaseConnectionTester {

    /**
     * Default connection timeout (10 seconds).
     */
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

    /**
     * Default query timeout for test queries (5 seconds).
     */
    public static final Duration DEFAULT_QUERY_TIMEOUT = Duration.ofSeconds(5);

    private final Duration connectionTimeout;
    private final Duration queryTimeout;

    /**
     * Creates a new tester with default timeouts.
     */
    public DatabaseConnectionTester() {
        this(DEFAULT_TIMEOUT, DEFAULT_QUERY_TIMEOUT);
    }

    /**
     * Creates a new tester with custom timeouts.
     *
     * @param connectionTimeout maximum time to wait for connection
     * @param queryTimeout maximum time to wait for test queries
     * @throws IllegalArgumentException if timeouts are null or negative
     */
    public DatabaseConnectionTester(Duration connectionTimeout, Duration queryTimeout) {
        if (connectionTimeout == null || connectionTimeout.isNegative()) {
            throw new IllegalArgumentException("Connection timeout must be positive");
        }
        if (queryTimeout == null || queryTimeout.isNegative()) {
            throw new IllegalArgumentException("Query timeout must be positive");
        }

        this.connectionTimeout = connectionTimeout;
        this.queryTimeout = queryTimeout;
    }

    /**
     * Tests database connection and performs comprehensive validation.
     *
     * <p>This method performs the following checks:</p>
     * <ol>
     *   <li>JDBC driver availability</li>
     *   <li>Network connectivity to database host</li>
     *   <li>Authentication (username/password)</li>
     *   <li>Database accessibility</li>
     *   <li>Basic read permissions (SELECT query)</li>
     *   <li>Database version detection</li>
     *   <li>Response time measurement</li>
     * </ol>
     *
     * @param config database configuration to test
     * @return test result with detailed information
     * @throws IllegalArgumentException if config is null
     */
    public ConnectionTestResult testConnection(DatabaseConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("Database configuration cannot be null");
        }

        Instant startTime = Instant.now();
        List<String> recommendations = new ArrayList<>();
        ConnectionTestResult.Builder resultBuilder = ConnectionTestResult.builder()
            .config(config)
            .startTime(startTime);

        Connection connection = null;

        try {
            // Step 1: Get JDBC URL and credentials
            String jdbcUrl = config.getDatabaseUrl();
            String username = config.getUsername();
            String password = config.getPassword();

            if (jdbcUrl == null || jdbcUrl.isEmpty()) {
                return resultBuilder
                    .successful(false)
                    .errorMessage("JDBC URL is null or empty")
                    .addRecommendation("Verify database configuration is properly initialized")
                    .responseTime(Duration.between(startTime, Instant.now()))
                    .build();
            }

            // Step 2: Try to establish connection
            DriverManager.setLoginTimeout((int) connectionTimeout.getSeconds());

            if (username != null && !username.isEmpty()) {
                connection = DriverManager.getConnection(jdbcUrl, username, password);
            } else {
                // SQLite doesn't need credentials
                connection = DriverManager.getConnection(jdbcUrl);
            }

            // Step 3: Connection successful - test it
            if (!connection.isValid((int) queryTimeout.getSeconds())) {
                return resultBuilder
                    .successful(false)
                    .errorMessage("Connection established but validation failed")
                    .addRecommendation("Database server may be overloaded or unresponsive")
                    .responseTime(Duration.between(startTime, Instant.now()))
                    .build();
            }

            // Step 4: Get database metadata
            DatabaseMetaData metadata = connection.getMetaData();
            String databaseVersion = metadata.getDatabaseProductName() + " " +
                                   metadata.getDatabaseProductVersion();
            String driverVersion = metadata.getDriverName() + " " + metadata.getDriverVersion();

            resultBuilder
                .databaseVersion(databaseVersion)
                .driverVersion(driverVersion);

            // Step 5: Test read permissions with a simple query
            String testQuery = getTestQuery(config.getDatabaseType());
            if (testQuery != null) {
                try (Statement stmt = connection.createStatement()) {
                    stmt.setQueryTimeout((int) queryTimeout.getSeconds());
                    try (ResultSet rs = stmt.executeQuery(testQuery)) {
                        if (rs.next()) {
                            resultBuilder.canRead(true);
                        }
                    }
                } catch (SQLException e) {
                    resultBuilder.canRead(false);
                    recommendations.add("Read permission test failed: " + e.getMessage());
                }
            }

            // Step 6: Check if database is read-only
            boolean readOnly = connection.isReadOnly();
            resultBuilder.readOnly(readOnly);

            if (readOnly) {
                recommendations.add("Database connection is read-only. Write operations will fail.");
            }

            // Step 7: Calculate response time
            Duration responseTime = Duration.between(startTime, Instant.now());
            resultBuilder.responseTime(responseTime);

            // Add performance recommendation if slow
            if (responseTime.toMillis() > 1000) {
                recommendations.add("Connection is slow (" + responseTime.toMillis() +
                    "ms). Consider checking network latency or database load.");
            }

            // Success!
            return resultBuilder
                .successful(true)
                .addRecommendations(recommendations)
                .build();

        } catch (SQLTimeoutException e) {
            return resultBuilder
                .successful(false)
                .errorMessage("Connection timeout: " + e.getMessage())
                .addRecommendation("Increase connection timeout or check network connectivity")
                .addRecommendation("Verify database server is running and accessible")
                .addRecommendation("Check firewall rules for port " + extractPort(config))
                .responseTime(Duration.between(startTime, Instant.now()))
                .build();

        } catch (SQLException e) {
            String errorMessage = e.getMessage();
            int errorCode = e.getErrorCode();
            String sqlState = e.getSQLState();

            // Analyze error and provide specific recommendations
            analyzeError(errorCode, sqlState, errorMessage, recommendations, config);

            return resultBuilder
                .successful(false)
                .errorMessage("SQL Error [" + errorCode + ", " + sqlState + "]: " + errorMessage)
                .addRecommendations(recommendations)
                .responseTime(Duration.between(startTime, Instant.now()))
                .build();

        } catch (Exception e) {
            return resultBuilder
                .successful(false)
                .errorMessage("Unexpected error: " + e.getClass().getSimpleName() + ": " + e.getMessage())
                .addRecommendation("Check JDBC driver is in classpath")
                .addRecommendation("Verify JDBC URL format is correct")
                .responseTime(Duration.between(startTime, Instant.now()))
                .build();

        } finally {
            // Always close connection
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    // Ignore close errors
                }
            }
        }
    }

    /**
     * Gets a simple test query for the given database type.
     */
    private String getTestQuery(DatabaseConfig.DatabaseType type) {
        if (type == null) {
            return "SELECT 1";
        }

        switch (type) {
            case SQLITE:
            case H2:
                return "SELECT 1";

            case POSTGRESQL:
                return "SELECT 1 AS test";

            case MYSQL:
                return "SELECT 1";

            default:
                return "SELECT 1";
        }
    }

    /**
     * Extracts port number from config for error messages.
     */
    private int extractPort(DatabaseConfig config) {
        try {
            String url = config.getDatabaseUrl();
            if (url != null && url.contains(":")) {
                int start = url.indexOf("//");
                if (start >= 0) {
                    start = url.indexOf(":", start + 2);
                    if (start >= 0) {
                        int end = url.indexOf("/", start);
                        if (end > start) {
                            return Integer.parseInt(url.substring(start + 1, end));
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignore
        }

        // Return default ports based on type
        DatabaseConfig.DatabaseType type = config.getDatabaseType();
        if (type == DatabaseConfig.DatabaseType.POSTGRESQL) {
            return 5432;
        } else if (type == DatabaseConfig.DatabaseType.MYSQL) {
            return 3306;
        }

        return 0;
    }

    /**
     * Analyzes SQL error and provides specific recommendations.
     */
    private void analyzeError(int errorCode, String sqlState, String message,
                             List<String> recommendations, DatabaseConfig config) {
        String lowerMessage = message != null ? message.toLowerCase() : "";

        // Authentication errors
        if (lowerMessage.contains("authentication") ||
            lowerMessage.contains("access denied") ||
            lowerMessage.contains("password") ||
            sqlState != null && sqlState.startsWith("28")) {

            recommendations.add("Authentication failed. Verify username and password.");
            recommendations.add("Check user exists in database");
            recommendations.add("Use environment variable DB_PASSWORD instead of config file.");
        }

        // Connection refused / host unreachable
        else if (lowerMessage.contains("connection refused") ||
                 lowerMessage.contains("cannot connect") ||
                 lowerMessage.contains("unable to connect")) {

            recommendations.add("Database server is not reachable. Is it running?");
            recommendations.add("Check host and port: " +
                SensitiveDataMasker.maskConnectionString(config.getDatabaseUrl()));
            recommendations.add("Verify firewall allows connections on port " + extractPort(config));
        }

        // Database doesn't exist
        else if (lowerMessage.contains("database") && lowerMessage.contains("does not exist") ||
                 lowerMessage.contains("unknown database")) {

            recommendations.add("Database does not exist. Create it first:");

            DatabaseConfig.DatabaseType type = config.getDatabaseType();
            if (type == DatabaseConfig.DatabaseType.POSTGRESQL) {
                recommendations.add("  createdb <database_name>");
            } else if (type == DatabaseConfig.DatabaseType.MYSQL) {
                recommendations.add("  CREATE DATABASE <database_name>;");
            }
        }

        // Permission errors
        else if (lowerMessage.contains("permission denied") ||
                 lowerMessage.contains("insufficient privilege")) {

            recommendations.add("User lacks necessary permissions.");
            recommendations.add("Grant required permissions to user.");
        }

        // Timeout errors
        else if (lowerMessage.contains("timeout")) {
            recommendations.add("Operation timed out. Database may be overloaded.");
            recommendations.add("Increase timeout or optimize database performance.");
        }

        // Generic fallback
        else {
            recommendations.add("Check database server logs for detailed error information.");
            recommendations.add("Verify JDBC URL format is correct for " + config.getDatabaseType());
        }
    }

    /**
     * Result of a database connection test.
     */
    public static final class ConnectionTestResult {
        private final boolean successful;
        private final String errorMessage;
        private final DatabaseConfig config;
        private final String databaseVersion;
        private final String driverVersion;
        private final Duration responseTime;
        private final boolean canRead;
        private final boolean readOnly;
        private final Instant startTime;
        private final List<String> recommendations;

        private ConnectionTestResult(Builder builder) {
            this.successful = builder.successful;
            this.errorMessage = builder.errorMessage;
            this.config = builder.config;
            this.databaseVersion = builder.databaseVersion;
            this.driverVersion = builder.driverVersion;
            this.responseTime = builder.responseTime;
            this.canRead = builder.canRead;
            this.readOnly = builder.readOnly;
            this.startTime = builder.startTime;
            this.recommendations = Collections.unmodifiableList(new ArrayList<>(builder.recommendations));
        }

        public boolean isSuccessful() {
            return successful;
        }

        public boolean isFailed() {
            return !successful;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public DatabaseConfig getConfig() {
            return config;
        }

        public String getDatabaseVersion() {
            return databaseVersion;
        }

        public String getDriverVersion() {
            return driverVersion;
        }

        public Duration getResponseTime() {
            return responseTime;
        }

        public boolean canRead() {
            return canRead;
        }

        public boolean isReadOnly() {
            return readOnly;
        }

        public Instant getStartTime() {
            return startTime;
        }

        public List<String> getRecommendations() {
            return recommendations;
        }

        public boolean hasRecommendations() {
            return !recommendations.isEmpty();
        }

        /**
         * Returns a human-readable summary of the test result.
         */
        public String getSummary() {
            if (successful) {
                return String.format("✅ Connection successful to %s (response time: %dms)",
                    config.getDatabaseType(),
                    responseTime.toMillis());
            } else {
                return String.format("❌ Connection failed to %s: %s",
                    config.getDatabaseType(),
                    errorMessage);
            }
        }

        @Override
        public String toString() {
            return "ConnectionTestResult{" +
                   "successful=" + successful +
                   ", responseTime=" + (responseTime != null ? responseTime.toMillis() + "ms" : "N/A") +
                   ", databaseType=" + (config != null ? config.getDatabaseType() : "unknown") +
                   '}';
        }

        /**
         * Builder for ConnectionTestResult.
         */
        static Builder builder() {
            return new Builder();
        }

        static final class Builder {
            private boolean successful;
            private String errorMessage;
            private DatabaseConfig config;
            private String databaseVersion;
            private String driverVersion;
            private Duration responseTime;
            private boolean canRead;
            private boolean readOnly;
            private Instant startTime;
            private List<String> recommendations = new ArrayList<>();

            private Builder() {
            }

            Builder successful(boolean successful) {
                this.successful = successful;
                return this;
            }

            Builder errorMessage(String errorMessage) {
                this.errorMessage = errorMessage;
                return this;
            }

            Builder config(DatabaseConfig config) {
                this.config = config;
                return this;
            }

            Builder databaseVersion(String databaseVersion) {
                this.databaseVersion = databaseVersion;
                return this;
            }

            Builder driverVersion(String driverVersion) {
                this.driverVersion = driverVersion;
                return this;
            }

            Builder responseTime(Duration responseTime) {
                this.responseTime = responseTime;
                return this;
            }

            Builder canRead(boolean canRead) {
                this.canRead = canRead;
                return this;
            }

            Builder readOnly(boolean readOnly) {
                this.readOnly = readOnly;
                return this;
            }

            Builder startTime(Instant startTime) {
                this.startTime = startTime;
                return this;
            }

            Builder addRecommendation(String recommendation) {
                if (recommendation != null && !recommendation.isEmpty()) {
                    this.recommendations.add(recommendation);
                }
                return this;
            }

            Builder addRecommendations(List<String> recommendations) {
                if (recommendations != null) {
                    this.recommendations.addAll(recommendations);
                }
                return this;
            }

            ConnectionTestResult build() {
                return new ConnectionTestResult(this);
            }
        }
    }
}
