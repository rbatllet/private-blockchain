# Private Blockchain Implementation

A comprehensive **post-quantum secure** private blockchain implementation in Java with ML-DSA-87 signatures (NIST FIPS 204), advanced features, and extensive testing.

## 📋 Table of Contents

- [Overview](#-overview)
- [Key Features](#-key-features)  
- [Performance Optimizations](#-performance-optimizations-v200)
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

This is a **post-quantum secure private blockchain** for controlled environments where only authorized users can add blocks. Unlike public blockchains, there is no mining - blocks are added directly by authorized users through cryptographic authorization using **ML-DSA-87 post-quantum signatures** (NIST FIPS 204 standardized).

**Key Differences from Public Blockchains:**
- **Controlled Access**: Only pre-approved users can add blocks
- **No Mining**: Direct block creation with post-quantum digital signatures
- **Private Environment**: Designed for organizational/enterprise use
- **Full Control**: Complete control over participants and data
- **Quantum-Resistant**: Protected against future quantum computer attacks with ML-DSA-87 (256-bit security)

---

> **🔄 BREAKING CHANGE (v1.0.6+)**: Critical security and operational methods now throw **exceptions** instead of returning `false`. This fail-fast pattern ensures security violations cannot be silently ignored. Affected methods: `revokeAuthorizedKey()`, `deleteAuthorizedKey()`, `rollbackBlocks()`, `rollbackToBlock()`, `exportChain()`, `importChain()`. See [Exception-Based Error Handling Guide](docs/security/EXCEPTION_BASED_ERROR_HANDLING_V1_0_6.md) for migration details.

---

## 🎯 Key Features

### Core Blockchain Features
- **Genesis Block**: Created automatically when blockchain starts
- **Post-Quantum Cryptography**: SHA3-256 hashing with ML-DSA-87 digital signatures (NIST FIPS 204, 256-bit quantum-resistant)
- **Access Control**: Hierarchical key management (Root/Intermediate/Operational)
- **Chain Validation**: Detailed blockchain integrity checking with `validateChainDetailed()`
- **Immutable Records**: Cryptographically secured blocks with digital signatures

### Advanced Functions
- **Export/Import**: Backup and restore complete blockchain with temporal consistency
- **Indexing Coordination**: Prevent infinite loops and coordinate indexing operations - see [IndexingCoordinator examples](docs/monitoring/INDEXING_COORDINATOR_EXAMPLES.md)
- **Advanced Search System**: Multi-level search with TRUE exhaustive capabilities
  - **SearchSpecialistAPI**: Professional search API with password-based initialization
  - **Generic Search**: searchAll() function searches both encrypted and non-encrypted content
  - **Enhanced Password Management**: Secure password registry for encrypted content access
  - **Multi-format Keywords**: autoKeywords field supports multiple encrypted strings
  - **Fast Search**: Keywords-only search for optimal performance
  - **Data Search**: Keywords + block content with balanced performance
  - **TRUE Exhaustive Search**: Complete search across on-chain content AND off-chain files
  - **Thread-Safe Search**: Concurrent search operations with intelligent caching
  - **Category Search**: Filter blocks by content categories (MEDICAL, FINANCE, TECHNICAL, LEGAL)
  - **Auto Keywords**: Automatic extraction of dates, numbers, emails, codes, and universal elements
  - **Search Validation**: Intelligent minimum length requirements with exceptions for useful short terms
  - **Mixed Content Search**: Advanced search across encrypted/plain text, on-chain/off-chain content
  - **API Compatibility**: Compatible search results between searchAndDecryptByTerms and SearchSpecialistAPI
- **Rollback Operations**: Safe removal of recent blocks with genesis protection
- **Off-Chain Storage**: Automatic storage for large data (>512KB) with AES-GCM encryption
  - **Block Linking**: New methods for creating blocks linked to off-chain data
  - **File Storage**: storeDataWithOffChainFile() for file-based off-chain storage

### 🚀 Performance Optimizations (v2.0.0)
- **Batch Retrieval System**: Eliminates N+1 query problems with `blockchain.batchRetrieveBlocks()`
  - **90%+ Performance Improvement**: Metadata search operations now complete in <200ms vs previous 2000+ms
  - **Query Optimization**: Replaces hundreds of individual SELECT statements with single batch IN clause
  - **Thread-Safe Operations**: Full concurrent access with intelligent transaction management
  - **JPA Integration**: Uses TypedQuery optimization for maximum database efficiency
- **Scalable Architecture**: Handles large blockchain datasets without performance degradation
- **Test Suite Optimization**: All optimization tests now pass (previously 8 failing tests due to timeouts)
  - **Text Storage**: storeDataWithOffChainText() for text-based off-chain content
  - **Searchable Off-Chain**: storeSearchableDataWithOffChainFile() with keyword indexing
- **Data Size Management**: Intelligent data placement based on configurable size thresholds
- **Integrity Verification**: Cryptographic verification of off-chain data with hash and signature validation
- **Enhanced Integrity Reporting**: **NEW v2.0** - Thread-safe `OffChainIntegrityReport` with comprehensive validation, resource protection, and intelligent recommendations
- **Detailed Validation**: Enhanced `validateChainDetailed()` with comprehensive off-chain data analysis
- **Real-time Monitoring**: Live validation results with detailed file status, integrity checks, and storage metrics
- **Data Consistency**: Complete synchronization between database and file system operations
- **Automatic Cleanup**: Off-chain file cleanup during rollback, export/import, and maintenance operations

### 🔐 Enhanced Security Features

### ⚡ Post-Quantum Cryptography
- **ML-DSA-87 Signatures**: NIST FIPS 204 standardized lattice-based signatures (256-bit quantum-resistant)
- **SHA3-256 Hashing**: Modern cryptographic hash function (quantum-resistant)
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
- **FormatUtil**: Formatting of blockchain data for display - see [quality assessment](docs/reports/FORMATUTIL_QUALITY_ASSESSMENT.md) | [technical analysis](docs/reports/FORMAT_UTIL_ROBUSTNESS_ANALYSIS.md)

### 🔐 User-Friendly Encryption API

**NEW**: Comprehensive encryption and blockchain management API that simplifies complex operations:

- **🎯 Complete Interface**: Single API for all encryption, search, storage, and recovery operations
- **🔍 Advanced Search**: Multi-level search (fast/balanced/exhaustive) with encrypted content support
- **🔑 Smart Key Management**: Hierarchical key system with automatic rotation and secure storage
- **📊 Health Monitoring**: Real-time validation, integrity checks, and performance diagnostics
- **🛡️ Robust Integrity Reports**: Thread-safe reporting with overflow protection, input validation, and context-aware recommendations
- **🛡️ Security Features**: Password generation, term visibility control, and audit trails
- **💾 Storage Tiering**: Intelligent data placement with compression and optimization
- **🔧 Chain Recovery**: Automated corruption detection and recovery mechanisms
- **📈 Analytics**: Comprehensive reporting and metrics for blockchain operations
- **🔧 Metadata Management**: Dynamic block metadata updates without modifying encrypted content - see [Metadata Management Guide](docs/data-management/METADATA_MANAGEMENT_GUIDE.md)

### Technical Features
- **Persistence**: Database-agnostic architecture with JPA standard for ORM (using Hibernate as provider)
  - **Supported Databases**: SQLite (development), PostgreSQL (production), MySQL (production), H2 (testing)
  - **Zero-Code Switching**: Switch databases via configuration without code changes
- **Off-Chain Storage**: Encrypted file storage with automatic data tiering (AES-256-GCM)
- **Professional Logging**: SLF4J with Logback - configurable performance modes (dev/production/test)
- **Comprehensive Testing**: **828+ JUnit 5 tests** with **72% code coverage** + integration demos + security tests
- **Production Ready**: Complete documentation and deployment guides with performance optimization
- **Clean Architecture**: Well-structured code with DAO pattern and enterprise logging
- **Scalable Storage**: Support for data up to 100MB per block through off-chain storage

## 🛡️ OffChainIntegrityReport v2.0 - Enhanced Robustness

**NEW in v2.0**: The `OffChainIntegrityReport` class has been completely rewritten for enterprise-grade reliability and performance:

### 🚀 Key Improvements
- **🧵 Thread Safety**: Full concurrent access support with `ReentrantReadWriteLock` and atomic operations
- **✅ Input Validation**: Comprehensive parameter validation with descriptive error messages and security limits
- **🛡️ Resource Protection**: Memory limits, overflow protection, and DoS attack prevention
- **🤖 Smart Recommendations**: Context-aware AI-driven suggestions based on data health and performance
- **📊 Enhanced Statistics**: Thread-safe counters with overflow detection and performance metrics
- **🔍 Structured Logging**: Professional SLF4J logging with appropriate levels for monitoring

### 🎯 Production Features
- **Memory Safety**: Collection size limits (100K results), string length validation, metadata limits
- **Error Handling**: Graceful degradation, exception chaining, and comprehensive error recovery
- **Performance**: Read-optimized data structures, lazy loading, efficient concurrent operations  
- **Security**: Input sanitization, resource limits, thread-safe operations, defensive copying

### 📋 Usage Example
```java
// Thread-safe creation with validation
OffChainIntegrityReport report = new OffChainIntegrityReport("REPORT_ID");

// Add validated check results
IntegrityCheckResult result = new IntegrityCheckResult(
    "data_001", "HASH_VERIFICATION", IntegrityStatus.HEALTHY,
    "Verification successful", Duration.ofMillis(100)
).addMetadata("bytesChecked", 2048L);

report.addCheckResult(result); // Thread-safe
report.generateRecommendations(); // AI-driven suggestions
System.out.println(report.getFormattedSummary()); // Rich output
```

**📚 Documentation**:
- **Quick Start**: [OFFCHAIN_INTEGRITY_REPORT_QUICK_START.md](docs/recovery/OFFCHAIN_INTEGRITY_REPORT_QUICK_START.md)
- **Complete Guide**: [OFFCHAIN_INTEGRITY_REPORT_ROBUSTNESS_IMPROVEMENTS.md](docs/reports/OFFCHAIN_INTEGRITY_REPORT_ROBUSTNESS_IMPROVEMENTS.md)
- **Test Coverage**: 10 comprehensive tests covering thread safety, validation, and robustness

## 🛠️ Technologies Used

- **Java 25** - Programming language with modern features
- **Maven** - Build and dependency management
- **Database-Agnostic Architecture** - 100% portable across multiple databases:
  - **SQLite** - Development and demos (single-writer, 2-5 connections)
  - **PostgreSQL** - Production environments (multi-writer, 10-60 connections)
  - **MySQL** - Production environments (multi-writer, 10-60 connections)
  - **H2** - Testing (in-memory, fast isolation)
- **JPA** - Java Persistence API with Hibernate as implementation provider (100% JPQL, zero native SQL)
- **Cryptography**:
  - **Hashing**: SHA3-256 (quantum-resistant hash function)
  - **Digital Signatures**: ML-DSA-87 (NIST FIPS 204, 256-bit quantum-resistant lattice-based)
  - **Key Management**: Hierarchical key structure with automatic rotation
  - **Encryption**: AES-256-GCM for off-chain data encryption with authenticated encryption
- **JUnit 5** - Testing framework for comprehensive validation

## 📦 Prerequisites

- **Java 25** or higher installed
- **Maven 3.6** or higher
- At least **500MB** of free disk space (additional space needed for off-chain storage)

### Checking Prerequisites
```zsh
# Check Java version (must be 25+)
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
# Run the basic blockchain demo (using provided script)
scripts/run_simple_demo.zsh

# Or run directly with Maven
mvn exec:java -Dexec.mainClass="demo.SimpleDemo"
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

### 8. Database-Agnostic Configuration Demo
```zsh
# Run database configuration demo (SQLite, PostgreSQL, MySQL, H2)
./scripts/run_database_config_demo.zsh
```

### 9. JPA Configuration Storage Demo
```zsh
# Run JPA configuration storage demo (database-agnostic)
./scripts/run_jpa_configuration_storage_demo.zsh
```

### 10. Secure Key Storage Demo
```zsh
# Run secure key storage demo with AES-256-GCM encryption
./scripts/run_secure_key_storage_demo.zsh
```

## 💻 How It Works

### Step 1: Initialize Blockchain
```java
Blockchain blockchain = new Blockchain();
// Creates genesis block automatically
```

> **🔑 PREREQUISITE**: Before Step 2, generate genesis-admin keys:
> ```bash
> ./tools/generate_genesis_keys.zsh
> ```
> This creates `./keys/genesis-admin.*` required for user management. **Backup these keys securely!**

### Step 2: Add Authorized Users (RBAC v1.0.6+)
```java
// Load bootstrap admin keys (generated via ./tools/generate_genesis_keys.zsh)
KeyPair bootstrapKeys = KeyFileLoader.loadKeyPairFromFiles(
    "./keys/genesis-admin.private",
    "./keys/genesis-admin.public"
);

// Register bootstrap admin in blockchain (REQUIRED!)
blockchain.createBootstrapAdmin(
    CryptoUtil.publicKeyToString(bootstrapKeys.getPublic()),
    "BOOTSTRAP_ADMIN"
);

// Create new user with USER role (requires bootstrap admin credentials)
KeyPair userKeys = CryptoUtil.generateKeyPair();
String publicKey = CryptoUtil.publicKeyToString(userKeys.getPublic());
blockchain.addAuthorizedKey(
    publicKey,
    "UserName",
    bootstrapKeys,      // Caller credentials (bootstrap admin)
    UserRole.USER       // Target role
);
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

```

## 🔐 User-Friendly Encryption API

The UserFriendlyEncryptionAPI provides a comprehensive, simplified interface for all blockchain operations with built-in encryption, search, and security features.

### 📋 Quick Start with UserFriendlyEncryptionAPI

```java
import com.rbatllet.blockchain.service.UserFriendlyEncryptionAPI;
import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.security.KeyFileLoader;

// 1. Create blockchain (creates genesis block automatically)
Blockchain blockchain = new Blockchain();

// 2. Load genesis admin keys (generate first via ./tools/generate_genesis_keys.zsh)
KeyPair genesisKeys = KeyFileLoader.loadKeyPairFromFiles(
    "./keys/genesis-admin.private",
    "./keys/genesis-admin.public"
);

// 3. Register bootstrap admin in blockchain (REQUIRED!)
blockchain.createBootstrapAdmin(
    CryptoUtil.publicKeyToString(genesisKeys.getPublic()),
    "BOOTSTRAP_ADMIN"
);

// 4. Create API with bootstrap admin credentials
UserFriendlyEncryptionAPI api = new UserFriendlyEncryptionAPI(blockchain);
api.setDefaultCredentials("BOOTSTRAP_ADMIN", genesisKeys);

// 5. Create regular user (requires authorized caller - bootstrap admin in this case)
KeyPair aliceKeys = api.createUser("alice");
api.setDefaultCredentials("alice", aliceKeys);

// 6. Store encrypted data with automatic keyword extraction
Block block = api.storeEncryptedData("Medical record: Patient exhibits normal symptoms", "secure123");

// Search encrypted content (password required)
List<Block> results = api.smartSearchEncryptedData("medical", "secure123", 10);

// Advanced search with multiple criteria
Map<String, Object> criteria = new HashMap<>();
criteria.put("terms", Arrays.asList("patient", "symptoms"));
criteria.put("includeEncrypted", true);
AdvancedSearchResult result = api.performAdvancedSearch(criteria, "secure123", 20);
```

### 🎯 Core Features

#### Intelligent Data Storage
```java
// Store data with searchable terms and categories
String[] searchTerms = {"medical", "cardiology", "2024"};
Block block = api.storeSearchableData(medicalData, password, searchTerms);

// Store with layered search terms (public/private separation)
String[] publicTerms = {"medical", "cardiology"};
String[] privateTerms = {"patient-001", "diagnosis"};
Block block = api.storeSearchableDataWithLayers(data, password, publicTerms, privateTerms);

// Smart storage with automatic compression and tiering
Block block = api.storeWithSmartTiering(largeDocument, password, metadata);
```

#### Advanced Search Capabilities
```java
// Multi-level search (FAST/DATA/EXHAUSTIVE)
List<Block> fastResults = api.searchByTerms(new String[]{"medical"}, null, 10);
List<Block> deepResults = api.searchWithAdaptiveDecryption("patient", password, 10);

// Advanced search with multiple criteria
Map<String, Object> criteria = new HashMap<>();
criteria.put("keywords", "blockchain security");
criteria.put("includeEncrypted", true);
AdvancedSearchResult results = api.performAdvancedSearch(criteria, password, 50);

// Time-based search with filtering
LocalDateTime start = LocalDateTime.now().minusDays(30);
LocalDateTime end = LocalDateTime.now();
AdvancedSearchResult timeResults = api.performTimeRangeSearch(start, end, filters);
```

#### Security & Key Management
```java
// Hierarchical key setup with automatic rotation
KeyManagementResult result = api.setupHierarchicalKeys("masterPassword123!");

// Generate secure passwords with validation
String password = api.generateValidatedPassword(16, true);

// Import and manage user credentials (v1.0.6+: requires authorized caller)
boolean imported = api.importAndSetDefaultUser("alice", "/path/to/key.pem");
List<CryptoUtil.KeyInfo> keys = api.listManagedKeys();
```

> **⚠️ Security Note (v1.0.6+):** Methods like `createUser()`, `importAndSetDefaultUser()`, and `loadUserCredentials()` require the caller to be pre-authorized. See [Pre-Authorization Guide](docs/security/PRE_AUTHORIZATION_GUIDE.md) for the mandatory secure initialization pattern.

#### Health Monitoring & Analytics
```java
// Comprehensive blockchain validation
ValidationReport report = api.performComprehensiveValidation();
HealthReport health = api.performHealthDiagnosis();

// Performance metrics and optimization
String analytics = api.getStorageAnalytics();
SearchMetrics metrics = api.getSearchMetrics();
String optimization = api.optimizeSearchPerformance();
```

#### Chain Recovery & Maintenance
```java
// Automated recovery from corruption
Map<String, Object> options = new HashMap<>();
options.put("autoRepair", true);
ChainRecoveryResult recovery = api.recoverFromCorruption(options);

// Safe rollback with data preservation
ChainRecoveryResult rollback = api.rollbackToSafeState(targetBlock, options);

// Enhanced integrity verification and repair (v2.0)
OffChainIntegrityReport integrity = api.verifyOffChainIntegrity(blockNumbers);
integrity.generateRecommendations(); // AI-driven recommendations
System.out.println(integrity.getFormattedSummary()); // Rich formatted output
```

### 📊 Testing & Quality Assurance

The UserFriendlyEncryptionAPI includes comprehensive testing with:

- **828+ JUnit 5 Tests** across multiple test classes
- **72% Code Coverage** with focus on critical functionality
- **Nested Test Organization** for improved maintainability
- **Concurrent Testing** for thread-safety validation
- **Security Testing** for encryption and key management
- **Integration Testing** for real-world scenarios

#### Test Classes Coverage
- `UserFriendlyEncryptionAPIPhase1Test` - Core functionality
- `UserFriendlyEncryptionAPIPhase2SearchTest` - Search capabilities  
- `UserFriendlyEncryptionAPIPhase3Test` - Storage and security
- `UserFriendlyEncryptionAPIPhase4Test` - Recovery and analytics
- `UserFriendlyEncryptionAPISecurityTest` - Security features
- `UserFriendlyEncryptionAPIZeroCoverageTest` - Edge cases
- `UserFriendlyEncryptionAPIRemainingCoverageTest` - Complete coverage

### 🔧 Configuration & Integration

```java
// Custom encryption configuration
EncryptionConfig.Builder builder = api.createCustomConfig();
EncryptionConfig config = builder
    .keyLength(256)
    .minPasswordLength(12)
    .metadataEncryptionEnabled(true)
    .build();

// Performance optimization
api.optimizeSearchCache();
StorageTieringManager.TieringReport tieringReport = api.optimizeStorageTiers();

// Export search results in multiple formats
String jsonExport = api.exportSearchResults(searchResult, "json");
String csvExport = api.exportSearchResults(searchResult, "csv");
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
- **AES-256-GCM Encryption**: All off-chain files are encrypted with authenticated encryption
- **SHA3-256 Integrity**: Each file has cryptographic hash verification
- **ML-DSA-87 Signatures**: Post-quantum digital signatures ensure authenticity (NIST FIPS 204, 256-bit quantum-resistant)
- **Deterministic Keys**: Encryption passwords derived from block metadata

### Storage Limits
- **On-Chain**: Up to 1MB per block (configurable)
- **Off-Chain**: Up to 100MB per file
- **Threshold**: 512KB default (configurable)
- **Total Capacity**: Limited only by available disk space

## 🧪 Basic Testing

### Comprehensive Test Suite

The project includes extensive testing with **828+ JUnit 5 tests** achieving **72% code coverage** plus integration demos and security testing:

#### Run All Tests (Recommended)
```zsh
./run_all_tests.zsh
```

**Expected Output:**
```
=== COMPREHENSIVE BLOCKCHAIN TEST RUNNER ===
✅ Compilation successful!
🎉 JUnit 5 tests: PASSED (828+ tests, 72% coverage)
✅ UserFriendlyEncryptionAPI tests: PASSED
✅ Security and encryption tests: PASSED
✅ Basic Core Functions test: PASSED
✅ Blockchain Demo: PASSED
✅ Simple Test: PASSED
✅ Quick Test: PASSED

📊 Test suites passed: 7/7
🎉 ALL TESTS PASSED SUCCESSFULLY!
```

#### Test Categories Included

- **Core API Tests**: UserFriendlyEncryptionAPI functionality with 7+ test classes
- **Security Tests**: Encryption, key management, and authentication
- **Search Tests**: Advanced search, semantic search, and caching
- **Storage Tests**: Off-chain storage, compression, and tiering
- **Recovery Tests**: Chain recovery, corruption detection, and repair
- **Concurrency Tests**: Thread-safety and concurrent operations
- **Integration Tests**: End-to-end functionality validation

#### Run Specific UserFriendlyEncryptionAPI Tests
```zsh
# Run all UserFriendlyEncryptionAPI tests
mvn test -Dtest="*UserFriendlyEncryptionAPI*Test"

# Run specific test phases
mvn test -Dtest=UserFriendlyEncryptionAPIPhase1Test          # Core functionality
mvn test -Dtest=UserFriendlyEncryptionAPIPhase2SearchTest    # Search capabilities
mvn test -Dtest=UserFriendlyEncryptionAPISecurityTest        # Security features
mvn test -Dtest=UserFriendlyEncryptionAPIZeroCoverageTest    # Edge cases
mvn test -Dtest=UserFriendlyEncryptionAPIRemainingCoverageTest # Full coverage

# Run with coverage report
```

## 🎬 Demo Applications

### Memory Safety Demonstrations (v1.0.6)

Two comprehensive interactive demos showcase the memory safety improvements from Phases A and B:

#### 1. Streaming APIs Demo (Phase B.2)

Demonstrates the 4 new memory-safe streaming methods:

```bash
./scripts/run_streaming_apis_demo.zsh
```

**Features Demonstrated:**
- ✅ `streamBlocksByTimeRange()` - Temporal queries with time filtering
- ✅ `streamEncryptedBlocks()` - Encryption audits and key rotation operations
- ✅ `streamBlocksWithOffChainData()` - Off-chain storage management
- ✅ `streamBlocksAfter()` - Incremental processing for large rollbacks
- ✅ Memory safety verification (constant ~50MB usage with any blockchain size)

**Sample Output:**
```
═══════════════════════════════════════════════════════════════
  STREAMING APIS DEMO - PHASE B.2
═══════════════════════════════════════════════════════════════

  🎯 DEMO: streamBlocksByTimeRange()
     - Blocks found: 25
     - Time range: 2024-01-01 to 2024-12-31
     - Execution time: 15ms
     - Memory-safe: Constant ~50MB usage

  🎯 DEMO: streamEncryptedBlocks()
     - Encrypted blocks found: 10
     - Audit passed: 10 ✅
     - Execution time: 12ms

  🎯 DEMO: streamBlocksWithOffChainData()
     - Off-chain blocks found: 10
     - Total off-chain data: 6.00 MB
     - Execution time: 18ms

  🎯 DEMO: streamBlocksAfter()
     - Blocks processed: 15
     - Block range: #36 to #50
     - Execution time: 10ms

✅ All streaming API demos completed successfully!
```

#### 2. Memory Safety Demo (Phase A)

Demonstrates critical memory safety improvements:

```bash
./scripts/run_memory_safety_demo.zsh
```

**Features Demonstrated:**
- ✅ Breaking changes validation (`maxResults` parameter enforcement)
- ✅ Batch processing with `processChainInBatches()` (constant memory)
- ✅ Streaming validation with `validateChainStreaming()` (unlimited size)
- ✅ Memory-safe search methods with automatic limits
- ✅ Memory safety constants (`MemorySafetyConstants`)
- ✅ Before vs After comparison (66% memory reduction)

**Sample Output:**
```
═══════════════════════════════════════════════════════════════
  MEMORY SAFETY DEMO - PHASE A
═══════════════════════════════════════════════════════════════

  ⚠️  DEMO: BREAKING CHANGES VALIDATION
     Test 1: Attempting negative maxResults...
       ✅ Correctly rejected: maxResults must be positive

     Test 2: Attempting zero maxResults...
       ✅ Correctly rejected: maxResults must be positive

     Test 3: Using valid maxResults (1000)...
       ✅ Accepted: Retrieved 1000 blocks

  🎯 DEMO: BATCH PROCESSING
     - Total blocks processed: 1001
     - Encrypted blocks: 200
     - Plain blocks: 801
     - Processing time: 850ms
     - Memory delta: 12.5 MB ✅

  🎯 DEMO: STREAMING VALIDATION
     - Blockchain valid: ✅ YES
     - Total blocks validated: 1001
     - Validation time: 920ms
     - Perfect for chains > 500K blocks

  📈 BEFORE vs AFTER COMPARISON
     ❌ BEFORE: Memory used: 125 MB
     ✅ AFTER: Memory used: 42 MB
     📈 Improvement: Memory reduction: 66.4% ✅

✅ All memory safety demos completed successfully!
```

#### Demo Source Code

- **Phase B.2**: `src/main/java/demo/StreamingApisDemo.java`
- **Phase A**: `src/main/java/demo/MemorySafetyDemo.java`

Both demos:
- Create real blockchains (50-1000 blocks)
- Mix data types (encrypted, off-chain, plain text)
- Perform actual blockchain operations (no simulations)
- Include memory measurement and verification
- Provide detailed output with metrics and timings

**Related Documentation:**
- [API_GUIDE.md - Memory Safety & Streaming APIs](docs/reference/API_GUIDE.md#-memory-safety--streaming-apis-v105)
- [PERFORMANCE_BENCHMARK_REPORT.md](docs/reports/PERFORMANCE_BENCHMARK_REPORT.md)
- [MIGRATION_GUIDE_V1_0_5.md](docs/reference/MIGRATION_GUIDE_V1_0_5.md)

---

# Run with coverage report
mvn clean test jacoco:report
# View report: target/site/jacoco/index.html
```

#### Test Coverage Achievements
- **Overall Coverage**: 72% (Target reached: 75%+)
- **Total Tests**: 828+ JUnit 5 tests across all test classes
- **Test Classes**: 10+ dedicated test classes for comprehensive validation
- **Critical Methods**: 100% coverage for security-critical operations
- **Edge Cases**: Comprehensive testing of error conditions and boundary cases

## 🔄 Thread-Safety Testing

### **NEW! Concurrent Operations Testing**
The blockchain now supports **complete thread-safety** for multi-threaded environments.

#### Run Thread-Safety Test (ZSH/Bash Compatible)
```bash
./run_thread_safety_test.zsh
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
./run_advanced_tests.zsh

# Basic core functions only
./run_basic_tests.zsh

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

#### TRUE Exhaustive Search Demo (NEW) 🔍
```bash
# Interactive demo with comprehensive search examples
./scripts/run_exhaustive_search_demo.zsh

# Practical examples for developers with step-by-step tutorials
./scripts/run_exhaustive_search_examples.zsh

# Or run programmatically
mvn exec:java -Dexec.mainClass="demo.ExhaustiveSearchDemo"
mvn exec:java -Dexec.mainClass="demo.ExhaustiveSearchExamples"
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
mvn exec:exec@run-tests
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
- **`OffChainIntegrityReportTest.java`** - ✨ **NEW v2.0**: Thread-safe integrity reporting with 10 comprehensive tests
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
- **`BlockValidationUtilTest.java`** - ✨ **NEW**: Comprehensive BlockValidationUtil testing (26 tests)

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
privateBlockchain/
├── src/
│   ├── main/java/
│   │   ├── demo/                                 # Demo applications
│   │   └── com/rbatllet/blockchain/              # Core blockchain implementation
│   └── test/java/                                # Test suites
├── docs/                                         # Documentation
├── scripts/                                      # All executable scripts
│   ├── lib/                                      # Common ZSH functions library
│   │   ├── common_functions.zsh                  # Shared functions for all scripts
│   │   └── README.md                             # Library documentation
│   ├── run_*.zsh                                 # Demo scripts
│   ├── test_*.zsh                                # Test scripts
│   └── clean_*.zsh                               # Utility scripts
├── logs/                                         # Application logs (created at runtime)
├── off-chain-data/                              # Off-chain storage (created at runtime)
└── pom.xml                                      # Maven configuration
```

### Source Code Structure

```
src/main/java/com/rbatllet/blockchain/
├── core/
│   ├── Blockchain.java                           # Main blockchain logic (public API)
│   └── BlockRepository.java                     # Package-private repository for block persistence (internal use only)
├── dao/
│   └── AuthorizedKeyDAO.java                    # Database operations for keys
├── entity/
│   ├── Block.java                               # Block data model (Phase 5.0: manual assignment)
│   ├── AuthorizedKey.java                       # Authorized key data model
│   └── OffChainData.java                        # Off-chain data references
├── recovery/
│   ├── ChainRecoveryManager.java               # Handles blockchain recovery operations
│   └── RecoveryConfig.java                      # Configuration for recovery processes
├── security/
│   ├── KeyFileLoader.java                       # Secure key file loading (ML-DSA-87)
│   ├── PasswordUtil.java                        # Password hashing and verification
│   └── SecureKeyStorage.java                    # Secure storage for cryptographic keys (AES-256-GCM)
├── util/
│   ├── CryptoUtil.java                          # Cryptographic utilities
│   ├── ExitUtil.java                            # Exit handling utilities
│   ├── JPAUtil.java                             # JPA EntityManager management
│   ├── format/
│   │   └── FormatUtil.java                      # Formatting utilities
│   └── validation/
│       ├── BlockValidationResult.java           # Block validation result model
│       └── BlockValidationUtil.java             # Block validation utilities (with comprehensive tests)
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
│   ├── OffChainStorageTest.java                        # ✨ ENHANCED: Off-chain storage testing
│   ├── DataConsistencyValidationTest.java              # ✨ ENHANCED: Data consistency tests
│   └── TestEnvironmentValidator.java                   # Validates test environment
├── dao/
│   └── AuthorizedKeyDAOTest.java                     # Tests for key management
├── util/validation/
│   └── BlockValidationUtilTest.java                   # ✨ NEW: Comprehensive validation utility tests (26 tests)
├── validation/
│   └── BlockValidationResultTest.java                 # Block validation result tests
├── advanced/
│   └── DataIntegrityThreadSafetyTest.java             # ✨ ENHANCED: Thread safety tests
└── recovery/
    ├── ChainRecoveryManagerTest.java               # Tests for recovery scenarios
    ├── ImprovedRollbackStrategyTest.java              # Enhanced rollback strategy tests
    └── RecoveryConfigTest.java                        # Recovery configuration tests

Configuration & Scripts:
├── src/main/resources/META-INF/persistence.xml  # JPA configuration
├── src/main/resources/logging.properties      # Logging configuration
├── src/test/resources/test.properties         # Test configuration
├── clean-database.zsh                            # Database cleanup utility
├── run_all_tests.zsh                             # ✨ Complete test runner with enhanced validation
├── run_advanced_tests.zsh                        # Advanced tests only
├── run_advanced_thread_safety_tests.zsh          # Advanced thread safety tests
├── run_basic_tests.zsh                           # Basic tests only
├── run_api_migration_demo.zsh                    # ✨ ENHANCED: API migration demonstration
├── run_crypto_security_demo.zsh                  # Cryptographic security demo
├── run_enhanced_dangerous_delete_demo.zsh        # Enhanced key deletion demo
├── run_thread_safety_test.zsh                    # Thread-safety testing
├── run_recovery_tests.zsh                        # Recovery tests runner
├── run_improved_rollback_test.zsh                # Improved rollback tests
├── run_security_analysis.zsh                     # Security analysis tests
├── run_security_tests.zsh                        # Security tests runner (ML-DSA-87 post-quantum)
├── run_search_framework_demo.zsh               # ✨ NEW: Search framework system demonstration script
├── test_race_condition_fix.zsh                   # Race condition testing
├── test_thread_safety_full.zsh                  # ✨ ENHANCED: Comprehensive thread safety (production)
├── test_thread_safety_simple.zsh               # ✨ NEW: Simple thread safety with detailed logging (debug)
├── test_data_consistency.zsh                    # ✨ ENHANCED: Data consistency validation
├── test_export_import.zsh                       # ✨ ENHANCED: Export/import functionality
├── test_validation.zsh                          # ✨ ENHANCED: Comprehensive validation
├── scripts/                                     # Script utilities directory
│   ├── lib/common_functions.zsh                 # ✨ CORE: Common functions library
│   ├── run_template.zsh                         # Template for new scripts
│   └── check-db-cleanup.zsh                     # Database cleanup verification
└── pom.xml                                      # Maven configuration
```

## 🔧 Automation Scripts

The project includes comprehensive automation scripts for testing, validation, and demonstration purposes.

### 📜 Core Test Runners

#### **run_all_tests.zsh** ✨ COMPREHENSIVE
Complete test suite execution with all categories
```bash
./run_all_tests.zsh
```
**Features**:
- Executes JUnit 5 tests (Additional Advanced Functions, Temporal Validation, Key Authorization, Critical Consistency)
- Runs all demo applications with enhanced validation
- Security tests for key deletion and cryptographic features
- Comprehensive result tracking and reporting
- Automatic database cleanup between test suites

#### **run_api_migration_demo.zsh** ✨ ENHANCED
Complete API migration benefits demonstration
```bash
./run_api_migration_demo.zsh
```
**Features**:
- Demonstrates all 11 demos with enhanced validation API
- Shows old vs new API comparison with detailed output
- Complete migration status tracking
- Enhanced debugging capabilities showcase

### 📜 Specialized Testing Scripts

#### Thread Safety Validation
```bash
./test_thread_safety_full.zsh     # ✨ Comprehensive thread safety with analysis (production)
./test_thread_safety_simple.zsh   # ✨ Simple thread safety with detailed logging (debug)
./run_thread_safety_test.zsh       # Basic thread safety validation
./run_advanced_thread_safety_tests.zsh  # Advanced concurrent operations
```

#### Data Consistency & Validation
```bash
./test_data_consistency.zsh       # ✨ Data consistency with off-chain analysis
./test_export_import.zsh          # ✨ Export/import with validation
./test_validation.zsh             # ✨ Comprehensive validation testing
```

#### Security & Recovery
```bash
./run_security_analysis.zsh        # Complete security analysis
./run_recovery_tests.zsh           # Chain recovery and repair testing
./run_crypto_security_demo.zsh     # Cryptographic security features
```

### 📜 Core Utility Scripts

#### **scripts/lib/common_functions.zsh** ✨ CORE LIBRARY
Common utility functions for all scripts
```bash
source ./scripts/lib/common_functions.zsh
```
**Key Functions**:
- `cleanup_database()` - Core database cleanup functionality
- `print_header()`, `print_info()`, `print_success()`, `print_warning()`, `print_error()` - Colored output
- `compile_project()` - Project compilation with error handling
- `check_java()` - Java availability and version validation
- `check_maven()` - Maven availability validation

#### Database Management
```bash
./clean-database.zsh               # Database cleanup and maintenance
./scripts/check-db-cleanup.zsh     # Database cleanup verification
```

### 🚀 Quick Script Commands

**Run complete test suite:**
```bash
./run_all_tests.zsh
```

**Test thread safety comprehensively:**
```bash
./test_thread_safety_full.zsh
```

**Validate data consistency:**
```bash
./test_data_consistency.zsh
```

**Demonstrate API migration benefits:**
```bash
./run_api_migration_demo.zsh
```

**Complete security analysis:**
```bash
./run_security_analysis.zsh
```

### 🎯 Script Categories

**Core Testing**: `run_all_tests.zsh`, `run_basic_tests.zsh`, `run_advanced_tests.zsh`  
**Thread Safety**: `test_thread_safety_full.zsh`, `test_thread_safety_simple.zsh`, `run_thread_safety_test.zsh`, `run_advanced_thread_safety_tests.zsh`  
**Data Consistency**: `test_data_consistency.zsh`, `test_export_import.zsh`, `test_validation.zsh`  
**Security & Recovery**: `run_security_tests.zsh`, `run_recovery_tests.zsh`, `run_security_analysis.zsh`  
**Demonstrations**: `run_api_migration_demo.zsh`, `run_crypto_security_demo.zsh`, `run_enhanced_dangerous_delete_demo.zsh`, `run_advanced_search_demo.zsh`  
**Utilities**: `clean-database.zsh`, `scripts/lib/common_functions.zsh`, `scripts/check-db-cleanup.zsh`

All scripts provide automatic database cleanup, environment management, and comprehensive result reporting. Enhanced scripts include detailed validation output with off-chain data analysis.

## 🔐 Security Module

The security module provides essential cryptographic operations and secure key management for the blockchain implementation.

### Key Components

1. **CryptoUtil (ML-DSA-87 Post-Quantum)**
   - Generates post-quantum ML-DSA-87 key pairs (NIST FIPS 204)
   - 256-bit quantum-resistant security (lattice-based cryptography)
   - Thread-safe implementation with OpenJDK 25 native support
   - Complete KeyPair storage required (no key derivation)
   - Example usage:
     ```java
     KeyPair keyPair = CryptoUtil.generateKeyPair();  // ML-DSA-87
     String signature = CryptoUtil.signData(data, keyPair.getPrivate());
     boolean valid = CryptoUtil.verifySignature(data, signature, keyPair.getPublic());
     ```

2. **KeyFileLoader**
   - Loads ML-DSA-87 key pairs from files
   - Supports both public and private keys in X.509/PKCS#8 formats
   - Handles Base64 encoding for key storage
   - Example usage:
     ```java
     KeyPair keys = KeyFileLoader.loadKeyPairFromFiles("private.key", "public.key");
     KeyFileLoader.saveKeyPairToFiles(keys, "private.key", "public.key");
     ```

3. **SecureKeyStorage (AES-256-GCM)**
   - Stores ML-DSA-87 key pairs with password protection
   - AES-256-GCM authenticated encryption with random IV
   - 128-bit authentication tag for tamper detection
   - Example usage:
     ```java
     SecureKeyStorage.saveKeyPair("user", keyPair, password);
     PrivateKey privateKey = SecureKeyStorage.loadPrivateKey("user", password);
     PublicKey publicKey = SecureKeyStorage.loadPublicKey("user", password);
     ```

4. **PasswordUtil**
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
./run_all_tests.zsh      # Includes automatic cleanup
./run_advanced_tests.zsh # Includes automatic cleanup  
./run_basic_tests.zsh    # Includes automatic cleanup
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

> **Note:** Make sure ZSH is installed on your system to run these scripts. Most macOS systems have ZSH installed by default.

### Manual Database Cleanup
If you encounter database corruption issues:

```zsh
# Clean corrupted database files manually
./clean-database.zsh

# Skip automatic cleanup (for debugging)
SKIP_DB_CLEANUP=true ./run_all_tests.zsh
```

### Script Development

#### Creating New Test Scripts
Use the provided template for consistent script structure:

```zsh
# Copy template for new test script
cp scripts/run_template.zsh run_my_new_test.zsh

# Make executable and customize
chmod +x run_my_new_test.zsh
# Edit the script to add your test logic
```

#### Verify Script Compliance
Check that all run_*.zsh scripts include database cleanup:

```zsh
# Verify all scripts have proper database cleanup
./scripts/check-db-cleanup.zsh
```

**Expected Output:**
```
✅ All run_*.zsh scripts are up to date! ✨
  ✅ Up to date: 3 scripts  
  🔧 Need update: 0 scripts
```

### Shared Functions Library
All scripts now use a centralized functions library at `scripts/lib/common_functions.zsh` providing:

- **Database cleanup functions**: Prevent corruption issues
- **Colored output functions**: Consistent formatting
- **Error handling utilities**: Robust script execution
- **Test environment setup**: Standardized initialization

## 🔐 Safe Key Management

The blockchain includes advanced safety features for key management to prevent accidental data corruption:

### Key Deletion Safety Levels

```java
// 🟢 LEVEL 1: Impact Analysis (RECOMMENDED FIRST STEP)
Blockchain.KeyDeletionImpact impact = blockchain.canDeleteAuthorizedKey(publicKey);
System.out.println("Impact: " + impact);

// 🟡 LEVEL 2: Safe Deletion (blocks dangerous operations)
boolean safe = blockchain.deleteAuthorizedKey(publicKey);

// 🟠 LEVEL 3: Secure Admin-Authorized Deletion (v1.0.6+: throws exceptions)
String adminSignature = CryptoUtil.createAdminSignature(publicKey, false, "GDPR compliance", adminPrivateKey);
try {
    boolean dangerous = blockchain.dangerouslyDeleteAuthorizedKey(publicKey, false, "GDPR compliance", adminSignature, adminPublicKey);
    System.out.println("✅ Key deleted successfully");
} catch (SecurityException e) {
    System.err.println("❌ Invalid admin authorization: " + e.getMessage());
} catch (IllegalStateException e) {
    System.err.println("❌ Deletion blocked: " + e.getMessage());
} catch (IllegalArgumentException e) {
    System.err.println("❌ Key not found: " + e.getMessage());
}

// 🔴 LEVEL 4: Nuclear Option (v1.0.6+: throws exceptions, breaks validation)
String forceSignature = CryptoUtil.createAdminSignature(publicKey, true, "Security incident", adminPrivateKey);
try {
    boolean forced = blockchain.dangerouslyDeleteAuthorizedKey(publicKey, true, "Security incident", forceSignature, adminPublicKey);
    System.out.println("⚠️ Key forcefully deleted - validation may be broken!");
} catch (SecurityException e) {
    System.err.println("❌ Invalid admin authorization: " + e.getMessage());
} catch (IllegalStateException e) {
    System.err.println("❌ Deletion blocked: " + e.getMessage());
} catch (IllegalArgumentException e) {
    System.err.println("❌ Key not found: " + e.getMessage());
}
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
        // Only use dangerous deletion in emergencies (v1.0.6+: throws exceptions):
        // String adminSignature = CryptoUtil.createAdminSignature(publicKey, true, reason, adminPrivateKey);
        // try {
        //     blockchain.dangerouslyDeleteAuthorizedKey(publicKey, true, reason, adminSignature, adminPublicKey);
        //     System.out.println("✅ Forced deletion completed");
        // } catch (SecurityException | IllegalStateException | IllegalArgumentException e) {
        //     System.err.println("❌ Deletion failed: " + e.getMessage());
        // }
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

// 2. Load bootstrap admin keys (RBAC v1.0.6+)
KeyPair bootstrapKeys = KeyFileLoader.loadKeyPairFromFiles(
    "./keys/genesis-admin.private",
    "./keys/genesis-admin.public"
);

// 3. Register bootstrap admin in blockchain (REQUIRED!)
blockchain.createBootstrapAdmin(
    CryptoUtil.publicKeyToString(bootstrapKeys.getPublic()),
    "BOOTSTRAP_ADMIN"
);

// 4. Add authorized users
KeyPair alice = CryptoUtil.generateKeyPair();
String alicePublicKey = CryptoUtil.publicKeyToString(alice.getPublic());
blockchain.addAuthorizedKey(
    alicePublicKey,
    "Alice",
    bootstrapKeys,      // Caller: bootstrap admin
    UserRole.USER       // Target role
);

// 5. Add blocks
blockchain.addBlock("Transaction: Payment to Bob",
                   alice.getPrivate(), alice.getPublic());

// 6. Validate chain with detailed information
ChainValidationResult result = blockchain.validateChainDetailed();
boolean isStructurallyIntact = result.isStructurallyIntact();
boolean isFullyCompliant = result.isFullyCompliant();
System.out.println("Blockchain is structurally intact: " + isStructurallyIntact);
System.out.println("Blockchain is fully compliant: " + isFullyCompliant);

// 7. Search blocks
List<Block> results = blockchain.searchBlocksByContent("Payment");

// 8. Export for backup
blockchain.exportChain("backup.json");
```

## 💡 Complete Usage Example

```java
public class BlockchainExample {
    public static void main(String[] args) {
        try {
            // 1. Initialize blockchain
            Blockchain blockchain = new Blockchain();

            // 2. Load bootstrap admin keys (RBAC v1.0.6+)
            KeyPair bootstrapKeys = KeyFileLoader.loadKeyPairFromFiles(
                "./keys/genesis-admin.private",
                "./keys/genesis-admin.public"
            );

            // 3. Register bootstrap admin in blockchain (REQUIRED!)
            blockchain.createBootstrapAdmin(
                CryptoUtil.publicKeyToString(bootstrapKeys.getPublic()),
                "BOOTSTRAP_ADMIN"
            );

            // 4. Add users with RBAC
            KeyPair alice = CryptoUtil.generateKeyPair();
            KeyPair bob = CryptoUtil.generateKeyPair();

            String aliceKey = CryptoUtil.publicKeyToString(alice.getPublic());
            String bobKey = CryptoUtil.publicKeyToString(bob.getPublic());

            blockchain.addAuthorizedKey(
                aliceKey,
                "Alice",
                bootstrapKeys,      // Caller: bootstrap admin
                UserRole.USER
            );
            blockchain.addAuthorizedKey(
                bobKey,
                "Bob",
                bootstrapKeys,      // Caller: bootstrap admin
                UserRole.USER
            );

            // 5. Add blocks
            blockchain.addBlock("Alice registers", alice.getPrivate(), alice.getPublic());
            blockchain.addBlock("Bob joins network", bob.getPrivate(), bob.getPublic());
            blockchain.addBlock("Alice sends payment", alice.getPrivate(), alice.getPublic());

            // 5. Search and validate
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

            // 6. Backup
            blockchain.exportChain("blockchain_backup.json");
            System.out.println("Blockchain backed up successfully!");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
```

## 📚 Documentation

This project includes 65+ comprehensive documentation files organized into **11 thematic subdirectories**:

### 📂 Documentation Structure

| Category | Location | Files | Description |
|----------|----------|-------|-------------|
| **🚀 Getting Started** | [getting-started/](docs/getting-started/) | 3 | Quick start, examples, troubleshooting |
| **🔍 Search** | [search/](docs/search/) | 9 | All search APIs and implementations |
| **🔐 Security** | [security/](docs/security/) | 6 | Encryption, key management, security |
| **🧪 Testing** | [testing/](docs/testing/) | 6 | Thread-safety, testing patterns, Phase 5.4 isolation |
| **📚 Reference** | [reference/](docs/reference/) | 4 | API reference (192 KB), technical details |
| **🗄️ Database** | [database/](docs/database/) | 3 | SQLite/PostgreSQL/MySQL/H2 configuration |
| **📊 Data Management** | [data-management/](docs/data-management/) | 6 | Pagination, batching, metadata |
| **🔧 Recovery** | [recovery/](docs/recovery/) | 4 | Validation, checkpoints, integrity |
| **📈 Monitoring** | [monitoring/](docs/monitoring/) | 4 | Performance metrics, logging |
| **🔧 Maintenance** | [maintenance/](docs/maintenance/) | 1 | Database VACUUM, size monitoring, cleanup |
| **📊 Reports** | [reports/](docs/reports/) | 18 | Technical audits and analysis |

### 🎯 Quick Start Paths

#### New Developers - Start Here
1. **[Getting Started](docs/getting-started/GETTING_STARTED.md)** - Quick start guide
2. **[Examples](docs/getting-started/EXAMPLES.md)** - Real-world use cases
3. **[API Reference](docs/reference/API_GUIDE.md)** - Complete API (192 KB)

#### Search Implementation
1. **[Search APIs Comparison](docs/search/SEARCH_APIS_COMPARISON.md)** - Which search API to use?
2. **[Search Framework Guide](docs/search/SEARCH_FRAMEWORK_GUIDE.md)** - Professional search
3. **[User-Friendly Search](docs/search/USER_FRIENDLY_SEARCH_GUIDE.md)** - Simplified search API

#### Security & Encryption
1. **[Security Guide](docs/security/SECURITY_GUIDE.md)** - Best practices
2. **[Key Management](docs/security/KEY_MANAGEMENT_GUIDE.md)** - Hierarchical keys
3. **[Encryption Guide](docs/security/ENCRYPTION_GUIDE.md)** - Block encryption

#### Database Configuration
1. **[Database-Agnostic](docs/database/DATABASE_AGNOSTIC.md)** - Switch databases (SQLite/PostgreSQL/MySQL/H2)
2. **[Configuration Storage](docs/database/CONFIGURATION_STORAGE_GUIDE.md)** - JPAConfigurationStorage guide
3. **[Field Limits](docs/database/DATABASE_FIELD_LIMITS.md)** - Size limits and overflow protection

#### Database Maintenance
1. **[Maintenance Guide](docs/maintenance/DATABASE_MAINTENANCE_GUIDE.md)** - Automated VACUUM, size monitoring, file cleanup
2. **[Production Deployment](docs/deployment/PRODUCTION_GUIDE.md)** - Production best practices

#### Technical Audits & Reports
1. **[StampedLock Audit](docs/reports/STAMPEDLOCK_AUDIT_REPORT.md)** - Lock migration audit (✅ Approved)
2. **[AtomicReference Audit](docs/reports/ATOMIC_REFERENCE_AUDIT_REPORT.md)** - Atomicity audit (✅ No issues)
3. **[All Reports Index](docs/reports/README.md)** - 18 reports categorized

### 🚀 Quick Navigation

| What you want to do | Go to |
|---------------------|-------|
| **Get started quickly with examples** | [Getting Started](docs/getting-started/GETTING_STARTED.md) |
| **Choose the right search API** | [Search APIs Comparison](docs/search/SEARCH_APIS_COMPARISON.md) |
| **Use the simplified encryption API** | **README.md - User-Friendly Encryption API section** |
| **Update block metadata dynamically** | [Metadata Management](docs/data-management/METADATA_MANAGEMENT_GUIDE.md) |
| See real-world examples and use cases | [Examples](docs/getting-started/EXAMPLES.md) |
| Learn the complete API and core functions | [API Guide](docs/reference/API_GUIDE.md) |
| Implement UserFriendlyAPI search functionality | [User-Friendly Search](docs/search/USER_FRIENDLY_SEARCH_GUIDE.md) |
| Use Search Framework Engine | [Search Framework](docs/search/SEARCH_FRAMEWORK_GUIDE.md) |
| **Implement robust integrity reporting** | [Off-Chain Integrity Guide](docs/recovery/OFFCHAIN_INTEGRITY_REPORT_GUIDE.md) |
| **Quick start with integrity reports** | [Integrity Quick Start](docs/recovery/OFFCHAIN_INTEGRITY_REPORT_QUICK_START.md) |
| **Review technical audits and analysis** | [Reports Index](docs/reports/README.md) |
| Manage keys and security | [Key Management](docs/security/KEY_MANAGEMENT_GUIDE.md) |
| Follow security best practices | [Security Guide](docs/security/SECURITY_GUIDE.md) |
| Troubleshoot common issues | [Troubleshooting](docs/getting-started/TROUBLESHOOTING_GUIDE.md) |
| Run comprehensive tests (828+ tests, 72% coverage) | **README.md - Testing section** |
| Run tests and troubleshoot issues | [Testing Guide](docs/testing/TESTING.md) |
| **Test components with shared static state** | [Shared State Testing](docs/testing/SHARED_STATE_TESTING_PATTERNS.md) |
| **Implement thread-safe multi-instance operations** | [Thread Safety & Semaphores](docs/development/SEMAPHORE_INDEXING_IMPLEMENTATION.md) |
| Understand technical implementation | [Technical Details](docs/reference/TECHNICAL_DETAILS.md) |
| Deploy to production | [Production Guide](docs/deployment/PRODUCTION_GUIDE.md) |
| Set up encryption and security | [Encryption Guide](docs/security/ENCRYPTION_GUIDE.md) |
| **Switch databases (SQLite/PostgreSQL/MySQL/H2)** | [Database-Agnostic](docs/database/DATABASE_AGNOSTIC.md) |
| Manage configuration storage | [Configuration Storage](docs/database/CONFIGURATION_STORAGE_GUIDE.md) |
| **Configure automated database maintenance** | [Maintenance Guide](docs/maintenance/DATABASE_MAINTENANCE_GUIDE.md) |

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
- **Architecture**: Database-agnostic with support for SQLite, PostgreSQL, MySQL, and H2
- **On-Chain Location**: Configured via `DatabaseConfig` (e.g., `blockchain.db` for SQLite)
- **Off-Chain Location**: `off-chain-data/` directory for large files
- **Configuration**: Switch databases via `DatabaseConfig.create*()` factory methods without code changes
- **JPA Provider**: Hibernate as JPA implementation (100% JPQL queries for database portability)
- **Connection Pooling**: HikariCP with database-optimized settings

### Security
- **Hash Algorithm**: SHA3-256 for block integrity (quantum-resistant)
- **Signature Algorithm**: ML-DSA-87 (NIST FIPS 204, 256-bit quantum-resistant)
- **Access Control**: Authorized public key validation
- **Off-Chain Encryption**: AES-256-GCM with authenticated encryption
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
- **Local Storage**: Off-chain files stored locally (not distributed)
- **No Network**: Designed for single-application use
- **No Consensus**: No multi-node consensus mechanism
- **Key Recovery**: No built-in key recovery system
- **SQLite Concurrency**: SQLite is single-writer (use PostgreSQL/MySQL for high-concurrency production)

### Performance Notes
- **On-Chain Performance**: Small blocks ensure fast blockchain operations
- **Off-Chain Performance**: Large files (up to 100MB) handled efficiently via streaming encryption
- **Search Operations**: Content search may be slow with many blocks
- **Database Size**: On-chain database stays small due to off-chain storage for large data
- **Disk Space**: Monitor off-chain directory growth for large data usage

## 🤝 Contributing

### Development Setup
1. **Environment**: Ensure Java 25+ and Maven 3.6+ are installed
2. **Clone**: Clone the repository to your local development environment
3. **Build**: Run `mvn clean compile` to build the project
4. **Test**: Run `./run_all_tests.zsh` to verify everything works (more than 40 tests)
5. **IDE**: Import as Maven project in your preferred IDE

### Testing New Features
1. **Feature Development**: Add your feature to the appropriate class following existing patterns
2. **Unit Tests**: Create comprehensive JUnit 5 tests following existing test structures
3. **Integration Tests**: Ensure your feature works with existing functionality
4. **Consistency Tests**: Add critical consistency tests for complex scenarios
5. **Documentation**: Update README.md and add code comments
6. **Full Test Suite**: Run `./run_all_tests.zsh` to ensure nothing is broken

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
1. Check the [TESTING.md](docs/testing/TESTING.md) troubleshooting section
2. Verify your Java and Maven versions meet requirements
3. Run `./run_all_tests.zsh` to identify problems
4. Check console output for specific error messages
5. Review [TECHNICAL_DETAILS.md](docs/reference/TECHNICAL_DETAILS.md) for implementation details

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
./run_all_tests.zsh
mvn exec:java -Dexec.mainClass="demo.BlockchainDemo"
```

### Quick Troubleshooting
```zsh
# Reset environment if tests fail
rm blockchain.db blockchain.db-*
./run_all_tests.zsh

# Check Java version (should be 21+)
java -version

# Clean build
mvn clean compile test-compile
```

---

**🚀 Ready to start?** 

1. Run `./run_all_tests.zsh` to verify everything works perfectly
2. Try the practical examples in [EXAMPLES.md](docs/getting-started/EXAMPLES.md) for your use case
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

### Scalability & Performance

**Current Implementation Supports:**

✅ **Unlimited blockchain size** - Memory-safe batch processing with constant O(1000) memory footprint
✅ **High-concurrency reads** - StampedLock with optimistic reads (~50% performance improvement)
✅ **Production-grade databases** - PostgreSQL/MySQL support with 10-60 concurrent connections
✅ **Streaming validation** - `validateChainStreaming()` processes unlimited blocks without memory limits
✅ **Batch operations** - Up to 10K blocks per batch with automatic memory safety checks

See [Performance Optimization Plan](docs/reports/PERFORMANCE_OPTIMIZATION_PLAN.md) for detailed benchmarks.

### Known Limitations

- [ ] **SQLite single-writer limitation**: For high-concurrency workloads, use PostgreSQL or MySQL instead (see [Database Configuration Guide](docs/database/DATABASE_AGNOSTIC.md))
- [x] **~~No automatic database compaction~~**: ✅ **RESOLVED** - Automated maintenance system now available with VACUUM/OPTIMIZE, size monitoring, and file cleanup (see [Maintenance Guide](docs/maintenance/DATABASE_MAINTENANCE_GUIDE.md))
- [ ] **Large exports require memory**: Exporting entire blockchains >500K blocks may require increased heap size (use range exports or streaming validation instead)

### Contribution Guidelines

We welcome contributions! For details on how to contribute to this project, please review the following:
- Follow the code style and patterns used in the codebase
- Ensure all tests pass before submitting changes
- Add tests for new features or bug fixes
- Update documentation as needed

---

**💡 Remember**: This blockchain includes **more than 150 comprehensive tests** covering everything from basic operations to critical consistency scenarios, ensuring enterprise-grade reliability for your applications.
