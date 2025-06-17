#!/usr/bin/env zsh

# Template for new run_*.sh scripts in Private Blockchain project
# Copy this template when creating new test scripts
# Version: 1.0.1
#
# Usage: 
#   1. Copy this file to run_your_test_name.sh
#   2. Update the script description and test logic
#   3. Make it executable: chmod +x run_your_test_name.sh

# Load shared functions (includes database cleanup functionality)
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
if [ -f "$SCRIPT_DIR/scripts/shared-functions.sh" ]; then
    source "$SCRIPT_DIR/scripts/shared-functions.sh"
else
    echo "❌ Error: shared-functions.sh not found. Please ensure the scripts directory exists."
    exit 1
fi

# Note: We don't use error_exit here because we haven't loaded shared-functions.sh yet

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
    # Initialize test environment (includes database cleanup)
    init_test_environment
    
    # Check dependencies
    check_dependencies
    
    # Compile project
    compile_project
    
    # Initialize test counters
    local total_tests=0
    local passed_tests=0
    local failed_tests=0
    
    # ========================================
    # YOUR TEST LOGIC GOES HERE
    # ========================================
    
    print_header "YOUR TEST SUITE NAME"
    
    # Example test execution:
    # clear_database_between_tests  # Call between test suites if needed
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
    print_test_summary "$total_tests" "$passed_tests" "$failed_tests"
    
    # Exit with appropriate code
    if [ "$failed_tests" -eq 0 ]; then
        exit 0
    else
        error_exit "Some tests have failed. Check the results above."
    fi
}

# Execute main function
main "$@"
