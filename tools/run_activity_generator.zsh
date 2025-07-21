#!/usr/bin/env zsh

# Activity Generator Script
# Generates blockchain activity for dashboard testing
# Version: 1.0.0

# Set script directory and navigate to project root
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Load common functions library
source "${SCRIPT_DIR}/../scripts/lib/common_functions.zsh"

# Change to project root directory
cd "$SCRIPT_DIR/.."

print_header "BLOCKCHAIN ACTIVITY GENERATOR"
print_info "This generates blockchain activity to test the dashboard"
print_info "Run this in one terminal while the dashboard runs in another"

# Check if we're in the correct directory
check_project_directory

# Check prerequisites
if ! check_java || ! check_maven; then
    exit 1
fi

print_separator

print_step "🚀 Starting activity generation..."
print_info "• Creates regular and encrypted blocks"
print_info "• Performs searches and validations"
print_info "• Generates off-chain data"
print_info "• Runs for 30 seconds with periodic activity"

print_separator

# Run the activity generator
mvn exec:java -Dexec.mainClass="tools.GenerateBlockchainActivity" -q