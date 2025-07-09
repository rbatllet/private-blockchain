#!/usr/bin/env zsh

# CRITICAL RACE CONDITION FIX VALIDATION TEST
# This test validates that the getLastBlock() race condition has been resolved
# Usage: ./test_race_condition_fix.zsh
# Version: 1.0.0

# Load shared functions for database cleanup and utility functions

# Set script directory and navigate to project root
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR/.."

# Load common functions library
source "${SCRIPT_DIR}/lib/common_functions.zsh"

print_header "RACE CONDITION FIX VALIDATION TEST"

print_info "This test will validate that the race condition in getLastBlock() has been resolved."
print_info "Previous issue: Multiple threads would see the same block number under extreme load."
print_info "Expected result: Each thread should see unique block numbers (no duplicates)."

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

print_step "🚀 Running EXTREME THREAD SAFETY TEST (200 threads x 5 blocks = 1000 blocks)..."
print_info "This is the exact scenario that revealed the race condition..."
print_info "The RaceConditionTest.java class is located in src/main/java/demo/"

# Compile and run the test
print_step "🔨 Compiling and running race condition test..."
mvn exec:java -Dexec.mainClass="demo.RaceConditionTest" \
  -Djava.util.logging.config.file=src/main/resources/logging.properties \
  -Djakarta.persistence.show_sql=false \
  -Dorg.slf4j.simpleLogger.defaultLogLevel=warn \
  -q 2>/dev/null || mvn exec:java -Dexec.mainClass="demo.RaceConditionTest" -q

TEST_RESULT=$?

print_separator
print_header "RACE CONDITION FIX TEST COMPLETED"

if [ $TEST_RESULT -eq 0 ]; then
    print_success "🎉 RACE CONDITION TEST PASSED!"
    print_success "The race condition fix is working correctly."
else
    print_error "RACE CONDITION TEST FAILED."
    print_info "Review the results above to see error details."
fi

print_separator
print_info "Test location: src/main/java/demo/RaceConditionTest.java"
print_info "Documentation: README.md"

exit $TEST_RESULT
