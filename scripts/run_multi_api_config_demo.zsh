#!/usr/bin/env zsh

# Set script directory before changing directories
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Load common functions library
source "${SCRIPT_DIR}/lib/common_functions.zsh"

# Change to project root directory
cd "$SCRIPT_DIR/.."

print_step "🔐 Multi-API EncryptionConfig Demo"
print_info "This demo shows how EncryptionConfig can be used across all APIs:"
print_info "- UserFriendlyEncryptionAPI with different configurations"
print_info "- SearchSpecialistAPI with different configurations"
print_info "- All APIs working together with unified configuration"

print_info "💡 Key benefits of using EncryptionConfig:"
print_info "   • Consistent security policies across APIs"
print_info "   • Flexible performance vs security trade-offs"
print_info "   • Centralized configuration management"
print_info "   • Easy switching between security levels"

print_step "🚀 Executing MultiAPIConfigurationDemo..."

# Compile the project first
print_info "Compiling project..."
mvn compile -q

# Execute the demo using Maven
mvn exec:java -Dexec.mainClass="demo.MultiAPIConfigurationDemo" -q

print_success "Multi-API EncryptionConfig demo completed! 🎉"