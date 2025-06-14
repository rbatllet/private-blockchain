#!/bin/bash

# Simple script to run the improved rollback strategy test
# Executes the ImprovedRollbackStrategyTest JUnit5 test

echo "🧪 IMPROVED ROLLBACK STRATEGY TEST"
echo "=================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}Running ImprovedRollbackStrategyTest...${NC}"
echo ""

# Check if we're in the correct directory
if [ ! -f "pom.xml" ]; then
    echo -e "${RED}❌ Error: pom.xml not found. Please run this script from the project root directory.${NC}"
    exit 1
fi

# Run the specific test
mvn test -Dtest=ImprovedRollbackStrategyTest

TEST_EXIT_CODE=$?

echo ""
if [ $TEST_EXIT_CODE -eq 0 ]; then
    echo -e "${GREEN}✅ Improved Rollback Strategy Test: PASSED${NC}"
    echo ""
    echo -e "${BLUE}🎯 Test verified:${NC}"
    echo "   • Intelligent rollback analysis"
    echo "   • Security-first approach with data preservation"
    echo "   • Hash chain integrity verification"
    echo "   • Multiple safety checks and fallbacks"
else
    echo -e "${RED}❌ Improved Rollback Strategy Test: FAILED${NC}"
fi

exit $TEST_EXIT_CODE
