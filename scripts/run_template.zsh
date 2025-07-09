#!/usr/bin/env zsh

# Template for new run_*.zsh scripts in Private Blockchain project
# Copy this template when creating new test scripts
# Version: 1.0.1
#
# Usage: 
#   1. Copy this file to run_your_test_name.zsh
#   2. Update the script description and test logic
#   3. Make it executable: chmod +x run_your_test_name.zsh

# Set script directory and navigate to project root
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR/.."

# Load common functions library
if [ -f "${SCRIPT_DIR}/lib/common_functions.zsh" ]; then
    source "${SCRIPT_DIR}/lib/common_functions.zsh"
else
    echo "❌ Error: common_functions.zsh not found. Please ensure the lib directory exists."
    exit 1
fi

# Note: We don't use error_exit here because we haven't loaded common_functions.zsh yet

# Script configuration
SCRIPT_NAME="$(basename "$0")"
SCRIPT_DESCRIPTION="Description of what this script does"

# Show usage if requested
if [ "$1" = "--help" ] || [ "$1" = "-h" ]; then
    show_usage "$SCRIPT_NAME" "$SCRIPT_DESCRIPTION"
    exit 0
fi

# Main script execution
main() {
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
    
    # Initialize test counters
    local total_tests=0
    local passed_tests=0
    local failed_tests=0
    
    # ========================================
    # YOUR TEST LOGIC GOES HERE
    # ========================================
    
    print_header "YOUR TEST SUITE NAME"
    
    # Example test execution:
    # cleanup_database  # Call between test suites if needed
    # 
    # print_step "🧪 Running your test..."
    # if mvn test -Dtest=YourTestClass -q; then
    #     print_success "✅ Your test: PASSED"
    #     ((passed_tests++))
    # else
    #     print_error "❌ Your test: FAILED"
    #     ((failed_tests++))
    # fi
    # ((total_tests++))
    
    # ========================================
    # END OF YOUR TEST LOGIC
    # ========================================
    
    # Print test summary
    print_separator
    print_header "TEST SUMMARY"
    print_info "Total tests: $total_tests"
    print_info "Passed: $passed_tests"
    print_info "Failed: $failed_tests"
    
    # Exit with appropriate code
    if [ "$failed_tests" -eq 0 ]; then
        print_success "All tests passed!"
        exit 0
    else
        error_exit "Some tests have failed. Check the results above."
    fi
}

# Execute main function
main "$@"
