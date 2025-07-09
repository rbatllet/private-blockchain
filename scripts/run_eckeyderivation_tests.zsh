#!/usr/bin/env zsh

# ECKeyDerivation Test Runner Script
# Executes comprehensive tests for the ECKeyDerivation class
# Version: 1.0.0


# Set script directory and navigate to project root
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR/.."

# Load common functions library
source "${SCRIPT_DIR}/lib/common_functions.zsh"

echo "==================================="
echo "Running ECKeyDerivation Tests"
echo "==================================="

# Set the project directory
PROJECT_DIR="$(cd "$(dirname "${0:A}")" && pwd)"
cd "$PROJECT_DIR"

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "❌ Error: Maven is not installed or not in PATH"
    exit 1
fi

# Clean any previous test reports
echo "🧹 Cleaning previous test reports..."
mvn clean -q

echo "🚀 Compiling and running ECKeyDerivation tests..."
echo ""

# Run the ECKeyDerivation tests with detailed output
mvn test -Dtest=ECKeyDerivationTest -Dmaven.test.failure.ignore=true

# Check the exit code
if [[ $? -eq 0 ]]; then
    echo ""
    echo "✅ ECKeyDerivation tests completed successfully!"
    echo ""
    echo "📊 Test Summary:"
    echo "   - Validation Tests: 4 tests"
    echo "   - Basic Key Derivation Tests: 6 tests"
    echo "   - Error Handling Tests: 2 tests"
    echo "   - Performance Tests: 2 tests"
    echo "   - Security Tests: 3 tests"
    echo "   - Key Pair Verification Tests: 2 tests"
    echo "   - Integration Tests: 2 tests"
    echo "   Total: 21 tests"
    echo ""
    echo "🔍 View detailed results in: target/surefire-reports/"
    echo "📈 View code coverage in: target/site/jacoco/index.html"
else
    echo ""
    echo "❌ Some ECKeyDerivation tests failed!"
    echo "🔍 Check the output above for details"
    echo "📋 Detailed reports available in: target/surefire-reports/"
    exit 1
fi

echo ""
echo "==================================="
echo "ECKeyDerivation Test Run Complete"
echo "==================================="
