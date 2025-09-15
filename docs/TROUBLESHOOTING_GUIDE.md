# Troubleshooting Guide

## 🛠️ Overview

This guide helps you resolve common issues when using the UserFriendlyEncryptionAPI. It covers error diagnosis, solutions, and preventive measures for the most frequent problems.

## 🚨 Common Error Categories

### 1. Authentication and Key Management Issues
### 2. Data Storage and Retrieval Problems
### 3. Search and Query Failures
### 4. Performance and Memory Issues
### 5. Validation and Security Errors

---

## 🔐 Authentication and Key Management Issues

### Problem: "No default credentials available"

**Error Message:**
```
SecurityException: No valid credentials available
IllegalStateException: Default credentials not set
```

**Causes:**
- API initialized without default credentials
- Credentials were cleared or corrupted
- Key loading failed

**Solutions:**

```java
// ✅ Solution 1: Set default credentials
KeyPair userKeys = api.createUser("username");
api.setDefaultCredentials("username", userKeys);

// ✅ Solution 2: Load existing credentials
boolean loaded = api.loadUserCredentials("username", "password");
if (!loaded) {
    // Create new credentials if loading fails
    KeyPair newKeys = api.createUser("username");
    api.setDefaultCredentials("username", newKeys);
}

// ✅ Solution 3: Verify credentials are set
if (!api.hasDefaultCredentials()) {
    throw new IllegalStateException("Must configure default credentials before operations");
}
```

**Prevention:**
```java
// Always verify credentials after initialization
public void initializeAPI() {
    UserFriendlyEncryptionAPI api = new UserFriendlyEncryptionAPI(blockchain);
    
    // Load or create credentials
    if (!api.loadUserCredentials("default-user", "password")) {
        KeyPair keys = api.createUser("default-user");
        api.setDefaultCredentials("default-user", keys);
    }
    
    // Verify setup
    assert api.hasDefaultCredentials() : "Credentials must be configured";
}
```

### Problem: "Key loading failed" or Key corruption

**Error Message:**
```
SecurityException: Failed to load user credentials
IOException: Unable to read key file
InvalidKeyException: Key format not recognized
```

**Diagnosis:**
```java
// Check if key file exists and is readable
File keyFile = new File("path/to/keyfile");
if (!keyFile.exists()) {
    logger.error("❌ Key file does not exist: " + keyFile.getPath());
}
if (!keyFile.canRead()) {
    logger.error("❌ Key file is not readable - check permissions");
}

// Verify key file format
try {
    KeyPair testKeys = KeyFileLoader.loadKeysFromFile(keyFile.getPath(), "password");
    logger.info("✅ Key file format is valid");
} catch (Exception e) {
    logger.error("❌ Key file is corrupted or password is wrong: " + e.getMessage());
}
```

**Solutions:**

```java
// ✅ Solution 1: Restore from backup
try {
    KeyPair backupKeys = loadBackupKeys("backup-path", "backup-password");
    api.setDefaultCredentials("username", backupKeys);
    logger.info("✅ Restored keys from backup");
} catch (Exception e) {
    logger.error("❌ Backup restoration failed", e);
}

// ✅ Solution 2: Emergency key recovery
EmergencyKeyManager emergency = new EmergencyKeyManager();
boolean recovered = emergency.executeEmergencyRecovery("emergency-password");
if (recovered) {
    logger.info("✅ Emergency recovery successful");
} else {
    logger.error("❌ Emergency recovery failed - manual intervention required");
}

// ✅ Solution 3: Generate new keys (data will be inaccessible)
logger.warn("⚠️  Generating new keys - existing encrypted data will be inaccessible");
KeyPair newKeys = api.createUser("username-new");
api.setDefaultCredentials("username-new", newKeys);
```

---

## 💾 Data Storage and Retrieval Problems

### Problem: "DoS protection triggered" - Data too large

**Error Message:**
```
IllegalArgumentException: Data size cannot exceed 50MB (DoS protection)
```

**Solutions:**

```java
// ✅ Solution 1: Use off-chain storage for large data
String largeData = "very large content...";
if (largeData.length() > 10 * 1024 * 1024) { // 10MB threshold
    
    // Store as file with off-chain storage
    OffChainData offChainResult = api.storeLargeFileSecurely(
        largeData.getBytes(), password, "text/plain");
    
    // Store reference in blockchain
    String reference = "offchain:" + offChainResult.getFileId();
    Block referenceBlock = api.storeSecret(reference, password);
    
    logger.info("✅ Large data stored off-chain: " + offChainResult.getFileId());
}

// ✅ Solution 2: Split large data into chunks
public List<Block> storeDataInChunks(String largeData, String password, int chunkSize) {
    List<Block> chunks = new ArrayList<>();
    String chunkId = UUID.randomUUID().toString();
    
    for (int i = 0; i < largeData.length(); i += chunkSize) {
        int end = Math.min(i + chunkSize, largeData.length());
        String chunk = largeData.substring(i, end);
        
        String chunkData = String.format("{\"chunkId\":\"%s\",\"part\":%d,\"data\":\"%s\"}", 
                                       chunkId, i / chunkSize, chunk);
        
        Block chunkBlock = api.storeDataWithIdentifier(chunkData, password, 
                                                      chunkId + "-part-" + (i / chunkSize));
        chunks.add(chunkBlock);
    }
    
    return chunks;
}

// ✅ Solution 3: Compress data before storage
public Block storeCompressedData(String data, String password) throws IOException {
    // Compress data
    ByteArrayOutputStream compressed = new ByteArrayOutputStream();
    try (GZIPOutputStream gzip = new GZIPOutputStream(compressed)) {
        gzip.write(data.getBytes(StandardCharsets.UTF_8));
    }
    
    // Encode as base64 for storage
    String compressedData = Base64.getEncoder().encodeToString(compressed.toByteArray());
    
    // Store with compression marker
    String markedData = "COMPRESSED:" + compressedData;
    return api.storeSecret(markedData, password);
}
```

### Problem: "Cannot retrieve encrypted data" or Decryption failures

**Error Message:**
```
SecurityException: Failed to decrypt block data
IllegalArgumentException: Invalid password or corrupted data
```

**Diagnosis:**
```java
// Check if block exists and is encrypted
public void diagnoseRetrievalIssue(String blockId, String password) {
    Block block = blockchain.getBlockById(blockId);
    
    if (block == null) {
        logger.error("❌ Block not found: " + blockId);
        return;
    }
    
    logger.info("📦 Block found - ID: " + blockId);
    logger.info("🔐 Encrypted: " + block.isDataEncrypted());
    logger.info("📅 Created: " + block.getTimestamp());
    logger.info("💾 Data length: " + block.getData().length());
    
    // Test password validation
    try {
        String testPassword = api.generateValidatedPassword(password.length(), false);
        logger.info("✅ Password format is valid");
    } catch (Exception e) {
        logger.error("❌ Password validation failed: " + e.getMessage());
    }
}
```

**Solutions:**

```java
// ✅ Solution 1: Try password variations
public String tryPasswordVariations(String blockId, String basePassword) {
    String[] variations = {
        basePassword,
        basePassword.toLowerCase(),
        basePassword.toUpperCase(),
        basePassword.trim(),
        basePassword + "!",
        basePassword + "123"
    };
    
    for (String password : variations) {
        try {
            String result = api.retrieveSecret(blockId, password);
            if (result != null) {
                logger.info("✅ Password variation successful: " + password);
                return result;
            }
        } catch (Exception e) {
            logger.debug("Password variation failed: " + password);
        }
    }
    
    logger.error("❌ All password variations failed");
    return null;
}

// ✅ Solution 2: Check for data corruption
public boolean verifyBlockIntegrity(String blockId) {
    try {
        ValidationReport report = api.performComprehensiveValidation();
        
        if (!report.isValid()) {
            logger.error("❌ Blockchain integrity issues: " + report.getIssues());
            return false;
        }
        
        // Additional block-specific validation
        Block block = blockchain.getBlockById(blockId);
        String computedHash = block.calculateHash();
        
        if (!computedHash.equals(block.getHash())) {
            logger.error("❌ Block hash mismatch - data may be corrupted");
            return false;
        }
        
        logger.info("✅ Block integrity verified");
        return true;
        
    } catch (Exception e) {
        logger.error("❌ Integrity verification failed", e);
        return false;
    }
}

// ✅ Solution 3: Recovery from backup or checkpoint
public String recoverDataFromBackup(String blockId) {
    try {
        // Look for recovery checkpoints
        List<RecoveryCheckpoint> checkpoints = api.listRecoveryCheckpoints();
        
        for (RecoveryCheckpoint checkpoint : checkpoints) {
            if (checkpoint.containsBlock(blockId)) {
                logger.info("🔄 Found block in checkpoint: " + checkpoint.getCheckpointId());
                
                ChainRecoveryResult recovery = api.recoverFromCheckpoint(checkpoint.getCheckpointId());
                if (recovery.isSuccess()) {
                    logger.info("✅ Successfully recovered from checkpoint");
                    return api.retrieveSecret(blockId, originalPassword);
                }
            }
        }
        
        logger.warn("⚠️  No valid checkpoints found containing block: " + blockId);
        return null;
        
    } catch (Exception e) {
        logger.error("❌ Recovery attempt failed", e);
        return null;
    }
}
```

---

## 🔍 Search and Query Failures

### Problem: SearchSpecialistAPI returns no results due to initialization order

**Error Symptoms:**
```
Test failure: smartSearchWithPassword should return results (was 0 before bug fix) ==> expected: <true> but was: <false>
```

**Cause:**
SearchSpecialistAPI initialized before calling `initializeAdvancedSearch()` on the blockchain causes blocks to be indexed with "public metadata only" mode, making encrypted keyword searches return empty results.

**Root Cause:**
When `storeSearchableData()` or similar methods are called BEFORE initializing SearchSpecialistAPI properly, the blocks are created without proper encrypted keyword indexing.

**Solutions:**

```java
// ❌ WRONG: Creating SearchSpecialistAPI after storing blocks without initialization
dataAPI.storeSearchableData("medical data", password, keywords);
// Blocks created with "public metadata only" indexing
SearchSpecialistAPI searchAPI = new SearchSpecialistAPI(blockchain, password, privateKey);

// ✅ CORRECT: Initialize SearchSpecialistAPI BEFORE storing searchable data
SearchSpecialistAPI searchAPI = new SearchSpecialistAPI(blockchain, password, privateKey);
// Or ensure initializeAdvancedSearch is called first
blockchain.initializeAdvancedSearch(password);
dataAPI.storeSearchableData("medical data", password, keywords);

// ✅ ALTERNATIVE: Use proper initialization order in tests
@BeforeEach
void setUp() {
    blockchain = new Blockchain();
    dataAPI = new UserFriendlyEncryptionAPI(blockchain);
    
    // CRITICAL: Initialize SearchSpecialistAPI BEFORE storing data
    blockchain.initializeAdvancedSearch(password);
    
    // Now store test data
    dataAPI.storeSearchableData("test data", password, keywords);
}
```

**Prevention:**
Always ensure `initializeAdvancedSearch()` is called or SearchSpecialistAPI constructor is used BEFORE any `storeSearchableData()` operations when you plan to use `smartSearchWithPassword()` methods.

### Problem: "Search returns no results" when data exists

**Causes:**
- Incorrect search terms  
- Missing password for encrypted content
- Index not built or corrupted
- Terms not in searchable metadata
- **NEW:** SearchSpecialistAPI initialization order issue (see above)

**Diagnosis:**
```java
// Debug search issue
public void debugSearchIssue(String[] searchTerms, String password) {
    logger.info("🔍 Debugging search for terms: " + Arrays.toString(searchTerms));
    
    // Check if data exists at all
    boolean hasData = api.hasEncryptedData();
    logger.info("📊 Blockchain has data: " + hasData);
    
    // Try different search approaches
    
    // 1. Search without password (public metadata only)
    List<Block> publicResults = api.searchByTerms(searchTerms, null, 100);
    logger.info("🔓 Public search results: " + publicResults.size());
    
    // 2. Search with password (includes encrypted content)
    List<Block> privateResults = api.searchByTerms(searchTerms, password, 100);
    logger.info("🔐 Private search results: " + privateResults.size());
    
    // 3. Search everything
    List<Block> allResults = api.searchEverything(String.join(" ", searchTerms));
    logger.info("📋 Comprehensive search results: " + allResults.size());
    
    // 4. Check individual terms
    for (String term : searchTerms) {
        List<Block> termResults = api.searchByTerms(new String[]{term}, password, 10);
        logger.info("🔎 Results for '" + term + "': " + termResults.size());
    }
}
```

**Solutions:**

```java
// ✅ Solution 1: Use more flexible search approaches
public List<Block> flexibleSearch(String query, String password) {
    List<Block> results = new ArrayList<>();
    
    // Try exact terms first
    String[] exactTerms = query.toLowerCase().split("\\s+");
    results.addAll(api.searchByTerms(exactTerms, password, 50));
    
    // Try partial matches
    for (String term : exactTerms) {
        if (term.length() > 3) {
            String partialTerm = term.substring(0, term.length() - 1);
            results.addAll(api.searchByTerms(new String[]{partialTerm}, password, 25));
        }
    }
    
    // Try smart search for contextual matching
    results.addAll(api.smartSearch(query, password, 25));
    
    // Remove duplicates
    return results.stream()
        .distinct()
        .collect(Collectors.toList());
}

// ✅ Solution 2: Advanced search with broader criteria
public List<Block> broadSearch(String query, String password) {
    Map<String, Object> criteria = new HashMap<>();
    criteria.put("keywords", query);
    criteria.put("includeEncrypted", true);
    criteria.put("confidenceThreshold", 0.5); // Lower threshold
    criteria.put("maxResults", 200);
    
    // Include all time periods
    criteria.put("startDate", LocalDateTime.of(2020, 1, 1, 0, 0));
    criteria.put("endDate", LocalDateTime.now().plusDays(1));
    
    AdvancedSearchResult result = api.performAdvancedSearch(criteria, password, 200);
    return result.getBlocks();
}

// ✅ Solution 3: Rebuild search indices
public void rebuildSearchIndices() {
    logger.info("🔧 Rebuilding search indices...");
    
    try {
        // Clear existing cache
        api.clearSearchCache();
        
        // Rebuild indices by searching for common terms
        String[] commonTerms = {"data", "test", "patient", "record", "transaction"};
        
        for (String term : commonTerms) {
            api.searchByTerms(new String[]{term}, null, 1);
            Thread.sleep(100); // Allow index building
        }
        
        logger.info("✅ Search indices rebuilt");
        
    } catch (Exception e) {
        logger.error("❌ Failed to rebuild search indices", e);
    }
}
```

### Problem: Search performance is too slow

**Diagnosis:**
```java
// Measure search performance
public void measureSearchPerformance(String query, String password) {
    long startTime = System.currentTimeMillis();
    
    List<Block> results = api.searchByTerms(query.split("\\s+"), password, 50);
    
    long executionTime = System.currentTimeMillis() - startTime;
    
    logger.info("📊 Search Performance Report:");
    logger.info("Query: " + query);
    logger.info("Results: " + results.size());
    logger.info("Execution Time: " + executionTime + "ms");
    
    if (executionTime > 5000) {
        logger.warn("⚠️  Search performance is slow - consider optimization");
    }
}
```

**Solutions:**

```java
// ✅ Solution 1: Enable and configure search caching
public void optimizeSearchPerformance() {
    // Configure search cache
    SearchCacheConfig cacheConfig = new SearchCacheConfig();
    cacheConfig.setEnabled(true);
    cacheConfig.setMaxCacheSize(2000);
    cacheConfig.setCacheTtlMinutes(60);
    
    api.configureSearchCache(cacheConfig);
    
    // Use performance-optimized search
    Map<String, Object> performanceOptions = Map.of(
        "useCache", true,
        "enableParallelSearch", true,
        "maxExecutionTimeMs", 3000
    );
    
    logger.info("✅ Search performance optimization enabled");
}

// ✅ Solution 2: Limit search scope
public List<Block> optimizedSearch(String query, String password) {
    Map<String, Object> criteria = new HashMap<>();
    criteria.put("keywords", query);
    criteria.put("maxResults", 50); // Reasonable limit
    criteria.put("includeEncrypted", true);
    
    // Limit time range to recent data
    criteria.put("startDate", LocalDateTime.now().minusMonths(6));
    
    // Target specific categories if known
    criteria.put("categories", Set.of("medical", "financial"));
    
    AdvancedSearchResult result = api.performAdvancedSearch(criteria, password, 50);
    return result.getBlocks();
}
```

---

## ⚡ Performance and Memory Issues

### Problem: Out of memory errors during operations

**Error Message:**
```
OutOfMemoryError: Java heap space
OutOfMemoryError: GC overhead limit exceeded
```

**Diagnosis:**
```java
// Monitor memory usage
public void monitorMemoryUsage(String operation) {
    Runtime runtime = Runtime.getRuntime();
    
    long beforeMemory = runtime.totalMemory() - runtime.freeMemory();
    
    // Perform operation
    performOperation(operation);
    
    long afterMemory = runtime.totalMemory() - runtime.freeMemory();
    long memoryUsed = afterMemory - beforeMemory;
    
    logger.info("📊 Memory Usage for " + operation + ":");
    logger.info("Before: " + (beforeMemory / 1024 / 1024) + " MB");
    logger.info("After: " + (afterMemory / 1024 / 1024) + " MB");
    logger.info("Used: " + (memoryUsed / 1024 / 1024) + " MB");
    
    if (memoryUsed > 100 * 1024 * 1024) { // 100MB
        logger.warn("⚠️  High memory usage detected");
    }
}
```

**Solutions:**

```java
// ✅ Solution 1: Process data in batches
public void batchProcessLargeDataset(List<String> largeDataset, String password) {
    int batchSize = 100;
    
    for (int i = 0; i < largeDataset.size(); i += batchSize) {
        int end = Math.min(i + batchSize, largeDataset.size());
        List<String> batch = largeDataset.subList(i, end);
        
        // Process batch
        for (String data : batch) {
            api.storeSecret(data, password);
        }
        
        // Force garbage collection between batches
        System.gc();
        
        // Small delay to allow memory cleanup
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            break;
        }
        
        logger.info("📦 Processed batch " + (i / batchSize + 1) + 
                   " of " + ((largeDataset.size() + batchSize - 1) / batchSize));
    }
}

// ✅ Solution 2: Use streaming for large operations
public void streamLargeSearch(String query, String password, Consumer<Block> processor) {
    Map<String, Object> criteria = new HashMap<>();
    criteria.put("keywords", query);
    criteria.put("includeEncrypted", true);
    criteria.put("maxResults", 1000);
    
    // Process results in chunks to avoid loading everything into memory
    int offset = 0;
    int batchSize = 50;
    
    while (true) {
        criteria.put("offset", offset);
        criteria.put("limit", batchSize);
        
        AdvancedSearchResult result = api.performAdvancedSearch(criteria, password, batchSize);
        List<Block> batch = result.getBlocks();
        
        if (batch.isEmpty()) {
            break;
        }
        
        // Process each block individually
        for (Block block : batch) {
            processor.accept(block);
        }
        
        offset += batchSize;
        
        // Clear batch from memory
        batch.clear();
        System.gc();
    }
}

// ✅ Solution 3: Configure JVM memory settings
public void configureMemorySettings() {
    logger.info("💡 Recommended JVM memory settings:");
    logger.info("-Xms2g -Xmx8g");
    logger.info("-XX:+UseG1GC");
    logger.info("-XX:MaxGCPauseMillis=200");
    logger.info("-XX:+HeapDumpOnOutOfMemoryError");
    logger.info("-XX:HeapDumpPath=/tmp/blockchain-heap-dump.hprof");
}
```

---

## 🛡️ Validation and Security Errors

### Problem: Validation failures

**Error Message:**
```
ValidationException: Blockchain validation failed
SecurityException: Invalid signature or tampered data
```

**Diagnosis:**
```java
// Comprehensive validation check
public void diagnoseValidationIssues() {
    logger.info("🔍 Running comprehensive validation...");
    
    try {
        ValidationReport report = api.performComprehensiveValidation();
        
        if (report.isValid()) {
            logger.info("✅ All validation checks passed");
        } else {
            logger.error("❌ Validation issues found:");
            for (String issue : report.getIssues()) {
                logger.error("  - " + issue);
            }
        }
        
        // Additional security validation
        Map<String, Object> securityOptions = Map.of(
            "checkIntegrity", true,
            "validateSignatures", true,
            "checkTimestamps", true
        );
        
        ValidationReport securityReport = api.validateSecurity(securityOptions);
        if (!securityReport.isValid()) {
            logger.error("❌ Security validation failed:");
            securityReport.getIssues().forEach(issue -> logger.error("  - " + issue));
        }
        
    } catch (Exception e) {
        logger.error("❌ Validation check failed", e);
    }
}
```

**Solutions:**

```java
// ✅ Solution 1: Repair blockchain integrity
public boolean repairBlockchainIntegrity() {
    try {
        logger.info("🔧 Attempting blockchain repair...");
        
        // Create recovery checkpoint before repair
        RecoveryCheckpoint checkpoint = api.createRecoveryCheckpoint(
            RecoveryCheckpoint.CheckpointType.EMERGENCY,
            "Pre-repair checkpoint");
        
        // Attempt automated repair
        boolean repaired = blockchain.repairChain();
        
        if (repaired) {
            // Verify repair was successful
            ValidationReport postRepairReport = api.performComprehensiveValidation();
            
            if (postRepairReport.isValid()) {
                logger.info("✅ Blockchain repair successful");
                return true;
            } else {
                logger.warn("⚠️  Repair completed but issues remain");
                return false;
            }
        } else {
            logger.error("❌ Automated repair failed");
            return false;
        }
        
    } catch (Exception e) {
        logger.error("❌ Repair attempt failed", e);
        return false;
    }
}

// ✅ Solution 2: Restore from known good state
public boolean restoreFromCheckpoint() {
    try {
        List<RecoveryCheckpoint> checkpoints = api.listRecoveryCheckpoints();
        
        // Find most recent valid checkpoint
        for (RecoveryCheckpoint checkpoint : checkpoints) {
            if (checkpoint.isValid()) {
                logger.info("🔄 Restoring from checkpoint: " + checkpoint.getCheckpointId());
                
                ChainRecoveryResult recovery = api.recoverFromCheckpoint(checkpoint.getCheckpointId());
                
                if (recovery.isSuccess()) {
                    logger.info("✅ Successfully restored from checkpoint");
                    return true;
                }
            }
        }
        
        logger.error("❌ No valid checkpoints available for restoration");
        return false;
        
    } catch (Exception e) {
        logger.error("❌ Checkpoint restoration failed", e);
        return false;
    }
}
```

---

## 🔧 General Troubleshooting Tools

### Comprehensive System Health Check

```java
public class SystemHealthChecker {
    
    public void performHealthCheck() {
        logger.info("🏥 Performing system health check...");
        
        // 1. API Configuration Check
        checkApiConfiguration();
        
        // 2. Blockchain Health Check
        checkBlockchainHealth();
        
        // 3. Key Management Check
        checkKeyManagement();
        
        // 4. Performance Check
        checkPerformance();
        
        // 5. Security Check
        checkSecurity();
        
        logger.info("🏥 Health check completed");
    }
    
    private void checkApiConfiguration() {
        logger.info("🔧 Checking API configuration...");
        
        try {
            boolean hasCredentials = api.hasDefaultCredentials();
            logger.info("Default credentials: " + (hasCredentials ? "✅ Set" : "❌ Missing"));
            
            String username = api.getDefaultUsername();
            logger.info("Default username: " + (username != null ? "✅ " + username : "❌ Not set"));
            
        } catch (Exception e) {
            logger.error("❌ API configuration check failed", e);
        }
    }
    
    private void checkBlockchainHealth() {
        logger.info("🔗 Checking blockchain health...");
        
        try {
            boolean hasData = api.hasEncryptedData();
            logger.info("Has encrypted data: " + (hasData ? "✅ Yes" : "ℹ️  No"));
            
            String summary = api.getBlockchainSummary();
            logger.info("Blockchain summary: " + summary);
            
            ValidationReport report = api.performComprehensiveValidation();
            logger.info("Validation status: " + (report.isValid() ? "✅ Valid" : "❌ Issues found"));
            
        } catch (Exception e) {
            logger.error("❌ Blockchain health check failed", e);
        }
    }
    
    private void checkKeyManagement() {
        logger.info("🔑 Checking key management...");
        
        try {
            Map<String, Object> keyOptions = Map.of(
                "checkKeyStrength", true,
                "validateAuthorization", true
            );
            
            ValidationReport keyReport = api.validateKeyManagement(keyOptions);
            logger.info("Key validation: " + (keyReport.isValid() ? "✅ Valid" : "❌ Issues found"));
            
        } catch (Exception e) {
            logger.error("❌ Key management check failed", e);
        }
    }
}
```

### Debug Information Collection

```java
public class DebugInfoCollector {
    
    public void collectDebugInfo() {
        logger.info("📊 Collecting debug information...");
        
        // System information
        logger.info("Java Version: " + System.getProperty("java.version"));
        logger.info("OS: " + System.getProperty("os.name"));
        logger.info("Available Memory: " + Runtime.getRuntime().maxMemory() / 1024 / 1024 + " MB");
        
        // API information
        try {
            logger.info("API Version: " + api.getClass().getPackage().getImplementationVersion());
            logger.info("Default Username: " + api.getDefaultUsername());
            logger.info("Has Default Credentials: " + api.hasDefaultCredentials());
            
            // Blockchain statistics
            String summary = api.getBlockchainSummary();
            logger.info("Blockchain Summary: " + summary);
            
        } catch (Exception e) {
            logger.error("Failed to collect API debug info", e);
        }
        
        // Recent errors
        collectRecentErrors();
    }
    
    private void collectRecentErrors() {
        logger.info("🚨 Recent errors (check logs for details):");
        // This would typically read from log files
        // Implementation depends on your logging setup
    }
}
```

## 📞 Getting Help

### When to Contact Support

Contact support if you encounter:
- Persistent data corruption that cannot be resolved with recovery procedures
- Security breaches or unauthorized access
- Performance issues that don't improve with optimization
- Validation errors that prevent normal operation

### Information to Provide

When requesting help, include:

1. **Error Details**
   - Complete error messages and stack traces
   - Steps to reproduce the issue
   - Expected vs actual behavior

2. **System Information**
   - Java version and JVM settings
   - Operating system details
   - Available memory and disk space

3. **API Configuration**
   - API version and configuration
   - Key management setup
   - Recent changes to the system

4. **Debug Information**
   ```java
   // Run this to collect comprehensive debug info
   DebugInfoCollector collector = new DebugInfoCollector();
   collector.collectDebugInfo();
   
   SystemHealthChecker healthChecker = new SystemHealthChecker();
   healthChecker.performHealthCheck();
   ```

## 📚 Additional Resources

- [Getting Started Guide](GETTING_STARTED.md) - Basic setup and common first steps
- [Security Best Practices](SECURITY_GUIDE.md) - Security configuration and best practices
- [Key Management Guide](KEY_MANAGEMENT_GUIDE.md) - Advanced key management and recovery
- [Advanced Search Guide](USER_FRIENDLY_SEARCH_GUIDE.md) - Search optimization and troubleshooting
- [API Reference](API_GUIDE.md) - Complete API documentation
- [Examples Guide](EXAMPLES.md) - Working examples and patterns
- [Technical Details](TECHNICAL_DETAILS.md) - Architecture and implementation details
- [Testing Guide](TESTING.md) - Comprehensive testing and validation