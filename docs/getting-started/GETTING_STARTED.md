# Getting Started with UserFriendlyEncryptionAPI

## 📋 Overview

The UserFriendlyEncryptionAPI provides a simplified interface for blockchain operations with enterprise-grade security. This guide will help you get started with the most common use cases.

> **⚠️ SECURITY UPDATE (v1.0.6)**: The API now requires **mandatory pre-authorization** of all users. Follow the secure initialization pattern below.

## 🔑 Prerequisites: Generate Genesis Admin Keys

**Before starting**, you need to generate the genesis-admin keys required for bootstrap authorization.

### For Production/Development

```bash
./tools/generate_genesis_keys.zsh
```

This creates:
- `./keys/genesis-admin.private` (6.6KB Dilithium ML-DSA-87 private key)
- `./keys/genesis-admin.public` (3.5KB Dilithium ML-DSA-87 public key)

> **🔐 IMPORTANT**: Backup these keys to a secure location! If lost, you cannot create new users.

### For Tests

Tests automatically generate genesis keys if they don't exist. See [AUTO_GENESIS_KEY_GENERATION.md](../testing/AUTO_GENESIS_KEY_GENERATION.md) for details.

> **⚠️ WARNING**: Never delete `./keys/genesis-admin.*` in production - you'll lose access to user management!

## 🚀 Quick Start

### 1. Secure Initialization (REQUIRED)

```java
import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.service.UserFriendlyEncryptionAPI;
import com.rbatllet.blockchain.security.KeyFileLoader;

// Step 1: Create blockchain (only genesis block is automatic)
Blockchain blockchain = new Blockchain();

// Step 2: Load bootstrap admin keys
KeyPair genesisKeys = KeyFileLoader.loadKeyPairFromFiles(
    "./keys/genesis-admin.private",
    "./keys/genesis-admin.public"
);

// Step 3: EXPLICIT bootstrap admin creation (REQUIRED for security!)
blockchain.createBootstrapAdmin(
    CryptoUtil.publicKeyToString(genesisKeys.getPublic()),
    "BOOTSTRAP_ADMIN"
);

// Step 4: Create API with bootstrap admin credentials
UserFriendlyEncryptionAPI api = new UserFriendlyEncryptionAPI(blockchain);
api.setDefaultCredentials("BOOTSTRAP_ADMIN", genesisKeys);

// Step 5: Create your user (authorized by bootstrap admin)
KeyPair yourKeys = api.createUser("your-username");

// Step 6: Switch to your user for daily operations
api.setDefaultCredentials("your-username", yourKeys);

// ✅ Now you're ready to use the API securely!
```

> **💡 TIP**: The genesis admin keys (`./keys/genesis-admin.*`) are required for all user management operations. Make sure to backup them securely!

### 2. Store Your First Secret

```java
// Store sensitive data
String sensitiveData = "Patient medical record: Diagnosis - Type 2 Diabetes";
String password = "SecurePassword123!";

Block block = api.storeSecret(sensitiveData, password);
System.out.println("✅ Data stored in block #" + block.getBlockNumber());
```

### 3. Retrieve Your Data

```java
// Retrieve the data
String retrieved = api.retrieveSecret(block.getId(), password);
System.out.println("📄 Retrieved data: " + retrieved);
```

## 🔐 Essential Security Practices

### Password Requirements
- **Minimum 8 characters** (enforced by validation)
- **Maximum 256 characters** (DoS protection)
- Use `generateValidatedPassword()` for cryptographically secure passwords

```java
// Generate secure password
String securePassword = api.generateValidatedPassword(16, false);

// Or with interactive confirmation
String confirmedPassword = api.generateValidatedPassword(20, true);
```

### Data Size Limits
- **Maximum 50MB per block** (DoS protection)
- Large files automatically use off-chain storage
- Use `storeLargeFileSecurely()` for files

## 🏥 Real-World Example: Medical Records

```java
public class MedicalRecordsExample {
    public static void main(String[] args) {
        // Setup
        Blockchain blockchain = new Blockchain();
        KeyPair doctorKeys = CryptoUtil.generateKeyPair();
        UserFriendlyEncryptionAPI api = new UserFriendlyEncryptionAPI(
            blockchain, "dr-smith", doctorKeys);
        
        // Generate secure password
        String medicalPassword = api.generateValidatedPassword(16, false);
        
        // Store patient record with searchable identifier
        String patientData = """
            Patient: John Doe
            DOB: 1985-03-15
            Diagnosis: Type 2 Diabetes Mellitus
            Treatment: Metformin 500mg twice daily
            Next appointment: 2024-08-15
            """;
        
        Block record = api.storeDataWithIdentifier(
            patientData, medicalPassword, "PATIENT-001");
        
        System.out.println("✅ Medical record stored securely");
        
        // Search for patient records
        List<Block> patientRecords = api.findRecordsByIdentifier("PATIENT-001");
        System.out.println("📋 Found " + patientRecords.size() + " records");
        
        // Retrieve specific record
        String retrievedRecord = api.retrieveSecret(record.getId(), medicalPassword);
        System.out.println("📄 Patient data: " + retrievedRecord);
    }
}
```

## 💼 Business Use Cases

### Financial Records
```java
// Store financial transaction
String transactionData = "Transfer: $50,000 from Account A to Account B";
Block transaction = api.storeDataWithIdentifier(
    transactionData, financialPassword, "TXN-2024-001");
```

### Legal Documents
```java
// Store legal contract
byte[] contractPdf = Files.readAllBytes(Paths.get("contract.pdf"));
OffChainData contract = api.storeLargeFileSecurely(
    contractPdf, legalPassword, "application/pdf");
```

### Scientific Data
```java
// Store research data with keywords
String[] researchKeywords = {"climate", "temperature", "analysis", "2024"};
Block research = api.storeSearchableData(
    researchData, researchPassword, researchKeywords);
```

## 🔍 Searching Your Data

### Basic Search
```java
// Search by terms
String[] searchTerms = {"diabetes", "patient"};
List<Block> results = api.searchByTerms(searchTerms, password, 10);
```

### Advanced Search
```java
// Advanced search with criteria
Map<String, Object> criteria = new HashMap<>();
criteria.put("keywords", "medical diagnosis");
criteria.put("categories", Set.of("medical", "healthcare"));
criteria.put("startDate", LocalDateTime.of(2024, 1, 1, 0, 0));
criteria.put("includeEncrypted", true);

AdvancedSearchResult results = api.performAdvancedSearch(criteria, password, 25);
```

## 🛠️ User Management

### Create New User
```java
// Create user account
KeyPair newUserKeys = api.createUser("alice-researcher");

// Save keys securely
SecureKeyStorage.savePrivateKey("alice-researcher", 
    newUserKeys.getPrivate(), "userPassword");
```

### Load Existing User

> **⚠️ v1.0.6+**: `loadUserCredentials()` requires the caller to be authorized. Assumes secure initialization completed above.

```java
// Load user credentials (requires authorized caller)
boolean loaded = api.loadUserCredentials("alice-researcher", "userPassword");
if (loaded) {
    System.out.println("✅ User credentials loaded successfully");
}
```

## 📊 Monitoring and Validation

### Blockchain Health Check
```java
// Get validation report
String report = api.getValidationReport();
System.out.println(report);

// Check if blockchain is healthy
boolean healthy = api.hasEncryptedData();
System.out.println("Blockchain has data: " + healthy);
```

### Performance Monitoring
```java
// Get blockchain summary
String summary = api.getBlockchainSummary();
System.out.println(summary);

// Enhanced SearchResults with robustness
SearchResults results = api.searchExhaustive("medical data", password);

// Safe result processing - all methods are null-safe
if (results.hasResults()) {
    System.out.println("Found: " + results.getResultCount() + " results");
    System.out.println("Query: " + results.getQuery());  // Never returns null
    
    // Safe formatting with null protection
    String formatted = api.formatSearchResults("medical", results.getBlocks());
    System.out.println(formatted);
    
    // Enhanced analysis with metadata
    results.addDetail("category", "medical")
           .addDetail("searchTime", System.currentTimeMillis());
           
    System.out.println("Complete report:\n" + results.toString());
} else {
    System.out.println("No results found for: " + results.getQuery());
}
```

## ⚠️ Common Mistakes to Avoid

### ❌ Don't Do This
```java
// Weak password
api.storeSecret(data, "123"); // Will throw IllegalArgumentException

// Null data
api.storeSecret(null, password); // Will throw IllegalArgumentException

// Oversized data without checking
String hugeData = "data".repeat(20_000_000); // 80MB
api.storeSecret(hugeData, password); // Will throw IllegalArgumentException
```

### ✅ Do This Instead
```java
// Strong password
String password = api.generateValidatedPassword(16, false);

// Validate data size
if (data.length() > 10 * 1024 * 1024) { // 10MB
    // Use off-chain storage for large data
    OffChainData offChain = api.storeLargeFileSecurely(
        data.getBytes(), password, "text/plain");
} else {
    Block block = api.storeSecret(data, password);
}

// Always check for null
if (data != null && !data.trim().isEmpty()) {
    Block block = api.storeSecret(data, password);
}
```

## 🔗 Next Steps

- **Security Best Practices**: Read [SECURITY_GUIDE.md](../security/SECURITY_GUIDE.md) for production security guidelines
- **Advanced Search**: See [USER_FRIENDLY_SEARCH_GUIDE.md](../search/USER_FRIENDLY_SEARCH_GUIDE.md) for powerful search capabilities  
- **Key Management**: Check [KEY_MANAGEMENT_GUIDE.md](../security/KEY_MANAGEMENT_GUIDE.md) for enterprise key management
- **Troubleshooting**: Visit [TROUBLESHOOTING_GUIDE.md](TROUBLESHOOTING_GUIDE.md) for common issues and solutions
- **Complete API Reference**: Explore [API_GUIDE.md](../reference/API_GUIDE.md) for comprehensive documentation
- **Real-World Examples**: See [EXAMPLES.md](EXAMPLES.md) for practical use cases

## 📞 Support

For questions or issues:
1. Check the [Troubleshooting Guide](TROUBLESHOOTING_GUIDE.md)
2. Review the [JavaDoc documentation](../reference/API_GUIDE.md)
3. Run the interactive demos in `/scripts/` folder
4. Check the comprehensive test suite for usage examples