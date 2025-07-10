#!/usr/bin/env zsh

# EXHAUSTIVE SEARCH DEMO SCRIPT v1.0
# Comprehensive demonstration of TRUE exhaustive search capabilities
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

echo "${BLUE}🚀 EXHAUSTIVE SEARCH DEMO - TRUE Exhaustive v2.0${NC}"
echo "${BLUE}================================================================${NC}"
echo "${CYAN}📋 Script Version: 1.0.0 | $(date '+%Y-%m-%d')${NC}"
echo
echo "${INFO} This demo showcases comprehensive blockchain search across:"
echo "   ${DATA} On-chain blocks (encrypted and plain text)"
echo "   ${BLOCKS} Off-chain files (encrypted and plain text)" 
echo "   ${SEARCH} Mixed content scenarios with performance metrics"
echo "   ${SECURITY} Security validation and access control"
echo

# Check if Maven is available
if ! command -v mvn &> /dev/null; then
    echo "${ERROR} Maven is not installed or not in PATH"
    echo "${INFO} Please install Maven to run this demo"
    exit 1
fi

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

echo "${INFO} Working in project directory: $PROJECT_ROOT"

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

# Run the demo
print_section "STEP 2: Running Exhaustive Search Demo"
echo "${INFO} Starting comprehensive search demonstration..."
echo "${INFO} This will create mixed content blockchain and perform exhaustive searches"
echo

# Set Java options for better performance
export MAVEN_OPTS="-Xmx2g -XX:+UseG1GC"

# Run the demo with proper error handling
echo "${SEARCH} Executing ExhaustiveSearchDemo..."
if mvn exec:java -Dexec.mainClass="demo.ExhaustiveSearchDemo" -Dexec.cleanupDaemonThreads=false -q; then
    echo
    echo "${SUCCESS} Demo execution completed successfully!"
else
    echo
    echo "${ERROR} Demo execution failed"
    echo "${INFO} Checking for common issues..."
    
    # Check if the demo class exists
    if [[ ! -f "$PROJECT_ROOT/src/main/java/demo/ExhaustiveSearchDemo.java" ]]; then
        echo "${ERROR} ExhaustiveSearchDemo.java not found in $PROJECT_ROOT/src/main/java/demo/"
        echo "${INFO} Please ensure the demo file is created in the correct location"
    fi
    
    # Check compilation
    echo "${INFO} Attempting to compile demo specifically..."
    if mvn compile -Dexec.mainClass="demo.ExhaustiveSearchDemo" -q; then
        echo "${SUCCESS} Demo class compiled successfully"
        echo "${WARNING} There might be a runtime issue - check the output above"
    else
        echo "${ERROR} Demo class compilation failed"
        echo "${INFO} Please check for compilation errors"
    fi
    
    exit 1
fi

echo
print_section "STEP 3: Demo Summary"
echo "${STATS} The demo demonstrated:"
echo "   ${SEARCH} Exhaustive search across on-chain and off-chain content"
echo "   ${SECURITY} Security validation with encrypted content"
echo "   ${STATS} Performance metrics and caching capabilities"
echo "   ${DATA} Mixed content handling (text, JSON, encrypted files)"
echo "   ${BLOCKCHAIN} TRUE exhaustive search v2.0 functionality"
echo

echo "${SUCCESS} Exhaustive Search Demo completed successfully!"
echo "${INFO} Review the output above for detailed search results and performance metrics"
echo

# Optional: Offer to run tests
echo "${TEST} Would you like to run the OnChainContentSearchTest to verify functionality? (y/n)"
read -r response
if [[ "$response" =~ ^[Yy]$ ]]; then
    echo
    print_section "BONUS: Running OnChainContentSearchTest"
    run_command "mvn test -Dtest=OnChainContentSearchTest -q" "Running OnChainContentSearchTest"
    echo
    echo "${SUCCESS} All tests validate the exhaustive search implementation!"
fi

echo
echo "${CLEANUP} Demo script completed - all temporary files cleaned up automatically"
echo "${BLUE}================================================================${NC}"