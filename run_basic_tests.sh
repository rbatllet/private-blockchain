#!/usr/bin/env zsh

# Script to run Basic Core Functions tests for the Blockchain
# Usage: ./run_basic_tests.sh
# Version: 1.0.1

# Load shared functions for database cleanup (but preserve original structure)
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
if [ -f "$SCRIPT_DIR/scripts/shared-functions.sh" ]; then
    source "$SCRIPT_DIR/scripts/shared-functions.sh"
    # Clean database at start to prevent corruption
    clean_database > /dev/null 2>&1
fi

print_step "=== 🧪 BLOCKCHAIN BASIC CORE FUNCTIONS TEST RUNNER ==="
print_info "Project directory: $(pwd)"
print_info ""

# Check if we're in the correct directory
if [ ! -f "pom.xml" ]; then
    error_exit "pom.xml not found. Make sure to run this script from the project root directory."
fi

print_step "Compiling the project..."
mvn clean compile test-compile -q

if [ $? -ne 0 ]; then
    error_exit "Compilation error. Please review the errors above."
fi

print_success "Compilation successful!"
print_info ""

print_info "🧪 Running Basic Core Functions demo..."
print_info ""

# Run basic core functions test (suppress most logs)
mvn exec:java -Dexec.mainClass="com.rbatllet.demo.CoreFunctionsDemo" \
  -Djava.util.logging.config.file=src/main/resources/logging.properties \
  -Djakarta.persistence.show_sql=false \
  -Dorg.slf4j.simpleLogger.defaultLogLevel=warn \
  -q 2>/dev/null || mvn exec:java -Dexec.mainClass="com.rbatllet.demo.CoreFunctionsDemo" -q

TEST_RESULT=$?

print_info ""
print_step "=== 📊 SUMMARY ==="

if [ $TEST_RESULT -eq 0 ]; then
    print_success "🎉 BASIC CORE FUNCTIONS TEST PASSED!"
    print_success "The basic core functions of the blockchain work correctly."
else
    print_error "❌ BASIC CORE FUNCTIONS TEST FAILED."
    print_info "Review the results above to see error details."
fi

print_info ""
print_info "Test location: src/main/java/com.rbatllet.demo/CoreFunctionsDemo.java"
print_info "Documentation: README.md"

exit $TEST_RESULT
