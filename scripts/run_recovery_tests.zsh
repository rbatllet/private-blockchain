#!/usr/bin/env zsh

# Recovery-focused test runner
# Runs all recovery-related tests including the improved rollback strategy
# Version: 1.0.1

# Set script directory before changing directories
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Load common functions library
source "${SCRIPT_DIR}/lib/common_functions.zsh"

# Change to project root directory
cd "$SCRIPT_DIR/.."

echo "🔄 RECOVERY TESTS RUNNER"
echo "========================"
echo ""

# Check if we're in the correct directory
if [[ ! -f "pom.xml" ]]; then
    print_error "pom.xml not found. Make sure to run this script from the project root directory."
    exit 1
fi

print_info "🏠 Project directory: $(pwd)"

# Check prerequisites
print_info "🔍 Checking prerequisites..."

if ! check_java; then
    exit 1
fi

if ! check_maven; then
    exit 1
fi

print_success "All prerequisites satisfied"

# Clean and compile
cleanup_database

if ! compile_project; then
    exit 1
fi

print_separator

print_step "BLOCKCHAIN RECOVERY TESTS"
echo "============================"
echo ""

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
print_separator

print_step "RECOVERY TESTS SUMMARY"
echo "=========================="
print_info "Total recovery test suites: $TOTAL_TESTS"
print_info "Passed: $PASSED_TESTS"
print_info "Failed: $((TOTAL_TESTS - PASSED_TESTS))"

if [ $PASSED_TESTS -eq $TOTAL_TESTS ]; then
    print_success "ALL RECOVERY TESTS PASSED!"
    echo ""
    print_info "Recovery features validated:"
    print_info "   ✅ Chain recovery after key deletion"
    print_info "   ✅ Intelligent rollback with data preservation"
    print_info "   ✅ Security-first recovery strategies"
    print_info "   ✅ Configuration and edge case handling"
    
    print_separator
    
    # Display next steps
    print_info "Next steps:"
    echo "  1. Run 'scripts/run_advanced_thread_safety_tests.zsh' for thread safety testing"
    echo "  2. Run 'scripts/run_blockchain_demo.zsh' for blockchain operations"
    echo "  3. Check 'target/surefire-reports/' for detailed test reports"
    echo ""
    
    # Final cleanup
    cleanup_database > /dev/null 2>&1
    
    print_success "Recovery tests completed successfully!"
    exit 0
else
    print_error "Some recovery tests failed. Check the output above for details."
    exit 1
fi
