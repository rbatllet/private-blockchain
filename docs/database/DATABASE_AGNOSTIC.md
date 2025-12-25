# Database-Agnostic Configuration Guide

## 📊 Overview

The PrivateBlockchain project supports multiple database backends through a unified configuration system. You can easily switch between H2 (default), SQLite, PostgreSQL, and MySQL without changing your code.

**Important Change (v1.0.5+)**: H2 with persistent file storage is now the default database, replacing SQLite. This provides better concurrency and performance while maintaining data persistence.

## 🎯 Supported Databases

| Database | Use Case | Pool Size | Concurrent Writers | Persistence |
|----------|----------|-----------|-------------------|-------------|
| **H2** (file) | **Default**, Development, Demos | 5-10 | Multiple | ✅ Persistent file (`./blockchain.mv.db`) |
| **H2** (memory) | Unit Testing, CI/CD | 5-10 | Multiple | ❌ In-memory (auto-cleanup) |
| **SQLite** | Legacy, Single-user apps | 2-5 | 1 (single writer) | ✅ Persistent file (`blockchain.db`) |
| **PostgreSQL** | Production, High Concurrency | 10-60 | Multiple | ✅ Server-based |
| **MySQL** | Production, Alternative | 10-50 | Multiple | ✅ Server-based |

## 🚀 Quick Start

### Option 1: Default H2 Persistent (No Configuration Needed)

```java
// H2 persistent file storage is the default - no configuration required
Blockchain blockchain = new Blockchain();
// Works out of the box! Data saved to ./blockchain.mv.db
```

### Option 2: Use H2 In-Memory for Tests

```java
import com.rbatllet.blockchain.config.DatabaseConfig;
import com.rbatllet.blockchain.util.JPAUtil;

// Switch to H2 in-memory for isolated, fast tests
DatabaseConfig h2TestConfig = DatabaseConfig.createH2TestConfig();
JPAUtil.initialize(h2TestConfig);

// Fast, isolated tests with automatic cleanup (data NOT persisted)
Blockchain blockchain = new Blockchain();
```

### Option 3: Use SQLite for Legacy Compatibility

```java
// Switch to SQLite (legacy default before v1.0.5)
DatabaseConfig sqliteConfig = DatabaseConfig.createSQLiteConfig();
JPAUtil.initialize(sqliteConfig);

// Works with single writer, data saved to blockchain.db
Blockchain blockchain = new Blockchain();
```

### Option 4: Switch to PostgreSQL for Production

```java
import com.rbatllet.blockchain.config.DatabaseConfig;
import com.rbatllet.blockchain.util.JPAUtil;

// Initialize PostgreSQL
DatabaseConfig pgConfig = DatabaseConfig.createPostgreSQLConfig(
    "prod-db.example.com",  // host
    "blockchain_prod",       // database name
    "blockchain_user",       // username
    "secure_password"        // password
);

JPAUtil.initialize(pgConfig);

// Now all blockchain operations use PostgreSQL
Blockchain blockchain = new Blockchain();
```

### Option 5: Environment-Based Configuration

```bash
# Set environment variables
export DB_TYPE=postgresql
export DB_HOST=prod-db.example.com
export DB_NAME=blockchain_prod
export DB_USER=blockchain_user
export DB_PASSWORD=secure_password
```

```java
// Load configuration from environment
DatabaseConfig envConfig = DatabaseConfig.createProductionConfigFromEnv();
JPAUtil.initialize(envConfig);
```

## 📦 Architecture

```
┌─────────────────────────────────────────────┐
│  Your Application Code                      │
│  (No changes needed!)                       │
└─────────────────┬───────────────────────────┘
                  │
┌─────────────────▼───────────────────────────┐
│  DatabaseConfig.java                        │
│  (Configuration Layer)                      │
│  - SQLite, PostgreSQL, MySQL, H2            │
│  - Builder pattern like EncryptionConfig    │
└─────────────────┬───────────────────────────┘
                  │
┌─────────────────▼───────────────────────────┐
│  JPAUtil.java                               │
│  (Persistence Layer)                        │
│  - Dynamic database selection               │
│  - Connection pool management               │
└─────────────────┬───────────────────────────┘
                  │
┌─────────────────▼───────────────────────────┐
│  persistence.xml                            │
│  (5 Persistence Units)                      │
│  - blockchainPU (default SQLite)            │
│  - blockchainPU-sqlite                      │
│  - blockchainPU-postgresql                  │
│  - blockchainPU-mysql                       │
│  - blockchainPU-h2                          │
└─────────────────┬───────────────────────────┘
                  │
┌─────────────────▼───────────────────────────┐
│  Your Database                              │
│  (SQLite / PostgreSQL / MySQL / H2)         │
└─────────────────────────────────────────────┘
```

## 💡 Advanced Usage

### Custom Configuration

```java
// Build custom configuration
DatabaseConfig customConfig = DatabaseConfig.builder()
    .databaseType(DatabaseConfig.DatabaseType.POSTGRESQL)
    .databaseUrl("jdbc:postgresql://custom-host:5432/custom_db")
    .username("custom_user")
    .password("custom_password")
    .poolMinSize(20)
    .poolMaxSize(100)
    .connectionTimeout(30000)
    .showSql(true)          // Enable SQL logging
    .formatSql(true)        // Pretty-print SQL
    .hbm2ddlAuto("update")  // Schema management
    .build();

JPAUtil.initialize(customConfig);
```

### Development Configuration with Logging

```java
// Use predefined development config with verbose logging
DatabaseConfig devConfig = DatabaseConfig.createDevelopmentConfig();
JPAUtil.initialize(devConfig);

// Output:
// - Show SQL: ✅ Enabled
// - Format SQL: ✅ Enabled
// - Highlight SQL: ✅ Enabled
// - Statistics: ✅ Enabled
```

### Configuration Summary

```java
DatabaseConfig config = JPAUtil.getCurrentConfig();
System.out.println(config.getSummary());

// Output:
// 📊 Database Configuration Summary:
//    Type: POSTGRESQL
//    Description: PostgreSQL (recommended for production with high concurrency)
//    URL: jdbc:postgresql://localhost:5432/blockchain
//    Username: blockchain_user
//    Pool Size: 10 - 60
//    Pool Name: BlockchainPostgreSQLPool
//    Connection Timeout: 30000 ms
//    Schema Management: update
//    Show SQL: ❌ Disabled
//    Format SQL: ❌ Disabled
//    Statistics: ❌ Disabled
```

### Detecting Database Type at Runtime

You can programmatically detect which database is being used and whether it's in-memory or persistent:

```java
DatabaseConfig config = JPAUtil.getCurrentConfig();

if (config != null) {
    // Get database type
    DatabaseConfig.DatabaseType type = config.getDatabaseType();
    String url = config.getDatabaseUrl();

    // Detect H2 mode (memory vs file)
    boolean isH2InMemory = type == DatabaseConfig.DatabaseType.H2 && url.contains(":mem:");
    boolean isH2Persistent = type == DatabaseConfig.DatabaseType.H2 && url.contains(":file:");

    // Branch logic based on database
    switch (type) {
        case H2:
            if (isH2InMemory) {
                System.out.println("🧪 Running with H2 in-memory (tests)");
                // Fast, isolated tests - data not persisted
            } else {
                System.out.println("💾 Running with H2 persistent file (default/demos)");
                // Data persisted to ./blockchain.mv.db
            }
            break;

        case SQLITE:
            System.out.println("📁 Running with SQLite (legacy)");
            // Single writer, data persisted to blockchain.db
            break;

        case POSTGRESQL:
            System.out.println("🐘 Running with PostgreSQL (production)");
            // Multiple writers, server-based
            break;

        case MYSQL:
            System.out.println("🐬 Running with MySQL (production)");
            // Multiple writers, server-based
            break;
    }

    // You can also check specific URL patterns
    if (url.contains(":mem:")) {
        System.out.println("⚠️  In-memory database - data will be lost on shutdown");
    } else if (url.contains(":file:") || url.startsWith("jdbc:sqlite:")) {
        System.out.println("✅ File-based database - data persisted");
    } else {
        System.out.println("🌐 Server-based database");
    }
}
```

### Factory Methods Reference

All available database configuration methods:

```java
// H2 Configurations
DatabaseConfig.createH2Config()                    // DEFAULT: H2 persistent file (./blockchain.mv.db)
DatabaseConfig.createH2TestConfig()                // H2 in-memory (tests, auto-cleanup)
DatabaseConfig.createH2FileConfig("/custom/path")  // H2 with custom file path

// SQLite Configuration
DatabaseConfig.createSQLiteConfig()                // SQLite persistent file (blockchain.db)
DatabaseConfig.createDevelopmentConfig()           // SQLite with verbose logging

// PostgreSQL Configurations
DatabaseConfig.createPostgreSQLConfig(host, db, user, pass)
DatabaseConfig.createPostgreSQLConfig(host, port, db, user, pass)

// MySQL Configurations
DatabaseConfig.createMySQLConfig(host, db, user, pass)
DatabaseConfig.createMySQLConfig(host, port, db, user, pass)

// Environment-based
DatabaseConfig.createProductionConfigFromEnv()     // Load from env vars

// Custom JDBC URL
DatabaseConfig.forDatabaseUrl(type, jdbcUrl)
DatabaseConfig.forDatabaseUrl(type, jdbcUrl, user, pass)
```

## 🔧 Database Setup Instructions

### PostgreSQL Setup

```bash
# Install PostgreSQL
brew install postgresql  # macOS
sudo apt install postgresql  # Ubuntu

# Create database and user
createdb blockchain_prod
psql -c "CREATE USER blockchain_user WITH PASSWORD 'secure_password';"
psql -c "GRANT ALL PRIVILEGES ON DATABASE blockchain_prod TO blockchain_user;"
```

### MySQL Setup

```bash
# Install MySQL
brew install mysql  # macOS
sudo apt install mysql-server  # Ubuntu

# Create database and user
mysql -u root -p <<EOF
CREATE DATABASE blockchain_prod;
CREATE USER 'blockchain_user'@'localhost' IDENTIFIED BY 'secure_password';
GRANT ALL PRIVILEGES ON blockchain_prod.* TO 'blockchain_user'@'localhost';
FLUSH PRIVILEGES;
EOF
```

### H2 Setup

```java
// No setup needed - H2 is embedded and ready to use!

// Option 1: Default H2 persistent (no code needed)
Blockchain blockchain = new Blockchain();
// Data saved to ./blockchain.mv.db

// Option 2: H2 in-memory for tests
DatabaseConfig h2TestConfig = DatabaseConfig.createH2TestConfig();
JPAUtil.initialize(h2TestConfig);
// Data NOT persisted, auto-cleanup on shutdown

// Option 3: H2 with custom file path
DatabaseConfig h2CustomConfig = DatabaseConfig.createH2FileConfig("/custom/path/mydb");
JPAUtil.initialize(h2CustomConfig);
// Data saved to /custom/path/mydb.mv.db
```

## ⚠️ Important Notes

### H2 Advantages (Default since v1.0.5)

- ✅ **Multiple concurrent writers** (better than SQLite)
- ✅ **Embedded** - no separate server needed
- ✅ **Fast** - excellent performance for development and demos
- ✅ **Two modes**: Persistent file (default) and in-memory (tests)
- ✅ **PostgreSQL compatibility mode** - easy migration to production
- ✅ **ACID transactions**

### H2 vs SQLite Comparison

| Feature | H2 (Default) | SQLite |
|---------|--------------|--------|
| **Concurrent Writers** | ✅ Multiple | ⚠️  Single |
| **Connection Pool** | 5-10 | 2-5 (max) |
| **Performance** | Fast | Fast |
| **Memory Mode** | ✅ Yes | ❌ No |
| **PostgreSQL Mode** | ✅ Yes | ❌ No |
| **Use Case** | Development, Demos, Tests | Legacy, Single-user |

### SQLite Limitations (Legacy)

- ✅ **Perfect for**: Single-user applications, legacy compatibility
- ⚠️ **Limitations**:
  - Single writer at a time (WAL mode helps with concurrent reads)
  - Connection pool: max 5 connections (2-5 recommended)
  - No true clustering or sharding support
- 💡 **Recommendation**: Use H2 (default) for better concurrency

### PostgreSQL Advantages (Production)

- ✅ **Multiple concurrent writers** (60+ connections)
- ✅ **True ACID transactions**
- ✅ **Clustering support** (with Citus extension for sharding)
- ✅ **Production-grade reliability**
- ✅ **Excellent performance at scale**

### Migration Path

1. **Development**: Start with H2 persistent (default, no config needed)
2. **Testing**: Use H2 in-memory (`createH2TestConfig()`) for fast, isolated tests
3. **Staging**: Test with PostgreSQL (`createPostgreSQLConfig()`)
4. **Production**: Deploy with PostgreSQL

### Backwards Compatibility

If you need to maintain compatibility with the old SQLite default (pre-v1.0.5):

```java
// Explicitly use SQLite (legacy behavior)
DatabaseConfig sqliteConfig = DatabaseConfig.createSQLiteConfig();
JPAUtil.initialize(sqliteConfig);
```

## 🧪 Testing with Different Databases

```java
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

public class MultiDatabaseTest {

    @BeforeEach
    void setupH2() {
        // Use H2 for isolated, fast tests
        DatabaseConfig h2Config = DatabaseConfig.createH2TestConfig();
        JPAUtil.initialize(h2Config);
    }

    @Test
    void testBlockchainWithH2() {
        Blockchain blockchain = new Blockchain();
        // Test with H2 in-memory database
        // Tables created automatically, cleaned up after test
    }
}
```

## 🔒 Security Best Practices

### 1. Never Hardcode Credentials

```java
// ❌ BAD: Hardcoded credentials
DatabaseConfig badConfig = DatabaseConfig.createPostgreSQLConfig(
    "localhost", "blockchain", "user", "password123"
);

// ✅ GOOD: Load from environment
DatabaseConfig goodConfig = DatabaseConfig.createProductionConfigFromEnv();
```

### 2. Use Environment Variables

```bash
# .env file (never commit to git!)
DB_TYPE=postgresql
DB_HOST=secure-db.example.com
DB_NAME=blockchain_prod
DB_USER=blockchain_user
DB_PASSWORD=your_secure_password_here
```

### 3. Different Credentials per Environment

```bash
# Development
export DB_PASSWORD=dev_password

# Staging
export DB_PASSWORD=staging_password

# Production
export DB_PASSWORD=$(vault read -field=password secret/blockchain/db)
```

### 4. SSL/TLS Encryption (NEW in v1.0.6+)

**PostgreSQL** - SSL with certificate verification by default:

```java
// Default configuration (v1.0.6+: verifies server certificate)
DatabaseConfig pgConfig = DatabaseConfig.createPostgreSQLConfig(
    "db.example.com", "blockchain", "user", "pass"
);
// Connection string: jdbc:postgresql://db.example.com:5432/blockchain?ssl=true&sslmode=verify-full

// ✅ SECURITY: sslmode=verify-full encrypts traffic AND verifies server certificate
// REQUIRES: Valid CA certificates in system truststore or custom sslrootcert parameter
// PROTECTION: Prevents MITM attacks by verifying server identity

// For self-signed certificates (development only):
DatabaseConfig devConfig = DatabaseConfig.builder()
    .databaseType(DatabaseConfig.DatabaseType.POSTGRESQL)
    .databaseUrl("jdbc:postgresql://db.example.com:5432/blockchain?ssl=true&sslmode=verify-full&sslrootcert=/path/to/ca.pem")
    .username("user")
    .password("pass")
    .build();
// Add &sslrootcert=/path/to/ca.pem to specify custom CA for self-signed certificates
```

**MySQL** - SSL with certificate verification by default:

```java
// Default configuration (v1.0.6+: verifies server certificate)
DatabaseConfig mysqlConfig = DatabaseConfig.createMySQLConfig(
    "db.example.com", "blockchain", "user", "pass"
);
// Connection string: jdbc:mysql://db.example.com:3306/blockchain?useSSL=true&requireSSL=true&verifyServerCertificate=true

// ✅ SECURITY: verifyServerCertificate=true encrypts traffic AND verifies server certificate
// REQUIRES: Valid CA certificates in system truststore or custom trustCertificateKeyStoreUrl
// PROTECTION: Prevents MITM attacks by verifying server identity

// For self-signed certificates (development only):
DatabaseConfig mysqlSelfSigned = DatabaseConfig.builder()
    .databaseType(DatabaseConfig.DatabaseType.MYSQL)
    .databaseUrl("jdbc:mysql://db.example.com:3306/blockchain?useSSL=true&requireSSL=true&verifyServerCertificate=false&trustCertificateKeyStoreUrl=file:/path/to/truststore.jks&trustCertificateKeyStorePassword=password")
    .username("user")
    .password("pass")
    .build();
// Use custom truststore for self-signed certificates (set verifyServerCertificate=false or configure truststore)
```

**SSL/TLS Security Levels:**

| Database | Default Mode (v1.0.6+) | Security | Notes |
|----------|----------------------|----------|-------|
| PostgreSQL | `sslmode=verify-full` | ✅ Encrypts + verifies cert | Production-ready, MITM protection enabled |
| MySQL | `verifyServerCertificate=true` | ✅ Encrypts + verifies cert | Production-ready, MITM protection enabled |
| SQLite | N/A (local file) | ✅ No network traffic | Development only |
| H2 | N/A (embedded/local) | ✅ No network traffic | Testing only |

**Production Checklist:**
- ✅ DatabaseConfig defaults (v1.0.6+) are production-ready with certificate verification
- ✅ Ensure database server has valid CA-signed certificates in system truststore
- ✅ For self-signed certs: Use `sslrootcert` (PostgreSQL) or custom truststore (MySQL)
- ⚠️ Never disable certificate verification in production environments

## 📊 Performance Tuning

### SQLite Optimization

```java
DatabaseConfig sqliteConfig = DatabaseConfig.builder()
    .databaseType(DatabaseConfig.DatabaseType.SQLITE)
    .databaseUrl("jdbc:sqlite:blockchain.db?journal_mode=WAL&synchronous=NORMAL&cache_size=10000")
    .poolMinSize(2)
    .poolMaxSize(5)  // SQLite limitation: single writer
    .build();
```

### PostgreSQL Optimization

```java
DatabaseConfig pgConfig = DatabaseConfig.builder()
    .databaseType(DatabaseConfig.DatabaseType.POSTGRESQL)
    .databaseUrl("jdbc:postgresql://localhost:5432/blockchain")
    .poolMinSize(10)
    .poolMaxSize(60)  // Scale based on your workload
    .connectionTimeout(30000)
    .idleTimeout(600000)
    .maxLifetime(1800000)
    .build();
```

## 🎯 Demos

### Database Configuration Demo

Run the comprehensive demo to see all database configurations in action:

```bash
./scripts/run_database_config_demo.zsh
```

The demo shows:
1. ✅ Default SQLite configuration
2. ✅ H2 in-memory configuration
3. ✅ Configuration summaries for all databases
4. ✅ Environment-based production configuration
5. ✅ Blockchain operations with each database

### JPA Configuration Storage Demo

Run the JPA configuration storage demo to see database-agnostic configuration management:

```bash
./scripts/run_jpa_configuration_storage_demo.zsh
```

The demo shows:
1. ✅ Configuration storage with SQLite
2. ✅ Configuration storage with H2 (same code!)
3. ✅ Advanced configuration operations (set, get, delete, audit log)
4. ✅ Zero code changes between databases

## 🔗 Related Documentation

- [API Guide](../reference/API_GUIDE.md) - Complete API reference
- [CONFIGURATION_STORAGE_GUIDE.md](CONFIGURATION_STORAGE_GUIDE.md) - JPAConfigurationStorage guide
- [PRODUCTION_GUIDE.md](../deployment/PRODUCTION_GUIDE.md) - Production deployment guide
- [TESTING.md](../testing/TESTING.md) - Testing strategies

## ❓ Troubleshooting

### Issue: "Cannot connect to PostgreSQL"

**Solution**: Check if PostgreSQL is running and credentials are correct

```bash
# Check PostgreSQL status
pg_isready

# Test connection
psql -h localhost -U blockchain_user -d blockchain_prod
```

### Issue: "Pool exhausted" with SQLite

**Solution**: SQLite has single writer limitation. Either:
1. Reduce concurrent writes
2. Switch to PostgreSQL for high concurrency

### Issue: "Schema not up to date"

**Solution**: Check `hbm2ddl.auto` setting

```java
// Auto-update schema (development/staging)
DatabaseConfig config = DatabaseConfig.createPostgreSQLConfig(
    "localhost", "blockchain", "user", "pass"
);
config.setHbm2ddlAuto("update");  // Updates schema automatically
JPAUtil.initialize(config);

// Validate only (production)
DatabaseConfig prodConfig = DatabaseConfig.createPostgreSQLConfig(
    "prod-db", "blockchain", "user", "pass"
);
prodConfig.setHbm2ddlAuto("validate");  // Fails if schema doesn't match
JPAUtil.initialize(prodConfig);
```

## 🎉 Summary

- ✅ **H2 is now the default** (v1.0.5+) - better concurrency than SQLite
- ✅ **Zero code changes** required to switch databases
- ✅ **Type-safe configuration** with enums and builder pattern
- ✅ **Production-ready** with PostgreSQL support
- ✅ **Fast testing** with H2 in-memory database
- ✅ **Environment-based** configuration for different deployments
- ✅ **Backwards compatible** with existing SQLite code
- ✅ **Runtime detection** of database type and persistence mode

**New in v1.0.5**:
- 🆕 H2 persistent file storage is now the default (replaces SQLite)
- 🆕 Better concurrent write support out of the box
- 🆕 `DatabaseConfig.createH2Config()` - default H2 persistent configuration
- 🆕 Enhanced database detection API for runtime introspection

**Switch databases with confidence!** 🚀
