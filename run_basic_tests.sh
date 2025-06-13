#!/bin/bash

# Script to run Basic Core Functions tests for the Blockchain
# Usage: ./run_basic_tests.sh

# Load shared functions for database cleanup (but preserve original structure)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [ -f "$SCRIPT_DIR/scripts/shared-functions.sh" ]; then
    source "$SCRIPT_DIR/scripts/shared-functions.sh"
    # Clean database at start to prevent corruption
    clean_database > /dev/null 2>&1
fi

echo "=== BLOCKCHAIN BASIC CORE FUNCTIONS TEST RUNNER ==="
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

echo "🧪 Running Basic Core Functions test..."
echo

# Run basic core functions test (suppress most logs)
mvn exec:java -Dexec.mainClass="com.rbatllet.blockchain.CoreFunctionsTest" \
  -Djava.util.logging.config.file=src/main/resources/logging.properties \
  -Dhibernate.show_sql=false \
  -Dorg.slf4j.simpleLogger.defaultLogLevel=warn \
  -q 2>/dev/null || mvn exec:java -Dexec.mainClass="com.rbatllet.blockchain.CoreFunctionsTest" -q

TEST_RESULT=$?

echo
echo "=== SUMMARY ==="

if [ $TEST_RESULT -eq 0 ]; then
    echo "🎉 BASIC CORE FUNCTIONS TEST PASSED!"
    echo "✅ The basic core functions of the blockchain work correctly."
else
    echo "❌ BASIC CORE FUNCTIONS TEST FAILED."
    echo "📝 Review the results above to see error details."
fi

echo
echo "📍 Test location: src/main/java/com/rbatllet/blockchain/CoreFunctionsTest.java"
echo "📖 Documentation: README.md"

exit $TEST_RESULT
