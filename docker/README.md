# Private Blockchain - Docker Database Setup

This directory contains Docker configurations for running PostgreSQL 18 and MySQL 8.0 with SSL/TLS enabled for the Private Blockchain project.

## 📋 Available Databases

| Database | Version | SSL/TLS | Documentation |
|----------|---------|---------|----------------|
| **PostgreSQL** | 18 | ✅ TLS 1.2/1.3 | [README-postgresql.md](README-postgresql.md) |
| **MySQL** | 8.0 | ✅ TLS 1.2/1.3 | [README-mysql.md](README-mysql.md) |

## 🚀 Quick Start

### PostgreSQL (Recommended for Production)

```bash
# Automated setup
./start-postgres.sh

# Or manual setup
cd postgresql && ./generate-certs.sh && cd ..
docker-compose -f docker-compose-postgres.yml up -d
```

**JDBC URL:** `jdbc:postgresql://localhost:5432/blockchain_prod?ssl=true&sslmode=require`

### MySQL (Alternative)

```bash
# Automated setup
./start-mysql.sh

# Or manual setup
cd mysql && ./generate-certs.sh && cd ..
docker-compose -f docker-compose-mysql.yml up -d
```

**JDBC URL:** `jdbc:mysql://localhost:3306/blockchain_prod?useSSL=true&requireSSL=true&trustServerCertificate=true&allowPublicKeyRetrieval=true`

## 🏗️ File Structure

```
docker/
├── README.md                      # This file - General overview
├── README-mysql.md                # MySQL 8.0 documentation
├── README-postgresql.md            # PostgreSQL 18 documentation
│
├── docker-compose-mysql.yml       # MySQL Docker configuration
├── docker-compose-postgres.yml     # PostgreSQL Docker configuration
│
├── start-mysql.sh                 # MySQL quick start script
├── start-postgres.sh              # PostgreSQL quick start script
│
├── test-mysql-ssl-connection.sh   # MySQL SSL test script
├── test-postgres-ssl-connection.sh # PostgreSQL SSL test script
│
├── mysql/                         # MySQL-specific files
│   ├── generate-certs.sh          # SSL certificate generator
│   ├── config/my.cnf              # MySQL configuration
│   └── certs/                     # SSL certificates (generated)
│
└── postgresql/                    # PostgreSQL-specific files
    ├── generate-certs.sh          # SSL certificate generator
    ├── config/postgresql.conf    # PostgreSQL configuration
    └── certs/                     # SSL certificates (generated)
```

## 📊 Database Comparison

| Feature | PostgreSQL 18 | MySQL 8.0 |
|---------|---------------|----------|
| **SSL Protocol** | TLS 1.2, 1.3 | TLS 1.2, 1.3 |
| **Default Cipher** | TLS_AES_256_GCM_SHA384 | TLS_AES_256_GCM_SHA384 |
| **JDBC SSL Parameter** | `ssl=true&sslmode=require` | `useSSL=true&requireSSL=true` |
| **Default Port** | 5432 | 3306 |
| **Admin Tool** | pgAdmin4 (port 5050) | phpMyAdmin (port 8080) |
| **Certificate Location** | `/etc/postgresql/certs/` | `/etc/mysql/certs/` |

## 🔗 Java Integration

Both databases are fully integrated with the Private Blockchain application via `DatabaseConfig`:

```java
// PostgreSQL
DatabaseConfig postgresConfig = DatabaseConfig.createPostgreSQLConfig(
    "localhost", 5432, "blockchain_prod", "blockchain_user", "password"
);

// MySQL
DatabaseConfig mysqlConfig = DatabaseConfig.createMySQLConfig(
    "localhost", 3306, "blockchain_prod", "blockchain_user", "password"
);
```

## 🛠️ Common Commands

### View logs
```bash
# PostgreSQL
docker-compose -f docker-compose-postgres.yml logs -f postgres

# MySQL
docker-compose -f docker-compose-mysql.yml logs -f mysql
```

### Stop services
```bash
# PostgreSQL
docker-compose -f docker-compose-postgres.yml down

# MySQL
docker-compose -f docker-compose-mysql.yml down
```

### Stop and remove volumes (⚠️ data loss)
```bash
# PostgreSQL
docker-compose -f docker-compose-postgres.yml down -v

# MySQL
docker-compose -f docker-compose-mysql.yml down -v
```

## ✅ SSL Verification

### PostgreSQL
```bash
./test-postgres-ssl-connection.sh
```

### MySQL
```bash
./test-mysql-ssl-connection.sh
```

## 🔒 Security Notes

- All databases use **self-signed certificates** for development
- SSL/TLS is **required** for all connections
- In production, use certificates from a trusted CA
- Change default passwords before deploying to production

## 📚 Detailed Documentation

- **PostgreSQL 18 Setup**: [README-postgresql.md](README-postgresql.md)
- **MySQL 8.0 Setup**: [README-mysql.md](README-mysql.md)

---

**For detailed setup instructions, SSL configuration, and troubleshooting, see the specific README for each database.**
