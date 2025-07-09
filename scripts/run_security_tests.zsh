#!/usr/bin/env zsh

# Script to run Key Deletion Security tests for the Blockchain
# Usage: ./run_security_tests.zsh
# Version: 1.0.1

# Load shared functions for database cleanup

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

print_step "=== 🔐 BLOCKCHAIN KEY DELETION SECURITY TEST RUNNER ==="
print_info "Project directory: $(pwd)"
print_info ""

# Check if we're in the correct directory
if [ ! -f "pom.xml" ]; then
    error_exit "pom.xml not found. Make sure to run this script from the project root directory."
fi

# Clean and compile
print_step "Compiling project..."
mvn clean compile -q > /dev/null 2>&1
if [ $? -ne 0 ]; then
    mvn clean compile
    error_exit "Compilation failed!"
fi
print_success "Compilation successful"
print_info ""

# Run Key Deletion Security Tests
print_info "🔐 Running Key Deletion Security Tests..."
print_step "================================================="
print_info ""
mvn test -Dtest=DangerousDeleteAuthorizedKeyTest
SECURITY_TEST_RESULT=$?
print_info ""

# Run the interactive demo
print_info "🎬 Running Key Deletion Security Demo..."
print_step "========================================="
print_info ""
mvn compile exec:java -Dexec.mainClass="demo.DangerousDeleteDemo" -q
DEMO_RESULT=$?
print_info ""

# Summary
print_step "📊 SECURITY TEST SUMMARY"
print_step "========================="
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
if command -v clean_database &> /dev/null; then
    clean_database > /dev/null 2>&1
fi

# Exit with appropriate code
if [ $SECURITY_TEST_RESULT -eq 0 ] && [ $DEMO_RESULT -eq 0 ]; then
    print_success "All security tests completed successfully!"
    exit 0
else
    error_exit "Some security tests failed. Check the output above."
fi
