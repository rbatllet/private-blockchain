#!/usr/bin/env zsh

# Simple script to run the improved rollback strategy test
# Executes the ImprovedRollbackStrategyTest JUnit5 test
# Version: 1.0.1

echo "🧪 IMPROVED ROLLBACK STRATEGY TEST"
echo "=================================="
echo ""

# Load shared functions for consistent output formatting and error handling
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
if [ -f "$SCRIPT_DIR/scripts/shared-functions.sh" ]; then
    source "$SCRIPT_DIR/scripts/shared-functions.sh"
else
    echo "❌ Error: shared-functions.sh not found. Please ensure the scripts directory exists."
    exit 1
fi

print_info "Running ImprovedRollbackStrategyTest..."
echo ""

# Check if we're in the correct directory
if [ ! -f "pom.xml" ]; then
    error_exit "pom.xml not found. Please run this script from the project root directory."
fi

# Run the specific test
mvn test -Dtest=ImprovedRollbackStrategyTest

TEST_EXIT_CODE=$?

echo ""
if [ $TEST_EXIT_CODE -eq 0 ]; then
    print_success "Improved Rollback Strategy Test: PASSED"
    echo ""
    print_info "Test verified:"
    echo "   • Intelligent rollback analysis"
    echo "   • Security-first approach with data preservation"
    echo "   • Hash chain integrity verification"
    echo "   • Multiple safety checks and fallbacks"
else
    print_error "Improved Rollback Strategy Test: FAILED"
fi

exit $TEST_EXIT_CODE
