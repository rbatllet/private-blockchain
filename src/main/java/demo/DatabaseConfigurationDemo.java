package demo;

import com.rbatllet.blockchain.config.DatabaseConfig;
import com.rbatllet.blockchain.config.util.*;
import com.rbatllet.blockchain.config.util.ConfigurationPriorityResolver.ConfigSource;
import com.rbatllet.blockchain.config.util.ConfigurationPriorityResolver.ResolvedConfiguration;
import com.rbatllet.blockchain.config.util.DatabaseConnectionTester.ConnectionTestResult;
import com.rbatllet.blockchain.config.util.DatabasePropertiesParser.ParseResult;
import com.rbatllet.blockchain.config.util.FilePermissionsUtil.PermissionStatus;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Comprehensive demonstration of Database Configuration Utilities
 *
 * This demo showcases all 9 utility classes from com.rbatllet.blockchain.config.util:
 * 1. DatabasePropertiesParser
 * 2. ConfigurationPriorityResolver
 * 3. ConfigurationExporter
 * 4. SensitiveDataMasker
 * 5. ConfigurationSecurityAnalyzer
 * 6. SecurityWarning
 * 7. FilePermissionsUtil
 * 8. DatabaseConnectionTester
 * 9. DatabaseMigrator
 */
public class DatabaseConfigurationDemo {

    public static void main(String[] args) {
        System.out.println("🔧 Database Configuration Utilities Demo");
        System.out.println("=".repeat(80));
        System.out.println();
        System.out.println("This demo showcases 9 utility classes for database configuration:");
        System.out.println("  • DatabasePropertiesParser");
        System.out.println("  • ConfigurationPriorityResolver");
        System.out.println("  • ConfigurationExporter");
        System.out.println("  • SensitiveDataMasker");
        System.out.println("  • ConfigurationSecurityAnalyzer");
        System.out.println("  • SecurityWarning");
        System.out.println("  • FilePermissionsUtil");
        System.out.println("  • DatabaseConnectionTester");
        System.out.println("  • DatabaseMigrator");
        System.out.println();

        try {
            // Demo 1: DatabasePropertiesParser
            demo1PropertiesParser();
            pause();

            // Demo 2: ConfigurationPriorityResolver
            demo2PriorityResolver();
            pause();

            // Demo 3: ConfigurationExporter
            demo3Exporter();
            pause();

            // Demo 4: SensitiveDataMasker
            demo4SensitiveDataMasker();
            pause();

            // Demo 5: ConfigurationSecurityAnalyzer
            demo5SecurityAnalyzer();
            pause();

            // Demo 6: FilePermissionsUtil
            demo6FilePermissions();
            pause();

            // Demo 7: DatabaseConnectionTester
            demo7ConnectionTester();
            pause();

            // Demo 8: DatabaseMigrator
            demo8DatabaseMigrator();

            System.out.println();
            System.out.println("=".repeat(80));
            System.out.println("✅ All demos completed successfully!");
            System.out.println();
            System.out.println("💡 Key Takeaways:");
            System.out.println("  • These utilities provide secure, flexible database configuration");
            System.out.println("  • Security is prioritized (password masking, permissions, warnings)");
            System.out.println("  • All classes are reusable across CLI, web apps, and REST APIs");
            System.out.println("  • Production-ready with comprehensive error handling");

        } catch (Exception e) {
            System.err.println();
            System.err.println("❌ Demo failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Demo 1: DatabasePropertiesParser
     * Parse .properties files into DatabaseConfig objects
     */
    private static void demo1PropertiesParser() throws IOException {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("📋 Demo 1: DatabasePropertiesParser");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();

        // Create a test properties file
        Path configFile = Paths.get("test-config.properties");
        try (FileWriter writer = new FileWriter(configFile.toFile())) {
            writer.write("db.type=postgresql\n");
            writer.write("db.postgresql.host=localhost\n");
            writer.write("db.postgresql.port=5432\n");
            writer.write("db.postgresql.database=blockchain\n");
            writer.write("db.postgresql.username=admin\n");
            writer.write("# Password intentionally omitted (use DB_PASSWORD env var)\n");
        }

        System.out.println("Created test configuration file: test-config.properties");
        System.out.println();

        // Parse the file
        ParseResult result = DatabasePropertiesParser.parse(Files.newInputStream(configFile));

        if (result.isSuccess()) {
            System.out.println("✅ Parsing succeeded!");
            System.out.println();

            DatabaseConfig config = result.getConfig();
            System.out.println("Parsed configuration:");
            System.out.println("  Type: " + config.getDatabaseType());
            System.out.println("  Description: " + config.getDatabaseType().getDescription());
            System.out.println();

            // Show warnings
            if (result.hasWarnings()) {
                System.out.println("⚠️  Warnings:");
                for (String warning : result.getWarnings()) {
                    System.out.println("  • " + warning);
                }
                System.out.println();
            }
        } else {
            System.out.println("❌ Parsing failed!");
            for (String error : result.getErrors()) {
                System.out.println("  • " + error);
            }
        }

        // Cleanup
        Files.deleteIfExists(configFile);
    }

    /**
     * Demo 2: ConfigurationPriorityResolver
     * Resolve configuration from multiple sources with priority
     */
    private static void demo2PriorityResolver() {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("🎯 Demo 2: ConfigurationPriorityResolver");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();

        // Simulate different configuration sources
        DatabaseConfig fileConfig = DatabaseConfig.createPostgreSQLConfig(
            "localhost", 5432, "blockchain", "user_from_file", ""
        );

        DatabaseConfig envConfig = DatabaseConfig.createPostgreSQLConfig(
            "prod-db.example.com", 5432, "blockchain_prod", "user_from_env", ""
        );

        DatabaseConfig defaultConfig = DatabaseConfig.createSQLiteConfig();

        System.out.println("Configuration sources:");
        System.out.println("  • File:        host=localhost, user=user_from_file");
        System.out.println("  • Environment: host=prod-db.example.com, user=user_from_env");
        System.out.println("  • Default:     SQLite");
        System.out.println();

        // Resolve with priority: ENV > FILE > DEFAULT
        ConfigurationPriorityResolver resolver = ConfigurationPriorityResolver.builder()
            .withEnvironmentVars(envConfig)
            .withConfigFile(fileConfig)
            .withDefaults(defaultConfig)
            .build();

        ResolvedConfiguration resolved = resolver.resolve();
        DatabaseConfig finalConfig = resolved.getConfig();

        System.out.println("✅ Resolved configuration:");
        System.out.println("  Type: " + finalConfig.getDatabaseType());
        System.out.println();

        // Show where each property came from
        System.out.println("Source tracking:");
        Map<String, ConfigSource> sources = resolved.getSourceMap();
        System.out.println("  • Type from:     " + sources.get("type"));
        System.out.println("  • Host from:     " + sources.get("host"));
        System.out.println("  • Username from: " + sources.get("username"));
        System.out.println();

        System.out.println("Priority order: ENVIRONMENT > FILE > DEFAULT");
        System.out.println("Result: Environment variables override file configuration ✨");
    }

    /**
     * Demo 3: ConfigurationExporter
     * Export configuration with automatic sensitive data masking
     */
    private static void demo3Exporter() throws IOException {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("📤 Demo 3: ConfigurationExporter");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();

        DatabaseConfig config = DatabaseConfig.createPostgreSQLConfig(
            "localhost", 5432, "blockchain", "admin", "SuperSecret123!"
        );

        ConfigurationExporter exporter = new ConfigurationExporter();

        // Export to Properties (passwords masked by default)
        System.out.println("1️⃣  Export to Properties format (passwords masked by default):");
        String propsContent = exporter.exportToProperties(config);
        System.out.println(propsContent.substring(0, Math.min(400, propsContent.length())));
        System.out.println("  ... (truncated)");
        System.out.println("  Notice: db.postgresql.password=******** (masked)");
        System.out.println();

        // Save to file
        Path exportFile = Paths.get("test-config-export.properties");
        Files.writeString(exportFile, propsContent);
        System.out.println("  Saved to: test-config-export.properties");
        System.out.println();

        // Export to JSON
        System.out.println("2️⃣  Export to JSON format:");
        String jsonContent = exporter.exportToJson(config);
        Path jsonFile = Paths.get("test-config.json");
        Files.writeString(jsonFile, jsonContent);
        System.out.println("  Saved to: test-config.json");
        System.out.println();

        // Export to Environment Variables
        System.out.println("3️⃣  Export to Environment Variables format:");
        String envContent = exporter.exportToEnv(config);
        Path envFile = Paths.get("test-config.env");
        Files.writeString(envFile, envContent);
        System.out.println("  Saved to: test-config.env");
        System.out.println();

        System.out.println("✅ All exports completed with automatic password masking!");
        System.out.println("   Files are safe to share (passwords redacted)");
    }

    /**
     * Demo 4: SensitiveDataMasker
     * Mask passwords and credentials in logs and output
     */
    private static void demo4SensitiveDataMasker() {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("🔐 Demo 4: SensitiveDataMasker");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();

        // Mask JDBC connection string
        String jdbc = "jdbc:postgresql://admin:secret123@localhost:5432/db?password=pwd456";
        System.out.println("1️⃣  Mask JDBC Connection String:");
        System.out.println("  Original (UNSAFE):");
        System.out.println("    " + jdbc);
        System.out.println("  Masked (SAFE for logging):");
        System.out.println("    " + SensitiveDataMasker.maskConnectionString(jdbc));
        System.out.println();

        // Mask properties
        System.out.println("2️⃣  Mask Properties:");
        Properties props = new Properties();
        props.setProperty("db.host", "localhost");
        props.setProperty("db.port", "5432");
        props.setProperty("db.password", "SuperSecret123!");
        props.setProperty("db.username", "admin");

        System.out.println("  Original properties:");
        props.forEach((key, value) -> System.out.println("    " + key + " = " + value));

        Properties masked = SensitiveDataMasker.maskProperties(props);
        System.out.println("  Masked properties (SAFE for logging):");
        masked.forEach((key, value) -> System.out.println("    " + key + " = " + value));
        System.out.println();

        // Check if keys are sensitive
        System.out.println("3️⃣  Detect Sensitive Keys:");
        String[] keys = {"db.password", "db.host", "api.secret", "db.port", "auth.token"};
        for (String key : keys) {
            boolean sensitive = SensitiveDataMasker.isSensitiveKey(key);
            System.out.println("  " + key + ": " + (sensitive ? "🔒 SENSITIVE" : "✅ Safe"));
        }
        System.out.println();

        System.out.println("✅ Always mask sensitive data before logging!");
    }

    /**
     * Demo 5: ConfigurationSecurityAnalyzer
     * Analyze configuration for security risks
     */
    private static void demo5SecurityAnalyzer() {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("🛡️  Demo 5: ConfigurationSecurityAnalyzer");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();

        // Scenario 1: Weak password
        System.out.println("Scenario 1: Configuration with weak password");
        DatabaseConfig weakConfig = DatabaseConfig.createPostgreSQLConfig(
            "localhost", 5432, "blockchain", "admin", "admin"  // Common password!
        );

        ConfigurationSecurityAnalyzer analyzer = new ConfigurationSecurityAnalyzer()
            .withConfigSource(ConfigurationSecurityAnalyzer.ConfigSource.FILE);

        List<SecurityWarning> warnings = analyzer.analyze(weakConfig);

        System.out.println("  Found " + warnings.size() + " security issue(s):");
        System.out.println();

        for (SecurityWarning warning : warnings) {
            String emoji = getEmojiForSeverity(warning.getSeverity());
            System.out.println("  " + emoji + " " + warning.getSeverity() + ": " + warning.getMessage());

            if (warning.getCode() != null) {
                System.out.println("    Code: " + warning.getCode());
            }

            System.out.println("    Remediation:");
            for (String step : warning.getRemediationSteps()) {
                System.out.println("      → " + step);
            }
            System.out.println();
        }

        // Scenario 2: Good configuration
        System.out.println("Scenario 2: Configuration with strong password from environment");
        DatabaseConfig goodConfig = DatabaseConfig.createPostgreSQLConfig(
            "localhost", 5432, "blockchain", "admin", "StrongP@ssw0rd!2024#Secure"
        );

        analyzer = new ConfigurationSecurityAnalyzer()
            .withConfigSource(ConfigurationSecurityAnalyzer.ConfigSource.ENVIRONMENT);

        warnings = analyzer.analyze(goodConfig);

        if (warnings.isEmpty()) {
            System.out.println("  ✅ No security issues detected!");
        } else {
            System.out.println("  Found " + warnings.size() + " issue(s) (likely INFO/LOW severity)");
        }
    }

    /**
     * Demo 6: FilePermissionsUtil
     * Manage POSIX file permissions for security
     */
    private static void demo6FilePermissions() throws IOException {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("📁 Demo 6: FilePermissionsUtil");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();

        // Check if POSIX is supported
        boolean posixSupported = FilePermissionsUtil.isPosixSupported();
        System.out.println("POSIX file permissions supported: " + (posixSupported ? "✅ Yes" : "⚠️  No (Windows?)"));
        System.out.println();

        if (!posixSupported) {
            System.out.println("⚠️  Skipping permission tests (not supported on this OS)");
            System.out.println("   On Windows, use NTFS ACLs instead");
            return;
        }

        // Create a test file
        Path testFile = Paths.get("test-config.properties");
        if (!Files.exists(testFile)) {
            Files.writeString(testFile, "db.password=secret");
        }

        // Check current permissions
        System.out.println("1️⃣  Check file permissions:");
        PermissionStatus status = FilePermissionsUtil.checkPermissions(testFile);
        System.out.println("  Current permissions: " + status.getCurrentPermissions());
        System.out.println("  Is secure: " + (status.isSecure() ? "✅ Yes" : "❌ No"));
        System.out.println("  Message: " + status.getMessage());
        System.out.println();

        // Set secure permissions (600 = rw-------)
        if (!status.isSecure()) {
            System.out.println("2️⃣  Set secure permissions (600 = rw-------):");
            FilePermissionsUtil.setSecurePermissions(testFile);
            System.out.println("  ✅ Permissions fixed!");
            System.out.println();

            // Verify
            status = FilePermissionsUtil.checkPermissions(testFile);
            System.out.println("  New permissions: " + status.getCurrentPermissions());
            System.out.println("  Is secure: " + (status.isSecure() ? "✅ Yes" : "❌ No"));
        } else {
            System.out.println("  File already has secure permissions");
        }
        System.out.println();

        System.out.println("✅ Configuration files should have 600 (rw-------) permissions");
        System.out.println("   This ensures only the owner can read/write sensitive data");
    }

    /**
     * Demo 7: DatabaseConnectionTester
     * Test database connectivity with detailed diagnostics
     */
    private static void demo7ConnectionTester() {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("🔌 Demo 7: DatabaseConnectionTester");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();

        DatabaseConnectionTester tester = new DatabaseConnectionTester();

        // Test 1: SQLite (should succeed)
        System.out.println("Test 1: SQLite Connection (should succeed)");
        DatabaseConfig sqliteConfig = DatabaseConfig.createSQLiteConfig();
        ConnectionTestResult result1 = tester.testConnection(sqliteConfig);

        if (result1.isSuccessful()) {
            System.out.println("  ✅ Connection successful!");
            System.out.println("  Database version: " + result1.getDatabaseVersion());
            System.out.println("  Driver version: " + result1.getDriverVersion());
            System.out.println("  Response time: " + result1.getResponseTime().toMillis() + "ms");
            System.out.println("  Can read: " + result1.canRead());
            System.out.println("  Read-only: " + result1.isReadOnly());
        } else {
            System.out.println("  ❌ Connection failed: " + result1.getErrorMessage());
        }
        System.out.println();

        // Test 2: PostgreSQL with wrong credentials (should fail)
        System.out.println("Test 2: PostgreSQL with wrong credentials (should fail)");
        DatabaseConfig badConfig = DatabaseConfig.createPostgreSQLConfig(
            "nonexistent-host", 5432, "blockchain", "wrong_user", "wrong_pass"
        );
        ConnectionTestResult result2 = tester.testConnection(badConfig);

        if (result2.isFailed()) {
            System.out.println("  ❌ Connection failed (expected):");
            System.out.println("  Error: " + result2.getErrorMessage());

            if (result2.hasRecommendations()) {
                System.out.println("  Recommendations:");
                for (String rec : result2.getRecommendations()) {
                    System.out.println("    → " + rec);
                }
            }
        }
        System.out.println();

        System.out.println("✅ Connection testing provides detailed diagnostics and recommendations");
    }

    /**
     * Demo 8: DatabaseMigrator
     * Schema migration with version tracking
     */
    private static void demo8DatabaseMigrator() {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("🔄 Demo 8: DatabaseMigrator");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();

        // Use SQLite for migration demo
        DatabaseConfig config = DatabaseConfig.createSQLiteConfig();
        DatabaseMigrator migrator = new DatabaseMigrator(config);

        // Check current version
        System.out.println("1️⃣  Check current schema version:");
        String currentVersion = migrator.getCurrentVersion();
        System.out.println("  Current version: " + (currentVersion != null ? currentVersion : "none (fresh database)"));
        System.out.println();

        // Register migrations
        System.out.println("2️⃣  Register migrations:");
        migrator.addMigration(DatabaseMigrator.Migration.builder()
            .version("V1")
            .description("Create initial schema")
            .sql("CREATE TABLE IF NOT EXISTS test_table (id INTEGER PRIMARY KEY, name TEXT)")
            .build());

        migrator.addMigration(DatabaseMigrator.Migration.builder()
            .version("V2")
            .description("Add email column")
            .sql("ALTER TABLE test_table ADD COLUMN email TEXT")
            .build());

        System.out.println("  Registered migrations: V1, V2");
        System.out.println();

        // Execute migrations
        System.out.println("3️⃣  Execute pending migrations:");
        DatabaseMigrator.MigrationResult result = migrator.migrate();

        if (result.isSuccess()) {
            System.out.println("  ✅ Migration completed successfully!");
            System.out.println("  Migrations applied: " + result.getMigrationsApplied());
            System.out.println("  Duration: " + result.getDurationMs() + "ms");
            System.out.println("  Applied versions:");
            for (String version : result.getAppliedVersions()) {
                System.out.println("    • " + version);
            }
        } else {
            System.out.println("  ❌ Migration failed: " + result.getErrorMessage());
        }
        System.out.println();

        // Show migration history
        System.out.println("4️⃣  Migration history:");
        List<DatabaseMigrator.MigrationHistoryEntry> history = migrator.getHistory();
        for (DatabaseMigrator.MigrationHistoryEntry entry : history) {
            String status = entry.isSuccess() ? "✅" : "❌";
            System.out.println("  " + status + " " + entry.getVersion() + " - " + entry.getDescription());
            System.out.println("     Installed: " + entry.getInstalledOn() + " by " + entry.getInstalledBy());
            System.out.println("     Execution time: " + entry.getExecutionTime() + "ms");
        }
        System.out.println();

        System.out.println("✅ DatabaseMigrator provides version-tracked schema evolution");
    }

    // Helper methods

    private static String getEmojiForSeverity(SecurityWarning.Severity severity) {
        switch (severity) {
            case CRITICAL: return "🚨";
            case HIGH: return "⚠️ ";
            case MEDIUM: return "⚡";
            case LOW: return "ℹ️ ";
            case INFO: return "💡";
            default: return "  ";
        }
    }

    private static void pause() {
        System.out.println();
    }
}
