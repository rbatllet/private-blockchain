# Security Best Practices Guide

## 🛡️ Overview

This guide covers security best practices for using UserFriendlyEncryptionAPI in production environments. Follow these guidelines to ensure maximum security for your blockchain applications.

## 🔐 Password Security

### Password Requirements
The API enforces strict password policies with built-in validation:

```java
// ✅ GOOD: Strong passwords
String strongPassword = api.generateValidatedPassword(16, false);  // 16 chars, cryptographically secure
String customStrong = "MySecure!Pass123$";  // 8+ chars with special characters

// ❌ BAD: Weak passwords (will throw IllegalArgumentException)
api.storeSecret(data, "weak");      // Too short (< 8 chars)
api.storeSecret(data, "");          // Empty
api.storeSecret(data, null);        // Null
```

### Secure Password Generation
Always use the API's secure password generator:

```java
// For automated systems
String systemPassword = api.generateValidatedPassword(24, false);

// For interactive users (with confirmation)
String userPassword = api.generateValidatedPassword(16, true);

// For high-security applications
String highSecPassword = api.generateValidatedPassword(32, false);
```

### Password Storage
**Never store passwords in plain text**:

```java
// ✅ CORRECT: Use SecureKeyStorage for keys
SecureKeyStorage.savePrivateKey("user-id", privateKey, "userMasterPassword");

// ❌ WRONG: Never store passwords in code
String password = "hardcoded123!"; // Security vulnerability!

// ✅ CORRECT: Load from secure configuration
String password = System.getenv("BLOCKCHAIN_PASSWORD");
if (password == null) {
    password = api.generateValidatedPassword(16, true);
}
```

## 🔒 Data Classification and Protection

### Classify Your Data
Different data types require different security levels:

```java
// HIGH SECURITY: Medical/Financial Data
EncryptionConfig highSecurity = api.getHighSecurityConfig();
String medicalPassword = api.generateValidatedPassword(24, false);

// STANDARD SECURITY: General Business Data
EncryptionConfig standard = api.getDefaultEncryptionConfig();
String businessPassword = api.generateValidatedPassword(16, false);

// PERFORMANCE OPTIMIZED: Non-sensitive operational data
EncryptionConfig performance = api.getPerformanceConfig();
```

### Data Size Validation
The API includes DoS protection with automatic validation:

```java
// Automatic validation (50MB limit)
try {
    Block block = api.storeSecret(largeData, password);
} catch (IllegalArgumentException e) {
    if (e.getMessage().contains("DoS protection")) {
        // Use off-chain storage for large data
        OffChainData offChain = api.storeLargeFileSecurely(
            largeData.getBytes(), password, "text/plain");
    }
}
```

## 👤 User Management Security

### Secure User Creation
```java
// ✅ SECURE: Proper user creation with validation
try {
    String username = "medical-user-001";  // Max 256 characters
    KeyPair userKeys = api.createUser(username);
    
    // Save keys with strong master password
    String masterPassword = api.generateValidatedPassword(20, true);
    SecureKeyStorage.savePrivateKey(username, userKeys.getPrivate(), masterPassword);
    
} catch (IllegalArgumentException e) {
    logger.error("User creation failed: {}", e.getMessage());
}

// ❌ INSECURE: Weak user management
KeyPair keys = api.createUser(""); // Empty username - will fail
api.setDefaultCredentials("user", null); // Null keypair - will fail
```

### Credential Management
```java
// ✅ SECURE: Load credentials securely
boolean loaded = api.loadUserCredentials("username", "strongMasterPassword");
if (!loaded) {
    logger.warn("Failed to load user credentials - check password");
    return;
}

// ✅ SECURE: Verify credentials are set
if (!api.hasDefaultCredentials()) {
    throw new SecurityException("No valid credentials available");
}

// ✅ SECURE: Use specific users for sensitive operations
UserFriendlyEncryptionAPI medicalAPI = new UserFriendlyEncryptionAPI(
    blockchain, "medical-staff", medicalKeys);
UserFriendlyEncryptionAPI publicAPI = new UserFriendlyEncryptionAPI(
    blockchain, "public-user", publicKeys);
```

## 🔍 Secure Search Practices

### Privacy-Preserving Search
```java
// Two-tier search strategy
String[] searchTerms = {"patient", "diabetes"};

// Public search (no sensitive data exposed)
List<Block> publicResults = api.searchByTerms(searchTerms, null, 10);

// Private search (with password for sensitive data)
List<Block> privateResults = api.searchByTerms(searchTerms, password, 10);

// Combined search with access control
List<Block> secureResults = api.searchEverythingWithPassword(
    "medical records", medicalPassword);
```

### Search Result Protection
```java
// ✅ SECURE: Filter results based on user permissions
List<Block> results = api.searchByTerms(terms, password, maxResults);
List<Block> filteredResults = results.stream()
    .filter(block -> userHasAccess(block, currentUser))
    .collect(Collectors.toList());

// ✅ SECURE: Sanitize search results for logging
String sanitizedQuery = query.replaceAll("[^\\w\\s]", ""); // Remove special chars
logger.info("Search performed: {}", sanitizedQuery);
```

## 🔐 Encryption Best Practices

### Critical Security Fix: Encrypted Block Validation

**⚠️ SECURITY VULNERABILITY RESOLVED** (September 2025)

A critical security vulnerability in encrypted block validation was identified and resolved:

**Issue**: The `validateBlock()` method was using standard content building (`buildBlockContent()`) for encrypted blocks instead of encryption-aware content building (`buildBlockContentForEncrypted()`). This caused corrupted encrypted block data to pass validation incorrectly.

**Impact**: 
- Corrupted encrypted blocks could be accepted as valid
- Data integrity violations in encrypted content went undetected
- Security breaches could occur without detection

**Resolution**: Enhanced `validateBlock()` method to:
```java
// ✅ FIXED: Proper validation for encrypted blocks
public boolean validateBlock(Block block, String publicKey) {
    // ... existing validation logic ...
    
    // 🔒 SECURITY FIX: Use encryption-aware content building for encrypted blocks
    String content;
    if (block.isEncrypted()) {
        content = getContentForHashing(block);  // Encryption-aware content building
    } else {
        content = buildBlockContent(/* ... */); // Standard content building
    }
    
    // Continue with hash validation using proper content
    String calculatedHash = HashUtil.calculateHash(content);
    return calculatedHash.equals(block.getHash());
}
```

**Verification**: The security fix was verified through comprehensive testing:
- `UserFriendlyEncryptionAPIBlockCorruptionTest` now properly detects corrupted encrypted blocks
- Tampered encryption markers (e.g., `[ENCRYPTED]` → `[FNCRYPTFD]`) are correctly identified
- Validation properly fails when encrypted block content is corrupted

**Prevention**: To prevent similar issues:
- Always use encryption-aware methods for encrypted block operations
- Implement comprehensive security testing for all encryption features  
- Regular security audits of validation logic

### Layered Security
```java
// Layer 1: Strong passwords
String primaryPassword = api.generateValidatedPassword(20, false);

// Layer 2: Key derivation for different purposes
String medicalKey = deriveKey(primaryPassword, "medical");
String financialKey = deriveKey(primaryPassword, "financial");

// Layer 3: Category-specific storage
Block medicalBlock = api.storeDataWithIdentifier(
    medicalData, medicalKey, "PATIENT-" + patientId);
Block financialBlock = api.storeDataWithIdentifier(
    financialData, financialKey, "TXN-" + transactionId);
```

### Secure Key Rotation
```java
// Hierarchical key management for enterprise security
String masterPassword = api.generateValidatedPassword(32, true);
KeyManagementResult keySetup = api.setupHierarchicalKeys(masterPassword);

if (keySetup.isSuccess()) {
    logger.info("✅ Hierarchical keys established");
    
    // Schedule key rotation (every 90 days for operational keys)
    scheduleKeyRotation(keySetup.getOperationalKeyId(), 90);
}
```

## 🛡️ Access Control

### Role-Based Security
```java
public class SecureBlockchainAccess {
    private final Map<String, UserFriendlyEncryptionAPI> roleApis;
    
    public SecureBlockchainAccess(Blockchain blockchain) {
        roleApis = new HashMap<>();
        
        // Create role-specific API instances
        roleApis.put("ADMIN", new UserFriendlyEncryptionAPI(
            blockchain, "admin", loadAdminKeys()));
        roleApis.put("MEDICAL", new UserFriendlyEncryptionAPI(
            blockchain, "medical-staff", loadMedicalKeys()));
        roleApis.put("FINANCIAL", new UserFriendlyEncryptionAPI(
            blockchain, "finance-team", loadFinanceKeys()));
        roleApis.put("PUBLIC", new UserFriendlyEncryptionAPI(
            blockchain, "public-user", loadPublicKeys()));
    }
    
    public Block storeSecureData(String role, String data, String category) {
        UserFriendlyEncryptionAPI api = roleApis.get(role);
        if (api == null) {
            throw new SecurityException("Invalid role: " + role);
        }
        
        String password = generateRolePassword(role, category);
        return api.storeDataWithIdentifier(data, password, generateIdentifier(category));
    }
}
```

## 📊 Security Monitoring

### Audit Logging
```java
// ✅ SECURE: Log security events without sensitive data
public void logSecurityEvent(String operation, String user, boolean success) {
    if (success) {
        logger.info("Security event: {} performed by {} - SUCCESS", operation, user);
    } else {
        logger.warn("Security event: {} attempted by {} - FAILED", operation, user);
    }
}

// ✅ SECURE: Monitor for suspicious activity
public void monitorAccess(String user, String operation) {
    if (isFailedAttempt(operation)) {
        failedAttempts.computeIfAbsent(user, k -> new AtomicInteger(0)).incrementAndGet();
        
        if (failedAttempts.get(user).get() > MAX_FAILED_ATTEMPTS) {
            logger.error("Security alert: Multiple failed attempts by user: {}", user);
            blockUser(user);
        }
    }
}
```

### Validation and Health Checks
```java
// Regular security validation
public void performSecurityValidation() {
    // Comprehensive validation
    ValidationReport report = api.performComprehensiveValidation();
    
    if (!report.isValid()) {
        logger.error("❌ Security validation failed: {}", report.getIssues());
        alertSecurityTeam(report);
    }
    
    // Key management validation
    Map<String, Object> keyOptions = Map.of(
        "checkKeyStrength", true,
        "validateAuthorization", true,
        "checkKeyExpiration", true
    );
    
    ValidationReport keyReport = api.validateKeyManagement(keyOptions);
    if (!keyReport.isValid()) {
        logger.error("❌ Key management validation failed");
        scheduleKeyRotation();
    }
}
```

## 🚨 Security Incident Response

### Error Handling
```java
// ✅ SECURE: Handle security errors properly
try {
    Block block = api.storeSecret(sensitiveData, password);
} catch (IllegalArgumentException e) {
    if (e.getMessage().contains("DoS protection")) {
        logger.warn("DoS protection triggered for user: {}", getCurrentUser());
        // Handle oversized data appropriately
    } else if (e.getMessage().contains("Password")) {
        logger.warn("Invalid password attempt by user: {}", getCurrentUser());
        // Do not log the actual password
    }
} catch (SecurityException e) {
    logger.error("Security violation: {}", e.getMessage());
    alertSecurityTeam(e);
}

// ❌ INSECURE: Never log sensitive data
logger.error("Failed with password: {}", password); // Security vulnerability!
```

### Recovery Procedures
```java
// Create recovery checkpoints before sensitive operations
RecoveryCheckpoint checkpoint = api.createRecoveryCheckpoint(
    RecoveryCheckpoint.CheckpointType.EMERGENCY,
    "Before critical system update");

try {
    performCriticalOperation();
} catch (Exception e) {
    logger.error("Critical operation failed, initiating recovery");
    
    // Recover from checkpoint if needed
    ChainRecoveryResult recovery = api.recoverFromCheckpoint(checkpoint.getCheckpointId());
    if (recovery.isSuccess()) {
        logger.info("✅ Successfully recovered from checkpoint");
    }
}
```

## 🔒 Production Deployment Security

### Environment Configuration
```java
// ✅ SECURE: Environment-based configuration
public class ProductionSecurityConfig {
    public static UserFriendlyEncryptionAPI createSecureAPI() {
        // Load from secure environment variables
        String masterPassword = System.getenv("BLOCKCHAIN_MASTER_PASSWORD");
        String keystorePath = System.getenv("BLOCKCHAIN_KEYSTORE_PATH");
        
        if (masterPassword == null || keystorePath == null) {
            throw new SecurityException("Missing required security configuration");
        }
        
        // Use high-security configuration for production
        Blockchain blockchain = new Blockchain();
        KeyPair keys = loadProductionKeys(keystorePath, masterPassword);
        
        return new UserFriendlyEncryptionAPI(blockchain, "production-user", keys);
    }
}
```

### Security Checklist
Before deploying to production:

- [ ] All passwords use `generateValidatedPassword()`
- [ ] No hardcoded passwords or keys in code
- [ ] Proper error handling without sensitive data exposure
- [ ] Role-based access control implemented
- [ ] Regular security validation scheduled
- [ ] Audit logging configured
- [ ] Recovery procedures tested
- [ ] Key rotation schedule established
- [ ] DoS protection limits appropriate for your use case
- [ ] Off-chain storage configured for large files
- [ ] **Encrypted block validation security fix applied** (validateBlock uses encryption-aware content building)
- [ ] Corruption detection tests pass for encrypted blocks
- [ ] SearchSpecialistAPI initialization warnings resolved

## 📚 Additional Resources

- [Getting Started Guide](GETTING_STARTED.md) - Basic security setup and configuration
- [Key Management Guide](KEY_MANAGEMENT_GUIDE.md) - Enterprise-grade key management
- [Advanced Search Guide](USER_FRIENDLY_SEARCH_GUIDE.md) - Secure search and access control
- [Troubleshooting Guide](TROUBLESHOOTING_GUIDE.md) - Security issue diagnosis and resolution
- [API Reference](API_GUIDE.md) - Complete security features documentation
- [Examples Guide](EXAMPLES.md) - Real-world security implementation patterns
- [Technical Details](TECHNICAL_DETAILS.md) - Security architecture and cryptographic details