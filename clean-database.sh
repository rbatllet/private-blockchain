#!/bin/bash

# Clean Database Script for Private Blockchain
# Quick script to clean corrupted SQLite database files
# Version: 1.0

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

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

echo -e "${BLUE}🧹 Private Blockchain Database Cleanup Script${NC}"
echo "=================================================="

# Get the script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

print_info "Working directory: $(pwd)"

# Check if we're in the correct directory
if [ ! -f "pom.xml" ]; then
    print_error "pom.xml not found. Please run this script from the project root directory."
    exit 1
fi

# Function to clean corrupted database files
clean_database() {
    print_info "Cleaning any corrupted database files..."
    
    # Clean SQLite database files in project root
    if [ -f "blockchain.db" ] || [ -f "blockchain.db-shm" ] || [ -f "blockchain.db-wal" ]; then
        print_info "Found existing database files, cleaning them..."
        rm -f blockchain.db blockchain.db-shm blockchain.db-wal 2>/dev/null || true
        print_success "Removed database files from project root"
    fi
    
    # If database exists but appears corrupted, try to repair it
    if [ -f "blockchain.db" ]; then
        print_info "Attempting to repair existing database..."
        if sqlite3 blockchain.db "PRAGMA wal_checkpoint(TRUNCATE);" 2>/dev/null; then
            print_success "WAL checkpoint completed"
        fi
        
        if sqlite3 blockchain.db "PRAGMA integrity_check;" > /dev/null 2>&1; then
            print_success "Database integrity check passed"
        else
            print_warning "Database appears corrupted, removing it..."
            rm -f blockchain.db blockchain.db-shm blockchain.db-wal 2>/dev/null || true
            print_success "Corrupted database removed"
        fi
    fi
    
    print_success "Database cleanup completed!"
}

# Check if sqlite3 is available
if ! command -v sqlite3 &> /dev/null; then
    print_warning "sqlite3 command not found - some repair operations will be skipped"
fi

# Run the cleanup
clean_database

echo ""
print_info "Cleanup complete. You can now run test scripts safely:"
print_info "  ./run_basic_tests.sh"
print_info "  ./run_advanced_tests.sh" 
print_info "  ./run_all_tests.sh"
