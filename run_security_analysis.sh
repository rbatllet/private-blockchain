#!/bin/bash

# Advanced Security Test Runner with detailed analysis
# Usage: ./run_security_analysis.sh

# Load shared functions for database cleanup
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [ -f "$SCRIPT_DIR/scripts/shared-functions.sh" ]; then
    source "$SCRIPT_DIR/scripts/shared-functions.sh"
    clean_database > /dev/null 2>&1
fi

echo "=== 🛡️  ADVANCED BLOCKCHAIN SECURITY ANALYSIS ==="
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
    exit 1
fi
echo "✅ Compilation successful"
echo

# Test Categories
TESTS_PASSED=0
TESTS_FAILED=0

run_test_category() {
    local test_name="$1"
    local test_class="$2"
    local description="$3"
    
    echo "🧪 Testing: $test_name"
    echo "📋 Description: $description"
    echo "⚙️  Running: $test_class"
    echo "----------------------------------------"
    
    mvn test -Dtest="$test_class" -q
    local result=$?
    
    if [ $result -eq 0 ]; then
        echo "✅ $test_name: PASSED"
        ((TESTS_PASSED++))
    else
        echo "❌ $test_name: FAILED"
        ((TESTS_FAILED++))
    fi
    echo
}

# Run different test categories
echo "🔐 SECURITY TEST CATEGORIES"
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
echo "🎬 SECURITY DEMONSTRATION ANALYSIS"
echo "==================================="
echo "Running comprehensive security demo..."
echo
mvn compile exec:java -Dexec.mainClass="com.rbatllet.blockchain.demo.DangerousDeleteDemo" -q
DEMO_RESULT=$?

if [ $DEMO_RESULT -eq 0 ]; then
    echo "✅ Security Demo: COMPLETED"
    ((TESTS_PASSED++))
else
    echo "❌ Security Demo: FAILED"
    ((TESTS_FAILED++))
fi
echo

# Security Analysis Summary
echo "🛡️  SECURITY ANALYSIS SUMMARY"
echo "=============================="
echo "Tests Passed: $TESTS_PASSED"
echo "Tests Failed: $TESTS_FAILED"
echo "Total Tests: $((TESTS_PASSED + TESTS_FAILED))"
echo

if [ $TESTS_FAILED -eq 0 ]; then
    echo "🎉 ALL SECURITY TESTS PASSED!"
    echo
    echo "🔐 Security Features Verified:"
    echo "  ✅ Multi-layered key deletion protection"
    echo "  ✅ Impact analysis before dangerous operations"
    echo "  ✅ Safe deletion with automatic blocking"
    echo "  ✅ Dangerous deletion with audit trails"
    echo "  ✅ Blockchain integrity protection"
    echo "  ✅ Temporal validation consistency"
    echo "  ✅ Database-level security operations"
    echo
    echo "🏆 Your blockchain implementation has enterprise-grade security!"
else
    echo "⚠️  SECURITY ISSUES DETECTED"
    echo "Failed tests: $TESTS_FAILED"
    echo "Please review the test output above and fix any issues."
fi

# Cleanup
if command -v clean_database &> /dev/null; then
    clean_database > /dev/null 2>&1
fi

# Exit with appropriate code
exit $TESTS_FAILED
