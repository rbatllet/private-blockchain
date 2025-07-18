#!/usr/bin/env zsh

# Clean Database Script for Private Blockchain
# Quick script to clean corrupted SQLite database files
# Version: 1.0.1

# Set script directory and navigate to project root
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR/.."

# Load common functions library
source "${SCRIPT_DIR}/lib/common_functions.zsh"

print_header "🧹 Private Blockchain Database Cleanup Script"

print_info "Working directory: $(pwd)"

# Check if we're in the correct directory
check_project_directory

# Check if sqlite3 is available
if ! command -v sqlite3 &> /dev/null; then
    print_warning "sqlite3 command not found - some repair operations will be skipped"
fi

# Run the cleanup using the function from common_functions.zsh
cleanup_database

echo ""
print_info "Cleanup complete. You can now run test scripts safely:"
print_info "  ./scripts/run_basic_tests.zsh"
print_info "  ./scripts/run_advanced_tests.zsh" 
print_info "  ./scripts/run_all_tests.zsh"
