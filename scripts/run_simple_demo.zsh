#!/usr/bin/env zsh

# Simple Demo Script
# Quick demonstration of basic blockchain operations
# Version: 1.0.0


# Set script directory before changing directories
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Load common functions library
source "${SCRIPT_DIR}/lib/common_functions.zsh"

# Change to project root directory
cd "$SCRIPT_DIR/.."

echo "⚡ SIMPLE BLOCKCHAIN DEMO"
echo "========================"
echo ""

# Function to run the demo
run_demo() {
    print_step "Starting Simple Demo..."
    print_info "This is a quick demonstration of:"
    print_info "  • Basic blockchain initialization"
    print_info "  • Simple block creation"
    print_info "  • Quick validation checks"
    echo ""
    
    print_step "🚀 Launching SimpleDemo..."
    java -cp "target/classes:$(mvn -q dependency:build-classpath -Dmdep.outputFile=/dev/stdout)" \
        demo.SimpleDemo
    
    if [ $? -eq 0 ]; then
        print_success "Simple demo completed successfully!"
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
print_step "🔍 Checking prerequisites..."

if ! check_java; then
    exit 1
fi

if ! check_maven; then
    exit 1
fi

print_success "All prerequisites satisfied"

# Clean and compile
cleanup_database

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
echo "  1. Run 'scripts/run_blockchain_demo.zsh' for complete blockchain features"
echo "  2. Run 'scripts/run_crypto_security_demo.zsh' for encryption demonstrations"
echo "  3. Run 'scripts/run_advanced_search_demo.zsh' for search capabilities"
echo ""
print_success "Thank you for exploring Private Blockchain!"
