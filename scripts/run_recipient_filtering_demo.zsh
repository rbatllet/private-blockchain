#!/usr/bin/env zsh

# Demo: P0 Performance Fix - Native Recipient Filtering
# This demo showcases the new native database queries for recipient filtering
# Version: 1.0.0
#
# Features demonstrated:
#   - Creating blocks with recipient public keys
#   - Native recipient filtering (O(1) index lookup)
#   - Counting blocks by recipient
#   - Hash integrity (recipientPublicKey in hash)
#   - Performance comparison

# Set script directory before changing directories
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Load common functions library
if [ -f "${SCRIPT_DIR}/lib/common_functions.zsh" ]; then
    source "${SCRIPT_DIR}/lib/common_functions.zsh"
else
    echo "❌ Error: common_functions.zsh not found. Please ensure the lib directory exists."
    exit 1
fi

# Change to project root directory
cd "$SCRIPT_DIR/.."

# Script configuration
SCRIPT_NAME="$(basename "$0")"
SCRIPT_DESCRIPTION="Demo: Native Recipient Filtering (P0 Performance Fix)"

# Show usage if requested
if [ "$1" = "--help" ] || [ "$1" = "-h" ]; then
    show_usage "$SCRIPT_NAME" "$SCRIPT_DESCRIPTION"
    exit 0
fi

# Main script execution
main() {
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

    # Initialize test counters
    local total_tests=0
    local passed_tests=0
    local failed_tests=0

    # ========================================
    # RECIPIENT FILTERING DEMO
    # ========================================

    print_separator
    echo "📊 RECIPIENT FILTERING DEMO (P0 Performance Fix)"
    echo "=============================================="
    echo ""
    echo "This demo showcases the new native database queries for recipient filtering."
    echo "Before: O(n) linear search + JSON string parsing"
    echo "After:  O(1) indexed database lookup"
    echo ""

    # Run the demo
    print_step "🧪 Running Recipient Filtering Demo..."

    if mvn exec:java -Dexec.mainClass="demo.RecipientFilteringDemo" -q; then
        print_success "✅ Recipient Filtering Demo: PASSED"
        ((passed_tests++))
    else
        print_error "❌ Recipient Filtering Demo: FAILED"
        ((failed_tests++))
    fi
    ((total_tests++))

    # ========================================
    # END OF DEMO
    # ========================================

    # Print test summary
    print_separator
    print_info "📊 TEST SUMMARY"
    echo "==============="
    print_info "Total tests: $total_tests"
    print_info "Passed: $passed_tests"
    print_info "Failed: $failed_tests"

    # Exit with appropriate code
    if [ "$failed_tests" -eq 0 ]; then
        print_success "Demo completed successfully!"
        echo ""
        echo "🎯 Key Features Demonstrated:"
        echo "  ✓ Creating blocks with recipient public keys (immutable field)"
        echo "  ✓ Native recipient filtering with indexed queries (O(1))"
        echo "  ✓ Counting blocks by recipient with native queries"
        echo "  ✓ Hash integrity: recipientPublicKey included in hash calculation"
        echo "  ✓ Thread-safe operations with GLOBAL_BLOCKCHAIN_LOCK"
        echo ""
        echo "📈 Performance Improvement:"
        echo "  Before: Load ALL blocks → Filter in-memory → Parse JSON strings"
        echo "  After:  Single indexed database query → O(1) lookup"
        echo ""
        exit 0
    else
        print_error "Demo failed. Check the results above."
        exit 1
    fi
}

# Execute main function
main "$@"
