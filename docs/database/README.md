# Database & Configuration

Database-agnostic architecture and configuration guides.

## 📚 Documents (4 files)

| Document | Description |
|----------|-------------|
| **[DATABASE_AGNOSTIC.md](DATABASE_AGNOSTIC.md)** | Switch between SQLite/PostgreSQL/MySQL/H2 with zero code changes |
| **[DATABASE_FIELD_LIMITS.md](DATABASE_FIELD_LIMITS.md)** | Database field size limits and overflow protection |
| **[CONFIGURATION_STORAGE_GUIDE.md](CONFIGURATION_STORAGE_GUIDE.md)** | JPAConfigurationStorage comprehensive guide |
| **[DATABASE_CONFIGURATION_UTILITIES.md](DATABASE_CONFIGURATION_UTILITIES.md)** | 🆕 Configuration utility classes API reference (9 classes) |

## 🎯 Supported Databases

- **H2** - **Default** (development, demos, testing)
  - Persistent file mode (default): `./blockchain.mv.db`
  - In-memory mode (tests): auto-cleanup
- **PostgreSQL** - Production (high concurrency)
- **MySQL** - Production (high concurrency)
- **SQLite** - Legacy (single-user apps)

## 🆕 What's New in v1.0.5

- **H2 is now the default database** (replaces SQLite)
- Better concurrent write support out of the box
- Persistent file storage by default (`./blockchain.mv.db`)
- Backwards compatible - SQLite still supported

---
**Directory**: `docs/database/` | **Files**: 4 | **Updated**: 2025-10-16
