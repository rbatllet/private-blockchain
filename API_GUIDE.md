# API Guide & Core Functions

Comprehensive guide to the Private Blockchain API, core functions, and programming interface.

## 📋 Table of Contents

- [Core Functions Usage](#-core-functions-usage)
- [API Reference](#-api-reference)
- [Configuration](#-configuration)
- [Configuration Parameters](#-configuration-parameters)
- [Best Practices](#-best-practices)

## 🎯 Core Functions Usage

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

```java
public boolean deleteAuthorizedKey(String publicKey)
```
- **Parameters:** `publicKey`: The public key to delete permanently
- **Returns:** `true` if one or more keys were deleted, `false` if no keys were found
- **Description:** **Permanently removes** all records for the specified public key (both active and revoked). ⚠️ **Use with caution**: This operation is irreversible and removes all historical authorization records.

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

#### Methods
```java
public String getPublicKey()
public String getOwnerName()
public boolean isActive()
public LocalDateTime getCreatedAt()
```

## ⚙️ Configuration

### Database Configuration
- **Location**: `blockchain.db` in project root directory
- **Type**: SQLite database
- **ORM**: Hibernate with automatic table creation
- **Logging**: SQL queries logged (can be disabled in hibernate.cfg.xml)

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
// Build block content for custom validation
String blockContent = blockchain.buildBlockContent(block);

// Get block size information
long blockSizeBytes = block.calculateSizeInBytes();
int blockDataLength = block.getData().length();

// Verify block hash
boolean hashValid = block.getHash().equals(block.calculateBlockHash());
```

### Advanced Configuration Methods

#### Database Configuration
```java
// Get current database statistics
long databaseSize = blockchain.getDatabaseSizeBytes();
int connectionPoolSize = blockchain.getConnectionPoolSize();

// Performance metrics
long averageBlockAddTime = blockchain.getAverageBlockAddTime();
long averageSearchTime = blockchain.getAverageSearchTime();
```

#### Security Configuration
```java
// Get security settings
String hashAlgorithm = blockchain.getHashAlgorithm();        // "SHA-256"
String signatureAlgorithm = blockchain.getSignatureAlgorithm(); // "SHA256withRSA"
int keySize = blockchain.getDefaultKeySize();                // 2048

// Validate security configuration
boolean securityValid = blockchain.validateSecurityConfiguration();
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
// ✅ GOOD: Batch processing with transaction management
public void addBlocksBatch(List<BlockData> blockDataList, 
                          PrivateKey signerKey, PublicKey signerPublic) {
    Session session = HibernateUtil.getSessionFactory().getCurrentSession();
    Transaction transaction = session.beginTransaction();
    
    try {
        for (int i = 0; i < blockDataList.size(); i++) {
            Block block = createBlock(blockDataList.get(i), signerKey, signerPublic);
            session.save(block);
            
            // Flush every 25 blocks for memory management
            if (i % 25 == 0) {
                session.flush();
                session.clear();
            }
        }
        transaction.commit();
    } catch (Exception e) {
        transaction.rollback();
        throw e;
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
// ✅ GOOD: Proper session management
public List<Block> getBlocksInRange(int startIndex, int endIndex) {
    Session session = HibernateUtil.getSessionFactory().getCurrentSession();
    Transaction transaction = session.beginTransaction();
    
    try {
        Query<Block> query = session.createQuery(
            "FROM Block WHERE blockIndex BETWEEN :start AND :end ORDER BY blockIndex",
            Block.class);
        query.setParameter("start", startIndex);
        query.setParameter("end", endIndex);
        
        List<Block> results = query.getResultList();
        transaction.commit();
        return results;
        
    } catch (Exception e) {
        transaction.rollback();
        throw e;
    }
}

// ❌ BAD: No transaction management
public List<Block> getBlocksBad() {
    Session session = HibernateUtil.getSessionFactory().getCurrentSession();
    return session.createQuery("FROM Block", Block.class).getResultList(); // No transaction!
}
```

For real-world usage examples, see [EXAMPLES.md](EXAMPLES.md).  
For production deployment guidance, see [PRODUCTION_GUIDE.md](PRODUCTION_GUIDE.md).  
For testing procedures, see [TESTING.md](TESTING.md).
