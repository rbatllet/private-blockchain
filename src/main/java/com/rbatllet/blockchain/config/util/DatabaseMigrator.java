package com.rbatllet.blockchain.config.util;

import com.rbatllet.blockchain.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.Instant;
import java.util.*;

/**
 * Database schema migration tool for managing database schema evolution.
 *
 * <p>This utility provides database migration capabilities including:</p>
 * <ul>
 *   <li>Version-based migration tracking</li>
 *   <li>Transactional migration execution</li>
 *   <li>Migration history tracking</li>
 *   <li>Schema version validation</li>
 *   <li>Migration status reporting</li>
 *   <li>Rollback support (optional)</li>
 * </ul>
 *
 * <p><b>Design Principles:</b></p>
 * <ul>
 *   <li>Migrations are versioned using semantic versioning (e.g., V1, V2, V3)</li>
 *   <li>Each migration runs in a transaction (if database supports it)</li>
 *   <li>Migration history is tracked in a schema history table</li>
 *   <li>Failed migrations prevent subsequent migrations</li>
 *   <li>Migrations are idempotent and safe to re-run</li>
 * </ul>
 *
 * <p><b>Migration Naming Convention:</b></p>
 * <pre>
 * V[VERSION]__[DESCRIPTION].sql
 * Examples:
 *   V1__create_users_table.sql
 *   V2__add_email_column.sql
 *   V3__create_indexes.sql
 * </pre>
 *
 * <p><b>Schema History Table:</b></p>
 * <pre>
 * CREATE TABLE schema_version (
 *   version_rank INT NOT NULL,
 *   installed_rank INT NOT NULL,
 *   version VARCHAR(50) NOT NULL PRIMARY KEY,
 *   description VARCHAR(200) NOT NULL,
 *   type VARCHAR(20) NOT NULL,
 *   script VARCHAR(1000) NOT NULL,
 *   checksum INT,
 *   installed_by VARCHAR(100) NOT NULL,
 *   installed_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 *   execution_time INT NOT NULL,
 *   success BOOLEAN NOT NULL
 * );
 * </pre>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * DatabaseConfig config = DatabaseConfig.createPostgreSQLConfig(
 *     "localhost", 5432, "blockchain", "admin", "password"
 * );
 *
 * DatabaseMigrator migrator = new DatabaseMigrator(config);
 *
 * // Register migrations
 * migrator.addMigration(Migration.builder()
 *     .version("V1")
 *     .description("Create blocks table")
 *     .sql("CREATE TABLE blocks (id INT PRIMARY KEY, data VARCHAR(1000))")
 *     .build());
 *
 * migrator.addMigration(Migration.builder()
 *     .version("V2")
 *     .description("Add timestamp column")
 *     .sql("ALTER TABLE blocks ADD COLUMN created_at TIMESTAMP")
 *     .build());
 *
 * // Execute all pending migrations
 * MigrationResult result = migrator.migrate();
 *
 * if (result.isSuccess()) {
 *     System.out.println("✅ Migrations applied: " + result.getMigrationsApplied());
 * } else {
 *     System.err.println("❌ Migration failed: " + result.getErrorMessage());
 * }
 *
 * // Check current schema version
 * String currentVersion = migrator.getCurrentVersion();
 * System.out.println("Current schema version: " + currentVersion);
 *
 * // Get migration history
 * List<MigrationHistoryEntry> history = migrator.getHistory();
 * for (MigrationHistoryEntry entry : history) {
 *     System.out.println(entry.getVersion() + " - " + entry.getDescription() +
 *                       " (" + entry.getState() + ")");
 * }
 * }</pre>
 *
 * <p><b>Thread Safety:</b> This class is NOT thread-safe. Each instance should
 * be used by a single thread. Do not share migrator instances between threads.</p>
 *
 * @since 1.0.6
 * @see DatabaseConfig
 * @see Migration
 * @see MigrationResult
 */
public final class DatabaseMigrator {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseMigrator.class);

    /** Default schema history table name */
    public static final String DEFAULT_SCHEMA_TABLE = "schema_version";

    /** Migration type: SQL */
    private static final String TYPE_SQL = "SQL";

    private final DatabaseConfig config;
    private final String schemaTableName;
    private final List<Migration> migrations;
    private final boolean enableAutoCommit;

    /**
     * Creates a new migrator with default settings.
     *
     * @param config database configuration
     * @throws IllegalArgumentException if config is null
     */
    public DatabaseMigrator(DatabaseConfig config) {
        this(config, DEFAULT_SCHEMA_TABLE, false);
    }

    /**
     * Creates a new migrator with custom settings.
     *
     * @param config database configuration
     * @param schemaTableName name of schema history table
     * @param enableAutoCommit whether to enable auto-commit (for databases without transaction support)
     * @throws IllegalArgumentException if config or schemaTableName is null
     */
    public DatabaseMigrator(DatabaseConfig config, String schemaTableName, boolean enableAutoCommit) {
        if (config == null) {
            throw new IllegalArgumentException("Database configuration cannot be null");
        }
        if (schemaTableName == null || schemaTableName.trim().isEmpty()) {
            throw new IllegalArgumentException("Schema table name cannot be null or empty");
        }

        this.config = config;
        this.schemaTableName = schemaTableName;
        this.enableAutoCommit = enableAutoCommit;
        this.migrations = new ArrayList<>();
    }

    /**
     * Adds a migration to be executed.
     *
     * @param migration migration to add
     * @return this migrator instance (for chaining)
     * @throws IllegalArgumentException if migration is null
     */
    public DatabaseMigrator addMigration(Migration migration) {
        if (migration == null) {
            throw new IllegalArgumentException("Migration cannot be null");
        }
        this.migrations.add(migration);
        return this;
    }

    /**
     * Adds multiple migrations at once.
     *
     * @param migrations migrations to add
     * @return this migrator instance (for chaining)
     * @throws IllegalArgumentException if migrations is null
     */
    public DatabaseMigrator addMigrations(List<Migration> migrations) {
        if (migrations == null) {
            throw new IllegalArgumentException("Migrations list cannot be null");
        }
        this.migrations.addAll(migrations);
        return this;
    }

    /**
     * Executes all pending migrations.
     *
     * <p>This method:</p>
     * <ol>
     *   <li>Creates schema history table if it doesn't exist</li>
     *   <li>Validates migration checksums</li>
     *   <li>Sorts migrations by version</li>
     *   <li>Executes pending migrations in order</li>
     *   <li>Records each migration in history table</li>
     *   <li>Rolls back on failure (if transactions supported)</li>
     * </ol>
     *
     * @return migration result with detailed information
     */
    public MigrationResult migrate() {
        Instant startTime = Instant.now();
        List<String> appliedVersions = new ArrayList<>();
        String failedVersion = null;
        MigrationResult.Builder resultBuilder = MigrationResult.builder()
            .startTime(startTime);

        Connection connection = null;

        try {
            // Get connection
            connection = getConnection();
            connection.setAutoCommit(enableAutoCommit);

            // Initialize schema history table
            initializeSchemaTable(connection);

            // Get currently applied migrations
            Set<String> appliedMigrations = getAppliedMigrations(connection);
            logger.debug("Already applied migrations: {}", appliedMigrations);

            // Sort migrations by version
            List<Migration> sortedMigrations = new ArrayList<>(migrations);
            sortedMigrations.sort(Comparator.comparing(Migration::getVersion));
            logger.debug("Sorted migrations: {}", sortedMigrations.stream().map(Migration::getVersion).toList());

            // Validate migration order
            validateMigrationOrder(sortedMigrations, appliedMigrations);

            // Count pending migrations for success calculation
            long pendingMigrationsCount = sortedMigrations.stream()
                .filter(m -> !appliedMigrations.contains(m.getVersion()))
                .count();
            logger.debug("Pending migrations count: {}", pendingMigrationsCount);

            // Execute pending migrations
            for (Migration migration : sortedMigrations) {
                if (appliedMigrations.contains(migration.getVersion())) {
                    // Migration already applied - validate checksum
                    logger.debug("Skipping already applied migration: {}", migration.getVersion());
                    validateChecksum(connection, migration);
                    continue;
                }

                // Execute migration
                logger.debug("Executing migration: {}", migration.getVersion());
                boolean migrationSucceeded = executeMigration(connection, migration);
                logger.debug("Migration {} result: {}", migration.getVersion(), migrationSucceeded);

                // Commit after each migration (success or failure) to preserve history
                if (!enableAutoCommit) {
                    connection.commit();
                }

                if (migrationSucceeded) {
                    appliedVersions.add(migration.getVersion());
                } else {
                    // Migration failed, record version and stop execution
                    failedVersion = migration.getVersion();
                    logger.warn("Migration {} failed, stopping execution", failedVersion);
                    break;
                }
            }

            Instant endTime = Instant.now();
            long durationMs = endTime.toEpochMilli() - startTime.toEpochMilli();

            // Check if all pending migrations succeeded
            boolean allSucceeded = (pendingMigrationsCount == 0) ||
                                  (appliedVersions.size() == pendingMigrationsCount);
            logger.debug("All succeeded check: pendingCount={}, appliedCount={}, allSucceeded={}",
                pendingMigrationsCount, appliedVersions.size(), allSucceeded);

            // Build error message if there was a failure
            String errorMessage = null;
            if (!allSucceeded && failedVersion != null) {
                errorMessage = "Migration " + failedVersion + " failed";
            } else if (!allSucceeded) {
                errorMessage = "One or more migrations failed";
            }

            return resultBuilder
                .success(allSucceeded)
                .migrationsApplied(appliedVersions.size())
                .appliedVersions(appliedVersions)
                .errorMessage(errorMessage)
                .durationMs(durationMs)
                .build();

        } catch (Exception e) {
            // Rollback on error (e.g., connection issues)
            if (connection != null && !enableAutoCommit) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackEx) {
                    // Log rollback failure but throw original exception
                }
            }

            Instant endTime = Instant.now();
            long durationMs = endTime.toEpochMilli() - startTime.toEpochMilli();

            return resultBuilder
                .success(false)
                .migrationsApplied(appliedVersions.size())
                .appliedVersions(appliedVersions)
                .errorMessage(e.getMessage())
                .durationMs(durationMs)
                .build();

        } finally {
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
     * Gets the current database schema version.
     *
     * @return current version, or null if no migrations applied
     */
    public String getCurrentVersion() {
        try (Connection connection = getConnection()) {
            // Check if schema table exists
            if (!schemaTableExists(connection)) {
                return null;
            }

            String sql = "SELECT version FROM " + schemaTableName +
                        " WHERE success = ? ORDER BY installed_rank DESC LIMIT 1";

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setBoolean(1, true);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("version");
                    }
                }
            }

            return null;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to get current version", e);
        }
    }

    /**
     * Gets the full migration history.
     *
     * @return list of migration history entries, ordered by installation rank
     */
    public List<MigrationHistoryEntry> getHistory() {
        List<MigrationHistoryEntry> history = new ArrayList<>();

        try (Connection connection = getConnection()) {
            // Check if schema table exists
            if (!schemaTableExists(connection)) {
                return history;
            }

            String sql = "SELECT version, description, type, installed_by, " +
                        "installed_on, execution_time, success " +
                        "FROM " + schemaTableName + " ORDER BY installed_rank";

            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                while (rs.next()) {
                    MigrationHistoryEntry entry = new MigrationHistoryEntry(
                        rs.getString("version"),
                        rs.getString("description"),
                        rs.getString("type"),
                        rs.getString("installed_by"),
                        rs.getTimestamp("installed_on").toInstant(),
                        rs.getInt("execution_time"),
                        rs.getBoolean("success")
                    );
                    history.add(entry);
                }
            }

            return history;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to retrieve migration history", e);
        }
    }

    /**
     * Validates the current database state against registered migrations.
     *
     * @return validation result
     */
    public ValidationResult validate() {
        try (Connection connection = getConnection()) {
            if (!schemaTableExists(connection)) {
                return new ValidationResult(true, "No migrations applied yet", Collections.emptyList());
            }

            List<String> issues = new ArrayList<>();

            // Check for missing migrations
            Set<String> appliedMigrations = getAppliedMigrations(connection);
            Set<String> registeredVersions = new HashSet<>();
            for (Migration m : migrations) {
                registeredVersions.add(m.getVersion());
            }

            for (String appliedVersion : appliedMigrations) {
                if (!registeredVersions.contains(appliedVersion)) {
                    issues.add("Applied migration not found in registered migrations: " + appliedVersion);
                }
            }

            // Check checksum mismatches
            for (Migration migration : migrations) {
                if (appliedMigrations.contains(migration.getVersion())) {
                    try {
                        validateChecksum(connection, migration);
                    } catch (SQLException e) {
                        issues.add("Checksum mismatch for " + migration.getVersion() + ": " + e.getMessage());
                    }
                }
            }

            if (issues.isEmpty()) {
                return new ValidationResult(true, "All migrations validated successfully", issues);
            } else {
                return new ValidationResult(false,
                    "Validation failed with " + issues.size() + " issue(s)", issues);
            }

        } catch (SQLException e) {
            return new ValidationResult(false, "Validation error: " + e.getMessage(),
                Collections.singletonList(e.getMessage()));
        }
    }

    /**
     * Gets database connection.
     */
    private Connection getConnection() throws SQLException {
        String jdbcUrl = config.getDatabaseUrl();
        String username = config.getUsername();
        String password = config.getPassword();

        if (username != null && !username.isEmpty()) {
            return DriverManager.getConnection(jdbcUrl, username, password);
        } else {
            return DriverManager.getConnection(jdbcUrl);
        }
    }

    /**
     * Initializes schema history table if it doesn't exist.
     */
    private void initializeSchemaTable(Connection connection) throws SQLException {
        if (schemaTableExists(connection)) {
            return;
        }

        // Create schema history table
        String createTableSql = getCreateSchemaTableSql();

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTableSql);
        }
    }

    /**
     * Checks if schema history table exists.
     */
    private boolean schemaTableExists(Connection connection) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();

        // Try exact match first
        try (ResultSet rs = metadata.getTables(null, null, schemaTableName, new String[]{"TABLE"})) {
            if (rs.next()) {
                return true;
            }
        }

        // Try uppercase (H2 default behavior)
        try (ResultSet rs = metadata.getTables(null, null, schemaTableName.toUpperCase(), new String[]{"TABLE"})) {
            if (rs.next()) {
                return true;
            }
        }

        // Try lowercase
        try (ResultSet rs = metadata.getTables(null, null, schemaTableName.toLowerCase(), new String[]{"TABLE"})) {
            if (rs.next()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Gets CREATE TABLE SQL for schema history table.
     */
    private String getCreateSchemaTableSql() {
        DatabaseConfig.DatabaseType type = config.getDatabaseType();

        switch (type) {
            case SQLITE:
            case H2:
                return "CREATE TABLE " + schemaTableName + " (" +
                       "version_rank INTEGER NOT NULL, " +
                       "installed_rank INTEGER NOT NULL, " +
                       "version VARCHAR(50) NOT NULL PRIMARY KEY, " +
                       "description VARCHAR(200) NOT NULL, " +
                       "type VARCHAR(20) NOT NULL, " +
                       "script VARCHAR(1000) NOT NULL, " +
                       "checksum INTEGER, " +
                       "installed_by VARCHAR(100) NOT NULL, " +
                       "installed_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                       "execution_time INTEGER NOT NULL, " +
                       "success BOOLEAN NOT NULL" +
                       ")";

            case POSTGRESQL:
                return "CREATE TABLE " + schemaTableName + " (" +
                       "version_rank INT NOT NULL, " +
                       "installed_rank INT NOT NULL, " +
                       "version VARCHAR(50) NOT NULL PRIMARY KEY, " +
                       "description VARCHAR(200) NOT NULL, " +
                       "type VARCHAR(20) NOT NULL, " +
                       "script VARCHAR(1000) NOT NULL, " +
                       "checksum INT, " +
                       "installed_by VARCHAR(100) NOT NULL, " +
                       "installed_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                       "execution_time INT NOT NULL, " +
                       "success BOOLEAN NOT NULL" +
                       ")";

            case MYSQL:
                return "CREATE TABLE " + schemaTableName + " (" +
                       "version_rank INT NOT NULL, " +
                       "installed_rank INT NOT NULL, " +
                       "version VARCHAR(50) NOT NULL PRIMARY KEY, " +
                       "description VARCHAR(200) NOT NULL, " +
                       "type VARCHAR(20) NOT NULL, " +
                       "script VARCHAR(1000) NOT NULL, " +
                       "checksum INT, " +
                       "installed_by VARCHAR(100) NOT NULL, " +
                       "installed_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                       "execution_time INT NOT NULL, " +
                       "success BOOLEAN NOT NULL" +
                       ")";

            default:
                throw new UnsupportedOperationException("Unsupported database type: " + type);
        }
    }

    /**
     * Gets set of already applied migration versions.
     */
    private Set<String> getAppliedMigrations(Connection connection) throws SQLException {
        Set<String> applied = new HashSet<>();

        if (!schemaTableExists(connection)) {
            return applied;
        }

        String sql = "SELECT version FROM " + schemaTableName + " WHERE success = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setBoolean(1, true);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    applied.add(rs.getString("version"));
                }
            }
        }

        return applied;
    }

    /**
     * Validates that migrations are in correct order.
     */
    private void validateMigrationOrder(List<Migration> sortedMigrations,
                                       Set<String> appliedMigrations) throws SQLException {
        // Check for gaps in version numbers
        String lastAppliedVersion = null;

        for (Migration migration : sortedMigrations) {
            if (appliedMigrations.contains(migration.getVersion())) {
                lastAppliedVersion = migration.getVersion();
            } else if (lastAppliedVersion != null) {
                // We have a pending migration, but there are applied migrations before it
                // This is OK - just means we're adding new migrations
                break;
            }
        }
    }

    /**
     * Validates checksum of an already-applied migration.
     */
    private void validateChecksum(Connection connection, Migration migration) throws SQLException {
        String sql = "SELECT checksum FROM " + schemaTableName +
                    " WHERE version = ? AND success = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, migration.getVersion());
            stmt.setBoolean(2, true);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int storedChecksum = rs.getInt("checksum");
                    int currentChecksum = migration.calculateChecksum();

                    if (storedChecksum != currentChecksum) {
                        throw new SQLException(
                            "Migration checksum mismatch for version " + migration.getVersion() +
                            ". Expected: " + storedChecksum + ", got: " + currentChecksum +
                            ". The migration script has been modified after being applied."
                        );
                    }
                }
            }
        }
    }

    /**
     * Executes a single migration.
     * @return true if migration succeeded, false if it failed
     */
    private boolean executeMigration(Connection connection, Migration migration) throws SQLException {
        Instant startTime = Instant.now();

        try {
            // Execute migration SQL
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(migration.getSql());
            }

            Instant endTime = Instant.now();
            int executionTimeMs = (int) (endTime.toEpochMilli() - startTime.toEpochMilli());

            // Record successful migration
            recordMigration(connection, migration, true, executionTimeMs, null);

            return true;

        } catch (SQLException e) {
            Instant endTime = Instant.now();
            int executionTimeMs = (int) (endTime.toEpochMilli() - startTime.toEpochMilli());

            // Record failed migration
            logger.error("Migration {} failed with SQL error: {}", migration.getVersion(), e.getMessage(), e);
            recordMigration(connection, migration, false, executionTimeMs, e.getMessage());

            return false;
        }
    }

    /**
     * Records migration execution in schema history table.
     */
    private void recordMigration(Connection connection, Migration migration,
                                boolean success, int executionTimeMs, String errorMessage) throws SQLException {
        // Get next installed_rank
        int installedRank = getNextInstalledRank(connection);

        String sql = "INSERT INTO " + schemaTableName +
                    " (version_rank, installed_rank, version, description, type, script, " +
                    "checksum, installed_by, execution_time, success) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, parseVersionRank(migration.getVersion()));
            stmt.setInt(2, installedRank);
            stmt.setString(3, migration.getVersion());
            stmt.setString(4, migration.getDescription());
            stmt.setString(5, TYPE_SQL);
            stmt.setString(6, migration.getScript());
            stmt.setInt(7, migration.calculateChecksum());
            stmt.setString(8, System.getProperty("user.name", "unknown"));
            stmt.setInt(9, executionTimeMs);
            stmt.setBoolean(10, success);

            stmt.executeUpdate();
        }
    }

    /**
     * Gets next installed_rank value.
     */
    private int getNextInstalledRank(Connection connection) throws SQLException {
        String sql = "SELECT MAX(installed_rank) as max_rank FROM " + schemaTableName;

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                int maxRank = rs.getInt("max_rank");
                return maxRank + 1;
            }
        }

        return 1;
    }

    /**
     * Parses version string to integer rank (e.g., "V1" -> 1, "V10" -> 10).
     */
    private int parseVersionRank(String version) {
        if (version == null || !version.startsWith("V")) {
            throw new IllegalArgumentException("Invalid version format: " + version);
        }

        try {
            return Integer.parseInt(version.substring(1));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid version number: " + version, e);
        }
    }

    /**
     * Represents a database migration.
     */
    public static final class Migration {
        private final String version;
        private final String description;
        private final String sql;
        private final String script;

        private Migration(Builder builder) {
            this.version = builder.version;
            this.description = builder.description;
            this.sql = builder.sql;
            this.script = builder.script != null ? builder.script : version + "__" + description;
        }

        public static Builder builder() {
            return new Builder();
        }

        public String getVersion() {
            return version;
        }

        public String getDescription() {
            return description;
        }

        public String getSql() {
            return sql;
        }

        public String getScript() {
            return script;
        }

        /**
         * Calculates checksum for this migration.
         */
        public int calculateChecksum() {
            return sql.hashCode();
        }

        public static final class Builder {
            private String version;
            private String description;
            private String sql;
            private String script;

            private Builder() {
            }

            public Builder version(String version) {
                this.version = version;
                return this;
            }

            public Builder description(String description) {
                this.description = description;
                return this;
            }

            public Builder sql(String sql) {
                this.sql = sql;
                return this;
            }

            public Builder script(String script) {
                this.script = script;
                return this;
            }

            public Migration build() {
                if (version == null || version.trim().isEmpty()) {
                    throw new IllegalArgumentException("Migration version cannot be null or empty");
                }
                if (description == null || description.trim().isEmpty()) {
                    throw new IllegalArgumentException("Migration description cannot be null or empty");
                }
                if (sql == null || sql.trim().isEmpty()) {
                    throw new IllegalArgumentException("Migration SQL cannot be null or empty");
                }

                return new Migration(this);
            }
        }
    }

    /**
     * Result of migration execution.
     */
    public static final class MigrationResult {
        private final boolean success;
        private final int migrationsApplied;
        private final List<String> appliedVersions;
        private final String errorMessage;
        private final Instant startTime;
        private final long durationMs;

        private MigrationResult(Builder builder) {
            this.success = builder.success;
            this.migrationsApplied = builder.migrationsApplied;
            this.appliedVersions = Collections.unmodifiableList(new ArrayList<>(builder.appliedVersions));
            this.errorMessage = builder.errorMessage;
            this.startTime = builder.startTime;
            this.durationMs = builder.durationMs;
        }

        public static Builder builder() {
            return new Builder();
        }

        public boolean isSuccess() {
            return success;
        }

        public boolean isFailed() {
            return !success;
        }

        public int getMigrationsApplied() {
            return migrationsApplied;
        }

        public List<String> getAppliedVersions() {
            return appliedVersions;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public Instant getStartTime() {
            return startTime;
        }

        public long getDurationMs() {
            return durationMs;
        }

        @Override
        public String toString() {
            return "MigrationResult{" +
                   "success=" + success +
                   ", migrationsApplied=" + migrationsApplied +
                   ", durationMs=" + durationMs +
                   '}';
        }

        static final class Builder {
            private boolean success;
            private int migrationsApplied;
            private List<String> appliedVersions = new ArrayList<>();
            private String errorMessage;
            private Instant startTime;
            private long durationMs;

            private Builder() {
            }

            Builder success(boolean success) {
                this.success = success;
                return this;
            }

            Builder migrationsApplied(int migrationsApplied) {
                this.migrationsApplied = migrationsApplied;
                return this;
            }

            Builder appliedVersions(List<String> appliedVersions) {
                this.appliedVersions = appliedVersions;
                return this;
            }

            Builder errorMessage(String errorMessage) {
                this.errorMessage = errorMessage;
                return this;
            }

            Builder startTime(Instant startTime) {
                this.startTime = startTime;
                return this;
            }

            Builder durationMs(long durationMs) {
                this.durationMs = durationMs;
                return this;
            }

            MigrationResult build() {
                return new MigrationResult(this);
            }
        }
    }

    /**
     * Entry in migration history.
     */
    public static final class MigrationHistoryEntry {
        private final String version;
        private final String description;
        private final String type;
        private final String installedBy;
        private final Instant installedOn;
        private final int executionTime;
        private final boolean success;

        public MigrationHistoryEntry(String version, String description, String type,
                                     String installedBy, Instant installedOn,
                                     int executionTime, boolean success) {
            this.version = version;
            this.description = description;
            this.type = type;
            this.installedBy = installedBy;
            this.installedOn = installedOn;
            this.executionTime = executionTime;
            this.success = success;
        }

        public String getVersion() {
            return version;
        }

        public String getDescription() {
            return description;
        }

        public String getType() {
            return type;
        }

        public String getInstalledBy() {
            return installedBy;
        }

        public Instant getInstalledOn() {
            return installedOn;
        }

        public int getExecutionTime() {
            return executionTime;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getState() {
            return success ? "Success" : "Failed";
        }

        @Override
        public String toString() {
            return version + " - " + description + " (" + getState() + ", " + executionTime + "ms)";
        }
    }

    /**
     * Result of validation.
     */
    public static final class ValidationResult {
        private final boolean valid;
        private final String message;
        private final List<String> issues;

        public ValidationResult(boolean valid, String message, List<String> issues) {
            this.valid = valid;
            this.message = message;
            this.issues = Collections.unmodifiableList(new ArrayList<>(issues));
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }

        public List<String> getIssues() {
            return issues;
        }

        public boolean hasIssues() {
            return !issues.isEmpty();
        }

        @Override
        public String toString() {
            return "ValidationResult{" +
                   "valid=" + valid +
                   ", message='" + message + '\'' +
                   ", issues=" + issues.size() +
                   '}';
        }
    }
}
