#!/usr/bin/env zsh

# Script to run Enhanced Dangerous Delete Demo for the Blockchain
# Usage: ./run_enhanced_dangerous_delete_demo.sh
# Version: 1.0.0

# Load shared functions for database cleanup
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
if [ -f "$SCRIPT_DIR/scripts/shared-functions.sh" ]; then
    source "$SCRIPT_DIR/scripts/shared-functions.sh"
    # Clean database at start to prevent corruption
    clean_database > /dev/null 2>&1
fi

print_step "=== 💀 ENHANCED DANGEROUS DELETE VALIDATION DEMO RUNNER ==="
print_info "Project directory: $(pwd)"
print_info ""

# Check if we're in the correct directory
if [ ! -f "pom.xml" ]; then
    error_exit "pom.xml not found. Make sure to run this script from the project root directory."
fi

# Clean and compile
print_step "Compiling project..."
mvn clean compile -q > /dev/null 2>&1
if [ $? -ne 0 ]; then
    mvn clean compile
    error_exit "Compilation failed!"
fi
print_success "Compilation successful"
print_info ""

# Run the Enhanced Dangerous Delete Demo
print_info "🎬 Running Enhanced Dangerous Delete Demo..."
print_step "============================================"
print_info ""
mvn compile exec:java -Dexec.mainClass="demo.EnhancedDangerousDeleteDemo" -q
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

# Final cleanup
if command -v clean_database &> /dev/null; then
    clean_database > /dev/null 2>&1
fi

# Exit with appropriate code
if [ $DEMO_RESULT -eq 0 ]; then
    print_success "Enhanced dangerous delete demo completed successfully!"
    print_info "Demo location: src/main/java/demo/EnhancedDangerousDeleteDemo.java"
    print_info "Documentation: README.md"
    exit 0
else
    error_exit "Enhanced dangerous delete demo failed. Check the output above."
fi