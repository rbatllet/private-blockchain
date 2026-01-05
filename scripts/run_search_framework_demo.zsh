#!/usr/bin/env zsh

# Advanced Search Demo Script  
# Comprehensive demonstration of advanced search capabilities
# Version: 1.0.0

# Set script directory before changing directories
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Load common functions library
source "${SCRIPT_DIR}/lib/common_functions.zsh"

# Change to project root directory
cd "$SCRIPT_DIR/.."

echo "🔍 SEARCH FRAMEWORK DEMO"
echo "========================"
echo ""

print_info "This comprehensive demonstration covers:"
print_info "• UserFriendlyEncryptionAPI with advanced search capabilities"
print_info "• Storing searchable data with custom keywords"
print_info "• Advanced search across encrypted blockchain content"
print_info "• Privacy-preserving search with metadata"
print_info "• Keyword-based search and decryption"
print_info "• Performance-optimized search operations"
print_info "• Real-world search scenarios and use cases"

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

# Run the demo
print_info "🚀 Launching SearchFrameworkDemo..."
java -cp "target/classes:$(mvn -q dependency:build-classpath -Dmdep.outputFile=/dev/stdout)" \
    demo.SearchFrameworkDemo

DEMO_RESULT=$?

print_separator

if [ $DEMO_RESULT -eq 0 ]; then
    print_success "Advanced search demo completed successfully!"
else
    print_error "Demo execution failed"
    exit 1
fi

# Display next steps
print_info "Next steps:"
echo "  1. Run 'scripts/run_granular_term_visibility_demo.zsh' for granular control demo"
echo "  2. Run 'scripts/run_user_friendly_encryption_demo.zsh' for encryption demo"
echo "  3. Check docs/search/SEARCH_FRAMEWORK_GUIDE.md for detailed documentation"
echo "  4. Run tests with 'mvn test -Dtest=AdvancedSearch*'"
echo ""

# Final cleanup
cleanup_database > /dev/null 2>&1

print_success "Search Framework Demo completed successfully!"
