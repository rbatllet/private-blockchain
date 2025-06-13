#!/bin/bash

# Script to automatically apply database cleanup fix to all run_*.sh scripts
# This script ensures ALL run_*.sh scripts include the database cleanup functionality
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

echo -e "${BLUE}🔧 Database Cleanup Fix Applicator${NC}"
echo "========================================"

# Get the script directory and go to project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_ROOT"

print_info "Working directory: $(pwd)"

# Check if we're in the correct directory
if [ ! -f "pom.xml" ]; then
    print_error "pom.xml not found. Please run this script from the project root directory."
    exit 1
fi

# Check if shared functions exist
if [ ! -f "scripts/shared-functions.sh" ]; then
    print_error "scripts/shared-functions.sh not found. Please ensure the shared functions are installed."
    exit 1
fi

print_info "Scanning for run_*.sh scripts..."

# Find all run_*.sh scripts
RUN_SCRIPTS=($(find . -maxdepth 1 -name "run_*.sh" -type f))

if [ ${#RUN_SCRIPTS[@]} -eq 0 ]; then
    print_warning "No run_*.sh scripts found in the current directory."
    exit 0
fi

print_info "Found ${#RUN_SCRIPTS[@]} run_*.sh scripts:"
for script in "${RUN_SCRIPTS[@]}"; do
    echo "  - $(basename "$script")"
done

echo

# Check each script for the shared functions pattern
SCRIPTS_NEED_UPDATE=()
SCRIPTS_UP_TO_DATE=()

for script in "${RUN_SCRIPTS[@]}"; do
    script_name=$(basename "$script")
    
    if grep -q "source.*shared-functions.sh" "$script"; then
        print_success "$script_name: Already uses shared functions"
        SCRIPTS_UP_TO_DATE+=("$script_name")
    else
        print_warning "$script_name: Needs database cleanup fix"
        SCRIPTS_NEED_UPDATE+=("$script_name")
    fi
done

echo

if [ ${#SCRIPTS_NEED_UPDATE[@]} -eq 0 ]; then
    print_success "All run_*.sh scripts are up to date! ✨"
    echo
    print_info "Summary:"
    print_info "  ✅ Up to date: ${#SCRIPTS_UP_TO_DATE[@]} scripts"
    print_info "  🔧 Need update: 0 scripts"
    exit 0
fi

print_warning "Found ${#SCRIPTS_NEED_UPDATE[@]} scripts that need the database cleanup fix:"
for script in "${SCRIPTS_NEED_UPDATE[@]}"; do
    echo "  - $script"
done

echo
print_info "Recommendation:"
print_info "  1. Backup your scripts: cp run_*.sh backup/"
print_info "  2. Update scripts manually using scripts/run_template.sh as reference"
print_info "  3. Ensure all scripts source scripts/shared-functions.sh"
print_info "  4. Test scripts after updating"

echo
print_info "For new scripts, use: cp scripts/run_template.sh run_your_new_test.sh"

echo
print_warning "Manual update required. Automatic conversion could break custom logic."
print_info "Use the template and shared functions for consistent database cleanup."
