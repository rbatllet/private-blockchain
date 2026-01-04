#!/usr/bin/env zsh

# Run SecureKeyStorage Demo with AES-256-GCM encryption

SCRIPT_DIR="${0:A:h}"
PROJECT_ROOT="${SCRIPT_DIR:h}"

# Source common functions
source "${SCRIPT_DIR}/lib/common_functions.zsh" 2>/dev/null || {
    echo "❌ Error: Could not load common functions"
    exit 1
}

# Configuration
MAIN_CLASS="demo.SecureKeyStorageDemo"
DEMO_NAME="SecureKeyStorage Demo (AES-256-GCM)"

print_header "🔐 ${DEMO_NAME}"

# Change to project root
cd "${PROJECT_ROOT}" || exit 1

# Clean up previous keys directory if it exists
if [[ -d "keys" ]]; then
    print_info "Cleaning up previous keys directory..."
    rm -rf keys
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

    # Show keys directory if it exists
    if [[ -d "keys" ]]; then
        echo
        print_info "Private keys directory contents:"
        ls -lh keys/ | grep -v "^total"
        echo
        TOTAL_SIZE=$(du -sh keys/ | cut -f1)
        print_info "Total directory size: ${TOTAL_SIZE}"
    fi

    exit 0
else
    echo
    print_error "Demo execution failed"
    exit 1
fi
