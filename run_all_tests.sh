#!/usr/bin/env zsh

# Comprehensive test runner for all Blockchain tests
# Executes both basic core function tests and additional advanced function tests
# Version: 1.0.1

# Load shared functions for database cleanup (but preserve original structure)
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
if [ -f "$SCRIPT_DIR/scripts/shared-functions.sh" ]; then
    source "$SCRIPT_DIR/scripts/shared-functions.sh"
    # Clean database at start to prevent corruption
    clean_database > /dev/null 2>&1
fi

print_step "=== 📊 COMPREHENSIVE BLOCKCHAIN TEST RUNNER ==="
print_info "This script runs ALL available tests in the correct order"
print_info "Project directory: $(pwd)"
print_info ""

# Check if we're in the correct directory
if [ ! -f "pom.xml" ]; then
    error_exit "pom.xml not found. Please run this script from the project root directory."
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

print_step "🔧 Step 1: Compiling project and tests..."
mvn clean compile test-compile -q

if [ $? -ne 0 ]; then
    error_exit "Compilation failed. Please check the errors above."
fi

print_success "✅ Compilation successful!"
print_info ""

# Clear any existing database to start fresh
if [ -f "blockchain.db" ]; then
    print_info "💡 Removing existing database for fresh start..."
    rm blockchain.db
    print_success "✅ Database cleared"
fi
print_info ""

print_step "=== 🧪 PART 1: JUNIT 5 TESTS ==="

# Clear database before JUnit tests
if [ -f "blockchain.db" ]; then
    rm blockchain.db
fi

print_info "🧪 Running JUnit 5 Additional Advanced Functions tests (22 tests)..."
mvn test -Dtest=BlockchainAdditionalAdvancedFunctionsTest -q
ADVANCED_RESULT=$?

if [ $ADVANCED_RESULT -eq 0 ]; then
    print_success "🎉 Additional Advanced Functions tests: PASSED (22/22)"
    record_test "Additional Advanced Functions (22 tests)" "PASS"
else
    print_error "❌ Additional Advanced Functions tests: FAILED"
    record_test "Additional Advanced Functions (22 tests)" "FAIL"
fi

# Clear database between test suites
if [ -f "blockchain.db" ]; then
    rm blockchain.db
fi

print_info "🧪 Running JUnit 5 Simple Temporal Validation tests (3 tests)..."
mvn test -Dtest=SimpleTemporalValidationTest -q
SIMPLE_TEMPORAL_RESULT=$?

if [ $SIMPLE_TEMPORAL_RESULT -eq 0 ]; then
    print_success "🎉 Simple Temporal Validation tests: PASSED (3/3)"
    record_test "Simple Temporal Validation (3 tests)" "PASS"
else
    print_error "❌ Simple Temporal Validation tests: FAILED"
    record_test "Simple Temporal Validation (3 tests)" "FAIL"
fi

# Clear database between test suites
if [ -f "blockchain.db" ]; then
    rm blockchain.db
fi

print_info "🧪 Running JUnit 5 Key Authorization tests (9 tests)..."
mvn test -Dtest=BlockchainKeyAuthorizationTest -q
AUTHORIZATION_RESULT=$?

if [ $AUTHORIZATION_RESULT -eq 0 ]; then
    print_success "🎉 Key Authorization tests: PASSED (9/9)"
    record_test "Key Authorization (9 tests)" "PASS"
else
    print_error "❌ Key Authorization tests: FAILED"
    record_test "Key Authorization (9 tests)" "FAIL"
fi

# Clear database between test suites
if [ -f "blockchain.db" ]; then
    rm blockchain.db
fi

print_info "🧪 Running JUnit 5 Critical Consistency tests (7 tests)..."
mvn test -Dtest=CriticalConsistencyTest -q
CRITICAL_CONSISTENCY_RESULT=$?

if [ $CRITICAL_CONSISTENCY_RESULT -eq 0 ]; then
    print_success "🎉 Critical Consistency tests: PASSED (7/7)"
    record_test "Critical Consistency (7 tests)" "PASS"
else
    print_error "❌ Critical Consistency tests: FAILED"
    record_test "Critical Consistency (7 tests)" "FAIL"
fi

print_info ""
print_step "=== 🧪 PART 2: BASIC CORE FUNCTIONS TESTS ==="
print_info "🧪 Running basic core functions comprehensive test..."

# Clear database between test suites
if [ -f "blockchain.db" ]; then
    rm blockchain.db
fi

mvn exec:java -Dexec.mainClass="demo.CoreFunctionsDemo" -q
BASIC_RESULT=$?

if [ $BASIC_RESULT -eq 0 ]; then
    print_success "✅ Core Functions Demo: PASSED"
    record_test "Core Functions Demo" "PASS"
else
    print_error "❌ Core Functions Demo: FAILED"
    record_test "Core Functions Demo" "FAIL"
fi

print_info ""
print_step "=== 🔐 PART 3: KEY DELETION SECURITY TESTS ==="

# Clear database before security tests
if [ -f "blockchain.db" ]; then
    rm blockchain.db
fi

print_info "🔐 Running Key Deletion Security tests (12 tests)..."
mvn test -Dtest=DangerousDeleteAuthorizedKeyTest -q
SECURITY_TEST_RESULT=$?

if [ $SECURITY_TEST_RESULT -eq 0 ]; then
    print_success "🎉 Key Deletion Security tests: PASSED (12/12)"
    record_test "Key Deletion Security (12 tests)" "PASS"
else
    print_error "❌ Key Deletion Security tests: FAILED"
    record_test "Key Deletion Security (12 tests)" "FAIL"
fi

print_info ""
print_step "=== 💻 PART 4: DEMO AND BASIC TESTS ==="

# Clear database
if [ -f "blockchain.db" ]; then
    rm blockchain.db
fi

print_info "🧪 Running Blockchain Demo..."
mvn exec:java -Dexec.mainClass="demo.BlockchainDemo" -q
DEMO_RESULT=$?

if [ $DEMO_RESULT -eq 0 ]; then
    print_success "✅ Blockchain Demo: PASSED"
    record_test "Blockchain Demo" "PASS"
else
    print_error "❌ Blockchain Demo: FAILED"
    record_test "Blockchain Demo" "FAIL"
fi

# Clear database
if [ -f "blockchain.db" ]; then
    rm blockchain.db
fi

print_info "🧪 Running Simple Demo..."
mvn exec:java -Dexec.mainClass="demo.SimpleDemo" -q
SIMPLE_RESULT=$?

if [ $SIMPLE_RESULT -eq 0 ]; then
    print_success "✅ Simple Demo: PASSED"
    record_test "Simple Demo" "PASS"
else
    print_error "❌ Simple Demo: FAILED"
    record_test "Simple Demo" "FAIL"
fi

# Clear database
if [ -f "blockchain.db" ]; then
    rm blockchain.db
fi

print_info "🧪 Running Quick Demo..."
mvn exec:java -Dexec.mainClass="demo.QuickDemo" -q
QUICK_RESULT=$?

if [ $QUICK_RESULT -eq 0 ]; then
    print_success "✅ Quick Demo: PASSED"
    record_test "Quick Demo" "PASS"
else
    print_error "❌ Quick Demo: FAILED"
    record_test "Quick Demo" "FAIL"
fi

# Clear database
if [ -f "blockchain.db" ]; then
    rm blockchain.db
fi

print_info "🔐 Running Key Deletion Security Demo..."
mvn exec:java -Dexec.mainClass="demo.DangerousDeleteDemo" -q
SECURITY_DEMO_RESULT=$?

if [ $SECURITY_DEMO_RESULT -eq 0 ]; then
    print_success "✅ Security Demo: PASSED"
    record_test "Key Deletion Security Demo" "PASS"
else
    print_error "❌ Security Demo: FAILED"
    record_test "Key Deletion Security Demo" "FAIL"
fi

# Thorough database cleanup before running crypto demo
if command -v clean_database &> /dev/null; then
    clean_database > /dev/null 2>&1
else
    # Fallback if clean_database function is not available
    if [ -f "blockchain.db" ]; then
        rm -f blockchain.db blockchain.db-shm blockchain.db-wal 2>/dev/null || true
    fi
fi

print_info "🔐 Running Cryptographic Security Demo..."
mvn exec:java -Dexec.mainClass="demo.CryptoSecurityDemo" -q
CRYPTO_SECURITY_DEMO_RESULT=$?

if [ $CRYPTO_SECURITY_DEMO_RESULT -eq 0 ]; then
    print_success "✅ Crypto Security Demo: PASSED"
    record_test "Cryptographic Security Demo" "PASS"
else
    print_error "❌ Crypto Security Demo: FAILED"
    record_test "Cryptographic Security Demo" "FAIL"
fi

# Thorough database cleanup after running crypto demo
if command -v clean_database &> /dev/null; then
    clean_database > /dev/null 2>&1
else
    # Fallback if clean_database function is not available
    if [ -f "blockchain.db" ]; then
        rm -f blockchain.db blockchain.db-shm blockchain.db-wal 2>/dev/null || true
    fi
fi

print_info ""
print_step "=== 📊 COMPREHENSIVE TEST EXECUTION SUMMARY ==="
print_info "📊 Total test suites executed: $TOTAL_TESTS"
print_info "📊 Test suites passed: $PASSED_TESTS"
print_info "📊 Test suites failed: $FAILED_TESTS"
print_info ""
print_info "📝 Detailed Results:"
for result in "${TEST_RESULTS[@]}"; do
    print_info "   $result"
done

print_info ""
print_step "=== 📊 FINAL STATUS ==="
if [ $FAILED_TESTS -eq 0 ]; then
    print_success "🎉 ALL TESTS PASSED SUCCESSFULLY!"
    print_success "✅ Your blockchain implementation is working perfectly!"
    print_success "✅ Both basic core functions and additional advanced functions are operational!"
    print_info ""
    print_info "🔧 Functions Validated:"
    print_info "   ✅ Basic blockchain operations (genesis, add blocks, validation)"
    print_info "   ✅ Security (authorized keys, signatures, revocation)"
    print_info "   ✅ ADDITIONAL: Block size validation"
    print_info "   ✅ ADDITIONAL: Chain export/import"
    print_info "   ✅ ADDITIONAL: Block rollback operations"
    print_info "   ✅ ADDITIONAL: Advanced search (content, hash, date range)"
    print_info "   ✅ ADDITIONAL: Key authorization with temporal validation"
    print_info "   ✅ ADDITIONAL: Import with temporal consistency"
    print_info "   ✅ ADDITIONAL: Re-authorization scenarios"
    print_info "   ✅ CRITICAL: Concurrency and consistency tests"
    print_info "   ✅ CRITICAL: Mass operations and error recovery"
    print_info "   ✅ SECURITY: Enhanced cryptographic features (SHA-3, ECDSA, key management)"
    print_info "   ✅ Integration and error handling"
    print_info ""
    FINAL_EXIT_CODE=0
else
    print_error "SOME TESTS FAILED ($FAILED_TESTS out of $TOTAL_TESTS test suites)"
    print_info "Please review the failed tests above"
    print_warning "Tips:"
    print_info "   - Check Java version (should be 21+)"
    print_info "   - Verify all dependencies are installed"
    print_info "   - Ensure no other processes are using the database"
    print_info "   - Try running individual test suites to isolate issues"
    print_info ""
    FINAL_EXIT_CODE=1
fi

print_info "📍 Test files location:"
print_info "   - JUnit 5 Advanced Function Tests: src/test/java/com/rbatllet/blockchain/core/"
print_info "   - JUnit 5 Key Authorization Tests: src/test/java/com/rbatllet/blockchain/core/"
print_info "   - JUnit 5 Critical Consistency Tests: src/test/java/com/rbatllet/blockchain/core/"
print_info "   - Basic Core Function Tests: src/main/java/demo/"
print_info "📖 Documentation: README.md"
print_info ""

# Clean up test database
if [ -f "blockchain.db" ]; then
    rm blockchain.db
    print_info "💡 Test database cleaned up"
fi

exit $FINAL_EXIT_CODE
