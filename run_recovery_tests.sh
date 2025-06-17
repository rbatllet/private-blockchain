#!/usr/bin/env zsh

# Recovery-focused test runner
# Runs all recovery-related tests including the improved rollback strategy
# Version: 1.0.1

echo "🔄 BLOCKCHAIN RECOVERY TESTS"
echo "============================"
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Check if we're in the correct directory
if [ ! -f "pom.xml" ]; then
    echo -e "${RED}❌ Error: pom.xml not found. Please run this script from the project root directory.${NC}"
    exit 1
fi

TOTAL_TESTS=0
PASSED_TESTS=0

echo -e "${BLUE}🧪 Running Chain Recovery Manager Tests...${NC}"
mvn test -Dtest=ChainRecoveryManagerTest -q
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Chain Recovery Manager Tests: PASSED${NC}"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    echo -e "${RED}❌ Chain Recovery Manager Tests: FAILED${NC}"
fi
TOTAL_TESTS=$((TOTAL_TESTS + 1))

echo ""
echo -e "${BLUE}🧪 Running Recovery Configuration Tests...${NC}"
mvn test -Dtest=RecoveryConfigTest -q
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Recovery Configuration Tests: PASSED${NC}"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    echo -e "${RED}❌ Recovery Configuration Tests: FAILED${NC}"
fi
TOTAL_TESTS=$((TOTAL_TESTS + 1))

echo ""
echo -e "${BLUE}🧠 Running Improved Rollback Strategy Tests...${NC}"
mvn test -Dtest=ImprovedRollbackStrategyTest -q
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Improved Rollback Strategy Tests: PASSED${NC}"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    echo -e "${RED}❌ Improved Rollback Strategy Tests: FAILED${NC}"
fi
TOTAL_TESTS=$((TOTAL_TESTS + 1))

echo ""
echo "=========================="
echo -e "${BLUE}📊 RECOVERY TESTS SUMMARY${NC}"
echo "=========================="
echo "Total recovery test suites: $TOTAL_TESTS"
echo "Passed: $PASSED_TESTS"
echo "Failed: $((TOTAL_TESTS - PASSED_TESTS))"

if [ $PASSED_TESTS -eq $TOTAL_TESTS ]; then
    echo -e "${GREEN}🎉 ALL RECOVERY TESTS PASSED!${NC}"
    echo ""
    echo -e "${BLUE}✅ Recovery features validated:${NC}"
    echo "   • Chain recovery after key deletion"
    echo "   • Intelligent rollback with data preservation"
    echo "   • Security-first recovery strategies"
    echo "   • Configuration and edge case handling"
    exit 0
else
    echo -e "${RED}❌ Some recovery tests failed.${NC}"
    exit 1
fi
