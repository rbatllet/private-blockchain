#!/bin/bash

# Shared Functions Library for Private Blockchain Scripts
# This file contains common utility functions used by all test scripts
# Version: 1.0
#
# Usage: source ./scripts/shared-functions.sh

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

# Utility functions for colored output
print_header() {
    echo -e "${BLUE}$1${NC}"
    echo "=============================================="
}

print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_step() {
    echo -e "${PURPLE}📋 $1${NC}"
}

# Function to check if we're in the correct project directory
check_project_directory() {
    if [ ! -f "pom.xml" ]; then
        print_error "pom.xml not found. Please run this script from the project root directory."
        exit 1
    fi
}

# Function to clean corrupted database files
# This is the core function that prevents SQLITE_IOERR_SHORT_READ errors
clean_database() {
    # Allow skipping database cleanup with environment variable
    if [ "${SKIP_DB_CLEANUP:-}" = "true" ]; then
        print_info "Database cleanup skipped (SKIP_DB_CLEANUP=true)"
        return 0
    fi
    
    print_info "Cleaning any corrupted database files..."
    
    # Clean SQLite database files in project root
    if [ -f "blockchain.db" ] || [ -f "blockchain.db-shm" ] || [ -f "blockchain.db-wal" ]; then
        print_info "Found existing database files, cleaning them..."
        rm -f blockchain.db blockchain.db-shm blockchain.db-wal 2>/dev/null || true
    fi
    
    # Clean SQLite database files in blockchain-data directory if it exists
    if [ -d "blockchain-data" ]; then
        if [ -f "blockchain-data/blockchain.db" ] || [ -f "blockchain-data/blockchain.db-shm" ] || [ -f "blockchain-data/blockchain.db-wal" ]; then
            print_info "Found existing database files in blockchain-data/, cleaning them..."
            rm -f blockchain-data/blockchain.db blockchain-data/blockchain.db-shm blockchain-data/blockchain.db-wal 2>/dev/null || true
        fi
    fi
    
    # If database exists but appears corrupted, try to repair it
    if [ -f "blockchain.db" ]; then
        print_info "Attempting to repair existing database..."
        sqlite3 blockchain.db "PRAGMA wal_checkpoint(TRUNCATE);" 2>/dev/null || true
        sqlite3 blockchain.db "PRAGMA integrity_check;" > /dev/null 2>&1 || {
            print_warning "Database appears corrupted, removing it..."
            rm -f blockchain.db blockchain.db-shm blockchain.db-wal 2>/dev/null || true
        }
    fi
    
    print_success "Database cleanup completed"
}
# Function to compile the project
compile_project() {
    print_step "Compiling project and tests..."
    mvn clean compile test-compile -q
    
    if [ $? -ne 0 ]; then
        print_error "Compilation failed. Please check the errors above."
        exit 1
    fi
    
    print_success "Compilation successful!"
}

# Function to initialize test environment
# This should be called at the beginning of every test script
init_test_environment() {
    print_header "INITIALIZING TEST ENVIRONMENT"
    print_info "Project directory: $(pwd)"
    echo
    
    check_project_directory
    clean_database
    
    echo
}

# Function to clear database between test suites
clear_database_between_tests() {
    if [ -f "blockchain.db" ]; then
        rm -f blockchain.db blockchain.db-shm blockchain.db-wal 2>/dev/null || true
    fi
}

# Function to check if required tools are available
check_dependencies() {
    local missing_deps=()
    
    if ! command -v mvn &> /dev/null; then
        missing_deps+=("maven")
    fi
    
    if ! command -v java &> /dev/null; then
        missing_deps+=("java")
    fi
    
    if ! command -v sqlite3 &> /dev/null; then
        print_warning "sqlite3 command not found - some repair operations will be skipped"
    fi
    
    if [ ${#missing_deps[@]} -ne 0 ]; then
        print_error "Missing required dependencies: ${missing_deps[*]}"
        print_info "Please install the missing dependencies and try again."
        exit 1
    fi
}

# Function to show script usage information
show_usage() {
    local script_name="$1"
    local description="$2"
    
    echo -e "${BLUE}Usage: $script_name${NC}"
    echo "Description: $description"
    echo
    echo "Environment Variables:"
    echo "  SKIP_DB_CLEANUP=true     - Skip database cleanup (for debugging)"
    echo "  SKIP_UNIT_TESTS=true     - Skip Maven unit tests (if applicable)"
    echo
    echo "Examples:"
    echo "  ./$script_name                    # Normal execution"
    echo "  SKIP_DB_CLEANUP=true ./$script_name  # Skip database cleanup"
    echo
}

# Function to create a test summary
print_test_summary() {
    local total_tests="$1"
    local passed_tests="$2"
    local failed_tests="$3"
    
    print_header "TEST SUMMARY"
    echo "🎯 Total Tests: $total_tests"
    echo "✅ Passed: $passed_tests"
    echo "❌ Failed: $failed_tests"
    
    if [ "$failed_tests" -eq 0 ]; then
        echo "📈 Success Rate: 100%"
        print_success "ALL TESTS PASSED! 🎉"
    else
        local success_rate=$((passed_tests * 100 / total_tests))
        echo "📈 Success Rate: ${success_rate}%"
        print_warning "Some tests failed. Please review the output above."
    fi
}

# Export functions so they can be used by sourcing scripts
export -f print_header print_info print_success print_warning print_error print_step
export -f check_project_directory clean_database compile_project init_test_environment
export -f clear_database_between_tests check_dependencies show_usage print_test_summary
