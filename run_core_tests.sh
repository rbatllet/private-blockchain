#!/bin/bash

# Script to run Additional Advanced Functions tests for the Blockchain
# Usage: ./run_core_tests.sh

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

echo "🧪 Running Additional Advanced Functions tests..."
echo

# Run tests with Maven
mvn test -Dtest=BlockchainAdditionalAdvancedFunctionsTest

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
