# API Guide & Core Functions

Comprehensive guide to the Private Blockchain API, core functions, and programming interface.

> **IMPORTANT NOTE**: This guide references the actual classes and methods implemented in the project. All mentioned classes (Blockchain, Block, AuthorizedKey, JPAUtil, CryptoUtil) exist in the source code. The code examples show the correct usage of the current API based on the JPA standard with Hibernate as the implementation provider.

## 📋 Table of Contents

- [Core Functions Usage](#-core-functions-usage)
- [API Reference](#-api-reference)
- [Configuration](#-configuration)
- [Configuration Parameters](#-configuration-parameters)
- [Best Practices](#-best-practices)

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
// Check if blockchain is valid
boolean isValid = blockchain.validateChain();
System.out.println("Blockchain is valid: " + isValid);
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
```java
// Search blocks by content (case-insensitive)
List<Block> paymentBlocks = blockchain.searchBlocksByContent("payment");

// Find block by hash
Block block = blockchain.getBlockByHash("a1b2c3d4...");

// Find blocks by date range
LocalDate start = LocalDate.of(2024, 1, 1);
LocalDate end = LocalDate.of(2024, 1, 31);
List<Block> monthlyBlocks = blockchain.getBlocksByDateRange(start, end);
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
            
            boolean isValid = blockchain.validateChain();
            System.out.println("Blockchain is valid: " + isValid);
            
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

// Validate
boolean isValid = blockchain.validateChain();

// Advanced operations
boolean exported = blockchain.exportChain("backup.json");
boolean imported = blockchain.importChain("backup.json");
boolean rolledBack = blockchain.rollbackBlocks(numberOfBlocks);
```

#### Search Operations
```java
// Search methods
List<Block> contentResults = blockchain.searchBlocksByContent("searchTerm");
Block hashResult = blockchain.getBlockByHash("hashString");
List<Block> dateResults = blockchain.getBlocksByDateRange(startDate, endDate);
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
public boolean validateChain()
```
- **Returns:** `true` if the entire blockchain is valid, `false` if any issues are found
- **Description:** Validates all blocks, hashes, signatures, and chain integrity

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

```java
public List<Block> searchBlocksByContent(String searchTerm)
```
- **Parameters:** `searchTerm`: Text to search for (case-insensitive)
- **Returns:** List of blocks containing the search term
- **Description:** Searches through all block data content

```java
public Block getBlockByHash(String hash)
```
- **Parameters:** `hash`: SHA-256 hash of the block
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

### CryptoUtil Class API

#### Key Generation
```java
public static KeyPair generateKeyPair()
```
- **Returns:** A new RSA key pair (2048-bit)
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
- **Returns:** SHA-256 hash as hexadecimal string

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
- `hash`: SHA-256 hash of this block
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
- **Hash Length**: 64 characters (SHA-256)

### Security Configuration
- **Hash Algorithm**: SHA-256
- **Signature Algorithm**: RSA
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
// Generate new RSA key pair
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
String hashAlgorithm = "SHA-256";        
String signatureAlgorithm = "SHA256withRSA"; 
int keySize = 2048;                

// Validate blockchain integrity
boolean securityValid = blockchain.validateChain();
```

## 🔧 Configuration Parameters

### Size and Performance Limits
```properties
# Block constraints
blockchain.block.max_data_size=10000           # 10,000 characters
blockchain.block.max_size_bytes=1048576        # 1MB (1,048,576 bytes)
blockchain.block.max_hash_length=64            # SHA-256 hash length

# Database settings
blockchain.database.connection_timeout=30000   # 30 seconds
blockchain.database.max_connections=20         # Connection pool size
blockchain.database.batch_size=25              # Batch operations

# Security settings
blockchain.security.min_key_size=2048          # Minimum RSA key size
blockchain.security.signature_algorithm=SHA256withRSA
blockchain.security.hash_algorithm=SHA-256

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

## 💡 Best Practices

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

// ❌ BAD: Individual transactions for each block
for (BlockData data : blockDataList) {
    blockchain.addBlock(data.getContent(), privateKey, publicKey); // Too slow!
}
```

#### Search Optimization
```java
// ✅ GOOD: Efficient search with caching
private Map<String, List<Block>> searchCache = new ConcurrentHashMap<>();

public List<Block> searchWithCache(String searchTerm) {
    // Check cache first
    String cacheKey = searchTerm.toLowerCase();
    if (searchCache.containsKey(cacheKey)) {
        return searchCache.get(cacheKey);
    }
    
    // Perform search
    List<Block> results = blockchain.searchBlocksByContent(searchTerm);
    
    // Cache results (with size limit)
    if (searchCache.size() < 100) {
        searchCache.put(cacheKey, results);
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
                if (!blockchain.validateChain()) {
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

For real-world usage examples, see [EXAMPLES.md](EXAMPLES.md).  
For production deployment guidance, see [PRODUCTION_GUIDE.md](PRODUCTION_GUIDE.md).  
For testing procedures, see [TESTING.md](TESTING.md).
