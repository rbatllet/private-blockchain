#!/usr/bin/env zsh

# Simple Thread Safety Test (with detailed logging)
# Version: 1.0.1

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
SCRIPT_DESCRIPTION="Simple Thread Safety Test with detailed logging (10 threads, 5 operations)"

# Show usage if requested
if [ "$1" = "--help" ] || [ "$1" = "-h" ]; then
    echo "Usage: $SCRIPT_NAME [OPTIONS]"
    echo ""
    echo "$SCRIPT_DESCRIPTION"
    echo ""
    echo "Options:"
    echo "  -h, --help   Show this help message"
    echo ""
    echo "Environment variables:"
    echo "  KEEP_TEST_FILES=true   Preserve test files after execution"
    echo "  KEEP_LOGS=true         Preserve log files after execution"
    exit 0
fi

# Main script execution
main() {
    print_header "SIMPLE THREAD SAFETY TEST (with detailed logging)"

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

    print_step "📝 Running SIMPLE thread safety tests with detailed logging..."
    print_info "This runs a simplified test suite (10 threads, 5 operations) with extensive logging"
    print_info "to help trace what's happening during concurrent operations. Ideal for debugging."

    print_step "🚀 Executing thread safety tests with comprehensive logging..."
    print_info "Check logs/test-app.log for persistent logs"

    # Set environment variables for detailed logging
    export LOG_LEVEL=DEBUG
    export THREAD_SAFETY_DEBUG=true

    # Prepare logging directory
    mkdir -p logs

    # Run the actual test with proper classpath
    print_step "SIMPLIFIED THREAD SAFETY TEST"
    java -cp "target/classes:target/test-classes:$(mvn -q dependency:build-classpath -Dmdep.outputFile=/dev/stdout)" \
        -Dlog4j.configurationFile=log4j2.xml \
        -Dlog.level=DEBUG \
        -Dthread.safety.debug=true \
        -Dfile.encoding=UTF-8 \
        -Djava.util.logging.config.file=logging.properties \
        -Dlog4j2.debug=false \
        demo.SimpleThreadSafetyTest 2>&1 | tee logs/simple-thread-safety-test.log

    # Check test result
    local TEST_RESULT=$?
    
    # Extract key statistics from STATS_SUMMARY block in the log
    local BLOCKS_CREATED="00"
    local OFF_CHAIN="00"
    local VALIDATIONS="00"
    local ERRORS="0"
    
    if [ -f logs/simple-thread-safety-test.log ]; then
        # Parse STATS_SUMMARY block
        local blocks=$(sed -n '/STATS_SUMMARY_START/,/STATS_SUMMARY_END/p' logs/simple-thread-safety-test.log 2>/dev/null | grep "BLOCKS_CREATED=" | cut -d'=' -f2)
        if [[ "$blocks" =~ ^[0-9]+$ ]] && [ "$blocks" -gt 0 ]; then
            BLOCKS_CREATED=$(printf "%02d" "$blocks")
        fi

        local offchain=$(sed -n '/STATS_SUMMARY_START/,/STATS_SUMMARY_END/p' logs/simple-thread-safety-test.log 2>/dev/null | grep "OFFCHAIN_BLOCKS=" | cut -d'=' -f2)
        if [[ "$offchain" =~ ^[0-9]+$ ]] && [ "$offchain" -gt 0 ]; then
            OFF_CHAIN=$(printf "%02d" "$offchain")
        fi

        local validations=$(sed -n '/STATS_SUMMARY_START/,/STATS_SUMMARY_END/p' logs/simple-thread-safety-test.log 2>/dev/null | grep "VALIDATIONS=" | cut -d'=' -f2)
        if [[ "$validations" =~ ^[0-9]+$ ]] && [ "$validations" -gt 0 ]; then
            VALIDATIONS=$(printf "%02d" "$validations")
        fi

        # Check for errors (exclude Log4j2 configuration errors)
        ERRORS=$(grep "ERROR" logs/simple-thread-safety-test.log 2>/dev/null | grep -v "Reconfiguration failed" | grep -v "No configuration found" | wc -l | tr -d ' ' || echo "0")
    fi
    
    if [ $TEST_RESULT -eq 0 ]; then
        print_success "Simple thread safety test completed successfully!"

        # Show statistics (already extracted above)
        print_info "📊 Test Statistics:"
        print_info "• Blocks created: $BLOCKS_CREATED"
        print_info "• Off-chain blocks: $OFF_CHAIN"
        print_info "• Validations performed: $VALIDATIONS"
        
        if [[ "$ERRORS" =~ ^[0-9]+$ ]] && [ "$ERRORS" -gt 0 ]; then
            print_warning "Warnings/Errors found: $ERRORS"
            print_info "Check logs/simple-thread-safety-test.log for details"
        else
            print_success "No errors detected"
        fi
    else
        print_error "Simple thread safety test failed!"
        print_info "Check logs/simple-thread-safety-test.log for details"
        exit 1
    fi

    print_success "SIMPLE THREAD SAFETY TEST COMPLETE"

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
    print_info "📝 Next steps:"
    print_info "1. Run 'scripts/test_thread_safety_full.zsh' for comprehensive thread safety testing"
    if [[ "${KEEP_LOGS:-false}" == "true" ]]; then
        print_info "2. Check logs/simple-thread-safety-test.log for detailed execution trace"
    else
        print_info "2. Run with KEEP_LOGS=true to preserve log files for detailed analysis"
    fi

    exit 0
}

# Execute main function
main "$@"
