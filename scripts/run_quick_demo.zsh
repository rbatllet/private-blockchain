#!/usr/bin/env zsh

# Quick Demo Script
# Fast demonstration of basic blockchain functionality
# Version: 1.0.0

echo "⚡ QUICK BLOCKCHAIN DEMO"
echo "========================"
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

# Function to run the demo
run_demo() {
    print_info "Running quick blockchain test..."
    
    mvn exec:java -Dexec.mainClass="demo.QuickDemo" -q
    
    if [ $? -eq 0 ]; then
        print_success "Quick demo completed!"
    else
        print_error "Demo failed"
        exit 1
    fi
}

# Main execution
print_info "🏠 Project directory: $(pwd)"

# Check if we're in the correct directory
if [[ ! -f "pom.xml" ]]; then
    print_error "This script must be run from the project root directory"
    exit 1
fi

# Check prerequisites
if ! check_java || ! check_maven; then
    exit 1
fi

# Clean, compile and run
cleanup_database

if ! compile_project; then
    exit 1
fi

print_separator
run_demo
print_separator

print_success "Quick test complete! Run other demos for more features."