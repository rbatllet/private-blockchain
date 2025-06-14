# Private Blockchain Implementation

A comprehensive private blockchain implementation in Java with advanced features, security controls, and extensive testing.

## 📋 Table of Contents

- [Overview](#-overview)
- [Key Features](#-key-features)
- [Technologies Used](#-technologies-used)
- [Prerequisites](#-prerequisites)
- [Quick Start](#-quick-start)
- [How It Works](#-how-it-works)
- [Basic Testing](#-basic-testing)
- [Database Utilities and Script Management](#️-database-utilities-and-script-management)
- [Project Structure](#-project-structure)
- [Basic Usage Example](#-basic-usage-example)
- [Complete Usage Example](#-complete-usage-example)
- [Documentation](#-documentation)
- [Configuration](#-configuration)
- [Important Notes](#-important-notes)
- [Contributing](#-contributing)
- [License](#-license)
- [Support](#-support)

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

### 🔐 Enhanced Security Features
- **Safe Key Deletion**: Multi-layered protection against dangerous key removal
- **Impact Analysis**: Pre-deletion analysis to assess blockchain integrity risks
- **Emergency Key Deletion**: GDPR-compliant forced deletion with comprehensive audit trails
- **Blockchain Integrity Protection**: Prevents accidental corruption of historical records

### Technical Features
- **Persistence**: SQLite database with JPA standard for ORM (using Hibernate as provider)
- **Comprehensive Testing**: More than 40 JUnit 5 tests + integration demos
- **Production Ready**: Complete documentation and deployment guides
- **Clean Architecture**: Well-structured code with DAO pattern

## 🛠️ Technologies Used

- **Java 21** - Programming language with modern features
- **Maven** - Build and dependency management
- **SQLite** - Lightweight database for data storage
- **JPA** - Java Persistence API with Hibernate as implementation provider
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
mvn exec:java -Dexec.mainClass="com.rbatllet.blockchain.AdditionalAdvancedFunctionsDemo"
```

### 4. Run Chain Recovery Demo
```bash
# Run blockchain chain recovery demonstration
mvn exec:java -Dexec.mainClass="com.rbatllet.blockchain.ChainRecoveryDemo"
```

### 5. Run Key Deletion Safety Features Demo
```bash
# Run demonstration of key deletion safety features
mvn exec:java -Dexec.mainClass="com.rbatllet.blockchain.DangerousDeleteDemo"
```

### 6. Run Enhanced Recovery Example
```bash
# Run example of advanced recovery techniques
mvn exec:java -Dexec.mainClass="com.rbatllet.blockchain.EnhancedRecoveryExample"
```

### 7. Quick Functionality Test
```bash
# Run quick verification test
mvn exec:java -Dexec.mainClass="com.rbatllet.blockchain.QuickDemo"
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

The project includes extensive testing with **more than 40 JUnit 5 tests** plus integration demos:

#### Run All Tests (Recommended)
```bash
./run_all_tests.sh
```

**Expected Output:**
```
=== COMPREHENSIVE BLOCKCHAIN TEST RUNNER ===
✅ Compilation successful!
🎉 JUnit 5 tests: PASSED (more than 40 tests)
✅ Basic Core Functions test: PASSED
✅ Blockchain Demo: PASSED
✅ Simple Test: PASSED
✅ Quick Test: PASSED

📊 Test suites passed: 5/5
🎉 ALL TESTS PASSED SUCCESSFULLY!
```

#### Individual Test Categories
```bash
# Advanced functions only (JUnit 5 tests)
./run_advanced_tests.sh

# Basic core functions only
./run_basic_tests.sh

# Core functions comprehensive test
mvn exec:java -Dexec.mainClass="com.rbatllet.blockchain.CoreFunctionsDemo"
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
│   └── JPAUtil.java                             # JPA EntityManager management
├── BlockchainDemo.java                          # Basic demo application
├── AdditionalAdvancedFunctionsDemo.java         # Advanced features demo
├── ChainRecoveryDemo.java                       # Chain recovery demonstration
├── DangerousDeleteDemo.java                     # Key deletion safety features demo
├── EnhancedRecoveryExample.java                 # Advanced recovery techniques example
├── CoreFunctionsDemo.java                       # Comprehensive core test
├── SimpleDemo.java                              # Basic functionality test
└── QuickDemo.java                               # Fast verification test

src/test/java/com/rbatllet/blockchain/core/
├── BlockchainAdditionalAdvancedFunctionsTest.java   # JUnit 5 test suite
├── BlockchainKeyAuthorizationTest.java             # Key authorization tests
├── CriticalConsistencyTest.java                    # Consistency validation tests
├── SimpleTemporalValidationTest.java               # Temporal validation tests
└── TestEnvironmentValidator.java                    # Environment validation

src/test/java/com/rbatllet/blockchain/dao/
└── AuthorizedKeyDAODeleteTest.java                 # DAO delete operation tests

Configuration & Scripts:
├── src/main/resources/META-INF/persistence.xml  # JPA configuration
├── src/main/resources/logging.properties      # Logging configuration
├── src/test/resources/test.properties         # Test configuration
├── clean-database.sh                            # Database cleanup utility
├── run_all_tests.sh                             # Complete test runner
├── run_advanced_tests.sh                        # Advanced tests only
├── run_basic_tests.sh                           # Basic tests only
├── scripts/                                     # Script utilities directory
│   ├── shared-functions.sh                     # Common functions library
│   ├── run_template.sh                         # Template for new scripts
│   └── check-db-cleanup.sh                     # Script compliance checker
└── pom.xml                                      # Maven configuration
```

## 🛠️ Database Utilities and Script Management

### Automatic Database Cleanup
All test scripts now include automatic database cleanup to prevent SQLite corruption issues:

```bash
# All scripts automatically clean corrupted database files
./run_all_tests.sh      # Includes automatic cleanup
./run_advanced_tests.sh # Includes automatic cleanup  
./run_basic_tests.sh    # Includes automatic cleanup
```

### Manual Database Cleanup
If you encounter database corruption issues:

```bash
# Clean corrupted database files manually
./clean-database.sh

# Skip automatic cleanup (for debugging)
SKIP_DB_CLEANUP=true ./run_all_tests.sh
```

### Script Development

#### Creating New Test Scripts
Use the provided template for consistent script structure:

```bash
# Copy template for new test script
cp scripts/run_template.sh run_my_new_test.sh

# Make executable and customize
chmod +x run_my_new_test.sh
# Edit the script to add your test logic
```

#### Verify Script Compliance
Check that all run_*.sh scripts include database cleanup:

```bash
# Verify all scripts have proper database cleanup
./scripts/check-db-cleanup.sh
```

**Expected Output:**
```
✅ All run_*.sh scripts are up to date! ✨
  ✅ Up to date: 3 scripts  
  🔧 Need update: 0 scripts
```

### Shared Functions Library
All scripts now use a centralized functions library at `scripts/shared-functions.sh` providing:

- **Database cleanup functions**: Prevent corruption issues
- **Colored output functions**: Consistent formatting
- **Error handling utilities**: Robust script execution
- **Test environment setup**: Standardized initialization

> 📚 **For detailed implementation information**, see [SCRIPTS_DATABASE_FIX.md](docs/SCRIPTS_DATABASE_FIX.md)

## 🔐 Safe Key Management

The blockchain includes advanced safety features for key management to prevent accidental data corruption:

### Key Deletion Safety Levels

```java
// 🟢 LEVEL 1: Impact Analysis (RECOMMENDED FIRST STEP)
Blockchain.KeyDeletionImpact impact = blockchain.canDeleteAuthorizedKey(publicKey);
System.out.println("Impact: " + impact);

// 🟡 LEVEL 2: Safe Deletion (blocks dangerous operations)
boolean safe = blockchain.deleteAuthorizedKey(publicKey);

// 🟠 LEVEL 3: Dangerous with Safety (still protected by default)
boolean dangerous = blockchain.dangerouslyDeleteAuthorizedKey(publicKey, "GDPR compliance");

// 🔴 LEVEL 4: Nuclear Option (breaks validation - emergency use only)
boolean forced = blockchain.dangerouslyDeleteAuthorizedKey(publicKey, true, "Security incident");
```

### Safe Usage Pattern

```java
// ALWAYS follow this pattern for key deletion:
public void safeKeyDeletionWorkflow(String publicKey, String reason) {
    // Step 1: Analyze impact
    Blockchain.KeyDeletionImpact impact = blockchain.canDeleteAuthorizedKey(publicKey);
    
    // Step 2: Check if safe
    if (impact.canSafelyDelete()) {
        blockchain.deleteAuthorizedKey(publicKey);  // Safe deletion
    } else {
        System.out.println("⚠️ Key has " + impact.getAffectedBlocks() + " blocks");
        // Only use dangerous deletion in emergencies:
        // blockchain.dangerouslyDeleteAuthorizedKey(publicKey, true, reason);
    }
}
```

**⚠️ Important**: Forced deletion (`force=true`) will **permanently break** blockchain validation for historical blocks signed by the deleted key. Only use for:
- GDPR "right to be forgotten" compliance
- Security incidents with compromised keys  
- Emergency situations requiring complete key removal

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
- **[EXAMPLES.md](docs/EXAMPLES.md)** - Real-world use cases and workflow patterns
- **[API_GUIDE.md](docs/API_GUIDE.md)** - Complete API reference and core functions
- **[TESTING.md](docs/TESTING.md)** - Comprehensive testing guide and troubleshooting

### 🏢 Technical & Production
- **[TECHNICAL_DETAILS.md](docs/TECHNICAL_DETAILS.md)** - Database schema, security model, architecture
- **[PRODUCTION_GUIDE.md](docs/PRODUCTION_GUIDE.md)** - Production deployment and operational guidelines
- **[SCRIPTS_DATABASE_FIX.md](docs/SCRIPTS_DATABASE_FIX.md)** - Database cleanup utilities implementation guide

### 🚀 Quick Navigation

| What you want to do | Go to |
|---------------------|-------|
| See real-world examples and use cases | [EXAMPLES.md](docs/EXAMPLES.md) |
| Learn the API and core functions | [API_GUIDE.md](docs/API_GUIDE.md) |
| Run tests and troubleshoot issues | [TESTING.md](docs/TESTING.md) |
| Understand technical implementation | [TECHNICAL_DETAILS.md](docs/TECHNICAL_DETAILS.md) |
| Deploy to production | [PRODUCTION_GUIDE.md](docs/PRODUCTION_GUIDE.md) |

## 🔧 Configuration

### Size Limits
- **Block Data**: 10,000 characters maximum
- **Block Size**: 1MB (1,048,576 bytes) maximum
- **Hash Length**: 64 characters (SHA-256)

### Database
- **Location**: `blockchain.db` in project root directory
- **Type**: SQLite database with automatic table creation
- **JPA Provider**: Hibernate as JPA implementation
- **Configuration**: `persistence.xml` for JPA settings

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
4. **Test**: Run `./run_all_tests.sh` to verify everything works (more than 40 tests)
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

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

### What this means:
- ✅ **Commercial use** - Use in commercial projects
- ✅ **Modification** - Modify and create derivative works  
- ✅ **Distribution** - Distribute original or modified versions
- ✅ **Private use** - Use privately without sharing source
- ✅ **No warranty** - Software provided "as is"

## 📞 Support

For issues or questions:
1. Check the [TESTING.md](docs/TESTING.md) troubleshooting section
2. Verify your Java and Maven versions meet requirements
3. Run `./run_all_tests.sh` to identify problems
4. Check console output for specific error messages
5. Review [TECHNICAL_DETAILS.md](docs/TECHNICAL_DETAILS.md) for implementation details

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
2. Try the practical examples in [EXAMPLES.md](docs/EXAMPLES.md) for your use case
3. Explore the comprehensive test suite to understand all features
4. Build your own blockchain application using the patterns provided!

**💡 Remember**: This blockchain includes **more than 40 comprehensive tests** covering everything from basic operations to critical consistency scenarios, ensuring enterprise-grade reliability for your applications.
