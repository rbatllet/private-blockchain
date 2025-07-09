#!/usr/bin/env zsh

# Off-Chain Validation Comprehensive Test
# Version: 1.0.0

# Set script directory and navigate to project root
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR/.."

# Load common functions library
source "${SCRIPT_DIR}/lib/common_functions.zsh"

print_header "OFF-CHAIN VALIDATION COMPREHENSIVE TEST"

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

print_step "📝 Running comprehensive off-chain validation test..."

# Run the test using Maven
print_step "🚀 Running validation tests..."
mvn -q exec:java -Dexec.mainClass="demo.TestOffChainValidation"

print_separator

print_header "FINAL VALIDATION"

# Check if any test artifacts remain
if [[ -d "off-chain-data" ]]; then
    local file_count=$(find off-chain-data -type f | wc -l)
    print_info "📁 Off-chain files remaining: $file_count"
    if [[ $file_count -eq 0 ]]; then
        print_success "Off-chain directory properly cleaned"
    else
        print_error "Some test files remain"
        ls -la off-chain-data/
    fi
else
    print_success "Off-chain directory properly cleaned up"
fi

print_success "OFF-CHAIN VALIDATION TEST COMPLETE"

# Optional: Clean up test files after completion
if [[ "${KEEP_TEST_FILES:-false}" != "true" ]]; then
    print_info "🧹 Cleaning up test files..."
    cleanup_database
    print_success "Test environment cleaned"
else
    print_info "💾 Test files preserved (KEEP_TEST_FILES=true)"
fi