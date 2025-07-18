#!/usr/bin/env zsh

# 🚀 Run blockchain in DEVELOPMENT mode
# Detailed logging with colors and debug information

echo "🔧 Starting Private Blockchain in DEVELOPMENT mode..."
echo "📊 Logging: Detailed with colors and debug info"

# Change to project root directory
cd "$(dirname "$0")/.."

# Run with development profile (uses log4j2.xml by default)
mvn clean compile -Pdevelopment
mvn exec:java -Dexec.mainClass="demo.BlockchainDemo" -Pdevelopment

echo "✅ Development session completed"