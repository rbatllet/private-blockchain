#!/usr/bin/env zsh

echo "=== 🔄 DATA CONSISTENCY VALIDATION TEST ==="
echo

# Clean up any existing database and files
echo "🧹 Cleaning up previous test files..."
rm -f blockchain.db blockchain.db-shm blockchain.db-wal
rm -f export-test.json
rm -rf off-chain-data off-chain-backup
rm -f TestDataConsistency.class TestExportImport.class
# Clean up any log files (if they exist)
find . -maxdepth 1 -name "*.log" -delete 2>/dev/null || true

echo "1. ✅ Starting with clean state"
echo

echo "2. 📝 Running data consistency test using demo class..."
echo

# Run the test using Maven
echo "3. 🚀 Running data consistency validation..."
echo
mvn -q exec:java -Dexec.mainClass="demo.TestDataConsistency"

echo
echo "=== 📊 FINAL VALIDATION ==="

# Check for any remaining files
if [[ -d "off-chain-data" ]]; then
    local file_count=$(find off-chain-data -type f | wc -l)
    echo "Off-chain directory files remaining: $file_count"
    if [[ $file_count -eq 0 ]]; then
        echo "✅ No orphaned files remaining"
    else
        echo "❌ Some files remain in off-chain directory"
        ls -la off-chain-data/
    fi
else
    echo "✅ Off-chain directory properly cleaned up"
fi

echo
echo "🎯 DATA CONSISTENCY VALIDATION COMPLETE"

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