# API Guide & Core Functions

Comprehensive guide to the Private Blockchain API, core functions, off-chain storage, and programming interface.

> **IMPORTANT NOTE**: This guide references the actual classes and methods implemented in the project. All mentioned classes (Blockchain, Block, AuthorizedKey, OffChainData, OffChainStorageService, JPAUtil, CryptoUtil) exist in the source code. The code examples show the correct usage of the current API based on the JPA standard with Hibernate as the implementation provider.

> **NEW FEATURE**: This blockchain now includes automatic off-chain storage for large data (>512KB) with AES-256-GCM encryption, integrity verification, and configurable size thresholds. All off-chain operations are handled transparently by the API.

## 📋 Table of Contents

- [Core Functions Usage](#-core-functions-usage)
- [API Reference](#-api-reference)
- [Chain Validation Result](#-chain-validation-result)
- [Enhanced Block Creation API](#-enhanced-block-creation-api)
- [Granular Term Visibility API](#-granular-term-visibility-api)
- [Off-Chain Storage API](#-off-chain-storage-api)
- [EncryptionConfig Integration](#-encryptionconfig-integration)
- [Configuration](#-configuration)
- [Configuration Parameters](#-configuration-parameters)
- [Thread Safety](#-thread-safety-and-concurrent-usage)
- [Best Practices](#-best-practices)

## 🔗 Chain Validation Result

The `ChainValidationResult` class provides detailed information about the blockchain validation status. It's returned by the `validateChainDetailed()` method and contains comprehensive validation status, including any invalid or revoked blocks.

### Key Methods

```java
// Check if the blockchain is structurally intact (all hashes and signatures are valid)
boolean isStructurallyIntact()

// Check if the blockchain is fully compliant (all blocks are authorized and not revoked)
boolean isFullyCompliant()

// Get the list of invalid blocks (empty if structurally intact)
List<Block> getInvalidBlocks()

// Get the list of revoked blocks (empty if fully compliant)
List<Block> getRevokedBlocks()

// Get a summary of the validation results
String toString()
```

### Example Usage

```java
// Get detailed validation results
ChainValidationResult result = blockchain.validateChainDetailed();

// Check structural integrity first (hashes and signatures)
if (!result.isStructurallyIntact()) {
    System.err.println("❌ CRITICAL: Blockchain structure is compromised!");
    System.err.println("Invalid blocks: " + result.getInvalidBlocks().size());
    
    // Handle invalid blocks (corrupted or tampered)
    for (Block invalidBlock : result.getInvalidBlocks()) {
        System.err.println(String.format(
            " - Block #%d (Hash: %s...) has invalid structure",
            invalidBlock.getBlockNumber(),
            invalidBlock.getHash().substring(0, 16)
        ));
    }
    
    // In production, you might want to trigger an alert or recovery process here
    triggerRecoveryProcess(result);
    
} else if (!result.isFullyCompliant()) {
    // Chain is structurally sound but has authorization issues
    System.out.println("⚠️  WARNING: Some blocks have authorization issues");
    System.out.println("Revoked blocks: " + result.getRevokedBlocks().size());
    
    // Handle revoked blocks (signed by revoked keys)
    for (Block revokedBlock : result.getRevokedBlocks()) {
        String signer = revokedBlock.getSignerPublicKey() != null ? 
            revokedBlock.getSignerPublicKey().substring(0, 16) + "..." : "unknown";
            
        System.out.println(String.format(
            " - Block #%d (Signed by: %s) was signed by a revoked key",
            revokedBlock.getBlockNumber(),
            signer
        ));
    }
    
    // You might want to re-sign these blocks or mark them for review
    handleRevokedBlocks(result.getRevokedBlocks());
    
} else {
    // Chain is fully valid
    System.out.println("✅ Blockchain validation passed");
    System.out.println("Total blocks: " + blockchain.getBlockCount());
}

// Print a detailed validation report
System.out.println("\n=== VALIDATION REPORT ===");
System.out.println(result.toString());
System.out.println("Validation timestamp: " + new Date());

// For logging or monitoring systems
metrics.recordValidationResult(
    result.isStructurallyIntact(),
    result.isFullyCompliant(),
    result.getInvalidBlocks().size(),
    result.getRevokedBlocks().size()
);
```

### Validation Scenarios

1. **Valid Chain**
   - `isStructurallyIntact()`: `true`
   - `isFullyCompliant()`: `true`
   - `getInvalidBlocks()`: Empty list
   - `getRevokedBlocks()`: Empty list

2. **Structurally Valid but with Revoked Keys**
   - `isStructurallyIntact()`: `true`
   - `isFullyCompliant()`: `false`
   - `getInvalidBlocks()`: Empty list
   - `getRevokedBlocks()`: List of blocks signed by revoked keys

3. **Structurally Invalid**
   - `isStructurallyIntact()`: `false`
   - `isFullyCompliant()`: `false` (always false if not structurally intact)
   - `getInvalidBlocks()`: List of blocks with invalid hashes or signatures
   - `getRevokedBlocks()`: May include additional revoked blocks

### Best Practices

- Always check `isStructurallyIntact()` before `isFullyCompliant()`
- Use `toString()` for user-friendly status messages
- Log detailed validation results for debugging
- Consider automatic recovery procedures for common issues

## 🚀 Enhanced Block Creation API

The UserFriendlyEncryptionAPI provides an enhanced **thread-safe** `createBlockWithOptions()` method that supports all available BlockCreationOptions features for advanced block creation with metadata, encryption, and off-chain storage.

### BlockCreationOptions Support

**✅ All 8 Options Supported** with thread-safe implementation:

| Option | Description | Implementation |
|--------|-------------|----------------|
| `category` | Block category (MEDICAL, FINANCIAL, etc.) | ✅ JPA transaction metadata update |
| `keywords` | Searchable keywords array | ✅ Atomic keyword string creation |
| `offChain` | Enable off-chain storage | ✅ Synchronized off-chain operations |
| `offChainFilePath` | File path for off-chain data | ✅ Thread-safe file operations |
| `password` | Encryption password | ✅ Concurrent-safe encryption |
| `encryption` | Force encryption mode | ✅ Thread-safe crypto operations |
| `username` | Block username | ✅ Atomic username assignment |
| `recipientUsername` | Target recipient | ⚠️ Future enhancement |

### Thread-Safe Implementation Details

#### Metadata Operations
```java
// Thread-safe metadata updates using JPA transactions
if (options.getCategory() != null || options.getKeywords() != null) {
    // Synchronized transaction ensures thread safety
    blockchain.updateBlockMetadata(result.getId(), 
                                 options.getCategory(), 
                                 keywordsString);
}
```

#### Off-Chain Storage Priority
```java
// Off-chain operations take priority and are thread-safe
if (options.isOffChain() && options.getOffChainFilePath() != null) {
    // Thread-safe off-chain storage service
    OffChainData offChainData = offChainStorage.storeData(
        fileContent, options.getPassword(), // Optional encryption
        userKeyPair.getPrivate(), CryptoUtil.publicKeyToString(userKeyPair.getPublic()),
        contentType
    );
    
    // Synchronized blockchain method with proper locking
    return blockchain.addBlockWithOffChainData(
        content, offChainData, options.getKeywords(),
        options.getPassword(), userKeyPair.getPrivate(), userKeyPair.getPublic()
    );
}
```

### Usage Examples

#### Basic Category and Keywords
```java
import com.rbatllet.blockchain.service.UserFriendlyEncryptionAPI.BlockCreationOptions;

BlockCreationOptions options = new BlockCreationOptions()
    .withCategory("MEDICAL")
    .withKeywords(new String[]{"patient", "diagnosis", "cardiology"})
    .withUsername("doctor");

Block result = api.createBlockWithOptions("Patient medical record", options);

// Result:
// - Block with category "MEDICAL"
// - Keywords: "patient diagnosis cardiology"
// - Thread-safe metadata via JPA transaction
```

#### Encrypted Block with Metadata
```java
BlockCreationOptions options = new BlockCreationOptions()
    .withPassword("SecurePass123!")
    .withEncryption(true)
    .withCategory("CONFIDENTIAL")
    .withKeywords(new String[]{"classified", "project-alpha"});

Block result = api.createBlockWithOptions("Top secret data", options);

// Result:
// - Encrypted block with password protection
// - Category: "CONFIDENTIAL"
// - Keywords: "classified project-alpha" (encrypted metadata)
// - Thread-safe encryption and metadata operations
```

#### Off-Chain Storage with Encryption
```java
// Create file for off-chain storage
Path documentFile = Files.createTempFile("report", ".pdf");
Files.writeString(documentFile, "Large quarterly report content...");

BlockCreationOptions options = new BlockCreationOptions()
    .withOffChain(true)
    .withOffChainFilePath(documentFile.toString())
    .withPassword("FilePassword123!")
    .withCategory("REPORT")
    .withKeywords(new String[]{"quarterly", "financial"});

Block result = api.createBlockWithOptions("Q4 Report metadata", options);

// Result:
// - Large file stored off-chain with AES encryption
// - Category automatically set to "OFF_CHAIN_LINKED"
// - MIME type detected automatically (application/pdf)
// - Thread-safe file operations and blockchain updates
```

#### Unencrypted Off-Chain Storage
```java
BlockCreationOptions options = new BlockCreationOptions()
    .withOffChain(true)
    .withOffChainFilePath("/path/to/public-document.txt")
    .withCategory("PUBLIC_DOCUMENT")
    .withKeywords(new String[]{"announcement", "policy"})
    .withUsername("publisher");

Block result = api.createBlockWithOptions("Public announcement", options);

// Result:
// - File stored off-chain without encryption
// - Public metadata preserved
// - Thread-safe operations maintained
```

### Error Handling and Edge Cases

#### Graceful Null Handling
```java
BlockCreationOptions options = new BlockCreationOptions()
    .withCategory("TEST")
    .withKeywords(null)  // Handled gracefully
    .withUsername("tester");

Block result = api.createBlockWithOptions("Test content", options);
// Result: Creates block with category but no keywords
```

#### Validation and Error Responses
```java
try {
    BlockCreationOptions options = new BlockCreationOptions()
        .withOffChain(true)
        .withOffChainFilePath("/nonexistent/file.txt");
    
    Block result = api.createBlockWithOptions("Test", options);
} catch (RuntimeException e) {
    // Handles invalid file paths gracefully
    System.err.println("File operation failed: " + e.getMessage());
}
```

### Performance Characteristics

- **Concurrent Safety**: All operations use proper synchronization
- **Transaction Integrity**: JPA transactions ensure atomicity
- **File Operations**: Thread-safe off-chain storage with proper locking
- **Memory Efficiency**: Minimal memory overhead for metadata operations
- **Error Recovery**: Graceful handling of edge cases and failures

### Integration with Existing APIs

The enhanced `createBlockWithOptions()` integrates seamlessly with existing blockchain functionality:

```java
// Works with all existing validation and query methods
ChainValidationResult validation = blockchain.validateChainDetailed();
List<Block> categoryBlocks = blockchain.searchBlocksByCategory("MEDICAL");
List<Block> keywordBlocks = blockchain.searchBlocksByKeywords("patient");

// Off-chain data automatically handled in validation
boolean offChainIntegrity = blockchain.verifyOffChainIntegrity(result);
```

## 🔐 Granular Term Visibility API

The blockchain supports **granular term visibility control**, allowing you to specify exactly which search terms should be publicly searchable versus password-protected. This provides fine-grained privacy control for compliance and data protection.

### TermVisibilityMap Class

Core class for controlling individual search term visibility:

```java
import com.rbatllet.blockchain.search.metadata.TermVisibilityMap;
import com.rbatllet.blockchain.search.metadata.TermVisibilityMap.VisibilityLevel;

// Constructor with default visibility level
TermVisibilityMap(VisibilityLevel defaultLevel)
TermVisibilityMap()  // Default: PUBLIC

// Configure term visibility
TermVisibilityMap setPublic(String... terms)
TermVisibilityMap setPrivate(String... terms)
TermVisibilityMap setTerm(String term, VisibilityLevel level)

// Query visibility
VisibilityLevel getVisibility(String term)
boolean isPublic(String term)
boolean isPrivate(String term)

// Get distributed terms
Set<String> getPublicTerms(Collection<String> allTerms)
Set<String> getPrivateTerms(Collection<String> allTerms)

// Utility methods
int size()
boolean isEmpty()
void clear()
TermVisibilityMap copy()
```

### UserFriendlyEncryptionAPI Extensions

New methods for granular term control:

```java
// Granular term visibility control
Block storeDataWithGranularTermControl(String data, String password, 
                                     Set<String> allSearchTerms, 
                                     TermVisibilityMap termVisibility)

// Convenience method for separated terms
Block storeDataWithSeparatedTerms(String data, String password,
                                String[] publicTerms, String[] privateTerms)
```

### MetadataLayerManager Extensions

Low-level metadata generation with granular control:

```java
// Generate metadata with granular term distribution
BlockMetadataLayers generateMetadataLayersWithGranularControl(
    Block block, 
    EncryptionConfig config, 
    String password, 
    PrivateKey privateKey,
    Set<String> allSearchTerms,
    TermVisibilityMap termVisibility)
```

### ⚙️ Internal Storage Logic

The granular term visibility system uses a **clean separation approach** for storing search terms:

#### Storage Fields

- **`manualKeywords`**: Stores **PUBLIC terms only** (unencrypted, searchable without password)
  - Contains terms with `PUBLIC:` prefix (e.g., `"public:patient public:treatment"`)
  - Set to `null` when no public terms are specified
  
- **`autoKeywords`**: Stores **PRIVATE terms only** (AES-256-GCM encrypted, requires password)
  - Contains encrypted private keywords and auto-extracted terms
  - Always encrypted when present

#### Term Processing Logic

```java
// Keywords without PUBLIC: prefix → Private (encrypted in autoKeywords)
String[] keywords = {"patient", "diabetes", "insulin"};
// Result: manualKeywords = null, autoKeywords = "[ENCRYPTED_DATA]"

// Keywords with PUBLIC: prefix → Public (unencrypted in manualKeywords)  
String[] keywords = {"PUBLIC:patient", "PUBLIC:treatment", "diabetes"};
// Result: manualKeywords = "public:patient public:treatment", autoKeywords = "[ENCRYPTED_diabetes]"
```

#### Search Behavior

- **Public search** (`searchByTerms(terms, null, limit)`):
  - Only searches `manualKeywords` field
  - Fast, no decryption required
  - Returns blocks where terms are publicly accessible

- **Private search** (`searchAndDecryptByTerms(terms, password, limit)`):
  - Searches both `manualKeywords` (public) and `autoKeywords` (private)
  - Decrypts private terms using provided password
  - Returns blocks where terms are found in either layer

### Practical Examples

#### Medical Record Privacy

```java
// Medical record with mixed privacy requirements
String medicalData = "Patient John Smith diagnosed with diabetes. Treatment with insulin therapy.";

Set<String> allTerms = Set.of("patient", "john", "smith", "diagnosed", 
                            "diabetes", "treatment", "insulin", "therapy");

TermVisibilityMap visibility = new TermVisibilityMap()
    .setPublic("patient", "diagnosed", "treatment", "therapy")    // Medical terms - public
    .setPrivate("john", "smith", "diabetes", "insulin");          // Personal/specific - private

// Store with granular control
Block block = api.storeDataWithGranularTermControl(medicalData, password, allTerms, visibility);

// Storage result:
// manualKeywords = "public:patient public:diagnosed public:treatment public:therapy"
// autoKeywords = "[ENCRYPTED: john smith diabetes insulin]"

// Search behavior:
// ✅ Public: searchByTerms(["patient"], null, 10) → finds results (from manualKeywords)
// ❌ Private: searchByTerms(["diabetes"], null, 10) → no results (not in manualKeywords)
// ✅ Private: searchAndDecryptByTerms(["diabetes"], password, 10) → finds results (decrypts autoKeywords)
```

#### Financial Data Protection

```java
// Financial transaction with default private, selective public
String financialData = "SWIFT transfer $25000 from account 987-654-321 to Maria Garcia for property purchase.";

Set<String> allTerms = Set.of("swift", "transfer", "25000", "account", "987-654-321", 
                            "maria", "garcia", "property", "purchase");

// Default PRIVATE with selective PUBLIC terms
TermVisibilityMap visibility = new TermVisibilityMap(VisibilityLevel.PRIVATE)
    .setPublic("swift", "transfer", "property", "purchase");  // Transaction type - public
    // Amounts, account numbers, names remain private

Block block = api.storeDataWithGranularTermControl(financialData, password, allTerms, visibility);

// Storage result:
// manualKeywords = "public:swift public:transfer public:property public:purchase"  
// autoKeywords = "[ENCRYPTED: 25000 account 987-654-321 maria garcia]"

// Search behavior:
// ✅ Public: searchByTerms(["swift"], null, 10) → finds results (transaction type is public)
// ❌ Private: searchByTerms(["25000"], null, 10) → no results (amount is private)
// ✅ Private: searchAndDecryptByTerms(["maria"], password, 10) → finds results (decrypts names)
```

#### HR Data with Separated Terms

```java
// Employee data with clear public/private separation
String employeeData = "Employee Alice Johnson salary $75000 department Engineering performance excellent.";

String[] publicTerms = {"employee", "salary", "department", "engineering", "performance"};
String[] privateTerms = {"alice", "johnson", "75000", "excellent"};

// Use convenience method
Block block = api.storeDataWithSeparatedTerms(employeeData, password, publicTerms, privateTerms);

// Storage result:
// manualKeywords = "public:employee public:salary public:department public:engineering public:performance"
// autoKeywords = "[ENCRYPTED: alice johnson 75000 excellent]"

// Search behavior:
// ✅ Public: searchByTerms(["engineering"], null, 10) → finds results (department is public)
// ❌ Private: searchByTerms(["alice"], null, 10) → no results (name is private)
// ✅ Private: searchAndDecryptByTerms(["75000"], password, 10) → finds results (decrypts salary)
```

### Compliance and Security Benefits

- **🏥 HIPAA Compliance**: Medical terms public, patient identifiers private
- **💰 Financial Privacy**: Transaction types public, amounts/accounts private
- **📊 Data Analytics**: Aggregate terms searchable, individual data protected
- **🔍 Audit Trails**: Activity types visible, specific details encrypted
- **⚡ Performance**: Public terms searchable without decryption overhead

### Testing and Validation

```bash
# Run granular visibility tests
mvn test -Dtest=TermVisibilityMapTest
mvn test -Dtest=GranularTermVisibilityIntegrationTest

# Interactive demo
./scripts/run_granular_term_visibility_demo.zsh
```

## 📁 Off-Chain Storage API

The off-chain storage system automatically handles large data (>512KB by default) by storing it in encrypted files outside the blockchain while maintaining cryptographic integrity and security.

### How It Works

1. **Automatic Detection**: When adding a block, the system checks data size
2. **Storage Decision**: Data >512KB (configurable) is automatically stored off-chain
3. **Encryption**: Off-chain files are encrypted with AES-256-GCM using authenticated encryption
4. **Reference Storage**: Block contains `OFF_CHAIN_REF:hash` instead of actual data
5. **Integrity Protection**: SHA3-256 hash and ECDSA signature verify data integrity

### Key Classes and Methods

#### Block Entity Extensions
```java
// Check if block uses off-chain storage
public boolean hasOffChainData()

// Get off-chain metadata (null if none)
public OffChainData getOffChainData()

// Set off-chain metadata (used internally)
public void setOffChainData(OffChainData offChainData)
```

#### OffChainData Entity
```java
// Data integrity hash (SHA3-256)
public String getDataHash()

// Digital signature for authenticity
public String getSignature()

// Encrypted file path
public String getFilePath()

// Original file size in bytes
public Long getFileSize()

// AES-256-GCM encryption IV (Base64 encoded)
public String getEncryptionIV()

// Content type (e.g., "text/plain", "application/pdf")
public String getContentType()

// Signer's public key
public String getSignerPublicKey()
```

### Usage Examples

#### Basic Off-Chain Operations
```java
// Adding data (automatic off-chain handling)
String largeDocument = loadLargeDocument(); // >512KB
Block block = blockchain.addBlockAndReturn(largeDocument, privateKey, publicKey);

// Check storage type
if (block.hasOffChainData()) {
    System.out.println("Data stored off-chain");
    OffChainData metadata = block.getOffChainData();
    System.out.println("File size: " + metadata.getFileSize() + " bytes");
    System.out.println("Content type: " + metadata.getContentType());
}

// Retrieve complete data (transparent)
String retrievedData = blockchain.getCompleteBlockData(block);
assert largeDocument.equals(retrievedData); // Should be identical
```

#### Storage Configuration
```java
// Configure for different use cases
public void setupForDocumentManagement() {
    // Store documents >100KB off-chain
    blockchain.setOffChainThresholdBytes(100 * 1024);
    
    // Allow larger on-chain blocks if needed
    blockchain.setMaxBlockSizeBytes(5 * 1024 * 1024);
    
    // Display current settings
    System.out.println(blockchain.getConfigurationSummary());
}

// Example output:
// Block Size Configuration:
// - Max block size: 5,242,880 bytes (5.0 MB)
// - Max data length: 10,000 characters
// - Off-chain threshold: 102,400 bytes (100.0 KB)
// - Default values: 1,048,576 bytes / 10,000 chars / 524,288 bytes
```

#### Data Validation and Storage Decision
```java
// Check what will happen before adding block
String testData = "Some data to test";
int decision = blockchain.validateAndDetermineStorage(testData);

switch (decision) {
    case 0:
        System.out.println("Data is invalid (too large or null)");
        break;
    case 1:
        System.out.println("Data will be stored on-chain");
        break;
    case 2:
        System.out.println("Data will be stored off-chain");
        break;
}
```

#### Integrity Verification
```java
// Verify individual block
public boolean checkBlockIntegrity(Block block) {
    if (block.hasOffChainData()) {
        boolean isValid = blockchain.verifyOffChainIntegrity(block);
        if (!isValid) {
            System.err.println("Off-chain data corrupted for block " + block.getBlockNumber());
            return false;
        }
    }
    return true;
}

// Verify entire blockchain off-chain data
public void performIntegrityAudit() {
    System.out.println("Starting comprehensive off-chain integrity audit...");
    
    boolean allValid = blockchain.verifyAllOffChainIntegrity();
    
    if (allValid) {
        System.out.println("✅ All off-chain data passed integrity checks");
    } else {
        System.err.println("❌ Some off-chain data failed integrity verification");
        // Investigate and potentially recover corrupted data
    }
}
```

### Security Features

#### Encryption Details
- **Algorithm**: AES-256-GCM with authenticated encryption
- **Key Derivation**: SHA3-256 hash of deterministic password (32 bytes)
- **Nonce Generation**: Cryptographically secure random 12-byte nonce per file
- **Password Generation**: Based on block number + signer public key (reproducible)

#### Integrity Protection
- **Content Hash**: SHA3-256 of original (unencrypted) data
- **Digital Signature**: ECDSA signature of the content hash
- **Verification**: Automatic integrity check on retrieval
- **Tamper Detection**: Any modification to off-chain files is detected

### Storage Limits and Thresholds

| Setting | Default | Range | Description |
|---------|---------|--------|------------|
| On-Chain Block Size | 1MB | 1B - 10MB | Maximum size for on-chain storage |
| On-Chain Data Length | 10,000 chars | 1 - 1M chars | Maximum characters for on-chain text |
| Off-Chain Threshold | 512KB | 1B - Block Size | Size threshold for off-chain storage |
| Off-Chain Max Size | 100MB | - | Maximum size per off-chain file |

### Error Handling

```java
// Proper error handling for off-chain operations
public String safeRetrieveBlockData(Block block) {
    try {
        return blockchain.getCompleteBlockData(block);
    } catch (FileNotFoundException e) {
        System.err.println("Off-chain file missing for block " + block.getBlockNumber());
        // Log incident, attempt recovery, or alert administrators
        return null;
    } catch (SecurityException e) {
        System.err.println("Off-chain data integrity check failed: " + e.getMessage());
        // Handle corrupted data - potential security incident
        return null;
    } catch (Exception e) {
        System.err.println("Unexpected error retrieving off-chain data: " + e.getMessage());
        return null;
    }
}
```

## 🎯 Core Functions Usage

### JPA Integration Examples

#### Direct JPA Operations
```java
// Using JPA EntityManager for custom operations
EntityManager em = JPAUtil.getEntityManager();
EntityTransaction transaction = null;

try {
    transaction = em.getTransaction();
    transaction.begin();
    
    // Create and persist a new block
    Block newBlock = new Block();
    newBlock.setBlockNumber(1L);
    newBlock.setData("Custom block data");
    newBlock.setTimestamp(LocalDateTime.now());
    
    em.persist(newBlock);
    transaction.commit();
    
} catch (Exception e) {
    if (transaction != null && transaction.isActive()) {
        transaction.rollback();
    }
    throw new RuntimeException("Error in JPA operation", e);
} finally {
    em.close();
}
```

#### JPA Query Language (JPQL) Examples
```java
// Custom JPQL queries for advanced operations
EntityManager em = JPAUtil.getEntityManager();
try {
    // Find blocks by date range using JPQL
    TypedQuery<Block> query = em.createQuery(
        "SELECT b FROM Block b WHERE b.timestamp BETWEEN :startTime AND :endTime ORDER BY b.blockNumber ASC", 
        Block.class);
    query.setParameter("startTime", startDateTime);
    query.setParameter("endTime", endDateTime);
    List<Block> blocks = query.getResultList();
    
    // Count blocks with specific content
    TypedQuery<Long> countQuery = em.createQuery(
        "SELECT COUNT(b) FROM Block b WHERE LOWER(b.data) LIKE :content", 
        Long.class);
    countQuery.setParameter("content", "%" + searchTerm.toLowerCase() + "%");
    Long count = countQuery.getSingleResult();
    
    // Get blocks by signer
    TypedQuery<Block> signerQuery = em.createQuery(
        "SELECT b FROM Block b WHERE b.signerPublicKey = :publicKey ORDER BY b.blockNumber ASC", 
        Block.class);
    signerQuery.setParameter("publicKey", publicKeyString);
    List<Block> signerBlocks = signerQuery.getResultList();
    
} finally {
    em.close();
}
```

#### Entity Management with JPA Annotations
```java
// Example of JPA entity configuration
@Entity
@Table(name = "blocks")
public class Block {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "block_number", unique = true, nullable = false)
    private Long blockNumber;
    
    @Column(name = "data", columnDefinition = "TEXT")
    private String data;
    
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
    
    @Column(name = "hash", length = 64, nullable = false)
    private String hash;
    
    // JPA lifecycle callbacks
    @PrePersist
    public void prePersist() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
    
    // Getters and setters...
}
```

### Basic Operations

#### Initialize and Setup
```java
// Create blockchain (automatic genesis block)
Blockchain blockchain = new Blockchain();

// Add authorized users
KeyPair alice = CryptoUtil.generateKeyPair();
String alicePublicKey = CryptoUtil.publicKeyToString(alice.getPublic());
blockchain.addAuthorizedKey(alicePublicKey, "Alice");
```

#### Add Blocks
```java
// Add data to blockchain
boolean success = blockchain.addBlock(
    "Transaction: Alice sends payment to Bob",  // Your data
    alice.getPrivate(),                         // Private key (for signing)
    alice.getPublic()                          // Public key (for verification)
);
```

#### Validate Chain
```java
// Check if blockchain is valid with detailed information
ChainValidationResult result = blockchain.validateChainDetailed();
System.out.println("Blockchain is structurally intact: " + result.isStructurallyIntact());
System.out.println("Blockchain is fully compliant: " + result.isFullyCompliant());

// Get detailed validation information
if (!result.isFullyCompliant()) {
    System.out.println("Revoked blocks: " + result.getRevokedBlocks());
}
if (!result.isStructurallyIntact()) {
    System.out.println("Invalid blocks: " + result.getInvalidBlocks());
}

// Get complete validation report
String report = result.getDetailedReport();
System.out.println("Validation report: " + report);
```

### Advanced Operations

#### Block Size Validation
```java
// Get size limits
int maxBytes = blockchain.getMaxBlockSizeBytes();      // 1MB limit
int maxChars = blockchain.getMaxBlockDataLength();     // 10K characters limit

// Size validation happens automatically when adding blocks
// Large blocks are rejected automatically
```

#### Chain Export/Import (Backup/Restore)
```java
// Export blockchain to JSON file
boolean exported = blockchain.exportChain("backup.json");

// Import blockchain from JSON file
boolean imported = blockchain.importChain("backup.json");
```

#### Block Rollback
```java
// Remove last 3 blocks
boolean success = blockchain.rollbackBlocks(3);

// Rollback to specific block (keep blocks 0-5)
boolean success = blockchain.rollbackToBlock(5);
```

#### Advanced Search

The blockchain now includes a comprehensive **hybrid search system** that allows searching both on-chain and off-chain content with multiple search levels and automatic keyword extraction.

##### Search Levels

The system supports three different search levels through the `SearchLevel` enum:

1. **FAST_ONLY** - Searches only in keywords (fastest performance)
2. **INCLUDE_DATA** - Searches in keywords + block data (balanced performance)  
3. **EXHAUSTIVE_OFFCHAIN** - Searches everything including off-chain content (comprehensive but slower)

##### Basic Search Methods

```java
// Legacy search method (still supported)
List<Block> paymentBlocks = blockchain.searchBlocksByContent("payment");

// New hybrid search with different levels
List<Block> fastResults = blockchain.searchBlocks("medical", SearchLevel.FAST_ONLY);
List<Block> dataResults = blockchain.searchBlocks("patient", SearchLevel.INCLUDE_DATA);
List<Block> exhaustiveResults = blockchain.searchBlocks("contract", SearchLevel.EXHAUSTIVE_OFFCHAIN);

// Convenience methods for each search level
List<Block> quickSearch = blockchain.searchBlocksFast("API");           // Keywords only
List<Block> dataSearch = blockchain.searchBlocksComplete("John Doe");   // Keywords + data + off-chain

// Search by content category
List<Block> medicalBlocks = blockchain.searchByCategory("MEDICAL");
List<Block> financeBlocks = blockchain.searchByCategory("FINANCE");
```

##### Adding Blocks with Keywords

```java
// Create blocks with manual keywords and automatic extraction
String[] manualKeywords = {"PATIENT-001", "CARDIOLOGY", "ECG-2024"};
blockchain.addBlockWithKeywords(
    "Patient examination data with detailed cardiac analysis...",
    manualKeywords,           // Manual keywords (optional)
    "MEDICAL",               // Content category
    privateKey,
    publicKey
);

// Block with automatic keyword extraction only
blockchain.addBlockWithKeywords(
    "API integration completed on 2024-01-15. Contact: admin@company.com for details.",
    null,                    // No manual keywords
    "TECHNICAL",
    privateKey,
    publicKey
);
```

##### Search Validation and Term Requirements

```java
// The system validates search terms automatically
List<Block> results;

// Valid searches (4+ characters or intelligent exceptions)
results = blockchain.searchBlocksFast("medical");     // ✅ Valid - 7 characters
results = blockchain.searchBlocksFast("2024");        // ✅ Valid - year exception
results = blockchain.searchBlocksFast("API");         // ✅ Valid - acronym exception
results = blockchain.searchBlocksFast("XML");         // ✅ Valid - technical term exception

// Invalid searches (too short)
results = blockchain.searchBlocksFast("hi");          // ❌ Returns empty list
results = blockchain.searchBlocksFast("a");           // ❌ Returns empty list
results = blockchain.searchBlocksFast("");            // ❌ Returns empty list

// Check search term validity manually
// Search term validation is now handled internally by the search engine
// Minimum length requirements are enforced automatically
```

##### Automatic Keyword Extraction

The system automatically extracts universal keywords that work across languages:

```java
// These elements are automatically extracted as keywords:
String blockData = """
    Patient: John Doe
    Date: 2024-01-15  
    Reference: MED-001
    Email: doctor@hospital.com
    Amount: 50000 EUR
    """;

// Automatically extracted keywords include:
// - Numbers: "2024", "50000"
// - Dates: "2024-01-15"
// - Codes: "med-001" 
// - Email: "doctor@hospital.com"
// - Currency: "eur"
```

##### Search Performance Examples

```java
// Performance comparison example
long startTime, endTime;

// FAST_ONLY - Best performance (keywords only)
startTime = System.nanoTime();
List<Block> fastResults = blockchain.searchBlocks("2024", SearchLevel.FAST_ONLY);
endTime = System.nanoTime();
System.out.println("FAST_ONLY: " + (endTime - startTime) / 1_000_000 + "ms");

// INCLUDE_DATA - Balanced performance (keywords + block data)
startTime = System.nanoTime();
List<Block> dataResults = blockchain.searchBlocks("2024", SearchLevel.INCLUDE_DATA);
endTime = System.nanoTime();
System.out.println("INCLUDE_DATA: " + (endTime - startTime) / 1_000_000 + "ms");

// EXHAUSTIVE_OFFCHAIN - Comprehensive search (all content)
startTime = System.nanoTime();
List<Block> exhaustiveResults = blockchain.searchBlocks("2024", SearchLevel.EXHAUSTIVE_OFFCHAIN);
endTime = System.nanoTime();
System.out.println("EXHAUSTIVE_OFFCHAIN: " + (endTime - startTime) / 1_000_000 + "ms");
```

##### Complete Search Example

```java
public class SearchFrameworkDemo {
    public static void main(String[] args) throws Exception {
        Blockchain blockchain = new Blockchain();
        
        // Generate keys
        KeyPair keyPair = CryptoUtil.generateKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();
        blockchain.addAuthorizedKey(CryptoUtil.publicKeyToString(publicKey), "SearchUser");
        
        // Add blocks with different content types
        String[] medicalKeywords = {"PATIENT-001", "CARDIOLOGY", "ECG-2024"};
        blockchain.addBlockWithKeywords(
            "Patient John Doe ECG examination on 2024-01-15. Normal cardiac rhythm detected.",
            medicalKeywords,
            "MEDICAL",
            privateKey, publicKey
        );
        
        String[] financeKeywords = {"PROJECT-ALPHA", "BUDGET-2024", "EUR"};
        blockchain.addBlockWithKeywords(
            "Project Alpha budget: 50000 EUR approved for Q1 2024 implementation.",
            financeKeywords,
            "FINANCE", 
            privateKey, publicKey
        );
        
        // Technical block with automatic keyword extraction only
        blockchain.addBlockWithKeywords(
            "API integration with database completed. JSON format used. Contact: admin@company.com",
            null,  // No manual keywords
            "TECHNICAL",
            privateKey, publicKey
        );
        
        // Demonstrate different search levels
        System.out.println("=== SEARCH DEMONSTRATIONS ===");
        
        // Fast search - keywords only
        System.out.println("\n1. FAST_ONLY Search for '2024':");
        List<Block> fastResults = blockchain.searchBlocks("2024", SearchLevel.FAST_ONLY);
        for (Block block : fastResults) {
            System.out.println("  Block #" + block.getBlockNumber() + " - " + block.getContentCategory());
        }
        
        // Include data search
        System.out.println("\n2. INCLUDE_DATA Search for 'John':");
        List<Block> dataResults = blockchain.searchBlocks("John", SearchLevel.INCLUDE_DATA);
        for (Block block : dataResults) {
            System.out.println("  Block #" + block.getBlockNumber() + " - " + 
                             block.getData().substring(0, Math.min(50, block.getData().length())) + "...");
        }
        
        // Category search
        System.out.println("\n3. Category Search for 'MEDICAL':");
        List<Block> medicalResults = blockchain.searchByCategory("MEDICAL");
        for (Block block : medicalResults) {
            System.out.println("  Block #" + block.getBlockNumber() + " - " + block.getContentCategory());
        }
        
        // Email search (automatically extracted)
        System.out.println("\n4. Email Search for 'admin@company.com':");
        List<Block> emailResults = blockchain.searchBlocks("admin@company.com", SearchLevel.INCLUDE_DATA);
        for (Block block : emailResults) {
            System.out.println("  Block #" + block.getBlockNumber() + " contains email reference");
        }
        
        // Technical term search (exception to length rule)
        System.out.println("\n5. Technical Term Search for 'API':");
        List<Block> apiResults = blockchain.searchBlocksFast("API");
        for (Block block : apiResults) {
            System.out.println("  Block #" + block.getBlockNumber() + " - " + block.getContentCategory());
        }
        
        System.out.println("\n=== SEARCH COMPLETE ===");
    }
}
```

##### Legacy Search Methods (Still Supported)

```java
// Basic content search (case-insensitive) 
List<Block> paymentBlocks = blockchain.searchBlocksByContent("payment");

// Find block by hash
Block block = blockchain.getBlockByHash("a1b2c3d4...");

// Find blocks by date range
LocalDate start = LocalDate.of(2024, 1, 1);
LocalDate end = LocalDate.of(2024, 1, 31);
List<Block> monthlyBlocks = blockchain.getBlocksByDateRange(start, end);
```

##### Search Thread Safety

All search operations are fully thread-safe and can be performed concurrently:

```java
// Concurrent search operations
ExecutorService executor = Executors.newFixedThreadPool(4);

CompletableFuture<List<Block>> search1 = CompletableFuture.supplyAsync(() -> 
    blockchain.searchBlocks("medical", SearchLevel.FAST_ONLY), executor);

CompletableFuture<List<Block>> search2 = CompletableFuture.supplyAsync(() -> 
    blockchain.searchByCategory("FINANCE"), executor);

CompletableFuture<List<Block>> search3 = CompletableFuture.supplyAsync(() -> 
    blockchain.searchBlocksComplete("2024"), executor);

// Wait for all searches to complete
CompletableFuture.allOf(search1, search2, search3)
    .thenRun(() -> System.out.println("All concurrent searches completed"));
```

### Complete Example
```java
public class BlockchainExample {
    public static void main(String[] args) {
        try {
            // 1. Initialize blockchain
            Blockchain blockchain = new Blockchain();
            
            // 2. Add users
            KeyPair alice = CryptoUtil.generateKeyPair();
            KeyPair bob = CryptoUtil.generateKeyPair();
            
            String aliceKey = CryptoUtil.publicKeyToString(alice.getPublic());
            String bobKey = CryptoUtil.publicKeyToString(bob.getPublic());
            
            blockchain.addAuthorizedKey(aliceKey, "Alice");
            blockchain.addAuthorizedKey(bobKey, "Bob");
            
            // 3. Add blocks
            blockchain.addBlock("Alice registers", alice.getPrivate(), alice.getPublic());
            blockchain.addBlock("Bob joins network", bob.getPrivate(), bob.getPublic());
            blockchain.addBlock("Alice sends payment", alice.getPrivate(), alice.getPublic());
            
            // 4. Search and validate
            List<Block> payments = blockchain.searchBlocksByContent("payment");
            System.out.println("Payment blocks found: " + payments.size());
            
            ChainValidationResult result = blockchain.validateChainDetailed();
            System.out.println("Blockchain is structurally intact: " + result.isStructurallyIntact());
            System.out.println("Blockchain is fully compliant: " + result.isFullyCompliant());
            
            // 5. Backup
            blockchain.exportChain("blockchain_backup.json");
            System.out.println("Blockchain backed up successfully!");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
```

## 📝 API Reference

### Core Methods

#### Blockchain Management
```java
// Basic information
long totalBlocks = blockchain.getBlockCount();
List<Block> allBlocks = blockchain.getAllBlocks();
Block lastBlock = blockchain.getLastBlock();
Block specificBlock = blockchain.getBlock(blockNumber);

// Configuration
int maxBytes = blockchain.getMaxBlockSizeBytes();
int maxChars = blockchain.getMaxBlockDataLength();
```

#### Key Management
```java
// Add/remove authorized keys
boolean added = blockchain.addAuthorizedKey(publicKeyString, "User Name");
boolean revoked = blockchain.revokeAuthorizedKey(publicKeyString);

// Get authorized keys
List<AuthorizedKey> activeKeys = blockchain.getAllAuthorizedKeys();
```

### DAO Methods

#### AuthorizedKeyDAO Methods
```java
// Save a new authorized key to the database
public void saveAuthorizedKey(AuthorizedKey authorizedKey) {
    // Persists a new authorized key with transaction management
}

// Check if a public key is currently authorized and active
public boolean isKeyAuthorized(String publicKey) {
    // Returns true if the key exists and is active
}

// Get all currently active authorized keys
public List<AuthorizedKey> getActiveAuthorizedKeys() {
    // Returns only the most recent authorization for each public key that is active
}

// Get ALL authorized keys (including revoked ones)
public List<AuthorizedKey> getAllAuthorizedKeys() {
    // Returns all keys for export functionality and historical validation
}

// Revoke an authorized key with proper temporal tracking
public void revokeAuthorizedKey(String publicKey) {
    // Sets isActive to false and records revocation timestamp
}

// Check if a public key was authorized at a specific time
public boolean wasKeyAuthorizedAt(String publicKey, LocalDateTime timestamp) {
    // Used for validating historical blocks
}

// Find an authorized key by owner name
public AuthorizedKey getAuthorizedKeyByOwner(String ownerName) {
    // Returns the most recent active authorization for the given owner
}

// Delete a specific authorized key by public key
public boolean deleteAuthorizedKey(String publicKey) {
    // Deletes ALL records for this public key (active and revoked)
}

// Delete all authorized keys (for import functionality)
public int deleteAllAuthorizedKeys() {
    // Removes all keys from the database
}
```

#### BlockDAO Methods
```java
// Get the next block number atomically (thread-safe)
public Long getNextBlockNumberAtomic() {
    // Atomically increments and returns the next block number from the sequence
    // Uses BlockSequence entity with pessimistic locking for true atomicity
    // This method is fully thread-safe and prevents race conditions in concurrent environments
}

// Get the last block with pessimistic locking to prevent race conditions
public Block getLastBlockWithLock() {
    // Returns the last block with a pessimistic lock to prevent concurrent modifications
    // Used in high-concurrency scenarios where consistency is critical
}

// Get the last block with forced refresh to see latest committed data
public Block getLastBlockWithRefresh() {
    // Returns the last block with a forced refresh from the database
    // Ensures we see the most recent data even in high-concurrency scenarios
    // Prevents race conditions where reads happen before writes are fully committed
}

// Check if a block with specific number exists
public boolean existsBlockWithNumber(Long blockNumber) {
    // Returns true if a block with the specified number exists
    // Used for race condition detection in concurrent environments
}

// Synchronize the block sequence with the actual max block number
public void synchronizeBlockSequence() {
    // Synchronizes the BlockSequence with the actual max block number in the database
    // Useful for initialization or after manual database operations
}

// Save a new block to the database
public void saveBlock(Block block) {
    // Persists a new block with transaction management
}

// Get a block by its number
public Block getBlockByNumber(Long blockNumber) {
    // Returns the block with the specified number or null if not found
}

// Get the last block in the chain
public Block getLastBlock() {
    // Returns the block with the highest block number or null if chain is empty
}

// Get all blocks in the chain ordered by number
public List<Block> getAllBlocks() {
    // Returns all blocks in ascending order by block number
}

// Get blocks within a time range
public List<Block> getBlocksByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
    // Returns blocks with timestamps between startTime and endTime
}

// Get the total number of blocks
public long getBlockCount() {
    // Returns the total number of blocks in the chain
}

// Check if a block with a specific hash exists
public boolean existsBlockWithHash(String hash) {
    // Returns true if a block with the specified hash exists
}

// Delete a block by its number
public boolean deleteBlockByNumber(Long blockNumber) {
    // Deletes the block with the specified number
}

// Delete blocks with block numbers greater than the specified number
public int deleteBlocksAfter(Long blockNumber) {
    // Deletes all blocks with block numbers greater than the specified number
}

// Delete all blocks (for import functionality)
public int deleteAllBlocks() {
    // Removes all blocks from the database
}

// Search blocks by content (case-insensitive)
public List<Block> searchBlocksByContent(String content) {
    // Returns blocks containing the specified content (case-insensitive)
}

// Get block by hash
public Block getBlockByHash(String hash) {
    // Returns the block with the specified hash or null if not found
}

// Add authorized key with specific timestamp (for CLI operations)
boolean added = blockchain.addAuthorizedKey(publicKeyString, "User Name", specificTimestamp);

// List authorized keys
List<AuthorizedKey> activeKeys = blockchain.getAuthorizedKeys();

// Get all authorized keys (including revoked ones) for export functionality
List<AuthorizedKey> allKeys = blockchain.getAllAuthorizedKeys();
```

#### Block Operations
```java
// Add block
boolean success = blockchain.addBlock(data, privateKey, publicKey);

// Validate with detailed results
ChainValidationResult result = blockchain.validateChainDetailed();
boolean isStructurallyIntact = result.isStructurallyIntact();
boolean isFullyCompliant = result.isFullyCompliant();

// Advanced operations
boolean exported = blockchain.exportChain("backup.json");
boolean imported = blockchain.importChain("backup.json");
boolean rolledBack = blockchain.rollbackBlocks(numberOfBlocks);
```

#### Search Operations
```java
// New hybrid search methods with multiple levels
List<Block> fastResults = blockchain.searchBlocks("searchTerm", SearchLevel.FAST_ONLY);
List<Block> dataResults = blockchain.searchBlocks("searchTerm", SearchLevel.INCLUDE_DATA);
List<Block> exhaustiveResults = blockchain.searchBlocks("searchTerm", SearchLevel.EXHAUSTIVE_OFFCHAIN);

// Convenience methods for different search levels
List<Block> quickSearch = blockchain.searchBlocksFast("keyword");        // Keywords only
List<Block> completeSearch = blockchain.searchBlocksComplete("content"); // All content

// Search by content category
List<Block> categoryResults = blockchain.searchByCategory("MEDICAL");

// Legacy search methods (still supported)
List<Block> contentResults = blockchain.searchBlocksByContent("searchTerm");
Block hashResult = blockchain.getBlockByHash("hashString");
List<Block> dateResults = blockchain.getBlocksByDateRange(startDate, endDate);

// Add blocks with keywords and categories
boolean success = blockchain.addBlockWithKeywords(data, manualKeywords, category, privateKey, publicKey);
```

### Blockchain Class API

#### Constructor
```java
public Blockchain()
```
Creates a new blockchain instance. Automatically creates the genesis block if this is the first time running.

#### Block Management Methods

```java
public boolean addBlock(String data, PrivateKey privateKey, PublicKey publicKey)
```
- **Parameters:**
  - `data`: The content to store in the block (max 10,000 characters)
  - `privateKey`: Private key for signing the block
  - `publicKey`: Public key for verification (must be authorized)
- **Returns:** `true` if block was added successfully, `false` otherwise
- **Throws:** `IllegalArgumentException` if data is too large or keys are invalid

```java
public Block addBlockAndReturn(String data, PrivateKey privateKey, PublicKey publicKey)
```
- **Parameters:**
  - `data`: The content to store in the block (max 10,000 characters)
  - `privateKey`: Private key for signing the block
  - `publicKey`: Public key for verification (must be authorized)
- **Returns:** The created `Block` object if successful, `null` if failed
- **Description:** Similar to `addBlock()` but returns the actual created block. Useful for concurrent scenarios where you need immediate access to block metadata (number, hash, timestamp).
- **Thread-Safety:** Fully thread-safe, uses the same global locking as `addBlock()`
- **Use Cases:**
  - ✅ Concurrent testing scenarios where you need the exact block created
  - ✅ Applications that need immediate access to block metadata
  - ✅ When you need to verify block creation success AND get block details

```java
public Block addBlockWithKeywords(String data, String[] manualKeywords, String category, 
                                PrivateKey privateKey, PublicKey publicKey)
```
- **Parameters:**
  - `data`: The content to store in the block (max 10,000 characters)
  - `manualKeywords`: Array of manual keywords for search indexing (optional, can be null)
  - `category`: Content category (e.g., "MEDICAL", "FINANCE", "TECHNICAL", "LEGAL")
  - `privateKey`: Private key for signing the block
  - `publicKey`: Public key for verification (must be authorized)
- **Returns:** The created `Block` object with search metadata populated
- **Description:** Enhanced block creation with search functionality. Combines manual keywords with automatic keyword extraction to enable efficient hybrid search capabilities.
- **Thread-Safety:** Fully thread-safe with global locking
- **Search Features:**
  - **Manual Keywords**: User-specified terms for targeted search results
  - **Automatic Extraction**: System extracts universal keywords (dates, numbers, emails, codes)
  - **Content Categories**: Enables category-based filtering and organization
  - **Combined Indexing**: Creates searchable content from manual + automatic keywords
- **Use Cases:**
  - ✅ Medical records with patient IDs and diagnostic codes
  - ✅ Financial transactions with project codes and amounts
  - ✅ Legal documents with contract references and dates
  - ✅ Technical documentation with API references and versions

```java
public ChainValidationResult validateChainDetailed()
```
- **Returns:** A `ChainValidationResult` object containing detailed validation information
- **Description:** Performs a comprehensive validation of the blockchain, including:
  - Structural integrity (hashes, links between blocks)
  - Cryptographic signatures
  - Authorization compliance
  - Key revocation status
  - Temporal consistency
- **Performance:** O(n) where n is the number of blocks in the chain
- **Thread Safety:** Thread-safe, can be called concurrently
- **Example:**
  ```java
  // Basic validation
  ChainValidationResult result = blockchain.validateChainDetailed();
  if (!result.isStructurallyIntact()) {
      System.err.println("Blockchain is corrupted!");
  } else if (!result.isFullyCompliant()) {
      System.out.println("Warning: Some blocks have authorization issues");
  } else {
      System.out.println("Blockchain is valid and fully compliant");
  }
  
  // Advanced validation with detailed reporting
  result = blockchain.validateChainDetailed();
  if (!result.getInvalidBlocks().isEmpty()) {
      System.err.println("Invalid blocks found:");
      for (Block block : result.getInvalidBlocks()) {
          System.err.println(" - Block " + block.getBlockNumber() + " (" + block.getHash() + ")");
      }
  }
  
  // Get a summary of validation results
  System.out.println("Validation summary: " + result.toString());
  ```
  - Returns detailed information about any issues found, including lists of invalid and revoked blocks
- **Example:**
  ```java
  ChainValidationResult result = blockchain.validateChainDetailed();
  if (result.isStructurallyIntact()) {
      System.out.println("Blockchain structure is valid");
      if (result.isFullyCompliant()) {
          System.out.println("All blocks are properly authorized");
      } else {
          System.out.println("Warning: " + result.getRevokedBlocks().size() + " blocks have authorization issues");
      }
  } else {
      System.err.println("Error: " + result.getInvalidBlocks().size() + " blocks have structural issues");
  }
  ```

```java
public long getBlockCount()
```
- **Returns:** Total number of blocks in the chain (including genesis block)

```java
public List<Block> getAllBlocks()
```
- **Returns:** List of all blocks in the blockchain, ordered by block number

```java
public Block getLastBlock()
```
- **Returns:** The most recently added block, or null if only genesis block exists

```java
public Block getBlock(long blockNumber)
```
- **Parameters:** `blockNumber`: The block number to retrieve (0 = genesis block)
- **Returns:** The block at the specified position, or null if not found

#### Key Management Methods

```java
public boolean addAuthorizedKey(String publicKey, String ownerName)
```
- **Parameters:**
  - `publicKey`: Base64-encoded public key string
  - `ownerName`: Human-readable name for the key owner
- **Returns:** `true` if key was added, `false` if it already exists
- **Description:** Adds a public key to the list of authorized signers

```java
public boolean addAuthorizedKey(String publicKey, String ownerName, LocalDateTime timestamp)
```
- **Parameters:**
  - `publicKey`: Base64-encoded public key string
  - `ownerName`: Human-readable name for the key owner
  - `timestamp`: Specific creation timestamp (for import/CLI operations)
- **Returns:** `true` if key was added successfully
- **Description:** Adds key with specific timestamp for temporal consistency

```java
public boolean revokeAuthorizedKey(String publicKey)
```
- **Parameters:** `publicKey`: The public key to revoke
- **Returns:** `true` if key was revoked, `false` if key was not found
- **Description:** Marks a key as inactive (revoked keys cannot sign new blocks)

#### Key Deletion Methods ⚠️

```java
public KeyDeletionImpact canDeleteAuthorizedKey(String publicKey)
```
- **Parameters:** `publicKey`: The public key to analyze
- **Returns:** `KeyDeletionImpact` object with safety information
- **Description:** **RECOMMENDED FIRST STEP** - Analyzes the impact of deleting a key without actually deleting it. Shows how many historical blocks would be affected.

```java
public boolean deleteAuthorizedKey(String publicKey)
```
- **Parameters:** `publicKey`: The public key to delete
- **Returns:** `true` if key was safely deleted, `false` if deletion was blocked
- **Description:** **SAFE DELETION** - Only deletes keys that haven't signed any blocks. Will refuse to delete keys with historical blocks and suggest using dangerous deletion if absolutely necessary.

```java
public boolean dangerouslyDeleteAuthorizedKey(String publicKey, String reason)
```
- **Parameters:** 
  - `publicKey`: The public key to delete
  - `reason`: Reason for deletion (for audit logging)
- **Returns:** `true` if key was deleted, `false` if deletion was blocked
- **Description:** **DANGEROUS DELETION (Safe Mode)** - Attempts to delete key but will still refuse if it affects historical blocks unless force is used.

```java
public boolean dangerouslyDeleteAuthorizedKey(String publicKey, boolean force, String reason)
```
- **Parameters:** 
  - `publicKey`: The public key to delete permanently
  - `force`: If `true`, deletes even if it affects historical blocks
  - `reason`: Reason for deletion (for audit logging)
- **Returns:** `true` if key was deleted, `false` if deletion failed
- **Description:** **EXTREMELY DANGEROUS DELETION** - Can permanently remove keys even if they signed historical blocks. ⚠️ **WARNING**: Using `force=true` will break blockchain validation for affected blocks. This operation is **IRREVERSIBLE**. Only use for GDPR compliance, security incidents, or emergency situations.

**Key Deletion Safety Levels:**
1. 🟢 **`canDeleteAuthorizedKey()`** - Analysis only, no deletion
2. 🟡 **`deleteAuthorizedKey()`** - Safe deletion, blocks dangerous operations
3. 🟠 **`dangerouslyDeleteAuthorizedKey(key, reason)`** - Dangerous but still protected
4. 🔴 **`dangerouslyDeleteAuthorizedKey(key, true, reason)`** - Nuclear option, breaks validation

#### Key Deletion Impact Analysis

```java
public static class KeyDeletionImpact {
    public boolean keyExists()           // Key exists in database
    public boolean canSafelyDelete()     // Safe to delete (no blocks affected)
    public boolean isSevereImpact()      // Would affect historical blocks
    public long getAffectedBlocks()      // Number of blocks that would be affected
    public String getMessage()           // Human-readable impact description
}
```

```java
public List<AuthorizedKey> getAuthorizedKeys()
```
- **Returns:** List of currently active authorized keys
- **Description:** Returns only keys that are currently active (not revoked)

```java
public List<AuthorizedKey> getAllAuthorizedKeys()
```
- **Returns:** List of all authorized keys (including revoked ones)
- **Description:** Used for export functionality to preserve complete key history

#### Search Methods

##### Hybrid Search System

The blockchain includes a comprehensive hybrid search system with multiple search levels and automatic keyword extraction.

```java
public List<Block> searchBlocks(String searchTerm, SearchLevel level)
```
- **Parameters:** 
  - `searchTerm`: Text to search for (case-insensitive, minimum 4 characters with intelligent exceptions)
  - `level`: Search level (`FAST_ONLY`, `INCLUDE_DATA`, or `EXHAUSTIVE_OFFCHAIN`)
- **Returns:** List of blocks matching the search term at the specified level
- **Description:** Primary search method with configurable search depth
- **Search Levels:**
  - `FAST_ONLY`: Searches only in keywords (fastest performance)
  - `INCLUDE_DATA`: Searches in keywords + block data (balanced performance)  
  - `EXHAUSTIVE_OFFCHAIN`: Searches everything including off-chain content (comprehensive)
- **Thread-Safety:** Fully thread-safe with read locks

```java
public List<Block> searchBlocksFast(String searchTerm)
```
- **Parameters:** `searchTerm`: Text to search for (keywords only)
- **Returns:** List of blocks with matching keywords
- **Description:** Convenience method for `searchBlocks(searchTerm, SearchLevel.FAST_ONLY)`
- **Performance:** Optimized for speed, searches only indexed keywords

```java
public List<Block> searchBlocksComplete(String searchTerm)
```
- **Parameters:** `searchTerm`: Text to search for (all content including off-chain)
- **Returns:** List of blocks with matching content across all sources
- **Description:** Convenience method for `searchBlocks(searchTerm, SearchLevel.EXHAUSTIVE_OFFCHAIN)`
- **Performance:** Comprehensive but slower, includes off-chain data search

```java
public List<Block> searchByCategory(String category)
```
- **Parameters:** `category`: Content category to filter by (case-insensitive)
- **Returns:** List of blocks in the specified category
- **Description:** Filters blocks by their assigned content category
- **Example Categories:** "MEDICAL", "FINANCE", "TECHNICAL", "LEGAL"

##### Search Term Validation

**Note:** Search term validation is now handled automatically by the Advanced Search Engine. The system applies intelligent validation rules internally.

**Validation Rules (Applied Automatically):**
- Minimum 4 characters (general rule)
- **Automatic Exceptions:** Years (2024), acronyms (API, SQL), technical terms (XML, JSON), numbers, IDs
- **Valid Examples:** "medical", "2024", "API", "XML", "123", "ID-001"
- **Invalid Examples:** "hi", "a", "", null, whitespace-only strings

**Usage:** Simply pass search terms to search methods - validation is handled internally:
```java
// The search engine validates terms automatically
List<Block> results = blockchain.searchBlocksFast("medical");  // ✅ Valid
List<Block> results = blockchain.searchBlocksFast("API");      // ✅ Valid (exception)
List<Block> results = blockchain.searchBlocksFast("hi");       // Returns empty (too short)
```

##### Legacy Search Methods (Still Supported)

```java
public List<Block> searchBlocksByContent(String searchTerm)
```
- **Parameters:** `searchTerm`: Text to search for (case-insensitive)
- **Returns:** List of blocks containing the search term
- **Description:** Legacy method - searches through all block data content

```java
public Block getBlockByHash(String hash)
```
- **Parameters:** `hash`: SHA3-256 hash of the block
- **Returns:** The block with matching hash, or null if not found

```java
public List<Block> getBlocksByDateRange(LocalDate startDate, LocalDate endDate)
```
- **Parameters:**
  - `startDate`: Start date (inclusive)
  - `endDate`: End date (inclusive)
- **Returns:** List of blocks created within the date range

#### Advanced Operations

```java
public boolean exportChain(String filename)
```
- **Parameters:** `filename`: Path to the output JSON file
- **Returns:** `true` if export was successful
- **Description:** Exports the entire blockchain and keys to a JSON file

```java
public boolean importChain(String filename)
```
- **Parameters:** `filename`: Path to the JSON file to import
- **Returns:** `true` if import was successful
- **Description:** Imports blockchain data from JSON file (replaces current chain)

```java
public boolean rollbackBlocks(int numberOfBlocks)
```
- **Parameters:** `numberOfBlocks`: Number of recent blocks to remove
- **Returns:** `true` if rollback was successful
- **Description:** Removes the specified number of most recent blocks

```java
public boolean rollbackToBlock(long blockNumber)
```
- **Parameters:** `blockNumber`: Keep blocks from 0 to this number (inclusive)
- **Returns:** `true` if rollback was successful
- **Description:** Removes all blocks after the specified block number

#### Configuration Methods

```java
public int getMaxBlockSizeBytes()
```
- **Returns:** Maximum block size in bytes (default: 1,048,576 bytes = 1MB)

```java
public int getMaxBlockDataLength()
```
- **Returns:** Maximum characters allowed in block data (default: 10,000)

#### Off-Chain Storage Configuration Methods

```java
public void setMaxBlockSizeBytes(int maxSizeBytes)
```
- **Parameters:** `maxSizeBytes`: New maximum block size (1 to 10MB)
- **Description:** Updates the on-chain block size limit at runtime

```java
public void setMaxBlockDataLength(int maxDataLength)
```
- **Parameters:** `maxDataLength`: New maximum data length (1 to 1M characters)
- **Description:** Updates the on-chain data length limit at runtime

```java
public void setOffChainThresholdBytes(int thresholdBytes)
```
- **Parameters:** `thresholdBytes`: New off-chain storage threshold
- **Description:** Sets the size threshold for automatic off-chain storage

```java
public int getCurrentMaxBlockSizeBytes()
```
- **Returns:** Current maximum block size in bytes

```java
public int getCurrentMaxBlockDataLength()
```
- **Returns:** Current maximum data length in characters

```java
public int getCurrentOffChainThresholdBytes()
```
- **Returns:** Current off-chain storage threshold in bytes

```java
public String getConfigurationSummary()
```
- **Returns:** Human-readable summary of current configuration settings

```java
public void resetLimitsToDefault()
```
- **Description:** Resets all size limits to their default values

#### Off-Chain Data Management Methods

```java
public String getCompleteBlockData(Block block)
```
- **Parameters:** `block`: The block to retrieve complete data for
- **Returns:** Complete block data (retrieves from off-chain storage if needed)
- **Throws:** `Exception` if off-chain data cannot be retrieved

```java
public boolean verifyOffChainIntegrity(Block block)
```
- **Parameters:** `block`: The block to verify off-chain data integrity
- **Returns:** `true` if off-chain data is valid or block has no off-chain data

```java
public boolean verifyAllOffChainIntegrity()
```
- **Returns:** `true` if all off-chain data in the blockchain passes integrity checks
- **Description:** Verifies integrity of all off-chain data files

```java
public int validateAndDetermineStorage(String data)
```
- **Parameters:** `data`: The data to validate and classify
- **Returns:** 0 = invalid, 1 = store on-chain, 2 = store off-chain
- **Description:** Determines storage strategy based on data size and current configuration

### CryptoUtil Class API

#### Key Generation
```java
public static KeyPair generateKeyPair()
```
- **Returns:** A new EC key pair (curve secp256r1)
- **Description:** Generates cryptographically secure key pair for blockchain operations

```java
public static String publicKeyToString(PublicKey publicKey)
```
- **Parameters:** `publicKey`: The public key to encode
- **Returns:** Base64-encoded string representation of the public key

```java
public static PublicKey stringToPublicKey(String publicKeyString)
```
- **Parameters:** `publicKeyString`: Base64-encoded public key string
- **Returns:** PublicKey object
- **Throws:** `IllegalArgumentException` if string is not a valid public key

#### Hashing and Signing
```java
public static String calculateHash(String data)
```
- **Parameters:** `data`: The data to hash
- **Returns:** SHA3-256 hash as hexadecimal string

```java
public static String signData(String data, PrivateKey privateKey)
```
- **Parameters:**
  - `data`: The data to sign
  - `privateKey`: Private key for signing
- **Returns:** Base64-encoded signature string

```java
public static boolean verifySignature(String data, String signature, PublicKey publicKey)
```
- **Parameters:**
  - `data`: Original data that was signed
  - `signature`: Base64-encoded signature to verify
  - `publicKey`: Public key for verification
- **Returns:** `true` if signature is valid, `false` otherwise

### Block Class

#### Properties
- `id`: Unique database identifier
- `blockNumber`: Sequential number in the chain (0 = genesis)
- `previousHash`: Hash of the previous block
- `data`: The block content (user data)
- `hash`: SHA3-256 hash of this block
- `signature`: Digital signature of the block
- `signerPublicKey`: Public key that signed this block
- `timestamp`: When the block was created

#### Methods
```java
public long getBlockNumber()
public String getPreviousHash()
public String getData()
public String getHash()
public String getSignature()
public String getSignerPublicKey()
public LocalDateTime getTimestamp()
```

### AuthorizedKey Class

#### Properties
- `id`: Unique database identifier
- `publicKey`: Base64-encoded public key
- `ownerName`: Human-readable owner name
- `isActive`: Whether the key is currently active
- `createdAt`: When the key was added
- `revokedAt`: When the key was revoked (null if still active)

#### Methods
```java
public String getPublicKey()
public String getOwnerName()
public boolean isActive()
public LocalDateTime getCreatedAt()
public LocalDateTime getRevokedAt()
```

## ⚙️ Configuration

### Database Configuration
- **Location**: `blockchain.db` in project root directory
- **Type**: SQLite database
- **JPA Standard**: Using JPA with Hibernate as implementation provider
- **Configuration**: `persistence.xml` for JPA settings
- **Logging**: SQL queries logged (can be disabled in persistence.xml)

### Size Limits
- **Block Data**: 10,000 characters maximum
- **Block Size**: 1MB (1,048,576 bytes) maximum
- **Hash Length**: 64 characters (SHA3-256)

### Security Configuration
- **Hash Algorithm**: SHA3-256
- **Signature Algorithm**: ECDSA
- **Key Size**: 2048 bits (default)

For detailed technical specifications and production considerations, see [TECHNICAL_DETAILS.md](TECHNICAL_DETAILS.md) and [PRODUCTION_GUIDE.md](PRODUCTION_GUIDE.md).
, 12, 31);
List<Block> dateResults = blockchain.getBlocksByDateRange(startDate, endDate);

// Advanced filtering with streams
List<Block> filteredBlocks = blockchain.getAllBlocks().stream()
    .filter(block -> block.getData().contains("keyword"))
    .filter(block -> block.getTimestamp().isAfter(someDateTime))
    .filter(block -> block.getSignerPublicKey().equals(specificPublicKey))
    .collect(Collectors.toList());
```

### Cryptographic Utility Methods

#### Key Generation and Management
```java
// Generate new EC key pair
KeyPair keyPair = CryptoUtil.generateKeyPair();

// Convert keys to/from string format
String publicKeyString = CryptoUtil.publicKeyToString(publicKey);
PublicKey publicKey = CryptoUtil.stringToPublicKey(publicKeyString);
String privateKeyString = CryptoUtil.privateKeyToString(privateKey);
PrivateKey privateKey = CryptoUtil.stringToPrivateKey(privateKeyString);

// Calculate hash
String hash = CryptoUtil.calculateHash(data);

// Digital signature operations
String signature = CryptoUtil.signData(data, privateKey);
boolean verified = CryptoUtil.verifySignature(data, signature, publicKey);
```

#### Block Content Analysis
```java
// Block content analysis (example)
String blockContent = "Block #" + block.getBlockNumber() + ": " + block.getData();

// Get block size information (example)
Long blockDataLength = (long) block.getData().length();
long estimatedSize = blockDataLength * 2; // rough estimate

// Verify block hash manually (example for validation)
boolean hashValid = block.getHash().equals(CryptoUtil.calculateHash(block.getData()));
```

### Advanced Configuration Methods

#### Database Configuration
```java
// Get current database statistics (example)
File dbFile = new File("blockchain.db");
long databaseSize = dbFile.length();

// Performance monitoring example
long blockCount = blockchain.getBlockCount();
System.out.println("Current performance: " + blockCount + " blocks");
```

#### Security Configuration
```java
// Security configuration constants (example)
String hashAlgorithm = "SHA3-256";        
String signatureAlgorithm = "SHA3withECDSA"; 
String curveName = "secp256r1";                

// Validate blockchain integrity
ChainValidationResult securityResult = blockchain.validateChainDetailed();
boolean securityValid = securityResult.isFullyCompliant();
```

## 🔐 EncryptionConfig Integration

The blockchain system now supports unified encryption configuration across all APIs through the `EncryptionConfig` class. This provides consistent security policies and flexible performance tuning.

### UserFriendlyEncryptionAPI Configuration

```java
// Default configuration (Balanced)
UserFriendlyEncryptionAPI api = new UserFriendlyEncryptionAPI(blockchain);

// Custom configuration
EncryptionConfig config = EncryptionConfig.createHighSecurityConfig();
UserFriendlyEncryptionAPI api = new UserFriendlyEncryptionAPI(blockchain, config);
```

### SearchSpecialistAPI Configuration

```java
// Default configuration (High Security)
SearchSpecialistAPI searchAPI = new SearchSpecialistAPI(blockchain, password, privateKey);

// Custom configuration
EncryptionConfig config = EncryptionConfig.createPerformanceConfig();
SearchSpecialistAPI searchAPI = new SearchSpecialistAPI(blockchain, password, privateKey, config);
```

### Available Configuration Types

#### Predefined Configurations
```java
// High Security: Maximum protection, slower performance
EncryptionConfig highSec = EncryptionConfig.createHighSecurityConfig();

// Performance: Optimized speed, reduced security
EncryptionConfig performance = EncryptionConfig.createPerformanceConfig();

// Balanced: Good compromise between security and performance
EncryptionConfig balanced = EncryptionConfig.createBalancedConfig();
```

#### Custom Configuration
```java
EncryptionConfig custom = EncryptionConfig.builder()
    .keyLength(256)                          // AES key length in bits
    .pbkdf2Iterations(120000)               // PBKDF2 iteration count
    .enableCompression(false)               // Enable/disable compression
    .corruptionDetectionEnabled(true)       // Enable corruption detection
    .metadataEncryptionEnabled(true)        // Encrypt metadata
    .validateEncryptionFormat(true)         // Validate encryption format
    .build();
```

### Configuration Properties

| Property | High Security | Performance | Balanced | Description |
|----------|---------------|-------------|----------|-------------|
| Key Length | 256 bits | 128 bits | 256 bits | AES encryption key length |
| PBKDF2 Iterations | 150,000 | 50,000 | 100,000 | Key derivation iterations |
| Compression | Disabled | Enabled | Disabled | Data compression |
| Corruption Detection | Enabled | Disabled | Enabled | Data integrity checking |
| Metadata Encryption | Enabled | Disabled | Enabled | Encrypt metadata |
| Format Validation | Enabled | Disabled | Enabled | Validate encryption format |

### Unified Configuration Example

```java
// Create organization-wide security policy
EncryptionConfig orgPolicy = EncryptionConfig.builder()
    .keyLength(256)
    .pbkdf2Iterations(120000)
    .enableCompression(false)
    .corruptionDetectionEnabled(true)
    .metadataEncryptionEnabled(true)
    .validateEncryptionFormat(true)
    .build();

// Apply to all APIs
UserFriendlyEncryptionAPI dataAPI = new UserFriendlyEncryptionAPI(blockchain, orgPolicy);
SearchSpecialistAPI searchAPI = new SearchSpecialistAPI(blockchain, password, privateKey, orgPolicy);

// All APIs now use consistent security settings
System.out.println("Configuration: " + orgPolicy.getSummary());
System.out.println("Security Level: " + orgPolicy.getSecurityLevel());
```

### Configuration Validation

```java
// Check current configuration
EncryptionConfig config = api.getEncryptionConfig();
System.out.println("Current Security Level: " + config.getSecurityLevel());

// Validate security requirements
if (config.getKeyLength() < 256) {
    System.out.println("⚠️ Warning: Key length below recommended 256 bits");
}

if (config.getPbkdf2Iterations() < 100000) {
    System.out.println("⚠️ Warning: PBKDF2 iterations below recommended 100,000");
}
```

For complete details on EncryptionConfig usage, see the [EncryptionConfig Integration Guide](ENCRYPTION_CONFIG_INTEGRATION_GUIDE.md).

## 🔧 Configuration Parameters

### Size and Performance Limits
```properties
# On-Chain Block constraints (configurable at runtime)
blockchain.block.max_data_size=10000           # 10,000 characters (default)
blockchain.block.max_size_bytes=1048576        # 1MB (1,048,576 bytes) (default)
blockchain.block.max_hash_length=64            # SHA3-256 hash length

# Off-Chain Storage settings
blockchain.offchain.threshold_bytes=524288     # 512KB threshold (default)
blockchain.offchain.max_file_size=104857600    # 100MB maximum per file
blockchain.offchain.storage_directory=off-chain-data  # Storage directory
blockchain.offchain.encryption_algorithm=AES/GCM/NoPadding  # Encryption method

# Database settings
blockchain.database.connection_timeout=30000   # 30 seconds
blockchain.database.max_connections=20         # Connection pool size
blockchain.database.batch_size=25              # Batch operations

# Security settings
blockchain.security.curve_name=secp256r1          # ECDSA curve name
blockchain.security.signature_algorithm=SHA3withECDSA
blockchain.security.hash_algorithm=SHA3-256

# Performance settings
blockchain.performance.chain_validation_batch=100    # Batch validation size
blockchain.performance.search_max_results=1000       # Max search results
blockchain.performance.export_buffer_size=8192       # Export buffer size

# Operational limits
blockchain.operations.max_rollback_depth=100         # Max rollback blocks
blockchain.operations.backup_retention_days=30      # Backup retention
blockchain.operations.log_retention_days=90         # Log retention
```

### Environment-Specific Configuration
```java
public class BlockchainConfig {
    public static final class Development {
        public static final boolean ENABLE_SQL_LOGGING = true;
        public static final boolean ENABLE_PERFORMANCE_METRICS = true;
        public static final int MAX_BLOCKS_IN_MEMORY = 1000;
        public static final boolean AUTO_CREATE_SCHEMA = true;
    }
    
    public static final class Production {
        public static final boolean ENABLE_SQL_LOGGING = false;
        public static final boolean ENABLE_PERFORMANCE_METRICS = false;
        public static final int MAX_BLOCKS_IN_MEMORY = 10000;
        public static final boolean AUTO_CREATE_SCHEMA = false;
    }
    
    public static final class Testing {
        public static final boolean USE_IN_MEMORY_DATABASE = true;
        public static final boolean RESET_DATABASE_ON_START = true;
        public static final int TEST_TIMEOUT_SECONDS = 30;
    }
}
```

## 🔒 Thread-Safety and Concurrent Usage

The Private Blockchain is **fully thread-safe** and designed for concurrent multi-threaded applications. All operations use a global `ReentrantReadWriteLock` for synchronization.

### Thread-Safety Guarantees

- ✅ **Global Synchronization**: All blockchain instances share the same lock
- ✅ **Read-Write Separation**: Multiple reads can occur simultaneously
- ✅ **Exclusive Writes**: Write operations have exclusive access
- ✅ **Atomic Operations**: All operations are atomic and consistent
- ✅ **ACID Compliance**: Database operations use proper JPA transactions
- ✅ **Atomic Block Numbering**: Block numbers are generated atomically using the `BlockSequence` entity

### Thread-Safe APIs

The following APIs are fully thread-safe and can be used concurrently:

#### Core Blockchain APIs
- ✅ **Blockchain class**: All methods are thread-safe with global ReentrantReadWriteLock
- ✅ **UserFriendlyEncryptionAPI**: All 212 methods are thread-safe for concurrent access
- ✅ **SearchMetrics**: Thread-safe metrics collection with concurrent data structures
- ✅ **ChainValidationResult**: Immutable result objects safe for concurrent access

#### Search and Performance APIs  
- ✅ **SearchSpecialistAPI**: Thread-safe search operations with concurrent caching
- ✅ **GranularTermVisibilityAPI**: Safe for concurrent term visibility operations
- ✅ **PerformanceMetricsService**: Concurrent monitoring with thread-safe collectors

#### Collection Returns and Immutability

⚠️  **Important**: Many APIs return immutable collections using `Collections.unmodifiableList()` and `Collections.unmodifiableMap()`. While these collections are safe to read concurrently, attempts to modify them will throw `UnsupportedOperationException`:

```java
// Safe concurrent reads
List<Block> blocks = blockchain.getAllBlocks(); // Returns unmodifiable list
blocks.forEach(block -> processBlock(block)); // ✅ Safe concurrent iteration

// Modification attempts will fail
blocks.add(newBlock); // ❌ Throws UnsupportedOperationException
blocks.clear(); // ❌ Throws UnsupportedOperationException

// Thread-safe access to validation results
ChainValidationResult result = blockchain.validateChainDetailed();
List<Block> invalidBlocks = result.getInvalidBlocks(); // Immutable collection
List<Block> revokedBlocks = result.getRevokedBlocks(); // Immutable collection
```

For detailed thread safety implementation standards, see [THREAD_SAFETY_STANDARDS.md](THREAD_SAFETY_STANDARDS.md).

### Block Sequence for Thread-Safe Block Numbering

The `BlockSequence` entity provides atomic, thread-safe block number generation to prevent race conditions in high-concurrency environments:

```java
// Thread-safe block number generation using BlockSequence
public Long getNextBlockNumber() {
    EntityManager em = JPAUtil.getEntityManager();
    EntityTransaction tx = em.getTransaction();
    
    try {
        tx.begin();
        
        // Use pessimistic locking to prevent concurrent updates
        BlockSequence sequence = em.find(BlockSequence.class, "block_number", 
                                      LockModeType.PESSIMISTIC_WRITE);
        
        if (sequence == null) {
            // Initialize if not exists
            sequence = new BlockSequence();
            sequence.setSequenceName("block_number");
            sequence.setNextValue(1L);
            em.persist(sequence);
        } else {
            // Increment the sequence
            Long nextVal = sequence.getNextValue();
            sequence.setNextValue(nextVal + 1);
        }
        
        Long result = sequence.getNextValue();
        tx.commit();
        return result;
    } catch (Exception e) {
        if (tx.isActive()) {
            tx.rollback();
        }
        throw new RuntimeException("Failed to get next block number", e);
    } finally {
        em.close();
    }
}

### Concurrent Usage Patterns

#### Basic Concurrent Block Addition
```java
// Both methods are fully thread-safe
CompletableFuture<Boolean> future1 = CompletableFuture.supplyAsync(() -> 
    blockchain.addBlock(data1, privateKey, publicKey));

CompletableFuture<Block> future2 = CompletableFuture.supplyAsync(() -> 
    blockchain.addBlockAndReturn(data2, privateKey, publicKey));
```

#### When to Use addBlock() vs addBlockAndReturn()

**Use `addBlock()` when:**
- You only need to know if the operation succeeded
- You're doing simple sequential operations
- You don't need immediate access to block metadata

**Use `addBlockAndReturn()` when:**
- You need the created block's details (number, hash, timestamp)
- You're doing concurrent operations and need immediate results
- You're building applications that display block information
- You're doing testing that needs to verify specific block creation

#### Concurrent Read Operations
```java
// Multiple threads can read simultaneously (no blocking)
CompletableFuture<Long> blockCount = CompletableFuture.supplyAsync(blockchain::getBlockCount);
CompletableFuture<List<Block>> allBlocks = CompletableFuture.supplyAsync(blockchain::getAllBlocks); // Returns unmodifiable list
CompletableFuture<ChainValidationResult> validationResult = CompletableFuture.supplyAsync(blockchain::validateChainDetailed);

// All these can execute in parallel
CompletableFuture.allOf(blockCount, allBlocks, validationResult)
    .thenRun(() -> {
        System.out.println("All reads completed");
        
        // Access results - collections are immutable and thread-safe
        List<Block> blocks = allBlocks.join(); // Immutable collection
        ChainValidationResult result = validationResult.join();
        
        // Safe concurrent iteration
        blocks.parallelStream().forEach(block -> {
            // Thread-safe processing of each block
            System.out.println("Processing block #" + block.getBlockNumber());
        });
        
        // Access validation results safely
        List<Block> invalidBlocks = result.getInvalidBlocks(); // Immutable collection
        List<Block> revokedBlocks = result.getRevokedBlocks(); // Immutable collection
    });
```

### Performance Characteristics

- **Read Operations**: Excellent scalability with multiple concurrent readers
- **Write Operations**: Serialized for safety, optimized for throughput
- **Mixed Workloads**: Read-heavy applications perform very well
- **High Concurrency**: Tested and verified with 20+ concurrent threads

For practical concurrent usage examples, see [EXAMPLES.md](EXAMPLES.md#thread-safe-concurrent-usage-patterns).

### Security Best Practices

#### Key Management
```java
// ✅ GOOD: Secure key generation
KeyPair keyPair = CryptoUtil.generateKeyPair();
// Store private key securely (encrypted storage, HSM, etc.)

// ❌ BAD: Hardcoded keys
String hardcodedKey = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASC..."; // Never do this!

// ✅ GOOD: Key validation before use
if (blockchain.isKeyAuthorized(publicKeyString)) {
    blockchain.addBlock(data, privateKey, publicKey);
}

// ✅ GOOD: Key rotation
blockchain.addAuthorizedKey(newPublicKey, "User (Rotated)");
Thread.sleep(transitionPeriod); // Allow overlap
blockchain.revokeAuthorizedKey(oldPublicKey);
```

#### Data Validation
```java
// ✅ GOOD: Input validation
public boolean addSecureBlock(String data, PrivateKey privateKey, PublicKey publicKey) {
    // Validate input size
    if (data.length() > blockchain.getMaxBlockDataLength()) {
        throw new IllegalArgumentException("Block data exceeds maximum size");
    }
    
    // Validate key authorization
    String publicKeyString = CryptoUtil.publicKeyToString(publicKey);
    if (!blockchain.isKeyAuthorized(publicKeyString)) {
        throw new SecurityException("Key not authorized");
    }
    
    // Sanitize sensitive data
    String sanitizedData = sanitizeData(data);
    
    return blockchain.addBlock(sanitizedData, privateKey, publicKey);
}

private String sanitizeData(String data) {
    // Remove or encrypt sensitive information
    return data.replaceAll("\\b\\d{4}\\s?\\d{4}\\s?\\d{4}\\s?\\d{4}\\b", "[CARD_REDACTED]")
               .replaceAll("\\b\\d{3}-\\d{2}-\\d{4}\\b", "[SSN_REDACTED]");
}
```

### Off-Chain Storage Best Practices

#### Automatic Data Handling
```java
// ✅ GOOD: Let the blockchain automatically handle storage decisions
public void addDataBlock(String data, PrivateKey privateKey, PublicKey publicKey) {
    // Blockchain automatically determines if data goes on-chain or off-chain
    Block block = blockchain.addBlockAndReturn(data, privateKey, publicKey);
    
    if (block.hasOffChainData()) {
        System.out.println("Large data stored off-chain securely");
        // Optional: Verify integrity immediately
        boolean isValid = blockchain.verifyOffChainIntegrity(block);
        if (!isValid) {
            throw new RuntimeException("Off-chain data integrity check failed");
        }
    }
}

// ❌ BAD: Trying to manually manage off-chain storage
// Don't attempt to directly manage off-chain files - let the blockchain handle it
```

#### Configuration Management
```java
// ✅ GOOD: Configure thresholds based on your use case
public void configureForDocumentStorage() {
    // For document management system - store documents off-chain
    blockchain.setOffChainThresholdBytes(100 * 1024); // 100KB threshold
    blockchain.setMaxBlockSizeBytes(5 * 1024 * 1024); // 5MB on-chain limit
    
    System.out.println("Configuration updated:");
    System.out.println(blockchain.getConfigurationSummary());
}

public void configureForTransactionLogs() {
    // For transaction logging - keep more data on-chain
    blockchain.setOffChainThresholdBytes(2 * 1024 * 1024); // 2MB threshold
    blockchain.setMaxBlockSizeBytes(10 * 1024 * 1024); // 10MB on-chain limit
}

// ✅ GOOD: Reset to defaults when needed
public void resetToDefaults() {
    blockchain.resetLimitsToDefault();
}
```

#### Data Retrieval Patterns
```java
// ✅ GOOD: Always use getCompleteBlockData() for full data access
public String getBlockContent(Block block) {
    try {
        // This method handles both on-chain and off-chain data automatically
        return blockchain.getCompleteBlockData(block);
    } catch (Exception e) {
        System.err.println("Failed to retrieve block data: " + e.getMessage());
        return null;
    }
}

// ❌ BAD: Directly accessing block.getData() for blocks that might have off-chain data
public String getBadBlockContent(Block block) {
    String data = block.getData();
    // This might return "OFF_CHAIN_REF:hash..." instead of actual data!
    return data;
}

// ✅ GOOD: Check for off-chain data before processing
public void processBlock(Block block) {
    if (block.hasOffChainData()) {
        // Verify integrity before processing
        if (!blockchain.verifyOffChainIntegrity(block)) {
            throw new RuntimeException("Off-chain data corrupted for block " + block.getBlockNumber());
        }
        System.out.println("Processing block with off-chain data");
    }
    
    String fullData = blockchain.getCompleteBlockData(block);
    // Process the complete data...
}
```

#### Integrity Verification
```java
// ✅ GOOD: Regular integrity checks
public void performMaintenanceCheck() {
    System.out.println("Starting off-chain integrity verification...");
    
    boolean allValid = blockchain.verifyAllOffChainIntegrity();
    
    if (!allValid) {
        System.err.println("❌ Some off-chain data failed integrity checks!");
        // Handle corrupted data - alert administrators, backup recovery, etc.
        alertAdministrators("Off-chain data integrity issues detected");
    } else {
        System.out.println("✅ All off-chain data integrity checks passed");
    }
}

// ✅ GOOD: Verify individual blocks when suspicious
public boolean verifySpecificBlock(long blockNumber) {
    Block block = blockchain.getBlock(blockNumber);
    if (block != null && block.hasOffChainData()) {
        return blockchain.verifyOffChainIntegrity(block);
    }
    return true; // No off-chain data to verify
}
```

#### Production Considerations
```java
// ✅ GOOD: Monitor disk space for off-chain storage
public void monitorStorage() {
    File offChainDir = new File("off-chain-data");
    if (offChainDir.exists()) {
        long totalSize = calculateDirectorySize(offChainDir);
        long availableSpace = offChainDir.getFreeSpace();
        
        if (availableSpace < totalSize * 0.1) { // Less than 10% free space
            System.err.println("⚠️ Warning: Low disk space for off-chain storage");
            // Trigger cleanup, archival, or expansion procedures
        }
    }
}

// ✅ GOOD: Backup strategy for off-chain data
public void backupOffChainData() {
    // Include off-chain directory in backup procedures
    // Verify integrity before backup
    if (blockchain.verifyAllOffChainIntegrity()) {
        // Proceed with backup of off-chain-data/ directory
        performDirectoryBackup("off-chain-data", "backup-location");
    } else {
        System.err.println("Cannot backup - integrity check failed");
    }
}
```

### Performance Best Practices

#### Batch Operations
```java
// ✅ GOOD: Batch processing with JPA transaction management
public void addBlocksBatch(List<BlockData> blockDataList, 
                          PrivateKey signerKey, PublicKey signerPublic) {
    EntityManager em = JPAUtil.getEntityManager();
    EntityTransaction transaction = null;
    
    try {
        transaction = em.getTransaction();
        transaction.begin();
        
        for (int i = 0; i < blockDataList.size(); i++) {
            Block block = createBlock(blockDataList.get(i), signerKey, signerPublic);
            em.persist(block);
            
            // Flush every 25 blocks for memory management
            if (i % 25 == 0) {
                em.flush();
                em.clear();
            }
        }
        transaction.commit();
    } catch (Exception e) {
        if (transaction != null && transaction.isActive()) {
            transaction.rollback();
        }
        throw e;
    } finally {
        em.close();
    }
}

// ❌ BAD: Individual transactions for each block (inefficient in concurrent scenarios)
for (BlockData data : blockDataList) {
    blockchain.addBlock(data.getContent(), privateKey, publicKey); // Works but not optimal
}

// ✅ BETTER: Concurrent batch processing for improved performance
public void addBlocksBatchConcurrent(List<BlockData> blockDataList, 
                                   PrivateKey signerKey, PublicKey signerPublic) {
    ExecutorService executor = Executors.newFixedThreadPool(4);
    List<CompletableFuture<Block>> futures = new ArrayList<>();
    
    for (BlockData data : blockDataList) {
        CompletableFuture<Block> future = CompletableFuture.supplyAsync(() -> {
            return blockchain.addBlockAndReturn(data.getContent(), signerKey, signerPublic);
        }, executor);
        futures.add(future);
    }
    
    // Wait for completion and collect results
    try {
        List<Block> results = futures.stream()
            .map(CompletableFuture::join)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        
        System.out.println("Successfully added " + results.size() + "/" + blockDataList.size() + " blocks");
    } finally {
        executor.shutdown();
    }
}
```

#### Search Optimization
```java
// ✅ GOOD: Efficient search with thread-safe caching
private final Map<String, List<Block>> searchCache = new ConcurrentHashMap<>();

public List<Block> searchWithCache(String searchTerm) {
    // Check cache first (thread-safe)
    String cacheKey = searchTerm.toLowerCase();
    List<Block> cachedResults = searchCache.get(cacheKey);
    if (cachedResults != null) {
        return new ArrayList<>(cachedResults); // Return defensive copy
    }
    
    // Perform search (blockchain.searchBlocksByContent is thread-safe)
    List<Block> results = blockchain.searchBlocksByContent(searchTerm);
    
    // Cache results (with size limit) - atomic operation
    if (searchCache.size() < 100) {
        searchCache.put(cacheKey, new ArrayList<>(results)); // Store defensive copy
    }
    
    return results;
}

// ✅ GOOD: Paginated search for large results
public List<Block> searchWithPagination(String searchTerm, int page, int pageSize) {
    List<Block> allResults = blockchain.searchBlocksByContent(searchTerm);
    int start = page * pageSize;
    int end = Math.min(start + pageSize, allResults.size());
    
    if (start >= allResults.size()) {
        return Collections.emptyList();
    }
    
    return allResults.subList(start, end);
}
```

### Error Handling Best Practices

#### Comprehensive Exception Management
```java
public class BlockchainService {
    
    public BlockOperationResult addBlockSafely(String data, PrivateKey privateKey, 
                                             PublicKey publicKey) {
        try {
            // Pre-validation
            validateBlockData(data);
            validateKeyAuthorization(publicKey);
            
            // Add block
            boolean success = blockchain.addBlock(data, privateKey, publicKey);
            
            if (success) {
                // Post-validation
                ChainValidationResult validationResult = blockchain.validateChainDetailed();
                if (!validationResult.isStructurallyIntact()) {
                    // Rollback if chain becomes invalid
                    blockchain.rollbackBlocks(1);
                    return BlockOperationResult.failure("Chain validation failed after block addition");
                }
                
                return BlockOperationResult.success("Block added successfully");
            } else {
                return BlockOperationResult.failure("Block addition failed");
            }
            
        } catch (IllegalArgumentException e) {
            return BlockOperationResult.failure("Invalid input: " + e.getMessage());
        } catch (SecurityException e) {
            return BlockOperationResult.failure("Security error: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error adding block", e);
            return BlockOperationResult.failure("System error: " + e.getMessage());
        }
    }
    
    private void validateBlockData(String data) {
        if (data == null || data.trim().isEmpty()) {
            throw new IllegalArgumentException("Block data cannot be null or empty");
        }
        if (data.length() > blockchain.getMaxBlockDataLength()) {
            throw new IllegalArgumentException("Block data exceeds maximum length");
        }
    }
    
    private void validateKeyAuthorization(PublicKey publicKey) {
        String publicKeyString = CryptoUtil.publicKeyToString(publicKey);
        if (!blockchain.isKeyAuthorized(publicKeyString)) {
            throw new SecurityException("Public key not authorized");
        }
    }
    
    public static class BlockOperationResult {
        private final boolean success;
        private final String message;
        private final String blockHash;
        
        private BlockOperationResult(boolean success, String message, String blockHash) {
            this.success = success;
            this.message = message;
            this.blockHash = blockHash;
        }
        
        public static BlockOperationResult success(String message) {
            return new BlockOperationResult(true, message, null);
        }
        
        public static BlockOperationResult failure(String message) {
            return new BlockOperationResult(false, message, null);
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getBlockHash() { return blockHash; }
    }
}
```

### Database Management Best Practices

#### Connection Management
```java
// ✅ GOOD: Proper JPA EntityManager management
public List<Block> getBlocksInRange(int startIndex, int endIndex) {
    EntityManager em = JPAUtil.getEntityManager();
    EntityTransaction transaction = null;
    
    try {
        transaction = em.getTransaction();
        transaction.begin();
        
        TypedQuery<Block> query = em.createQuery(
            "SELECT b FROM Block b WHERE b.blockNumber BETWEEN :start AND :end ORDER BY b.blockNumber",
            Block.class);
        query.setParameter("start", startIndex);
        query.setParameter("end", endIndex);
        
        List<Block> results = query.getResultList();
        transaction.commit();
        return results;
        
    } catch (Exception e) {
        if (transaction != null && transaction.isActive()) {
            transaction.rollback();
        }
        throw e;
    } finally {
        em.close();
    }
}

// ❌ BAD: No transaction management
public List<Block> getBlocksBad() {
    EntityManager em = JPAUtil.getEntityManager();
    // No transaction management - dangerous!
    return em.createQuery("SELECT b FROM Block b", Block.class).getResultList();
}
```

## 🔄 Data Consistency & Maintenance API

### Off-Chain File Cleanup Methods

```java
// Clean up orphaned off-chain files (maintenance utility)
public int cleanupOrphanedFiles() {
    // Identifies and removes off-chain files that no longer have corresponding database entries
    // Returns: number of orphaned files deleted
    // ✅ Thread-safe operation with read lock
    // ✅ Compares database file paths with actual files in off-chain directory
}

// Manual integrity verification for all off-chain data
public boolean verifyAllOffChainIntegrity() {
    // Verifies SHA3-256 hash and ECDSA signature for all off-chain data
    // Returns: true if all off-chain data passes integrity checks
    // ⚠️ Can be expensive for large numbers of off-chain files
}

// Verify integrity of specific block's off-chain data
public boolean verifyOffChainIntegrity(Block block) {
    // Verifies hash and signature for a single block's off-chain data
    // Returns: true if off-chain data is valid, false if corrupted
}
```

### Blockchain Operations with Data Consistency

#### Rollback Operations (Enhanced)
```java
// Roll back N blocks with automatic off-chain cleanup
public boolean rollbackBlocks(Long numberOfBlocks) {
    // ✅ NEW: Automatically deletes off-chain files before removing blocks
    // ✅ Thread-safe with global write lock and transaction management
    // ✅ Comprehensive logging of cleanup operations
    // ✅ Returns detailed success/failure status
}

// Roll back to specific block number with off-chain cleanup
public boolean rollbackToBlock(Long targetBlockNumber) {
    // ✅ NEW: Uses getBlocksAfter() to identify all affected off-chain files
    // ✅ Deletes off-chain files before database operations
    // ✅ Atomic operation - all blocks after target are removed
}
```

#### Export/Import Operations (Enhanced)
```java
// Export blockchain with off-chain file backup
public boolean exportChain(String filePath) {
    // ✅ NEW: Creates "off-chain-backup" directory alongside export file
    // ✅ Copies all off-chain files with proper naming (block_N_filename.ext)
    // ✅ Updates file paths in export data to point to backup location
    // ✅ Version 1.1 export format includes off-chain support
}

// Import blockchain with off-chain file restoration
public boolean importChain(String filePath) {
    // ✅ NEW: Cleans up existing off-chain files before import
    // ✅ Restores off-chain files from backup directory to standard location
    // ✅ Generates new file paths to prevent conflicts
    // ✅ Handles missing backup files gracefully
    // ✅ Validates imported off-chain data integrity
}
```

#### Database Management (Enhanced)
```java
// Clear blockchain with complete off-chain cleanup
public void clearAndReinitialize() {
    // ✅ NEW: Deletes all off-chain files before clearing database
    // ✅ Runs orphaned file cleanup after database operations
    // ✅ Thread-safe with global write lock
    // ⚠️ WARNING: Destroys all blockchain data - use only for testing
}
```

### Data Consistency Best Practices

#### Automatic Cleanup Operations
```java
// ✅ GOOD: Rollback operations automatically clean up off-chain files
public void safeRollback() {
    // The system automatically handles off-chain cleanup
    boolean success = blockchain.rollbackBlocks(3L);
    if (success) {
        System.out.println("Rollback completed with automatic file cleanup");
    }
}

// ✅ GOOD: Regular maintenance to clean orphaned files
public void regularMaintenance() {
    int cleanedFiles = blockchain.cleanupOrphanedFiles();
    if (cleanedFiles > 0) {
        System.out.println("Cleaned up " + cleanedFiles + " orphaned files");
    }
}

// ✅ GOOD: Verify integrity before important operations
public void verifyBeforeOperation() {
    boolean allValid = blockchain.verifyAllOffChainIntegrity();
    if (!allValid) {
        throw new RuntimeException("Off-chain data integrity check failed");
    }
}
```

#### Export/Import with Data Consistency
```java
// ✅ GOOD: Complete backup including off-chain files
public void createCompleteBackup(String backupPath) {
    boolean exported = blockchain.exportChain(backupPath);
    if (exported) {
        // Verify backup includes off-chain files
        File backupDir = new File(new File(backupPath).getParent(), "off-chain-backup");
        if (backupDir.exists()) {
            System.out.println("Complete backup created with off-chain files");
        }
    }
}

// ✅ GOOD: Safe import with validation
public void safeImport(String importPath) {
    // Import automatically handles off-chain file restoration
    boolean imported = blockchain.importChain(importPath);
    if (imported) {
        // Verify all off-chain data after import
        boolean valid = blockchain.verifyAllOffChainIntegrity();
        if (valid) {
            System.out.println("Import completed with data integrity verified");
        }
    }
}
```

#### Error Recovery Patterns
```java
// ✅ GOOD: Handle partial failures gracefully
public void handlePartialFailure() {
    try {
        blockchain.rollbackBlocks(5L);
    } catch (Exception e) {
        // Even if rollback has issues, run cleanup
        int orphaned = blockchain.cleanupOrphanedFiles();
        System.out.println("Emergency cleanup removed " + orphaned + " orphaned files");
    }
}

// ❌ BAD: Manual file management alongside blockchain operations
public void badFileManagement() {
    // Don't manually delete off-chain files - the blockchain handles it automatically
    File offChainDir = new File("off-chain-data");
    // ❌ This could cause data inconsistency!
    // deleteDirectory(offChainDir);
}
```

### Data Consistency Guarantees

The blockchain now provides the following consistency guarantees:

1. **Atomic Operations**: File and database operations are coordinated within transaction scope
2. **Pre-deletion Cleanup**: Off-chain files are deleted before corresponding database records
3. **Cascade Safety**: JPA cascade operations automatically handle off-chain metadata deletion
4. **Rollback Safety**: Database rollbacks work even if file cleanup encounters issues
5. **Import/Export Integrity**: Complete backup and restoration of off-chain files
6. **Maintenance Tools**: Utilities to detect and clean orphaned files
7. **Verification Methods**: Cryptographic verification of off-chain data integrity

## 🔐 UserFriendlyEncryptionAPI - Complete Reference

The UserFriendlyEncryptionAPI provides a comprehensive, simplified interface for all blockchain operations with built-in encryption, search, and security features. This section documents all 212 methods available in the API.

### 📋 API Initialization

```java
// Basic initialization
UserFriendlyEncryptionAPI api = new UserFriendlyEncryptionAPI(blockchain);

// Initialize with default user credentials
KeyPair userKeys = CryptoUtil.generateKeyPair();
UserFriendlyEncryptionAPI api = new UserFriendlyEncryptionAPI(blockchain, "username", userKeys);
```

### 🎯 Core Data Storage Methods

#### Encrypted Data Storage
```java
// Store encrypted data with automatic keyword extraction
Block storeEncryptedData(String data, String password)

// Store secret data with enhanced security
Block storeSecret(String secretData, String password)

// Store data with searchable terms
Block storeSearchableData(String data, String password, String[] searchTerms)

// Store with layered search terms (public/private separation)
Block storeSearchableDataWithLayers(String data, String password, 
                                   String[] publicTerms, String[] privateTerms)

// Store with granular term visibility control
Block storeDataWithGranularTermControl(String data, String password, 
                                     Set<String> allSearchTerms, 
                                     TermVisibilityMap termVisibility)

// Store with smart compression and tiering
Block storeWithSmartTiering(String data, String password, Map<String, Object> metadata)
```

#### Data Retrieval Methods
```java
// Retrieve and decrypt data
String retrieveSecret(Long blockId, String password)
String retrieveEncryptedData(Long blockId, String password)

// Check encryption status
boolean isBlockEncrypted(Long blockId)
boolean hasEncryptedData()

// Find encrypted blocks
List<Long> findEncryptedBlocks()
List<Block> findEncryptedData(String searchTerm, String password)
```

### 🔍 Advanced Search Methods

#### Multi-Level Search
```java
// Search by terms with different access levels
List<Block> searchByTerms(String[] searchTerms, String password, int maxResults)

// Adaptive decryption search
List<Block> searchWithAdaptiveDecryption(String searchTerm, String password, int maxResults)

// Advanced search with criteria
AdvancedSearchResult performAdvancedSearch(Map<String, Object> searchCriteria, 
                                         String password, int maxResults)

// Semantic search for concept-based discovery
AdvancedSearchResult performSemanticSearch(String concept, String password)

// Time-based search
AdvancedSearchResult performTimeRangeSearch(LocalDateTime startDate, LocalDateTime endDate, 
                                          Map<String, Object> filters)

// Cached search operations
AdvancedSearchResult performCachedSearch(String searchType, String query, 
                                       Map<String, Object> options, String password)
```

#### Smart Search Features
```java
// Smart search with AI-like capabilities
List<Block> smartSearch(String query, String password, int maxResults)
List<Block> smartSearchEncryptedData(String query, String password, int maxResults)
List<Block> smartAdvancedSearch(String query, String password, int maxResults)

// Exhaustive search across all content
SearchResults searchExhaustive(String query, String password)

// Targeted search types
SearchResults searchPublicFast(String query)
SearchResults searchEncryptedOnly(String query, String password)
```

#### Search Management
```java
// Cache management
void clearSearchCache()
void optimizeSearchCache()
void warmUpCache(List<String> commonTerms)
void invalidateCacheForBlocks(List<Long> blockNumbers)

// Search metrics and optimization - All methods are thread-safe
SearchMetrics getSearchMetrics()
SearchMetrics getSearchPerformanceStats()
SearchCacheManager.CacheStatistics getCacheStatistics()
String optimizeSearchPerformance()
Map<String, Object> getRealtimeSearchMetrics()
```

#### Thread-Safe SearchMetrics Example
```java
// Multiple threads can safely collect metrics concurrently
UserFriendlyEncryptionAPI api = new UserFriendlyEncryptionAPI(blockchain);

// Concurrent metrics collection
CompletableFuture<SearchMetrics> metricsTask = CompletableFuture.supplyAsync(() -> {
    return api.getSearchMetrics(); // Thread-safe metrics retrieval
});

CompletableFuture<Map<String, Object>> realtimeTask = CompletableFuture.supplyAsync(() -> {
    return api.getRealtimeSearchMetrics(); // Thread-safe realtime metrics
});

// Both operations can run concurrently without synchronization issues
CompletableFuture.allOf(metricsTask, realtimeTask).thenRun(() -> {
    SearchMetrics metrics = metricsTask.join();
    Map<String, Object> realtime = realtimeTask.join();
    
    // Process metrics safely - returned collections are immutable
    System.out.println("Total searches: " + metrics.getTotalSearches()); // Thread-safe access
});
```

### 🔑 Security and Key Management

#### Key Management Operations
```java
// Hierarchical key setup
KeyManagementResult setupHierarchicalKeys(String masterPassword)

// Key generation and validation
KeyManagementResult generateHierarchicalKey(String purpose, int depth, 
                                          Map<String, Object> options)
ValidationReport validateKeyHierarchy(String keyId)
ValidationReport validateKeyManagement(Map<String, Object> options)

// Key storage and retrieval
boolean saveUserKeySecurely(String password)
boolean savePrivateKeySecurely(String username, PrivateKey privateKey, String password)
PrivateKey loadPrivateKeySecurely(String username, String password)

// Key management utilities
boolean hasStoredKey(String username)
boolean deleteStoredKey(String username)
String[] listStoredUsers()
List<CryptoUtil.KeyInfo> listManagedKeys()
```

#### Import/Export Operations
```java
// Key file operations
PrivateKey importPrivateKeyFromFile(String keyFilePath)
PublicKey importPublicKeyFromFile(String keyFilePath)
boolean importAndRegisterUser(String username, String keyFilePath)
boolean importAndSetDefaultUser(String username, String keyFilePath)
String detectKeyFileFormat(String keyFilePath)
```

#### Cryptographic Utilities
```java
// Key derivation and verification
PublicKey derivePublicKeyFromPrivate(PrivateKey privateKey)
boolean verifyKeyPairConsistency(PrivateKey privateKey, PublicKey publicKey)
KeyPair createKeyPairFromPrivate(PrivateKey privateKey)
boolean verifyKeyPairMathematically(PrivateKey privateKey, PublicKey publicKey)

// Curve parameters
ECParameterSpec getCurveParameters(String curveName)
```

### 🛡️ Security and Validation

#### Password and Security
```java
// Password generation
String generatePasswordForConfig(EncryptionConfig config)
String generateValidatedPassword(int length, boolean includeSpecialChars)
Object getPasswordRegistryStats()
```

#### Validation and Integrity
```java
// Comprehensive validation
ValidationReport performComprehensiveValidation()
ValidationReport performComprehensiveValidation(Map<String, Object> options)
ValidationReport validateChainIntegrity()

// Health monitoring
HealthReport performHealthDiagnosis()
HealthReport generateHealthReport(Map<String, Object> options)

// Data integrity checking
boolean detectDataTampering(Long blockId)
boolean offChainFilesExist(Long blockId)
boolean validateGenesisBlock()
boolean wasKeyAuthorizedAt(String username, LocalDateTime timestamp)
String generateIntegrityReport()

// Off-chain integrity
OffChainIntegrityReport verifyOffChainIntegrity(List<Long> blockNumbers)
OffChainIntegrityReport performBatchIntegrityCheck(Long startBlock, Long endBlock, 
                                                 Map<String, Object> options)
```

### 💾 Storage and File Management

#### Large File Operations
```java
// Large file storage
OffChainData storeLargeFileSecurely(byte[] fileData, String password, String contentType)
OffChainData storeLargeFileWithSigner(byte[] fileData, String password, KeyPair signerKeyPair, 
                                    String contentType, String username)

// Large file retrieval
byte[] retrieveLargeFile(OffChainData offChainData, String password)
boolean verifyLargeFileIntegrity(OffChainData offChainData, String password)

// Text document operations
OffChainData storeLargeTextDocument(String textContent, String password, String filename)
String retrieveLargeTextDocument(OffChainData offChainData, String password)
```

#### Storage Management
```java
// Storage analytics and optimization
String getStorageAnalytics()
Map<String, Object> getStorageTierMetrics()
StorageTieringManager.TieringReport optimizeStorageTiers()

// Smart storage operations
Block storeWithSmartTiering(String data, String password, Map<String, Object> metadata)
Object retrieveFromAnyTier(Long blockNumber, String password)
```

#### Compression and Analysis
```java
// Compression operations
CompressionAnalysisResult analyzeCompressionOptions(String data, String contentType)
UserFriendlyEncryptionAPI.CompressedDataResult performAdaptiveCompression(String data, 
                                                                         String contentType)
```

### 🔧 Chain Recovery and Maintenance

#### Recovery Operations
```java
// Chain recovery
ChainRecoveryResult recoverFromCorruption(Map<String, Object> options)
ChainRecoveryResult repairBrokenChain(Long startBlock, Long endBlock)
ChainRecoveryResult rollbackToSafeState(Long targetBlock, Map<String, Object> options)

// Recovery checkpoints
RecoveryCheckpoint createRecoveryCheckpoint(RecoveryCheckpoint.CheckpointType type, 
                                          String description)
```

### ⚙️ Configuration and Reporting

#### Configuration Management
```java
// Configuration builders
EncryptionConfig.Builder createCustomConfig()

// Reporting methods
String generateOffChainStorageReport()
String generateBlockchainStatusReport()
String getEncryptionConfigComparison()
String getSearchEngineReport()
```

#### Formatting and Display
```java
// Data formatting
String formatBlocksSummary(List<Block> blocks)
String formatBlockDisplay(Block block)
String formatSearchResults(String searchTerm, List<Block> results)
String formatFileSize(long sizeInBytes)

// Export functionality
String exportSearchResults(AdvancedSearchResult result, String format)
```

### 📊 Analytics and User Management

#### User Operations
```java
// User management
Block createUser(String username, String password)
boolean setDefaultCredentials(String username, String password)
Map<String, Object> loadUserCredentials(String username)
```

#### Analytics and Summary
```java
// Blockchain analytics
String getBlockchainSummary()
Map<String, Object> getBlockchainSummary(boolean includeDetails)
boolean hasEncryptedData()

// Content analysis
String[] extractKeywords(String data)
List<Block> analyzeContent(String data, String password)
```

### 🎯 Testing and Quality Assurance

The UserFriendlyEncryptionAPI is thoroughly tested with **795 JUnit 5 tests** achieving **72% code coverage**:

#### Test Classes Structure
- **UserFriendlyEncryptionAPIPhase1Test** - Core functionality (46 tests)
- **UserFriendlyEncryptionAPIPhase2SearchTest** - Search capabilities 
- **UserFriendlyEncryptionAPIPhase3Test** - Storage and security
- **UserFriendlyEncryptionAPIPhase4Test** - Recovery and analytics
- **UserFriendlyEncryptionAPISecurityTest** - Security features
- **UserFriendlyEncryptionAPIZeroCoverageTest** - Edge cases and validation
- **UserFriendlyEncryptionAPIRemainingCoverageTest** - Complete coverage testing

#### Testing Categories Covered
```java
// Core functionality testing
@DisplayName("📦 Core Functionality Tests")
- Data storage and retrieval
- Encryption and decryption
- User management

// Search capability testing  
@DisplayName("🔍 Search and Analytics Tests")
- Multi-level search operations
- Cache management
- Performance optimization

// Security feature testing
@DisplayName("🔐 Security and Key Management Tests")
- Key generation and storage
- Cryptographic operations
- Authentication and authorization

// Recovery and maintenance testing
@DisplayName("🔧 Recovery and Maintenance Tests")
- Chain recovery operations
- Data integrity verification
- Error handling and edge cases
```

### 🚀 Usage Patterns and Best Practices

#### Initialization Pattern
```java
// Recommended initialization for production
Blockchain blockchain = new Blockchain();
KeyPair userKeys = CryptoUtil.generateKeyPair();
UserFriendlyEncryptionAPI api = new UserFriendlyEncryptionAPI(blockchain, "production-user", userKeys);

// Setup hierarchical security - fully thread-safe
KeyManagementResult keySetup = api.setupHierarchicalKeys("masterPassword123!");
if (keySetup.isSuccess()) {
    System.out.println("✅ Security setup completed");
}

// Thread-safe concurrent access example
CompletableFuture<Void> initTask = CompletableFuture.runAsync(() -> {
    // Multiple threads can safely use the same API instance
    api.validateUserSetup(); // Thread-safe validation
});
```

#### Data Storage Pattern
```java
// Store sensitive data with search capabilities
String sensitiveData = "Patient medical record with diagnosis";
String[] searchTerms = {"medical", "patient", "diagnosis"};
String password = api.generateValidatedPassword(16, true);

Block block = api.storeSearchableData(sensitiveData, password, searchTerms);
if (block != null) {
    System.out.println("✅ Data stored securely in block #" + block.getBlockNumber());
}
```

#### Search Pattern
```java
// Multi-level search approach
List<Block> fastResults = api.searchByTerms(new String[]{"medical"}, null, 10);
if (fastResults.isEmpty()) {
    // Try deeper search with password
    List<Block> deepResults = api.searchWithAdaptiveDecryption("medical", password, 10);
    System.out.println("Found " + deepResults.size() + " encrypted results");
}
```

#### Health Monitoring Pattern
```java
// Regular health checks
ValidationReport health = api.performComprehensiveValidation();
if (!health.isFullyValid()) {
    System.err.println("⚠️ Health issues detected");
    
    // Attempt automatic recovery
    Map<String, Object> options = Map.of("autoRepair", true);
    ChainRecoveryResult recovery = api.recoverFromCorruption(options);
    
    if (recovery.isSuccess()) {
        System.out.println("✅ Automatic recovery successful");
    }
}
```

## 📚 Additional Resources

### 🚀 Getting Started
- **[GETTING_STARTED.md](GETTING_STARTED.md)** - Quick start guide with essential examples
- **[EXAMPLES.md](EXAMPLES.md)** - Real-world usage examples and patterns

### 🔍 Search & Data Management  
- **[SEARCH_APIS_COMPARISON.md](SEARCH_APIS_COMPARISON.md)** - 🎯 **Which search API to use? Complete comparison and recommendations**
- **[USER_FRIENDLY_SEARCH_GUIDE.md](USER_FRIENDLY_SEARCH_GUIDE.md)** - UserFriendlyEncryptionAPI search functionality guide
- **[SEARCH_FRAMEWORK_GUIDE.md](SEARCH_FRAMEWORK_GUIDE.md)** - Advanced Search Engine comprehensive guide
- **[EXHAUSTIVE_SEARCH_GUIDE.md](EXHAUSTIVE_SEARCH_GUIDE.md)** - Complete search across on-chain and off-chain content
- **[SEARCH_COMPARISON.md](SEARCH_COMPARISON.md)** - Comparison of all search types with benchmarks

### 🔐 Security & Key Management
- **[SECURITY_GUIDE.md](SECURITY_GUIDE.md)** - Security best practices for production
- **[KEY_MANAGEMENT_GUIDE.md](KEY_MANAGEMENT_GUIDE.md)** - Hierarchical key management and rotation
- **[ENCRYPTION_GUIDE.md](ENCRYPTION_GUIDE.md)** - Block encryption and metadata management
- **[ENCRYPTED_EXPORT_IMPORT_GUIDE.md](ENCRYPTED_EXPORT_IMPORT_GUIDE.md)** - Encrypted backup procedures

### 🛠️ Operations & Troubleshooting
- **[TROUBLESHOOTING_GUIDE.md](TROUBLESHOOTING_GUIDE.md)** - Common issues and diagnostic tools
- **[TESTING.md](TESTING.md)** - Comprehensive testing procedures
- **[LOGGING.md](LOGGING.md)** - Professional logging system configuration

### 🏢 Technical & Production
- **[TECHNICAL_DETAILS.md](TECHNICAL_DETAILS.md)** - Architecture and implementation details
- **[PRODUCTION_GUIDE.md](PRODUCTION_GUIDE.md)** - Production deployment guidance
- **[ENHANCED_VALIDATION_GUIDE.md](ENHANCED_VALIDATION_GUIDE.md)** - Advanced validation techniques

### 📖 Reference Guides
- **[SECURITY_CLASSES_GUIDE.md](SECURITY_CLASSES_GUIDE.md)** - Security classes usage guide
- **[UTILITY_CLASSES_GUIDE.md](UTILITY_CLASSES_GUIDE.md)** - Utility classes reference
- **[THREAD_SAFETY_TESTS.md](THREAD_SAFETY_TESTS.md)** - Thread safety testing guide
