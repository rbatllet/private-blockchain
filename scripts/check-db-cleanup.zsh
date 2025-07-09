#!/usr/bin/env zsh

# Script to automatically apply database cleanup fix to all run_*.zsh scripts
# This script ensures ALL run_*.zsh scripts include the database cleanup functionality
# Version: 1.0.1

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

# Standardized function to exit with error
error_exit() {
    print_error "❌ ERROR: $1"
    print_error "❌ Aborting operation."
    exit 1
}

print_step() {
    echo -e "${BLUE}📝 $1${NC}"
}

print_step "🔧 Database Cleanup Fix Applicator"
echo "========================================"

# Get the script directory and go to project root
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_ROOT"

print_info "Working directory: $(pwd)"

# Check if we're in the correct directory
if [ ! -f "pom.xml" ]; then
    error_exit "pom.xml not found. Please run this script from the project root directory."
fi

# Check if shared functions exist
if [ ! -f "scripts/lib/common_functions.zsh" ]; then
    error_exit "scripts/lib/common_functions.zsh not found. Please ensure the shared functions are installed."
fi

print_info "Scanning for run_*.zsh scripts..."

# Find all run_*.zsh scripts
RUN_SCRIPTS=($(find scripts -maxdepth 1 -name "run_*.zsh" -type f))

if [ ${#RUN_SCRIPTS[@]} -eq 0 ]; then
    print_warning "No run_*.zsh scripts found in the scripts directory."
    exit 0
fi

print_info "Found ${#RUN_SCRIPTS[@]} run_*.zsh scripts:"
for script in "${RUN_SCRIPTS[@]}"; do
    echo "  - $(basename "$script")"
done

echo

# Check each script for the shared functions pattern
SCRIPTS_NEED_UPDATE=()
SCRIPTS_UP_TO_DATE=()

for script in "${RUN_SCRIPTS[@]}"; do
    script_name=$(basename "$script")
    
    if grep -q "source.*common_functions.zsh" "$script"; then
        print_success "$script_name: Already uses shared functions"
        SCRIPTS_UP_TO_DATE+=("$script_name")
    else
        print_warning "$script_name: Needs database cleanup fix"
        SCRIPTS_NEED_UPDATE+=("$script_name")
    fi
done

echo

if [ ${#SCRIPTS_NEED_UPDATE[@]} -eq 0 ]; then
    print_success "All run_*.zsh scripts are up to date! ✨"
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
print_info "  1. Backup your scripts: cp scripts/run_*.zsh backup/"
print_info "  2. Update scripts manually using scripts/run_template.zsh as reference"
print_info "  3. Ensure all scripts source scripts/lib/common_functions.zsh"
print_info "  4. Test scripts after updating"

echo
print_info "For new scripts, use: cp scripts/run_template.zsh run_your_new_test.zsh"

echo
print_warning "Manual update required. Automatic conversion could break custom logic."
print_info "Use the template and shared functions for consistent database cleanup."
