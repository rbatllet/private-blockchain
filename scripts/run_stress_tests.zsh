#!/usr/bin/env zsh

# Stress Tests Execution Script
# Validates thread safety improvements in blockchain components
# Version: 1.0.0

# Set script directory and navigate to project root
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Load common functions library
source "${SCRIPT_DIR}/lib/common_functions.zsh"

# Change to project root directory
cd "$SCRIPT_DIR/.."

print_header "🧪 BLOCKCHAIN THREAD SAFETY STRESS TESTS"
print_info "Validates AtomicReference improvements and read lock synchronization"
print_info "Tests concurrent operations under high contention scenarios"

# Check if we're in the correct directory
check_project_directory

# Check prerequisites
if ! check_java || ! check_maven; then
    exit 1
fi

print_separator

print_step "🔐 Running UserFriendlyEncryptionAPI stress tests..."
print_info "• Testing AtomicReference thread safety for defaultKeyPair and defaultUsername"
print_info "• Validating concurrent credential changes and operations"
print_info "• Verifying blockchain operations under credential changes"

mvn test -Dtest="UserFriendlyEncryptionAPIStressTest" -q

if [ $? -eq 0 ]; then
    print_success "✅ UserFriendlyEncryptionAPI stress tests passed!"
else
    print_error "❌ UserFriendlyEncryptionAPI stress tests failed!"
    exit 1
fi

print_separator

print_step "🔍 Running Blockchain search initialization stress tests..."
print_info "• Testing read lock synchronization in initializeAdvancedSearch()"
print_info "• Validating concurrent search initialization calls"
print_info "• Verifying mixed operations with block creation"

mvn test -Dtest="BlockchainSearchInitializationStressTest" -q

if [ $? -eq 0 ]; then
    print_success "✅ Blockchain search initialization stress tests passed!"
else
    print_error "❌ Blockchain search initialization stress tests failed!"
    exit 1
fi

print_separator

print_step "📊 Running comprehensive thread safety validation..."
print_info "• Executing all existing thread safety tests"
print_info "• Ensuring no regressions in thread safety improvements"

mvn test -Dtest="*ThreadSafety*Test" -q

if [ $? -eq 0 ]; then
    print_success "✅ All thread safety tests passed!"
else
    print_error "❌ Some thread safety tests failed!"
    exit 1
fi

print_separator

print_success "🎉 ALL STRESS TESTS COMPLETED SUCCESSFULLY!"
print_info "Thread safety improvements validated under high-contention scenarios:"
print_info "• AtomicReference implementation for UserFriendlyEncryptionAPI ✅"
print_info "• Read lock synchronization for Blockchain.initializeAdvancedSearch() ✅"
print_info "• Concurrent operations integrity maintained ✅"
print_info "• No regressions in existing thread safety ✅"

print_separator
print_info "📈 Performance characteristics validated:"
print_info "• High-contention scenarios handled gracefully"
print_info "• No deadlocks or race conditions detected"
print_info "• Blockchain integrity preserved under stress"
print_info "• Credential management remains atomic and consistent"