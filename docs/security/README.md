# Security & Encryption Documentation

This directory contains comprehensive security, encryption, and key management documentation for the Private Blockchain.

## 📚 Documents in This Directory (6 files)

### 🎯 Essential Guides
| Document | Description | Recommended For |
|----------|-------------|-----------------|
| **[SECURITY_GUIDE.md](SECURITY_GUIDE.md)** | Security best practices and guidelines | **START HERE** - All developers |
| **[ENCRYPTION_GUIDE.md](ENCRYPTION_GUIDE.md)** | Block encryption and metadata layer management | Encryption basics |
| **[KEY_MANAGEMENT_GUIDE.md](KEY_MANAGEMENT_GUIDE.md)** | Hierarchical key management with rotation | Key lifecycle |

### 🔧 Configuration & Integration
| Document | Description | Recommended For |
|----------|-------------|-----------------|
| **[ENCRYPTION_CONFIG_INTEGRATION_GUIDE.md](ENCRYPTION_CONFIG_INTEGRATION_GUIDE.md)** | Encryption configuration integration | Configuration setup |
| **[ENCRYPTED_EXPORT_IMPORT_GUIDE.md](ENCRYPTED_EXPORT_IMPORT_GUIDE.md)** | Encrypted chain export/import procedures | Data portability |

### 📖 Reference
| Document | Description | Recommended For |
|----------|-------------|-----------------|
| **[SECURITY_CLASSES_GUIDE.md](SECURITY_CLASSES_GUIDE.md)** | Security classes reference documentation | API reference |

## 🚀 Recommended Reading Order

### For New Developers
1. **[SECURITY_GUIDE.md](SECURITY_GUIDE.md)** - Understand security fundamentals
2. **[ENCRYPTION_GUIDE.md](ENCRYPTION_GUIDE.md)** - Learn encryption basics
3. **[KEY_MANAGEMENT_GUIDE.md](KEY_MANAGEMENT_GUIDE.md)** - Master key management

### For Production Deployment
1. **[SECURITY_GUIDE.md](SECURITY_GUIDE.md)** - Review all best practices
2. **[KEY_MANAGEMENT_GUIDE.md](KEY_MANAGEMENT_GUIDE.md)** - Implement key rotation
3. **[ENCRYPTED_EXPORT_IMPORT_GUIDE.md](ENCRYPTED_EXPORT_IMPORT_GUIDE.md)** - Backup procedures

## 🔐 Security Architecture

### Cryptographic Primitives
- **Hashing**: SHA3-256 (FIPS 202 standard)
- **Signatures**: ECDSA with secp256r1 curve
- **Encryption**: AES-256-GCM (authenticated encryption)
- **Key Derivation**: PBKDF2 with SHA-256

### Key Management Hierarchy
```
Root Key (Master)
    ├─ Intermediate Key 1
    │   ├─ Operational Key 1.1
    │   └─ Operational Key 1.2
    └─ Intermediate Key 2
        ├─ Operational Key 2.1
        └─ Operational Key 2.2
```

**See**: [KEY_MANAGEMENT_GUIDE.md](KEY_MANAGEMENT_GUIDE.md) for details

### Access Control
- Pre-approved user registration via `addAuthorizedKey()`
- Public key verification for all blocks
- Revocation support with timestamp tracking
- No anonymous blockchain access

**See**: [SECURITY_GUIDE.md](SECURITY_GUIDE.md) for implementation

## 🎯 Common Tasks

### Encrypt a Block
```java
// See: ENCRYPTION_GUIDE.md
EncryptionService encryption = new EncryptionService();
String encrypted = encryption.encryptData("sensitive data", "password");
blockchain.addBlock(encrypted, privateKey, publicKey);
```

### Manage Keys
```java
// See: KEY_MANAGEMENT_GUIDE.md
ECKeyDerivation keyDerivation = new ECKeyDerivation();
KeyPair rootKey = keyDerivation.generateMasterKeyPair();
KeyPair operationalKey = keyDerivation.deriveChildKey(rootKey, 1);
```

### Export Encrypted Chain
```java
// See: ENCRYPTED_EXPORT_IMPORT_GUIDE.md
blockchain.exportEncryptedChain("backup.enc", "strong-password");
```

### Configure Encryption
```java
// See: ENCRYPTION_CONFIG_INTEGRATION_GUIDE.md
EncryptionConfig config = EncryptionConfig.createHighSecurityConfig();
UserFriendlyEncryptionAPI api = new UserFriendlyEncryptionAPI(blockchain, config);
```

## 🛡️ Security Best Practices

### ✅ DO
- Use strong passwords (12+ characters, mixed case, numbers, symbols)
- Rotate keys periodically (see KEY_MANAGEMENT_GUIDE.md)
- Store private keys securely (never in code or version control)
- Use AES-256-GCM for all encryption
- Validate all inputs before processing
- Audit key usage and access patterns

### ❌ DON'T
- Hardcode passwords or keys in source code
- Share private keys between users
- Use weak passwords (< 8 characters)
- Disable signature verification
- Skip key rotation indefinitely
- Store unencrypted sensitive data

**See**: [SECURITY_GUIDE.md](SECURITY_GUIDE.md) for complete checklist

## 🔬 Security Features

| Feature | Implementation | Documentation |
|---------|---------------|---------------|
| **Block Signing** | ECDSA secp256r1 | SECURITY_GUIDE.md |
| **Data Encryption** | AES-256-GCM | ENCRYPTION_GUIDE.md |
| **Key Derivation** | Hierarchical (BIP32-like) | KEY_MANAGEMENT_GUIDE.md |
| **Password Hashing** | PBKDF2-SHA256 | ENCRYPTION_CONFIG_INTEGRATION_GUIDE.md |
| **Access Control** | Pre-approved keys | SECURITY_GUIDE.md |
| **Key Revocation** | Timestamp-based | SECURITY_CLASSES_GUIDE.md |

## 📊 Encryption Performance

| Data Size | Encryption Time | Decryption Time |
|-----------|----------------|-----------------|
| 1 KB | ~1 ms | ~1 ms |
| 100 KB | ~10 ms | ~10 ms |
| 1 MB | ~100 ms | ~100 ms |
| 10 MB | ~1 sec | ~1 sec |

**Note**: Times are approximate. See ENCRYPTION_GUIDE.md for optimization tips.

## 🔗 Related Documentation

- **[../getting-started/GETTING_STARTED.md](../getting-started/GETTING_STARTED.md)** - Basic setup
- **[../reference/API_GUIDE.md](../reference/API_GUIDE.md)** - Complete API reference
- **[../search/USER_FRIENDLY_SEARCH_GUIDE.md](../search/USER_FRIENDLY_SEARCH_GUIDE.md)** - Encrypted search
- **[../testing/TESTING.md](../testing/TESTING.md)** - Security testing

## ⚠️ Security Notices

### Critical Security Updates
- **2025-10-04**: StampedLock migration (thread-safety improvements)
- **2025-10-04**: AtomicReference atomicity fixes (credential protection)

**See**: [../reports/](../reports/) for audit reports

### Vulnerability Reporting
If you discover a security vulnerability, please report it to the project maintainers immediately. Do not disclose publicly until a fix is available.

---

**Directory**: `docs/security/`
**Files**: 6
**Last Updated**: 2025-10-04
