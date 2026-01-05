# PostgreSQL 18 with SSL/TLS - Docker Setup

## 📋 Overview

This configuration provides a PostgreSQL 18 server with SSL/TLS enabled, ideal for development and testing of the Private Blockchain project. PostgreSQL 18 includes native SSL/TLS support with TLS 1.2 and 1.3 protocols.

## 🏗️ Architecture

```
docker/
├── docker-compose-postgres.yml # Docker Compose configuration
└── postgresql/
    ├── generate-certs.zsh      # SSL certificates generator
    ├── config/
    │   └── postgresql.conf     # PostgreSQL custom configuration
    └── certs/                  # SSL certificates (generated)
        ├── ca.pem              # CA Certificate
        ├── ca-key.pem          # CA Private Key
        ├── server-cert.pem     # Server Certificate
        └── server-key.pem      # Server Private Key
```

## 🚀 Quick Start

### Option A: Automated Setup (Recommended)

```bash
cd docker
./start-postgres.zsh
```

This script will:
1. Generate SSL certificates
2. Start PostgreSQL container
3. Display connection information

### Option B: Manual Setup

#### 1. Generate SSL certificates

```bash
cd docker/postgresql
./generate-certs.zsh
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

### Option A: Using `DatabaseConfig.createPostgreSQLConfig()`

```java
// In your Java application
DatabaseConfig postgresConfig = DatabaseConfig.createPostgreSQLConfig(
    "localhost",           // host
    5432,                   // port
    "blockchain_prod",      // database name
    "blockchain_user",      // username
    "SecurePassword123!"    // password
);
```

### Option B: Manual configuration with SSL

```java
String jdbcUrl = "jdbc:postgresql://localhost:5432/blockchain_prod" +
    "?ssl=true" +                           // Enable SSL
    "&sslmode=require";                     // Require SSL

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

#### Standard JPA Properties (Hibernate):
```properties
jakarta.persistence.jdbc.url=jdbc:postgresql://localhost:5432/blockchain_prod?ssl=true&sslmode=require
jakarta.persistence.jdbc.user=blockchain_user
jakarta.persistence.jdbc.password=SecurePassword123!
```

#### HikariCP Specific Configuration:
```properties
hibernate.hikari.jdbcUrl=jdbc:postgresql://localhost:5432/blockchain_prod?ssl=true&sslmode=require
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

**Development (self-signed certificates):**
```
jdbc:postgresql://localhost:5432/blockchain_prod?ssl=true&sslmode=require
```

**Production (CA verified):**
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
./generate-certs.zsh
docker-compose -f docker-compose-postgres.yml restart postgres
```

### View SSL connection statistics

```bash
# Connect to PostgreSQL
docker exec -it postgres-blockchain-ssl psql -U blockchain_user -d blockchain_prod

# View SSL statistics
SELECT * FROM pg_stat_ssl;

# Check if current connection uses SSL
SELECT ssl_is_used();
```

## 🐛 Troubleshooting

### Error: "SSL error: certificate verify failed"

**Problem:** The Java client doesn't trust the self-signed certificate.

**Solution:** For development, use `sslmode=require` which doesn't verify the certificate:

```java
"?ssl=true&sslmode=require"
```

For production, either:
- Add the CA certificate to Java's truststore
- Use a certificate from a trusted CA

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
2. **Certificates**: Use certificates from a valid CA
3. **Network**: Use an isolated Docker network or VPN
4. **Backups**: Configure regular database backups (pg_dump, WAL archiving)
5. **Monitoring**: Enable PostgreSQL monitoring (pg_stat_statements)
6. **SSL Mode**: Use `sslmode=verify-full` for maximum security

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
