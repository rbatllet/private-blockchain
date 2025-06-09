# Private Blockchain

A simple and secure private blockchain implementation using Java 21, SQLite, and Hibernate.

## 📋 Overview

This is a **private blockchain** for controlled environments where only authorized users can add blocks. Unlike public blockchains, there is no mining - blocks are added directly by authorized users.

## ✨ Main Features

### Core Blockchain Features
- **Genesis Block**: Created automatically when blockchain starts
- **Block Creation**: Add new blocks with data to the chain
- **Hash Verification**: SHA-256 hashing ensures data integrity
- **Digital Signatures**: RSA signatures verify block authenticity
- **Chain Validation**: Complete blockchain integrity checking

### Security Features
- **Authorized Keys**: Only approved users can add blocks
- **Cryptographic Protection**: Each block is cryptographically signed
- **Immutable Records**: Blocks cannot be changed once added
- **Sequential Validation**: Each block links to the previous block

### Advanced Features
- **Block Size Validation**: Prevents oversized blocks
- **Chain Export/Import**: Backup and restore complete blockchain
- **Block Rollback**: Safe removal of recent blocks
- **Advanced Search**: Find blocks by content, hash, or date range

### Technical Features
- **Persistent Storage**: SQLite database with Hibernate ORM
- **Auto-generated Tables**: Database schema created automatically
- **Clean Architecture**: Well-structured code with DAO pattern
- **Java 21**: Modern Java features and performance

## 🛠️ Technologies Used

- **Java 21** - Programming language
- **Maven** - Build and dependency management
- **SQLite** - Lightweight database for data storage
- **Hibernate** - Object-relational mapping (ORM)
- **SHA-256** - Cryptographic hash function
- **RSA** - Digital signature algorithm
- **JUnit 5** - Testing framework

## 📦 Project Structure

```
src/main/java/com/rbatllet/blockchain/
├── core/
│   └── Blockchain.java                           # Main blockchain logic
├── dao/
│   ├── BlockDAO.java                            # Database operations for blocks
│   └── AuthorizedKeyDAO.java                    # Database operations for keys
├── entity/
│   ├── Block.java                               # Block data model
│   └── AuthorizedKey.java                       # Authorized key data model
├── util/
│   ├── CryptoUtil.java                          # Cryptographic utilities
│   └── HibernateUtil.java                       # Database connection management
├── BlockchainDemo.java                          # Basic demo application
├── AdditionalAdvancedFunctionsDemo.java         # Advanced features demo
├── CoreFunctionsTest.java                       # Comprehensive core test
├── SimpleTest.java                              # Basic functionality test
└── QuickTest.java                               # Fast verification test

src/test/java/com/rbatllet/blockchain/core/
├── BlockchainAdditionalAdvancedFunctionsTest.java   # JUnit 5 test suite (22 tests)
├── BlockchainAdditionalAdvancedFunctionsTestRunner.java # Test runner
└── TestEnvironmentValidator.java                    # Environment validation

Scripts:
├── run_all_tests.sh                             # Run all tests (recommended)
└── run_core_tests.sh                            # Run advanced functions tests only
```

## 🚀 How to Run

### Prerequisites
- **Java 21** or higher
- **Maven 3.6** or higher

### Quick Start
```bash
# 1. Navigate to project directory
cd /path/to/privateBockChain

# 2. Compile the project
mvn clean compile

# 3. Run the basic demo
mvn exec:java -Dexec.mainClass="com.rbatllet.blockchain.BlockchainDemo"

# 4. Run advanced features demo
mvn exec:java -Dexec.mainClass="com.rbatllet.blockchain.core.AdditionalAdvancedFunctionsDemo"
```

### Expected Output (Basic Demo)
```
=== PRIVATE BLOCKCHAIN DEMO ===
Genesis block created successfully!
Authorized key added for: Alice
Authorized key added for: Bob
Block #1 added successfully!
Block #2 added successfully!
Block #3 added successfully!
Chain validation successful! Total blocks: 4
Blockchain is valid: true
=== BLOCKCHAIN STATUS ===
Total blocks: 4
Authorized keys: 2
=== DEMO COMPLETED ===
```

## 💻 How It Works

### Step 1: Initialize Blockchain
```java
Blockchain blockchain = new Blockchain();
// Creates genesis block automatically
```

### Step 2: Add Authorized Users
```java
KeyPair userKeys = CryptoUtil.generateKeyPair();
String publicKey = CryptoUtil.publicKeyToString(userKeys.getPublic());
blockchain.addAuthorizedKey(publicKey, "UserName");
```

### Step 3: Add Blocks
```java
boolean success = blockchain.addBlock(
    "Your data here", 
    userKeys.getPrivate(), 
    userKeys.getPublic()
);
```

### Step 4: Validate Chain
```java
boolean isValid = blockchain.validateChain();
```

## 🧪 Testing

The project includes comprehensive test suites to verify all functionality.

### Recommended Testing Order

#### 1. Run All Tests (Complete Validation) ⭐ **RECOMMENDED**
```bash
./run_all_tests.sh
```
This runs everything: basic core tests + advanced function tests.

**Expected output:**
```
=== COMPREHENSIVE BLOCKCHAIN TEST RUNNER ===
✅ Compilation successful!
🎉 JUnit 5 Additional Advanced Functions tests: PASSED (22/22)
✅ Basic Core Functions test: PASSED
✅ Blockchain Demo: PASSED
✅ Simple Test: PASSED
✅ Quick Test: PASSED

📊 Test suites passed: 5/5
🎉 ALL TESTS PASSED SUCCESSFULLY!
```

#### 2. Advanced Functions Only (JUnit 5 Tests)
```bash
./run_core_tests.sh
```
Runs 22 professional JUnit 5 tests for advanced functions.

**Expected output:**
```
Tests run: 22, Failures: 0, Errors: 0, Skipped: 0
🎉 ALL TESTS PASSED!
```

#### 3. Interactive Demonstrations
```bash
# Advanced features demo with practical examples
mvn exec:java -Dexec.mainClass="com.rbatllet.blockchain.core.AdditionalAdvancedFunctionsDemo"

# Basic demo with multiple users
mvn exec:java -Dexec.mainClass="com.rbatllet.blockchain.BlockchainDemo"

# Core functions comprehensive test
mvn exec:java -Dexec.mainClass="com.rbatllet.blockchain.CoreFunctionsTest"
```

#### 4. Quick Verification Tests
```bash
# Fast verification
mvn exec:java -Dexec.mainClass="com.rbatllet.blockchain.QuickTest"

# Basic functionality
mvn exec:java -Dexec.mainClass="com.rbatllet.blockchain.SimpleTest"
```

### What Gets Tested

#### Core Blockchain Functions
- ✅ Genesis block creation
- ✅ Add/revoke authorized keys  
- ✅ Add blocks to chain
- ✅ Chain validation and integrity
- ✅ Security controls and authorization
- ✅ Error handling and edge cases

#### Advanced Functions (22 JUnit 5 Tests)
- ✅ **Block Size Validation**: Prevents oversized blocks
- ✅ **Chain Export**: Complete blockchain backup to JSON
- ✅ **Chain Import**: Blockchain restore from backup
- ✅ **Block Rollback**: Safe removal of recent blocks
- ✅ **Advanced Search**: Content, hash, and date range search
- ✅ **Integration**: All functions working together
- ✅ **Error Handling**: Graceful failure handling
- ✅ **Performance**: Execution time validation

### Troubleshooting Tests

#### If Tests Fail
```bash
# Reset database and try again
rm blockchain.db
./run_all_tests.sh

# Check Java version
java -version  # Should be 21+

# Validate environment
mvn clean compile test-compile
```

#### Database Issues
```bash
# Reset database
rm blockchain.db

# Check permissions
ls -la blockchain.db
```

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

## 🔒 Security Model

### Block Security
- Each block contains a SHA-256 hash of its content
- Blocks are linked by including the previous block's hash
- Any tampering breaks the chain validation

### Access Control
- Only users with authorized public keys can add blocks
- Each block is digitally signed with the user's private key
- Signatures are verified before accepting blocks

### Data Integrity
- All blocks are validated when checking the chain
- Hash verification ensures no data has been modified
- Sequential validation confirms proper block order

## 📊 Database Schema

The application automatically creates these tables:

### blocks table
- `id` - Unique identifier
- `block_number` - Sequential block number (starts from 0)
- `previous_hash` - Hash of the previous block
- `data` - Block content (user data)
- `hash` - SHA-256 hash of the block
- `signature` - Digital signature of the block
- `signer_public_key` - Public key of the block creator
- `timestamp` - When the block was created

### authorized_keys table
- `id` - Unique identifier
- `public_key` - User's public key (unique)
- `owner_name` - Human-readable name
- `is_active` - Whether the key is currently active
- `created_at` - When the key was added

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

// List authorized keys
List<AuthorizedKey> activeKeys = blockchain.getAuthorizedKeys();
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

## 🔧 Configuration

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

## 🚨 Important Notes

### Production Considerations
- **Key Management**: Store private keys securely
- **Database Security**: Consider encryption for sensitive data
- **Backup Strategy**: Regular database backups recommended
- **Access Control**: Implement proper user authentication

### Current Limitations
- **Single Database**: Uses one SQLite file
- **No Network**: Designed for single-application use
- **No Consensus**: No multi-node consensus mechanism
- **Key Recovery**: No built-in key recovery system

### Performance Notes
- **Block Size**: Large blocks may affect performance
- **Search Operations**: Content search is case-insensitive but may be slow with many blocks
- **Rollback Operations**: Large rollbacks may take time
- **Database Size**: Consider regular maintenance for large blockchains

## 🤝 Contributing

### Development Setup
1. Ensure Java 21+ and Maven 3.6+ are installed
2. Clone the repository
3. Run `mvn clean compile` to build
4. Run `./run_all_tests.sh` to verify everything works

### Testing New Features
1. Add your feature to the appropriate class
2. Create tests following the existing patterns
3. Run all tests to ensure nothing is broken
4. Update documentation as needed

### Code Style
- Use clear, descriptive variable names
- Add comments for complex logic
- Follow existing naming conventions
- Ensure proper error handling

## 📄 License

This project is provided as-is for educational and development purposes.

## 📞 Support

For issues or questions:
1. Check the troubleshooting section above
2. Verify your Java and Maven versions
3. Run `./run_all_tests.sh` to identify problems
4. Check the console output for specific error messages

---

**Ready to start?** Run `./run_all_tests.sh` to verify everything works, then try the demos!
