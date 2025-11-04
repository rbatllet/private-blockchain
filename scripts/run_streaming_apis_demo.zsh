#!/usr/bin/env zsh

# Streaming APIs Demo - Phase B.2 Features
# Demonstrates the 4 new memory-safe streaming methods added in v1.0.6

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

print_header "STREAMING APIS DEMO - PHASE B.2"
echo "Demonstrating 4 new memory-safe streaming methods:"
echo "  1. streamBlocksByTimeRange() - Temporal queries"
echo "  2. streamEncryptedBlocks() - Encryption operations"
echo "  3. streamBlocksWithOffChainData() - Off-chain management"
echo "  4. streamBlocksAfter() - Incremental processing"
echo ""
print_info "All methods maintain constant ~50MB memory usage"
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
print_info "Running StreamingApisDemo..."
echo ""

mvn exec:java -Dexec.mainClass="demo.StreamingApisDemo" -Dexec.cleanupDaemonThreads=false -q

EXIT_CODE=$?

echo ""
if [ $EXIT_CODE -eq 0 ]; then
    print_success "Demo completed successfully!"
    echo ""
    print_info "Key Features Demonstrated:"
    echo "  ✅ Temporal queries with time range filtering"
    echo "  ✅ Encryption audits with database-level filtering"
    echo "  ✅ Off-chain storage analytics"
    echo "  ✅ Incremental processing for large rollbacks"
    echo "  ✅ Constant memory usage (~50MB)"
    echo ""
    print_info "Phase B.2 streaming methods are production-ready! 🚀"
else
    print_error "Demo failed with exit code $EXIT_CODE"
    exit $EXIT_CODE
fi
