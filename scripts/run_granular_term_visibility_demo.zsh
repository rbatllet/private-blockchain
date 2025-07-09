#!/usr/bin/env zsh

# 🔐 Granular Term Visibility Demo Runner
# ======================================
# 
# This script demonstrates the advanced granular term visibility control feature
# that allows users to specify exactly which search terms should be public vs private.
# Version: 1.0.0

# Set script directory and navigate to project root
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR/.."

# Load common functions library
source "${SCRIPT_DIR}/lib/common_functions.zsh"

print_header "GRANULAR TERM VISIBILITY DEMO"

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
echo ""

# Run the granular term visibility demo
echo "🚀 Running Granular Term Visibility Demo..."
echo "============================================="

mvn exec:java \
    -Dexec.mainClass="demo.GranularTermVisibilityDemo" \
    -Dexec.cleanupDaemonThreads=false \
    -q

echo ""
echo "✅ Demo completed successfully!"
echo ""
echo "📋 What this demo showed:"
echo "  🔍 How to control individual term visibility"
echo "  🌍 Public terms searchable without password"  
echo "  🔐 Private terms require password authentication"
echo "  🏥 Medical record privacy example"
echo "  💰 Financial data protection example"
echo "  🔍 Search behavior differences"
echo ""
echo "💡 Key benefits:"
echo "  ✨ Fine-grained privacy control"
echo "  🎯 Compliance with data protection regulations"
echo "  🔒 Sensitive data protection while maintaining searchability"
echo "  🚀 Flexible term-level security configuration"