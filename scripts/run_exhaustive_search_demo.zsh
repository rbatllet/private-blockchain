#!/usr/bin/env zsh

# EXHAUSTIVE SEARCH DEMO SCRIPT v1.0
# Comprehensive demonstration of TRUE exhaustive search capabilities
# Version: 1.0.0
# Created: 2025-07-10

# Set script directory before changing directories
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Load common functions library
source "${SCRIPT_DIR}/lib/common_functions.zsh"

# Change to project root directory
cd "$SCRIPT_DIR/.."

# Icons (matching CLAUDE.md style) - kept for compatibility with remaining echo statements
SUCCESS="✅"
ERROR="❌"
INFO="ℹ️"
WARNING="⚠️"
STATS="📊"
TEST="📋"
KEY="🔑"
BLOCKCHAIN="🔗"
DATA="📝"
SEARCH="🔍"
SECURITY="🔐"
BLOCKS="📦"
CLEANUP="🧹"

echo "🚀 EXHAUSTIVE SEARCH DEMO - TRUE Exhaustive v2.0"
echo "================================================================"
echo "📋 Script Version: 1.0.0 | $(date '+%Y-%m-%d')"
echo
print_info "This demo showcases comprehensive blockchain search across:"
print_info "   📝 On-chain blocks (encrypted and plain text)"
print_info "   📦 Off-chain files (encrypted and plain text)" 
print_info "   🔍 Mixed content scenarios with performance metrics"
print_info "   🔐 Security validation and access control"
echo

# Check if project directory exists
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

# Run the demo
print_step "STEP 2: Running Exhaustive Search Demo"
print_info "Starting comprehensive search demonstration..."
print_info "This will create mixed content blockchain and perform exhaustive searches"
echo

# Set Java options for better performance
export MAVEN_OPTS="-Xmx2g -XX:+UseG1GC"

# Run the demo with proper error handling
print_info "🚀 Launching ExhaustiveSearchDemo..."
if java -cp "target/classes:$(mvn -q dependency:build-classpath -Dmdep.outputFile=/dev/stdout)" \
    demo.ExhaustiveSearchDemo; then
    echo
    print_success "Demo execution completed successfully!"
else
    echo
    print_error "Demo execution failed"
    print_info "Checking for common issues..."
    
    # Check if the demo class exists
    if [[ ! -f "src/main/java/demo/ExhaustiveSearchDemo.java" ]]; then
        print_error "ExhaustiveSearchDemo.java not found in src/main/java/demo/"
        print_info "Please ensure the demo file is created in the correct location"
    fi
    
    # Check compilation
    print_info "Attempting to compile demo specifically..."
    if mvn compile -q; then
        print_success "Demo class compiled successfully"
        print_warning "There might be a runtime issue - check the output above"
    else
        print_error "Demo class compilation failed"
        print_info "Please check for compilation errors"
    fi
    
    exit 1
fi

print_separator

print_step "STEP 3: Demo Summary"
print_info "📊 The demo demonstrated:"
print_info "   🔍 Exhaustive search across on-chain and off-chain content"
print_info "   🔐 Security validation with encrypted content"
print_info "   📊 Performance metrics and caching capabilities"
print_info "   📝 Mixed content handling (text, JSON, encrypted files)"
print_info "   🔗 TRUE exhaustive search v2.0 functionality"
echo

print_success "Exhaustive Search Demo completed successfully!"
print_info "Review the output above for detailed search results and performance metrics"

print_separator

# Display next steps
print_info "Next steps:"
echo "  1. Run 'scripts/run_search_framework_demo.zsh' for advanced search features"
echo "  2. Run 'scripts/run_blockchain_demo.zsh' for basic blockchain operations"
echo "  3. Test with 'mvn test -Dtest=OnChainContentSearchTest' to verify functionality"
echo ""

# Final cleanup
cleanup_database > /dev/null 2>&1

print_success "🧹 Demo script completed - all temporary files cleaned up automatically"
