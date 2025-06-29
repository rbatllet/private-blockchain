#!/bin/zsh

# Enhanced Search Demo Runner for Private Blockchain
# This script compiles and runs the SearchDemo class

echo "🔍 Enhanced Search Functionality Demo"
echo "====================================="
echo

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if Maven is available
if ! command -v mvn &> /dev/null; then
    echo "${RED}❌ Maven not found. Please install Maven first.${NC}"
    exit 1
fi

echo "${YELLOW}📦 Compiling project with Maven...${NC}"
if mvn compile -q; then
    echo "${GREEN}✅ Compilation successful${NC}"
    echo
else
    echo "${RED}❌ Compilation failed${NC}"
    exit 1
fi

echo "${YELLOW}🚀 Running Search Demo...${NC}"
echo

# Run the SearchDemo class
mvn exec:java -Dexec.mainClass="demo.SearchDemo" -Dexec.args="" -q

echo
echo "${GREEN}🎯 Demo completed!${NC}"