#!/usr/bin/env zsh

# Advanced Search Demo Script  
# Comprehensive demonstration of advanced search capabilities
# Version: 1.0.0

# Set script directory and navigate to project root
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR/.."

# Load common functions library
source "${SCRIPT_DIR}/lib/common_functions.zsh"

print_header "SEARCH FRAMEWORK DEMO"

print_info "This comprehensive demonstration covers:"
print_info "• Multi-level search strategies (FAST, INCLUDE_DATA, EXHAUSTIVE)"
print_info "• Content-agnostic search with user-defined terms"
print_info "• Public metadata search without passwords"
print_info "• Private encrypted content search with passwords"
print_info "• Granular term visibility control"
print_info "• Performance-optimized search operations"
print_info "• Advanced search scenarios and edge cases"

# Check if we're in the correct directory
check_project_directory

# Check prerequisites
if ! check_java || ! check_maven; then
    exit 1
fi

# Clean and compile
cleanup_database

if ! compile_project; then
    exit 1
fi

print_separator

# Run the demo
print_step "🚀 Launching Advanced Search Demo..."
mvn exec:java -Dexec.mainClass="demo.SearchFrameworkDemo" -q

if [ $? -eq 0 ]; then
    print_success "Advanced search demo completed successfully!"
else
    print_error "Demo execution failed"
    exit 1
fi

print_separator

# Next steps
print_info "📝 Related demos:"
print_info "1. Run 'scripts/run_granular_term_visibility_demo.zsh' for granular control demo"
print_info "2. Run 'scripts/run_user_friendly_encryption_demo.zsh' for encryption demo"
print_info "3. Check docs/SEARCH_GUIDE.md for detailed documentation"
print_info "4. Run tests with 'mvn test -Dtest=AdvancedSearch*'"

print_success "Advanced search demo complete!"