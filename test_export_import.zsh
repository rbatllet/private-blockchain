#!/usr/bin/env zsh

echo "=== 📦 EXPORT/IMPORT DATA CONSISTENCY TEST ==="
echo

# Clean up any existing files
echo "🧹 Cleaning up previous test files..."
rm -f blockchain.db blockchain.db-shm blockchain.db-wal
rm -f export-test.json import-test.json
rm -rf off-chain-data off-chain-backup
rm -f TestDataConsistency.class TestExportImport.class
# Clean up any log files (if they exist)
find . -maxdepth 1 -name "*.log" -delete 2>/dev/null || true

echo "1. ✅ Starting with clean state"
echo

echo "2. 📝 Running export/import test using demo class..."
echo

# Run the test using Maven
echo "3. 🚀 Running export/import test..."
echo
mvn -q exec:java -Dexec.mainClass="demo.TestExportImport"

echo
echo "=== 📊 FINAL VALIDATION ==="

# Check exported files
if [[ -f "export-test.json" ]]; then
    echo "✅ Export file created: export-test.json"
    echo "   📏 Export file size: $(du -h export-test.json | cut -f1)"
else
    echo "❌ Export file missing"
fi

# Check off-chain files
if [[ -d "off-chain-data" ]]; then
    local file_count=$(find off-chain-data -type f | wc -l)
    echo "📁 Off-chain files after import: $file_count"
    if [[ $file_count -gt 0 ]]; then
        echo "✅ Off-chain files properly restored"
    fi
else
    echo "❌ Off-chain directory not found"
fi

echo
echo "🎯 EXPORT/IMPORT VALIDATION COMPLETE"

# Optional: Clean up test files after completion
if [[ "${KEEP_TEST_FILES:-false}" != "true" ]]; then
    echo
    echo "🧹 Cleaning up test files..."
    rm -f blockchain.db blockchain.db-shm blockchain.db-wal
    rm -f export-test.json import-test.json
    rm -rf off-chain-data off-chain-backup
    echo "✅ Test environment cleaned"
else
    echo "💾 Test files preserved (KEEP_TEST_FILES=true)"
fi