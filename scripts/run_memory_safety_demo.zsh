#!/usr/bin/env zsh

# Memory Safety Demo - Phase A Features
# Demonstrates memory safety improvements from Phase A.1-A.8

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
PROJECT_ROOT="$(pwd)"

# Trap to ensure cleanup on exit
trap cleanup_database EXIT INT TERM

cd "$PROJECT_ROOT"

print_header "MEMORY SAFETY DEMO - PHASE A"
echo "Demonstrating critical memory safety improvements from v1.0.6+:"
echo "  1. Breaking changes: maxResults validation"
echo "  2. Batch processing with processChainInBatches()"
echo "  3. Streaming validation with validateChainStreaming()"
echo "  4. Memory-safe search methods"
echo "  5. Memory limits and safety constants"
echo "  6. Before vs After comparison"
echo ""
print_info "Phase A.1-A.8: Critical Memory Safety Refactoring"
echo ""

# Compile if needed
if [ ! -d "target/classes" ]; then
    print_info "Compiling project..."
    mvn clean compile -q
    if [ $? -ne 0 ]; then
        print_error "Compilation failed"
        exit 1
    fi
    print_success "Compilation successful"
fi

# Run the demo
print_info "Running MemorySafetyDemo (creates 1000 blocks for testing)..."
echo ""

mvn exec:java -Dexec.mainClass="demo.MemorySafetyDemo" -Dexec.cleanupDaemonThreads=false -q

EXIT_CODE=$?

echo ""
if [ $EXIT_CODE -eq 0 ]; then
    print_success "Demo completed successfully!"
    echo ""
    print_info "Key Improvements Demonstrated:"
    echo "  ✅ maxResults parameter validation prevents memory bombs"
    echo "  ✅ Batch processing maintains constant memory usage"
    echo "  ✅ Streaming validation supports unlimited blockchain size"
    echo "  ✅ Search methods have automatic safety limits"
    echo "  ✅ Centralized memory safety constants"
    echo "  ✅ Significant memory reduction vs v1.0.5"
    echo ""
    print_info "Phase A memory safety is production-ready! 🚀"
else
    print_error "Demo failed with exit code $EXIT_CODE"
    exit $EXIT_CODE
fi
