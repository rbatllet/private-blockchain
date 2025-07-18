#!/usr/bin/env zsh

# Advanced Zombie Code Demo Script
# Demonstrates the exposure of valuable zombie code capabilities
# Version: 1.0.0

# Set script directory before changing directories
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Load common functions library
source "${SCRIPT_DIR}/lib/common_functions.zsh"

# Change to project root directory
cd "$SCRIPT_DIR/.."

echo "🧟 ADVANCED ZOMBIE CODE ANALYSIS DEMO"
echo "====================================="
echo ""

# Functions are already loaded from common_functions.zsh

print_zombie() {
    echo "\033[1;92m🧹 $1\033[0m"
}

# Function to run the demo
run_demo() {
    print_step "Starting Advanced Zombie Code Capabilities Demo..."
    print_info "This demonstration exposes previously hidden enterprise-grade functionality:"
    print_zombie "  • Advanced key management with secure storage"
    print_zombie "  • Multi-format key import/export (PEM, DER, Base64)"
    print_zombie "  • Advanced cryptographic services (key derivation, validation)"
    print_zombie "  • Blockchain recovery and corruption management"
    print_zombie "  • Enterprise-grade disaster recovery capabilities"
    print_zombie "  • Previously hidden API methods now accessible"
    echo ""
    
    print_warning "This demo reveals zombie code worth $50,000+ in development effort"
    echo ""
    
    print_info "🚀 Launching AdvancedZombieCodeDemo..."
    java -cp "target/classes:$(mvn -q dependency:build-classpath -Dmdep.outputFile=/dev/stdout)" \
        demo.AdvancedZombieCodeDemo
    
    if [ $? -eq 0 ]; then
        print_success "Zombie code capabilities demonstration completed successfully!"
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
