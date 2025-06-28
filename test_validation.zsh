#!/usr/bin/env zsh

echo "=== 🔍 OFF-CHAIN VALIDATION COMPREHENSIVE TEST ==="
echo

# Clean up any existing files
echo "🧹 Cleaning up previous test files..."
rm -f blockchain.db blockchain.db-shm blockchain.db-wal
rm -f export-test.json import-test.json
rm -rf off-chain-data off-chain-backup
rm -f TestOffChainValidation.class
# Clean up any log files (if they exist)
find . -maxdepth 1 -name "*.log" -delete 2>/dev/null || true

echo "1. ✅ Starting with clean state"
echo

echo "2. 📝 Running comprehensive off-chain validation test..."
echo

# Run the test using Maven
echo "3. 🚀 Running validation tests..."
echo
mvn -q exec:java -Dexec.mainClass="demo.TestOffChainValidation"

echo
echo "=== 📊 FINAL VALIDATION ==="

# Check if any test artifacts remain
if [[ -d "off-chain-data" ]]; then
    local file_count=$(find off-chain-data -type f | wc -l)
    echo "📁 Off-chain files remaining: $file_count"
    if [[ $file_count -eq 0 ]]; then
        echo "✅ Off-chain directory properly cleaned"
    else
        echo "❌ Some test files remain"
        ls -la off-chain-data/
    fi
else
    echo "✅ Off-chain directory properly cleaned up"
fi

echo
echo "🎯 OFF-CHAIN VALIDATION TEST COMPLETE"

# Optional: Clean up test files after completion
if [[ "${KEEP_TEST_FILES:-false}" != "true" ]]; then
    echo
    echo "🧹 Cleaning up test files..."
    rm -f blockchain.db blockchain.db-shm blockchain.db-wal
    rm -rf off-chain-data off-chain-backup
    echo "✅ Test environment cleaned"
else
    echo "💾 Test files preserved (KEEP_TEST_FILES=true)"
fi