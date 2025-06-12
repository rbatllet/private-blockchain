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

#### Advanced Functions (22 JUnit 5 Tests)
- ✅ **Block Size Validation**: Prevents oversized blocks
- ✅ **Chain Export**: Complete blockchain backup to JSON
- ✅ **Chain Import**: Blockchain restore from backup
- ✅ **Block Rollback**: Safe removal of recent blocks
- ✅ **Advanced Search**: Content, hash, and date range search
- ✅ **Integration**: All functions working together
- ✅ **Error Handling**: Graceful failure handling
- ✅ **Performance**: Execution time validation

### Test Statistics
- **Total Test Files**: 5 comprehensive test suites
- **JUnit 5 Tests**: 22 professional unit tests
- **Demo Applications**: 3 interactive demonstrations
- **Verification Tests**: 2 quick validation tests
- **Total Coverage**: 100% of implemented functionality

## 🚀 Test Execution Guide

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

**What it does:**
1. Compiles the project
2. Runs 22 JUnit 5 tests for advanced functions
3. Executes core blockchain functionality test
4. Runs demo applications to verify end-to-end functionality
5. Performs quick verification tests

#### 2. Advanced Functions Only (JUnit 5 Tests)
```bash
./run_advanced_tests.sh
```

Runs 22 professional JUnit 5 tests for additional advanced functions only.

**Expected output:**
```
=== ADVANCED FUNCTIONS TEST RUNNER ===
✅ Compilation successful!

Running JUnit 5 tests...
[INFO] Tests run: 22, Failures: 0, Errors: 0, Skipped: 0

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

#### 3. Basic Core Functions Only
```bash
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
```bash
# Advanced features demo with practical examples
mvn exec:java -Dexec.mainClass="com.rbatllet.blockchain.core.AdditionalAdvancedFunctionsDemo"

# Basic demo with multiple users
mvn exec:java -Dexec.mainClass="com.rbatllet.blockchain.BlockchainDemo"

# Core functions comprehensive test
mvn exec:java -Dexec.mainClass="com.rbatllet.blockchain.CoreFunctionsTest"
```

#### 5. Quick Verification Tests
```bash
# Fast verification
mvn exec:java -Dexec.mainClass="com.rbatllet.blockchain.QuickTest"

# Basic functionality
mvn exec:java -Dexec.mainClass="com.rbatllet.blockchain.SimpleTest"
```

## 📝 Test Suites Description

### 1. JUnit 5 Advanced Functions Test Suite

**File**: `BlockchainAdditionalAdvancedFunctionsTest.java`  
**Tests**: 22 comprehensive unit tests  
**Coverage**: Advanced blockchain features

#### Test Categories:

**Block Size Validation Tests**
- `testBlockSizeValidation()` - Ensures blocks respect size limits
- `testOversizedBlockRejection()` - Large blocks are properly rejected
- `testBlockDataLengthLimits()` - Character limits enforced

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

#### Running Individual Tests

```bash
# Run specific test method
mvn test -Dtest=BlockchainAdditionalAdvancedFunctionsTest#testChainExport

# Run test category
mvn test -Dtest=BlockchainAdditionalAdvancedFunctionsTest#test*Export*

# Run all advanced tests with verbose output
mvn test -Dtest=BlockchainAdditionalAdvancedFunctionsTest -X
```

### 2. Core Functions Test

**File**: `CoreFunctionsTest.java`  
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
```bash
mvn exec:java -Dexec.mainClass="com.rbatllet.blockchain.CoreFunctionsTest"
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

**Files**: `SimpleTest.java`, `QuickTest.java`  
**Type**: Fast verification tests  
**Coverage**: Basic functionality verification

**Purpose**: Quick validation that core functionality works without extensive testing.

## 🔧 Troubleshooting Tests

### Common Test Issues and Solutions

#### Issue: Tests Fail with "Database locked" Error

**Symptoms:**
```
org.hibernate.exception.GenericJDBCException: could not execute statement
Caused by: java.sql.SQLException: [SQLITE_BUSY] The database file is locked
```

**Solutions:**
```bash
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
- Final test count should show 22/22 tests passed

#### Issue: Maven Build Fails

**Symptoms:**
```bash
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin
```

**Solutions:**
```bash
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
```bash
# Reset and retest
rm blockchain.db*
./run_all_tests.sh

# Check for data corruption
mvn exec:java -Dexec.mainClass="com.rbatllet.blockchain.CoreFunctionsTest"

# Validate specific components
mvn test -Dtest=BlockchainAdditionalAdvancedFunctionsTest#testCompleteWorkflow
```

### Database Issues

#### Reset Database Completely
```bash
# Remove all database files
rm blockchain.db*

# Clear any temporary files
rm -f *.json
rm -f *.tmp

# Restart tests
./run_all_tests.sh
```

#### Check Database Permissions
```bash
# Check permissions
ls -la blockchain.db*

# Fix permissions if needed
chmod 644 blockchain.db*

# Check disk space
df -h .
```

#### Database Integrity Check
```bash
# Check if database is corrupted
sqlite3 blockchain.db "PRAGMA integrity_check;"

# Should return: ok
# If not, database is corrupted and needs reset
```

### Test Environment Validation

#### Validate Java Environment
```bash
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
```bash
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
        boolean isValid = blockchain.validateChain();
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
        System.out.println("  Chain valid: " + isValid);
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
```bash
# Increase JVM heap size
export MAVEN_OPTS="-Xmx2g -Xms512m"
mvn exec:java -Dexec.mainClass="com.rbatllet.blockchain.BlockchainDemo"

# For persistent issues, edit Maven configuration
echo 'export MAVEN_OPTS="-Xmx4g -Xms1g"' >> ~/.bashrc
source ~/.bashrc

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
```bash
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
```bash
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
[ERROR] Failed to read artifact descriptor for org.hibernate:hibernate-core:jar:6.2.5.Final
```

**Solutions:**
```bash
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
```bash
Permission denied: ./run_all_tests.sh
touch: cannot touch 'blockchain.db': Permission denied
```

**Solutions:**
```bash
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
```bash
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
```bash
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
```bash
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
```bash
# Check disk space
df -h .

# Check write permissions
touch test_write.tmp && rm test_write.tmp

# Test export manually
mvn exec:java -Dexec.mainClass="com.rbatllet.blockchain.BlockchainDemo"
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
mvn exec:java -Dexec.mainClass="com.rbatllet.blockchain.CoreFunctionsTest"
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
```bash
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
```bash
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

```bash
# Run tests with detailed debug output
mvn -X test -Dtest=BlockchainAdditionalAdvancedFunctionsTest

# Run with SQLite debug info
mvn -Dhibernate.show_sql=true test

# Run with full Java debug output
mvn -Dmaven.surefire.debug test
```

#### Log Analysis

```bash
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

```bash
#!/bin/bash
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
```bash
chmod +x health_check.sh
./health_check.sh
```

For comprehensive API documentation, see [API_GUIDE.md](API_GUIDE.md).  
For real-world usage examples, see [EXAMPLES.md](EXAMPLES.md).  
For production deployment, see [PRODUCTION_GUIDE.md](PRODUCTION_GUIDE.md).
