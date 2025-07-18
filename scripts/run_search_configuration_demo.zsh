#!/usr/bin/env zsh

# Set script directory before changing directories
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Load common functions library
source "${SCRIPT_DIR}/lib/common_functions.zsh"

# Change to project root directory
cd "$SCRIPT_DIR/.."

print_step "🔍 Search Configuration Demo"
print_info "This demo showcases different EncryptionConfig options with SearchSpecialistAPI:"
print_info "- High Security Configuration (maximum protection)"
print_info "- Performance Configuration (optimized speed)"
print_info "- Balanced Configuration (good compromise)"
print_info "- Custom Configuration (tailored settings)"

print_info "💡 Key features demonstrated:"
print_info "   • Security vs Performance trade-offs"
print_info "   • Configurable encryption parameters"
print_info "   • Real-time performance metrics"
print_info "   • Flexible API configuration"

print_step "🚀 Executing SearchConfigurationDemo..."

# Compile the project first
print_info "Compiling project..."
mvn compile -q

# Execute the demo using Maven
mvn exec:java -Dexec.mainClass="demo.SearchConfigurationDemo" -q

print_success "Search Configuration demo completed! 🎉"