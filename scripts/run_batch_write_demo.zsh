#!/usr/bin/env zsh

# Batch Write API Demo - Phase 5.2 Features
# Demonstrates high-throughput batch write operations with deferred indexing

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

print_header "BATCH WRITE API DEMO - PHASE 5.2"
echo "Demonstrating high-throughput batch write operations:"
echo "  1. Single-block baseline performance"
echo "  2. Batch writing (50 blocks/batch)"
echo "  3. Write-only mode (deferred indexing)"
echo "  4. Batch indexing of deferred blocks"
echo "  5. Search verification"
echo ""
print_info "Expected: 5-50x throughput improvement with batch operations"
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
print_info "Running BatchWriteDemo..."
echo ""

mvn exec:java -Dexec.mainClass="demo.BatchWriteDemo" -Dexec.cleanupDaemonThreads=false -q

EXIT_CODE=$?

echo ""
if [ $EXIT_CODE -eq 0 ]; then
    print_success "Demo completed successfully!"
    echo ""
    print_info "Key Features Demonstrated:"
    echo "  ✅ Single-block baseline (100 blocks)"
    echo "  ✅ Batch write API (50 blocks/batch)"
    echo "  ✅ Write-only mode (skipIndexing=true)"
    echo "  ✅ Batch indexing (indexBlocksRange)"
    echo "  ✅ Search functionality verification"
    echo ""
    print_info "Phase 5.2 batch write API is production-ready! 🚀"
else
    print_error "Demo failed with exit code $EXIT_CODE"
    exit $EXIT_CODE
fi
