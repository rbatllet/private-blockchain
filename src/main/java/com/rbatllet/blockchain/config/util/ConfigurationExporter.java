package com.rbatllet.blockchain.config.util;

import com.rbatllet.blockchain.config.DatabaseConfig;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Exports database configuration to various formats.
 *
 * <p>This utility exports DatabaseConfig to:</p>
 * <ul>
 *   <li>Java Properties format (.properties)</li>
 *   <li>JSON format (.json)</li>
 *   <li>Environment variables format (.env)</li>
 * </ul>
 *
 * <p><b>Security Feature:</b> By default, sensitive data (passwords) are masked
 * in exported files. Use {@code withMasking(false)} to disable masking when
 * exporting to secure locations.</p>
 *
 * <p><b>Design Principle:</b> This class only handles export logic. It does NOT
 * validate configurations, test connections, or manage files - those are separate
 * responsibilities.</p>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * DatabaseConfig config = DatabaseConfig.createPostgreSQLConfig(
 *     "localhost", 5432, "blockchain", "admin", "password123"
 * );
 *
 * ConfigurationExporter exporter = new ConfigurationExporter();
 *
 * // Export to Properties (password masked)
 * String propsContent = exporter.exportToProperties(config);
 * Files.writeString(Path.of("database.properties"), propsContent);
 *
 * // Export to JSON (password masked)
 * String jsonContent = exporter.exportToJson(config);
 * Files.writeString(Path.of("database.json"), jsonContent);
 *
 * // Export without masking (for secure locations only!)
 * ConfigurationExporter unsafeExporter = new ConfigurationExporter().withMasking(false);
 * String unmaskedProps = unsafeExporter.exportToProperties(config);
 * }</pre>
 *
 * <p><b>Thread Safety:</b> This class is immutable and thread-safe.</p>
 *
 * @since 1.0.5
 * @see DatabaseConfig
 * @see SensitiveDataMasker
 */
public final class ConfigurationExporter {

    private final boolean maskSensitiveData;
    private final boolean prettyPrint;

    /**
     * Creates a new exporter with default settings (masking enabled, pretty print enabled).
     */
    public ConfigurationExporter() {
        this(true, true);
    }

    /**
     * Creates a new exporter with custom settings.
     *
     * @param maskSensitiveData whether to mask passwords and sensitive data
     * @param prettyPrint whether to format output for readability
     */
    private ConfigurationExporter(boolean maskSensitiveData, boolean prettyPrint) {
        this.maskSensitiveData = maskSensitiveData;
        this.prettyPrint = prettyPrint;
    }

    /**
     * Creates a new exporter with masking enabled/disabled.
     *
     * @param maskSensitiveData if true, passwords will be masked; if false, exported as-is
     * @return new exporter instance
     */
    public ConfigurationExporter withMasking(boolean maskSensitiveData) {
        return new ConfigurationExporter(maskSensitiveData, this.prettyPrint);
    }

    /**
     * Creates a new exporter with pretty printing enabled/disabled.
     *
     * @param prettyPrint if true, output will be formatted for readability
     * @return new exporter instance
     */
    public ConfigurationExporter withPrettyPrint(boolean prettyPrint) {
        return new ConfigurationExporter(this.maskSensitiveData, prettyPrint);
    }

    /**
     * Exports configuration to Java Properties format.
     *
     * <p>Format example:</p>
     * <pre>
     * db.type=postgresql
     * db.postgresql.host=localhost
     * db.postgresql.port=5432
     * db.postgresql.database=blockchain
     * db.postgresql.username=admin
     * db.postgresql.password=********
     * </pre>
     *
     * @param config database configuration to export
     * @return Properties formatted string
     * @throws IllegalArgumentException if config is null
     */
    public String exportToProperties(DatabaseConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("Database configuration cannot be null");
        }

        Properties props = buildProperties(config);

        // Apply masking if enabled
        if (maskSensitiveData) {
            props = SensitiveDataMasker.maskProperties(props);
        }

        // Convert to string with comments
        StringWriter writer = new StringWriter();
        PrintWriter out = new PrintWriter(writer);

        out.println("# Database Configuration");
        out.println("# Generated: " + java.time.Instant.now());
        out.println();

        // Database type
        out.println("# Database Type");
        out.println("db.type=" + config.getDatabaseType().name().toLowerCase());
        out.println();

        // Type-specific properties
        DatabaseConfig.DatabaseType type = config.getDatabaseType();
        switch (type) {
            case POSTGRESQL:
                exportPostgreSQLProperties(config, props, out);
                break;
            case MYSQL:
                exportMySQLProperties(config, props, out);
                break;
            case H2:
                exportH2Properties(config, props, out);
                break;
            case SQLITE:
                exportSQLiteProperties(config, props, out);
                break;
        }

        return writer.toString();
    }

    /**
     * Exports configuration to JSON format.
     *
     * <p>Format example:</p>
     * <pre>
     * {
     *   "type": "postgresql",
     *   "host": "localhost",
     *   "port": 5432,
     *   "database": "blockchain",
     *   "username": "admin",
     *   "password": "********"
     * }
     * </pre>
     *
     * @param config database configuration to export
     * @return JSON formatted string
     * @throws IllegalArgumentException if config is null
     * @throws RuntimeException if JSON serialization fails
     */
    public String exportToJson(DatabaseConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("Database configuration cannot be null");
        }

        Map<String, Object> data = buildConfigMap(config);

        // Apply masking if enabled
        if (maskSensitiveData && data.containsKey("password")) {
            data.put("password", "********");
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectWriter writer = mapper.writer();
            if (prettyPrint) {
                writer = writer.withDefaultPrettyPrinter();
            }
            return writer.writeValueAsString(data);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize configuration to JSON", e);
        }
    }

    /**
     * Exports configuration to environment variables format (.env).
     *
     * <p>Format example:</p>
     * <pre>
     * DB_TYPE=postgresql
     * DB_HOST=localhost
     * DB_PORT=5432
     * DB_DATABASE=blockchain
     * DB_USERNAME=admin
     * DB_PASSWORD=********
     * </pre>
     *
     * @param config database configuration to export
     * @return environment variables formatted string
     * @throws IllegalArgumentException if config is null
     */
    public String exportToEnv(DatabaseConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("Database configuration cannot be null");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("# Database Environment Variables\n");
        sb.append("# Generated: ").append(java.time.Instant.now()).append("\n");
        sb.append("\n");

        sb.append("DB_TYPE=").append(config.getDatabaseType().name().toLowerCase()).append("\n");

        DatabaseConfig.DatabaseType type = config.getDatabaseType();
        if (type != DatabaseConfig.DatabaseType.SQLITE) {
            // Extract host, port, database from URL
            String url = config.getDatabaseUrl();
            if (url != null) {
                String host = extractHost(url);
                int port = extractPort(url);
                String database = extractDatabase(url);

                if (host != null) {
                    sb.append("DB_HOST=").append(host).append("\n");
                }
                if (port > 0) {
                    sb.append("DB_PORT=").append(port).append("\n");
                }
                if (database != null) {
                    sb.append("DB_DATABASE=").append(database).append("\n");
                }
            }

            // Username
            if (config.getUsername() != null) {
                sb.append("DB_USERNAME=").append(config.getUsername()).append("\n");
            }

            // Password (masked if enabled)
            if (config.getPassword() != null) {
                String password = maskSensitiveData ? "********" : config.getPassword();
                sb.append("DB_PASSWORD=").append(password).append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * Exports configuration directly to a file.
     *
     * @param config database configuration to export
     * @param outputPath output file path
     * @param format export format (determined by file extension if null)
     * @throws IllegalArgumentException if config or outputPath is null
     * @throws IOException if file cannot be written
     */
    public void exportToFile(DatabaseConfig config, Path outputPath, ExportFormat format) throws IOException {
        if (config == null) {
            throw new IllegalArgumentException("Database configuration cannot be null");
        }
        if (outputPath == null) {
            throw new IllegalArgumentException("Output path cannot be null");
        }

        // Auto-detect format from extension if not provided
        if (format == null) {
            String fileName = outputPath.getFileName().toString().toLowerCase();
            if (fileName.endsWith(".properties")) {
                format = ExportFormat.PROPERTIES;
            } else if (fileName.endsWith(".json")) {
                format = ExportFormat.JSON;
            } else if (fileName.endsWith(".env")) {
                format = ExportFormat.ENV;
            } else {
                throw new IllegalArgumentException(
                    "Cannot determine format from file extension: " + fileName + ". Specify format explicitly."
                );
            }
        }

        String content;
        switch (format) {
            case PROPERTIES:
                content = exportToProperties(config);
                break;
            case JSON:
                content = exportToJson(config);
                break;
            case ENV:
                content = exportToEnv(config);
                break;
            default:
                throw new IllegalArgumentException("Unsupported format: " + format);
        }

        Files.writeString(outputPath, content, StandardCharsets.UTF_8);
    }

    /**
     * Builds a Properties object from DatabaseConfig.
     */
    private Properties buildProperties(DatabaseConfig config) {
        Properties props = new Properties();

        props.setProperty("db.type", config.getDatabaseType().name().toLowerCase());

        DatabaseConfig.DatabaseType type = config.getDatabaseType();
        String url = config.getDatabaseUrl();

        if (type != DatabaseConfig.DatabaseType.SQLITE && url != null) {
            String host = extractHost(url);
            int port = extractPort(url);
            String database = extractDatabase(url);

            String prefix = getPropertyPrefix(type);

            if (host != null) {
                props.setProperty(prefix + ".host", host);
            }
            if (port > 0) {
                props.setProperty(prefix + ".port", String.valueOf(port));
            }
            if (database != null) {
                props.setProperty(prefix + ".database", database);
            }
            if (config.getUsername() != null) {
                props.setProperty(prefix + ".username", config.getUsername());
            }
            if (config.getPassword() != null) {
                props.setProperty(prefix + ".password", config.getPassword());
            }
        }

        return props;
    }

    /**
     * Builds a Map from DatabaseConfig for JSON export.
     */
    private Map<String, Object> buildConfigMap(DatabaseConfig config) {
        Map<String, Object> map = new LinkedHashMap<>();

        map.put("type", config.getDatabaseType().name().toLowerCase());

        DatabaseConfig.DatabaseType type = config.getDatabaseType();
        if (type != DatabaseConfig.DatabaseType.SQLITE) {
            String url = config.getDatabaseUrl();
            if (url != null) {
                String host = extractHost(url);
                int port = extractPort(url);
                String database = extractDatabase(url);

                if (host != null) {
                    map.put("host", host);
                }
                if (port > 0) {
                    map.put("port", port);
                }
                if (database != null) {
                    map.put("database", database);
                }
            }

            if (config.getUsername() != null) {
                map.put("username", config.getUsername());
            }
            if (config.getPassword() != null) {
                map.put("password", config.getPassword());
            }
        }

        return map;
    }

    private void exportPostgreSQLProperties(DatabaseConfig config, Properties props, PrintWriter out) {
        out.println("# PostgreSQL Configuration");
        exportCommonProperties("db.postgresql", props, out);
    }

    private void exportMySQLProperties(DatabaseConfig config, Properties props, PrintWriter out) {
        out.println("# MySQL Configuration");
        exportCommonProperties("db.mysql", props, out);
    }

    private void exportH2Properties(DatabaseConfig config, Properties props, PrintWriter out) {
        out.println("# H2 Database Configuration");
        out.println("# Note: H2 is for testing only");
        // H2 specific properties would go here
    }

    private void exportSQLiteProperties(DatabaseConfig config, Properties props, PrintWriter out) {
        out.println("# SQLite Configuration");
        out.println("# SQLite uses in-memory database by default");
    }

    private void exportCommonProperties(String prefix, Properties props, PrintWriter out) {
        String host = props.getProperty(prefix + ".host");
        String port = props.getProperty(prefix + ".port");
        String database = props.getProperty(prefix + ".database");
        String username = props.getProperty(prefix + ".username");
        String password = props.getProperty(prefix + ".password");

        if (host != null) {
            out.println(prefix + ".host=" + host);
        }
        if (port != null) {
            out.println(prefix + ".port=" + port);
        }
        if (database != null) {
            out.println(prefix + ".database=" + database);
        }
        if (username != null) {
            out.println(prefix + ".username=" + username);
        }
        if (password != null) {
            out.println(prefix + ".password=" + password);
        }
    }

    private String getPropertyPrefix(DatabaseConfig.DatabaseType type) {
        switch (type) {
            case POSTGRESQL:
                return "db.postgresql";
            case MYSQL:
                return "db.mysql";
            case H2:
                return "db.h2";
            default:
                return "db";
        }
    }

    private String extractHost(String url) {
        try {
            if (url.contains("//")) {
                int start = url.indexOf("//") + 2;
                int end = url.indexOf(":", start);
                if (end > start) {
                    return url.substring(start, end);
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    private int extractPort(String url) {
        try {
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
        } catch (Exception e) {
            // Ignore
        }
        return 0;
    }

    private String extractDatabase(String url) {
        try {
            int start = url.lastIndexOf("/") + 1;
            int end = url.indexOf("?", start);
            if (end > 0) {
                return url.substring(start, end);
            } else if (start < url.length()) {
                return url.substring(start);
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    /**
     * Export format options.
     */
    public enum ExportFormat {
        /** Java Properties format */
        PROPERTIES,

        /** JSON format */
        JSON,

        /** Environment variables format */
        ENV
    }
}
