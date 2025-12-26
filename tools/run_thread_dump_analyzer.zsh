#!/usr/bin/env zsh

# Thread Dump Analyzer Runner
# Analyzes thread patterns for Java 25 Virtual Threads

SCRIPT_DIR="${0:a:h}"
PROJECT_ROOT="${SCRIPT_DIR}/.."

# Source common functions
source "${PROJECT_ROOT}/scripts/lib/common_functions.zsh"

print_header "Thread Dump Analyzer - Virtual Threads"

# Clean and compile
print_info "Compiling project..."
cd "$PROJECT_ROOT" || exit 1
mvn clean compile -q || {
    print_error "Compilation failed"
    exit 1
}

print_success "Compilation successful"
echo ""

# Run analyzer
print_info "Analyzing current thread dump..."
echo ""

mvn exec:java \
    -Dexec.mainClass="tools.ThreadDumpAnalyzer" \
    -Dexec.cleanupDaemonThreads=false \
    -q

analyzer_exit_code=$?

echo ""
if [ $analyzer_exit_code -eq 0 ]; then
    print_success "Thread dump analysis completed"
else
    print_error "Analysis failed with exit code: $analyzer_exit_code"
    exit $analyzer_exit_code
fi

print_success "Thread Dump Analyzer completed"
