# Testing Guide

Comprehensive testing guide for the Private Blockchain implementation with detailed information about test suites, execution, and troubleshooting.

## 📋 Table of Contents

- [Testing Overview](#-testing-overview)
- [Test Execution Guide](#-test-execution-guide)
- [Test Suites Description](#-test-suites-description)
- [Troubleshooting Tests](#-troubleshooting-tests)
- [Performance Testing](#-performance-testing)

## 🧪 Testing Overview

The project includes comprehensive test suites to verify all functionality and ensure reliability.

### Test Categories

#### Core Blockchain Functions
- ✅ Genesis block creation
- ✅ Add/revoke authorized keys  
- ✅ Add blocks to chain
- ✅ Chain validation and integrity
- ✅ Security controls and authorization
- ✅ Error handling and edge cases

#### Security Functions (Migrated from CLI)
- ✅ **Secure Key Storage**: AES-256-GCM encrypted private key storage
- ✅ **Password Validation**: Strong password requirements and handling
- ✅ **Key File Loading**: Secure loading of keys from files
- ✅ **Concurrency Testing**: Thread-safe key operations
- ✅ **Performance Testing**: Key operation benchmarks
- ✅ **EC Key Derivation**: Comprehensive tests for elliptic curve key generation and validation

#### Advanced Functions (**828+ JUnit 5 Tests** with **72% Code Coverage**)
- ✅ **Block Size Validation**: Prevents oversized blocks
- ✅ **Chain Export**: Complete blockchain backup to JSON
- ✅ **Chain Import**: Blockchain restore from backup
- ✅ **Block Rollback**: Safe removal of recent blocks
- ✅ **Hybrid Search System**: Multi-level search with keyword extraction and validation
- ✅ **Advanced Search**: Content, hash, and date range search
- ✅ **UserFriendlyEncryptionAPI**: Comprehensive 212-method encryption and blockchain API
- ✅ **Security Testing**: Encryption, key management, and authentication
- ✅ **Search Testing**: Multi-level search, caching, and advanced search
- ✅ **Storage Testing**: Off-chain storage, compression, and tiering
- ✅ **Recovery Testing**: Chain recovery, corruption detection, and repair
- ✅ **Integration**: All functions working together
- ✅ **Error Handling**: Graceful failure handling
- ✅ **Performance**: Execution time validation

### Test Statistics

#### Core Components
- **Blockchain Core**: 15+ test classes
- **UserFriendlyEncryptionAPI**: **7+ dedicated test classes** with **46+ tests each**
- **DAO Layer**: 5+ test classes
- **Security Module**: 8+ test classes
- **Recovery Module**: 4+ test classes  
- **Search Module**: 3+ test classes (SearchFunctionalityTest + UserFriendlyEncryptionAPI search tests)

#### New ECKeyDerivation Tests
- **Basic Key Derivation**: 5+ test cases
- **Input Validation**: 4+ test cases
- **Thread Safety**: 3+ test cases
- **Edge Cases**: 3+ test cases
- **Performance**: 2+ test cases
- **Validation**: 3+ test classes

#### Test Types
- **Unit Tests**: **828+ individual test cases** (added 33 comprehensive security tests)
- **Integration Tests**: 25+ test scenarios
- **Performance Tests**: 15+ benchmark tests
- **Security Tests**: **83+ security test cases** (enhanced with comprehensive Blockchain security testing)
- **Recovery Tests**: **20+ recovery scenarios** (expanded chain recovery)
- **Search Tests**: **100+ search test cases** (multi-level search testing)
- **API Tests**: **300+ UserFriendlyEncryptionAPI test cases**

#### Test Execution
- **Total Test Files**: **46+ comprehensive test suites** (added BlockchainComprehensiveSecurityTest)
- **JUnit 5 Tests**: **828+ professional unit tests** (33 new security tests)
- **Code Coverage**: **72% overall coverage** (target: 75%+)
- **Demo Applications**: 9 interactive demonstrations (including SearchFrameworkDemo)
- **Verification Tests**: 5+ quick validation tests
- **Script Test Runners**: 15+ specialized test scripts
- **Critical Path Coverage**: 100% security-critical operations

#### Test Categories by Coverage
- **Core Blockchain**: 100% coverage (enhanced with 33 comprehensive security tests)
- **Security Features**: 100% coverage (comprehensive validation of uncovered methods)
- **UserFriendlyEncryptionAPI**: 72% coverage (**212 methods tested**)
- **Recovery Operations**: 100% coverage
- **Database Operations**: 100% coverage
- **Concurrency**: 100% coverage (thread safety validation with 10 threads, 50 operations)
- **Search Operations**: 85% coverage (multi-level search + malicious input testing)
- **Storage Management**: 80% coverage (off-chain + compression + size validation)

## 🚀 Test Execution Guide

### Recommended Testing Order

#### 1. Run All Tests (Complete Validation) ⭐ **RECOMMENDED**
```zsh
./scripts/run_all_tests.zsh
```

This runs everything: basic core tests + advanced function tests.

**Expected output:**
```
=== COMPREHENSIVE BLOCKCHAIN TEST RUNNER ===
✅ Compilation successful!
🎉 JUnit 5 tests: PASSED (828+ tests, 72% coverage)
✅ Basic Core Functions test: PASSED
✅ Blockchain Demo: PASSED
✅ Simple Test: PASSED
✅ Quick Test: PASSED

📊 Test suites passed: 5/5
🎉 ALL TESTS PASSED SUCCESSFULLY!
```

**What it does:**
1. Compiles the project
2. Runs JUnit 5 tests for advanced functions
3. Executes core blockchain functionality test
4. Runs demo applications to verify end-to-end functionality
5. Performs quick verification tests

#### 2. Advanced Functions Only (JUnit 5 Tests)
```zsh
./scripts/run_advanced_tests.zsh
```

Runs professional JUnit 5 tests for additional advanced functions only.

**Expected output:**
```
=== ADVANCED FUNCTIONS TEST RUNNER ===
✅ Compilation successful!

Running JUnit 5 tests...
[INFO] Tests run: more than 40, Failures: 0, Errors: 0, Skipped: 0

Test Results:
✅ testBlockSizeValidation - Block size limits enforced
✅ testChainExport - Export functionality working
✅ testChainImport - Import functionality working
✅ testBlockRollback - Rollback operations safe
✅ testAdvancedSearch - Search capabilities verified
✅ testErrorHandling - Error handling robust
... (and 16 more tests)

🎉 ALL ADVANCED FUNCTION TESTS PASSED!
```

**Note**: You may see error messages like "Error exporting chain" or "Import file not found" during these tests. These are **intentional test cases** that verify proper error handling - they are not actual failures.

#### 3. Recovery Tests (Chain Recovery & Rollback)
```zsh
./scripts/run_recovery_tests.zsh
```

Runs all recovery-related tests including chain recovery manager, recovery configuration, and improved rollback strategy.

**Expected output:**
```
🔄 BLOCKCHAIN RECOVERY TESTS
============================

🧪 Running Chain Recovery Manager Tests...
✅ Chain Recovery Manager Tests: PASSED

🧪 Running Recovery Configuration Tests...
✅ Recovery Configuration Tests: PASSED

🧠 Running Improved Rollback Strategy Tests...
✅ Improved Rollback Strategy Tests: PASSED

📊 Test suites passed: 3/3
🎉 ALL RECOVERY TESTS PASSED SUCCESSFULLY!
```

#### 4. UserFriendlyEncryptionAPI Tests ⭐ **NEW**
```zsh
# Run all UserFriendlyEncryptionAPI tests
mvn test -Dtest="*UserFriendlyEncryptionAPI*Test"

# Run specific test phases
mvn test -Dtest=UserFriendlyEncryptionAPIPhase1Test          # Core functionality  
mvn test -Dtest=UserFriendlyEncryptionAPIPhase2SearchTest    # Search capabilities
mvn test -Dtest=UserFriendlyEncryptionAPISecurityTest        # Security features
mvn test -Dtest=UserFriendlyEncryptionAPIZeroCoverageTest    # Edge cases
mvn test -Dtest=UserFriendlyEncryptionAPIRemainingCoverageTest # Full coverage
```

**Expected output:**
```
[INFO] Tests run: 46, Failures: 0, Errors: 0, Skipped: 0
[INFO] Tests run: 42, Failures: 0, Errors: 0, Skipped: 0  
[INFO] Tests run: 38, Failures: 0, Errors: 0, Skipped: 0
[INFO] Tests run: 51, Failures: 0, Errors: 0, Skipped: 0
[INFO] Tests run: 46, Failures: 0, Errors: 0, Skipped: 0

🎉 UserFriendlyEncryptionAPI Tests: PASSED (828+ tests total)
📊 Code Coverage: 72% achieved (target: 75%+)
✅ All security-critical methods: 100% coverage
```

**Test Categories Covered:**
- **📦 Core Functionality**: Data storage, retrieval, encryption/decryption
- **🔍 Search Operations**: Multi-level search, caching, advanced search  
- **🔐 Security Features**: Key management, authentication, validation
- **💾 Storage Management**: Off-chain storage, compression, tiering
- **🔧 Recovery Operations**: Chain recovery, corruption detection, repair
- **📊 Analytics**: Reporting, metrics, performance monitoring
- **🎯 Edge Cases**: Error handling, boundary conditions, invalid inputs

#### 5. Coverage Report Generation
```zsh
# Generate detailed coverage report
mvn clean test jacoco:report

# View coverage report in browser
open target/site/jacoco/index.html
```

**Coverage Report Contents:**
- **Overall Coverage**: 72% across all classes
- **UserFriendlyEncryptionAPI**: 185 out of 212 methods covered
- **Critical Paths**: 100% coverage for security operations
- **Test Distribution**: 828+ tests across 8+ test classes
- **Missing Coverage**: 27 methods identified for future testing

#### 6. Improved Rollback Strategy Test
```zsh
./scripts/run_improved_rollback_test.zsh
```

Runs only the improved rollback strategy tests that verify the intelligent rollback analysis.

**Expected output:**
```
🧪 IMPROVED ROLLBACK STRATEGY TEST
==================================

Running ImprovedRollbackStrategyTest...

✅ Improved Rollback Strategy Test: PASSED

🎯 Test verified:
   • Intelligent rollback analysis
   • Security-first approach with data preservation
   • Hash chain integrity verification
   • Multiple safety checks and fallbacks
```

#### 5. Basic Tests Only (Quick Verification)
```zsh
./scripts/run_basic_tests.zsh
```

Runs the comprehensive basic core functions test that validates fundamental blockchain operations.

**Expected output:**
```
=== BASIC CORE FUNCTIONS TEST RUNNER ===
✅ Compilation successful!

Running basic core functionality tests...

=== BASIC CORE FUNCTIONS TEST ===
✅ Genesis block created successfully!
✅ Authorized key added for: TestUser1
✅ Authorized key added for: TestUser2
✅ Block #1 added successfully!
✅ Block #2 added successfully!
✅ Block #3 added successfully!
✅ Chain validation successful!
✅ Content search test passed!
✅ Export/import test passed!

🎉 ALL BASIC CORE TESTS PASSED!
```

#### 4. Interactive Demonstrations
```zsh
# Advanced features demo with practical examples
mvn exec:java -Dexec.mainClass="demo.AdditionalAdvancedFunctionsDemo"

# Basic demo with multiple users
mvn exec:java -Dexec.mainClass="demo.BlockchainDemo"

# Chain recovery demonstration
mvn exec:java -Dexec.mainClass="demo.ChainRecoveryDemo"

# Key deletion safety features demo
mvn exec:java -Dexec.mainClass="demo.DangerousDeleteDemo"

# Advanced recovery techniques example
mvn exec:java -Dexec.mainClass="demo.EnhancedRecoveryExample"

# Core functions comprehensive test
mvn exec:java -Dexec.mainClass="demo.CoreFunctionsDemo"
```

#### 5. Quick Verification Tests
```zsh
# Fast verification
mvn exec:java -Dexec.mainClass="demo.QuickDemo"

# Basic functionality
mvn exec:java -Dexec.mainClass="demo.SimpleDemo"
```

## 🌟 Testing Best Practices

The project follows these testing best practices:

### 1. Behavior-Based Testing

- Tests verify behavior rather than specific output formats
- Assertions focus on expected behavior, not implementation details
- No hardcoded special cases in implementation or tests
- Tests use assertions that allow implementation to evolve without breaking

### 2. Edge Case Coverage

- Edge cases are properly handled and tested (null inputs, empty strings, etc.)
- Boundary conditions are explicitly tested
- Error conditions and exception handling are verified

### 3. Test Independence

- Tests do not depend on each other's state
- Each test can run independently
- Test setup and teardown properly manage test environment

### 4. Code Quality in Tests

- Test code follows the same quality standards as production code
- Tests are readable and maintainable
- Test names clearly describe what is being tested

### 5. Database Lifecycle Management

**⚠️ CRITICAL: Proper JPAUtil Lifecycle Management**

Following existing test patterns, tests MUST manage JPAUtil properly:

#### ✅ CORRECT Pattern (Used by All Maintenance Tests)

```java
@DisplayName("Example Test Class")
class ExampleTest {

    @BeforeAll
    static void initializeDatabase() {
        // Initialize JPAUtil with default configuration (respects environment variables)
        JPAUtil.initializeDefault();
    }

    @AfterEach
    void tearDown() {
        // Close EntityManager for current thread (NOT shutdown!)
        JPAUtil.closeEntityManager();
    }

    // For Nested tests that intentionally shutdown JPAUtil
    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @AfterEach
        void reinitializeJPAUtil() {
            // Reinitialize JPAUtil after tests that intentionally shut it down
            JPAUtil.initializeDefault();
        }

        @Test
        void testDatabaseErrors() {
            JPAUtil.shutdown();  // Intentional shutdown for error testing
            // Test error handling...
        }
    }
}
```

#### ❌ INCORRECT Patterns

```java
// ❌ BAD: Calling shutdown() in @AfterEach
@AfterEach
void tearDown() {
    JPAUtil.shutdown();  // WRONG! Closes EntityManagerFactory globally!
}

// ❌ BAD: Not initializing JPAUtil at all
// Tests may fail if EntityManagerFactory not initialized

// ❌ BAD: Initializing in @BeforeEach instead of @BeforeAll
@BeforeEach
void setUp() {
    JPAUtil.initializeDefault();  // WRONG! Should be @BeforeAll
}
```

#### Key Principles

1. **@BeforeAll (static)**: Initialize JPAUtil once per test class
   - Respects environment variables for database configuration
   - Creates EntityManagerFactory with default or configured database

2. **@AfterEach (instance)**: Clean up thread-local EntityManager
   - Closes EntityManager for current thread only
   - Does NOT close EntityManagerFactory
   - Allows subsequent tests to reuse the factory

3. **Nested Test Classes**: Reinitialize after intentional shutdowns
   - Use `@AfterEach` in nested classes that call `JPAUtil.shutdown()`
   - Call `JPAUtil.initializeDefault()` to restore factory

4. **Database-Agnostic**: Tests work with SQLite, PostgreSQL, MySQL, H2
   - `JPAUtil.initializeDefault()` checks environment variables
   - Falls back to SQLite if no env vars configured

#### Why This Matters

- ❌ **EntityManagerFactory is closed**: If you call `shutdown()` in `@AfterEach`, ALL subsequent tests fail with "EntityManagerFactory is closed"
- ✅ **EntityManager is thread-local**: Each test gets its own EntityManager, properly cleaned up after each test
- ✅ **EntityManagerFactory is shared**: All tests in the class reuse the same factory, avoiding expensive recreations

## 📝 Test Suites Description

### 1. JUnit 5 Test Suites

#### 1.1 Core Blockchain Test Suite
**File**: `BlockchainTest.java`
**Tests**: Comprehensive core functionality tests
**Coverage**: Basic blockchain operations, initialization, and validation

#### 1.1.1 Comprehensive Security Test Suite ⭐ **NEW**
**File**: `BlockchainComprehensiveSecurityTest.java`
**Tests**: 33 rigorous security tests covering previously untested methods
**Coverage**: Advanced security validation for uncovered Blockchain methods

**Test Categories:**
- 🧹 **Test Cleanup and Maintenance** (2 tests): `completeCleanupForTests()` safety and multiple calls - removes blocks, sequences, and authorized keys (but NOT off-chain files)
- 📊 **Chain Data Retrieval** (3 tests): Batch processing and pagination with thread safety (10 threads, 50 operations)
- 🔍 **Search Functionality Security** (4 tests): `searchBlocksComplete()`, malicious input handling (SQL injection, XSS, path traversal)
- 🔐 **Authorization and Compliance** (6 tests): `getAuthorizedKeyByOwner()`, `isFullyCompliant()` validation
- 📅 **Time-Based Operations** (3 tests): `getBlocksByTimeRange()` with null parameters and invalid ranges
- 🔒 **Encryption and Security Validation** (3 tests): `validateBlockSize()` with null data and extremely large inputs (10MB)
- 🛡️ **Block Registration Security** (3 tests): Private method testing through public APIs and unauthorized key validation
- 🔒 **Block Input Validation Security** (5 tests): `validateBlockInput()` through `addBlock()` testing (null data, keys, mismatched pairs)
- 🔄 **Block Update Security** (4 tests): `updateBlock()` with JPA-level immutable field protection (`updatable=false`)

**Key Security Features Tested:**
- **Thread Safety**: Concurrent operations with ExecutorService (10 threads, 50 operations)
- **Malicious Input Defense**: SQL injection, XSS, path traversal, null bytes, oversized strings
- **Authorization Controls**: Unauthorized key rejection, JPA-enforced immutable field protection
- **Input Validation**: Comprehensive null-safety, empty parameter handling
- **Edge Case Handling**: Invalid time ranges, non-existent blocks, extremely large data
- **Private Method Coverage**: Testing through public API behavior patterns

#### 1.2 Advanced Functions Test Suite
**File**: `BlockchainAdditionalAdvancedFunctionsTest.java`  
**Tests**: Comprehensive unit tests  
**Coverage**: Advanced blockchain features

#### 1.3 Key Authorization Test Suite
**File**: `BlockchainKeyAuthorizationTest.java`  
**Tests**: Key authorization functionality tests  
**Coverage**: Key management and authorization

#### 1.4 Critical Consistency Test Suite
**File**: `CriticalConsistencyTest.java`  
**Tests**: Data consistency validation tests  
**Coverage**: Blockchain data integrity and consistency

#### 1.5 Temporal Validation Test Suite
**File**: `SimpleTemporalValidationTest.java`  
**Tests**: Temporal validation tests  
**Coverage**: Time-based operations and validations

#### 1.6 Search Functionality Test Suite ⭐ **NEW**
**File**: `SearchFunctionalityTest.java`  
**Tests**: 12 comprehensive search system tests  
**Coverage**: Complete hybrid search functionality

**Test Methods:**
- `testFastOnlySearch()` - Keywords-only search performance and accuracy
- `testIncludeDataSearch()` - Keywords + block data search functionality
- `testExhaustiveOffchainSearch()` - Complete content search including off-chain data
- `testSearchByCategory()` - Content category filtering and validation
- `testSearchTermValidation()` - Search term validation with intelligent exceptions
- Search validation is now handled internally by the Search Framework Engine
- Automatic keyword extraction is now integrated into the search engine
- `testSearchPerformance()` - Performance comparison across search levels
- `testConcurrentSearchOperations()` - Thread-safety with concurrent searches
- `testSearchDuringBlockCreation()` - Search consistency during block operations
- `testSearchEdgeCases()` - Edge cases and special character handling
- `testSearchResultConsistency()` - Result consistency and ordering validation

**Key Features Tested:**
- Multi-level search (FAST_ONLY, INCLUDE_DATA, EXHAUSTIVE_OFFCHAIN)
- Automatic keyword extraction (dates, numbers, emails, codes, technical terms)
- Content categorization (MEDICAL, FINANCE, TECHNICAL, LEGAL)
- Search validation with 4+ character minimum and intelligent exceptions
- Thread-safety with up to 20 concurrent search operations
- Performance optimization and result consistency
- Integration with off-chain storage system

#### 1.7 DAO Delete Operations Test Suite
**File**: `AuthorizedKeyDAODeleteTest.java`  
**Tests**: DAO delete operations tests  
**Coverage**: Key deletion and database operations

#### 1.7 Chain Recovery Manager Test Suite
**File**: `ChainRecoveryManagerTest.java`  
**Tests**: Recovery functionality tests  
**Coverage**: Chain recovery strategies, error handling, and multi-user scenarios

#### 1.8 Recovery Configuration Test Suite
**File**: `RecoveryConfigTest.java`  
**Tests**: Recovery configuration tests  
**Coverage**: Configuration validation, presets, and builder pattern

#### 1.9 Improved Rollback Strategy Test Suite
**File**: `ImprovedRollbackStrategyTest.java`  
**Tests**: Enhanced rollback strategy tests  
**Coverage**: Intelligent rollback analysis, security-first approach, hash chain integrity verification

#### 1.10 EC Key Derivation Test Suite
**File**: `ECKeyDerivationTest.java` and `ECKeyDerivationThreadSafetyTest.java`  
**Tests**: Comprehensive validation of elliptic curve key derivation  
**Key Features Tested**:
- Basic key derivation from private keys
- Input validation and error conditions
- Thread safety under concurrent access
- Cryptographic correctness
- Performance characteristics  
**Coverage**: 95%+ line and branch coverage

**Test Cases**:

1. **Basic Key Derivation**
   - Derives valid public key from EC private key
   - Verifies derived key matches original key pair
   - Ensures consistent derivation results across multiple runs
   - Validates key pair verification
   - Tests with different key sizes and curves

2. **Input Validation**
   - Handles null private key input
   - Validates curve parameters
   - Rejects invalid key formats
   - Validates point on curve checks

3. **Thread Safety**
   - Concurrent key derivation (10+ threads)
   - Thread-local resource management
   - No race conditions in provider initialization
   - Consistent results under high concurrency

4. **Edge Cases**
   - Handles key at infinity
   - Validates boundary values for curve parameters
   - Recovers from temporary resource constraints
   - Graceful handling of invalid states

5. **Performance**
   - Meets throughput requirements (1000+ ops/sec)
   - Minimal memory overhead
   - Efficient caching of curve parameters
   - Linear scaling with thread count

#### 1.11 Format Utility Test Suite
**File**: `FormatUtilTest.java`  
**Tests**: String formatting and display utility tests  
**Coverage**: Hash truncation, fixed-width formatting, timestamp formatting, block information display

#### 1.12 OffChainFileSearch Robustness Test Suite ⭐ **NEW**
**File**: `OffChainFileSearchRobustnessTest.java`  
**Tests**: 24 comprehensive defensive programming tests  
**Coverage**: Off-chain file search with advanced error handling and thread safety

**Test Categories:**
- **searchContent() Tests (6 tests)**: Null data handling, empty arrays, null parameters
- **performTextSearch() Tests (4 tests)**: Null content, search terms, valid matching  
- **performJsonSearch() Tests (4 tests)**: Null inputs, invalid JSON, valid JSON processing
- **performBinarySearch() Tests (3 tests)**: Null handling, binary content processing
- **searchJsonObject() Tests (4 tests)**: Null parameters, recursion limit enforcement
- **cleanupCache() Tests (2 tests)**: Empty cache handling, thread-safe operations
- **Integration Tests (2 tests)**: Null-safety across all methods, cache robustness

**Key Features Tested:**
- **Defensive Programming**: Comprehensive null-safety and input validation
- **Thread Safety**: Synchronized cache operations and concurrent access protection
- **Recursion Control**: MAX_RECURSION_DEPTH prevents infinite loops in JSON traversal
- **Memory Protection**: Efficient resource management and cleanup
- **Format Support**: Text, JSON, and binary file content searching
- **Error Recovery**: Graceful fallback mechanisms for invalid inputs

**Test Results:**
```
Tests run: 24, Failures: 0, Errors: 0, Skipped: 0
Success rate: 100%
Expected defensive warnings: ✅ Logged correctly
Thread safety: ✅ Verified under concurrent access
```

#### Test Categories:

**Block Size Validation Tests**
- `testValidBlockSizes()` - Ensures blocks respect size limits
- `testInvalidBlockSizes()` - Tests rejection of null data and oversized blocks
- `testBlockDataLengthLimits()` - Character limits enforced
- `testEmptyStringAccepted()` - Empty strings allowed for system blocks
- `testNullDataRejected()` - Null data properly rejected

**Chain Export/Import Tests**
- `testChainExport()` - Export functionality works correctly
- `testChainImport()` - Import restores blockchain properly
- `testExportImportIntegrity()` - Data integrity preserved during export/import
- `testExportToNonexistentDirectory()` - Error handling for invalid paths
- `testImportNonexistentFile()` - Error handling for missing files

**Block Rollback Tests**
- `testBlockRollback()` - Rollback removes correct number of blocks
- `testRollbackToSpecificBlock()` - Rollback to specific position works
- `testRollbackBeyondGenesis()` - Cannot rollback past genesis block
- `testRollbackEmptyChain()` - Proper handling of empty chain rollback

**Advanced Search Tests**
- `testSearchBlocksByContent()` - Content search finds correct blocks
- `testSearchBlocksByHash()` - Hash search works accurately
- `testSearchBlocksByDateRange()` - Date range search functions properly
- `testSearchCaseInsensitive()` - Search is case-insensitive
- `testSearchNoResults()` - Proper handling when no results found

**Integration Tests**
- `testCompleteWorkflow()` - End-to-end functionality test
- `testMultiUserScenario()` - Multiple users working together
- `testErrorRecovery()` - System recovers from errors properly

**Performance Tests**
- `testLargeChainPerformance()` - Performance with many blocks
- `testBulkOperationPerformance()` - Bulk operations execute efficiently

### Complete Test Files Inventory

#### Thread Safety Test Files
- `AdvancedThreadSafetyTest.java` - Advanced concurrency testing
- `ComprehensiveThreadSafetyTest.java` - Comprehensive thread safety validation
- `DataIntegrityThreadSafetyTest.java` - Data integrity under concurrent operations
- `EdgeCaseThreadSafetyTest.java` - Edge case handling in concurrent scenarios
- `ExtremeThreadSafetyTest.java` - Extreme load testing for thread safety
- `RaceConditionFixTest.java` - Validation of race condition fixes
- `ThreadSafetyTest.java` - Basic thread safety testing

#### Core Functionality Test Files
- `BlockchainAdditionalAdvancedFunctionsTest.java` - Advanced blockchain functions
- `BlockchainComprehensiveSecurityTest.java` - Comprehensive security tests for uncovered methods ⭐ **NEW**
- `BlockchainKeyAuthorizationTest.java` - Key authorization functionality
- `BlockchainTest.java` - Core blockchain functionality
- `CriticalConsistencyTest.java` - Data consistency validation
- `DangerousDeleteAuthorizedKeyTest.java` - Safe key deletion testing
- `SimpleTemporalValidationTest.java` - Temporal validation testing
- `TestEnvironmentValidator.java` - Test environment validation

#### DAO Test Files
- `AuthorizedKeyDAODeleteTest.java` - Key deletion in DAO layer

#### Recovery Test Files
- `ChainRecoveryManagerTest.java` - Chain recovery functionality
- `ImprovedRollbackStrategyTest.java` - Improved rollback strategy
- `RecoveryConfigTest.java` - Recovery configuration testing

#### Security Test Files
- `KeyFileLoaderTest.java` - Key file loading functionality
- `PasswordUtilTest.java` - Password utility testing
- `SecureKeyStorageAdvancedTest.java` - Advanced key storage security
- `SecureKeyStorageTest.java` - Basic key storage security

#### Utility Test Files
- `CryptoUtilTest.java` - Cryptographic utility testing
- `ExitUtilTest.java` - Exit utility testing
- `FormatUtilTest.java` - Format utility testing
- `BlockValidationResultTest.java` - Block validation result testing
- `BlockValidationUtilTest.java` - Block validation utility testing

#### Running Individual Tests

```zsh
# Run specific test method
mvn test -Dtest=BlockchainAdditionalAdvancedFunctionsTest#testChainExport

# Run test category
mvn test -Dtest=BlockchainAdditionalAdvancedFunctionsTest#test*Export*

# Run all advanced tests with verbose output
mvn test -Dtest=BlockchainAdditionalAdvancedFunctionsTest -X

# Run NEW advanced search functionality tests
mvn test -Dtest=com.rbatllet.blockchain.search.*Test

# Run specific search test suites
mvn test -Dtest=SearchFrameworkBasicTest
mvn test -Dtest=SearchFrameworkExhaustiveTest
```

### 2. Core Functions Test

**File**: `CoreFunctionsDemo.java`  
**Type**: Interactive demonstration with comprehensive validation  
**Coverage**: Fundamental blockchain operations

**What it tests:**
- Genesis block automatic creation
- Authorized key management (add/list)
- Block addition with proper authorization
- Chain validation and integrity checking
- Basic search functionality
- Export/import operations

**Sample execution:**
```zsh
mvn exec:java -Dexec.mainClass="demo.CoreFunctionsDemo"
```

### 3. Basic Demo Application

**File**: `BlockchainDemo.java`  
**Type**: Multi-user demonstration  
**Coverage**: Real-world usage patterns

**Demonstrates:**
- Setting up multiple authorized users
- Adding blocks from different signers
- Validation of multi-user blockchain
- Basic statistics and reporting

### 4. Advanced Functions Demo

**File**: `AdditionalAdvancedFunctionsDemo.java`  
**Type**: Interactive advanced features demonstration  
**Coverage**: All advanced blockchain features

**Demonstrates:**
- Block size validation with large data
- Chain export and import operations
- Block rollback functionality
- Advanced search capabilities
- Error handling scenarios

### 5. Search System Demo ⭐ **NEW**

**File**: `SearchFrameworkDemo.java`  
**Type**: Comprehensive search system demonstration  
**Coverage**: Complete hybrid search functionality

**Demonstrates:**
- Multi-level search (FAST_ONLY, INCLUDE_DATA, EXHAUSTIVE_OFFCHAIN)
- Automatic keyword extraction from universal elements
- Content categorization (MEDICAL, FINANCE, TECHNICAL, LEGAL)
- Search validation with intelligent exceptions
- Performance comparison across search levels

**How to run:**
```zsh
# Run the search demo directly
mvn exec:java -Dexec.mainClass="demo.SearchFrameworkDemo"

# Or use the provided script
./scripts/run_advanced_search_demo.zsh
```

**Expected output:**
```
=== SEARCH SYSTEM DEMONSTRATION ===
✅ Blockchain initialized
✅ Created test blocks with different categories
🔍 Demonstrating FAST_ONLY search...
🔍 Demonstrating INCLUDE_DATA search...
🔍 Demonstrating EXHAUSTIVE_OFFCHAIN search...
🔍 Demonstrating category search...
📊 Performance comparison completed
🎉 SEARCH DEMO COMPLETED SUCCESSFULLY!
```

### 6. Quick Validation Tests

**Files**: `SimpleDemo.java`, `QuickDemo.java`  
**Type**: Fast verification tests  
**Coverage**: Basic functionality verification

**Purpose**: Quick validation that core functionality works without extensive testing.

## 🔧 Troubleshooting Tests

ℹ️ **For comprehensive troubleshooting beyond test execution issues, see the [TROUBLESHOOTING_GUIDE.md](../getting-started/TROUBLESHOOTING_GUIDE.md) for detailed error resolution, performance optimization, and system diagnostics.**

### Database Utilities and Cleanup

#### Automatic Database Cleanup
All test scripts now include automatic database cleanup to prevent SQLite corruption issues:

```zsh
# All scripts include automatic cleanup
./scripts/run_all_tests.zsh                   # Auto-cleans before execution
./scripts/run_advanced_tests.zsh              # Auto-cleans before execution  
./scripts/run_advanced_thread_safety_tests.zsh # Advanced thread safety tests
./scripts/run_basic_tests.zsh                 # Auto-cleans before execution
./scripts/run_crypto_security_demo.zsh        # Cryptographic security demo
./scripts/run_security_analysis.zsh           # Security analysis tests
./scripts/run_security_tests.zsh              # Security tests runner
./scripts/run_thread_safety_test.zsh          # Thread-safety testing
./scripts/test_race_condition_fix.zsh         # Race condition testing
```

#### Manual Database Cleanup
For persistent database issues:

```zsh
# Manual cleanup of corrupted database files
./scripts/clean-database.zsh

# Skip automatic cleanup for debugging
SKIP_DB_CLEANUP=true ./scripts/run_all_tests.zsh
```

#### Database Cleanup Verification
Verify all scripts have proper database cleanup:

```zsh
# Check script compliance
./scripts/check-db-cleanup.zsh
```

**Expected Output:**
```
✅ All run_*.zsh scripts are up to date! ✨
  ✅ Up to date: 3 scripts  
  🔧 Need update: 0 scripts
```

### Common Test Issues and Solutions

#### Issue: Tests Fail with "Database locked" Error

**Symptoms:**
```
org.hibernate.exception.GenericJDBCException: could not execute statement
Caused by: java.sql.SQLException: [SQLITE_BUSY] The database file is locked
```

**Solutions:**
```zsh
# Solution 1: Reset database
rm blockchain.db*
./scripts/run_all_tests.zsh

# Solution 2: Check for hanging processes
ps aux | grep java
kill -9 <java_process_id>

# Solution 3: Restart and clean build
mvn clean compile
./scripts/run_all_tests.zsh
```

#### Issue: JUnit Tests Show "Intentional" Error Messages

**Symptoms:**
```
Error exporting chain to invalid/path/file.json
Import failed: File not found
```

**Explanation:**
These are **intentional test cases** that verify proper error handling. They are not failures - they confirm the system correctly handles error conditions.

**What to look for:**
- Test result should still show `✅ PASSED`
- Error messages should be followed by successful error handling
- Final test count should show more than 40 tests passed

#### Issue: Maven Build Fails

**Symptoms:**
```zsh
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin
```

**Solutions:**
```zsh
# Check Java version (must be 21+)
java -version

# Clean and rebuild
mvn clean compile

# Update Maven dependencies
mvn dependency:resolve

# Check for missing dependencies
mvn dependency:tree
```

#### Issue: Tests Pass But Blockchain Validation Fails

**Symptoms:**
```
✅ Tests completed but chain validation: false
```

**Solutions:**
```zsh
# Reset and retest
rm blockchain.db*
./scripts/run_all_tests.zsh

# Check for data corruption
mvn exec:java -Dexec.mainClass="demo.CoreFunctionsDemo"

# Validate specific components
mvn test -Dtest=BlockchainAdditionalAdvancedFunctionsTest#testCompleteWorkflow
```

### Database Issues

#### Reset Database Completely
```zsh
# Remove all database files
rm blockchain.db*

# Clear any temporary files
rm -f *.json
rm -f *.tmp

# Restart tests
./scripts/run_all_tests.zsh
```

#### Check Database Permissions
```zsh
# Check permissions
ls -la blockchain.db*

# Fix permissions if needed
chmod 644 blockchain.db*

# Check disk space
df -h .
```

#### Database Integrity Check
```zsh
# Check if database is corrupted
sqlite3 blockchain.db "PRAGMA integrity_check;"

# Should return: ok
# If not, database is corrupted and needs reset
```

### Test Environment Validation

#### Validate Java Environment
```zsh
# Check Java version (must be 21+)
java -version

# Check JAVA_HOME
echo $JAVA_HOME

# Check Maven version
mvn -version

# Test basic compilation
mvn clean compile test-compile
```

#### Environment Validator Test
```zsh
mvn test -Dtest=TestEnvironmentValidator
```

This test validates:
- Java version compatibility
- Maven configuration
- Database connectivity
- File system permissions
- Required dependencies

## 📊 Performance Testing

### Performance Benchmarks

The test suite includes performance validation:

#### Block Addition Performance
```
Target: < 100ms per block
Measured: ~10-50ms per block (depending on system)
```

#### Chain Validation Performance
```
Target: < 1 second for chains up to 1000 blocks
Measured: ~100-500ms for 100 blocks
```

#### Search Performance
```
Target: < 500ms for content search in 1000 blocks
Measured: ~50-200ms for typical searches
```

### Custom Performance Testing

#### Load Testing Script
```java
public class PerformanceTest {
    public static void main(String[] args) {
        Blockchain blockchain = new Blockchain();
        
        // Setup
        KeyPair testKeys = CryptoUtil.generateKeyPair();
        String publicKey = CryptoUtil.publicKeyToString(testKeys.getPublic());
        blockchain.addAuthorizedKey(publicKey, "PerfTestUser");
        
        // Performance test: Add 1000 blocks
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 1000; i++) {
            String data = "Performance test block #" + i + " with timestamp " + System.currentTimeMillis();
            blockchain.addBlock(data, testKeys.getPrivate(), testKeys.getPublic());
            
            if (i % 100 == 0) {
                System.out.println("Added " + i + " blocks...");
            }
        }
        
        long addTime = System.currentTimeMillis() - startTime;
        
        // Performance test: Chain validation
        startTime = System.currentTimeMillis();
        ChainValidationResult result = blockchain.validateChainDetailed();
        boolean isStructurallyIntact = result.isStructurallyIntact();
        boolean isFullyCompliant = result.isFullyCompliant();
        long validateTime = System.currentTimeMillis() - startTime;
        
        // Performance test: Search
        startTime = System.currentTimeMillis();
        List<Block> results = blockchain.searchBlocksByContent("Performance test");
        long searchTime = System.currentTimeMillis() - startTime;
        
        // Results
        System.out.println("\n📊 Performance Results:");
        System.out.println("  Add 1000 blocks: " + addTime + "ms (" + (addTime/1000.0) + "ms per block)");
        System.out.println("  Validate chain: " + validateTime + "ms");
        System.out.println("  Search results: " + results.size() + " blocks found in " + searchTime + "ms");
        System.out.println("  Chain structurally intact: " + isStructurallyIntact);
        System.out.println("  Chain fully compliant: " + isFullyCompliant);
    }
}
```

For production deployment considerations and technical specifications, see [PRODUCTION_GUIDE.md](../PRODUCTION_GUIDE.md) and [TECHNICAL_DETAILS.md](../reference/TECHNICAL_DETAILS.md).

### Advanced Troubleshooting

#### Memory and Performance Issues

**Issue: OutOfMemoryError During Large Operations**

**Symptoms:**
```
java.lang.OutOfMemoryError: Java heap space
Exception in thread "main" java.lang.OutOfMemoryError: GC overhead limit exceeded
```

**Solutions:**
```zsh
# Increase JVM heap size
export MAVEN_OPTS="-Xmx2g -Xms512m"
mvn exec:java -Dexec.mainClass="demo.BlockchainDemo"

# For persistent issues, edit Maven configuration
echo 'export MAVEN_OPTS="-Xmx4g -Xms1g"' >> ~/.zshrc
source ~/.zshrc

# Check memory usage during tests
jstat -gc [java_process_id]

# Monitor system memory
free -h
```

**Issue: Slow Test Execution**

**Symptoms:**
- Tests take more than 5 minutes to complete
- Individual operations are very slow

**Diagnosis:**
```zsh
# Check system resources
top
htop  # if available

# Check disk space
df -h

# Check database size
du -h blockchain.db*

# Monitor test execution
./scripts/run_all_tests.zsh 2>&1 | tee test_output.log
```

**Solutions:**
```zsh
# Clean and optimize
rm blockchain.db*
mvn clean compile
./scripts/run_basic_tests.zsh  # Test with smaller suite first

# Check for resource conflicts
ps aux | grep java
ps aux | grep sqlite

# Optimize database
sqlite3 blockchain.db "VACUUM; ANALYZE;"
```

#### Network and Environment Issues

**Issue: Maven Cannot Download Dependencies**

**Symptoms:**
```
[ERROR] Failed to read artifact descriptor for org.hibernate:hibernate-core:jar:6.6.17.Final
```

**Solutions:**
```zsh
# Clear Maven cache
rm -rf ~/.m2/repository/org/hibernate

# Try offline mode if dependencies exist
mvn -o compile

# Check Maven settings
mvn help:effective-settings

# Update Maven dependencies
mvn dependency:purge-local-repository
mvn compile
```

**Issue: Permission Denied Errors**

**Symptoms:**
```zsh
Permission denied: ./scripts/run_all_tests.zsh
touch: cannot touch 'blockchain.db': Permission denied
```

**Solutions:**
```zsh
# Fix script permissions
chmod +x *.zsh

# Fix directory permissions
chmod 755 .
chmod 644 *.md *.xml

# Check file ownership
ls -la

# Fix ownership if needed (adjust username)
sudo chown -R $USER:$USER .
```

#### Version Compatibility Issues

**Issue: Wrong Java Version**

**Symptoms:**
```
Error: A JNI error has occurred
Unsupported major.minor version 65.0
```

**Solutions:**
```zsh
# Check current Java version
java -version
javac -version

# Check available Java versions (Linux)
update-alternatives --list java

# Check available Java versions (macOS with Homebrew)
/usr/libexec/java_home -V

# Set correct Java version (example for Java 21)
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
export PATH=$JAVA_HOME/bin:$PATH

# Verify Maven uses correct Java
mvn -version
```

**Issue: Maven Version Compatibility**

**Symptoms:**
```
[ERROR] The project com.rbatllet:blockchain:1.0-SNAPSHOT requires Maven version 3.6.0
```

**Solutions:**
```zsh
# Check Maven version
mvn -version

# Update Maven (varies by system)
# On Ubuntu/Debian:
sudo apt update
sudo apt install maven

# On macOS with Homebrew:
brew upgrade maven

# Verify version
mvn -version  # Should show 3.6.0 or higher
```

### Test Data and State Issues

#### Issue: Test Data Conflicts

**Symptoms:**
- Tests fail with "Key already exists" errors
- Inconsistent test results between runs
- Chain validation fails unexpectedly

**Solutions:**
```zsh
# Complete reset procedure
rm blockchain.db*
rm *.json  # Remove any export files
rm -f *.tmp
mvn clean
mvn compile
./scripts/run_all_tests.zsh

# If problems persist, check for hidden files
ls -la
rm -f .blockchain*
```

#### Issue: Import/Export Test Failures

**Symptoms:**
```
Export test failed: Could not write to file
Import test failed: JSON parsing error
```

**Diagnosis and Solutions:**
```zsh
# Check disk space
df -h .

# Check write permissions
touch test_write.tmp && rm test_write.tmp

# Test export manually
mvn exec:java -Dexec.mainClass="demo.BlockchainDemo"
# Then check if export files are created

# Validate JSON format
if [ -f "blockchain_export.json" ]; then
    python -m json.tool blockchain_export.json > /dev/null
    echo "JSON is valid"
fi

# Check file encoding
file blockchain_export.json
```

### Platform-Specific Issues

#### Windows-Specific Issues

**Issue: Script Execution Problems**

**Solutions:**
```cmd
# Use Git Bash or WSL for shell scripts
zsh ./scripts/run_all_tests.zsh

# Or run Maven commands directly
mvn clean compile
mvn test -Dtest=BlockchainAdditionalAdvancedFunctionsTest
mvn exec:java -Dexec.mainClass="demo.CoreFunctionsDemo"
```

**Issue: Path Separator Issues**

**Symptoms:**
- Database file not found
- Export/import path errors

**Solutions:**
- Ensure all paths use forward slashes or proper platform separators
- Check that JAVA_HOME uses backslashes on Windows

#### macOS-Specific Issues

**Issue: SQLite Library Issues**

**Symptoms:**
```
java.lang.UnsatisfiedLinkError: no sqlite-jdbc in java.library.path
```

**Solutions:**
```zsh
# Install native SQLite if needed
brew install sqlite

# Check if issue persists
mvn clean compile
./scripts/run_basic_tests.zsh
```

#### Linux-Specific Issues

**Issue: Missing Build Tools**

**Symptoms:**
```
bash: mvn: command not found
bash: java: command not found
```

**Solutions:**
```zsh
# Install required packages (Ubuntu/Debian)
sudo apt update
sudo apt install openjdk-21-jdk maven

# Install required packages (CentOS/RHEL)
sudo yum install java-21-openjdk maven

# Verify installation
java -version
mvn -version
```

### Getting Additional Help

#### Debug Mode Execution

```zsh
# Run tests with detailed debug output
mvn -X test -Dtest=BlockchainAdditionalAdvancedFunctionsTest

# Run with SQLite debug info
mvn -Dhibernate.show_sql=true test

# Run with full Java debug output
mvn -Dmaven.surefire.debug test
```

#### Log Analysis

```zsh
# Capture complete test output
./scripts/run_all_tests.zsh > test_complete.log 2>&1

# Search for specific errors
grep -i error test_complete.log
grep -i failed test_complete.log
grep -i exception test_complete.log

# Check for warnings
grep -i warn test_complete.log
```

#### Health Check Script

```zsh
#!/usr/bin/env zsh
# health_check.zsh - Comprehensive environment validation

echo "🔍 Blockchain Environment Health Check"
echo "======================================"

# Java version check
echo "☕ Java Version:"
java -version 2>&1 | head -1
echo

# Maven version check  
echo "📦 Maven Version:"
mvn -version | head -1
echo

# File permissions check
echo "📁 File Permissions:"
ls -la *.zsh | head -3
echo

# Disk space check
echo "💾 Disk Space:"
df -h . | tail -1
echo

# Database status
echo "🗄️  Database Status:"
if [ -f "blockchain.db" ]; then
    echo "Database exists: $(ls -lh blockchain.db | awk '{print $5}')"
    sqlite3 blockchain.db "PRAGMA integrity_check;" 2>/dev/null || echo "Database check failed"
else
    echo "No database file found (normal for first run)"
fi
echo

# Test compilation
echo "🔨 Compilation Test:"
mvn compile -q && echo "✅ Compilation successful" || echo "❌ Compilation failed"
echo

echo "🎯 Run './scripts/run_all_tests.zsh' to execute full test suite"
```

Make the script executable and run:
```zsh
chmod +x health_check.zsh
./health_check.zsh
```

For comprehensive API documentation, see [API_GUIDE.md](../reference/API_GUIDE.md).  
For real-world usage examples, see [EXAMPLES.md](../getting-started/EXAMPLES.md).  
For system troubleshooting and error resolution, see [TROUBLESHOOTING_GUIDE.md](../getting-started/TROUBLESHOOTING_GUIDE.md).  
For production deployment, see [PRODUCTION_GUIDE.md](../PRODUCTION_GUIDE.md).

## 🛠️ Script Development and Management

### Creating New Test Scripts
Use the provided template for consistent script structure:

```zsh
# Copy template for new test script
cp scripts/run_template.zsh run_my_new_test.zsh

# Make executable and customize
chmod +x run_my_new_test.zsh
# Edit the script to add your test logic
```

### Shared Functions Library
All scripts now use a centralized functions library at `scripts/lib/common_functions.zsh` providing:

- **Database cleanup functions**: Prevent corruption issues
- **Colored output functions**: Consistent formatting  
- **Error handling utilities**: Robust script execution
- **Test environment setup**: Standardized initialization

### Environment Variables
Control script behavior with environment variables:

```zsh
# Skip database cleanup (for debugging)
SKIP_DB_CLEANUP=true ./scripts/run_all_tests.zsh

# Skip Maven unit tests (if applicable)
SKIP_UNIT_TESTS=true ./scripts/run_basic_tests.zsh
```

### Script Compliance
Verify all run_*.zsh scripts follow best practices:

```zsh
# Check all scripts have proper database cleanup
./scripts/check-db-cleanup.zsh
```

**Documentation References:**
*(For detailed database fix implementation, see the scripts directory)*

---

## Test Isolation Troubleshooting

This section documents common test isolation problems and their solutions to prevent future occurrences.

### Problem 1: Incomplete Cleanup Methods

**Symptom:** Tests pass individually but fail when run in batch with errors like:
```
org.opentest4j.AssertionFailedError: expected: <true> but was: <false>
```

**Root Cause:** Cleanup methods (`@AfterEach`) not removing all test data, causing contamination between tests.

**Example Case (October 2025):**
- **Test:** `BlockchainAdditionalAdvancedFunctionsTest`
- **Issue:** `completeCleanupForTests()` was only cleaning `Block` and `BlockSequence` tables
- **Missing:** Did NOT clean `AuthorizedKey` table
- **Result:** Keys from Test A remained in database, causing Test B to fail with duplicate key errors

**Solution Applied:**
```java
// Blockchain.java - completeCleanupForTests()
public void completeCleanupForTests() {
    blockchain.completeCleanupTestData();      // ✅ Cleans blocks & sequences
    authorizedKeyDAO.cleanupTestData();      // ✅ ADDED: Cleans authorized keys
    // Note: Does NOT clean off-chain files (use clearAndReinitialize() for full cleanup)
}
```

**How to Detect:**
1. Run single test: `mvn test -Dtest=YourTest#testMethod` → ✅ Passes
2. Run all tests: `mvn test` → ❌ Fails
3. Check `@AfterEach` cleanup methods
4. Verify ALL database tables are cleaned

**Prevention Checklist:**
- [ ] Every `@AfterEach` must clean ALL tables used by the test
- [ ] Document what each cleanup method cleans (and what it doesn't)
- [ ] Test cleanup in batch runs, not just individually
- [ ] Use `clearAndReinitialize()` for full cleanup when needed

### Problem 2: Shared Mutable State (Configuration)

**Symptom:** Tests fail randomly when run in parallel or different order with incorrect validation results.

**Root Cause:** Tests modify shared configuration (e.g., `currentMaxBlockChars`) and don't restore defaults.

**Example Case (October 2025):**
- **Test:** `OffChainStorageTest.testDynamicConfiguration()`
- **Issue:** Modified `currentMaxBlockChars`, `currentMaxBlockSizeBytes`, `currentOffChainThresholdBytes`
- **Missing:** `@AfterEach` did NOT call `blockchain.resetLimitsToDefault()`
- **Result:** `BlockchainAdditionalAdvancedFunctionsTest` used wrong limits and failed validation

**Solution Applied:**
```java
// OffChainStorageTest.java
@AfterEach
void cleanUp() {
    // CRITICAL: Reset configuration to defaults to avoid contaminating other tests
    blockchain.resetLimitsToDefault();  // ✅ ADDED
    
    // Clean up off-chain data directory
    // ...
}
```

**How to Detect:**
1. Test modifies `volatile` fields or configuration
2. Test passes alone but fails in batch
3. Error messages show unexpected configuration values
4. Different test order produces different results

**Prevention Checklist:**
- [ ] If test modifies configuration, `@AfterEach` MUST restore defaults
- [ ] Avoid modifying shared state when possible (use test-specific instances)
- [ ] Document configuration modifications in test comments
- [ ] Use `resetLimitsToDefault()` or equivalent reset methods

### Problem 3: Incomplete Resource Cleanup

**Symptom:** Resource exhaustion, file handle leaks, or "too many open files" errors in CI/CD.

**Root Cause:** Tests don't clean up external resources (files, connections, threads).

**Common Resources to Clean:**
- Off-chain data files (`off-chain-data/` directory)
- Database connections (usually automatic with `@Transactional`)
- Temporary files created during tests
- Search engine indexes
- Thread pools or scheduled executors

**Best Practice:**
```java
@AfterEach
void cleanUp() {
    // 1. Reset configuration
    blockchain.resetLimitsToDefault();
    
    // 2. Clean database (if using completeCleanupForTests)
    // blockchain.completeCleanupForTests(); // Only if not using @Transactional
    
    // 3. Clean external files
    try {
        File offChainDir = new File("off-chain-data");
        if (offChainDir.exists()) {
            deleteDirectory(offChainDir);
        }
    } catch (Exception e) {
        logger.error("Cleanup error: " + e.getMessage());
    }
}
```

### Quick Diagnosis Guide

**Test passes individually but fails in batch:**
1. Check `@AfterEach` cleans ALL tables/state
2. Check for shared mutable configuration
3. Run with `mvn test -X` for detailed logs

**Test fails randomly:**
1. Check for race conditions (threading issues)
2. Check for shared state between tests
3. Check test execution order sensitivity

**Test fails in CI but passes locally:**
1. Check for resource leaks (connections, files)
2. Check for environment-specific configuration
3. Check for timing-dependent assertions

### Cleanup Method Comparison

| Method | Cleans Blocks | Cleans Keys | Cleans Off-Chain | Cleans Emergency Backups | Reinitializes Genesis |
|--------|---------------|-------------|------------------|--------------------------|----------------------|
| `completeCleanupForTests()` | ✅ | ✅ | ❌ | ❌ | ❌ |
| `completeCleanupForTestsWithBackups()` | ✅ | ✅ | ❌ | ✅ | ❌ |
| `clearAndReinitialize()` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `cleanupEmergencyBackups()` | ❌ | ❌ | ❌ | ✅ | ❌ |

**When to use:**
- **`completeCleanupForTests()`**: Fast database-only cleanup for unit tests
- **`completeCleanupForTestsWithBackups()`**: Database cleanup + remove accumulated backups
- **`clearAndReinitialize()`**: Full cleanup for integration tests (database + files + reinit)
- **`cleanupEmergencyBackups()`**: Manual cleanup of orphaned backup files only

**Emergency Backup Behavior:**
- **Automatic Creation:** Methods like `clearAndReinitialize()` and `dangerouslyDeleteAuthorizedKey()` create temporary backups in `emergency-backups/` before dangerous operations
- **Backup contains:** Database records only (blocks + keys)
- **Does NOT contain:** Off-chain files (they remain in `off-chain-data/` directory)
- **Rationale:** Off-chain files are cleaned separately; temporary backups are lightweight and fast
- **Automatic Cleanup:** ✨ If operation succeeds → backup is **automatically deleted** (prevents accumulation)
- **Manual Recovery:** If operation fails → backup remains for manual recovery (use `restoreFromBackup()`)
- **Test Suites:** Use `completeCleanupForTestsWithBackups()` to prevent backup accumulation during test runs
- **Methods with Auto-Cleanup:**
  - `clearAndReinitialize()` → Creates `emergency-reinitialize-{timestamp}.json`, deletes after success 🧹
  - `dangerouslyDeleteAuthorizedKey()` → Creates `emergency-key-deletion-{timestamp}.json`, deletes after success 🧹

**Off-Chain Storage:**
- `emergency-backups/` directory should ONLY contain JSON files
- If you see `emergency-backups/off-chain-backup/`, these are orphaned files from old backups
- Use `cleanupEmergencyBackups()` to remove both JSON backups and orphaned off-chain subdirectories

**⚠️ IMPORTANT: Test Execution Mode:**
When tests modify shared configuration (like `OffChainStorageTest.testDynamicConfiguration()`), they can contaminate other tests running in parallel. To prevent this:

```java
@Execution(ExecutionMode.SAME_THREAD)  // Force sequential execution
public class OffChainStorageTest {
    // Tests that modify blockchain configuration
}
```

This ensures tests run one after another, preventing race conditions and configuration contamination.

### Documentation References
- [API_GUIDE.md](API_GUIDE.md#test-utilities) - Complete API for test methods
- [THREAD_SAFETY_TESTS.md](THREAD_SAFETY_TESTS.md) - Concurrency testing patterns
- [SHARED_STATE_TESTING_PATTERNS.md](SHARED_STATE_TESTING_PATTERNS.md) - Shared state management
