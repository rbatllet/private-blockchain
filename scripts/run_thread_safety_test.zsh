#!/usr/bin/env zsh
# Compatible with both Bash and ZSH

# Thread-Safety Test Script for Private Blockchain
# This script compiles and runs the thread-safety test
# Version: 1.0.0


# Set script directory and navigate to project root
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR/.."

# Load common functions library
source "${SCRIPT_DIR}/lib/common_functions.zsh"

echo "🧪 Private Blockchain - Thread-Safety Test"
echo "==========================================="

# Check if we're in the right directory
if [[ ! -f "pom.xml" ]]; then
    echo "❌ Error: pom.xml not found. Please run this script from the project root directory."
    exit 1
fi

# Clean previous test database
echo "🧹 Cleaning previous test database..."
rm -f blockchain.db blockchain.db-shm blockchain.db-wal

# Compile the project and tests
echo "🔨 Compiling project and tests..."
mvn clean test-compile -q

if [[ $? -ne 0 ]]; then
    echo "❌ Compilation failed!"
    exit 1
fi

echo "✅ Compilation successful!"

# Run the thread-safety test as main class
echo ""
echo "🚀 Running Thread-Safety Test..."
echo "================================"

mvn exec:java -Dexec.mainClass="com.rbatllet.blockchain.advanced.ThreadSafetyTest" -Dexec.classpathScope="test" -q

TEST_RESULT=$?

echo ""
echo "================================"
if [[ $TEST_RESULT -eq 0 ]]; then
    echo "🎉 Thread-Safety Test completed!"
    echo "✅ Check the output above for detailed results."
else
    echo "💥 Thread-Safety Test failed!"
    echo "❌ Exit code: $TEST_RESULT"
fi

echo ""
echo "📊 Database files after test:"
ls -la *.db* 2>/dev/null || echo "No database files found"

echo ""
echo "💡 If the test passed, your blockchain is now thread-safe!"
echo "💡 If the test failed, check the output for specific error details."