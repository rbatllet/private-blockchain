#!/usr/bin/env zsh

# Structured Alerts System Demo Script
# Demonstrates JSON-structured alerts and monitoring capabilities

echo "🚨 Structured Alerts System Demo"
echo "================================="

# Change to project root directory
cd "$(dirname "$0")/.."

# Check if Maven is available
if ! command -v mvn &> /dev/null; then
    echo "❌ Error: Maven is not installed or not in PATH"
    exit 1
fi

# Check if we're in the correct directory
if [[ ! -f "pom.xml" ]]; then
    echo "❌ Error: pom.xml not found. Please run this script from the project root directory."
    exit 1
fi

# Set environment variable for development configuration
export ENV=development

echo "📋 Compiling project..."
mvn compile -q

if [[ $? -ne 0 ]]; then
    echo "❌ Error: Compilation failed"
    exit 1
fi

echo "✅ Compilation successful"
echo ""

# Create logs directory if it doesn't exist
mkdir -p logs

echo "🚀 Starting Structured Alerts Demo..."
echo ""
echo "📊 This demo will generate various types of alerts:"
echo "   • Performance alerts (slow operations, high memory)"
echo "   • System health alerts (health score, error rates)"
echo "   • Security alerts (unauthorized access, data tampering)"
echo "   • Blockchain integrity alerts (hash mismatches, corruption)"
echo "   • Custom business logic alerts"
echo ""
echo "📁 Alert logs will be written to:"
echo "   • logs/structured-alerts.log (JSON format)"
echo "   • Console output (formatted display)"
echo ""

# Auto-continue in non-interactive mode
if [[ -t 0 ]]; then
    read -q "REPLY?Press 'y' to continue or any other key to abort: "
    echo ""
    if [[ $REPLY != "y" ]]; then
        echo "❌ Demo aborted by user"
        exit 0
    fi
else
    echo "🚀 Running in non-interactive mode, continuing automatically..."
fi

echo ""
echo "🎬 Running demo..."

# Run the structured alerts demo - use java directly to avoid Maven exec issues
# Add system property to skip sleep delays in demo
java -cp "target/classes:$(mvn -q dependency:build-classpath -Dmdep.outputFile=/dev/stdout)" \
    -Ddemo.skip.sleep=true \
    demo.StructuredAlertsDemo

# Demo completed, show summary
echo "✅ Structured Alerts Demo execution completed"

if [[ $? -eq 0 ]]; then
    echo ""
    echo "✅ Demo completed successfully!"
    echo ""
    
    # Show alert log information
    if [[ -f "logs/structured-alerts.log" ]]; then
        echo "📊 Alert Statistics:"
        echo "==================="
        
        # Count total alerts
        TOTAL_ALERTS=$(grep -c '"alert_type"' logs/structured-alerts.log 2>/dev/null || echo "0")
        echo "   Total Alerts Generated: $TOTAL_ALERTS"
        
        # Count by severity
        CRITICAL_ALERTS=$(grep -c '"severity":"CRITICAL"' logs/structured-alerts.log 2>/dev/null || echo "0")
        WARNING_ALERTS=$(grep -c '"severity":"WARNING"' logs/structured-alerts.log 2>/dev/null || echo "0")
        INFO_ALERTS=$(grep -c '"severity":"INFO"' logs/structured-alerts.log 2>/dev/null || echo "0")
        
        echo "   Critical Alerts: $CRITICAL_ALERTS"
        echo "   Warning Alerts: $WARNING_ALERTS"
        echo "   Info Alerts: $INFO_ALERTS"
        echo ""
        
        # Show log file size
        LOG_SIZE=$(du -h logs/structured-alerts.log 2>/dev/null | cut -f1 || echo "0B")
        echo "   Alert Log Size: $LOG_SIZE"
        echo ""
        
        echo "📁 View detailed alerts:"
        echo "   cat logs/structured-alerts.log | jq '.' | head -20"
        echo ""
        echo "🔍 Search specific alert types:"
        echo "   grep '\"alert_type\":\"SLOW_OPERATION\"' logs/structured-alerts.log | jq '.'"
        echo "   grep '\"severity\":\"CRITICAL\"' logs/structured-alerts.log | jq '.'"
        echo ""
        
        # Show sample alerts if jq is available
        if command -v jq &> /dev/null; then
            echo "📋 Sample Alert (most recent):"
            echo "=============================="
            tail -1 logs/structured-alerts.log | jq '.' 2>/dev/null || echo "   (Unable to parse JSON)"
            echo ""
        fi
        
        echo "💡 Integration Examples:"
        echo "======================="
        echo "   # Monitor alerts in real-time:"
        echo "   tail -f logs/structured-alerts.log | jq '.'"
        echo ""
        echo "   # Extract critical alerts:"
        echo "   grep '\"severity\":\"CRITICAL\"' logs/structured-alerts.log | jq -r '.timestamp + \" - \" + .alert_type + \": \" + .message'"
        echo ""
        echo "   # Count alerts by type:"
        echo "   grep -o '\"alert_type\":\"[^\"]*\"' logs/structured-alerts.log | sort | uniq -c"
        echo ""
        
    else
        echo "⚠️  Alert log file not found. Alerts may have been logged to console only."
    fi
    
    echo "🎯 Next Steps:"
    echo "============="
    echo "   1. Review alert log format and structure"
    echo "   2. Integrate with external monitoring tools (ELK, Splunk, Grafana)"
    echo "   3. Set up automated alert processing scripts"
    echo "   4. Configure alert thresholds for your environment"
    echo "   5. Implement custom alert types for business logic"
    echo ""
    
else
    echo ""
    echo "❌ Demo failed with exit code $?"
    echo ""
    echo "🔍 Troubleshooting:"
    echo "   • Check if all dependencies are properly installed"
    echo "   • Verify that logs directory is writable"
    echo "   • Review console output for specific error messages"
    echo "   • Ensure Jackson JSON dependencies are available"
    exit 1
fi