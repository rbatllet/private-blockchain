#!/bin/bash

# Script to run Key Deletion Security tests for the Blockchain
# Usage: ./run_security_tests.sh

# Load shared functions for database cleanup
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [ -f "$SCRIPT_DIR/scripts/shared-functions.sh" ]; then
    source "$SCRIPT_DIR/scripts/shared-functions.sh"
    # Clean database at start to prevent corruption
    clean_database > /dev/null 2>&1
fi

echo "=== 🔐 BLOCKCHAIN KEY DELETION SECURITY TEST RUNNER ==="
echo "Project directory: $(pwd)"
echo

# Check if we're in the correct directory
if [ ! -f "pom.xml" ]; then
    echo "❌ Error: pom.xml not found. Make sure to run this script from the project root directory."
    exit 1
fi

# Clean and compile
echo "📦 Compiling project..."
mvn clean compile -q > /dev/null 2>&1
if [ $? -ne 0 ]; then
    echo "❌ Compilation failed!"
    mvn clean compile
    exit 1
fi
echo "✅ Compilation successful"
echo

# Run Key Deletion Security Tests
echo "🔐 Running Key Deletion Security Tests..."
echo "================================================="
echo
mvn test -Dtest=DangerousDeleteAuthorizedKeyTest
SECURITY_TEST_RESULT=$?
echo

# Run the interactive demo
echo "🎬 Running Key Deletion Security Demo..."
echo "========================================="
echo
mvn compile exec:java -Dexec.mainClass="com.rbatllet.blockchain.demo.DangerousDeleteDemo" -q
DEMO_RESULT=$?
echo

# Summary
echo "📊 SECURITY TEST SUMMARY"
echo "========================="
if [ $SECURITY_TEST_RESULT -eq 0 ]; then
    echo "✅ Key Deletion Security Tests: PASSED"
else
    echo "❌ Key Deletion Security Tests: FAILED"
fi

if [ $DEMO_RESULT -eq 0 ]; then
    echo "✅ Security Demo: COMPLETED"
else
    echo "❌ Security Demo: FAILED"
fi

echo
echo "🔍 Security Features Tested:"
echo "  • Impact analysis (canDeleteAuthorizedKey)"
echo "  • Safe deletion (deleteAuthorizedKey)"
echo "  • Dangerous deletion with safety (dangerouslyDeleteAuthorizedKey)"
echo "  • Forced deletion with blockchain corruption"
echo "  • Multi-level protection scenarios"
echo "  • Comprehensive audit logging"
echo

# Final cleanup
if command -v clean_database &> /dev/null; then
    clean_database > /dev/null 2>&1
fi

# Exit with appropriate code
if [ $SECURITY_TEST_RESULT -eq 0 ] && [ $DEMO_RESULT -eq 0 ]; then
    echo "🎉 All security tests completed successfully!"
    exit 0
else
    echo "⚠️  Some security tests failed. Check the output above."
    exit 1
fi
