#!/usr/bin/env zsh

# Script to run Enhanced Dangerous Delete Demo for the Blockchain
# Usage: ./run_enhanced_dangerous_delete_demo.zsh
# Version: 1.0.0

# Set script directory before changing directories
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Load common functions library
source "${SCRIPT_DIR}/lib/common_functions.zsh"

# Change to project root directory
cd "$SCRIPT_DIR/.."

echo "💀 ENHANCED DANGEROUS DELETE VALIDATION DEMO"
echo "============================================="
echo ""

# Check if we're in the correct directory
if [ ! -f "pom.xml" ]; then
    print_error "pom.xml not found. Make sure to run this script from the project root directory."
    exit 1
fi

print_info "🏠 Project directory: $(pwd)"

# Check prerequisites
print_info "🔍 Checking prerequisites..."

if ! check_java; then
    exit 1
fi

if ! check_maven; then
    exit 1
fi

print_success "All prerequisites satisfied"

# Clean and compile
cleanup_database

if ! compile_project; then
    exit 1
fi

print_separator

# Run the Enhanced Dangerous Delete Demo
print_info "🎬 Running Enhanced Dangerous Delete Demo..."
print_step "============================================"
print_info ""
print_info "🚀 Launching EnhancedDangerousDeleteDemo..."
java -cp "target/classes:$(mvn -q dependency:build-classpath -Dmdep.outputFile=/dev/stdout)" \
    demo.EnhancedDangerousDeleteDemo
DEMO_RESULT=$?
print_info ""

# Summary
print_step "📊 ENHANCED DANGEROUS DELETE DEMO SUMMARY"
print_step "========================================="
if [ $DEMO_RESULT -eq 0 ]; then
    print_success "✅ Enhanced Dangerous Delete Demo: COMPLETED"
else
    print_error "❌ Enhanced Dangerous Delete Demo: FAILED"
fi

print_info ""
print_info "🔍 Validation Features Demonstrated:"
print_info "  • 🔍 Enhanced chain validation with detailed results"
print_info "  • ⚖️ Comparison between old and new validation methods"
print_info "  • ✅ Safe key deletion (inactive users)"
print_info "  • 💀 Dangerous forced key deletion with tracking"
print_info "  • 📊 Multiple chain perspectives (full/valid/orphaned)"
print_info "  • 📋 Comprehensive audit reports"
print_info "  • 🎯 Legacy compatibility maintenance"
print_info ""

print_info "💡 Key Learnings:"
print_info "  • Granular validation information vs binary results"
print_info "  • Structural integrity vs authorization compliance"
print_info "  • Audit trail preservation during dangerous operations"
print_info "  • Clear block orphaning and recovery mechanisms"
print_info ""

print_separator

# Display next steps
print_info "Next steps:"
echo "  1. Run 'scripts/run_blockchain_demo.zsh' for basic blockchain operations"
echo "  2. Run 'scripts/run_advanced_thread_safety_tests.zsh' for thread safety testing"
echo "  3. Review the demo location: src/main/java/demo/EnhancedDangerousDeleteDemo.java"
echo ""

# Final cleanup
cleanup_database > /dev/null 2>&1

# Exit with appropriate code
if [ $DEMO_RESULT -eq 0 ]; then
    print_success "Enhanced dangerous delete demo completed successfully!"
    exit 0
else
    print_error "Enhanced dangerous delete demo failed. Check the output above."
    exit 1
fi
