# Search Documentation

---

## ⚠️ SECURITY UPDATE (v1.0.6)

> **CRITICAL**: All UserFriendlyEncryptionAPI usage now requires **mandatory pre-authorization**. Users must be authorized before performing any operations.

### Required Secure Initialization

All code examples assume this initialization pattern:

```java
// 1. Create blockchain (auto-creates genesis admin)
Blockchain blockchain = new Blockchain();

// 2. Load genesis admin keys
KeyPair genesisKeys = KeyFileLoader.loadKeyPairFromFiles(
    "./keys/genesis-admin.private",
    "./keys/genesis-admin.public"
);

// 3. Create API with genesis admin credentials
UserFriendlyEncryptionAPI api = new UserFriendlyEncryptionAPI(blockchain);
api.setDefaultCredentials("GENESIS_ADMIN", genesisKeys);

// 4. Create user for operations
KeyPair userKeys = api.createUser("username");
api.setDefaultCredentials("username", userKeys);
```

> **💡 NOTE**: See [../reference/API_GUIDE.md](../reference/API_GUIDE.md#-secure-initialization--authorization) for complete security details.

---

This directory contains comprehensive documentation for all search functionality in the Private Blockchain.

## 📚 Documents in This Directory (9 files)

### 🎯 Start Here
| Document | Description | Use Case |
|----------|-------------|----------|
| **[SEARCH_APIS_COMPARISON.md](SEARCH_APIS_COMPARISON.md)** | Compare all search APIs and choose the right one | **START HERE** - Choosing an API |

### 🔍 Search Framework Engine
| Document | Description | Use Case |
|----------|-------------|----------|
| **[SEARCH_FRAMEWORK_GUIDE.md](SEARCH_FRAMEWORK_GUIDE.md)** | Complete guide to Search Framework Engine | Professional search with caching |
| **[SEARCH_COMPARISON.md](SEARCH_COMPARISON.md)** | Detailed comparison of search implementations | Understanding differences |

### 👤 User-Friendly Search
| Document | Description | Use Case |
|----------|-------------|----------|
| **[USER_FRIENDLY_SEARCH_GUIDE.md](USER_FRIENDLY_SEARCH_GUIDE.md)** | UserFriendlyEncryptionAPI search guide | Simplified encrypted search |
| **[EXHAUSTIVE_SEARCH_GUIDE.md](EXHAUSTIVE_SEARCH_GUIDE.md)** | Exhaustive search capabilities | Deep content search |

### 🔬 SearchSpecialistAPI
| Document | Description | Use Case |
|----------|-------------|----------|
| **[SEARCHSPECIALISTAPI_INITIALIZATION_GUIDE.md](SEARCHSPECIALISTAPI_INITIALIZATION_GUIDE.md)** | Initialization and setup guide | Getting started with SearchSpecialistAPI |
| **[SEARCHSPECIALISTAPI_IMPROVEMENTS_V2.md](SEARCHSPECIALISTAPI_IMPROVEMENTS_V2.md)** | Version 2.0 improvements and features | Understanding v2 enhancements |
| **[SEARCHSPECIALISTAPI_INDEX.md](SEARCHSPECIALISTAPI_INDEX.md)** | SearchSpecialistAPI documentation index | Quick reference |

### 📖 Basic Search
| Document | Description | Use Case |
|----------|-------------|----------|
| **[SEARCH_PUBLIC_METHOD_GUIDE.md](SEARCH_PUBLIC_METHOD_GUIDE.md)** | Public search methods in Blockchain.java | Simple content search |

## 🎯 Decision Tree: Which Search API?

```
START: What do you need?
│
├─ Simple keyword search?
│  └─> SEARCH_PUBLIC_METHOD_GUIDE.md (Basic Blockchain search)
│
├─ Search encrypted blocks?
│  ├─ Need full control?
│  │  └─> SEARCHSPECIALISTAPI_INITIALIZATION_GUIDE.md
│  └─ Want simplicity?
│     └─> USER_FRIENDLY_SEARCH_GUIDE.md
│
├─ Professional search with caching?
│  └─> SEARCH_FRAMEWORK_GUIDE.md
│
└─ Deep content analysis?
   └─> EXHAUSTIVE_SEARCH_GUIDE.md
```

## 📊 Search API Comparison

| API | Complexity | Features | Best For |
|-----|-----------|----------|----------|
| **Basic Blockchain** | 🟢 Low | Simple content search | Quick queries |
| **UserFriendlyAPI** | 🟡 Medium | Encrypted search, smart levels | General use |
| **SearchFrameworkEngine** | 🟡 Medium | Caching, optimization | Professional apps |
| **SearchSpecialistAPI** | 🔴 High | Full control, password-based | Advanced users |

## 🚀 Quick Start

### New to Search?
1. Read **[SEARCH_APIS_COMPARISON.md](SEARCH_APIS_COMPARISON.md)** first
2. Choose your API based on needs
3. Follow the specific guide for that API

### Common Use Cases

#### Search Plain Text Blocks
```java
// See: SEARCH_PUBLIC_METHOD_GUIDE.md
List<Block> results = blockchain.searchBlocksByContent("keyword");
```

#### Search Encrypted Blocks (Simple)
```java
// See: USER_FRIENDLY_SEARCH_GUIDE.md (after secure initialization shown above)
// Genesis admin creates alice user:
KeyPair aliceKeys = api.createUser("alice");
api.setDefaultCredentials("alice", aliceKeys);
List<Block> results = api.smartSearchEncryptedData("keyword", "password", 10);
```

#### Professional Search with Caching
```java
// See: SEARCH_FRAMEWORK_GUIDE.md
SearchFrameworkEngine engine = new SearchFrameworkEngine(blockchain);
List<Block> results = engine.searchWithMetadata("keyword", filters);
```

## 📈 Performance Tips

- **Use SearchFrameworkEngine** for repeated searches (caching)
- **Use smart search levels** in UserFriendlyAPI (FAST → DATA → EXHAUSTIVE)
- **Limit results** to avoid memory issues
- **See [SEARCH_APIS_COMPARISON.md](SEARCH_APIS_COMPARISON.md)** for detailed performance comparison

## 🔗 Related Documentation

- **[../getting-started/EXAMPLES.md](../getting-started/EXAMPLES.md)** - Search examples
- **[../reference/API_GUIDE.md](../reference/API_GUIDE.md)** - Complete API reference
- **[../security/ENCRYPTION_GUIDE.md](../security/ENCRYPTION_GUIDE.md)** - Encryption guide

---

**Directory**: `docs/search/`
**Files**: 9
**Last Updated**: 2025-10-04
