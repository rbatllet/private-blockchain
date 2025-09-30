#!/usr/bin/env zsh

# Comprehensive Thread Safety Test
# Version: 1.0.0

# Set script directory before changing directories
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Load common functions library
source "${SCRIPT_DIR}/lib/common_functions.zsh"

# Change to project root directory
cd "$SCRIPT_DIR/.."

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

# Run the actual test (without -q to get statistics)
print_step "COMPREHENSIVE THREAD SAFETY TEST"
mvn test -Dtest=ComprehensiveThreadSafetyTest 2>&1 | tee logs/thread-safety-full.log

# Check test result
TEST_RESULT=$?
if [ $TEST_RESULT -eq 0 ]; then
    print_success "All thread safety tests passed!"

    # Extract test statistics from Maven Surefire output
    # Format: [INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
    print_info "📊 Test Statistics:"

    # Extract from the Results section (more reliable)
    STATS_LINE=$(grep -E "^\[INFO\] Tests run:" logs/thread-safety-full.log | tail -1)

    if [[ -n "$STATS_LINE" ]]; then
        TESTS_RUN=$(echo "$STATS_LINE" | grep -oE "Tests run: [0-9]+" | grep -oE "[0-9]+")
        FAILURES=$(echo "$STATS_LINE" | grep -oE "Failures: [0-9]+" | grep -oE "[0-9]+")
        ERRORS=$(echo "$STATS_LINE" | grep -oE "Errors: [0-9]+" | grep -oE "[0-9]+")

        print_info "• Tests run: ${TESTS_RUN:-0}"
        print_info "• Failures: ${FAILURES:-0}"
        print_info "• Errors: ${ERRORS:-0}"

        if [[ "${FAILURES:-0}" -eq 0 && "${ERRORS:-0}" -eq 0 && "${TESTS_RUN:-0}" -gt 0 ]]; then
            print_success "All tests: PASSED"
        fi
    else
        print_warning "Could not extract test statistics from log"
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
