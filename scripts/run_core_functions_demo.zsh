#!/usr/bin/env zsh

# Core Functions Demo Script
# Comprehensive demonstration of all blockchain core functionality
# Version: 1.0.0

# Set script directory before changing directories
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Load common functions library
source "${SCRIPT_DIR}/lib/common_functions.zsh"

# Change to project root directory
cd "$SCRIPT_DIR/.."

echo "💎 COMPREHENSIVE BLOCKCHAIN FUNCTIONS DEMO"
echo "=========================================="
echo ""

# Functions are already loaded from common_functions.zsh

# Function to run the demo
run_demo() {
    print_step "Starting Core Functions Demo..."
    print_info "This comprehensive demo covers:"
    print_info "  • Complete blockchain initialization with genesis block"
    print_info "  • Advanced key management and authorization"
    print_info "  • All block operations (add, validate, retrieve)"
    print_info "  • Chain integrity validation and detailed checking"
    print_info "  • Core blockchain functionality demonstration"
    print_info "  • Authorized key management and validation"
    print_info "  • Block data integrity and chain validation"
    echo ""
    
    print_warning "This is a comprehensive test - it may take a few minutes"
    echo ""
    
    print_info "🚀 Launching CoreFunctionsDemo..."
    java -cp "target/classes:$(mvn -q dependency:build-classpath -Dmdep.outputFile=/dev/stdout)" \
        demo.CoreFunctionsDemo
    
    if [ $? -eq 0 ]; then
        print_success "Core functions demo completed successfully!"
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

# Compile project
if ! compile_project; then
    exit 1
fi

print_separator

# Run the demo
run_demo

print_separator

# Display next steps
print_info "Additional demos available:"
echo "  1. Run 'scripts/test_thread_safety_simple.zsh' for thread safety testing"
echo "  2. Run 'scripts/test_export_import.zsh' for export/import testing"
echo "  3. Check the 'logs/' directory for detailed execution logs"
echo ""
print_success "Comprehensive testing complete!"
