# API Reference Documentation

This directory contains comprehensive API reference documentation, technical details, and utility class guides.

## 📚 Documents in This Directory (4 files)

### 🎯 Essential Reference
| Document | Size | Description | Recommended For |
|----------|------|-------------|-----------------|
| **[API_GUIDE.md](API_GUIDE.md)** | 192 KB | **Complete API reference** - All classes, methods, examples | **PRIMARY REFERENCE** |
| **[TECHNICAL_DETAILS.md](TECHNICAL_DETAILS.md)** | 73 KB | Database schema, security model, architecture | System understanding |

### 🔧 Utility Documentation
| Document | Description | Recommended For |
|----------|-------------|-----------------|
| **[UTILITY_CLASSES_GUIDE.md](UTILITY_CLASSES_GUIDE.md)** | Utility classes reference | Helper methods |
| **[FORMAT_UTIL_AND_BLOCK_OPTIONS_GUIDE.md](FORMAT_UTIL_AND_BLOCK_OPTIONS_GUIDE.md)** | Formatting utilities and block options | Data formatting |

## 🚀 Quick Start

### Finding Information

#### I need to know...
- **How to use a specific method** → [API_GUIDE.md](API_GUIDE.md) - Search for method name
- **Database schema details** → [TECHNICAL_DETAILS.md](TECHNICAL_DETAILS.md) - Section 2
- **How encryption works** → [TECHNICAL_DETAILS.md](TECHNICAL_DETAILS.md) - Section 3
- **Utility methods available** → [UTILITY_CLASSES_GUIDE.md](UTILITY_CLASSES_GUIDE.md)
- **How to format data** → [FORMAT_UTIL_AND_BLOCK_OPTIONS_GUIDE.md](FORMAT_UTIL_AND_BLOCK_OPTIONS_GUIDE.md)

## 📖 API_GUIDE.md - Primary Reference

### Structure (192 KB, comprehensive)
1. **Core Blockchain API** - Block operations, chain management
2. **UserFriendlyEncryptionAPI** - 212 methods for encryption and search
3. **Search APIs** - Multiple search implementations
4. **Security APIs** - Key management, encryption services
5. **Recovery APIs** - Chain recovery and validation
6. **Utility APIs** - Helper classes and methods
7. **Thread-Safety** - Concurrency guarantees
8. **Examples** - Code snippets for all major features

### Key Sections

#### Core Operations
```java
// See: API_GUIDE.md - Section 1
Blockchain blockchain = new Blockchain();
blockchain.addBlock(data, privateKey, publicKey);
Block block = blockchain.getBlock(0);
```

#### User-Friendly Encryption
```java
// See: API_GUIDE.md - Section 2
UserFriendlyEncryptionAPI api = new UserFriendlyEncryptionAPI(blockchain, "alice", keyPair);
Block encrypted = api.storeEncryptedData("data", "password");
List<Block> results = api.smartSearchEncryptedData("keyword", "password", 10);
```

#### Thread-Safety Guarantees
- All 212 UserFriendlyEncryptionAPI methods are thread-safe
- Global `StampedLock` protects all blockchain operations
- Optimistic reads for ~50% better performance
- See: API_GUIDE.md - Section "Thread-Safety and Concurrent Usage"

## 🏗️ TECHNICAL_DETAILS.md - Architecture

### Contents (73 KB)
1. **Overview** - Project structure and design principles
2. **Database Schema** - JPA entities and relationships
3. **Security Model** - Cryptographic primitives and access control
4. **Performance** - Optimization strategies and benchmarks
5. **Thread-Safety** - Concurrency architecture
6. **Off-Chain Storage** - Large data handling

### Key Information

#### Database Schema
- **Block Entity**: Main blockchain data structure
- **BlockSequence Entity**: Thread-safe block numbering
- **AuthorizedKey Entity**: Access control with revocation
- **OffChainData Entity**: Large file metadata

**See**: TECHNICAL_DETAILS.md - Section 2

#### Cryptographic Primitives
- **Hashing**: SHA3-256 (FIPS 202)
- **Signatures**: ECDSA secp256r1
- **Encryption**: AES-256-GCM
- **Key Derivation**: PBKDF2-SHA256

**See**: TECHNICAL_DETAILS.md - Section 3

## 🔧 Utility Classes

### UTILITY_CLASSES_GUIDE.md
Comprehensive documentation for helper classes:
- **CryptoUtil** - Cryptographic operations
- **FormatUtil** - Data formatting and conversion
- **ValidationUtil** - Input validation
- **CompressionUtil** - Data compression

### FORMAT_UTIL_AND_BLOCK_OPTIONS_GUIDE.md
Specific guide for:
- **FormatUtil** methods and usage
- **BlockOptions** configuration
- Formatting patterns and examples

## 📊 Documentation Statistics

| Document | Size | Lines | Sections |
|----------|------|-------|----------|
| API_GUIDE.md | 192 KB | ~6,500 | 100+ |
| TECHNICAL_DETAILS.md | 73 KB | ~2,500 | 20+ |
| UTILITY_CLASSES_GUIDE.md | 14 KB | ~500 | 10+ |
| FORMAT_UTIL_AND_BLOCK_OPTIONS_GUIDE.md | 15 KB | ~550 | 8+ |

## 🎯 Common Use Cases

### Finding Method Documentation
1. Open **[API_GUIDE.md](API_GUIDE.md)**
2. Use Ctrl+F / Cmd+F to search for method name
3. Read method signature, parameters, returns, examples

### Understanding System Architecture
1. Open **[TECHNICAL_DETAILS.md](TECHNICAL_DETAILS.md)**
2. Navigate to relevant section (database, security, performance)
3. Review diagrams and explanations

### Using Utility Methods
1. Open **[UTILITY_CLASSES_GUIDE.md](UTILITY_CLASSES_GUIDE.md)**
2. Find the utility class you need
3. Copy example code and adapt

## 🔍 Search Tips

### In API_GUIDE.md
Search for:
- Method names: `addBlock`, `searchByCategory`
- Class names: `UserFriendlyEncryptionAPI`, `Blockchain`
- Features: "encryption", "search", "validation"

### In TECHNICAL_DETAILS.md
Search for:
- Components: "database", "security", "threading"
- Technologies: "SHA3", "ECDSA", "StampedLock"
- Concepts: "off-chain", "block numbering"

## 🔗 Related Documentation

### Getting Started
- **[../getting-started/GETTING_STARTED.md](../getting-started/GETTING_STARTED.md)** - Basic setup
- **[../getting-started/EXAMPLES.md](../getting-started/EXAMPLES.md)** - Code examples

### Feature-Specific Guides
- **[../search/](../search/)** - Search functionality
- **[../security/](../security/)** - Security and encryption
- **[../testing/](../testing/)** - Testing and thread-safety

### Technical Reports
- **[../reports/](../reports/)** - Audits and analysis

## 📈 API Evolution

### Recent Updates (October 2025)
- ✅ StampedLock migration (~50% read performance improvement)
- ✅ AtomicReference atomicity fixes (multi-field protection)
- ✅ Memory-safe pagination (10K default limits)
- ✅ Database field overflow protection

**See**: [../reports/](../reports/) for detailed reports

### Version History
- **v1.0.5** - Current version
- **v1.0.0** - Initial release

## ⚠️ Important Notes

### Thread-Safety
All blockchain operations use `StampedLock` (NOT reentrant). See [TECHNICAL_DETAILS.md](TECHNICAL_DETAILS.md) and [../testing/THREAD_SAFETY_STANDARDS.md](../testing/THREAD_SAFETY_STANDARDS.md) for details.

### Memory Limits
- Maximum batch size: 10,000 items
- Maximum search results: 10,000 (configurable)
- Maximum export size: 500,000 blocks

**See**: [API_GUIDE.md](API_GUIDE.md) - Memory Safety section

### Database Compatibility
- SQLite (development)
- PostgreSQL (production)
- MySQL (production)
- H2 (testing)

**See**: [TECHNICAL_DETAILS.md](TECHNICAL_DETAILS.md) - Database section

---

**Directory**: `docs/reference/`
**Files**: 4
**Total Size**: ~294 KB
**Last Updated**: 2025-10-04
