#!/usr/bin/env zsh

# Script to run application with different logging profiles

# Set script directory before changing directories
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Load common functions library
source "${SCRIPT_DIR}/lib/common_functions.zsh"

# Change to project root directory
cd "$SCRIPT_DIR/.."

print_header "🚀 PRIVATE BLOCKCHAIN - PROFILE SELECTOR"
echo ""
echo "1) Development (detailed logging)"
echo "2) Production (minimal logging)"
echo ""
read -r "profile?Select profile [1-2]: "

case $profile in
    1)
        print_info "📊 Running with DEVELOPMENT profile..."

        # Clean database before running
        cleanup_database

        # Check prerequisites
        if ! check_java || ! check_maven; then
            exit 1
        fi

        # Compile project
        if ! compile_project; then
            exit 1
        fi

        print_info "🎬 Running BlockchainDemo with DEVELOPMENT profile..."
        # Uses default configuration (log4j2.xml)
        mvn exec:java -Dexec.mainClass="demo.BlockchainDemo" -Pdevelopment
        ;;
    2)
        print_info "🔐 Running with PRODUCTION profile..."

        # Clean database before running
        cleanup_database

        # Check prerequisites
        if ! check_java || ! check_maven; then
            exit 1
        fi

        # Compile project
        if ! compile_project; then
            exit 1
        fi

        print_info "🎬 Running BlockchainDemo with PRODUCTION profile..."
        # Uses production configuration automatically
        mvn exec:java -Dexec.mainClass="demo.BlockchainDemo" -Pproduction
        ;;
    *)
        print_error "❌ Invalid option"
        exit 1
        ;;
esac

# Final cleanup
cleanup_database

print_success "🎉 Demo completed successfully!"