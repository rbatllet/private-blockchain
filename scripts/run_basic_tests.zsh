#!/usr/bin/env zsh

# Script to run Basic Core Functions tests for the Blockchain
# Usage: ./run_basic_tests.zsh
# Version: 1.0.1

# Set script directory
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Load common functions library
source "${SCRIPT_DIR}/lib/common_functions.zsh"

# Change to project root directory
cd "$SCRIPT_DIR/.."

# Clean database at start to prevent corruption
cleanup_database > /dev/null 2>&1

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

# Run basic core functions test using direct Java execution
java -cp "target/classes:$(mvn -q dependency:build-classpath -Dmdep.outputFile=/dev/stdout)" \
  -Djava.util.logging.config.file=src/main/resources/logging.properties \
  -Djakarta.persistence.show_sql=false \
  -Dorg.slf4j.simpleLogger.defaultLogLevel=warn \
  demo.CoreFunctionsDemo 2>/dev/null

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
print_info "Test location: src/main/java/demo/CoreFunctionsDemo.java"
print_info "Documentation: README.md"

exit $TEST_RESULT
