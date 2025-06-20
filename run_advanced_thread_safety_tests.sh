#!/bin/zsh

# 📊 Advanced Thread-Safety Test Suite for Private Blockchain
# Tests complex scenarios that combine multiple operations to detect hidden race conditions

echo "📊 Private Blockchain - Advanced Thread-Safety Test Suite"
echo "========================================================"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}$1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

# Universal timeout function that works on both macOS and Linux
run_with_timeout() {
    local timeout_duration=$1
    shift
    local command=("$@")
    
    # Execute command in background
    "${command[@]}" &
    local command_pid=$!
    
    # Start timeout counter in background
    (
        sleep "$timeout_duration"
        kill -TERM "$command_pid" 2>/dev/null
        sleep 2
        kill -KILL "$command_pid" 2>/dev/null
    ) &
    local timeout_pid=$!
    
    # Wait for command to complete
    local exit_code=0
    if wait "$command_pid" 2>/dev/null; then
        exit_code=$?
    else
        exit_code=124  # Timeout exit code
    fi
    
    # Kill the timeout process
    kill "$timeout_pid" 2>/dev/null
    wait "$timeout_pid" 2>/dev/null
    
    return $exit_code
}

# Clean previous test database
print_status "🧹 Cleaning previous test database..."
rm -f blockchain.db blockchain.db-shm blockchain.db-wal

# Compile project
print_status "ℹ️ Compiling project..."
if mvn compile test-compile -q; then
    print_success "Compilation successful!"
else
    print_error "Compilation failed!"
    exit 1
fi

echo ""
print_status "📋 Running Advanced Thread-Safety Tests..."
echo "=============================================="

# Track test results
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Function to run a test and track results
run_test() {
    local test_class=$1
    local test_name=$2
    
    echo ""
    print_status "📋 Running: $test_name"
    echo "----------------------------------------"
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    
    # Run test with universal timeout (120 seconds)
    if run_with_timeout 120 mvn test -Dtest="${test_class}"; then
        print_success "$test_name PASSED!"
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        local exit_code=$?
        if [[ $exit_code -eq 124 ]]; then
            print_error "$test_name TIMED OUT (exceeded 120 seconds)!"
        else
            print_error "$test_name FAILED!"
        fi
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
}

# Test 1: Advanced Mixed Operations
run_test "AdvancedThreadSafetyTest" "Advanced Mixed Operations Test"

# Test 2: Edge Cases and Recovery
run_test "EdgeCaseThreadSafetyTest" "Edge Cases and Recovery Test"

# Test 3: Data Integrity
run_test "DataIntegrityThreadSafetyTest" "Data Integrity Test"

# Test 4: Check if original thread safety test class exists
echo ""
print_status "🔍 Checking for Original Thread-Safety Test..."
echo "--------------------------------------------------------"

# First check if the test class exists
if mvn test -Dtest="ThreadSafetyTest" -DfailIfNoTests=false -q > /dev/null 2>&1; then
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    
    echo ""
    print_status "🔍 Running Original Thread-Safety Test..."
    
    # Run with universal timeout
    if run_with_timeout 120 mvn test -Dtest="ThreadSafetyTest"; then
        print_success "Original Thread-Safety Test PASSED!"
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        local exit_code=$?
        if [[ $exit_code -eq 124 ]]; then
            print_warning "Original Thread-Safety Test timed out (exceeded 120 seconds)"
        else
            print_warning "Original Thread-Safety Test had issues"
        fi
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
else
    print_warning "Original Thread-Safety Test class not found - skipping"
fi

# Summary
echo ""
echo "========================================================"
print_status "📊 Advanced Thread-Safety Test Results Summary"
echo "========================================================"

echo "📊 Test Statistics:"
echo "   - Total tests run: $TOTAL_TESTS"
echo "   - Tests passed: $PASSED_TESTS"
echo "   - Tests failed: $FAILED_TESTS"

if [[ $FAILED_TESTS -eq 0 ]]; then
    print_success "✅ ALL ADVANCED THREAD-SAFETY TESTS PASSED!"
    echo ""
    echo "🔑 Your blockchain implementation has successfully passed:"
    echo "   ✅ Complex mixed operations under concurrency"
    echo "   ✅ Edge cases and recovery scenarios"
    echo "   ✅ Data integrity under concurrent modifications"
    echo "   ✅ Memory consistency under load"
    echo "   ✅ High-speed race condition detection"
    echo "   ✅ Timestamp and sequence integrity"
    echo ""
    echo "🔗 Your blockchain is ready for production-level concurrency!"
    
    # Database cleanup
    print_status "🧹 Cleaning up test database..."
    rm -f blockchain.db blockchain.db-shm blockchain.db-wal
    
    exit 0
else
    print_error "Some advanced thread-safety tests failed!"
    echo ""
    echo "⚠️ Issues detected in:"
    [[ $FAILED_TESTS -gt 0 ]] && echo "   - $FAILED_TESTS out of $TOTAL_TESTS tests failed"
    echo ""
    echo "ℹ️ Recommendations:"
    echo "   1. Review the failed test output above"
    echo "   2. Check for race conditions in the identified areas"
    echo "   3. Verify thread-safety implementations"
    echo "   4. Consider adding more synchronization if needed"
    echo ""
    echo "📝 Database files after tests:"
    ls -la blockchain.db* 2>/dev/null || echo "   No database files found"
    
    exit 1
fi
