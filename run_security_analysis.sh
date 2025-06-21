#!/usr/bin/env zsh

# Advanced Security Test Runner with detailed analysis
# Usage: ./run_security_analysis.sh
# Version: 1.0.1

# Load shared functions for database cleanup
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
if [ -f "$SCRIPT_DIR/scripts/shared-functions.sh" ]; then
    source "$SCRIPT_DIR/scripts/shared-functions.sh"
    clean_database > /dev/null 2>&1
fi

print_step "=== ADVANCED BLOCKCHAIN SECURITY ANALYSIS ==="
print_info "Project directory: $(pwd)"
echo

# Check if we're in the correct directory
if [ ! -f "pom.xml" ]; then
    error_exit "pom.xml not found. Make sure to run this script from the project root directory."
fi

# Clean and compile
print_step "Compiling project..."
mvn clean compile -q > /dev/null 2>&1
if [ $? -ne 0 ]; then
    error_exit "Compilation failed!"
fi
print_success "Compilation successful"
echo

# Test Categories
TESTS_PASSED=0
TESTS_FAILED=0

run_test_category() {
    local test_name="$1"
    local test_class="$2"
    local description="$3"
    
    print_info "Testing: $test_name"
    print_info "Description: $description"
    print_info "Running: $test_class"
    echo "----------------------------------------"
    
    mvn test -Dtest="$test_class" -q
    local result=$?
    
    if [ $result -eq 0 ]; then
        print_success "$test_name: PASSED"
        ((TESTS_PASSED++))
    else
        print_error "$test_name: FAILED"
        ((TESTS_FAILED++))
    fi
    echo
}

# Run different test categories
print_step "SECURITY TEST CATEGORIES"
echo "============================"
echo

run_test_category \
    "Key Deletion Security" \
    "DangerousDeleteAuthorizedKeyTest" \
    "Multi-layered key deletion protection system"

run_test_category \
    "Key Authorization & Management" \
    "BlockchainKeyAuthorizationTest" \
    "Core key authorization and management functionality"

run_test_category \
    "DAO Key Deletion" \
    "AuthorizedKeyDAODeleteTest" \
    "Database-level key deletion operations"

run_test_category \
    "Temporal Validation" \
    "SimpleTemporalValidationTest" \
    "Time-based key authorization validation"

run_test_category \
    "Critical Consistency" \
    "CriticalConsistencyTest" \
    "Blockchain consistency and integrity checks"

# Security Demo Analysis
print_step "SECURITY DEMONSTRATION ANALYSIS"
echo "==================================="
print_info "Running comprehensive security demo..."
echo
mvn compile exec:java -Dexec.mainClass="demo.DangerousDeleteDemo" -q
DEMO_RESULT=$?

if [ $DEMO_RESULT -eq 0 ]; then
    print_success "Security Demo: COMPLETED"
    ((TESTS_PASSED++))
else
    print_error "Security Demo: FAILED"
    ((TESTS_FAILED++))
fi
echo

# Security Analysis Summary
print_step "SECURITY ANALYSIS SUMMARY"
echo "=============================="
print_info "Tests Passed: $TESTS_PASSED"
print_info "Tests Failed: $TESTS_FAILED"
print_info "Total Tests: $((TESTS_PASSED + TESTS_FAILED))"
echo

if [ $TESTS_FAILED -eq 0 ]; then
    print_success "ALL SECURITY TESTS PASSED!"
    echo
    print_info "Security Features Verified:"
    print_success "Multi-layered key deletion protection"
    print_success "Impact analysis before dangerous operations"
    print_success "Safe deletion with automatic blocking"
    print_success "Dangerous deletion with audit trails"
    print_success "Blockchain integrity protection"
    print_success "Temporal validation consistency"
    print_success "Database-level security operations"
    echo
    print_success "Your blockchain implementation has enterprise-grade security!"
else
    print_warning "SECURITY ISSUES DETECTED"
    print_error "Failed tests: $TESTS_FAILED"
    print_info "Please review the test output above and fix any issues."
fi

# Cleanup
if command -v clean_database &> /dev/null; then
    clean_database > /dev/null 2>&1
fi

# Exit with appropriate code
exit $TESTS_FAILED
