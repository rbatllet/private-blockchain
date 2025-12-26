package com.rbatllet.blockchain.config.util;

import com.rbatllet.blockchain.config.DatabaseConfig;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Analyzes database configuration for security concerns and best practices.
 *
 * <p>This analyzer detects common security issues such as:</p>
 * <ul>
 *   <li>Passwords stored in configuration files</li>
 *   <li>Insecure file permissions on configuration files</li>
 *   <li>Passwords visible in command-line arguments</li>
 *   <li>Missing encryption for database connections</li>
 *   <li>Weak or default credentials</li>
 * </ul>
 *
 * <p><b>Design Principle:</b> This class detects security issues and returns
 * structured warnings. It does NOT display warnings or make decisions about how
 * to present them - that's the responsibility of the presentation layer (CLI/Web).</p>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * DatabaseConfig config = DatabaseConfig.createPostgreSQL("localhost", 5432, "db", "user", "pass");
 * ConfigurationSecurityAnalyzer analyzer = new ConfigurationSecurityAnalyzer();
 *
 * List<SecurityWarning> warnings = analyzer.analyze(config);
 *
 * // CLI would display warnings to console
 * // Web would show as banners
 * // API would include in JSON response
 * for (SecurityWarning warning : warnings) {
 *     System.err.println(warning.getSeverity() + ": " + warning.getMessage());
 * }
 * }</pre>
 *
 * @since 1.0.5
 * @see SecurityWarning
 * @see DatabaseConfig
 */
public class ConfigurationSecurityAnalyzer {

    /**
     * Configuration file path (if known).
     * Used to check file permissions.
     */
    private Path configFilePath;

    /**
     * Source of configuration (for context in warnings).
     */
    private ConfigSource configSource;

    /**
     * Creates a new analyzer with default settings.
     */
    public ConfigurationSecurityAnalyzer() {
        this.configSource = ConfigSource.UNKNOWN;
    }

    /**
     * Sets the configuration file path for permission analysis.
     *
     * @param configFilePath the path to the configuration file
     * @return this analyzer (for method chaining)
     */
    public ConfigurationSecurityAnalyzer withConfigFile(Path configFilePath) {
        this.configFilePath = configFilePath;
        return this;
    }

    /**
     * Sets the configuration file path for permission analysis.
     *
     * @param configFilePath the path to the configuration file as string
     * @return this analyzer (for method chaining)
     */
    public ConfigurationSecurityAnalyzer withConfigFile(String configFilePath) {
        if (configFilePath != null && !configFilePath.isEmpty()) {
            this.configFilePath = Paths.get(configFilePath);
        }
        return this;
    }

    /**
     * Sets the configuration source for context in warnings.
     *
     * @param configSource the configuration source
     * @return this analyzer (for method chaining)
     */
    public ConfigurationSecurityAnalyzer withConfigSource(ConfigSource configSource) {
        this.configSource = configSource != null ? configSource : ConfigSource.UNKNOWN;
        return this;
    }

    /**
     * Analyzes a database configuration for security concerns.
     *
     * @param config the database configuration to analyze
     * @return list of security warnings (empty if no issues found)
     */
    public List<SecurityWarning> analyze(DatabaseConfig config) {
        List<SecurityWarning> warnings = new ArrayList<>();

        if (config == null) {
            warnings.add(SecurityWarning.critical("Database configuration is null"));
            return warnings;
        }

        // Check for password in configuration
        analyzePasswordStorage(config, warnings);

        // Check file permissions if config file is known
        if (configFilePath != null) {
            analyzeFilePermissions(warnings);
        }

        // Check connection security
        analyzeConnectionSecurity(config, warnings);

        // Check for weak credentials
        analyzeCredentialStrength(config, warnings);

        return warnings;
    }

    /**
     * Analyzes how the password is stored and warns if insecure.
     */
    private void analyzePasswordStorage(DatabaseConfig config, List<SecurityWarning> warnings) {
        // SQLite doesn't use passwords
        if (config.getDatabaseType() == DatabaseConfig.DatabaseType.SQLITE) {
            return;
        }

        String password = getPasswordFromConfig(config);

        if (password == null || password.isEmpty()) {
            // No password configured - could be using environment variable (good)
            // or no authentication (bad for production)
            if (configSource == ConfigSource.FILE || configSource == ConfigSource.CLI_ARGS) {
                warnings.add(SecurityWarning.builder()
                    .severity(SecurityWarning.Severity.INFO)
                    .message("No password found in " + configSource + ". Using environment variable is recommended.")
                    .code("NO_PASSWORD_IN_CONFIG")
                    .build());
            }
        } else {
            // Password is present
            if (configSource == ConfigSource.FILE) {
                warnings.add(SecurityWarning.builder()
                    .severity(SecurityWarning.Severity.HIGH)
                    .message("Database password loaded from configuration file")
                    .code("PASSWORD_IN_FILE")
                    .addRemediationStep("Use DB_PASSWORD environment variable instead")
                    .addRemediationStep("Remove password from configuration file")
                    .addRemediationStep("Follow 12-factor app principles: https://12factor.net/config")
                    .build());
            } else if (configSource == ConfigSource.CLI_ARGS) {
                warnings.add(SecurityWarning.builder()
                    .severity(SecurityWarning.Severity.CRITICAL)
                    .message("Database password provided via command-line arguments")
                    .code("PASSWORD_IN_CLI_ARGS")
                    .addRemediationStep("NEVER use --password in command-line (visible in process list)")
                    .addRemediationStep("Use DB_PASSWORD environment variable instead")
                    .addRemediationStep("Password is visible in: ps aux, shell history, CI/CD logs")
                    .build());
            }
        }
    }

    /**
     * Analyzes file permissions on the configuration file.
     */
    private void analyzeFilePermissions(List<SecurityWarning> warnings) {
        try {
            FilePermissionsUtil.PermissionStatus status =
                FilePermissionsUtil.checkPermissions(configFilePath);

            if (!status.exists()) {
                // File doesn't exist - not necessarily a problem
                return;
            }

            if (!status.isSupported()) {
                warnings.add(SecurityWarning.builder()
                    .severity(SecurityWarning.Severity.LOW)
                    .message("POSIX file permissions not supported on this system")
                    .code("POSIX_NOT_SUPPORTED")
                    .addRemediationStep("Ensure file access is restricted by other means (NTFS ACLs on Windows)")
                    .build());
                return;
            }

            if (!status.isSecure()) {
                warnings.add(SecurityWarning.builder()
                    .severity(SecurityWarning.Severity.HIGH)
                    .message("Configuration file has insecure permissions: " + status.getCurrentPermissions())
                    .code("INSECURE_FILE_PERMISSIONS")
                    .addRemediationStep("Set file permissions to 600 (rw-------)")
                    .addRemediationStep("Run: chmod 600 " + configFilePath)
                    .addRemediationStep("Current permissions allow group or others to read the file")
                    .build());
            }

        } catch (IOException e) {
            warnings.add(SecurityWarning.builder()
                .severity(SecurityWarning.Severity.MEDIUM)
                .message("Could not check file permissions: " + e.getMessage())
                .code("PERMISSION_CHECK_FAILED")
                .build());
        }
    }

    /**
     * Analyzes connection security (SSL/TLS).
     */
    private void analyzeConnectionSecurity(DatabaseConfig config, List<SecurityWarning> warnings) {
        // Only check SSL for network-based databases (PostgreSQL, MySQL)
        // Embedded databases (SQLite, H2) don't need SSL as they're local file-based
        DatabaseConfig.DatabaseType dbType = config.getDatabaseType();

        // Only warn for network databases
        if (dbType == DatabaseConfig.DatabaseType.POSTGRESQL ||
            dbType == DatabaseConfig.DatabaseType.MYSQL) {

            String jdbcUrl = config.getDatabaseUrl();
            if (jdbcUrl != null && !jdbcUrl.toLowerCase().contains("ssl")) {
                warnings.add(SecurityWarning.builder()
                    .severity(SecurityWarning.Severity.MEDIUM)
                    .message("Database connection may not be using SSL/TLS encryption")
                    .code("NO_SSL_DETECTED")
                    .addRemediationStep("Consider enabling SSL for database connections in production")
                    .addRemediationStep("PostgreSQL: Add ?ssl=true to JDBC URL")
                    .addRemediationStep("MySQL: Add ?useSSL=true&requireSSL=true to JDBC URL")
                    .build());
            }
        }
    }

    /**
     * Analyzes credential strength.
     */
    private void analyzeCredentialStrength(DatabaseConfig config, List<SecurityWarning> warnings) {
        if (config.getDatabaseType() == DatabaseConfig.DatabaseType.SQLITE) {
            return;
        }

        String password = getPasswordFromConfig(config);
        if (password != null && !password.isEmpty()) {
            // Check for weak passwords
            if (password.length() < 8) {
                warnings.add(SecurityWarning.builder()
                    .severity(SecurityWarning.Severity.MEDIUM)
                    .message("Database password is too short (less than 8 characters)")
                    .code("WEAK_PASSWORD_LENGTH")
                    .addRemediationStep("Use a password with at least 12 characters")
                    .addRemediationStep("Include uppercase, lowercase, numbers, and special characters")
                    .build());
            }

            // Check for common weak passwords
            String lowerPassword = password.toLowerCase();
            if (lowerPassword.equals("password") ||
                lowerPassword.equals("admin") ||
                lowerPassword.equals("root") ||
                lowerPassword.equals("123456") ||
                lowerPassword.equals("test")) {
                warnings.add(SecurityWarning.builder()
                    .severity(SecurityWarning.Severity.CRITICAL)
                    .message("Database is using a common/default password")
                    .code("COMMON_PASSWORD")
                    .addRemediationStep("IMMEDIATELY change to a strong, unique password")
                    .addRemediationStep("Use a password manager to generate strong passwords")
                    .addRemediationStep("Common passwords are easily guessed by attackers")
                    .build());
            }
        }
    }

    /**
     * Attempts to extract password from DatabaseConfig.
     * Returns null if not accessible (which is actually good for security).
     */
    private String getPasswordFromConfig(DatabaseConfig config) {
        try {
            // Try to get password directly
            String password = config.getPassword();
            if (password != null && !password.isEmpty()) {
                return password;
            }

            // Fallback: try to extract from JDBC URL
            String jdbcUrl = config.getDatabaseUrl();
            if (jdbcUrl != null && jdbcUrl.contains("password=")) {
                // Extract password from JDBC URL if present
                int startIdx = jdbcUrl.indexOf("password=") + 9;
                int endIdx = jdbcUrl.indexOf('&', startIdx);
                if (endIdx == -1) {
                    endIdx = jdbcUrl.length();
                }
                return jdbcUrl.substring(startIdx, endIdx);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Represents the source of configuration.
     */
    public enum ConfigSource {
        /** Configuration from environment variables (recommended) */
        ENVIRONMENT,

        /** Configuration from file (caution: may contain passwords) */
        FILE,

        /** Configuration from command-line arguments (not recommended) */
        CLI_ARGS,

        /** Configuration from code/defaults */
        DEFAULT,

        /** Unknown source */
        UNKNOWN
    }
}
