#!/usr/bin/env zsh

# Quick Demo Script
# Fast demonstration of basic blockchain functionality
# Version: 1.0.0


# Set script directory before changing directories
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Load common functions library
source "${SCRIPT_DIR}/lib/common_functions.zsh"

# Change to project root directory
cd "$SCRIPT_DIR/.."

echo "⚡ QUICK BLOCKCHAIN DEMO"
echo "========================"
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

# Ensure genesis admin keys exist (auto-generates if missing)
ensure_genesis_keys

if ! compile_project; then
    exit 1
fi

print_separator

# Function to run the demo
run_demo() {
    print_info "🚀 Launching QuickDemo..."
    
    java -cp "target/classes:$(mvn -q dependency:build-classpath -Dmdep.outputFile=/dev/stdout)" \
        demo.QuickDemo
    
    if [ $? -eq 0 ]; then
        print_success "Quick demo completed!"
    else
        print_error "Demo failed"
        exit 1
    fi
}

# Run the demo
run_demo

print_separator

# Display next steps
print_info "Next steps:"
echo "  1. Run 'scripts/run_simple_demo.zsh' for more detailed demonstrations"
echo "  2. Run 'scripts/run_blockchain_demo.zsh' for comprehensive blockchain operations"
echo "  3. Check the 'logs/' directory for execution logs"
echo ""

# Final cleanup
cleanup_database > /dev/null 2>&1

# Ensure genesis admin keys exist (auto-generates if missing)
ensure_genesis_keys

print_success "Quick test complete! Run other demos for more features."
