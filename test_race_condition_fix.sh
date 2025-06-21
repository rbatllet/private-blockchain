#!/usr/bin/env zsh

# CRITICAL RACE CONDITION FIX VALIDATION TEST
# This test validates that the getLastBlock() race condition has been resolved
# Usage: ./test_race_condition_fix.sh
# Version: 1.0.0

# Load shared functions for database cleanup and utility functions
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
if [ -f "$SCRIPT_DIR/scripts/shared-functions.sh" ]; then
    source "$SCRIPT_DIR/scripts/shared-functions.sh"
else
    # Fallback if shared functions not available
    print_info() { echo "ℹ️  $1"; }
    print_success() { echo "✅ $1"; }
    print_error() { echo "❌ $1"; }
    print_step() { echo "📋 $1"; }
    error_exit() { echo "❌ ERROR: $1"; exit 1; }
    clean_database() { rm -f blockchain.db blockchain.db-shm blockchain.db-wal 2>/dev/null || true; }
fi

print_step "🚨 TESTING RACE CONDITION FIX..."
print_step "================================================================"
print_info "This test will validate that the race condition in getLastBlock() has been resolved."
print_info "Previous issue: Multiple threads would see the same block number under extreme load."
print_info "Expected result: Each thread should see unique block numbers (no duplicates)."
print_info ""

# Check if we're in the correct directory
if [ ! -f "pom.xml" ]; then
    error_exit "pom.xml not found. Make sure to run this script from the project root directory."
fi

# Clean database before test
print_info "🧹 Cleaning database..."
clean_database

# Compile the project first
print_step "🔨 Compiling project..."
mvn clean compile -q

if [ $? -ne 0 ]; then
    error_exit "Compilation failed!"
fi

print_info ""
print_step "🚀 Running EXTREME THREAD SAFETY TEST (200 threads x 5 blocks = 1000 blocks)..."
print_info "This is the exact scenario that revealed the race condition..."
print_info "The RaceConditionTest.java class is located in src/main/java/demo/"
print_info ""

# Compile and run the test
print_step "🔨 Compiling and running race condition test..."
mvn exec:java -Dexec.mainClass="demo.RaceConditionTest" \
  -Djava.util.logging.config.file=src/main/resources/logging.properties \
  -Djakarta.persistence.show_sql=false \
  -Dorg.slf4j.simpleLogger.defaultLogLevel=warn \
  -q 2>/dev/null || mvn exec:java -Dexec.mainClass="demo.RaceConditionTest" -q

TEST_RESULT=$?

print_info ""
print_step "================================================================"
print_step "🎯 RACE CONDITION FIX TEST COMPLETED!"
print_step "================================================================"

if [ $TEST_RESULT -eq 0 ]; then
    print_success "🎉 RACE CONDITION TEST PASSED!"
    print_success "The race condition fix is working correctly."
else
    print_error "❌ RACE CONDITION TEST FAILED."
    print_info "Review the results above to see error details."
fi

print_info ""
print_info "Test location: src/main/java/demo/RaceConditionTest.java"
print_info "Documentation: README.md"

exit $TEST_RESULT
