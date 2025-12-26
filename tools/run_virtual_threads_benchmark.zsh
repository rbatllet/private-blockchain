#!/usr/bin/env zsh

# Virtual Threads Benchmark Runner
# Executes comprehensive performance benchmarks for Java 25 Virtual Threads

SCRIPT_DIR="${0:a:h}"
PROJECT_ROOT="${SCRIPT_DIR}/.."

# Source common functions
source "${PROJECT_ROOT}/scripts/lib/common_functions.zsh"

print_header "Virtual Threads Performance Benchmark"

# Ensure genesis keys exist
ensure_genesis_keys

# Clean and compile
print_info "Compiling project..."
cd "$PROJECT_ROOT" || exit 1
mvn clean compile -q || {
    print_error "Compilation failed"
    exit 1
}

print_success "Compilation successful"
echo ""

# Run benchmark
print_info "Running Virtual Threads Benchmark Suite..."
print_info "This will take approximately 5-10 minutes..."
echo ""

mvn exec:java \
    -Dexec.mainClass="tools.VirtualThreadsBenchmark" \
    -Dexec.cleanupDaemonThreads=false \
    -q

benchmark_exit_code=$?

echo ""
if [ $benchmark_exit_code -eq 0 ]; then
    print_success "Benchmark completed successfully"
else
    print_error "Benchmark failed with exit code: $benchmark_exit_code"
    exit $benchmark_exit_code
fi

# Cleanup
cleanup_database

print_success "Virtual Threads Benchmark completed"
