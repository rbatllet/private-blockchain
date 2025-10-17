package com.rbatllet.blockchain.config.util;

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for masking sensitive data in strings, connection strings, and properties.
 *
 * This class provides methods to redact sensitive information such as passwords, usernames,
 * hosts, and other credentials from various data formats. It is essential for secure logging,
 * output display, and data export operations.
 *
 * <p><b>Security-First Design:</b></p>
 * <ul>
 *   <li>All methods are thread-safe and stateless</li>
 *   <li>Original data is never modified (immutable approach)</li>
 *   <li>Masks passwords, usernames, hosts, and connection strings</li>
 *   <li>Supports JDBC URLs, properties files, and generic strings</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Mask a password
 * String masked = SensitiveDataMasker.maskPassword("secret123");
 * // Result: "***REDACTED***"
 *
 * // Mask a JDBC connection string
 * String jdbc = "jdbc:postgresql://localhost:5432/db?user=admin&password=secret123";
 * String safe = SensitiveDataMasker.maskConnectionString(jdbc);
 * // Result: "jdbc:postgresql://localhost:5432/db?user=***REDACTED***&password=***REDACTED***"
 *
 * // Mask properties
 * Properties props = new Properties();
 * props.setProperty("db.password", "secret");
 * props.setProperty("db.user", "admin");
 * Properties masked = SensitiveDataMasker.maskProperties(props);
 * // Result: db.password=***REDACTED***, db.user=***REDACTED***
 * }</pre>
 *
 * @since 1.0.6
 * @see com.rbatllet.blockchain.config.DatabaseConfig
 */
public final class SensitiveDataMasker {

    /**
     * Redaction marker used to replace sensitive data.
     */
    public static final String REDACTION_MARKER = "***REDACTED***";

    /**
     * Redaction marker with additional context for environment variables.
     */
    public static final String REDACTION_MARKER_ENV = "***REDACTED - Set via environment variable***";

    /**
     * List of sensitive property keys that should always be masked.
     */
    private static final String[] SENSITIVE_KEYS = {
        "password", "passwd", "pwd",
        "user", "username", "usr",
        "secret", "token", "key",
        "credential", "auth"
    };

    /**
     * Pattern to match password in JDBC URLs and query strings.
     * Matches: password=value, pwd=value, passwd=value (case insensitive)
     */
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "(password|pwd|passwd)=([^&\\s;]+)",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * Pattern to match username in JDBC URLs and query strings.
     * Matches: user=value, username=value, usr=value (case insensitive)
     */
    private static final Pattern USER_PATTERN = Pattern.compile(
        "(user|username|usr)=([^&\\s;]+)",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * Pattern to match credentials in URLs with @ syntax.
     * Matches: //username:password@host or //username@host
     */
    private static final Pattern URL_CREDENTIALS_PATTERN = Pattern.compile(
        "//([^:@]+)(:([^@]+))?@"
    );

    // Private constructor to prevent instantiation
    private SensitiveDataMasker() {
        throw new AssertionError("Utility class cannot be instantiated");
    }

    /**
     * Masks a password string with the redaction marker.
     *
     * @param password the password to mask (can be null)
     * @return the redaction marker, or null if input was null
     */
    public static String maskPassword(String password) {
        return password == null ? null : REDACTION_MARKER;
    }

    /**
     * Masks sensitive information in a JDBC connection string.
     *
     * <p>This method redacts:</p>
     * <ul>
     *   <li>Passwords in query parameters (password=, pwd=, passwd=)</li>
     *   <li>Usernames in query parameters (user=, username=, usr=)</li>
     *   <li>Credentials in URL format (user:password@host)</li>
     * </ul>
     *
     * @param connectionString the JDBC connection string to mask (can be null)
     * @return the masked connection string, or null if input was null
     *
     * @example
     * <pre>{@code
     * String jdbc = "jdbc:postgresql://admin:secret@localhost:5432/db?password=pwd123";
     * String safe = maskConnectionString(jdbc);
     * // Result: "jdbc:postgresql://***REDACTED***:***REDACTED***@localhost:5432/db?password=***REDACTED***"
     * }</pre>
     */
    public static String maskConnectionString(String connectionString) {
        if (connectionString == null || connectionString.isEmpty()) {
            return connectionString;
        }

        String masked = connectionString;

        // Mask password in query parameters
        Matcher passwordMatcher = PASSWORD_PATTERN.matcher(masked);
        masked = passwordMatcher.replaceAll("$1=" + REDACTION_MARKER);

        // Mask username in query parameters
        Matcher userMatcher = USER_PATTERN.matcher(masked);
        masked = userMatcher.replaceAll("$1=" + REDACTION_MARKER);

        // Mask credentials in URL format (user:password@host)
        Matcher urlMatcher = URL_CREDENTIALS_PATTERN.matcher(masked);
        if (urlMatcher.find()) {
            if (urlMatcher.group(3) != null) {
                // user:password@host format
                masked = urlMatcher.replaceAll("//" + REDACTION_MARKER + ":" + REDACTION_MARKER + "@");
            } else {
                // user@host format (no password)
                masked = urlMatcher.replaceAll("//" + REDACTION_MARKER + "@");
            }
        }

        return masked;
    }

    /**
     * Masks sensitive data in a Properties object.
     *
     * <p>Creates a new Properties object with sensitive values redacted.
     * The original Properties object is not modified.</p>
     *
     * <p>Sensitive keys include (case-insensitive):</p>
     * <ul>
     *   <li>Any key containing: password, passwd, pwd</li>
     *   <li>Any key containing: user, username, usr</li>
     *   <li>Any key containing: secret, token, key</li>
     *   <li>Any key containing: credential, auth</li>
     * </ul>
     *
     * @param properties the properties to mask (can be null)
     * @return a new Properties object with sensitive values masked, or null if input was null
     *
     * @example
     * <pre>{@code
     * Properties props = new Properties();
     * props.setProperty("db.password", "secret123");
     * props.setProperty("db.host", "localhost");
     * props.setProperty("db.postgresql.username", "admin");
     *
     * Properties masked = maskProperties(props);
     * // masked.getProperty("db.password") → "***REDACTED***"
     * // masked.getProperty("db.host") → "localhost" (not sensitive)
     * // masked.getProperty("db.postgresql.username") → "***REDACTED***"
     * }</pre>
     */
    public static Properties maskProperties(Properties properties) {
        return maskProperties(properties, false);
    }

    /**
     * Masks sensitive data in a Properties object with optional environment variable hint.
     *
     * <p>Creates a new Properties object with sensitive values redacted.
     * The original Properties object is not modified.</p>
     *
     * @param properties the properties to mask (can be null)
     * @param useEnvMarker if true, uses REDACTION_MARKER_ENV which suggests using environment variables
     * @return a new Properties object with sensitive values masked, or null if input was null
     *
     * @see #maskProperties(Properties)
     */
    public static Properties maskProperties(Properties properties, boolean useEnvMarker) {
        if (properties == null) {
            return null;
        }

        Properties masked = new Properties();
        String marker = useEnvMarker ? REDACTION_MARKER_ENV : REDACTION_MARKER;

        for (String key : properties.stringPropertyNames()) {
            String value = properties.getProperty(key);

            if (isSensitiveKey(key)) {
                // Mask the value if it's not already empty
                masked.setProperty(key, value.isEmpty() ? value : marker);
            } else {
                // Keep non-sensitive values unchanged
                masked.setProperty(key, value);
            }
        }

        return masked;
    }

    /**
     * Masks a generic string that may contain sensitive data.
     *
     * <p>This is a convenience method that applies connection string masking.
     * Use this for any string that might contain credentials in URL format
     * or query parameters.</p>
     *
     * @param text the text to mask (can be null)
     * @return the masked text, or null if input was null
     *
     * @see #maskConnectionString(String)
     */
    public static String maskSensitiveData(String text) {
        return maskConnectionString(text);
    }

    /**
     * Checks if a property key is sensitive and should be masked.
     *
     * <p>A key is considered sensitive if it contains (case-insensitive):</p>
     * <ul>
     *   <li>password, passwd, pwd</li>
     *   <li>user, username, usr</li>
     *   <li>secret, token, key</li>
     *   <li>credential, auth</li>
     * </ul>
     *
     * @param key the property key to check
     * @return true if the key is sensitive, false otherwise
     */
    public static boolean isSensitiveKey(String key) {
        if (key == null || key.isEmpty()) {
            return false;
        }

        String lowerKey = key.toLowerCase();

        for (String sensitivePattern : SENSITIVE_KEYS) {
            if (lowerKey.contains(sensitivePattern)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if a string contains sensitive data that should be masked.
     *
     * <p>Returns true if the string appears to contain:</p>
     * <ul>
     *   <li>Password parameters in URLs</li>
     *   <li>Username parameters in URLs</li>
     *   <li>Credentials in URL format (user:pass@host)</li>
     * </ul>
     *
     * @param text the text to check
     * @return true if the text appears to contain sensitive data
     */
    public static boolean containsSensitiveData(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        return PASSWORD_PATTERN.matcher(text).find() ||
               USER_PATTERN.matcher(text).find() ||
               URL_CREDENTIALS_PATTERN.matcher(text).find();
    }
}
