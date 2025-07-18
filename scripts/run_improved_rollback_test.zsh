#!/usr/bin/env zsh

# Simple script to run the improved rollback strategy test
# Executes the ImprovedRollbackStrategyTest JUnit5 test
# Version: 1.0.1


# Set script directory before changing directories
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Load common functions library
source "${SCRIPT_DIR}/lib/common_functions.zsh"

# Change to project root directory
cd "$SCRIPT_DIR/.."

echo "🧪 IMPROVED ROLLBACK STRATEGY TEST"
echo "=================================="
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

# Run the improved rollback strategy test
print_info "🚀 Running Improved Rollback Strategy Test..."
print_info "Running ImprovedRollbackStrategyTest..."
echo ""

# Execute the test
mvn test -Dtest=ImprovedRollbackStrategyTest
TEST_RESULT=$?

print_separator
if [ $TEST_RESULT -eq 0 ]; then
    print_success "Improved Rollback Strategy Test: PASSED"
    echo ""
    print_info "Test verified:"
    print_info "   • Intelligent rollback analysis"
    print_info "   • Security-first approach with data preservation"
    print_info "   • Hash chain integrity verification"
    print_info "   • Multiple safety checks and fallbacks"
else
    print_error "Improved Rollback Strategy Test: FAILED"
fi

print_separator

# Display next steps
print_info "Next steps:"
echo "  1. Run 'scripts/run_advanced_thread_safety_tests.zsh' for thread safety testing"
echo "  2. Run 'scripts/run_blockchain_demo.zsh' for blockchain operations"
echo "  3. Check 'target/surefire-reports/' for detailed test reports"
echo ""

# Final cleanup
cleanup_database > /dev/null 2>&1

print_success "Improved Rollback Strategy Test completed!"
exit $TEST_RESULT
