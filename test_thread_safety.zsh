#!/usr/bin/env zsh

echo "=== 🔒 COMPREHENSIVE THREAD SAFETY TEST ==="
echo

# Clean up any existing files
echo "🧹 Cleaning up previous test files..."
rm -f blockchain.db blockchain.db-shm blockchain.db-wal
rm -f export_test_*.json
rm -rf off-chain-data off-chain-backup
rm -f ComprehensiveThreadSafetyTest.class
# Clean up any log files (if they exist)
find . -maxdepth 1 -name "*.log" -delete 2>/dev/null || true

echo "1. ✅ Starting with clean state"
echo

echo "2. 📝 Running comprehensive thread safety tests..."
echo "   This test will run multiple concurrent operations to detect race conditions"
echo "   and verify thread safety of blockchain operations including off-chain storage."
echo

# Set JVM options for better concurrency testing
export MAVEN_OPTS="-Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=100"

# Run the test using Maven with increased memory and optimized GC
echo "3. 🚀 Executing thread safety tests..."
echo
mvn -q exec:java -Dexec.mainClass="demo.ComprehensiveThreadSafetyTest" -Dexec.cleanupDaemonThreads=false

echo
echo "=== 📊 THREAD SAFETY ANALYSIS ==="

# Check for any remaining inconsistencies
if [[ -d "off-chain-data" ]]; then
    local file_count=$(find off-chain-data -type f | wc -l)
    echo "📁 Off-chain files remaining: $file_count"
    if [[ $file_count -gt 0 ]]; then
        echo "   Files found:"
        ls -la off-chain-data/
    fi
else
    echo "📁 No off-chain directory found"
fi

# Check for any export files left behind
local export_files=$(find . -maxdepth 1 -name "export_test_*.json" | wc -l)
if [[ $export_files -gt 0 ]]; then
    echo "📄 Export files remaining: $export_files"
    ls -la export_test_*.json
else
    echo "📄 No export files remaining"
fi

# Check database files
if [[ -f "blockchain.db" ]]; then
    local db_size=$(du -h blockchain.db | cut -f1)
    echo "💾 Database size: $db_size"
else
    echo "💾 No database file found"
fi

echo
echo "🎯 THREAD SAFETY TEST COMPLETE"

# Optional: Clean up test files after completion
if [[ "${KEEP_TEST_FILES:-false}" != "true" ]]; then
    echo
    echo "🧹 Cleaning up test files..."
    rm -f blockchain.db blockchain.db-shm blockchain.db-wal
    rm -f export_test_*.json
    rm -rf off-chain-data off-chain-backup
    echo "✅ Test environment cleaned"
else
    echo "💾 Test files preserved (KEEP_TEST_FILES=true)"
fi

echo
echo "📋 Test Summary:"
echo "   - Tested concurrent block addition with off-chain storage"
echo "   - Verified mixed operations under load"
echo "   - Stress tested chain validation"
echo "   - Validated off-chain file operations thread safety"
echo "   - Tested rollback operation safety"
echo "   - Verified export/import operation safety"
echo "   - Tested edge cases with rapid key operations"