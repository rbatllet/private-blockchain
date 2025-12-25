# PostgreSQL 18 with SSL/TLS - Docker Setup

## 📋 Overview

This configuration provides a PostgreSQL 18 server with SSL/TLS enabled, ideal for development and testing of the Private Blockchain project. PostgreSQL 18 includes native SSL/TLS support with TLS 1.2 and 1.3 protocols.

## 🏗️ Architecture

```
docker/
├── docker-compose-postgres.yml    # Docker Compose configuration for PostgreSQL
├── start-postgres.sh               # Quick start script for PostgreSQL
├── test-postgres-ssl.sh            # SSL connection test script
└── postgresql/
    ├── generate-certs.sh           # SSL certificates generator
    ├── config/
    │   └── postgresql.conf         # PostgreSQL custom configuration
    └── certs/                      # SSL certificates (generated)
        ├── ca.pem                 # CA Certificate
        ├── ca-key.pem             # CA Private Key
        ├── server-cert.pem        # Server Certificate
        └── server-key.pem         # Server Private Key
```

## 🚀 Quick Start

### Option A: Automated Setup (Recommended)

```bash
cd docker
./start-postgres.sh
```

This script will:
1. Generate SSL certificates
2. Start PostgreSQL container
3. Display connection information

### Option B: Manual Setup

#### 1. Generate SSL certificates

```bash
cd docker/postgresql
./generate-certs.sh
```

This will generate RSA 3072-bit self-signed certificates valid for 2 years (NIST-compliant).

#### 2. Start PostgreSQL with SSL

```bash
cd docker
docker-compose -f docker-compose-postgres.yml up -d
```

#### 3. Verify SSL is enabled

```bash
# Connect to PostgreSQL
docker exec -it postgres-blockchain-ssl psql -U blockchain_user -d blockchain_prod

# Inside PostgreSQL, run:
SHOW ssl;
```

You should see:
```
 ssl
-----
 on
```

## 🔗 Connect from Java Application

### Option A: Using `DatabaseConfig.createPostgreSQLConfig()` (v1.0.6+)

```java
// In your Java application (default: sslmode=verify-full)
DatabaseConfig postgresConfig = DatabaseConfig.createPostgreSQLConfig(
    "localhost",           // host
    5432,                   // port
    "blockchain_prod",      // database name
    "blockchain_user",      // username
    "SecurePassword123!"    // password
);
// Uses: jdbc:postgresql://localhost:5432/blockchain_prod?ssl=true&sslmode=verify-full
```

### Option B: Manual configuration with SSL

```java
// Production-ready with certificate verification (v1.0.6+)
String jdbcUrl = "jdbc:postgresql://localhost:5432/blockchain_prod" +
    "?ssl=true" +                           // Enable SSL
    "&sslmode=verify-full";                 // Verify server certificate (DEFAULT v1.0.6+)

JPAUtil.initialize(
    DatabaseConfig.builder()
        .databaseType(DatabaseType.POSTGRESQL)
        .databaseUrl(jdbcUrl)
        .username("blockchain_user")
        .password("SecurePassword123!")
        .build()
);
```

### 📋 JDBC SSL Configuration according to PostgreSQL 18

According to the official PostgreSQL documentation, the correct JDBC configuration for SSL is:

#### Standard JPA Properties (Hibernate v1.0.6+):
```properties
jakarta.persistence.jdbc.url=jdbc:postgresql://localhost:5432/blockchain_prod?ssl=true&sslmode=verify-full
jakarta.persistence.jdbc.user=blockchain_user
jakarta.persistence.jdbc.password=SecurePassword123!
```

#### HikariCP Specific Configuration (v1.0.6+):
```properties
hibernate.hikari.jdbcUrl=jdbc:postgresql://localhost:5432/blockchain_prod?ssl=true&sslmode=verify-full
hibernate.hikari.username=blockchain_user
hibernate.hikari.password=SecurePassword123!
```

#### JDBC SSL Parameters Explained:

| Parameter | Description |
|-----------|-------------|
| `ssl=true` | Enables SSL/TLS for the connection |
| `sslmode=disable` | No SSL (not recommended) |
| `sslmode=allow` | Try SSL, fall back to non-SSL |
| `sslmode=prefer` (default) | Try SSL, fall back to non-SSL |
| `sslmode=require` | Require SSL (doesn't verify certificate) |
| `sslmode=verify-ca` | Require SSL and verify CA |
| `sslmode=verify-full` | Require SSL, verify CA and hostname |
| `sslrootcert=/path/to/ca.pem` | Path to CA certificate file |
| `sslcert=/path/to/cert.pem` | Path to client certificate |
| `sslkey=/path/to/key.pem` | Path to client private key |

#### PostgreSQL SSL Configuration (postgresql.conf):

```ini
# Enable SSL
ssl = on

# Certificate files (set via command line in docker-compose)
ssl_cert_file = '/etc/postgresql/certs/server-cert.pem'
ssl_key_file = '/etc/postgresql/certs/server-key.pem'
ssl_ca_file = '/etc/postgresql/certs/ca.pem'

# Protocol versions (PostgreSQL 18)
ssl_min_protocol_version = 'TLSv1.2'
ssl_max_protocol_version = 'TLSv1.3'

# ECDH curves for key exchange (PostgreSQL 18+)
ssl_ecdh_curve = 'X25519:prime256v1'

# Cipher suites
ssl_ciphers = 'HIGH:!aNULL'
```

#### Complete JDBC URLs:

**Production (v1.0.6+ default - verifies CA):**
```
jdbc:postgresql://localhost:5432/blockchain_prod?ssl=true&sslmode=verify-full
```

**Development with self-signed certificates:**
```
jdbc:postgresql://localhost:5432/blockchain_prod?ssl=true&sslmode=verify-full&sslrootcert=/path/to/ca.pem
```

**With client certificate authentication:**
```
jdbc:postgresql://localhost:5432/blockchain_prod?ssl=true&sslmode=verify-full&sslcert=/path/to/client-cert.pem&sslkey=/path/to/client-key.pem
```

**⚠️ Security Note:** In production with valid CA certificates, use `sslmode=verify-full` to ensure both certificate and hostname verification.

## 🛠️ Docker Commands

### Start services
```bash
docker-compose -f docker-compose-postgres.yml up -d
```

### View logs
```bash
docker-compose -f docker-compose-postgres.yml logs -f postgres
```

### Stop services
```bash
docker-compose -f docker-compose-postgres.yml down
```

### Stop and remove volumes (⚠️ data will be lost!)
```bash
docker-compose -f docker-compose-postgres.yml down -v
```

### Restart PostgreSQL
```bash
docker-compose -f docker-compose-postgres.yml restart postgres
```

### Enter PostgreSQL container
```bash
docker exec -it postgres-blockchain-ssl bash
```

### Connect directly to PostgreSQL
```bash
docker exec -it postgres-blockchain-ssl psql -U blockchain_user -d blockchain_prod
```

## 💡 Interactive Database Operations

### Connect as different users

```bash
# Connect as application user to specific database
docker exec -it postgres-blockchain-ssl psql -U blockchain_user -d blockchain_prod

# Connect as postgres superuser
docker exec -it postgres-blockchain-ssl psql -U postgres

# Connect to specific database with postgres user
docker exec -it postgres-blockchain-ssl psql -U postgres -d blockchain_prod
```

### Common PostgreSQL commands

```sql
-- Once connected to PostgreSQL, you can run:

-- List all databases
\l
-- or
SELECT datname FROM pg_database WHERE datistemplate = false;

-- Connect to a database
\c blockchain_prod

-- List all tables in current database
\dt
-- or
SELECT tablename FROM pg_tables WHERE schemaname = 'public';

-- Describe table structure
\d table_name
-- or
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_name = 'table_name';

-- Show table indexes
\di table_name
-- or
SELECT indexname, indexdef
FROM pg_indexes
WHERE tablename = 'table_name';

-- Show table creation details
\d+ table_name

-- Count rows in a table
SELECT COUNT(*) FROM table_name;

-- Show first 10 rows
SELECT * FROM table_name LIMIT 10;

-- Show all PostgreSQL settings
SHOW ALL;

-- Show SSL status
SHOW ssl;

-- Show current connections
SELECT * FROM pg_stat_activity WHERE datname = 'blockchain_prod';

-- Show database size
SELECT pg_size_pretty(pg_database_size('blockchain_prod'));

-- Show table sizes
SELECT
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- Show largest tables
SELECT
    relname AS table_name,
    pg_size_pretty(pg_total_relation_size(relid)) AS total_size,
    pg_size_pretty(pg_relation_size(relid)) AS data_size,
    pg_size_pretty(pg_total_relation_size(relid) - pg_relation_size(relid)) AS index_size
FROM pg_catalog.pg_statio_user_tables
ORDER BY pg_total_relation_size(relid) DESC
LIMIT 10;
```

### Quick queries from command line

```bash
# Execute single query
docker exec -it postgres-blockchain-ssl psql -U blockchain_user -d blockchain_prod -c "SELECT * FROM blocks LIMIT 5;"

# Execute query and output as CSV
docker exec -it postgres-blockchain-ssl psql -U blockchain_user -d blockchain_prod -c "COPY (SELECT * FROM blocks LIMIT 5) TO STDOUT WITH CSV HEADER" > output.csv

# Count rows without entering psql
docker exec -it postgres-blockchain-ssl psql -U blockchain_user -d blockchain_prod -c "SELECT COUNT(*) FROM blocks;"

# Execute query and format as aligned table
docker exec -it postgres-blockchain-ssl psql -U blockchain_user -d blockchain_prod -c "SELECT * FROM blocks LIMIT 5;" -A

# Run query from file
docker exec -i postgres-blockchain-ssl psql -U blockchain_user -d blockchain_prod < query.sql
```

### Database backup and restore

```bash
# Backup entire database
docker exec -it postgres-blockchain-ssl pg_dump -U blockchain_user blockchain_prod > backup.sql

# Backup specific tables
docker exec -it postgres-blockchain-ssl pg_dump -U blockchain_user -t blocks -t offchain_data blockchain_prod > backup_tables.sql

# Backup with custom format (recommended)
docker exec -it postgres-blockchain-ssl pg_dump -U blockchain_user -F c -f /var/lib/postgresql/backup.dump blockchain_prod

# Restore database from SQL file
docker exec -i postgres-blockchain-ssl psql -U blockchain_user blockchain_prod < backup.sql

# Restore from custom format backup
docker exec -i postgres-blockchain-ssl pg_restore -U blockchain_user -d blockchain_prod -v /var/lib/postgresql/backup.dump

# Backup to file inside container
docker exec postgres-blockchain-ssl pg_dump -U blockchain_user blockchain_prod > /var/lib/postgresql/backup.sql
docker cp postgres-blockchain-ssl:/var/lib/postgresql/backup.sql ./backup.sql

# Backup all databases
docker exec -it postgres-blockchain-ssl pg_dumpall -U postgres > all_databases.sql
```

### Database operations

```bash
# Create a new database
docker exec -it postgres-blockchain-ssl psql -U postgres -c "CREATE DATABASE test_db;"

# Drop a database (⚠️ careful!)
docker exec -it postgres-blockchain-ssl psql -U postgres -c "DROP DATABASE test_db;"

# Create a new user
docker exec -it postgres-blockchain-ssl psql -U postgres -c "CREATE USER new_user WITH PASSWORD 'Password123!';"

# Grant privileges
docker exec -it postgres-blockchain-ssl psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE blockchain_prod TO new_user;"

# Grant schema privileges
docker exec -it postgres-blockchain-ssl psql -U postgres -d blockchain_prod -c "GRANT ALL ON SCHEMA public TO new_user;"

# Show all users
docker exec -it postgres-blockchain-ssl psql -U postgres -c "\du"
# or
docker exec -it postgres-blockchain-ssl psql -U postgres -c "SELECT usename, usecreatedb, usesuper FROM pg_user;"

# Reassign database ownership
docker exec -it postgres-blockchain-ssl psql -U postgres -c "ALTER DATABASE blockchain_prod OWNER TO new_user;"
```

### Performance and monitoring

```sql
-- Show slow queries
SELECT
    query,
    calls,
    total_time,
    mean_time,
    max_time
FROM pg_stat_statements
ORDER BY mean_time DESC
LIMIT 10;

-- Show table statistics
SELECT
    schemaname,
    tablename,
    seq_scan,
    seq_tup_read,
    idx_scan,
    idx_tup_fetch
FROM pg_stat_user_tables
ORDER BY seq_scan DESC;

-- Show index usage
SELECT
    schemaname,
    tablename,
    indexname,
    idx_scan,
    idx_tup_read,
    idx_tup_fetch
FROM pg_stat_user_indexes
ORDER BY idx_scan DESC;

-- Show cache hit ratio
SELECT
    sum(heap_blks_read) as heap_read,
    sum(heap_blks_hit) as heap_hit,
    sum(heap_blks_hit) / (sum(heap_blks_hit) + sum(heap_blks_read)) AS cache_hit_ratio
FROM pg_statio_user_tables;

-- Show vacuum statistics
SELECT
    relname,
    last_vacuum,
    last_autovacuum,
    vacuum_count,
    autovacuum_count
FROM pg_stat_user_tables
ORDER BY last_autovacuum DESC;

-- Show long-running queries
SELECT
    pid,
    now() - query_start as duration,
    query,
    state
FROM pg_stat_activity
WHERE (now() - query_start) > interval '5 minutes'
AND state != 'idle'
ORDER BY duration DESC;

-- Show connection statistics
SELECT
    count(*) as total,
    state,
    application_name
FROM pg_stat_activity
GROUP BY state, application_name
ORDER BY total DESC;

-- Show database size and bloat
SELECT
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size,
    pg_size_pretty(pg_indexes_size(schemaname||'.'||tablename)) AS index_size
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
```

### PostgreSQL metadata queries

```sql
-- Show all columns in a table
SELECT
    column_name,
    data_type,
    is_nullable,
    column_default
FROM information_schema.columns
WHERE table_name = 'blocks'
ORDER BY ordinal_position;

-- Show foreign keys
SELECT
    tc.table_name,
    tc.constraint_name,
    kcu.column_name,
    ccu.table_name AS foreign_table_name,
    ccu.column_name AS foreign_column_name
FROM information_schema.table_constraints AS tc
JOIN information_schema.key_column_usage AS kcu
    ON tc.constraint_name = kcu.constraint_name
JOIN information_schema.constraint_column_usage AS ccu
    ON ccu.constraint_name = tc.constraint_name
WHERE tc.constraint_type = 'FOREIGN KEY';

-- Show check constraints
SELECT
    conname AS constraint_name,
    conrelid::regclass AS table_name,
    pg_get_constraintdef(c.oid) AS constraint_definition
FROM pg_constraint c
JOIN pg_namespace n ON n.oid = c.connamespace
WHERE contype = 'c'
AND conrelid::regclass = 'blocks'::regclass;

-- Show table row count estimate
SELECT
    schemaname,
    tablename,
    n_live_tup AS estimated_rows
FROM pg_stat_user_tables
ORDER BY n_live_tup DESC;
```

### psql meta-commands reference

```bash
# Useful psql meta-commands (run inside psql)

\?              # Show all psql commands
\h              # Help on SQL commands
\q              # Quit psql
\conninfo       # Show current connection info
\dt             # List tables
\di             # List indexes
\ds             # List sequences
\dv             # List views
\du             # List users
\dp table_name  # Show table privileges
\l              # List databases
\c db_name      # Connect to database
\d table_name   # Describe table
\d+ table_name  # Describe table (detailed)
\x              # Toggle expanded output
\timing         # Toggle timing display
\echo 'text'    # Print text
\set var value  # Set variable
\echo :var      # Print variable
```

## 📊 pgAdmin4 (Optional)

For graphical database administration:

```bash
# Start with pgAdmin4
docker-compose -f docker-compose-postgres.yml --profile tools up -d

# Access at: http://localhost:5050
# Email: admin@blockchain.local
# Password: AdminPassword123!
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
cd docker/postgresql
rm -rf certs/*
./generate-certs.sh
docker-compose -f docker-compose-postgres.yml restart postgres
```

### View SSL connection statistics

```bash
# Connect to PostgreSQL
docker exec -it postgres-blockchain-ssl psql -U blockchain_user -d blockchain_prod

# View SSL statistics
SELECT * FROM pg_stat_ssl;
```

## 🐛 Troubleshooting

### Error: "SSL error: certificate verify failed"

**Problem:** The Java client doesn't trust the self-signed certificate (v1.0.6+ uses `sslmode=verify-full` by default).

**Solution 1 (Recommended for development):** Specify the CA certificate path:

```java
"?ssl=true&sslmode=verify-full&sslrootcert=/path/to/docker/postgresql/certs/ca.pem"
```

**Solution 2 (Development only - NOT recommended):** Temporarily disable verification:

```java
"?ssl=true&sslmode=require"  // ⚠️ Vulnerable to MITM attacks
```

For production:
- ✅ Add the CA certificate to Java's truststore (recommended)
- ✅ Use certificates from a trusted CA (Let's Encrypt, DigiCert, etc.)

### Error: "FATAL: no pg_hba.conf entry for host"

**Problem:** SSL connections are not allowed in pg_hba.conf.

**Solution:** Verify that pg_hba.conf allows SSL connections. The default Docker image includes:
```
hostssl all all all scram-sha-256
```

### Error: "Port 5432 already in use"

**Problem:** Another PostgreSQL is already running on port 5432.

**Solution 1:** Stop the other PostgreSQL:
```bash
# Linux
sudo systemctl stop postgresql
# or
brew services stop postgresql  # macOS
```

**Solution 2:** Change the port in `docker-compose-postgres.yml`:
```yaml
ports:
  - "5433:5432"  # Use 5433 instead of 5432
```

### View generated certificates

```bash
cd docker/postgresql/certs
openssl x509 -in ca.pem -text -noout
openssl x509 -in server-cert.pem -text -noout
```

### Verify SSL connection

```bash
# Test with openssl
openssl s_client -connect localhost:5432 -starttls postgres -showcerts

# You should see:
# - Certificate chain
# - Subject: CN = postgresql-blockchain
# - Issuer: CN = postgresql-blockchain-CA
# - SSL-Session: TLS 1.3
```

### Check SSL configuration in PostgreSQL

```bash
# Connect to PostgreSQL
docker exec -it postgres-blockchain-ssl psql -U blockchain_user -d blockchain_prod

# Show all SSL settings
SHOW ALL LIKE 'ssl%';
```

### Run SSL connection test

```bash
cd docker
./test-postgres-ssl-connection.sh
```

This script will verify:
1. PostgreSQL container is running
2. SSL connection works
3. SSL configuration is correct
4. Connection with application user works

## 📝 Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `POSTGRES_DB` | `blockchain_prod` | Database name |
| `POSTGRES_USER` | `blockchain_user` | Application user |
| `POSTGRES_PASSWORD` | `SecurePassword123!` | User password (CHANGE!) |
| `POSTGRES_PORT` | `5432` | PostgreSQL exposed port |
| `PGADMIN_PORT` | `5050` | pgAdmin4 port |
| `PGADMIN_EMAIL` | `admin@blockchain.local` | pgAdmin4 email |
| `PGADMIN_PASSWORD` | `AdminPassword123!` | pgAdmin4 password (CHANGE!) |

## 🔧 Production Considerations

### For production, change:

1. **Passwords**: Use strong and unique passwords
2. **Certificates**: Use certificates from a valid CA (v1.0.6+ uses `sslmode=verify-full` by default)
3. **Network**: Use an isolated Docker network or VPN
4. **Backups**: Configure regular database backups (pg_dump, WAL archiving)
5. **Monitoring**: Enable PostgreSQL monitoring (pg_stat_statements)
6. **SSL Mode**: Default `sslmode=verify-full` is production-ready (v1.0.6+)

### Production configuration example:

```yaml
# docker-compose-postgres-prod.yml
services:
  postgres:
    image: postgres:18
    secrets:
      - postgres_user_password
    environment:
      POSTGRES_PASSWORD_FILE: /run/secrets/postgres_user_password
    # ... other configs ...

secrets:
  postgres_user_password:
    file: ./secrets/postgres_password.txt
```

### PostgreSQL SSL Best Practices (Production):

1. **Use `sslmode=verify-full`** to verify both certificate and hostname
2. **Implement certificate rotation** before expiration
3. **Use TLS 1.3** exclusively (`ssl_min_protocol_version = 'TLSv1.3'`)
4. **Monitor SSL connections** via `pg_stat_ssl` view
5. **Disable weak ciphers** with `ssl_ciphers = 'HIGH:!aNULL'`
6. **Use strong ECDH curves** (`ssl_ecdh_curve = 'X25519:prime256v1'`)

## 📚 References

- [PostgreSQL 18 Official Documentation](https://www.postgresql.org/docs/18/)
- [PostgreSQL 18 SSL/TLS Configuration](https://www.postgresql.org/docs/18/libpq-ssl.html)
- [PostgreSQL JDBC Driver Documentation](https://jdbc.postgresql.org/documentation/)
- [PostgreSQL Docker Official Image](https://hub.docker.com/_/postgres)

## ✅ Verification Checklist

- [ ] SSL certificates generated correctly
- [ ] PostgreSQL container started correctly
- [ ] SSL is enabled (`SHOW ssl;` returns `on`)
- [ ] pgAdmin4 accessible (optional)
- [ ] Connection from Java works
- [ ] Tests pass with PostgreSQL SSL
- [ ] SSL statistics show encrypted connections

---

**Note:** This configuration is for development and testing. For production, review all security parameters and use certificates from a trusted Certificate Authority.
