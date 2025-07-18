#!/usr/bin/env zsh

# Script to run Cryptographic Security Demo for the Blockchain
# Usage: ./run_crypto_security_demo.zsh
# Version: 1.0.0

# Set script directory before changing directories
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Load common functions library
source "${SCRIPT_DIR}/lib/common_functions.zsh"

# Change to project root directory
cd "$SCRIPT_DIR/.."

echo "🔐 BLOCKCHAIN CRYPTOGRAPHIC SECURITY DEMO"
echo "=========================================="
echo ""

# Check if we're in the correct directory
if [ ! -f "pom.xml" ]; then
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
print_info "🚀 Launching CryptoSecurityDemo..."
java -cp "target/classes:$(mvn -q dependency:build-classpath -Dmdep.outputFile=/dev/stdout)" \
    demo.CryptoSecurityDemo
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

print_separator

# Display next steps
print_info "Next steps:"
echo "  1. Run 'scripts/run_advanced_thread_safety_tests.zsh' for thread safety testing"
echo "  2. Run 'scripts/run_blockchain_demo.zsh' for basic blockchain operations"
echo "  3. Check the 'logs/' directory for detailed execution logs"
echo ""

# Final cleanup
cleanup_database > /dev/null 2>&1

# Exit with appropriate code
if [ $SECURITY_TEST_RESULT -eq 0 ] && [ $DEMO_RESULT -eq 0 ]; then
    print_success "All cryptographic security tests and demos completed successfully!"
    exit 0
else
    print_error "Some cryptographic security tests or demos failed. Check the output above."
    exit 1
fi
