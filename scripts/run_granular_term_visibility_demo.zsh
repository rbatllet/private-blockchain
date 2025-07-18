#!/usr/bin/env zsh

# 🔐 Granular Term Visibility Demo Runner
# ======================================
# 
# This script demonstrates the advanced granular term visibility control feature
# that allows users to specify exactly which search terms should be public vs private.
# Version: 1.0.0

# Set script directory before changing directories
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Load common functions library
source "${SCRIPT_DIR}/lib/common_functions.zsh"

# Change to project root directory
cd "$SCRIPT_DIR/.."

echo "🔐 GRANULAR TERM VISIBILITY DEMO"
echo "================================"
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

# Run the granular term visibility demo
print_info "🚀 Running Granular Term Visibility Demo..."
print_step "============================================="

print_info "🚀 Launching GranularTermVisibilityDemo..."
java -cp "target/classes:$(mvn -q dependency:build-classpath -Dmdep.outputFile=/dev/stdout)" \
    demo.GranularTermVisibilityDemo

DEMO_RESULT=$?

print_separator

if [ $DEMO_RESULT -eq 0 ]; then
    print_success "Demo completed successfully!"
    echo ""
    print_info "📋 What this demo showed:"
    print_info "  🔍 How to control individual term visibility"
    print_info "  🌍 Public terms searchable without password"  
    print_info "  🔐 Private terms require password authentication"
    print_info "  🏥 Medical record privacy example"
    print_info "  💰 Financial data protection example"
    print_info "  🔍 Search behavior differences"
else
    print_error "Demo execution failed"
    exit 1
fi

echo ""
print_info "💡 Key benefits:"
print_info "  ✨ Fine-grained privacy control"
print_info "  🎯 Compliance with data protection regulations"
print_info "  🔒 Sensitive data protection while maintaining searchability"
print_info "  🚀 Flexible term-level security configuration"

print_separator

# Display next steps
print_info "Next steps:"
echo "  1. Run 'scripts/run_search_framework_demo.zsh' for advanced search features"
echo "  2. Run 'scripts/run_blockchain_demo.zsh' for basic blockchain operations"
echo "  3. Review the demo location: src/main/java/demo/GranularTermVisibilityDemo.java"
echo ""

# Final cleanup
cleanup_database > /dev/null 2>&1

print_success "Granular Term Visibility Demo completed successfully!"
