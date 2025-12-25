#!/usr/bin/env zsh

# Basic Blockchain Demo Script
# Demonstrates core blockchain functionality and key management
# Version: 1.0.0

# Set script directory before changing directories
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Load common functions library
source "${SCRIPT_DIR}/lib/common_functions.zsh"

# Change to project root directory
cd "$SCRIPT_DIR/.."

echo "🔗 BASIC BLOCKCHAIN FUNCTIONALITY DEMO"
echo "======================================"
echo ""

# Functions are already loaded from common_functions.zsh

# Function to run the demo
run_demo() {
    print_step "Starting Blockchain Demo..."
    print_info "This demonstration covers:"
    print_info "  • Genesis block creation"
    print_info "  • Key generation and authorization"
    print_info "  • Block creation and validation"
    print_info "  • Chain integrity verification"
    print_info "  • Search functionality"
    print_info "  • Export/Import operations"
    echo ""
    
    print_info "🚀 Launching BlockchainDemo..."
    java -cp "target/classes:$(mvn -q dependency:build-classpath -Dmdep.outputFile=/dev/stdout)" \
        demo.BlockchainDemo
    
    if [ $? -eq 0 ]; then
        print_success "Blockchain demo completed successfully!"
    else
        print_error "Demo execution failed"
        exit 1
    fi
}

# Main execution
print_info "🏠 Project directory: $(pwd)"

# Check if we're in the correct directory
if [[ ! -f "pom.xml" ]]; then
    print_error "This script must be run from the project root directory"
    print_info "Current directory: $(pwd)"
    exit 1
fi

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

# Ensure genesis admin keys exist (auto-generates if missing)
ensure_genesis_keys

# Compile project
if ! compile_project; then
    exit 1
fi

print_separator

# Run the demo
run_demo

print_separator

# Display next steps
print_info "Next steps:"
echo "  1. Run 'scripts/run_crypto_security_demo.zsh' for encryption demonstrations"
echo "  2. Run 'scripts/run_advanced_search_demo.zsh' for search capabilities"
echo "  3. Run 'scripts/run_core_functions_demo.zsh' for comprehensive testing"
echo ""
print_success "Thank you for exploring Private Blockchain!"
