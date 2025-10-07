#!/usr/bin/env zsh

# Performance Dashboard Script
# Real-time monitoring of blockchain performance using logs
# Version: 1.0.0

# Set script directory and navigate to project root
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="${SCRIPT_DIR}/.."

# Load common functions library
source "${PROJECT_ROOT}/scripts/lib/common_functions.zsh"

# Change to project root directory
cd "$PROJECT_ROOT"

print_header "BLOCKCHAIN PERFORMANCE DASHBOARD"
print_info "Real-time monitoring using our advanced logging system"
print_info "This dashboard analyzes logs to show live performance metrics"

# Check if we're in the correct directory
check_project_directory

# Check prerequisites
if ! check_java || ! check_maven; then
    exit 1
fi

# Ensure logs directory exists
mkdir -p logs

print_separator

print_step "📊 Starting Performance Dashboard..."
print_info "• Monitor blockchain metrics in real-time"
print_info "• Analyze log patterns for performance insights"
print_info "• Generate periodic performance reports"
print_info "• Track TPS, memory usage, and response times"

print_separator

print_info "🔄 Dashboard updates every second"
print_info "📁 Logs are saved in: logs/"
print_info "🛑 Press Ctrl+C to stop monitoring"

print_separator

# Run the log analysis dashboard
mvn exec:java -Dexec.mainClass="tools.LogAnalysisDashboard" -q