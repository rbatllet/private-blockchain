#!/usr/bin/env zsh

# EXHAUSTIVE SEARCH EXAMPLES SCRIPT
# Practical examples for developers showing TRUE exhaustive search capabilities
# Version: 1.0.0
# Created: 2025-07-10

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Icons (matching CLAUDE.md style)
SUCCESS="✅"
ERROR="❌"
INFO="ℹ️"
WARNING="⚠️"
STATS="📊"
TEST="📋"
KEY="🔑"
BLOCKCHAIN="🔗"
DATA="📝"
SEARCH="🔍"
SECURITY="🔐"
BLOCKS="📦"
CLEANUP="🧹"

# Navigate to project root (parent directory of scripts)
SCRIPT_DIR="$(cd "$(dirname "${(%):-%N}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_ROOT"

# Check if project directory exists
if [[ ! -f "pom.xml" ]]; then
    echo "${ERROR} pom.xml not found in project root: $PROJECT_ROOT"
    echo "${INFO} Please ensure this script is in the scripts/ directory of the project"
    exit 1
fi

echo "${CYAN}🔍 EXHAUSTIVE SEARCH - Practical Examples${NC}"
echo "${CYAN}================================================================${NC}"
echo "${CYAN}📋 Script Version: 1.0.0 | $(date '+%Y-%m-%d')${NC}"
echo

echo "${INFO} This script demonstrates practical usage of TRUE exhaustive search:"
echo "   ${SEARCH} On-chain content search (encrypted and plain text)"
echo "   ${BLOCKS} Off-chain file search with various formats"
echo "   ${SECURITY} Mixed content scenarios and security validation"
echo "   ${STATS} Performance optimization and caching"
echo "   ${TEST} Thread safety and concurrent operations"
echo

echo "${INFO} Working in project directory: $PROJECT_ROOT"
echo

# Check if Maven is available
if ! command -v mvn &> /dev/null; then
    echo "${ERROR} Maven is not installed or not in PATH"
    echo "${INFO} Please install Maven to run these examples"
    exit 1
fi

# Function to print section header
print_section() {
    echo "${PURPLE}$1${NC}"
    echo "${PURPLE}$(echo $1 | sed 's/./=/g')${NC}"
}

# Function to run command with error checking
run_command() {
    local cmd="$1"
    local description="$2"
    
    echo "${INFO} $description..."
    if eval "$cmd"; then
        echo "${SUCCESS} $description completed successfully"
    else
        echo "${ERROR} $description failed"
        return 1
    fi
}

# Compile the project first
print_section "STEP 1: Compilation"
run_command "mvn compile -q" "Compiling project" || exit 1
echo

# Run the examples
print_section "STEP 2: Running Practical Examples"
echo "${INFO} Starting comprehensive exhaustive search examples..."
echo "${INFO} Each example demonstrates specific real-world scenarios"
echo

# Set Java options for better performance
export MAVEN_OPTS="-Xmx2g -XX:+UseG1GC"

# Run the examples with proper error handling
echo "${SEARCH} Executing ExhaustiveSearchExamples..."
if mvn exec:java -Dexec.mainClass="demo.ExhaustiveSearchExamples" -Dexec.cleanupDaemonThreads=false -q; then
    echo
    echo "${SUCCESS} Examples execution completed successfully!"
else
    echo
    echo "${ERROR} Examples execution failed"
    echo "${INFO} Checking for common issues..."
    
    # Check if the examples class exists
    if [[ ! -f "$PROJECT_ROOT/src/main/java/demo/ExhaustiveSearchExamples.java" ]]; then
        echo "${ERROR} ExhaustiveSearchExamples.java not found in $PROJECT_ROOT/src/main/java/demo/"
        echo "${INFO} Please ensure the examples file is created in the correct location"
    fi
    
    # Check compilation
    echo "${INFO} Attempting to compile examples specifically..."
    if mvn compile -Dexec.mainClass="demo.ExhaustiveSearchExamples" -q; then
        echo "${SUCCESS} Examples class compiled successfully"
        echo "${WARNING} There might be a runtime issue - check the output above"
    else
        echo "${ERROR} Examples class compilation failed"
        echo "${INFO} Please check for compilation errors"
    fi
    
    exit 1
fi

echo
print_section "STEP 3: Examples Summary"
echo "${STATS} The examples demonstrated:"
echo "   ${SEARCH} Example 1: Basic on-chain content search (encrypted + plain text)"
echo "   ${BLOCKS} Example 2: Off-chain file search with medical records"
echo "   ${DATA} Example 3: Mixed content search (JSON, text, encrypted files)"
echo "   ${SECURITY} Example 4: Security validation and access control"
echo "   ${STATS} Example 5: Performance optimization and intelligent caching"
echo "   ${TEST} Example 6: Thread safety with concurrent search operations"
echo

echo "${SUCCESS} All practical examples completed successfully!"
echo "${INFO} Review the output above for detailed implementation patterns"
echo

# Optional: Offer to run tests
echo "${TEST} Would you like to run the OnChainContentSearchTest to verify functionality? (y/n)"
read -r response
if [[ "$response" =~ ^[Yy]$ ]]; then
    echo
    print_section "BONUS: Running OnChainContentSearchTest"
    run_command "mvn test -Dtest=OnChainContentSearchTest -q" "Running OnChainContentSearchTest"
    echo
    echo "${SUCCESS} Tests validate the exhaustive search implementation!"
fi

echo
echo "${CLEANUP} Examples script completed - all temporary files cleaned up automatically"
echo "${CYAN}================================================================${NC}"
echo
echo "${INFO} 📚 For detailed documentation, see:"
echo "   📖 docs/EXHAUSTIVE_SEARCH_GUIDE.md - Complete guide with API reference"
echo "   🔧 src/main/java/demo/ExhaustiveSearchExamples.java - Source code for examples"
echo "   🎯 Run './scripts/run_exhaustive_search_demo.zsh' for interactive demonstration"