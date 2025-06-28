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
- **Modern Cryptography**: SHA3-256 hashing with ECDSA digital signatures (secp256r1 curve)
- **Access Control**: Hierarchical key management (Root/Intermediate/Operational)
- **Chain Validation**: Detailed blockchain integrity checking with `validateChainDetailed()`
- **Immutable Records**: Cryptographically secured blocks with digital signatures

### Advanced Functions
- **Export/Import**: Backup and restore complete blockchain with temporal consistency
- **Search Capabilities**: Find blocks by content, hash, or date range
- **Rollback Operations**: Safe removal of recent blocks with genesis protection
- **Off-Chain Storage**: Automatic storage for large data (>512KB) with AES-CBC encryption
- **Data Size Management**: Intelligent data placement based on configurable size thresholds
- **Integrity Verification**: Cryptographic verification of off-chain data with hash and signature validation
- **Detailed Validation**: Enhanced `validateChainDetailed()` with comprehensive off-chain data analysis
- **Real-time Monitoring**: Live validation results with detailed file status, integrity checks, and storage metrics
- **Data Consistency**: Complete synchronization between database and file system operations
- **Automatic Cleanup**: Off-chain file cleanup during rollback, export/import, and maintenance operations

### 🔐 Enhanced Security Features

### ⚡ Modern Cryptography
- **ECDSA Signatures**: Using secp256r1 (NIST P-256) curve
- **SHA3-256 Hashing**: Modern cryptographic hash function
- **Key Hierarchy**: Three-tier key management (Root/Intermediate/Operational)
- **Automatic Key Rotation**: Built-in key rotation policies
- **Key Revocation**: Secure key revocation with audit trails

### 🛡️ Security Controls
- **Thread-Safe Implementation**: Safe for concurrent access
- **Database Encryption**: Sensitive data at rest encryption
- **Secure Key Storage**: AES-256 encrypted key storage
- **Audit Logging**: Comprehensive security event logging
- **Input Validation**: Protection against injection attacks

### 🔄 Key Management
- **Root Keys**: 5-year validity, signs intermediate keys
- **Intermediate Keys**: 1-year validity, signs operational keys
- **Operational Keys**: 90-day validity, used for daily operations
- **Automatic Expiration**: Keys automatically expire based on type
- **Revocation Support**: Immediate key revocation capability

### 🧰️ Utility Classes
- **ExitUtil**: Test-compatible system exit handling
- **BlockValidationUtil**: Utilities for block validation
- **BlockValidationResult**: Representation of validation results
- **FormatUtil**: Formatting of blockchain data for display

### Technical Features
- **Persistence**: SQLite database with JPA standard for ORM (using Hibernate as provider)
- **Off-Chain Storage**: Encrypted file storage with automatic data tiering (AES-128-CBC)
- **Comprehensive Testing**: More than 40 JUnit 5 tests + integration demos + off-chain storage tests
- **Production Ready**: Complete documentation and deployment guides
- **Clean Architecture**: Well-structured code with DAO pattern
- **Scalable Storage**: Support for data up to 100MB per block through off-chain storage

## 🛠️ Technologies Used

- **Java 21** - Programming language with modern features
- **Maven** - Build and dependency management
- **SQLite** - Lightweight database for data storage
- **JPA** - Java Persistence API with Hibernate as implementation provider
- **Cryptography**:
  - **Hashing**: SHA3-256 (modern, secure hash function)
  - **Digital Signatures**: ECDSA with secp256r1 (NIST P-256) curve
  - **Key Management**: Hierarchical key structure with automatic rotation
  - **Encryption**: AES-128-CBC for off-chain data encryption with unique IV per file
- **JUnit 5** - Testing framework for comprehensive validation

## 📦 Prerequisites

- **Java 21** or higher installed
- **Maven 3.6** or higher
- At least **500MB** of free disk space (additional space needed for off-chain storage)

### Checking Prerequisites
```zsh
# Check Java version (must be 21+)
java -version

# Check Maven version
mvn -version
```

## 🚀 Quick Start

### 1. Clone and Build
```zsh
# Navigate to project directory
cd /path/to/privateBlockchain

# Compile the project
mvn clean compile

# Package the application (creates JAR with dependencies)
mvn package
```

### 2. Run Basic Demo
```zsh
# Run the basic blockchain demo
mvn exec:java -Dexec.mainClass="demo.BlockchainDemo"
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

### 3. Run Off-Chain Storage Demo
```zsh
# Run demonstration of large data handling with off-chain storage
mvn test -Dtest=OffChainStorageTest
```

### 4. Run Advanced Features Demo
```zsh
# Run advanced features demonstration
mvn exec:java -Dexec.mainClass="demo.AdditionalAdvancedFunctionsDemo"
```

### 4. Run Chain Recovery Demo
```zsh
# Run blockchain chain recovery demonstration
mvn exec:java -Dexec.mainClass="demo.ChainRecoveryDemo"
```

### 5. Run Key Deletion Safety Features Demo
```zsh
# Run demonstration of key deletion safety features
mvn exec:java -Dexec.mainClass="demo.DangerousDeleteDemo"
```

### 6. Run Enhanced Recovery Example
```zsh
# Run example of advanced recovery techniques
mvn exec:java -Dexec.mainClass="demo.EnhancedRecoveryExample"
```

### 7. Quick Functionality Test
```zsh
# Run quick verification test
mvn exec:java -Dexec.mainClass="demo.QuickDemo"
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
// Small data (stored on-chain)
boolean success = blockchain.addBlock(
    "Your data here", 
    userKeys.getPrivate(), 
    userKeys.getPublic()
);

// Large data (automatically stored off-chain if >512KB)
boolean success = blockchain.addBlock(
    largeDocumentData, 
    userKeys.getPrivate(), 
    userKeys.getPublic()
);
```

### Step 4: Validate Chain

```java
// Get detailed validation information (recommended)
ChainValidationResult result = blockchain.validateChainDetailed();

// Check chain status
if (result.isStructurallyIntact()) {
    if (result.isFullyCompliant()) {
        System.out.println("✅ Chain is fully valid");
    } else {
        System.out.println("⚠️ Chain has authorization issues");
        System.out.println("Revoked blocks: " + result.getRevokedBlocks());
    }
} else {
    System.out.println("❌ Chain has structural problems");
    System.out.println("Invalid blocks: " + result.getInvalidBlocks());
}

// Get detailed validation report
String report = result.getDetailedReport();
System.out.println(report);

// Legacy validation (deprecated)
// boolean isValid = blockchain.validateChain(); // Avoid using this - will be removed in future versions
```

## 📁 Off-Chain Storage Feature

### How Off-Chain Storage Works

The blockchain automatically handles large data through a sophisticated off-chain storage system:

#### Automatic Data Management
```java
// Small data (< 512KB) - stored directly in blockchain
blockchain.addBlock("Small transaction data", privateKey, publicKey);
// Block contains: "Small transaction data"

// Large data (> 512KB) - automatically stored off-chain
blockchain.addBlock(largeFileContent, privateKey, publicKey);
// Block contains: "OFF_CHAIN_REF:a1b2c3d4..." (reference)
// File stored: off-chain-data/offchain_123456789_1234.dat (encrypted)
```

#### Data Retrieval
```java
// Get complete data (automatically handles on-chain/off-chain)
String fullData = blockchain.getCompleteBlockData(block);

// Check if block has off-chain data
if (block.hasOffChainData()) {
    System.out.println("Block uses off-chain storage");
}

// Verify off-chain data integrity
boolean isValid = blockchain.verifyOffChainIntegrity(block);
```

#### Configuration Management
```java
// View current configuration
System.out.println(blockchain.getConfigurationSummary());

// Adjust thresholds
blockchain.setOffChainThresholdBytes(1024 * 1024); // 1MB threshold
blockchain.setMaxBlockSizeBytes(5 * 1024 * 1024);  // 5MB on-chain max

// Reset to defaults
blockchain.resetLimitsToDefault();
```

### Security Features
- **AES-128-CBC Encryption**: All off-chain files are encrypted with unique IV
- **SHA3-256 Integrity**: Each file has cryptographic hash verification
- **ECDSA Signatures**: Digital signatures ensure authenticity
- **Deterministic Keys**: Encryption passwords derived from block metadata

### Storage Limits
- **On-Chain**: Up to 1MB per block (configurable)
- **Off-Chain**: Up to 100MB per file
- **Threshold**: 512KB default (configurable)
- **Total Capacity**: Limited only by available disk space

## 🧪 Basic Testing

### Comprehensive Test Suite

The project includes extensive testing with **more than 40 JUnit 5 tests** plus integration demos:

#### Run All Tests (Recommended)
```zsh
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

## 🔄 Thread-Safety Testing

### **NEW! Concurrent Operations Testing**
The blockchain now supports **complete thread-safety** for multi-threaded environments.

#### Run Thread-Safety Test (ZSH/Bash Compatible)
```bash
./run_thread_safety_test.sh
```

**✨ Script Features:**
- 🐚 **Shell Compatible**: Works with both Bash and ZSH
- 🔄 **Auto-cleanup**: Removes previous test databases  
- 🔨 **Auto-compile**: Compiles project before testing
- 📊 **Live monitoring**: Shows concurrent operations in real-time

**Expected Output:**
```
🧪 Private Blockchain - Thread-Safety Test
===========================================
🔨 Compiling project...
✅ Compilation successful!

🚀 Running Thread-Safety Test...
================================
🧪 Starting Thread-Safety Test for Blockchain
Threads: 10, Blocks per thread: 5
✅ Authorized key added successfully

🧪 Testing concurrent block addition...
✅ Thread 0 added block 0
✅ Thread 1 added block 0
... (50 blocks total)

📊 Concurrent block addition results:
   - Successful blocks: 50
   - Failed blocks: 0
   - Expected blocks: 50

🔍 Final chain validation: ✅ SUCCESS
📊 Final blockchain stats:
   - Total blocks: 51
   - Expected blocks: 51
🎉 Thread-safety test PASSED!
```

#### What Gets Tested
- ✅ **Concurrent block addition** (10 threads adding 5 blocks each)
- ✅ **Race condition prevention** (no duplicate block numbers)
- ✅ **Concurrent key operations** (add/revoke simultaneous)
- ✅ **Consistent reads** during writes
- ✅ **Database integrity** under high load
- ✅ **Transaction isolation** verification

#### Thread-Safety Features
- 🔒 **Global synchronization** across multiple Blockchain instances
- 🔄 **Pessimistic locking** for critical database operations
- ⚡ **Read-Write locks** for optimal concurrent read performance
- 🛡️ **ACID transactions** with automatic rollback on failures
- 📊 **Consistent timestamps** preventing temporal anomalies

#### Performance Characteristics
- **High-Read Workloads**: Multiple threads can read simultaneously
- **Write Operations**: Serialized with exclusive locks for safety
- **Database Optimizations**: Connection pooling and WAL mode enabled
- **Memory Efficiency**: ThreadLocal EntityManager management

---

```

#### Individual Test Categories
```zsh
# Advanced functions only (JUnit 5 tests)
./run_advanced_tests.sh

# Basic core functions only
./run_basic_tests.sh

# Core functions comprehensive test
mvn exec:java -Dexec.mainClass="demo.CoreFunctionsDemo"
```

### What Gets Tested
- ✅ **Genesis block creation** and initialization
- ✅ **Authorized key management** (add/revoke/list)
- ✅ **Block addition** with proper authorization
- ✅ **Chain validation** and integrity checking
- ✅ **Advanced features**: Export/Import, Search, Rollback
- ✅ **Error handling** and edge cases
- ✅ **Performance** and size validation
- ✅ **Off-chain data integrity** and validation
- ✅ **Detailed validation output** with comprehensive analysis

## 🚀 Running Enhanced Demo Applications

### Core Functionality Demos

#### Basic Blockchain Demo with Enhanced Validation
```bash
mvn exec:java -Dexec.mainClass="demo.BlockchainDemo"
```

#### Complete Off-Chain Validation Demo (NEW)
```bash
mvn exec:java -Dexec.mainClass="demo.TestDetailedValidation"
```

#### Core Functions with Enhanced Analysis
```bash
mvn exec:java -Dexec.mainClass="demo.CoreFunctionsDemo"
```

### Off-Chain Storage Demos

#### Comprehensive Off-Chain Validation Tests
```bash
mvn exec:java -Dexec.mainClass="demo.TestOffChainValidation"
```

#### Data Consistency Validation
```bash
mvn exec:java -Dexec.mainClass="demo.TestDataConsistency"
```

#### Export/Import with Validation Analysis
```bash
mvn exec:java -Dexec.mainClass="demo.TestExportImport"
```

### Thread Safety Demos with Enhanced Validation

#### Simple Thread Safety Test
```bash
mvn exec:java -Dexec.mainClass="demo.SimpleThreadSafetyTest"
```

#### Comprehensive Thread Safety Analysis
```bash
mvn exec:java -Dexec.mainClass="demo.ComprehensiveThreadSafetyTest"
```

### Recovery & Management Demos

#### Chain Recovery with Validation
```bash
mvn exec:java -Dexec.mainClass="demo.ChainRecoveryDemo"
```

### Expected Enhanced Output

All demos now show detailed validation information:

```
=== 📊 ENHANCED FINAL VALIDATION WITH OFF-CHAIN ANALYSIS ===
🔍 [main] Detailed validation of block #1
✅ [main] Block #1 is fully valid
🔍 [main] Detailed validation of block #3
✅ [main] Off-chain data fully validated for block #3
   📁 File: offchain_1751131802520_69.dat
   📦 Size: 2832.0 KB
   🔐 Integrity: verified (hash + encryption + signature)
   ⏰ Created: 2025-06-28T19:30:02.616
   🔗 Hash: e1c03c4a...92a31581
✅ [main] Block #3 is fully valid
📊 Chain validation completed: ✅ Chain is fully valid (6 blocks: 6 valid)
🗂️ Off-chain data summary:
   📊 Blocks with off-chain data: 2/6 (33.3%)
   ✅ Valid off-chain blocks: 2/2 (100.0%)
   📦 Total off-chain storage: 6.08 MB
```

### Testing Enhanced Validation

#### Run All Tests with Enhanced Output
```bash
mvn test
```

#### Run Specific Enhanced Test Categories
```bash
# Off-chain storage tests with detailed validation
mvn test -Dtest=OffChainStorageTest

# Data consistency tests with validation analysis  
mvn test -Dtest=DataConsistencyValidationTest

# Thread safety tests with enhanced output
mvn test -Dtest=DataIntegrityThreadSafetyTest
```

## 🎯 Demo Classes & Tests with Enhanced Off-Chain Validation

### 📺 Demo Applications (All Enhanced with Detailed Validation)

#### Core Functionality Demos
- **`BlockchainDemo.java`** - Basic blockchain operations with enhanced validation output
- **`CoreFunctionsDemo.java`** - Comprehensive core functionality demonstration
- **`TestDetailedValidation.java`** - ✨ **NEW**: Complete off-chain validation demonstration

#### Off-Chain Storage Demos
- **`TestOffChainValidation.java`** - ✨ **ENHANCED**: Comprehensive off-chain data validation tests
- **`TestDataConsistency.java`** - ✨ **ENHANCED**: Data consistency validation with detailed output
- **`TestExportImport.java`** - ✨ **ENHANCED**: Export/import operations with validation analysis

#### Thread Safety Demos
- **`SimpleThreadSafetyTest.java`** - ✨ **ENHANCED**: Basic thread safety with detailed validation
- **`ComprehensiveThreadSafetyTest.java`** - ✨ **ENHANCED**: Advanced thread safety with off-chain analysis

#### Recovery & Management Demos
- **`ChainRecoveryDemo.java`** - Chain recovery operations with validation
- **`DangerousDeleteDemo.java`** - Key deletion safety features
- **`EnhancedRecoveryExample.java`** - Advanced recovery techniques

### 🧪 Test Suites (All Enhanced with Detailed Validation)

#### Core Tests
- **`BlockchainTest.java`** - ✨ **ENHANCED**: Core blockchain functionality tests
- **`OffChainStorageTest.java`** - ✨ **ENHANCED**: Off-chain storage comprehensive testing
- **`DataConsistencyValidationTest.java`** - ✨ **ENHANCED**: Data consistency validation tests

#### Advanced Thread Safety Tests
- **`DataIntegrityThreadSafetyTest.java`** - ✨ **ENHANCED**: Data integrity under concurrent access
- **`ComprehensiveThreadSafetyTest.java`** - ✨ **ENHANCED**: Comprehensive concurrent operations
- **`ThreadSafetyTest.java`** - Basic thread safety validation
- **`AdvancedThreadSafetyTest.java`** - Advanced thread safety scenarios
- **`ExtremeThreadSafetyTest.java`** - Extreme load testing
- **`EdgeCaseThreadSafetyTest.java`** - Edge case validation

#### Validation & Recovery Tests
- **`CriticalConsistencyTest.java`** - Critical consistency validation
- **`ChainRecoveryManagerTest.java`** - Recovery manager testing
- **`ImprovedRollbackStrategyTest.java`** - Rollback strategy validation

### 🔍 Enhanced Validation Features

All demo classes and tests now include **detailed validation output** showing:

#### For Off-Chain Data Blocks:
- **📁 File name** and location
- **📦 File size** in KB/MB  
- **🔐 Integrity status** (hash + encryption + signature verification)
- **⏰ Creation timestamp** 
- **🔗 Truncated hash** for identification
- **⚠️ Specific error details** when validation fails

#### Blockchain Summary:
- **📊 Percentage of blocks** with off-chain data
- **✅ Validation success rate** for off-chain blocks
- **📦 Total storage size** of off-chain data
- **🚨 Alert notifications** for integrity issues

## 📊 Project Structure

```
src/
├── main/java/
│   ├── demo/                                     # ✨ ALL ENHANCED WITH DETAILED VALIDATION
│   │   ├── BlockchainDemo.java                      # Basic demo with enhanced validation
│   │   ├── CoreFunctionsDemo.java                   # Comprehensive core functionality
│   │   ├── TestDetailedValidation.java              # NEW: Complete validation demo
│   │   ├── TestOffChainValidation.java              # ENHANCED: Off-chain validation tests
│   │   ├── TestDataConsistency.java                 # ENHANCED: Data consistency validation
│   │   ├── TestExportImport.java                    # ENHANCED: Export/import validation
│   │   ├── SimpleThreadSafetyTest.java              # ENHANCED: Thread safety with validation
│   │   ├── ComprehensiveThreadSafetyTest.java       # ENHANCED: Advanced thread safety
│   │   ├── ChainRecoveryDemo.java                   # Chain recovery with validation
│   │   ├── DangerousDeleteDemo.java                 # Key deletion safety features
│   │   └── EnhancedRecoveryExample.java             # Advanced recovery techniques
│   └── com/rbatllet/blockchain/
├── core/
│   └── Blockchain.java                           # Main blockchain logic
├── dao/
│   ├── BlockDAO.java                            # Database operations for blocks with thread-safe block number generation
│   └── AuthorizedKeyDAO.java                    # Database operations for keys
├── entity/
│   ├── Block.java                               # Block data model
│   ├── AuthorizedKey.java                       # Authorized key data model
│   └── BlockSequence.java                       # Atomic block numbering entity
├── recovery/
│   ├── ChainRecoveryManager.java               # Handles blockchain recovery operations
│   └── RecoveryConfig.java                      # Configuration for recovery processes
├── security/
│   ├── ECKeyDerivation.java                    # Elliptic Curve key derivation utilities
│   ├── KeyFileLoader.java                       # Secure key file loading
│   ├── PasswordUtil.java                        # Password hashing and verification
│   └── SecureKeyStorage.java                    # Secure storage for cryptographic keys
├── util/
│   ├── CryptoUtil.java                          # Cryptographic utilities
│   ├── ExitUtil.java                            # Exit handling utilities
│   ├── JPAUtil.java                             # JPA EntityManager management
│   ├── format/
│   │   └── FormatUtil.java                      # Formatting utilities
│   └── validation/
│       ├── BlockValidationResult.java           # Block validation result model
│       └── BlockValidationUtil.java             # Block validation utilities
└── validation/
    ├── BlockStatus.java                        # Block status enumeration
    ├── BlockValidationResult.java               # Block validation results
    └── ChainValidationResult.java               # Chain validation results

src/test/java/com/rbatllet/blockchain/
├── core/
│   ├── BlockchainTest.java                             # Core blockchain tests
│   ├── BlockchainAdditionalAdvancedFunctionsTest.java   # JUnit 5 test suite
│   ├── BlockchainKeyAuthorizationTest.java             # Key authorization tests
│   ├── CriticalConsistencyTest.java                    # Consistency validation tests
│   ├── SimpleTemporalValidationTest.java               # Temporal validation tests
│   └── TestEnvironmentValidator.java                   # Validates test environment
├── dao/
│   └── AuthorizedKeyDAOTest.java                     # Tests for key management
└── recovery/
    ├── ChainRecoveryManagerTest.java               # Tests for recovery scenarios
    ├── ImprovedRollbackStrategyTest.java              # Enhanced rollback strategy tests
    └── RecoveryConfigTest.java                        # Recovery configuration tests

Configuration & Scripts:
├── src/main/resources/META-INF/persistence.xml  # JPA configuration
├── src/main/resources/logging.properties      # Logging configuration
├── src/test/resources/test.properties         # Test configuration
├── clean-database.sh                            # Database cleanup utility
├── run_all_tests.sh                             # ✨ Complete test runner with enhanced validation
├── run_advanced_tests.sh                        # Advanced tests only
├── run_advanced_thread_safety_tests.sh          # Advanced thread safety tests
├── run_basic_tests.sh                           # Basic tests only
├── run_api_migration_demo.sh                    # ✨ ENHANCED: API migration demonstration
├── run_crypto_security_demo.sh                  # Cryptographic security demo
├── run_enhanced_dangerous_delete_demo.sh        # Enhanced key deletion demo
├── run_thread_safety_test.sh                    # Thread-safety testing
├── run_recovery_tests.sh                        # Recovery tests runner
├── run_improved_rollback_test.sh                # Improved rollback tests
├── run_security_analysis.sh                     # Security analysis tests
├── run_security_tests.sh                        # Security tests runner
├── run_eckeyderivation_tests.sh                 # Elliptic curve key derivation tests
├── test_race_condition_fix.sh                   # Race condition testing
├── test_thread_safety.zsh                       # ✨ ENHANCED: Comprehensive thread safety
├── test_thread_safety_with_logs.zsh             # Thread safety with detailed logging
├── test_data_consistency.zsh                    # ✨ ENHANCED: Data consistency validation
├── test_export_import.zsh                       # ✨ ENHANCED: Export/import functionality
├── test_validation.zsh                          # ✨ ENHANCED: Comprehensive validation
├── scripts/                                     # Script utilities directory
│   ├── shared-functions.sh                     # ✨ CORE: Common functions library
│   ├── run_template.sh                         # Template for new scripts
│   └── check-db-cleanup.sh                     # Database cleanup verification
└── pom.xml                                      # Maven configuration
```

## 🔧 Automation Scripts

The project includes comprehensive automation scripts for testing, validation, and demonstration purposes.

### 📜 Core Test Runners

#### **run_all_tests.sh** ✨ COMPREHENSIVE
Complete test suite execution with all categories
```bash
./run_all_tests.sh
```
**Features**:
- Executes JUnit 5 tests (Additional Advanced Functions, Temporal Validation, Key Authorization, Critical Consistency)
- Runs all demo applications with enhanced validation
- Security tests for key deletion and cryptographic features
- Comprehensive result tracking and reporting
- Automatic database cleanup between test suites

#### **run_api_migration_demo.sh** ✨ ENHANCED
Complete API migration benefits demonstration
```bash
./run_api_migration_demo.sh
```
**Features**:
- Demonstrates all 11 demos with enhanced validation API
- Shows old vs new API comparison with detailed output
- Complete migration status tracking
- Enhanced debugging capabilities showcase

### 📜 Specialized Testing Scripts

#### Thread Safety Validation
```bash
./test_thread_safety.zsh          # ✨ Comprehensive thread safety with analysis
./run_thread_safety_test.sh       # Basic thread safety validation
./run_advanced_thread_safety_tests.sh  # Advanced concurrent operations
```

#### Data Consistency & Validation
```bash
./test_data_consistency.zsh       # ✨ Data consistency with off-chain analysis
./test_export_import.zsh          # ✨ Export/import with validation
./test_validation.zsh             # ✨ Comprehensive validation testing
```

#### Security & Recovery
```bash
./run_security_analysis.sh        # Complete security analysis
./run_recovery_tests.sh           # Chain recovery and repair testing
./run_crypto_security_demo.sh     # Cryptographic security features
```

### 📜 Core Utility Scripts

#### **scripts/shared-functions.sh** ✨ CORE LIBRARY
Common utility functions for all scripts
```bash
source ./scripts/shared-functions.sh
```
**Key Functions**:
- `clean_database()` - Core database cleanup functionality
- `print_header()`, `print_info()`, `print_success()`, `print_warning()`, `print_error()` - Colored output
- `compile_project()` - Project compilation with error handling
- `init_test_environment()` - Test environment initialization
- `check_dependencies()` - Dependency validation

#### Database Management
```bash
./clean-database.sh               # Database cleanup and maintenance
./scripts/check-db-cleanup.sh     # Database cleanup verification
```

### 🚀 Quick Script Commands

**Run complete test suite:**
```bash
./run_all_tests.sh
```

**Test thread safety comprehensively:**
```bash
./test_thread_safety.zsh
```

**Validate data consistency:**
```bash
./test_data_consistency.zsh
```

**Demonstrate API migration benefits:**
```bash
./run_api_migration_demo.sh
```

**Complete security analysis:**
```bash
./run_security_analysis.sh
```

### 🎯 Script Categories

**Core Testing**: `run_all_tests.sh`, `run_basic_tests.sh`, `run_advanced_tests.sh`  
**Thread Safety**: `test_thread_safety.zsh`, `run_thread_safety_test.sh`, `run_advanced_thread_safety_tests.sh`  
**Data Consistency**: `test_data_consistency.zsh`, `test_export_import.zsh`, `test_validation.zsh`  
**Security & Recovery**: `run_security_tests.sh`, `run_recovery_tests.sh`, `run_security_analysis.sh`  
**Demonstrations**: `run_api_migration_demo.sh`, `run_crypto_security_demo.sh`, `run_enhanced_dangerous_delete_demo.sh`  
**Utilities**: `clean-database.sh`, `scripts/shared-functions.sh`, `scripts/check-db-cleanup.sh`

All scripts provide automatic database cleanup, environment management, and comprehensive result reporting. Enhanced scripts include detailed validation output with off-chain data analysis.

## 🔐 Security Module

The security module provides essential cryptographic operations and secure key management for the blockchain implementation.

### Key Components

1. **ECKeyDerivation**
   - Generates secure EC key pairs using Bouncy Castle provider
   - Thread-safe implementation with proper provider registration
   - Supports standard EC curves (secp256k1, prime256v1)
   - Example usage:
     ```java
     KeyPair keyPair = ECKeyDerivation.generateKeyPair();
     ```

2. **KeyFileLoader**
   - Loads cryptographic keys from various file formats (PEM, DER, raw Base64)
   - Supports both private and public keys
   - Handles different key encodings and formats
   - Example usage:
     ```java
     PrivateKey privateKey = KeyFileLoader.loadPrivateKey("private.pem");
     PublicKey publicKey = KeyFileLoader.loadPublicKey("public.pem");
     ```

3. **PasswordUtil**
   - Secure password input handling
   - Works in both console and IDE environments
   - Password strength validation
   - Example usage:
     ```java
     String password = PasswordUtil.readPassword("Enter your password: ");
     ```

4. **SecureKeyStorage**
   - Encrypted storage for private keys
   - Uses AES-256 encryption with PBKDF2 key derivation
   - Secure file handling with proper permissions
   - Example usage:
     ```java
     SecureKeyStorage.storeKey("mykey", privateKey, password);
     PrivateKey loadedKey = SecureKeyStorage.loadKey("mykey", password);
     ```

### Security Best Practices

- All cryptographic operations use strong algorithms and key lengths
- Sensitive data is zeroed out after use
- Thread-safe implementations where needed
- Secure defaults for all cryptographic parameters

## 🔄 Recovery Module

The recovery module provides robust mechanisms for handling blockchain corruption and recovery scenarios, particularly after key deletions or other disruptive operations.

### Key Components

1. **ChainRecoveryManager**
   - Handles blockchain corruption scenarios after key deletion
   - Implements multiple recovery strategies:
     - Reauthorization of blocks with new keys
     - Rollback to last valid state
     - Partial export of valid chain segments
   - Thread-safe implementation with proper synchronization
   - Example usage:
     ```java
     ChainRecoveryManager recoveryManager = new ChainRecoveryManager(blockchain);
     RecoveryResult result = recoveryManager.recoverChain("compromised_key_id");
     if (result.isSuccessful()) {
         System.out.println("Recovery completed: " + result.getRecoverySummary());
     }
     ```

2. **RecoveryConfig**
   - Configurable parameters for recovery operations
   - Strategy enablement flags (reauthorization, rollback, etc.)
   - Logging and audit configuration
   - Export and backup settings
   - Example usage:
     ```java
     RecoveryConfig config = new RecoveryConfig()
         .setRollbackEnabled(true)
         .setAutoRecoveryEnabled(true)
         .setMaxRecoveryAttempts(3)
         .setBackupDirectory("recovery_backups");
     ```

### Recovery Strategies

1. **Reauthorization**
   - Re-signs blocks with new authorized keys
   - Preserves blockchain history when possible
   - Configurable owner name suffix for recovered blocks

2. **Rollback**
   - Reverts to last known valid state
   - Creates backup of rolled-back blocks
   - Configurable rollback depth

3. **Partial Export**
   - Exports valid chain segments
   - Creates importable backups
   - Preserves as much data as possible

### Best Practices

- Always enable audit logging for recovery operations
- Set appropriate recovery timeouts
- Regularly test recovery procedures
- Maintain secure backups of authorized keys
- Monitor for recovery events in production

## 🛠️ Database Utilities and Script Management

### Automatic Database Cleanup
All test scripts now include automatic database cleanup to prevent SQLite corruption issues:

```zsh
# All scripts automatically clean corrupted database files
./run_all_tests.sh      # Includes automatic cleanup
./run_advanced_tests.sh # Includes automatic cleanup  
./run_basic_tests.sh    # Includes automatic cleanup
```

### ZSH Script Implementation
All scripts in this project use ZSH (Z Shell) instead of Bash for improved compatibility and features:

```zsh
# All scripts use the portable shebang format
#!/usr/bin/env zsh
```

**Key ZSH Features Used:**
- Improved function handling (no need for `export -f` as in Bash)
- Better error handling and debugging capabilities
- Enhanced portability across different Unix-like systems
- Consistent script behavior across environments
- Standardized error handling with colored output

**Error Handling Standard:**
- All scripts use a consistent error handling approach
- Centralized `error_exit()` function for fatal errors
- Standardized output functions with visual indicators
- See [ERROR_HANDLING_STANDARD.md](docs/ERROR_HANDLING_STANDARD.md) for details

> **Note:** Make sure ZSH is installed on your system to run these scripts. Most macOS systems have ZSH installed by default.

### Manual Database Cleanup
If you encounter database corruption issues:

```zsh
# Clean corrupted database files manually
./clean-database.sh

# Skip automatic cleanup (for debugging)
SKIP_DB_CLEANUP=true ./run_all_tests.sh
```

### Script Development

#### Creating New Test Scripts
Use the provided template for consistent script structure:

```zsh
# Copy template for new test script
cp scripts/run_template.sh run_my_new_test.sh

# Make executable and customize
chmod +x run_my_new_test.sh
# Edit the script to add your test logic
```

#### Verify Script Compliance
Check that all run_*.sh scripts include database cleanup:

```zsh
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

// 4. Validate chain with detailed information
ChainValidationResult result = blockchain.validateChainDetailed();
boolean isStructurallyIntact = result.isStructurallyIntact();
boolean isFullyCompliant = result.isFullyCompliant();
System.out.println("Blockchain is structurally intact: " + isStructurallyIntact);
System.out.println("Blockchain is fully compliant: " + isFullyCompliant);

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
            
            // Validate blockchain with detailed results
            ChainValidationResult result = blockchain.validateChainDetailed();
            boolean isStructurallyIntact = result.isStructurallyIntact();
            boolean isFullyCompliant = result.isFullyCompliant();
            
            System.out.println("Blockchain validation results:");
            System.out.println("- Structural integrity: " + (isStructurallyIntact ? "✅ Valid" : "❌ Compromised"));
            System.out.println("- Full compliance: " + (isFullyCompliant ? "✅ Compliant" : "❌ Non-compliant"));
            
            if (!isStructurallyIntact) {
                System.out.println("Invalid blocks detected: " + result.getInvalidBlocks());
            }
            if (!isFullyCompliant) {
                System.out.println("Revoked blocks detected: " + result.getRevokedBlocks());
            }
            
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
- **[SECURITY_CLASSES_GUIDE.md](docs/SECURITY_CLASSES_GUIDE.md)** - Guía de uso de las clases de seguridad (migradas desde CLI)
- **[UTILITY_CLASSES_GUIDE.md](docs/UTILITY_CLASSES_GUIDE.md)** - Guía de uso de las clases de utilidad (migradas desde CLI)

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

### Storage Configuration
- **On-Chain Block Data**: 10,000 characters maximum (configurable)
- **On-Chain Block Size**: 1MB (1,048,576 bytes) maximum (configurable)
- **Off-Chain Threshold**: 512KB default (configurable)
- **Off-Chain Maximum**: 100MB per file
- **Hash Length**: 64 characters (SHA3-256)

### Dynamic Configuration Methods
```java
// Adjust storage limits at runtime
blockchain.setMaxBlockSizeBytes(2 * 1024 * 1024);     // 2MB on-chain limit
blockchain.setOffChainThresholdBytes(1024 * 1024);    // 1MB off-chain threshold
blockchain.getConfigurationSummary();                 // Display current settings
blockchain.resetLimitsToDefault();                    // Reset to defaults
```

### Database
- **On-Chain Location**: `blockchain.db` in project root directory
- **Off-Chain Location**: `off-chain-data/` directory for large files
- **Type**: SQLite database with automatic table creation
- **JPA Provider**: Hibernate as JPA implementation
- **Configuration**: `persistence.xml` for JPA settings

### Security
- **Hash Algorithm**: SHA3-256 for block integrity
- **Signature Algorithm**: ECDSA with secp256r1 curve
- **Access Control**: Authorized public key validation
- **Off-Chain Encryption**: AES-128-CBC with unique IV per file
- **Key Derivation**: SHA3-256 based deterministic encryption passwords
- **Integrity Verification**: Dual verification with hash and digital signature for off-chain data

## 🚨 Important Notes

### Production Considerations
- **Key Management**: Store private keys securely
- **Database Security**: Consider encryption for sensitive data
- **Backup Strategy**: Regular database backups recommended + off-chain file backups
- **Access Control**: Implement proper user authentication
- **Off-Chain Storage**: Ensure adequate disk space and backup off-chain files
- **Data Recovery**: Plan for off-chain data recovery and integrity verification

### Current Limitations
- **Single Database**: Uses one SQLite file for on-chain data
- **Local Storage**: Off-chain files stored locally (not distributed)
- **No Network**: Designed for single-application use
- **No Consensus**: No multi-node consensus mechanism
- **Key Recovery**: No built-in key recovery system

### Performance Notes
- **On-Chain Performance**: Small blocks ensure fast blockchain operations
- **Off-Chain Performance**: Large files (up to 100MB) handled efficiently via streaming encryption
- **Search Operations**: Content search may be slow with many blocks
- **Database Size**: On-chain database stays small due to off-chain storage for large data
- **Disk Space**: Monitor off-chain directory growth for large data usage

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
```zsh
# Complete verification steps
cd /path/to/privateBlockchain
mvn clean compile test-compile
./run_all_tests.sh
mvn exec:java -Dexec.mainClass="demo.BlockchainDemo"
```

### Quick Troubleshooting
```zsh
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

## 🚀 Future Improvements

### Planned Features

1. **Enhanced Security**
   - Support for hardware security modules (HSM)
   - Multi-signature transactions
   - Advanced key rotation policies

2. **Performance Optimizations**
   - Sharding support for large blockchains
   - Parallel block validation
   - Optimized storage for high-throughput scenarios

3. **Extended Functionality**
   - Smart contract support
   - Cross-chain interoperability
   - Privacy-preserving transactions

### Known Issues

- [ ] Performance degradation with very large blockchains (>1M blocks)
- [ ] Limited support for concurrent modifications in high-load scenarios
- [ ] Database size growth management for long-running nodes

### Contribution Guidelines

We welcome contributions! Please see our [Contribution Guidelines](CONTRIBUTING.md) for details on how to contribute to this project.

---

**💡 Remember**: This blockchain includes **more than 150 comprehensive tests** covering everything from basic operations to critical consistency scenarios, ensuring enterprise-grade reliability for your applications.

### 🔄 Migration Guide

### From RSA/SHA-2 to ECDSA/SHA-3

#### Key Changes
- **Digital Signatures**: Migrated from RSA to ECDSA (SHA3-256withECDSA)
- **Hashing**: Upgraded from SHA-256 to SHA3-256
- **Key Management**: New hierarchical key management system

#### Required Code Changes

1. **Key Generation**
   ```java
   // Old (RSA)
   KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
   keyGen.initialize(2048);
   
   // New (ECDSA)
   KeyPair keyPair = CryptoUtil.generateKeyPair();
   ```

2. **Signature Verification**
   ```java
   // Old (deprecated)
   boolean isValid = blockchain.validateChain();
   
   // New (recommended)
   ChainValidationResult result = blockchain.validateChainDetailed();
   if (result.isStructurallyIntact() && result.isFullyCompliant()) {
       // Chain is valid
   }
   ```

For complete migration details, see the [Crypto Migration Guide](docs/Crypto_Migration_Guide.md).
