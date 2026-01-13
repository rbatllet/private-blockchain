# Security & Encryption Documentation

This directory contains comprehensive security, encryption, and key management documentation for the Private Blockchain.

## 📚 Documents in This Directory (17 files)

### 🎯 Essential Guides
| Document | Description | Recommended For |
|----------|-------------|-----------------|
| **[SECURITY_GUIDE.md](SECURITY_GUIDE.md)** | Security best practices and guidelines | **START HERE** - All developers |
| **[PRE_AUTHORIZATION_GUIDE.md](PRE_AUTHORIZATION_GUIDE.md)** | Pre-Authorization Security Model (v1.0.6+) | **REQUIRED** - User creation workflow |
| **[ROLE_BASED_ACCESS_CONTROL.md](ROLE_BASED_ACCESS_CONTROL.md)** | 🆕 **v1.0.6** RBAC system with 4 roles (SUPER_ADMIN, ADMIN, USER, READ_ONLY) | Access control implementation |
| **[ENCRYPTION_GUIDE.md](ENCRYPTION_GUIDE.md)** | Block encryption and metadata layer management | Encryption basics |
| **[KEY_MANAGEMENT_GUIDE.md](KEY_MANAGEMENT_GUIDE.md)** | Hierarchical key management with rotation | Key lifecycle |
| **[BLOCKCHAIN_MASTER_ENCRYPTION_KEY.md](BLOCKCHAIN_MASTER_ENCRYPTION_KEY.md)** | 🆕 **v1.0.6** Blockchain Master Encryption Key (BMEK) architecture | Master key management |

### 🔧 Configuration & Integration
| Document | Description | Recommended For |
|----------|-------------|-----------------|
| **[ENCRYPTION_CONFIG_INTEGRATION_GUIDE.md](ENCRYPTION_CONFIG_INTEGRATION_GUIDE.md)** | Encryption configuration integration | Configuration setup |
| **[ENCRYPTED_EXPORT_IMPORT_GUIDE.md](ENCRYPTED_EXPORT_IMPORT_GUIDE.md)** | Encrypted chain export/import procedures | Data portability |
| **[EXCEPTION_BASED_ERROR_HANDLING_V1_0_6.md](EXCEPTION_BASED_ERROR_HANDLING_V1_0_6.md)** | 🆕 **v1.0.6** Exception-based error handling (breaking change) | Error handling migration |

### 📖 Reference & Quick Guides
| Document | Description | Recommended For |
|----------|-------------|-----------------|
| **[SECURITY_CLASSES_GUIDE.md](SECURITY_CLASSES_GUIDE.md)** | Security classes reference documentation | API reference |
| **[KEY_DERIVATION_GUIDE.md](KEY_DERIVATION_GUIDE.md)** | Key derivation functions and PBKDF2 usage | Cryptographic key generation |
| **[CALLER_IDENTIFICATION_PROPOSAL.md](CALLER_IDENTIFICATION_PROPOSAL.md)** | 🆕 **v1.0.6** Caller identification for RBAC implementation | Architecture reference |

### 🚨 Vulnerability Reports & Security Fixes
| Document | Description | Status |
|----------|-------------|--------|
| **[VULNERABILITY_REPORT_CVE_addAuthorizedKey.md](VULNERABILITY_REPORT_CVE_addAuthorizedKey.md)** | 🔴 **CRITICAL** Unauthorized self-authorization vulnerability (CVSS 9.8) | ✅ Fixed v1.0.6 |
| **[VULNERABILITY_REPORT_HIERARCHICAL_KEY_RBAC.md](VULNERABILITY_REPORT_HIERARCHICAL_KEY_RBAC.md)** | 🔴 **CRITICAL** Hierarchical key RBAC bypass (6 vulnerabilities, CVSS 9.1) | ✅ Fixed v1.0.6 |
| **[AUTO_AUTHORIZATION_SECURITY_FIX_PLAN.md](AUTO_AUTHORIZATION_SECURITY_FIX_PLAN.md)** | Auto-authorization security fix completion report (6 attack vectors) | ✅ Completed 2025-11-02 |

## 🚀 Recommended Reading Order

### For New Developers
1. **[SECURITY_GUIDE.md](SECURITY_GUIDE.md)** - Understand security fundamentals
2. **[PRE_AUTHORIZATION_GUIDE.md](PRE_AUTHORIZATION_GUIDE.md)** - Learn user creation workflow (v1.0.6+)
3. **[ENCRYPTION_GUIDE.md](ENCRYPTION_GUIDE.md)** - Learn encryption basics
4. **[KEY_MANAGEMENT_GUIDE.md](KEY_MANAGEMENT_GUIDE.md)** - Master key management

### For Test Development (v1.0.6+)
1. **[PRE_AUTHORIZATION_GUIDE.md](PRE_AUTHORIZATION_GUIDE.md)** - **REQUIRED** - Pre-authorization pattern for tests
2. **[SECURITY_GUIDE.md](SECURITY_GUIDE.md)** - Security best practices

### For Production Deployment
1. **[SECURITY_GUIDE.md](SECURITY_GUIDE.md)** - Review all best practices
2. **[KEY_MANAGEMENT_GUIDE.md](KEY_MANAGEMENT_GUIDE.md)** - Implement key rotation
3. **[ENCRYPTED_EXPORT_IMPORT_GUIDE.md](ENCRYPTED_EXPORT_IMPORT_GUIDE.md)** - Backup procedures

### For Compliance Requirements (GDPR, HIPAA)
1. **[ENCRYPTION_GUIDE.md](ENCRYPTION_GUIDE.md)** - Data protection mechanisms
2. **[KEY_MANAGEMENT_GUIDE.md](KEY_MANAGEMENT_GUIDE.md)** - Key lifecycle for compliance

## 🔐 Security Architecture

### Cryptographic Primitives
- **Hashing**: SHA3-256 (FIPS 202 standard, quantum-resistant)
- **Signatures**: ML-DSA-87 (NIST FIPS 204, 256-bit quantum-resistant, Module-Lattice Digital Signature Algorithm)
- **Encryption**: AES-256-GCM (authenticated encryption, quantum-resistant)
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

// ML-DSA-87 post-quantum: Generate independent key pairs (no derivation)
KeyPair rootKey = CryptoUtil.generateKeyPair();  // ML-DSA-87
KeyPair operationalKey = CryptoUtil.generateKeyPair();  // Independent key

// Save both keys together (ML-DSA-87 requires saving public + private)
SecureKeyStorage.saveKeyPair("root-key", rootKey, masterPassword);
SecureKeyStorage.saveKeyPair("operational-key", operationalKey, operationalPassword);
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
| **Block Signing** | ML-DSA-87 (NIST FIPS 204, 256-bit quantum-resistant) | SECURITY_GUIDE.md |
| **Data Encryption** | AES-256-GCM (quantum-resistant) | ENCRYPTION_GUIDE.md |
| **Key Derivation** | Hierarchical key management | KEY_MANAGEMENT_GUIDE.md |
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

### Critical Security Updates (v1.0.6)
- **2025-11-14**: 🔴 **CRITICAL** Fixed hierarchical key RBAC bypass (6 vulnerabilities, CVSS 9.1)
- **2025-11-04**: 🔴 **CRITICAL** Fixed unauthorized self-authorization (CVSS 9.8)
- **2025-11-02**: ✅ Auto-authorization security fix completed (6 attack vectors)
- **2025-10-04**: StampedLock migration (thread-safety improvements)
- **2025-10-04**: AtomicReference atomicity fixes (credential protection)

**See**: [Vulnerability Reports](#-vulnerability-reports--security-fixes) above and [../reports/](../reports/) for audit reports

### Vulnerability Reporting
If you discover a security vulnerability, please report it to the project maintainers immediately. Do not disclose publicly until a fix is available.

---

**Directory**: `docs/security/`
**Files**: 15
**Last Updated**: 2026-01-12
