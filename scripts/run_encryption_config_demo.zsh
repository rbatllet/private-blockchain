#!/usr/bin/env zsh

# Script to run the Encryption Configuration Demo
# Usage: ./run_encryption_config_demo.zsh
# Version: 1.0.0

# Set script directory before changing directories
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Load common functions library
source "${SCRIPT_DIR}/lib/common_functions.zsh"

# Change to project root directory
cd "$SCRIPT_DIR/.."

echo "🔐 ENCRYPTION CONFIGURATION DEMO"
echo "================================="
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

# Demo execution
print_step "Running Encryption Configuration Demo..."

# Run the Encryption Configuration Demo
print_info "🎬 Running Encryption Configuration Demo..."
print_step "==========================================="
print_info ""
print_info "This demo showcases different encryption configuration options:"
print_info "• Default configuration (balanced security/performance)"
print_info "• High security configuration (maximum protection)" 
print_info "• Performance configuration (optimized for speed)"
print_info "• Test configuration (fast processing for development)"
print_info "• Custom configuration (builder pattern)"
print_info ""

java -cp "target/classes:$(mvn -q dependency:build-classpath -Dmdep.outputFile=/dev/stdout)" \
    demo.EncryptionConfigDemo
DEMO_RESULT=$?
print_info ""

# Run related tests to validate functionality
print_info "🧪 Running Encryption Configuration Tests..."
print_step "===========================================" 
print_info ""
mvn test -Dtest=EncryptionConfigTest -q
TEST_RESULT=$?
print_info ""

# Summary
print_step "📊 ENCRYPTION CONFIGURATION SUMMARY"
print_step "===================================="

if [ $DEMO_RESULT -eq 0 ]; then
    print_success "✅ Encryption Configuration Demo: COMPLETED"
else
    print_error "❌ Encryption Configuration Demo: FAILED"
fi

if [ $TEST_RESULT -eq 0 ]; then
    print_success "✅ Encryption Configuration Tests: PASSED"
else
    print_error "❌ Encryption Configuration Tests: FAILED"
fi

if [ $DEMO_RESULT -eq 0 ] && [ $TEST_RESULT -eq 0 ]; then
    print_info ""
    print_info "📋 Summary of what was demonstrated:"
    print_info "   • Default encryption configuration (AES-256-GCM, 12-char passwords)"
    print_info "   • High security configuration (256-bit keys, metadata encryption)"
    print_info "   • Performance configuration (optimized for speed)"
    print_info "   • Custom configuration building with validation"
    print_info "   • Password generation matching configuration requirements"
    print_info "   • Configuration comparison and recommendations"
    print_info ""
    print_info "💡 Next steps:"
    print_info "   • Run './run_user_friendly_encryption_demo.zsh' for practical usage"
    print_info "   • Review docs/security/ENCRYPTION_GUIDE.md for detailed documentation"
    print_info "   • Check encryption tests with 'mvn test -Dtest=*Encryption*'"
    print_info "   • Explore advanced features in BlockDataEncryptionService"
else
    print_error "Some tests or demos failed. Check the output above for details."
    exit 1
fi

print_info ""
print_info "🔧 Configuration Options Demonstrated:"
print_info "  • 🔒 Default Config: getDefaultEncryptionConfig()"
print_info "  • 🛡️ High Security: getHighSecurityConfig() - 256-bit, metadata encryption"
print_info "  • ⚡ Performance: getPerformanceConfig() - optimized for speed"
print_info "  • 🧪 Test Config: getTestConfig() - reduced security for testing"
print_info "  • 🔧 Custom Config: createCustomConfig() - builder pattern"
print_info "  • 📊 Comparison: getEncryptionConfigComparison()"
print_info "  • 🔑 Smart Passwords: generatePasswordForConfig()"
print_info "  • 🏪 Configured Storage: storeSecretWithHighSecurity/Performance/CustomConfig()"
print_info ""

print_separator

# Display next steps
print_info "Next steps:"
echo "  1. Run 'scripts/run_user_friendly_encryption_demo.zsh' for practical usage"
echo "  2. Run 'scripts/run_crypto_security_demo.zsh' for security testing"
echo "  3. Check the 'docs/security/ENCRYPTION_GUIDE.md' for detailed documentation"
echo ""

# Final cleanup
cleanup_database > /dev/null 2>&1

print_success "🎉 Encryption Configuration Demo completed successfully!"
exit 0
