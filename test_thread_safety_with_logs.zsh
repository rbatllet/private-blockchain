#!/usr/bin/env zsh

echo "=== 🔒 THREAD SAFETY TEST WITH LOGGING ==="
echo

# Clean up any existing files
echo "🧹 Cleaning up previous test files..."
rm -f blockchain.db blockchain.db-shm blockchain.db-wal
rm -f export_test_*.json
rm -rf off-chain-data off-chain-backup
rm -f *.class
# Clean up any log files (if they exist)
find . -maxdepth 1 -name "*.log" -delete 2>/dev/null || true

echo "1. ✅ Starting with clean state"
echo

echo "2. 📝 Running thread safety tests with detailed logging..."
echo "   This will show both the test execution and the internal blockchain logs"
echo "   to help trace what's happening during concurrent operations."
echo

# Create logs directory if it doesn't exist
mkdir -p logs

# Set logging level to DEBUG for more detailed output during tests
export MAVEN_OPTS="-Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=100 -Dlog4j2.configurationFile=src/test/resources/log4j2-test.xml"

echo "3. 🚀 Executing thread safety tests with comprehensive logging..."
echo "   Check logs/test-app.log for persistent logs"
echo

# Run the simplified test first (cleaner output)
echo "--- 📊 SIMPLIFIED THREAD SAFETY TEST ---"
mvn -q exec:java -Dexec.mainClass="demo.SimpleThreadSafetyTest"

echo
echo "=== 📊 THREAD SAFETY ANALYSIS ==="

# Check for any remaining inconsistencies
if [[ -d "off-chain-data" ]]; then
    local file_count=$(find off-chain-data -type f 2>/dev/null | wc -l)
    echo "📁 Off-chain files remaining: $file_count"
    if [[ $file_count -gt 0 ]]; then
        echo "   Files found:"
        ls -la off-chain-data/ 2>/dev/null || echo "   (directory accessible but empty)"
    fi
else
    echo "📁 No off-chain directory found"
fi

# Check for any export files left behind
local export_files=$(find . -maxdepth 1 -name "export_test_*.json" 2>/dev/null | wc -l)
if [[ $export_files -gt 0 ]]; then
    echo "📄 Export files remaining: $export_files"
    ls -la export_test_*.json 2>/dev/null
else
    echo "📄 No export files remaining"
fi

# Check database files
if [[ -f "blockchain.db" ]]; then
    local db_size=$(du -h blockchain.db 2>/dev/null | cut -f1)
    echo "💾 Database size: $db_size"
else
    echo "💾 No database file found"
fi

# Check log files
if [[ -f "logs/test-app.log" ]]; then
    local log_lines=$(wc -l < "logs/test-app.log" 2>/dev/null)
    local log_size=$(du -h "logs/test-app.log" 2>/dev/null | cut -f1)
    echo "📋 Test log file: $log_lines lines, $log_size"
    
    echo
    echo "📋 Recent log entries (last 20 lines):"
    echo "----------------------------------------"
    tail -20 logs/test-app.log 2>/dev/null || echo "Could not read log file"
    echo "----------------------------------------"
else
    echo "📋 No test log file found"
fi

echo
echo "🎯 THREAD SAFETY TEST WITH LOGGING COMPLETE"

# Optional: Clean up test files after completion
if [[ "${KEEP_TEST_FILES:-false}" != "true" ]]; then
    echo
    echo "🧹 Cleaning up test files..."
    rm -f blockchain.db blockchain.db-shm blockchain.db-wal
    rm -f export_test_*.json
    rm -rf off-chain-data off-chain-backup
    echo "✅ Test environment cleaned"
    
    if [[ "${KEEP_LOGS:-false}" != "true" ]]; then
        echo "🗑️ Cleaning up log files..."
        rm -f logs/test-app.log
        echo "✅ Log files cleaned"
    else
        echo "📋 Log files preserved (KEEP_LOGS=true)"
    fi
else
    echo "💾 Test files preserved (KEEP_TEST_FILES=true)"
fi

echo
echo "📋 Logging Summary:"
echo "   - All blockchain operations are logged with timestamps and thread IDs"
echo "   - Test execution details are logged at INFO level"
echo "   - Thread synchronization details at DEBUG level"
echo "   - Off-chain operations are traced in detail"
echo "   - Any errors or warnings are highlighted"
echo
echo "💡 To see more detailed logs, set DEBUG logging:"
echo "   MAVEN_OPTS=\"-Dlog4j2.configurationFile=src/test/resources/log4j2-test.xml\" mvn exec:java ..."
echo
echo "💡 To preserve logs for analysis:"
echo "   KEEP_LOGS=true ./test_thread_safety_with_logs.zsh"