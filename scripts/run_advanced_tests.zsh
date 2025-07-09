#!/usr/bin/env zsh

# Script to run Additional Advanced Functions tests for the Blockchain
# Usage: ./run_advanced_tests.zsh
# Version: 1.0.1

# Load shared functions for database cleanup (but preserve original structure)

# Set script directory and navigate to project root
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR/.."

# Load common functions library
source "${SCRIPT_DIR}/lib/common_functions.zsh"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
if [ -f "$SCRIPT_DIR/scripts/lib/common_functions.zsh" ]; then
    source "$SCRIPT_DIR/scripts/lib/common_functions.zsh"
    # Clean database at start to prevent corruption
    clean_database > /dev/null 2>&1
fi

print_step "=== 🔍 BLOCKCHAIN ADDITIONAL ADVANCED FUNCTIONS TEST RUNNER ==="
print_info "Project directory: $(pwd)"
print_info ""

# Check if we're in the correct directory
if [ ! -f "pom.xml" ]; then
    error_exit "pom.xml not found. Make sure to run this script from the project root directory."
fi

print_step "Compiling the project..."
mvn clean compile test-compile -q

if [ $? -ne 0 ]; then
    error_exit "Compilation error. Please review the errors above."
fi

print_success "Compilation successful!"

# Clear any existing database to start fresh
if [ -f "blockchain.db" ]; then
    print_info "💡 Removing existing database for fresh test start..."
    rm blockchain.db
    print_success "Database cleared"
fi

print_info "🧪 Running Additional Advanced Functions tests..."
print_info "Note: 'Error exporting' and 'Import file not found' messages are intentional test cases for error handling"

# Run tests with Maven (suppress error output that's expected in tests)
mvn test -Dtest=BlockchainAdditionalAdvancedFunctionsTest -Djava.util.logging.config.file=src/main/resources/logging.properties

TEST_RESULT=$?

print_info ""
print_step "=== 📊 SUMMARY ==="

if [ $TEST_RESULT -eq 0 ]; then
    print_success "🎉 ALL TESTS PASSED!"
    print_success "The additional advanced functions of the blockchain work correctly."
else
    print_error "❌ SOME TESTS FAILED."
    print_info "Review the results above to see error details."
fi

print_info ""
print_info "Test location: src/test/java/com/rbatllet/blockchain/core/"
print_info "Documentation: README.md"

exit $TEST_RESULT
