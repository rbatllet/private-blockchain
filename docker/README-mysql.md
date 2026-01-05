# MySQL 8.0 with SSL/TLS - Docker Setup

## 📋 Overview

This configuration provides a MySQL 8.0 server with SSL/TLS enabled, ideal for development and testing of the Private Blockchain project.

## 🏗️ Architecture

```
docker/
├── .env                         # Environment variables (passwords, config)
├── .env.example                 # Template for environment variables
├── .gitignore                   # Git ignore rules (excludes .env and certs)
│
├── README.md                    # General overview
├── README-mysql.md              # MySQL 8.0 documentation (this file)
│
├── docker-compose-mysql.yml      # Docker Compose configuration for MySQL
├── start-mysql.zsh               # MySQL quick start script
├── test-mysql-ssl-connection.zsh # MySQL SSL connection test script
│
└── mysql/                       # MySQL-specific files
    ├── generate-certs.zsh       # SSL certificates generator
    ├── config/
    │   └── my.cnf               # MySQL custom configuration
    └── certs/                   # SSL certificates (generated)
        ├── ca.pem               # CA Certificate
        ├── ca-key.pem           # CA Private Key (keep secure!)
        ├── server-cert.pem      # Server Certificate
        ├── server-key.pem       # Server Private Key (keep secure!)
        ├── client-cert.pem      # Client Certificate (optional)
        └── client-key.pem       # Client Private Key (optional)
```

## 🚀 Quick Start

### Option A: Automated Setup (Recommended)

```bash
cd docker
./start-mysql.zsh
```

This script will:
1. Generate SSL certificates
2. Start MySQL container
3. Display connection information

### Option B: Manual Setup

#### 1. Generate SSL certificates

```bash
cd docker/mysql
./generate-certs.zsh
```

This will generate RSA 3072-bit self-signed certificates valid for 2 years (NIST-compliant).

#### 2. Start MySQL with SSL

```bash
cd docker
docker-compose -f docker-compose-mysql.yml up -d
```

#### 3. Verify SSL is enabled

```bash
# Connect to MySQL (password from .env file: MYSQL_ROOT_PASSWORD)
docker exec -it mysql-blockchain-ssl mysql -u root -p

# Inside MySQL, run:
SHOW VARIABLES LIKE '%ssl%';
```

You should see:
```
+---------------+--------------------------+
| Variable_name | Value                    |
+---------------+--------------------------+
| have_ssl      | YES                      |
| ssl_ca        | /etc/mysql/certs/ca.pem  |
| ssl_cert      | /etc/mysql/certs/...     |
| ssl_key       | /etc/mysql/certs/...     |
+---------------+--------------------------+
```

## 🔗 Connect from Java Application

### Option A: Using `DatabaseConfig.createMySQLConfig()` (v1.0.6+)

```java
// In your Java application (default: verifyServerCertificate=true)
DatabaseConfig mysqlConfig = DatabaseConfig.createMySQLConfig(
    "localhost",           // host
    3306,                   // port
    "blockchain_prod",      // database name
    "blockchain_user",      // username
    "SecurePassword123!"    // password
);
// Uses: jdbc:mysql://localhost:3306/blockchain_prod?useSSL=true&requireSSL=true&verifyServerCertificate=true&allowPublicKeyRetrieval=true
```

### Option B: Manual configuration with SSL

```java
// Production-ready with certificate verification (v1.0.6+)
String jdbcUrl = "jdbc:mysql://localhost:3306/blockchain_prod" +
    "?useSSL=true" +                           // Enable SSL
    "&requireSSL=true" +                        // Require SSL
    "&verifyServerCertificate=true" +          // Verify server certificate (DEFAULT v1.0.6+)
    "&allowPublicKeyRetrieval=true";           // For MySQL 8+

JPAUtil.initialize(
    DatabaseConfig.builder()
        .databaseType(DatabaseType.MYSQL)
        .databaseUrl(jdbcUrl)
        .username("blockchain_user")
        .password("SecurePassword123!")
        .build()
);
```

### 📋 JDBC SSL Configuration according to Hibernate ORM

According to the official Hibernate documentation, the correct JDBC configuration for SSL with MySQL is:

#### Standard JPA Properties (Hibernate v1.0.6+):
```properties
jakarta.persistence.jdbc.url=jdbc:mysql://localhost:3306/blockchain_prod?useSSL=true&requireSSL=true&verifyServerCertificate=true&allowPublicKeyRetrieval=true
jakarta.persistence.jdbc.user=blockchain_user
jakarta.persistence.jdbc.password=SecurePassword123!
```

#### HikariCP Specific Configuration (v1.0.6+):
```properties
hibernate.hikari.jdbcUrl=jdbc:mysql://localhost:3306/blockchain_prod?useSSL=true&requireSSL=true&verifyServerCertificate=true&allowPublicKeyRetrieval=true
hibernate.hikari.username=blockchain_user
hibernate.hikari.password=SecurePassword123!
```

#### JDBC Parameters Explained:

| Parameter | Description |
|-----------|-------------|
| `useSSL=true` | Enables SSL/TLS for the connection |
| `requireSSL=true` | Requires SSL connection (fails if server doesn't support SSL) |
| `verifyServerCertificate=true` | Verifies certificate with CA (v1.0.6+ DEFAULT, production-ready) |
| `allowPublicKeyRetrieval=true` | Allows public key retrieval (MySQL 8+) |
| `trustServerCertificate=true` | Trusts self-signed certificates (⚠️ development only, disables verification) |

#### Complete JDBC URLs:

**Production (v1.0.6+ default - verifies CA):**
```
jdbc:mysql://localhost:3306/blockchain_prod?useSSL=true&requireSSL=true&verifyServerCertificate=true&allowPublicKeyRetrieval=true
```

**Development with self-signed certs (disable verification):**
```
jdbc:mysql://localhost:3306/blockchain_prod?useSSL=true&requireSSL=true&verifyServerCertificate=false&trustCertificateKeyStoreUrl=file:/path/to/truststore.jks&allowPublicKeyRetrieval=true
```

**✅ Security Note:** v1.0.6+ defaults to `verifyServerCertificate=true` for production-ready security.

## 🛠️ Docker Commands

### Start services
```bash
docker-compose -f docker-compose-mysql.yml up -d
```

### View logs
```bash
docker-compose -f docker-compose-mysql.yml logs -f mysql
```

### Stop services
```bash
docker-compose -f docker-compose-mysql.yml down
```

### Stop and remove volumes (⚠️ data will be lost!)
```bash
docker-compose -f docker-compose-mysql.yml down -v
```

### Restart MySQL
```bash
docker-compose -f docker-compose-mysql.yml restart mysql
```

### Enter MySQL container
```bash
docker exec -it mysql-blockchain-ssl bash
```

### Connect directly to MySQL
```bash
docker exec -it mysql-blockchain-ssl mysql -u root -pRootPassword123!
```

## 💡 Interactive Database Operations

### Connect as different users

```bash
# Connect as root user (password from .env: MYSQL_ROOT_PASSWORD)
docker exec -it mysql-blockchain-ssl mysql -u root -p

# Connect as application user (password from .env: MYSQL_PASSWORD)
docker exec -it mysql-blockchain-ssl mysql -u blockchain_user -p blockchain_prod
```

### Common MySQL commands

```sql
-- Once connected to MySQL, you can run:

-- Show all databases
SHOW DATABASES;

-- Select database to use
USE blockchain_prod;

-- Show all tables in current database
SHOW TABLES;

-- Describe table structure
DESCRIBE table_name;
-- or
SHOW COLUMNS FROM table_name;

-- Show table indexes
SHOW INDEX FROM table_name;

-- Show table creation details
SHOW CREATE TABLE table_name;

-- Count rows in a table
SELECT COUNT(*) FROM table_name;

-- Show first 10 rows
SELECT * FROM table_name LIMIT 10;

-- Show MySQL variables
SHOW VARIABLES;

-- Show SSL status
SHOW VARIABLES LIKE '%ssl%';

-- Show current connections
SHOW PROCESSLIST;

-- Show table sizes
SELECT
    table_name AS 'Table',
    ROUND(((data_length + index_length) / 1024 / 1024), 2) AS 'Size (MB)'
FROM information_schema.TABLES
WHERE table_schema = 'blockchain_prod'
ORDER BY (data_length + index_length) DESC;
```

### Quick queries from command line

```bash
# Execute single query
docker exec -it mysql-blockchain-ssl mysql -u blockchain_user -pSecurePassword123! blockchain_prod -e "SHOW TABLES;"

# Execute query and output as CSV
docker exec -it mysql-blockchain-ssl mysql -u blockchain_user -pSecurePassword123! blockchain_prod -e "SELECT * FROM blocks LIMIT 5;" --batch --raw > output.csv

# Count rows without entering MySQL
docker exec -it mysql-blockchain-ssl mysql -u blockchain_user -pSecurePassword123! blockchain_prod -e "SELECT COUNT(*) FROM blocks;"
```

### Database backup and restore

```bash
# Backup entire database
docker exec -it mysql-blockchain-ssl mysqldump -u blockchain_user -pSecurePassword123! blockchain_prod > backup.sql

# Backup specific tables
docker exec -it mysql-blockchain-ssl mysqldump -u blockchain_user -pSecurePassword123! blockchain_prod blocks offchain_data > backup_tables.sql

# Restore database
docker exec -i mysql-blockchain-ssl mysql -u blockchain_user -pSecurePassword123! blockchain_prod < backup.sql

# Backup to file inside container (useful for large databases)
docker exec mysql-blockchain-ssl mysqldump -u blockchain_user -pSecurePassword123! blockchain_prod > /var/lib/mysql/backup.sql
docker exec mysql-blockchain-ssl ls -lh /var/lib/mysql/backup.sql
```

### Database operations

```bash
# Create a new database
docker exec -it mysql-blockchain-ssl mysql -u root -pRootPassword123! -e "CREATE DATABASE test_db;"

# Drop a database (⚠️ careful!)
docker exec -it mysql-blockchain-ssl mysql -u root -pRootPassword123! -e "DROP DATABASE test_db;"

# Create a new user
docker exec -it mysql-blockchain-ssl mysql -u root -pRootPassword123! -e "CREATE USER 'new_user'@'%' IDENTIFIED BY 'Password123!';"

# Grant privileges
docker exec -it mysql-blockchain-ssl mysql -u root -pRootPassword123! -e "GRANT ALL PRIVILEGES ON blockchain_prod.* TO 'new_user'@'%'; FLUSH PRIVILEGES;"

# Show users
docker exec -it mysql-blockchain-ssl mysql -u root -pRootPassword123! -e "SELECT user, host FROM mysql.user;"
```

### Performance and monitoring

```sql
-- Show engine status
SHOW ENGINE INNODB STATUS;

-- Show InnoDB metrics
SELECT * FROM sys.metrics;

-- Show connection statistics
SHOW STATUS LIKE 'Connections';

-- Show query cache statistics
SHOW STATUS LIKE 'Qcache%';

-- Show slow queries
SELECT * FROM mysql.slow_log ORDER BY start_time DESC LIMIT 10;

-- Show table locks
SHOW OPEN TABLES WHERE In_use > 0;
```

### MySQL shell alternatives

```bash
# Using mysql command with pager (for long output)
docker exec -it mysql-blockchain-ssl mysql -u blockchain_user -pSecurePassword123! blockchain_prod --pager

# Vertical output (useful for wide tables)
docker exec -it mysql-blockchain-ssl mysql -u blockchain_user -pSecurePassword123! blockchain_prod -e "SELECT * FROM blocks\G"

# Execute SQL from file
docker exec -i mysql-blockchain-ssl mysql -u blockchain_user -pSecurePassword123! blockchain_prod < query.sql

# Export query result to CSV
docker exec -it mysql-blockchain-ssl mysql -u blockchain_user -pSecurePassword123! blockchain_prod -e "SELECT * FROM blocks INTO OUTFILE '/var/lib/mysql/result.csv' FIELDS TERMINATED BY ',' ENCLOSED BY '\"' LINES TERMINATED BY '\n';"

# Copy CSV from container
docker cp mysql-blockchain-ssl:/var/lib/mysql/result.csv ./result.csv
```

## 📊 phpMyAdmin (Optional)

For graphical database administration:

```bash
# Start with phpMyAdmin
docker-compose -f docker-compose-mysql.yml --profile tools up -d

# Access at: http://localhost:8080
# User: root
# Password: (value from .env file: MYSQL_ROOT_PASSWORD)
```

## 🔒 SSL Certificate Details

### Self-Signed Certificates

The generated certificates are self-signed and are useful for:
- ✅ Local development
- ✅ Testing environments
- ✅ Continuous integration (CI/CD)

For **production**, use certificates from a trusted CA such as:
- Let's Encrypt (free)
- DigiCert
- Comodo
- Other commercial providers

### Regenerate certificates

```bash
cd docker/mysql
rm -rf certs/*
./generate-certs.zsh
docker-compose -f docker-compose-mysql.yml restart mysql
```

## 🐛 Troubleshooting

### Error: "SSL connection error" or "certificate verification failed"

**Problem:** The Java client doesn't trust the self-signed certificate (v1.0.6+ uses `verifyServerCertificate=true` by default).

**Solution 1 (Recommended for development):** Configure custom truststore:

```java
"?useSSL=true&requireSSL=true&verifyServerCertificate=false&trustCertificateKeyStoreUrl=file:/path/to/docker/mysql/certs/truststore.jks"
```

**Solution 2 (Development only - NOT recommended):** Temporarily disable verification:

```java
"?useSSL=true&requireSSL=true&verifyServerCertificate=false"  // ⚠️ Vulnerable to MITM attacks
```

For production:
- ✅ Add the CA certificate to Java's truststore (recommended)
- ✅ Use certificates from a trusted CA (Let's Encrypt, DigiCert, etc.)

### Error: "Access denied for user"

**Problem:** Username or password is incorrect.

**Solution:** Verify credentials in the `.env` file:

```bash
# Check current credentials
docker exec -it mysql-blockchain-ssl mysql -u blockchain_user -p
# Password: SecurePassword123!
```

### Error: "Port 3306 already in use"

**Problem:** Another MySQL is already running on port 3306.

**Solution 1:** Stop the other MySQL:
```bash
# Linux
sudo systemctl stop mysql
# or
brew services stop mysql  # macOS
```

**Solution 2:** Change the port in `docker-compose-mysql.yml`:
```yaml
ports:
  - "3307:3306"  # Use 3307 instead of 3306
```

### View generated certificates

```bash
cd docker/mysql/certs
openssl x509 -in ca.pem -text -noout
openssl x509 -in server-cert.pem -text -noout
```

### Verify SSL connection

```bash
# Test with openssl
openssl s_client -connect localhost:3306 -showcerts

# You should see:
# - Certificate chain
# - Subject: CN = mysql-blockchain
# - Issuer: CN = mysql-blockchain-CA
# - SSL-Session: TLS 1.3
```

### Run SSL connection test

```bash
cd docker
./test-mysql-ssl-connection.zsh
```

This script will verify:
1. MySQL container is running
2. SSL connection works
3. SSL configuration is correct
4. Connection with application user works

## 📝 Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `MYSQL_ROOT_PASSWORD` | `RootPassword123!` | Root password (CHANGE IN PROD!) |
| `MYSQL_DATABASE` | `blockchain_prod` | Database name |
| `MYSQL_USER` | `blockchain_user` | Application user |
| `MYSQL_PASSWORD` | `SecurePassword123!` | User password (CHANGE!) |
| `MYSQL_PORT` | `3306` | MySQL exposed port |
| `PHPMYADMIN_PORT` | `8080` | phpMyAdmin port |

## 🔧 Production Considerations

### For production, change:

1. **Passwords**: Use strong and unique passwords
2. **Certificates**: Use certificates from a valid CA (v1.0.6+ uses `verifyServerCertificate=true` by default)
3. **Network**: Use an isolated Docker network or VPN
4. **Backups**: Configure regular database backups
5. **Monitoring**: Enable MySQL monitoring
6. **SSL Mode**: Default `verifyServerCertificate=true` is production-ready (v1.0.6+)

### Production configuration example:

```yaml
# docker-compose-mysql-prod.yml
services:
  mysql:
    # ... other configs ...
    environment:
      MYSQL_ROOT_PASSWORD_FILE: /run/secrets/mysql_root_password
      MYSQL_PASSWORD_FILE: /run/secrets/mysql_password
    secrets:
      - mysql_root_password
      - mysql_password

secrets:
  mysql_root_password:
    file: ./secrets/mysql_root_password.txt
  mysql_password:
    file: ./secrets/mysql_password.txt
```

## 📚 References

- [MySQL Docker Official Image](https://hub.docker.com/_/mysql)
- [MySQL 8.0 SSL Reference](https://dev.mysql.com/doc/refman/8.0/en/using-encrypted-connections.html)
- [Connector/J 8.0 SSL Configuration](https://dev.mysql.com/doc/connector-j/8.0/en/connector-j-reference-type-configuration-connection-properties.html)

## ✅ Verification Checklist

- [ ] SSL certificates generated correctly
- [ ] MySQL container started correctly
- [ ] SSL is enabled (`have_ssl = YES`)
- [ ] phpMyAdmin accessible (optional)
- [ ] Connection from Java works
- [ ] Tests pass with MySQL SSL

---

**Note:** This configuration is for development and testing. For production, review all security parameters.
