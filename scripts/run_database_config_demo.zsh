#!/usr/bin/env zsh

# Run Database Configuration Utilities Demo

SCRIPT_DIR="${0:A:h}"
PROJECT_ROOT="${SCRIPT_DIR:h}"

# Source common functions
source "${SCRIPT_DIR}/lib/common_functions.zsh" 2>/dev/null || {
    echo "❌ Error: Could not load common functions"
    exit 1
}

# Configuration
MAIN_CLASS="demo.DatabaseConfigurationDemo"
DEMO_NAME="Database Configuration Utilities Demo"

print_header "🔧 ${DEMO_NAME}"

# Change to project root
cd "${PROJECT_ROOT}" || exit 1

# Clean up test files
print_info "Cleaning up previous test files..."
rm -f test-config.properties test-config-export.properties test-config.json test-config.env
rm -f blockchain-test.db blockchain-test.db-shm blockchain-test.db-wal

# Compile the project
print_info "Compiling project..."
if ! mvn -q clean compile; then
    print_error "Compilation failed"
    exit 1
fi
print_success "Compilation successful"

# Run the demo
print_info "Running ${DEMO_NAME}..."
echo

if mvn -q exec:java -Dexec.mainClass="${MAIN_CLASS}" -Dexec.cleanupDaemonThreads=false; then
    echo
    print_success "Demo completed successfully"
    
    # Show generated files
    echo
    print_info "Generated files:"
    if [[ -f "test-config-export.properties" ]]; then
        echo "  📄 test-config-export.properties (exported configuration with masking)"
    fi
    if [[ -f "test-config.json" ]]; then
        echo "  📄 test-config.json (JSON export)"
    fi
    if [[ -f "test-config.env" ]]; then
        echo "  📄 test-config.env (environment variables export)"
    fi
    
    exit 0
else
    echo
    print_error "Demo execution failed"
    exit 1
fi
