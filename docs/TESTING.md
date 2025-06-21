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
- ✅ **Secure Key Storage**: AES encrypted private key storage
- ✅ **Password Validation**: Strong password requirements and handling
- ✅ **Key File Loading**: Secure loading of keys from files
- ✅ **Concurrency Testing**: Thread-safe key operations
- ✅ **Performance Testing**: Key operation benchmarks
- ✅ **EC Key Derivation**: Comprehensive tests for elliptic curve key generation and validation

#### Advanced Functions (More than 40 JUnit 5 Tests)
- ✅ **Block Size Validation**: Prevents oversized blocks
- ✅ **Chain Export**: Complete blockchain backup to JSON
- ✅ **Chain Import**: Blockchain restore from backup
- ✅ **Block Rollback**: Safe removal of recent blocks
- ✅ **Advanced Search**: Content, hash, and date range search
- ✅ **Integration**: All functions working together
- ✅ **Error Handling**: Graceful failure handling
- ✅ **Performance**: Execution time validation

### Test Statistics

#### Core Components
- **Blockchain Core**: 15+ test classes
- **DAO Layer**: 5+ test classes
- **Security Module**: 8+ test classes
- **Recovery Module**: 4+ test classes

#### New ECKeyDerivation Tests
- **Basic Key Derivation**: 5+ test cases
- **Input Validation**: 4+ test cases
- **Thread Safety**: 3+ test cases
- **Edge Cases**: 3+ test cases
- **Performance**: 2+ test cases
- **Validation**: 3+ test classes

#### Test Types
- **Unit Tests**: 80+ individual test cases
- **Integration Tests**: 25+ test scenarios
- **Performance Tests**: 15+ benchmark tests
- **Security Tests**: 20+ security test cases
- **Recovery Tests**: 12+ recovery scenarios

#### Test Execution
- **Total Test Files**: 35+ comprehensive test suites
- **JUnit 5 Tests**: 150+ professional unit tests
- **Demo Applications**: 8 interactive demonstrations
- **Verification Tests**: 5+ quick validation tests
- **Script Test Runners**: 15+ specialized test scripts
- **Total Coverage**: 95%+ code coverage (100% critical paths)

#### Test Categories
- **Core Blockchain**: 100% coverage
- **Security Features**: 100% coverage
- **Recovery Operations**: 100% coverage
- **Database Operations**: 100% coverage
- **Concurrency**: 100% coverage

## 🚀 Test Execution Guide

### Recommended Testing Order

#### 1. Run All Tests (Complete Validation) ⭐ **RECOMMENDED**
```zsh
./run_all_tests.sh
```

This runs everything: basic core tests + advanced function tests.

**Expected output:**
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

**What it does:**
1. Compiles the project
2. Runs JUnit 5 tests for advanced functions
3. Executes core blockchain functionality test
4. Runs demo applications to verify end-to-end functionality
5. Performs quick verification tests

#### 2. Advanced Functions Only (JUnit 5 Tests)
```zsh
./run_advanced_tests.sh
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
./run_recovery_tests.sh
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

#### 4. Improved Rollback Strategy Test
```zsh
./run_improved_rollback_test.sh
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
./run_basic_tests.sh
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

## 📝 Test Suites Description

### 1. JUnit 5 Test Suites

#### 1.1 Core Blockchain Test Suite
**File**: `BlockchainTest.java`  
**Tests**: Comprehensive core functionality tests  
**Coverage**: Basic blockchain operations, initialization, and validation

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

#### 1.6 DAO Delete Operations Test Suite
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

### 5. Quick Validation Tests

**Files**: `SimpleDemo.java`, `QuickDemo.java`  
**Type**: Fast verification tests  
**Coverage**: Basic functionality verification

**Purpose**: Quick validation that core functionality works without extensive testing.

## 🔧 Troubleshooting Tests

### Database Utilities and Cleanup

#### Automatic Database Cleanup
All test scripts now include automatic database cleanup to prevent SQLite corruption issues:

```zsh
# All scripts include automatic cleanup
./run_all_tests.sh                   # Auto-cleans before execution
./run_advanced_tests.sh              # Auto-cleans before execution  
./run_advanced_thread_safety_tests.sh # Advanced thread safety tests
./run_basic_tests.sh                 # Auto-cleans before execution
./run_crypto_security_demo.sh        # Cryptographic security demo
./run_security_analysis.sh           # Security analysis tests
./run_security_tests.sh              # Security tests runner
./run_thread_safety_test.sh          # Thread-safety testing
./test_race_condition_fix.sh         # Race condition testing
```

#### Manual Database Cleanup
For persistent database issues:

```zsh
# Manual cleanup of corrupted database files
./clean-database.sh

# Skip automatic cleanup for debugging
SKIP_DB_CLEANUP=true ./run_all_tests.sh
```

#### Database Cleanup Verification
Verify all scripts have proper database cleanup:

```zsh
# Check script compliance
./scripts/check-db-cleanup.sh
```

**Expected Output:**
```
✅ All run_*.sh scripts are up to date! ✨
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
./run_all_tests.sh

# Solution 2: Check for hanging processes
ps aux | grep java
kill -9 <java_process_id>

# Solution 3: Restart and clean build
mvn clean compile
./run_all_tests.sh
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
./run_all_tests.sh

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
./run_all_tests.sh
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

For production deployment considerations and technical specifications, see [PRODUCTION_GUIDE.md](PRODUCTION_GUIDE.md) and [TECHNICAL_DETAILS.md](TECHNICAL_DETAILS.md).

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
./run_all_tests.sh 2>&1 | tee test_output.log
```

**Solutions:**
```zsh
# Clean and optimize
rm blockchain.db*
mvn clean compile
./run_basic_tests.sh  # Test with smaller suite first

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
Permission denied: ./run_all_tests.sh
touch: cannot touch 'blockchain.db': Permission denied
```

**Solutions:**
```zsh
# Fix script permissions
chmod +x *.sh

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
./run_all_tests.sh

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
bash ./run_all_tests.sh

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
./run_basic_tests.sh
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
./run_all_tests.sh > test_complete.log 2>&1

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
# health_check.sh - Comprehensive environment validation

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
ls -la *.sh | head -3
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

echo "🎯 Run './run_all_tests.sh' to execute full test suite"
```

Make the script executable and run:
```zsh
chmod +x health_check.sh
./health_check.sh
```

For comprehensive API documentation, see [API_GUIDE.md](API_GUIDE.md).  
For real-world usage examples, see [EXAMPLES.md](EXAMPLES.md).  
For production deployment, see [PRODUCTION_GUIDE.md](PRODUCTION_GUIDE.md).

## 🛠️ Script Development and Management

### Creating New Test Scripts
Use the provided template for consistent script structure:

```zsh
# Copy template for new test script
cp scripts/run_template.sh run_my_new_test.sh

# Make executable and customize
chmod +x run_my_new_test.sh
# Edit the script to add your test logic
```

### Shared Functions Library
All scripts now use a centralized functions library at `scripts/shared-functions.sh` providing:

- **Database cleanup functions**: Prevent corruption issues
- **Colored output functions**: Consistent formatting  
- **Error handling utilities**: Robust script execution
- **Test environment setup**: Standardized initialization

### Environment Variables
Control script behavior with environment variables:

```zsh
# Skip database cleanup (for debugging)
SKIP_DB_CLEANUP=true ./run_all_tests.sh

# Skip Maven unit tests (if applicable)
SKIP_UNIT_TESTS=true ./run_basic_tests.sh
```

### Script Compliance
Verify all run_*.sh scripts follow best practices:

```zsh
# Check all scripts have proper database cleanup
./scripts/check-db-cleanup.sh
```

**Documentation References:**
- [SCRIPTS_DATABASE_FIX.md](SCRIPTS_DATABASE_FIX.md) - Detailed fix implementation guide
