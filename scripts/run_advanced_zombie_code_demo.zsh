#!/usr/bin/env zsh

# Advanced Zombie Code Demo Script
# Demonstrates the absence of zombie code and clean architecture
# Version: 1.0.0

echo "🧟 ADVANCED ZOMBIE CODE ANALYSIS DEMO"
echo "====================================="
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

print_zombie() {
    echo "\033[1;92m🧹 $1\033[0m"
}

# Function to run the demo
run_demo() {
    print_step "Starting Advanced Zombie Code Analysis Demo..."
    print_info "This demonstration proves:"
    print_zombie "  • No unused Zero Knowledge classes or methods"
    print_zombie "  • Clean two-layer architecture (Public + Private)"
    print_zombie "  • No redundant search implementations"
    print_zombie "  • No misleading API methods"
    print_zombie "  • Streamlined metadata management"
    print_zombie "  • Content-agnostic search system"
    echo ""
    
    print_warning "This demo validates the clean codebase architecture"
    echo ""
    
    print_status "🚀 Launching AdvancedZombieCodeDemo..."
    mvn exec:java -Dexec.mainClass="demo.AdvancedZombieCodeDemo" -q
    
    if [ $? -eq 0 ]; then
        print_success "Zombie code analysis completed successfully!"
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

# Clean environment completely
print_zombie "Performing deep cleanup before demo..."
cleanup_database

# Compile project
if ! compile_project; then
    exit 1
fi

print_separator

# Run the demo
run_demo

print_separator

# Cleanup summary
print_zombie "Code Cleanup Achievements:"
echo "  • Removed all Zero Knowledge layer code"
echo "  • Eliminated SearchTermSuggester automatic categorization"
echo "  • Removed misleading storeSecret* methods"
echo "  • Simplified search API wrapper methods"
echo "  • Cleaned up redundant metadata caches"
echo "  • Removed all hardcoded content categories"
echo ""

# Display next steps
print_info "Clean Architecture Benefits:"
echo "  • Faster compilation and execution"
echo "  • Clearer API without confusion"
echo "  • Easier maintenance and debugging"
echo "  • Better performance without overhead"
echo ""
print_success "Clean codebase demonstration complete!"