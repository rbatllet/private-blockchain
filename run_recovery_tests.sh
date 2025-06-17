#!/usr/bin/env zsh

# Recovery-focused test runner
# Runs all recovery-related tests including the improved rollback strategy
# Version: 1.0.1

print_step "BLOCKCHAIN RECOVERY TESTS"
echo "============================"
echo ""

# Load shared functions for consistent output formatting and error handling
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
if [ -f "$SCRIPT_DIR/scripts/shared-functions.sh" ]; then
    source "$SCRIPT_DIR/scripts/shared-functions.sh"
else
    echo "❌ Error: shared-functions.sh not found. Please ensure the scripts directory exists."
    exit 1
fi

# Check if we're in the correct directory
if [ ! -f "pom.xml" ]; then
    error_exit "pom.xml not found. Please run this script from the project root directory."
fi

TOTAL_TESTS=0
PASSED_TESTS=0

print_info "Running Chain Recovery Manager Tests..."
mvn test -Dtest=ChainRecoveryManagerTest -q
if [ $? -eq 0 ]; then
    print_success "Chain Recovery Manager Tests: PASSED"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    print_error "Chain Recovery Manager Tests: FAILED"
fi
TOTAL_TESTS=$((TOTAL_TESTS + 1))

echo ""
print_info "Running Recovery Configuration Tests..."
mvn test -Dtest=RecoveryConfigTest -q
if [ $? -eq 0 ]; then
    print_success "Recovery Configuration Tests: PASSED"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    print_error "Recovery Configuration Tests: FAILED"
fi
TOTAL_TESTS=$((TOTAL_TESTS + 1))

echo ""
print_info "Running Improved Rollback Strategy Tests..."
mvn test -Dtest=ImprovedRollbackStrategyTest -q
if [ $? -eq 0 ]; then
    print_success "Improved Rollback Strategy Tests: PASSED"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    print_error "Improved Rollback Strategy Tests: FAILED"
fi
TOTAL_TESTS=$((TOTAL_TESTS + 1))

echo ""
echo "=========================="
print_step "RECOVERY TESTS SUMMARY"
echo "=========================="
print_info "Total recovery test suites: $TOTAL_TESTS"
print_info "Passed: $PASSED_TESTS"
print_info "Failed: $((TOTAL_TESTS - PASSED_TESTS))"

if [ $PASSED_TESTS -eq $TOTAL_TESTS ]; then
    print_success "ALL RECOVERY TESTS PASSED!"
    echo ""
    print_info "Recovery features validated:"
    print_success "Chain recovery after key deletion"
    print_success "Intelligent rollback with data preservation"
    print_success "Security-first recovery strategies"
    print_success "Configuration and edge case handling"
    exit 0
else
    error_exit "Some recovery tests failed."
fi
