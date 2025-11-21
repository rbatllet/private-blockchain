# Database & Configuration

Database-agnostic architecture and configuration guides.

## 📚 Documents (8 files)

| Document | Description |
|----------|-------------|
| **[JPA_TRANSACTION_GUIDE.md](JPA_TRANSACTION_GUIDE.md)** | 🆕 **v1.0.6** JPA transaction isolation and getLastBlock() usage guide |
| **[TRANSACTION_ISOLATION_FIX.md](TRANSACTION_ISOLATION_FIX.md)** | 🆕 Transaction isolation fix for getLastBlock() - constraint violation prevention |
| **[GETLASTBLOCK_DOCUMENTATION_UPDATE.md](GETLASTBLOCK_DOCUMENTATION_UPDATE.md)** | 🆕 Documentation update summary - getLastBlock() usage guidance across all docs |
| **[DATABASE_MIGRATION_STRATEGY.md](DATABASE_MIGRATION_STRATEGY.md)** | Comprehensive guide on hbm2ddl.auto vs DatabaseMigrator (when to use each) |
| **[DATABASE_AGNOSTIC.md](DATABASE_AGNOSTIC.md)** | Switch between SQLite/PostgreSQL/MySQL/H2 with zero code changes |
| **[DATABASE_FIELD_LIMITS.md](DATABASE_FIELD_LIMITS.md)** | Database field size limits and overflow protection |
| **[CONFIGURATION_STORAGE_GUIDE.md](CONFIGURATION_STORAGE_GUIDE.md)** | JPAConfigurationStorage comprehensive guide |
| **[DATABASE_CONFIGURATION_UTILITIES.md](DATABASE_CONFIGURATION_UTILITIES.md)** | Configuration utility classes API reference (9 classes) |

## 🎯 Supported Databases

- **H2** - **Default** (development, demos, testing)
  - Persistent file mode (default): `./blockchain.mv.db`
  - In-memory mode (tests): auto-cleanup
- **PostgreSQL** - Production (high concurrency)
- **MySQL** - Production (high concurrency)
- **SQLite** - Legacy (single-user apps)

## 🆕 What's New in v1.0.6

- **Transaction Isolation Fix** - Fixed constraint violation in addBlockWithOffChainData
- **Performance Optimization** - getLastBlock() now uses MAX() instead of ORDER BY (O(1) vs O(n log n))
- **Transaction-Aware Methods** - New overloaded getLastBlock(EntityManager em) for active transactions
- **Documentation Updates** - Comprehensive usage guidance for getLastBlock() across API_GUIDE and other docs

## What's New in v1.0.5

- **H2 is now the default database** (replaces SQLite)
- Better concurrent write support out of the box
- Persistent file storage by default (`./blockchain.mv.db`)
- Backwards compatible - SQLite still supported

---
**Directory**: `docs/database/` | **Files**: 8 | **Updated**: 2025-01-21
