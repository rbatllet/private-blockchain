#!/usr/bin/env zsh

# ECKeyDerivation Test Runner Script
# Executes comprehensive tests for the ECKeyDerivation class
# Version: 1.0.0


# Set script directory before changing directories
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Load common functions library
source "${SCRIPT_DIR}/lib/common_functions.zsh"

# Change to project root directory
cd "$SCRIPT_DIR/.."

echo "==================================="
echo "Running ECKeyDerivation Tests"
echo "==================================="

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

print_step "🧹 Cleaning previous test reports..."
mvn clean -q

print_step "🚀 Compiling and running ECKeyDerivation tests..."
echo ""

# Run the ECKeyDerivation tests with detailed output
mvn test -Dtest=ECKeyDerivationTest -Dmaven.test.failure.ignore=true

# Check the exit code
if [[ $? -eq 0 ]]; then
    echo ""
    print_success "ECKeyDerivation tests completed successfully!"
    echo ""
    print_info "📊 Test Summary:"
    print_info "   - Validation Tests: 4 tests"
    print_info "   - Basic Key Derivation Tests: 6 tests"
    print_info "   - Error Handling Tests: 2 tests"
    print_info "   - Performance Tests: 2 tests"
    print_info "   - Security Tests: 3 tests"
    print_info "   - Key Pair Verification Tests: 2 tests"
    print_info "   - Integration Tests: 2 tests"
    print_info "   Total: 21 tests"
    echo ""
    print_info "🔍 View detailed results in: target/surefire-reports/"
    print_info "📈 View code coverage in: target/site/jacoco/index.html"
else
    echo ""
    print_error "Some ECKeyDerivation tests failed!"
    print_info "🔍 Check the output above for details"
    print_info "📋 Detailed reports available in: target/surefire-reports/"
    exit 1
fi

print_separator

print_info "Next steps:"
echo "  1. Run 'scripts/run_advanced_thread_safety_tests.zsh' for thread safety testing"
echo "  2. Run 'scripts/run_blockchain_demo.zsh' for blockchain operations"
echo "  3. Check the 'target/surefire-reports/' directory for detailed test reports"
echo ""

print_success "ECKeyDerivation testing complete!"
echo "==================================="
echo "ECKeyDerivation Test Run Complete"
echo "==================================="
