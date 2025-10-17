# Production Deployment Guide

Comprehensive production deployment guide for the Private Blockchain implementation, covering security, performance, monitoring, and maintenance.

## 📋 Table of Contents

- [Production Setup](#-production-setup)
- [Security Configuration](#-security-configuration)
- [Performance Optimization](#-performance-optimization)
- [Monitoring and Maintenance](#-monitoring-and-maintenance)
- [Backup and Recovery](#-backup-and-recovery)
- [Troubleshooting](#-troubleshooting)

## 🏗️ Production Setup

### Infrastructure Requirements

#### Minimum System Requirements
- **CPU**: 4 cores minimum, 8 cores recommended
- **RAM**: 8GB minimum, 16GB recommended  
- **Storage**: 100GB SSD with high IOPS
- **OS**: Ubuntu 20.04 LTS or RHEL 8+
- **Java**: OpenJDK 21 or Oracle JDK 21
- **Network**: Dedicated VLAN for blockchain traffic

#### Production Directory Structure
```bash
/opt/blockchain/
├── application/                 # Application binaries
│   ├── private-blockchain.jar  # Main application
│   ├── blockchain.db           # SQLite database
│   └── blockchain.pid          # Process ID file
├── config/                     # Configuration files
│   ├── application.properties  # App configuration
│   ├── persistence.xml         # JPA configuration
│   └── logging.properties      # Logging configuration
├── keys/                       # Cryptographic keys
│   ├── master/                 # Master keys (highly restricted)
│   ├── operational/            # Daily operation keys
│   └── backup/                 # Key backups
├── scripts/                    # Script utilities directory
│   ├── lib/common_functions.zsh     # Common functions library
│   ├── run_template.zsh         # Template for new scripts
│   └── check-db-cleanup.zsh     # Script compliance checker
├── logs/                      # Application logs
│   ├── application.log        # Main application log
│   ├── security.log           # Security events
│   ├── performance.log        # Performance metrics
│   └── gc.log                 # Garbage collection logs
└── backups/                   # Database backups
    ├── daily/                 # Daily backups
    ├── weekly/                # Weekly backups
    └── disaster/              # Emergency backups
```

### Initial Setup Script
```bash
#!/usr/bin/env zsh
# production-setup.zsh - Initial production environment setup

set -e

APP_HOME="/opt/blockchain"
APP_USER="blockchain"

echo "🚀 Setting up production blockchain environment..."

# Create application user
if ! id "$APP_USER" &>/dev/null; then
    sudo useradd -r -s /bin/false -d "$APP_HOME" "$APP_USER"
    echo "✅ Created application user: $APP_USER"
fi

# Create directory structure
sudo mkdir -p "$APP_HOME"/{application,config,keys/{master,operational,backup},scripts,logs,backups/{daily,weekly,disaster}}
echo "✅ Created directory structure"

# Set ownership and permissions
sudo chown -R "$APP_USER:$APP_USER" "$APP_HOME"
sudo chmod 750 "$APP_HOME"
sudo chmod 700 "$APP_HOME/keys"
sudo chmod 755 "$APP_HOME/scripts"
sudo chmod 750 "$APP_HOME/logs"
sudo chmod 750 "$APP_HOME/backups"
echo "✅ Set permissions"

# Install Java 21 (if not present)
if ! java -version 2>&1 | grep -q "21\|22\|23"; then
    echo "Installing Java 21..."
    sudo apt update
    sudo apt install -y openjdk-21-jdk
fi

# Install SQLite (if not present)
if ! command -v sqlite3 &> /dev/null; then
    sudo apt install -y sqlite3
fi

echo "🎉 Production setup completed!"
echo "Next steps:"
echo "1. Copy application JAR to $APP_HOME/application/"
echo "2. Configure application properties"
echo "3. Set up SSL certificates (if using web interface)"
echo "4. Configure monitoring"
echo "5. Set up backup schedule"
```

## 🔐 Security Configuration

### Key Management

#### Master Key Generation
```bash
#!/usr/bin/env zsh
# generate-master-keys.zsh - Generate master cryptographic keys

APP_HOME="/opt/blockchain"
KEYS_DIR="$APP_HOME/keys/master"

echo "🔐 Generating master keys..."

# Generate master key pair for blockchain administration (ECDSA with secp256r1 curve)
openssl ecparam -name prime256v1 -genkey -out "$KEYS_DIR/master_private.pem"
openssl ec -in "$KEYS_DIR/master_private.pem" -pubout -out "$KEYS_DIR/master_public.pem"

# Generate operational keys for daily use (ECDSA with secp256r1 curve)
openssl ecparam -name prime256v1 -genkey -out "$APP_HOME/keys/operational/admin_private.pem"
openssl ec -in "$APP_HOME/keys/operational/admin_private.pem" -pubout -out "$APP_HOME/keys/operational/admin_public.pem"

openssl ecparam -name prime256v1 -genkey -out "$APP_HOME/keys/operational/operator_private.pem"
openssl ec -in "$APP_HOME/keys/operational/operator_private.pem" -pubout -out "$APP_HOME/keys/operational/operator_public.pem"

# Set restrictive permissions
chmod 600 "$KEYS_DIR"/*.pem
chmod 600 "$APP_HOME/keys/operational"/*.pem
chown blockchain:blockchain "$APP_HOME/keys" -R

echo "✅ Master keys generated successfully"
echo "🚨 CRITICAL: Backup master keys to secure offline storage immediately!"
```

#### Access Control Configuration
```bash
# Set strict file permissions
chmod 700 /opt/blockchain/application
chmod 600 /opt/blockchain/application/blockchain.db
chmod 600 /opt/blockchain/config/*.properties
chmod 755 /opt/blockchain/scripts/*.zsh

# Create dedicated blockchain user
sudo useradd -r -s /bin/false blockchain
sudo chown -R blockchain:blockchain /opt/blockchain
```

### Network Security (If Exposed)

#### Firewall Configuration
```bash
# If running web interface (not included in core application)
sudo ufw allow from 10.0.0.0/8 to any port 8080
sudo ufw deny 8080

# Only allow specific management IPs
sudo ufw allow from 192.168.1.100 to any port 22
sudo ufw allow from 192.168.1.101 to any port 22
```

#### SSL/TLS Configuration (For Web Interface)
```yaml
# nginx.conf - Reverse proxy with SSL
server {
    listen 443 ssl;
    server_name blockchain.company.com;
    
    ssl_certificate /opt/blockchain/ssl/cert.pem;
    ssl_certificate_key /opt/blockchain/ssl/key.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    
    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # Security headers
        add_header X-Content-Type-Options nosniff;
        add_header X-Frame-Options DENY;
        add_header X-XSS-Protection "1; mode=block";
    }
}
```

## ⚡ Performance Optimization

### Database Optimization

#### SQLite Performance Tuning
```sql
-- /opt/blockchain/scripts/optimize-database.sql
-- Run these commands periodically for optimal performance

-- Enable WAL mode for better concurrency
PRAGMA journal_mode=WAL;

-- Optimize SQLite settings
PRAGMA synchronous=NORMAL;
PRAGMA cache_size=20000;       -- 20MB cache
PRAGMA temp_store=memory;
PRAGMA mmap_size=1073741824;   -- 1GB memory mapping

-- Rebuild indexes
REINDEX;

-- Analyze query performance
ANALYZE;

-- Clean up fragmentation
VACUUM;
```

#### JPA Configuration for Production
```xml
<!-- /opt/blockchain/config/persistence.xml - Production JPA configuration -->
<?xml version="1.0" encoding="UTF-8"?>
<persistence xmlns="http://java.sun.com/xml/ns/persistence"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="http://java.sun.com/xml/ns/persistence
             http://java.sun.com/xml/ns/persistence/persistence_2_0.xsd"
             version="2.0">

    <persistence-unit name="blockchainPU" transaction-type="RESOURCE_LOCAL">
        <provider>org.hibernate.jpa.HibernatePersistenceProvider</provider>
        
        <!-- Entities -->
        <class>com.rbatllet.blockchain.entity.Block</class>
        <class>com.rbatllet.blockchain.entity.AuthorizedKey</class>
        
        <properties>
            <!-- Database connection settings -->
            <property name="jakarta.persistence.jdbc.driver" value="org.sqlite.JDBC"/>
            <property name="jakarta.persistence.jdbc.url" value="jdbc:sqlite:/opt/blockchain/application/blockchain.db?journal_mode=WAL"/>
            
            <!-- Hibernate specific settings -->
            <property name="hibernate.dialect" value="org.hibernate.community.dialect.SQLiteDialect"/>
            <property name="hibernate.hbm2ddl.auto" value="validate"/>  <!-- Use validate in production -->
            <property name="hibernate.show_sql" value="false"/>
            <property name="hibernate.format_sql" value="false"/>
            
            <!-- Connection pool settings - Increased for production -->
            <property name="hibernate.connection.pool_size" value="20"/>
            
            <!-- Improve transaction handling -->
            <property name="hibernate.connection.autocommit" value="false"/>
            <property name="hibernate.current_session_context_class" value="thread"/>
            
            <!-- Connection timeout and validation -->
            <property name="hibernate.connection.timeout" value="30000"/>
            <property name="hibernate.connection.validation_timeout" value="5000"/>
            
            <!-- Enable the query cache for production -->
            <property name="hibernate.cache.use_query_cache" value="true"/>
            <property name="hibernate.cache.use_second_level_cache" value="true"/>
            <property name="hibernate.cache.region.factory_class" value="org.hibernate.cache.jcache.JCacheRegionFactory"/>
        </properties>
    </persistence-unit>
</persistence>
```

#### Database Maintenance Script
```bash
#!/usr/bin/env zsh
# database-maintenance.zsh - Weekly database optimization

APP_HOME="/opt/blockchain"
DB_FILE="$APP_HOME/application/blockchain.db"
BACKUP_FILE="$APP_HOME/backups/maintenance_backup_$(date +%Y%m%d).db"

echo "🔧 Starting database maintenance..."

# Create backup before maintenance
cp "$DB_FILE" "$BACKUP_FILE"
echo "📦 Backup created: $BACKUP_FILE"

# Check database integrity
echo "🔍 Checking database integrity..."
INTEGRITY_CHECK=$(sqlite3 "$DB_FILE" "PRAGMA integrity_check;")
if [ "$INTEGRITY_CHECK" != "ok" ]; then
    echo "❌ Database integrity check failed: $INTEGRITY_CHECK"
    exit 1
fi
echo "✅ Database integrity check passed"

# Optimize database
echo "⚡ Optimizing database..."
sqlite3 "$DB_FILE" << EOF
PRAGMA journal_mode=WAL;
PRAGMA synchronous=NORMAL;
PRAGMA cache_size=20000;
PRAGMA temp_store=memory;
ANALYZE;
VACUUM;
EOF

# Get database statistics
DB_SIZE=$(stat -c%s "$DB_FILE")
DB_SIZE_MB=$((DB_SIZE / 1024 / 1024))
BLOCK_COUNT=$(sqlite3 "$DB_FILE" "SELECT COUNT(*) FROM blocks;")
KEY_COUNT=$(sqlite3 "$DB_FILE" "SELECT COUNT(*) FROM authorized_keys;")
SEQ_VALUE=$(sqlite3 "$DB_FILE" "SELECT next_value FROM block_sequence WHERE sequence_name='block_number';")

echo "📊 Database statistics:"
echo "  Size: ${DB_SIZE_MB}MB"
echo "  Blocks: $BLOCK_COUNT"
echo "  Keys: $KEY_COUNT"
echo "  Next Block Number: $SEQ_VALUE"

# Verify sequence integrity
if [ "$BLOCK_COUNT" -gt 0 ]; then
    MAX_BLOCK_NUM=$(sqlite3 "$DB_FILE" "SELECT MAX(block_number) FROM blocks;")
    if [ "$SEQ_VALUE" -le "$MAX_BLOCK_NUM" ]; then
        echo "⚠️ Warning: Block sequence value ($SEQ_VALUE) is not greater than max block number ($MAX_BLOCK_NUM)"
        echo "   Fixing sequence value..."
        sqlite3 "$DB_FILE" "UPDATE block_sequence SET next_value = $((MAX_BLOCK_NUM + 1)) WHERE sequence_name='block_number';"
        echo "✅ Sequence fixed: next_value set to $((MAX_BLOCK_NUM + 1))"
    else
        echo "✅ Block sequence integrity verified"
    fi
fi

echo "✅ Database maintenance completed"
```

### Application Performance

#### JVM Tuning
```bash
# production-jvm.conf - JVM optimization for production

# Memory settings
-Xms1g                          # Initial heap size
-Xmx4g                          # Maximum heap size
-XX:NewRatio=3                  # Old/Young generation ratio
-XX:MaxMetaspaceSize=256m       # Metaspace limit

# Garbage Collection (G1GC)
-XX:+UseG1GC                    # Use G1 garbage collector
-XX:MaxGCPauseMillis=200        # Target GC pause time
-XX:G1HeapRegionSize=16m        # G1 heap region size
-XX:G1ReservePercent=10         # Reserve heap percentage

# Performance optimizations
-XX:+UseBiasedLocking          # Enable biased locking
-XX:+OptimizeStringConcat      # Optimize string concatenation
-XX:+UseCompressedOops         # Compress object pointers

# Monitoring and debugging
-XX:+PrintGC                   # Print GC information
-XX:+PrintGCDetails           # Detailed GC information
-Xloggc:/opt/blockchain/logs/gc.log  # GC log file

# Security
-Djava.security.egd=file:/dev/./urandom  # Entropy source
```

## 🔍 Blockchain Validation in Production

### Validation Strategies

Production blockchain validation requires custom Java applications that use the BlockchainService API. The core library does not provide CLI commands - integrate validation into your application layer.

#### Memory-Safe Validation for Large Blockchains

> ⚠️ **CRITICAL FOR PRODUCTION**: Standard `validateChainDetailed()` has memory limits:
> - **Warning threshold**: 100,000 blocks
> - **Hard limit**: 500,000 blocks (throws exception)
> - **Production requirement**: Use `validateChainStreaming()` for chains >100K blocks

**Java Example - Detailed Validation:**
```java
import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.validation.ChainValidationResult;

Blockchain blockchain = new Blockchain();

// For chains < 100K blocks
ChainValidationResult result = blockchain.validateChainDetailed();

System.out.println("📊 Validation Summary: " + result.getSummary());
System.out.println("Structural Integrity: " + (result.isStructurallyIntact() ? "✅" : "❌"));
System.out.println("Authorization Compliance: " + (result.isFullyCompliant() ? "✅" : "⚠️"));
System.out.println("Total Blocks: " + result.getTotalBlocks());
System.out.println("Valid Blocks: " + result.getValidBlocks());
System.out.println("Revoked Blocks: " + result.getRevokedBlocks());
System.out.println("Invalid Blocks: " + result.getInvalidBlocks());

if (!result.isStructurallyIntact()) {
    System.err.println("❌ Chain integrity compromised!");
    System.err.println(result.getDetailedReport());
}
```

**Java Example - Streaming Validation (for large chains):**
```java
import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.core.Blockchain.ValidationSummary;
import com.rbatllet.blockchain.validation.BlockValidationResult;
import com.rbatllet.blockchain.validation.BlockStatus;
import java.util.List;

Blockchain blockchain = new Blockchain();

// For chains > 100K blocks - memory efficient
ValidationSummary summary = blockchain.validateChainStreaming(
    (List<BlockValidationResult> batchResults) -> {
        // Process each batch as it's validated
        batchResults.forEach(result -> {
            if (result.getStatus() != BlockStatus.VALID) {
                System.err.println("Issue in block #" + result.getBlock().getBlockNumber());
            }
        });
    },
    1000  // Batch size
);

System.out.println("📊 Streaming Validation Complete");
System.out.println("Total Blocks: " + summary.getTotalBlocks());
System.out.println("Valid Blocks: " + summary.getValidBlocks());
System.out.println("Invalid Blocks: " + summary.getInvalidBlocks());
System.out.println("Revoked Blocks: " + summary.getRevokedBlocks());
```

**Key Configuration for Large Blockchains:**

Memory thresholds are defined in `MemorySafetyConstants`:
- `SAFE_EXPORT_LIMIT`: 100,000 blocks
- `MAX_EXPORT_LIMIT`: 500,000 blocks (hard limit)
- `MAX_BATCH_SIZE`: 10,000 blocks per batch
- `LARGE_ROLLBACK_THRESHOLD`: 100,000 blocks

**JVM Memory Settings:**
```bash
# For chains with 1M+ blocks
java -Xmx4g -Xms4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 YourApplication

# For chains with 10M+ blocks
java -Xmx8g -Xms8g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 YourApplication
```

**Performance Comparison:**

| Chain Size | validateChainDetailed() | validateChainStreaming() |
|------------|------------------------|--------------------------|
| 10K blocks | ✅ 2s / 50MB RAM | ✅ 3s / 20MB RAM |
| 100K blocks | ⚠️ 45s / 1.2GB RAM | ✅ 60s / 50MB RAM |
| 500K blocks | ❌ Exception | ✅ 5min / 50MB RAM |
| 1M blocks | ❌ Exception | ✅ 10min / 50MB RAM |
| 10M blocks | ❌ Exception | ✅ 90min / 50MB RAM |

**Production Best Practices:**

1. **Automated Size-Based Selection:**
   ```java
   import com.rbatllet.blockchain.core.Blockchain;
   import com.rbatllet.blockchain.core.Blockchain.ValidationSummary;
   import com.rbatllet.blockchain.validation.ChainValidationResult;

   Blockchain blockchain = new Blockchain();
   long blockCount = blockchain.getBlockCount();

   if (blockCount > 100_000) {
       // Use streaming validation for large chains
       ValidationSummary summary = blockchain.validateChainStreaming(
           batchResults -> {
               // Process batches
           },
           1000
       );
       System.out.println("Validation: " + summary.getTotalBlocks() + " blocks");
   } else {
       // Use detailed validation for smaller chains
       ChainValidationResult result = blockchain.validateChainDetailed();
       if (!result.isValid()) {
           System.err.println("Validation failed: " + result.getSummary());
       }
   }
   ```

2. **Alert Thresholds:**
   - **INFO**: Validation started/completed
   - **WARNING**: >1% invalid blocks detected
   - **CRITICAL**: >5% invalid blocks or validation failure

3. **Resource Allocation:**
   - Reserve 2-4GB RAM for streaming validation
   - Use dedicated CPU cores during validation
   - Schedule during low-traffic periods

### Monitoring Validation Results

#### Integration with Monitoring Systems

1. **Prometheus Metrics**
   ```yaml
   # Sample prometheus.yml configuration
   scrape_configs:
     - job_name: 'blockchain_validation'
       static_configs:
         - targets: ['localhost:9091']  # Blockchain metrics endpoint
   ```

2. **Grafana Dashboard**
   - Create a dashboard with the following panels:
     - Validation status (OK/WARNING/CRITICAL)
     - Validation duration over time
     - Number of invalid/revoked blocks
     - Block validation time distribution

3. **Alerting Rules**
   ```yaml
   # Sample alert rules
   groups:
   - name: blockchain.rules
     rules:
     - alert: BlockchainValidationFailed
       expr: blockchain_validation_failed == 1
       for: 5m
       labels:
         severity: critical
       annotations:
         summary: "Blockchain validation failed"
         description: "Blockchain validation has failed. Check logs for details."
   ```

## 📊 Monitoring and Maintenance

### Application Monitoring

#### Health Check Script
```bash
#!/usr/bin/env zsh
# health-check.zsh - Application health monitoring

APP_HOME="/opt/blockchain"
DB_FILE="$APP_HOME/application/blockchain.db"
PID_FILE="$APP_HOME/application/blockchain.pid"

# Check if application is running
if [ ! -f "$PID_FILE" ]; then
    echo "❌ Application not running (PID file missing)"
    exit 1
fi

PID=$(cat "$PID_FILE")
if ! ps -p $PID > /dev/null; then
    echo "❌ Application process not found (PID: $PID)"
    exit 1
fi

# Check database connectivity
if ! sqlite3 "$DB_FILE" "SELECT 1;" > /dev/null 2>&1; then
    echo "❌ Database connection failed"
    exit 1
fi

# Check database integrity
INTEGRITY=$(sqlite3 "$DB_FILE" "PRAGMA quick_check;" 2>&1)
if [ "$INTEGRITY" != "ok" ]; then
    echo "❌ Database integrity check failed: $INTEGRITY"
    exit 1
fi

# Check disk space (warn if less than 1GB free)
FREE_SPACE=$(df "$APP_HOME" | tail -1 | awk '{print $4}')
FREE_SPACE_MB=$((FREE_SPACE / 1024))
if [ $FREE_SPACE_MB -lt 1024 ]; then
    echo "⚠️  Low disk space: ${FREE_SPACE_MB}MB free"
fi

# Check memory usage
MEMORY_USAGE=$(ps -p $PID -o %mem= | awk '{print $1}')
if (( $(echo "$MEMORY_USAGE > 80" | bc -l) )); then
    echo "⚠️  High memory usage: ${MEMORY_USAGE}%"
fi

echo "✅ Application health check passed"
echo "📊 Process: $PID, Memory: ${MEMORY_USAGE}%, Disk: ${FREE_SPACE_MB}MB free"
```

#### Metrics Collection
```bash
#!/usr/bin/env zsh
# collect-metrics.zsh - System metrics collection

APP_HOME="/opt/blockchain"
METRICS_FILE="$APP_HOME/logs/metrics.log"

# Function to log metrics
log_metric() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" >> "$METRICS_FILE"
}

# Database metrics
DB_FILE="$APP_HOME/application/blockchain.db"
if [ -f "$DB_FILE" ]; then
    DB_SIZE=$(stat -c%s "$DB_FILE")
    DB_SIZE_MB=$((DB_SIZE / 1024 / 1024))
    BLOCK_COUNT=$(sqlite3 "$DB_FILE" "SELECT COUNT(*) FROM blocks;" 2>/dev/null || echo "0")
    KEY_COUNT=$(sqlite3 "$DB_FILE" "SELECT COUNT(*) FROM authorized_keys;" 2>/dev/null || echo "0")
    
    log_metric "DATABASE_SIZE_MB=$DB_SIZE_MB"
    log_metric "BLOCK_COUNT=$BLOCK_COUNT"
    log_metric "KEY_COUNT=$KEY_COUNT"
fi

# System metrics
DISK_USAGE=$(df "$APP_HOME" | tail -1 | awk '{print $5}' | sed 's/%//')
MEMORY_USAGE=$(free | grep Mem | awk '{printf "%.1f", $3/$2 * 100.0}')
CPU_USAGE=$(top -bn1 | grep "Cpu(s)" | sed "s/.*, *\([0-9.]*\)%* id.*/\1/" | awk '{print 100 - $1}')

log_metric "DISK_USAGE_PERCENT=$DISK_USAGE"
log_metric "MEMORY_USAGE_PERCENT=$MEMORY_USAGE"
log_metric "CPU_USAGE_PERCENT=$CPU_USAGE"

# Application metrics (if process is running)
PID_FILE="$APP_HOME/application/blockchain.pid"
if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    if ps -p $PID > /dev/null; then
        # Process-specific metrics
        PROCESS_MEMORY=$(ps -p $PID -o rss= | awk '{print $1/1024}')
        PROCESS_CPU=$(ps -p $PID -o %cpu= | awk '{print $1}')
        
        log_metric "PROCESS_MEMORY_MB=$PROCESS_MEMORY"
        log_metric "PROCESS_CPU_PERCENT=$PROCESS_CPU"
        log_metric "APPLICATION_STATUS=RUNNING"
    else
        log_metric "APPLICATION_STATUS=STOPPED"
    fi
else
    log_metric "APPLICATION_STATUS=NOT_STARTED"
fi
```

### Log Management

#### Log Rotation Configuration
```bash
# /etc/logrotate.d/blockchain - Log rotation configuration
/opt/blockchain/logs/*.log {
    daily
    missingok
    rotate 30
    compress
    delaycompress
    notifempty
    sharedscripts
    postrotate
        # Send HUP signal to application to reopen log files
        PID_FILE="/opt/blockchain/application/blockchain.pid"
        if [ -f "$PID_FILE" ]; then
            PID=$(cat "$PID_FILE")
            if ps -p $PID > /dev/null; then
                kill -HUP $PID
            fi
        fi
    endscript
}
```

## 💾 Backup and Recovery

### Automated Backup Strategy

#### Daily Backup Script
```bash
#!/usr/bin/env zsh
# daily-backup.zsh - Automated daily backups

set -e

APP_HOME="/opt/blockchain"
DB_FILE="$APP_HOME/application/blockchain.db"
BACKUP_DIR="$APP_HOME/backups/daily"
WEEKLY_DIR="$APP_HOME/backups/weekly"
RETENTION_DAYS=30

# Create backup directories
mkdir -p "$BACKUP_DIR" "$WEEKLY_DIR"

# Generate backup filename
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="$BACKUP_DIR/blockchain_$DATE.db"
EXPORT_FILE="$BACKUP_DIR/blockchain_export_$DATE.json"

echo "🔄 Starting daily backup: $DATE"

# 1. SQLite database backup
if [ -f "$DB_FILE" ]; then
    # Hot backup using SQLite backup API
    sqlite3 "$DB_FILE" ".backup $BACKUP_FILE"
    
    if [ $? -eq 0 ]; then
        echo "✅ Database backup completed: $BACKUP_FILE"
        
        # Compress backup
        gzip "$BACKUP_FILE"
        echo "📦 Backup compressed: $BACKUP_FILE.gz"
    else
        echo "❌ Database backup failed"
        exit 1
    fi
fi

# 2. Weekly backup (every Sunday)
if [ "$(date +%u)" -eq 7 ]; then
    WEEKLY_FILE="$WEEKLY_DIR/blockchain_weekly_$(date +%Y_W%U).db.gz"
    cp "$BACKUP_FILE.gz" "$WEEKLY_FILE"
    echo "📅 Weekly backup created: $WEEKLY_FILE"
fi

# 3. Cleanup old backups
find "$BACKUP_DIR" -name "*.gz" -mtime +$RETENTION_DAYS -delete
echo "🧹 Cleaned up backups older than $RETENTION_DAYS days"

# 4. Verify backup integrity
gunzip -t "$BACKUP_FILE.gz"
if [ $? -eq 0 ]; then
    echo "✅ Backup integrity verified"
else
    echo "❌ Backup integrity check failed"
    exit 1
fi

# 5. Log backup statistics
BACKUP_SIZE=$(stat -c%s "$BACKUP_FILE.gz")
BACKUP_SIZE_MB=$((BACKUP_SIZE / 1024 / 1024))
echo "📊 Backup size: ${BACKUP_SIZE_MB}MB"

echo "🎉 Daily backup completed successfully"
```

#### Disaster Recovery Script
```zsh
#!/usr/bin/env zsh
# disaster-recovery.zsh - Complete system recovery from backup

# Enable strict error handling and better error messages
emulate -L zsh
set -euo pipefail

# Configuration
APP_HOME="/opt/blockchain"
BACKUP_PATH="${1:-}"
LOG_FILE="$APP_HOME/logs/disaster_recovery_$(date +%Y%m%d_%H%M%S).log"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Logging function
log() {
    local level=$1
    local message=$2
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    
    case $level in
        "INFO") echo -e "[${timestamp}] ${GREEN}INFO${NC} - ${message}" | tee -a "$LOG_FILE" ;;
        "WARN") echo -e "[${timestamp}] ${YELLOW}WARN${NC} - ${message}" | tee -a "$LOG_FILE" >&2 ;;
        "ERROR") echo -e "[${timestamp}] ${RED}ERROR${NC} - ${message}" | tee -a "$LOG_FILE" >&2 ;;
    esac
}

# Check if running as root
if [[ $EUID -ne 0 ]]; then
    log "ERROR" "This script must be run as root"
    exit 1
fi

# Check backup path
if [[ -z "$BACKUP_PATH" ]]; then
    log "INFO" "Usage: $0 <backup_file.db.gz>"
    log "INFO" "Available backups in $APP_HOME/backups/daily/:"
    ls -la "$APP_HOME/backups/daily/" | tee -a "$LOG_FILE"
    exit 1
fi

if [[ ! -f "$BACKUP_PATH" ]]; then
    log "ERROR" "Backup file not found: $BACKUP_PATH"
    exit 1
fi

# Confirm action
log "WARN" "🚨 Starting disaster recovery from: $(basename "$BACKUP_PATH")"
log "WARN" "⚠️  This will replace the current database!"
read -r "?Are you sure you want to continue? (y/N): " confirm

if [[ ! "$confirm" =~ ^[Yy]$ ]]; then
    log "INFO" "Recovery cancelled by user"
    exit 0
fi

# Stop application
log "INFO" "🛑 Stopping application..."
if ! "$APP_HOME/scripts/stop.zsh" >> "$LOG_FILE" 2>&1; then
    log "WARN" "Failed to stop application, attempting to continue..."
fi

# Backup current state
CURRENT_DB="$APP_HOME/application/blockchain.db"
if [[ -f "$CURRENT_DB" ]]; then
    DISASTER_BACKUP="$APP_HOME/backups/disaster_backup_$(date +%Y%m%d_%H%M%S).db"
    if ! cp "$CURRENT_DB" "$DISASTER_BACKUP"; then
        log "ERROR" "Failed to create backup of current database"
        exit 1
    fi
    log "INFO" "💾 Current database backed up to: $DISASTER_BACKUP"
fi

# Restore from backup
log "INFO" "📥 Restoring database from backup..."
if ! gunzip -c "$BACKUP_PATH" > "$CURRENT_DB"; then
    log "ERROR" "Failed to restore from backup"
    exit 1
fi

# Verify restored database
log "INFO" "🔍 Verifying restored database..."
INTEGRITY_CHECK=$(sqlite3 "$CURRENT_DB" "PRAGMA integrity_check;" 2>> "$LOG_FILE" || echo "error")
if [[ "$INTEGRITY_CHECK" != "ok" ]]; then
    log "ERROR" "Restored database integrity check failed: $INTEGRITY_CHECK"
    exit 1
fi

# Set correct permissions
chmod 600 "$CURRENT_DB"
chown blockchain:blockchain "$CURRENT_DB"

# Start application
log "INFO" "🚀 Starting application..."
if ! "$APP_HOME/scripts/start.zsh" >> "$LOG_FILE" 2>&1; then
    log "ERROR" "Failed to start application"
    exit 1
fi

# Verify application startup
log "INFO" "⏳ Verifying application health..."
sleep 10
if "$APP_HOME/scripts/health-check.zsh" >> "$LOG_FILE" 2>&1; then
    # Get recovery stats
    BLOCK_COUNT=$(sqlite3 "$CURRENT_DB" "SELECT COUNT(*) FROM blocks;" 2>> "$LOG_FILE" || echo "unknown")
    KEY_COUNT=$(sqlite3 "$CURRENT_DB" "SELECT COUNT(*) FROM authorized_keys;" 2>> "$LOG_FILE" || echo "unknown")
    
    log "INFO" "✅ Disaster recovery completed successfully"
    log "INFO" "📊 Recovery stats: $BLOCK_COUNT blocks, $KEY_COUNT keys"
    log "INFO" "📝 Detailed logs available at: $LOG_FILE"
else
    log "ERROR" "Application health check failed after recovery"
    log "ERROR" "Check the logs for more details: $LOG_FILE"
    exit 1
fi

## 🚨 Troubleshooting

### Chain Validation Issues

#### Problem: Validation Fails with Invalid Blocks

**Symptoms:**
- `isStructurallyIntact()` returns `false`
- `getInvalidBlocks()` returns count > 0

**Possible Causes:**

1. **Corrupted Block Data**
   - Block data has been modified or corrupted
   - Check database integrity with SQLite tools
   - Restore from backup if corruption confirmed

2. **Invalid Previous Hash**
   - The `previousHash` of a block doesn't match the hash of the previous block
   - This usually indicates data corruption or tampering
   - Use `getInvalidBlocksList()` to identify affected blocks

3. **Invalid Signatures**
   - Block signatures don't match the block data
   - May indicate key compromise or data corruption
   - Review block validation results from `validateChainDetailed()`

**Java Example - Identifying Invalid Blocks:**
```java
import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.validation.ChainValidationResult;
import com.rbatllet.blockchain.entity.Block;
import java.util.List;

Blockchain blockchain = new Blockchain();
ChainValidationResult result = blockchain.validateChainDetailed();

if (!result.isStructurallyIntact()) {
    System.err.println("❌ Chain integrity compromised");

    // Get list of invalid blocks
    List<Block> invalidBlocks = result.getInvalidBlocksList();
    invalidBlocks.forEach(block -> {
        System.err.println("Invalid Block #" + block.getBlockNumber());
        System.err.println("  Hash: " + block.getHash());
        System.err.println("  Previous Hash: " + block.getPreviousHash());
    });

    // Recovery action: restore from backup
}
```

#### Problem: Validation Shows Revoked Keys

**Symptoms:**
- `isFullyCompliant()` returns `false`
- `getRevokedBlocks()` returns count > 0

**Solutions:**

1. **Review Key Revocation Impact**
   ```java
   import com.rbatllet.blockchain.core.Blockchain;
   import com.rbatllet.blockchain.validation.ChainValidationResult;
   import com.rbatllet.blockchain.entity.Block;
   import java.util.List;

   Blockchain blockchain = new Blockchain();
   ChainValidationResult result = blockchain.validateChainDetailed();

   if (!result.isFullyCompliant()) {
       List<Block> revokedBlocks = result.getOrphanedBlocks();
       System.out.println("⚠️ Blocks signed by revoked keys: " + revokedBlocks.size());

       revokedBlocks.forEach(block -> {
           System.out.println("Block #" + block.getBlockNumber() +
                            " signed by revoked key: " + block.getSignerPublicKey());
       });
   }
   ```

2. **Document Revoked Blocks**
   - Blocks signed by revoked keys remain in chain (audit trail)
   - Document the reason for key revocation
   - Implement business logic to handle revoked blocks appropriately

### Performance Issues

#### Problem: Validation Takes Too Long

**Symptoms:**
- Validation process takes longer than expected
- High CPU usage during validation

**Solutions:**

1. **Use Streaming Validation**
   ```java
   import com.rbatllet.blockchain.core.Blockchain;
   import com.rbatllet.blockchain.core.Blockchain.ValidationSummary;

   // For large chains (>100K blocks), use streaming validation
   Blockchain blockchain = new Blockchain();
   ValidationSummary summary = blockchain.validateChainStreaming(
       batchResults -> { /* Process batches */ },
       1000  // Batch size
   );
   // Much faster and memory-efficient for large chains
   ```

2. **Adjust JVM Settings**
   ```bash
   # Increase heap size
   java -Xmx4G -Xms4G -XX:+UseG1GC -XX:MaxGCPauseMillis=200 YourApplication
   ```

3. **Optimize Database**
   ```sql
   -- Run periodically
   PRAGMA journal_mode=WAL;
   PRAGMA synchronous=NORMAL;
   VACUUM;
   ANALYZE;
   ```

### Recovery Procedures

#### Database Recovery

1. **From Backup**
   ```bash
   # Stop application
   # Restore database from backup
   gunzip -c /opt/blockchain/backups/daily/blockchain_backup.db.gz > blockchain.db
   # Verify integrity
   sqlite3 blockchain.db "PRAGMA integrity_check;"
   # Restart application
   ```

2. **Database Backup and Restore**
   - Use SQLite backup commands for database-level backup
   - Restore from backup file to recover blockchain state
   - Verify database integrity after restore with `PRAGMA integrity_check`

### Common Issues and Solutions Issues

#### Issue: High CPU Usage
**Symptoms:**
- CPU usage consistently above 80%
- Slow response times
{{ ... }}
- Application timeouts

**Solutions:**
```bash
# Optimize JVM settings
JAVA_OPTS="$JAVA_OPTS -XX:+UseG1GC -XX:MaxGCPauseMillis=100"

# Optimize database
sqlite3 /opt/blockchain/application/blockchain.db "VACUUM; ANALYZE;"

# Check for inefficient queries
tail -f /opt/blockchain/logs/application.log | grep -i "slow query"
```

#### Issue: Database Corruption
**Symptoms:**
- Chain validation failures
- SQLite integrity check failures
- Application startup errors

**Recovery:**
```bash
# Stop application
/opt/blockchain/scripts/stop.zsh

# Check database integrity
sqlite3 /opt/blockchain/application/blockchain.db "PRAGMA integrity_check;"

# Attempt database repair
sqlite3 /opt/blockchain/application/blockchain.db ".recover /tmp/recovered.db"

# If repair successful, replace database
mv /tmp/recovered.db /opt/blockchain/application/blockchain.db

# If repair fails, restore from backup
/opt/blockchain/scripts/disaster-recovery.zsh /opt/blockchain/backups/daily/latest_backup.db.gz
```

#### Issue: Memory Leaks
**Symptoms:**
- Gradually increasing memory usage
- OutOfMemoryError in logs
- Application becoming unresponsive

**Solutions:**
```bash
# Increase heap size temporarily
# Edit start.zsh: -Xmx4g to -Xmx8g

# Enable heap dump on OOM
JAVA_OPTS="$JAVA_OPTS -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/opt/blockchain/logs/"

# Monitor memory usage over time
watch -n 30 'ps -p $(cat /opt/blockchain/application/blockchain.pid) -o pid,vsz,rss,pmem'
```

### Emergency Procedures

#### Emergency Restart Protocol
```bash
#!/usr/bin/env zsh
# emergency-restart.zsh - Emergency application restart

echo "🚨 EMERGENCY RESTART INITIATED"

# Create incident log
INCIDENT_LOG="/opt/blockchain/logs/incident_$(date +%Y%m%d_%H%M%S).log"
echo "Emergency restart at $(date)" > "$INCIDENT_LOG"

# Capture current state
ps aux | grep blockchain >> "$INCIDENT_LOG"
df -h /opt/blockchain >> "$INCIDENT_LOG"
free -h >> "$INCIDENT_LOG"

# Stop application forcefully if needed
PID_FILE="/opt/blockchain/application/blockchain.pid"
if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    kill -KILL $PID 2>/dev/null || true
    rm -f "$PID_FILE"
fi

# Quick database integrity check
DB_FILE="/opt/blockchain/application/blockchain.db"
if [ -f "$DB_FILE" ]; then
    INTEGRITY=$(sqlite3 "$DB_FILE" "PRAGMA quick_check;" 2>&1)
    echo "Database check: $INTEGRITY" >> "$INCIDENT_LOG"
    
    if [ "$INTEGRITY" != "ok" ]; then
        echo "❌ Database corruption detected, manual intervention required"
        exit 1
    fi
fi

# Restart application
/opt/blockchain/scripts/start.zsh

# Verify startup
sleep 15
if /opt/blockchain/scripts/health-check.zsh; then
    echo "✅ Emergency restart completed successfully"
else
    echo "❌ Emergency restart failed, manual intervention required"
    exit 1
fi
```

---

For comprehensive examples and use cases, see [EXAMPLES.md](../getting-started/EXAMPLES.md) and [API_GUIDE.md](../reference/API_GUIDE.md).

### Advanced Security Best Practices

#### Private Key Management
- **Hardware Security Modules (HSMs)**: Store private keys in dedicated secure hardware
- **Encrypted Storage**: Use strong encryption for private key files at rest
- **Key Derivation**: Implement proper key derivation functions for password-based encryption
- **Access Control**: Restrict private key access to authorized processes only
- **Key Rotation**: Regular rotation of cryptographic keys (monthly/quarterly)

#### Database Security Enhancements
- **Transparent Database Encryption**: Encrypt database files at rest
- **Connection Security**: Use TLS for all database connections
- **Access Logging**: Log all database access for audit trails
- **Data Classification**: Classify sensitive data and apply appropriate protections
- **Backup Encryption**: Encrypt all backup files with separate keys

#### Application-Level Security
- **Authentication**: Implement robust user authentication mechanisms
- **Authorization**: Fine-grained access control for blockchain operations
- **Session Management**: Secure session handling for multi-user environments
- **Input Validation**: Comprehensive validation of all user inputs
- **Rate Limiting**: Prevent abuse through request rate limiting

#### Network Security
- **TLS/SSL**: Use strong encryption for all network communications
- **Certificate Management**: Proper SSL certificate lifecycle management
- **Network Segmentation**: Isolate blockchain infrastructure
- **Firewall Rules**: Restrict network access to necessary ports only
- **VPN Access**: Secure remote access through VPN tunneling

#### Audit and Compliance
- **Comprehensive Logging**: Log all blockchain operations with timestamps
- **Tamper-Evident Logs**: Protect audit logs from modification
- **Regular Audits**: Periodic security assessments and penetration testing
- **Compliance Monitoring**: Automated compliance checking and reporting
- **Incident Response**: Prepared incident response procedures

### Advanced Backup and Recovery

#### Automated Backup Strategies
- **Multi-Tier Backups**: Daily, weekly, monthly backup retention
- **Geographic Distribution**: Store backups in multiple locations
- **Incremental Backups**: Efficient storage using incremental backup strategies
- **Backup Validation**: Automated integrity checking of all backups
- **Recovery Testing**: Regular testing of backup restoration procedures

#### Point-in-Time Recovery
- **Transaction Log Backups**: Frequent transaction log backups for granular recovery
- **Snapshot Management**: Database snapshots for quick recovery points
- **Version Control**: Maintain multiple backup versions for historical recovery
- **Recovery Procedures**: Documented step-by-step recovery processes
- **Recovery Time Objectives**: Defined RTO and RPO for business continuity

#### Disaster Recovery Planning
- **Business Continuity**: Comprehensive disaster recovery plans
- **Hot Standby Systems**: Real-time replication to standby systems
- **Data Replication**: Automated data replication to remote sites
- **Failover Procedures**: Automated failover mechanisms
- **Communication Plans**: Incident communication and escalation procedures

### Performance Optimization Strategies

#### Database Performance
- **Query Optimization**: Regular analysis and optimization of database queries
- **Index Management**: Strategic indexing for optimal query performance
- **Connection Pooling**: Efficient database connection pool configuration
- **Cache Strategies**: Implement caching layers for frequently accessed data
- **Database Partitioning**: Partition large tables for improved performance

#### Application Performance
- **Memory Management**: Optimize JVM heap and garbage collection settings
- **Concurrent Processing**: Implement parallel processing for bulk operations
- **Asynchronous Operations**: Use async processing for non-blocking operations
- **Resource Pooling**: Pool expensive resources like cryptographic operations
- **Performance Profiling**: Regular application performance profiling and tuning

#### Infrastructure Performance
- **Hardware Optimization**: Appropriate hardware sizing for production workloads
- **Storage Performance**: High-performance storage for database operations
- **Network Optimization**: Optimize network configuration for throughput
- **Load Balancing**: Distribute load across multiple application instances
- **Monitoring and Alerting**: Real-time performance monitoring and alerting

### Monitoring and Alerting Systems

#### Blockchain-Specific Monitoring
- **Chain Integrity Monitoring**: Automated chain validation checks
- **Block Addition Monitoring**: Track block addition times and success rates
- **Key Management Monitoring**: Monitor key authorization and revocation events
- **Transaction Volume Monitoring**: Track transaction patterns and anomalies
- **Security Event Monitoring**: Monitor for suspicious blockchain activities

#### System Performance Monitoring
- **Resource Utilization**: Monitor CPU, memory, disk, and network usage
- **Database Performance**: Track query execution times and lock contention
- **Application Metrics**: Monitor application-specific performance metrics
- **Error Rate Monitoring**: Track error rates and failure patterns
- **Capacity Planning**: Predictive monitoring for capacity planning

#### Alerting and Response
- **Threshold-Based Alerts**: Configure alerts for performance thresholds
- **Anomaly Detection**: Machine learning-based anomaly detection
- **Escalation Procedures**: Tiered alerting with escalation paths
- **Automated Responses**: Automated remediation for common issues
- **Incident Management**: Integration with incident management systems

### Scalability Considerations

#### Current Limitations and Mitigation
- **Single Node Architecture**: Design considerations for distributed deployment
- **Storage Growth Management**: Strategies for managing blockchain growth
- **Performance Scaling**: Horizontal and vertical scaling strategies
- **Key Management Scale**: Scaling key management for large organizations
- **Backup Scale**: Scaling backup strategies for large blockchain databases

#### Future Scalability Planning
- **Distributed Architecture**: Planning for multi-node blockchain networks
- **Sharding Strategies**: Database sharding for large-scale deployments
- **Caching Layers**: Implement distributed caching for improved performance
- **Microservices Architecture**: Break down monolithic application into services
- **Cloud-Native Deployment**: Design for cloud-native scalability patterns

For operational procedures and day-to-day management, see the monitoring and maintenance sections above.
For development and testing information, see [TESTING.md](../testing/TESTING.md) and [API_GUIDE.md](../reference/API_GUIDE.md).
