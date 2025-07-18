#!/usr/bin/env zsh

# Script to run Key Deletion Security tests for the Blockchain
# Usage: ./run_security_tests.zsh
# Version: 1.0.1

# Set script directory before changing directories
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Load common functions library
source "${SCRIPT_DIR}/lib/common_functions.zsh"

# Change to project root directory
cd "$SCRIPT_DIR/.."

echo "🔐 BLOCKCHAIN KEY DELETION SECURITY TEST RUNNER"
echo "================================================"
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

# Run Key Deletion Security Tests
print_info "🔐 Running Key Deletion Security Tests..."
echo "================================================="
echo ""
mvn test -Dtest=DangerousDeleteAuthorizedKeyTest
SECURITY_TEST_RESULT=$?
echo ""

# Run the interactive demo
print_info "🎬 Running Key Deletion Security Demo..."
echo "========================================="
echo ""
java -cp "target/classes:$(mvn -q dependency:build-classpath -Dmdep.outputFile=/dev/stdout)" \
    demo.DangerousDeleteDemo
DEMO_RESULT=$?
echo ""

# Summary
print_info "📊 SECURITY TEST SUMMARY"
echo "========================="
if [ $SECURITY_TEST_RESULT -eq 0 ]; then
    print_success "✅ Key Deletion Security Tests: PASSED"
else
    print_error "❌ Key Deletion Security Tests: FAILED"
fi

if [ $DEMO_RESULT -eq 0 ]; then
    print_success "✅ Security Demo: COMPLETED"
else
    print_error "❌ Security Demo: FAILED"
fi

print_info ""
print_info "🔍 Security Features Tested:"
print_info "  • Impact analysis (canDeleteAuthorizedKey)"
print_info "  • Safe deletion (deleteAuthorizedKey)"
print_info "  • Dangerous deletion with safety (dangerouslyDeleteAuthorizedKey)"
print_info "  • Forced deletion with blockchain corruption"
print_info "  • Multi-level protection scenarios"
print_info "  • Comprehensive audit logging"
print_info ""

# Final cleanup
if command -v cleanup_database &> /dev/null; then
    cleanup_database > /dev/null 2>&1
fi

# Exit with appropriate code
if [ $SECURITY_TEST_RESULT -eq 0 ] && [ $DEMO_RESULT -eq 0 ]; then
    print_success "All security tests completed successfully!"
    exit 0
else
    print_error "Some security tests failed. Check the output above."
    exit 1
fi
