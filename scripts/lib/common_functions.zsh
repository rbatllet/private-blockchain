#!/usr/bin/env zsh

# Common Functions Library for Private Blockchain Scripts
# This file contains all shared utility functions used by demo and test scripts
# Version: 2.0.0

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

# Utility functions for colored output
print_header() {
    echo -e "${BLUE}📊 $1${NC}"
    echo "==============================================="
}

print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_step() {
    echo -e "${PURPLE}📋 $1${NC}"
}

# Enhanced function to exit with error message
error_exit() {
    print_error "$1"
    exit 1
}

# Function to check if we're in the correct project directory
check_project_directory() {
    if [ ! -f "pom.xml" ]; then
        error_exit "pom.xml not found. Please run this script from the project root directory."
    fi
}

# Enhanced database cleanup function with corruption handling
cleanup_database() {
    # Allow skipping database cleanup with environment variable
    if [ "${SKIP_DB_CLEANUP:-}" = "true" ]; then
        print_info "Database cleanup skipped (SKIP_DB_CLEANUP=true)"
        return 0
    fi
    
    echo "🧹 Cleaning up database and temporary files..."
    
    # Clean SQLite database files in project root
    if [ -f "blockchain.db" ] || [ -f "blockchain.db-shm" ] || [ -f "blockchain.db-wal" ]; then
        print_info "Found existing database files, cleaning them..."
        rm -f blockchain.db blockchain.db-shm blockchain.db-wal 2>/dev/null || true
    fi
    
    # Clean database files using NULL_GLOB for safe wildcard handling
    setopt NULL_GLOB
    rm -f *.db *.sqlite *.sqlite3 2>/dev/null
    rm -f blockchain.db-shm blockchain.db-wal 2>/dev/null
    unsetopt NULL_GLOB
    
    # Clean SQLite database files in blockchain-data directory if it exists
    if [ -d "blockchain-data" ]; then
        if [ -f "blockchain-data/blockchain.db" ] || [ -f "blockchain-data/blockchain.db-shm" ] || [ -f "blockchain-data/blockchain.db-wal" ]; then
            print_info "Found existing database files in blockchain-data/, cleaning them..."
            rm -f blockchain-data/blockchain.db blockchain-data/blockchain.db-shm blockchain-data/blockchain.db-wal 2>/dev/null || true
        fi
    fi
    
    # Remove off-chain directories
    rm -rf off-chain-data off-chain-backup 2>/dev/null
    
    # Remove test export files if they exist
    setopt NULL_GLOB
    rm -f export_test_*.json 2>/dev/null
    rm -f export-test.json import-test.json 2>/dev/null
    rm -f corrupted_chain_recovery_*.json 2>/dev/null
    unsetopt NULL_GLOB
    
    # Remove key files if they exist
    setopt NULL_GLOB
    rm -f *.key *.pem 2>/dev/null
    unsetopt NULL_GLOB
    
    # Remove class files if they exist
    find . -maxdepth 1 -name "*.class" -delete 2>/dev/null || true
    
    # Remove log files if they exist
    find . -maxdepth 1 -name "*.log" -delete 2>/dev/null || true
    
    # If database exists but appears corrupted, try to repair it
    if [ -f "blockchain.db" ]; then
        print_info "Attempting to repair existing database..."
        sqlite3 blockchain.db "PRAGMA wal_checkpoint(TRUNCATE);" 2>/dev/null || true
        sqlite3 blockchain.db "PRAGMA integrity_check;" > /dev/null 2>&1 || {
            print_warning "Database appears corrupted, removing it..."
            rm -f blockchain.db blockchain.db-shm blockchain.db-wal 2>/dev/null || true
        }
    fi
    
    echo "✅ Cleanup complete"
}

# Function to check Java availability
check_java() {
    if ! command -v java &> /dev/null; then
        echo "❌ Java is not installed or not in PATH"
        echo "ℹ️  Please install Java 11 or higher"
        return 1
    fi
    
    # Check Java version
    local java_version=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [[ "$java_version" -lt 11 ]]; then
        echo "❌ Java version $java_version detected. Java 11 or higher is required."
        return 1
    fi
    
    return 0
}

# Function to check Maven availability
check_maven() {
    if ! command -v mvn &> /dev/null; then
        echo "❌ Maven is not installed or not in PATH"
        echo "ℹ️  Please install Maven 3.6 or higher"
        return 1
    fi
    return 0
}

# Enhanced function to compile the project
compile_project() {
    echo "📦 Compiling project..."
    if mvn compile -q; then
        echo "✅ Compilation successful"
        return 0
    else
        echo "❌ Compilation failed"
        return 1
    fi
}

# Function to compile project and tests
compile_project_with_tests() {
    print_step "Compiling project and tests..."
    mvn clean compile test-compile -q
    
    if [ $? -ne 0 ]; then
        error_exit "Project compilation failed. Please check for syntax errors."
    fi
    
    print_success "Project and tests compiled successfully"
}

# Function to create a colored separator
print_separator() {
    echo
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo
}

# Function to print a section header
print_section() {
    local title="$1"
    echo
    echo "📊 $title"
    echo "────────────────────────────────────────────────────"
}

# Function to handle errors
handle_error() {
    local error_msg="$1"
    echo
    echo "❌ ERROR: $error_msg"
    echo "ℹ️  Check the logs for more details"
    exit 1
}

# Function to clean logs directory
cleanup_logs() {
    if [[ -d "logs" ]]; then
        echo "🗑️  Cleaning up log files..."
        rm -f logs/*.log 2>/dev/null || true
        echo "✅ Log files cleaned"
    fi
}

# Function to ensure logs directory exists
ensure_logs_dir() {
    if [[ ! -d "logs" ]]; then
        mkdir -p logs
    fi
}

# Function to check if running as admin/root (useful for some tests)
check_not_root() {
    if [ "$EUID" -eq 0 ]; then
        print_warning "Running as root is not recommended for this application"
    fi
}

# Function to check disk space
check_disk_space() {
    local required_mb=${1:-100}  # Default 100MB
    local available_mb=$(df . | tail -1 | awk '{print $4}')
    available_mb=$((available_mb / 1024))  # Convert to MB
    
    if [ "$available_mb" -lt "$required_mb" ]; then
        print_warning "Low disk space: ${available_mb}MB available, ${required_mb}MB recommended"
        return 1
    fi
    return 0
}

# Function to wait for user confirmation
confirm() {
    local message=${1:-"Do you want to continue?"}
    echo -n "${message} [y/N]: "
    read -r response
    case "$response" in
        [yY]|[yY][eE][sS]) return 0 ;;
        *) return 1 ;;
    esac
}

# Functions are automatically available when sourcing in ZSH
# No need to export them