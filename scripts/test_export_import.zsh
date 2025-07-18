#!/usr/bin/env zsh

# Export/Import Data Consistency Test
# Version: 1.0.0

# Set script directory before changing directories
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Load common functions library
source "${SCRIPT_DIR}/lib/common_functions.zsh"

# Change to project root directory
cd "$SCRIPT_DIR/.."

print_header "EXPORT/IMPORT DATA CONSISTENCY TEST"

# Check if we're in the correct directory
check_project_directory

# Check prerequisites
if ! check_java || ! check_maven; then
    exit 1
fi

# Clean and compile
cleanup_database

if ! compile_project; then
    exit 1
fi

print_step "📝 Running export/import test using demo class..."

# Run the test using Maven
print_step "🚀 Running export/import test..."
mvn -q exec:java -Dexec.mainClass="demo.TestExportImport"

print_separator

print_header "FINAL VALIDATION"

# Check exported files
if [[ -f "export-test.json" ]]; then
    print_success "Export file created: export-test.json"
    print_info "📏 Export file size: $(du -h export-test.json | cut -f1)"
else
    print_error "Export file missing"
fi

# Check off-chain files
if [[ -d "off-chain-data" ]]; then
    local file_count=$(find off-chain-data -type f | wc -l)
    print_info "📁 Off-chain files after import: $file_count"
    if [[ $file_count -gt 0 ]]; then
        print_success "Off-chain files properly restored"
    fi
else
    print_error "Off-chain directory not found"
fi

print_success "EXPORT/IMPORT VALIDATION COMPLETE"

# Optional: Clean up test files after completion
if [[ "${KEEP_TEST_FILES:-false}" != "true" ]]; then
    print_info "🧹 Cleaning up test files..."
    cleanup_database
    setopt NULL_GLOB
    rm -f export-test.json import-test.json 2>/dev/null
    rm -rf off-chain-data off-chain-backup 2>/dev/null
    unsetopt NULL_GLOB
    print_success "Test environment cleaned"
else
    print_info "💾 Test files preserved (KEEP_TEST_FILES=true)"
fi
