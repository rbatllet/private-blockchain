#!/usr/bin/env zsh

# Run JPA Configuration Storage Demo

SCRIPT_DIR="${0:A:h}"
PROJECT_ROOT="${SCRIPT_DIR:h}"

# Source common functions
source "${SCRIPT_DIR}/lib/common_functions.zsh" 2>/dev/null || {
    echo "❌ Error: Could not load common functions"
    exit 1
}

# Configuration
MAIN_CLASS="demo.JPAConfigurationStorageDemo"
DEMO_NAME="JPA Configuration Storage Demo"

print_header "📊 ${DEMO_NAME}"

# Change to project root
cd "${PROJECT_ROOT}" || exit 1

# Clean up test databases
if [[ -f "blockchain-dev.db" ]]; then
    print_info "Cleaning up previous test databases..."
    rm -f blockchain-dev.db blockchain-dev.db-shm blockchain-dev.db-wal
fi

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
    exit 0
else
    echo
    print_error "Demo execution failed"
    exit 1
fi
