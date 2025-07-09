#!/usr/bin/env zsh

# Comprehensive Thread Safety Test
# Version: 1.0.0

# Set script directory and navigate to project root
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR/.."

# Load common functions library
source "${SCRIPT_DIR}/lib/common_functions.zsh"

print_header "COMPREHENSIVE THREAD SAFETY TEST"

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

print_step "📝 Running comprehensive thread safety tests..."
print_info "This includes tests for:"
print_info "• Block number uniqueness under extreme load"
print_info "• Concurrent blockchain operations"
print_info "• Thread-safe DAO operations"
print_info "• Export/Import under concurrent access"
print_info "• Search operations with multiple threads"

# Prepare logging directory
mkdir -p logs

# Run the actual test
print_step "COMPREHENSIVE THREAD SAFETY TEST"
mvn test -Dtest=ComprehensiveThreadSafetyTest -q 2>&1 | tee logs/thread-safety-full.log

# Check test result
TEST_RESULT=$?
if [ $TEST_RESULT -eq 0 ]; then
    print_success "All thread safety tests passed!"
    
    # Extract test statistics
    print_info "📊 Test Statistics:"
    TESTS_RUN=$(grep -oE "Tests run: [0-9]+" logs/thread-safety-full.log | tail -1 | grep -oE "[0-9]+")
    FAILURES=$(grep -oE "Failures: [0-9]+" logs/thread-safety-full.log | tail -1 | grep -oE "[0-9]+")
    ERRORS=$(grep -oE "Errors: [0-9]+" logs/thread-safety-full.log | tail -1 | grep -oE "[0-9]+")
    
    print_info "• Tests run: ${TESTS_RUN:-0}"
    print_info "• Failures: ${FAILURES:-0}"
    print_info "• Errors: ${ERRORS:-0}"
    
    if [[ "${FAILURES:-0}" -eq 0 && "${ERRORS:-0}" -eq 0 ]]; then
        print_success "All tests: PASSED"
    fi
else
    print_error "Thread safety tests failed!"
    print_info "Check logs/thread-safety-full.log for details"
    
    # Show last few error lines
    print_info "Recent errors:"
    grep -E "ERROR|FAILED|Exception" logs/thread-safety-full.log | tail -10
    exit 1
fi

print_success "COMPREHENSIVE THREAD SAFETY TEST COMPLETE"

# Optional: Clean up test files after completion
if [[ "${KEEP_TEST_FILES:-false}" != "true" ]]; then
    cleanup_database
    
    if [[ "${KEEP_LOGS:-false}" != "true" ]]; then
        print_info "🧹 Cleaning up logs..."
        setopt NULL_GLOB
        rm -f logs/*.log 2>/dev/null
        unsetopt NULL_GLOB
    else
        print_info "Logs preserved in logs/ directory"
    fi
else
    print_info "Test files preserved (KEEP_TEST_FILES=true)"
fi

print_separator
print_info "📝 Analysis:"
print_info "• Thread safety is critical for blockchain integrity"
print_info "• All concurrent operations must maintain consistency"
print_info "• Block numbers must remain unique under load"
print_info "• Search and export operations must be thread-safe"