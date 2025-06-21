#!/usr/bin/env zsh

# Script to run Cryptographic Security Demo for the Blockchain
# Usage: ./run_crypto_security_demo.sh
# Version: 1.0.0

# Load shared functions for database cleanup
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
if [ -f "$SCRIPT_DIR/scripts/shared-functions.sh" ]; then
    source "$SCRIPT_DIR/scripts/shared-functions.sh"
    # Clean database at start to prevent corruption
    clean_database > /dev/null 2>&1
fi

print_step "=== 🔐 BLOCKCHAIN CRYPTOGRAPHIC SECURITY DEMO RUNNER ==="
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

# Run the Cryptographic Security Tests
print_info "🔐 Running Cryptographic Security Tests..."
print_step "================================================="
print_info ""
mvn test -Dtest=com.rbatllet.blockchain.util.CryptoUtilTest
SECURITY_TEST_RESULT=$?
print_info ""

# Run the Cryptographic Security Demo
print_info "🎬 Running Cryptographic Security Demo..."
print_step "========================================="
print_info ""
mvn compile exec:java -Dexec.mainClass="demo.CryptoSecurityDemo" -q
DEMO_RESULT=$?
print_info ""

# Summary
print_step "📊 CRYPTOGRAPHIC SECURITY SUMMARY"
print_step "================================="
if [ $SECURITY_TEST_RESULT -eq 0 ]; then
    print_success "✅ Cryptographic Security Tests: PASSED"
else
    print_error "❌ Cryptographic Security Tests: FAILED"
fi

if [ $DEMO_RESULT -eq 0 ]; then
    print_success "✅ Cryptographic Security Demo: COMPLETED"
else
    print_error "❌ Cryptographic Security Demo: FAILED"
fi

print_info ""
print_info "🔍 Security Features Demonstrated:"
print_info "  • 🔗 SHA-3 hashing (replacing SHA-256)"
print_info "  • 🔐 ECDSA signatures (replacing RSA)"
print_info "  • 🔑 Hierarchical key management system"
print_info "  • 🔄 Key rotation capabilities"
print_info "  • ❌ Key revocation with cascade option"
print_info ""

# Final cleanup
if command -v clean_database &> /dev/null; then
    clean_database > /dev/null 2>&1
fi

# Exit with appropriate code
if [ $SECURITY_TEST_RESULT -eq 0 ] && [ $DEMO_RESULT -eq 0 ]; then
    print_success "All cryptographic security tests and demos completed successfully!"
    exit 0
else
    error_exit "Some cryptographic security tests or demos failed. Check the output above."
fi
