#!/usr/bin/env zsh

# 🚀 Run blockchain in DEVELOPMENT mode
# Detailed logging with colors and debug information

echo "🔧 Starting Private Blockchain in DEVELOPMENT mode..."
echo "📊 Logging: Detailed with colors and debug info"

# Set development logging configuration
export LOGBACK_CONFIG_FILE=logback-development.xml

# Run with development profile
mvn clean compile -Pdevelopment
mvn exec:java -Pdevelopment -Dexec.mainClass="demo.BlockchainDemo"

echo "✅ Development session completed"