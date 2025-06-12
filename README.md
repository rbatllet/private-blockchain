# Private Blockchain Implementation

A comprehensive private blockchain implementation in Java with advanced features, security controls, and extensive testing.

## 📋 Overview

This is a **private blockchain** for controlled environments where only authorized users can add blocks. Unlike public blockchains, there is no mining - blocks are added directly by authorized users through cryptographic authorization.

**Key Differences from Public Blockchains:**
- **Controlled Access**: Only pre-approved users can add blocks
- **No Mining**: Direct block creation with digital signatures
- **Private Environment**: Designed for organizational/enterprise use
- **Full Control**: Complete control over participants and data

## 🎯 Key Features

### Core Blockchain Features
- **Genesis Block**: Created automatically when blockchain starts
- **Secure Blockchain**: SHA-256 hashing with RSA digital signatures
- **Access Control**: Authorized key management for secure operations
- **Chain Validation**: Complete blockchain integrity checking
- **Immutable Records**: Blocks cannot be changed once added

### Advanced Functions
- **Export/Import**: Backup and restore complete blockchain with temporal consistency
- **Search Capabilities**: Find blocks by content, hash, or date range
- **Rollback Operations**: Safe removal of recent blocks with genesis protection
- **Block Size Validation**: Prevents oversized blocks

### Technical Features
- **Persistence**: SQLite database with Hibernate ORM
- **Comprehensive Testing**: 22 JUnit 5 tests + integration demos
- **Production Ready**: Complete documentation and deployment guides
- **Clean Architecture**: Well-structured code with DAO pattern

## 🛠️ Technologies Used

- **Java 21** - Programming language with modern features
- **Maven** - Build and dependency management
- **SQLite** - Lightweight database for data storage
- **Hibernate** - Object-relational mapping (ORM)
- **SHA-256** - Cryptographic hash function for integrity
- **RSA** - Digital signature algorithm for authentication
- **JUnit 5** - Testing framework for comprehensive validation

## 📦 Prerequisites

- **Java 21** or higher installed
- **Maven 3.6** or higher
- At least **50MB** of free disk space

### Checking Prerequisites
```bash
# Check Java version (must be 21+)
java -version

# Check Maven version
mvn -version
```

## 🚀 Quick Start

### 1. Clone and Build
```bash
# Navigate to project directory
cd /path/to/privateBlockchain

# Compile the project
mvn clean compile

# Package the application (creates JAR with dependencies)
mvn package
```

### 2. Run Basic Demo
```bash
# Run the basic blockchain demo
mvn exec:java -Dexec.mainClass="com.rbatllet.blockchain.BlockchainDemo"
```

**Expected Output:**
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

### 3. Run Advanced Features Demo
```bash
# Run advanced features demonstration
mvn exec:java -Dexec.mainClass="com.rbatllet.blockchain.core.AdditionalAdvancedFunctionsDemo"
```

### 4. Quick Functionality Test
```bash
# Run quick verification test
mvn exec:java -Dexec.mainClass="com.rbatllet.blockchain.QuickTest"
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

## 🧪 Basic Testing

### Comprehensive Test Suite

The project includes extensive testing with **22 JUnit 5 tests** plus integration demos:

#### Run All Tests (Recommended)
```bash
./run_all_tests.sh
```

**Expected Output:**
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

#### Individual Test Categories
```bash
# Advanced functions only (22 JUnit 5 tests)
./run_advanced_tests.sh

# Basic core functions only
./run_basic_tests.sh

# Core functions comprehensive test
mvn exec:java -Dexec.mainClass="com.rbatllet.blockchain.CoreFunctionsTest"
```

### What Gets Tested
- ✅ **Genesis block creation** and initialization
- ✅ **Authorized key management** (add/revoke/list)
- ✅ **Block addition** with proper authorization
- ✅ **Chain validation** and integrity checking
- ✅ **Advanced features**: Export/Import, Search, Rollback
- ✅ **Error handling** and edge cases
- ✅ **Performance** and size validation

## 📊 Project Structure

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

Configuration & Scripts:
├── hibernate.cfg.xml                            # Database configuration
├── run_all_tests.sh                             # Complete test runner
├── run_advanced_tests.sh                        # Advanced tests only
├── run_basic_tests.sh                           # Basic tests only
└── pom.xml                                      # Maven configuration
```

## 💡 Basic Usage Example

```java
// 1. Initialize blockchain
Blockchain blockchain = new Blockchain();

// 2. Add authorized users
KeyPair alice = CryptoUtil.generateKeyPair();
String alicePublicKey = CryptoUtil.publicKeyToString(alice.getPublic());
blockchain.addAuthorizedKey(alicePublicKey, "Alice");

// 3. Add blocks
blockchain.addBlock("Transaction: Payment to Bob", 
                   alice.getPrivate(), alice.getPublic());

// 4. Validate chain
boolean isValid = blockchain.validateChain();
System.out.println("Blockchain is valid: " + isValid);

// 5. Search blocks
List<Block> results = blockchain.searchBlocksByContent("Payment");

// 6. Export for backup
blockchain.exportChain("backup.json");
```

## 💡 Complete Usage Example

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

## 📚 Documentation

This project includes comprehensive documentation for different use cases:

### 📖 User Guides
- **[EXAMPLES.md](EXAMPLES.md)** - Real-world use cases and workflow patterns
- **[API_GUIDE.md](API_GUIDE.md)** - Complete API reference and core functions
- **[TESTING.md](TESTING.md)** - Comprehensive testing guide and troubleshooting

### 🏢 Technical & Production
- **[TECHNICAL_DETAILS.md](TECHNICAL_DETAILS.md)** - Database schema, security model, architecture
- **[PRODUCTION_GUIDE.md](PRODUCTION_GUIDE.md)** - Production deployment and operational guidelines

### 🚀 Quick Navigation

| What you want to do | Go to |
|---------------------|-------|
| See real-world examples and use cases | [EXAMPLES.md](EXAMPLES.md) |
| Learn the API and core functions | [API_GUIDE.md](API_GUIDE.md) |
| Run tests and troubleshoot issues | [TESTING.md](TESTING.md) |
| Understand technical implementation | [TECHNICAL_DETAILS.md](TECHNICAL_DETAILS.md) |
| Deploy to production | [PRODUCTION_GUIDE.md](PRODUCTION_GUIDE.md) |

## 🔧 Configuration

### Size Limits
- **Block Data**: 10,000 characters maximum
- **Block Size**: 1MB (1,048,576 bytes) maximum
- **Hash Length**: 64 characters (SHA-256)

### Database
- **Location**: `blockchain.db` in project root directory
- **Type**: SQLite database with automatic table creation
- **ORM**: Hibernate for database operations

### Security
- **Hash Algorithm**: SHA-256 for block integrity
- **Signature Algorithm**: RSA with 2048-bit keys
- **Access Control**: Authorized public key validation

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
- **Search Operations**: Content search may be slow with many blocks
- **Database Size**: Consider regular maintenance for large blockchains

## 🤝 Contributing

### Development Setup
1. **Environment**: Ensure Java 21+ and Maven 3.6+ are installed
2. **Clone**: Clone the repository to your local development environment
3. **Build**: Run `mvn clean compile` to build the project
4. **Test**: Run `./run_all_tests.sh` to verify everything works (22+ tests)
5. **IDE**: Import as Maven project in your preferred IDE

### Testing New Features
1. **Feature Development**: Add your feature to the appropriate class following existing patterns
2. **Unit Tests**: Create comprehensive JUnit 5 tests following existing test structures
3. **Integration Tests**: Ensure your feature works with existing functionality
4. **Consistency Tests**: Add critical consistency tests for complex scenarios
5. **Documentation**: Update README.md and add code comments
6. **Full Test Suite**: Run `./run_all_tests.sh` to ensure nothing is broken

### Code Quality Standards
- **Clear Naming**: Use descriptive variable and method names
- **Comments**: Add comprehensive comments for complex logic
- **Error Handling**: Implement proper exception handling and logging
- **Consistency**: Follow existing naming conventions and code style
- **Performance**: Consider performance implications of new features
- **Security**: Ensure new features maintain security properties

### Contribution Areas
- **Performance Optimization**: Database query optimization, caching strategies
- **Security Enhancements**: Additional cryptographic features, audit capabilities
- **Monitoring**: Health check improvements, metrics collection
- **Integration**: APIs for external systems, import/export formats
- **Documentation**: Examples, tutorials, best practices guides

## 📄 License

This project is provided as-is for educational and development purposes.

## 📞 Support

For issues or questions:
1. Check the [TESTING.md](TESTING.md) troubleshooting section
2. Verify your Java and Maven versions meet requirements
3. Run `./run_all_tests.sh` to identify problems
4. Check console output for specific error messages
5. Review [TECHNICAL_DETAILS.md](TECHNICAL_DETAILS.md) for implementation details

### Getting Help
1. **Check this documentation** for common use cases and examples
2. **Review the test files** for comprehensive usage examples
3. **Run the health check** using the workflow patterns above
4. **Examine the console output** for specific error messages
5. **Verify your environment** meets the prerequisites

### Complete Verification Procedure
```bash
# Complete verification steps
cd /path/to/privateBlockchain
mvn clean compile test-compile
./run_all_tests.sh
mvn exec:java -Dexec.mainClass="com.rbatllet.blockchain.BlockchainDemo"
```

### Quick Troubleshooting
```bash
# Reset environment if tests fail
rm blockchain.db blockchain.db-*
./run_all_tests.sh

# Check Java version (should be 21+)
java -version

# Clean build
mvn clean compile test-compile
```

---

**🚀 Ready to start?** 

1. Run `./run_all_tests.sh` to verify everything works perfectly
2. Try the practical examples in [EXAMPLES.md](EXAMPLES.md) for your use case
3. Explore the comprehensive test suite to understand all features
4. Build your own blockchain application using the patterns provided!

**💡 Remember**: This blockchain includes **22+ comprehensive tests** covering everything from basic operations to critical consistency scenarios, ensuring enterprise-grade reliability for your applications.
