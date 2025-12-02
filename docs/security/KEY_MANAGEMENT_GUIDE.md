# Key Management Guide

## 🔑 Overview

The UserFriendlyEncryptionAPI provides enterprise-grade hierarchical key management for secure blockchain operations. This guide covers key generation, storage, rotation, and recovery best practices.

---

## ⚠️ SECURITY UPDATE (v1.0.6)

> **IMPORTANT**: All key management operations now require **pre-authorization**. You must initialize the API with genesis admin credentials before creating or managing keys.

### Secure Initialization Required

Before using any key management features, initialize the API:

> **🔑 PREREQUISITE**: Generate genesis-admin keys first:
> ```bash
> ./tools/generate_genesis_keys.zsh
> ```
> This creates `./keys/genesis-admin.*` required for all key management operations. **Backup securely!**

```java
// 1. Create blockchain (only genesis block is automatic)
Blockchain blockchain = new Blockchain();

// 2. Load genesis admin keys (generated via ./tools/generate_genesis_keys.zsh)
KeyPair genesisKeys = KeyFileLoader.loadKeyPairFromFiles(
    "./keys/genesis-admin.private",
    "./keys/genesis-admin.public"
);

// 3. Register bootstrap admin in blockchain (REQUIRED!)
blockchain.createBootstrapAdmin(
    CryptoUtil.publicKeyToString(genesisKeys.getPublic()),
    "BOOTSTRAP_ADMIN"
);

// 4. Create API with genesis admin credentials
UserFriendlyEncryptionAPI api = new UserFriendlyEncryptionAPI(blockchain);
api.setDefaultCredentials("BOOTSTRAP_ADMIN", genesisKeys);

// 5. Now you can create and manage keys
KeyPair userKeys = api.createUser("username");
```

> **⚠️ NOTE**: All code examples in this guide assume you have already initialized the API with authorized credentials. See [API_GUIDE.md](../reference/API_GUIDE.md#-secure-initialization--authorization) for complete initialization details.

---

## 🔒 RBAC FOR HIERARCHICAL KEYS (v1.0.6)

> **CRITICAL SECURITY UPDATE**: v1.0.6 introduces mandatory Role-Based Access Control (RBAC) for hierarchical key operations.

### Permission Matrix

Hierarchical key operations now enforce strict role-based permissions:

| Operation | SUPER_ADMIN | ADMIN | USER | READ_ONLY |
|-----------|:-----------:|:-----:|:----:|:---------:|
| `setupHierarchicalKeys()` | ✅ | ❌ | ❌ | ❌ |
| Create ROOT keys (depth=1) | ✅ | ❌ | ❌ | ❌ |
| Create INTERMEDIATE keys (depth=2) | ✅ | ✅ | ❌ | ❌ |
| Create OPERATIONAL keys (depth=3+) | ✅ | ✅ | ✅ | ❌ |
| Rotate ROOT keys | ✅ | ❌ | ❌ | ❌ |
| Rotate INTERMEDIATE keys | ✅ | ✅ | ❌ | ❌ |
| Rotate OPERATIONAL keys | ✅ | ✅ | ✅ | ❌ |

### Security Enforcement

**All hierarchical key methods now throw `SecurityException` for unauthorized access**:

```java
// ❌ This will throw SecurityException if caller is not SUPER_ADMIN:
UserFriendlyEncryptionAPI userApi = new UserFriendlyEncryptionAPI(blockchain, "regularUser", userKeys);
try {
    KeyManagementResult result = userApi.setupHierarchicalKeys("password");
} catch (SecurityException e) {
    // Expected: "❌ PERMISSION DENIED: This operation requires ... SUPER_ADMIN"
    System.err.println(e.getMessage());
}

// ✅ Correct: Use SUPER_ADMIN credentials
UserFriendlyEncryptionAPI adminApi = new UserFriendlyEncryptionAPI(blockchain, "superadmin", superAdminKeys);
KeyManagementResult result = adminApi.setupHierarchicalKeys("password");  // Success!
```

### Breaking Changes in v1.0.6

⚠️ **IMPORTANT**: If you upgrade from pre-v1.0.6, you **must** use appropriate role credentials:

1. **ROOT key operations** → SUPER_ADMIN credentials required
2. **INTERMEDIATE key operations** → SUPER_ADMIN or ADMIN credentials required
3. **OPERATIONAL key operations** → Any authorized user (USER or higher)

### Vulnerability Report

Six critical vulnerabilities were fixed in v1.0.6 that allowed unauthorized key creation:
- CVE-2025-001 through CVE-2025-006

See [VULNERABILITY_REPORT_HIERARCHICAL_KEY_RBAC.md](VULNERABILITY_REPORT_HIERARCHICAL_KEY_RBAC.md) for complete details.

---

## 🏗️ Key Architecture

### Hierarchical Key Structure

The API implements a three-tier key hierarchy for maximum security:

```
🔐 Root Key (Master)
├── 🔑 Intermediate Keys (Category-specific)
│   ├── 🔓 Operational Keys (Day-to-day operations)
│   ├── 🔓 Backup Keys (Recovery purposes)
│   └── 🔓 Derived Keys (Specific functions)
└── 🔑 Emergency Keys (Break-glass access)
```

### Key Types and Purposes

| Key Type | Purpose | Rotation Frequency | Security Level |
|----------|---------|-------------------|----------------|
| Root Key | Master key derivation | 1-2 years | Highest |
| Intermediate | Category encryption | 6 months | High |
| Operational | Daily operations | 90 days | Medium |
| Emergency | Recovery access | As needed | Highest |

## 🚀 Setting Up Key Management

### Basic Key Generation

```java
// Generate user keys
KeyPair userKeys = api.createUser("username");

// Set as default credentials
api.setDefaultCredentials("username", userKeys);

// Verify setup
boolean hasCredentials = api.hasDefaultCredentials();
System.out.println("✅ Credentials configured: " + hasCredentials);
```

### Hierarchical Key Setup

For enterprise environments requiring advanced key management:

⚠️ **v1.0.6+ SECURITY REQUIREMENT**: Hierarchical key setup requires **SUPER_ADMIN** credentials.

```java
// ✅ STEP 1: Initialize API with SUPER_ADMIN credentials
KeyPair superAdminKeys = KeyFileLoader.loadKeyPairFromFiles(
    "./keys/genesis-admin.private",
    "./keys/genesis-admin.public"
);

// Register bootstrap admin in blockchain (REQUIRED!)
blockchain.createBootstrapAdmin(
    CryptoUtil.publicKeyToString(superAdminKeys.getPublic()),
    "BOOTSTRAP_ADMIN"
);

UserFriendlyEncryptionAPI api = new UserFriendlyEncryptionAPI(blockchain);
api.setDefaultCredentials("BOOTSTRAP_ADMIN", superAdminKeys);

// ✅ STEP 2: Setup hierarchical key management (requires SUPER_ADMIN)
String masterPassword = api.generateValidatedPassword(32, true);

try {
    KeyManagementResult result = api.setupHierarchicalKeys(masterPassword);

    if (result.isSuccess()) {
        System.out.println("✅ Hierarchical keys established");
        System.out.println("🔑 Root Key ID: " + result.getRootKeyId());
        System.out.println("🔑 Intermediate Key ID: " + result.getIntermediateKeyId());
        System.out.println("🔑 Operational Key ID: " + result.getOperationalKeyId());

        // Save key management configuration
        saveKeyConfiguration(result);
    } else {
        System.err.println("❌ Key setup failed: " + result.getErrorMessage());
    }
} catch (SecurityException e) {
    System.err.println("❌ PERMISSION DENIED: " + e.getMessage());
    System.err.println("⚠️  Hierarchical key setup requires SUPER_ADMIN role");
}
```

## 🔐 Secure Key Storage

### Using SecureKeyStorage (AES-256-GCM)

```java
import com.rbatllet.blockchain.security.SecureKeyStorage;

// Save private key with AES-256-GCM authenticated encryption
String username = "medical-staff";
PrivateKey privateKey = userKeys.getPrivate();
String protectionPassword = api.generateValidatedPassword(24, true);

boolean saved = SecureKeyStorage.savePrivateKey(username, privateKey, protectionPassword);
if (saved) {
    System.out.println("✅ Private key saved with AES-256-GCM encryption");
    System.out.println("🔐 Features: Random IV, 128-bit auth tag, SHA-3-256 key derivation");
}

// Load private key when needed (authentication tag verified automatically)
PrivateKey loadedKey = SecureKeyStorage.loadPrivateKey(username, protectionPassword);
if (loadedKey != null) {
    System.out.println("✅ Private key loaded and authenticated successfully");
} else {
    System.out.println("❌ Authentication failed (wrong password or corrupted data)");
}
```

### Key File Management

```java
import com.rbatllet.blockchain.security.KeyFileLoader;

// Save keys to encrypted file
KeyFileLoader.saveKeysToFile(userKeys, "user-keys.dat", protectionPassword);

// Load keys from encrypted file
KeyPair loadedKeys = KeyFileLoader.loadKeysFromFile("user-keys.dat", protectionPassword);

// Verify key integrity
boolean keysMatch = Arrays.equals(
    userKeys.getPrivate().getEncoded(),
    loadedKeys.getPrivate().getEncoded()
);
System.out.println("🔍 Key integrity verified: " + keysMatch);
```

## 🔄 Key Rotation and Lifecycle

### Automated Key Rotation

```java
public class KeyRotationManager {
    private final UserFriendlyEncryptionAPI api;
    private final ScheduledExecutorService scheduler;
    
    public KeyRotationManager(UserFriendlyEncryptionAPI api) {
        this.api = api;
        this.scheduler = Executors.newScheduledThreadPool(2);
    }
    
    public void scheduleKeyRotation() {
        // Rotate operational keys every 90 days
        scheduler.scheduleAtFixedRate(
            this::rotateOperationalKeys, 
            90, 90, TimeUnit.DAYS
        );
        
        // Rotate intermediate keys every 6 months
        scheduler.scheduleAtFixedRate(
            this::rotateIntermediateKeys, 
            180, 180, TimeUnit.DAYS
        );
    }
    
    private void rotateOperationalKeys() {
        try {
            logger.info("🔄 Starting operational key rotation");
            
            // Generate new operational keys
            KeyPair newOperationalKeys = CryptoUtil.generateKeyPair();
            
            // Update key hierarchy
            updateOperationalKeys(newOperationalKeys);
            
            // Re-encrypt recent data with new keys
            reEncryptRecentData(newOperationalKeys);
            
            logger.info("✅ Operational key rotation completed");
            
        } catch (Exception e) {
            logger.error("❌ Key rotation failed", e);
            alertSecurityTeam("Key rotation failure", e);
        }
    }
    
    private void rotateIntermediateKeys() {
        logger.info("🔄 Starting intermediate key rotation");
        
        // This is a more complex operation requiring careful coordination
        String newMasterPassword = api.generateValidatedPassword(32, false);
        KeyManagementResult rotationResult = api.setupHierarchicalKeys(newMasterPassword);
        
        if (rotationResult.isSuccess()) {
            // Migrate encrypted data to new key hierarchy
            migrateDataToNewKeys(rotationResult);
            logger.info("✅ Intermediate key rotation completed");
        } else {
            logger.error("❌ Intermediate key rotation failed");
        }
    }
}
```

### Manual Key Rotation

```java
public class ManualKeyRotation {
    
    public void rotateUserKeys(String username) {
        // Generate new key pair
        KeyPair newKeys = api.createUser(username + "-v2");
        
        // Backup current data with old keys
        List<Block> userBlocks = findUserBlocks(username);
        backupUserData(userBlocks);
        
        // Re-encrypt data with new keys
        for (Block block : userBlocks) {
            String data = api.retrieveSecret(block.getId(), oldPassword);
            if (data != null) {
                // Store with new keys
                api.storeDataWithIdentifier(data, newPassword, 
                    "MIGRATED-" + block.getId());
            }
        }
        
        // Update user credentials
        api.setDefaultCredentials(username, newKeys);
        
        // Archive old keys securely
        archiveOldKeys(username, userBlocks);
        
        System.out.println("✅ User key rotation completed for: " + username);
    }
}
```

## 🛡️ Category-Specific Key Management (ML-DSA-87)

> **⚠️ IMPORTANT**: ML-DSA-87 (post-quantum) does NOT support public key derivation from private keys. Always generate complete KeyPairs and save both public + private keys together.

### Category-Specific Keys with ML-DSA-87

With ML-DSA-87, each category requires independent key pair generation (no derivation):

```java
import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.security.SecureKeyStorage;

public class CategoryKeyManager {
    private final UserFriendlyEncryptionAPI api;

    public CategoryKeyManager(UserFriendlyEncryptionAPI api) {
        this.api = api;
    }

    public void setupCategoryKeys() {
        // Generate independent ML-DSA-87 key pairs for each category
        // (No key derivation with post-quantum crypto)

        // Medical data keys (ML-DSA-87)
        KeyPair medicalKeys = api.createUser("medical-system");
        String medicalPassword = api.generateValidatedPassword(24, true);
        SecureKeyStorage.saveKeyPair("medical-system", medicalKeys, medicalPassword);

        // Financial data keys (ML-DSA-87)
        KeyPair financialKeys = api.createUser("financial-system");
        String financialPassword = api.generateValidatedPassword(24, true);
        SecureKeyStorage.saveKeyPair("financial-system", financialKeys, financialPassword);

        // Research data keys (ML-DSA-87)
        KeyPair researchKeys = api.createUser("research-system");
        String researchPassword = api.generateValidatedPassword(24, true);
        SecureKeyStorage.saveKeyPair("research-system", researchKeys, researchPassword);

        // Store category mappings with independent passwords
        saveCategoryMapping("medical", medicalPassword);
        saveCategoryMapping("financial", financialPassword);
        saveCategoryMapping("research", researchPassword);
    }

    private void saveCategoryMapping(String category, String password) {
        // Store category→password mapping securely
        // (Implementation depends on your secure configuration storage)
    }
}
```

### Password Derivation for Categories (Separate from Key Pairs)

While ML-DSA-87 keys cannot be derived, you can still derive **passwords** from a master password:

```java
import com.rbatllet.blockchain.util.CryptoUtil;

public class PasswordDerivationManager {

    /**
     * Derive category-specific passwords from master password
     * (Note: This derives PASSWORDS, not cryptographic keys)
     */
    public String deriveCategoryPassword(String masterPassword, String category) {
        // Use CryptoUtil password derivation (PBKDF2 with SHA3-256)
        String salt = "category-" + category;
        return CryptoUtil.deriveKeyFromPassword(masterPassword, salt);
    }

    public Map<String, String> deriveMultiplePasswords(String masterPassword, String[] categories) {
        Map<String, String> derivedPasswords = new HashMap<>();

        for (String category : categories) {
            String password = deriveCategoryPassword(masterPassword, category);
            derivedPasswords.put(category, password);
        }

        return derivedPasswords;
    }

    public String deriveTimeBasedPassword(String masterPassword) {
        // Create time-based password that changes daily
        LocalDate today = LocalDate.now();
        String timeSalt = "daily-" + today.toString();

        return CryptoUtil.deriveKeyFromPassword(masterPassword, timeSalt);
    }

    public String deriveUserSpecificPassword(String masterPassword, String userId, String purpose) {
        // Create user and purpose specific password
        String salt = "user-" + userId + "-" + purpose;
        return CryptoUtil.deriveKeyFromPassword(masterPassword, salt);
    }
}
```

### Complete Category Setup Example

```java
public class CompleteCategorySetup {

    public void setupCategoriesWithDerivedPasswords(String masterPassword) {
        // Derive category-specific passwords from master password
        PasswordDerivationManager passwordManager = new PasswordDerivationManager();

        String medicalPassword = passwordManager.deriveCategoryPassword(masterPassword, "medical");
        String financialPassword = passwordManager.deriveCategoryPassword(masterPassword, "financial");
        String researchPassword = passwordManager.deriveCategoryPassword(masterPassword, "research");

        // Generate independent ML-DSA-87 key pairs (NOT derived)
        KeyPair medicalKeys = api.createUser("medical-system");
        KeyPair financialKeys = api.createUser("financial-system");
        KeyPair researchKeys = api.createUser("research-system");

        // Save keys with derived passwords
        SecureKeyStorage.saveKeyPair("medical-system", medicalKeys, medicalPassword);
        SecureKeyStorage.saveKeyPair("financial-system", financialKeys, financialPassword);
        SecureKeyStorage.saveKeyPair("research-system", researchKeys, researchPassword);

        System.out.println("✅ Category keys generated (ML-DSA-87)");
        System.out.println("✅ Passwords derived from master password");
        System.out.println("⚠️  Remember: Keys are NOT derived (ML-DSA-87 limitation)");
    }
}
```

## 🚨 Emergency Key Management

### Emergency Access Keys

```java
public class EmergencyKeyManager {
    
    public void setupEmergencyAccess() {
        // Create emergency break-glass keys
        KeyPair emergencyKeys = api.createUser("emergency-access");
        String emergencyPassword = api.generateValidatedPassword(40, true);
        
        // Store in secure offline location
        storeEmergencyKeys(emergencyKeys, emergencyPassword);
        
        // Create emergency recovery data
        EmergencyRecoveryData recoveryData = new EmergencyRecoveryData(
            emergencyKeys.getPublic(),
            getCurrentKeyHierarchy(),
            System.currentTimeMillis()
        );
        
        saveEmergencyRecoveryData(recoveryData);
        
        System.out.println("🚨 Emergency access configured");
    }
    
    public boolean executeEmergencyRecovery(String emergencyPassword) {
        logger.warn("🚨 EMERGENCY RECOVERY INITIATED");
        
        try {
            // Load emergency keys
            KeyPair emergencyKeys = loadEmergencyKeys(emergencyPassword);
            
            // Validate emergency access authorization
            if (!validateEmergencyAccess()) {
                logger.error("❌ Emergency access not authorized");
                return false;
            }
            
            // Create emergency API instance
            UserFriendlyEncryptionAPI emergencyAPI = new UserFriendlyEncryptionAPI(
                blockchain, "emergency-user", emergencyKeys);
            
            // Perform recovery operations
            List<Block> criticalData = emergencyAPI.searchEverythingWithPassword(
                "critical emergency", emergencyPassword);
            
            // Log emergency access
            auditEmergencyAccess(emergencyKeys.getPublic());
            
            logger.info("✅ Emergency recovery completed");
            return true;
            
        } catch (Exception e) {
            logger.error("❌ Emergency recovery failed", e);
            return false;
        }
    }
}
```

## 🔍 Key Validation and Health Monitoring

### Key Health Checks

```java
public class KeyHealthMonitor {
    
    public void performKeyHealthCheck() {
        System.out.println("🔍 Performing key health check...");
        
        // Check default credentials
        checkDefaultCredentials();
        
        // Validate key strength
        validateKeyStrength();
        
        // Check key expiration
        checkKeyExpiration();
        
        // Verify key integrity
        verifyKeyIntegrity();
        
        // Generate health report
        generateKeyHealthReport();
    }
    
    private void checkDefaultCredentials() {
        if (!api.hasDefaultCredentials()) {
            logger.warn("⚠️  No default credentials configured");
            return;
        }
        
        try {
            // Test key functionality
            String testData = "key-health-test-" + System.currentTimeMillis();
            String testPassword = api.generateValidatedPassword(16, false);
            
            Block testBlock = api.storeSecret(testData, testPassword);
            String retrieved = api.retrieveSecret(testBlock.getId(), testPassword);
            
            if (testData.equals(retrieved)) {
                logger.info("✅ Default credentials working correctly");
            } else {
                logger.error("❌ Default credentials test failed");
            }
            
        } catch (Exception e) {
            logger.error("❌ Default credentials validation failed", e);
        }
    }
    
    private void validateKeyStrength() {
        // Use API's built-in key validation
        Map<String, Object> keyOptions = Map.of(
            "checkKeyStrength", true,
            "validateAuthorization", true,
            "checkKeyExpiration", true
        );
        
        ValidationReport keyReport = api.validateKeyManagement(keyOptions);
        
        if (keyReport.isValid()) {
            logger.info("✅ Key strength validation passed");
        } else {
            logger.warn("⚠️  Key strength issues detected: " + keyReport.getIssues());
        }
    }
}
```

### Key Audit and Compliance

```java
public class KeyAuditManager {
    
    public void generateKeyAuditReport() {
        System.out.println("📊 Generating key audit report...");
        
        KeyAuditReport report = new KeyAuditReport();
        
        // Collect key usage statistics
        report.setTotalKeys(countManagedKeys());
        report.setActiveKeys(countActiveKeys());
        report.setExpiredKeys(countExpiredKeys());
        report.setLastRotationDate(getLastRotationDate());
        
        // Check compliance with policies
        report.setCompliant(checkComplianceStatus());
        report.setComplianceIssues(getComplianceIssues());
        
        // Generate recommendations
        report.setRecommendations(generateRecommendations());
        
        // Save and log report
        saveAuditReport(report);
        logAuditSummary(report);
        
        System.out.println("📋 Key audit report generated");
    }
    
    private List<String> generateRecommendations() {
        List<String> recommendations = new ArrayList<>();
        
        if (getLastRotationDate().isBefore(LocalDateTime.now().minusDays(90))) {
            recommendations.add("Consider rotating operational keys (last rotation > 90 days)");
        }
        
        if (countExpiredKeys() > 0) {
            recommendations.add("Remove or renew expired keys");
        }
        
        if (!isEmergencyAccessConfigured()) {
            recommendations.add("Configure emergency access procedures");
        }
        
        return recommendations;
    }
}
```

## 💾 Key Backup and Recovery

### Automated Key Backup

```java
public class KeyBackupManager {
    
    public void createKeyBackup() {
        logger.info("💾 Creating key backup...");
        
        // Collect all managed keys
        Map<String, KeyPair> managedKeys = collectManagedKeys();
        
        // Create encrypted backup
        String backupPassword = api.generateValidatedPassword(32, true);
        EncryptedKeyBackup backup = createEncryptedBackup(managedKeys, backupPassword);
        
        // Store backup securely
        storeBackupSecurely(backup);
        
        // Create recovery instructions
        RecoveryInstructions instructions = new RecoveryInstructions(
            backup.getBackupId(),
            backup.getCreationTime(),
            managedKeys.keySet()
        );
        
        saveRecoveryInstructions(instructions);
        
        logger.info("✅ Key backup completed - ID: " + backup.getBackupId());
    }
    
    public boolean restoreFromBackup(String backupId, String backupPassword) {
        logger.info("🔄 Restoring keys from backup: " + backupId);
        
        try {
            // Load backup
            EncryptedKeyBackup backup = loadBackup(backupId);
            
            // Decrypt and restore keys
            Map<String, KeyPair> restoredKeys = decryptBackup(backup, backupPassword);
            
            // Validate restored keys
            if (!validateRestoredKeys(restoredKeys)) {
                logger.error("❌ Restored keys validation failed");
                return false;
            }
            
            // Restore keys to system
            for (Map.Entry<String, KeyPair> entry : restoredKeys.entrySet()) {
                restoreUserKeys(entry.getKey(), entry.getValue());
            }
            
            logger.info("✅ Key restoration completed");
            return true;
            
        } catch (Exception e) {
            logger.error("❌ Key restoration failed", e);
            return false;
        }
    }
}
```

## 🔧 Configuration and Best Practices

### Production Key Management Configuration

```java
public class ProductionKeyConfig {
    
    public static void configureProductionKeys() {
        // Load configuration from secure environment
        String keystorePath = System.getenv("BLOCKCHAIN_KEYSTORE_PATH");
        String masterPassword = System.getenv("BLOCKCHAIN_MASTER_PASSWORD");
        
        if (keystorePath == null || masterPassword == null) {
            throw new SecurityException("Missing required key configuration");
        }
        
        // Initialize key management with production settings
        KeyManagementConfig config = new KeyManagementConfig();
        config.setKeystorePath(keystorePath);
        config.setRotationPolicy(RotationPolicy.AGGRESSIVE); // 90-day rotation
        config.setBackupEnabled(true);
        config.setAuditLogging(true);
        config.setEmergencyAccess(true);
        
        // Apply configuration
        applyKeyConfiguration(config);
        
        System.out.println("🔐 Production key management configured");
    }
}
```

### Key Management Best Practices Checklist

- [ ] **Master passwords** use `generateValidatedPassword()` with 32+ characters
- [ ] **Key rotation** scheduled appropriately (90 days operational, 6 months intermediate)
- [ ] **Emergency access** configured with offline secure storage
- [ ] **Backup procedures** automated and tested regularly
- [ ] **Audit logging** enabled for all key operations
- [ ] **Key validation** performed regularly via health checks
- [ ] **Category separation** implemented for different data types
- [ ] **Compliance monitoring** configured for regulatory requirements
- [ ] **Recovery procedures** documented and tested
- [ ] **Access controls** implemented with role-based permissions

## 📚 Additional Resources

- [Getting Started Guide](../getting-started/GETTING_STARTED.md) - Basic key setup and usage examples
- [Security Best Practices](SECURITY_GUIDE.md) - Overall security guidelines and policies
- [Advanced Search Guide](../search/USER_FRIENDLY_SEARCH_GUIDE.md) - Using keys for search operations and access control
- [Troubleshooting Guide](../getting-started/TROUBLESHOOTING_GUIDE.md) - Key management issues and solutions
- [API Reference](../reference/API_GUIDE.md) - Complete key management methods documentation
- [Examples Guide](../getting-started/EXAMPLES.md) - Real-world key management scenarios
- [Technical Details](../reference/TECHNICAL_DETAILS.md) - Key management architecture and cryptography
