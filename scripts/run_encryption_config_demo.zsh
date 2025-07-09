#!/usr/bin/env zsh

# Script to run the Encryption Configuration Demo
# Usage: ./run_encryption_config_demo.zsh
# Version: 1.0.0

# Set script directory and navigate to project root
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR/.."

# Load common functions library
source "${SCRIPT_DIR}/lib/common_functions.zsh"

print_header "🔐 ENCRYPTION CONFIGURATION DEMO RUNNER"
print_info "Project directory: $(pwd)"
print_info ""

# Check if we're in the correct directory
check_project_directory

# Check prerequisites
if ! check_java || ! check_maven; then
    exit 1
fi

# Clean and compile
cleanup_database

if ! compile_project; then
    exit 1
fi

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

mvn exec:java -Dexec.mainClass="demo.EncryptionConfigDemo" -q
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
    print_info "   • Review docs/ENCRYPTION_GUIDE.md for detailed documentation"
    print_info "   • Check encryption tests with 'mvn test -Dtest=*Encryption*'"
    print_info "   • Explore advanced features in BlockDataEncryptionService"
else
    error_exit "Some tests or demos failed. Check the output above for details."
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

# Final cleanup
if command -v clean_database &> /dev/null; then
    clean_database > /dev/null 2>&1
fi

print_success "🎉 Encryption Configuration Demo completed successfully!"
exit 0