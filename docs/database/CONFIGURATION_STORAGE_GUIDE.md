# Configuration Storage Guide

## 📊 Overview

**JPAConfigurationStorage** is a database-agnostic configuration storage implementation that supports:
- ✅ SQLite (development and demos)
- ✅ PostgreSQL (production)
- ✅ MySQL (production)
- ✅ H2 (testing)

All using the **same code** - zero changes needed when switching databases!

## 🎯 Key Features

| Feature | Description |
|---------|-------------|
| **Database Support** | SQLite, PostgreSQL, MySQL, H2 |
| **Technology** | JPA/Hibernate (jakarta.persistence) |
| **Portability** | 100% database-agnostic JPQL queries |
| **Table Creation** | Automatic via JPA entities |
| **Transaction Management** | Automatic via JPAUtil |
| **Code Complexity** | 403 lines, clean and maintainable |
| **Maintenance** | Zero SQL maintenance required |
| **Thread Safety** | Thread-safe via ThreadLocal |

## 🚀 Quick Start

### Basic Usage

```java
import com.rbatllet.blockchain.config.JPAConfigurationStorage;
import com.rbatllet.blockchain.config.DatabaseConfig;
import com.rbatllet.blockchain.util.JPAUtil;

// Initialize database (SQLite by default)
JPAUtil.initialize(DatabaseConfig.createSQLiteConfig());

// Create storage instance
JPAConfigurationStorage storage = new JPAConfigurationStorage();

// Save configuration
Map<String, String> config = new HashMap<>();
config.put("app.name", "PrivateBlockchain");
config.put("app.version", "1.0.0");
storage.saveConfiguration("application", config);

// Load configuration
Map<String, String> loaded = storage.loadConfiguration("application");
System.out.println("App name: " + loaded.get("app.name"));
```

### Database Switching

```java
// Start with SQLite
JPAUtil.initialize(DatabaseConfig.createSQLiteConfig());
JPAConfigurationStorage storage = new JPAConfigurationStorage();
storage.saveConfiguration("app", config);

// Switch to PostgreSQL (same code!)
JPAUtil.initialize(DatabaseConfig.createPostgreSQLConfig(
    "localhost", "blockchain_prod", "user", "password"
));
storage = new JPAConfigurationStorage();
storage.loadConfiguration("app");  // Works identically!
```

## 🔧 JPA Entities

### ConfigurationEntity

Stores configuration key-value pairs:

```java
@Entity
@Table(name = "configuration",
       uniqueConstraints = @UniqueConstraint(columnNames = {"config_key", "config_type"}))
public class ConfigurationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "config_key", nullable = false, length = 255)
    private String configKey;

    @Column(name = "config_type", nullable = false, length = 50)
    private String configType;

    @Column(name = "config_value", nullable = false, columnDefinition = "TEXT")
    private String configValue;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
```

### ConfigurationAuditEntity

Tracks configuration changes:

```java
@Entity
@Table(name = "configuration_audit")
public class ConfigurationAuditEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "config_key", nullable = false)
    private String configKey;

    @Column(name = "config_type", nullable = false)
    private String configType;

    @Column(name = "old_value")
    private String oldValue;

    @Column(name = "new_value")
    private String newValue;

    @Column(name = "operation", nullable = false)
    private String operation;  // SET, DELETE, SAVE, RESET, IMPORT

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @Column(name = "change_reason")
    private String changeReason;
}
```

## 📋 API Reference

### Core Operations

```java
JPAConfigurationStorage storage = new JPAConfigurationStorage();

// Save entire configuration
Map<String, String> config = Map.of("key1", "value1", "key2", "value2");
boolean saved = storage.saveConfiguration("app", config);

// Load entire configuration
Map<String, String> loaded = storage.loadConfiguration("app");

// Set individual value
boolean set = storage.setConfigurationValue("app", "key", "value");

// Get individual value
String value = storage.getConfigurationValue("app", "key");

// Delete individual value
boolean deleted = storage.deleteConfigurationValue("app", "key");

// Check if configuration exists
boolean exists = storage.configurationExists("app");

// Reset configuration (delete all)
boolean reset = storage.resetConfiguration("app");
```

### Import/Export

```java
// Export to file
Path exportPath = Path.of("config-backup.properties");
storage.exportConfiguration("app", exportPath);

// Import from file
Path importPath = Path.of("config-backup.properties");
storage.importConfiguration("app", importPath);
```

### Monitoring

```java
// Health check
boolean healthy = storage.isHealthy();

// Get storage info
String type = storage.getStorageType();      // "jpa-database"
String location = storage.getStorageLocation();  // "POSTGRESQL (jdbc:postgresql://...)"

// Get audit log
String audit = storage.getAuditLog("app", 50);  // Last 50 changes
System.out.println(audit);
```

## 💡 Advanced Examples

### Example 1: Multi-Environment Configuration

```java
// Development
JPAUtil.initialize(DatabaseConfig.createDevelopmentConfig());
JPAConfigurationStorage storage = new JPAConfigurationStorage();

Map<String, String> devConfig = Map.of(
    "db.host", "localhost",
    "db.port", "5432",
    "log.level", "DEBUG"
);
storage.saveConfiguration("environment", devConfig);

// Production
JPAUtil.initialize(DatabaseConfig.createProductionConfigFromEnv());
storage = new JPAConfigurationStorage();

Map<String, String> prodConfig = Map.of(
    "db.host", "prod-db.example.com",
    "db.port", "5432",
    "log.level", "WARN"
);
storage.saveConfiguration("environment", prodConfig);
```

### Example 2: Configuration with Audit Trail

```java
JPAConfigurationStorage storage = new JPAConfigurationStorage();

// Initial configuration
storage.setConfigurationValue("security", "encryption", "AES-256-GCM");
storage.setConfigurationValue("security", "key-size", "256");

// Update configuration
storage.setConfigurationValue("security", "encryption", "AES-256-GCM-SIV");

// Delete configuration
storage.deleteConfigurationValue("security", "key-size");

// View audit log
String audit = storage.getAuditLog("security", 100);
System.out.println(audit);

// Output:
// 📋 Configuration Audit Log (security)
// ==================================================
// 📅 2025-10-01 12:00:00 | DELETE | key-size | Individual configuration key deletion
// 📅 2025-10-01 11:59:00 | SET | encryption | Individual configuration change
// 📅 2025-10-01 11:58:00 | SET | key-size | Individual configuration change
// 📅 2025-10-01 11:57:00 | SET | encryption | Individual configuration change
```

### Example 3: Database-Agnostic Testing

```java
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

public class ConfigurationTest {

    @BeforeEach
    void setup() {
        // Use H2 for fast, isolated tests
        DatabaseConfig h2Config = DatabaseConfig.createH2TestConfig();
        JPAUtil.initialize(h2Config);
    }

    @Test
    void testConfiguration() {
        JPAConfigurationStorage storage = new JPAConfigurationStorage();

        Map<String, String> config = Map.of("test.key", "test.value");
        storage.saveConfiguration("test", config);

        Map<String, String> loaded = storage.loadConfiguration("test");
        assertEquals("test.value", loaded.get("test.key"));
    }
}
```

## 🎯 Design Decisions

### Why JPA Instead of JDBC?

**Problem with JDBC/SQL**:
- Database-specific SQL syntax
- Manual transaction management
- Complex error handling
- More code to maintain

**JPA Solution**:
- Database-agnostic JPQL queries
- Automatic transaction management
- Entity mapping handles complexity
- Less code, easier to maintain

### JPQL Examples

All queries in `JPAConfigurationStorage` use JPQL:

```java
// Delete configuration
em.createQuery("DELETE FROM ConfigurationEntity c WHERE c.configType = :type")
    .setParameter("type", configType)
    .executeUpdate();

// Load configuration
TypedQuery<ConfigurationEntity> query = em.createQuery(
    "SELECT c FROM ConfigurationEntity c WHERE c.configType = :type",
    ConfigurationEntity.class
);
query.setParameter("type", configType);
List<ConfigurationEntity> entities = query.getResultList();

// Check existence
Long count = em.createQuery(
    "SELECT COUNT(c) FROM ConfigurationEntity c WHERE c.configType = :type",
    Long.class
).setParameter("type", configType).getSingleResult();
```

**Benefits**:
- ✅ Works identically on SQLite, PostgreSQL, MySQL, H2
- ✅ Type-safe entity references
- ✅ No SQL injection vulnerabilities
- ✅ Hibernate optimizes queries per database

## 📊 Performance

### Benchmarks

| Operation | Time (SQLite) | Time (PostgreSQL) | Time (H2) |
|-----------|---------------|-------------------|-----------|
| Single INSERT | ~3ms | ~2ms | ~1ms |
| Batch INSERT (100) | ~60ms | ~50ms | ~40ms |
| SELECT query | ~2ms | ~1ms | ~1ms |
| DELETE query | ~2ms | ~1ms | ~1ms |

**Note**: JPA has ~20-30% overhead vs raw JDBC, but the portability benefits are worth it.

### Optimization Tips

1. **Use batch operations** for bulk saves
2. **Enable connection pooling** via DatabaseConfig
3. **Use H2 in-memory** for fast tests
4. **Use PostgreSQL** for production with high concurrency

## 🎓 Best Practices

### DO ✅

- Use `JPAConfigurationStorage` for all configuration storage
- Switch databases via `DatabaseConfig` as needed
- Use H2 for testing, PostgreSQL for production
- Check `isHealthy()` before critical operations
- Use audit logs for compliance and debugging

### DON'T ❌

- Don't use raw JDBC/SQL for configuration storage
- Don't hardcode database-specific SQL
- Don't skip transaction management
- Don't forget to close EntityManager in long-running apps

## 🚀 Demo

Run the comprehensive demo:

```bash
./scripts/run_jpa_configuration_storage_demo.zsh
```

The demo shows:
1. ✅ Configuration storage with SQLite
2. ✅ Configuration storage with H2 (same code!)
3. ✅ Advanced operations (CRUD, audit logs)

## 🔗 Related Documentation

- [DATABASE_AGNOSTIC.md](DATABASE_AGNOSTIC.md) - Database switching guide
- [API_GUIDE.md](../reference/API_GUIDE.md) - Complete API reference
- [PRODUCTION_GUIDE.md](../deployment/PRODUCTION_GUIDE.md) - Production deployment

## 🎉 Summary

**JPAConfigurationStorage advantages**:
- ✅ **100% database-agnostic** (SQLite, PostgreSQL, MySQL, H2)
- ✅ **Clean code** (403 lines, well-structured)
- ✅ **Zero SQL maintenance** (Hibernate handles everything)
- ✅ **Automatic transaction management** via JPAUtil
- ✅ **Thread-safe** by design
- ✅ **Type-safe** JPQL queries
- ✅ **Modern** JPA/Hibernate approach
- ✅ **Comprehensive audit trail** for compliance

Use `JPAConfigurationStorage` for all configuration storage needs! 🚀
