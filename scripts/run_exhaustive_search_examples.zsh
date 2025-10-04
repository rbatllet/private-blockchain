#!/usr/bin/env zsh

# EXHAUSTIVE SEARCH EXAMPLES SCRIPT
# Practical examples for developers showing TRUE exhaustive search capabilities
# Version: 1.0.0
# Created: 2025-07-10

# Set script directory before changing directories
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Load common functions library
source "${SCRIPT_DIR}/lib/common_functions.zsh"

# Change to project root directory
cd "$SCRIPT_DIR/.."

# Icons - kept for compatibility
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

# Check if project directory exists
if [[ ! -f "pom.xml" ]]; then
    print_error "pom.xml not found. Make sure to run this script from the project root directory."
    exit 1
fi

echo "🔍 EXHAUSTIVE SEARCH - Practical Examples"
echo "================================================================"
echo "📋 Script Version: 1.0.0 | $(date '+%Y-%m-%d')"
echo

print_info "This script demonstrates practical usage of TRUE exhaustive search:"
print_info "   🔍 On-chain content search (encrypted and plain text)"
print_info "   📦 Off-chain file search with various formats"
print_info "   🔐 Mixed content scenarios and security validation"
print_info "   📊 Performance optimization and caching"
print_info "   📋 Thread safety and concurrent operations"
echo

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

# Function to run command with error checking
run_command() {
    local cmd="$1"
    local description="$2"
    
    print_info "$description..."
    if eval "$cmd"; then
        print_success "$description completed successfully"
    else
        print_error "$description failed"
        return 1
    fi
}

# Run the examples
print_step "STEP 2: Running Practical Examples"
print_info "Starting comprehensive exhaustive search examples..."
print_info "Each example demonstrates specific real-world scenarios"
echo

# Set Java options for better performance
export MAVEN_OPTS="-Xmx2g -XX:+UseG1GC"

# Run the examples with proper error handling
print_info "🚀 Launching ExhaustiveSearchExamples..."
if java -cp "target/classes:$(mvn -q dependency:build-classpath -Dmdep.outputFile=/dev/stdout)" \
    demo.ExhaustiveSearchExamples; then
    echo
    print_success "Examples execution completed successfully!"
else
    echo
    print_error "Examples execution failed"
    print_info "Checking for common issues..."
    
    # Check if the examples class exists
    if [[ ! -f "src/main/java/demo/ExhaustiveSearchExamples.java" ]]; then
        print_error "ExhaustiveSearchExamples.java not found in src/main/java/demo/"
        print_info "Please ensure the examples file is created in the correct location"
    fi
    
    # Check compilation
    print_info "Attempting to compile examples specifically..."
    if mvn compile -q; then
        print_success "Examples class compiled successfully"
        print_warning "There might be a runtime issue - check the output above"
    else
        print_error "Examples class compilation failed"
        print_info "Please check for compilation errors"
    fi
    
    exit 1
fi

print_separator

print_step "STEP 3: Examples Summary"
print_info "📊 The examples demonstrated:"
print_info "   🔍 Example 1: Basic on-chain content search (encrypted + plain text)"
print_info "   📦 Example 2: Off-chain file search with medical records"
print_info "   📝 Example 3: Mixed content search (JSON, text, encrypted files)"
print_info "   🔐 Example 4: Security validation and access control"
print_info "   📊 Example 5: Performance optimization and intelligent caching"
print_info "   📋 Example 6: Thread safety with concurrent search operations"
echo

print_success "All practical examples completed successfully!"
print_info "Review the output above for detailed implementation patterns"

print_separator

# Display next steps
print_info "Next steps:"
echo "  1. Run 'scripts/run_exhaustive_search_demo.zsh' for interactive demonstration"
echo "  2. Run 'scripts/run_search_framework_demo.zsh' for advanced search features"
echo "  3. Test with 'mvn test -Dtest=OnChainContentSearchTest' to verify functionality"
echo ""

# Final cleanup
cleanup_database > /dev/null 2>&1

print_success "🧹 Examples script completed - all temporary files cleaned up automatically"
echo ""
print_info "📚 For detailed documentation, see:"
print_info "   📖 docs/EXHAUSTIVE_SEARCH_GUIDE.md - Complete guide with API reference"
print_info "   🔧 src/main/java/demo/ExhaustiveSearchExamples.java - Source code for examples"
print_info "   🎯 Run './scripts/run_exhaustive_search_demo.zsh' for interactive demonstration"
