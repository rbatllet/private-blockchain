#!/usr/bin/env zsh

# Off-Chain Validation Comprehensive Test
# Version: 1.0.0

# Set script directory before changing directories
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Load common functions library
source "${SCRIPT_DIR}/lib/common_functions.zsh"

# Change to project root directory
cd "$SCRIPT_DIR/.."

echo "✅ OFF-CHAIN VALIDATION COMPREHENSIVE TEST"
echo "==========================================="
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

print_info "📝 Running comprehensive off-chain validation test..."
print_info "🚀 Running validation tests..."
java -cp "target/classes:$(mvn -q dependency:build-classpath -Dmdep.outputFile=/dev/stdout)" \
    demo.TestOffChainValidation

print_separator

print_info "📊 FINAL VALIDATION"
echo "==================="

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
