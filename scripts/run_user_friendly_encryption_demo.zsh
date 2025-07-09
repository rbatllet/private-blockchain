#!/usr/bin/env zsh

# Script to run the User-Friendly Encryption API Demo
# Usage: ./run_user_friendly_encryption_demo.zsh
# Version: 1.0.0

# Set script directory and navigate to project root
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR/.."

# Load common functions library
source "${SCRIPT_DIR}/lib/common_functions.zsh"

print_header "🔐 USER-FRIENDLY ENCRYPTION API DEMO RUNNER"
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

# Run the User-Friendly Encryption API Demo
print_info "🎬 Running User-Friendly Encryption API Demo..."
print_step "==============================================="
print_info ""
print_info "This demo showcases practical encrypted blockchain operations:"
print_info "• Easy storage of different types of encrypted data"
print_info "• Automatic user management and key generation"
print_info "• Privacy-preserving metadata search"
print_info "• Secure retrieval with password protection"
print_info "• Advanced search with decryption capabilities"
print_info "• Unified search across encrypted and public data"
print_info "• Blockchain validation and integrity checking"
print_info ""

mvn exec:java -Dexec.mainClass="demo.UserFriendlyEncryptionDemo" -q
DEMO_RESULT=$?
print_info ""

# Run related tests to validate functionality
print_info "🧪 Running User-Friendly Encryption API Tests..."
print_step "==============================================="
print_info ""
mvn test -Dtest=UserFriendlyEncryptionAPITest -q
TEST_RESULT=$?
print_info ""

# Summary
print_step "📊 USER-FRIENDLY ENCRYPTION API SUMMARY"
print_step "========================================"

if [ $DEMO_RESULT -eq 0 ]; then
    print_success "✅ User-Friendly Encryption API Demo: COMPLETED"
else
    print_error "❌ User-Friendly Encryption API Demo: FAILED"
fi

if [ $TEST_RESULT -eq 0 ]; then
    print_success "✅ User-Friendly Encryption API Tests: PASSED"
else
    print_error "❌ User-Friendly Encryption API Tests: FAILED"
fi

if [ $DEMO_RESULT -eq 0 ] && [ $TEST_RESULT -eq 0 ]; then
    print_info ""
    print_info "📋 Summary of what was demonstrated:"
    print_info "   • Storing medical, financial, and legal encrypted data"
    print_info "   • Generating secure passwords and managing users"
    print_info "   • Privacy-preserving search without content disclosure"
    print_info "   • Password-based content search and retrieval"
    print_info "   • Unified search across mixed blockchain data"
    print_info "   • Comprehensive validation and integrity checking"
    print_info ""
    print_info "💡 Next steps:"
    print_info "   • Run './run_encryption_config_demo.zsh' for configuration options"
    print_info "   • Review docs/ENCRYPTION_GUIDE.md for detailed documentation"
    print_info "   • Explore advanced search with 'scripts/run_revolutionary_search_demo.zsh'"
    print_info "   • Check all encryption tests with 'mvn test -Dtest=*Encryption*'"
else
    error_exit "Some tests or demos failed. Check the output above for details."
fi

print_info ""
print_info "🔍 API Features Demonstrated:"
print_info "  • 🏥 Medical Records: storeMedicalRecord(), findMedicalRecords()"
print_info "  • 💰 Financial Data: storeFinancialRecord(), findFinancialRecords()"
print_info "  • ⚖️ Legal Documents: storeLegalDocument(), findLegalDocuments()"
print_info "  • 🔐 Generic Secrets: storeSecret(), retrieveSecret()"
print_info "  • 🔍 Privacy Search: findEncryptedData(), searchEverything()"
print_info "  • 🔓 Content Search: findAndDecryptData(), searchEverythingWithPassword()"
print_info "  • 👤 User Management: createUser(), setDefaultCredentials()"
print_info "  • 🛡️ Security: generateSecurePassword(), validateEncryptedBlocks()"
print_info ""

# Final cleanup
cleanup_database

print_success "🎉 User-Friendly Encryption API Demo completed successfully!"
exit 0