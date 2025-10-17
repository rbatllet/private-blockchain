#!/usr/bin/env zsh

# Simple Thread Safety Test (with detailed logging)
# Version: 1.0.0

# Set script directory before changing directories
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Load common functions library
source "${SCRIPT_DIR}/lib/common_functions.zsh"

# Change to project root directory
cd "$SCRIPT_DIR/.."

print_header "SIMPLE THREAD SAFETY TEST (with detailed logging)"

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
TEST_RESULT=$?
if [ $TEST_RESULT -eq 0 ]; then
    print_success "Simple thread safety test completed successfully!"
    
    # Extract key statistics from the log
    print_info "📊 Test Statistics:"
    
    # Count blocks created (look for "Block #X added successfully!")
    BLOCKS_CREATED=$(grep -c "Block #[0-9]* added successfully" logs/simple-thread-safety-test.log 2>/dev/null || echo "0")
    BLOCKS_CREATED=$(printf "%02d" "$BLOCKS_CREATED" 2>/dev/null || echo "00")
    print_info "• Blocks created: $BLOCKS_CREATED"

    # Count off-chain operations (look for off-chain in validation results)
    OFF_CHAIN=$(grep -c "✅ Valid off-chain blocks:" logs/simple-thread-safety-test.log 2>/dev/null || echo "0")
    if [ "$OFF_CHAIN" -gt 0 ]; then
        OFF_CHAIN=$(grep "Blocks with off-chain data:" logs/simple-thread-safety-test.log 2>/dev/null | grep -o "[0-9]*/[0-9]*" | cut -d'/' -f1 | tail -1 || echo "0")
    fi
    OFF_CHAIN=$(printf "%02d" "$OFF_CHAIN" 2>/dev/null || echo "00")
    print_info "• Off-chain blocks: $OFF_CHAIN"

    # Count validations performed (look for "Block #X validation passed")
    VALIDATIONS=$(grep -c "Block #[0-9]* validation passed" logs/simple-thread-safety-test.log 2>/dev/null || echo "0")
    VALIDATIONS=$(printf "%02d" "$VALIDATIONS" 2>/dev/null || echo "00")
    print_info "• Validations performed: $VALIDATIONS"
    
    # Check for any errors
    ERRORS=$(grep -c "ERROR\|Exception\|Failed" logs/simple-thread-safety-test.log 2>/dev/null || echo "0")
    # Clean the variable to ensure it's a valid integer
    ERRORS=$(echo "$ERRORS" | tr -d '\n' | tr -d ' ' | head -1)
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
print_info "2. Check logs/simple-thread-safety-test.log for detailed execution trace"
print_info "3. Run with KEEP_LOGS=true to preserve log files"
