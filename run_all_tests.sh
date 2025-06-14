#!/bin/bash

# Comprehensive test runner for all Blockchain tests
# Executes both basic core function tests and additional advanced function tests

# Load shared functions for database cleanup (but preserve original structure)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [ -f "$SCRIPT_DIR/scripts/shared-functions.sh" ]; then
    source "$SCRIPT_DIR/scripts/shared-functions.sh"
    # Clean database at start to prevent corruption
    clean_database > /dev/null 2>&1
fi

echo "=== COMPREHENSIVE BLOCKCHAIN TEST RUNNER ==="
echo "This script runs ALL available tests in the correct order"
echo "Project directory: $(pwd)"
echo

# Check if we're in the correct directory
if [ ! -f "pom.xml" ]; then
    echo "❌ Error: pom.xml not found. Please run this script from the project root directory."
    exit 1
fi

# Initialize test results tracking
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0
TEST_RESULTS=()

# Function to record test results
record_test() {
    local test_name="$1"
    local result="$2"
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    if [ "$result" = "PASS" ]; then
        PASSED_TESTS=$((PASSED_TESTS + 1))
        TEST_RESULTS+=("✅ $test_name")
    else
        FAILED_TESTS=$((FAILED_TESTS + 1))
        TEST_RESULTS+=("❌ $test_name")
    fi
}

echo "📦 Step 1: Compiling project and tests..."
mvn clean compile test-compile -q

if [ $? -ne 0 ]; then
    echo "❌ Compilation failed. Please check the errors above."
    exit 1
fi

echo "✅ Compilation successful!"
echo

# Clear any existing database to start fresh
if [ -f "blockchain.db" ]; then
    echo "🗑️ Removing existing database for fresh start..."
    rm blockchain.db
    echo "✅ Database cleared"
fi
echo

echo "=== PART 1: JUNIT 5 TESTS ==="

# Clear database before JUnit tests
if [ -f "blockchain.db" ]; then
    rm blockchain.db
fi

echo "🧪 Running JUnit 5 Additional Advanced Functions tests (22 tests)..."
mvn test -Dtest=BlockchainAdditionalAdvancedFunctionsTest -q
ADVANCED_RESULT=$?

if [ $ADVANCED_RESULT -eq 0 ]; then
    echo "🎉 Additional Advanced Functions tests: PASSED (22/22)"
    record_test "Additional Advanced Functions (22 tests)" "PASS"
else
    echo "❌ Additional Advanced Functions tests: FAILED"
    record_test "Additional Advanced Functions (22 tests)" "FAIL"
fi

# Clear database between test suites
if [ -f "blockchain.db" ]; then
    rm blockchain.db
fi

echo "🧪 Running JUnit 5 Simple Temporal Validation tests (3 tests)..."
mvn test -Dtest=SimpleTemporalValidationTest -q
SIMPLE_TEMPORAL_RESULT=$?

if [ $SIMPLE_TEMPORAL_RESULT -eq 0 ]; then
    echo "🎉 Simple Temporal Validation tests: PASSED (3/3)"
    record_test "Simple Temporal Validation (3 tests)" "PASS"
else
    echo "❌ Simple Temporal Validation tests: FAILED"
    record_test "Simple Temporal Validation (3 tests)" "FAIL"
fi

# Clear database between test suites
if [ -f "blockchain.db" ]; then
    rm blockchain.db
fi

echo "🧪 Running JUnit 5 Key Authorization tests (9 tests)..."
mvn test -Dtest=BlockchainKeyAuthorizationTest -q
AUTHORIZATION_RESULT=$?

if [ $AUTHORIZATION_RESULT -eq 0 ]; then
    echo "🎉 Key Authorization tests: PASSED (9/9)"
    record_test "Key Authorization (9 tests)" "PASS"
else
    echo "❌ Key Authorization tests: FAILED"
    record_test "Key Authorization (9 tests)" "FAIL"
fi

# Clear database between test suites
if [ -f "blockchain.db" ]; then
    rm blockchain.db
fi

echo "🧪 Running JUnit 5 Critical Consistency tests (7 tests)..."
mvn test -Dtest=CriticalConsistencyTest -q
CRITICAL_CONSISTENCY_RESULT=$?

if [ $CRITICAL_CONSISTENCY_RESULT -eq 0 ]; then
    echo "🎉 Critical Consistency tests: PASSED (7/7)"
    record_test "Critical Consistency (7 tests)" "PASS"
else
    echo "❌ Critical Consistency tests: FAILED"
    record_test "Critical Consistency (7 tests)" "FAIL"
fi

echo
echo "=== PART 2: BASIC CORE FUNCTIONS TESTS ==="
echo "🧪 Running basic core functions comprehensive test..."

# Clear database between test suites
if [ -f "blockchain.db" ]; then
    rm blockchain.db
fi

mvn exec:java -Dexec.mainClass="com.rbatllet.blockchain.CoreFunctionsDemo" -q
BASIC_RESULT=$?

if [ $BASIC_RESULT -eq 0 ]; then
    echo "✅ Core Functions Demo: PASSED"
    record_test "Core Functions Demo" "PASS"
else
    echo "❌ Core Functions Demo: FAILED"
    record_test "Core Functions Demo" "FAIL"
fi

echo
echo "=== PART 3: KEY DELETION SECURITY TESTS ==="

# Clear database before security tests
if [ -f "blockchain.db" ]; then
    rm blockchain.db
fi

echo "🔐 Running Key Deletion Security tests (12 tests)..."
mvn test -Dtest=DangerousDeleteAuthorizedKeyTest -q
SECURITY_TEST_RESULT=$?

if [ $SECURITY_TEST_RESULT -eq 0 ]; then
    echo "🎉 Key Deletion Security tests: PASSED (12/12)"
    record_test "Key Deletion Security (12 tests)" "PASS"
else
    echo "❌ Key Deletion Security tests: FAILED"
    record_test "Key Deletion Security (12 tests)" "FAIL"
fi

echo
echo "=== PART 4: DEMO AND BASIC TESTS ==="

# Clear database
if [ -f "blockchain.db" ]; then
    rm blockchain.db
fi

echo "🧪 Running Blockchain Demo..."
mvn exec:java -Dexec.mainClass="com.rbatllet.blockchain.BlockchainDemo" -q
DEMO_RESULT=$?

if [ $DEMO_RESULT -eq 0 ]; then
    echo "✅ Blockchain Demo: PASSED"
    record_test "Blockchain Demo" "PASS"
else
    echo "❌ Blockchain Demo: FAILED"
    record_test "Blockchain Demo" "FAIL"
fi

# Clear database
if [ -f "blockchain.db" ]; then
    rm blockchain.db
fi

echo "🧪 Running Simple Demo..."
mvn exec:java -Dexec.mainClass="com.rbatllet.blockchain.SimpleDemo" -q
SIMPLE_RESULT=$?

if [ $SIMPLE_RESULT -eq 0 ]; then
    echo "✅ Simple Demo: PASSED"
    record_test "Simple Demo" "PASS"
else
    echo "❌ Simple Demo: FAILED"
    record_test "Simple Demo" "FAIL"
fi

# Clear database
if [ -f "blockchain.db" ]; then
    rm blockchain.db
fi

echo "🧪 Running Quick Demo..."
mvn exec:java -Dexec.mainClass="com.rbatllet.blockchain.QuickDemo" -q
QUICK_RESULT=$?

if [ $QUICK_RESULT -eq 0 ]; then
    echo "✅ Quick Demo: PASSED"
    record_test "Quick Demo" "PASS"
else
    echo "❌ Quick Demo: FAILED"
    record_test "Quick Demo" "FAIL"
fi

# Clear database
if [ -f "blockchain.db" ]; then
    rm blockchain.db
fi

echo "🔐 Running Key Deletion Security Demo..."
mvn exec:java -Dexec.mainClass="com.rbatllet.blockchain.DangerousDeleteDemo" -q
SECURITY_DEMO_RESULT=$?

if [ $SECURITY_DEMO_RESULT -eq 0 ]; then
    echo "✅ Security Demo: PASSED"
    record_test "Key Deletion Security Demo" "PASS"
else
    echo "❌ Security Demo: FAILED"
    record_test "Key Deletion Security Demo" "FAIL"
fi

echo
echo "=== COMPREHENSIVE TEST EXECUTION SUMMARY ==="
echo "📊 Total test suites executed: $TOTAL_TESTS"
echo "📊 Test suites passed: $PASSED_TESTS"
echo "📊 Test suites failed: $FAILED_TESTS"
echo
echo "📋 Detailed Results:"
for result in "${TEST_RESULTS[@]}"; do
    echo "   $result"
done

echo
echo "=== FINAL STATUS ==="
if [ $FAILED_TESTS -eq 0 ]; then
    echo "🎉 ALL TESTS PASSED SUCCESSFULLY!"
    echo "✅ Your blockchain implementation is working perfectly!"
    echo "✅ Both basic core functions and additional advanced functions are operational!"
    echo
    echo "🔧 Functions Validated:"
    echo "   ✅ Basic blockchain operations (genesis, add blocks, validation)"
    echo "   ✅ Security (authorized keys, signatures, revocation)"
    echo "   ✅ ADDITIONAL: Block size validation"
    echo "   ✅ ADDITIONAL: Chain export/import"
    echo "   ✅ ADDITIONAL: Block rollback operations"
    echo "   ✅ ADDITIONAL: Advanced search (content, hash, date range)"
    echo "   ✅ ADDITIONAL: Key authorization with temporal validation"
    echo "   ✅ ADDITIONAL: Import with temporal consistency"
    echo "   ✅ ADDITIONAL: Re-authorization scenarios"
    echo "   ✅ CRITICAL: Concurrency and consistency tests"
    echo "   ✅ CRITICAL: Mass operations and error recovery"
    echo "   ✅ Integration and error handling"
    echo
    FINAL_EXIT_CODE=0
else
    echo "❌ SOME TESTS FAILED ($FAILED_TESTS out of $TOTAL_TESTS test suites)"
    echo "📝 Please review the failed tests above"
    echo "💡 Tips:"
    echo "   - Check Java version (should be 21+)"
    echo "   - Verify all dependencies are installed"
    echo "   - Ensure no other processes are using the database"
    echo "   - Try running individual test suites to isolate issues"
    echo
    FINAL_EXIT_CODE=1
fi

echo "📍 Test files location:"
echo "   - JUnit 5 Advanced Function Tests: src/test/java/com/rbatllet/blockchain/core/"
echo "   - JUnit 5 Key Authorization Tests: src/test/java/com/rbatllet/blockchain/core/"
echo "   - JUnit 5 Critical Consistency Tests: src/test/java/com/rbatllet/blockchain/core/"
echo "   - Basic Core Function Tests: src/main/java/com/rbatllet/blockchain/"
echo "📖 Documentation: README.md"
echo

# Clean up test database
if [ -f "blockchain.db" ]; then
    rm blockchain.db
    echo "🗑️ Test database cleaned up"
fi

exit $FINAL_EXIT_CODE
