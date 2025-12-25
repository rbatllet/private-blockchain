#!/usr/bin/env zsh

# ============================================================================
# Indexing Sync Demo - Private Blockchain
# ============================================================================
# Demonstrates synchronous indexing patterns from INDEXING_GUIDE.md:
# - Example 1: Unit Test Pattern (sync for assertions)
# - Example 2: Demo Script Pattern (sync for sequential flow)
# - Example 3: Background Job Pattern (async for non-blocking)
# - Example 4: CLI Tool Pattern (sync for user expectations)
# ============================================================================

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

# Script configuration
SCRIPT_NAME="$(basename "$0")"
SCRIPT_DESCRIPTION="Demonstrate synchronous vs asynchronous indexing patterns from documentation"

# Show usage if requested
if [ "$1" = "--help" ] || [ "$1" = "-h" ]; then
    show_usage "$SCRIPT_NAME" "$SCRIPT_DESCRIPTION"
    exit 0
fi

# Main script execution
main() {
    # Check if we're in the correct directory
    if [[ ! -f "pom.xml" ]]; then
        print_error "pom.xml not found. Make sure to run this script from the project root directory."
        exit 1
    fi
    
    print_info "🏠 Project directory: $(pwd)"
    
    # Check prerequisites
    print_info "🔍 Checking prerequisites..."
    
    if ! check_java; then
        exit 1
    fi
    
    if ! check_maven; then
        exit 1
    fi
    
    # Check if genesis admin keys exist
    if [[ ! -f "./keys/genesis-admin.private" ]] || [[ ! -f "./keys/genesis-admin.public" ]]; then
        print_warning "Genesis admin keys not found. Generating..."
        ./tools/generate_genesis_keys.zsh
    fi
    
    print_success "All prerequisites satisfied"
    
    # Clean and compile
    cleanup_database

# Ensure genesis admin keys exist (auto-generates if missing)
ensure_genesis_keys
    
    if ! compile_project; then
        exit 1
    fi
    
    # Run the demo
    print_separator
    print_info "🚀 Running Indexing Sync Demo (4 Examples from Documentation)"
    print_separator
    
    mvn exec:java -q \
        -Dexec.mainClass="demo.IndexingSyncDemo" \
        -Dexec.cleanupDaemonThreads=false
    
    local exit_code=$?
    
    print_separator
    if [[ $exit_code -eq 0 ]]; then
        print_success "Demo completed successfully!"
    else
        print_error "Demo failed with exit code: $exit_code"
    fi
    
    return $exit_code
}

# Run main function
main "$@"
