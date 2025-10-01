# Database-Agnostic Configuration Guide

## 📊 Overview

The PrivateBlockchain project now supports multiple database backends through a unified configuration system. You can easily switch between SQLite (development), H2 (testing), PostgreSQL (production), and MySQL without changing your code.

## 🎯 Supported Databases

| Database | Use Case | Pool Size | Concurrent Writers |
|----------|----------|-----------|-------------------|
| **SQLite** | Development, Demos | 2-5 | 1 (single writer) |
| **PostgreSQL** | Production, High Concurrency | 10-60 | Multiple |
| **MySQL** | Production, Alternative | 10-50 | Multiple |
| **H2** | Testing, CI/CD | 5-10 | Multiple (in-memory) |

## 🚀 Quick Start

### Option 1: Default SQLite (No Configuration Needed)

```java
// SQLite is the default - no configuration required
Blockchain blockchain = new Blockchain();
// Works out of the box!
```

### Option 2: Switch to PostgreSQL for Production

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

### Option 3: H2 for Fast Testing

```java
// Switch to H2 in-memory for testing
DatabaseConfig h2Config = DatabaseConfig.createH2TestConfig();
JPAUtil.initialize(h2Config);

// Fast, isolated tests with automatic cleanup
Blockchain blockchain = new Blockchain();
```

### Option 4: Environment-Based Configuration

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
// No setup needed - H2 is embedded and in-memory
DatabaseConfig h2Config = DatabaseConfig.createH2TestConfig();
JPAUtil.initialize(h2Config);
// Ready to use!
```

## ⚠️ Important Notes

### SQLite Limitations

- ✅ **Perfect for**: Development, demos, single-user applications
- ⚠️ **Limitations**:
  - Single writer at a time (WAL mode helps with concurrent reads)
  - Connection pool: max 5 connections (2-5 recommended)
  - No true clustering or sharding support
- 💡 **For production with high write concurrency**: Use PostgreSQL

### PostgreSQL Advantages

- ✅ **Multiple concurrent writers** (60+ connections)
- ✅ **True ACID transactions**
- ✅ **Clustering support** (with Citus extension for sharding)
- ✅ **Production-grade reliability**
- ✅ **Excellent performance at scale**

### Migration Path

1. **Development**: Start with SQLite (default)
2. **Testing**: Use H2 for fast, isolated tests
3. **Staging**: Test with PostgreSQL
4. **Production**: Deploy with PostgreSQL

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

- [API Guide](API_GUIDE.md) - Complete API reference
- [CONFIGURATION_STORAGE_GUIDE.md](CONFIGURATION_STORAGE_GUIDE.md) - JPAConfigurationStorage guide
- [CLAUDE.md](../CLAUDE.md) - Project overview and architecture
- [PRODUCTION_GUIDE.md](PRODUCTION_GUIDE.md) - Production deployment guide
- [TESTING.md](TESTING.md) - Testing strategies

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

- ✅ **Zero code changes** required to switch databases
- ✅ **Type-safe configuration** with enums and builder pattern
- ✅ **Production-ready** with PostgreSQL support
- ✅ **Fast testing** with H2 in-memory database
- ✅ **Environment-based** configuration for different deployments
- ✅ **Backwards compatible** with existing SQLite code

**Switch databases with confidence!** 🚀
