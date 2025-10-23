# Database Migration Strategy

## Overview

This document clarifies when to use **Hibernate automatic schema generation** (`hbm2ddl.auto`) versus the **explicit migration system** (`DatabaseMigrator`). Both approaches serve different purposes and contexts.

## Quick Decision Tree

```
Are you in DEVELOPMENT?
├─ YES: Use hbm2ddl.auto="update" (automatic)
└─ NO: Are you in TESTING?
    ├─ YES: Use hbm2ddl.auto="create-drop" (isolated)
    └─ NO: Are you in PRODUCTION?
        ├─ YES: Use DatabaseMigrator + hbm2ddl.auto="validate"
        └─ UNKNOWN: Treat as PRODUCTION for safety
```

---

## Approach 1: Hibernate Automatic Schema Generation (hbm2ddl.auto)

### When to Use

- ✅ **Development**: Rapid iteration with entity changes
- ✅ **Testing**: Each test run needs fresh schema
- ✅ **Prototyping**: Quick proof-of-concepts
- ✅ **Learning**: Understanding JPA/Hibernate basics

### Configuration

```xml
<!-- Development -->
<property name="hibernate.hbm2ddl.auto" value="update"/>

<!-- Testing (isolated) -->
<property name="hibernate.hbm2ddl.auto" value="create-drop"/>
```

### How It Works

1. **Application starts** → Hibernate inspects entity definitions
2. **Database accessed** → Hibernate compares schema with entities
3. **Differences detected** → Hibernate auto-generates ALTER TABLE statements
4. **Schema updated** → Database modified automatically

### Advantages

- 📈 **Zero friction**: Change entity → Schema auto-updates
- 🚀 **Development speed**: No migration files to write
- 🔄 **Idempotent**: Safe to run multiple times
- 📦 **All-in-one**: No external configuration needed

### Disadvantages

- 🚨 **No audit trail**: Cannot see who/when/how schema changed
- ❌ **No rollback**: Cannot revert to previous schema version
- ⚠️ **Production risk**: Uncontrolled changes dangerous
- 🤔 **Data loss risk**: Complex migrations may lose data
- 📊 **No history**: Cannot track schema evolution
- 🔐 **Security**: No approval process for schema changes

### Example: When hbm2ddl="update"

```java
// Entity definition
@Entity
public class Block {
    @Id private Long id;
    private String data;
}

// Change entity
@Entity
public class Block {
    @Id private Long id;
    private String data;
    private String newField;  // ← Added field
}

// Result: Next startup, Hibernate automatically:
// ALTER TABLE block ADD COLUMN new_field VARCHAR(255);
```

---

## Approach 2: Explicit Database Migrations (DatabaseMigrator)

### When to Use

- ✅ **Production**: All deployments must be controlled
- ✅ **Staging**: Pre-production validation
- ✅ **Teams**: Multiple developers need coordination
- ✅ **Compliance**: Schema changes need audit trail
- ✅ **Complex changes**: Data migration logic needed
- ✅ **Zero-downtime**: Plan deployment windows
- ✅ **Rollback**: Need ability to revert changes

### Configuration

```xml
<!-- Production -->
<property name="hibernate.hbm2ddl.auto" value="validate"/>
```

### How It Works

1. **Create migration file** → `V1__create_tables.sql`, `V2__add_columns.sql`
2. **Register migrations** → `migrator.addMigration(...)`
3. **Run migrations** → `migrator.migrate()` or `cli database migrate run`
4. **Track history** → `schema_version` table records all changes
5. **Validate schema** → Hibernate validates entities match tables

### Advantages

- 📋 **Full audit trail**: Exactly what changed, when, by whom
- ↩️ **Reversible**: Can rollback to any previous version
- 🎯 **Controlled**: Manual approval before deployment
- 🔍 **Testable**: Test migrations independently
- 📊 **Documented**: All schema changes in version control
- 🔐 **Secure**: Proper authorization workflow
- ⏰ **Scheduled**: Can plan migrations during maintenance windows
- 🏢 **Enterprise**: Standard industry practice (Flyway, Liquibase)

### Disadvantages

- 📝 **More work**: Write SQL migration files for every change
- 🐢 **Slower dev**: Cannot change entities and auto-migrate
- 💡 **Requires knowledge**: Developer must understand SQL/schema design
- ❌ **Manual mistakes**: SQL errors possible
- 🗄️ **Database-specific**: SQL differs between PostgreSQL/MySQL/SQLite

### Example: Migration Files

```sql
-- V1__create_initial_schema.sql
CREATE TABLE blocks (
    id BIGINT PRIMARY KEY,
    data VARCHAR(4000),
    created_at TIMESTAMP,
    hash VARCHAR(128) UNIQUE
);

CREATE INDEX idx_blocks_hash ON blocks(hash);
```

```sql
-- V2__add_status_column.sql
ALTER TABLE blocks ADD COLUMN status VARCHAR(50) DEFAULT 'PENDING';
```

```sql
-- V3__add_audit_columns.sql
ALTER TABLE blocks ADD COLUMN created_by VARCHAR(255);
ALTER TABLE blocks ADD COLUMN modified_at TIMESTAMP;
ALTER TABLE blocks ADD COLUMN modified_by VARCHAR(255);
```

---

## Comparison Matrix

| Aspect | hbm2ddl.auto | DatabaseMigrator |
|--------|---|---|
| **Audit Trail** | ❌ No | ✅ Yes |
| **History** | ❌ None | ✅ Complete |
| **Rollback** | ❌ Impossible | ✅ Possible |
| **Data Migration** | ⚠️ Limited | ✅ Full control |
| **Version Control** | ❌ Not tracked | ✅ .sql files |
| **Development Speed** | ✅ Fast | ⚠️ Slower |
| **Production Safe** | ❌ No | ✅ Yes |
| **Approval Workflow** | ❌ None | ✅ Possible |
| **Learning Curve** | ✅ Easy | ⚠️ Moderate |
| **Team Coordination** | ❌ Poor | ✅ Good |
| **Zero-Downtime Migrations** | ❌ No | ✅ Yes (with planning) |
| **Schema Validation** | ✅ Automatic | ✅ Explicit |

---

## Recommended Hybrid Approach

### Development Environment

```xml
<property name="hibernate.hbm2ddl.auto" value="update"/>
```

**Workflow:**
1. Change entity → Hibernate auto-updates schema
2. When ready for production → Create migration file
3. Document the changes in SQL

### Testing Environment

```xml
<property name="hibernate.hbm2ddl.auto" value="create-drop"/>
```

**Workflow:**
1. Each test: Fresh schema (clean state)
2. Reliable test isolation
3. No migration dependency

### Staging Environment

```xml
<property name="hibernate.hbm2ddl.auto" value="validate"/>
```

**Workflow:**
1. Pre-production validation
2. Test migrations before production
3. Verify schema matches expectations

### Production Environment

```xml
<property name="hibernate.hbm2ddl.auto" value="validate"/>
```

**Workflow:**
1. Before deployment: Run migrations manually
   ```bash
   blockchain database migrate run
   ```
2. Validate schema matches entities:
   ```bash
   blockchain database migrate validate
   ```
3. Check migration history:
   ```bash
   blockchain database migrate show-history
   ```
4. Application starts: Hibernate validates (does NOT modify)

---

## Migration Creation Best Practices

### 1. Create Migration for Every Schema Change

Don't rely on hbm2ddl.auto in production. Always create explicit migrations:

```sql
-- ✅ GOOD: Explicit migration file
-- V4__add_performance_indexes.sql
CREATE INDEX idx_blocks_timestamp ON blocks(created_at);
CREATE INDEX idx_blocks_category ON blocks(category);
```

```java
// ❌ BAD: Relying on hbm2ddl.auto in production
// Just add @Index to entity and let Hibernate create indexes automatically
```

### 2. One Logical Change Per Migration

```sql
-- ✅ GOOD: Single responsibility
-- V5__add_email_column.sql
ALTER TABLE blocks ADD COLUMN email VARCHAR(255);

-- ✅ GOOD: Related changes together
-- V6__add_authentication_fields.sql
ALTER TABLE blocks ADD COLUMN username VARCHAR(128);
ALTER TABLE blocks ADD COLUMN password_hash VARCHAR(255);
ALTER TABLE blocks ADD COLUMN auth_token VARCHAR(500);
```

### 3. Include Rollback Information

```sql
-- V7__add_new_table.sql
-- Forward: Create new table
CREATE TABLE block_metadata (
    id BIGINT PRIMARY KEY,
    block_id BIGINT,
    key VARCHAR(255),
    value TEXT,
    FOREIGN KEY (block_id) REFERENCES blocks(id)
);

-- Rollback (commented for reference, not executed):
-- DROP TABLE block_metadata;
```

### 4. Handle Data Migrations Carefully

```sql
-- V8__migrate_status_values.sql
-- Forward: Update schema and data
ALTER TABLE blocks ADD COLUMN new_status VARCHAR(50);

UPDATE blocks
SET new_status = CASE
    WHEN status = 'ACTIVE' THEN 'RUNNING'
    WHEN status = 'INACTIVE' THEN 'PAUSED'
    WHEN status = 'ERROR' THEN 'FAILED'
    ELSE status
END;

ALTER TABLE blocks DROP COLUMN status;
ALTER TABLE blocks RENAME COLUMN new_status TO status;
```

---

## CLI Commands for Migration Management

### Run Pending Migrations

```bash
# Execute all pending migrations
blockchain database migrate run

# Show which migrations will be executed
blockchain database migrate validate
```

### View Migration History

```bash
# Show all migrations applied
blockchain database migrate show-history

# Show in JSON format (for automation)
blockchain database migrate show-history --json
```

### Check Current Version

```bash
# Display current schema version
blockchain database migrate current-version
```

---

## Migration Best Practices for Each Database

### SQLite

```sql
-- SQLite: Use PRAGMA for checking
PRAGMA table_info(blocks);

-- SQLite: No concurrent writers, simpler locking
-- Keep migrations simple and fast
```

### PostgreSQL

```sql
-- PostgreSQL: Recommended for production
-- Supports complex migrations, concurrent safe

-- Example: Add column with computed default
ALTER TABLE blocks
ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Example: Create indexed type
CREATE TYPE block_status AS ENUM ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED');
ALTER TABLE blocks ADD COLUMN status block_status;
```

### MySQL

```sql
-- MySQL: Watch for table locks
-- Use ALGORITHM=INPLACE for non-blocking migrations when possible

-- Example: Add column (will lock table briefly)
ALTER TABLE blocks
ADD COLUMN last_verified_at TIMESTAMP NULL,
ALGORITHM=INPLACE, LOCK=NONE;
```

---

## Troubleshooting

### Schema Mismatch Error

**Problem**: `Validation failed: Table 'blocks' expected but not found`

**Cause**: Migrations not applied, or hbm2ddl.auto="validate"

**Solution**:
```bash
# Check current version
blockchain database migrate current-version

# Run migrations
blockchain database migrate run

# Validate
blockchain database migrate validate
```

### Migration Failed

**Problem**: Migration error, subsequent migrations blocked

**Cause**: SQL error in migration file

**Solution**:
1. Check error details: `blockchain database migrate show-history`
2. Fix the SQL in the migration file
3. Manually rollback (if transactions not supported)
4. Re-run migrations

### Foreign Key Violation

**Problem**: Cannot drop column due to foreign key constraints

**Solution**: Disable/enable foreign keys around migration

```sql
-- MySQL
SET FOREIGN_KEY_CHECKS = 0;
ALTER TABLE blocks DROP COLUMN old_reference;
SET FOREIGN_KEY_CHECKS = 1;
```

---

## Summary

- **Use hbm2ddl.auto** for rapid development iteration
- **Use DatabaseMigrator** for production deployments
- **Version control** all production migrations
- **Test migrations** in staging before production
- **Maintain audit trail** of all schema changes
- **Document reasons** for schema changes
- **Plan migrations** during low-usage windows
- **Always validate** schema before starting application

For details on `DatabaseMigrator` usage, see [API_GUIDE.md](../reference/API_GUIDE.md#database-migrator).

