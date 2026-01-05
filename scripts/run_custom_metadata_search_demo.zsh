#!/usr/bin/env zsh

# 🔍 Custom Metadata Search Demo Runner
# ======================================
#
# This script demonstrates the powerful custom metadata search capabilities
# that allow querying blockchain blocks by their structured JSON metadata fields.
# Version: 1.0.0

# Set script directory before changing directories
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Load common functions library
source "${SCRIPT_DIR}/lib/common_functions.zsh"

# Change to project root directory
cd "$SCRIPT_DIR/.."

echo "🔍 CUSTOM METADATA SEARCH DEMO"
echo "==============================="
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

# Run the custom metadata search demo
print_info "🚀 Running Custom Metadata Search Demo..."
print_step "==========================================="

print_info "🚀 Launching CustomMetadataSearchDemo..."
java -cp "target/classes:$(mvn -q dependency:build-classpath -Dmdep.outputFile=/dev/stdout)" \
    demo.CustomMetadataSearchDemo

DEMO_RESULT=$?

print_separator

if [ $DEMO_RESULT -eq 0 ]; then
    print_success "Demo completed successfully!"
    echo ""
    print_info "📋 What this demo showed:"
    print_info "  🔍 Substring search in custom metadata (case-insensitive)"
    print_info "  🎯 Exact key-value pair matching for precise queries"
    print_info "  🔗 Multiple criteria search with AND logic"
    print_info "  🏥 Medical dashboard with patient records"
    print_info "  ⚖️  Legal document management and tracking"
    print_info "  🛒 E-commerce order tracking and analytics"
else
    print_error "Demo execution failed"
    exit 1
fi

echo ""
print_info "💡 Key benefits:"
print_info "  ✨ Query blocks by structured JSON metadata"
print_info "  🚀 Three powerful search methods for different use cases"
print_info "  🔒 Thread-safe with comprehensive input validation"
print_info "  📊 Real-time analytics without decrypting content"
print_info "  🎯 Support for numeric, boolean, string, arrays, and nested objects"

print_separator

# Display next steps
print_info "Next steps:"
echo "  1. Review the implementation: src/main/java/demo/CustomMetadataSearchDemo.java"
echo "  2. Read the documentation: docs/data-management/METADATA_MANAGEMENT_GUIDE.md"
echo "  3. Run tests: mvn test -Dtest=CustomMetadataSearchTest"
echo "  4. Run 'scripts/run_blockchain_demo.zsh' for basic blockchain operations"
echo ""

# Final cleanup
cleanup_database > /dev/null 2>&1

print_success "Custom Metadata Search Demo completed successfully!"
