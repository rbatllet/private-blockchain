#!/usr/bin/env zsh

# Core Functions Demo Script
# Comprehensive demonstration of all blockchain core functionality
# Version: 1.0.0

echo "💎 COMPREHENSIVE BLOCKCHAIN FUNCTIONS DEMO"
echo "=========================================="
echo ""

# Set script directory and navigate to project root
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR/.."

# Load common functions library
source "${SCRIPT_DIR}/lib/common_functions.zsh"

# Function to print colored output (specific to this script)
print_status() {
    echo "\033[1;34m$1\033[0m"
}

print_success() {
    echo "\033[1;32m✅ $1\033[0m"
}

print_error() {
    echo "\033[1;31m❌ $1\033[0m"
}

print_info() {
    echo "\033[1;36mℹ️  $1\033[0m"
}

print_step() {
    echo "\033[1;35m📋 $1\033[0m"
}

print_warning() {
    echo "\033[1;33m⚠️  $1\033[0m"
}

# Function to run the demo
run_demo() {
    print_step "Starting Core Functions Demo..."
    print_info "This comprehensive demo covers:"
    print_info "  • Complete blockchain initialization"
    print_info "  • Advanced key management"
    print_info "  • All block operations (add, validate, search)"
    print_info "  • Chain integrity and recovery"
    print_info "  • Export/Import functionality"
    print_info "  • Thread safety demonstrations"
    print_info "  • Performance metrics"
    echo ""
    
    print_warning "This is a comprehensive test - it may take a few minutes"
    echo ""
    
    print_status "🚀 Launching CoreFunctionsDemo..."
    mvn exec:java -Dexec.mainClass="demo.CoreFunctionsDemo" -q
    
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
print_status "🔍 Checking prerequisites..."

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