#!/usr/bin/env zsh

# Dynamic Search Configuration Demo
# Demonstrates correct usage of singleton SearchSpecialistAPI with dynamic config updates

# Set script directory before changing directories
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Load common functions library
source "${SCRIPT_DIR}/lib/common_functions.zsh"

# Change to project root directory
cd "$SCRIPT_DIR/.."

print_header "DYNAMIC SEARCH CONFIGURATION DEMO"
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

# Clean up any existing data
cleanup_database

# Compile if needed
if ! compile_project; then
    exit 1
fi

print_separator

print_info "🚀 Running Singleton Search API Demo..."
print_info ""
print_info "This demo shows TWO CORRECT approaches to use SearchSpecialistAPI:"
print_info "  1️⃣  SINGLETON: blockchain.getSearchSpecialistAPI() - shared index"
print_info "  2️⃣  CONSTRUCTOR: new SearchSpecialistAPI(blockchain, password, privateKey, config) - custom config"
print_info ""

# Run the demo
java -cp "target/classes:$(mvn -q dependency:build-classpath -Dmdep.outputFile=/dev/stdout)" \
    -Dlog4j.configurationFile=log4j2.xml \
    -Dfile.encoding=UTF-8 \
    demo.DynamicSearchConfigurationDemo

DEMO_RESULT=$?

print_separator

if [ $DEMO_RESULT -eq 0 ]; then
    print_success "Search API Comparison Demo completed successfully!"
    
    print_info ""
    print_info "📚 Two Valid Approaches Demonstrated:"
    print_info ""
    print_info "  1️⃣  SINGLETON PATTERN:"
    print_info "     Code: blockchain.getSearchSpecialistAPI()"
    print_info "     ✅ Shared index (fast, consistent)"
    print_info "     ✅ Automatic blockchain integration"
    print_info "     ✅ Best for general use cases"
    print_info ""
    print_info "  2️⃣  DIRECT CONSTRUCTOR:"
    print_info "     Code: new SearchSpecialistAPI(blockchain, password, privateKey, customConfig)"
    print_info "     ✅ Custom EncryptionConfig (high-security, balanced, performance)"
    print_info "     ✅ Independent search instance"
    print_info "     ✅ Perfect for specialized requirements"
    print_info "     ⚠️  Requires blockchain with existing blocks"
else
    print_error "Dynamic Search Configuration Demo failed!"
    exit 1
fi

# Cleanup
if [[ "${KEEP_TEST_FILES:-false}" != "true" ]]; then
    print_info "🧹 Cleaning up test files..."
    cleanup_database
    print_success "Cleanup complete"
else
    print_info "💾 Test files preserved (KEEP_TEST_FILES=true)"
fi

print_separator
print_success "DEMO COMPLETE"

exit 0
