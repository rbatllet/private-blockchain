# Private Blockchain

A simple and secure private blockchain implementation using Java 21, SQLite, and Hibernate.

## 📋 Overview

This project is a **private blockchain** designed for controlled environments where only authorized users can add blocks. Unlike public blockchains, there is no mining process - blocks are added directly by authorized parties.

## ✨ Features

### Core Blockchain Features
- **Genesis Block**: Automatically created when the blockchain starts
- **Block Creation**: Add new blocks with data to the chain
- **Hash Verification**: SHA-256 cryptographic hashing for data integrity
- **Digital Signatures**: RSA signatures to verify block authenticity
- **Chain Validation**: Complete blockchain integrity checking

### Security Features
- **Authorized Keys**: Only approved users can add blocks
- **Cryptographic Protection**: Each block is cryptographically signed
- **Immutable Records**: Once added, blocks cannot be changed
- **Sequential Validation**: Each block references the previous block

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

## 📦 Project Structure

```
src/main/java/com/rbatllet/blockchain/
├── core/
│   └── Blockchain.java          # Main blockchain logic
├── dao/
│   ├── BlockDAO.java            # Database operations for blocks
│   └── AuthorizedKeyDAO.java    # Database operations for keys
├── entity/
│   ├── Block.java               # Block data model
│   └── AuthorizedKey.java       # Authorized key data model
├── util/
│   ├── CryptoUtil.java          # Cryptographic utilities
│   └── HibernateUtil.java       # Database connection management
└── BlockchainDemo.java          # Demo application
```

## 🚀 How to Run

### Prerequisites
- **Java 21** or higher
- **Maven 3.6** or higher

### Step 1: Clone and Navigate
```bash
cd /path/to/your/directory
# Project should be in: privateBockChain/
```

### Step 2: Compile the Project
```bash
mvn clean compile
```

### Step 3: Run the Demo
```bash
mvn exec:java -Dexec.mainClass="com.rbatllet.blockchain.BlockchainDemo"
```

### Expected Output
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

### 1. Initialize Blockchain
```java
Blockchain blockchain = new Blockchain();
// Creates genesis block automatically
```

### 2. Add Authorized Users
```java
KeyPair userKeys = CryptoUtil.generateKeyPair();
String publicKey = CryptoUtil.publicKeyToString(userKeys.getPublic());
blockchain.addAuthorizedKey(publicKey, "UserName");
```

### 3. Add Blocks
```java
boolean success = blockchain.addBlock(
    "Your data here", 
    userKeys.getPrivate(), 
    userKeys.getPublic()
);
```

### 4. Validate Chain
```java
boolean isValid = blockchain.validateChain();
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

### blocks
- `id` - Unique identifier
- `block_number` - Sequential block number
- `previous_hash` - Hash of the previous block
- `data` - Block content
- `hash` - SHA-256 hash of the block
- `signature` - Digital signature
- `signer_public_key` - Public key of the block creator
- `timestamp` - When the block was created

### authorized_keys
- `id` - Unique identifier
- `public_key` - User's public key (unique)
- `owner_name` - Human-readable name
- `is_active` - Whether the key is currently active
- `created_at` - When the key was added

## 🧪 Testing

The project includes several test classes to verify functionality and demonstrate usage.

### Available Test Classes

#### 1. Complete Core Functions Test
**File**: `CoreFunctionsTest.java`  
**Purpose**: Tests ALL core blockchain functions with detailed validation

```bash
mvn exec:java -Dexec.mainClass="com.rbatllet.blockchain.CoreFunctionsTest"
```

**What it tests:**
- ✅ Genesis block creation
- ✅ Add/revoke authorized keys
- ✅ Add blocks to chain
- ✅ Chain validation
- ✅ Security controls
- ✅ Error handling

**Expected output:**
```
=== TESTING ALL CORE FUNCTIONS ===
1. TESTING: Initialize Blockchain + Genesis Block
   ✓ Genesis block created
   ✓ Initial block count: 1
   SUCCESS: Blockchain initialized

2. TESTING: Add Authorized Keys
   ✓ Alice added: true
   ✓ Bob added: true
   ✓ Charlie added: true
   SUCCESS: Authorized keys management working

[... more detailed testing ...]

🎉 ALL CORE FUNCTIONS WORKING PERFECTLY! 🎉
```

#### 2. Demo Application
**File**: `BlockchainDemo.java`  
**Purpose**: Complete demonstration with multiple users and transactions

```bash
mvn exec:java -Dexec.mainClass="com.rbatllet.blockchain.BlockchainDemo"
```

**What it demonstrates:**
- Multi-user blockchain usage
- Real transaction examples
- Chain validation
- Final statistics

#### 3. Simple Test
**File**: `SimpleTest.java`  
**Purpose**: Basic functionality test with one user

```bash
mvn exec:java -Dexec.mainClass="com.rbatllet.blockchain.SimpleTest"
```

**What it shows:**
- Basic blockchain operations
- Single user workflow
- Block creation and validation

#### 4. Quick Test
**File**: `QuickTest.java`  
**Purpose**: Fast verification that blockchain works

```bash
mvn exec:java -Dexec.mainClass="com.rbatllet.blockchain.QuickTest"
```

**What it verifies:**
- Blockchain initialization
- Block addition
- Basic functionality

### Running Tests Step by Step

#### Prerequisites
1. Make sure you have Java 21+ and Maven installed
2. Navigate to the project directory
3. Compile the project first

#### Complete Test Workflow
```bash
# 1. Navigate to project directory
cd /path/to/privateBockChain

# 2. Clean and compile
mvn clean compile

# 3. Run complete core functions test (RECOMMENDED)
mvn exec:java -Dexec.mainClass="com.rbatllet.blockchain.CoreFunctionsTest"

# 4. Optional: Run demo application
mvn exec:java -Dexec.mainClass="com.rbatllet.blockchain.BlockchainDemo"

# 5. Optional: Run simple tests
mvn exec:java -Dexec.mainClass="com.rbatllet.blockchain.SimpleTest"
mvn exec:java -Dexec.mainClass="com.rbatllet.blockchain.QuickTest"
```

#### Database Reset
To start fresh between tests:
```bash
# Remove the database file
rm blockchain.db

# Then run any test
mvn exec:java -Dexec.mainClass="com.rbatllet.blockchain.CoreFunctionsTest"
```

### Test Results Interpretation

#### Success Indicators
- ✅ All functions return `true` for successful operations
- ✅ "Chain validation successful!" message appears
- ✅ "Blockchain is valid: true" confirmation
- ✅ Expected number of blocks and authorized keys

#### Failure Indicators
- ❌ Functions return `false` for failed operations
- ❌ "Chain validation failed" error messages
- ❌ Exception stack traces
- ❌ Assertion errors in CoreFunctionsTest

#### Common Test Outputs
```bash
# Successful block addition
Block #1 added successfully!

# Successful key management
Authorized key added for: Alice

# Successful validation
Chain validation successful! Total blocks: 4
Blockchain is valid: true

# Security working correctly
Unauthorized key attempting to add block  # This is expected!
```

### Troubleshooting Tests

#### Test Compilation Issues
```bash
# Clean and recompile
mvn clean compile

# Check Java version
java -version  # Should be 21+
```

#### Database Issues
```bash
# Reset database
rm blockchain.db

# Check file permissions
ls -la blockchain.db
```

#### Memory or Performance Issues
```bash
# Run with more memory if needed
mvn exec:java -Dexec.mainClass="com.rbatllet.blockchain.CoreFunctionsTest" -Dexec.args="-Xmx512m"
```

### Creating Custom Tests

You can create your own test class following this pattern:

```java
public class MyCustomTest {
    public static void main(String[] args) {
        try {
            Blockchain blockchain = new Blockchain();
            
            // Your custom test logic here
            // Generate keys, add blocks, validate, etc.
            
            System.out.println("Custom test completed successfully!");
        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
        }
    }
}
```

Then run it with:
```bash
mvn exec:java -Dexec.mainClass="com.rbatllet.blockchain.MyCustomTest"
```

## 🎯 Core Functions Usage

This section shows how to use each **CORE function** of the blockchain with practical examples.

### 1. Initialize Blockchain (Genesis Block)
```java
// Creates blockchain and genesis block automatically
Blockchain blockchain = new Blockchain();
// Output: "Genesis block created successfully!"
```

### 2. Add Authorized Key
```java
// Generate key pair for a user
KeyPair userKeys = CryptoUtil.generateKeyPair();
String publicKey = CryptoUtil.publicKeyToString(userKeys.getPublic());

// Add user to authorized list
boolean success = blockchain.addAuthorizedKey(publicKey, "Alice");
// Output: "Authorized key added for: Alice"
```

### 3. Add Block to Chain
```java
// Add a new block with data (requires authorized key)
boolean blockAdded = blockchain.addBlock(
    "Transaction: Alice sends 100 tokens to Bob",  // Your data
    userKeys.getPrivate(),                         // Private key to sign
    userKeys.getPublic()                          // Public key for verification
);

if (blockAdded) {
    System.out.println("Block added successfully!");
} else {
    System.out.println("Failed to add block");
}
```

### 4. Validate Individual Block
```java
// This is called automatically when adding blocks, but you can also:
// Get a specific block
Block block = blockchain.getBlock(1);  // Get block number 1

// The validateBlock() method is private, but validation happens when:
// - Adding new blocks
// - Validating the entire chain
```

### 5. Validate Entire Chain
```java
// Check if the entire blockchain is valid
boolean isValid = blockchain.validateChain();

if (isValid) {
    System.out.println("Blockchain is valid and secure!");
} else {
    System.out.println("Blockchain has been compromised!");
}
```

### 6. Revoke Authorized Key
```java
// Remove access for a user
boolean revoked = blockchain.revokeAuthorizedKey(publicKey);

if (revoked) {
    System.out.println("Key access revoked successfully");
} else {
    System.out.println("Key not found or already inactive");
}
```

### Complete Example: Using All Core Functions
```java
public class BlockchainExample {
    public static void main(String[] args) {
        try {
            // 1. CORE: Initialize blockchain (creates genesis block)
            Blockchain blockchain = new Blockchain();
            
            // 2. CORE: Add authorized keys
            KeyPair alice = CryptoUtil.generateKeyPair();
            KeyPair bob = CryptoUtil.generateKeyPair();
            
            String alicePublicKey = CryptoUtil.publicKeyToString(alice.getPublic());
            String bobPublicKey = CryptoUtil.publicKeyToString(bob.getPublic());
            
            blockchain.addAuthorizedKey(alicePublicKey, "Alice");
            blockchain.addAuthorizedKey(bobPublicKey, "Bob");
            
            // 3. CORE: Add blocks to chain
            blockchain.addBlock("Alice registers in system", 
                              alice.getPrivate(), alice.getPublic());
                              
            blockchain.addBlock("Bob joins the network", 
                              bob.getPrivate(), bob.getPublic());
                              
            blockchain.addBlock("Alice transfers data to Bob", 
                              alice.getPrivate(), alice.getPublic());
            
            // 4. CORE: Validate entire chain
            boolean isValid = blockchain.validateChain();
            System.out.println("Blockchain is valid: " + isValid);
            
            // 5. CORE: Revoke access (optional)
            // blockchain.revokeAuthorizedKey(bobPublicKey);
            
            // View results
            System.out.println("Total blocks: " + blockchain.getBlockCount());
            System.out.println("Authorized users: " + blockchain.getAuthorizedKeys().size());
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
```

### Core Function Summary

| Function | Purpose | Returns | Notes |
|----------|---------|---------|-------|
| `new Blockchain()` | Initialize + Genesis Block | Blockchain object | Automatic genesis creation |
| `addAuthorizedKey()` | Add user permissions | boolean | Required before adding blocks |
| `addBlock()` | Add data to chain | boolean | Validates and signs automatically |
| `validateChain()` | Check chain integrity | boolean | Verifies all blocks |
| `revokeAuthorizedKey()` | Remove user access | boolean | Deactivates key |

## 📝 API Usage

### Core Methods

#### Blockchain Management
```java
// Get blockchain statistics
long totalBlocks = blockchain.getBlockCount();
List<Block> allBlocks = blockchain.getAllBlocks();
Block lastBlock = blockchain.getLastBlock();

// Get blocks by time range
List<Block> recentBlocks = blockchain.getBlocksByTimeRange(startTime, endTime);
```

#### Key Management
```java
// Add authorized key
blockchain.addAuthorizedKey(publicKeyString, "User Name");

// Revoke key access
blockchain.revokeAuthorizedKey(publicKeyString);

// List authorized keys
List<AuthorizedKey> activeKeys = blockchain.getAuthorizedKeys();
```

#### Block Operations
```java
// Add block with data
boolean success = blockchain.addBlock(data, privateKey, publicKey);

// Get specific block
Block block = blockchain.getBlock(blockNumber);

// Validate entire chain
boolean isValid = blockchain.validateChain();
```

## 🔧 Configuration

### Database Location
The SQLite database is created as `blockchain.db` in the project root directory.

### Hibernate Configuration
Database settings are in `src/main/resources/hibernate.cfg.xml`:
- Automatic table creation
- SQL query logging (can be disabled)
- Connection pooling

## 🚨 Important Notes

### Production Considerations
- **Key Management**: Secure private key storage is critical
- **Database Security**: Consider encryption for sensitive data
- **Network Security**: Use HTTPS for any network communication
- **Backup Strategy**: Regular database backups recommended

### Limitations
- **Single Database**: Currently uses one SQLite file
- **No Network**: Designed for single-application use
- **No Consensus**: No multi-node consensus mechanism
- **Key Recovery**: No built-in key recovery mechanism

## 🤝 Use Cases

This private blockchain is ideal for:
- **Document Tracking**: Immutable document version history
- **Audit Trails**: Tamper-proof activity logging
- **Supply Chain**: Product authenticity verification
- **Internal Records**: Company transaction logging
- **Compliance**: Regulatory requirement tracking

## 📄 License

This project is for educational and development purposes.

## 🆘 Troubleshooting

### Common Issues

**Compilation Errors**
- Ensure Java 21+ is installed
- Check Maven configuration
- Verify all dependencies are downloaded

**Database Issues**
- Delete `blockchain.db` to reset
- Check file permissions
- Ensure SQLite JDBC driver is available

**Runtime Errors**
- Check that authorized keys are added before creating blocks
- Verify private/public key pairs match
- Ensure proper exception handling

### Getting Help
If you encounter issues:
1. Check the console output for error messages
2. Verify your Java and Maven versions
3. Try deleting the database file and restarting
4. Review the demo code for proper usage examples