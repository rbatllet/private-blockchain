#!/usr/bin/env zsh

# Generate genesis-admin key pair for tests and demos
# This script creates ./keys/genesis-admin.private and ./keys/genesis-admin.public
# Version: 1.0.0
#
# Usage:
#   ./tools/generate_genesis_keys.zsh

# Set script directory before changing directories
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Load common functions library from scripts/lib
if [ -f "${SCRIPT_DIR}/../scripts/lib/common_functions.zsh" ]; then
    source "${SCRIPT_DIR}/../scripts/lib/common_functions.zsh"
else
    echo "❌ Error: common_functions.zsh not found. Please ensure the scripts/lib directory exists."
    exit 1
fi

# Change to project root directory
cd "$SCRIPT_DIR/.."

# Script configuration
SCRIPT_NAME="$(basename "$0")"
SCRIPT_DESCRIPTION="Generate genesis-admin key pair for tests and demos"

# Show usage if requested
if [ "$1" = "--help" ] || [ "$1" = "-h" ]; then
    show_usage "$SCRIPT_NAME" "$SCRIPT_DESCRIPTION"
    echo ""
    echo "ℹ️  This script generates bootstrap admin keys required by tests and demos."
    echo "ℹ️  Keys will be saved to:"
    echo "   📂 ./keys/genesis-admin.private"
    echo "   📂 ./keys/genesis-admin.public"
    exit 0
fi

# Main script execution
main() {
    # Check if we're in the correct directory
    if [[ ! -f "pom.xml" ]]; then
        print_error "pom.xml not found. Make sure to run this script from the project root directory."
        exit 1
    fi

    print_header "🔑 Genesis Admin Key Generator"
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

    # Check if keys already exist
    if [[ -f "./keys/genesis-admin.private" ]] || [[ -f "./keys/genesis-admin.public" ]]; then
        print_warning "⚠️  Genesis admin keys already exist!"
        print_info "📂 Existing keys:"
        [[ -f "./keys/genesis-admin.private" ]] && echo "   - ./keys/genesis-admin.private"
        [[ -f "./keys/genesis-admin.public" ]] && echo "   - ./keys/genesis-admin.public"

        echo ""
        print_warning "Do you want to overwrite them? (y/N)"
        read -r response
        if [[ ! "$response" =~ ^[Yy]$ ]]; then
            print_info "Cancelled by user. Existing keys preserved."
            exit 0
        fi
        print_warning "Overwriting existing keys..."
    fi

    # Create keys directory if it doesn't exist
    if [[ ! -d "./keys" ]]; then
        print_info "📁 Creating ./keys/ directory..."
        mkdir -p ./keys
        if [[ $? -ne 0 ]]; then
            print_error "Failed to create ./keys/ directory"
            exit 1
        fi
        print_success "Directory created"
    fi

    # Compile project (quietly)
    print_info "🔨 Compiling project..."
    if ! mvn compile -q > /dev/null 2>&1; then
        print_error "Compilation failed. Please fix compilation errors first."
        exit 1
    fi
    print_success "Compilation successful"

    # Generate keys
    print_separator
    print_info "🔑 Generating genesis admin key pair..."
    echo ""

    if mvn exec:java -Dexec.mainClass="tools.GenerateGenesisAdminKeys" -q 2>&1 | grep -v "WARNING:"; then
        echo ""
        print_separator
        print_success "✅ Genesis admin keys generated successfully!"

        # Verify files exist
        if [[ -f "./keys/genesis-admin.private" ]] && [[ -f "./keys/genesis-admin.public" ]]; then
            print_info "📋 Key files:"
            ls -lh ./keys/genesis-admin.* | while read -r line; do
                echo "   $line"
            done

            echo ""
            print_success "🎉 Keys are ready for use in tests and demos!"
            exit 0
        else
            print_error "❌ Key files were not created. Check for errors above."
            exit 1
        fi
    else
        print_error "❌ Failed to generate keys. Check for errors above."
        exit 1
    fi
}

# Execute main function
main "$@"
