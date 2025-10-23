# Database Configuration Utilities API Reference

Comprehensive guide to database configuration utilities in `com.rbatllet.blockchain.config.util` package. These utilities provide secure, flexible, and production-ready database configuration management.

> **IMPORTANT NOTE**: All classes and methods documented here exist in the actual implementation. The code examples are tested and functional. This guide follows the project's documentation standards.

> **SECURITY FIRST**: These utilities prioritize security through password masking, permission validation, secure defaults, and comprehensive warning systems.

## 📋 Table of Contents

- [Overview](#overview)
- [DatabasePropertiesParser](#-databasepropertiesparser)
- [ConfigurationPriorityResolver](#-configurationpriorityresolver)
- [ConfigurationExporter](#-configurationexporter)
- [SensitiveDataMasker](#-sensitivedatamasker)
- [ConfigurationSecurityAnalyzer](#-configurationsecurityanalyzer)
- [SecurityWarning](#-securitywarning)
- [FilePermissionsUtil](#-filepermissionsutil)
- [DatabaseConnectionTester](#-databaseconnectiontester)
- [DatabaseMigrator](#-databasemigrator)
- [Best Practices](#-best-practices)

---

## Overview

The `com.rbatllet.blockchain.config.util` package provides 9 utility classes for database configuration management:

### Configuration Management
- **DatabasePropertiesParser** - Parse `.properties` files into `DatabaseConfig`
- **ConfigurationPriorityResolver** - Resolve configuration from multiple sources (CLI > ENV > FILE > DEFAULT)
- **ConfigurationExporter** - Export configuration with automatic sensitive data masking

### Security & Validation
- **SensitiveDataMasker** - Mask passwords and credentials in logs and output
- **ConfigurationSecurityAnalyzer** - Detect security risks and provide recommendations
- **SecurityWarning** - Structured security warning representation
- **FilePermissionsUtil** - Manage POSIX file permissions (600 = rw-------)

### Database Operations
- **DatabaseConnectionTester** - Test database connectivity with detailed diagnostics
- **DatabaseMigrator** - Schema migration with version tracking and history

### Design Principles

1. **Security by Default**: All utilities prioritize security (masking, warnings, permissions)
2. **Separation of Concerns**: CORE provides business logic, CLI provides presentation
3. **Immutability**: Most classes are immutable and thread-safe
4. **Comprehensive Validation**: Extensive error checking and user-friendly messages
5. **Reusability**: Can be used in CLI, web apps, REST APIs, or any Java application

---

## 🔧 DatabasePropertiesParser

**Package**: `com.rbatllet.blockchain.config.util`
**Since**: 1.0.5
**Thread-Safe**: ✅ Yes (stateless utility class)

### Purpose

Parses database configuration from Java Properties files and converts them into strongly-typed `DatabaseConfig` objects. Supports SQLite, PostgreSQL, MySQL, and H2 databases with validation and error reporting.

### Key Methods

```java
// Parse from InputStream
public static ParseResult parse(InputStream inputStream) throws IOException

// Parse from Reader
public static ParseResult parse(Reader reader) throws IOException

// Parse from Properties object (main method)
public static ParseResult parse(Properties properties)
```

### ParseResult Class

```java
public boolean isSuccess()          // Check if parsing succeeded
public boolean isFailure()          // Check if parsing failed
public DatabaseConfig getConfig()   // Get parsed configuration (null if failed)
public List<String> getErrors()     // Get error messages
public List<String> getWarnings()   // Get warning messages
public boolean hasErrors()          // Check if there are errors
public boolean hasWarnings()        // Check if there are warnings
```

### Usage Example

```java
import com.rbatllet.blockchain.config.util.DatabasePropertiesParser;
import com.rbatllet.blockchain.config.util.DatabasePropertiesParser.ParseResult;

try (InputStream in = Files.newInputStream(Paths.get("database.properties"))) {
    ParseResult result = DatabasePropertiesParser.parse(in);

    if (result.isSuccess()) {
        DatabaseConfig config = result.getConfig();
        System.out.println("✅ Configuration loaded: " + config.getDatabaseType());

        if (result.hasWarnings()) {
            for (String warning : result.getWarnings()) {
                System.out.println("⚠️  " + warning);
            }
        }
    } else {
        System.err.println("❌ Failed to parse configuration:");
        for (String error : result.getErrors()) {
            System.err.println("  - " + error);
        }
    }
}
```

### Property Format Examples

**PostgreSQL**:
```properties
db.type=postgresql
db.postgresql.host=localhost
db.postgresql.port=5432
db.postgresql.database=blockchain
db.postgresql.username=admin
# db.postgresql.password=  # Use DB_PASSWORD env var instead
```

**MySQL**:
```properties
db.type=mysql
db.mysql.host=localhost
db.mysql.port=3306
db.mysql.database=blockchain
db.mysql.username=root
```

**SQLite** (default):
```properties
db.type=sqlite
```

**See**: [DATABASE_AGNOSTIC.md](DATABASE_AGNOSTIC.md) for complete configuration guide.

---

## 🎯 ConfigurationPriorityResolver

**Package**: `com.rbatllet.blockchain.config.util`
**Since**: 1.0.5
**Thread-Safe**: ✅ Yes (immutable after build)

### Purpose

Resolves database configuration from multiple sources with priority handling. Implements the "configuration layering" pattern.

### Priority Order (Highest to Lowest)

1. **CLI Arguments** - Explicit user overrides
2. **Environment Variables** - System-wide settings (production recommended)
3. **Configuration File** - Project-specific defaults
4. **Application Defaults** - Hardcoded fallbacks

### Usage Example

```java
import com.rbatllet.blockchain.config.util.ConfigurationPriorityResolver;
import com.rbatllet.blockchain.config.util.ConfigurationPriorityResolver.ResolvedConfiguration;

ConfigurationPriorityResolver resolver = ConfigurationPriorityResolver.builder()
    .withCliArgs(cliConfig)              // Highest priority
    .withEnvironmentVars(envConfig)      // Second priority
    .withConfigFile(fileConfig)          // Third priority
    .withDefaults(defaultConfig)         // Lowest priority
    .build();

ResolvedConfiguration resolved = resolver.resolve();
DatabaseConfig finalConfig = resolved.getConfig();

// Track where each property came from
Map<String, ConfigSource> sources = resolved.getSourceMap();
System.out.println("Password from: " + sources.get("password"));
```

### Source Tracking

```java
ConfigSource passwordSource = resolved.getSource("password");

if (passwordSource == ConfigSource.CLI_ARGS) {
    System.err.println("⚠️  WARNING: Password provided via CLI arguments!");
    System.err.println("   This is insecure. Use environment variables instead.");
} else if (passwordSource == ConfigSource.ENVIRONMENT) {
    System.out.println("✅ Password loaded from environment variable (secure)");
}
```

**See**: Examples in Best Practices section below.

---

## 📤 ConfigurationExporter

**Package**: `com.rbatllet.blockchain.config.util`
**Since**: 1.0.5
**Thread-Safe**: ✅ Yes (immutable)

### Purpose

Exports `DatabaseConfig` to various formats (Properties, JSON, Environment Variables) with automatic sensitive data masking.

### Supported Formats

- **Properties** (`.properties`) - Java Properties format
- **JSON** (`.json`) - JSON format
- **Environment Variables** (`.env`) - Shell-compatible format

### Usage Example

```java
import com.rbatllet.blockchain.config.util.ConfigurationExporter;

DatabaseConfig config = DatabaseConfig.createPostgreSQLConfig(
    "localhost", 5432, "blockchain", "admin", "secretPassword123"
);

ConfigurationExporter exporter = new ConfigurationExporter();

// Export to Properties (password masked by default)
String propsContent = exporter.exportToProperties(config);
// db.postgresql.password=********

// Export to JSON
String jsonContent = exporter.exportToJson(config);

// Export to Environment Variables
String envContent = exporter.exportToEnv(config);

// Export directly to file
exporter.exportToFile(config, Paths.get("database.properties"), null);
```

### Security: Masking Configuration

```java
// Default: Passwords masked
ConfigurationExporter safe = new ConfigurationExporter();
String masked = safe.exportToProperties(config);
// password=********

// CAUTION: Disable masking only for secure storage (e.g., encrypted vaults)
ConfigurationExporter unsafe = new ConfigurationExporter()
    .withMasking(false);
String unmasked = unsafe.exportToProperties(config);
// password=secret123  (⚠️ plaintext!)
```

---

## 🔐 SensitiveDataMasker

**Package**: `com.rbatllet.blockchain.config.util`
**Since**: 1.0.5
**Thread-Safe**: ✅ Yes (stateless utility class)

### Purpose

Masks sensitive information (passwords, usernames, credentials) in strings, connection strings, and properties for secure logging and display.

### Key Methods

```java
public static String maskPassword(String password)
public static String maskConnectionString(String connectionString)
public static Properties maskProperties(Properties properties)
public static boolean isSensitiveKey(String key)
public static boolean containsSensitiveData(String text)
```

### Constants

```java
public static final String REDACTION_MARKER = "***REDACTED***";
public static final String REDACTION_MARKER_ENV = "***REDACTED - Set via environment variable***";
```

### Usage Example

```java
import com.rbatllet.blockchain.config.util.SensitiveDataMasker;

// Mask JDBC connection string
String jdbc = "jdbc:postgresql://admin:secret@localhost:5432/db?password=pwd123";
String safe = SensitiveDataMasker.maskConnectionString(jdbc);
// jdbc:postgresql://***REDACTED***:***REDACTED***@localhost:5432/db?password=***REDACTED***

// Mask properties for logging
Properties props = new Properties();
props.setProperty("db.password", "secret123");
props.setProperty("db.host", "localhost");

Properties masked = SensitiveDataMasker.maskProperties(props);
// db.password=***REDACTED***
// db.host=localhost  (not sensitive)

// Safe to log
logger.info("Database configuration: " + masked);
```

### Real-World Example

```java
// ❌ UNSAFE: Never log actual connection strings
logger.info("Connecting to: " + config.getDatabaseUrl());

// ✅ SAFE: Always mask sensitive data
String safeUrl = SensitiveDataMasker.maskConnectionString(config.getDatabaseUrl());
logger.info("Connecting to: " + safeUrl);
```

---

## 🛡️ ConfigurationSecurityAnalyzer

**Package**: `com.rbatllet.blockchain.config.util`
**Since**: 1.0.5
**Thread-Safe**: ✅ Yes (stateless)

### Purpose

Analyzes database configuration for security concerns and best practices. Detects common security issues such as passwords in files, weak credentials, insecure permissions, and missing encryption.

### Key Methods

```java
public List<SecurityWarning> analyze(DatabaseConfig config)
public ConfigurationSecurityAnalyzer withConfigFile(Path configFilePath)
public ConfigurationSecurityAnalyzer withConfigSource(ConfigSource configSource)
```

### Usage Example

```java
import com.rbatllet.blockchain.config.util.ConfigurationSecurityAnalyzer;
import com.rbatllet.blockchain.config.util.SecurityWarning;

ConfigurationSecurityAnalyzer analyzer = new ConfigurationSecurityAnalyzer()
    .withConfigFile(Paths.get("database.properties"))
    .withConfigSource(ConfigurationSecurityAnalyzer.ConfigSource.FILE);

List<SecurityWarning> warnings = analyzer.analyze(config);

for (SecurityWarning warning : warnings) {
    System.err.println(warning.getSeverity() + ": " + warning.getMessage());

    for (String step : warning.getRemediationSteps()) {
        System.err.println("  → " + step);
    }
}
```

### Common Security Issues Detected

1. **Password in CLI Arguments** (CRITICAL)
2. **Common/Default Password** (CRITICAL)
3. **Password in Configuration File** (HIGH)
4. **Insecure File Permissions** (HIGH)
5. **Weak Password** (MEDIUM)
6. **No SSL Detected** (MEDIUM)

### Handling Critical Issues

```java
List<SecurityWarning> warnings = analyzer.analyze(config);

long criticalCount = warnings.stream()
    .filter(w -> w.getSeverity() == SecurityWarning.Severity.CRITICAL)
    .count();

if (criticalCount > 0) {
    System.err.println("🚨 CRITICAL SECURITY ISSUES DETECTED:");
    warnings.stream()
        .filter(w -> w.getSeverity() == SecurityWarning.Severity.CRITICAL)
        .forEach(w -> {
            System.err.println("  - " + w.getMessage());
            w.getRemediationSteps().forEach(step ->
                System.err.println("    → " + step));
        });

    throw new SecurityException("Critical security issues must be resolved");
}
```

---

## ⚠️ SecurityWarning

**Package**: `com.rbatllet.blockchain.config.util`
**Since**: 1.0.5
**Thread-Safe**: ✅ Yes (immutable)

### Purpose

Represents a security warning with severity levels, messages, remediation steps, and optional warning codes.

### Severity Levels

```java
public enum Severity {
    CRITICAL,  // Immediate action required
    HIGH,      // Should be addressed soon
    MEDIUM,    // Should be reviewed
    LOW,       // Optional improvement
    INFO       // Informational only
}
```

### Usage Example

```java
import com.rbatllet.blockchain.config.util.SecurityWarning;

// Create detailed warning
SecurityWarning warning = SecurityWarning.builder()
    .severity(SecurityWarning.Severity.CRITICAL)
    .message("Database password provided via command-line arguments")
    .code("PASSWORD_IN_CLI_ARGS")
    .addRemediationStep("NEVER use --password in command-line")
    .addRemediationStep("Use DB_PASSWORD environment variable instead")
    .addRemediationStep("Password visible in: ps aux, shell history, CI/CD logs")
    .build();

// Quick creation
SecurityWarning critical = SecurityWarning.critical("Database using default password");
SecurityWarning high = SecurityWarning.high("Password stored in file");
SecurityWarning medium = SecurityWarning.medium("Connection not using SSL");
```

### Common Warning Codes

| Code | Severity | Description |
|------|----------|-------------|
| `PASSWORD_IN_CLI_ARGS` | CRITICAL | Password in command-line arguments |
| `COMMON_PASSWORD` | CRITICAL | Using common/default password |
| `PASSWORD_IN_FILE` | HIGH | Password stored in configuration file |
| `INSECURE_FILE_PERMISSIONS` | HIGH | File has insecure permissions |
| `NO_SSL_DETECTED` | MEDIUM | Database connection not using SSL/TLS |
| `WEAK_PASSWORD_LENGTH` | MEDIUM | Password too short |

---

## 📁 FilePermissionsUtil

**Package**: `com.rbatllet.blockchain.config.util`
**Since**: 1.0.5
**Thread-Safe**: ✅ Yes (stateless utility class)

### Purpose

Manages POSIX file permissions for security-sensitive files. Provides methods to check and set secure permissions (600 = rw-------).

### Platform Compatibility

- **Unix-like systems** (Linux, macOS, BSD): ✅ Full POSIX support
- **Windows**: ⚠️ Limited support, may throw `UnsupportedOperationException`

### Key Methods

```java
public static PermissionStatus checkPermissions(Path path) throws IOException
public static void setSecurePermissions(Path path) throws IOException
public static void setPermissions(Path path, String permissionString) throws IOException
public static String getPermissionsString(Path path) throws IOException
public static boolean isPosixSupported()
```

### Constants

```java
public static final String SECURE_PERMISSIONS = "rw-------";  // 600
```

### Usage Example

```java
import com.rbatllet.blockchain.config.util.FilePermissionsUtil;
import com.rbatllet.blockchain.config.util.FilePermissionsUtil.PermissionStatus;

Path configFile = Paths.get("/etc/blockchain/database.properties");

PermissionStatus status = FilePermissionsUtil.checkPermissions(configFile);

if (!status.isSecure()) {
    System.err.println("⚠️  WARNING: " + status.getMessage());
    System.err.println("Current permissions: " + status.getCurrentPermissions());

    // Fix permissions
    FilePermissionsUtil.setSecurePermissions(configFile);
    System.out.println("✅ Permissions fixed to 600 (rw-------)");
}
```

### Check POSIX Support

```java
if (FilePermissionsUtil.isPosixSupported()) {
    PermissionStatus status = FilePermissionsUtil.checkPermissions(configFile);

    if (!status.isSecure()) {
        FilePermissionsUtil.setSecurePermissions(configFile);
    }
} else {
    System.out.println("⚠️  POSIX not supported (Windows?)");
    System.out.println("   Use OS-specific security (NTFS ACLs, etc.)");
}
```

---

## 🔌 DatabaseConnectionTester

**Package**: `com.rbatllet.blockchain.config.util`
**Since**: 1.0.5
**Thread-Safe**: ✅ Yes (stateless)

### Purpose

Tests database connections and validates configuration. Performs comprehensive testing including connectivity, authentication, permissions, response time, and version detection.

### Key Methods

```java
public DatabaseConnectionTester()
public DatabaseConnectionTester(Duration connectionTimeout, Duration queryTimeout)
public ConnectionTestResult testConnection(DatabaseConfig config)
```

### ConnectionTestResult Class

```java
public boolean isSuccessful()
public boolean isFailed()
public String getErrorMessage()
public String getDatabaseVersion()
public String getDriverVersion()
public Duration getResponseTime()
public boolean canRead()
public boolean isReadOnly()
public List<String> getRecommendations()
public String getSummary()
```

### Usage Example

```java
import com.rbatllet.blockchain.config.util.DatabaseConnectionTester;
import com.rbatllet.blockchain.config.util.DatabaseConnectionTester.ConnectionTestResult;

DatabaseConnectionTester tester = new DatabaseConnectionTester();
ConnectionTestResult result = tester.testConnection(config);

if (result.isSuccessful()) {
    System.out.println("✅ Connection successful!");
    System.out.println("Database version: " + result.getDatabaseVersion());
    System.out.println("Response time: " + result.getResponseTime().toMillis() + "ms");
    System.out.println("Can read: " + result.canRead());
    System.out.println("Read-only: " + result.isReadOnly());

} else {
    System.err.println("❌ Connection failed: " + result.getErrorMessage());

    for (String recommendation : result.getRecommendations()) {
        System.err.println("  → " + recommendation);
    }
}
```

### Pre-Startup Validation

```java
// Test connection before starting application
DatabaseConnectionTester tester = new DatabaseConnectionTester();
ConnectionTestResult result = tester.testConnection(config);

if (result.isFailed()) {
    System.err.println("❌ Cannot start: Database connection failed");
    System.err.println("Error: " + result.getErrorMessage());

    for (String rec : result.getRecommendations()) {
        System.err.println("  → " + rec);
    }

    System.exit(1);
}

System.out.println("✅ Database connection OK");
System.out.println("   Version: " + result.getDatabaseVersion());
System.out.println("   Response: " + result.getResponseTime().toMillis() + "ms");
```

---

## 🔄 DatabaseMigrator

**Package**: `com.rbatllet.blockchain.config.util`
**Since**: 1.0.5
**Thread-Safe**: ❌ No (instance-level, single thread only)

### Purpose

Provides database schema migration capabilities with version tracking, transactional execution, migration history, and validation.

### Key Features

- Version-based migration tracking (V1, V2, V3...)
- Transactional migration execution
- Migration history table (`schema_version`)
- Schema version validation
- Checksum verification
- Idempotent migrations

### Key Methods

```java
public DatabaseMigrator(DatabaseConfig config)
public DatabaseMigrator addMigration(Migration migration)
public DatabaseMigrator addMigrations(List<Migration> migrations)
public MigrationResult migrate()
public String getCurrentVersion()
public List<MigrationHistoryEntry> getHistory()
public ValidationResult validate()
```

### Usage Example

```java
import com.rbatllet.blockchain.config.util.DatabaseMigrator;
import com.rbatllet.blockchain.config.util.DatabaseMigrator.Migration;
import com.rbatllet.blockchain.config.util.DatabaseMigrator.MigrationResult;

DatabaseMigrator migrator = new DatabaseMigrator(config);

// Register migrations
migrator.addMigration(Migration.builder()
    .version("V1")
    .description("Create blocks table")
    .sql("CREATE TABLE blocks (id BIGINT PRIMARY KEY, hash VARCHAR(64), data TEXT)")
    .build());

migrator.addMigration(Migration.builder()
    .version("V2")
    .description("Add previous_hash column")
    .sql("ALTER TABLE blocks ADD COLUMN previous_hash VARCHAR(64)")
    .build());

// Execute all pending migrations
MigrationResult result = migrator.migrate();

if (result.isSuccess()) {
    System.out.println("✅ Migrations applied: " + result.getMigrationsApplied());
    System.out.println("Duration: " + result.getDurationMs() + "ms");

    for (String version : result.getAppliedVersions()) {
        System.out.println("  - " + version);
    }
} else {
    System.err.println("❌ Migration failed: " + result.getErrorMessage());
}
```

### Check Current Version

```java
String currentVersion = migrator.getCurrentVersion();

if (currentVersion != null) {
    System.out.println("Current schema version: " + currentVersion);
} else {
    System.out.println("No migrations applied yet");
}
```

### View Migration History

```java
List<MigrationHistoryEntry> history = migrator.getHistory();

System.out.println("Migration History:");
for (MigrationHistoryEntry entry : history) {
    String status = entry.isSuccess() ? "✅" : "❌";

    System.out.printf("%s %s - %s%n",
        status, entry.getVersion(), entry.getDescription());
    System.out.printf("   Installed: %s by %s%n",
        entry.getInstalledOn(), entry.getInstalledBy());
    System.out.printf("   Execution time: %dms%n",
        entry.getExecutionTime());
}
```

---

## ✅ Best Practices

### 1. Security

#### Never Store Passwords in Configuration Files

```java
// ❌ WRONG
Properties props = new Properties();
props.setProperty("db.password", "secret123");

// ✅ CORRECT - Use environment variable
String password = System.getenv("DB_PASSWORD");
```

#### Always Mask Sensitive Data in Logs

```java
// ❌ WRONG
logger.info("Connecting to: " + config.getDatabaseUrl());

// ✅ CORRECT
String safeUrl = SensitiveDataMasker.maskConnectionString(config.getDatabaseUrl());
logger.info("Connecting to: " + safeUrl);
```

#### Check File Permissions

```java
Path configFile = Paths.get("database.properties");

PermissionStatus status = FilePermissionsUtil.checkPermissions(configFile);
if (!status.isSecure()) {
    FilePermissionsUtil.setSecurePermissions(configFile);
}
```

#### Analyze Configuration Security

```java
ConfigurationSecurityAnalyzer analyzer = new ConfigurationSecurityAnalyzer()
    .withConfigFile(configFile)
    .withConfigSource(ConfigSource.FILE);

List<SecurityWarning> warnings = analyzer.analyze(config);

// Fail on critical issues
long criticalCount = warnings.stream()
    .filter(w -> w.getSeverity() == SecurityWarning.Severity.CRITICAL)
    .count();

if (criticalCount > 0) {
    throw new SecurityException("Critical security issues detected");
}
```

### 2. Configuration Management

#### Use Priority Resolver for Multiple Sources

```java
ConfigurationPriorityResolver resolver = ConfigurationPriorityResolver.builder()
    .withCliArgs(cliConfig)              // Highest priority
    .withEnvironmentVars(envConfig)      // Second priority (RECOMMENDED for production)
    .withConfigFile(fileConfig)          // Third priority
    .withDefaults(defaultConfig)         // Lowest priority
    .build();

ResolvedConfiguration resolved = resolver.resolve();
DatabaseConfig finalConfig = resolved.getConfig();
```

#### Always Parse with Error Handling

```java
try (InputStream in = Files.newInputStream(configPath)) {
    ParseResult result = DatabasePropertiesParser.parse(in);

    if (result.isFailure()) {
        for (String error : result.getErrors()) {
            logger.error("Configuration error: {}", error);
        }
        throw new ConfigurationException("Failed to parse configuration");
    }

    // Log warnings
    for (String warning : result.getWarnings()) {
        logger.warn("Configuration warning: {}", warning);
    }

    return result.getConfig();
}
```

#### Test Connection Before Use

```java
DatabaseConnectionTester tester = new DatabaseConnectionTester();
ConnectionTestResult result = tester.testConnection(config);

if (result.isFailed()) {
    logger.error("Database connection failed: {}", result.getErrorMessage());

    for (String rec : result.getRecommendations()) {
        logger.info("Recommendation: {}", rec);
    }

    throw new DatabaseException("Cannot connect to database");
}
```

### 3. Export and Documentation

#### Export with Masking by Default

```java
ConfigurationExporter exporter = new ConfigurationExporter();
String safeProps = exporter.exportToProperties(config);

// Safe to share (passwords masked)
Files.writeString(Paths.get("example-config.properties"), safeProps);
```

#### Use Export for Templates

```java
DatabaseConfig template = DatabaseConfig.createPostgreSQLConfig(
    "localhost", 5432, "blockchain", "your-username", ""
);

ConfigurationExporter exporter = new ConfigurationExporter();
String templateContent = exporter.exportToProperties(template);

Files.writeString(Paths.get("database.properties.example"), templateContent);
```

### 4. Migration Management

#### Always Version Migrations

```java
// Good version naming: V1, V2, V3, V10, V11...
Migration m1 = Migration.builder()
    .version("V1")
    .description("Create initial schema")
    .sql("CREATE TABLE ...")
    .build();
```

#### Check Current Version Before Migrating

```java
DatabaseMigrator migrator = new DatabaseMigrator(config);
String currentVersion = migrator.getCurrentVersion();

logger.info("Current schema version: {}",
    currentVersion != null ? currentVersion : "none");

// Add and execute pending migrations
migrator.addMigrations(pendingMigrations);
MigrationResult result = migrator.migrate();
```

#### Validate After Migration

```java
MigrationResult result = migrator.migrate();

if (result.isSuccess()) {
    ValidationResult validation = migrator.validate();

    if (!validation.isValid()) {
        logger.warn("Post-migration validation warnings:");
        validation.getIssues().forEach(issue -> logger.warn("  - {}", issue));
    }
}
```

---

## 📚 Related Documentation

### Database Configuration
- **[DATABASE_AGNOSTIC.md](DATABASE_AGNOSTIC.md)** - Complete database configuration guide
- **[DATABASE_FIELD_LIMITS.md](DATABASE_FIELD_LIMITS.md)** - Database field size limits
- **[CONFIGURATION_STORAGE_GUIDE.md](CONFIGURATION_STORAGE_GUIDE.md)** - JPAConfigurationStorage guide

### Reference Documentation
- **[../reference/API_GUIDE.md](../reference/API_GUIDE.md)** - Complete API reference
- **[../reference/UTILITY_CLASSES_GUIDE.md](../reference/UTILITY_CLASSES_GUIDE.md)** - Other utility classes

### Security
- **[../security/SECURITY_GUIDE.md](../security/SECURITY_GUIDE.md)** - Security best practices
- **[../security/KEY_MANAGEMENT_GUIDE.md](../security/KEY_MANAGEMENT_GUIDE.md)** - Key management

### Deployment
- **[../deployment/PRODUCTION_GUIDE.md](../deployment/PRODUCTION_GUIDE.md)** - Production deployment guide

---

**Document Version**: 1.0.5
**Last Updated**: October 2025
**Package**: `com.rbatllet.blockchain.config.util`
**Maintained By**: Private Blockchain Team
