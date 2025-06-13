#!/bin/bash

# Script to run Additional Advanced Functions tests for the Blockchain
# Usage: ./run_advanced_tests.sh

# Load shared functions for database cleanup (but preserve original structure)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [ -f "$SCRIPT_DIR/scripts/shared-functions.sh" ]; then
    source "$SCRIPT_DIR/scripts/shared-functions.sh"
    # Clean database at start to prevent corruption
    clean_database > /dev/null 2>&1
fi

echo "=== BLOCKCHAIN ADDITIONAL ADVANCED FUNCTIONS TEST RUNNER ==="
echo "Project directory: $(pwd)"
echo

# Check if we're in the correct directory
if [ ! -f "pom.xml" ]; then
    echo "❌ Error: pom.xml not found. Make sure to run this script from the project root directory."
    exit 1
fi

echo "📦 Compiling the project..."
mvn clean compile test-compile -q

if [ $? -ne 0 ]; then
    echo "❌ Compilation error. Please review the errors above."
    exit 1
fi

echo "✅ Compilation successful!"
echo

# Clear any existing database to start fresh
if [ -f "blockchain.db" ]; then
    echo "🗑️ Removing existing database for fresh test start..."
    rm blockchain.db
    echo "✅ Database cleared"
fi
echo

echo "🧪 Running Additional Advanced Functions tests..."
echo "ℹ️  Note: 'Error exporting' and 'Import file not found' messages are intentional test cases for error handling"
echo

# Run tests with Maven (suppress error output that's expected in tests)
mvn test -Dtest=BlockchainAdditionalAdvancedFunctionsTest -Djava.util.logging.config.file=src/main/resources/logging.properties

TEST_RESULT=$?

echo
echo "=== SUMMARY ==="

if [ $TEST_RESULT -eq 0 ]; then
    echo "🎉 ALL TESTS PASSED!"
    echo "✅ The additional advanced functions of the blockchain work correctly."
else
    echo "❌ SOME TESTS FAILED."
    echo "📝 Review the results above to see error details."
fi

echo
echo "📍 Test location: src/test/java/com/rbatllet/blockchain/core/"
echo "📖 Documentation: README.md"

exit $TEST_RESULT
