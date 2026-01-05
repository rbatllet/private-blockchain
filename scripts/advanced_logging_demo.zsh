#!/usr/bin/env zsh

# Advanced Logging System Demo Script
# Demonstrates comprehensive logging capabilities with detailed operation tracking


set -e

# Script configuration
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

# Load common functions library
source "${SCRIPT_DIR}/lib/common_functions.zsh"

# Change to project root directory
cd "$PROJECT_DIR"

DEMO_CLASS="demo.AdvancedLoggingDemo"

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
WHITE='\033[1;37m'
NC='\033[0m' # No Color

# Function to print colored output
print_colored() {
    local color=$1
    shift
    echo -e "${color}$@${NC}"
}

# Function to print section header
print_section() {
    echo ""
    print_colored $BLUE "================================"
    print_colored $BLUE "$1"
    print_colored $BLUE "================================"
}

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Function to check prerequisites
check_prerequisites() {
    print_section "📋 Checking Prerequisites"

    if ! command_exists java; then
        print_colored $RED "❌ Java is not installed"
        exit 1
    fi

    if ! command_exists mvn; then
        print_colored $RED "❌ Maven is not installed"
        exit 1
    fi

    print_colored $GREEN "✅ Java: $(java -version 2>&1 | head -n1)"
    print_colored $GREEN "✅ Maven: $(mvn -version 2>&1 | head -n1)"
}

# Function to build project
build_project() {
    print_section "🏗️  Building Project"
    
    cd "$PROJECT_DIR"
    
    if ! mvn clean compile -q; then
        print_colored $RED "❌ Build failed"
        exit 1
    fi
    
    print_colored $GREEN "✅ Project built successfully"
}

# Function to run demo
run_demo() {
    print_section "🔍 Running Advanced Logging Demo"

    cd "$PROJECT_DIR"

    # Clean database before running demo (demo requires fresh state)
    print_colored $CYAN "🧹 Cleaning database for fresh demo execution..."
    cleanup_database

    # Set JVM options for better logging
    export JAVA_OPTS="-Xmx1024m -Dlog4j2.configurationFile=file:src/main/resources/log4j2.xml"

    print_colored $CYAN "🚀 Starting Advanced Logging System Demo..."
    print_colored $YELLOW "ℹ️  This demo will show comprehensive logging capabilities"
    print_colored $YELLOW "ℹ️  Watch the console for detailed operation tracking"

    echo ""

    # Run the demo
    if ! java -cp "target/classes:$(mvn -q dependency:build-classpath -Dmdep.outputFile=/dev/stdout)" \
        "$DEMO_CLASS"; then
        print_colored $RED "❌ Demo execution failed"
        exit 1
    fi

    print_colored $GREEN "✅ Demo completed successfully"
}

# Function to show demo features
show_features() {
    print_section "🎯 Advanced Logging Features Demonstrated"
    
    print_colored $WHITE "📦 Blockchain Operation Logging:"
    print_colored $CYAN "   • Block creation with performance tracking"
    print_colored $CYAN "   • Blockchain validation with timing"
    print_colored $CYAN "   • Operation correlation with unique IDs"
    
    print_colored $WHITE "🔍 Search Operation Logging:"
    print_colored $CYAN "   • Keyword search with result metrics"
    print_colored $CYAN "   • Semantic search performance tracking"
    print_colored $CYAN "   • Advanced search with cache monitoring"
    
    print_colored $WHITE "🔐 Crypto Operation Logging:"
    print_colored $CYAN "   • Encryption/decryption with algorithm tracking"
    print_colored $CYAN "   • Key generation and management logging"
    print_colored $CYAN "   • Performance metrics for crypto operations"
    
    print_colored $WHITE "🛡️  Security Event Logging:"
    print_colored $CYAN "   • Authentication events with severity levels"
    print_colored $CYAN "   • Unauthorized access detection"
    print_colored $CYAN "   • Key rotation and security operations"
    
    print_colored $WHITE "📊 Performance Monitoring:"
    print_colored $CYAN "   • Memory usage tracking and alerts"
    print_colored $CYAN "   • Database operation performance"
    print_colored $CYAN "   • System health monitoring"
    
    print_colored $WHITE "❌ Failure Handling:"
    print_colored $CYAN "   • Comprehensive error tracking"
    print_colored $CYAN "   • Failed operation correlation"
    print_colored $CYAN "   • Recovery and retry logging"
    
    print_colored $WHITE "📋 Comprehensive Reporting:"
    print_colored $CYAN "   • Operation metrics and statistics"
    print_colored $CYAN "   • Performance trends and analysis"
    print_colored $CYAN "   • System health and resource usage"
}

# Function to show usage
show_usage() {
    print_colored $WHITE "Usage: $0 [OPTIONS]"
    print_colored $WHITE ""
    print_colored $WHITE "Options:"
    print_colored $CYAN "  -h, --help     Show this help message"
    print_colored $CYAN "  -f, --features Show advanced logging features"
    print_colored $CYAN "  -b, --build    Build project only"
    print_colored $CYAN "  -r, --run      Run demo only (requires built project)"
    print_colored $WHITE ""
    print_colored $WHITE "Examples:"
    print_colored $CYAN "  $0                    # Full demo (build + run)"
    print_colored $CYAN "  $0 --features         # Show features overview"
    print_colored $CYAN "  $0 --build            # Build project only"
    print_colored $CYAN "  $0 --run              # Run demo only"
}

# Function to show header
show_header() {
    print_colored $PURPLE "🔍 ADVANCED LOGGING SYSTEM DEMO"
    print_colored $WHITE "Comprehensive operation tracking and performance monitoring"
    print_colored $WHITE "Project: Private Blockchain Advanced Logging"
    print_colored $WHITE "Date: $(date '+%Y-%m-%d %H:%M:%S')"
}

# Main function
main() {
    show_header
    
    case "${1:-}" in
        -h|--help)
            show_usage
            exit 0
            ;;
        -f|--features)
            show_features
            exit 0
            ;;
        -b|--build)
            check_prerequisites
            ensure_genesis_keys
            build_project
            print_colored $GREEN "✅ Build completed. Run with --run to execute demo."
            exit 0
            ;;
        -r|--run)
            ensure_genesis_keys
            run_demo
            exit 0
            ;;
        "")
            # Full demo
            check_prerequisites
            ensure_genesis_keys
            build_project
            show_features
            run_demo
            ;;
        *)
            print_colored $RED "❌ Unknown option: $1"
            show_usage
            exit 1
            ;;
    esac
    
    print_section "🎉 Advanced Logging Demo Complete"
    print_colored $GREEN "✅ All logging capabilities demonstrated successfully"
    print_colored $YELLOW "ℹ️  Check the console output above for detailed operation tracking"
    print_colored $YELLOW "ℹ️  Log files are available in the logs directory"
}

# Trap to handle interruption
trap 'print_colored $RED "\n❌ Demo interrupted by user"; exit 1' INT

# Run main function
main "$@"
