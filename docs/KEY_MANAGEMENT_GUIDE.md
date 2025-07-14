# Key Management Guide

## 🔑 Overview

The UserFriendlyEncryptionAPI provides enterprise-grade hierarchical key management for secure blockchain operations. This guide covers key generation, storage, rotation, and recovery best practices.

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

```java
// Setup hierarchical key management
String masterPassword = api.generateValidatedPassword(32, true);
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
```

## 🔐 Secure Key Storage

### Using SecureKeyStorage

```java
import com.rbatllet.blockchain.security.SecureKeyStorage;

// Save private key with strong protection
String username = "medical-staff";
PrivateKey privateKey = userKeys.getPrivate();
String protectionPassword = api.generateValidatedPassword(24, true);

boolean saved = SecureKeyStorage.savePrivateKey(username, privateKey, protectionPassword);
if (saved) {
    System.out.println("✅ Private key saved securely");
}

// Load private key when needed
PrivateKey loadedKey = SecureKeyStorage.loadPrivateKey(username, protectionPassword);
if (loadedKey != null) {
    System.out.println("✅ Private key loaded successfully");
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

## 🛡️ Key Derivation and Categories

### Category-Specific Keys

```java
import com.rbatllet.blockchain.security.ECKeyDerivation;

public class CategoryKeyManager {
    private final ECKeyDerivation keyDerivation;
    
    public CategoryKeyManager() {
        this.keyDerivation = new ECKeyDerivation();
    }
    
    public String deriveCategoryPassword(String masterPassword, String category) {
        // Derive category-specific passwords from master password
        return keyDerivation.derivePassword(masterPassword, category);
    }
    
    public void setupCategoryKeys(String masterPassword) {
        // Medical data keys
        String medicalPassword = deriveCategoryPassword(masterPassword, "medical");
        KeyPair medicalKeys = api.createUser("medical-system");
        
        // Financial data keys
        String financialPassword = deriveCategoryPassword(masterPassword, "financial");
        KeyPair financialKeys = api.createUser("financial-system");
        
        // Research data keys
        String researchPassword = deriveCategoryPassword(masterPassword, "research");
        KeyPair researchKeys = api.createUser("research-system");
        
        // Store category mappings
        saveCategoryMapping("medical", medicalKeys, medicalPassword);
        saveCategoryMapping("financial", financialKeys, financialPassword);
        saveCategoryMapping("research", researchKeys, researchPassword);
    }
}
```

### Advanced Key Derivation

```java
public class AdvancedKeyDerivation {
    
    public Map<String, String> deriveMultipleKeys(String masterPassword, int count) {
        Map<String, String> derivedKeys = new HashMap<>();
        
        for (int i = 0; i < count; i++) {
            String keyId = "key-" + (i + 1);
            String derivedKey = keyDerivation.deriveKey(masterPassword, keyId, 256);
            derivedKeys.put(keyId, derivedKey);
        }
        
        return derivedKeys;
    }
    
    public String deriveTimeBasedKey(String masterPassword) {
        // Create time-based key that changes daily
        LocalDate today = LocalDate.now();
        String timeSalt = today.toString();
        
        return keyDerivation.deriveKey(masterPassword, timeSalt, 256);
    }
    
    public String deriveUserSpecificKey(String masterPassword, String userId, String purpose) {
        // Create user and purpose specific key
        String derivationInput = userId + ":" + purpose;
        return keyDerivation.deriveKey(masterPassword, derivationInput, 256);
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

- [Getting Started Guide](GETTING_STARTED.md) - Basic key setup and usage examples
- [Security Best Practices](SECURITY_GUIDE.md) - Overall security guidelines and policies
- [Advanced Search Guide](USER_FRIENDLY_SEARCH_GUIDE.md) - Using keys for search operations and access control
- [Troubleshooting Guide](TROUBLESHOOTING_GUIDE.md) - Key management issues and solutions
- [API Reference](API_GUIDE.md) - Complete key management methods documentation
- [Examples Guide](EXAMPLES.md) - Real-world key management scenarios
- [Technical Details](TECHNICAL_DETAILS.md) - Key management architecture and cryptography